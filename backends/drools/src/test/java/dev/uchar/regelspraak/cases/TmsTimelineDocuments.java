package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.kern.Namen;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The hand-written documents and cases of the truth-maintenance witnesses over timelines (RUNTIME-SEMANTICS ch. 3): a
 * member on a timeline, a characteristic on a timeline, the pro-rata part and the day count that write one, the
 * temporal total that reads one, and a case that carries timelines, on Jutterswet material in the wire's shape
 * ({@link IrPath} for a rule). Next to {@link TmsDocuments}, whose builders hold one value per member.
 */
final class TmsTimelineDocuments {
    private TmsTimelineDocuments() {}

    static final String WHOLE = TmsDocuments.WHOLE;

    /** A timeline facet of {@code granularity} in its wire spelling ({@code month}, {@code year}, …), with the fixed
     *  start point {@code vast} (null on the calendar grid). */
    static String facet(String granularity, String fixed) {
        return "{\"node\":\"TimelineFacet\",\"granularity\":\"" + granularity + "\",\"count\":1"
                + (fixed == null ? "" : ",\"fixedStart\":\"" + fixed + "\"") + "}";
    }

    /** An attribute of {@code datatype} on a timeline of {@code granularity}. */
    static String timeAttribute(String naam, String datatype, String granularity) {
        return timeAttribute(naam, datatype, granularity, null);
    }

