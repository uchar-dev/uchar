package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.core.UnitConflict;
import dev.uchar.regelspraak.transpiler.tms.ComputeFault;
import dev.uchar.regelspraak.transpiler.tms.ConditionReading;
import dev.uchar.regelspraak.transpiler.tms.DefaultsDecision;
import dev.uchar.regelspraak.transpiler.tms.Fixpoint;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieSession;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsDocuments.aggregation;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribute;
import static dev.uchar.regelspraak.cases.TmsDocuments.attributeGroup;
import static dev.uchar.regelspraak.cases.TmsDocuments.binary;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicGroup;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.comparison;
import static dev.uchar.regelspraak.cases.TmsDocuments.compound;
import static dev.uchar.regelspraak.cases.TmsDocuments.contextValue;
import static dev.uchar.regelspraak.cases.TmsDocuments.decision;
import static dev.uchar.regelspraak.cases.TmsDocuments.defaultEntry;
import static dev.uchar.regelspraak.cases.TmsDocuments.factTypeOneToMany;
import static dev.uchar.regelspraak.cases.TmsDocuments.literal;
import static dev.uchar.regelspraak.cases.TmsDocuments.model;
import static dev.uchar.regelspraak.cases.TmsDocuments.modelWith;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.numberType;
import static dev.uchar.regelspraak.cases.TmsDocuments.objectType;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.predicate;
import static dev.uchar.regelspraak.cases.TmsDocuments.sourceRole;
import static dev.uchar.regelspraak.cases.TmsDocuments.subtype;
import static dev.uchar.regelspraak.cases.TmsDocuments.unit;
import static dev.uchar.regelspraak.cases.TmsDocuments.unitSystem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Witnesses of RUNTIME-SEMANTICS ch. 1.5 ("An open default with a constant") and {@link DefaultsDecision#open()} on
 * both branches ({@code EXEC_MODEL} and {@code DRL_TEXT}), with and without readings:
 *
 * <ul>
 *   <li>Unit restatement and the write boundary on constant open defaults: {@code 1} (unitless) vs {@code 1 ct}
 *       (differs, stays open), {@code 1} vs {@code 100 ct} (equal in {@code €}, decides {@code 1}), {@code 0.005}
 *       failing a 2-decimal target's write boundary (stays {@code OPEN}, displacing what stands below), and an
 *       incompatible unit {@code 1 kg} on a {@code €} target (refused at build time).</li>
 *   <li>Provisional fold filter deposit and fault below a constant open default: EF-005 suppressed while provisional and
 *       deposited once the higher default gives way; EF-011 turned into {@code OPEN} while provisional, excluded from
 *       {@link DefaultsDecision#open()}, and raised once the higher default gives way.</li>
 *   <li>{@link DefaultsDecision#open()} on characteristic root closing alone, beside an open supertype rule and beside
 *       input {@code holds}, on {@code jutterswet-zelfde-conclusie-ongeordend} ({@code havenloon} case 5, excluding the
 *       harmless {@code OPEN_ONLY(0)} member), on {@code jutterswet-zelfde-conclusie-fout} (excluding the provisionally
 *       faulted base while its reading carries {@code OPEN} and EF-011), and on genuine open reads below
 *       {@code OPEN_ONLY}.</li>
 * </ul>
 */
class SameConclusionWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final String ALL_TRUE = compound("all", 0, List.of());

    /** Unit-converted OPEN_ONLY, write-boundary rejection of OPEN_ONLY, and build-time refusal on incompatible units. */
    @Test void unit_conversion_and_write_boundary() throws Exception {
        String eur = unit("€", "Valuta", "€", 1, 1);
        String ct = unit("ct", "Valuta", "€", 1, 100);
        String kg = unit("kg", "Gewicht", "kg", 1, 1);
        String eur2 = numberType(false, 2, eur);
        String ctType = numberType(true, 0, ct);
        String dec3 = numberType(false, 3, eur);
        String kgType = numberType(true, 0, kg);
        String m = modelWith(List.of(objectType("Vondst",
                List.of(attribute("premie", eur2), attribute("toeslag", eur2), attribute("boete", eur2)),
                List.of("betwist"))), List.of(), List.of(),
                List.of(unitSystem("Valuta", List.of(eur, ct)), unitSystem("Gewicht", List.of(kg))));
        Path ir = strictDocument(m, List.of(), List.of(
                // 1 (unitless) above 1 ct -> 1 € != 0.01 €, stays open
                attributeGroup("premie", "Vondst", "premie", List.of(
                        defaultEntry(check("betwist", false), number("1")),
                        defaultEntry(ALL_TRUE, literal("1", ctType)))),
                // 1 (unitless) above 100 ct -> 1 € == 1.00 €, decides 1
                attributeGroup("toeslag", "Vondst", "toeslag", List.of(
                        defaultEntry(check("betwist", false), number("1")),
                        defaultEntry(ALL_TRUE, literal("100", ctType)))),
                // 0.005 on 2-decimal target fails write boundary -> OPEN (not OPEN_ONLY), lower default DISPLACED
                attributeGroup("boete", "Vondst", "boete", List.of(
                        defaultEntry(check("betwist", false), literal("0.005", dec3)),
                        defaultEntry(ALL_TRUE, literal("0.005", dec3))))));
        JsonNode openCase = strictCase(null, List.of(
                strictInstance("V1", "Vondst", Map.of(), List.of(), List.of(), Map.of()),
                strictInstance("V2", "Vondst", Map.of(), List.of(), List.of("betwist"), Map.of())));
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            Run r = run(ir, openCase, branch, true);
            // V1: betwist is open
            assertTrue(r.isOpen("V1", "premie"), branch + ": 1 vs 1 ct stays open");
            DefaultsDecision dPremie = r.decision("V1", "premie");
            assertNull(dPremie.decider(), branch.toString());
            assertEquals(List.of("/rules/prioritizedDefaults/0/defaults/0"), dPremie.open(), branch.toString());

            assertFalse(r.isOpen("V1", "toeslag"), branch + ": 1 vs 100 ct decides");
            assertEquals(0, BigDecimal.ONE.compareTo((BigDecimal) r.derivedValue("V1", "toeslag")), branch.toString());
            DefaultsDecision dToeslag = r.decision("V1", "toeslag");
            assertEquals("/rules/prioritizedDefaults/1/defaults/1", dToeslag.decider().pointer(), branch.toString());
            assertEquals(List.of(), dToeslag.open(), branch.toString());

            assertTrue(r.isOpen("V1", "boete"), branch + ": 0.005 fails write boundary, stays open");
            assertEquals(ConditionReading.Status.DISPLACED,
                    r.reading("V1", "/rules/prioritizedDefaults/2/defaults/1").status(),
                    branch + ": lower default under non-fitting literal is DISPLACED");

            // V2: betwist absent -> premie 0.01, toeslag 1.00
            assertEquals(0, new BigDecimal("0.01").compareTo((BigDecimal) r.derivedValue("V2", "premie")),
                    branch.toString());
            assertEquals(0, BigDecimal.ONE.compareTo((BigDecimal) r.derivedValue("V2", "toeslag")), branch.toString());

            // 1 kg on € target is refused at build time
            Path bad = strictDocument(m, List.of(), List.of(
                    attributeGroup("premie", "Vondst", "premie", List.of(
                            defaultEntry(check("betwist", false), literal("1", kgType)),
                            defaultEntry(ALL_TRUE, number("1"))))));
            assertThrows(UnitConflict.class, () -> DroolsBackend.build(bad, branch, false), branch.toString());
        }
    }

    /** Provisional fold filter deposit (EF-005) and fold filter fault (EF-011) below a constant open default. */
    @Test void provisional_fold_filter_deposit_and_fault() throws Exception {
        String ft = factTypeOneToMany("vondst van jutter", "vinder", "Jutter", "vondst", "Vondst", "vondsten")
                .replace("\"many\":true", "\"many\":true,\"complete\":true");
        String m = model(List.of(
                objectType("Jutter", List.of(attribute("premie", WHOLE), attribute("toeslag", WHOLE)),
                        List.of("betwist")),
                objectType("Vondst", List.of(attribute("gewicht", WHOLE), attribute("getuigen", WHOLE)), List.of())),
                List.of(ft));
        String divByZeroEmpty = predicate("empty",
                binary("dividedBy", ownRead("gewicht", "Vondst"), ownRead("getuigen", "Vondst")));
        String countDeposit = aggregation("sum", ownRead("gewicht", "Vondst"), "Vondst",
                sourceRole("vondst van jutter", "vondst"), divByZeroEmpty, "null", WHOLE,
                "som van het gewicht van vondsten met lege deling");
        String countFault = aggregation("sum", ownRead("gewicht", "Vondst"), "Vondst",
                sourceRole("vondst van jutter", "vondst"),
                comparison("greater", contextValue("evaluationYear", WHOLE), number("2020")), "null",
                WHOLE, "som van het gewicht van vondsten na 2020");

        Path depositIr = strictDocument(m, List.of(), List.of(
                attributeGroup("premie", "Jutter", "premie", List.of(
                        defaultEntry(check("betwist", false), number("2")),
                        defaultEntry(comparison("greater", countDeposit, number("0")), number("2"))))));
        Path faultIr = strictDocument(m, List.of(), List.of(
                attributeGroup("toeslag", "Jutter", "toeslag", List.of(
                        defaultEntry(check("betwist", false), number("2")),
                        defaultEntry(comparison("greater", countFault, number("0")), number("2"))))));

        JsonNode openInput = strictCase(null, List.of(
                strictInstance("J1", "Jutter", Map.of(), List.of(), List.of(), Map.of("vondsten", List.of("V1"))),
                strictInstance("V1", "Vondst", Map.of("gewicht", "10", "getuigen", "0"), List.of(), List.of(), Map.of())));
        JsonNode absentInput = strictCase(null, List.of(
                strictInstance("J1", "Jutter", Map.of(), List.of(), List.of("betwist"), Map.of("vondsten", List.of("V1"))),
                strictInstance("V1", "Vondst", Map.of("gewicht", "10", "getuigen", "0"), List.of(), List.of(), Map.of())));

        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            // Deposit: suppressed when provisional, present when betwist stands not to hold
            Run depOpen = run(depositIr, openInput, branch, true);
            assertEquals(0, BigDecimal.TWO.compareTo((BigDecimal) depOpen.derivedValue("J1", "premie")),
                    branch.toString());
            assertTrue(depOpen.fixpoint.inconsistencies().isEmpty(), branch + ": provisional fold filter deposit dropped");
            assertEquals(List.of(), depOpen.decision("J1", "premie").open(), branch.toString());

            Run depReal = run(depositIr, absentInput, branch, true);
            assertEquals(0, BigDecimal.TWO.compareTo((BigDecimal) depReal.derivedValue("J1", "premie")),
                    branch.toString());
            assertEquals(1, depReal.fixpoint.inconsistencies().size(), branch + ": real fold filter deposit stands");

            // Fault: turned into OPEN without session fault when provisional; faults with EF-011 when real
            Run fltOpen = run(faultIr, openInput, branch, true);
            assertTrue(fltOpen.fixpoint.faults().isEmpty(), branch + ": provisional fold filter fault suppressed");
            assertTrue(fltOpen.isOpen("J1", "toeslag"), branch.toString());
            DefaultsDecision dFlt = fltOpen.decision("J1", "toeslag");
            assertNull(dFlt.decider(), branch.toString());
            assertEquals(List.of("/rules/prioritizedDefaults/0/defaults/0"), dFlt.open(),
                    branch + ": provisionally faulted default excluded from DefaultsDecision.open()");
            ConditionReading rFlt = fltOpen.reading("J1", "/rules/prioritizedDefaults/0/defaults/1");
            assertEquals(ConditionReading.Status.OPEN, rFlt.status(), branch.toString());

            Run fltReal = run(faultIr, absentInput, branch, false);
            assertEquals(1, fltReal.fixpoint.faults().size(), branch + ": real fold filter fault raised");
        }
    }

    /** Characteristic root closing beside an open supertype rule and beside input {@code holds}: {@code d.open()}
     *  distinguishes a root-closed stack ({@code []}) from an open stack ({@code [ptr]}). */
    @Test void decisions_open_on_characteristic_root_closing_beside_supertype_rule_and_input() throws Exception {
        String m = model(List.of(
                objectType("Vondst", List.of(attribute("gewicht", WHOLE)), List.of("betwist", "beloonbaar")),
                subtype("Wrakvondst", "Vondst", List.of(attribute("diepte", WHOLE)), List.of())), List.of());
        String superRule = characteristicRule("beloonbaar algemeen", "Vondst", "beloonbaar",
                comparison("greater", ownRead("gewicht", "Vondst"), number("2")));
        Path closedStack = strictDocument(m, List.of(superRule), List.of(
                characteristicGroup("wrakbeloonbaarheid", "Wrakvondst", "beloonbaar", List.of(
                        decision(check("betwist", false), false),
                        decision(comparison("greater", ownRead("diepte", "Wrakvondst"), number("2")), true)))));
        Path openStack = strictDocument(m, List.of(superRule), List.of(
                characteristicGroup("wrakbeloonbaarheid", "Wrakvondst", "beloonbaar", List.of(
                        decision(check("betwist", false), true),
                        decision(comparison("greater", ownRead("diepte", "Wrakvondst"), number("2")), true)))));
        JsonNode c = strictCase(null, List.of(
                // W1: diepte 1 (base fails), betwist open, gewicht open (supertype rule open)
                strictInstance("W1", "Wrakvondst", Map.of("diepte", "1"), List.of(), List.of(), Map.of()),
                // W2: diepte 1 (base fails), betwist open, gewicht 1 (supertype rule fails), beloonbaar holds in input
                strictInstance("W2", "Wrakvondst", Map.of("diepte", "1", "gewicht", "1"), List.of("beloonbaar"),
                        List.of(), Map.of())));

        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            Run closed = run(closedStack, c, branch, true);
            assertTrue(closed.isOpen("W1", "beloonbaar"), branch + ": supertype rule keeps W1 open");
            assertNull(closed.decision("W1", "beloonbaar").decider(), branch.toString());
            assertEquals(List.of(), closed.decision("W1", "beloonbaar").open(),
                    branch + ": root-closed stack has empty d.open() beside open supertype rule");
            assertFalse(closed.isOpen("W2", "beloonbaar"), branch + ": input holds on W2");
            assertEquals(List.of(), closed.decision("W2", "beloonbaar").open(),
                    branch + ": root-closed stack has empty d.open() beside input holds");

            Run open = run(openStack, c, branch, true);
            assertTrue(open.isOpen("W1", "beloonbaar"), branch.toString());
            assertEquals(List.of("/rules/prioritizedDefaults/0/defaults/0"), open.decision("W1", "beloonbaar").open(),
                    branch + ": OPEN_ONLY(true) stack keeps d.open() populated beside open supertype rule");
            assertFalse(open.isOpen("W2", "beloonbaar"), branch.toString());
            assertEquals(List.of("/rules/prioritizedDefaults/0/defaults/0"), open.decision("W2", "beloonbaar").open(),
                    branch + ": OPEN_ONLY(true) stack keeps d.open() populated beside input holds");
        }
    }

    /** {@link DefaultsDecision#open()} on the corpus goldens: unordered group excluding harmless {@code OPEN_ONLY(v)}
     *  ({@code havenloon} case 5), provisional fault ({@code jutterswet-zelfde-conclusie-fout}), same-conclusion
     *  decider ({@code jutterswet-zelfde-conclusie-waarde}), genuine open read below {@code OPEN_ONLY}, and
     *  characteristic root closing ({@code jutterswet-zelfde-conclusie-kenmerk}). */
    @Test void decisions_open_on_goldens_and_genuine_open_below_open_only() throws Exception {
        Path unorderedDir = GOLDEN.resolve("jutterswet-zelfde-conclusie-ongeordend");
        JsonNode unorderedCase5 = TmsDocuments.JSON.readTree(Files.readString(unorderedDir.resolve("case5-gemeld-open.json")));
        Path faultDir = GOLDEN.resolve("jutterswet-zelfde-conclusie-fout");
        JsonNode faultCase1 = TmsDocuments.JSON.readTree(Files.readString(faultDir.resolve("case1-betwist-open.json")));
        Path valueDir = GOLDEN.resolve("jutterswet-zelfde-conclusie-waarde");
        JsonNode valueCase1 = TmsDocuments.JSON.readTree(Files.readString(valueDir.resolve("case1-betwist-open.json")));
        JsonNode valueBothOpen = strictCase(null, List.of(
                strictInstance("V1", "Vondst", Map.of(), List.of(), List.of("beschadigd", "gemeld"), Map.of())));
        Path charDir = GOLDEN.resolve("jutterswet-zelfde-conclusie-kenmerk");
        JsonNode charCase5 = TmsDocuments.JSON.readTree(Files.readString(charDir.resolve("case5-wortel-betwist-open.json")));

        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            // havenloon case 5: {OPEN 0 (betwist), HOLDS 0 (beschadigd), OPEN 5 (gemeld)} -> only gemeld in d.open()
            Run u = run(unorderedDir.resolve("ir.json"), unorderedCase5, branch, true);
            DefaultsDecision haven = u.decision("V1", "havenloon");
            assertNull(haven.decider(), branch.toString());
            assertNull(haven.conflict(), branch.toString());
            assertEquals(List.of(haven.rootChains().get(2).get(0)), haven.open(),
                    branch + ": harmless OPEN_ONLY(0) excluded, blocking OPEN_ONLY(5) included");

            // jutterswet-zelfde-conclusie-fout case 1: rekenloon and werkloon exclude provisionally faulted base
            Run f = run(faultDir.resolve("ir.json"), faultCase1, branch, true);
            assertTrue(f.fixpoint.faults().isEmpty(), branch.toString());
            for (String member : List.of("rekenloon", "werkloon")) {
                DefaultsDecision d = f.decision("V1", member);
                assertNull(d.decider(), branch + " " + member);
                assertEquals(List.of(d.rootChains().get(0).get(0)), d.open(), branch + " " + member);
                String basePtr = d.rootChains().get(0).get(1);
                ConditionReading baseReading = f.reading("V1", basePtr);
                assertEquals(ConditionReading.Status.OPEN, baseReading.status(), branch + " " + member);
                List<ConditionReading.NodeReading> nodes = baseReading.nodes();
                int rootIdx = member.equals("rekenloon") ? 0 : 1;
                assertTrue(nodes.size() > rootIdx, branch + " " + member);
                assertNotNull(nodes.get(rootIdx).fault(), branch + " " + member + ": root node carries provisional fault");
                assertTrue(nodes.get(rootIdx).fault() instanceof ComputeFault, branch + " " + member);
                assertFalse(nodes.get(rootIdx).undetermined(), branch + " " + member);
            }

            // jutterswet-zelfde-conclusie-waarde case 1: vindloon decides 0 via lower default, d.open() empty
            Run v1 = run(valueDir.resolve("ir.json"), valueCase1, branch, true);
            DefaultsDecision vind = v1.decision("V1", "vindloon");
            assertNotNull(vind.decider(), branch.toString());
            assertEquals(vind.rootChains().get(0).get(1), vind.decider().pointer(), branch.toString());
            assertEquals(0, BigDecimal.ZERO.compareTo((BigDecimal) vind.decider().decision()), branch.toString());
            assertEquals(List.of(), vind.open(), branch.toString());

            // value stack with genuine open read below OPEN_ONLY: both betwist and base in d.open() in stack order
            Run vBoth = run(valueDir.resolve("ir.json"), valueBothOpen, branch, true);
            DefaultsDecision vindOpen = vBoth.decision("V1", "vindloon");
            assertNull(vindOpen.decider(), branch.toString());
            assertEquals(List.of(vindOpen.rootChains().get(0).get(0), vindOpen.rootChains().get(0).get(2)),
                    vindOpen.open(), branch + ": OPEN_ONLY and genuine open base both in stack order");

            // jutterswet-zelfde-conclusie-kenmerk case 5: beloonbaar and keurbaar close at root, d.open() empty
            Run ch = run(charDir.resolve("ir.json"), charCase5, branch, true);
            for (String member : List.of("beloonbaar", "keurbaar")) {
                DefaultsDecision d = ch.decision("V1", member);
                assertNull(d.decider(), branch + " " + member);
                assertNull(d.conflict(), branch + " " + member);
                assertEquals(List.of(), d.open(), branch + " " + member);
                assertFalse(ch.isOpen("V1", member), branch + " " + member);
            }
        }
    }

    // ---------------- helpers ----------------

    private record Run(Fixpoint fixpoint, Map<Object, String> labelOf, List<DefaultsDecision> decisions,
                       List<ConditionReading> readings) {
        boolean isOpen(String label, String member) {
            return fixpoint.open().stream()
                    .anyMatch(o -> label.equals(labelOf.get(o.object())) && member.equals(o.member()));
        }

        Object derivedValue(String label, String attribute) {
            return fixpoint.values().stream()
                    .filter(v -> label.equals(labelOf.get(v.object())) && attribute.equals(v.attribute()))
                    .map(Fixpoint.DerivedValue::value)
                    .findFirst().orElse(null);
        }

        DefaultsDecision decision(String label, String member) {
            return decisions.stream()
                    .filter(d -> label.equals(labelOf.get(d.object())) && member.equals(d.member()))
                    .findFirst().orElseThrow(() -> new AssertionError("no decision for " + label + " " + member));
        }

        ConditionReading reading(String label, String pointer) {
            return readings.stream()
                    .filter(r -> label.equals(labelOf.get(r.object())) && pointer.equals(r.form()))
                    .findFirst().orElseThrow(() -> new AssertionError("no reading for " + label + " " + pointer));
        }
    }

    private static Run run(Path ir, JsonNode caseJson, DroolsBackend.Branch branch, boolean withReadings)
            throws Exception {
        DroolsBackend.Build build = DroolsBackend.build(ir, branch, withReadings);
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(build.beanLoader());
        KieSession session = build.kieBase().newKieSession();
        try {
            CaseInput input = CaseInput.of(build, caseJson);
            if (!caseJson.path("asOfDate").isNull() && !caseJson.path("asOfDate").isMissingNode()) {
                var date = java.time.LocalDate.parse(caseJson.get("asOfDate").asText());
                if (build.kieBase().getKiePackages().stream().anyMatch(p -> p.getGlobalVariables().stream()
                        .anyMatch(g -> "evaluationDate".equals(g.getName()))))
                    session.setGlobal("evaluationDate", date);
            }
            if (branch == DroolsBackend.Branch.DRL_TEXT)
                build.dayKindGlobals().forEach((k, v) -> session.setGlobal(k, v));
            DroolsBackend.insert(build, session, input.inputData());
            DroolsBackend.Firing firing = DroolsBackend.fire(build, session);
            assertNull(firing.noFixpoint(), branch + ": fixpoint");
            Map<Object, String> labelOf = new IdentityHashMap<>();
            input.beans().forEach((l, o) -> labelOf.put(o, l));
            Fixpoint fp = DroolsBackend.fixpointOf(build, session, labelOf::get);
            List<DefaultsDecision> decs = DroolsBackend.decisionsOf(build, session);
            List<ConditionReading> reads = withReadings ? DroolsBackend.readingsOf(build, session) : List.of();
            return new Run(fp, labelOf, decs, reads);
        } finally {
            session.dispose();
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    private static Path strictDocument(String model, List<String> rules, List<String> defaults) throws Exception {
        String ir = "{\"format\":\"uchar-ir-v1\",\"inferFromEmpty\":false,\"model\":" + model + ",\"rules\":{\"rules\":["
                + String.join(",", rules) + "],\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],"
                + "\"consistencies\":[],\"uniquenesses\":[],\"factCreations\":[],\"startPointDeterminations\":[],"
                + "\"ruleGroups\":[],\"prioritizedDefaults\":[" + String.join(",", defaults) + "]},\"diagnostics\":[]}";
        Path tmp = Files.createTempFile("same-conclusion-witness", ".json");
        tmp.toFile().deleteOnExit();
        return Files.writeString(tmp, ir);
    }

    private static String strictInstance(String label, String type, Map<String, String> numbers,
                                         List<String> present, List<String> absent, Map<String, List<String>> roles) {
        List<String> a = new ArrayList<>(), p = new ArrayList<>(), ab = new ArrayList<>(), r = new ArrayList<>();
        numbers.forEach((name, lexeme) -> a.add("\"" + name + "\":{\"number\":\"" + lexeme + "\"}"));
        for (String k : present) p.add("\"" + k + "\"");
        for (String k : absent) ab.add("\"" + k + "\"");
        roles.forEach((role, targets) -> {
            List<String> t = new ArrayList<>();
            for (String target : targets) t.add("\"" + target + "\"");
            r.add("\"" + role + "\":[" + String.join(",", t) + "]");
        });
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + String.join(",", a)
                + "},\"roles\":{" + String.join(",", r) + "},\"characteristics\":[" + String.join(",", p)
                + "],\"characteristicsAbsent\":[" + String.join(",", ab) + "]}";
    }

    private static JsonNode strictCase(String asOfDate, List<String> instances) throws Exception {
        return TmsDocuments.JSON.readTree("{\"format\":\"uchar-case-v0\",\"asOfDate\":"
                + (asOfDate == null ? "null" : "\"" + asOfDate + "\"") + ",\"input\":{\"instances\":["
                + String.join(",", instances) + "],\"parameters\":{}},\"expected\":[]}");
    }
}
