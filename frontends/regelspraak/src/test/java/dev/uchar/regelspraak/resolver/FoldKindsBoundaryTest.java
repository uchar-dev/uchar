package dev.uchar.regelspraak.resolver;

import dev.uchar.regelspraak.ir.Expr;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * One boundary, two languages: {@code RuleChecker.FOLD_KINDS} and the uchar verifier's
 * {@code value_fold_name} MUST admit the same kinds, because they are the same rule — the position
 * where a set reading is legitimate (WF-096's exemption until L2, WF-093's discriminator since). The
 * frontend's copy decides what reaches the wire; the verifier's decides what the wire accepts. A
 * frontend fence that is WIDER emits documents the verifier rejects; a NARROWER one refuses sentences
 * the wire allows.
 *
 * <p><b>Both failures have now happened, one from each side, which is why this reads the source
 * instead of pinning a list.</b> On 2026-09-19 the frontend was wider by {@code AANTAL} and emitted
 * ill-formed documents with {@code diagnostics: []}
 * ({@link dev.uchar.regelspraak.CountSetSourceTest}). On 2026-09-20 the verifier grew
 * {@code AANTAL_GEVULDE} with the wire and the frontend stayed at six for two cards; nothing measured
 * it, because no frontend could write the kind yet. A comment claiming the two agree is exactly what
 * was there both times.
 *
 * <p><b>What it reads and why that is the right anchor.</b> Since L2 of the v1 wire migration the wire
 * has no set flag and WF-096 is withdrawn; its counting half is WF-093's (IR-SPEC ch. 4.3), and the verifier draws
 * the boundary in {@code source_reading}: a ranged source under {@code Aantal} is read as the linked
 * instances, under every other aggregation kind as a value per contributor, and {@code value_fold_name}
 * names exactly those other kinds. This test pins both halves: the arm list of that function (the
 * kinds), and the split itself — the two {@code Aantal} arms of {@code source_reading} and the
 * aggregation kinds they split. Pinning only the list would miss a boundary redrawn on some other set;
 * pinning only the split would miss a kind added or removed.
 *
 * <p>The verifier source is found by walking up from the working directory. A missing file FAILS —
 * a test that skips when its subject is absent is the silent green this repository's gates exist to
 * prevent.
 */
class FoldKindsBoundaryTest {

    private static final String CHECKS = "spec/verifier/lib/checks.ml";

    /** A constructor of the verifier's AST, which carries the wire names (card rename-ocaml); the frontend's kinds keep
     *  the RegelSpraak words, so {@link #kind} maps one to the other. */
    private static final Pattern CONSTRUCTOR = Pattern.compile("\\b[A-Z][A-Za-z0-9_]*\\b");

    private static final java.util.Map<String, String> VERIFIER_KINDS = java.util.Map.of(
            "SUM_FUNCTION", "SOM", "TOTAL", "TOTAAL", "COUNT", "AANTAL", "MINIMUM", "MINIMUM", "MAXIMUM", "MAXIMUM",
            "FIRST", "EERSTE", "LAST", "LAATSTE", "FILLED_COUNT", "AANTAL_GEVULDE");

    /** The frontend's kind name of a verifier constructor; an unmapped constructor FAILS, so a kind added on the
     *  verifier's side cannot pass unseen. */
    private static String kind(String constructor) {
        String name = VERIFIER_KINDS.get(constructor.toUpperCase(java.util.Locale.ROOT));
        if (name == null) fail("the verifier's constructor " + constructor + " has no frontend kind in VERIFIER_KINDS");
        return name;
    }

    /** The two arms of {@code source_reading} that read a ranged source under the COUNT kind as instances;
     *  every other aggregation kind falls to the value arms below them. Whitespace-tolerant. */
    private static final Pattern COUNT_ARM_MEMBER = Pattern.compile("\\|\\s*Count\\s*,\\s*true\\s*->\\s*Src_count_of_member");
    private static final Pattern COUNT_ARM_ROLE = Pattern.compile("\\|\\s*Count\\s*,\\s*false\\s*->\\s*Src_count\\b");

    /** The verifier's list of aggregation kinds, the set {@code source_reading} splits. */
    private static final Pattern AGGREGATION_KINDS = Pattern.compile("let aggregation_kinds\\s*=\\s*\\[([^\\]]*)\\]");

