package dev.uchar.regelspraak.transpiler.tms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.transpiler.tms.plan.Accumulation;
import dev.uchar.regelspraak.transpiler.tms.plan.Alpha;
import dev.uchar.regelspraak.transpiler.tms.plan.Exists;
import dev.uchar.regelspraak.transpiler.tms.plan.Binding;
import dev.uchar.regelspraak.transpiler.tms.plan.Consequence;
import dev.uchar.regelspraak.transpiler.tms.plan.Join;
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The rules of the forms that fill or read the §9.5 channel come whole from a plan (card
 * drl-teksttak-op-truth-maintenance, step 1c-B1): the rule of a consistency, the key rule, the key-form rule, the
 * violation rule and the channel rule of a uniqueness, the two rules of a channel's standing, and the recipient rule
 * and the rule of the distribution of an allocation. Each is a {@link RulePlan} whose left-hand side holds the
 * patterns, the existence, the negation, the accumulates and the constraints the form built itself before, with their
 * constraint ids in their order, among them an identity join on the subject pattern that reads a reference field on the
 * other side and a constraint between two keys by value; and whose consequence carries every decision as a field, with
 * what it computes as a route of the plan ({@code plan.Route}): the keys of a uniqueness, the fault of a key's form and
 * the facts of a distribution, which {@code Allocation.bereken} computes. This witness pins those plans as text, per
 * plan in order: the name, the form and the group; the slots, each with its kind, its variable and its key; every item
 * of the left-hand side in order with its kind, its pattern class, its constraint ids, each field test with its path
 * and its form of reading, its joins with the reference field they read on the other side, its constraints by value
 * with their function, and its bindings with their path and form; and the kind of the consequence with every field. A
 * term, the test of a rule or a key component of a consequence, is pinned as a tree and on the lines after it node by
 * node with its source fragment and of a constant the class of its value and its whole IR type
 * ({@link TermTree#fields}).
 *
 * <ul>
 *   <li>{@code jutterswet-vondstnummer}: a consistency whose assertion is a comparison of dates, negated, and a
 *       uniqueness of one source and one component, without a scope.</li>
 *   <li>{@code jutterswet-keurspelling}: a uniqueness over an enumeration, whose key component is the label in its
 *       normalised form.</li>
 *   <li>The worked example of RUNTIME-SEMANTICS ch. 5.6 ({@link TestTermWitnessTest#TOEZICHT}), since no golden tests
 *       a channel: a consistency and the two rules of the standing of its channel, which a rule reads.</li>
 *   <li>{@code anchor/jutterswet-kustregister}: a uniqueness scoped by a parent, and a union of two sources.</li>
 *   <li>{@code anchor/jutterswet-strandregister}: a uniqueness over a key on a timeline, whose key rule makes a key
 *       per value the timeline ever carries, beside the rule of the key's form.</li>
 *   <li>{@code jutterswet-rangmaximering}: an allocation in order with a weight, an ordering key, a cap and a rest,
 *       whose second case deposits on a recipient: the recipient rule after the source's pattern, joined to it by the
 *       recipient's reference field, and the rule of the distribution over the list of recipients.</li>
 *   <li>{@code jutterswet-bergersdeling} with a relation of two to-one roles ({@link #sourcePointsToRecipient}), since
 *       no golden has one: the join reads the reference field on the source. This document is also fired with a case
 *       of three sources, and its shares and rests pinned, since the plan names the field and only a firing shows that
 *       the rendering reads it.</li>
 *   <li>Document F of {@code TmsAllocationWitnessTest} ({@link #recipientFromCreation}), since no golden has one: an
 *       allocation over a relation a form creates, whose range is the existence of a link.</li>
 * </ul>
 *
 * <p>That the rendering of these plans builds the rules of before the step is what the harness {@code FireTrace}
 * shows, and the executable model of the corpus, which is equal as text; for the three documents outside the corpus
 * the executable model of before and after the step is equal as text as well.
 *
 * <p><b>Red</b> before the step: these forms built their rules themselves, so there was no plan of them, and this class
 * did not compile, since the plan had no join on the subject pattern, no constraint between two patterns by value and
 * no join that reads a field on the other side; with only the names of the plans pinned, none of the eight documents
 * had a plan of any of these rules.
 */
class ChannelPlanWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void a_consistency_and_a_uniqueness_without_scope() throws Exception {
        assertEquals(List.of(
                "rule aanmelding niet voor de vondstdag | form aanmelding niet voor de vondstdag | group null",
                "  readings [0 INPUT $aanmelddatum (eigen ElementKey[objectType=vondst, name=aanmelddatum,"
                        + " characteristic=false]), 1 INPUT $vondstdag (eigen ElementKey[objectType=vondst,"
                        + " name=vondstdag, characteristic=false])]",
                "  subject Vondst $onderwerp alpha [] join [] bind [$aanmelddatum = [] Vondst.aanmelddatum VALUE,"
                        + " $vondstdag = [] Vondst.vondstdag VALUE]",
                "  test voorwaarde_0 CONDITION probe -1 slots [0, 1] size 2: NIET(DATE_COMPARE(Read 0, Read 1,"
                        + " Const LATER_OF_GELIJK))",
                "    test 0 Call source 'De aanmelddatum van een Vondst moet later of gelijk zijn aan de vondstdag"
                        + " van de Vondst'",
                "    test 0.0 Call source 'De aanmelddatum van een Vondst moet later of gelijk zijn aan de vondstdag"
                        + " van de Vondst'",
                "    test 0.0.0 SlotValue source 'De aanmelddatum van een Vondst'",
                "    test 0.0.1 SlotValue source 'de vondstdag van de Vondst'",
                "    test 0.0.2 Const Operator",
                "  consequence Inconsistency [subject=$onderwerp, form=aanmelding niet voor de vondstdag,"
                        + " codes=[EF-025], insertion=LOGICAL]",
                "rule eenmalig vondstnummer: sleutel van Vondst 1 | form eenmalig vondstnummer | group null",
                "  readings [0 INPUT $vondstnummer (eigen ElementKey[objectType=vondst, name=vondstnummer,"
                        + " characteristic=false])]",
                "  subject Vondst $onderwerp alpha [] join [] bind [$vondstnummer = [] Vondst.vondstnummer VALUE]",
                "  test voorwaarde_1 VORM probe -1 slots [0] size 1: Compound ALLE 0(EMPTY_TEST(Read 0, Const"
                        + " false))",
                "    test 0 Compound",
                "    test 0.0 Call",
                "    test 0.0.0 SlotValue",
                "    test 0.0.1 Const Boolean",
                "  consequence UniquenessKey [subject=$onderwerp, uniqueness=eenmalig vondstnummer, source=0,"
                        + " components=[Read 0], timeline=-1, scope=-1, slots=[0], size=1, insertion=LOGICAL]",
                "    components 0 0 SlotValue",
                "rule eenmalig vondstnummer | form eenmalig vondstnummer | group null",
                "  readings [0 COLLISIONS $botsingen (botsingen)]",
                "  pattern UniquenessKey $sleutel alpha [uniciteit_2: [[] uniqueness VALUE GELIJK eenmalig"
                        + " vondstnummer]] join [] beta [] bind [$element = [] UniquenessKey.element VALUE]",
                "  accumulation UniquenessKey $ander alpha [uniciteit_3: [[] uniqueness VALUE GELIJK eenmalig"
                        + " vondstnummer]] join [] beta [botsing_4: COLLIDES_WITH $sleutel] bind [] functions [COUNT null"
                        + " -> $botsingen]",
                "  test dubbel_5 VORM probe -1 slots [0] size 1: DUPLICATE(Read 0)",
                "    test 0 Call",
                "    test 0.0 SlotValue",
                "  consequence UniquenessViolation [subject=$element, uniqueness=eenmalig vondstnummer, insertion=LOGICAL]",
                "rule eenmalig vondstnummer: kanaal | form eenmalig vondstnummer | group null",
                "  readings []",
                "  pattern UniquenessViolation $schending alpha [schending_6: [[] uniqueness VALUE GELIJK eenmalig"
                        + " vondstnummer]] join [] beta [] bind [$subject = [] UniquenessViolation.element VALUE]",
                "  consequence Inconsistency [subject=$subject, form=eenmalig vondstnummer, codes=[EF-026], insertion=LOGICAL]"),
                plans(golden("jutterswet-vondstnummer"), List.of("aanmelding niet voor de vondstdag",
                        "eenmalig vondstnummer")));
    }

    @Test void a_uniqueness_over_an_enumeration() throws Exception {
        assertEquals(List.of(
                "rule eenmalige staat: sleutel van Vondst 1 | form eenmalige staat | group null",
                "  readings [0 INPUT $staat (eigen ElementKey[objectType=vondst, name=staat, characteristic=false])]",
                "  subject Vondst $onderwerp alpha [] join [] bind [$staat = [] Vondst.staat VALUE]",
                "  test voorwaarde_5 VORM probe -1 slots [0] size 1: Compound ALLE 0(EMPTY_TEST(Read 0, Const"
                        + " false))",
                "    test 0 Compound",
                "    test 0.0 Call",
                "    test 0.0.0 SlotValue",
                "    test 0.0.1 Const Boolean",
                "  consequence UniquenessKey [subject=$onderwerp, uniqueness=eenmalige staat, source=0,"
                        + " components=[RS_ENUM_NAME(Read 0)], timeline=-1, scope=-1, slots=[0], size=1,"
                        + " insertion=LOGICAL]",
                "    components 0 0 Call",
                "    components 0 0.0 SlotValue",
                "rule eenmalige staat | form eenmalige staat | group null",
                "  readings [0 COLLISIONS $botsingen (botsingen)]",
                "  pattern UniquenessKey $sleutel alpha [uniciteit_6: [[] uniqueness VALUE GELIJK eenmalige staat]]"
                        + " join [] beta [] bind [$element = [] UniquenessKey.element VALUE]",
                "  accumulation UniquenessKey $ander alpha [uniciteit_7: [[] uniqueness VALUE GELIJK eenmalige"
                        + " staat]] join [] beta [botsing_8: COLLIDES_WITH $sleutel] bind [] functions [COUNT null ->"
                        + " $botsingen]",
                "  test dubbel_9 VORM probe -1 slots [0] size 1: DUPLICATE(Read 0)",
                "    test 0 Call",
                "    test 0.0 SlotValue",
                "  consequence UniquenessViolation [subject=$element, uniqueness=eenmalige staat, insertion=LOGICAL]",
                "rule eenmalige staat: kanaal | form eenmalige staat | group null",
                "  readings []",
                "  pattern UniquenessViolation $schending alpha [schending_10: [[] uniqueness VALUE GELIJK eenmalige"
                        + " staat]] join [] beta [] bind [$subject = [] UniquenessViolation.element VALUE]",
                "  consequence Inconsistency [subject=$subject, form=eenmalige staat, codes=[EF-026], insertion=LOGICAL]"),
                plans(golden("jutterswet-keurspelling"), List.of("eenmalige staat")));
    }

    @Test void a_consistency_and_the_two_channel_states() throws Exception {
        assertEquals(List.of(
                "rule controle saldo | form controle saldo | group null",
                "  readings [0 INPUT $saldo (eigen ElementKey[objectType=rekeninghouder, name=saldo,"
                        + " characteristic=false])]",
                "  subject Rekeninghouder $onderwerp alpha [] join [] bind [$saldo = [] Rekeninghouder.saldo"
                        + " VALUE]",
                "  test voorwaarde_2 CONDITION probe -1 slots [0] size 1: NIET(RS_AGG_COMPARE(Read 0, Const 0,"
                        + " Const >=))",
                "    test 0 Call source 'zijn saldo is groter dan of gelijk aan 0'",
                "    test 0.0 Call source 'zijn saldo is groter dan of gelijk aan 0'",
                "    test 0.0.0 SlotValue source 'zijn saldo'",
                "    test 0.0.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0, sign=ONBEPAALD,"
                        + " unit=null] source '0'",
                "    test 0.0.2 Const String",
                "  consequence Inconsistency [subject=$onderwerp, form=controle saldo, codes=[EF-025], insertion=LOGICAL]",
                "rule kanaalstand controle saldo: inconsistent | form null | group null",
                "  readings []",
                "  exists [Inconsistentie $inconsistentie alpha [kanaal_3: [[] rule VALUE GELIJK controle saldo]]"
                        + " join [] beta [] bind []]",
                "  consequence InsertChannelState [producer=controle saldo, inconsistent=true, insertion=LOGICAL]",
                "rule kanaalstand controle saldo: consistent | form null | group null",
                "  readings []",
                "  not Inconsistentie $inconsistentie alpha [kanaal_4: [[] rule VALUE GELIJK controle saldo]] join"
                        + " [] beta [] bind []",
                "  consequence InsertChannelState [producer=controle saldo, inconsistent=false, insertion=LOGICAL]"),
                plans(document(TestTermWitnessTest.TOEZICHT), List.of("controle saldo")));
    }

    @Test void a_uniqueness_with_scope_and_a_union() throws Exception {
        assertEquals(List.of(
                "rule eenmalig kaveletiket: sleutel van Kavel 1 | form eenmalig kaveletiket | group null",
                "  readings [0 INPUT $etiket (eigen ElementKey[objectType=kavel, name=etiket, characteristic=false]),"
                        + " 1 SCOPE $gemeente (scope kavelindeling gemeente)]",
                "  subject Kavel $onderwerp alpha [] join [] bind [$etiket = [] Kavel.etiket VALUE, $gemeente ="
                        + " [] Kavel.gemeente VALUE]",
                "  test voorwaarde_5 VORM probe -1 slots [0, 1] size 2: Compound ALLE 0(EMPTY_TEST(Read 0,"
                        + " Const false), EMPTY_TEST(Read 1, Const false))",
                "    test 0 Compound",
                "    test 0.0 Call",
                "    test 0.0.0 SlotValue",
                "    test 0.0.1 Const Boolean",
                "    test 0.1 Call",
                "    test 0.1.0 SlotValue",
                "    test 0.1.1 Const Boolean",
                "  consequence UniquenessKey [subject=$onderwerp, uniqueness=eenmalig kaveletiket, source=0,"
                        + " components=[Read 0], timeline=-1, scope=1, slots=[0, 1], size=2, insertion=LOGICAL]",
                "    components 0 0 SlotValue",
                "rule eenmalig kaveletiket | form eenmalig kaveletiket | group null",
                "  readings [0 COLLISIONS $botsingen (botsingen)]",
                "  pattern UniquenessKey $sleutel alpha [uniciteit_6: [[] uniqueness VALUE GELIJK eenmalig"
                        + " kaveletiket]] join [] beta [] bind [$element = [] UniquenessKey.element VALUE]",
                "  accumulation UniquenessKey $ander alpha [uniciteit_7: [[] uniqueness VALUE GELIJK eenmalig"
                        + " kaveletiket]] join [] beta [botsing_8: COLLIDES_WITH $sleutel] bind [] functions [COUNT null"
                        + " -> $botsingen]",
                "  test dubbel_9 VORM probe -1 slots [0] size 1: DUPLICATE(Read 0)",
                "    test 0 Call",
                "    test 0.0 SlotValue",
                "  consequence UniquenessViolation [subject=$element, uniqueness=eenmalig kaveletiket, insertion=LOGICAL]",
                "rule eenmalig kaveletiket: kanaal | form eenmalig kaveletiket | group null",
                "  readings []",
                "  pattern UniquenessViolation $schending alpha [schending_10: [[] uniqueness VALUE GELIJK eenmalig"
                        + " kaveletiket]] join [] beta [] bind [$subject = [] UniquenessViolation.element VALUE]",
                "  consequence Inconsistency [subject=$subject, form=eenmalig kaveletiket, codes=[EF-026], insertion=LOGICAL]",
                "rule eenmalig vondstnummer: sleutel van Strandvondst 1 | form eenmalig vondstnummer | group null",
                "  readings [0 INPUT $vondstnummer (eigen ElementKey[objectType=strandvondst,"
                        + " name=vondstnummer, characteristic=false])]",
                "  subject Strandvondst $onderwerp alpha [] join [] bind [$vondstnummer = []"
                        + " Strandvondst.vondstnummer VALUE]",
                "  test voorwaarde_17 VORM probe -1 slots [0] size 1: Compound ALLE 0(EMPTY_TEST(Read 0, Const"
                        + " false))",
                "    test 0 Compound",
                "    test 0.0 Call",
                "    test 0.0.0 SlotValue",
                "    test 0.0.1 Const Boolean",
                "  consequence UniquenessKey [subject=$onderwerp, uniqueness=eenmalig vondstnummer, source=0,"
                        + " components=[Read 0], timeline=-1, scope=-1, slots=[0], size=1, insertion=LOGICAL]",
                "    components 0 0 SlotValue",
                "rule eenmalig vondstnummer: sleutel van Wrakvondst 2 | form eenmalig vondstnummer | group null",
                "  readings [0 INPUT $vondstnummer (eigen ElementKey[objectType=wrakvondst, name=vondstnummer,"
                        + " characteristic=false])]",
                "  subject Wrakvondst $onderwerp alpha [] join [] bind [$vondstnummer = [] Wrakvondst.vondstnummer"
                        + " VALUE]",
                "  test voorwaarde_18 VORM probe -1 slots [0] size 1: Compound ALLE 0(EMPTY_TEST(Read 0, Const"
                        + " false))",
                "    test 0 Compound",
                "    test 0.0 Call",
                "    test 0.0.0 SlotValue",
                "    test 0.0.1 Const Boolean",
                "  consequence UniquenessKey [subject=$onderwerp, uniqueness=eenmalig vondstnummer, source=1,"
                        + " components=[Read 0], timeline=-1, scope=-1, slots=[0], size=1, insertion=LOGICAL]",
                "    components 0 0 SlotValue",
                "rule eenmalig vondstnummer | form eenmalig vondstnummer | group null",
                "  readings [0 COLLISIONS $botsingen (botsingen)]",
                "  pattern UniquenessKey $sleutel alpha [uniciteit_19: [[] uniqueness VALUE GELIJK eenmalig"
                        + " vondstnummer]] join [] beta [] bind [$element = [] UniquenessKey.element VALUE]",
                "  accumulation UniquenessKey $ander alpha [uniciteit_20: [[] uniqueness VALUE GELIJK eenmalig"
                        + " vondstnummer]] join [] beta [botsing_21: COLLIDES_WITH $sleutel] bind [] functions [COUNT null"
                        + " -> $botsingen]",
                "  test dubbel_22 VORM probe -1 slots [0] size 1: DUPLICATE(Read 0)",
                "    test 0 Call",
                "    test 0.0 SlotValue",
                "  consequence UniquenessViolation [subject=$element, uniqueness=eenmalig vondstnummer, insertion=LOGICAL]",
                "rule eenmalig vondstnummer: kanaal | form eenmalig vondstnummer | group null",
                "  readings []",
                "  pattern UniquenessViolation $schending alpha [schending_23: [[] uniqueness VALUE GELIJK eenmalig"
                        + " vondstnummer]] join [] beta [] bind [$subject = [] UniquenessViolation.element VALUE]",
                "  consequence Inconsistency [subject=$subject, form=eenmalig vondstnummer, codes=[EF-026], insertion=LOGICAL]"),
                plans(golden("anchor/jutterswet-kustregister"), List.of("eenmalig kaveletiket",
                        "eenmalig vondstnummer")));
    }

    @Test void a_key_on_a_timeline_and_the_key_form_rule() throws Exception {
        assertEquals(List.of(
                "rule eenmalig nummer: sleutel van Strandvondst 1 | form eenmalig nummer | group null",
                "  readings [0 TIJDLIJN $tijdlijn_0 (tijdlijn ElementKey[objectType=strandvondst,"
                        + " name=vondstnummer, characteristic=false])]",
                "  subject Strandvondst $onderwerp alpha [] join [] bind []",
                "  pattern State $stand_0 alpha [stand_0: [[] attribute VALUE GELIJK"
                        + " ElementKey[objectType=strandvondst, name=vondstnummer, characteristic=false]]] join"
                        + " [stand-onderwerp_1: subject = $onderwerp] beta [] bind [$tijdlijn_0 = [] State.value"
                        + " VALUE]",
                "  test voorwaarde_2 VORM probe -1 slots [0] size 1: Compound ALLE 0(BUCKET_SCALAR(Read 0,"
                        + " Const false))",
                "    test 0 Compound",
                "    test 0.0 Call",
                "    test 0.0.0 SlotValue",
                "    test 0.0.1 Const Boolean",
                "  consequence UniquenessKey [subject=$onderwerp, uniqueness=eenmalig nummer, source=0,"
                        + " components=[Read 0], timeline=0, scope=-1, slots=[0], size=1, insertion=LOGICAL]",
                "    components 0 0 SlotValue",
                "rule eenmalig nummer: sleutelvorm van Strandvondst 1 | form eenmalig nummer | group null",
                "  readings [0 TIJDLIJN $tijdlijn_0 (tijdlijn ElementKey[objectType=strandvondst,"
                        + " name=vondstnummer, characteristic=false])]",
                "  subject Strandvondst $onderwerp alpha [] join [] bind []",
                "  pattern State $stand_0 alpha [stand_3: [[] attribute VALUE GELIJK"
                        + " ElementKey[objectType=strandvondst, name=vondstnummer, characteristic=false]]] join"
                        + " [stand-onderwerp_4: subject = $onderwerp] beta [] bind [$tijdlijn_0 = [] State.value"
                        + " VALUE]",
                "  test voorwaarde_5 VORM probe -1 slots [0] size 1: Compound ALLE 0(BUCKET_SCALAR(Read 0,"
                        + " Const true))",
                "    test 0 Compound",
                "    test 0.0 Call",
                "    test 0.0.0 SlotValue",
                "    test 0.0.1 Const Boolean",
                "  consequence InputFormFault [subject=$onderwerp, before=the ever-unique key \"vondstnummer\" needs the"
                        + " full timeline; , after= supplies only a scalar (ch. 5.9), fault=FaultName[form=eenmalig nummer,"
                        + " rule=eenmalig nummer], insertion=LOGICAL]",
                "rule eenmalig nummer | form eenmalig nummer | group null",
                "  readings [0 COLLISIONS $botsingen (botsingen)]",
                "  pattern UniquenessKey $sleutel alpha [uniciteit_6: [[] uniqueness VALUE GELIJK eenmalig nummer]]"
                        + " join [] beta [] bind [$element = [] UniquenessKey.element VALUE]",
                "  accumulation UniquenessKey $ander alpha [uniciteit_7: [[] uniqueness VALUE GELIJK eenmalig"
                        + " nummer]] join [] beta [botsing_8: COLLIDES_WITH $sleutel] bind [] functions [COUNT null ->"
                        + " $botsingen]",
                "  test dubbel_9 VORM probe -1 slots [0] size 1: DUPLICATE(Read 0)",
                "    test 0 Call",
                "    test 0.0 SlotValue",
                "  consequence UniquenessViolation [subject=$element, uniqueness=eenmalig nummer, insertion=LOGICAL]",
                "rule eenmalig nummer: kanaal | form eenmalig nummer | group null",
                "  readings []",
                "  pattern UniquenessViolation $schending alpha [schending_10: [[] uniqueness VALUE GELIJK eenmalig"
                        + " nummer]] join [] beta [] bind [$subject = [] UniquenessViolation.element VALUE]",
                "  consequence Inconsistency [subject=$subject, form=eenmalig nummer, codes=[EF-026], insertion=LOGICAL]"),
                plans(golden("anchor/jutterswet-strandregister"), List.of("eenmalig nummer")));
    }

    @Test void an_allocation_with_recipients_rest_and_a_deposit() throws Exception {
        assertEquals(List.of(
                "rule verdeel het penningbudget van een penningkas: ontvanger | form verdeel het penningbudget van"
                        + " een penningkas | group null",
                "  readings [0 SUBJECT $onderwerp (onderwerp zelf), 1 INPUT $voorranggewicht (eigen"
                        + " ElementKey[objectType=wachter, name=voorranggewicht, characteristic=false]), 2 INPUT"
                        + " $wachtjaar (eigen ElementKey[objectType=wachter, name=wachtjaar, characteristic=false]), 3"
                        + " INPUT $penningmaximum (eigen ElementKey[objectType=wachter, name=penningmaximum,"
                        + " characteristic=false])]",
                "  pattern Penningkas $vouwonderwerp alpha [] join [] beta [] bind []",
                "  subject Wachter $onderwerp alpha [] join [verdelingsbereik_0: penningkas = $vouwonderwerp] bind"
                        + " [$voorranggewicht = [] Wachter.voorranggewicht VALUE, $wachtjaar = []"
                        + " Wachter.wachtjaar VALUE, $penningmaximum = [] Wachter.penningmaximum VALUE]",
                "  consequence Recipient [subject=$vouwonderwerp, allocation=verdeel het penningbudget van een penningkas,"
                        + " recipient=0, weight=1, key=2, maximum=3, insertion=LOGICAL]",
                "rule verdeel het penningbudget van een penningkas | form verdeel het penningbudget van een"
                        + " penningkas | group null",
                "  readings [0 INPUT $penningbudget (eigen ElementKey[objectType=penningkas,"
                        + " name=penningbudget, characteristic=false]), 1 RECIPIENTS $ontvangers_verdeling_2 (ontvangers"
                        + " verdeel het penningbudget van een penningkas)]",
                "  subject Penningkas $onderwerp alpha [] join [] bind [$penningbudget = []"
                        + " Penningkas.penningbudget VALUE]",
                "  accumulation Recipient $ontvanger_verdeling_1 alpha [ontvanger_3: [[] allocation VALUE GELIJK verdeel"
                        + " het penningbudget van een penningkas]] join [ontvanger-bron_4: source = $onderwerp] beta []"
                        + " bind [] functions [LIST null -> $ontvangers_verdeling_2]",
                "  consequence Payout [subject=$onderwerp, form=verdeel het penningbudget van een penningkas,"
                        + " method=OP_VOLGORDE, pot=0, list=1, weighted=true, capped=true, descending=false,"
                        + " decimals=0, share=ElementKey[objectType=wachter, name=penningtoewijzing,"
                        + " characteristic=false], rest=ElementKey[objectType=penningkas, name=onuitgekeerd budget,"
                        + " characteristic=false], writer=verdeel het penningbudget van een penningkas,"
                        + " insertion=LOGICAL]"),
                plans(golden("jutterswet-rangmaximering"),
                        List.of("verdeel het penningbudget van een penningkas")));
    }

    @Test void an_allocation_whose_source_points_to_the_recipient() throws Exception {
        assertEquals(List.of(
                "rule verdeel de opbrengst van een berging: ontvanger | form verdeel de opbrengst van een berging |"
                        + " group null",
                "  readings [0 SUBJECT $onderwerp (onderwerp zelf)]",
                "  pattern Berging $vouwonderwerp alpha [] join [] beta [] bind []",
                "  subject Hoofdberger $onderwerp alpha [] join [verdelingsbereik_0: null ="
                        + " $vouwonderwerp.Berging.berger] bind []",
                "  consequence Recipient [subject=$vouwonderwerp, allocation=verdeel de opbrengst van een berging,"
                        + " recipient=0, weight=-1, key=-1, maximum=-1, insertion=LOGICAL]",
                "rule verdeel de opbrengst van een berging | form verdeel de opbrengst van een berging | group null",
                "  readings [0 INPUT $opbrengst (eigen ElementKey[objectType=berging, name=opbrengst,"
                        + " characteristic=false]), 1 RECIPIENTS $ontvangers_verdeling_2 (ontvangers verdeel de opbrengst"
                        + " van een berging)]",
                "  subject Berging $onderwerp alpha [] join [] bind [$opbrengst = [] Berging.opbrengst VALUE]",
                "  accumulation Recipient $ontvanger_verdeling_1 alpha [ontvanger_3: [[] allocation VALUE GELIJK verdeel"
                        + " de opbrengst van een berging]] join [ontvanger-bron_4: source = $onderwerp] beta [] bind []"
                        + " functions [LIST null -> $ontvangers_verdeling_2]",
                "  consequence Payout [subject=$onderwerp, form=verdeel de opbrengst van een berging,"
                        + " method=GELIJKE_DELEN, pot=0, list=1, weighted=false, capped=false, descending=false,"
                        + " decimals=0, share=ElementKey[objectType=berger, name=bergingsaandeel,"
                        + " characteristic=false], rest=ElementKey[objectType=berging, name=onverdeelde rest,"
                        + " characteristic=false], writer=verdeel de opbrengst van een berging, insertion=LOGICAL]"),
                plans(sourcePointsToRecipient(), List.of("verdeel de opbrengst van een berging")));
    }

    /** The case of {@link #sourcePointsToRecipient}: three sources, each naming its one berger. The recipients are the
     *  hoofdbergers, so B1 and B3 give their whole yield to H1 and H2, with a rest of 0, and B2, whose berger G1 is no
     *  hoofdberger, has no recipient and distributes nothing (ch. 4.4). The reference interpreter and the ASP backend
     *  both pass these seven expectations. */
    private static final String SOURCE_POINTS_TO_RECIPIENT_CASE = """
            {"format": "uchar-case-v0", "asOfDate": null, "input": {"instances": [
              {"label": "B1", "objectType": "Berging", "attributes": {"opbrengst": {"number": "1001"}},
               "roles": {"berger": ["H1"]}, "characteristics": []},
              {"label": "B2", "objectType": "Berging", "attributes": {"opbrengst": {"number": "700"}},
               "roles": {"berger": ["G1"]}, "characteristics": []},
              {"label": "B3", "objectType": "Berging", "attributes": {"opbrengst": {"number": "300"}},
               "roles": {"berger": ["H2"]}, "characteristics": []},
              {"label": "H1", "objectType": "Hoofdberger", "attributes": {"rang": {"number": "1"}}, "roles": {},
               "characteristics": []},
              {"label": "H2", "objectType": "Hoofdberger", "attributes": {"rang": {"number": "2"}}, "roles": {},
               "characteristics": []},
              {"label": "G1", "objectType": "Berger", "attributes": {}, "roles": {}, "characteristics": []}],
              "parameters": {}},
             "expected": [
              {"kind": "attributeValue", "label": "H1", "attribute": "bergingsaandeel", "value": {"number": "1001"}},
              {"kind": "attributeValue", "label": "B1", "attribute": "onverdeelde rest", "value": {"number": "0"}},
              {"kind": "attributeEmpty", "label": "G1", "attribute": "bergingsaandeel"},
              {"kind": "attributeEmpty", "label": "B2", "attribute": "onverdeelde rest"},
              {"kind": "attributeValue", "label": "H2", "attribute": "bergingsaandeel", "value": {"number": "300"}},
              {"kind": "attributeValue", "label": "B3", "attribute": "onverdeelde rest", "value": {"number": "0"}},
              {"kind": "consistent"}]}
            """;

    /** The plan of the join that reads the reference field on the source, pinned above, fired: the comparison of plans
     *  and of the executable model shows the field the join reads by name but not that its rendering reads it, and no
     *  document of the corpus or the suite has this shape, so the harness does not watch it. A rendering that read
     *  another field of the source would match no recipient and lose the shares. */
    @Test void an_allocation_whose_source_points_to_the_recipient_fires(@TempDir Path map) throws Exception {
        Path ir = map.resolve("ir.json");
        Files.writeString(ir, JSON.writeValueAsString(sourcePointsToRecipient()));
        assertNull(FoldPlanWitnessTest.playCase(ir, JSON.readTree(SOURCE_POINTS_TO_RECIPIENT_CASE)));
    }

    @Test void an_allocation_over_a_relation_that_makes_a_creation() throws Exception {
        assertEquals(List.of(
                "rule verdeel de opbrengst gelijk: ontvanger | form verdeel de opbrengst gelijk | group null",
                "  readings [0 SUBJECT $onderwerp (onderwerp zelf)]",
                "  pattern Berging $vouwonderwerp alpha [] join [] beta [] bind []",
                "  subject Berger $onderwerp alpha [] join [] bind []",
                "  exists [RelationLink $koppeling_bereik_0 alpha [bereik-koppeling_1: [[] relation VALUE GELIJK"
                        + " bergingsdeelname]] join [bereik-lid_2: second = $onderwerp, bereik-drager_3: first ="
                        + " $vouwonderwerp] beta [] bind []]",
                "  consequence Recipient [subject=$vouwonderwerp, allocation=verdeel de opbrengst gelijk, recipient=0,"
                        + " weight=-1, key=-1, maximum=-1, insertion=LOGICAL]",
                "rule verdeel de opbrengst gelijk | form verdeel de opbrengst gelijk | group null",
                "  readings [0 INPUT $opbrengst (eigen ElementKey[objectType=berging, name=opbrengst,"
                        + " characteristic=false]), 1 RECIPIENTS $ontvangers_verdeling_5 (ontvangers verdeel de opbrengst"
                        + " gelijk)]",
                "  subject Berging $onderwerp alpha [] join [] bind [$opbrengst = [] Berging.opbrengst VALUE]",
                "  accumulation Recipient $ontvanger_verdeling_4 alpha [ontvanger_6: [[] allocation VALUE GELIJK verdeel"
                        + " de opbrengst gelijk]] join [ontvanger-bron_7: source = $onderwerp] beta [] bind [] functions"
                        + " [LIST null -> $ontvangers_verdeling_5]",
                "  consequence Payout [subject=$onderwerp, form=verdeel de opbrengst gelijk, method=GELIJKE_DELEN,"
                        + " pot=0, list=1, weighted=false, capped=false, descending=false, decimals=0,"
                        + " share=ElementKey[objectType=berger, name=bergingsaandeel, characteristic=false],"
                        + " rest=ElementKey[objectType=berging, name=onverdeelde rest, characteristic=false],"
                        + " writer=verdeel de opbrengst gelijk, insertion=LOGICAL]"),
                plans(recipientFromCreation(), List.of("verdeel de opbrengst gelijk")));
    }

    /** {@code jutterswet-bergersdeling} with both roles of its relation to-one, since no golden has an allocation whose
     *  recipient role is to-one: the source's bean then holds the reference to its one recipient, so the recipient
     *  rule joins the subject itself to that reference field of the source. Verifier-clean. */
    static JsonNode sourcePointsToRecipient() throws Exception {
        JsonNode d = golden("jutterswet-bergersdeling");
        for (JsonNode role : d.get("model").get("factTypes").get(0).get("roles")) {
            ((ObjectNode) role).put("many", false);
            ((ObjectNode) role).remove("plural");
        }
        return d;
    }

    /** Document F of {@code TmsAllocationWitnessTest}, which plays it: an allocation whose recipients an object creation
     *  adds over the allocation's connecting relation, so its range is a relation a form creates. Taken from that
     *  witness by reflection, as its members are of its own package, so that the document has one source. */
    static JsonNode recipientFromCreation() throws Exception {
        Class<?> f = Class.forName("dev.uchar.regelspraak.cases.TmsAllocationWitnessTest");
        Method ir = f.getDeclaredMethod("ir", List.class, List.class, List.class);
        ir.setAccessible(true);
        return document((String) ir.invoke(null, List.of(), List.of(field(f, "F_EQUAL")), List.of(field(f,
                "HELPER_BERGER"))));
    }

    private static String field(Class<?> javaClass, String name) throws ReflectiveOperationException {
        Field f = javaClass.getDeclaredField(name);
        f.setAccessible(true);
        return (String) f.get(null);
    }

    /** Every plan of {@code document} of a form named in {@code forms}, and of the standing of its channel, in the
     *  order of the model, as text; after a line that holds a term, the term node by node. */
    static List<String> plans(JsonNode document, List<String> forms) throws ReflectiveOperationException {
        List<String> out = new ArrayList<>();
        for (RulePlan p : DroolsBackend.build(document, DroolsBackend.Branch.EXEC_MODEL).execModelTranspiler()
                .modelplan().rules()) {
            if (forms.stream().noneMatch(v -> p.name().equals(v) || p.name().startsWith(v + ": ")
                    || p.name().startsWith("kanaalstand " + v + ": ")))
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
            out.add("  consequence " + consequence(p.consequence()));
            for (RecordComponent c : p.consequence().getClass().getRecordComponents()) {
                Object w = c.getAccessor().invoke(p.consequence());
                if (w instanceof Term t) TermTree.fields("    " + c.getName(), t, "0", out);
                if (w instanceof List<?> l)
                    for (int i = 0; i < l.size(); i++)
                        if (l.get(i) instanceof Term t) TermTree.fields("    " + c.getName() + " " + i, t, "0", out);
            }
        }
        return out;
    }

    static JsonNode golden(String name) throws Exception {
        return JSON.readTree(GOLDEN.resolve(name).resolve("ir.json").toFile());
    }

    static JsonNode document(String ir) throws Exception {
        return JSON.readTree(ir);
    }

    private static String item(LhsItem item) {
        return switch (item) {
            case Subject o -> "subject " + o.beanClass().getSimpleName() + " " + o.name() + " alpha "
                    + alpha(o.alpha()) + " join " + joins(o.joins()) + " bind " + bindings(o.bindings());
            case FactPattern p -> "pattern " + pattern(p);
            case Not n -> "not " + (n.patterns().size() == 1 ? pattern(n.patterns().get(0)) : "inside "
                    + n.inside() + " outside " + n.outside() + " "
                    + n.patterns().stream().map(ChannelPlanWitnessTest::pattern).toList());
            case Exists b -> "exists " + b.patterns().stream().map(ChannelPlanWitnessTest::pattern).toList();
            case Accumulation a -> "accumulation " + pattern(a.source()) + " functions "
                    + a.aggregations().stream().map(f -> f.kind() + " " + f.source() + " -> " + f.target()).toList();
            case TestItem t -> "test " + t.id() + " " + t.role() + " probe " + t.depositFlag() + " slots " + t.slots()
                    + " size " + t.size() + ": " + TermTree.tree(t.test());
        };
    }

    private static String pattern(FactPattern p) {
        return p.factClass().getSimpleName() + " " + p.name() + " alpha " + alpha(p.alpha()) + " join "
                + joins(p.joins()) + " beta " + p.betaTests().stream().map(t -> t.id() + ": " + t.function() + " "
                + t.target()).toList() + " bind " + bindings(p.bindings());
    }

    /** Each identity join with its id: the field of the pattern's object (null: the object itself), and the variable
     *  on the other side with the reference field read there, where it reads one. */
    private static List<String> joins(List<Join> joins) {
        return joins.stream().map(j -> j.id() + ": " + j.field() + " = " + j.target()
                + (j.targetField() == null ? "" : "." + field(j.targetField()))).toList();
    }

    /** Each constraint with its id and its field tests: the path, the field (null: the object itself), the form of
     *  reading, the comparison and the value. */
    private static List<String> alpha(List<Alpha> alpha) {
        List<String> out = new ArrayList<>();
        for (Alpha a : alpha)
            out.add(a.id() + ": " + a.tests().stream().map(t -> fields(t.path()) + " " + t.field() + " " + t.form()
                    + " " + t.op() + (t.value() == null ? "" : " " + t.value())).toList());
        return out;
    }

    /** Each binding with its variable, its path, its field on its class and its form of reading. */
    private static List<String> bindings(List<Binding> bindings) {
        return bindings.stream().map(b -> b.target() + " = " + fields(b.path()) + " " + field(b.field()) + " " + b.form()
                + (b.category() == null ? "" : " " + b.category().getSimpleName())).toList();
    }

    private static String fields(List<Binding.Field> path) {
        return path.stream().map(ChannelPlanWitnessTest::field).toList().toString();
    }

    private static String field(Binding.Field v) {
        return v.javaClass().getSimpleName() + "." + v.name();
    }

    /** The kind of {@code g} and every field of it, a term as a tree. */
    private static String consequence(Consequence g) throws ReflectiveOperationException {
        List<String> fields = new ArrayList<>();
        for (RecordComponent c : g.getClass().getRecordComponents()) {
            Object w = c.getAccessor().invoke(g);
            fields.add(c.getName() + "=" + (w instanceof Term t ? TermTree.tree(t) : w instanceof List<?> l
                    && !l.isEmpty() && l.get(0) instanceof Term ? l.stream().map(x -> TermTree.tree((Term) x)).toList()
                    : String.valueOf(w)));
        }
        return g.getClass().getSimpleName() + " " + fields;
    }
}
