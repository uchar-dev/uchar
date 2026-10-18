package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A non-terminating exact fraction at rest, through both branches (card drl-teksttak-op-truth-maintenance, step 6b;
 * the owner's decision on norm question 3 of Fable advice 4): "het quotient van een Deling moet berekend worden als
 * zijn teller gedeeld door zijn noemer", 1 divided by 3 into an attribute in the unbounded form "getal"
 * ({@code whole: false}, {@code decimals: 0}). The division gives the fraction 1/3, which no decimal holds exactly.
 * Before the value comes to rest in a fact, the plan collapses it ({@code collapse} of a value consequence,
 * {@code ComputeHelpers.collapse}): a fraction that terminates becomes its decimal, and one that does not becomes
 * {@code Rational.naarDecimal()}, cut off at {@code DECIMAL64}. Until this witness no case of the corpus and no witness
 * brought a non-terminating fraction at rest through either branch, so a DRL rendering that ignored the collapse of the
 * plan (mutation W4 of Fable advice 4) and a plan without it (A1) passed every measurement of behaviour; only the pins
 * of the text and of the plan saw them.
 *
 * <p>The norm is the exact value: an unbounded target is exempt from the assignment gate, and 1/3 written into it is
 * 1/3 (RUNTIME-SEMANTICS ch. 2.4, point 3 and its worked example of the unbounded form). Of the three executors only
 * the reference interpreter writes it. The other two have a limit of their own, which the same chapter records after
 * that worked example; neither limit is the norm:
 * <ul>
 *   <li>the reference interpreter writes the exact value 1/3 and plays the case, whose expectation is 1/3;</li>
 *   <li>the ASP backend holds the unbounded form as a whole number, its recorded limit, and refuses a written result
 *       that is not whole by name ({@code ASP-TRANSLATION.md} S2, S19): "a division into a number declared without
 *       decimals is not whole, and this encoding holds the unbounded form as a whole number";</li>
 *   <li>the Drools backend carries a decimal on the unbounded form in both branches, its recorded limit,
 *       0.3333333333333333, and refuses the case value written as the rational 1/3 by name: "rational value on
 *       'quotient' is not carried by the v0 backend"; with the expectation 0.3333333333333333 both branches play the
 *       case.</li>
 * </ul>
 * The square does not hold, so the document is no golden: it stays in the test resources ({@code breuk/een-derde}).
 * Measured 2026-10-14 on {@code b53713ed}; the raw output of the verifier (PASS), the reference interpreter and the
 * ASP runner is kept with step 6b of the card ({@code breuk-ruw.txt}). The bounded form, the same quotient into four
 * decimals, is the red case {@code decimal-nonterminating}, which gate 8 lists for this backend as a NOFAULT: there
 * the spec asks for a rule (the v0 mirror note of ch. 2.4), not for a change in a branch.
 *
 * <p>The witness fires the case on both branches, demands the same verdict and the same dump ({@link BranchDumps}), and
 * pins the value in the facts at the fixpoint as both branches hold it, and what {@code DroolsBackend.lees} gives. The
 * pins guard the recorded limit of this backend, so that a change to it shows; they do not make the approximation the
 * norm, which stays the exact 1/3 of ch. 2.4.
 *
 * <p><b>Red</b> with two mutations of Fable advice 4, each in a copy (not committed; the diffs and the output are kept
 * with step 6b of the card): W4, a DRL rendering that ignores the collapse of the plan, leaves a {@code Breuk} in the
 * facts of the DRL-text branch where the exec-model branch holds the decimal, so the two dumps differ and the
 * write-back of the DRL-text branch fails on the {@code Breuk} ("ClassCastException: Cannot cast … Breuk to
 * java.math.BigDecimal"), the verdict {@link BranchDumps} compares first; A1, a plan without the collapse, leaves the two
 * branches alike and fails the pin of the value in the facts.
 */
class RationalAtRestWitnessTest {

    // cwd under surefire is the module dir (backends/drools)
    private static final Path DOCUMENT = Path.of("src", "test", "resources", "rational", "one-third");

    @Test void a_third_at_rest_is_the_same_decimal_on_both_branches() throws Exception {
        Path ir = DOCUMENT.resolve("ir.json");
        JsonNode caseDocument = TmsDocuments.JSON.readTree(Files.readString(DOCUMENT.resolve("case.json")));
        String dump = BranchDumps.equal(ir, caseDocument, "een derde");
        // the decimal of 1/3 at DECIMAL64, sixteen significant digits, with its scale: the BigDecimal of the collapse,
        // the recorded limit of this backend, not the exact value the norm asks for (ch. 2.4)
        assertEquals(List.of("feit Value(onderwerp=\"D1\", attribuut=\"deling\".\"quotient\","
                        + " waarde=BigDecimal(0.3333333333333333), writer=String(\"quotient van een deling\"))"),
                rules(dump, "feit ", "0.3333", "Breuk"), () -> "de waarde in de feiten:\n" + dump);
        assertEquals(List.of("lees waarde \"D1\".\"quotient\" = BigDecimal(0.3333333333333333)"),
                rules(dump, "lees ", "quotient", "quotient"), () -> "lees:\n" + dump);
        // the expectation pins the norm's exact 1/3, which this backend refuses by name on both branches, its recorded
        // limit (ch. 2.4)
        assertEquals("Excluded: rational value on 'quotient' is not carried by the v0 backend",
                TmsFaultWitnessTest.play(ir, caseDocument, DroolsBackend.Branch.EXEC_MODEL),
                "het oordeel over de verwachting 1/3");
    }

    /** The lines of {@code dump} that start with {@code section} and hold {@code a} or {@code b}, in their order. */
    private static List<String> rules(String dump, String section, String a, String b) {
        return Stream.of(dump.split("\n")).filter(r -> r.startsWith(section) && (r.contains(a) || r.contains(b)))
                .toList();
    }
}
