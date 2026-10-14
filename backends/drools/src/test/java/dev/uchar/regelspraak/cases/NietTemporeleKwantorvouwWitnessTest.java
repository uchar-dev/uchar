package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The QUANTIFIER FOLD with a NON-temporal matrix (RUNTIME-SEMANTICS ch. 3.9a), replayed through BOTH
 * transpiler branches over the corpus golden that pins it.
 *
 * <p><b>Why this test exists beside gate 12.</b> Gate 12 replays every golden through the EXEC-MODEL
 * branch only, and gate 13 drives the DRL-TEXT branch over the goldens it can TWIST — which are the
 * unit-bearing ones, and this document declares no units. So the DRL-text lowering of this node has no
 * gate that would notice it breaking. This is that gate: one file, both branches, the corpus's own
 * hand-computed expectations on both sides.
 *
 * <p>A pass here is the square closing on this form: the same five cells that {@code ref} and the
 * clingo backend replay (gates 5 and 6) are replayed by the two Drools lowerings, each of which
 * reaches them by a different road — the exec-model branch derives the verdict with truth maintenance
 * (until card tms-omschakeling, 2026-10-10, the destructive exec branch collected the contributors and
 * asked a leaf predicate of each), the DRL branch denies the failure witness with a {@code not} pattern.
 *
 * <p>The corpus files are READ and never written; the mutation row below copies a case into a
 * {@link TempDir} first.
 */
class NietTemporeleKwantorvouwWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path GOLDEN =
            Path.of("..", "..", "corpus", "golden", "jutterswet-schouwploeg");

    /** Its counting sibling: the three counting quantifiers over a matrix with no period qualifier. */
    private static final Path TEL_GOLDEN =
            Path.of("..", "..", "corpus", "golden", "jutterswet-vuurtoren");

    @TempDir Path tmp;

    private static List<Path> casesVan(Path golden) throws IOException {
        try (Stream<Path> s = Files.list(golden)) {
            return s.filter(p -> p.getFileName().toString().startsWith("case")).sorted().toList();
        }
    }

    private static List<Path> cases() throws IOException { return casesVan(GOLDEN); }

    private static List<Path> telCases() throws IOException { return casesVan(TEL_GOLDEN); }

    @Test void de_exec_model_tak_speelt_de_cellen() throws Exception {
        List<Path> cases = cases();
        assertFalse(cases.isEmpty(), "de golden hoort casebestanden te dragen");
        CaseRunner.Result r = CaseRunner.viaExecModel(GOLDEN.resolve("ir.json"), cases);
        assertEquals("PLAYED", r.status(),
                "de exec-model-tak hoort de niet-temporele kwantorvouw te loweren: " + r.reason());
        assertEquals(cases.size(), r.casesPlayed(), "elke cel hoort gespeeld te zijn, niet overgeslagen");
    }

    /**
     * De DRL-tekst-tak lowert dezelfde vouw als een ONTKENDE GETUIGE: {@code not <Element>( <join>,
     * <matrix of haar ontkenning> )}. Deze rij is de enige plek in de hele poortverzameling waar die
     * tak op deze knoop gemeten wordt — gate 12 speelt alleen de exec-tak, en gate 13 raakt dit
     * document niet, want het draagt geen eenheidsdragende annotatie om te verdraaien.
     */
    @Test void de_drl_tekst_tak_speelt_dezelfde_cellen() throws Exception {
        List<Path> cases = cases();
        CaseRunner.Result r = CaseRunner.viaDrlTekst(GOLDEN.resolve("ir.json"), cases);
        assertEquals("PLAYED", r.status(),
                "de DRL-tekst-tak hoort de niet-temporele kwantorvouw te loweren: " + r.reason());
        assertEquals(cases.size(), r.casesPlayed(), "elke cel hoort gespeeld te zijn, niet overgeslagen");
    }

    /**
     * PLAYED alleen is geen getuige. Een regel die stil niet lowert schrijft niets, en elke cel die een
     * kenmerk AFWEZIG verwacht haalt dan vanzelf — zo zou een tak die de hele vouw overslaat nog steeds
     * kunnen "spelen". Daarom deze mutant: de vacuümcel, waar alle vier de vouwen WAAR lezen, met één
     * verwachting omgeklapt naar afwezig. Beide takken moeten hem nu VERWERPEN. Faalt deze rij, dan zegt
     * PLAYED hierboven niets over de kwantor.
     */
    @Test void een_mutant_op_de_vacuumcel_wordt_door_beide_takken_verworpen() throws Exception {
        Path bron = GOLDEN.resolve("case4-lege-ploeg.json");
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(bron));
        boolean geraakt = false;
        for (JsonNode e : doc.withArray("expected"))
            if ("characteristicPresent".equals(e.path("kind").asText())
                    && "ervaren".equals(e.path("characteristic").asText())) {
                ((ObjectNode) e).put("kind", "characteristicAbsent");
                geraakt = true;
            }
        assertTrue(geraakt, "de vacuümcel hoort 'ervaren' AANWEZIG te verwachten — anders muteert deze rij niets");
        Path mutant = tmp.resolve("case4-mutant.json");
        Files.writeString(mutant, doc.toPrettyString());

        Path ir = GOLDEN.resolve("ir.json");
        assertEquals("FAIL", CaseRunner.viaExecModel(ir, List.of(mutant)).status(),
                "de exec-model-tak hoort de omgeklapte vacuümverwachting te verwerpen");
        assertEquals("FAIL", CaseRunner.viaDrlTekst(ir, List.of(mutant)).status(),
                "de DRL-tekst-tak hoort hem even goed te verwerpen");
    }

    // ---------------- the COUNTING quantifiers over the same, non-temporal matrix ----------------

    /**
     * {@code corpus/golden/jutterswet-vuurtoren} is the counting sibling of the golden above: the three
     * counting quantifiers over an ordinary comparison, each at its threshold from both sides. It is the
     * ONLY corpus document that reaches the DRL-text branch's counting lowering at all — the whole-period
     * counting golden ({@code jutterswet-reddingsvloot}) is markered there, exactly as its ALLE/GEEN sibling
     * is — and until card tms-omschakeling (2026-10-10) it was the only one that reached the destructive exec
     * branch's NON-temporal counting route, since gate 12 would otherwise only ever have seen the timeline
     * route. The two lowerings were different roads again: that exec branch counted over a {@code collectList}
     * accumulate of the contributors; the DRL branch writes a
     * {@code Number( intValue <op> N ) from accumulate( … count(…) )} over the element pattern.
     */
    @Test void beide_takken_tellen_dezelfde_cellen() throws Exception {
        List<Path> cases = telCases();
        assertFalse(cases.isEmpty(), "de tel-golden hoort casebestanden te dragen");
        Path ir = TEL_GOLDEN.resolve("ir.json");

        CaseRunner.Result exec = CaseRunner.viaExecModel(ir, cases);
        assertEquals("PLAYED", exec.status(),
                "de exec-model-tak hoort de tellende vouw te loweren: " + exec.reason());
        assertEquals(cases.size(), exec.casesPlayed(), "elke cel hoort gespeeld te zijn");

        CaseRunner.Result drl = CaseRunner.viaDrlTekst(ir, cases);
        assertEquals("PLAYED", drl.status(),
                "de DRL-tekst-tak hoort haar even goed te loweren: " + drl.reason());
        assertEquals(cases.size(), drl.casesPlayed(), "elke cel hoort gespeeld te zijn");
    }

    /**
     * PLAYED alleen is ook hier geen getuige, en bij een TELLING minder dan ooit: de lege-toren-cel leest
     * {@code zuinig} (ten hoogste 1) AANWEZIG en de andere drie AFWEZIG, dus een tak die stil niets lowert
     * zou drie van de vier verwachtingen halen. Deze mutant klapt juist de AANWEZIGE om, zodat alleen een
     * tak die de telling werkelijk uitrekent hem verwerpt.
     */
    @Test void een_mutant_op_de_telling_wordt_door_beide_takken_verworpen() throws Exception {
        Path bron = TEL_GOLDEN.resolve("case3-lege-toren.json");
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(bron));
        boolean geraakt = false;
        for (JsonNode e : doc.withArray("expected"))
            if ("characteristicPresent".equals(e.path("kind").asText())
                    && "zuinig".equals(e.path("characteristic").asText())) {
                ((ObjectNode) e).put("kind", "characteristicAbsent");
                geraakt = true;
            }
        assertTrue(geraakt, "de lege-toren-cel hoort 'zuinig' AANWEZIG te verwachten");
        Path mutant = tmp.resolve("tel-mutant.json");
        Files.writeString(mutant, doc.toPrettyString());

        Path ir = TEL_GOLDEN.resolve("ir.json");
        assertEquals("FAIL", CaseRunner.viaExecModel(ir, List.of(mutant)).status(),
                "de exec-model-tak hoort de omgeklapte telling te verwerpen");
        assertEquals("FAIL", CaseRunner.viaDrlTekst(ir, List.of(mutant)).status(),
                "de DRL-tekst-tak hoort hem even goed te verwerpen");
    }

    /**
     * Een GENESTE vouw als matrix: "alle wachten van de Schouwploeg hebben alle stukken gekeurd". Welgevormd,
     * en de twee OCaml-uitvoerders antwoorden erop — gemeten op dit document:
     * <pre>
     *   verifier : PASS
     *   ref      : oracle: PASS (2 expectations)
     *   clingo   : clingo: PASS (2 expectations)
     * </pre>
     * De analyse met truth maintenance speelt hem zoals {@code ref} antwoordt, met de case van deze rij
     * ({@code TmsZoalsRefWitnessTest}, {@code geneste-vouw-als-matrix}), en sinds kaart
     * drl-teksttak-op-truth-maintenance in beide takken. Tot die kaart droeg de destructieve DRL-teksttak hem niet
     * en weigerde hem BIJ NAAM, met de matrixvorm in de reden, zoals de destructieve exec-modeltak deed; vóór de
     * vangst van 2026-09-16 stierf het pakket daar op een parsefout, en de rij {@code de_drl_markertekst_scheidt_de_redenen}
     * hield de reden per weigering vast. Met die tak vervallen beide beweringen: hier geldt één oordeel.
     */
    @Test void een_geneste_vouw_als_matrix_speelt_in_beide_takken() throws Exception {
        Path ir = genesteVouw();
        Path c = tmp.resolve("genest-case.json");
        Files.writeString(c, GENEST_CASE);
        CaseRunner.Result tms = CaseRunner.viaExecModel(ir, List.of(c));
        assertEquals("PLAYED", tms.status(), "de exec-model-tak antwoordt zoals ref: " + tms);
        CaseRunner.Result drl = CaseRunner.viaDrlTekst(ir, List.of(c));
        assertEquals("PLAYED", drl.status(), "de DRL-teksttak antwoordt zoals ref: " + drl);
        assertEquals(tms.casesPlayed(), drl.casesPlayed());
    }

    /** De heelperiodevouw speelt in beide takken (kaart drl-heelperiode-arm, 2026-09-18): de vroegere
     *  kanaalsplitsing blijft dicht. */
    @Test void de_heelperiodevouw_speelt_in_de_drl_teksttak() throws Exception {
        Path ploegpremie = GOLDEN.resolveSibling("jutterswet-ploegpremie");
        CaseRunner.Result heel = CaseRunner.viaDrlTekst(ploegpremie.resolve("ir.json"),
                List.of(ploegpremie.resolve("case1-allen-boven-de-grens.json")));
        assertEquals("PLAYED", heel.status(), "de heelperiodevouw is geen kanaalsplitsing meer: " + heel.reason());
    }

    // ================= the nested-fold document, spliced into the corpus golden =================

    /** The golden's document with a second relation ({@code Strandwacht} -&gt; {@code Uitrusting}) and ONE rule
     *  whose voorwaarde is a fold whose MATRIX is another fold. Written to the {@link TempDir}; the corpus files
     *  are read and never written. */
    private Path genesteVouw() throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(GOLDEN.resolve("ir.json")));
        ObjectNode ploeg = IrPad.objectType(doc, "Schouwploeg");
        ploeg.set("characteristics", JSON.readTree(
                "[ { \"node\": \"Characteristic\", \"name\": \"uitgerust\", \"kind\": \"adjectival\" } ]"));
        ploeg.set("attributes", JSON.readTree("[]"));
        IrPad.modelLijst(doc, "objectTypes").add(JSON.readTree("""
                { "node": "ObjectType", "name": "Uitrusting", "animate": false, "plural": "Uitrustingen",
                  "attributes": [],
                  "characteristics": [ { "node": "Characteristic", "name": "gekeurd", "kind": "adjectival" } ] }"""));
        IrPad.modelLijst(doc, "factTypes").add(JSON.readTree("""
                { "node": "FactType", "name": "wachtuitrusting", "reciprocal": false, "roles": [
                    { "node": "Role", "name": "drager", "objectType": "Strandwacht",
                      "many": false },
                    { "node": "Role", "name": "stuk", "objectType": "Uitrusting",
                      "plural": "stukken", "many": true } ] }"""));
        IrPad.rules(doc).set("rules", JSON.readTree("""
                [ { "node": "Rule", "name": "uitgerust bij keuring", "versions": [ {
                      "validity": { "node": "Validity" },
                      "target": { "node": "AttributeReference", "attribute": "uitgerust", "subjectType": "Schouwploeg",
                              "source": { "snippet": "Een Schouwploeg is uitgerust" } },
                      "value": { "node": "Literal", "value": { "boolean": true }, "type": { "node": "Boolean" }, "source": { "snippet": "waar" } },
                      "condition": {
                      "node": "QuantifierFold", "quantifier": "all", "elementType": "Strandwacht",

                      "test": { "node": "QuantifierFold", "quantifier": "all", "elementType": "Uitrusting",

                                 "test": { "node": "CharacteristicCheck", "characteristic": "gekeurd", "negated": false,
                                            "source": { "snippet": "het stuk gekeurd is" } },
                                 "source": { "snippet": "alle stukken van de Strandwacht gekeurd zijn" },
                                 "sourceRole": { "node": "RoleBinding", "factType": "wachtuitrusting",
                                              "role": "stuk" } },
                      "source": { "snippet": "alle wachten van de Schouwploeg alle stukken gekeurd hebben" },
                      "sourceRole": { "node": "RoleBinding", "factType": "schouwindeling", "role": "wacht" } },
                      "initialisation": false } ] } ]"""));
        Path p = tmp.resolve("genest-ir-" + (n++) + ".json");
        Files.writeString(p, doc.toPrettyString());
        return p;
    }

    private int n;

    private static final String GENEST_CASE = """
            { "format": "uchar-case-v0", "asOfDate": null,
              "input": { "instances": [
                  { "label": "S1", "objectType": "Schouwploeg", "attributes": {},
                    "roles": { "wachten": [ "W1", "W2" ] }, "characteristics": [] },
                  { "label": "W1", "objectType": "Strandwacht", "attributes": {},
                    "roles": { "stukken": [ "U1" ] }, "characteristics": [] },
                  { "label": "W2", "objectType": "Strandwacht", "attributes": {},
                    "roles": { "stukken": [ "U2" ] }, "characteristics": [] },
                  { "label": "U1", "objectType": "Uitrusting", "attributes": {}, "roles": {},
                    "characteristics": [ "gekeurd" ] },
                  { "label": "U2", "objectType": "Uitrusting", "attributes": {}, "roles": {},
                    "characteristics": [ "gekeurd" ] } ], "parameters": {} },
              "expected": [ { "kind": "characteristicPresent", "label": "S1", "characteristic": "uitgerust" },
                            { "kind": "consistent" } ] }
            """;
}
