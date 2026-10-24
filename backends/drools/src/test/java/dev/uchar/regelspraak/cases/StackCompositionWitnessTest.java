package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.transpiler.tms.DefaultsDecision;
import dev.uchar.regelspraak.transpiler.tms.StackComposition;
import dev.uchar.regelspraak.transpiler.tms.StackComposition.Entry;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribute;
import static dev.uchar.regelspraak.cases.TmsDocuments.attributeGroup;
import static dev.uchar.regelspraak.cases.TmsDocuments.binary;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.defaultEntry;
import static dev.uchar.regelspraak.cases.TmsDocuments.groupDefault;
import static dev.uchar.regelspraak.cases.TmsDocuments.instance;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.objectType;
import static dev.uchar.regelspraak.cases.TmsDocuments.subtype;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The composition of a stack in {@code TmsTranspiler.stacks} on goldens that show each shape (card drl-rule-part,
 * acceptance (b)), and on a document written here for the shape of the workbench's measurement on v2 that no golden
 * has: a subtype's group with three deviations above the inherited base. Per stack the whole composition, chain by
 * chain and default by default, with the exception groups below their default, and whether chains stand side by side;
 * beside it, where the shape needs it, what the toolchain reads at a pointer: the name of the group or of the
 * ordinary rule. {@link StackCompositionCorpusWitnessTest} holds the same over the whole corpus.
 */
class StackCompositionWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final String P = "/rules/prioritizedDefaults/";

    private static DroolsBackend.Build build(String golden) throws Exception {
        return DroolsBackend.build(GOLDEN.resolve(golden).resolve("ir.json"), DroolsBackend.Branch.EXEC_MODEL);
    }

    private static JsonNode document(String golden) throws Exception {
        return TmsDocuments.JSON.readTree(GOLDEN.resolve(golden).resolve("ir.json").toFile());
    }

    /** The composition of {@code stack}, which must be a stack of {@code t}. */
    private static StackComposition composition(TmsTranspiler t, String stack) {
        TmsTranspiler.Stack s = t.stacks().get(stack);
        assertTrue(s != null, () -> "geen stapel " + stack + ": " + t.stacks().keySet());
        return s.composition();
    }

    private static Entry entry(String pointer) {
        return new Entry(pointer, null);
    }

    private static Entry entry(String pointer, StackComposition exceptions) {
        return new Entry(pointer, exceptions);
    }

    private static List<Entry> chain(Entry... entries) {
        return List.of(entries);
    }

    @SafeVarargs
    private static StackComposition chains(List<Entry>... chains) {
        return new StackComposition(List.of(chains));
    }

    @Test void a_subtype_stack_has_its_own_exceptions_above_the_inherited_ones_and_its_own_base() throws Exception {
        // ch. 7.10.1: the effective Wrakrecht stack for uitkering is the own 55-exception, the inherited 99-exception
        // and the own base, which replaces the inherited one; the Strandrecht stack is its own group as written
        TmsTranspiler t = build("anchor/jutterswet-strandrechtketen").execModelTranspiler();
        String strandrecht = P + "0/defaults/", wrakrecht = P + "1/defaults/";
        assertEquals(chains(chain(entry(strandrecht + 0), entry(strandrecht + 1))),
                composition(t, "defaults A:Strandrecht.uitkering"));
        StackComposition c = composition(t, "defaults A:Wrakrecht.uitkering");
        assertEquals(chains(chain(entry(wrakrecht + 0), entry(strandrecht + 0), entry(wrakrecht + 1))), c);
        assertFalse(c.unordered());
        // what the toolchain reads there: the group of each default, and the default itself
        JsonNode ir = document("anchor/jutterswet-strandrechtketen");
        assertEquals(List.of("uitkering van een wrakrecht", "uitkering van een strandrecht", "uitkering van een wrakrecht"),
                List.of(ir.at(P + "1/name").asText(), ir.at(P + "0/name").asText(), ir.at(P + "1/name").asText()));
        assertEquals(List.of("55.00", "99.00", "zijn grondslag maal 2"), List.of(
                ir.at(wrakrecht + "0/consequence/source/snippet").asText(),
                ir.at(strandrecht + "0/consequence/source/snippet").asText(),
                ir.at(wrakrecht + "1/consequence/source/snippet").asText()));
    }

    @Test void the_group_of_a_subtype_with_its_deviation_stands_above_the_inherited_base() throws Exception {
        // the RegelSpraak "Afwijking van meldtermijn: meldtermijn tot het vergunningseinde", written for a Strandjutter:
        // the frontend makes the group "meldtermijn voor Strandjutter" of one group-default with one exception group,
        // and the stack of a Strandjutter puts it above the base of the group on Jutter, which it inherits
        TmsTranspiler t = build("jutterswet-meldtermijn-strandjutter").execModelTranspiler();
        String base = P + "0/defaults/0", group = P + "1/defaults/0";
        assertEquals(chains(chain(entry(base))), composition(t, "defaults A:Jutter.meldtermijn"));
        StackComposition c = composition(t, "defaults A:Strandjutter.meldtermijn");
        assertEquals(chains(chain(entry(group, chains(chain(entry(group + "/exceptions/members/0/defaults/0")))),
                entry(base))), c);
        assertFalse(c.unordered());
        assertFalse(c.chains().get(0).get(0).exceptions().unordered());
        JsonNode ir = document("jutterswet-meldtermijn-strandjutter");
        assertEquals(List.of("meldtermijn voor Strandjutter", "meldtermijn tot het vergunningseinde", "meldtermijn"),
                List.of(ir.at(P + "1/name").asText(), ir.at(group + "/exceptions/members/0/defaults/0/name").asText(),
                        ir.at(P + "0/name").asText()));
    }

    /** A Recht with a herlevingstermijn and a subtype RechtBijOntslag, the shape of the workbench's measurement on v2:
     *  the base on Recht, and for a RechtBijOntslag the group the RegelSpraak frontend makes of three
     *  {@code Afwijking van} blocks written for it, one group-default with one ordered exception group, its later-written
     *  block highest, the middle one with an {@code Uitzondering op} of its own (on v2 the labels {@code default
     *  0.0.1.0.0} and {@code eens 0.0.1}). */
    private static Path deviations() throws Exception {
        String model = TmsDocuments.model(List.of(
                objectType("Recht", List.of(attribute("herlevingstermijn", WHOLE)),
                        List.of("herleefbaar", "ontslagen", "ziek", "langdurig", "mantelzorger")),
                subtype("RechtBijOntslag", "Recht", List.of(), List.of())), List.of());
        String base = attributeGroup("herlevingstermijn", "Recht", "herlevingstermijn",
                List.of(defaultEntry(check("herleefbaar", false), number("12"))));
        String own = attributeGroup("herlevingstermijn voor RechtBijOntslag", "RechtBijOntslag", "herlevingstermijn",
                List.of(groupDefault(List.of(List.of(
                        defaultEntry(check("mantelzorger", false), number("36")),
                        TmsDocuments.withExceptions(check("ziek", false), number("24"), List.of(List.of(
                                defaultEntry(TmsDocuments.compound("all", 0, List.of(check("ziek", false),
                                        check("langdurig", false))), number("30"))))),
                        defaultEntry(check("ontslagen", false), number("18")))))))
                // an Afwijking van and an Uitzondering op lower to one ordered exception group (grammar/DIVERGENCES.md,
                // form 2)
                .replace("\"unordered\":true,\"members\"", "\"unordered\":false,\"members\"");
        return TmsDocuments.document(model, List.of(), List.of(), List.of(base, own));
    }

    @Test void three_deviations_of_a_subtype_stand_below_its_group_in_the_order_of_the_document() throws Exception {
        Path ir = deviations();
        String base = P + "0/defaults/0", group = P + "1/defaults/0", member = group + "/exceptions/members/0/defaults/";
        String nested = member + "1/exceptions/members/0/defaults/0";
        StackComposition expected = chains(chain(entry(group, chains(chain(entry(member + 0),
                entry(member + 1, chains(chain(entry(nested)))), entry(member + 2)))), entry(base)));
        JsonNode c = TmsDocuments.caseDocument(List.of(
                instance("O1", "RechtBijOntslag", Map.of(), List.of("herleefbaar", "ziek", "ontslagen"), Map.of()),
                instance("O2", "RechtBijOntslag", Map.of(), List.of("herleefbaar", "ziek", "langdurig"), Map.of()),
                instance("R1", "Recht", Map.of(), List.of("herleefbaar", "ziek"), Map.of())), List.of());
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            List<String> records = CaseRunner.forMeasurement(ir, branch).withSession(c, (build, session, labels) -> {
                TmsTranspiler t = build.execModelTranspiler();
                assertEquals(expected, composition(t, "defaults A:RechtBijOntslag.herlevingstermijn"), branch.name());
                assertEquals(chains(chain(entry(base))), composition(t, "defaults A:Recht.herlevingstermijn"),
                        branch.name());
                assertEquals(null, DroolsBackend.fire(build, session).noFixpoint(), branch + ": een fixpunt");
                Map<Object, String> label = new IdentityHashMap<>();
                labels.forEach((l, o) -> label.put(o, l));
                List<String> out = new ArrayList<>();
                for (DefaultsDecision d : DroolsBackend.decisionsOf(build, session)) {
                    String stack = "defaults A:" + (label.get(d.object()).startsWith("O") ? "RechtBijOntslag" : "Recht")
                            + ".herlevingstermijn";
                    // the root chains of every record of a stack are the root of its composition
                    assertEquals(StackCompositionCorpusWitnessTest.pointers(composition(t, stack)), d.rootChains(),
                            branch + ": " + stack);
                    out.add(label.get(d.object()) + " " + d.decider().pointer() + " " + d.decider().decision());
                }
                return out.stream().sorted().toList();
            });
            // O1 is ziek and ontslagen: the higher of the two deviations decides, above the base; O2 is ziek and
            // langdurig, and the exception of that deviation decides; the Recht has the base alone
            assertEquals(List.of("O1 " + member + "1 24", "O2 " + nested + " 30", "R1 " + base + " 12"), records,
                    branch.name());
        }
    }

    @Test void a_nested_exception_stands_below_its_default_at_every_depth() throws Exception {
        // ch. 7.9, nest depth: default 0 with the unordered siblings s0 and s1, s1 with its own unordered pair
        TmsTranspiler t = build("anchor/jutterswet-nestdiepte").execModelTranspiler();
        String d0 = P + "0/defaults/0", s1 = d0 + "/exceptions/members/1/defaults/0";
        StackComposition c = composition(t, "defaults A:Jutter.heffing");
        assertEquals(chains(chain(entry(d0, chains(
                chain(entry(d0 + "/exceptions/members/0/defaults/0")),
                chain(entry(s1, chains(
                        chain(entry(s1 + "/exceptions/members/0/defaults/0")),
                        chain(entry(s1 + "/exceptions/members/1/defaults/0"))))))))), c);
        // one group at the root, ordered; the exception groups side by side at both depths
        assertFalse(c.unordered());
        Entry root = c.chains().get(0).get(0);
        assertTrue(root.exceptions().unordered());
        assertNull(root.exceptions().chains().get(0).get(0).exceptions());
        assertTrue(root.exceptions().chains().get(1).get(0).exceptions().unordered());
    }

    @Test void groups_side_by_side_at_the_root_are_unordered_and_a_subtype_inherits_them_whole() throws Exception {
        // three declared-unordered groups on Jutter.wachtgeld: three chains of one default each, side by side
        TmsTranspiler w = build("jutterswet-wachtgeld").execModelTranspiler();
        StackComposition c = composition(w, "defaults A:Jutter.wachtgeld");
        assertEquals(chains(chain(entry(P + "0/defaults/0")), chain(entry(P + "1/defaults/0")),
                chain(entry(P + "2/defaults/0"))), c);
        assertTrue(c.unordered());
        // two declared-unordered groups on Strandrecht.uitkering, and Wrakrecht declares nothing on it: its stack is the
        // inherited set, whole (ch. 7.10, an inherited declared-unordered set)
        TmsTranspiler k = build("jutterswet-wrakconflict").execModelTranspiler();
        StackComposition set = chains(chain(entry(P + "0/defaults/0")), chain(entry(P + "1/defaults/0")));
        assertEquals(set, composition(k, "defaults A:Strandrecht.uitkering"));
        assertEquals(set, composition(k, "defaults A:Wrakrecht.uitkering"));
        assertTrue(composition(k, "defaults A:Wrakrecht.uitkering").unordered());
    }

    @Test void a_subtype_stack_takes_in_an_ordinary_rule_by_its_version() throws Exception {
        // rule 1 of SUBTYPING §4.1: the ordinary rule on Wrakrecht is its own base, below the inherited exception
        TmsTranspiler t = build("jutterswet-wrakbasis").execModelTranspiler();
        assertEquals(chains(chain(entry(P + "0/defaults/0"), entry("/rules/rules/0/versions/0"))),
                composition(t, "defaults A:Wrakrecht.uitkering"));
        assertEquals(chains(chain(entry(P + "0/defaults/0"), entry(P + "0/defaults/1"))),
                composition(t, "defaults A:Strandrecht.uitkering"));
        assertEquals("uitkering van een wrakrecht", document("jutterswet-wrakbasis").at("/rules/rules/0/name").asText());
        // three levels, the group on the middle one: on a Diepwrak its own rule replaces the group's base, below the
        // inherited exception; on a Wrakrecht the group's own base replaces the rule on Strandrecht
        TmsTranspiler d = build("jutterswet-diepwrakbasis").execModelTranspiler();
        assertEquals(chains(chain(entry(P + "0/defaults/0"), entry("/rules/rules/1/versions/0"))),
                composition(d, "defaults A:Diepwrak.uitkering"));
        assertEquals(chains(chain(entry(P + "0/defaults/0"), entry(P + "0/defaults/1"))),
                composition(d, "defaults A:Wrakrecht.uitkering"));
        assertEquals("uitkering van een diepwrak", document("jutterswet-diepwrakbasis").at("/rules/rules/1/name").asText());
        // an initialisation on Vondst becomes the initialisation default of the Wrakvondst stack, lowest
        TmsTranspiler i = build("jutterswet-wrakinitialisatie").execModelTranspiler();
        assertEquals(chains(chain(entry(P + "0/defaults/0"), entry(P + "0/defaults/1"), entry("/rules/rules/0/versions/0"))),
                composition(i, "defaults A:Wrakvondst.bergloon"));
        assertTrue(document("jutterswet-wrakinitialisatie").at("/rules/rules/0/versions/0/initialisation").asBoolean());
    }

    @Test void a_refused_stack_has_no_composition_and_a_name_of_no_stack_no_entry() throws Exception {
        // a division in a default is refused (DROOLS: the division of a default), so the stack lays no knots; the
        // analysis built here, without the backend, which refuses such a document whole
        String model = TmsDocuments.model(List.of(objectType("Jutter", List.of(attribute("heffing", WHOLE)),
                List.of("zwaar"))), List.of());
        Path ir = TmsDocuments.document(model, List.of(), List.of(), List.of(attributeGroup("heffing", "Jutter", "heffing",
                List.of(defaultEntry(check("zwaar", false), binary("dividedBy", number("10"), number("2")))))));
        IrLoader.Positioned positioned = IrLoader.loadWithPositions(TmsDocuments.JSON.readTree(ir.toFile()));
        ClassLoader cl = dev.uchar.regelspraak.engine.BeanCompiler.compile(
                dev.uchar.regelspraak.schema.FactModelGenerator.generate(positioned.loaded().model()));
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        TmsTranspiler t;
        try {
            t = TmsTranspiler.build(positioned, cl);
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
        String stack = "defaults A:Jutter.heffing";
        assertTrue(t.coverage().stream().anyMatch(d -> d.name().equals(stack) && !d.lowered()), () -> "geweigerd: "
                + t.coverage());
        // the refused stack has its type and no composition, as the reason of the refusal is in the manifest
        assertEquals(new TmsTranspiler.Stack(RegelSpraakModel.Designatable.objectType("Jutter"), null),
                t.stacks().get(stack));
        // the name of the group is no stack's
        assertNull(t.stacks().get("heffing"));
    }
}
