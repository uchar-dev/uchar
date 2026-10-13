package dev.uchar.regelspraak.transpiler.tms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * and the rule of the distribution of an allocation. Each is a {@link Regelplan} whose left-hand side holds the
 * patterns, the existence, the negation, the accumulates and the constraints the form built itself before, with their
 * constraint ids in their order, among them an identity join on the subject pattern that reads a reference field on the
 * other side and a constraint between two keys by value; and whose consequence carries every decision as a field, with
 * what it computes as a route of the plan ({@code plan.Route}): the keys of a uniqueness, the fault of a key's form and
 * the facts of a distribution, which {@code Verdeling.bereken} computes. This witness pins those plans as text, per
 * plan in order: the name, the form and the group; the slots, each with its kind, its variable and its key; every item
 * of the left-hand side in order with its kind, its pattern class, its constraint ids, each field test with its path
 * and its form of reading, its joins with the reference field they read on the other side, its constraints by value
 * with their function, and its bindings with their path and form; and the kind of the consequence with every field. A
 * term, the test of a rule or a key component of a consequence, is pinned as a tree and on the lines after it node by
 * node with its source fragment and of a constant the class of its value and its whole IR type
 * ({@link Termboom#velden}).
 *
 * <ul>
 *   <li>{@code jutterswet-vondstnummer}: a consistency whose assertion is a comparison of dates, negated, and a
 *       uniqueness of one source and one component, without a scope.</li>
 *   <li>{@code jutterswet-keurspelling}: a uniqueness over an enumeration, whose key component is the label in its
 *       normalised form.</li>
 *   <li>The worked example of RUNTIME-SEMANTICS ch. 5.6 ({@link ToetstermWitnessTest#TOEZICHT}), since no golden tests
 *       a channel: a consistency and the two rules of the standing of its channel, which a rule reads.</li>
 *   <li>{@code anchor/jutterswet-kustregister}: a uniqueness scoped by a parent, and a union of two sources.</li>
 *   <li>{@code anchor/jutterswet-strandregister}: a uniqueness over a key on a timeline, whose key rule makes a key
 *       per value the timeline ever carries, beside the rule of the key's form.</li>
 *   <li>{@code jutterswet-rangmaximering}: an allocation in order with a weight, an ordering key, a cap and a rest,
 *       whose second case deposits on a recipient: the recipient rule after the source's pattern, joined to it by the
 *       recipient's reference field, and the rule of the distribution over the list of recipients.</li>
 *   <li>{@code jutterswet-bergersdeling} with a relation of two to-one roles ({@link #bronWijstNaarOntvanger}), since
 *       no golden has one: the join reads the reference field on the source. This document is also fired with a case
 *       of three sources, and its shares and rests pinned, since the plan names the field and only a firing shows that
 *       the rendering reads it.</li>
 *   <li>Document F of {@code TmsVerdelingWitnessTest} ({@link #ontvangerUitEenCreatie}), since no golden has one: an
 *       allocation over a relation a form creates, whose range is the existence of a link.</li>
 * </ul>
 *
 * <p>That the rendering of these plans builds the rules of before the step is what the harness {@code Vuurspoor}
 * shows, and the executable model of the corpus, which is equal as text; for the three documents outside the corpus
 * the executable model of before and after the step is equal as text as well.
 *
 * <p><b>Red</b> before the step: these forms built their rules themselves, so there was no plan of them, and this class
 * did not compile, since the plan had no join on the subject pattern, no constraint between two patterns by value and
 * no join that reads a field on the other side; with only the names of the plans pinned, none of the eight documents
 * had a plan of any of these rules.
 */
class KanaalplanWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void een_consistentie_en_een_uniciteit_zonder_scope() throws Exception {
        assertEquals(List.of(
                "regel aanmelding niet voor de vondstdag | vorm aanmelding niet voor de vondstdag | groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=vondst, naam=aanmelddatum,"
                        + " kenmerk=false]), 1 INVOER $lezing_1 (eigen ElementSleutel[objecttype=vondst,"
                        + " naam=vondstdag, kenmerk=false])]",
                "  onderwerp Vondst $onderwerp alfa [] join [] bind [$lezing_0 = [] Vondst.aanmelddatum WAARDE,"
                        + " $lezing_1 = [] Vondst.vondstdag WAARDE]",
                "  toets voorwaarde_0 VOORWAARDE sonde -1 slots [0, 1] grootte 2: NIET(DATUM_VERGELIJK(Lees 0, Lees 1,"
                        + " Const LATER_OF_GELIJK))",
                "    toets 0 Aanroep bron 'De aanmelddatum van een Vondst moet later of gelijk zijn aan de vondstdag"
                        + " van de Vondst'",
                "    toets 0.0 Aanroep bron 'De aanmelddatum van een Vondst moet later of gelijk zijn aan de vondstdag"
                        + " van de Vondst'",
                "    toets 0.0.0 Lees bron 'De aanmelddatum van een Vondst'",
                "    toets 0.0.1 Lees bron 'de vondstdag van de Vondst'",
                "    toets 0.0.2 Const Operator",
                "  gevolg Inconsistentie [onderwerp=$onderwerp, vorm=aanmelding niet voor de vondstdag,"
                        + " invoeging=LOGISCH]",
                "regel eenmalig vondstnummer: sleutel van Vondst 1 | vorm eenmalig vondstnummer | groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=vondst, naam=vondstnummer,"
                        + " kenmerk=false])]",
                "  onderwerp Vondst $onderwerp alfa [] join [] bind [$lezing_0 = [] Vondst.vondstnummer WAARDE]",
                "  toets voorwaarde_1 VORM sonde -1 slots [0] grootte 1: Samengesteld ALLE 0(LEEGTOETS(Lees 0, Const"
                        + " false))",
                "    toets 0 Samengesteld",
                "    toets 0.0 Aanroep",
                "    toets 0.0.0 Lees",
                "    toets 0.0.1 Const Boolean",
                "  gevolg Uniciteitssleutel [onderwerp=$onderwerp, uniciteit=eenmalig vondstnummer, bron=0,"
                        + " componenten=[Lees 0], tijdlijn=-1, scope=-1, slots=[0], grootte=1, invoeging=LOGISCH]",
                "    componenten 0 0 Lees",
                "regel eenmalig vondstnummer | vorm eenmalig vondstnummer | groep null",
                "  lezingen [0 BOTSINGEN $botsingen (botsingen)]",
                "  patroon Uniciteitssleutel $sleutel alfa [uniciteit_2: [[] uniciteit WAARDE GELIJK eenmalig"
                        + " vondstnummer]] join [] beta [] bind [$element = [] Uniciteitssleutel.element WAARDE]",
                "  accumulatie Uniciteitssleutel $ander alfa [uniciteit_3: [[] uniciteit WAARDE GELIJK eenmalig"
                        + " vondstnummer]] join [] beta [botsing_4: BOTST_MET $sleutel] bind [] functies [AANTAL null"
                        + " -> $botsingen]",
                "  toets dubbel_5 VORM sonde -1 slots [0] grootte 1: DUBBEL(Lees 0)",
                "    toets 0 Aanroep",
                "    toets 0.0 Lees",
                "  gevolg Uniciteitsschending [onderwerp=$element, uniciteit=eenmalig vondstnummer, invoeging=LOGISCH]",
                "regel eenmalig vondstnummer: kanaal | vorm eenmalig vondstnummer | groep null",
                "  lezingen []",
                "  patroon Uniciteitsschending $schending alfa [schending_6: [[] uniciteit WAARDE GELIJK eenmalig"
                        + " vondstnummer]] join [] beta [] bind [$subject = [] Uniciteitsschending.element WAARDE]",
                "  gevolg Inconsistentie [onderwerp=$subject, vorm=eenmalig vondstnummer, invoeging=LOGISCH]"),
                plannen(golden("jutterswet-vondstnummer"), List.of("aanmelding niet voor de vondstdag",
                        "eenmalig vondstnummer")));
    }

    @Test void een_uniciteit_over_een_enumeratie() throws Exception {
        assertEquals(List.of(
                "regel eenmalige staat: sleutel van Vondst 1 | vorm eenmalige staat | groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=vondst, naam=staat, kenmerk=false])]",
                "  onderwerp Vondst $onderwerp alfa [] join [] bind [$lezing_0 = [] Vondst.staat WAARDE]",
                "  toets voorwaarde_5 VORM sonde -1 slots [0] grootte 1: Samengesteld ALLE 0(LEEGTOETS(Lees 0, Const"
                        + " false))",
                "    toets 0 Samengesteld",
                "    toets 0.0 Aanroep",
                "    toets 0.0.0 Lees",
                "    toets 0.0.1 Const Boolean",
                "  gevolg Uniciteitssleutel [onderwerp=$onderwerp, uniciteit=eenmalige staat, bron=0,"
                        + " componenten=[RS_ENUM_NAAM(Lees 0)], tijdlijn=-1, scope=-1, slots=[0], grootte=1,"
                        + " invoeging=LOGISCH]",
                "    componenten 0 0 Aanroep",
                "    componenten 0 0.0 Lees",
                "regel eenmalige staat | vorm eenmalige staat | groep null",
                "  lezingen [0 BOTSINGEN $botsingen (botsingen)]",
                "  patroon Uniciteitssleutel $sleutel alfa [uniciteit_6: [[] uniciteit WAARDE GELIJK eenmalige staat]]"
                        + " join [] beta [] bind [$element = [] Uniciteitssleutel.element WAARDE]",
                "  accumulatie Uniciteitssleutel $ander alfa [uniciteit_7: [[] uniciteit WAARDE GELIJK eenmalige"
                        + " staat]] join [] beta [botsing_8: BOTST_MET $sleutel] bind [] functies [AANTAL null ->"
                        + " $botsingen]",
                "  toets dubbel_9 VORM sonde -1 slots [0] grootte 1: DUBBEL(Lees 0)",
                "    toets 0 Aanroep",
                "    toets 0.0 Lees",
                "  gevolg Uniciteitsschending [onderwerp=$element, uniciteit=eenmalige staat, invoeging=LOGISCH]",
                "regel eenmalige staat: kanaal | vorm eenmalige staat | groep null",
                "  lezingen []",
                "  patroon Uniciteitsschending $schending alfa [schending_10: [[] uniciteit WAARDE GELIJK eenmalige"
                        + " staat]] join [] beta [] bind [$subject = [] Uniciteitsschending.element WAARDE]",
                "  gevolg Inconsistentie [onderwerp=$subject, vorm=eenmalige staat, invoeging=LOGISCH]"),
                plannen(golden("jutterswet-keurspelling"), List.of("eenmalige staat")));
    }

    @Test void een_consistentie_en_de_twee_kanaalstanden() throws Exception {
        assertEquals(List.of(
                "regel controle saldo | vorm controle saldo | groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=rekeninghouder, naam=saldo,"
                        + " kenmerk=false])]",
                "  onderwerp Rekeninghouder $onderwerp alfa [] join [] bind [$lezing_0 = [] Rekeninghouder.saldo"
                        + " WAARDE]",
                "  toets voorwaarde_2 VOORWAARDE sonde -1 slots [0] grootte 1: NIET(RS_AGG_VERGELIJK(Lees 0, Const 0,"
                        + " Const >=))",
                "    toets 0 Aanroep bron 'zijn saldo is groter dan of gelijk aan 0'",
                "    toets 0.0 Aanroep bron 'zijn saldo is groter dan of gelijk aan 0'",
                "    toets 0.0.0 Lees bron 'zijn saldo'",
                "    toets 0.0.1 Const BigDecimal type Numeriek[geheel=true, decimalen=0, teken=ONBEPAALD,"
                        + " eenheid=null] bron '0'",
                "    toets 0.0.2 Const String",
                "  gevolg Inconsistentie [onderwerp=$onderwerp, vorm=controle saldo, invoeging=LOGISCH]",
                "regel kanaalstand controle saldo: inconsistent | vorm null | groep null",
                "  lezingen []",
                "  bestaat [Inconsistentie $inconsistentie alfa [kanaal_3: [[] regel WAARDE GELIJK controle saldo]]"
                        + " join [] beta [] bind []]",
                "  gevolg Kanaalstand [producent=controle saldo, inconsistent=true, invoeging=LOGISCH]",
                "regel kanaalstand controle saldo: consistent | vorm null | groep null",
                "  lezingen []",
                "  niet Inconsistentie $inconsistentie alfa [kanaal_4: [[] regel WAARDE GELIJK controle saldo]] join"
                        + " [] beta [] bind []",
                "  gevolg Kanaalstand [producent=controle saldo, inconsistent=false, invoeging=LOGISCH]"),
                plannen(document(ToetstermWitnessTest.TOEZICHT), List.of("controle saldo")));
    }

    @Test void een_uniciteit_met_scope_en_een_unie() throws Exception {
        assertEquals(List.of(
                "regel eenmalig kaveletiket: sleutel van Kavel 1 | vorm eenmalig kaveletiket | groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=kavel, naam=etiket, kenmerk=false]),"
                        + " 1 SCOPE $lezing_1 (scope kavelindeling gemeente)]",
                "  onderwerp Kavel $onderwerp alfa [] join [] bind [$lezing_0 = [] Kavel.etiket WAARDE, $lezing_1 = []"
                        + " Kavel.gemeente WAARDE]",
                "  toets voorwaarde_5 VORM sonde -1 slots [0, 1] grootte 2: Samengesteld ALLE 0(LEEGTOETS(Lees 0,"
                        + " Const false), LEEGTOETS(Lees 1, Const false))",
                "    toets 0 Samengesteld",
                "    toets 0.0 Aanroep",
                "    toets 0.0.0 Lees",
                "    toets 0.0.1 Const Boolean",
                "    toets 0.1 Aanroep",
                "    toets 0.1.0 Lees",
                "    toets 0.1.1 Const Boolean",
                "  gevolg Uniciteitssleutel [onderwerp=$onderwerp, uniciteit=eenmalig kaveletiket, bron=0,"
                        + " componenten=[Lees 0], tijdlijn=-1, scope=1, slots=[0, 1], grootte=2, invoeging=LOGISCH]",
                "    componenten 0 0 Lees",
                "regel eenmalig kaveletiket | vorm eenmalig kaveletiket | groep null",
                "  lezingen [0 BOTSINGEN $botsingen (botsingen)]",
                "  patroon Uniciteitssleutel $sleutel alfa [uniciteit_6: [[] uniciteit WAARDE GELIJK eenmalig"
                        + " kaveletiket]] join [] beta [] bind [$element = [] Uniciteitssleutel.element WAARDE]",
                "  accumulatie Uniciteitssleutel $ander alfa [uniciteit_7: [[] uniciteit WAARDE GELIJK eenmalig"
                        + " kaveletiket]] join [] beta [botsing_8: BOTST_MET $sleutel] bind [] functies [AANTAL null"
                        + " -> $botsingen]",
                "  toets dubbel_9 VORM sonde -1 slots [0] grootte 1: DUBBEL(Lees 0)",
                "    toets 0 Aanroep",
                "    toets 0.0 Lees",
                "  gevolg Uniciteitsschending [onderwerp=$element, uniciteit=eenmalig kaveletiket, invoeging=LOGISCH]",
                "regel eenmalig kaveletiket: kanaal | vorm eenmalig kaveletiket | groep null",
                "  lezingen []",
                "  patroon Uniciteitsschending $schending alfa [schending_10: [[] uniciteit WAARDE GELIJK eenmalig"
                        + " kaveletiket]] join [] beta [] bind [$subject = [] Uniciteitsschending.element WAARDE]",
                "  gevolg Inconsistentie [onderwerp=$subject, vorm=eenmalig kaveletiket, invoeging=LOGISCH]",
                "regel eenmalig vondstnummer: sleutel van Strandvondst 1 | vorm eenmalig vondstnummer | groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=strandvondst, naam=vondstnummer,"
                        + " kenmerk=false])]",
                "  onderwerp Strandvondst $onderwerp alfa [] join [] bind [$lezing_0 = [] Strandvondst.vondstnummer"
                        + " WAARDE]",
                "  toets voorwaarde_17 VORM sonde -1 slots [0] grootte 1: Samengesteld ALLE 0(LEEGTOETS(Lees 0, Const"
                        + " false))",
                "    toets 0 Samengesteld",
                "    toets 0.0 Aanroep",
                "    toets 0.0.0 Lees",
                "    toets 0.0.1 Const Boolean",
                "  gevolg Uniciteitssleutel [onderwerp=$onderwerp, uniciteit=eenmalig vondstnummer, bron=0,"
                        + " componenten=[Lees 0], tijdlijn=-1, scope=-1, slots=[0], grootte=1, invoeging=LOGISCH]",
                "    componenten 0 0 Lees",
                "regel eenmalig vondstnummer: sleutel van Wrakvondst 2 | vorm eenmalig vondstnummer | groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=wrakvondst, naam=vondstnummer,"
                        + " kenmerk=false])]",
                "  onderwerp Wrakvondst $onderwerp alfa [] join [] bind [$lezing_0 = [] Wrakvondst.vondstnummer"
                        + " WAARDE]",
                "  toets voorwaarde_18 VORM sonde -1 slots [0] grootte 1: Samengesteld ALLE 0(LEEGTOETS(Lees 0, Const"
                        + " false))",
                "    toets 0 Samengesteld",
                "    toets 0.0 Aanroep",
                "    toets 0.0.0 Lees",
                "    toets 0.0.1 Const Boolean",
                "  gevolg Uniciteitssleutel [onderwerp=$onderwerp, uniciteit=eenmalig vondstnummer, bron=1,"
                        + " componenten=[Lees 0], tijdlijn=-1, scope=-1, slots=[0], grootte=1, invoeging=LOGISCH]",
                "    componenten 0 0 Lees",
                "regel eenmalig vondstnummer | vorm eenmalig vondstnummer | groep null",
                "  lezingen [0 BOTSINGEN $botsingen (botsingen)]",
                "  patroon Uniciteitssleutel $sleutel alfa [uniciteit_19: [[] uniciteit WAARDE GELIJK eenmalig"
                        + " vondstnummer]] join [] beta [] bind [$element = [] Uniciteitssleutel.element WAARDE]",
                "  accumulatie Uniciteitssleutel $ander alfa [uniciteit_20: [[] uniciteit WAARDE GELIJK eenmalig"
                        + " vondstnummer]] join [] beta [botsing_21: BOTST_MET $sleutel] bind [] functies [AANTAL null"
                        + " -> $botsingen]",
                "  toets dubbel_22 VORM sonde -1 slots [0] grootte 1: DUBBEL(Lees 0)",
                "    toets 0 Aanroep",
                "    toets 0.0 Lees",
                "  gevolg Uniciteitsschending [onderwerp=$element, uniciteit=eenmalig vondstnummer, invoeging=LOGISCH]",
                "regel eenmalig vondstnummer: kanaal | vorm eenmalig vondstnummer | groep null",
                "  lezingen []",
                "  patroon Uniciteitsschending $schending alfa [schending_23: [[] uniciteit WAARDE GELIJK eenmalig"
                        + " vondstnummer]] join [] beta [] bind [$subject = [] Uniciteitsschending.element WAARDE]",
                "  gevolg Inconsistentie [onderwerp=$subject, vorm=eenmalig vondstnummer, invoeging=LOGISCH]"),
                plannen(golden("anchor/jutterswet-kustregister"), List.of("eenmalig kaveletiket",
                        "eenmalig vondstnummer")));
    }

    @Test void een_sleutel_op_een_tijdlijn_en_de_sleutelvormregel() throws Exception {
        assertEquals(List.of(
                "regel eenmalig nummer: sleutel van Strandvondst 1 | vorm eenmalig nummer | groep null",
                "  lezingen [0 TIJDLIJN $tijdlijn_0 (tijdlijn ElementSleutel[objecttype=strandvondst,"
                        + " naam=vondstnummer, kenmerk=false])]",
                "  onderwerp Strandvondst $onderwerp alfa [] join [] bind []",
                "  patroon Stand $stand_0 alfa [stand_0: [[] attribuut WAARDE GELIJK"
                        + " ElementSleutel[objecttype=strandvondst, naam=vondstnummer, kenmerk=false]]] join"
                        + " [stand-onderwerp_1: onderwerp = $onderwerp] beta [] bind [$tijdlijn_0 = [] Stand.waarde"
                        + " WAARDE]",
                "  toets voorwaarde_2 VORM sonde -1 slots [0] grootte 1: Samengesteld ALLE 0(EMMERSCALAIR(Lees 0,"
                        + " Const false))",
                "    toets 0 Samengesteld",
                "    toets 0.0 Aanroep",
                "    toets 0.0.0 Lees",
                "    toets 0.0.1 Const Boolean",
                "  gevolg Uniciteitssleutel [onderwerp=$onderwerp, uniciteit=eenmalig nummer, bron=0,"
                        + " componenten=[Lees 0], tijdlijn=0, scope=-1, slots=[0], grootte=1, invoeging=LOGISCH]",
                "    componenten 0 0 Lees",
                "regel eenmalig nummer: sleutelvorm van Strandvondst 1 | vorm eenmalig nummer | groep null",
                "  lezingen [0 TIJDLIJN $tijdlijn_0 (tijdlijn ElementSleutel[objecttype=strandvondst,"
                        + " naam=vondstnummer, kenmerk=false])]",
                "  onderwerp Strandvondst $onderwerp alfa [] join [] bind []",
                "  patroon Stand $stand_0 alfa [stand_3: [[] attribuut WAARDE GELIJK"
                        + " ElementSleutel[objecttype=strandvondst, naam=vondstnummer, kenmerk=false]]] join"
                        + " [stand-onderwerp_4: onderwerp = $onderwerp] beta [] bind [$tijdlijn_0 = [] Stand.waarde"
                        + " WAARDE]",
                "  toets voorwaarde_5 VORM sonde -1 slots [0] grootte 1: Samengesteld ALLE 0(EMMERSCALAIR(Lees 0,"
                        + " Const true))",
                "    toets 0 Samengesteld",
                "    toets 0.0 Aanroep",
                "    toets 0.0.0 Lees",
                "    toets 0.0.1 Const Boolean",
                "  gevolg Invoervormfout [onderwerp=$onderwerp, voor=the ever-unique key \"vondstnummer\" needs the"
                        + " full timeline; , na= supplies only a scalar (ch. 5.9), fout=Foutnaam[vorm=eenmalig nummer,"
                        + " regel=eenmalig nummer], invoeging=LOGISCH]",
                "regel eenmalig nummer | vorm eenmalig nummer | groep null",
                "  lezingen [0 BOTSINGEN $botsingen (botsingen)]",
                "  patroon Uniciteitssleutel $sleutel alfa [uniciteit_6: [[] uniciteit WAARDE GELIJK eenmalig nummer]]"
                        + " join [] beta [] bind [$element = [] Uniciteitssleutel.element WAARDE]",
                "  accumulatie Uniciteitssleutel $ander alfa [uniciteit_7: [[] uniciteit WAARDE GELIJK eenmalig"
                        + " nummer]] join [] beta [botsing_8: BOTST_MET $sleutel] bind [] functies [AANTAL null ->"
                        + " $botsingen]",
                "  toets dubbel_9 VORM sonde -1 slots [0] grootte 1: DUBBEL(Lees 0)",
                "    toets 0 Aanroep",
                "    toets 0.0 Lees",
                "  gevolg Uniciteitsschending [onderwerp=$element, uniciteit=eenmalig nummer, invoeging=LOGISCH]",
                "regel eenmalig nummer: kanaal | vorm eenmalig nummer | groep null",
                "  lezingen []",
                "  patroon Uniciteitsschending $schending alfa [schending_10: [[] uniciteit WAARDE GELIJK eenmalig"
                        + " nummer]] join [] beta [] bind [$subject = [] Uniciteitsschending.element WAARDE]",
                "  gevolg Inconsistentie [onderwerp=$subject, vorm=eenmalig nummer, invoeging=LOGISCH]"),
                plannen(golden("anchor/jutterswet-strandregister"), List.of("eenmalig nummer")));
    }

    @Test void een_verdeling_met_ontvangers_rest_en_een_deponering() throws Exception {
        assertEquals(List.of(
                "regel verdeel het penningbudget van een penningkas: ontvanger | vorm verdeel het penningbudget van"
                        + " een penningkas | groep null",
                "  lezingen [0 ONDERWERP $onderwerp (onderwerp zelf), 1 INVOER $lezing_1 (eigen"
                        + " ElementSleutel[objecttype=wachter, naam=voorranggewicht, kenmerk=false]), 2 INVOER"
                        + " $lezing_2 (eigen ElementSleutel[objecttype=wachter, naam=wachtjaar, kenmerk=false]), 3"
                        + " INVOER $lezing_3 (eigen ElementSleutel[objecttype=wachter, naam=penningmaximum,"
                        + " kenmerk=false])]",
                "  patroon Penningkas $vouwonderwerp alfa [] join [] beta [] bind []",
                "  onderwerp Wachter $onderwerp alfa [] join [verdelingsbereik_0: penningkas = $vouwonderwerp] bind"
                        + " [$lezing_1 = [] Wachter.voorranggewicht WAARDE, $lezing_2 = [] Wachter.wachtjaar WAARDE,"
                        + " $lezing_3 = [] Wachter.penningmaximum WAARDE]",
                "  gevolg Ontvanger [onderwerp=$vouwonderwerp, verdeling=verdeel het penningbudget van een penningkas,"
                        + " ontvanger=0, gewicht=1, sleutel=2, maximum=3, invoeging=LOGISCH]",
                "regel verdeel het penningbudget van een penningkas | vorm verdeel het penningbudget van een"
                        + " penningkas | groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=penningkas, naam=penningbudget,"
                        + " kenmerk=false]), 1 ONTVANGERS $ontvangers_verdeling_2 (ontvangers verdeel het"
                        + " penningbudget van een penningkas)]",
                "  onderwerp Penningkas $onderwerp alfa [] join [] bind [$lezing_0 = [] Penningkas.penningbudget"
                        + " WAARDE]",
                "  accumulatie Ontvanger $ontvanger_verdeling_1 alfa [ontvanger_3: [[] verdeling WAARDE GELIJK verdeel"
                        + " het penningbudget van een penningkas]] join [ontvanger-bron_4: bron = $onderwerp] beta []"
                        + " bind [] functies [LIJST null -> $ontvangers_verdeling_2]",
                "  gevolg Uitdeling [onderwerp=$onderwerp, vorm=verdeel het penningbudget van een penningkas,"
                        + " methode=OP_VOLGORDE, pot=0, lijst=1, gewogen=true, gemaximeerd=true, afnemend=false,"
                        + " decimalen=0, aandeel=ElementSleutel[objecttype=wachter, naam=penningtoewijzing,"
                        + " kenmerk=false], rest=ElementSleutel[objecttype=penningkas, naam=onuitgekeerd budget,"
                        + " kenmerk=false], schrijver=verdeel het penningbudget van een penningkas,"
                        + " invoeging=LOGISCH]"),
                plannen(golden("jutterswet-rangmaximering"),
                        List.of("verdeel het penningbudget van een penningkas")));
    }

    @Test void een_verdeling_waarvan_de_bron_naar_de_ontvanger_wijst() throws Exception {
        assertEquals(List.of(
                "regel verdeel de opbrengst van een berging: ontvanger | vorm verdeel de opbrengst van een berging |"
                        + " groep null",
                "  lezingen [0 ONDERWERP $onderwerp (onderwerp zelf)]",
                "  patroon Berging $vouwonderwerp alfa [] join [] beta [] bind []",
                "  onderwerp Hoofdberger $onderwerp alfa [] join [verdelingsbereik_0: null ="
                        + " $vouwonderwerp.Berging.berger] bind []",
                "  gevolg Ontvanger [onderwerp=$vouwonderwerp, verdeling=verdeel de opbrengst van een berging,"
                        + " ontvanger=0, gewicht=-1, sleutel=-1, maximum=-1, invoeging=LOGISCH]",
                "regel verdeel de opbrengst van een berging | vorm verdeel de opbrengst van een berging | groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=berging, naam=opbrengst,"
                        + " kenmerk=false]), 1 ONTVANGERS $ontvangers_verdeling_2 (ontvangers verdeel de opbrengst van"
                        + " een berging)]",
                "  onderwerp Berging $onderwerp alfa [] join [] bind [$lezing_0 = [] Berging.opbrengst WAARDE]",
                "  accumulatie Ontvanger $ontvanger_verdeling_1 alfa [ontvanger_3: [[] verdeling WAARDE GELIJK verdeel"
                        + " de opbrengst van een berging]] join [ontvanger-bron_4: bron = $onderwerp] beta [] bind []"
                        + " functies [LIJST null -> $ontvangers_verdeling_2]",
                "  gevolg Uitdeling [onderwerp=$onderwerp, vorm=verdeel de opbrengst van een berging,"
                        + " methode=GELIJKE_DELEN, pot=0, lijst=1, gewogen=false, gemaximeerd=false, afnemend=false,"
                        + " decimalen=0, aandeel=ElementSleutel[objecttype=berger, naam=bergingsaandeel,"
                        + " kenmerk=false], rest=ElementSleutel[objecttype=berging, naam=onverdeelde rest,"
                        + " kenmerk=false], schrijver=verdeel de opbrengst van een berging, invoeging=LOGISCH]"),
                plannen(bronWijstNaarOntvanger(), List.of("verdeel de opbrengst van een berging")));
    }

    /** The case of {@link #bronWijstNaarOntvanger}: three sources, each naming its one berger. The recipients are the
     *  hoofdbergers, so B1 and B3 give their whole yield to H1 and H2, with a rest of 0, and B2, whose berger G1 is no
     *  hoofdberger, has no recipient and distributes nothing (ch. 4.4). The reference interpreter and the ASP backend
     *  both pass these seven expectations. */
    private static final String BRON_WIJST_NAAR_ONTVANGER_CASE = """
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
    @Test void een_verdeling_waarvan_de_bron_naar_de_ontvanger_wijst_vuurt(@TempDir Path map) throws Exception {
        Path ir = map.resolve("ir.json");
        Files.writeString(ir, JSON.writeValueAsString(bronWijstNaarOntvanger()));
        assertNull(VouwplanWitnessTest.speelCase(ir, JSON.readTree(BRON_WIJST_NAAR_ONTVANGER_CASE)));
    }

    @Test void een_verdeling_over_een_relatie_die_een_creatie_maakt() throws Exception {
        assertEquals(List.of(
                "regel verdeel de opbrengst gelijk: ontvanger | vorm verdeel de opbrengst gelijk | groep null",
                "  lezingen [0 ONDERWERP $onderwerp (onderwerp zelf)]",
                "  patroon Berging $vouwonderwerp alfa [] join [] beta [] bind []",
                "  onderwerp Berger $onderwerp alfa [] join [] bind []",
                "  bestaat [Koppeling $koppeling_bereik_0 alfa [bereik-koppeling_1: [[] relatie WAARDE GELIJK"
                        + " bergingsdeelname]] join [bereik-lid_2: tweede = $onderwerp, bereik-drager_3: eerste ="
                        + " $vouwonderwerp] beta [] bind []]",
                "  gevolg Ontvanger [onderwerp=$vouwonderwerp, verdeling=verdeel de opbrengst gelijk, ontvanger=0,"
                        + " gewicht=-1, sleutel=-1, maximum=-1, invoeging=LOGISCH]",
                "regel verdeel de opbrengst gelijk | vorm verdeel de opbrengst gelijk | groep null",
                "  lezingen [0 INVOER $lezing_0 (eigen ElementSleutel[objecttype=berging, naam=opbrengst,"
                        + " kenmerk=false]), 1 ONTVANGERS $ontvangers_verdeling_5 (ontvangers verdeel de opbrengst"
                        + " gelijk)]",
                "  onderwerp Berging $onderwerp alfa [] join [] bind [$lezing_0 = [] Berging.opbrengst WAARDE]",
                "  accumulatie Ontvanger $ontvanger_verdeling_4 alfa [ontvanger_6: [[] verdeling WAARDE GELIJK verdeel"
                        + " de opbrengst gelijk]] join [ontvanger-bron_7: bron = $onderwerp] beta [] bind [] functies"
                        + " [LIJST null -> $ontvangers_verdeling_5]",
                "  gevolg Uitdeling [onderwerp=$onderwerp, vorm=verdeel de opbrengst gelijk, methode=GELIJKE_DELEN,"
                        + " pot=0, lijst=1, gewogen=false, gemaximeerd=false, afnemend=false, decimalen=0,"
                        + " aandeel=ElementSleutel[objecttype=berger, naam=bergingsaandeel, kenmerk=false],"
                        + " rest=ElementSleutel[objecttype=berging, naam=onverdeelde rest, kenmerk=false],"
                        + " schrijver=verdeel de opbrengst gelijk, invoeging=LOGISCH]"),
                plannen(ontvangerUitEenCreatie(), List.of("verdeel de opbrengst gelijk")));
    }

    /** {@code jutterswet-bergersdeling} with both roles of its relation to-one, since no golden has an allocation whose
     *  recipient role is to-one: the source's bean then holds the reference to its one recipient, so the recipient
     *  rule joins the subject itself to that reference field of the source. Verifier-clean. */
    static JsonNode bronWijstNaarOntvanger() throws Exception {
        JsonNode d = golden("jutterswet-bergersdeling");
        for (JsonNode rol : d.get("model").get("factTypes").get(0).get("roles")) {
            ((ObjectNode) rol).put("many", false);
            ((ObjectNode) rol).remove("plural");
        }
        return d;
    }

    /** Document F of {@code TmsVerdelingWitnessTest}, which plays it: an allocation whose recipients an object creation
     *  adds over the allocation's connecting relation, so its range is a relation a form creates. Taken from that
     *  witness by reflection, as its members are of its own package, so that the document has one source. */
    static JsonNode ontvangerUitEenCreatie() throws Exception {
        Class<?> f = Class.forName("dev.uchar.regelspraak.cases.TmsVerdelingWitnessTest");
        Method ir = f.getDeclaredMethod("ir", List.class, List.class, List.class);
        ir.setAccessible(true);
        return document((String) ir.invoke(null, List.of(), List.of(veld(f, "F_GELIJK")), List.of(veld(f,
                "HULPBERGER"))));
    }

    private static String veld(Class<?> klasse, String naam) throws ReflectiveOperationException {
        Field f = klasse.getDeclaredField(naam);
        f.setAccessible(true);
        return (String) f.get(null);
    }

    /** Every plan of {@code document} of a form named in {@code vormen}, and of the standing of its channel, in the
     *  order the rules were built, as text; after a line that holds a term, the term node by node. */
    static List<String> plannen(JsonNode document, List<String> vormen) throws ReflectiveOperationException {
        List<String> uit = new ArrayList<>();
        for (Regelplan p : DroolsBackend.build(document, DroolsBackend.Branch.EXEC_MODEL).execModelTranspiler()
                .regelplannen()) {
            if (vormen.stream().noneMatch(v -> p.naam().equals(v) || p.naam().startsWith(v + ": ")
                    || p.naam().startsWith("kanaalstand " + v + ": ")))
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
            uit.add("  gevolg " + gevolg(p.gevolg()));
            for (RecordComponent c : p.gevolg().getClass().getRecordComponents()) {
                Object w = c.getAccessor().invoke(p.gevolg());
                if (w instanceof Term t) Termboom.velden("    " + c.getName(), t, "0", uit);
                if (w instanceof List<?> l)
                    for (int i = 0; i < l.size(); i++)
                        if (l.get(i) instanceof Term t) Termboom.velden("    " + c.getName() + " " + i, t, "0", uit);
            }
        }
        return uit;
    }

    static JsonNode golden(String naam) throws Exception {
        return JSON.readTree(GOLDEN.resolve(naam).resolve("ir.json").toFile());
    }

    static JsonNode document(String ir) throws Exception {
        return JSON.readTree(ir);
    }

    private static String item(LhsItem item) {
        return switch (item) {
            case Onderwerp o -> "onderwerp " + o.bonenklasse().getSimpleName() + " " + o.naam() + " alfa "
                    + alfa(o.alfa()) + " join " + joins(o.joins()) + " bind " + bindingen(o.bindingen());
            case Patroon p -> "patroon " + patroon(p);
            case Niet n -> "niet " + patroon(n.patroon());
            case Bestaat b -> "bestaat " + b.patronen().stream().map(KanaalplanWitnessTest::patroon).toList();
            case Accumulatie a -> "accumulatie " + patroon(a.bron()) + " functies "
                    + a.functies().stream().map(f -> f.soort() + " " + f.bron() + " -> " + f.doel()).toList();
            case Toetsitem t -> "toets " + t.id() + " " + t.rol() + " sonde " + t.sonde() + " slots " + t.slots()
                    + " grootte " + t.grootte() + ": " + Termboom.boom(t.toets());
        };
    }

    private static String patroon(Patroon p) {
        return p.feitklasse().getSimpleName() + " " + p.naam() + " alfa " + alfa(p.alfa()) + " join "
                + joins(p.joins()) + " beta " + p.betatoetsen().stream().map(t -> t.id() + ": " + t.functie() + " "
                + t.doel()).toList() + " bind " + bindingen(p.bindingen());
    }

    /** Each identity join with its id: the field of the pattern's object (null: the object itself), and the variable
     *  on the other side with the reference field read there, where it reads one. */
    private static List<String> joins(List<Join> joins) {
        return joins.stream().map(j -> j.id() + ": " + j.veld() + " = " + j.doel()
                + (j.doelveld() == null ? "" : "." + veld(j.doelveld()))).toList();
    }

    /** Each constraint with its id and its field tests: the path, the field (null: the object itself), the form of
     *  reading, the comparison and the value. */
    private static List<String> alfa(List<Alfa> alfa) {
        List<String> uit = new ArrayList<>();
        for (Alfa a : alfa)
            uit.add(a.id() + ": " + a.toetsen().stream().map(t -> velden(t.pad()) + " " + t.veld() + " " + t.vorm()
                    + " " + t.op() + (t.waarde() == null ? "" : " " + t.waarde())).toList());
        return uit;
    }

    /** Each binding with its variable, its path, its field on its class and its form of reading. */
    private static List<String> bindingen(List<Binding> bindingen) {
        return bindingen.stream().map(b -> b.doel() + " = " + velden(b.pad()) + " " + veld(b.veld()) + " " + b.vorm()
                + (b.soort() == null ? "" : " " + b.soort().getSimpleName())).toList();
    }

    private static String velden(List<Binding.Veld> pad) {
        return pad.stream().map(KanaalplanWitnessTest::veld).toList().toString();
    }

    private static String veld(Binding.Veld v) {
        return v.klasse().getSimpleName() + "." + v.naam();
    }

    /** The kind of {@code g} and every field of it, a term as a tree. */
    private static String gevolg(Gevolg g) throws ReflectiveOperationException {
        List<String> velden = new ArrayList<>();
        for (RecordComponent c : g.getClass().getRecordComponents()) {
            Object w = c.getAccessor().invoke(g);
            velden.add(c.getName() + "=" + (w instanceof Term t ? Termboom.boom(t) : w instanceof List<?> l
                    && !l.isEmpty() && l.get(0) instanceof Term ? l.stream().map(x -> Termboom.boom((Term) x)).toList()
                    : String.valueOf(w)));
        }
        return g.getClass().getSimpleName() + " " + velden;
    }
}
