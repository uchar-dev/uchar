package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.tms.DefaultsDecision;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static dev.uchar.regelspraak.cases.TmsDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribute;
import static dev.uchar.regelspraak.cases.TmsDocuments.attributeGroup;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicGroup;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.compound;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.decision;
import static dev.uchar.regelspraak.cases.TmsDocuments.decisionWithExceptions;
import static dev.uchar.regelspraak.cases.TmsDocuments.defaultEntry;
import static dev.uchar.regelspraak.cases.TmsDocuments.groupDefault;
import static dev.uchar.regelspraak.cases.TmsDocuments.initialisationEntry;
import static dev.uchar.regelspraak.cases.TmsDocuments.instance;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.objectType;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.subtype;
import static dev.uchar.regelspraak.cases.TmsDocuments.unordered;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@code DroolsBackend.decisionsOf}, the entry that says per object and effective defaults stack which default decided
 * (card defaults-decider): the pointer of the deciding default with its decision, a do-not-attribute decision
 * included, the conflict that stood, or no record where no default decided. Three small documents of Jutterswet
 * material, each case with its expected records written by hand from RUNTIME-SEMANTICS ch. 7, on both branches, which
 * render one analysis and must give the same records.
 *
 * <ul>
 *   <li>The simple form (one chain, no exception groups): the highest, a middle and the lowest default deciding; a
 *       do-not-attribute decision in the middle and one at the bottom, which until this card had no rule of its own and
 *       left nothing in the session; no default deciding; an attribute default whose consequence is leeg and falls
 *       through to a lower one; and the initialisation default (ch. 7.3), which decides only where the target is empty
 *       in the input.</li>
 *   <li>The general form: a decision two exception groups deep (ch. 7.9); the fall-back of a form-a default on its own
 *       decision, at the root and one level down; a conflict of a declared-unordered root (ch. 7.2), and its agreement,
 *       where the first deciding group is the decider; a conflict among the exception groups of a group-default, which
 *       goes up to the root under its path key.</li>
 *   <li>Inheritance (ch. 7.10): the stack of a subtype composed from its own group and its supertype's, the own
 *       exception above the inherited one and the own base in place of the inherited base, so that the order of
 *       evaluation is not that of either declaration; and below it a subtype whose ordinary rule the stack takes in as
 *       its base, a default named by its rule version.</li>
 * </ul>
 *
 * <p><b>Red</b> on {@code d9c0b9a5}, before the entry: this class did not compile, since {@code DroolsBackend} had no
 * {@code decisionsOf} and the backend no {@code DefaultsDecision}; the simple form left no control fact for its lowest
 * deciding default, nor for a do-not-attribute decision at the bottom, which got no rule at all.
 */
class DefaultsDecisionWitnessTest {

    private static final String P = "/rules/prioritizedDefaults/";

    // ---------------- the simple form ----------------

    private static final String SIMPLE_MODEL = TmsDocuments.model(List.of(objectType("Jutter",
            List.of(attribute("bonus", WHOLE), attribute("vergoeding", WHOLE), attribute("toeslag", WHOLE)),
            List.of("ijverig", "berispt", "erkend", "geschorst", "havenmeester", "vrijgesteld"))), List.of());

    private static final List<String> SIMPLE_GROUPS = List.of(
            characteristicGroup("vrijstelling van een jutter", "Jutter", "vrijgesteld", List.of(
                    decision(check("ijverig", false), true),
                    decision(check("berispt", false), false),
                    decision(check("erkend", false), true),
                    decision(check("geschorst", false), false))),
            attributeGroup("vergoeding van een jutter", "Jutter", "vergoeding", List.of(
                    defaultEntry(check("ijverig", false), ownRead("bonus", "Jutter")),
                    defaultEntry(check("erkend", false), number("10")),
                    defaultEntry(check("havenmeester", false), number("20")))),
            attributeGroup("toeslag van een jutter", "Jutter", "toeslag", List.of(
                    defaultEntry(check("ijverig", false), number("7")),
                    initialisationEntry(number("3")))));

