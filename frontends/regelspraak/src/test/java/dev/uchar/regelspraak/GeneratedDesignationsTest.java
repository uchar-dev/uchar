package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Designatable;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * An element the opgave marker generates carries, in every vocabulary in which the marked attribute X carries a
 * designation, its own name with X replaced by that designation, taken over literally; where X carries none, neither
 * does the element (grammar/DIVERGENCES.md, "Gegenereerde elementen", "Designations"). The composed designation is a
 * designation like a written one: in the model's side table, on the wire, and judged by WF-118 to WF-120.
 */
class GeneratedDesignationsTest {

    private static RegelSpraakModel clean(String source) {
        RegelSpraakModel m = Resolver.resolve(source);
        assertEquals(List.of(), m.errors().stream().map(Diagnostic::toString).toList());
        return m;
    }

    /** The six elements of the marker on 'meldingsdatum' of Vondst, in the order of the origin table. */
    private static final List<Designatable> FAMILY = List.of(
            Designatable.attribute("Vondst", "opgegeven meldingsdatum"),
            Designatable.attribute("Vondst", "berekende meldingsdatum"),
            Designatable.characteristic("Vondst", "afwijkende opgave meldingsdatum"),
            Designatable.form("afwijking opgave meldingsdatum"),
            Designatable.form("controle opgave meldingsdatum"),
            Designatable.form("opgave boven berekening meldingsdatum"));

    /** The wire node of each element of {@link #FAMILY}. */
    private static final List<String> NODES = List.of("Attribute", "Attribute", "Characteristic", "Rule", "Consistency",
            "PrioritizedDefaults");

    /** The designation of each element in {@code vocabulary}, "-" where it carries none. */
    private static List<String> designations(RegelSpraakModel m, String vocabulary, List<Designatable> elements) {
        return elements.stream().map(e -> m.designation(vocabulary, e).orElse("-")).toList();
    }

    /** The designations the IR writes as the last field of the node {@code node} named {@code naam}, each
     *  "vocabulary: designation", in written order; empty where the node carries none. */
    private static List<String> onTheWire(String ir, String node, String name) {
        String[] r = ir.split("\n", -1);
        for (int i = 1; i + 1 < r.length; i++) {
            if (!r[i].trim().equals("\"node\": \"" + node + "\",")
                    || !r[i + 1].trim().equals("\"name\": \"" + name + "\",")) continue;
            String indent = r[i].substring(0, r[i].indexOf('"'));
            String closingLine = indent.substring(2) + "}";
            List<String> result = new ArrayList<>();
            String vocabulary = null;
            boolean within = false;
            for (int j = i; j < r.length && !r[j].equals(closingLine) && !r[j].equals(closingLine + ","); j++) {
                String t = r[j].trim();
                if (r[j].equals(indent + "\"designations\": [")) within = true;
                else if (!within) continue;
                else if (t.startsWith("\"vocabulary\": ")) vocabulary = t.substring(15, t.length() - 2);
                else if (t.startsWith("\"designation\": ")) result.add(vocabulary + ": " + t.substring(16, t.length() - 1));
            }
            return result;
        }
        throw new AssertionError("geen knoop " + node + " '" + name + "' in\n" + ir);
    }

    private static final String MESSAGE = """
        Vocabulaire cgm (taal: nl)
        Vocabulaire handboek (taal: nl)

        Objecttype de Vondst (mv: Vondsten)
            de vinddag Datum in dagen;
            de meldingsdatum %s Datum in dagen (mag opgegeven worden);

        Regel melding
            geldig altijd
                De meldingsdatum van een Vondst moet berekend worden als de vinddag van de Vondst plus 3 dagen.
        """;

