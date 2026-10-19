package dev.uchar.regelspraak.transpiler.tms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import dev.uchar.regelspraak.transpiler.tms.plan.TestItem;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The standing rules of the exec-model branch come whole from a plan (card drl-teksttak-op-truth-maintenance, step
 * 1c-A2): the three rules of a characteristic's {@code CharacteristicState}, the rules of an attribute's {@code State} from a
 * derivation and from the input, for a member on a timeline also as its asOfDate-bucket scalar, and the two rules of a
 * {@code PathState}. Each is a {@link RulePlan} whose left-hand side holds the patterns, the negations and the
 * existence that the rule built itself before, with their constraint ids in their order, and whose consequence says
 * where the standing comes from as a field ({@link Consequence.StateSource}). Every read of a bean names its field, the path
 * of reference fields it walks, and its form of reading ({@link Binding.ReadForm}): the decision of the analysis, which
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
 * <p>That the rendering of these plans builds the rules of before the step is what the harness {@code FireTrace} shows,
 * and the executable model of the corpus, which is equal as text.
 *
 * <p><b>Red</b> before the step: the standing rules built their rules themselves and a field test named only a field of
 * its own object, so there was no plan of them, and this class did not compile.
 */
class StatePlanWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void characteristic_attribute_and_timeline_with_bucket_scalar() throws Exception {
        assertEquals(List.of(
                "regel stand Jutter.aantal aangemelde vondsten: uit de invoer | vorm null | groep null",
                "  lezingen [0 INVOER $invoer (invoer ElementKey[objectType=jutter, naam=aantal aangemelde"
                        + " vondsten, characteristic=false])]",
                "  onderwerp Jutter $onderwerp alfa [] bind [$invoer = [] Jutter.aantal aangemelde vondsten"
                        + " PERIODS]",
                "  niet BucketInput $emmerinvoer alfa [emmerinvoer_9: [[] attribute VALUE GELIJK"
                        + " ElementKey[objectType=jutter, naam=aantal aangemelde vondsten, characteristic=false]]] join"
                        + " [emmerinvoer-onderwerp_10: onderwerp = $onderwerp] bind []",
                "  niet Value $waarde alfa [waarde_11: [[] attribute VALUE GELIJK ElementKey[objectType=jutter,"
                        + " naam=aantal aangemelde vondsten, characteristic=false]]] join [waarde-onderwerp_12: onderwerp ="
                        + " $onderwerp] bind []",
                "  gevolg State [onderwerp=$onderwerp, attribute=ElementKey[objectType=jutter, naam=aantal"
                        + " aangemelde vondsten, characteristic=false], source=INVOER, value=0, bucketScalar=false,"
                        + " insertion=LOGICAL]",
                "regel stand Jutter.aantal aangemelde vondsten: uit de invoer, een emmerscalair | vorm null | groep"
                        + " null",
                "  lezingen [0 INVOER $invoer (invoer ElementKey[objectType=jutter, naam=aantal aangemelde"
                        + " vondsten, characteristic=false])]",
                "  onderwerp Jutter $onderwerp alfa [] bind [$invoer = [] Jutter.aantal aangemelde vondsten"
                        + " PERIODS]",
                "  bestaat [BucketInput $emmerinvoer alfa [emmerinvoer_13: [[] attribute VALUE GELIJK"
                        + " ElementKey[objectType=jutter, naam=aantal aangemelde vondsten, characteristic=false]]] join"
                        + " [emmerinvoer-onderwerp_14: onderwerp = $onderwerp] bind []]",
                "  niet Value $waarde alfa [waarde_15: [[] attribute VALUE GELIJK ElementKey[objectType=jutter,"
                        + " naam=aantal aangemelde vondsten, characteristic=false]]] join [waarde-onderwerp_16: onderwerp ="
                        + " $onderwerp] bind []",
                "  gevolg State [onderwerp=$onderwerp, attribute=ElementKey[objectType=jutter, naam=aantal"
                        + " aangemelde vondsten, characteristic=false], source=INVOER, value=0, bucketScalar=true,"
                        + " insertion=LOGICAL]",
                "regel stand Jutter.totaal aangemelde vondsten: uit een afleiding | vorm null | groep null",
                "  lezingen [0 DERIVATION $v (afleiding ElementKey[objectType=jutter, naam=totaal aangemelde"
                        + " vondsten, characteristic=false])]",
                "  onderwerp Jutter $onderwerp alfa [] bind []",
                "  patroon Value $waarde alfa [waarde_17: [[] attribute VALUE GELIJK"
                        + " ElementKey[objectType=jutter, naam=totaal aangemelde vondsten, characteristic=false]]] join"
                        + " [waarde-onderwerp_18: onderwerp = $onderwerp] bind [$v = [] Value.value VALUE]",
                "  gevolg State [onderwerp=$onderwerp, attribute=ElementKey[objectType=jutter, naam=totaal"
                        + " aangemelde vondsten, characteristic=false], source=DERIVATION, value=0, bucketScalar=false,"
                        + " insertion=LOGICAL]",
                "regel stand Jutter.totaal aangemelde vondsten: uit de invoer | vorm null | groep null",
                "  lezingen [0 INVOER $invoer (invoer ElementKey[objectType=jutter, naam=totaal aangemelde"
                        + " vondsten, characteristic=false])]",
                "  onderwerp Jutter $onderwerp alfa [] bind [$invoer = [] Jutter.totaal aangemelde vondsten VALUE]",
                "  niet Value $waarde alfa [waarde_19: [[] attribute VALUE GELIJK ElementKey[objectType=jutter,"
                        + " naam=totaal aangemelde vondsten, characteristic=false]]] join [waarde-onderwerp_20: onderwerp ="
                        + " $onderwerp] bind []",
                "  gevolg State [onderwerp=$onderwerp, attribute=ElementKey[objectType=jutter, naam=totaal"
                        + " aangemelde vondsten, characteristic=false], source=INVOER, value=0, bucketScalar=false,"
                        + " insertion=LOGICAL]",
                "regel kenmerkstand Jutter.penninggerechtigd: waar uit de invoer | vorm null | groep null",
                "  lezingen []",
                "  onderwerp Jutter $onderwerp alfa [invoer_21: [[] penninggerechtigd WAAR GELIJK true]] bind []",
                "  gevolg InsertCharacteristicState [onderwerp=$onderwerp, characteristic=ElementKey[objectType=jutter,"
                        + " naam=penninggerechtigd, characteristic=true], source=INVOER, holds=true, insertion=LOGICAL]",
                "regel kenmerkstand Jutter.penninggerechtigd: waar uit een afleiding | vorm null | groep null",
                "  lezingen []",
                "  onderwerp Jutter $onderwerp alfa [] bind []",
                "  patroon HasCharacteristic $afgeleid alfa [afgeleid_22: [[] characteristic VALUE GELIJK"
                        + " ElementKey[objectType=jutter, naam=penninggerechtigd, characteristic=true]]] join"
                        + " [afgeleid-onderwerp_23: onderwerp = $onderwerp] bind []",
                "  gevolg InsertCharacteristicState [onderwerp=$onderwerp, characteristic=ElementKey[objectType=jutter,"
                        + " naam=penninggerechtigd, characteristic=true], source=DERIVATION, holds=true, insertion=LOGICAL]",
                "regel kenmerkstand Jutter.penninggerechtigd: onwaar | vorm null | groep null",
                "  lezingen []",
                "  onderwerp Jutter $onderwerp alfa [geen-invoer_24: [[] penninggerechtigd WAAR GELIJK false]] bind"
                        + " []",
                "  niet HasCharacteristic $afgeleid alfa [afgeleid_25: [[] characteristic VALUE GELIJK"
                        + " ElementKey[objectType=jutter, naam=penninggerechtigd, characteristic=true]]] join"
                        + " [afgeleid-onderwerp_26: onderwerp = $onderwerp] bind []",
                "  gevolg InsertCharacteristicState [onderwerp=$onderwerp, characteristic=ElementKey[objectType=jutter,"
                        + " naam=penninggerechtigd, characteristic=true], source=GEEN, holds=false, insertion=LOGICAL]"),
                statePlans("jutterswet-penningjaar"));
    }

    @Test void path_states_over_two_and_three_steps() throws Exception {
        assertEquals(List.of(
                "regel padstand Melding.vondst.jutter naar Jutter.premie: link aanwezig | vorm null | groep null",
                "  lezingen [0 REACHED_STATE $v (stand Melding:vondst.jutter ElementKey[objectType=jutter,"
                        + " naam=premie, characteristic=false])]",
                "  onderwerp Melding $onderwerp alfa [link_71: [[Melding.vondst, Vondst.jutter] null VALUE GEVULD]]"
                        + " bind [$bereikt = [Melding.vondst] Vondst.jutter VALUE]",
                "  patroon State $stand alfa [stand_72: [[] attribute VALUE GELIJK ElementKey[objectType=jutter,"
                        + " naam=premie, characteristic=false]]] join [stand-bereikt_73: onderwerp = $bereikt] bind [$v = []"
                        + " State.value VALUE]",
                "  gevolg InsertPathState [onderwerp=$onderwerp, path=Melding:vondst.jutter,"
                        + " member=ElementKey[objectType=jutter, naam=premie, characteristic=false], source=REACHED,"
                        + " value=0, insertion=LOGICAL]",
                "regel padstand Melding.vondst.jutter naar Jutter.premie: link afwezig | vorm null | groep null",
                "  lezingen []",
                "  onderwerp Melding $onderwerp alfa [geen-link_74: [[Melding.vondst, Vondst.jutter] null VALUE"
                        + " LEEG]] bind []",
                "  gevolg InsertPathState [onderwerp=$onderwerp, path=Melding:vondst.jutter,"
                        + " member=ElementKey[objectType=jutter, naam=premie, characteristic=false], source=MISSING_LINK,"
                        + " value=-1, insertion=LOGICAL]",
                "regel padstand Controle.melding.vondst.jutter naar Jutter.premie: link aanwezig | vorm null | groep"
                        + " null",
                "  lezingen [0 REACHED_STATE $v (stand Controle:melding.vondst.jutter"
                        + " ElementKey[objectType=jutter, naam=premie, characteristic=false])]",
                "  onderwerp Controle $onderwerp alfa [link_75: [[Controle.melding, Melding.vondst, Vondst.jutter]"
                        + " null VALUE GEVULD]] bind [$bereikt = [Controle.melding, Melding.vondst] Vondst.jutter"
                        + " VALUE]",
                "  patroon State $stand alfa [stand_76: [[] attribute VALUE GELIJK ElementKey[objectType=jutter,"
                        + " naam=premie, characteristic=false]]] join [stand-bereikt_77: onderwerp = $bereikt] bind [$v = []"
                        + " State.value VALUE]",
                "  gevolg InsertPathState [onderwerp=$onderwerp, path=Controle:melding.vondst.jutter,"
                        + " member=ElementKey[objectType=jutter, naam=premie, characteristic=false], source=REACHED,"
                        + " value=0, insertion=LOGICAL]",
                "regel padstand Controle.melding.vondst.jutter naar Jutter.premie: link afwezig | vorm null | groep"
                        + " null",
                "  lezingen []",
                "  onderwerp Controle $onderwerp alfa [geen-link_78: [[Controle.melding, Melding.vondst,"
                        + " Vondst.jutter] null VALUE LEEG]] bind []",
                "  gevolg InsertPathState [onderwerp=$onderwerp, path=Controle:melding.vondst.jutter,"
                        + " member=ElementKey[objectType=jutter, naam=premie, characteristic=false], source=MISSING_LINK,"
                        + " value=-1, insertion=LOGICAL]",
                "regel stand Jutter.premie: uit een afleiding | vorm null | groep null",
                "  lezingen [0 DERIVATION $v (afleiding ElementKey[objectType=jutter, naam=premie,"
                        + " characteristic=false])]",
                "  onderwerp Jutter $onderwerp alfa [] bind []",
                "  patroon Value $waarde alfa [waarde_79: [[] attribute VALUE GELIJK"
                        + " ElementKey[objectType=jutter, naam=premie, characteristic=false]]] join"
                        + " [waarde-onderwerp_80: onderwerp = $onderwerp] bind [$v = [] Value.value VALUE]",
                "  gevolg State [onderwerp=$onderwerp, attribute=ElementKey[objectType=jutter, naam=premie,"
                        + " characteristic=false], source=DERIVATION, value=0, bucketScalar=false, insertion=LOGICAL]",
                "regel stand Jutter.premie: uit de invoer | vorm null | groep null",
                "  lezingen [0 INVOER $invoer (invoer ElementKey[objectType=jutter, naam=premie,"
                        + " characteristic=false])]",
                "  onderwerp Jutter $onderwerp alfa [] bind [$invoer = [] Jutter.premie VALUE]",
                "  niet Value $waarde alfa [waarde_81: [[] attribute VALUE GELIJK ElementKey[objectType=jutter,"
                        + " naam=premie, characteristic=false]]] join [waarde-onderwerp_82: onderwerp = $onderwerp] bind []",
                "  gevolg State [onderwerp=$onderwerp, attribute=ElementKey[objectType=jutter, naam=premie,"
                        + " characteristic=false], source=INVOER, value=0, bucketScalar=false, insertion=LOGICAL]",
                "regel kenmerkstand Vondst.zwaar: waar uit de invoer | vorm null | groep null",
                "  lezingen []",
                "  onderwerp Vondst $onderwerp alfa [invoer_83: [[] zwaar WAAR GELIJK true]] bind []",
                "  gevolg InsertCharacteristicState [onderwerp=$onderwerp, characteristic=ElementKey[objectType=vondst, naam=zwaar,"
                        + " characteristic=true], source=INVOER, holds=true, insertion=LOGICAL]",
                "regel kenmerkstand Vondst.zwaar: waar uit een afleiding | vorm null | groep null",
                "  lezingen []",
                "  onderwerp Vondst $onderwerp alfa [] bind []",
                "  patroon HasCharacteristic $afgeleid alfa [afgeleid_84: [[] characteristic VALUE GELIJK"
                        + " ElementKey[objectType=vondst, naam=zwaar, characteristic=true]]] join"
                        + " [afgeleid-onderwerp_85: onderwerp = $onderwerp] bind []",
                "  gevolg InsertCharacteristicState [onderwerp=$onderwerp, characteristic=ElementKey[objectType=vondst, naam=zwaar,"
                        + " characteristic=true], source=DERIVATION, holds=true, insertion=LOGICAL]",
                "regel kenmerkstand Vondst.zwaar: onwaar | vorm null | groep null",
                "  lezingen []",
                "  onderwerp Vondst $onderwerp alfa [geen-invoer_86: [[] zwaar WAAR GELIJK false]] bind []",
                "  niet HasCharacteristic $afgeleid alfa [afgeleid_87: [[] characteristic VALUE GELIJK"
                        + " ElementKey[objectType=vondst, naam=zwaar, characteristic=true]]] join"
                        + " [afgeleid-onderwerp_88: onderwerp = $onderwerp] bind []",
                "  gevolg InsertCharacteristicState [onderwerp=$onderwerp, characteristic=ElementKey[objectType=vondst, naam=zwaar,"
                        + " characteristic=true], source=GEEN, holds=false, insertion=LOGICAL]"),
                statePlans("jutterswet-premiestaffel"));
    }

    /** Every plan of a standing rule of the golden {@code naam}, in the order of the model, as text. */
    private static List<String> statePlans(String naam) throws Exception {
        JsonNode document = JSON.readTree(GOLDEN.resolve(naam).resolve("ir.json").toFile());
        List<String> out = new ArrayList<>();
        for (RulePlan p : DroolsBackend.build(document, DroolsBackend.Branch.EXEC_MODEL).execModelTranspiler()
                .modelplan().rules()) {
            if (!p.naam().startsWith("kenmerkstand ") && !p.naam().startsWith("stand ")
                    && !p.naam().startsWith("padstand "))
                continue;
            out.add("regel " + p.naam() + " | vorm " + p.form() + " | groep " + p.agendaGroup());
            List<String> slots = new ArrayList<>();
            for (Read l : p.readings().slots())
                slots.add(l.slot() + " " + l.category() + " " + l.naam() + " (" + l.key() + ")");
            out.add("  lezingen " + slots);
            for (LhsItem item : p.leftSide()) out.add("  " + item(item));
            out.add("  gevolg " + consequence(p.consequence()));
        }
        return out;
    }

    private static String item(LhsItem item) {
        return switch (item) {
            case Subject o -> "onderwerp " + o.beanClass().getSimpleName() + " " + o.naam() + " alfa "
                    + alpha(o.alpha()) + " bind " + bindings(o.bindings());
            case FactPattern p -> "patroon " + pattern(p);
            case Not n -> "niet " + (n.patterns().size() == 1 ? pattern(n.patterns().get(0)) : "binnen "
                    + n.inside() + " buiten " + n.outside() + " "
                    + n.patterns().stream().map(StatePlanWitnessTest::pattern).toList());
            case Exists b -> "bestaat " + b.patterns().stream().map(StatePlanWitnessTest::pattern).toList();
            case Accumulation a -> "accumulatie " + pattern(a.source());
            case TestItem t -> "toets " + t.id();
        };
    }

    private static String pattern(FactPattern p) {
        return p.factClass().getSimpleName() + " " + p.naam() + " alfa " + alpha(p.alpha()) + " join "
                + p.joins().stream().map((Join j) -> j.id() + ": " + j.field() + " = " + j.target()).toList() + " bind "
                + bindings(p.bindings());
    }

    /** Each constraint with its id and its field tests: the path, the field (null: the object itself, or the object
     *  the path reaches), the form of reading, the comparison and the value. */
    private static List<String> alpha(List<Alpha> alpha) {
        List<String> out = new ArrayList<>();
        for (Alpha a : alpha)
            out.add(a.id() + ": " + a.tests().stream().map(t -> fields(t.path()) + " " + t.field() + " " + t.form()
                    + " " + t.op() + (t.value() == null ? "" : " " + t.value())).toList());
        return out;
    }

    /** Each binding with its variable, its path, its field on its class and its form of reading. */
    private static List<String> bindings(List<Binding> bindings) {
        return bindings.stream().map(b -> b.target() + " = " + fields(b.path()) + " " + field(b.field()) + " " + b.form()
                + (b.category() == null ? "" : " " + b.category().getSimpleName())).toList();
    }

    private static String fields(List<Binding.Field> path) {
        return path.stream().map(StatePlanWitnessTest::field).toList().toString();
    }

    private static String field(Binding.Field v) {
        return v.javaClass().getSimpleName() + "." + v.naam();
    }

    /** The kind of {@code g} and every field of it. */
    private static String consequence(Consequence g) throws ReflectiveOperationException {
        List<String> fields = new ArrayList<>();
        for (RecordComponent c : g.getClass().getRecordComponents())
            fields.add(c.getName() + "=" + c.getAccessor().invoke(g));
        return g.getClass().getSimpleName() + " " + fields;
    }
}
