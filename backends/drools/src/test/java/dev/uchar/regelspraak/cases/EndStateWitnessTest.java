package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import dev.uchar.regelspraak.runtime.Breuk;
import dev.uchar.regelspraak.transpiler.tms.Fixpoint;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The outcome reader {@link DroolsBackend#endStateOf} (RUNTIME-SEMANTICS ch. 5.12a), on both branches: an object a
 * creation made stands with its type, its creation, its subject and its members, and its link to its subject; and a
 * value of the unbounded form that does not terminate stands exact, where {@link Fixpoint} gives the decimal the bean
 * takes. Gate 17 holds the text of the end state to the reference interpreter's over the corpus; this holds the reader's
 * records, which the text does not show: the beans and the exact rational.
 */
class EndStateWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    private record Read(EndState state, Fixpoint fixpoint, Map<String, Object> labels, List<String> lines) {}

    private static Read read(String golden, String caseFile, DroolsBackend.Branch branch) throws Exception {
        Path dir = GOLDEN.resolve(golden);
        JsonNode c = TmsDocuments.JSON.readTree(dir.resolve(caseFile).toFile());
        return CaseRunner.forMeasurement(dir.resolve("ir.json"), branch).withSession(c, (build, session, labels) -> {
            DroolsBackend.Firing firing = DroolsBackend.fire(build, session);
            if (firing.noFixpoint() != null) throw new IllegalStateException("no fixpoint: " + firing.noFixpoint());
            Map<Object, String> label = new IdentityHashMap<>();
            labels.forEach((l, b) -> label.put(b, l));
            return new Read(DroolsBackend.endStateOf(build, session, label::get),
                    DroolsBackend.fixpointOf(build, session, label::get), Map.copyOf(labels),
                    SessionDump.endStateLines(build, session, labels));
        });
    }

    private static EndState.Member member(EndState.EndObject o, String name) {
        return o.members().stream().filter(m -> m.name().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError(o.type() + " has no member " + name + ": " + o.members()));
    }

    @Test void a_made_object_stands_with_its_creation_its_subject_its_members_and_its_link() throws Exception {
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            Read r = read("jutterswet-penningbesluit", "case1-twee-besluiten.json", branch);
            List<EndState.EndObject> objects = r.state().objects();
            assertEquals(List.of("J1", "J2"), objects.stream().filter(o -> o.label() != null)
                    .map(EndState.EndObject::label).toList(), branch + ": the objects of the input");
            List<EndState.EndObject> made = objects.stream().filter(o -> o.label() == null).toList();
            assertEquals(2, made.size(), branch + ": the made objects " + made);
            for (EndState.EndObject m : made) {
                assertEquals("Penningbesluit", m.type());
                assertEquals("stel het penningbesluit vast", m.creation());
                Object subject = m.subject();
                assertTrue(subject == r.labels().get("J1") || subject == r.labels().get("J2"), branch + ": the subject");
                assertEquals(0, new BigDecimal("250").compareTo((BigDecimal) member(m, "besluitbedrag").value()),
                        branch + ": the member assignment");
                assertNull(member(m, "besluitbedrag").cell());
                long links = r.state().links().stream()
                        .filter(k -> k.first() == subject && k.second() == m.object()).count();
                assertEquals(1, links, branch + ": the link of the made object to its subject");
            }
            assertTrue(r.lines().contains("object «\"stel het penningbesluit vast\"»(\"J1\")#1 \"Penningbesluit\""),
                    branch + ": " + r.lines());
        }
    }

    @Test void a_value_that_does_not_terminate_stands_exact() throws Exception {
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            Read r = read("anchor/jutterswet-verzuimrente-dagen", "case1-voorbeeld.json", branch);
            EndState.EndObject a1 = r.state().objects().stream().filter(o -> "A1".equals(o.label())).findFirst()
                    .orElseThrow();
            Breuk exact = assertInstanceOf(Breuk.class, member(a1, "verzuimrente").value(), branch + ": exact");
            assertEquals("10160/73", exact.teller() + "/" + exact.noemer(), branch.toString());
            Fixpoint.DerivedValue derived = r.fixpoint().values().stream()
                    .filter(v -> v.object() == a1.object() && v.attribute().equals("verzuimrente")).findFirst()
                    .orElseThrow();
            assertInstanceOf(BigDecimal.class, derived.value(), branch + ": the bean takes a decimal");
            assertSame(derived.exact().getClass(), Breuk.class, branch + ": the fixpoint keeps the exact value beside it");
            assertTrue(r.lines().contains("attribute \"A1\" \"verzuimrente\" 10160/73 \"EUR\""), branch + ": " + r.lines());
        }
    }
}
