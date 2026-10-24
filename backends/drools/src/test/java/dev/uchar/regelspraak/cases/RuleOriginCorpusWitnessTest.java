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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
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
 * <p>Since card drl-rule-part, per rule beside that:
 *
 * <ul>
 *   <li>every rule of a form ({@code FORM}, {@code DEFAULT}, {@code STACK}) has a role, and it is the role the label in
 *       its name spells ({@link #roleByName}): the witness reads the name as an oracle of its own, where the lowering
 *       notes the role while it builds the rule and never reads a name; a reading rule has the form, kind and pointer
 *       of the rule it reads and the role {@code READING};</li>
 *   <li>every rule of a fold names its fold, and the pointer resolves in the golden's document to an
 *       {@code Aggregation} or a {@code QuantifierFold} below the default it lowers, or below the form it is of;</li>
 *   <li>two folds of one form and one subject never name one node ({@link #foldsOfOneSubject}): the lowering builds a
 *       fold once per node and subject and numbers each fold it builds, so two numbers on one node and one subject
 *       are rules that name a node not their own, such as that of the fold in whose filter their fold stands. The
 *       number is the one in the rule's name ({@code vouw 3}) and the subject the class the DRL text binds the fold's
 *       subject to ({@code $vouwonderwerp}), two more oracles of this witness;</li>
 * </ul>
 *
 * and every effective stack of {@code generated} has a type in {@code stacks}: an object type of the model that
 * declares the member {@code generated} gives or inherits it, the type the stack's name begins with
 * ({@code defaults A:<type>.}), a second oracle that reads the name.
 *
 * <p>Floors, measured 2026-10-23 at the card: {@value #EXPECTED_MIN_GOLDENS} goldens carried,
 * {@value #EXPECTED_MIN_RULES} rules in the builds without readings, of them {@value #EXPECTED_MIN_DEFAULT_RULES} of a
 * default, {@value #EXPECTED_MIN_RULE_VERSION_RULES} of those of an ordinary rule a stack took in,
 * {@value #EXPECTED_MIN_DEFAULT_CONTRIBUTIONS} contribution rules of a fold of a default, and
 * {@value #EXPECTED_MIN_STACK_RULES} of a stack itself; {@value #EXPECTED_MIN_READING_RULES} reading rules in the builds
 * with readings; and {@value #EXPECTED_MIN_DEFAULTS} defaults of the documents, each with a rule. A smaller count is a
 * corpus that lost goldens or defaults, or a selection that silently stopped checking. Floors of card drl-rule-part,
 * measured 2026-10-24: {@link #EXPECTED_MIN_ROLES} per kind and role in the builds without readings, and the reading
 * rules per kind in the builds with them; {@value #EXPECTED_MIN_FOLD_RULES} rules of a fold, each with its fold;
 * {@value #EXPECTED_MIN_STACK_TYPES} stacks with a type, {@value #EXPECTED_MIN_SUBTYPE_STACKS} of them on a type
 * that inherits the member; and, measured 2026-10-24 at the review of that card,
 * {@value #EXPECTED_MIN_NESTED_FOLDS} folds below another fold of their form and subject.
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
    /** The rules per kind and role (its key) in the builds without readings, and the reading rules per kind in the
     *  builds with them, measured 2026-10-24 on 282 carried goldens. No golden has the fault rule of a justification
     *  or a fold of a default whose filter or member can fault: those shapes are {@code RuleOriginWitnessTest}'s. */
    static final Map<String, Integer> EXPECTED_MIN_ROLES = Map.ofEntries(
            Map.entry("FORM own", 796), Map.entry("FORM condition-fault", 48), Map.entry("FORM condition-deposit", 13),
            Map.entry("FORM no-evaluation-date", 7), Map.entry("FORM fold-contribution", 251),
            Map.entry("FORM fold-filter-fault", 17), Map.entry("FORM fold-member-fault", 3),
            Map.entry("FORM fold-filter-deposit", 8), Map.entry("FORM uniqueness-key", 19),
            Map.entry("FORM uniqueness-key-form-fault", 6), Map.entry("FORM uniqueness-channel", 13),
            Map.entry("FORM allocation-recipient", 17), Map.entry("DEFAULT own", 163),
            Map.entry("DEFAULT fallback", 13), Map.entry("DEFAULT reconcile-pass-on", 1),
            Map.entry("DEFAULT reconcile-difference", 8), Map.entry("DEFAULT reconcile-agreement", 16),
            Map.entry("DEFAULT fold-contribution", 12), Map.entry("STACK no-evaluation-date", 1),
            Map.entry("STACK reconcile-pass-on", 6), Map.entry("STACK reconcile-difference", 4),
            Map.entry("STACK reconcile-agreement", 15),
            Map.entry("FORM reading met lezingen", 746), Map.entry("DEFAULT reading met lezingen", 176));
    /** The rules of a fold in the builds without readings, each with its fold, measured 2026-10-24: 452 of an
     *  aggregation and 130 of a quantifier fold over both builds. */
    static final int EXPECTED_MIN_FOLD_RULES = 291;
    /** The effective stacks of {@code generated}, each with a type, measured 2026-10-24. */
    static final int EXPECTED_MIN_STACK_TYPES = 69;
    /** Of those, the stacks on a type that inherits the member it decides (ch. 7.10), measured 2026-10-24: in eleven
     *  goldens, among them jutterswet-meldtermijn-strandjutter and jutterswet-opgave-subtype. */
    static final int EXPECTED_MIN_SUBTYPE_STACKS = 15;
    /** The folds whose node stands below the node of another fold of their form and subject, in the builds without
     *  readings, measured 2026-10-24: three in anchor-forms, one each in fold-filter-subject-two-spellings and
     *  jutterswet-schone-maand-na-zware-klacht. Two more nested folds, in fold-filter-inner-fold-via-element-role and
     *  fold-filter-subject-two-spellings, have another subject than the fold around them. */
    static final int EXPECTED_MIN_NESTED_FOLDS = 5;

    private static final Pattern GROUP = Pattern.compile("/rules/prioritizedDefaults/[0-9]+");
    private static final Pattern RULE_VERSION = Pattern.compile("/rules/rules/[0-9]+/versions/[0-9]+");
    /** The declaration of a form in the document: its section and its place there. */
    private static final Pattern FORM_PLACE = Pattern.compile("(/rules/[A-Za-z]+/[0-9]+)/");
    private static final Set<String> FOLD_NODES = Set.of("Aggregation", "QuantifierFold");
    private static final String READING = ": lezing";
    /** The number of the fold in the name of a rule of a fold. */
    private static final Pattern FOLD_NUMBER = Pattern.compile(" (?:in|aan) vouw ([0-9]+)");
    /** The binding of the fold's subject in the DRL text of a rule of a fold. */
    private static final Pattern FOLD_SUBJECT = Pattern.compile("\\$vouwonderwerp : ([^\\s(]+)\\(");

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
            stackTypes(name, build, differences, plain);
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
        for (Map.Entry<String, Integer> e : EXPECTED_MIN_ROLES.entrySet())
            if (e.getKey().endsWith(" met lezingen"))
                floor(withReadings, e.getKey().substring(0, e.getKey().length() - " met lezingen".length()), e.getValue());
            else floor(plain, e.getKey(), e.getValue());
        floor(plain, "regels van een vouw", EXPECTED_MIN_FOLD_RULES);
        floor(plain, "stapels met een type", EXPECTED_MIN_STACK_TYPES);
        floor(plain, "stapels op een type dat het lid erft", EXPECTED_MIN_SUBTYPE_STACKS);
        floor(plain, "geneste vouwen van één onderwerp", EXPECTED_MIN_NESTED_FOLDS);
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
        // per form and subject of a fold, the fold numbers that name each node
        Map<String, Map<String, Set<String>>> folds = new TreeMap<>();
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
                // a reading rule: no form for the observer, and the origin of the rule it reads, in its own role
                String own = rule.endsWith(READING) ? rule.substring(0, rule.length() - READING.length()) : null;
                RuleOrigin read = own == null ? null : t.originOf(own);
                if (!build.readings() || read == null || !o.equals(new RuleOrigin(read.form(), read.kind(),
                        RuleOrigin.Role.READING, read.pointer(), null)))
                    differences.add(where + ": formOf geeft geen vorm en de herkomst is " + o + ", en dat is geen"
                            + " leesregel met de herkomst van de regel die hij leest");
                counts.merge("leesregels", 1, Integer::sum);
            } else if (!Objects.equals(form, o.form()))
                differences.add(where + ": formOf geeft " + form + " en de herkomst " + o);
            if (o.kind() != RuleOrigin.Kind.NO_FORM) {
                counts.merge(o.kind() + " " + o.role().key(), 1, Integer::sum);
                RuleOrigin.Role named = roleByName(rule, o);
                if (o.role() != named)
                    differences.add(where + ": de rol is " + o.role() + ", het label in de naam zegt " + named);
                if (o.fold() != null) {
                    counts.merge("regels van een vouw", 1, Integer::sum);
                    fold(where, ir, o, differences);
                    foldNumber(where, build.drl(), rule, o, folds, differences);
                }
            }
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
        foldsOfOneSubject(which, folds, differences, counts);
    }

    /** The role the label in the name of {@code rule}, of origin {@code o}, spells: the oracle of this witness, which
     *  the lowering does not consult. The label of a rule of a stack follows the stack's name; any other rule of a form
     *  whose name carries no label of a helper rule is the form's own. */
    static RuleOrigin.Role roleByName(String rule, RuleOrigin o) {
        if (rule.endsWith(READING)) return RuleOrigin.Role.READING;
        if (rule.endsWith(": fout in de voorwaarde")) return RuleOrigin.Role.CONDITION_FAULT;
        if (rule.endsWith(": deponering in de voorwaarde")) return RuleOrigin.Role.CONDITION_DEPOSIT;
        if (rule.endsWith(": geen rekendatum")) return RuleOrigin.Role.NO_EVALUATION_DATE;
        if (rule.endsWith(": kanaal")) return RuleOrigin.Role.UNIQUENESS_CHANNEL;
        if (rule.endsWith(": ontvanger")) return RuleOrigin.Role.ALLOCATION_RECIPIENT;
        if (rule.contains(": filterfout in vouw ")) return RuleOrigin.Role.FOLD_FILTER_FAULT;
        if (rule.contains(": lidfout in vouw ")) return RuleOrigin.Role.FOLD_MEMBER_FAULT;
        if (rule.contains(": filterdeponering in vouw ")) return RuleOrigin.Role.FOLD_FILTER_DEPOSIT;
        if (rule.contains(": bijdrage aan vouw ")) return RuleOrigin.Role.FOLD_CONTRIBUTION;
        if (rule.contains(": sleutelvorm van ")) return RuleOrigin.Role.UNIQUENESS_KEY_FORM_FAULT;
        if (rule.contains(": sleutel van ")) return RuleOrigin.Role.UNIQUENESS_KEY;
        String stack = o.form() + ": ";
        if (o.kind() != RuleOrigin.Kind.FORM && rule.startsWith(stack)) {
            String label = rule.substring(stack.length());
            if (label.startsWith("doorgeven ")) return RuleOrigin.Role.RECONCILE_PASS_ON;
            if (label.startsWith("verschil ")) return RuleOrigin.Role.RECONCILE_DIFFERENCE;
            if (label.startsWith("eens ")) return RuleOrigin.Role.RECONCILE_AGREEMENT;
            if (label.startsWith("terugval ")) return RuleOrigin.Role.FALLBACK;
            if (label.startsWith("default ")) return RuleOrigin.Role.OWN;
            return null;
        }
        return RuleOrigin.Role.OWN;
    }

    /** Notes, for {@code rule}, a rule of a fold of origin {@code o}, the number of its fold under its form and the
     *  subject of the fold, as the name and the DRL text {@code drl} spell them. */
    private static void foldNumber(String where, String drl, String rule, RuleOrigin o,
                                   Map<String, Map<String, Set<String>>> folds, List<String> differences) {
        Matcher number = FOLD_NUMBER.matcher(rule);
        String header = "rule " + Namen.drlString(rule) + "\n";
        int at = drl.indexOf(header), end = at < 0 ? -1 : drl.indexOf("\nend\n", at);
        Matcher subject = end < 0 ? null : FOLD_SUBJECT.matcher(drl.substring(at, end));
        if (!number.find() || subject == null || !subject.find()) {
            differences.add(where + ": een regel van een vouw zonder het nummer van zijn vouw in zijn naam of zonder het"
                    + " onderwerp van de vouw in de DRL-tekst");
            return;
        }
        folds.computeIfAbsent(o.form() + " op " + subject.group(1), k -> new TreeMap<>())
                .computeIfAbsent(o.fold(), k -> new TreeSet<>()).add(number.group(1));
    }

    /** Two folds of one form and one subject name two nodes: the lowering builds one fold per node and subject. A fold
     *  whose node stands below another one's is counted. */
    private static void foldsOfOneSubject(String which, Map<String, Map<String, Set<String>>> folds,
                                          List<String> differences, Map<String, Integer> counts) {
        folds.forEach((formAndSubject, nodes) -> nodes.forEach((node, numbers) -> {
            if (numbers.size() > 1)
                differences.add(which + " | " + formAndSubject + ": de vouwen " + numbers + " noemen allebei " + node);
            if (nodes.keySet().stream().anyMatch(outer -> node.startsWith(outer + "/")))
                counts.merge("geneste vouwen van één onderwerp", 1, Integer::sum);
        }));
    }

    /** The fold of {@code o} names an aggregation or a quantifier fold of {@code ir}, below the default it lowers or
     *  below the declaration of its form. */
    private static void fold(String where, JsonNode ir, RuleOrigin o, List<String> differences) {
        JsonNode node = ir.at(o.fold());
        if (!FOLD_NODES.contains(node.path("node").asText()))
            differences.add(where + ": de vouw " + o.fold() + " noemt geen vouw maar " + (node.isMissingNode() ? "niets"
                    : node.path("node").asText()));
        if (o.kind() == RuleOrigin.Kind.DEFAULT) {
            if (!o.fold().startsWith(o.pointer() + "/"))
                differences.add(where + ": de vouw " + o.fold() + " staat niet onder de default " + o.pointer());
            return;
        }
        Matcher m = FORM_PLACE.matcher(o.fold());
        String name = m.lookingAt() ? ir.at(m.group(1)).path("name").asText(null) : null;
        if (!RegelSpraakModel.formName(o.form()).equals(name))
            differences.add(where + ": de vouw " + o.fold() + " staat niet onder de vorm " + o.form());
    }

    /** Every effective stack of {@code generated} has a type: an object type of the model whose chain declares the
     *  member the stack decides, and the type its name begins with; and {@code stacks} names no other stack. */
    private static void stackTypes(String which, DroolsBackend.Build build, List<String> differences,
                                   Map<String, Integer> counts) {
        TmsTranspiler t = build.execModelTranspiler();
        RegelSpraakModel model = build.model();
        Set<String> stacks = new LinkedHashSet<>();
        t.generated().forEach((n, g) -> { if (g.form() == RegelSpraakModel.OriginForm.DEFAULTS_STACK) stacks.add(n); });
        if (!stacks.equals(t.stacks().keySet()))
            differences.add(which + ": de stapels " + stacks + " en de stapels met een type " + t.stacks().keySet());
        for (String stack : stacks) {
            TmsTranspiler.Stack s = t.stacks().get(stack);
            if (s == null) continue;
            RegelSpraakModel.Designatable type = s.type(), member = t.generated().get(stack).source();
            String where = which + " | " + stack;
            RegelSpraakModel.ObjectType runs = type.kind() == RegelSpraakModel.Designatable.Kind.OBJECTTYPE
                    ? model.objectTypeByName(type.name()) : null;
            RegelSpraakModel.ObjectType declares = model.objectTypeByName(member.owner());
            if (runs == null || declares == null || !runs.chain().contains(declares))
                differences.add(where + ": het type " + type + " is geen objecttype dat " + member + " declareert of"
                        + " erft");
            String prefix = (member.kind() == RegelSpraakModel.Designatable.Kind.KENMERK ? "defaults K:" : "defaults A:")
                    + type.name() + ".";
            if (!stack.startsWith(prefix))
                differences.add(where + ": het type " + type + ", en de naam begint niet met '" + prefix + "'");
            counts.merge("stapels met een type", 1, Integer::sum);
            if (runs != declares) counts.merge("stapels op een type dat het lid erft", 1, Integer::sum);
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
