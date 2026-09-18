package io.github.yhavinga.regelspraak.cases;

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
 * reaches them by a different road — the exec branch collects the contributors and asks a leaf
 * predicate of each, the DRL branch denies the failure witness with a {@code not} pattern.
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
     * is — and it is the only one that reaches the exec branch's NON-temporal counting route, since gate 12
     * would otherwise only ever see the timeline route. The two lowerings are different roads again: the
     * exec branch counts over a {@code collectList} accumulate of the contributors, the DRL branch writes a
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
     * Beide Drools-takken dragen hem niet, en dat is de bewering van deze rij: allebei moeten zij hem BIJ
     * NAAM weigeren, met de matrixvorm in de reden.
     *
     * <p><b>Waarom juist deze vorm.</b> Zij is de eerste conditie die haar clausule RECHTSTREEKS in de Acc
     * hijst en een LEGE inline teruggeeft — de lowering van deze kaart doet dat zelf, dus zij kan zichzelf
     * als matrix niet aan. {@code conditieInline} ziet dat niet: die kent alleen het {@code extra}-kanaal.
     * Vóór de vangst van 2026-09-16 kwam er daarom een weesclausule uit die tegen {@code $subject} joinde met
     * een veld van het VERKEERDE type, plus een lege {@code !(  )}-constraint, en stierf het pakket op
     * {@code [ERR 101] no viable alternative at input ')'} — luid, maar een parsefout is niet de benoemde
     * weigering die ch. 3.12 belooft. Deze rij faalt zodra die vangst wegvalt.
     */
    @Test void een_geneste_vouw_als_matrix_wordt_door_beide_takken_bij_naam_geweigerd() throws Exception {
        Path ir = genesteVouw();
        Path c = tmp.resolve("genest-case.json");
        Files.writeString(c, GENEST_CASE);

        CaseRunner.Result exec = CaseRunner.viaExecModel(ir, List.of(c));
        assertEquals("EXCLUDE", exec.status(), "een niet-gelowerde vouw mag nooit meespelen");
        assertTrue(exec.reason().contains("Kwantorvouw"),
                "de exec-reden noemt de MATRIXVORM, niet alleen dat er een grens is: " + exec.reason());

        CaseRunner.Result drl = CaseRunner.viaDrlTekst(ir, List.of(c));
        assertEquals("EXCLUDE", drl.status(), "de DRL-tekst-tak mag dit document niet stil draaien");
        assertFalse(drl.reason().contains("drl compile"),
                "een parsefout is geen benoemde weigering — de vangst op het Acc-kanaal ontbreekt: " + drl.reason());
        assertTrue(drl.reason().contains("kwantorvouw") && drl.reason().contains("Kwantorvouw"),
                "de DRL-reden noemt de knoop EN de matrixvorm waarop hij afketst: " + drl.reason());
    }

    /**
     * De markertekst van de DRL-tekst-tak is PER REDEN waar. Tot 2026-09-16 zei elke weigering "ch. 3.9a
     * wordt alleen in de exec-model-tak gelowerd", ook waar de exec-tak juist óók weigert (zoals hierboven);
     * de redenen zijn sindsdien per decline.
     *
     * <p><b>De rij verschoof op 2026-09-18</b> (kaart drl-heelperiode-arm) en met haar de bewering. Tot dan
     * was de HEELPERIODEMATRIX de ene weigering die wél echt exec-only was, en zij was hier de tegenhanger
     * van de geneste vouw. Die kanaalsplitsing is dicht: die matrix speelt nu in beide takken, dus de rij
     * leest hem als een PLAY en houdt daarnaast vast dat de geneste vouw haar EIGEN reden draagt — en dat
     * geen enkele weigering nog beweert dat een vorm alleen in de exec-tak zou lowerden.
     */
    @Test void de_drl_markertekst_scheidt_de_redenen() throws Exception {
        Path ploegpremie = GOLDEN.resolveSibling("jutterswet-ploegpremie");
        CaseRunner.Result heel = CaseRunner.viaDrlTekst(ploegpremie.resolve("ir.json"),
                List.of(ploegpremie.resolve("case1-allen-boven-de-grens.json")));
        assertEquals("PLAYED", heel.status(),
                "de heelperiodevouw is geen kanaalsplitsing meer: " + heel.reason());

        Path c = tmp.resolve("genest-case-2.json");
        Files.writeString(c, GENEST_CASE);
        String genest = CaseRunner.viaDrlTekst(genesteVouw(), List.of(c)).reason();
        assertTrue(genest.contains("kwantorvouw") && genest.contains("Kwantorvouw"),
                "de geneste vouw draagt haar eigen reden, met de matrixvorm erin: " + genest);
        assertFalse(genest.contains("alleen in de exec-model-tak"),
                "de geneste vouw wordt door de exec-tak óók geweigerd, dus die zin is daar onwaar: " + genest);
    }

    // ================= the nested-fold document, spliced into the corpus golden =================

    /** The golden's document with a second relation ({@code Strandwacht} -&gt; {@code Uitrusting}) and ONE rule
     *  whose voorwaarde is a fold whose MATRIX is another fold. Written to the {@link TempDir}; the corpus files
     *  are read and never written. */
    private Path genesteVouw() throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(GOLDEN.resolve("ir.json")));
        ObjectNode ots = (ObjectNode) doc.get("objecttypen");
        ((ObjectNode) ots.get("Schouwploeg")).set("kenmerken", JSON.readTree("[ \"uitgerust\" ]"));
        ((ObjectNode) ots.get("Schouwploeg")).set("kenmerkSoorten",
                JSON.readTree("{ \"uitgerust\": \"BIJVOEGLIJK\" }"));
        ((ObjectNode) ots.get("Schouwploeg")).set("attributen", JSON.readTree("{}"));
        ots.set("Uitrusting", JSON.readTree("""
                { "node": "Objecttype", "naam": "Uitrusting", "attributen": {}, "attribuutMeervoud": {},
                  "attribuutDimensies": {}, "attribuutGranulariteit": {}, "attribuutTijdlijnAantal": {},
                  "attribuutTijdlijnNaam": {}, "kenmerken": [ "gekeurd" ],
                  "kenmerkSoorten": { "gekeurd": "BIJVOEGLIJK" }, "kenmerkGranulariteit": {},
                  "kenmerkTijdlijnAantal": {}, "bezield": false, "meervoud": "Uitrustingen" }"""));
        ((ObjectNode) doc.get("feittypen")).set("wachtuitrusting", JSON.readTree("""
                { "node": "Feittype", "naam": "wachtuitrusting", "wederkerig": false, "rollen": [
                    { "node": "Rol", "naam": "drager", "objecttypeNaam": "Strandwacht",
                      "meervoud": null, "meervoudig": false },
                    { "node": "Rol", "naam": "stuk", "objecttypeNaam": "Uitrusting",
                      "meervoud": "stukken", "meervoudig": true } ] }"""));
        doc.set("regels", JSON.readTree(
                "[ { \"node\": \"Regel\", \"naam\": \"uitgerust bij keuring\", \"soort\": \"regel\" } ]"));
        doc.set("resolvedRegels", JSON.readTree("""
                [ { "node": "ResolvedRegel", "naam": "uitgerust bij keuring", "soort": "KENMERKTOEKENNING",
                    "doel": { "node": "AttribuutRef", "attribuut": "uitgerust", "objecttype": "Schouwploeg",
                              "onderwerpBron": "Een Schouwploeg", "navigatieRol": null,
                              "type": { "node": "Booleaan" }, "bron": "Een Schouwploeg is uitgerust",
                              "set": false, "navigatiePad": [] },
                    "waarde": { "node": "Literal", "waarde": "waar", "type": { "node": "Booleaan" },
                                "bron": "waar" },
                    "voorwaarde": {
                      "node": "Kwantorvouw", "kwantor": "ALLE", "elementObjecttype": "Strandwacht",
                      "filter": null,
                      "toets": { "node": "Kwantorvouw", "kwantor": "ALLE", "elementObjecttype": "Uitrusting",
                                 "filter": null,
                                 "toets": { "node": "Kenmerk", "kenmerk": "gekeurd", "navigatie": null,
                                            "negatie": false, "eigenaar": null, "periode": null,
                                            "bron": "het stuk gekeurd is" },
                                 "bron": "alle stukken van de Strandwacht gekeurd zijn",
                                 "bronRol": { "node": "RolBinding", "feittype": "wachtuitrusting",
                                              "rol": "stuk" } },
                      "bron": "alle wachten van de Schouwploeg alle stukken gekeurd hebben",
                      "bronRol": { "node": "RolBinding", "feittype": "schouwindeling", "rol": "wacht" } },
                    "geldigheid": { "node": "Geldigheid", "vanaf": null, "tot": null } } ]"""));
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
