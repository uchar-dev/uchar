package dev.uchar.regelspraak.transpiler.tms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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

import java.lang.reflect.RecordComponent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The ordinary rules of the exec-model branch come whole from a plan (card drl-teksttak-op-truth-maintenance, step
 * 1c-A1): {@code TmsTranspiler} plans each rule as a {@link RulePlan}, its name, its form, its agenda group, the slots
 * of its value array, its left-hand side with the constraints the form sets itself, and its consequence as a
 * {@link Consequence} whose every decision is a field, and {@link PatternDslRendering#rule} builds the rule of it. This
 * witness pins the plans of seven documents as text, per plan in order: the name, the form and the group; the slots,
 * each with its kind, its variable and its key; every item of the left-hand side in order with its kind, its
 * constraint ids and what each constraint tests, its joins and its bindings; and the kind of the consequence with every
 * field. A term, the condition of a test or a field of a consequence, is pinned twice: as a tree with the fields each
 * node decides ({@link TermTree#tree}), and on the lines after it node by node with what the tree leaves out, the
 * node's source fragment, which a rendering into DRL text puts above the text, and of a constant the class of its value
 * and its whole IR type ({@link TermTree#fields}). That the rendering of these plans builds the rules of before the
 * step is what the harness {@code FireTrace} shows, and the executable model of the corpus, which is equal as text.
 *
 * <ul>
 *   <li>{@code jutterswet-penningdrempel}: a characteristic attribution, which reads a parameter, and the standing of
 *       that parameter, read off its global.</li>
 *   <li>{@code jutterswet-ploegaandeel}: a computing rule whose value and condition divide, so its consequence has a
 *       probe; beside it the rule of the condition's fault, which evaluates the condition again, and the rule of the
 *       condition's deposit.</li>
 *   <li>{@code jutterswet-vensterheffing}: a rule with a bounded validity, beside which a rule faults a session without
 *       an evaluation date, and the standing of the date, read off its global.</li>
 *   <li>{@code jutterswet-vrijgavetoets}: two computing rules on one target and the rule of two writers that stand
 *       with different values.</li>
 *   <li>{@code jutterswet-wrakerfbasis}: a rule whose subject pattern leaves out the objects of a type a defaults
 *       stack writes the target of; the plans of the stack itself, which step 1c-C2 planned, are left out here and
 *       pinned by {@link DefaultsplanWitnessTest}.</li>
 *   <li>{@code jutterswet-voorwaardelijke-initialisatie}: an initialisation beside no other writer, guarded by the
 *       input on the target and the absence of another writer's value.</li>
 *   <li>The same document with two computing rules beside the initialisation
 *       ({@link #initialisationBesideEmptyWriter}), since no golden has one: outside the standing domain a computing
 *       rule writes leeg, so the guard counts the other writers that wrote leeg and, where none did, tests the input,
 *       which the test reads off the bean as a node of the plan with its member and its form of reading.</li>
 * </ul>
 *
 * <p><b>Red</b> before the step: the transpiler planned only the left-hand sides and built every rule itself, so there
 * was no plan of a whole rule, and this class did not compile.
 */
class RulePlanWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void a_characteristic_rule_and_a_parameter_state() throws Exception {
        assertEquals(List.of(
                "regel Kenmerktoekenning jutter penninggerechtigd | vorm Kenmerktoekenning jutter penninggerechtigd |"
                        + " groep null",
                "  lezingen [0 INPUT $aantalAangemeldeVondsten (eigen ElementKey[objectType=jutter,"
                        + " name=aantal aangemelde vondsten, characteristic=false]), 1 PARAMETER $vondstdrempel (parameter"
                        + " vondstdrempel)]",
                "  onderwerp Jutter $onderwerp alfa [] bind [$aantalAangemeldeVondsten = [].aantal aangemelde"
                        + " vondsten VALUE]",
                "  patroon ParameterState $parameter_1 alfa [parameter_0: [parameter GELIJK vondstdrempel]] join []"
                        + " bind [$vondstdrempel = [].value VALUE]",
                "  toets voorwaarde_1 CONDITION sonde -1 slots [0, 1] grootte 2: RS_AGG_COMPARE(Lees 0, Lees 1,"
                        + " Const >=)",
                "    toets 0 Call bron 'zijn aantal aangemelde vondsten groter of gelijk is aan de vondstdrempel'",
                "    toets 0.0 SlotValue bron 'zijn aantal aangemelde vondsten'",
                "    toets 0.1 SlotValue bron 'de vondstdrempel'",
                "    toets 0.2 Const String",
                "  gevolg Characteristic [subject=$onderwerp, characteristic=ElementKey[objectType=jutter,"
                        + " name=penninggerechtigd, characteristic=true], insertion=LOGICAL]",
                "regel parameterstand vondstdrempel | vorm null | groep null",
                "  lezingen []",
                "  gevolg InsertParameterState [parameter=vondstdrempel, global=vondstdrempel, insertion=LOGICAL]"),
                rules(golden("jutterswet-penningdrempel")));
    }

    @Test void a_computing_rule_with_a_deposit_flag_and_the_rules_of_its_condition() throws Exception {
        assertEquals(List.of(
                "regel bergingsaandeel van een jutter | vorm bergingsaandeel van een jutter | groep null",
                "  lezingen [0 INPUT $ploegomvang (eigen ElementKey[objectType=jutter, name=ploegomvang,"
                        + " characteristic=false]), 1 INPUT $vangstwaarde (eigen ElementKey[objectType=jutter,"
                        + " name=vangstwaarde, characteristic=false]), 2 DEPOSIT_FLAG null (null)]",
                "  onderwerp Jutter $onderwerp alfa [] bind [$ploegomvang = [].ploegomvang VALUE, $vangstwaarde ="
                        + " [].vangstwaarde VALUE]",
                "  toets voorwaarde_0 WITHOUT_FAULT sonde -1 slots [0, 1] grootte 3: Samengesteld ALLE"
                        + " 0(RS_AGG_COMPARE(Lees 0, Const 0, Const !=), RS_AGG_COMPARE(RS_ROUNDING(Deel"
                        + " DIVIDED sonde -1 (Lees 1, Lees 0), Const 0, Const FLOOR), Const 0, Const >))",
                "    toets 0 Compound bron 'hij aan alle volgende voorwaarden voldoet: - zijn ploegomvang is"
                        + " ongelijk aan 0 - het aandeel is groter dan 0.'",
                "    toets 0.0 Call bron 'zijn ploegomvang is ongelijk aan 0'",
                "    toets 0.0.0 SlotValue bron 'zijn ploegomvang'",
                "    toets 0.0.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0, sign=ONBEPAALD,"
                        + " unit=null] bron '0'",
                "    toets 0.0.2 Const String",
                "    toets 0.1 Call bron 'het aandeel is groter dan 0'",
                "    toets 0.1.0 Call bron 'de vangstwaarde van de Jutter gedeeld door de ploegomvang van de"
                        + " Jutter naar beneden afgerond op 0 decimalen'",
                "    toets 0.1.0.0 Division bron 'de vangstwaarde van de Jutter gedeeld door de ploegomvang van de"
                        + " Jutter'",
                "    toets 0.1.0.0.0 SlotValue bron 'de vangstwaarde van de Jutter'",
                "    toets 0.1.0.0.1 SlotValue bron 'de ploegomvang van de Jutter'",
                "    toets 0.1.0.1 Const Integer",
                "    toets 0.1.0.2 Const RoundingMode",
                "    toets 0.1.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0, sign=ONBEPAALD,"
                        + " unit=null] bron '0'",
                "    toets 0.1.2 Const String",
                "  gevolg Value [subject=$onderwerp, target=ElementKey[objectType=jutter, name=bergingsaandeel,"
                        + " characteristic=false], writer=bergingsaandeel van een jutter, value=Const 1, slots=[],"
                        + " size=3, collapse=true, writesEmpty=false, deposit=Deposit[depositFlag=2,"
                        + " form=bergingsaandeel van een jutter], fault=FaultName[form=bergingsaandeel van een jutter,"
                        + " rule=bergingsaandeel van een jutter], insertion=LOGICAL]",
                "    value 0 Const BigDecimal type Numeric[wholeNumber=true, decimals=0, sign=ONBEPAALD, unit=null]"
                        + " bron '1'",
                "regel bergingsaandeel van een jutter: fout in de voorwaarde | vorm bergingsaandeel van een jutter |"
                        + " groep null",
                "  lezingen [0 INPUT $ploegomvang (eigen ElementKey[objectType=jutter, name=ploegomvang,"
                        + " characteristic=false]), 1 INPUT $vangstwaarde (eigen ElementKey[objectType=jutter,"
                        + " name=vangstwaarde, characteristic=false]), 2 DEPOSIT_FLAG null (null)]",
                "  onderwerp Jutter $onderwerp alfa [] bind [$ploegomvang = [].ploegomvang VALUE, $vangstwaarde ="
                        + " [].vangstwaarde VALUE]",
                "  toets voorwaarde_1 FAULT sonde -1 slots [0, 1] grootte 3: Samengesteld ALLE"
                        + " 0(RS_AGG_COMPARE(Lees 0, Const 0, Const !=), RS_AGG_COMPARE(RS_ROUNDING(Deel"
                        + " DIVIDED sonde -1 (Lees 1, Lees 0), Const 0, Const FLOOR), Const 0, Const >))",
                "    toets 0 Compound bron 'hij aan alle volgende voorwaarden voldoet: - zijn ploegomvang is"
                        + " ongelijk aan 0 - het aandeel is groter dan 0.'",
                "    toets 0.0 Call bron 'zijn ploegomvang is ongelijk aan 0'",
                "    toets 0.0.0 SlotValue bron 'zijn ploegomvang'",
                "    toets 0.0.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0, sign=ONBEPAALD,"
                        + " unit=null] bron '0'",
                "    toets 0.0.2 Const String",
                "    toets 0.1 Call bron 'het aandeel is groter dan 0'",
                "    toets 0.1.0 Call bron 'de vangstwaarde van de Jutter gedeeld door de ploegomvang van de"
                        + " Jutter naar beneden afgerond op 0 decimalen'",
                "    toets 0.1.0.0 Division bron 'de vangstwaarde van de Jutter gedeeld door de ploegomvang van de"
                        + " Jutter'",
                "    toets 0.1.0.0.0 SlotValue bron 'de vangstwaarde van de Jutter'",
                "    toets 0.1.0.0.1 SlotValue bron 'de ploegomvang van de Jutter'",
                "    toets 0.1.0.1 Const Integer",
                "    toets 0.1.0.2 Const RoundingMode",
                "    toets 0.1.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0, sign=ONBEPAALD,"
                        + " unit=null] bron '0'",
                "    toets 0.1.2 Const String",
                "  gevolg TestFault [subject=$onderwerp, test=Samengesteld ALLE 0(RS_AGG_COMPARE(Lees 0, Const"
                        + " 0, Const !=), RS_AGG_COMPARE(RS_ROUNDING(Deel DIVIDED sonde -1 (Lees 1, Lees 0),"
                        + " Const 0, Const FLOOR), Const 0, Const >)), role=FAULT, slots=[0, 1], size=3,"
                        + " fault=FaultName[form=bergingsaandeel van een jutter, rule=bergingsaandeel van een"
                        + " jutter], insertion=LOGICAL]",
                "    test 0 Compound bron 'hij aan alle volgende voorwaarden voldoet: - zijn ploegomvang is"
                        + " ongelijk aan 0 - het aandeel is groter dan 0.'",
                "    test 0.0 Call bron 'zijn ploegomvang is ongelijk aan 0'",
                "    test 0.0.0 SlotValue bron 'zijn ploegomvang'",
                "    test 0.0.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0, sign=ONBEPAALD,"
                        + " unit=null] bron '0'",
                "    test 0.0.2 Const String",
                "    test 0.1 Call bron 'het aandeel is groter dan 0'",
                "    test 0.1.0 Call bron 'de vangstwaarde van de Jutter gedeeld door de ploegomvang van de"
                        + " Jutter naar beneden afgerond op 0 decimalen'",
                "    test 0.1.0.0 Division bron 'de vangstwaarde van de Jutter gedeeld door de ploegomvang van de"
                        + " Jutter'",
                "    test 0.1.0.0.0 SlotValue bron 'de vangstwaarde van de Jutter'",
                "    test 0.1.0.0.1 SlotValue bron 'de ploegomvang van de Jutter'",
                "    test 0.1.0.1 Const Integer",
                "    test 0.1.0.2 Const RoundingMode",
                "    test 0.1.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0, sign=ONBEPAALD,"
                        + " unit=null] bron '0'",
                "    test 0.1.2 Const String",
                "regel bergingsaandeel van een jutter: deponering in de voorwaarde | vorm bergingsaandeel van een"
                        + " jutter | groep null",
                "  lezingen [0 INPUT $ploegomvang (eigen ElementKey[objectType=jutter, name=ploegomvang,"
                        + " characteristic=false]), 1 INPUT $vangstwaarde (eigen ElementKey[objectType=jutter,"
                        + " name=vangstwaarde, characteristic=false]), 2 DEPOSIT_FLAG null (null)]",
                "  onderwerp Jutter $onderwerp alfa [] bind [$ploegomvang = [].ploegomvang VALUE, $vangstwaarde ="
                        + " [].vangstwaarde VALUE]",
                "  toets voorwaarde_2 DEPOSIT sonde 2 slots [0, 1] grootte 3: Samengesteld ALLE"
                        + " 0(RS_AGG_COMPARE(Lees 0, Const 0, Const !=), RS_AGG_COMPARE(RS_ROUNDING(Deel DIVIDED"
                        + " sonde 2 (Lees 1, Lees 0), Const 0, Const FLOOR), Const 0, Const >))",
                "    toets 0 Compound bron 'hij aan alle volgende voorwaarden voldoet: - zijn ploegomvang is"
                        + " ongelijk aan 0 - het aandeel is groter dan 0.'",
                "    toets 0.0 Call bron 'zijn ploegomvang is ongelijk aan 0'",
                "    toets 0.0.0 SlotValue bron 'zijn ploegomvang'",
                "    toets 0.0.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0, sign=ONBEPAALD,"
                        + " unit=null] bron '0'",
                "    toets 0.0.2 Const String",
                "    toets 0.1 Call bron 'het aandeel is groter dan 0'",
                "    toets 0.1.0 Call bron 'de vangstwaarde van de Jutter gedeeld door de ploegomvang van de"
                        + " Jutter naar beneden afgerond op 0 decimalen'",
                "    toets 0.1.0.0 Division bron 'de vangstwaarde van de Jutter gedeeld door de ploegomvang van de"
                        + " Jutter'",
                "    toets 0.1.0.0.0 SlotValue bron 'de vangstwaarde van de Jutter'",
                "    toets 0.1.0.0.1 SlotValue bron 'de ploegomvang van de Jutter'",
                "    toets 0.1.0.1 Const Integer",
                "    toets 0.1.0.2 Const RoundingMode",
                "    toets 0.1.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0, sign=ONBEPAALD,"
                        + " unit=null] bron '0'",
                "    toets 0.1.2 Const String",
                "  gevolg ConditionDeposit [subject=$onderwerp, form=bergingsaandeel van een jutter,"
                        + " condition=Samengesteld ALLE 0(RS_AGG_COMPARE(Lees 0, Const 0, Const !=),"
                        + " RS_AGG_COMPARE(RS_ROUNDING(Deel DIVIDED sonde 2 (Lees 1, Lees 0), Const 0, Const FLOOR),"
                        + " Const 0, Const >)), depositFlag=2, slots=[0, 1], size=3, insertion=LOGICAL]",
                "    condition 0 Compound bron 'hij aan alle volgende voorwaarden voldoet: - zijn ploegomvang is"
                        + " ongelijk aan 0 - het aandeel is groter dan 0.'",
                "    condition 0.0 Call bron 'zijn ploegomvang is ongelijk aan 0'",
                "    condition 0.0.0 SlotValue bron 'zijn ploegomvang'",
                "    condition 0.0.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0, sign=ONBEPAALD,"
                        + " unit=null] bron '0'",
                "    condition 0.0.2 Const String",
                "    condition 0.1 Call bron 'het aandeel is groter dan 0'",
                "    condition 0.1.0 Call bron 'de vangstwaarde van de Jutter gedeeld door de ploegomvang van de"
                        + " Jutter naar beneden afgerond op 0 decimalen'",
                "    condition 0.1.0.0 Division bron 'de vangstwaarde van de Jutter gedeeld door de ploegomvang van de"
                        + " Jutter'",
                "    condition 0.1.0.0.0 SlotValue bron 'de vangstwaarde van de Jutter'",
                "    condition 0.1.0.0.1 SlotValue bron 'de ploegomvang van de Jutter'",
                "    condition 0.1.0.1 Const Integer",
                "    condition 0.1.0.2 Const RoundingMode",
                "    condition 0.1.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0, sign=ONBEPAALD,"
                        + " unit=null] bron '0'",
                "    condition 0.1.2 Const String"),
                rules(golden("jutterswet-ploegaandeel")));
    }

    @Test void a_clamped_validity_and_the_evaluation_date_state() throws Exception {
        assertEquals(List.of(
                "regel vensterheffing in 2026 | vorm vensterheffing in 2026 | groep null",
                "  lezingen [0 INPUT $meldwaarde (eigen ElementKey[objectType=vondstmelding, name=meldwaarde,"
                        + " characteristic=false])]",
                "  onderwerp Vondstmelding $onderwerp alfa [] bind [$meldwaarde = [].meldwaarde VALUE]",
                "  patroon EvaluationDate $geldigheid_0 alfa [geldig_0: [date WITHIN Validity[from=2026-01-01,"
                        + " until=2026-12-31]]] join [] bind []",
                "  gevolg Value [subject=$onderwerp, target=ElementKey[objectType=vondstmelding,"
                        + " name=vensterheffing, characteristic=false], writer=vensterheffing in 2026,"
                        + " value=RS_PLUS(Lees 0, Const 25), slots=[0], size=1, collapse=true, writesEmpty=true,"
                        + " deposit=null, fault=FaultName[form=vensterheffing in 2026, rule=vensterheffing in"
                        + " 2026], insertion=LOGICAL]",
                "    value 0 Call bron 'de meldwaarde van de Vondstmelding vermeerderd met 25'",
                "    value 0.0 SlotValue bron 'de meldwaarde van de Vondstmelding'",
                "    value 0.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0, sign=ONBEPAALD,"
                        + " unit=null] bron '25'",
                "regel vensterheffing in 2026: geen rekendatum | vorm vensterheffing in 2026 | groep null",
                "  lezingen []",
                "  patroon EvaluationDate $rekendatum alfa [geen-rekendatum_1: [date LEEG]] join [] bind []",
                "  gevolg Fault [subject=$rekendatum, sentence=a validity-bounded form needs an evaluation date; this"
                        + " session is time-free, fault=FaultName[form=vensterheffing in 2026, rule=vensterheffing in"
                        + " 2026], insertion=LOGICAL]",
                "regel overgangsheffing tot en met 2025 | vorm overgangsheffing tot en met 2025 | groep null",
                "  lezingen [0 INPUT $meldwaarde (eigen ElementKey[objectType=vondstmelding, name=meldwaarde,"
                        + " characteristic=false])]",
                "  onderwerp Vondstmelding $onderwerp alfa [] bind [$meldwaarde = [].meldwaarde VALUE]",
                "  patroon EvaluationDate $geldigheid_0 alfa [geldig_2: [date WITHIN Validity[from=null,"
                        + " until=2025-12-31]]] join [] bind []",
                "  gevolg Value [subject=$onderwerp, target=ElementKey[objectType=vondstmelding,"
                        + " name=overgangsheffing, characteristic=false], writer=overgangsheffing tot en met 2025,"
                        + " value=RS_PLUS(Lees 0, Const 40), slots=[0], size=1, collapse=true, writesEmpty=true,"
                        + " deposit=null, fault=FaultName[form=overgangsheffing tot en met 2025,"
                        + " rule=overgangsheffing tot en met 2025], insertion=LOGICAL]",
                "    value 0 Call bron 'de meldwaarde van de Vondstmelding vermeerderd met 40'",
                "    value 0.0 SlotValue bron 'de meldwaarde van de Vondstmelding'",
                "    value 0.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0, sign=ONBEPAALD,"
                        + " unit=null] bron '40'",
                "regel overgangsheffing tot en met 2025: geen rekendatum | vorm overgangsheffing tot en met 2025 |"
                        + " groep null",
                "  lezingen []",
                "  patroon EvaluationDate $rekendatum alfa [geen-rekendatum_3: [date LEEG]] join [] bind []",
                "  gevolg Fault [subject=$rekendatum, sentence=a validity-bounded form needs an evaluation date; this"
                        + " session is time-free, fault=FaultName[form=overgangsheffing tot en met 2025,"
                        + " rule=overgangsheffing tot en met 2025], insertion=LOGICAL]",
                "regel rekendatumstand | vorm null | groep null",
                "  lezingen []",
                "  gevolg EvaluationDateState [global=rekendatum, insertion=LOGICAL]"),
                rules(golden("jutterswet-vensterheffing")));
    }

    @Test void two_writers_of_a_target() throws Exception {
        assertEquals(List.of(
                "regel vrijgave zonder beletsel | vorm vrijgave zonder beletsel | groep null",
                "  lezingen [0 INPUT $geclaimd (eigen ElementKey[objectType=bergingszaak, name=geclaimd,"
                        + " characteristic=true]), 1 INPUT $bezwaren (eigen ElementKey[objectType=bergingszaak,"
                        + " name=bezwaren, characteristic=false])]",
                "  onderwerp Bergingszaak $onderwerp alfa [] bind [$geclaimd = [].geclaimd WAAR, $bezwaren ="
                        + " [].bezwaren VALUE]",
                "  toets voorwaarde_0 CONDITION sonde -1 slots [0, 1] grootte 2: Samengesteld GEEN 0(KENMERK(Lees 0,"
                        + " Const true), RS_AGG_COMPARE(Lees 1, Const 0, Const >))",
                "    toets 0 Compound bron 'geen van: geclaimd, bezwaren boven 0'",
                "    toets 0.0 Call bron 'de Bergingszaak geclaimd is'",
                "    toets 0.0.0 SlotValue",
                "    toets 0.0.1 Const Boolean",
                "    toets 0.1 Call bron 'haar bezwaren groter zijn dan 0'",
                "    toets 0.1.0 SlotValue bron 'de bezwaren van de Bergingszaak'",
                "    toets 0.1.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0, sign=ONBEPAALD,"
                        + " unit=null] bron '0'",
                "    toets 0.1.2 Const String",
                "  gevolg Value [subject=$onderwerp, target=ElementKey[objectType=bergingszaak, name=status,"
                        + " characteristic=false], writer=vrijgave zonder beletsel, value=Const vrijgegeven, slots=[],"
                        + " size=2, collapse=true, writesEmpty=true, deposit=null, fault=FaultName[form=vrijgave"
                        + " zonder beletsel, rule=vrijgave zonder beletsel], insertion=LOGICAL]",
                "    value 0 Const String type Text[] bron ''vrijgegeven''",
                "regel aanhouding bij beletsel | vorm aanhouding bij beletsel | groep null",
                "  lezingen [0 INPUT $geclaimd (eigen ElementKey[objectType=bergingszaak, name=geclaimd,"
                        + " characteristic=true]), 1 INPUT $bezwaren (eigen ElementKey[objectType=bergingszaak,"
                        + " name=bezwaren, characteristic=false])]",
                "  onderwerp Bergingszaak $onderwerp alfa [] bind [$geclaimd = [].geclaimd WAAR, $bezwaren ="
                        + " [].bezwaren VALUE]",
                "  toets voorwaarde_1 CONDITION sonde -1 slots [0, 1] grootte 2: Samengesteld TEN_MINSTE"
                        + " 1(KENMERK(Lees 0, Const true), RS_AGG_COMPARE(Lees 1, Const 0, Const >))",
                "    toets 0 Compound bron 'ten minste één van: geclaimd, bezwaren boven 0'",
                "    toets 0.0 Call bron 'de Bergingszaak geclaimd is'",
                "    toets 0.0.0 SlotValue",
                "    toets 0.0.1 Const Boolean",
                "    toets 0.1 Call bron 'haar bezwaren groter zijn dan 0'",
                "    toets 0.1.0 SlotValue bron 'de bezwaren van de Bergingszaak'",
                "    toets 0.1.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0, sign=ONBEPAALD,"
                        + " unit=null] bron '0'",
                "    toets 0.1.2 Const String",
                "  gevolg Value [subject=$onderwerp, target=ElementKey[objectType=bergingszaak, name=status,"
                        + " characteristic=false], writer=aanhouding bij beletsel, value=Const aangehouden, slots=[],"
                        + " size=2, collapse=true, writesEmpty=true, deposit=null,"
                        + " fault=FaultName[form=aanhouding bij beletsel, rule=aanhouding bij beletsel],"
                        + " insertion=LOGICAL]",
                "    value 0 Const String type Text[] bron ''aangehouden''",
                "regel schrijvers Bergingszaak.status: verschil | vorm null | groep null",
                "  lezingen [0 VALUES $waarden (waarden)]",
                "  onderwerp Bergingszaak $onderwerp alfa [] bind []",
                "  accumulatie Value $waarde alfa [waarde_2: [attribute GELIJK"
                        + " ElementKey[objectType=bergingszaak, name=status, characteristic=false]]] join"
                        + " [waarde-onderwerp_3: subject = $onderwerp] bind [] functies [LIST null -> $waarden]",
                "  toets verschil_4 VORM sonde -1 slots [0] grootte 1: DIFFER(Lees 0)",
                "    toets 0 Call",
                "    toets 0.0 SlotValue",
                "  gevolg WritersConflict [subject=$onderwerp, values=0,"
                        + " target=ElementKey[objectType=bergingszaak, name=status, characteristic=false],"
                        + " attribute=status, documentOrder=[vrijgave zonder beletsel, aanhouding bij beletsel],"
                        + " typeClasses=[Bergingszaak], types=[Bergingszaak], insertion=LOGICAL]"),
                rules(golden("jutterswet-vrijgavetoets")));
    }

    @Test void a_subject_outside_a_stack() throws Exception {
        assertEquals(List.of(
                "regel uitkering van een strandrecht | vorm uitkering van een strandrecht | groep null",
                "  lezingen [0 INPUT $grondslag (eigen ElementKey[objectType=strandrecht, name=grondslag,"
                        + " characteristic=false])]",
                "  onderwerp Strandrecht $onderwerp alfa [niet-in-een-stapel_0: [null CLASS_NOT_IN [Wrakrecht]]]"
                        + " bind [$grondslag = [].grondslag VALUE]",
                "  gevolg Value [subject=$onderwerp, target=ElementKey[objectType=strandrecht, name=uitkering,"
                        + " characteristic=false], writer=uitkering van een strandrecht, value=RS_TIMES(Lees 0, Const 2),"
                        + " slots=[0], size=1, collapse=true, writesEmpty=false, deposit=null,"
                        + " fault=FaultName[form=uitkering van een strandrecht, rule=uitkering van een strandrecht],"
                        + " insertion=LOGICAL]",
                "    value 0 Call bron 'zijn grondslag maal 2'",
                "    value 0.0 SlotValue bron 'zijn grondslag'",
                "    value 0.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0, sign=ONBEPAALD,"
                        + " unit=null] bron '2'"),
                rules(golden("jutterswet-wrakerfbasis"), p -> !RulePlanWitnessTest.ofStack(p)));
    }

    @Test void an_initialisation_without_other_writer() throws Exception {
        assertEquals(List.of(
                "regel standaardbergloon | vorm standaardbergloon | groep null",
                "  lezingen [0 INPUT $gewicht (eigen ElementKey[objectType=vondst, name=gewicht,"
                        + " characteristic=false])]",
                "  onderwerp Vondst $onderwerp alfa [doel-leeg-in-de-invoer_1: [bergloon LEEG]] bind [$gewicht ="
                        + " [].gewicht VALUE]",
                "  toets voorwaarde_0 CONDITION sonde -1 slots [0] grootte 1: RS_AGG_COMPARE(Lees 0, Const 10,"
                        + " Const >)",
                "    toets 0 Call bron 'zijn gewicht groter is dan 10'",
                "    toets 0.0 SlotValue bron 'zijn gewicht'",
                "    toets 0.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0, sign=ONBEPAALD,"
                        + " unit=null] bron '10'",
                "    toets 0.2 Const String",
                "  niet Value $andere_schrijver alfa [andere-schrijver_2: [attribute GELIJK"
                        + " ElementKey[objectType=vondst, name=bergloon, characteristic=false], writer NOT_EQUAL"
                        + " standaardbergloon]] join [andere-schrijver-onderwerp_3: subject = $onderwerp] bind []",
                "  gevolg Value [subject=$onderwerp, target=ElementKey[objectType=vondst, name=bergloon,"
                        + " characteristic=false], writer=standaardbergloon, value=RS_TIMES(Const 2, Lees 0), slots=[0],"
                        + " size=1, collapse=true, writesEmpty=false, deposit=null,"
                        + " fault=FaultName[form=standaardbergloon, rule=standaardbergloon], insertion=LOGICAL]",
                "    value 0 Call bron '2 maal zijn gewicht'",
                "    value 0.0 Const BigDecimal type Numeric[wholeNumber=true, decimals=0, sign=ONBEPAALD,"
                        + " unit=null] bron '2'",
                "    value 0.1 SlotValue bron 'zijn gewicht'"),
                rules(golden("jutterswet-voorwaardelijke-initialisatie")));
    }

    @Test void an_initialisation_beside_a_writer_that_writes_empty() throws Exception {
        assertEquals(List.of(
                "regel standaardbergloon | vorm standaardbergloon | groep null",
                "  lezingen [0 INPUT $gewicht (eigen ElementKey[objectType=vondst, name=gewicht,"
                        + " characteristic=false]), 1 EMPTY_WRITERS $lege_schrijvers (lege schrijvers), 2 SUBJECT"
                        + " $onderwerp (onderwerp zelf)]",
                "  onderwerp Vondst $onderwerp alfa [] bind [$gewicht = [].gewicht VALUE]",
                "  toets voorwaarde_0 CONDITION sonde -1 slots [0] grootte 1: RS_AGG_COMPARE(Lees 0, Const 10,"
                        + " Const >)",
                "    toets 0 Call bron 'zijn gewicht groter is dan 10'",
                "    toets 0.0 SlotValue bron 'zijn gewicht'",
                "    toets 0.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0, sign=ONBEPAALD,"
                        + " unit=null] bron '10'",
                "    toets 0.2 Const String",
                "  niet Value $andere_schrijver alfa [andere-schrijver_1: [attribute GELIJK"
                        + " ElementKey[objectType=vondst, name=bergloon, characteristic=false], writer NOT_EQUAL"
                        + " standaardbergloon, value GEVULD]] join [andere-schrijver-onderwerp_2: subject ="
                        + " $onderwerp] bind []",
                "  accumulatie Value $lege_schrijver alfa [lege-schrijver_3: [attribute GELIJK"
                        + " ElementKey[objectType=vondst, name=bergloon, characteristic=false], writer NOT_EQUAL"
                        + " standaardbergloon, value LEEG]] join [lege-schrijver-onderwerp_4: subject ="
                        + " $onderwerp] bind [] functies [COUNT null -> $lege_schrijvers]",
                "  toets doel-leeg_5 VORM sonde -1 slots [1, 2] grootte 3: TARGET_EMPTY(Lees 1, Invoer Vondst.bergloon"
                        + " VALUE (Lees 2))",
                "    toets 0 Call",
                "    toets 0.0 SlotValue",
                "    toets 0.1 Input",
                "    toets 0.1.0 SlotValue",
                "  gevolg Value [subject=$onderwerp, target=ElementKey[objectType=vondst, name=bergloon,"
                        + " characteristic=false], writer=standaardbergloon, value=RS_TIMES(Const 2, Lees 0), slots=[0],"
                        + " size=3, collapse=true, writesEmpty=false, deposit=null,"
                        + " fault=FaultName[form=standaardbergloon, rule=standaardbergloon], insertion=LOGICAL]",
                "    value 0 Call bron '2 maal zijn gewicht'",
                "    value 0.0 Const BigDecimal type Numeric[wholeNumber=true, decimals=0, sign=ONBEPAALD,"
                        + " unit=null] bron '2'",
                "    value 0.1 SlotValue bron 'zijn gewicht'",
                "regel zwaar bergloon | vorm zwaar bergloon | groep null",
                "  lezingen [0 INPUT $gewicht (eigen ElementKey[objectType=vondst, name=gewicht,"
                        + " characteristic=false])]",
                "  onderwerp Vondst $onderwerp alfa [] bind [$gewicht = [].gewicht VALUE]",
                "  toets voorwaarde_6 CONDITION sonde -1 slots [0] grootte 1: RS_AGG_COMPARE(Lees 0, Const 10,"
                        + " Const >)",
                "    toets 0 Call bron 'zijn gewicht groter is dan 10'",
                "    toets 0.0 SlotValue bron 'zijn gewicht'",
                "    toets 0.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0, sign=ONBEPAALD,"
                        + " unit=null] bron '10'",
                "    toets 0.2 Const String",
                "  gevolg Value [subject=$onderwerp, target=ElementKey[objectType=vondst, name=bergloon,"
                        + " characteristic=false], writer=zwaar bergloon, value=RS_TIMES(Const 2, Lees 0), slots=[0],"
                        + " size=1, collapse=true, writesEmpty=true, deposit=null, fault=FaultName[form=zwaar"
                        + " bergloon, rule=zwaar bergloon], insertion=LOGICAL]",
                "    value 0 Call bron '2 maal zijn gewicht'",
                "    value 0.0 Const BigDecimal type Numeric[wholeNumber=true, decimals=0, sign=ONBEPAALD,"
                        + " unit=null] bron '2'",
                "    value 0.1 SlotValue bron 'zijn gewicht'",
                "regel licht bergloon | vorm licht bergloon | groep null",
                "  lezingen [0 INPUT $gewicht (eigen ElementKey[objectType=vondst, name=gewicht,"
                        + " characteristic=false])]",
                "  onderwerp Vondst $onderwerp alfa [] bind [$gewicht = [].gewicht VALUE]",
                "  toets voorwaarde_7 CONDITION sonde -1 slots [0] grootte 1: RS_AGG_COMPARE(Lees 0, Const 10,"
                        + " Const >)",
                "    toets 0 Call bron 'zijn gewicht groter is dan 10'",
                "    toets 0.0 SlotValue bron 'zijn gewicht'",
                "    toets 0.1 Const BigDecimal type Numeric[wholeNumber=true, decimals=0, sign=ONBEPAALD,"
                        + " unit=null] bron '10'",
                "    toets 0.2 Const String",
                "  gevolg Value [subject=$onderwerp, target=ElementKey[objectType=vondst, name=bergloon,"
                        + " characteristic=false], writer=licht bergloon, value=RS_TIMES(Const 2, Lees 0), slots=[0],"
                        + " size=1, collapse=true, writesEmpty=true, deposit=null, fault=FaultName[form=licht"
                        + " bergloon, rule=licht bergloon], insertion=LOGICAL]",
                "    value 0 Call bron '2 maal zijn gewicht'",
                "    value 0.0 Const BigDecimal type Numeric[wholeNumber=true, decimals=0, sign=ONBEPAALD,"
                        + " unit=null] bron '2'",
                "    value 0.1 SlotValue bron 'zijn gewicht'",
                "regel schrijvers Vondst.bergloon: verschil | vorm null | groep null",
                "  lezingen [0 VALUES $waarden (waarden)]",
                "  onderwerp Vondst $onderwerp alfa [] bind []",
                "  accumulatie Value $waarde alfa [waarde_8: [attribute GELIJK ElementKey[objectType=vondst,"
                        + " name=bergloon, characteristic=false]]] join [waarde-onderwerp_9: subject = $onderwerp] bind []"
                        + " functies [LIST null -> $waarden]",
                "  toets verschil_10 VORM sonde -1 slots [0] grootte 1: DIFFER(Lees 0)",
                "    toets 0 Call",
                "    toets 0.0 SlotValue",
                "  gevolg WritersConflict [subject=$onderwerp, values=0,"
                        + " target=ElementKey[objectType=vondst, name=bergloon, characteristic=false],"
                        + " attribute=bergloon, documentOrder=[standaardbergloon, zwaar bergloon, licht"
                        + " bergloon], typeClasses=[Vondst], types=[Vondst], insertion=LOGICAL]"),
                rules(initialisationBesideEmptyWriter()));
    }

    /** The golden {@code jutterswet-voorwaardelijke-initialisatie}, whose initialisation of the bergloon is the
     *  target's one writer, with two computing rules on the bergloon beside it, copies of the initialisation's version
     *  that compute ({@code calculation}): two computing rules on one target are both outside the standing domain
     *  (RUNTIME-SEMANTICS ch. 7.8), so each writes a computed leeg. Verifier-clean. */
    static JsonNode initialisationBesideEmptyWriter() throws Exception {
        ObjectNode d = (ObjectNode) golden("jutterswet-voorwaardelijke-initialisatie");
        ArrayNode rules = (ArrayNode) d.path("rules").path("rules");
        ObjectNode initialisation = (ObjectNode) rules.get(0);
        for (String name : List.of("zwaar bergloon", "licht bergloon")) {
            ObjectNode r = initialisation.deepCopy();
            r.put("name", name);
            ((ObjectNode) r.path("versions").get(0)).put("initialisation", false).put("presentation", "calculation");
            rules.add(r);
        }
        return d;
    }

    private static JsonNode golden(String name) throws Exception {
        return JSON.readTree(GOLDEN.resolve(name).resolve("ir.json").toFile());
    }

    /** Every plan of {@code document}'s rules as text, in the order of the model; after a line that holds a
     *  term, the term node by node ({@link TermTree#fields}). */
    private static List<String> rules(JsonNode document) throws ReflectiveOperationException {
        return rules(document, p -> true);
    }

    /** As {@link #rules(JsonNode)}, of the plans {@code which} takes. */
    static List<String> rules(JsonNode document, Predicate<RulePlan> which) throws ReflectiveOperationException {
        List<String> out = new ArrayList<>();
        for (RulePlan p : DroolsBackend.build(document, DroolsBackend.Branch.EXEC_MODEL).execModelTranspiler()
                .modelplan().rules()) {
            if (!which.test(p)) continue;
            out.add("regel " + p.name() + " | vorm " + p.form() + " | groep " + p.agendaGroup());
            List<String> slots = new ArrayList<>();
            for (Read l : p.readings().slots())
                slots.add(l.slot() + " " + l.category() + " " + l.name() + " (" + l.key() + ")");
            out.add("  lezingen " + slots);
            for (LhsItem item : p.leftSide()) {
                out.add("  " + item(item));
                if (item instanceof TestItem t) TermTree.fields("    toets", t.test(), "0", out);
            }
            out.add("  gevolg " + consequence(p.consequence()));
            for (RecordComponent c : p.consequence().getClass().getRecordComponents())
                if (c.getAccessor().invoke(p.consequence()) instanceof Term t)
                    TermTree.fields("    " + c.getName(), t, "0", out);
        }
        return out;
    }

    /** Whether {@code p} is a plan of a rule of a defaults stack, whose form the manifest names after the stack. */
    static boolean ofStack(RulePlan p) {
        return p.form() != null && p.form().startsWith("defaults ");
    }

    private static String item(LhsItem item) {
        return switch (item) {
            case Subject o -> "onderwerp " + o.beanClass().getSimpleName() + " " + o.name() + " alfa "
                    + alpha(o.alpha()) + " bind " + bindings(o.bindings());
            case FactPattern p -> "patroon " + pattern(p);
            case Not n -> "niet " + (n.patterns().size() == 1 ? pattern(n.patterns().get(0)) : "binnen "
                    + n.inside() + " buiten " + n.outside() + " "
                    + n.patterns().stream().map(RulePlanWitnessTest::pattern).toList());
            case Exists b -> "bestaat " + b.patterns().stream().map(RulePlanWitnessTest::pattern).toList();
            case Accumulation a -> "accumulatie " + pattern(a.source()) + " functies "
                    + a.aggregations().stream().map(f -> f.category() + " " + f.source() + " -> " + f.target()).toList();
            case TestItem t -> "toets " + t.id() + " " + t.role() + " sonde " + t.depositFlag() + " slots " + t.slots()
                    + " grootte " + t.size() + ": " + TermTree.tree(t.test());
        };
    }

    private static String pattern(FactPattern p) {
        return p.factClass().getSimpleName() + " " + p.name() + " alfa " + alpha(p.alpha()) + " join "
                + p.joins().stream().map((Join j) -> j.id() + ": " + j.field() + " = " + j.target()).toList() + " bind "
                + bindings(p.bindings());
    }

    private static List<String> alpha(List<Alpha> alpha) {
        List<String> out = new ArrayList<>();
        for (Alpha a : alpha)
            out.add(a.id() + ": " + a.tests().stream().map(t -> t.field() + " " + t.op()
                    + (t.value() == null ? "" : " " + value(t.value()))).toList());
        return out;
    }

    private static List<String> bindings(List<Binding> bindings) {
        return bindings.stream().map(b -> b.target() + " = " + b.path().stream().map(Binding.Field::name).toList() + "."
                + b.field().name() + " " + b.form()).toList();
    }

    /** The kind of {@code g} and every field of it, a term as a tree and a table in key order. */
    private static String consequence(Consequence g) {
        List<String> fields = new ArrayList<>();
        try {
            for (RecordComponent c : g.getClass().getRecordComponents())
                fields.add(c.getName() + "=" + value(c.getAccessor().invoke(g)));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return g.getClass().getSimpleName() + " " + fields;
    }

    private static String value(Object w) {
        if (w instanceof Term t) return TermTree.tree(t);
        if (w instanceof Class<?> k) return k.getSimpleName();
        if (w instanceof Map<?, ?> m) {
            Map<String, String> ordered = new TreeMap<>();
            m.forEach((k, v) -> ordered.put(String.valueOf(k), String.valueOf(v)));
            return ordered.toString();
        }
        if (w instanceof Collection<?> c && !c.isEmpty() && c.iterator().next() instanceof Class<?>) {
            TreeSet<String> names = new TreeSet<>();
            for (Object k : c) names.add(((Class<?>) k).getSimpleName());
            return names.toString();
        }
        return String.valueOf(w);
    }
}
