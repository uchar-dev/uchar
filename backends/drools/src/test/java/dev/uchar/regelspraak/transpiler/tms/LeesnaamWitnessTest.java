package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.CaseRunner;
import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.transpiler.tms.plan.Accumulatie;
import dev.uchar.regelspraak.transpiler.tms.plan.Bestaat;
import dev.uchar.regelspraak.transpiler.tms.plan.Binding;
import dev.uchar.regelspraak.transpiler.tms.plan.Gevolg;
import dev.uchar.regelspraak.transpiler.tms.plan.Leessoort;
import dev.uchar.regelspraak.transpiler.tms.plan.Leestabel;
import dev.uchar.regelspraak.transpiler.tms.plan.Lezing;
import dev.uchar.regelspraak.transpiler.tms.plan.LhsItem;
import dev.uchar.regelspraak.transpiler.tms.plan.Niet;
import dev.uchar.regelspraak.transpiler.tms.plan.Onderwerp;
import dev.uchar.regelspraak.transpiler.tms.plan.Patroon;
import dev.uchar.regelspraak.transpiler.tms.plan.Regelplan;
import dev.uchar.regelspraak.transpiler.tms.plan.Toetsitem;

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
 *       {@link TmsTranspiler#VASTE_NAMEN}: a read name has no underscore and is none of them, so it does not meet
 *       another variable of its rule. That is how the analysis avoids a second declaration; that none reaches a
 *       rendering, also in a document outside the corpus, is the plan's own check ({@link Regelplan}).</li>
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
 * slots over the corpus, and {@code VASTE_NAMEN} did not exist. <b>Red</b> before fix round 1e, on {@code 32b44001}
 * with only the read name drawn out into a method of its own: {@code "artikel 3² inconsistent"} read
 * {@code $artikel3²Inconsistent}, which no DRL function can declare, and the plan took two declarations of one name.
 */
class LeesnaamWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final Path TRANSPILER = Path.of("src", "main", "java", "dev", "uchar", "regelspraak", "transpiler",
            "tms", "TmsTranspiler.java");
    private static final Pattern GENUMMERD = Pattern.compile("\\$lezing_\\d+");

    /** Floors, measured 2026-10-13 at step 1e, so that a check over nothing does not pass: the goldens the branch
     *  builds, the plans they hold, and the slots with a variable. A deliberate removal lowers a floor in the same
     *  commit. */
    private static final int EXPECTED_MIN_GEBOUWD = 253;
    private static final int EXPECTED_MIN_PLANNEN = 1859;
    private static final int EXPECTED_MIN_SLOTS = 2372;

    @Test void elke_lezing_heet_naar_wat_zij_leest_en_eenmaal_per_regel() throws Exception {
        List<String> genummerd = new ArrayList<>(), geenIdentifier = new ArrayList<>(), dubbel = new ArrayList<>();
        int gebouwd = 0, plannen = 0, slots = 0;
        for (Path ir : goldens()) {
            DroolsBackend.Build b;
            try {
                b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
            } catch (IrLoader.Refusal | IrLoader.Documentfout | CaseRunner.Excluded geweigerd) {
                // a document the branch does not build has no plan; the floor counts the ones it builds
                continue;
            }
            gebouwd++;
            String golden = GOLDEN.relativize(ir.getParent()).toString();
            for (Regelplan p : b.execModelTranspiler().modelplan().regels()) {
                plannen++;
                for (Lezing l : p.lezingen().slots()) {
                    if (l.naam() == null) continue;
                    slots++;
                    if (GENUMMERD.matcher(l.naam()).matches()) genummerd.add(golden + " | " + p.naam() + " | " + l);
                    if (!identifier(l.naam())) geenIdentifier.add(golden + " | " + p.naam() + " | " + l);
                }
                Set<String> gezien = new HashSet<>();
                for (String v : variabelen(p))
                    if (!gezien.add(v)) dubbel.add(golden + " | " + p.naam() + " | " + v);
            }
        }
        assertEquals(List.of(), genummerd, "genummerde lezingen");
        assertEquals(List.of(), geenIdentifier, "namen die geen identifier zijn");
        assertEquals(List.of(), dubbel, "variabelen die een regel twee keer bindt");
        int g = gebouwd, n = plannen, s = slots;
        assertTrue(g >= EXPECTED_MIN_GEBOUWD, () -> g + " gebouwde goldens, onder de vloer " + EXPECTED_MIN_GEBOUWD);
        assertTrue(n >= EXPECTED_MIN_PLANNEN, () -> n + " plannen, onder de vloer " + EXPECTED_MIN_PLANNEN);
        assertTrue(s >= EXPECTED_MIN_SLOTS, () -> s + " slots met een variabele, onder de vloer " + EXPECTED_MIN_SLOTS);
    }

    @Test void de_namen_van_zes_regels() throws Exception {
        assertEquals(List.of("INVOERPAD $postKustvakGevarenklasse"),
                slots("anchor/jutterswet-kustwacht", "risicotoeslag van een wachter"));
        assertEquals(List.of("PADSTAND $rekenkamerHeffing"),
                slots("anchor/jutterswet-rekenkamer", "heffing van een jutter"));
        assertEquals(List.of("ROLVELD $toegewezenWachtpostGezet"),
                slots("jutterswet-wachtindeling", "wachtdoend bij wachtpost"));
        assertEquals(List.of("INVOER $rang", "SCOPE $berging"),
                slots("jutterswet-hoofdberging", "rang per berging: sleutel van Hoofdberger 1"));
        assertEquals(List.of("INVOER $aantalAangemeldeVondsten", "PARAMETER $vondstdrempel"),
                slots("jutterswet-penningdrempel", "Kenmerktoekenning jutter penninggerechtigd"));
        // "waarde" is a member of Berging; $waarde is a fixed name of the analysis, so the read takes a number
        assertEquals(List.of("INVOER $waarde2", "INVOER $vermenigvuldiger"),
                slots("jutterswet-bergloon", "product van een berging"));
    }

    @Test void de_vaste_namen_zijn_die_van_de_bron() throws Exception {
        Set<String> inBron = new TreeSet<>();
        Matcher m = Pattern.compile("\"(\\$[A-Za-z]+)\"").matcher(Files.readString(TRANSPILER));
        while (m.find()) inBron.add(m.group(1));
        assertEquals(inBron, new TreeSet<>(TmsTranspiler.VASTE_NAMEN));
    }

    @Test void een_leesnaam_is_een_java_identifier_ook_buiten_het_corpus() {
        Map<String, String> verwacht = new LinkedHashMap<>();
        verwacht.put("artikel 3² inconsistent", "$artikel3Inconsistent");
        verwacht.put("½ deel", "$Deel");
        verwacht.put("3e lid", "$3eLid");
        verwacht.put("één coördinaat", "$éénCoördinaat");
        verwacht.put("art. 5, lid 2 (oud)", "$art5Lid2Oud");
        verwacht.put("", "$x");
        verwacht.put("²³", "$x");
        verwacht.put("waarde", "$waarde2");
        Map<String, String> gekregen = new LinkedHashMap<>();
        for (String gelezen : verwacht.keySet()) gekregen.put(gelezen, TmsTranspiler.leesnaam(gelezen, Set.of()));
        assertEquals(verwacht, gekregen);
        for (String naam : gekregen.values())
            assertTrue(identifier(naam) && !naam.contains("_"), () -> naam + " is geen variabele zonder underscore");
        // a name of which nothing stays takes a number where $x is taken, as every read name does
        assertEquals("$x2", TmsTranspiler.leesnaam("²³", Set.of("$x")));
        assertEquals("$x3", TmsTranspiler.leesnaam("", Set.of("$x", "$x2")));
    }

    @Test void het_plan_weigert_een_tweede_declaratie_in_haar_scope() {
        Leestabel leeg = new Leestabel(List.of());
        Gevolg kanaal = new Gevolg.Kanaalstand("proef", true, Gevolg.Invoeging.LOGISCH);
        Onderwerp onderwerp = new Onderwerp(Object.class, "$o", List.of(), List.of(),
                List.of(Binding.van("$v", Waarde.class, "waarde")));
        assertEquals("de regel twee patronen declareert de variabele $o twee keer", assertThrows(
                IllegalArgumentException.class, () -> new Regelplan("twee patronen", null, null, leeg,
                        List.of(onderwerp, new Patroon(Waarde.class, "$o", List.of(), List.of(), List.of())), kanaal))
                .getMessage());
        assertEquals("de regel binding als patroon declareert de variabele $v twee keer", assertThrows(
                IllegalArgumentException.class, () -> new Regelplan("binding als patroon", null, null, leeg,
                        List.of(onderwerp, new Patroon(Waarde.class, "$v", List.of(), List.of(), List.of())), kanaal))
                .getMessage());
        assertEquals("de regel twee resultaten declareert de variabele $v twee keer", assertThrows(
                IllegalArgumentException.class, () -> new Regelplan("twee resultaten", null, null, leeg,
                        List.of(onderwerp, new Accumulatie(new Patroon(Waarde.class, "$w", List.of(), List.of(),
                                List.of()), List.of(new Accumulatie.Functie(Accumulatie.Soort.AANTAL, null, "$v")))),
                        kanaal)).getMessage());
        assertEquals("de regel binnen en buiten declareert de variabele $v twee keer", assertThrows(
                IllegalArgumentException.class, () -> new Regelplan("binnen en buiten", null, null, leeg,
                        List.of(onderwerp, new Niet(new Patroon(Waarde.class, "$v", List.of(), List.of(), List.of()))),
                        kanaal)).getMessage());
        Leestabel tweeSlots = new Leestabel(List.of(new Lezing(0, Leessoort.INVOER, "a", "$v"),
                new Lezing(1, Leessoort.INVOER, "b", "$v")));
        assertEquals("de regel twee slots geeft twee slots de variabele $v", assertThrows(
                IllegalArgumentException.class, () -> new Regelplan("twee slots", null, null, tweeSlots,
                        List.of(onderwerp), kanaal)).getMessage());
        // what a quantification declares lives only inside it: a second quantification and an item after both may
        // declare that name again
        Niet eerste = new Niet(new Patroon(Waarde.class, "$w", List.of(), List.of(), List.of()));
        Bestaat tweede = new Bestaat(List.of(new Patroon(Waarde.class, "$w", List.of(), List.of(), List.of())),
                List.of("$w"), List.of());
        assertEquals(4, new Regelplan("lokaal", null, null, leeg, List.of(onderwerp, eerste, tweede,
                new Patroon(Waarde.class, "$w", List.of(), List.of(), List.of())), kanaal).linkerkant().size());
    }

    /** The kind and the variable of every slot of the rule {@code regel} in the golden {@code golden}. */
    private static List<String> slots(String golden, String regel) throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(GOLDEN.resolve(golden).resolve("ir.json"),
                DroolsBackend.Branch.EXEC_MODEL);
        List<String> uit = new ArrayList<>();
        for (Regelplan p : b.execModelTranspiler().modelplan().regels())
            if (p.naam().equals(regel))
                for (Lezing l : p.lezingen().slots()) uit.add(l.soort() + " " + l.naam());
        return uit;
    }

    /** Every variable the left-hand side of {@code p} declares, in order: a pattern's own and those its bindings bind,
     *  inside a {@code not}, an {@code exists} and the source of an accumulate as well, and the results of an
     *  accumulate. */
    private static List<String> variabelen(Regelplan p) {
        List<String> uit = new ArrayList<>();
        for (LhsItem item : p.linkerkant()) {
            switch (item) {
                case Onderwerp o -> {
                    uit.add(o.naam());
                    for (Binding b : o.bindingen()) uit.add(b.doel());
                }
                case Patroon pt -> patroon(pt, uit);
                case Niet n -> n.patronen().forEach(pt -> patroon(pt, uit));
                case Bestaat e -> e.patronen().forEach(pt -> patroon(pt, uit));
                case Accumulatie a -> {
                    patroon(a.bron(), uit);
                    for (Accumulatie.Functie f : a.functies()) uit.add(f.doel());
                }
                case Toetsitem t -> { }
            }
        }
        return uit;
    }

    private static void patroon(Patroon pt, List<String> uit) {
        uit.add(pt.naam());
        for (Binding b : pt.bindingen()) uit.add(b.doel());
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
