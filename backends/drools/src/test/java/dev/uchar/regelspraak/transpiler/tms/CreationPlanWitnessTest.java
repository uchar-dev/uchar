package dev.uchar.regelspraak.transpiler.tms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.uchar.regelspraak.cases.CaseRunner;
import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.graaf.ProducentConsument.ElementSleutel;
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
import org.junit.jupiter.api.io.TempDir;
import org.kie.api.event.rule.ObjectDeletedEvent;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.event.rule.ObjectUpdatedEvent;
import org.kie.api.event.rule.RuleRuntimeEventListener;
import org.kie.api.runtime.KieSession;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The rules of the creations and the links come whole from a plan (card drl-teksttak-op-truth-maintenance, step 1c-C1):
 * the rule of an object creation with its guard, the rule of a fact creation with the guard on its target, the rule
 * that inserts the links a reference field of the input carries, the rule of a second filler of a to-one role, and the
 * two rules of the standing of a path over a relation a form creates. Each is a {@link RulePlan} whose left-hand side
 * holds what the form built itself before, in its order and with its ids: the negations of the guard, the chain of a
 * path as patterns, a negation over the whole chain with its scope, and a guard on a target after the constraint that
 * reaches it ({@link FactPattern#afterAlpha}); and whose consequence carries every decision as a field: the class of the new
 * bean, its initialisations with their terms, its construction before any insertion, its depth and budget, the path of
 * its fault, each insertion with its kind, and the direction of a link ({@link Consequence.Creation},
 * {@link Consequence.RelationLink}, {@link Consequence.SecondFilling}).
 *
 * <p>This witness pins those plans as text, per plan in order: the name, the form and the group; the slots, each with
 * its kind, its variable and its key; every item of the left-hand side in order with its kind, its pattern class, its
 * constraint ids, each field test with the class it reads the field through where the plan names one, its joins, its
 * constraints between two objects with their function and each constant with every field, the constraints after
 * those, its bindings, and of an existence or a negation the variables it declares and those it reads from outside;
 * and the consequence with every field. A term, the test of a rule or the value of an initialisation, is pinned as a
 * tree and on the lines after it node by node with its source fragment and of a constant the class of its value and
 * its whole IR type ({@link TermTree#fields}).
 *
 * <ul>
 *   <li>{@code anchor/jutterswet-penninguitgifte}: two object creations without a {@code connectingRole} binding, one
 *       with an attribute initialisation and a characteristic initialised to waar, the other without one and with
 *       the guard of a 1:1 relation.</li>
 *   <li>{@code jutterswet-penningbesluit}: an object creation without a binding, whose value reads a parameter.</li>
 *   <li>{@code jutterswet-bakensein}: a fact creation whose guard on the target reads the characteristic off the bean,
 *       through the getter of its declaring type, a supertype of the target; the rule of the links of the input and
 *       the rule of a second filler of the created relation.</li>
 *   <li>{@link #gehesenByRule}, since no golden has one: the same fact creation where a rule writes the
 *       characteristic, so the guard reads its standing, joined to the target. Fired as well, against the values the
 *       reference interpreter and the ASP backend give.</li>
 *   <li>{@code jutterswet-havenketen}: the standing of a path whose first hop goes over the input and whose second
 *       over a relation a fact creation makes, of a member a rule writes and of one off the bean.</li>
 *   <li>{@code anchor/jutterswet-erflijn}: fact creations whose path walks the relation they create, and so not a link
 *       of the phase they fire in.</li>
 * </ul>
 *
 * <p>That the rendering of these plans builds the rules of before the step is what the harness {@code FireTrace}
 * shows, and the executable model of the corpus, which is equal as text; for {@link #gehesenByRule}, outside the
 * corpus, the executable model of before and after the step is equal as text as well. That a creation constructs its
 * new object before it inserts anything is what the constructor of {@link Consequence.Creation} holds and what
 * {@link #the_nieuwe_bean_comes_filled_in_the_working_memory} watches in working memory; that it inserts in the order of
 * its consequence before the step, and in no other, the constructor holds as well
 * ({@link #the_creation_keeps_the_order_of_its_consequence}, fix round 1c-C1).
 *
 * <p><b>Red</b> before the step: these forms built their rules themselves, so there was no plan of them, and this class
 * did not compile, since the plan had no consequence of a creation or a link, no constraint after the joins and no
 * scope of a negation; with only the names of the plans pinned, none of the six documents had a plan of any of these
 * rules.
 */
class CreationPlanWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final ObjectMapper JSON = new ObjectMapper();

    /** A plan of this step's forms: a creation, a link, a second filler, or the standing of a path over a relation a
     *  form creates, which reads that relation's {@code RelationLink} facts. */
    private static boolean ofThisStep(RulePlan p) {
        return switch (p.consequence()) {
            case Consequence.Creation c -> true;
            case Consequence.RelationLink k -> true;
            case Consequence.SecondFilling t -> true;
            case Consequence.InsertPathState s -> p.leftSide().stream().anyMatch(CreationPlanWitnessTest::readsLinks);
            default -> false;
        };
    }

    private static boolean readsLinks(LhsItem item) {
        return switch (item) {
            case FactPattern p -> p.factClass() == RelationLink.class;
            case Not n -> n.patterns().stream().anyMatch(p -> p.factClass() == RelationLink.class);
            default -> false;
        };
    }

    @Test void object_creations_with_initialisations_a_true_characteristic_and_the_watch_of_a_one_to_one_relation()
            throws Exception {
        assertEquals(List.of(
                "regel penning voor een gerechtigde jutter | vorm penning voor een gerechtigde jutter |"
                        + " groep uchar-objectcreatie",
                "  lezingen [0 INVOER $penninggerechtigd (eigen ElementSleutel[objecttype=jutter,"
                        + " naam=penninggerechtigd, kenmerk=true]), 1 INVOER $vondstwaarde (eigen"
                        + " ElementSleutel[objecttype=jutter, naam=vondstwaarde, kenmerk=false]), 2 CREATION_PHASE"
                        + " $fase (creatiefase), 3 ORIGIN $herkomsten_creatie_42 (herkomst)]",
                "  onderwerp Jutter $onderwerp alfa [] join [] beta [] bind [$penninggerechtigd = []"
                        + " Jutter.penninggerechtigd WAAR, $vondstwaarde = [] Jutter.vondstwaarde VALUE]",
                "  patroon CreationPhase $creatiefase alfa [creatiestratum_40: [[] stratum VALUE AT_LEAST"
                        + " 0]] join [] beta [] na [] bind [$fase = [] CreationPhase.number VALUE]",
                "  accumulatie CreationStamp $herkomst_creatie_41 alfa [] join [herkomst_43: object ="
                        + " $onderwerp] beta [] na [] bind [] functies [LIST null -> $herkomsten_creatie_42]",
                "  niet binnen [$gemaakt_creatie_44] buiten [$onderwerp] [CreationStamp $gemaakt_creatie_44"
                        + " alfa [gemaakt_46: [[] regel VALUE GELIJK penning voor een gerechtigde jutter]] join"
                        + " [gemaakt-onderwerp_47: onderwerp = $onderwerp] beta [] na [] bind []]",
                "  niet binnen [$nieuw_creatie_45] buiten [$onderwerp, $fase] [CreationStamp $nieuw_creatie_45"
                        + " alfa [] join [nieuw-object_48: object = $onderwerp] beta [nieuw-in-deze-fase_49:"
                        + " IN_PHASE $fase] na [] bind []]",
                "  toets voorwaarde_50 CONDITION sonde -1 slots [0] grootte 4: KENMERK(Lees 0, Const"
                        + " true)",
                "    toets 0 Call bron 'de Jutter is penninggerechtigd'",
                "    toets 0.0 SlotValue",
                "    toets 0.1 Const Boolean",
                "  niet binnen [$eigen_fout] buiten [$onderwerp] [Fout $eigen_fout alfa [eigen-fout_51:"
                        + " [[] regel VALUE GELIJK penning voor een gerechtigde jutter]] join"
                        + " [eigen-fout-onderwerp_52: onderwerp = $onderwerp] beta [] na [] bind []]",
                "  gevolg Creatie [onderwerp=$onderwerp, creatie=penning voor een gerechtigde jutter,"
                        + " diepte=CreationDepth[origin=3, stratum=0, type=Jutter, budget=100],"
                        + " fout=FaultName[vorm=penning voor een gerechtigde jutter, regel=penning voor een"
                        + " gerechtigde jutter], foutinvoeging=STATED, phase=2, slots=[1, 2, 3], grootte=4]",
                "    constructie Penning zettingen [0 Penning.bedrag]",
                "    initialisatie 0 ElementSleutel[objecttype=penning, naam=bedrag, kenmerk=false]:"
                        + " Lees 1",
                "      waarde 0 SlotValue bron 'de vondstwaarde van de Jutter'",
                "    initialisatie 1 ElementSleutel[objecttype=penning, naam=uitgereikt, kenmerk=true]:"
                        + " Const true",
                "      waarde 0 Const Boolean type Booleaan[] bron 'waar'",
                "    invoegingen [New[insertion=STATED], Mark[insertion=STATED],"
                        + " Link[relation=penningbezit, newIsFirst=false, insertion=STATED],"
                        + " Characteristic[initialisatie=1, insertion=STATED]]",
                "regel dossier voor een gerechtigde jutter | vorm dossier voor een gerechtigde jutter |"
                        + " groep uchar-objectcreatie",
                "  lezingen [0 INVOER $penninggerechtigd (eigen ElementSleutel[objecttype=jutter,"
                        + " naam=penninggerechtigd, kenmerk=true]), 1 CREATION_PHASE $fase (creatiefase), 2 ORIGIN"
                        + " $herkomsten_creatie_55 (herkomst)]",
                "  onderwerp Jutter $onderwerp alfa [geen-gekoppelde-instantie_66: [[] dossier VALUE LEEG]] join"
                        + " [] beta [] bind [$penninggerechtigd = [] Jutter.penninggerechtigd WAAR]",
                "  patroon CreationPhase $creatiefase alfa [creatiestratum_53: [[] stratum VALUE AT_LEAST"
                        + " 0]] join [] beta [] na [] bind [$fase = [] CreationPhase.number VALUE]",
                "  accumulatie CreationStamp $herkomst_creatie_54 alfa [] join [herkomst_56: object ="
                        + " $onderwerp] beta [] na [] bind [] functies [LIST null -> $herkomsten_creatie_55]",
                "  niet binnen [$gemaakt_creatie_57] buiten [$onderwerp] [CreationStamp $gemaakt_creatie_57"
                        + " alfa [gemaakt_59: [[] regel VALUE GELIJK dossier voor een gerechtigde jutter]] join"
                        + " [gemaakt-onderwerp_60: onderwerp = $onderwerp] beta [] na [] bind []]",
                "  niet binnen [$nieuw_creatie_58] buiten [$onderwerp, $fase] [CreationStamp $nieuw_creatie_58"
                        + " alfa [] join [nieuw-object_61: object = $onderwerp] beta [nieuw-in-deze-fase_62:"
                        + " IN_PHASE $fase] na [] bind []]",
                "  toets voorwaarde_63 CONDITION sonde -1 slots [0] grootte 3: KENMERK(Lees 0, Const"
                        + " true)",
                "    toets 0 Call bron 'de Jutter is penninggerechtigd'",
                "    toets 0.0 SlotValue",
                "    toets 0.1 Const Boolean",
                "  niet binnen [$eigen_fout] buiten [$onderwerp] [Fout $eigen_fout alfa [eigen-fout_64:"
                        + " [[] regel VALUE GELIJK dossier voor een gerechtigde jutter]] join"
                        + " [eigen-fout-onderwerp_65: onderwerp = $onderwerp] beta [] na [] bind []]",
                "  gevolg Creatie [onderwerp=$onderwerp, creatie=dossier voor een gerechtigde jutter,"
                        + " diepte=CreationDepth[origin=2, stratum=0, type=Jutter, budget=100],"
                        + " fout=FaultName[vorm=dossier voor een gerechtigde jutter, regel=dossier voor een"
                        + " gerechtigde jutter], foutinvoeging=STATED, phase=1, slots=[1, 2], grootte=3]",
                "    constructie Dossier zettingen []",
                "    invoegingen [New[insertion=STATED], Mark[insertion=STATED],"
                        + " Link[relation=dossiervoering, newIsFirst=false, insertion=STATED]]"),
                plans(golden("anchor/jutterswet-penninguitgifte"), p -> p.consequence() instanceof Consequence.Creation));
    }

    @Test void an_object_creation_without_binding() throws Exception {
        assertEquals(List.of(
                "regel stel het penningbesluit vast | vorm stel het penningbesluit vast | groep"
                        + " uchar-objectcreatie",
                "  lezingen [0 CHARACTERISTIC_STATE $penninggerechtigd (eigen ElementSleutel[objecttype=jutter,"
                        + " naam=penninggerechtigd, kenmerk=true]), 1 PARAMETER $penningbedrag (parameter"
                        + " penningbedrag), 2 CREATION_PHASE $fase (creatiefase), 3 ORIGIN $herkomsten_creatie_21"
                        + " (herkomst)]",
                "  onderwerp Jutter $onderwerp alfa [geen-gekoppelde-instantie_32: [[] toegekend besluit"
                        + " VALUE LEEG]] join [] beta [] bind []",
                "  patroon ParameterState $parameter_1 alfa [parameter_18: [[] parameter VALUE GELIJK"
                        + " penningbedrag]] join [] beta [] na [] bind [$penningbedrag = [] ParameterState.waarde"
                        + " VALUE]",
                "  patroon CreationPhase $creatiefase alfa [creatiestratum_19: [[] stratum VALUE AT_LEAST"
                        + " 0]] join [] beta [] na [] bind [$fase = [] CreationPhase.number VALUE]",
                "  patroon CharacteristicState $stand_0 alfa [stand_16: [[] kenmerk VALUE GELIJK"
                        + " ElementSleutel[objecttype=jutter, naam=penninggerechtigd, kenmerk=true]]] join"
                        + " [stand-onderwerp_17: onderwerp = $onderwerp] beta [] na [] bind [$penninggerechtigd ="
                        + " [] CharacteristicState.holds VALUE]",
                "  accumulatie CreationStamp $herkomst_creatie_20 alfa [] join [herkomst_22: object ="
                        + " $onderwerp] beta [] na [] bind [] functies [LIST null -> $herkomsten_creatie_21]",
                "  niet binnen [$gemaakt_creatie_23] buiten [$onderwerp] [CreationStamp $gemaakt_creatie_23"
                        + " alfa [gemaakt_25: [[] regel VALUE GELIJK stel het penningbesluit vast]] join"
                        + " [gemaakt-onderwerp_26: onderwerp = $onderwerp] beta [] na [] bind []]",
                "  niet binnen [$nieuw_creatie_24] buiten [$onderwerp, $fase] [CreationStamp $nieuw_creatie_24"
                        + " alfa [] join [nieuw-object_27: object = $onderwerp] beta [nieuw-in-deze-fase_28:"
                        + " IN_PHASE $fase] na [] bind []]",
                "  toets voorwaarde_29 CONDITION sonde -1 slots [0] grootte 4: KENMERK(Lees 0, Const"
                        + " true)",
                "    toets 0 Call bron 'hij penninggerechtigd is'",
                "    toets 0.0 SlotValue",
                "    toets 0.1 Const Boolean",
                "  niet binnen [$eigen_fout] buiten [$onderwerp] [Fout $eigen_fout alfa [eigen-fout_30:"
                        + " [[] regel VALUE GELIJK stel het penningbesluit vast]] join [eigen-fout-onderwerp_31:"
                        + " onderwerp = $onderwerp] beta [] na [] bind []]",
                "  gevolg Creatie [onderwerp=$onderwerp, creatie=stel het penningbesluit vast,"
                        + " diepte=CreationDepth[origin=3, stratum=0, type=Jutter, budget=100],"
                        + " fout=FaultName[vorm=stel het penningbesluit vast, regel=stel het penningbesluit vast],"
                        + " foutinvoeging=STATED, phase=2, slots=[1, 2, 3], grootte=4]",
                "    constructie Penningbesluit zettingen [0 Penningbesluit.besluitbedrag]",
                "    initialisatie 0 ElementSleutel[objecttype=penningbesluit, naam=besluitbedrag,"
                        + " kenmerk=false]: Lees 1",
                "      waarde 0 SlotValue bron 'het penningbedrag'",
                "    invoegingen [New[insertion=STATED], Mark[insertion=STATED],"
                        + " Link[relation=penningtoekenning, newIsFirst=false, insertion=STATED]]"),
                plans(golden("jutterswet-penningbesluit"), p -> p.consequence() instanceof Consequence.Creation));
    }

    @Test void a_fact_creation_that_reads_the_characteristic_of_the_target_of_the_bean_with_the_link_rules() throws Exception {
        assertEquals(List.of(
                "regel vakbeseining door het kustvak | vorm vakbeseining door het kustvak | groep"
                        + " uchar-feitcreatie",
                "  lezingen [0 CREATION_PHASE $fase (creatiefase)]",
                "  onderwerp Kustvak $onderwerp alfa [] join [] beta [] bind []",
                "  patroon CreationPhase $creatiefase alfa [creatiestratum_13: [[] stratum VALUE AT_LEAST"
                        + " 0]] join [] beta [] na [] bind [$fase = [] CreationPhase.number VALUE]",
                "  patroon Post $stap_keten_14 alfa [] join [] beta [veld-ver_15: MEETING $onderwerp"
                        + " [kustvak] {List<String>, type null, bron null} [] {List<>, type null, bron null}] na"
                        + " [] bind []",
                "  patroon Sein $stap_keten_16 alfa [] join [] beta [veld-ver_17: MEETING"
                        + " $stap_keten_14 [post] {List<String>, type null, bron null} [] {List<>, type null, bron"
                        + " null}] na [doel-gastheer_18: [[] null VALUE IS_A Baken], doel-kenmerk_19: [[]"
                        + " gehesen WAAR GELIJK true via Baken]] bind []",
                "  niet binnen [$bestaand_feitcreatie_20] buiten [$onderwerp, $stap_keten_16] [RelationLink"
                        + " $bestaand_feitcreatie_20 alfa [bestaand_21: [[] relation VALUE GELIJK vakbeseining]]"
                        + " join [bestaand-anker_22: eerste = $onderwerp, bestaand-doel_23: tweede ="
                        + " $stap_keten_16] beta [] na [] bind []]",
                "  gevolg RelationLink[relation=vakbeseining, eerste=$onderwerp, tweede=$stap_keten_16,"
                        + " phase=0, insertion=STATED]",
                "regel koppeling vakbeseining uit de invoer: Sein.seinvak | vorm null | groep null",
                "  lezingen []",
                "  onderwerp Sein $houder alfa [koppeling-invoer_24: [[] seinvak VALUE GEVULD]] join []"
                        + " beta [] bind [$ander = [] Sein.seinvak VALUE]",
                "  gevolg RelationLink[relation=vakbeseining, eerste=$ander, tweede=$houder, phase=-1,"
                        + " insertion=LOGICAL]",
                "regel koppeling vakbeseining: tweede vulling van seinvak | vorm null | groep null",
                "  lezingen []",
                "  patroon RelationLink $een alfa [vulling_25: [[] relation VALUE GELIJK vakbeseining]]"
                        + " join [] beta [] na [] bind [$nabij = [] RelationLink.tweede VALUE]",
                "  patroon RelationLink $twee alfa [tweede-vulling_26: [[] relation VALUE GELIJK"
                        + " vakbeseining]] join [tweede-vulling-nabij_27: tweede = $een.RelationLink.tweede] beta"
                        + " [tweede-vulling-ander_28: OTHER_FILLING $een eerste {String, type null, bron null}]"
                        + " na [] bind []",
                "  gevolg SecondFilling[onderwerp=$nabij, relation=vakbeseining, sentence=de naar-één-rol"
                        + " \"seinvak\" van een Sein heeft twee verschillende vullingen; een tweede vulling is een"
                        + " fout, geen overschrijving (RUNTIME-SEMANTICS ch. 5.5), insertion=LOGICAL]"),
                plans(golden("jutterswet-bakensein"), CreationPlanWitnessTest::ofThisStep));
    }

    @Test void a_fact_creation_that_reads_the_characteristic_of_the_target_out_of_its_state() throws Exception {
        assertEquals(List.of(
                "regel vakbeseining door het kustvak | vorm vakbeseining door het kustvak | groep"
                        + " uchar-feitcreatie",
                "  lezingen [0 CREATION_PHASE $fase (creatiefase)]",
                "  onderwerp Kustvak $onderwerp alfa [] join [] beta [] bind []",
                "  patroon CreationPhase $creatiefase alfa [creatiestratum_14: [[] stratum VALUE AT_LEAST"
                        + " 0]] join [] beta [] na [] bind [$fase = [] CreationPhase.number VALUE]",
                "  patroon Post $stap_keten_15 alfa [] join [] beta [veld-ver_16: MEETING $onderwerp"
                        + " [kustvak] {List<String>, type null, bron null} [] {List<>, type null, bron null}] na"
                        + " [] bind []",
                "  patroon Sein $stap_keten_17 alfa [] join [] beta [veld-ver_18: MEETING"
                        + " $stap_keten_15 [post] {List<String>, type null, bron null} [] {List<>, type null, bron"
                        + " null}] na [doel-gastheer_19: [[] null VALUE IS_A Baken]] bind []",
                "  patroon CharacteristicState $doelstand_doel_20 alfa [doelstand_21: [[] kenmerk VALUE GELIJK"
                        + " ElementSleutel[objecttype=baken, naam=gehesen, kenmerk=true]]] join"
                        + " [doelstand-doel_22: onderwerp = $stap_keten_17] beta [] na [doelstand-waar_23: [[]"
                        + " holds VALUE GELIJK true]] bind []",
                "  niet binnen [$bestaand_feitcreatie_24] buiten [$onderwerp, $stap_keten_17] [RelationLink"
                        + " $bestaand_feitcreatie_24 alfa [bestaand_25: [[] relation VALUE GELIJK vakbeseining]]"
                        + " join [bestaand-anker_26: eerste = $onderwerp, bestaand-doel_27: tweede ="
                        + " $stap_keten_17] beta [] na [] bind []]",
                "  gevolg RelationLink[relation=vakbeseining, eerste=$onderwerp, tweede=$stap_keten_17,"
                        + " phase=0, insertion=STATED]",
                "regel koppeling vakbeseining uit de invoer: Sein.seinvak | vorm null | groep null",
                "  lezingen []",
                "  onderwerp Sein $houder alfa [koppeling-invoer_28: [[] seinvak VALUE GEVULD]] join []"
                        + " beta [] bind [$ander = [] Sein.seinvak VALUE]",
                "  gevolg RelationLink[relation=vakbeseining, eerste=$ander, tweede=$houder, phase=-1,"
                        + " insertion=LOGICAL]"),
                plans(gehesenByRule(), p -> p.consequence() instanceof Consequence.RelationLink));
    }

    @Test void the_path_states_over_a_chain_with_a_made_link() throws Exception {
        assertEquals(List.of(
                "regel padstand Vondst.jutter.haven naar Haven.afgeleide openingsdag: link aanwezig |"
                        + " vorm null | groep null",
                "  lezingen [0 REACHED_STATE $v (stand Vondst:jutter.haven"
                        + " ElementSleutel[objecttype=haven, naam=afgeleide openingsdag, kenmerk=false])]",
                "  onderwerp Vondst $onderwerp alfa [] join [] beta [] bind []",
                "  patroon Jutter $stap_keten_24 alfa [] join [] beta [veld-nabij_25: MEETING"
                        + " $onderwerp [] {List<>, type null, bron null} [jutter] {List<String>, type null, bron"
                        + " null}] na [] bind []",
                "  patroon RelationLink $koppeling_keten_27 alfa [koppeling_29: [[] relation VALUE GELIJK"
                        + " havenheeftjutter]] join [koppeling-nabij_30: tweede = $stap_keten_24] beta [] na []"
                        + " bind [$overzijde_keten_28 = [] RelationLink.eerste VALUE]",
                "  patroon Haven $stap_keten_26 alfa [] join [koppeling-ver_31: null ="
                        + " $overzijde_keten_28] beta [] na [] bind []",
                "  patroon State $stand alfa [stand_32: [[] attribuut VALUE GELIJK"
                        + " ElementSleutel[objecttype=haven, naam=afgeleide openingsdag, kenmerk=false]]] join"
                        + " [stand-bereikt_33: onderwerp = $stap_keten_26] beta [] na [] bind [$v = []"
                        + " State.waarde VALUE]",
                "  gevolg InsertPathState[onderwerp=$onderwerp, pad=Vondst:jutter.haven,"
                        + " member=ElementSleutel[objecttype=haven, naam=afgeleide openingsdag, kenmerk=false],"
                        + " bron=REACHED, waarde=0, insertion=LOGICAL]",
                "regel padstand Vondst.jutter.haven naar Haven.afgeleide openingsdag: link afwezig |"
                        + " vorm null | groep null",
                "  lezingen []",
                "  onderwerp Vondst $onderwerp alfa [] join [] beta [] bind []",
                "  niet binnen [$stap_keten_34, $koppeling_keten_37, $overzijde_keten_38,"
                        + " $stap_keten_36] buiten [$onderwerp] [Jutter $stap_keten_34 alfa [] join [] beta"
                        + " [veld-nabij_35: MEETING $onderwerp [] {List<>, type null, bron null} [jutter]"
                        + " {List<String>, type null, bron null}] na [] bind [], RelationLink $koppeling_keten_37"
                        + " alfa [koppeling_39: [[] relation VALUE GELIJK havenheeftjutter]] join"
                        + " [koppeling-nabij_40: tweede = $stap_keten_34] beta [] na [] bind [$overzijde_keten_38"
                        + " = [] RelationLink.eerste VALUE], Haven $stap_keten_36 alfa [] join [koppeling-ver_41:"
                        + " null = $overzijde_keten_38] beta [] na [] bind []]",
                "  gevolg InsertPathState[onderwerp=$onderwerp, pad=Vondst:jutter.haven,"
                        + " member=ElementSleutel[objecttype=haven, naam=afgeleide openingsdag, kenmerk=false],"
                        + " bron=MISSING_LINK, waarde=-1, insertion=LOGICAL]",
                "regel padstand Vondst.jutter.haven naar Haven.openingsdag: link aanwezig | vorm null |"
                        + " groep null",
                "  lezingen [0 REACHED_INPUT $v (invoer Vondst:jutter.haven"
                        + " ElementSleutel[objecttype=haven, naam=openingsdag, kenmerk=false])]",
                "  onderwerp Vondst $onderwerp alfa [] join [] beta [] bind []",
                "  patroon Jutter $stap_keten_42 alfa [] join [] beta [veld-nabij_43: MEETING"
                        + " $onderwerp [] {List<>, type null, bron null} [jutter] {List<String>, type null, bron"
                        + " null}] na [] bind []",
                "  patroon RelationLink $koppeling_keten_45 alfa [koppeling_47: [[] relation VALUE GELIJK"
                        + " havenheeftjutter]] join [koppeling-nabij_48: tweede = $stap_keten_42] beta [] na []"
                        + " bind [$overzijde_keten_46 = [] RelationLink.eerste VALUE]",
                "  patroon Haven $stap_keten_44 alfa [] join [koppeling-ver_49: null ="
                        + " $overzijde_keten_46] beta [] na [] bind [$v = [] Haven.openingsdag VALUE]",
                "  gevolg InsertPathState[onderwerp=$onderwerp, pad=Vondst:jutter.haven,"
                        + " member=ElementSleutel[objecttype=haven, naam=openingsdag, kenmerk=false], bron=REACHED,"
                        + " waarde=0, insertion=LOGICAL]",
                "regel padstand Vondst.jutter.haven naar Haven.openingsdag: link afwezig | vorm null |"
                        + " groep null",
                "  lezingen []",
                "  onderwerp Vondst $onderwerp alfa [] join [] beta [] bind []",
                "  niet binnen [$stap_keten_50, $koppeling_keten_53, $overzijde_keten_54,"
                        + " $stap_keten_52] buiten [$onderwerp] [Jutter $stap_keten_50 alfa [] join [] beta"
                        + " [veld-nabij_51: MEETING $onderwerp [] {List<>, type null, bron null} [jutter]"
                        + " {List<String>, type null, bron null}] na [] bind [], RelationLink $koppeling_keten_53"
                        + " alfa [koppeling_55: [[] relation VALUE GELIJK havenheeftjutter]] join"
                        + " [koppeling-nabij_56: tweede = $stap_keten_50] beta [] na [] bind [$overzijde_keten_54"
                        + " = [] RelationLink.eerste VALUE], Haven $stap_keten_52 alfa [] join [koppeling-ver_57:"
                        + " null = $overzijde_keten_54] beta [] na [] bind []]",
                "  gevolg InsertPathState[onderwerp=$onderwerp, pad=Vondst:jutter.haven,"
                        + " member=ElementSleutel[objecttype=haven, naam=openingsdag, kenmerk=false],"
                        + " bron=MISSING_LINK, waarde=-1, insertion=LOGICAL]"),
                plans(golden("jutterswet-havenketen"), p -> p.consequence() instanceof Consequence.InsertPathState
                        && p.leftSide().stream().anyMatch(CreationPlanWitnessTest::readsLinks)));
    }

    @Test void a_fact_creation_walks_no_link_of_its_own_phase() throws Exception {
        assertEquals(List.of(
                "regel erf de directe opvolger | vorm erf de directe opvolger | groep uchar-feitcreatie",
                "  lezingen [0 CREATION_PHASE $fase (creatiefase)]",
                "  onderwerp Wachter $onderwerp alfa [] join [] beta [] bind []",
                "  patroon CreationPhase $creatiefase alfa [creatiestratum_8: [[] stratum VALUE AT_LEAST"
                        + " 0]] join [] beta [] na [] bind [$fase = [] CreationPhase.number VALUE]",
                "  patroon Wachter $stap_keten_9 alfa [] join [] beta [veld-nabij_10: MEETING"
                        + " $onderwerp [] {List<>, type null, bron null} [opvolger] {List<String>, type null, bron"
                        + " null}] na [] bind []",
                "  niet binnen [$bestaand_feitcreatie_11] buiten [$onderwerp, $stap_keten_9] [RelationLink"
                        + " $bestaand_feitcreatie_11 alfa [bestaand_12: [[] relation VALUE GELIJK erflijn]] join"
                        + " [bestaand-anker_13: eerste = $onderwerp, bestaand-doel_14: tweede = $stap_keten_9]"
                        + " beta [] na [] bind []]",
                "  gevolg RelationLink[relation=erflijn, eerste=$onderwerp, tweede=$stap_keten_9, phase=0,"
                        + " insertion=STATED]",
                "regel erfestafette: erf de lijn door | vorm erf de lijn door | groep uchar-feitcreatie",
                "  lezingen [0 CREATION_PHASE $fase (creatiefase)]",
                "  onderwerp Wachter $onderwerp alfa [] join [] beta [] bind []",
                "  patroon CreationPhase $creatiefase alfa [creatiestratum_15: [[] stratum VALUE AT_LEAST"
                        + " 1]] join [] beta [] na [] bind [$fase = [] CreationPhase.number VALUE]",
                "  patroon RelationLink $koppeling_keten_17 alfa [koppeling_19: [[] relation VALUE GELIJK"
                        + " erflijn]] join [koppeling-nabij_20: eerste = $onderwerp] beta [koppeling-eerder_21:"
                        + " NOT_IN_PHASE $fase] na [] bind [$overzijde_keten_18 = [] RelationLink.tweede VALUE]",
                "  patroon Wachter $stap_keten_16 alfa [] join [koppeling-ver_22: null ="
                        + " $overzijde_keten_18] beta [] na [] bind []",
                "  patroon Wachter $stap_keten_23 alfa [] join [] beta [veld-nabij_24: MEETING"
                        + " $stap_keten_16 [] {List<>, type null, bron null} [opvolger] {List<String>, type null,"
                        + " bron null}] na [] bind []",
                "  niet binnen [$bestaand_feitcreatie_25] buiten [$onderwerp, $stap_keten_23] [RelationLink"
                        + " $bestaand_feitcreatie_25 alfa [bestaand_26: [[] relation VALUE GELIJK erflijn]] join"
                        + " [bestaand-anker_27: eerste = $onderwerp, bestaand-doel_28: tweede = $stap_keten_23]"
                        + " beta [] na [] bind []]",
                "  gevolg RelationLink[relation=erflijn, eerste=$onderwerp, tweede=$stap_keten_23, phase=0,"
                        + " insertion=STATED]"),
                plans(golden("anchor/jutterswet-erflijn"), p -> p.consequence() instanceof Consequence.RelationLink));
    }

    // ---------------- the construction before the insertion ----------------

    /**
     * The plan of a creation holds the order of its consequence: every initialisation is set on the new bean in its
     * construction where it is an attribute, or inserted as a characteristic after the bean where it is one, exactly
     * once, and the bean is the first insertion. The constructor refuses a plan that would write on a bean in working
     * memory, as a later setter after the insertion would, or that would insert a fact naming the bean before it.
     */
    @Test void the_plan_of_a_creation_constructs_before_inserting() {
        ElementSleutel amount = ElementSleutel.van("Penning", "bedrag", false);
        ElementSleutel uitgereikt = ElementSleutel.van("Penning", "uitgereikt", true);
        List<Consequence.Initialisation> inits = List.of(new Consequence.Initialisation(amount, new Term.SlotValue(1, "bedrag")),
                new Consequence.Initialisation(uitgereikt, new Term.Const(true, null, "waar")));
        Consequence.Construction bouw = new Consequence.Construction(Object.class,
                List.of(new Consequence.Construction.Setting(0, new Binding.Field(Object.class, "bedrag"))));
        Consequence.Insert fresh = new Consequence.Insert.New(Consequence.Insertion.STATED);
        Consequence.Insert mark = new Consequence.Insert.Mark(Consequence.Insertion.STATED);
        Consequence.Insert link = new Consequence.Insert.Link("uitgifte", true, Consequence.Insertion.STATED);
        Consequence.Insert kenmerk = new Consequence.Insert.Characteristic(1, Consequence.Insertion.STATED);
        assertEquals(List.of("ok",
                        "een creatie voegt eerst het nieuwe object in: [Mark[insertion=STATED],"
                                + " New[insertion=STATED], Link[relation=uitgifte, newIsFirst=true,"
                                + " insertion=STATED], Characteristic[initialisatie=1, insertion=STATED]]",
                        "een kenmerkinitialisatie wordt ingevoegd, niet gezet: " + uitgereikt,
                        "een attribuutinitialisatie wordt gezet, niet ingevoegd: " + amount,
                        "de initialisatie van " + uitgereikt + " komt 0 keer in de constructie of de invoegingen,"
                                + " niet één keer",
                        "een creatie voegt het nieuwe object, zijn merk en zijn koppeling elk één keer in:"
                                + " [New[insertion=STATED], Mark[insertion=STATED], Characteristic[initialisatie=1,"
                                + " insertion=STATED]]"),
                List.of(creatie(inits, bouw, List.of(fresh, mark, link, kenmerk)),
                        creatie(inits, bouw, List.of(mark, fresh, link, kenmerk)),
                        creatie(inits, new Consequence.Construction(Object.class, List.of(bouw.settings().get(0),
                                new Consequence.Construction.Setting(1, new Binding.Field(Object.class, "uitgereikt")))),
                                List.of(fresh, mark, link, kenmerk)),
                        creatie(inits, new Consequence.Construction(Object.class, List.of()),
                                List.of(fresh, mark, link, kenmerk, new Consequence.Insert.Characteristic(0,
                                        Consequence.Insertion.STATED))),
                        creatie(inits, bouw, List.of(fresh, mark, link)),
                        creatie(inits, bouw, List.of(fresh, mark, kenmerk))));
    }

    /**
     * The constructor holds a creation to the order of its consequence before step 1c-C1 exactly (fix round 1c-C1): the
     * new object, its mark, its link, and then the characteristics in the order of their initialisations, with the
     * attributes set in that order too. A plan whose kinds and counts are right but whose order is another, which a
     * rendering would carry out as listed, is refused: the mark and the link swapped, two characteristics reversed, a
     * characteristic before the link, two attributes set in reverse.
     */
    @Test void the_creation_keeps_the_order_of_its_consequence() {
        ElementSleutel amount = ElementSleutel.van("Penning", "bedrag", false);
        ElementSleutel weight = ElementSleutel.van("Penning", "gewicht", false);
        ElementSleutel uitgereikt = ElementSleutel.van("Penning", "uitgereikt", true);
        ElementSleutel minted = ElementSleutel.van("Penning", "gemunt", true);
        List<Consequence.Initialisation> inits = List.of(new Consequence.Initialisation(amount, new Term.SlotValue(1, "bedrag")),
                new Consequence.Initialisation(uitgereikt, new Term.Const(true, null, "waar")),
                new Consequence.Initialisation(minted, new Term.Const(true, null, "waar")),
                new Consequence.Initialisation(weight, new Term.SlotValue(1, "gewicht")));
        Consequence.Construction.Setting setAmount = new Consequence.Construction.Setting(0, new Binding.Field(Object.class, "bedrag"));
        Consequence.Construction.Setting setWeight = new Consequence.Construction.Setting(3,
                new Binding.Field(Object.class, "gewicht"));
        Consequence.Construction bouw = new Consequence.Construction(Object.class, List.of(setAmount, setWeight));
        Consequence.Insert fresh = new Consequence.Insert.New(Consequence.Insertion.STATED);
        Consequence.Insert mark = new Consequence.Insert.Mark(Consequence.Insertion.STATED);
        Consequence.Insert link = new Consequence.Insert.Link("uitgifte", true, Consequence.Insertion.STATED);
        Consequence.Insert eerste = new Consequence.Insert.Characteristic(1, Consequence.Insertion.STATED);
        Consequence.Insert tweede = new Consequence.Insert.Characteristic(2, Consequence.Insertion.STATED);
        String verwacht = "een creatie voegt het nieuwe object, zijn merk, zijn koppeling en dan haar"
                + " kenmerkinitialisaties in hun volgorde in, [New, Mark, Link, Kenmerk 1, Kenmerk 2], niet ";
        assertEquals(List.of("ok",
                        verwacht + "[New, Link, Mark, Kenmerk 1, Kenmerk 2]",
                        verwacht + "[New, Mark, Link, Kenmerk 2, Kenmerk 1]",
                        verwacht + "[New, Mark, Kenmerk 1, Link, Kenmerk 2]",
                        "een creatie zet haar attribuutinitialisaties in hun volgorde [0, 3], niet [3, 0]"),
                List.of(creatie(inits, bouw, List.of(fresh, mark, link, eerste, tweede)),
                        creatie(inits, bouw, List.of(fresh, link, mark, eerste, tweede)),
                        creatie(inits, bouw, List.of(fresh, mark, link, tweede, eerste)),
                        creatie(inits, bouw, List.of(fresh, mark, eerste, link, tweede)),
                        creatie(inits, new Consequence.Construction(Object.class, List.of(setWeight, setAmount)),
                                List.of(fresh, mark, link, eerste, tweede))));
    }

    /**
     * A creation states what it makes (RUNTIME-SEMANTICS ch. 5.5; the owner's decision at card 1: a creation is stated,
     * not logical), and the analysis makes every insertion of a creation {@code GESTELD}. The constructor refuses a plan
     * that would insert the new object, its mark, its link or a characteristic logically, and one that would insert the
     * fault at the making logically, each with a sentence that names it (round 5b, on the lens of round 5: a rendering
     * that inserted a logical characteristic as a stated one passed every measurement, since no plan of the analysis
     * had one). <b>Red</b> on {@code ea8b5c6f}, where the constructor took all six plans: "expected: &lt;[ok, een
     * creatie stelt wat ze maakt, …]&gt; but was: &lt;[ok, ok, ok, ok, ok, ok]&gt;".
     */
    @Test void a_creation_sets_what_it_makes() {
        ElementSleutel amount = ElementSleutel.van("Penning", "bedrag", false);
        ElementSleutel uitgereikt = ElementSleutel.van("Penning", "uitgereikt", true);
        List<Consequence.Initialisation> inits = List.of(new Consequence.Initialisation(amount, new Term.SlotValue(1, "bedrag")),
                new Consequence.Initialisation(uitgereikt, new Term.Const(true, null, "waar")));
        Consequence.Construction bouw = new Consequence.Construction(Object.class,
                List.of(new Consequence.Construction.Setting(0, new Binding.Field(Object.class, "bedrag"))));
        Consequence.Insertion set = Consequence.Insertion.STATED, logical = Consequence.Insertion.LOGICAL;
        Consequence.Insert fresh = new Consequence.Insert.New(set);
        Consequence.Insert mark = new Consequence.Insert.Mark(set);
        Consequence.Insert link = new Consequence.Insert.Link("uitgifte", true, set);
        Consequence.Insert kenmerk = new Consequence.Insert.Characteristic(1, set);
        String sentence = "een creatie stelt wat ze maakt, want dat vervalt nooit: ";
        assertEquals(List.of("ok",
                        sentence + "New[insertion=LOGICAL] is niet gesteld",
                        sentence + "Mark[insertion=LOGICAL] is niet gesteld",
                        sentence + "Link[relation=uitgifte, newIsFirst=true, insertion=LOGICAL] is niet gesteld",
                        sentence + "Characteristic[initialisatie=1, insertion=LOGICAL] is niet gesteld",
                        "een creatie stelt haar fout bij het maken, want die houdt haar regel tegen: de foutinvoeging"
                                + " LOGICAL is niet gesteld"),
                List.of(creatie(inits, bouw, List.of(fresh, mark, link, kenmerk)),
                        creatie(inits, bouw, List.of(new Consequence.Insert.New(logical), mark, link, kenmerk)),
                        creatie(inits, bouw, List.of(fresh, new Consequence.Insert.Mark(logical), link, kenmerk)),
                        creatie(inits, bouw, List.of(fresh, mark, new Consequence.Insert.Link("uitgifte", true, logical),
                                kenmerk)),
                        creatie(inits, bouw, List.of(fresh, mark, link, new Consequence.Insert.Characteristic(1, logical))),
                        creatie(inits, bouw, List.of(fresh, mark, link, kenmerk), logical)));
    }

    /** "ok" where the constructor of {@link Consequence.Creation} takes the plan, else its refusal. */
    private static String creatie(List<Consequence.Initialisation> inits, Consequence.Construction bouw,
                                  List<Consequence.Insert> insertions) {
        return creatie(inits, bouw, insertions, Consequence.Insertion.STATED);
    }

    /** {@link #creatie(List, Consequence.Construction, List)} with the fault at the making inserted as {@code faultInsertion}. */
    private static String creatie(List<Consequence.Initialisation> inits, Consequence.Construction bouw,
                                  List<Consequence.Insert> insertions, Consequence.Insertion faultInsertion) {
        try {
            new Consequence.Creation("$onderwerp", "uitgifte", new Consequence.CreationDepth(0, 1, "Jutter", 100), inits,
                    new Consequence.FaultName("uitgifte", "uitgifte"), faultInsertion, bouw, 2, insertions,
                    List.of(0, 1, 2), 3);
            return "ok";
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    /**
     * The rendering follows the plan: in a fired case of {@code anchor/jutterswet-penninguitgifte} the new Penning
     * enters working memory with its bedrag set, the value of its attribute initialisation, and its characteristic
     * uitgereikt comes after it as a {@code HasCharacteristic}; no bean is updated in working memory. The insertions follow
     * the plan's order: the bean, its {@code CreationStamp}, its {@code RelationLink}, then the characteristic. The dossier,
     * created without an initialisation, enters as it is made.
     */
    @Test void the_nieuwe_bean_comes_filled_in_the_working_memory() throws Exception {
        Path map = GOLDEN.resolve("anchor/jutterswet-penninguitgifte");
        assertEquals(List.of(
                "bean Penning1 {getBedrag=12, getHouder=null, isUitgereikt=false}",
                "Gemaakt penning voor een gerechtigde jutter: J1 -> Penning1, fase 1, diepte 1",
                "Koppeling penningbezit: J1, Penning1, fase 1",
                "HeeftKenmerk Penning1 uitgereikt",
                "bean Dossier2 {getDossierhouder=null}",
                "Gemaakt dossier voor een gerechtigde jutter: J1 -> Dossier2, fase 1, diepte 1",
                "Koppeling dossiervoering: J1, Dossier2, fase 1"),
                insertions(map.resolve("ir.json"), JSON.readTree(map.resolve("case1.json").toFile())));
    }

    /** The insertions and updates of the session of {@code caseDocument} on {@code ir} after the input, in order: a bean a
     *  creation made with its attributes as they stand at its insertion, and each fact of a creation by its class and
     *  what it names. Through the measuring seam of {@code CaseRunner}, by reflection, as
     *  {@code FoldPlanWitnessTest.filterErrors} reads its facts. */
    @SuppressWarnings("unchecked")
    private static List<String> insertions(Path ir, JsonNode caseDocument) throws Exception {
        Method forMeasurement = CaseRunner.class.getDeclaredMethod("forMeasurement", Path.class, DroolsBackend.Branch.class);
        forMeasurement.setAccessible(true);
        Object runner = forMeasurement.invoke(null, ir, DroolsBackend.Branch.EXEC_MODEL);
        Class<?> measurement = Class.forName("dev.uchar.regelspraak.cases.CaseRunner$SessionMeasurement");
        Object reader = Proxy.newProxyInstance(measurement.getClassLoader(), new Class<?>[]{measurement}, (proxy, m, a) -> {
            if (!m.getName().equals("meet")) throw new UnsupportedOperationException(m.getName());
            DroolsBackend.Build bouw = (DroolsBackend.Build) a[0];
            KieSession sessie = (KieSession) a[1];
            Map<Object, String> label = new IdentityHashMap<>();
            ((Map<String, Object>) a[2]).forEach((l, beanObject) -> label.put(beanObject, l));
            List<String> out = new ArrayList<>();
            Map<Object, String> made = new IdentityHashMap<>();
            sessie.addEventListener(new RuleRuntimeEventListener() {
                @Override public void objectInserted(ObjectInsertedEvent e) {
                    Object o = e.getObject();
                    // a bean of the model that is no input, and no §9.5 inconsistency, is one a creation made
                    if (o.getClass().getClassLoader() == bouw.beanLoader() && !label.containsKey(o)
                            && !o.getClass().getName().equals(TmsTranspiler.INCONSISTENCY)) {
                        made.put(o, o.getClass().getSimpleName() + (made.size() + 1));
                        out.add("bean " + made.get(o) + " " + fields(o, label, made, bouw.beanLoader()));
                    } else if (o instanceof CreationStamp g)
                        out.add("Gemaakt " + g.regel() + ": " + naam(g.onderwerp(), label, made) + " -> "
                                + naam(g.object(), label, made) + ", fase " + g.phase() + ", diepte " + g.depth());
                    else if (o instanceof RelationLink k && k.phase() > 0)
                        out.add("Koppeling " + k.relation() + ": " + naam(k.eerste(), label, made) + ", "
                                + naam(k.tweede(), label, made) + ", fase " + k.phase());
                    else if (o instanceof HasCharacteristic h && made.containsKey(h.onderwerp()))
                        out.add("HeeftKenmerk " + naam(h.onderwerp(), label, made) + " " + h.kenmerk().naam());
                }

                @Override public void objectUpdated(ObjectUpdatedEvent e) {
                    out.add("bijgewerkt " + e.getObject());
                }

                @Override public void objectDeleted(ObjectDeletedEvent e) {}
            });
            DroolsBackend.Vuring firing = DroolsBackend.vuur(bouw, sessie);
            if (firing.geenFixpunt() != null) throw new AssertionError(firing.geenFixpunt());
            return out;
        });
        Method withSession = CaseRunner.class.getDeclaredMethod("withSession", JsonNode.class, measurement);
        withSession.setAccessible(true);
        return (List<String>) withSession.invoke(runner, caseDocument, reader);
    }

    private static String naam(Object o, Map<Object, String> label, Map<Object, String> made) {
        return label.containsKey(o) ? label.get(o) : made.getOrDefault(o, "?");
    }

    /** The properties of bean {@code o} that a getter without arguments gives, by getter name in name order, a bean
     *  among them by its label or the name it got here. */
    private static String fields(Object o, Map<Object, String> label, Map<Object, String> made, ClassLoader beans) {
        java.util.TreeMap<String, Object> out = new java.util.TreeMap<>();
        for (Method m : o.getClass().getMethods())
            if (m.getParameterCount() == 0 && m.getDeclaringClass() != Object.class
                    && (m.getName().startsWith("get") || m.getName().startsWith("is"))) {
                Object w;
                try {
                    w = m.invoke(o);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
                out.put(m.getName(), w != null && w.getClass().getClassLoader() == beans ? naam(w, label, made) : w);
            }
        return out.toString();
    }

    // ---------------- the document of a characteristic a rule writes ----------------

    /** The case of {@link #gehesenByRule}: P1, the post of V1, carries S1, S2 and S3. S1's vlaghoogte 5 makes it
     *  gehesen by the rule, S2's 1 does not and it has no input, and S3, at 1 as well, is gehesen in the input. The
     *  creation links V1 to S1 and S3, so V1 counts 2 vaksein and P1 3 seinen. The reference interpreter and the ASP
     *  backend both pass these three expectations. */
    private static final String GEHESEN_CASE = """
            {"format": "uchar-case-v0", "asOfDate": null, "input": {"instances": [
              {"label": "V1", "objectType": "Kustvak", "attributes": {}, "roles": {"posten": ["P1"]},
               "characteristics": ["gehesen"]},
              {"label": "P1", "objectType": "Post", "attributes": {}, "roles": {"kustvak": ["V1"],
               "seinen": ["S1", "S2", "S3"]}, "characteristics": []},
              {"label": "S1", "objectType": "Sein", "attributes": {"vlaghoogte": {"number": "5"}}, "roles": {},
               "characteristics": []},
              {"label": "S2", "objectType": "Sein", "attributes": {"vlaghoogte": {"number": "1"}}, "roles": {},
               "characteristics": []},
              {"label": "S3", "objectType": "Sein", "attributes": {"vlaghoogte": {"number": "1"}}, "roles": {},
               "characteristics": ["gehesen"]}],
              "parameters": {}},
             "expected": [
              {"kind": "attributeValue", "label": "P1", "attribute": "seinaantal", "value": {"number": "3"}},
              {"kind": "attributeValue", "label": "V1", "attribute": "vakseinaantal", "value": {"number": "2"}},
              {"kind": "consistent"}]}
            """;

    /** {@link #gehesenByRule} fired: no document of the corpus or the suite has a fact creation whose guard reads
     *  the target's characteristic through its standing, so the harness does not watch the rendering of it. */
    @Test void the_fact_creation_fires_over_a_written_characteristic(@TempDir Path map) throws Exception {
        Path ir = map.resolve("ir.json");
        Files.writeString(ir, JSON.writeValueAsString(gehesenByRule()));
        assertNull(FoldPlanWitnessTest.playCase(ir, JSON.readTree(GEHESEN_CASE)));
    }

    /** {@code jutterswet-bakensein} with a vlaghoogte on Baken and a rule "Een Baken is gehesen indien zijn vlaghoogte
     *  groter is dan 2", so that a form writes the characteristic the fact creation's guard reads on its target. */
    static JsonNode gehesenByRule() throws Exception {
        ObjectNode d = (ObjectNode) golden("jutterswet-bakensein");
        String number = "{\"node\": \"Number\", \"whole\": true, \"decimals\": 0, \"sign\": \"nonNegative\"}";
        for (JsonNode ot : d.get("model").get("objectTypes"))
            if (ot.get("name").asText().equals("Baken"))
                ((ArrayNode) ot.get("attributes")).add(JSON.readTree("{\"node\": \"Attribute\", \"name\":"
                        + " \"vlaghoogte\", \"datatype\": " + number + "}"));
        ((ArrayNode) d.get("rules").get("rules")).add(JSON.readTree("{\"node\": \"Rule\", \"name\": \"gehesen van een"
                + " baken\", \"versions\": [{\"validity\": {\"node\": \"Validity\"}, \"target\": {\"node\":"
                + " \"AttributeReference\", \"attribute\": \"gehesen\", \"subjectType\": \"Baken\", \"source\":"
                + " {\"snippet\": \"Een Baken is gehesen\"}}, \"value\": {\"node\": \"Literal\", \"value\": {\"boolean\":"
                + " true}, \"type\": {\"node\": \"Boolean\"}, \"source\": {\"snippet\": \"waar\"}}, \"condition\":"
                + " {\"node\": \"Comparison\", \"op\": \"greater\", \"left\": {\"node\": \"AttributeReference\","
                + " \"attribute\": \"vlaghoogte\", \"subjectType\": \"Baken\", \"source\": {\"snippet\": \"zijn"
                + " vlaghoogte\"}}, \"right\": {\"node\": \"Literal\", \"value\": {\"number\": \"2\"}, \"type\": {\"node\":"
                + " \"Number\", \"whole\": true, \"decimals\": 0, \"sign\": \"unspecified\"}, \"source\": {\"snippet\":"
                + " \"2\"}}, \"source\": {\"snippet\": \"zijn vlaghoogte groter is dan 2\"}}, \"initialisation\":"
                + " false}]}"));
        return d;
    }

    static JsonNode golden(String naam) throws Exception {
        return JSON.readTree(GOLDEN.resolve(naam).resolve("ir.json").toFile());
    }

    // ---------------- the plans as text ----------------

    /** Every plan of {@code document} that {@code keuze} takes, in the order of the model, as text; after a
     *  line that holds a term, the term node by node. */
    static List<String> plans(JsonNode document, Predicate<RulePlan> keuze) {
        List<String> out = new ArrayList<>();
        for (RulePlan p : DroolsBackend.build(document, DroolsBackend.Branch.EXEC_MODEL).execModelTranspiler()
                .modelplan().rules()) {
            if (!keuze.test(p)) continue;
            out.add("regel " + p.naam() + " | vorm " + p.vorm() + " | groep " + p.agendaGroup());
            List<String> slots = new ArrayList<>();
            for (Read l : p.lezingen().slots())
                slots.add(l.slot() + " " + l.soort() + " " + l.naam() + " (" + l.sleutel() + ")");
            out.add("  lezingen " + slots);
            for (LhsItem item : p.leftSide()) {
                out.add("  " + item(item));
                if (item instanceof TestItem t) TermTree.fields("    toets", t.test(), "0", out);
            }
            consequence(p.consequence(), out);
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
                    + n.patterns().stream().map(CreationPlanWitnessTest::pattern).toList();
            case Exists b -> "bestaat binnen " + b.inside() + " buiten " + b.outside() + " "
                    + b.patterns().stream().map(CreationPlanWitnessTest::pattern).toList();
            case Accumulation a -> "accumulatie " + pattern(a.bron()) + " functies "
                    + a.aggregations().stream().map(f -> f.soort() + " " + f.bron() + " -> " + f.doel()).toList();
            case TestItem t -> "toets " + t.id() + " " + t.rol() + " sonde " + t.depositFlag() + " slots " + t.slots()
                    + " grootte " + t.size() + ": " + TermTree.tree(t.test());
        };
    }

    private static String pattern(FactPattern p) {
        return p.factClass().getSimpleName() + " " + p.naam() + " alfa " + alpha(p.alpha()) + " join "
                + joins(p.joins()) + " beta " + beta(p.betaTests()) + " na " + alpha(p.afterAlpha()) + " bind "
                + bindings(p.bindings());
    }

    /** Each constraint between two objects with its id, its function, the variable on the other side and each of its
     *  constants with every field. */
    private static List<String> beta(List<BetaTest> beta) {
        return beta.stream().map(t -> t.id() + ": " + t.function() + " " + t.doel()
                + t.constants().stream().map(c -> " " + constant(c)).reduce("", String::concat)).toList();
    }

    /** A constant of the analysis with every field: its value, the class of its value (a list by the classes of its
     *  elements), its IR type and its source fragment, each null where the constant has none. */
    private static String constant(Term.Const c) {
        return c.waarde() + " {" + javaClass(c.waarde()) + ", type " + c.type() + ", bron " + c.bron() + "}";
    }

    private static String javaClass(Object waarde) {
        if (waarde == null) return "null";
        if (waarde instanceof List<?> l)
            return "List<" + String.join(",", l.stream().map(e -> e == null ? "null" : e.getClass().getSimpleName())
                    .distinct().toList()) + ">";
        return waarde.getClass().getSimpleName();
    }

    private static List<String> joins(List<Join> joins) {
        return joins.stream().map(j -> j.id() + ": " + j.veld() + " = " + j.doel()
                + (j.targetField() == null ? "" : "." + veld(j.targetField()))).toList();
    }

    /** Each constraint on the object alone with its id and its field tests: the path, the field, the form of reading,
     *  the comparison, the value (a class by its name) and the class whose getter reads the field where the test
     *  names one. */
    private static List<String> alpha(List<Alpha> alpha) {
        List<String> out = new ArrayList<>();
        for (Alpha a : alpha)
            out.add(a.id() + ": " + a.tests().stream().map(t -> fields(t.pad()) + " " + t.veld() + " " + t.vorm()
                    + " " + t.op() + (t.waarde() == null ? "" : " " + (t.waarde() instanceof Class<?> k
                    ? k.getSimpleName() : t.waarde())) + (t.declarant() == null ? ""
                    : " via " + t.declarant().getSimpleName())).toList());
        return out;
    }

    private static List<String> bindings(List<Binding> bindings) {
        return bindings.stream().map(b -> b.doel() + " = " + fields(b.pad()) + " " + veld(b.veld()) + " " + b.vorm()
                + (b.soort() == null ? "" : " " + b.soort().getSimpleName())).toList();
    }

    private static String fields(List<Binding.Field> pad) {
        return pad.stream().map(CreationPlanWitnessTest::veld).toList().toString();
    }

    private static String veld(Binding.Field v) {
        return v.javaClass().getSimpleName() + "." + v.naam();
    }

    /** The consequence {@code g} with every field; of a creation its depth, its construction, each initialisation with
     *  its term as a tree and node by node, and each insertion. */
    private static void consequence(Consequence g, List<String> out) {
        switch (g) {
            case Consequence.Creation c -> {
                out.add("  gevolg Creatie [onderwerp=" + c.onderwerp() + ", creatie=" + c.creatie() + ", diepte="
                        + c.depth() + ", fout=" + c.fout() + ", foutinvoeging=" + c.faultInsertion() + ", phase="
                        + c.phase() + ", slots=" + c.slots() + ", grootte=" + c.size() + "]");
                out.add("    constructie " + c.construction().javaClass().getSimpleName() + " zettingen "
                        + c.construction().settings().stream().map(z -> z.initialisatie() + " " + veld(z.veld()))
                        .toList());
                for (int i = 0; i < c.initialisations().size(); i++) {
                    Consequence.Initialisation init = c.initialisations().get(i);
                    out.add("    initialisatie " + i + " " + init.member() + ": " + TermTree.tree(init.waarde()));
                    TermTree.fields("      waarde", init.waarde(), "0", out);
                }
                out.add("    invoegingen " + c.insertions());
            }
            case Consequence.RelationLink k -> out.add("  gevolg " + k);
            case Consequence.SecondFilling t -> out.add("  gevolg " + t);
            case Consequence.InsertPathState s -> out.add("  gevolg " + s);
            default -> throw new IllegalStateException("geen gevolg van een creatie of een koppeling: " + g);
        }
    }
}
