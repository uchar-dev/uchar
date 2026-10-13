package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.graaf.ProducentConsument.ElementSleutel;
import dev.uchar.regelspraak.transpiler.tms.Fout;
import dev.uchar.regelspraak.transpiler.tms.Waarde;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuut;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
 *
 * <p>Every name stands quoted (lens on round 4), also one that holds a space, "=", ".", "(" or ")", which WF-057 admits.
 * <b>Red</b> on {@code 605fc574}, before the fix: the dump wrote {@code lees waarde "V1".tarief = vak (oud).nieuw =
 * BigDecimal(1)} and {@code lees kenmerk "V1" wrak (x) = y}; the keys of "a.b" with "c" and of "a" with "b.c" both
 * printed {@code attribuut=a.b.c}, and the two dumps were equal; and the links of the fact type "F x" (roles "r", "s")
 * and of the fact type "F" (roles "x r", "s") both printed {@code lees koppeling F x r="V1" s=«"maak"»("V1")#1}. The
 * documents pass the verifier, and the reference interpreter and the ASP runner pass their cases.
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

    /** An attribute and a characteristic whose names WF-057 admits and that hold the characters of the structure of a
     *  line: a space, "=", ".", "(" and ")". The dump quotes both, so the line of a derived value and that of a derived
     *  characteristic keep one reading. */
    @Test void een_naam_met_tekens_van_de_regelopbouw_staat_geciteerd() throws Exception {
        String d = dump(CaseRunner.voorMeting(STRUCTUURNAMEN, DroolsBackend.Branch.EXEC_MODEL), structuurzaak());
        assertTrue(d.contains("\nlees waarde \"V1\".\"tarief = vak (oud).nieuw\" = BigDecimal(1)\n"),
                () -> "de dump citeert het attribuut niet:\n" + d);
        assertTrue(d.contains("\nlees kenmerk \"V1\" \"wrak (x) = y\"\n"), () -> "de dump citeert het kenmerk niet:\n" + d);
    }

    /** Two keys of members that differ, whose type and name read as one text where they stand unquoted: "a.b" with "c"
     *  and "a" with "b.c", and the attribute "b (kenmerk)" with the characteristic "b". Each pair gives two dumps that
     *  differ, with the type and the name of the key quoted. */
    @Test void twee_sleutels_die_als_tekst_samenvallen_verschillen_in_de_dump() throws Exception {
        assertSleutels(ElementSleutel.van("A.B", "c", false), ElementSleutel.van("A", "B.c", false),
                "\"a.b\".\"c\"", "\"a\".\"b.c\"");
        assertSleutels(ElementSleutel.van("A", "b (kenmerk)", false), ElementSleutel.van("A", "b", true),
                "\"a\".\"b (kenmerk)\"", "\"a\".\"b\" (kenmerk)");
    }

    /** Two created links of fact types that differ, whose fact type and first role read as one text where they stand
     *  unquoted: the fact type "F x" with the roles "r" and "s", and the fact type "F" with the roles "x r" and "s".
     *  Each link is the one an object creation lays between its subject and the Boei it makes. */
    @Test void twee_koppelingen_die_als_tekst_samenvallen_verschillen_in_de_dump() throws Exception {
        String een = koppelingregel("F x", "r"), twee = koppelingregel("F", "x r");
        assertNotEquals(een, twee, "twee koppelingen van verschillende feittypen kregen dezelfde regel");
        assertEquals("lees koppeling \"F x\" \"r\"=\"V1\" \"s\"=«\"maak\"»(\"V1\")#1", een);
        assertEquals("lees koppeling \"F\" \"x r\"=\"V1\" \"s\"=«\"maak\"»(\"V1\")#1", twee);
    }

    /**
     * The outcome of the fire stands in a line of its own, in the section {@code fixpunt}, which the comparison of the
     * harness holds strictly (round 5b, Fable advice 4 §2.4 point 1): {@code fixpunt ja} at a fixpoint,
     * {@code fixpunt nee "<zin>"} at the cap or the creation budget, and {@code fixpunt gooit Klasse: "<zin>"} where
     * the fire threw. The line {@code vuringen}, which the comparison only reports, keeps the count, the cap and the
     * sentence. Without the line, a session that reached its fixpoint on one branch and not on the other differed only
     * through {@code lees}, which a session that derives nothing leaves empty either way. The fixpoint is a fire of a
     * case; the two other outcomes are written for the same session as {@code vuur} would hand them over. <b>Red</b> on
     * {@code da00178e}, before the line: "expected: &lt;[[fixpunt ja], [fixpunt nee "geen fixpunt binnen \"100\"
     * vuringen"], [fixpunt gooit IllegalStateException: "de proef gooit"]]&gt; but was: &lt;[[], [], []]&gt;".
     */
    @Test void de_uitkomst_van_het_vuren_staat_in_een_eigen_regel() throws Exception {
        String model = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst", List.of(attribuut("merk", TEKST)),
                List.of())), List.of());
        CaseRunner runner = CaseRunner.voorMeting(TmsDocumenten.document(model, List.of()),
                DroolsBackend.Branch.EXEC_MODEL);
        String ja = dump(runner, vondst("V1", "x"));
        String nee = runner.metSessie(vondst("V1", "x"), (bouw, sessie, labels) -> {
            Sessiedump d = Sessiedump.op(sessie, labels);
            StringBuilder uit = new StringBuilder();
            d.schrijf(uit, bouw, new DroolsBackend.Vuring(7, 100, "geen fixpunt binnen \"100\" vuringen"), labels);
            return uit.toString();
        });
        String gooit = runner.metSessie(vondst("V1", "x"), (bouw, sessie, labels) -> {
            Sessiedump d = Sessiedump.op(sessie, labels);
            StringBuilder uit = new StringBuilder();
            d.schrijf(uit, new IllegalStateException("de proef gooit"), labels);
            return uit.toString();
        });
        assertEquals(List.of(List.of("fixpunt ja"), List.of("fixpunt nee \"geen fixpunt binnen \\\"100\\\" vuringen\""),
                        List.of("fixpunt gooit IllegalStateException: \"de proef gooit\"")),
                List.of(sectie(ja, "fixpunt"), sectie(nee, "fixpunt"), sectie(gooit, "fixpunt")));
        assertEquals(List.of(List.of("vuringen 7 plafond 100 geenFixpunt \"geen fixpunt binnen \\\"100\\\" vuringen\""),
                        List.of("vuringen gooit IllegalStateException: \"de proef gooit\"")),
                List.of(sectie(nee, "vuringen"), sectie(gooit, "vuringen")));
    }

    /** The lines of the section {@code sectie} of dump {@code d}, in their order. */
    private static List<String> sectie(String d, String sectie) {
        return Stream.of(d.split("\n")).filter(r -> r.startsWith(sectie + " ")).toList();
    }

    /** The document of {@link #een_naam_met_tekens_van_de_regelopbouw_staat_geciteerd}, in the test resources, where
     *  the verifier, the reference interpreter and the ASP runner read it too. */
    private static final Path STRUCTUURNAMEN = Path.of("src", "test", "resources", "sessiedump", "structuurnamen",
            "ir.json");

    /** The case of {@link #STRUCTUURNAMEN}, read from the file beside it. */
    private static JsonNode structuurzaak() throws Exception {
        return TmsDocumenten.JSON.readTree(java.nio.file.Files.readString(STRUCTUURNAMEN.resolveSibling("case.json")));
    }

    /** Puts a {@link Waarde} under {@code a} and one under {@code b} on V1 in two sessions, and asserts that their
     *  dumps differ, each with its key rendered as {@code tekstA} and {@code tekstB}. The dumps are written without a
     *  fire, as for a fire that threw, so that only the rendering of the facts counts. */
    private static void assertSleutels(ElementSleutel a, ElementSleutel b, String tekstA, String tekstB)
            throws Exception {
        String dumpA = zonderVuren(a), dumpB = zonderVuren(b);
        assertNotEquals(dumpA, dumpB, () -> a + " en " + b + " kregen dezelfde dump:\n" + dumpA);
        assertTrue(dumpA.contains("attribuut=" + tekstA + ","), () -> "de dump citeert " + a + " niet:\n" + dumpA);
        assertTrue(dumpB.contains("attribuut=" + tekstB + ","), () -> "de dump citeert " + b + " niet:\n" + dumpB);
    }

    private static String zonderVuren(ElementSleutel sleutel) throws Exception {
        String model = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst", List.of(attribuut("merk", TEKST)),
                List.of())), List.of());
        return CaseRunner.voorMeting(TmsDocumenten.document(model, List.of()), DroolsBackend.Branch.EXEC_MODEL)
                .metSessie(vondst("V1", "x"), (bouw, sessie, labels) -> {
                    sessie.insert(new Waarde(labels.get("V1"), sleutel, BigDecimal.ONE, "proef"));
                    Sessiedump d = Sessiedump.op(sessie, labels);
                    StringBuilder uit = new StringBuilder();
                    d.schrijf(uit, new IllegalStateException("proef"), labels);
                    return uit.toString();
                });
    }

    /** The line of {@code lees} for the link the creation "maak" lays between V1 and the Boei it makes for V1, in a
     *  document whose fact type {@code feittype} relates a Vondst in the role {@code rol} to a Boei in the role "s". */
    private static String koppelingregel(String feittype, String rol) throws Exception {
        String d = dump(CaseRunner.voorMeting(koppelingdocument(feittype, rol), DroolsBackend.Branch.EXEC_MODEL),
                TmsDocumenten.zaak(List.of(TmsDocumenten.instantie("V1", "Vondst", Map.of(), List.of(), Map.of())),
                        List.of(TmsDocumenten.consistent())));
        List<String> regels = Stream.of(d.split("\n")).filter(r -> r.startsWith("lees koppeling ")).toList();
        assertEquals(1, regels.size(), () -> "één gemaakte koppeling in lees:\n" + d);
        return regels.get(0);
    }

    /** "Een Vondst heeft een Boei": the creation "maak", whose new Boei plays the role "s" of {@code feittype} beside the
     *  Vondst in the role {@code rol}. */
    static Path koppelingdocument(String feittype, String rol) throws Exception {
        String model = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst", List.of(), List.of()),
                TmsDocumenten.objecttype("Boei", List.of(), List.of())),
                List.of(TmsDocumenten.feittype(feittype, rol, "Vondst", "s", "Boei")));
        return TmsCreatieWitnessTest.document(model, List.of(), List.of(), List.of(TmsCreatieWitnessTest.objectcreatie(
                "maak", "Vondst", "Boei", List.of(), "null", feittype, "s")), List.of(), List.of());
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
