package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.DATE;
import static dev.uchar.regelspraak.cases.TmsDocuments.DATE_TIME;
import static dev.uchar.regelspraak.cases.TmsDocuments.COUNTING;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribute;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.dateValue;
import static dev.uchar.regelspraak.cases.TmsDocuments.unit;
import static dev.uchar.regelspraak.cases.TmsDocuments.numberType;
import static dev.uchar.regelspraak.cases.TmsDocuments.numberValue;
import static dev.uchar.regelspraak.cases.TmsDocuments.instanceWith;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.literal;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.value;
import static dev.uchar.regelspraak.cases.TmsDocuments.valueJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Day kinds on the exec-model branch (RUNTIME-SEMANTICS ch. 3.8, IR-SPEC ch. 3.7, WF-031): a date or a
 * date-time plus or minus a count of days of a kind walks the calendar, and a duration in whole days of a kind counts
 * them. A definition reads nothing of working memory, so its predicate is a constant of the document, compiled where a
 * form steps or counts in its kind; the reads the step depends on are the date and the count.
 *
 * <p><b>A, a date derived later.</b> The springtijdag of {@code jutterswet-bergingsklok}, the 1st and the 15th of a
 * month. "De meldingsdatum van een Bergingsploeg moet berekend worden als zijn vinddatum plus 2 dagen"; "De uiterste
 * datum … als zijn meldingsdatum plus zijn bestek" and "De vroegste datum … min zijn bestek", the bestek in
 * springtijdagen; "De spanne … als de tijdsduur van zijn meldingsdatum tot zijn opleverdatum in hele springtijdagen";
 * "De afstand … als de absolute tijdsduur van zijn opleverdatum tot zijn meldingsdatum in hele springtijdagen". By hand:
 * P1 (vinddatum 2026-03-10, meldingsdatum 2026-06-01 as input, bestek 3, opleverdatum 2026-05-20): meldingsdatum
 * 2026-03-12; uiterste 2026-04-15 (03-15, 04-01, 04-15, the start never counting); vroegste 2026-02-01 (03-01, 02-15,
 * 02-01); spanne 5 (03-15, 04-01, 04-15, 05-01, 05-15) and afstand 5, where the input meldingsdatum would give
 * 2026-07-15, 2026-04-15, -1 and 1. P2 (vinddatum 2026-12-30, no bestek, opleverdatum 2027-01-01): meldingsdatum
 * 2027-01-01, a leeg count leaves it unchanged in both steps, and equal dates count 0. P3 (no vinddatum): everything
 * leeg. P4 (vinddatum 2026-01-13, bestek 0, opleverdatum 2026-01-01): meldingsdatum 2026-01-15, a springtijdag, which a
 * count of 0 leaves unchanged; spanne -1 (the 15th, the later date, counts and the 1st, the earlier, does not) and
 * afstand 1. Every order of the five rules.
 *
 * <p><b>A2, a date-time.</b> "Het meldtijdstip van een Getijpost moet berekend worden als zijn aanvraagtijdstip plus 3
 * uur"; "Het sluittijdstip … als zijn meldtijdstip plus zijn wachttermijn" and "Het openingstijdstip … min zijn
 * wachttermijn", the wachttermijn in springtijdagen. G1 (aanvraagtijdstip 2026-03-10T21:15:30, wachttermijn 2):
 * meldtijdstip 2026-03-11T00:15:30, sluittijdstip 2026-04-01T00:15:30, openingstijdstip 2026-02-15T00:15:30, the clock
 * time riding through; the meldtijdstip read off the bean, which holds none, would leave both leeg. G2
 * (aanvraagtijdstip 2026-03-14T23:00:00, wachttermijn 1): meldtijdstip 2026-03-15T02:00:00, itself a springtijdag that
 * does not count: sluittijdstip 2026-04-01T02:00:00, openingstijdstip 2026-03-01T02:00:00. Every order of the three
 * rules.
 *
 * <p><b>B, the reach of the walk.</b> A seizoensdag is a day later than 31-12-2025 and earlier than 01-01-2027. "Een
 * Vondst is erkend indien hij gemeld is"; "De termijn van een Vondst moet berekend worden als zijn vinddatum plus zijn
 * wachttijd, indien hij niet erkend is", the wachttijd in seizoensdagen. From a date after 2026 the walk finds no
 * seizoensdag within its reach of 40 000 days: a fault. V1 (gemeld, vinddatum 2027-06-01, wachttijd 5) is erkend, so
 * the termijn is not evaluated at the fixpoint: no fault, termijn leeg; where the termijn's rule fires before the
 * erkenning, the walk faults on that intermediate state, and the fault goes with it. V2 (vinddatum 2026-03-01): termijn
 * 2026-03-06. Every order of the two rules, and the generated rules in a hundred sampled orders on a V1 of its own, in
 * some of which the fault was inserted and withdrawn. V3 (not gemeld, vinddatum 2027-06-01) faults at the fixpoint, in
 * the reference interpreter's sentence, in both orders.
 *
 * <p><b>C, definitions that do not compile.</b> A schouwdag is a day later than 28-02-2026 and earlier than 01-10-2026;
 * a hoogschouwdag is defined as "21-06-2026 is een schouwdag", a test of a day kind on another day than the bound day,
 * which no executor carries (red case {@code day-kind-reference-other-day}). "De termijn van een Schouwer … als zijn
 * vinddatum plus zijn wachttijd", in schouwdagen, plays beside the unused hoogschouwdag in both orders of the two
 * definitions: S1 (2026-03-10 plus 2) 2026-03-12, S2 (2026-02-27 plus 2) 2026-03-02. A rule that steps in
 * hoogschouwdagen is refused by name, and so is one that steps in havendagen, whose definition reads the parameter
 * openingsdatum, which WF-031 admits and this branch does not read.
 *
 * <p><b>D</b>, a duration in whole springtijdagen between two date-times, is refused by name, as the reference
 * interpreter and the ASP backend refuse it (ch. 3.8, 3.12).
 *
 * <p><b>Measured</b> (2026-10-09, every document of {@link #main}, every order; the verifier passes every one): the
 * reference interpreter's runner and the ASP runner pass A in all 120 orders (21 expectations), A2 in all 6 (7), B's
 * transient case in both orders (3) and C's playing document in all 6 orders of its definitions (3). On B's case V3
 * both fault, "EVALUATION FAULT: day-kind walk: no day of the kind within 40000 calendar days of 2027-06-01 (the walk
 * reaches no further; a day kind whose next occurrence lies beyond the reach is refused, not diagnosed)" and
 * "UNSATISFIABLE — a fail-fast guard fired: viol("day-kind walk: no day of the kind within 40000 calendar days …".
 * With hoogschouwdagen both refuse, "a nested day-kind test on a date other than the bound day is a recognised,
 * unexecuted form (the §9.8 composition is carried on the bound day only)", and the duration of D, "duration in a day
 * kind over datum-tijd operands is a recognised, unexecuted form …". With havendagen both compute the streefdatum,
 * 2026-03-01 from 2026-01-10 plus 2 with the openingsdatum 2026-01-01: this branch does not read the parameter, as the
 * card decides. With a meldtijdstip as input on G1, which a rule computes, the ASP runner refused A2 ("a case-supplied
 * datum-tijd computed target … has no encoding here"), so A2 carries none.
 *
 * <p><b>Red</b> on 0e879163, the code before the change: every test, on the refusal of every step in a day kind,
 * "Excluded: tms-transpiler SKIP: regel: termijn — een duur in dagsoorten ('zijn vinddatum plus zijn wachttijd'):
 * dagsoorten zijn buiten de proef van de truth-maintenance-tak", the same for the uiterste datum and the sluittijdstip,
 * in C also where the expected refusal names hoogtermijn, and for D "… een duration in dagsoorten ('de tijdsduur van
 * zijn meldtijdstip tot zijn sluittijdstip in hele springtijdagen'): dagsoorten zijn buiten de proef van de
 * truth-maintenance-tak".
 */
class TmsDayKindWitnessTest {

    // ---------------- builders ----------------

    static final String THE_DAY = "{\"node\":\"BoundDay\",\"type\":" + DATE + ",\"source\":{\"snippet\":\"de dag\"}}";

    /** A day kind of the model, with the plural that spells its unit. */
    static String dayKind(String naam, String plural) {
        return "{\"node\":\"DayKind\",\"name\":\"" + naam + "\",\"plural\":\"" + plural + "\"}";
    }

    /** "Een dag is een {@code naam} indien {@code condition}", always valid. */
    static String definition(String naam, String condition) {
        return "{\"node\":\"DayKindDefinition\",\"dayKind\":\"" + naam + "\",\"condition\":" + condition
                + ",\"validity\":{\"node\":\"Validity\"}}";
    }

    /** The unit of a count of days of the day kind whose plural is {@code meervoud}. */
    static String dayKindUnit(String plural) {
        return unit(plural, null, plural, 1, 1);
    }

    /** A whole number of days of the day kind whose plural is {@code meervoud}. */
    static String dayKindNumber(String plural) {
        return numberType(true, 0, dayKindUnit(plural));
    }

    /** "de dag {@code op} {@code datum}", a comparison of the bound day with a date literal. */
    static String dayComparison(String op, String date) {
        return "{\"node\":\"Comparison\",\"op\":\"" + op + "\",\"left\":" + THE_DAY + ",\"right\":" + literal(date, DATE)
                + ",\"source\":{\"snippet\":\"de dag " + op + " " + date + "\"}}";
    }

    /** "de dag uit de dag gelijk is aan {@code n}". */
    static String dayNumber(String n) {
        String day = "{\"node\":\"BuiltinCall\",\"kind\":\"dayOf\",\"arguments\":[" + THE_DAY + "],\"type\":" + COUNTING
                + ",\"source\":{\"snippet\":\"de dag uit de dag\"}}";
        return "{\"node\":\"Comparison\",\"op\":\"equal\",\"left\":" + day + ",\"right\":" + TmsDocuments.number(n)
                + ",\"source\":{\"snippet\":\"de dag uit de dag gelijk is aan " + n + "\"}}";
    }

    /** A date step, {@code op} plus or minus, of {@code datum} by {@code duur}, with the result {@code type}. */
    static String dateStep(String op, String date, String duration, String type, String source) {
        return "{\"node\":\"Binary\",\"op\":\"" + op + "\",\"left\":" + date + ",\"right\":" + duration + ",\"type\":" + type
                + ",\"source\":{\"snippet\":\"" + source + "\"}}";
    }

    /** A duration, {@code kind} {@code duration} or {@code absoluteDuration}, from {@code van} to {@code tot}. */
    static String duration(String kind, String van, String to, String type, String source) {
        return TmsDocuments.builtin(kind, List.of(van, to), type, source);
    }

    /** A model of {@code objectTypes} with {@code parameters} and the day kinds {@code dayKinds}. */
    static String model(List<String> objectTypes, List<String> parameters, List<String> dayKinds) {
        return "{\"objectTypes\":[" + String.join(",", objectTypes) + "],\"factTypes\":[],\"parameters\":["
                + String.join(",", parameters) + "],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],"
                + "\"dayKinds\":[" + String.join(",", dayKinds) + "]}";
    }

    /** The text of the document of {@code model} with {@code rules} and the day-kind {@code definitions}. */
    static String ir(String model, List<String> rules, List<String> definitions) {
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + model + ",\"rules\":{\"rules\":[" + String.join(",", rules)
                + "],\"dayKindDefinitions\":[" + String.join(",", definitions) + "],\"objectCreations\":[],"
                + "\"allocations\":[],\"consistencies\":[],\"uniquenesses\":[],\"factCreations\":[],"
                + "\"startPointDeterminations\":[],\"ruleGroups\":[],\"prioritizedDefaults\":[]},\"diagnostics\":[]}";
    }

    static Path document(String model, List<String> rules, List<String> definitions) throws Exception {
        Path tmp = Files.createTempFile("tms-dagsoort", ".json");
        tmp.toFile().deleteOnExit();
        return Files.writeString(tmp, ir(model, rules, definitions));
    }

    /** The case of {@code instances} with the parameters {@code parameters}, wire values by name. */
    static JsonNode caseDocument(List<String> instances, Map<String, String> parameters, List<String> expectations)
            throws Exception {
        List<String> p = new ArrayList<>();
        parameters.forEach((naam, value) -> p.add("\"" + naam + "\":" + value));
        return TmsDocuments.JSON.readTree("{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":["
                + String.join(",", instances) + "],\"parameters\":{" + String.join(",", p) + "}},\"expected\":["
                + String.join(",", expectations) + "]}");
    }

    static String tijdstip(String iso) {
        return "{\"dateTime\":\"" + iso + "\"}";
    }

    /** Every order of {@code rules}, and within each every order of {@code definitions}, replayed on {@code caseDocument}
     *  through the exec-model branch: "[rule names] [definition names] " and what the replay gives. */
    static List<String> inEveryOrder(String model, List<String> rules, List<String> definitions, JsonNode caseDocument)
            throws Exception {
        List<String> out = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(rules))
            for (List<String> defs : TmsDocuments.permutations(definitions))
                out.add(TmsDocuments.ruleNames(order) + " " + definitionNames(defs) + " " + TmsFaultWitnessTest.play(
                        document(model, order, defs), caseDocument, DroolsBackend.Branch.EXEC_MODEL));
        return out;
    }

    /** What {@link #inEveryOrder} gives where every order gives {@code uitkomst}. */
    static List<String> everywhere(List<String> rules, List<String> definitions, String outcome) throws Exception {
        List<String> out = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(rules))
            for (List<String> defs : TmsDocuments.permutations(definitions))
                out.add(TmsDocuments.ruleNames(order) + " " + definitionNames(defs) + " " + outcome);
        return out;
    }

    static List<String> definitionNames(List<String> definitions) throws Exception {
        List<String> out = new ArrayList<>();
        for (String d : definitions) out.add(TmsDocuments.JSON.readTree(d).path("dayKind").asText());
        return out;
    }

    // ---------------- A: a date derived later ----------------

    static final String SPRINGTIJDAG = definition("springtijdag", TmsDocuments.compound("atLeast", 1,
            List.of(dayNumber("1"), dayNumber("15"))));
    static final String SPRINGTIJ = dayKindNumber("springtijdagen");

    static final String MODEL_A = model(List.of(TmsDocuments.objectType("Bergingsploeg", List.of(
            attribute("vinddatum", DATE), attribute("meldingsdatum", DATE), attribute("bestek", SPRINGTIJ),
            attribute("opleverdatum", DATE), attribute("uiterste datum", DATE), attribute("vroegste datum", DATE),
            attribute("spanne", SPRINGTIJ), attribute("afstand", SPRINGTIJ)), List.of())), List.of(),
            List.of(dayKind("springtijdag", "springtijdagen")));

    static String ploegRead(String attribute) {
        return ownRead(attribute, "Bergingsploeg");
    }

    static final String MELDINGSDATUM = computingRule("meldingsdatum", "BEREKENING", "Bergingsploeg", "meldingsdatum",
            dateStep("plus", ploegRead("vinddatum"), literal("2", numberType(true, 0, unit("dagen"))), DATE,
                    "zijn vinddatum plus 2 dagen"), "null");
    static final String UITERSTE = computingRule("uiterste datum", "BEREKENING", "Bergingsploeg", "uiterste datum",
            dateStep("plus", ploegRead("meldingsdatum"), ploegRead("bestek"), DATE,
                    "zijn meldingsdatum plus zijn bestek"), "null");
    static final String VROEGSTE = computingRule("vroegste datum", "BEREKENING", "Bergingsploeg", "vroegste datum",
            dateStep("minus", ploegRead("meldingsdatum"), ploegRead("bestek"), DATE,
                    "zijn meldingsdatum min zijn bestek"), "null");
    static final String SPANNE = computingRule("spanne", "BEREKENING", "Bergingsploeg", "spanne",
            duration("duration", ploegRead("meldingsdatum"), ploegRead("opleverdatum"), SPRINGTIJ,
                    "de tijdsduur van zijn meldingsdatum tot zijn opleverdatum in hele springtijdagen"), "null");
    static final String AFSTAND = computingRule("afstand", "BEREKENING", "Bergingsploeg", "afstand",
            duration("absoluteDuration", ploegRead("opleverdatum"), ploegRead("meldingsdatum"), SPRINGTIJ,
                    "de absolute tijdsduur van zijn opleverdatum tot zijn meldingsdatum in hele springtijdagen"), "null");

    static final List<String> RULES_A = List.of(MELDINGSDATUM, UITERSTE, VROEGSTE, SPANNE, AFSTAND);

    static JsonNode caseA() throws Exception {
        List<String> expected = new ArrayList<>();
        Map<String, String[]> data = new LinkedHashMap<>();
        // label: meldingsdatum, uiterste, vroegste, spanne, afstand
        data.put("P1", new String[]{"2026-03-12", "2026-04-15", "2026-02-01", "5", "5"});
        data.put("P2", new String[]{"2027-01-01", "2027-01-01", "2027-01-01", "0", "0"});
        data.put("P3", new String[]{null, null, null, null, null});
        data.put("P4", new String[]{"2026-01-15", "2026-01-15", "2026-01-15", "-1", "1"});
        data.forEach((p, v) -> {
            expected.add(v[0] == null ? value(p, "meldingsdatum", null) : valueJson(p, "meldingsdatum", dateValue(v[0])));
            expected.add(v[1] == null ? value(p, "uiterste datum", null)
                    : valueJson(p, "uiterste datum", dateValue(v[1])));
            expected.add(v[2] == null ? value(p, "vroegste datum", null)
                    : valueJson(p, "vroegste datum", dateValue(v[2])));
            expected.add(value(p, "spanne", v[3]));
            expected.add(value(p, "afstand", v[4]));
        });
        expected.add(consistent());
        return caseDocument(List.of(
                instanceWith("P1", "Bergingsploeg", Map.of("vinddatum", dateValue("2026-03-10"), "meldingsdatum",
                        dateValue("2026-06-01"), "bestek", numberValue("3"), "opleverdatum", dateValue("2026-05-20")),
                        List.of()),
                instanceWith("P2", "Bergingsploeg", Map.of("vinddatum", dateValue("2026-12-30"), "opleverdatum",
                        dateValue("2027-01-01")), List.of()),
                instanceWith("P3", "Bergingsploeg", Map.of("bestek", numberValue("2"), "opleverdatum",
                        dateValue("2026-05-20")), List.of()),
                instanceWith("P4", "Bergingsploeg", Map.of("vinddatum", dateValue("2026-01-13"), "bestek",
                        numberValue("0"), "opleverdatum", dateValue("2026-01-01")), List.of())),
                Map.of(), expected);
    }

    @Test void a_step_and_a_duration_over_a_later_derived_date_in_every_order() throws Exception {
        JsonNode caseDocument = caseA();
        List<String> definitions = List.of(SPRINGTIJDAG);
        assertEquals(everywhere(RULES_A, definitions, "gespeeld"), inEveryOrder(MODEL_A, RULES_A, definitions, caseDocument));
    }

    // ---------------- A2: a date-time derived later ----------------

    static final String MODEL_A2 = model(List.of(TmsDocuments.objectType("Getijpost", List.of(
            attribute("aanvraagtijdstip", DATE_TIME), attribute("meldtijdstip", DATE_TIME),
            attribute("wachttermijn", SPRINGTIJ), attribute("sluittijdstip", DATE_TIME),
            attribute("openingstijdstip", DATE_TIME)), List.of())), List.of(),
            List.of(dayKind("springtijdag", "springtijdagen")));

    static final String MELDTIJDSTIP = computingRule("meldtijdstip", "BEREKENING", "Getijpost", "meldtijdstip",
            dateStep("plus", ownRead("aanvraagtijdstip", "Getijpost"), literal("3", numberType(true, 0, unit("uur"))),
                    DATE_TIME, "zijn aanvraagtijdstip plus 3 uur"), "null");
    static final String SLUITTIJDSTIP = computingRule("sluittijdstip", "BEREKENING", "Getijpost", "sluittijdstip",
            dateStep("plus", ownRead("meldtijdstip", "Getijpost"), ownRead("wachttermijn", "Getijpost"), DATE_TIME,
                    "zijn meldtijdstip plus zijn wachttermijn"), "null");
    static final String OPENINGSTIJDSTIP = computingRule("openingstijdstip", "BEREKENING", "Getijpost", "openingstijdstip",
            dateStep("minus", ownRead("meldtijdstip", "Getijpost"), ownRead("wachttermijn", "Getijpost"), DATE_TIME,
                    "zijn meldtijdstip min zijn wachttermijn"), "null");

    static final List<String> RULES_A2 = List.of(MELDTIJDSTIP, SLUITTIJDSTIP, OPENINGSTIJDSTIP);

    static JsonNode caseA2() throws Exception {
        return caseDocument(List.of(
                instanceWith("G1", "Getijpost", Map.of("aanvraagtijdstip", tijdstip("2026-03-10T21:15:30"),
                        "wachttermijn", numberValue("2")), List.of()),
                instanceWith("G2", "Getijpost", Map.of("aanvraagtijdstip", tijdstip("2026-03-14T23:00:00"),
                        "wachttermijn", numberValue("1")), List.of())),
                Map.of(), List.of(
                        valueJson("G1", "meldtijdstip", tijdstip("2026-03-11T00:15:30")),
                        valueJson("G1", "sluittijdstip", tijdstip("2026-04-01T00:15:30")),
                        valueJson("G1", "openingstijdstip", tijdstip("2026-02-15T00:15:30")),
                        valueJson("G2", "meldtijdstip", tijdstip("2026-03-15T02:00:00")),
                        valueJson("G2", "sluittijdstip", tijdstip("2026-04-01T02:00:00")),
                        valueJson("G2", "openingstijdstip", tijdstip("2026-03-01T02:00:00")),
                        consistent()));
    }

    @Test void a_step_over_a_later_derived_date_time_in_every_order() throws Exception {
        JsonNode caseDocument = caseA2();
        List<String> definitions = List.of(SPRINGTIJDAG);
        assertEquals(everywhere(RULES_A2, definitions, "gespeeld"), inEveryOrder(MODEL_A2, RULES_A2, definitions, caseDocument));
    }

    // ---------------- B: the reach of the walk ----------------

    static final String SEIZOENSDAG = definition("seizoensdag", TmsDocuments.compound("all", 0,
            List.of(dayComparison("later", "2025-12-31"), dayComparison("earlier", "2027-01-01"))));

    static final String MODEL_B = model(List.of(TmsDocuments.objectType("Vondst", List.of(
            attribute("vinddatum", DATE), attribute("wachttijd", dayKindNumber("seizoensdagen")),
            attribute("termijn", DATE)), List.of("gemeld", "erkend"))), List.of(),
            List.of(dayKind("seizoensdag", "seizoensdagen")));

    static final String ERKENNING = characteristicRule("erkenning", "Vondst", "erkend", check("gemeld", false));
    static final String TERM = computingRule("termijn", "BEREKENING", "Vondst", "termijn",
            dateStep("plus", ownRead("vinddatum", "Vondst"), ownRead("wachttijd", "Vondst"), DATE,
                    "zijn vinddatum plus zijn wachttijd"), check("erkend", true));

    static final List<String> RULES_B = List.of(ERKENNING, TERM);

    static final String OUTSIDE_RANGE = "ComputeFault: fout in de regel \"termijn\": day-kind walk: no day of the kind"
            + " within 40000 calendar days of 2027-06-01 (the walk reaches no further; a day kind whose next occurrence"
            + " lies beyond the reach is refused, not diagnosed)";

    static JsonNode caseB() throws Exception {
        return caseDocument(List.of(
                instanceWith("V1", "Vondst", Map.of("vinddatum", dateValue("2027-06-01"), "wachttijd", numberValue("5")),
                        List.of("gemeld")),
                instanceWith("V2", "Vondst", Map.of("vinddatum", dateValue("2026-03-01"), "wachttijd", numberValue("5")),
                        List.of())),
                Map.of(), List.of(value("V1", "termijn", null),
                        valueJson("V2", "termijn", dateValue("2026-03-06")), consistent()));
    }

    static JsonNode caseBFixpoint() throws Exception {
        return caseDocument(List.of(instanceWith("V3", "Vondst", Map.of("vinddatum", dateValue("2027-06-01"), "wachttijd",
                numberValue("5")), List.of())), Map.of(), List.of(consistent()));
    }

    @Test void the_reach_fault_on_an_intermediate_state_lapses_and_stays_at_the_fixpoint() throws Exception {
        List<String> definitions = List.of(SEIZOENSDAG);
        assertEquals(everywhere(RULES_B, definitions, "gespeeld"), inEveryOrder(MODEL_B, RULES_B, definitions, caseB()));
        assertEquals(everywhere(RULES_B, definitions, OUTSIDE_RANGE),
                inEveryOrder(MODEL_B, RULES_B, definitions, caseBFixpoint()));
    }

    @Test void the_reach_fault_of_an_intermediate_state_is_withdrawn() throws Exception {
        Path ir = document(MODEL_B, RULES_B, List.of(SEIZOENSDAG));
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        int intermediateState = 0;
        for (List<Rule> order : TmsDocuments.sample(b.execModelTranspiler().model().getRules(), 20261009L, 100)) {
            List<String> names = order.stream().map(Rule::getName).toList();
            Object v1 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("wachttijd", "5"), List.of("gemeld"));
            v1.getClass().getMethod(dev.uchar.regelspraak.transpiler.core.Namen.setter("vinddatum"), LocalDate.class)
                    .invoke(v1, LocalDate.of(2027, 6, 1));
            TmsLetWitnessTest.Facts facts = new TmsLetWitnessTest.Facts();
            assertNull(TmsDocuments.fixpoint(b, order, List.of(v1), session -> session.addEventListener(facts)),
                    () -> "volgorde " + names);
            assertEquals(facts.inserted, facts.withdrawn, () -> "foutfeiten in volgorde " + names);
            assertNull(TmsDocuments.read(v1, "termijn"), () -> "termijn in volgorde " + names);
            if (facts.faults > 0) intermediateState++;
        }
        assertTrue(intermediateState > 0, "geen volgorde liet de termijn vóór de erkenning rekenen");
    }

    // ---------------- C: definitions that do not compile ----------------

    static final String SCHOUWDAG = definition("schouwdag", TmsDocuments.compound("all", 0,
            List.of(dayComparison("later", "2026-02-28"), dayComparison("earlier", "2026-10-01"))));
    static final String HOOGSCHOUWDAG = definition("hoogschouwdag", "{\"node\":\"Predicate\",\"kind\":\"dayKind\","
            + "\"operand\":" + literal("2026-06-21", DATE) + ",\"dayKind\":\"schouwdag\",\"negated\":false,\"digits\":0,"
            + "\"source\":{\"snippet\":\"21-06-2026 een schouwdag is\"}}");
    static final String HAVENDAG = definition("havendag", TmsDocuments.compound("all", 0, List.of(
            "{\"node\":\"Comparison\",\"op\":\"later\",\"left\":" + THE_DAY + ",\"right\":"
                    + TmsDocuments.parameterRead("openingsdatum") + ",\"source\":{\"snippet\":\"de dag later is dan"
                    + " de openingsdatum\"}}", dayNumber("1"))));

    static final String MODEL_C = model(List.of(TmsDocuments.objectType("Schouwer", List.of(
            attribute("vinddatum", DATE), attribute("wachttijd", dayKindNumber("schouwdagen")),
            attribute("hoogwachttijd", dayKindNumber("hoogschouwdagen")),
            attribute("doorlooptijd", dayKindNumber("havendagen")), attribute("termijn", DATE),
            attribute("hoogtermijn", DATE), attribute("streefdatum", DATE)), List.of())),
            List.of(TmsDocuments.parameter("openingsdatum", DATE)),
            List.of(dayKind("schouwdag", "schouwdagen"), dayKind("hoogschouwdag", "hoogschouwdagen"),
                    dayKind("havendag", "havendagen")));

    static String schouwerStep(String naam, String target, String duration) {
        return computingRule(naam, "BEREKENING", "Schouwer", target, dateStep("plus", ownRead("vinddatum", "Schouwer"),
                ownRead(duration, "Schouwer"), DATE, "zijn vinddatum plus zijn " + duration), "null");
    }

    static final String SCHOUW_TERM = schouwerStep("termijn", "termijn", "wachttijd");
    static final String HIGH_TERM = schouwerStep("hoogtermijn", "hoogtermijn", "hoogwachttijd");
    static final String STREEFDATUM = schouwerStep("streefdatum", "streefdatum", "doorlooptijd");

    static JsonNode caseC() throws Exception {
        return caseDocument(List.of(
                instanceWith("S1", "Schouwer", Map.of("vinddatum", dateValue("2026-03-10"), "wachttijd",
                        numberValue("2"), "hoogwachttijd", numberValue("1"), "doorlooptijd", numberValue("2")), List.of()),
                instanceWith("S2", "Schouwer", Map.of("vinddatum", dateValue("2026-02-27"), "wachttijd",
                        numberValue("2")), List.of())),
                Map.of("openingsdatum", dateValue("2026-01-01")),
                List.of(valueJson("S1", "termijn", dateValue("2026-03-12")),
                        valueJson("S2", "termijn", dateValue("2026-03-02")), consistent()));
    }

    @Test void an_unused_definition_that_is_not_to_compile_lets_the_document_play() throws Exception {
        List<String> definitions = List.of(SCHOUWDAG, HOOGSCHOUWDAG, HAVENDAG);
        assertEquals(everywhere(List.of(SCHOUW_TERM), definitions, "gespeeld"),
                inEveryOrder(MODEL_C, List.of(SCHOUW_TERM), definitions, caseC()));
    }

    @Test void a_form_with_a_not_to_compile_definition_used_is_refused_by_name() throws Exception {
        List<String> definitions = List.of(SCHOUWDAG, HOOGSCHOUWDAG, HAVENDAG);
        assertEquals("transpiler SKIP: regel: hoogtermijn — een duur in dagsoorten ('zijn vinddatum plus zijn"
                        + " hoogwachttijd'): de definitie van de dagsoort 'hoogschouwdag' toetst de dagsoort 'schouwdag' op"
                        + " een andere dag dan de gebonden dag ('21-06-2026 een schouwdag is'), een vorm die geen executor"
                        + " uitvoert (RUNTIME-SEMANTICS ch. 3.8)",
                refusal(document(MODEL_C, List.of(SCHOUW_TERM, HIGH_TERM), definitions)));
        assertEquals("transpiler SKIP: regel: streefdatum — een duur in dagsoorten ('zijn vinddatum plus zijn"
                        + " doorlooptijd'): de definitie van de dagsoort 'havendag' leest de parameter 'openingsdatum',"
                        + " die deze tak in een dagsoortdefinitie niet leest (WF-031 laat haar toe)",
                refusal(document(MODEL_C, List.of(SCHOUW_TERM, STREEFDATUM), definitions)));
    }

    // ---------------- D: a duration in a day kind between two date-times ----------------

    static final String MODEL_D = model(List.of(TmsDocuments.objectType("Getijpost", List.of(
            attribute("meldtijdstip", DATE_TIME), attribute("sluittijdstip", DATE_TIME), attribute("getijspanne", SPRINGTIJ)),
            List.of())), List.of(), List.of(dayKind("springtijdag", "springtijdagen")));
    static final String GETIJSPANNE = computingRule("getijspanne", "BEREKENING", "Getijpost", "getijspanne",
            duration("duration", ownRead("meldtijdstip", "Getijpost"), ownRead("sluittijdstip", "Getijpost"), SPRINGTIJ,
                    "de tijdsduur van zijn meldtijdstip tot zijn sluittijdstip in hele springtijdagen"), "null");

    @Test void a_duration_in_day_kinds_between_two_date_times_is_refused_by_name() throws Exception {
        assertEquals("transpiler SKIP: regel: getijspanne — een duration in dagsoorten ('de tijdsduur van zijn"
                        + " meldtijdstip tot zijn sluittijdstip in hele springtijdagen') tussen twee datum-tijden: de"
                        + " referentie-interpreter en de ASP-backend weigeren haar met naam, omdat het tellen van hele"
                        + " dagen van een soort tussen twee tijdstippen een regel voor de deeldag aan elk eind vraagt,"
                        + " die RUNTIME-SEMANTICS ch. 3.8 niet vastlegt (ch. 3.12)",
                refusal(document(MODEL_D, List.of(GETIJSPANNE), List.of(SPRINGTIJDAG))));
    }

    /** The refusal of the document {@code ir} by the exec-model branch, as the build raises it. */
    static String refusal(Path ir) {
        return assertThrows(CaseRunner.Excluded.class,
                () -> DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL)).getMessage();
    }

    // ---------------- the documents, for the other executors ----------------

    /** Writes every document of this class, in every order of its rules and definitions, with its case, under
     *  {@code args[0]}: a map per part and order, holding {@code ir.json} and {@code case.json}, for the verifier, ref
     *  and clingo. The refused documents of C and D are written once, with a case of what the reference interpreter
     *  would compute where it carries the form. */
    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        writeAll(out.resolve("a-datum"), MODEL_A, RULES_A, List.of(SPRINGTIJDAG), caseA());
        writeAll(out.resolve("a2-tijdstip"), MODEL_A2, RULES_A2, List.of(SPRINGTIJDAG), caseA2());
        writeAll(out.resolve("b-tussenstand"), MODEL_B, RULES_B, List.of(SEIZOENSDAG), caseB());
        writeAll(out.resolve("b-fixpunt"), MODEL_B, RULES_B, List.of(SEIZOENSDAG), caseBFixpoint());
        List<String> c = List.of(SCHOUWDAG, HOOGSCHOUWDAG, HAVENDAG);
        writeAll(out.resolve("c-ongebruikt"), MODEL_C, List.of(SCHOUW_TERM), c, caseC());
        JsonNode high = caseDocument(List.of(instanceWith("S1", "Schouwer", Map.of("vinddatum", dateValue("2026-03-10"),
                "hoogwachttijd", numberValue("1")), List.of())), Map.of("openingsdatum", dateValue("2026-01-01")),
                List.of(consistent()));
        TmsTimelineWitnessTest.write(out.resolve("c-gebruikt"), ir(MODEL_C, List.of(HIGH_TERM), c), high);
        JsonNode haven = caseDocument(List.of(instanceWith("S1", "Schouwer", Map.of("vinddatum", dateValue("2026-01-10"),
                "doorlooptijd", numberValue("2")), List.of())), Map.of("openingsdatum", dateValue("2026-01-01")),
                List.of(valueJson("S1", "streefdatum", dateValue("2026-03-01")), consistent()));
        TmsTimelineWitnessTest.write(out.resolve("c-parameter"), ir(MODEL_C, List.of(STREEFDATUM), c), haven);
        JsonNode getij = caseDocument(List.of(instanceWith("G1", "Getijpost", Map.of("meldtijdstip", tijdstip("2026-03-10T09:00:00"),
                "sluittijdstip", tijdstip("2026-04-02T09:00:00")), List.of())), Map.of(), List.of(consistent()));
        TmsTimelineWitnessTest.write(out.resolve("d-datumtijdduur"), ir(MODEL_D, List.of(GETIJSPANNE),
                List.of(SPRINGTIJDAG)), getij);
    }

    static void writeAll(Path map, String model, List<String> rules, List<String> definitions, JsonNode caseDocument)
            throws Exception {
        int n = 0;
        for (List<String> order : TmsDocuments.permutations(rules))
            for (List<String> defs : TmsDocuments.permutations(definitions))
                TmsTimelineWitnessTest.write(map.resolve(String.format("v%03d", n++)), ir(model, order, defs), caseDocument);
    }
}
