package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Benoembaar;
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
class GegenereerdeBenamingenTest {

    private static RegelSpraakModel schoon(String bron) {
        RegelSpraakModel m = Resolver.resolve(bron);
        assertEquals(List.of(), m.errors().stream().map(Diagnostic::toString).toList());
        return m;
    }

    /** The six elements of the marker on 'meldingsdatum' of Vondst, in the order of the origin table. */
    private static final List<Benoembaar> FAMILIE = List.of(
            Benoembaar.attribuut("Vondst", "opgegeven meldingsdatum"),
            Benoembaar.attribuut("Vondst", "berekende meldingsdatum"),
            Benoembaar.kenmerk("Vondst", "afwijkende opgave meldingsdatum"),
            Benoembaar.vorm("afwijking opgave meldingsdatum"),
            Benoembaar.vorm("controle opgave meldingsdatum"),
            Benoembaar.vorm("opgave boven berekening meldingsdatum"));

    /** The wire node of each element of {@link #FAMILIE}. */
    private static final List<String> KNOPEN = List.of("Attribute", "Attribute", "Characteristic", "Rule", "Consistency",
            "PrioritizedDefaults");

    /** The designation of each element in {@code vocabulaire}, "-" where it carries none. */
    private static List<String> benamingen(RegelSpraakModel m, String vocabulaire, List<Benoembaar> elementen) {
        return elementen.stream().map(e -> m.benaming(vocabulaire, e).orElse("-")).toList();
    }

    /** The designations the IR writes as the last field of the node {@code node} named {@code naam}, each
     *  "vocabulary: designation", in written order; empty where the node carries none. */
    private static List<String> opDeWire(String ir, String node, String naam) {
        String[] r = ir.split("\n", -1);
        for (int i = 1; i + 1 < r.length; i++) {
            if (!r[i].trim().equals("\"node\": \"" + node + "\",")
                    || !r[i + 1].trim().equals("\"name\": \"" + naam + "\",")) continue;
            String rand = r[i].substring(0, r[i].indexOf('"'));
            String sluit = rand.substring(2) + "}";
            List<String> uit = new ArrayList<>();
            String vocabulaire = null;
            boolean binnen = false;
            for (int j = i; j < r.length && !r[j].equals(sluit) && !r[j].equals(sluit + ","); j++) {
                String t = r[j].trim();
                if (r[j].equals(rand + "\"designations\": [")) binnen = true;
                else if (!binnen) continue;
                else if (t.startsWith("\"vocabulary\": ")) vocabulaire = t.substring(15, t.length() - 2);
                else if (t.startsWith("\"designation\": ")) uit.add(vocabulaire + ": " + t.substring(16, t.length() - 1));
            }
            return uit;
        }
        throw new AssertionError("geen knoop " + node + " '" + naam + "' in\n" + ir);
    }

    private static final String MELDING = """
        Vocabulaire cgm (taal: nl)
        Vocabulaire handboek (taal: nl)

        Objecttype de Vondst (mv: Vondsten)
            de vinddag Datum in dagen;
            de meldingsdatum %s Datum in dagen (mag opgegeven worden);

        Regel melding
            geldig altijd
                De meldingsdatum van een Vondst moet berekend worden als de vinddag van de Vondst plus 3 dagen.
        """;

    @Test void elk_lid_draagt_in_beide_vocabulaires_de_samengestelde_benaming() {
        RegelSpraakModel m = schoon(MELDING.formatted("(cgm: Datum melding vondst) (handboek: dag van de melding)"));
        assertEquals(FAMILIE, List.copyOf(m.gegenereerd.keySet()));
        // the designation of the source is taken over literally, the capital in the middle of the name included
        assertEquals(List.of("opgegeven Datum melding vondst", "berekende Datum melding vondst",
                "afwijkende opgave Datum melding vondst", "afwijking opgave Datum melding vondst",
                "controle opgave Datum melding vondst", "opgave boven berekening Datum melding vondst"),
                benamingen(m, "cgm", FAMILIE));
        assertEquals(List.of("opgegeven dag van de melding", "berekende dag van de melding",
                "afwijkende opgave dag van de melding", "afwijking opgave dag van de melding",
                "controle opgave dag van de melding", "opgave boven berekening dag van de melding"),
                benamingen(m, "handboek", FAMILIE));
        // the way back from the composed designation finds the generated element
        assertEquals(Benoembaar.attribuut("Vondst", "opgegeven meldingsdatum"),
                m.benoemd("cgm", Benoembaar.Soort.ATTRIBUUT, "Vondst", "Opgegeven datum melding vondst").orElseThrow());
        // on the wire, as the last field of each generated declaration, in the declaration order of the vocabularies
        String ir = IrEmitter.emit(m);
        for (int i = 0; i < FAMILIE.size(); i++) {
            String naam = FAMILIE.get(i).naam();
            String woorden = naam.substring(0, naam.length() - "meldingsdatum".length());
            assertEquals(List.of("cgm: " + woorden + "Datum melding vondst", "handboek: " + woorden + "dag van de melding"),
                    opDeWire(ir, KNOPEN.get(i), naam), naam);
        }
        assertEquals(List.of("cgm: Datum melding vondst", "handboek: dag van de melding"),
                opDeWire(ir, "Attribute", "meldingsdatum"));
    }

