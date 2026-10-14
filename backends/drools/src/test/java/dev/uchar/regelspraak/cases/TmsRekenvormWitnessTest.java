package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocumenten.PROCENT;
import static dev.uchar.regelspraak.cases.TmsDocumenten.afronding;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocumenten.begrenzing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.binair;
import static dev.uchar.regelspraak.cases.TmsDocumenten.consistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.eenheid;
import static dev.uchar.regelspraak.cases.TmsDocumenten.eenheidstelsel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getaltype;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getalwaarde;
import static dev.uchar.regelspraak.cases.TmsDocumenten.instantieMet;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.literaal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.vergelijking;
import static dev.uchar.regelspraak.cases.TmsDocumenten.verwacht;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The arithmetic value forms on the exec-model branch, each over an operand another rule derives later: a
 * rounding (RUNTIME-SEMANTICS ch. 2.5), a clamp with both bounds (ch. 2.7), a power with a whole exponent (ch. 2.2), a
 * percentage of a value (ch. 2.2, 2.6), a sum of percentages, a comparison of percentages, a percentage that a rule
 * derives taken of a value, and the restatement of units (ch. 2.6) in an additive operand, at the write boundary, in a
 * comparison and in a clamp bound. The case input puts a value on every derived operand that the derivation replaces,
 * so in an order where the reader fires first it reads that value and must give up what it computed from it.
 *
 * <p>The value document: a Vondst with {@code basis} as input and "De waarde van een Vondst moet berekend worden als
 * zijn basis maal 1,5", read by "Het afgeronde van een Vondst moet berekend worden als zijn waarde naar beneden afgerond
 * op 0 decimalen", "Het begrensde … als zijn waarde begrensd op minimaal 10 en maximaal 20", "Het kwadraat … als zijn
 * waarde tot de macht 2" and "Het aandeel … als 20% van zijn waarde", each in a document of its own with the
 * derivation, in both orders. V1 (basis 12,34, waarde 100 in the input): waarde 18,51, afgerond 18, begrensd 18,51,
 * kwadraat 342,6201, aandeel 3,702. V2 (basis 20): waarde 30, begrensd 20, kwadraat 900, aandeel 6. V3 (basis 4): waarde
 * 6, begrensd 10. V4 (no basis): "maal" counts a leeg operand as 0 (ch. 1.2), waarde 0, begrensd 10.
 *
 * <p>The percentage document: "Het tarief van een Vondst moet berekend worden als zijn grondtarief plus 5%", "De toeslag
 * … als zijn tarief van zijn basis" and "Een Vondst is duur indien zijn tarief groter is dan 20%", in every order. V1
 * (grondtarief 18, basis 12,34, tarief 50 in the input): tarief 23, toeslag 2,8382, duur. V2 (grondtarief 10, basis 20):
 * tarief 15, toeslag 3, not duur.
 *
 * <p>The unit document: a Sleepbaan with {@code grondaanloop} in m, "De aanloop … als zijn grondaanloop maal 2", "De
 * sleeplengte … als 2,5 km plus zijn aanloop" (in m), "De begrensde aanloop … als zijn aanloop begrensd op maximaal 0,2
 * km" (in m) and "Een Sleepbaan is lang indien zijn sleeplengte groter is dan 3 km", in every order. S1 (grondaanloop
 * 150, aanloop 5000 in the input): aanloop 300, sleeplengte 2800, begrensde aanloop 200, not lang. S2 (grondaanloop
 * 400): aanloop 800, sleeplengte 3300, lang. The listed fold over the aanloop is described at its row.
 *
 * <p><b>Measured</b> (2026-10-09, on every order of every document): the reference interpreter's runner passes every
 * case. The ASP runner passes the value forms and the percentage document, and declines the unit documents ("non-identical
 * units under a comparison are not translated", "expression kind not translated" for the listed fold). The exec-model
 * branch refuses a percentage in arithmetic and in a comparison ("regel (waarde buiten scope)"); the DRL-text branch
 * plays the percentage documents.
 *
 * <p><b>Red before the change</b> (measured 2026-10-09 on a copy of the branch at its claim): every row was refused on
 * its form, "een Rounding in een waarde ('afgerond op 0 decimalen')", "een Clamp in een waarde ('begrensd')", "een
 * Binary power ('power')", "een Binary over een percentage ('percentageOf')" and "('plus')", each with "de
 * truth-maintenance-tak draagt deze vorm nog niet", and the units as "een additieve operandgrens over eenheden die
 * herrekend moeten worden (m naar km)" and "een §5.8.3 operand van een gelijste vouw over eenheden die herrekend moeten
 * worden (km naar m)".
 */
class TmsRekenvormWitnessTest {

    private static final String DRIE = getaltype(false, 3, null), ZES = getaltype(false, 6, null),
            TWEE = getaltype(false, 2, null), VIER = getaltype(false, 4, null), GEHEEL = getaltype(true, 0, null);

    // ---------------- the value forms ----------------

    private static final String WAARDEMODEL = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst", List.of(
            attribuut("basis", TWEE), attribuut("waarde", DRIE), attribuut("afgerond", GEHEEL),
            attribuut("begrensd", DRIE), attribuut("kwadraat", ZES), attribuut("aandeel", DRIE)), List.of())), List.of());

    private static final String WAARDE = lezing("waarde", "Vondst");

    /** "De waarde van een Vondst moet berekend worden als zijn basis maal 1,5". */
    static final String AFLEIDING = rekenregel("waarde van een vondst", "BEREKENING", "Vondst", "waarde",
            binair("times", lezing("basis", "Vondst"), literaal("1.5", getaltype(false, 1, null)), DRIE), "null");

    static final String AFRONDING = rekenregel("afgerond van een vondst", "BEREKENING", "Vondst", "afgerond",
            afronding(WAARDE, "floor", 0, GEHEEL), "null");

    static final String BEGRENZING = rekenregel("begrensd van een vondst", "BEREKENING", "Vondst", "begrensd",
            begrenzing(WAARDE, literaal("10", GEHEEL), literaal("20", GEHEEL), DRIE), "null");

    static final String MACHT = rekenregel("kwadraat van een vondst", "BEREKENING", "Vondst", "kwadraat",
            binair("power", WAARDE, literaal("2", GEHEEL), ZES), "null");

    static final String PROCENTVAN = rekenregel("aandeel van een vondst", "BEREKENING", "Vondst", "aandeel",
            binair("percentageOf", literaal("20", getaltype(true, 0, PROCENT)), WAARDE, DRIE), "null");

    static JsonNode waardezaak(String doel, List<String> waarden) throws Exception {
        List<String> verwachtingen = new ArrayList<>();
        String[] labels = {"V1", "V2", "V3", "V4"};
        for (int i = 0; i < labels.length; i++) verwachtingen.add(waarde(labels[i], doel, waarden.get(i)));
        verwachtingen.add(consistent());
        return TmsDocumenten.zaakOp(null, List.of(
                instantieMet("V1", "Vondst", Map.of("basis", getalwaarde("12.34"), "waarde", getalwaarde("100")), List.of()),
                instantieMet("V2", "Vondst", Map.of("basis", getalwaarde("20.00")), List.of()),
                instantieMet("V3", "Vondst", Map.of("basis", getalwaarde("4.00")), List.of()),
                instantieMet("V4", "Vondst", Map.of(), List.of())), verwachtingen);
    }

    @Test void een_afronding_over_een_later_afgeleide_waarde() throws Exception {
        assertElkeVolgorde(WAARDEMODEL, List.of(AFLEIDING, AFRONDING),
                waardezaak("afgerond", List.of("18", "30", "6", "0")));
    }

    @Test void een_begrenzing_over_een_later_afgeleide_waarde() throws Exception {
        assertElkeVolgorde(WAARDEMODEL, List.of(AFLEIDING, BEGRENZING),
                waardezaak("begrensd", List.of("18.51", "20", "10", "10")));
    }

    @Test void een_macht_over_een_later_afgeleide_waarde() throws Exception {
        assertElkeVolgorde(WAARDEMODEL, List.of(AFLEIDING, MACHT),
                waardezaak("kwadraat", List.of("342.6201", "900", "36", "0")));
    }

    @Test void een_percentage_van_een_later_afgeleide_waarde() throws Exception {
        assertElkeVolgorde(WAARDEMODEL, List.of(AFLEIDING, PROCENTVAN),
                waardezaak("aandeel", List.of("3.702", "6", "1.2", "0")));
    }

    // ---------------- percentages ----------------

    private static final String PCT = getaltype(true, 0, PROCENT);

    private static final String PROCENTMODEL = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst", List.of(
            attribuut("basis", TWEE), attribuut("grondtarief", PCT), attribuut("tarief", PCT), attribuut("toeslag", VIER)),
            List.of("duur"))), List.of());

    static final List<String> PROCENTREGELS = List.of(
            rekenregel("tarief van een vondst", "BEREKENING", "Vondst", "tarief",
                    binair("plus", lezing("grondtarief", "Vondst"), literaal("5", PCT), PCT), "null"),
            rekenregel("toeslag van een vondst", "BEREKENING", "Vondst", "toeslag",
                    binair("percentageOf", lezing("tarief", "Vondst"), lezing("basis", "Vondst"), VIER), "null"),
            kenmerkregel("duur bij tarief", "Vondst", "duur",
                    vergelijking("greater", lezing("tarief", "Vondst"), literaal("20", PCT))));

    static JsonNode procentzaak() throws Exception {
        return TmsDocumenten.zaakOp(null, List.of(
                instantieMet("V1", "Vondst", Map.of("grondtarief", getalwaarde("18"), "basis", getalwaarde("12.34"),
                        "tarief", getalwaarde("50")), List.of()),
                instantieMet("V2", "Vondst", Map.of("grondtarief", getalwaarde("10"), "basis", getalwaarde("20.00")),
                        List.of())),
                List.of(waarde("V1", "tarief", "23"), waarde("V1", "toeslag", "2.8382"), verwacht("V1", "duur", true),
                        waarde("V2", "tarief", "15"), waarde("V2", "toeslag", "3"), verwacht("V2", "duur", false),
                        consistent()));
    }

    @Test void percentages_rekenen_en_vergelijken_over_een_later_afgeleid_tarief() throws Exception {
        assertElkeVolgorde(PROCENTMODEL, PROCENTREGELS, procentzaak());
    }

    // ---------------- units ----------------

    private static final String METER = eenheid("m", "Afstand", "m", 1, 1), KILOMETER = eenheid("km", "Afstand", "m", 1000, 1);
    private static final String M = getaltype(true, 0, METER);

    static final String EENHEIDMODEL = TmsDocumenten.modelMet(List.of(TmsDocumenten.objecttype("Sleepbaan", List.of(
            attribuut("grondaanloop", M), attribuut("aanloop", M), attribuut("sleeplengte", M),
            attribuut("begrensde aanloop", M), attribuut("ruimste aanloop", M)), List.of("lang"))), List.of(), List.of(),
            List.of(eenheidstelsel("Afstand", List.of(METER, KILOMETER))));

    static final List<String> EENHEIDREGELS = List.of(
            rekenregel("aanloop van een sleepbaan", "BEREKENING", "Sleepbaan", "aanloop",
                    binair("times", lezing("grondaanloop", "Sleepbaan"), literaal("2", GEHEEL), M), "null"),
            rekenregel("sleeplengte van een sleepbaan", "BEREKENING", "Sleepbaan", "sleeplengte",
                    binair("plus", literaal("2.5", getaltype(false, 1, KILOMETER)), lezing("aanloop", "Sleepbaan"),
                            getaltype(false, 1, KILOMETER)), "null"),
            rekenregel("begrensde aanloop van een sleepbaan", "BEREKENING", "Sleepbaan", "begrensde aanloop",
                    begrenzing(lezing("aanloop", "Sleepbaan"), "null", literaal("0.2", getaltype(false, 1, KILOMETER)), M),
                    "null"),
            kenmerkregel("lang bij sleeplengte", "Sleepbaan", "lang",
                    vergelijking("greater", lezing("sleeplengte", "Sleepbaan"), literaal("3", getaltype(true, 0, KILOMETER)))));

    static JsonNode eenheidzaak() throws Exception {
        return TmsDocumenten.zaakOp(null, List.of(
                instantieMet("S1", "Sleepbaan", Map.of("grondaanloop", getalwaarde("150"), "aanloop", getalwaarde("5000")),
                        List.of()),
                instantieMet("S2", "Sleepbaan", Map.of("grondaanloop", getalwaarde("400")), List.of())),
                List.of(waarde("S1", "aanloop", "300"), waarde("S1", "sleeplengte", "2800"),
                        waarde("S1", "begrensde aanloop", "200"), verwacht("S1", "lang", false),
                        waarde("S2", "aanloop", "800"), waarde("S2", "sleeplengte", "3300"),
                        waarde("S2", "begrensde aanloop", "200"), verwacht("S2", "lang", true), consistent()));
    }

    @Test void eenheden_worden_herrekend_over_een_later_afgeleide_aanloop() throws Exception {
        assertElkeVolgorde(EENHEIDMODEL, EENHEIDREGELS, eenheidzaak());
    }

    /** "De ruimste aanloop van een Sleepbaan moet berekend worden als de maximale van zijn aanloop en 0,5 km" (in m):
     *  every operand after the first restated into the first one's unit, the unit of the result (§4.6 Eenheid1,
     *  ch. 2.6). S1: 500 (the aanloop 300 m against 0,5 km; 5000 m on the way, from the input), S2: 800. */
    @Test void een_gelijste_vouw_herrekent_haar_operanden_over_een_later_afgeleide_aanloop() throws Exception {
        String ruimste = rekenregel("ruimste aanloop van een sleepbaan", "BEREKENING", "Sleepbaan", "ruimste aanloop",
                TmsDocumenten.gelijsteVouw("maximum", List.of(lezing("aanloop", "Sleepbaan"),
                        literaal("0.5", getaltype(false, 1, KILOMETER))), M, "de maximale van zijn aanloop en 0,5 km"),
                "null");
        assertElkeVolgorde(EENHEIDMODEL, List.of(EENHEIDREGELS.get(0), ruimste), TmsDocumenten.zaakOp(null, List.of(
                instantieMet("S1", "Sleepbaan", Map.of("grondaanloop", getalwaarde("150"), "aanloop", getalwaarde("5000")),
                        List.of()),
                instantieMet("S2", "Sleepbaan", Map.of("grondaanloop", getalwaarde("400")), List.of())),
                List.of(waarde("S1", "ruimste aanloop", "500"), waarde("S2", "ruimste aanloop", "800"), consistent())));
    }

    /** "De totale lengte van een Sleepbaan moet berekend worden als de som van de lengte van alle baandelen van de
     *  Sleepbaan, of 1 km als die er niet zijn" (in m), beside "De lengte van een Baandeel moet berekend worden als zijn
     *  grondlengte": the fallback is restated into the unit of the folded member (ch. 2.6) and then, with the sum, into
     *  the target's. S1 (G1 300, G2 200 with a lengte 999 in the input): 500; S2 (no baandeel): 1000; S3 (G3 without a
     *  grondlengte, so its lengte stays leeg and the sum has no filled member): 1000. Measured 2026-10-09: the reference
     *  interpreter's runner passes the case in both orders; the ASP runner declines it ("sum fallback other than 0 is
     *  not translated yet"); the exec-model branch and the DRL-text branch write the fallback unrestated, 1 where 1000
     *  is expected. <b>Red</b> (2026-10-09) at the claim of the card: "een §5.8.2 terugvalwaarde over eenheden die
     *  herrekend moeten worden (km naar m): de truth-maintenance-tak draagt deze vorm nog niet". */
    @Test void de_terugvalwaarde_van_een_som_wordt_herrekend_naast_later_afgeleide_lengtes() throws Exception {
        String model = TmsDocumenten.modelMet(List.of(
                TmsDocumenten.objecttype("Sleepbaan", List.of(attribuut("totale lengte", M)), List.of()),
                TmsDocumenten.objecttype("Baandeel", List.of(attribuut("grondlengte", M), attribuut("lengte", M)),
                        List.of())),
                List.of(TmsDocumenten.feittypeEenNaarVeel("baanindeling", "baan", "Sleepbaan", "baandeel", "Baandeel",
                        "baandelen")),
                List.of(), List.of(eenheidstelsel("Afstand", List.of(METER, KILOMETER))));
        String lengte = rekenregel("lengte van een baandeel", "BEREKENING", "Baandeel", "lengte",
                lezing("grondlengte", "Baandeel"), "null");
        String totaal = rekenregel("totale lengte van een sleepbaan", "BEREKENING", "Sleepbaan", "totale lengte",
                TmsDocumenten.aggregatie("sum", lezing("lengte", "Baandeel"), "Baandeel",
                        TmsDocumenten.bronrol("baanindeling", "baandeel"), "null",
                        literaal("1", getaltype(true, 0, KILOMETER)), M,
                        "de som van de lengte van alle baandelen van de Sleepbaan of 1 km als die er niet zijn"), "null");
        assertElkeVolgorde(model, List.of(lengte, totaal), TmsDocumenten.zaakOp(null, List.of(
                TmsDocumenten.instantieMetRollen("S1", "Sleepbaan", Map.of(), List.of(),
                        Map.of("baandelen", List.of("G1", "G2"))),
                TmsDocumenten.instantieMetRollen("S2", "Sleepbaan", Map.of(), List.of(), Map.of()),
                TmsDocumenten.instantieMetRollen("S3", "Sleepbaan", Map.of(), List.of(), Map.of("baandelen", List.of("G3"))),
                TmsDocumenten.instantieMetRollen("G1", "Baandeel", Map.of("grondlengte", "300"), List.of(), Map.of()),
                TmsDocumenten.instantieMetRollen("G2", "Baandeel", Map.of("grondlengte", "200", "lengte", "999"), List.of(),
                        Map.of()),
                TmsDocumenten.instantieMetRollen("G3", "Baandeel", Map.of(), List.of(), Map.of())),
                List.of(waarde("S1", "totale lengte", "500"), waarde("S2", "totale lengte", "1000"),
                        waarde("S3", "totale lengte", "1000"), consistent())));
    }

    // ---------------- helpers ----------------

    /** Every order of {@code regels} in the document of {@code model} plays {@code zaak}. */
    static void assertElkeVolgorde(String model, List<String> regels, JsonNode zaak) throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(regels)) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            uitkomsten.add(namen + " " + TmsFoutWitnessTest.speel(TmsDocumenten.document(model, volgorde), zaak,
                    DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(namen + " gespeeld");
        }
        assertEquals(verwacht, uitkomsten);
    }
}
