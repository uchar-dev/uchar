package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.CaseRunner;
import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.transpiler.tms.plan.Alfa;
import dev.uchar.regelspraak.transpiler.tms.plan.LhsItem;
import dev.uchar.regelspraak.transpiler.tms.plan.Onderwerp;
import dev.uchar.regelspraak.transpiler.tms.plan.Regelplan;

import org.junit.jupiter.api.Test;

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
 *       ({@link Alfa.Vergelijker#KLASSE_NIET_IN}) come in the order of the document's types: in
 *       {@code jutterswet-diepwrakbasis} Wrakrecht before Diepwrak, as the document declares them.</li>
 * </ul>
 *
 * <p><b>Red</b> before the step, on {@code 892be570}: that set was a {@code Set.copyOf}, whose order differed between
 * runs of the measurement {@code planvorm} with the same classes.
 */
class PlanvolgordeWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    /** Floor, measured 2026-10-13 at step 1e: the sets and maps of two or more elements in the plans of the corpus. A
     *  deliberate removal lowers it in the same commit. */
    private static final int EXPECTED_MIN_VERZAMELINGEN = 1;

    @Test void geen_verzameling_van_het_plan_heeft_een_volgorde_per_run() throws Exception {
        List<String> gezouten = new ArrayList<>();
        int[] verzamelingen = {0};
        List<Path> goldens;
        try (Stream<Path> s = Files.walk(GOLDEN)) {
            goldens = s.filter(p -> p.getFileName().toString().equals("ir.json")).sorted().toList();
        }
        for (Path ir : goldens) {
            DroolsBackend.Build b;
            try {
                b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
            } catch (IrLoader.Refusal | IrLoader.Documentfout | CaseRunner.Excluded geweigerd) {
                continue;
            }
            String golden = GOLDEN.relativize(ir.getParent()).toString();
            for (Regelplan p : b.execModelTranspiler().modelplan().regels())
                loop(p, golden + " | " + p.naam(), gezouten, verzamelingen, new IdentityHashMap<>());
        }
        assertEquals(List.of(), gezouten, "verzamelingen met een volgorde per run");
        int n = verzamelingen[0];
        assertTrue(n >= EXPECTED_MIN_VERZAMELINGEN, () -> n + " verzamelingen, onder de vloer "
                + EXPECTED_MIN_VERZAMELINGEN);
    }

    @Test void de_klassen_buiten_een_stapel_staan_in_documentvolgorde() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(GOLDEN.resolve("jutterswet-diepwrakbasis").resolve("ir.json"),
                DroolsBackend.Branch.EXEC_MODEL);
        Set<List<String>> volgordes = new TreeSet<>((x, y) -> x.toString().compareTo(y.toString()));
        for (Regelplan p : b.execModelTranspiler().modelplan().regels())
            for (LhsItem i : p.linkerkant())
                if (i instanceof Onderwerp o)
                    for (Alfa a : o.alfa())
                        for (Alfa.Veldtoets t : a.toetsen())
                            if (t.op() == Alfa.Vergelijker.KLASSE_NIET_IN) {
                                List<String> namen = new ArrayList<>();
                                for (Object k : (Collection<?>) t.waarde()) namen.add(((Class<?>) k).getSimpleName());
                                volgordes.add(namen);
                            }
        assertEquals("[[Diepwrak], [Wrakrecht, Diepwrak]]", volgordes.toString());
    }

    /** Records under {@code o} every set or map of two or more elements, and each that iterates in an order per run. */
    private static void loop(Object o, String plaats, List<String> gezouten, int[] verzamelingen,
                             Map<Object, Boolean> gezien) throws ReflectiveOperationException {
        if (o == null || o instanceof String || o instanceof Number || o instanceof Enum<?> || o instanceof Class<?>)
            return;
        if (gezien.put(o, Boolean.TRUE) != null) return;
        if ((o instanceof Set<?> s && s.size() > 1) || (o instanceof Map<?, ?> m && m.size() > 1)) {
            verzamelingen[0]++;
            String klasse = o.getClass().getName();
            if (klasse.startsWith("java.util.ImmutableCollections$") || o.getClass() == HashSet.class
                    || o.getClass() == HashMap.class)
                gezouten.add(plaats + " | " + klasse + " " + o);
        }
        if (o instanceof Collection<?> c) {
            for (Object x : c) loop(x, plaats, gezouten, verzamelingen, gezien);
        } else if (o instanceof Map<?, ?> m) {
            for (Map.Entry<?, ?> e : m.entrySet()) {
                loop(e.getKey(), plaats, gezouten, verzamelingen, gezien);
                loop(e.getValue(), plaats, gezouten, verzamelingen, gezien);
            }
        } else if (o.getClass().isRecord()) {
            for (RecordComponent rc : o.getClass().getRecordComponents()) {
                rc.getAccessor().setAccessible(true);
                loop(rc.getAccessor().invoke(o), plaats, gezouten, verzamelingen, gezien);
            }
        }
    }
}
