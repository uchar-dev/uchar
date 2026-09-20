package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ENUMERATION VALUE as this backend reads it off the wire — both transpiler branches, both positions.
 *
 * <p><b>What was wrong.</b> The shared reader {@code Literalen.tekstOfEnumLabel} routed an enumeration
 * literal through {@code enumLabel}, which demands the {@code '…'} delimiters of the RegelSpraak source.
 * The wire never carries them: IR-SPEC ch. 4.1 puts the BARE label in {@code waarde} and WF-039 crosses it
 * byte-exactly against the declared domain. So on every conforming document the reader answered null, and
 * the FIVE arms that read through it refused the whole rule: FOUR in the exec-model branch — the
 * own-attribute condition arm, the navigated one, the assertie arm, and the VALUE arm that turns a written
 * literal into a String constant — and ONE in the DRL-text branch, whose general literal lowering serves
 * the condition and the value position alike. Both branches therefore refused every enumeration EQUALITY
 * and every enumeration WRITE.
 *
 * <p><b>Red first, measured 2026-09-20 on the pre-fix jar</b> (card `drools-enumlabel-lezer`):
 * <pre>
 *   jutterswet-keurstaat    exec : EXCLUDE transpiler SKIP: regel: veilbaarheid naar staat — regel
 *                                  (voorwaarde buiten Phase-B scope)
 *                           drl  : EXCLUDE drl-transpiler MARKER: regel: veilbaarheid naar staat —
 *                                  TODO voorwaarde: haar staat gelijk is aan 'gaaf'
 *   jutterswet-vondstklasse exec : EXCLUDE transpiler SKIP: regel: bestemming van drijfgoed — regel
 *                                  (voorwaarde buiten Phase-B scope)
 *                           drl  : EXCLUDE drl-transpiler MARKER: regel: bestemming van drijfgoed —
 *                                  TODO voorwaarde: zijn klasse gelijk is aan 'drijfgoed'
 * </pre>
 * and on the same document with that condition removed, which is how the WRITE half was separated from the
 * equality half: exec {@code regel (waarde buiten scope)}, DRL-text {@code TODO literal: wrakgoed}. The
 * conditionless row below is that measurement kept.
 *
 * <p><b>Why this class and not the gate alone.</b> Gate 12 replays the two goldens and would go red if
 * either lost its PLAYED channel — but only as a count and a channel name. These rows name the FORM, hold
 * both branches to it, and (the mutation rows) turn "agrees with the expectation" into "wrote the label":
 * a branch that silently stopped writing leaves an empty attribute, which no positive-value expectation
 * survives, while a branch that refuses the document throws {@link CaseRunner.Excluded} out of here by name.
 */
class EnumLabelWireWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** The enum equality on the subject's OWN attribute, both polarities, plus a literal WRITE under a
     *  kenmerk condition and an attribute-to-attribute copy. Hand-written IR. */
    private static final String KEURSTAAT = "jutterswet-keurstaat";

    /** The same two positions emitted from a {@code bron.rgs} (card v8-enum-emitter): the literal in the
     *  write target and in the comparison, and a derived enum read back as a comparison operand. */
    private static final String VONDSTKLASSE = "jutterswet-vondstklasse";

    @TempDir Path tmp;

    // ---------------- 1. both goldens, both branches, every case ----------------

    @Test void beide_takken_spelen_de_twee_enumgoldens() throws Exception {
        for (String naam : List.of(KEURSTAAT, VONDSTKLASSE)) {
            Path ir = CORPUS.resolve(naam).resolve("ir.json");
            List<Path> cases = casesVan(CORPUS.resolve(naam));
            assertFalse(cases.isEmpty(), naam + " hoort casebestanden te dragen");

            CaseRunner.Result exec = CaseRunner.viaExecModel(ir, cases);
            assertEquals("PLAYED", exec.status(), naam + " hoort door de exec-model-tak gespeeld te worden: "
                    + exec.reason());
            assertEquals(cases.size(), exec.casesPlayed(), naam + ": elke casus hoort gespeeld te zijn");

            CaseRunner.Result drl = CaseRunner.viaDrlTekst(ir, cases);
            assertEquals("PLAYED", drl.status(), naam + " hoort door de DRL-tekst-tak even goed gespeeld te "
                    + "worden: " + drl.reason());
            assertEquals(cases.size(), drl.casesPlayed(), naam + ": elke casus hoort gespeeld te zijn");
        }
    }

    /**
     * De mutatiekant van de rij hierboven: één GESCHREVEN enumwaarde per golden omgedraaid naar een ANDERE
     * waarde van hetzelfde domein. Beide takken horen de mutant waarneembaar te verwerpen — pas dan zegt het
     * PLAYED hierboven dat het label geschreven is en niet alleen dat er niets stukging.
     */
    @Test void een_gemuteerde_geschreven_enumwaarde_wordt_door_beide_takken_verworpen() throws Exception {
        // keurstaat: `stempel` krijgt de literal 'gaaf' onder een kenmerkvoorwaarde (de VALUE-arm).
        BeideTakken.rood(CORPUS.resolve(KEURSTAAT).resolve("ir.json"),
                metEnumVerwachting(KEURSTAAT, "case1-gaaf.json", "stempel", "wrakhout"),
                "werkelijk gaaf", "de geschreven literal is 'gaaf' en geen andere waarde van het domein");
        // vondstklasse: `bestemming` krijgt 'wrakgoed' onder een enumgelijkheid (VALUE-arm + conditie-arm).
        BeideTakken.rood(CORPUS.resolve(VONDSTKLASSE).resolve("ir.json"),
                metEnumVerwachting(VONDSTKLASSE, "case1-drijfgoed.json", "bestemming", "strandgoed"),
                "werkelijk wrakgoed", "de geschreven literal is 'wrakgoed'");
    }

    // ---------------- 2. the WRITE arm on its own, without an equality above it ----------------

    /**
     * De twee helften van de weigering zaten in elkaar: de conditie-arm ketste als eerste af, dus het PLAYED
     * hierboven bewijst de WAARDE-arm alleen omdat dezelfde regel beide draagt. Deze rij haalt ze uit elkaar
     * door de voorwaarde van R1 weg te nemen — precies het document waarop de weigering van de waarde-arm
     * vóór de fix gemeten is ({@code regel (waarde buiten scope)} / {@code TODO literal: wrakgoed}). Case 1
     * houdt exact dezelfde verwachtingen: R2 vuurt er niet (haar klasse IS drijfgoed), dus R1 schrijft
     * onvoorwaardelijk wat zij daar ook voorwaardelijk schreef.
     */
    @Test void de_enumtoekenning_speelt_ook_zonder_haar_voorwaarde_in_beide_takken() throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(
                Files.readString(CORPUS.resolve(VONDSTKLASSE).resolve("ir.json")));
        boolean geraakt = false;
        for (JsonNode r : ir.withArray("resolvedRegels"))
            if ("bestemming van drijfgoed".equals(r.path("naam").asText())) {
                ((ObjectNode) r).putNull("voorwaarde");
                geraakt = true;
            }
        assertTrue(geraakt, "de regel die de literal schrijft hoort in het document te staan");
        Path zonderVoorwaarde = tmp.resolve("vondstklasse-zonder-voorwaarde.json");
        Files.writeString(zonderVoorwaarde, ir.toPrettyString());

        BeideTakken.groen(zonderVoorwaarde, geval(VONDSTKLASSE, "case1-drijfgoed.json"),
                "de geschreven enumliteral staat op zichzelf, in beide takken");
    }

    // ---------------- 3. the read-back identity: a declared NAME, not a byte string ----------------

    /**
     * Een enumeratiewaarde is een GEDECLAREERDE NAAM en ref vergelijkt die tot op {@code Checks.norm}
     * ({@code arith.ml}: {@code Enum a, Enum b -> norm a = norm b}); de ASP-backend legt het genormaliseerde
     * label zelfs vast. {@code jutterswet-keurstaat} spelt haar invoer daarom met opzet anders dan haar
     * declaratie ("Beschadigd" tegen een gedeclareerde "beschadigd") — de bean houdt de spelling van de
     * casus, net als ref, en de leesterug moet hem tot op de normalisatie gelijk noemen. Tot deze kaart was
     * die arm onbereikbaar: de lezer weigerde het hele document.
     *
     * <p>De rij heeft twee kanten, want een norm-vergelijking die te ver gaat leest alles gelijk: de
     * hoofdlettervariant hoort te HALEN en een ANDERE waarde van hetzelfde domein hoort te FALEN.
     */
    @Test void de_leesterug_van_een_enumwaarde_is_naamidentiteit_in_beide_takken() throws Exception {
        Path ir = CORPUS.resolve(KEURSTAAT).resolve("ir.json");
        BeideTakken.groen(ir, metEnumVerwachting(KEURSTAAT, "case2-beschadigd.json", "oordeel", "BESCHADIGD"),
                "dezelfde naam in een derde spelling blijft dezelfde waarde");
        BeideTakken.rood(ir, metEnumVerwachting(KEURSTAAT, "case2-beschadigd.json", "oordeel", "wrakhout"),
                "werkelijk Beschadigd", "een ANDERE waarde van het domein hoort waarneembaar te falen");
    }

    /**
     * En de tekstkant van diezelfde vergelijker, zodat de normalisatie hierboven niet ongemerkt naar
     * {@code Tekst} lekt: §13.3.3.4 geeft een tekstwaarde EXACTE waardegelijkheid, dus dezelfde verwachting
     * in een andere hoofdletterspelling hoort daar juist te FALEN. De golden is de tekstzuster van
     * keurstaat.
     */
    @Test void een_tekstwaarde_houdt_haar_exacte_gelijkheid() throws Exception {
        String golden = "jutterswet-vondstetiket";
        JsonNode origineel = geval(golden, "case1-zeevondst.json");
        String tekst = tekstVerwachting(origineel);
        BeideTakken.rood(CORPUS.resolve(golden).resolve("ir.json"),
                metTekstVerwachting(origineel, tekst.toUpperCase(java.util.Locale.ROOT)),
                "werkelijk " + tekst,
                "een Tekst vergelijkt byte-exact — de enumnormalisatie hoort daar niet te gelden");
    }

    // ---------------- fixtures ----------------

    private static List<Path> casesVan(Path golden) throws Exception {
        try (java.util.stream.Stream<Path> s = Files.list(golden)) {
            return s.filter(p -> p.getFileName().toString().startsWith("case")).sorted().toList();
        }
    }

    private JsonNode geval(String naam, String caseNaam) throws Exception {
        return JSON.readTree(Files.readString(CORPUS.resolve(naam).resolve(caseNaam)));
    }

    /** De casus met ÉÉN enumverwachting vervangen — de populatie blijft onaangeroerd, zodat een verschil
     *  alleen van de geschreven of teruggelezen waarde kan komen. */
    private JsonNode metEnumVerwachting(String naam, String caseNaam, String attribuut, String waarde)
            throws Exception {
        ObjectNode c = (ObjectNode) geval(naam, caseNaam);
        boolean geraakt = false;
        for (JsonNode e : c.withArray("expected"))
            if ("attributeValue".equals(e.path("kind").asText())
                    && attribuut.equals(e.path("attribute").asText())
                    && e.path("value").has("enumeration")) {
                ((ObjectNode) e.get("value")).put("enumeration", waarde);
                geraakt = true;
            }
        assertTrue(geraakt, naam + "/" + caseNaam + " hoort een enumverwachting op '" + attribuut
                + "' te dragen — anders muteert deze rij niets");
        return c;
    }

    /** De eerste tekstverwachting van een casus, en haar vervanger — de tekstzuster van de twee hulpjes
     *  hierboven, apart omdat het veld {@code text} heet en niet {@code enumeration}. */
    private static String tekstVerwachting(JsonNode c) {
        for (JsonNode e : c.get("expected"))
            if ("attributeValue".equals(e.path("kind").asText()) && e.path("value").has("text"))
                return e.get("value").get("text").asText();
        throw new IllegalStateException("de casus draagt geen tekstverwachting");
    }

    private JsonNode metTekstVerwachting(JsonNode origineel, String waarde) {
        ObjectNode c = origineel.deepCopy();
        for (JsonNode e : c.withArray("expected"))
            if ("attributeValue".equals(e.path("kind").asText()) && e.path("value").has("text")) {
                ((ObjectNode) e.get("value")).put("text", waarde);
                return c;
            }
        throw new IllegalStateException("de casus draagt geen tekstverwachting");
    }
}
