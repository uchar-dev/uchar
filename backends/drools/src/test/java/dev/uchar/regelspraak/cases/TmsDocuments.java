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
 * is written here, on Jutterswet material, in the wire's shape ({@link IrPath} for a rule and a literal).
 */
final class TmsDocuments {
    private TmsDocuments() {}

    static final ObjectMapper JSON = new ObjectMapper();

    private static final String WAAR = IrPath.literal("waar", "{\"node\":\"Boolean\"}", "waar");

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
    static String characteristicRule(String naam, String doel, String voorwaarde) {
        String target = "{\"node\":\"AttributeReference\",\"attribute\":\"" + doel + "\",\"subjectType\":\"Jutter\","
                + "\"source\":{\"snippet\":\"Een Jutter is " + doel + "\"}}";
        return IrPath.regel(naam, "KENMERKTOEKENNING", target, WAAR, voorwaarde);
    }

    /** "hij is [niet] {@code kenmerk}": a check of the subject's own characteristic. */
    static String check(String kenmerk, boolean negated) {
        return "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"" + kenmerk + "\",\"negated\":" + negated
                + ",\"source\":{\"snippet\":\"hij " + (negated ? "niet " : "") + kenmerk + " is\"}}";
    }

    /** A composite of {@code parts} under the quantifier in its wire spelling. */
    static String compound(String quantifier, int threshold, List<String> parts) {
        return "{\"node\":\"Compound\",\"quantifier\":\"" + quantifier + "\",\"threshold\":" + threshold + ",\"parts\":["
                + String.join(",", parts) + "],\"source\":{\"snippet\":\"" + quantifier + " " + threshold + "\"}}";
    }

    /** The document of {@code model} with {@code rules}, in that order, written to a temporary file. */
    static Path document(String model, List<String> rules) throws Exception {
        return document(model, rules, List.of());
    }

    /** The document of {@code model} with {@code rules} and {@code consistencies}, each in its order. */
    static Path document(String model, List<String> rules, List<String> consistencies) throws Exception {
        return document(model, rules, consistencies, List.of());
    }