    @Test void the_simple_form() throws Exception {
        Path ir = TmsDocuments.document(SIMPLE_MODEL, List.of(), List.of(), SIMPLE_GROUPS);
        JsonNode c = TmsDocuments.caseDocument(List.of(
                instance("J1", "Jutter", Map.of("bonus", "40"), List.of("ijverig", "erkend"), Map.of()),
                instance("J2", "Jutter", Map.of(), List.of("berispt", "erkend"), Map.of()),
                instance("J3", "Jutter", Map.of(), List.of("erkend"), Map.of()),
                instance("J4", "Jutter", Map.of(), List.of("geschorst", "havenmeester"), Map.of()),
                instance("J5", "Jutter", Map.of("toeslag", "9"), List.of(), Map.of()),
                instance("J6", "Jutter", Map.of(), List.of("ijverig", "havenmeester"), Map.of())), List.of());
        List<String> expected = List.of(
                // the highest default of each group decides
                "J1 toeslag: " + P + "2/defaults/0 7",
                "J1 vergoeding: " + P + "1/defaults/0 40",
                "J1 vrijgesteld: " + P + "0/defaults/0 true",
                // a do-not-attribute decision in the middle; a middle attribute default; the initialisation default
                "J2 toeslag: " + P + "2/defaults/1 3",
                "J2 vergoeding: " + P + "1/defaults/1 10",
                "J2 vrijgesteld: " + P + "0/defaults/1 false",
                // a middle attribution, below a do-not-attribute default that is not justified
                "J3 toeslag: " + P + "2/defaults/1 3",
                "J3 vergoeding: " + P + "1/defaults/1 10",
                "J3 vrijgesteld: " + P + "0/defaults/2 true",
                // a do-not-attribute decision at the bottom; the lowest attribute default
                "J4 toeslag: " + P + "2/defaults/1 3",
                "J4 vergoeding: " + P + "1/defaults/2 20",
                "J4 vrijgesteld: " + P + "0/defaults/3 false",
                // J5: no default decides, the initialisation not either, since toeslag holds 9 in the input
                // the highest vergoeding default is justified, its consequence leeg, and it falls through
                "J6 toeslag: " + P + "2/defaults/0 7",
                "J6 vergoeding: " + P + "1/defaults/2 20",
                "J6 vrijgesteld: " + P + "0/defaults/0 true");
        Map<String, List<List<String>>> chains = new TreeMap<>(Map.of(
                "Jutter toeslag", List.of(List.of(P + "2/defaults/0", P + "2/defaults/1")),
                "Jutter vergoeding", List.of(List.of(P + "1/defaults/0", P + "1/defaults/1", P + "1/defaults/2")),
                "Jutter vrijgesteld", List.of(List.of(P + "0/defaults/0", P + "0/defaults/1", P + "0/defaults/2",
                        P + "0/defaults/3"))));
        bothBranches(ir, c, Map.of("J1", "Jutter", "J2", "Jutter", "J3", "Jutter", "J4", "Jutter", "J5", "Jutter",
                "J6", "Jutter"), expected, chains);
    }

    // ---------------- the general form ----------------

    private static final String GENERAL_MODEL = TmsDocuments.model(List.of(objectType("Jutter",
            List.of(attribute("wachtgeld", WHOLE)),
            List.of("strandvoogd", "berispt", "gehoord", "kustwacht", "geschorst", "erkend", "havenmeester",
                    "penninggerechtigd"))), List.of());

    private static String all(String... parts) {
        List<String> p = new ArrayList<>();
        for (String part : parts) p.add(check(part, false));
        return compound("all", 0, p);
    }

    /** The penningrecht: a form-a default whose one exception group holds a form-a default with one exception group of
     *  its own; a group-default with two unordered members, which conflict where they decide differently; and a plain
     *  default. Beside it the wachtgeld of three declared-unordered groups at the root. */
    private static final List<String> GENERAL_GROUPS = List.of(
            characteristicGroup("penningrecht van een jutter", "Jutter", "penninggerechtigd", List.of(
                    decisionWithExceptions(check("strandvoogd", false), true, List.of(List.of(
                            decisionWithExceptions(all("strandvoogd", "berispt"), false, List.of(List.of(
                                    decision(all("strandvoogd", "berispt", "gehoord"), true))))))),
                    groupDefault(List.of(
                            List.of(decision(check("kustwacht", false), true)),
                            List.of(decision(all("kustwacht", "geschorst"), false)))),
                    decision(check("erkend", false), true))),
            unordered(attributeGroup("wachtgeld voor kustwachters", "Jutter", "wachtgeld",
                    List.of(defaultEntry(check("kustwacht", false), number("30"))))),
            unordered(attributeGroup("wachtgeld voor havenmeesters", "Jutter", "wachtgeld",
                    List.of(defaultEntry(check("havenmeester", false), number("45"))))),
            unordered(attributeGroup("wachtgeld voor strandvoogden", "Jutter", "wachtgeld",
                    List.of(defaultEntry(check("strandvoogd", false), number("30"))))));

