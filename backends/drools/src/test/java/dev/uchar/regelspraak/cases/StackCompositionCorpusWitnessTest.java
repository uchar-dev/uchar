package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.core.Namen;
import dev.uchar.regelspraak.transpiler.tms.DefaultsDecision;
import dev.uchar.regelspraak.transpiler.tms.StackComposition;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler.RuleOrigin;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code TmsTranspiler.stacks} over the corpus (card drl-rule-part, the composition of a stack): every effective
 * stack of every golden the backend carries, on the exec-model branch, whose analysis the toolchain reads
 * {@code generated} from, has a composition, and per stack:
 *
 * <ul>
 *   <li>every default stands in it once, and every pointer resolves in the golden's document to a node that writes the
 *       stack's member: a {@code Default} of a group on it, or the version of an ordinary rule whose target it is
 *       (ch. 7.10), which stands at the root and carries no exception groups;</li>
 *   <li>a default carries exception groups in the composition exactly where it does in the document, and they stand
 *       there in the order of the document: member {@code m}, default {@code j} is the default's pointer with
 *       {@code /exceptions/members/<m>/defaults/<j>} after it, as many members and defaults as the document has;</li>
 *   <li>chains side by side ({@code unordered}) are declared unordered in the document: at the root every group a
 *       chain names carries {@code unordered}, below a default its {@code exceptions} does;</li>
 *   <li>at the root, the defaults of one group stand in the group's order, and the order of the levels is the block
 *       order of ch. 7.10, read off the document: the initialisations last, and before the lowest ordinary default of
 *       a chain, the base, the levels that declare the defaults from the stack's type upward, so the type's own
 *       exceptions above the inherited ones and the child's above the grandparent's;</li>
 *   <li>against {@code originOf} of the stack's rules: every default of a rule of the stack stands in the composition,
 *       and a default that decides itself (an own consequence or decision, an ordinary rule) has exactly one rule of
 *       its own decision there, {@code fallback} where it carries exception groups and {@code own} where it carries
 *       none, while a group-default (exception groups and no own content) has none, only the rules that reconcile
 *       its exception groups;</li>
 *   <li>against {@code decisionsOf}: on every case of every golden with a defaults group that the exec-model branch
 *       plays, the root chains of each record are the chains at the root of its stack's composition, pointer for
 *       pointer. The stack of a record is the one whose type ({@code Stack.type}) is the object's type and whose member
 *       ({@code generated}) is the record's: no name is read.</li>
 * </ul>
 *
 * <p>Floors, measured 2026-10-24 at the card: {@value #EXPECTED_MIN_GOLDENS} carried goldens with a stack,
 * {@value #EXPECTED_MIN_STACKS} stacks with a composition, {@value #EXPECTED_MIN_ENTRIES} defaults in them, of which
 * {@value #EXPECTED_MIN_RULE_VERSIONS} ordinary rules and {@value #EXPECTED_MIN_NESTED} below a default;
 * {@value #EXPECTED_MIN_WITH_EXCEPTIONS} defaults with exception groups, {@value #EXPECTED_MIN_GROUP_DEFAULTS} of
 * them group-defaults; {@value #EXPECTED_MIN_UNORDERED_ROOTS} roots and {@value #EXPECTED_MIN_UNORDERED_BELOW}
 * exception slots side by side; {@value #EXPECTED_MIN_COMPOSED} stacks whose root names defaults of two levels of a
 * chain; and {@value #EXPECTED_MIN_COMPARED_STACKS} stacks, in {@value #EXPECTED_MIN_COMPARED_RECORDS} records,
 * compared with {@code decisionsOf}. A smaller count is a corpus that lost defaults goldens or a selection that
 * silently stopped checking.
 */
class StackCompositionCorpusWitnessTest {

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    /** The carried goldens with an effective stack, measured 2026-10-24. */
    static final int EXPECTED_MIN_GOLDENS = 43;
    /** Their effective stacks, each with a composition, measured 2026-10-24. */
    static final int EXPECTED_MIN_STACKS = 69;
    /** The defaults in those compositions, at any depth, measured 2026-10-24: 176 that decide themselves, 3
     *  group-defaults. */
    static final int EXPECTED_MIN_ENTRIES = 179;
    /** Of those, ordinary rules a subtype's stack took in, measured 2026-10-24: jutterswet-wrakbasis,
     *  jutterswet-diepwrakbasis, jutterswet-wrakinitialisatie and jutterswet-opgave-subtype. */
    static final int EXPECTED_MIN_RULE_VERSIONS = 4;
    /** Of those, the defaults below a default, measured 2026-10-24. */
    static final int EXPECTED_MIN_NESTED = 26;
    /** The defaults with exception groups, measured 2026-10-24. */
    static final int EXPECTED_MIN_WITH_EXCEPTIONS = 16;
    /** Of those, the group-defaults, measured 2026-10-24: one of them the group of jutterswet-meldtermijn-strandjutter
     *  for a Strandjutter. */
    static final int EXPECTED_MIN_GROUP_DEFAULTS = 3;
    /** The roots with chains side by side, measured 2026-10-24: jutterswet-wachtgeld, jutterswet-opgave-vormen, and
     *  jutterswet-wrakconflict on Strandrecht and on Wrakrecht. anchor/jutterswet-toelagegronden has two more, and the
     *  backend does not carry it (DROOLS-054). */
    static final int EXPECTED_MIN_UNORDERED_ROOTS = 4;
    /** The exception slots with groups side by side, measured 2026-10-24. */
    static final int EXPECTED_MIN_UNORDERED_BELOW = 8;
    /** The stacks with a chain at the root that names defaults of two levels of a type chain, measured 2026-10-24. */
    static final int EXPECTED_MIN_COMPOSED = 8;
    /** The stacks compared with {@code decisionsOf}, measured 2026-10-24: all of them. */
    static final int EXPECTED_MIN_COMPARED_STACKS = 69;
    /** The records compared, measured 2026-10-24. */
    static final int EXPECTED_MIN_COMPARED_RECORDS = 331;

    /** The pointer of a default at the root of a group, and of one at any depth below it. */
    private static final Pattern ROOT_DEFAULT = Pattern.compile("/rules/prioritizedDefaults/([0-9]+)/defaults/([0-9]+)");
    private static final Pattern GROUP = Pattern.compile("/rules/prioritizedDefaults/[0-9]+");
    private static final Pattern RULE_VERSION = Pattern.compile("/rules/rules/([0-9]+)/versions/[0-9]+");

    @Test void every_carried_stack_has_its_composition() throws Exception {
        List<Path> goldens;
        try (Stream<Path> s = Files.walk(GOLDEN)) {
            goldens = s.filter(p -> p.getFileName().toString().equals("ir.json")).map(Path::getParent).sorted().toList();
        }
        List<String> differences = new ArrayList<>();
        Map<String, Integer> counts = new TreeMap<>();
        Set<String> compared = new LinkedHashSet<>();
        for (Path g : goldens) {
            JsonNode ir = TmsDocuments.JSON.readTree(g.resolve("ir.json").toFile());
            DroolsBackend.Build build;
            try {
                build = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
            } catch (IrLoader.Refusal | IrLoader.DocumentError | CaseRunner.Excluded notCarried) {
                continue;
            }
            TmsTranspiler t = build.execModelTranspiler();
            if (t.stacks().isEmpty()) continue;
            counts.merge("goldens", 1, Integer::sum);
            String name = GOLDEN.relativize(g).toString();
            Map<String, List<RuleOrigin>> rulesOf = new HashMap<>();
            for (org.drools.model.Rule r : t.model().getRules()) {
                RuleOrigin o = t.originOf(r.getName());
                if (o.kind() == RuleOrigin.Kind.DEFAULT) rulesOf.computeIfAbsent(o.form(), k -> new ArrayList<>()).add(o);
            }
            for (Map.Entry<String, TmsTranspiler.Stack> s : t.stacks().entrySet()) {
                String stack = s.getKey(), where = name + " | " + stack;
                StackComposition c = s.getValue().composition();
                if (c == null) {
                    differences.add(where + ": een stapel van een gedragen golden zonder samenstelling");
                    continue;
                }
                counts.merge("stapels", 1, Integer::sum);
                check(where, ir, build.model(), t.generated().get(stack).source(), s.getValue().type().name(), c,
                        rulesOf.getOrDefault(stack, List.of()), differences, counts);
            }
            againstDecisions(name, g, ir, differences, counts, compared);
        }
        assertEquals(List.of(), differences, differences.size() + " verschillen in de samenstellingen");
        counts.put("vergeleken stapels", compared.size());
        System.out.println("StackCompositionCorpusWitnessTest: " + counts);
        floor(counts, "goldens", EXPECTED_MIN_GOLDENS);
        floor(counts, "stapels", EXPECTED_MIN_STACKS);
        floor(counts, "defaults", EXPECTED_MIN_ENTRIES);
        floor(counts, "regelversies", EXPECTED_MIN_RULE_VERSIONS);
        floor(counts, "onder een default", EXPECTED_MIN_NESTED);
        floor(counts, "met uitzonderingsgroepen", EXPECTED_MIN_WITH_EXCEPTIONS);
        floor(counts, "groep-defaults", EXPECTED_MIN_GROUP_DEFAULTS);
        floor(counts, "ongeordende wortels", EXPECTED_MIN_UNORDERED_ROOTS);
        floor(counts, "ongeordende uitzonderingsgroepen", EXPECTED_MIN_UNORDERED_BELOW);
        floor(counts, "over twee niveaus", EXPECTED_MIN_COMPOSED);
        floor(counts, "vergeleken stapels", EXPECTED_MIN_COMPARED_STACKS);
        floor(counts, "vergeleken records", EXPECTED_MIN_COMPARED_RECORDS);
    }

    private static void floor(Map<String, Integer> counts, String key, int floor) {
        int n = counts.getOrDefault(key, 0);
        assertTrue(n >= floor, () -> key + " " + n + ", onder de vloer " + floor + ": " + counts);
    }

    /** The checks of composition {@code c} of a stack on {@code member}, run for the type {@code type}, against the
     *  document and against the stack's rules of a default, {@code rules}. */
    private static void check(String where, JsonNode ir, RegelSpraakModel model, RegelSpraakModel.Designatable member,
                              String type, StackComposition c, List<RuleOrigin> rules, List<String> differences,
                              Map<String, Integer> counts) {
        List<StackComposition.Entry> all = new ArrayList<>();
        entries(c, all);
        Set<String> seen = new HashSet<>();
        for (StackComposition.Entry e : all) {
            counts.merge("defaults", 1, Integer::sum);
            if (!seen.add(e.pointer())) differences.add(where + ": de default " + e.pointer() + " staat er twee keer in");
            resolve(where, ir, e.pointer(), member, differences);
            exceptions(where, ir, e, differences, counts);
        }
        for (List<StackComposition.Entry> chain : c.chains())
            for (StackComposition.Entry e : chain)
                if (RULE_VERSION.matcher(e.pointer()).matches()) counts.merge("regelversies", 1, Integer::sum);
        for (StackComposition.Entry e : all)
            if (RULE_VERSION.matcher(e.pointer()).matches() && c.chains().stream().noneMatch(ch -> ch.contains(e)))
                differences.add(where + ": de regelversie " + e.pointer() + " staat onder een default");
        if (c.unordered()) {
            counts.merge("ongeordende wortels", 1, Integer::sum);
            for (List<StackComposition.Entry> chain : c.chains())
                for (StackComposition.Entry e : chain) {
                    Matcher m = GROUP.matcher(e.pointer());
                    if (!m.lookingAt() || !ir.at(m.group()).path("unordered").asBoolean(false))
                        differences.add(where + ": de wortel heeft " + c.chains().size() + " ketens naast elkaar, en "
                                + e.pointer() + " staat niet in een groep die het document ongeordend verklaart");
                }
        }
        for (List<StackComposition.Entry> chain : c.chains()) blockOrder(where, ir, model, type, chain, differences, counts);
        ownership(where, ir, all, rules, differences, counts);
    }

    /** Every entry of {@code c}, at any depth, in the order of the composition. */
    private static void entries(StackComposition c, List<StackComposition.Entry> out) {
        for (List<StackComposition.Entry> chain : c.chains())
            for (StackComposition.Entry e : chain) {
                out.add(e);
                if (e.exceptions() != null) entries(e.exceptions(), out);
            }
    }

    /** The exception groups of entry {@code e} are those of its default in the document, in the document's order. */
    private static void exceptions(String where, JsonNode ir, StackComposition.Entry e, List<String> differences,
                                   Map<String, Integer> counts) {
        JsonNode declared = RULE_VERSION.matcher(e.pointer()).matches() ? null : ir.at(e.pointer()).get("exceptions");
        if (declared == null || declared.isNull()) {
            if (e.exceptions() != null)
                differences.add(where + ": " + e.pointer() + " heeft in de samenstelling uitzonderingsgroepen, en in het"
                        + " document geen");
            return;
        }
        if (e.exceptions() == null) {
            differences.add(where + ": " + e.pointer() + " heeft in het document uitzonderingsgroepen, en in de"
                    + " samenstelling geen");
            return;
        }
        counts.merge("met uitzonderingsgroepen", 1, Integer::sum);
        JsonNode d = ir.at(e.pointer());
        if (!d.has("consequence") && !d.has("decision")) counts.merge("groep-defaults", 1, Integer::sum);
        JsonNode members = declared.path("members");
        List<List<String>> expected = new ArrayList<>();
        for (int m = 0; m < members.size(); m++) {
            List<String> chain = new ArrayList<>();
            for (int j = 0; j < members.get(m).path("defaults").size(); j++)
                chain.add(e.pointer() + "/exceptions/members/" + m + "/defaults/" + j);
            expected.add(chain);
        }
        List<List<String>> actual = pointers(e.exceptions());
        if (!expected.equals(actual))
            differences.add(where + ": de uitzonderingsgroepen van " + e.pointer() + " zijn " + actual + ", in het"
                    + " document " + expected);
        for (List<String> chain : actual) counts.merge("onder een default", chain.size(), Integer::sum);
        if (e.exceptions().unordered()) {
            counts.merge("ongeordende uitzonderingsgroepen", 1, Integer::sum);
            if (!declared.path("unordered").asBoolean(false))
                differences.add(where + ": de uitzonderingsgroepen van " + e.pointer() + " staan naast elkaar, en het"
                        + " document verklaart ze niet ongeordend");
        }
    }

    /** The pointers of the chains of {@code c}, chain by chain. */
    static List<List<String>> pointers(StackComposition c) {
        return c.chains().stream().map(chain -> chain.stream().map(StackComposition.Entry::pointer).toList()).toList();
    }

    /**
     * The block order of ch. 7.10 on one chain at the root, read off the document: the defaults of one group in the
     * group's order; the initialisations last; and before the lowest ordinary default, the base, the levels that
     * declare the defaults rising from the stack's type {@code type}: the type's own exceptions first, then the
     * inherited ones, the child's above the grandparent's. A chain with defaults of two levels is counted.
     */
    private static void blockOrder(String where, JsonNode ir, RegelSpraakModel model, String type,
                                   List<StackComposition.Entry> chain, List<String> differences,
                                   Map<String, Integer> counts) {
        ObjectType runs = model.objectTypeByName(type);
        List<Integer> levels = new ArrayList<>();
        List<Boolean> initialisations = new ArrayList<>();
        Map<Integer, Integer> lastOfGroup = new HashMap<>();
        for (StackComposition.Entry e : chain) {
            Matcher d = ROOT_DEFAULT.matcher(e.pointer()), r = RULE_VERSION.matcher(e.pointer());
            String declarant;
            boolean initialisation;
            if (d.matches()) {
                int group = Integer.parseInt(d.group(1)), place = Integer.parseInt(d.group(2));
                Integer before = lastOfGroup.put(group, place);
                if (before != null && before >= place)
                    differences.add(where + ": in een keten van de wortel staat " + e.pointer() + " na default " + before
                            + " van zijn groep");
                declarant = ir.at("/rules/prioritizedDefaults/" + group + "/subject").asText();
                initialisation = ir.at(e.pointer()).path("initialisation").asBoolean(false);
            } else if (r.matches()) {
                JsonNode version = ir.at(e.pointer());
                declarant = version.path("target").path("subjectType").asText(null);
                initialisation = version.path("initialisation").asBoolean(false);
            } else {
                differences.add(where + ": aan de wortel een pointer van onbekende vorm: " + e.pointer());
                return;
            }
            ObjectType level = model.objectTypeByName(declarant);
            int up = runs == null || level == null ? -1 : runs.chain().indexOf(level);
            if (up < 0) {
                differences.add(where + ": " + e.pointer() + " is gedeclareerd op " + declarant + ", geen type van de"
                        + " keten van " + type);
                return;
            }
            levels.add(up);
            initialisations.add(initialisation);
        }
        int firstInit = initialisations.indexOf(true);
        if (firstInit >= 0 && initialisations.subList(firstInit, initialisations.size()).contains(false))
            differences.add(where + ": een initialisatie boven een gewone default in " + pointersOf(chain));
        int ordinary = firstInit < 0 ? levels.size() : firstInit;
        for (int i = 1; i < ordinary - 1; i++)
            if (levels.get(i) < levels.get(i - 1))
                differences.add(where + ": de uitzonderingen staan niet van het eigen type omhoog: niveaus "
                        + levels + " in " + pointersOf(chain));
        if (new HashSet<>(levels).size() > 1) counts.merge("over twee niveaus", 1, Integer::sum);
    }

    private static List<String> pointersOf(List<StackComposition.Entry> chain) {
        return chain.stream().map(StackComposition.Entry::pointer).toList();
    }

    /**
     * Against {@code originOf}: every default of a rule of the stack stands in the composition, and every entry that
     * decides itself has exactly one rule of its own decision, {@code fallback} with exception groups and {@code own}
     * without; a group-default has none.
     */
    private static void ownership(String where, JsonNode ir, List<StackComposition.Entry> all, List<RuleOrigin> rules,
                                  List<String> differences, Map<String, Integer> counts) {
        Set<String> inComposition = new HashSet<>();
        for (StackComposition.Entry e : all) inComposition.add(e.pointer());
        Map<String, List<RuleOrigin.Role>> deciding = new HashMap<>();
        for (RuleOrigin o : rules) {
            if (!inComposition.contains(o.pointer()))
                differences.add(where + ": een regel van de default " + o.pointer() + " (" + o.role().key() + "), en die"
                        + " staat niet in de samenstelling");
            if (o.role() == RuleOrigin.Role.OWN || o.role() == RuleOrigin.Role.FALLBACK)
                deciding.computeIfAbsent(o.pointer(), k -> new ArrayList<>()).add(o.role());
        }
        for (StackComposition.Entry e : all) {
            JsonNode d = ir.at(e.pointer());
            boolean decidesItself = RULE_VERSION.matcher(e.pointer()).matches() || d.has("consequence")
                    || d.has("decision");
            List<RuleOrigin.Role> expected = !decidesItself ? List.of()
                    : List.of(e.exceptions() != null ? RuleOrigin.Role.FALLBACK : RuleOrigin.Role.OWN);
            List<RuleOrigin.Role> actual = deciding.getOrDefault(e.pointer(), List.of());
            if (!expected.equals(actual))
                differences.add(where + ": " + e.pointer() + " heeft de regels van eigen beslissing " + actual
                        + ", verwacht " + expected);
            if (decidesItself) counts.merge("met een eigen beslissing", 1, Integer::sum);
        }
    }

    /** The pointer {@code p} names a node of {@code ir} that writes {@code member}: a {@code Default} of a group on it,
     *  or the version of a rule whose target it is. */
    private static void resolve(String where, JsonNode ir, String p, RegelSpraakModel.Designatable member,
                                List<String> differences) {
        boolean characteristic = member.kind() == RegelSpraakModel.Designatable.Kind.KENMERK;
        JsonNode node = ir.at(p);
        Matcher group = GROUP.matcher(p);
        String target;
        if (group.lookingAt()) {
            if (!node.path("node").asText().equals("Default")) {
                differences.add(where + ": pointer " + p + " noemt geen Default maar " + node);
                return;
            }
            target = ir.at(group.group()).path(characteristic ? "characteristic" : "attribute").asText(null);
        } else if (RULE_VERSION.matcher(p).matches()) {
            target = node.path("target").path(characteristic ? "characteristic" : "attribute").asText(null);
        } else {
            differences.add(where + ": een pointer van onbekende vorm: " + p);
            return;
        }
        if (target == null || !Namen.norm(target).equals(Namen.norm(member.name())))
            differences.add(where + ": pointer " + p + " noemt een knooppunt met doel " + target + ", niet "
                    + member.name());
    }

    /** Against {@code decisionsOf}: on every case of golden {@code g} that the exec-model branch plays, the root
     *  chains of every record are the chains at the root of its stack's composition. */
    private static void againstDecisions(String name, Path g, JsonNode ir, List<String> differences,
                                         Map<String, Integer> counts, Set<String> compared) throws Exception {
        if (ir.path("rules").path("prioritizedDefaults").isEmpty()) return;
        List<Path> casePaths = new ArrayList<>();
        try (Stream<Path> s = Files.list(g)) {
            for (Path casePath : s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList())
                if (!TmsDocuments.JSON.readTree(casePath.toFile()).has("expectedFault")) casePaths.add(casePath);
        }
        if (casePaths.isEmpty()) return;
        if (!CaseRunner.viaExecModel(g.resolve("ir.json"), casePaths).status().equals("PLAYED")) return;
        CaseRunner runner = CaseRunner.forMeasurement(g.resolve("ir.json"), DroolsBackend.Branch.EXEC_MODEL);
        for (Path casePath : casePaths) {
            String which = name + "/" + casePath.getFileName();
            runner.withSession(TmsDocuments.JSON.readTree(casePath.toFile()), (build, session, labels) -> {
                DroolsBackend.fire(build, session);
                TmsTranspiler t = build.execModelTranspiler();
                for (DefaultsDecision d : DroolsBackend.decisionsOf(build, session)) {
                    String type = typeOf(build.model(), d.object()).name;
                    List<String> stacks = new ArrayList<>();
                    t.generated().forEach((stack, from) -> {
                        TmsTranspiler.Stack runs = t.stacks().get(stack);
                        if (runs != null && runs.type().name().equals(type)
                                && (from.source().kind() == RegelSpraakModel.Designatable.Kind.KENMERK) == d.characteristic()
                                && Namen.norm(from.source().name()).equals(Namen.norm(d.member())))
                            stacks.add(stack);
                    });
                    if (stacks.size() != 1) {
                        differences.add(which + ": een record op " + type + "." + d.member() + " en de stapels " + stacks);
                        continue;
                    }
                    List<List<String>> root = pointers(t.stacks().get(stacks.get(0)).composition());
                    if (!root.equals(d.rootChains()))
                        differences.add(which + " | " + stacks.get(0) + ": de wortel van de samenstelling " + root
                                + ", en decisionsOf geeft de wortelketens " + d.rootChains());
                    counts.merge("vergeleken records", 1, Integer::sum);
                    compared.add(name + " | " + stacks.get(0));
                }
                return null;
            });
        }
    }

    /** The object type whose bean class {@code o} is. */
    private static ObjectType typeOf(RegelSpraakModel model, Object o) {
        for (ObjectType t : model.objectTypes.values())
            if (o.getClass().getName().equals(FactModelGenerator.PACKAGE + "." + Namen.ident(t.name, true))) return t;
        throw new AssertionError("geen objecttype voor " + o.getClass().getName());
    }
}
