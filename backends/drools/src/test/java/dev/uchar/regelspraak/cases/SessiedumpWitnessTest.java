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

    /**
     * The whole dump, every section, of two documents on both branches, held against a file in the test resources
     * (round 5b, Fable advice 4 §1.4): the document {@link #STRUCTUURNAMEN} with its case, and the document of
     * {@link #twee_koppelingen_die_als_tekst_samenvallen_verschillen_in_de_dump} with the fact type "F x", whose
     * creation "maak" makes a Boei for V1 and lays the link between them. Until then the dump was held by the baseline
     * of the harness, which is made with the dump code of the worktree and so changes along with it, and by single
     * lines of this class: a change in the spelling of a value or a section passed unseen once the baseline was made
     * again. The pins live in {@code src/test/resources/sessiedump/<document>/dump.<branch>.txt}; under the property
     * {@code uchar.dumppin} the test writes each dump it makes to that directory as well, to renew a pin by hand after
     * a deliberate change. The two branches give the same dump of each document.
     *
     * <p><b>Red</b> with a mutant of {@code Sessiedump} that writes a {@code BigDecimal} without its class (not
     * committed): "de dump in structuurnamen/dump.exec_model.txt ==&gt; expected: &lt;… lees waarde "V1"."tarief = vak
     * (oud).nieuw" = BigDecimal(1) …&gt; but was: &lt;… lees waarde "V1"."tarief = vak (oud).nieuw" = 1 …&gt;"; and on
     * {@code da00178e}, before the section {@code fixpunt}, where the dump lacked the line "fixpunt ja".
     */
    @Test void de_hele_dump_van_twee_documenten_staat_vast() throws Exception {
        Map<String, Path> documenten = new java.util.LinkedHashMap<>();
        documenten.put("structuurnamen", STRUCTUURNAMEN);
        documenten.put("koppeling", koppelingdocument("F x", "r"));
        Map<String, JsonNode> zaken = Map.of("structuurnamen", structuurzaak(), "koppeling", TmsDocumenten.zaak(
                List.of(TmsDocumenten.instantie("V1", "Vondst", Map.of(), List.of(), Map.of())),
                List.of(TmsDocumenten.consistent())));
        String vernieuw = System.getProperty("uchar.dumppin");
        // every dump is made, and written under the property, before the first comparison can stop the test
        Map<Path, String> dumps = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Path> doc : documenten.entrySet())
            for (DroolsBackend.Branch tak : List.of(DroolsBackend.Branch.EXEC_MODEL, DroolsBackend.Branch.DRL_TMS)) {
                String d = Takdumps.dump(doc.getValue(), zaken.get(doc.getKey()), tak);
                Path bestand = Path.of(doc.getKey(), "dump." + tak.name().toLowerCase(java.util.Locale.ROOT) + ".txt");
                dumps.put(bestand, d);
                if (vernieuw != null) {
                    Path pad = Path.of(vernieuw).resolve(bestand);
                    java.nio.file.Files.createDirectories(pad.getParent());
                    java.nio.file.Files.writeString(pad, d);
                }
            }
        for (Map.Entry<Path, String> d : dumps.entrySet())
            assertEquals(java.nio.file.Files.readString(PINMAP.resolve(d.getKey())), d.getValue(),
                    () -> "de dump in " + d.getKey());
    }

    /**
     * Every form in which the dump spells a value (Fable advice 4 §2.2), through the dump of one session and pinned as
     * text, on both branches (fix round 5b, lens on round 5b): the pins of
     * {@link #de_hele_dump_van_twee_documenten_staat_vast} hold no global, list, map, other collection, enumeration
     * label, exception or record, so a change in their spelling passed unseen once the baseline of the harness was made
     * again with the changed code. A global is a parameter the rule reads, set by the case; the other values are put
     * into working memory right before the dump, as the facts of a fire that threw are written: a text with a quote, a
     * backslash, a carriage return and a line feed; a list in its order, with a number in three scales and an empty
     * element; a map and a set, sorted; an enumeration label; an exception with its class and its sentence; a record
     * with its components; the five classes whose text stands unquoted ({@code Boolean}, {@code Integer},
     * {@code Long}, {@code LocalDate}, {@code LocalDateTime}); another class with its own text, quoted; and the key of a
     * characteristic. A bean without a name and an object whose class has no text of its own are refused, each in its
     * own sentence.
     *
     * <p><b>Red</b> with a mutant of {@code Sessiedump} that writes a collection that is no list in its own order
     * instead of sorted (not committed; the diff and the output are kept with fix round 5b of step 6a).
     */
    @Test void elke_spelling_van_de_dump_staat_vast() throws Exception {
        String model = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst", List.of(attribuut("merk", TEKST),
                attribuut("tarief", TmsDocumenten.GEHEEL)), List.of())), List.of(), List.of("drempel"));
        Path ir = TmsDocumenten.document(model, List.of(TmsDocumenten.rekenregel("tarief", "GELIJKSTELLING", "Vondst",
                "tarief", TmsDocumenten.parameterlezing("drempel"), "null")));
        JsonNode zaak = TmsDocumenten.zaak(List.of("{\"label\":\"V1\",\"objectType\":\"Vondst\",\"attributes\":{\"merk\":"
                + "{\"text\":\"x\"}},\"roles\":{},\"characteristics\":[]}"), Map.of("drempel", "3"),
                List.of(TmsDocumenten.consistent()));
        String verwacht = String.join("\n",
                "invoer bean \"V1\" Vondst(merk=String(\"x\"), tarief=leeg)",
                "invoer global \"drempel\" = BigDecimal(3)",
                "vuringen gooit IllegalStateException: \"de proef vuurt niet\"",
                "fixpunt gooit IllegalStateException: \"de proef vuurt niet\"",
                "spoor-telling {}",
                "spoor-volgorde []",
                "feiten 15",
                "feit \"vondst\".\"wrak\" (kenmerk)",
                "feit Boolean(true)",
                "feit IllegalArgumentException: \"een \\\"fout\\\"\"",
                "feit Integer(7)",
                "feit LocalDate(2026-10-14)",
                "feit LocalDateTime(2026-10-14T09:15:30)",
                "feit LocalTime(\"09:15\")",
                "feit Long(8)",
                "feit Proefrecord(naam=String(\"r\"), getal=BigDecimal(1.50), lijst=[Boolean(false)])",
                "feit Proefsoort.EERSTE_SOORT",
                "feit String(\"a\\\"b\\\\c\\rd\\ne\")",
                "feit [BigDecimal(2), BigDecimal(2.0), BigDecimal(2.00), leeg, String(\"b\")]",
                "feit bean \"V1\" Vondst(merk=String(\"x\"), tarief=leeg)",
                "feit {String(\"a\"), String(\"z\")}",
                "feit {String(\"a\")=Integer(2), String(\"b\")=Integer(1)}",
                "");
        for (DroolsBackend.Branch tak : TmsDocumenten.TAKKEN) {
            String d = CaseRunner.voorMeting(ir, tak).metSessie(zaak, (bouw, sessie, labels) -> {
                Sessiedump dump = Sessiedump.op(sessie, labels);
                for (Object o : elkeVorm()) sessie.insert(o);
                StringBuilder uit = new StringBuilder();
                dump.schrijf(uit, new IllegalStateException("de proef vuurt niet"), labels);
                return uit.toString();
            });
            assertEquals(verwacht, d, () -> "de dump op " + tak);
        }
        CaseRunner runner = CaseRunner.voorMeting(ir, DroolsBackend.Branch.EXEC_MODEL);
        IllegalStateException naamloos = assertThrows(IllegalStateException.class, () -> runner.metSessie(zaak,
                (bouw, sessie, labels) -> geweigerd(sessie, labels, naamlozeVondst(bouw.beanLoader()))));
        assertEquals("de dump heeft geen naam voor een bean van Vondst: geen caselabel en geen creatie die haar maakte",
                naamloos.getMessage());
        IllegalStateException zonderTekst = assertThrows(IllegalStateException.class, () -> runner.metSessie(zaak,
                (bouw, sessie, labels) -> geweigerd(sessie, labels, new Zonderzin())));
        assertEquals("de dump schrijft geen identiteit: een " + Zonderzin.class.getName() + " heeft geen tekst van"
                + " zichzelf", zonderTekst.getMessage());
    }

    /** An enumeration of the test, whose label the dump writes with its class. */
    private enum Proefsoort { EERSTE_SOORT }

    /** A record of the test, which the dump writes with its components. */
    private record Proefrecord(String naam, BigDecimal getal, List<Object> lijst) {}

    /** A class of the test without a text of its own, which the dump refuses: its text would be its identity. */
    private static final class Zonderzin {}

    /** The values of {@link #elke_spelling_van_de_dump_staat_vast}, one per form: the set is put in "z" before "a" and
     *  the map holds two keys, so that the sorting shows. */
    private static List<Object> elkeVorm() {
        java.util.Set<Object> verzameling = new java.util.LinkedHashSet<>(List.of("z", "a"));
        Map<Object, Object> kaart = new java.util.LinkedHashMap<>();
        kaart.put("b", 1);
        kaart.put("a", 2);
        return List.of("a\"b\\c\rd\ne",
                java.util.Arrays.asList(new BigDecimal("2"), new BigDecimal("2.0"), new BigDecimal("2.00"), null, "b"),
                kaart, verzameling, Proefsoort.EERSTE_SOORT, new IllegalArgumentException("een \"fout\""),
                new Proefrecord("r", new BigDecimal("1.50"), List.of(false)), Boolean.TRUE, 7, 8L,
                java.time.LocalDate.of(2026, 10, 14), java.time.LocalDateTime.of(2026, 10, 14, 9, 15, 30),
                java.time.LocalTime.of(9, 15), ElementSleutel.van("Vondst", "wrak", true));
    }

    /** The dump of a session into which {@code object} is put right before the dump, which must refuse it. */
    private static String geweigerd(org.kie.api.runtime.KieSession sessie, Map<String, Object> labels, Object object) {
        Sessiedump dump = Sessiedump.op(sessie, labels);
        sessie.insert(object);
        StringBuilder uit = new StringBuilder();
        dump.schrijf(uit, new IllegalStateException("de proef vuurt niet"), labels);
        return uit.toString();
    }

    /** A Vondst of {@code beanLoader} that no case label names and no creation made. */
    private static Object naamlozeVondst(ClassLoader beanLoader) {
        try {
            return TmsDocumenten.boon(beanLoader, "Vondst", Map.of(), List.of());
        } catch (Exception e) {
            throw new AssertionError("de bean Vondst van de proef is niet te maken", e);
        }
    }

    /** The directory of the pinned dumps of {@link #de_hele_dump_van_twee_documenten_staat_vast}. */
    private static final Path PINMAP = Path.of("src", "test", "resources", "sessiedump");

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
