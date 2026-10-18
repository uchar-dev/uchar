package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Documents fired through the DRL-text branch ({@code DRL_TEXT}), the analysis of the exec-model branch rendered as DRL
 * text (card drl-teksttak-op-truth-maintenance, since step 2b), each case a match of the expectations the reference
 * interpreter, the ASP backend and the exec-model branch give; the harness of the card compares every fact of these
 * sessions with that branch.
 *
 * <p><b>A link that is absent</b> (FABLE-2 §4 point 2): a read along a path is a function of the text that walks the
 * path with the route {@code langs} and gives null where a link is absent, as the executable model reads it; the
 * null-safe navigation of the dialect ({@code post!.hoogte}) would drop the match instead. The cases are the four
 * goldens whose case derives a value over an absent link in a binding along a path, which the spike of the second
 * advice measured to differ under {@code !.}, and two goldens whose case has an absent link in a field test along a
 * path: a Wachter without a post in {@code anchor/jutterswet-kustwacht}, a Schuit without a werf in
 * {@code jutterswet-gehele-periode-leeg}. Each text holds the form, and each case leaves the link out.
 *
 * <p><b>Members with names outside the corpus</b>: the documents of {@code PropertyWitnessTest}, the member "İş"
 * with and without input, the input "日 X" and the derived "İç", the input characteristic "ışık", and the cases of the
 * golden {@code jutterswet-sluimerwacht} with a key in another capitalisation: the text reads and writes each member
 * through the names the generator wrote.
 *
 * <p><b>Red</b> on {@code 5eda2eff}, before the branch: {@code DroolsBackend.Branch.DRL_TMS} did not exist, and this class
 * did not compile. Until the switch-over (step 6 of the card) the analysis rendered as DRL text was a third branch,
 * {@code DRL_TMS} with the flag {@code --drl-tms}, and this class was {@code DrlTmsWitnessTest}; since then that
 * rendering is the DRL-text branch.
 */
class DrlTextWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final Path PROPERTY = Path.of("src", "test", "resources", "property");

    /** A binding of a value along a path: the function of the path as the bound expression. */
    private static final Pattern PATH_IN_BINDING = Pattern.compile("\\$\\w+ : \\(r\\d+_pad\\d+\\(this\\)\\)");
    /** A field test along a path: the function of the path tested for null. */
    private static final Pattern PATH_IN_FIELD_TEST = Pattern.compile("r\\d+_pad\\d+\\(this\\) [=!]= null");

    /** One case: a golden, its case file, the form of the text the case reaches, and the instance whose link the case
     *  leaves out with the role it lacks. */
    private record Case(String golden, String caseFileName, Pattern form, String label, String role) {}

    @Test void a_missing_link_in_a_binding_and_in_a_field_test() throws Exception {
        List<Case> cases = List.of(
                new Case("synth-navigatiepremie", "case2.json", PATH_IN_BINDING, "S1", "werf"),
                new Case("jutterswet-havengeld", "case4-zonder-taxatie.json", PATH_IN_BINDING, "V", null),
                new Case("jutterswet-sluitsom", "case2-leeg-en-onverbonden.json", PATH_IN_BINDING, "V3",
                        "bewarende jutter"),
                new Case("synth-gehelejaarkenmerk", "case1-geen-koppeling.json", PATH_IN_BINDING, "S1", "werf"),
                new Case("anchor/jutterswet-kustwacht", "case2.json", PATH_IN_FIELD_TEST, "W3", "post"),
                new Case("jutterswet-gehele-periode-leeg", "case1-geheel-leeg.json", PATH_IN_FIELD_TEST, "S2", "werf"));
        List<String> faults = new ArrayList<>();
        for (Case g : cases) {
            Path ir = GOLDEN.resolve(g.golden()).resolve("ir.json");
            JsonNode caseDocument = JSON.readTree(GOLDEN.resolve(g.golden()).resolve(g.caseFileName()).toFile());
            String text = DroolsBackend.build(ir, DroolsBackend.Branch.DRL_TEXT).drl();
            if (!g.form().matcher(text).find()) faults.add(g + ": de tekst heeft de vorm niet");
            if (!withoutLink(caseDocument, g)) faults.add(g + ": de case geeft de link");
            String outcome = CaseRunner.playCase(ir, caseDocument, DroolsBackend.Branch.DRL_TEXT);
            if (outcome != null) faults.add(g.golden() + " " + g.caseFileName() + ": " + outcome);
        }
        assertEquals(List.of(), faults);
    }

    /** Whether the case leaves out the link of {@code g}: the instance carries no role {@code g.role()}, or, where that is
     *  null, no instance names the label in a role, the link held from the other end. */
    private static boolean withoutLink(JsonNode caseDocument, Case g) {
        for (JsonNode i : caseDocument.get("input").get("instances")) {
            JsonNode roles = i.get("roles");
            if (g.role() == null) {
                if (roles != null) for (JsonNode labels : roles) for (JsonNode l : labels)
                    if (l.asText().equals(g.label())) return false;
            } else if (i.get("label").asText().equals(g.label())) {
                return roles == null || !roles.has(g.role()) || roles.get(g.role()).isEmpty();
            }
        }
        return g.role() == null;
    }

    /** The documents of {@code PropertyWitnessTest} and the cases of {@code eigenschap/spelling}, each case through
     *  the DRL-text branch; that test fires them through the exec-model branch. */
    @Test void the_documents_with_awkward_names_fire() throws Exception {
        List<String[]> cases = new ArrayList<>();
        for (String document : List.of("turkish-member", "awkward-members", "turkish-characteristic"))
            try (Stream<Path> s = Files.list(PROPERTY.resolve(document))) {
                for (Path casePath : s.filter(p -> p.getFileName().toString().startsWith("case")).sorted().toList())
                    cases.add(new String[]{PROPERTY.resolve(document).resolve("ir.json").toString(), casePath.toString()});
            }
        try (Stream<Path> s = Files.list(PROPERTY.resolve("spelling"))) {
            for (Path casePath : s.filter(p -> p.getFileName().toString().startsWith("case")).sorted().toList())
                cases.add(new String[]{GOLDEN.resolve("jutterswet-sluimerwacht").resolve("ir.json").toString(),
                        casePath.toString()});
        }
        List<String> faults = new ArrayList<>();
        for (String[] g : cases) {
            String outcome = CaseRunner.playCase(Path.of(g[0]), JSON.readTree(Path.of(g[1]).toFile()),
                    DroolsBackend.Branch.DRL_TEXT);
            if (outcome != null) faults.add(g[1] + ": " + outcome);
        }
        assertEquals(9, cases.size(), "de cases van de documenten");
        assertEquals(List.of(), faults);
    }

    /** The CLI's {@code --drl} replays through the branch: a golden plays, also one with day kinds, whose
     *  predicates the build gives as the globals the text declares ({@code Build.dayKindGlobals}). <b>Red</b> on
     *  {@code 2704d81d}, before the day kinds of step 5: the branch, then behind {@code --drl-tms}, refused the golden
     *  with day kinds, "EXCLUDE drl-tms: een document met dagsoorten", and this test pinned that refusal. */
    @Test void the_flag_drl_plays_through_the_branch() throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        Path kustwacht = GOLDEN.resolve("anchor").resolve("jutterswet-kustwacht");
        int code = DroolsBackendMain.run(new String[]{"--drl", kustwacht.resolve("ir.json").toString(),
                kustwacht.resolve("case1.json").toString(), kustwacht.resolve("case2.json").toString()},
                new java.io.PrintStream(out, true, java.nio.charset.StandardCharsets.UTF_8));
        assertEquals("PLAYED 2", out.toString(java.nio.charset.StandardCharsets.UTF_8).trim());
        assertEquals(0, code);
        out.reset();
        Path getijklok = GOLDEN.resolve("jutterswet-getijklok");
        List<String> arguments = new ArrayList<>(List.of("--drl", getijklok.resolve("ir.json").toString()));
        try (Stream<Path> s = Files.list(getijklok)) {
            s.filter(p -> p.getFileName().toString().startsWith("case")).sorted().forEach(p -> arguments.add(p.toString()));
        }
        code = DroolsBackendMain.run(arguments.toArray(String[]::new),
                new java.io.PrintStream(out, true, java.nio.charset.StandardCharsets.UTF_8));
        assertEquals("PLAYED 2", out.toString(java.nio.charset.StandardCharsets.UTF_8).trim());
        assertEquals(0, code);
        DroolsBackend.Build b = DroolsBackend.build(getijklok.resolve("ir.json"), DroolsBackend.Branch.DRL_TEXT);
        java.util.Set<String> declared = new java.util.TreeSet<>();
        java.util.regex.Matcher m = Pattern.compile("(?m)^global java\\.util\\.function\\.Predicate (\\w+);$")
                .matcher(b.drl());
        while (m.find()) declared.add(m.group(1));
        assertTrue(!declared.isEmpty(), "de tekst declareert de global van een dagsoort");
        assertEquals(declared, new java.util.TreeSet<>(b.dayKindGlobals().keySet()));
    }

    /**
     * Two day kinds whose predicates differ on the dates the case reaches (round 5b, on the lens of round 5), so that a
     * text that swaps two predicates cannot pass. On the DRL-text branch the predicate of a day kind is a global of the
     * text, which the session sets by its name from {@code Build.dayKindGlobals}; the test of the names alone
     * ({@link #the_flag_drl_plays_through_the_branch}) passes with a swapped pair. The document
     * {@code src/test/resources/dagsoorten/twee-dagsoorten} is {@code jutterswet-getijklok} with a second day kind
     * beside the springtijdag (day 1 or 15 of the month): the doodtijdag (day 8 or 22), with a doodtermijn in
     * doodtijdagen and a doodsluiting, the meldtijdstip plus the doodtermijn. The case steps over both: the
     * sluittijdstip of G1 is 2026-04-15T09:15:30 and its doodsluiting 2026-04-22T09:15:30, which a swap turns around.
     * Each global carries the predicate of its own day kind, true on a day of that kind and false on one of the other,
     * and the case plays on both branches.
     *
     * <p><b>Measured</b> (2026-10-14): verifier PASS; the reference interpreter and the ASP backend PASS (7
     * expectations). <b>Red</b> with a mutant that swaps the two predicates among the globals of the text (in
     * {@code DrlRendering.render}, not committed): "rsDagsoort_Springtijdag: de springtijdag ==&gt; expected: &lt;[true,
     * false]&gt; but was: &lt;[false, true]&gt;"; and with that check left out of a copy, the case on the DRL-text
     * branch (then {@code DRL_TMS}), "case: attribuut sluittijdstip: verwacht 2026-04-15T09:15:30, werkelijk
     * 2026-04-22T09:15:30; attribuut doodsluiting: verwacht 2026-04-22T09:15:30, werkelijk 2026-04-15T09:15:30;
     * attribuut openingstijdstip: verwacht 2026-02-01T09:15:30, werkelijk 2026-02-08T09:15:30; …".
     */
    @Test void two_day_kinds_on_dates_on_which_their_predicates_differ() throws Exception {
        Path map = Path.of("src", "test", "resources", "day-kinds", "two-day-kinds");
        Path ir = map.resolve("ir.json");
        JsonNode caseDocument = JSON.readTree(map.resolve("case.json").toFile());
        Map<String, Predicate<LocalDate>> predicates =
                DroolsBackend.build(ir, DroolsBackend.Branch.DRL_TEXT).dayKindGlobals();
        assertEquals(Set.of("rsDagsoort_Springtijdag", "rsDagsoort_Doodtijdag"), predicates.keySet(),
                "de predicaatglobals van de tekst");
        LocalDate fifteen = LocalDate.of(2026, 3, 15), twentyTwo = LocalDate.of(2026, 3, 22);
        Predicate<LocalDate> spring = predicates.get("rsDagsoort_Springtijdag");
        Predicate<LocalDate> dood = predicates.get("rsDagsoort_Doodtijdag");
        assertEquals(List.of(true, false), List.of(spring.test(fifteen), spring.test(twentyTwo)),
                "rsDagsoort_Springtijdag: de springtijdag");
        assertEquals(List.of(false, true), List.of(dood.test(fifteen), dood.test(twentyTwo)),
                "rsDagsoort_Doodtijdag: de doodtijdag");
        for (DroolsBackend.Branch branch : List.of(DroolsBackend.Branch.EXEC_MODEL, DroolsBackend.Branch.DRL_TEXT))
            assertEquals(null, CaseRunner.playCase(ir, caseDocument, branch), branch + ": twee dagsoorten");
    }
}