    /** The document of {@code model} with {@code rules}, {@code consistencies} and the prioritized-defaults groups
     *  {@code defaults}, each in its order. */
    static Path document(String model, List<String> rules, List<String> consistencies, List<String> defaults)
            throws Exception {
        String ir = "{\"format\":\"uchar-ir-v1\",\"model\":" + model + ",\"rules\":{\"rules\":[" + String.join(",", rules)
                + "],\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":["
                + String.join(",", consistencies) + "],"
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

    static JsonNode caseDocument(List<String> instanties, List<String> verwachtingen) throws Exception {
        return caseDocument(instanties, Map.of(), verwachtingen);
    }

    /** A case with whole-number {@code parameters} by name. */
    static JsonNode caseDocument(List<String> instanties, Map<String, String> parameters, List<String> verwachtingen)
            throws Exception {
        List<String> p = new ArrayList<>();
        parameters.forEach((naam, lexeme) -> p.add("\"" + naam + "\":{\"number\":\"" + lexeme + "\"}"));
        return JSON.readTree("{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":["
                + String.join(",", instanties) + "],\"parameters\":{" + String.join(",", p) + "}},\"expected\":["
                + String.join(",", verwachtingen) + "]}");
    }

    /** A Jutter bean of {@code beanLoader} with the characteristics of {@code kenmerken} set, as the case input sets
     *  them, for a witness that fires a session itself. */
    static Object jutterBean(ClassLoader beanLoader, List<String> kenmerken) throws Exception {
        Object beanObject = Class.forName(FactModelGenerator.PAKKET + ".Jutter", true, beanLoader).getConstructor().newInstance();
        for (String k : kenmerken) beanObject.getClass().getMethod(Namen.setter(k), boolean.class).invoke(beanObject, true);
        return beanObject;
    }

    /** A bean of {@code type} with whole-number attributes and characteristics set as the case input sets them. */
    static Object beanObject(ClassLoader beanLoader, String type, Map<String, String> numbers, List<String> kenmerken)
            throws Exception {
        Object beanObject = Class.forName(FactModelGenerator.PAKKET + "." + Namen.ident(type, true), true, beanLoader)
                .getConstructor().newInstance();
        for (Map.Entry<String, String> e : numbers.entrySet())
            beanObject.getClass().getMethod(Namen.setter(e.getKey()), java.math.BigDecimal.class)
                    .invoke(beanObject, new java.math.BigDecimal(e.getValue()));
        for (String k : kenmerken) beanObject.getClass().getMethod(Namen.setter(k), boolean.class).invoke(beanObject, true);
        return beanObject;
    }

    /** The value of attribute {@code attribuut} on the bean. */
    static Object lees(Object beanObject, String attribuut) throws Exception {
        return beanObject.getClass().getMethod(Namen.getter(attribuut)).invoke(beanObject);
    }

    /** Whether the bean carries {@code kenmerk}, read as the replay reads it. */
    static boolean has(Object beanObject, String kenmerk) throws Exception {
        return (Boolean) beanObject.getClass().getMethod("is" + Namen.ident(kenmerk, true)).invoke(beanObject);
    }

    // ---------------- documents with attributes, fact types and paths ----------------

    static final String WHOLE = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";

    /** An object type with attributes ({@link #attribuut}) and characteristics. */
    static String objecttype(String naam, List<String> attributes, List<String> kenmerken) {
        List<String> k = new ArrayList<>();
        for (String kenmerk : kenmerken)
            k.add("{\"node\":\"Characteristic\",\"name\":\"" + kenmerk + "\",\"kind\":\"adjectival\"}");
        return "{\"node\":\"ObjectType\",\"name\":\"" + naam + "\",\"animate\":true,\"plural\":\"" + naam + "s\","
                + "\"attributes\":[" + String.join(",", attributes) + "],\"characteristics\":[" + String.join(",", k) + "]}";
    }

    static String attribuut(String naam, String datatype) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + datatype + "}";
    }

    /** A fact type of two to-one roles. */
    static String feittype(String naam, String roleA, String typeA, String roleB, String typeB) {
        return "{\"node\":\"FactType\",\"name\":\"" + naam + "\",\"reciprocal\":false,\"roles\":[{\"node\":\"Role\",\"name\":\""
                + roleA + "\",\"objectType\":\"" + typeA + "\",\"many\":false},{\"node\":\"Role\",\"name\":\"" + roleB
                + "\",\"objectType\":\"" + typeB + "\",\"many\":false}]}";
    }

    /** A fact type of a to-one role {@code roleOne} played by {@code typeOne} and a to-many role {@code roleMany}, plural
     *  {@code meervoud}, played by {@code typeMany}: each {@code typeMany} relates to at most one {@code typeOne}. */
    static String factTypeOneToMany(String naam, String roleOne, String typeOne, String roleMany, String typeMany,
                                      String plural) {
        return "{\"node\":\"FactType\",\"name\":\"" + naam + "\",\"reciprocal\":false,\"roles\":[{\"node\":\"Role\",\"name\":\""
                + roleOne + "\",\"objectType\":\"" + typeOne + "\",\"many\":false},{\"node\":\"Role\",\"name\":\"" + roleMany
                + "\",\"objectType\":\"" + typeMany + "\",\"plural\":\"" + plural + "\",\"many\":true}]}";
    }

    /** A RoleBinding: the role {@code rol} of fact type {@code feittype}. */
    static String roleBinding(String feittype, String rol) {
        return "{\"node\":\"RoleBinding\",\"factType\":\"" + feittype + "\",\"role\":\"" + rol + "\"}";
    }

    static final String COUNTING = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"nonNegative\"}";

    /** An Aggregation over a set source: {@code kind} in its wire spelling ({@code count}, {@code sum}, …), its one
     *  {@code argument}, the {@code elementType}, the range as {@code "sourceRole":…} or {@code "sourcePath":[…]}, a
     *  {@code filter} and an {@code emptyFallback} (each "null" for none), and the result {@code type}. */
    static String aggregation(String kind, String argument, String elementType, String range, String filter,
                             String fallback, String type, String bron) {
        return "{\"node\":\"Aggregation\",\"kind\":\"" + kind + "\",\"arguments\":[" + argument + "],\"elementType\":\""
                + elementType + "\"" + (filter.equals("null") ? "" : ",\"filter\":" + filter)
                + (fallback.equals("null") ? "" : ",\"emptyFallback\":" + fallback) + "," + range + ",\"type\":" + type
                + ",\"source\":{\"snippet\":\"" + bron + "\"}}";
    }

    /** The range of a fold over one relation step. */
    static String sourceRole(String feittype, String rol) {
        return "\"sourceRole\":" + roleBinding(feittype, rol);
    }

    /** The range of a fold over two or more relation steps, each a RoleBinding. */
    static String sourcePath(List<String> steps) {
        return "\"sourcePath\":[" + String.join(",", steps) + "]";
    }

    /** A listed fold, "de eerste/laatste/minimale/maximale van A en B …", over the {@code operands}. */
    static String listedFold(String kind, List<String> operands, String type, String bron) {
        return "{\"node\":\"Aggregation\",\"kind\":\"" + kind + "\",\"arguments\":[" + String.join(",", operands)
                + "],\"type\":" + type + ",\"source\":{\"snippet\":\"" + bron + "\"}}";
    }

    /** An instance of the case whose roles may link several instances each, by role name. */
    static String instanceWithRoles(String label, String type, Map<String, String> numbers, List<String> kenmerken,
                                     Map<String, List<String>> roles) {
        List<String> a = new ArrayList<>(), k = new ArrayList<>(), r = new ArrayList<>();
        numbers.forEach((naam, lexeme) -> a.add("\"" + naam + "\":{\"number\":\"" + lexeme + "\"}"));
        for (String kenmerk : kenmerken) k.add("\"" + kenmerk + "\"");
        roles.forEach((rol, targets) -> {
            List<String> d = new ArrayList<>();
            for (String doel : targets) d.add("\"" + doel + "\"");
            r.add("\"" + rol + "\":[" + String.join(",", d) + "]");
        });
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + String.join(",", a)
                + "},\"roles\":{" + String.join(",", r) + "},\"characteristics\":[" + String.join(",", k) + "]}";
    }

    static String model(List<String> objectTypes, List<String> factTypes) {
        return model(objectTypes, factTypes, List.of());
    }

    /** A model with {@code parameters}, each a whole-number Parameter named by its entry. */
    static String model(List<String> objectTypes, List<String> factTypes, List<String> parameters) {
        List<String> p = new ArrayList<>();
        for (String naam : parameters)
            p.add("{\"node\":\"Parameter\",\"name\":\"" + naam + "\",\"datatype\":" + WHOLE + "}");
        return "{\"objectTypes\":[" + String.join(",", objectTypes) + "],\"factTypes\":[" + String.join(",", factTypes)
                + "],\"parameters\":[" + String.join(",", p) + "],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],"
                + "\"timelines\":[],\"dayKinds\":[]}";
    }

    /** "het {@code parameter}": a read of a parameter. */
    static String parameterRead(String parameter) {
        return "{\"node\":\"ParameterReference\",\"parameter\":\"" + parameter + "\",\"source\":{\"snippet\":\"het "
                + parameter + "\"}}";
    }

    /** "zijn {@code attribuut}": a read of the subject's own attribute. */
    static String ownRead(String attribuut, String type) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"" + type
                + "\",\"source\":{\"snippet\":\"zijn " + attribuut + "\"}}";
    }

    /** "de {@code attribuut} van zijn {@code rol}": a read one role away. */
    static String pathRead(String attribuut, String type, String feittype, String rol) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"" + type
                + "\",\"source\":{\"snippet\":\"de " + attribuut + " van zijn " + rol + "\"},\"path\":[{\"node\":"
                + "\"RoleBinding\",\"factType\":\"" + feittype + "\",\"role\":\"" + rol + "\"}]}";
    }

