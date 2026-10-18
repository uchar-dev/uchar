package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.CaseRunner;
import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.transpiler.tms.plan.Accumulation;
import dev.uchar.regelspraak.transpiler.tms.plan.Exists;
import dev.uchar.regelspraak.transpiler.tms.plan.Binding;
import dev.uchar.regelspraak.transpiler.tms.plan.Consequence;
import dev.uchar.regelspraak.transpiler.tms.plan.ReadKind;
import dev.uchar.regelspraak.transpiler.tms.plan.ReadTable;
import dev.uchar.regelspraak.transpiler.tms.plan.Read;
import dev.uchar.regelspraak.transpiler.tms.plan.LhsItem;
import dev.uchar.regelspraak.transpiler.tms.plan.Not;
import dev.uchar.regelspraak.transpiler.tms.plan.Subject;
import dev.uchar.regelspraak.transpiler.tms.plan.FactPattern;
import dev.uchar.regelspraak.transpiler.tms.plan.RulePlan;
import dev.uchar.regelspraak.transpiler.tms.plan.TestItem;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The analysis names the variable of a slot after what the slot reads (card drl-teksttak-op-truth-maintenance, step 1e;
 * FABLE-2 §1.2 point 1), so that both renderings, the executable model and DRL text, show one name and neither makes it
 * up: a member of the subject by its name ({@code $aantalAangemeldeVondsten}), a member along a path by the roles and
 * the member ({@code $postKustvakGevarenklasse}), a parameter by its name, a reference field by its name and
 * {@code Gezet}, the parent in the scope of a uniqueness by its role, the channel of a form by the form and
 * {@code Inconsistent}. Three checks:
 * <ul>
 *   <li>over the plans of every golden the branch builds: no slot is called {@code $lezing_} and a number any more,
 *       every slot's variable is {@code $} and a Java identifier, and every variable a rule declares, at its top level
 *       and inside a {@code not}, an {@code exists} or the source of an accumulate, is declared once, so a read name is
 *       unique against the pattern variables such as {@code $onderwerp} as well;</li>
 *   <li>the names of six rules of the corpus, one per kind of read that the corpus names this way, among them a member
 *       called "waarde", whose name {@code $waarde} is a fixed name of the analysis and so takes a number;</li>
 *   <li>the fixed names, those without an underscore that {@code TmsTranspiler} gives a variable itself, are exactly
 *       {@link TmsTranspiler#FIXED_NAMES}: a read name has no underscore and is none of them, so it does not meet
 *       another variable of its rule. That is how the analysis avoids a second declaration; that none reaches a
 *       rendering, also in a document outside the corpus, is the plan's own check ({@link RulePlan}).</li>
 * </ul>
 * Fix round 1e adds two checks outside the corpus:
 * <ul>
 *   <li>a read name is {@code $} and a Java identifier for any declared name: of what {@code Namen.ident} keeps, only
 *       the characters a Java identifier may hold stay, so a number such as {@code ²} or {@code ½} goes, and a name of
 *       which nothing stays reads {@code $x}, with a number where that is taken;</li>
 *   <li>the plan refuses a rule that declares a variable a second time in its scope, or gives two slots one
 *       variable.</li>
 * </ul>
 *
 * <p><b>Red</b> before the step, on {@code 123e39e5}: the analysis called a read {@code $lezing_} with its slot, 1112
 * slots over the corpus, and {@code FIXED_NAMES} did not exist. <b>Red</b> before fix round 1e, on {@code 32b44001}
 * with only the read name drawn out into a method of its own: {@code "artikel 3² inconsistent"} read
 * {@code $artikel3²Inconsistent}, which no DRL function can declare, and the plan took two declarations of one name.
 */
class ReadNameWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final Path TRANSPILER = Path.of("src", "main", "java", "dev", "uchar", "regelspraak", "transpiler",
            "tms", "TmsTranspiler.java");
    private static final Pattern NUMBERED = Pattern.compile("\\$lezing_\\d+");

    /** Floors, measured 2026-10-13 at step 1e, so that a check over nothing does not pass: the goldens the branch
     *  builds, the plans they hold, and the slots with a variable. A deliberate removal lowers a floor in the same
     *  commit. */
    private static final int EXPECTED_MIN_BUILT = 253;
    private static final int EXPECTED_MIN_PLANS = 1859;
    private static final int EXPECTED_MIN_SLOTS = 2372;

    @Test void every_read_is_named_after_what_it_reads_and_once_per_rule() throws Exception {
        List<String> numbered = new ArrayList<>(), noIdentifier = new ArrayList<>(), duplicate = new ArrayList<>();
        int built = 0, plans = 0, slots = 0;
        for (Path ir : goldens()) {
            DroolsBackend.Build b;
            try {
                b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
            } catch (IrLoader.Refusal | IrLoader.Documentfout | CaseRunner.Excluded refused) {
                // a document the branch does not build has no plan; the floor counts the ones it builds
                continue;
            }
            built++;
            String golden = GOLDEN.relativize(ir.getParent()).toString();
            for (RulePlan p : b.execModelTranspiler().modelplan().rules()) {
                plans++;
                for (Read l : p.lezingen().slots()) {
                    if (l.naam() == null) continue;
                    slots++;
                    if (NUMBERED.matcher(l.naam()).matches()) numbered.add(golden + " | " + p.naam() + " | " + l);
                    if (!identifier(l.naam())) noIdentifier.add(golden + " | " + p.naam() + " | " + l);
                }
                Set<String> seen = new HashSet<>();
                for (String v : variables(p))
                    if (!seen.add(v)) duplicate.add(golden + " | " + p.naam() + " | " + v);
            }
        }
        assertEquals(List.of(), numbered, "genummerde lezingen");
        assertEquals(List.of(), noIdentifier, "namen die geen identifier zijn");
        assertEquals(List.of(), duplicate, "variabelen die een regel twee keer bindt");
        int g = built, n = plans, s = slots;
        assertTrue(g >= EXPECTED_MIN_BUILT, () -> g + " gebouwde goldens, onder de vloer " + EXPECTED_MIN_BUILT);
        assertTrue(n >= EXPECTED_MIN_PLANS, () -> n + " plannen, onder de vloer " + EXPECTED_MIN_PLANS);
        assertTrue(s >= EXPECTED_MIN_SLOTS, () -> s + " slots met een variabele, onder de vloer " + EXPECTED_MIN_SLOTS);
    }

    @Test void the_names_of_six_rules() throws Exception {
        assertEquals(List.of("INPUT_PATH $postKustvakGevarenklasse"),
                slots("anchor/jutterswet-kustwacht", "risicotoeslag van een wachter"));
        assertEquals(List.of("PATH_STATE $rekenkamerHeffing"),
                slots("anchor/jutterswet-rekenkamer", "heffing van een jutter"));
        assertEquals(List.of("ROLE_FIELD $toegewezenWachtpostGezet"),
                slots("jutterswet-wachtindeling", "wachtdoend bij wachtpost"));
        assertEquals(List.of("INVOER $rang", "SCOPE $berging"),
                slots("jutterswet-hoofdberging", "rang per berging: sleutel van Hoofdberger 1"));
        assertEquals(List.of("INVOER $aantalAangemeldeVondsten", "PARAMETER $vondstdrempel"),
                slots("jutterswet-penningdrempel", "Kenmerktoekenning jutter penninggerechtigd"));
        // "waarde" is a member of Berging; $waarde is a fixed name of the analysis, so the read takes a number
        assertEquals(List.of("INVOER $waarde2", "INVOER $vermenigvuldiger"),
                slots("jutterswet-bergloon", "product van een berging"));
    }

    @Test void the_fixed_names_are_those_of_the_source() throws Exception {
        Set<String> inSource = new TreeSet<>();
        Matcher m = Pattern.compile("\"(\\$[A-Za-z]+)\"").matcher(Files.readString(TRANSPILER));
        while (m.find()) inSource.add(m.group(1));
        assertEquals(inSource, new TreeSet<>(TmsTranspiler.FIXED_NAMES));
    }

    @Test void a_read_name_is_a_java_identifier_also_outside_the_corpus() {
        Map<String, String> verwacht = new LinkedHashMap<>();
        verwacht.put("artikel 3² inconsistent", "$artikel3Inconsistent");
        verwacht.put("½ deel", "$Deel");
        verwacht.put("3e lid", "$3eLid");
        verwacht.put("één coördinaat", "$éénCoördinaat");
        verwacht.put("art. 5, lid 2 (oud)", "$art5Lid2Oud");
        verwacht.put("", "$x");
        verwacht.put("²³", "$x");
        verwacht.put("waarde", "$waarde2");
        Map<String, String> received = new LinkedHashMap<>();
        for (String read : verwacht.keySet()) received.put(read, TmsTranspiler.readName(read, Set.of()));
        assertEquals(verwacht, received);
        for (String naam : received.values())
            assertTrue(identifier(naam) && !naam.contains("_"), () -> naam + " is geen variabele zonder underscore");
        // a name of which nothing stays takes a number where $x is taken, as every read name does
        assertEquals("$x2", TmsTranspiler.readName("²³", Set.of("$x")));
        assertEquals("$x3", TmsTranspiler.readName("", Set.of("$x", "$x2")));
    }

    @Test void the_plan_refuses_a_second_declaration_in_its_scope() {
        ReadTable empty = new ReadTable(List.of());
        Consequence channel = new Consequence.InsertChannelState("proef", true, Consequence.Insertion.LOGICAL);
        Subject onderwerp = new Subject(Object.class, "$o", List.of(), List.of(),
                List.of(Binding.van("$v", Value.class, "waarde")));
        assertEquals("de regel twee patronen declareert de variabele $o twee keer", assertThrows(
                IllegalArgumentException.class, () -> new RulePlan("twee patronen", null, null, empty,
                        List.of(onderwerp, new FactPattern(Value.class, "$o", List.of(), List.of(), List.of())), channel))
                .getMessage());
        assertEquals("de regel binding als patroon declareert de variabele $v twee keer", assertThrows(
                IllegalArgumentException.class, () -> new RulePlan("binding als patroon", null, null, empty,
                        List.of(onderwerp, new FactPattern(Value.class, "$v", List.of(), List.of(), List.of())), channel))
                .getMessage());
        assertEquals("de regel twee resultaten declareert de variabele $v twee keer", assertThrows(
                IllegalArgumentException.class, () -> new RulePlan("twee resultaten", null, null, empty,
                        List.of(onderwerp, new Accumulation(new FactPattern(Value.class, "$w", List.of(), List.of(),
                                List.of()), List.of(new Accumulation.Aggregation(Accumulation.Kind.COUNT, null, "$v")))),
                        channel)).getMessage());
        assertEquals("de regel binnen en buiten declareert de variabele $v twee keer", assertThrows(
                IllegalArgumentException.class, () -> new RulePlan("binnen en buiten", null, null, empty,
                        List.of(onderwerp, new Not(new FactPattern(Value.class, "$v", List.of(), List.of(), List.of()))),
                        channel)).getMessage());
        ReadTable twoSlots = new ReadTable(List.of(new Read(0, ReadKind.INVOER, "a", "$v"),
                new Read(1, ReadKind.INVOER, "b", "$v")));
        assertEquals("de regel twee slots geeft twee slots de variabele $v", assertThrows(
                IllegalArgumentException.class, () -> new RulePlan("twee slots", null, null, twoSlots,
                        List.of(onderwerp), channel)).getMessage());
        // what a quantification declares lives only inside it: a second quantification and an item after both may
        // declare that name again
        Not eerste = new Not(new FactPattern(Value.class, "$w", List.of(), List.of(), List.of()));
        Exists tweede = new Exists(List.of(new FactPattern(Value.class, "$w", List.of(), List.of(), List.of())),
                List.of("$w"), List.of());
        assertEquals(4, new RulePlan("lokaal", null, null, empty, List.of(onderwerp, eerste, tweede,
                new FactPattern(Value.class, "$w", List.of(), List.of(), List.of())), channel).leftSide().size());
    }

    /** The kind and the variable of every slot of the rule {@code regel} in the golden {@code golden}. */
    private static List<String> slots(String golden, String regel) throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(GOLDEN.resolve(golden).resolve("ir.json"),
                DroolsBackend.Branch.EXEC_MODEL);
        List<String> out = new ArrayList<>();
        for (RulePlan p : b.execModelTranspiler().modelplan().rules())
            if (p.naam().equals(regel))
                for (Read l : p.lezingen().slots()) out.add(l.soort() + " " + l.naam());
        return out;
    }

    /** Every variable the left-hand side of {@code p} declares, in order: a pattern's own and those its bindings bind,
     *  inside a {@code not}, an {@code exists} and the source of an accumulate as well, and the results of an
     *  accumulate. */
    private static List<String> variables(RulePlan p) {
        List<String> out = new ArrayList<>();
        for (LhsItem item : p.leftSide()) {
            switch (item) {
                case Subject o -> {
                    out.add(o.naam());
                    for (Binding b : o.bindings()) out.add(b.doel());
                }
                case FactPattern pt -> pattern(pt, out);
                case Not n -> n.patterns().forEach(pt -> pattern(pt, out));
                case Exists e -> e.patterns().forEach(pt -> pattern(pt, out));
                case Accumulation a -> {
                    pattern(a.bron(), out);
                    for (Accumulation.Aggregation f : a.aggregations()) out.add(f.doel());
                }
                case TestItem t -> { }
            }
        }
        return out;
    }

    private static void pattern(FactPattern pt, List<String> out) {
        out.add(pt.naam());
        for (Binding b : pt.bindings()) out.add(b.doel());
    }

    /** Whether {@code naam} is {@code $} and a Java identifier, which DRL text can spell as a variable. */
    private static boolean identifier(String naam) {
        if (naam.length() < 2 || naam.charAt(0) != '$') return false;
        for (int i = 1; i < naam.length(); i++)
            if (!Character.isJavaIdentifierPart(naam.charAt(i))) return false;
        return true;
    }

    private static List<Path> goldens() throws Exception {
        try (Stream<Path> s = Files.walk(GOLDEN)) {
            return s.filter(p -> p.getFileName().toString().equals("ir.json")).sorted().toList();
        }
    }
}
