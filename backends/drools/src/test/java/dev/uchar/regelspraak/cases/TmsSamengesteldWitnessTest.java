package dev.uchar.regelspraak.cases;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.samengesteld;
import static dev.uchar.regelspraak.cases.TmsDocumenten.verwacht;
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
class TmsSamengesteldWitnessTest {

    private static final List<String> KENMERKEN = List.of("erkend", "gemeld", "geschorst", "vergund", "ingedeeld");

    private static final List<String> LEDEN = List.of(check("gemeld", false), check("geschorst", true),
            check("vergund", false));

    @Test void alle() throws Exception { speel(samengesteld("all", 0, LEDEN), n -> n == 3); }

    @Test void de() throws Exception { speel(samengesteld("the", 0, LEDEN), n -> n == 3); }

    @Test void geen() throws Exception { speel(samengesteld("none", 0, LEDEN), n -> n == 0); }

    @Test void ten_minste_twee() throws Exception { speel(samengesteld("atLeast", 2, LEDEN), n -> n >= 2); }

    @Test void ten_hoogste_een() throws Exception { speel(samengesteld("atMost", 1, LEDEN), n -> n <= 1); }

    @Test void precies_twee() throws Exception { speel(samengesteld("exactly", 2, LEDEN), n -> n == 2); }

    /** GEEN of (ALLE of gemeld and vergund) and geschorst: neither both, nor geschorst. */
    @Test void genest() throws Exception {
        String voorwaarde = samengesteld("none", 0, List.of(
                samengesteld("all", 0, List.of(check("gemeld", false), check("vergund", false))),
                check("geschorst", false)));
        List<String> jutters = new ArrayList<>(), verwacht = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            boolean erkend = (i & 1) != 0, gemeld = (i & 2) != 0, geschorst = (i & 4) != 0;
            jutters.add(jutter(i, erkend, gemeld, geschorst));
            verwacht.add(verwacht("J" + i, "ingedeeld", !(gemeld && erkend) && !geschorst));
        }
        speelDocument(voorwaarde, jutters, verwacht);
    }

    /** The document with {@code voorwaarde} over the eight Jutters, ingedeeld where {@code oordeel} holds of the number
     *  of the three members that hold. */
    private static void speel(String voorwaarde, IntPredicate oordeel) throws Exception {
        List<String> jutters = new ArrayList<>(), verwacht = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            boolean erkend = (i & 1) != 0, gemeld = (i & 2) != 0, geschorst = (i & 4) != 0;
            int raak = (gemeld ? 1 : 0) + (geschorst ? 0 : 1) + (erkend ? 1 : 0);
            jutters.add(jutter(i, erkend, gemeld, geschorst));
            verwacht.add(verwacht("J" + i, "vergund", erkend));
            verwacht.add(verwacht("J" + i, "ingedeeld", oordeel.test(raak)));
        }
        speelDocument(voorwaarde, jutters, verwacht);
    }

    private static void speelDocument(String voorwaarde, List<String> jutters, List<String> verwacht) throws Exception {
        Path ir = TmsDocumenten.document(TmsDocumenten.model(KENMERKEN), List.of(
                kenmerkregel("vergund indien erkend", "vergund", check("erkend", false)),
                kenmerkregel("ingedeeld", "ingedeeld", voorwaarde)));
        var zaak = TmsDocumenten.zaak(jutters, verwacht);
        assertNull(CaseRunner.speelCase(ir, zaak, DroolsBackend.Branch.EXEC_MODEL));
    }

    private static String jutter(int i, boolean erkend, boolean gemeld, boolean geschorst) {
        List<String> k = new ArrayList<>();
        if (erkend) k.add("erkend");
        if (gemeld) k.add("gemeld");
        if (geschorst) k.add("geschorst");
        return TmsDocumenten.jutter("J" + i, k);
    }
}
