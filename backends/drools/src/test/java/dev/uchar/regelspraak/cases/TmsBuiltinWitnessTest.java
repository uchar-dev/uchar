package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.core.Namen;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.DATE;
import static dev.uchar.regelspraak.cases.TmsDocuments.DATE_TIME;
import static dev.uchar.regelspraak.cases.TmsDocuments.TEKST;
import static dev.uchar.regelspraak.cases.TmsDocuments.rounding;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribute;
import static dev.uchar.regelspraak.cases.TmsDocuments.binary;
import static dev.uchar.regelspraak.cases.TmsDocuments.binderRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.builtin;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.dateValue;
import static dev.uchar.regelspraak.cases.TmsDocuments.unit;
import static dev.uchar.regelspraak.cases.TmsDocuments.numberType;
import static dev.uchar.regelspraak.cases.TmsDocuments.numberValue;
import static dev.uchar.regelspraak.cases.TmsDocuments.inconsistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.instanceWith;
import static dev.uchar.regelspraak.cases.TmsDocuments.choice;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.let;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.literal;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.compound;
import static dev.uchar.regelspraak.cases.TmsDocuments.textPart;
import static dev.uchar.regelspraak.cases.TmsDocuments.textExpression;
import static dev.uchar.regelspraak.cases.TmsDocuments.textComposition;
import static dev.uchar.regelspraak.cases.TmsDocuments.textValue;
import static dev.uchar.regelspraak.cases.TmsDocuments.expected;
import static dev.uchar.regelspraak.cases.TmsDocuments.comparison;
import static dev.uchar.regelspraak.cases.TmsDocuments.value;
import static dev.uchar.regelspraak.cases.TmsDocuments.valueJson;
import static dev.uchar.regelspraak.cases.TmsComputeFormWitnessTest.assertEveryOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The builtin calls and the remaining value and condition forms on the exec-model branch, each over an operand
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
 *
 * <p>Two rows pin a refusal whose sentence the switch-over rewrote (card tms-omschakeling, step 5a2): a cell over two
 * dimensions and the eleven test over a path. The verifier passes both documents (measured 2026-10-10). <b>Red</b> on
 * {@code 3d50652a}, before that rewrite: "… ('zijn omzet in k2 in noord'): de exec-modeltak en deze tak lezen alleen
 * een cel van één dimensie" and "… ('elevenTest'): de exec-modeltak laat een afwezige link beide polariteiten onwaar
 * maken, …". Step 6c of card drl-teksttak-op-truth-maintenance rewrote the eleven test's sentence once more, with what
 * the analysis itself cannot tell apart instead of the behaviour of a branch. <b>Red</b> on {@code fbd2f2c0}, before
 * that rewrite: "… ('elevenTest'): de DRL-teksttak laat een afwezige link beide polariteiten onwaar maken, …".
 */
class TmsBuiltinWitnessTest {

    private static final String WHOLE = numberType(true, 0, null), TWO = numberType(false, 2, null),
            THREE = numberType(false, 3, null);

    // ---------------- the date builtins ----------------

    private static final String DATE_MODEL = TmsDocuments.model(List.of(TmsDocuments.objectType("Vondst", List.of(
            attribute("vinddatum", DATE), attribute("peildatum", DATE), attribute("peiljaar", WHOLE),
            attribute("peilmaand", WHOLE), attribute("peildag", WHOLE),
            attribute("weken", numberType(true, 0, unit("weken"))),
            attribute("dagen", numberType(true, 0, unit("dagen"))), attribute("paasdatum", DATE)), List.of())), List.of());

    private static final String AS_OF_DATE = ownRead("peildatum", "Vondst"), VINDDATUM = ownRead("vinddatum", "Vondst");

    /** "De peildatum van een Vondst moet berekend worden als zijn vinddatum plus 10 dagen". */
    static final String AS_OF = computingRule("peildatum van een vondst", "BEREKENING", "Vondst", "peildatum",
            binary("plus", VINDDATUM, literal("10", numberType(true, 0, unit("dagen"))), DATE), "null");

