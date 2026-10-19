package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.graph.ProducerConsumer.ElementKey;
import dev.uchar.regelspraak.transpiler.tms.Fault;
import dev.uchar.regelspraak.transpiler.tms.Value;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.uchar.regelspraak.cases.TmsDocuments.attribute;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The dump of a session ({@link SessionDump}) names every object by a name of its own, never by its identity, and
 * replaces nothing: a case label, the name of a rule and a text value stand quoted and escaped as they are. Two sessions
 * that differ only in such a name, one that looks like an identity, "Post@dead" against "Post@beef", get different
 * dumps, each with its name as it is; and a sentence of the branch that names an object by its identity is refused,
 * never written. Without that, the harness of card drl-teksttak-op-truth-maintenance saw two different facts as one
 * (lens on step 3a for a text value, lens on step 3b for a label and the name of a rule).
 *
 * <p><b>Red</b> on {@code d02ccea9}, before the fix of the text value: both dumps of the first test carried
 * {@code String("Post@?")}, and the two were equal. <b>Red</b> on {@code 55a24e61}, before the fix of the names: the
 * dumps of the two labels were equal, both naming the bean {@code Post@?}; the sections {@code read} and {@code fact} of
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
class SessionDumpWitnessTest {

    private static final String TEKST = "{\"node\":\"Text\"}";

    @Test void a_text_that_resembles_an_identity_stays_as_it_is() throws Exception {
        String model = TmsDocuments.model(List.of(TmsDocuments.objectType("Vondst", List.of(attribute("merk", TEKST)),
                List.of())), List.of());
        Path ir = TmsDocuments.document(model, List.of());
        CaseRunner runner = CaseRunner.forMeasurement(ir, DroolsBackend.Branch.EXEC_MODEL);
        String dead = dump(runner, vondst("V1", "Post@dead")), beef = dump(runner, vondst("V1", "Post@beef"));
        assertNotEquals(dead, beef, "twee teksten die alleen in de hex na de @ verschillen, kregen dezelfde dump:\n" + dead);
        assertTrue(dead.contains("String(\"Post@dead\")"), () -> "de dump citeert de tekst niet zoals hij is:\n" + dead);
        assertTrue(beef.contains("String(\"Post@beef\")"), () -> "de dump citeert de tekst niet zoals hij is:\n" + beef);
    }

    /** A case label is free text (the case schema), so it can look like an identity; the dump quotes it. */
    @Test void a_label_that_resembles_an_identity_stays_as_it_is() throws Exception {
        String model = TmsDocuments.model(List.of(TmsDocuments.objectType("Vondst", List.of(attribute("merk", TEKST)),
                List.of())), List.of());
        Path ir = TmsDocuments.document(model, List.of());
        CaseRunner runner = CaseRunner.forMeasurement(ir, DroolsBackend.Branch.EXEC_MODEL);
        String dead = dump(runner, vondst("Post@dead", "x")), beef = dump(runner, vondst("Post@beef", "x"));
        assertNotEquals(dead, beef, "twee labels die alleen in de hex na de @ verschillen, kregen dezelfde dump:\n" + dead);
        assertTrue(dead.contains("bean \"Post@dead\" Vondst("), () -> "de dump citeert het label niet:\n" + dead);
        assertTrue(beef.contains("bean \"Post@beef\" Vondst("), () -> "de dump citeert het label niet:\n" + beef);
    }

    /** The §9.5 inconsistency of a consistency rule whose name looks like an identity: what {@code read} gives and the
     *  facts differ between the two names, each with its name quoted. */
    @Test void an_inconsistency_under_a_name_that_resembles_an_identity() throws Exception {
        String dead = readAndFacts(inconsistency("Post@dead")), beef = readAndFacts(inconsistency("Post@beef"));
        assertNotEquals(dead, beef, "twee regels die alleen in de hex na de @ verschillen, kregen dezelfde lees en feiten:\n"
                + dead);
        assertTrue(dead.contains("read inconsistency Inconsistentie(String(\"Post@dead\"), \"V1\")"),
                () -> "de dump citeert de naam van de regel niet:\n" + dead);
        assertTrue(beef.contains("read inconsistency Inconsistentie(String(\"Post@beef\"), \"V1\")"),
                () -> "de dump citeert de naam van de regel niet:\n" + beef);
    }