    @Test void each_member_carries_in_both_vocabularies_the_compound_designation() {
        RegelSpraakModel m = clean(MESSAGE.formatted("(cgm: Datum melding vondst) (handboek: dag van de melding)"));
        assertEquals(FAMILY, List.copyOf(m.generated.keySet()));
        // the designation of the source is taken over literally, the capital in the middle of the name included
        assertEquals(List.of("opgegeven Datum melding vondst", "berekende Datum melding vondst",
                "afwijkende opgave Datum melding vondst", "afwijking opgave Datum melding vondst",
                "controle opgave Datum melding vondst", "opgave boven berekening Datum melding vondst"),
                designations(m, "cgm", FAMILY));
        assertEquals(List.of("opgegeven dag van de melding", "berekende dag van de melding",
                "afwijkende opgave dag van de melding", "afwijking opgave dag van de melding",
                "controle opgave dag van de melding", "opgave boven berekening dag van de melding"),
                designations(m, "handboek", FAMILY));
        // the way back from the composed designation finds the generated element
        assertEquals(Designatable.attribute("Vondst", "opgegeven meldingsdatum"),
                m.namedElement("cgm", Designatable.Kind.ATTRIBUUT, "Vondst", "Opgegeven datum melding vondst").orElseThrow());
        // on the wire, as the last field of each generated declaration, in the declaration order of the vocabularies
        String ir = IrEmitter.emit(m);
        for (int i = 0; i < FAMILY.size(); i++) {
            String name = FAMILY.get(i).name();
            String words = name.substring(0, name.length() - "meldingsdatum".length());
            assertEquals(List.of("cgm: " + words + "Datum melding vondst", "handboek: " + words + "dag van de melding"),
                    onTheWire(ir, NODES.get(i), name), name);
        }
        assertEquals(List.of("cgm: Datum melding vondst", "handboek: dag van de melding"),
                onTheWire(ir, "Attribute", "meldingsdatum"));
    }

    @Test void without_designation_of_the_source_in_a_vocabulary_the_member_has_none_there() {
        // the source carries a designation in cgm and none in handboek
        RegelSpraakModel m = clean(MESSAGE.formatted("(cgm: Datum melding vondst)"));
        assertEquals(List.of("opgegeven Datum melding vondst", "berekende Datum melding vondst",
                "afwijkende opgave Datum melding vondst", "afwijking opgave Datum melding vondst",
                "controle opgave Datum melding vondst", "opgave boven berekening Datum melding vondst"),
                designations(m, "cgm", FAMILY));
        assertEquals(List.of("-", "-", "-", "-", "-", "-"), designations(m, "handboek", FAMILY));
        // the effective name there is the canonical name
        assertEquals(FAMILY.stream().map(Designatable::name).toList(),
                FAMILY.stream().map(e -> m.effectiveName("handboek", e)).toList());
        String ir = IrEmitter.emit(m);
        for (int i = 0; i < FAMILY.size(); i++) {
            String name = FAMILY.get(i).name();
            assertEquals(1, onTheWire(ir, NODES.get(i), name).size(), name);
        }
        // a source without any designation: the elements carry none, and the wire writes none for them
        RegelSpraakModel plain = clean(MESSAGE.formatted(""));
        assertEquals(List.of("-", "-", "-", "-", "-", "-"), designations(plain, "cgm", FAMILY));
        assertEquals(List.of(), onTheWire(IrEmitter.emit(plain), "Attribute", "opgegeven meldingsdatum"));
    }

    /** The ERROR {@code fragment} stands on the first line of {@code source} that carries {@code merker}. */
    private static void error(String source, String merker, String fragment) {
        RegelSpraakModel m = Resolver.resolve(source);
        String[] rules = source.split("\n", -1);
        int rule = 0;
        for (int i = 0; i < rules.length; i++) if (rules[i].contains(merker)) { rule = i + 1; break; }
        assertTrue(rule > 0, "merker '" + merker + "' niet in de bron");
        final int r = rule;
        List<Diagnostic> faults = m.errors();
        assertTrue(faults.stream().anyMatch(d -> d.message().contains(fragment) && d.line() == r),
                () -> "verwacht op regel " + r + ": " + fragment + "\nkreeg: " + faults);
    }

