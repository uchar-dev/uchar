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
        return document(model, regels, List.of());
    }

    /** The document of {@code model} with {@code regels} and {@code consistenties}, each in its order. */
    static Path document(String model, List<String> regels, List<String> consistenties) throws Exception {
        return document(model, regels, consistenties, List.of());
    }

    /** The document of {@code model} with {@code regels}, {@code consistenties} and the prioritized-defaults groups
     *  {@code defaults}, each in its order. */
    static Path document(String model, List<String> regels, List<String> consistenties, List<String> defaults)
            throws Exception {
        String ir = "{\"format\":\"uchar-ir-v1\",\"model\":" + model + ",\"rules\":{\"rules\":[" + String.join(",", regels)
                + "],\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":["
                + String.join(",", consistenties) + "],"
                + "\"uniquenesses\":[],\"factCreations\":[],\"startPointDeterminations\":[],\"ruleGroups\":[],"
                + "\"prioritizedDefaults\":[" + String.join(",", defaults) + "]},\"diagnostics\":[]}";
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
        return zaak(instanties, Map.of(), verwachtingen);
    }

    /** A case with whole-number {@code parameters} by name. */
    static JsonNode zaak(List<String> instanties, Map<String, String> parameters, List<String> verwachtingen)
            throws Exception {
        List<String> p = new ArrayList<>();
        parameters.forEach((naam, lexeem) -> p.add("\"" + naam + "\":{\"number\":\"" + lexeem + "\"}"));
        return JSON.readTree("{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":["
                + String.join(",", instanties) + "],\"parameters\":{" + String.join(",", p) + "}},\"expected\":["
                + String.join(",", verwachtingen) + "]}");
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

    /** A fact type of a to-one role {@code rolEen} played by {@code typeEen} and a to-many role {@code rolVeel}, plural
     *  {@code meervoud}, played by {@code typeVeel}: each {@code typeVeel} relates to at most one {@code typeEen}. */
    static String feittypeEenNaarVeel(String naam, String rolEen, String typeEen, String rolVeel, String typeVeel,
                                      String meervoud) {
        return "{\"node\":\"FactType\",\"name\":\"" + naam + "\",\"reciprocal\":false,\"roles\":[{\"node\":\"Role\",\"name\":\""
                + rolEen + "\",\"objectType\":\"" + typeEen + "\",\"many\":false},{\"node\":\"Role\",\"name\":\"" + rolVeel
                + "\",\"objectType\":\"" + typeVeel + "\",\"plural\":\"" + meervoud + "\",\"many\":true}]}";
    }

    /** A RoleBinding: the role {@code rol} of fact type {@code feittype}. */
    static String rolbinding(String feittype, String rol) {
        return "{\"node\":\"RoleBinding\",\"factType\":\"" + feittype + "\",\"role\":\"" + rol + "\"}";
    }

    static final String TELLING = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"nonNegative\"}";

    /** An Aggregation over a set source: {@code kind} in its wire spelling ({@code count}, {@code sum}, …), its one
     *  {@code argument}, the {@code elementType}, the range as {@code "sourceRole":…} or {@code "sourcePath":[…]}, a
     *  {@code filter} and an {@code emptyFallback} (each "null" for none), and the result {@code type}. */
    static String aggregatie(String kind, String argument, String elementType, String bereik, String filter,
                             String terugval, String type, String bron) {
        return "{\"node\":\"Aggregation\",\"kind\":\"" + kind + "\",\"arguments\":[" + argument + "],\"elementType\":\""
                + elementType + "\"" + (filter.equals("null") ? "" : ",\"filter\":" + filter)
                + (terugval.equals("null") ? "" : ",\"emptyFallback\":" + terugval) + "," + bereik + ",\"type\":" + type
                + ",\"source\":{\"snippet\":\"" + bron + "\"}}";
    }

    /** The range of a fold over one relation step. */
    static String bronrol(String feittype, String rol) {
        return "\"sourceRole\":" + rolbinding(feittype, rol);
    }

    /** The range of a fold over two or more relation steps, each a RoleBinding. */
    static String bronpad(List<String> stappen) {
        return "\"sourcePath\":[" + String.join(",", stappen) + "]";
    }

    /** A listed fold, "de eerste/laatste/minimale/maximale van A en B …", over the {@code operanden}. */
    static String gelijsteVouw(String kind, List<String> operanden, String type, String bron) {
        return "{\"node\":\"Aggregation\",\"kind\":\"" + kind + "\",\"arguments\":[" + String.join(",", operanden)
                + "],\"type\":" + type + ",\"source\":{\"snippet\":\"" + bron + "\"}}";
    }

    /** An instance of the case whose roles may link several instances each, by role name. */
    static String instantieMetRollen(String label, String type, Map<String, String> getallen, List<String> kenmerken,
                                     Map<String, List<String>> rollen) {
        List<String> a = new ArrayList<>(), k = new ArrayList<>(), r = new ArrayList<>();
        getallen.forEach((naam, lexeem) -> a.add("\"" + naam + "\":{\"number\":\"" + lexeem + "\"}"));
        for (String kenmerk : kenmerken) k.add("\"" + kenmerk + "\"");
        rollen.forEach((rol, doelen) -> {
            List<String> d = new ArrayList<>();
            for (String doel : doelen) d.add("\"" + doel + "\"");
            r.add("\"" + rol + "\":[" + String.join(",", d) + "]");
        });
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + String.join(",", a)
                + "},\"roles\":{" + String.join(",", r) + "},\"characteristics\":[" + String.join(",", k) + "]}";
    }

    static String model(List<String> objecttypen, List<String> feittypen) {
        return model(objecttypen, feittypen, List.of());
    }

    /** A model with {@code parameters}, each a whole-number Parameter named by its entry. */
    static String model(List<String> objecttypen, List<String> feittypen, List<String> parameters) {
        List<String> p = new ArrayList<>();
        for (String naam : parameters)
            p.add("{\"node\":\"Parameter\",\"name\":\"" + naam + "\",\"datatype\":" + GEHEEL + "}");
        return "{\"objectTypes\":[" + String.join(",", objecttypen) + "],\"factTypes\":[" + String.join(",", feittypen)
                + "],\"parameters\":[" + String.join(",", p) + "],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],"
                + "\"timelines\":[],\"dayKinds\":[]}";
    }

    /** "het {@code parameter}": a read of a parameter. */
    static String parameterlezing(String parameter) {
        return "{\"node\":\"ParameterReference\",\"parameter\":\"" + parameter + "\",\"source\":{\"snippet\":\"het "
                + parameter + "\"}}";
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

    /** A Consistency of one always-valid version: {@code assertie} must hold for every {@code subject}. */
    static String consistentie(String naam, String subject, String assertie) {
        return "{\"node\":\"Consistency\",\"name\":\"" + naam + "\",\"versions\":[{\"subject\":\"" + subject
                + "\",\"assertion\":" + assertie + ",\"validity\":{\"node\":\"Validity\"},\"source\":{\"snippet\":\""
                + naam + "\"}}]}";
    }

    /** An object type that is a {@code supertype} (IR-SPEC ch. 2.2), with attributes and characteristics of its own. */
    static String subtype(String naam, String supertype, List<String> attributen, List<String> kenmerken) {
        String t = objecttype(naam, attributen, kenmerken);
        return t.replace("\"name\":\"" + naam + "\",", "\"name\":\"" + naam + "\",\"supertype\":\"" + supertype + "\",");
    }

    // ---------------- prioritized defaults ----------------

    /** A defaults group on attribute {@code attribuut} of {@code type}, its {@code defaults} highest first. */
    static String attribuutgroep(String naam, String type, String attribuut, List<String> defaults) {
        return "{\"node\":\"PrioritizedDefaults\",\"name\":\"" + naam + "\",\"subject\":\"" + type + "\",\"attribute\":\""
                + attribuut + "\",\"defaults\":[" + String.join(",", defaults) + "],\"validity\":{\"node\":\"Validity\"}}";
    }

    /** A defaults group on characteristic {@code kenmerk} of {@code type}, its {@code defaults} highest first. */
    static String kenmerkgroep(String naam, String type, String kenmerk, List<String> defaults) {
        return "{\"node\":\"PrioritizedDefaults\",\"name\":\"" + naam + "\",\"subject\":\"" + type
                + "\",\"characteristic\":\"" + kenmerk + "\",\"defaults\":[" + String.join(",", defaults)
                + "],\"validity\":{\"node\":\"Validity\"}}";
    }

    /** An attribute default: its justification and its consequence. */
    static String standaard(String rechtvaardiging, String consequentie) {
        return "{\"node\":\"Default\",\"justification\":" + rechtvaardiging + ",\"consequence\":" + consequentie + "}";
    }

    /** A characteristic default: its justification and its decision, attribute or do-not-attribute. */
    static String besluit(String rechtvaardiging, boolean besluit) {
        return "{\"node\":\"Default\",\"decision\":" + besluit + ",\"justification\":" + rechtvaardiging + "}";
    }

    /** A form-a default with exception groups: its own justification and consequence, and the members, each a chain of
     *  defaults highest first, reconciled as a declared-unordered group (RUNTIME-SEMANTICS ch. 7.9). */
    static String metUitzonderingen(String rechtvaardiging, String consequentie, List<List<String>> leden) {
        return "{\"node\":\"Default\",\"justification\":" + rechtvaardiging + ",\"consequence\":" + consequentie
                + ",\"exceptions\":" + uitzonderingen(leden) + "}";
    }

    /** A group-default: exception groups and no content of its own. */
    static String groepsdefault(List<List<String>> leden) {
        return "{\"node\":\"Default\",\"exceptions\":" + uitzonderingen(leden) + "}";
    }

    private static String uitzonderingen(List<List<String>> leden) {
        List<String> m = new ArrayList<>();
        for (List<String> lid : leden)
            m.add("{\"node\":\"ExceptionGroup\",\"defaults\":[" + String.join(",", lid) + "]}");
        return "{\"unordered\":true,\"members\":[" + String.join(",", m) + "]}";
    }

    /** "regelversie {@code regel} is inconsistent": a test of the §9.5 channel of the named form. */
    static String kanaaltoets(String regel) {
        return "{\"node\":\"Regelversie\",\"regel\":\"" + regel + "\",\"inconsistent\":true,\"source\":{\"snippet\":"
                + "\"regelversie " + regel + " is inconsistent\"}}";
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
        org.drools.model.impl.ModelImpl m = new org.drools.model.impl.ModelImpl(b.execModelTranspiler().model().getName());
        volgorde.forEach(m::addRule);
        b.execModelTranspiler().model().getGlobals().forEach(m::addGlobal);
        return org.drools.modelcompiler.KieBaseBuilder.createKieBaseFromModel(m,
                org.kie.api.conf.EqualityBehaviorOption.EQUALITY);
    }

    /** Fires {@code bonen} through the rules of {@code b} added in {@code volgorde}: {@code voor} sees the session before
     *  the fire (to add a listener), the fire must reach its fixpoint below the cap, the derivations are written back,
     *  and {@code na} sees the session before it is disposed. */
    static void vuur(DroolsBackend.Build b, List<org.drools.model.Rule> volgorde, List<Object> bonen,
                     java.util.function.Consumer<org.kie.api.runtime.KieSession> voor,
                     java.util.function.Consumer<org.kie.api.runtime.KieSession> na) {
        vuur(b, kieBase(b, volgorde), volgorde.stream().map(org.drools.model.Rule::getName).toList(), bonen, voor, na);
    }

    /** As {@link #vuur(DroolsBackend.Build, List, List, java.util.function.Consumer, java.util.function.Consumer)}
     *  through {@code kieBase}, which holds the rules of {@code b} in the order {@code namen}: those of the executable
     *  model ({@link #kieBase}) or those of the text ({@code Tekstvolgorde}). */
    static void vuur(DroolsBackend.Build b, org.kie.api.KieBase kieBase, List<String> namen, List<Object> bonen,
                     java.util.function.Consumer<org.kie.api.runtime.KieSession> voor,
                     java.util.function.Consumer<org.kie.api.runtime.KieSession> na) {
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        org.kie.api.runtime.KieSession sessie = kieBase.newKieSession();
        try {
            voor.accept(sessie);
            for (Object boon : bonen) sessie.insert(boon);
            if (DroolsBackend.vuur(b, sessie, 100_000).geenFixpunt() != null)
                throw new AssertionError(b.branch() + ": geen fixpunt in volgorde " + namen);
            DroolsBackend.zetAfgeleideWaarden(b, sessie);
            na.accept(sessie);
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    /** The §9.5 facts of {@code sessie} as "rule @ label", sorted, with {@code labels} naming the beans. */
    static List<String> inconsistenties(org.kie.api.runtime.KieSession sessie, Map<Object, String> labels) {
        List<String> uit = new ArrayList<>();
        for (Object f : sessie.getObjects(o -> o.getClass().getSimpleName().equals("Inconsistentie"))) {
            try {
                Object regel = f.getClass().getMethod("getRegel").invoke(f);
                Object subject = f.getClass().getMethod("getSubject").invoke(f);
                uit.add(regel + " @ " + labels.getOrDefault(subject, String.valueOf(subject)));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
        uit.sort(null);
        return uit;
    }

    /** The names of the rules of a document, in their order, for a message that names the order that failed. */
    static List<String> regelnamen(List<String> regels) throws Exception {
        List<String> uit = new ArrayList<>();
        for (String r : regels) uit.add(JSON.readTree(r).path("name").asText());
        return uit;
    }

    // ---------------- documents with dates, texts, units, the value forms and an evaluation date ----------------

    static final String DATUM = "{\"node\":\"Date\",\"precision\":\"day\"}";
    static final String DATUMTIJD = "{\"node\":\"Date\",\"precision\":\"millisecond\"}";
    static final String TEKST = "{\"node\":\"Text\"}";

    /** A number type, whole or with {@code decimalen} decimals, with the unit {@code eenheid} (a Unit node, or null). */
    static String getaltype(boolean geheel, int decimalen, String eenheid) {
        return "{\"node\":\"Number\",\"whole\":" + geheel + ",\"decimals\":" + decimalen + ",\"sign\":\"unspecified\""
                + (eenheid == null ? "" : ",\"unit\":" + eenheid) + "}";
    }

    /** A Unit of system {@code stelsel}, {@code num}/{@code den} of the canonical unit {@code canoniek}. */
    static String eenheid(String afkorting, String stelsel, String canoniek, int num, int den) {
        return "{\"node\":\"Unit\",\"abbreviation\":\"" + afkorting + "\"" + (stelsel == null ? "" : ",\"system\":\""
                + stelsel + "\"") + ",\"canonical\":\"" + canoniek + "\",\"factorToCanonical\":{\"num\":" + num
                + ",\"den\":" + den + "}}";
    }

    /** A Unit written by its abbreviation alone, as a calendar unit is (RUNTIME-SEMANTICS ch. 3.8). */
    static String eenheid(String afkorting) {
        return "{\"node\":\"Unit\",\"abbreviation\":\"" + afkorting + "\"}";
    }

    /** The unit of a percentage, which the loader reads as the Percentage datatype. */
    static final String PROCENT = eenheid("%", null, "%", 1, 1);

    /** A unit system of {@code eenheden}, Unit nodes of that system. */
    static String eenheidstelsel(String naam, List<String> eenheden) {
        return "{\"node\":\"UnitSystem\",\"name\":\"" + naam + "\",\"units\":[" + String.join(",", eenheden) + "]}";
    }

    /** A model with object types, fact types, parameters as written ({@link #parameter}) and unit systems. */
    static String modelMet(List<String> objecttypen, List<String> feittypen, List<String> parameters,
                           List<String> stelsels) {
        return "{\"objectTypes\":[" + String.join(",", objecttypen) + "],\"factTypes\":[" + String.join(",", feittypen)
                + "],\"parameters\":[" + String.join(",", parameters) + "],\"domains\":[],\"unitSystems\":["
                + String.join(",", stelsels) + "],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";
    }

    /** A Parameter of datatype {@code type}. */
    static String parameter(String naam, String type) {
        return "{\"node\":\"Parameter\",\"name\":\"" + naam + "\",\"datatype\":" + type + "}";
    }

    /** A Literal of {@code type}, its snippet the lexeme. */
    static String literaal(String lexeem, String type) {
        return IrPad.literal(lexeem, type, lexeem);
    }

    /** A binary operation in its wire spelling with the result {@code type}. */
    static String binair(String op, String links, String rechts, String type) {
        return "{\"node\":\"Binary\",\"op\":\"" + op + "\",\"left\":" + links + ",\"right\":" + rechts + ",\"type\":" + type
                + ",\"source\":{\"snippet\":\"" + op + "\"}}";
    }

    /** "{@code basis} afgerond op {@code decimalen} decimalen" in the mode's wire spelling. */
    static String afronding(String basis, String modus, int decimalen, String type) {
        return "{\"node\":\"Rounding\",\"base\":" + basis + ",\"mode\":\"" + modus + "\",\"decimals\":" + decimalen
                + ",\"type\":" + type + ",\"source\":{\"snippet\":\"afgerond op " + decimalen + " decimalen\"}}";
    }

    /** "{@code basis} begrensd" with a minimum and a maximum, each "null" for none. */
    static String begrenzing(String basis, String minimum, String maximum, String type) {
        return "{\"node\":\"Clamp\",\"base\":" + basis + (minimum.equals("null") ? "" : ",\"minimum\":" + minimum)
                + (maximum.equals("null") ? "" : ",\"maximum\":" + maximum) + ",\"type\":" + type
                + ",\"source\":{\"snippet\":\"begrensd\"}}";
    }

    /** A BuiltinCall of {@code kind} in its wire spelling over {@code argumenten}, with the result {@code type}. */
    static String builtin(String kind, List<String> argumenten, String type, String bron) {
        return "{\"node\":\"BuiltinCall\",\"kind\":\"" + kind + "\",\"arguments\":[" + String.join(",", argumenten)
                + "],\"type\":" + type + ",\"source\":{\"snippet\":\"" + bron + "\"}}";
    }

    /** "Daarbij geldt: {@code binder} is {@code waarde}" around {@code lichaam}: a Let of the body's {@code type}. */
    static String let(String binder, String waarde, String lichaam, String type) {
        return "{\"node\":\"Let\",\"type\":" + type + ",\"binder\":\"" + binder + "\",\"value\":" + waarde + ",\"body\":"
                + lichaam + ",\"source\":{\"snippet\":\"" + binder + " is …\"}}";
    }

    /** A read of the binder {@code binder}. */
    static String binderlezing(String binder) {
        return "{\"node\":\"BinderRef\",\"name\":\"" + binder + "\",\"source\":{\"snippet\":\"" + binder + "\"}}";
    }

    /** "als {@code voorwaarde} dan {@code dan} anders {@code anders}" of {@code type}. */
    static String keuze(String voorwaarde, String dan, String anders, String type) {
        return "{\"node\":\"IfThenElse\",\"type\":" + type + ",\"condition\":" + voorwaarde + ",\"then\":" + dan
                + ",\"else\":" + anders + ",\"source\":{\"snippet\":\"als … dan … anders …\"}}";
    }

    /** The context value {@code naam} (evaluationDate, evaluationYear) of {@code type}. */
    static String contextwaarde(String naam, String type) {
        return "{\"node\":\"ContextValue\",\"name\":\"" + naam + "\",\"type\":" + type + ",\"source\":{\"snippet\":\"het "
                + naam + "\"}}";
    }

    /** A TimeWindow from {@code van} to {@code tot}, each "null" for an open bound. */
    static String venster(String van, String tot, boolean totInclusief) {
        return "{\"node\":\"TimeWindow\"" + (van.equals("null") ? "" : ",\"from\":" + van)
                + (tot.equals("null") ? "" : ",\"to\":" + tot) + ",\"toInclusive\":" + totInclusief
                + ",\"source\":{\"snippet\":\"van … tot …\"}}";
    }

    /** A TextComposition of {@code delen}, each a {@link #tekstdeel} or an {@link #tekstexpressie}. */
    static String tekstreeks(List<String> delen) {
        return "{\"node\":\"TextComposition\",\"parts\":[" + String.join(",", delen) + "],\"type\":" + TEKST
                + ",\"source\":{\"snippet\":\"de reeks van …\"}}";
    }

    static String tekstdeel(String tekst) {
        return "{\"node\":\"LiteralChunk\",\"text\":\"" + tekst + "\"}";
    }

    static String tekstexpressie(String expr) {
        return "{\"node\":\"EmbeddedExpression\",\"expr\":" + expr + "}";
    }

    /** A Validity from {@code van} to {@code tot}, both inclusive, each null for an open bound. */
    static String geldigheid(String van, String tot) {
        return "{\"node\":\"Validity\"" + (van == null ? "" : ",\"from\":\"" + van + "\"")
                + (tot == null ? "" : ",\"to\":\"" + tot + "\"") + "}";
    }

    /** The case with the evaluation date {@code asOfDate} (null for a time-free session). */
    static JsonNode zaakOp(String asOfDate, List<String> instanties, List<String> verwachtingen) throws Exception {
        return JSON.readTree("{\"format\":\"uchar-case-v0\",\"asOfDate\":"
                + (asOfDate == null ? "null" : "\"" + asOfDate + "\"") + ",\"input\":{\"instances\":["
                + String.join(",", instanties) + "],\"parameters\":{}},\"expected\":[" + String.join(",", verwachtingen)
                + "]}");
    }

    /** An instance whose attributes are given as case values in the wire's shape ({@code {"number":"5"}},
     *  {@code {"date":"2026-01-01"}}, {@code {"text":"…"}}), by attribute. */
    static String instantieMet(String label, String type, Map<String, String> waarden, List<String> kenmerken) {
        List<String> a = new ArrayList<>(), k = new ArrayList<>();
        waarden.forEach((naam, waarde) -> a.add("\"" + naam + "\":" + waarde));
        for (String kenmerk : kenmerken) k.add("\"" + kenmerk + "\"");
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + String.join(",", a)
                + "},\"roles\":{},\"characteristics\":[" + String.join(",", k) + "]}";
    }

    /** The expectation that attribute {@code attribuut} of {@code label} ends at a case value in the wire's shape. */
    static String waardeJson(String label, String attribuut, String waarde) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"" + attribuut + "\",\"value\":"
                + waarde + "}";
    }

    static String getalwaarde(String lexeem) {
        return "{\"number\":\"" + lexeem + "\"}";
    }

    static String datumwaarde(String iso) {
        return "{\"date\":\"" + iso + "\"}";
    }

    static String tekstwaarde(String tekst) {
        return "{\"text\":\"" + tekst + "\"}";
    }

    /** Fires {@code bonen} through the rules of {@code b} added in {@code volgorde}, with {@code voor} seeing the session
     *  before the fire (a listener, a global). The fire must reach its fixpoint below the cap without throwing: an
     *  evaluation fault is a fact of the session, not an exception out of the fire. Returns the fault the write-back
     *  raises at the fixpoint as "SimpleName: message", or null where it writes the derivations back. */
    static String fixpunt(DroolsBackend.Build b, List<org.drools.model.Rule> volgorde, List<Object> bonen,
                          java.util.function.Consumer<org.kie.api.runtime.KieSession> voor) {
        List<String> namen = volgorde.stream().map(org.drools.model.Rule::getName).toList();
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        org.kie.api.runtime.KieSession sessie = kieBase(b, volgorde).newKieSession();
        try {
            voor.accept(sessie);
            for (Object boon : bonen) sessie.insert(boon);
            DroolsBackend.Vuring vuring;
            try {
                vuring = DroolsBackend.vuur(b, sessie, 100_000);
            } catch (RuntimeException e) {
                throw new AssertionError("de fire wierp in volgorde " + namen + ": " + e, e);
            }
            if (vuring.geenFixpunt() != null) throw new AssertionError("geen fixpunt in volgorde " + namen);
            try {
                DroolsBackend.zetAfgeleideWaarden(b, sessie);
                return null;
            } catch (RuntimeException e) {
                return e.getClass().getSimpleName() + ": " + e.getMessage();
            }
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(vorige);
        }
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
