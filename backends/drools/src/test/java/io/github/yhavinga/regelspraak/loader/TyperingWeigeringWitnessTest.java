package io.github.yhavinga.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The loader on the operand typing of WF-035 (date ± duration) and WF-038 (a square root, an absolute value and the
 * duration clause): each form the verifier refuses is refused here at load, as an {@link IrLoader.Documentfout} at the
 * verifier's pointer and in the sentence the verifier, {@code ref} and the ASP backend share, written out literally
 * below so that a sentence which drifts from theirs fails here. Before the loader asked, a duration plus a date PLAYED
 * in both branches with the addition silently commuted (measured on the twist of
 * {@link #de_duur_voor_de_datum_weigert}), a text operand under a square root faulted at fire, and the other forms
 * were refused as rules out of scope, in each branch's own words.
 *
 * <p>Every fixture is a corpus golden with ONE twist in memory; the corpus file is read and never written.
 */
class TyperingWeigeringWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** Rule 0: a Vondst's aangiftedatum (day precision) plus 2 wk. */
    private static final String VERVALTERMIJN = "jutterswet-vervaltermijn";
    /** Rule 2: a Getijdenpaar's aankomst (millisecond precision) plus 26 uur. */
    private static final String GETIJDENPAAR = "jutterswet-getijdenpaar";
    /** Rule 0: the duration from a Jutter's geboortedatum to its peildatum in jr; rule 2 the absolute one in mnd. */
    private static final String JUTLEEFTIJD = "anchor/jutterswet-jutleeftijd";
    /** Rule 0: a product of two numbers of a Meting. */
    private static final String GRENSRAND = "synth-grensrand";

    private static final String WAARDE = "/rules/rules/0/versions/0/value";
    private static final String MS_WAARDE = "/rules/rules/2/versions/0/value";
    private static final String ABS_WAARDE = "/rules/rules/2/versions/0/value";

    private static final String DAG = "{\"node\":\"AttributeReference\",\"attribute\":\"aangiftedatum\","
            + "\"subjectType\":\"Vondst\",\"source\":{\"snippet\":\"zijn aangiftedatum\"}}";
    private static final String AANKOMST = "{\"node\":\"AttributeReference\",\"attribute\":\"aankomst\","
            + "\"subjectType\":\"Getijdenpaar\",\"source\":{\"snippet\":\"zijn aankomst\"}}";
    private static final String DAGDATUM = "{\"node\":\"AttributeReference\",\"attribute\":\"dagdatum\","
            + "\"subjectType\":\"Getijdenpaar\",\"source\":{\"snippet\":\"zijn dagdatum\"}}";

    // ---------------- WF-035: the form of date ± x ----------------

    @Test void datum_plus_datum_weigert_in_elke_precisie() throws Exception {
        String zin = ": date ± date is ill-formed (date arithmetic takes a date and a duration)";
        ObjectNode ir = golden(VERVALTERMIJN);
        zet(ir, WAARDE + "/right", DAG);
        assertTrue(weigert(ir, WAARDE).getMessage().endsWith(zin));
        ir = golden(GETIJDENPAAR);
        zet(ir, MS_WAARDE + "/right", AANKOMST);
        assertTrue(weigert(ir, MS_WAARDE).getMessage().endsWith(zin));
        // mixed precision: a day date beside a millisecond one is two dates all the same
        ir = golden(GETIJDENPAAR);
        dagdatumErbij(ir);
        zet(ir, MS_WAARDE + "/left", DAGDATUM);
        zet(ir, MS_WAARDE + "/right", AANKOMST);
        assertTrue(weigert(ir, MS_WAARDE).getMessage().endsWith(zin));
    }

    /** The form that PLAYED: "2 wk plus zijn aangiftedatum" answered the aangiftedatum plus two weeks in both branches,
     *  the operands commuted without a word, where the verifier, ref and the ASP backend refuse it. */
    @Test void de_duur_voor_de_datum_weigert() throws Exception {
        ObjectNode ir = golden(VERVALTERMIJN);
        ObjectNode w = (ObjectNode) ir.at(WAARDE);
        JsonNode links = w.get("left");
        w.set("left", w.get("right"));
        w.set("right", links);
        assertTrue(weigert(ir, WAARDE).getMessage().endsWith(": duration + date is ill-formed (the date stands on the"
                + " left; date arithmetic does not commute)"));
        w.put("op", "minus");
        assertTrue(weigert(ir, WAARDE).getMessage().endsWith(": duration − date is ill-formed (subtraction admits"
                + " the date on the left only)"));
    }

    @Test void de_stap_draagt_een_getal_met_een_tijdseenheid() throws Exception {
        ObjectNode ir = golden(VERVALTERMIJN);
        zet(ir, WAARDE + "/right", getal("3", "vadem"));
        assertTrue(weigert(ir, WAARDE + "/right").getMessage().endsWith(": date ± duration: the duration unit"
                + " 'vadem' is neither a recognised time unit nor a declared day kind"));
        ir = golden(VERVALTERMIJN);
        zet(ir, WAARDE + "/right", getal("3", null));
        assertTrue(weigert(ir, WAARDE + "/right").getMessage().endsWith(": date ± duration: the duration operand"
                + " carries no time unit"));
        ir = golden(VERVALTERMIJN);
        zet(ir, WAARDE + "/right", tekst("drie weken"));
        assertTrue(weigert(ir, WAARDE + "/right").getMessage().endsWith(": date ± duration: the duration operand"
                + " is text, must be a number carrying a time unit"));
    }

    @Test void een_eenheid_onder_de_dag_vraagt_een_datum_tijd() throws Exception {
        String zin = ": sub-day time unit on a day-precision date (millisecond precision required)";
        ObjectNode ir = golden(VERVALTERMIJN);
        zet(ir, WAARDE + "/right", getal("3", "uur"));
        assertTrue(weigert(ir, WAARDE).getMessage().endsWith(zin));
        // a day date written into a millisecond target: the form is the date's, not the target's
        ir = golden(GETIJDENPAAR);
        dagdatumErbij(ir);
        zet(ir, MS_WAARDE + "/left", DAGDATUM);
        assertTrue(weigert(ir, MS_WAARDE).getMessage().endsWith(zin));
    }

    // ---------------- WF-038: the operands of the builtins ----------------

    @Test void wortel_en_absolute_waarde_nemen_een_getal() throws Exception {
        ObjectNode ir = golden(GRENSRAND);
        zet(ir, WAARDE, bouwsteen("absoluteValue", tekst("vier"), getaltype(null)));
        assertTrue(weigert(ir, WAARDE).getMessage().endsWith(": absoluteValue operand is text, must be a number"));
        ir = golden(GRENSRAND);
        zet(ir, WAARDE, "{\"node\":\"Rounding\",\"base\":"
                + bouwsteen("squareRoot", "{\"node\":\"Literal\",\"value\":{\"boolean\":true},\"type\":{\"node\":"
                        + "\"Boolean\"},\"source\":{\"snippet\":\"waar\"}}", getaltype(null))
                + ",\"mode\":\"halfAwayFromZero\",\"decimals\":0,\"type\":" + getaltype(null)
                + ",\"source\":{\"snippet\":\"afgerond\"}}");
        assertTrue(weigert(ir, WAARDE + "/base").getMessage()
                .endsWith(": squareRoot operand is boolean, must be a number"));
    }

    @Test void de_tijdsduur_telt_tussen_twee_datums_in_een_tijdseenheid() throws Exception {
        ObjectNode ir = golden(JUTLEEFTIJD);
        zet(ir, WAARDE + "/arguments/0", tekst("gisteren"));
        assertTrue(weigert(ir, WAARDE + "/arguments/0").getMessage()
                .endsWith(": duration operand is text, must be a date"));

        ir = golden(JUTLEEFTIJD);
        ((ArrayNode) ir.at("/model/objectTypes/0/attributes")).add(JSON.readTree("{\"node\":\"Attribute\","
                + "\"name\":\"stempelmoment\",\"datatype\":{\"node\":\"Date\",\"precision\":\"millisecond\"}}"));
        zet(ir, WAARDE + "/arguments/1", "{\"node\":\"AttributeReference\",\"attribute\":\"stempelmoment\","
                + "\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"zijn stempelmoment\"}}");
        assertTrue(weigert(ir, WAARDE).getMessage()
                .endsWith(": duration operands mix date precisions (both must share one precision)"));

        ir = golden(JUTLEEFTIJD);
        ((ObjectNode) ir.at(WAARDE)).set("type", JSON.readTree(getaltype(null)));
        assertTrue(weigert(ir, WAARDE).getMessage()
                .endsWith(": duration without a recognised time unit on its node type"));

        ir = golden(JUTLEEFTIJD);
        ((ObjectNode) ir.at(WAARDE + "/type/unit")).put("abbreviation", "uur");
        ((ObjectNode) ir.at("/model/objectTypes/0/attributes/2/datatype/unit")).put("abbreviation", "uur");
        assertTrue(weigert(ir, WAARDE).getMessage().endsWith(": sub-day duration unit on day-precision operands"
                + " (millisecond precision required)"));
    }

    /** The counted unit that is neither a time unit nor a declared day kind: the sentence names the builtin's own
     *  kind. */
    @Test void een_onbekende_teleenheid_noemt_de_eigen_soort() throws Exception {
        ObjectNode ir = golden(JUTLEEFTIJD);
        ((ObjectNode) ir.at(WAARDE + "/type/unit")).put("abbreviation", "vadem");
        ((ObjectNode) ir.at("/model/objectTypes/0/attributes/2/datatype/unit")).put("abbreviation", "vadem");
        assertTrue(weigert(ir, WAARDE).getMessage().endsWith(": duration: the counted unit 'vadem' is neither a"
                + " recognised time unit nor a declared day kind"));
        ir = golden(JUTLEEFTIJD);
        ((ObjectNode) ir.at(ABS_WAARDE + "/type/unit")).put("abbreviation", "vadem");
        ((ObjectNode) ir.at("/model/objectTypes/0/attributes/4/datatype/unit")).put("abbreviation", "vadem");
        assertTrue(weigert(ir, ABS_WAARDE).getMessage().endsWith(": absoluteDuration: the counted unit 'vadem' is"
                + " neither a recognised time unit nor a declared day kind"));
    }

    // ---------------- fixtures ----------------

    private static String getaltype(String eenheid) {
        return "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\""
                + (eenheid == null ? "" : ",\"unit\":{\"node\":\"Unit\",\"abbreviation\":\"" + eenheid + "\"}") + "}";
    }

    private static String getal(String lexeem, String eenheid) {
        return "{\"node\":\"Literal\",\"value\":{\"number\":\"" + lexeem + "\"},\"type\":" + getaltype(eenheid)
                + ",\"source\":{\"snippet\":\"" + lexeem + "\"}}";
    }

    private static String tekst(String t) {
        return "{\"node\":\"Literal\",\"value\":{\"text\":\"" + t + "\"},\"type\":{\"node\":\"Text\"},"
                + "\"source\":{\"snippet\":\"'" + t + "'\"}}";
    }

    private static String bouwsteen(String soort, String argument, String type) {
        return "{\"node\":\"BuiltinCall\",\"kind\":\"" + soort + "\",\"arguments\":[" + argument + "],\"type\":" + type
                + ",\"source\":{\"snippet\":\"" + soort + "\"}}";
    }

    /** A day-precision attribute beside the two millisecond ones of the Getijdenpaar. */
    private static void dagdatumErbij(ObjectNode ir) throws Exception {
        ((ArrayNode) ir.at("/model/objectTypes/0/attributes")).add(JSON.readTree("{\"node\":\"Attribute\","
                + "\"name\":\"dagdatum\",\"datatype\":{\"node\":\"Date\",\"precision\":\"day\"}}"));
    }

    private static void zet(ObjectNode ir, String pad, String json) throws Exception {
        int slash = pad.lastIndexOf('/');
        JsonNode ouder = ir.at(pad.substring(0, slash));
        if (ouder instanceof ArrayNode a) {
            a.set(Integer.parseInt(pad.substring(slash + 1)), JSON.readTree(json));
            return;
        }
        if (!(ouder instanceof ObjectNode o)) throw new AssertionError("no object at " + pad.substring(0, slash));
        o.set(pad.substring(slash + 1), JSON.readTree(json));
    }

    private static IrLoader.Documentfout weigert(JsonNode ir, String plek) {
        IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.loadAll(ir),
                "verwacht een documentfout op " + plek);
        assertEquals(plek, e.plek(), e.getMessage());
        return e;
    }

    private static ObjectNode golden(String naam) throws Exception {
        Path ir = CORPUS.resolve(naam).resolve("ir.json");
        assertTrue(Files.exists(ir), "golden ir.json present: " + ir.toAbsolutePath());
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(ir));
        IrLoader.loadAll(doc);   // the untwisted golden loads, so the refusal below is the twist's alone
        return doc;
    }
}