    static JsonNode dateCase(String target, String v1, String v2) throws Exception {
        return TmsDocuments.caseOn(null, List.of(
                instanceWith("V1", "Vondst", Map.of("vinddatum", dateValue("2026-03-25"),
                        "peildatum", dateValue("2000-01-01")), List.of()),
                instanceWith("V2", "Vondst", Map.of("vinddatum", dateValue("2024-12-28")), List.of()),
                instanceWith("V3", "Vondst", Map.of(), List.of())),
                List.of(valueJson("V1", target, v1), valueJson("V2", target, v2), value("V3", target, null), consistent()));
    }

    private static String extractor(String naam, String kind, String target) {
        return computingRule(naam, "BEREKENING", "Vondst", target, builtin(kind, List.of(AS_OF_DATE), WHOLE,
                kind + " van zijn peildatum"), "null");
    }

    @Test void year_month_and_day_of_a_later_derived_date() throws Exception {
        assertEveryOrder(DATE_MODEL, List.of(AS_OF, extractor("peiljaar", "yearOf", "peiljaar")),
                dateCase("peiljaar", numberValue("2026"), numberValue("2025")));
        assertEveryOrder(DATE_MODEL, List.of(AS_OF, extractor("peilmaand", "monthOf", "peilmaand")),
                dateCase("peilmaand", numberValue("4"), numberValue("1")));
        assertEveryOrder(DATE_MODEL, List.of(AS_OF, extractor("peildag", "dayOf", "peildag")),
                dateCase("peildag", numberValue("4"), numberValue("7")));
    }

    @Test void the_duration_to_a_later_derived_date() throws Exception {
        String weeks = computingRule("weken van een vondst", "BEREKENING", "Vondst", "weken", builtin("duration",
                List.of(VINDDATUM, AS_OF_DATE), numberType(true, 0, unit("weken")),
                "de tijdsduur van zijn vinddatum tot zijn peildatum in hele weken"), "null");
        assertEveryOrder(DATE_MODEL, List.of(AS_OF, weeks), dateCase("weken", numberValue("1"), numberValue("1")));
        String days = computingRule("dagen van een vondst", "BEREKENING", "Vondst", "dagen", builtin("absoluteDuration",
                List.of(AS_OF_DATE, VINDDATUM), numberType(true, 0, unit("dagen")),
                "de absolute tijdsduur van zijn peildatum tot zijn vinddatum in hele dagen"), "null");
        assertEveryOrder(DATE_MODEL, List.of(AS_OF, days), dateCase("dagen", numberValue("10"), numberValue("10")));
    }

    @Test void the_easter_sunday_of_the_year_of_a_later_derived_date() throws Exception {
        String pasen = computingRule("paasdatum van een vondst", "BEREKENING", "Vondst", "paasdatum", builtin("easterSunday",
                List.of(builtin("yearOf", List.of(AS_OF_DATE), WHOLE, "het jaar uit zijn peildatum")), DATE,
                "de eerste paasdag van het jaar uit zijn peildatum"), "null");
        assertEveryOrder(DATE_MODEL, List.of(AS_OF, pasen),
                dateCase("paasdatum", dateValue("2026-04-05"), dateValue("2025-04-20")));
    }

    // ---------------- the absolute value and the square root ----------------

    private static final String NUMBER_MODEL = TmsDocuments.model(List.of(TmsDocuments.objectType("Vondst", List.of(
            attribute("basis", TWO), attribute("saldo", TWO), attribute("afstand", TWO), attribute("oppervlak", TWO),
            attribute("zijde", TWO)), List.of())), List.of());

    @Test void the_absolute_value_of_a_later_derived_balance() throws Exception {
        String balance = computingRule("saldo van een vondst", "BEREKENING", "Vondst", "saldo",
                binary("minus", ownRead("basis", "Vondst"), literal("50.00", TWO), TWO), "null");
        String afstand = computingRule("afstand van een vondst", "BEREKENING", "Vondst", "afstand",
                builtin("absoluteValue", List.of(ownRead("saldo", "Vondst")), TWO, "de absolute waarde van zijn saldo"),
                "null");
        assertEveryOrder(NUMBER_MODEL, List.of(balance, afstand), TmsDocuments.caseOn(null, List.of(
                instanceWith("V1", "Vondst", Map.of("basis", numberValue("20.00"), "saldo", numberValue("7.00")), List.of()),
                instanceWith("V2", "Vondst", Map.of("basis", numberValue("80.00")), List.of()),
                instanceWith("V3", "Vondst", Map.of(), List.of())),
                List.of(value("V1", "afstand", "30.00"), value("V2", "afstand", "30.00"),
                        value("V3", "afstand", "50.00"), consistent())));
    }