    @Test void the_general_form() throws Exception {
        Path ir = TmsDocuments.document(GENERAL_MODEL, List.of(), List.of(), GENERAL_GROUPS);
        JsonNode c = TmsDocuments.caseDocument(List.of(
                instance("P1", "Jutter", Map.of(), List.of("strandvoogd", "berispt", "gehoord"), Map.of()),
                instance("P2", "Jutter", Map.of(), List.of("strandvoogd", "berispt"), Map.of()),
                instance("P3", "Jutter", Map.of(), List.of("strandvoogd"), Map.of()),
                instance("P4", "Jutter", Map.of(), List.of("kustwacht", "geschorst"), Map.of()),
                instance("P5", "Jutter", Map.of(), List.of("kustwacht"), Map.of()),
                instance("P6", "Jutter", Map.of(), List.of("erkend"), Map.of()),
                instance("P7", "Jutter", Map.of(), List.of(), Map.of()),
                instance("P8", "Jutter", Map.of(), List.of("kustwacht", "havenmeester"), Map.of()),
                instance("P9", "Jutter", Map.of(), List.of("kustwacht", "strandvoogd"), Map.of())), List.of());
        String r0 = P + "0/defaults/0", y0 = r0 + "/exceptions/members/0/defaults/0",
                z0 = y0 + "/exceptions/members/0/defaults/0", r1 = P + "0/defaults/1",
                w0 = r1 + "/exceptions/members/0/defaults/0", w1 = r1 + "/exceptions/members/1/defaults/0";
        List<String> expected = List.of(
                // two exception groups deep: z0 decides, y0 and r0 take its decision
                "P1 penninggerechtigd: " + z0 + " true",
                "P1 wachtgeld: " + P + "3/defaults/0 30",
                // y0 falls back on its own do-not-attribute decision, z0 not justified
                "P2 penninggerechtigd: " + y0 + " false",
                "P2 wachtgeld: " + P + "3/defaults/0 30",
                // r0 falls back on its own decision, no member settles
                "P3 penninggerechtigd: " + r0 + " true",
                "P3 wachtgeld: " + P + "3/defaults/0 30",
                // the two members of r1 decide differently: a conflict under r1's path key, no write
                "P4 penninggerechtigd: conflict at " + r1 + " under Jutter.penninggerechtigd/1: " + w0 + " true, " + w1
                        + " false",
                "P4 wachtgeld: " + P + "1/defaults/0 30",
                // one member of r1 decides
                "P5 penninggerechtigd: " + w0 + " true",
                "P5 wachtgeld: " + P + "1/defaults/0 30",
                "P6 penninggerechtigd: " + P + "0/defaults/2 true",
                // P7: nothing decides
                "P8 penninggerechtigd: " + w0 + " true",
                // two groups at the root decide differently: a conflict under the bare key
                "P8 wachtgeld: conflict at the root under Jutter.wachtgeld: " + P + "1/defaults/0 30, " + P
                        + "2/defaults/0 45",
                // r0 settles first and displaces r1
                "P9 penninggerechtigd: " + r0 + " true",
                // two groups at the root decide alike: the first in member order is the decider
                "P9 wachtgeld: " + P + "1/defaults/0 30");
        Map<String, List<List<String>>> chains = new TreeMap<>(Map.of(
                "Jutter penninggerechtigd", List.of(List.of(r0, r1, P + "0/defaults/2")),
                "Jutter wachtgeld", List.of(List.of(P + "1/defaults/0"), List.of(P + "2/defaults/0"),
                        List.of(P + "3/defaults/0"))));
        Map<String, String> types = new TreeMap<>();
        for (int i = 1; i <= 9; i++) types.put("P" + i, "Jutter");
        bothBranches(ir, c, types, expected, chains);
    }

    // ---------------- inheritance ----------------

    private static final String INHERITANCE_MODEL = TmsDocuments.model(List.of(
            objectType("Strandrecht", List.of(attribute("uitkering", WHOLE)), List.of("betwist", "gezonken", "geborgen")),
            subtype("Wrakrecht", "Strandrecht", List.of(), List.of()),
            subtype("Diepwrak", "Wrakrecht", List.of(), List.of())), List.of());

