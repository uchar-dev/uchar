package dev.uchar.regelspraak.resolver;

import dev.uchar.regelspraak.message.Component;
import dev.uchar.regelspraak.message.Msg;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Designation;
import dev.uchar.regelspraak.model.RegelSpraakModel.Designatable;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.OriginForm;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.model.RegelSpraakModel.Vocabulary;
import dev.uchar.regelspraak.parser.RegelSpraak.*;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic.params;

/**
 * The vocabulary dialect extension (grammar/DIVERGENCES.md, "Vocabulaires en benamingen"; IR-SPEC ch. 2.12): the
 * 'Vocabulaire' blocks and the designation annotations of a document, read into the model's side table and refused
 * where the verifier refuses the document it would give (WF-116 to WF-120) and where the source is no designation at
 * all. Two passes: {@link #parse} after the declarations, when every declaration an annotation stands on has its model
 * name; {@link #check} after the rule check, when the model holds every form the emitter writes. Between them
 * {@link #compose} composes the designations of the elements the frontend names after declared names — the members
 * and forms of a lowered opgave marker, the group of a subtype's blocks, the member and rule of a fold with steps after
 * the set — which have no place for an annotation. No rule reads the side table: a designation never resolves, so a
 * rule that writes one where it means a declaration is refused as any undeclared name is.
 */
final class Designations {

    private final RegelSpraakModel model;
    /** The source line of every declaration pass 1 visited, annotated or not: the line pass 2 refuses at. A
     *  designatable declaration that is not here was written by the frontend itself (a row of a decision table, a
     *  member a marker generates), where no annotation can stand. */
    private final Map<Designatable, Integer> regels = new LinkedHashMap<>();
    /** The line of each vocabulary declaration, by its name as declared. */
    private final Map<String, Integer> vocabularyLines = new LinkedHashMap<>();
    /** Every element whose designations {@link #compose} composes, with the parts of its name it composes them from:
     *  a refusal of such an element names those parts and stands on the line of the first one the source declares,
     *  where the author can change it. */
    private final Map<Designatable, List<NamePart>> composedFrom = new LinkedHashMap<>();

    /** A piece of a generated name: words of the name as they stand in it, and the declared element they name, null
     *  for the fixed words the frontend writes between the names. */
    record NamePart(String tekst, Designatable element) {}

    /** The members an extension declares again ({@link DefinitionResolver}): the type carries the declaration they
     *  repeat, so their designations are that declaration's, read there once; a refused repeat is refused by its own
     *  sentence. */
    private final java.util.Set<ObjectTypeMemberContext> repetitions;

    Designations(RegelSpraakModel model, java.util.Set<ObjectTypeMemberContext> repetitions) {
        this.model = model;
        this.repetitions = repetitions;
    }

    // --- pass 1: the declarations and their annotations -----------------------------------------

    void parse(RegelSpraakDocumentContext doc) {
        for (DefinitieContext d : doc.definitie())
            if (d.vocabulaireDefinition() != null) vocabulary(d.vocabulaireDefinition());
        List<Vocabulary> isDefault = model.vocabularies.stream().filter(Vocabulary::isDefault).toList();
        for (Vocabulary v : isDefault.subList(Math.min(1, isDefault.size()), isDefault.size()))
            error(
                    "RGS-038", params("vocabulary", isDefault.get(0).naam(), "otherVocabulary", v.naam()), vocabularyLines.get(v.naam()));
        for (DefinitieContext d : doc.definitie()) {
            if (d.objectTypeDefinition() != null) objectType(d.objectTypeDefinition());
            if (d.objectTypeExtensie() != null) extension(d.objectTypeExtensie());
            if (d.domeinDefinition() != null) domain(d.domeinDefinition());
            if (d.parameterDefinition() != null) {
                ParameterDefinitionContext p = d.parameterDefinition();
                annotations(Designatable.parameter(DefinitionResolver.src(p.parameterNaam())), p.benaming(), p);
            }
            if (d.feitTypeDefinition() != null) factType(d.feitTypeDefinition());
        }
        for (RegelContext r : DefinitionResolver.allRules(doc))
            annotations(Designatable.form(DefinitionResolver.src(r.regelNaam())), r.benaming(), r);
        for (RegelGroepContext g : doc.regelGroep())
            annotations(Designatable.ruleGroup(DefinitionResolver.src(g.kaleNaam())), g.benaming(), g);
        for (BeslistabelContext b : DefinitionResolver.allDecisionTables(doc))
            if (!b.benaming().isEmpty())
                error(
                        "RGS-039", params("name", DefinitionResolver.src(b.beslistabelNaam())), b.getStart().getLine());
        for (UitzonderingRegelContext u : DefinitionResolver.allExceptions(doc))
            if (!u.benaming().isEmpty())
                error(
                        "RGS-040", params("name", DefinitionResolver.src(u.regelNaam())),
                        u.getStart().getLine());
    }

