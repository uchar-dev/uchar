package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler.RuleOrigin;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static dev.uchar.regelspraak.cases.TmsDocuments.COUNTING;
import static dev.uchar.regelspraak.cases.TmsDocuments.DATE;
import static dev.uchar.regelspraak.cases.TmsDocuments.aggregation;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribute;
import static dev.uchar.regelspraak.cases.TmsDocuments.attributeGroup;
import static dev.uchar.regelspraak.cases.TmsDocuments.binary;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.comparison;
import static dev.uchar.regelspraak.cases.TmsDocuments.compound;
import static dev.uchar.regelspraak.cases.TmsDocuments.defaultEntry;
import static dev.uchar.regelspraak.cases.TmsDocuments.factTypeOneToMany;
import static dev.uchar.regelspraak.cases.TmsDocuments.literal;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.numberType;
import static dev.uchar.regelspraak.cases.TmsDocuments.objectType;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.sourceRole;
import static dev.uchar.regelspraak.cases.TmsDocuments.unit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@code TmsTranspiler.originOf} on goldens that show each shape (card drl-rule-origin, acceptance (b)), and on one
 * document written here for a shape no golden has: per rule of an effective stack, in the order of the model, its kind
 * and the default it lowers, as the pointer {@code DroolsBackend.decisionsOf} names a decider by. The labels in the names
 * are the lowering's own numbering; the pointers are the document's. Beside the table, where the shape needs it, what the toolchain reads at the pointer: the
 * group's name and the variant's name, or the ordinary rule's name ({@code GROUND} in the toolchain's ENGINE.md).
 * {@link RuleOriginCorpusWitnessTest} holds the same over the whole corpus.
 */
class RuleOriginWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    private static DroolsBackend.Build build(String golden, boolean readings) throws Exception {
        return DroolsBackend.build(GOLDEN.resolve(golden).resolve("ir.json"), DroolsBackend.Branch.DRL_TEXT, readings);
    }

    private static JsonNode document(String golden) throws Exception {
        return TmsDocuments.JSON.readTree(GOLDEN.resolve(golden).resolve("ir.json").toFile());
    }

    /** The rules of {@code form}, in the order of the model, each as {@code <name> -> <kind> <pointer>}. */
    private static List<String> table(DroolsBackend.Build b, String form) {
        TmsTranspiler t = b.execModelTranspiler();
        List<String> out = new ArrayList<>();
        for (org.drools.model.Rule r : t.model().getRules()) {
            RuleOrigin o = t.originOf(r.getName());
            if (form.equals(o.form()))
                out.add(r.getName() + " -> " + o.kind() + (o.pointer() == null ? "" : " " + o.pointer()));
        }
        return out;
    }

    @Test void a_flat_group_names_each_default_by_its_place_in_the_group() throws Exception {
        // the group "premie" is the fourth of the document, so its defaults are /rules/prioritizedDefaults/3/defaults/j
        DroolsBackend.Build b = build("jutterswet-premiestaffel", false);
        String stack = "defaults A:Jutter.premie";
        assertEquals(List.of(
                stack + ": default 0 -> DEFAULT /rules/prioritizedDefaults/3/defaults/0",
                stack + ": default 1 -> DEFAULT /rules/prioritizedDefaults/3/defaults/1",
                stack + ": default 2 -> DEFAULT /rules/prioritizedDefaults/3/defaults/2",
                stack + ": bijdrage aan vouw 0 -> DEFAULT /rules/prioritizedDefaults/3/defaults/0",
                stack + ": bijdrage aan vouw 1 -> DEFAULT /rules/prioritizedDefaults/3/defaults/1"), table(b, stack));
        // what the toolchain reads there: the group by its name, the variant by the rest of the pointer
        JsonNode ir = document("jutterswet-premiestaffel");
        assertEquals("premie", ir.at("/rules/prioritizedDefaults/3/name").asText());
        assertEquals(List.of("premie bij een zware vangst", "premie bij veel vondsten", "premie"),
                List.of(ir.at("/rules/prioritizedDefaults/3/defaults/0/name").asText(),
                        ir.at("/rules/prioritizedDefaults/3/defaults/1/name").asText(),
                        ir.at("/rules/prioritizedDefaults/3/defaults/2/name").asText()));
    }

    @Test void a_nested_exception_names_the_default_at_its_depth() throws Exception {
        // two levels of exception groups below the one default at the root: a leaf names its default at its depth,
        // the rules that reconcile the exception groups of a default name that default, and the reconciliation of the
        // root is the stack's own
        DroolsBackend.Build b = build("anchor/jutterswet-nestdiepte", false);
        String stack = "defaults A:Jutter.heffing", d0 = "/rules/prioritizedDefaults/0/defaults/0";
        String d010 = d0 + "/exceptions/members/1/defaults/0";
        assertEquals(List.of(
                stack + ": default 0.0.0 -> DEFAULT " + d0 + "/exceptions/members/0/defaults/0",
                stack + ": default 0.1.0.0.0 -> DEFAULT " + d010 + "/exceptions/members/0/defaults/0",
                stack + ": default 0.1.0.1.0 -> DEFAULT " + d010 + "/exceptions/members/1/defaults/0",
                stack + ": verschil 0.1.0 -> DEFAULT " + d010,
                stack + ": eens 0.1.0 -> DEFAULT " + d010,
                stack + ": terugval 0.1.0 -> DEFAULT " + d010,
                stack + ": doorgeven 0 -> DEFAULT " + d0,
                stack + ": verschil 0 -> DEFAULT " + d0,
                stack + ": eens 0 -> DEFAULT " + d0,
                stack + ": terugval 0 -> DEFAULT " + d0,
                stack + ": doorgeven wortel -> STACK",
                stack + ": eens wortel -> STACK"), table(b, stack));
    }

    @Test void a_subtype_stack_names_the_ordinary_rule_it_took_in() throws Exception {
        // Wrakrecht is a Strandrecht: its stack takes the default of the group on Strandrecht and, as its base, the
        // ordinary rule on Wrakrecht (RUNTIME-SEMANTICS ch. 7.10), named by that rule's version; the rule's own form
        // keeps its own rule, on the types the stack does not take
        DroolsBackend.Build b = build("jutterswet-wrakbasis", true);
        String stack = "defaults A:Wrakrecht.uitkering";
        assertEquals(List.of(
                stack + ": default 0 -> DEFAULT /rules/prioritizedDefaults/0/defaults/0",
                stack + ": default 1 -> DEFAULT /rules/rules/0/versions/0",
                stack + ": default 0: lezing -> DEFAULT /rules/prioritizedDefaults/0/defaults/0",
                stack + ": default 1: lezing -> DEFAULT /rules/rules/0/versions/0"), table(b, stack));
        assertEquals("uitkering van een wrakrecht", document("jutterswet-wrakbasis").at("/rules/rules/0/name").asText());
        TmsTranspiler t = b.execModelTranspiler();
        assertEquals(new RuleOrigin("uitkering van een wrakrecht", RuleOrigin.Kind.FORM, null),
                t.originOf("uitkering van een wrakrecht"));
        // a reading rule is in no form for formOf, and has the origin of the rule it reads
        assertEquals(null, t.formOf(stack + ": default 1: lezing"));
    }

    @Test void the_contributions_of_a_fold_in_a_justification_name_its_default() throws Exception {
        DroolsBackend.Build b = build("jutterswet-premiestaffel", false);
        TmsTranspiler t = b.execModelTranspiler();
        // "premie bij een zware vangst" sums in its justification, "premie bij veel vondsten" counts, and "lichte
        // vondsten" folds twice, a minimum and a maximum
        JsonNode ir = document("jutterswet-premiestaffel");
        assertEquals("sum", ir.at("/rules/prioritizedDefaults/3/defaults/0/justification/left/kind").asText());
        assertEquals("count", ir.at("/rules/prioritizedDefaults/3/defaults/1/justification/left/kind").asText());
        assertEquals(List.of("minimum", "maximum"), List.of(
                ir.at("/rules/prioritizedDefaults/4/defaults/0/justification/parts/0/left/kind").asText(),
                ir.at("/rules/prioritizedDefaults/4/defaults/0/justification/parts/1/left/kind").asText()));
        assertEquals(List.of(
                "defaults K:Jutter.ijverig: default 0 -> DEFAULT /rules/prioritizedDefaults/4/defaults/0",
                "defaults K:Jutter.ijverig: default 1 -> DEFAULT /rules/prioritizedDefaults/4/defaults/1",
                "defaults K:Jutter.ijverig: bijdrage aan vouw 2 -> DEFAULT /rules/prioritizedDefaults/4/defaults/0",
                "defaults K:Jutter.ijverig: bijdrage aan vouw 3 -> DEFAULT /rules/prioritizedDefaults/4/defaults/0"),
                table(b, "defaults K:Jutter.ijverig"));
        assertEquals(new RuleOrigin("defaults A:Jutter.premie", RuleOrigin.Kind.DEFAULT,
                "/rules/prioritizedDefaults/3/defaults/0"), t.originOf("defaults A:Jutter.premie: bijdrage aan vouw 0"));
        // a fold an ordinary form reads is that form's, beside a stack whose default folds too
        DroolsBackend.Build m = build("jutterswet-dringende-melding", false);
        assertEquals(new RuleOrigin("dringende melding", RuleOrigin.Kind.FORM, null),
                m.execModelTranspiler().originOf("dringende melding: bijdrage aan vouw 0"));
        assertEquals(new RuleOrigin("defaults K:Melding.voorrang", RuleOrigin.Kind.DEFAULT,
                "/rules/prioritizedDefaults/0/defaults/0"),
                m.execModelTranspiler().originOf("defaults K:Melding.voorrang: bijdrage aan vouw 4"));
    }

    @Test void the_fault_rule_of_a_justification_registers_its_folds_under_its_default() throws Exception {
        // "De meldtermijn van een Jutter is zijn aanmelddatum indien het aantal vondsten van de Jutter groter is dan 2
        // en zijn aanmelddatum plus zijn wachtdagen later is dan 2026-01-01": the date step can fault, so the default
        // has a fault rule, which registers the reads of the justification at once, the count's fold among them, before
        // the stack registers the reads of the default's own rule. No golden has that shape: in the corpus no
        // justification that reads a fold can fault. The lower default reads no fold
        String model = TmsDocuments.model(List.of(
                objectType("Jutter", List.of(attribute("aanmelddatum", DATE),
                        attribute("wachtdagen", numberType(true, 0, unit("dagen"))), attribute("meldtermijn", DATE)),
                        List.of("gemeld")),
                objectType("Vondst", List.of(attribute("taxatie", numberType(true, 0, null))), List.of())),
                List.of(factTypeOneToMany("aanmelding", "aanmelder", "Jutter", "vondst", "Vondst", "vondsten")));
        String vondsten = "{\"node\":\"AttributeReference\",\"attribute\":\"vondsten\",\"subjectType\":\"Jutter\","
                + "\"source\":{\"snippet\":\"vondsten van de Jutter\"}}";
        String count = aggregation("count", vondsten, "Vondst", sourceRole("aanmelding", "vondst"), "null", "null",
                COUNTING, "het aantal vondsten van de Jutter");
        String aanmelddatum = ownRead("aanmelddatum", "Jutter");
        String justification = compound("all", 0, List.of(
                comparison("greater", count, number("2")),
                comparison("later", binary("plus", aanmelddatum, ownRead("wachtdagen", "Jutter"), DATE),
                        literal("2026-01-01", DATE))));
        String group = attributeGroup("meldtermijn van een jutter", "Jutter", "meldtermijn", List.of(
                defaultEntry(justification, aanmelddatum), defaultEntry(check("gemeld", false), aanmelddatum)));
        DroolsBackend.Build b = DroolsBackend.build(TmsDocuments.document(model, List.of(), List.of(), List.of(group)),
                DroolsBackend.Branch.DRL_TEXT, false);
        String stack = "defaults A:Jutter.meldtermijn", d0 = "/rules/prioritizedDefaults/0/defaults/0";
        assertEquals(List.of(
                stack + ": default 0 -> DEFAULT " + d0,
                stack + ": default 0: fout in de voorwaarde -> DEFAULT " + d0,
                stack + ": default 1 -> DEFAULT /rules/prioritizedDefaults/0/defaults/1",
                stack + ": bijdrage aan vouw 0 -> DEFAULT " + d0), table(b, stack));
    }

    @Test void a_stack_lowers_itself_where_no_default_is_at_hand() throws Exception {
        // a group with a dated validity: the fault of a session without an evaluation date is the stack's own
        DroolsBackend.Build b = build("jutterswet-uitsluiting-gedateerd", false);
        assertEquals(new RuleOrigin("defaults K:Jutter.uitgesloten", RuleOrigin.Kind.STACK, null),
                b.execModelTranspiler().originOf("defaults K:Jutter.uitgesloten: geen rekendatum"));
    }

    @Test void a_rule_of_no_form_has_no_form_and_a_name_of_no_rule_is_refused() throws Exception {
        TmsTranspiler t = build("jutterswet-dringende-melding", false).execModelTranspiler();
        assertEquals(new RuleOrigin(null, RuleOrigin.Kind.NO_FORM, null),
                t.originOf("kenmerkstand Vondst.zwaar: onwaar"));
        assertThrows(IllegalArgumentException.class, () -> t.originOf("geen regel van dit model"));
    }
}
