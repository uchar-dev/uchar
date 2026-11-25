package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.refusal.DroolsCode;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.core.Namen;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieSession;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An attribute the input states to have no value ({@code attributesEmpty}, RUNTIME-SEMANTICS ch. 1.5), including names
 * whose declared spelling is not its bean property as written, across scalar datatypes ({@code Boolean}, {@code Number},
 * {@code Text}), on both branches:
 * <ul>
 *   <li>in a document with {@code inferFromEmpty: false} an input attribute stated to have no value is settled empty
 *       (idempotent under repeated or differently spelled entries), and one the case does not name is open;</li>
 *   <li>in an ordinary document the statement changes nothing: every attribute the case gives no value is empty;</li>
 *   <li>an attribute the case gives a value in {@code attributes} and names in {@code attributesEmpty} is EF-048 with
 *       {@code label} and {@code attribute} as written in {@code attributesEmpty}, and an undeclared one is EF-040;</li>
 *   <li>a dimensioned attribute and a time-dependent attribute in {@code attributesEmpty} are refused at case load,
 *       dimensioned before time-dependent;</li>
 *   <li>{@link DroolsBackend#insert} takes {@link Input#attributeEmpty} in the declared spelling and in another spelling
 *       under {@link Namen#norm} (inherited attributes included), removes that attribute from what is open, and refuses
 *       every invalid call with DROOLS-375 ({@code not-in-input}, {@code undeclared}, {@code dimensioned},
 *       {@code time-dependent}, {@code filled});</li>
 *   <li>in a build with readings, both a standalone initialisation rule and a single-default initialisation stack come
 *       at their turn when their target is named in {@code attributesEmpty}, while leaving it out keeps the target open,
 *       with identical readings and end states on both branches.</li>
 * </ul>
 */
class EmptyAttributeInputWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private record NamedAttribute(String name, String datatypeKind, Object sampleJsonValue, Object sampleBeanValue,
                                  Class<?> setterType) {}

    /** Declared names whose bean member a getter derived from the name as written misses, paired with {@code Boolean},
     *  {@code Number} and {@code Text} datatypes (including zero/false/empty-text filled values). */
    private static final List<NamedAttribute> ATTRIBUTES = List.of(
            new NamedAttribute("op zee", "boolean", Map.of("boolean", false), Boolean.FALSE, Boolean.class),
            new NamedAttribute("niet-erkend", "number", Map.of("number", "0"), BigDecimal.ZERO, BigDecimal.class),
            new NamedAttribute("jutter's", "text", Map.of("text", ""), "", String.class),
            new NamedAttribute("EU", "boolean", Map.of("boolean", true), Boolean.TRUE, Boolean.class),
            new NamedAttribute("3e lid", "number", Map.of("number", "12"), new BigDecimal("12"), BigDecimal.class),
            new NamedAttribute("m²", "number", Map.of("number", "7"), new BigDecimal("7"), BigDecimal.class),
            new NamedAttribute("İş", "text", Map.of("text", "wrak"), "wrak", String.class));

    @Test void an_attribute_stated_empty_is_settled_empty_in_a_strict_document() throws Exception {
        assertEquals(List.of(), play(false, "attributeEmpty", "attributeOpen"));
    }

    @Test void an_attribute_stated_empty_changes_nothing_in_an_ordinary_document() throws Exception {
        assertEquals(List.of(), play(true, "attributeEmpty", "attributeEmpty"));
    }

    @Test void an_attribute_given_a_value_and_stated_empty_is_ef_048() throws Exception {
        List<String> faults = new ArrayList<>();
        for (boolean ordinary : new boolean[]{false, true}) {
            Path ir = Files.createTempFile("attribuutnamen", ".json"),
                    c = Files.createTempFile("attribuutnamen-case", ".json");
            try {
                Files.writeString(ir, document(ordinary, false));
                for (NamedAttribute attr : ATTRIBUTES)
                    for (String emptySpelling : List.of(attr.name(), otherSpelling(attr.name()))) {
                        Files.writeString(c, JSON.writeValueAsString(caseOf("Attribuutproef",
                                Map.of(attr.name(), attr.sampleJsonValue()), List.of(emptySpelling),
                                expected("attributeEmpty", attr.name()))));
                        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
                            CaseRunner.RedResult r = CaseRunner.runRed(ir, c, branch);
                            if (!"FAULT".equals(r.status()) || !"EF-048".equals(r.code())
                                    || !List.of(Map.of("label", "A1", "attribute", emptySpelling)).equals(r.params()))
                                faults.add(branch + (ordinary ? " ordinary " : " strict ") + attr.name() + " as «"
                                        + emptySpelling + "» | " + r.status() + " " + r.code() + " " + r.params() + ": "
                                        + r.detail());
                        }
                    }
                Files.writeString(c, JSON.writeValueAsString(caseOf("Attribuutproef", Map.of(), List.of("onbestaand"),
                        expected("attributeEmpty", "op zee"))));
                for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
                    CaseRunner.RedResult r = CaseRunner.runRed(ir, c, branch);
                    if (!"FAULT".equals(r.status()) || !"EF-040".equals(r.code())
                            || !List.of(Map.of("label", "A1", "name", "onbestaand")).equals(r.params()))
                        faults.add(branch + " undeclared | " + r.status() + " " + r.code() + " " + r.params());
                }
            } finally {
                Files.delete(ir);
                Files.delete(c);
            }
        }
        assertEquals(List.of(), faults);
    }

    @Test void dimensioned_and_time_dependent_attributes_are_refused_in_attributesEmpty() throws Exception {
        JsonNode ir = JSON.readTree(document(true, true));
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            DroolsBackend.Build build = DroolsBackend.build(ir, branch);
            for (String attr : List.of("seizoensgewicht", "beide")) {
                IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                        () -> CaseInput.of(build, caseOf("Attribuutproef", Map.of(), List.of(attr), List.of())));
                assertTrue(e.getMessage().contains("dimensioned") && e.getMessage().contains("attributesEmpty"),
                        () -> branch + " " + attr + ": " + e.getMessage());
            }
            IllegalArgumentException temporal = assertThrows(IllegalArgumentException.class,
                    () -> CaseInput.of(build, caseOf("Attribuutproef", Map.of(), List.of("maandbedrag"), List.of())));
            assertTrue(temporal.getMessage().contains("time-dependent")
                    && temporal.getMessage().contains("attributesEmpty"), () -> branch + ": " + temporal.getMessage());
        }
    }

    @Test void insert_takes_an_attribute_stated_empty_in_any_spelling_and_refuses_invalid_calls() throws Exception {
        List<String> faults = new ArrayList<>();
        List<String> allNames = ATTRIBUTES.stream().map(NamedAttribute::name).toList();
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            DroolsBackend.Build strict = DroolsBackend.build(JSON.readTree(document(false, false)), branch);
            Class<?> trial = Class.forName(FactModelGenerator.PACKAGE + ".Attribuutproef", true, strict.beanLoader());
            Class<?> sub = Class.forName(FactModelGenerator.PACKAGE + ".Strandproef", true, strict.beanLoader());
            TmsRepeatedFireWitnessTest.inSession(strict, () -> {
                for (Class<?> cls : List.of(trial, sub))
                    for (NamedAttribute attr : ATTRIBUTES)
                        for (String spelling : List.of(attr.name(), otherSpelling(attr.name()))) {
                            String what = branch + " " + cls.getSimpleName() + " " + attr.name() + " as «"
                                    + spelling + "»";
                            List<String> expectedOpen = new ArrayList<>(allNames);
                            expectedOpen.remove(attr.name());
                            try {
                                List<String> found = openAfter(strict, cls.getConstructor().newInstance(), spelling);
                                if (!expectedOpen.equals(found))
                                    faults.add(what + " | open " + found + ", expected " + expectedOpen);
                            } catch (RuntimeException e) {
                                faults.add(what + " | " + e);
                            }
                            Object filled = cls.getConstructor().newInstance();
                            cls.getMethod(Namen.setter(attr.name()), attr.setterType())
                                    .invoke(filled, attr.sampleBeanValue());
                            String refusal = DroolsCode.INSERT_EMPTY_ATTRIBUTE.reason("bean", beanId(filled),
                                    "attribute", spelling, "reason",
                                    DroolsCode.INSERT_EMPTY_ATTRIBUTE.part("filled")).text();
                            expectInsertRefusal(strict, new Input().bean(filled).attributeEmpty(filled, spelling),
                                    refusal, what, faults);
                        }
                Object inInput = trial.getConstructor().newInstance();
                Object outsider = trial.getConstructor().newInstance();
                expectInsertRefusal(strict, new Input().bean(inInput).attributeEmpty(outsider, "op zee"),
                        DroolsCode.INSERT_EMPTY_ATTRIBUTE.reason("bean", beanId(outsider), "attribute", "op zee",
                                "reason", DroolsCode.INSERT_EMPTY_ATTRIBUTE.part("not-in-input")).text(),
                        branch + " not-in-input", faults);
                expectInsertRefusal(strict, new Input().bean(inInput).attributeEmpty(inInput, "onbestaand"),
                        DroolsCode.INSERT_EMPTY_ATTRIBUTE.reason("bean", beanId(inInput), "attribute", "onbestaand",
                                "reason", DroolsCode.INSERT_EMPTY_ATTRIBUTE.part("undeclared", "objectType",
                                        "Attribuutproef")).text(),
                        branch + " undeclared", faults);
            });
            DroolsBackend.Build withFacets = DroolsBackend.build(JSON.readTree(document(true, true)), branch);
            Class<?> facetBeanClass = Class.forName(FactModelGenerator.PACKAGE + ".Attribuutproef", true,
                    withFacets.beanLoader());
            TmsRepeatedFireWitnessTest.inSession(withFacets, () -> {
                Object b = facetBeanClass.getConstructor().newInstance();
                expectInsertRefusal(withFacets, new Input().bean(b).attributeEmpty(b, "seizoensgewicht"),
                        DroolsCode.INSERT_EMPTY_ATTRIBUTE.reason("bean", beanId(b), "attribute", "seizoensgewicht",
                                "reason", DroolsCode.INSERT_EMPTY_ATTRIBUTE.part("dimensioned", "objectType",
                                        "Attribuutproef")).text(),
                        branch + " dimensioned", faults);
                expectInsertRefusal(withFacets, new Input().bean(b).attributeEmpty(b, "maandbedrag"),
                        DroolsCode.INSERT_EMPTY_ATTRIBUTE.reason("bean", beanId(b), "attribute", "maandbedrag",
                                "reason", DroolsCode.INSERT_EMPTY_ATTRIBUTE.part("time-dependent", "objectType",
                                        "Attribuutproef")).text(),
                        branch + " time-dependent", faults);
            });
        }
        assertEquals(List.of(), faults);
    }

    @Test void readings_and_end_state_cover_initialisation_rule_and_single_default_initialisation_stack()
            throws Exception {
        Path ir = Files.createTempFile("init-empty", ".json"),
                omitted = Files.createTempFile("init-omitted", ".json"),
                statedHolds = Files.createTempFile("init-holds", ".json"),
                statedFails = Files.createTempFile("init-fails", ".json");
        try {
            Files.writeString(ir, initialisationDocument());
            Files.writeString(omitted, JSON.writeValueAsString(caseOf("Vondst",
                    Map.of("vangst", Map.of("number", "4")), List.of(),
                    List.of(Map.of("kind", "attributeOpen", "label", "A1", "attribute", "strandloon"),
                            Map.of("kind", "attributeOpen", "label", "A1", "attribute", "bergloon")))));
            Files.writeString(statedHolds, JSON.writeValueAsString(caseOf("Vondst",
                    Map.of("vangst", Map.of("number", "4")), List.of("strandloon", "bergloon"),
                    List.of(expectNumber("strandloon", "5"), expectNumber("bergloon", "7")))));
            Files.writeString(statedFails, JSON.writeValueAsString(caseOf("Vondst",
                    Map.of("vangst", Map.of("number", "0")), List.of("strandloon", "bergloon"),
                    List.of(Map.of("kind", "attributeEmpty", "label", "A1", "attribute", "strandloon"),
                            expectNumber("bergloon", "7")))));

            List<Path> cases = List.of(omitted, statedHolds, statedFails);
            for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
                CaseRunner.Result res = branch == DroolsBackend.Branch.EXEC_MODEL
                        ? CaseRunner.viaExecModel(ir, cases) : CaseRunner.viaDrlText(ir, cases);
                assertEquals("PLAYED 3", res.status() + " " + res.casesPlayed(), () -> branch + ": " + res);
            }
            List<String> execOmitted = CaseRunner.readings(ir, List.of(omitted), DroolsBackend.Branch.EXEC_MODEL);
            List<String> drlOmitted = CaseRunner.readings(ir, List.of(omitted), DroolsBackend.Branch.DRL_TEXT);
            assertEquals(execOmitted, drlOmitted);
            assertTrue(execOmitted.contains("state attribute \"A1\" \"strandloon\" open"), execOmitted::toString);
            assertTrue(execOmitted.contains("state attribute \"A1\" \"bergloon\" open"), execOmitted::toString);

            List<String> execHolds = CaseRunner.readings(ir, List.of(statedHolds), DroolsBackend.Branch.EXEC_MODEL);
            List<String> drlHolds = CaseRunner.readings(ir, List.of(statedHolds), DroolsBackend.Branch.DRL_TEXT);
            assertEquals(execHolds, drlHolds);
            for (String expectedLine : List.of(
                    "reading /rules/rules/0/versions/0 \"A1\" 0 Comparison true",
                    "reading /rules/rules/0/versions/0 \"A1\" HOLDS",
                    "reading /rules/prioritizedDefaults/0/defaults/0 \"A1\" 0 Literal 7",
                    "reading /rules/prioritizedDefaults/0/defaults/0 \"A1\" HOLDS",
                    "state attribute \"A1\" \"strandloon\" 5",
                    "state attribute \"A1\" \"bergloon\" 7"))
                assertTrue(execHolds.contains(expectedLine), () -> expectedLine + " in " + execHolds);

            List<String> execFails = CaseRunner.readings(ir, List.of(statedFails), DroolsBackend.Branch.EXEC_MODEL);
            List<String> drlFails = CaseRunner.readings(ir, List.of(statedFails), DroolsBackend.Branch.DRL_TEXT);
            assertEquals(execFails, drlFails);
            for (String expectedLine : List.of(
                    "reading /rules/rules/0/versions/0 \"A1\" 0 Comparison false",
                    "reading /rules/rules/0/versions/0 \"A1\" FAILS",
                    "reading /rules/prioritizedDefaults/0/defaults/0 \"A1\" HOLDS",
                    "state attribute \"A1\" \"bergloon\" 7"))
                assertTrue(execFails.contains(expectedLine), () -> expectedLine + " in " + execFails);
            assertTrue(execFails.stream().noneMatch(l -> l.contains("\"strandloon\"")), execFails::toString);
        } finally {
            Files.deleteIfExists(ir);
            Files.deleteIfExists(omitted);
            Files.deleteIfExists(statedHolds);
            Files.deleteIfExists(statedFails);
        }
    }

    private static Map<String, Object> expectNumber(String attr, String n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("kind", "attributeValue");
        m.put("label", "A1");
        m.put("attribute", attr);
        m.put("value", Map.of("number", n));
        return m;
    }

    private static void expectInsertRefusal(DroolsBackend.Build b, Input input, String expectedMessage, String what,
                                            List<String> faults) {
        KieSession session = b.kieBase().newKieSession();
        try {
            DroolsBackend.insert(b, session, input);
            faults.add(what + " | insert accepted an invalid Input.attributeEmpty");
        } catch (IllegalArgumentException e) {
            if (!expectedMessage.equals(e.getMessage()))
                faults.add(what + " | got «" + e.getMessage() + "», expected «" + expectedMessage + "»");
        } catch (RuntimeException e) {
            faults.add(what + " | " + e);
        } finally {
            session.dispose();
        }
    }

    private static List<String> openAfter(DroolsBackend.Build b, Object bean, String spelling) {
        KieSession session = b.kieBase().newKieSession();
        try {
            DroolsBackend.insert(b, session, new Input().bean(bean).attributeEmpty(bean, spelling));
            assertNull(DroolsBackend.fire(b, session).noFixpoint(), "the fixpoint");
            List<String> open = new ArrayList<>();
            for (EndState.Open o : DroolsBackend.endStateOf(b, session, x -> "A1").objects().get(0).open())
                if (!o.characteristic()) open.add(o.name());
            return open;
        } finally {
            session.dispose();
        }
    }

    private static String otherSpelling(String name) {
        StringBuilder out = new StringBuilder(" ");
        for (char c : name.toCharArray())
            out.append(c == ' ' ? "  " : c >= 'a' && c <= 'z' ? String.valueOf((char) (c - 32))
                    : c >= 'A' && c <= 'Z' ? String.valueOf((char) (c + 32)) : String.valueOf(c));
        return out.toString();
    }

    private static List<String> play(boolean ordinary, String stated, String unnamed) throws Exception {
        List<String> faults = new ArrayList<>();
        Path file = Files.createTempFile("attribuutnamen", ".json");
        try {
            Files.writeString(file, document(ordinary, false));
            for (NamedAttribute attr : ATTRIBUTES) {
                List<JsonNode> cases = List.of(
                        caseOf("Attribuutproef", Map.of(), List.of(attr.name()), expected(stated, attr.name())),
                        caseOf("Attribuutproef", Map.of(), List.of(attr.name(), otherSpelling(attr.name())),
                                expected(stated, attr.name())),
                        caseOf("Attribuutproef", Map.of(), List.of(), expected(unnamed, attr.name())));
                for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
                    try {
                        List<String> verdicts = CaseRunner.playCases(file, cases, branch, false);
                        for (int i = 0; i < verdicts.size(); i++)
                            if (verdicts.get(i) != null)
                                faults.add(branch + " " + attr.name() + " case " + (i + 1) + " | " + verdicts.get(i));
                    } catch (RuntimeException e) {
                        faults.add(branch + " " + attr.name() + " | " + e);
                    }
                }
            }
        } finally {
            Files.delete(file);
        }
        return faults;
    }

    private static JsonNode caseOf(String objectType, Map<String, Object> attrs, List<String> empty,
                                   List<Map<String, Object>> expected) {
        Map<String, Object> instance = new LinkedHashMap<>();
        instance.put("label", "A1");
        instance.put("objectType", objectType);
        instance.put("attributes", attrs);
        if (!empty.isEmpty()) instance.put("attributesEmpty", empty);
        instance.put("roles", Map.of());
        instance.put("characteristics", List.of());
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("format", "uchar-case-v0");
        c.put("asOfDate", null);
        c.put("input", Map.of("instances", List.of(instance), "parameters", Map.of()));
        c.put("expected", expected);
        return JSON.valueToTree(c);
    }

    private static List<Map<String, Object>> expected(String kind, String name) {
        return List.of(Map.of("kind", kind, "label", "A1", "attribute", name));
    }

    private static Map<String, Object> datatypeNode(String kind) {
        return switch (kind) {
            case "boolean" -> Map.of("node", "Boolean");
            case "text" -> Map.of("node", "Text");
            default -> {
                Map<String, Object> n = new LinkedHashMap<>();
                n.put("node", "Number");
                n.put("whole", true);
                n.put("decimals", 0);
                n.put("sign", "unspecified");
                yield n;
            }
        };
    }

    private static String document(boolean ordinary, boolean withFacets) throws Exception {
        List<Map<String, Object>> attributes = new ArrayList<>();
        for (NamedAttribute attr : ATTRIBUTES) {
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("node", "Attribute");
            a.put("name", attr.name());
            a.put("datatype", datatypeNode(attr.datatypeKind()));
            attributes.add(a);
        }
        if (withFacets) {
            Map<String, Object> dim = new LinkedHashMap<>();
            dim.put("node", "Attribute");
            dim.put("name", "seizoensgewicht");
            dim.put("datatype", datatypeNode("number"));
            dim.put("dimensions", List.of("seizoen"));
            attributes.add(dim);

            Map<String, Object> tl = new LinkedHashMap<>();
            tl.put("node", "Attribute");
            tl.put("name", "maandbedrag");
            tl.put("datatype", datatypeNode("number"));
            tl.put("timeline", Map.of("node", "TimelineFacet", "granularity", "month", "count", 1));
            attributes.add(tl);

            Map<String, Object> both = new LinkedHashMap<>();
            both.put("node", "Attribute");
            both.put("name", "beide");
            both.put("datatype", datatypeNode("number"));
            both.put("dimensions", List.of("seizoen"));
            both.put("timeline", Map.of("node", "TimelineFacet", "granularity", "month", "count", 1));
            attributes.add(both);
        }
        Map<String, Object> baseType = new LinkedHashMap<>();
        baseType.put("node", "ObjectType");
        baseType.put("name", "Attribuutproef");
        baseType.put("animate", false);
        baseType.put("plural", "Attribuutproeven");
        baseType.put("attributes", attributes);
        baseType.put("characteristics", List.of());

        Map<String, Object> subType = new LinkedHashMap<>();
        subType.put("node", "ObjectType");
        subType.put("name", "Strandproef");
        subType.put("supertype", "Attribuutproef");
        subType.put("animate", false);
        subType.put("plural", "Strandproeven");
        subType.put("attributes", List.of());
        subType.put("characteristics", List.of());

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("objectTypes", List.of(baseType, subType));
        for (String k : List.of("factTypes", "parameters", "domains", "unitSystems")) model.put(k, List.of());
        model.put("dimensions", withFacets ? List.of(Map.of("node", "Dimension", "name", "seizoen",
                "plural", "seizoenen", "preposition", "in", "labels", List.of("voorjaar", "najaar"))) : List.of());
        for (String k : List.of("timelines", "dayKinds")) model.put(k, List.of());

        Map<String, Object> rules = new LinkedHashMap<>();
        for (String k : List.of("rules", "dayKindDefinitions", "objectCreations", "allocations", "consistencies",
                "uniquenesses", "factCreations", "startPointDeterminations", "ruleGroups")) rules.put(k, List.of());

        Map<String, Object> d = new LinkedHashMap<>();
        d.put("format", "uchar-ir-v1");
        if (!ordinary) d.put("inferFromEmpty", false);
        d.put("model", model);
        d.put("rules", rules);
        d.put("diagnostics", List.of());
        return JSON.writeValueAsString(d);
    }

    /** A strict document with a conditional initialisation rule on {@code strandloon} and a single-default
     *  initialisation stack (no exception group) on {@code bergloon}. */
    private static String initialisationDocument() {
        return """
                {
                  "format": "uchar-ir-v1",
                  "inferFromEmpty": false,
                  "model": {
                    "objectTypes": [
                      {
                        "node": "ObjectType",
                        "name": "Vondst",
                        "animate": false,
                        "plural": "Vondsten",
                        "attributes": [
                          {
                            "node": "Attribute",
                            "name": "vangst",
                            "datatype": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" }
                          },
                          {
                            "node": "Attribute",
                            "name": "strandloon",
                            "datatype": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" }
                          },
                          {
                            "node": "Attribute",
                            "name": "bergloon",
                            "datatype": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" }
                          }
                        ],
                        "characteristics": []
                      }
                    ],
                    "factTypes": [],
                    "parameters": [],
                    "domains": [],
                    "unitSystems": [],
                    "dimensions": [],
                    "timelines": [],
                    "dayKinds": []
                  },
                  "rules": {
                    "rules": [
                      {
                        "node": "Rule",
                        "name": "standaard strandloon",
                        "versions": [
                          {
                            "validity": { "node": "Validity" },
                            "target": {
                              "node": "AttributeReference",
                              "attribute": "strandloon",
                              "subjectType": "Vondst",
                              "source": { "snippet": "Het strandloon van een Vondst" }
                            },
                            "value": {
                              "node": "Literal",
                              "value": { "number": "5" },
                              "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" },
                              "source": { "snippet": "5" }
                            },
                            "condition": {
                              "node": "Comparison",
                              "op": "greater",
                              "left": {
                                "node": "AttributeReference",
                                "attribute": "vangst",
                                "subjectType": "Vondst",
                                "source": { "snippet": "zijn vangst" }
                              },
                              "right": {
                                "node": "Literal",
                                "value": { "number": "1" },
                                "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" },
                                "source": { "snippet": "1" }
                              },
                              "source": { "snippet": "zijn vangst groter is dan 1" }
                            },
                            "initialisation": true
                          }
                        ]
                      }
                    ],
                    "dayKindDefinitions": [],
                    "objectCreations": [],
                    "allocations": [],
                    "consistencies": [],
                    "uniquenesses": [],
                    "factCreations": [],
                    "startPointDeterminations": [],
                    "ruleGroups": [],
                    "prioritizedDefaults": [
                      {
                        "node": "PrioritizedDefaults",
                        "name": "standaard bergloon",
                        "subject": "Vondst",
                        "attribute": "bergloon",
                        "defaults": [
                          {
                            "node": "Default",
                            "name": "aanvankelijk bergloon",
                            "consequence": {
                              "node": "Literal",
                              "value": { "number": "7" },
                              "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" },
                              "source": { "snippet": "7" }
                            },
                            "initialisation": true,
                            "source": { "snippet": "aanvankelijk 7" }
                          }
                        ],
                        "validity": { "node": "Validity" }
                      }
                    ]
                  },
                  "diagnostics": []
                }
                """;
    }

    private static String beanId(Object o) {
        return o.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(o));
    }
}
