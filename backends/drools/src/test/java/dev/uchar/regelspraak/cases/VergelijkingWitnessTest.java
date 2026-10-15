package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.engine.ScenarioResultaat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.kie.api.runtime.KieSession;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The comparison of the expectations of a case with a session, through the backend's public entry:
 * {@code DroolsBackend.vergelijk(build, session, label, expected)}, after {@code vuur} and
 * {@code zetAfgeleideWaarden}. A consumer that fires a session itself, as the distribution's decision service does,
 * gets the verdict the scenario report gives ({@code CaseRunner.rapport}, whose replay compares through the same entry)
 * without a copy of the comparison. Every session here is set up as the replay sets it up ({@code CaseRunner.metSessie})
 * and fired, written back and compared through the entry alone.
 *
 * <p>The rows pin what the report cannot show: that the labels are the caller's, given by a function from bean to label
 * as a decision service gives the ids of its request, and need not be those of the case file; and the refusals of the
 * entry, of a session that has no answer to compare or that changed after its fixpoint, of a label function that gives
 * one label to two beans, and of an expectation on a label no bean of the session carries, where the replay read "leeg"
 * or "afwezig" until this entry and so let attributeEmpty and characteristicAbsent hold on a label that names nothing.
 */
class VergelijkingWitnessTest {

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final Path KUSTWACHT = GOLDEN.resolve("anchor").resolve("jutterswet-kustwacht");
    private static final Path KUSTREGISTER = GOLDEN.resolve("anchor").resolve("jutterswet-kustregister");
    private static final Path BORGDEEL = GOLDEN.resolve("anchor").resolve("jutterswet-borgdeel");
    /** The labels of a decision service: the ids of its request, here the case label after this prefix. */
    private static final String VERZOEK = "verzoek/";

    /** Played goldens whose cases together carry every kind of {@code expected}: attribute values (input and derived,
     *  numbers and dates), attributeEmpty, attributeTimeline, characteristics present and absent (derived and not),
     *  consistent, inconsistent bare, with a rule, and with a rule and a label, also where a deposit of the rule lies on
     *  an object a creation made, which no label names. */
    private static final List<String[]> GEVALLEN = List.of(
            new String[]{"anchor/jutterswet-kustwacht", "case1.json"},
            new String[]{"anchor/jutterswet-jutleeftijd", "case3-leeg.json"},
            new String[]{"anchor/jutterswet-borgdeel", "case1.json"},
            new String[]{"anchor/jutterswet-kustregister", "case2.json"},
            new String[]{"anchor/jutterswet-wachtersketen", "case1.json"},
            new String[]{"anchor/jutterswet-wachtersketen", "case2.json"},
            new String[]{"jutterswet-meldingsketen", "case2-herstelde-melding.json"});

    private static JsonNode lees(Path pad) throws Exception {
        return TmsDocumenten.JSON.readTree(pad.toFile());
    }

    /** The label {@code id} gives the case label of an input bean, by identity, and null for any other object: the
     *  function a decision service gives with the ids of its request. */
    private static Function<Object, String> labelfunctie(Map<String, Object> labels, UnaryOperator<String> id) {
        Map<Object, String> perBoon = new IdentityHashMap<>();
        labels.forEach((l, boon) -> perBoon.put(boon, id.apply(l)));
        return perBoon::get;
    }

    /** {@code verwachtingen} with every label {@code l} replaced by {@code id(l)}. */
    private static JsonNode herlabeld(JsonNode verwachtingen, UnaryOperator<String> id) {
        JsonNode uit = verwachtingen.deepCopy();
        for (JsonNode v : uit)
            if (v.hasNonNull("label")) ((ObjectNode) v).put("label", id.apply(v.get("label").asText()));
        return uit;
    }

