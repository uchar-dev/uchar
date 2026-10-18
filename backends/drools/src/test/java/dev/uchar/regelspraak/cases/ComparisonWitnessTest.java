package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.engine.ScenarioResultaat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.kie.api.runtime.KieSession;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The comparison of the expectations of a case with a session, through the backend's public entry:
 * {@code DroolsBackend.vergelijk(build, session, label, expected)}, after {@code vuur} and
 * {@code zetAfgeleideWaarden}. A consumer that fires a session itself, as the distribution's decision service does,
 * gets the verdict the scenario report gives ({@code CaseRunner.rapport}, whose replay compares through the same entry)
 * without a copy of the comparison. Every session here is set up as the replay sets it up ({@code CaseRunner.withSession})
 * and fired, written back and compared through the entry alone.
 *
 * <p>The rows pin what the report cannot show: that the labels are the caller's, given by a function from bean to label
 * as a decision service gives the ids of its request, and need not be those of the case file; and the refusals of the
 * entry, of a session that has no answer to compare or that changed after its fixpoint, of a label function that gives
 * one label to two beans, and of an expectation on a label no bean of the session carries, where the replay read "leeg"
 * or "afwezig" until this entry and so let attributeEmpty and characteristicAbsent hold on a label that names nothing.
 */
class ComparisonWitnessTest {

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final Path KUSTWACHT = GOLDEN.resolve("anchor").resolve("jutterswet-kustwacht");
    private static final Path KUSTREGISTER = GOLDEN.resolve("anchor").resolve("jutterswet-kustregister");
    private static final Path BORGDEEL = GOLDEN.resolve("anchor").resolve("jutterswet-borgdeel");
    /** The labels of a decision service: the ids of its request, here the case label after this prefix. */
    private static final String REQUEST = "verzoek/";

    /** Played goldens whose cases together carry every kind of {@code expected}: attribute values (input and derived,
     *  numbers and dates), attributeEmpty, attributeTimeline, characteristics present and absent (derived and not),
     *  consistent, inconsistent bare, with a rule, and with a rule and a label, also where a deposit of the rule lies on
     *  an object a creation made, which no label names. */
    private static final List<String[]> CASES = List.of(
            new String[]{"anchor/jutterswet-kustwacht", "case1.json"},
            new String[]{"anchor/jutterswet-jutleeftijd", "case3-leeg.json"},
            new String[]{"anchor/jutterswet-borgdeel", "case1.json"},
            new String[]{"anchor/jutterswet-kustregister", "case2.json"},
            new String[]{"anchor/jutterswet-wachtersketen", "case1.json"},
            new String[]{"anchor/jutterswet-wachtersketen", "case2.json"},
            new String[]{"jutterswet-meldingsketen", "case2-herstelde-melding.json"});

    private static JsonNode lees(Path pad) throws Exception {
        return TmsDocuments.JSON.readTree(pad.toFile());
    }

    /** The label {@code id} gives the case label of an input bean, by identity, and null for any other object: the
     *  function a decision service gives with the ids of its request. */
    private static Function<Object, String> labelFunction(Map<String, Object> labels, UnaryOperator<String> id) {
        Map<Object, String> perBean = new IdentityHashMap<>();
        labels.forEach((l, beanObject) -> perBean.put(beanObject, id.apply(l)));
        return perBean::get;
    }

    /** {@code verwachtingen} with every label {@code l} replaced by {@code id(l)}. */
    private static JsonNode relabelled(JsonNode verwachtingen, UnaryOperator<String> id) {
        JsonNode out = verwachtingen.deepCopy();
        for (JsonNode v : out)
            if (v.hasNonNull("label")) ((ObjectNode) v).put("label", id.apply(v.get("label").asText()));
        return out;
    }

    /** Case {@code c} of {@code golden} on {@code branch}, set up as the replay sets it up, fired and written back through
     *  the entry with the labels {@code id} gives, and compared with {@code verwachtingen}. */
    private static DroolsBackend.Vergelijking vergelijk(Path golden, JsonNode c, DroolsBackend.Branch branch,
                                                        UnaryOperator<String> id, JsonNode verwachtingen) throws Exception {
        return CaseRunner.forMeasurement(golden.resolve("ir.json"), branch).withSession(c, (bouw, sessie, labels) -> {
            Function<Object, String> label = labelFunction(labels, id);
            assertNull(DroolsBackend.vuur(bouw, sessie).geenFixpunt(), branch + ": the fire reaches its fixpoint");
            DroolsBackend.zetAfgeleideWaarden(bouw, sessie, label);
            return DroolsBackend.vergelijk(bouw, sessie, label, verwachtingen);
        });
    }

