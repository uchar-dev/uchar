package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two conditional rules that write one attribute and whose conditions both hold on one instance ("De meldingsdatum
 * van een Vondst moet gesteld worden op zijn vinddatum indien zijn vinddatum gevuld is", and the same on its
 * aanspoeldatum). WF-079 refuses two unconditional writers only, so such a document is well-formed and the executors
 * answer it. RUNTIME-SEMANTICS ch. 5.1 reads every rule as an equation; two equations that demand different values
 * for one target have no fixpoint, and ch. 5.7 makes that divergence a fault. Both Drools branches must report it as
 * such, naming the writers, the target and the values they write, and never a cycle the document does not contain.
 * Where only one condition holds, or both writers write the same value, or the conditions exclude each other, the
 * document plays. Every document here is authored in this class on Jutterswet material, each one measured form of the
 * card's form space; {@code ref} faults on the same bodies and the ASP backend derives a named witness.
 *
 * <p><b>Red before the fix</b> (measured 2026-10-01 on the jar of the claim): the exec-model branch ran into its fire
 * cap and reported {@code geen fixpunt (kringverwijzing?)}, a cause the document does not have; the DRL-text branch
 * fired each writer once and kept the value of the writer declared last, without a diagnostic ({@code PLAYED} where
 * that was the expected value, {@code verwacht 2026-03-04, werkelijk leeg} where the last writer evaluated leeg). The
 * three flank rows are green before and after.
 *
 * <p>Since card tms-omschakeling (2026-10-10) the rows of the exec-model branch run on the truth-maintenance branch. It
 * holds both writers' values as facts and finds the divergence at the fixpoint, where its write-back throws the fault
 * ({@code Schrijversconflict}) in the sentence the DRL-text branch gives as its verdict; the destructive exec-model branch
 * found it at its fire cap.
 */
class OverlappendeSchrijversWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ---------------- the card's reproductions ----------------

    @Test void r1_twee_voorwaardelijke_schrijvers_op_een_type() throws Exception {
        // The card's R1: V with vinddatum 04-03-2026 and aanspoeldatum 01-03-2026. ref: no fixpoint, the two rules named.
        String ir = document(objecttype("Vondst", "Vondsten", null, "", DATUMS),
                meldingOp("melding op vinddag", "Vondst", "vinddatum"),
                meldingOp("melding op aanspoeldag", "Vondst", "aanspoeldatum"));
        String zaak = zaak(List.of(vondst("V", "Vondst", "2026-03-04", "2026-03-01", null)),
                datumVerwacht("V", "meldingsdatum", "2026-03-01"));
        for (DroolsBackend.Branch drl : TAKKEN)
            meldtSchrijvers(speel(ir, zaak, drl), tak(drl), "twee schrijvers van één doel geven verschillende waarden",
                    "de regel \"melding op vinddag\" schrijft 2026-03-04",
                    "de regel \"melding op aanspoeldag\" schrijft 2026-03-01", "in het attribuut meldingsdatum van V, een Vondst");
    }

    @Test void r2_de_tweede_schrijver_op_een_subtype() throws Exception {
        // The card's R2: the second writer on the subtype Strandvondst, S a Strandvondst with both dates.
        String ir = document(objecttype("Vondst", "Vondsten", null, "", attribuut("vinddatum", DATUM),
                        attribuut("meldingsdatum", DATUM)) + ","
                        + objecttype("Strandvondst", "Strandvondsten", "Vondst", "", attribuut("aanspoeldatum", DATUM)),
                meldingOp("melding op vinddag", "Vondst", "vinddatum"),
                meldingOp("melding op aanspoeldag", "Strandvondst", "aanspoeldatum"));
        String zaak = zaak(List.of(vondst("S", "Strandvondst", "2026-03-04", "2026-03-01", null)),
                datumVerwacht("S", "meldingsdatum", "2026-03-01"));
        for (DroolsBackend.Branch drl : TAKKEN)
            meldtSchrijvers(speel(ir, zaak, drl), tak(drl), "twee schrijvers van één doel geven verschillende waarden",
                    "\"melding op vinddag\" schrijft 2026-03-04",
                    "\"melding op aanspoeldag\" schrijft 2026-03-01", "in het attribuut meldingsdatum van S, een Strandvondst");
    }

    @Test void r1_op_de_rode_route() throws Exception {
        // The red path of gate 8 replays the case on its own fire loop: the same sentence, as a FAULT.
        String ir = document(objecttype("Vondst", "Vondsten", null, "", DATUMS),
                meldingOp("melding op vinddag", "Vondst", "vinddatum"),
                meldingOp("melding op aanspoeldag", "Vondst", "aanspoeldatum"));
        Path map = Files.createTempDirectory("overlappende-schrijvers");
        map.toFile().deleteOnExit();
        Path irPad = map.resolve("ir.json");
        Path casePad = map.resolve("case1.json");
        Files.writeString(irPad, ir);
        Files.writeString(casePad, "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":["
                + vondst("V", "Vondst", "2026-03-04", "2026-03-01", null) + "],\"parameters\":{}},\"expectedFault\":"
                + "{\"ref\":\"two writers of one target disagree\",\"clingo\":\"two writers of one target disagree\"}}");
        irPad.toFile().deleteOnExit();
        casePad.toFile().deleteOnExit();
        CaseRunner.RoodResult r = CaseRunner.runRood(irPad, casePad);
        assertEquals("FAULT", r.status(), r.detail());
        meldtSchrijvers(r.detail(), "rode route", "de regel \"melding op vinddag\" schrijft 2026-03-04",
                "de regel \"melding op aanspoeldag\" schrijft 2026-03-01", "in het attribuut meldingsdatum van V, een Vondst");
    }

    // ---------------- the form space ----------------

    @Test void drie_schrijvers_met_drie_waarden() throws Exception {
        // Three writers, each with its own date. ref names all three; in the engine two of them can keep firing each
        // other while the third waits on the agenda, so a branch names at least two, each with its own value.
        String ir = document(objecttype("Vondst", "Vondsten", null, "", DATUMS),
                meldingOp("melding op vinddag", "Vondst", "vinddatum"),
                meldingOp("melding op aanspoeldag", "Vondst", "aanspoeldatum"),
                meldingOp("melding op bergdag", "Vondst", "bergdatum"));
        String zaak = zaak(List.of(vondst("V", "Vondst", "2026-03-04", "2026-03-01", "2026-03-06")),
                datumVerwacht("V", "meldingsdatum", "2026-03-01"));
        for (DroolsBackend.Branch drl : TAKKEN) {
            String uit = speel(ir, zaak, drl);
            meldtSchrijvers(uit, tak(drl), "schrijvers van één doel geven verschillende waarden",
                    "in het attribuut meldingsdatum van V, een Vondst");
            long genoemd = java.util.stream.Stream.of("de regel \"melding op vinddag\" schrijft 2026-03-04",
                    "de regel \"melding op aanspoeldag\" schrijft 2026-03-01",
                    "de regel \"melding op bergdag\" schrijft 2026-03-06").filter(uit::contains).count();
            assertTrue(genoemd >= 2, tak(drl) + " noemt ten minste twee schrijvers met hun waarde: " + uit);
        }
    }

    @Test void een_schrijver_die_leeg_uitrekent_tegenover_een_die_een_waarde_schrijft() throws Exception {
        // Both conditions read the vinddatum; the second writer's value, the aanspoeldatum, is leeg, and leeg over a
        // value is a write (ch. 1.2g), so the two keep writing 04-03-2026 and leeg. ref faults; the ASP backend gave
        // 04-03-2026 before the fix, because a leeg value derives no atom there.
        String ir = document(objecttype("Vondst", "Vondsten", null, "", DATUMS),
                meldingOp("melding op vinddag", "Vondst", "vinddatum"),
                regel("melding op aanspoeldag", "Vondst", "meldingsdatum", lees("Vondst", "aanspoeldatum"),
                        toets("filled", "Vondst", "vinddatum"), "equation"));
        String zaak = zaak(List.of(vondst("V", "Vondst", "2026-03-04", null, null)),
                datumVerwacht("V", "meldingsdatum", "2026-03-04"));
        for (DroolsBackend.Branch drl : TAKKEN)
            meldtSchrijvers(speel(ir, zaak, drl), tak(drl), "twee schrijvers van één doel geven verschillende waarden",
                    "\"melding op vinddag\" schrijft 2026-03-04",
                    "\"melding op aanspoeldag\" schrijft leeg", "in het attribuut meldingsdatum van V, een Vondst");
    }

    @Test void een_gelijkstelling_naast_een_berekening() throws Exception {
        // A number: "gesteld worden op zijn schatting" beside "berekend worden als zijn waarde min 5,00", schatting
        // 20,00 and waarde 50,00, so 20.00 and 45.00.
        String ir = document(objecttype("Vondst", "Vondsten", null, "", attribuut("waarde", TWEE_DECIMALEN),
                        attribuut("schatting", TWEE_DECIMALEN), attribuut("vindersloon", TWEE_DECIMALEN)),
                regel("vindersloon naar schatting", "Vondst", "vindersloon", lees("Vondst", "schatting"),
                        toets("filled", "Vondst", "schatting"), "equation"),
                regel("vindersloon naar waarde", "Vondst", "vindersloon", WAARDE_MIN_VIJF,
                        toets("filled", "Vondst", "waarde"), "calculation"));
        String zaak = zaak(List.of(instantie("V", "Vondst", "\"schatting\":{\"number\":\"20.00\"},\"waarde\":{\"number\":\"50.00\"}")),
                "{\"kind\":\"attributeValue\",\"label\":\"V\",\"attribute\":\"vindersloon\",\"value\":{\"number\":\"20.00\"}}");
        for (DroolsBackend.Branch drl : TAKKEN)
            meldtSchrijvers(speel(ir, zaak, drl), tak(drl), "\"vindersloon naar schatting\" schrijft 20.00",
                    "\"vindersloon naar waarde\" schrijft 45.00", "in het attribuut vindersloon van V, een Vondst");
    }

    @Test void de_schrijvers_in_een_regelgroep_heten_naar_hun_regel() throws Exception {
        // The engine names a member of a rule group "<groep>: <regel>"; the report names the rule.
        String ir = document(objecttype("Vondst", "Vondsten", null, "", DATUMS),
                List.of("{\"node\":\"RuleGroup\",\"name\":\"melding\",\"recursive\":false,\"members\":[\"melding op vinddag\","
                        + "\"melding op aanspoeldag\"],\"source\":{\"snippet\":\"melding\"}}"),
                meldingOp("melding op vinddag", "Vondst", "vinddatum"),
                meldingOp("melding op aanspoeldag", "Vondst", "aanspoeldatum"));
        String zaak = zaak(List.of(vondst("V", "Vondst", "2026-03-04", "2026-03-01", null)),
                datumVerwacht("V", "meldingsdatum", "2026-03-01"));
        for (DroolsBackend.Branch drl : TAKKEN) {
            String uit = speel(ir, zaak, drl);
            meldtSchrijvers(uit, tak(drl), "de regel \"melding op vinddag\" schrijft 2026-03-04",
                    "de regel \"melding op aanspoeldag\" schrijft 2026-03-01");
            assertFalse(uit.contains("\"melding: "), tak(drl) + " noemt de regel, niet de regelnaam van de engine: " + uit);
        }
    }

    // ---------------- flanks: what keeps playing ----------------

    @Test void de_disjuncte_vorm_speelt() throws Exception {
        // "indien zijn aanspoeldatum gevuld is" beside "indien zijn aanspoeldatum leeg is": one writer per instance.
        String ir = document(objecttype("Vondst", "Vondsten", null, "", DATUMS),
                meldingOp("melding op aanspoeldag", "Vondst", "aanspoeldatum"),
                regel("melding op vinddag", "Vondst", "meldingsdatum", lees("Vondst", "vinddatum"),
                        toets("empty", "Vondst", "aanspoeldatum"), "equation"));
        String zaak = zaak(List.of(vondst("V1", "Vondst", "2026-03-04", "2026-03-01", null),
                        vondst("V2", "Vondst", "2026-03-04", null, null), vondst("V3", "Vondst", null, "2026-03-01", null),
                        vondst("V4", "Vondst", null, null, "2026-03-06")),
                datumVerwacht("V1", "meldingsdatum", "2026-03-01"), datumVerwacht("V2", "meldingsdatum", "2026-03-04"),
                datumVerwacht("V3", "meldingsdatum", "2026-03-01"),
                "{\"kind\":\"attributeEmpty\",\"label\":\"V4\",\"attribute\":\"meldingsdatum\"}");
        for (DroolsBackend.Branch drl : TAKKEN) assertNull(speel(ir, zaak, drl), tak(drl));
    }

    @Test void twee_schrijvers_met_dezelfde_waarde_spelen() throws Exception {
        // Both conditions hold and both values are 02-03-2026: both equations hold, the fixpoint exists.
        String ir = document(objecttype("Vondst", "Vondsten", null, "", DATUMS),
                meldingOp("melding op vinddag", "Vondst", "vinddatum"),
                meldingOp("melding op aanspoeldag", "Vondst", "aanspoeldatum"));
        String zaak = zaak(List.of(vondst("V", "Vondst", "2026-03-02", "2026-03-02", null)),
                datumVerwacht("V", "meldingsdatum", "2026-03-02"));
        for (DroolsBackend.Branch drl : TAKKEN) assertNull(speel(ir, zaak, drl), tak(drl));
    }

    @Test void twee_kenmerktoekenningen_spelen() throws Exception {
        // A characteristic is monotone (ch. 5.3): two attributions that both hold set it once.
        String ir = document(objecttype("Vondst", "Vondsten", null,
                        "{\"node\":\"Characteristic\",\"name\":\"meldplichtig\",\"kind\":\"adjectival\"}", DATUMS),
                kenmerkRegel("meldplicht na vinden", "Vondst", "meldplichtig", toets("filled", "Vondst", "vinddatum")),
                kenmerkRegel("meldplicht na aanspoelen", "Vondst", "meldplichtig", toets("filled", "Vondst", "aanspoeldatum")));
        String zaak = zaak(List.of(vondst("V1", "Vondst", "2026-03-04", "2026-03-01", null), vondst("V2", "Vondst", null, null, null)),
                "{\"kind\":\"characteristicPresent\",\"label\":\"V1\",\"characteristic\":\"meldplichtig\"}",
                "{\"kind\":\"characteristicAbsent\",\"label\":\"V2\",\"characteristic\":\"meldplichtig\"}");
        for (DroolsBackend.Branch drl : TAKKEN) assertNull(speel(ir, zaak, drl), tak(drl));
    }

    // ---------------- assertions ----------------

    /** The truth-maintenance branch, which gives the divergence as the fault its write-back throws at the fixpoint,
     *  and the DRL-text branch, which gives it as the verdict of the case. */
    private static final List<DroolsBackend.Branch> TAKKEN = List.of(DroolsBackend.Branch.TRUTH_MAINTENANCE,
            DroolsBackend.Branch.DRL_TEXT);

    private static String tak(DroolsBackend.Branch tak) {
        return tak == DroolsBackend.Branch.DRL_TEXT ? "DRL-teksttak" : "tak met truth maintenance";
    }

    /** The verdict names the divergence with each of {@code fragmenten}, and no cycle. */
    private static void meldtSchrijvers(String uit, String wie, String... fragmenten) {
        assertNotNull(uit, wie + ": speelt, waar de twee vergelijkingen geen fixpunt hebben");
        assertTrue(uit.contains("geen fixpunt"), wie + ": " + uit);
        assertTrue(uit.contains("divergentie is een fout (RUNTIME-SEMANTICS ch. 5.7)"), wie + ": " + uit);
        assertFalse(uit.contains("kringverwijzing"), wie + " noemt een kring die het document niet heeft: " + uit);
        for (String f : fragmenten) assertTrue(uit.contains(f), wie + " noemt '" + f + "': " + uit);
    }

    // ---------------- documents ----------------

    private static final String DATUM = "{\"node\":\"Date\",\"precision\":\"day\"}";
    private static final String TWEE_DECIMALEN = "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}";
    private static final String GEHEEL = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";

    private static String attribuut(String naam, String datatype) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + datatype + "}";
    }

    private static final String DATUMS = String.join(",", attribuut("vinddatum", DATUM), attribuut("aanspoeldatum", DATUM),
            attribuut("bergdatum", DATUM), attribuut("meldingsdatum", DATUM));

    private static String objecttype(String naam, String meervoud, String supertype, String kenmerken, String... attributen) {
        return "{\"node\":\"ObjectType\",\"name\":\"" + naam + "\"" + (supertype == null ? "" : ",\"supertype\":\"" + supertype + "\"")
                + ",\"animate\":false,\"plural\":\"" + meervoud + "\",\"attributes\":[" + String.join(",", attributen)
                + "],\"characteristics\":[" + kenmerken + "]}";
    }

    private static String lees(String type, String attribuut) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"" + type + "\",\"source\":"
                + "{\"snippet\":\"zijn " + attribuut + "\"}}";
    }

    /** "zijn &lt;attribuut&gt; gevuld is" ({@code filled}) or "leeg is" ({@code empty}). */
    private static String toets(String soort, String type, String attribuut) {
        return "{\"node\":\"Predicate\",\"kind\":\"" + soort + "\",\"operand\":" + lees(type, attribuut) + ",\"negated\":false,"
                + "\"digits\":0,\"source\":{\"snippet\":\"zijn " + attribuut + (soort.equals("filled") ? " gevuld is" : " leeg is") + "\"}}";
    }

    /** "zijn waarde min 5,00", as the frontend writes it. */
    private static final String WAARDE_MIN_VIJF = "{\"node\":\"Binary\",\"op\":\"minus\",\"left\":" + lees("Vondst", "waarde")
            + ",\"right\":{\"node\":\"Literal\",\"value\":{\"number\":\"5.00\"},\"type\":" + GEHEEL + ",\"source\":{\"snippet\":"
            + "\"5,00\"}},\"type\":" + TWEE_DECIMALEN + ",\"source\":{\"snippet\":\"zijn waarde min 5,00\"}}";

    private static String regel(String naam, String type, String doel, String waarde, String voorwaarde, String presentatie) {
        return "{\"node\":\"Rule\",\"name\":\"" + naam + "\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":"
                + "{\"node\":\"AttributeReference\",\"attribute\":\"" + doel + "\",\"subjectType\":\"" + type + "\",\"source\":"
                + "{\"snippet\":\"De " + doel + " van een " + type + "\"}},\"value\":" + waarde
                + (voorwaarde == null ? "" : ",\"condition\":" + voorwaarde)
                + ",\"initialisation\":false,\"presentation\":\"" + presentatie + "\"}]}";
    }

    /** "De meldingsdatum van een &lt;type&gt; moet gesteld worden op zijn &lt;bron&gt; indien zijn &lt;bron&gt; gevuld is." */
    private static String meldingOp(String naam, String type, String bron) {
        return regel(naam, type, "meldingsdatum", lees(type, bron), toets("filled", type, bron), "equation");
    }

    private static String kenmerkRegel(String naam, String type, String kenmerk, String voorwaarde) {
        return "{\"node\":\"Rule\",\"name\":\"" + naam + "\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":"
                + "{\"node\":\"AttributeReference\",\"attribute\":\"" + kenmerk + "\",\"subjectType\":\"" + type + "\",\"source\":"
                + "{\"snippet\":\"Een " + type + " is " + kenmerk + "\"}},\"value\":{\"node\":\"Literal\",\"value\":{\"boolean\":true},"
                + "\"type\":{\"node\":\"Boolean\"},\"source\":{\"snippet\":\"waar\"}},\"condition\":" + voorwaarde
                + ",\"initialisation\":false}]}";
    }

    private static String document(String objecttypen, String... regels) {
        return document(objecttypen, List.of(), regels);
    }

    private static String document(String objecttypen, List<String> groepen, String... regels) {
        return "{\"format\":\"uchar-ir-v1\",\"model\":{\"objectTypes\":[" + objecttypen + "],\"factTypes\":[],\"parameters\":[],"
                + "\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]},\"rules\":{\"rules\":["
                + String.join(",", regels) + "],\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],"
                + "\"consistencies\":[],\"uniquenesses\":[],\"factCreations\":[],\"startPointDeterminations\":[],\"ruleGroups\":["
                + String.join(",", groepen) + "]},\"diagnostics\":[]}";
    }

    // ---------------- cases ----------------

    private static String instantie(String label, String type, String attributen) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + attributen + "},\"roles\":{},"
                + "\"characteristics\":[]}";
    }

    private static String vondst(String label, String type, String vinddatum, String aanspoeldatum, String bergdatum) {
        List<String> a = new ArrayList<>();
        if (vinddatum != null) a.add("\"vinddatum\":{\"date\":\"" + vinddatum + "\"}");
        if (aanspoeldatum != null) a.add("\"aanspoeldatum\":{\"date\":\"" + aanspoeldatum + "\"}");
        if (bergdatum != null) a.add("\"bergdatum\":{\"date\":\"" + bergdatum + "\"}");
        return instantie(label, type, String.join(",", a));
    }

    private static String datumVerwacht(String label, String attribuut, String iso) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"" + attribuut + "\",\"value\":{\"date\":\""
                + iso + "\"}}";
    }

    private static String zaak(List<String> instanties, String... verwacht) {
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + String.join(",", instanties)
                + "],\"parameters\":{}},\"expected\":[" + String.join(",", verwacht) + "]}";
    }

    /** One branch's verdict on one case: null when every expectation holds, and the message of the fault the replay
     *  throws, which on the truth-maintenance branch carries a divergence at the fixpoint. */
    private static String speel(String ir, String zaak, DroolsBackend.Branch tak) throws Exception {
        Path tmp = Files.createTempFile("overlappende-schrijvers", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        JsonNode c = JSON.readTree(zaak);
        try {
            return CaseRunner.speelCase(tmp, c, tak);
        } catch (RuntimeException e) {
            return e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }
}
