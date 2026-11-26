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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A non-reciprocal to-one role the input states to carry no link ({@code rolesEmpty}, RUNTIME-SEMANTICS ch. 1.5,
 * ch. 5.7), including names whose declared spelling is not a Java identifier as written and singular/plural variants,
 * on both branches:
 * <ul>
 *   <li>in a document with {@code inferFromEmpty: false} an input to-one role stated empty is settled absent
 *       (idempotent under repeated, differently cased/spaced, or singular + plural entries), and one the case does not
 *       name is open;</li>
 *   <li>in an ordinary document the statement changes nothing: every unlinked role is settled absent;</li>
 *   <li>a role linked on the instance or from its counterpart and also named in {@code rolesEmpty} is EF-049 with
 *       {@code label} and the first conflicting {@code role} in {@code rolesEmpty} list order (slot-exact on a
 *       self-relation), an undeclared name in the model is EF-040, and an ambiguous role across two fact types is
 *       EF-001;</li>
 *   <li>a role with 0 opposite hits on the instance's type, a {@code many: true} role and a role of a reciprocal fact
 *       type in {@code rolesEmpty} are refused at case load;</li>
 *   <li>{@link DroolsBackend#insert} takes {@link Input#roleEmpty} in the declared spelling, another spelling under
 *       {@link Namen#norm}, and the declared plural (inherited roles on subtypes included), removes that role from what
 *       is open, and refuses every invalid call with DROOLS-376 ({@code not-in-input}, {@code undeclared},
 *       {@code ambiguous}, {@code not-to-one} on {@code many} and {@code reciprocal}, and {@code linked} via own or
 *       counterpart reference fields, slot-exact on self-relations).</li>
 * </ul>
 */
class EmptyRoleInputWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path RED_ROLE_DIR = Path.of("..", "..", "corpus", "red", "jutterswet-rol-gevuld-en-leeg");

    private record NamedRole(String factType, String name, String plural, String nearRole) {}

    /** Declared role names whose bean member a getter derived from the name as written misses, each with a plural. */
    private static final List<NamedRole> ROLES = List.of(
            new NamedRole("zeevinding", "op zee", "op zeeën", "zeevondst"),
            new NamedRole("medevinding", "mede-vinder", "mede-vinders", "medevondst"),
            new NamedRole("juttervinding", "jutter's", "jutter's vinders", "juttervondst"),
            new NamedRole("euvinding", "EU", "EU-vinders", "euvondst"),
            new NamedRole("lidvinding", "3e lid", "3e leden", "lidvondst"),
            new NamedRole("oppervlakvinding", "m²", "m²-vinders", "oppervlakvondst"),
            new NamedRole("isvinding", "İş", "İş-vinders", "isvondst"));

    @Test void a_role_stated_empty_is_settled_absent_in_a_strict_document() throws Exception {
        assertEquals(List.of(), play(false, true));
    }

    @Test void a_role_stated_empty_changes_nothing_in_an_ordinary_document() throws Exception {
        assertEquals(List.of(), play(true, false));
    }

    @Test void red_corpus_ef_049_cases_fault_on_both_branches() {
        Path ir = RED_ROLE_DIR.resolve("ir.json");
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            CaseRunner.RedResult same = CaseRunner.runRed(ir, RED_ROLE_DIR.resolve("case1-zelfde-instantie.json"), branch);
            assertEquals("FAULT", same.status(), () -> branch + ": " + same.detail());
            assertEquals("EF-049", same.code(), () -> branch + ": " + same.detail());
            assertEquals(List.of(Map.of("label", "V1", "role", "vinder")), same.params(), branch::name);

            CaseRunner.RedResult counter = CaseRunner.runRed(ir, RED_ROLE_DIR.resolve("case2-tegeninstantie.json"), branch);
            assertEquals("FAULT", counter.status(), () -> branch + ": " + counter.detail());
            assertEquals("EF-049", counter.code(), () -> branch + ": " + counter.detail());
            assertEquals(List.of(Map.of("label", "V2", "role", "vinder")), counter.params(), branch::name);
        }
    }

    @Test void a_role_linked_and_stated_empty_is_ef_049_and_undeclared_or_ambiguous_faults() throws Exception {
        List<String> faults = new ArrayList<>();
        for (boolean ordinary : new boolean[]{false, true}) {
            Path ir = Files.createTempFile("rolnamen", ".json"),
                    c = Files.createTempFile("rolnamen-case", ".json");
            try {
                Files.writeString(ir, document(ordinary, true));
                for (NamedRole role : ROLES) {
                    for (String emptySpelling : List.of(role.name(), otherSpelling(role.name()), role.plural())) {
                        // direct link on V1 + V1.rolesEmpty
                        Files.writeString(c, JSON.writeValueAsString(twoInstanceCase(
                                Map.of(role.name(), List.of("J1")), List.of(emptySpelling), Map.of())));
                        expectFault(ir, c, ordinary, role.name() + " direct «" + emptySpelling + "»", "EF-049",
                                Map.of("label", "V1", "role", emptySpelling), faults);

                        // counterpart link on J1 + V1.rolesEmpty
                        Files.writeString(c, JSON.writeValueAsString(twoInstanceCase(
                                Map.of(), List.of(emptySpelling), Map.of(role.nearRole() + "en", List.of("V1")))));
                        expectFault(ir, c, ordinary, role.name() + " counterpart «" + emptySpelling + "»", "EF-049",
                                Map.of("label", "V1", "role", emptySpelling), faults);
                    }
                }
                // list-order first conflicting entry in rolesEmpty: "mede-vinder" is unlinked, "  OP  ZEE " is first conflict
                Files.writeString(c, JSON.writeValueAsString(twoInstanceCase(
                        Map.of("op zee", List.of("J1")),
                        List.of("mede-vinder", "  OP  ZEE ", "op zeeën"), Map.of())));
                expectFault(ir, c, ordinary, "list-order first conflict", "EF-049",
                        Map.of("label", "V1", "role", "  OP  ZEE "), faults);

                // undeclared in model -> EF-040
                Files.writeString(c, JSON.writeValueAsString(twoInstanceCase(
                        Map.of(), List.of("onbestaand"), Map.of())));
                expectFault(ir, c, ordinary, "undeclared", "EF-040",
                        Map.of("label", "V1", "name", "onbestaand"), faults);

                // ambiguous across two fact types -> EF-001
                Files.writeString(c, JSON.writeValueAsString(twoInstanceCase(
                        Map.of(), List.of("gedeeld"), Map.of())));
                expectFault(ir, c, ordinary, "ambiguous", "EF-001",
                        Map.of("roleKey", "gedeeld", "objectType", "Rolproef"), faults);
            } finally {
                Files.deleteIfExists(ir);
                Files.deleteIfExists(c);
            }
        }
        // self-relation slot-exactness: V2.vorige = V1 leaves V1.vorige unlinked (allowed) and V1.volgende linked (EF-049)
        Path selfIr = Files.createTempFile("zelfrelatie", ".json"),
                selfCase = Files.createTempFile("zelfrelatie-case", ".json");
        try {
            Files.writeString(selfIr, selfRelationDocument(false));
            JsonNode allowed = selfRelationCase(List.of("vorige vergunning"), List.of("volgende vergunning"));
            for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
                List<String> verdicts = CaseRunner.playCases(selfIr, List.of(allowed), branch, false);
                if (verdicts.get(0) != null) faults.add(branch + " self-relation allowed | " + verdicts.get(0));
            }
            Files.writeString(selfCase, JSON.writeValueAsString(selfRelationCase(
                    List.of("volgende vergunning"), List.of())));
            expectFault(selfIr, selfCase, false, "self-relation conflict", "EF-049",
                    Map.of("label", "V1", "role", "volgende vergunning"), faults);
        } finally {
            Files.deleteIfExists(selfIr);
            Files.deleteIfExists(selfCase);
        }
        assertEquals(List.of(), faults);
    }

    @Test void non_to_one_reciprocal_and_zero_opposite_roles_are_refused_in_rolesEmpty() throws Exception {
        JsonNode ir = JSON.readTree(document(false, true));
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            DroolsBackend.Build build = DroolsBackend.build(ir, branch);
            // 0 opposite hits on Rolproef ("vreemde rol" is on Vreemd <-> Derde)
            IllegalArgumentException zeroHits = assertThrows(IllegalArgumentException.class,
                    () -> CaseInput.of(build, twoInstanceCase(Map.of(), List.of("vreemde rol"), Map.of())));
            assertTrue(zeroHits.getMessage().contains("rolesEmpty"), () -> branch + ": " + zeroHits.getMessage());

            // many: true role ("zeevondsten" on Jutter)
            IllegalArgumentException many = assertThrows(IllegalArgumentException.class,
                    () -> CaseInput.of(build, jutterEmptyCase(List.of("zeevondsten"))));
            assertTrue(many.getMessage().contains("non-reciprocal to-one") && many.getMessage().contains("rolesEmpty"),
                    () -> branch + ": " + many.getMessage());

            // reciprocal fact type ("partner" on Jutter)
            IllegalArgumentException reciprocal = assertThrows(IllegalArgumentException.class,
                    () -> CaseInput.of(build, jutterEmptyCase(List.of("partner"))));
            assertTrue(reciprocal.getMessage().contains("non-reciprocal to-one")
                    && reciprocal.getMessage().contains("rolesEmpty"),
                    () -> branch + ": " + reciprocal.getMessage());
        }
    }

    @Test void insert_takes_a_role_stated_empty_in_any_spelling_and_refuses_invalid_calls() throws Exception {
        List<String> faults = new ArrayList<>();
        List<String> allRoleNames = ROLES.stream().map(NamedRole::name).sorted().toList();
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            for (boolean ordinary : new boolean[]{false, true}) {
                DroolsBackend.Build build = DroolsBackend.build(JSON.readTree(document(ordinary, true)), branch);
                Class<?> trial = Class.forName(FactModelGenerator.PACKAGE + ".Rolproef", true, build.beanLoader());
                Class<?> sub = Class.forName(FactModelGenerator.PACKAGE + ".Strandrolproef", true, build.beanLoader());
                Class<?> jutterCls = Class.forName(FactModelGenerator.PACKAGE + ".Jutter", true, build.beanLoader());
                TmsRepeatedFireWitnessTest.inSession(build, () -> {
                    for (Class<?> cls : List.of(trial, sub))
                        for (NamedRole role : ROLES)
                            for (String spelling : List.of(role.name(), otherSpelling(role.name()), role.plural())) {
                                String what = branch + (ordinary ? " ordinary " : " strict ") + cls.getSimpleName()
                                        + " " + role.name() + " as «" + spelling + "»";
                                try {
                                    Object bean = cls.getConstructor().newInstance();
                                    List<String> found = openRolesAfter(build, new Input().bean(bean)
                                            .roleEmpty(bean, spelling).roleEmpty(bean, "tegenpartij"));
                                    List<String> expectedOpen = ordinary ? List.of()
                                            : allRoleNames.stream().filter(n -> !n.equals(role.name())).toList();
                                    if (!expectedOpen.equals(found))
                                        faults.add(what + " | open " + found + ", expected " + expectedOpen);
                                } catch (RuntimeException e) {
                                    faults.add(what + " | " + e);
                                }
                                Object linked = cls.getConstructor().newInstance();
                                Object j1 = jutterCls.getConstructor().newInstance();
                                cls.getMethod(Namen.setter(role.name()), jutterCls).invoke(linked, j1);
                                String refusal = DroolsCode.INSERT_EMPTY_ROLE.reason("bean", beanId(linked),
                                        "role", spelling, "reason", DroolsCode.INSERT_EMPTY_ROLE.part("linked")).text();
                                expectInsertRefusal(build, new Input().bean(linked).bean(j1).roleEmpty(linked, spelling),
                                        refusal, what + " own-linked", faults);
                            }

                    // 1:1 counterpart link set only on Jutter -> refuses roleEmpty on Rolproef as linked
                    Object v = trial.getConstructor().newInstance();
                    Object j = jutterCls.getConstructor().newInstance();
                    jutterCls.getMethod(Namen.setter("koppelingproef"), trial).invoke(j, v);
                    expectInsertRefusal(build, new Input().bean(v).bean(j).roleEmpty(v, "tegenpartij"),
                            DroolsCode.INSERT_EMPTY_ROLE.reason("bean", beanId(v), "role", "tegenpartij",
                                    "reason", DroolsCode.INSERT_EMPTY_ROLE.part("linked")).text(),
                            branch + " 1:1 counterpart-linked", faults);

                    Object inInput = trial.getConstructor().newInstance();
                    Object outsider = trial.getConstructor().newInstance();
                    expectInsertRefusal(build, new Input().bean(inInput).roleEmpty(outsider, "op zee"),
                            DroolsCode.INSERT_EMPTY_ROLE.reason("bean", beanId(outsider), "role", "op zee",
                                    "reason", DroolsCode.INSERT_EMPTY_ROLE.part("not-in-input")).text(),
                            branch + " not-in-input", faults);
                    expectInsertRefusal(build, new Input().bean(inInput).roleEmpty(inInput, "onbestaand"),
                            DroolsCode.INSERT_EMPTY_ROLE.reason("bean", beanId(inInput), "role", "onbestaand",
                                    "reason", DroolsCode.INSERT_EMPTY_ROLE.part("undeclared", "objectType", "Rolproef"))
                                    .text(),
                            branch + " undeclared", faults);
                    expectInsertRefusal(build, new Input().bean(inInput).roleEmpty(inInput, "vreemde rol"),
                            DroolsCode.INSERT_EMPTY_ROLE.reason("bean", beanId(inInput), "role", "vreemde rol",
                                    "reason", DroolsCode.INSERT_EMPTY_ROLE.part("undeclared", "objectType", "Rolproef"))
                                    .text(),
                            branch + " zero-opposite", faults);
                    expectInsertRefusal(build, new Input().bean(inInput).roleEmpty(inInput, "gedeeld"),
                            DroolsCode.INSERT_EMPTY_ROLE.reason("bean", beanId(inInput), "role", "gedeeld",
                                    "reason", DroolsCode.INSERT_EMPTY_ROLE.part("ambiguous", "objectType", "Rolproef"))
                                    .text(),
                            branch + " ambiguous", faults);

                    Object jIn = jutterCls.getConstructor().newInstance();
                    expectInsertRefusal(build, new Input().bean(jIn).roleEmpty(jIn, "zeevondsten"),
                            DroolsCode.INSERT_EMPTY_ROLE.reason("bean", beanId(jIn), "role", "zeevondsten",
                                    "reason", DroolsCode.INSERT_EMPTY_ROLE.part("not-to-one", "objectType", "Jutter"))
                                    .text(),
                            branch + " many-role", faults);
                    expectInsertRefusal(build, new Input().bean(jIn).roleEmpty(jIn, "partner"),
                            DroolsCode.INSERT_EMPTY_ROLE.reason("bean", beanId(jIn), "role", "partner",
                                    "reason", DroolsCode.INSERT_EMPTY_ROLE.part("not-to-one", "objectType", "Jutter"))
                                    .text(),
                            branch + " reciprocal-role", faults);
                });

                // self-relation direction independence on insert
                DroolsBackend.Build selfBuild = DroolsBackend.build(JSON.readTree(selfRelationDocument(ordinary)), branch);
                Class<?> vergCls = Class.forName(FactModelGenerator.PACKAGE + ".Vergunning", true, selfBuild.beanLoader());
                TmsRepeatedFireWitnessTest.inSession(selfBuild, () -> {
                    Object v1 = vergCls.getConstructor().newInstance();
                    Object v2 = vergCls.getConstructor().newInstance();
                    vergCls.getMethod(Namen.setter("vorige vergunning"), vergCls).invoke(v2, v1);
                    // V1.vorige vergunning is unlinked -> succeeds; V1.volgende vergunning is linked by V2 -> DROOLS-376 linked
                    List<String> openOnV1 = openRolesForLabel(selfBuild,
                            new Input().bean(v1).bean(v2).roleEmpty(v1, "vorige vergunning")
                                    .roleEmpty(v2, "volgende vergunning"),
                            Map.of(v1, "V1", v2, "V2"));
                    if (!openOnV1.isEmpty())
                        faults.add(branch + " self-relation open after closing both ends: " + openOnV1);
                    expectInsertRefusal(selfBuild, new Input().bean(v1).bean(v2).roleEmpty(v1, "volgende vergunning"),
                            DroolsCode.INSERT_EMPTY_ROLE.reason("bean", beanId(v1), "role", "volgende vergunning",
                                    "reason", DroolsCode.INSERT_EMPTY_ROLE.part("linked")).text(),
                            branch + " self-relation counterpart-linked", faults);
                });
            }
        }
        assertEquals(List.of(), faults);
    }

    private static void expectFault(Path ir, Path c, boolean ordinary, String what, String code,
                                    Map<String, String> params, List<String> faults) {
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            CaseRunner.RedResult r = CaseRunner.runRed(ir, c, branch);
            if (!"FAULT".equals(r.status()) || !code.equals(r.code()) || !List.of(params).equals(r.params()))
                faults.add(branch + (ordinary ? " ordinary " : " strict ") + what + " | "
                        + r.status() + " " + r.code() + " " + r.params() + ": " + r.detail());
        }
    }

    private static void expectInsertRefusal(DroolsBackend.Build b, Input input, String expectedMessage, String what,
                                            List<String> faults) {
        KieSession session = b.kieBase().newKieSession();
        try {
            DroolsBackend.insert(b, session, input);
            faults.add(what + " | insert accepted an invalid Input.roleEmpty");
        } catch (IllegalArgumentException e) {
            if (!expectedMessage.equals(e.getMessage()))
                faults.add(what + " | got «" + e.getMessage() + "», expected «" + expectedMessage + "»");
        } catch (RuntimeException e) {
            faults.add(what + " | " + e);
        } finally {
            session.dispose();
        }
    }

    private static List<String> openRolesAfter(DroolsBackend.Build b, Input input) {
        KieSession session = b.kieBase().newKieSession();
        try {
            DroolsBackend.insert(b, session, input);
            assertNull(DroolsBackend.fire(b, session).noFixpoint(), "the fixpoint");
            List<String> out = new ArrayList<>();
            for (EndState.OpenLink k : DroolsBackend.endStateOf(b, session, x -> "V1").openLinks())
                if (!k.factType().startsWith("dubbel"))
                    out.add(k.first() == null ? k.firstRole() : k.secondRole());
            out.sort(null);
            return out;
        } finally {
            session.dispose();
        }
    }

    private static List<String> openRolesForLabel(DroolsBackend.Build b, Input input, Map<Object, String> labels) {
        KieSession session = b.kieBase().newKieSession();
        try {
            DroolsBackend.insert(b, session, input);
            assertNull(DroolsBackend.fire(b, session).noFixpoint(), "the fixpoint");
            List<String> out = new ArrayList<>();
            for (EndState.OpenLink k : DroolsBackend.endStateOf(b, session, labels::get).openLinks())
                out.add(k.factType() + ":" + (k.first() == null ? "open->" + labels.get(k.second())
                        : labels.get(k.first()) + "->open"));
            return out;
        } finally {
            session.dispose();
        }
    }

    private static List<String> play(boolean ordinary, boolean unnamedIsOpen) throws Exception {
        List<String> faults = new ArrayList<>();
        Path file = Files.createTempFile("rolnamen-play", ".json");
        try {
            Files.writeString(file, document(ordinary, false));
            for (NamedRole role : ROLES) {
                List<String> othersEmpty = ROLES.stream().map(NamedRole::name).filter(n -> !n.equals(role.name())).toList();
                List<String> statedSingle = new ArrayList<>(othersEmpty);
                statedSingle.add(role.name());
                List<String> statedMany = new ArrayList<>(othersEmpty);
                statedMany.addAll(List.of(role.name(), otherSpelling(role.name()), role.plural()));

                String chName = "heeft " + role.factType();
                List<Map<String, Object>> expectSettled = List.of(
                        Map.of("kind", "characteristicAbsent", "label", "V1", "characteristic", chName));
                List<Map<String, Object>> expectUnnamed = List.of(
                        Map.of("kind", unnamedIsOpen ? "characteristicOpen" : "characteristicAbsent",
                                "label", "V1", "characteristic", chName));

                List<JsonNode> cases = List.of(
                        singleInstanceCase(statedSingle, expectSettled),
                        singleInstanceCase(statedMany, expectSettled),
                        singleInstanceCase(othersEmpty, expectUnnamed));
                for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
                    try {
                        List<String> verdicts = CaseRunner.playCases(file, cases, branch, false);
                        for (int i = 0; i < verdicts.size(); i++)
                            if (verdicts.get(i) != null)
                                faults.add(branch + " " + role.name() + " case " + (i + 1) + " | " + verdicts.get(i));
                    } catch (RuntimeException e) {
                        faults.add(branch + " " + role.name() + " | " + e);
                    }
                }
            }
        } finally {
            Files.deleteIfExists(file);
        }
        return faults;
    }

    private static String otherSpelling(String name) {
        StringBuilder out = new StringBuilder(" ");
        for (char c : name.toCharArray())
            out.append(c == ' ' ? "  " : c >= 'a' && c <= 'z' ? String.valueOf((char) (c - 32))
                    : c >= 'A' && c <= 'Z' ? String.valueOf((char) (c + 32)) : String.valueOf(c));
        return out.toString();
    }

    private static JsonNode singleInstanceCase(List<String> rolesEmpty, List<Map<String, Object>> expected) {
        Map<String, Object> v1 = new LinkedHashMap<>();
        v1.put("label", "V1");
        v1.put("objectType", "Rolproef");
        v1.put("attributes", Map.of());
        v1.put("roles", Map.of());
        if (!rolesEmpty.isEmpty()) v1.put("rolesEmpty", rolesEmpty);
        v1.put("characteristics", List.of());
        return wrapCase(List.of(v1), expected);
    }

    private static JsonNode twoInstanceCase(Map<String, Object> v1Roles, List<String> v1RolesEmpty,
                                            Map<String, Object> j1Roles) {
        Map<String, Object> j1 = new LinkedHashMap<>();
        j1.put("label", "J1");
        j1.put("objectType", "Jutter");
        j1.put("attributes", Map.of());
        j1.put("roles", j1Roles);
        j1.put("characteristics", List.of());

        Map<String, Object> v1 = new LinkedHashMap<>();
        v1.put("label", "V1");
        v1.put("objectType", "Rolproef");
        v1.put("attributes", Map.of());
        v1.put("roles", v1Roles);
        if (!v1RolesEmpty.isEmpty()) v1.put("rolesEmpty", v1RolesEmpty);
        v1.put("characteristics", List.of());
        return wrapCase(List.of(j1, v1), List.of());
    }

    private static JsonNode jutterEmptyCase(List<String> j1RolesEmpty) {
        Map<String, Object> j1 = new LinkedHashMap<>();
        j1.put("label", "J1");
        j1.put("objectType", "Jutter");
        j1.put("attributes", Map.of());
        j1.put("roles", Map.of());
        j1.put("rolesEmpty", j1RolesEmpty);
        j1.put("characteristics", List.of());
        return wrapCase(List.of(j1), List.of());
    }

    private static JsonNode selfRelationCase(List<String> v1RolesEmpty, List<String> v2RolesEmpty) {
        Map<String, Object> v1 = new LinkedHashMap<>();
        v1.put("label", "V1");
        v1.put("objectType", "Vergunning");
        v1.put("attributes", Map.of());
        v1.put("roles", Map.of());
        if (!v1RolesEmpty.isEmpty()) v1.put("rolesEmpty", v1RolesEmpty);
        v1.put("characteristics", List.of());

        Map<String, Object> v2 = new LinkedHashMap<>();
        v2.put("label", "V2");
        v2.put("objectType", "Vergunning");
        v2.put("attributes", Map.of());
        v2.put("roles", Map.of("vorige vergunning", List.of("V1")));
        if (!v2RolesEmpty.isEmpty()) v2.put("rolesEmpty", v2RolesEmpty);
        v2.put("characteristics", List.of());
        return wrapCase(List.of(v1, v2), List.of(Map.of("kind", "consistent")));
    }

    private static JsonNode wrapCase(List<Map<String, Object>> instances, List<Map<String, Object>> expected) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("format", "uchar-case-v0");
        c.put("asOfDate", null);
        c.put("input", Map.of("instances", instances, "parameters", Map.of()));
        c.put("expected", expected);
        return JSON.valueToTree(c);
    }

    private static String document(boolean ordinary, boolean withExtraShapes) throws Exception {
        List<Map<String, Object>> characteristics = new ArrayList<>();
        List<Map<String, Object>> factTypes = new ArrayList<>();
        List<Map<String, Object>> rules = new ArrayList<>();
        for (NamedRole r : ROLES) {
            String chName = "heeft " + r.factType();
            characteristics.add(Map.of("node", "Characteristic", "name", chName, "kind", "adjectival"));
            factTypes.add(Map.of(
                    "node", "FactType",
                    "name", r.factType(),
                    "reciprocal", false,
                    "roles", List.of(
                            Map.of("node", "Role", "name", r.name(), "objectType", "Jutter",
                                    "plural", r.plural(), "many", false),
                            Map.of("node", "Role", "name", r.nearRole(), "objectType", "Rolproef",
                                    "plural", r.nearRole() + "en", "many", true))));
            rules.add(Map.of(
                    "node", "Rule",
                    "name", "regel " + r.factType(),
                    "versions", List.of(Map.of(
                            "validity", Map.of("node", "Validity"),
                            "target", Map.of("node", "AttributeReference", "attribute", chName,
                                    "subjectType", "Rolproef"),
                            "value", Map.of("node", "Literal", "value", Map.of("boolean", true),
                                    "type", Map.of("node", "Boolean")),
                            "condition", Map.of("node", "RoleMembership", "factType", r.factType(),
                                    "role", r.nearRole(), "negated", false),
                            "initialisation", false))));
        }

        List<Map<String, Object>> objectTypes = new ArrayList<>();
        objectTypes.add(Map.of(
                "node", "ObjectType", "name", "Rolproef", "animate", false, "plural", "Rolproeven",
                "attributes", List.of(), "characteristics", characteristics));
        objectTypes.add(Map.of(
                "node", "ObjectType", "name", "Strandrolproef", "supertype", "Rolproef", "animate", false,
                "plural", "Strandrolproeven", "attributes", List.of(), "characteristics", List.of()));
        objectTypes.add(Map.of(
                "node", "ObjectType", "name", "Jutter", "animate", true, "plural", "Jutters",
                "attributes", List.of(), "characteristics", List.of()));

        if (withExtraShapes) {
            objectTypes.add(Map.of(
                    "node", "ObjectType", "name", "Vreemd", "animate", false, "plural", "Vreemden",
                    "attributes", List.of(), "characteristics", List.of()));
            objectTypes.add(Map.of(
                    "node", "ObjectType", "name", "Derde", "animate", false, "plural", "Derden",
                    "attributes", List.of(), "characteristics", List.of()));
            // 0 opposite hits on Rolproef
            factTypes.add(Map.of("node", "FactType", "name", "vreemdvinding", "reciprocal", false,
                    "roles", List.of(
                            Map.of("node", "Role", "name", "vreemde rol", "objectType", "Derde", "many", false),
                            Map.of("node", "Role", "name", "vreemdeling", "objectType", "Vreemd", "many", false))));
            // >1 opposite hits on Rolproef ("gedeeld" on two fact types against Derde and Vreemd so Jutter bean fields stay unique)
            factTypes.add(Map.of("node", "FactType", "name", "dubbel een", "reciprocal", false,
                    "roles", List.of(
                            Map.of("node", "Role", "name", "gedeeld", "objectType", "Derde", "many", false),
                            Map.of("node", "Role", "name", "eerste proef", "objectType", "Rolproef", "many", true))));
            factTypes.add(Map.of("node", "FactType", "name", "dubbel twee", "reciprocal", false,
                    "roles", List.of(
                            Map.of("node", "Role", "name", "gedeeld", "objectType", "Vreemd", "many", false),
                            Map.of("node", "Role", "name", "tweede proef", "objectType", "Rolproef", "many", true))));
            // reciprocal fact type on Jutter
            factTypes.add(Map.of("node", "FactType", "name", "partnerschap", "reciprocal", true,
                    "roles", List.of(
                            Map.of("node", "Role", "name", "partner", "objectType", "Jutter",
                                    "plural", "partners", "many", false))));
            // 1:1 relation between Jutter and Rolproef
            factTypes.add(Map.of("node", "FactType", "name", "eenopeen", "reciprocal", false,
                    "roles", List.of(
                            Map.of("node", "Role", "name", "tegenpartij", "objectType", "Jutter", "many", false),
                            Map.of("node", "Role", "name", "koppelingproef", "objectType", "Rolproef", "many", false))));
        }

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("objectTypes", objectTypes);
        model.put("factTypes", factTypes);
        for (String k : List.of("parameters", "domains", "unitSystems", "dimensions", "timelines", "dayKinds"))
            model.put(k, List.of());

        Map<String, Object> rulesSection = new LinkedHashMap<>();
        rulesSection.put("rules", rules);
        for (String k : List.of("dayKindDefinitions", "objectCreations", "allocations", "consistencies",
                "uniquenesses", "factCreations", "startPointDeterminations", "ruleGroups"))
            rulesSection.put(k, List.of());

        Map<String, Object> d = new LinkedHashMap<>();
        d.put("format", "uchar-ir-v1");
        if (!ordinary) d.put("inferFromEmpty", false);
        d.put("model", model);
        d.put("rules", rulesSection);
        d.put("diagnostics", List.of());
        return JSON.writeValueAsString(d);
    }

    private static String selfRelationDocument(boolean ordinary) {
        return """
                {
                  "format": "uchar-ir-v1",
                """ + (ordinary ? "" : "  \"inferFromEmpty\": false,\n") + """
                  "model": {
                    "objectTypes": [
                      {
                        "node": "ObjectType",
                        "name": "Vergunning",
                        "animate": false,
                        "plural": "Vergunningen",
                        "attributes": [],
                        "characteristics": []
                      }
                    ],
                    "factTypes": [
                      {
                        "node": "FactType",
                        "name": "opvolging",
                        "reciprocal": false,
                        "roles": [
                          { "node": "Role", "name": "vorige vergunning", "objectType": "Vergunning", "many": false },
                          { "node": "Role", "name": "volgende vergunning", "objectType": "Vergunning", "many": false }
                        ]
                      }
                    ],
                    "parameters": [],
                    "domains": [],
                    "unitSystems": [],
                    "dimensions": [],
                    "timelines": [],
                    "dayKinds": []
                  },
                  "rules": {
                    "rules": [],
                    "dayKindDefinitions": [],
                    "objectCreations": [],
                    "allocations": [],
                    "consistencies": [],
                    "uniquenesses": [],
                    "factCreations": [],
                    "startPointDeterminations": [],
                    "ruleGroups": []
                  },
                  "diagnostics": []
                }
                """;
    }

    private static String beanId(Object o) {
        return o.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(o));
    }
}
