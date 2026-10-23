package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.transpiler.core.Namen;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler.RuleOrigin;

import org.junit.jupiter.api.Test;
import org.kie.api.definition.KiePackage;
import org.kie.api.definition.rule.Rule;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code TmsTranspiler.originOf} over the corpus (card drl-rule-origin, acceptance (a)): every golden the backend
 * carries, built on the DRL-text branch, the branch whose text the toolchain shows, once without readings, the build
 * an answer is given from, and once with them, whose reading rules are helper rules of a form too. Per rule of the
 * KieBase, by the name the DRL text gives it:
 *
 * <ul>
 *   <li>{@code originOf} answers, and its form is the one {@code formOf} gives; a reading rule, which {@code formOf}
 *       puts in no form, has the origin of the rule it reads, and no other rule differs from {@code formOf};</li>
 *   <li>a rule of no form is {@code NO_FORM}, a rule of a form that is no effective defaults stack is {@code FORM}, and a
 *       rule of a stack ({@code generated} says which forms are stacks) is {@code DEFAULT} or {@code STACK}, never
 *       {@code FORM}: a rule of a stack names a default of it or says that it lowers the stack itself;</li>
 *   <li>the pointer of a {@code DEFAULT} resolves in the golden's document to a node that writes the stack's member: a
 *       {@code Default} of a group on that member, or the version of an ordinary rule on it (ch. 7.10).</li>
 * </ul>
 *
 * And every {@code Default} of every group of a carried golden, at any depth of nesting, is the default of at least
 * one rule: the toolchain finds the DRL of each variant through these pointers.
 *
 * <p>Floors, measured 2026-10-23 at the card: {@value #EXPECTED_MIN_GOLDENS} goldens carried,
 * {@value #EXPECTED_MIN_RULES} rules in the builds without readings, of them {@value #EXPECTED_MIN_DEFAULT_RULES} of a
 * default, {@value #EXPECTED_MIN_RULE_VERSION_RULES} of those of an ordinary rule a stack took in,
 * {@value #EXPECTED_MIN_DEFAULT_CONTRIBUTIONS} contribution rules of a fold of a default, and
 * {@value #EXPECTED_MIN_STACK_RULES} of a stack itself; {@value #EXPECTED_MIN_READING_RULES} reading rules in the builds
 * with readings; and {@value #EXPECTED_MIN_DEFAULTS} defaults of the documents, each with a rule. A smaller count is a
 * corpus that lost goldens or defaults, or a selection that silently stopped checking.
 */
class RuleOriginCorpusWitnessTest {

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    /** The goldens the backend carries, measured 2026-10-23: 277 of the 323. */
    static final int EXPECTED_MIN_GOLDENS = 277;
    /** The rules of their builds without readings, measured 2026-10-23: 1142 of a form, 681 of no form. */
    static final int EXPECTED_MIN_RULES = 2062;
    /** The rules of a default among them, measured 2026-10-23. */
    static final int EXPECTED_MIN_DEFAULT_RULES = 213;
    /** Of those, the rules of an ordinary rule a subtype's stack took in, measured 2026-10-23: four goldens. */
    static final int EXPECTED_MIN_RULE_VERSION_RULES = 4;
    /** Of those, the contribution rules of a fold a default reads, measured 2026-10-23. */
    static final int EXPECTED_MIN_DEFAULT_CONTRIBUTIONS = 12;
    /** The rules of a stack itself, measured 2026-10-23: 25 of a reconciliation at the root, one fault without an
     *  evaluation date. */
    static final int EXPECTED_MIN_STACK_RULES = 26;
    /** The reading rules of the builds with readings, measured 2026-10-23: 176 of a default, 725 of a form. */
    static final int EXPECTED_MIN_READING_RULES = 901;
    /** The defaults of the groups of the carried goldens, at any depth, measured 2026-10-23. */
    static final int EXPECTED_MIN_DEFAULTS = 160;

    private static final Pattern GROUP = Pattern.compile("/rules/prioritizedDefaults/[0-9]+");
    private static final Pattern RULE_VERSION = Pattern.compile("/rules/rules/[0-9]+/versions/[0-9]+");
    private static final String READING = ": lezing";

    @Test void every_rule_of_every_carried_golden_has_its_origin() throws Exception {
        List<Path> goldens;
        try (Stream<Path> s = Files.walk(GOLDEN)) {
            goldens = s.filter(p -> p.getFileName().toString().equals("ir.json")).map(Path::getParent).sorted().toList();
        }
        List<String> differences = new ArrayList<>();
        Map<String, Integer> plain = new TreeMap<>(), withReadings = new TreeMap<>();
        int carried = 0, defaults = 0;
        for (Path g : goldens) {
            JsonNode ir = TmsDocuments.JSON.readTree(g.resolve("ir.json").toFile());
            DroolsBackend.Build build;
            try {
                build = DroolsBackend.build(ir, DroolsBackend.Branch.DRL_TEXT);
            } catch (IrLoader.Refusal | IrLoader.DocumentError | CaseRunner.Excluded notCarried) {
                continue;
            }
            carried++;
            String name = GOLDEN.relativize(g).toString();
            Set<String> pointers = new HashSet<>();
            check(name, ir, build, pointers, differences, plain);
            check(name + " met lezingen", ir, DroolsBackend.build(ir, DroolsBackend.Branch.DRL_TEXT, true), pointers,
                    differences, withReadings);
            JsonNode groups = ir.path("rules").path("prioritizedDefaults");
            for (int i = 0; i < groups.size(); i++)
                for (String p : defaultPointers(groups.get(i).path("defaults"), "/rules/prioritizedDefaults/" + i)) {
                    defaults++;
                    if (!pointers.contains(p)) differences.add(name + ": de default " + p + " is van geen regel");
                }
        }
        assertEquals(List.of(), differences, "regels zonder juiste herkomst");
        int n = carried, d = defaults;
        System.out.println("RuleOriginCorpusWitnessTest: " + n + " goldens, " + d + " defaults, zonder lezingen "
                + plain + ", met lezingen " + withReadings);
        assertTrue(n >= EXPECTED_MIN_GOLDENS, () -> "gedragen goldens " + n + ", onder de vloer " + EXPECTED_MIN_GOLDENS);
        assertTrue(d >= EXPECTED_MIN_DEFAULTS, () -> "defaults van de documenten " + d + ", onder de vloer "
                + EXPECTED_MIN_DEFAULTS);
        floor(plain, "regels", EXPECTED_MIN_RULES);
        floor(plain, "DEFAULT", EXPECTED_MIN_DEFAULT_RULES);
        floor(plain, "DEFAULT van een regelversie", EXPECTED_MIN_RULE_VERSION_RULES);
        floor(plain, "DEFAULT bijdrage aan een vouw", EXPECTED_MIN_DEFAULT_CONTRIBUTIONS);
        floor(plain, "STACK", EXPECTED_MIN_STACK_RULES);
        floor(withReadings, "leesregels", EXPECTED_MIN_READING_RULES);
        assertEquals(0, plain.getOrDefault("leesregels", 0), () -> "leesregels in een bouw zonder lezingen: " + plain);
    }

    private static void floor(Map<String, Integer> counts, String key, int floor) {
        int n = counts.getOrDefault(key, 0);
        assertTrue(n >= floor, () -> key + " " + n + ", onder de vloer " + floor + ": " + counts);
    }

    /** The checks of every rule of {@code build}; the pointers of the defaults go into {@code pointers}. */
    private static void check(String which, JsonNode ir, DroolsBackend.Build build, Set<String> pointers,
                              List<String> differences, Map<String, Integer> counts) {
        TmsTranspiler t = build.execModelTranspiler();
        KiePackage p = build.kieBase().getKiePackage(TmsTranspiler.PKG);
        List<Rule> rules = p == null ? List.of() : List.copyOf(p.getRules());
        for (Rule r : rules) {
            String rule = r.getName(), where = which + " | " + rule;
            counts.merge("regels", 1, Integer::sum);
            RuleOrigin o;
            try {
                o = t.originOf(rule);
            } catch (RuntimeException e) {
                differences.add(where + ": " + e);
                continue;
            }
            String form = t.formOf(rule);
            if (form == null && o.kind() != RuleOrigin.Kind.NO_FORM) {
                // a reading rule: no form for the observer, and the origin of the rule it reads
                String own = rule.endsWith(READING) ? rule.substring(0, rule.length() - READING.length()) : null;
                if (!build.readings() || own == null || !o.equals(t.originOf(own)))
                    differences.add(where + ": formOf geeft geen vorm en de herkomst is " + o + ", en dat is geen"
                            + " leesregel met de herkomst van de regel die hij leest");
                counts.merge("leesregels", 1, Integer::sum);
            } else if (!Objects.equals(form, o.form()))
                differences.add(where + ": formOf geeft " + form + " en de herkomst " + o);
            RegelSpraakModel.GeneratedFrom generated = o.form() == null ? null : t.generated().get(o.form());
            boolean stack = generated != null && generated.form() == RegelSpraakModel.OriginForm.DEFAULTS_STACK;
            counts.merge(o.kind().name(), 1, Integer::sum);
            switch (o.kind()) {
                case NO_FORM -> { }
                case FORM -> {
                    if (stack) differences.add(where + ": een regel van de stapel " + o.form() + " zonder default en"
                            + " niet van de stapel zelf: " + o);
                }
                case STACK -> {
                    if (!stack) differences.add(where + ": van de stapel zelf, en " + o.form() + " is geen stapel");
                }
                case DEFAULT -> {
                    pointers.add(o.pointer());
                    if (RULE_VERSION.matcher(o.pointer()).matches())
                        counts.merge("DEFAULT van een regelversie", 1, Integer::sum);
                    if (rule.contains(": bijdrage aan "))
                        counts.merge("DEFAULT bijdrage aan een vouw", 1, Integer::sum);
                    if (stack) resolve(where, ir, o.pointer(), generated.source(), differences);
                    else differences.add(where + ": een default, en " + o.form() + " is geen stapel");
                }
            }
        }
    }

    /** The pointer {@code p} names a node of {@code ir} that writes {@code member}, the member of the stack: a
     *  {@code Default} of a group on it, or the version of a rule whose target it is. */
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

    /** The pointers of the defaults of {@code chain}, held by {@code holder}, and of every default of their exception
     *  groups, at any depth. */
    private static List<String> defaultPointers(JsonNode chain, String holder) {
        List<String> out = new ArrayList<>();
        for (int j = 0; j < chain.size(); j++) {
            String p = holder + "/defaults/" + j;
            out.add(p);
            JsonNode members = chain.get(j).path("exceptions").path("members");
            for (int m = 0; m < members.size(); m++)
                out.addAll(defaultPointers(members.get(m).path("defaults"), p + "/exceptions/members/" + m));
        }
        return out;
    }
}
