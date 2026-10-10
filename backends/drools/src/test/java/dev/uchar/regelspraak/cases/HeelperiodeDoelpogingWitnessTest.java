package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The §13.4.7 creation guard under a §8.2 whole-period qualifier, on the DOEL attempt. Both branches try a guard on
 * the anker first and, when that declines, on the doel. Under a whole-period qualifier the doel is not a bound
 * object (WF-109's third scope fence, {@code spec/IR-SPEC.md} ch. 4.6 (q2)): the anker is the only object the guard
 * can be about. A doel attempt that lowers such a guard therefore answers a question the document did not ask —
 * and the DRL-text branch did worse: its whole-period lowering is wired to {@code $subject}, which inside the
 * {@code $doel} pattern is the ANKER, so it either asked the anker's bean for a field it lacks and sank the WHOLE
 * package at compile ("Unable to Analyse Expression rsGeheelPeriode($subject.get…") or, where the anker happened to
 * carry a same-named timeline, silently read the anker from the doel's pattern.
 *
 * <p>Both branches now refuse the doel attempt of a qualified guard by name, with one sentence. The truth-maintenance
 * branch, which makes no doel attempt, refuses these guards in sentences of its own, which name the form: a check with a
 * host that is not the subject, and a whole-period comparison over a timeline along a path; the rows of the exec-model
 * branch pin those since card tms-omschakeling (2026-10-10). Every model below is
 * built in memory from {@code corpus/negatief/wf109-doelgastheer-periode/ir.json} (read, never written): anker
 * {@code Kustvak}, one tussenstap {@code Post}, doel {@code Sein}, beside the rule that COUNTS the created relation.
 * S1 carries the tested timeline for the whole of 2024 and S2 for its first half, so a doel reading is 1; the anker
 * V1 is set up per test so the anker reading differs from it. Measurements 2026-09-23 on main 0f386245, recorded at
 * each test.
 */
class HeelperiodeDoelpogingWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path BASIS = Path.of("..", "..", "corpus", "negatief", "wf109-doelgastheer-periode", "ir.json");

    /** The sentence both branches carry — the creation guard's standing refusal plus its reason here. */
    private static final String REDEN = "voorwaarde niet naar anker of doel te lowereren — gehele-periodekwalificatie";

    private static final String JAAR = "[{\"from\":\"2024-01-01\",\"to\":\"2025-01-01\"}]";
    private static final String HALF = "[{\"from\":\"2024-01-01\",\"to\":\"2024-07-01\"}]";

    // ---------------- 1. the verifier-clean route that sank the package ----------------

    @Test void de_genavigeerde_vergelijking_met_verdrongen_rol_weigert_in_beide_takken() throws Exception {
        // Guard "de seinhoogte van het vlagsein gedurende het gehele jaar groter is dan 0": a to-one navigation from
        // the anker over `vlagvoering`, written by role name. Kustvak also declares a MEMBER named `vlagsein`, which
        // displaces the reference field (the §13.3.9 bean limit), so the anker attempt declines; the doel attempt
        // then read the Sein-typed operand as the doel's OWN attribute. Measured RED FIRST, V1.vlagsein = S2:
        //     verifier : PASS
        //     ref      : EVALUATION FAULT: whole-period qualifier on a comparison whose operands are not a
        //                time-dependent attribute of "Kustvak" and a scalar bound (WF-043)
        //     clingo   : TRANSLATION FAULT: a whole-period comparison over a NAVIGATED or SET timeline read
        //                ("seinhoogte" on "Sein") is not translated
        //     exec     : EXCLUDE transpiler SKIP: feitcreatie … (voorwaarde niet naar anker of doel te lowereren: …)
        //     drl-tekst: EXCLUDE drl compile: … Unable to Analyse Expression rsGeheelPeriode($subject.getSeinhoogte(),
        //                "JAAR", 2, …)
        // Rewiring the lowering to the doel would not have repaired this: the guard is about V1's vlagsein, and the
        // doel reading of its own seinhoogte is a different question.
        ObjectNode ir = basis();
        ObjectNode sein = objecttype(ir, "Sein");
        IrPad.timeline(IrPad.addAttribute(sein, "seinhoogte", getal()), "month");
        IrPad.addAttribute(objecttype(ir, "Kustvak"), "vlagsein", getal());
        IrPad.modelLijst(ir, "factTypes").add(JSON.readTree("""
                { "node": "FactType", "name": "vlagvoering", "reciprocal": false, "roles": [
                  { "node": "Role", "name": "vlagvak", "objectType": "Kustvak", "many": false },
                  { "node": "Role", "name": "vlagsein", "objectType": "Sein", "many": false } ] }
                """));
        guard(ir, JSON.readTree("""
                { "node": "Comparison", "op": "greater",
                  "left": { "node": "AttributeReference", "attribute": "seinhoogte", "subjectType": "Sein",
                             "source": { "snippet": "de seinhoogte van het vlagsein" },
                             "path": [ { "node": "RoleBinding", "factType": "vlagvoering", "role": "vlagsein" } ] },
                  "right": { "node": "Literal", "value": { "number": "0" }, "type": %s, "source": { "snippet": "0" } },
                  "wholePeriod": { "node": "WholePeriod", "granularity": "year", "negated": false },
                  "source": { "snippet": "de seinhoogte van het vlagsein gedurende het gehele jaar groter is dan 0" } }
                """.formatted(getal())));
        String s1 = "\"attributes\": { \"seinhoogte\": { \"timeline\": " + metWaarde(JAAR) + " } }";
        String s2 = "\"attributes\": { \"seinhoogte\": { \"timeline\": " + metWaarde(HALF) + " } }";
        // The two branches name different causes here, both true: the DRL-text report cites the FIRST marker the
        // rule collected, which is the anker attempt's bean limit; the exec branch cites the declined doel attempt.
        Path p = schrijf(ir);
        JsonNode c = geval("\"characteristics\": [\"gehesen\"], \"attributes\": {}", ", \"vlagsein\": [\"S2\"]", s1, s2, "0");
        geweigerdTms(p, c, "feitcreatie: vakbeseining door het kustvak — een Comparison met wholePeriod ('de seinhoogte"
                + " van het vlagsein gedurende het gehele jaar groter is dan 0') over een tijdlijn langs een pad ('de"
                + " seinhoogte van het vlagsein'): de referentie-interpreter vouwt alleen een eigen tijdsafhankelijk"
                + " attribuut van het onderwerp (RUNTIME-SEMANTICS ch. 3.9, 3.12)");
        geweigerd(p, c, true, "de rol 'vlagsein' op Kustvak deelt haar naam met een gedeclareerd lid");
    }

    private static void geweigerdTms(Path ir, JsonNode c, String fragment) {
        CaseRunner.Excluded e = org.junit.jupiter.api.Assertions.assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(ir, c, DroolsBackend.Branch.TRUTH_MAINTENANCE), "tms weigert");
        org.junit.jupiter.api.Assertions.assertTrue(e.getMessage().contains(fragment),
                "tms names '" + fragment + "': " + e.getMessage());
    }

    private static void geweigerd(Path ir, JsonNode c, boolean viaDrl, String fragment) {
        String tak = viaDrl ? "drl-tekst" : "exec-model";
        CaseRunner.Excluded e = org.junit.jupiter.api.Assertions.assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(ir, c, viaDrl), tak + " weigert");
        org.junit.jupiter.api.Assertions.assertTrue(e.getMessage().contains(fragment),
                tak + " names '" + fragment + "': " + e.getMessage());
        org.junit.jupiter.api.Assertions.assertFalse(e.getMessage().contains("drl compile"),
                tak + " refuses per construct, not by a package-wide compile error: " + e.getMessage());
    }

    // ---------------- 2. the route that answered from the wrong object ----------------

    @Test void de_naamnorm_route_speelt_in_de_drl_teksttak_en_weigert_de_gastheer() throws Exception {
        // NOT verifier-clean: the document keeps the host 'Sein' of its basis, and the verifier rejects it (WF-109, the
        // check is hosted on 'Sein', which no object bound there can be; measured 2026-10-10), where the measurement
        // below gave PASS. The truth-maintenance branch refuses that host by name; the DRL-text branch answers 0.
        // Guard "gedurende het gehele jaar Gehesen": under the verifier's key the check is the ANKER's
        // `gehesen` (names compare case-insensitively, as in every reader). The anker attempt of both branches looks
        // the time-dependent characteristic up by exact spelling and declines; the doel declares `Gehesen` itself,
        // so the doel attempt lowered. V1 carries its timeline for the first half year only, so the anker reading is
        // 0 and the doel reading 1. Measured RED FIRST — this is corpus golden jutterswet-seinjaar, case 2:
        //     verifier : PASS
        //     ref      : oracle: PASS — 0, the anker reading
        //     clingo   : clingo: PASS — 0
        //     exec     : FAIL vakseinaantal: verwacht 0, werkelijk 1 — the DOEL reading, silently
        //     drl-tekst: PLAYED — 0, by accident: `$subject.getGehesen()` inside the $doel pattern read the anker
        // Card drl-periode-zink then refused the doel attempt in both branches (this test asserted that refusal).
        // Card drools-temporele-naamnorm (2026-09-23) routes the temporal lookups through Namen.norm, so the ANKER
        // attempt finds `gehesen` and lowers, and the doel attempt is never reached: both branches answer 0, ref's
        // answer. The refusal itself stays pinned by tests 1 and 3.
        ObjectNode ir = basis();
        IrPad.timeline(IrPad.characteristic(objecttype(ir, "Kustvak"), "gehesen"), "year");
        ObjectNode sein = objecttype(ir, "Sein");
        IrPad.timeline(sein.putArray("characteristics").addObject()
                .put("node", "Characteristic").put("name", "Gehesen").put("kind", "adjectival"), "year");
        ObjectNode k = (ObjectNode) IrPad.versie(ir, "factCreations", 0, 0).get("condition");
        k.put("characteristic", "Gehesen");
        Path p = schrijf(ir);
        JsonNode c = geval("\"characteristics\": [], \"characteristicTimelines\": { \"gehesen\": " + HALF + " }, \"attributes\": {}",
                "", "\"characteristicTimelines\": { \"Gehesen\": " + JAAR + " }",
                "\"characteristicTimelines\": { \"Gehesen\": " + HALF + " }", "0");
        org.junit.jupiter.api.Assertions.assertNull(CaseRunner.speelCase(p, c, true),
                "drl-tekst — een gekwalificeerde guard wordt op de anker gelezen, niet op het doel");
        geweigerdTms(p, c, "feitcreatie: vakbeseining door het kustvak — een CharacteristicCheck met een host die het"
                + " onderwerp niet is ('het sein gedurende het gehele jaar gehesen is')");
    }

    // ---------------- 3. the fenced witness no longer sinks the package ----------------

    @Test void de_wf109_periodegetuige_weigert_per_construct() throws Exception {
        // The negatief witness itself (WF-109 FAIL, the third fence). Measured RED FIRST:
        //     ref / clingo : FAULT (the host names an object this position does not bind)
        //     exec         : PLAYED — 1, the doel reading of a verifier-rejected document
        //     drl-tekst    : EXCLUDE drl compile: … rsGeheelPeriode($subject.getGehesen(), "JAAR", 7, null)
        BeideTakken.geweigerdApart(schrijf(basis()),
                geval("\"characteristics\": [\"gehesen\"], \"attributes\": {}", "",
                        "\"characteristicTimelines\": { \"gehesen\": " + JAAR + " }",
                        "\"characteristicTimelines\": { \"gehesen\": " + HALF + " }", "1"),
                "feitcreatie: vakbeseining door het kustvak — een CharacteristicCheck met een host die het onderwerp niet"
                        + " is ('het sein gedurende het gehele jaar gehesen is')",
                REDEN, "de doelgastheer onder een kwalificatie is een benoemde weigering, geen compilerfout");
    }

    // ---------------- 4. the flank: an anker-lowerable qualified guard still plays ----------------

    @Test void de_anker_lowerbare_heelperiode_guard_speelt_nog_in_beide_takken() throws Exception {
        // The card's counter-measurement: `gehesen` also time-dependent on the anker, host dropped. The anker attempt
        // lowers and the doel attempt never runs. Measured 2026-09-23, before and after: verifier PASS, ref and
        // clingo PASS, both branches PLAYED — 2 with V1 hoisted all year, 0 with V1 hoisted half a year (the doel
        // reading would be 1 in both).
        ObjectNode ir = basis();
        IrPad.timeline(IrPad.characteristic(objecttype(ir, "Kustvak"), "gehesen"), "year");
        ((ObjectNode) IrPad.versie(ir, "factCreations", 0, 0).get("condition")).remove("host");
        Path p = schrijf(ir);
        String s1 = "\"characteristicTimelines\": { \"gehesen\": " + JAAR + " }";
        String s2 = "\"characteristicTimelines\": { \"gehesen\": " + HALF + " }";
        BeideTakken.groen(p, geval("\"characteristics\": [], \"characteristicTimelines\": { \"gehesen\": " + JAAR
                + " }, \"attributes\": {}", "", s1, s2, "2"), "heel jaar gehesen anker: beide doelen");
        BeideTakken.groen(p, geval("\"characteristics\": [], \"characteristicTimelines\": { \"gehesen\": " + HALF
                + " }, \"attributes\": {}", "", s1, s2, "0"), "half jaar gehesen anker: geen doelen");
    }

    // ---------------- fixtures ----------------

    private static ObjectNode basis() throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(BASIS));
    }

    private static ObjectNode objecttype(ObjectNode ir, String naam) {
        return IrPad.objectType(ir, naam);
    }

    private static void guard(ObjectNode ir, JsonNode g) {
        IrPad.versie(ir, "factCreations", 0, 0).set("condition", g);
    }

    private static ObjectNode getal() {
        return JSON.createObjectNode().put("node", "Number").put("whole", true).put("decimals", 0)
                .put("sign", "unspecified");
    }

    private static String metWaarde(String segmenten) throws Exception {
        com.fasterxml.jackson.databind.node.ArrayNode a = (com.fasterxml.jackson.databind.node.ArrayNode) JSON.readTree(segmenten);
        a.forEach(s -> ((ObjectNode) s).putObject("value").put("number", "3"));
        return a.toString();
    }

    private static Path schrijf(ObjectNode ir) throws Exception {
        Path tmp = Files.createTempFile("heelperiode-doelpoging", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }

    /** V1 -> P1 -> {S1, S2}. {@code vak} is V1's characteristics/attributes block, {@code vakRollen} extra V1 roles,
     *  {@code s1}/{@code s2} the seinen's extra fields; the expectation is V1's {@code vakseinaantal}. */
    private static JsonNode geval(String vak, String vakRollen, String s1, String s2, String verwacht) throws Exception {
        return JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": "2024-06-15",
                  "input": { "instances": [
                      { "label": "V1", "objectType": "Kustvak", %s,
                        "roles": { "posten": ["P1"]%s } },
                      { "label": "P1", "objectType": "Post", "attributes": {},
                        "roles": { "kustvak": ["V1"], "seinen": ["S1", "S2"] }, "characteristics": ["gehesen"] },
                      { "label": "S1", "objectType": "Sein", "roles": {}, "characteristics": [], %s },
                      { "label": "S2", "objectType": "Sein", "roles": {}, "characteristics": [], %s } ],
                    "parameters": {} },
                  "expected": [
                      { "kind": "attributeValue", "label": "V1", "attribute": "vakseinaantal",
                        "value": { "number": "%s" } },
                      { "kind": "consistent" } ] }
                """.formatted(vak, vakRollen, s1.contains("\"attributes\"") ? s1 : "\"attributes\": {}, " + s1,
                s2.contains("\"attributes\"") ? s2 : "\"attributes\": {}, " + s2, verwacht));
    }
}
