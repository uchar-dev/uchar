package dev.uchar.regelspraak.resolver;

import dev.uchar.regelspraak.message.Component;
import dev.uchar.regelspraak.message.Msg;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Designation;
import dev.uchar.regelspraak.model.RegelSpraakModel.Designatable;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.OriginForm;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.model.RegelSpraakModel.Vocabulary;
import dev.uchar.regelspraak.parser.RegelSpraak.*;
import dev.uchar.regelspraak.parser.RegelSpraakLexer;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

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
    /** The first declaration of every element pass 1 visited, annotated or not: its line is the line pass 2 refuses
     *  at, and with its comment it is the element's entry in {@link RegelSpraakModel#declarations}. A designatable
     *  declaration that is not here was written by the frontend itself (a row of a decision table, a member a marker
     *  generates), where no annotation can stand. */
    private final Map<Designatable, RegelSpraakModel.Declaration> rules = new LinkedHashMap<>();
    /** The elements pass 1 visited more than once: an element of a namespace no rule keeps unique (the rule groups,
     *  IR-SPEC ch. 3.9), or a refused repeat, which carries an ERROR of its own. */
    private final java.util.Set<Designatable> declaredAgain = new java.util.HashSet<>();
    /** The token stream the document was parsed from: its HIDDEN channel holds the comments above a declaration. */
    private final CommonTokenStream tokens;
    /** The line of each vocabulary declaration, by its name as declared. */
    private final Map<String, Integer> vocabularyLines = new LinkedHashMap<>();
    /** Every element whose designations {@link #compose} composes, with the parts of its name it composes them from:
     *  a refusal of such an element names those parts and stands on the line of the first one the source declares,
     *  where the author can change it. */
    private final Map<Designatable, List<NamePart>> composedFrom = new LinkedHashMap<>();

    /** A piece of a generated name: words of the name as they stand in it, and the declared element they name, null
     *  for the fixed words the frontend writes between the names. */
    record NamePart(String text, Designatable element) {}

    /** The members an extension declares again ({@link DefinitionResolver}): the type carries the declaration they
     *  repeat, so their designations are that declaration's, read there once; a refused repeat is refused by its own
     *  sentence. */
    private final java.util.Set<ObjectTypeMemberContext> repetitions;

    Designations(RegelSpraakModel model, java.util.Set<ObjectTypeMemberContext> repetitions, CommonTokenStream tokens) {
        this.model = model;
        this.repetitions = repetitions;
        this.tokens = tokens;
    }

    // --- pass 1: the declarations and their annotations -----------------------------------------

    void parse(RegelSpraakDocumentContext doc) {
        for (DefinitieContext d : doc.definitie())
            if (d.vocabulaireDefinition() != null) vocabulary(d.vocabulaireDefinition());
        List<Vocabulary> isDefault = model.vocabularies.stream().filter(Vocabulary::isDefault).toList();
        for (Vocabulary v : isDefault.subList(Math.min(1, isDefault.size()), isDefault.size()))
            error(
                    "RGS-038", params("vocabulary", isDefault.get(0).name(), "otherVocabulary", v.name()), vocabularyLines.get(v.name()));
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
        // a second group of one name is refused by its own sentence (WF-001, DefinitionResolver); its annotations
        // would land on the first group's key and be refused again as that group's
        java.util.Set<String> groups = new java.util.HashSet<>();
        for (RegelGroepContext g : doc.regelGroep())
            if (groups.add(dev.uchar.regelspraak.emit.IrEmitter.norm(DefinitionResolver.src(g.kaleNaam()))))
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
        String name = ctx.vocabulaireNaam().getText();
        int line = ctx.getStart().getLine();
        if (name.equalsIgnoreCase("mv")) {
            error(
                    "RGS-041", params(), line);
            return;
        }
        Vocabulary earlier = model.vocabulary(name);
        if (earlier != null) {
            error("RGS-042", params("name", name, "earlierName", earlier.name(),
                    "renamed", earlier.name().equals(name) ? "" : Msg.frontend("RGS-042.as", "earlierName", earlier.name())), line);
            return;
        }
        boolean isDefault = false;
        String language = null;
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if (!(ctx.getChild(i) instanceof org.antlr.v4.runtime.tree.TerminalNode t)
                    || t.getSymbol().getType() != dev.uchar.regelspraak.parser.RegelSpraakLexer.STANDAARD) continue;
            if (isDefault)
                error("RGS-043", params("vocabulary", name), line);
            isDefault = true;
        }
        boolean languageSeen = false;
        for (BenamingContext b : ctx.benaming()) {
            String key = key(b);
            if (!closed(b, Msg.frontend("RGS-051.vocabulary", "name", name))) continue;
            if (!key.equals("taal")) {
                error(
                        "RGS-044", params("vocabulary", name, "property", key), b.getStart().getLine());
                continue;
            }
            if (languageSeen) {
                error(
                        "RGS-045", params("vocabulary", name), b.getStart().getLine());
                continue;
            }
            languageSeen = true;
            String code = text(b);
            if (code.isEmpty() || code.chars().anyMatch(Character::isWhitespace))
                error(
                        "RGS-046", params("vocabulary", name, "code", code), b.getStart().getLine());
            else language = code;
        }
        model.vocabularies.add(new Vocabulary(name, isDefault, language));
        vocabularyLines.put(name, line);
    }

    private void objectType(ObjectTypeDefinitionContext ctx) {
        ObjectType ot = model.objectTypes.get(DefinitionResolver.src(ctx.objectTypeNaam()));
        if (ot == null) return;
        annotations(Designatable.objectType(ot.name), ctx.benaming(), ctx);
        for (ObjectTypeMemberContext m : ctx.objectTypeMember()) member(ot, m);
    }

    private void extension(ObjectTypeExtensieContext ctx) {
        ObjectType ot = model.objectTypeByName(DefinitionResolver.src(ctx.objectTypeNaam()));
        if (ot == null) return;
        for (ObjectTypeMemberContext m : ctx.objectTypeMember()) member(ot, m);
    }

    private void member(ObjectType ot, ObjectTypeMemberContext m) {
        if (repetitions.contains(m)) return;
        if (m.attribuutSpecificatie() != null) {
            AttribuutSpecificatieContext a = m.attribuutSpecificatie();
            annotations(Designatable.attribute(ot.name, DefinitionResolver.src(a.lowerNaam())), a.benaming(), a);
        } else if (m.kenmerkSpecificatie() instanceof BijvoeglijkKenmerkContext k) {
            annotations(Designatable.characteristic(ot.name, DefinitionResolver.src(k.lowerNaam())), k.benaming(), k);
        } else if (m.kenmerkSpecificatie() instanceof NaamwoordKenmerkContext k) {
            annotations(Designatable.characteristic(ot.name, DefinitionResolver.src(k.lowerNaam())), k.benaming(), k);
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
        if (rules.containsKey(element)) declaredAgain.add(element);
        else rules.put(element, new RegelSpraakModel.Declaration(declaration.getStart().getLine(), comment(declaration)));
        for (BenamingContext b : list) {
            String vocabulary = key(b);
            int line = b.getStart().getLine();
            if (!closed(b, description(element))) continue;
            String text = text(b);
            if (text.isEmpty()) {
                error(
                        "RGS-047", params("element", description(element), "vocabulary", vocabulary), line);
                continue;
            }
            if (text.indexOf('(') >= 0) {
                error(
                        "RGS-048", params("text", text, "element", description(element), "vocabulary", vocabulary), line);
                continue;
            }
            Vocabulary v = model.vocabulary(vocabulary);
            if (v == null) {
                error(
                        "RGS-049", params("text", text, "element", description(element), "vocabulary", vocabulary), line);
                continue;
            }
            List<Designation> existing = model.designations.computeIfAbsent(element, k -> new ArrayList<>());
            Designation earlier = existing.stream().filter(x -> x.vocabulary().equalsIgnoreCase(v.name()))
                    .findFirst().orElse(null);
            if (earlier != null) {
                error(
                        "RGS-050", params("element", head(description(element)), "vocabulary", v.name(), "earlierText", earlier.wording(), "text", text), line);
                continue;
            }
            existing.add(new Designation(v.name(), text));
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
    private static String text(BenamingContext b) {
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
            String name = element.name();
            int clip = name.length() - source.name().length();
            if (source.kind() != Designatable.Kind.ATTRIBUUT || clip < 1 || name.charAt(clip - 1) != ' '
                    || !name.regionMatches(true, clip, source.name(), 0, source.name().length()))
                throw new IllegalStateException("the generated element " + element + " is not named after its source "
                        + source + ", and the frontend cannot compose a designation for it");
            compose(element, List.of(new NamePart(name.substring(0, clip), null),
                    new NamePart(name.substring(clip), source)));
        }
        for (var e : nameParts.entrySet()) {
            Designatable element = e.getKey();
            String name = String.join("", e.getValue().stream().map(NamePart::text).toList());
            if (!name.equals(element.name())) {
                // a generated name another declaration also carries is refused with an ERROR of its own (WF-020)
                if (model.isOk())
                    throw new IllegalStateException("the generated element " + element + " is not named after its "
                            + "parts " + e.getValue() + ", and the frontend cannot compose a designation for it");
                continue;
            }
            compose(element, e.getValue());
        }
    }

    /** Composes and writes the designations of {@code element}, whose name is the pieces {@code parts}. */
    private void compose(Designatable element, List<NamePart> parts) {
        composedFrom.put(element, parts);
        for (Vocabulary v : model.vocabularies) {
            StringBuilder text = new StringBuilder();
            boolean named = false;
            for (NamePart d : parts) {
                String designation = d.element() == null ? null : model.designation(v.name(), d.element()).orElse(null);
                text.append(designation == null ? d.text() : designation);
                named |= designation != null;
            }
            if (!named || !v.isDefault() && text.toString().equals(element.name())) continue;
            if (model.designation(v.name(), element).isPresent()) {
                // only a declaration the author wrote under the generated name can carry one, and the form that
                // generates the name refuses such a name with an ERROR of its own
                if (model.isOk())
                    throw new IllegalStateException("the generated element " + element + " already carries a "
                            + "designation in the vocabulary '" + v.name() + "'");
                continue;
            }
            model.designations.computeIfAbsent(element, k -> new ArrayList<>())
                    .add(new Designation(v.name(), text.toString()));
        }
    }

    /** The clause that names where the designation of {@code element} in {@code v} comes from, where the frontend
     *  composed it: each part that carries a designation there, with that designation; empty for a written one, and
     *  where the element carries none there. */
    private Object compound(Vocabulary v, Designatable element) {
        List<NamePart> parts = composedFrom.get(element);
        if (parts == null || model.designation(v.name(), element).isEmpty()) return "";
        List<Msg> result = new ArrayList<>();
        for (NamePart d : parts)
            if (d.element() != null)
                model.designation(v.name(), d.element()).ifPresent(b ->
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
    private Integer line(Designatable element) {
        List<NamePart> parts = composedFrom.get(element);
        if (parts == null) return rules.containsKey(element) ? declaredLine(element) : null;
        for (Designatable d : parts(parts)) if (rules.containsKey(d)) return declaredLine(d);
        return null;
    }

    /** The line of the first declaration of {@code element}, 0 where pass 1 visited none. */
    private int declaredLine(Designatable element) {
        RegelSpraakModel.Declaration d = rules.get(element);
        return d == null ? 0 : d.line();
    }

    // --- pass 2: the finished model ----------------------------------------------------------------

    void check() {
        if (model.vocabularies.isEmpty() && model.designations.isEmpty()) return;
        List<Designatable> all = model.designatable();
        // a designation on a rule that is no named form on the wire would be lost with the rule's form
        for (Designatable b : model.designations.keySet())
            if (b.kind() == Designatable.Kind.VORM && all.stream().noneMatch(x -> x.equals(b)))
                error(
                        "RGS-052", params("name", b.name()), declaredLine(b));
        for (Vocabulary v : model.vocabularies) {
            if (v.isDefault()) complete(v, all);
            unique(v, all);
            if (!v.isDefault()) redundant(v, all);
        }
    }

    /** WF-119: the default vocabulary names every designatable declaration. */
    private void complete(Vocabulary v, List<Designatable> all) {
        for (Designatable b : all) {
            if (model.designation(v.name(), b).isPresent()) continue;
            List<Designatable> sources = composedFrom.containsKey(b) ? parts(composedFrom.get(b)) : null;
            Integer line = line(b);
            // a name of one part is the opgave family's, whose part is an attribute: 'dat'
            if (sources != null)
                error("RGS-053", params("element", head(description(b)),
                        "vocabulary", v.name(), "sources", enumeration(sources.stream().map(Designations::description).toList()),
                        "outcome", Msg.frontend(sources.size() == 1 ? "RGS-053.none-of-one" : "RGS-053.none-of-many")),
                        line == null ? 0 : line);
            else if (line != null)
                error("RGS-054", params("element", head(description(b)), "vocabulary", v.name()), line);
            else
                error("RGS-055", params("element", head(description(b)),
                        "vocabulary", v.name()), 0);
        }
    }

    /** WF-120: a designation that spells its own declaration's canonical name, in a vocabulary that is not the
     *  default, changes nothing. A WARNING, whose message quotes the designation: that is how the document accounts
     *  for it. */
    private void redundant(Vocabulary v, List<Designatable> all) {
        for (Designatable b : all)
            model.designation(v.name(), b).filter(t -> t.equals(b.name())).ifPresent(t ->
                    model.diagnostics.add(new Diagnostic(Severity.WARNING,
                            "RGS-056", params("text", t, "element", description(b), "vocabulary", v.name()),
                            declaredLine(b))));
    }

    /** WF-118: within one scope the effective names in a vocabulary are unique, case-insensitively; a collision of two
     *  canonical names is another rule's. The scopes are the canonical names': the kinds without an owner each on its
     *  own, the attributes and the characteristics of an object type with the ones it inherits, the roles of a fact
     *  type, the values of a domain. Each pair is reported once, at the type whose own member takes part in it. */
    private void unique(Vocabulary v, List<Designatable> all) {
        for (Designatable.Kind s : List.of(Designatable.Kind.OBJECTTYPE, Designatable.Kind.FEITTYPE,
                Designatable.Kind.PARAMETER, Designatable.Kind.VORM, Designatable.Kind.REGELGROEP))
            collisions(v, of(all, s, null), List.of());
        for (Designatable.Kind s : List.of(Designatable.Kind.ROL, Designatable.Kind.ENUMERATIEWAARDE)) {
            List<String> owners = new ArrayList<>();
            for (Designatable b : all) if (b.kind() == s && !owners.contains(b.owner())) owners.add(b.owner());
            for (String e : owners) collisions(v, of(all, s, e), List.of());
        }
        for (Designatable.Kind s : List.of(Designatable.Kind.ATTRIBUUT, Designatable.Kind.KENMERK))
            for (ObjectType ot : model.objectTypes.values()) {
                List<Designatable> own = of(all, s, ot.name);
                List<Designatable> members = new ArrayList<>(own);
                for (ObjectType t : ot.chain().subList(1, ot.chain().size())) members.addAll(of(all, s, t.name));
                collisions(v, members, own);
            }
    }

    private static List<Designatable> of(List<Designatable> all, Designatable.Kind category, String owner) {
        return all.stream().filter(b -> b.kind() == category
                && (owner == null || owner.equals(b.owner()))).toList();
    }

    /** The collisions among {@code members}; with a non-empty {@code own} only the pairs one of whose members is in it. */
    private void collisions(Vocabulary v, List<Designatable> members, List<Designatable> own) {
        for (int j = 0; j < members.size(); j++)
            for (int i = 0; i < j; i++) {
                Designatable a = members.get(i), b = members.get(j);
                if (!own.isEmpty() && !own.contains(a) && !own.contains(b)) continue;
                boolean da = model.designation(v.name(), a).isPresent(), db = model.designation(v.name(), b).isPresent();
                String nameA = model.effectiveName(v.name(), a), nb = model.effectiveName(v.name(), b);
                if (!(da || db) || !nameA.equalsIgnoreCase(nb)) continue;
                // at the later declaration, and in a chain scope at the type's own member, where the collision
                // arises: an inherited member was declared before the subtype that takes it in
                Designatable place = own.isEmpty() || own.contains(b) ? b : a;
                error(
                        "RGS-057", params("element", head(description(a)), "otherElement", description(b), "vocabulary", v.name(), "name", nb,
                                "compound", compound(v, a), "otherCompound", compound(v, b)),
                        line(place));
            }
    }

    // --- the declarations of the finished model --------------------------------------------------

    /** Writes {@link RegelSpraakModel#declarations}: per element of the finished model that the source declares in
     *  exactly one place, that declaration, in the order of {@link RegelSpraakModel#designatable()}. Runs after the
     *  rule check, when the model holds every named form; a rule that becomes no named form (a day kind, a start point,
     *  an allocation without a method) is no element and gets no entry. */
    void declare() {
        for (Designatable b : model.designatable())
            if (rules.containsKey(b) && !declaredAgain.contains(b)) model.declarations.putIfAbsent(b, rules.get(b));
    }

    /**
     * The comment directly above {@code declaration}: the line comments to the left of its first token, on the lines
     * right above its own, up to the first line that is empty or holds more than a comment, each without its
     * {@code //} and stripped, in source order. A comment behind the token before them, on that token's line, ends the
     * run: its line is no comment line.
     */
    private List<String> comment(ParserRuleContext declaration) {
        Token start = declaration.getStart();
        int index = start.getTokenIndex();
        if (index < 0 || index >= tokens.size() || tokens.get(index) != start)
            throw new IllegalStateException("the declaration on line " + start.getLine()
                    + " is not in the token stream of its parse");
        List<Token> hidden = tokens.getHiddenTokensToLeft(index);
        if (hidden == null) return List.of();
        int first = hidden.get(0).getTokenIndex();
        int previousLine = first == 0 ? 0 : endLine(tokens.get(first - 1));
        java.util.LinkedList<String> out = new java.util.LinkedList<>();
        int expected = start.getLine() - 1;
        for (int i = hidden.size() - 1; i >= 0; i--) {
            Token t = hidden.get(i);
            if (t.getType() != RegelSpraakLexer.LINE_COMMENT || t.getLine() != expected || t.getLine() <= previousLine)
                break;
            out.addFirst(t.getText().substring(2).strip());
            expected--;
        }
        return out;
    }

    /** The line of the last character of {@code t}. */
    private static int endLine(Token t) {
        String text = t.getText();
        return t.getLine() + (text == null ? 0 : (int) text.chars().filter(c -> c == '\n').count());
    }

    // --- sentences -------------------------------------------------------------------------------

    static Msg description(Designatable b) {
        return switch (b.kind()) {
            case OBJECTTYPE -> Msg.frontend("PART.designation.objecttype", "name", b.name());
            case ATTRIBUUT -> Msg.frontend("PART.designation.attribute", "name", b.name(), "owner", b.owner());
            case KENMERK -> Msg.frontend("PART.designation.characteristic-of-type", "name", b.name(), "owner", b.owner());
            case FEITTYPE -> Msg.frontend("PART.designation.fact-type", "name", b.name());
            case ROL -> Msg.frontend("PART.designation.role", "name", b.name(), "owner", b.owner());
            case ENUMERATIEWAARDE -> Msg.frontend("PART.designation.enumeration-value", "name", b.name(), "owner", b.owner());
            case PARAMETER -> Msg.frontend("PART.designation.parameter-declaration", "name", b.name());
            case VORM -> Msg.frontend("PART.designation.rule", "name", b.name());
            case REGELGROEP -> Msg.frontend("PART.designation.rule-group", "name", b.name());
        };
    }

    /** The description opening a sentence: the same part under its capitalised entry. */
    private static Msg head(Msg description) { return new Msg(description.component(), description.key() + ".head", description.args()); }

    /** 'a', 'a en b', 'a, b en c'. */
    private static Msg.Enumeration enumeration(List<?> parts) { return new Msg.Enumeration(Component.FRONTEND, parts); }

    private void error(String code, java.util.Map<String, Object> params, Integer line) {
        model.diagnostics.add(new Diagnostic(Severity.ERROR, code, params, line == null ? 0 : line));
    }
}
