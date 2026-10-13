package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.CaseRunner;
import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.transpiler.tms.plan.Accumulatie;
import dev.uchar.regelspraak.transpiler.tms.plan.Bestaat;
import dev.uchar.regelspraak.transpiler.tms.plan.Binding;
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
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 *       {@link TmsTranspiler#VASTE_NAMEN}: a read name has no underscore and is none of them, so it can meet no other
 *       variable of its rule, also in a document outside the corpus.</li>
 * </ul>
 *
 * <p><b>Red</b> before the step, on {@code 123e39e5}: the analysis called a read {@code $lezing_} with its slot, 1112
 * slots over the corpus, and {@code VASTE_NAMEN} did not exist.
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