    /** "zijn {@code rol} is [niet] {@code kenmerk}": a characteristic check one role away. */
    static String pathCheck(String kenmerk, boolean negated, String feittype, String rol) {
        return "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"" + kenmerk + "\",\"negated\":" + negated
                + ",\"source\":{\"snippet\":\"zijn " + rol + " " + (negated ? "niet " : "") + kenmerk + " is\"},"
                + "\"navigation\":{\"node\":\"RoleBinding\",\"factType\":\"" + feittype + "\",\"role\":\"" + rol + "\"}}";
    }

    static String number(String lexeme) {
        return IrPath.literal(lexeme, WHOLE, lexeme);
    }

    /** A binary operation in its wire spelling ({@code plus}, {@code times}, {@code dividedBy}, …), of type whole number. */
    static String binary(String op, String left, String right) {
        return "{\"node\":\"Binary\",\"op\":\"" + op + "\",\"left\":" + left + ",\"right\":" + right + ",\"type\":" + WHOLE
                + ",\"source\":{\"snippet\":\"" + op + "\"}}";
    }

    /** A comparison in its wire spelling ({@code greater}, {@code notEqual}, …). */
    static String comparison(String op, String left, String right) {
        return "{\"node\":\"Comparison\",\"op\":\"" + op + "\",\"left\":" + left + ",\"right\":" + right
                + ",\"source\":{\"snippet\":\"" + op + "\"}}";
    }

    /** A leeg ({@code empty}) or gevuld ({@code filled}) test. */
    static String predicate(String kind, String operand) {
        return "{\"node\":\"Predicate\",\"kind\":\"" + kind + "\",\"operand\":" + operand + ",\"negated\":false,\"digits\":0,"
                + "\"source\":{\"snippet\":\"" + kind + "\"}}";
    }

    /** An assignment of attribute {@code doel} of {@code type} ({@code soort} as {@link IrPath#regel} names it). */
    static String computingRule(String naam, String soort, String type, String doel, String waarde, String voorwaarde) {
        String target = "{\"node\":\"AttributeReference\",\"attribute\":\"" + doel + "\",\"subjectType\":\"" + type + "\","
                + "\"source\":{\"snippet\":\"De " + doel + " van een " + type + "\"}}";
        return IrPath.regel(naam, soort, target, waarde, voorwaarde);
    }

