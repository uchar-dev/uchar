package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.transpiler.tms.plan.Route;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every route of the plan ({@link Route}) is a public static method of a public class, the only method of its name
 * there, so that a DRL text calls it class-qualified and finds one method by its name (card
 * drl-teksttak-op-truth-maintenance, step 1c-A2): a fresh probe, a report to it and its verdict, the fault of a fixed sentence, the inconsistency of a form and
 * the asOfDate-bucket scalar of an input had no public route before the step; the fault of an input's form, the keys of
 * a uniqueness and the facts of an allocation's distribution none before step 1c-B1; the creation depth of an object
 * creation, whose fault past the budget has no public constructor, none before step 1c-C1; and none before step 1e the
 * routes a DRL text needs where MVEL has no construct of its own (FABLE-2 §2.1): catching, naming and throwing an
 * evaluation fault, joining texts, reading a bean and walking a path with null at an absent link, the tests of a
 * reading form, a validity window and a class by its name, the cell of a {@code Let} with its public
 * {@link EmptyBarrier}, the verdict of a matrix, a broken contract, the fault of writers that disagree over lists, and
 * a list of a fixed number of values; and none before step 5 the construction of an object creation's new bean, of a
 * class the text names, and the setting of its attribute initialisations. The witness pins each route with its
 * signature, which a rendering into DRL text writes, what the three routes of the probe do, what the routes of step 1e
 * do, and what those of step 5 do. The rendering into closures calls
 * the same methods where it has a use for them ({@link PatternDslRendering}, {@link ClosureRendering}); that its
 * behaviour did not change is what the harness {@code FireTrace} shows.
 *
 * <p><b>Red</b> before the step: there was no {@code Route}, and this class did not compile. Before step 1e the routes
 * of that step did not exist, and the two tests over them did not compile; before step 5 neither did the routes of a
 * creation, and their test did not compile.
 */
class RouteWitnessTest {

    @Test void every_route_is_the_only_public_static_method_of_its_name() {
        List<String> fault = new ArrayList<>();
        for (Route r : Route.values()) {
            List<Method> methods = Arrays.stream(r.javaClass().getMethods())
                    .filter(m -> m.getName().equals(r.method())).toList();
            if (methods.size() != 1 || !Modifier.isStatic(methods.get(0).getModifiers())
                    || !Modifier.isPublic(r.javaClass().getModifiers()))
                fault.add(r + " " + r.javaClass().getName() + "." + r.method() + ": " + methods);
        }
        assertEquals(List.of(), fault);
    }

