package dev.uchar.regelspraak.resolver;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Designation;
import dev.uchar.regelspraak.model.RegelSpraakModel.Benoembaar;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Herkomstvorm;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.model.RegelSpraakModel.Vocabulaire;
import dev.uchar.regelspraak.parser.RegelSpraak.*;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private final Map<Benoembaar, Integer> regels = new LinkedHashMap<>();
    /** The line of each vocabulary declaration, by its name as declared. */
    private final Map<String, Integer> vocabularyLines = new LinkedHashMap<>();
    /** Every element whose designations {@link #compose} composes, with the parts of its name it composes them from:
     *  a refusal of such an element names those parts and stands on the line of the first one the source declares,
     *  where the author can change it. */
    private final Map<Benoembaar, List<NamePart>> composedFrom = new LinkedHashMap<>();

    /** A piece of a generated name: words of the name as they stand in it, and the declared element they name, null
     *  for the fixed words the frontend writes between the names. */
    record NamePart(String tekst, Benoembaar element) {}

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
            if (d.vocabulaireDefinition() != null) vocabulaire(d.vocabulaireDefinition());
        List<Vocabulaire> standaard = model.vocabulaires.stream().filter(Vocabulaire::standaard).toList();
        for (Vocabulaire v : standaard.subList(Math.min(1, standaard.size()), standaard.size()))
            fout("de vocabulaires '" + standaard.get(0).naam() + "' en '" + v.naam() + "' zijn allebei het "
                    + "standaardvocabulaire; een document heeft er hooguit één (WF-116)", vocabularyLines.get(v.naam()));
        for (DefinitieContext d : doc.definitie()) {
            if (d.objectTypeDefinition() != null) objecttype(d.objectTypeDefinition());
            if (d.objectTypeExtensie() != null) extension(d.objectTypeExtensie());
            if (d.domeinDefinition() != null) domein(d.domeinDefinition());
            if (d.parameterDefinition() != null) {
                ParameterDefinitionContext p = d.parameterDefinition();
                annotations(Benoembaar.parameter(DefinitionResolver.src(p.parameterNaam())), p.benaming(), p);
            }
            if (d.feitTypeDefinition() != null) feittype(d.feitTypeDefinition());
        }
        for (RegelContext r : DefinitionResolver.allRules(doc))
            annotations(Benoembaar.vorm(DefinitionResolver.src(r.regelNaam())), r.benaming(), r);
        for (RegelGroepContext g : doc.regelGroep())
            annotations(Benoembaar.regelgroep(DefinitionResolver.src(g.kaleNaam())), g.benaming(), g);
        for (BeslistabelContext b : DefinitionResolver.allDecisionTables(doc))
            if (!b.benaming().isEmpty())
                fout("beslistabel '" + DefinitionResolver.src(b.beslistabelNaam()) + "' draagt een benaming, en een "
                        + "beslistabel heeft er geen: zij staat voor de regels van haar rijen, die hun namen van de "
                        + "frontend krijgen; schrijf de benaming op een regel", b.getStart().getLine());
        for (UitzonderingRegelContext u : DefinitionResolver.allExceptions(doc))
            if (!u.benaming().isEmpty())
                fout("het blok '" + DefinitionResolver.src(u.regelNaam()) + "' draagt een benaming, en een "
                        + "uitzonderings- of afwijkingsblok heeft er geen: het wordt een default in de groep van zijn "
                        + "basisregel, en een default heeft geen naam op de wire; schrijf de benaming op de basisregel",
                        u.getStart().getLine());
    }

    /** A vocabulary head: 'Vocabulaire &lt;naam&gt;' with '(standaard)' and '(taal: &lt;code&gt;)', each at most
     *  once, in free order. The name is a word by its token; 'mv' cannot be one, since '(mv:' opens a plural. */
    private void vocabulaire(VocabulaireDefinitionContext ctx) {
        String naam = ctx.vocabulaireNaam().getText();
        int regel = ctx.getStart().getLine();
        if (naam.equalsIgnoreCase("mv")) {
            fout("vocabulaire 'mv': die naam is gereserveerd, want '(mv:' opent de meervoudsvorm van een naam, en een "
                    + "benaming in dit vocabulaire zou als meervoud gelezen worden", regel);
            return;
        }
        Vocabulaire earlier = model.vocabulaire(naam);
        if (earlier != null) {
            fout("vocabulaire '" + naam + "' is al gedeclareerd"
                    + (earlier.naam().equals(naam) ? "" : " als '" + earlier.naam() + "'")
                    + "; de namen van de vocabulaires zijn uniek, hoofdletterongevoelig (WF-116)", regel);
            return;
        }
        boolean standaard = false;
        String language = null;
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if (!(ctx.getChild(i) instanceof org.antlr.v4.runtime.tree.TerminalNode t)
                    || t.getSymbol().getType() != dev.uchar.regelspraak.parser.RegelSpraakLexer.STANDAARD) continue;
            if (standaard)
                fout("vocabulaire '" + naam + "' draagt '(standaard)' twee keer", regel);
            standaard = true;
        }
        boolean languageSeen = false;
        for (BenamingContext b : ctx.benaming()) {
            String sleutel = sleutel(b);
            if (!closed(b, "vocabulaire '" + naam + "'")) continue;
            if (!sleutel.equals("taal")) {
                fout("vocabulaire '" + naam + "': '(" + sleutel + ": …)' is geen eigenschap van een vocabulaire; een "
                        + "vocabulairekop kent '(standaard)' en '(taal: <code>)'", b.getStart().getLine());
                continue;
            }
            if (languageSeen) {
                fout("vocabulaire '" + naam + "' draagt '(taal: …)' twee keer", b.getStart().getLine());
                continue;
            }
            languageSeen = true;
            String code = tekst(b);
            if (code.isEmpty() || code.chars().anyMatch(Character::isWhitespace))
                fout("vocabulaire '" + naam + "': de taalcode '" + code + "' is leeg of bevat witruimte; een taalcode "
                        + "is één woord (WF-116)", b.getStart().getLine());
            else language = code;
        }
        model.vocabulaires.add(new Vocabulaire(naam, standaard, language));
        vocabularyLines.put(naam, regel);
    }

    private void objecttype(ObjectTypeDefinitionContext ctx) {
        Objecttype ot = model.objecttypen.get(DefinitionResolver.src(ctx.objectTypeNaam()));
        if (ot == null) return;
        annotations(Benoembaar.objecttype(ot.naam), ctx.benaming(), ctx);
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
            annotations(Benoembaar.attribuut(ot.naam, DefinitionResolver.src(a.lowerNaam())), a.benaming(), a);
        } else if (m.kenmerkSpecificatie() instanceof BijvoeglijkKenmerkContext k) {
            annotations(Benoembaar.kenmerk(ot.naam, DefinitionResolver.src(k.lowerNaam())), k.benaming(), k);
        } else if (m.kenmerkSpecificatie() instanceof NaamwoordKenmerkContext k) {
            annotations(Benoembaar.kenmerk(ot.naam, DefinitionResolver.src(k.lowerNaam())), k.benaming(), k);
        }
    }

    private void domein(DomeinDefinitionContext ctx) {
        if (ctx.enumeratieSpecificatie() == null) return;
        String domein = DefinitionResolver.src(ctx.domeinNaam());
        for (EnumeratieWaardeContext w : ctx.enumeratieSpecificatie().enumeratieWaarde()) {
            String t = w.ENUM_LITERAL().getText();
            annotations(Benoembaar.enumeratiewaarde(domein, t.substring(1, t.length() - 1)), w.benaming(), w);
        }
    }

    private void feittype(FeitTypeDefinitionContext ctx) {
        String ft = DefinitionResolver.src(ctx.feitTypeNaam());
        annotations(Benoembaar.feittype(ft), ctx.benaming(), ctx);
        for (RolSpecificatieContext r : ctx.rolSpecificatie())
            annotations(Benoembaar.rol(ft, DefinitionResolver.src(r.rolNaam())), r.benaming(), r);
    }

    /** The annotations of one declaration: each refused by name where the verifier would refuse the document, the
     *  others added to the model's side table in written order (the emitter sorts them by vocabulary). */
    private void annotations(Benoembaar element, List<BenamingContext> lijst, ParserRuleContext declaratie) {
        regels.putIfAbsent(element, declaratie.getStart().getLine());
        for (BenamingContext b : lijst) {
            String vocabulaire = sleutel(b);
            int regel = b.getStart().getLine();
            if (!closed(b, description(element))) continue;
            String tekst = tekst(b);
            if (tekst.isEmpty()) {
                fout("de benaming van " + description(element) + " in het vocabulaire '" + vocabulaire + "' is leeg; "
                        + "een benaming is tekst tussen de dubbele punt en het sluithaakje (WF-117)", regel);
                continue;
            }
            if (tekst.indexOf('(') >= 0) {
                fout("de benaming '" + tekst + "' van " + description(element) + " in het vocabulaire '" + vocabulaire
                        + "' bevat een haakje; een benaming is tekst zonder haakjes, want het sluithaakje sluit de "
                        + "annotatie (WF-117)", regel);
                continue;
            }
            Vocabulaire v = model.vocabulaire(vocabulaire);
            if (v == null) {
                fout("de benaming '" + tekst + "' van " + description(element) + " noemt het vocabulaire '"
                        + vocabulaire + "', dat het document niet declareert ('Vocabulaire " + vocabulaire + "', "
                        + "WF-117)", regel);
                continue;
            }
            List<Designation> existing = model.designations.computeIfAbsent(element, k -> new ArrayList<>());
            Designation earlier = existing.stream().filter(x -> x.vocabulaire().equalsIgnoreCase(v.naam()))
                    .findFirst().orElse(null);
            if (earlier != null) {
                fout(head(description(element)) + " draagt twee benamingen in het vocabulaire '" + v.naam() + "', '"
                        + earlier.tekst() + "' en '" + tekst + "'; een declaratie heeft hooguit één benaming per "
                        + "vocabulaire (WF-117)", regel);
                continue;
            }
            existing.add(new Designation(v.naam(), tekst));
        }
    }

    /** Whether an annotation is closed on its own line; an open one is refused by name. */
    private boolean closed(BenamingContext b, String which) {
        if (b.BENAMING_EINDE() != null) return true;
        fout("de annotatie '(" + sleutel(b) + ":" + (b.BENAMING_TEKST() == null ? "" : b.BENAMING_TEKST().getText())
                + "' bij " + which + " is niet op haar regel gesloten; een benaming staat tussen haakjes op één regel",
                b.getStart().getLine());
        return false;
    }

    /** The word between '(' and ':' of an annotation's opener. */
    private static String sleutel(BenamingContext b) {
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
     *  draws already. Runs after the rule check, which fills {@link RegelSpraakModel#gegenereerd}, and before
     *  {@link #check}, which judges these designations as it judges the written ones. The opgave family goes first: its
     *  members can be a part of another name, and a part is read from the side table. The other forms of the origin
     *  table get none: their names come from a source no vocabulary names. */
    void compose(Map<Benoembaar, List<NamePart>> nameParts) {
        if (model.vocabulaires.isEmpty()) return;
        for (var e : model.gegenereerd.entrySet()) {
            if (e.getValue().vorm() != Herkomstvorm.OPGAVE_BOVEN_BEREKENING) continue;
            Benoembaar element = e.getKey(), bron = e.getValue().bron();
            String naam = element.naam();
            int clip = naam.length() - bron.naam().length();
            if (bron.soort() != Benoembaar.Soort.ATTRIBUUT || clip < 1 || naam.charAt(clip - 1) != ' '
                    || !naam.regionMatches(true, clip, bron.naam(), 0, bron.naam().length()))
                throw new IllegalStateException("het gegenereerde element " + element + " heet niet naar zijn bron "
                        + bron + ", en de frontend kan er geen benaming voor samenstellen");
            compose(element, List.of(new NamePart(naam.substring(0, clip), null),
                    new NamePart(naam.substring(clip), bron)));
        }
        for (var e : nameParts.entrySet()) {
            Benoembaar element = e.getKey();
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
    private void compose(Benoembaar element, List<NamePart> parts) {
        composedFrom.put(element, parts);
        for (Vocabulaire v : model.vocabulaires) {
            StringBuilder tekst = new StringBuilder();
            boolean benoemd = false;
            for (NamePart d : parts) {
                String benaming = d.element() == null ? null : model.benaming(v.naam(), d.element()).orElse(null);
                tekst.append(benaming == null ? d.tekst() : benaming);
                benoemd |= benaming != null;
            }
            if (!benoemd || !v.standaard() && tekst.toString().equals(element.naam())) continue;
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
    private String compound(Vocabulaire v, Benoembaar element) {
        List<NamePart> parts = composedFrom.get(element);
        if (parts == null || model.benaming(v.naam(), element).isEmpty()) return "";
        List<String> result = new ArrayList<>();
        for (NamePart d : parts)
            if (d.element() != null)
                model.benaming(v.naam(), d.element()).ifPresent(b ->
                        result.add("de benaming '" + b + "' van " + description(d.element())));
        return "; de frontend stelde de benaming van " + description(element) + " samen uit " + enumeration(result);
    }

    /** The declared parts of a composed element's name, in the order of the name. */
    private static List<Benoembaar> parts(List<NamePart> parts) {
        return parts.stream().map(NamePart::element).filter(java.util.Objects::nonNull).toList();
    }

    /** The line a refusal of {@code element} stands on: its own declaration's, or for an element whose designations
     *  the frontend composes, that of the first part of its name the source declares, where the author can change
     *  what it is named; null where the frontend wrote the element, and every part of it, without a source line. */
    private Integer regel(Benoembaar element) {
        List<NamePart> parts = composedFrom.get(element);
        if (parts == null) return regels.get(element);
        for (Benoembaar d : parts(parts)) if (regels.containsKey(d)) return regels.get(d);
        return null;
    }

    // --- pass 2: the finished model ----------------------------------------------------------------

    void check() {
        if (model.vocabulaires.isEmpty() && model.designations.isEmpty()) return;
        List<Benoembaar> all = model.benoembaar();
        // a designation on a rule that is no named form on the wire would be lost with the rule's form
        for (Benoembaar b : model.designations.keySet())
            if (b.soort() == Benoembaar.Soort.VORM && all.stream().noneMatch(x -> x.equals(b)))
                fout("de regel '" + b.naam() + "' draagt een benaming, maar wordt op de wire geen benoemde vorm (een "
                        + "regel, creatie, verdeling, consistentie, uniciteit of defaultsgroep): een dagsoortdefinitie "
                        + "en een startpuntbepaling hebben geen naam, en een verdeling zonder methode wordt niet "
                        + "geschreven; zonder vorm heeft de benaming geen plaats", regels.getOrDefault(b, 0));
        for (Vocabulaire v : model.vocabulaires) {
            if (v.standaard()) complete(v, all);
            unique(v, all);
            if (!v.standaard()) redundant(v, all);
        }
    }

    /** WF-119: the default vocabulary names every designatable declaration. */
    private void complete(Vocabulaire v, List<Benoembaar> all) {
        for (Benoembaar b : all) {
            if (model.benaming(v.naam(), b).isPresent()) continue;
            List<Benoembaar> sources = composedFrom.containsKey(b) ? parts(composedFrom.get(b)) : null;
            Integer regel = regel(b);
            // a name of one part is the opgave family's, whose part is an attribute: 'dat'
            String suffix = sources != null ? "; de frontend stelt de benaming van deze declaratie samen uit die van "
                    + enumeration(sources.stream().map(Designations::description).toList())
                    + (sources.size() == 1 ? ", en dat draagt er geen" : ", en geen daarvan draagt er een")
                    : regel != null ? "" : "; de frontend schreef deze declaratie zelf, zonder plaats voor een annotatie";
            fout(head(description(b)) + " draagt geen benaming in het standaardvocabulaire '" + v.naam() + "', dat "
                    + "elke declaratie noemt (WF-119)" + suffix, regel == null ? 0 : regel);
        }
    }

    /** WF-120: a designation that spells its own declaration's canonical name, in a vocabulary that is not the
     *  default, changes nothing. A WARNING, whose message quotes the designation: that is how the document accounts
     *  for it. */
    private void redundant(Vocabulaire v, List<Benoembaar> all) {
        for (Benoembaar b : all)
            model.benaming(v.naam(), b).filter(t -> t.equals(b.naam())).ifPresent(t ->
                    model.diagnostics.add(new Diagnostic(Severity.WARNING, "de benaming '" + t + "' van "
                            + description(b) + " in het vocabulaire '" + v.naam() + "' spelt haar canonieke naam en "
                            + "is overbodig: de effectieve naam is dezelfde zonder haar (WF-120)",
                            regels.getOrDefault(b, 0))));
    }

    /** WF-118: within one scope the effective names in a vocabulary are unique, case-insensitively; a collision of two
     *  canonical names is another rule's. The scopes are the canonical names': the kinds without an owner each on its
     *  own, the attributes and the characteristics of an object type with the ones it inherits, the roles of a fact
     *  type, the values of a domain. Each pair is reported once, at the type whose own member takes part in it. */
    private void unique(Vocabulaire v, List<Benoembaar> all) {
        for (Benoembaar.Soort s : List.of(Benoembaar.Soort.OBJECTTYPE, Benoembaar.Soort.FEITTYPE,
                Benoembaar.Soort.PARAMETER, Benoembaar.Soort.VORM, Benoembaar.Soort.REGELGROEP))
            collisions(v, van(all, s, null), List.of());
        for (Benoembaar.Soort s : List.of(Benoembaar.Soort.ROL, Benoembaar.Soort.ENUMERATIEWAARDE)) {
            List<String> owners = new ArrayList<>();
            for (Benoembaar b : all) if (b.soort() == s && !owners.contains(b.eigenaar())) owners.add(b.eigenaar());
            for (String e : owners) collisions(v, van(all, s, e), List.of());
        }
        for (Benoembaar.Soort s : List.of(Benoembaar.Soort.ATTRIBUUT, Benoembaar.Soort.KENMERK))
            for (Objecttype ot : model.objecttypen.values()) {
                List<Benoembaar> own = van(all, s, ot.naam);
                List<Benoembaar> leden = new ArrayList<>(own);
                for (Objecttype t : ot.keten().subList(1, ot.keten().size())) leden.addAll(van(all, s, t.naam));
                collisions(v, leden, own);
            }
    }

    private static List<Benoembaar> van(List<Benoembaar> all, Benoembaar.Soort soort, String eigenaar) {
        return all.stream().filter(b -> b.soort() == soort
                && (eigenaar == null || eigenaar.equals(b.eigenaar()))).toList();
    }

    /** The collisions among {@code leden}; with a non-empty {@code eigen} only the pairs one of whose members is in it. */
    private void collisions(Vocabulaire v, List<Benoembaar> leden, List<Benoembaar> own) {
        for (int j = 0; j < leden.size(); j++)
            for (int i = 0; i < j; i++) {
                Benoembaar a = leden.get(i), b = leden.get(j);
                if (!own.isEmpty() && !own.contains(a) && !own.contains(b)) continue;
                boolean da = model.benaming(v.naam(), a).isPresent(), db = model.benaming(v.naam(), b).isPresent();
                String nameA = model.effectiveName(v.naam(), a), nb = model.effectiveName(v.naam(), b);
                if (!(da || db) || !nameA.equalsIgnoreCase(nb)) continue;
                // at the later declaration, and in a chain scope at the type's own member, where the collision
                // arises: an inherited member was declared before the subtype that takes it in
                Benoembaar place = own.isEmpty() || own.contains(b) ? b : a;
                fout(head(description(a)) + " en " + description(b) + " hebben in het vocabulaire '" + v.naam()
                        + "' dezelfde effectieve naam '" + nb + "'; binnen hun reikwijdte is die uniek, "
                        + "hoofdletterongevoelig, zodat de weg terug naar de declaratie één antwoord heeft (WF-118)"
                        + compound(v, a) + compound(v, b), regel(place));
            }
    }

    // --- sentences -------------------------------------------------------------------------------

    static String description(Benoembaar b) {
        return switch (b.soort()) {
            case OBJECTTYPE -> "het objecttype '" + b.naam() + "'";
            case ATTRIBUUT -> "het attribuut '" + b.naam() + "' van '" + b.eigenaar() + "'";
            case KENMERK -> "het kenmerk '" + b.naam() + "' van '" + b.eigenaar() + "'";
            case FEITTYPE -> "het feittype '" + b.naam() + "'";
            case ROL -> "de rol '" + b.naam() + "' van '" + b.eigenaar() + "'";
            case ENUMERATIEWAARDE -> "de enumeratiewaarde '" + b.naam() + "' van '" + b.eigenaar() + "'";
            case PARAMETER -> "de parameter '" + b.naam() + "'";
            case VORM -> "de regel '" + b.naam() + "'";
            case REGELGROEP -> "de regelgroep '" + b.naam() + "'";
        };
    }

    private static String head(String s) { return Character.toUpperCase(s.charAt(0)) + s.substring(1); }

    /** 'a', 'a en b', 'a, b en c'. */
    private static String enumeration(List<String> parts) {
        if (parts.size() < 2) return String.join("", parts);
        return String.join(", ", parts.subList(0, parts.size() - 1)) + " en " + parts.get(parts.size() - 1);
    }

    private void fout(String message, Integer regel) {
        model.diagnostics.add(new Diagnostic(Severity.ERROR, message, regel == null ? 0 : regel));
    }
}