    /** What the entry throws on case {@code c} of {@code golden}, set up as the replay sets it up, after {@code voor}
     *  did what it does with the session, with the labels {@code label} makes of the case labels. */
    private static <X extends Throwable> X refusal(Class<X> soort, Path golden, JsonNode c,
                                                     Function<Map<String, Object>, Function<Object, String>> label,
                                                     JsonNode verwachtingen, Preparation before) throws Exception {
        return CaseRunner.forMeasurement(golden.resolve("ir.json"), DroolsBackend.Branch.EXEC_MODEL).withSession(c,
                (bouw, sessie, labels) -> {
                    Function<Object, String> l = label.apply(labels);
                    before.doe(bouw, sessie, l);
                    return assertThrows(soort, () -> DroolsBackend.vergelijk(bouw, sessie, l, verwachtingen));
                });
    }

    @FunctionalInterface
    private interface Preparation {
        void doe(DroolsBackend.Build bouw, KieSession sessie, Function<Object, String> label);
    }

    private static final Preparation FIRED_AND_WRITTEN_BACK = (bouw, sessie, label) -> {
        assertNull(DroolsBackend.vuur(bouw, sessie).geenFixpunt());
        DroolsBackend.zetAfgeleideWaarden(bouw, sessie, label);
    };

    private static Function<Map<String, Object>, Function<Object, String>> caselabels() {
        return labels -> labelFunction(labels, l -> l);
    }

    private static ScenarioResultaat rapport(Path golden, Path casePath) {
        return CaseRunner.rapport(golden.resolve("ir.json"), List.of(casePath)).get(0);
    }

    /** The diagnose of the report without the name of the case file and ": " before it. */
    private static String diagnose(ScenarioResultaat r) {
        return r.diagnose().isEmpty() ? "" : r.diagnose().substring(r.naam().length() + 2);
    }