    @Test void the_routes_with_their_signature() {
        List<String> out = new ArrayList<>();
        for (Route r : Route.values())
            for (Method m : r.javaClass().getMethods())
                if (m.getName().equals(r.method()))
                    out.add(r + ": " + m.getReturnType().getSimpleName() + " " + r.javaClass().getSimpleName() + "."
                            + m.getName() + Arrays.stream(m.getParameterTypes()).map(Class::getSimpleName).toList());
        assertEquals(List.of(
                "FRESH_DEPOSIT_FLAG: Object ComputeHelpers.depositFlag[]",
                "REPORT: void ComputeHelpers.report[Object]",
                "REPORTED: boolean ComputeHelpers.reported[Object]",
                "IS_EMPTY: boolean ComputeHelpers.isEmpty[Object]",
                "COMPUTE_FAULT: ComputeFault ComputeHelpers.computeFault[String, String]",
                "INCONSISTENCY: Object ComputeHelpers.inconsistency[String, Object]",
                "BUCKET_SCALAR: BucketScalar ComputeHelpers.bucketScalarOf[Object]",
                "INPUT_FORM_FAULT: InputFormFault ComputeHelpers.inputFormFault[String, Object, String, String]",
                "UNIQUENESS_KEYS: List ComputeHelpers.uniquenessKeys[String, int, Object, Object, List, int]",
                "PAYOUT: List ComputeHelpers.payout[String, Object, Method, Object, Object, boolean, boolean,"
                        + " boolean, Integer, ElementKey, ElementKey, String]",
                "CONTRIBUTION: Contribution ComputeHelpers.contribution[String, Object, Object, Object, String, boolean]",
                "FOLD_PLACE: List ComputeHelpers.foldPlace[PlaceReason, String, Object, int, List, List, List]",
                "CREATION_DEPTH: int ComputeHelpers.creationDepth[String, int, String, List, int]",
                "NEW_OBJECT: Object ComputeHelpers.newObject[Object, String]",
                "SET: void ComputeHelpers.set[Object, String, Object]",
                "IS_EVALUATION_FAULT: boolean ComputeHelpers.isEvaluationFault[Object]",
                "CAUGHT: RuntimeException ComputeHelpers.caught[RuntimeException]",
                "DESIGNATE: RuntimeException ComputeHelpers.designate[Object, String]",
                "FAIL: Object ComputeHelpers.fail[String]",
                "REQUIRED: Object ComputeHelpers.required[Object, String]",
                "TEXT_COMPOSITION: String ComputeHelpers.textComposition[List]",
                "READ: Object ComputeHelpers.readProperty[Object, String]",
                "ALONG: Object ComputeHelpers.along[Object, List]",
                "WAAR: boolean ComputeHelpers.holds[Object]",
                "PERIODS: List ComputeHelpers.periods[Object]",
                "IS_SET: boolean ComputeHelpers.isSet[Object, String]",
                "WITHIN: boolean ComputeHelpers.inside[Object, String, String]",
                "IS_A: boolean ComputeHelpers.isA[Object, String]",
                "CLASS_NOT_IN: boolean ComputeHelpers.classNotIn[Object, List]",
                "CLASS_IS: boolean ComputeHelpers.classIs[Object, String]",
                "BINDER_CELL: Object ComputeHelpers.binderCell[]",
                "BINDER_CELL_FILLED: boolean ComputeHelpers.binderCellFilled[Object]",
                "FILL_BINDER_CELL: Object ComputeHelpers.fillBinderCell[Object, Object]",
                "BINDER_VALUE: Object ComputeHelpers.binderValue[Object]",
                "INTERCEPTED: Object ComputeHelpers.intercepted[EmptyBarrier, Object]",
                "ESTABLISHED: Boolean ComputeHelpers.established[Quantifier, int, int, int, int]",
                "END_VERDICT: boolean ComputeHelpers.endVerdict[Quantifier, int, long, long]",
                "VERDICT: BigDecimal ComputeHelpers.verdict[boolean]",
                "CONTRACT_BREACH: Object ComputeHelpers.contractBreach[String]",
                "WRITERS_CONFLICT: Fault ComputeHelpers.writersConflict[Object, Object, String, List, List, List]",
                "LIST0: List ComputeHelpers.list0[]",
                "LIST1: List ComputeHelpers.list1[Object]",
                "LIST2: List ComputeHelpers.list2[Object, Object]",
                "LIST3: List ComputeHelpers.list3[Object, Object, Object]",
                "LIST4: List ComputeHelpers.list4[Object, Object, Object, Object]",
                "LIST5: List ComputeHelpers.list5[Object, Object, Object, Object, Object]",
                "LIST6: List ComputeHelpers.list6[Object, Object, Object, Object, Object, Object]"), out);
    }

    /** A fresh probe heard nothing; a report makes it heard; a report to an empty slot and the verdict of an empty slot
     *  do nothing and hear nothing, since a place outside a deposit test or a depositing consequence reports to an
     *  empty slot. */
    @Test void a_deposit_flag_is_raised_by_what_is_reported_to_it() {
        Object depositFlag = ComputeHelpers.depositFlag();
        assertFalse(ComputeHelpers.reported(depositFlag));
        ComputeHelpers.report(depositFlag);
        assertTrue(ComputeHelpers.reported(depositFlag));
        assertFalse(ComputeHelpers.reported(ComputeHelpers.depositFlag()));
        ComputeHelpers.report(null);
        assertFalse(ComputeHelpers.reported(null));
    }

