package dev.uchar.regelspraak.transpiler.tms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.graph.ProducerConsumer.ElementKey;
import dev.uchar.regelspraak.transpiler.tms.plan.Accumulation;
import dev.uchar.regelspraak.transpiler.tms.plan.Alpha;
import dev.uchar.regelspraak.transpiler.tms.plan.Exists;
import dev.uchar.regelspraak.transpiler.tms.plan.BetaTest;
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

import java.lang.reflect.RecordComponent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The rules of a defaults stack come whole from a plan (card drl-teksttak-op-truth-maintenance, step 1c-C2): the rule
 * of a default's own decision, its fault rule, the rules of a reconciliation (passing a member's conflict on, a
 * conflict of its own where two members decide differently, the agreement) and the rule that faults a session without
 * an evaluation date where a group of the stack is dated. Each is a {@link RulePlan} whose left-hand side holds what
 * the forms built themselves before, in its order and with its ids: the subject pattern restricted to exactly the
 * stack's owner, with the input test of an initialisation default; the patterns of the outcomes a reconciliation binds
 * and of those it waits on, with the constraint between two outcomes as a function of the plan; and the control-fact
 * patterns by which a lower default waits on the higher ones. Its consequence carries every decision as a field: the
 * decision's value as a term, with its fault where it is computed, and the facts it inserts with it
 * ({@link Consequence.Decision}), or a conflict's key and the fact it inserts with that ({@link Consequence.Conflict}), each
 * fact a kind of its own ({@link Consequence.StackFact}).
 *
 * <p>This witness pins those plans as text, per plan in order: the name, the form and the group; the slots, each with
 * its kind, its variable and its key; every item of the left-hand side in order with its kind, its pattern class, its
 * constraint ids, each field test with the class of its value and the class it reads the field through where the plan
 * names one, its joins, its constraints between two objects with their function, the constraints after those, its
 * bindings, and of an existence or a negation the variables it declares and those it reads from outside; and the
 * consequence with every field. A term, the test of a rule or the value of a consequence, is pinned as a tree and on
 * the lines after it node by node with its source fragment and of a constant the class of its value and its whole IR
 * type ({@link TermTree#fields}).
 *
 * <ul>
 *   <li>{@code anchor/jutterswet-vondstwaardering}: the simple form on an attribute, three defaults, each inserting the
 *       control fact {@code Beslist} beside its value where a lower default has a rule, and each lower one waiting on
 *       the higher ones; the lowest an initialisation default, whose subject pattern tests the input on the target
 *       through the getter of the declaring type.</li>
 *   <li>{@link #exclusionDated}, since no golden has a dated group: {@code anchor/jutterswet-uitsluiting} with its
 *       group valid from 2026. The simple form on a characteristic, with a do-not-attribute decision between two
 *       attributions, which inserts only its control fact; every rule gated on the evaluation date, and beside them the
 *       rule that faults every object of the owner in a session without one.</li>
 *   <li>{@code synth3-nestkorting}: the general form on an attribute: a default with an unordered exception group of
 *       three defaults, the chain of that group with its outcomes, the reconciliation of the group (the conflict of a
 *       member passed on, a conflict of its own, the agreement) and the default's fall back to its own decision; at the
 *       root the reconciliation of the stack's groups, which writes the target or deposits the §9.5 inconsistency; and
 *       an initialisation default in a chain of its own.</li>
 *   <li>{@code anchor/jutterswet-nestkenmerk}: the general form on a characteristic, whose exception group holds a
 *       do-not-attribute decision, an outcome like any other; at the root only an attribution writes, a test on the
 *       outcome's value.</li>
 *   <li>{@code jutterswet-wachtgeld}: the general form for want of exception groups, with three groups at the root,
 *       each a chain of one default; their reconciliation writes the target where they agree and deposits its own
 *       conflict, under the bare key, as the §9.5 inconsistency where two decide differently.</li>
 * </ul>
 *
 * <p>That the rendering of these plans builds the rules of before the step is what the harness {@code FireTrace}
 * shows, and the executable model of the corpus, which is equal as text; for {@link #exclusionDated}, outside
 * the corpus, the executable model of before and after the step is equal as text as well. The constructors of the two
 * consequences keep a decision and a conflict apart ({@link #a_decision_and_a_conflict_each_insert_their_own_facts}).
 *
 * <p><b>Red</b> before the step: the forms of the defaults built their rules themselves, so there was no plan of
 * them, and this class did not compile, since the plan had no consequence of a decision or a conflict of a stack;
 * with only the names of the plans pinned, none of the five documents had a plan of any of these rules.
 */
class DefaultsplanWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void the_simple_form_with_an_initialisation_default() throws Exception {
        assertEquals(List.of(
                "regel defaults A:Jutter.waardering: default 0 | vorm defaults A:Jutter.waardering |"
                        + " groep null",
                "  lezingen [0 INVOER $erkend (eigen ElementKey[objectType=jutter, naam=erkend, characteristic=true]),"
                        + " 1 INVOER $basisbedrag (eigen ElementKey[objectType=jutter, naam=basisbedrag,"
                        + " characteristic=false])]",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_0: [[] null VALUE CLASS_IS Jutter]] join []"
                        + " beta [] bind [$erkend = [] Jutter.erkend WAAR, $basisbedrag = [] Jutter.basisbedrag"
                        + " VALUE]",
                "  toets voorwaarde_1 CONDITION sonde -1 slots [0] grootte 2: KENMERK(Lees 0, Const"
                        + " true)",
                "    toets 0 Call bron 'hij erkend is'",
                "    toets 0.0 SlotValue",
                "    toets 0.1 Const Boolean",
                "  gevolg Decision [onderwerp=$onderwerp, value=Lees 1, slots=[1], size=2,"
                        + " collapse=true, fault=FaultName[form=defaults A:Jutter.waardering, regel=defaults"
                        + " A:Jutter.waardering], facts=[Value[target=ElementKey[objectType=jutter,"
                        + " naam=waardering, characteristic=false], writer=defaults A:Jutter.waardering],"
                        + " Decides[stack=defaults A:Jutter.waardering, index=0]], insertion=LOGICAL]",
                "    value 0 SlotValue bron 'zijn basisbedrag'",
                "regel defaults A:Jutter.waardering: default 1 | vorm defaults A:Jutter.waardering |"
                        + " groep null",
                "  lezingen [0 INVOER $kandidaat (eigen ElementKey[objectType=jutter, naam=kandidaat,"
                        + " characteristic=true]), 1 INVOER $kandidaatbedrag (eigen ElementKey[objectType=jutter,"
                        + " naam=kandidaatbedrag, characteristic=false])]",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_2: [[] null VALUE CLASS_IS Jutter]] join []"
                        + " beta [] bind [$kandidaat = [] Jutter.kandidaat WAAR, $kandidaatbedrag = []"
                        + " Jutter.kandidaatbedrag VALUE]",
                "  niet binnen [$hoger_3] buiten [$onderwerp] [Decides $hoger_3 alfa [hoger-beslist_3:"
                        + " [[] stack VALUE GELIJK defaults A:Jutter.waardering {String}, [] index VALUE UNDER"
                        + " 1 {Integer}]] join [hoger-beslist-onderwerp_4: onderwerp = $onderwerp] beta [] na []"
                        + " bind []]",
                "  toets voorwaarde_5 CONDITION sonde -1 slots [0] grootte 2: KENMERK(Lees 0, Const"
                        + " true)",
                "    toets 0 Call bron 'hij kandidaat is'",
                "    toets 0.0 SlotValue",
                "    toets 0.1 Const Boolean",
                "  gevolg Decision [onderwerp=$onderwerp, value=Lees 1, slots=[1], size=2,"
                        + " collapse=true, fault=FaultName[form=defaults A:Jutter.waardering, regel=defaults"
                        + " A:Jutter.waardering], facts=[Value[target=ElementKey[objectType=jutter,"
                        + " naam=waardering, characteristic=false], writer=defaults A:Jutter.waardering],"
                        + " Decides[stack=defaults A:Jutter.waardering, index=1]], insertion=LOGICAL]",
                "    value 0 SlotValue bron 'zijn kandidaatbedrag'",
                "regel defaults A:Jutter.waardering: default 2 | vorm defaults A:Jutter.waardering |"
                        + " groep null",
                "  lezingen []",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_6: [[] null VALUE CLASS_IS Jutter],"
                        + " doel-leeg-in-de-invoer_7: [[] waardering VALUE LEEG via Jutter]] join [] beta [] bind"
                        + " []",
                "  niet binnen [$hoger_8] buiten [$onderwerp] [Decides $hoger_8 alfa [hoger-beslist_8:"
                        + " [[] stack VALUE GELIJK defaults A:Jutter.waardering {String}, [] index VALUE UNDER"
                        + " 2 {Integer}]] join [hoger-beslist-onderwerp_9: onderwerp = $onderwerp] beta [] na []"
                        + " bind []]",
                "  gevolg Decision [onderwerp=$onderwerp, value=Const 25, slots=[], size=0,"
                        + " collapse=true, fault=FaultName[form=defaults A:Jutter.waardering, regel=defaults"
                        + " A:Jutter.waardering], facts=[Value[target=ElementKey[objectType=jutter,"
                        + " naam=waardering, characteristic=false], writer=defaults A:Jutter.waardering]],"
                        + " insertion=LOGICAL]",
                "    value 0 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '25'"), plans(golden("anchor/jutterswet-vondstwaardering")));
    }

    @Test void the_simple_form_on_a_characteristic_with_a_dated_group() throws Exception {
        assertEquals(List.of(
                "regel defaults K:Jutter.uitgesloten: default 0 | vorm defaults K:Jutter.uitgesloten |"
                        + " groep null",
                "  lezingen [0 INVOER $vergunningGeweigerd (eigen ElementKey[objectType=jutter, naam=vergunning"
                        + " geweigerd, characteristic=true]), 1 INVOER $weigeringHerroepen (eigen"
                        + " ElementKey[objectType=jutter, naam=weigering herroepen, characteristic=true]), 2 INVOER"
                        + " $herroepingVervallen (eigen ElementKey[objectType=jutter, naam=herroeping"
                        + " vervallen, characteristic=true])]",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_3: [[] null VALUE CLASS_IS Jutter]] join []"
                        + " beta [] bind [$vergunningGeweigerd = [] Jutter.vergunning geweigerd WAAR,"
                        + " $weigeringHerroepen = [] Jutter.weigering herroepen WAAR, $herroepingVervallen = []"
                        + " Jutter.herroeping vervallen WAAR]",
                "  patroon EvaluationDate $geldigheid_0 alfa [geldig_4: [[] date VALUE WITHIN"
                        + " Validity[from=2026-01-01, until=null] {Validity}]] join [] beta [] na [] bind []",
                "  toets voorwaarde_5 CONDITION sonde -1 slots [0, 1, 2] grootte 3: Samengesteld ALLE"
                        + " 0(KENMERK(Lees 0, Const true), KENMERK(Lees 1, Const true), KENMERK(Lees 2, Const"
                        + " true))",
                "    toets 0 Compound bron 'de weigering is herroepen maar de herroeping is"
                        + " vervallen'",
                "    toets 0.0 Call bron 'zijn raapvergunning is geweigerd'",
                "    toets 0.0.0 SlotValue",
                "    toets 0.0.1 Const Boolean",
                "    toets 0.1 Call bron 'de weigering is herroepen'",
                "    toets 0.1.0 SlotValue",
                "    toets 0.1.1 Const Boolean",
                "    toets 0.2 Call bron 'de herroeping is vervallen'",
                "    toets 0.2.0 SlotValue",
                "    toets 0.2.1 Const Boolean",
                "  gevolg Decision [onderwerp=$onderwerp, value=Const true, slots=[], size=3,"
                        + " collapse=false, fault=null, facts=[Characteristic[characteristic=ElementKey[objectType=jutter,"
                        + " naam=uitgesloten, characteristic=true]], Decides[stack=defaults K:Jutter.uitgesloten,"
                        + " index=0]], insertion=LOGICAL]",
                "    value 0 Const Boolean",
                "regel defaults K:Jutter.uitgesloten: default 1 | vorm defaults K:Jutter.uitgesloten |"
                        + " groep null",
                "  lezingen [0 INVOER $vergunningGeweigerd (eigen ElementKey[objectType=jutter, naam=vergunning"
                        + " geweigerd, characteristic=true]), 1 INVOER $weigeringHerroepen (eigen"
                        + " ElementKey[objectType=jutter, naam=weigering herroepen, characteristic=true])]",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_6: [[] null VALUE CLASS_IS Jutter]] join []"
                        + " beta [] bind [$vergunningGeweigerd = [] Jutter.vergunning geweigerd WAAR,"
                        + " $weigeringHerroepen = [] Jutter.weigering herroepen WAAR]",
                "  patroon EvaluationDate $geldigheid_0 alfa [geldig_7: [[] date VALUE WITHIN"
                        + " Validity[from=2026-01-01, until=null] {Validity}]] join [] beta [] na [] bind []",
                "  niet binnen [$hoger_8] buiten [$onderwerp] [Decides $hoger_8 alfa [hoger-beslist_8:"
                        + " [[] stack VALUE GELIJK defaults K:Jutter.uitgesloten {String}, [] index VALUE UNDER"
                        + " 1 {Integer}]] join [hoger-beslist-onderwerp_9: onderwerp = $onderwerp] beta [] na []"
                        + " bind []]",
                "  toets voorwaarde_10 CONDITION sonde -1 slots [0, 1] grootte 2: Samengesteld ALLE"
                        + " 0(KENMERK(Lees 0, Const true), KENMERK(Lees 1, Const true))",
                "    toets 0 Compound bron 'de weigering is herroepen'",
                "    toets 0.0 Call bron 'zijn raapvergunning is geweigerd'",
                "    toets 0.0.0 SlotValue",
                "    toets 0.0.1 Const Boolean",
                "    toets 0.1 Call bron 'de weigering is herroepen'",
                "    toets 0.1.0 SlotValue",
                "    toets 0.1.1 Const Boolean",
                "  gevolg Decision [onderwerp=$onderwerp, value=Const false, slots=[], size=2,"
                        + " collapse=false, fault=null, facts=[Decides[stack=defaults K:Jutter.uitgesloten,"
                        + " index=1]], insertion=LOGICAL]",
                "    value 0 Const Boolean",
                "regel defaults K:Jutter.uitgesloten: default 2 | vorm defaults K:Jutter.uitgesloten |"
                        + " groep null",
                "  lezingen [0 INVOER $vergunningGeweigerd (eigen ElementKey[objectType=jutter, naam=vergunning"
                        + " geweigerd, characteristic=true])]",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_11: [[] null VALUE CLASS_IS Jutter]] join []"
                        + " beta [] bind [$vergunningGeweigerd = [] Jutter.vergunning geweigerd WAAR]",
                "  patroon EvaluationDate $geldigheid_0 alfa [geldig_12: [[] date VALUE WITHIN"
                        + " Validity[from=2026-01-01, until=null] {Validity}]] join [] beta [] na [] bind []",
                "  niet binnen [$hoger_13] buiten [$onderwerp] [Decides $hoger_13 alfa"
                        + " [hoger-beslist_13: [[] stack VALUE GELIJK defaults K:Jutter.uitgesloten {String}, []"
                        + " index VALUE UNDER 2 {Integer}]] join [hoger-beslist-onderwerp_14: onderwerp ="
                        + " $onderwerp] beta [] na [] bind []]",
                "  toets voorwaarde_15 CONDITION sonde -1 slots [0] grootte 1: KENMERK(Lees 0, Const"
                        + " true)",
                "    toets 0 Call bron 'zijn raapvergunning is geweigerd'",
                "    toets 0.0 SlotValue",
                "    toets 0.1 Const Boolean",
                "  gevolg Decision [onderwerp=$onderwerp, value=Const true, slots=[], size=1,"
                        + " collapse=false, fault=null, facts=[Characteristic[characteristic=ElementKey[objectType=jutter,"
                        + " naam=uitgesloten, characteristic=true]]], insertion=LOGICAL]",
                "    value 0 Const Boolean",
                "regel defaults K:Jutter.uitgesloten: geen rekendatum | vorm defaults"
                        + " K:Jutter.uitgesloten | groep null",
                "  lezingen []",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_16: [[] null VALUE CLASS_IS"
                        + " Jutter]] join [] beta [] bind []",
                "  patroon EvaluationDate $rekendatum alfa [geen-rekendatum_17: [[] date VALUE LEEG]] join"
                        + " [] beta [] na [] bind []",
                "  gevolg Fault [onderwerp=$onderwerp, sentence=a validity-bounded form needs an evaluation"
                        + " date; this session is time-free, fault=FaultName[form=defaults K:Jutter.uitgesloten,"
                        + " regel=defaults K:Jutter.uitgesloten], insertion=LOGICAL]"), plans(exclusionDated()));
    }

    @Test void the_general_form_with_a_chain_and_its_reconciliations() throws Exception {
        assertEquals(List.of(
                "regel defaults A:Jutter.korting: default 0.0.0 | vorm defaults A:Jutter.korting | groep"
                        + " null",
                "  lezingen [0 INVOER $grondP (eigen ElementKey[objectType=jutter, naam=grondp, characteristic=true]),"
                        + " 1 INVOER $grondslag (eigen ElementKey[objectType=jutter, naam=grondslag,"
                        + " characteristic=false])]",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_0: [[] null VALUE CLASS_IS Jutter]] join []"
                        + " beta [] bind [$grondP = [] Jutter.grondP WAAR, $grondslag = [] Jutter.grondslag VALUE]",
                "  toets voorwaarde_1 CONDITION sonde -1 slots [0] grootte 2: KENMERK(Lees 0, Const"
                        + " true)",
                "    toets 0 Call bron 'hij grondP heeft'",
                "    toets 0.0 SlotValue",
                "    toets 0.1 Const Boolean",
                "  gevolg Decision [onderwerp=$onderwerp, value=RS_PLUS(Lees 1, Const 20), slots=[1],"
                        + " size=2, collapse=true, fault=FaultName[form=defaults A:Jutter.korting, regel=defaults"
                        + " A:Jutter.korting], facts=[Outcome[stack=defaults A:Jutter.korting, chain=1,"
                        + " position=0, category=DECIDED]], insertion=LOGICAL]",
                "    value 0 Call bron 'de grondslag plus 20.00'",
                "    value 0.0 SlotValue bron 'de grondslag van de Jutter'",
                "    value 0.1 Const BigDecimal type Numeriek[geheel=false, decimalen=2,"
                        + " teken=ONBEPAALD, eenheid=null] bron '20.00'",
                "regel defaults A:Jutter.korting: default 0.1.0 | vorm defaults A:Jutter.korting | groep"
                        + " null",
                "  lezingen [0 INVOER $grondQ (eigen ElementKey[objectType=jutter, naam=grondq, characteristic=true]),"
                        + " 1 INVOER $grondslag (eigen ElementKey[objectType=jutter, naam=grondslag,"
                        + " characteristic=false])]",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_2: [[] null VALUE CLASS_IS Jutter]] join []"
                        + " beta [] bind [$grondQ = [] Jutter.grondQ WAAR, $grondslag = [] Jutter.grondslag VALUE]",
                "  toets voorwaarde_3 CONDITION sonde -1 slots [0] grootte 2: KENMERK(Lees 0, Const"
                        + " true)",
                "    toets 0 Call bron 'hij grondQ heeft'",
                "    toets 0.0 SlotValue",
                "    toets 0.1 Const Boolean",
                "  gevolg Decision [onderwerp=$onderwerp, value=RS_PLUS(Lees 1, Const 35), slots=[1],"
                        + " size=2, collapse=true, fault=FaultName[form=defaults A:Jutter.korting, regel=defaults"
                        + " A:Jutter.korting], facts=[Outcome[stack=defaults A:Jutter.korting, chain=2,"
                        + " position=0, category=DECIDED]], insertion=LOGICAL]",
                "    value 0 Call bron 'de grondslag plus 35.00'",
                "    value 0.0 SlotValue bron 'de grondslag van de Jutter'",
                "    value 0.1 Const BigDecimal type Numeriek[geheel=false, decimalen=2,"
                        + " teken=ONBEPAALD, eenheid=null] bron '35.00'",
                "regel defaults A:Jutter.korting: default 0.2.0 | vorm defaults A:Jutter.korting | groep"
                        + " null",
                "  lezingen [0 INVOER $grondR (eigen ElementKey[objectType=jutter, naam=grondr, characteristic=true]),"
                        + " 1 INVOER $grondslag (eigen ElementKey[objectType=jutter, naam=grondslag,"
                        + " characteristic=false])]",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_4: [[] null VALUE CLASS_IS Jutter]] join []"
                        + " beta [] bind [$grondR = [] Jutter.grondR WAAR, $grondslag = [] Jutter.grondslag VALUE]",
                "  toets voorwaarde_5 CONDITION sonde -1 slots [0] grootte 2: KENMERK(Lees 0, Const"
                        + " true)",
                "    toets 0 Call bron 'hij grondR heeft'",
                "    toets 0.0 SlotValue",
                "    toets 0.1 Const Boolean",
                "  gevolg Decision [onderwerp=$onderwerp, value=RS_PLUS(RS_PLUS(Lees 1, Const 15), Const"
                        + " 5), slots=[1], size=2, collapse=true, fault=FaultName[form=defaults A:Jutter.korting,"
                        + " regel=defaults A:Jutter.korting], facts=[Outcome[stack=defaults A:Jutter.korting,"
                        + " chain=3, position=0, category=DECIDED]], insertion=LOGICAL]",
                "    value 0 Call bron 'die som plus 5.00'",
                "    value 0.0 Call bron 'de grondslag plus 15.00'",
                "    value 0.0.0 SlotValue bron 'de grondslag van de Jutter'",
                "    value 0.0.1 Const BigDecimal type Numeriek[geheel=false, decimalen=2,"
                        + " teken=ONBEPAALD, eenheid=null] bron '15.00'",
                "    value 0.1 Const BigDecimal type Numeriek[geheel=false, decimalen=2,"
                        + " teken=ONBEPAALD, eenheid=null] bron '5.00'",
                "regel defaults A:Jutter.korting: verschil 0 | vorm defaults A:Jutter.korting | groep"
                        + " null",
                "  lezingen []",
                "  patroon Outcome $besluit alfa [lid-besluit_6: [[] stack VALUE GELIJK defaults"
                        + " A:Jutter.korting {String}, [] category VALUE GELIJK DECIDED {Kind}, [] chain VALUE"
                        + " AT_LEAST 1 {Integer}, [] chain VALUE UNDER 4 {Integer}]] join [] beta [] na [] bind"
                        + " [$onderwerp = [] Outcome.onderwerp VALUE]",
                "  niet binnen [$uitkomst_7] buiten [$onderwerp, $besluit] [Outcome $uitkomst_7 alfa"
                        + " [uitkomst_7: [[] stack VALUE GELIJK defaults A:Jutter.korting {String}, [] chain"
                        + " VALUE AT_LEAST 1 {Integer}, [] chain VALUE UNDER 4 {Integer}, [] category VALUE GELIJK"
                        + " DECIDED {Kind}]] join [uitkomst-onderwerp_8: onderwerp = $onderwerp] beta"
                        + " [eerder-lid_9: EARLIER_MEMBER $besluit] na [] bind []]",
                "  bestaat binnen [$uitkomst_10] buiten [$onderwerp, $besluit] [Outcome $uitkomst_10"
                        + " alfa [uitkomst_10: [[] stack VALUE GELIJK defaults A:Jutter.korting {String}, []"
                        + " chain VALUE AT_LEAST 1 {Integer}, [] chain VALUE UNDER 4 {Integer}, [] category VALUE"
                        + " GELIJK DECIDED {Kind}]] join [uitkomst-onderwerp_11: onderwerp = $onderwerp] beta"
                        + " [ander-besluit_12: OTHER_DECISION $besluit] na [] bind []]",
                "  gevolg Conflict [onderwerp=$onderwerp, key=Const Jutter.korting/0, slots=[],"
                        + " size=0, fact=Outcome[stack=defaults A:Jutter.korting, chain=0, position=0,"
                        + " category=CONFLICT], insertion=LOGICAL]",
                "    key 0 Const String",
                "regel defaults A:Jutter.korting: eens 0 | vorm defaults A:Jutter.korting | groep null",
                "  lezingen [0 DECISION $waarde (besluit)]",
                "  patroon Outcome $besluit alfa [lid-besluit_13: [[] stack VALUE GELIJK defaults"
                        + " A:Jutter.korting {String}, [] category VALUE GELIJK DECIDED {Kind}, [] chain VALUE"
                        + " AT_LEAST 1 {Integer}, [] chain VALUE UNDER 4 {Integer}]] join [] beta [] na [] bind"
                        + " [$onderwerp = [] Outcome.onderwerp VALUE, $waarde = [] Outcome.value VALUE]",
                "  niet binnen [$uitkomst_14] buiten [$onderwerp, $besluit] [Outcome $uitkomst_14 alfa"
                        + " [uitkomst_14: [[] stack VALUE GELIJK defaults A:Jutter.korting {String}, [] chain"
                        + " VALUE AT_LEAST 1 {Integer}, [] chain VALUE UNDER 4 {Integer}, [] category VALUE GELIJK"
                        + " DECIDED {Kind}]] join [uitkomst-onderwerp_15: onderwerp = $onderwerp] beta"
                        + " [eerder-lid_16: EARLIER_MEMBER $besluit] na [] bind []]",
                "  niet binnen [$uitkomst_17] buiten [$onderwerp, $besluit] [Outcome $uitkomst_17 alfa"
                        + " [uitkomst_17: [[] stack VALUE GELIJK defaults A:Jutter.korting {String}, [] chain"
                        + " VALUE AT_LEAST 1 {Integer}, [] chain VALUE UNDER 4 {Integer}, [] category VALUE GELIJK"
                        + " DECIDED {Kind}]] join [uitkomst-onderwerp_18: onderwerp = $onderwerp] beta"
                        + " [ander-besluit_19: OTHER_DECISION $besluit] na [] bind []]",
                "  gevolg Decision [onderwerp=$onderwerp, value=Lees 0, slots=[0], size=1,"
                        + " collapse=false, fault=null, facts=[Outcome[stack=defaults A:Jutter.korting, chain=0,"
                        + " position=0, category=DECIDED]], insertion=LOGICAL]",
                "    value 0 SlotValue",
                "regel defaults A:Jutter.korting: terugval 0 | vorm defaults A:Jutter.korting | groep"
                        + " null",
                "  lezingen [0 INVOER $rechtA (eigen ElementKey[objectType=jutter, naam=rechta, characteristic=true]),"
                        + " 1 INVOER $grondslag (eigen ElementKey[objectType=jutter, naam=grondslag,"
                        + " characteristic=false])]",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_20: [[] null VALUE CLASS_IS Jutter]] join []"
                        + " beta [] bind [$rechtA = [] Jutter.rechtA WAAR, $grondslag = [] Jutter.grondslag VALUE]",
                "  niet binnen [$uitkomst_21] buiten [$onderwerp] [Outcome $uitkomst_21 alfa"
                        + " [uitkomst_21: [[] stack VALUE GELIJK defaults A:Jutter.korting {String}, [] chain"
                        + " VALUE AT_LEAST 1 {Integer}, [] chain VALUE UNDER 4 {Integer}]] join"
                        + " [uitkomst-onderwerp_22: onderwerp = $onderwerp] beta [] na [] bind []]",
                "  toets voorwaarde_23 CONDITION sonde -1 slots [0] grootte 2: KENMERK(Lees 0, Const"
                        + " true)",
                "    toets 0 Call bron 'hij rechtA heeft'",
                "    toets 0.0 SlotValue",
                "    toets 0.1 Const Boolean",
                "  gevolg Decision [onderwerp=$onderwerp, value=RS_PLUS(Lees 1, Const 90), slots=[1],"
                        + " size=2, collapse=true, fault=FaultName[form=defaults A:Jutter.korting, regel=defaults"
                        + " A:Jutter.korting], facts=[Outcome[stack=defaults A:Jutter.korting, chain=0,"
                        + " position=0, category=DECIDED]], insertion=LOGICAL]",
                "    value 0 Call bron 'de grondslag plus 90.00'",
                "    value 0.0 SlotValue bron 'de grondslag van de Jutter'",
                "    value 0.1 Const BigDecimal type Numeriek[geheel=false, decimalen=2,"
                        + " teken=ONBEPAALD, eenheid=null] bron '90.00'",
                "regel defaults A:Jutter.korting: default 1 | vorm defaults A:Jutter.korting | groep null",
                "  lezingen []",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_24: [[] null VALUE CLASS_IS"
                        + " Jutter], doel-leeg-in-de-invoer_25: [[] korting VALUE LEEG via Jutter]] join [] beta"
                        + " [] bind []",
                "  niet binnen [$hoger_26] buiten [$onderwerp] [Outcome $hoger_26 alfa"
                        + " [hoger-uitkomst_26: [[] stack VALUE GELIJK defaults A:Jutter.korting {String}, []"
                        + " chain VALUE GELIJK 0 {Integer}, [] position VALUE UNDER 1 {Integer}]] join"
                        + " [hoger-uitkomst-onderwerp_27: onderwerp = $onderwerp] beta [] na [] bind []]",
                "  gevolg Decision [onderwerp=$onderwerp, value=Const 5, slots=[], size=0,"
                        + " collapse=true, fault=FaultName[form=defaults A:Jutter.korting, regel=defaults"
                        + " A:Jutter.korting], facts=[Outcome[stack=defaults A:Jutter.korting, chain=0,"
                        + " position=1, category=DECIDED]], insertion=LOGICAL]",
                "    value 0 Const BigDecimal type Numeriek[geheel=false, decimalen=2, teken=ONBEPAALD,"
                        + " eenheid=null] bron '5.00'",
                "regel defaults A:Jutter.korting: doorgeven wortel | vorm defaults A:Jutter.korting |"
                        + " groep null",
                "  lezingen [0 CONFLICT_KEY $sleutel (conflictsleutel)]",
                "  patroon Outcome $conflict alfa [lid-conflict_28: [[] stack VALUE GELIJK defaults"
                        + " A:Jutter.korting {String}, [] category VALUE GELIJK CONFLICT {Kind}, [] chain VALUE"
                        + " AT_LEAST 0 {Integer}, [] chain VALUE UNDER 1 {Integer}]] join [] beta [] na [] bind"
                        + " [$onderwerp = [] Outcome.onderwerp VALUE, $sleutel = [] Outcome.key VALUE]",
                "  gevolg Conflict [onderwerp=$onderwerp, key=Lees 0, slots=[0], size=1,"
                        + " fact=Inconsistency[], insertion=LOGICAL]",
                "    key 0 SlotValue",
                "regel defaults A:Jutter.korting: eens wortel | vorm defaults A:Jutter.korting | groep"
                        + " null",
                "  lezingen [0 DECISION $waarde (besluit)]",
                "  patroon Outcome $besluit alfa [lid-besluit_29: [[] stack VALUE GELIJK defaults"
                        + " A:Jutter.korting {String}, [] category VALUE GELIJK DECIDED {Kind}, [] chain VALUE"
                        + " AT_LEAST 0 {Integer}, [] chain VALUE UNDER 1 {Integer}]] join [] beta [] na [] bind"
                        + " [$onderwerp = [] Outcome.onderwerp VALUE, $waarde = [] Outcome.value VALUE]",
                "  gevolg Decision [onderwerp=$onderwerp, value=Lees 0, slots=[0], size=1,"
                        + " collapse=false, fault=null, facts=[Value[target=ElementKey[objectType=jutter,"
                        + " naam=korting, characteristic=false], writer=defaults A:Jutter.korting]], insertion=LOGICAL]",
                "    value 0 SlotValue"), plans(golden("synth3-nestkorting")));
    }

    @Test void the_general_form_on_a_characteristic() throws Exception {
        assertEquals(List.of(
                "regel defaults K:Jutter.vrijgesteld: default 0.0.0 | vorm defaults K:Jutter.vrijgesteld"
                        + " | groep null",
                "  lezingen [0 INVOER $nx (eigen ElementKey[objectType=jutter, naam=nx, characteristic=true]), 1"
                        + " INVOER $ny (eigen ElementKey[objectType=jutter, naam=ny, characteristic=true])]",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_0: [[] null VALUE CLASS_IS Jutter]] join []"
                        + " beta [] bind [$nx = [] Jutter.nx WAAR, $ny = [] Jutter.ny WAAR]",
                "  toets voorwaarde_1 CONDITION sonde -1 slots [0, 1] grootte 2: Samengesteld ALLE"
                        + " 0(KENMERK(Lees 0, Const true), KENMERK(Lees 1, Const true))",
                "    toets 0 Compound bron 'hij nx heeft en hij ny heeft'",
                "    toets 0.0 Call bron 'hij nx heeft'",
                "    toets 0.0.0 SlotValue",
                "    toets 0.0.1 Const Boolean",
                "    toets 0.1 Call bron 'hij ny heeft'",
                "    toets 0.1.0 SlotValue",
                "    toets 0.1.1 Const Boolean",
                "  gevolg Decision [onderwerp=$onderwerp, value=Const false, slots=[], size=2,"
                        + " collapse=false, fault=null, facts=[Outcome[stack=defaults K:Jutter.vrijgesteld,"
                        + " chain=1, position=0, category=DECIDED]], insertion=LOGICAL]",
                "    value 0 Const Boolean",
                "regel defaults K:Jutter.vrijgesteld: default 0.1.0 | vorm defaults K:Jutter.vrijgesteld"
                        + " | groep null",
                "  lezingen [0 INVOER $nx (eigen ElementKey[objectType=jutter, naam=nx, characteristic=true]), 1"
                        + " INVOER $nz (eigen ElementKey[objectType=jutter, naam=nz, characteristic=true])]",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_2: [[] null VALUE CLASS_IS Jutter]] join []"
                        + " beta [] bind [$nx = [] Jutter.nx WAAR, $nz = [] Jutter.nz WAAR]",
                "  toets voorwaarde_3 CONDITION sonde -1 slots [0, 1] grootte 2: Samengesteld ALLE"
                        + " 0(KENMERK(Lees 0, Const true), KENMERK(Lees 1, Const true))",
                "    toets 0 Compound bron 'hij nx heeft en hij nz heeft'",
                "    toets 0.0 Call bron 'hij nx heeft'",
                "    toets 0.0.0 SlotValue",
                "    toets 0.0.1 Const Boolean",
                "    toets 0.1 Call bron 'hij nz heeft'",
                "    toets 0.1.0 SlotValue",
                "    toets 0.1.1 Const Boolean",
                "  gevolg Decision [onderwerp=$onderwerp, value=Const true, slots=[], size=2,"
                        + " collapse=false, fault=null, facts=[Outcome[stack=defaults K:Jutter.vrijgesteld,"
                        + " chain=2, position=0, category=DECIDED]], insertion=LOGICAL]",
                "    value 0 Const Boolean",
                "regel defaults K:Jutter.vrijgesteld: verschil 0 | vorm defaults K:Jutter.vrijgesteld |"
                        + " groep null",
                "  lezingen []",
                "  patroon Outcome $besluit alfa [lid-besluit_4: [[] stack VALUE GELIJK defaults"
                        + " K:Jutter.vrijgesteld {String}, [] category VALUE GELIJK DECIDED {Kind}, [] chain VALUE"
                        + " AT_LEAST 1 {Integer}, [] chain VALUE UNDER 3 {Integer}]] join [] beta [] na [] bind"
                        + " [$onderwerp = [] Outcome.onderwerp VALUE]",
                "  niet binnen [$uitkomst_5] buiten [$onderwerp, $besluit] [Outcome $uitkomst_5 alfa"
                        + " [uitkomst_5: [[] stack VALUE GELIJK defaults K:Jutter.vrijgesteld {String}, [] chain"
                        + " VALUE AT_LEAST 1 {Integer}, [] chain VALUE UNDER 3 {Integer}, [] category VALUE GELIJK"
                        + " DECIDED {Kind}]] join [uitkomst-onderwerp_6: onderwerp = $onderwerp] beta"
                        + " [eerder-lid_7: EARLIER_MEMBER $besluit] na [] bind []]",
                "  bestaat binnen [$uitkomst_8] buiten [$onderwerp, $besluit] [Outcome $uitkomst_8 alfa"
                        + " [uitkomst_8: [[] stack VALUE GELIJK defaults K:Jutter.vrijgesteld {String}, [] chain"
                        + " VALUE AT_LEAST 1 {Integer}, [] chain VALUE UNDER 3 {Integer}, [] category VALUE GELIJK"
                        + " DECIDED {Kind}]] join [uitkomst-onderwerp_9: onderwerp = $onderwerp] beta"
                        + " [ander-besluit_10: OTHER_DECISION $besluit] na [] bind []]",
                "  gevolg Conflict [onderwerp=$onderwerp, key=Const Jutter.vrijgesteld/0, slots=[],"
                        + " size=0, fact=Outcome[stack=defaults K:Jutter.vrijgesteld, chain=0, position=0,"
                        + " category=CONFLICT], insertion=LOGICAL]",
                "    key 0 Const String",
                "regel defaults K:Jutter.vrijgesteld: eens 0 | vorm defaults K:Jutter.vrijgesteld |"
                        + " groep null",
                "  lezingen [0 DECISION $waarde (besluit)]",
                "  patroon Outcome $besluit alfa [lid-besluit_11: [[] stack VALUE GELIJK defaults"
                        + " K:Jutter.vrijgesteld {String}, [] category VALUE GELIJK DECIDED {Kind}, [] chain VALUE"
                        + " AT_LEAST 1 {Integer}, [] chain VALUE UNDER 3 {Integer}]] join [] beta [] na [] bind"
                        + " [$onderwerp = [] Outcome.onderwerp VALUE, $waarde = [] Outcome.value VALUE]",
                "  niet binnen [$uitkomst_12] buiten [$onderwerp, $besluit] [Outcome $uitkomst_12 alfa"
                        + " [uitkomst_12: [[] stack VALUE GELIJK defaults K:Jutter.vrijgesteld {String}, []"
                        + " chain VALUE AT_LEAST 1 {Integer}, [] chain VALUE UNDER 3 {Integer}, [] category VALUE"
                        + " GELIJK DECIDED {Kind}]] join [uitkomst-onderwerp_13: onderwerp = $onderwerp] beta"
                        + " [eerder-lid_14: EARLIER_MEMBER $besluit] na [] bind []]",
                "  niet binnen [$uitkomst_15] buiten [$onderwerp, $besluit] [Outcome $uitkomst_15 alfa"
                        + " [uitkomst_15: [[] stack VALUE GELIJK defaults K:Jutter.vrijgesteld {String}, []"
                        + " chain VALUE AT_LEAST 1 {Integer}, [] chain VALUE UNDER 3 {Integer}, [] category VALUE"
                        + " GELIJK DECIDED {Kind}]] join [uitkomst-onderwerp_16: onderwerp = $onderwerp] beta"
                        + " [ander-besluit_17: OTHER_DECISION $besluit] na [] bind []]",
                "  gevolg Decision [onderwerp=$onderwerp, value=Lees 0, slots=[0], size=1,"
                        + " collapse=false, fault=null, facts=[Outcome[stack=defaults K:Jutter.vrijgesteld,"
                        + " chain=0, position=0, category=DECIDED]], insertion=LOGICAL]",
                "    value 0 SlotValue",
                "regel defaults K:Jutter.vrijgesteld: terugval 0 | vorm defaults K:Jutter.vrijgesteld |"
                        + " groep null",
                "  lezingen [0 INVOER $nx (eigen ElementKey[objectType=jutter, naam=nx, characteristic=true])]",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_18: [[] null VALUE CLASS_IS Jutter]] join []"
                        + " beta [] bind [$nx = [] Jutter.nx WAAR]",
                "  niet binnen [$uitkomst_19] buiten [$onderwerp] [Outcome $uitkomst_19 alfa"
                        + " [uitkomst_19: [[] stack VALUE GELIJK defaults K:Jutter.vrijgesteld {String}, []"
                        + " chain VALUE AT_LEAST 1 {Integer}, [] chain VALUE UNDER 3 {Integer}]] join"
                        + " [uitkomst-onderwerp_20: onderwerp = $onderwerp] beta [] na [] bind []]",
                "  toets voorwaarde_21 CONDITION sonde -1 slots [0] grootte 1: KENMERK(Lees 0, Const"
                        + " true)",
                "    toets 0 Call bron 'hij nx heeft'",
                "    toets 0.0 SlotValue",
                "    toets 0.1 Const Boolean",
                "  gevolg Decision [onderwerp=$onderwerp, value=Const true, slots=[], size=1,"
                        + " collapse=false, fault=null, facts=[Outcome[stack=defaults K:Jutter.vrijgesteld,"
                        + " chain=0, position=0, category=DECIDED]], insertion=LOGICAL]",
                "    value 0 Const Boolean",
                "regel defaults K:Jutter.vrijgesteld: doorgeven wortel | vorm defaults"
                        + " K:Jutter.vrijgesteld | groep null",
                "  lezingen [0 CONFLICT_KEY $sleutel (conflictsleutel)]",
                "  patroon Outcome $conflict alfa [lid-conflict_22: [[] stack VALUE GELIJK defaults"
                        + " K:Jutter.vrijgesteld {String}, [] category VALUE GELIJK CONFLICT {Kind}, [] chain"
                        + " VALUE AT_LEAST 0 {Integer}, [] chain VALUE UNDER 1 {Integer}]] join [] beta [] na []"
                        + " bind [$onderwerp = [] Outcome.onderwerp VALUE, $sleutel = [] Outcome.key VALUE]",
                "  gevolg Conflict [onderwerp=$onderwerp, key=Lees 0, slots=[0], size=1,"
                        + " fact=Inconsistency[], insertion=LOGICAL]",
                "    key 0 SlotValue",
                "regel defaults K:Jutter.vrijgesteld: eens wortel | vorm defaults K:Jutter.vrijgesteld |"
                        + " groep null",
                "  lezingen [0 DECISION $waarde (besluit)]",
                "  patroon Outcome $besluit alfa [lid-besluit_23: [[] stack VALUE GELIJK defaults"
                        + " K:Jutter.vrijgesteld {String}, [] category VALUE GELIJK DECIDED {Kind}, [] chain VALUE"
                        + " AT_LEAST 0 {Integer}, [] chain VALUE UNDER 1 {Integer}, [] value VALUE GELIJK true"
                        + " {Boolean}]] join [] beta [] na [] bind [$onderwerp = [] Outcome.onderwerp VALUE,"
                        + " $waarde = [] Outcome.value VALUE]",
                "  gevolg Decision [onderwerp=$onderwerp, value=Lees 0, slots=[0], size=1,"
                        + " collapse=false, fault=null, facts=[Characteristic[characteristic=ElementKey[objectType=jutter,"
                        + " naam=vrijgesteld, characteristic=true]]], insertion=LOGICAL]",
                "    value 0 SlotValue"), plans(golden("anchor/jutterswet-nestkenmerk")));
    }

    @Test void the_general_form_with_more_groups_to_the_root() throws Exception {
        assertEquals(List.of(
                "regel defaults A:Jutter.wachtgeld: default 0:0 | vorm defaults A:Jutter.wachtgeld |"
                        + " groep null",
                "  lezingen [0 INVOER $kustwacht (eigen ElementKey[objectType=jutter, naam=kustwacht,"
                        + " characteristic=true])]",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_0: [[] null VALUE CLASS_IS Jutter]] join []"
                        + " beta [] bind [$kustwacht = [] Jutter.kustwacht WAAR]",
                "  toets voorwaarde_1 CONDITION sonde -1 slots [0] grootte 1: KENMERK(Lees 0, Const"
                        + " true)",
                "    toets 0 Call bron 'hij kustwacht is'",
                "    toets 0.0 SlotValue",
                "    toets 0.1 Const Boolean",
                "  gevolg Decision [onderwerp=$onderwerp, value=Const 30, slots=[], size=1,"
                        + " collapse=true, fault=FaultName[form=defaults A:Jutter.wachtgeld, regel=defaults"
                        + " A:Jutter.wachtgeld], facts=[Outcome[stack=defaults A:Jutter.wachtgeld, chain=0,"
                        + " position=0, category=DECIDED]], insertion=LOGICAL]",
                "    value 0 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '30'",
                "regel defaults A:Jutter.wachtgeld: default 1:0 | vorm defaults A:Jutter.wachtgeld |"
                        + " groep null",
                "  lezingen [0 INVOER $havenmeester (eigen ElementKey[objectType=jutter, naam=havenmeester,"
                        + " characteristic=true])]",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_2: [[] null VALUE CLASS_IS Jutter]] join []"
                        + " beta [] bind [$havenmeester = [] Jutter.havenmeester WAAR]",
                "  toets voorwaarde_3 CONDITION sonde -1 slots [0] grootte 1: KENMERK(Lees 0, Const"
                        + " true)",
                "    toets 0 Call bron 'hij havenmeester is'",
                "    toets 0.0 SlotValue",
                "    toets 0.1 Const Boolean",
                "  gevolg Decision [onderwerp=$onderwerp, value=Const 45, slots=[], size=1,"
                        + " collapse=true, fault=FaultName[form=defaults A:Jutter.wachtgeld, regel=defaults"
                        + " A:Jutter.wachtgeld], facts=[Outcome[stack=defaults A:Jutter.wachtgeld, chain=1,"
                        + " position=0, category=DECIDED]], insertion=LOGICAL]",
                "    value 0 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '45'",
                "regel defaults A:Jutter.wachtgeld: default 2:0 | vorm defaults A:Jutter.wachtgeld |"
                        + " groep null",
                "  lezingen [0 INVOER $strandvoogd (eigen ElementKey[objectType=jutter, naam=strandvoogd,"
                        + " characteristic=true])]",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_4: [[] null VALUE CLASS_IS Jutter]] join []"
                        + " beta [] bind [$strandvoogd = [] Jutter.strandvoogd WAAR]",
                "  toets voorwaarde_5 CONDITION sonde -1 slots [0] grootte 1: KENMERK(Lees 0, Const"
                        + " true)",
                "    toets 0 Call bron 'hij strandvoogd is'",
                "    toets 0.0 SlotValue",
                "    toets 0.1 Const Boolean",
                "  gevolg Decision [onderwerp=$onderwerp, value=Const 30, slots=[], size=1,"
                        + " collapse=true, fault=FaultName[form=defaults A:Jutter.wachtgeld, regel=defaults"
                        + " A:Jutter.wachtgeld], facts=[Outcome[stack=defaults A:Jutter.wachtgeld, chain=2,"
                        + " position=0, category=DECIDED]], insertion=LOGICAL]",
                "    value 0 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '30'",
                "regel defaults A:Jutter.wachtgeld: verschil wortel | vorm defaults A:Jutter.wachtgeld |"
                        + " groep null",
                "  lezingen []",
                "  patroon Outcome $besluit alfa [lid-besluit_6: [[] stack VALUE GELIJK defaults"
                        + " A:Jutter.wachtgeld {String}, [] category VALUE GELIJK DECIDED {Kind}, [] chain VALUE"
                        + " AT_LEAST 0 {Integer}, [] chain VALUE UNDER 3 {Integer}]] join [] beta [] na [] bind"
                        + " [$onderwerp = [] Outcome.onderwerp VALUE]",
                "  niet binnen [$uitkomst_7] buiten [$onderwerp, $besluit] [Outcome $uitkomst_7 alfa"
                        + " [uitkomst_7: [[] stack VALUE GELIJK defaults A:Jutter.wachtgeld {String}, [] chain"
                        + " VALUE AT_LEAST 0 {Integer}, [] chain VALUE UNDER 3 {Integer}, [] category VALUE GELIJK"
                        + " DECIDED {Kind}]] join [uitkomst-onderwerp_8: onderwerp = $onderwerp] beta"
                        + " [eerder-lid_9: EARLIER_MEMBER $besluit] na [] bind []]",
                "  bestaat binnen [$uitkomst_10] buiten [$onderwerp, $besluit] [Outcome $uitkomst_10"
                        + " alfa [uitkomst_10: [[] stack VALUE GELIJK defaults A:Jutter.wachtgeld {String}, []"
                        + " chain VALUE AT_LEAST 0 {Integer}, [] chain VALUE UNDER 3 {Integer}, [] category VALUE"
                        + " GELIJK DECIDED {Kind}]] join [uitkomst-onderwerp_11: onderwerp = $onderwerp] beta"
                        + " [ander-besluit_12: OTHER_DECISION $besluit] na [] bind []]",
                "  gevolg Conflict [onderwerp=$onderwerp, key=Const Jutter.wachtgeld, slots=[],"
                        + " size=0, fact=Inconsistency[], insertion=LOGICAL]",
                "    key 0 Const String",
                "regel defaults A:Jutter.wachtgeld: eens wortel | vorm defaults A:Jutter.wachtgeld |"
                        + " groep null",
                "  lezingen [0 DECISION $waarde (besluit)]",
                "  patroon Outcome $besluit alfa [lid-besluit_13: [[] stack VALUE GELIJK defaults"
                        + " A:Jutter.wachtgeld {String}, [] category VALUE GELIJK DECIDED {Kind}, [] chain VALUE"
                        + " AT_LEAST 0 {Integer}, [] chain VALUE UNDER 3 {Integer}]] join [] beta [] na [] bind"
                        + " [$onderwerp = [] Outcome.onderwerp VALUE, $waarde = [] Outcome.value VALUE]",
                "  niet binnen [$uitkomst_14] buiten [$onderwerp, $besluit] [Outcome $uitkomst_14 alfa"
                        + " [uitkomst_14: [[] stack VALUE GELIJK defaults A:Jutter.wachtgeld {String}, [] chain"
                        + " VALUE AT_LEAST 0 {Integer}, [] chain VALUE UNDER 3 {Integer}, [] category VALUE GELIJK"
                        + " DECIDED {Kind}]] join [uitkomst-onderwerp_15: onderwerp = $onderwerp] beta"
                        + " [eerder-lid_16: EARLIER_MEMBER $besluit] na [] bind []]",
                "  niet binnen [$uitkomst_17] buiten [$onderwerp, $besluit] [Outcome $uitkomst_17 alfa"
                        + " [uitkomst_17: [[] stack VALUE GELIJK defaults A:Jutter.wachtgeld {String}, [] chain"
                        + " VALUE AT_LEAST 0 {Integer}, [] chain VALUE UNDER 3 {Integer}, [] category VALUE GELIJK"
                        + " DECIDED {Kind}]] join [uitkomst-onderwerp_18: onderwerp = $onderwerp] beta"
                        + " [ander-besluit_19: OTHER_DECISION $besluit] na [] bind []]",
                "  gevolg Decision [onderwerp=$onderwerp, value=Lees 0, slots=[0], size=1,"
                        + " collapse=false, fault=null, facts=[Value[target=ElementKey[objectType=jutter,"
                        + " naam=wachtgeld, characteristic=false], writer=defaults A:Jutter.wachtgeld]],"
                        + " insertion=LOGICAL]",
                "    value 0 SlotValue"), plans(golden("jutterswet-wachtgeld")));
    }

    /**
     * A decision inserts the target or its control fact with the value it decided, and a conflict inserts its outcome
     * of the kind {@code CONFLICT} or, at the root, the inconsistency, with its key: the constructors refuse a plan that
     * mixes them, which a rendering would carry out as listed, a key written as a value or a value as a key.
     */
    @Test void a_decision_and_a_conflict_each_insert_their_own_facts() {
        ElementKey target = ElementKey.van("Jutter", "korting", false);
        Consequence.StackFact value = new Consequence.StackFact.Value(target, "defaults A:Jutter.korting");
        Consequence.StackFact decides = new Consequence.StackFact.Outcome("defaults A:Jutter.korting", 0, 0,
                Outcome.Kind.DECIDED);
        Consequence.StackFact conflict = new Consequence.StackFact.Outcome("defaults A:Jutter.korting", 0, 0,
                Outcome.Kind.CONFLICT);
        Consequence.StackFact inconsistency = new Consequence.StackFact.Inconsistency();
        assertEquals(List.of("ok", "ok",
                        "een besluit voegt ten minste één feit in",
                        "een besluit voegt geen conflict in: " + conflict,
                        "een besluit voegt geen conflict in: Inconsistency[]",
                        "ok", "ok",
                        "een conflict voegt zijn uitkomst of de inconsistentie in, niet " + decides,
                        "een conflict voegt zijn uitkomst of de inconsistentie in, niet " + value),
                List.of(decision(List.of(value)), decision(List.of(decides)), decision(List.of()),
                        decision(List.of(value, conflict)), decision(List.of(inconsistency)),
                        conflict(conflict), conflict(inconsistency), conflict(decides), conflict(value)));
    }

    /** "ok" where the constructor of {@link Consequence.Decision} takes a decision that inserts {@code facts}, else its
     *  refusal. */
    private static String decision(List<Consequence.StackFact> facts) {
        try {
            new Consequence.Decision("$onderwerp", new Term.SlotValue(0, null), List.of(0), 1, false, null, facts,
                    Consequence.Insertion.LOGICAL);
            return "ok";
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    /** "ok" where the constructor of {@link Consequence.Conflict} takes a conflict that inserts {@code feit}, else its
     *  refusal. */
    private static String conflict(Consequence.StackFact fact) {
        try {
            new Consequence.Conflict("$onderwerp", new Term.SlotValue(0, null), List.of(0), 1, fact, Consequence.Insertion.LOGICAL);
            return "ok";
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    /** {@code anchor/jutterswet-uitsluiting} with its defaults group valid from 2026-01-01, since no golden has a group
     *  with a dated validity. Verifier-clean. */
    static JsonNode exclusionDated() throws Exception {
        JsonNode d = golden("anchor/jutterswet-uitsluiting");
        JsonNode group = d.get("rules").get("prioritizedDefaults").get(0);
        ((ObjectNode) group.get("validity")).put("from", "2026-01-01");
        return d;
    }

    static JsonNode golden(String naam) throws Exception {
        return JSON.readTree(GOLDEN.resolve(naam).resolve("ir.json").toFile());
    }

    // ---------------- the plans as text ----------------

    /** Every plan of a rule of a defaults stack of {@code document}, in the order of the model, as text; after
     *  a line that holds a term, the term node by node. */
    static List<String> plans(JsonNode document) {
        List<String> out = new ArrayList<>();
        for (RulePlan p : DroolsBackend.build(document, DroolsBackend.Branch.EXEC_MODEL).execModelTranspiler()
                .modelplan().rules()) {
            if (p.form() == null || !p.form().startsWith("defaults ")) continue;
            out.add("regel " + p.naam() + " | vorm " + p.form() + " | groep " + p.agendaGroup());
            List<String> slots = new ArrayList<>();
            for (Read l : p.readings().slots())
                slots.add(l.slot() + " " + l.category() + " " + l.naam() + " (" + l.key() + ")");
            out.add("  lezingen " + slots);
            for (LhsItem item : p.leftSide()) {
                out.add("  " + item(item));
                if (item instanceof TestItem t) TermTree.fields("    toets", t.test(), "0", out);
            }
            out.add("  gevolg " + consequence(p.consequence()));
            try {
                for (RecordComponent c : p.consequence().getClass().getRecordComponents())
                    if (c.getAccessor().invoke(p.consequence()) instanceof Term t)
                        TermTree.fields("    " + c.getName(), t, "0", out);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
        return out;
    }

    private static String item(LhsItem item) {
        return switch (item) {
            case Subject o -> "onderwerp " + o.beanClass().getSimpleName() + " " + o.naam() + " alfa "
                    + alpha(o.alpha()) + " join " + joins(o.joins()) + " beta " + beta(o.betaTests()) + " bind "
                    + bindings(o.bindings());
            case FactPattern p -> "patroon " + pattern(p);
            case Not n -> "niet binnen " + n.inside() + " buiten " + n.outside() + " "
                    + n.patterns().stream().map(DefaultsplanWitnessTest::pattern).toList();
            case Exists b -> "bestaat binnen " + b.inside() + " buiten " + b.outside() + " "
                    + b.patterns().stream().map(DefaultsplanWitnessTest::pattern).toList();
            case Accumulation a -> "accumulatie " + pattern(a.source()) + " functies "
                    + a.aggregations().stream().map(f -> f.category() + " " + f.source() + " -> " + f.target()).toList();
            case TestItem t -> "toets " + t.id() + " " + t.role() + " sonde " + t.depositFlag() + " slots " + t.slots()
                    + " grootte " + t.size() + ": " + TermTree.tree(t.test());
        };
    }

    private static String pattern(FactPattern p) {
        return p.factClass().getSimpleName() + " " + p.naam() + " alfa " + alpha(p.alpha()) + " join "
                + joins(p.joins()) + " beta " + beta(p.betaTests()) + " na " + alpha(p.afterAlpha()) + " bind "
                + bindings(p.bindings());
    }

    /** Each constraint between two objects with its id, its function, the variable on the other side and each of its
     *  constants with its value and its class. */
    private static List<String> beta(List<BetaTest> beta) {
        return beta.stream().map(t -> t.id() + ": " + t.function() + " " + t.target()
                + t.constants().stream().map(c -> " " + value(c.value())).reduce("", String::concat)).toList();
    }

    private static List<String> joins(List<Join> joins) {
        return joins.stream().map(j -> j.id() + ": " + j.field() + " = " + j.target()
                + (j.targetField() == null ? "" : "." + field(j.targetField()))).toList();
    }

    /** Each constraint on the object alone with its id and its field tests: the path, the field, the form of reading,
     *  the comparison, the value with its class (a class by its name), and the class whose getter reads the field
     *  where the test names one. */
    private static List<String> alpha(List<Alpha> alpha) {
        List<String> out = new ArrayList<>();
        for (Alpha a : alpha)
            out.add(a.id() + ": " + a.tests().stream().map(t -> fields(t.path()) + " " + t.field() + " " + t.form()
                    + " " + t.op() + (t.value() == null ? "" : " " + value(t.value()))
                    + (t.declarant() == null ? "" : " via " + t.declarant().getSimpleName())).toList());
        return out;
    }

    /** A value of the analysis with its class: a class by its name, anything else by its text and the simple name of
     *  its class, so that an {@code Integer} and a {@code Long} of one number differ. */
    private static String value(Object w) {
        if (w instanceof Class<?> k) return k.getSimpleName();
        return w + " {" + w.getClass().getSimpleName() + "}";
    }

    private static List<String> bindings(List<Binding> bindings) {
        return bindings.stream().map(b -> b.target() + " = " + fields(b.path()) + " " + field(b.field()) + " " + b.form()
                + (b.category() == null ? "" : " " + b.category().getSimpleName())).toList();
    }

    private static String fields(List<Binding.Field> path) {
        return path.stream().map(DefaultsplanWitnessTest::field).toList().toString();
    }

    private static String field(Binding.Field v) {
        return v.javaClass().getSimpleName() + "." + v.naam();
    }

    /** The kind of {@code g} and every field of it, a term as a tree. */
    private static String consequence(Consequence g) {
        List<String> fields = new ArrayList<>();
        try {
            for (RecordComponent c : g.getClass().getRecordComponents()) {
                Object w = c.getAccessor().invoke(g);
                fields.add(c.getName() + "=" + (w instanceof Term t ? TermTree.tree(t) : String.valueOf(w)));
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return g.getClass().getSimpleName() + " " + fields;
    }
}