    private static Path verifierSource() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
            Path candidate = p.resolve(CHECKS);
            if (Files.isRegularFile(candidate)) return candidate;
        }
        return fail("kon " + CHECKS + " niet vinden vanaf " + Path.of("").toAbsolutePath()
                + " — deze getuige LEEST de verifiergrens en mag niet overslaan als hij hem niet vindt");
    }

    @Test void the_frontend_boundary_carries_exactly_the_value_folds_of_the_verifier() throws IOException {
        String source = Files.readString(verifierSource(), StandardCharsets.UTF_8);
        int start = source.indexOf("let value_fold_name = function");
        assertTrue(start >= 0, () -> "value_fold_name staat niet meer in " + CHECKS
                + " — de grens is verplaatst en deze getuige moet mee verhuizen");

        // The kinds are the constructors in the pattern of every arm that returns Some, one constructor per arm or
        // several in an or-pattern. The arms are read by their constructors, not by the strings they return: those
        // spell the kind as the verifier's messages do, which follows the wire (lowerCamel since the L5 landing).
        String function = source.substring(start + "let value_fold_name = function".length());
        int end = function.indexOf("\n\n");
        assertTrue(end >= 0, "value_fold_name heeft geen einde (een lege regel) meer — de getuige leest haar niet");
        List<String> kinds = new ArrayList<>();
        for (String arm : function.substring(0, end).split("\n\\s*\\|")) {    // an arm opens a line with '|'
            int arrow = arm.indexOf("->");
            if (arrow < 0 || !arm.substring(arrow + 2).strip().startsWith("Some")) continue;   // the None catch-all
            Matcher m = CONSTRUCTOR.matcher(arm.substring(0, arrow).replaceAll("\\bas\\s+\\w+", ""));
            while (m.find()) kinds.add(kind(m.group()));
        }
        assertFalse(kinds.isEmpty(), "geen enkele arm gelezen — het patroon sluit niet meer aan op de bron");

        List<String> frontend = RuleChecker.FOLD_KINDS.stream().map(Expr.FunctionKind::name).sorted().toList();
        assertEquals(kinds.stream().sorted().toList(), frontend,
                "VOUWSOORTEN en de verifier's value_fold_name lopen uiteen — dit is één grens in "
                + "twee talen, dus een soort erbij of eraf hoort in dezelfde beweging aan beide kanten");
    }

    @Test void the_verifier_separates_the_count_of_exactly_that_list() throws IOException {
        // The split itself, so a boundary silently redrawn on another set cannot pass the check above by
        // leaving value_fold_name untouched: source_reading keys the instance reading on Aantal alone, and
        // the aggregation kinds without Aantal are exactly the frontend's fold kinds.
        String source = Files.readString(verifierSource(), StandardCharsets.UTF_8);
        assertTrue(COUNT_ARM_MEMBER.matcher(source).find() && COUNT_ARM_ROLE.matcher(source).find(),
                "source_reading keyt de instantielezing niet meer op `Aantal` — de grens is elders getrokken "
                + "en de frontendkopie hierboven bewijst sindsdien niets meer");
        Matcher m = AGGREGATION_KINDS.matcher(source);
        assertTrue(m.find(), "aggregation_kinds staat niet meer in " + CHECKS);
        List<String> valueFolds = new ArrayList<>();
        for (String category : m.group(1).split(";")) {
            String kindName = category.strip();
            if (!kindName.isEmpty() && !kindName.equals("Count")) valueFolds.add(kind(kindName));
        }
        assertEquals(valueFolds.stream().sorted().toList(),
                RuleChecker.FOLD_KINDS.stream().map(Expr.FunctionKind::name).sorted().toList(),
                "de aggregatiesoorten zonder AANTAL zijn de soorten die de verifier per bijdrager als waarde "
                + "leest, en dus precies VOUWSOORTEN");
    }

    @Test void the_boundary_counts_seven_kinds() {
        // A floor-and-ceiling on the frontend half alone, so a change that edits BOTH sides in one
        // sweep still comes past a human: the count is the number the RuleChecker doc, the setlezing
        // diagnostic and RUNTIME-SEMANTICS ch. 3.4c all spell out.
        assertEquals(7, RuleChecker.FOLD_KINDS.size(),
                () -> "de waardevouwen zijn " + RuleChecker.FOLD_KINDS + " — was dat de bedoeling, werk dan "
                        + "ook de zeven-tellingen in RuleChecker en in de setlezing-melding bij");
        assertTrue(RuleChecker.FOLD_KINDS.contains(Expr.FunctionKind.AANTAL_GEVULDE),
                "de gevulde telling leest een waarde per bijdrager en hoort dus in de exemptie (ch. 3.4c)");
        assertFalse(RuleChecker.FOLD_KINDS.contains(Expr.FunctionKind.AANTAL),
                "de instantietelling vouwt de leden zelf en hoort er NIET in (besluit 2026-09-19)");
    }
}