    /** The routes of the faults and the texts: an evaluation fault is caught, named and recognised, any other exception
     *  passes on; a sentence and a leeg required value fault; texts join in order. */
    @Test void the_routes_of_a_fault_and_a_text() {
        ComputeFault fault = assertThrows(ComputeFault.class, () -> ComputeHelpers.fail("geen dag"));
        assertEquals("geen dag", fault.sentence());
        assertTrue(ComputeHelpers.isEvaluationFault(fault));
        assertFalse(ComputeHelpers.isEvaluationFault(new IllegalStateException("contract")));
        assertFalse(ComputeHelpers.isEvaluationFault("geen fout"));
        assertSame(fault, ComputeHelpers.caught(fault));
        IllegalStateException breach = new IllegalStateException("contract");
        assertSame(breach, assertThrows(IllegalStateException.class, () -> ComputeHelpers.caught(breach)));
        assertEquals("fout in de regel \"r\": geen dag", ComputeHelpers.designate(fault, "r").getMessage());
        assertEquals(5, ComputeHelpers.required(5, "leeg"));
        assertEquals("leeg", assertThrows(ComputeFault.class, () -> ComputeHelpers.required(null, "leeg")).sentence());
        assertEquals("ab", ComputeHelpers.textComposition(List.of("a", "b")));
        assertEquals("x", assertThrows(IllegalStateException.class, () -> ComputeHelpers.contractBreach("x")).getMessage());
    }

    /** The routes of a read: a bean member by its property, null on an absent object; a walk over reference fields that
     *  reads null at an absent link; the reading forms; a class by its name, through its superclasses and interfaces;
     *  a validity window with open bounds. */
    @Test void the_routes_of_a_read() {
        Member b = new Member(null, BigDecimal.TEN), a = new Member(b, BigDecimal.ONE);
        assertEquals(BigDecimal.ONE, ComputeHelpers.readProperty(a, "amount"));
        assertNull(ComputeHelpers.readProperty(null, "amount"));
        assertSame(b, ComputeHelpers.along(a, List.of("next")));
        assertNull(ComputeHelpers.along(a, List.of("next", "next", "next")));
        assertEquals(BigDecimal.TEN, ComputeHelpers.readProperty(ComputeHelpers.along(a, List.of("next")), "amount"));
        assertTrue(ComputeHelpers.holds(Boolean.TRUE));
        assertFalse(ComputeHelpers.holds(null));
        assertNull(ComputeHelpers.periods(null));
        assertTrue(ComputeHelpers.isSet(a, null));
        assertTrue(ComputeHelpers.isSet(a, Member.class.getName()));
        assertFalse(ComputeHelpers.isSet(a, "java.lang.String"));
        assertFalse(ComputeHelpers.isSet(null, null));
        assertTrue(ComputeHelpers.isA(new ArrayList<>(), "java.util.AbstractList"));
        assertTrue(ComputeHelpers.isA(new ArrayList<>(), "java.util.Collection"));
        assertFalse(ComputeHelpers.isA(new ArrayList<>(), "java.util.Map"));
        assertFalse(ComputeHelpers.isA(null, "java.lang.Object"));
        assertTrue(ComputeHelpers.classNotIn(new ArrayList<>(), List.of("java.util.AbstractList")));
        assertFalse(ComputeHelpers.classNotIn(new ArrayList<>(), List.of("java.util.LinkedList", "java.util.ArrayList")));
        assertTrue(ComputeHelpers.classIs(new ArrayList<>(), "java.util.ArrayList"));
        assertFalse(ComputeHelpers.classIs(new ArrayList<>(), "java.util.AbstractList"));
        LocalDate day = LocalDate.of(2026, 1, 1);
        assertTrue(ComputeHelpers.inside(day, "2026-01-01", null));
        assertFalse(ComputeHelpers.inside(day, null, "2025-12-31"));
        assertFalse(ComputeHelpers.inside(null, null, null));
    }