    @Test void the_entry_gives_on_both_branches_what_the_report_gives() throws Exception {
        Set<String> kinds = new TreeSet<>();
        for (String[] entry : CASES) {
            Path golden = GOLDEN.resolve(entry[0]);
            JsonNode c = lees(golden.resolve(entry[1]));
            for (JsonNode v : c.get("expected"))
                kinds.add(v.get("kind").asText() + (v.has("rule") ? "+rule" : "") + (v.has("label")
                        && v.get("kind").asText().equals("inconsistent") ? "+label" : ""));
            ScenarioResultaat r = rapport(golden, golden.resolve(entry[1]));
            assertEquals(ScenarioResultaat.Status.PASS, r.status(), entry[0] + "/" + entry[1]);
            for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
                DroolsBackend.Vergelijking g = vergelijk(golden, c, branch, l -> l, c.get("expected"));
                assertEquals(r.verwachtingen(), g.verwachtingen(), branch + " " + entry[0] + "/" + entry[1]);
                assertEquals("", g.diagnose(), branch + " " + entry[0] + "/" + entry[1]);
                assertTrue(g.pass(), branch + " " + entry[0] + "/" + entry[1]);
            }
        }
        assertEquals(Set.of("attributeEmpty", "attributeTimeline", "attributeValue", "characteristicAbsent",
                "characteristicPresent", "consistent", "inconsistent", "inconsistent+rule",
                "inconsistent+rule+label"), kinds, "the rows carry every kind of expected");
    }

    /** A decision service names the beans of its session by the ids of its request: the labels of the case file are
     *  not in the session. With the expectations in the ids the verdict is the report's, up to the labels, the subject
     *  of a §9.5 deposit among them; with the labels of the case file the entry finds no bean and says so. */
    @Test void the_labels_are_those_of_the_caller() throws Exception {
        JsonNode c = lees(KUSTREGISTER.resolve("case2.json"));
        ScenarioResultaat r = rapport(KUSTREGISTER, KUSTREGISTER.resolve("case2.json"));
        UnaryOperator<String> id = l -> REQUEST + l;
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            DroolsBackend.Vergelijking g = vergelijk(KUSTREGISTER, c, branch, id, relabelled(c.get("expected"), id));
            assertEquals(List.of("attributeValue verzoek/G1 kavelaantal", "inconsistent (eenmalig vondstnummer @ verzoek/S1)",
                    "inconsistent (eenmalig vondstnummer @ verzoek/W1)"),
                    g.verwachtingen().stream().map(ScenarioResultaat.VerwachtingResultaat::bron).toList(), branch::name);
            assertEquals("inconsistent (eenmalig vondstnummer @ verzoek/S1, eenmalig vondstnummer @ verzoek/W1)",
                    g.verwachtingen().get(1).werkelijk(), branch + ": the subject of a deposit has the caller's label");
            List<ScenarioResultaat.VerwachtingResultaat> withoutRequest = g.verwachtingen().stream()
                    .map(v -> new ScenarioResultaat.VerwachtingResultaat(v.bron().replace(REQUEST, ""),
                            v.verwacht().replace(REQUEST, ""), v.werkelijk().replace(REQUEST, ""), v.ok(), v.positie()))
                    .toList();
            assertEquals(r.verwachtingen(), withoutRequest, branch::name);
            assertTrue(g.pass(), branch::name);

            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> vergelijk(KUSTREGISTER, c, branch, id, c.get("expected")));
            assertEquals("attributeValue-verwachting op onbekend label 'G1': geen bean van de sessie draagt het, dus er"
                    + " is niets te lezen", e.getMessage(), branch::name);
        }
    }

    /** An expectation that does not hold: the verdict per expectation and the diagnose the report gives for the same
     *  case file, after the name of the file. */
    @Test void a_deviation_gives_the_diagnostic_of_the_report(@TempDir Path tmp) throws Exception {
        ObjectNode c = (ObjectNode) lees(KUSTWACHT.resolve("case1.json"));
        ((ObjectNode) c.get("expected").get(0)).putObject("value").put("number", "16");
        ((ObjectNode) c.get("expected").get(7)).put("kind", "characteristicPresent");
        Path file = Files.writeString(tmp.resolve("case-afwijkend.json"), c.toString());
        ScenarioResultaat r = rapport(KUSTWACHT, file);
        assertEquals(ScenarioResultaat.Status.FAIL, r.status());
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            DroolsBackend.Vergelijking g = vergelijk(KUSTWACHT, c, branch, l -> l, c.get("expected"));
            assertFalse(g.pass(), branch::name);
            assertEquals("attribuut uitkijkbonus: verwacht 16, werkelijk 15; kenmerk risicopost: verwacht aanwezig,"
                    + " werkelijk afwezig", g.diagnose(), branch::name);
            assertEquals(diagnose(r), g.diagnose(), branch::name);
            assertEquals(new ScenarioResultaat.VerwachtingResultaat("attributeValue W1 uitkijkbonus", "16", "15", false, 1),
                    g.verwachtingen().get(0), branch + ": the expected side, then the actual one");
            assertEquals(r.verwachtingen(), g.verwachtingen(), branch::name);
            assertEquals(List.of(1, 8), g.verwachtingen().stream().filter(v -> !v.ok())
                    .map(ScenarioResultaat.VerwachtingResultaat::positie).toList(), branch::name);
        }
    }

    /** The beans carry the fixpoint only after the write-back: before it the entry refuses, as it refuses what
     *  {@code lees} refuses, a session not fired and one updated after its write-back. */
    @Test void a_session_without_answer_is_refused() throws Exception {
        JsonNode c = lees(KUSTWACHT.resolve("case1.json"));
        IllegalStateException notWrittenBack = refusal(IllegalStateException.class, KUSTWACHT, c, caselabels(),
                c.get("expected"), (bouw, sessie, label) -> assertNull(DroolsBackend.vuur(bouw, sessie).geenFixpunt()));
        assertEquals("vergelijk: deze sessie is niet teruggezet; de beans dragen het fixpunt pas na"
                + " DroolsBackend.zetAfgeleideWaarden, en tot dan alleen de invoer, dus zet eerst terug",
                notWrittenBack.getMessage());

        IllegalStateException notFired = refusal(IllegalStateException.class, KUSTWACHT, c, caselabels(),
                c.get("expected"), (bouw, sessie, label) -> { });
        assertTrue(notFired.getMessage().startsWith("geen fixpunt om te lezen of terug te zetten: deze sessie is niet"
                + " via DroolsBackend.vuur gevuurd"), notFired.getMessage());

        IllegalStateException updated = refusal(IllegalStateException.class, KUSTWACHT, c, caselabels(),
                c.get("expected"), (bouw, sessie, label) -> {
                    FIRED_AND_WRITTEN_BACK.doe(bouw, sessie, label);
                    Object beanObject = sessie.getObjects(o -> "W1".equals(label.apply(o))).iterator().next();
                    sessie.update(sessie.getFactHandle(beanObject), beanObject);
                });
        assertTrue(updated.getMessage().contains("gebeurde er iets buiten vuur om"), updated.getMessage());
    }

    /** A fault at the fixpoint: the rules give no answer, the write-back throws instead of writing, and the entry says
     *  so with the fault, where it would otherwise report "not written back". */
    @Test void an_evaluation_fault_at_the_fixpoint_is_named() throws Exception {
        Path division = Path.of("..", "..", "corpus", "rood", "division-by-numeric-zero");
        JsonNode c = lees(division.resolve("case1.json"));
        IllegalStateException e = refusal(IllegalStateException.class, division, c, caselabels(),
                TmsDocuments.JSON.readTree("[{\"kind\": \"consistent\"}]"),
                (bouw, sessie, label) -> assertNull(DroolsBackend.vuur(bouw, sessie).geenFixpunt()));
        assertTrue(e.getMessage().startsWith("vergelijk: aan het fixpunt van deze sessie staan 1 evaluatiefout(en), de"
                + " eerste van 'deel van een vondst': deling door nul in de regel \"deel van een vondst\""), e.getMessage());
    }

    @Test void a_label_of_two_beans_and_an_object_as_expectations_are_refused() throws Exception {
        JsonNode c = lees(KUSTWACHT.resolve("case1.json"));
        IllegalArgumentException duplicate = refusal(IllegalArgumentException.class, KUSTWACHT, c,
                labels -> o -> labels.containsValue(o) ? "dienst" : null, c.get("expected"), FIRED_AND_WRITTEN_BACK);
        assertTrue(duplicate.getMessage().startsWith("vergelijk: de labelfunctie geeft het label 'dienst' aan "),
                duplicate.getMessage());
        assertTrue(duplicate.getMessage().endsWith(", dus een verwachting op 'dienst' wijst geen bean aan"),
                duplicate.getMessage());

        IllegalArgumentException noArray = refusal(IllegalArgumentException.class, KUSTWACHT, c, caselabels(), c,
                FIRED_AND_WRITTEN_BACK);
        assertEquals("vergelijk: de verwachtingen zijn de array 'expected' van een case, en dit is een OBJECT",
                noArray.getMessage());
    }

    /** An expectation on a member of a label no bean of the session carries has nothing to read: refused, for each of the
     *  four kinds, in the sentence the timeline kind gave already. Until this entry the replay read "leeg" and "afwezig"
     *  there, so attributeEmpty and characteristicAbsent held on a label that names nothing. */
    @Test void an_unknown_label_is_refused() throws Exception {
        JsonNode c = lees(KUSTWACHT.resolve("case1.json"));
        for (String expectation : List.of(
                "{\"kind\": \"attributeValue\", \"label\": \"W9\", \"attribute\": \"uitkijkbonus\", \"value\": {\"number\": \"15\"}}",
                "{\"kind\": \"attributeEmpty\", \"label\": \"W9\", \"attribute\": \"uitkijkbonus\"}",
                "{\"kind\": \"characteristicPresent\", \"label\": \"W9\", \"characteristic\": \"rustwacht\"}",
                "{\"kind\": \"characteristicAbsent\", \"label\": \"W9\", \"characteristic\": \"rustwacht\"}")) {
            JsonNode v = TmsDocuments.JSON.readTree("[" + expectation + "]");
            IllegalArgumentException e = refusal(IllegalArgumentException.class, KUSTWACHT, c, caselabels(), v,
                    FIRED_AND_WRITTEN_BACK);
            assertEquals(v.get(0).get("kind").asText() + "-verwachting op onbekend label 'W9': geen bean van de sessie"
                    + " draagt het, dus er is niets te lezen", e.getMessage());
        }
    }

    /** What the backend does not compare, the entry refuses with the exclusion of the replay, which the report gives as
     *  a MARKER with the same sentence: a scalar read of an attribute on a timeline, and a kind uchar-case-v0 does not
     *  have. */
    @Test void what_the_entry_does_not_compare_it_refuses_like_the_report(@TempDir Path tmp) throws Exception {
        JsonNode c = lees(BORGDEEL.resolve("case1.json"));
        Map<String, String> sentences = Map.of(
                "{\"kind\": \"attributeValue\", \"label\": \"J1\", \"attribute\": \"borgdeel\", \"value\": {\"number\": \"12\"}}",
                "scalar read-back of temporal attribute 'borgdeel' is not carried by the v0 backend — the bean holds a"
                        + " List<Segment>; use an attributeTimeline expectation",
                "{\"kind\": \"attributeRange\", \"label\": \"J1\", \"attribute\": \"borgdeel\"}",
                "unknown expectation kind: attributeRange");
        for (Map.Entry<String, String> z : sentences.entrySet()) {
            JsonNode v = TmsDocuments.JSON.readTree("[" + z.getKey() + "]");
            CaseRunner.Excluded e = refusal(CaseRunner.Excluded.class, BORGDEEL, c, caselabels(), v,
                    FIRED_AND_WRITTEN_BACK);
            assertEquals(z.getValue(), e.getMessage());
            ObjectNode withExpectation = c.deepCopy();
            withExpectation.set("expected", v);
            ScenarioResultaat r = rapport(BORGDEEL, Files.writeString(tmp.resolve("case-" + z.getValue().length()
                    + ".json"), withExpectation.toString()));
            assertEquals(ScenarioResultaat.Status.MARKER, r.status());
            assertEquals(z.getValue(), r.diagnose());
        }
    }
}
