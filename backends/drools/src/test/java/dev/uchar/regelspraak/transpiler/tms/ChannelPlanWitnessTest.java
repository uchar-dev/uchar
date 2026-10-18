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
                "regel aanmelding niet voor de vondstdag | vorm aanmelding niet voor de vondstdag | groep null",
                "  lezingen [0 INVOER $aanmelddatum (eigen ElementSleutel[objecttype=vondst, naam=aanmelddatum,"
                        + " kenmerk=false]), 1 INVOER $vondstdag (eigen ElementSleutel[objecttype=vondst,"
                        + " naam=vondstdag, kenmerk=false])]",
                "  onderwerp Vondst $onderwerp alfa [] join [] bind [$aanmelddatum = [] Vondst.aanmelddatum VALUE,"
                        + " $vondstdag = [] Vondst.vondstdag VALUE]",
                "  toets voorwaarde_0 CONDITION sonde -1 slots [0, 1] grootte 2: NIET(DATE_COMPARE(Lees 0, Lees 1,"
                        + " Const LATER_OF_GELIJK))",
                "    toets 0 Call bron 'De aanmelddatum van een Vondst moet later of gelijk zijn aan de vondstdag"
                        + " van de Vondst'",
                "    toets 0.0 Call bron 'De aanmelddatum van een Vondst moet later of gelijk zijn aan de vondstdag"
                        + " van de Vondst'",
                "    toets 0.0.0 SlotValue bron 'De aanmelddatum van een Vondst'",
                "    toets 0.0.1 SlotValue bron 'de vondstdag van de Vondst'",
                "    toets 0.0.2 Const Operator",
                "  gevolg Inconsistency [onderwerp=$onderwerp, vorm=aanmelding niet voor de vondstdag,"
                        + " insertion=LOGICAL]",
                "regel eenmalig vondstnummer: sleutel van Vondst 1 | vorm eenmalig vondstnummer | groep null",
                "  lezingen [0 INVOER $vondstnummer (eigen ElementSleutel[objecttype=vondst, naam=vondstnummer,"
                        + " kenmerk=false])]",
                "  onderwerp Vondst $onderwerp alfa [] join [] bind [$vondstnummer = [] Vondst.vondstnummer VALUE]",
                "  toets voorwaarde_1 VORM sonde -1 slots [0] grootte 1: Samengesteld ALLE 0(EMPTY_TEST(Lees 0, Const"
                        + " false))",
                "    toets 0 Compound",
                "    toets 0.0 Call",
                "    toets 0.0.0 SlotValue",
                "    toets 0.0.1 Const Boolean",
                "  gevolg UniquenessKey [onderwerp=$onderwerp, uniqueness=eenmalig vondstnummer, bron=0,"
                        + " components=[Lees 0], timeline=-1, scope=-1, slots=[0], size=1, insertion=LOGICAL]",
                "    components 0 0 SlotValue",
                "regel eenmalig vondstnummer | vorm eenmalig vondstnummer | groep null",
                "  lezingen [0 COLLISIONS $botsingen (botsingen)]",
                "  patroon UniquenessKey $sleutel alfa [uniciteit_2: [[] uniqueness VALUE GELIJK eenmalig"
                        + " vondstnummer]] join [] beta [] bind [$element = [] UniquenessKey.element VALUE]",
                "  accumulatie UniquenessKey $ander alfa [uniciteit_3: [[] uniqueness VALUE GELIJK eenmalig"
                        + " vondstnummer]] join [] beta [botsing_4: COLLIDES_WITH $sleutel] bind [] functies [COUNT null"
                        + " -> $botsingen]",
                "  toets dubbel_5 VORM sonde -1 slots [0] grootte 1: DUPLICATE(Lees 0)",
                "    toets 0 Call",
                "    toets 0.0 SlotValue",
                "  gevolg UniquenessViolation [onderwerp=$element, uniqueness=eenmalig vondstnummer, insertion=LOGICAL]",
                "regel eenmalig vondstnummer: kanaal | vorm eenmalig vondstnummer | groep null",
                "  lezingen []",
                "  patroon UniquenessViolation $schending alfa [schending_6: [[] uniqueness VALUE GELIJK eenmalig"
                        + " vondstnummer]] join [] beta [] bind [$subject = [] UniquenessViolation.element VALUE]",
                "  gevolg Inconsistency [onderwerp=$subject, vorm=eenmalig vondstnummer, insertion=LOGICAL]"),
                plans(golden("jutterswet-vondstnummer"), List.of("aanmelding niet voor de vondstdag",
                        "eenmalig vondstnummer")));
    }

    @Test void a_uniqueness_over_an_enumeration() throws Exception {
        assertEquals(List.of(
                "regel eenmalige staat: sleutel van Vondst 1 | vorm eenmalige staat | groep null",
                "  lezingen [0 INVOER $staat (eigen ElementSleutel[objecttype=vondst, naam=staat, kenmerk=false])]",
                "  onderwerp Vondst $onderwerp alfa [] join [] bind [$staat = [] Vondst.staat VALUE]",
                "  toets voorwaarde_5 VORM sonde -1 slots [0] grootte 1: Samengesteld ALLE 0(EMPTY_TEST(Lees 0, Const"
                        + " false))",
                "    toets 0 Compound",
                "    toets 0.0 Call",
                "    toets 0.0.0 SlotValue",
                "    toets 0.0.1 Const Boolean",
                "  gevolg UniquenessKey [onderwerp=$onderwerp, uniqueness=eenmalige staat, bron=0,"
                        + " components=[RS_ENUM_NAME(Lees 0)], timeline=-1, scope=-1, slots=[0], size=1,"
                        + " insertion=LOGICAL]",
                "    components 0 0 Call",
                "    components 0 0.0 SlotValue",
                "regel eenmalige staat | vorm eenmalige staat | groep null",
                "  lezingen [0 COLLISIONS $botsingen (botsingen)]",
                "  patroon UniquenessKey $sleutel alfa [uniciteit_6: [[] uniqueness VALUE GELIJK eenmalige staat]]"
                        + " join [] beta [] bind [$element = [] UniquenessKey.element VALUE]",
                "  accumulatie UniquenessKey $ander alfa [uniciteit_7: [[] uniqueness VALUE GELIJK eenmalige"
                        + " staat]] join [] beta [botsing_8: COLLIDES_WITH $sleutel] bind [] functies [COUNT null ->"
                        + " $botsingen]",
                "  toets dubbel_9 VORM sonde -1 slots [0] grootte 1: DUPLICATE(Lees 0)",
                "    toets 0 Call",
                "    toets 0.0 SlotValue",
                "  gevolg UniquenessViolation [onderwerp=$element, uniqueness=eenmalige staat, insertion=LOGICAL]",
                "regel eenmalige staat: kanaal | vorm eenmalige staat | groep null",
                "  lezingen []",
                "  patroon UniquenessViolation $schending alfa [schending_10: [[] uniqueness VALUE GELIJK eenmalige"
                        + " staat]] join [] beta [] bind [$subject = [] UniquenessViolation.element VALUE]",
                "  gevolg Inconsistency [onderwerp=$subject, vorm=eenmalige staat, insertion=LOGICAL]"),
                plans(golden("jutterswet-keurspelling"), List.of("eenmalige staat")));
    }

    @Test void a_consistency_and_the_two_channel_states() throws Exception {
        assertEquals(List.of(
                "regel controle saldo | vorm controle saldo | groep null",
                "  lezingen [0 INVOER $saldo (eigen ElementSleutel[objecttype=rekeninghouder, naam=saldo,"
                        + " kenmerk=false])]",
                "  onderwerp Rekeninghouder $onderwerp alfa [] join [] bind [$saldo = [] Rekeninghouder.saldo"
                        + " VALUE]",
                "  toets voorwaarde_2 CONDITION sonde -1 slots [0] grootte 1: NIET(RS_AGG_COMPARE(Lees 0, Const 0,"
                        + " Const >=))",
                "    toets 0 Call bron 'zijn saldo is groter dan of gelijk aan 0'",
                "    toets 0.0 Call bron 'zijn saldo is groter dan of gelijk aan 0'",
                "    toets 0.0.0 SlotValue bron 'zijn saldo'",
                "    toets 0.0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '0'",
                "    toets 0.0.2 Const String",
                "  gevolg Inconsistency [onderwerp=$onderwerp, vorm=controle saldo, insertion=LOGICAL]",
                "regel kanaalstand controle saldo: inconsistent | vorm null | groep null",
                "  lezingen []",
                "  bestaat [Inconsistentie $inconsistentie alfa [kanaal_3: [[] regel VALUE GELIJK controle saldo]]"
                        + " join [] beta [] bind []]",
                "  gevolg InsertChannelState [producer=controle saldo, inconsistent=true, insertion=LOGICAL]",
                "regel kanaalstand controle saldo: consistent | vorm null | groep null",
                "  lezingen []",
                "  niet Inconsistentie $inconsistentie alfa [kanaal_4: [[] regel VALUE GELIJK controle saldo]] join"
                        + " [] beta [] bind []",
                "  gevolg InsertChannelState [producer=controle saldo, inconsistent=false, insertion=LOGICAL]"),
                plans(document(TestTermWitnessTest.TOEZICHT), List.of("controle saldo")));
    }

    @Test void a_uniqueness_with_scope_and_a_union() throws Exception {
        assertEquals(List.of(
                "regel eenmalig kaveletiket: sleutel van Kavel 1 | vorm eenmalig kaveletiket | groep null",
                "  lezingen [0 INVOER $etiket (eigen ElementSleutel[objecttype=kavel, naam=etiket, kenmerk=false]),"
                        + " 1 SCOPE $gemeente (scope kavelindeling gemeente)]",
                "  onderwerp Kavel $onderwerp alfa [] join [] bind [$etiket = [] Kavel.etiket VALUE, $gemeente ="
                        + " [] Kavel.gemeente VALUE]",
                "  toets voorwaarde_5 VORM sonde -1 slots [0, 1] grootte 2: Samengesteld ALLE 0(EMPTY_TEST(Lees 0,"
                        + " Const false), EMPTY_TEST(Lees 1, Const false))",
                "    toets 0 Compound",
                "    toets 0.0 Call",
                "    toets 0.0.0 SlotValue",
                "    toets 0.0.1 Const Boolean",
                "    toets 0.1 Call",
                "    toets 0.1.0 SlotValue",
                "    toets 0.1.1 Const Boolean",
                "  gevolg UniquenessKey [onderwerp=$onderwerp, uniqueness=eenmalig kaveletiket, bron=0,"
                        + " components=[Lees 0], timeline=-1, scope=1, slots=[0, 1], size=2, insertion=LOGICAL]",
                "    components 0 0 SlotValue",
                "regel eenmalig kaveletiket | vorm eenmalig kaveletiket | groep null",
                "  lezingen [0 COLLISIONS $botsingen (botsingen)]",
                "  patroon UniquenessKey $sleutel alfa [uniciteit_6: [[] uniqueness VALUE GELIJK eenmalig"
                        + " kaveletiket]] join [] beta [] bind [$element = [] UniquenessKey.element VALUE]",
                "  accumulatie UniquenessKey $ander alfa [uniciteit_7: [[] uniqueness VALUE GELIJK eenmalig"
                        + " kaveletiket]] join [] beta [botsing_8: COLLIDES_WITH $sleutel] bind [] functies [COUNT null"
                        + " -> $botsingen]",
                "  toets dubbel_9 VORM sonde -1 slots [0] grootte 1: DUPLICATE(Lees 0)",
                "    toets 0 Call",
                "    toets 0.0 SlotValue",
                "  gevolg UniquenessViolation [onderwerp=$element, uniqueness=eenmalig kaveletiket, insertion=LOGICAL]",
                "regel eenmalig kaveletiket: kanaal | vorm eenmalig kaveletiket | groep null",
                "  lezingen []",
                "  patroon UniquenessViolation $schending alfa [schending_10: [[] uniqueness VALUE GELIJK eenmalig"
                        + " kaveletiket]] join [] beta [] bind [$subject = [] UniquenessViolation.element VALUE]",
                "  gevolg Inconsistency [onderwerp=$subject, vorm=eenmalig kaveletiket, insertion=LOGICAL]",
                "regel eenmalig vondstnummer: sleutel van Strandvondst 1 | vorm eenmalig vondstnummer | groep null",
                "  lezingen [0 INVOER $vondstnummer (eigen ElementSleutel[objecttype=strandvondst,"
                        + " naam=vondstnummer, kenmerk=false])]",
                "  onderwerp Strandvondst $onderwerp alfa [] join [] bind [$vondstnummer = []"
                        + " Strandvondst.vondstnummer VALUE]",
                "  toets voorwaarde_17 VORM sonde -1 slots [0] grootte 1: Samengesteld ALLE 0(EMPTY_TEST(Lees 0, Const"
                        + " false))",
                "    toets 0 Compound",
                "    toets 0.0 Call",
                "    toets 0.0.0 SlotValue",
                "    toets 0.0.1 Const Boolean",
                "  gevolg UniquenessKey [onderwerp=$onderwerp, uniqueness=eenmalig vondstnummer, bron=0,"
                        + " components=[Lees 0], timeline=-1, scope=-1, slots=[0], size=1, insertion=LOGICAL]",
                "    components 0 0 SlotValue",
                "regel eenmalig vondstnummer: sleutel van Wrakvondst 2 | vorm eenmalig vondstnummer | groep null",
                "  lezingen [0 INVOER $vondstnummer (eigen ElementSleutel[objecttype=wrakvondst, naam=vondstnummer,"
                        + " kenmerk=false])]",
                "  onderwerp Wrakvondst $onderwerp alfa [] join [] bind [$vondstnummer = [] Wrakvondst.vondstnummer"
                        + " VALUE]",
                "  toets voorwaarde_18 VORM sonde -1 slots [0] grootte 1: Samengesteld ALLE 0(EMPTY_TEST(Lees 0, Const"
                        + " false))",
                "    toets 0 Compound",
                "    toets 0.0 Call",
                "    toets 0.0.0 SlotValue",
                "    toets 0.0.1 Const Boolean",
                "  gevolg UniquenessKey [onderwerp=$onderwerp, uniqueness=eenmalig vondstnummer, bron=1,"
                        + " components=[Lees 0], timeline=-1, scope=-1, slots=[0], size=1, insertion=LOGICAL]",
                "    components 0 0 SlotValue",
                "regel eenmalig vondstnummer | vorm eenmalig vondstnummer | groep null",
                "  lezingen [0 COLLISIONS $botsingen (botsingen)]",
                "  patroon UniquenessKey $sleutel alfa [uniciteit_19: [[] uniqueness VALUE GELIJK eenmalig"
                        + " vondstnummer]] join [] beta [] bind [$element = [] UniquenessKey.element VALUE]",
                "  accumulatie UniquenessKey $ander alfa [uniciteit_20: [[] uniqueness VALUE GELIJK eenmalig"
                        + " vondstnummer]] join [] beta [botsing_21: COLLIDES_WITH $sleutel] bind [] functies [COUNT null"
                        + " -> $botsingen]",
                "  toets dubbel_22 VORM sonde -1 slots [0] grootte 1: DUPLICATE(Lees 0)",
                "    toets 0 Call",
                "    toets 0.0 SlotValue",
                "  gevolg UniquenessViolation [onderwerp=$element, uniqueness=eenmalig vondstnummer, insertion=LOGICAL]",
                "regel eenmalig vondstnummer: kanaal | vorm eenmalig vondstnummer | groep null",
                "  lezingen []",
                "  patroon UniquenessViolation $schending alfa [schending_23: [[] uniqueness VALUE GELIJK eenmalig"
                        + " vondstnummer]] join [] beta [] bind [$subject = [] UniquenessViolation.element VALUE]",
                "  gevolg Inconsistency [onderwerp=$subject, vorm=eenmalig vondstnummer, insertion=LOGICAL]"),
                plans(golden("anchor/jutterswet-kustregister"), List.of("eenmalig kaveletiket",
                        "eenmalig vondstnummer")));
    }

    @Test void a_key_on_a_timeline_and_the_key_form_rule() throws Exception {
        assertEquals(List.of(
                "regel eenmalig nummer: sleutel van Strandvondst 1 | vorm eenmalig nummer | groep null",
                "  lezingen [0 TIJDLIJN $tijdlijn_0 (tijdlijn ElementSleutel[objecttype=strandvondst,"
                        + " naam=vondstnummer, kenmerk=false])]",
                "  onderwerp Strandvondst $onderwerp alfa [] join [] bind []",
                "  patroon State $stand_0 alfa [stand_0: [[] attribuut VALUE GELIJK"
                        + " ElementSleutel[objecttype=strandvondst, naam=vondstnummer, kenmerk=false]]] join"
                        + " [stand-onderwerp_1: onderwerp = $onderwerp] beta [] bind [$tijdlijn_0 = [] State.waarde"
                        + " VALUE]",
                "  toets voorwaarde_2 VORM sonde -1 slots [0] grootte 1: Samengesteld ALLE 0(BUCKET_SCALAR(Lees 0,"
                        + " Const false))",
                "    toets 0 Compound",
                "    toets 0.0 Call",
                "    toets 0.0.0 SlotValue",
                "    toets 0.0.1 Const Boolean",
                "  gevolg UniquenessKey [onderwerp=$onderwerp, uniqueness=eenmalig nummer, bron=0,"
                        + " components=[Lees 0], timeline=0, scope=-1, slots=[0], size=1, insertion=LOGICAL]",
                "    components 0 0 SlotValue",
                "regel eenmalig nummer: sleutelvorm van Strandvondst 1 | vorm eenmalig nummer | groep null",
                "  lezingen [0 TIJDLIJN $tijdlijn_0 (tijdlijn ElementSleutel[objecttype=strandvondst,"
                        + " naam=vondstnummer, kenmerk=false])]",
                "  onderwerp Strandvondst $onderwerp alfa [] join [] bind []",
                "  patroon State $stand_0 alfa [stand_3: [[] attribuut VALUE GELIJK"
                        + " ElementSleutel[objecttype=strandvondst, naam=vondstnummer, kenmerk=false]]] join"
                        + " [stand-onderwerp_4: onderwerp = $onderwerp] beta [] bind [$tijdlijn_0 = [] State.waarde"
                        + " VALUE]",
                "  toets voorwaarde_5 VORM sonde -1 slots [0] grootte 1: Samengesteld ALLE 0(BUCKET_SCALAR(Lees 0,"
                        + " Const true))",
                "    toets 0 Compound",
                "    toets 0.0 Call",
                "    toets 0.0.0 SlotValue",
                "    toets 0.0.1 Const Boolean",
                "  gevolg Invoervormfout [onderwerp=$onderwerp, before=the ever-unique key \"vondstnummer\" needs the"
                        + " full timeline; , after= supplies only a scalar (ch. 5.9), fout=FaultName[vorm=eenmalig nummer,"
                        + " regel=eenmalig nummer], insertion=LOGICAL]",
                "regel eenmalig nummer | vorm eenmalig nummer | groep null",
                "  lezingen [0 COLLISIONS $botsingen (botsingen)]",
                "  patroon UniquenessKey $sleutel alfa [uniciteit_6: [[] uniqueness VALUE GELIJK eenmalig nummer]]"
                        + " join [] beta [] bind [$element = [] UniquenessKey.element VALUE]",
                "  accumulatie UniquenessKey $ander alfa [uniciteit_7: [[] uniqueness VALUE GELIJK eenmalig"
                        + " nummer]] join [] beta [botsing_8: COLLIDES_WITH $sleutel] bind [] functies [COUNT null ->"
                        + " $botsingen]",
                "  toets dubbel_9 VORM sonde -1 slots [0] grootte 1: DUPLICATE(Lees 0)",
                "    toets 0 Call",
                "    toets 0.0 SlotValue",
                "  gevolg UniquenessViolation [onderwerp=$element, uniqueness=eenmalig nummer, insertion=LOGICAL]",
                "regel eenmalig nummer: kanaal | vorm eenmalig nummer | groep null",
                "  lezingen []",
                "  patroon UniquenessViolation $schending alfa [schending_10: [[] uniqueness VALUE GELIJK eenmalig"
                        + " nummer]] join [] beta [] bind [$subject = [] UniquenessViolation.element VALUE]",
                "  gevolg Inconsistency [onderwerp=$subject, vorm=eenmalig nummer, insertion=LOGICAL]"),
                plans(golden("anchor/jutterswet-strandregister"), List.of("eenmalig nummer")));
    }

    @Test void an_allocation_with_recipients_rest_and_a_deposit() throws Exception {
        assertEquals(List.of(
                "regel verdeel het penningbudget van een penningkas: ontvanger | vorm verdeel het penningbudget van"
                        + " een penningkas | groep null",
                "  lezingen [0 SUBJECT $onderwerp (onderwerp zelf), 1 INVOER $voorranggewicht (eigen"
                        + " ElementSleutel[objecttype=wachter, naam=voorranggewicht, kenmerk=false]), 2 INVOER"
                        + " $wachtjaar (eigen ElementSleutel[objecttype=wachter, naam=wachtjaar, kenmerk=false]), 3"
                        + " INVOER $penningmaximum (eigen ElementSleutel[objecttype=wachter, naam=penningmaximum,"
                        + " kenmerk=false])]",
                "  patroon Penningkas $vouwonderwerp alfa [] join [] beta [] bind []",
                "  onderwerp Wachter $onderwerp alfa [] join [verdelingsbereik_0: penningkas = $vouwonderwerp] bind"
                        + " [$voorranggewicht = [] Wachter.voorranggewicht VALUE, $wachtjaar = []"
                        + " Wachter.wachtjaar VALUE, $penningmaximum = [] Wachter.penningmaximum VALUE]",
                "  gevolg Recipient [onderwerp=$vouwonderwerp, allocation=verdeel het penningbudget van een penningkas,"
                        + " recipient=0, weight=1, sleutel=2, maximum=3, insertion=LOGICAL]",
                "regel verdeel het penningbudget van een penningkas | vorm verdeel het penningbudget van een"
                        + " penningkas | groep null",
                "  lezingen [0 INVOER $penningbudget (eigen ElementSleutel[objecttype=penningkas,"
                        + " naam=penningbudget, kenmerk=false]), 1 RECIPIENTS $ontvangers_verdeling_2 (ontvangers"
                        + " verdeel het penningbudget van een penningkas)]",
                "  onderwerp Penningkas $onderwerp alfa [] join [] bind [$penningbudget = []"
                        + " Penningkas.penningbudget VALUE]",
                "  accumulatie Recipient $ontvanger_verdeling_1 alfa [ontvanger_3: [[] allocation VALUE GELIJK verdeel"
                        + " het penningbudget van een penningkas]] join [ontvanger-bron_4: bron = $onderwerp] beta []"
                        + " bind [] functies [LIST null -> $ontvangers_verdeling_2]",
                "  gevolg Payout [onderwerp=$onderwerp, vorm=verdeel het penningbudget van een penningkas,"
                        + " methode=OP_VOLGORDE, pot=0, lijst=1, gewogen=true, capped=true, descending=false,"
                        + " decimals=0, aandeel=ElementSleutel[objecttype=wachter, naam=penningtoewijzing,"
                        + " kenmerk=false], rest=ElementSleutel[objecttype=penningkas, naam=onuitgekeerd budget,"
                        + " kenmerk=false], writer=verdeel het penningbudget van een penningkas,"
                        + " insertion=LOGICAL]"),
                plans(golden("jutterswet-rangmaximering"),
                        List.of("verdeel het penningbudget van een penningkas")));
    }

    @Test void an_allocation_whose_source_points_to_the_recipient() throws Exception {
        assertEquals(List.of(
                "regel verdeel de opbrengst van een berging: ontvanger | vorm verdeel de opbrengst van een berging |"
                        + " groep null",
                "  lezingen [0 SUBJECT $onderwerp (onderwerp zelf)]",
                "  patroon Berging $vouwonderwerp alfa [] join [] beta [] bind []",
                "  onderwerp Hoofdberger $onderwerp alfa [] join [verdelingsbereik_0: null ="
                        + " $vouwonderwerp.Berging.berger] bind []",
                "  gevolg Recipient [onderwerp=$vouwonderwerp, allocation=verdeel de opbrengst van een berging,"
                        + " recipient=0, weight=-1, sleutel=-1, maximum=-1, insertion=LOGICAL]",
                "regel verdeel de opbrengst van een berging | vorm verdeel de opbrengst van een berging | groep null",
                "  lezingen [0 INVOER $opbrengst (eigen ElementSleutel[objecttype=berging, naam=opbrengst,"
                        + " kenmerk=false]), 1 RECIPIENTS $ontvangers_verdeling_2 (ontvangers verdeel de opbrengst"
                        + " van een berging)]",
                "  onderwerp Berging $onderwerp alfa [] join [] bind [$opbrengst = [] Berging.opbrengst VALUE]",
                "  accumulatie Recipient $ontvanger_verdeling_1 alfa [ontvanger_3: [[] allocation VALUE GELIJK verdeel"
                        + " de opbrengst van een berging]] join [ontvanger-bron_4: bron = $onderwerp] beta [] bind []"
                        + " functies [LIST null -> $ontvangers_verdeling_2]",
                "  gevolg Payout [onderwerp=$onderwerp, vorm=verdeel de opbrengst van een berging,"
                        + " methode=GELIJKE_DELEN, pot=0, lijst=1, gewogen=false, capped=false, descending=false,"
                        + " decimals=0, aandeel=ElementSleutel[objecttype=berger, naam=bergingsaandeel,"
                        + " kenmerk=false], rest=ElementSleutel[objecttype=berging, naam=onverdeelde rest,"
                        + " kenmerk=false], writer=verdeel de opbrengst van een berging, insertion=LOGICAL]"),
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
                "regel verdeel de opbrengst gelijk: ontvanger | vorm verdeel de opbrengst gelijk | groep null",
                "  lezingen [0 SUBJECT $onderwerp (onderwerp zelf)]",
                "  patroon Berging $vouwonderwerp alfa [] join [] beta [] bind []",
                "  onderwerp Berger $onderwerp alfa [] join [] bind []",
                "  bestaat [RelationLink $koppeling_bereik_0 alfa [bereik-koppeling_1: [[] relation VALUE GELIJK"
                        + " bergingsdeelname]] join [bereik-lid_2: tweede = $onderwerp, bereik-drager_3: eerste ="
                        + " $vouwonderwerp] beta [] bind []]",
                "  gevolg Recipient [onderwerp=$vouwonderwerp, allocation=verdeel de opbrengst gelijk, recipient=0,"
                        + " weight=-1, sleutel=-1, maximum=-1, insertion=LOGICAL]",
                "regel verdeel de opbrengst gelijk | vorm verdeel de opbrengst gelijk | groep null",
                "  lezingen [0 INVOER $opbrengst (eigen ElementSleutel[objecttype=berging, naam=opbrengst,"
                        + " kenmerk=false]), 1 RECIPIENTS $ontvangers_verdeling_5 (ontvangers verdeel de opbrengst"
                        + " gelijk)]",
                "  onderwerp Berging $onderwerp alfa [] join [] bind [$opbrengst = [] Berging.opbrengst VALUE]",
                "  accumulatie Recipient $ontvanger_verdeling_4 alfa [ontvanger_6: [[] allocation VALUE GELIJK verdeel"
                        + " de opbrengst gelijk]] join [ontvanger-bron_7: bron = $onderwerp] beta [] bind [] functies"
                        + " [LIST null -> $ontvangers_verdeling_5]",
                "  gevolg Payout [onderwerp=$onderwerp, vorm=verdeel de opbrengst gelijk, methode=GELIJKE_DELEN,"
                        + " pot=0, lijst=1, gewogen=false, capped=false, descending=false, decimals=0,"
                        + " aandeel=ElementSleutel[objecttype=berger, naam=bergingsaandeel, kenmerk=false],"
                        + " rest=ElementSleutel[objecttype=berging, naam=onverdeelde rest, kenmerk=false],"
                        + " writer=verdeel de opbrengst gelijk, insertion=LOGICAL]"),
                plans(recipientFromCreation(), List.of("verdeel de opbrengst gelijk")));
    }

    /** {@code jutterswet-bergersdeling} with both roles of its relation to-one, since no golden has an allocation whose
     *  recipient role is to-one: the source's bean then holds the reference to its one recipient, so the recipient
     *  rule joins the subject itself to that reference field of the source. Verifier-clean. */
    static JsonNode sourcePointsToRecipient() throws Exception {
        JsonNode d = golden("jutterswet-bergersdeling");
        for (JsonNode rol : d.get("model").get("factTypes").get(0).get("roles")) {
            ((ObjectNode) rol).put("many", false);
            ((ObjectNode) rol).remove("plural");
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
        return document((String) ir.invoke(null, List.of(), List.of(veld(f, "F_EQUAL")), List.of(veld(f,
                "HELPER_BERGER"))));
    }

    private static String veld(Class<?> javaClass, String naam) throws ReflectiveOperationException {
        Field f = javaClass.getDeclaredField(naam);
        f.setAccessible(true);
        return (String) f.get(null);
    }

    /** Every plan of {@code document} of a form named in {@code vormen}, and of the standing of its channel, in the
     *  order of the model, as text; after a line that holds a term, the term node by node. */
    static List<String> plans(JsonNode document, List<String> vormen) throws ReflectiveOperationException {
        List<String> out = new ArrayList<>();
        for (RulePlan p : DroolsBackend.build(document, DroolsBackend.Branch.EXEC_MODEL).execModelTranspiler()
                .modelplan().rules()) {
            if (vormen.stream().noneMatch(v -> p.naam().equals(v) || p.naam().startsWith(v + ": ")
                    || p.naam().startsWith("kanaalstand " + v + ": ")))
                continue;
            out.add("regel " + p.naam() + " | vorm " + p.vorm() + " | groep " + p.agendaGroup());
            List<String> slots = new ArrayList<>();
            for (Read l : p.lezingen().slots())
                slots.add(l.slot() + " " + l.soort() + " " + l.naam() + " (" + l.sleutel() + ")");
            out.add("  lezingen " + slots);
            for (LhsItem item : p.leftSide()) {
                out.add("  " + item(item));
                if (item instanceof TestItem t) TermTree.fields("    toets", t.test(), "0", out);
            }
            out.add("  gevolg " + consequence(p.consequence()));
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

    static JsonNode golden(String naam) throws Exception {
        return JSON.readTree(GOLDEN.resolve(naam).resolve("ir.json").toFile());
    }

    static JsonNode document(String ir) throws Exception {
        return JSON.readTree(ir);
    }

    private static String item(LhsItem item) {
        return switch (item) {
            case Subject o -> "onderwerp " + o.beanClass().getSimpleName() + " " + o.naam() + " alfa "
                    + alpha(o.alpha()) + " join " + joins(o.joins()) + " bind " + bindings(o.bindings());
            case FactPattern p -> "patroon " + pattern(p);
            case Not n -> "niet " + (n.patterns().size() == 1 ? pattern(n.patterns().get(0)) : "binnen "
                    + n.inside() + " buiten " + n.outside() + " "
                    + n.patterns().stream().map(ChannelPlanWitnessTest::pattern).toList());
            case Exists b -> "bestaat " + b.patterns().stream().map(ChannelPlanWitnessTest::pattern).toList();
            case Accumulation a -> "accumulatie " + pattern(a.bron()) + " functies "
                    + a.aggregations().stream().map(f -> f.soort() + " " + f.bron() + " -> " + f.doel()).toList();
            case TestItem t -> "toets " + t.id() + " " + t.rol() + " sonde " + t.depositFlag() + " slots " + t.slots()
                    + " grootte " + t.size() + ": " + TermTree.tree(t.test());
        };
    }

    private static String pattern(FactPattern p) {
        return p.factClass().getSimpleName() + " " + p.naam() + " alfa " + alpha(p.alpha()) + " join "
                + joins(p.joins()) + " beta " + p.betaTests().stream().map(t -> t.id() + ": " + t.function() + " "
                + t.doel()).toList() + " bind " + bindings(p.bindings());
    }

    /** Each identity join with its id: the field of the pattern's object (null: the object itself), and the variable
     *  on the other side with the reference field read there, where it reads one. */
    private static List<String> joins(List<Join> joins) {
        return joins.stream().map(j -> j.id() + ": " + j.veld() + " = " + j.doel()
                + (j.targetField() == null ? "" : "." + veld(j.targetField()))).toList();
    }

    /** Each constraint with its id and its field tests: the path, the field (null: the object itself), the form of
     *  reading, the comparison and the value. */
    private static List<String> alpha(List<Alpha> alpha) {
        List<String> out = new ArrayList<>();
        for (Alpha a : alpha)
            out.add(a.id() + ": " + a.tests().stream().map(t -> fields(t.pad()) + " " + t.veld() + " " + t.vorm()
                    + " " + t.op() + (t.waarde() == null ? "" : " " + t.waarde())).toList());
        return out;
    }

    /** Each binding with its variable, its path, its field on its class and its form of reading. */
    private static List<String> bindings(List<Binding> bindings) {
        return bindings.stream().map(b -> b.doel() + " = " + fields(b.pad()) + " " + veld(b.veld()) + " " + b.vorm()
                + (b.soort() == null ? "" : " " + b.soort().getSimpleName())).toList();
    }

    private static String fields(List<Binding.Field> pad) {
        return pad.stream().map(ChannelPlanWitnessTest::veld).toList().toString();
    }

    private static String veld(Binding.Field v) {
        return v.javaClass().getSimpleName() + "." + v.naam();
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