    /** As {@link #timeAttribute(String, String, String)}, on the grid of the fixed start point {@code vast}. */
    static String timeAttribute(String naam, String datatype, String granularity, String fixed) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + datatype + ",\"timeline\":"
                + facet(granularity, fixed) + "}";
    }

    /** A characteristic that holds one truth value. */
    static String kenmerk(String naam) {
        return "{\"node\":\"Characteristic\",\"name\":\"" + naam + "\",\"kind\":\"adjectival\"}";
    }

    /** A characteristic on a timeline of {@code granularity}: its value is its true periods. */
    static String timeCharacteristic(String naam, String granularity) {
        return "{\"node\":\"Characteristic\",\"name\":\"" + naam + "\",\"kind\":\"adjectival\",\"timeline\":"
                + facet(granularity, null) + "}";
    }

    /** An object type with its attributes and characteristics as written ({@link #timeAttribute}, {@link #kenmerk}). */
    static String objecttype(String naam, List<String> attributes, List<String> kenmerken) {
        return "{\"node\":\"ObjectType\",\"name\":\"" + naam + "\",\"animate\":true,\"plural\":\"" + naam + "s\","
                + "\"attributes\":[" + String.join(",", attributes) + "],\"characteristics\":[" + String.join(",", kenmerken)
                + "]}";
    }

    /** "het tijdsevenredig deel per {@code granularity} van zijn {@code basis} gedurende de tijd dat {@code filter}"
     *  (RUNTIME-SEMANTICS ch. 3.7). */
    static String timeProportional(String base, String type, String filter, String granularity) {
        return "{\"node\":\"BuiltinCall\",\"kind\":\"timeProportionalPart\",\"arguments\":[" + TmsDocuments.reading(base, type)
                + "],\"filter\":" + filter + ",\"granularity\":\"" + granularity + "\",\"type\":" + WHOLE
                + ",\"source\":{\"snippet\":\"het tijdsevenredig deel per " + granularity + " van zijn " + base + "\"}}";
    }

    /** "het aantal dagen in {@code granularity} dat {@code filter}" (RUNTIME-SEMANTICS ch. 3.8). */
    static String dayCount(String filter, String granularity) {
        return "{\"node\":\"BuiltinCall\",\"kind\":\"dayCount\",\"arguments\":[],\"filter\":" + filter + ",\"granularity\":\""
                + granularity + "\",\"type\":" + WHOLE + ",\"source\":{\"snippet\":\"het aantal dagen in de "
                + granularity + "\"}}";
    }

    /** "het totaal van zijn {@code attribuut}", optionally "gedurende" {@code filter} ("null" for none): the total of the
     *  subject's own timeline (RUNTIME-SEMANTICS ch. 3.6), a fold without a range. */
    static String ownTotal(String attribuut, String type, String filter) {
        return "{\"node\":\"Aggregation\",\"kind\":\"total\",\"arguments\":[" + TmsDocuments.reading(attribuut, type)
                + "],\"elementType\":\"" + type + "\"" + (filter.equals("null") ? "" : ",\"filter\":" + filter)
                + ",\"type\":" + WHOLE + ",\"source\":{\"snippet\":\"het totaal van zijn " + attribuut + "\"}}";
    }

    /** The document of {@code model} with {@code rules} and {@code uniquenesses}, each in its order. */
    static Path document(String model, List<String> rules, List<String> uniquenesses) throws Exception {
        Path tmp = Files.createTempFile("tms-tijdlijn", ".json");
        tmp.toFile().deleteOnExit();
        return Files.writeString(tmp, ir(model, rules, uniquenesses));
    }

    /** The text of the document of {@link #document}. */
    static String ir(String model, List<String> rules, List<String> uniquenesses) {
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + model + ",\"rules\":{\"rules\":[" + String.join(",", rules)
                + "],\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],"
                + "\"uniquenesses\":[" + String.join(",", uniquenesses) + "],\"factCreations\":[],"
                + "\"startPointDeterminations\":[],\"ruleGroups\":[],\"prioritizedDefaults\":[]},\"diagnostics\":[]}";
    }

    /** One segment of a case timeline, {@code [van, tot)} with the number {@code lexeme}. */
    static String segment(String van, String to, String lexeme) {
        return "{\"from\":\"" + van + "\",\"to\":\"" + to + "\",\"value\":{\"number\":\"" + lexeme + "\"}}";
    }

    /** A case timeline of {@code segments} ({@link #segment}). */
    static String timeline(List<String> segments) {
        return "{\"timeline\":[" + String.join(",", segments) + "]}";
    }

    /** One true period of a characteristic on a timeline, {@code [van, tot)}. */
    static String period(String van, String to) {
        return "{\"from\":\"" + van + "\",\"to\":\"" + to + "\"}";
    }

    /** An instance of the case: attributes as case values in the wire's shape, characteristics that hold one truth
     *  value, the true periods of characteristics on a timeline, and to-many roles by role name. */
    static String instance(String label, String type, Map<String, String> waarden, List<String> kenmerken,
                            Map<String, List<String>> periods, Map<String, List<String>> roles) {
        List<String> a = new ArrayList<>(), k = new ArrayList<>(), p = new ArrayList<>(), r = new ArrayList<>();
        waarden.forEach((naam, waarde) -> a.add("\"" + naam + "\":" + waarde));
        for (String kenmerk : kenmerken) k.add("\"" + kenmerk + "\"");
        periods.forEach((kenmerk, lijst) -> p.add("\"" + kenmerk + "\":[" + String.join(",", lijst) + "]"));
        roles.forEach((rol, targets) -> {
            List<String> d = new ArrayList<>();
            for (String doel : targets) d.add("\"" + doel + "\"");
            r.add("\"" + rol + "\":[" + String.join(",", d) + "]");
        });
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + String.join(",", a)
                + "},\"roles\":{" + String.join(",", r) + "},\"characteristics\":[" + String.join(",", k) + "]"
                + (p.isEmpty() ? "" : ",\"characteristicTimelines\":{" + String.join(",", p) + "}") + "}";
    }

    /** The expectation that attribute {@code attribuut} of {@code label} ends at the timeline of {@code segments}
     *  ({@link #segment}); an empty list expects no segment, which a never-written target meets as well. */
    static String expectedTimeline(String label, String attribuut, List<String> segments) {
        return "{\"kind\":\"attributeTimeline\",\"label\":\"" + label + "\",\"attribute\":\"" + attribuut
                + "\",\"timeline\":[" + String.join(",", segments) + "]}";
    }

    /** The case with the evaluation date {@code asOfDate} (null for a time-free session). */
    static JsonNode caseDocument(String asOfDate, List<String> instanties, List<String> verwachtingen) throws Exception {
        return TmsDocuments.caseOn(asOfDate, instanties, verwachtingen);
    }

    /** The months {@code [van, tot)} from the first of month {@code vanaf} of {@code jaar}, {@code aantal} of them, each
     *  a segment with the number {@code lexeme}. */
    static List<String> months(int year, int from, int aantal, String lexeme) {
        List<String> out = new ArrayList<>();
        LocalDate d = LocalDate.of(year, from, 1);
        for (int i = 0; i < aantal; i++, d = d.plusMonths(1))
            out.add(segment(d.toString(), d.plusMonths(1).toString(), lexeme));
        return out;
    }

    // ---------------- beans with timelines, for a witness that fires a session itself ----------------

    /** A segment of the bean model of {@code beanLoader}, as the case runner lays one. */
    static Object beanSegment(ClassLoader beanLoader, String van, String to, String lexeme) throws Exception {
        return RegelSpraakRuntime.newSegment(Class.forName(FactModelGenerator.PAKKET + ".Segment", true, beanLoader),
                LocalDate.parse(van), LocalDate.parse(to), lexeme == null ? null : new BigDecimal(lexeme));
    }

    /** Puts the list {@code segments} of bean segments on member {@code lid} of {@code beanObject}, as the case runner puts a
     *  case timeline or the true periods of a characteristic there. */
    static void set(Object beanObject, String member, List<Object> segments) throws Exception {
        beanObject.getClass().getMethod(Namen.setter(member), List.class).invoke(beanObject, segments);
    }
}
