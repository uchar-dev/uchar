package dev.uchar.regelspraak.transpiler.tms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import dev.uchar.regelspraak.transpiler.tms.plan.Toetsitem;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The standing rules of the exec-model branch come whole from a plan (card drl-teksttak-op-truth-maintenance, step
 * 1c-A2): the three rules of a characteristic's {@code KenmerkStand}, the rules of an attribute's {@code Stand} from a
 * derivation and from the input, for a member on a timeline also as its asOfDate-bucket scalar, and the two rules of a
 * {@code PadStand}. Each is a {@link Regelplan} whose left-hand side holds the patterns, the negations and the
 * existence that the rule built itself before, with their constraint ids in their order, and whose consequence says
 * where the standing comes from as a field ({@link Gevolg.Standbron}). Every read of a bean names its field, the path
 * of reference fields it walks, and its form of reading ({@link Binding.Leesvorm}): the decision of the analysis, which
 * a rendering only executes. This witness pins the standing plans of two documents as text, per plan: the name; the
 * slots, each with its kind, its variable and its key; every item of the left-hand side in order with its kind, its
 * constraint ids, each field test with its path and its form of reading, its joins, and its bindings with their path
 * and form; and the consequence with every field.
 *
 * <ul>
 *   <li>{@code jutterswet-penningjaar}: a characteristic read through its standing, waar from the input, waar from a
 *       derivation and onwaar; an attribute a form derives, its standing from the derivation and from the input as it
 *       is; and an attribute on a timeline that no form writes, whose input stands as its segments and, where the case
 *       gives the asOfDate bucket, as that scalar.</li>
 *   <li>{@code jutterswet-premiestaffel}: two paths of input links, of two and of three hops, to a derived attribute,
 *       with the link present and the link absent, beside the standings of that attribute and of a
 *       characteristic.</li>
 * </ul>
 *
 * <p>That the rendering of these plans builds the rules of before the step is what the harness {@code Vuurspoor} shows,
 * and the executable model of the corpus, which is equal as text.
 *
 * <p><b>Red</b> before the step: the standing rules built their rules themselves and a field test named only a field of
 * its own object, so there was no plan of them, and this class did not compile.
 */
class StandplanWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void kenmerk_attribuut_en_tijdlijn_met_emmerscalair() throws Exception {
        assertEquals(List.of(
                "regel stand Jutter.aantal aangemelde vondsten: uit de invoer | vorm null | groep null",
                "  lezingen [0 INVOER $invoer (invoer ElementSleutel[objecttype=jutter, naam=aantal aangemelde"
                        + " vondsten, kenmerk=false])]",
                "  onderwerp Jutter $onderwerp alfa [] bind [$invoer = [] Jutter.aantal aangemelde vondsten"
                        + " TIJDVAKKEN]",
                "  niet Emmerinvoer $emmerinvoer alfa [emmerinvoer_9: [[] attribuut WAARDE GELIJK"
                        + " ElementSleutel[objecttype=jutter, naam=aantal aangemelde vondsten, kenmerk=false]]] join"
                        + " [emmerinvoer-onderwerp_10: onderwerp = $onderwerp] bind []",
                "  niet Waarde $waarde alfa [waarde_11: [[] attribuut WAARDE GELIJK ElementSleutel[objecttype=jutter,"
                        + " naam=aantal aangemelde vondsten, kenmerk=false]]] join [waarde-onderwerp_12: onderwerp ="
                        + " $onderwerp] bind []",
                "  gevolg Stand [onderwerp=$onderwerp, attribuut=ElementSleutel[objecttype=jutter, naam=aantal"
                        + " aangemelde vondsten, kenmerk=false], bron=INVOER, waarde=0, emmerscalair=false,"
                        + " invoeging=LOGISCH]",
                "regel stand Jutter.aantal aangemelde vondsten: uit de invoer, een emmerscalair | vorm null | groep"
                        + " null",
                "  lezingen [0 INVOER $invoer (invoer ElementSleutel[objecttype=jutter, naam=aantal aangemelde"
                        + " vondsten, kenmerk=false])]",
                "  onderwerp Jutter $onderwerp alfa [] bind [$invoer = [] Jutter.aantal aangemelde vondsten"
                        + " TIJDVAKKEN]",
                "  bestaat [Emmerinvoer $emmerinvoer alfa [emmerinvoer_13: [[] attribuut WAARDE GELIJK"
                        + " ElementSleutel[objecttype=jutter, naam=aantal aangemelde vondsten, kenmerk=false]]] join"
                        + " [emmerinvoer-onderwerp_14: onderwerp = $onderwerp] bind []]",
                "  niet Waarde $waarde alfa [waarde_15: [[] attribuut WAARDE GELIJK ElementSleutel[objecttype=jutter,"
                        + " naam=aantal aangemelde vondsten, kenmerk=false]]] join [waarde-onderwerp_16: onderwerp ="
                        + " $onderwerp] bind []",
                "  gevolg Stand [onderwerp=$onderwerp, attribuut=ElementSleutel[objecttype=jutter, naam=aantal"
                        + " aangemelde vondsten, kenmerk=false], bron=INVOER, waarde=0, emmerscalair=true,"
                        + " invoeging=LOGISCH]",
                "regel stand Jutter.totaal aangemelde vondsten: uit een afleiding | vorm null | groep null",
                "  lezingen [0 AFLEIDING $v (afleiding ElementSleutel[objecttype=jutter, naam=totaal aangemelde"
                        + " vondsten, kenmerk=false])]",
                "  onderwerp Jutter $onderwerp alfa [] bind []",
                "  patroon Waarde $waarde alfa [waarde_17: [[] attribuut WAARDE GELIJK"
                        + " ElementSleutel[objecttype=jutter, naam=totaal aangemelde vondsten, kenmerk=false]]] join"
                        + " [waarde-onderwerp_18: onderwerp = $onderwerp] bind [$v = [] Waarde.waarde WAARDE]",
                "  gevolg Stand [onderwerp=$onderwerp, attribuut=ElementSleutel[objecttype=jutter, naam=totaal"
                        + " aangemelde vondsten, kenmerk=false], bron=AFLEIDING, waarde=0, emmerscalair=false,"
                        + " invoeging=LOGISCH]",
                "regel stand Jutter.totaal aangemelde vondsten: uit de invoer | vorm null | groep null",
                "  lezingen [0 INVOER $invoer (invoer ElementSleutel[objecttype=jutter, naam=totaal aangemelde"
                        + " vondsten, kenmerk=false])]",
                "  onderwerp Jutter $onderwerp alfa [] bind [$invoer = [] Jutter.totaal aangemelde vondsten WAARDE]",
                "  niet Waarde $waarde alfa [waarde_19: [[] attribuut WAARDE GELIJK ElementSleutel[objecttype=jutter,"
                        + " naam=totaal aangemelde vondsten, kenmerk=false]]] join [waarde-onderwerp_20: onderwerp ="
                        + " $onderwerp] bind []",
                "  gevolg Stand [onderwerp=$onderwerp, attribuut=ElementSleutel[objecttype=jutter, naam=totaal"
                        + " aangemelde vondsten, kenmerk=false], bron=INVOER, waarde=0, emmerscalair=false,"
                        + " invoeging=LOGISCH]",
                "regel kenmerkstand Jutter.penninggerechtigd: waar uit de invoer | vorm null | groep null",
                "  lezingen []",
                "  onderwerp Jutter $onderwerp alfa [invoer_21: [[] penninggerechtigd WAAR GELIJK true]] bind []",
                "  gevolg Kenmerkstand [onderwerp=$onderwerp, kenmerk=ElementSleutel[objecttype=jutter,"
                        + " naam=penninggerechtigd, kenmerk=true], bron=INVOER, waar=true, invoeging=LOGISCH]",
                "regel kenmerkstand Jutter.penninggerechtigd: waar uit een afleiding | vorm null | groep null",
                "  lezingen []",
                "  onderwerp Jutter $onderwerp alfa [] bind []",
                "  patroon HeeftKenmerk $afgeleid alfa [afgeleid_22: [[] kenmerk WAARDE GELIJK"
                        + " ElementSleutel[objecttype=jutter, naam=penninggerechtigd, kenmerk=true]]] join"
                        + " [afgeleid-onderwerp_23: onderwerp = $onderwerp] bind []",
                "  gevolg Kenmerkstand [onderwerp=$onderwerp, kenmerk=ElementSleutel[objecttype=jutter,"
                        + " naam=penninggerechtigd, kenmerk=true], bron=AFLEIDING, waar=true, invoeging=LOGISCH]",
                "regel kenmerkstand Jutter.penninggerechtigd: onwaar | vorm null | groep null",
                "  lezingen []",
                "  onderwerp Jutter $onderwerp alfa [geen-invoer_24: [[] penninggerechtigd WAAR GELIJK false]] bind"
                        + " []",
                "  niet HeeftKenmerk $afgeleid alfa [afgeleid_25: [[] kenmerk WAARDE GELIJK"
                        + " ElementSleutel[objecttype=jutter, naam=penninggerechtigd, kenmerk=true]]] join"
                        + " [afgeleid-onderwerp_26: onderwerp = $onderwerp] bind []",
                "  gevolg Kenmerkstand [onderwerp=$onderwerp, kenmerk=ElementSleutel[objecttype=jutter,"
                        + " naam=penninggerechtigd, kenmerk=true], bron=GEEN, waar=false, invoeging=LOGISCH]"),
                standplannen("jutterswet-penningjaar"));
    }

    @Test void padstanden_over_twee_en_drie_stappen() throws Exception {
        assertEquals(List.of(
                "regel padstand Melding.vondst.jutter naar Jutter.premie: link aanwezig | vorm null | groep null",
                "  lezingen [0 BEREIKTE_STAND $v (stand Melding:vondst.jutter ElementSleutel[objecttype=jutter,"
                        + " naam=premie, kenmerk=false])]",
                "  onderwerp Melding $onderwerp alfa [link_71: [[Melding.vondst, Vondst.jutter] null WAARDE GEVULD]]"
                        + " bind [$bereikt = [Melding.vondst] Vondst.jutter WAARDE]",
                "  patroon Stand $stand alfa [stand_72: [[] attribuut WAARDE GELIJK ElementSleutel[objecttype=jutter,"
                        + " naam=premie, kenmerk=false]]] join [stand-bereikt_73: onderwerp = $bereikt] bind [$v = []"
                        + " Stand.waarde WAARDE]",
                "  gevolg Padstand [onderwerp=$onderwerp, pad=Melding:vondst.jutter,"
                        + " lid=ElementSleutel[objecttype=jutter, naam=premie, kenmerk=false], bron=BEREIKT,"
                        + " waarde=0, invoeging=LOGISCH]",
                "regel padstand Melding.vondst.jutter naar Jutter.premie: link afwezig | vorm null | groep null",
                "  lezingen []",
                "  onderwerp Melding $onderwerp alfa [geen-link_74: [[Melding.vondst, Vondst.jutter] null WAARDE"
                        + " LEEG]] bind []",
                "  gevolg Padstand [onderwerp=$onderwerp, pad=Melding:vondst.jutter,"
                        + " lid=ElementSleutel[objecttype=jutter, naam=premie, kenmerk=false], bron=ONTBREKENDE_LINK,"
                        + " waarde=-1, invoeging=LOGISCH]",
                "regel padstand Controle.melding.vondst.jutter naar Jutter.premie: link aanwezig | vorm null | groep"
                        + " null",
                "  lezingen [0 BEREIKTE_STAND $v (stand Controle:melding.vondst.jutter"
                        + " ElementSleutel[objecttype=jutter, naam=premie, kenmerk=false])]",
                "  onderwerp Controle $onderwerp alfa [link_75: [[Controle.melding, Melding.vondst, Vondst.jutter]"
                        + " null WAARDE GEVULD]] bind [$bereikt = [Controle.melding, Melding.vondst] Vondst.jutter"
                        + " WAARDE]",
                "  patroon Stand $stand alfa [stand_76: [[] attribuut WAARDE GELIJK ElementSleutel[objecttype=jutter,"
                        + " naam=premie, kenmerk=false]]] join [stand-bereikt_77: onderwerp = $bereikt] bind [$v = []"
                        + " Stand.waarde WAARDE]",
                "  gevolg Padstand [onderwerp=$onderwerp, pad=Controle:melding.vondst.jutter,"
                        + " lid=ElementSleutel[objecttype=jutter, naam=premie, kenmerk=false], bron=BEREIKT,"
                        + " waarde=0, invoeging=LOGISCH]",
                "regel padstand Controle.melding.vondst.jutter naar Jutter.premie: link afwezig | vorm null | groep"
                        + " null",
                "  lezingen []",
                "  onderwerp Controle $onderwerp alfa [geen-link_78: [[Controle.melding, Melding.vondst,"
                        + " Vondst.jutter] null WAARDE LEEG]] bind []",
                "  gevolg Padstand [onderwerp=$onderwerp, pad=Controle:melding.vondst.jutter,"
                        + " lid=ElementSleutel[objecttype=jutter, naam=premie, kenmerk=false], bron=ONTBREKENDE_LINK,"
                        + " waarde=-1, invoeging=LOGISCH]",
                "regel stand Jutter.premie: uit een afleiding | vorm null | groep null",
                "  lezingen [0 AFLEIDING $v (afleiding ElementSleutel[objecttype=jutter, naam=premie,"
                        + " kenmerk=false])]",
                "  onderwerp Jutter $onderwerp alfa [] bind []",
                "  patroon Waarde $waarde alfa [waarde_79: [[] attribuut WAARDE GELIJK"
                        + " ElementSleutel[objecttype=jutter, naam=premie, kenmerk=false]]] join"
                        + " [waarde-onderwerp_80: onderwerp = $onderwerp] bind [$v = [] Waarde.waarde WAARDE]",
                "  gevolg Stand [onderwerp=$onderwerp, attribuut=ElementSleutel[objecttype=jutter, naam=premie,"
                        + " kenmerk=false], bron=AFLEIDING, waarde=0, emmerscalair=false, invoeging=LOGISCH]",
                "regel stand Jutter.premie: uit de invoer | vorm null | groep null",
                "  lezingen [0 INVOER $invoer (invoer ElementSleutel[objecttype=jutter, naam=premie,"
                        + " kenmerk=false])]",
                "  onderwerp Jutter $onderwerp alfa [] bind [$invoer = [] Jutter.premie WAARDE]",
                "  niet Waarde $waarde alfa [waarde_81: [[] attribuut WAARDE GELIJK ElementSleutel[objecttype=jutter,"
                        + " naam=premie, kenmerk=false]]] join [waarde-onderwerp_82: onderwerp = $onderwerp] bind []",
                "  gevolg Stand [onderwerp=$onderwerp, attribuut=ElementSleutel[objecttype=jutter, naam=premie,"
                        + " kenmerk=false], bron=INVOER, waarde=0, emmerscalair=false, invoeging=LOGISCH]",
                "regel kenmerkstand Vondst.zwaar: waar uit de invoer | vorm null | groep null",
                "  lezingen []",
                "  onderwerp Vondst $onderwerp alfa [invoer_83: [[] zwaar WAAR GELIJK true]] bind []",
                "  gevolg Kenmerkstand [onderwerp=$onderwerp, kenmerk=ElementSleutel[objecttype=vondst, naam=zwaar,"
                        + " kenmerk=true], bron=INVOER, waar=true, invoeging=LOGISCH]",
                "regel kenmerkstand Vondst.zwaar: waar uit een afleiding | vorm null | groep null",
                "  lezingen []",
                "  onderwerp Vondst $onderwerp alfa [] bind []",
                "  patroon HeeftKenmerk $afgeleid alfa [afgeleid_84: [[] kenmerk WAARDE GELIJK"
                        + " ElementSleutel[objecttype=vondst, naam=zwaar, kenmerk=true]]] join"
                        + " [afgeleid-onderwerp_85: onderwerp = $onderwerp] bind []",
                "  gevolg Kenmerkstand [onderwerp=$onderwerp, kenmerk=ElementSleutel[objecttype=vondst, naam=zwaar,"
                        + " kenmerk=true], bron=AFLEIDING, waar=true, invoeging=LOGISCH]",
                "regel kenmerkstand Vondst.zwaar: onwaar | vorm null | groep null",
                "  lezingen []",
                "  onderwerp Vondst $onderwerp alfa [geen-invoer_86: [[] zwaar WAAR GELIJK false]] bind []",
                "  niet HeeftKenmerk $afgeleid alfa [afgeleid_87: [[] kenmerk WAARDE GELIJK"
                        + " ElementSleutel[objecttype=vondst, naam=zwaar, kenmerk=true]]] join"
                        + " [afgeleid-onderwerp_88: onderwerp = $onderwerp] bind []",
                "  gevolg Kenmerkstand [onderwerp=$onderwerp, kenmerk=ElementSleutel[objecttype=vondst, naam=zwaar,"
                        + " kenmerk=true], bron=GEEN, waar=false, invoeging=LOGISCH]"),
                standplannen("jutterswet-premiestaffel"));
    }

    /** Every plan of a standing rule of the golden {@code naam}, in the order the rules were built, as text. */
    private static List<String> standplannen(String naam) throws Exception {
        JsonNode document = JSON.readTree(GOLDEN.resolve(naam).resolve("ir.json").toFile());
        List<String> uit = new ArrayList<>();
        for (Regelplan p : DroolsBackend.build(document, DroolsBackend.Branch.EXEC_MODEL).execModelTranspiler()
                .regelplannen()) {
            if (!p.naam().startsWith("kenmerkstand ") && !p.naam().startsWith("stand ")
                    && !p.naam().startsWith("padstand "))
                continue;
            uit.add("regel " + p.naam() + " | vorm " + p.vorm() + " | groep " + p.agendagroep());
            List<String> slots = new ArrayList<>();
            for (Lezing l : p.lezingen().slots())
                slots.add(l.slot() + " " + l.soort() + " " + l.naam() + " (" + l.sleutel() + ")");
            uit.add("  lezingen " + slots);
            for (LhsItem item : p.linkerkant()) uit.add("  " + item(item));
            uit.add("  gevolg " + gevolg(p.gevolg()));
        }
        return uit;
    }

    private static String item(LhsItem item) {
        return switch (item) {
            case Onderwerp o -> "onderwerp " + o.bonenklasse().getSimpleName() + " " + o.naam() + " alfa "
                    + alfa(o.alfa()) + " bind " + bindingen(o.bindingen());
            case Patroon p -> "patroon " + patroon(p);
            case Niet n -> "niet " + (n.patronen().size() == 1 ? patroon(n.patronen().get(0)) : "binnen "
                    + n.binnen() + " buiten " + n.buiten() + " "
                    + n.patronen().stream().map(StandplanWitnessTest::patroon).toList());
            case Bestaat b -> "bestaat " + b.patronen().stream().map(StandplanWitnessTest::patroon).toList();
            case Accumulatie a -> "accumulatie " + patroon(a.bron());
            case Toetsitem t -> "toets " + t.id();
        };
    }

    private static String patroon(Patroon p) {
        return p.feitklasse().getSimpleName() + " " + p.naam() + " alfa " + alfa(p.alfa()) + " join "
                + p.joins().stream().map((Join j) -> j.id() + ": " + j.veld() + " = " + j.doel()).toList() + " bind "
                + bindingen(p.bindingen());
    }

    /** Each constraint with its id and its field tests: the path, the field (null: the object itself, or the object
     *  the path reaches), the form of reading, the comparison and the value. */
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
        return pad.stream().map(StandplanWitnessTest::veld).toList().toString();
    }

    private static String veld(Binding.Veld v) {
        return v.klasse().getSimpleName() + "." + v.naam();
    }

    /** The kind of {@code g} and every field of it. */
    private static String gevolg(Gevolg g) throws ReflectiveOperationException {
        List<String> velden = new ArrayList<>();
        for (RecordComponent c : g.getClass().getRecordComponents())
            velden.add(c.getName() + "=" + c.getAccessor().invoke(g));
        return g.getClass().getSimpleName() + " " + velden;
    }
}
