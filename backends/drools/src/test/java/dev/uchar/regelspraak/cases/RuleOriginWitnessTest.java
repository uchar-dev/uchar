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
 * {@code TmsTranspiler.originOf} on goldens that show each shape (card drl-rule-origin, acceptance (b)), and on
 * documents written here for shapes no golden has: per rule of an effective stack or of a form, in the order of the
 * model, its kind, its role (card drl-rule-part, by the role's key), the default it lowers, as the pointer
 * {@code DroolsBackend.decisionsOf} names a decider by, and for a rule of a fold the node of the fold. The labels in
 * the names are the lowering's own numbering and wording; the roles are fixed keys and the pointers are the
 * document's. Beside the table, where the shape needs it, what the toolchain reads at the pointer: the group's name and
 * the variant's name, the ordinary rule's name ({@code GROUND} in the toolchain's ENGINE.md), or the text of the fold.
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

    /** The rules of {@code form}, in the order of the model, each as {@code <name> -> <kind> <role> <pointer>
     *  vouw <fold>}, the pointer and the fold where the rule has one. */
    private static List<String> table(DroolsBackend.Build b, String form) {
        TmsTranspiler t = b.execModelTranspiler();
        List<String> out = new ArrayList<>();
        for (org.drools.model.Rule r : t.model().getRules()) {
            RuleOrigin o = t.originOf(r.getName());
            if (form.equals(o.form()))
                out.add(r.getName() + " -> " + o.kind() + " " + o.role().key()
                        + (o.pointer() == null ? "" : " " + o.pointer()) + (o.fold() == null ? "" : " vouw " + o.fold()));
        }
        return out;
    }

    @Test void a_flat_group_names_each_default_by_its_place_in_the_group() throws Exception {
        // the group "premie" is the fourth of the document, so its defaults are /rules/prioritizedDefaults/3/defaults/j
        DroolsBackend.Build b = build("jutterswet-premiestaffel", false);
        String stack = "defaults A:Jutter.premie", g = "/rules/prioritizedDefaults/3";
        assertEquals(List.of(
                stack + ": default 0 -> DEFAULT own " + g + "/defaults/0",
                stack + ": default 1 -> DEFAULT own " + g + "/defaults/1",
                stack + ": default 2 -> DEFAULT own " + g + "/defaults/2",
                stack + ": bijdrage aan vouw 0 -> DEFAULT fold-contribution " + g + "/defaults/0 vouw " + g
                        + "/defaults/0/justification/left",
                stack + ": bijdrage aan vouw 1 -> DEFAULT fold-contribution " + g + "/defaults/1 vouw " + g
                        + "/defaults/1/justification/left"), table(b, stack));
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
        // the rules that reconcile the exception groups of a default name that default, as does its fall-back, and the
        // reconciliation of the root is the stack's own
        DroolsBackend.Build b = build("anchor/jutterswet-nestdiepte", false);
        String stack = "defaults A:Jutter.heffing", d0 = "/rules/prioritizedDefaults/0/defaults/0";
        String d010 = d0 + "/exceptions/members/1/defaults/0";
        assertEquals(List.of(
                stack + ": default 0.0.0 -> DEFAULT own " + d0 + "/exceptions/members/0/defaults/0",
                stack + ": default 0.1.0.0.0 -> DEFAULT own " + d010 + "/exceptions/members/0/defaults/0",
                stack + ": default 0.1.0.1.0 -> DEFAULT own " + d010 + "/exceptions/members/1/defaults/0",
                stack + ": verschil 0.1.0 -> DEFAULT reconcile-difference " + d010,
                stack + ": eens 0.1.0 -> DEFAULT reconcile-agreement " + d010,
                stack + ": terugval 0.1.0 -> DEFAULT fallback " + d010,
                stack + ": doorgeven 0 -> DEFAULT reconcile-pass-on " + d0,
                stack + ": verschil 0 -> DEFAULT reconcile-difference " + d0,
                stack + ": eens 0 -> DEFAULT reconcile-agreement " + d0,
                stack + ": terugval 0 -> DEFAULT fallback " + d0,
                stack + ": doorgeven wortel -> STACK reconcile-pass-on",
                stack + ": eens wortel -> STACK reconcile-agreement"), table(b, stack));
    }

    @Test void a_subtype_stack_names_the_ordinary_rule_it_took_in() throws Exception {
        // Wrakrecht is a Strandrecht: its stack takes the default of the group on Strandrecht and, as its base, the
        // ordinary rule on Wrakrecht (RUNTIME-SEMANTICS ch. 7.10), named by that rule's version; the rule's own form
        // keeps its own rule, on the types the stack does not take
        DroolsBackend.Build b = build("jutterswet-wrakbasis", true);
        String stack = "defaults A:Wrakrecht.uitkering";
        assertEquals(List.of(
                stack + ": default 0 -> DEFAULT own /rules/prioritizedDefaults/0/defaults/0",
                stack + ": default 1 -> DEFAULT own /rules/rules/0/versions/0",
                stack + ": default 0: lezing -> DEFAULT reading /rules/prioritizedDefaults/0/defaults/0",
                stack + ": default 1: lezing -> DEFAULT reading /rules/rules/0/versions/0",
                // below the inherited default, the rule has a speculative reading rule as well (ch. 5.12)
                stack + ": default 1: lezing buiten de beurt -> DEFAULT reading /rules/rules/0/versions/0"),
                table(b, stack));
        assertEquals("uitkering van een wrakrecht", document("jutterswet-wrakbasis").at("/rules/rules/0/name").asText());
        TmsTranspiler t = b.execModelTranspiler();
        assertEquals(new RuleOrigin("uitkering van een wrakrecht", RuleOrigin.Kind.FORM, RuleOrigin.Role.OWN, null, null),
                t.originOf("uitkering van een wrakrecht"));
        // a reading rule is in no form for formOf, and has the origin of the rule it reads, in its own role
        assertEquals(null, t.formOf(stack + ": default 1: lezing"));
    }

    @Test void two_stacks_of_one_member_on_a_type_and_its_subtype_each_decide_the_default_of_the_supertype()
            throws Exception {
        // a Strandjutter is a Jutter: the group on Jutter.meldtermijn decides in the stack of Jutter and, below the
        // group on Strandjutter, in the stack of Strandjutter (ch. 7.10); the rule of its one default is in each stack
        // its own decision, with the same pointer, and the stack of the subtype reconciles its two groups at the root
        DroolsBackend.Build b = build("jutterswet-meldtermijn-strandjutter", false);
        String jutter = "defaults A:Jutter.meldtermijn", strandjutter = "defaults A:Strandjutter.meldtermijn";
        String onJutter = "/rules/prioritizedDefaults/0/defaults/0", onStrandjutter = "/rules/prioritizedDefaults/1/defaults/0";
        assertEquals(List.of(jutter + ": default 0 -> DEFAULT own " + onJutter), table(b, jutter));
        assertEquals(List.of(
                strandjutter + ": default 0.0.0 -> DEFAULT own " + onStrandjutter + "/exceptions/members/0/defaults/0",
                strandjutter + ": eens 0 -> DEFAULT reconcile-agreement " + onStrandjutter,
                strandjutter + ": default 1 -> DEFAULT own " + onJutter,
                strandjutter + ": eens wortel -> STACK reconcile-agreement"), table(b, strandjutter));
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
        String d40 = "/rules/prioritizedDefaults/4/defaults/0";
        assertEquals(List.of(
                "defaults K:Jutter.ijverig: default 0 -> DEFAULT own " + d40,
                "defaults K:Jutter.ijverig: default 1 -> DEFAULT own /rules/prioritizedDefaults/4/defaults/1",
                "defaults K:Jutter.ijverig: bijdrage aan vouw 2 -> DEFAULT fold-contribution " + d40 + " vouw " + d40
                        + "/justification/parts/0/left",
                "defaults K:Jutter.ijverig: bijdrage aan vouw 3 -> DEFAULT fold-contribution " + d40 + " vouw " + d40
                        + "/justification/parts/1/left"),
                table(b, "defaults K:Jutter.ijverig"));
        RuleOrigin sum = t.originOf("defaults A:Jutter.premie: bijdrage aan vouw 0");
        assertEquals(new RuleOrigin("defaults A:Jutter.premie", RuleOrigin.Kind.DEFAULT,
                RuleOrigin.Role.FOLD_CONTRIBUTION, "/rules/prioritizedDefaults/3/defaults/0",
                "/rules/prioritizedDefaults/3/defaults/0/justification/left"), sum);
        // what a reader shows of the fold: the text of its node
        assertEquals(ir.at("/rules/prioritizedDefaults/3/defaults/0/justification/left/source/snippet").asText(),
                ir.at(sum.fold()).path("source").path("snippet").asText());
        // a fold an ordinary form reads is that form's, beside a stack whose default folds too
        DroolsBackend.Build m = build("jutterswet-dringende-melding", false);
        assertEquals(new RuleOrigin("dringende melding", RuleOrigin.Kind.FORM, RuleOrigin.Role.FOLD_CONTRIBUTION, null,
                "/rules/rules/1/versions/0/condition"),
                m.execModelTranspiler().originOf("dringende melding: bijdrage aan vouw 0"));
        assertEquals(new RuleOrigin("defaults K:Melding.voorrang", RuleOrigin.Kind.DEFAULT,
                RuleOrigin.Role.FOLD_CONTRIBUTION, "/rules/prioritizedDefaults/0/defaults/0",
                "/rules/prioritizedDefaults/0/defaults/0/justification"),
                m.execModelTranspiler().originOf("defaults K:Melding.voorrang: bijdrage aan vouw 4"));
        assertEquals("QuantifierFold", document("jutterswet-dringende-melding")
                .at("/rules/prioritizedDefaults/0/defaults/0/justification/node").asText());
    }

    @Test void a_fold_in_the_filter_of_a_fold_names_its_own_node() throws Exception {
        // the inner fold stands in the filter of the outer one, so the lowering builds it while it builds the outer
        // fold's contribution rule; each names its own node: an aggregation in an aggregation, and a quantifier fold in
        // one. The inner folds are anchored to the form's subject ("van de Vergunning zelf"), so all four are folds of
        // the Vergunning
        DroolsBackend.Build b = build("anchor-forms", false);
        JsonNode ir = document("anchor-forms");
        String equal = "/rules/rules/6/versions/0/value", beside = "/rules/rules/3/versions/0/value";
        assertEquals(List.of(
                "gelijk aan later tarief -> FORM own",
                "gelijk aan later tarief: bijdrage aan vouw 7 -> FORM fold-contribution vouw " + equal,
                "gelijk aan later tarief: bijdrage aan vouw 8 -> FORM fold-contribution vouw " + equal
                        + "/filter/parts/1/right"),
                table(b, "gelijk aan later tarief"));
        assertEquals(List.of(
                "tarief naast duur register -> FORM own",
                "tarief naast duur register: bijdrage aan vouw 3 -> FORM fold-contribution vouw " + beside,
                "tarief naast duur register: bijdrage aan vouw 4 -> FORM fold-contribution vouw " + beside
                        + "/filter/parts/1"),
                table(b, "tarief naast duur register"));
        assertEquals(List.of("Aggregation", "Aggregation", "Aggregation", "QuantifierFold"), List.of(
                ir.at(equal + "/node").asText(), ir.at(equal + "/filter/parts/1/right/node").asText(),
                ir.at(beside + "/node").asText(), ir.at(beside + "/filter/parts/1/node").asText()));
        assertEquals("de maximale waarde van het tarief van alle vergunningen van het register van de Vergunning zelf die"
                + " later ingaan dan de Vergunning zelf die zijn ingangsdag is later dan de ingangsdag van de Vergunning"
                + " zelf", ir.at(equal + "/filter/parts/1/right/source/snippet").asText());
    }

    /** "De meldtermijn van een Jutter is zijn aanmelddatum indien {@code justification}", and below it "… indien hij
     *  niet gemeld is", on a Jutter with an aanmelddatum and Vondsten with a vondstdatum and wachtdagen. */
    private static Path meldtermijn(String justification) throws Exception {
        String model = TmsDocuments.model(List.of(
                objectType("Jutter", List.of(attribute("aanmelddatum", DATE), attribute("meldtermijn", DATE)),
                        List.of("gemeld")),
                objectType("Vondst", List.of(attribute("vondstdatum", DATE),
                        attribute("wachtdagen", numberType(true, 0, unit("dagen")))), List.of())),
                List.of(factTypeOneToMany("aanmelding", "aanmelder", "Jutter", "vondst", "Vondst", "vondsten")));
        String aanmelddatum = ownRead("aanmelddatum", "Jutter");
        String group = attributeGroup("meldtermijn van een jutter", "Jutter", "meldtermijn", List.of(
                defaultEntry(justification, aanmelddatum), defaultEntry(check("gemeld", false), aanmelddatum)));
        return TmsDocuments.document(model, List.of(), List.of(), List.of(group));
    }

    private static final String VONDSTEN = "{\"node\":\"AttributeReference\",\"attribute\":\"vondsten\","
            + "\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"vondsten van de Jutter\"}}";

    @Test void the_fault_rule_of_a_justification_registers_its_folds_under_its_default() throws Exception {
        // "… indien het aantal vondsten van de Jutter groter is dan 2 en zijn aanmelddatum plus zijn wachtdagen later
        // is dan 2026-01-01": the date step can fault, so the default has a fault rule, which registers the reads of
        // the justification at once, the count's fold among them, before the stack registers the reads of the
        // default's own rule. No golden has that shape: in the corpus no justification that reads a fold can fault.
        // The lower default reads no fold
        String model = TmsDocuments.model(List.of(
                objectType("Jutter", List.of(attribute("aanmelddatum", DATE),
                        attribute("wachtdagen", numberType(true, 0, unit("dagen"))), attribute("meldtermijn", DATE)),
                        List.of("gemeld")),
                objectType("Vondst", List.of(attribute("taxatie", numberType(true, 0, null))), List.of())),
                List.of(factTypeOneToMany("aanmelding", "aanmelder", "Jutter", "vondst", "Vondst", "vondsten")));
        String count = aggregation("count", VONDSTEN, "Vondst", sourceRole("aanmelding", "vondst"), "null", "null",
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
                stack + ": default 0 -> DEFAULT own " + d0,
                stack + ": default 0: fout in de voorwaarde -> DEFAULT condition-fault " + d0,
                stack + ": default 1 -> DEFAULT own /rules/prioritizedDefaults/0/defaults/1",
                stack + ": bijdrage aan vouw 0 -> DEFAULT fold-contribution " + d0 + " vouw " + d0
                        + "/justification/parts/0/left"), table(b, stack));
    }

    @Test void the_fault_rule_of_a_filter_in_a_justification_names_the_fold_and_its_default() throws Exception {
        // "… indien het aantal vondsten van de Jutter waarvan de vondstdatum plus de wachtdagen later is dan 2026-01-01
        // groter is dan 2": the date step in the filter can fault for a member, so the fold has a filter fault rule
        // beside its contribution rule, and both name the fold in the justification and its default; the read of the
        // fold can fault in turn, so the default has the fault rule of its justification. No golden has a fold of a
        // default whose filter can fault
        String filter = comparison("later", binary("plus", ownRead("vondstdatum", "Vondst"),
                ownRead("wachtdagen", "Vondst"), DATE), literal("2026-01-01", DATE));
        String count = aggregation("count", VONDSTEN, "Vondst", sourceRole("aanmelding", "vondst"), filter, "null",
                COUNTING, "het aantal vondsten van de Jutter waarvan de vondstdatum plus de wachtdagen later is dan"
                        + " 2026-01-01");
        Path document = meldtermijn(comparison("greater", count, number("2")));
        DroolsBackend.Build b = DroolsBackend.build(document, DroolsBackend.Branch.DRL_TEXT, false);
        String stack = "defaults A:Jutter.meldtermijn", d0 = "/rules/prioritizedDefaults/0/defaults/0";
        String fold = d0 + "/justification/left";
        assertEquals(List.of(
                stack + ": default 0 -> DEFAULT own " + d0,
                stack + ": default 0: fout in de voorwaarde -> DEFAULT condition-fault " + d0,
                stack + ": default 1 -> DEFAULT own /rules/prioritizedDefaults/0/defaults/1",
                stack + ": bijdrage aan vouw 0 -> DEFAULT fold-contribution " + d0 + " vouw " + fold,
                stack + ": filterfout in vouw 0 -> DEFAULT fold-filter-fault " + d0 + " vouw " + fold),
                table(b, stack));
        JsonNode ir = TmsDocuments.JSON.readTree(document.toFile());
        assertEquals("Aggregation", ir.at(fold + "/node").asText());
        assertEquals("het aantal vondsten van de Jutter waarvan de vondstdatum plus de wachtdagen later is dan"
                + " 2026-01-01", ir.at(fold + "/source/snippet").asText());
    }

    @Test void the_helper_rules_of_an_ordinary_form_have_the_roles_of_their_kind() throws Exception {
        // a condition that faults and deposits, and the folds of a value and of a condition, with the filter fault and
        // the filter deposit of a filter that divides
        DroolsBackend.Build f = build("fold-filter-divides-by-zero", false);
        String value = "/rules/rules/1/versions/0/value", condition = "/rules/rules/2/versions/0/condition/parts/1/left";
        assertEquals(List.of(
                "zware vondsten -> FORM own",
                "zware vondsten: bijdrage aan vouw 1 -> FORM fold-contribution vouw " + value,
                "zware vondsten: filterfout in vouw 1 -> FORM fold-filter-fault vouw " + value,
                "zware vondsten: filterdeponering in vouw 1 -> FORM fold-filter-deposit vouw " + value),
                table(f, "zware vondsten"));
        assertEquals(List.of(
                "drukte -> FORM own",
                "drukte: fout in de voorwaarde -> FORM condition-fault",
                "drukte: deponering in de voorwaarde -> FORM condition-deposit",
                "drukte: bijdrage aan vouw 2 -> FORM fold-contribution vouw " + condition,
                "drukte: filterfout in vouw 2 -> FORM fold-filter-fault vouw " + condition,
                "drukte: filterdeponering in vouw 2 -> FORM fold-filter-deposit vouw " + condition),
                table(f, "drukte"));
        // a uniqueness over two sources, each with a key on a timeline, and an allocation
        assertEquals(List.of(
                "eenmalig vondstnummer: sleutel van Strandvondst 1 -> FORM uniqueness-key",
                "eenmalig vondstnummer: sleutelvorm van Strandvondst 1 -> FORM uniqueness-key-form-fault",
                "eenmalig vondstnummer: sleutel van Wrakvondst 2 -> FORM uniqueness-key",
                "eenmalig vondstnummer: sleutelvorm van Wrakvondst 2 -> FORM uniqueness-key-form-fault",
                "eenmalig vondstnummer -> FORM own",
                "eenmalig vondstnummer: kanaal -> FORM uniqueness-channel"),
                table(build("anchor/jutterswet-nummerregister", false), "eenmalig vondstnummer"));
        assertEquals(List.of(
                "verdeel de opbrengst van een berging: ontvanger -> FORM allocation-recipient",
                "verdeel de opbrengst van een berging -> FORM own"),
                table(build("jutterswet-bergersdeling", false), "verdeel de opbrengst van een berging"));
    }

    @Test void a_stack_lowers_itself_where_no_default_is_at_hand() throws Exception {
        // a group with a dated validity: the fault of a session without an evaluation date is the stack's own
        DroolsBackend.Build b = build("jutterswet-uitsluiting-gedateerd", false);
        assertEquals(new RuleOrigin("defaults K:Jutter.uitgesloten", RuleOrigin.Kind.STACK,
                        RuleOrigin.Role.NO_EVALUATION_DATE, null, null),
                b.execModelTranspiler().originOf("defaults K:Jutter.uitgesloten: geen rekendatum"));
    }

    @Test void a_rule_of_no_form_has_no_form_and_a_name_of_no_rule_is_refused() throws Exception {
        TmsTranspiler t = build("jutterswet-dringende-melding", false).execModelTranspiler();
        assertEquals(new RuleOrigin(null, RuleOrigin.Kind.NO_FORM, null, null, null),
                t.originOf("kenmerkstand Vondst.zwaar: onwaar"));
        assertThrows(IllegalArgumentException.class, () -> t.originOf("geen regel van dit model"));
    }
}