    /** A sentence of the branch that names an object of the session by its type and identity, here a fault put in by
     *  hand, is refused by the dump, which neither writes nor replaces an identity. */
    @Test void a_sentence_with_an_identity_is_refused() throws Exception {
        String model = TmsDocuments.model(List.of(TmsDocuments.objectType("Vondst", List.of(attribute("merk", TEKST)),
                List.of())), List.of());
        Path ir = TmsDocuments.document(model, List.of());
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> CaseRunner.forMeasurement(ir,
                DroolsBackend.Branch.EXEC_MODEL).withSession(vondst("V1", "x"), (build, session, labels) -> {
            Object v1 = labels.get("V1");
            String identity = v1.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(v1));
            session.insert(new Fault(v1, "proef", new IllegalStateException("een zin over " + identity)));
            return SessionDump.op(session, labels);
        }));
        assertTrue(e.getMessage().startsWith("de dump schrijft geen identiteit: de zin \"een zin over Vondst@"),
                e.getMessage());
    }

    /** An attribute and a characteristic whose names WF-057 admits and that hold the characters of the structure of a
     *  line: a space, "=", ".", "(" and ")". The dump quotes both, so the line of a derived value and that of a derived
     *  characteristic keep one reading. */
    @Test void a_name_with_signs_of_the_line_structure_is_quoted() throws Exception {
        String d = dump(CaseRunner.forMeasurement(STRUCTURE_NAMES, DroolsBackend.Branch.EXEC_MODEL), structureCase());
        assertTrue(d.contains("\nread value \"V1\".\"tarief = vak (oud).nieuw\" = BigDecimal(1)\n"),
                () -> "de dump citeert het attribuut niet:\n" + d);
        assertTrue(d.contains("\nread characteristic \"V1\" \"wrak (x) = y\"\n"), () -> "de dump citeert het kenmerk niet:\n" + d);
    }

    /** Two keys of members that differ, whose type and name read as one text where they stand unquoted: "a.b" with "c"
     *  and "a" with "b.c", and the attribute "b (characteristic)" with the characteristic "b". Each pair gives two dumps that
     *  differ, with the type and the name of the key quoted. */
    @Test void two_keys_that_coincide_as_text_differ_in_the_dump() throws Exception {
        assertKeys(ElementKey.of("A.B", "c", false), ElementKey.of("A", "B.c", false),
                "\"a.b\".\"c\"", "\"a\".\"b.c\"");
        assertKeys(ElementKey.of("A", "b (characteristic)", false), ElementKey.of("A", "b", true),
                "\"a\".\"b (characteristic)\"", "\"a\".\"b\" (characteristic)");
    }

    /** Two created links of fact types that differ, whose fact type and first role read as one text where they stand
     *  unquoted: the fact type "F x" with the roles "r" and "s", and the fact type "F" with the roles "x r" and "s".
     *  Each link is the one an object creation lays between its subject and the Boei it makes. */
    @Test void two_links_that_coincide_as_text_differ_in_the_dump() throws Exception {
        String one = linkLine("F x", "r"), two = linkLine("F", "x r");
        assertNotEquals(one, two, "twee koppelingen van verschillende feittypen kregen dezelfde regel");
        assertEquals("read link \"F x\" \"r\"=\"V1\" \"s\"=«\"maak\"»(\"V1\")#1", one);
        assertEquals("read link \"F\" \"x r\"=\"V1\" \"s\"=«\"maak\"»(\"V1\")#1", two);
    }

    /**
     * The outcome of the fire stands in a line of its own, in the section {@code fixpoint}, which the comparison of the
     * harness holds strictly (round 5b, Fable advice 4 §2.4 point 1): {@code fixpoint yes} at a fixpoint,
     * {@code fixpoint no "<sentence>"} at the cap or the creation budget, and {@code fixpoint throws Class: "<sentence>"} where
     * the fire threw. The line {@code firings}, which the comparison only reports, keeps the count, the cap and the
     * sentence. Without the line, a session that reached its fixpoint on one branch and not on the other differed only
     * through {@code read}, which a session that derives nothing leaves empty either way. The fixpoint is a fire of a
     * case; the two other outcomes are written for the same session as {@code fire} would hand them over. <b>Red</b> on
     * {@code da00178e}, before the line: "expected: &lt;[[fixpunt ja], [fixpunt nee "geen fixpunt binnen \"100\"
     * vuringen"], [fixpunt gooit IllegalStateException: "de proef gooit"]]&gt; but was: &lt;[[], [], []]&gt;".
     */
    @Test void the_outcome_of_the_firing_stands_in_its_own_line() throws Exception {
        String model = TmsDocuments.model(List.of(TmsDocuments.objectType("Vondst", List.of(attribute("merk", TEKST)),
                List.of())), List.of());
        CaseRunner runner = CaseRunner.forMeasurement(TmsDocuments.document(model, List.of()),
                DroolsBackend.Branch.EXEC_MODEL);
        String atFixpoint = dump(runner, vondst("V1", "x"));
        String atCap = runner.withSession(vondst("V1", "x"), (build, session, labels) -> {
            SessionDump d = SessionDump.op(session, labels);
            StringBuilder out = new StringBuilder();
            d.write(out, build, new DroolsBackend.Firing(7, 100, "geen fixpunt binnen \"100\" vuringen"), labels);
            return out.toString();
        });
        String thrown = runner.withSession(vondst("V1", "x"), (build, session, labels) -> {
            SessionDump d = SessionDump.op(session, labels);
            StringBuilder out = new StringBuilder();
            d.write(out, new IllegalStateException("de proef gooit"), labels);
            return out.toString();
        });
        assertEquals(List.of(List.of("fixpoint yes"), List.of("fixpoint no \"geen fixpunt binnen \\\"100\\\" vuringen\""),
                        List.of("fixpoint throws IllegalStateException: \"de proef gooit\"")),
                List.of(section(atFixpoint, "fixpoint"), section(atCap, "fixpoint"), section(thrown, "fixpoint")));
        assertEquals(List.of(List.of("firings 7 cap 100 noFixpoint \"geen fixpunt binnen \\\"100\\\" vuringen\""),
                        List.of("firings throws IllegalStateException: \"de proef gooit\"")),
                List.of(section(atCap, "firings"), section(thrown, "firings")));
    }

    /**
     * The whole dump, every section, of two documents on both branches, held against a file in the test resources
     * (round 5b, Fable advice 4 §1.4): the document {@link #STRUCTURE_NAMES} with its case, and the document of
     * {@link #two_links_that_coincide_as_text_differ_in_the_dump} with the fact type "F x", whose
     * creation "maak" makes a Boei for V1 and lays the link between them. Until then the dump was held by the baseline
     * of the harness, which is made with the dump code of the worktree and so changes along with it, and by single
     * lines of this class: a change in the spelling of a value or a section passed unseen once the baseline was made
     * again. The pins live in {@code src/test/resources/session-dump/<document>/dump.<branch>.txt}; under the property
     * {@code uchar.dumppin} the test writes each dump it makes to that directory as well, to renew a pin by hand after
     * a deliberate change. The two branches give the same dump of each document.
     *
     * <p><b>Red</b> with a mutant of {@code SessionDump} that writes a {@code BigDecimal} without its class (not
     * committed): "de dump in structure-names/dump.exec_model.txt ==&gt; expected: &lt;… lees waarde "V1"."tarief = vak
     * (oud).nieuw" = BigDecimal(1) …&gt; but was: &lt;… lees waarde "V1"."tarief = vak (oud).nieuw" = 1 …&gt;"; and on
     * {@code da00178e}, before the section {@code fixpoint}, where the dump lacked the line "fixpoint yes".
     */
    @Test void the_whole_dump_of_two_documents_is_fixed() throws Exception {
        Map<String, Path> documents = new java.util.LinkedHashMap<>();
        documents.put("structure-names", STRUCTURE_NAMES);
        documents.put("relation-link", linkDocument("F x", "r"));
        Map<String, JsonNode> cases = Map.of("structure-names", structureCase(), "relation-link", TmsDocuments.caseDocument(
                List.of(TmsDocuments.instance("V1", "Vondst", Map.of(), List.of(), Map.of())),
                List.of(TmsDocuments.consistent())));
        String renew = System.getProperty("uchar.dumppin");
        // every dump is made, and written under the property, before the first comparison can stop the test
        Map<Path, String> dumps = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Path> doc : documents.entrySet())
            for (DroolsBackend.Branch branch : List.of(DroolsBackend.Branch.EXEC_MODEL, DroolsBackend.Branch.DRL_TEXT)) {
                String d = BranchDumps.dump(doc.getValue(), cases.get(doc.getKey()), branch);
                Path file = Path.of(doc.getKey(), "dump." + branch.name().toLowerCase(java.util.Locale.ROOT) + ".txt");
                dumps.put(file, d);
                if (renew != null) {
                    Path path = Path.of(renew).resolve(file);
                    java.nio.file.Files.createDirectories(path.getParent());
                    java.nio.file.Files.writeString(path, d);
                }
            }
        for (Map.Entry<Path, String> d : dumps.entrySet())
            assertEquals(java.nio.file.Files.readString(PIN_DIRECTORY.resolve(d.getKey())), d.getValue(),
                    () -> "de dump in " + d.getKey());
    }

    /**
     * Every form in which the dump spells a value (Fable advice 4 §2.2), through the dump of one session and pinned as
     * text, on both branches (fix round 5b, lens on round 5b): the pins of
     * {@link #the_whole_dump_of_two_documents_is_fixed} hold no global, list, map, other collection, enumeration
     * label, exception or record, so a change in their spelling passed unseen once the baseline of the harness was made
     * again with the changed code. A global is a parameter the rule reads, set by the case; the other values are put
     * into working memory right before the dump, as the facts of a fire that threw are written: a text with a quote, a
     * backslash, a carriage return and a line feed; a list in its order, with a number in three scales and an empty
     * element; a map and a set, sorted; an enumeration label; an exception with its class and its sentence; a record
     * with its components; the five classes whose text stands unquoted ({@code Boolean}, {@code Integer},
     * {@code Long}, {@code LocalDate}, {@code LocalDateTime}); another class with its own text, quoted; and the key of a
     * characteristic. A bean without a name and an object whose class has no text of its own are refused, each in its
     * own sentence, on both branches (lens on step 6a: until then on the exec-model branch alone).
     *
     * <p><b>Red</b> with a mutant of {@code SessionDump} that writes a collection that is no list in its own order
     * instead of sorted (not committed; the diff and the output are kept with fix round 5b of step 6a). The refusals
     * stand in code both branches share; red on the DRL-text branch alone with a mutant that tells that branch by its
     * session, the resource of its rules, and names a bean without a name there instead of refusing it (not committed;
     * the diff and the output are kept with step 6b).
     */
    @Test void every_spelling_of_the_dump_is_fixed() throws Exception {
        String model = TmsDocuments.model(List.of(TmsDocuments.objectType("Vondst", List.of(attribute("merk", TEKST),
                attribute("tarief", TmsDocuments.WHOLE)), List.of())), List.of(), List.of("drempel"));
        Path ir = TmsDocuments.document(model, List.of(TmsDocuments.computingRule("tarief", "GELIJKSTELLING", "Vondst",
                "tarief", TmsDocuments.parameterRead("drempel"), "null")));
        JsonNode caseDocument = TmsDocuments.caseDocument(List.of("{\"label\":\"V1\",\"objectType\":\"Vondst\",\"attributes\":{\"merk\":"
                + "{\"text\":\"x\"}},\"roles\":{},\"characteristics\":[]}"), Map.of("drempel", "3"),
                List.of(TmsDocuments.consistent()));
        String expected = String.join("\n",
                "input bean \"V1\" Vondst(merk=String(\"x\"), tarief=empty)",
                "input global \"drempel\" = BigDecimal(3)",
                "firings throws IllegalStateException: \"de proef vuurt niet\"",
                "fixpoint throws IllegalStateException: \"de proef vuurt niet\"",
                "trace-count {}",
                "trace-order []",
                "facts 15",
                "fact \"vondst\".\"wrak\" (characteristic)",
                "fact Boolean(true)",
                "fact IllegalArgumentException: \"een \\\"fout\\\"\"",
                "fact Integer(7)",
                "fact LocalDate(2026-10-14)",
                "fact LocalDateTime(2026-10-14T09:15:30)",
                "fact LocalTime(\"09:15\")",
                "fact Long(8)",
                "fact SampleRecord(name=String(\"r\"), number=BigDecimal(1.50), list=[Boolean(false)])",
                "fact SampleType.FIRST_KIND",
                "fact String(\"a\\\"b\\\\c\\rd\\ne\")",
                "fact [BigDecimal(2), BigDecimal(2.0), BigDecimal(2.00), empty, String(\"b\")]",
                "fact bean \"V1\" Vondst(merk=String(\"x\"), tarief=empty)",
                "fact {String(\"a\"), String(\"z\")}",
                "fact {String(\"a\")=Integer(2), String(\"b\")=Integer(1)}",
                "");
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            CaseRunner runner = CaseRunner.forMeasurement(ir, branch);
            String d = runner.withSession(caseDocument, (build, session, labels) -> {
                SessionDump dump = SessionDump.op(session, labels);
                for (Object o : everyForm()) session.insert(o);
                StringBuilder out = new StringBuilder();
                dump.write(out, new IllegalStateException("de proef vuurt niet"), labels);
                return out.toString();
            });
            assertEquals(expected, d, () -> "de dump op " + branch);
            IllegalStateException unnamed = assertThrows(IllegalStateException.class, () -> runner.withSession(caseDocument,
                    (build, session, labels) -> refused(session, labels, unnamedVondst(build.beanLoader()))),
                    () -> "de bean zonder naam op " + branch);
            assertEquals("de dump heeft geen naam voor een bean van Vondst: geen caselabel en geen creatie die haar"
                    + " maakte", unnamed.getMessage(), () -> "de bean zonder naam op " + branch);
            IllegalStateException withoutText = assertThrows(IllegalStateException.class, () -> runner.withSession(caseDocument,
                    (build, session, labels) -> refused(session, labels, new Textless())),
                    () -> "het object zonder tekst op " + branch);
            assertEquals("de dump schrijft geen identiteit: een " + Textless.class.getName() + " heeft geen tekst van"
                    + " zichzelf", withoutText.getMessage(), () -> "het object zonder tekst op " + branch);
        }
    }

    /** An enumeration of the test, whose label the dump writes with its class. */
    private enum SampleType { FIRST_KIND }

    /** A record of the test, which the dump writes with its components. */
    private record SampleRecord(String name, BigDecimal number, List<Object> list) {}

    /** A class of the test without a text of its own, which the dump refuses: its text would be its identity. */
    private static final class Textless {}

    /** The values of {@link #every_spelling_of_the_dump_is_fixed}, one per form: the set is put in "z" before "a" and
     *  the map holds two keys, so that the sorting shows. */
    private static List<Object> everyForm() {
        java.util.Set<Object> set = new java.util.LinkedHashSet<>(List.of("z", "a"));
        Map<Object, Object> map = new java.util.LinkedHashMap<>();
        map.put("b", 1);
        map.put("a", 2);
        return List.of("a\"b\\c\rd\ne",
                java.util.Arrays.asList(new BigDecimal("2"), new BigDecimal("2.0"), new BigDecimal("2.00"), null, "b"),
                map, set, SampleType.FIRST_KIND, new IllegalArgumentException("een \"fout\""),
                new SampleRecord("r", new BigDecimal("1.50"), List.of(false)), Boolean.TRUE, 7, 8L,
                java.time.LocalDate.of(2026, 10, 14), java.time.LocalDateTime.of(2026, 10, 14, 9, 15, 30),
                java.time.LocalTime.of(9, 15), ElementKey.of("Vondst", "wrak", true));
    }

    /** The dump of a session into which {@code object} is put right before the dump, which must refuse it. */
    private static String refused(org.kie.api.runtime.KieSession session, Map<String, Object> labels, Object object) {
        SessionDump dump = SessionDump.op(session, labels);
        session.insert(object);
        StringBuilder out = new StringBuilder();
        dump.write(out, new IllegalStateException("de proef vuurt niet"), labels);
        return out.toString();
    }

    /** A Vondst of {@code beanLoader} that no case label names and no creation made. */
    private static Object unnamedVondst(ClassLoader beanLoader) {
        try {
            return TmsDocuments.beanObject(beanLoader, "Vondst", Map.of(), List.of());
        } catch (Exception e) {
            throw new AssertionError("de bean Vondst van de proef is niet te maken", e);
        }
    }

    /** The directory of the pinned dumps of {@link #the_whole_dump_of_two_documents_is_fixed}. */
    private static final Path PIN_DIRECTORY = Path.of("src", "test", "resources", "session-dump");

    /** The lines of the section {@code section} of dump {@code d}, in their order. */
    private static List<String> section(String d, String section) {
        return Stream.of(d.split("\n")).filter(r -> r.startsWith(section + " ")).toList();
    }

    /** The document of {@link #a_name_with_signs_of_the_line_structure_is_quoted}, in the test resources, where
     *  the verifier, the reference interpreter and the ASP runner read it too. */
    private static final Path STRUCTURE_NAMES = Path.of("src", "test", "resources", "session-dump", "structure-names",
            "ir.json");

    /** The case of {@link #STRUCTURE_NAMES}, read from the file beside it. */
    private static JsonNode structureCase() throws Exception {
        return TmsDocuments.JSON.readTree(java.nio.file.Files.readString(STRUCTURE_NAMES.resolveSibling("case.json")));
    }

    /** Puts a {@link Value} under {@code a} and one under {@code b} on V1 in two sessions, and asserts that their
     *  dumps differ, each with its key rendered as {@code textA} and {@code textB}. The dumps are written without a
     *  fire, as for a fire that threw, so that only the rendering of the facts counts. */
    private static void assertKeys(ElementKey a, ElementKey b, String textA, String textB)
            throws Exception {
        String dumpA = withoutFire(a), dumpB = withoutFire(b);
        assertNotEquals(dumpA, dumpB, () -> a + " en " + b + " kregen dezelfde dump:\n" + dumpA);
        assertTrue(dumpA.contains("attribute=" + textA + ","), () -> "de dump citeert " + a + " niet:\n" + dumpA);
        assertTrue(dumpB.contains("attribute=" + textB + ","), () -> "de dump citeert " + b + " niet:\n" + dumpB);
    }

    private static String withoutFire(ElementKey key) throws Exception {
        String model = TmsDocuments.model(List.of(TmsDocuments.objectType("Vondst", List.of(attribute("merk", TEKST)),
                List.of())), List.of());
        return CaseRunner.forMeasurement(TmsDocuments.document(model, List.of()), DroolsBackend.Branch.EXEC_MODEL)
                .withSession(vondst("V1", "x"), (build, session, labels) -> {
                    session.insert(new Value(labels.get("V1"), key, BigDecimal.ONE, "proef"));
                    SessionDump d = SessionDump.op(session, labels);
                    StringBuilder out = new StringBuilder();
                    d.write(out, new IllegalStateException("proef"), labels);
                    return out.toString();
                });
    }

    /** The line of {@code read} for the link the creation "maak" lays between V1 and the Boei it makes for V1, in a
     *  document whose fact type {@code factType} relates a Vondst in the role {@code role} to a Boei in the role "s". */
    private static String linkLine(String factType, String role) throws Exception {
        String d = dump(CaseRunner.forMeasurement(linkDocument(factType, role), DroolsBackend.Branch.EXEC_MODEL),
                TmsDocuments.caseDocument(List.of(TmsDocuments.instance("V1", "Vondst", Map.of(), List.of(), Map.of())),
                        List.of(TmsDocuments.consistent())));
        List<String> rules = Stream.of(d.split("\n")).filter(r -> r.startsWith("read link ")).toList();
        assertEquals(1, rules.size(), () -> "één gemaakte koppeling in lees:\n" + d);
        return rules.get(0);
    }

    /** "Een Vondst heeft een Boei": the creation "maak", whose new Boei plays the role "s" of {@code factType} beside the
     *  Vondst in the role {@code role}. */
    static Path linkDocument(String factType, String role) throws Exception {
        String model = TmsDocuments.model(List.of(TmsDocuments.objectType("Vondst", List.of(), List.of()),
                TmsDocuments.objectType("Boei", List.of(), List.of())),
                List.of(TmsDocuments.factType(factType, role, "Vondst", "s", "Boei")));
        return TmsCreationWitnessTest.document(model, List.of(), List.of(), List.of(TmsCreationWitnessTest.objectCreation(
                "maak", "Vondst", "Boei", List.of(), "null", factType, "s")), List.of(), List.of());
    }

    /** The case of one Vondst labelled {@code label} whose merk is {@code merk}. */
    private static JsonNode vondst(String label, String mark) throws Exception {
        return TmsDocuments.caseDocument(List.of("{\"label\":\"" + label + "\",\"objectType\":\"Vondst\",\"attributes\":{\"merk\":"
                + "{\"text\":\"" + mark + "\"}},\"roles\":{},\"characteristics\":[]}"), List.of(TmsDocuments.consistent()));
    }

    /** The document of a consistency named {@code name} that every Vondst be verdacht. */
    private static Path inconsistency(String name) throws Exception {
        String model = TmsDocuments.model(List.of(TmsDocuments.objectType("Vondst", List.of(), List.of("verdacht"))),
                List.of());
        return TmsDocuments.document(model, List.of(),
                List.of(TmsDocuments.consistency(name, "Vondst", TmsDocuments.check("verdacht", false))));
    }

    /** The sections {@code read} and {@code fact} of the dump of a Vondst V1 that is not verdacht. */
    private static String readAndFacts(Path ir) throws Exception {
        JsonNode c = TmsDocuments.caseDocument(List.of("{\"label\":\"V1\",\"objectType\":\"Vondst\",\"attributes\":{},"
                + "\"roles\":{},\"characteristics\":[]}"), List.of(TmsDocuments.consistent()));
        String d = dump(CaseRunner.forMeasurement(ir, DroolsBackend.Branch.EXEC_MODEL), c);
        return Stream.of(d.split("\n")).filter(r -> r.startsWith("read ") || r.startsWith("fact "))
                .collect(Collectors.joining("\n"));
    }

    /** The dump of the session of {@code c}. */
    private static String dump(CaseRunner runner, JsonNode c) throws Exception {
        return runner.withSession(c, (build, session, labels) -> {
            SessionDump d = SessionDump.op(session, labels);
            StringBuilder out = new StringBuilder();
            d.write(out, build, DroolsBackend.fire(build, session), labels);
            return out.toString();
        });
    }
}
