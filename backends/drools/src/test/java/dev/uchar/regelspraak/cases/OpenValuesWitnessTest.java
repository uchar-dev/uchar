package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The principles of RUNTIME-SEMANTICS ch. 1.5 that this backend carries, each on the smallest document of the
 * Jutterswet that shows it, under the name of the example in the spec (WV-1.5-…): a document with
 * {@code inferFromEmpty: false} read with the three states of a member, filled, settled leeg and open. The documents
 * stand in {@code src/test/resources/open-values/<name>/}, written in RegelSpraak ({@code source.rgs}) and taken to the
 * wire by the frontend ({@code ir.json}), but the four of a stack with an initialisation, which the dialect writes only
 * beside an opgave and which stand as the frontend's document with that default added, {@code quantifier-fold}, with
 * a consistency rule over a quantifier fold added, and {@code fold-subtype}, {@code past-undetermined-fold},
 * {@code past-undetermined-filter}, {@code quantifier-fold-filter-faults}, {@code quantifier-fold-two-routes},
 * {@code anchored-read},
 * {@code fold-over-a-union} and {@code fold-in-a-date-stack}, with a part of one of their forms put in another, where the
 * dialect does not write it; their cases carry the expectations of the spec's example, and the reference
 * interpreter plays every one of them ({@code runner}, {@code runner --closed-inputs} for a case named
 * {@code closed*}), so a case here is the outcome of ref beside that of this backend. Every case plays on both branches
 * and must meet its expectations there; a case named {@code closed*} plays in a session that treats every input
 * member as complete ({@link Input#closedInputs}); one named {@code red*} must fault on both branches with the fault
 * kind and the parameters it pins, as gate 8 holds a red case.
 *
 * <p><b>Red</b> on the code before the reading was carried: every document with the property was refused with
 * DROOLS-370 and {@code form=document} before it was built, and the two documents without it played as now. Every
 * document {@code stack-*} was refused with {@code form=defaults-group} before the stack was carried, and every
 * document with a fold with {@code form=set-fold} or {@code form=quantifier-fold} before the folds were, and
 * {@code anchored-read} with {@code form=anchored-read} before an anchored read of open input was.
 */
class OpenValuesWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path FIXTURES = Path.of("src", "test", "resources", "open-values");

    /** Plays every case of the fixture {@code name} whose file name starts with {@code prefix} on both branches, and
     *  returns how many it played; a fixture without such a case fails. */
    private static int play(String name, String prefix) throws Exception {
        Path dir = FIXTURES.resolve(name), ir = dir.resolve("ir.json");
        List<Path> cases = cases(dir, prefix);
        assertFalse(cases.isEmpty(), () -> name + ": no case " + prefix + "*");
        List<JsonNode> contents = new ArrayList<>();
        for (Path c : cases) contents.add(JSON.readTree(Files.readString(c)));
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            List<String> verdicts = prefix.equals("red") ? null
                    : CaseRunner.playCases(ir, contents, branch, prefix.equals("closed"));
            for (int i = 0; i < cases.size(); i++) {
                String what = branch + " " + name + "/" + cases.get(i).getFileName();
                if (verdicts != null) {
                    assertNull(verdicts.get(i), what);
                    continue;
                }
                JsonNode pinned = contents.get(i).get("expectedFault");
                CaseRunner.RedResult r = CaseRunner.runRed(ir, cases.get(i), branch);
                assertEquals("FAULT", r.status(), () -> what + ": " + r.detail());
                assertEquals(pinned.get("code").asText(), r.code(), () -> what + ": " + r.detail());
                assertEquals(JSON.convertValue(pinned.get("params"), java.util.Map.class), r.params().get(0), what);
            }
        }
        return cases.size();
    }

    private static List<Path> cases(Path dir, String prefix) throws IOException {
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.getFileName().toString().startsWith(prefix)
                    && p.getFileName().toString().endsWith(".json")).sorted().toList();
        }
    }

    /** WV-1.5-open-input, WV-1.5-case-*: an input member the case gives no value is open, and so is what reads it;
     *  an input characteristic the case does not name is open, and one it states not to hold in
     *  {@code characteristicsAbsent} stands not to hold. */
    @Test void wv_1_5_open_input() throws Exception {
        assertEquals(3, play("open-input", "case"));
    }

    /** WV-1.5-no-open-without-the-property: without the property the same document has no open member, and
     *  {@code characteristicsAbsent} changes no outcome. */
    @Test void wv_1_5_no_open_without_the_property() throws Exception {
        assertEquals(2, play("no-open-without-the-property", "case"));
    }

    /** WV-1.5-complete-*, WV-1.5-preserved: in a session that treats every input member as complete nothing is open,
     *  and the outcomes are those of ch. 1.2–1.4. */
    @Test void wv_1_5_complete_session() throws Exception {
        assertEquals(1, play("open-input", "closed"));
        assertEquals(1, play("filled-test", "closed"));
    }

    /** WV-1.5-complete-*: a member declared complete, an attribute and a characteristic, is settled leeg without a value
     *  on the objects where it is input, those of a subtype among them; on an object where a rule writes it, it follows
     *  the rule as without the field. */
    @Test void wv_1_5_complete() throws Exception {
        assertEquals(2, play("complete", "case"));
        assertEquals(1, play("complete-derived", "case"));
    }

    /** WV-1.5-instance-ordered: an instance-ordered part reaches its fixpoint per object, the leeg test of the
     *  predecessor's member a closure over an open one. */
    @Test void wv_1_5_instance_ordered() throws Exception {
        assertEquals(2, play("instance-ordered", "case"));
    }

    /** WV-1.5-settled-operand, WV-1.5-derived-*, WV-1.5-negation, WV-1.5-input-beside-a-writer, WV-1.5-case-*: a
     *  derived member follows its writer; what stands computes as ch. 1.2–1.4; a negated characteristic check is open
     *  where the characteristic is; a value the case gives a derived member stands only where its writer fills
     *  nothing; a derived characteristic named in {@code characteristicsAbsent} follows its writer. The rule
     *  "toeslag" reads the vindloon and stands before its writer in the document, so where it fires first truth
     *  maintenance withdraws what it derived from the standing it read. */
    @Test void wv_1_5_derived() throws Exception {
        assertEquals(9, play("derived", "case"));
    }

    /** WV-1.5-input-per-object: whether a member is input is asked per object. */
    @Test void wv_1_5_input_per_object() throws Exception {
        assertEquals(1, play("input-per-object", "case"));
    }

    /** WV-1.5-derived-*: a member with two writers is settled leeg where both fill nothing, open where one leaves
     *  it open, and filled where one decides. */
    @Test void wv_1_5_derived_two_writers() throws Exception {
        assertEquals(3, play("two-writers", "case"));
    }

    /** WV-1.5-open-passes-on, WV-1.5-reached, WV-1.5-boundary-*: an open read gives no value and drops what the
     *  evaluation deposited; a settled leeg dividend decides the division without the divisor, and a binder the body
     *  does not read is not read; without the property the division by a leeg divisor deposits EF-031. */
    @Test void wv_1_5_reach() throws Exception {
        assertEquals(3, play("reach", "case"));
        assertEquals(1, play("reach-without-the-property", "case"));
    }

    /** WV-1.5-boundary-*: within one boundary a fault raised before the open read stays a fault, and one that would
     *  come after it does not arise. */
    @Test void wv_1_5_boundary() throws Exception {
        assertEquals(1, play("boundary-before", "red"));
        assertEquals(1, play("boundary-after", "case"));
    }

    /** WV-1.5-atom, WV-1.5-table-*: an atomic condition that reads an open value is undetermined, and a composite
     *  follows Kleene's strong table: the nine rows of all, at least one and none over two members, the three rows of
     *  "niet A", and at least two, at most one and exactly one over three members with the third undetermined. */
    @Test void wv_1_5_composite_tables() throws Exception {
        assertEquals(9, play("composite-tables", "case"));
    }

    /** WV-1.5-past-undetermined-deposit, WV-1.5-past-undetermined-fault: a member read after one undetermined for
     *  another is read provisionally: its truth counts, its deposit is dropped and a fault leaves it undetermined; a
     *  member read after members that all stand is read as ch. 5.10 reads it. */
    @Test void wv_1_5_past_undetermined() throws Exception {
        assertEquals(3, play("past-undetermined-deposit", "case"));
        assertEquals(3, play("past-undetermined-fault", "case"));
        assertEquals(1, play("past-undetermined-fault", "red"));
    }

    /** WV-1.5-read-for-another: a leeg test over an open value is true for the form itself and undetermined for
     *  another, so the composite reads on, and the deposit of the member after it is dropped; with every input
     *  member complete the first member stands for both and the read stops there. */
    @Test void wv_1_5_read_for_another() throws Exception {
        assertEquals(1, play("read-for-another", "case"));
        assertEquals(1, play("read-for-another", "closed"));
    }

    /** WV-1.5-filled-test-*, WV-1.5-filled-guard, WV-1.5-filled-test-computed, WV-1.5-own-negation: the test on leeg
     *  and gevuld over an open value or a computed value with an open read is a closure for the form itself and
     *  undetermined for another; a guard of gevuld beside an ordering is no closure; the form's own negation of a
     *  gevuld test closes as a leeg test does; an assertion of gevuld reports over an open value. */
    @Test void wv_1_5_filled_test() throws Exception {
        assertEquals(2, play("filled-test", "case"));
    }

    /** WV-1.5-rule-*: a rule decides, fills nothing where its condition fails for another, and leaves its target
     *  open where its condition is undetermined or its value has an open read. */
    @Test void wv_1_5_rule() throws Exception {
        assertEquals(5, play("rule", "case"));
    }

    /** WV-1.5-initialisation: an initialisation closes over a writer that leaves its target open, and with a
     *  condition of its own that is undetermined it leaves the target open. */
    @Test void wv_1_5_initialisation() throws Exception {
        assertEquals(2, play("initialisation", "case"));
        assertEquals(2, play("initialisation-condition", "case"));
    }

    /** WV-1.5-consistency: a consistency reports where its assertion fails for the form itself, and not where it is
     *  undetermined. */
    @Test void wv_1_5_consistency() throws Exception {
        assertEquals(3, play("consistency", "case"));
    }

    /** WV-1.5-link-*: a read over an absent link is settled leeg and a navigated check over it stands false; over a
     *  present link an open member of the object it reaches is an open read; a role check reads no value and
     *  stands. */
    @Test void wv_1_5_link() throws Exception {
        assertEquals(1, play("link", "case"));
    }

    /** WV-1.5-outside-the-window: a rule outside its window of validity writes its target and fills nothing there, and so
     *  does a defaults group. */
    @Test void wv_1_5_outside_the_window() throws Exception {
        assertEquals(2, play("outside-the-window", "case"));
        assertEquals(2, play("stack-outside-the-window", "case"));
    }

    /** WV-1.5-stack-first-decides: the first default that decides decides the target; a lower one decides only where it
     *  stands of every higher one that it gives way, and at the first default that is open, by its justification or, as
     *  the unconditional base, by its value, the target is open. */
    @Test void wv_1_5_stack_first_decides() throws Exception {
        assertEquals(5, play("stack-first-decides", "case"));
    }

    /** WV-1.5-stack-gives-way, WV-1.5-stack-settled-leeg: a default whose justification holds with a settled leeg value
     *  gives way, with an open value it is open; where every default gives way the target is settled leeg, or its input
     *  stands, which does not stand beside an open default. */
    @Test void wv_1_5_stack_gives_way() throws Exception {
        assertEquals(6, play("stack-gives-way", "case"));
    }

    /** WV-1.5-stack-initialisation: the initialisation of a stack decides where nothing above it decides, also where a
     *  default above it is open; an input of the target stands above it, and with an open value the target is open. In a
     *  stack with an inherited initialisation above its own (ch. 7.10) each closes so, the lower one also where the upper
     *  one gives way or is open (WV-1.5-stack-two-initialisations), and so does one below exception
     *  groups, where it decides beside a group that is open and not beside one that conflicts. */
    @Test void wv_1_5_stack_initialisation() throws Exception {
        assertEquals(6, play("stack-initialisation", "case"));
        assertEquals(2, play("stack-initialisation-value", "case"));
        assertEquals(6, play("stack-two-initialisations", "case"));
        assertEquals(5, play("stack-exception-groups-initialisation", "case"));
    }

    /** WV-1.5-stack-opgave: an opgave the case gives no value is open and the computation below it does not decide;
     *  declared complete it gives way. */
    @Test void wv_1_5_stack_opgave() throws Exception {
        assertEquals(2, play("stack-opgave", "case"));
        assertEquals(2, play("stack-opgave-complete", "case"));
    }

    /** WV-1.5-stack-characteristic: a stack on a characteristic attributes, does not attribute, or leaves it open; a
     *  characteristic the case names holds beside an open default. */
    @Test void wv_1_5_stack_characteristic() throws Exception {
        assertEquals(6, play("stack-characteristic", "case"));
    }

    /** WV-1.5-stack-exception-groups: the exception groups of a default, reconciled: beside an open member the group is
     *  open, unless two other members decide differently; nested below an exception, whose own decision is the fall-back
     *  where no member settles. */
    @Test void wv_1_5_stack_exception_groups() throws Exception {
        assertEquals(5, play("stack-exception-groups", "case"));
        assertEquals(7, play("stack-exception-groups-nested", "case"));
    }

    /** WV-1.5-stack-unordered: a declared-unordered group reconciles as exception groups do. */
    @Test void wv_1_5_stack_unordered() throws Exception {
        assertEquals(4, play("stack-unordered", "case"));
    }

    /** WV-1.5-stack-type-chain: the effective stack of a subtype reads as any stack. */
    @Test void wv_1_5_stack_type_chain() throws Exception {
        assertEquals(1, play("stack-type-chain", "case"));
    }

    /** "A stack": what the justification of a default deposits arises where the default decides, and not where it is open. */
    @Test void the_deposit_of_a_default() throws Exception {
        assertEquals(4, play("stack-deposit", "case"));
    }

    /** WV-1.5-set-fold*: a sum, a minimum, a maximum, a listed fold and a count under a filter over members that can be
     *  open, the members of a derived member among them: an open member makes the fold open, a count reads no value, a
     *  settled leeg member is skipped, and over no member the fold is settled leeg or its fallback. */
    @Test void wv_1_5_set_fold() throws Exception {
        assertEquals(5, play("set-fold", "case"));
    }

    /** WV-1.5-set-fold with a fold in the filter: a closure in the inner one (a filled-value count), an open value of it, its
     *  own filter, two levels, and a count over a union of such selections. */
    @Test void a_fold_in_a_filter() throws Exception {
        assertEquals(4, play("fold-in-a-filter", "case"));
    }

    /** WV-1.5-closure-in-a-value: a filled-value count and a filter on gevuld are closures: they decide for the form
     *  itself, a comparison of one that is false leaves its target open, and so does a value that is leeg after one, with
     *  what it deposited. */
    @Test void wv_1_5_closure_in_a_value() throws Exception {
        assertEquals(5, play("closure-in-a-value", "case"));
    }

    /** WV-1.5-closure-before-a-barrier and WV-1.5-reached, over a fold: the barrier of a Let over a binder that is leeg
     *  after a closure leaves the target open, and a closure the evaluation does not reach, in the divisor of a leeg
     *  dividend or an unread binder, does not count. */
    @Test void wv_1_5_fold_in_a_binder() throws Exception {
        assertEquals(7, play("fold-in-a-binder", "case"));
    }

    /** WV-1.5-quantifier-fold-*, without a filter or under one that stands: each quantifier by Kleene's table, over no
     *  member, over a matrix that closes, in a composite and in a consistency rule. */
    @Test void wv_1_5_quantifier_fold() throws Exception {
        assertEquals(9, play("quantifier-fold", "case"));
    }

    /** WV-1.5-quantifier-fold-*, the keurder, under a filter that can be open, also of a derived member: each quantifier by
     *  Kleene's table over filter and matrix, over no member, over a matrix that closes, with the member whose filter is open
     *  before and after the others. */
    @Test void wv_1_5_quantifier_fold_filter() throws Exception {
        assertEquals(11, play("quantifier-fold-filter", "case"));
    }

    /** WV-1.5-past-undetermined-filter: every filter is read before a matrix, so a filter that is open, also that of a later
     *  member, leaves a deposit of every matrix out; a filter that deposits does so beside another that is open. */
    @Test void wv_1_5_past_undetermined_filter() throws Exception {
        assertEquals(8, play("past-undetermined-filter", "case"));
    }

    /** WV-1.5-past-undetermined-filter over a fault: a matrix that faults is undetermined past a filter that is open and faults
     *  where every filter stands, and one that the verdict does not wait for, over too few members, is not read; a filter that
     *  faults does so past one that is open, the first of the members in order. */
    @Test void wv_1_5_past_undetermined_filter_over_a_fault() throws Exception {
        assertEquals(4, play("quantifier-fold-matrix-faults", "case"));
        assertEquals(1, play("quantifier-fold-matrix-faults", "red"));
        assertEquals(3, play("quantifier-fold-filter-faults", "case"));
        assertEquals(4, play("quantifier-fold-filter-faults", "red"));
    }

    /** A member that two routes reach is one member of a quantifier fold under a filter that can be open, in a build without
     *  readings and, where it has a place per route, with them ({@code OpenReadingsWitnessTest}); a filter that deposits. */
    @Test void a_quantifier_fold_over_two_routes() throws Exception {
        assertEquals(5, play("quantifier-fold-two-routes", "case"));
    }

    /** An anchored read of input of the form's subject that can be open (IR-SPEC ch. 4.1a): in a filter, in a member of it that
     *  no member of the range reaches, in a test on leeg, in the matrix and in the filter of a fold in a filter; an anchored
     *  characteristic check. */
    @Test void an_anchored_read_of_an_open_input() throws Exception {
        assertEquals(8, play("anchored-read", "case"));
    }

    /** WV-1.5-open-passes-on, over folds: over a derived member whose writer stands after the fold, over a fold, over a
     *  path of two steps, in a quantifier fold, over an instance-set union, and in the justification and the base of a
     *  stack. */
    @Test void wv_1_5_fold_chain() throws Exception {
        assertEquals(5, play("fold-chain", "case"));
    }

    /** WV-1.5-input-per-object in a fold: over a member that is input on the objects of one type and derived on those of
     *  its subtype; an anchored read of a derived member in a filter; a fold in an initialisation and under a "verminderd
     *  met"; a fold nested in the matrix of a quantifier fold. */
    @Test void wv_1_5_fold_subtype() throws Exception {
        assertEquals(6, play("fold-subtype", "case"));
    }

    /** WV-1.5-past-undetermined-fold, and the same in the filter of a fold over a set: a member of a quantifier fold read past
     *  one undetermined decides it, its deposit dropped; a fold over a set that an undetermined filter or an open value breaks
     *  off deposits nothing of its filters. */
    @Test void wv_1_5_past_undetermined_fold() throws Exception {
        assertEquals(7, play("past-undetermined-fold", "case"));
    }

    /** A filter and a matrix over a parameter (EF-014) and over the evaluation date (EF-011): with one bound the fold is as
     *  over a literal; without one the first member in fold order that reads it faults, and one that is undetermined before
     *  it breaks the fold off, or leaves the member after it undetermined in a quantifier fold. */
    @Test void a_fold_over_what_a_session_may_not_bind() throws Exception {
        assertEquals(6, play("fold-over-a-parameter", "case"));
        assertEquals(2, play("fold-over-a-parameter", "red"));
        assertEquals(4, play("fold-over-the-date", "case"));
        assertEquals(2, play("fold-over-the-date", "red"));
    }

    /** The filters of every member of a fold over a set are read before a value: a filter that faults on a later member
     *  faults where the value of an earlier one is open. */
    @Test void the_filters_of_a_fold_come_before_its_values() throws Exception {
        assertEquals(5, play("fold-value-after-filters", "case"));
        assertEquals(1, play("fold-value-after-filters", "red"));
    }

    /** The filters of a union are read selection by selection: one that is undetermined in the first comes before a fault of
     *  the second, and a fault of an earlier member of the second before one undetermined after it. */
    @Test void the_filters_of_a_union_are_read_per_selection() throws Exception {
        assertEquals(5, play("fold-over-a-union", "case"));
        assertEquals(2, play("fold-over-a-union", "red"));
    }

    /** The first date of a set under "all of" a negated derived characteristic of the member and a date calculation over an
     *  anchored derived date of the subject, which can be open and can fault (EF-007), as the consequence of one default and
     *  in the justification of another of the same stack. */
    @Test void a_fold_over_dates_in_a_stack() throws Exception {
        assertEquals(16, play("fold-in-a-date-stack", "case"));
        assertEquals(3, play("fold-in-a-date-stack", "red"));
    }

    /** A value a consumer puts on a bean between two fires fills an input that is not complete, and one it takes away
     *  leaves it open again: a read takes the bean beside the {@code OpenInput} that {@code insert} stated once, also of
     *  a bean a path reaches and of the form's subject in an anchored read. */
    @Test void a_value_on_the_bean_between_two_fires() throws Exception {
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            between("open-input", "case1-vindloon-open.json", branch, "V1", "uitbetaling", "V1", "setVindloon", 4,
                    "open", "7");
            between("link", "case1-links.json", branch, "V2", "vindpremie", "J1", "setBonus", 20, "open", "120");
            between("stack-first-decides", "case5-basis-open.json", branch, "V1", "vindloon", "V1", "setGewicht", 50, "open",
                    "100");
            between("set-fold", "case2-gewicht-open.json", branch, "J1", "totaalgewicht", "V2", "setGewicht", 5, "open", "25");
            between("fold-in-a-filter", "case2-score-open.json", branch, "J1", "gewicht ksom", "K2", "setScore", 7, "open", "3");
            between("fold-in-a-filter", "case2-score-open.json", branch, "J1", "gewicht kgevuld", "K2", "setScore", 7, "open", "3");
            between("set-fold", "case2-gewicht-open.json", branch, "J1", "zwaargewicht", "V2", "setGewicht", 5, "open", "20");
            between("anchored-read", "case2-drempel-open.json", branch, "J1", "merktal", "J1", "setDrempel", 10, "open", "1");
            between("anchored-read", "case2-drempel-open.json", branch, "J1", "drukgewicht", "J1", "setDrempel", 10, "open", "50");
            between("closure-in-a-value", "case2-alle-open.json", branch, "J1", "gewogen totaal", "V2", "setGewicht", 5, "open",
                    "5");
        }
    }

    /** Plays case {@code name} of {@code fixture} on {@code branch}: attribute {@code attribute} of the object {@code of}
     *  is {@code without}, {@code with} after a fire on the number {@code value} that {@code setter} puts on the bean
     *  {@code at}, and {@code without} again after one on leeg. */
    private static void between(String fixture, String name, DroolsBackend.Branch branch, String of, String attribute,
                                String at, String setter, int value, String without, String with) throws Exception {
        Path dir = FIXTURES.resolve(fixture);
        JsonNode json = JSON.readTree(Files.readString(dir.resolve(name)));
        CaseRunner.forMeasurement(dir.resolve("ir.json"), branch).withSession(json, (build, session, labels) -> {
            Object bean = labels.get(at);
            assertEquals(without, fired(build, session, labels.get(of), attribute), branch::name);
            CaseRunner.call(bean, setter, java.math.BigDecimal.class, java.math.BigDecimal.valueOf(value));
            session.update(session.getFactHandle(bean), bean);
            assertEquals(with, fired(build, session, labels.get(of), attribute), branch::name);
            CaseRunner.call(bean, setter, java.math.BigDecimal.class, null);
            session.update(session.getFactHandle(bean), bean);
            assertEquals(without, fired(build, session, labels.get(of), attribute), branch::name);
            return null;
        });
    }

    /** Attribute {@code attribute} of {@code object} after a fire of {@code session}: its value, or open. */
    private static String fired(DroolsBackend.Build build, org.kie.api.runtime.KieSession session, Object object,
                                String attribute) {
        assertNull(DroolsBackend.fire(build, session).noFixpoint());
        dev.uchar.regelspraak.transpiler.tms.Fixpoint fp = DroolsBackend.fixpointOf(build, session);
        List<String> out = new ArrayList<>();
        for (var v : fp.values()) if (v.object() == object && v.attribute().equals(attribute)) out.add(String.valueOf(v.value()));
        for (var o : fp.open()) if (o.object() == object && o.member().equals(attribute)) out.add("open");
        return String.join(", ", out);
    }

    /** Who decided a stack with an open default ({@link DroolsBackend#decisionsOf}): no default where the target is open,
     *  the initialisation where it closes over one, in either form the default that decided where none is open, and the
     *  conflict of two members beside an open one. */
    @Test void the_decisions_of_a_stack_with_an_open_default() throws Exception {
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            assertEquals("none", decision("stack-first-decides", "case3-betwist-open.json", branch));
            assertEquals("/rules/prioritizedDefaults/0/defaults/0 0",
                    decision("stack-first-decides", "case4-betwist-gewicht-open.json", branch));
            assertEquals("/rules/prioritizedDefaults/0/defaults/2 5",
                    decision("stack-initialisation", "case1-onder-een-open-uitzondering.json", branch));
            assertEquals("none", decision("stack-initialisation", "case5-onder-de-invoer.json", branch));
            assertEquals("/rules/prioritizedDefaults/0/defaults/2 4", decision("stack-exception-groups-initialisation",
                    "case1-schatting-onder-een-open-groep.json", branch));
            assertEquals("none", decision("stack-exception-groups-initialisation",
                    "case2-schatting-open-onder-een-open-groep.json", branch));
            assertEquals("none", decision("stack-exception-groups", "case4-betwist-naast-open.json", branch));
            assertEquals("/rules/prioritizedDefaults/0/defaults/0/exceptions/members/0/defaults/0 0",
                    decision("stack-exception-groups", "case2-betwist.json", branch));
            assertEquals("conflict Vondst.vindloon/0",
                    decision("stack-exception-groups-nested", "case6-botsing-naast-open.json", branch));
            assertEquals("none", decision("stack-exception-groups-nested", "case7-twee-eens-naast-open.json", branch));
            assertEquals("none", decision("stack-unordered", "case2-betwist-naast-open.json", branch));
        }
    }

    /** The decision of the one stack of {@code fixture} on the one object of case {@code name}: its decider, by pointer and
     *  decision, its conflict, or none. */
    private static String decision(String fixture, String name, DroolsBackend.Branch branch) throws Exception {
        Path dir = FIXTURES.resolve(fixture);
        JsonNode json = JSON.readTree(Files.readString(dir.resolve(name)));
        return CaseRunner.forMeasurement(dir.resolve("ir.json"), branch).withSession(json, (build, session, labels) -> {
            assertNull(DroolsBackend.fire(build, session).noFixpoint());
            List<dev.uchar.regelspraak.transpiler.tms.DefaultsDecision> all = DroolsBackend.decisionsOf(build, session);
            assertEquals(1, all.size(), () -> fixture + "/" + name);
            var d = all.get(0);
            return d.decider() != null ? d.decider().pointer() + " " + d.decider().decision()
                    : d.conflict() != null ? "conflict " + d.conflict().key() : "none";
        });
    }

    /** Every fixture is played by a test above, and every case file of it under a prefix the tests play. */
    @Test void every_fixture_is_played() throws Exception {
        List<String> names = new ArrayList<>();
        try (Stream<Path> s = Files.list(FIXTURES)) {
            s.filter(Files::isDirectory).forEach(p -> names.add(p.getFileName().toString()));
        }
        names.sort(null);
        assertEquals(List.of("anchored-read", "boundary-after", "boundary-before", "closure-in-a-value", "complete", "complete-derived",
                "composite-tables", "consistency", "derived", "filled-test", "fold-chain", "fold-in-a-binder",
                "fold-in-a-date-stack", "fold-in-a-filter", "fold-over-a-parameter", "fold-over-a-union", "fold-over-the-date", "fold-subtype",
                "fold-value-after-filters", "initialisation", "initialisation-condition", "input-per-object", "instance-ordered", "link",
                "no-open-without-the-property", "open-input", "outside-the-window", "past-undetermined-deposit",
                "past-undetermined-fault", "past-undetermined-filter", "past-undetermined-fold", "quantifier-fold",
                "quantifier-fold-filter", "quantifier-fold-filter-faults", "quantifier-fold-matrix-faults",
                "quantifier-fold-two-routes", "reach", "reach-without-the-property", "read-for-another", "rule",
                "set-fold", "stack-characteristic", "stack-deposit", "stack-exception-groups",
                "stack-exception-groups-initialisation", "stack-exception-groups-nested",
                "stack-first-decides", "stack-gives-way", "stack-initialisation", "stack-initialisation-value",
                "stack-opgave", "stack-opgave-complete", "stack-outside-the-window", "stack-two-initialisations",
                "stack-type-chain", "stack-unordered", "two-writers"), names);
        for (String name : names)
            try (Stream<Path> s = Files.list(FIXTURES.resolve(name))) {
                s.map(p -> p.getFileName().toString()).filter(f -> f.endsWith(".json") && !f.equals("ir.json"))
                        .forEach(f -> assertEquals(true, f.startsWith("case") || f.startsWith("closed")
                                || f.startsWith("red"), () -> name + "/" + f));
            }
    }
}
