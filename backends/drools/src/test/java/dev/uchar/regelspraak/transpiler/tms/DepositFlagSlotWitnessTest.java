package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.CaseRunner;
import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.transpiler.tms.plan.Consequence;
import dev.uchar.regelspraak.transpiler.tms.plan.LhsItem;
import dev.uchar.regelspraak.transpiler.tms.plan.RulePlan;
import dev.uchar.regelspraak.transpiler.tms.plan.Term;
import dev.uchar.regelspraak.transpiler.tms.plan.TestItem;

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
 * 1e; FABLE-2 §1.2 point 3): a test of deposits ({@link TestItem.Role#DEPOSIT}), the value of a
 * {@link Consequence.Value} with a deposit, the matrix of a {@link Consequence.Input.MemberTest} with a probe, and, since the
 * card zero-division-channel (2026-10-19), the condition of a {@link Consequence.ConditionDeposit} and the filter of a
 * {@link Consequence.FilterDeposit}, evaluated again to hear the codes, each with the slot it fills. Everywhere else, as in the rule of a condition's fault or of a filter's fault, the analysis sets the
 * probe of a division, a square root, a fold's evaluation and a quantifier fold to -1, so that a rendering needs no rule
 * for a slot nobody fills. Over the plans of every golden the branch builds.
 *
 * <p><b>Red</b> before the step, on {@code 8447b5f2}: 36 positions named a probe slot that nobody filled, the tests
 * with the role {@code WITHOUT_FAULT} or {@code FAULT}, the {@code TestFault} and the {@code FilterFault} of
 * {@code jutterswet-filterwacht}, {@code jutterswet-ploegaandeel} and others.
 */
class DepositFlagSlotWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    /** Floors, measured 2026-10-13 at step 1e, so that a check over nothing does not pass: the positions that fill a
     *  probe and whose terms name it. A deliberate removal lowers a floor in the same commit. */
    private static final int EXPECTED_MIN_FILLED = 48;

    @Test void a_term_names_a_deposit_flag_only_where_its_place_fills_it() throws Exception {
        List<String> unfilled = new ArrayList<>();
        int filled = 0;
        List<Path> goldens;
        try (Stream<Path> s = Files.walk(GOLDEN)) {
            goldens = s.filter(p -> p.getFileName().toString().equals("ir.json")).sorted().toList();
        }
        for (Path ir : goldens) {
            DroolsBackend.Build b;
            try {
                b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
            } catch (IrLoader.Refusal | IrLoader.DocumentError | CaseRunner.Excluded refused) {
                continue;
            }
            String golden = GOLDEN.relativize(ir.getParent()).toString();
            for (RulePlan p : b.execModelTranspiler().modelplan().rules()) {
                for (LhsItem i : p.leftSide()) {
                    if (!(i instanceof TestItem t)) continue;
                    Set<Integer> s = sondes(t.test());
                    if (s.isEmpty()) continue;
                    if (t.role() == TestItem.Role.DEPOSIT && s.equals(Set.of(t.depositFlag()))) filled++;
                    else unfilled.add(golden + " | " + p.naam() + " | toets " + t.role() + " " + s);
                }
                Set<Integer> s = sondes(p.consequence());
                if (s.isEmpty()) continue;
                Integer fills = switch (p.consequence()) {
                    case Consequence.Value w when w.deposit() != null -> w.deposit().depositFlag();
                    case Consequence.Contribution bd when bd.input() instanceof Consequence.Input.MemberTest l && l.depositFlag() >= 0 ->
                            l.depositFlag();
                    case Consequence.ConditionDeposit c -> c.depositFlag();
                    case Consequence.FilterDeposit f -> f.depositFlag();
                    default -> null;
                };
                if (fills != null && s.equals(Set.of(fills))) filled++;
                else unfilled.add(golden + " | " + p.naam() + " | gevolg " + p.consequence().getClass().getSimpleName()
                        + " " + s);
            }
        }
        assertEquals(List.of(), unfilled, "sondes die geen plaats vult");
        int n = filled;
        assertTrue(n >= EXPECTED_MIN_FILLED, () -> n + " plaatsen die hun sonde vullen, onder de vloer "
                + EXPECTED_MIN_FILLED);
    }

    /** The probe slots the terms under {@code o} name: of a division, a square root, a fold's evaluation and a
     *  quantifier fold. The probe of a test, of a deposit and of a member's test is the slot a position fills, so it is
     *  left out. */
    private static Set<Integer> sondes(Object o) throws ReflectiveOperationException {
        Set<Integer> out = new TreeSet<>();
        collect(o, out);
        return out;
    }

    private static void collect(Object o, Set<Integer> out) throws ReflectiveOperationException {
        if (o == null || o instanceof String || o instanceof Number || o instanceof Enum<?> || o instanceof Class<?>)
            return;
        switch (o) {
            case Term.Division d when d.depositFlag() >= 0 -> out.add(d.depositFlag());
            case Term.SquareRoot w when w.depositFlag() >= 0 -> out.add(w.depositFlag());
            case Term.FoldRead v when v.depositFlag() >= 0 -> out.add(v.depositFlag());
            case Term.QuantifierRead k when k.depositFlag() >= 0 -> out.add(k.depositFlag());
            default -> { }
        }
        if (o instanceof Collection<?> c) {
            for (Object x : c) collect(x, out);
        } else if (o instanceof Map<?, ?> m) {
            for (Object x : m.values()) collect(x, out);
        } else if (o.getClass().isRecord() && !(o instanceof Consequence.Deposit)) {
            for (RecordComponent rc : o.getClass().getRecordComponents()) {
                rc.getAccessor().setAccessible(true);
                collect(rc.getAccessor().invoke(o), out);
            }
        }
    }
}
