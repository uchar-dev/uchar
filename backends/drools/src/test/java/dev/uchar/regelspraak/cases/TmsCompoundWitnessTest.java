package dev.uchar.regelspraak.cases;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.compound;
import static dev.uchar.regelspraak.cases.TmsDocuments.verwacht;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A composite condition on the exec-model branch, under each quantifier, read as a predicate over bound values:
 * "Een Jutter is ingedeeld indien hij aan Q van de volgende voorwaarden voldoet: hij is gemeld, hij is niet geschorst, hij
 * is vergund", with gemeld and geschorst case input, read off the bean, and vergund derived ("Een Jutter is vergund
 * indien hij erkend is"), read through its standing. Eight Jutters cover every combination of erkend, gemeld and
 * geschorst, and the expected verdict is computed here from the three members, apart from the branch: ALLE and DE
 * (all three), GEEN (none), TEN MINSTE 2, TEN HOOGSTE 1, PRECIES 2, and a nested composite, GEEN of (ALLE of gemeld and
 * vergund) and geschorst.
 *
 * <p><b>Red before the lowering</b> (measured 2026-10-08). On the branch's skeleton every row was refused, "tms-transpiler
 * SKIP: regel: vergund indien erkend — de verlaging van een kenmerktoekenning is nog niet gebouwd"; with the lowering in
 * place and the write-back to the beans switched off, every row answered on truth maintenance with lines such as "case:
 * kenmerk vergund: verwacht aanwezig, werkelijk afwezig; kenmerk ingedeeld: verwacht aanwezig, werkelijk afwezig; …".
 */
class TmsCompoundWitnessTest {

    private static final List<String> CHARACTERISTICS = List.of("erkend", "gemeld", "geschorst", "vergund", "ingedeeld");

    private static final List<String> MEMBERS = List.of(check("gemeld", false), check("geschorst", true),
            check("vergund", false));

    @Test void all() throws Exception { play(compound("all", 0, MEMBERS), n -> n == 3); }

    @Test void the() throws Exception { play(compound("the", 0, MEMBERS), n -> n == 3); }

    @Test void none() throws Exception { play(compound("none", 0, MEMBERS), n -> n == 0); }

    @Test void at_least_two() throws Exception { play(compound("atLeast", 2, MEMBERS), n -> n >= 2); }

    @Test void at_most_one() throws Exception { play(compound("atMost", 1, MEMBERS), n -> n <= 1); }

    @Test void exactly_two() throws Exception { play(compound("exactly", 2, MEMBERS), n -> n == 2); }

    /** GEEN of (ALLE of gemeld and vergund) and geschorst: neither both, nor geschorst. */
    @Test void nested() throws Exception {
        String voorwaarde = compound("none", 0, List.of(
                compound("all", 0, List.of(check("gemeld", false), check("vergund", false))),
                check("geschorst", false)));
        List<String> jutters = new ArrayList<>(), verwacht = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            boolean erkend = (i & 1) != 0, reported = (i & 2) != 0, geschorst = (i & 4) != 0;
            jutters.add(jutter(i, erkend, reported, geschorst));
            verwacht.add(verwacht("J" + i, "ingedeeld", !(reported && erkend) && !geschorst));
        }
        playDocument(voorwaarde, jutters, verwacht);
    }

    /** The document with {@code voorwaarde} over the eight Jutters, ingedeeld where {@code oordeel} holds of the number
     *  of the three members that hold. */
    private static void play(String voorwaarde, IntPredicate verdict) throws Exception {
        List<String> jutters = new ArrayList<>(), verwacht = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            boolean erkend = (i & 1) != 0, reported = (i & 2) != 0, geschorst = (i & 4) != 0;
            int touch = (reported ? 1 : 0) + (geschorst ? 0 : 1) + (erkend ? 1 : 0);
            jutters.add(jutter(i, erkend, reported, geschorst));
            verwacht.add(verwacht("J" + i, "vergund", erkend));
            verwacht.add(verwacht("J" + i, "ingedeeld", verdict.test(touch)));
        }
        playDocument(voorwaarde, jutters, verwacht);
    }

    private static void playDocument(String voorwaarde, List<String> jutters, List<String> verwacht) throws Exception {
        Path ir = TmsDocuments.document(TmsDocuments.model(CHARACTERISTICS), List.of(
                characteristicRule("vergund indien erkend", "vergund", check("erkend", false)),
                characteristicRule("ingedeeld", "ingedeeld", voorwaarde)));
        var caseDocument = TmsDocuments.caseDocument(jutters, verwacht);
        assertNull(CaseRunner.playCase(ir, caseDocument, DroolsBackend.Branch.EXEC_MODEL));
    }

    private static String jutter(int i, boolean erkend, boolean reported, boolean geschorst) {
        List<String> k = new ArrayList<>();
        if (erkend) k.add("erkend");
        if (reported) k.add("gemeld");
        if (geschorst) k.add("geschorst");
        return TmsDocuments.jutter("J" + i, k);
    }
}
