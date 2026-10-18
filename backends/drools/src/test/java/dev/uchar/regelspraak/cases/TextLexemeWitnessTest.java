package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One {@code Literal} of type {@code Text}, one value — in BOTH transpiler branches. IR-SPEC ch. 4.1 puts the
 * parsed string in its {@code text} value (the 2026-09-18 canonicalisation strips only the emitter's outer delimiters;
 * a hand-authored value may carry any text); the reference interpreter reads it verbatim into its text value and a
 * case input's {@code {"text": …}} carries it verbatim too, so the value a back-end compares (or writes) must be
 * that string and nothing else. Until 2026-09-18 the two branches disagreed: the exec-model branch had a private
 * verbatim reader while the DRL-text branch went through a shared reader that removed EVERY double quote, and the
 * comment claiming one shared interpretation was therefore false.
 *
 * <p>Two lexeme families reach the reader and the removal broke them in OPPOSITE directions:
 * <ul>
 *   <li>a text with an EMBEDDED quote ({@code merk "AA"}) lost the quote, so an equality that should hold fired
 *       false — {@code corpus/golden/jutterswet-keurmerk} is exactly that document, and it FAILED through the DRL
 *       branch while the exec branch played it;</li>
 *   <li>a value whose OWN outer quotes are part of it lost its delimiters while the case side kept them, so an
 *       equality that should hold fired false as well.</li>
 * </ul>
 * A reading that strips and a reading that keeps are each self-consistent; what no document survives is the two
 * of them in one toolchain. So every witness below asserts BOTH branches on ONE document. Measured against the
 * removal, put back BOTH ways — restored in the shared reader while the exec branch keeps its private one (the
 * exact pre-fix state), and restored in the shared reader with both branches on it (the success-shaped no-op, a
 * "fix" that only deletes the private copy) — each probe turns 6 of the 8 quote witnesses (sections 1–5) red. The
 * two that stay green are the quote-free flank and the mirror direction, both pinned for exactly that reason.
 *
 * <p>Section 6 pins the second character class the same splice must survive: a CR or LF inside the value, which
 * until 2026-09-18 reached the generated DRL as a REAL line break and was then dropped on the WRITE side only —
 * the same shape of divergence one character class further on, and the reason this class grew rather than a new
 * one appearing beside it.
 *
 * <p>All models are SYNTHETIC, built in memory from {@code jutterswet-keurmerk}'s {@code ir.json} off the wire;
 * the corpus files are read and never written. Each was measured through the verifier, {@code ref/} and the
 * clingo backend, and the outcomes are recorded at the witness that pins them — a Drools branch agreeing with
 * itself proves nothing; agreeing with the arbiters is the point.
 */
class TextLexemeWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** A ch. 5.3 Tekst equality in the voorwaarde and a ch. 13.4.6 Tekst value in the consequence — both halves of
     *  the question in two rules over one Vondst, and both literals carry an embedded double quote. */
    private static final String KEURMERK = "jutterswet-keurmerk";

    /** The consequence value wherever a witness wants the WRITE side quote-bearing too: its own outer quotes are
     *  part of the text, so a branch that unwraps the write fails the read-back just as one that unwraps the
     *  comparison fails the condition. */
    private static final String CONSEQUENCE = "\"Noordzee\"";

    /** The quote-free consequence, used by the one witness that must stay green under the old reading. */
    private static final String RAW_CONSEQUENCE = "Zeehaven";

    // ---------------- 1. raw == raw: the untouched flank ----------------

    @Test void raw_text_compares_equal_in_both_branches() throws Exception {
        // The flank that must NOT move, and the only witness here with no quote anywhere in it: comparison AND
        // consequence are quote-free, so the old DRL reading and the new one agree — measured, both mutation probes
        // below leave this one green. That is what it is for: a "fix" that starts unwrapping, trimming or
        // case-folding something else breaks this witness first. Measured:
        //     verifier: PASS
        //     ref     : oracle: PASS (3 expectations)
        //     clingo  : clingo: PASS (3 expectations)
        Path ir = model("tekstlexeem-rauw", "equal", "Flevoland", RAW_CONSEQUENCE);
        playBothBranches(ir, caseOf("rauw-raakt", "Flevoland", true, RAW_CONSEQUENCE));
    }

    // ---------------- 2. quote-bearing == the same quote-bearing case value ----------------

    @Test void quote_carrying_text_compares_equal_in_both_branches() throws Exception {
        // MUTATION WITNESS. Literal and case value carry the SAME outer double quotes — two identical strings, so
        // every executor that compares the value answers WAAR. The old DRL reading unwrapped only the literal and
        // then compared `Flevoland` against `"Flevoland"`, so the rule never fired and the case FAILED:
        //     Drools DRL-tekst (pre-fix): FAIL — kenmerk gekeurd: verwacht aanwezig, werkelijk afwezig
        // Measured on the fixed jar:
        //     verifier: PASS
        //     ref     : oracle: PASS (3 expectations)
        //     clingo  : clingo: PASS (3 expectations)
        Path ir = model("tekstlexeem-quote", "equal", "\"Flevoland\"", CONSEQUENCE);
        playBothBranches(ir, caseOf("quote-raakt", "\"Flevoland\"", true, CONSEQUENCE));
    }

    // ---------------- 3. an embedded quote survives, in the condition AND in the consequence ----------------

    @Test void embedded_quotes_survive_in_both_branches() throws Exception {
        // MUTATION WITNESS, and the one document that needs no fixture at all: the corpus golden itself compares
        // against a text with an embedded quote and WRITES another one. Pre-fix the DRL branch read the comparison
        // literal as its quote-free interior, so the keurmerk rule never fired and the write never happened:
        //     Drools DRL-tekst (pre-fix): FAIL — kenmerk gekeurd: verwacht aanwezig, werkelijk afwezig;
        //                                 attribuut afslag: verwacht veiling "Zee", werkelijk leeg
        // Gates 5 and 6 pin these same expectations for ref and clingo, so this is the square on a real golden.
        Path ir = ir(KEURMERK);
        playBothBranches(ir, ir.resolveSibling("case.json"));
    }

    @Test void the_consequence_writes_the_whole_lexeme_in_both_branches() throws Exception {
        // The WRITE side on its own, with a mutation probe so a bare PLAYED cannot carry the claim: the expectation
        // asks for the unwrapped interior, which is exactly what a stripping branch would store, so BOTH branches
        // must FAIL here and must name the same read-back. Measured:
        //     verifier: PASS
        //     ref     : MISMATCH: V1.afslag = "\"Noordzee\"", expected "Noordzee"
        //     clingo  : MISMATCH: V1.afslag = [""Noordzee""], expected "Noordzee"
        Path ir = model("tekstlexeem-gevolg", "equal", "Flevoland", CONSEQUENCE);
        Path c = caseOf("gevolg-ontdaan", "Flevoland", true, "Noordzee");

        CaseRunner.Result exec = CaseRunner.viaExecModel(ir, List.of(c));
        assertEquals("FAIL", exec.status(), exec.reason());
        assertTrue(exec.caseFailures().toString().contains("afslag: verwacht Noordzee, werkelijk \"Noordzee\""),
                exec.caseFailures().toString());
        CaseRunner.Result drl = CaseRunner.viaDrlText(ir, List.of(c));
        assertEquals("FAIL", drl.status(), drl.reason());
        assertTrue(drl.caseFailures().toString().contains("afslag: verwacht Noordzee, werkelijk \"Noordzee\""),
                drl.caseFailures().toString());
    }

    // ---------------- 4. raw and quote-bearing are DIFFERENT values ----------------

    @Test void a_quote_carrying_literal_does_not_touch_a_raw_case_value() throws Exception {
        // MUTATION WITNESS, and the sharpest one: pre-fix the DRL branch unwrapped the literal to `Flevoland`,
        // matched the raw case value and FIRED a rule the wire does not license —
        //     Drools DRL-tekst (pre-fix): FAIL — kenmerk gekeurd: verwacht afwezig, werkelijk aanwezig
        // — so it is red for BOTH failure modes: for a branch that keeps its own reading, and for a shared helper
        // that strips again (both branches would then fire). Measured on the fixed jar:
        //     verifier: PASS
        //     ref     : oracle: PASS (3 expectations)
        //     clingo  : clingo: PASS (3 expectations)
        Path ir = model("tekstlexeem-quote", "equal", "\"Flevoland\"", CONSEQUENCE);
        playBothBranches(ir, caseOf("rauwe-invoer", "Flevoland", false, null));
    }

    @Test void a_raw_literal_does_not_touch_a_quote_carrying_case_value() throws Exception {
        // The mirror direction. No branch ever got this one wrong, and it is pinned because the fix must not be
        // an implicit normalisation in the OTHER direction either (unwrapping the case input instead of the
        // literal would pass every witness above and fail this one). Measured on the fixed jar:
        //     verifier: PASS
        //     ref     : oracle: PASS (3 expectations)
        //     clingo  : clingo: PASS (3 expectations)
        Path ir = model("tekstlexeem-rauw", "equal", "Flevoland", RAW_CONSEQUENCE);
        playBothBranches(ir, caseOf("quote-invoer", "\"Flevoland\"", false, null));
    }

    // ---------------- 5. ONGELIJK, and the beslistabel's OR-row ----------------

    @Test void not_equal_mirrors_the_same_lexeme_equality_in_both_branches() throws Exception {
        // The complement of the equality, on the same quote-bearing pair: ONGELIJK over two identical strings is
        // onwaar (so nothing fires), ONGELIJK against a different province is waar. Both branches, one document
        // each. Measured on the fixed jar:
        //     verifier: PASS
        //     ref     : oracle: PASS (3 expectations) for both cases
        //     clingo  : clingo: PASS (3 expectations) for both cases
        Path ir = model("tekstlexeem-ongelijk", "notEqual", "\"Flevoland\"", CONSEQUENCE);
        playBothBranches(ir, caseOf("ongelijk-zelfde", "\"Flevoland\"", false, null));
        playBothBranches(ir, caseOf("ongelijk-ander", "\"Zeeland\"", true, CONSEQUENCE));
    }

    @Test void the_or_row_chooses_on_the_whole_lexeme_in_both_branches() throws Exception {
        // MUTATION WITNESS on the ch. 12 beslistabel shape: a TEN_MINSTE-1 Samengesteld whose onderdelen are the
        // "is gelijk aan X" cells of one row. Every cell carries its own outer quotes, so pre-fix the DRL branch
        // unwrapped all three and the row matched NOTHING a case supplies with quotes:
        //     Drools DRL-tekst (pre-fix): FAIL — kenmerk gekeurd: verwacht aanwezig, werkelijk afwezig
        // A member hits, a non-member misses, and a member's unwrapped interior misses too — the third case is
        // what keeps a stripping helper from passing this witness by accident. Measured on the fixed jar:
        //     verifier: PASS
        //     ref     : oracle: PASS (3 expectations) for all three cases
        //     clingo  : clingo: PASS (3 expectations) for all three cases
        Path ir = orRow("tekstlexeem-ofrij", "\"Flevoland\"", "\"Zeeland\"", "\"Drenthe\"");
        playBothBranches(ir, caseOf("ofrij-lid", "\"Zeeland\"", true, CONSEQUENCE));
        playBothBranches(ir, caseOf("ofrij-geen-lid", "\"Utrecht\"", false, null));
        playBothBranches(ir, caseOf("ofrij-ontdaan-lid", "Zeeland", false, null));
    }

    // ---------------- 6. the OTHER character the DRL text must escape: a line terminator ----------------

    /** A Tekst whose value contains a line break, and the same value flattened — two DIFFERENT strings to the wire,
     *  to ref and to the verifier, so no executor may confuse them. */
    private static final String LINE_END = "uit\nvoer";
    private static final String FLATTENED = "uitvoer";

    /** The consequence value for the line-terminator witnesses: the WRITE side is where the loss happened, so the
     *  written value carries a line break of its own. */
    private static final String CONSEQUENCE_LINE_END = "afslag\nnoord";

    @Test void a_line_end_survives_comparison_and_consequence_in_both_branches() throws Exception {
        // MUTATION WITNESS for the second escape in the shared naming contract's DRL-string helper. Until 2026-09-18 that
        // helper escaped `\` and `"` but neither line terminator, so a CR or LF reached the generated DRL as a
        // REAL line break — and the two halves of a rule then disagreed about it. The constraint side kept the
        // character (so the comparison below fired), while the consequence block's pre-processor dropped it:
        //     Drools DRL-tekst (pre-fix, LF): FAIL — attribuut afslag: verwacht afslag\nnoord, werkelijk afslagnoord
        //     Drools DRL-tekst (pre-fix, CR): FAIL — attribuut afslag: verwacht uit\rvoer, werkelijk uitvoer
        // — a wrong value with no complaint, while the exec-model branch played both. BOTH terminators are asserted
        // because they are two separate substitutions in that helper and a witness on one leaves the other free.
        // Measured on the fixed jar (each document, its own case):
        //     verifier: PASS
        //     ref     : oracle: PASS (3 expectations)
        //     clingo  : TRANSLATION FAULT — the ASP string encoding cannot quote a line terminator, so clingo
        //               REFUSES this flank loudly and ref is the arbiter here (measured the same day).
        for (String lineEnd : List.of("\n", "\r")) {
            String waarde = "uit" + lineEnd + "voer";
            String consequence = "afslag" + lineEnd + "noord";
            Path ir = model("tekstlexeem-regeleinde", "equal", waarde, consequence);
            playBothBranches(ir, caseOf("regeleinde-raakt", waarde, true, consequence));
        }
    }

    @Test void a_line_end_carrying_literal_does_not_touch_the_flattened_case_value() throws Exception {
        // The mirror direction, pinned for the same reason as the quote mirror above: no branch ever got this one
        // wrong — pre-fix the constraint side kept the line break, so this document already PLAYED in both — and it
        // is here so the escaping can never become a NORMALISATION. A helper that dropped or collapsed line
        // terminators instead of escaping them would satisfy the witness above (both branches losing the character
        // agree with each other) and fire this rule the wire does not license. Measured on the fixed jar:
        //     verifier: PASS
        //     ref     : oracle: PASS (3 expectations)
        Path ir = model("tekstlexeem-regeleinde", "equal", LINE_END, CONSEQUENCE_LINE_END);
        playBothBranches(ir, caseOf("platgeslagen-invoer", FLATTENED, false, null));
    }

    @Test void a_value_that_ends_with_a_backslash_plays_in_both_branches() throws Exception {
        // The flank the escaping of the destructive DRL-text lowering did NOT rescue: there the doubled backslash made
        // the DRL consequence pre-processor read the closing quote as escaped and run off the end of the block, a
        // per-form compile refusal ("drl compile: … Unable to build expression for 'consequence' …"), measured on
        // 2026-09-18 beside exec-model PLAYED 1 and verifier, ref and clingo PASS (3 expectations). Since card
        // drl-teksttak-op-truth-maintenance the text renders the analysis, whose comparison is a Java function of the
        // text with the lexeme as a Java string literal, and both branches play it. A backslash in the MIDDLE
        // (`pad\naar`, a real backslash) played in both branches before.
        playBothBranches(model("tekstlexeem-backslash", "equal", "\\", "\\"), caseOf("backslash-raakt", "\\", true,
                "\\"));
    }

    // ---------------- the two-branch assertion ----------------

    /** One document, one case, BOTH branches — the inequality this witness tree exists for is only visible when
     *  the two are asserted together, so no test here asks a single branch a question. */
    private static void playBothBranches(Path ir, Path c) {
        CaseRunner.Result exec = CaseRunner.viaExecModel(ir, List.of(c));
        assertEquals("PLAYED", exec.status(), exec.reason() + " " + exec.caseFailures());
        CaseRunner.Result drl = CaseRunner.viaDrlText(ir, List.of(c));
        assertEquals("PLAYED", drl.status(), drl.reason() + " " + drl.caseFailures());
    }

    // ---------------- fixtures ----------------

    private static Path ir(String golden) {
        Path p = CORPUS.resolve(golden).resolve("ir.json");
        assertTrue(Files.exists(p), "golden ir.json present: " + p.toAbsolutePath());
        return p;
    }

    private static Path temporary(String naam, String inhoud) throws Exception {
        Path tmp = Files.createTempFile(naam, ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, inhoud);
        return tmp;
    }

    /** The one version of the golden's {@code i}-th rule. */
    private static ObjectNode regel(ObjectNode ir, int i) { return IrPath.version(ir, "rules", i, 0); }

    /** The golden's OWN `het keurmerk van de Vondst` reference, so every fixture asks about the same member and the
     *  only thing that varies between them is the literal's lexeme. */
    private static ObjectNode keurmerkRef(ObjectNode ir) {
        return ((ObjectNode) regel(ir, 0).get("condition").get("left")).deepCopy();
    }

    private static ObjectNode textLiteral(String waarde) {
        ObjectNode lit = JSON.createObjectNode();
        lit.put("node", "Literal");
        lit.putObject("value").put("text", waarde);
        lit.set("type", JSON.createObjectNode().put("node", "Text"));
        lit.set("source", IrPath.sourceReference(waarde));
        return lit;
    }

    private static ObjectNode keurmerkComparison(ObjectNode ir, String op, String literal) {
        ObjectNode v = JSON.createObjectNode();
        v.put("node", "Comparison");
        v.put("op", op);
        v.set("left", keurmerkRef(ir));
        v.set("right", textLiteral(literal));
        v.set("source", IrPath.sourceReference("haar keurmerk"));
        return v;
    }

    /** jutterswet-keurmerk with its condition replaced by ONE keurmerk comparison and its consequence writing
     *  {@code consequence} — the two-rule chain (compare, then write) stays exactly the golden's. */
    private static ObjectNode base(String op, String literal, String consequence) throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(ir(KEURMERK)));
        regel(ir, 0).set("condition", keurmerkComparison(ir, op, literal));
        ((ObjectNode) regel(ir, 1).get("value")).putObject("value").put("text", consequence);
        ((ObjectNode) regel(ir, 1).get("value")).set("source", IrPath.sourceReference(consequence));
        return ir;
    }

    private static Path model(String naam, String op, String literal, String consequence) throws Exception {
        return temporary(naam, base(op, literal, consequence).toString());
    }

    /** The same model with the condition widened to a ch. 12 OR-row: TEN_MINSTE 1 of N "is gelijk aan X" cells. */
    private static Path orRow(String naam, String... literals) throws Exception {
        ObjectNode ir = base("equal", literals[0], CONSEQUENCE);
        ObjectNode sam = JSON.createObjectNode();
        sam.put("node", "Compound");
        sam.put("quantifier", "atLeast");
        sam.put("threshold", 1);
        ArrayNode parts = sam.putArray("parts");
        for (String l : literals) parts.add(keurmerkComparison(ir, "equal", l));
        sam.set("source", IrPath.sourceReference("aan ten minste een van de volgende voorwaarden wordt voldaan"));
        regel(ir, 0).set("condition", sam);
        return temporary(naam, ir.toString());
    }

    /** One Vondst whose keurmerk is {@code invoer}, with the two expectations the rule chain produces: the kenmerk
     *  the comparison assigns, and the afslag the second rule then writes (leeg when the chain does not fire). */
    private static Path caseOf(String naam, String invoer, boolean gekeurd, String afslag) throws Exception {
        ObjectNode c = JSON.createObjectNode();
        c.put("format", "uchar-case-v0");
        c.putNull("asOfDate");
        ObjectNode inputNode = c.putObject("input");
        ObjectNode v1 = inputNode.putArray("instances").addObject();
        v1.put("label", "V1");
        v1.put("objectType", "Vondst");
        v1.putObject("attributes").putObject("keurmerk").put("text", invoer);
        v1.putObject("roles");
        v1.putArray("characteristics");
        inputNode.putObject("parameters");
        ArrayNode verwacht = c.putArray("expected");
        verwacht.addObject().put("kind", gekeurd ? "characteristicPresent" : "characteristicAbsent")
                .put("label", "V1").put("characteristic", "gekeurd");
        if (afslag == null) {
            verwacht.addObject().put("kind", "attributeEmpty").put("label", "V1").put("attribute", "afslag");
        } else {
            ObjectNode e = verwacht.addObject();
            e.put("kind", "attributeValue").put("label", "V1").put("attribute", "afslag");
            e.putObject("value").put("text", afslag);
        }
        verwacht.addObject().put("kind", "consistent");
        return temporary(naam, c.toString());
    }
}
