package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.CaseRunner;
import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.transpiler.kern.Namen;
import dev.uchar.regelspraak.transpiler.tms.plan.Alpha;
import dev.uchar.regelspraak.transpiler.tms.plan.LhsItem;
import dev.uchar.regelspraak.transpiler.tms.plan.Subject;
import dev.uchar.regelspraak.transpiler.tms.plan.RulePlan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The plan iterates in the same order in every run (card drl-teksttak-op-truth-maintenance, step 1e): a rendering into
 * DRL text spells every collection of the plan in its order, and the text must be the same in every run. The immutable
 * sets and maps of {@code Set.of}, {@code Set.copyOf}, {@code Map.of} and {@code Map.copyOf} iterate in an order that
 * the JVM salts per run, and a hash set or hash map orders by hash codes, which for a class are identity hashes. Two
 * checks:
 * <ul>
 *   <li>over the plans of every golden the branch builds, no set or map of two or more elements is one of those;</li>
 *   <li>the classes a rule leaves out because a defaults stack writes its target there
 *       ({@link Alpha.Comparator#CLASS_NOT_IN}) come in the order of the document's types: in
 *       {@code jutterswet-diepwrakbasis} Wrakrecht before Diepwrak, as the document declares them.</li>
 * </ul>
 * The first check sees the class of a collection, and so not a hash set behind a wrapper (fix round 1e). The second
 * therefore checks the order itself, over every such set of the corpus, and builds {@code jutterswet-diepwrakbasis}
 * twelve times, each time with one more unread attribute, so that every build compiles bean classes of its own, with
 * identity hashes of their own: a set that orders by hash gives the document's order in all twelve with a chance of
 * one in 4096.
 *
 * <p><b>Red</b> before the step, on {@code 892be570}: that set was a {@code Set.copyOf}, whose order differed between
 * runs of the measurement {@code planvorm} with the same classes. <b>Red</b> for fix round 1e, on {@code 32b44001} with
 * the set made a {@code HashSet} behind {@code Collections.unmodifiableSet}: the first check stays green, the order
 * check fails.
 */
class PlanOrderWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    /** Floor, measured 2026-10-13 at step 1e: the sets and maps of two or more elements in the plans of the corpus. A
     *  deliberate removal lowers it in the same commit. */
    private static final int EXPECTED_MIN_SETS = 1;

    @Test void no_set_of_the_plan_has_an_order_per_run() throws Exception {
        List<String> salted = new ArrayList<>();
        int[] setCount = {0};
        List<Path> goldens;
        try (Stream<Path> s = Files.walk(GOLDEN)) {
            goldens = s.filter(p -> p.getFileName().toString().equals("ir.json")).sorted().toList();
        }
        for (Path ir : goldens) {
            DroolsBackend.Build b;
            try {
                b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
            } catch (IrLoader.Refusal | IrLoader.Documentfout | CaseRunner.Excluded refused) {
                continue;
            }
            String golden = GOLDEN.relativize(ir.getParent()).toString();
            for (RulePlan p : b.execModelTranspiler().modelplan().rules())
                loop(p, golden + " | " + p.naam(), salted, setCount, new IdentityHashMap<>());
        }
        assertEquals(List.of(), salted, "verzamelingen met een volgorde per run");
        int n = setCount[0];
        assertTrue(n >= EXPECTED_MIN_SETS, () -> n + " verzamelingen, onder de vloer "
                + EXPECTED_MIN_SETS);
    }

    /** Floor, from the measurement planvorm of 2026-10-13 (fix round 1e): the six sets of classes a rule leaves out in
     *  the plans of the goldens the branch plays. A deliberate removal lowers it in the same commit. */
    private static final int EXPECTED_MIN_CLASSES_SETS = 6;

    @Test void the_classes_outside_a_stack_are_in_document_order(@TempDir Path map) throws Exception {
        List<String> outsideOrder = new ArrayList<>();
        int sets = 0;
        try (Stream<Path> s = Files.walk(GOLDEN)) {
            for (Path ir : s.filter(p -> p.getFileName().toString().equals("ir.json")).sorted().toList()) {
                DroolsBackend.Build b;
                try {
                    b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
                } catch (IrLoader.Refusal | IrLoader.Documentfout | CaseRunner.Excluded refused) {
                    continue;
                }
                List<String> typen = new ArrayList<>();
                for (String type : b.model().objecttypen.keySet()) typen.add(Namen.ident(type, true));
                for (List<String> names : classesOutsideStack(b)) {
                    sets++;
                    List<Integer> places = new ArrayList<>();
                    for (String n : names) places.add(typen.indexOf(n));
                    List<Integer> sorted = new ArrayList<>(places);
                    sorted.sort(null);
                    if (places.contains(-1) || !places.equals(sorted))
                        outsideOrder.add(GOLDEN.relativize(ir.getParent()) + " | " + names + " | typen " + typen);
                }
            }
        }
        assertEquals(List.of(), outsideOrder, "klassen buiten een stapel die niet in documentvolgorde staan");
        int n = sets;
        assertTrue(n >= EXPECTED_MIN_CLASSES_SETS, () -> n + " sets, onder de vloer " + EXPECTED_MIN_CLASSES_SETS);
        // twelve builds, each with bean classes of its own: the bean compiler shares the classes of equal sources, so
        // each build's document carries one more unread attribute, with a name of its own
        com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode diepwrak =
                json.readTree(GOLDEN.resolve("jutterswet-diepwrakbasis").resolve("ir.json").toFile());
        Set<ClassLoader> laders = new HashSet<>();
        for (int i = 0; i < 12; i++) {
            com.fasterxml.jackson.databind.node.ObjectNode d = diepwrak.deepCopy();
            ((com.fasterxml.jackson.databind.node.ArrayNode) d.get("model").get("objectTypes").get(0).get("attributes"))
                    .add(json.readTree("{\"node\": \"Attribute\", \"name\": \"proef " + i + "\", \"datatype\":"
                            + " {\"node\": \"Number\", \"whole\": true, \"decimals\": 0, \"sign\": \"nonNegative\"}}"));
            Path ir = map.resolve("proef" + i + ".json");
            json.writeValue(ir.toFile(), d);
            DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
            assertTrue(laders.add(b.beanLoader()), "elke bouw laadt zijn beans opnieuw");
            Set<List<String>> orders = new TreeSet<>((x, y) -> x.toString().compareTo(y.toString()));
            orders.addAll(classesOutsideStack(b));
            assertEquals("[[Diepwrak], [Wrakrecht, Diepwrak]]", orders.toString(), "bouw " + i);
        }
    }

    /** The simple names of the classes of every {@link Alpha.Comparator#CLASS_NOT_IN} test of the plans of
     *  {@code b}, each in the order its set iterates. */
    private static List<List<String>> classesOutsideStack(DroolsBackend.Build b) {
        List<List<String>> out = new ArrayList<>();
        for (RulePlan p : b.execModelTranspiler().modelplan().rules())
            for (LhsItem i : p.leftSide())
                if (i instanceof Subject o)
                    for (Alpha a : o.alpha())
                        for (Alpha.FieldTest t : a.tests())
                            if (t.op() == Alpha.Comparator.CLASS_NOT_IN) {
                                List<String> names = new ArrayList<>();
                                for (Object k : (Collection<?>) t.waarde()) names.add(((Class<?>) k).getSimpleName());
                                out.add(names);
                            }
        return out;
    }

    /** Records under {@code o} every set or map of two or more elements, and each that iterates in an order per run. */
    private static void loop(Object o, String place, List<String> salted, int[] setCount,
                             Map<Object, Boolean> seen) throws ReflectiveOperationException {
        if (o == null || o instanceof String || o instanceof Number || o instanceof Enum<?> || o instanceof Class<?>)
            return;
        if (seen.put(o, Boolean.TRUE) != null) return;
        if ((o instanceof Set<?> s && s.size() > 1) || (o instanceof Map<?, ?> m && m.size() > 1)) {
            setCount[0]++;
            String javaClass = o.getClass().getName();
            if (javaClass.startsWith("java.util.ImmutableCollections$") || o.getClass() == HashSet.class
                    || o.getClass() == HashMap.class)
                salted.add(place + " | " + javaClass + " " + o);
        }
        if (o instanceof Collection<?> c) {
            for (Object x : c) loop(x, place, salted, setCount, seen);
        } else if (o instanceof Map<?, ?> m) {
            for (Map.Entry<?, ?> e : m.entrySet()) {
                loop(e.getKey(), place, salted, setCount, seen);
                loop(e.getValue(), place, salted, setCount, seen);
            }
        } else if (o.getClass().isRecord()) {
            for (RecordComponent rc : o.getClass().getRecordComponents()) {
                rc.getAccessor().setAccessible(true);
                loop(rc.getAccessor().invoke(o), place, salted, setCount, seen);
            }
        }
    }
}
