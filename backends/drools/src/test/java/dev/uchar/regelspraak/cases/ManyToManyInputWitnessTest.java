package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieSession;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ch. 5.7a witness on the case input of a relation both of whose roles are to-many: {@code berging} of
 * {@code jutterswet-ketenkwantor}, where a Boot bergt several Vondsten and a Vondst is geborgen by several Boten. The bean
 * model stores a relation in the reference field of its one side, and this relation has none, so the case input reads
 * each link as a fact of its own ({@link Input#link}) and {@link DroolsBackend#insert} puts it in as a
 * {@code RelationLink} of phase 0, the input's. A supplied link establishes the relation in both directions, so the
 * case may write it from the Boot, from the Vondst or from both, and it is one link each time.
 *
 * <p>The expectations of the golden's cases are its own, replayed by gates 5, 6 and 12; this class pins what those
 * totals do not show: which links go in, that the three spellings give the same ones on both branches, and the
 * sentences with which {@code insert} refuses a link that does not fit the beans or the model.
 */
class ManyToManyInputWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden", "jutterswet-ketenkwantor");

    /** The links of case 1: B1 bergt V1 and V2, B2 bergt V2 and V3, V2 shared. Boot fills the first role. */
    private static final List<String> LINKS = List.of("berging: B1 V1", "berging: B1 V2", "berging: B2 V2",
            "berging: B2 V3");

    @Test void the_three_spellings_of_a_link_give_the_same_links_on_both_branches() throws Exception {
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            DroolsBackend.Build b = DroolsBackend.build(model(), branch);
            for (Map.Entry<String, JsonNode> spelling : spellings().entrySet())
                assertEquals(LINKS, links(b, spelling.getValue()), branch + ", " + spelling.getKey());
        }
    }

    @Test void insert_puts_each_link_in_once_as_a_fact_of_the_input() throws Exception {
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            DroolsBackend.Build b = DroolsBackend.build(model(), branch);
            TmsRepeatedFireWitnessTest.inSession(b, () -> {
                CaseInput in = CaseInput.of(b, spellings().get("both sides"));
                Map<Object, String> label = labels(in);
                KieSession session = b.kieBase().newKieSession();
                try {
                    DroolsBackend.insert(b, session, in.inputData());
                    List<String> facts = new ArrayList<>();
                    for (Object[] k : TmsCreationWitnessTest.links(session, "berging")) {
                        assertEquals(0, k[2], branch + ": a link of the input has phase 0");
                        facts.add("berging: " + label.get(k[0]) + " " + label.get(k[1]));
                    }
                    facts.sort(null);
                    assertEquals(LINKS, facts, branch + ": one fact per link, the one written from both sides once");
                } finally {
                    session.dispose();
                }
            });
        }
    }

    @Test void a_link_that_does_not_fit_refuses_insert_loudly() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(model(), DroolsBackend.Branch.EXEC_MODEL);
        TmsRepeatedFireWitnessTest.inSession(b, () -> {
            CaseInput in = CaseInput.of(b, read("case1-gedeelde-vondst-onder-de-drempel"));
            Map<String, Object> beans = in.beans();
            Object j1 = beans.get("J1"), b1 = beans.get("B1"), v1 = beans.get("V1");
            List<Object> all = new ArrayList<>(beans.values());
            List<Object> withoutV1 = new ArrayList<>(all);
            withoutV1.remove(v1);
            String b1v1 = "insert: de koppeling " + name(b1) + " - " + name(v1) + " van 'berging'";
            assertEquals("insert: de koppeling " + name(j1) + " - " + name(b1) + " van 'bemanning' noemt geen feittype"
                    + " van twee rollen die allebei meervoudig zijn; een relatie met een enkelvoudige rol staat in het"
                    + " referentieveld van haar ene kant", refusal(b, new Input().beans(all).link("bemanning", j1, b1)));
            assertEquals(b1v1 + " noemt " + name(v1) + ", dat geen bean van de invoer is",
                    refusal(b, new Input().beans(withoutV1).link("berging", b1, v1)));
            assertEquals("insert: de koppeling " + name(v1) + " - " + name(b1) + " van 'berging': " + name(v1)
                    + " is een Vondst en vult de rol 'bergende boot' van Boot niet",
                    refusal(b, new Input().beans(all).link("berging", v1, b1)));
            // the second statement names the fact type in another spelling of the same name, and is the one refused
            assertEquals("insert: de koppeling " + name(b1) + " - " + name(v1) + " van 'Berging' komt twee keer voor",
                    refusal(b, new Input().beans(all).link("berging", b1, v1).link("Berging", b1, v1)));
        });
    }

    // ---------------- fixtures ----------------

    /** The golden's document without its two rules: the case input and {@code insert} read the model only, and a build
     *  of the document refuses as long as a form of it does. */
    private static JsonNode model() throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(GOLDEN.resolve("ir.json")));
        ((ObjectNode) doc.get("rules")).set("rules", JSON.createArrayNode());
        return doc;
    }

    /** Case 1 in three spellings: as the golden writes it, from the Boot; from the Vondst only; and from both. */
    private static Map<String, JsonNode> spellings() throws Exception {
        Map<String, JsonNode> out = new java.util.LinkedHashMap<>();
        ObjectNode asWritten = read("case1-gedeelde-vondst-onder-de-drempel");
        out.put("from the Boot", asWritten);
        ObjectNode farSide = asWritten.deepCopy();
        for (JsonNode i : farSide.get("input").get("instances")) {
            ObjectNode roles = (ObjectNode) i.get("roles");
            if ("Boot".equals(i.get("objectType").asText())) roles.remove("geborgen vondst");
        }
        roles(farSide, "V1").set("bergende boot", labels("B1"));
        roles(farSide, "V2").set("bergende boot", labels("B1", "B2"));
        roles(farSide, "V3").set("bergende boot", labels("B2"));
        out.put("from the Vondst", farSide);
        ObjectNode both = asWritten.deepCopy();
        roles(both, "V2").set("bergende boot", labels("B1", "B2"));
        out.put("both sides", both);
        return out;
    }

    private static ObjectNode roles(ObjectNode c, String label) {
        for (JsonNode i : c.get("input").get("instances"))
            if (label.equals(i.get("label").asText())) return (ObjectNode) i.get("roles");
        throw new IllegalStateException("no instance " + label);
    }

    private static ArrayNode labels(String... labels) {
        ArrayNode out = JSON.createArrayNode();
        for (String l : labels) out.add(l);
        return out;
    }

    private static ObjectNode read(String name) throws Exception {
        Path p = GOLDEN.resolve(name + ".json");
        assertTrue(Files.exists(p), "golden case present: " + p.toAbsolutePath());
        return (ObjectNode) JSON.readTree(Files.readString(p));
    }

    /** The links the case input of {@code c} gives, as "fact type: first second" by label, sorted. */
    private static List<String> links(DroolsBackend.Build b, JsonNode c) throws Exception {
        List<String> out = new ArrayList<>();
        TmsRepeatedFireWitnessTest.inSession(b, () -> {
            CaseInput in = CaseInput.of(b, c);
            Map<Object, String> label = labels(in);
            for (Input.Link k : in.inputData().links())
                out.add(k.factType() + ": " + label.get(k.first()) + " " + label.get(k.second()));
        });
        out.sort(null);
        return out;
    }

    private static Map<Object, String> labels(CaseInput in) {
        Map<Object, String> out = new IdentityHashMap<>();
        in.beans().forEach((l, bean) -> out.put(bean, l));
        return out;
    }

    /** The sentence with which {@link DroolsBackend#insert} refuses {@code input} on a fresh session, which then holds
     *  no fact. */
    private static String refusal(DroolsBackend.Build b, Input input) {
        KieSession session = b.kieBase().newKieSession();
        try {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> DroolsBackend.insert(b, session, input));
            assertEquals(0, session.getFactCount(), "nothing was inserted: " + e.getMessage());
            return e.getMessage();
        } finally {
            session.dispose();
        }
    }

    /** An object in a sentence of {@code insert}: its type and its identity. */
    private static String name(Object o) {
        return o.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(o));
    }
}
