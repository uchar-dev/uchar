package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.loader.IrLoader;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ch. 4.3 {@code sourceRole} witness for the ch. 3.9a QUANTIFIER FOLD — the fourth seam that resolves a fold's
 * own range, after the instance fold, the §5.8.2 temporal set fold and the §13.4.6 creation back-link.
 *
 * <p>RED FIRST, measured on the jar from the commit before the fix, on {@code jutterswet-keurploeg} with ONE
 * extra feittype over the very same (Keurploeg, Keurmeester) pair and nothing else changed:
 * <pre>
 *   uniek paar     exec PLAYED 7   drl PLAYED 7        (7 = the golden's seven case files)
 *   herhaald paar  exec PLAYED 7   drl EXCLUDE drl-transpiler MARKER: regel: premiewaardig bij
 *                                  keurvergoeding — TODO §8.2 kwantorvouw (ALLE over Keurmeester) —
 *                                  geen verbindende rol tussen Keurploeg en Keurmeester
 * </pre>
 * The exec branch has read {@code kv.bronRol()} since the fold landed; this branch read the by-pair search
 * alone, which two feittypen make ambiguous by construction. Same wire, two answers — the same branch
 * inequality the som-over-bronrol card closed for the instance fold, still open here.
 *
 * <p>The rows below replay the golden's OWN seven cases, so what they pin is the corpus's hand-written
 * expectations (the quantifier verdicts and the control count), not a value invented here.
 *
 * <p>Every model is an in-memory variant of that golden; its files are never touched.
 */
class QuantifierFoldRangeWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** Keurploeg -- keurindeling -- Keurmeester, with two §8.2 whole-period quantifier folds over a
     *  die/dat-subselected range plus an unfiltered count as the control. */
    private static final String KEURPLOEG = "jutterswet-keurploeg";

    @Test void the_quantifier_fold_over_a_unique_pair_stays_playing() throws Exception {
        // The regression mirror: the golden as it stands, one feittype over the pair. Green before this card as
        // well; since L2 of the v1 wire migration the binding decides here too, as it does everywhere.
        Path p = temporary("kwantorvouw-uniek", lees());
        for (Path caseDocument : cases()) playBoth(p, caseDocument);
    }

    @Test void the_quantifier_fold_reads_the_wire_binding_on_a_repeated_pair() throws Exception {
        // THE ROW. A SECOND feittype relates Keurploeg to Keurmeester, so the by-pair search is ambiguous and
        // only the ch. 4.3 binding names the relation. All seven cases, both branches, the golden's own
        // expectations — the DRL-text branch used to refuse this document whole.
        Path p = temporary("kwantorvouw-herhaald", repeatedPair());
        for (Path caseDocument : cases()) playBoth(p, caseDocument);
    }

    @Test void the_quantifier_fold_without_binding_is_a_document_error() throws Exception {
        // THE GUARD AGAINST WIDENING THE FIX INTO A GUESS. Strip the range off the wire and two feittypen are
        // left with nothing to choose between; neither branch may pick a winner. Since L2 of the v1 wire
        // migration the range is required on this node (IR-SPEC ch. 4.5), so the stripped document is no
        // uchar-ir-v1 document at all: the loader refuses it at the fold, before either branch lowers a thing.
        ObjectNode ir = repeatedPair();
        for (JsonNode r : IrPath.rulesList(ir, "rules")) stripSourceRole(r);
        Path p = temporary("kwantorvouw-geen-binding", ir);
        JsonNode c = JSON.readTree(Files.readString(cases().get(1)));
        for (boolean viaDrl : new boolean[] { false, true }) {
            IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class,
                    () -> CaseRunner.playCase(p, c, viaDrl),
                    (viaDrl ? "drl-tekst" : "exec-model") + " never lowers a bindingless quantifier fold");
            assertTrue(e.getMessage().contains("a quantifier fold carries its range (sourceRole or sourcePath): the"
                    + " range is written, never derived"), e.getMessage());
        }
    }

    // ---------------- fixtures ----------------

    private static void playBoth(Path ir, Path caseDocument) throws Exception {
        BothBranches.green(ir, JSON.readTree(Files.readString(caseDocument)), caseDocument.getFileName().toString());
    }

    /** Every {@code sourceRole} under a rule, at whatever depth — the folds sit inside the rule's conditions. */
    private static void stripSourceRole(JsonNode n) {
        if (n instanceof ObjectNode o) {
            o.remove("sourceRole");
            o.forEach(QuantifierFoldRangeWitnessTest::stripSourceRole);
        } else if (n.isArray()) {
            n.forEach(QuantifierFoldRangeWitnessTest::stripSourceRole);
        }
    }

    private static ObjectNode lees() throws Exception {
        Path p = CORPUS.resolve(KEURPLOEG).resolve("ir.json");
        assertTrue(Files.exists(p), "golden ir.json present: " + p.toAbsolutePath());
        return (ObjectNode) JSON.readTree(Files.readString(p));
    }

    private static List<Path> cases() throws Exception {
        List<Path> out = new ArrayList<>();
        try (var s = Files.list(CORPUS.resolve(KEURPLOEG))) {
            s.filter(f -> f.getFileName().toString().startsWith("case")).sorted().forEach(out::add);
        }
        assertTrue(out.size() >= 7, "the golden's seven cases are there: " + out);
        return out;
    }

    /** The golden plus a SECOND feittype over the very same (Keurploeg, Keurmeester) pair, which is all it
     *  takes to make the by-pair search ambiguous while the wire binding stays exact. The case inputs never
     *  fill it, so the world the cases describe is unchanged. */
    private static ObjectNode repeatedPair() throws Exception {
        ObjectNode ir = lees();
        ObjectNode ft = IrPath.modelList(ir, "factTypes").addObject();
        ft.put("node", "FactType").put("name", "schaduwindeling").put("reciprocal", false);
        ft.putArray("roles")
          .add(rol("schaduwploeg", "Keurploeg", null, false))
          .add(rol("schaduwmeester", "Keurmeester", "schaduwmeesters", true));
        return ir;
    }

    private static ObjectNode rol(String naam, String objecttype, String plural, boolean many) {
        ObjectNode r = JSON.createObjectNode().put("node", "Role").put("name", naam)
                .put("objectType", objecttype);
        if (plural != null) r.put("plural", plural);
        return r.put("many", many);
    }

    private static Path temporary(String naam, ObjectNode ir) throws Exception {
        Path tmp = Files.createTempFile(naam, ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }
}