    /** The cell of a {@code Let}: empty until a read fills it, and a leeg value ends its own {@code Let} with leeg,
     *  while the barrier of another passes on; the verdict of a matrix; the lists of a fixed number of values, which
     *  may hold null. */
    @Test void the_routes_of_a_let_and_a_list() {
        Object cell = ComputeHelpers.binderCell(), other = ComputeHelpers.binderCell();
        assertFalse(ComputeHelpers.binderCellFilled(cell));
        assertEquals(7, ComputeHelpers.fillBinderCell(cell, 7));
        assertTrue(ComputeHelpers.binderCellFilled(cell));
        assertEquals(7, ComputeHelpers.binderValue(cell));
        ComputeHelpers.fillBinderCell(other, null);
        EmptyBarrier barrier = assertThrows(EmptyBarrier.class, () -> ComputeHelpers.binderValue(other));
        assertSame(other, barrier.cell());
        assertNull(ComputeHelpers.intercepted(barrier, other));
        assertSame(barrier, assertThrows(EmptyBarrier.class, () -> ComputeHelpers.intercepted(barrier, cell)));
        assertEquals(BigDecimal.ONE, ComputeHelpers.verdict(true));
        assertEquals(BigDecimal.ZERO, ComputeHelpers.verdict(false));
        assertEquals(List.of(), ComputeHelpers.list0());
        assertEquals(Arrays.asList(1, null, 3), ComputeHelpers.list3(1, null, 3));
        assertEquals(List.of(1, 2, 3, 4, 5, 6), ComputeHelpers.list6(1, 2, 3, 4, 5, 6));
    }

    /** The routes of a creation: a new bean of a class by its name, through the class loader of the subject's bean;
     *  a class that does not load and one without a public constructor without arguments fail with their names; an
     *  attribute set through the setter of the property, and a leeg value left unset. */
    @Test void the_routes_of_a_creation() {
        Member onderwerp = new Member(null, BigDecimal.ONE);
        Object fresh = ComputeHelpers.newObject(onderwerp, New.class.getName());
        assertTrue(fresh instanceof New, () -> "een " + fresh.getClass());
        assertTrue(assertThrows(IllegalStateException.class, () -> ComputeHelpers.newObject(onderwerp,
                "dev.uchar.regelspraak.facts.Bestaatniet")).getMessage().contains("dev.uchar.regelspraak.facts.Bestaatniet"));
        assertTrue(assertThrows(IllegalStateException.class, () -> ComputeHelpers.newObject(onderwerp,
                Member.class.getName())).getMessage().contains(Member.class.getName() + ", maar die klasse heeft geen publieke"
                + " constructor zonder argumenten"));
        ComputeHelpers.set(fresh, "amount", BigDecimal.TEN);
        assertEquals(BigDecimal.TEN, ((New) fresh).getAmount());
        ComputeHelpers.set(fresh, "amount", null);
        assertEquals(BigDecimal.TEN, ((New) fresh).getAmount(), "een lege waarde laat het lid zoals het is");
    }

    /** A bean of a creation: a public constructor without arguments and a member with its setter. */
    public static final class New {
        private BigDecimal amount;

        public New() {}

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }

    /** A bean with a member and a reference field, read by property. */
    public static final class Member {
        private final Member next;
        private final BigDecimal amount;

        Member(Member next, BigDecimal amount) {
            this.next = next;
            this.amount = amount;
        }

        public Member getNext() {
            return next;
        }

        public BigDecimal getAmount() {
            return amount;
        }
    }
}
