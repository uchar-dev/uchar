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

import static dev.uchar.regelspraak.cases.TmsDocumenten.DATUM;
import static dev.uchar.regelspraak.cases.TmsDocumenten.DATUMTIJD;
import static dev.uchar.regelspraak.cases.TmsDocumenten.TELLING;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.consistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.datumwaarde;
import static dev.uchar.regelspraak.cases.TmsDocumenten.eenheid;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getaltype;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getalwaarde;
import static dev.uchar.regelspraak.cases.TmsDocumenten.instantieMet;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.literaal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waarde;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waardeJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Day kinds on the truth-maintenance branch (RUNTIME-SEMANTICS ch. 3.8, IR-SPEC ch. 3.7, WF-031): a date or a
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
 * which no executor carries (red case {@code dagsoortverwijzing-andere-dag}). "De termijn van een Schouwer … als zijn
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
class TmsDagsoortWitnessTest {

    // ---------------- builders ----------------

    static final String DE_DAG = "{\"node\":\"BoundDay\",\"type\":" + DATUM + ",\"source\":{\"snippet\":\"de dag\"}}";

    /** A day kind of the model, with the plural that spells its unit. */
    static String dagsoort(String naam, String meervoud) {
        return "{\"node\":\"DayKind\",\"name\":\"" + naam + "\",\"plural\":\"" + meervoud + "\"}";
    }

    /** "Een dag is een {@code naam} indien {@code voorwaarde}", always valid. */
    static String definitie(String naam, String voorwaarde) {
        return "{\"node\":\"DayKindDefinition\",\"dayKind\":\"" + naam + "\",\"condition\":" + voorwaarde
                + ",\"validity\":{\"node\":\"Validity\"}}";
    }

    /** The unit of a count of days of the day kind whose plural is {@code meervoud}. */
    static String dagsoorteenheid(String meervoud) {
        return eenheid(meervoud, null, meervoud, 1, 1);
    }

    /** A whole number of days of the day kind whose plural is {@code meervoud}. */
    static String dagsoortgetal(String meervoud) {
        return getaltype(true, 0, dagsoorteenheid(meervoud));
    }

    /** "de dag {@code op} {@code datum}", a comparison of the bound day with a date literal. */
    static String dagVergelijking(String op, String datum) {
        return "{\"node\":\"Comparison\",\"op\":\"" + op + "\",\"left\":" + DE_DAG + ",\"right\":" + literaal(datum, DATUM)
                + ",\"source\":{\"snippet\":\"de dag " + op + " " + datum + "\"}}";
    }

    /** "de dag uit de dag gelijk is aan {@code n}". */
    static String dagnummer(String n) {
        String dag = "{\"node\":\"BuiltinCall\",\"kind\":\"dayOf\",\"arguments\":[" + DE_DAG + "],\"type\":" + TELLING
                + ",\"source\":{\"snippet\":\"de dag uit de dag\"}}";
        return "{\"node\":\"Comparison\",\"op\":\"equal\",\"left\":" + dag + ",\"right\":" + TmsDocumenten.getal(n)
                + ",\"source\":{\"snippet\":\"de dag uit de dag gelijk is aan " + n + "\"}}";
    }

    /** A date step, {@code op} plus or minus, of {@code datum} by {@code duur}, with the result {@code type}. */
    static String datumstap(String op, String datum, String duur, String type, String bron) {
        return "{\"node\":\"Binary\",\"op\":\"" + op + "\",\"left\":" + datum + ",\"right\":" + duur + ",\"type\":" + type
                + ",\"source\":{\"snippet\":\"" + bron + "\"}}";
    }

    /** A duration, {@code kind} {@code duration} or {@code absoluteDuration}, from {@code van} to {@code tot}. */
    static String duur(String kind, String van, String tot, String type, String bron) {
        return TmsDocumenten.builtin(kind, List.of(van, tot), type, bron);
    }