    static final String OPPERVLAK = computingRule("oppervlak van een vondst", "BEREKENING", "Vondst", "oppervlak",
            binary("times", ownRead("basis", "Vondst"), literal("2", WHOLE), TWO), "null");

    static final String SIDE = computingRule("zijde van een vondst", "BEREKENING", "Vondst", "zijde",
            rounding(builtin("squareRoot", List.of(ownRead("oppervlak", "Vondst")), TWO, "de wortel van zijn oppervlak"),
                    "halfAwayFromZero", 2, TWO), "null");

    @Test void the_square_root_of_a_later_derived_oppervlak_under_its_rounding() throws Exception {
        assertEveryOrder(NUMBER_MODEL, List.of(OPPERVLAK, SIDE), TmsDocuments.caseOn(null, List.of(
                instanceWith("V1", "Vondst", Map.of("basis", numberValue("20.00"), "oppervlak", numberValue("-9.00")),
                        List.of()),
                instanceWith("V2", "Vondst", Map.of("basis", numberValue("8.00")), List.of())),
                List.of(value("V1", "zijde", "6.32"), value("V2", "zijde", "4.00"), consistent())));
        assertEveryOrder(NUMBER_MODEL, List.of(OPPERVLAK, SIDE), TmsDocuments.caseOn(null, List.of(
                instanceWith("V3", "Vondst", Map.of("basis", numberValue("-2.00")), List.of())),
                List.of(value("V3", "zijde", null), inconsistent("zijde van een vondst", "V3"))));
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
    @Test void a_square_root_without_rounding_is_exact_or_a_fault() throws Exception {
        String side = computingRule("zijde van een vondst", "BEREKENING", "Vondst", "zijde", builtin("squareRoot",
                List.of(ownRead("oppervlak", "Vondst")), TWO, "de wortel van zijn oppervlak"), "null");
        JsonNode exact = TmsDocuments.caseOn(null, List.of(
                instanceWith("V1", "Vondst", Map.of("basis", numberValue("2.00"), "oppervlak", numberValue("2.00")),
                        List.of()),
                instanceWith("V2", "Vondst", Map.of("basis", numberValue("8.00")), List.of()),
                instanceWith("V3", "Vondst", Map.of("basis", numberValue("0.08")), List.of())),
                List.of(value("V1", "zijde", "2"), value("V2", "zijde", "4"), value("V3", "zijde", "0.4"),
                        consistent()));
        JsonNode irrational = TmsDocuments.caseOn(null, List.of(
                instanceWith("V4", "Vondst", Map.of("basis", numberValue("1.00")), List.of())), List.of(consistent()));
        JsonNode negative = TmsDocuments.caseOn(null, List.of(
                instanceWith("V5", "Vondst", Map.of("basis", numberValue("-2.00")), List.of())),
                List.of(value("V5", "zijde", null), inconsistent("zijde van een vondst", "V5")));
        List<String> outcomes = new ArrayList<>(), expected = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(List.of(OPPERVLAK, side))) {
            List<String> names = TmsDocuments.ruleNames(order);
            java.nio.file.Path ir = TmsDocuments.document(NUMBER_MODEL, order);
            for (JsonNode caseDocument : List.of(exact, irrational, negative))
                outcomes.add(names + " " + TmsFaultWitnessTest.play(ir, caseDocument, DroolsBackend.Branch.EXEC_MODEL));
            expected.add(names + " gespeeld");
            expected.add(names + " ComputeFault: fout in de regel \"zijde van een vondst\": irrational square root of 2"
                    + " reached without an explicit rounding (§13.4.17.13 mandates one)");
            expected.add(names + " gespeeld");
        }
        assertEquals(expected, outcomes);
    }

