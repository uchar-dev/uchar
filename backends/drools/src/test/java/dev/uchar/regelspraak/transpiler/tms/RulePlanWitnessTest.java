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
 * {@link Consequence} whose every decision is a field, and {@link PatternDslRendering#regel} builds the rule of it. This
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
                "  lezingen [0 INVOER $aantalAangemeldeVondsten (eigen ElementSleutel[objecttype=jutter,"
                        + " naam=aantal aangemelde vondsten, kenmerk=false]), 1 PARAMETER $vondstdrempel (parameter"
                        + " vondstdrempel)]",
                "  onderwerp Jutter $onderwerp alfa [] bind [$aantalAangemeldeVondsten = [].aantal aangemelde"
                        + " vondsten VALUE]",
                "  patroon ParameterState $parameter_1 alfa [parameter_0: [parameter GELIJK vondstdrempel]] join []"
                        + " bind [$vondstdrempel = [].waarde VALUE]",
                "  toets voorwaarde_1 CONDITION sonde -1 slots [0, 1] grootte 2: RS_AGG_COMPARE(Lees 0, Lees 1,"
                        + " Const >=)",
                "    toets 0 Call bron 'zijn aantal aangemelde vondsten groter of gelijk is aan de vondstdrempel'",
                "    toets 0.0 SlotValue bron 'zijn aantal aangemelde vondsten'",
                "    toets 0.1 SlotValue bron 'de vondstdrempel'",
                "    toets 0.2 Const String",
                "  gevolg Characteristic [onderwerp=$onderwerp, kenmerk=ElementSleutel[objecttype=jutter,"
                        + " naam=penninggerechtigd, kenmerk=true], insertion=LOGICAL]",
                "regel parameterstand vondstdrempel | vorm null | groep null",
                "  lezingen []",
                "  gevolg InsertParameterState [parameter=vondstdrempel, global=vondstdrempel, insertion=LOGICAL]"),
                rules(golden("jutterswet-penningdrempel")));
    }

    @Test void a_computing_rule_with_a_deposit_flag_and_the_rules_of_its_condition() throws Exception {
        assertEquals(List.of(
                "regel bergingsaandeel van een jutter | vorm bergingsaandeel van een jutter | groep null",
                "  lezingen [0 INVOER $ploegomvang (eigen ElementSleutel[objecttype=jutter, naam=ploegomvang,"
                        + " kenmerk=false]), 1 INVOER $vangstwaarde (eigen ElementSleutel[objecttype=jutter,"
                        + " naam=vangstwaarde, kenmerk=false]), 2 DEPOSIT_FLAG null (null)]",
                "  onderwerp Jutter $onderwerp alfa [] bind [$ploegomvang = [].ploegomvang VALUE, $vangstwaarde ="
                        + " [].vangstwaarde VALUE]",
                "  toets voorwaarde_0 WITHOUT_FAULT sonde -1 slots [0, 1] grootte 3: Samengesteld ALLE"
                        + " 0(RS_AGG_COMPARE(Lees 0, Const 0, Const !=), RS_AGG_COMPARE(RS_ROUNDING(Deel"
                        + " DIVIDED sonde -1 (Lees 1, Lees 0), Const 0, Const FLOOR), Const 0, Const >))",
                "    toets 0 Compound bron 'hij aan alle volgende voorwaarden voldoet: - zijn ploegomvang is"
                        + " ongelijk aan 0 - het aandeel is groter dan 0.'",
                "    toets 0.0 Call bron 'zijn ploegomvang is ongelijk aan 0'",
                "    toets 0.0.0 SlotValue bron 'zijn ploegomvang'",
                "    toets 0.0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '0'",
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
                "    toets 0.1.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '0'",
                "    toets 0.1.2 Const String",
                "  gevolg Value [onderwerp=$onderwerp, doel=ElementSleutel[objecttype=jutter, naam=bergingsaandeel,"
                        + " kenmerk=false], writer=bergingsaandeel van een jutter, waarde=Const 1, slots=[],"
                        + " size=3, collapse=true, writesEmpty=false, deposit=Deposit[depositFlag=2,"
                        + " vorm=bergingsaandeel van een jutter], fout=FaultName[vorm=bergingsaandeel van een jutter,"
                        + " regel=bergingsaandeel van een jutter], insertion=LOGICAL]",
                "    waarde 0 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD, eenheid=null]"
                        + " bron '1'",
                "regel bergingsaandeel van een jutter: fout in de voorwaarde | vorm bergingsaandeel van een jutter |"
                        + " groep null",
                "  lezingen [0 INVOER $ploegomvang (eigen ElementSleutel[objecttype=jutter, naam=ploegomvang,"
                        + " kenmerk=false]), 1 INVOER $vangstwaarde (eigen ElementSleutel[objecttype=jutter,"
                        + " naam=vangstwaarde, kenmerk=false]), 2 DEPOSIT_FLAG null (null)]",
                "  onderwerp Jutter $onderwerp alfa [] bind [$ploegomvang = [].ploegomvang VALUE, $vangstwaarde ="
                        + " [].vangstwaarde VALUE]",
                "  toets voorwaarde_1 FAULT sonde -1 slots [0, 1] grootte 3: Samengesteld ALLE"
                        + " 0(RS_AGG_COMPARE(Lees 0, Const 0, Const !=), RS_AGG_COMPARE(RS_ROUNDING(Deel"
                        + " DIVIDED sonde -1 (Lees 1, Lees 0), Const 0, Const FLOOR), Const 0, Const >))",
                "    toets 0 Compound bron 'hij aan alle volgende voorwaarden voldoet: - zijn ploegomvang is"
                        + " ongelijk aan 0 - het aandeel is groter dan 0.'",
                "    toets 0.0 Call bron 'zijn ploegomvang is ongelijk aan 0'",
                "    toets 0.0.0 SlotValue bron 'zijn ploegomvang'",
                "    toets 0.0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '0'",
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
                "    toets 0.1.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '0'",
                "    toets 0.1.2 Const String",
                "  gevolg TestFault [onderwerp=$onderwerp, test=Samengesteld ALLE 0(RS_AGG_COMPARE(Lees 0, Const"
                        + " 0, Const !=), RS_AGG_COMPARE(RS_ROUNDING(Deel DIVIDED sonde -1 (Lees 1, Lees 0),"
                        + " Const 0, Const FLOOR), Const 0, Const >)), rol=FAULT, slots=[0, 1], size=3,"
                        + " fout=FaultName[vorm=bergingsaandeel van een jutter, regel=bergingsaandeel van een"
                        + " jutter], insertion=LOGICAL]",
                "    test 0 Compound bron 'hij aan alle volgende voorwaarden voldoet: - zijn ploegomvang is"
                        + " ongelijk aan 0 - het aandeel is groter dan 0.'",
                "    test 0.0 Call bron 'zijn ploegomvang is ongelijk aan 0'",
                "    test 0.0.0 SlotValue bron 'zijn ploegomvang'",
                "    test 0.0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '0'",
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
                "    test 0.1.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '0'",
                "    test 0.1.2 Const String",
                "regel bergingsaandeel van een jutter: deponering in de voorwaarde | vorm bergingsaandeel van een"
                        + " jutter | groep null",
                "  lezingen [0 INVOER $ploegomvang (eigen ElementSleutel[objecttype=jutter, naam=ploegomvang,"
                        + " kenmerk=false]), 1 INVOER $vangstwaarde (eigen ElementSleutel[objecttype=jutter,"
                        + " naam=vangstwaarde, kenmerk=false]), 2 DEPOSIT_FLAG null (null)]",
                "  onderwerp Jutter $onderwerp alfa [] bind [$ploegomvang = [].ploegomvang VALUE, $vangstwaarde ="
                        + " [].vangstwaarde VALUE]",
                "  toets voorwaarde_2 DEPOSIT sonde 2 slots [0, 1] grootte 3: Samengesteld ALLE"
                        + " 0(RS_AGG_COMPARE(Lees 0, Const 0, Const !=), RS_AGG_COMPARE(RS_ROUNDING(Deel DIVIDED"
                        + " sonde 2 (Lees 1, Lees 0), Const 0, Const FLOOR), Const 0, Const >))",
                "    toets 0 Compound bron 'hij aan alle volgende voorwaarden voldoet: - zijn ploegomvang is"
                        + " ongelijk aan 0 - het aandeel is groter dan 0.'",
                "    toets 0.0 Call bron 'zijn ploegomvang is ongelijk aan 0'",
                "    toets 0.0.0 SlotValue bron 'zijn ploegomvang'",
                "    toets 0.0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '0'",
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
                "    toets 0.1.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '0'",
                "    toets 0.1.2 Const String",
                "  gevolg Inconsistency [onderwerp=$onderwerp, vorm=bergingsaandeel van een jutter,"
                        + " insertion=LOGICAL]"),
                rules(golden("jutterswet-ploegaandeel")));
    }

    @Test void a_clamped_validity_and_the_evaluation_date_state() throws Exception {
        assertEquals(List.of(
                "regel vensterheffing in 2026 | vorm vensterheffing in 2026 | groep null",
                "  lezingen [0 INVOER $meldwaarde (eigen ElementSleutel[objecttype=vondstmelding, naam=meldwaarde,"
                        + " kenmerk=false])]",
                "  onderwerp Vondstmelding $onderwerp alfa [] bind [$meldwaarde = [].meldwaarde VALUE]",
                "  patroon EvaluationDate $geldigheid_0 alfa [geldig_0: [date WITHIN Geldigheid[vanaf=2026-01-01,"
                        + " tot=2026-12-31]]] join [] bind []",
                "  gevolg Value [onderwerp=$onderwerp, doel=ElementSleutel[objecttype=vondstmelding,"
                        + " naam=vensterheffing, kenmerk=false], writer=vensterheffing in 2026,"
                        + " waarde=RS_PLUS(Lees 0, Const 25), slots=[0], size=1, collapse=true, writesEmpty=true,"
                        + " deposit=null, fout=FaultName[vorm=vensterheffing in 2026, regel=vensterheffing in"
                        + " 2026], insertion=LOGICAL]",
                "    waarde 0 Call bron 'de meldwaarde van de Vondstmelding vermeerderd met 25'",
                "    waarde 0.0 SlotValue bron 'de meldwaarde van de Vondstmelding'",
                "    waarde 0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '25'",
                "regel vensterheffing in 2026: geen rekendatum | vorm vensterheffing in 2026 | groep null",
                "  lezingen []",
                "  patroon EvaluationDate $rekendatum alfa [geen-rekendatum_1: [date LEEG]] join [] bind []",
                "  gevolg Fout [onderwerp=$rekendatum, sentence=a validity-bounded form needs an evaluation date; this"
                        + " session is time-free, fout=FaultName[vorm=vensterheffing in 2026, regel=vensterheffing in"
                        + " 2026], insertion=LOGICAL]",
                "regel overgangsheffing tot en met 2025 | vorm overgangsheffing tot en met 2025 | groep null",
                "  lezingen [0 INVOER $meldwaarde (eigen ElementSleutel[objecttype=vondstmelding, naam=meldwaarde,"
                        + " kenmerk=false])]",
                "  onderwerp Vondstmelding $onderwerp alfa [] bind [$meldwaarde = [].meldwaarde VALUE]",
                "  patroon EvaluationDate $geldigheid_0 alfa [geldig_2: [date WITHIN Geldigheid[vanaf=null,"
                        + " tot=2025-12-31]]] join [] bind []",
                "  gevolg Value [onderwerp=$onderwerp, doel=ElementSleutel[objecttype=vondstmelding,"
                        + " naam=overgangsheffing, kenmerk=false], writer=overgangsheffing tot en met 2025,"
                        + " waarde=RS_PLUS(Lees 0, Const 40), slots=[0], size=1, collapse=true, writesEmpty=true,"
                        + " deposit=null, fout=FaultName[vorm=overgangsheffing tot en met 2025,"
                        + " regel=overgangsheffing tot en met 2025], insertion=LOGICAL]",
                "    waarde 0 Call bron 'de meldwaarde van de Vondstmelding vermeerderd met 40'",
                "    waarde 0.0 SlotValue bron 'de meldwaarde van de Vondstmelding'",
                "    waarde 0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '40'",
                "regel overgangsheffing tot en met 2025: geen rekendatum | vorm overgangsheffing tot en met 2025 |"
                        + " groep null",
                "  lezingen []",
                "  patroon EvaluationDate $rekendatum alfa [geen-rekendatum_3: [date LEEG]] join [] bind []",
                "  gevolg Fout [onderwerp=$rekendatum, sentence=a validity-bounded form needs an evaluation date; this"
                        + " session is time-free, fout=FaultName[vorm=overgangsheffing tot en met 2025,"
                        + " regel=overgangsheffing tot en met 2025], insertion=LOGICAL]",
                "regel rekendatumstand | vorm null | groep null",
                "  lezingen []",
                "  gevolg EvaluationDateState [global=rekendatum, insertion=LOGICAL]"),
                rules(golden("jutterswet-vensterheffing")));
    }

    @Test void two_writers_of_a_target() throws Exception {
        assertEquals(List.of(
                "regel vrijgave zonder beletsel | vorm vrijgave zonder beletsel | groep null",
                "  lezingen [0 INVOER $geclaimd (eigen ElementSleutel[objecttype=bergingszaak, naam=geclaimd,"
                        + " kenmerk=true]), 1 INVOER $bezwaren (eigen ElementSleutel[objecttype=bergingszaak,"
                        + " naam=bezwaren, kenmerk=false])]",
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
                "    toets 0.1.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '0'",
                "    toets 0.1.2 Const String",
                "  gevolg Value [onderwerp=$onderwerp, doel=ElementSleutel[objecttype=bergingszaak, naam=status,"
                        + " kenmerk=false], writer=vrijgave zonder beletsel, waarde=Const vrijgegeven, slots=[],"
                        + " size=2, collapse=true, writesEmpty=true, deposit=null, fout=FaultName[vorm=vrijgave"
                        + " zonder beletsel, regel=vrijgave zonder beletsel], insertion=LOGICAL]",
                "    waarde 0 Const String type Tekst[] bron ''vrijgegeven''",
                "regel aanhouding bij beletsel | vorm aanhouding bij beletsel | groep null",
                "  lezingen [0 INVOER $geclaimd (eigen ElementSleutel[objecttype=bergingszaak, naam=geclaimd,"
                        + " kenmerk=true]), 1 INVOER $bezwaren (eigen ElementSleutel[objecttype=bergingszaak,"
                        + " naam=bezwaren, kenmerk=false])]",
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
                "    toets 0.1.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '0'",
                "    toets 0.1.2 Const String",
                "  gevolg Value [onderwerp=$onderwerp, doel=ElementSleutel[objecttype=bergingszaak, naam=status,"
                        + " kenmerk=false], writer=aanhouding bij beletsel, waarde=Const aangehouden, slots=[],"
                        + " size=2, collapse=true, writesEmpty=true, deposit=null,"
                        + " fout=FaultName[vorm=aanhouding bij beletsel, regel=aanhouding bij beletsel],"
                        + " insertion=LOGICAL]",
                "    waarde 0 Const String type Tekst[] bron ''aangehouden''",
                "regel schrijvers Bergingszaak.status: verschil | vorm null | groep null",
                "  lezingen [0 VALUES $waarden (waarden)]",
                "  onderwerp Bergingszaak $onderwerp alfa [] bind []",
                "  accumulatie Value $waarde alfa [waarde_2: [attribuut GELIJK"
                        + " ElementSleutel[objecttype=bergingszaak, naam=status, kenmerk=false]]] join"
                        + " [waarde-onderwerp_3: onderwerp = $onderwerp] bind [] functies [LIST null -> $waarden]",
                "  toets verschil_4 VORM sonde -1 slots [0] grootte 1: DIFFER(Lees 0)",
                "    toets 0 Call",
                "    toets 0.0 SlotValue",
                "  gevolg Schrijversconflict [onderwerp=$onderwerp, waarden=0,"
                        + " doel=ElementSleutel[objecttype=bergingszaak, naam=status, kenmerk=false],"
                        + " attribuut=status, documentOrder=[vrijgave zonder beletsel, aanhouding bij beletsel],"
                        + " typeClasses=[Bergingszaak], typen=[Bergingszaak], insertion=LOGICAL]"),
                rules(golden("jutterswet-vrijgavetoets")));
    }

    @Test void a_subject_outside_a_stack() throws Exception {
        assertEquals(List.of(
                "regel uitkering van een strandrecht | vorm uitkering van een strandrecht | groep null",
                "  lezingen [0 INVOER $grondslag (eigen ElementSleutel[objecttype=strandrecht, naam=grondslag,"
                        + " kenmerk=false])]",
                "  onderwerp Strandrecht $onderwerp alfa [niet-in-een-stapel_0: [null CLASS_NOT_IN [Wrakrecht]]]"
                        + " bind [$grondslag = [].grondslag VALUE]",
                "  gevolg Value [onderwerp=$onderwerp, doel=ElementSleutel[objecttype=strandrecht, naam=uitkering,"
                        + " kenmerk=false], writer=uitkering van een strandrecht, waarde=RS_TIMES(Lees 0, Const 2),"
                        + " slots=[0], size=1, collapse=true, writesEmpty=false, deposit=null,"
                        + " fout=FaultName[vorm=uitkering van een strandrecht, regel=uitkering van een strandrecht],"
                        + " insertion=LOGICAL]",
                "    waarde 0 Call bron 'zijn grondslag maal 2'",
                "    waarde 0.0 SlotValue bron 'zijn grondslag'",
                "    waarde 0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '2'"),
                rules(golden("jutterswet-wrakerfbasis"), p -> !RulePlanWitnessTest.ofStack(p)));
    }

    @Test void an_initialisation_without_other_writer() throws Exception {
        assertEquals(List.of(
                "regel standaardbergloon | vorm standaardbergloon | groep null",
                "  lezingen [0 INVOER $gewicht (eigen ElementSleutel[objecttype=vondst, naam=gewicht,"
                        + " kenmerk=false])]",
                "  onderwerp Vondst $onderwerp alfa [doel-leeg-in-de-invoer_1: [bergloon LEEG]] bind [$gewicht ="
                        + " [].gewicht VALUE]",
                "  toets voorwaarde_0 CONDITION sonde -1 slots [0] grootte 1: RS_AGG_COMPARE(Lees 0, Const 10,"
                        + " Const >)",
                "    toets 0 Call bron 'zijn gewicht groter is dan 10'",
                "    toets 0.0 SlotValue bron 'zijn gewicht'",
                "    toets 0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '10'",
                "    toets 0.2 Const String",
                "  niet Value $andere_schrijver alfa [andere-schrijver_2: [attribuut GELIJK"
                        + " ElementSleutel[objecttype=vondst, naam=bergloon, kenmerk=false], writer NOT_EQUAL"
                        + " standaardbergloon]] join [andere-schrijver-onderwerp_3: onderwerp = $onderwerp] bind []",
                "  gevolg Value [onderwerp=$onderwerp, doel=ElementSleutel[objecttype=vondst, naam=bergloon,"
                        + " kenmerk=false], writer=standaardbergloon, waarde=RS_TIMES(Const 2, Lees 0), slots=[0],"
                        + " size=1, collapse=true, writesEmpty=false, deposit=null,"
                        + " fout=FaultName[vorm=standaardbergloon, regel=standaardbergloon], insertion=LOGICAL]",
                "    waarde 0 Call bron '2 maal zijn gewicht'",
                "    waarde 0.0 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '2'",
                "    waarde 0.1 SlotValue bron 'zijn gewicht'"),
                rules(golden("jutterswet-voorwaardelijke-initialisatie")));
    }

    @Test void an_initialisation_beside_a_writer_that_writes_empty() throws Exception {
        assertEquals(List.of(
                "regel standaardbergloon | vorm standaardbergloon | groep null",
                "  lezingen [0 INVOER $gewicht (eigen ElementSleutel[objecttype=vondst, naam=gewicht,"
                        + " kenmerk=false]), 1 EMPTY_WRITERS $lege_schrijvers (lege schrijvers), 2 SUBJECT"
                        + " $onderwerp (onderwerp zelf)]",
                "  onderwerp Vondst $onderwerp alfa [] bind [$gewicht = [].gewicht VALUE]",
                "  toets voorwaarde_0 CONDITION sonde -1 slots [0] grootte 1: RS_AGG_COMPARE(Lees 0, Const 10,"
                        + " Const >)",
                "    toets 0 Call bron 'zijn gewicht groter is dan 10'",
                "    toets 0.0 SlotValue bron 'zijn gewicht'",
                "    toets 0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '10'",
                "    toets 0.2 Const String",
                "  niet Value $andere_schrijver alfa [andere-schrijver_1: [attribuut GELIJK"
                        + " ElementSleutel[objecttype=vondst, naam=bergloon, kenmerk=false], writer NOT_EQUAL"
                        + " standaardbergloon, waarde GEVULD]] join [andere-schrijver-onderwerp_2: onderwerp ="
                        + " $onderwerp] bind []",
                "  accumulatie Value $lege_schrijver alfa [lege-schrijver_3: [attribuut GELIJK"
                        + " ElementSleutel[objecttype=vondst, naam=bergloon, kenmerk=false], writer NOT_EQUAL"
                        + " standaardbergloon, waarde LEEG]] join [lege-schrijver-onderwerp_4: onderwerp ="
                        + " $onderwerp] bind [] functies [COUNT null -> $lege_schrijvers]",
                "  toets doel-leeg_5 VORM sonde -1 slots [1, 2] grootte 3: TARGET_EMPTY(Lees 1, Invoer Vondst.bergloon"
                        + " VALUE (Lees 2))",
                "    toets 0 Call",
                "    toets 0.0 SlotValue",
                "    toets 0.1 Invoer",
                "    toets 0.1.0 SlotValue",
                "  gevolg Value [onderwerp=$onderwerp, doel=ElementSleutel[objecttype=vondst, naam=bergloon,"
                        + " kenmerk=false], writer=standaardbergloon, waarde=RS_TIMES(Const 2, Lees 0), slots=[0],"
                        + " size=3, collapse=true, writesEmpty=false, deposit=null,"
                        + " fout=FaultName[vorm=standaardbergloon, regel=standaardbergloon], insertion=LOGICAL]",
                "    waarde 0 Call bron '2 maal zijn gewicht'",
                "    waarde 0.0 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '2'",
                "    waarde 0.1 SlotValue bron 'zijn gewicht'",
                "regel zwaar bergloon | vorm zwaar bergloon | groep null",
                "  lezingen [0 INVOER $gewicht (eigen ElementSleutel[objecttype=vondst, naam=gewicht,"
                        + " kenmerk=false])]",
                "  onderwerp Vondst $onderwerp alfa [] bind [$gewicht = [].gewicht VALUE]",
                "  toets voorwaarde_6 CONDITION sonde -1 slots [0] grootte 1: RS_AGG_COMPARE(Lees 0, Const 10,"
                        + " Const >)",
                "    toets 0 Call bron 'zijn gewicht groter is dan 10'",
                "    toets 0.0 SlotValue bron 'zijn gewicht'",
                "    toets 0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '10'",
                "    toets 0.2 Const String",
                "  gevolg Value [onderwerp=$onderwerp, doel=ElementSleutel[objecttype=vondst, naam=bergloon,"
                        + " kenmerk=false], writer=zwaar bergloon, waarde=RS_TIMES(Const 2, Lees 0), slots=[0],"
                        + " size=1, collapse=true, writesEmpty=true, deposit=null, fout=FaultName[vorm=zwaar"
                        + " bergloon, regel=zwaar bergloon], insertion=LOGICAL]",
                "    waarde 0 Call bron '2 maal zijn gewicht'",
                "    waarde 0.0 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '2'",
                "    waarde 0.1 SlotValue bron 'zijn gewicht'",
                "regel licht bergloon | vorm licht bergloon | groep null",
                "  lezingen [0 INVOER $gewicht (eigen ElementSleutel[objecttype=vondst, naam=gewicht,"
                        + " kenmerk=false])]",
                "  onderwerp Vondst $onderwerp alfa [] bind [$gewicht = [].gewicht VALUE]",
                "  toets voorwaarde_7 CONDITION sonde -1 slots [0] grootte 1: RS_AGG_COMPARE(Lees 0, Const 10,"
                        + " Const >)",
                "    toets 0 Call bron 'zijn gewicht groter is dan 10'",
                "    toets 0.0 SlotValue bron 'zijn gewicht'",
                "    toets 0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '10'",
                "    toets 0.2 Const String",
                "  gevolg Value [onderwerp=$onderwerp, doel=ElementSleutel[objecttype=vondst, naam=bergloon,"
                        + " kenmerk=false], writer=licht bergloon, waarde=RS_TIMES(Const 2, Lees 0), slots=[0],"
                        + " size=1, collapse=true, writesEmpty=true, deposit=null, fout=FaultName[vorm=licht"
                        + " bergloon, regel=licht bergloon], insertion=LOGICAL]",
                "    waarde 0 Call bron '2 maal zijn gewicht'",
                "    waarde 0.0 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '2'",
                "    waarde 0.1 SlotValue bron 'zijn gewicht'",
                "regel schrijvers Vondst.bergloon: verschil | vorm null | groep null",
                "  lezingen [0 VALUES $waarden (waarden)]",
                "  onderwerp Vondst $onderwerp alfa [] bind []",
                "  accumulatie Value $waarde alfa [waarde_8: [attribuut GELIJK ElementSleutel[objecttype=vondst,"
                        + " naam=bergloon, kenmerk=false]]] join [waarde-onderwerp_9: onderwerp = $onderwerp] bind []"
                        + " functies [LIST null -> $waarden]",
                "  toets verschil_10 VORM sonde -1 slots [0] grootte 1: DIFFER(Lees 0)",
                "    toets 0 Call",
                "    toets 0.0 SlotValue",
                "  gevolg Schrijversconflict [onderwerp=$onderwerp, waarden=0,"
                        + " doel=ElementSleutel[objecttype=vondst, naam=bergloon, kenmerk=false],"
                        + " attribuut=bergloon, documentOrder=[standaardbergloon, zwaar bergloon, licht"
                        + " bergloon], typeClasses=[Vondst], typen=[Vondst], insertion=LOGICAL]"),
                rules(initialisationBesideEmptyWriter()));
    }

    /** The golden {@code jutterswet-voorwaardelijke-initialisatie}, whose initialisation of the bergloon is the
     *  target's one writer, with two computing rules on the bergloon beside it, copies of the initialisation's version
     *  that compute ({@code calculation}): two computing rules on one target are both outside the standing domain
     *  (RUNTIME-SEMANTICS ch. 7.8), so each writes a computed leeg. Verifier-clean. */
    static JsonNode initialisationBesideEmptyWriter() throws Exception {
        ObjectNode d = (ObjectNode) golden("jutterswet-voorwaardelijke-initialisatie");
        ArrayNode rules = (ArrayNode) d.path("rules").path("rules");
        ObjectNode initialisatie = (ObjectNode) rules.get(0);
        for (String naam : List.of("zwaar bergloon", "licht bergloon")) {
            ObjectNode r = initialisatie.deepCopy();
            r.put("name", naam);
            ((ObjectNode) r.path("versions").get(0)).put("initialisation", false).put("presentation", "calculation");
            rules.add(r);
        }
        return d;
    }

    private static JsonNode golden(String naam) throws Exception {
        return JSON.readTree(GOLDEN.resolve(naam).resolve("ir.json").toFile());
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
            for (RecordComponent c : p.consequence().getClass().getRecordComponents())
                if (c.getAccessor().invoke(p.consequence()) instanceof Term t)
                    TermTree.fields("    " + c.getName(), t, "0", out);
        }
        return out;
    }

    /** Whether {@code p} is a plan of a rule of a defaults stack, whose form the manifest names after the stack. */
    static boolean ofStack(RulePlan p) {
        return p.vorm() != null && p.vorm().startsWith("defaults ");
    }

    private static String item(LhsItem item) {
        return switch (item) {
            case Subject o -> "onderwerp " + o.beanClass().getSimpleName() + " " + o.naam() + " alfa "
                    + alpha(o.alpha()) + " bind " + bindings(o.bindings());
            case FactPattern p -> "patroon " + pattern(p);
            case Not n -> "niet " + (n.patterns().size() == 1 ? pattern(n.patterns().get(0)) : "binnen "
                    + n.inside() + " buiten " + n.outside() + " "
                    + n.patterns().stream().map(RulePlanWitnessTest::pattern).toList());
            case Exists b -> "bestaat " + b.patterns().stream().map(RulePlanWitnessTest::pattern).toList();
            case Accumulation a -> "accumulatie " + pattern(a.bron()) + " functies "
                    + a.functions().stream().map(f -> f.soort() + " " + f.bron() + " -> " + f.doel()).toList();
            case TestItem t -> "toets " + t.id() + " " + t.rol() + " sonde " + t.depositFlag() + " slots " + t.slots()
                    + " grootte " + t.size() + ": " + TermTree.tree(t.test());
        };
    }

    private static String pattern(FactPattern p) {
        return p.factClass().getSimpleName() + " " + p.naam() + " alfa " + alpha(p.alpha()) + " join "
                + p.joins().stream().map((Join j) -> j.id() + ": " + j.veld() + " = " + j.doel()).toList() + " bind "
                + bindings(p.bindings());
    }

    private static List<String> alpha(List<Alpha> alpha) {
        List<String> out = new ArrayList<>();
        for (Alpha a : alpha)
            out.add(a.id() + ": " + a.tests().stream().map(t -> t.veld() + " " + t.op()
                    + (t.waarde() == null ? "" : " " + waarde(t.waarde()))).toList());
        return out;
    }

    private static List<String> bindings(List<Binding> bindings) {
        return bindings.stream().map(b -> b.doel() + " = " + b.pad().stream().map(Binding.Field::naam).toList() + "."
                + b.veld().naam() + " " + b.vorm()).toList();
    }

    /** The kind of {@code g} and every field of it, a term as a tree and a table in key order. */
    private static String consequence(Consequence g) {
        List<String> fields = new ArrayList<>();
        try {
            for (RecordComponent c : g.getClass().getRecordComponents())
                fields.add(c.getName() + "=" + waarde(c.getAccessor().invoke(g)));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return g.getClass().getSimpleName() + " " + fields;
    }

    private static String waarde(Object w) {
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