    /** A model of {@code objecttypen} with {@code parameters} and the day kinds {@code dagsoorten}. */
    static String model(List<String> objecttypen, List<String> parameters, List<String> dagsoorten) {
        return "{\"objectTypes\":[" + String.join(",", objecttypen) + "],\"factTypes\":[],\"parameters\":["
                + String.join(",", parameters) + "],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],"
                + "\"dayKinds\":[" + String.join(",", dagsoorten) + "]}";
    }

    /** The text of the document of {@code model} with {@code regels} and the day-kind {@code definities}. */
    static String ir(String model, List<String> regels, List<String> definities) {
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + model + ",\"rules\":{\"rules\":[" + String.join(",", regels)
                + "],\"dayKindDefinitions\":[" + String.join(",", definities) + "],\"objectCreations\":[],"
                + "\"allocations\":[],\"consistencies\":[],\"uniquenesses\":[],\"factCreations\":[],"
                + "\"startPointDeterminations\":[],\"ruleGroups\":[],\"prioritizedDefaults\":[]},\"diagnostics\":[]}";
    }

    static Path document(String model, List<String> regels, List<String> definities) throws Exception {
        Path tmp = Files.createTempFile("tms-dagsoort", ".json");
        tmp.toFile().deleteOnExit();
        return Files.writeString(tmp, ir(model, regels, definities));
    }

    /** The case of {@code instanties} with the parameters {@code parameters}, wire values by name. */
    static JsonNode zaak(List<String> instanties, Map<String, String> parameters, List<String> verwachtingen)
            throws Exception {
        List<String> p = new ArrayList<>();
        parameters.forEach((naam, waarde) -> p.add("\"" + naam + "\":" + waarde));
        return TmsDocumenten.JSON.readTree("{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":["
                + String.join(",", instanties) + "],\"parameters\":{" + String.join(",", p) + "}},\"expected\":["
                + String.join(",", verwachtingen) + "]}");
    }

    static String tijdstip(String iso) {
        return "{\"dateTime\":\"" + iso + "\"}";
    }

    /** Every order of {@code regels}, and within each every order of {@code definities}, replayed on {@code zaak}
     *  through the truth-maintenance branch: "[rule names] [definition names] " and what the replay gives. */
    static List<String> inElkeVolgorde(String model, List<String> regels, List<String> definities, JsonNode zaak)
            throws Exception {
        List<String> uit = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(regels))
            for (List<String> defs : TmsDocumenten.permutaties(definities))
                uit.add(TmsDocumenten.regelnamen(volgorde) + " " + definitienamen(defs) + " " + TmsFoutWitnessTest.speel(
                        document(model, volgorde, defs), zaak, DroolsBackend.Branch.TRUTH_MAINTENANCE));
        return uit;
    }