    // ---------------- text ----------------

    private static final String TEXT_MODEL = TmsDocuments.model(List.of(TmsDocuments.objectType("Vondst", List.of(
            attribute("naam", TEKST), attribute("titel", TEKST), attribute("omschrijving", TEKST), attribute("code", TEKST),
            attribute("basis", TWO)), List.of())), List.of());

    static final String TITLE = computingRule("titel van een vondst", "BEREKENING", "Vondst", "titel",
            ownRead("naam", "Vondst"), "null");

    static JsonNode textCase(String target, String v1, String v2) throws Exception {
        return TmsDocuments.caseOn(null, List.of(
                instanceWith("V1", "Vondst", Map.of("naam", textValue("anker"), "titel", textValue("oud")), List.of()),
                instanceWith("V2", "Vondst", Map.of(), List.of())),
                List.of(valueJson("V1", target, textValue(v1)), valueJson("V2", target, textValue(v2)), consistent()));
    }

    @Test void a_text_sequence_and_a_concatenation_over_a_later_derived_title() throws Exception {
        String description = computingRule("omschrijving van een vondst", "BEREKENING", "Vondst", "omschrijving",
                textComposition(List.of(textPart("Vondst: "), textExpression(ownRead("titel", "Vondst")))), "null");
        assertEveryOrder(TEXT_MODEL, List.of(TITLE, description), textCase("omschrijving", "Vondst: anker", "Vondst: "));
        String code = computingRule("code van een vondst", "BEREKENING", "Vondst", "code", builtin("concatenation",
                List.of(ownRead("titel", "Vondst"), ownRead("naam", "Vondst")), TEKST, "de concatenatie van zijn titel en"
                        + " zijn naam"), "null");
        assertEveryOrder(TEXT_MODEL, List.of(TITLE, code), textCase("code", "ankeranker", ""));
    }