    /** "Een {@code type} is {@code doel} indien {@code voorwaarde}". */
    static String characteristicRule(String naam, String type, String doel, String voorwaarde) {
        String target = "{\"node\":\"AttributeReference\",\"attribute\":\"" + doel + "\",\"subjectType\":\"" + type + "\","
                + "\"source\":{\"snippet\":\"Een " + type + " is " + doel + "\"}}";
        return IrPath.regel(naam, "KENMERKTOEKENNING", target, WAAR, voorwaarde);
    }

    /** An instance of the case: whole-number attributes by name, characteristics, and to-one roles by role name. */
    static String instance(String label, String type, Map<String, String> numbers, List<String> kenmerken,
                            Map<String, String> roles) {
        List<String> a = new ArrayList<>(), k = new ArrayList<>(), r = new ArrayList<>();
        numbers.forEach((naam, lexeme) -> a.add("\"" + naam + "\":{\"number\":\"" + lexeme + "\"}"));
        for (String kenmerk : kenmerken) k.add("\"" + kenmerk + "\"");
        roles.forEach((rol, doel) -> r.add("\"" + rol + "\":[\"" + doel + "\"]"));
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + String.join(",", a)
                + "},\"roles\":{" + String.join(",", r) + "},\"characteristics\":[" + String.join(",", k) + "]}";
    }

    /** The expectation that attribute {@code attribuut} of {@code label} ends at the number {@code lexeme}, or leeg. */
    static String waarde(String label, String attribuut, String lexeme) {
        return lexeme == null
                ? "{\"kind\":\"attributeEmpty\",\"label\":\"" + label + "\",\"attribute\":\"" + attribuut + "\"}"
                : "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"" + attribuut + "\",\"value\":"
                  + "{\"number\":\"" + lexeme + "\"}}";
    }

    /** A Consistency of one always-valid version: {@code assertion} must hold for every {@code subject}. */
    static String consistency(String naam, String subject, String assertion) {
        return "{\"node\":\"Consistency\",\"name\":\"" + naam + "\",\"versions\":[{\"subject\":\"" + subject
                + "\",\"assertion\":" + assertion + ",\"validity\":{\"node\":\"Validity\"},\"source\":{\"snippet\":\""
                + naam + "\"}}]}";
    }

    /** An object type that is a {@code supertype} (IR-SPEC ch. 2.2), with attributes and characteristics of its own. */
    static String subtype(String naam, String supertype, List<String> attributes, List<String> kenmerken) {
        String t = objecttype(naam, attributes, kenmerken);
        return t.replace("\"name\":\"" + naam + "\",", "\"name\":\"" + naam + "\",\"supertype\":\"" + supertype + "\",");
    }

    // ---------------- prioritized defaults ----------------

    /** A defaults group on attribute {@code attribuut} of {@code type}, its {@code defaults} highest first. */
    static String attributeGroup(String naam, String type, String attribuut, List<String> defaults) {
        return "{\"node\":\"PrioritizedDefaults\",\"name\":\"" + naam + "\",\"subject\":\"" + type + "\",\"attribute\":\""
                + attribuut + "\",\"defaults\":[" + String.join(",", defaults) + "],\"validity\":{\"node\":\"Validity\"}}";
    }

    /** A defaults group on characteristic {@code kenmerk} of {@code type}, its {@code defaults} highest first. */
    static String characteristicGroup(String naam, String type, String kenmerk, List<String> defaults) {
        return "{\"node\":\"PrioritizedDefaults\",\"name\":\"" + naam + "\",\"subject\":\"" + type
                + "\",\"characteristic\":\"" + kenmerk + "\",\"defaults\":[" + String.join(",", defaults)
                + "],\"validity\":{\"node\":\"Validity\"}}";
    }

    /** An attribute default: its justification and its consequence. */
    static String standaard(String rechtvaardiging, String consequentie) {
        return "{\"node\":\"Default\",\"justification\":" + rechtvaardiging + ",\"consequence\":" + consequentie + "}";
    }

    /** A characteristic default: its justification and its decision, attribute or do-not-attribute. */
    static String decision(String rechtvaardiging, boolean decision) {
        return "{\"node\":\"Default\",\"decision\":" + decision + ",\"justification\":" + rechtvaardiging + "}";
    }

    /** A form-a default with exception groups: its own justification and consequence, and the members, each a chain of
     *  defaults highest first, reconciled as a declared-unordered group (RUNTIME-SEMANTICS ch. 7.9). */
    static String withExceptions(String rechtvaardiging, String consequentie, List<List<String>> members) {
        return "{\"node\":\"Default\",\"justification\":" + rechtvaardiging + ",\"consequence\":" + consequentie
                + ",\"exceptions\":" + exceptions(members) + "}";
    }