    @Test void two_marked_attributes_with_a_designation_collide_with_their_forms() {
        String source = """
            Vocabulaire handboek

            Objecttype de Vondst (mv: Vondsten)
                de vinddag Datum in dagen;
                de meldingsdatum (handboek: dag van de melding) Datum in dagen (mag opgegeven worden);

            Objecttype de Schuit (mv: Schuiten)
                de stranddag Datum in dagen;
                de aangiftedatum (handboek: dag van de melding) Datum in dagen (mag opgegeven worden);

            Regel melding
                geldig altijd
                    De meldingsdatum van een Vondst moet berekend worden als de vinddag van de Vondst plus 3 dagen.

            Regel aangifte
                geldig altijd
                    De aangiftedatum van een Schuit moet berekend worden als de stranddag van de Schuit plus 5 dagen.
            """;
        // the three forms share one namespace; the members stand each on their own type and do not collide
        for (String words : List.of("afwijking opgave ", "controle opgave ", "opgave boven berekening "))
            error(source, "de aangiftedatum", "De regel '" + words + "meldingsdatum' en de regel '" + words
                    + "aangiftedatum' hebben in het vocabulaire 'handboek' dezelfde effectieve naam '" + words
                    + "dag van de melding'; binnen hun reikwijdte is die uniek, hoofdletterongevoelig, zodat de weg "
                    + "terug naar de declaratie één antwoord heeft (WF-118); de frontend stelde de benaming van de "
                    + "regel '" + words + "meldingsdatum' samen uit de benaming 'dag van de melding' van het "
                    + "attribuut 'meldingsdatum' van 'Vondst'; de frontend stelde de benaming van de regel '"
                    + words + "aangiftedatum' samen uit de benaming 'dag van de melding' van het attribuut "
                    + "'aangiftedatum' van 'Schuit'");
        RegelSpraakModel m = Resolver.resolve(source);
        assertEquals(3, m.errors().size(), m.errors()::toString);
    }

    @Test void a_compound_designation_collides_with_a_written_name() {
        String source = """
            Vocabulaire handboek

            Objecttype de Vondst (mv: Vondsten)
                de vinddag Datum in dagen;
                de meldingsdatum (handboek: melddag) Datum in dagen (mag opgegeven worden);
                de opgegeven melddag Datum in dagen;

            Regel melding
                geldig altijd
                    De meldingsdatum van een Vondst moet berekend worden als de vinddag van de Vondst plus 3 dagen.
            """;
        error(source, "de opgegeven melddag", "Het attribuut 'opgegeven meldingsdatum' van 'Vondst' en het attribuut "
                + "'opgegeven melddag' van 'Vondst' hebben in het vocabulaire 'handboek' dezelfde effectieve naam "
                + "'opgegeven melddag'; binnen hun reikwijdte is die uniek, hoofdletterongevoelig, zodat de weg terug "
                + "naar de declaratie één antwoord heeft (WF-118); de frontend stelde de benaming van het attribuut "
                + "'opgegeven meldingsdatum' van 'Vondst' samen uit de benaming 'melddag' van het attribuut "
                + "'meldingsdatum' van 'Vondst'");
        assertEquals(1, Resolver.resolve(source).errors().size());
    }

    private static final String STANDAARD = """
        Vocabulaire cgm (standaard)

        Objecttype de Vondst (mv: Vondsten) (cgm: VONDST)
            de vinddag (cgm: VINDDAG) Datum in dagen;
            de meldingsdatum %s Datum in dagen (mag opgegeven worden);

        Regel melding (cgm: R_MELDING)
            geldig altijd
                De meldingsdatum van een Vondst moet berekend worden als de vinddag van de Vondst plus 3 dagen.
        """;

    @Test void a_default_vocabulary_names_the_members_if_it_names_the_source() {
        // until this card WF-119 refused every generated element here: none had a place for an annotation
        RegelSpraakModel m = clean(STANDAARD.formatted("(cgm: MELDDATUM)"));
        assertEquals(List.of("opgegeven MELDDATUM", "berekende MELDDATUM", "afwijkende opgave MELDDATUM",
                "afwijking opgave MELDDATUM", "controle opgave MELDDATUM", "opgave boven berekening MELDDATUM"),
                designations(m, "cgm", FAMILY));
        // without a designation of the source, the source and each generated element are refused on the source's line
        String source = STANDAARD.formatted("");
        error(source, "de meldingsdatum", "Het attribuut 'meldingsdatum' van 'Vondst' draagt geen benaming in het "
                + "standaardvocabulaire 'cgm', dat elke declaratie noemt (WF-119)");
        for (Designatable e : FAMILY)
            error(source, "de meldingsdatum", (e.kind() == Designatable.Kind.VORM ? "De regel '" + e.name() + "'"
                    : "Het " + (e.kind() == Designatable.Kind.KENMERK ? "kenmerk" : "attribuut") + " '" + e.name()
                    + "' van 'Vondst'") + " draagt geen benaming in het standaardvocabulaire 'cgm', dat elke "
                    + "declaratie noemt (WF-119); de frontend stelt de benaming van deze declaratie samen uit die van "
                    + "het attribuut 'meldingsdatum' van 'Vondst', en dat draagt er geen");
        assertEquals(7, Resolver.resolve(source).errors().size());
    }

