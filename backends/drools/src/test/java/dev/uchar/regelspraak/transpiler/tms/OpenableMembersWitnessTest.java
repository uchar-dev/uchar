package dev.uchar.regelspraak.transpiler.tms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.graph.ProducerConsumer;
import dev.uchar.regelspraak.graph.ProducerConsumer.ElementKey;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The members that can be open on an object ({@link OpenableMembers}), over small documents: the input that is not
 * complete, per type of the object; what follows from it along the reads of the forms; and none in a document without
 * the property. The documents are the fixtures of {@code OpenValuesWitnessTest}, one of them with a field
 * {@code complete} added, and a witness of the reference interpreter with a creation.
 */
class OpenableMembersWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path FIXTURES = Path.of("src", "test", "resources", "open-values");

    private record Document(IrLoader.Loaded loaded, OpenableMembers openable) {

        /** The members of every type, "type.member", for which {@code asked} answers yes, ascending. */
        private Set<String> members(java.util.function.Predicate<ElementKey> asked) {
            Set<String> out = new TreeSet<>();
            for (ObjectType t : loaded.model().objectTypes.values()) {
                List<ElementKey> own = new ArrayList<>();
                for (String a : t.attributes.keySet()) own.add(ProducerConsumer.elementOf(t, a, false));
                for (String k : t.characteristics) own.add(ProducerConsumer.elementOf(t, k, true));
                for (ElementKey m : own) if (asked.test(m)) out.add(m.objectType() + "." + m.name());
            }
            return out;
        }

        /** The members that are input and not complete on an object of type {@code type}, in their order. */
        private List<String> incomplete(String type) {
            return openable.incompleteInput(loaded.model().objectTypeByName(type)).stream()
                    .map(m -> m.objectType() + "." + m.name()).toList();
        }
    }

    private static Document document(JsonNode json) throws Exception {
        IrLoader.Loaded loaded = IrLoader.loadAll(json);
        return new Document(loaded, OpenableMembers.of(loaded.model(), loaded.prioritizedDefaults(),
                ProducerConsumer.wireNavigation(loaded)));
    }

    private static JsonNode json(Path ir) throws Exception {
        return JSON.readTree(Files.readString(ir));
    }

    private static JsonNode fixture(String name) throws Exception {
        return json(FIXTURES.resolve(name).resolve("ir.json"));
    }

    /** The declaration of member {@code name} of the first type of {@code document}. */
    private static ObjectNode attribute(JsonNode document, String name) {
        for (JsonNode a : document.get("model").get("objectTypes").get(0).get("attributes"))
            if (a.get("name").asText().equals(name)) return (ObjectNode) a;
        throw new IllegalArgumentException(name);
    }

    @Test void no_member_of_a_document_without_the_property() throws Exception {
        Document d = document(fixture("no-open-without-the-property"));
        assertEquals(Set.of(), d.members(d.openable()::contains));
        assertEquals(List.of(), d.incomplete("Vondst"));
    }

    /** Input without a writer can be open, an attribute and a characteristic, and what a form computes from it. */
    @Test void input_and_what_reads_it() throws Exception {
        Document d = document(fixture("open-input"));
        assertEquals(List.of("vondst.vindloon", "vondst.bergloon", "vondst.drijfhout"), d.incomplete("Vondst"));
        assertEquals(Set.of("vondst.bergloon", "vondst.drijfhout", "vondst.vindloon"), d.members(d.openable()::openInput));
        assertEquals(Set.of("vondst.drijftoeslag", "vondst.uitbetaling"), d.members(d.openable()::leftOpen));
        assertEquals(5, d.members(d.openable()::contains).size());
    }

    /** A complete input is not open, and what reads only such inputs is not open either. */
    @Test void complete_input() throws Exception {
        JsonNode json = fixture("open-input");
        attribute(json, "vindloon").put("complete", true);
        Document one = document(json);
        assertEquals(List.of("vondst.bergloon", "vondst.drijfhout"), one.incomplete("Vondst"));
        assertEquals(Set.of("vondst.drijftoeslag", "vondst.uitbetaling"), one.members(one.openable()::leftOpen));
        attribute(json, "bergloon").put("complete", true);
        Document both = document(json);
        assertEquals(List.of("vondst.drijfhout"), both.incomplete("Vondst"));
        assertEquals(Set.of("vondst.drijftoeslag"), both.members(both.openable()::leftOpen));
        assertEquals(Set.of("vondst.drijfhout", "vondst.drijftoeslag"), both.members(both.openable()::contains));
    }

    /** A chain of rules: the reader of a member a writer can leave open can be left open, before its writer in the
     *  document or after it. */
    @Test void a_chain_of_rules() throws Exception {
        Document d = document(fixture("derived"));
        assertEquals(List.of("vondst.taxatie", "vondst.bergloon"), d.incomplete("Vondst"));
        assertEquals(Set.of("vondst.beloond", "vondst.bewakingstoeslag", "vondst.kostbaar", "vondst.toeslag",
                "vondst.uitbetaling", "vondst.vindloon"), d.members(d.openable()::leftOpen));
        JsonNode json = fixture("derived");
        attribute(json, "taxatie").put("complete", true);
        Document complete = document(json);
        assertEquals(Set.of("vondst.uitbetaling"), complete.members(complete.openable()::leftOpen));
    }

    /** Whether a member is input is asked per type of the object: the vindloon a rule of a Strandvondst writes is input
     *  on a Vondst that is none, and follows the rule on a Strandvondst. */
    @Test void a_writer_on_a_subtype() throws Exception {
        Document d = document(fixture("input-per-object"));
        assertEquals(List.of("vondst.taxatie", "vondst.vindloon"), d.incomplete("Vondst"));
        assertEquals(List.of("vondst.taxatie"), d.incomplete("Strandvondst"));
        assertEquals(Set.of("vondst.taxatie", "vondst.vindloon"), d.members(d.openable()::openInput));
        assertEquals(Set.of("vondst.vindloon"), d.members(d.openable()::leftOpen));
    }

    /** A creation that reads a member that can be open makes every form count as reading one, since it can leave a
     *  relation open, which the reads of the forms do not name. */
    @Test void a_creation_that_can_leave_a_relation_open() throws Exception {
        Document d = document(json(Path.of("..", "..", "ref", "test", "fixtures", "strict-empty", "object-creation", "ir.json")));
        Set<String> written = new TreeSet<>();
        for (ProducerConsumer.Form f : ProducerConsumer.forms(d.loaded().model(), d.loaded().prioritizedDefaults(),
                ProducerConsumer.wireNavigation(d.loaded())))
            for (ElementKey m : f.writes()) written.add(m.objectType() + "." + m.name());
        assertEquals(written, d.members(d.openable()::leftOpen));
    }
}
