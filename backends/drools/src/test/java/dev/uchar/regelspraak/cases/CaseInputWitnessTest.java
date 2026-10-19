package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import dev.uchar.regelspraak.refusal.DroolsCode;

import dev.uchar.regelspraak.engine.ScenarioResult;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
import dev.uchar.regelspraak.transpiler.core.Namen;
import dev.uchar.regelspraak.transpiler.tms.Fixpoint;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieSession;

import java.math.BigDecimal;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The input step of a case as a consumer calls it ({@link CaseInput}): the case read onto beans, the role links set
 * after RUNTIME-SEMANTICS ch. 5.7a, the {@link Input} that {@link DroolsBackend#insert} takes, and the refusals with
 * the link they name. Every row reads its case through {@link CaseInput#of} and fires, where it fires, through
 * {@link DroolsBackend}, as a consumer with a request of its own does; no row goes through the replay's seams, so what
 * holds here holds for a consumer outside this package.
 *
 * <p>The models are corpus goldens ({@code jutterswet-ketenband}, a self-typed one-to-many relation with the to-one
 * {@code voorligger} and the to-many {@code opvolger}; {@code jutterswet-kavelgrens}, a self-typed one-to-one relation,
 * {@code noordbuur} and {@code zuidbuur}; {@code anchor/jutterswet-maatschap}, the reciprocal {@code maat};
 * {@code jutterswet-penningjaar}, an attribute on a year timeline), and the fold document of
 * {@link TmsFilterFaultWitnessTest}. The cases that collide are authored here; the corpus is not touched.
 */
class CaseInputWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    private static final String KETENBAND = "jutterswet-ketenband";
    private static final String KAVELGRENS = "jutterswet-kavelgrens";
    private static final String MAATSCHAP = "anchor/jutterswet-maatschap";

    // ---------------- ch. 5.7a: one leg, both legs ----------------

    @Test void a_to_many_relation_is_in_the_field_of_the_member_however_the_case_writes_it() throws Exception {
        // case1 writes both legs, case2 the to-many leg on W1 only, case3 the to-one legs on W2 and W3 only: one
        // relation, so one fact base. The relation has ONE field, voorligger on the member, which serves both
        // directions; nothing fills it toward W1.
        DroolsBackend.Build b = build(KETENBAND);
        for (String c : List.of("case1", "case2", "case3")) {
            CaseInput input = CaseInput.of(b, caseFile(KETENBAND, c));
            Map<String, Object> beans = input.beans();
            assertEquals(List.of("W1", "W2", "W3"), List.copyOf(beans.keySet()), c + ": the labels in case order");
            assertSame(beans.get("W1"), field(beans.get("W2"), "voorligger"), c);
            assertSame(beans.get("W1"), field(beans.get("W3"), "voorligger"), c);
            assertNull(field(beans.get("W1"), "voorligger"), c + ": no link toward W1 is invented");
            for (String label : beans.keySet()) assertEquals("Wachter", input.types().get(label).name, c);
        }
    }

    @Test void a_one_to_one_relation_out_of_a_leg_fills_both_fields() throws Exception {
        // case2 writes zuidbuur only (K1 -> K2, K2 -> K3). A one-to-one relation owns a field per direction, so the
        // entry writes the far field too: K2's noordbuur is K1, K3's is K2. case1 writes both legs: the same beans.
        DroolsBackend.Build b = build(KAVELGRENS);
        for (String c : List.of("case1", "case2")) {
            Map<String, Object> k = CaseInput.of(b, caseFile(KAVELGRENS, c)).beans();
            assertSame(k.get("K2"), field(k.get("K1"), "zuidbuur"), c);
            assertSame(k.get("K3"), field(k.get("K2"), "zuidbuur"), c);
            assertSame(k.get("K1"), field(k.get("K2"), "noordbuur"), c + ": the far field of K1's link");
            assertSame(k.get("K2"), field(k.get("K3"), "noordbuur"), c + ": the far field of K2's link");
            assertNull(field(k.get("K1"), "noordbuur"), c);
            assertNull(field(k.get("K3"), "zuidbuur"), c);
        }
    }

    @Test void a_reciprocal_role_out_of_a_leg_applies_on_both_sides_and_two_legs_are_a_relation() throws Exception {
        DroolsBackend.Build b = build(MAATSCHAP);
        Map<String, Object> one = CaseInput.of(b, caseFile(MAATSCHAP, "case1")).beans();
        assertSame(one.get("J2"), field(one.get("J1"), "maat"), "the leg the case writes");
        assertSame(one.get("J1"), field(one.get("J2"), "maat"), "the reverse leg of the separate pass");
        assertNull(field(one.get("J3"), "maat"), "the pass writes no more than the relation");
        Map<String, Object> two = CaseInput.of(b, maatschap(List.of("J1", "J2", "J3"),
                Map.of("J1", List.of("J2"), "J2", List.of("J1")))).beans();
        assertSame(two.get("J2"), field(two.get("J1"), "maat"));
        assertSame(two.get("J1"), field(two.get("J2"), "maat"));
    }

    // ---------------- WF-013: a second, different target on a to-one field, at each of the four places ----------------

    @Test void a_second_target_on_the_own_to_one_role_names_that_link() throws Exception {
        CaseInput.RoleError f = roleError(() -> CaseInput.of(build(MAATSCHAP), maatschap(List.of("J1", "J2", "J3"),
                Map.of("J1", List.of("J2", "J3")))));
        collision(f, "J1", "maat", "J3");
    }

    @Test void a_second_target_on_the_far_field_of_a_one_to_one_relation_names_that_link() throws Exception {
        // K1 and K3 both name K2 as their zuidbuur: the forward fields are two, the far field, K2's noordbuur, is one.
        CaseInput.RoleError f = roleError(() -> CaseInput.of(build(KAVELGRENS), chain("Duinkavel", "kavelbreedte",
                List.of("K1", "K2", "K3"), Map.of("K1", Map.of("zuidbuur", List.of("K2")),
                        "K3", Map.of("zuidbuur", List.of("K2"))))));
        collision(f, "K3", "zuidbuur", "K2");
    }

    @Test void a_second_target_on_the_field_of_a_to_many_role_names_that_link() throws Exception {
        // W2 names W3 as its voorligger first; then W1 writes W2 among its opvolgers, which is stored in that same
        // field of W2. The forward write of W2 came first, so the link that collides is W1's.
        CaseInput.RoleError f = roleError(() -> CaseInput.of(build(KETENBAND), chain("Wachter", "rangnummer",
                List.of("W2", "W3", "W1"), Map.of("W2", Map.of("voorligger", List.of("W3")),
                        "W1", Map.of("opvolgers", List.of("W2"))))));
        collision(f, "W1", "opvolgers", "W2");
    }

    @Test void a_colliding_reversed_leg_names_the_link_whose_step_mirrored_the_leg() throws Exception {
        // J1's maat is J2 and J2's maat is J3: the reverse leg of J1's link would make J2's maat J1. In either order
        // of the instances the pass meets the collision at J1's leg, and names the link J1 wrote.
        for (List<String> order : List.of(List.of("J1", "J2", "J3"), List.of("J2", "J3", "J1"))) {
            CaseInput.RoleError f = roleError(() -> CaseInput.of(build(MAATSCHAP), maatschap(order,
                    Map.of("J1", List.of("J2"), "J2", List.of("J3")))));
            collision(f, "J1", "maat", "J2");
            assertTrue(((RuntimeException) f).getMessage().contains("de omgekeerde poot botst"), order.toString());
        }
    }

    // ---------------- a link the bean model cannot hold ----------------

    @Test void an_unknown_target_label_is_refused_with_the_link() throws Exception {
        CaseInput.RoleError f = roleError(() -> CaseInput.of(build(KETENBAND), chain("Wachter", "rangnummer",
                List.of("W1", "W2"), Map.of("W1", Map.of("opvolgers", List.of("W2", "W9"))))));
        refusal(f, "W1", "opvolgers", "W9", "role 'opvolgers' names unknown label W9");
    }

    @Test void a_role_without_to_a_reference_field_is_refused_with_the_link() throws Exception {
        // a key that names no role of the self-typed pair: neither by name nor by the pair, which carries two roles
        CaseInput.RoleError f = roleError(() -> CaseInput.of(build(KETENBAND), chain("Wachter", "rangnummer",
                List.of("W1", "W2"), Map.of("W1", Map.of("buur", List.of("W2"))))));
        refusal(f, "W1", "buur", "W2", "role 'buur' (Wachter -> Wachter) reaches no to-one reference field: the"
                + " written role name resolves to no unique role, and neither direction of the type pair derives a"
                + " to-one role");
    }

    @Test void a_displaced_reference_field_is_refused_with_the_link() throws Exception {
        // ketenband without its rules and with an attribute voorligger on Wachter: the attribute takes the bean
        // property of the role (attribute > characteristic > reference), so the field to write is gone
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(ir(KETENBAND)));
        ((ArrayNode) ir.get("rules").get("rules")).removeAll();
        ((ArrayNode) IrPath.objectType(ir, "Wachter").get("attributes")).addObject().put("node", "Attribute")
                .put("name", "voorligger").set("datatype", JSON.readTree(
                        "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"nonNegative\"}"));
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        CaseInput.RoleError f = roleError(() -> CaseInput.of(b, chain("Wachter", "rangnummer", List.of("W1", "W2"),
                Map.of("W2", Map.of("voorligger", List.of("W1"))))));
        refusal(f, "W2", "voorligger", "W1", "role 'voorligger' resolves to the to-one role 'voorligger' on Wachter,"
                + " which shares its name with a declared member — the bean cannot carry the reference field"
                + " (name-sharing bean limit: attribute > characteristic > reference)");
    }

    // ---------------- the labels of a request ----------------

    @Test void with_the_ids_of_a_request_as_labels_gives_the_entry_what_the_report_gives() throws Exception {
        // A service writes its request as the instances of a case, with its ids as labels. ketenband case2 so
        // relabelled reads, fires and compares through the entries alone as the report reads the case itself, the
        // labels in the expectations aside.
        Map<String, String> ids = Map.of("W1", "wachter/1", "W2", "wachter/2", "W3", "wachter/3");
        Path path = CORPUS.resolve(KETENBAND).resolve("case2.json");
        ScenarioResult report = CaseRunner.report(ir(KETENBAND), List.of(path)).get(0);
        assertEquals(ScenarioResult.Status.PASS, report.status());
        ScenarioResult entry = play(build(KETENBAND), "case2.json", rename(JSON.readTree(Files.readString(path)), ids));
        Map<String, String> backLabel = new LinkedHashMap<>();
        ids.forEach((label, id) -> backLabel.put(id, label));
        List<ScenarioResult.ExpectationResult> expectations = new ArrayList<>();
        for (ScenarioResult.ExpectationResult v : entry.expectations()) {
            String source = v.source();
            for (Map.Entry<String, String> e : backLabel.entrySet()) source = source.replace(e.getKey(), e.getValue());
            assertTrue(!source.equals(v.source()) || v.source().equals("consistent"), "the expectation names an id: " + v);
            expectations.add(new ScenarioResult.ExpectationResult(source, v.expected(), v.actual(), v.ok(),
                    v.position()));
        }
        List<ScenarioResult.Firing> firings = new ArrayList<>();
        for (ScenarioResult.Firing v : entry.trace().firings())
            firings.add(new ScenarioResult.Firing(v.rule(), v.labels().stream().map(backLabel::get).toList(),
                    v.count()));
        assertEquals(new ScenarioResult(report.name(), report.status(), expectations,
                new ScenarioResult.Trace(firings), entry.coverage(), entry.diagnostic()), report);
        // and a collision names the ids
        CaseInput.RoleError f = roleError(() -> CaseInput.of(build(KAVELGRENS), chain("Duinkavel", "kavelbreedte",
                List.of("kavel-1", "kavel-2", "kavel-3"), Map.of("kavel-1", Map.of("zuidbuur", List.of("kavel-2")),
                        "kavel-3", Map.of("zuidbuur", List.of("kavel-2"))))));
        collision(f, "kavel-3", "zuidbuur", "kavel-2");
    }

    // ---------------- the Invoer ----------------

    @Test void a_list_gives_the_written_members_in_write_order_and_then_the_mirrored_in_byte_order()
            throws Exception {
        // W1 writes opvolgers W9 then W3; W3, W10 and W2 each name W1 as their voorligger. The list under W1: the
        // written members in the order written, then the mirrored ones by label in byte order ("W10" < "W2"), and
        // W3, in both, once, where it was written.
        DroolsBackend.Build b = build(KETENBAND);
        CaseInput input = CaseInput.of(b, chain("Wachter", "rangnummer", List.of("W1", "W9", "W3", "W2", "W10"),
                Map.of("W1", Map.of("opvolgers", List.of("W9", "W3")), "W3", Map.of("voorligger", List.of("W1")),
                        "W10", Map.of("voorligger", List.of("W1")), "W2", Map.of("voorligger", List.of("W1")))));
        Map<String, Object> w = input.beans();
        List<Input.ToManyList> lists = input.inputData().lists();
        assertEquals(1, lists.size(), () -> "one list: " + lists);
        assertSame(w.get("W1"), lists.get(0).holder());
        assertEquals("voorligger", lists.get(0).role());
        assertEquals(List.of("W9", "W3", "W10", "W2"), labels(w, lists.get(0).members()));
        // insert takes it, and the session counts the four
        Fixpoint fixpoint = fire(b, input, Map.of());
        assertEquals(0, new BigDecimal("4").compareTo((BigDecimal) value(fixpoint, w.get("W1"), "opvolgeraantal")));
    }

    @Test void the_order_of_the_input_decides_which_member_a_fold_fault_names() throws Exception {
        // The fold of TmsFilterFaultWitnessTest stops at the first member in fold order whose filter faults
        // (RUNTIME-SEMANTICS ch. 5.11): the root of a gewicht that is no square (until the card zero-division-channel a
        // division by 0, which deposits since). V9 takes the root of 2, V10 of 3. Written under J1 as V9, V10: 2.
        // Mirrored from their own links: V10 before V9 in byte order, so 3, where the order of the case would give 2.
        DroolsBackend.Build b = DroolsBackend.build(TmsFilterFaultWitnessTest.document(TmsFilterFaultWitnessTest.ZWARE),
                DroolsBackend.Branch.EXEC_MODEL);
        String v9 = TmsDocuments.instanceWithRoles("V9", "Vondst", Map.of("gewicht", "2"), List.of(), Map.of());
        String v10 = TmsDocuments.instanceWithRoles("V10", "Vondst", Map.of("gewicht", "3"), List.of(), Map.of());
        JsonNode written = TmsDocuments.caseDocument(List.of(TmsDocuments.instanceWithRoles("J1", "Jutter", Map.of(),
                List.of("actief"), Map.of("vondsten", List.of("V9", "V10"))), v9, v10), List.of());
        assertEquals(List.of("square root of 2"), rootFaults(b, written));
        JsonNode mirrored = TmsDocuments.caseDocument(List.of(TmsDocuments.instanceWithRoles("J1", "Jutter", Map.of(),
                List.of("actief"), Map.of()),
                TmsDocuments.instanceWithRoles("V9", "Vondst", Map.of("gewicht", "2"), List.of(),
                        Map.of("aanmelder", List.of("J1"))),
                TmsDocuments.instanceWithRoles("V10", "Vondst", Map.of("gewicht", "3"), List.of(),
                        Map.of("aanmelder", List.of("J1")))), List.of());
        assertEquals(List.of("square root of 3"), rootFaults(b, mirrored));
    }

    @Test void a_scalar_on_a_timeline_is_a_bucket_scalar_and_a_timeline_is_not() throws Exception {
        DroolsBackend.Build b = build("jutterswet-penningjaar");
        CaseInput scalar = CaseInput.of(b, penningjaar("{ \"number\": \"14\" }"));
        Object j1 = scalar.beans().get("J1");
        List<Input.Bucket> emmers = scalar.inputData().emmers();
        assertEquals(1, emmers.size(), () -> "one bucket scalar: " + emmers);
        assertSame(j1, emmers.get(0).bean());
        assertEquals("aantal aangemelde vondsten", emmers.get(0).attribute());
        List<?> tl = (List<?>) RegelSpraakRuntime.get(j1, Namen.ident("aantal aangemelde vondsten", false));
        assertEquals(1, tl.size(), "the bean holds the one segment of the asOfDate bucket");
        assertEquals(LocalDate.of(2026, 1, 1), RegelSpraakRuntime.get(tl.get(0), "van"));
        assertEquals(LocalDate.of(2027, 1, 1), RegelSpraakRuntime.get(tl.get(0), "tot"));
        CaseInput timeline = CaseInput.of(b, penningjaar(
                "{ \"timeline\": [ { \"from\": \"2026-01-01\", \"to\": \"2027-01-01\", \"value\": { \"number\": \"14\" } } ] }"));
        assertEquals(List.of(), timeline.inputData().emmers(), "a full timeline is no bucket scalar");
    }

    @Test void input_is_at_each_call_fresh_and_beans_is_not_to_be_changed() throws Exception {
        CaseInput input = CaseInput.of(build(KETENBAND), caseFile(KETENBAND, "case2"));
        Input one = input.inputData(), two = input.inputData();
        assertNotSame(one, two);
        one.bean(new Object());
        assertEquals(List.copyOf(input.beans().values()), two.beans(), "the second Invoer holds the beans alone");
        assertEquals(List.copyOf(input.beans().values()), input.inputData().beans());
        assertThrows(UnsupportedOperationException.class, () -> input.beans().put("W4", new Object()));
        assertThrows(UnsupportedOperationException.class, () -> input.types().clear());
    }

    @Test void from_the_factory_the_context_class_loader_comes_back_also_after_a_refusal() throws Exception {
        DroolsBackend.Build b = build(MAATSCHAP);
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader own = new URLClassLoader(new URL[0], previous)) {
            Thread.currentThread().setContextClassLoader(own);
            CaseInput.of(b, caseFile(MAATSCHAP, "case1"));
            assertSame(own, Thread.currentThread().getContextClassLoader());
            assertThrows(CaseInput.RoleCollision.class, () -> CaseInput.of(b, maatschap(List.of("J1", "J2", "J3"),
                    Map.of("J1", List.of("J2", "J3")))));
            assertSame(own, Thread.currentThread().getContextClassLoader());
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    @Test void a_case_without_instances_is_refused() throws Exception {
        DroolsBackend.Build b = build(MAATSCHAP);
        for (String c : List.of("{ \"asOfDate\": null }", "{ \"input\": { \"parameters\": {} } }",
                "{ \"input\": { \"instances\": {} } }")) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> CaseInput.of(b, JSON.readTree(c)));
            assertTrue(e.getMessage().startsWith("CaseInput.of: de case draagt geen array input.instances"),
                    e.getMessage());
        }
    }

    // ---------------- the whole corpus, through the entries alone ----------------

    @Test void every_case_of_the_corpus_plays_via_the_entries_as_in_the_report() throws Exception {
        // Every golden with case files, read with CaseInput.of and played with insert, vuur, setDerivedValues and
        // vergelijk as a consumer plays it, gives per case the ScenarioResult of the report: verdict, expectations,
        // firings, coverage, and for a form not carried the MARKER with the replay's reason.
        int goldens = 0, cases = 0, played = 0;
        List<Path> irs;
        try (Stream<Path> s = Files.walk(CORPUS)) {
            irs = s.filter(p -> p.getFileName().toString().equals("ir.json")).sorted().toList();
        }
        for (Path ir : irs) {
            List<Path> paths;
            try (Stream<Path> s = Files.list(ir.getParent())) {
                paths = s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList();
            }
            if (paths.isEmpty()) continue;
            goldens++;
            List<ScenarioResult> report = CaseRunner.report(ir, paths);
            List<ScenarioResult> entry = viaTheEntries(ir, paths);
            assertEquals(report, entry, ir.toString());
            cases += entry.size();
            played += (int) entry.stream().filter(r -> r.status() == ScenarioResult.Status.PASS).count();
        }
        // the counts of the report on the corpus of this card's start (300 goldens, 823 cases, 695 PASS), as floors
        assertTrue(goldens >= 300 && cases >= 823 && played >= 695,
                "goldens " + goldens + ", cases " + cases + ", gespeeld " + played);
    }

    // ---------------- fixtures ----------------

    private static Path ir(String golden) {
        Path p = CORPUS.resolve(golden).resolve("ir.json");
        assertTrue(Files.exists(p), "golden ir.json present: " + p.toAbsolutePath());
        return p;
    }

    private static DroolsBackend.Build build(String golden) throws Exception {
        return DroolsBackend.build(ir(golden), DroolsBackend.Branch.EXEC_MODEL);
    }

    private static JsonNode caseFile(String golden, String name) throws Exception {
        return JSON.readTree(Files.readString(CORPUS.resolve(golden).resolve(name + ".json")));
    }

    /** A reference field of a bean, read by its property name. */
    private static Object field(Object bean, String role) {
        return RegelSpraakRuntime.get(bean, role);
    }

    /** A case of {@code type} instances in the order of {@code labels}, each with the whole number {@code attribute} 1,
     *  and the roles {@code roles} by label. */
    private static JsonNode chain(String type, String attribute, List<String> labels,
                                  Map<String, Map<String, List<String>>> roles) throws Exception {
        List<String> instances = new ArrayList<>();
        for (String label : labels)
            instances.add(TmsDocuments.instanceWithRoles(label, type, Map.of(attribute, "1"), List.of(),
                    new LinkedHashMap<>(roles.getOrDefault(label, Map.of()))));
        return TmsDocuments.caseDocument(instances, List.of());
    }

    /** Jutters of the maatschap golden, in the order of {@code labels}, with the maat {@code maten} by label. */
    private static JsonNode maatschap(List<String> labels, Map<String, List<String>> maten) throws Exception {
        List<String> instances = new ArrayList<>();
        for (String label : labels)
            instances.add(TmsDocuments.instanceWithRoles(label, "Jutter", Map.of("vondstwaarde", "1"), List.of(),
                    maten.containsKey(label) ? Map.of("maat", maten.get(label)) : Map.of()));
        return TmsDocuments.caseDocument(instances, List.of());
    }

    /** J1 of the penningjaar golden on asOfDate 2026-03-15, with {@code value} as its aantal aangemelde vondsten. */
    private static JsonNode penningjaar(String value) throws Exception {
        return JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": "2026-03-15",
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": { "aantal aangemelde vondsten": %s },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} } }
                """.formatted(value));
    }

    /** A read of a case that may throw. */
    @FunctionalInterface
    private interface Read {
        CaseInput readOf() throws Exception;
    }

    /** The refusal of a role link that {@code read} throws, caught as a consumer catches both kinds: in one clause,
     *  whose parameter is a {@link CaseInput.RoleError}. */
    private static CaseInput.RoleError roleError(Read read) throws Exception {
        try {
            read.readOf();
        } catch (CaseInput.RoleCollision | CaseInput.RoleRefusal e) {
            return e;
        }
        return fail("no refusal of a role link");
    }

    private static void collision(CaseInput.RoleError f, String label, String role, String target) {
        CaseInput.RoleCollision b = assertInstanceOf(CaseInput.RoleCollision.class, f);
        assertInstanceOf(IllegalArgumentException.class, b, "a defect of the case");
        assertEquals(List.of(label, role, target), List.of(f.label(), f.role(), f.target()), b.getMessage());
        assertTrue(b.getMessage().contains("WF-013"), b.getMessage());
    }

    private static void refusal(CaseInput.RoleError f, String label, String role, String target, String sentence) {
        CaseInput.RoleRefusal w = assertInstanceOf(CaseInput.RoleRefusal.class, f);
        assertInstanceOf(CaseRunner.Excluded.class, w, "a form this backend does not carry");
        assertEquals(List.of(label, role, target), List.of(f.label(), f.role(), f.target()), w.getMessage());
        assertEquals(sentence, w.getMessage());
    }

    private static List<String> labels(Map<String, Object> beans, List<Object> members) {
        Map<Object, String> label = new IdentityHashMap<>();
        beans.forEach((l, b) -> label.put(b, l));
        return members.stream().map(label::get).toList();
    }

    private static Object value(Fixpoint fixpoint, Object object, String attribute) {
        return fixpoint.values().stream().filter(w -> w.object() == object && w.attribute().equals(attribute))
                .map(Fixpoint.DerivedValue::value).findFirst().orElse(null);
    }

    /** The fixpoint of {@code input} on {@code b}, read without a write-back, with {@code globals} set. */
    private static Fixpoint fire(DroolsBackend.Build b, CaseInput input, Map<String, Object> globals) {
        Map<Object, String> label = new IdentityHashMap<>();
        input.beans().forEach((l, bean) -> label.put(bean, l));
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        KieSession session = b.kieBase().newKieSession();
        try {
            globals.forEach((name, w) -> setGlobal(session, name, w));
            DroolsBackend.insert(b, session, input.inputData());
            DroolsBackend.Firing v = DroolsBackend.fire(b, session);
            assertNull(v.noFixpoint());
            return DroolsBackend.fixpointOf(b, session, label::get);
        } finally {
            session.dispose();
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    /** The dividends of the divisions by zero that stand at the fixpoint of case {@code c}, as "n gedeeld door 0". */
    private static List<String> rootFaults(DroolsBackend.Build b, JsonNode c) {
        Fixpoint f = fire(b, CaseInput.of(b, c), Map.of());
        List<String> out = new ArrayList<>();
        for (var fault : f.faults()) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("square root of (\\d+)")
                    .matcher(fault.fault().getMessage());
            assertTrue(m.find(), fault.fault().getMessage());
            out.add(m.group());
        }
        return out;
    }

    private static void setGlobal(KieSession session, String name, Object value) {
        try {
            session.setGlobal(name, value);
        } catch (RuntimeException notDeclared) {
            // a global the KieBase does not declare is one no rule reads
        }
    }

    /** Case {@code c} with every label renamed after {@code ids}: the instances, their role targets, the expectations. */
    private static JsonNode rename(JsonNode c, Map<String, String> ids) {
        ObjectNode out = c.deepCopy();
        for (JsonNode i : out.get("input").get("instances")) {
            ((ObjectNode) i).put("label", ids.get(i.get("label").asText()));
            for (JsonNode targets : i.get("roles"))
                for (int k = 0; k < targets.size(); k++)
                    ((ArrayNode) targets).set(k, TextNode.valueOf(ids.get(targets.get(k).asText())));
        }
        for (JsonNode v : out.get("expected"))
            if (v.hasNonNull("label")) ((ObjectNode) v).put("label", ids.get(v.get("label").asText()));
        return out;
    }

    /** Every case of {@code paths} that is no red-path case, as {@link CaseRunner#report} reports it, played through
     *  the public entries alone. */
    private static List<ScenarioResult> viaTheEntries(Path ir, List<Path> paths) throws Exception {
        DroolsBackend.Build b = null;
        String refusal = null;
        try {
            b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        } catch (CaseRunner.Excluded e) {
            refusal = e.getMessage();
        } catch (IrLoader.Refusal e) {
            refusal = "loader: " + e.getMessage();
        }
        List<ScenarioResult> out = new ArrayList<>();
        for (Path p : paths) {
            JsonNode c = JSON.readTree(Files.readString(p));
            if (c.has("expectedFault")) continue;
            String name = p.getFileName().toString();
            if (b == null) {
                out.add(marker(name, refusal));
                continue;
            }
            try {
                out.add(play(b, name, c));
            } catch (CaseRunner.Excluded e) {
                out.add(marker(name, e.getMessage()));
            }
        }
        return out;
    }

    private static ScenarioResult marker(String name, String reason) {
        return new ScenarioResult(name, ScenarioResult.Status.MARKER, List.of(),
                new ScenarioResult.Trace(List.of()), List.of(), reason);
    }

    /** Case {@code c} as a consumer plays it: read through {@link CaseInput}, its parameters and asOfDate as globals,
     *  put in, fired with a waarnemer, written back and compared. */
    private static ScenarioResult play(DroolsBackend.Build b, String name, JsonNode c) {
        CaseInput input = CaseInput.of(b, c);
        Map<Object, String> label = new IdentityHashMap<>();
        input.beans().forEach((l, bean) -> label.put(bean, l));
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        KieSession session = b.kieBase().newKieSession();
        try {
            JsonNode params = c.get("input").get("parameters");
            if (params != null) for (Map.Entry<String, JsonNode> e : params.properties())
                setGlobal(session, Namen.ident(e.getKey(), false), parameterValue(e.getKey(), e.getValue()));
            if (c.hasNonNull("asOfDate")) setGlobal(session, "rekendatum", LocalDate.parse(c.get("asOfDate").asText()));
            DroolsBackend.insert(b, session, input.inputData());
            DroolsBackend.Observer observer = new DroolsBackend.Observer(label::get);
            DroolsBackend.Firing firing = DroolsBackend.fire(b, session, observer);
            ScenarioResult.Trace trace = new ScenarioResult.Trace(observer.firings());
            List<ScenarioResult.CoverageRule> coverage = DroolsBackend.coverage(b, observer.forms());
            if (firing.noFixpoint() != null)
                return new ScenarioResult(name, ScenarioResult.Status.FAIL, List.of(), trace, coverage,
                        name + ": " + firing.noFixpoint());
            DroolsBackend.setDerivedValues(b, session, label::get);
            DroolsBackend.Comparison v = DroolsBackend.compare(b, session, label::get, c.get("expected"));
            return new ScenarioResult(name, v.pass() ? ScenarioResult.Status.PASS : ScenarioResult.Status.FAIL,
                    v.expectations(), trace, coverage, v.pass() ? "" : name + ": " + v.diagnostic());
        } finally {
            session.dispose();
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    /** A parameter value as a consumer gives it: the forms the corpus carries, and the replay's refusal of a timeline
     *  and of a rational number. */
    private static Object parameterValue(String name, JsonNode w) {
        if (w.has("timeline"))
            throw new CaseRunner.Excluded(DroolsCode.GRANULAR_PARAMETER_TIMELINE, "granular (timeline) parameter '" + name + "' is not carried by the v0 backend");
        if (w.has("number")) {
            if (w.get("number").asText().contains("/"))
                throw new CaseRunner.Excluded(DroolsCode.RATIONAL_VALUE, "rational value on '" + name + "' is not carried by the v0 backend");
            return new BigDecimal(w.get("number").asText());
        }
        if (w.has("date")) return LocalDate.parse(w.get("date").asText());
        if (w.has("boolean")) return w.get("boolean").asBoolean();
        if (w.has("text")) return w.get("text").asText();
        if (w.has("enumeration")) return w.get("enumeration").asText();
        return fail("a parameter form this witness does not set: " + name + " " + w);
    }
}