    /** The text of a number is not decided by RUNTIME-SEMANTICS ch. 1.2, which says only that a leeg part reads "",
     *  and the reference interpreter (5,50 as "5.5", waar as "waar") and the runtime's {@code rsTekst} (5.50, true)
     *  answer it differently: the branch refuses such a part by name. */
    @Test void a_text_composition_with_a_number_is_refused() throws Exception {
        String description = computingRule("omschrijving van een vondst", "BEREKENING", "Vondst", "omschrijving",
                textComposition(List.of(textPart("Bedrag: "), textExpression(ownRead("basis", "Vondst")))), "null");
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> DroolsBackend.build(
                TmsDocuments.document(TEXT_MODEL, List.of(description)), DroolsBackend.Branch.EXEC_MODEL));
        assertEquals("transpiler SKIP: regel: omschrijving van een vondst — een tekstdeel van type Numeriek in een"
                + " TextComposition ('zijn basis'): RUNTIME-SEMANTICS ch. 1.2 legt alleen vast dat een leeg deel \"\" leest;"
                + " de tekst van een getal, een waarheidswaarde of een datum beslist de spec niet, en de"
                + " referentie-interpreter en de runtimehulp rsTekst geven er een verschillende", e.getMessage());
    }

    // ---------------- the eleven test and the exact digits ----------------

    private static final String TRIAL_MODEL = TmsDocuments.model(List.of(TmsDocuments.objectType("Vondst", List.of(
            attribute("grondnummer", WHOLE), attribute("nummer", WHOLE), attribute("grondcode", TEKST),
            attribute("postcode", TEKST)), List.of("geldig", "viercijferig"))), List.of());

    private static String trial(String kind, String operand, int digits) {
        return "{\"node\":\"Predicate\",\"kind\":\"" + kind + "\",\"operand\":" + operand + ",\"negated\":false,\"digits\":"
                + digits + ",\"source\":{\"snippet\":\"" + kind + "\"}}";
    }

    @Test void the_eleven_test_and_exact_four_digits_over_a_later_derived_value() throws Exception {
        String number = computingRule("nummer van een vondst", "BEREKENING", "Vondst", "nummer",
                ownRead("grondnummer", "Vondst"), "null");
        String valid = characteristicRule("geldig bij elfproef", "Vondst", "geldig",
                trial("elevenTest", ownRead("nummer", "Vondst"), 0));
        String postcode = computingRule("postcode van een vondst", "BEREKENING", "Vondst", "postcode",
                ownRead("grondcode", "Vondst"), "null");
        String four = characteristicRule("viercijferig bij postcode", "Vondst", "viercijferig",
                trial("exactDigits", ownRead("postcode", "Vondst"), 4));
        JsonNode caseDocument = TmsDocuments.caseOn(null, List.of(
                instanceWith("V1", "Vondst", Map.of("grondnummer", numberValue("123456782"), "nummer",
                        numberValue("111111111"), "grondcode", textValue("1234"), "postcode", textValue("abcd")),
                        List.of()),
                instanceWith("V2", "Vondst", Map.of("grondnummer", numberValue("111111111"), "grondcode",
                        textValue("12a4")), List.of())),
                List.of(expected("V1", "geldig", true), expected("V2", "geldig", false), expected("V1", "viercijferig", true),
                        expected("V2", "viercijferig", false), consistent()));
        assertEveryOrder(TRIAL_MODEL, List.of(number, valid, postcode, four), caseDocument);
    }

    /** "Een Vondst is geldig indien het nummer van zijn vinder aan de elfproef voldoet": the eleven test over a path of
     *  one role. The branch reads the operand as any value, in which an absent link is leeg (RUNTIME-SEMANTICS ch. 5.8,
     *  rule 1), so it cannot tell an absent link from a leeg nummer; it refuses the form by name. */
    @Test void the_eleven_test_over_a_path_is_refused_by_name() throws Exception {
        String model = TmsDocuments.model(List.of(
                TmsDocuments.objectType("Jutter", List.of(attribute("nummer", WHOLE)), List.of()),
                TmsDocuments.objectType("Vondst", List.of(), List.of("geldig"))),
                List.of(TmsDocuments.factTypeOneToMany("vinding", "vinder", "Jutter", "vondst", "Vondst", "vondsten")));
        String valid = characteristicRule("geldig bij elfproef", "Vondst", "geldig",
                trial("elevenTest", TmsDocuments.pathRead("nummer", "Jutter", "vinding", "vinder"), 0));
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> DroolsBackend.build(
                TmsDocuments.document(model, List.of(valid)), DroolsBackend.Branch.EXEC_MODEL));
        assertEquals("transpiler SKIP: regel: geldig bij elfproef — een Predicate elevenTest over een pad"
                + " ('elevenTest'): de operand wordt als een waarde gelezen, waarin een afwezige link leeg is"
                + " (RUNTIME-SEMANTICS ch. 5.8, regel 1), zodat de toets een afwezige link hier niet van een lege waarde"
                + " kan onderscheiden", e.getMessage());
    }

    // ---------------- the sub-day extractors, dateWith and dateTimeWith ----------------

    private static final String GETIJMODEL = TmsDocuments.model(List.of(TmsDocuments.objectType("Getij", List.of(
            attribute("vertrek", DATE_TIME), attribute("aankomst", DATE_TIME), attribute("uur", WHOLE),
            attribute("minuut", WHOLE), attribute("seconde", WHOLE), attribute("milliseconde", WHOLE),
            attribute("einddatum", DATE), attribute("eindtijd", DATE_TIME)), List.of())), List.of());

    private static final String AANKOMST = ownRead("aankomst", "Getij");

    @Test void time_parts_and_date_construction_of_a_later_derived_aankomst() throws Exception {
        String aankomst = computingRule("aankomst van een getij", "BEREKENING", "Getij", "aankomst",
                binary("plus", ownRead("vertrek", "Getij"), literal("2", numberType(true, 0, unit("uur"))), DATE_TIME),
                "null");
        String year = builtin("yearOf", List.of(AANKOMST), WHOLE, "het jaar uit zijn aankomst");
        List<String> rules = List.of(aankomst,
                computingRule("uur", "BEREKENING", "Getij", "uur", builtin("hourOf", List.of(AANKOMST), WHOLE, "uur"), "null"),
                computingRule("minuut", "BEREKENING", "Getij", "minuut", builtin("minuteOf", List.of(AANKOMST), WHOLE,
                        "minuut"), "null"),
                computingRule("seconde", "BEREKENING", "Getij", "seconde", builtin("secondOf", List.of(AANKOMST), WHOLE,
                        "seconde"), "null"),
                computingRule("milliseconde", "BEREKENING", "Getij", "milliseconde", builtin("millisecondOf",
                        List.of(AANKOMST), WHOLE, "milliseconde"), "null"),
                computingRule("einddatum", "BEREKENING", "Getij", "einddatum", builtin("dateWith", List.of(year,
                        literal("12", WHOLE), literal("31", WHOLE)), DATE, "de datum met …"), "null"),
                computingRule("eindtijd", "BEREKENING", "Getij", "eindtijd", builtin("dateTimeWith", List.of(year,
                        literal("12", WHOLE), literal("31", WHOLE), literal("23", WHOLE), literal("59", WHOLE),
                        literal("59", WHOLE), literal("999", WHOLE)), DATE_TIME, "de datum-tijd met …"), "null"));
        JsonNode caseDocument = TmsDocuments.caseOn(null, List.of(instanceWith("G1", "Getij", Map.of(
                "vertrek", "{\"dateTime\":\"2026-03-01T22:30:15.250\"}",
                "aankomst", "{\"dateTime\":\"2000-01-01T00:00:00.000\"}"), List.of())),
                List.of(value("G1", "uur", "0"), value("G1", "minuut", "30"), value("G1", "seconde", "15"),
                        value("G1", "milliseconde", "250"), valueJson("G1", "einddatum", dateValue("2026-12-31")),
                        valueJson("G1", "eindtijd", "{\"dateTime\":\"2026-12-31T23:59:59.999\"}"), consistent()));
        for (int i = 1; i < rules.size(); i++)
            assertEveryOrder(GETIJMODEL, List.of(rules.get(0), rules.get(i)), caseDocument(caseDocument, i));
    }

    /** {@code caseDocument} with only the expectation of reader {@code i} (1-based: uur first) and the channel. */
    private static JsonNode caseDocument(JsonNode caseDocument, int i) {
        JsonNode copy = caseDocument.deepCopy();
        com.fasterxml.jackson.databind.node.ArrayNode expected = (com.fasterxml.jackson.databind.node.ArrayNode)
                copy.get("expected");
        JsonNode own = expected.get(i - 1), channel = expected.get(expected.size() - 1);
        expected.removeAll();
        expected.add(own);
        expected.add(channel);
        return copy;
    }

    // ---------------- an Unresolved node ----------------

    private static final String UNRESOLVED_MODEL = TmsDocuments.model(List.of(TmsDocuments.objectType("Vondst",
            List.of(attribute("basis", TWO), attribute("waarde", THREE), attribute("heffing", TWO)), List.of("duur"))),
            List.of());

    private static final String VALUE = computingRule("waarde van een vondst", "BEREKENING", "Vondst", "waarde",
            binary("times", ownRead("basis", "Vondst"), literal("1.5", numberType(false, 1, null)), THREE), "null");

    static final String UNRESOLVED_VALUE = "{\"node\":\"Unresolved\",\"type\":" + TWO + ",\"source\":{\"snippet\":"
            + "\"een onopgeloste heffing\"}}";
    static final String UNRESOLVED_CONDITION = "{\"node\":\"Unresolved\",\"source\":{\"snippet\":\"een onopgeloste"
            + " voorwaarde\"}}";

    @Test void an_unresolved_marker_fails_only_where_the_evaluation_reaches_it() throws Exception {
        String heffing = computingRule("heffing van een vondst", "BEREKENING", "Vondst", "heffing", let("k",
                ownRead("waarde", "Vondst"), choice(comparison("greater", binderRead("k"), literal("10", WHOLE)),
                        literal("5.00", TWO), UNRESOLVED_VALUE, TWO), TWO), "null");
        String duration = characteristicRule("duur bij waarde", "Vondst", "duur", compound("all", 0, List.of(
                comparison("greater", ownRead("waarde", "Vondst"), literal("100", WHOLE)), UNRESOLVED_CONDITION)));
        // the input waarde 3 sends the heffing to the marker on the way, 500 sends the condition to it
        for (String intermediateState : List.of("3", "500"))
            assertEveryOrder(UNRESOLVED_MODEL, List.of(VALUE, heffing, duration), TmsDocuments.caseOn(null, List.of(
                    instanceWith("V1", "Vondst", Map.of("basis", numberValue("12.34"), "waarde", numberValue(intermediateState)),
                            List.of())),
                    List.of(value("V1", "heffing", "5.00"), expected("V1", "duur", false), consistent())));
        List<String> outcomes = new ArrayList<>(), expected = new ArrayList<>();
        JsonNode low = TmsDocuments.caseOn(null, List.of(
                instanceWith("V2", "Vondst", Map.of("basis", numberValue("4.00")), List.of())), List.of(consistent()));
        JsonNode high = TmsDocuments.caseOn(null, List.of(
                instanceWith("V3", "Vondst", Map.of("basis", numberValue("100.00")), List.of())), List.of(consistent()));
        for (List<String> order : TmsDocuments.permutations(List.of(VALUE, heffing))) {
            List<String> names = TmsDocuments.ruleNames(order);
            outcomes.add(names + " " + TmsFaultWitnessTest.play(TmsDocuments.document(UNRESOLVED_MODEL, order), low,
                    DroolsBackend.Branch.EXEC_MODEL));
            expected.add(names + " ComputeFault: fout in de regel \"heffing van een vondst\": Unresolved reached in"
                    + " evaluation: this rule fails observably");
        }
        for (List<String> order : TmsDocuments.permutations(List.of(VALUE, duration))) {
            List<String> names = TmsDocuments.ruleNames(order);
            outcomes.add(names + " " + TmsFaultWitnessTest.play(TmsDocuments.document(UNRESOLVED_MODEL, order), high,
                    DroolsBackend.Branch.EXEC_MODEL));
            expected.add(names + " ComputeFault: fout in de regel \"duur bij waarde\": Unresolved condition reached in"
                    + " evaluation");
        }
        assertEquals(expected, outcomes);
    }

    // ---------------- a dimension cell and a fold over a dimension selection ----------------

    private static final String DIMENSION_MODEL = "{\"objectTypes\":[{\"node\":\"ObjectType\",\"name\":\"Jutter\","
            + "\"animate\":true,\"plural\":\"Jutters\",\"attributes\":[{\"node\":\"Attribute\",\"name\":\"omzet\","
            + "\"datatype\":" + TWO + ",\"dimensions\":[\"kwartaal\"]}," + attribute("grondtoeslag", TWO) + ","
            + attribute("toeslag", TWO) + "," + attribute("tweede kwartaal", TWO) + "," + attribute("jaaromzet", TWO)
            + "],\"characteristics\":[]}],\"factTypes\":[],\"parameters\":[],\"domains\":[],\"unitSystems\":[],"
            + "\"dimensions\":[{\"node\":\"Dimension\",\"name\":\"kwartaal\",\"plural\":\"kwartalen\",\"preposition\":"
            + "\"in\",\"labels\":[\"k1\",\"k2\",\"k3\",\"k4\"]}],\"timelines\":[],\"dayKinds\":[]}";

    private static final String OMZET = ownRead("omzet", "Jutter");

    /** The case reader does not carry dimensioned input (§3.6, a Map bean member), on any branch, so the beans are
     *  set here; "Het tweede kwartaal … als zijn omzet in k2 plus zijn toeslag" and "De jaaromzet … als de som van zijn
     *  omzet over alle kwartalen plus zijn toeslag", with "De toeslag … als zijn grondtoeslag", in every order of the
     *  generated rules. J1: omzet k1 10, k2 20, k4 5 (no k3), grondtoeslag 1, toeslag 99 in the input: tweede kwartaal
     *  21, jaaromzet 36. */
    /** "Het tweede kwartaal … als zijn omzet in k2 in noord": a cell over two dimensions, which the branch refuses by
     *  name, since it reads a cell of one dimension (the row below). */
    @Test void a_cell_over_two_dimensions_is_refused_by_name() throws Exception {
        String model = DIMENSION_MODEL.replace("\"dimensions\":[\"kwartaal\"]", "\"dimensions\":[\"kwartaal\",\"regio\"]")
                .replace("\"labels\":[\"k1\",\"k2\",\"k3\",\"k4\"]}]", "\"labels\":[\"k1\",\"k2\",\"k3\",\"k4\"]},"
                        + "{\"node\":\"Dimension\",\"name\":\"regio\",\"plural\":\"regio's\",\"preposition\":\"in\","
                        + "\"labels\":[\"noord\",\"zuid\"]}]");
        String cell = "{\"node\":\"DimensionCellRead\",\"attribute\":" + OMZET + ",\"labels\":[\"k2\",\"noord\"],"
                + "\"source\":{\"snippet\":\"zijn omzet in k2 in noord\"}}";
        String second = computingRule("tweede kwartaal van een jutter", "BEREKENING", "Jutter", "tweede kwartaal", cell, "null");
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> DroolsBackend.build(
                TmsDocuments.document(model, List.of(second)), DroolsBackend.Branch.EXEC_MODEL));
        assertEquals("transpiler SKIP: regel: tweede kwartaal van een jutter — een DimensionCellRead over 2 dimensies"
                + " ('zijn omzet in k2 in noord'): deze tak leest alleen een cel van één dimensie", e.getMessage());
    }

    @Test void a_dimension_cell_and_a_dimension_fold_beside_a_later_derived_toeslag() throws Exception {
        String toeslag = computingRule("toeslag van een jutter", "BEREKENING", "Jutter", "toeslag",
                ownRead("grondtoeslag", "Jutter"), "null");
        String cell = "{\"node\":\"DimensionCellRead\",\"attribute\":" + OMZET + ",\"labels\":[\"k2\"],\"source\":"
                + "{\"snippet\":\"zijn omzet in k2\"}}";
        String second = computingRule("tweede kwartaal van een jutter", "BEREKENING", "Jutter", "tweede kwartaal",
                binary("plus", cell, ownRead("toeslag", "Jutter"), TWO), "null");
        String fold = "{\"node\":\"Aggregation\",\"kind\":\"sum\",\"arguments\":[" + OMZET + "],\"dimensionSelection\":"
                + "{\"node\":\"DimensionSelection\",\"dimension\":\"kwartaal\",\"labels\":[\"k1\",\"k2\",\"k3\",\"k4\"]},"
                + "\"type\":" + TWO + ",\"source\":{\"snippet\":\"de som van zijn omzet over alle kwartalen\"}}";
        String year = computingRule("jaaromzet van een jutter", "BEREKENING", "Jutter", "jaaromzet",
                binary("plus", fold, ownRead("toeslag", "Jutter"), TWO), "null");
        DroolsBackend.Build b = DroolsBackend.build(TmsDocuments.document(DIMENSION_MODEL, List.of(toeslag, second, year)),
                DroolsBackend.Branch.EXEC_MODEL);
        int orders = 0;
        for (List<Rule> order : TmsDocuments.permutations(b.execModelTranspiler().model().getRules())) {
            List<String> names = order.stream().map(Rule::getName).toList();
            Object j1 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of("grondtoeslag", "1", "toeslag", "99"),
                    List.of());
            Map<String, BigDecimal> omzet = new LinkedHashMap<>();
            omzet.put("k1", new BigDecimal("10"));
            omzet.put("k2", new BigDecimal("20"));
            omzet.put("k4", new BigDecimal("5"));
            j1.getClass().getMethod(Namen.setter("omzet"), Map.class).invoke(j1, omzet);
            assertNull(TmsDocuments.fixpoint(b, order, List.of(j1), session -> { }), () -> "volgorde " + names);
            TmsFaultWitnessTest.assertNumber("21", TmsDocuments.read(j1, "tweede kwartaal"), "tweede kwartaal, " + names);
            TmsFaultWitnessTest.assertNumber("36", TmsDocuments.read(j1, "jaaromzet"), "jaaromzet, " + names);
            orders++;
        }
        assertEquals(120, orders);
    }
}
