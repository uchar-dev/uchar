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
 * ({@link IrPad} for a rule). Next to {@link TmsDocumenten}, whose builders hold one value per member.
 */
final class TmsTijdlijnDocumenten {
    private TmsTijdlijnDocumenten() {}

    static final String GEHEEL = TmsDocumenten.GEHEEL;

    /** A timeline facet of {@code granulariteit} in its wire spelling ({@code month}, {@code year}, …), with the fixed
     *  start point {@code vast} (null on the calendar grid). */
    static String facet(String granulariteit, String vast) {
        return "{\"node\":\"TimelineFacet\",\"granularity\":\"" + granulariteit + "\",\"count\":1"
                + (vast == null ? "" : ",\"fixedStart\":\"" + vast + "\"") + "}";
    }

    /** An attribute of {@code datatype} on a timeline of {@code granulariteit}. */
    static String tijdattribuut(String naam, String datatype, String granulariteit) {
        return tijdattribuut(naam, datatype, granulariteit, null);
    }

    /** As {@link #tijdattribuut(String, String, String)}, on the grid of the fixed start point {@code vast}. */
    static String tijdattribuut(String naam, String datatype, String granulariteit, String vast) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + datatype + ",\"timeline\":"
                + facet(granulariteit, vast) + "}";
    }

    /** A characteristic that holds one truth value. */
    static String kenmerk(String naam) {
        return "{\"node\":\"Characteristic\",\"name\":\"" + naam + "\",\"kind\":\"adjectival\"}";
    }

    /** A characteristic on a timeline of {@code granulariteit}: its value is its true periods. */
    static String tijdkenmerk(String naam, String granulariteit) {
        return "{\"node\":\"Characteristic\",\"name\":\"" + naam + "\",\"kind\":\"adjectival\",\"timeline\":"
                + facet(granulariteit, null) + "}";
    }

    /** An object type with its attributes and characteristics as written ({@link #tijdattribuut}, {@link #kenmerk}). */
    static String objecttype(String naam, List<String> attributen, List<String> kenmerken) {
        return "{\"node\":\"ObjectType\",\"name\":\"" + naam + "\",\"animate\":true,\"plural\":\"" + naam + "s\","
                + "\"attributes\":[" + String.join(",", attributen) + "],\"characteristics\":[" + String.join(",", kenmerken)
                + "]}";
    }

    /** "het tijdsevenredig deel per {@code granulariteit} van zijn {@code basis} gedurende de tijd dat {@code filter}"
     *  (RUNTIME-SEMANTICS ch. 3.7). */
    static String tijdsevenredig(String basis, String type, String filter, String granulariteit) {
        return "{\"node\":\"BuiltinCall\",\"kind\":\"timeProportionalPart\",\"arguments\":[" + TmsDocumenten.lezing(basis, type)
                + "],\"filter\":" + filter + ",\"granularity\":\"" + granulariteit + "\",\"type\":" + GEHEEL
                + ",\"source\":{\"snippet\":\"het tijdsevenredig deel per " + granulariteit + " van zijn " + basis + "\"}}";
    }

    /** "het aantal dagen in {@code granulariteit} dat {@code filter}" (RUNTIME-SEMANTICS ch. 3.8). */
    static String dagentelling(String filter, String granulariteit) {
        return "{\"node\":\"BuiltinCall\",\"kind\":\"dayCount\",\"arguments\":[],\"filter\":" + filter + ",\"granularity\":\""
                + granulariteit + "\",\"type\":" + GEHEEL + ",\"source\":{\"snippet\":\"het aantal dagen in de "
                + granulariteit + "\"}}";
    }

    /** "het totaal van zijn {@code attribuut}", optionally "gedurende" {@code filter} ("null" for none): the total of the
     *  subject's own timeline (RUNTIME-SEMANTICS ch. 3.6), a fold without a range. */
    static String eigenTotaal(String attribuut, String type, String filter) {
        return "{\"node\":\"Aggregation\",\"kind\":\"total\",\"arguments\":[" + TmsDocumenten.lezing(attribuut, type)
                + "],\"elementType\":\"" + type + "\"" + (filter.equals("null") ? "" : ",\"filter\":" + filter)
                + ",\"type\":" + GEHEEL + ",\"source\":{\"snippet\":\"het totaal van zijn " + attribuut + "\"}}";
    }

    /** The document of {@code model} with {@code regels} and {@code uniciteiten}, each in its order. */
    static Path document(String model, List<String> regels, List<String> uniciteiten) throws Exception {
        Path tmp = Files.createTempFile("tms-tijdlijn", ".json");
        tmp.toFile().deleteOnExit();
        return Files.writeString(tmp, ir(model, regels, uniciteiten));
    }

    /** The text of the document of {@link #document}. */
    static String ir(String model, List<String> regels, List<String> uniciteiten) {
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + model + ",\"rules\":{\"rules\":[" + String.join(",", regels)
                + "],\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],"
                + "\"uniquenesses\":[" + String.join(",", uniciteiten) + "],\"factCreations\":[],"
                + "\"startPointDeterminations\":[],\"ruleGroups\":[],\"prioritizedDefaults\":[]},\"diagnostics\":[]}";
    }

    /** One segment of a case timeline, {@code [van, tot)} with the number {@code lexeem}. */
    static String segment(String van, String tot, String lexeem) {
        return "{\"from\":\"" + van + "\",\"to\":\"" + tot + "\",\"value\":{\"number\":\"" + lexeem + "\"}}";
    }

    /** A case timeline of {@code segmenten} ({@link #segment}). */
    static String tijdlijn(List<String> segmenten) {
        return "{\"timeline\":[" + String.join(",", segmenten) + "]}";
    }

    /** One true period of a characteristic on a timeline, {@code [van, tot)}. */
    static String periode(String van, String tot) {
        return "{\"from\":\"" + van + "\",\"to\":\"" + tot + "\"}";
    }

    /** An instance of the case: attributes as case values in the wire's shape, characteristics that hold one truth
     *  value, the true periods of characteristics on a timeline, and to-many roles by role name. */
    static String instantie(String label, String type, Map<String, String> waarden, List<String> kenmerken,
                            Map<String, List<String>> perioden, Map<String, List<String>> rollen) {
        List<String> a = new ArrayList<>(), k = new ArrayList<>(), p = new ArrayList<>(), r = new ArrayList<>();
        waarden.forEach((naam, waarde) -> a.add("\"" + naam + "\":" + waarde));
        for (String kenmerk : kenmerken) k.add("\"" + kenmerk + "\"");
        perioden.forEach((kenmerk, lijst) -> p.add("\"" + kenmerk + "\":[" + String.join(",", lijst) + "]"));
        rollen.forEach((rol, doelen) -> {
            List<String> d = new ArrayList<>();
            for (String doel : doelen) d.add("\"" + doel + "\"");
            r.add("\"" + rol + "\":[" + String.join(",", d) + "]");
        });
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + String.join(",", a)
                + "},\"roles\":{" + String.join(",", r) + "},\"characteristics\":[" + String.join(",", k) + "]"
                + (p.isEmpty() ? "" : ",\"characteristicTimelines\":{" + String.join(",", p) + "}") + "}";
    }

    /** The expectation that attribute {@code attribuut} of {@code label} ends at the timeline of {@code segmenten}
     *  ({@link #segment}); an empty list expects no segment, which a never-written target meets as well. */
    static String verwachteTijdlijn(String label, String attribuut, List<String> segmenten) {
        return "{\"kind\":\"attributeTimeline\",\"label\":\"" + label + "\",\"attribute\":\"" + attribuut
                + "\",\"timeline\":[" + String.join(",", segmenten) + "]}";
    }

    /** The case with the evaluation date {@code asOfDate} (null for a time-free session). */
    static JsonNode zaak(String asOfDate, List<String> instanties, List<String> verwachtingen) throws Exception {
        return TmsDocumenten.zaakOp(asOfDate, instanties, verwachtingen);
    }

    /** The months {@code [van, tot)} from the first of month {@code vanaf} of {@code jaar}, {@code aantal} of them, each
     *  a segment with the number {@code lexeem}. */
    static List<String> maanden(int jaar, int vanaf, int aantal, String lexeem) {
        List<String> uit = new ArrayList<>();
        LocalDate d = LocalDate.of(jaar, vanaf, 1);
        for (int i = 0; i < aantal; i++, d = d.plusMonths(1))
            uit.add(segment(d.toString(), d.plusMonths(1).toString(), lexeem));
        return uit;
    }

    // ---------------- beans with timelines, for a witness that fires a session itself ----------------

    /** A segment of the bean model of {@code beanLoader}, as the case runner lays one. */
    static Object boonsegment(ClassLoader beanLoader, String van, String tot, String lexeem) throws Exception {
        return RegelSpraakRuntime.newSegment(Class.forName(FactModelGenerator.PAKKET + ".Segment", true, beanLoader),
                LocalDate.parse(van), LocalDate.parse(tot), lexeem == null ? null : new BigDecimal(lexeem));
    }

    /** Puts the list {@code segmenten} of bean segments on member {@code lid} of {@code boon}, as the case runner puts a
     *  case timeline or the true periods of a characteristic there. */
    static void zet(Object boon, String lid, List<Object> segmenten) throws Exception {
        boon.getClass().getMethod(Namen.setter(lid), List.class).invoke(boon, segmenten);
    }
}
