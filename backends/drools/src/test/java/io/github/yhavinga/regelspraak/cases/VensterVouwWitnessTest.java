package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * A TIME WINDOW (RUNTIME-SEMANTICS ch. 3.10) inside a QUANTIFIER FOLD (ch. 3.9a), replayed through BOTH
 * Drools branches: played where WF-112 admits it — the {@code toets} and the {@code filter} of an
 * {@code ALLE} fold, and beside them the die/dat filter of an aggregation, which shares the filter's
 * machinery — and refused BY NAME everywhere WF-112 forbids it.
 *
 * <p><b>Why these rows exist.</b> Measured 2026-09-23 (card drools-venster-vouwtoets) on documents built
 * exactly as below: the exec-model branch refused the window in an {@code ALLE} fold's toets and filter
 * ("een conditie van soort Periode", "een die/dat-subselectie die deze tak niet op de source kan binden")
 * where the verifier, {@code ref}, clingo and the DRL-text branch all carry it with the values pinned here.
 * The other flank: under {@code GEEN} and the counting quantifiers the DRL-text branch PLAYED the window,
 * in a time-free session under {@code GEEN} with the complement — every ploeg bevoegd — while the verifier
 * rejects the document (WF-112) and {@code ref} and clingo refuse it. A document that skips the verifier
 * reaches this backend directly, so the branch has to refuse the position itself.
 *
 * <p><b>The document.</b> {@code corpus/golden/jutterswet-schouwploeg} reduced to its rule "bevoegd bij
 * diploma", with a day-precision {@code aanstelling} added to BOTH object types. The fold's own subject
 * carries the attribute too, with values chosen so that a lowering which read the PLOEG's date instead of
 * the contributor's would compile and answer differently: S1 and S5 hold a date after the evaluation date,
 * S2 and S6 one before it. In the toets the measured ref/clingo answer and that wrong reading part company
 * on S1, S2, S5 and S6, in the filter on S5 and S6, so a pass here excludes the wrong subject. S6 also holds
 * the one contributor that the filter must DROP to change the verdict: W7 is outside the window and not
 * gediplomeerd, so a filter that selected everyone would read S6 onwaar.
 *
 * <p><b>The instances</b>, shared by every row but one (evaluation date 2024-06-15 unless the row says
 * otherwise; the exception is the whole-period route, built from {@code jutterswet-ploegpremie} the same way
 * and described at its row):
 * <pre>
 *   S1 (own 2025-06-01): W1 2023-01-01 gediplomeerd, W2 2024-06-15 gediplomeerd (ON the date: vanaf is inclusive)
 *   S2 (own 2020-01-01): W3 2024-09-01 gediplomeerd (after the date: the window misses it)
 *   S3 (no own date):    W4 2023-01-01 gediplomeerd, W5 without a date (a leeg bound never matches)
 *   S4 (own 2020-01-01): no wachten (the vacuous row of ch. 3.9a)
 *   S5 (own 2025-06-01): W6 2023-01-01, NOT gediplomeerd
 *   S6 (own 2020-01-01): W7 2024-09-01, NOT gediplomeerd (after the date), W8 2023-01-01 gediplomeerd
 * </pre>
 * Every expected verdict below was computed by hand before the first run and measured equal in {@code ref}
 * and clingo on the same document; each row names the ploegen that come out bevoegd, every other ploeg is
 * pinned ABSENT, so a branch that silently lowered nothing fails on the present ones.
 */
class VensterVouwWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path SCHOUWPLOEG =
            Path.of("..", "..", "corpus", "golden", "jutterswet-schouwploeg");
    private static final Path PLOEGPREMIE =
            Path.of("..", "..", "corpus", "golden", "jutterswet-ploegpremie");
    private static final Path NEGATIEF = Path.of("..", "..", "corpus", "negatief");

    private static final String BINNEN = "2024-06-15";

    @TempDir Path tmp;

    private int n;

    // ---------------- the window in an ALLE fold's TOETS: played, with ref's values ----------------

    /** vanaf the CONTRIBUTOR's own aanstelling: W3 (after the date) and W5 (leeg) miss it, the rest hit. */
    @Test void het_venster_in_de_toets_leest_de_grens_van_elke_bijdrager() throws Exception {
        BeideTakken.groen(document("ALLE", null, null, ATTR), casus(BINNEN, "S1", "S4", "S5"),
                "de grens is de aanstelling van de wacht, per bijdrager gelezen");
    }

    /** The same document against the answer a lowering reading the PLOEG's own date would give: both branches
     *  must reject it, so the row above really separates the two subjects. */
    @Test void de_lezing_van_de_ploeg_zelf_wordt_door_beide_takken_verworpen() throws Exception {
        BeideTakken.rood(document("ALLE", null, null, ATTR), casus(BINNEN, "S2", "S4", "S6"),
                "kenmerk bevoegd: verwacht aanwezig, werkelijk afwezig",
                "de datum van de ploeg is niet de grens van de bijdrager");
    }

    /** ch. 3.10: {@code tot} is exclusive and "tot en met" inclusive — on the evaluation date itself the two
     *  spellings of one window give opposite folds. */
    @Test void de_totgrens_sluit_de_evaluatiedatum_uit_en_tot_en_met_sluit_hem_in() throws Exception {
        BeideTakken.groen(document("ALLE", null, null, EXCL), casus(BINNEN, "S4"),
                "tot 15 juni: de evaluatiedatum ligt buiten het venster");
        BeideTakken.groen(document("ALLE", null, null, INCL), casus(BINNEN, "S1", "S2", "S3", "S4", "S5", "S6"),
                "tot en met 15 juni: de evaluatiedatum ligt erin");
    }

    /** An unset evaluation date never matches (ch. 3.10), so every non-empty ploeg fails ALLE and only the
     *  vacuous one holds — the reading WF-112 keeps because nothing complements it here. */
    @Test void in_een_tijdvrije_sessie_haalt_geen_bijdrager_het_venster() throws Exception {
        BeideTakken.groen(document("ALLE", null, null, LIT), casus(null, "S4"),
                "tijdvrij: het venster matcht nooit");
    }

    /** The window as a member of an ALLE and of a DE composite matrix: S5's wacht is in the window but not
     *  gediplomeerd, so the composite differs from the bare window exactly there. */
    @Test void het_venster_als_lid_van_een_samengestelde_matrix() throws Exception {
        BeideTakken.groen(document("ALLE", null, null, samen("ALLE", GEDIPLOMEERD, ATTR)),
                casus(BINNEN, "S1", "S4"), "ALLE[gediplomeerd, venster]");
        BeideTakken.groen(document("ALLE", null, null, samen("DE", GEDIPLOMEERD, ATTR)),
                casus(BINNEN, "S1", "S4"), "DE[gediplomeerd, venster]");
    }

    /** The window beside a numeric PARAMETER bound in one matrix: both are read. Under keurgrens 5 every wacht
     *  (8 dienstjaren) passes the comparison and the window decides; under 9 none passes. */
    @Test void het_venster_naast_een_parametergrens_leest_beide() throws Exception {
        Path ir = metKeurgrens(document("ALLE", null, null, samen("ALLE", KEURGRENS, ATTR)));
        BeideTakken.groen(ir, metParameter(casus(BINNEN, "S1", "S4", "S5"), "5"), "keurgrens 5: het venster beslist");
        BeideTakken.groen(ir, metParameter(casus(BINNEN, "S4"), "9"), "keurgrens 9: de vergelijking beslist");
    }

    // ---------------- the window in an ALLE fold's FILTER: played, with ref's values ----------------

    /** The window SELECTS the contributors, per contributor: S2's only wacht falls out and S2 holds vacuously,
     *  S5's wacht stays in and is not gediplomeerd, and S6 holds only because W7 falls out. In a time-free
     *  session nothing is selected anywhere, so every ploeg holds vacuously — ch. 3.12's recorded reading of this
     *  position (maintainer decision 2026-09-23). */
    @Test void het_venster_in_het_filter_kiest_de_bijdragers_per_bijdrager() throws Exception {
        Path ir = document("ALLE", null, ATTR, GEDIPLOMEERD);
        BeideTakken.groen(ir, casus(BINNEN, "S1", "S2", "S3", "S4", "S6"), "het filter selecteert per bijdrager");
        BeideTakken.groen(ir, casus(null, "S1", "S2", "S3", "S4", "S5", "S6"),
                "tijdvrij: niets geselecteerd, vacuüm waar");
    }

    /** The fold's OTHER route — the ch. 3.9 whole-year matrix over each contributor's own timeline,
     *  {@code corpus/golden/jutterswet-ploegpremie}'s "premiegerechtigd" — takes its contributors from the same
     *  filtered source, so the window has to select there too. B2 is appointed after the evaluation date and
     *  falls out, which leaves P1 with B1 at 50 above the bound; B3 is selected and at 30 fails it. Each ploeg
     *  carries a date of its own on the other side of the evaluation date. Time-free: nothing is selected. */
    @Test void het_venster_in_het_filter_van_een_heelperiodevouw() throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(PLOEGPREMIE.resolve("ir.json")));
        for (String ot : new String[] { "Berger", "Bergingsploeg" })
            IrPad.addAttribute(IrPad.objectType(doc, ot), "aanstelling", JSON.readTree(DATUM));
        houdRegel(doc, "premiegerechtigd bij dagvergoeding");
        ((ObjectNode) IrPad.versie(doc, "rules", 0, 0).get("condition"))
                .set("filter", JSON.readTree(venster("""
                { "node": "AttributeReference", "attribute": "aanstelling", "subjectType": "Berger",
                  "source": { "snippet": "de aanstelling van de berger" } }""", "null", false, "vanaf de aanstelling van de berger")));
        Path ir = tmp.resolve("venstervouw-" + (n++) + ".json");
        Files.writeString(ir, doc.toPrettyString());
        BeideTakken.groen(ir, ploegCasus("2026-03-15", "P1"), "het filter selecteert B1 en B3");
        BeideTakken.groen(ir, ploegCasus(null, "P1", "P2"), "tijdvrij: niets geselecteerd, vacuüm waar");
    }

    // ---------------- the same window in an AGGREGATION's filter: the neighbouring position ----------------

    /** The golden's rule "wachttal" — the count of the ploeg's wachten — with the window as its die/dat filter.
     *  Measured the same day as the fold rows: ref, clingo and the DRL-text branch counted it and the exec-model
     *  branch declined the whole aggregation ("aggregatie buiten Phase-C scope"), because the window in a VALUE's
     *  filter left the rekendatum global unregistered. Per ploeg, at 2024-06-15: S1 2 (W2 on the date), S2 0,
     *  S3 1 (W5 leeg), S4 0, S5 1, S6 1 (W7 after the date); a reading of the ploeg's own date would count 0, 1,
     *  0, 0, 0, 2. Time-free: nothing counts. The ALLE subselection beside it drops S5's W6 as well. */
    @Test void het_venster_in_het_filter_van_een_aggregatie_telt_per_element() throws Exception {
        Path ir = aggregatieDocument(ATTR);
        BeideTakken.groen(ir, tellingCasus(BINNEN, 2, 0, 1, 0, 1, 1), "de telling selecteert per wacht");
        BeideTakken.groen(ir, tellingCasus(null, 0, 0, 0, 0, 0, 0), "tijdvrij: niets telt");
        BeideTakken.groen(aggregatieDocument(samen("ALLE", GEDIPLOMEERD, ATTR)), tellingCasus(BINNEN, 2, 0, 1, 0, 0, 1),
                "ALLE[gediplomeerd, venster] als subselectie");
    }

    /** The same count as the OPERAND of a rule condition — "bevoegd" when more than 0 of the ploeg's wachten
     *  serve from their own aanstelling — which reaches the rekendatum registration through the condition, not
     *  through a value: measured, the exec-model branch declined it ("voorwaarde buiten Phase-B scope") where the
     *  other three answered S1, S3, S5 and S6 at 2024-06-15 and nobody in a time-free session. */
    @Test void de_telling_met_een_venster_als_operand_van_een_voorwaarde() throws Exception {
        Path ir = voorwaardeTellingDocument(ATTR);
        BeideTakken.groen(ir, casus(BINNEN, "S1", "S3", "S5", "S6"), "de telling in de voorwaarde selecteert per wacht");
        BeideTakken.groen(ir, casus(null), "tijdvrij: niemand telt, dus niemand bevoegd");
    }

    // ---------------- WF-112: GEEN and the counting quantifiers, refused by name in both branches ----------------

    /** The complement flank: a time-free case, where a lowered GEEN would answer bevoegd for every ploeg. Each
     *  refusal names the position (toets or filter), the fold's quantifier and element type, and the rule, in
     *  both branches (WF-112 reaches both positions). In the filter the window SELECTS the members the quantifier
     *  counts or complements the toets over, and the sentence says that rather than the toets's reason. */
    @Test void onder_geen_en_de_tellende_kwantoren_weigeren_beide_takken_het_venster() throws Exception {
        for (String kw : List.of("GEEN", "TEN_MINSTE", "TEN_HOOGSTE", "PRECIES")) {
            Integer drempel = "GEEN".equals(kw) ? null : 1;
            String vouw = "een kwantorvouw onder " + kw + " over Strandwacht (WF-112)";
            BeideTakken.geweigerd(document(kw, drempel, null, LIT), casus(null, ALLE_PLOEGEN),
                    "periodevenster in de toets van " + vouw + ": de kwantor telt of ontkent het vensterverdict",
                    "het venster in de toets onder " + kw);
            BeideTakken.geweigerd(document(kw, drempel, LIT, GEDIPLOMEERD), casus(null, ALLE_PLOEGEN),
                    "periodevenster in het filter van " + vouw
                            + ": het venster kiest de leden waarover de kwantor telt of ontkent",
                    "het venster in het filter onder " + kw);
        }
    }

    /** WF-112's walk is deep: a GEEN composite inside an ALLE fold's toets complements the window as surely as a
     *  GEEN fold does, and is refused on ITS quantifier. */
    @Test void een_venster_onder_een_geen_samenstelling_in_een_alle_vouw_wordt_geweigerd() throws Exception {
        BeideTakken.geweigerd(document("ALLE", null, null, samen("GEEN", LIT)), casus(null, ALLE_PLOEGEN),
                "GEEN-samenstelling (WF-112)", "GEEN[venster] als toets van een ALLE-vouw");
    }

    /** The same position as an aggregation's subselection, in a value and as the operand of a condition. Both
     *  branches refused it before, under a generic note ("aggregatie buiten Phase-C scope" / "TODO aggregatie
     *  AANTAL"; "voorwaarde buiten Phase-B scope" / "TODO voorwaarde"), and now name the rule. */
    @Test void een_venster_onder_geen_in_een_aggregatiefilter_wordt_bij_naam_geweigerd() throws Exception {
        BeideTakken.geweigerd(aggregatieDocument(samen("GEEN", ATTR)), tellingCasus(null, 2, 1, 2, 0, 1, 2),
                "GEEN-samenstelling (WF-112)", "GEEN[venster] als subselectie van een telling");
        BeideTakken.geweigerd(voorwaardeTellingDocument(samen("GEEN", ATTR)), casus(null, ALLE_PLOEGEN),
                "GEEN-samenstelling (WF-112)", "diezelfde telling als operand van een voorwaarde");
    }

    /** The corpus's own WF-112 witnesses, loaded as gate 12 and the replay API load a document: with no
     *  verifier in front. The fold arm is replayed with the golden's time-free case 1, which pins S1 bevoegd
     *  PRESENT — exactly the complement a lowered GEEN answers, and the answer the DRL-text branch gave until
     *  this refusal. The composite arm (the window as a member of a GEEN composite that is the whole rule
     *  condition) was refused by both branches before, but under a generic scope note in the exec branch and
     *  without the rule's number in the DRL-text branch; it now names WF-112 in the same words as the fold. */
    @Test void de_wf112_negatieven_van_het_corpus_weigeren_beide_takken_bij_naam() throws Exception {
        BeideTakken.geweigerd(NEGATIEF.resolve("wf112-venster-in-geen-vouw").resolve("ir.json"),
                JSON.readTree(Files.readString(SCHOUWPLOEG.resolve("case1-allen-ervaren.json"))),
                "periodevenster in de toets van een kwantorvouw onder GEEN over Strandwacht (WF-112)",
                "de vouwarm van WF-112, tijdvrij");
        BeideTakken.geweigerd(NEGATIEF.resolve("wf112-periodevenster-tellende-kwantor").resolve("ir.json"),
                JSON.readTree("""
                        { "format": "uchar-case-v0", "asOfDate": null,
                          "input": { "instances": [ { "label": "V1", "objectType": "Vondst", "attributes": {},
                                                      "roles": {}, "characteristics": [] } ], "parameters": {} },
                          "expected": [ { "kind": "attributeEmpty", "label": "V1", "attribute": "premie" },
                                        { "kind": "consistent" } ] }"""),
                "GEEN-samenstelling (WF-112)", "de samenstellingsarm van WF-112, tijdvrij");
    }

    // ================= the document and the cases =================

    private static final String DATUM = "{ \"node\": \"Date\", \"precision\": \"day\" }";

    private static String literal(String d) {
        return "{ \"node\": \"Literal\", \"value\": { \"date\": \"" + d + "\" }, \"type\": " + DATUM
                + ", \"source\": {\"snippet\": \"" + d + "\"} }";
    }

    private static String venster(String vanaf, String tot, boolean inclusief, String bron) {
        return "{ \"node\": \"TimeWindow\", \"from\": " + vanaf + ", \"to\": " + tot + ", \"toInclusive\": "
                + inclusief + ", \"source\": {\"snippet\": \"" + bron + "\"} }";
    }

    /** van 1 januari 2024 tot 1 januari 2025 */
    private static final String LIT = venster(literal("2024-01-01"), literal("2025-01-01"), false,
            "van 1 januari 2024 tot 1 januari 2025");
    /** vanaf the contributor's own aanstelling, open above */
    private static final String ATTR = venster("""
            { "node": "AttributeReference", "attribute": "aanstelling", "subjectType": "Strandwacht",
              "source": { "snippet": "de aanstelling van de wacht" } }""", "null", false, "vanaf de aanstelling van de wacht");
    /** van 1 januari 2024 tot 15 juni 2024 */
    private static final String EXCL = venster(literal("2024-01-01"), literal("2024-06-15"), false,
            "van 1 januari 2024 tot 15 juni 2024");
    /** van 1 januari 2024 tot en met 15 juni 2024 */
    private static final String INCL = venster(literal("2024-01-01"), literal("2024-06-15"), true,
            "van 1 januari 2024 tot en met 15 juni 2024");

    private static final String GEDIPLOMEERD = """
            { "node": "CharacteristicCheck", "characteristic": "gediplomeerd", "negated": false,
              "source": { "snippet": "hij gediplomeerd is" } }""";

    private static final String KEURGRENS = """
            { "node": "Comparison", "op": "greater",
              "left": { "node": "AttributeReference", "attribute": "dienstjaren", "subjectType": "Strandwacht",
                         "source": { "snippet": "zijn dienstjaren" } },
              "right": { "node": "ParameterReference", "parameter": "keurgrens",
                          "source": { "snippet": "de keurgrens" } },
              "source": { "snippet": "zijn dienstjaren groter zijn dan de keurgrens" } }""";

    private static String samen(String kwantor, String... delen) {
        return "{ \"node\": \"Compound\", \"quantifier\": \"" + kwantor + "\", \"threshold\": 0, \"parts\": [ "
                + String.join(", ", delen) + " ], \"source\": {\"snippet\": \"hij aan de volgende voorwaarden voldoet\"} }";
    }

    /** The golden's document with ONE rule, "bevoegd bij diploma", whose fold is set to the given quantifier,
     *  threshold (null under ALLE and GEEN, WF-108 (e)), filter (null for none) and toets. */
    private Path document(String kwantor, Integer drempel, String filter, String toets) throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(SCHOUWPLOEG.resolve("ir.json")));
        for (String ot : new String[] { "Strandwacht", "Schouwploeg" })
            IrPad.addAttribute(IrPad.objectType(doc, ot), "aanstelling", JSON.readTree(DATUM));
        houdRegel(doc, "bevoegd bij diploma");
        ObjectNode kv = (ObjectNode) IrPad.versie(doc, "rules", 0, 0).get("condition");
        kv.put("kwantor", kwantor);
        if (drempel == null) kv.remove("drempel"); else kv.put("drempel", drempel);
        kv.set("filter", filter == null ? JSON.nullNode() : JSON.readTree(filter));
        kv.set("toets", JSON.readTree(toets));
        Path p = tmp.resolve("venstervouw-" + (n++) + ".json");
        Files.writeString(p, doc.toPrettyString());
        return p;
    }

    private static void houdRegel(ObjectNode doc, String naam) {
        ArrayNode uit = JSON.createArrayNode();
        for (JsonNode r : IrPad.rulesLijst(doc, "rules")) if (naam.equals(r.path("name").asText())) uit.add(r);
        IrPad.rules(doc).set("rules", uit);
    }

    /** The golden's document with ONE rule, "wachttal van een schouwploeg", whose count carries {@code filter}. */
    private Path aggregatieDocument(String filter) throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(SCHOUWPLOEG.resolve("ir.json")));
        for (String ot : new String[] { "Strandwacht", "Schouwploeg" })
            IrPad.addAttribute(IrPad.objectType(doc, ot), "aanstelling", JSON.readTree(DATUM));
        houdRegel(doc, "wachttal van een schouwploeg");
        ((ObjectNode) IrPad.versie(doc, "rules", 0, 0).get("value"))
                .set("filter", JSON.readTree(filter));
        Path p = tmp.resolve("venstervouw-" + (n++) + ".json");
        Files.writeString(p, doc.toPrettyString());
        return p;
    }

    /** The golden's document with ONE rule, "bevoegd bij diploma", whose whole condition is "the count of the
     *  ploeg's wachten carrying {@code filter} is greater than 0". */
    private Path voorwaardeTellingDocument(String filter) throws Exception {
        ObjectNode telDoc = (ObjectNode) JSON.readTree(Files.readString(aggregatieDocument(filter)));
        JsonNode telling = IrPad.versie(telDoc, "rules", 0, 0).get("value");
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(SCHOUWPLOEG.resolve("ir.json")));
        for (String ot : new String[] { "Strandwacht", "Schouwploeg" })
            IrPad.addAttribute(IrPad.objectType(doc, ot), "aanstelling", JSON.readTree(DATUM));
        houdRegel(doc, "bevoegd bij diploma");
        ObjectNode vergelijking = JSON.createObjectNode();
        vergelijking.put("node", "Vergelijking").put("op", "GROTER").set("links", telling);
        vergelijking.set("rechts", JSON.readTree("""
                { "node": "Literal", "value": { "number": "0" }, "type": { "node": "Number", "whole": true, "decimals": 0,
                  "sign": "unspecified" }, "source": { "snippet": "0" } }"""));
        vergelijking.putNull("periode").set("source", IrPad.bronrecord("het aantal wachten die vanaf hun aanstelling dienen groter is dan 0"));
        IrPad.versie(doc, "rules", 0, 0).set("condition", vergelijking);
        Path p = tmp.resolve("venstervouw-" + (n++) + ".json");
        Files.writeString(p, doc.toPrettyString());
        return p;
    }

    /** The shared instances at {@code asOf}, expecting each ploeg's wachttal, S1 to S6 in order. */
    private static JsonNode tellingCasus(String asOf, int... tal) throws Exception {
        ObjectNode c = JSON.createObjectNode();
        c.put("format", "uchar-case-v0");
        if (asOf == null) c.putNull("asOfDate"); else c.put("asOfDate", asOf);
        ObjectNode input = c.putObject("input");
        input.set("instances", JSON.readTree(INSTANTIES));
        input.putObject("parameters");
        ArrayNode verwacht = c.putArray("expected");
        for (int i = 0; i < ALLE_PLOEGEN.length; i++)
            verwacht.addObject().put("kind", "attributeValue").put("label", ALLE_PLOEGEN[i]).put("attribute", "wachttal")
                    .set("value", JSON.createObjectNode().put("number", String.valueOf(tal[i])));
        verwacht.addObject().put("kind", "consistent");
        return c;
    }

    /** {@code ir} with the numeric parameter {@code keurgrens} declared. */
    private Path metKeurgrens(Path ir) throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(ir));
        IrPad.modelLijst(doc, "parameters").add(JSON.readTree("""
                { "node": "Parameter", "name": "keurgrens",
                  "datatype": { "node": "Number", "whole": true, "decimals": 0,
                                "sign": "nonNegative" } }"""));
        Files.writeString(ir, doc.toPrettyString());
        return ir;
    }

    private static JsonNode metParameter(JsonNode casus, String keurgrens) {
        ((ObjectNode) casus.get("input").get("parameters")).set("keurgrens",
                JSON.createObjectNode().put("number", keurgrens));
        return casus;
    }

    private static final String INSTANTIES = """
            [ { "label": "S1", "objectType": "Schouwploeg", "attributes": { "aanstelling": { "date": "2025-06-01" } },
                "roles": { "wachten": [ "W1", "W2" ] }, "characteristics": [] },
              { "label": "S2", "objectType": "Schouwploeg", "attributes": { "aanstelling": { "date": "2020-01-01" } },
                "roles": { "wachten": [ "W3" ] }, "characteristics": [] },
              { "label": "S3", "objectType": "Schouwploeg", "attributes": {},
                "roles": { "wachten": [ "W4", "W5" ] }, "characteristics": [] },
              { "label": "S4", "objectType": "Schouwploeg", "attributes": { "aanstelling": { "date": "2020-01-01" } },
                "roles": {}, "characteristics": [] },
              { "label": "S5", "objectType": "Schouwploeg", "attributes": { "aanstelling": { "date": "2025-06-01" } },
                "roles": { "wachten": [ "W6" ] }, "characteristics": [] },
              { "label": "S6", "objectType": "Schouwploeg", "attributes": { "aanstelling": { "date": "2020-01-01" } },
                "roles": { "wachten": [ "W7", "W8" ] }, "characteristics": [] },
              { "label": "W1", "objectType": "Strandwacht",
                "attributes": { "dienstjaren": { "number": "8" }, "aanstelling": { "date": "2023-01-01" } },
                "roles": {}, "characteristics": [ "gediplomeerd" ] },
              { "label": "W2", "objectType": "Strandwacht",
                "attributes": { "dienstjaren": { "number": "8" }, "aanstelling": { "date": "2024-06-15" } },
                "roles": {}, "characteristics": [ "gediplomeerd" ] },
              { "label": "W3", "objectType": "Strandwacht",
                "attributes": { "dienstjaren": { "number": "8" }, "aanstelling": { "date": "2024-09-01" } },
                "roles": {}, "characteristics": [ "gediplomeerd" ] },
              { "label": "W4", "objectType": "Strandwacht",
                "attributes": { "dienstjaren": { "number": "8" }, "aanstelling": { "date": "2023-01-01" } },
                "roles": {}, "characteristics": [ "gediplomeerd" ] },
              { "label": "W5", "objectType": "Strandwacht", "attributes": { "dienstjaren": { "number": "8" } },
                "roles": {}, "characteristics": [ "gediplomeerd" ] },
              { "label": "W6", "objectType": "Strandwacht",
                "attributes": { "dienstjaren": { "number": "8" }, "aanstelling": { "date": "2023-01-01" } },
                "roles": {}, "characteristics": [] },
              { "label": "W7", "objectType": "Strandwacht",
                "attributes": { "dienstjaren": { "number": "8" }, "aanstelling": { "date": "2024-09-01" } },
                "roles": {}, "characteristics": [] },
              { "label": "W8", "objectType": "Strandwacht",
                "attributes": { "dienstjaren": { "number": "8" }, "aanstelling": { "date": "2023-01-01" } },
                "roles": {}, "characteristics": [ "gediplomeerd" ] } ]""";

    /** The shared instances at {@code asOf} (null: a time-free session), expecting "bevoegd" present on exactly
     *  the named ploegen and absent on the others, plus no inconsistency. */
    private static JsonNode casus(String asOf, String... bevoegd) throws Exception {
        return casus(INSTANTIES, List.of(ALLE_PLOEGEN), "bevoegd", asOf, bevoegd);
    }

    private static final String[] ALLE_PLOEGEN = { "S1", "S2", "S3", "S4", "S5", "S6" };

    private static final String BERGERS = """
            [ { "label": "P1", "objectType": "Bergingsploeg", "attributes": { "aanstelling": { "date": "2020-01-01" } },
                "roles": { "bergers": [ "B1", "B2" ] }, "characteristics": [] },
              { "label": "P2", "objectType": "Bergingsploeg", "attributes": { "aanstelling": { "date": "2027-01-01" } },
                "roles": { "bergers": [ "B3" ] }, "characteristics": [] },
              { "label": "B1", "objectType": "Berger", "attributes": {
                  "dagvergoeding": { "timeline": [ { "from": "2026-01-01", "to": "2027-01-01", "value": { "number": "50" } } ] },
                  "aanstelling": { "date": "2025-01-01" } }, "roles": {}, "characteristics": [] },
              { "label": "B2", "objectType": "Berger", "attributes": {
                  "dagvergoeding": { "timeline": [ { "from": "2026-01-01", "to": "2027-01-01", "value": { "number": "30" } } ] },
                  "aanstelling": { "date": "2026-06-01" } }, "roles": {}, "characteristics": [] },
              { "label": "B3", "objectType": "Berger", "attributes": {
                  "dagvergoeding": { "timeline": [ { "from": "2026-01-01", "to": "2027-01-01", "value": { "number": "30" } } ] },
                  "aanstelling": { "date": "2025-01-01" } }, "roles": {}, "characteristics": [] } ]""";

    /** The bergers at {@code asOf}, expecting "premiegerechtigd" present on exactly the named ploegen. */
    private static JsonNode ploegCasus(String asOf, String... premiegerechtigd) throws Exception {
        return casus(BERGERS, List.of("P1", "P2"), "premiegerechtigd", asOf, premiegerechtigd);
    }

    private static JsonNode casus(String instanties, List<String> labels, String kenmerk, String asOf,
            String... aanwezig) throws Exception {
        ObjectNode c = JSON.createObjectNode();
        c.put("format", "uchar-case-v0");
        if (asOf == null) c.putNull("asOfDate"); else c.put("asOfDate", asOf);
        ObjectNode input = c.putObject("input");
        input.set("instances", JSON.readTree(instanties));
        input.putObject("parameters");
        ArrayNode verwacht = c.putArray("expected");
        Set<String> waar = Set.of(aanwezig);
        for (String p : labels)
            verwacht.addObject().put("kind", waar.contains(p) ? "characteristicPresent" : "characteristicAbsent")
                    .put("label", p).put("characteristic", kenmerk);
        verwacht.addObject().put("kind", "consistent");
        return c;
    }
}
