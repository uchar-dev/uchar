package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.refusal.DroolsCode;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.core.Namen;
import dev.uchar.regelspraak.transpiler.tms.Fixpoint;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.kie.api.runtime.KieSession;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Non-reciprocal to-many roles under {@code inferFromEmpty: false} (RUNTIME-SEMANTICS ch. 1.5, 5.7, 5.8a, 5.12a):
 * <ul>
 *   <li>{@code rolesClosed} validation in {@link CaseInput} (EF-040 undeclared, 0-opposite refusal, EF-001 ambiguous,
 *       refusal on {@code many: false} and {@code reciprocal: true}, idempotent on duplicates and valid alongside
 *       supplied links without EF-049);</li>
 *   <li>{@link Input#roleClosed} validation in {@link DroolsBackend#insert} ({@code DROOLS-377}: {@code not-in-input},
 *       {@code undeclared}, {@code ambiguous}, {@code not-to-many}) in both EN and NL locales, and open {@code OpenInput}
 *       retention on unclosed to-many roles even when {@code >= 1} links are present;</li>
 *   <li>the universal range law across {@code RoleMembership} (including {@code counterpartType} subtype narrowing),
 *       {@code Aggregation} / {@code InstanceSetUnion}, and {@code QuantifierFold} (monotone short-circuiting,
 *       provisional matrix faults and deposits, and buffered filter deposits inside {@code Compound}) on both
 *       {@link DroolsBackend.Branch#EXEC_MODEL} and {@link DroolsBackend.Branch#DRL_TEXT};</li>
 *   <li>replay of both corpus goldens {@code jutterswet-invoer-collectie-strikt} and {@code jutterswet-invoer-collectie}
 *       on both branches.</li>
 * </ul>
 */
class ClosedRoleInputWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    @Test void corpus_collection_goldens_play_on_both_branches() throws Exception {
        for (String name : List.of("jutterswet-invoer-collectie-strikt", "jutterswet-invoer-collectie")) {
            Path dir = GOLDEN.resolve(name), ir = dir.resolve("ir.json");
            List<Path> cases;
            try (Stream<Path> s = Files.list(dir)) {
                cases = s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList();
            }
            CaseRunner.Result exec = CaseRunner.viaExecModel(ir, cases);
            CaseRunner.Result drl = CaseRunner.viaDrlText(ir, cases);
            assertEquals("PLAYED " + cases.size(), exec.status() + " " + exec.casesPlayed(), () -> name + " exec: " + exec);
            assertEquals("PLAYED " + cases.size(), drl.status() + " " + drl.casesPlayed(), () -> name + " drl: " + drl);
        }
    }

    @Test void rolesClosed_validates_like_ref_and_coexists_with_links(@TempDir Path dir) throws Exception {
        Path ir = dir.resolve("ir.json");
        Files.writeString(ir, validationDocument());
        JsonNode irNode = JSON.readTree(Files.readString(ir));

        // 1. undeclared in model -> EF-040
        Path undeclaredCase = dir.resolve("undeclared.json");
        Files.writeString(undeclaredCase, caseWithRolesClosed("J1", "Jutter", Map.of(), List.of("onbestaand"), true));
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            CaseRunner.RedResult r = CaseRunner.runRed(ir, undeclaredCase, branch);
            assertEquals("FAULT", r.status(), branch::name);
            assertEquals("EF-040", r.code(), branch::name);
            assertEquals(List.of(Map.of("label", "J1", "name", "onbestaand")), r.params(), branch::name);
        }

        // 2. ambiguous across declared fact types -> EF-001
        Path ambiguousCase = dir.resolve("ambiguous.json");
        Files.writeString(ambiguousCase, caseWithRolesClosed("J1", "Jutter", Map.of(), List.of("meldingen"), true));
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            CaseRunner.RedResult r = CaseRunner.runRed(ir, ambiguousCase, branch);
            assertEquals("FAULT", r.status(), branch::name);
            assertEquals("EF-001", r.code(), branch::name);
            assertEquals(List.of(Map.of("roleKey", "meldingen", "objectType", "Jutter")), r.params(), branch::name);
        }

        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            DroolsBackend.Build build = DroolsBackend.build(irNode, branch);

            // 3. 0 opposite hits on Jutter ("vreemde posten" is on Derde <-> Vreemd) -> refusal without EF code
            IllegalArgumentException zeroHits = assertThrows(IllegalArgumentException.class,
                    () -> CaseInput.of(build, JSON.readTree(
                            caseWithRolesClosed("J1", "Jutter", Map.of(), List.of("vreemde posten"), false))));
            assertTrue(!(zeroHits instanceof CaseInput.NameFault) && !(zeroHits instanceof CaseInput.AmbiguousRoleFault)
                    && zeroHits.getMessage().contains("rolesClosed"), () -> branch + ": " + zeroHits);

            // 4. to-one role ("vinder" on Vondst) -> refusal without EF code
            IllegalArgumentException toOne = assertThrows(IllegalArgumentException.class,
                    () -> CaseInput.of(build, JSON.readTree(
                            caseWithRolesClosed("V1", "Vondst", Map.of(), List.of("vinder"), false))));
            assertTrue(toOne.getMessage().contains("non-reciprocal to-many") && toOne.getMessage().contains("rolesClosed"),
                    () -> branch + ": " + toOne);

            // 5. reciprocal fact type ("maat" on Jutter) -> refusal without EF code
            IllegalArgumentException reciprocal = assertThrows(IllegalArgumentException.class,
                    () -> CaseInput.of(build, JSON.readTree(
                            caseWithRolesClosed("J1", "Jutter", Map.of(), List.of("maat"), false))));
            assertTrue(reciprocal.getMessage().contains("non-reciprocal to-many")
                    && reciprocal.getMessage().contains("rolesClosed"), () -> branch + ": " + reciprocal);

            // 6. duplicates (singular + plural + spacing/case) AND supplied links on the same role succeed (no EF-049!)
            JsonNode validWithLinks = JSON.readTree("""
                    {
                      "format": "uchar-case-v0",
                      "asOfDate": null,
                      "input": {
                        "instances": [
                          { "label": "J1", "objectType": "Jutter", "attributes": {},
                            "roles": { "vondsten": ["V1"] },
                            "rolesClosed": ["vondst", "vondsten", "  VONDSTEN ", "bergingen"],
                            "characteristics": [] },
                          { "label": "V1", "objectType": "Vondst", "attributes": {}, "roles": {}, "characteristics": [] },
                          { "label": "V2", "objectType": "Vondst", "attributes": {}, "roles": {}, "characteristics": [] }
                        ],
                        "parameters": {}
                      },
                      "expected": [
                        { "kind": "attributeValue", "label": "J1", "attribute": "vondstaantal", "value": { "number": "1" } },
                        { "kind": "characteristicPresent", "label": "V1", "characteristic": "toegewezen" },
                        { "kind": "characteristicOpen", "label": "V2", "characteristic": "toegewezen" },
                        { "kind": "consistent" }
                      ]
                    }
                    """);
            assertNull(CaseRunner.playCase(ir, validWithLinks, branch), branch::name);
        }
    }

    @Test void insert_validates_roleClosed_with_drools_377_and_keeps_unclosed_collections_open() throws Exception {
        JsonNode irNode = JSON.readTree(validationDocument());
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            DroolsBackend.Build build = DroolsBackend.build(irNode, branch);
            Class<?> jutterCls = Class.forName(FactModelGenerator.PACKAGE + ".Jutter", true, build.beanLoader());
            Class<?> subJutterCls = Class.forName(FactModelGenerator.PACKAGE + ".Strandjutter", true, build.beanLoader());
            Class<?> vondstCls = Class.forName(FactModelGenerator.PACKAGE + ".Vondst", true, build.beanLoader());

            TmsRepeatedFireWitnessTest.inSession(build, () -> {
                Object j1 = jutterCls.getConstructor().newInstance();
                Object outsider = jutterCls.getConstructor().newInstance();
                Object v1 = vondstCls.getConstructor().newInstance();

                // DROOLS-377 refusals in English (default) and Dutch locales
                expectClosedRefusal(build, new Input().bean(j1).roleClosed(outsider, "vondsten"),
                        DroolsCode.INSERT_CLOSED_ROLE.reason("bean", beanId(outsider), "role", "vondsten",
                                "reason", DroolsCode.INSERT_CLOSED_ROLE.part("not-in-input")),
                        branch + " not-in-input");
                expectClosedRefusal(build, new Input().bean(j1).roleClosed(j1, "onbestaand"),
                        DroolsCode.INSERT_CLOSED_ROLE.reason("bean", beanId(j1), "role", "onbestaand",
                                "reason", DroolsCode.INSERT_CLOSED_ROLE.part("undeclared", "objectType", "Jutter")),
                        branch + " undeclared");
                expectClosedRefusal(build, new Input().bean(j1).roleClosed(j1, "vreemde posten"),
                        DroolsCode.INSERT_CLOSED_ROLE.reason("bean", beanId(j1), "role", "vreemde posten",
                                "reason", DroolsCode.INSERT_CLOSED_ROLE.part("undeclared", "objectType", "Jutter")),
                        branch + " zero-opposite");
                expectClosedRefusal(build, new Input().bean(j1).roleClosed(j1, "meldingen"),
                        DroolsCode.INSERT_CLOSED_ROLE.reason("bean", beanId(j1), "role", "meldingen",
                                "reason", DroolsCode.INSERT_CLOSED_ROLE.part("ambiguous", "objectType", "Jutter")),
                        branch + " ambiguous");
                expectClosedRefusal(build, new Input().bean(v1).roleClosed(v1, "vinder"),
                        DroolsCode.INSERT_CLOSED_ROLE.reason("bean", beanId(v1), "role", "vinder",
                                "reason", DroolsCode.INSERT_CLOSED_ROLE.part("not-to-many", "objectType", "Vondst")),
                        branch + " to-one");
                expectClosedRefusal(build, new Input().bean(j1).roleClosed(j1, "maat"),
                        DroolsCode.INSERT_CLOSED_ROLE.reason("bean", beanId(j1), "role", "maat",
                                "reason", DroolsCode.INSERT_CLOSED_ROLE.part("not-to-many", "objectType", "Jutter")),
                        branch + " reciprocal");

                // Unclosed to-many roles (1:n and n:m) stay open on Strandjutter EVEN WHEN links are present!
                Object sj = subJutterCls.getConstructor().newInstance();
                Object linkedVondst = vondstCls.getConstructor().newInstance();
                vondstCls.getMethod(Namen.setter("vinder"), jutterCls).invoke(linkedVondst, sj);

                KieSession openSession = build.kieBase().newKieSession();
                try {
                    DroolsBackend.insert(build, openSession, new Input().bean(sj).bean(linkedVondst)
                            .link("berging", sj, linkedVondst));
                    assertNull(DroolsBackend.fire(build, openSession).noFixpoint());
                    List<Fixpoint.OpenValue> openRoles = DroolsBackend.fixpointOf(build, openSession).open().stream()
                            .filter(o -> o.role() && o.object() == sj).toList();
                    assertTrue(openRoles.contains(Fixpoint.OpenValue.role(sj, "vondst", "vinding", 1)),
                            () -> branch + ": " + openRoles);
                    assertTrue(openRoles.contains(Fixpoint.OpenValue.role(sj, "berging", "berging", 1)),
                            () -> branch + ": " + openRoles);
                    EndState end = DroolsBackend.endStateOf(build, openSession, Map.of(sj, "SJ1", linkedVondst, "V1")::get);
                    assertTrue(end.links().stream().anyMatch(l -> l.factType().equals("vinding") && l.first() == sj && l.second() == linkedVondst));
                    assertTrue(end.openLinks().contains(new EndState.OpenLink("vinding", "vinder", sj, "vondst", null)));
                    assertTrue(end.openLinks().contains(new EndState.OpenLink("berging", "berger", sj, "berging", null)));
                } finally {
                    openSession.dispose();
                }

                // Closing via Input.roleClosed (with singular, plural, and duplicate calls) alongside existing links closes them
                KieSession closedSession = build.kieBase().newKieSession();
                try {
                    DroolsBackend.insert(build, closedSession, new Input().bean(sj).bean(linkedVondst)
                            .link("berging", sj, linkedVondst)
                            .roleClosed(sj, "vondst").roleClosed(sj, " VONDSTEN ")
                            .roleClosed(sj, "bergingen")
                            .roleClosed(sj, "eerste melding").roleClosed(sj, "tweede melding"));
                    assertNull(DroolsBackend.fire(build, closedSession).noFixpoint());
                    List<Fixpoint.OpenValue> openRoles = DroolsBackend.fixpointOf(build, closedSession).open().stream()
                            .filter(o -> o.role() && o.object() == sj).toList();
                    assertEquals(List.of(), openRoles, branch::name);
                    assertEquals(new java.math.BigDecimal("1"),
                            DroolsBackend.fixpointOf(build, closedSession).values().stream()
                                    .filter(v -> v.object() == sj && v.attribute().equals("vondstaantal"))
                                    .findFirst().orElseThrow().value());
                } finally {
                    closedSession.dispose();
                }
            });
        }
    }

    @Test void subtype_counterpart_role_check_and_quantifier_deposit_buffering_match_ref(@TempDir Path dir)
            throws Exception {
        Path ir = dir.resolve("ir.json");
        Files.writeString(ir, rangeSemanticsDocument());
        List<JsonNode> cases = List.of(
                // Case 1: J1 has plain Vondst V1 (tarief=0, vrijgesteld absent, asOfDate=null) on OPEN vondsten.
                // - heeft vondst -> true (V1 settles existence!)
                // - heeft zware vondst (counterpartType ZwareVondst) -> OPEN (0 ZwareVondsten linked and vondsten is open!)
                // - unie aantal -> OPEN (even though branch 1 filter would fault EF-014 on evaluationYear, range open breaks off first!)
                // - alle veilig (matrix evaluationYear > 2025 faults EF-014 on V1) -> provisional under open range -> OPEN, no session fault!
                // - alle gedekt (matrix deler / tarief > 0 deposits EF-005 on V1, yields false -> `atLeast 1` stays OPEN!) -> provisional under open range -> OPEN, no channel deposit!
                // - samengesteld met filterdeponering (Compound atLeast 1: [QF(filter deposits EF-005 on V1, matrix true -> QF stays OPEN), true]):
                //   Compound is true, and QF's buffered filter deposit EF-005 is DROPPED because QF stayed Undetermined!
                JSON.readTree("""
                        {
                          "format": "uchar-case-v0",
                          "asOfDate": null,
                          "input": {
                            "instances": [
                              { "label": "J1", "objectType": "Jutter", "attributes": {},
                                "roles": { "vondsten": ["V1"] }, "characteristics": [] },
                              { "label": "V1", "objectType": "Vondst",
                                "attributes": { "deler": { "number": "10" }, "tarief": { "number": "0" } },
                                "roles": {}, "characteristics": ["actief"],
                                "characteristicsAbsent": ["vrijgesteld"] }
                            ],
                            "parameters": {}
                          },
                          "expected": [
                            { "kind": "characteristicPresent", "label": "J1", "characteristic": "heeft vondst" },
                            { "kind": "characteristicOpen", "label": "J1", "characteristic": "heeft zware vondst" },
                            { "kind": "attributeOpen", "label": "J1", "attribute": "unie aantal" },
                            { "kind": "characteristicOpen", "label": "J1", "characteristic": "alle veilig" },
                            { "kind": "characteristicOpen", "label": "J1", "characteristic": "een gedekt" },
                            { "kind": "characteristicPresent", "label": "J1", "characteristic": "samengesteld met filterdeponering" },
                            { "kind": "consistent" }
                          ]
                        }
                        """),
                // Case 2: V1 has actief=false (characteristicsAbsent), while filter still deposits EF-005 (deler / tarief > 0 || true).
                // Under open range, `all` short-circuits to Established FALSE on V1, so QF's buffered filter deposit EF-005 IS
                // flushed to the rule's probe, and the second branch makes the rule decide true WITH channel deposit EF-005!
                // Meanwhile linking ZwareVondst ZV1 settles `heeft zware vondst` to true!
                JSON.readTree("""
                        {
                          "format": "uchar-case-v0",
                          "asOfDate": null,
                          "input": {
                            "instances": [
                              { "label": "J1", "objectType": "Jutter", "attributes": {},
                                "roles": { "vondsten": ["V1", "ZV1"] }, "characteristics": [] },
                              { "label": "V1", "objectType": "Vondst",
                                "attributes": { "deler": { "number": "10" }, "tarief": { "number": "0" } },
                                "roles": {}, "characteristics": ["vrijgesteld"], "characteristicsAbsent": ["actief"] },
                              { "label": "ZV1", "objectType": "ZwareVondst",
                                "attributes": { "deler": { "number": "10" }, "tarief": { "number": "5" } },
                                "roles": {}, "characteristics": ["actief", "vrijgesteld"] }
                            ],
                            "parameters": {}
                          },
                          "expected": [
                            { "kind": "characteristicPresent", "label": "J1", "characteristic": "heeft vondst" },
                            { "kind": "characteristicPresent", "label": "J1", "characteristic": "heeft zware vondst" },
                            { "kind": "characteristicOpen", "label": "J1", "characteristic": "alle veilig" },
                            { "kind": "characteristicPresent", "label": "J1", "characteristic": "een gedekt" },
                            { "kind": "characteristicPresent", "label": "J1", "characteristic": "samengesteld met filterdeponering" },
                            { "kind": "inconsistent", "rule": "samengesteld met filterdeponering" }
                          ]
                        }
                        """));
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            List<String> verdicts = CaseRunner.playCases(ir, cases, branch, false);
            assertEquals(List.of(), verdicts.stream().filter(java.util.Objects::nonNull).toList(),
                    () -> branch + ": " + verdicts);
        }
    }

    private static void expectClosedRefusal(DroolsBackend.Build build, Input input,
                                            dev.uchar.regelspraak.refusal.Reason expected, String what) {
        KieSession session = build.kieBase().newKieSession();
        try {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> DroolsBackend.insert(build, session, input), what);
            assertEquals(expected.text(), ex.getMessage(), what);
            assertTrue(expected.text(Locale.ENGLISH).startsWith("insert: the input states that the role "),
                    () -> what + " en: " + expected.text(Locale.ENGLISH));
            assertTrue(expected.text(Locale.forLanguageTag("nl")).startsWith("insert: de invoer zegt dat de rol "),
                    () -> what + " nl: " + expected.text(Locale.forLanguageTag("nl")));
        } finally {
            session.dispose();
        }
    }

    private static String beanId(Object bean) {
        return bean.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(bean));
    }

    private static String caseWithRolesClosed(String label, String objectType, Map<String, Object> roles,
                                              List<String> rolesClosed, boolean red) throws Exception {
        Map<String, Object> inst = new LinkedHashMap<>();
        inst.put("label", label);
        inst.put("objectType", objectType);
        inst.put("attributes", Map.of());
        inst.put("roles", roles);
        inst.put("rolesClosed", rolesClosed);
        inst.put("characteristics", List.of());
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("format", "uchar-case-v0");
        root.put("asOfDate", null);
        root.put("input", Map.of("instances", List.of(inst), "parameters", Map.of()));
        if (red) root.put("expectedFault", Map.of("code", "EF-040"));
        else root.put("expected", List.of(Map.of("kind", "consistent")));
        return JSON.writeValueAsString(root);
    }

    private static String validationDocument() {
        return """
                {
                  "format": "uchar-ir-v1",
                  "inferFromEmpty": false,
                  "model": {
                    "objectTypes": [
                      { "node": "ObjectType", "name": "Jutter", "animate": true, "plural": "Jutters",
                        "attributes": [
                          { "node": "Attribute", "name": "vondstaantal",
                            "datatype": { "node": "Number", "whole": true, "decimals": 0, "sign": "nonNegative" } }
                        ],
                        "characteristics": [] },
                      { "node": "ObjectType", "name": "Strandjutter", "supertype": "Jutter", "animate": true,
                        "plural": "Strandjutters", "attributes": [], "characteristics": [] },
                      { "node": "ObjectType", "name": "Vondst", "animate": false, "plural": "Vondsten",
                        "attributes": [],
                        "characteristics": [{ "node": "Characteristic", "name": "toegewezen", "kind": "adjectival" }] },
                      { "node": "ObjectType", "name": "Melding", "animate": false, "plural": "Meldingen",
                        "attributes": [], "characteristics": [] },
                      { "node": "ObjectType", "name": "Derde", "animate": false, "plural": "Derden",
                        "attributes": [], "characteristics": [] },
                      { "node": "ObjectType", "name": "Vreemd", "animate": false, "plural": "Vreemden",
                        "attributes": [], "characteristics": [] }
                    ],
                    "factTypes": [
                      { "node": "FactType", "name": "vinding", "reciprocal": false, "roles": [
                        { "node": "Role", "name": "vinder", "objectType": "Jutter", "many": false },
                        { "node": "Role", "name": "vondst", "objectType": "Vondst", "plural": "vondsten", "many": true }
                      ]},
                      { "node": "FactType", "name": "berging", "reciprocal": false, "roles": [
                        { "node": "Role", "name": "berger", "objectType": "Jutter", "plural": "bergers", "many": true },
                        { "node": "Role", "name": "berging", "objectType": "Vondst", "plural": "bergingen", "many": true }
                      ]},
                      { "node": "FactType", "name": "partnerschap", "reciprocal": true, "roles": [
                        { "node": "Role", "name": "maat", "objectType": "Jutter", "plural": "maten", "many": false }
                      ]},
                      { "node": "FactType", "name": "eerste meldingsrelatie", "reciprocal": false, "roles": [
                        { "node": "Role", "name": "eerste melder", "objectType": "Jutter", "many": false },
                        { "node": "Role", "name": "eerste melding", "objectType": "Melding", "plural": "meldingen", "many": true }
                      ]},
                      { "node": "FactType", "name": "tweede meldingsrelatie", "reciprocal": false, "roles": [
                        { "node": "Role", "name": "tweede melder", "objectType": "Jutter", "many": false },
                        { "node": "Role", "name": "tweede melding", "objectType": "Melding", "plural": "meldingen", "many": true }
                      ]},
                      { "node": "FactType", "name": "vreemde relatie", "reciprocal": false, "roles": [
                        { "node": "Role", "name": "eigenaar", "objectType": "Derde", "many": false },
                        { "node": "Role", "name": "vreemde post", "objectType": "Vreemd", "plural": "vreemde posten", "many": true }
                      ]}
                    ],
                    "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [], "dayKinds": []
                  },
                  "rules": {
                    "rules": [
                      { "node": "Rule", "name": "toegewezen", "versions": [{
                        "validity": { "node": "Validity" },
                        "target": { "node": "AttributeReference", "attribute": "toegewezen", "subjectType": "Vondst" },
                        "value": { "node": "Literal", "value": { "boolean": true }, "type": { "node": "Boolean" } },
                        "condition": { "node": "RoleMembership", "factType": "vinding", "role": "vondst", "negated": false },
                        "initialisation": false
                      }]},
                      { "node": "Rule", "name": "vondstaantal", "versions": [{
                        "validity": { "node": "Validity" },
                        "target": { "node": "AttributeReference", "attribute": "vondstaantal", "subjectType": "Jutter" },
                        "value": {
                          "node": "Aggregation", "kind": "count",
                          "arguments": [{ "node": "AttributeReference", "attribute": "vondsten", "subjectType": "Jutter" }],
                          "elementType": "Vondst",
                          "sourceRole": { "node": "RoleBinding", "factType": "vinding", "role": "vondst" },
                          "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "nonNegative" }
                        },
                        "initialisation": false, "presentation": "calculation"
                      }]}
                    ],
                    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
                    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [], "ruleGroups": []
                  },
                  "diagnostics": []
                }
                """;
    }

    private static String rangeSemanticsDocument() {
        return """
                {
                  "format": "uchar-ir-v1",
                  "inferFromEmpty": false,
                  "model": {
                    "objectTypes": [
                      { "node": "ObjectType", "name": "Jutter", "animate": true, "plural": "Jutters",
                        "attributes": [
                          { "node": "Attribute", "name": "unie aantal",
                            "datatype": { "node": "Number", "whole": true, "decimals": 0, "sign": "nonNegative" } }
                        ],
                        "characteristics": [
                          { "node": "Characteristic", "name": "heeft vondst", "kind": "adjectival" },
                          { "node": "Characteristic", "name": "heeft zware vondst", "kind": "adjectival" },
                          { "node": "Characteristic", "name": "alle veilig", "kind": "adjectival" },
                          { "node": "Characteristic", "name": "een gedekt", "kind": "adjectival" },
                          { "node": "Characteristic", "name": "samengesteld met filterdeponering", "kind": "adjectival" }
                        ] },
                      { "node": "ObjectType", "name": "Vondst", "animate": false, "plural": "Vondsten",
                        "attributes": [
                          { "node": "Attribute", "name": "deler",
                            "datatype": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" } },
                          { "node": "Attribute", "name": "tarief",
                            "datatype": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" } }
                        ],
                        "characteristics": [
                          { "node": "Characteristic", "name": "actief", "kind": "adjectival" },
                          { "node": "Characteristic", "name": "vrijgesteld", "kind": "adjectival" }
                        ] },
                      { "node": "ObjectType", "name": "ZwareVondst", "supertype": "Vondst", "animate": false,
                        "plural": "ZwareVondsten", "attributes": [], "characteristics": [] }
                    ],
                    "factTypes": [
                      { "node": "FactType", "name": "vinding", "reciprocal": false, "roles": [
                        { "node": "Role", "name": "vinder", "objectType": "Jutter", "many": false, "complete": true },
                        { "node": "Role", "name": "vondst", "objectType": "Vondst", "plural": "vondsten", "many": true }
                      ]},
                      { "node": "FactType", "name": "reservevinding", "reciprocal": false, "roles": [
                        { "node": "Role", "name": "reservevinder", "objectType": "Jutter", "many": false, "complete": true },
                        { "node": "Role", "name": "reservevondst", "objectType": "Vondst", "plural": "reservevondsten", "many": true }
                      ]}
                    ],
                    "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [], "dayKinds": []
                  },
                  "rules": {
                    "rules": [
                      { "node": "Rule", "name": "heeft vondst", "versions": [{
                        "validity": { "node": "Validity" },
                        "target": { "node": "AttributeReference", "attribute": "heeft vondst", "subjectType": "Jutter" },
                        "value": { "node": "Literal", "value": { "boolean": true }, "type": { "node": "Boolean" } },
                        "condition": { "node": "RoleMembership", "factType": "vinding", "role": "vinder", "negated": false },
                        "initialisation": false
                      }]},
                      { "node": "Rule", "name": "heeft zware vondst", "versions": [{
                        "validity": { "node": "Validity" },
                        "target": { "node": "AttributeReference", "attribute": "heeft zware vondst", "subjectType": "Jutter" },
                        "value": { "node": "Literal", "value": { "boolean": true }, "type": { "node": "Boolean" } },
                        "condition": { "node": "RoleMembership", "factType": "vinding", "role": "vinder",
                                       "counterpartType": "ZwareVondst", "negated": false },
                        "initialisation": false
                      }]},
                      { "node": "Rule", "name": "unie aantal", "versions": [{
                        "validity": { "node": "Validity" },
                        "target": { "node": "AttributeReference", "attribute": "unie aantal", "subjectType": "Jutter" },
                        "value": {
                          "node": "Aggregation", "kind": "count",
                          "arguments": [{
                            "node": "InstanceSetUnion",
                            "branches": [
                              { "node": "InstanceSetBranch", "elementType": "Vondst",
                                "sourceRole": { "node": "RoleBinding", "factType": "vinding", "role": "vondst" },
                                "filter": {
                                  "node": "Comparison", "op": "greater",
                                  "left": {
                                    "node": "ContextValue", "name": "evaluationYear",
                                    "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" }
                                  },
                                  "right": {
                                    "node": "Literal", "value": { "number": "2025" },
                                    "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" }
                                  }
                                } },
                              { "node": "InstanceSetBranch", "elementType": "Vondst",
                                "sourceRole": { "node": "RoleBinding", "factType": "reservevinding", "role": "reservevondst" },
                                "filter": { "node": "Compound", "quantifier": "all", "threshold": 0, "parts": [] } }
                            ]
                          }],
                          "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "nonNegative" }
                        },
                        "initialisation": false, "presentation": "calculation"
                      }]},
                      { "node": "Rule", "name": "alle veilig", "versions": [{
                        "validity": { "node": "Validity" },
                        "target": { "node": "AttributeReference", "attribute": "alle veilig", "subjectType": "Jutter" },
                        "value": { "node": "Literal", "value": { "boolean": true }, "type": { "node": "Boolean" } },
                        "condition": {
                          "node": "QuantifierFold", "quantifier": "all", "elementType": "Vondst",
                          "sourceRole": { "node": "RoleBinding", "factType": "vinding", "role": "vondst" },
                          "test": {
                            "node": "Compound", "quantifier": "atLeast", "threshold": 1,
                            "parts": [
                              { "node": "CharacteristicCheck", "characteristic": "vrijgesteld", "negated": false },
                              { "node": "Comparison", "op": "greater",
                                "left": {
                                  "node": "ContextValue", "name": "evaluationYear",
                                  "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" }
                                },
                                "right": {
                                  "node": "Literal", "value": { "number": "2025" },
                                  "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" }
                                } }
                            ]
                          }
                        },
                        "initialisation": false
                      }]},
                      { "node": "Rule", "name": "een gedekt", "versions": [{
                        "validity": { "node": "Validity" },
                        "target": { "node": "AttributeReference", "attribute": "een gedekt", "subjectType": "Jutter" },
                        "value": { "node": "Literal", "value": { "boolean": true }, "type": { "node": "Boolean" } },
                        "condition": {
                          "node": "QuantifierFold", "quantifier": "atLeast", "threshold": 1, "elementType": "Vondst",
                          "sourceRole": { "node": "RoleBinding", "factType": "vinding", "role": "vondst" },
                          "test": {
                            "node": "Comparison", "op": "greater",
                            "left": {
                              "node": "Binary", "op": "dividedBy",
                              "left": { "node": "AttributeReference", "attribute": "deler", "subjectType": "Vondst" },
                              "right": { "node": "AttributeReference", "attribute": "tarief", "subjectType": "Vondst" },
                              "type": { "node": "Number", "whole": false, "decimals": 0, "sign": "unspecified" }
                            },
                            "right": { "node": "Literal", "value": { "number": "0" }, "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" } }
                          }
                        },
                        "initialisation": false
                      }]},
                      { "node": "Rule", "name": "samengesteld met filterdeponering", "versions": [{
                        "validity": { "node": "Validity" },
                        "target": { "node": "AttributeReference", "attribute": "samengesteld met filterdeponering", "subjectType": "Jutter" },
                        "value": { "node": "Literal", "value": { "boolean": true }, "type": { "node": "Boolean" } },
                        "condition": {
                          "node": "Compound", "quantifier": "atLeast", "threshold": 1,
                          "parts": [
                            {
                              "node": "QuantifierFold", "quantifier": "all", "elementType": "Vondst",
                              "sourceRole": { "node": "RoleBinding", "factType": "vinding", "role": "vondst" },
                              "filter": {
                                "node": "Compound", "quantifier": "atLeast", "threshold": 1,
                                "parts": [
                                  { "node": "Comparison", "op": "greater",
                                    "left": {
                                      "node": "Binary", "op": "dividedBy",
                                      "left": { "node": "AttributeReference", "attribute": "deler", "subjectType": "Vondst" },
                                      "right": { "node": "AttributeReference", "attribute": "tarief", "subjectType": "Vondst" },
                                      "type": { "node": "Number", "whole": false, "decimals": 0, "sign": "unspecified" }
                                    },
                                    "right": { "node": "Literal", "value": { "number": "0" }, "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" } } },
                                  { "node": "Compound", "quantifier": "all", "threshold": 0, "parts": [] }
                                ]
                              },
                              "test": { "node": "CharacteristicCheck", "characteristic": "actief", "negated": false }
                            },
                            { "node": "Compound", "quantifier": "all", "threshold": 0, "parts": [] }
                          ]
                        },
                        "initialisation": false
                      }]}
                    ],
                    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
                    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [], "ruleGroups": []
                  },
                  "diagnostics": []
                }
                """;
    }
}
