package io.github.yhavinga.regelspraak.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * IR-SPEC ch. 4.3 / WF-037 — a fold that names its population ({@code elementType}) and hands no source is refused
 * while loading, in the verifier's sentence, so neither branch lowers it. Three shapes: no argument, under every
 * kind; one {@code ListLiteral} under a count or a filled count; and under every kind one node of any other kind but
 * an {@code AttributeReference}, the fold source (the card aantal-bronarm-en-drools-lader). Beside them WF-110 (c):
 * a count over an instance-set union names no source of its own.
 *
 * <p>Measured before that card, with the population of golden {@code jutterswet-vondsttelling} (J1 reaches three
 * Vondsten, J2 none, J3 one; expected counts 3, 0 and 1): a count beside its range whose one argument was a
 * {@code Literal}, a {@code Binary}, a {@code ParameterReference}, a {@code VariableReference}, a
 * {@code BuiltinCall}, an {@code Unresolved} marker or a nested {@code Aggregation} PLAYED in both branches — it
 * counted the instances the range reaches and never read the argument — and so did a count without an argument
 * beside its range; the DRL-text branch played the filled count with such an argument, and the filled count
 * without one beside its range, as an ordinary count. The rest was refused by scope notes (transpiler SKIP,
 * {@code TODO aggregatie}) or, for the {@code ListLiteral}, as a node this backend does not carry — never by the
 * shape. The verifier refuses all of them under WF-037, and {@code ref} and the ASP backend in the same sentences.
 *
 * <p>Every document here is a corpus negatief of that rule, read and never written, or an in-memory variant of
 * one; each row pins the full sentence at the node's position, character for character as the verifier reports
 * it (the negatief's own {@code expect.txt} pins a fragment of it for gate 3).
 */
class TellingBronarmWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path NEGATIEF = Path.of("..", "..", "corpus", "negatief");
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final String PLEK = "/rules/rules/0/versions/0/value";
    private static final String BRONARM = " operand: the source of an instance fold is an AttributeReference";
    private static final String GEEN_BRON = " but no source operand: an instance fold ranges over a written source";
    private static final String UNIE = "a count over an instance-set union names no source of its own, and this node"
            + " carries ";
    private static final String UNIE_STAART = ": the union's selections carry the population, the relation and the"
            + " subselection, one set per selection, so a source here is a second claim on one node (ch. 4.3)";
    private static final String GEHEEL = "{ \"node\": \"Number\", \"whole\": true, \"decimals\": 0, \"sign\": \"unspecified\" }";

    // ---------------- 1. the fold-source arm: one node that is no AttributeReference ----------------

    @Test void een_literal_als_telbron_wordt_geweigerd() throws Exception {
        weigert("wf037-aantal-elementtype-literaal", "a count aggregation carries elementType \"Vondst\" and a Literal"
                + BRONARM);
    }

    @Test void een_binary_als_telbron_wordt_geweigerd() throws Exception {
        weigert("wf037-aantal-elementtype-binair", "a count aggregation carries elementType \"Vondst\" and a Binary"
                + BRONARM);
    }

    @Test void een_parameter_als_telbron_wordt_geweigerd() throws Exception {
        weigert("wf037-aantal-elementtype-parameter", "a count aggregation carries elementType \"Vondst\" and a"
                + " ParameterReference" + BRONARM);
    }

    @Test void een_variabele_als_telbron_wordt_geweigerd() throws Exception {
        weigert("wf037-aantal-elementtype-variabele", "a count aggregation carries elementType \"Vondst\" and a"
                + " VariableReference" + BRONARM);
    }

    @Test void een_builtin_als_telbron_wordt_geweigerd() throws Exception {
        weigert("wf037-aantal-elementtype-builtin", "a count aggregation carries elementType \"Vondst\" and a"
                + " BuiltinCall" + BRONARM);
    }

    @Test void een_marker_als_telbron_wordt_geweigerd_en_niet_verlaagd() throws Exception {
        // before: PLAYED in both branches, so the marker was lowered silently — the article follows the vowel
        weigert("wf037-aantal-elementtype-onopgelost", "a count aggregation carries elementType \"Vondst\" and an"
                + " Unresolved" + BRONARM);
    }

    @Test void een_geneste_aggregatie_als_telbron_wordt_geweigerd() throws Exception {
        // the argument is itself a well-formed count; what is refused is its position, not its content
        ObjectNode ir = negatief("wf037-aantal-elementtype-literaal");
        ObjectNode telling = (ObjectNode) waarde(ir);
        ObjectNode binnen = telling.deepCopy();
        binnen.putArray("arguments").addObject().put("node", "AttributeReference").put("attribute", "vondsten")
                .put("subjectType", "Jutter");
        telling.putArray("arguments").add(binnen);
        weigert(ir, "a count aggregation carries elementType \"Vondst\" and an Aggregation" + BRONARM);
    }

    @Test void een_literal_als_bron_van_de_gevulde_telling_wordt_geweigerd() throws Exception {
        // before: the DRL-text branch played it as an ordinary count, the exec-model branch skipped the rule
        weigert("wf037-aantalgevulde-niet-setbron", "a filledCount aggregation carries elementType \"Vondst\" and a"
                + " Literal" + BRONARM);
    }

    @Test void een_literal_als_bron_van_de_som_wordt_geweigerd() throws Exception {
        // before: the DRL-text branch crashed on it (ClassCastException), the exec-model branch skipped the rule
        weigert("wf037-som-elementtype-literaal", "a sum aggregation carries elementType \"Vondst\" and a Literal"
                + BRONARM);
    }

    @Test void een_parameter_als_bron_van_de_som_wordt_geweigerd() throws Exception {
        weigert("wf037-som-elementtype-parameter", "a sum aggregation carries elementType \"Vondst\" and a"
                + " ParameterReference" + BRONARM);
    }

    @Test void een_literal_als_eigen_lezing_van_het_totaal_wordt_geweigerd() throws Exception {
        // a total without a range: the argument is the subject's own read, and a literal is no read. Before: the
        // verifier passed it, and no branch ran it (transpiler SKIP, DRL marker)
        weigert("wf037-totaal-elementtype-literaal", "a total aggregation carries elementType \"Jutter\" and a Literal"
                + BRONARM);
    }

    // ---------------- 2. the converse arm: no argument, or one ListLiteral ----------------

    @Test void een_telling_zonder_argument_wordt_geweigerd() throws Exception {
        weigert("wf037-aantal-elementtype-zonder-operand", "a count aggregation carries elementType \"Vondst\""
                + GEEN_BRON);
    }

    @Test void een_telling_zonder_argument_naast_haar_bereik_wordt_geweigerd() throws Exception {
        // the variant that PLAYED in both branches before: the range written, the argument list empty
        ObjectNode ir = negatief("wf037-aantal-elementtype-zonder-operand");
        ((ObjectNode) waarde(ir)).putObject("sourceRole").put("node", "RoleBinding")
                .put("factType", "vondstaanmelding").put("role", "vondst");
        weigert(ir, "a count aggregation carries elementType \"Vondst\"" + GEEN_BRON);
    }

    @Test void een_gevulde_telling_zonder_argument_wordt_geweigerd() throws Exception {
        weigert("wf037-gevuldetelling-elementtype-zonder-operand", "a filledCount aggregation carries elementType"
                + " \"Vondst\"" + GEEN_BRON);
    }

    @Test void een_som_zonder_argument_wordt_geweigerd() throws Exception {
        weigert("wf037-som-elementtype-zonder-operand", "a sum aggregation carries elementType \"Vondst\""
                + GEEN_BRON);
    }

    @Test void een_lijstliteraal_als_telbron_wordt_geweigerd_als_vorm_en_niet_als_knoop() throws Exception {
        weigert("wf037-aantal-elementtype-lijstliteraal", "a count aggregation carries elementType \"Vondst\" and a"
                + " ListLiteral operand: a listed value is no source of instances");
    }

    @Test void een_lijstliteraal_als_bron_van_de_gevulde_telling_wordt_geweigerd() throws Exception {
        weigert("wf037-gevuldetelling-elementtype-lijstliteraal", "a filledCount aggregation carries elementType"
                + " \"Vondst\" and a ListLiteral operand: a listed value is no source of instances");
    }

    // ---------------- 3. WF-110 (c): the count over a union names no source of its own ----------------

    @Test void een_elementtype_met_bereik_naast_de_unie_wordt_geweigerd() throws Exception {
        // before: PLAYED in both branches, the union counted and the node's own source never read
        weigert("wf110-unie-naast-vouwbron", UNIE + "elementType + sourceRole" + UNIE_STAART);
    }

    @Test void een_bereik_alleen_naast_de_unie_wordt_geweigerd() throws Exception {
        ObjectNode ir = negatief("wf110-unie-naast-vouwbron");
        ((ObjectNode) waarde(ir)).remove("elementType");
        weigert(ir, UNIE + "sourceRole" + UNIE_STAART);
    }

    @Test void een_filter_alleen_naast_de_unie_wordt_geweigerd() throws Exception {
        ObjectNode ir = negatief("wf110-unie-naast-vouwbron");
        ObjectNode telling = (ObjectNode) waarde(ir);
        telling.remove("elementType");
        telling.remove("sourceRole");
        telling.set("filter", JSON.readTree("{ \"node\": \"Comparison\", \"op\": \"greater\","
                + " \"left\": { \"node\": \"Literal\", \"value\": { \"number\": \"1\" }, \"type\": " + GEHEEL + " },"
                + " \"right\": { \"node\": \"Literal\", \"value\": { \"number\": \"0\" }, \"type\": " + GEHEEL + " } }"));
        weigert(ir, UNIE + "filter" + UNIE_STAART);
    }

    // ---------------- 4. the boundary ----------------

    @Test void een_telling_over_haar_bron_laadt() throws Exception {
        // the golden's count source, an AttributeReference beside its range: the arm leaves it alone
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(
                GOLDEN.resolve("jutterswet-vondsttelling").resolve("ir.json")));
        assertDoesNotThrow(() -> IrLoader.loadAll(ir));
    }

    // ---------------- fixtures ----------------

    private static ObjectNode negatief(String naam) throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(NEGATIEF.resolve(naam).resolve("ir.json")));
    }

    private static com.fasterxml.jackson.databind.JsonNode waarde(ObjectNode ir) {
        return ir.get("rules").get("rules").get(0).get("versions").get(0).get("value");
    }

    private static void weigert(String naam, String zin) throws Exception {
        weigert(negatief(naam), zin);
    }

    /** The load refusal, for both branches: the loader is theirs in common, so nothing is lowered. */
    private static void weigert(ObjectNode ir, String zin) {
        IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.loadAll(ir));
        assertEquals(PLEK, e.plek(), e.getMessage());
        assertEquals("malformed uchar-ir-v1 document at '" + PLEK + "': " + zin, e.getMessage());
    }
}