    /** A group-default: exception groups and no content of its own. */
    static String groupDefault(List<List<String>> members) {
        return "{\"node\":\"Default\",\"exceptions\":" + exceptions(members) + "}";
    }

    private static String exceptions(List<List<String>> members) {
        List<String> m = new ArrayList<>();
        for (List<String> member : members)
            m.add("{\"node\":\"ExceptionGroup\",\"defaults\":[" + String.join(",", member) + "]}");
        return "{\"unordered\":true,\"members\":[" + String.join(",", m) + "]}";
    }

    /** "regelversie {@code regel} is inconsistent": a test of the §9.5 channel of the named form. */
    static String channelTest(String regel) {
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
     *  {@code aantal} shuffles from {@code seed}, so a failing order can be replayed. */
    static <T> List<List<T>> sample(List<T> l, long seed, int aantal) {
        List<List<T>> out = new ArrayList<>();
        out.add(l);
        List<T> reversed = new ArrayList<>(l);
        java.util.Collections.reverse(reversed);
        out.add(reversed);
        java.util.Random chance = new java.util.Random(seed);
        for (int i = 0; i < aantal; i++) {
            List<T> v = new ArrayList<>(l);
            java.util.Collections.shuffle(v, chance);
            out.add(v);
        }
        return out;
    }

    /** A KieBase of the rules {@code b}'s transpiler generated, added in {@code order}, as the branch builds it. */
    static org.kie.api.KieBase kieBase(DroolsBackend.Build b, List<org.drools.model.Rule> order) {
        org.drools.model.impl.ModelImpl m = new org.drools.model.impl.ModelImpl(b.execModelTranspiler().model().getName());
        order.forEach(m::addRule);
        b.execModelTranspiler().model().getGlobals().forEach(m::addGlobal);
        return org.drools.modelcompiler.KieBaseBuilder.createKieBaseFromModel(m,
                org.kie.api.conf.EqualityBehaviorOption.EQUALITY);
    }

    /** The branches a sample of orders of the rules fires on (card drl-teksttak-op-truth-maintenance, round 5b): the
     *  exec-model branch, and {@code DRL_TEXT}, its analysis rendered as DRL text, whose rules the sample puts in order
     *  in the text ({@code TextOrder}), since the order of the rules in the text is the order in which Drools loads
     *  them, and with it the order of the agenda among matches of equal standing. */
    static final List<DroolsBackend.Branch> BRANCHES = List.of(DroolsBackend.Branch.EXEC_MODEL,
            DroolsBackend.Branch.DRL_TEXT);

    /** The shuffles of a sample on the text in the samples brought to the text after round 5b ({@link #shuffles}). */
    static final int TEXT_SHUFFLES = 40;

    /** The shuffles of a sample of orders on {@code branch} ({@link #sample}): {@code onModel} on the exec-model
     *  branch, and on the text at most {@link #TEXT_SHUFFLES}, since there every order renders and compiles a DRL
     *  text of its own, which costs some 70 ms where the executable model adds its rules in a millisecond, and the
     *  suite has a budget for the samples on the text (fix round 5b, in step 6a). With the same seed the orders on the
     *  text are the first of those on the model: the document's order, its reverse and the first shuffles. */
    static int shuffles(DroolsBackend.Branch branch, int onModel) {
        return branch == DroolsBackend.Branch.EXEC_MODEL ? onModel : Math.min(onModel, TEXT_SHUFFLES);
    }

    /** The names of the rules of {@code b}, in the order in which its branch generates them: those of its executable
     *  model, or on {@code DRL_TEXT} those of its text, which must be the same in the same order, so that one sample
     *  gives both branches the same orders. */
    static List<String> gegenereerd(DroolsBackend.Build b) {
        List<String> model = b.execModelTranspiler().model().getRules().stream().map(org.drools.model.Rule::getName)
                .toList();
        if (b.branch() == DroolsBackend.Branch.EXEC_MODEL) return model;
        List<String> text = dev.uchar.regelspraak.transpiler.tms.TextOrder.rules(b);
        if (!text.equals(model))
            throw new AssertionError("de regels van de tekst " + text + " zijn niet die van het model " + model);
        return text;
    }

    /** A KieBase of the rules of {@code b} named in {@code namen}, each once and in that order, as its branch builds
     *  them: the rules of its executable model ({@link #kieBase}), or on {@code DRL_TEXT} the rules of its plan
     *  rendered as text ({@code TextOrder}). */
    static org.kie.api.KieBase kieBaseOf(DroolsBackend.Build b, List<String> names) {
        if (b.branch() == DroolsBackend.Branch.DRL_TEXT)
            return dev.uchar.regelspraak.transpiler.tms.TextOrder.kieBase(b, names);
        Map<String, org.drools.model.Rule> perName = new java.util.HashMap<>();
        for (org.drools.model.Rule r : b.execModelTranspiler().model().getRules())
            if (perName.put(r.getName(), r) != null)
                throw new IllegalStateException("twee regels heten " + r.getName());
        List<org.drools.model.Rule> order = new ArrayList<>();
        for (String naam : names) {
            org.drools.model.Rule r = perName.remove(naam);
            if (r == null) throw new IllegalArgumentException("geen regel " + naam + " (of twee keer) in " + names);
            order.add(r);
        }
        if (!perName.isEmpty()) throw new IllegalArgumentException("de volgorde mist " + perName.keySet());
        return kieBase(b, order);
    }

    /** Fires {@code beans} through the rules of {@code b} added in {@code order}: {@code voor} sees the session before
     *  the fire (to add a listener), the fire must reach its fixpoint below the cap, the derivations are written back,
     *  and {@code na} sees the session before it is disposed. */
    static void vuur(DroolsBackend.Build b, List<org.drools.model.Rule> order, List<Object> beans,
                     java.util.function.Consumer<org.kie.api.runtime.KieSession> before,
                     java.util.function.Consumer<org.kie.api.runtime.KieSession> after) {
        vuur(b, kieBase(b, order), order.stream().map(org.drools.model.Rule::getName).toList(), beans, before, after);
    }

    /** As {@link #vuur(DroolsBackend.Build, List, List, java.util.function.Consumer, java.util.function.Consumer)}
     *  through {@code kieBase}, which holds the rules of {@code b} in the order {@code namen}: those of the executable
     *  model ({@link #kieBase}) or those of the text ({@code TextOrder}). */
    static void vuur(DroolsBackend.Build b, org.kie.api.KieBase kieBase, List<String> names, List<Object> beans,
                     java.util.function.Consumer<org.kie.api.runtime.KieSession> before,
                     java.util.function.Consumer<org.kie.api.runtime.KieSession> after) {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        org.kie.api.runtime.KieSession sessie = kieBase.newKieSession();
        try {
            before.accept(sessie);
            for (Object beanObject : beans) sessie.insert(beanObject);
            if (DroolsBackend.vuur(b, sessie, 100_000).geenFixpunt() != null)
                throw new AssertionError(b.branch() + ": geen fixpunt in volgorde " + names);
            DroolsBackend.zetAfgeleideWaarden(b, sessie);
            after.accept(sessie);
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    /** The §9.5 facts of {@code sessie} as "rule @ label", sorted, with {@code labels} naming the beans. */
    static List<String> inconsistenties(org.kie.api.runtime.KieSession sessie, Map<Object, String> labels) {
        List<String> out = new ArrayList<>();
        for (Object f : sessie.getObjects(o -> o.getClass().getSimpleName().equals("Inconsistentie"))) {
            try {
                Object regel = f.getClass().getMethod("getRegel").invoke(f);
                Object subject = f.getClass().getMethod("getSubject").invoke(f);
                out.add(regel + " @ " + labels.getOrDefault(subject, String.valueOf(subject)));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
        out.sort(null);
        return out;
    }

    /** The names of the rules of a document, in their order, for a message that names the order that failed. */
    static List<String> ruleNames(List<String> rules) throws Exception {
        List<String> out = new ArrayList<>();
        for (String r : rules) out.add(JSON.readTree(r).path("name").asText());
        return out;
    }

    // ---------------- documents with dates, texts, units, the value forms and an evaluation date ----------------

    static final String DATE = "{\"node\":\"Date\",\"precision\":\"day\"}";
    static final String DATE_TIME = "{\"node\":\"Date\",\"precision\":\"millisecond\"}";
    static final String TEKST = "{\"node\":\"Text\"}";

    /** A number type, whole or with {@code decimals} decimals, with the unit {@code unit} (a Unit node, or null). */
    static String numberType(boolean whole, int decimals, String unit) {
        return "{\"node\":\"Number\",\"whole\":" + whole + ",\"decimals\":" + decimals + ",\"sign\":\"unspecified\""
                + (unit == null ? "" : ",\"unit\":" + unit) + "}";
    }

    /** A Unit of system {@code system}, {@code num}/{@code den} of the canonical unit {@code canonical}. */
    static String unit(String abbreviation, String system, String canonical, int num, int den) {
        return "{\"node\":\"Unit\",\"abbreviation\":\"" + abbreviation + "\"" + (system == null ? "" : ",\"system\":\""
                + system + "\"") + ",\"canonical\":\"" + canonical + "\",\"factorToCanonical\":{\"num\":" + num
                + ",\"den\":" + den + "}}";
    }

    /** A Unit written by its abbreviation alone, as a calendar unit is (RUNTIME-SEMANTICS ch. 3.8). */
    static String unit(String abbreviation) {
        return "{\"node\":\"Unit\",\"abbreviation\":\"" + abbreviation + "\"}";
    }

    /** The unit of a percentage, which the loader reads as the Percentage datatype. */
    static final String PERCENT = unit("%", null, "%", 1, 1);

    /** A unit system of {@code eenheden}, Unit nodes of that system. */
    static String unitSystem(String naam, List<String> units) {
        return "{\"node\":\"UnitSystem\",\"name\":\"" + naam + "\",\"units\":[" + String.join(",", units) + "]}";
    }

    /** A model with object types, fact types, parameters as written ({@link #parameter}) and unit systems. */
    static String modelWith(List<String> objectTypes, List<String> factTypes, List<String> parameters,
                           List<String> systems) {
        return "{\"objectTypes\":[" + String.join(",", objectTypes) + "],\"factTypes\":[" + String.join(",", factTypes)
                + "],\"parameters\":[" + String.join(",", parameters) + "],\"domains\":[],\"unitSystems\":["
                + String.join(",", systems) + "],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";
    }

    /** A Parameter of datatype {@code type}. */
    static String parameter(String naam, String type) {
        return "{\"node\":\"Parameter\",\"name\":\"" + naam + "\",\"datatype\":" + type + "}";
    }

    /** A Literal of {@code type}, its snippet the lexeme. */
    static String literal(String lexeme, String type) {
        return IrPath.literal(lexeme, type, lexeme);
    }

    /** A binary operation in its wire spelling with the result {@code type}. */
    static String binary(String op, String left, String right, String type) {
        return "{\"node\":\"Binary\",\"op\":\"" + op + "\",\"left\":" + left + ",\"right\":" + right + ",\"type\":" + type
                + ",\"source\":{\"snippet\":\"" + op + "\"}}";
    }

    /** "{@code basis} afgerond op {@code decimals} decimalen" in the mode's wire spelling. */
    static String rounding(String base, String mode, int decimals, String type) {
        return "{\"node\":\"Rounding\",\"base\":" + base + ",\"mode\":\"" + mode + "\",\"decimals\":" + decimals
                + ",\"type\":" + type + ",\"source\":{\"snippet\":\"afgerond op " + decimals + " decimalen\"}}";
    }

    /** "{@code basis} begrensd" with a minimum and a maximum, each "null" for none. */
    static String clamp(String base, String minimum, String maximum, String type) {
        return "{\"node\":\"Clamp\",\"base\":" + base + (minimum.equals("null") ? "" : ",\"minimum\":" + minimum)
                + (maximum.equals("null") ? "" : ",\"maximum\":" + maximum) + ",\"type\":" + type
                + ",\"source\":{\"snippet\":\"begrensd\"}}";
    }

    /** A BuiltinCall of {@code kind} in its wire spelling over {@code arguments}, with the result {@code type}. */
    static String builtin(String kind, List<String> arguments, String type, String bron) {
        return "{\"node\":\"BuiltinCall\",\"kind\":\"" + kind + "\",\"arguments\":[" + String.join(",", arguments)
                + "],\"type\":" + type + ",\"source\":{\"snippet\":\"" + bron + "\"}}";
    }

    /** "Daarbij geldt: {@code binder} is {@code waarde}" around {@code body}: a Let of the body's {@code type}. */
    static String let(String binder, String waarde, String body, String type) {
        return "{\"node\":\"Let\",\"type\":" + type + ",\"binder\":\"" + binder + "\",\"value\":" + waarde + ",\"body\":"
                + body + ",\"source\":{\"snippet\":\"" + binder + " is …\"}}";
    }

    /** A read of the binder {@code binder}. */
    static String binderRead(String binder) {
        return "{\"node\":\"BinderRef\",\"name\":\"" + binder + "\",\"source\":{\"snippet\":\"" + binder + "\"}}";
    }

    /** "als {@code voorwaarde} dan {@code dan} anders {@code anders}" of {@code type}. */
    static String keuze(String voorwaarde, String then, String otherwise, String type) {
        return "{\"node\":\"IfThenElse\",\"type\":" + type + ",\"condition\":" + voorwaarde + ",\"then\":" + then
                + ",\"else\":" + otherwise + ",\"source\":{\"snippet\":\"als … dan … anders …\"}}";
    }

    /** The context value {@code naam} (evaluationDate, evaluationYear) of {@code type}. */
    static String contextValue(String naam, String type) {
        return "{\"node\":\"ContextValue\",\"name\":\"" + naam + "\",\"type\":" + type + ",\"source\":{\"snippet\":\"het "
                + naam + "\"}}";
    }

    /** A TimeWindow from {@code van} to {@code tot}, each "null" for an open bound. */
    static String window(String van, String to, boolean toInclusive) {
        return "{\"node\":\"TimeWindow\"" + (van.equals("null") ? "" : ",\"from\":" + van)
                + (to.equals("null") ? "" : ",\"to\":" + to) + ",\"toInclusive\":" + toInclusive
                + ",\"source\":{\"snippet\":\"van … tot …\"}}";
    }

    /** A TextComposition of {@code parts}, each a {@link #textPart} or an {@link #textExpression}. */
    static String textComposition(List<String> parts) {
        return "{\"node\":\"TextComposition\",\"parts\":[" + String.join(",", parts) + "],\"type\":" + TEKST
                + ",\"source\":{\"snippet\":\"de reeks van …\"}}";
    }

    static String textPart(String text) {
        return "{\"node\":\"LiteralChunk\",\"text\":\"" + text + "\"}";
    }

    static String textExpression(String expr) {
        return "{\"node\":\"EmbeddedExpression\",\"expr\":" + expr + "}";
    }

    /** A Validity from {@code van} to {@code tot}, both inclusive, each null for an open bound. */
    static String validity(String van, String to) {
        return "{\"node\":\"Validity\"" + (van == null ? "" : ",\"from\":\"" + van + "\"")
                + (to == null ? "" : ",\"to\":\"" + to + "\"") + "}";
    }

    /** The case with the evaluation date {@code asOfDate} (null for a time-free session). */
    static JsonNode caseOn(String asOfDate, List<String> instanties, List<String> verwachtingen) throws Exception {
        return JSON.readTree("{\"format\":\"uchar-case-v0\",\"asOfDate\":"
                + (asOfDate == null ? "null" : "\"" + asOfDate + "\"") + ",\"input\":{\"instances\":["
                + String.join(",", instanties) + "],\"parameters\":{}},\"expected\":[" + String.join(",", verwachtingen)
                + "]}");
    }

    /** An instance whose attributes are given as case values in the wire's shape ({@code {"number":"5"}},
     *  {@code {"date":"2026-01-01"}}, {@code {"text":"…"}}), by attribute. */
    static String instanceWith(String label, String type, Map<String, String> waarden, List<String> kenmerken) {
        List<String> a = new ArrayList<>(), k = new ArrayList<>();
        waarden.forEach((naam, waarde) -> a.add("\"" + naam + "\":" + waarde));
        for (String kenmerk : kenmerken) k.add("\"" + kenmerk + "\"");
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + String.join(",", a)
                + "},\"roles\":{},\"characteristics\":[" + String.join(",", k) + "]}";
    }

    /** The expectation that attribute {@code attribuut} of {@code label} ends at a case value in the wire's shape. */
    static String valueJson(String label, String attribuut, String waarde) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"" + attribuut + "\",\"value\":"
                + waarde + "}";
    }

    static String numberValue(String lexeme) {
        return "{\"number\":\"" + lexeme + "\"}";
    }

    static String dateValue(String iso) {
        return "{\"date\":\"" + iso + "\"}";
    }

    static String textValue(String text) {
        return "{\"text\":\"" + text + "\"}";
    }

    /** Fires {@code beans} through the rules of {@code b} added in {@code order}, with {@code voor} seeing the session
     *  before the fire (a listener, a global). The fire must reach its fixpoint below the cap without throwing: an
     *  evaluation fault is a fact of the session, not an exception out of the fire. Returns the fault the write-back
     *  raises at the fixpoint as "SimpleName: message", or null where it writes the derivations back. */
    static String fixpunt(DroolsBackend.Build b, List<org.drools.model.Rule> order, List<Object> beans,
                          java.util.function.Consumer<org.kie.api.runtime.KieSession> before) {
        List<String> names = order.stream().map(org.drools.model.Rule::getName).toList();
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        org.kie.api.runtime.KieSession sessie = kieBase(b, order).newKieSession();
        try {
            before.accept(sessie);
            for (Object beanObject : beans) sessie.insert(beanObject);
            DroolsBackend.Vuring firing;
            try {
                firing = DroolsBackend.vuur(b, sessie, 100_000);
            } catch (RuntimeException e) {
                throw new AssertionError("de fire wierp in volgorde " + names + ": " + e, e);
            }
            if (firing.geenFixpunt() != null) throw new AssertionError("geen fixpunt in volgorde " + names);
            try {
                DroolsBackend.zetAfgeleideWaarden(b, sessie);
                return null;
            } catch (RuntimeException e) {
                return e.getClass().getSimpleName() + ": " + e.getMessage();
            }
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    /** Every order of {@code l}. */
    static <T> List<List<T>> permutations(List<T> l) {
        if (l.size() <= 1) return List.of(l);
        List<List<T>> out = new ArrayList<>();
        for (int i = 0; i < l.size(); i++) {
            List<T> rest = new ArrayList<>(l);
            T eerste = rest.remove(i);
            for (List<T> p : permutations(rest)) {
                List<T> q = new ArrayList<>();
                q.add(eerste);
                q.addAll(p);
                out.add(q);
            }
        }
        return out;
    }
}
