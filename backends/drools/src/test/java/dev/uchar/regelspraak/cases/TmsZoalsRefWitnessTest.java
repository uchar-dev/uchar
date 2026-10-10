package dev.uchar.regelspraak.cases;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The documents on which the destructive exec-model branch refused a form or answered otherwise than the reference
 * interpreter, and which this branch plays as the reference interpreter answers. Each lies under
 * {@code src/test/resources/zoals-ref/<name>/} with a case whose expectations the reference interpreter passes; this
 * witness replays every case through the branch and demands the reference interpreter's values, so the switch-over of
 * the exec-model branch to this one keeps them. The documents are those of witnesses of the destructive branch, which
 * pin its refusal or its answer; they are listed with that witness, the form, and what the destructive branch gave.
 *
 * <ul>
 * <li>{@code begrenzing-deling-in-maximum} and {@code begrenzing-nuldeling-in-maximum} ({@code CompanionPadWitnessTest}):
 * "Let d = deler in begrensd(inkomen, minimum d, maximum inkomen ÷ tweede)" and the same with "÷ nul". A clamp evaluates
 * its base, its minimum and its maximum in that order (RUNTIME-SEMANTICS ch. 2.7), so the minimum's leeg read ends the
 * Let before the maximum divides: uit leeg, consistent. The destructive branch evaluated the maximum first: inconsistent,
 * and "DelingDoorNul … 10 gedeeld door 0". The witness's documents type the clamp with 4 decimals over a base of 2,
 * which the verifier refuses (WF-091); these carry the clamp's own type, 2 decimals. Clingo declines a clamp bound that
 * is no literal.
 * <li>{@code let-telling-deling} ({@code DatumvouwOverRolWitnessTest}): "de vangst gedeeld door zijn gewicht. Daarbij
 * geldt: de vangst is het aantal vondsten van de Jutter", with a gewicht per jutter in this case: 2.00, 0.00, 0.25,
 * leeg with a deposit. The destructive branch refused a fold and a division under one Let.
 * <li>{@code defaults-kwantorvouw-rechtvaardiging} ({@code DefaultsRechtvaardigingWitnessTest}): a quantifier fold in the
 * justification of a default; the destructive branch refused it.
 * <li>{@code dubbelmeervoudige-zelfrelatie} ({@code OpslagrichtingWitnessTest}): a count over a self-relation with both
 * roles plural, the golden {@code anchor/jutterswet-erflijn} with a case of that witness; the destructive branch refused
 * the join, which the bean model holds on no field.
 * <li>{@code feitcreatie-naamdelende-schrijfrol} ({@code RolveldlezerWitnessTest}): a fact creation whose written role
 * shares its name with a declared member; the destructive branch refused it.
 * <li>{@code filtervouw-ten-hoogste-1}, {@code filtervouw-ten-minste-2}, {@code filtervouw-geen-over-alle} and
 * {@code filtervouw-andere-kwantor-1} to {@code -5} ({@code FilterfoutWitnessTest}): a fold whose subselection divides
 * and can fault, as a member of a composite condition that the short circuit of ch. 5.10 can settle before the fold; the
 * destructive branch refused every composite other than "alle" with such a fold.
 * <li>{@code geneste-vouw-als-matrix} ({@code NietTemporeleKwantorvouwWitnessTest}), {@code matrix-tekstparameter} and
 * {@code matrix-twee-parameters} ({@code VouwMatrixVormWitnessTest}): a quantifier fold whose matrix is itself a fold,
 * compares with a text parameter, or reads two parameters; the destructive branch refused them. Clingo declines the text
 * parameter.
 * <li>{@code heelperiode-filter-dat-hijst}, {@code heelperiode-niet-conjunctieve-kwantor} and
 * {@code heelperiode-niet-scalaire-grens} ({@code HeelperiodeKwantorvouwWitnessTest}): "gedurende het gehele jaar" as the
 * matrix of a quantifier fold with a subselection, under a quantifier other than "alle", and against a bound the element
 * reads itself; the destructive branch refused them. The third case expects zeewaardig absent, the reference
 * interpreter's answer, where the witness's own case expected it present; clingo declines that bound.
 * <li>{@code let-kruiseenheid} ({@code LetDragerWandelingWitnessTest}): "Let a = aanloop in (als a groter dan 1 km dan
 * 1,00 anders 2,00)" with an aanloop of 500 m: 2.00, the comparison restated (ch. 2.6). The destructive branch refused
 * the rule. Clingo answers 1.00, an existing fault of the ASP backend, reported with the card's second step.
 * <li>{@code literaal-deeltal-1} and {@code -2} ({@code LetMeldplekWitnessTest}): "100,00 gedeeld door restpost" with
 * the restpost absent, with and without a Let: uit leeg and the §9.5 deposit, inconsistent. The destructive branch made
 * no deposit for a literal dividend and answered consistent.
 * <li>{@code rekenjaar-in-consistentie} ({@code RekenjaarWitnessTest}): the calculation year in the assertion of a
 * consistency; the destructive branch refused it.
 * <li>{@code unieselectie-via-rol} ({@code VouwfilterVerreKantWitnessTest}): the subselection of an instance-set union
 * that reads through a role of the element; the destructive branch refused it.
 * <li>{@code vouw-onder-let-naast-deling} ({@code VouwBegeleiderWitnessTest}): a fold and a division under one Let; the
 * destructive branch refused it.
 * </ul>
 *
 * <p><b>Measured</b> 2026-10-10 with the verifier, the reference interpreter, the ASP runner, the jar of the claim of
 * the card (331b1179, no flag and {@code --tms}) and this branch: the verifier passes every document, the reference
 * interpreter passes every case, the ASP runner every case but the five named above, and this branch plays every case.
 * The destructive branch refuses 22 documents and answers four otherwise, as listed.
 */
class TmsZoalsRefWitnessTest {

    // cwd under surefire is the module dir (backends/drools)
    private static final Path MAP = Path.of("src", "test", "resources", "zoals-ref");

    /** The number of documents under {@link #MAP}: a document that disappears must say so here. */
    private static final int AANTAL = 26;

    @Test void elk_document_speelt_zoals_ref() throws Exception {
        List<String> afwijkend = new ArrayList<>();
        List<Path> mappen;
        try (var lijst = Files.list(MAP)) {
            mappen = lijst.filter(Files::isDirectory).sorted().toList();
        }
        for (Path m : mappen) {
            String uitkomst;
            // every document is replayed, so a failure names each one that deviates, not only the first
            try {
                uitkomst = CaseRunner.speelCase(m.resolve("ir.json"),
                        TmsDocumenten.JSON.readTree(Files.readString(m.resolve("case.json"))),
                        DroolsBackend.Branch.TRUTH_MAINTENANCE);
            } catch (RuntimeException e) {
                uitkomst = e.getClass().getSimpleName() + ": " + e.getMessage();
            }
            if (uitkomst != null) afwijkend.add(m.getFileName() + ": " + uitkomst);
        }
        assertEquals(List.of(), afwijkend);
        assertEquals(AANTAL, mappen.size(), "documents under " + MAP);
    }
}
