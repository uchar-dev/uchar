package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The harness of card drl-teksttak-op-truth-maintenance as a witness of the suite (the owner's decision on norm
 * question 1 of Fable advice 4): the two branches of the analysis, the exec-model branch and the DRL-text branch, over
 * the whole corpus. Gate 12 holds each golden to its expectations and the two branches to the same status line; this
 * holds them to the same session.
 *
 * <p>Every golden the exec-model branch plays, and every case of it, is fired through both branches as a consumer fires
 * it, on one build per branch, and the dumps of the two sessions ({@link Sessiedump}) must be equal: the input, what
 * {@code DroolsBackend.lees} gives, the facts at the fixpoint and the outcome of the fire. The firings and their order
 * are left out, as in {@link Takdumps}: they follow the agenda, which differs between the two renderings, while the
 * fixpoint is the same. A case whose fire reaches no fixpoint, at a cap, at the creation budget or by an exception,
 * would compare only its input and its line {@code fixpunt}, strictly, since the facts where a fire stops depend on the
 * order of the firings; no case of a played golden is one (measured 2026-10-14), and {@link #ZONDER_FIXPUNT} names
 * them, so that one that arrives shows by name. A golden the exec-model branch does not play is gate 12's channel, and
 * this witness skips it.
 *
 * <p>Every case under {@code corpus/rood} is played through the red path of both branches
 * ({@link CaseRunner#runRood(Path, Path, DroolsBackend.Branch)}), which must give the same {@code RoodResult}: the same
 * status and the same text. Gate 8's Drools leg plays the exec-model branch alone, so without this row no gate would
 * bring a red case to the DRL-text branch (decision 46 of the card).
 *
 * <p>Floors, measured 2026-10-14 at step 6b of the card, after its five goldens and its red case entered the corpus:
 * {@value #EXPECTED_MIN_GOLDENS} goldens compared and {@value #EXPECTED_MIN_RODE_CASES} red case files. A smaller
 * count is a corpus that lost goldens, or a selection that silently stopped comparing; a growing corpus needs no change
 * here. The two rows take about 17 s together (measured 2026-10-14).
 *
 * <p><b>Red</b> with mutation W2 of Fable advice 4, in a copy (not committed; the diff and the output are kept with
 * step 6b of the card): a DRL rendering that inserts every consequence as a stated fact instead of a logical one, so
 * that the facts of a retracted justification stay on the DRL-text branch.
 */
class TakdumpsCorpusWitnessTest {

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up
    private static final Path CORPUS = Path.of("..", "..", "corpus");

    /** The goldens compared, measured 2026-10-14: the 250 the exec-model branch played before step 6b and its five,
     *  with 695 cases. */
    static final int EXPECTED_MIN_GOLDENS = 255;
    /** The case files under {@code corpus/rood}, measured 2026-10-14: the 49 of before step 6b and its one. */
    static final int EXPECTED_MIN_RODE_CASES = 50;
    /** The cases of played goldens whose fire reaches no fixpoint, measured 2026-10-14: none. */
    static final List<String> ZONDER_FIXPUNT = List.of();

    @Test void elke_case_van_elke_gespeelde_golden_heeft_op_beide_takken_dezelfde_dump() throws Exception {
        List<Path> goldens;
        try (Stream<Path> s = Files.walk(CORPUS.resolve("golden"))) {
            goldens = s.filter(p -> p.getFileName().toString().equals("ir.json")).map(Path::getParent).sorted()
                    .toList();
        }
        List<String> anders = new ArrayList<>(), zonderFixpunt = new ArrayList<>();
        int vergeleken = 0, cases = 0;
        for (Path g : goldens) {
            List<Path> zaken = new ArrayList<>();
            for (Path zaak : zaken(g))
                if (!TmsDocumenten.JSON.readTree(zaak.toFile()).has("expectedFault")) zaken.add(zaak);
            if (zaken.isEmpty()) continue;
            String naam = CORPUS.resolve("golden").relativize(g).toString();
            Path ir = g.resolve("ir.json");
            if (!CaseRunner.viaExecModel(ir, zaken).status().equals("PLAYED")) continue;
            CaseRunner exec = CaseRunner.voorMeting(ir, DroolsBackend.Branch.EXEC_MODEL), drl;
            try {
                drl = CaseRunner.voorMeting(ir, DroolsBackend.Branch.DRL_TEXT);
            } catch (RuntimeException e) {
                anders.add(naam + ": de DRL-teksttak bouwt het document niet: " + e);
                continue;
            }
            vergeleken++;
            for (Path zaak : zaken) {
                JsonNode c = TmsDocumenten.JSON.readTree(zaak.toFile());
                String welke = naam + "/" + zaak.getFileName();
                String a = Takdumps.dump(exec, c), b = Takdumps.dump(drl, c);
                if (a.lines().noneMatch(r -> r.equals("fixpunt ja"))) zonderFixpunt.add(welke);
                String verschil = eersteVerschil(vergelijkbaar(a), vergelijkbaar(b));
                if (verschil != null) anders.add(welke + ": " + verschil);
                cases++;
            }
        }
        assertEquals(List.of(), anders, "cases waarvan de dumps van de twee takken verschillen");
        assertEquals(ZONDER_FIXPUNT, zonderFixpunt, "de cases zonder fixpunt");
        int n = vergeleken, m = cases;
        assertTrue(n >= EXPECTED_MIN_GOLDENS, () -> "vergeleken goldens " + n + " (" + m + " cases), onder de vloer "
                + EXPECTED_MIN_GOLDENS);
    }

    @Test void elke_rode_case_geeft_op_beide_takken_hetzelfde_roodresultaat() throws Exception {
        List<Path> zaken;
        try (Stream<Path> s = Files.walk(CORPUS.resolve("rood"))) {
            zaken = s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList();
        }
        List<String> anders = new ArrayList<>();
        for (Path zaak : zaken) {
            Path ir = zaak.resolveSibling("ir.json");
            String exec = rood(ir, zaak, DroolsBackend.Branch.EXEC_MODEL);
            String drl = rood(ir, zaak, DroolsBackend.Branch.DRL_TEXT);
            if (!exec.equals(drl))
                anders.add(CORPUS.resolve("rood").relativize(zaak) + ": exec-model " + exec + " | drl-tekst " + drl);
        }
        assertEquals(List.of(), anders, "rode cases met een ander resultaat op de twee takken");
        int n = zaken.size();
        assertTrue(n >= EXPECTED_MIN_RODE_CASES, () -> "rode cases " + n + ", onder de vloer "
                + EXPECTED_MIN_RODE_CASES);
    }

    /** The red path of {@code tak} on one case: its status and text, or the exception that left it. */
    private static String rood(Path ir, Path zaak, DroolsBackend.Branch tak) {
        try {
            CaseRunner.RoodResult r = CaseRunner.runRood(ir, zaak, tak);
            return r.status() + " " + r.detail();
        } catch (RuntimeException e) {
            return "ERROR " + e;
        }
    }

    /** What the two branches must have alike in a dump: at a fixpoint everything but the firings, and without one only
     *  the input and the line {@code fixpunt}. */
    private static String vergelijkbaar(String dump) {
        if (dump.lines().anyMatch(r -> r.equals("fixpunt ja"))) return Takdumps.zonderVuringen(dump);
        return Stream.of(dump.split("\n")).filter(r -> r.startsWith("invoer ") || r.startsWith("fixpunt "))
                .collect(Collectors.joining("\n"));
    }

    /** The first line where {@code exec} and {@code drl} part, with both lines; null where they are equal. */
    private static String eersteVerschil(String exec, String drl) {
        if (exec.equals(drl)) return null;
        String[] a = exec.split("\n"), b = drl.split("\n");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            String x = i < a.length ? a[i] : "<einde>", y = i < b.length ? b[i] : "<einde>";
            if (!x.equals(y)) return "regel " + (i + 1) + ": exec-model " + x + " | drl-tekst " + y;
        }
        return "gelijke regels, ander einde";
    }

    private static List<Path> zaken(Path map) throws Exception {
        try (Stream<Path> s = Files.list(map)) {
            return s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList();
        }
    }
}