    @Test void zonder_benaming_van_de_bron_in_een_vocabulaire_heeft_het_lid_daar_geen() {
        // the source carries a designation in cgm and none in handboek
        RegelSpraakModel m = schoon(MELDING.formatted("(cgm: Datum melding vondst)"));
        assertEquals(List.of("opgegeven Datum melding vondst", "berekende Datum melding vondst",
                "afwijkende opgave Datum melding vondst", "afwijking opgave Datum melding vondst",
                "controle opgave Datum melding vondst", "opgave boven berekening Datum melding vondst"),
                benamingen(m, "cgm", FAMILIE));
        assertEquals(List.of("-", "-", "-", "-", "-", "-"), benamingen(m, "handboek", FAMILIE));
        // the effective name there is the canonical name
        assertEquals(FAMILIE.stream().map(Benoembaar::naam).toList(),
                FAMILIE.stream().map(e -> m.effectieveNaam("handboek", e)).toList());
        String ir = IrEmitter.emit(m);
        for (int i = 0; i < FAMILIE.size(); i++) {
            String naam = FAMILIE.get(i).naam();
            assertEquals(1, opDeWire(ir, KNOPEN.get(i), naam).size(), naam);
        }
        // a source without any designation: the elements carry none, and the wire writes none for them
        RegelSpraakModel kaal = schoon(MELDING.formatted(""));
        assertEquals(List.of("-", "-", "-", "-", "-", "-"), benamingen(kaal, "cgm", FAMILIE));
        assertEquals(List.of(), opDeWire(IrEmitter.emit(kaal), "Attribute", "opgegeven meldingsdatum"));
    }

    /** The ERROR {@code fragment} stands on the first line of {@code bron} that carries {@code merker}. */
    private static void fout(String bron, String merker, String fragment) {
        RegelSpraakModel m = Resolver.resolve(bron);
        String[] regels = bron.split("\n", -1);
        int regel = 0;
        for (int i = 0; i < regels.length; i++) if (regels[i].contains(merker)) { regel = i + 1; break; }
        assertTrue(regel > 0, "merker '" + merker + "' niet in de bron");
        final int r = regel;
        List<Diagnostic> fouten = m.errors();
        assertTrue(fouten.stream().anyMatch(d -> d.message().contains(fragment) && d.line() == r),
                () -> "verwacht op regel " + r + ": " + fragment + "\nkreeg: " + fouten);
    }

