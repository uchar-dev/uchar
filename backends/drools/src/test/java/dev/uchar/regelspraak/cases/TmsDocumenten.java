package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.kern.Namen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The hand-written documents and cases of the truth-maintenance witnesses: one object type, Jutter, with
 * characteristics only, and characteristic attributions over checks of its own characteristics. Every document is
 * written here, on Jutterswet material, in the wire's shape ({@link IrPad} for a rule and a literal).
 */
final class TmsDocumenten {
    private TmsDocumenten() {}

    static final ObjectMapper JSON = new ObjectMapper();

    private static final String WAAR = IrPad.literal("waar", "{\"node\":\"Boolean\"}", "waar");

    /** A model with one object type, Jutter, carrying {@code kenmerken} and no attribute. */
    static String model(List<String> kenmerken) {
        List<String> k = new ArrayList<>();
        for (String naam : kenmerken)
            k.add("{\"node\":\"Characteristic\",\"name\":\"" + naam + "\",\"kind\":\"adjectival\"}");
        return "{\"objectTypes\":[{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\","
                + "\"attributes\":[],\"characteristics\":[" + String.join(",", k) + "]}],\"factTypes\":[],"
                + "\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";
    }

    /** "Een Jutter is {@code doel} indien {@code voorwaarde}"; a voorwaarde of "null" writes none. */
    static String kenmerkregel(String naam, String doel, String voorwaarde) {
        String target = "{\"node\":\"AttributeReference\",\"attribute\":\"" + doel + "\",\"subjectType\":\"Jutter\","
                + "\"source\":{\"snippet\":\"Een Jutter is " + doel + "\"}}";
        return IrPad.regel(naam, "KENMERKTOEKENNING", target, WAAR, voorwaarde);
    }

    /** "hij is [niet] {@code kenmerk}": a check of the subject's own characteristic. */
    static String check(String kenmerk, boolean ontkend) {
        return "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"" + kenmerk + "\",\"negated\":" + ontkend
                + ",\"source\":{\"snippet\":\"hij " + (ontkend ? "niet " : "") + kenmerk + " is\"}}";
    }

    /** A composite of {@code delen} under the quantifier in its wire spelling. */
    static String samengesteld(String kwantor, int drempel, List<String> delen) {
        return "{\"node\":\"Compound\",\"quantifier\":\"" + kwantor + "\",\"threshold\":" + drempel + ",\"parts\":["
                + String.join(",", delen) + "],\"source\":{\"snippet\":\"" + kwantor + " " + drempel + "\"}}";
    }

    /** The document of {@code model} with {@code regels}, in that order, written to a temporary file. */
    static Path document(String model, List<String> regels) throws Exception {
        String ir = "{\"format\":\"uchar-ir-v1\",\"model\":" + model + ",\"rules\":{\"rules\":[" + String.join(",", regels)
                + "],\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],"
                + "\"uniquenesses\":[],\"factCreations\":[],\"startPointDeterminations\":[],\"ruleGroups\":[],"
                + "\"prioritizedDefaults\":[]},\"diagnostics\":[]}";
        Path tmp = Files.createTempFile("tms-getuige", ".json");
        tmp.toFile().deleteOnExit();
        return Files.writeString(tmp, ir);
    }

    /** A Jutter of the case, with the characteristics its input sets. */
    static String jutter(String label, List<String> kenmerken) {
        List<String> k = new ArrayList<>();
        for (String naam : kenmerken) k.add("\"" + naam + "\"");
        return "{\"label\":\"" + label + "\",\"objectType\":\"Jutter\",\"attributes\":{},\"roles\":{},\"characteristics\":["
                + String.join(",", k) + "]}";
    }

    /** The expectation that {@code label} carries {@code kenmerk} at the end, or does not. */
    static String verwacht(String label, String kenmerk, boolean aanwezig) {
        return "{\"kind\":\"" + (aanwezig ? "characteristicPresent" : "characteristicAbsent") + "\",\"label\":\"" + label
                + "\",\"characteristic\":\"" + kenmerk + "\"}";
    }

    static JsonNode zaak(List<String> instanties, List<String> verwachtingen) throws Exception {
        return JSON.readTree("{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":["
                + String.join(",", instanties) + "],\"parameters\":{}},\"expected\":[" + String.join(",", verwachtingen)
                + "]}");
    }

    /** A Jutter bean of {@code beanLoader} with the characteristics of {@code kenmerken} set, as the case input sets
     *  them, for a witness that fires a session itself. */
    static Object jutterBoon(ClassLoader beanLoader, List<String> kenmerken) throws Exception {
        Object boon = Class.forName(FactModelGenerator.PAKKET + ".Jutter", true, beanLoader).getConstructor().newInstance();
        for (String k : kenmerken) boon.getClass().getMethod(Namen.setter(k), boolean.class).invoke(boon, true);
        return boon;
    }

    /** Whether the bean carries {@code kenmerk}, read as the replay reads it. */
    static boolean heeft(Object boon, String kenmerk) throws Exception {
        return (Boolean) boon.getClass().getMethod("is" + Namen.ident(kenmerk, true)).invoke(boon);
    }

    /** Every order of {@code l}. */
    static <T> List<List<T>> permutaties(List<T> l) {
        if (l.size() <= 1) return List.of(l);
        List<List<T>> uit = new ArrayList<>();
        for (int i = 0; i < l.size(); i++) {
            List<T> rest = new ArrayList<>(l);
            T eerste = rest.remove(i);
            for (List<T> p : permutaties(rest)) {
                List<T> q = new ArrayList<>();
                q.add(eerste);
                q.addAll(p);
                uit.add(q);
            }
        }
        return uit;
    }
}