    /** Case {@code c} of {@code golden} on {@code tak}, set up as the replay sets it up, fired and written back through
     *  the entry with the labels {@code id} gives, and compared with {@code verwachtingen}. */
    private static DroolsBackend.Vergelijking vergelijk(Path golden, JsonNode c, DroolsBackend.Branch tak,
                                                        UnaryOperator<String> id, JsonNode verwachtingen) throws Exception {
        return CaseRunner.voorMeting(golden.resolve("ir.json"), tak).metSessie(c, (bouw, sessie, labels) -> {
            Function<Object, String> label = labelfunctie(labels, id);
            assertNull(DroolsBackend.vuur(bouw, sessie).geenFixpunt(), tak + ": the fire reaches its fixpoint");
            DroolsBackend.zetAfgeleideWaarden(bouw, sessie, label);
            return DroolsBackend.vergelijk(bouw, sessie, label, verwachtingen);
        });
    }

    /** What the entry throws on case {@code c} of {@code golden}, set up as the replay sets it up, after {@code voor}
     *  did what it does with the session, with the labels {@code label} makes of the case labels. */
    private static <X extends Throwable> X weigering(Class<X> soort, Path golden, JsonNode c,
                                                     Function<Map<String, Object>, Function<Object, String>> label,
                                                     JsonNode verwachtingen, Voorbereiding voor) throws Exception {
        return CaseRunner.voorMeting(golden.resolve("ir.json"), DroolsBackend.Branch.EXEC_MODEL).metSessie(c,
                (bouw, sessie, labels) -> {
                    Function<Object, String> l = label.apply(labels);
                    voor.doe(bouw, sessie, l);
                    return assertThrows(soort, () -> DroolsBackend.vergelijk(bouw, sessie, l, verwachtingen));
                });
    }

    @FunctionalInterface
    private interface Voorbereiding {
        void doe(DroolsBackend.Build bouw, KieSession sessie, Function<Object, String> label);
    }

    private static final Voorbereiding GEVUURD_EN_TERUGGEZET = (bouw, sessie, label) -> {
        assertNull(DroolsBackend.vuur(bouw, sessie).geenFixpunt());
        DroolsBackend.zetAfgeleideWaarden(bouw, sessie, label);
    };

    private static Function<Map<String, Object>, Function<Object, String>> caselabels() {
        return labels -> labelfunctie(labels, l -> l);
    }

    private static ScenarioResultaat rapport(Path golden, Path casus) {
        return CaseRunner.rapport(golden.resolve("ir.json"), List.of(casus)).get(0);
    }

    /** The diagnose of the report without the name of the case file and ": " before it. */
    private static String diagnose(ScenarioResultaat r) {
        return r.diagnose().isEmpty() ? "" : r.diagnose().substring(r.naam().length() + 2);
    }

    @Test void de_ingang_geeft_op_beide_takken_wat_het_rapport_geeft() throws Exception {
        Set<String> soorten = new TreeSet<>();
        for (String[] geval : GEVALLEN) {
            Path golden = GOLDEN.resolve(geval[0]);
            JsonNode c = lees(golden.resolve(geval[1]));
            for (JsonNode v : c.get("expected"))
                soorten.add(v.get("kind").asText() + (v.has("rule") ? "+rule" : "") + (v.has("label")
                        && v.get("kind").asText().equals("inconsistent") ? "+label" : ""));
            ScenarioResultaat r = rapport(golden, golden.resolve(geval[1]));
            assertEquals(ScenarioResultaat.Status.PASS, r.status(), geval[0] + "/" + geval[1]);
            for (DroolsBackend.Branch tak : DroolsBackend.Branch.values()) {
                DroolsBackend.Vergelijking g = vergelijk(golden, c, tak, l -> l, c.get("expected"));
                assertEquals(r.verwachtingen(), g.verwachtingen(), tak + " " + geval[0] + "/" + geval[1]);
                assertEquals("", g.diagnose(), tak + " " + geval[0] + "/" + geval[1]);
                assertTrue(g.pass(), tak + " " + geval[0] + "/" + geval[1]);
            }
        }
        assertEquals(Set.of("attributeEmpty", "attributeTimeline", "attributeValue", "characteristicAbsent",
                "characteristicPresent", "consistent", "inconsistent", "inconsistent+rule",
                "inconsistent+rule+label"), soorten, "the rows carry every kind of expected");
    }

