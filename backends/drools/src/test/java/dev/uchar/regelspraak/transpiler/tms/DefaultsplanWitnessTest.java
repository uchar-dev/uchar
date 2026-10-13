package dev.uchar.regelspraak.transpiler.tms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

import java.lang.reflect.RecordComponent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The rules of a defaults stack come whole from a plan (card drl-teksttak-op-truth-maintenance, step 1c-C2): the rule
 * of a default's own decision, its fault rule, the rules of a reconciliation (passing a member's conflict on, a
 * conflict of its own where two members decide differently, the agreement) and the rule that faults a session without
 * an evaluation date where a group of the stack is dated. Each is a {@link Regelplan} whose left-hand side holds what
 * the forms built themselves before, in its order and with its ids: the subject pattern restricted to exactly the
 * stack's owner, with the input test of an initialisation default; the patterns of the outcomes a reconciliation binds
 * and of those it waits on, with the constraint between two outcomes as a function of the plan; and the control-fact
 * patterns by which a lower default waits on the higher ones. Its consequence carries every decision as a field: the
 * decision's value as a term, with its fault where it is computed, and the facts it inserts with it
 * ({@link Gevolg.Afloop}), or a conflict's key and the fact it inserts with that ({@link Gevolg.Conflict}), each
 * fact a kind of its own ({@link Gevolg.Stapelfeit}).
 *
 * <p>This witness pins those plans as text, per plan in order: the name, the form and the group; the slots, each with
 * its kind, its variable and its key; every item of the left-hand side in order with its kind, its pattern class, its
 * constraint ids, each field test with the class of its value and the class it reads the field through where the plan
 * names one, its joins, its constraints between two objects with their function, the constraints after those, its
 * bindings, and of an existence or a negation the variables it declares and those it reads from outside; and the
 * consequence with every field. A term, the test of a rule or the value of a consequence, is pinned as a tree and on
 * the lines after it node by node with its source fragment and of a constant the class of its value and its whole IR
 * type ({@link Termboom#velden}).
 *
 * <ul>
 *   <li>{@code anchor/jutterswet-vondstwaardering}: the simple form on an attribute, three defaults, each inserting the
 *       control fact {@code Beslist} beside its value where a lower default has a rule, and each lower one waiting on
 *       the higher ones; the lowest an initialisation default, whose subject pattern tests the input on the target
 *       through the getter of the declaring type.</li>
 *   <li>{@link #uitsluitingGedateerd}, since no golden has a dated group: {@code anchor/jutterswet-uitsluiting} with its
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
 * <p>That the rendering of these plans builds the rules of before the step is what the harness {@code Vuurspoor}
 * shows, and the executable model of the corpus, which is equal as text; for {@link #uitsluitingGedateerd}, outside
 * the corpus, the executable model of before and after the step is equal as text as well. The constructors of the two
 * consequences keep a decision and a conflict apart ({@link #een_besluit_en_een_conflict_voegen_elk_hun_eigen_feiten_in}).
 *
 * <p><b>Red</b> before the step: the forms of the defaults built their rules themselves, so there was no plan of
 * them, and this class did not compile, since the plan had no consequence of a decision or a conflict of a stack;
 * with only the names of the plans pinned, none of the five documents had a plan of any of these rules.
 */
class DefaultsplanWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void de_eenvoudige_vorm_met_een_initialisatiedefault() throws Exception {
        assertEquals(List.of(
                "regel defaults A:Jutter.waardering: default 0 | vorm defaults A:Jutter.waardering |"
                        + " groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=jutter, naam=erkend,"
                        + " kenmerk=true]), 1 INVOER $lezing_1 (eigen ElementSleutel[objecttype=jutter,"
                        + " naam=basisbedrag, kenmerk=false])]",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_0: [[] null WAARDE KLASSE_IS Jutter]]"
                        + " join [] beta [] bind [$lezing_0 = [] Jutter.erkend WAAR, $lezing_1 = []"
                        + " Jutter.basisbedrag WAARDE]",
                "  toets voorwaarde_1 VOORWAARDE sonde -1 slots [0] grootte 2: KENMERK(Lees 0, Const"
                        + " true)",
                "    toets 0 Aanroep bron 'hij erkend is'",
                "    toets 0.0 Lees",
                "    toets 0.1 Const Boolean",
                "  gevolg Afloop [onderwerp=$onderwerp, waarde=Lees 1, slots=[1], grootte=2,"
                        + " collapse=true, fout=Foutnaam[vorm=defaults A:Jutter.waardering, regel=defaults"
                        + " A:Jutter.waardering], feiten=[Waarde[doel=ElementSleutel[objecttype=jutter,"
                        + " naam=waardering, kenmerk=false], schrijver=defaults A:Jutter.waardering],"
                        + " Beslist[stapel=defaults A:Jutter.waardering, index=0]], invoeging=LOGISCH]",
                "    waarde 0 Lees bron 'zijn basisbedrag'",
                "regel defaults A:Jutter.waardering: default 1 | vorm defaults A:Jutter.waardering |"
                        + " groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=jutter, naam=kandidaat,"
                        + " kenmerk=true]), 1 INVOER $lezing_1 (eigen ElementSleutel[objecttype=jutter,"
                        + " naam=kandidaatbedrag, kenmerk=false])]",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_2: [[] null WAARDE KLASSE_IS Jutter]]"
                        + " join [] beta [] bind [$lezing_0 = [] Jutter.kandidaat WAAR, $lezing_1 = []"
                        + " Jutter.kandidaatbedrag WAARDE]",
                "  niet binnen [$hoger_3] buiten [$onderwerp] [Beslist $hoger_3 alfa [hoger-beslist_3:"
                        + " [[] stapel WAARDE GELIJK defaults A:Jutter.waardering {String}, [] index WAARDE ONDER"
                        + " 1 {Integer}]] join [hoger-beslist-onderwerp_4: onderwerp = $onderwerp] beta [] na []"
                        + " bind []]",
                "  toets voorwaarde_5 VOORWAARDE sonde -1 slots [0] grootte 2: KENMERK(Lees 0, Const"
                        + " true)",
                "    toets 0 Aanroep bron 'hij kandidaat is'",
                "    toets 0.0 Lees",
                "    toets 0.1 Const Boolean",
                "  gevolg Afloop [onderwerp=$onderwerp, waarde=Lees 1, slots=[1], grootte=2,"
                        + " collapse=true, fout=Foutnaam[vorm=defaults A:Jutter.waardering, regel=defaults"
                        + " A:Jutter.waardering], feiten=[Waarde[doel=ElementSleutel[objecttype=jutter,"
                        + " naam=waardering, kenmerk=false], schrijver=defaults A:Jutter.waardering],"
                        + " Beslist[stapel=defaults A:Jutter.waardering, index=1]], invoeging=LOGISCH]",
                "    waarde 0 Lees bron 'zijn kandidaatbedrag'",
                "regel defaults A:Jutter.waardering: default 2 | vorm defaults A:Jutter.waardering |"
                        + " groep null",
                "  lezingen []",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_6: [[] null WAARDE KLASSE_IS Jutter],"
                        + " doel-leeg-in-de-invoer_7: [[] waardering WAARDE LEEG via Jutter]] join [] beta [] bind"
                        + " []",
                "  niet binnen [$hoger_8] buiten [$onderwerp] [Beslist $hoger_8 alfa [hoger-beslist_8:"
                        + " [[] stapel WAARDE GELIJK defaults A:Jutter.waardering {String}, [] index WAARDE ONDER"
                        + " 2 {Integer}]] join [hoger-beslist-onderwerp_9: onderwerp = $onderwerp] beta [] na []"
                        + " bind []]",
                "  gevolg Afloop [onderwerp=$onderwerp, waarde=Const 25, slots=[], grootte=0,"
                        + " collapse=true, fout=Foutnaam[vorm=defaults A:Jutter.waardering, regel=defaults"
                        + " A:Jutter.waardering], feiten=[Waarde[doel=ElementSleutel[objecttype=jutter,"
                        + " naam=waardering, kenmerk=false], schrijver=defaults A:Jutter.waardering]],"
                        + " invoeging=LOGISCH]",
                "    waarde 0 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '25'"), plannen(golden("anchor/jutterswet-vondstwaardering")));
    }

    @Test void de_eenvoudige_vorm_op_een_kenmerk_met_een_gedateerde_groep() throws Exception {
        assertEquals(List.of(
                "regel defaults K:Jutter.uitgesloten: default 0 | vorm defaults K:Jutter.uitgesloten |"
                        + " groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=jutter, naam=vergunning"
                        + " geweigerd, kenmerk=true]), 1 INVOER $lezing_1 (eigen ElementSleutel[objecttype=jutter,"
                        + " naam=weigering herroepen, kenmerk=true]), 2 INVOER $lezing_2 (eigen"
                        + " ElementSleutel[objecttype=jutter, naam=herroeping vervallen, kenmerk=true])]",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_3: [[] null WAARDE KLASSE_IS Jutter]]"
                        + " join [] beta [] bind [$lezing_0 = [] Jutter.vergunning geweigerd WAAR, $lezing_1 = []"
                        + " Jutter.weigering herroepen WAAR, $lezing_2 = [] Jutter.herroeping vervallen WAAR]",
                "  patroon Rekendatum $geldigheid_0 alfa [geldig_4: [[] datum WAARDE BINNEN"
                        + " Geldigheid[vanaf=2026-01-01, tot=null] {Geldigheid}]] join [] beta [] na [] bind []",
                "  toets voorwaarde_5 VOORWAARDE sonde -1 slots [0, 1, 2] grootte 3: Samengesteld ALLE"
                        + " 0(KENMERK(Lees 0, Const true), KENMERK(Lees 1, Const true), KENMERK(Lees 2, Const"
                        + " true))",
                "    toets 0 Samengesteld bron 'de weigering is herroepen maar de herroeping is"
                        + " vervallen'",
                "    toets 0.0 Aanroep bron 'zijn raapvergunning is geweigerd'",
                "    toets 0.0.0 Lees",
                "    toets 0.0.1 Const Boolean",
                "    toets 0.1 Aanroep bron 'de weigering is herroepen'",
                "    toets 0.1.0 Lees",
                "    toets 0.1.1 Const Boolean",
                "    toets 0.2 Aanroep bron 'de herroeping is vervallen'",
                "    toets 0.2.0 Lees",
                "    toets 0.2.1 Const Boolean",
                "  gevolg Afloop [onderwerp=$onderwerp, waarde=Const true, slots=[], grootte=3,"
                        + " collapse=false, fout=null, feiten=[Kenmerk[kenmerk=ElementSleutel[objecttype=jutter,"
                        + " naam=uitgesloten, kenmerk=true]], Beslist[stapel=defaults K:Jutter.uitgesloten,"
                        + " index=0]], invoeging=LOGISCH]",
                "    waarde 0 Const Boolean",
                "regel defaults K:Jutter.uitgesloten: default 1 | vorm defaults K:Jutter.uitgesloten |"
                        + " groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=jutter, naam=vergunning"
                        + " geweigerd, kenmerk=true]), 1 INVOER $lezing_1 (eigen ElementSleutel[objecttype=jutter,"
                        + " naam=weigering herroepen, kenmerk=true])]",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_6: [[] null WAARDE KLASSE_IS Jutter]]"
                        + " join [] beta [] bind [$lezing_0 = [] Jutter.vergunning geweigerd WAAR, $lezing_1 = []"
                        + " Jutter.weigering herroepen WAAR]",
                "  patroon Rekendatum $geldigheid_0 alfa [geldig_7: [[] datum WAARDE BINNEN"
                        + " Geldigheid[vanaf=2026-01-01, tot=null] {Geldigheid}]] join [] beta [] na [] bind []",
                "  niet binnen [$hoger_8] buiten [$onderwerp] [Beslist $hoger_8 alfa [hoger-beslist_8:"
                        + " [[] stapel WAARDE GELIJK defaults K:Jutter.uitgesloten {String}, [] index WAARDE ONDER"
                        + " 1 {Integer}]] join [hoger-beslist-onderwerp_9: onderwerp = $onderwerp] beta [] na []"
                        + " bind []]",
                "  toets voorwaarde_10 VOORWAARDE sonde -1 slots [0, 1] grootte 2: Samengesteld ALLE"
                        + " 0(KENMERK(Lees 0, Const true), KENMERK(Lees 1, Const true))",
                "    toets 0 Samengesteld bron 'de weigering is herroepen'",
                "    toets 0.0 Aanroep bron 'zijn raapvergunning is geweigerd'",
                "    toets 0.0.0 Lees",
                "    toets 0.0.1 Const Boolean",
                "    toets 0.1 Aanroep bron 'de weigering is herroepen'",
                "    toets 0.1.0 Lees",
                "    toets 0.1.1 Const Boolean",
                "  gevolg Afloop [onderwerp=$onderwerp, waarde=Const false, slots=[], grootte=2,"
                        + " collapse=false, fout=null, feiten=[Beslist[stapel=defaults K:Jutter.uitgesloten,"
                        + " index=1]], invoeging=LOGISCH]",
                "    waarde 0 Const Boolean",
                "regel defaults K:Jutter.uitgesloten: default 2 | vorm defaults K:Jutter.uitgesloten |"
                        + " groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=jutter, naam=vergunning"
                        + " geweigerd, kenmerk=true])]",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_11: [[] null WAARDE KLASSE_IS"
                        + " Jutter]] join [] beta [] bind [$lezing_0 = [] Jutter.vergunning geweigerd WAAR]",
                "  patroon Rekendatum $geldigheid_0 alfa [geldig_12: [[] datum WAARDE BINNEN"
                        + " Geldigheid[vanaf=2026-01-01, tot=null] {Geldigheid}]] join [] beta [] na [] bind []",
                "  niet binnen [$hoger_13] buiten [$onderwerp] [Beslist $hoger_13 alfa"
                        + " [hoger-beslist_13: [[] stapel WAARDE GELIJK defaults K:Jutter.uitgesloten {String}, []"
                        + " index WAARDE ONDER 2 {Integer}]] join [hoger-beslist-onderwerp_14: onderwerp ="
                        + " $onderwerp] beta [] na [] bind []]",
                "  toets voorwaarde_15 VOORWAARDE sonde -1 slots [0] grootte 1: KENMERK(Lees 0, Const"
                        + " true)",
                "    toets 0 Aanroep bron 'zijn raapvergunning is geweigerd'",
                "    toets 0.0 Lees",
                "    toets 0.1 Const Boolean",
                "  gevolg Afloop [onderwerp=$onderwerp, waarde=Const true, slots=[], grootte=1,"
                        + " collapse=false, fout=null, feiten=[Kenmerk[kenmerk=ElementSleutel[objecttype=jutter,"
                        + " naam=uitgesloten, kenmerk=true]]], invoeging=LOGISCH]",
                "    waarde 0 Const Boolean",
                "regel defaults K:Jutter.uitgesloten: geen rekendatum | vorm defaults"
                        + " K:Jutter.uitgesloten | groep null",
                "  lezingen []",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_16: [[] null WAARDE KLASSE_IS"
                        + " Jutter]] join [] beta [] bind []",
                "  patroon Rekendatum $rekendatum alfa [geen-rekendatum_17: [[] datum WAARDE LEEG]] join"
                        + " [] beta [] na [] bind []",
                "  gevolg Fout [onderwerp=$onderwerp, zin=a validity-bounded form needs an evaluation"
                        + " date; this session is time-free, fout=Foutnaam[vorm=defaults K:Jutter.uitgesloten,"
                        + " regel=defaults K:Jutter.uitgesloten], invoeging=LOGISCH]"), plannen(uitsluitingGedateerd()));
    }

    @Test void de_algemene_vorm_met_een_keten_en_haar_verzoeningen() throws Exception {
        assertEquals(List.of(
                "regel defaults A:Jutter.korting: default 0.0.0 | vorm defaults A:Jutter.korting | groep"
                        + " null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=jutter, naam=grondp,"
                        + " kenmerk=true]), 1 INVOER $lezing_1 (eigen ElementSleutel[objecttype=jutter,"
                        + " naam=grondslag, kenmerk=false])]",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_0: [[] null WAARDE KLASSE_IS Jutter]]"
                        + " join [] beta [] bind [$lezing_0 = [] Jutter.grondP WAAR, $lezing_1 = []"
                        + " Jutter.grondslag WAARDE]",
                "  toets voorwaarde_1 VOORWAARDE sonde -1 slots [0] grootte 2: KENMERK(Lees 0, Const"
                        + " true)",
                "    toets 0 Aanroep bron 'hij grondP heeft'",
                "    toets 0.0 Lees",
                "    toets 0.1 Const Boolean",
                "  gevolg Afloop [onderwerp=$onderwerp, waarde=RS_PLUS(Lees 1, Const 20), slots=[1],"
                        + " grootte=2, collapse=true, fout=Foutnaam[vorm=defaults A:Jutter.korting, regel=defaults"
                        + " A:Jutter.korting], feiten=[Uitkomst[stapel=defaults A:Jutter.korting, keten=1,"
                        + " positie=0, soort=BESLIST]], invoeging=LOGISCH]",
                "    waarde 0 Aanroep bron 'de grondslag plus 20.00'",
                "    waarde 0.0 Lees bron 'de grondslag van de Jutter'",
                "    waarde 0.1 Const BigDecimal type Numeriek[geheel=false, decimalen=2,"
                        + " teken=ONBEPAALD, eenheid=null] bron '20.00'",
                "regel defaults A:Jutter.korting: default 0.1.0 | vorm defaults A:Jutter.korting | groep"
                        + " null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=jutter, naam=grondq,"
                        + " kenmerk=true]), 1 INVOER $lezing_1 (eigen ElementSleutel[objecttype=jutter,"
                        + " naam=grondslag, kenmerk=false])]",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_2: [[] null WAARDE KLASSE_IS Jutter]]"
                        + " join [] beta [] bind [$lezing_0 = [] Jutter.grondQ WAAR, $lezing_1 = []"
                        + " Jutter.grondslag WAARDE]",
                "  toets voorwaarde_3 VOORWAARDE sonde -1 slots [0] grootte 2: KENMERK(Lees 0, Const"
                        + " true)",
                "    toets 0 Aanroep bron 'hij grondQ heeft'",
                "    toets 0.0 Lees",
                "    toets 0.1 Const Boolean",
                "  gevolg Afloop [onderwerp=$onderwerp, waarde=RS_PLUS(Lees 1, Const 35), slots=[1],"
                        + " grootte=2, collapse=true, fout=Foutnaam[vorm=defaults A:Jutter.korting, regel=defaults"
                        + " A:Jutter.korting], feiten=[Uitkomst[stapel=defaults A:Jutter.korting, keten=2,"
                        + " positie=0, soort=BESLIST]], invoeging=LOGISCH]",
                "    waarde 0 Aanroep bron 'de grondslag plus 35.00'",
                "    waarde 0.0 Lees bron 'de grondslag van de Jutter'",
                "    waarde 0.1 Const BigDecimal type Numeriek[geheel=false, decimalen=2,"
                        + " teken=ONBEPAALD, eenheid=null] bron '35.00'",
                "regel defaults A:Jutter.korting: default 0.2.0 | vorm defaults A:Jutter.korting | groep"
                        + " null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=jutter, naam=grondr,"
                        + " kenmerk=true]), 1 INVOER $lezing_1 (eigen ElementSleutel[objecttype=jutter,"
                        + " naam=grondslag, kenmerk=false])]",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_4: [[] null WAARDE KLASSE_IS Jutter]]"
                        + " join [] beta [] bind [$lezing_0 = [] Jutter.grondR WAAR, $lezing_1 = []"
                        + " Jutter.grondslag WAARDE]",
                "  toets voorwaarde_5 VOORWAARDE sonde -1 slots [0] grootte 2: KENMERK(Lees 0, Const"
                        + " true)",
                "    toets 0 Aanroep bron 'hij grondR heeft'",
                "    toets 0.0 Lees",
                "    toets 0.1 Const Boolean",
                "  gevolg Afloop [onderwerp=$onderwerp, waarde=RS_PLUS(RS_PLUS(Lees 1, Const 15), Const"
                        + " 5), slots=[1], grootte=2, collapse=true, fout=Foutnaam[vorm=defaults A:Jutter.korting,"
                        + " regel=defaults A:Jutter.korting], feiten=[Uitkomst[stapel=defaults A:Jutter.korting,"
                        + " keten=3, positie=0, soort=BESLIST]], invoeging=LOGISCH]",
                "    waarde 0 Aanroep bron 'die som plus 5.00'",
                "    waarde 0.0 Aanroep bron 'de grondslag plus 15.00'",
                "    waarde 0.0.0 Lees bron 'de grondslag van de Jutter'",
                "    waarde 0.0.1 Const BigDecimal type Numeriek[geheel=false, decimalen=2,"
                        + " teken=ONBEPAALD, eenheid=null] bron '15.00'",
                "    waarde 0.1 Const BigDecimal type Numeriek[geheel=false, decimalen=2,"
                        + " teken=ONBEPAALD, eenheid=null] bron '5.00'",
                "regel defaults A:Jutter.korting: verschil 0 | vorm defaults A:Jutter.korting | groep"
                        + " null",
                "  lezingen []",
                "  patroon Uitkomst $besluit alfa [lid-besluit_6: [[] stapel WAARDE GELIJK defaults"
                        + " A:Jutter.korting {String}, [] soort WAARDE GELIJK BESLIST {Soort}, [] keten WAARDE"
                        + " MINSTENS 1 {Integer}, [] keten WAARDE ONDER 4 {Integer}]] join [] beta [] na [] bind"
                        + " [$onderwerp = [] Uitkomst.onderwerp WAARDE]",
                "  niet binnen [$uitkomst_7] buiten [$onderwerp, $besluit] [Uitkomst $uitkomst_7 alfa"
                        + " [uitkomst_7: [[] stapel WAARDE GELIJK defaults A:Jutter.korting {String}, [] keten"
                        + " WAARDE MINSTENS 1 {Integer}, [] keten WAARDE ONDER 4 {Integer}, [] soort WAARDE GELIJK"
                        + " BESLIST {Soort}]] join [uitkomst-onderwerp_8: onderwerp = $onderwerp] beta"
                        + " [eerder-lid_9: EERDER_LID $besluit] na [] bind []]",
                "  bestaat binnen [$uitkomst_10] buiten [$onderwerp, $besluit] [Uitkomst $uitkomst_10"
                        + " alfa [uitkomst_10: [[] stapel WAARDE GELIJK defaults A:Jutter.korting {String}, []"
                        + " keten WAARDE MINSTENS 1 {Integer}, [] keten WAARDE ONDER 4 {Integer}, [] soort WAARDE"
                        + " GELIJK BESLIST {Soort}]] join [uitkomst-onderwerp_11: onderwerp = $onderwerp] beta"
                        + " [ander-besluit_12: ANDER_BESLUIT $besluit] na [] bind []]",
                "  gevolg Conflict [onderwerp=$onderwerp, sleutel=Const Jutter.korting/0, slots=[],"
                        + " grootte=0, feit=Uitkomst[stapel=defaults A:Jutter.korting, keten=0, positie=0,"
                        + " soort=CONFLICT], invoeging=LOGISCH]",
                "    sleutel 0 Const String",
                "regel defaults A:Jutter.korting: eens 0 | vorm defaults A:Jutter.korting | groep null",
                "  lezingen [0 BESLUIT $waarde (besluit)]",
                "  patroon Uitkomst $besluit alfa [lid-besluit_13: [[] stapel WAARDE GELIJK defaults"
                        + " A:Jutter.korting {String}, [] soort WAARDE GELIJK BESLIST {Soort}, [] keten WAARDE"
                        + " MINSTENS 1 {Integer}, [] keten WAARDE ONDER 4 {Integer}]] join [] beta [] na [] bind"
                        + " [$onderwerp = [] Uitkomst.onderwerp WAARDE, $waarde = [] Uitkomst.waarde WAARDE]",
                "  niet binnen [$uitkomst_14] buiten [$onderwerp, $besluit] [Uitkomst $uitkomst_14 alfa"
                        + " [uitkomst_14: [[] stapel WAARDE GELIJK defaults A:Jutter.korting {String}, [] keten"
                        + " WAARDE MINSTENS 1 {Integer}, [] keten WAARDE ONDER 4 {Integer}, [] soort WAARDE GELIJK"
                        + " BESLIST {Soort}]] join [uitkomst-onderwerp_15: onderwerp = $onderwerp] beta"
                        + " [eerder-lid_16: EERDER_LID $besluit] na [] bind []]",
                "  niet binnen [$uitkomst_17] buiten [$onderwerp, $besluit] [Uitkomst $uitkomst_17 alfa"
                        + " [uitkomst_17: [[] stapel WAARDE GELIJK defaults A:Jutter.korting {String}, [] keten"
                        + " WAARDE MINSTENS 1 {Integer}, [] keten WAARDE ONDER 4 {Integer}, [] soort WAARDE GELIJK"
                        + " BESLIST {Soort}]] join [uitkomst-onderwerp_18: onderwerp = $onderwerp] beta"
                        + " [ander-besluit_19: ANDER_BESLUIT $besluit] na [] bind []]",
                "  gevolg Afloop [onderwerp=$onderwerp, waarde=Lees 0, slots=[0], grootte=1,"
                        + " collapse=false, fout=null, feiten=[Uitkomst[stapel=defaults A:Jutter.korting, keten=0,"
                        + " positie=0, soort=BESLIST]], invoeging=LOGISCH]",
                "    waarde 0 Lees",
                "regel defaults A:Jutter.korting: terugval 0 | vorm defaults A:Jutter.korting | groep"
                        + " null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=jutter, naam=rechta,"
                        + " kenmerk=true]), 1 INVOER $lezing_1 (eigen ElementSleutel[objecttype=jutter,"
                        + " naam=grondslag, kenmerk=false])]",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_20: [[] null WAARDE KLASSE_IS"
                        + " Jutter]] join [] beta [] bind [$lezing_0 = [] Jutter.rechtA WAAR, $lezing_1 = []"
                        + " Jutter.grondslag WAARDE]",
                "  niet binnen [$uitkomst_21] buiten [$onderwerp] [Uitkomst $uitkomst_21 alfa"
                        + " [uitkomst_21: [[] stapel WAARDE GELIJK defaults A:Jutter.korting {String}, [] keten"
                        + " WAARDE MINSTENS 1 {Integer}, [] keten WAARDE ONDER 4 {Integer}]] join"
                        + " [uitkomst-onderwerp_22: onderwerp = $onderwerp] beta [] na [] bind []]",
                "  toets voorwaarde_23 VOORWAARDE sonde -1 slots [0] grootte 2: KENMERK(Lees 0, Const"
                        + " true)",
                "    toets 0 Aanroep bron 'hij rechtA heeft'",
                "    toets 0.0 Lees",
                "    toets 0.1 Const Boolean",
                "  gevolg Afloop [onderwerp=$onderwerp, waarde=RS_PLUS(Lees 1, Const 90), slots=[1],"
                        + " grootte=2, collapse=true, fout=Foutnaam[vorm=defaults A:Jutter.korting, regel=defaults"
                        + " A:Jutter.korting], feiten=[Uitkomst[stapel=defaults A:Jutter.korting, keten=0,"
                        + " positie=0, soort=BESLIST]], invoeging=LOGISCH]",
                "    waarde 0 Aanroep bron 'de grondslag plus 90.00'",
                "    waarde 0.0 Lees bron 'de grondslag van de Jutter'",
                "    waarde 0.1 Const BigDecimal type Numeriek[geheel=false, decimalen=2,"
                        + " teken=ONBEPAALD, eenheid=null] bron '90.00'",
                "regel defaults A:Jutter.korting: default 1 | vorm defaults A:Jutter.korting | groep null",
                "  lezingen []",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_24: [[] null WAARDE KLASSE_IS"
                        + " Jutter], doel-leeg-in-de-invoer_25: [[] korting WAARDE LEEG via Jutter]] join [] beta"
                        + " [] bind []",
                "  niet binnen [$hoger_26] buiten [$onderwerp] [Uitkomst $hoger_26 alfa"
                        + " [hoger-uitkomst_26: [[] stapel WAARDE GELIJK defaults A:Jutter.korting {String}, []"
                        + " keten WAARDE GELIJK 0 {Integer}, [] positie WAARDE ONDER 1 {Integer}]] join"
                        + " [hoger-uitkomst-onderwerp_27: onderwerp = $onderwerp] beta [] na [] bind []]",
                "  gevolg Afloop [onderwerp=$onderwerp, waarde=Const 5, slots=[], grootte=0,"
                        + " collapse=true, fout=Foutnaam[vorm=defaults A:Jutter.korting, regel=defaults"
                        + " A:Jutter.korting], feiten=[Uitkomst[stapel=defaults A:Jutter.korting, keten=0,"
                        + " positie=1, soort=BESLIST]], invoeging=LOGISCH]",
                "    waarde 0 Const BigDecimal type Numeriek[geheel=false, decimalen=2, teken=ONBEPAALD,"
                        + " eenheid=null] bron '5.00'",
                "regel defaults A:Jutter.korting: doorgeven wortel | vorm defaults A:Jutter.korting |"
                        + " groep null",
                "  lezingen [0 CONFLICTSLEUTEL $sleutel (conflictsleutel)]",
                "  patroon Uitkomst $conflict alfa [lid-conflict_28: [[] stapel WAARDE GELIJK defaults"
                        + " A:Jutter.korting {String}, [] soort WAARDE GELIJK CONFLICT {Soort}, [] keten WAARDE"
                        + " MINSTENS 0 {Integer}, [] keten WAARDE ONDER 1 {Integer}]] join [] beta [] na [] bind"
                        + " [$onderwerp = [] Uitkomst.onderwerp WAARDE, $sleutel = [] Uitkomst.sleutel WAARDE]",
                "  gevolg Conflict [onderwerp=$onderwerp, sleutel=Lees 0, slots=[0], grootte=1,"
                        + " feit=Inconsistentie[], invoeging=LOGISCH]",
                "    sleutel 0 Lees",
                "regel defaults A:Jutter.korting: eens wortel | vorm defaults A:Jutter.korting | groep"
                        + " null",
                "  lezingen [0 BESLUIT $waarde (besluit)]",
                "  patroon Uitkomst $besluit alfa [lid-besluit_29: [[] stapel WAARDE GELIJK defaults"
                        + " A:Jutter.korting {String}, [] soort WAARDE GELIJK BESLIST {Soort}, [] keten WAARDE"
                        + " MINSTENS 0 {Integer}, [] keten WAARDE ONDER 1 {Integer}]] join [] beta [] na [] bind"
                        + " [$onderwerp = [] Uitkomst.onderwerp WAARDE, $waarde = [] Uitkomst.waarde WAARDE]",
                "  gevolg Afloop [onderwerp=$onderwerp, waarde=Lees 0, slots=[0], grootte=1,"
                        + " collapse=false, fout=null, feiten=[Waarde[doel=ElementSleutel[objecttype=jutter,"
                        + " naam=korting, kenmerk=false], schrijver=defaults A:Jutter.korting]], invoeging=LOGISCH]",
                "    waarde 0 Lees"), plannen(golden("synth3-nestkorting")));
    }

    @Test void de_algemene_vorm_op_een_kenmerk() throws Exception {
        assertEquals(List.of(
                "regel defaults K:Jutter.vrijgesteld: default 0.0.0 | vorm defaults K:Jutter.vrijgesteld"
                        + " | groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=jutter, naam=nx,"
                        + " kenmerk=true]), 1 INVOER $lezing_1 (eigen ElementSleutel[objecttype=jutter, naam=ny,"
                        + " kenmerk=true])]",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_0: [[] null WAARDE KLASSE_IS Jutter]]"
                        + " join [] beta [] bind [$lezing_0 = [] Jutter.nx WAAR, $lezing_1 = [] Jutter.ny WAAR]",
                "  toets voorwaarde_1 VOORWAARDE sonde -1 slots [0, 1] grootte 2: Samengesteld ALLE"
                        + " 0(KENMERK(Lees 0, Const true), KENMERK(Lees 1, Const true))",
                "    toets 0 Samengesteld bron 'hij nx heeft en hij ny heeft'",
                "    toets 0.0 Aanroep bron 'hij nx heeft'",
                "    toets 0.0.0 Lees",
                "    toets 0.0.1 Const Boolean",
                "    toets 0.1 Aanroep bron 'hij ny heeft'",
                "    toets 0.1.0 Lees",
                "    toets 0.1.1 Const Boolean",
                "  gevolg Afloop [onderwerp=$onderwerp, waarde=Const false, slots=[], grootte=2,"
                        + " collapse=false, fout=null, feiten=[Uitkomst[stapel=defaults K:Jutter.vrijgesteld,"
                        + " keten=1, positie=0, soort=BESLIST]], invoeging=LOGISCH]",
                "    waarde 0 Const Boolean",
                "regel defaults K:Jutter.vrijgesteld: default 0.1.0 | vorm defaults K:Jutter.vrijgesteld"
                        + " | groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=jutter, naam=nx,"
                        + " kenmerk=true]), 1 INVOER $lezing_1 (eigen ElementSleutel[objecttype=jutter, naam=nz,"
                        + " kenmerk=true])]",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_2: [[] null WAARDE KLASSE_IS Jutter]]"
                        + " join [] beta [] bind [$lezing_0 = [] Jutter.nx WAAR, $lezing_1 = [] Jutter.nz WAAR]",
                "  toets voorwaarde_3 VOORWAARDE sonde -1 slots [0, 1] grootte 2: Samengesteld ALLE"
                        + " 0(KENMERK(Lees 0, Const true), KENMERK(Lees 1, Const true))",
                "    toets 0 Samengesteld bron 'hij nx heeft en hij nz heeft'",
                "    toets 0.0 Aanroep bron 'hij nx heeft'",
                "    toets 0.0.0 Lees",
                "    toets 0.0.1 Const Boolean",
                "    toets 0.1 Aanroep bron 'hij nz heeft'",
                "    toets 0.1.0 Lees",
                "    toets 0.1.1 Const Boolean",
                "  gevolg Afloop [onderwerp=$onderwerp, waarde=Const true, slots=[], grootte=2,"
                        + " collapse=false, fout=null, feiten=[Uitkomst[stapel=defaults K:Jutter.vrijgesteld,"
                        + " keten=2, positie=0, soort=BESLIST]], invoeging=LOGISCH]",
                "    waarde 0 Const Boolean",
                "regel defaults K:Jutter.vrijgesteld: verschil 0 | vorm defaults K:Jutter.vrijgesteld |"
                        + " groep null",
                "  lezingen []",
                "  patroon Uitkomst $besluit alfa [lid-besluit_4: [[] stapel WAARDE GELIJK defaults"
                        + " K:Jutter.vrijgesteld {String}, [] soort WAARDE GELIJK BESLIST {Soort}, [] keten WAARDE"
                        + " MINSTENS 1 {Integer}, [] keten WAARDE ONDER 3 {Integer}]] join [] beta [] na [] bind"
                        + " [$onderwerp = [] Uitkomst.onderwerp WAARDE]",
                "  niet binnen [$uitkomst_5] buiten [$onderwerp, $besluit] [Uitkomst $uitkomst_5 alfa"
                        + " [uitkomst_5: [[] stapel WAARDE GELIJK defaults K:Jutter.vrijgesteld {String}, [] keten"
                        + " WAARDE MINSTENS 1 {Integer}, [] keten WAARDE ONDER 3 {Integer}, [] soort WAARDE GELIJK"
                        + " BESLIST {Soort}]] join [uitkomst-onderwerp_6: onderwerp = $onderwerp] beta"
                        + " [eerder-lid_7: EERDER_LID $besluit] na [] bind []]",
                "  bestaat binnen [$uitkomst_8] buiten [$onderwerp, $besluit] [Uitkomst $uitkomst_8 alfa"
                        + " [uitkomst_8: [[] stapel WAARDE GELIJK defaults K:Jutter.vrijgesteld {String}, [] keten"
                        + " WAARDE MINSTENS 1 {Integer}, [] keten WAARDE ONDER 3 {Integer}, [] soort WAARDE GELIJK"
                        + " BESLIST {Soort}]] join [uitkomst-onderwerp_9: onderwerp = $onderwerp] beta"
                        + " [ander-besluit_10: ANDER_BESLUIT $besluit] na [] bind []]",
                "  gevolg Conflict [onderwerp=$onderwerp, sleutel=Const Jutter.vrijgesteld/0, slots=[],"
                        + " grootte=0, feit=Uitkomst[stapel=defaults K:Jutter.vrijgesteld, keten=0, positie=0,"
                        + " soort=CONFLICT], invoeging=LOGISCH]",
                "    sleutel 0 Const String",
                "regel defaults K:Jutter.vrijgesteld: eens 0 | vorm defaults K:Jutter.vrijgesteld |"
                        + " groep null",
                "  lezingen [0 BESLUIT $waarde (besluit)]",
                "  patroon Uitkomst $besluit alfa [lid-besluit_11: [[] stapel WAARDE GELIJK defaults"
                        + " K:Jutter.vrijgesteld {String}, [] soort WAARDE GELIJK BESLIST {Soort}, [] keten WAARDE"
                        + " MINSTENS 1 {Integer}, [] keten WAARDE ONDER 3 {Integer}]] join [] beta [] na [] bind"
                        + " [$onderwerp = [] Uitkomst.onderwerp WAARDE, $waarde = [] Uitkomst.waarde WAARDE]",
                "  niet binnen [$uitkomst_12] buiten [$onderwerp, $besluit] [Uitkomst $uitkomst_12 alfa"
                        + " [uitkomst_12: [[] stapel WAARDE GELIJK defaults K:Jutter.vrijgesteld {String}, []"
                        + " keten WAARDE MINSTENS 1 {Integer}, [] keten WAARDE ONDER 3 {Integer}, [] soort WAARDE"
                        + " GELIJK BESLIST {Soort}]] join [uitkomst-onderwerp_13: onderwerp = $onderwerp] beta"
                        + " [eerder-lid_14: EERDER_LID $besluit] na [] bind []]",
                "  niet binnen [$uitkomst_15] buiten [$onderwerp, $besluit] [Uitkomst $uitkomst_15 alfa"
                        + " [uitkomst_15: [[] stapel WAARDE GELIJK defaults K:Jutter.vrijgesteld {String}, []"
                        + " keten WAARDE MINSTENS 1 {Integer}, [] keten WAARDE ONDER 3 {Integer}, [] soort WAARDE"
                        + " GELIJK BESLIST {Soort}]] join [uitkomst-onderwerp_16: onderwerp = $onderwerp] beta"
                        + " [ander-besluit_17: ANDER_BESLUIT $besluit] na [] bind []]",
                "  gevolg Afloop [onderwerp=$onderwerp, waarde=Lees 0, slots=[0], grootte=1,"
                        + " collapse=false, fout=null, feiten=[Uitkomst[stapel=defaults K:Jutter.vrijgesteld,"
                        + " keten=0, positie=0, soort=BESLIST]], invoeging=LOGISCH]",
                "    waarde 0 Lees",
                "regel defaults K:Jutter.vrijgesteld: terugval 0 | vorm defaults K:Jutter.vrijgesteld |"
                        + " groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=jutter, naam=nx,"
                        + " kenmerk=true])]",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_18: [[] null WAARDE KLASSE_IS"
                        + " Jutter]] join [] beta [] bind [$lezing_0 = [] Jutter.nx WAAR]",
                "  niet binnen [$uitkomst_19] buiten [$onderwerp] [Uitkomst $uitkomst_19 alfa"
                        + " [uitkomst_19: [[] stapel WAARDE GELIJK defaults K:Jutter.vrijgesteld {String}, []"
                        + " keten WAARDE MINSTENS 1 {Integer}, [] keten WAARDE ONDER 3 {Integer}]] join"
                        + " [uitkomst-onderwerp_20: onderwerp = $onderwerp] beta [] na [] bind []]",
                "  toets voorwaarde_21 VOORWAARDE sonde -1 slots [0] grootte 1: KENMERK(Lees 0, Const"
                        + " true)",
                "    toets 0 Aanroep bron 'hij nx heeft'",
                "    toets 0.0 Lees",
                "    toets 0.1 Const Boolean",
                "  gevolg Afloop [onderwerp=$onderwerp, waarde=Const true, slots=[], grootte=1,"
                        + " collapse=false, fout=null, feiten=[Uitkomst[stapel=defaults K:Jutter.vrijgesteld,"
                        + " keten=0, positie=0, soort=BESLIST]], invoeging=LOGISCH]",
                "    waarde 0 Const Boolean",
                "regel defaults K:Jutter.vrijgesteld: doorgeven wortel | vorm defaults"
                        + " K:Jutter.vrijgesteld | groep null",
                "  lezingen [0 CONFLICTSLEUTEL $sleutel (conflictsleutel)]",
                "  patroon Uitkomst $conflict alfa [lid-conflict_22: [[] stapel WAARDE GELIJK defaults"
                        + " K:Jutter.vrijgesteld {String}, [] soort WAARDE GELIJK CONFLICT {Soort}, [] keten"
                        + " WAARDE MINSTENS 0 {Integer}, [] keten WAARDE ONDER 1 {Integer}]] join [] beta [] na []"
                        + " bind [$onderwerp = [] Uitkomst.onderwerp WAARDE, $sleutel = [] Uitkomst.sleutel WAARDE]",
                "  gevolg Conflict [onderwerp=$onderwerp, sleutel=Lees 0, slots=[0], grootte=1,"
                        + " feit=Inconsistentie[], invoeging=LOGISCH]",
                "    sleutel 0 Lees",
                "regel defaults K:Jutter.vrijgesteld: eens wortel | vorm defaults K:Jutter.vrijgesteld |"
                        + " groep null",
                "  lezingen [0 BESLUIT $waarde (besluit)]",
                "  patroon Uitkomst $besluit alfa [lid-besluit_23: [[] stapel WAARDE GELIJK defaults"
                        + " K:Jutter.vrijgesteld {String}, [] soort WAARDE GELIJK BESLIST {Soort}, [] keten WAARDE"
                        + " MINSTENS 0 {Integer}, [] keten WAARDE ONDER 1 {Integer}, [] waarde WAARDE GELIJK true"
                        + " {Boolean}]] join [] beta [] na [] bind [$onderwerp = [] Uitkomst.onderwerp WAARDE,"
                        + " $waarde = [] Uitkomst.waarde WAARDE]",
                "  gevolg Afloop [onderwerp=$onderwerp, waarde=Lees 0, slots=[0], grootte=1,"
                        + " collapse=false, fout=null, feiten=[Kenmerk[kenmerk=ElementSleutel[objecttype=jutter,"
                        + " naam=vrijgesteld, kenmerk=true]]], invoeging=LOGISCH]",
                "    waarde 0 Lees"), plannen(golden("anchor/jutterswet-nestkenmerk")));
    }

    @Test void de_algemene_vorm_met_meer_groepen_aan_de_wortel() throws Exception {
        assertEquals(List.of(
                "regel defaults A:Jutter.wachtgeld: default 0:0 | vorm defaults A:Jutter.wachtgeld |"
                        + " groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=jutter, naam=kustwacht,"
                        + " kenmerk=true])]",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_0: [[] null WAARDE KLASSE_IS Jutter]]"
                        + " join [] beta [] bind [$lezing_0 = [] Jutter.kustwacht WAAR]",
                "  toets voorwaarde_1 VOORWAARDE sonde -1 slots [0] grootte 1: KENMERK(Lees 0, Const"
                        + " true)",
                "    toets 0 Aanroep bron 'hij kustwacht is'",
                "    toets 0.0 Lees",
                "    toets 0.1 Const Boolean",
                "  gevolg Afloop [onderwerp=$onderwerp, waarde=Const 30, slots=[], grootte=1,"
                        + " collapse=true, fout=Foutnaam[vorm=defaults A:Jutter.wachtgeld, regel=defaults"
                        + " A:Jutter.wachtgeld], feiten=[Uitkomst[stapel=defaults A:Jutter.wachtgeld, keten=0,"
                        + " positie=0, soort=BESLIST]], invoeging=LOGISCH]",
                "    waarde 0 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '30'",
                "regel defaults A:Jutter.wachtgeld: default 1:0 | vorm defaults A:Jutter.wachtgeld |"
                        + " groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=jutter,"
                        + " naam=havenmeester, kenmerk=true])]",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_2: [[] null WAARDE KLASSE_IS Jutter]]"
                        + " join [] beta [] bind [$lezing_0 = [] Jutter.havenmeester WAAR]",
                "  toets voorwaarde_3 VOORWAARDE sonde -1 slots [0] grootte 1: KENMERK(Lees 0, Const"
                        + " true)",
                "    toets 0 Aanroep bron 'hij havenmeester is'",
                "    toets 0.0 Lees",
                "    toets 0.1 Const Boolean",
                "  gevolg Afloop [onderwerp=$onderwerp, waarde=Const 45, slots=[], grootte=1,"
                        + " collapse=true, fout=Foutnaam[vorm=defaults A:Jutter.wachtgeld, regel=defaults"
                        + " A:Jutter.wachtgeld], feiten=[Uitkomst[stapel=defaults A:Jutter.wachtgeld, keten=1,"
                        + " positie=0, soort=BESLIST]], invoeging=LOGISCH]",
                "    waarde 0 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '45'",
                "regel defaults A:Jutter.wachtgeld: default 2:0 | vorm defaults A:Jutter.wachtgeld |"
                        + " groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=jutter,"
                        + " naam=strandvoogd, kenmerk=true])]",
                "  onderwerp Jutter $onderwerp alfa [concreet-type_4: [[] null WAARDE KLASSE_IS Jutter]]"
                        + " join [] beta [] bind [$lezing_0 = [] Jutter.strandvoogd WAAR]",
                "  toets voorwaarde_5 VOORWAARDE sonde -1 slots [0] grootte 1: KENMERK(Lees 0, Const"
                        + " true)",
                "    toets 0 Aanroep bron 'hij strandvoogd is'",
                "    toets 0.0 Lees",
                "    toets 0.1 Const Boolean",
                "  gevolg Afloop [onderwerp=$onderwerp, waarde=Const 30, slots=[], grootte=1,"
                        + " collapse=true, fout=Foutnaam[vorm=defaults A:Jutter.wachtgeld, regel=defaults"
                        + " A:Jutter.wachtgeld], feiten=[Uitkomst[stapel=defaults A:Jutter.wachtgeld, keten=2,"
                        + " positie=0, soort=BESLIST]], invoeging=LOGISCH]",
                "    waarde 0 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '30'",
                "regel defaults A:Jutter.wachtgeld: verschil wortel | vorm defaults A:Jutter.wachtgeld |"
                        + " groep null",
                "  lezingen []",
                "  patroon Uitkomst $besluit alfa [lid-besluit_6: [[] stapel WAARDE GELIJK defaults"
                        + " A:Jutter.wachtgeld {String}, [] soort WAARDE GELIJK BESLIST {Soort}, [] keten WAARDE"
                        + " MINSTENS 0 {Integer}, [] keten WAARDE ONDER 3 {Integer}]] join [] beta [] na [] bind"
                        + " [$onderwerp = [] Uitkomst.onderwerp WAARDE]",
                "  niet binnen [$uitkomst_7] buiten [$onderwerp, $besluit] [Uitkomst $uitkomst_7 alfa"
                        + " [uitkomst_7: [[] stapel WAARDE GELIJK defaults A:Jutter.wachtgeld {String}, [] keten"
                        + " WAARDE MINSTENS 0 {Integer}, [] keten WAARDE ONDER 3 {Integer}, [] soort WAARDE GELIJK"
                        + " BESLIST {Soort}]] join [uitkomst-onderwerp_8: onderwerp = $onderwerp] beta"
                        + " [eerder-lid_9: EERDER_LID $besluit] na [] bind []]",
                "  bestaat binnen [$uitkomst_10] buiten [$onderwerp, $besluit] [Uitkomst $uitkomst_10"
                        + " alfa [uitkomst_10: [[] stapel WAARDE GELIJK defaults A:Jutter.wachtgeld {String}, []"
                        + " keten WAARDE MINSTENS 0 {Integer}, [] keten WAARDE ONDER 3 {Integer}, [] soort WAARDE"
                        + " GELIJK BESLIST {Soort}]] join [uitkomst-onderwerp_11: onderwerp = $onderwerp] beta"
                        + " [ander-besluit_12: ANDER_BESLUIT $besluit] na [] bind []]",
                "  gevolg Conflict [onderwerp=$onderwerp, sleutel=Const Jutter.wachtgeld, slots=[],"
                        + " grootte=0, feit=Inconsistentie[], invoeging=LOGISCH]",
                "    sleutel 0 Const String",
                "regel defaults A:Jutter.wachtgeld: eens wortel | vorm defaults A:Jutter.wachtgeld |"
                        + " groep null",
                "  lezingen [0 BESLUIT $waarde (besluit)]",
                "  patroon Uitkomst $besluit alfa [lid-besluit_13: [[] stapel WAARDE GELIJK defaults"
                        + " A:Jutter.wachtgeld {String}, [] soort WAARDE GELIJK BESLIST {Soort}, [] keten WAARDE"
                        + " MINSTENS 0 {Integer}, [] keten WAARDE ONDER 3 {Integer}]] join [] beta [] na [] bind"
                        + " [$onderwerp = [] Uitkomst.onderwerp WAARDE, $waarde = [] Uitkomst.waarde WAARDE]",
                "  niet binnen [$uitkomst_14] buiten [$onderwerp, $besluit] [Uitkomst $uitkomst_14 alfa"
                        + " [uitkomst_14: [[] stapel WAARDE GELIJK defaults A:Jutter.wachtgeld {String}, [] keten"
                        + " WAARDE MINSTENS 0 {Integer}, [] keten WAARDE ONDER 3 {Integer}, [] soort WAARDE GELIJK"
                        + " BESLIST {Soort}]] join [uitkomst-onderwerp_15: onderwerp = $onderwerp] beta"
                        + " [eerder-lid_16: EERDER_LID $besluit] na [] bind []]",
                "  niet binnen [$uitkomst_17] buiten [$onderwerp, $besluit] [Uitkomst $uitkomst_17 alfa"
                        + " [uitkomst_17: [[] stapel WAARDE GELIJK defaults A:Jutter.wachtgeld {String}, [] keten"
                        + " WAARDE MINSTENS 0 {Integer}, [] keten WAARDE ONDER 3 {Integer}, [] soort WAARDE GELIJK"
                        + " BESLIST {Soort}]] join [uitkomst-onderwerp_18: onderwerp = $onderwerp] beta"
                        + " [ander-besluit_19: ANDER_BESLUIT $besluit] na [] bind []]",
                "  gevolg Afloop [onderwerp=$onderwerp, waarde=Lees 0, slots=[0], grootte=1,"
                        + " collapse=false, fout=null, feiten=[Waarde[doel=ElementSleutel[objecttype=jutter,"
                        + " naam=wachtgeld, kenmerk=false], schrijver=defaults A:Jutter.wachtgeld]],"
                        + " invoeging=LOGISCH]",
                "    waarde 0 Lees"), plannen(golden("jutterswet-wachtgeld")));
    }

    /**
     * A decision inserts the target or its control fact with the value it decided, and a conflict inserts its outcome
     * of the kind {@code CONFLICT} or, at the root, the inconsistency, with its key: the constructors refuse a plan that
     * mixes them, which a rendering would carry out as listed, a key written as a value or a value as a key.
     */
    @Test void een_besluit_en_een_conflict_voegen_elk_hun_eigen_feiten_in() {
        ElementSleutel doel = ElementSleutel.van("Jutter", "korting", false);
        Gevolg.Stapelfeit waarde = new Gevolg.Stapelfeit.Waarde(doel, "defaults A:Jutter.korting");
        Gevolg.Stapelfeit beslist = new Gevolg.Stapelfeit.Uitkomst("defaults A:Jutter.korting", 0, 0,
                Uitkomst.Soort.BESLIST);
        Gevolg.Stapelfeit conflict = new Gevolg.Stapelfeit.Uitkomst("defaults A:Jutter.korting", 0, 0,
                Uitkomst.Soort.CONFLICT);
        Gevolg.Stapelfeit inconsistentie = new Gevolg.Stapelfeit.Inconsistentie();
        assertEquals(List.of("ok", "ok",
                        "een besluit voegt ten minste één feit in",
                        "een besluit voegt geen conflict in: " + conflict,
                        "een besluit voegt geen conflict in: Inconsistentie[]",
                        "ok", "ok",
                        "een conflict voegt zijn uitkomst of de inconsistentie in, niet " + beslist,
                        "een conflict voegt zijn uitkomst of de inconsistentie in, niet " + waarde),
                List.of(besluit(List.of(waarde)), besluit(List.of(beslist)), besluit(List.of()),
                        besluit(List.of(waarde, conflict)), besluit(List.of(inconsistentie)),
                        conflict(conflict), conflict(inconsistentie), conflict(beslist), conflict(waarde)));
    }

    /** "ok" where the constructor of {@link Gevolg.Afloop} takes a decision that inserts {@code feiten}, else its
     *  refusal. */
    private static String besluit(List<Gevolg.Stapelfeit> feiten) {
        try {
            new Gevolg.Afloop("$onderwerp", new Term.Lees(0, null), List.of(0), 1, false, null, feiten,
                    Gevolg.Invoeging.LOGISCH);
            return "ok";
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    /** "ok" where the constructor of {@link Gevolg.Conflict} takes a conflict that inserts {@code feit}, else its
     *  refusal. */
    private static String conflict(Gevolg.Stapelfeit feit) {
        try {
            new Gevolg.Conflict("$onderwerp", new Term.Lees(0, null), List.of(0), 1, feit, Gevolg.Invoeging.LOGISCH);
            return "ok";
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    /** {@code anchor/jutterswet-uitsluiting} with its defaults group valid from 2026-01-01, since no golden has a group
     *  with a dated validity. Verifier-clean. */
    static JsonNode uitsluitingGedateerd() throws Exception {
        JsonNode d = golden("anchor/jutterswet-uitsluiting");
        JsonNode groep = d.get("rules").get("prioritizedDefaults").get(0);
        ((ObjectNode) groep.get("validity")).put("from", "2026-01-01");
        return d;
    }

    static JsonNode golden(String naam) throws Exception {
        return JSON.readTree(GOLDEN.resolve(naam).resolve("ir.json").toFile());
    }

    // ---------------- the plans as text ----------------

    /** Every plan of a rule of a defaults stack of {@code document}, in the order of the model, as text; after
     *  a line that holds a term, the term node by node. */
    static List<String> plannen(JsonNode document) {
        List<String> uit = new ArrayList<>();
        for (Regelplan p : DroolsBackend.build(document, DroolsBackend.Branch.EXEC_MODEL).execModelTranspiler()
                .modelplan().regels()) {
            if (p.vorm() == null || !p.vorm().startsWith("defaults ")) continue;
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
            try {
                for (RecordComponent c : p.gevolg().getClass().getRecordComponents())
                    if (c.getAccessor().invoke(p.gevolg()) instanceof Term t)
                        Termboom.velden("    " + c.getName(), t, "0", uit);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
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
                    + n.patronen().stream().map(DefaultsplanWitnessTest::patroon).toList();
            case Bestaat b -> "bestaat binnen " + b.binnen() + " buiten " + b.buiten() + " "
                    + b.patronen().stream().map(DefaultsplanWitnessTest::patroon).toList();
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
     *  constants with its value and its class. */
    private static List<String> beta(List<Betatoets> beta) {
        return beta.stream().map(t -> t.id() + ": " + t.functie() + " " + t.doel()
                + t.constanten().stream().map(c -> " " + waarde(c.waarde())).reduce("", String::concat)).toList();
    }

    private static List<String> joins(List<Join> joins) {
        return joins.stream().map(j -> j.id() + ": " + j.veld() + " = " + j.doel()
                + (j.doelveld() == null ? "" : "." + veld(j.doelveld()))).toList();
    }

    /** Each constraint on the object alone with its id and its field tests: the path, the field, the form of reading,
     *  the comparison, the value with its class (a class by its name), and the class whose getter reads the field
     *  where the test names one. */
    private static List<String> alfa(List<Alfa> alfa) {
        List<String> uit = new ArrayList<>();
        for (Alfa a : alfa)
            uit.add(a.id() + ": " + a.toetsen().stream().map(t -> velden(t.pad()) + " " + t.veld() + " " + t.vorm()
                    + " " + t.op() + (t.waarde() == null ? "" : " " + waarde(t.waarde()))
                    + (t.declarant() == null ? "" : " via " + t.declarant().getSimpleName())).toList());
        return uit;
    }

    /** A value of the analysis with its class: a class by its name, anything else by its text and the simple name of
     *  its class, so that an {@code Integer} and a {@code Long} of one number differ. */
    private static String waarde(Object w) {
        if (w instanceof Class<?> k) return k.getSimpleName();
        return w + " {" + w.getClass().getSimpleName() + "}";
    }

    private static List<String> bindingen(List<Binding> bindingen) {
        return bindingen.stream().map(b -> b.doel() + " = " + velden(b.pad()) + " " + veld(b.veld()) + " " + b.vorm()
                + (b.soort() == null ? "" : " " + b.soort().getSimpleName())).toList();
    }

    private static String velden(List<Binding.Veld> pad) {
        return pad.stream().map(DefaultsplanWitnessTest::veld).toList().toString();
    }

    private static String veld(Binding.Veld v) {
        return v.klasse().getSimpleName() + "." + v.naam();
    }

    /** The kind of {@code g} and every field of it, a term as a tree. */
    private static String gevolg(Gevolg g) {
        List<String> velden = new ArrayList<>();
        try {
            for (RecordComponent c : g.getClass().getRecordComponents()) {
                Object w = c.getAccessor().invoke(g);
                velden.add(c.getName() + "=" + (w instanceof Term t ? Termboom.boom(t) : String.valueOf(w)));
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return g.getClass().getSimpleName() + " " + velden;
    }
}
