package dev.uchar.regelspraak.transpiler.tms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.uchar.regelspraak.cases.CaseRunner;
import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.transpiler.tms.plan.Accumulatie;
import dev.uchar.regelspraak.transpiler.tms.plan.Alfa;
import dev.uchar.regelspraak.transpiler.tms.plan.Bestaat;
import dev.uchar.regelspraak.transpiler.tms.plan.Betatoets;
import dev.uchar.regelspraak.transpiler.tms.plan.Binding;
import dev.uchar.regelspraak.transpiler.tms.plan.Gevolg;
import dev.uchar.regelspraak.transpiler.tms.plan.Join;
import dev.uchar.regelspraak.transpiler.tms.plan.Leestabel;
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
 * and of a member whose timeline the case gives as the asOfDate-bucket scalar. Each is a {@link Regelplan} whose
 * left-hand side holds what the form built itself before: the pattern of the fold's subject before the member's, the
 * join of the range on the member's pattern or the meeting of the two walks of a range of more steps, the existence of
 * a link of a relation a form creates or of the intermediates of a range of more steps, with its scope, the
 * intermediates bound per route and the accumulates of the members' places; and whose consequence carries every
 * decision as a field: what the member contributes ({@link Gevolg.Inbreng}), the member's place in the fold's order
 * ({@link Gevolg.Vouwplaats}), the probe, and what the routes of the plan compute. This witness pins those plans as
 * text, per plan in order: the name, the form and the group; the slots, each with its kind, its variable and its key;
 * every item of the left-hand side in order with its kind, its pattern class, its constraint ids, each field test, its
 * joins, its constraints between two objects with their function and each of their constants with its value, the class
 * of its value, its IR type and its source fragment, its bindings, and of an existence the variables it declares and
 * those it reads from outside; and the kind of the consequence with every field. A term, the
 * test of a rule or the matrix or the filter of a consequence, is pinned as a tree and on the lines after it node by
 * node with its source fragment and of a constant the class of its value and its whole IR type
 * ({@link Termboom#velden}).
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
 *       each route's {@link Filterfout} carries its own place and the rule names the first route's fault.</li>
 *   <li>The document of {@code TmsKwantorvouwWitnessTest} whose matrix divides ({@link #matrixDieDeelt}), since no
 *       golden has one: a quantifier fold whose matrix can fault and deposit, so a member contributes its test, with
 *       its place and the probe.</li>
 * </ul>
 *
 * <p>That the rendering of these plans builds the rules of before the step is what the harness {@code Vuurspoor}
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
 * existence cover what its patterns bind ({@link #de_scope_van_een_bestaan_omvat_de_bindingen_van_zijn_patronen}).
 */
class VouwplanWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void een_telling_en_een_som_over_een_koppeling() throws Exception {
        assertEquals(List.of(
                "regel seinaantal van een post: bijdrage aan vouw 0 | vorm seinaantal van een post |"
                        + " groep null",
                "  lezingen [0 ONDERWERP $onderwerp (onderwerp zelf)]",
                "  patroon Post $vouwonderwerp alfa [] join [] beta [] bind []",
                "  onderwerp Sein $onderwerp alfa [] join [bereik_1: post = $vouwonderwerp] beta [] bind"
                        + " []",
                "  gevolg Bijdrage [onderwerp=$vouwonderwerp, vouw=seinaantal van een post: vouw 0,"
                        + " lid=0, slots=[0], grootte=1, invoeging=LOGISCH]",
                "    inbreng Telling",
                "regel seingewicht van een post: bijdrage aan vouw 1 | vorm seingewicht van een post |"
                        + " groep null",
                "  lezingen [0 ONDERWERP $onderwerp (onderwerp zelf), 1 INVOER $lezing_1 (eigen"
                        + " ElementSleutel[objecttype=sein, naam=gewicht, kenmerk=false])]",
                "  patroon Post $vouwonderwerp alfa [] join [] beta [] bind []",
                "  onderwerp Sein $onderwerp alfa [] join [bereik_6: post = $vouwonderwerp] beta [] bind"
                        + " [$lezing_1 = [] Sein.gewicht WAARDE]",
                "  gevolg Bijdrage [onderwerp=$vouwonderwerp, vouw=seingewicht van een post: vouw 1,"
                        + " lid=0, slots=[0, 1], grootte=2, invoeging=LOGISCH]",
                "    inbreng Lidwaarde [waarde=1, totaal=null, gevuld=false]",
                "regel vakseinaantal van een kustvak: bijdrage aan vouw 2 | vorm vakseinaantal van een"
                        + " kustvak | groep null",
                "  lezingen [0 ONDERWERP $onderwerp (onderwerp zelf)]",
                "  patroon Kustvak $vouwonderwerp alfa [] join [] beta [] bind []",
                "  onderwerp Sein $onderwerp alfa [] join [] beta [] bind []",
                "  bestaat binnen [$koppeling_bereik_13] buiten [$onderwerp, $vouwonderwerp] [Koppeling"
                        + " $koppeling_bereik_13 alfa [bereik-koppeling_14: [[] relatie WAARDE GELIJK"
                        + " vakbeseining]] join [bereik-lid_15: tweede = $onderwerp, bereik-drager_16: eerste ="
                        + " $vouwonderwerp] beta [] bind []]",
                "  gevolg Bijdrage [onderwerp=$vouwonderwerp, vouw=vakseinaantal van een kustvak: vouw"
                        + " 2, lid=0, slots=[0], grootte=1, invoeging=LOGISCH]",
                "    inbreng Telling"),
                plannen(golden("jutterswet-postwacht")));
    }

    @Test void een_tijdlijntotaal_met_zijn_lidfout() throws Exception {
        assertEquals(List.of(
                "regel keurlast van een jutter: bijdrage aan vouw 0 | vorm keurlast van een jutter |"
                        + " groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=vondst, naam=gekeurd,"
                        + " kenmerk=true]), 1 ONDERWERP $onderwerp (onderwerp zelf), 2 TIJDLIJN $tijdlijn_2"
                        + " (tijdlijn ElementSleutel[objecttype=vondst, naam=keurtarief, kenmerk=false])]",
                "  patroon Jutter $vouwonderwerp alfa [] join [] beta [] bind []",
                "  onderwerp Vondst $onderwerp alfa [] join [bereik_0: keurende jutter = $vouwonderwerp]"
                        + " beta [] bind [$lezing_0 = [] Vondst.gekeurd WAAR]",
                "  patroon Stand $stand_2 alfa [stand_1: [[] attribuut WAARDE GELIJK"
                        + " ElementSleutel[objecttype=vondst, naam=keurtarief, kenmerk=false]]] join"
                        + " [stand-onderwerp_2: onderwerp = $onderwerp] beta [] bind [$tijdlijn_2 = []"
                        + " Stand.waarde WAARDE]",
                "  toets voorwaarde_3 VOORWAARDE sonde -1 slots [0] grootte 3: KENMERK(Lees 0, Const"
                        + " true)",
                "    toets 0 Aanroep bron 'gekeurd zijn'",
                "    toets 0.0 Lees",
                "    toets 0.1 Const Boolean",
                "  gevolg Bijdrage [onderwerp=$vouwonderwerp, vouw=keurlast van een jutter: vouw 0,"
                        + " lid=1, slots=[1, 2], grootte=3, invoeging=LOGISCH]",
                "    inbreng Lidwaarde [waarde=2, totaal=MAAND, gevuld=false]",
                "regel keurlast van een jutter: lidfout in vouw 0 | vorm keurlast van een jutter | groep"
                        + " null",
                "  lezingen [0 LIJSTPLAATSEN $lijstplaatsen_plaats_7 (lijstplaatsen 0), 1 LIJSTPLAATS"
                        + " $plaats_plaats_8 (lijstplaats 0), 2 INVOER $lezing_2 (eigen"
                        + " ElementSleutel[objecttype=vondst, naam=gekeurd, kenmerk=true]), 3 TIJDLIJN $tijdlijn_3"
                        + " (tijdlijn ElementSleutel[objecttype=vondst, naam=keurtarief, kenmerk=false]), 4"
                        + " ONDERWERP $onderwerp (onderwerp zelf)]",
                "  patroon Jutter $vouwonderwerp alfa [] join [] beta [] bind []",
                "  onderwerp Vondst $onderwerp alfa [] join [bereik_4: keurende jutter = $vouwonderwerp]"
                        + " beta [] bind [$lezing_2 = [] Vondst.gekeurd WAAR]",
                "  accumulatie Lijstplaats $lijstplaats_plaats_5 alfa [plaats-veld_9: [[] veld WAARDE"
                        + " GELIJK keurendeJutter]] join [] beta [plaats-lid_10: PLAATS_VAN $onderwerp [] {List<>,"
                        + " type null, bron null}] bind [$lijstplaatswaarde_plaats_6 = [] Lijstplaats.plaats"
                        + " WAARDE] functies [AANTAL null -> $lijstplaatsen_plaats_7, MINIMUM"
                        + " $lijstplaatswaarde_plaats_6 -> $plaats_plaats_8]",
                "  patroon Stand $stand_3 alfa [stand_11: [[] attribuut WAARDE GELIJK"
                        + " ElementSleutel[objecttype=vondst, naam=keurtarief, kenmerk=false]]] join"
                        + " [stand-onderwerp_12: onderwerp = $onderwerp] beta [] bind [$tijdlijn_3 = []"
                        + " Stand.waarde WAARDE]",
                "  toets voorwaarde_13 VORM sonde -1 slots [2, 3] grootte 5: Samengesteld ALLE"
                        + " 0(KENMERK(Lees 2, Const true), EMMERSCALAIR(Lees 3, Const true))",
                "    toets 0 Samengesteld",
                "    toets 0.0 Aanroep bron 'gekeurd zijn'",
                "    toets 0.0.0 Lees",
                "    toets 0.0.1 Const Boolean",
                "    toets 0.1 Aanroep",
                "    toets 0.1.0 Lees",
                "    toets 0.1.1 Const Boolean",
                "  gevolg Lidfout [onderwerp=$vouwonderwerp, vouw=keurlast van een jutter: vouw 0,"
                        + " lid=4, plaats=selectie 0 [keurendeJutter 0 1] LIDFOUT, voor=the per-element temporal"
                        + " total of \"keurtarief\" over , na=: the case supplies an asOfDate-bucket scalar, which"
                        + " carries no period of its own; a set fold needs each member's own timeline (ch."
                        + " 3.6/§7.1, ch. 8.4), invoeging=LOGISCH]"),
                plannen(golden("jutterswet-keurlast")));
    }

    @Test void een_filter_dat_deponeert_en_faalt() throws Exception {
        assertEquals(List.of(
                "regel vondstental: bijdrage aan vouw 0 | vorm vondstental | groep null",
                "  lezingen [0 ONDERWERP $onderwerp (onderwerp zelf)]",
                "  patroon Jutter $vouwonderwerp alfa [] join [] beta [] bind []",
                "  onderwerp Vondst $onderwerp alfa [] join [bereik_0: aanmelder = $vouwonderwerp] beta"
                        + " [] bind []",
                "  gevolg Bijdrage [onderwerp=$vouwonderwerp, vouw=vondstental: vouw 0, lid=0,"
                        + " slots=[0], grootte=1, invoeging=LOGISCH]",
                "    inbreng Telling",
                "regel zware vondsten: bijdrage aan vouw 1 | vorm zware vondsten | groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=vondst, naam=gewicht,"
                        + " kenmerk=false]), 1 INVOER $lezing_1 (eigen ElementSleutel[objecttype=vondst,"
                        + " naam=deelnemers, kenmerk=false]), 2 SONDE null (null), 3 ONDERWERP $onderwerp"
                        + " (onderwerp zelf)]",
                "  patroon Jutter $vouwonderwerp alfa [] join [] beta [] bind []",
                "  onderwerp Vondst $onderwerp alfa [] join [bereik_5: aanmelder = $vouwonderwerp] beta"
                        + " [] bind [$lezing_0 = [] Vondst.gewicht WAARDE, $lezing_1 = [] Vondst.deelnemers WAARDE]",
                "  toets voorwaarde_6 ZONDER_FOUT sonde -1 slots [0, 1] grootte 4: Samengesteld DE"
                        + " 0(RS_AGG_VERGELIJK(Deel GEDEELD sonde 2 (Lees 0, Lees 1), Const 2, Const >))",
                "    toets 0 Samengesteld bron 'aan de volgende voorwaarde voldoen: zijn gewicht gedeeld"
                        + " door zijn deelnemers is groter dan 2'",
                "    toets 0.0 Aanroep bron 'zijn gewicht gedeeld door zijn deelnemers is groter dan 2'",
                "    toets 0.0.0 Deel bron 'zijn gewicht gedeeld door zijn deelnemers'",
                "    toets 0.0.0.0 Lees bron 'zijn gewicht'",
                "    toets 0.0.0.1 Lees bron 'zijn deelnemers'",
                "    toets 0.0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0,"
                        + " teken=ONBEPAALD, eenheid=null] bron '2'",
                "    toets 0.0.2 Const String",
                "  gevolg Bijdrage [onderwerp=$vouwonderwerp, vouw=zware vondsten: vouw 1, lid=3,"
                        + " slots=[3], grootte=4, invoeging=LOGISCH]",
                "    inbreng Telling",
                "regel zware vondsten: filterfout in vouw 1 | vorm zware vondsten | groep null",
                "  lezingen [0 LIJSTPLAATSEN $lijstplaatsen_plaats_10 (lijstplaatsen 0), 1 LIJSTPLAATS"
                        + " $plaats_plaats_11 (lijstplaats 0), 2 INVOER $lezing_2 (eigen"
                        + " ElementSleutel[objecttype=vondst, naam=gewicht, kenmerk=false]), 3 INVOER $lezing_3"
                        + " (eigen ElementSleutel[objecttype=vondst, naam=deelnemers, kenmerk=false]), 4 SONDE"
                        + " null (null), 5 ONDERWERP $onderwerp (onderwerp zelf)]",
                "  patroon Jutter $vouwonderwerp alfa [] join [] beta [] bind []",
                "  onderwerp Vondst $onderwerp alfa [] join [bereik_7: aanmelder = $vouwonderwerp] beta"
                        + " [] bind [$lezing_2 = [] Vondst.gewicht WAARDE, $lezing_3 = [] Vondst.deelnemers WAARDE]",
                "  accumulatie Lijstplaats $lijstplaats_plaats_8 alfa [plaats-veld_12: [[] veld WAARDE"
                        + " GELIJK aanmelder]] join [] beta [plaats-lid_13: PLAATS_VAN $onderwerp [] {List<>, type"
                        + " null, bron null}] bind [$lijstplaatswaarde_plaats_9 = [] Lijstplaats.plaats WAARDE]"
                        + " functies [AANTAL null -> $lijstplaatsen_plaats_10, MINIMUM $lijstplaatswaarde_plaats_9"
                        + " -> $plaats_plaats_11]",
                "  toets voorwaarde_14 FOUT sonde -1 slots [2, 3] grootte 6: Samengesteld DE"
                        + " 0(RS_AGG_VERGELIJK(Deel GEDEELD sonde 4 (Lees 2, Lees 3), Const 2, Const >))",
                "    toets 0 Samengesteld bron 'aan de volgende voorwaarde voldoen: zijn gewicht gedeeld"
                        + " door zijn deelnemers is groter dan 2'",
                "    toets 0.0 Aanroep bron 'zijn gewicht gedeeld door zijn deelnemers is groter dan 2'",
                "    toets 0.0.0 Deel bron 'zijn gewicht gedeeld door zijn deelnemers'",
                "    toets 0.0.0.0 Lees bron 'zijn gewicht'",
                "    toets 0.0.0.1 Lees bron 'zijn deelnemers'",
                "    toets 0.0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0,"
                        + " teken=ONBEPAALD, eenheid=null] bron '2'",
                "    toets 0.0.2 Const String",
                "  gevolg Filterfout [onderwerp=$vouwonderwerp, vouw=zware vondsten: vouw 1, lid=5,"
                        + " plaats=selectie 0 [aanmelder 0 1] FILTERFOUT, slots=[0, 1, 2, 3, 5], grootte=6,"
                        + " invoeging=LOGISCH] filter Samengesteld DE 0(RS_AGG_VERGELIJK(Deel GEDEELD sonde 4"
                        + " (Lees 2, Lees 3), Const 2, Const >))",
                "    filter 0 Samengesteld bron 'aan de volgende voorwaarde voldoen: zijn gewicht"
                        + " gedeeld door zijn deelnemers is groter dan 2'",
                "    filter 0.0 Aanroep bron 'zijn gewicht gedeeld door zijn deelnemers is groter dan 2'",
                "    filter 0.0.0 Deel bron 'zijn gewicht gedeeld door zijn deelnemers'",
                "    filter 0.0.0.0 Lees bron 'zijn gewicht'",
                "    filter 0.0.0.1 Lees bron 'zijn deelnemers'",
                "    filter 0.0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0,"
                        + " teken=ONBEPAALD, eenheid=null] bron '2'",
                "    filter 0.0.2 Const String",
                "regel zware vondsten: filterdeponering in vouw 1 | vorm zware vondsten | groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=vondst, naam=gewicht,"
                        + " kenmerk=false]), 1 INVOER $lezing_1 (eigen ElementSleutel[objecttype=vondst,"
                        + " naam=deelnemers, kenmerk=false]), 2 SONDE null (null), 3 ONDERWERP $onderwerp"
                        + " (onderwerp zelf)]",
                "  patroon Jutter $vouwonderwerp alfa [] join [] beta [] bind []",
                "  onderwerp Vondst $onderwerp alfa [] join [bereik_15: aanmelder = $vouwonderwerp] beta"
                        + " [] bind [$lezing_0 = [] Vondst.gewicht WAARDE, $lezing_1 = [] Vondst.deelnemers WAARDE]",
                "  toets voorwaarde_16 DEPONERING sonde 2 slots [0, 1] grootte 4: Samengesteld DE"
                        + " 0(RS_AGG_VERGELIJK(Deel GEDEELD sonde 2 (Lees 0, Lees 1), Const 2, Const >))",
                "    toets 0 Samengesteld bron 'aan de volgende voorwaarde voldoen: zijn gewicht gedeeld"
                        + " door zijn deelnemers is groter dan 2'",
                "    toets 0.0 Aanroep bron 'zijn gewicht gedeeld door zijn deelnemers is groter dan 2'",
                "    toets 0.0.0 Deel bron 'zijn gewicht gedeeld door zijn deelnemers'",
                "    toets 0.0.0.0 Lees bron 'zijn gewicht'",
                "    toets 0.0.0.1 Lees bron 'zijn deelnemers'",
                "    toets 0.0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0,"
                        + " teken=ONBEPAALD, eenheid=null] bron '2'",
                "    toets 0.0.2 Const String",
                "  gevolg Filterdeponering [onderwerp=$vouwonderwerp, vouw=zware vondsten: vouw 1,"
                        + " lid=3, invoeging=LOGISCH]",
                "regel drukte: bijdrage aan vouw 2 | vorm drukte | groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=vondst, naam=gewicht,"
                        + " kenmerk=false]), 1 INVOER $lezing_1 (eigen ElementSleutel[objecttype=vondst,"
                        + " naam=deelnemers, kenmerk=false]), 2 SONDE null (null), 3 ONDERWERP $onderwerp"
                        + " (onderwerp zelf)]",
                "  patroon Jutter $vouwonderwerp alfa [] join [] beta [] bind []",
                "  onderwerp Vondst $onderwerp alfa [] join [bereik_31: aanmelder = $vouwonderwerp] beta"
                        + " [] bind [$lezing_0 = [] Vondst.gewicht WAARDE, $lezing_1 = [] Vondst.deelnemers WAARDE]",
                "  toets voorwaarde_32 ZONDER_FOUT sonde -1 slots [0, 1] grootte 4: Samengesteld DE"
                        + " 0(RS_AGG_VERGELIJK(Deel GEDEELD sonde 2 (Lees 0, Lees 1), Const 2, Const >))",
                "    toets 0 Samengesteld bron 'aan de volgende voorwaarde voldoen: zijn gewicht gedeeld"
                        + " door zijn deelnemers is groter dan 2'",
                "    toets 0.0 Aanroep bron 'zijn gewicht gedeeld door zijn deelnemers is groter dan 2'",
                "    toets 0.0.0 Deel bron 'zijn gewicht gedeeld door zijn deelnemers'",
                "    toets 0.0.0.0 Lees bron 'zijn gewicht'",
                "    toets 0.0.0.1 Lees bron 'zijn deelnemers'",
                "    toets 0.0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0,"
                        + " teken=ONBEPAALD, eenheid=null] bron '2'",
                "    toets 0.0.2 Const String",
                "  gevolg Bijdrage [onderwerp=$vouwonderwerp, vouw=drukte: vouw 2, lid=3, slots=[3],"
                        + " grootte=4, invoeging=LOGISCH]",
                "    inbreng Telling",
                "regel drukte: filterfout in vouw 2 | vorm drukte | groep null",
                "  lezingen [0 LIJSTPLAATSEN $lijstplaatsen_plaats_36 (lijstplaatsen 0), 1 LIJSTPLAATS"
                        + " $plaats_plaats_37 (lijstplaats 0), 2 INVOER $lezing_2 (eigen"
                        + " ElementSleutel[objecttype=vondst, naam=gewicht, kenmerk=false]), 3 INVOER $lezing_3"
                        + " (eigen ElementSleutel[objecttype=vondst, naam=deelnemers, kenmerk=false]), 4 SONDE"
                        + " null (null), 5 ONDERWERP $onderwerp (onderwerp zelf)]",
                "  patroon Jutter $vouwonderwerp alfa [] join [] beta [] bind []",
                "  onderwerp Vondst $onderwerp alfa [] join [bereik_33: aanmelder = $vouwonderwerp] beta"
                        + " [] bind [$lezing_2 = [] Vondst.gewicht WAARDE, $lezing_3 = [] Vondst.deelnemers WAARDE]",
                "  accumulatie Lijstplaats $lijstplaats_plaats_34 alfa [plaats-veld_38: [[] veld WAARDE"
                        + " GELIJK aanmelder]] join [] beta [plaats-lid_39: PLAATS_VAN $onderwerp [] {List<>, type"
                        + " null, bron null}] bind [$lijstplaatswaarde_plaats_35 = [] Lijstplaats.plaats WAARDE]"
                        + " functies [AANTAL null -> $lijstplaatsen_plaats_36, MINIMUM"
                        + " $lijstplaatswaarde_plaats_35 -> $plaats_plaats_37]",
                "  toets voorwaarde_40 FOUT sonde -1 slots [2, 3] grootte 6: Samengesteld DE"
                        + " 0(RS_AGG_VERGELIJK(Deel GEDEELD sonde 4 (Lees 2, Lees 3), Const 2, Const >))",
                "    toets 0 Samengesteld bron 'aan de volgende voorwaarde voldoen: zijn gewicht gedeeld"
                        + " door zijn deelnemers is groter dan 2'",
                "    toets 0.0 Aanroep bron 'zijn gewicht gedeeld door zijn deelnemers is groter dan 2'",
                "    toets 0.0.0 Deel bron 'zijn gewicht gedeeld door zijn deelnemers'",
                "    toets 0.0.0.0 Lees bron 'zijn gewicht'",
                "    toets 0.0.0.1 Lees bron 'zijn deelnemers'",
                "    toets 0.0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0,"
                        + " teken=ONBEPAALD, eenheid=null] bron '2'",
                "    toets 0.0.2 Const String",
                "  gevolg Filterfout [onderwerp=$vouwonderwerp, vouw=drukte: vouw 2, lid=5,"
                        + " plaats=selectie 0 [aanmelder 0 1] FILTERFOUT, slots=[0, 1, 2, 3, 5], grootte=6,"
                        + " invoeging=LOGISCH] filter Samengesteld DE 0(RS_AGG_VERGELIJK(Deel GEDEELD sonde 4"
                        + " (Lees 2, Lees 3), Const 2, Const >))",
                "    filter 0 Samengesteld bron 'aan de volgende voorwaarde voldoen: zijn gewicht"
                        + " gedeeld door zijn deelnemers is groter dan 2'",
                "    filter 0.0 Aanroep bron 'zijn gewicht gedeeld door zijn deelnemers is groter dan 2'",
                "    filter 0.0.0 Deel bron 'zijn gewicht gedeeld door zijn deelnemers'",
                "    filter 0.0.0.0 Lees bron 'zijn gewicht'",
                "    filter 0.0.0.1 Lees bron 'zijn deelnemers'",
                "    filter 0.0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0,"
                        + " teken=ONBEPAALD, eenheid=null] bron '2'",
                "    filter 0.0.2 Const String",
                "regel drukte: filterdeponering in vouw 2 | vorm drukte | groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=vondst, naam=gewicht,"
                        + " kenmerk=false]), 1 INVOER $lezing_1 (eigen ElementSleutel[objecttype=vondst,"
                        + " naam=deelnemers, kenmerk=false]), 2 SONDE null (null), 3 ONDERWERP $onderwerp"
                        + " (onderwerp zelf)]",
                "  patroon Jutter $vouwonderwerp alfa [] join [] beta [] bind []",
                "  onderwerp Vondst $onderwerp alfa [] join [bereik_41: aanmelder = $vouwonderwerp] beta"
                        + " [] bind [$lezing_0 = [] Vondst.gewicht WAARDE, $lezing_1 = [] Vondst.deelnemers WAARDE]",
                "  toets voorwaarde_42 DEPONERING sonde 2 slots [0, 1] grootte 4: Samengesteld DE"
                        + " 0(RS_AGG_VERGELIJK(Deel GEDEELD sonde 2 (Lees 0, Lees 1), Const 2, Const >))",
                "    toets 0 Samengesteld bron 'aan de volgende voorwaarde voldoen: zijn gewicht gedeeld"
                        + " door zijn deelnemers is groter dan 2'",
                "    toets 0.0 Aanroep bron 'zijn gewicht gedeeld door zijn deelnemers is groter dan 2'",
                "    toets 0.0.0 Deel bron 'zijn gewicht gedeeld door zijn deelnemers'",
                "    toets 0.0.0.0 Lees bron 'zijn gewicht'",
                "    toets 0.0.0.1 Lees bron 'zijn deelnemers'",
                "    toets 0.0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0,"
                        + " teken=ONBEPAALD, eenheid=null] bron '2'",
                "    toets 0.0.2 Const String",
                "  gevolg Filterdeponering [onderwerp=$vouwonderwerp, vouw=drukte: vouw 2, lid=3,"
                        + " invoeging=LOGISCH]"),
                plannen(golden("jutterswet-filterwacht")));
    }

    @Test void een_pad_waarvan_de_wandelingen_elkaar_ontmoeten() throws Exception {
        assertEquals(List.of(
                "regel laagste vondstgewicht van een jutter: bijdrage aan vouw 0 | vorm laagste"
                        + " vondstgewicht van een jutter | groep null",
                "  lezingen [0 ONDERWERP $onderwerp (onderwerp zelf), 1 INVOER $lezing_1 (eigen"
                        + " ElementSleutel[objecttype=vondst, naam=gewicht, kenmerk=false])]",
                "  patroon Jutter $vouwonderwerp alfa [] join [] beta [] bind []",
                "  onderwerp Vondst $onderwerp alfa [] join [] beta [pad_0: ONTMOETING $vouwonderwerp"
                        + " [bergendeBoot, schipper] {List<String>, type null, bron null} [] {List<>, type null,"
                        + " bron null}] bind [$lezing_1 = [] Vondst.gewicht WAARDE]",
                "  gevolg Bijdrage [onderwerp=$vouwonderwerp, vouw=laagste vondstgewicht van een jutter:"
                        + " vouw 0, lid=0, slots=[0, 1], grootte=2, invoeging=LOGISCH]",
                "    inbreng Lidwaarde [waarde=1, totaal=null, gevuld=false]"),
                plannen(golden("jutterswet-vondstketen")));
    }

    @Test void een_pad_met_twee_tussenknopen_bestaand_en_per_route() throws Exception {
        assertEquals(List.of(
                "regel aantal gevulde bergplaatsen: bijdrage aan vouw 0 | vorm aantal gevulde"
                        + " bergplaatsen | groep null",
                "  lezingen [0 ONDERWERP $onderwerp (onderwerp zelf), 1 INVOER $lezing_1 (eigen"
                        + " ElementSleutel[objecttype=bergplaats, naam=vangstgewicht, kenmerk=false])]",
                "  patroon Jutter $vouwonderwerp alfa [] join [] beta [] bind []",
                "  onderwerp Bergplaats $onderwerp alfa [] join [] beta [] bind [$lezing_1 = []"
                        + " Bergplaats.vangstgewicht WAARDE]",
                "  bestaat binnen [$tussen_pad_0] buiten [$vouwonderwerp, $onderwerp] [Boot"
                        + " $tussen_pad_0 alfa [] join [] beta [pad_1: ONTMOETING $vouwonderwerp [schipper]"
                        + " {List<String>, type null, bron null} [] {List<>, type null, bron null}, pad_2:"
                        + " ONTMOETING $onderwerp [losplaats] {List<String>, type null, bron null} [] {List<>,"
                        + " type null, bron null}] bind []]",
                "  gevolg Bijdrage [onderwerp=$vouwonderwerp, vouw=aantal gevulde bergplaatsen: vouw 0,"
                        + " lid=0, slots=[0, 1], grootte=2, invoeging=LOGISCH]",
                "    inbreng Lidwaarde [waarde=1, totaal=null, gevuld=true]",
                "regel havengeldtotaal van een Jutter: bijdrage aan vouw 1 | vorm havengeldtotaal van"
                        + " een Jutter | groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=haven, naam=havengeld,"
                        + " kenmerk=false]), 1 INVOER $lezing_1 (eigen ElementSleutel[objecttype=haven,"
                        + " naam=ligplaatsen, kenmerk=false]), 2 SONDE null (null), 3 ONDERWERP $onderwerp"
                        + " (onderwerp zelf)]",
                "  patroon Jutter $vouwonderwerp alfa [] join [] beta [] bind []",
                "  onderwerp Haven $onderwerp alfa [] join [] beta [] bind [$lezing_0 = []"
                        + " Haven.havengeld WAARDE, $lezing_1 = [] Haven.ligplaatsen WAARDE]",
                "  bestaat binnen [$tussen_pad_7, $tussen_pad_8] buiten [$vouwonderwerp, $onderwerp]"
                        + " [Boot $tussen_pad_7 alfa [] join [] beta [pad_9: ONTMOETING $vouwonderwerp [schipper]"
                        + " {List<String>, type null, bron null} [] {List<>, type null, bron null}] bind [],"
                        + " Bergplaats $tussen_pad_8 alfa [] join [] beta [pad_10: ONTMOETING $tussen_pad_7 []"
                        + " {List<>, type null, bron null} [losplaats] {List<String>, type null, bron null},"
                        + " pad_11: ONTMOETING $onderwerp [haven] {List<String>, type null, bron null} [] {List<>,"
                        + " type null, bron null}] bind []]",
                "  toets voorwaarde_12 ZONDER_FOUT sonde -1 slots [0, 1] grootte 4: Samengesteld DE"
                        + " 0(RS_AGG_VERGELIJK(Deel GEDEELD sonde 2 (Lees 0, Lees 1), Const 2, Const >))",
                "    toets 0 Samengesteld bron 'aan de volgende voorwaarde voldoen: zijn havengeld"
                        + " gedeeld door zijn ligplaatsen is groter dan 2'",
                "    toets 0.0 Aanroep bron 'zijn havengeld gedeeld door zijn ligplaatsen is groter dan"
                        + " 2'",
                "    toets 0.0.0 Deel bron 'zijn havengeld gedeeld door zijn ligplaatsen'",
                "    toets 0.0.0.0 Lees bron 'zijn havengeld'",
                "    toets 0.0.0.1 Lees bron 'zijn ligplaatsen'",
                "    toets 0.0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0,"
                        + " teken=ONBEPAALD, eenheid=null] bron '2'",
                "    toets 0.0.2 Const String",
                "  gevolg Bijdrage [onderwerp=$vouwonderwerp, vouw=havengeldtotaal van een Jutter: vouw"
                        + " 1, lid=3, slots=[0, 3], grootte=4, invoeging=LOGISCH]",
                "    inbreng Lidwaarde [waarde=0, totaal=null, gevuld=false]",
                "regel havengeldtotaal van een Jutter: filterfout in vouw 1 | vorm havengeldtotaal van"
                        + " een Jutter | groep null",
                "  lezingen [0 LIJSTPLAATSEN $lijstplaatsen_plaats_20 (lijstplaatsen 0), 1 LIJSTPLAATS"
                        + " $plaats_plaats_21 (lijstplaats 0), 2 INVOER $lezing_2 (eigen"
                        + " ElementSleutel[objecttype=haven, naam=havengeld, kenmerk=false]), 3 INVOER $lezing_3"
                        + " (eigen ElementSleutel[objecttype=haven, naam=ligplaatsen, kenmerk=false]), 4 SONDE"
                        + " null (null), 5 ONDERWERP $onderwerp (onderwerp zelf)]",
                "  patroon Jutter $vouwonderwerp alfa [] join [] beta [] bind []",
                "  onderwerp Haven $onderwerp alfa [] join [] beta [] bind [$lezing_2 = []"
                        + " Haven.havengeld WAARDE, $lezing_3 = [] Haven.ligplaatsen WAARDE]",
                "  patroon Boot $tussen_pad_13 alfa [] join [] beta [pad_15: ONTMOETING $vouwonderwerp"
                        + " [schipper] {List<String>, type null, bron null} [] {List<>, type null, bron null}]"
                        + " bind []",
                "  patroon Bergplaats $tussen_pad_14 alfa [] join [] beta [pad_16: ONTMOETING"
                        + " $tussen_pad_13 [] {List<>, type null, bron null} [losplaats] {List<String>, type null,"
                        + " bron null}, pad_17: ONTMOETING $onderwerp [haven] {List<String>, type null, bron null}"
                        + " [] {List<>, type null, bron null}] bind []",
                "  accumulatie Lijstplaats $lijstplaats_plaats_18 alfa [plaats-veld_22: [[] veld WAARDE"
                        + " GELIJK schipper]] join [] beta [plaats-lid_23: PLAATS_VAN $tussen_pad_13 [] {List<>,"
                        + " type null, bron null}] bind [$lijstplaatswaarde_plaats_19 = [] Lijstplaats.plaats"
                        + " WAARDE] functies [AANTAL null -> $lijstplaatsen_plaats_20, MINIMUM"
                        + " $lijstplaatswaarde_plaats_19 -> $plaats_plaats_21]",
                "  toets voorwaarde_24 FOUT sonde -1 slots [2, 3] grootte 6: Samengesteld DE"
                        + " 0(RS_AGG_VERGELIJK(Deel GEDEELD sonde 4 (Lees 2, Lees 3), Const 2, Const >))",
                "    toets 0 Samengesteld bron 'aan de volgende voorwaarde voldoen: zijn havengeld"
                        + " gedeeld door zijn ligplaatsen is groter dan 2'",
                "    toets 0.0 Aanroep bron 'zijn havengeld gedeeld door zijn ligplaatsen is groter dan"
                        + " 2'",
                "    toets 0.0.0 Deel bron 'zijn havengeld gedeeld door zijn ligplaatsen'",
                "    toets 0.0.0.0 Lees bron 'zijn havengeld'",
                "    toets 0.0.0.1 Lees bron 'zijn ligplaatsen'",
                "    toets 0.0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0,"
                        + " teken=ONBEPAALD, eenheid=null] bron '2'",
                "    toets 0.0.2 Const String",
                "  gevolg Filterfout [onderwerp=$vouwonderwerp, vouw=havengeldtotaal van een Jutter:"
                        + " vouw 1, lid=5, plaats=selectie 0 [schipper 0 1] FILTERFOUT, slots=[0, 1, 2, 3, 5],"
                        + " grootte=6, invoeging=LOGISCH] filter Samengesteld DE 0(RS_AGG_VERGELIJK(Deel GEDEELD"
                        + " sonde 4 (Lees 2, Lees 3), Const 2, Const >))",
                "    filter 0 Samengesteld bron 'aan de volgende voorwaarde voldoen: zijn havengeld"
                        + " gedeeld door zijn ligplaatsen is groter dan 2'",
                "    filter 0.0 Aanroep bron 'zijn havengeld gedeeld door zijn ligplaatsen is groter dan"
                        + " 2'",
                "    filter 0.0.0 Deel bron 'zijn havengeld gedeeld door zijn ligplaatsen'",
                "    filter 0.0.0.0 Lees bron 'zijn havengeld'",
                "    filter 0.0.0.1 Lees bron 'zijn ligplaatsen'",
                "    filter 0.0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0,"
                        + " teken=ONBEPAALD, eenheid=null] bron '2'",
                "    filter 0.0.2 Const String",
                "regel havengeldtotaal van een Jutter: filterdeponering in vouw 1 | vorm havengeldtotaal"
                        + " van een Jutter | groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=haven, naam=havengeld,"
                        + " kenmerk=false]), 1 INVOER $lezing_1 (eigen ElementSleutel[objecttype=haven,"
                        + " naam=ligplaatsen, kenmerk=false]), 2 SONDE null (null), 3 ONDERWERP $onderwerp"
                        + " (onderwerp zelf)]",
                "  patroon Jutter $vouwonderwerp alfa [] join [] beta [] bind []",
                "  onderwerp Haven $onderwerp alfa [] join [] beta [] bind [$lezing_0 = []"
                        + " Haven.havengeld WAARDE, $lezing_1 = [] Haven.ligplaatsen WAARDE]",
                "  bestaat binnen [$tussen_pad_25, $tussen_pad_26] buiten [$vouwonderwerp, $onderwerp]"
                        + " [Boot $tussen_pad_25 alfa [] join [] beta [pad_27: ONTMOETING $vouwonderwerp"
                        + " [schipper] {List<String>, type null, bron null} [] {List<>, type null, bron null}]"
                        + " bind [], Bergplaats $tussen_pad_26 alfa [] join [] beta [pad_28: ONTMOETING"
                        + " $tussen_pad_25 [] {List<>, type null, bron null} [losplaats] {List<String>, type null,"
                        + " bron null}, pad_29: ONTMOETING $onderwerp [haven] {List<String>, type null, bron null}"
                        + " [] {List<>, type null, bron null}] bind []]",
                "  toets voorwaarde_30 DEPONERING sonde 2 slots [0, 1] grootte 4: Samengesteld DE"
                        + " 0(RS_AGG_VERGELIJK(Deel GEDEELD sonde 2 (Lees 0, Lees 1), Const 2, Const >))",
                "    toets 0 Samengesteld bron 'aan de volgende voorwaarde voldoen: zijn havengeld"
                        + " gedeeld door zijn ligplaatsen is groter dan 2'",
                "    toets 0.0 Aanroep bron 'zijn havengeld gedeeld door zijn ligplaatsen is groter dan"
                        + " 2'",
                "    toets 0.0.0 Deel bron 'zijn havengeld gedeeld door zijn ligplaatsen'",
                "    toets 0.0.0.0 Lees bron 'zijn havengeld'",
                "    toets 0.0.0.1 Lees bron 'zijn ligplaatsen'",
                "    toets 0.0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0,"
                        + " teken=ONBEPAALD, eenheid=null] bron '2'",
                "    toets 0.0.2 Const String",
                "  gevolg Filterdeponering [onderwerp=$vouwonderwerp, vouw=havengeldtotaal van een"
                        + " Jutter: vouw 1, lid=3, invoeging=LOGISCH]"),
                plannen(havenketen()));
    }

    @Test void een_kwantorbijdrage_met_lidtoets_en_sonde() throws Exception {
        assertEquals(List.of(
                "regel rustigheid: bijdrage aan vouw 0 | vorm rustigheid | groep null",
                "  lezingen [0 ONDERWERP $onderwerp (onderwerp zelf), 1 INVOER $lezing_1 (eigen"
                        + " ElementSleutel[objecttype=wacht, naam=opgave, kenmerk=false]), 2 STAND $lezing_2"
                        + " (eigen ElementSleutel[objecttype=wacht, naam=deelnemers, kenmerk=false]), 3 SONDE null"
                        + " (null), 4 LIJSTPLAATSEN $lijstplaatsen_plaats_5 (lijstplaatsen 4), 5 LIJSTPLAATS"
                        + " $plaats_plaats_6 (lijstplaats 4)]",
                "  patroon Ploeg $vouwonderwerp alfa [] join [] beta [] bind []",
                "  onderwerp Wacht $onderwerp alfa [] join [bereik_2: ploeg = $vouwonderwerp] beta []"
                        + " bind [$lezing_1 = [] Wacht.opgave WAARDE]",
                "  accumulatie Lijstplaats $lijstplaats_plaats_3 alfa [plaats-veld_7: [[] veld WAARDE"
                        + " GELIJK ploeg]] join [] beta [plaats-lid_8: PLAATS_VAN $onderwerp [] {List<>, type"
                        + " null, bron null}] bind [$lijstplaatswaarde_plaats_4 = [] Lijstplaats.plaats WAARDE]"
                        + " functies [AANTAL null -> $lijstplaatsen_plaats_5, MINIMUM $lijstplaatswaarde_plaats_4"
                        + " -> $plaats_plaats_6]",
                "  patroon Stand $stand_2 alfa [stand_0: [[] attribuut WAARDE GELIJK"
                        + " ElementSleutel[objecttype=wacht, naam=deelnemers, kenmerk=false]]] join"
                        + " [stand-onderwerp_1: onderwerp = $onderwerp] beta [] bind [$lezing_2 = [] Stand.waarde"
                        + " WAARDE]",
                "  gevolg Bijdrage [onderwerp=$vouwonderwerp, vouw=rustigheid: vouw 0, lid=0, slots=[0,"
                        + " 1, 2, 4, 5], grootte=6, invoeging=LOGISCH]",
                "    inbreng Lidtoets [sonde=3, plaats=selectie 0 [ploeg 4 5] KWANTOR] matrix"
                        + " RS_AGG_VERGELIJK(Deel GEDEELD sonde 3 (Lees 1, Lees 2), Const 2, Const >)",
                "      matrix 0 Aanroep bron 'greater'",
                "      matrix 0.0 Deel bron 'dividedBy'",
                "      matrix 0.0.0 Lees bron 'zijn opgave'",
                "      matrix 0.0.1 Lees bron 'zijn deelnemers'",
                "      matrix 0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0,"
                        + " teken=ONBEPAALD, eenheid=null] bron '2'",
                "      matrix 0.2 Const String"),
                plannen(matrixDieDeelt()));
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
    @Test void het_pad_met_twee_tussenknopen_vuurt(@TempDir Path map) throws Exception {
        Path ir = map.resolve("ir.json");
        Files.writeString(ir, JSON.writeValueAsString(havenketen()));
        assertNull(speelCase(ir, JSON.readTree(HAVENKETEN_CASE)));
    }

    /** Case {@code zaak} of {@code ir} through the full replay on the exec-model branch, as the witnesses of the replay
     *  fire a case ({@code CaseRunner.speelCase}): the failure line of the case, null where every expectation matches.
     *  That entry is package-private in its own package, so this takes it by reflection, as {@link #matrixDieDeelt}
     *  takes its document. */
    static String speelCase(Path ir, JsonNode zaak) throws Exception {
        Method speel = CaseRunner.class.getDeclaredMethod("speelCase", Path.class, JsonNode.class,
                DroolsBackend.Branch.class);
        speel.setAccessible(true);
        return (String) speel.invoke(null, ir, zaak, DroolsBackend.Branch.EXEC_MODEL);
    }

    /** The case of {@link #havenketen} where the filter faults (fix round 1c-B2): J1's boats B1, B2 and B3, in that
     *  order, unload at P1, P2 and P3, which lie in H1, H2 and H1, and both havens have 0 ligplaatsen, so the filter
     *  divides by zero along every route. The reference interpreter and the ASP backend both fault with "division by
     *  numeric zero", a sentence that names no member. */
    private static final String HAVENKETEN_FOUT = """
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
              {"label": "H1", "objectType": "Haven", "attributes": {"havengeld": {"number": "4"},
               "ligplaatsen": {"number": "0"}}, "roles": {}, "characteristics": []},
              {"label": "H2", "objectType": "Haven", "attributes": {"havengeld": {"number": "30"},
               "ligplaatsen": {"number": "0"}}, "roles": {}, "characteristics": []}],
              "parameters": {}},
             "expectedFault": {"ref": "division by numeric zero", "clingo": "division by numeric zero",
              "drools": "4 gedeeld door 0"}}
            """;

    /** {@link #havenketen} fired where the filter faults (fix round 1c-B2): the rule of the filter's fault binds the
     *  two intermediates per route and reads the place of the route's Boot, the first one, in J1's list, so each route
     *  gives a {@link Filterfout} with its own place: H1 along B1 at [0, 0], H2 along B2 at [0, 1], H1 along B3 at
     *  [0, 2]. The least is the first route's, H1's, so the rule names H1's fault, the first member in fold order
     *  (RUNTIME-SEMANTICS ch. 5.11): "4 gedeeld door 0", where the least sentence would name H2's 30. A place reader
     *  that reads the wrong place gives another fault or other places; no document of the corpus or the suite fires
     *  this form, so the harness does not watch it. */
    @Test void het_pad_met_twee_tussenknopen_faalt_op_de_eerste_routeplaats(@TempDir Path map) throws Exception {
        Path ir = map.resolve("ir.json"), zaak = map.resolve("case.json");
        Files.writeString(ir, JSON.writeValueAsString(havenketen()));
        Files.writeString(zaak, HAVENKETEN_FOUT);
        String deling = "DelingDoorNul: deling door nul in de regel \"havengeldtotaal van een Jutter: vouw 1\": %s"
                + " gedeeld door 0 — een numerieke nul als deler is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4,"
                + " punt 4)";
        assertEquals(List.of(
                "Filterfout J1 H1 [0, 0]: " + deling.formatted("4"),
                "Filterfout J1 H2 [0, 1]: " + deling.formatted("30"),
                "Filterfout J1 H1 [0, 2]: " + deling.formatted("4")),
                filterfouten(ir, JSON.readTree(HAVENKETEN_FOUT)));
        CaseRunner.RoodResult r = CaseRunner.runRood(ir, zaak);
        assertEquals("FAULT", r.status(), r::detail);
        assertTrue(r.detail().startsWith("DelingDoorNul: deling door nul in de regel \"havengeldtotaal van een"
                + " Jutter\": 4 gedeeld door 0 — "), r::detail);
    }

    /** The {@link Filterfout} facts that stand at the fixpoint of {@code zaak} on {@code ir}, in their own order, each
     *  with the case labels of the fold's subject and of the member, its place and its fault as named after the fold.
     *  Read off the session after the fire and before the write-back, which raises the fault of the rule instead,
     *  through the measuring seam of {@code CaseRunner} ({@code voorMeting}, {@code metSessie}), which is
     *  package-private in its own package: by reflection, as {@link #matrixDieDeelt} takes its document. */
    @SuppressWarnings("unchecked")
    private static List<String> filterfouten(Path ir, JsonNode zaak) throws Exception {
        Method voorMeting = CaseRunner.class.getDeclaredMethod("voorMeting", Path.class, DroolsBackend.Branch.class);
        voorMeting.setAccessible(true);
        Object runner = voorMeting.invoke(null, ir, DroolsBackend.Branch.EXEC_MODEL);
        Class<?> meting = Class.forName("dev.uchar.regelspraak.cases.CaseRunner$Sessiemeting");
        Object lezer = Proxy.newProxyInstance(meting.getClassLoader(), new Class<?>[]{meting}, (proxy, m, a) -> {
            if (!m.getName().equals("meet")) throw new UnsupportedOperationException(m.getName());
            DroolsBackend.Build bouw = (DroolsBackend.Build) a[0];
            KieSession sessie = (KieSession) a[1];
            DroolsBackend.Vuring vuring = DroolsBackend.vuur(bouw, sessie);
            if (vuring.geenFixpunt() != null) throw new AssertionError(vuring.geenFixpunt());
            Map<Object, String> label = new IdentityHashMap<>();
            ((Map<String, Object>) a[2]).forEach((l, boon) -> label.put(boon, l));
            List<Filterfout> fouten = new ArrayList<>();
            for (Object f : sessie.getObjects()) if (f instanceof Filterfout x) fouten.add(x);
            fouten.sort(null);
            List<String> uit = new ArrayList<>();
            for (Filterfout x : fouten) {
                RuntimeException benoemd = Rekenfout.benoem(x.fout(), x.vouw());
                uit.add("Filterfout " + label.get(x.onderwerp()) + " " + label.get(x.element()) + " " + x.plaats()
                        + ": " + benoemd.getClass().getSimpleName() + ": " + benoemd.getMessage());
            }
            return uit;
        });
        Method metSessie = CaseRunner.class.getDeclaredMethod("metSessie", JsonNode.class, meting);
        metSessie.setAccessible(true);
        return (List<String>) metSessie.invoke(runner, zaak, lezer);
    }

    /** The scope of an existence covers what its patterns bind as well (fix round 1c-B2): a value a pattern inside
     *  binds lives only inside it, like the pattern's object. The plan declares it among the variables of the
     *  existence, the constructor refuses a scope without it, and the rendering forgets it after the existence, so an
     *  item after it that names the value fails loudly instead of joining a value of the quantification. */
    @Test void de_scope_van_een_bestaan_omvat_de_bindingen_van_zijn_patronen() {
        Patroon metBinding = new Patroon(Waarde.class, "$waarde", List.of(), List.of(),
                List.of(Binding.van("$v", Waarde.class, "waarde")));
        IllegalArgumentException zonder = assertThrows(IllegalArgumentException.class,
                () -> new Bestaat(List.of(metBinding), List.of("$waarde"), List.of()));
        assertEquals("een bestaat-item declareert de variabelen van zijn patronen en van hun bindingen,"
                + " [$waarde, $v], niet [$waarde]", zonder.getMessage());
        Bestaat bestaan = new Bestaat(List.of(metBinding), List.of("$waarde", "$v"), List.of());
        Regelplan erna = new Regelplan("na het bestaan", null, null, new Leestabel(List.of()), List.of(bestaan,
                new Patroon(Waarde.class, "$erna", List.of(), List.of(new Join("erna", null, "$v")), List.of())),
                new Gevolg.Kanaalstand("bestaan", true, Gevolg.Invoeging.LOGISCH));
        IllegalStateException weg = assertThrows(IllegalStateException.class,
                () -> PatternDslWeergave.regel(erna, naam -> null));
        assertEquals("de variabele $v is nog niet gebonden", weg.getMessage());
    }

    /** {@code jutterswet-gevuldepad} (a Jutter's boats, each unloading at one Bergplaats) with a third step, since no
     *  golden has a range of more steps with more than one intermediate: a Bergplaats lies in one Haven, and a sum of
     *  the havens' havengeld over the three steps, whose filter divides. The first step goes back from the Jutter and
     *  the two after it forward, so neither walk reaches the Boot or the Bergplaats. Verifier-clean. */
    static JsonNode havenketen() throws Exception {
        ObjectNode d = (ObjectNode) golden("jutterswet-gevuldepad");
        String getal = "{\"node\": \"Number\", \"whole\": true, \"decimals\": 0, \"sign\": \"nonNegative\"}";
        for (JsonNode ot : d.get("model").get("objectTypes"))
            if (ot.get("name").asText().equals("Jutter"))
                ((ArrayNode) ot.get("attributes")).add(JSON.readTree("{\"node\": \"Attribute\", \"name\":"
                        + " \"havengeldtotaal\", \"datatype\": " + getal + "}"));
        ((ArrayNode) d.get("model").get("objectTypes")).add(JSON.readTree("{\"node\": \"ObjectType\", \"name\": \"Haven\","
                + " \"animate\": false, \"plural\": \"Havens\", \"attributes\": [{\"node\": \"Attribute\", \"name\":"
                + " \"havengeld\", \"datatype\": " + getal + "}, {\"node\": \"Attribute\", \"name\": \"ligplaatsen\","
                + " \"datatype\": " + getal + "}], \"characteristics\": []}"));
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
        String pad = "[{\"node\": \"RoleBinding\", \"factType\": \"bemanning\", \"role\": \"gevoerde boot\"}, {\"node\":"
                + " \"RoleBinding\", \"factType\": \"lossing\", \"role\": \"losplaats\"}, {\"node\": \"RoleBinding\","
                + " \"factType\": \"ligging\", \"role\": \"haven\"}]";
        ((ArrayNode) d.get("rules").get("rules")).add(JSON.readTree("{\"node\": \"Rule\", \"name\": \"havengeldtotaal van"
                + " een Jutter\", \"versions\": [{\"validity\": {\"node\": \"Validity\"}, \"target\": "
                + ref("havengeldtotaal", "Jutter", "Het havengeldtotaal van een Jutter") + ", \"value\": {\"node\":"
                + " \"Aggregation\", \"kind\": \"sum\", \"arguments\": [" + ref("havengeld", "Haven", "het havengeld van"
                + " alle havens van alle losplaatsen van alle gevoerde boten van de Jutter") + "], \"elementType\":"
                + " \"Haven\", \"filter\": " + filter + ", \"sourcePath\": " + pad + ", \"type\": " + getal + ","
                + " \"source\": {\"snippet\": \"de som van het havengeld van alle havens van alle losplaatsen van alle"
                + " gevoerde boten van de Jutter die aan de volgende voorwaarde voldoen: zijn havengeld gedeeld door zijn"
                + " ligplaatsen is groter dan 2\"}}, \"initialisation\": false, \"presentation\": \"calculation\"}]}"));
        return d;
    }

    private static String ref(String attribuut, String type, String snippet) {
        return "{\"node\": \"AttributeReference\", \"attribute\": \"" + attribuut + "\", \"subjectType\": \"" + type
                + "\", \"source\": {\"snippet\": \"" + snippet + "\"}}";
    }

    /** The document of {@code TmsKwantorvouwWitnessTest} whose quantifier fold "geen van de wachten van de Ploeg"
     *  tests a matrix that divides by the members' deelnemers, a derived value, and so can fault and deposit, with the
     *  rule of the deelnemers before it. Taken from that witness by reflection, as its members are of its own package,
     *  so that the document has one source. */
    static JsonNode matrixDieDeelt() throws Exception {
        Class<?> k = Class.forName("dev.uchar.regelspraak.cases.TmsKwantorvouwWitnessTest");
        Method document = Class.forName("dev.uchar.regelspraak.cases.TmsDocumenten").getDeclaredMethod("document",
                String.class, List.class);
        document.setAccessible(true);
        Path ir = (Path) document.invoke(null, veld(k, "MODEL"), List.of(veld(k, "DEELNEMERS"), veld(k, "RUSTIG")));
        return JSON.readTree(ir.toFile());
    }

    private static String veld(Class<?> klasse, String naam) throws ReflectiveOperationException {
        Field f = klasse.getDeclaredField(naam);
        f.setAccessible(true);
        return (String) f.get(null);
    }

    static JsonNode golden(String naam) throws Exception {
        return JSON.readTree(GOLDEN.resolve(naam).resolve("ir.json").toFile());
    }

    /** Every plan of {@code document} of a rule of a fold, in the order the rules were built, as text; after a line that
     *  holds a term, the term node by node. */
    static List<String> plannen(JsonNode document) {
        List<String> uit = new ArrayList<>();
        for (Regelplan p : DroolsBackend.build(document, DroolsBackend.Branch.EXEC_MODEL).execModelTranspiler()
                .regelplannen()) {
            if (!(p.gevolg() instanceof Gevolg.Bijdrage || p.gevolg() instanceof Gevolg.Filterdeponering
                    || p.gevolg() instanceof Gevolg.Filterfout || p.gevolg() instanceof Gevolg.Lidfout))
                continue;
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
            case Niet n -> "niet " + patroon(n.patroon());
            case Bestaat b -> "bestaat binnen " + b.binnen() + " buiten " + b.buiten() + " "
                    + b.patronen().stream().map(VouwplanWitnessTest::patroon).toList();
            case Accumulatie a -> "accumulatie " + patroon(a.bron()) + " functies "
                    + a.functies().stream().map(f -> f.soort() + " " + f.bron() + " -> " + f.doel()).toList();
            case Toetsitem t -> "toets " + t.id() + " " + t.rol() + " sonde " + t.sonde() + " slots " + t.slots()
                    + " grootte " + t.grootte() + ": " + Termboom.boom(t.toets());
        };
    }

    private static String patroon(Patroon p) {
        return p.feitklasse().getSimpleName() + " " + p.naam() + " alfa " + alfa(p.alfa()) + " join "
                + joins(p.joins()) + " beta " + beta(p.betatoetsen()) + " bind " + bindingen(p.bindingen());
    }

    /** Each constraint between two objects with its id, its function, the variable on the other side and each of its
     *  constants with every field ({@link #constante}). */
    private static List<String> beta(List<Betatoets> beta) {
        return beta.stream().map(t -> t.id() + ": " + t.functie() + " " + t.doel()
                + t.constanten().stream().map(c -> " " + constante(c)).reduce("", String::concat)).toList();
    }

    /** A constant of the analysis with every field: its value, the class of its value (a list by the classes of its
     *  elements), its IR type and its source fragment, each null where the constant has none (fix round 1c-B2). */
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

    private static List<String> alfa(List<Alfa> alfa) {
        List<String> uit = new ArrayList<>();
        for (Alfa a : alfa)
            uit.add(a.id() + ": " + a.toetsen().stream().map(t -> velden(t.pad()) + " " + t.veld() + " " + t.vorm()
                    + " " + t.op() + (t.waarde() == null ? "" : " " + t.waarde())).toList());
        return uit;
    }

    private static List<String> bindingen(List<Binding> bindingen) {
        return bindingen.stream().map(b -> b.doel() + " = " + velden(b.pad()) + " " + veld(b.veld()) + " " + b.vorm()
                + (b.soort() == null ? "" : " " + b.soort().getSimpleName())).toList();
    }

    private static String velden(List<Binding.Veld> pad) {
        return pad.stream().map(VouwplanWitnessTest::veld).toList().toString();
    }

    private static String veld(Binding.Veld v) {
        return v.klasse().getSimpleName() + "." + v.naam();
    }

    /** The consequence {@code g} with every field, what a member contributes and its place spelled out, and each term
     *  as a tree and node by node. */
    private static void gevolg(Gevolg g, List<String> uit) {
        switch (g) {
            case Gevolg.Bijdrage b -> {
                uit.add("  gevolg Bijdrage [onderwerp=" + b.onderwerp() + ", vouw=" + b.vouw() + ", lid=" + b.lid()
                        + ", slots=" + b.slots() + ", grootte=" + b.grootte() + ", invoeging=" + b.invoeging() + "]");
                switch (b.inbreng()) {
                    case Gevolg.Inbreng.Telling t -> uit.add("    inbreng Telling");
                    case Gevolg.Inbreng.Lidwaarde l -> uit.add("    inbreng Lidwaarde [waarde=" + l.waarde() + ", totaal="
                            + l.totaal() + ", gevuld=" + l.gevuld() + "]");
                    case Gevolg.Inbreng.Oordeel o -> {
                        uit.add("    inbreng Oordeel matrix " + Termboom.boom(o.matrix()));
                        Termboom.velden("      matrix", o.matrix(), "0", uit);
                    }
                    case Gevolg.Inbreng.Lidtoets l -> {
                        uit.add("    inbreng Lidtoets [sonde=" + l.sonde() + ", plaats=" + plaats(l.plaats()) + "] matrix "
                                + Termboom.boom(l.matrix()));
                        Termboom.velden("      matrix", l.matrix(), "0", uit);
                    }
                }
            }
            case Gevolg.Filterdeponering f -> uit.add("  gevolg Filterdeponering [onderwerp=" + f.onderwerp() + ", vouw="
                    + f.vouw() + ", lid=" + f.lid() + ", invoeging=" + f.invoeging() + "]");
            case Gevolg.Filterfout f -> {
                uit.add("  gevolg Filterfout [onderwerp=" + f.onderwerp() + ", vouw=" + f.vouw() + ", lid=" + f.lid()
                        + ", plaats=" + plaats(f.plaats()) + ", slots=" + f.slots() + ", grootte=" + f.grootte()
                        + ", invoeging=" + f.invoeging() + "] filter " + Termboom.boom(f.filter()));
                Termboom.velden("    filter", f.filter(), "0", uit);
            }
            case Gevolg.Lidfout f -> uit.add("  gevolg Lidfout [onderwerp=" + f.onderwerp() + ", vouw=" + f.vouw()
                    + ", lid=" + f.lid() + ", plaats=" + plaats(f.plaats()) + ", voor=" + f.voor() + ", na=" + f.na()
                    + ", invoeging=" + f.invoeging() + "]");
            default -> throw new IllegalStateException("geen gevolg van een vouw: " + g);
        }
    }

    private static String plaats(Gevolg.Vouwplaats p) {
        return "selectie " + p.selectie() + " " + p.stappen().stream().map(s -> s.veld() + " " + s.aantal() + " "
                + s.plaats()).toList() + " " + p.reden();
    }
}