    /** What {@link #inElkeVolgorde} gives where every order gives {@code uitkomst}. */
    static List<String> overal(List<String> regels, List<String> definities, String uitkomst) throws Exception {
        List<String> uit = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(regels))
            for (List<String> defs : TmsDocumenten.permutaties(definities))
                uit.add(TmsDocumenten.regelnamen(volgorde) + " " + definitienamen(defs) + " " + uitkomst);
        return uit;
    }

    static List<String> definitienamen(List<String> definities) throws Exception {
        List<String> uit = new ArrayList<>();
        for (String d : definities) uit.add(TmsDocumenten.JSON.readTree(d).path("dayKind").asText());
        return uit;
    }

    // ---------------- A: a date derived later ----------------

    static final String SPRINGTIJDAG = definitie("springtijdag", TmsDocumenten.samengesteld("atLeast", 1,
            List.of(dagnummer("1"), dagnummer("15"))));
    static final String SPRINGTIJ = dagsoortgetal("springtijdagen");

    static final String MODEL_A = model(List.of(TmsDocumenten.objecttype("Bergingsploeg", List.of(
            attribuut("vinddatum", DATUM), attribuut("meldingsdatum", DATUM), attribuut("bestek", SPRINGTIJ),
            attribuut("opleverdatum", DATUM), attribuut("uiterste datum", DATUM), attribuut("vroegste datum", DATUM),
            attribuut("spanne", SPRINGTIJ), attribuut("afstand", SPRINGTIJ)), List.of())), List.of(),
            List.of(dagsoort("springtijdag", "springtijdagen")));

    static String ploeglezing(String attribuut) {
        return lezing(attribuut, "Bergingsploeg");
    }

    static final String MELDINGSDATUM = rekenregel("meldingsdatum", "BEREKENING", "Bergingsploeg", "meldingsdatum",
            datumstap("plus", ploeglezing("vinddatum"), literaal("2", getaltype(true, 0, eenheid("dagen"))), DATUM,
                    "zijn vinddatum plus 2 dagen"), "null");
    static final String UITERSTE = rekenregel("uiterste datum", "BEREKENING", "Bergingsploeg", "uiterste datum",
            datumstap("plus", ploeglezing("meldingsdatum"), ploeglezing("bestek"), DATUM,
                    "zijn meldingsdatum plus zijn bestek"), "null");
    static final String VROEGSTE = rekenregel("vroegste datum", "BEREKENING", "Bergingsploeg", "vroegste datum",
            datumstap("minus", ploeglezing("meldingsdatum"), ploeglezing("bestek"), DATUM,
                    "zijn meldingsdatum min zijn bestek"), "null");
    static final String SPANNE = rekenregel("spanne", "BEREKENING", "Bergingsploeg", "spanne",
            duur("duration", ploeglezing("meldingsdatum"), ploeglezing("opleverdatum"), SPRINGTIJ,
                    "de tijdsduur van zijn meldingsdatum tot zijn opleverdatum in hele springtijdagen"), "null");
    static final String AFSTAND = rekenregel("afstand", "BEREKENING", "Bergingsploeg", "afstand",
            duur("absoluteDuration", ploeglezing("opleverdatum"), ploeglezing("meldingsdatum"), SPRINGTIJ,
                    "de absolute tijdsduur van zijn opleverdatum tot zijn meldingsdatum in hele springtijdagen"), "null");

    static final List<String> REGELS_A = List.of(MELDINGSDATUM, UITERSTE, VROEGSTE, SPANNE, AFSTAND);

    static JsonNode zaakA() throws Exception {
        List<String> verwacht = new ArrayList<>();
        Map<String, String[]> data = new LinkedHashMap<>();
        // label: meldingsdatum, uiterste, vroegste, spanne, afstand
        data.put("P1", new String[]{"2026-03-12", "2026-04-15", "2026-02-01", "5", "5"});
        data.put("P2", new String[]{"2027-01-01", "2027-01-01", "2027-01-01", "0", "0"});
        data.put("P3", new String[]{null, null, null, null, null});
        data.put("P4", new String[]{"2026-01-15", "2026-01-15", "2026-01-15", "-1", "1"});
        data.forEach((p, v) -> {
            verwacht.add(v[0] == null ? waarde(p, "meldingsdatum", null) : waardeJson(p, "meldingsdatum", datumwaarde(v[0])));
            verwacht.add(v[1] == null ? waarde(p, "uiterste datum", null)
                    : waardeJson(p, "uiterste datum", datumwaarde(v[1])));
            verwacht.add(v[2] == null ? waarde(p, "vroegste datum", null)
                    : waardeJson(p, "vroegste datum", datumwaarde(v[2])));
            verwacht.add(waarde(p, "spanne", v[3]));
            verwacht.add(waarde(p, "afstand", v[4]));
        });
        verwacht.add(consistent());
        return zaak(List.of(
                instantieMet("P1", "Bergingsploeg", Map.of("vinddatum", datumwaarde("2026-03-10"), "meldingsdatum",
                        datumwaarde("2026-06-01"), "bestek", getalwaarde("3"), "opleverdatum", datumwaarde("2026-05-20")),
                        List.of()),
                instantieMet("P2", "Bergingsploeg", Map.of("vinddatum", datumwaarde("2026-12-30"), "opleverdatum",
                        datumwaarde("2027-01-01")), List.of()),
                instantieMet("P3", "Bergingsploeg", Map.of("bestek", getalwaarde("2"), "opleverdatum",
                        datumwaarde("2026-05-20")), List.of()),
                instantieMet("P4", "Bergingsploeg", Map.of("vinddatum", datumwaarde("2026-01-13"), "bestek",
                        getalwaarde("0"), "opleverdatum", datumwaarde("2026-01-01")), List.of())),
                Map.of(), verwacht);
    }

    @Test void een_stap_en_een_duur_over_een_later_afgeleide_datum_in_elke_volgorde() throws Exception {
        JsonNode zaak = zaakA();
        List<String> definities = List.of(SPRINGTIJDAG);
        assertEquals(overal(REGELS_A, definities, "gespeeld"), inElkeVolgorde(MODEL_A, REGELS_A, definities, zaak));
    }

    // ---------------- A2: a date-time derived later ----------------

    static final String MODEL_A2 = model(List.of(TmsDocumenten.objecttype("Getijpost", List.of(
            attribuut("aanvraagtijdstip", DATUMTIJD), attribuut("meldtijdstip", DATUMTIJD),
            attribuut("wachttermijn", SPRINGTIJ), attribuut("sluittijdstip", DATUMTIJD),
            attribuut("openingstijdstip", DATUMTIJD)), List.of())), List.of(),
            List.of(dagsoort("springtijdag", "springtijdagen")));

    static final String MELDTIJDSTIP = rekenregel("meldtijdstip", "BEREKENING", "Getijpost", "meldtijdstip",
            datumstap("plus", lezing("aanvraagtijdstip", "Getijpost"), literaal("3", getaltype(true, 0, eenheid("uur"))),
                    DATUMTIJD, "zijn aanvraagtijdstip plus 3 uur"), "null");
    static final String SLUITTIJDSTIP = rekenregel("sluittijdstip", "BEREKENING", "Getijpost", "sluittijdstip",
            datumstap("plus", lezing("meldtijdstip", "Getijpost"), lezing("wachttermijn", "Getijpost"), DATUMTIJD,
                    "zijn meldtijdstip plus zijn wachttermijn"), "null");
    static final String OPENINGSTIJDSTIP = rekenregel("openingstijdstip", "BEREKENING", "Getijpost", "openingstijdstip",
            datumstap("minus", lezing("meldtijdstip", "Getijpost"), lezing("wachttermijn", "Getijpost"), DATUMTIJD,
                    "zijn meldtijdstip min zijn wachttermijn"), "null");

    static final List<String> REGELS_A2 = List.of(MELDTIJDSTIP, SLUITTIJDSTIP, OPENINGSTIJDSTIP);

    static JsonNode zaakA2() throws Exception {
        return zaak(List.of(
                instantieMet("G1", "Getijpost", Map.of("aanvraagtijdstip", tijdstip("2026-03-10T21:15:30"),
                        "wachttermijn", getalwaarde("2")), List.of()),
                instantieMet("G2", "Getijpost", Map.of("aanvraagtijdstip", tijdstip("2026-03-14T23:00:00"),
                        "wachttermijn", getalwaarde("1")), List.of())),
                Map.of(), List.of(
                        waardeJson("G1", "meldtijdstip", tijdstip("2026-03-11T00:15:30")),
                        waardeJson("G1", "sluittijdstip", tijdstip("2026-04-01T00:15:30")),
                        waardeJson("G1", "openingstijdstip", tijdstip("2026-02-15T00:15:30")),
                        waardeJson("G2", "meldtijdstip", tijdstip("2026-03-15T02:00:00")),
                        waardeJson("G2", "sluittijdstip", tijdstip("2026-04-01T02:00:00")),
                        waardeJson("G2", "openingstijdstip", tijdstip("2026-03-01T02:00:00")),
                        consistent()));
    }

    @Test void een_stap_over_een_later_afgeleide_datum_tijd_in_elke_volgorde() throws Exception {
        JsonNode zaak = zaakA2();
        List<String> definities = List.of(SPRINGTIJDAG);
        assertEquals(overal(REGELS_A2, definities, "gespeeld"), inElkeVolgorde(MODEL_A2, REGELS_A2, definities, zaak));
    }

    // ---------------- B: the reach of the walk ----------------

    static final String SEIZOENSDAG = definitie("seizoensdag", TmsDocumenten.samengesteld("all", 0,
            List.of(dagVergelijking("later", "2025-12-31"), dagVergelijking("earlier", "2027-01-01"))));

    static final String MODEL_B = model(List.of(TmsDocumenten.objecttype("Vondst", List.of(
            attribuut("vinddatum", DATUM), attribuut("wachttijd", dagsoortgetal("seizoensdagen")),
            attribuut("termijn", DATUM)), List.of("gemeld", "erkend"))), List.of(),
            List.of(dagsoort("seizoensdag", "seizoensdagen")));

    static final String ERKENNING = kenmerkregel("erkenning", "Vondst", "erkend", check("gemeld", false));
    static final String TERMIJN = rekenregel("termijn", "BEREKENING", "Vondst", "termijn",
            datumstap("plus", lezing("vinddatum", "Vondst"), lezing("wachttijd", "Vondst"), DATUM,
                    "zijn vinddatum plus zijn wachttijd"), check("erkend", true));

    static final List<String> REGELS_B = List.of(ERKENNING, TERMIJN);

    static final String BUITEN_BEREIK = "Rekenfout: fout in de regel \"termijn\": day-kind walk: no day of the kind"
            + " within 40000 calendar days of 2027-06-01 (the walk reaches no further; a day kind whose next occurrence"
            + " lies beyond the reach is refused, not diagnosed)";

    static JsonNode zaakB() throws Exception {
        return zaak(List.of(
                instantieMet("V1", "Vondst", Map.of("vinddatum", datumwaarde("2027-06-01"), "wachttijd", getalwaarde("5")),
                        List.of("gemeld")),
                instantieMet("V2", "Vondst", Map.of("vinddatum", datumwaarde("2026-03-01"), "wachttijd", getalwaarde("5")),
                        List.of())),
                Map.of(), List.of(waarde("V1", "termijn", null),
                        waardeJson("V2", "termijn", datumwaarde("2026-03-06")), consistent()));
    }

    static JsonNode zaakBFixpunt() throws Exception {
        return zaak(List.of(instantieMet("V3", "Vondst", Map.of("vinddatum", datumwaarde("2027-06-01"), "wachttijd",
                getalwaarde("5")), List.of())), Map.of(), List.of(consistent()));
    }

    @Test void de_reikwijdtefout_op_een_tussenstand_vervalt_en_aan_het_fixpunt_blijft_ze() throws Exception {
        List<String> definities = List.of(SEIZOENSDAG);
        assertEquals(overal(REGELS_B, definities, "gespeeld"), inElkeVolgorde(MODEL_B, REGELS_B, definities, zaakB()));
        assertEquals(overal(REGELS_B, definities, BUITEN_BEREIK),
                inElkeVolgorde(MODEL_B, REGELS_B, definities, zaakBFixpunt()));
    }

    @Test void de_reikwijdtefout_van_een_tussenstand_wordt_ingetrokken() throws Exception {
        Path ir = document(MODEL_B, REGELS_B, List.of(SEIZOENSDAG));
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.TRUTH_MAINTENANCE);
        int tussenstand = 0;
        for (List<Rule> volgorde : TmsDocumenten.steekproef(b.tmsTranspiler().model().getRules(), 20261009L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object v1 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("wachttijd", "5"), List.of("gemeld"));
            v1.getClass().getMethod(dev.uchar.regelspraak.transpiler.kern.Namen.setter("vinddatum"), LocalDate.class)
                    .invoke(v1, LocalDate.of(2027, 6, 1));
            TmsLetWitnessTest.Feiten feiten = new TmsLetWitnessTest.Feiten();
            assertNull(TmsDocumenten.fixpunt(b, volgorde, List.of(v1), sessie -> sessie.addEventListener(feiten)),
                    () -> "volgorde " + namen);
            assertEquals(feiten.ingevoegd, feiten.ingetrokken, () -> "foutfeiten in volgorde " + namen);
            assertNull(TmsDocumenten.lees(v1, "termijn"), () -> "termijn in volgorde " + namen);
            if (feiten.fouten > 0) tussenstand++;
        }
        assertTrue(tussenstand > 0, "geen volgorde liet de termijn vóór de erkenning rekenen");
    }

    // ---------------- C: definitions that do not compile ----------------

    static final String SCHOUWDAG = definitie("schouwdag", TmsDocumenten.samengesteld("all", 0,
            List.of(dagVergelijking("later", "2026-02-28"), dagVergelijking("earlier", "2026-10-01"))));
    static final String HOOGSCHOUWDAG = definitie("hoogschouwdag", "{\"node\":\"Predicate\",\"kind\":\"dayKind\","
            + "\"operand\":" + literaal("2026-06-21", DATUM) + ",\"dayKind\":\"schouwdag\",\"negated\":false,\"digits\":0,"
            + "\"source\":{\"snippet\":\"21-06-2026 een schouwdag is\"}}");
    static final String HAVENDAG = definitie("havendag", TmsDocumenten.samengesteld("all", 0, List.of(
            "{\"node\":\"Comparison\",\"op\":\"later\",\"left\":" + DE_DAG + ",\"right\":"
                    + TmsDocumenten.parameterlezing("openingsdatum") + ",\"source\":{\"snippet\":\"de dag later is dan"
                    + " de openingsdatum\"}}", dagnummer("1"))));

    static final String MODEL_C = model(List.of(TmsDocumenten.objecttype("Schouwer", List.of(
            attribuut("vinddatum", DATUM), attribuut("wachttijd", dagsoortgetal("schouwdagen")),
            attribuut("hoogwachttijd", dagsoortgetal("hoogschouwdagen")),
            attribuut("doorlooptijd", dagsoortgetal("havendagen")), attribuut("termijn", DATUM),
            attribuut("hoogtermijn", DATUM), attribuut("streefdatum", DATUM)), List.of())),
            List.of(TmsDocumenten.parameter("openingsdatum", DATUM)),
            List.of(dagsoort("schouwdag", "schouwdagen"), dagsoort("hoogschouwdag", "hoogschouwdagen"),
                    dagsoort("havendag", "havendagen")));

    static String schouwerstap(String naam, String doel, String duur) {
        return rekenregel(naam, "BEREKENING", "Schouwer", doel, datumstap("plus", lezing("vinddatum", "Schouwer"),
                lezing(duur, "Schouwer"), DATUM, "zijn vinddatum plus zijn " + duur), "null");
    }

    static final String SCHOUWTERMIJN = schouwerstap("termijn", "termijn", "wachttijd");
    static final String HOOGTERMIJN = schouwerstap("hoogtermijn", "hoogtermijn", "hoogwachttijd");
    static final String STREEFDATUM = schouwerstap("streefdatum", "streefdatum", "doorlooptijd");

    static JsonNode zaakC() throws Exception {
        return zaak(List.of(
                instantieMet("S1", "Schouwer", Map.of("vinddatum", datumwaarde("2026-03-10"), "wachttijd",
                        getalwaarde("2"), "hoogwachttijd", getalwaarde("1"), "doorlooptijd", getalwaarde("2")), List.of()),
                instantieMet("S2", "Schouwer", Map.of("vinddatum", datumwaarde("2026-02-27"), "wachttijd",
                        getalwaarde("2")), List.of())),
                Map.of("openingsdatum", datumwaarde("2026-01-01")),
                List.of(waardeJson("S1", "termijn", datumwaarde("2026-03-12")),
                        waardeJson("S2", "termijn", datumwaarde("2026-03-02")), consistent()));
    }

    @Test void een_ongebruikte_definitie_die_niet_te_compileren_is_laat_het_document_spelen() throws Exception {
        List<String> definities = List.of(SCHOUWDAG, HOOGSCHOUWDAG, HAVENDAG);
        assertEquals(overal(List.of(SCHOUWTERMIJN), definities, "gespeeld"),
                inElkeVolgorde(MODEL_C, List.of(SCHOUWTERMIJN), definities, zaakC()));
    }

    @Test void een_vorm_die_een_niet_te_compileren_definitie_gebruikt_wordt_geweigerd_met_naam() throws Exception {
        List<String> definities = List.of(SCHOUWDAG, HOOGSCHOUWDAG, HAVENDAG);
        assertEquals("tms-transpiler SKIP: regel: hoogtermijn — een duur in dagsoorten ('zijn vinddatum plus zijn"
                        + " hoogwachttijd'): de definitie van de dagsoort 'hoogschouwdag' toetst de dagsoort 'schouwdag' op"
                        + " een andere dag dan de gebonden dag ('21-06-2026 een schouwdag is'), een vorm die geen executor"
                        + " uitvoert (RUNTIME-SEMANTICS ch. 3.8)",
                weigering(document(MODEL_C, List.of(SCHOUWTERMIJN, HOOGTERMIJN), definities)));
        assertEquals("tms-transpiler SKIP: regel: streefdatum — een duur in dagsoorten ('zijn vinddatum plus zijn"
                        + " doorlooptijd'): de definitie van de dagsoort 'havendag' leest de parameter 'openingsdatum',"
                        + " die de truth-maintenance-tak in een dagsoortdefinitie niet leest (WF-031 laat haar toe)",
                weigering(document(MODEL_C, List.of(SCHOUWTERMIJN, STREEFDATUM), definities)));
    }

    // ---------------- D: a duration in a day kind between two date-times ----------------

    static final String MODEL_D = model(List.of(TmsDocumenten.objecttype("Getijpost", List.of(
            attribuut("meldtijdstip", DATUMTIJD), attribuut("sluittijdstip", DATUMTIJD), attribuut("getijspanne", SPRINGTIJ)),
            List.of())), List.of(), List.of(dagsoort("springtijdag", "springtijdagen")));
    static final String GETIJSPANNE = rekenregel("getijspanne", "BEREKENING", "Getijpost", "getijspanne",
            duur("duration", lezing("meldtijdstip", "Getijpost"), lezing("sluittijdstip", "Getijpost"), SPRINGTIJ,
                    "de tijdsduur van zijn meldtijdstip tot zijn sluittijdstip in hele springtijdagen"), "null");

    @Test void een_duur_in_dagsoorten_tussen_twee_datum_tijden_wordt_geweigerd_met_naam() throws Exception {
        assertEquals("tms-transpiler SKIP: regel: getijspanne — een duration in dagsoorten ('de tijdsduur van zijn"
                        + " meldtijdstip tot zijn sluittijdstip in hele springtijdagen') tussen twee datum-tijden: de"
                        + " referentie-interpreter en de ASP-backend weigeren haar met naam, omdat het tellen van hele"
                        + " dagen van een soort tussen twee tijdstippen een regel voor de deeldag aan elk eind vraagt,"
                        + " die RUNTIME-SEMANTICS ch. 3.8 niet vastlegt (ch. 3.12)",
                weigering(document(MODEL_D, List.of(GETIJSPANNE), List.of(SPRINGTIJDAG))));
    }

    /** The refusal of the document {@code ir} by the truth-maintenance branch, as the build raises it. */
    static String weigering(Path ir) {
        return assertThrows(CaseRunner.Excluded.class,
                () -> DroolsBackend.build(ir, DroolsBackend.Branch.TRUTH_MAINTENANCE)).getMessage();
    }

    // ---------------- the documents, for the other executors ----------------

    /** Writes every document of this class, in every order of its rules and definitions, with its case, under
     *  {@code args[0]}: a map per part and order, holding {@code ir.json} and {@code case.json}, for the verifier, ref
     *  and clingo. The refused documents of C and D are written once, with a case of what the reference interpreter
     *  would compute where it carries the form. */
    public static void main(String[] args) throws Exception {
        Path uit = Path.of(args[0]);
        schrijfAlle(uit.resolve("a-datum"), MODEL_A, REGELS_A, List.of(SPRINGTIJDAG), zaakA());
        schrijfAlle(uit.resolve("a2-tijdstip"), MODEL_A2, REGELS_A2, List.of(SPRINGTIJDAG), zaakA2());
        schrijfAlle(uit.resolve("b-tussenstand"), MODEL_B, REGELS_B, List.of(SEIZOENSDAG), zaakB());
        schrijfAlle(uit.resolve("b-fixpunt"), MODEL_B, REGELS_B, List.of(SEIZOENSDAG), zaakBFixpunt());
        List<String> c = List.of(SCHOUWDAG, HOOGSCHOUWDAG, HAVENDAG);
        schrijfAlle(uit.resolve("c-ongebruikt"), MODEL_C, List.of(SCHOUWTERMIJN), c, zaakC());
        JsonNode hoog = zaak(List.of(instantieMet("S1", "Schouwer", Map.of("vinddatum", datumwaarde("2026-03-10"),
                "hoogwachttijd", getalwaarde("1")), List.of())), Map.of("openingsdatum", datumwaarde("2026-01-01")),
                List.of(consistent()));
        TmsTijdlijnWitnessTest.schrijf(uit.resolve("c-gebruikt"), ir(MODEL_C, List.of(HOOGTERMIJN), c), hoog);
        JsonNode haven = zaak(List.of(instantieMet("S1", "Schouwer", Map.of("vinddatum", datumwaarde("2026-01-10"),
                "doorlooptijd", getalwaarde("2")), List.of())), Map.of("openingsdatum", datumwaarde("2026-01-01")),
                List.of(waardeJson("S1", "streefdatum", datumwaarde("2026-03-01")), consistent()));
        TmsTijdlijnWitnessTest.schrijf(uit.resolve("c-parameter"), ir(MODEL_C, List.of(STREEFDATUM), c), haven);
        JsonNode getij = zaak(List.of(instantieMet("G1", "Getijpost", Map.of("meldtijdstip", tijdstip("2026-03-10T09:00:00"),
                "sluittijdstip", tijdstip("2026-04-02T09:00:00")), List.of())), Map.of(), List.of(consistent()));
        TmsTijdlijnWitnessTest.schrijf(uit.resolve("d-datumtijdduur"), ir(MODEL_D, List.of(GETIJSPANNE),
                List.of(SPRINGTIJDAG)), getij);
    }

    static void schrijfAlle(Path map, String model, List<String> regels, List<String> definities, JsonNode zaak)
            throws Exception {
        int n = 0;
        for (List<String> volgorde : TmsDocumenten.permutaties(regels))
            for (List<String> defs : TmsDocumenten.permutaties(definities))
                TmsTijdlijnWitnessTest.schrijf(map.resolve(String.format("v%03d", n++)), ir(model, volgorde, defs), zaak);
    }
}
