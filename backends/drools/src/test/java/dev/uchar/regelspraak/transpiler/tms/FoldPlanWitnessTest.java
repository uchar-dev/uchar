package dev.uchar.regelspraak.transpiler.tms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.uchar.regelspraak.cases.CaseRunner;
import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.transpiler.tms.plan.Accumulation;
import dev.uchar.regelspraak.transpiler.tms.plan.Alpha;
import dev.uchar.regelspraak.transpiler.tms.plan.Exists;
import dev.uchar.regelspraak.transpiler.tms.plan.BetaTest;
import dev.uchar.regelspraak.transpiler.tms.plan.Binding;
import dev.uchar.regelspraak.transpiler.tms.plan.Consequence;
import dev.uchar.regelspraak.transpiler.tms.plan.Join;
import dev.uchar.regelspraak.transpiler.tms.plan.ReadTable;
import dev.uchar.regelspraak.transpiler.tms.plan.Read;
import dev.uchar.regelspraak.transpiler.tms.plan.LhsItem;
import dev.uchar.regelspraak.transpiler.tms.plan.Not;
import dev.uchar.regelspraak.transpiler.tms.plan.Subject;
import dev.uchar.regelspraak.transpiler.tms.plan.FactPattern;
import dev.uchar.regelspraak.transpiler.tms.plan.RulePlan;
import dev.uchar.regelspraak.transpiler.tms.plan.Term;
import dev.uchar.regelspraak.transpiler.tms.plan.TestItem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.kie.api.runtime.KieSession;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rules of a fold come whole from a plan (card drl-teksttak-op-truth-maintenance, step 1c-B2): the contribution
 * rule of a fold and of a quantifier fold, and beside it the rules of a filter that deposits, of a filter that faults
 * and of a member whose timeline the case gives as the asOfDate-bucket scalar. Each is a {@link RulePlan} whose
 * left-hand side holds what the form built itself before: the pattern of the fold's subject before the member's, the
 * join of the range on the member's pattern or the meeting of the two walks of a range of more steps, the existence of
 * a link of a relation a form creates or of the intermediates of a range of more steps, with its scope, the
 * intermediates bound per route and the accumulates of the members' places; and whose consequence carries every
 * decision as a field: what the member contributes ({@link Consequence.Input}), the member's place in the fold's order
 * ({@link Consequence.FoldPlace}), the probe, and what the routes of the plan compute. This witness pins those plans as
 * text, per plan in order: the name, the form and the group; the slots, each with its kind, its variable and its key;
 * every item of the left-hand side in order with its kind, its pattern class, its constraint ids, each field test, its
 * joins, its constraints between two objects with their function and each of their constants with its value, the class
 * of its value, its IR type and its source fragment, its bindings, and of an existence the variables it declares and
 * those it reads from outside; and the kind of the consequence with every field. A term, the
 * test of a rule or the matrix or the filter of a consequence, is pinned as a tree and on the lines after it node by
 * node with its source fragment and of a constant the class of its value and its whole IR type
 * ({@link TermTree#fields}).
 *
 * <ul>
 *   <li>{@code jutterswet-postwacht}: a count and a sum, one of them over a relation a form creates, whose range is
 *       the existence of a link.</li>
 *   <li>{@code jutterswet-keurlast}: a total over time per member, beside the rule of a member whose timeline the case
 *       gives as the asOfDate-bucket scalar, which binds the member's place in its list.</li>
 *   <li>{@code jutterswet-filterwacht}: a count whose filter divides, beside the rules of its filter's deposit and of
 *       its filter's fault, with the member's place.</li>
 *   <li>{@code jutterswet-vondstketen}: a range of more steps whose two walks meet, a constraint on the member's
 *       pattern.</li>
 *   <li>{@link #havenketen}, since no golden has one: a range of three steps with two intermediates, quantified
 *       together after the member's pattern in the contribution rule and bound per route in the rule of the filter's
 *       fault, whose place of the first step is that of an intermediate. Fired as well, against the values the
 *       reference interpreter and the ASP backend give, and with a case whose filter faults along every route, where
 *       each route's {@link FilterFault} carries its own place and the rule names the first route's fault.</li>
 *   <li>The document of {@code TmsQuantifierFoldWitnessTest} whose matrix divides ({@link #matrixThatDivides}), since no
 *       golden has one: a quantifier fold whose matrix can fault and deposit, so a member contributes its test, with
 *       its place and the probe.</li>
 * </ul>
 *
 * <p>That the rendering of these plans builds the rules of before the step is what the harness {@code FireTrace}
 * shows, and the executable model of the corpus, which is equal as text; for {@link #havenketen}, outside the corpus
 * and the suite, the executable model of before and after the step is equal as text as well.
 *
 * <p><b>Red</b> before the step: these forms built their rules themselves, so there was no plan of them, and this class
 * did not compile, since the plan had no consequence of a fold, no constraint on the subject pattern between two
 * objects, no constants of such a constraint and no scope of an existence; with only the names of the plans pinned,
 * none of the six documents had a plan of any of these rules.
 *
 * <p>The fix round of step 1c-B2 added the case of {@link #havenketen} whose filter faults, red under a mutation of the
 * place reader, pinned the constants of a constraint between two objects with every field, and made the scope of an
 * existence cover what its patterns bind ({@link #the_scope_of_an_exists_includes_the_bindings_of_its_patterns}).
 * The fix round of step 1c-C1 let the rendering forget such a binding entirely after its quantification, so that a
 * second quantification may bind the same name ({@link #two_quantifications_each_bind_the_same_local_name}).
 */
class FoldPlanWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void a_count_and_a_sum_over_a_link() throws Exception {
        assertEquals(List.of(
                "rule seinaantal van een post: bijdrage aan vouw 0 | form seinaantal van een post |"
                        + " group null",
                "  readings [0 SUBJECT $onderwerp (onderwerp zelf)]",
                "  pattern Post $vouwonderwerp alpha [] join [] beta [] bind []",
                "  subject Sein $onderwerp alpha [] join [bereik_1: post = $vouwonderwerp] beta [] bind"
                        + " []",
                "  consequence Contribution [subject=$vouwonderwerp, fold=seinaantal van een post: vouw 0,"
                        + " member=0, slots=[0], size=1, insertion=LOGICAL]",
                "    input Count",
                "rule seingewicht van een post: bijdrage aan vouw 1 | form seingewicht van een post |"
                        + " group null",
                "  readings [0 SUBJECT $onderwerp (onderwerp zelf), 1 INPUT $gewicht (eigen"
                        + " ElementKey[objectType=sein, name=gewicht, characteristic=false])]",
                "  pattern Post $vouwonderwerp alpha [] join [] beta [] bind []",
                "  subject Sein $onderwerp alpha [] join [bereik_6: post = $vouwonderwerp] beta [] bind [$gewicht"
                        + " = [] Sein.gewicht VALUE]",
                "  consequence Contribution [subject=$vouwonderwerp, fold=seingewicht van een post: vouw 1,"
                        + " member=0, slots=[0, 1], size=2, insertion=LOGICAL]",
                "    input MemberValue [value=1, total=null, filled=false]",
                "rule vakseinaantal van een kustvak: bijdrage aan vouw 2 | form vakseinaantal van een"
                        + " kustvak | group null",
                "  readings [0 SUBJECT $onderwerp (onderwerp zelf)]",
                "  pattern Kustvak $vouwonderwerp alpha [] join [] beta [] bind []",
                "  subject Sein $onderwerp alpha [] join [] beta [] bind []",
                "  exists inside [$koppeling_bereik_13] outside [$onderwerp, $vouwonderwerp] [RelationLink"
                        + " $koppeling_bereik_13 alpha [bereik-koppeling_14: [[] relation VALUE GELIJK"
                        + " vakbeseining]] join [bereik-lid_15: second = $onderwerp, bereik-drager_16: first ="
                        + " $vouwonderwerp] beta [] bind []]",
                "  consequence Contribution [subject=$vouwonderwerp, fold=vakseinaantal van een kustvak: vouw"
                        + " 2, member=0, slots=[0], size=1, insertion=LOGICAL]",
                "    input Count"),
                plans(golden("jutterswet-postwacht")));
    }

    @Test void a_timeline_total_with_its_member_fault() throws Exception {
        assertEquals(List.of(
                "rule keurlast van een jutter: bijdrage aan vouw 0 | form keurlast van een jutter |"
                        + " group null",
                "  readings [0 INPUT $gekeurd (eigen ElementKey[objectType=vondst, name=gekeurd,"
                        + " characteristic=true]), 1 SUBJECT $onderwerp (onderwerp zelf), 2 TIJDLIJN $tijdlijn_2"
                        + " (tijdlijn ElementKey[objectType=vondst, name=keurtarief, characteristic=false])]",
                "  pattern Jutter $vouwonderwerp alpha [] join [] beta [] bind []",
                "  subject Vondst $onderwerp alpha [] join [bereik_0: keurende jutter = $vouwonderwerp] beta []"
                        + " bind [$gekeurd = [] Vondst.gekeurd WAAR]",
                "  pattern State $stand_2 alpha [stand_1: [[] attribute VALUE GELIJK"
                        + " ElementKey[objectType=vondst, name=keurtarief, characteristic=false]]] join"
                        + " [stand-onderwerp_2: subject = $onderwerp] beta [] bind [$tijdlijn_2 = []"
                        + " State.value VALUE]",
                "  test voorwaarde_3 CONDITION probe -1 slots [0] size 3: KENMERK(Read 0, Const"
                        + " true)",
                "    test 0 Call source 'gekeurd zijn'",
                "    test 0.0 SlotValue",
                "    test 0.1 Const Boolean",
                "  consequence Contribution [subject=$vouwonderwerp, fold=keurlast van een jutter: vouw 0,"
                        + " member=1, slots=[1, 2], size=3, insertion=LOGICAL]",
                "    input MemberValue [value=2, total=MAAND, filled=false]",
                "rule keurlast van een jutter: lidfout in vouw 0 | form keurlast van een jutter | group"
                        + " null",
                "  readings [0 LIST_PLACES $lijstplaatsen_plaats_7 (lijstplaatsen 0), 1 LIST_PLACE"
                        + " $plaats_plaats_8 (lijstplaats 0), 2 INPUT $gekeurd (eigen"
                        + " ElementKey[objectType=vondst, name=gekeurd, characteristic=true]), 3 TIJDLIJN $tijdlijn_3"
                        + " (tijdlijn ElementKey[objectType=vondst, name=keurtarief, characteristic=false]), 4"
                        + " SUBJECT $onderwerp (onderwerp zelf)]",
                "  pattern Jutter $vouwonderwerp alpha [] join [] beta [] bind []",
                "  subject Vondst $onderwerp alpha [] join [bereik_4: keurende jutter = $vouwonderwerp] beta []"
                        + " bind [$gekeurd = [] Vondst.gekeurd WAAR]",
                "  accumulation ListPlace $lijstplaats_plaats_5 alpha [plaats-veld_9: [[] field VALUE"
                        + " GELIJK keurendeJutter]] join [] beta [plaats-lid_10: PLACE_OF $onderwerp [] {List<>,"
                        + " type null, source null}] bind [$lijstplaatswaarde_plaats_6 = [] ListPlace.place"
                        + " VALUE] functions [COUNT null -> $lijstplaatsen_plaats_7, MINIMUM"
                        + " $lijstplaatswaarde_plaats_6 -> $plaats_plaats_8]",
                "  pattern State $stand_3 alpha [stand_11: [[] attribute VALUE GELIJK"
                        + " ElementKey[objectType=vondst, name=keurtarief, characteristic=false]]] join"
                        + " [stand-onderwerp_12: subject = $onderwerp] beta [] bind [$tijdlijn_3 = []"
                        + " State.value VALUE]",
                "  test voorwaarde_13 VORM probe -1 slots [2, 3] size 5: Compound ALLE"
                        + " 0(KENMERK(Read 2, Const true), BUCKET_SCALAR(Read 3, Const true))",
                "    test 0 Compound",
                "    test 0.0 Call source 'gekeurd zijn'",
                "    test 0.0.0 SlotValue",
                "    test 0.0.1 Const Boolean",
                "    test 0.1 Call",
                "    test 0.1.0 SlotValue",
                "    test 0.1.1 Const Boolean",
                "  consequence MemberFault [subject=$vouwonderwerp, fold=keurlast van een jutter: vouw 0,"
                        + " member=4, place=selection 0 [keurendeJutter 0 1] MEMBER_FAULT, before=the per-element temporal"
                        + " total of \"keurtarief\" over , after=: the case supplies an asOfDate-bucket scalar, which"
                        + " carries no period of its own; a set fold needs each member's own timeline (ch."
                        + " 3.6/§7.1, ch. 8.4), insertion=LOGICAL]"),
                plans(golden("jutterswet-keurlast")));
    }

    @Test void a_filter_that_deposits_and_fails() throws Exception {
        assertEquals(List.of(
                "rule vondstental: bijdrage aan vouw 0 | form vondstental | group null",
                "  readings [0 SUBJECT $onderwerp (onderwerp zelf)]",
                "  pattern Jutter $vouwonderwerp alpha [] join [] beta [] bind []",
                "  subject Vondst $onderwerp alpha [] join [bereik_0: aanmelder = $vouwonderwerp] beta"
                        + " [] bind []",
                "  consequence Contribution [subject=$vouwonderwerp, fold=vondstental: vouw 0, member=0,"
                        + " slots=[0], size=1, insertion=LOGICAL]",
                "    input Count",
                "rule zware vondsten: bijdrage aan vouw 1 | form zware vondsten | group null",
                "  readings [0 INPUT $gewicht (eigen ElementKey[objectType=vondst, name=gewicht,"
                        + " characteristic=false]), 1 INPUT $deelnemers (eigen ElementKey[objectType=vondst,"
                        + " name=deelnemers, characteristic=false]), 2 DEPOSIT_FLAG null (null), 3 SUBJECT $onderwerp"
                        + " (onderwerp zelf)]",
                "  pattern Jutter $vouwonderwerp alpha [] join [] beta [] bind []",
                "  subject Vondst $onderwerp alpha [] join [bereik_5: aanmelder = $vouwonderwerp] beta [] bind"
                        + " [$gewicht = [] Vondst.gewicht VALUE, $deelnemers = [] Vondst.deelnemers VALUE]",
                "  test voorwaarde_6 WITHOUT_FAULT probe -1 slots [0, 1] size 4: Compound DE"
                        + " 0(RS_AGG_COMPARE(Division DIVIDED probe -1 (Read 0, Read 1), Const 2, Const >))",
                "    test 0 Compound source 'aan de volgende voorwaarde voldoen: zijn gewicht gedeeld"
                        + " door zijn deelnemers is groter dan 2'",
                "    test 0.0 Call source 'zijn gewicht gedeeld door zijn deelnemers is groter dan 2'",
                "    test 0.0.0 Division source 'zijn gewicht gedeeld door zijn deelnemers'",
                "    test 0.0.0.0 SlotValue source 'zijn gewicht'",
                "    test 0.0.0.1 SlotValue source 'zijn deelnemers'",
                "    test 0.0.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0,"
                        + " sign=ONBEPAALD, unit=null] source '2'",
                "    test 0.0.2 Const String",
                "  consequence Contribution [subject=$vouwonderwerp, fold=zware vondsten: vouw 1, member=3,"
                        + " slots=[3], size=4, insertion=LOGICAL]",
                "    input Count",
                "rule zware vondsten: filterfout in vouw 1 | form zware vondsten | group null",
                "  readings [0 LIST_PLACES $lijstplaatsen_plaats_10 (lijstplaatsen 0), 1 LIST_PLACE"
                        + " $plaats_plaats_11 (lijstplaats 0), 2 INPUT $gewicht (eigen"
                        + " ElementKey[objectType=vondst, name=gewicht, characteristic=false]), 3 INPUT $deelnemers"
                        + " (eigen ElementKey[objectType=vondst, name=deelnemers, characteristic=false]), 4 DEPOSIT_FLAG null"
                        + " (null), 5 SUBJECT $onderwerp (onderwerp zelf)]",
                "  pattern Jutter $vouwonderwerp alpha [] join [] beta [] bind []",
                "  subject Vondst $onderwerp alpha [] join [bereik_7: aanmelder = $vouwonderwerp] beta [] bind"
                        + " [$gewicht = [] Vondst.gewicht VALUE, $deelnemers = [] Vondst.deelnemers VALUE]",
                "  accumulation ListPlace $lijstplaats_plaats_8 alpha [plaats-veld_12: [[] field VALUE"
                        + " GELIJK aanmelder]] join [] beta [plaats-lid_13: PLACE_OF $onderwerp [] {List<>, type"
                        + " null, source null}] bind [$lijstplaatswaarde_plaats_9 = [] ListPlace.place VALUE]"
                        + " functions [COUNT null -> $lijstplaatsen_plaats_10, MINIMUM $lijstplaatswaarde_plaats_9"
                        + " -> $plaats_plaats_11]",
                "  test voorwaarde_14 FAULT probe -1 slots [2, 3] size 6: Compound DE"
                        + " 0(RS_AGG_COMPARE(Division DIVIDED probe -1 (Read 2, Read 3), Const 2, Const >))",
                "    test 0 Compound source 'aan de volgende voorwaarde voldoen: zijn gewicht gedeeld"
                        + " door zijn deelnemers is groter dan 2'",
                "    test 0.0 Call source 'zijn gewicht gedeeld door zijn deelnemers is groter dan 2'",
                "    test 0.0.0 Division source 'zijn gewicht gedeeld door zijn deelnemers'",
                "    test 0.0.0.0 SlotValue source 'zijn gewicht'",
                "    test 0.0.0.1 SlotValue source 'zijn deelnemers'",
                "    test 0.0.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0,"
                        + " sign=ONBEPAALD, unit=null] source '2'",
                "    test 0.0.2 Const String",
                "  consequence FilterFault [subject=$vouwonderwerp, fold=zware vondsten: vouw 1, member=5, place=selection"
                        + " 0 [aanmelder 0 1] FILTER_FAULT, slots=[0, 1, 2, 3, 5], size=6, insertion=LOGICAL]"
                        + " filter Compound DE 0(RS_AGG_COMPARE(Division DIVIDED probe -1 (Read 2, Read 3), Const"
                        + " 2, Const >))",
                "    filter 0 Compound source 'aan de volgende voorwaarde voldoen: zijn gewicht"
                        + " gedeeld door zijn deelnemers is groter dan 2'",
                "    filter 0.0 Call source 'zijn gewicht gedeeld door zijn deelnemers is groter dan 2'",
                "    filter 0.0.0 Division source 'zijn gewicht gedeeld door zijn deelnemers'",
                "    filter 0.0.0.0 SlotValue source 'zijn gewicht'",
                "    filter 0.0.0.1 SlotValue source 'zijn deelnemers'",
                "    filter 0.0.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0,"
                        + " sign=ONBEPAALD, unit=null] source '2'",
                "    filter 0.0.2 Const String",
                "rule zware vondsten: filterdeponering in vouw 1 | form zware vondsten | group null",
                "  readings [0 INPUT $gewicht (eigen ElementKey[objectType=vondst, name=gewicht,"
                        + " characteristic=false]), 1 INPUT $deelnemers (eigen ElementKey[objectType=vondst,"
                        + " name=deelnemers, characteristic=false]), 2 DEPOSIT_FLAG null (null), 3 SUBJECT $onderwerp"
                        + " (onderwerp zelf)]",
                "  pattern Jutter $vouwonderwerp alpha [] join [] beta [] bind []",
                "  subject Vondst $onderwerp alpha [] join [bereik_15: aanmelder = $vouwonderwerp] beta [] bind"
                        + " [$gewicht = [] Vondst.gewicht VALUE, $deelnemers = [] Vondst.deelnemers VALUE]",
                "  test voorwaarde_16 DEPOSIT probe 2 slots [0, 1] size 4: Compound DE"
                        + " 0(RS_AGG_COMPARE(Division DIVIDED probe 2 (Read 0, Read 1), Const 2, Const >))",
                "    test 0 Compound source 'aan de volgende voorwaarde voldoen: zijn gewicht gedeeld"
                        + " door zijn deelnemers is groter dan 2'",
                "    test 0.0 Call source 'zijn gewicht gedeeld door zijn deelnemers is groter dan 2'",
                "    test 0.0.0 Division source 'zijn gewicht gedeeld door zijn deelnemers'",
                "    test 0.0.0.0 SlotValue source 'zijn gewicht'",
                "    test 0.0.0.1 SlotValue source 'zijn deelnemers'",
                "    test 0.0.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0,"
                        + " sign=ONBEPAALD, unit=null] source '2'",
                "    test 0.0.2 Const String",
                "  consequence FilterDeposit [subject=$vouwonderwerp, fold=zware vondsten: vouw 1,"
                        + " member=3, insertion=LOGICAL]",
                "rule drukte: bijdrage aan vouw 2 | form drukte | group null",
                "  readings [0 INPUT $gewicht (eigen ElementKey[objectType=vondst, name=gewicht,"
                        + " characteristic=false]), 1 INPUT $deelnemers (eigen ElementKey[objectType=vondst,"
                        + " name=deelnemers, characteristic=false]), 2 DEPOSIT_FLAG null (null), 3 SUBJECT $onderwerp"
                        + " (onderwerp zelf)]",
                "  pattern Jutter $vouwonderwerp alpha [] join [] beta [] bind []",
                "  subject Vondst $onderwerp alpha [] join [bereik_31: aanmelder = $vouwonderwerp] beta [] bind"
                        + " [$gewicht = [] Vondst.gewicht VALUE, $deelnemers = [] Vondst.deelnemers VALUE]",
                "  test voorwaarde_32 WITHOUT_FAULT probe -1 slots [0, 1] size 4: Compound DE"
                        + " 0(RS_AGG_COMPARE(Division DIVIDED probe -1 (Read 0, Read 1), Const 2, Const >))",
                "    test 0 Compound source 'aan de volgende voorwaarde voldoen: zijn gewicht gedeeld"
                        + " door zijn deelnemers is groter dan 2'",
                "    test 0.0 Call source 'zijn gewicht gedeeld door zijn deelnemers is groter dan 2'",
                "    test 0.0.0 Division source 'zijn gewicht gedeeld door zijn deelnemers'",
                "    test 0.0.0.0 SlotValue source 'zijn gewicht'",
                "    test 0.0.0.1 SlotValue source 'zijn deelnemers'",
                "    test 0.0.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0,"
                        + " sign=ONBEPAALD, unit=null] source '2'",
                "    test 0.0.2 Const String",
                "  consequence Contribution [subject=$vouwonderwerp, fold=drukte: vouw 2, member=3, slots=[3],"
                        + " size=4, insertion=LOGICAL]",
                "    input Count",
                "rule drukte: filterfout in vouw 2 | form drukte | group null",
                "  readings [0 LIST_PLACES $lijstplaatsen_plaats_36 (lijstplaatsen 0), 1 LIST_PLACE"
                        + " $plaats_plaats_37 (lijstplaats 0), 2 INPUT $gewicht (eigen"
                        + " ElementKey[objectType=vondst, name=gewicht, characteristic=false]), 3 INPUT $deelnemers"
                        + " (eigen ElementKey[objectType=vondst, name=deelnemers, characteristic=false]), 4 DEPOSIT_FLAG null"
                        + " (null), 5 SUBJECT $onderwerp (onderwerp zelf)]",
                "  pattern Jutter $vouwonderwerp alpha [] join [] beta [] bind []",
                "  subject Vondst $onderwerp alpha [] join [bereik_33: aanmelder = $vouwonderwerp] beta [] bind"
                        + " [$gewicht = [] Vondst.gewicht VALUE, $deelnemers = [] Vondst.deelnemers VALUE]",
                "  accumulation ListPlace $lijstplaats_plaats_34 alpha [plaats-veld_38: [[] field VALUE"
                        + " GELIJK aanmelder]] join [] beta [plaats-lid_39: PLACE_OF $onderwerp [] {List<>, type"
                        + " null, source null}] bind [$lijstplaatswaarde_plaats_35 = [] ListPlace.place VALUE]"
                        + " functions [COUNT null -> $lijstplaatsen_plaats_36, MINIMUM"
                        + " $lijstplaatswaarde_plaats_35 -> $plaats_plaats_37]",
                "  test voorwaarde_40 FAULT probe -1 slots [2, 3] size 6: Compound DE"
                        + " 0(RS_AGG_COMPARE(Division DIVIDED probe -1 (Read 2, Read 3), Const 2, Const >))",
                "    test 0 Compound source 'aan de volgende voorwaarde voldoen: zijn gewicht gedeeld"
                        + " door zijn deelnemers is groter dan 2'",
                "    test 0.0 Call source 'zijn gewicht gedeeld door zijn deelnemers is groter dan 2'",
                "    test 0.0.0 Division source 'zijn gewicht gedeeld door zijn deelnemers'",
                "    test 0.0.0.0 SlotValue source 'zijn gewicht'",
                "    test 0.0.0.1 SlotValue source 'zijn deelnemers'",
                "    test 0.0.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0,"
                        + " sign=ONBEPAALD, unit=null] source '2'",
                "    test 0.0.2 Const String",
                "  consequence FilterFault [subject=$vouwonderwerp, fold=drukte: vouw 2, member=5, place=selection 0"
                        + " [aanmelder 0 1] FILTER_FAULT, slots=[0, 1, 2, 3, 5], size=6, insertion=LOGICAL] filter"
                        + " Compound DE 0(RS_AGG_COMPARE(Division DIVIDED probe -1 (Read 2, Read 3), Const 2,"
                        + " Const >))",
                "    filter 0 Compound source 'aan de volgende voorwaarde voldoen: zijn gewicht"
                        + " gedeeld door zijn deelnemers is groter dan 2'",
                "    filter 0.0 Call source 'zijn gewicht gedeeld door zijn deelnemers is groter dan 2'",
                "    filter 0.0.0 Division source 'zijn gewicht gedeeld door zijn deelnemers'",
                "    filter 0.0.0.0 SlotValue source 'zijn gewicht'",
                "    filter 0.0.0.1 SlotValue source 'zijn deelnemers'",
                "    filter 0.0.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0,"
                        + " sign=ONBEPAALD, unit=null] source '2'",
                "    filter 0.0.2 Const String",
                "rule drukte: filterdeponering in vouw 2 | form drukte | group null",
                "  readings [0 INPUT $gewicht (eigen ElementKey[objectType=vondst, name=gewicht,"
                        + " characteristic=false]), 1 INPUT $deelnemers (eigen ElementKey[objectType=vondst,"
                        + " name=deelnemers, characteristic=false]), 2 DEPOSIT_FLAG null (null), 3 SUBJECT $onderwerp"
                        + " (onderwerp zelf)]",
                "  pattern Jutter $vouwonderwerp alpha [] join [] beta [] bind []",
                "  subject Vondst $onderwerp alpha [] join [bereik_41: aanmelder = $vouwonderwerp] beta [] bind"
                        + " [$gewicht = [] Vondst.gewicht VALUE, $deelnemers = [] Vondst.deelnemers VALUE]",
                "  test voorwaarde_42 DEPOSIT probe 2 slots [0, 1] size 4: Compound DE"
                        + " 0(RS_AGG_COMPARE(Division DIVIDED probe 2 (Read 0, Read 1), Const 2, Const >))",
                "    test 0 Compound source 'aan de volgende voorwaarde voldoen: zijn gewicht gedeeld"
                        + " door zijn deelnemers is groter dan 2'",
                "    test 0.0 Call source 'zijn gewicht gedeeld door zijn deelnemers is groter dan 2'",
                "    test 0.0.0 Division source 'zijn gewicht gedeeld door zijn deelnemers'",
                "    test 0.0.0.0 SlotValue source 'zijn gewicht'",
                "    test 0.0.0.1 SlotValue source 'zijn deelnemers'",
                "    test 0.0.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0,"
                        + " sign=ONBEPAALD, unit=null] source '2'",
                "    test 0.0.2 Const String",
                "  consequence FilterDeposit [subject=$vouwonderwerp, fold=drukte: vouw 2, member=3,"
                        + " insertion=LOGICAL]"),
                plans(golden("jutterswet-filterwacht")));
    }

    @Test void a_path_whose_walks_meet_each_other() throws Exception {
        assertEquals(List.of(
                "rule laagste vondstgewicht van een jutter: bijdrage aan vouw 0 | form laagste"
                        + " vondstgewicht van een jutter | group null",
                "  readings [0 SUBJECT $onderwerp (onderwerp zelf), 1 INPUT $gewicht (eigen"
                        + " ElementKey[objectType=vondst, name=gewicht, characteristic=false])]",
                "  pattern Jutter $vouwonderwerp alpha [] join [] beta [] bind []",
                "  subject Vondst $onderwerp alpha [] join [] beta [pad_0: MEETING $vouwonderwerp"
                        + " [bergendeBoot, schipper] {List<String>, type null, source null} [] {List<>, type null,"
                        + " source null}] bind [$gewicht = [] Vondst.gewicht VALUE]",
                "  consequence Contribution [subject=$vouwonderwerp, fold=laagste vondstgewicht van een jutter:"
                        + " vouw 0, member=0, slots=[0, 1], size=2, insertion=LOGICAL]",
                "    input MemberValue [value=1, total=null, filled=false]"),
                plans(golden("jutterswet-vondstketen")));
    }

    @Test void a_path_with_two_between_nodes_existing_and_per_route() throws Exception {
        assertEquals(List.of(
                "rule aantal gevulde bergplaatsen: bijdrage aan vouw 0 | form aantal gevulde"
                        + " bergplaatsen | group null",
                "  readings [0 SUBJECT $onderwerp (onderwerp zelf), 1 INPUT $vangstgewicht (eigen"
                        + " ElementKey[objectType=bergplaats, name=vangstgewicht, characteristic=false])]",
                "  pattern Jutter $vouwonderwerp alpha [] join [] beta [] bind []",
                "  subject Bergplaats $onderwerp alpha [] join [] beta [] bind [$vangstgewicht = []"
                        + " Bergplaats.vangstgewicht VALUE]",
                "  exists inside [$tussen_pad_0] outside [$vouwonderwerp, $onderwerp] [Boot"
                        + " $tussen_pad_0 alpha [] join [] beta [pad_1: MEETING $vouwonderwerp [schipper]"
                        + " {List<String>, type null, source null} [] {List<>, type null, source null}, pad_2:"
                        + " MEETING $onderwerp [losplaats] {List<String>, type null, source null} [] {List<>,"
                        + " type null, source null}] bind []]",
                "  consequence Contribution [subject=$vouwonderwerp, fold=aantal gevulde bergplaatsen: vouw 0,"
                        + " member=0, slots=[0, 1], size=2, insertion=LOGICAL]",
                "    input MemberValue [value=1, total=null, filled=true]",
                "rule havengeldtotaal van een Jutter: bijdrage aan vouw 1 | form havengeldtotaal van"
                        + " een Jutter | group null",
                "  readings [0 INPUT $havengeld (eigen ElementKey[objectType=haven, name=havengeld,"
                        + " characteristic=false]), 1 INPUT $ligplaatsen (eigen ElementKey[objectType=haven,"
                        + " name=ligplaatsen, characteristic=false]), 2 DEPOSIT_FLAG null (null), 3 SUBJECT $onderwerp"
                        + " (onderwerp zelf)]",
                "  pattern Jutter $vouwonderwerp alpha [] join [] beta [] bind []",
                "  subject Haven $onderwerp alpha [] join [] beta [] bind [$havengeld = [] Haven.havengeld VALUE,"
                        + " $ligplaatsen = [] Haven.ligplaatsen VALUE]",
                "  exists inside [$tussen_pad_7, $tussen_pad_8] outside [$vouwonderwerp, $onderwerp]"
                        + " [Boot $tussen_pad_7 alpha [] join [] beta [pad_9: MEETING $vouwonderwerp [schipper]"
                        + " {List<String>, type null, source null} [] {List<>, type null, source null}] bind [],"
                        + " Bergplaats $tussen_pad_8 alpha [] join [] beta [pad_10: MEETING $tussen_pad_7 []"
                        + " {List<>, type null, source null} [losplaats] {List<String>, type null, source null},"
                        + " pad_11: MEETING $onderwerp [haven] {List<String>, type null, source null} [] {List<>,"
                        + " type null, source null}] bind []]",
                "  test voorwaarde_12 WITHOUT_FAULT probe -1 slots [0, 1] size 4: Compound DE"
                        + " 0(RS_AGG_COMPARE(Division DIVIDED probe -1 (Read 0, Read 1), Const 2, Const >))",
                "    test 0 Compound source 'aan de volgende voorwaarde voldoen: zijn havengeld"
                        + " gedeeld door zijn ligplaatsen is groter dan 2'",
                "    test 0.0 Call source 'zijn havengeld gedeeld door zijn ligplaatsen is groter dan"
                        + " 2'",
                "    test 0.0.0 Division source 'zijn havengeld gedeeld door zijn ligplaatsen'",
                "    test 0.0.0.0 SlotValue source 'zijn havengeld'",
                "    test 0.0.0.1 SlotValue source 'zijn ligplaatsen'",
                "    test 0.0.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0,"
                        + " sign=ONBEPAALD, unit=null] source '2'",
                "    test 0.0.2 Const String",
                "  consequence Contribution [subject=$vouwonderwerp, fold=havengeldtotaal van een Jutter: vouw"
                        + " 1, member=3, slots=[0, 3], size=4, insertion=LOGICAL]",
                "    input MemberValue [value=0, total=null, filled=false]",
                "rule havengeldtotaal van een Jutter: filterfout in vouw 1 | form havengeldtotaal van"
                        + " een Jutter | group null",
                "  readings [0 LIST_PLACES $lijstplaatsen_plaats_20 (lijstplaatsen 0), 1 LIST_PLACE"
                        + " $plaats_plaats_21 (lijstplaats 0), 2 INPUT $havengeld (eigen"
                        + " ElementKey[objectType=haven, name=havengeld, characteristic=false]), 3 INPUT $ligplaatsen"
                        + " (eigen ElementKey[objectType=haven, name=ligplaatsen, characteristic=false]), 4 DEPOSIT_FLAG null"
                        + " (null), 5 SUBJECT $onderwerp (onderwerp zelf)]",
                "  pattern Jutter $vouwonderwerp alpha [] join [] beta [] bind []",
                "  subject Haven $onderwerp alpha [] join [] beta [] bind [$havengeld = [] Haven.havengeld VALUE,"
                        + " $ligplaatsen = [] Haven.ligplaatsen VALUE]",
                "  pattern Boot $tussen_pad_13 alpha [] join [] beta [pad_15: MEETING $vouwonderwerp"
                        + " [schipper] {List<String>, type null, source null} [] {List<>, type null, source null}]"
                        + " bind []",
                "  pattern Bergplaats $tussen_pad_14 alpha [] join [] beta [pad_16: MEETING"
                        + " $tussen_pad_13 [] {List<>, type null, source null} [losplaats] {List<String>, type null,"
                        + " source null}, pad_17: MEETING $onderwerp [haven] {List<String>, type null, source null}"
                        + " [] {List<>, type null, source null}] bind []",
                "  accumulation ListPlace $lijstplaats_plaats_18 alpha [plaats-veld_22: [[] field VALUE"
                        + " GELIJK schipper]] join [] beta [plaats-lid_23: PLACE_OF $tussen_pad_13 [] {List<>,"
                        + " type null, source null}] bind [$lijstplaatswaarde_plaats_19 = [] ListPlace.place"
                        + " VALUE] functions [COUNT null -> $lijstplaatsen_plaats_20, MINIMUM"
                        + " $lijstplaatswaarde_plaats_19 -> $plaats_plaats_21]",
                "  test voorwaarde_24 FAULT probe -1 slots [2, 3] size 6: Compound DE"
                        + " 0(RS_AGG_COMPARE(Division DIVIDED probe -1 (Read 2, Read 3), Const 2, Const >))",
                "    test 0 Compound source 'aan de volgende voorwaarde voldoen: zijn havengeld"
                        + " gedeeld door zijn ligplaatsen is groter dan 2'",
                "    test 0.0 Call source 'zijn havengeld gedeeld door zijn ligplaatsen is groter dan"
                        + " 2'",
                "    test 0.0.0 Division source 'zijn havengeld gedeeld door zijn ligplaatsen'",
                "    test 0.0.0.0 SlotValue source 'zijn havengeld'",
                "    test 0.0.0.1 SlotValue source 'zijn ligplaatsen'",
                "    test 0.0.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0,"
                        + " sign=ONBEPAALD, unit=null] source '2'",
                "    test 0.0.2 Const String",
                "  consequence FilterFault [subject=$vouwonderwerp, fold=havengeldtotaal van een Jutter: vouw 1, member=5,"
                        + " place=selection 0 [schipper 0 1] FILTER_FAULT, slots=[0, 1, 2, 3, 5], size=6,"
                        + " insertion=LOGICAL] filter Compound DE 0(RS_AGG_COMPARE(Division DIVIDED probe -1"
                        + " (Read 2, Read 3), Const 2, Const >))",
                "    filter 0 Compound source 'aan de volgende voorwaarde voldoen: zijn havengeld"
                        + " gedeeld door zijn ligplaatsen is groter dan 2'",
                "    filter 0.0 Call source 'zijn havengeld gedeeld door zijn ligplaatsen is groter dan"
                        + " 2'",
                "    filter 0.0.0 Division source 'zijn havengeld gedeeld door zijn ligplaatsen'",
                "    filter 0.0.0.0 SlotValue source 'zijn havengeld'",
                "    filter 0.0.0.1 SlotValue source 'zijn ligplaatsen'",
                "    filter 0.0.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0,"
                        + " sign=ONBEPAALD, unit=null] source '2'",
                "    filter 0.0.2 Const String",
                "rule havengeldtotaal van een Jutter: filterdeponering in vouw 1 | form havengeldtotaal"
                        + " van een Jutter | group null",
                "  readings [0 INPUT $havengeld (eigen ElementKey[objectType=haven, name=havengeld,"
                        + " characteristic=false]), 1 INPUT $ligplaatsen (eigen ElementKey[objectType=haven,"
                        + " name=ligplaatsen, characteristic=false]), 2 DEPOSIT_FLAG null (null), 3 SUBJECT $onderwerp"
                        + " (onderwerp zelf)]",
                "  pattern Jutter $vouwonderwerp alpha [] join [] beta [] bind []",
                "  subject Haven $onderwerp alpha [] join [] beta [] bind [$havengeld = [] Haven.havengeld VALUE,"
                        + " $ligplaatsen = [] Haven.ligplaatsen VALUE]",
                "  exists inside [$tussen_pad_25, $tussen_pad_26] outside [$vouwonderwerp, $onderwerp]"
                        + " [Boot $tussen_pad_25 alpha [] join [] beta [pad_27: MEETING $vouwonderwerp"
                        + " [schipper] {List<String>, type null, source null} [] {List<>, type null, source null}]"
                        + " bind [], Bergplaats $tussen_pad_26 alpha [] join [] beta [pad_28: MEETING"
                        + " $tussen_pad_25 [] {List<>, type null, source null} [losplaats] {List<String>, type null,"
                        + " source null}, pad_29: MEETING $onderwerp [haven] {List<String>, type null, source null}"
                        + " [] {List<>, type null, source null}] bind []]",
                "  test voorwaarde_30 DEPOSIT probe 2 slots [0, 1] size 4: Compound DE"
                        + " 0(RS_AGG_COMPARE(Division DIVIDED probe 2 (Read 0, Read 1), Const 2, Const >))",
                "    test 0 Compound source 'aan de volgende voorwaarde voldoen: zijn havengeld"
                        + " gedeeld door zijn ligplaatsen is groter dan 2'",
                "    test 0.0 Call source 'zijn havengeld gedeeld door zijn ligplaatsen is groter dan"
                        + " 2'",
                "    test 0.0.0 Division source 'zijn havengeld gedeeld door zijn ligplaatsen'",
                "    test 0.0.0.0 SlotValue source 'zijn havengeld'",
                "    test 0.0.0.1 SlotValue source 'zijn ligplaatsen'",
                "    test 0.0.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0,"
                        + " sign=ONBEPAALD, unit=null] source '2'",
                "    test 0.0.2 Const String",
                "  consequence FilterDeposit [subject=$vouwonderwerp, fold=havengeldtotaal van een"
                        + " Jutter: vouw 1, member=3, insertion=LOGICAL]"),
                plans(havenketen()));
    }

    @Test void a_quantifier_contribution_with_member_test_and_deposit_flag() throws Exception {
        assertEquals(List.of(
                "rule rustigheid: bijdrage aan vouw 0 | form rustigheid | group null",
                "  readings [0 SUBJECT $onderwerp (onderwerp zelf), 1 INPUT $opgave (eigen"
                        + " ElementKey[objectType=wacht, name=opgave, characteristic=false]), 2 STATE $deelnemers"
                        + " (eigen ElementKey[objectType=wacht, name=deelnemers, characteristic=false]), 3 DEPOSIT_FLAG null"
                        + " (null), 4 LIST_PLACES $lijstplaatsen_plaats_5 (lijstplaatsen 4), 5 LIST_PLACE"
                        + " $plaats_plaats_6 (lijstplaats 4)]",
                "  pattern Ploeg $vouwonderwerp alpha [] join [] beta [] bind []",
                "  subject Wacht $onderwerp alpha [] join [bereik_2: ploeg = $vouwonderwerp] beta [] bind [$opgave"
                        + " = [] Wacht.opgave VALUE]",
                "  accumulation ListPlace $lijstplaats_plaats_3 alpha [plaats-veld_7: [[] field VALUE"
                        + " GELIJK ploeg]] join [] beta [plaats-lid_8: PLACE_OF $onderwerp [] {List<>, type"
                        + " null, source null}] bind [$lijstplaatswaarde_plaats_4 = [] ListPlace.place VALUE]"
                        + " functions [COUNT null -> $lijstplaatsen_plaats_5, MINIMUM $lijstplaatswaarde_plaats_4"
                        + " -> $plaats_plaats_6]",
                "  pattern State $stand_2 alpha [stand_0: [[] attribute VALUE GELIJK"
                        + " ElementKey[objectType=wacht, name=deelnemers, characteristic=false]]] join"
                        + " [stand-onderwerp_1: subject = $onderwerp] beta [] bind [$deelnemers = [] State.value"
                        + " VALUE]",
                "  consequence Contribution [subject=$vouwonderwerp, fold=rustigheid: vouw 0, member=0, slots=[0,"
                        + " 1, 2, 4, 5], size=6, insertion=LOGICAL]",
                "    input MemberTest [probe=3, place=selection 0 [ploeg 4 5] QUANTIFIER] matrix"
                        + " RS_AGG_COMPARE(Division DIVIDED probe 3 (Read 1, Read 2), Const 2, Const >)",
                "      matrix 0 Call source 'greater'",
                "      matrix 0.0 Division source 'dividedBy'",
                "      matrix 0.0.0 SlotValue source 'zijn opgave'",
                "      matrix 0.0.1 SlotValue source 'zijn deelnemers'",
                "      matrix 0.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0,"
                        + " sign=ONBEPAALD, unit=null] source '2'",
                "      matrix 0.2 Const String"),
                plans(matrixThatDivides()));
    }

    /** The case of {@link #havenketen}: J1's two boats unload at P1 and P2, which both lie in H1, so the one haven is
     *  reached by two routes and counts once (30, whose 30 / 10 passes the filter); J2 reaches only H2, whose 4 / 2
     *  does not pass it, so its sum is over no member and leeg. The reference interpreter and the ASP backend both
     *  pass these five expectations. */
    private static final String HAVENKETEN_CASE = """
            {"format": "uchar-case-v0", "asOfDate": null, "input": {"instances": [
              {"label": "J1", "objectType": "Jutter", "attributes": {}, "roles": {"gevoerde boot": ["B1", "B2"]},
               "characteristics": []},
              {"label": "J2", "objectType": "Jutter", "attributes": {}, "roles": {"gevoerde boot": ["B3"]},
               "characteristics": []},
              {"label": "B1", "objectType": "Boot", "attributes": {}, "roles": {"losplaats": ["P1"]},
               "characteristics": []},
              {"label": "B2", "objectType": "Boot", "attributes": {}, "roles": {"losplaats": ["P2"]},
               "characteristics": []},
              {"label": "B3", "objectType": "Boot", "attributes": {}, "roles": {"losplaats": ["P3"]},
               "characteristics": []},
              {"label": "P1", "objectType": "Bergplaats", "attributes": {"vangstgewicht": {"number": "4.50"}},
               "roles": {"haven": ["H1"]}, "characteristics": []},
              {"label": "P2", "objectType": "Bergplaats", "attributes": {}, "roles": {"haven": ["H1"]},
               "characteristics": []},
              {"label": "P3", "objectType": "Bergplaats", "attributes": {"vangstgewicht": {"number": "1"}},
               "roles": {"haven": ["H2"]}, "characteristics": []},
              {"label": "H1", "objectType": "Haven", "attributes": {"havengeld": {"number": "30"},
               "ligplaatsen": {"number": "10"}}, "roles": {}, "characteristics": []},
              {"label": "H2", "objectType": "Haven", "attributes": {"havengeld": {"number": "4"},
               "ligplaatsen": {"number": "2"}}, "roles": {}, "characteristics": []}],
              "parameters": {}},
             "expected": [
              {"kind": "attributeValue", "label": "J1", "attribute": "havengeldtotaal", "value": {"number": "30"}},
              {"kind": "attributeEmpty", "label": "J2", "attribute": "havengeldtotaal"},
              {"kind": "attributeValue", "label": "J1", "attribute": "aantal gevulde bergplaatsen",
               "value": {"number": "1"}},
              {"kind": "attributeValue", "label": "J2", "attribute": "aantal gevulde bergplaatsen",
               "value": {"number": "1"}},
              {"kind": "consistent"}]}
            """;

    /** {@link #havenketen} fired: no document of the corpus or the suite has two intermediates, so the harness does not
     *  watch the rendering of an existence whose second pattern reads the first one's object. */
    @Test void the_path_with_two_between_nodes_fires(@TempDir Path map) throws Exception {
        Path ir = map.resolve("ir.json");
        Files.writeString(ir, JSON.writeValueAsString(havenketen()));
        assertNull(playCase(ir, JSON.readTree(HAVENKETEN_CASE)));
    }

    /** Case {@code caseDocument} of {@code ir} through the full replay on the exec-model branch, as the witnesses of the replay
     *  fire a case ({@code CaseRunner.playCase}): the failure line of the case, null where every expectation matches.
     *  That entry is package-private in its own package, so this takes it by reflection, as {@link #matrixThatDivides}
     *  takes its document. */
    static String playCase(Path ir, JsonNode caseDocument) throws Exception {
        Method play = CaseRunner.class.getDeclaredMethod("playCase", Path.class, JsonNode.class,
                DroolsBackend.Branch.class);
        play.setAccessible(true);
        return (String) play.invoke(null, ir, caseDocument, DroolsBackend.Branch.EXEC_MODEL);
    }

    /** The case of {@link #havenketenWithRoot} where the filter faults (fix round 1c-B2): J1's boats B1, B2 and B3, in
     *  that order, unload at P1, P2 and P3, which lie in H1, H2 and H1, and both havens have a havengeld that is no
     *  square, 3 and 2, so the filter faults along every route. Until the card zero-division-channel (2026-10-19) the
     *  filter of {@link #havenketen} divided the havengeld (4 and 30) by 0 ligplaatsen, a fault in the reference
     *  interpreter and the ASP backend then ("division by numeric zero", a sentence that names no member), a deposit
     *  since. */
    private static final String HAVENKETEN_FAULT = """
            {"format": "uchar-case-v0", "asOfDate": null, "input": {"instances": [
              {"label": "J1", "objectType": "Jutter", "attributes": {}, "roles": {"gevoerde boot": ["B1", "B2", "B3"]},
               "characteristics": []},
              {"label": "B1", "objectType": "Boot", "attributes": {}, "roles": {"losplaats": ["P1"]},
               "characteristics": []},
              {"label": "B2", "objectType": "Boot", "attributes": {}, "roles": {"losplaats": ["P2"]},
               "characteristics": []},
              {"label": "B3", "objectType": "Boot", "attributes": {}, "roles": {"losplaats": ["P3"]},
               "characteristics": []},
              {"label": "P1", "objectType": "Bergplaats", "attributes": {}, "roles": {"haven": ["H1"]},
               "characteristics": []},
              {"label": "P2", "objectType": "Bergplaats", "attributes": {}, "roles": {"haven": ["H2"]},
               "characteristics": []},
              {"label": "P3", "objectType": "Bergplaats", "attributes": {}, "roles": {"haven": ["H1"]},
               "characteristics": []},
              {"label": "H1", "objectType": "Haven", "attributes": {"havengeld": {"number": "3"},
               "ligplaatsen": {"number": "0"}}, "roles": {}, "characteristics": []},
              {"label": "H2", "objectType": "Haven", "attributes": {"havengeld": {"number": "2"},
               "ligplaatsen": {"number": "0"}}, "roles": {}, "characteristics": []}],
              "parameters": {}},
             "expected": []}
            """;

    /** {@link #havenketen} with "de wortel van zijn havengeld is groter dan 2" in place of the filter's division,
     *  "zijn havengeld gedeeld door zijn ligplaatsen is groter dan 2": the root of a havengeld that is no square faults
     *  (RUNTIME-SEMANTICS ch. 2.2), where a numeric zero divisor deposits since the card zero-division-channel. */
    static JsonNode havenketenWithRoot() throws Exception {
        ObjectNode d = (ObjectNode) havenketen();
        JsonNode rules = d.get("rules").get("rules");
        ObjectNode value = (ObjectNode) rules.get(rules.size() - 1).get("versions").get(0).get("value");
        ObjectNode comparison = (ObjectNode) value.get("filter").get("parts").get(0);
        comparison.set("left", JSON.readTree("{\"node\": \"BuiltinCall\", \"kind\": \"squareRoot\", \"arguments\": ["
                + ref("havengeld", "Haven", "zijn havengeld") + "], \"type\": {\"node\": \"Number\", \"whole\": true,"
                + " \"decimals\": 0, \"sign\": \"unspecified\"}, \"source\": {\"snippet\": \"de wortel van zijn"
                + " havengeld\"}}"));
        return d;
    }

    /** {@link #havenketenWithRoot} fired where the filter faults (fix round 1c-B2): the rule of the filter's fault binds
     *  the two intermediates per route and reads the place of the route's Boot, the first one, in J1's list, so each
     *  route gives a {@link FilterFault} with its own place: H1 along B1 at [0, 0], H2 along B2 at [0, 1], H1 along B3
     *  at [0, 2]. The least is the first route's, H1's, so the rule names H1's fault, the first member in fold order
     *  (RUNTIME-SEMANTICS ch. 5.11): the root of 3, where the least sentence would name H2's 2. A place reader that
     *  reads the wrong place gives another fault or other places; no document of the corpus or the suite fires this
     *  form, so the harness does not watch it. Until the card zero-division-channel the filter divided H1's 4 and H2's
     *  30 by 0. */
    @Test void the_path_with_two_between_nodes_fails_at_the_first_route_place(@TempDir Path map) throws Exception {
        Path ir = map.resolve("ir.json"), caseDocument = map.resolve("case.json");
        Files.writeString(ir, JSON.writeValueAsString(havenketenWithRoot()));
        Files.writeString(caseDocument, HAVENKETEN_FAULT);
        String root = "ComputeFault: fout in de regel \"havengeldtotaal van een Jutter: vouw 1\": irrational square root"
                + " of %s reached without an explicit rounding (§13.4.17.13 mandates one)";
        assertEquals(List.of(
                "FilterFault J1 H1 [0, 0]: " + root.formatted("3"),
                "FilterFault J1 H2 [0, 1]: " + root.formatted("2"),
                "FilterFault J1 H1 [0, 2]: " + root.formatted("3")),
                filterFaults(ir, JSON.readTree(HAVENKETEN_FAULT)));
        CaseRunner.RedResult r = CaseRunner.runRed(ir, caseDocument);
        assertEquals("FAULT", r.status(), r::detail);
        assertTrue(r.detail().startsWith("ComputeFault: fout in de regel \"havengeldtotaal van een Jutter\": irrational"
                + " square root of 3 "), r::detail);
    }

    /** The {@link FilterFault} facts that stand at the fixpoint of {@code caseDocument} on {@code ir}, in their own order, each
     *  with the case labels of the fold's subject and of the member, its place and its fault as named after the fold.
     *  Read off the session after the fire and before the write-back, which raises the fault of the rule instead,
     *  through the measuring seam of {@code CaseRunner} ({@code forMeasurement}, {@code withSession}), which is
     *  package-private in its own package: by reflection, as {@link #matrixThatDivides} takes its document. */
    @SuppressWarnings("unchecked")
    private static List<String> filterFaults(Path ir, JsonNode caseDocument) throws Exception {
        Method forMeasurement = CaseRunner.class.getDeclaredMethod("forMeasurement", Path.class, DroolsBackend.Branch.class);
        forMeasurement.setAccessible(true);
        Object runner = forMeasurement.invoke(null, ir, DroolsBackend.Branch.EXEC_MODEL);
        Class<?> measurement = Class.forName("dev.uchar.regelspraak.cases.CaseRunner$SessionMeasurement");
        Object reader = Proxy.newProxyInstance(measurement.getClassLoader(), new Class<?>[]{measurement}, (proxy, m, a) -> {
            if (!m.getName().equals("meet")) throw new UnsupportedOperationException(m.getName());
            DroolsBackend.Build build = (DroolsBackend.Build) a[0];
            KieSession session = (KieSession) a[1];
            DroolsBackend.Firing firing = DroolsBackend.fire(build, session);
            if (firing.noFixpoint() != null) throw new AssertionError(firing.noFixpoint());
            Map<Object, String> label = new IdentityHashMap<>();
            ((Map<String, Object>) a[2]).forEach((l, beanObject) -> label.put(beanObject, l));
            List<FilterFault> faults = new ArrayList<>();
            for (Object f : session.getObjects()) if (f instanceof FilterFault x) faults.add(x);
            faults.sort(null);
            List<String> out = new ArrayList<>();
            for (FilterFault x : faults) {
                RuntimeException named = ComputeFault.designate(x.fault(), x.fold());
                out.add("FilterFault " + label.get(x.subject()) + " " + label.get(x.element()) + " " + x.place()
                        + ": " + named.getClass().getSimpleName() + ": " + named.getMessage());
            }
            return out;
        });
        Method withSession = CaseRunner.class.getDeclaredMethod("withSession", JsonNode.class, measurement);
        withSession.setAccessible(true);
        return (List<String>) withSession.invoke(runner, caseDocument, reader);
    }

    /** The scope of an existence covers what its patterns bind as well (fix round 1c-B2): a value a pattern inside
     *  binds lives only inside it, like the pattern's object. The plan declares it among the variables of the
     *  existence, the constructor refuses a scope without it, and the rendering forgets it after the existence, so an
     *  item after it that names the value fails loudly instead of joining a value of the quantification. */
    @Test void the_scope_of_an_exists_includes_the_bindings_of_its_patterns() {
        FactPattern withBinding = new FactPattern(Value.class, "$waarde", List.of(), List.of(),
                List.of(Binding.of("$v", Value.class, "value")));
        IllegalArgumentException without = assertThrows(IllegalArgumentException.class,
                () -> new Exists(List.of(withBinding), List.of("$waarde"), List.of()));
        assertEquals("een bestaat-item declareert de variabelen van zijn patronen en van hun bindingen,"
                + " [$waarde, $v], niet [$waarde]", without.getMessage());
        Exists exists = new Exists(List.of(withBinding), List.of("$waarde", "$v"), List.of());
        RulePlan afterwards = new RulePlan("na het bestaan", null, null, new ReadTable(List.of()), List.of(exists,
                new FactPattern(Value.class, "$erna", List.of(), List.of(new Join("erna", null, "$v")), List.of())),
                new Consequence.InsertChannelState("bestaan", true, Consequence.Insertion.LOGICAL));
        IllegalStateException unbound = assertThrows(IllegalStateException.class,
                () -> PatternDslRendering.rule(afterwards, name -> null));
        assertEquals("de variabele $v is nog niet gebonden", unbound.getMessage());
    }

    /** Two quantifications that each bind the same name inside, an existence and a negation, are a plan the scope of
     *  each allows (fix round 1c-C1): what one binds lives only inside it, so the rendering takes the second binding
     *  of the name as well, and an item after both that names the value still fails loudly. */
    @Test void two_quantifications_each_bind_the_same_local_name() {
        Exists exists = new Exists(List.of(new FactPattern(Value.class, "$eerste", List.of(), List.of(),
                List.of(Binding.of("$v", Value.class, "value")))), List.of("$eerste", "$v"), List.of());
        Not not = new Not(new FactPattern(Value.class, "$tweede", List.of(), List.of(),
                List.of(Binding.of("$v", Value.class, "value"))));
        Consequence channel = new Consequence.InsertChannelState("bestaan", true, Consequence.Insertion.LOGICAL);
        RulePlan two = new RulePlan("twee kwantificaties", null, null, new ReadTable(List.of()),
                List.of(exists, not), channel);
        assertEquals("twee kwantificaties", PatternDslRendering.rule(two, name -> null).getName());
        RulePlan afterwards = new RulePlan("na twee kwantificaties", null, null, new ReadTable(List.of()), List.of(exists,
                not, new FactPattern(Value.class, "$erna", List.of(), List.of(new Join("erna", null, "$v")), List.of())),
                channel);
        IllegalStateException unbound = assertThrows(IllegalStateException.class,
                () -> PatternDslRendering.rule(afterwards, name -> null));
        assertEquals("de variabele $v is nog niet gebonden", unbound.getMessage());
    }

    /** {@code jutterswet-gevuldepad} (a Jutter's boats, each unloading at one Bergplaats) with a third step, since no
     *  golden has a range of more steps with more than one intermediate: a Bergplaats lies in one Haven, and a sum of
     *  the havens' havengeld over the three steps, whose filter divides. The first step goes back from the Jutter and
     *  the two after it forward, so neither walk reaches the Boot or the Bergplaats. Verifier-clean. */
    static JsonNode havenketen() throws Exception {
        ObjectNode d = (ObjectNode) golden("jutterswet-gevuldepad");
        String number = "{\"node\": \"Number\", \"whole\": true, \"decimals\": 0, \"sign\": \"nonNegative\"}";
        for (JsonNode ot : d.get("model").get("objectTypes"))
            if (ot.get("name").asText().equals("Jutter"))
                ((ArrayNode) ot.get("attributes")).add(JSON.readTree("{\"node\": \"Attribute\", \"name\":"
                        + " \"havengeldtotaal\", \"datatype\": " + number + "}"));
        ((ArrayNode) d.get("model").get("objectTypes")).add(JSON.readTree("{\"node\": \"ObjectType\", \"name\": \"Haven\","
                + " \"animate\": false, \"plural\": \"Havens\", \"attributes\": [{\"node\": \"Attribute\", \"name\":"
                + " \"havengeld\", \"datatype\": " + number + "}, {\"node\": \"Attribute\", \"name\": \"ligplaatsen\","
                + " \"datatype\": " + number + "}], \"characteristics\": []}"));
        ((ArrayNode) d.get("model").get("factTypes")).add(JSON.readTree("{\"node\": \"FactType\", \"name\": \"ligging\","
                + " \"reciprocal\": false, \"roles\": [{\"node\": \"Role\", \"name\": \"gelegen bergplaats\","
                + " \"objectType\": \"Bergplaats\", \"plural\": \"gelegen bergplaatsen\", \"many\": true}, {\"node\":"
                + " \"Role\", \"name\": \"haven\", \"objectType\": \"Haven\", \"plural\": \"havens\", \"many\": false}]}"));
        String filter = "{\"node\": \"Compound\", \"quantifier\": \"the\", \"threshold\": 0, \"parts\": [{\"node\":"
                + " \"Comparison\", \"op\": \"greater\", \"left\": {\"node\": \"Binary\", \"op\": \"dividedBy\", \"left\": "
                + ref("havengeld", "Haven", "zijn havengeld") + ", \"right\": "
                + ref("ligplaatsen", "Haven", "zijn ligplaatsen") + ", \"type\": {\"node\": \"Number\", \"whole\": false,"
                + " \"decimals\": 0, \"sign\": \"unspecified\"}, \"source\": {\"snippet\": \"zijn havengeld gedeeld door"
                + " zijn ligplaatsen\"}}, \"right\": {\"node\": \"Literal\", \"value\": {\"number\": \"2\"}, \"type\":"
                + " {\"node\": \"Number\", \"whole\": true, \"decimals\": 0, \"sign\": \"unspecified\"}, \"source\":"
                + " {\"snippet\": \"2\"}}, \"source\": {\"snippet\": \"zijn havengeld gedeeld door zijn ligplaatsen is"
                + " groter dan 2\"}}], \"source\": {\"snippet\": \"aan de volgende voorwaarde voldoen: zijn havengeld"
                + " gedeeld door zijn ligplaatsen is groter dan 2\"}}";
        String path = "[{\"node\": \"RoleBinding\", \"factType\": \"bemanning\", \"role\": \"gevoerde boot\"}, {\"node\":"
                + " \"RoleBinding\", \"factType\": \"lossing\", \"role\": \"losplaats\"}, {\"node\": \"RoleBinding\","
                + " \"factType\": \"ligging\", \"role\": \"haven\"}]";
        ((ArrayNode) d.get("rules").get("rules")).add(JSON.readTree("{\"node\": \"Rule\", \"name\": \"havengeldtotaal van"
                + " een Jutter\", \"versions\": [{\"validity\": {\"node\": \"Validity\"}, \"target\": "
                + ref("havengeldtotaal", "Jutter", "Het havengeldtotaal van een Jutter") + ", \"value\": {\"node\":"
                + " \"Aggregation\", \"kind\": \"sum\", \"arguments\": [" + ref("havengeld", "Haven", "het havengeld van"
                + " alle havens van alle losplaatsen van alle gevoerde boten van de Jutter") + "], \"elementType\":"
                + " \"Haven\", \"filter\": " + filter + ", \"sourcePath\": " + path + ", \"type\": " + number + ","
                + " \"source\": {\"snippet\": \"de som van het havengeld van alle havens van alle losplaatsen van alle"
                + " gevoerde boten van de Jutter die aan de volgende voorwaarde voldoen: zijn havengeld gedeeld door zijn"
                + " ligplaatsen is groter dan 2\"}}, \"initialisation\": false, \"presentation\": \"calculation\"}]}"));
        return d;
    }

    private static String ref(String attribute, String type, String snippet) {
        return "{\"node\": \"AttributeReference\", \"attribute\": \"" + attribute + "\", \"subjectType\": \"" + type
                + "\", \"source\": {\"snippet\": \"" + snippet + "\"}}";
    }

    /** The document of {@code TmsQuantifierFoldWitnessTest} whose quantifier fold "geen van de wachten van de Ploeg"
     *  tests a matrix that divides by the members' deelnemers, a derived value, and so can fault and deposit, with the
     *  rule of the deelnemers before it. Taken from that witness by reflection, as its members are of its own package,
     *  so that the document has one source. */
    static JsonNode matrixThatDivides() throws Exception {
        Class<?> k = Class.forName("dev.uchar.regelspraak.cases.TmsQuantifierFoldWitnessTest");
        Method document = Class.forName("dev.uchar.regelspraak.cases.TmsDocuments").getDeclaredMethod("document",
                String.class, List.class);
        document.setAccessible(true);
        Path ir = (Path) document.invoke(null, field(k, "MODEL"), List.of(field(k, "DEELNEMERS"), field(k, "CALM")));
        return JSON.readTree(ir.toFile());
    }

    private static String field(Class<?> javaClass, String name) throws ReflectiveOperationException {
        Field f = javaClass.getDeclaredField(name);
        f.setAccessible(true);
        return (String) f.get(null);
    }

    static JsonNode golden(String name) throws Exception {
        return JSON.readTree(GOLDEN.resolve(name).resolve("ir.json").toFile());
    }

    /** Every plan of {@code document} of a rule of a fold, in the order of the model, as text; after a line that
     *  holds a term, the term node by node. */
    static List<String> plans(JsonNode document) {
        List<String> out = new ArrayList<>();
        for (RulePlan p : DroolsBackend.build(document, DroolsBackend.Branch.EXEC_MODEL).execModelTranspiler()
                .modelplan().rules()) {
            if (!(p.consequence() instanceof Consequence.Contribution || p.consequence() instanceof Consequence.FilterDeposit
                    || p.consequence() instanceof Consequence.FilterFault || p.consequence() instanceof Consequence.MemberFault))
                continue;
            out.add("rule " + p.name() + " | form " + p.form() + " | group " + p.agendaGroup());
            List<String> slots = new ArrayList<>();
            for (Read l : p.readings().slots())
                slots.add(l.slot() + " " + l.kind() + " " + l.name() + " (" + l.key() + ")");
            out.add("  readings " + slots);
            for (LhsItem item : p.leftSide()) {
                out.add("  " + item(item));
                if (item instanceof TestItem t) TermTree.fields("    test", t.test(), "0", out);
            }
            consequence(p.consequence(), out);
        }
        return out;
    }

    private static String item(LhsItem item) {
        return switch (item) {
            case Subject o -> "subject " + o.beanClass().getSimpleName() + " " + o.name() + " alpha "
                    + alpha(o.alpha()) + " join " + joins(o.joins()) + " beta " + beta(o.betaTests()) + " bind "
                    + bindings(o.bindings());
            case FactPattern p -> "pattern " + pattern(p);
            case Not n -> "not " + (n.patterns().size() == 1 ? pattern(n.patterns().get(0)) : "inside "
                    + n.inside() + " outside " + n.outside() + " "
                    + n.patterns().stream().map(FoldPlanWitnessTest::pattern).toList());
            case Exists b -> "exists inside " + b.inside() + " outside " + b.outside() + " "
                    + b.patterns().stream().map(FoldPlanWitnessTest::pattern).toList();
            case Accumulation a -> "accumulation " + pattern(a.source()) + " functions "
                    + a.aggregations().stream().map(f -> f.kind() + " " + f.source() + " -> " + f.target()).toList();
            case TestItem t -> "test " + t.id() + " " + t.role() + " probe " + t.depositFlag() + " slots " + t.slots()
                    + " size " + t.size() + ": " + TermTree.tree(t.test());
        };
    }

    private static String pattern(FactPattern p) {
        return p.factClass().getSimpleName() + " " + p.name() + " alpha " + alpha(p.alpha()) + " join "
                + joins(p.joins()) + " beta " + beta(p.betaTests()) + " bind " + bindings(p.bindings());
    }

    /** Each constraint between two objects with its id, its function, the variable on the other side and each of its
     *  constants with every field ({@link #constant}). */
    private static List<String> beta(List<BetaTest> beta) {
        return beta.stream().map(t -> t.id() + ": " + t.function() + " " + t.target()
                + t.constants().stream().map(c -> " " + constant(c)).reduce("", String::concat)).toList();
    }

    /** A constant of the analysis with every field: its value, the class of its value (a list by the classes of its
     *  elements), its IR type and its source fragment, each null where the constant has none (fix round 1c-B2). */
    private static String constant(Term.Const c) {
        return c.value() + " {" + javaClass(c.value()) + ", type " + c.type() + ", source " + c.source() + "}";
    }

    private static String javaClass(Object value) {
        if (value == null) return "null";
        if (value instanceof List<?> l)
            return "List<" + String.join(",", l.stream().map(e -> e == null ? "null" : e.getClass().getSimpleName())
                    .distinct().toList()) + ">";
        return value.getClass().getSimpleName();
    }

    private static List<String> joins(List<Join> joins) {
        return joins.stream().map(j -> j.id() + ": " + j.field() + " = " + j.target()
                + (j.targetField() == null ? "" : "." + field(j.targetField()))).toList();
    }

    private static List<String> alpha(List<Alpha> alpha) {
        List<String> out = new ArrayList<>();
        for (Alpha a : alpha)
            out.add(a.id() + ": " + a.tests().stream().map(t -> fields(t.path()) + " " + t.field() + " " + t.form()
                    + " " + t.op() + (t.value() == null ? "" : " " + t.value())).toList());
        return out;
    }

    private static List<String> bindings(List<Binding> bindings) {
        return bindings.stream().map(b -> b.target() + " = " + fields(b.path()) + " " + field(b.field()) + " " + b.form()
                + (b.category() == null ? "" : " " + b.category().getSimpleName())).toList();
    }

    private static String fields(List<Binding.Field> path) {
        return path.stream().map(FoldPlanWitnessTest::field).toList().toString();
    }

    private static String field(Binding.Field v) {
        return v.javaClass().getSimpleName() + "." + v.name();
    }

    /** The consequence {@code g} with every field, what a member contributes and its place spelled out, and each term
     *  as a tree and node by node. */
    private static void consequence(Consequence g, List<String> out) {
        switch (g) {
            case Consequence.Contribution b -> {
                out.add("  consequence Contribution [subject=" + b.subject() + ", fold=" + b.fold() + ", member=" + b.member()
                        + ", slots=" + b.slots() + ", size=" + b.size() + ", insertion=" + b.insertion() + "]");
                switch (b.input()) {
                    case Consequence.Input.Count t -> out.add("    input Count");
                    case Consequence.Input.MemberValue l -> out.add("    input MemberValue [value=" + l.value() + ", total="
                            + l.total() + ", filled=" + l.filled() + "]");
                    case Consequence.Input.Verdict o -> {
                        out.add("    input Verdict matrix " + TermTree.tree(o.matrix()));
                        TermTree.fields("      matrix", o.matrix(), "0", out);
                    }
                    case Consequence.Input.MemberTest l -> {
                        out.add("    input MemberTest [probe=" + l.depositFlag() + ", place=" + place(l.place()) + "] matrix "
                                + TermTree.tree(l.matrix()));
                        TermTree.fields("      matrix", l.matrix(), "0", out);
                    }
                }
            }
            case Consequence.FilterDeposit f -> out.add("  consequence FilterDeposit [subject=" + f.subject() + ", fold="
                    + f.fold() + ", member=" + f.member() + ", insertion=" + f.insertion() + "]");
            case Consequence.FilterFault f -> {
                out.add("  consequence FilterFault [subject=" + f.subject() + ", fold=" + f.fold() + ", member=" + f.member()
                        + ", place=" + place(f.place()) + ", slots=" + f.slots() + ", size=" + f.size()
                        + ", insertion=" + f.insertion() + "] filter " + TermTree.tree(f.filter()));
                TermTree.fields("    filter", f.filter(), "0", out);
            }
            case Consequence.MemberFault f -> out.add("  consequence MemberFault [subject=" + f.subject() + ", fold=" + f.fold()
                    + ", member=" + f.member() + ", place=" + place(f.place()) + ", before=" + f.before() + ", after=" + f.after()
                    + ", insertion=" + f.insertion() + "]");
            default -> throw new IllegalStateException("geen gevolg van een vouw: " + g);
        }
    }

    private static String place(Consequence.FoldPlace p) {
        return "selection " + p.selection() + " " + p.steps().stream().map(s -> s.field() + " " + s.count() + " "
                + s.place()).toList() + " " + p.reason();
    }
}
