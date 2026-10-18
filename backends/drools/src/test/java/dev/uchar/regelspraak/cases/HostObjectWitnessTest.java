package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The GASTHEER of a characteristic check, pinned against the spec text that norms it (WF-109,
 * {@code spec/IR-SPEC.md} ch. 4.6 (q2); {@code spec/RUNTIME-SEMANTICS.md} ch. 5.5 and ch. 5.8b worked
 * example B). The field names the object type the check LIVES ON, and the §13.4.7 creation guard is the one
 * condition position with two objects in scope — the anker it iterates and the doel its join chain reaches.
 *
 * <p><b>Why this backend needs its own witness for a reading it already had.</b> Both branches here have
 * hosted a doel-tagged guard on the {@code $doel} pattern since that arm was written, and until 2026-09-19
 * they were alone in it: {@code ref} and the ASP backend refused the form, so the corpus could not carry it
 * and the behaviour rested on nothing but the code. Now the corpus does carry it
 * ({@code jutterswet-seinhijsing}, {@code jutterswet-mastsein}) and the gate replays both branches over it —
 * but a corpus replay pins ONE host per creation. What this class adds is the CONTRAST on the same document:
 * the anker host and the third-type host, which no golden can hold beside the doel host, plus the
 * value-divergence probes that turn "agrees with the expectation" into "reads the doel".
 *
 * <p>Every model is built in memory from a committed golden's {@code ir.json}, off the wire, with exactly one
 * field moved; the corpus files are read and never written.
 */
class HostObjectWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** One intermediate step (Kustvak -> Post -> Sein), both polarities of the doel-hosted guard. */
    private static final String SEINHIJSING = "jutterswet-seinhijsing";

    /** Two intermediate steps (Kustvak -> Post -> Mast -> Sein), one doel-hosted guard. */
    private static final String MASTSEIN = "jutterswet-mastsein";

    /** The doel's SUPERTYPE in the host field (Sein extends Baken, host "Baken") — hand-written IR, because
     *  the frontend tags a guard with the doel's own type and never with an ancestor. */
    private static final String BAKENSEIN = "jutterswet-bakensein";

    /** The reason both branches print for a guard that lowers to neither pattern. */
    private static final String REASON = "voorwaarde niet naar anker of doel te lowereren";

    // ---------------- 1. the doel reading, and the two readings it is NOT ----------------

    @Test void the_target_host_reads_the_target_in_both_branches() throws Exception {
        // The golden as committed. `gehesen` is declared on Kustvak, Post AND Sein, and the case carries it
        // on the Kustvak (V1) and on one of the two Seinen (S1) but not on the Post — so the three candidate
        // readings give three different counts and the answer identifies the reading:
        //     anker reading        -> V1 is gehesen, every sein reached -> vakseinaantal 2
        //     intermediate reading -> P1 is not     -> the creation never fires -> 0
        //     doel reading         -> S1 yes, S2 no -> 1
        // Measured 2026-09-19 in both branches: PLAYED, vakseinaantal 1 and duisteraantal 1 (the negated
        // sister creation over the same chain), which is the partition the two polarities state: 1 + 1 = 2.
        Path ir = golden(SEINHIJSING);
        BothBranches.green(ir, caseOf(SEINHIJSING), "de doel-gehoste guard leest het doel, in beide takken");
    }

    @Test void the_other_two_reads_fail_and_are_observable_in_both_branches() throws Exception {
        // The probes that make the row above a measurement instead of a coincidence: claim the ANKER
        // reading's number and the intermediate step's number, and both branches must say so by value.
        Path ir = golden(SEINHIJSING);
        BothBranches.red(ir, withExpectation(SEINHIJSING, "vakseinaantal", "2"), "werkelijk 1",
                "de ankerlezing (2) is niet wat er gebeurt");
        BothBranches.red(ir, withExpectation(SEINHIJSING, "vakseinaantal", "0"), "werkelijk 1",
                "de tussenstaplezing (0) is niet wat er gebeurt");
        // and the negated sister creation, the same way round
        BothBranches.red(ir, withExpectation(SEINHIJSING, "duisteraantal", "2"), "werkelijk 1",
                "de ontkende guard filtert het complement, niet alles");
    }

    // ---------------- 2. the anker host: the contrast no golden can hold ----------------

    @Test void the_anchor_host_reads_the_anchor_in_both_branches() throws Exception {
        // One field moved: the first creation's host from `Sein` to `Kustvak`. The guard then GATES the anker
        // — V1 is gehesen, so every sein it reaches is linked — and vakseinaantal becomes 2, while the second
        // creation keeps its doel host and its 1. Same document, same case population, a different number
        // because a different object was read. This is the row the corpus cannot carry beside the other one.
        Path ir = withHost(SEINHIJSING, 0, "Kustvak");
        BothBranches.green(ir, withExpectation(SEINHIJSING, "vakseinaantal", "2"),
                "de anker-gehoste guard poort de anker, in beide takken");
    }

    // ---------------- 3. the intermediate step: an object no pattern carries ----------------

    @Test void the_path_step_host_is_refused_in_both_branches() throws Exception {
        // `Post` is the type the chain walks THROUGH. It declares `gehesen` in this model, so WF-100's
        // declaration half is satisfied and the defect is the object half alone: the creation has two
        // patterns and this names a third object. WF-109 refuses the document before execution; this row
        // pins that the backend does not quietly host it somewhere either, in either branch.
        Path ir = withHost(SEINHIJSING, 0, "Post");
        BothBranches.refused(ir, caseOf(SEINHIJSING), "feitcreatie: vakbeseining door het kustvak — een"
                        + " CharacteristicCheck met een host die het onderwerp niet is ('het sein gehesen is')",
                "een gastheer die anker noch doel is, is een weigering en geen nul-creatie");
    }

    // ---------------- 4. two intermediate steps: the chain's END, not its next hop ----------------

    @Test void the_multi_step_target_host_reads_the_target_in_both_branches() throws Exception {
        // Kustvak -> Post -> Mast -> Sein, with `gehesen` declared on all four and carried by the anker
        // (V1), NOT by the first step (P1), by the second step (M1), and by two of the three Seinen. The four
        // candidate readings give 3, 0, 3 and 2; only the doel reading gives 2. Measured 2026-09-19 in both
        // branches: PLAYED, vakseinaantal 2.
        Path ir = golden(MASTSEIN);
        BothBranches.green(ir, caseOf(MASTSEIN), "de doel-lezing reikt langs twee tussenstappen, in beide takken");
        BothBranches.red(ir, withExpectation(MASTSEIN, "vakseinaantal", "3"), "werkelijk 2",
                "noch de anker noch de tweede tussenstap (3) is wat er gelezen wordt");
        BothBranches.red(ir, withExpectation(MASTSEIN, "vakseinaantal", "0"), "werkelijk 2",
                "de eerste tussenstap (0) is het evenmin");
    }

    // ---------------- 5. the EXTENSIONAL host: a supertype is this pattern's type too ----------------

    @Test void the_supertype_host_reads_the_target_in_both_branches() throws Exception {
        // IR-SPEC ch. 4.6 (q2) reads "an object of type T satisfies host O iff T ⊑ O", so a host naming an
        // ANCESTOR of the doel is legal and must be carried. Until 2026-09-19 both branches asked the question by
        // NAME IDENTITY (`k.owner() != subject`) and refused it, while the verifier, ref and the ASP backend all
        // carried the document — two executors answering and two refusing one verifier-clean form, which is the
        // split WF-109 exists to close. Measured RED FIRST on the pre-fix jar:
        //     verifier : PASS
        //     ref      : PASS (3 expectations)  — vakseinaantal 1, the doel reading
        //     clingo   : PASS (3 expectations)  — idem
        //     exec     : EXCLUDE transpiler SKIP: feitcreatie (voorwaarde niet naar anker of doel te lowereren)
        //     drl-tekst: EXCLUDE drl-transpiler MARKER: the same reason line
        // The golden carries `gehesen` on Baken (inherited by the Sein doel) and a SEPARATE `gehesen` on the
        // Kustvak anker and on the Post step, so the anker reading (2) and the step reading (0) stay observable.
        Path ir = golden(BAKENSEIN);
        BothBranches.green(ir, caseOf(BAKENSEIN), "de supertype-gastheer is de doel-gastheer, in beide takken");
        BothBranches.red(ir, withExpectation(BAKENSEIN, "vakseinaantal", "2"), "werkelijk 1",
                "de ankerlezing (2) is niet wat er gebeurt");
        BothBranches.red(ir, withExpectation(BAKENSEIN, "vakseinaantal", "0"), "werkelijk 1",
                "de tussenstaplezing (0) is het evenmin");
    }

    // ---------------- fixtures ----------------

    /** A committed golden's IR, unmodified — a temp copy, so the corpus file is never handed to a writer. */
    private Path golden(String naam) throws Exception {
        return write(JSON.readTree(Files.readString(CORPUS.resolve(naam).resolve("ir.json"))));
    }

    /** The same IR with creation {@code nr}'s guard re-hosted on {@code type} — one field, nothing else. */
    private Path withHost(String naam, int nr, String type) throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(CORPUS.resolve(naam).resolve("ir.json")));
        ((ObjectNode) IrPath.version(ir, "factCreations", nr, 0).get("condition")).put("host", type);
        return write(ir);
    }

    private Path write(JsonNode ir) throws Exception {
        Path tmp = Files.createTempFile("gastheer-object", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }

    /** The golden's own case, verbatim. */
    private JsonNode caseOf(String naam) throws Exception {
        return JSON.readTree(Files.readString(CORPUS.resolve(naam).resolve("case.json")));
    }

    /** The golden's own case with ONE expected attribute value replaced — the population is untouched, so a
     *  divergence can only come from the reading. */
    private JsonNode withExpectation(String naam, String attribute, String value) throws Exception {
        ObjectNode c = (ObjectNode) caseOf(naam);
        for (JsonNode e : c.get("expected"))
            if ("attributeValue".equals(e.path("kind").asText()) && attribute.equals(e.path("attribute").asText()))
                ((ObjectNode) e.get("value")).put("number", value);
        return c;
    }
}
