package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A count over an instance-set union whose selections range over two populations: the golden
 * {@code jutterswet-vondstunie} with its second selection, the geborgen vondsten, turned into the geborgen kavels of a
 * relation of its own. One count answers a number of instances of one declared type, and whether two selections of
 * two types share an instance is a question about the subtype lattice (IR-SPEC ch. 4.3, WF-110 (d)); the branch refuses
 * the form by name. Before, it counted the members of both selections, one contribution per instance, and answered.
 *
 * <p><b>Measured</b> 2026-10-10 on that document with the golden's case: the verifier refuses it, "WF-110 FAIL
 * …/branches/1/elementType: the selections of an instance-set union range over ONE population: selection 0 names
 * 'Vondst' and this one names 'Kavel' (ch. 4.3)"; the reference interpreter faults, "an instance-set union ranges over
 * TWO populations ("Vondst" and "Kavel"): a count answers a cardinality over ONE declared type, and whether two
 * selections can then share an instance is a question about the subtype lattice that deduplication would have to
 * answer", and the ASP runner declines it in the same words; the exec-model and the DRL-text branch refuse it by name,
 * "instantie-unie over twee populaties ('Vondst' en 'Kavel'): één telling antwoordt een aantal over ÉÉN gedeclareerd
 * type (WF-110 (d))".
 *
 * <p><b>Red</b> on the claim of the card, 331b1179: the manifest carried no refusal.
 */
class TmsUnionPopulationWitnessTest {

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden", "jutterswet-vondstunie");

    /** The golden with a Kavel, the relation kavelberging between a Jutter and its geborgen kavels, and the second
     *  selection of the count over that relation. */
    static ObjectNode twoPopulations() throws Exception {
        ObjectNode ir = (ObjectNode) TmsDocuments.JSON.readTree(Files.readString(GOLDEN.resolve("ir.json")));
        ((ArrayNode) ir.get("model").get("objectTypes")).add(TmsDocuments.JSON.readTree(
                "{\"node\":\"ObjectType\",\"name\":\"Kavel\",\"animate\":false,\"plural\":\"Kavels\",\"attributes\":[],"
                        + "\"characteristics\":[]}"));
        ((ArrayNode) ir.get("model").get("factTypes")).add(TmsDocuments.JSON.readTree(
                "{\"node\":\"FactType\",\"name\":\"kavelberging\",\"reciprocal\":false,\"roles\":[{\"node\":\"Role\","
                        + "\"name\":\"kavelberger\",\"objectType\":\"Jutter\",\"many\":false},{\"node\":\"Role\",\"name\":"
                        + "\"geborgen kavel\",\"objectType\":\"Kavel\",\"plural\":\"geborgen kavels\",\"many\":true}]}"));
        ObjectNode branch = (ObjectNode) IrPath.version(ir, "rules", 0, 0).get("value").get("arguments").get(0)
                .get("branches").get(1);
        branch.put("elementType", "Kavel");
        ((ObjectNode) branch.get("sourceRole")).put("factType", "kavelberging").put("role", "geborgen kavel");
        ((ObjectNode) branch.get("source")).put("snippet", "geborgen kavels van de Jutter");
        return ir;
    }

    @Test void a_union_over_two_populations_is_refused_by_name() throws Exception {
        Path ir = Files.writeString(Files.createTempFile("tms-uniepopulatie", ".json"),
                TmsDocuments.JSON.writeValueAsString(twoPopulations()));
        ir.toFile().deleteOnExit();
        assertEquals(List.of("transpiler SKIP: regel: aantal strandvondsten van een jutter — een InstanceSetUnion over"
                        + " twee populaties ('Vondst' en 'Kavel', 'het aantal aangemelde vondsten van de Jutter en geborgen"
                        + " vondsten van de Jutter'): één telling antwoordt een aantal over één gedeclareerd type, en of twee"
                        + " selecties dan een instantie delen, is een vraag over de subtyperelatie (WF-110 (d))"),
                TmsTimelineRefusalWitnessTest.refusals(ir));
    }

    /** Writes the refused document with the golden's case under {@code args[0]}. */
    public static void main(String[] args) throws Exception {
        TmsTimelineWitnessTest.write(Path.of(args[0]).resolve("twee-populaties"),
                TmsDocuments.JSON.writeValueAsString(twoPopulations()),
                TmsDocuments.JSON.readTree(Files.readString(GOLDEN.resolve("case.json"))));
    }
}
