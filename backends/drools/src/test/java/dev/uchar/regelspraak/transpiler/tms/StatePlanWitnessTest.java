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
                "rule stand Jutter.aantal aangemelde vondsten: uit de invoer | form null | group null",
                "  readings [0 INPUT $invoer (invoer ElementKey[objectType=jutter, name=aantal aangemelde"
                        + " vondsten, characteristic=false])]",
                "  subject Jutter $onderwerp alpha [] bind [$invoer = [] Jutter.aantal aangemelde vondsten"
                        + " PERIODS]",
                "  not BucketInput $emmerinvoer alpha [emmerinvoer_9: [[] attribute VALUE GELIJK"
                        + " ElementKey[objectType=jutter, name=aantal aangemelde vondsten, characteristic=false]]] join"
                        + " [emmerinvoer-onderwerp_10: subject = $onderwerp] bind []",
                "  not Value $waarde alpha [waarde_11: [[] attribute VALUE GELIJK ElementKey[objectType=jutter,"
                        + " name=aantal aangemelde vondsten, characteristic=false]]] join [waarde-onderwerp_12: subject ="
                        + " $onderwerp] bind []",
                "  consequence State [subject=$onderwerp, attribute=ElementKey[objectType=jutter, name=aantal"
                        + " aangemelde vondsten, characteristic=false], source=INPUT, value=0, bucketScalar=false,"
                        + " insertion=LOGICAL]",
                "rule stand Jutter.aantal aangemelde vondsten: uit de invoer, een emmerscalair | form null | group"
                        + " null",
                "  readings [0 INPUT $invoer (invoer ElementKey[objectType=jutter, name=aantal aangemelde"
                        + " vondsten, characteristic=false])]",
                "  subject Jutter $onderwerp alpha [] bind [$invoer = [] Jutter.aantal aangemelde vondsten"
                        + " PERIODS]",
                "  exists [BucketInput $emmerinvoer alpha [emmerinvoer_13: [[] attribute VALUE GELIJK"
                        + " ElementKey[objectType=jutter, name=aantal aangemelde vondsten, characteristic=false]]] join"
                        + " [emmerinvoer-onderwerp_14: subject = $onderwerp] bind []]",
                "  not Value $waarde alpha [waarde_15: [[] attribute VALUE GELIJK ElementKey[objectType=jutter,"
                        + " name=aantal aangemelde vondsten, characteristic=false]]] join [waarde-onderwerp_16: subject ="
                        + " $onderwerp] bind []",
                "  consequence State [subject=$onderwerp, attribute=ElementKey[objectType=jutter, name=aantal"
                        + " aangemelde vondsten, characteristic=false], source=INPUT, value=0, bucketScalar=true,"
                        + " insertion=LOGICAL]",
                "rule stand Jutter.totaal aangemelde vondsten: uit een afleiding | form null | group null",
                "  readings [0 DERIVATION $v (afleiding ElementKey[objectType=jutter, name=totaal aangemelde"
                        + " vondsten, characteristic=false])]",
                "  subject Jutter $onderwerp alpha [] bind []",
                "  pattern Value $waarde alpha [waarde_17: [[] attribute VALUE GELIJK"
                        + " ElementKey[objectType=jutter, name=totaal aangemelde vondsten, characteristic=false]]] join"
                        + " [waarde-onderwerp_18: subject = $onderwerp] bind [$v = [] Value.value VALUE]",
                "  consequence State [subject=$onderwerp, attribute=ElementKey[objectType=jutter, name=totaal"
                        + " aangemelde vondsten, characteristic=false], source=DERIVATION, value=0, bucketScalar=false,"
                        + " insertion=LOGICAL]",
                "rule stand Jutter.totaal aangemelde vondsten: uit de invoer | form null | group null",
                "  readings [0 INPUT $invoer (invoer ElementKey[objectType=jutter, name=totaal aangemelde"
                        + " vondsten, characteristic=false])]",
                "  subject Jutter $onderwerp alpha [] bind [$invoer = [] Jutter.totaal aangemelde vondsten VALUE]",
                "  not Value $waarde alpha [waarde_19: [[] attribute VALUE GELIJK ElementKey[objectType=jutter,"
                        + " name=totaal aangemelde vondsten, characteristic=false]]] join [waarde-onderwerp_20: subject ="
                        + " $onderwerp] bind []",
                "  consequence State [subject=$onderwerp, attribute=ElementKey[objectType=jutter, name=totaal"
                        + " aangemelde vondsten, characteristic=false], source=INPUT, value=0, bucketScalar=false,"
                        + " insertion=LOGICAL]",
                "rule kenmerkstand Jutter.penninggerechtigd: waar uit de invoer | form null | group null",
                "  readings []",
                "  subject Jutter $onderwerp alpha [invoer_21: [[] penninggerechtigd WAAR GELIJK true]] bind []",
                "  consequence InsertCharacteristicState [subject=$onderwerp, characteristic=ElementKey[objectType=jutter,"
                        + " name=penninggerechtigd, characteristic=true], source=INPUT, holds=true, insertion=LOGICAL]",
                "rule kenmerkstand Jutter.penninggerechtigd: waar uit een afleiding | form null | group null",
                "  readings []",
                "  subject Jutter $onderwerp alpha [] bind []",
                "  pattern HasCharacteristic $afgeleid alpha [afgeleid_22: [[] characteristic VALUE GELIJK"
                        + " ElementKey[objectType=jutter, name=penninggerechtigd, characteristic=true]]] join"
                        + " [afgeleid-onderwerp_23: subject = $onderwerp] bind []",
                "  consequence InsertCharacteristicState [subject=$onderwerp, characteristic=ElementKey[objectType=jutter,"
                        + " name=penninggerechtigd, characteristic=true], source=DERIVATION, holds=true, insertion=LOGICAL]",
                "rule kenmerkstand Jutter.penninggerechtigd: onwaar | form null | group null",
                "  readings []",
                "  subject Jutter $onderwerp alpha [geen-invoer_24: [[] penninggerechtigd WAAR GELIJK false]] bind"
                        + " []",
                "  not HasCharacteristic $afgeleid alpha [afgeleid_25: [[] characteristic VALUE GELIJK"
                        + " ElementKey[objectType=jutter, name=penninggerechtigd, characteristic=true]]] join"
                        + " [afgeleid-onderwerp_26: subject = $onderwerp] bind []",
                "  consequence InsertCharacteristicState [subject=$onderwerp, characteristic=ElementKey[objectType=jutter,"
                        + " name=penninggerechtigd, characteristic=true], source=NONE, holds=false, insertion=LOGICAL]"),
                statePlans("jutterswet-penningjaar"));
    }

    @Test void path_states_over_two_and_three_steps() throws Exception {
        assertEquals(List.of(
                "rule padstand Melding.vondst.jutter naar Jutter.premie: link aanwezig | form null | group null",
                "  readings [0 REACHED_STATE $v (stand Melding:vondst.jutter ElementKey[objectType=jutter,"
                        + " name=premie, characteristic=false])]",
                "  subject Melding $onderwerp alpha [link_71: [[Melding.vondst, Vondst.jutter] null VALUE GEVULD]]"
                        + " bind [$bereikt = [Melding.vondst] Vondst.jutter VALUE]",
                "  pattern State $stand alpha [stand_72: [[] attribute VALUE GELIJK ElementKey[objectType=jutter,"
                        + " name=premie, characteristic=false]]] join [stand-bereikt_73: subject = $bereikt] bind [$v = []"
                        + " State.value VALUE]",
                "  consequence InsertPathState [subject=$onderwerp, path=Melding:vondst.jutter,"
                        + " member=ElementKey[objectType=jutter, name=premie, characteristic=false], source=REACHED,"
                        + " value=0, reached=$bereikt, insertion=LOGICAL]",
                "rule padstand Melding.vondst.jutter naar Jutter.premie: link afwezig | form null | group null",
                "  readings []",
                "  subject Melding $onderwerp alpha [geen-link_74: [[Melding.vondst, Vondst.jutter] null VALUE"
                        + " LEEG]] bind []",
                "  consequence InsertPathState [subject=$onderwerp, path=Melding:vondst.jutter,"
                        + " member=ElementKey[objectType=jutter, name=premie, characteristic=false], source=MISSING_LINK,"
                        + " value=-1, reached=null, insertion=LOGICAL]",
                "rule padstand Controle.melding.vondst.jutter naar Jutter.premie: link aanwezig | form null | group"
                        + " null",
                "  readings [0 REACHED_STATE $v (stand Controle:melding.vondst.jutter"
                        + " ElementKey[objectType=jutter, name=premie, characteristic=false])]",
                "  subject Controle $onderwerp alpha [link_75: [[Controle.melding, Melding.vondst, Vondst.jutter]"
                        + " null VALUE GEVULD]] bind [$bereikt = [Controle.melding, Melding.vondst] Vondst.jutter"
                        + " VALUE]",
                "  pattern State $stand alpha [stand_76: [[] attribute VALUE GELIJK ElementKey[objectType=jutter,"
                        + " name=premie, characteristic=false]]] join [stand-bereikt_77: subject = $bereikt] bind [$v = []"
                        + " State.value VALUE]",
                "  consequence InsertPathState [subject=$onderwerp, path=Controle:melding.vondst.jutter,"
                        + " member=ElementKey[objectType=jutter, name=premie, characteristic=false], source=REACHED,"
                        + " value=0, reached=$bereikt, insertion=LOGICAL]",
                "rule padstand Controle.melding.vondst.jutter naar Jutter.premie: link afwezig | form null | group"
                        + " null",
                "  readings []",
                "  subject Controle $onderwerp alpha [geen-link_78: [[Controle.melding, Melding.vondst,"
                        + " Vondst.jutter] null VALUE LEEG]] bind []",
                "  consequence InsertPathState [subject=$onderwerp, path=Controle:melding.vondst.jutter,"
                        + " member=ElementKey[objectType=jutter, name=premie, characteristic=false], source=MISSING_LINK,"
                        + " value=-1, reached=null, insertion=LOGICAL]",
                "rule stand Jutter.premie: uit een afleiding | form null | group null",
                "  readings [0 DERIVATION $v (afleiding ElementKey[objectType=jutter, name=premie,"
                        + " characteristic=false])]",
                "  subject Jutter $onderwerp alpha [] bind []",
                "  pattern Value $waarde alpha [waarde_79: [[] attribute VALUE GELIJK"
                        + " ElementKey[objectType=jutter, name=premie, characteristic=false]]] join"
                        + " [waarde-onderwerp_80: subject = $onderwerp] bind [$v = [] Value.value VALUE]",
                "  consequence State [subject=$onderwerp, attribute=ElementKey[objectType=jutter, name=premie,"
                        + " characteristic=false], source=DERIVATION, value=0, bucketScalar=false, insertion=LOGICAL]",
                "rule stand Jutter.premie: uit de invoer | form null | group null",
                "  readings [0 INPUT $invoer (invoer ElementKey[objectType=jutter, name=premie,"
                        + " characteristic=false])]",
                "  subject Jutter $onderwerp alpha [] bind [$invoer = [] Jutter.premie VALUE]",
                "  not Value $waarde alpha [waarde_81: [[] attribute VALUE GELIJK ElementKey[objectType=jutter,"
                        + " name=premie, characteristic=false]]] join [waarde-onderwerp_82: subject = $onderwerp] bind []",
                "  consequence State [subject=$onderwerp, attribute=ElementKey[objectType=jutter, name=premie,"
                        + " characteristic=false], source=INPUT, value=0, bucketScalar=false, insertion=LOGICAL]",
                "rule kenmerkstand Vondst.zwaar: waar uit de invoer | form null | group null",
                "  readings []",
                "  subject Vondst $onderwerp alpha [invoer_83: [[] zwaar WAAR GELIJK true]] bind []",
                "  consequence InsertCharacteristicState [subject=$onderwerp, characteristic=ElementKey[objectType=vondst, name=zwaar,"
                        + " characteristic=true], source=INPUT, holds=true, insertion=LOGICAL]",
                "rule kenmerkstand Vondst.zwaar: waar uit een afleiding | form null | group null",
                "  readings []",
                "  subject Vondst $onderwerp alpha [] bind []",
                "  pattern HasCharacteristic $afgeleid alpha [afgeleid_84: [[] characteristic VALUE GELIJK"
                        + " ElementKey[objectType=vondst, name=zwaar, characteristic=true]]] join"
                        + " [afgeleid-onderwerp_85: subject = $onderwerp] bind []",
                "  consequence InsertCharacteristicState [subject=$onderwerp, characteristic=ElementKey[objectType=vondst, name=zwaar,"
                        + " characteristic=true], source=DERIVATION, holds=true, insertion=LOGICAL]",
                "rule kenmerkstand Vondst.zwaar: onwaar | form null | group null",
                "  readings []",
                "  subject Vondst $onderwerp alpha [geen-invoer_86: [[] zwaar WAAR GELIJK false]] bind []",
                "  not HasCharacteristic $afgeleid alpha [afgeleid_87: [[] characteristic VALUE GELIJK"
                        + " ElementKey[objectType=vondst, name=zwaar, characteristic=true]]] join"
                        + " [afgeleid-onderwerp_88: subject = $onderwerp] bind []",
                "  consequence InsertCharacteristicState [subject=$onderwerp, characteristic=ElementKey[objectType=vondst, name=zwaar,"
                        + " characteristic=true], source=NONE, holds=false, insertion=LOGICAL]"),
                statePlans("jutterswet-premiestaffel"));
    }

    /** Every plan of a standing rule of the golden {@code name}, in the order of the model, as text. */
    private static List<String> statePlans(String name) throws Exception {
        JsonNode document = JSON.readTree(GOLDEN.resolve(name).resolve("ir.json").toFile());
        List<String> out = new ArrayList<>();
        for (RulePlan p : DroolsBackend.build(document, DroolsBackend.Branch.EXEC_MODEL).execModelTranspiler()
                .modelplan().rules()) {
            if (!p.name().startsWith("kenmerkstand ") && !p.name().startsWith("stand ")
                    && !p.name().startsWith("padstand "))
                continue;
            out.add("rule " + p.name() + " | form " + p.form() + " | group " + p.agendaGroup());
            List<String> slots = new ArrayList<>();
            for (Read l : p.readings().slots())
                slots.add(l.slot() + " " + l.kind() + " " + l.name() + " (" + l.key() + ")");
            out.add("  readings " + slots);
            for (LhsItem item : p.leftSide()) out.add("  " + item(item));
            out.add("  consequence " + consequence(p.consequence()));
        }
        return out;
    }

    private static String item(LhsItem item) {
        return switch (item) {
            case Subject o -> "subject " + o.beanClass().getSimpleName() + " " + o.name() + " alpha "
                    + alpha(o.alpha()) + " bind " + bindings(o.bindings());
            case FactPattern p -> "pattern " + pattern(p);
            case Not n -> "not " + (n.patterns().size() == 1 ? pattern(n.patterns().get(0)) : "inside "
                    + n.inside() + " outside " + n.outside() + " "
                    + n.patterns().stream().map(StatePlanWitnessTest::pattern).toList());
            case Exists b -> "exists " + b.patterns().stream().map(StatePlanWitnessTest::pattern).toList();
            case Accumulation a -> "accumulation " + pattern(a.source());
            case TestItem t -> "test " + t.id();
        };
    }

    private static String pattern(FactPattern p) {
        return p.factClass().getSimpleName() + " " + p.name() + " alpha " + alpha(p.alpha()) + " join "
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
        return v.javaClass().getSimpleName() + "." + v.name();
    }

    /** The kind of {@code g} and every field of it. */
    private static String consequence(Consequence g) throws ReflectiveOperationException {
        List<String> fields = new ArrayList<>();
        for (RecordComponent c : g.getClass().getRecordComponents())
            fields.add(c.getName() + "=" + c.getAccessor().invoke(g));
        return g.getClass().getSimpleName() + " " + fields;
    }
}
