package dev.uchar.regelspraak.loader;

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
 * {@link #the_duration_for_the_date_is_refused}), a text operand under a square root faulted at fire, and the other forms
 * were refused as rules out of scope, in each branch's own words.
 *
 * <p>Every fixture is a corpus golden with ONE twist in memory; the corpus file is read and never written.
 */
class TypingRefusalWitnessTest {

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
    private static final String BOUNDARY_EDGE = "synth-grensrand";

    private static final String VALUE = "/rules/rules/0/versions/0/value";
    private static final String MS_VALUE = "/rules/rules/2/versions/0/value";
    private static final String ABS_VALUE = "/rules/rules/2/versions/0/value";

    private static final String DAG = "{\"node\":\"AttributeReference\",\"attribute\":\"aangiftedatum\","
            + "\"subjectType\":\"Vondst\",\"source\":{\"snippet\":\"zijn aangiftedatum\"}}";
    private static final String AANKOMST = "{\"node\":\"AttributeReference\",\"attribute\":\"aankomst\","
            + "\"subjectType\":\"Getijdenpaar\",\"source\":{\"snippet\":\"zijn aankomst\"}}";
    private static final String DAY_DATE = "{\"node\":\"AttributeReference\",\"attribute\":\"dagdatum\","
            + "\"subjectType\":\"Getijdenpaar\",\"source\":{\"snippet\":\"zijn dagdatum\"}}";

    // ---------------- WF-035: the form of date ± x ----------------

    @Test void date_plus_date_is_refused_in_each_precision() throws Exception {
        String sentence = ": date ± date is ill-formed (date arithmetic takes a date and a duration)";
        ObjectNode ir = golden(VERVALTERMIJN);
        set(ir, VALUE + "/right", DAG);
        assertTrue(refuses(ir, VALUE).getMessage().endsWith(sentence));
        ir = golden(GETIJDENPAAR);
        set(ir, MS_VALUE + "/right", AANKOMST);
        assertTrue(refuses(ir, MS_VALUE).getMessage().endsWith(sentence));
        // mixed precision: a day date beside a millisecond one is two dates all the same
        ir = golden(GETIJDENPAAR);
        addDayDateAttribute(ir);
        set(ir, MS_VALUE + "/left", DAY_DATE);
        set(ir, MS_VALUE + "/right", AANKOMST);
        assertTrue(refuses(ir, MS_VALUE).getMessage().endsWith(sentence));
    }

    /** The form that PLAYED: "2 wk plus zijn aangiftedatum" answered the aangiftedatum plus two weeks in both branches,
     *  the operands commuted without a word, where the verifier, ref and the ASP backend refuse it. */
    @Test void the_duration_for_the_date_is_refused() throws Exception {
        ObjectNode ir = golden(VERVALTERMIJN);
        ObjectNode w = (ObjectNode) ir.at(VALUE);
        JsonNode left = w.get("left");
        w.set("left", w.get("right"));
        w.set("right", left);
        assertTrue(refuses(ir, VALUE).getMessage().endsWith(": duration + date is ill-formed (the date stands on the"
                + " left; date arithmetic does not commute)"));
        w.put("op", "minus");
        assertTrue(refuses(ir, VALUE).getMessage().endsWith(": duration − date is ill-formed (subtraction admits"
                + " the date on the left only)"));
    }

    @Test void the_step_carries_a_number_with_a_time_unit() throws Exception {
        ObjectNode ir = golden(VERVALTERMIJN);
        set(ir, VALUE + "/right", number("3", "vadem"));
        assertTrue(refuses(ir, VALUE + "/right").getMessage().endsWith(": date ± duration: the duration unit"
                + " 'vadem' is neither a recognised time unit nor a declared day kind"));
        ir = golden(VERVALTERMIJN);
        set(ir, VALUE + "/right", number("3", null));
        assertTrue(refuses(ir, VALUE + "/right").getMessage().endsWith(": date ± duration: the duration operand"
                + " carries no time unit"));
        ir = golden(VERVALTERMIJN);
        set(ir, VALUE + "/right", text("drie weken"));
        assertTrue(refuses(ir, VALUE + "/right").getMessage().endsWith(": date ± duration: the duration operand"
                + " is text, must be a number carrying a time unit"));
    }

    @Test void a_unit_under_the_day_requires_a_date_time() throws Exception {
        String sentence = ": sub-day time unit on a day-precision date (millisecond precision required)";
        ObjectNode ir = golden(VERVALTERMIJN);
        set(ir, VALUE + "/right", number("3", "uur"));
        assertTrue(refuses(ir, VALUE).getMessage().endsWith(sentence));
        // a day date written into a millisecond target: the form is the date's, not the target's
        ir = golden(GETIJDENPAAR);
        addDayDateAttribute(ir);
        set(ir, MS_VALUE + "/left", DAY_DATE);
        assertTrue(refuses(ir, MS_VALUE).getMessage().endsWith(sentence));
    }

    // ---------------- WF-038: the operands of the builtins ----------------

    @Test void square_root_and_absolute_value_take_a_number() throws Exception {
        ObjectNode ir = golden(BOUNDARY_EDGE);
        set(ir, VALUE, builtinCall("absoluteValue", text("vier"), numberType(null)));
        assertTrue(refuses(ir, VALUE).getMessage().endsWith(": absoluteValue operand is text, must be a number"));
        ir = golden(BOUNDARY_EDGE);
        set(ir, VALUE, "{\"node\":\"Rounding\",\"base\":"
                + builtinCall("squareRoot", "{\"node\":\"Literal\",\"value\":{\"boolean\":true},\"type\":{\"node\":"
                        + "\"Boolean\"},\"source\":{\"snippet\":\"waar\"}}", numberType(null))
                + ",\"mode\":\"halfAwayFromZero\",\"decimals\":0,\"type\":" + numberType(null)
                + ",\"source\":{\"snippet\":\"afgerond\"}}");
        assertTrue(refuses(ir, VALUE + "/base").getMessage()
                .endsWith(": squareRoot operand is boolean, must be a number"));
    }

    @Test void the_duration_counts_between_two_dates_in_a_time_unit() throws Exception {
        ObjectNode ir = golden(JUTLEEFTIJD);
        set(ir, VALUE + "/arguments/0", text("gisteren"));
        assertTrue(refuses(ir, VALUE + "/arguments/0").getMessage()
                .endsWith(": duration operand is text, must be a date"));

        ir = golden(JUTLEEFTIJD);
        ((ArrayNode) ir.at("/model/objectTypes/0/attributes")).add(JSON.readTree("{\"node\":\"Attribute\","
                + "\"name\":\"stempelmoment\",\"datatype\":{\"node\":\"Date\",\"precision\":\"millisecond\"}}"));
        set(ir, VALUE + "/arguments/1", "{\"node\":\"AttributeReference\",\"attribute\":\"stempelmoment\","
                + "\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"zijn stempelmoment\"}}");
        assertTrue(refuses(ir, VALUE).getMessage()
                .endsWith(": duration operands mix date precisions (both must share one precision)"));

        ir = golden(JUTLEEFTIJD);
        ((ObjectNode) ir.at(VALUE)).set("type", JSON.readTree(numberType(null)));
        assertTrue(refuses(ir, VALUE).getMessage()
                .endsWith(": duration without a recognised time unit on its node type"));

        ir = golden(JUTLEEFTIJD);
        ((ObjectNode) ir.at(VALUE + "/type/unit")).put("abbreviation", "uur");
        ((ObjectNode) ir.at("/model/objectTypes/0/attributes/2/datatype/unit")).put("abbreviation", "uur");
        assertTrue(refuses(ir, VALUE).getMessage().endsWith(": sub-day duration unit on day-precision operands"
                + " (millisecond precision required)"));
    }

    /** The counted unit that is neither a time unit nor a declared day kind: the sentence names the builtin's own
     *  kind. */
    @Test void an_unknown_count_unit_names_the_own_kind() throws Exception {
        ObjectNode ir = golden(JUTLEEFTIJD);
        ((ObjectNode) ir.at(VALUE + "/type/unit")).put("abbreviation", "vadem");
        ((ObjectNode) ir.at("/model/objectTypes/0/attributes/2/datatype/unit")).put("abbreviation", "vadem");
        assertTrue(refuses(ir, VALUE).getMessage().endsWith(": duration: the counted unit 'vadem' is neither a"
                + " recognised time unit nor a declared day kind"));
        ir = golden(JUTLEEFTIJD);
        ((ObjectNode) ir.at(ABS_VALUE + "/type/unit")).put("abbreviation", "vadem");
        ((ObjectNode) ir.at("/model/objectTypes/0/attributes/4/datatype/unit")).put("abbreviation", "vadem");
        assertTrue(refuses(ir, ABS_VALUE).getMessage().endsWith(": absoluteDuration: the counted unit 'vadem' is"
                + " neither a recognised time unit nor a declared day kind"));
    }

    // ---------------- fixtures ----------------

    private static String numberType(String unit) {
        return "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\""
                + (unit == null ? "" : ",\"unit\":{\"node\":\"Unit\",\"abbreviation\":\"" + unit + "\"}") + "}";
    }

    private static String number(String lexeme, String unit) {
        return "{\"node\":\"Literal\",\"value\":{\"number\":\"" + lexeme + "\"},\"type\":" + numberType(unit)
                + ",\"source\":{\"snippet\":\"" + lexeme + "\"}}";
    }

    private static String text(String t) {
        return "{\"node\":\"Literal\",\"value\":{\"text\":\"" + t + "\"},\"type\":{\"node\":\"Text\"},"
                + "\"source\":{\"snippet\":\"'" + t + "'\"}}";
    }

    private static String builtinCall(String kind, String argument, String type) {
        return "{\"node\":\"BuiltinCall\",\"kind\":\"" + kind + "\",\"arguments\":[" + argument + "],\"type\":" + type
                + ",\"source\":{\"snippet\":\"" + kind + "\"}}";
    }

    /** A day-precision attribute beside the two millisecond ones of the Getijdenpaar. */
    private static void addDayDateAttribute(ObjectNode ir) throws Exception {
        ((ArrayNode) ir.at("/model/objectTypes/0/attributes")).add(JSON.readTree("{\"node\":\"Attribute\","
                + "\"name\":\"dagdatum\",\"datatype\":{\"node\":\"Date\",\"precision\":\"day\"}}"));
    }

    private static void set(ObjectNode ir, String path, String json) throws Exception {
        int slash = path.lastIndexOf('/');
        JsonNode parent = ir.at(path.substring(0, slash));
        if (parent instanceof ArrayNode a) {
            a.set(Integer.parseInt(path.substring(slash + 1)), JSON.readTree(json));
            return;
        }
        if (!(parent instanceof ObjectNode o)) throw new AssertionError("no object at " + path.substring(0, slash));
        o.set(path.substring(slash + 1), JSON.readTree(json));
    }

    private static IrLoader.Documentfout refuses(JsonNode ir, String site) {
        IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.loadAll(ir),
                "verwacht een documentfout op " + site);
        assertEquals(site, e.site(), e.getMessage());
        return e;
    }

    private static ObjectNode golden(String name) throws Exception {
        Path ir = CORPUS.resolve(name).resolve("ir.json");
        assertTrue(Files.exists(ir), "golden ir.json present: " + ir.toAbsolutePath());
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(ir));
        IrLoader.loadAll(doc);   // the untwisted golden loads, so the refusal below is the twist's alone
        return doc;
    }
}
