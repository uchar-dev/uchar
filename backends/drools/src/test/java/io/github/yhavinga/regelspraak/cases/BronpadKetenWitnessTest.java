package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The WF-102 fold-source PATH, walked by BOTH Drools transpiler branches (IR-SPEC ch. 4.3,
 * RUNTIME-SEMANTICS ch. 3.4b).
 *
 * <p><b>Why the model is authored here instead of read from the corpus.</b> The three corpus goldens that
 * carry a {@code sourcePath} pin ONE shape between them — two to-many hops, i.e. a walk that is functional
 * from the element in every step — and two of the three are not replayable by this backend at all (their
 * middle relation is many-to-many, so §13.3.9 derives no bean field at either end and the walk refuses by
 * name). Everything this card had to BUILD beyond that one shape therefore has no corpus witness: the
 * to-one/to-many mix, the DEDUP that mix makes necessary, the subtype landing, and the refusal to fall
 * back on a coincidental direct relation. Each row below is that missing witness, with values computed
 * from the SENTENCE by hand before anything was run.
 *
 * <p><b>The model.</b> A {@code Tolhuis} manages {@code Sluis}en (to-many); a {@code Sluis} locks
 * {@code Praam}en through (to-many) and uses ONE {@code Ankersteen} (to-one, shared); a {@code Praam}
 * carries {@code Vat}en (to-many). Beside all of that runs a DIRECT {@code Tolhuis}-{@code Praam}
 * registration, which no rule here may ever fold. The five rules are one fold each:
 *
 * <ol>
 *   <li>{@code laagste vracht} — MINIMUM over the two-hop path, both hops to-many;</li>
 *   <li>{@code totale vracht} — SOM with an "of 0" fallback over the same path;</li>
 *   <li>{@code steenlast} — SOM over to-many THEN to-one, the mix in which two intermediates reach ONE
 *       element and the flat union must count it once;</li>
 *   <li>{@code laagste zegelvracht} — the same two-hop path landing on a strict SUBTYPE of what the last
 *       role fills;</li>
 *   <li>{@code laagste vatinhoud} — three hops, so a dropped or reordered step is observable.</li>
 * </ol>
 *
 * <p><b>The hand computation</b> (case {@code de-keten}; T = Tolhuis, S = Sluis, P = Praam, A =
 * Ankersteen, V = Vat):
 *
 * <pre>
 *   T1 -> {S1, S2};  S1 -> {P1(3,00), P2(7,00)};  S2 -> {P3(5,00, een Zegelpraam), P5(geen vracht)}
 *                    S1.ankersteen = A1(12,00);   S2.ankersteen = A1   <- ONE steen, TWO routes
 *                    P1 -> {V1(2,50), V2(9,00)};  P2 -> {V3(6,00)}
 *                    T1 registreert daarnaast RECHTSTREEKS P4(1,00)    <- nooit in het bereik
 *   T2 -> {S3};      S3 heeft geen pramen;        S3.ankersteen = A2(4,00)
 *   T3 -> {}         leeg bij de EERSTE stap
 *   T4 -> {S4};      S4 -> {P6(geen vracht)}      de lege bijdrager als enige
 *
 *   laagste vracht      T1 = 3,00   T2 = leeg   T3 = leeg   T4 = leeg
 *   totale vracht       T1 = 15,00  T2 = 0      T3 = 0      T4 = 0
 *   steenlast           T1 = 12,00  T2 = 4,00   T3 = 0      T4 = 0
 *   laagste zegelvracht T1 = 5,00   T2 = leeg   T3 = leeg   T4 = leeg
 *   laagste vatinhoud   T1 = 2,50   T2 = leeg   T3 = leeg   T4 = leeg
 * </pre>
 *
 * <p>Every one of those cells is chosen to be WRONG under a specific defect: 3,00/15,00 become 1,00/1,00
 * if the direct registration is folded instead of the path; 12,00 becomes 24,00 if the shared steen is
 * counted per route; 5,00 becomes 3,00 if the landing ignores the subtype; 2,50 moves if a hop is dropped.
 */
class BronpadKetenWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir Path tmp;

    // ---------------- the canonical hops ----------------

    private static final String PAD_VRACHT = stap("sluisbeheer", "beheerde sluis") + ", "
            + stap("schutting", "geschutte praam");
    private static final String PAD_STEEN = stap("sluisbeheer", "beheerde sluis") + ", "
            + stap("verankering", "gebruikte ankersteen");
    private static final String PAD_VAT = stap("sluisbeheer", "beheerde sluis") + ", "
            + stap("schutting", "geschutte praam") + ", " + stap("lading", "geladen vat");

    private static String stap(String feittype, String rol) {
        return "{ \"node\": \"RolBinding\", \"feittype\": \"" + feittype + "\", \"rol\": \"" + rol + "\" }";
    }

    // ---------------- 1-5: the five readings, both branches ----------------

    /**
     * The whole hand table above, in one replay per branch. It carries rows 1 (the MIN and the SOM,
     * including the contributor whose own value is leeg), 2 (an intermediate that reaches nothing, and a
     * subject that reaches no intermediate at all), 3 (one element, two routes), 4 (the direct relation
     * that must not be folded) and 5 (the subtype landing) at once, because they are readings of ONE
     * document and separating them would mean four more models that say the same thing.
     */
    @Test void de_vlakke_unie_over_het_pad_in_beide_takken() throws Exception {
        BeideTakken.groen(ir(doc()), keten(), "de vlakke unie over het sourcePath");
    }

    /**
     * The dedup is not an accident of the model: with the shared steen counted per ROUTE the answer is
     * 24,00, and that is what an accumulate source joining the intermediate would produce. This row plays
     * the same document against that wrong number and demands BOTH branches reject it, so row 1's 12,00 is
     * a measurement rather than a coincidence.
     */
    @Test void dezelfde_eindinstantie_via_twee_routes_telt_niet_dubbel() throws Exception {
        String perRoute = KETEN.replace(
                "\"label\": \"T1\", \"attribute\": \"steenlast\", \"value\": { \"number\": \"12.00\" }",
                "\"label\": \"T1\", \"attribute\": \"steenlast\", \"value\": { \"number\": \"24.00\" }");
        assertTrue(perRoute.contains("24.00"), "de mutatie hoort de gedeelde steen dubbel te tellen");
        JsonNode c = JSON.readTree(perRoute);
        BeideTakken.rood(ir(doc()), c, "steenlast",
                "24,00 is de per-route-som; de vlakke unie telt A1 EEN keer");
    }

    /**
     * The flank of row 4: the same document with the path REPLACED by the direct registration's one-step
     * binding — the shape every consumer would derive if it dropped the field. It answers 1,00 where the
     * sentence says 3,00, which is exactly why the loader may not ignore {@code sourcePath}. Played against
     * the UNCHANGED expectations, so a failure here is the whole point.
     */
    @Test void de_directe_relatie_geeft_een_ander_antwoord_dan_het_pad() throws Exception {
        String metBronRol = doc().replace("\"sourcePath\": [ " + PAD_VRACHT + " ]",
                "\"sourceRole\": { \"node\": \"RolBinding\", \"feittype\": \"praamregister\","
                        + " \"rol\": \"geregistreerde praam\" }");
        assertTrue(metBronRol.contains("sourceRole"), "de vervanging hoort het pad door de directe binding te ruilen");
        BeideTakken.rood(ir(metBronRol), keten(), "laagste vracht",
                "de directe registratie levert 1,00; alleen het pad levert de 3,00 die de zin zegt");
    }

    // ---------------- the walk's own refusals: a path the bean model cannot follow ----------------

    /** A hop removed: {@code sluisbeheer} then {@code lading} is no longer a connected walk — the second
     *  hop's opposite role is filled by a Praam and the first reaches a Sluis. */
    @Test void een_verwijderde_stap_breekt_de_wandeling() throws Exception {
        String stuk = doc().replace(PAD_VAT, stap("sluisbeheer", "beheerde sluis") + ", " + stap("lading", "geladen vat"));
        BeideTakken.geweigerd(ir(stuk), keten(), "verbonden wandeling",
                "een pad met een ontbrekende stap is geen wandeling meer");
    }

    /** Two hops swapped: the walk no longer starts at the fold subject. The steps are READ POSITIONALLY,
     *  which is what makes this observable at all — a consumer deriving the range from the endpoints would
     *  answer the same thing for both orders. */
    @Test void twee_verwisselde_stappen_breken_de_wandeling() throws Exception {
        String stuk = doc().replace(PAD_VAT, stap("schutting", "geschutte praam") + ", "
                + stap("sluisbeheer", "beheerde sluis") + ", " + stap("lading", "geladen vat"));
        BeideTakken.geweigerd(ir(stuk), keten(), "verbonden wandeling",
                "een verwisselde eerste stap begint niet bij het vouwonderwerp");
    }

    /** A path that resolves and joins but lands one type short of the element type. */
    @Test void een_pad_dat_naast_het_elementtype_landt_wordt_geweigerd() throws Exception {
        String stuk = doc().replace("\"sourcePath\": [ " + PAD_VRACHT + " ]", "\"sourcePath\": [ " + PAD_STEEN + " ]");
        assertTrue(stuk.contains(PAD_STEEN) && !stuk.equals(doc()), "de vervanging hoort het pad te ruilen");
        BeideTakken.geweigerd(ir(stuk), keten(), "landen",
                "een pad dat op Ankersteen eindigt is niet het bereik van een Praam-vouw");
    }

    /** A hop whose relation is many-to-many: §13.3.9 stores a relation in ONE scalar field, on the ONE
     *  side, and a relation with no ONE side has no field at either end. That is a per-form refusal with
     *  the SHARED sentence, never a KieBase that dies at first fire. */
    @Test void een_cardinaliteitsstrijdige_stap_wordt_geweigerd() throws Exception {
        String stuk = doc().replace(
                "{ \"node\": \"Rol\", \"naam\": \"schuttende sluis\", \"objecttypeNaam\": \"Sluis\", \"meervoud\": \"schuttende sluizen\", \"meervoudig\": false }",
                "{ \"node\": \"Rol\", \"naam\": \"schuttende sluis\", \"objecttypeNaam\": \"Sluis\", \"meervoud\": \"schuttende sluizen\", \"meervoudig\": true }");
        BeideTakken.geweigerd(ir(stuk), keten(), "opslagrichting",
                "beide zijden meervoudig: geen van beide beans draagt de relatie");
    }

    // ---------------- helpers ----------------

    private Path ir(String json) throws Exception {
        Path p = Files.createTempFile(tmp, "bronpad", ".json");
        Files.writeString(p, json);
        return p;
    }

    private static JsonNode keten() throws Exception { return JSON.readTree(KETEN); }

    private static String doc() { return DOCUMENT; }

    private static final String NUM2 =
            "{ \"node\": \"Number\", \"whole\": false, \"decimals\": 2, \"sign\": \"ONBEPAALD\" }";

    /** One fold rule: {@code <attribuut>} of a Tolhuis is {@code <kind>} of {@code <arg>} of all
     *  {@code <element>} the path reaches. */
    private static String regel(String naam, String attribuut, String kind, String element, String arg,
            String leegwaarde, String pad) {
        return """
                { "node": "Rule", "name": "%s", "versions": [ {
                  "validity": { "node": "Geldigheid", "vanaf": null, "tot": null },
                  "target": { "node": "AttribuutRef", "attribuut": "%s", "objecttype": "Tolhuis",
                              "bron": "De %s van een Tolhuis" },
                  "value": { "node": "Aggregation", "kind": "%s",
                    "arguments": [ { "node": "AttribuutRef", "attribuut": "%s", "objecttype": "%s",
                                     "bron": "de %s van alle %sen" } ],
                    "elementType": "%s",%s
                    "sourcePath": [ %s ], "type": %s, "bron": "de vouw over het pad" },
                  "initialisation": false, "presentation": "BEREKENING" } ] }"""
                .formatted(naam, attribuut, attribuut, kind, arg, element, arg, element, element,
                        leegwaarde.equals("null") ? "" : " \"emptyFallback\": " + leegwaarde + ",", pad, NUM2);
    }

    private static String objecttype(String naam, String meervoud, String supertype, String attributen) {
        return """
                { "node": "ObjectType", "name": "%s", %s"animate": false, "plural": "%s",
                  "attributes": [ %s ],
                  "characteristics": [] }"""
                .formatted(naam, supertype == null ? "" : "\"supertype\": \"" + supertype + "\", ",
                        meervoud, attributen);
    }

    /** One entry of an object type's attribute list. */
    private static String attribuut(String naam, String datatype) {
        return "{ \"node\": \"Attribute\", \"name\": \"" + naam + "\", \"datatype\": " + datatype + " }";
    }

    private static String rol(String naam, String otn, String meervoud, boolean meervoudig) {
        return "{ \"node\": \"Rol\", \"naam\": \"" + naam + "\", \"objecttypeNaam\": \"" + otn
                + "\", \"meervoud\": \"" + meervoud + "\", \"meervoudig\": " + meervoudig + " }";
    }

    private static String feittype(String naam, String a, String b) {
        return "{ \"node\": \"Feittype\", \"naam\": \"" + naam + "\", \"wederkerig\": false,"
                + " \"rollen\": [ " + a + ", " + b + " ] }";
    }

    private static final String DOCUMENT = """
            { "format": "uchar-ir-v1",
              "model": {
                "objectTypes": [
                  %s,
                  %s,
                  %s,
                  %s,
                  %s,
                  %s ],
                "factTypes": [ %s, %s, %s, %s, %s ],
                "parameters": [], "domains": [], "unitSystems": [], "dimensions": [],
                "timelines": [], "dayKinds": [] },
              "rules": {
                "resolvedRegels": [ %s ],
                "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [], "consistenties": [],
                "uniciteiten": [], "feitCreaties": [], "resolvedStartpunten": [],
                "regelGroepen": [] },
              "diagnostics": [] }"""
            .formatted(
                    objecttype("Tolhuis", "Tolhuizen", null,
                            String.join(", ", attribuut("laagste vracht", NUM2), attribuut("totale vracht", NUM2),
                                    attribuut("steenlast", NUM2), attribuut("laagste zegelvracht", NUM2),
                                    attribuut("laagste vatinhoud", NUM2))),
                    objecttype("Sluis", "Sluizen", null, ""),
                    objecttype("Praam", "Pramen", null, attribuut("vracht", NUM2)),
                    objecttype("Zegelpraam", "Zegelpramen", "Praam", ""),
                    objecttype("Ankersteen", "Ankerstenen", null, attribuut("steengewicht", NUM2)),
                    objecttype("Vat", "Vaten", null, attribuut("inhoud", NUM2)),
                    feittype("sluisbeheer", rol("tolhuis", "Tolhuis", "tolhuizen", false),
                            rol("beheerde sluis", "Sluis", "beheerde sluizen", true)),
                    feittype("schutting", rol("schuttende sluis", "Sluis", "schuttende sluizen", false),
                            rol("geschutte praam", "Praam", "geschutte pramen", true)),
                    feittype("verankering", rol("verankerde sluis", "Sluis", "verankerde sluizen", true),
                            rol("gebruikte ankersteen", "Ankersteen", "gebruikte ankerstenen", false)),
                    feittype("praamregister", rol("registerhouder", "Tolhuis", "registerhouders", false),
                            rol("geregistreerde praam", "Praam", "geregistreerde pramen", true)),
                    feittype("lading", rol("ladende praam", "Praam", "ladende pramen", false),
                            rol("geladen vat", "Vat", "geladen vaten", true)),
                    String.join(", ",
                            regel("laagste vracht", "laagste vracht", "MINIMUM", "Praam", "vracht", "null", PAD_VRACHT),
                            regel("totale vracht", "totale vracht", "SOM", "Praam", "vracht",
                                    "{ \"node\": \"Literal\", \"value\": { \"number\": \"0\" }, \"type\": { \"node\": \"Number\","
                                    + " \"whole\": true, \"decimals\": 0, \"sign\": \"ONBEPAALD\" }, \"bron\": \"0\" }", PAD_VRACHT),
                            regel("steenlast", "steenlast", "SOM", "Ankersteen", "steengewicht",
                                    "{ \"node\": \"Literal\", \"value\": { \"number\": \"0\" }, \"type\": { \"node\": \"Number\","
                                    + " \"whole\": true, \"decimals\": 0, \"sign\": \"ONBEPAALD\" }, \"bron\": \"0\" }", PAD_STEEN),
                            regel("laagste zegelvracht", "laagste zegelvracht", "MINIMUM", "Zegelpraam", "vracht",
                                    "null", PAD_VRACHT),
                            regel("laagste vatinhoud", "laagste vatinhoud", "MINIMUM", "Vat", "inhoud", "null", PAD_VAT)));

    private static final String KETEN = """
            { "format": "uchar-case-v0", "asOfDate": null,
              "input": {
                "instances": [
                  { "label": "T1", "objectType": "Tolhuis", "attributes": {},
                    "roles": { "beheerde sluis": ["S1", "S2"], "geregistreerde praam": ["P4"] }, "characteristics": [] },
                  { "label": "T2", "objectType": "Tolhuis", "attributes": {},
                    "roles": { "beheerde sluis": ["S3"] }, "characteristics": [] },
                  { "label": "T3", "objectType": "Tolhuis", "attributes": {}, "roles": {}, "characteristics": [] },
                  { "label": "T4", "objectType": "Tolhuis", "attributes": {},
                    "roles": { "beheerde sluis": ["S4"] }, "characteristics": [] },
                  { "label": "S1", "objectType": "Sluis", "attributes": {},
                    "roles": { "geschutte praam": ["P1", "P2"], "gebruikte ankersteen": ["A1"] }, "characteristics": [] },
                  { "label": "S2", "objectType": "Sluis", "attributes": {},
                    "roles": { "geschutte praam": ["P3", "P5"], "gebruikte ankersteen": ["A1"] }, "characteristics": [] },
                  { "label": "S3", "objectType": "Sluis", "attributes": {},
                    "roles": { "gebruikte ankersteen": ["A2"] }, "characteristics": [] },
                  { "label": "S4", "objectType": "Sluis", "attributes": {},
                    "roles": { "geschutte praam": ["P6"] }, "characteristics": [] },
                  { "label": "P1", "objectType": "Praam", "attributes": { "vracht": { "number": "3.00" } },
                    "roles": { "geladen vat": ["V1", "V2"] }, "characteristics": [] },
                  { "label": "P2", "objectType": "Praam", "attributes": { "vracht": { "number": "7.00" } },
                    "roles": { "geladen vat": ["V3"] }, "characteristics": [] },
                  { "label": "P3", "objectType": "Zegelpraam", "attributes": { "vracht": { "number": "5.00" } },
                    "roles": {}, "characteristics": [] },
                  { "label": "P4", "objectType": "Praam", "attributes": { "vracht": { "number": "1.00" } },
                    "roles": {}, "characteristics": [] },
                  { "label": "P5", "objectType": "Praam", "attributes": {}, "roles": {}, "characteristics": [] },
                  { "label": "P6", "objectType": "Praam", "attributes": {}, "roles": {}, "characteristics": [] },
                  { "label": "A1", "objectType": "Ankersteen", "attributes": { "steengewicht": { "number": "12.00" } },
                    "roles": {}, "characteristics": [] },
                  { "label": "A2", "objectType": "Ankersteen", "attributes": { "steengewicht": { "number": "4.00" } },
                    "roles": {}, "characteristics": [] },
                  { "label": "V1", "objectType": "Vat", "attributes": { "inhoud": { "number": "2.50" } },
                    "roles": {}, "characteristics": [] },
                  { "label": "V2", "objectType": "Vat", "attributes": { "inhoud": { "number": "9.00" } },
                    "roles": {}, "characteristics": [] },
                  { "label": "V3", "objectType": "Vat", "attributes": { "inhoud": { "number": "6.00" } },
                    "roles": {}, "characteristics": [] }
                ],
                "parameters": {} },
              "expected": [
                { "kind": "attributeValue", "label": "T1", "attribute": "laagste vracht", "value": { "number": "3.00" } },
                { "kind": "attributeValue", "label": "T1", "attribute": "totale vracht", "value": { "number": "15.00" } },
                { "kind": "attributeValue", "label": "T1", "attribute": "steenlast", "value": { "number": "12.00" } },
                { "kind": "attributeValue", "label": "T1", "attribute": "laagste zegelvracht", "value": { "number": "5.00" } },
                { "kind": "attributeValue", "label": "T1", "attribute": "laagste vatinhoud", "value": { "number": "2.50" } },
                { "kind": "attributeEmpty", "label": "T2", "attribute": "laagste vracht" },
                { "kind": "attributeValue", "label": "T2", "attribute": "totale vracht", "value": { "number": "0" } },
                { "kind": "attributeValue", "label": "T2", "attribute": "steenlast", "value": { "number": "4.00" } },
                { "kind": "attributeEmpty", "label": "T2", "attribute": "laagste zegelvracht" },
                { "kind": "attributeEmpty", "label": "T2", "attribute": "laagste vatinhoud" },
                { "kind": "attributeEmpty", "label": "T3", "attribute": "laagste vracht" },
                { "kind": "attributeValue", "label": "T3", "attribute": "totale vracht", "value": { "number": "0" } },
                { "kind": "attributeValue", "label": "T3", "attribute": "steenlast", "value": { "number": "0" } },
                { "kind": "attributeEmpty", "label": "T3", "attribute": "laagste zegelvracht" },
                { "kind": "attributeEmpty", "label": "T3", "attribute": "laagste vatinhoud" },
                { "kind": "attributeEmpty", "label": "T4", "attribute": "laagste vracht" },
                { "kind": "attributeValue", "label": "T4", "attribute": "totale vracht", "value": { "number": "0" } },
                { "kind": "attributeValue", "label": "T4", "attribute": "steenlast", "value": { "number": "0" } },
                { "kind": "attributeEmpty", "label": "T4", "attribute": "laagste zegelvracht" },
                { "kind": "attributeEmpty", "label": "T4", "attribute": "laagste vatinhoud" },
                { "kind": "consistent" }
              ] }""";
}
