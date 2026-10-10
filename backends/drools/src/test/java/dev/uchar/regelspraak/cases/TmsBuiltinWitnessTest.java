package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.kern.Namen;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocumenten.DATUM;
import static dev.uchar.regelspraak.cases.TmsDocumenten.DATUMTIJD;
import static dev.uchar.regelspraak.cases.TmsDocumenten.TEKST;
import static dev.uchar.regelspraak.cases.TmsDocumenten.afronding;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocumenten.binair;
import static dev.uchar.regelspraak.cases.TmsDocumenten.binderlezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.builtin;
import static dev.uchar.regelspraak.cases.TmsDocumenten.consistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.datumwaarde;
import static dev.uchar.regelspraak.cases.TmsDocumenten.eenheid;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getaltype;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getalwaarde;
import static dev.uchar.regelspraak.cases.TmsDocumenten.inconsistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.instantieMet;
import static dev.uchar.regelspraak.cases.TmsDocumenten.keuze;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.let;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.literaal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.samengesteld;
import static dev.uchar.regelspraak.cases.TmsDocumenten.tekstdeel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.tekstexpressie;
import static dev.uchar.regelspraak.cases.TmsDocumenten.tekstreeks;
import static dev.uchar.regelspraak.cases.TmsDocumenten.tekstwaarde;
import static dev.uchar.regelspraak.cases.TmsDocumenten.verwacht;
import static dev.uchar.regelspraak.cases.TmsDocumenten.vergelijking;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waarde;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waardeJson;
import static dev.uchar.regelspraak.cases.TmsRekenvormWitnessTest.assertElkeVolgorde;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The builtin calls and the remaining value and condition forms on the truth-maintenance branch, each over an operand
 * another rule derives later, in both orders of the two rules (the case input puts a value on the derived operand that
 * the derivation replaces): the day-precision extractors, the duration and the absolute duration in a calendar unit and
 * easterSunday (RUNTIME-SEMANTICS ch. 3.8); the absolute value and the square root under its rounding (ch. 2.2), with
 * the §9.5 inconsistency of a negative radicand (ch. 1.2); a text composition and a concatenation of texts (ch. 1.2);
 * the eleven test and the exact-digits test; the sub-day extractors, dateWith and dateTimeWith; an {@code Unresolved}
 * node in a value and in a condition (ch. 5.7, WF-044), which faults only where the evaluation reaches it; and a
 * dimension cell and a fold over a dimension selection, read off the case input.
 *
 * <p><b>Measured</b> (2026-10-09, the reference interpreter's runner, the ASP runner and the exec-model branch on every
 * order of every document with a case): the reference interpreter passes the three date extractors, the duration,
 * easterSunday, the absolute value, the text composition and the concatenation, and the square root where the
 * oppervlak rule comes first. Where the zijde comes first it reads the input oppervlak −9 and keeps the §6.6 deposit
 * made there ("MISMATCH: expected consistent, but the channel carries: zijde van een vondst × V1"), the deposit of an
 * intermediate state that the owner's decision of 2026-10-09 (PLAN §9) does not count. It faults on the Unresolved
 * nodes of V2 and V3 in this witness's sentences, passes the input waarde 500 in every order, and faults on the input
 * waarde 3 in the three orders where the heffing reads it before the waarde is derived, the fault of an intermediate
 * state of that decision. It declines the eleven test and the exact-digit test ("predicate kind not implemented in this
 * card"), the sub-day extractors, dateWith and dateTimeWith ("function kind not implemented in this card"), and the
 * dimension forms, as RUNTIME-SEMANTICS ch. 5.11 records; for those the expectations are the exec-model branch's, whose
 * runtime helpers this branch calls, and that branch played every one of those documents that has a case (the
 * dimension row fires beans set by hand, since no case reader carries dimensioned input). The ASP runner passes the
 * date extractors, the duration and easterSunday, and declines the other forms at translation ("absoluteValue is not
 * translated", "rounded root is not translated", "condition kind not translated", a computed text or datum-tijd target
 * that the case also supplies, "Unresolved reached in translation").
 *
 * <p><b>Red before the change</b> (measured 2026-10-09 on a copy of the branch at its claim): every row was refused on
 * its form, among them "een BuiltinCall in een waarde ('de absolute waarde van zijn saldo'): buiten de proef van de
 * truth-maintenance-tak", "een Rounding in een waarde ('afgerond op 2 decimalen'): de truth-maintenance-tak draagt deze
 * vorm nog niet", "een TextComposition in een waarde ('de reeks van …')", "een Predicate elevenTest ('elevenTest')",
 * "een Let in een waarde ('k is …')" and "een DimensionCellRead in een waarde ('zijn omzet in k2')"; the row of the
 * text of a number expected its own sentence and got the TextComposition's.
 */
class TmsBuiltinWitnessTest {

    private static final String GEHEEL = getaltype(true, 0, null), TWEE = getaltype(false, 2, null),
            DRIE = getaltype(false, 3, null);

    // ---------------- the date builtins ----------------

    private static final String DATUMMODEL = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst", List.of(
            attribuut("vinddatum", DATUM), attribuut("peildatum", DATUM), attribuut("peiljaar", GEHEEL),
            attribuut("peilmaand", GEHEEL), attribuut("peildag", GEHEEL),
            attribuut("weken", getaltype(true, 0, eenheid("weken"))),
            attribuut("dagen", getaltype(true, 0, eenheid("dagen"))), attribuut("paasdatum", DATUM)), List.of())), List.of());

    private static final String PEILDATUM = lezing("peildatum", "Vondst"), VINDDATUM = lezing("vinddatum", "Vondst");

    /** "De peildatum van een Vondst moet berekend worden als zijn vinddatum plus 10 dagen". */
    static final String PEIL = rekenregel("peildatum van een vondst", "BEREKENING", "Vondst", "peildatum",
            binair("plus", VINDDATUM, literaal("10", getaltype(true, 0, eenheid("dagen"))), DATUM), "null");

    static JsonNode datumzaak(String doel, String v1, String v2) throws Exception {
        return TmsDocumenten.zaakOp(null, List.of(
                instantieMet("V1", "Vondst", Map.of("vinddatum", datumwaarde("2026-03-25"),
                        "peildatum", datumwaarde("2000-01-01")), List.of()),
                instantieMet("V2", "Vondst", Map.of("vinddatum", datumwaarde("2024-12-28")), List.of()),
                instantieMet("V3", "Vondst", Map.of(), List.of())),
                List.of(waardeJson("V1", doel, v1), waardeJson("V2", doel, v2), waarde("V3", doel, null), consistent()));
    }

    private static String extractor(String naam, String kind, String doel) {
        return rekenregel(naam, "BEREKENING", "Vondst", doel, builtin(kind, List.of(PEILDATUM), GEHEEL,
                kind + " van zijn peildatum"), "null");
    }

    @Test void jaar_maand_en_dag_uit_een_later_afgeleide_datum() throws Exception {
        assertElkeVolgorde(DATUMMODEL, List.of(PEIL, extractor("peiljaar", "yearOf", "peiljaar")),
                datumzaak("peiljaar", getalwaarde("2026"), getalwaarde("2025")));
        assertElkeVolgorde(DATUMMODEL, List.of(PEIL, extractor("peilmaand", "monthOf", "peilmaand")),
                datumzaak("peilmaand", getalwaarde("4"), getalwaarde("1")));
        assertElkeVolgorde(DATUMMODEL, List.of(PEIL, extractor("peildag", "dayOf", "peildag")),
                datumzaak("peildag", getalwaarde("4"), getalwaarde("7")));
    }

    @Test void de_tijdsduur_tot_een_later_afgeleide_datum() throws Exception {
        String weken = rekenregel("weken van een vondst", "BEREKENING", "Vondst", "weken", builtin("duration",
                List.of(VINDDATUM, PEILDATUM), getaltype(true, 0, eenheid("weken")),
                "de tijdsduur van zijn vinddatum tot zijn peildatum in hele weken"), "null");
        assertElkeVolgorde(DATUMMODEL, List.of(PEIL, weken), datumzaak("weken", getalwaarde("1"), getalwaarde("1")));
        String dagen = rekenregel("dagen van een vondst", "BEREKENING", "Vondst", "dagen", builtin("absoluteDuration",
                List.of(PEILDATUM, VINDDATUM), getaltype(true, 0, eenheid("dagen")),
                "de absolute tijdsduur van zijn peildatum tot zijn vinddatum in hele dagen"), "null");
        assertElkeVolgorde(DATUMMODEL, List.of(PEIL, dagen), datumzaak("dagen", getalwaarde("10"), getalwaarde("10")));
    }

    @Test void de_eerste_paasdag_van_het_jaar_uit_een_later_afgeleide_datum() throws Exception {
        String pasen = rekenregel("paasdatum van een vondst", "BEREKENING", "Vondst", "paasdatum", builtin("easterSunday",
                List.of(builtin("yearOf", List.of(PEILDATUM), GEHEEL, "het jaar uit zijn peildatum")), DATUM,
                "de eerste paasdag van het jaar uit zijn peildatum"), "null");
        assertElkeVolgorde(DATUMMODEL, List.of(PEIL, pasen),
                datumzaak("paasdatum", datumwaarde("2026-04-05"), datumwaarde("2025-04-20")));
    }

    // ---------------- the absolute value and the square root ----------------

    private static final String GETALMODEL = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst", List.of(
            attribuut("basis", TWEE), attribuut("saldo", TWEE), attribuut("afstand", TWEE), attribuut("oppervlak", TWEE),
            attribuut("zijde", TWEE)), List.of())), List.of());

    @Test void de_absolute_waarde_van_een_later_afgeleid_saldo() throws Exception {
        String saldo = rekenregel("saldo van een vondst", "BEREKENING", "Vondst", "saldo",
                binair("minus", lezing("basis", "Vondst"), literaal("50.00", TWEE), TWEE), "null");
        String afstand = rekenregel("afstand van een vondst", "BEREKENING", "Vondst", "afstand",
                builtin("absoluteValue", List.of(lezing("saldo", "Vondst")), TWEE, "de absolute waarde van zijn saldo"),
                "null");
        assertElkeVolgorde(GETALMODEL, List.of(saldo, afstand), TmsDocumenten.zaakOp(null, List.of(
                instantieMet("V1", "Vondst", Map.of("basis", getalwaarde("20.00"), "saldo", getalwaarde("7.00")), List.of()),
                instantieMet("V2", "Vondst", Map.of("basis", getalwaarde("80.00")), List.of()),
                instantieMet("V3", "Vondst", Map.of(), List.of())),
                List.of(waarde("V1", "afstand", "30.00"), waarde("V2", "afstand", "30.00"),
                        waarde("V3", "afstand", "50.00"), consistent())));
    }

    static final String OPPERVLAK = rekenregel("oppervlak van een vondst", "BEREKENING", "Vondst", "oppervlak",
            binair("times", lezing("basis", "Vondst"), literaal("2", GEHEEL), TWEE), "null");

    static final String ZIJDE = rekenregel("zijde van een vondst", "BEREKENING", "Vondst", "zijde",
            afronding(builtin("squareRoot", List.of(lezing("oppervlak", "Vondst")), TWEE, "de wortel van zijn oppervlak"),
                    "halfAwayFromZero", 2, TWEE), "null");

    @Test void de_wortel_van_een_later_afgeleid_oppervlak_onder_zijn_afronding() throws Exception {
        assertElkeVolgorde(GETALMODEL, List.of(OPPERVLAK, ZIJDE), TmsDocumenten.zaakOp(null, List.of(
                instantieMet("V1", "Vondst", Map.of("basis", getalwaarde("20.00"), "oppervlak", getalwaarde("-9.00")),
                        List.of()),
                instantieMet("V2", "Vondst", Map.of("basis", getalwaarde("8.00")), List.of())),
                List.of(waarde("V1", "zijde", "6.32"), waarde("V2", "zijde", "4.00"), consistent())));
        assertElkeVolgorde(GETALMODEL, List.of(OPPERVLAK, ZIJDE), TmsDocumenten.zaakOp(null, List.of(
                instantieMet("V3", "Vondst", Map.of("basis", getalwaarde("-2.00")), List.of())),
                List.of(waarde("V3", "zijde", null), inconsistent("zijde van een vondst", "V3"))));
    }

    /** "De zijde van een Vondst moet berekend worden als de wortel van zijn oppervlak", without a rounding: exact where
     *  the oppervlak is the square of a rational, as the reference interpreter computes it (RUNTIME-SEMANTICS ch. 2.2),
     *  and that executor's fault where it is not, since an irrational root exists only under the rounding §13.4.17.13
     *  mandates. V1 (basis 2, oppervlak 2 in the input, so √2 on the way where the zijde comes first): zijde 2; V2
     *  (basis 8): 4; V3 (basis 0,08): 0,4. V4 (basis 1): √2 at the fixpoint, the fault. V5 (basis −2): leeg, with the
     *  §6.6 deposit. Measured 2026-10-09: the reference interpreter's runner gives these outcomes where the oppervlak
     *  rule comes first, and in the other order faults on V1's transient √2, the fault of an intermediate state that
     *  the owner's decision of 2026-10-09 (PLAN §9) does not count; the ASP runner declines the root ("squareRoot is
     *  not translated"); the exec-model branch plays V4 without a fault, from its approximate root, where the reference
     *  interpreter faults. <b>Red</b> (2026-10-09) at the claim of the card, "een BuiltinCall in een waarde ('de wortel
     *  van zijn oppervlak'): buiten de proef van de truth-maintenance-tak", and in the first commit of the card, which
     *  refused it as "een squareRoot die niet de basis van een afronding is". */
    @Test void een_wortel_zonder_afronding_is_exact_of_een_fout() throws Exception {
        String zijde = rekenregel("zijde van een vondst", "BEREKENING", "Vondst", "zijde", builtin("squareRoot",
                List.of(lezing("oppervlak", "Vondst")), TWEE, "de wortel van zijn oppervlak"), "null");
        JsonNode exact = TmsDocumenten.zaakOp(null, List.of(
                instantieMet("V1", "Vondst", Map.of("basis", getalwaarde("2.00"), "oppervlak", getalwaarde("2.00")),
                        List.of()),
                instantieMet("V2", "Vondst", Map.of("basis", getalwaarde("8.00")), List.of()),
                instantieMet("V3", "Vondst", Map.of("basis", getalwaarde("0.08")), List.of())),
                List.of(waarde("V1", "zijde", "2"), waarde("V2", "zijde", "4"), waarde("V3", "zijde", "0.4"),
                        consistent()));
        JsonNode irrationaal = TmsDocumenten.zaakOp(null, List.of(
                instantieMet("V4", "Vondst", Map.of("basis", getalwaarde("1.00")), List.of())), List.of(consistent()));
        JsonNode negatief = TmsDocumenten.zaakOp(null, List.of(
                instantieMet("V5", "Vondst", Map.of("basis", getalwaarde("-2.00")), List.of())),
                List.of(waarde("V5", "zijde", null), inconsistent("zijde van een vondst", "V5")));
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(List.of(OPPERVLAK, zijde))) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            java.nio.file.Path ir = TmsDocumenten.document(GETALMODEL, volgorde);
            for (JsonNode zaak : List.of(exact, irrationaal, negatief))
                uitkomsten.add(namen + " " + TmsFoutWitnessTest.speel(ir, zaak, DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(namen + " gespeeld");
            verwacht.add(namen + " Rekenfout: fout in de regel \"zijde van een vondst\": irrational square root of 2"
                    + " reached without an explicit rounding (§13.4.17.13 mandates one)");
            verwacht.add(namen + " gespeeld");
        }
        assertEquals(verwacht, uitkomsten);
    }

    // ---------------- text ----------------

    private static final String TEKSTMODEL = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst", List.of(
            attribuut("naam", TEKST), attribuut("titel", TEKST), attribuut("omschrijving", TEKST), attribuut("code", TEKST),
            attribuut("basis", TWEE)), List.of())), List.of());

    static final String TITEL = rekenregel("titel van een vondst", "BEREKENING", "Vondst", "titel",
            lezing("naam", "Vondst"), "null");

    static JsonNode tekstzaak(String doel, String v1, String v2) throws Exception {
        return TmsDocumenten.zaakOp(null, List.of(
                instantieMet("V1", "Vondst", Map.of("naam", tekstwaarde("anker"), "titel", tekstwaarde("oud")), List.of()),
                instantieMet("V2", "Vondst", Map.of(), List.of())),
                List.of(waardeJson("V1", doel, tekstwaarde(v1)), waardeJson("V2", doel, tekstwaarde(v2)), consistent()));
    }

    @Test void een_tekstreeks_en_een_concatenatie_over_een_later_afgeleide_titel() throws Exception {
        String omschrijving = rekenregel("omschrijving van een vondst", "BEREKENING", "Vondst", "omschrijving",
                tekstreeks(List.of(tekstdeel("Vondst: "), tekstexpressie(lezing("titel", "Vondst")))), "null");
        assertElkeVolgorde(TEKSTMODEL, List.of(TITEL, omschrijving), tekstzaak("omschrijving", "Vondst: anker", "Vondst: "));
        String code = rekenregel("code van een vondst", "BEREKENING", "Vondst", "code", builtin("concatenation",
                List.of(lezing("titel", "Vondst"), lezing("naam", "Vondst")), TEKST, "de concatenatie van zijn titel en"
                        + " zijn naam"), "null");
        assertElkeVolgorde(TEKSTMODEL, List.of(TITEL, code), tekstzaak("code", "ankeranker", ""));
    }

    /** The text of a number is not decided by RUNTIME-SEMANTICS ch. 1.2, which says only that a leeg part reads "",
     *  and the reference interpreter (5,50 as "5.5", waar as "waar") and the runtime's {@code rsTekst} (5.50, true)
     *  answer it differently: the branch refuses such a part by name. */
    @Test void een_tekstreeks_met_een_getal_wordt_geweigerd() throws Exception {
        String omschrijving = rekenregel("omschrijving van een vondst", "BEREKENING", "Vondst", "omschrijving",
                tekstreeks(List.of(tekstdeel("Bedrag: "), tekstexpressie(lezing("basis", "Vondst")))), "null");
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> DroolsBackend.build(
                TmsDocumenten.document(TEKSTMODEL, List.of(omschrijving)), DroolsBackend.Branch.EXEC_MODEL));
        assertEquals("transpiler SKIP: regel: omschrijving van een vondst — een tekstdeel van type Numeriek in een"
                + " TextComposition ('zijn basis'): RUNTIME-SEMANTICS ch. 1.2 legt alleen vast dat een leeg deel \"\" leest;"
                + " de tekst van een getal, een waarheidswaarde of een datum beslist de spec niet, en de"
                + " referentie-interpreter en de runtimehulp rsTekst geven er een verschillende", e.getMessage());
    }

    // ---------------- the eleven test and the exact digits ----------------

    private static final String PROEFMODEL = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst", List.of(
            attribuut("grondnummer", GEHEEL), attribuut("nummer", GEHEEL), attribuut("grondcode", TEKST),
            attribuut("postcode", TEKST)), List.of("geldig", "viercijferig"))), List.of());

    private static String proef(String kind, String operand, int cijfers) {
        return "{\"node\":\"Predicate\",\"kind\":\"" + kind + "\",\"operand\":" + operand + ",\"negated\":false,\"digits\":"
                + cijfers + ",\"source\":{\"snippet\":\"" + kind + "\"}}";
    }

    @Test void de_elfproef_en_exact_vier_cijfers_over_een_later_afgeleide_waarde() throws Exception {
        String nummer = rekenregel("nummer van een vondst", "BEREKENING", "Vondst", "nummer",
                lezing("grondnummer", "Vondst"), "null");
        String geldig = kenmerkregel("geldig bij elfproef", "Vondst", "geldig",
                proef("elevenTest", lezing("nummer", "Vondst"), 0));
        String postcode = rekenregel("postcode van een vondst", "BEREKENING", "Vondst", "postcode",
                lezing("grondcode", "Vondst"), "null");
        String vier = kenmerkregel("viercijferig bij postcode", "Vondst", "viercijferig",
                proef("exactDigits", lezing("postcode", "Vondst"), 4));
        JsonNode zaak = TmsDocumenten.zaakOp(null, List.of(
                instantieMet("V1", "Vondst", Map.of("grondnummer", getalwaarde("123456782"), "nummer",
                        getalwaarde("111111111"), "grondcode", tekstwaarde("1234"), "postcode", tekstwaarde("abcd")),
                        List.of()),
                instantieMet("V2", "Vondst", Map.of("grondnummer", getalwaarde("111111111"), "grondcode",
                        tekstwaarde("12a4")), List.of())),
                List.of(verwacht("V1", "geldig", true), verwacht("V2", "geldig", false), verwacht("V1", "viercijferig", true),
                        verwacht("V2", "viercijferig", false), consistent()));
        assertElkeVolgorde(PROEFMODEL, List.of(nummer, geldig, postcode, vier), zaak);
    }

    // ---------------- the sub-day extractors, dateWith and dateTimeWith ----------------

    private static final String GETIJMODEL = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Getij", List.of(
            attribuut("vertrek", DATUMTIJD), attribuut("aankomst", DATUMTIJD), attribuut("uur", GEHEEL),
            attribuut("minuut", GEHEEL), attribuut("seconde", GEHEEL), attribuut("milliseconde", GEHEEL),
            attribuut("einddatum", DATUM), attribuut("eindtijd", DATUMTIJD)), List.of())), List.of());

    private static final String AANKOMST = lezing("aankomst", "Getij");

    @Test void tijddelen_en_datumconstructie_uit_een_later_afgeleide_aankomst() throws Exception {
        String aankomst = rekenregel("aankomst van een getij", "BEREKENING", "Getij", "aankomst",
                binair("plus", lezing("vertrek", "Getij"), literaal("2", getaltype(true, 0, eenheid("uur"))), DATUMTIJD),
                "null");
        String jaar = builtin("yearOf", List.of(AANKOMST), GEHEEL, "het jaar uit zijn aankomst");
        List<String> regels = List.of(aankomst,
                rekenregel("uur", "BEREKENING", "Getij", "uur", builtin("hourOf", List.of(AANKOMST), GEHEEL, "uur"), "null"),
                rekenregel("minuut", "BEREKENING", "Getij", "minuut", builtin("minuteOf", List.of(AANKOMST), GEHEEL,
                        "minuut"), "null"),
                rekenregel("seconde", "BEREKENING", "Getij", "seconde", builtin("secondOf", List.of(AANKOMST), GEHEEL,
                        "seconde"), "null"),
                rekenregel("milliseconde", "BEREKENING", "Getij", "milliseconde", builtin("millisecondOf",
                        List.of(AANKOMST), GEHEEL, "milliseconde"), "null"),
                rekenregel("einddatum", "BEREKENING", "Getij", "einddatum", builtin("dateWith", List.of(jaar,
                        literaal("12", GEHEEL), literaal("31", GEHEEL)), DATUM, "de datum met …"), "null"),
                rekenregel("eindtijd", "BEREKENING", "Getij", "eindtijd", builtin("dateTimeWith", List.of(jaar,
                        literaal("12", GEHEEL), literaal("31", GEHEEL), literaal("23", GEHEEL), literaal("59", GEHEEL),
                        literaal("59", GEHEEL), literaal("999", GEHEEL)), DATUMTIJD, "de datum-tijd met …"), "null"));
        JsonNode zaak = TmsDocumenten.zaakOp(null, List.of(instantieMet("G1", "Getij", Map.of(
                "vertrek", "{\"dateTime\":\"2026-03-01T22:30:15.250\"}",
                "aankomst", "{\"dateTime\":\"2000-01-01T00:00:00.000\"}"), List.of())),
                List.of(waarde("G1", "uur", "0"), waarde("G1", "minuut", "30"), waarde("G1", "seconde", "15"),
                        waarde("G1", "milliseconde", "250"), waardeJson("G1", "einddatum", datumwaarde("2026-12-31")),
                        waardeJson("G1", "eindtijd", "{\"dateTime\":\"2026-12-31T23:59:59.999\"}"), consistent()));
        for (int i = 1; i < regels.size(); i++)
            assertElkeVolgorde(GETIJMODEL, List.of(regels.get(0), regels.get(i)), zaak(zaak, i));
    }

    /** {@code zaak} with only the expectation of reader {@code i} (1-based: uur first) and the channel. */
    private static JsonNode zaak(JsonNode zaak, int i) {
        JsonNode kopie = zaak.deepCopy();
        com.fasterxml.jackson.databind.node.ArrayNode verwacht = (com.fasterxml.jackson.databind.node.ArrayNode)
                kopie.get("expected");
        JsonNode eigen = verwacht.get(i - 1), kanaal = verwacht.get(verwacht.size() - 1);
        verwacht.removeAll();
        verwacht.add(eigen);
        verwacht.add(kanaal);
        return kopie;
    }

    // ---------------- an Unresolved node ----------------

    private static final String ONOPGELOSTMODEL = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst",
            List.of(attribuut("basis", TWEE), attribuut("waarde", DRIE), attribuut("heffing", TWEE)), List.of("duur"))),
            List.of());

    private static final String WAARDE = rekenregel("waarde van een vondst", "BEREKENING", "Vondst", "waarde",
            binair("times", lezing("basis", "Vondst"), literaal("1.5", getaltype(false, 1, null)), DRIE), "null");

    static final String ONOPGELOSTE_WAARDE = "{\"node\":\"Unresolved\",\"type\":" + TWEE + ",\"source\":{\"snippet\":"
            + "\"een onopgeloste heffing\"}}";
    static final String ONOPGELOSTE_VOORWAARDE = "{\"node\":\"Unresolved\",\"source\":{\"snippet\":\"een onopgeloste"
            + " voorwaarde\"}}";

    @Test void een_onopgelost_merkteken_faalt_alleen_waar_de_evaluatie_het_bereikt() throws Exception {
        String heffing = rekenregel("heffing van een vondst", "BEREKENING", "Vondst", "heffing", let("k",
                lezing("waarde", "Vondst"), keuze(vergelijking("greater", binderlezing("k"), literaal("10", GEHEEL)),
                        literaal("5.00", TWEE), ONOPGELOSTE_WAARDE, TWEE), TWEE), "null");
        String duur = kenmerkregel("duur bij waarde", "Vondst", "duur", samengesteld("all", 0, List.of(
                vergelijking("greater", lezing("waarde", "Vondst"), literaal("100", GEHEEL)), ONOPGELOSTE_VOORWAARDE)));
        // the input waarde 3 sends the heffing to the marker on the way, 500 sends the condition to it
        for (String tussenstand : List.of("3", "500"))
            assertElkeVolgorde(ONOPGELOSTMODEL, List.of(WAARDE, heffing, duur), TmsDocumenten.zaakOp(null, List.of(
                    instantieMet("V1", "Vondst", Map.of("basis", getalwaarde("12.34"), "waarde", getalwaarde(tussenstand)),
                            List.of())),
                    List.of(waarde("V1", "heffing", "5.00"), verwacht("V1", "duur", false), consistent())));
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        JsonNode laag = TmsDocumenten.zaakOp(null, List.of(
                instantieMet("V2", "Vondst", Map.of("basis", getalwaarde("4.00")), List.of())), List.of(consistent()));
        JsonNode hoog = TmsDocumenten.zaakOp(null, List.of(
                instantieMet("V3", "Vondst", Map.of("basis", getalwaarde("100.00")), List.of())), List.of(consistent()));
        for (List<String> volgorde : TmsDocumenten.permutaties(List.of(WAARDE, heffing))) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            uitkomsten.add(namen + " " + TmsFoutWitnessTest.speel(TmsDocumenten.document(ONOPGELOSTMODEL, volgorde), laag,
                    DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(namen + " Rekenfout: fout in de regel \"heffing van een vondst\": Unresolved reached in"
                    + " evaluation: this rule fails observably");
        }
        for (List<String> volgorde : TmsDocumenten.permutaties(List.of(WAARDE, duur))) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            uitkomsten.add(namen + " " + TmsFoutWitnessTest.speel(TmsDocumenten.document(ONOPGELOSTMODEL, volgorde), hoog,
                    DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(namen + " Rekenfout: fout in de regel \"duur bij waarde\": Unresolved condition reached in"
                    + " evaluation");
        }
        assertEquals(verwacht, uitkomsten);
    }

    // ---------------- a dimension cell and a fold over a dimension selection ----------------

    private static final String DIMENSIEMODEL = "{\"objectTypes\":[{\"node\":\"ObjectType\",\"name\":\"Jutter\","
            + "\"animate\":true,\"plural\":\"Jutters\",\"attributes\":[{\"node\":\"Attribute\",\"name\":\"omzet\","
            + "\"datatype\":" + TWEE + ",\"dimensions\":[\"kwartaal\"]}," + attribuut("grondtoeslag", TWEE) + ","
            + attribuut("toeslag", TWEE) + "," + attribuut("tweede kwartaal", TWEE) + "," + attribuut("jaaromzet", TWEE)
            + "],\"characteristics\":[]}],\"factTypes\":[],\"parameters\":[],\"domains\":[],\"unitSystems\":[],"
            + "\"dimensions\":[{\"node\":\"Dimension\",\"name\":\"kwartaal\",\"plural\":\"kwartalen\",\"preposition\":"
            + "\"in\",\"labels\":[\"k1\",\"k2\",\"k3\",\"k4\"]}],\"timelines\":[],\"dayKinds\":[]}";

    private static final String OMZET = lezing("omzet", "Jutter");

    /** The case reader does not carry dimensioned input (§3.6, a Map bean member), on any branch, so the beans are
     *  set here; "Het tweede kwartaal … als zijn omzet in k2 plus zijn toeslag" and "De jaaromzet … als de som van zijn
     *  omzet over alle kwartalen plus zijn toeslag", with "De toeslag … als zijn grondtoeslag", in every order of the
     *  generated rules. J1: omzet k1 10, k2 20, k4 5 (no k3), grondtoeslag 1, toeslag 99 in the input: tweede kwartaal
     *  21, jaaromzet 36. */
    @Test void een_dimensiecel_en_een_dimensievouw_naast_een_later_afgeleide_toeslag() throws Exception {
        String toeslag = rekenregel("toeslag van een jutter", "BEREKENING", "Jutter", "toeslag",
                lezing("grondtoeslag", "Jutter"), "null");
        String cel = "{\"node\":\"DimensionCellRead\",\"attribute\":" + OMZET + ",\"labels\":[\"k2\"],\"source\":"
                + "{\"snippet\":\"zijn omzet in k2\"}}";
        String tweede = rekenregel("tweede kwartaal van een jutter", "BEREKENING", "Jutter", "tweede kwartaal",
                binair("plus", cel, lezing("toeslag", "Jutter"), TWEE), "null");
        String vouw = "{\"node\":\"Aggregation\",\"kind\":\"sum\",\"arguments\":[" + OMZET + "],\"dimensionSelection\":"
                + "{\"node\":\"DimensionSelection\",\"dimension\":\"kwartaal\",\"labels\":[\"k1\",\"k2\",\"k3\",\"k4\"]},"
                + "\"type\":" + TWEE + ",\"source\":{\"snippet\":\"de som van zijn omzet over alle kwartalen\"}}";
        String jaar = rekenregel("jaaromzet van een jutter", "BEREKENING", "Jutter", "jaaromzet",
                binair("plus", vouw, lezing("toeslag", "Jutter"), TWEE), "null");
        DroolsBackend.Build b = DroolsBackend.build(TmsDocumenten.document(DIMENSIEMODEL, List.of(toeslag, tweede, jaar)),
                DroolsBackend.Branch.EXEC_MODEL);
        int volgordes = 0;
        for (List<Rule> volgorde : TmsDocumenten.permutaties(b.execModelTranspiler().model().getRules())) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of("grondtoeslag", "1", "toeslag", "99"),
                    List.of());
            Map<String, BigDecimal> omzet = new LinkedHashMap<>();
            omzet.put("k1", new BigDecimal("10"));
            omzet.put("k2", new BigDecimal("20"));
            omzet.put("k4", new BigDecimal("5"));
            j1.getClass().getMethod(Namen.setter("omzet"), Map.class).invoke(j1, omzet);
            assertNull(TmsDocumenten.fixpunt(b, volgorde, List.of(j1), sessie -> { }), () -> "volgorde " + namen);
            TmsFoutWitnessTest.assertGetal("21", TmsDocumenten.lees(j1, "tweede kwartaal"), "tweede kwartaal, " + namen);
            TmsFoutWitnessTest.assertGetal("36", TmsDocumenten.lees(j1, "jaaromzet"), "jaaromzet, " + namen);
            volgordes++;
        }
        assertEquals(120, volgordes);
    }
}
