package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * The §13.4.7 creation guard that lowers to NEITHER side — the one construct where this backend's two branches gave
 * two answers, and the exec one was SILENT. A guard is attached to the anker's pattern when it lowers there and to
 * the doel's pattern otherwise; a guard that lowers to neither must not be dropped, because dropping it fires the
 * rule unconditionally and OVER-CREATES the relation. Both branches understood that and stopped creating. Only the
 * DRL-text branch SAID so: it emits an {@code eval(false)} carrying a marker, so {@code Dekking} reports MARKER and
 * the replay refuses the document by name. The exec branch returned no rule and recorded no skip, which the coverage
 * manifest reads as LOWERED — the document replayed as PLAYED with the creation's count at 0.
 *
 * <p>That is the gate-8 principle inside one backend: the two measured shapes below are an {@code Unresolved} guard
 * and a hosting the reader cannot honour, and both are FAULTS in {@code ref} and in the clingo backend. A fault that
 * becomes a plain zero in the third executor is the divergence the differential exists to catch, and the played
 * channel counted it as agreement.
 *
 * <p>Every model below is built in memory from {@code jutterswet-postwacht}'s {@code ir.json} off the wire, reduced
 * to its creation plus the rule that COUNTS what the creation makes; the corpus files are read and never written.
 * Each was pushed through the verifier, {@code ref/} and the clingo backend, and the measurements are recorded at
 * the test that pins them (2026-09-19). This card changed the exec branch's answer only — the guard semantics was
 * untouched: the two flanks at the bottom pin that a guard which DOES lower to a side still plays, with the same
 * values, in both branches. The hosting question they left open was decided the same day
 * ({@code backlog/done/20260910-eigenaar-gastheer-object.md}, WF-109), which moved the verifier and the two OCaml
 * executors and not this backend; the per-test measurement blocks below carry the lines that moved.
 */
class StilleGuardflankWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** A §13.4.7 feitcreatie with one tussenstap: Kustvak -> Post -> Sein, writing Sein.seinvak, beside the
     *  §5.8 AANTAL over the created relation — so what the creation does is a NUMBER on the anker. */
    private static final String POSTWACHT = "jutterswet-postwacht";

    /** The reason both branches must carry. The DRL-text branch has printed it since the creation's guard arm was
     *  written; this is the sentence the exec branch adopts rather than a second wording for one limit. */
    private static final String REDEN = "voorwaarde niet naar anker of doel te lowereren";

    // ---------------- 1. the Onopgelost guard: a fault that became a zero ----------------

    @Test void de_onopgeloste_feitcreatie_guard_weigert_in_beide_takken() throws Exception {
        // The RegelSpraak frontend emits this shape itself: on a creation whose doel type EQUALS its anker type the
        // doel-comparison guard has no second subject to rebuild against, so the resolver hands the wire a literal
        // `Onopgelost` condition. Measured RED FIRST on the pre-card jar, with P1 risicopost and both seinen linked:
        //     verifier : PASS
        //     ref      : EVALUATION FAULT: Onopgelost condition reached in evaluation (WF-044)
        //     clingo   : TRANSLATION FAULT: condition kind not translated (no corpus case exercises it)
        //     exec     : PLAYED 1 — vakseinaantal 0, and a probe pinning 2 FAILS observably, so the 0 is a value
        //                and not a token: an Onopgelost DISAPPEARED into an ordinary answer
        //     drl-tekst: EXCLUDE drl-transpiler MARKER: feitcreatie: vakbeseining door het kustvak
        //                — TODO voorwaarde niet naar anker of doel te lowereren — regel inert i.p.v. over-creatie
        // The guard is unreadable in every executor that reads it; this backend is the only one that answered.
        Path ir = metGuard(onopgelost());
        BeideTakken.geweigerd(ir, geval("[\"risicopost\"]", "[]", "0"), REDEN,
                "een Onopgelost-guard is geen nul-creatie maar een onleesbaar document");
    }

    // ---------------- 2. the third-type owner: a hosting no pattern carries ----------------

    @Test void de_derde_type_eigenaar_weigert_in_beide_takken() throws Exception {
        // A characteristic check whose `eigenaar` is a declared type that is NEITHER the anker NOR the doel — here
        // the tussenstap's own type, which declares the characteristic, so WF-100's declaration half is satisfied.
        // The creation has exactly two patterns to host a guard on; this names a third object. Measured RED FIRST on
        // the pre-card jar (the verifier line has MOVED since: WF-109, 2026-09-19, refuses this host by name, so the
        // document is no longer verifier-clean and this row is the backend's own fence under that verdict):
        //     verifier : PASS  (2026-09-19: WF-109 FAIL — the host is neither bound object)
        //     ref      : EVALUATION FAULT: characteristic check "risicopost" is hosted on "Post" while the subject
        //                is "Kustvak": the owner names the object the check is about (WF-100), and this reader has
        //                no other object to read
        //     clingo   : TRANSLATION FAULT (the same sentence)
        //     exec     : PLAYED 1 — vakseinaantal 0, probe at 2 FAILS: the same silent zero
        //     drl-tekst: EXCLUDE drl-transpiler MARKER (the same reason line as (1))
        // NB the sister shape in an ORDINARY rule was already loud in both branches — a rule whose voorwaarde does
        // not lower is a recorded skip there. Only the creation, with its second pattern and its inert fallback,
        // swallowed it.
        Path ir = metGuard(kenmerkGuard("risicopost", "Post", "de post risicopost is"));
        BeideTakken.geweigerd(ir, geval("[\"risicopost\"]", "[]", "0"), REDEN,
                "een eigenaar die anker noch doel is, is een weigering en geen nul-creatie");
    }

    // ---------------- 3+4. the flanks: a guard that DOES lower still plays ----------------

    @Test void de_doel_gehoste_guard_speelt_nog_in_beide_takken() throws Exception {
        // The over-refusal flank on the side the refusal is nearest to: `eigenaar` names the DOEL, so the anker
        // attempt declines and the doel attempt carries it. It must keep playing, and keep playing the DOEL: S1
        // carries the characteristic and S2 does not, so the count is 1 and not 2 (the anker reading) or 0 (a dropped
        // guard). Measured 2026-09-19, unchanged by this card — and the two OCaml lines have MOVED since, at the card
        // that made this reading normative (WF-109; goldens jutterswet-seinhijsing and jutterswet-mastsein):
        //     verifier : PASS
        //     ref      : EVALUATION FAULT (WF-100)  -> since 2026-09-19: reads the doel, the same 1
        //     clingo   : TRANSLATION FAULT (idem)   -> since 2026-09-19: reads the doel, the same 1
        //     exec     : PLAYED 1 — vakseinaantal 1; probes at 0 and at 2 both FAIL observably
        //     drl-tekst: PLAYED 1 — the same three measurements
        Path ir = metKenmerkOpType("Sein", "gehesen", kenmerkGuard("gehesen", "Sein", "het sein gehesen is"));
        BeideTakken.groen(ir, geval("[]", "[\"gehesen\"]", "1"),
                "de doel-gehoste guard filtert de doelen nog, in beide takken");
    }

    @Test void de_anker_gehoste_guard_speelt_nog_in_beide_takken() throws Exception {
        // The flank on the other side, and the one the whole differential square reaches: `eigenaar` names the ANKER,
        // which IS the subject of the anker-first attempt, so the guard lowers there and no owner test fires at all.
        // This is the only one of the four that every executor reads. Measured 2026-09-19, unchanged by this card:
        //     verifier : PASS
        //     ref      : oracle: PASS (2 expectations)
        //     clingo   : clingo: PASS (2 expectations)
        //     exec     : PLAYED 1 — vakseinaantal 2; probes at 0 and at 1 both FAIL observably
        //     drl-tekst: PLAYED 1 — the same three measurements
        Path ir = metKenmerkOpType("Kustvak", "gevaarlijk",
                kenmerkGuard("gevaarlijk", "Kustvak", "het kustvak gevaarlijk is"));
        BeideTakken.groen(ir, gevalMetVak("[\"gevaarlijk\"]", "2"),
                "de anker-gehoste guard blijft de ankers filteren, in beide takken");
    }

    // ---------------- fixtures ----------------

    /** postwacht's model reduced to the creation plus the rule that counts what it creates, with {@code guard} as the
     *  creation's voorwaarde. Everything else — the two other objecttypen, all four feittypen, the uniciteit —
     *  stays as the golden has it, so the document differs from a played corpus golden in exactly one field. */
    private Path metGuard(ObjectNode guard) throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(CORPUS.resolve(POSTWACHT).resolve("ir.json")));
        ArrayNode houd = JSON.createArrayNode();
        for (JsonNode r : IrPad.rulesLijst(ir, "rules"))
            if (Set.of("vakseinaantal van een kustvak").contains(r.get("name").asText())) houd.add(r);
        IrPad.rules(ir).set("rules", houd);
        IrPad.versie(ir, "factCreations", 0, 0).set("condition", guard);
        Path tmp = Files.createTempFile("stille-guardflank", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }

    /** The same model, with {@code kenmerk} first DECLARED on {@code objecttype} — WF-100's declaration half, which
     *  the two flank guards need and the third-type guard already has from the golden. */
    private Path metKenmerkOpType(String objecttype, String kenmerk, ObjectNode guard) throws Exception {
        Path p = metGuard(guard);
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(p));
        ObjectNode ot = IrPad.objectType(ir, objecttype);
        ot.putArray("characteristics").addObject()
                .put("node", "Characteristic").put("name", kenmerk).put("kind", "adjectival");
        Files.writeString(p, ir.toString());
        return p;
    }

    private static ObjectNode onopgelost() {
        return JSON.createObjectNode().put("node", "Unresolved").set("source", IrPad.bronrecord("het sein het sein is"));
    }

    private static ObjectNode kenmerkGuard(String kenmerk, String eigenaar, String bron) {
        ObjectNode k = JSON.createObjectNode().put("node", "CharacteristicCheck").put("characteristic", kenmerk);
        k.put("negated", false).put("host", eigenaar);
        k.set("source", IrPad.bronrecord(bron));
        return k;
    }

    /** One Kustvak with one Post carrying two Seinen — the creation's whole reach — expecting {@code vakseinaantal}. */
    private static JsonNode geval(String postKenmerken, String s1Kenmerken, String verwacht) throws Exception {
        return gevalRuw("[]", postKenmerken, s1Kenmerken, verwacht);
    }

    /** The same population with the characteristic on the KUSTVAK instead. */
    private static JsonNode gevalMetVak(String vakKenmerken, String verwacht) throws Exception {
        return gevalRuw(vakKenmerken, "[]", "[]", verwacht);
    }

    private static JsonNode gevalRuw(String vakKenmerken, String postKenmerken, String s1Kenmerken, String verwacht)
            throws Exception {
        return JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "V1", "objectType": "Kustvak", "attributes": { "gevarenklasse": { "number": "7" } },
                        "roles": { "posten": ["P1"] }, "characteristics": %s },
                      { "label": "P1", "objectType": "Post", "attributes": { "hoogte": { "number": "15" } },
                        "roles": { "kustvak": ["V1"], "seinen": ["S1", "S2"] }, "characteristics": %s },
                      { "label": "S1", "objectType": "Sein", "attributes": { "gewicht": { "number": "4" } },
                        "roles": {}, "characteristics": %s },
                      { "label": "S2", "objectType": "Sein", "attributes": { "gewicht": { "number": "6" } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [
                      { "kind": "attributeValue", "label": "V1", "attribute": "vakseinaantal",
                        "value": { "number": "%s" } },
                      { "kind": "consistent" } ] }
                """.formatted(vakKenmerken, postKenmerken, s1Kenmerken, verwacht));
    }
}
