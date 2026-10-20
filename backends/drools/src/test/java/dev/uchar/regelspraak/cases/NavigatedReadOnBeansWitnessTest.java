package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.engine.BeanCompiler;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.ResolvedRule;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;
import dev.uchar.regelspraak.model.RegelSpraakModel.RoleBindingRef;
import dev.uchar.regelspraak.refusal.DroolsCode;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.core.Namen;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieSession;

import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A consumer of the written-back beans reads a read over a role path as the exec-model branch reads it: the walk of a
 * read of the model of a build ({@code TmsTranspiler.navigationBindings}) and the object that walk reaches from a bean
 * ({@link DroolsBackend#reached}). An explanation of an outcome needs both: it names the value an operand had, and an
 * operand like "de afgeleide openingsdag van de haven van de jutter van de Vondst" stands on another object than the
 * subject. The model a loader reads carries the walk beside the read, not on it, so without the first a consumer reads
 * such an operand as the subject's own member; without the second it derives the navigation a second time.
 *
 * <p>Golden {@code jutterswet-havenketen}: a Vondst reads over its jutter one hop, and over the haven of its jutter two
 * hops, a derived and an input member; in case 1 the jutter J3 of V4 has no haven, and in case 2 the haven of J1 is a
 * link the fact creation {@code werving} lays while the rules run. Every case is played through the public entry, as
 * a consumer with a request of its own plays it. The refusals use the walks of that golden from a type they do not
 * start on and over a to-many role, and the displaced field of {@code synth-navigatiepremie}, made as
 * {@link StorageDirectionWitnessTest} makes it. The corpus is read and never written.
 */
class NavigatedReadOnBeansWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    private static final String HAVENKETEN = "jutterswet-havenketen";
    private static final String NAVIGATION_PREMIE = "synth-navigatiepremie";

    private static final String DERIVED_TWO_HOPS = "de afgeleide openingsdag van de haven van de jutter van de Vondst";
    private static final String INPUT_TWO_HOPS = "de openingsdag van de haven van de jutter van de Vondst";
    private static final String ONE_HOP = "de vergunningsdag van de jutter van de Vondst";
    private static final String OWN = "zijn vinddag";

    @Test
    void the_walk_of_a_read_of_the_model_of_a_build_is_the_one_the_loader_read_beside_it() throws Exception {
        DroolsBackend.Build b = build(HAVENKETEN);
        List<RoleBindingRef> twoHops = List.of(new RoleBindingRef("JutterHeeftVondst", "jutter"),
                new RoleBindingRef("HavenHeeftJutter", "haven"));
        // the derived member is read twice, in the condition of havendatum and the value of havenopening
        assertEquals(2, reads(b, DERIVED_TWO_HOPS).size());
        for (Expr.AttributeReference read : reads(b, DERIVED_TWO_HOPS)) assertEquals(twoHops, walk(b, read));
        assertEquals(twoHops, walk(b, only(reads(b, INPUT_TWO_HOPS))));
        assertEquals(List.of(new RoleBindingRef("JutterHeeftVondst", "jutter")), walk(b, only(reads(b, ONE_HOP))));
        // the read carries none of it itself: the loader leaves the written navigation empty (IR-SPEC ch. 4.1a)
        Expr.AttributeReference oneHop = only(reads(b, ONE_HOP));
        assertNull(oneHop.navigationRole());
        assertTrue(oneHop.navigationPath().isEmpty());
        for (Expr.AttributeReference read : reads(b, OWN)) assertEquals(List.of(), walk(b, read));
    }

    @Test
    void a_walk_reaches_the_object_the_branch_read_and_nothing_where_a_link_is_absent() throws Exception {
        DroolsBackend.Build b = build(HAVENKETEN);
        Map<String, Object> o = playAndWriteBack(b, "case1-vier-vondsten");
        ObjectType vondst = b.model().objectTypeByName("Vondst");
        for (Expr.AttributeReference read : reads(b, DERIVED_TWO_HOPS)) {
            assertSame(o.get("H1"), reached(b, o.get("V1"), vondst, read), "V1 over J1");
            assertSame(o.get("H1"), reached(b, o.get("V2"), vondst, read), "V2 over J1");
            assertSame(o.get("H2"), reached(b, o.get("V3"), vondst, read), "V3 over J2");
            assertNull(reached(b, o.get("V4"), vondst, read), "J3, the jutter of V4, has no haven");
        }
        assertSame(o.get("H1"), reached(b, o.get("V1"), vondst, only(reads(b, INPUT_TWO_HOPS))));
        assertNull(reached(b, o.get("V4"), vondst, only(reads(b, INPUT_TWO_HOPS))));
        assertSame(o.get("J3"), reached(b, o.get("V4"), vondst, only(reads(b, ONE_HOP))), "one hop reaches J3");
        for (Expr.AttributeReference read : reads(b, OWN))
            assertSame(o.get("V1"), reached(b, o.get("V1"), vondst, read), "an own read stays on the subject");
        // on the object it reaches stands the value the engine read: havenopening is the afgeleide openingsdag of the
        // haven of the jutter, written back on both beans
        Object h1 = reached(b, o.get("V1"), vondst, only(havenopeningValue(b)));
        assertEquals(LocalDate.of(2026, 1, 1), member(h1, "afgeleide openingsdag"));
        assertEquals(member(h1, "afgeleide openingsdag"), member(o.get("V1"), "havenopening"));
        assertNull(member(o.get("H2"), "afgeleide openingsdag"));
        assertNull(member(o.get("V3"), "havenopening"));
    }

    @Test
    void a_link_a_creation_laid_is_reached_over_the_field_after_the_write_back() throws Exception {
        // no input links J1 to a haven: werving makes J1, a geworven jutter of K1, a havenjutter of H1, whose kustwacht
        // K1 is, while the rules run; the write-back puts that link on J1's field haven
        DroolsBackend.Build b = build(HAVENKETEN);
        Map<String, Object> o = playAndWriteBack(b, "case2-haven-uit-de-werving");
        ObjectType vondst = b.model().objectTypeByName("Vondst");
        for (Expr.AttributeReference read : reads(b, DERIVED_TWO_HOPS)) {
            assertSame(o.get("H1"), reached(b, o.get("V1"), vondst, read), "V1 over J1, linked by werving");
            assertNull(reached(b, o.get("V2"), vondst, read), "K2, the werver of J2, guards no haven");
        }
        assertEquals(member(o.get("H1"), "afgeleide openingsdag"), member(o.get("V1"), "havenopening"));
    }

    @Test
    void a_walk_that_does_not_start_on_the_subject_type_is_refused_before_anything_is_read() throws Exception {
        DroolsBackend.Build b = build(HAVENKETEN);
        Map<String, Object> o = playAndWriteBack(b, "case1-vier-vondsten");
        Expr.AttributeReference read = only(reads(b, INPUT_TWO_HOPS));
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> DroolsBackend.reached(b.model(),
                o.get("H1"), b.model().objectTypeByName("Haven"), read, walk(b, read)));
        assertEquals(DroolsCode.NAVIGATION_DOES_NOT_RESOLVE, e.code());
        assertEquals("de lezing '" + INPUT_TWO_HOPS + "': de navigatie resolvet niet vanaf Haven", e.getMessage());
        // from the subject type, but its last hop reaches a Haven where the read reads a member of a Jutter
        Expr.AttributeReference oneHop = only(reads(b, ONE_HOP));
        CaseRunner.Excluded f = assertThrows(CaseRunner.Excluded.class, () -> DroolsBackend.reached(b.model(),
                o.get("V1"), b.model().objectTypeByName("Vondst"), oneHop, walk(b, read)));
        assertEquals(DroolsCode.NAVIGATION_DOES_NOT_RESOLVE, f.code());
        assertEquals("de lezing '" + ONE_HOP + "': de navigatie resolvet niet vanaf Vondst", f.getMessage());
    }

    @Test
    void a_walk_over_a_to_many_role_is_refused_before_anything_is_read() throws Exception {
        // from a Haven over havenjutter, a role of which a Haven has several
        DroolsBackend.Build b = build(HAVENKETEN);
        Map<String, Object> o = playAndWriteBack(b, "case1-vier-vondsten");
        Expr.AttributeReference read = only(reads(b, ONE_HOP));
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> DroolsBackend.reached(b.model(),
                o.get("H1"), b.model().objectTypeByName("Haven"), read,
                List.of(new RoleBindingRef("HavenHeeftJutter", "havenjutter"))));
        assertEquals(DroolsCode.NAVIGATION_TO_MANY_ROLE, e.code());
        assertEquals("de lezing '" + ONE_HOP + "': de navigatie gaat over een rol naar veel", e.getMessage());
    }

    @Test
    void a_hop_without_a_reference_field_is_refused_before_anything_is_read() throws Exception {
        // a Numeriek member werf on Schuit displaces the field of the role werf (§13.3.9); the build refuses the rule
        // that reads over it, so the model is read by the loader and its beans are compiled alone
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(ir(NAVIGATION_PREMIE)));
        IrPath.addAttribute(IrPath.objectType(ir, "Schuit"), "werf", JSON.createObjectNode().put("node", "Number")
                .put("whole", true).put("decimals", 0).put("sign", "unspecified"));
        IrLoader.Loaded loaded = IrLoader.loadAll(ir);
        RegelSpraakModel model = loaded.model();
        ClassLoader beans = BeanCompiler.compile(FactModelGenerator.generate(model));
        Object schuit = beans.loadClass(FactModelGenerator.PACKAGE + ".Schuit").getDeclaredConstructor().newInstance();
        RegelSpraakRuntime.set(schuit, "werf", new java.math.BigDecimal(5));
        List<Expr.AttributeReference> reads = new ArrayList<>();
        for (ResolvedRule r : model.resolvedRules) collect(r, reads);
        Expr.AttributeReference read = reads.stream()
                .filter(a -> "het tarief van de werf van de Schuit".equals(a.source().snippet())).findFirst().orElseThrow();
        List<RoleBindingRef> walk = loaded.navigationBindings().get(read);
        assertEquals(List.of(new RoleBindingRef("ligplaats", "werf")), walk);
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> DroolsBackend.reached(model, schuit,
                model.objectTypeByName("Schuit"), read, walk));
        assertEquals(DroolsCode.NAVIGATION_ROLE_NO_FIELD, e.code());
        assertEquals("de lezing 'het tarief van de werf van de Schuit': Schuit heeft geen referentieveld 'werf' naar"
                + " Werf (§13.3.9 opslagrichting)", e.getMessage());
    }

    // ---------------- helpers ----------------

    private static Object reached(DroolsBackend.Build b, Object subject, ObjectType subjectType,
                                  Expr.AttributeReference read) {
        return DroolsBackend.reached(b.model(), subject, subjectType, read, walk(b, read));
    }

    private static List<RoleBindingRef> walk(DroolsBackend.Build b, Expr.AttributeReference read) {
        return b.execModelTranspiler().navigationBindings(read);
    }

    /** The reads of the rules of the build's model whose source text is {@code snippet}. */
    private static List<Expr.AttributeReference> reads(DroolsBackend.Build b, String snippet) {
        List<Expr.AttributeReference> all = new ArrayList<>();
        for (ResolvedRule r : b.model().resolvedRules) collect(r, all);
        List<Expr.AttributeReference> out = new ArrayList<>();
        for (Expr.AttributeReference a : all)
            if (a.source() != null && snippet.equals(a.source().snippet())) out.add(a);
        assertTrue(!out.isEmpty(), "no read '" + snippet + "' in the model");
        return out;
    }

    /** The read the value of the rule havenopening is. */
    private static List<Expr.AttributeReference> havenopeningValue(DroolsBackend.Build b) {
        List<Expr.AttributeReference> out = new ArrayList<>();
        for (ResolvedRule r : b.model().resolvedRules)
            if (r.name().startsWith("havenopening")) collect(r.value(), out);
        return out;
    }

    private static <T> T only(List<T> list) {
        assertEquals(1, list.size(), list::toString);
        return list.get(0);
    }

    /** Every {@link Expr.AttributeReference} in {@code node}, a record of the IR or a collection of them, in order. */
    private static void collect(Object node, List<Expr.AttributeReference> sink) {
        if (node instanceof Expr.AttributeReference a) sink.add(a);
        if (node instanceof Collection<?> c) {
            for (Object o : c) collect(o, sink);
            return;
        }
        if (node == null || !node.getClass().isRecord()) return;
        for (RecordComponent rc : node.getClass().getRecordComponents()) {
            try {
                collect(rc.getAccessor().invoke(node), sink);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static Object member(Object bean, String name) {
        assertNotNull(bean);
        return RegelSpraakRuntime.get(bean, Namen.property(name));
    }

    private static DroolsBackend.Build build(String golden) throws Exception {
        return DroolsBackend.build(ir(golden), DroolsBackend.Branch.EXEC_MODEL);
    }

    private static Path ir(String golden) {
        Path p = CORPUS.resolve(golden).resolve("ir.json");
        assertTrue(Files.exists(p), "golden ir.json present: " + p.toAbsolutePath());
        return p;
    }

    /** Case {@code name} of golden havenketen played through the public entry and written back: the beans by label.
     *  The cases carry no parameter and no evaluation date. */
    private static Map<String, Object> playAndWriteBack(DroolsBackend.Build b, String name) throws Exception {
        JsonNode c = JSON.readTree(Files.readString(CORPUS.resolve(HAVENKETEN).resolve(name + ".json")));
        assertTrue(c.get("asOfDate").isNull() && c.get("input").get("parameters").isEmpty(), name);
        CaseInput input = CaseInput.of(b, c);
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        KieSession session = b.kieBase().newKieSession();
        try {
            DroolsBackend.insert(b, session, input.inputData());
            assertNull(DroolsBackend.fire(b, session).noFixpoint(), name);
            DroolsBackend.setDerivedValues(b, session);
        } finally {
            session.dispose();
            Thread.currentThread().setContextClassLoader(previous);
        }
        return input.beans();
    }
}
