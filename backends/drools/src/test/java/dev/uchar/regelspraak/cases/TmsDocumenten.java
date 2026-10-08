package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.kern.Namen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The hand-written documents and cases of the truth-maintenance witnesses: one object type, Jutter, with
 * characteristics only, and characteristic attributions over checks of its own characteristics; and object types with
 * whole-number attributes, fact types of to-one roles, and the reads, tests and computations over them. Every document
 * is written here, on Jutterswet material, in the wire's shape ({@link IrPad} for a rule and a literal).
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

    /** A bean of {@code type} with whole-number attributes and characteristics set as the case input sets them. */
    static Object boon(ClassLoader beanLoader, String type, Map<String, String> getallen, List<String> kenmerken)
            throws Exception {
        Object boon = Class.forName(FactModelGenerator.PAKKET + "." + Namen.ident(type, true), true, beanLoader)
                .getConstructor().newInstance();
        for (Map.Entry<String, String> e : getallen.entrySet())
            boon.getClass().getMethod(Namen.setter(e.getKey()), java.math.BigDecimal.class)
                    .invoke(boon, new java.math.BigDecimal(e.getValue()));
        for (String k : kenmerken) boon.getClass().getMethod(Namen.setter(k), boolean.class).invoke(boon, true);
        return boon;
    }

    /** The value of attribute {@code attribuut} on the bean. */
    static Object lees(Object boon, String attribuut) throws Exception {
        return boon.getClass().getMethod(Namen.getter(attribuut)).invoke(boon);
    }

    /** Whether the bean carries {@code kenmerk}, read as the replay reads it. */
    static boolean heeft(Object boon, String kenmerk) throws Exception {
        return (Boolean) boon.getClass().getMethod("is" + Namen.ident(kenmerk, true)).invoke(boon);
    }

    // ---------------- documents with attributes, fact types and paths ----------------

    static final String GEHEEL = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";

    /** An object type with attributes ({@link #attribuut}) and characteristics. */
    static String objecttype(String naam, List<String> attributen, List<String> kenmerken) {
        List<String> k = new ArrayList<>();
        for (String kenmerk : kenmerken)
            k.add("{\"node\":\"Characteristic\",\"name\":\"" + kenmerk + "\",\"kind\":\"adjectival\"}");
        return "{\"node\":\"ObjectType\",\"name\":\"" + naam + "\",\"animate\":true,\"plural\":\"" + naam + "s\","
                + "\"attributes\":[" + String.join(",", attributen) + "],\"characteristics\":[" + String.join(",", k) + "]}";
    }

    static String attribuut(String naam, String datatype) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + datatype + "}";
    }

    /** A fact type of two to-one roles. */
    static String feittype(String naam, String rolA, String typeA, String rolB, String typeB) {
        return "{\"node\":\"FactType\",\"name\":\"" + naam + "\",\"reciprocal\":false,\"roles\":[{\"node\":\"Role\",\"name\":\""
                + rolA + "\",\"objectType\":\"" + typeA + "\",\"many\":false},{\"node\":\"Role\",\"name\":\"" + rolB
                + "\",\"objectType\":\"" + typeB + "\",\"many\":false}]}";
    }

    static String model(List<String> objecttypen, List<String> feittypen) {
        return "{\"objectTypes\":[" + String.join(",", objecttypen) + "],\"factTypes\":[" + String.join(",", feittypen)
                + "],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";
    }

    /** "zijn {@code attribuut}": a read of the subject's own attribute. */
    static String lezing(String attribuut, String type) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"" + type
                + "\",\"source\":{\"snippet\":\"zijn " + attribuut + "\"}}";
    }

    /** "de {@code attribuut} van zijn {@code rol}": a read one role away. */
    static String padlezing(String attribuut, String type, String feittype, String rol) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"" + type
                + "\",\"source\":{\"snippet\":\"de " + attribuut + " van zijn " + rol + "\"},\"path\":[{\"node\":"
                + "\"RoleBinding\",\"factType\":\"" + feittype + "\",\"role\":\"" + rol + "\"}]}";
    }

    /** "zijn {@code rol} is [niet] {@code kenmerk}": a characteristic check one role away. */
    static String padcheck(String kenmerk, boolean ontkend, String feittype, String rol) {
        return "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"" + kenmerk + "\",\"negated\":" + ontkend
                + ",\"source\":{\"snippet\":\"zijn " + rol + " " + (ontkend ? "niet " : "") + kenmerk + " is\"},"
                + "\"navigation\":{\"node\":\"RoleBinding\",\"factType\":\"" + feittype + "\",\"role\":\"" + rol + "\"}}";
    }

    static String getal(String lexeem) {
        return IrPad.literal(lexeem, GEHEEL, lexeem);
    }

    /** A binary operation in its wire spelling ({@code plus}, {@code times}, {@code dividedBy}, …), of type whole number. */
    static String binair(String op, String links, String rechts) {
        return "{\"node\":\"Binary\",\"op\":\"" + op + "\",\"left\":" + links + ",\"right\":" + rechts + ",\"type\":" + GEHEEL
                + ",\"source\":{\"snippet\":\"" + op + "\"}}";
    }

    /** A comparison in its wire spelling ({@code greater}, {@code notEqual}, …). */
    static String vergelijking(String op, String links, String rechts) {
        return "{\"node\":\"Comparison\",\"op\":\"" + op + "\",\"left\":" + links + ",\"right\":" + rechts
                + ",\"source\":{\"snippet\":\"" + op + "\"}}";
    }

    /** A leeg ({@code empty}) or gevuld ({@code filled}) test. */
    static String predicaat(String kind, String operand) {
        return "{\"node\":\"Predicate\",\"kind\":\"" + kind + "\",\"operand\":" + operand + ",\"negated\":false,\"digits\":0,"
                + "\"source\":{\"snippet\":\"" + kind + "\"}}";
    }

    /** An assignment of attribute {@code doel} of {@code type} ({@code soort} as {@link IrPad#regel} names it). */
    static String rekenregel(String naam, String soort, String type, String doel, String waarde, String voorwaarde) {
        String target = "{\"node\":\"AttributeReference\",\"attribute\":\"" + doel + "\",\"subjectType\":\"" + type + "\","
                + "\"source\":{\"snippet\":\"De " + doel + " van een " + type + "\"}}";
        return IrPad.regel(naam, soort, target, waarde, voorwaarde);
    }

    /** "Een {@code type} is {@code doel} indien {@code voorwaarde}". */
    static String kenmerkregel(String naam, String type, String doel, String voorwaarde) {
        String target = "{\"node\":\"AttributeReference\",\"attribute\":\"" + doel + "\",\"subjectType\":\"" + type + "\","
                + "\"source\":{\"snippet\":\"Een " + type + " is " + doel + "\"}}";
        return IrPad.regel(naam, "KENMERKTOEKENNING", target, WAAR, voorwaarde);
    }

    /** An instance of the case: whole-number attributes by name, characteristics, and to-one roles by role name. */
    static String instantie(String label, String type, Map<String, String> getallen, List<String> kenmerken,
                            Map<String, String> rollen) {
        List<String> a = new ArrayList<>(), k = new ArrayList<>(), r = new ArrayList<>();
        getallen.forEach((naam, lexeem) -> a.add("\"" + naam + "\":{\"number\":\"" + lexeem + "\"}"));
        for (String kenmerk : kenmerken) k.add("\"" + kenmerk + "\"");
        rollen.forEach((rol, doel) -> r.add("\"" + rol + "\":[\"" + doel + "\"]"));
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + String.join(",", a)
                + "},\"roles\":{" + String.join(",", r) + "},\"characteristics\":[" + String.join(",", k) + "]}";
    }

    /** The expectation that attribute {@code attribuut} of {@code label} ends at the number {@code lexeem}, or leeg. */
    static String waarde(String label, String attribuut, String lexeem) {
        return lexeem == null
                ? "{\"kind\":\"attributeEmpty\",\"label\":\"" + label + "\",\"attribute\":\"" + attribuut + "\"}"
                : "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"" + attribuut + "\",\"value\":"
                  + "{\"number\":\"" + lexeem + "\"}}";
    }

    /** The expectation that the session holds no §9.5 Inconsistentie at all. */
    static String consistent() {
        return "{\"kind\":\"consistent\"}";
    }

    /** The expectation of a §9.5 Inconsistentie that rule {@code regel} deposited on {@code label}. */
    static String inconsistent(String regel, String label) {
        return "{\"kind\":\"inconsistent\",\"rule\":\"" + regel + "\",\"label\":\"" + label + "\"}";
    }

    /** A sample of the orders of {@code l} too long to try every order: {@code l} itself, its reverse, and
     *  {@code aantal} shuffles from {@code zaad}, so a failing order can be replayed. */
    static <T> List<List<T>> steekproef(List<T> l, long zaad, int aantal) {
        List<List<T>> uit = new ArrayList<>();
        uit.add(l);
        List<T> omgekeerd = new ArrayList<>(l);
        java.util.Collections.reverse(omgekeerd);
        uit.add(omgekeerd);
        java.util.Random toeval = new java.util.Random(zaad);
        for (int i = 0; i < aantal; i++) {
            List<T> v = new ArrayList<>(l);
            java.util.Collections.shuffle(v, toeval);
            uit.add(v);
        }
        return uit;
    }

    /** A KieBase of the rules {@code b}'s transpiler generated, added in {@code volgorde}, as the branch builds it. */
    static org.kie.api.KieBase kieBase(DroolsBackend.Build b, List<org.drools.model.Rule> volgorde) {
        org.drools.model.impl.ModelImpl m = new org.drools.model.impl.ModelImpl(b.tmsTranspiler().model().getName());
        volgorde.forEach(m::addRule);
        b.tmsTranspiler().model().getGlobals().forEach(m::addGlobal);
        return org.drools.modelcompiler.KieBaseBuilder.createKieBaseFromModel(m,
                org.kie.api.conf.EqualityBehaviorOption.EQUALITY);
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
