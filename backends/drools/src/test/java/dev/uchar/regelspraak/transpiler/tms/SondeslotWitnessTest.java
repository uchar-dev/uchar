package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.CaseRunner;
import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.transpiler.tms.plan.Gevolg;
import dev.uchar.regelspraak.transpiler.tms.plan.LhsItem;
import dev.uchar.regelspraak.transpiler.tms.plan.Regelplan;
import dev.uchar.regelspraak.transpiler.tms.plan.Term;
import dev.uchar.regelspraak.transpiler.tms.plan.Toetsitem;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A term names a probe slot only where its position in the rule fills it (card drl-teksttak-op-truth-maintenance, step
 * 1e; FABLE-2 §1.2 point 3): a test of deposits ({@link Toetsitem.Rol#DEPONERING}), the value of a
 * {@link Gevolg.Waarde} with a deposit, and the matrix of a {@link Gevolg.Inbreng.Lidtoets} with a probe, each with the
 * slot it fills. Everywhere else, as in the rule of a condition's fault or of a filter's fault, the analysis sets the
 * probe of a division, a square root, a fold's evaluation and a quantifier fold to -1, so that a rendering needs no rule
 * for a slot nobody fills. Over the plans of every golden the branch builds.
 *
 * <p><b>Red</b> before the step, on {@code 8447b5f2}: 36 positions named a probe slot that nobody filled, the tests
 * with the role {@code ZONDER_FOUT} or {@code FOUT}, the {@code Toetsfout} and the {@code Filterfout} of
 * {@code jutterswet-filterwacht}, {@code jutterswet-ploegaandeel} and others.
 */
class SondeslotWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    /** Floors, measured 2026-10-13 at step 1e, so that a check over nothing does not pass: the positions that fill a
     *  probe and whose terms name it. A deliberate removal lowers a floor in the same commit. */
    private static final int EXPECTED_MIN_GEVULD = 48;

    @Test void een_term_noemt_alleen_een_sonde_die_haar_plaats_vult() throws Exception {
        List<String> ongevuld = new ArrayList<>();
        int gevuld = 0;
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
            for (Regelplan p : b.execModelTranspiler().modelplan().regels()) {
                for (LhsItem i : p.linkerkant()) {
                    if (!(i instanceof Toetsitem t)) continue;
                    Set<Integer> s = sondes(t.toets());
                    if (s.isEmpty()) continue;
                    if (t.rol() == Toetsitem.Rol.DEPONERING && s.equals(Set.of(t.sonde()))) gevuld++;
                    else ongevuld.add(golden + " | " + p.naam() + " | toets " + t.rol() + " " + s);
                }
                Set<Integer> s = sondes(p.gevolg());
                if (s.isEmpty()) continue;
                Integer vult = switch (p.gevolg()) {
                    case Gevolg.Waarde w when w.deponering() != null -> w.deponering().sonde();
                    case Gevolg.Bijdrage bd when bd.inbreng() instanceof Gevolg.Inbreng.Lidtoets l && l.sonde() >= 0 ->
                            l.sonde();
                    default -> null;
                };
                if (vult != null && s.equals(Set.of(vult))) gevuld++;
                else ongevuld.add(golden + " | " + p.naam() + " | gevolg " + p.gevolg().getClass().getSimpleName()
                        + " " + s);
            }
        }
        assertEquals(List.of(), ongevuld, "sondes die geen plaats vult");
        int n = gevuld;
        assertTrue(n >= EXPECTED_MIN_GEVULD, () -> n + " plaatsen die hun sonde vullen, onder de vloer "
                + EXPECTED_MIN_GEVULD);
    }

    /** The probe slots the terms under {@code o} name: of a division, a square root, a fold's evaluation and a
     *  quantifier fold. The probe of a test, of a deposit and of a member's test is the slot a position fills, so it is
     *  left out. */
    private static Set<Integer> sondes(Object o) throws ReflectiveOperationException {
        Set<Integer> uit = new TreeSet<>();
        verzamel(o, uit);
        return uit;
    }

    private static void verzamel(Object o, Set<Integer> uit) throws ReflectiveOperationException {
        if (o == null || o instanceof String || o instanceof Number || o instanceof Enum<?> || o instanceof Class<?>)
            return;
        switch (o) {
            case Term.Deel d when d.sonde() >= 0 -> uit.add(d.sonde());
            case Term.Wortel w when w.sonde() >= 0 -> uit.add(w.sonde());
            case Term.Vouwlezing v when v.sonde() >= 0 -> uit.add(v.sonde());
            case Term.Kwantorlezing k when k.sonde() >= 0 -> uit.add(k.sonde());
            default -> { }
        }
        if (o instanceof Collection<?> c) {
            for (Object x : c) verzamel(x, uit);
        } else if (o instanceof Map<?, ?> m) {
            for (Object x : m.values()) verzamel(x, uit);
        } else if (o.getClass().isRecord() && !(o instanceof Gevolg.Deponering)) {
            for (RecordComponent rc : o.getClass().getRecordComponents()) {
                rc.getAccessor().setAccessible(true);
                verzamel(rc.getAccessor().invoke(o), uit);
            }
        }
    }
}
