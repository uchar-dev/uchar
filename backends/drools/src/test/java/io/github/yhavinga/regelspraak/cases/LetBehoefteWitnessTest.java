package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RUNTIME-SEMANTICS ch. 10.6 as UNIFIED to <b>call-by-need</b> (maintainer decision O3, 2026-09-01) — the
 * three clauses that separate L from the strict binding this backend carried before, witnessed through
 * {@link CaseRunner#speelCase} over models authored HERE (no corpus file is read, moved or touched).
 *
 * <p>The two sibling files pin what the cluster computes ({@code LetClusterWitnessTest}) and what a ch. 4.10
 * node hides from the hand-written walks ({@code LetDragerWandelingWitnessTest}); both keep their pins under L,
 * because every binder they bind is read on every path. This file is the other half: the models on which
 * strict and call-by-need DISAGREE, so a backend that quietly kept the strict binding fails here and nowhere
 * else. <b>Every pinned value was produced by {@code ref/} on this exact model+case first</b> — ref carries the
 * decision, this backend is the third executor — and each was re-checked by mutation (the laziness reverted to
 * a strict bind in a scratch copy makes exactly these witnesses red).
 *
 * <ul>
 *   <li><b>clause 3, an unread binder evaluates nothing.</b> A value that would FAULT (division by numeric
 *       zero) under a binder the body never reads: the Let's outcome is the body's, no fault — where the
 *       strict binding evaluated the value first and died. Its twin reads the binder, and there the fault
 *       stands: a "never evaluate anything" implementation passes the first and fails the second;</li>
 *   <li><b>clause 2, the barrier sits at the READ.</b> The probe-B1 shape: a leeg value whose binder is read
 *       only in the branch ch. 10.2 does NOT take, so nothing reads it and the target is written with the
 *       body's outcome — where the strict binding emptied the whole Let at the bind place and wrote nothing;</li>
 *   <li><b>clause 1, one need, however many reads.</b> The same binder read twice against the same binder read
 *       once: same value, same §9.5 verdict.</li>
 * </ul>
 */
class LetBehoefteWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ---------------- clause 3: an unread binder evaluates nothing ----------------

    @Test void een_ongelezen_binder_met_faultende_waarde_laat_de_body_beslissen() throws Exception {
        // "uitkomst := Let q = 100,00 gedeeld door 0,00 in 42,00" — the binder is NEVER read, so ch. 10.6
        // clause 3 never evaluates the value: no fault, no leeg, no §9.5 reason, and the Let's outcome IS the
        // body's. ref on this exact model+case: uitkomst = 42.00, CONSISTENT. Under the strict binding the
        // value ran first and the run died on the division.
        assertNull(speel(nulDelingModel(false), jutter("", waarde("uitkomst", "42.00"))));
    }

    @Test void dezelfde_waarde_onder_een_gelezen_binder_houdt_de_fout() throws Exception {
        // the twin that keeps the first honest: the SAME value, now read. The read forces the need, so the
        // division by numeric zero happens and fails fast. ref: EVALUATION FAULT "division by numeric zero";
        // here the mirror is the runtime's own ArithmeticException, surfacing out of fire.
        RuntimeException fout = assertThrows(RuntimeException.class,
                () -> speel(nulDelingModel(true), jutter("", waarde("uitkomst", "42.00"))));
        assertTrue(oorzaakTekst(fout).contains("deling door 0"),
                "de gelezen behoefte draagt de deling-door-nul: " + oorzaakTekst(fout));
    }

    // ---------------- clause 2: the leeg-barrier fires at the READ, not at the bind ----------------

    @Test void een_lege_waarde_gelezen_in_de_niet_gekozen_tak_schrijft_het_doel_toch() throws Exception {
        // the probe-B1 shape, the one that closed the false-green window on the clingo side:
        //   "uitkomst := Let k = kindertal in Let x = leegwaarde in (als k groter dan 1 dan 7,00 anders x maal 2,00)"
        // `leegwaarde` is ABSENT. The condition reads `k` (so k's need IS forced, on every path — WF-077), and
        // `x` is read ONLY in the anders-branch, which kindertal = 2 does not take. Nothing reads x, so nothing
        // evaluates its leeg value and no barrier fires. ref on this exact model+case: uitkomst = 7.00,
        // CONSISTENT. Under the strict binding the leeg value emptied the inner Let at its BIND place and the
        // target was never written.
        assertNull(speel(probeModel(), jutter("\"kindertal\": { \"number\": \"2\" }", waarde("uitkomst", "7.00"))));
    }

    @Test void diezelfde_lege_waarde_gelezen_op_de_wel_gekozen_tak_maakt_de_let_leeg() throws Exception {
        // the other flank of the same model: kindertal = 0 takes the anders-branch, which DOES read `x`, so the
        // barrier fires at that read and empties the Let that bound x — and with it the whole value, so the
        // standing scalar target is not written. ref: uitkomst leeg, CONSISTENT. Without this flank the witness
        // above would also pass on an implementation that simply ignored the barrier.
        assertNull(speel(probeModel(), jutter("\"kindertal\": { \"number\": \"0\" }",
                "{ \"kind\": \"attributeEmpty\", \"label\": \"J1\", \"attribute\": \"uitkomst\" }")));
    }

    // ---------------- clause 1: one need, however many reads ----------------

    @Test void tweemaal_lezen_geeft_dezelfde_uitkomst_als_eenmaal_lezen() throws Exception {
        // "tweemaal := Let q = grondslag gedeeld door deler in q plus q" beside
        // "eenmaal  := Let q = grondslag gedeeld door deler in q", `deler` ABSENT.
        // §6.5 Tabel 12 makes the need leeg AND deposits a Fout; the read then raises the barrier, so BOTH
        // targets are leeg and both rules are inconsistent — the second read neither recomputes into a
        // different answer nor coalesces to 0 inside `plus`. ref on this exact model+case: tweemaal leeg,
        // eenmaal leeg, INCONSISTENT for both rules.
        //
        // NB the once-ness itself has no observable artefact in THIS backend: its §9.5 companion is a separate
        // STATIC rule keyed on the operands' leegte (see delingCompanions), so it fires once per division site
        // whatever the read count. What is pinnable here is the outcome identity across one read and two —
        // ref's own clause-1 witness (a single channel reason) carries the multiplicity.
        assertNull(speel(behoefteModel(), jutter("\"grondslag\": { \"number\": \"100.00\" }",
                "{ \"kind\": \"attributeEmpty\", \"label\": \"J1\", \"attribute\": \"tweemaal\" }, "
                        + "{ \"kind\": \"attributeEmpty\", \"label\": \"J1\", \"attribute\": \"eenmaal\" }, "
                        + "{ \"kind\": \"inconsistent\", \"rule\": \"tweemaal van een jutter\" }, "
                        + "{ \"kind\": \"inconsistent\", \"rule\": \"eenmaal van een jutter\" }")));
    }

    // ---------------- helpers: replay ----------------

    /** One authored model + case through the FULL replay; the per-case failure line, null on a match. */
    private static String speel(String ir, String caseJson) throws Exception {
        Path tmp = Files.createTempFile("let-behoefte-witness", ".json");
        try {
            Files.writeString(tmp, ir);
            return CaseRunner.speelCase(tmp, JSON.readTree(caseJson));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    /** The whole cause chain's messages — a fault at fire reaches the caller wrapped by Drools. */
    private static String oorzaakTekst(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (Throwable c = t; c != null; c = c.getCause() == c ? null : c.getCause()) sb.append(c).append(" | ");
        return sb.toString();
    }

    /** One Jutter, the given input attributes, the given expectations. The consistency verdict is part of each
     *  expectation list, so it is never left unasserted. */
    private static String jutter(String attributen, String verwacht) {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": { %s },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ %s ] }
                """.formatted(attributen, verwacht);
    }

    /** A value expectation plus the consistent verdict — the shape the three non-§6.5 witnesses use. */
    private static String waarde(String attribuut, String getal) {
        return """
                { "kind": "attributeValue", "label": "J1", "attribute": "%s", "value": { "number": "%s" } },
                { "kind": "consistent" }""".formatted(attribuut, getal);
    }

    // ---------------- helpers: the wire shapes (IR-SPEC ch. 4.10) ----------------

    private static String num(int dec, boolean geheel, String teken) {
        return "{\"node\":\"Numeriek\",\"geheel\":%b,\"decimalen\":%d,\"teken\":\"%s\",\"eenheid\":null}"
                .formatted(geheel, dec, teken);
    }
    private static String num(int dec) { return num(dec, false, "ONBEPAALD"); }
    private static final String GEHEEL_POS = num(0, true, "NIET_NEGATIEF");

    private static String attr(String naam, String type) {
        return ("{\"node\":\"AttribuutRef\",\"attribuut\":\"%s\",\"objecttype\":\"Jutter\","
                + "\"bron\":\"het %s van de Jutter\"}")
                .formatted(naam, naam);
    }

    private static String lit(String waarde, String type) {
        return "{\"node\":\"Literal\",\"waarde\":\"%s\",\"type\":%s,\"bron\":\"%s\"}".formatted(waarde, type, waarde);
    }

    private static String binair(String op, String links, String rechts, String type) {
        return "{\"node\":\"Binair\",\"op\":\"%s\",\"links\":%s,\"rechts\":%s,\"type\":%s,\"bron\":\"%s\"}"
                .formatted(op, links, rechts, type, op.toLowerCase(java.util.Locale.ROOT));
    }

    /** ch. 4.10 the binding — a NEED under ch. 10.6's call-by-need. Its {@code type} is the BODY's (WF-076). */
    private static String let(String binder, String value, String body, String type) {
        return "{\"node\":\"Let\",\"type\":%s,\"binder\":\"%s\",\"value\":%s,\"body\":%s,\"bron\":\"%s is …\"}"
                .formatted(type, binder, value, body, binder);
    }

    /** NB the wire field is {@code name}, not {@code naam}. */
    private static String bref(String naam, String type) {
        return "{\"node\":\"BinderRef\",\"name\":\"%s\",\"bron\":\"%s\"}".formatted(naam, naam);
    }

    private static String ite(String conditie, String dan, String anders, String type) {
        return ("{\"node\":\"IfThenElse\",\"type\":%s,\"condition\":%s,\"then\":%s,\"else\":%s,"
                + "\"bron\":\"als … dan … anders …\"}").formatted(type, conditie, dan, anders);
    }

    private static String verg(String op, String links, String rechts) {
        return "{\"node\":\"Vergelijking\",\"op\":\"%s\",\"links\":%s,\"rechts\":%s,\"periode\":null,\"bron\":\"%s\"}"
                .formatted(op, links, rechts, op.toLowerCase(java.util.Locale.ROOT));
    }

    private static String regel(String naam, String doel, String waarde) {
        return ("{\"node\":\"ResolvedRegel\",\"naam\":\"%s\",\"soort\":\"BEREKENING\",\"doel\":%s,\"waarde\":%s,"
                + "\"voorwaarde\":null,\"geldigheid\":{\"node\":\"Geldigheid\",\"vanaf\":null,\"tot\":null}}")
                .formatted(naam, doel, waarde);
    }

    /** A one-objecttype document over {@code Jutter}. */
    private static String model(String attributen, String[] regels) {
        return ("{\"format\":\"uchar-ir-v1\",\"model\":{"
                + "\"objectTypes\":[{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\","
                + "\"attributes\":[%s],\"characteristics\":[]}],"
                + "\"factTypes\":[],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],"
                + "\"timelines\":[],\"dayKinds\":[],\"dagsoortPerEenheid\":{}},"
                + "\"rules\":{\"resolvedRegels\":[%s],"
                + "\"resolvedDagsoorten\":[],\"objectCreaties\":[],\"verdelingen\":[],\"consistenties\":[],"
                + "\"uniciteiten\":[],\"uniciteitUnies\":[],\"feitCreaties\":[],\"resolvedStartpunten\":[],"
                + "\"regelGroepen\":[]},\"diagnostics\":[]}")
                .formatted(attributen, String.join(",", regels));
    }

    /** The {@code attributes} list of {@code Jutter}, from alternating name / datatype arguments. */
    private static String attributen(String... naamEnType) {
        if (naamEnType.length % 2 != 0) throw new IllegalArgumentException("a name without a datatype");
        java.util.StringJoiner lijst = new java.util.StringJoiner(",");
        for (int i = 0; i < naamEnType.length; i += 2)
            lijst.add("{\"node\":\"Attribute\",\"name\":\"%s\",\"datatype\":%s}".formatted(naamEnType[i], naamEnType[i + 1]));
        return lijst.toString();
    }

    // ---------------- the models (each ref-replayed before its values were pinned) ----------------

    /** ch. 10.6 clause 3 / its twin: a value that FAULTS, under a binder the body reads or does not. */
    private static String nulDelingModel(boolean gelezen) {
        String deling = binair("GEDEELD_DOOR", lit("100.00", num(2)), lit("0.00", num(2)), num(2));
        String body = gelezen ? bref("q", num(2)) : lit("42.00", num(2));
        return model(attributen("uitkomst", num(2)),
                new String[]{regel("uitkomst van een jutter", attr("uitkomst", num(2)),
                        let("q", deling, body, num(2)))});
    }

    /** ch. 10.6 clause 2, the probe-B1 shape: a leeg need read ONLY in one branch of a ch. 10.2 choice whose
     *  condition reads a DIFFERENT, filled binder. `kindertal` decides which branch — and so whether the leeg
     *  need is ever read at all. */
    private static String probeModel() {
        String binnen = let("x", attr("leegwaarde", num(2)),
                ite(verg("GROTER", bref("k", GEHEEL_POS), lit("1", GEHEEL_POS)),
                        lit("7.00", num(2)),
                        binair("MAAL", bref("x", num(2)), lit("2.00", num(2)), num(2)),
                        num(2)),
                num(2));
        return model(attributen("kindertal", GEHEEL_POS, "leegwaarde", num(2), "uitkomst", num(2)),
                new String[]{regel("uitkomst van een jutter", attr("uitkomst", num(2)),
                        let("k", attr("kindertal", GEHEEL_POS), binnen, num(2)))});
    }

    /** ch. 10.6 clause 1: one §6.5-depositing need, read twice and read once, on one subject. */
    private static String behoefteModel() {
        String deling = binair("GEDEELD_DOOR", attr("grondslag", num(2)), attr("deler", num(2)), num(2));
        return model(attributen("grondslag", num(2), "deler", num(2), "tweemaal", num(2), "eenmaal", num(2)),
                new String[]{
                        regel("tweemaal van een jutter", attr("tweemaal", num(2)),
                                let("q", deling, binair("PLUS", bref("q", num(2)), bref("q", num(2)), num(2)), num(2))),
                        regel("eenmaal van een jutter", attr("eenmaal", num(2)),
                                let("q", deling, bref("q", num(2)), num(2)))});
    }
}