    @Test void twee_gemarkeerde_attributen_met_een_benaming_botsen_met_hun_vormen() {
        String bron = """
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
        for (String woorden : List.of("afwijking opgave ", "controle opgave ", "opgave boven berekening "))
            fout(bron, "de aangiftedatum", "De regel '" + woorden + "meldingsdatum' en de regel '" + woorden
                    + "aangiftedatum' hebben in het vocabulaire 'handboek' dezelfde effectieve naam '" + woorden
                    + "dag van de melding'; binnen hun reikwijdte is die uniek, hoofdletterongevoelig, zodat de weg "
                    + "terug naar de declaratie één antwoord heeft (WF-118); de frontend stelde de benaming van de "
                    + "regel '" + woorden + "meldingsdatum' samen uit de benaming 'dag van de melding' van het "
                    + "attribuut 'meldingsdatum' van 'Vondst'; de frontend stelde de benaming van de regel '"
                    + woorden + "aangiftedatum' samen uit de benaming 'dag van de melding' van het attribuut "
                    + "'aangiftedatum' van 'Schuit'");
        RegelSpraakModel m = Resolver.resolve(bron);
        assertEquals(3, m.errors().size(), m.errors()::toString);
    }

    @Test void een_samengestelde_benaming_botst_met_een_geschreven_naam() {
        String bron = """
            Vocabulaire handboek

            Objecttype de Vondst (mv: Vondsten)
                de vinddag Datum in dagen;
                de meldingsdatum (handboek: melddag) Datum in dagen (mag opgegeven worden);
                de opgegeven melddag Datum in dagen;

            Regel melding
                geldig altijd
                    De meldingsdatum van een Vondst moet berekend worden als de vinddag van de Vondst plus 3 dagen.
            """;
        fout(bron, "de opgegeven melddag", "Het attribuut 'opgegeven meldingsdatum' van 'Vondst' en het attribuut "
                + "'opgegeven melddag' van 'Vondst' hebben in het vocabulaire 'handboek' dezelfde effectieve naam "
                + "'opgegeven melddag'; binnen hun reikwijdte is die uniek, hoofdletterongevoelig, zodat de weg terug "
                + "naar de declaratie één antwoord heeft (WF-118); de frontend stelde de benaming van het attribuut "
                + "'opgegeven meldingsdatum' van 'Vondst' samen uit de benaming 'melddag' van het attribuut "
                + "'meldingsdatum' van 'Vondst'");
        assertEquals(1, Resolver.resolve(bron).errors().size());
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

    @Test void een_standaardvocabulaire_noemt_de_leden_als_het_de_bron_noemt() {
        // until this card WF-119 refused every generated element here: none had a place for an annotation
        RegelSpraakModel m = schoon(STANDAARD.formatted("(cgm: MELDDATUM)"));
        assertEquals(List.of("opgegeven MELDDATUM", "berekende MELDDATUM", "afwijkende opgave MELDDATUM",
                "afwijking opgave MELDDATUM", "controle opgave MELDDATUM", "opgave boven berekening MELDDATUM"),
                benamingen(m, "cgm", FAMILIE));
        // without a designation of the source, the source and each generated element are refused on the source's line
        String bron = STANDAARD.formatted("");
        fout(bron, "de meldingsdatum", "Het attribuut 'meldingsdatum' van 'Vondst' draagt geen benaming in het "
                + "standaardvocabulaire 'cgm', dat elke declaratie noemt (WF-119)");
        for (Benoembaar e : FAMILIE)
            fout(bron, "de meldingsdatum", (e.soort() == Benoembaar.Soort.VORM ? "De regel '" + e.naam() + "'"
                    : "Het " + (e.soort() == Benoembaar.Soort.KENMERK ? "kenmerk" : "attribuut") + " '" + e.naam()
                    + "' van 'Vondst'") + " draagt geen benaming in het standaardvocabulaire 'cgm', dat elke "
                    + "declaratie noemt (WF-119); de frontend stelt de benaming van deze declaratie samen uit die van "
                    + "het attribuut 'meldingsdatum' van 'Vondst', en dat draagt er geen");
        assertEquals(7, Resolver.resolve(bron).errors().size());
    }

    @Test void een_overbodige_benaming_van_de_bron_wordt_niet_samengesteld() {
        // 'handboek' is no default: the designation spells the canonical name, and WF-120 warns it once, at the source
        RegelSpraakModel m = schoon(MELDING.formatted("(handboek: meldingsdatum)"));
        assertEquals(List.of("-", "-", "-", "-", "-", "-"), benamingen(m, "handboek", FAMILIE));
        assertEquals(1, m.diagnostics.stream().filter(d -> d.message().endsWith("(WF-120)")).count(),
                m.diagnostics::toString);
        // a designation that differs in case only is the vocabulary's spelling, and is composed
        RegelSpraakModel hoofd = schoon(MELDING.formatted("(handboek: Meldingsdatum)"));
        assertEquals("opgegeven Meldingsdatum", hoofd.benaming("handboek", FAMILIE.get(0)).orElseThrow());
    }

    @Test void de_andere_gegenereerde_vormen_krijgen_geen_benaming() {
        // the group of a subtype: two names, the base rule's and the subtype's, each with a designation of its own
        RegelSpraakModel groep = schoon("""
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
        assertTrue(groep.gegenereerdUit(Benoembaar.vorm("meldtermijn voor Strandjutter")).isPresent());
        assertEquals("-", groep.benaming("handboek", Benoembaar.vorm("meldtermijn voor Strandjutter")).orElse("-"));
        // the group of the tree carries its base rule's name, and so its designation
        assertEquals("termijn van melding", groep.benaming("handboek", Benoembaar.vorm("meldtermijn")).orElseThrow());

        // the member of a fold with steps after the set: the member and each role are parts of its name
        RegelSpraakModel vouw = schoon("""
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
        Benoembaar lid = Benoembaar.attribuut("Jutter", "gewicht via vondst");
        assertTrue(vouw.gegenereerdUit(lid).isPresent(), () -> vouw.gegenereerd.toString());
        assertEquals("-", vouw.benaming("handboek", lid).orElse("-"));
        assertEquals("-", vouw.benaming("handboek", Benoembaar.vorm("gewicht via vondst")).orElse("-"));
    }
}