    @Test void a_stack_composed_from_two_declarations() throws Exception {
        Path ir = TmsDocuments.document(INHERITANCE_MODEL, List.of(
                computingRule("uitkering van een diepwrak", "GELIJKSTELLING", "Diepwrak", "uitkering", number("200"),
                        "null")), List.of(), List.of(
                attributeGroup("uitkering van een strandrecht", "Strandrecht", "uitkering", List.of(
                        defaultEntry(check("betwist", false), number("99")),
                        defaultEntry(compound("all", 0, List.of()), number("50")))),
                attributeGroup("uitkering van een wrakrecht", "Wrakrecht", "uitkering", List.of(
                        defaultEntry(check("gezonken", false), number("120")),
                        defaultEntry(check("geborgen", false), number("80"))))));
        JsonNode c = TmsDocuments.caseDocument(List.of(
                instance("S1", "Strandrecht", Map.of(), List.of("betwist"), Map.of()),
                instance("S2", "Strandrecht", Map.of(), List.of(), Map.of()),
                instance("W1", "Wrakrecht", Map.of(), List.of("betwist", "gezonken"), Map.of()),
                instance("W2", "Wrakrecht", Map.of(), List.of("betwist"), Map.of()),
                instance("W3", "Wrakrecht", Map.of(), List.of("geborgen"), Map.of()),
                instance("W4", "Wrakrecht", Map.of(), List.of(), Map.of()),
                instance("D1", "Diepwrak", Map.of(), List.of(), Map.of()),
                instance("D2", "Diepwrak", Map.of(), List.of("gezonken"), Map.of())), List.of());
        List<String> expected = List.of(
                "D1 uitkering: /rules/rules/0/versions/0 200",
                "D2 uitkering: " + P + "1/defaults/0 120",
                "S1 uitkering: " + P + "0/defaults/0 99",
                "S2 uitkering: " + P + "0/defaults/1 50",
                // the own exception above the inherited one
                "W1 uitkering: " + P + "1/defaults/0 120",
                // the inherited exception, second in the effective stack and first in its declaration
                "W2 uitkering: " + P + "0/defaults/0 99",
                // the own base, which replaces the inherited one
                "W3 uitkering: " + P + "1/defaults/1 80");
                // W4: the inherited base is not in the stack of a Wrakrecht, and nothing decides
        Map<String, List<List<String>>> chains = new TreeMap<>(Map.of(
                "Diepwrak uitkering", List.of(List.of(P + "1/defaults/0", P + "0/defaults/0", "/rules/rules/0/versions/0")),
                "Strandrecht uitkering", List.of(List.of(P + "0/defaults/0", P + "0/defaults/1")),
                "Wrakrecht uitkering", List.of(List.of(P + "1/defaults/0", P + "0/defaults/0", P + "1/defaults/1"))));
        bothBranches(ir, c, Map.of("S1", "Strandrecht", "S2", "Strandrecht", "W1", "Wrakrecht", "W2", "Wrakrecht",
                "W3", "Wrakrecht", "W4", "Wrakrecht", "D1", "Diepwrak", "D2", "Diepwrak"), expected, chains);
    }

    // ---------------- the reading ----------------

    /** The records of {@code c} on both branches, each the expected list, and the root chains of each stack that
     *  decided something, by the type of its objects and its member. */
    private static void bothBranches(Path ir, JsonNode c, Map<String, String> types, List<String> expected,
                                     Map<String, List<List<String>>> chains) throws Exception {
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            Map<String, List<List<String>>> seen = new TreeMap<>();
            List<String> records = CaseRunner.forMeasurement(ir, branch).withSession(c, (build, session, labels) -> {
                DroolsBackend.Firing firing = DroolsBackend.fire(build, session);
                assertEquals(null, firing.noFixpoint(), branch + ": een fixpunt");
                Map<Object, String> label = new IdentityHashMap<>();
                labels.forEach((l, o) -> label.put(o, l));
                List<String> out = new ArrayList<>();
                for (DefaultsDecision d : DroolsBackend.decisionsOf(build, session)) {
                    String l = label.get(d.object());
                    out.add(line(l, d));
                    seen.put(types.get(l) + " " + d.member(), d.rootChains());
                }
                return out.stream().sorted().toList();
            });
            assertEquals(expected, records, branch + ": de beslissingen");
            assertEquals(chains, seen, branch + ": de wortelketens van de stapels");
        }
    }

    /** A record as one line: the label, the member, and the decider with its decision or the conflict. */
    private static String line(String label, DefaultsDecision d) {
        if (d.decider() != null)
            return label + " " + d.member() + ": " + d.decider().pointer() + " " + text(d.decider().decision());
        List<String> deciders = new ArrayList<>();
        for (DefaultsDecision.Decider x : d.conflict().deciders()) deciders.add(x.pointer() + " " + text(x.decision()));
        return label + " " + d.member() + ": conflict at " + (d.conflict().pointer() == null ? "the root"
                : d.conflict().pointer()) + " under " + d.conflict().key() + ": " + String.join(", ", deciders);
    }

    private static String text(Object decision) {
        return decision instanceof BigDecimal b ? b.stripTrailingZeros().toPlainString() : String.valueOf(decision);
    }
}
