package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.engine.ScenarioResult;
import dev.uchar.regelspraak.loader.IrLoader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Role expectations {@code roleEmpty}, {@code roleCount}, {@code roleTarget} and {@code roleOpen}
 * (RUNTIME-SEMANTICS ch. 1.5, ch. 5.7) in {@link CaseRunner} and {@link DroolsBackend#compare} on both
 * {@link DroolsBackend.Branch#EXEC_MODEL} and {@link DroolsBackend.Branch#DRL_TEXT}:
 * <ul>
 *   <li>load validation without an {@code EF-*} code (unknown {@code label}, undeclared/unreachable {@code role},
 *       ambiguous {@code role}, non-integer or negative {@code count}, {@code roleTarget} on {@code to-many} or
 *       reciprocal, unknown {@code target} including {@code oc0::J1}, and incompatible/supertype {@code target});</li>
 *   <li>fixpoint evaluation across {@code ObjectCreation} (0, 1, and 2 created instances, 1:1 input guard, and a
 *       nameless created object never matching {@code roleTarget}), {@code FactCreation}, 1:1 reciprocal relations,
 *       self-relations in both directions, and subtype targets;</li>
 *   <li>strict {@code inferFromEmpty: false} open vs closed {@code to-one} and {@code to-many} input roles (including
 *       an open {@code to-many} role with linked elements, where {@code roleOpen} holds and {@code roleCount} fails,
 *       and {@code --closed-inputs});</li>
 *   <li>mismatch diagnostics and {@link CaseRunner#report} {@link ScenarioResult.ExpectationResult} rendering.</li>
 * </ul>
 */
class RoleExpectationsWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void load_validation_refuses_invalid_role_expectations_without_ef_codes(@TempDir Path dir) throws Exception {
        Path ir = dir.resolve("ir.json");
        Files.writeString(ir, strictInputModel());

        List<String> invalidExpectations = List.of(
                // unknown label
                "{ \"kind\": \"roleEmpty\", \"label\": \"ONBEKEND\", \"role\": \"vinder\" }",
                // undeclared role
                "{ \"kind\": \"roleEmpty\", \"label\": \"J1\", \"role\": \"onbekend\" }",
                // unreachable role (declared on Vreemd <-> Derde, 0 hits from Jutter)
                "{ \"kind\": \"roleCount\", \"label\": \"J1\", \"role\": \"vreemde post\", \"count\": 0 }",
                // ambiguous role across two fact types
                "{ \"kind\": \"roleEmpty\", \"label\": \"J1\", \"role\": \"meldingen\" }",
                "{ \"kind\": \"roleOpen\", \"label\": \"J1\", \"role\": \"meldingen\" }",
                // missing / negative / float / string / boolean count
                "{ \"kind\": \"roleCount\", \"label\": \"J1\", \"role\": \"vondsten\" }",
                "{ \"kind\": \"roleCount\", \"label\": \"J1\", \"role\": \"vondsten\", \"count\": -1 }",
                "{ \"kind\": \"roleCount\", \"label\": \"J1\", \"role\": \"vondsten\", \"count\": 1.5 }",
                "{ \"kind\": \"roleCount\", \"label\": \"J1\", \"role\": \"vondsten\", \"count\": 1.0 }",
                "{ \"kind\": \"roleCount\", \"label\": \"J1\", \"role\": \"vondsten\", \"count\": \"1\" }",
                "{ \"kind\": \"roleCount\", \"label\": \"J1\", \"role\": \"vondsten\", \"count\": true }",
                // roleTarget on to-many and on reciprocal
                "{ \"kind\": \"roleTarget\", \"label\": \"J1\", \"role\": \"vondsten\", \"target\": \"V1\" }",
                "{ \"kind\": \"roleTarget\", \"label\": \"J1\", \"role\": \"maat\", \"target\": \"J2\" }",
                // roleTarget with unknown target or internal creation-style label
                "{ \"kind\": \"roleTarget\", \"label\": \"V1\", \"role\": \"vinder\", \"target\": \"J99\" }",
                "{ \"kind\": \"roleTarget\", \"label\": \"V1\", \"role\": \"vinder\", \"target\": \"oc0::J1\" }",
                // roleTarget with incompatible / supertype target (Persoon P1 is supertype of Jutter)
                "{ \"kind\": \"roleTarget\", \"label\": \"V1\", \"role\": \"vinder\", \"target\": \"P1\" }",
                "{ \"kind\": \"roleTarget\", \"label\": \"V1\", \"role\": \"vinder\", \"target\": \"V1\" }");

        JsonNode validControl = JSON.readTree(caseWithExpectation(
                "{ \"kind\": \"roleOpen\", \"label\": \"J1\", \"role\": \"vondsten\" }"));
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            assertNull(CaseRunner.playCase(ir, validControl, branch), branch::name);
            for (String exp : invalidExpectations) {
                JsonNode c = JSON.readTree(caseWithExpectation(exp));
                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                        () -> CaseRunner.playCase(ir, c, branch), () -> branch + ": " + exp);
                assertFalse(ex instanceof IrLoader.DocumentError,
                        () -> branch + " must pass IR loading for " + exp + ": " + ex);
                assertEquals(List.of(), CaseRunner.faultKinds(ex),
                        () -> branch + " must carry no EF-* code for " + exp + ": " + ex);
            }
        }
    }

    @Test void creations_self_relations_and_reciprocal_roles_evaluate_identically_on_both_branches(@TempDir Path dir)
            throws Exception {
        Path ir = dir.resolve("ir.json");
        Files.writeString(ir, creationModel());

        // Case 1: J1 gets 2 created Penningbesluiten, 1 created Oorkonde, FactCreation links J1 -> H1,
        // self-relation keten V1 -> V2, reciprocal maat J1 <-> J2; J2 gets 0 creations and no haven.
        JsonNode passCase = JSON.readTree("""
                {
                  "format": "uchar-case-v0",
                  "asOfDate": null,
                  "input": {
                    "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": { "vondstscore": { "number": "20" } },
                        "roles": { "thuishaven": ["H1"], "maat": ["J2"] },
                        "characteristics": ["verdienstelijk"] },
                      { "label": "J2", "objectType": "Strandjutter",
                        "attributes": { "vondstscore": { "number": "3" } },
                        "roles": {},
                        "characteristics": [] },
                      { "label": "J3", "objectType": "Jutter",
                        "attributes": { "vondstscore": { "number": "20" } },
                        "roles": { "oorkonde": ["O1"] },
                        "characteristics": [] },
                      { "label": "O1", "objectType": "Oorkonde",
                        "attributes": {}, "roles": {}, "characteristics": [] },
                      { "label": "H1", "objectType": "Haven",
                        "attributes": {}, "roles": {}, "characteristics": [] },
                      { "label": "V1", "objectType": "Vergunning",
                        "attributes": {}, "roles": { "volgende vergunning": ["V2"] }, "characteristics": [] },
                      { "label": "V2", "objectType": "Vergunning",
                        "attributes": {}, "roles": {}, "characteristics": [] }
                    ],
                    "parameters": {}
                  },
                  "expected": [
                    { "kind": "roleCount", "label": "J1", "role": "penningbesluiten", "count": 2 },
                    { "kind": "roleCount", "label": "J1", "role": "oorkonde", "count": 1 },
                    { "kind": "roleTarget", "label": "J1", "role": "toegewezen haven", "target": "H1" },
                    { "kind": "roleCount", "label": "J1", "role": "maat", "count": 1 },
                    { "kind": "roleCount", "label": "J2", "role": "maat", "count": 1 },
                    { "kind": "roleEmpty", "label": "J3", "role": "maat" },
                    { "kind": "roleEmpty", "label": "J2", "role": "penningbesluiten" },
                    { "kind": "roleCount", "label": "J2", "role": "penningbesluit", "count": 0 },
                    { "kind": "roleEmpty", "label": "J2", "role": "oorkonde" },
                    { "kind": "roleEmpty", "label": "J2", "role": "toegewezen haven" },
                    { "kind": "roleTarget", "label": "J3", "role": "oorkonde", "target": "O1" },
                    { "kind": "roleCount", "label": "J3", "role": "oorkonde", "count": 1 },
                    { "kind": "roleEmpty", "label": "V1", "role": "vorige vergunning" },
                    { "kind": "roleTarget", "label": "V1", "role": "volgende vergunning", "target": "V2" },
                    { "kind": "roleTarget", "label": "V2", "role": "vorige vergunning", "target": "V1" },
                    { "kind": "roleEmpty", "label": "V2", "role": "volgende vergunning" }
                  ]
                }
                """);

        // A nameless created Oorkonde on J1 NEVER matches roleTarget O1!
        JsonNode namelessCreatedFailsRoleTarget = JSON.readTree("""
                {
                  "format": "uchar-case-v0",
                  "asOfDate": null,
                  "input": {
                    "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": { "vondstscore": { "number": "20" } },
                        "roles": {}, "characteristics": [] },
                      { "label": "O1", "objectType": "Oorkonde",
                        "attributes": {}, "roles": {}, "characteristics": [] }
                    ],
                    "parameters": {}
                  },
                  "expected": [
                    { "kind": "roleTarget", "label": "J1", "role": "oorkonde", "target": "O1" }
                  ]
                }
                """);

        // roleOpen always fails in an ordinary document (inferFromEmpty: true)
        JsonNode roleOpenInOrdinaryFails = JSON.readTree("""
                {
                  "format": "uchar-case-v0",
                  "asOfDate": null,
                  "input": {
                    "instances": [
                      { "label": "J2", "objectType": "Jutter",
                        "attributes": { "vondstscore": { "number": "3" } },
                        "roles": {}, "characteristics": [] }
                    ],
                    "parameters": {}
                  },
                  "expected": [
                    { "kind": "roleOpen", "label": "J2", "role": "oorkonde" }
                  ]
                }
                """);

        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            assertNull(CaseRunner.playCase(ir, passCase, branch), branch::name);

            String failNameless = CaseRunner.playCase(ir, namelessCreatedFailsRoleTarget, branch);
            assertNotNull(failNameless, branch::name);
            assertTrue(failNameless.contains("O1") && failNameless.contains("<Oorkonde>"),
                    () -> branch + ": " + failNameless);

            String failOpen = CaseRunner.playCase(ir, roleOpenInOrdinaryFails, branch);
            assertNotNull(failOpen, branch::name);
            assertTrue(failOpen.contains("oorkonde"), () -> branch + ": " + failOpen);
        }
    }

    @Test void strict_open_and_closed_roles_evaluate_and_report_cleanly_on_both_branches(@TempDir Path dir)
            throws Exception {
        Path ir = dir.resolve("ir.json");
        Files.writeString(ir, strictInputModel());

        // J1: open to-many 'vondsten' with 1 linked element V1 -> roleOpen holds, roleCount/roleEmpty fail!
        // J2 (Strandjutter subtype): closed empty 'vondsten' via rolesClosed -> roleEmpty & roleCount=0 hold!
        // V1: linked to J1 -> roleTarget=J1 & roleCount=1 hold!
        // V2: rolesEmpty: ["vinder"] -> roleEmpty & roleCount=0 hold!
        // V3: unlinked & unclosed 'vinder' -> roleOpen holds!
        // J2 linked as V4's vinder -> subtype Strandjutter satisfies roleTarget on V4!
        JsonNode strictPass = JSON.readTree("""
                {
                  "format": "uchar-case-v0",
                  "asOfDate": null,
                  "input": {
                    "instances": [
                      { "label": "P1", "objectType": "Persoon", "attributes": {}, "roles": {}, "characteristics": [] },
                      { "label": "J1", "objectType": "Jutter", "attributes": {},
                        "roles": { "vondsten": ["V1"] }, "characteristics": [] },
                      { "label": "J2", "objectType": "Strandjutter", "attributes": {},
                        "roles": { "vondsten": ["V4"] },
                        "rolesClosed": ["vondsten"],
                        "characteristics": [] },
                      { "label": "J3", "objectType": "Jutter", "attributes": {},
                        "roles": {}, "rolesClosed": ["vondsten"], "characteristics": [] },
                      { "label": "V1", "objectType": "Vondst", "attributes": {}, "roles": {}, "characteristics": [] },
                      { "label": "V2", "objectType": "Vondst", "attributes": {}, "roles": {},
                        "rolesEmpty": ["vinder"], "characteristics": [] },
                      { "label": "V3", "objectType": "Vondst", "attributes": {}, "roles": {}, "characteristics": [] },
                      { "label": "V4", "objectType": "Vondst", "attributes": {}, "roles": {}, "characteristics": [] }
                    ],
                    "parameters": {}
                  },
                  "expected": [
                    { "kind": "roleOpen", "label": "J1", "role": "vondsten" },
                    { "kind": "roleCount", "label": "J2", "role": "vondsten", "count": 1 },
                    { "kind": "roleEmpty", "label": "J3", "role": "vondsten" },
                    { "kind": "roleCount", "label": "J3", "role": "vondsten", "count": 0 },
                    { "kind": "roleTarget", "label": "V1", "role": "vinder", "target": "J1" },
                    { "kind": "roleCount", "label": "V1", "role": "vinder", "count": 1 },
                    { "kind": "roleEmpty", "label": "V2", "role": "vinder" },
                    { "kind": "roleCount", "label": "V2", "role": "vinder", "count": 0 },
                    { "kind": "roleOpen", "label": "V3", "role": "vinder" },
                    { "kind": "roleTarget", "label": "V4", "role": "vinder", "target": "J2" }
                  ]
                }
                """);

        // Under --closed-inputs, J1.vondsten ([V1]) and V3.vinder ([]) are closed!
        JsonNode closedInputsPass = JSON.readTree("""
                {
                  "format": "uchar-case-v0",
                  "asOfDate": null,
                  "input": {
                    "instances": [
                      { "label": "J1", "objectType": "Jutter", "attributes": {},
                        "roles": { "vondsten": ["V1"] }, "characteristics": [] },
                      { "label": "V1", "objectType": "Vondst", "attributes": {}, "roles": {}, "characteristics": [] },
                      { "label": "V3", "objectType": "Vondst", "attributes": {}, "roles": {}, "characteristics": [] }
                    ],
                    "parameters": {}
                  },
                  "expected": [
                    { "kind": "roleCount", "label": "J1", "role": "vondsten", "count": 1 },
                    { "kind": "roleEmpty", "label": "V3", "role": "vinder" }
                  ]
                }
                """);

        // Mismatch: roleCount=1 on open J1.vondsten fails because J1.vondsten is open!
        JsonNode openCollectionFailsCount = JSON.readTree("""
                {
                  "format": "uchar-case-v0",
                  "asOfDate": null,
                  "input": {
                    "instances": [
                      { "label": "J1", "objectType": "Jutter", "attributes": {},
                        "roles": { "vondsten": ["V1"] }, "characteristics": [] },
                      { "label": "V1", "objectType": "Vondst", "attributes": {}, "roles": {}, "characteristics": [] }
                    ],
                    "parameters": {}
                  },
                  "expected": [
                    { "kind": "roleCount", "label": "J1", "role": "vondsten", "count": 1 }
                  ]
                }
                """);

        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            assertNull(CaseRunner.playCase(ir, strictPass, branch), branch::name);
            assertEquals(java.util.Collections.singletonList((String) null),
                    CaseRunner.playCases(ir, List.of(closedInputsPass), branch, true), branch::name);
            String mismatch = CaseRunner.playCase(ir, openCollectionFailsCount, branch);
            assertNotNull(mismatch, branch::name);
            assertTrue(mismatch.contains("vondsten") && mismatch.contains("J1"), () -> branch + ": " + mismatch);
        }

        // Verify CaseRunner.report populates source, expected and actual on ExpectationResult
        Path failPath = dir.resolve("case1-mismatch.json");
        Files.writeString(failPath, JSON.writeValueAsString(openCollectionFailsCount));
        Path passPath = dir.resolve("case2-pass.json");
        Files.writeString(passPath, JSON.writeValueAsString(strictPass));
        List<ScenarioResult> reports = CaseRunner.report(ir, List.of(failPath, passPath));
        assertEquals(2, reports.size());
        assertEquals(ScenarioResult.Status.FAIL, reports.get(0).status());
        ScenarioResult.ExpectationResult er = reports.get(0).expectations().get(0);
        assertFalse(er.ok());
        assertEquals("roleCount J1 vondsten", er.source());
        assertEquals("1", er.expected());
        assertTrue(er.actual().equals("open") || er.actual().equals("openstaand"), er.actual());
        assertEquals(ScenarioResult.Status.PASS, reports.get(1).status());
    }

    private static String caseWithExpectation(String expectationJson) {
        return """
                {
                  "format": "uchar-case-v0",
                  "asOfDate": null,
                  "input": {
                    "instances": [
                      { "label": "P1", "objectType": "Persoon", "attributes": {}, "roles": {}, "characteristics": [] },
                      { "label": "J1", "objectType": "Jutter", "attributes": {}, "roles": {}, "characteristics": [] },
                      { "label": "J2", "objectType": "Strandjutter", "attributes": {}, "roles": {}, "characteristics": [] },
                      { "label": "V1", "objectType": "Vondst", "attributes": {}, "roles": {}, "characteristics": [] }
                    ],
                    "parameters": {}
                  },
                  "expected": [
                    %s
                  ]
                }
                """.formatted(expectationJson);
    }

    private static String strictInputModel() {
        return """
                {
                  "format": "uchar-ir-v1",
                  "inferFromEmpty": false,
                  "model": {
                    "objectTypes": [
                      { "node": "ObjectType", "name": "Persoon", "animate": true, "plural": "Personen",
                        "attributes": [], "characteristics": [] },
                      { "node": "ObjectType", "name": "Jutter", "supertype": "Persoon", "animate": true,
                        "plural": "Jutters", "attributes": [], "characteristics": [
                          { "node": "Characteristic", "name": "heeft vondsten", "kind": "possessive" }
                        ] },
                      { "node": "ObjectType", "name": "Strandjutter", "supertype": "Jutter", "animate": true,
                        "plural": "Strandjutters", "attributes": [], "characteristics": [] },
                      { "node": "ObjectType", "name": "Vondst", "animate": false, "plural": "Vondsten",
                        "attributes": [], "characteristics": [] },
                      { "node": "ObjectType", "name": "Vreemd", "animate": false, "plural": "Vreemden",
                        "attributes": [], "characteristics": [] },
                      { "node": "ObjectType", "name": "Derde", "animate": false, "plural": "Derden",
                        "attributes": [], "characteristics": [] }
                    ],
                    "factTypes": [
                      { "node": "FactType", "name": "vinding", "reciprocal": false, "roles": [
                        { "node": "Role", "name": "vinder", "objectType": "Jutter", "many": false },
                        { "node": "Role", "name": "vondst", "plural": "vondsten", "objectType": "Vondst", "many": true }
                      ] },
                      { "node": "FactType", "name": "eerste melding", "reciprocal": false, "roles": [
                        { "node": "Role", "name": "melder", "objectType": "Jutter", "many": false },
                        { "node": "Role", "name": "melding", "plural": "meldingen", "objectType": "Vondst", "many": true }
                      ] },
                      { "node": "FactType", "name": "tweede melding", "reciprocal": false, "roles": [
                        { "node": "Role", "name": "herkeurder", "objectType": "Jutter", "many": false },
                        { "node": "Role", "name": "melding", "plural": "meldingen", "objectType": "Vondst", "many": true }
                      ] },
                      { "node": "FactType", "name": "vreemde relatie", "reciprocal": false, "roles": [
                        { "node": "Role", "name": "vreemde drager", "objectType": "Derde", "many": false },
                        { "node": "Role", "name": "vreemde post", "objectType": "Vreemd", "many": false }
                      ] },
                      { "node": "FactType", "name": "strandmaat", "reciprocal": true, "roles": [
                        { "node": "Role", "name": "maat", "plural": "maten", "objectType": "Jutter", "many": false }
                      ] }
                    ],
                    "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [], "dayKinds": []
                  },
                  "rules": {
                    "rules": [
                      { "node": "Rule", "name": "vondstbezit", "versions": [
                        { "validity": { "node": "Validity" },
                          "target": { "node": "AttributeReference", "attribute": "heeft vondsten", "subjectType": "Jutter" },
                          "value": { "node": "Literal", "value": { "boolean": true }, "type": { "node": "Boolean" } },
                          "condition": { "node": "RoleMembership", "factType": "vinding", "role": "vinder", "negated": false },
                          "initialisation": false }
                      ] }
                    ],
                    "dayKindDefinitions": [], "objectCreations": [], "allocations": [], "consistencies": [],
                    "uniquenesses": [], "factCreations": [], "startPointDeterminations": [], "ruleGroups": []
                  },
                  "diagnostics": []
                }
                """;
    }

    private static String creationModel() {
        return """
                {
                  "format": "uchar-ir-v1",
                  "model": {
                    "objectTypes": [
                      { "node": "ObjectType", "name": "Jutter", "animate": true, "plural": "Jutters",
                        "attributes": [
                          { "node": "Attribute", "name": "vondstscore",
                            "datatype": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" } }
                        ],
                        "characteristics": [
                          { "node": "Characteristic", "name": "verdienstelijk", "kind": "adjectival" }
                        ] },
                      { "node": "ObjectType", "name": "Strandjutter", "supertype": "Jutter", "animate": true,
                        "plural": "Strandjutters", "attributes": [], "characteristics": [] },
                      { "node": "ObjectType", "name": "Penningbesluit", "animate": false, "plural": "Penningbesluiten",
                        "attributes": [], "characteristics": [] },
                      { "node": "ObjectType", "name": "Oorkonde", "animate": false, "plural": "Oorkonden",
                        "attributes": [], "characteristics": [] },
                      { "node": "ObjectType", "name": "Haven", "animate": false, "plural": "Havens",
                        "attributes": [], "characteristics": [] },
                      { "node": "ObjectType", "name": "Vergunning", "animate": false, "plural": "Vergunningen",
                        "attributes": [], "characteristics": [] }
                    ],
                    "factTypes": [
                      { "node": "FactType", "name": "penningtoekenning", "reciprocal": false, "roles": [
                        { "node": "Role", "name": "gerechtigde", "objectType": "Jutter", "many": false },
                        { "node": "Role", "name": "penningbesluit", "plural": "penningbesluiten",
                          "objectType": "Penningbesluit", "many": true }
                      ] },
                      { "node": "FactType", "name": "oorkondeverlening", "reciprocal": false, "roles": [
                        { "node": "Role", "name": "ontvanger", "objectType": "Jutter", "many": false },
                        { "node": "Role", "name": "oorkonde", "objectType": "Oorkonde", "many": false }
                      ] },
                      { "node": "FactType", "name": "thuishavenrelatie", "reciprocal": false, "roles": [
                        { "node": "Role", "name": "vaste jutter", "plural": "vaste jutters", "objectType": "Jutter", "many": true },
                        { "node": "Role", "name": "thuishaven", "objectType": "Haven", "many": false }
                      ] },
                      { "node": "FactType", "name": "haventoewijzing", "reciprocal": false, "roles": [
                        { "node": "Role", "name": "aangewezen jutter", "plural": "aangewezen jutters",
                          "objectType": "Jutter", "many": true },
                        { "node": "Role", "name": "toegewezen haven", "objectType": "Haven", "many": false }
                      ] },
                      { "node": "FactType", "name": "opvolging", "reciprocal": false, "roles": [
                        { "node": "Role", "name": "vorige vergunning", "objectType": "Vergunning", "many": false },
                        { "node": "Role", "name": "volgende vergunning", "objectType": "Vergunning", "many": false }
                      ] },
                      { "node": "FactType", "name": "strandmaat", "reciprocal": true, "roles": [
                        { "node": "Role", "name": "maat", "plural": "maten", "objectType": "Jutter", "many": false }
                      ] }
                    ],
                    "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [], "dayKinds": []
                  },
                  "rules": {
                    "rules": [],
                    "dayKindDefinitions": [],
                    "objectCreations": [
                      { "node": "ObjectCreation", "name": "basisbesluit", "versions": [
                        { "createdType": "Penningbesluit", "subject": "Jutter",
                          "assignments": [],
                          "condition": { "node": "Comparison", "op": "greaterOrEqual",
                            "left": { "node": "AttributeReference", "subjectType": "Jutter", "attribute": "vondstscore" },
                            "right": { "node": "Literal", "value": { "number": "10" },
                              "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" } } },
                          "validity": { "node": "Validity" } }
                      ] },
                      { "node": "ObjectCreation", "name": "erebesluit", "versions": [
                        { "createdType": "Penningbesluit", "subject": "Jutter",
                          "assignments": [],
                          "condition": { "node": "CharacteristicCheck", "characteristic": "verdienstelijk", "negated": false },
                          "validity": { "node": "Validity" } }
                      ] },
                      { "node": "ObjectCreation", "name": "oorkonde", "versions": [
                        { "createdType": "Oorkonde", "subject": "Jutter",
                          "assignments": [],
                          "condition": { "node": "Comparison", "op": "greaterOrEqual",
                            "left": { "node": "AttributeReference", "subjectType": "Jutter", "attribute": "vondstscore" },
                            "right": { "node": "Literal", "value": { "number": "10" },
                              "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" } } },
                          "validity": { "node": "Validity" } }
                      ] }
                    ],
                    "allocations": [], "consistencies": [], "uniquenesses": [],
                    "factCreations": [
                      { "node": "FactCreation", "name": "wijs haven toe", "versions": [
                        { "factType": "haventoewijzing", "anchorRole": "aangewezen jutter", "targetRole": "toegewezen haven",
                          "anchor": "Jutter",
                          "path": [ { "node": "RoleBinding", "factType": "thuishavenrelatie", "role": "thuishaven" } ],
                          "condition": { "node": "CharacteristicCheck", "characteristic": "verdienstelijk", "negated": false },
                          "validity": { "node": "Validity" } }
                      ] }
                    ],
                    "startPointDeterminations": [], "ruleGroups": []
                  },
                  "diagnostics": []
                }
                """;
    }
}