    /** A decision service names the beans of its session by the ids of its request: the labels of the case file are
     *  not in the session. With the expectations in the ids the verdict is the report's, up to the labels, the subject
     *  of a §9.5 deposit among them; with the labels of the case file the entry finds no bean and says so. */
    @Test void de_labels_zijn_die_van_de_aanroeper() throws Exception {
        JsonNode c = lees(KUSTREGISTER.resolve("case2.json"));
        ScenarioResultaat r = rapport(KUSTREGISTER, KUSTREGISTER.resolve("case2.json"));
        UnaryOperator<String> id = l -> VERZOEK + l;
        for (DroolsBackend.Branch tak : DroolsBackend.Branch.values()) {
            DroolsBackend.Vergelijking g = vergelijk(KUSTREGISTER, c, tak, id, herlabeld(c.get("expected"), id));
            assertEquals(List.of("attributeValue verzoek/G1 kavelaantal", "inconsistent (eenmalig vondstnummer @ verzoek/S1)",
                    "inconsistent (eenmalig vondstnummer @ verzoek/W1)"),
                    g.verwachtingen().stream().map(ScenarioResultaat.VerwachtingResultaat::bron).toList(), tak::name);
            assertEquals("inconsistent (eenmalig vondstnummer @ verzoek/S1, eenmalig vondstnummer @ verzoek/W1)",
                    g.verwachtingen().get(1).werkelijk(), tak + ": the subject of a deposit has the caller's label");
            List<ScenarioResultaat.VerwachtingResultaat> zonderVerzoek = g.verwachtingen().stream()
                    .map(v -> new ScenarioResultaat.VerwachtingResultaat(v.bron().replace(VERZOEK, ""),
                            v.verwacht().replace(VERZOEK, ""), v.werkelijk().replace(VERZOEK, ""), v.ok(), v.positie()))
                    .toList();
            assertEquals(r.verwachtingen(), zonderVerzoek, tak::name);
            assertTrue(g.pass(), tak::name);

            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> vergelijk(KUSTREGISTER, c, tak, id, c.get("expected")));
            assertEquals("attributeValue-verwachting op onbekend label 'G1': geen bean van de sessie draagt het, dus er"
                    + " is niets te lezen", e.getMessage(), tak::name);
        }
    }

    /** An expectation that does not hold: the verdict per expectation and the diagnose the report gives for the same
     *  case file, after the name of the file. */
    @Test void een_afwijking_geeft_de_diagnose_van_het_rapport(@TempDir Path tmp) throws Exception {
        ObjectNode c = (ObjectNode) lees(KUSTWACHT.resolve("case1.json"));
        ((ObjectNode) c.get("expected").get(0)).putObject("value").put("number", "16");
        ((ObjectNode) c.get("expected").get(7)).put("kind", "characteristicPresent");
        Path bestand = Files.writeString(tmp.resolve("case-afwijkend.json"), c.toString());
        ScenarioResultaat r = rapport(KUSTWACHT, bestand);
        assertEquals(ScenarioResultaat.Status.FAIL, r.status());
        for (DroolsBackend.Branch tak : DroolsBackend.Branch.values()) {
            DroolsBackend.Vergelijking g = vergelijk(KUSTWACHT, c, tak, l -> l, c.get("expected"));
            assertFalse(g.pass(), tak::name);
            assertEquals("attribuut uitkijkbonus: verwacht 16, werkelijk 15; kenmerk risicopost: verwacht aanwezig,"
                    + " werkelijk afwezig", g.diagnose(), tak::name);
            assertEquals(diagnose(r), g.diagnose(), tak::name);
            assertEquals(new ScenarioResultaat.VerwachtingResultaat("attributeValue W1 uitkijkbonus", "16", "15", false, 1),
                    g.verwachtingen().get(0), tak + ": the expected side, then the actual one");
            assertEquals(r.verwachtingen(), g.verwachtingen(), tak::name);
            assertEquals(List.of(1, 8), g.verwachtingen().stream().filter(v -> !v.ok())
                    .map(ScenarioResultaat.VerwachtingResultaat::positie).toList(), tak::name);
        }
    }

    /** The beans carry the fixpoint only after the write-back: before it the entry refuses, as it refuses what
     *  {@code lees} refuses, a session not fired and one updated after its write-back. */
    @Test void een_sessie_zonder_antwoord_wordt_geweigerd() throws Exception {
        JsonNode c = lees(KUSTWACHT.resolve("case1.json"));
        IllegalStateException nietTeruggezet = weigering(IllegalStateException.class, KUSTWACHT, c, caselabels(),
                c.get("expected"), (bouw, sessie, label) -> assertNull(DroolsBackend.vuur(bouw, sessie).geenFixpunt()));
        assertEquals("vergelijk: deze sessie is niet teruggezet; de beans dragen het fixpunt pas na"
                + " DroolsBackend.zetAfgeleideWaarden, en tot dan alleen de invoer, dus zet eerst terug",
                nietTeruggezet.getMessage());

        IllegalStateException nietGevuurd = weigering(IllegalStateException.class, KUSTWACHT, c, caselabels(),
                c.get("expected"), (bouw, sessie, label) -> { });
        assertTrue(nietGevuurd.getMessage().startsWith("geen fixpunt om te lezen of terug te zetten: deze sessie is niet"
                + " via DroolsBackend.vuur gevuurd"), nietGevuurd.getMessage());

        IllegalStateException bijgewerkt = weigering(IllegalStateException.class, KUSTWACHT, c, caselabels(),
                c.get("expected"), (bouw, sessie, label) -> {
                    GEVUURD_EN_TERUGGEZET.doe(bouw, sessie, label);
                    Object boon = sessie.getObjects(o -> "W1".equals(label.apply(o))).iterator().next();
                    sessie.update(sessie.getFactHandle(boon), boon);
                });
        assertTrue(bijgewerkt.getMessage().contains("gebeurde er iets buiten vuur om"), bijgewerkt.getMessage());
    }

    /** A fault at the fixpoint: the rules give no answer, the write-back throws instead of writing, and the entry says
     *  so with the fault, where it would otherwise report "not written back". */
    @Test void een_evaluatiefout_aan_het_fixpunt_wordt_genoemd() throws Exception {
        Path deling = Path.of("..", "..", "corpus", "rood", "division-by-numeric-zero");
        JsonNode c = lees(deling.resolve("case1.json"));
        IllegalStateException e = weigering(IllegalStateException.class, deling, c, caselabels(),
                TmsDocumenten.JSON.readTree("[{\"kind\": \"consistent\"}]"),
                (bouw, sessie, label) -> assertNull(DroolsBackend.vuur(bouw, sessie).geenFixpunt()));
        assertTrue(e.getMessage().startsWith("vergelijk: aan het fixpunt van deze sessie staan 1 evaluatiefout(en), de"
                + " eerste van 'deel van een vondst': deling door nul in de regel \"deel van een vondst\""), e.getMessage());
    }

    @Test void een_label_van_twee_beans_en_een_object_als_verwachtingen_worden_geweigerd() throws Exception {
        JsonNode c = lees(KUSTWACHT.resolve("case1.json"));
        IllegalArgumentException dubbel = weigering(IllegalArgumentException.class, KUSTWACHT, c,
                labels -> o -> labels.containsValue(o) ? "dienst" : null, c.get("expected"), GEVUURD_EN_TERUGGEZET);
        assertTrue(dubbel.getMessage().startsWith("vergelijk: de labelfunctie geeft het label 'dienst' aan "),
                dubbel.getMessage());
        assertTrue(dubbel.getMessage().endsWith(", dus een verwachting op 'dienst' wijst geen bean aan"),
                dubbel.getMessage());

        IllegalArgumentException geenArray = weigering(IllegalArgumentException.class, KUSTWACHT, c, caselabels(), c,
                GEVUURD_EN_TERUGGEZET);
        assertEquals("vergelijk: de verwachtingen zijn de array 'expected' van een case, en dit is een OBJECT",
                geenArray.getMessage());
    }

    /** An expectation on a member of a label no bean of the session carries has nothing to read: refused, for each of the
     *  four kinds, in the sentence the timeline kind gave already. Until this entry the replay read "leeg" and "afwezig"
     *  there, so attributeEmpty and characteristicAbsent held on a label that names nothing. */
    @Test void een_onbekend_label_wordt_geweigerd() throws Exception {
        JsonNode c = lees(KUSTWACHT.resolve("case1.json"));
        for (String verwachting : List.of(
                "{\"kind\": \"attributeValue\", \"label\": \"W9\", \"attribute\": \"uitkijkbonus\", \"value\": {\"number\": \"15\"}}",
                "{\"kind\": \"attributeEmpty\", \"label\": \"W9\", \"attribute\": \"uitkijkbonus\"}",
                "{\"kind\": \"characteristicPresent\", \"label\": \"W9\", \"characteristic\": \"rustwacht\"}",
                "{\"kind\": \"characteristicAbsent\", \"label\": \"W9\", \"characteristic\": \"rustwacht\"}")) {
            JsonNode v = TmsDocumenten.JSON.readTree("[" + verwachting + "]");
            IllegalArgumentException e = weigering(IllegalArgumentException.class, KUSTWACHT, c, caselabels(), v,
                    GEVUURD_EN_TERUGGEZET);
            assertEquals(v.get(0).get("kind").asText() + "-verwachting op onbekend label 'W9': geen bean van de sessie"
                    + " draagt het, dus er is niets te lezen", e.getMessage());
        }
    }

    /** What the backend does not compare, the entry refuses with the exclusion of the replay, which the report gives as
     *  a MARKER with the same sentence: a scalar read of an attribute on a timeline, and a kind uchar-case-v0 does not
     *  have. */
    @Test void wat_de_ingang_niet_vergelijkt_weigert_hij_zoals_het_rapport(@TempDir Path tmp) throws Exception {
        JsonNode c = lees(BORGDEEL.resolve("case1.json"));
        Map<String, String> zinnen = Map.of(
                "{\"kind\": \"attributeValue\", \"label\": \"J1\", \"attribute\": \"borgdeel\", \"value\": {\"number\": \"12\"}}",
                "scalar read-back of temporal attribute 'borgdeel' is not carried by the v0 backend — the bean holds a"
                        + " List<Segment>; use an attributeTimeline expectation",
                "{\"kind\": \"attributeRange\", \"label\": \"J1\", \"attribute\": \"borgdeel\"}",
                "unknown expectation kind: attributeRange");
        for (Map.Entry<String, String> z : zinnen.entrySet()) {
            JsonNode v = TmsDocumenten.JSON.readTree("[" + z.getKey() + "]");
            CaseRunner.Excluded e = weigering(CaseRunner.Excluded.class, BORGDEEL, c, caselabels(), v,
                    GEVUURD_EN_TERUGGEZET);
            assertEquals(z.getValue(), e.getMessage());
            ObjectNode metVerwachting = c.deepCopy();
            metVerwachting.set("expected", v);
            ScenarioResultaat r = rapport(BORGDEEL, Files.writeString(tmp.resolve("case-" + z.getValue().length()
                    + ".json"), metVerwachting.toString()));
            assertEquals(ScenarioResultaat.Status.MARKER, r.status());
            assertEquals(z.getValue(), r.diagnose());
        }
    }
}
