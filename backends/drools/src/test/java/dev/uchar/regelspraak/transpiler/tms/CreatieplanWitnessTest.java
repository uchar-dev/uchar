package dev.uchar.regelspraak.transpiler.tms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.uchar.regelspraak.cases.CaseRunner;
import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.graaf.ProducentConsument.ElementSleutel;
import dev.uchar.regelspraak.transpiler.tms.plan.Accumulatie;
import dev.uchar.regelspraak.transpiler.tms.plan.Alfa;
import dev.uchar.regelspraak.transpiler.tms.plan.Bestaat;
import dev.uchar.regelspraak.transpiler.tms.plan.Betatoets;
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
 * two rules of the standing of a path over a relation a form creates. Each is a {@link Regelplan} whose left-hand side
 * holds what the form built itself before, in its order and with its ids: the negations of the guard, the chain of a
 * path as patterns, a negation over the whole chain with its scope, and a guard on a target after the constraint that
 * reaches it ({@link Patroon#naAlfa}); and whose consequence carries every decision as a field: the class of the new
 * bean, its initialisations with their terms, its construction before any insertion, its depth and budget, the path of
 * its fault, each insertion with its kind, and the direction of a link ({@link Gevolg.Creatie},
 * {@link Gevolg.Koppeling}, {@link Gevolg.TweedeVulling}).
 *
 * <p>This witness pins those plans as text, per plan in order: the name, the form and the group; the slots, each with
 * its kind, its variable and its key; every item of the left-hand side in order with its kind, its pattern class, its
 * constraint ids, each field test with the class it reads the field through where the plan names one, its joins, its
 * constraints between two objects with their function and each constant with every field, the constraints after
 * those, its bindings, and of an existence or a negation the variables it declares and those it reads from outside;
 * and the consequence with every field. A term, the test of a rule or the value of an initialisation, is pinned as a
 * tree and on the lines after it node by node with its source fragment and of a constant the class of its value and
 * its whole IR type ({@link Termboom#velden}).
 *
 * <ul>
 *   <li>{@code anchor/jutterswet-penninguitgifte}: two object creations without a {@code connectingRole} binding, one
 *       with an attribute initialisation and a characteristic initialised to waar, the other without one and with
 *       the guard of a 1:1 relation.</li>
 *   <li>{@code jutterswet-penningbesluit}: an object creation without a binding, whose value reads a parameter.</li>
 *   <li>{@code jutterswet-bakensein}: a fact creation whose guard on the target reads the characteristic off the bean,
 *       through the getter of its declaring type, a supertype of the target; the rule of the links of the input and
 *       the rule of a second filler of the created relation.</li>
 *   <li>{@link #gehesenDoorEenRegel}, since no golden has one: the same fact creation where a rule writes the
 *       characteristic, so the guard reads its standing, joined to the target. Fired as well, against the values the
 *       reference interpreter and the ASP backend give.</li>
 *   <li>{@code jutterswet-havenketen}: the standing of a path whose first hop goes over the input and whose second
 *       over a relation a fact creation makes, of a member a rule writes and of one off the bean.</li>
 *   <li>{@code anchor/jutterswet-erflijn}: fact creations whose path walks the relation they create, and so not a link
 *       of the phase they fire in.</li>
 * </ul>
 *
 * <p>That the rendering of these plans builds the rules of before the step is what the harness {@code Vuurspoor}
 * shows, and the executable model of the corpus, which is equal as text; for {@link #gehesenDoorEenRegel}, outside the
 * corpus, the executable model of before and after the step is equal as text as well. That a creation constructs its
 * new object before it inserts anything is what the constructor of {@link Gevolg.Creatie} holds and what
 * {@link #de_nieuwe_bean_komt_gevuld_in_het_werkgeheugen} watches in working memory; that it inserts in the order of
 * its consequence before the step, and in no other, the constructor holds as well
 * ({@link #de_creatie_houdt_de_volgorde_van_haar_gevolg}, fix round 1c-C1).
 *
 * <p><b>Red</b> before the step: these forms built their rules themselves, so there was no plan of them, and this class
 * did not compile, since the plan had no consequence of a creation or a link, no constraint after the joins and no
 * scope of a negation; with only the names of the plans pinned, none of the six documents had a plan of any of these
 * rules.
 */
class CreatieplanWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final ObjectMapper JSON = new ObjectMapper();

    /** A plan of this step's forms: a creation, a link, a second filler, or the standing of a path over a relation a
     *  form creates, which reads that relation's {@code Koppeling} facts. */
    private static boolean vanDezeStap(Regelplan p) {
        return switch (p.gevolg()) {
            case Gevolg.Creatie c -> true;
            case Gevolg.Koppeling k -> true;
            case Gevolg.TweedeVulling t -> true;
            case Gevolg.Padstand s -> p.linkerkant().stream().anyMatch(CreatieplanWitnessTest::leestKoppelingen);
            default -> false;
        };
    }

    private static boolean leestKoppelingen(LhsItem item) {
        return switch (item) {
            case Patroon p -> p.feitklasse() == Koppeling.class;
            case Niet n -> n.patronen().stream().anyMatch(p -> p.feitklasse() == Koppeling.class);
            default -> false;
        };
    }

    @Test void objectcreaties_met_initialisaties_een_waar_kenmerk_en_de_bewaking_van_een_een_op_een_relatie()
            throws Exception {
        assertEquals(List.of(
                "regel penning voor een gerechtigde jutter | vorm penning voor een gerechtigde jutter |"
                        + " groep uchar-objectcreatie",
                "  lezingen [0 INVOER $penninggerechtigd (eigen ElementSleutel[objecttype=jutter,"
                        + " naam=penninggerechtigd, kenmerk=true]), 1 INVOER $vondstwaarde (eigen"
                        + " ElementSleutel[objecttype=jutter, naam=vondstwaarde, kenmerk=false]), 2 CREATIEFASE"
                        + " $fase (creatiefase), 3 HERKOMST $herkomsten_creatie_42 (herkomst)]",
                "  onderwerp Jutter $onderwerp alfa [] join [] beta [] bind [$penninggerechtigd = []"
                        + " Jutter.penninggerechtigd WAAR, $vondstwaarde = [] Jutter.vondstwaarde WAARDE]",
                "  patroon Creatiefase $creatiefase alfa [creatiestratum_40: [[] stratum WAARDE MINSTENS"
                        + " 0]] join [] beta [] na [] bind [$fase = [] Creatiefase.nummer WAARDE]",
                "  accumulatie Gemaakt $herkomst_creatie_41 alfa [] join [herkomst_43: object ="
                        + " $onderwerp] beta [] na [] bind [] functies [LIJST null -> $herkomsten_creatie_42]",
                "  niet binnen [$gemaakt_creatie_44] buiten [$onderwerp] [Gemaakt $gemaakt_creatie_44"
                        + " alfa [gemaakt_46: [[] regel WAARDE GELIJK penning voor een gerechtigde jutter]] join"
                        + " [gemaakt-onderwerp_47: onderwerp = $onderwerp] beta [] na [] bind []]",
                "  niet binnen [$nieuw_creatie_45] buiten [$onderwerp, $fase] [Gemaakt $nieuw_creatie_45"
                        + " alfa [] join [nieuw-object_48: object = $onderwerp] beta [nieuw-in-deze-fase_49:"
                        + " IN_FASE $fase] na [] bind []]",
                "  toets voorwaarde_50 VOORWAARDE sonde -1 slots [0] grootte 4: KENMERK(Lees 0, Const"
                        + " true)",
                "    toets 0 Aanroep bron 'de Jutter is penninggerechtigd'",
                "    toets 0.0 Lees",
                "    toets 0.1 Const Boolean",
                "  niet binnen [$eigen_fout] buiten [$onderwerp] [Fout $eigen_fout alfa [eigen-fout_51:"
                        + " [[] regel WAARDE GELIJK penning voor een gerechtigde jutter]] join"
                        + " [eigen-fout-onderwerp_52: onderwerp = $onderwerp] beta [] na [] bind []]",
                "  gevolg Creatie [onderwerp=$onderwerp, creatie=penning voor een gerechtigde jutter,"
                        + " diepte=Creatiediepte[herkomst=3, stratum=0, type=Jutter, budget=100],"
                        + " fout=Foutnaam[vorm=penning voor een gerechtigde jutter, regel=penning voor een"
                        + " gerechtigde jutter], foutinvoeging=GESTELD, fase=2, slots=[1, 2, 3], grootte=4]",
                "    constructie Penning zettingen [0 Penning.bedrag]",
                "    initialisatie 0 ElementSleutel[objecttype=penning, naam=bedrag, kenmerk=false]:"
                        + " Lees 1",
                "      waarde 0 Lees bron 'de vondstwaarde van de Jutter'",
                "    initialisatie 1 ElementSleutel[objecttype=penning, naam=uitgereikt, kenmerk=true]:"
                        + " Const true",
                "      waarde 0 Const Boolean type Booleaan[] bron 'waar'",
                "    invoegingen [Nieuw[invoeging=GESTELD], Merk[invoeging=GESTELD],"
                        + " Link[relatie=penningbezit, nieuwIsEerste=false, invoeging=GESTELD],"
                        + " Kenmerk[initialisatie=1, invoeging=GESTELD]]",
                "regel dossier voor een gerechtigde jutter | vorm dossier voor een gerechtigde jutter |"
                        + " groep uchar-objectcreatie",
                "  lezingen [0 INVOER $penninggerechtigd (eigen ElementSleutel[objecttype=jutter,"
                        + " naam=penninggerechtigd, kenmerk=true]), 1 CREATIEFASE $fase (creatiefase), 2 HERKOMST"
                        + " $herkomsten_creatie_55 (herkomst)]",
                "  onderwerp Jutter $onderwerp alfa [geen-gekoppelde-instantie_66: [[] dossier WAARDE LEEG]] join"
                        + " [] beta [] bind [$penninggerechtigd = [] Jutter.penninggerechtigd WAAR]",
                "  patroon Creatiefase $creatiefase alfa [creatiestratum_53: [[] stratum WAARDE MINSTENS"
                        + " 0]] join [] beta [] na [] bind [$fase = [] Creatiefase.nummer WAARDE]",
                "  accumulatie Gemaakt $herkomst_creatie_54 alfa [] join [herkomst_56: object ="
                        + " $onderwerp] beta [] na [] bind [] functies [LIJST null -> $herkomsten_creatie_55]",
                "  niet binnen [$gemaakt_creatie_57] buiten [$onderwerp] [Gemaakt $gemaakt_creatie_57"
                        + " alfa [gemaakt_59: [[] regel WAARDE GELIJK dossier voor een gerechtigde jutter]] join"
                        + " [gemaakt-onderwerp_60: onderwerp = $onderwerp] beta [] na [] bind []]",
                "  niet binnen [$nieuw_creatie_58] buiten [$onderwerp, $fase] [Gemaakt $nieuw_creatie_58"
                        + " alfa [] join [nieuw-object_61: object = $onderwerp] beta [nieuw-in-deze-fase_62:"
                        + " IN_FASE $fase] na [] bind []]",
                "  toets voorwaarde_63 VOORWAARDE sonde -1 slots [0] grootte 3: KENMERK(Lees 0, Const"
                        + " true)",
                "    toets 0 Aanroep bron 'de Jutter is penninggerechtigd'",
                "    toets 0.0 Lees",
                "    toets 0.1 Const Boolean",
                "  niet binnen [$eigen_fout] buiten [$onderwerp] [Fout $eigen_fout alfa [eigen-fout_64:"
                        + " [[] regel WAARDE GELIJK dossier voor een gerechtigde jutter]] join"
                        + " [eigen-fout-onderwerp_65: onderwerp = $onderwerp] beta [] na [] bind []]",
                "  gevolg Creatie [onderwerp=$onderwerp, creatie=dossier voor een gerechtigde jutter,"
                        + " diepte=Creatiediepte[herkomst=2, stratum=0, type=Jutter, budget=100],"
                        + " fout=Foutnaam[vorm=dossier voor een gerechtigde jutter, regel=dossier voor een"
                        + " gerechtigde jutter], foutinvoeging=GESTELD, fase=1, slots=[1, 2], grootte=3]",
                "    constructie Dossier zettingen []",
                "    invoegingen [Nieuw[invoeging=GESTELD], Merk[invoeging=GESTELD],"
                        + " Link[relatie=dossiervoering, nieuwIsEerste=false, invoeging=GESTELD]]"),
                plannen(golden("anchor/jutterswet-penninguitgifte"), p -> p.gevolg() instanceof Gevolg.Creatie));
    }

    @Test void een_objectcreatie_zonder_binding() throws Exception {
        assertEquals(List.of(
                "regel stel het penningbesluit vast | vorm stel het penningbesluit vast | groep"
                        + " uchar-objectcreatie",
                "  lezingen [0 KENMERKSTAND $penninggerechtigd (eigen ElementSleutel[objecttype=jutter,"
                        + " naam=penninggerechtigd, kenmerk=true]), 1 PARAMETER $penningbedrag (parameter"
                        + " penningbedrag), 2 CREATIEFASE $fase (creatiefase), 3 HERKOMST $herkomsten_creatie_21"
                        + " (herkomst)]",
                "  onderwerp Jutter $onderwerp alfa [geen-gekoppelde-instantie_32: [[] toegekend besluit"
                        + " WAARDE LEEG]] join [] beta [] bind []",
                "  patroon ParameterStand $parameter_1 alfa [parameter_18: [[] parameter WAARDE GELIJK"
                        + " penningbedrag]] join [] beta [] na [] bind [$penningbedrag = [] ParameterStand.waarde"
                        + " WAARDE]",
                "  patroon Creatiefase $creatiefase alfa [creatiestratum_19: [[] stratum WAARDE MINSTENS"
                        + " 0]] join [] beta [] na [] bind [$fase = [] Creatiefase.nummer WAARDE]",
                "  patroon KenmerkStand $stand_0 alfa [stand_16: [[] kenmerk WAARDE GELIJK"
                        + " ElementSleutel[objecttype=jutter, naam=penninggerechtigd, kenmerk=true]]] join"
                        + " [stand-onderwerp_17: onderwerp = $onderwerp] beta [] na [] bind [$penninggerechtigd ="
                        + " [] KenmerkStand.waar WAARDE]",
                "  accumulatie Gemaakt $herkomst_creatie_20 alfa [] join [herkomst_22: object ="
                        + " $onderwerp] beta [] na [] bind [] functies [LIJST null -> $herkomsten_creatie_21]",
                "  niet binnen [$gemaakt_creatie_23] buiten [$onderwerp] [Gemaakt $gemaakt_creatie_23"
                        + " alfa [gemaakt_25: [[] regel WAARDE GELIJK stel het penningbesluit vast]] join"
                        + " [gemaakt-onderwerp_26: onderwerp = $onderwerp] beta [] na [] bind []]",
                "  niet binnen [$nieuw_creatie_24] buiten [$onderwerp, $fase] [Gemaakt $nieuw_creatie_24"
                        + " alfa [] join [nieuw-object_27: object = $onderwerp] beta [nieuw-in-deze-fase_28:"
                        + " IN_FASE $fase] na [] bind []]",
                "  toets voorwaarde_29 VOORWAARDE sonde -1 slots [0] grootte 4: KENMERK(Lees 0, Const"
                        + " true)",
                "    toets 0 Aanroep bron 'hij penninggerechtigd is'",
                "    toets 0.0 Lees",
                "    toets 0.1 Const Boolean",
                "  niet binnen [$eigen_fout] buiten [$onderwerp] [Fout $eigen_fout alfa [eigen-fout_30:"
                        + " [[] regel WAARDE GELIJK stel het penningbesluit vast]] join [eigen-fout-onderwerp_31:"
                        + " onderwerp = $onderwerp] beta [] na [] bind []]",
                "  gevolg Creatie [onderwerp=$onderwerp, creatie=stel het penningbesluit vast,"
                        + " diepte=Creatiediepte[herkomst=3, stratum=0, type=Jutter, budget=100],"
                        + " fout=Foutnaam[vorm=stel het penningbesluit vast, regel=stel het penningbesluit vast],"
                        + " foutinvoeging=GESTELD, fase=2, slots=[1, 2, 3], grootte=4]",
                "    constructie Penningbesluit zettingen [0 Penningbesluit.besluitbedrag]",
                "    initialisatie 0 ElementSleutel[objecttype=penningbesluit, naam=besluitbedrag,"
                        + " kenmerk=false]: Lees 1",
                "      waarde 0 Lees bron 'het penningbedrag'",
                "    invoegingen [Nieuw[invoeging=GESTELD], Merk[invoeging=GESTELD],"
                        + " Link[relatie=penningtoekenning, nieuwIsEerste=false, invoeging=GESTELD]]"),
                plannen(golden("jutterswet-penningbesluit"), p -> p.gevolg() instanceof Gevolg.Creatie));
    }

    @Test void een_feitcreatie_die_het_kenmerk_van_het_doel_van_de_bean_leest_met_de_koppelingregels() throws Exception {
        assertEquals(List.of(
                "regel vakbeseining door het kustvak | vorm vakbeseining door het kustvak | groep"
                        + " uchar-feitcreatie",
                "  lezingen [0 CREATIEFASE $fase (creatiefase)]",
                "  onderwerp Kustvak $onderwerp alfa [] join [] beta [] bind []",
                "  patroon Creatiefase $creatiefase alfa [creatiestratum_13: [[] stratum WAARDE MINSTENS"
                        + " 0]] join [] beta [] na [] bind [$fase = [] Creatiefase.nummer WAARDE]",
                "  patroon Post $stap_keten_14 alfa [] join [] beta [veld-ver_15: ONTMOETING $onderwerp"
                        + " [kustvak] {List<String>, type null, bron null} [] {List<>, type null, bron null}] na"
                        + " [] bind []",
                "  patroon Sein $stap_keten_16 alfa [] join [] beta [veld-ver_17: ONTMOETING"
                        + " $stap_keten_14 [post] {List<String>, type null, bron null} [] {List<>, type null, bron"
                        + " null}] na [doel-gastheer_18: [[] null WAARDE IS_EEN Baken], doel-kenmerk_19: [[]"
                        + " gehesen WAAR GELIJK true via Baken]] bind []",
                "  niet binnen [$bestaand_feitcreatie_20] buiten [$onderwerp, $stap_keten_16] [Koppeling"
                        + " $bestaand_feitcreatie_20 alfa [bestaand_21: [[] relatie WAARDE GELIJK vakbeseining]]"
                        + " join [bestaand-anker_22: eerste = $onderwerp, bestaand-doel_23: tweede ="
                        + " $stap_keten_16] beta [] na [] bind []]",
                "  gevolg Koppeling[relatie=vakbeseining, eerste=$onderwerp, tweede=$stap_keten_16,"
                        + " fase=0, invoeging=GESTELD]",
                "regel koppeling vakbeseining uit de invoer: Sein.seinvak | vorm null | groep null",
                "  lezingen []",
                "  onderwerp Sein $houder alfa [koppeling-invoer_24: [[] seinvak WAARDE GEVULD]] join []"
                        + " beta [] bind [$ander = [] Sein.seinvak WAARDE]",
                "  gevolg Koppeling[relatie=vakbeseining, eerste=$ander, tweede=$houder, fase=-1,"
                        + " invoeging=LOGISCH]",
                "regel koppeling vakbeseining: tweede vulling van seinvak | vorm null | groep null",
                "  lezingen []",
                "  patroon Koppeling $een alfa [vulling_25: [[] relatie WAARDE GELIJK vakbeseining]]"
                        + " join [] beta [] na [] bind [$nabij = [] Koppeling.tweede WAARDE]",
                "  patroon Koppeling $twee alfa [tweede-vulling_26: [[] relatie WAARDE GELIJK"
                        + " vakbeseining]] join [tweede-vulling-nabij_27: tweede = $een.Koppeling.tweede] beta"
                        + " [tweede-vulling-ander_28: ANDERE_VULLING $een eerste {String, type null, bron null}]"
                        + " na [] bind []",
                "  gevolg TweedeVulling[onderwerp=$nabij, relatie=vakbeseining, zin=de naar-één-rol"
                        + " \"seinvak\" van een Sein heeft twee verschillende vullingen; een tweede vulling is een"
                        + " fout, geen overschrijving (RUNTIME-SEMANTICS ch. 5.5), invoeging=LOGISCH]"),
                plannen(golden("jutterswet-bakensein"), CreatieplanWitnessTest::vanDezeStap));
    }

    @Test void een_feitcreatie_die_het_kenmerk_van_het_doel_uit_zijn_stand_leest() throws Exception {
        assertEquals(List.of(
                "regel vakbeseining door het kustvak | vorm vakbeseining door het kustvak | groep"
                        + " uchar-feitcreatie",
                "  lezingen [0 CREATIEFASE $fase (creatiefase)]",
                "  onderwerp Kustvak $onderwerp alfa [] join [] beta [] bind []",
                "  patroon Creatiefase $creatiefase alfa [creatiestratum_14: [[] stratum WAARDE MINSTENS"
                        + " 0]] join [] beta [] na [] bind [$fase = [] Creatiefase.nummer WAARDE]",
                "  patroon Post $stap_keten_15 alfa [] join [] beta [veld-ver_16: ONTMOETING $onderwerp"
                        + " [kustvak] {List<String>, type null, bron null} [] {List<>, type null, bron null}] na"
                        + " [] bind []",
                "  patroon Sein $stap_keten_17 alfa [] join [] beta [veld-ver_18: ONTMOETING"
                        + " $stap_keten_15 [post] {List<String>, type null, bron null} [] {List<>, type null, bron"
                        + " null}] na [doel-gastheer_19: [[] null WAARDE IS_EEN Baken]] bind []",
                "  patroon KenmerkStand $doelstand_doel_20 alfa [doelstand_21: [[] kenmerk WAARDE GELIJK"
                        + " ElementSleutel[objecttype=baken, naam=gehesen, kenmerk=true]]] join"
                        + " [doelstand-doel_22: onderwerp = $stap_keten_17] beta [] na [doelstand-waar_23: [[]"
                        + " waar WAARDE GELIJK true]] bind []",
                "  niet binnen [$bestaand_feitcreatie_24] buiten [$onderwerp, $stap_keten_17] [Koppeling"
                        + " $bestaand_feitcreatie_24 alfa [bestaand_25: [[] relatie WAARDE GELIJK vakbeseining]]"
                        + " join [bestaand-anker_26: eerste = $onderwerp, bestaand-doel_27: tweede ="
                        + " $stap_keten_17] beta [] na [] bind []]",
                "  gevolg Koppeling[relatie=vakbeseining, eerste=$onderwerp, tweede=$stap_keten_17,"
                        + " fase=0, invoeging=GESTELD]",
                "regel koppeling vakbeseining uit de invoer: Sein.seinvak | vorm null | groep null",
                "  lezingen []",
                "  onderwerp Sein $houder alfa [koppeling-invoer_28: [[] seinvak WAARDE GEVULD]] join []"
                        + " beta [] bind [$ander = [] Sein.seinvak WAARDE]",
                "  gevolg Koppeling[relatie=vakbeseining, eerste=$ander, tweede=$houder, fase=-1,"
                        + " invoeging=LOGISCH]"),
                plannen(gehesenDoorEenRegel(), p -> p.gevolg() instanceof Gevolg.Koppeling));
    }

    @Test void de_padstanden_over_een_keten_met_een_gemaakte_link() throws Exception {
        assertEquals(List.of(
                "regel padstand Vondst.jutter.haven naar Haven.afgeleide openingsdag: link aanwezig |"
                        + " vorm null | groep null",
                "  lezingen [0 BEREIKTE_STAND $v (stand Vondst:jutter.haven"
                        + " ElementSleutel[objecttype=haven, naam=afgeleide openingsdag, kenmerk=false])]",
                "  onderwerp Vondst $onderwerp alfa [] join [] beta [] bind []",
                "  patroon Jutter $stap_keten_24 alfa [] join [] beta [veld-nabij_25: ONTMOETING"
                        + " $onderwerp [] {List<>, type null, bron null} [jutter] {List<String>, type null, bron"
                        + " null}] na [] bind []",
                "  patroon Koppeling $koppeling_keten_27 alfa [koppeling_29: [[] relatie WAARDE GELIJK"
                        + " havenheeftjutter]] join [koppeling-nabij_30: tweede = $stap_keten_24] beta [] na []"
                        + " bind [$overzijde_keten_28 = [] Koppeling.eerste WAARDE]",
                "  patroon Haven $stap_keten_26 alfa [] join [koppeling-ver_31: null ="
                        + " $overzijde_keten_28] beta [] na [] bind []",
                "  patroon Stand $stand alfa [stand_32: [[] attribuut WAARDE GELIJK"
                        + " ElementSleutel[objecttype=haven, naam=afgeleide openingsdag, kenmerk=false]]] join"
                        + " [stand-bereikt_33: onderwerp = $stap_keten_26] beta [] na [] bind [$v = []"
                        + " Stand.waarde WAARDE]",
                "  gevolg Padstand[onderwerp=$onderwerp, pad=Vondst:jutter.haven,"
                        + " lid=ElementSleutel[objecttype=haven, naam=afgeleide openingsdag, kenmerk=false],"
                        + " bron=BEREIKT, waarde=0, invoeging=LOGISCH]",
                "regel padstand Vondst.jutter.haven naar Haven.afgeleide openingsdag: link afwezig |"
                        + " vorm null | groep null",
                "  lezingen []",
                "  onderwerp Vondst $onderwerp alfa [] join [] beta [] bind []",
                "  niet binnen [$stap_keten_34, $koppeling_keten_37, $overzijde_keten_38,"
                        + " $stap_keten_36] buiten [$onderwerp] [Jutter $stap_keten_34 alfa [] join [] beta"
                        + " [veld-nabij_35: ONTMOETING $onderwerp [] {List<>, type null, bron null} [jutter]"
                        + " {List<String>, type null, bron null}] na [] bind [], Koppeling $koppeling_keten_37"
                        + " alfa [koppeling_39: [[] relatie WAARDE GELIJK havenheeftjutter]] join"
                        + " [koppeling-nabij_40: tweede = $stap_keten_34] beta [] na [] bind [$overzijde_keten_38"
                        + " = [] Koppeling.eerste WAARDE], Haven $stap_keten_36 alfa [] join [koppeling-ver_41:"
                        + " null = $overzijde_keten_38] beta [] na [] bind []]",
                "  gevolg Padstand[onderwerp=$onderwerp, pad=Vondst:jutter.haven,"
                        + " lid=ElementSleutel[objecttype=haven, naam=afgeleide openingsdag, kenmerk=false],"
                        + " bron=ONTBREKENDE_LINK, waarde=-1, invoeging=LOGISCH]",
                "regel padstand Vondst.jutter.haven naar Haven.openingsdag: link aanwezig | vorm null |"
                        + " groep null",
                "  lezingen [0 BEREIKTE_INVOER $v (invoer Vondst:jutter.haven"
                        + " ElementSleutel[objecttype=haven, naam=openingsdag, kenmerk=false])]",
                "  onderwerp Vondst $onderwerp alfa [] join [] beta [] bind []",
                "  patroon Jutter $stap_keten_42 alfa [] join [] beta [veld-nabij_43: ONTMOETING"
                        + " $onderwerp [] {List<>, type null, bron null} [jutter] {List<String>, type null, bron"
                        + " null}] na [] bind []",
                "  patroon Koppeling $koppeling_keten_45 alfa [koppeling_47: [[] relatie WAARDE GELIJK"
                        + " havenheeftjutter]] join [koppeling-nabij_48: tweede = $stap_keten_42] beta [] na []"
                        + " bind [$overzijde_keten_46 = [] Koppeling.eerste WAARDE]",
                "  patroon Haven $stap_keten_44 alfa [] join [koppeling-ver_49: null ="
                        + " $overzijde_keten_46] beta [] na [] bind [$v = [] Haven.openingsdag WAARDE]",
                "  gevolg Padstand[onderwerp=$onderwerp, pad=Vondst:jutter.haven,"
                        + " lid=ElementSleutel[objecttype=haven, naam=openingsdag, kenmerk=false], bron=BEREIKT,"
                        + " waarde=0, invoeging=LOGISCH]",
                "regel padstand Vondst.jutter.haven naar Haven.openingsdag: link afwezig | vorm null |"
                        + " groep null",
                "  lezingen []",
                "  onderwerp Vondst $onderwerp alfa [] join [] beta [] bind []",
                "  niet binnen [$stap_keten_50, $koppeling_keten_53, $overzijde_keten_54,"
                        + " $stap_keten_52] buiten [$onderwerp] [Jutter $stap_keten_50 alfa [] join [] beta"
                        + " [veld-nabij_51: ONTMOETING $onderwerp [] {List<>, type null, bron null} [jutter]"
                        + " {List<String>, type null, bron null}] na [] bind [], Koppeling $koppeling_keten_53"
                        + " alfa [koppeling_55: [[] relatie WAARDE GELIJK havenheeftjutter]] join"
                        + " [koppeling-nabij_56: tweede = $stap_keten_50] beta [] na [] bind [$overzijde_keten_54"
                        + " = [] Koppeling.eerste WAARDE], Haven $stap_keten_52 alfa [] join [koppeling-ver_57:"
                        + " null = $overzijde_keten_54] beta [] na [] bind []]",
                "  gevolg Padstand[onderwerp=$onderwerp, pad=Vondst:jutter.haven,"
                        + " lid=ElementSleutel[objecttype=haven, naam=openingsdag, kenmerk=false],"
                        + " bron=ONTBREKENDE_LINK, waarde=-1, invoeging=LOGISCH]"),
                plannen(golden("jutterswet-havenketen"), p -> p.gevolg() instanceof Gevolg.Padstand
                        && p.linkerkant().stream().anyMatch(CreatieplanWitnessTest::leestKoppelingen)));
    }

    @Test void een_feitcreatie_loopt_geen_link_van_haar_eigen_fase() throws Exception {
        assertEquals(List.of(
                "regel erf de directe opvolger | vorm erf de directe opvolger | groep uchar-feitcreatie",
                "  lezingen [0 CREATIEFASE $fase (creatiefase)]",
                "  onderwerp Wachter $onderwerp alfa [] join [] beta [] bind []",
                "  patroon Creatiefase $creatiefase alfa [creatiestratum_8: [[] stratum WAARDE MINSTENS"
                        + " 0]] join [] beta [] na [] bind [$fase = [] Creatiefase.nummer WAARDE]",
                "  patroon Wachter $stap_keten_9 alfa [] join [] beta [veld-nabij_10: ONTMOETING"
                        + " $onderwerp [] {List<>, type null, bron null} [opvolger] {List<String>, type null, bron"
                        + " null}] na [] bind []",
                "  niet binnen [$bestaand_feitcreatie_11] buiten [$onderwerp, $stap_keten_9] [Koppeling"
                        + " $bestaand_feitcreatie_11 alfa [bestaand_12: [[] relatie WAARDE GELIJK erflijn]] join"
                        + " [bestaand-anker_13: eerste = $onderwerp, bestaand-doel_14: tweede = $stap_keten_9]"
                        + " beta [] na [] bind []]",
                "  gevolg Koppeling[relatie=erflijn, eerste=$onderwerp, tweede=$stap_keten_9, fase=0,"
                        + " invoeging=GESTELD]",
                "regel erfestafette: erf de lijn door | vorm erf de lijn door | groep uchar-feitcreatie",
                "  lezingen [0 CREATIEFASE $fase (creatiefase)]",
                "  onderwerp Wachter $onderwerp alfa [] join [] beta [] bind []",
                "  patroon Creatiefase $creatiefase alfa [creatiestratum_15: [[] stratum WAARDE MINSTENS"
                        + " 1]] join [] beta [] na [] bind [$fase = [] Creatiefase.nummer WAARDE]",
                "  patroon Koppeling $koppeling_keten_17 alfa [koppeling_19: [[] relatie WAARDE GELIJK"
                        + " erflijn]] join [koppeling-nabij_20: eerste = $onderwerp] beta [koppeling-eerder_21:"
                        + " NIET_IN_FASE $fase] na [] bind [$overzijde_keten_18 = [] Koppeling.tweede WAARDE]",
                "  patroon Wachter $stap_keten_16 alfa [] join [koppeling-ver_22: null ="
                        + " $overzijde_keten_18] beta [] na [] bind []",
                "  patroon Wachter $stap_keten_23 alfa [] join [] beta [veld-nabij_24: ONTMOETING"
                        + " $stap_keten_16 [] {List<>, type null, bron null} [opvolger] {List<String>, type null,"
                        + " bron null}] na [] bind []",
                "  niet binnen [$bestaand_feitcreatie_25] buiten [$onderwerp, $stap_keten_23] [Koppeling"
                        + " $bestaand_feitcreatie_25 alfa [bestaand_26: [[] relatie WAARDE GELIJK erflijn]] join"
                        + " [bestaand-anker_27: eerste = $onderwerp, bestaand-doel_28: tweede = $stap_keten_23]"
                        + " beta [] na [] bind []]",
                "  gevolg Koppeling[relatie=erflijn, eerste=$onderwerp, tweede=$stap_keten_23, fase=0,"
                        + " invoeging=GESTELD]"),
                plannen(golden("anchor/jutterswet-erflijn"), p -> p.gevolg() instanceof Gevolg.Koppeling));
    }

    // ---------------- the construction before the insertion ----------------

    /**
     * The plan of a creation holds the order of its consequence: every initialisation is set on the new bean in its
     * construction where it is an attribute, or inserted as a characteristic after the bean where it is one, exactly
     * once, and the bean is the first insertion. The constructor refuses a plan that would write on a bean in working
     * memory, as a later setter after the insertion would, or that would insert a fact naming the bean before it.
     */
    @Test void het_plan_van_een_creatie_construeert_voor_het_invoegen() {
        ElementSleutel bedrag = ElementSleutel.van("Penning", "bedrag", false);
        ElementSleutel uitgereikt = ElementSleutel.van("Penning", "uitgereikt", true);
        List<Gevolg.Initialisatie> inits = List.of(new Gevolg.Initialisatie(bedrag, new Term.Lees(1, "bedrag")),
                new Gevolg.Initialisatie(uitgereikt, new Term.Const(true, null, "waar")));
        Gevolg.Constructie bouw = new Gevolg.Constructie(Object.class,
                List.of(new Gevolg.Constructie.Zetting(0, new Binding.Veld(Object.class, "bedrag"))));
        Gevolg.Invoegsel nieuw = new Gevolg.Invoegsel.Nieuw(Gevolg.Invoeging.GESTELD);
        Gevolg.Invoegsel merk = new Gevolg.Invoegsel.Merk(Gevolg.Invoeging.GESTELD);
        Gevolg.Invoegsel link = new Gevolg.Invoegsel.Link("uitgifte", true, Gevolg.Invoeging.GESTELD);
        Gevolg.Invoegsel kenmerk = new Gevolg.Invoegsel.Kenmerk(1, Gevolg.Invoeging.GESTELD);
        assertEquals(List.of("ok",
                        "een creatie voegt eerst het nieuwe object in: [Merk[invoeging=GESTELD],"
                                + " Nieuw[invoeging=GESTELD], Link[relatie=uitgifte, nieuwIsEerste=true,"
                                + " invoeging=GESTELD], Kenmerk[initialisatie=1, invoeging=GESTELD]]",
                        "een kenmerkinitialisatie wordt ingevoegd, niet gezet: " + uitgereikt,
                        "een attribuutinitialisatie wordt gezet, niet ingevoegd: " + bedrag,
                        "de initialisatie van " + uitgereikt + " komt 0 keer in de constructie of de invoegingen,"
                                + " niet één keer",
                        "een creatie voegt het nieuwe object, zijn merk en zijn koppeling elk één keer in:"
                                + " [Nieuw[invoeging=GESTELD], Merk[invoeging=GESTELD], Kenmerk[initialisatie=1,"
                                + " invoeging=GESTELD]]"),
                List.of(creatie(inits, bouw, List.of(nieuw, merk, link, kenmerk)),
                        creatie(inits, bouw, List.of(merk, nieuw, link, kenmerk)),
                        creatie(inits, new Gevolg.Constructie(Object.class, List.of(bouw.zettingen().get(0),
                                new Gevolg.Constructie.Zetting(1, new Binding.Veld(Object.class, "uitgereikt")))),
                                List.of(nieuw, merk, link, kenmerk)),
                        creatie(inits, new Gevolg.Constructie(Object.class, List.of()),
                                List.of(nieuw, merk, link, kenmerk, new Gevolg.Invoegsel.Kenmerk(0,
                                        Gevolg.Invoeging.GESTELD))),
                        creatie(inits, bouw, List.of(nieuw, merk, link)),
                        creatie(inits, bouw, List.of(nieuw, merk, kenmerk))));
    }

    /**
     * The constructor holds a creation to the order of its consequence before step 1c-C1 exactly (fix round 1c-C1): the
     * new object, its mark, its link, and then the characteristics in the order of their initialisations, with the
     * attributes set in that order too. A plan whose kinds and counts are right but whose order is another, which a
     * rendering would carry out as listed, is refused: the mark and the link swapped, two characteristics reversed, a
     * characteristic before the link, two attributes set in reverse.
     */
    @Test void de_creatie_houdt_de_volgorde_van_haar_gevolg() {
        ElementSleutel bedrag = ElementSleutel.van("Penning", "bedrag", false);
        ElementSleutel gewicht = ElementSleutel.van("Penning", "gewicht", false);
        ElementSleutel uitgereikt = ElementSleutel.van("Penning", "uitgereikt", true);
        ElementSleutel gemunt = ElementSleutel.van("Penning", "gemunt", true);
        List<Gevolg.Initialisatie> inits = List.of(new Gevolg.Initialisatie(bedrag, new Term.Lees(1, "bedrag")),
                new Gevolg.Initialisatie(uitgereikt, new Term.Const(true, null, "waar")),
                new Gevolg.Initialisatie(gemunt, new Term.Const(true, null, "waar")),
                new Gevolg.Initialisatie(gewicht, new Term.Lees(1, "gewicht")));
        Gevolg.Constructie.Zetting zetBedrag = new Gevolg.Constructie.Zetting(0, new Binding.Veld(Object.class, "bedrag"));
        Gevolg.Constructie.Zetting zetGewicht = new Gevolg.Constructie.Zetting(3,
                new Binding.Veld(Object.class, "gewicht"));
        Gevolg.Constructie bouw = new Gevolg.Constructie(Object.class, List.of(zetBedrag, zetGewicht));
        Gevolg.Invoegsel nieuw = new Gevolg.Invoegsel.Nieuw(Gevolg.Invoeging.GESTELD);
        Gevolg.Invoegsel merk = new Gevolg.Invoegsel.Merk(Gevolg.Invoeging.GESTELD);
        Gevolg.Invoegsel link = new Gevolg.Invoegsel.Link("uitgifte", true, Gevolg.Invoeging.GESTELD);
        Gevolg.Invoegsel eerste = new Gevolg.Invoegsel.Kenmerk(1, Gevolg.Invoeging.GESTELD);
        Gevolg.Invoegsel tweede = new Gevolg.Invoegsel.Kenmerk(2, Gevolg.Invoeging.GESTELD);
        String verwacht = "een creatie voegt het nieuwe object, zijn merk, zijn koppeling en dan haar"
                + " kenmerkinitialisaties in hun volgorde in, [Nieuw, Merk, Link, Kenmerk 1, Kenmerk 2], niet ";
        assertEquals(List.of("ok",
                        verwacht + "[Nieuw, Link, Merk, Kenmerk 1, Kenmerk 2]",
                        verwacht + "[Nieuw, Merk, Link, Kenmerk 2, Kenmerk 1]",
                        verwacht + "[Nieuw, Merk, Kenmerk 1, Link, Kenmerk 2]",
                        "een creatie zet haar attribuutinitialisaties in hun volgorde [0, 3], niet [3, 0]"),
                List.of(creatie(inits, bouw, List.of(nieuw, merk, link, eerste, tweede)),
                        creatie(inits, bouw, List.of(nieuw, link, merk, eerste, tweede)),
                        creatie(inits, bouw, List.of(nieuw, merk, link, tweede, eerste)),
                        creatie(inits, bouw, List.of(nieuw, merk, eerste, link, tweede)),
                        creatie(inits, new Gevolg.Constructie(Object.class, List.of(zetGewicht, zetBedrag)),
                                List.of(nieuw, merk, link, eerste, tweede))));
    }

    /** "ok" where the constructor of {@link Gevolg.Creatie} takes the plan, else its refusal. */
    private static String creatie(List<Gevolg.Initialisatie> inits, Gevolg.Constructie bouw,
                                  List<Gevolg.Invoegsel> invoegingen) {
        try {
            new Gevolg.Creatie("$onderwerp", "uitgifte", new Gevolg.Creatiediepte(0, 1, "Jutter", 100), inits,
                    new Gevolg.Foutnaam("uitgifte", "uitgifte"), Gevolg.Invoeging.GESTELD, bouw, 2, invoegingen,
                    List.of(0, 1, 2), 3);
            return "ok";
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    /**
     * The rendering follows the plan: in a fired case of {@code anchor/jutterswet-penninguitgifte} the new Penning
     * enters working memory with its bedrag set, the value of its attribute initialisation, and its characteristic
     * uitgereikt comes after it as a {@code HeeftKenmerk}; no bean is updated in working memory. The insertions follow
     * the plan's order: the bean, its {@code Gemaakt}, its {@code Koppeling}, then the characteristic. The dossier,
     * created without an initialisation, enters as it is made.
     */
    @Test void de_nieuwe_bean_komt_gevuld_in_het_werkgeheugen() throws Exception {
        Path map = GOLDEN.resolve("anchor/jutterswet-penninguitgifte");
        assertEquals(List.of(
                "bean Penning1 {getBedrag=12, getHouder=null, isUitgereikt=false}",
                "Gemaakt penning voor een gerechtigde jutter: J1 -> Penning1, fase 1, diepte 1",
                "Koppeling penningbezit: J1, Penning1, fase 1",
                "HeeftKenmerk Penning1 uitgereikt",
                "bean Dossier2 {getDossierhouder=null}",
                "Gemaakt dossier voor een gerechtigde jutter: J1 -> Dossier2, fase 1, diepte 1",
                "Koppeling dossiervoering: J1, Dossier2, fase 1"),
                invoegingen(map.resolve("ir.json"), JSON.readTree(map.resolve("case1.json").toFile())));
    }

    /** The insertions and updates of the session of {@code zaak} on {@code ir} after the input, in order: a bean a
     *  creation made with its attributes as they stand at its insertion, and each fact of a creation by its class and
     *  what it names. Through the measuring seam of {@code CaseRunner}, by reflection, as
     *  {@code VouwplanWitnessTest.filterfouten} reads its facts. */
    @SuppressWarnings("unchecked")
    private static List<String> invoegingen(Path ir, JsonNode zaak) throws Exception {
        Method voorMeting = CaseRunner.class.getDeclaredMethod("voorMeting", Path.class, DroolsBackend.Branch.class);
        voorMeting.setAccessible(true);
        Object runner = voorMeting.invoke(null, ir, DroolsBackend.Branch.EXEC_MODEL);
        Class<?> meting = Class.forName("dev.uchar.regelspraak.cases.CaseRunner$Sessiemeting");
        Object lezer = Proxy.newProxyInstance(meting.getClassLoader(), new Class<?>[]{meting}, (proxy, m, a) -> {
            if (!m.getName().equals("meet")) throw new UnsupportedOperationException(m.getName());
            DroolsBackend.Build bouw = (DroolsBackend.Build) a[0];
            KieSession sessie = (KieSession) a[1];
            Map<Object, String> label = new IdentityHashMap<>();
            ((Map<String, Object>) a[2]).forEach((l, boon) -> label.put(boon, l));
            List<String> uit = new ArrayList<>();
            Map<Object, String> gemaakt = new IdentityHashMap<>();
            sessie.addEventListener(new RuleRuntimeEventListener() {
                @Override public void objectInserted(ObjectInsertedEvent e) {
                    Object o = e.getObject();
                    // a bean of the model that is no input, and no §9.5 inconsistency, is one a creation made
                    if (o.getClass().getClassLoader() == bouw.beanLoader() && !label.containsKey(o)
                            && !o.getClass().getName().equals(TmsTranspiler.INCONSISTENTIE)) {
                        gemaakt.put(o, o.getClass().getSimpleName() + (gemaakt.size() + 1));
                        uit.add("bean " + gemaakt.get(o) + " " + velden(o, label, gemaakt, bouw.beanLoader()));
                    } else if (o instanceof Gemaakt g)
                        uit.add("Gemaakt " + g.regel() + ": " + naam(g.onderwerp(), label, gemaakt) + " -> "
                                + naam(g.object(), label, gemaakt) + ", fase " + g.fase() + ", diepte " + g.diepte());
                    else if (o instanceof Koppeling k && k.fase() > 0)
                        uit.add("Koppeling " + k.relatie() + ": " + naam(k.eerste(), label, gemaakt) + ", "
                                + naam(k.tweede(), label, gemaakt) + ", fase " + k.fase());
                    else if (o instanceof HeeftKenmerk h && gemaakt.containsKey(h.onderwerp()))
                        uit.add("HeeftKenmerk " + naam(h.onderwerp(), label, gemaakt) + " " + h.kenmerk().naam());
                }

                @Override public void objectUpdated(ObjectUpdatedEvent e) {
                    uit.add("bijgewerkt " + e.getObject());
                }

                @Override public void objectDeleted(ObjectDeletedEvent e) {}
            });
            DroolsBackend.Vuring vuring = DroolsBackend.vuur(bouw, sessie);
            if (vuring.geenFixpunt() != null) throw new AssertionError(vuring.geenFixpunt());
            return uit;
        });
        Method metSessie = CaseRunner.class.getDeclaredMethod("metSessie", JsonNode.class, meting);
        metSessie.setAccessible(true);
        return (List<String>) metSessie.invoke(runner, zaak, lezer);
    }

    private static String naam(Object o, Map<Object, String> label, Map<Object, String> gemaakt) {
        return label.containsKey(o) ? label.get(o) : gemaakt.getOrDefault(o, "?");
    }

    /** The properties of bean {@code o} that a getter without arguments gives, by getter name in name order, a bean
     *  among them by its label or the name it got here. */
    private static String velden(Object o, Map<Object, String> label, Map<Object, String> gemaakt, ClassLoader bonen) {
        java.util.TreeMap<String, Object> uit = new java.util.TreeMap<>();
        for (Method m : o.getClass().getMethods())
            if (m.getParameterCount() == 0 && m.getDeclaringClass() != Object.class
                    && (m.getName().startsWith("get") || m.getName().startsWith("is"))) {
                Object w;
                try {
                    w = m.invoke(o);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
                uit.put(m.getName(), w != null && w.getClass().getClassLoader() == bonen ? naam(w, label, gemaakt) : w);
            }
        return uit.toString();
    }

    // ---------------- the document of a characteristic a rule writes ----------------

    /** The case of {@link #gehesenDoorEenRegel}: P1, the post of V1, carries S1, S2 and S3. S1's vlaghoogte 5 makes it
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

    /** {@link #gehesenDoorEenRegel} fired: no document of the corpus or the suite has a fact creation whose guard reads
     *  the target's characteristic through its standing, so the harness does not watch the rendering of it. */
    @Test void de_feitcreatie_over_een_geschreven_kenmerk_vuurt(@TempDir Path map) throws Exception {
        Path ir = map.resolve("ir.json");
        Files.writeString(ir, JSON.writeValueAsString(gehesenDoorEenRegel()));
        assertNull(VouwplanWitnessTest.speelCase(ir, JSON.readTree(GEHESEN_CASE)));
    }

    /** {@code jutterswet-bakensein} with a vlaghoogte on Baken and a rule "Een Baken is gehesen indien zijn vlaghoogte
     *  groter is dan 2", so that a form writes the characteristic the fact creation's guard reads on its target. */
    static JsonNode gehesenDoorEenRegel() throws Exception {
        ObjectNode d = (ObjectNode) golden("jutterswet-bakensein");
        String getal = "{\"node\": \"Number\", \"whole\": true, \"decimals\": 0, \"sign\": \"nonNegative\"}";
        for (JsonNode ot : d.get("model").get("objectTypes"))
            if (ot.get("name").asText().equals("Baken"))
                ((ArrayNode) ot.get("attributes")).add(JSON.readTree("{\"node\": \"Attribute\", \"name\":"
                        + " \"vlaghoogte\", \"datatype\": " + getal + "}"));
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
    static List<String> plannen(JsonNode document, Predicate<Regelplan> keuze) {
        List<String> uit = new ArrayList<>();
        for (Regelplan p : DroolsBackend.build(document, DroolsBackend.Branch.EXEC_MODEL).execModelTranspiler()
                .modelplan().regels()) {
            if (!keuze.test(p)) continue;
            uit.add("regel " + p.naam() + " | vorm " + p.vorm() + " | groep " + p.agendagroep());
            List<String> slots = new ArrayList<>();
            for (Lezing l : p.lezingen().slots())
                slots.add(l.slot() + " " + l.soort() + " " + l.naam() + " (" + l.sleutel() + ")");
            uit.add("  lezingen " + slots);
            for (LhsItem item : p.linkerkant()) {
                uit.add("  " + item(item));
                if (item instanceof Toetsitem t) Termboom.velden("    toets", t.toets(), "0", uit);
            }
            gevolg(p.gevolg(), uit);
        }
        return uit;
    }

    private static String item(LhsItem item) {
        return switch (item) {
            case Onderwerp o -> "onderwerp " + o.bonenklasse().getSimpleName() + " " + o.naam() + " alfa "
                    + alfa(o.alfa()) + " join " + joins(o.joins()) + " beta " + beta(o.betatoetsen()) + " bind "
                    + bindingen(o.bindingen());
            case Patroon p -> "patroon " + patroon(p);
            case Niet n -> "niet binnen " + n.binnen() + " buiten " + n.buiten() + " "
                    + n.patronen().stream().map(CreatieplanWitnessTest::patroon).toList();
            case Bestaat b -> "bestaat binnen " + b.binnen() + " buiten " + b.buiten() + " "
                    + b.patronen().stream().map(CreatieplanWitnessTest::patroon).toList();
            case Accumulatie a -> "accumulatie " + patroon(a.bron()) + " functies "
                    + a.functies().stream().map(f -> f.soort() + " " + f.bron() + " -> " + f.doel()).toList();
            case Toetsitem t -> "toets " + t.id() + " " + t.rol() + " sonde " + t.sonde() + " slots " + t.slots()
                    + " grootte " + t.grootte() + ": " + Termboom.boom(t.toets());
        };
    }

    private static String patroon(Patroon p) {
        return p.feitklasse().getSimpleName() + " " + p.naam() + " alfa " + alfa(p.alfa()) + " join "
                + joins(p.joins()) + " beta " + beta(p.betatoetsen()) + " na " + alfa(p.naAlfa()) + " bind "
                + bindingen(p.bindingen());
    }

    /** Each constraint between two objects with its id, its function, the variable on the other side and each of its
     *  constants with every field. */
    private static List<String> beta(List<Betatoets> beta) {
        return beta.stream().map(t -> t.id() + ": " + t.functie() + " " + t.doel()
                + t.constanten().stream().map(c -> " " + constante(c)).reduce("", String::concat)).toList();
    }

    /** A constant of the analysis with every field: its value, the class of its value (a list by the classes of its
     *  elements), its IR type and its source fragment, each null where the constant has none. */
    private static String constante(Term.Const c) {
        return c.waarde() + " {" + klasse(c.waarde()) + ", type " + c.type() + ", bron " + c.bron() + "}";
    }

    private static String klasse(Object waarde) {
        if (waarde == null) return "null";
        if (waarde instanceof List<?> l)
            return "List<" + String.join(",", l.stream().map(e -> e == null ? "null" : e.getClass().getSimpleName())
                    .distinct().toList()) + ">";
        return waarde.getClass().getSimpleName();
    }

    private static List<String> joins(List<Join> joins) {
        return joins.stream().map(j -> j.id() + ": " + j.veld() + " = " + j.doel()
                + (j.doelveld() == null ? "" : "." + veld(j.doelveld()))).toList();
    }

    /** Each constraint on the object alone with its id and its field tests: the path, the field, the form of reading,
     *  the comparison, the value (a class by its name) and the class whose getter reads the field where the test
     *  names one. */
    private static List<String> alfa(List<Alfa> alfa) {
        List<String> uit = new ArrayList<>();
        for (Alfa a : alfa)
            uit.add(a.id() + ": " + a.toetsen().stream().map(t -> velden(t.pad()) + " " + t.veld() + " " + t.vorm()
                    + " " + t.op() + (t.waarde() == null ? "" : " " + (t.waarde() instanceof Class<?> k
                    ? k.getSimpleName() : t.waarde())) + (t.declarant() == null ? ""
                    : " via " + t.declarant().getSimpleName())).toList());
        return uit;
    }

    private static List<String> bindingen(List<Binding> bindingen) {
        return bindingen.stream().map(b -> b.doel() + " = " + velden(b.pad()) + " " + veld(b.veld()) + " " + b.vorm()
                + (b.soort() == null ? "" : " " + b.soort().getSimpleName())).toList();
    }

    private static String velden(List<Binding.Veld> pad) {
        return pad.stream().map(CreatieplanWitnessTest::veld).toList().toString();
    }

    private static String veld(Binding.Veld v) {
        return v.klasse().getSimpleName() + "." + v.naam();
    }

    /** The consequence {@code g} with every field; of a creation its depth, its construction, each initialisation with
     *  its term as a tree and node by node, and each insertion. */
    private static void gevolg(Gevolg g, List<String> uit) {
        switch (g) {
            case Gevolg.Creatie c -> {
                uit.add("  gevolg Creatie [onderwerp=" + c.onderwerp() + ", creatie=" + c.creatie() + ", diepte="
                        + c.diepte() + ", fout=" + c.fout() + ", foutinvoeging=" + c.foutinvoeging() + ", fase="
                        + c.fase() + ", slots=" + c.slots() + ", grootte=" + c.grootte() + "]");
                uit.add("    constructie " + c.constructie().klasse().getSimpleName() + " zettingen "
                        + c.constructie().zettingen().stream().map(z -> z.initialisatie() + " " + veld(z.veld()))
                        .toList());
                for (int i = 0; i < c.initialisaties().size(); i++) {
                    Gevolg.Initialisatie init = c.initialisaties().get(i);
                    uit.add("    initialisatie " + i + " " + init.lid() + ": " + Termboom.boom(init.waarde()));
                    Termboom.velden("      waarde", init.waarde(), "0", uit);
                }
                uit.add("    invoegingen " + c.invoegingen());
            }
            case Gevolg.Koppeling k -> uit.add("  gevolg " + k);
            case Gevolg.TweedeVulling t -> uit.add("  gevolg " + t);
            case Gevolg.Padstand s -> uit.add("  gevolg " + s);
            default -> throw new IllegalStateException("geen gevolg van een creatie of een koppeling: " + g);
        }
    }
}