    @Test void a_redundant_designation_of_the_source_is_not_compounded() {
        // 'handboek' is no default: the designation spells the canonical name, and WF-120 warns it once, at the source
        RegelSpraakModel m = clean(MESSAGE.formatted("(handboek: meldingsdatum)"));
        assertEquals(List.of("-", "-", "-", "-", "-", "-"), designations(m, "handboek", FAMILY));
        assertEquals(1, m.diagnostics.stream().filter(d -> d.message().endsWith("(WF-120)")).count(),
                m.diagnostics::toString);
        // a designation that differs in case only is the vocabulary's spelling, and is composed
        RegelSpraakModel head = clean(MESSAGE.formatted("(handboek: Meldingsdatum)"));
        assertEquals("opgegeven Meldingsdatum", head.designation("handboek", FAMILY.get(0)).orElseThrow());
    }

    @Test void the_group_of_a_subtype_and_the_member_of_a_fold_get_a_designation_per_part() {
        // the group of a subtype: two names, the base rule's and the subtype's, each with a designation of its own;
        // since the card gegenereerde-namen-per-deel each part is replaced by its designation
        // (GeneratedNamesPerPartTest)
        RegelSpraakModel group = clean("""
            Vocabulaire handboek

            Eenheidsysteem Tijd
                de dag dg
                de maand mnd

            Objecttype de Jutter (mv: Jutters) (bezield) (handboek: juttersman)
                de vinddag Datum in dagen;
                de meldtermijn Datum in dagen;

            Objecttype de Strandjutter is een Jutter (mv: Strandjutters) (handboek: strandloper)
                het vergunningseinde Datum in dagen;

            Regel meldtermijn (handboek: termijn van melding)
                geldig altijd
                    De meldtermijn van een Jutter moet berekend worden als zijn vinddag plus 6 mnd
                    indien zijn vinddag gevuld is.

            Afwijking van meldtermijn: meldtermijn tot het vergunningseinde
                geldig altijd
                    De meldtermijn van een Strandjutter moet gesteld worden op zijn vergunningseinde
                    indien zijn vergunningseinde gevuld is.
            """);
        assertTrue(group.generatedFrom(Designatable.form("meldtermijn voor Strandjutter")).isPresent());
        assertEquals("termijn van melding voor strandloper",
                group.designation("handboek", Designatable.form("meldtermijn voor Strandjutter")).orElse("-"));
        // the group of the tree carries its base rule's name, and so its designation
        assertEquals("termijn van melding", group.designation("handboek", Designatable.form("meldtermijn")).orElseThrow());

        // the member of a fold with steps after the set: the member and each role are parts of its name
        RegelSpraakModel fold = clean("""
            Vocabulaire handboek

            Objecttype de Ploeg (mv: Ploegen)
                het zwaarste gewicht Numeriek (geheel getal);

            Objecttype de Jutter (mv: Jutters) (bezield)
                de startdag Datum in dagen;

            Objecttype de Vondst (mv: Vondsten)
                het gewicht (handboek: massa) Numeriek (geheel getal);

            Feittype PloegHeeftJutter
                de ploeg Ploeg
                de jutter (mv: jutters) Jutter
            Eén ploeg heeft meerdere jutters

            Feittype JutterHeeftVondst
                de jutter Jutter
                de vondst (handboek: vangst) Vondst
            Eén jutter heeft één vondst

            Regel zwaarste gewicht
                geldig altijd
                    Het zwaarste gewicht van een Ploeg moet berekend worden als de maximale waarde van het gewicht van de vondst van alle jutters van de Ploeg.
            """);
        Designatable member = Designatable.attribute("Jutter", "gewicht via vondst");
        assertTrue(fold.generatedFrom(member).isPresent(), () -> fold.generated.toString());
        assertEquals("massa via vangst", fold.designation("handboek", member).orElse("-"));
        assertEquals("massa via vangst", fold.designation("handboek", Designatable.form("gewicht via vondst")).orElse("-"));
    }
}
