package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.refusal.DroolsCode;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.core.Namen;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieSession;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A characteristic the input states not to hold ({@code characteristicsAbsent}, RUNTIME-SEMANTICS ch. 1.5) whose declared
 * name is not its bean property as written (card absent-characteristic-property). The input reads the bean through the
 * property of the declared name ({@link Namen#property}), as it writes the characteristics that hold through
 * {@link Namen#setter}, and a direct caller of {@link Input#characteristicAbsent} names the characteristic in any
 * spelling that is its declared name under {@link Namen#norm}. Per name of {@link #NAMES}, on a document with an object
 * type {@code Kenmerkproef} that declares a characteristic for each and has no rule:
 * <ul>
 *   <li>in a document with {@code inferFromEmpty: false} a characteristic stated not to hold does not hold, and one the
 *       case does not name is open;</li>
 *   <li>in an ordinary document the statement changes nothing: every characteristic the case does not state to hold
 *       does not hold;</li>
 *   <li>a characteristic the case names in {@code characteristics} and in {@code characteristicsAbsent} is EF-047;</li>
 *   <li>{@link DroolsBackend#insert} takes {@link Input#characteristicAbsent} in the declared spelling and in another
 *       spelling of the same name, leaves that characteristic out of what is open, and refuses it with DROOLS-371 where
 *       the bean holds it.</li>
 * </ul>
 * Each on both branches. The golden {@code jutterswet-kenmerknamen-strikt} plays such names through the reference
 * interpreter, the ASP backend and this one; this class reaches the names the RegelSpraak lexer does not write in a
 * characteristic (a word in capitals, a digit first, {@code ²}, {@code İ}).
 *
 * <p><b>Red</b> on {@code d655416e0}: each case with {@code characteristicsAbsent} in both documents, and each case that
 * names a characteristic in both lists, fails with "geen getter voor de eigenschap '&lt;name&gt;' op
 * dev.uchar.regelspraak.facts.Kenmerkproef: get&lt;name&gt; noch is&lt;name&gt;", the first letter of the name in
 * upper case (an {@code IllegalStateException}, no
 * EF code); {@code insert} fails so in the declared spelling, and refuses another spelling with DROOLS-371 and
 * "zijn type Kenmerkproef declareert geen kenmerk met die naam".
 */
class AbsentCharacteristicNameWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** Declared names whose bean member a getter derived from the name as written misses: a space, a hyphen, an
     *  apostrophe, a word in capitals ({@code EU}, whose getter is {@code isEu}), a digit first ({@code isX3eLid}), a
     *  number sign no identifier holds ({@code isM2}), and {@code İş}, whose property is {@code iş}. A capital first
     *  letter or a diacritic alone ("reëel") gives the getter of the generator, and is no witness here. */
    static final List<String> NAMES = List.of("op zee", "niet-erkend", "jutter's", "EU", "3e lid", "m²", "İş");

    @Test void a_characteristic_stated_absent_does_not_hold_in_a_strict_document() throws Exception {
        assertEquals(List.of(), play(false, "characteristicAbsent", "characteristicOpen"));
    }

    @Test void a_characteristic_stated_absent_changes_nothing_in_an_ordinary_document() throws Exception {
        assertEquals(List.of(), play(true, "characteristicAbsent", "characteristicAbsent"));
    }

    @Test void a_characteristic_stated_to_hold_and_not_to_hold_is_ef_047() throws Exception {
        List<String> faults = new ArrayList<>();
        for (boolean ordinary : new boolean[] { false, true }) {
            Path ir = Files.createTempFile("kenmerknamen", ".json"), c = Files.createTempFile("kenmerknamen-case", ".json");
            try {
                Files.writeString(ir, document(ordinary));
                for (String name : NAMES) {
                    Files.writeString(c, JSON.writeValueAsString(caseOf(List.of(name), List.of(name),
                            expected("characteristicPresent", name))));
                    for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
                        CaseRunner.RedResult r = CaseRunner.runRed(ir, c, branch);
                        if (!"FAULT".equals(r.status()) || !"EF-047".equals(r.code())
                                || !List.of(Map.of("label", "K1", "characteristic", name)).equals(r.params()))
                            faults.add(branch + (ordinary ? " ordinary " : " strict ") + name + " | " + r.status() + " "
                                    + r.code() + " " + r.params() + ": " + r.detail());
                    }
                }
            } finally {
                Files.delete(ir);
                Files.delete(c);
            }
        }
        assertEquals(List.of(), faults);
    }

    @Test void insert_takes_a_characteristic_stated_absent_in_any_spelling_of_its_name() throws Exception {
        List<String> faults = new ArrayList<>();
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            DroolsBackend.Build b = DroolsBackend.build(JSON.readTree(document(false)), branch);
            Class<?> trial = Class.forName(FactModelGenerator.PACKAGE + ".Kenmerkproef", true, b.beanLoader());
            TmsRepeatedFireWitnessTest.inSession(b, () -> {
                for (String name : NAMES)
                    for (String spelling : List.of(name, otherSpelling(name))) {
                        String what = branch + " " + name + " as «" + spelling + "»";
                        List<String> open = new ArrayList<>(NAMES);
                        open.remove(name);
                        try {
                            List<String> found = openAfter(b, trial.getConstructor().newInstance(), spelling);
                            if (!open.equals(found)) faults.add(what + " | open " + found + ", expected " + open);
                        } catch (RuntimeException e) {
                            faults.add(what + " | " + e);
                        }
                        Object holding = trial.getConstructor().newInstance();
                        trial.getMethod(Namen.setter(name), boolean.class).invoke(holding, true);
                        String refusal = DroolsCode.INSERT_ABSENT_CHARACTERISTIC.reason("bean", name(holding),
                                "characteristic", spelling, "reason",
                                DroolsCode.INSERT_ABSENT_CHARACTERISTIC.part("holds")).text();
                        KieSession session = b.kieBase().newKieSession();
                        try {
                            DroolsBackend.insert(b, session, new Input().bean(holding).characteristicAbsent(holding, spelling));
                            faults.add(what + " | the bean holds it, and insert took it");
                        } catch (IllegalArgumentException e) {
                            if (!refusal.equals(e.getMessage())) faults.add(what + " | " + e.getMessage());
                        } catch (RuntimeException e) {
                            faults.add(what + " | " + e);
                        } finally {
                            session.dispose();
                        }
                    }
            });
        }
        assertEquals(List.of(), faults);
    }

    /** The names of the characteristics that are open after {@code bean}, stated not to hold {@code spelling}, is put in
     *  a fresh session of {@code b} and fired. */
    private static List<String> openAfter(DroolsBackend.Build b, Object bean, String spelling) {
        KieSession session = b.kieBase().newKieSession();
        try {
            DroolsBackend.insert(b, session, new Input().bean(bean).characteristicAbsent(bean, spelling));
            assertNull(DroolsBackend.fire(b, session).noFixpoint(), "the fixpoint");
            List<String> open = new ArrayList<>();
            for (EndState.Open o : DroolsBackend.endStateOf(b, session, x -> "K1").objects().get(0).open())
                if (o.characteristic()) open.add(o.name());
            return open;
        } finally {
            session.dispose();
        }
    }

    /** {@code name} in another spelling that is the same name under {@link Namen#norm}: a leading space, every space
     *  doubled and the letters A-Z in the other case. */
    private static String otherSpelling(String name) {
        StringBuilder out = new StringBuilder(" ");
        for (char c : name.toCharArray())
            out.append(c == ' ' ? "  " : c >= 'a' && c <= 'z' ? String.valueOf((char) (c - 32))
                    : c >= 'A' && c <= 'Z' ? String.valueOf((char) (c + 32)) : String.valueOf(c));
        return out.toString();
    }

    /** Per name of {@link #NAMES} and branch, the document {@link #document} played with two cases: the name stated not
     *  to hold, expected {@code stated}, and no characteristic named, expected {@code unnamed}; what fails, by name. */
    private static List<String> play(boolean ordinary, String stated, String unnamed) throws Exception {
        List<String> faults = new ArrayList<>();
        Path file = Files.createTempFile("kenmerknamen", ".json");
        try {
            Files.writeString(file, document(ordinary));
            for (String name : NAMES) {
                List<JsonNode> cases = List.of(caseOf(List.of(), List.of(name), expected(stated, name)),
                        caseOf(List.of(), List.of(), expected(unnamed, name)));
                for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
                    try {
                        List<String> verdicts = CaseRunner.playCases(file, cases, branch, false);
                        for (int i = 0; i < cases.size(); i++)
                            if (verdicts.get(i) != null) faults.add(branch + " " + name + " case " + (i + 1) + " | "
                                    + verdicts.get(i));
                    } catch (RuntimeException e) {
                        faults.add(branch + " " + name + " | " + e);
                    }
                }
            }
        } finally {
            Files.delete(file);
        }
        return faults;
    }

    /** One {@code Kenmerkproef} K1 with {@code holding} in {@code characteristics} and {@code absent} in
     *  {@code characteristicsAbsent}, and the expectations {@code expected}. */
    private static JsonNode caseOf(List<String> holding, List<String> absent, List<Map<String, String>> expected) {
        Map<String, Object> instance = new java.util.LinkedHashMap<>();
        instance.put("label", "K1");
        instance.put("objectType", "Kenmerkproef");
        instance.put("attributes", Map.of());
        instance.put("roles", Map.of());
        instance.put("characteristics", holding);
        if (!absent.isEmpty()) instance.put("characteristicsAbsent", absent);
        Map<String, Object> c = new java.util.LinkedHashMap<>();
        c.put("format", "uchar-case-v0");
        c.put("asOfDate", null);
        c.put("input", Map.of("instances", List.of(instance), "parameters", Map.of()));
        c.put("expected", expected);
        return JSON.valueToTree(c);
    }

    /** The expectation of kind {@code kind} on the characteristic {@code name} of K1. */
    private static List<Map<String, String>> expected(String kind, String name) {
        return List.of(Map.of("kind", kind, "label", "K1", "characteristic", name));
    }

    /** A document without rules whose object type {@code Kenmerkproef} declares an adjectival characteristic for each
     *  name of {@link #NAMES}; with {@code inferFromEmpty: false} unless {@code ordinary}. */
    private static String document(boolean ordinary) throws Exception {
        List<Map<String, Object>> characteristics = new ArrayList<>();
        for (String n : NAMES) {
            Map<String, Object> k = new java.util.LinkedHashMap<>();
            k.put("node", "Characteristic");
            k.put("name", n);
            k.put("kind", "adjectival");
            characteristics.add(k);
        }
        Map<String, Object> type = new java.util.LinkedHashMap<>();
        type.put("node", "ObjectType");
        type.put("name", "Kenmerkproef");
        type.put("animate", false);
        type.put("plural", "Kenmerkproeven");
        type.put("attributes", List.of());
        type.put("characteristics", characteristics);
        Map<String, Object> model = new java.util.LinkedHashMap<>();
        model.put("objectTypes", List.of(type));
        for (String k : List.of("factTypes", "parameters", "domains", "unitSystems", "dimensions", "timelines",
                "dayKinds")) model.put(k, List.of());
        Map<String, Object> rules = new java.util.LinkedHashMap<>();
        for (String k : List.of("rules", "dayKindDefinitions", "objectCreations", "allocations", "consistencies",
                "uniquenesses", "factCreations", "startPointDeterminations", "ruleGroups")) rules.put(k, List.of());
        Map<String, Object> d = new java.util.LinkedHashMap<>();
        d.put("format", "uchar-ir-v1");
        if (!ordinary) d.put("inferFromEmpty", false);
        d.put("model", model);
        d.put("rules", rules);
        d.put("diagnostics", List.of());
        return JSON.writeValueAsString(d);
    }

    private static String name(Object o) {
        return o.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(o));
    }
}
