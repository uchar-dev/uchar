package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.tms.Fout;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuut;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The dump of a session ({@link Sessiedump}) names every object by a name of its own, never by its identity, and
 * replaces nothing: a case label, the name of a rule and a text value stand quoted and escaped as they are. Two sessions
 * that differ only in such a name, one that looks like an identity, "Post@dead" against "Post@beef", get different
 * dumps, each with its name as it is; and a sentence of the branch that names an object by its identity is refused,
 * never written. Without that, the harness of card drl-teksttak-op-truth-maintenance saw two different facts as one
 * (lens on step 3a for a text value, lens on step 3b for a label and the name of a rule).
 *
 * <p><b>Red</b> on {@code d02ccea9}, before the fix of the text value: both dumps of the first test carried
 * {@code String("Post@?")}, and the two were equal. <b>Red</b> on {@code 55a24e61}, before the fix of the names: the
 * dumps of the two labels were equal, both naming the bean {@code Post@?}; the sections {@code lees} and {@code feit} of
 * the two rules were equal, both {@code Inconsistentie(Post@?, V1)}; and the dump wrote the sentence with the identity,
 * replaced by the label, where this class wants it refused.
 */
class SessiedumpWitnessTest {

    private static final String TEKST = "{\"node\":\"Text\"}";

    @Test void een_tekst_die_op_een_identiteit_lijkt_blijft_zoals_hij_is() throws Exception {
        String model = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst", List.of(attribuut("merk", TEKST)),
                List.of())), List.of());
        Path ir = TmsDocumenten.document(model, List.of());
        CaseRunner runner = CaseRunner.voorMeting(ir, DroolsBackend.Branch.EXEC_MODEL);
        String dead = dump(runner, vondst("V1", "Post@dead")), beef = dump(runner, vondst("V1", "Post@beef"));
        assertNotEquals(dead, beef, "twee teksten die alleen in de hex na de @ verschillen, kregen dezelfde dump:\n" + dead);
        assertTrue(dead.contains("String(\"Post@dead\")"), () -> "de dump citeert de tekst niet zoals hij is:\n" + dead);
        assertTrue(beef.contains("String(\"Post@beef\")"), () -> "de dump citeert de tekst niet zoals hij is:\n" + beef);
    }

    /** A case label is free text (the case schema), so it can look like an identity; the dump quotes it. */
    @Test void een_label_dat_op_een_identiteit_lijkt_blijft_zoals_het_is() throws Exception {
        String model = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst", List.of(attribuut("merk", TEKST)),
                List.of())), List.of());
        Path ir = TmsDocumenten.document(model, List.of());
        CaseRunner runner = CaseRunner.voorMeting(ir, DroolsBackend.Branch.EXEC_MODEL);
        String dead = dump(runner, vondst("Post@dead", "x")), beef = dump(runner, vondst("Post@beef", "x"));
        assertNotEquals(dead, beef, "twee labels die alleen in de hex na de @ verschillen, kregen dezelfde dump:\n" + dead);
        assertTrue(dead.contains("bean \"Post@dead\" Vondst("), () -> "de dump citeert het label niet:\n" + dead);
        assertTrue(beef.contains("bean \"Post@beef\" Vondst("), () -> "de dump citeert het label niet:\n" + beef);
    }

    /** The §9.5 inconsistency of a consistency rule whose name looks like an identity: what {@code lees} gives and the
     *  facts differ between the two names, each with its name quoted. */
    @Test void een_inconsistentie_onder_een_naam_die_op_een_identiteit_lijkt() throws Exception {
        String dead = leesEnFeiten(inconsistentie("Post@dead")), beef = leesEnFeiten(inconsistentie("Post@beef"));
        assertNotEquals(dead, beef, "twee regels die alleen in de hex na de @ verschillen, kregen dezelfde lees en feiten:\n"
                + dead);
        assertTrue(dead.contains("lees inconsistentie Inconsistentie(String(\"Post@dead\"), \"V1\")"),
                () -> "de dump citeert de naam van de regel niet:\n" + dead);
        assertTrue(beef.contains("lees inconsistentie Inconsistentie(String(\"Post@beef\"), \"V1\")"),
                () -> "de dump citeert de naam van de regel niet:\n" + beef);
    }

    /** A sentence of the branch that names an object of the session by its type and identity, here a fault put in by
     *  hand, is refused by the dump, which neither writes nor replaces an identity. */
    @Test void een_zin_met_een_identiteit_wordt_geweigerd() throws Exception {
        String model = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst", List.of(attribuut("merk", TEKST)),
                List.of())), List.of());
        Path ir = TmsDocumenten.document(model, List.of());
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> CaseRunner.voorMeting(ir,
                DroolsBackend.Branch.EXEC_MODEL).metSessie(vondst("V1", "x"), (bouw, sessie, labels) -> {
            Object v1 = labels.get("V1");
            String identiteit = v1.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(v1));
            sessie.insert(new Fout(v1, "proef", new IllegalStateException("een zin over " + identiteit)));
            return Sessiedump.op(sessie, labels);
        }));
        assertTrue(e.getMessage().startsWith("de dump schrijft geen identiteit: de zin \"een zin over Vondst@"),
                e.getMessage());
    }

    /** The case of one Vondst labelled {@code label} whose merk is {@code merk}. */
    private static JsonNode vondst(String label, String merk) throws Exception {
        return TmsDocumenten.zaak(List.of("{\"label\":\"" + label + "\",\"objectType\":\"Vondst\",\"attributes\":{\"merk\":"
                + "{\"text\":\"" + merk + "\"}},\"roles\":{},\"characteristics\":[]}"), List.of(TmsDocumenten.consistent()));
    }

    /** The document of a consistency named {@code naam} that every Vondst be verdacht. */
    private static Path inconsistentie(String naam) throws Exception {
        String model = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst", List.of(), List.of("verdacht"))),
                List.of());
        return TmsDocumenten.document(model, List.of(),
                List.of(TmsDocumenten.consistentie(naam, "Vondst", TmsDocumenten.check("verdacht", false))));
    }

    /** The sections {@code lees} and {@code feit} of the dump of a Vondst V1 that is not verdacht. */
    private static String leesEnFeiten(Path ir) throws Exception {
        JsonNode c = TmsDocumenten.zaak(List.of("{\"label\":\"V1\",\"objectType\":\"Vondst\",\"attributes\":{},"
                + "\"roles\":{},\"characteristics\":[]}"), List.of(TmsDocumenten.consistent()));
        String d = dump(CaseRunner.voorMeting(ir, DroolsBackend.Branch.EXEC_MODEL), c);
        return Stream.of(d.split("\n")).filter(r -> r.startsWith("lees ") || r.startsWith("feit "))
                .collect(Collectors.joining("\n"));
    }

    /** The dump of the session of {@code c}. */
    private static String dump(CaseRunner runner, JsonNode c) throws Exception {
        return runner.metSessie(c, (bouw, sessie, labels) -> {
            Sessiedump d = Sessiedump.op(sessie, labels);
            StringBuilder uit = new StringBuilder();
            d.schrijf(uit, bouw, DroolsBackend.vuur(bouw, sessie), labels);
            return uit.toString();
        });
    }
}
