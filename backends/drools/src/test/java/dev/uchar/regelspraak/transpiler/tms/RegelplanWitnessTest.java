package dev.uchar.regelspraak.transpiler.tms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.transpiler.tms.plan.Accumulatie;
import dev.uchar.regelspraak.transpiler.tms.plan.Alfa;
import dev.uchar.regelspraak.transpiler.tms.plan.Bestaat;
import dev.uchar.regelspraak.transpiler.tms.plan.Binding;
import dev.uchar.regelspraak.transpiler.tms.plan.Gevolg;
import dev.uchar.regelspraak.transpiler.tms.plan.Join;
import dev.uchar.regelspraak.transpiler.tms.plan.Lezing;
import dev.uchar.regelspraak.transpiler.tms.plan.LhsItem;
import dev.uchar.regelspraak.transpiler.tms.plan.Niet;
import dev.uchar.regelspraak.transpiler.tms.plan.Onderwerp;
import dev.uchar.regelspraak.transpiler.tms.plan.Patroon;
import dev.uchar.regelspraak.transpiler.tms.plan.Regelplan;
import dev.uchar.regelspraak.transpiler.tms.plan.Term;
import dev.uchar.regelspraak.transpiler.tms.plan.Toetsitem;

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
 * 1c-A1): {@code TmsTranspiler} plans each rule as a {@link Regelplan}, its name, its form, its agenda group, the slots
 * of its value array, its left-hand side with the constraints the form sets itself, and its consequence as a
 * {@link Gevolg} whose every decision is a field, and {@link PatternDslWeergave#regel} builds the rule of it. This
 * witness pins the plans of seven documents as text, per plan in order: the name, the form and the group; the slots,
 * each with its kind, its variable and its key; every item of the left-hand side in order with its kind, its
 * constraint ids and what each constraint tests, its joins and its bindings; and the kind of the consequence with every
 * field. A term, the condition of a test or a field of a consequence, is pinned twice: as a tree with the fields each
 * node decides ({@link Termboom#boom}), and on the lines after it node by node with what the tree leaves out, the
 * node's source fragment, which a rendering into DRL text puts above the text, and of a constant the class of its value
 * and its whole IR type ({@link Termboom#velden}). That the rendering of these plans builds the rules of before the
 * step is what the harness {@code Vuurspoor} shows, and the executable model of the corpus, which is equal as text.
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
 *       ({@link #initialisatieNaastLegeSchrijver}), since no golden has one: outside the standing domain a computing
 *       rule writes leeg, so the guard counts the other writers that wrote leeg and, where none did, tests the input,
 *       which the test reads off the bean as a node of the plan with its member and its form of reading.</li>
 * </ul>
 *
 * <p><b>Red</b> before the step: the transpiler planned only the left-hand sides and built every rule itself, so there
 * was no plan of a whole rule, and this class did not compile.
 */
class RegelplanWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void een_kenmerkregel_en_een_parameterstand() throws Exception {
        assertEquals(List.of(
                "regel Kenmerktoekenning jutter penninggerechtigd | vorm Kenmerktoekenning jutter penninggerechtigd |"
                        + " groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=jutter, naam=aantal aangemelde"
                        + " vondsten, kenmerk=false]), 1 PARAMETER $lezing_1 (parameter vondstdrempel)]",
                "  onderwerp Jutter $onderwerp alfa [] bind [$lezing_0 = [].aantal aangemelde vondsten WAARDE]",
                "  patroon ParameterStand $parameter_1 alfa [parameter_0: [parameter GELIJK vondstdrempel]] join []"
                        + " bind [$lezing_1 = [].waarde WAARDE]",
                "  toets voorwaarde_1 VOORWAARDE sonde -1 slots [0, 1] grootte 2: RS_AGG_VERGELIJK(Lees 0, Lees 1,"
                        + " Const >=)",
                "    toets 0 Aanroep bron 'zijn aantal aangemelde vondsten groter of gelijk is aan de vondstdrempel'",
                "    toets 0.0 Lees bron 'zijn aantal aangemelde vondsten'",
                "    toets 0.1 Lees bron 'de vondstdrempel'",
                "    toets 0.2 Const String",
                "  gevolg Kenmerk [onderwerp=$onderwerp, kenmerk=ElementSleutel[objecttype=jutter,"
                        + " naam=penninggerechtigd, kenmerk=true], invoeging=LOGISCH]",
                "regel parameterstand vondstdrempel | vorm null | groep null",
                "  lezingen []",
                "  gevolg Parameterstand [parameter=vondstdrempel, global=vondstdrempel, invoeging=LOGISCH]"),
                regels(golden("jutterswet-penningdrempel")));
    }

    @Test void een_rekenregel_met_sonde_en_de_regels_van_haar_voorwaarde() throws Exception {
        assertEquals(List.of(
                "regel bergingsaandeel van een jutter | vorm bergingsaandeel van een jutter | groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=jutter, naam=ploegomvang,"
                        + " kenmerk=false]), 1 INVOER $lezing_1 (eigen ElementSleutel[objecttype=jutter,"
                        + " naam=vangstwaarde, kenmerk=false]), 2 SONDE null (null)]",
                "  onderwerp Jutter $onderwerp alfa [] bind [$lezing_0 = [].ploegomvang WAARDE, $lezing_1 ="
                        + " [].vangstwaarde WAARDE]",
                "  toets voorwaarde_0 ZONDER_FOUT sonde -1 slots [0, 1] grootte 3: Samengesteld ALLE"
                        + " 0(RS_AGG_VERGELIJK(Lees 0, Const 0, Const !=), RS_AGG_VERGELIJK(RS_AFRONDING(Deel GEDEELD"
                        + " sonde 2 (Lees 1, Lees 0), Const 0, Const FLOOR), Const 0, Const >))",
                "    toets 0 Samengesteld bron 'hij aan alle volgende voorwaarden voldoet: - zijn ploegomvang is"
                        + " ongelijk aan 0 - het aandeel is groter dan 0.'",
                "    toets 0.0 Aanroep bron 'zijn ploegomvang is ongelijk aan 0'",
                "    toets 0.0.0 Lees bron 'zijn ploegomvang'",
                "    toets 0.0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '0'",
                "    toets 0.0.2 Const String",
                "    toets 0.1 Aanroep bron 'het aandeel is groter dan 0'",
                "    toets 0.1.0 Aanroep bron 'de vangstwaarde van de Jutter gedeeld door de ploegomvang van de"
                        + " Jutter naar beneden afgerond op 0 decimalen'",
                "    toets 0.1.0.0 Deel bron 'de vangstwaarde van de Jutter gedeeld door de ploegomvang van de"
                        + " Jutter'",
                "    toets 0.1.0.0.0 Lees bron 'de vangstwaarde van de Jutter'",
                "    toets 0.1.0.0.1 Lees bron 'de ploegomvang van de Jutter'",
                "    toets 0.1.0.1 Const Integer",
                "    toets 0.1.0.2 Const RoundingMode",
                "    toets 0.1.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '0'",
                "    toets 0.1.2 Const String",
                "  gevolg Waarde [onderwerp=$onderwerp, doel=ElementSleutel[objecttype=jutter, naam=bergingsaandeel,"
                        + " kenmerk=false], schrijver=bergingsaandeel van een jutter, waarde=Const 1, slots=[],"
                        + " grootte=3, collapse=true, schrijftLeeg=false, deponering=Deponering[sonde=2,"
                        + " vorm=bergingsaandeel van een jutter], fout=Foutnaam[vorm=bergingsaandeel van een jutter,"
                        + " regel=bergingsaandeel van een jutter], invoeging=LOGISCH]",
                "    waarde 0 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD, eenheid=null]"
                        + " bron '1'",
                "regel bergingsaandeel van een jutter: fout in de voorwaarde | vorm bergingsaandeel van een jutter |"
                        + " groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=jutter, naam=ploegomvang,"
                        + " kenmerk=false]), 1 INVOER $lezing_1 (eigen ElementSleutel[objecttype=jutter,"
                        + " naam=vangstwaarde, kenmerk=false]), 2 SONDE null (null)]",
                "  onderwerp Jutter $onderwerp alfa [] bind [$lezing_0 = [].ploegomvang WAARDE, $lezing_1 ="
                        + " [].vangstwaarde WAARDE]",
                "  toets voorwaarde_1 FOUT sonde -1 slots [0, 1] grootte 3: Samengesteld ALLE 0(RS_AGG_VERGELIJK(Lees"
                        + " 0, Const 0, Const !=), RS_AGG_VERGELIJK(RS_AFRONDING(Deel GEDEELD sonde 2 (Lees 1, Lees"
                        + " 0), Const 0, Const FLOOR), Const 0, Const >))",
                "    toets 0 Samengesteld bron 'hij aan alle volgende voorwaarden voldoet: - zijn ploegomvang is"
                        + " ongelijk aan 0 - het aandeel is groter dan 0.'",
                "    toets 0.0 Aanroep bron 'zijn ploegomvang is ongelijk aan 0'",
                "    toets 0.0.0 Lees bron 'zijn ploegomvang'",
                "    toets 0.0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '0'",
                "    toets 0.0.2 Const String",
                "    toets 0.1 Aanroep bron 'het aandeel is groter dan 0'",
                "    toets 0.1.0 Aanroep bron 'de vangstwaarde van de Jutter gedeeld door de ploegomvang van de"
                        + " Jutter naar beneden afgerond op 0 decimalen'",
                "    toets 0.1.0.0 Deel bron 'de vangstwaarde van de Jutter gedeeld door de ploegomvang van de"
                        + " Jutter'",
                "    toets 0.1.0.0.0 Lees bron 'de vangstwaarde van de Jutter'",
                "    toets 0.1.0.0.1 Lees bron 'de ploegomvang van de Jutter'",
                "    toets 0.1.0.1 Const Integer",
                "    toets 0.1.0.2 Const RoundingMode",
                "    toets 0.1.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '0'",
                "    toets 0.1.2 Const String",
                "  gevolg Toetsfout [onderwerp=$onderwerp, toets=Samengesteld ALLE 0(RS_AGG_VERGELIJK(Lees 0, Const"
                        + " 0, Const !=), RS_AGG_VERGELIJK(RS_AFRONDING(Deel GEDEELD sonde 2 (Lees 1, Lees 0), Const"
                        + " 0, Const FLOOR), Const 0, Const >)), rol=FOUT, slots=[0, 1], grootte=3,"
                        + " fout=Foutnaam[vorm=bergingsaandeel van een jutter, regel=bergingsaandeel van een jutter],"
                        + " invoeging=LOGISCH]",
                "    toets 0 Samengesteld bron 'hij aan alle volgende voorwaarden voldoet: - zijn ploegomvang is"
                        + " ongelijk aan 0 - het aandeel is groter dan 0.'",
                "    toets 0.0 Aanroep bron 'zijn ploegomvang is ongelijk aan 0'",
                "    toets 0.0.0 Lees bron 'zijn ploegomvang'",
                "    toets 0.0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '0'",
                "    toets 0.0.2 Const String",
                "    toets 0.1 Aanroep bron 'het aandeel is groter dan 0'",
                "    toets 0.1.0 Aanroep bron 'de vangstwaarde van de Jutter gedeeld door de ploegomvang van de"
                        + " Jutter naar beneden afgerond op 0 decimalen'",
                "    toets 0.1.0.0 Deel bron 'de vangstwaarde van de Jutter gedeeld door de ploegomvang van de"
                        + " Jutter'",
                "    toets 0.1.0.0.0 Lees bron 'de vangstwaarde van de Jutter'",
                "    toets 0.1.0.0.1 Lees bron 'de ploegomvang van de Jutter'",
                "    toets 0.1.0.1 Const Integer",
                "    toets 0.1.0.2 Const RoundingMode",
                "    toets 0.1.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '0'",
                "    toets 0.1.2 Const String",
                "regel bergingsaandeel van een jutter: deponering in de voorwaarde | vorm bergingsaandeel van een"
                        + " jutter | groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=jutter, naam=ploegomvang,"
                        + " kenmerk=false]), 1 INVOER $lezing_1 (eigen ElementSleutel[objecttype=jutter,"
                        + " naam=vangstwaarde, kenmerk=false]), 2 SONDE null (null)]",
                "  onderwerp Jutter $onderwerp alfa [] bind [$lezing_0 = [].ploegomvang WAARDE, $lezing_1 ="
                        + " [].vangstwaarde WAARDE]",
                "  toets voorwaarde_2 DEPONERING sonde 2 slots [0, 1] grootte 3: Samengesteld ALLE"
                        + " 0(RS_AGG_VERGELIJK(Lees 0, Const 0, Const !=), RS_AGG_VERGELIJK(RS_AFRONDING(Deel GEDEELD"
                        + " sonde 2 (Lees 1, Lees 0), Const 0, Const FLOOR), Const 0, Const >))",
                "    toets 0 Samengesteld bron 'hij aan alle volgende voorwaarden voldoet: - zijn ploegomvang is"
                        + " ongelijk aan 0 - het aandeel is groter dan 0.'",
                "    toets 0.0 Aanroep bron 'zijn ploegomvang is ongelijk aan 0'",
                "    toets 0.0.0 Lees bron 'zijn ploegomvang'",
                "    toets 0.0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '0'",
                "    toets 0.0.2 Const String",
                "    toets 0.1 Aanroep bron 'het aandeel is groter dan 0'",
                "    toets 0.1.0 Aanroep bron 'de vangstwaarde van de Jutter gedeeld door de ploegomvang van de"
                        + " Jutter naar beneden afgerond op 0 decimalen'",
                "    toets 0.1.0.0 Deel bron 'de vangstwaarde van de Jutter gedeeld door de ploegomvang van de"
                        + " Jutter'",
                "    toets 0.1.0.0.0 Lees bron 'de vangstwaarde van de Jutter'",
                "    toets 0.1.0.0.1 Lees bron 'de ploegomvang van de Jutter'",
                "    toets 0.1.0.1 Const Integer",
                "    toets 0.1.0.2 Const RoundingMode",
                "    toets 0.1.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '0'",
                "    toets 0.1.2 Const String",
                "  gevolg Inconsistentie [onderwerp=$onderwerp, vorm=bergingsaandeel van een jutter,"
                        + " invoeging=LOGISCH]"),
                regels(golden("jutterswet-ploegaandeel")));
    }

    @Test void een_begrensde_geldigheid_en_de_rekendatumstand() throws Exception {
        assertEquals(List.of(
                "regel vensterheffing in 2026 | vorm vensterheffing in 2026 | groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=vondstmelding, naam=meldwaarde,"
                        + " kenmerk=false])]",
                "  onderwerp Vondstmelding $onderwerp alfa [] bind [$lezing_0 = [].meldwaarde WAARDE]",
                "  patroon Rekendatum $geldigheid_0 alfa [geldig_0: [datum BINNEN Geldigheid[vanaf=2026-01-01,"
                        + " tot=2026-12-31]]] join [] bind []",
                "  gevolg Waarde [onderwerp=$onderwerp, doel=ElementSleutel[objecttype=vondstmelding,"
                        + " naam=vensterheffing, kenmerk=false], schrijver=vensterheffing in 2026,"
                        + " waarde=RS_PLUS(Lees 0, Const 25), slots=[0], grootte=1, collapse=true, schrijftLeeg=true,"
                        + " deponering=null, fout=Foutnaam[vorm=vensterheffing in 2026, regel=vensterheffing in"
                        + " 2026], invoeging=LOGISCH]",
                "    waarde 0 Aanroep bron 'de meldwaarde van de Vondstmelding vermeerderd met 25'",
                "    waarde 0.0 Lees bron 'de meldwaarde van de Vondstmelding'",
                "    waarde 0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '25'",
                "regel vensterheffing in 2026: geen rekendatum | vorm vensterheffing in 2026 | groep null",
                "  lezingen []",
                "  patroon Rekendatum $rekendatum alfa [geen-rekendatum_1: [datum LEEG]] join [] bind []",
                "  gevolg Fout [onderwerp=$rekendatum, zin=a validity-bounded form needs an evaluation date; this"
                        + " session is time-free, fout=Foutnaam[vorm=vensterheffing in 2026, regel=vensterheffing in"
                        + " 2026], invoeging=LOGISCH]",
                "regel overgangsheffing tot en met 2025 | vorm overgangsheffing tot en met 2025 | groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=vondstmelding, naam=meldwaarde,"
                        + " kenmerk=false])]",
                "  onderwerp Vondstmelding $onderwerp alfa [] bind [$lezing_0 = [].meldwaarde WAARDE]",
                "  patroon Rekendatum $geldigheid_0 alfa [geldig_2: [datum BINNEN Geldigheid[vanaf=null,"
                        + " tot=2025-12-31]]] join [] bind []",
                "  gevolg Waarde [onderwerp=$onderwerp, doel=ElementSleutel[objecttype=vondstmelding,"
                        + " naam=overgangsheffing, kenmerk=false], schrijver=overgangsheffing tot en met 2025,"
                        + " waarde=RS_PLUS(Lees 0, Const 40), slots=[0], grootte=1, collapse=true, schrijftLeeg=true,"
                        + " deponering=null, fout=Foutnaam[vorm=overgangsheffing tot en met 2025,"
                        + " regel=overgangsheffing tot en met 2025], invoeging=LOGISCH]",
                "    waarde 0 Aanroep bron 'de meldwaarde van de Vondstmelding vermeerderd met 40'",
                "    waarde 0.0 Lees bron 'de meldwaarde van de Vondstmelding'",
                "    waarde 0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '40'",
                "regel overgangsheffing tot en met 2025: geen rekendatum | vorm overgangsheffing tot en met 2025 |"
                        + " groep null",
                "  lezingen []",
                "  patroon Rekendatum $rekendatum alfa [geen-rekendatum_3: [datum LEEG]] join [] bind []",
                "  gevolg Fout [onderwerp=$rekendatum, zin=a validity-bounded form needs an evaluation date; this"
                        + " session is time-free, fout=Foutnaam[vorm=overgangsheffing tot en met 2025,"
                        + " regel=overgangsheffing tot en met 2025], invoeging=LOGISCH]",
                "regel rekendatumstand | vorm null | groep null",
                "  lezingen []",
                "  gevolg Rekendatumstand [global=rekendatum, invoeging=LOGISCH]"),
                regels(golden("jutterswet-vensterheffing")));
    }

    @Test void twee_schrijvers_van_een_doel() throws Exception {
        assertEquals(List.of(
                "regel vrijgave zonder beletsel | vorm vrijgave zonder beletsel | groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=bergingszaak, naam=geclaimd,"
                        + " kenmerk=true]), 1 INVOER $lezing_1 (eigen ElementSleutel[objecttype=bergingszaak,"
                        + " naam=bezwaren, kenmerk=false])]",
                "  onderwerp Bergingszaak $onderwerp alfa [] bind [$lezing_0 = [].geclaimd WAAR, $lezing_1 ="
                        + " [].bezwaren WAARDE]",
                "  toets voorwaarde_0 VOORWAARDE sonde -1 slots [0, 1] grootte 2: Samengesteld GEEN 0(KENMERK(Lees 0,"
                        + " Const true), RS_AGG_VERGELIJK(Lees 1, Const 0, Const >))",
                "    toets 0 Samengesteld bron 'geen van: geclaimd, bezwaren boven 0'",
                "    toets 0.0 Aanroep bron 'de Bergingszaak geclaimd is'",
                "    toets 0.0.0 Lees",
                "    toets 0.0.1 Const Boolean",
                "    toets 0.1 Aanroep bron 'haar bezwaren groter zijn dan 0'",
                "    toets 0.1.0 Lees bron 'de bezwaren van de Bergingszaak'",
                "    toets 0.1.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '0'",
                "    toets 0.1.2 Const String",
                "  gevolg Waarde [onderwerp=$onderwerp, doel=ElementSleutel[objecttype=bergingszaak, naam=status,"
                        + " kenmerk=false], schrijver=vrijgave zonder beletsel, waarde=Const vrijgegeven, slots=[],"
                        + " grootte=2, collapse=true, schrijftLeeg=true, deponering=null, fout=Foutnaam[vorm=vrijgave"
                        + " zonder beletsel, regel=vrijgave zonder beletsel], invoeging=LOGISCH]",
                "    waarde 0 Const String type Tekst[] bron ''vrijgegeven''",
                "regel aanhouding bij beletsel | vorm aanhouding bij beletsel | groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=bergingszaak, naam=geclaimd,"
                        + " kenmerk=true]), 1 INVOER $lezing_1 (eigen ElementSleutel[objecttype=bergingszaak,"
                        + " naam=bezwaren, kenmerk=false])]",
                "  onderwerp Bergingszaak $onderwerp alfa [] bind [$lezing_0 = [].geclaimd WAAR, $lezing_1 ="
                        + " [].bezwaren WAARDE]",
                "  toets voorwaarde_1 VOORWAARDE sonde -1 slots [0, 1] grootte 2: Samengesteld TEN_MINSTE"
                        + " 1(KENMERK(Lees 0, Const true), RS_AGG_VERGELIJK(Lees 1, Const 0, Const >))",
                "    toets 0 Samengesteld bron 'ten minste één van: geclaimd, bezwaren boven 0'",
                "    toets 0.0 Aanroep bron 'de Bergingszaak geclaimd is'",
                "    toets 0.0.0 Lees",
                "    toets 0.0.1 Const Boolean",
                "    toets 0.1 Aanroep bron 'haar bezwaren groter zijn dan 0'",
                "    toets 0.1.0 Lees bron 'de bezwaren van de Bergingszaak'",
                "    toets 0.1.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '0'",
                "    toets 0.1.2 Const String",
                "  gevolg Waarde [onderwerp=$onderwerp, doel=ElementSleutel[objecttype=bergingszaak, naam=status,"
                        + " kenmerk=false], schrijver=aanhouding bij beletsel, waarde=Const aangehouden, slots=[],"
                        + " grootte=2, collapse=true, schrijftLeeg=true, deponering=null,"
                        + " fout=Foutnaam[vorm=aanhouding bij beletsel, regel=aanhouding bij beletsel],"
                        + " invoeging=LOGISCH]",
                "    waarde 0 Const String type Tekst[] bron ''aangehouden''",
                "regel schrijvers Bergingszaak.status: verschil | vorm null | groep null",
                "  lezingen [0 WAARDEN $waarden (waarden)]",
                "  onderwerp Bergingszaak $onderwerp alfa [] bind []",
                "  accumulatie Waarde $waarde alfa [waarde_2: [attribuut GELIJK"
                        + " ElementSleutel[objecttype=bergingszaak, naam=status, kenmerk=false]]] join"
                        + " [waarde-onderwerp_3: onderwerp = $onderwerp] bind [] functies [LIJST null -> $waarden]",
                "  toets verschil_4 VORM sonde -1 slots [0] grootte 1: VERSCHILLEN(Lees 0)",
                "    toets 0 Aanroep",
                "    toets 0.0 Lees",
                "  gevolg Schrijversconflict [onderwerp=$onderwerp, waarden=0,"
                        + " doel=ElementSleutel[objecttype=bergingszaak, naam=status, kenmerk=false],"
                        + " attribuut=status, documentplaats={aanhouding bij beletsel=1, vrijgave zonder beletsel=0},"
                        + " typenamen={Bergingszaak=Bergingszaak}, invoeging=LOGISCH]"),
                regels(golden("jutterswet-vrijgavetoets")));
    }

    @Test void een_onderwerp_buiten_een_stapel() throws Exception {
        assertEquals(List.of(
                "regel uitkering van een strandrecht | vorm uitkering van een strandrecht | groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=strandrecht, naam=grondslag,"
                        + " kenmerk=false])]",
                "  onderwerp Strandrecht $onderwerp alfa [niet-in-een-stapel_0: [null KLASSE_NIET_IN [Wrakrecht]]]"
                        + " bind [$lezing_0 = [].grondslag WAARDE]",
                "  gevolg Waarde [onderwerp=$onderwerp, doel=ElementSleutel[objecttype=strandrecht, naam=uitkering,"
                        + " kenmerk=false], schrijver=uitkering van een strandrecht, waarde=RS_MAAL(Lees 0, Const 2),"
                        + " slots=[0], grootte=1, collapse=true, schrijftLeeg=false, deponering=null,"
                        + " fout=Foutnaam[vorm=uitkering van een strandrecht, regel=uitkering van een strandrecht],"
                        + " invoeging=LOGISCH]",
                "    waarde 0 Aanroep bron 'zijn grondslag maal 2'",
                "    waarde 0.0 Lees bron 'zijn grondslag'",
                "    waarde 0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '2'"),
                regels(golden("jutterswet-wrakerfbasis"), p -> !RegelplanWitnessTest.vanEenStapel(p)));
    }

    @Test void een_initialisatie_zonder_andere_schrijver() throws Exception {
        assertEquals(List.of(
                "regel standaardbergloon | vorm standaardbergloon | groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=vondst, naam=gewicht,"
                        + " kenmerk=false])]",
                "  onderwerp Vondst $onderwerp alfa [doel-leeg-in-de-invoer_1: [bergloon LEEG]] bind [$lezing_0 ="
                        + " [].gewicht WAARDE]",
                "  toets voorwaarde_0 VOORWAARDE sonde -1 slots [0] grootte 1: RS_AGG_VERGELIJK(Lees 0, Const 10,"
                        + " Const >)",
                "    toets 0 Aanroep bron 'zijn gewicht groter is dan 10'",
                "    toets 0.0 Lees bron 'zijn gewicht'",
                "    toets 0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '10'",
                "    toets 0.2 Const String",
                "  niet Waarde $andere_schrijver alfa [andere-schrijver_2: [attribuut GELIJK"
                        + " ElementSleutel[objecttype=vondst, naam=bergloon, kenmerk=false], schrijver NIET_GELIJK"
                        + " standaardbergloon]] join [andere-schrijver-onderwerp_3: onderwerp = $onderwerp] bind []",
                "  gevolg Waarde [onderwerp=$onderwerp, doel=ElementSleutel[objecttype=vondst, naam=bergloon,"
                        + " kenmerk=false], schrijver=standaardbergloon, waarde=RS_MAAL(Const 2, Lees 0), slots=[0],"
                        + " grootte=1, collapse=true, schrijftLeeg=false, deponering=null,"
                        + " fout=Foutnaam[vorm=standaardbergloon, regel=standaardbergloon], invoeging=LOGISCH]",
                "    waarde 0 Aanroep bron '2 maal zijn gewicht'",
                "    waarde 0.0 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '2'",
                "    waarde 0.1 Lees bron 'zijn gewicht'"),
                regels(golden("jutterswet-voorwaardelijke-initialisatie")));
    }

    @Test void een_initialisatie_naast_een_schrijver_die_leeg_schrijft() throws Exception {
        assertEquals(List.of(
                "regel standaardbergloon | vorm standaardbergloon | groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=vondst, naam=gewicht,"
                        + " kenmerk=false]), 1 LEGE_SCHRIJVERS $lege_schrijvers (lege schrijvers), 2 ONDERWERP"
                        + " $onderwerp (onderwerp zelf)]",
                "  onderwerp Vondst $onderwerp alfa [] bind [$lezing_0 = [].gewicht WAARDE]",
                "  toets voorwaarde_0 VOORWAARDE sonde -1 slots [0] grootte 1: RS_AGG_VERGELIJK(Lees 0, Const 10,"
                        + " Const >)",
                "    toets 0 Aanroep bron 'zijn gewicht groter is dan 10'",
                "    toets 0.0 Lees bron 'zijn gewicht'",
                "    toets 0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '10'",
                "    toets 0.2 Const String",
                "  niet Waarde $andere_schrijver alfa [andere-schrijver_1: [attribuut GELIJK"
                        + " ElementSleutel[objecttype=vondst, naam=bergloon, kenmerk=false], schrijver NIET_GELIJK"
                        + " standaardbergloon, waarde GEVULD]] join [andere-schrijver-onderwerp_2: onderwerp ="
                        + " $onderwerp] bind []",
                "  accumulatie Waarde $lege_schrijver alfa [lege-schrijver_3: [attribuut GELIJK"
                        + " ElementSleutel[objecttype=vondst, naam=bergloon, kenmerk=false], schrijver NIET_GELIJK"
                        + " standaardbergloon, waarde LEEG]] join [lege-schrijver-onderwerp_4: onderwerp ="
                        + " $onderwerp] bind [] functies [AANTAL null -> $lege_schrijvers]",
                "  toets doel-leeg_5 VORM sonde -1 slots [1, 2] grootte 3: DOEL_LEEG(Lees 1, Invoer Vondst.bergloon"
                        + " WAARDE (Lees 2))",
                "    toets 0 Aanroep",
                "    toets 0.0 Lees",
                "    toets 0.1 Invoer",
                "    toets 0.1.0 Lees",
                "  gevolg Waarde [onderwerp=$onderwerp, doel=ElementSleutel[objecttype=vondst, naam=bergloon,"
                        + " kenmerk=false], schrijver=standaardbergloon, waarde=RS_MAAL(Const 2, Lees 0), slots=[0],"
                        + " grootte=3, collapse=true, schrijftLeeg=false, deponering=null,"
                        + " fout=Foutnaam[vorm=standaardbergloon, regel=standaardbergloon], invoeging=LOGISCH]",
                "    waarde 0 Aanroep bron '2 maal zijn gewicht'",
                "    waarde 0.0 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '2'",
                "    waarde 0.1 Lees bron 'zijn gewicht'",
                "regel zwaar bergloon | vorm zwaar bergloon | groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=vondst, naam=gewicht,"
                        + " kenmerk=false])]",
                "  onderwerp Vondst $onderwerp alfa [] bind [$lezing_0 = [].gewicht WAARDE]",
                "  toets voorwaarde_6 VOORWAARDE sonde -1 slots [0] grootte 1: RS_AGG_VERGELIJK(Lees 0, Const 10,"
                        + " Const >)",
                "    toets 0 Aanroep bron 'zijn gewicht groter is dan 10'",
                "    toets 0.0 Lees bron 'zijn gewicht'",
                "    toets 0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '10'",
                "    toets 0.2 Const String",
                "  gevolg Waarde [onderwerp=$onderwerp, doel=ElementSleutel[objecttype=vondst, naam=bergloon,"
                        + " kenmerk=false], schrijver=zwaar bergloon, waarde=RS_MAAL(Const 2, Lees 0), slots=[0],"
                        + " grootte=1, collapse=true, schrijftLeeg=true, deponering=null, fout=Foutnaam[vorm=zwaar"
                        + " bergloon, regel=zwaar bergloon], invoeging=LOGISCH]",
                "    waarde 0 Aanroep bron '2 maal zijn gewicht'",
                "    waarde 0.0 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '2'",
                "    waarde 0.1 Lees bron 'zijn gewicht'",
                "regel licht bergloon | vorm licht bergloon | groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=vondst, naam=gewicht,"
                        + " kenmerk=false])]",
                "  onderwerp Vondst $onderwerp alfa [] bind [$lezing_0 = [].gewicht WAARDE]",
                "  toets voorwaarde_7 VOORWAARDE sonde -1 slots [0] grootte 1: RS_AGG_VERGELIJK(Lees 0, Const 10,"
                        + " Const >)",
                "    toets 0 Aanroep bron 'zijn gewicht groter is dan 10'",
                "    toets 0.0 Lees bron 'zijn gewicht'",
                "    toets 0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '10'",
                "    toets 0.2 Const String",
                "  gevolg Waarde [onderwerp=$onderwerp, doel=ElementSleutel[objecttype=vondst, naam=bergloon,"
                        + " kenmerk=false], schrijver=licht bergloon, waarde=RS_MAAL(Const 2, Lees 0), slots=[0],"
                        + " grootte=1, collapse=true, schrijftLeeg=true, deponering=null, fout=Foutnaam[vorm=licht"
                        + " bergloon, regel=licht bergloon], invoeging=LOGISCH]",
                "    waarde 0 Aanroep bron '2 maal zijn gewicht'",
                "    waarde 0.0 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '2'",
                "    waarde 0.1 Lees bron 'zijn gewicht'",
                "regel schrijvers Vondst.bergloon: verschil | vorm null | groep null",
                "  lezingen [0 WAARDEN $waarden (waarden)]",
                "  onderwerp Vondst $onderwerp alfa [] bind []",
                "  accumulatie Waarde $waarde alfa [waarde_8: [attribuut GELIJK ElementSleutel[objecttype=vondst,"
                        + " naam=bergloon, kenmerk=false]]] join [waarde-onderwerp_9: onderwerp = $onderwerp] bind []"
                        + " functies [LIJST null -> $waarden]",
                "  toets verschil_10 VORM sonde -1 slots [0] grootte 1: VERSCHILLEN(Lees 0)",
                "    toets 0 Aanroep",
                "    toets 0.0 Lees",
                "  gevolg Schrijversconflict [onderwerp=$onderwerp, waarden=0, doel=ElementSleutel[objecttype=vondst,"
                        + " naam=bergloon, kenmerk=false], attribuut=bergloon, documentplaats={licht bergloon=2,"
                        + " standaardbergloon=0, zwaar bergloon=1}, typenamen={Vondst=Vondst}, invoeging=LOGISCH]"),
                regels(initialisatieNaastLegeSchrijver()));
    }

    /** The golden {@code jutterswet-voorwaardelijke-initialisatie}, whose initialisation of the bergloon is the
     *  target's one writer, with two computing rules on the bergloon beside it, copies of the initialisation's version
     *  that compute ({@code calculation}): two computing rules on one target are both outside the standing domain
     *  (RUNTIME-SEMANTICS ch. 7.8), so each writes a computed leeg. Verifier-clean. */
    static JsonNode initialisatieNaastLegeSchrijver() throws Exception {
        ObjectNode d = (ObjectNode) golden("jutterswet-voorwaardelijke-initialisatie");
        ArrayNode regels = (ArrayNode) d.path("rules").path("rules");
        ObjectNode initialisatie = (ObjectNode) regels.get(0);
        for (String naam : List.of("zwaar bergloon", "licht bergloon")) {
            ObjectNode r = initialisatie.deepCopy();
            r.put("name", naam);
            ((ObjectNode) r.path("versions").get(0)).put("initialisation", false).put("presentation", "calculation");
            regels.add(r);
        }
        return d;
    }

    private static JsonNode golden(String naam) throws Exception {
        return JSON.readTree(GOLDEN.resolve(naam).resolve("ir.json").toFile());
    }

    /** Every plan of {@code document}'s rules as text, in the order of the model; after a line that holds a
     *  term, the term node by node ({@link Termboom#velden}). */
    private static List<String> regels(JsonNode document) throws ReflectiveOperationException {
        return regels(document, p -> true);
    }

    /** As {@link #regels(JsonNode)}, of the plans {@code welke} takes. */
    static List<String> regels(JsonNode document, Predicate<Regelplan> welke) throws ReflectiveOperationException {
        List<String> uit = new ArrayList<>();
        for (Regelplan p : DroolsBackend.build(document, DroolsBackend.Branch.EXEC_MODEL).execModelTranspiler()
                .modelplan().regels()) {
            if (!welke.test(p)) continue;
            uit.add("regel " + p.naam() + " | vorm " + p.vorm() + " | groep " + p.agendagroep());
            List<String> slots = new ArrayList<>();
            for (Lezing l : p.lezingen().slots())
                slots.add(l.slot() + " " + l.soort() + " " + l.naam() + " (" + l.sleutel() + ")");
            uit.add("  lezingen " + slots);
            for (LhsItem item : p.linkerkant()) {
                uit.add("  " + item(item));
                if (item instanceof Toetsitem t) Termboom.velden("    toets", t.toets(), "0", uit);
            }
            uit.add("  gevolg " + gevolg(p.gevolg()));
            for (RecordComponent c : p.gevolg().getClass().getRecordComponents())
                if (c.getAccessor().invoke(p.gevolg()) instanceof Term t)
                    Termboom.velden("    " + c.getName(), t, "0", uit);
        }
        return uit;
    }

    /** Whether {@code p} is a plan of a rule of a defaults stack, whose form the manifest names after the stack. */
    static boolean vanEenStapel(Regelplan p) {
        return p.vorm() != null && p.vorm().startsWith("defaults ");
    }

    private static String item(LhsItem item) {
        return switch (item) {
            case Onderwerp o -> "onderwerp " + o.bonenklasse().getSimpleName() + " " + o.naam() + " alfa "
                    + alfa(o.alfa()) + " bind " + bindingen(o.bindingen());
            case Patroon p -> "patroon " + patroon(p);
            case Niet n -> "niet " + (n.patronen().size() == 1 ? patroon(n.patronen().get(0)) : "binnen "
                    + n.binnen() + " buiten " + n.buiten() + " "
                    + n.patronen().stream().map(RegelplanWitnessTest::patroon).toList());
            case Bestaat b -> "bestaat " + b.patronen().stream().map(RegelplanWitnessTest::patroon).toList();
            case Accumulatie a -> "accumulatie " + patroon(a.bron()) + " functies "
                    + a.functies().stream().map(f -> f.soort() + " " + f.bron() + " -> " + f.doel()).toList();
            case Toetsitem t -> "toets " + t.id() + " " + t.rol() + " sonde " + t.sonde() + " slots " + t.slots()
                    + " grootte " + t.grootte() + ": " + Termboom.boom(t.toets());
        };
    }

    private static String patroon(Patroon p) {
        return p.feitklasse().getSimpleName() + " " + p.naam() + " alfa " + alfa(p.alfa()) + " join "
                + p.joins().stream().map((Join j) -> j.id() + ": " + j.veld() + " = " + j.doel()).toList() + " bind "
                + bindingen(p.bindingen());
    }

    private static List<String> alfa(List<Alfa> alfa) {
        List<String> uit = new ArrayList<>();
        for (Alfa a : alfa)
            uit.add(a.id() + ": " + a.toetsen().stream().map(t -> t.veld() + " " + t.op()
                    + (t.waarde() == null ? "" : " " + waarde(t.waarde()))).toList());
        return uit;
    }

    private static List<String> bindingen(List<Binding> bindingen) {
        return bindingen.stream().map(b -> b.doel() + " = " + b.pad().stream().map(Binding.Veld::naam).toList() + "."
                + b.veld().naam() + " " + b.vorm()).toList();
    }

    /** The kind of {@code g} and every field of it, a term as a tree and a table in key order. */
    private static String gevolg(Gevolg g) {
        List<String> velden = new ArrayList<>();
        try {
            for (RecordComponent c : g.getClass().getRecordComponents())
                velden.add(c.getName() + "=" + waarde(c.getAccessor().invoke(g)));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return g.getClass().getSimpleName() + " " + velden;
    }

    private static String waarde(Object w) {
        if (w instanceof Term t) return Termboom.boom(t);
        if (w instanceof Class<?> k) return k.getSimpleName();
        if (w instanceof Map<?, ?> m) {
            Map<String, String> geordend = new TreeMap<>();
            m.forEach((k, v) -> geordend.put(String.valueOf(k), String.valueOf(v)));
            return geordend.toString();
        }
        if (w instanceof Collection<?> c && !c.isEmpty() && c.iterator().next() instanceof Class<?>) {
            TreeSet<String> namen = new TreeSet<>();
            for (Object k : c) namen.add(((Class<?>) k).getSimpleName());
            return namen.toString();
        }
        return String.valueOf(w);
    }
}