    /** A vocabulary head: 'Vocabulaire &lt;naam&gt;' with '(standaard)' and '(taal: &lt;code&gt;)', each at most
     *  once, in free order. The name is a word by its token; 'mv' cannot be one, since '(mv:' opens a plural. */
    private void vocabulary(VocabulaireDefinitionContext ctx) {
        String naam = ctx.vocabulaireNaam().getText();
        int regel = ctx.getStart().getLine();
        if (naam.equalsIgnoreCase("mv")) {
            error(
                    "RGS-041", params(), regel);
            return;
        }
        Vocabulary earlier = model.vocabulary(naam);
        if (earlier != null) {
            error("RGS-042", params("name", naam, "earlierName", earlier.naam(),
                    "renamed", earlier.naam().equals(naam) ? "" : Msg.frontend("RGS-042.as", "earlierName", earlier.naam())), regel);
            return;
        }
        boolean isDefault = false;
        String language = null;
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if (!(ctx.getChild(i) instanceof org.antlr.v4.runtime.tree.TerminalNode t)
                    || t.getSymbol().getType() != dev.uchar.regelspraak.parser.RegelSpraakLexer.STANDAARD) continue;
            if (isDefault)
                error("RGS-043", params("vocabulary", naam), regel);
            isDefault = true;
        }
        boolean languageSeen = false;
        for (BenamingContext b : ctx.benaming()) {
            String key = key(b);
            if (!closed(b, Msg.frontend("RGS-051.vocabulary", "name", naam))) continue;
            if (!key.equals("taal")) {
                error(
                        "RGS-044", params("vocabulary", naam, "property", key), b.getStart().getLine());
                continue;
            }
            if (languageSeen) {
                error(
                        "RGS-045", params("vocabulary", naam), b.getStart().getLine());
                continue;
            }
            languageSeen = true;
            String code = tekst(b);
            if (code.isEmpty() || code.chars().anyMatch(Character::isWhitespace))
                error(
                        "RGS-046", params("vocabulary", naam, "code", code), b.getStart().getLine());
            else language = code;
        }
        model.vocabularies.add(new Vocabulary(naam, isDefault, language));
        vocabularyLines.put(naam, regel);
    }

    private void objectType(ObjectTypeDefinitionContext ctx) {
        Objecttype ot = model.objecttypen.get(DefinitionResolver.src(ctx.objectTypeNaam()));
        if (ot == null) return;
        annotations(Designatable.objectType(ot.naam), ctx.benaming(), ctx);
        for (ObjectTypeMemberContext m : ctx.objectTypeMember()) member(ot, m);
    }

    private void extension(ObjectTypeExtensieContext ctx) {
        Objecttype ot = model.objecttypen.get(DefinitionResolver.src(ctx.objectTypeNaam()));
        if (ot == null) return;
        for (ObjectTypeMemberContext m : ctx.objectTypeMember()) member(ot, m);
    }

    private void member(Objecttype ot, ObjectTypeMemberContext m) {
        if (repetitions.contains(m)) return;
        if (m.attribuutSpecificatie() != null) {
            AttribuutSpecificatieContext a = m.attribuutSpecificatie();
            annotations(Designatable.attribute(ot.naam, DefinitionResolver.src(a.lowerNaam())), a.benaming(), a);
        } else if (m.kenmerkSpecificatie() instanceof BijvoeglijkKenmerkContext k) {
            annotations(Designatable.characteristic(ot.naam, DefinitionResolver.src(k.lowerNaam())), k.benaming(), k);
        } else if (m.kenmerkSpecificatie() instanceof NaamwoordKenmerkContext k) {
            annotations(Designatable.characteristic(ot.naam, DefinitionResolver.src(k.lowerNaam())), k.benaming(), k);
        }
    }

    private void domain(DomeinDefinitionContext ctx) {
        if (ctx.enumeratieSpecificatie() == null) return;
        String domain = DefinitionResolver.src(ctx.domeinNaam());
        for (EnumeratieWaardeContext w : ctx.enumeratieSpecificatie().enumeratieWaarde()) {
            String t = w.ENUM_LITERAL().getText();
            annotations(Designatable.enumerationValue(domain, t.substring(1, t.length() - 1)), w.benaming(), w);
        }
    }

    private void factType(FeitTypeDefinitionContext ctx) {
        String ft = DefinitionResolver.src(ctx.feitTypeNaam());
        annotations(Designatable.factType(ft), ctx.benaming(), ctx);
        for (RolSpecificatieContext r : ctx.rolSpecificatie())
            annotations(Designatable.role(ft, DefinitionResolver.src(r.rolNaam())), r.benaming(), r);
    }

    /** The annotations of one declaration: each refused by name where the verifier would refuse the document, the
     *  others added to the model's side table in written order (the emitter sorts them by vocabulary). */
    private void annotations(Designatable element, List<BenamingContext> list, ParserRuleContext declaration) {
        regels.putIfAbsent(element, declaration.getStart().getLine());
        for (BenamingContext b : list) {
            String vocabulary = key(b);
            int regel = b.getStart().getLine();
            if (!closed(b, description(element))) continue;
            String tekst = tekst(b);
            if (tekst.isEmpty()) {
                error(
                        "RGS-047", params("element", description(element), "vocabulary", vocabulary), regel);
                continue;
            }
            if (tekst.indexOf('(') >= 0) {
                error(
                        "RGS-048", params("text", tekst, "element", description(element), "vocabulary", vocabulary), regel);
                continue;
            }
            Vocabulary v = model.vocabulary(vocabulary);
            if (v == null) {
                error(
                        "RGS-049", params("text", tekst, "element", description(element), "vocabulary", vocabulary), regel);
                continue;
            }
            List<Designation> existing = model.designations.computeIfAbsent(element, k -> new ArrayList<>());
            Designation earlier = existing.stream().filter(x -> x.vocabulary().equalsIgnoreCase(v.naam()))
                    .findFirst().orElse(null);
            if (earlier != null) {
                error(
                        "RGS-050", params("element", head(description(element)), "vocabulary", v.naam(), "earlierText", earlier.tekst(), "text", tekst), regel);
                continue;
            }
            existing.add(new Designation(v.naam(), tekst));
        }
    }

    /** Whether an annotation is closed on its own line; an open one is refused by name. */
    private boolean closed(BenamingContext b, Msg which) {
        if (b.BENAMING_EINDE() != null) return true;
        error(
                "RGS-051", params("key", key(b), "text", b.BENAMING_TEKST() == null ? "" : b.BENAMING_TEKST().getText(), "element", which),
                b.getStart().getLine());
        return false;
    }

    /** The word between '(' and ':' of an annotation's opener. */
    private static String key(BenamingContext b) {
        String t = b.BENAMING_START().getText();
        return t.substring(1, t.length() - 1);
    }

    /** The text of an annotation, trimmed at both ends. */
    private static String tekst(BenamingContext b) {
        return b.BENAMING_TEKST() == null ? "" : b.BENAMING_TEKST().getText().trim();
    }

    // --- between the passes: the designations the frontend composes -------------------------------

    /** The designations of the elements the frontend names after declared names (grammar/DIVERGENCES.md, "Gegenereerde
     *  elementen", "Designations" and "Part by part"). A member or form of a lowered opgave marker is named
     *  '&lt;woorden&gt; X' after the marked attribute X; the group of a subtype's blocks and the member and rule of a
     *  fold with steps after the set are named after several declarations, whose pieces the rule check hands over in
     *  {@code nameParts}. In every vocabulary in which a part carries a designation, the element carries its own name
     *  with each such part replaced by that designation, taken over literally, the other parts and the fixed words as
     *  they stand; where no part carries one, neither does the element (maintainer decisions 2026-10-15). In a
     *  vocabulary that is not the default a composed designation that spells the element's own name is left out: it
     *  changes no effective name, and the wire would need a warning for it (WF-120), which the part's own designation
     *  draws already. Runs after the rule check, which fills {@link RegelSpraakModel#generated}, and before
     *  {@link #check}, which judges these designations as it judges the written ones. The opgave family goes first: its
     *  members can be a part of another name, and a part is read from the side table. The other forms of the origin
     *  table get none: their names come from a source no vocabulary names. */
    void compose(Map<Designatable, List<NamePart>> nameParts) {
        if (model.vocabularies.isEmpty()) return;
        for (var e : model.generated.entrySet()) {
            if (e.getValue().form() != OriginForm.OPGAVE_BOVEN_BEREKENING) continue;
            Designatable element = e.getKey(), source = e.getValue().source();
            String naam = element.naam();
            int clip = naam.length() - source.naam().length();
            if (source.category() != Designatable.Kind.ATTRIBUUT || clip < 1 || naam.charAt(clip - 1) != ' '
                    || !naam.regionMatches(true, clip, source.naam(), 0, source.naam().length()))
                throw new IllegalStateException("het gegenereerde element " + element + " heet niet naar zijn bron "
                        + source + ", en de frontend kan er geen benaming voor samenstellen");
            compose(element, List.of(new NamePart(naam.substring(0, clip), null),
                    new NamePart(naam.substring(clip), source)));
        }
        for (var e : nameParts.entrySet()) {
            Designatable element = e.getKey();
            String naam = String.join("", e.getValue().stream().map(NamePart::tekst).toList());
            if (!naam.equals(element.naam())) {
                // a generated name another declaration also carries is refused with an ERROR of its own (WF-020)
                if (model.isOk())
                    throw new IllegalStateException("het gegenereerde element " + element + " heet niet naar zijn "
                            + "delen " + e.getValue() + ", en de frontend kan er geen benaming voor samenstellen");
                continue;
            }
            compose(element, e.getValue());
        }
    }

    /** Composes and writes the designations of {@code element}, whose name is the pieces {@code parts}. */
    private void compose(Designatable element, List<NamePart> parts) {
        composedFrom.put(element, parts);
        for (Vocabulary v : model.vocabularies) {
            StringBuilder tekst = new StringBuilder();
            boolean named = false;
            for (NamePart d : parts) {
                String benaming = d.element() == null ? null : model.benaming(v.naam(), d.element()).orElse(null);
                tekst.append(benaming == null ? d.tekst() : benaming);
                named |= benaming != null;
            }
            if (!named || !v.isDefault() && tekst.toString().equals(element.naam())) continue;
            if (model.benaming(v.naam(), element).isPresent()) {
                // only a declaration the author wrote under the generated name can carry one, and the form that
                // generates the name refuses such a name with an ERROR of its own
                if (model.isOk())
                    throw new IllegalStateException("het gegenereerde element " + element + " draagt al een "
                            + "benaming in het vocabulaire '" + v.naam() + "'");
                continue;
            }
            model.designations.computeIfAbsent(element, k -> new ArrayList<>())
                    .add(new Designation(v.naam(), tekst.toString()));
        }
    }

    /** The clause that names where the designation of {@code element} in {@code v} comes from, where the frontend
     *  composed it: each part that carries a designation there, with that designation; empty for a written one, and
     *  where the element carries none there. */
    private Object compound(Vocabulary v, Designatable element) {
        List<NamePart> parts = composedFrom.get(element);
        if (parts == null || model.benaming(v.naam(), element).isEmpty()) return "";
        List<Msg> result = new ArrayList<>();
        for (NamePart d : parts)
            if (d.element() != null)
                model.benaming(v.naam(), d.element()).ifPresent(b ->
                        result.add(Msg.frontend("RGS-057.source-designation", "designation", b, "element", description(d.element()))));
        return Msg.frontend("RGS-057.compound", "element", description(element), "sources", enumeration(result));
    }

    /** The declared parts of a composed element's name, in the order of the name. */
    private static List<Designatable> parts(List<NamePart> parts) {
        return parts.stream().map(NamePart::element).filter(java.util.Objects::nonNull).toList();
    }

    /** The line a refusal of {@code element} stands on: its own declaration's, or for an element whose designations
     *  the frontend composes, that of the first part of its name the source declares, where the author can change
     *  what it is named; null where the frontend wrote the element, and every part of it, without a source line. */
    private Integer regel(Designatable element) {
        List<NamePart> parts = composedFrom.get(element);
        if (parts == null) return regels.get(element);
        for (Designatable d : parts(parts)) if (regels.containsKey(d)) return regels.get(d);
        return null;
    }

    // --- pass 2: the finished model ----------------------------------------------------------------

    void check() {
        if (model.vocabularies.isEmpty() && model.designations.isEmpty()) return;
        List<Designatable> all = model.designatable();
        // a designation on a rule that is no named form on the wire would be lost with the rule's form
        for (Designatable b : model.designations.keySet())
            if (b.category() == Designatable.Kind.VORM && all.stream().noneMatch(x -> x.equals(b)))
                error(
                        "RGS-052", params("name", b.naam()), regels.getOrDefault(b, 0));
        for (Vocabulary v : model.vocabularies) {
            if (v.isDefault()) complete(v, all);
            unique(v, all);
            if (!v.isDefault()) redundant(v, all);
        }
    }

    /** WF-119: the default vocabulary names every designatable declaration. */
    private void complete(Vocabulary v, List<Designatable> all) {
        for (Designatable b : all) {
            if (model.benaming(v.naam(), b).isPresent()) continue;
            List<Designatable> sources = composedFrom.containsKey(b) ? parts(composedFrom.get(b)) : null;
            Integer regel = regel(b);
            // a name of one part is the opgave family's, whose part is an attribute: 'dat'
            if (sources != null)
                error("RGS-053", params("element", head(description(b)),
                        "vocabulary", v.naam(), "sources", enumeration(sources.stream().map(Designations::description).toList()),
                        "outcome", Msg.frontend(sources.size() == 1 ? "RGS-053.none-of-one" : "RGS-053.none-of-many")),
                        regel == null ? 0 : regel);
            else if (regel != null)
                error("RGS-054", params("element", head(description(b)), "vocabulary", v.naam()), regel);
            else
                error("RGS-055", params("element", head(description(b)),
                        "vocabulary", v.naam()), 0);
        }
    }

    /** WF-120: a designation that spells its own declaration's canonical name, in a vocabulary that is not the
     *  default, changes nothing. A WARNING, whose message quotes the designation: that is how the document accounts
     *  for it. */
    private void redundant(Vocabulary v, List<Designatable> all) {
        for (Designatable b : all)
            model.benaming(v.naam(), b).filter(t -> t.equals(b.naam())).ifPresent(t ->
                    model.diagnostics.add(new Diagnostic(Severity.WARNING,
                            "RGS-056", params("text", t, "element", description(b), "vocabulary", v.naam()),
                            regels.getOrDefault(b, 0))));
    }

    /** WF-118: within one scope the effective names in a vocabulary are unique, case-insensitively; a collision of two
     *  canonical names is another rule's. The scopes are the canonical names': the kinds without an owner each on its
     *  own, the attributes and the characteristics of an object type with the ones it inherits, the roles of a fact
     *  type, the values of a domain. Each pair is reported once, at the type whose own member takes part in it. */
    private void unique(Vocabulary v, List<Designatable> all) {
        for (Designatable.Kind s : List.of(Designatable.Kind.OBJECTTYPE, Designatable.Kind.FEITTYPE,
                Designatable.Kind.PARAMETER, Designatable.Kind.VORM, Designatable.Kind.REGELGROEP))
            collisions(v, van(all, s, null), List.of());
        for (Designatable.Kind s : List.of(Designatable.Kind.ROL, Designatable.Kind.ENUMERATIEWAARDE)) {
            List<String> owners = new ArrayList<>();
            for (Designatable b : all) if (b.category() == s && !owners.contains(b.owner())) owners.add(b.owner());
            for (String e : owners) collisions(v, van(all, s, e), List.of());
        }
        for (Designatable.Kind s : List.of(Designatable.Kind.ATTRIBUUT, Designatable.Kind.KENMERK))
            for (Objecttype ot : model.objecttypen.values()) {
                List<Designatable> own = van(all, s, ot.naam);
                List<Designatable> members = new ArrayList<>(own);
                for (Objecttype t : ot.chain().subList(1, ot.chain().size())) members.addAll(van(all, s, t.naam));
                collisions(v, members, own);
            }
    }

    private static List<Designatable> van(List<Designatable> all, Designatable.Kind category, String owner) {
        return all.stream().filter(b -> b.category() == category
                && (owner == null || owner.equals(b.owner()))).toList();
    }

    /** The collisions among {@code leden}; with a non-empty {@code eigen} only the pairs one of whose members is in it. */
    private void collisions(Vocabulary v, List<Designatable> members, List<Designatable> own) {
        for (int j = 0; j < members.size(); j++)
            for (int i = 0; i < j; i++) {
                Designatable a = members.get(i), b = members.get(j);
                if (!own.isEmpty() && !own.contains(a) && !own.contains(b)) continue;
                boolean da = model.benaming(v.naam(), a).isPresent(), db = model.benaming(v.naam(), b).isPresent();
                String nameA = model.effectiveName(v.naam(), a), nb = model.effectiveName(v.naam(), b);
                if (!(da || db) || !nameA.equalsIgnoreCase(nb)) continue;
                // at the later declaration, and in a chain scope at the type's own member, where the collision
                // arises: an inherited member was declared before the subtype that takes it in
                Designatable place = own.isEmpty() || own.contains(b) ? b : a;
                error(
                        "RGS-057", params("element", head(description(a)), "otherElement", description(b), "vocabulary", v.naam(), "name", nb,
                                "compound", compound(v, a), "otherCompound", compound(v, b)),
                        regel(place));
            }
    }

    // --- sentences -------------------------------------------------------------------------------

    static Msg description(Designatable b) {
        return switch (b.category()) {
            case OBJECTTYPE -> Msg.frontend("PART.designation.objecttype", "name", b.naam());
            case ATTRIBUUT -> Msg.frontend("PART.designation.attribute", "name", b.naam(), "owner", b.owner());
            case KENMERK -> Msg.frontend("PART.designation.characteristic-of-type", "name", b.naam(), "owner", b.owner());
            case FEITTYPE -> Msg.frontend("PART.designation.fact-type", "name", b.naam());
            case ROL -> Msg.frontend("PART.designation.role", "name", b.naam(), "owner", b.owner());
            case ENUMERATIEWAARDE -> Msg.frontend("PART.designation.enumeration-value", "name", b.naam(), "owner", b.owner());
            case PARAMETER -> Msg.frontend("PART.designation.parameter-declaration", "name", b.naam());
            case VORM -> Msg.frontend("PART.designation.rule", "name", b.naam());
            case REGELGROEP -> Msg.frontend("PART.designation.rule-group", "name", b.naam());
        };
    }

    /** The description opening a sentence: the same part under its capitalised entry. */
    private static Msg head(Msg description) { return new Msg(description.component(), description.key() + ".head", description.args()); }

    /** 'a', 'a en b', 'a, b en c'. */
    private static Msg.Enumeration enumeration(List<?> parts) { return new Msg.Enumeration(Component.FRONTEND, parts); }

    private void error(String code, java.util.Map<String, Object> params, Integer regel) {
        model.diagnostics.add(new Diagnostic(Severity.ERROR, code, params, regel == null ? 0 : regel));
    }
}
