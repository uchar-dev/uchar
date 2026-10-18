package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.tms.TextOrder;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.runtime.KieSession;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocuments.binary;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistency;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.inconsistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.instance;
import static dev.uchar.regelspraak.cases.TmsDocuments.channelTest;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.reading;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.comparison;
import static dev.uchar.regelspraak.cases.TmsDocuments.verwacht;
import static dev.uchar.regelspraak.cases.TmsDocuments.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Consistencies and the §9.5 channel on the exec-model branch (IR-SPEC ch. 3.5, RUNTIME-SEMANTICS ch. 5.6). A
 * consistency is a rule that inserts the {@code Inconsistentie} of its subject logically where its assertion does not
 * hold, so a violation found against a transient value goes when that value changes. The first document has the
 * consistency read an attribute a chain of two rules fills: "Het tussengewicht van een Vondst moet berekend worden als
 * zijn gewicht plus 1", "Het eindgewicht van een Vondst moet berekend worden als zijn tussengewicht plus 1", and "het
 * eindgewicht van een Vondst moet groter zijn dan 10". V1 (gewicht 12): eindgewicht 14, no violation. V2 (gewicht 5):
 * 7, violated. V3 (no gewicht): 2, violated (a leeg operand of plus counts as 0). Before the chain fills, the
 * consistency reads the eindgewicht's input standing, leeg, and an ordering with a leeg operand is violated; that
 * violation must be withdrawn. Both orders of the two rules in the document, and a seeded sample of orders of the
 * generated rules, at least one of which must fire the consistency on V1.
 *
 * <p>The second is the worked example of RUNTIME-SEMANTICS ch. 5.6: "controle saldo" asserts that the saldo of a
 * Rekeninghouder is at least 0, and "Een Dossier is onder verscherpt toezicht indien regelversie controle saldo
 * inconsistent is". Houders h1 (saldo -5) and h2 (saldo 10), dossiers d1 and d2: one error fact, on h1, and both
 * dossiers carry the characteristic; with h1 at saldo 5, no error fact and neither dossier. The document, and every
 * order of the four generated rules. Then the leeg tables of RUNTIME-SEMANTICS ch. 1.2 in an assertion: an equality,
 * an inequality and an ordering of two numbers, and an equality and an inequality of two texts, each over a leeg and a
 * filled operand in every combination, against the outcome the tables give. Last, what the branch refuses: a cycle
 * through the channel, a read of "is gevuurd", and a channel test that names no producer the branch carries.
 *
 * <p><b>Red before the lowering</b> (measured 2026-10-08 on the branch's third step): the rows of the chain and of
 * the leeg tables were refused on the consistency, "tms-transpiler SKIP: consistentie: controle eindgewicht — een
 * Consistency: de truth-maintenance-tak draagt deze vorm nog niet"; the rows of the worked example on the reading rule,
 * "tms-transpiler SKIP: regel: toezicht — een Regelversie in de voorwaarde ('regelversie controle saldo is
 * inconsistent'): buiten de proef van de truth-maintenance-tak"; the cycle row and the refusal row got that sentence
 * instead of their own, "but was: &lt;tms-transpiler SKIP: regel: verdenking — een Regelversie in de voorwaarde
 * ('regelversie controle verdenking is inconsistent'): buiten de proef van de truth-maintenance-tak&gt;". The row of
 * the chain on the exec-model branch was green from the start.
 *
 * <p><b>Red by mutation</b> (measured the same day, not committed). (1) With the error fact inserted as a stated fact
 * instead of a logical one, the violations found against the transient eindgewicht stayed, and the sample failed on
 * its first order, "foutfeiten in volgorde [stand Vondst.eindgewicht: uit de invoer, stand Vondst.eindgewicht: uit een
 * afleiding, stand Vondst.tussengewicht: uit de invoer, stand Vondst.tussengewicht: uit een afleiding, controle
 * eindgewicht, eindgewicht van een vondst, tussengewicht van een vondst] ==&gt; expected: &lt;[controle eindgewicht @
 * V2, controle eindgewicht @ V3]&gt; but was: &lt;[controle eindgewicht @ V1, controle eindgewicht @ V1, controle
 * eindgewicht @ V2, controle eindgewicht @ V2, controle eindgewicht @ V2, controle eindgewicht @ V3, controle
 * eindgewicht @ V3, controle eindgewicht @ V3]&gt;"; the document orders stayed green under it, since the replay
 * fires the consistency on V1 in neither, which is why the sample row is here. (2) With the channel edges left out of
 * the cycle check, the cycle row failed on "Expected …CaseRunner.Excluded to be thrown, but nothing was thrown", and a
 * replay of one Vondst ended on "case: geen fixpunt (kringverwijzing?)".
 *
 * <p><b>Both branches of the analysis</b> (card drl-teksttak-op-truth-maintenance, step 3b): every row runs on the
 * exec-model branch and on {@code DRL_TEXT}, its rendering as DRL text, and the orders of the generated rules are tried
 * on both: on {@code DRL_TEXT} the rules of the plan rendered as text in each order ({@link TextOrder}), since the
 * order of the rules in the text is the order in which Drools loads them. <b>Red</b> on {@code 0458b2d0}, before the
 * step: both rows of the worked example failed on {@code DRL_TMS} with "drl-tms: de DRL-weergave draagt Kanaalstand
 * nog niet", the row of the document through the replay and the row of the orders where it builds the branch. The rows
 * of the chain, of the leeg tables and of the refusals held there already: a consistency the rendering carried since
 * step 2a, and the refusals come from the analysis, which the two branches share.
 */
class TmsConsistencyWitnessTest {

    private static final String INCONSISTENCY = FactModelGenerator.PAKKET + ".Inconsistentie";

    /** The branches of the analysis: the executable model and its rendering as DRL text. */
    private static final List<DroolsBackend.Branch> BRANCHES = List.of(DroolsBackend.Branch.EXEC_MODEL,
            DroolsBackend.Branch.DRL_TEXT);

    // ---------------- a consistency over a chain ----------------

    private static final List<String> CHAIN = List.of(
            computingRule("tussengewicht van een vondst", "BEREKENING", "Vondst", "tussengewicht",
                    binary("plus", reading("gewicht", "Vondst"), number("1")), "null"),
            computingRule("eindgewicht van een vondst", "BEREKENING", "Vondst", "eindgewicht",
                    binary("plus", reading("tussengewicht", "Vondst"), number("1")), "null"));

    private static final String CHECK = consistency("controle eindgewicht", "Vondst",
            comparison("greater", reading("eindgewicht", "Vondst"), number("10")));

    private static final String CHAIN_MODEL = TmsDocuments.model(List.of(TmsDocuments.objecttype("Vondst",
            List.of(attribuut("gewicht", WHOLE), attribuut("tussengewicht", WHOLE), attribuut("eindgewicht", WHOLE)),
            List.of())), List.of());

    @Test void a_consistency_over_a_chain_in_every_order_of_the_rules_in_the_document() throws Exception {
        int orders = 0;
        for (List<String> order : TmsDocuments.permutations(CHAIN)) {
            Path ir = TmsDocuments.document(CHAIN_MODEL, order, List.of(CHECK));
            List<String> names = names(order);
            for (DroolsBackend.Branch branch : BRANCHES) {
                assertNull(CaseRunner.playCase(ir, chainWithoutViolation(), branch), () -> branch + ": V1 in volgorde " + names);
                assertNull(CaseRunner.playCase(ir, chainWithViolation(), branch),
                        () -> branch + ": V2 en V3 in volgorde " + names);
                orders++;
            }
        }
        assertEquals(4, orders);
    }

    /** Two rules, the consistency, and the standings of the tussengewicht and the eindgewicht (two rules each); on
     *  each branch the same sample of orders. */
    @Test void sample_of_orders_of_the_generated_rules() throws Exception {
        Path ir = TmsDocuments.document(CHAIN_MODEL, CHAIN, List.of(CHECK));
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        List<Rule> gegenereerd = b.execModelTranspiler().model().getRules();
        assertEquals(7, gegenereerd.size(), () -> "gegenereerde regels: " + gegenereerd.stream().map(Rule::getName).toList());
        int intermediateState = 0;
        for (List<Rule> order : TmsDocuments.sample(gegenereerd, 20261008L, 100))
            if (fireChain(b, TmsDocuments.kieBase(b, order), order.stream().map(Rule::getName).toList()))
                intermediateState++;
        assertTrue(intermediateState > 0, "geen volgorde vuurde de consistentie op V1");
        DroolsBackend.Build t = DroolsBackend.build(ir, DroolsBackend.Branch.DRL_TEXT);
        List<String> rules = TextOrder.rules(t);
        assertEquals(gegenereerd.stream().map(Rule::getName).toList(), rules, "de regels van de tekst");
        int text = 0;
        for (List<String> order : TmsDocuments.sample(rules, 20261008L, 100))
            if (fireChain(t, TextOrder.kieBase(t, order), order)) text++;
        assertTrue(text > 0, "geen volgorde van de tekst vuurde de consistentie op V1");
    }

    /** Fires V1, V2 and V3 through {@code kieBase}, the rules of {@code b} in the order {@code namen}, checks the error
     *  facts at the fixpoint and the beans after the write-back, and says whether the consistency fired on V1, which
     *  holds no violation at the end. */
    private static boolean fireChain(DroolsBackend.Build b, KieBase kieBase, List<String> names) throws Exception {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        KieSession sessie = kieBase.newKieSession();
        try {
            Object v1 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("gewicht", "12"), List.of());
            Object v2 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("gewicht", "5"), List.of());
            Object v3 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of(), List.of());
            boolean[] intermediateState = {false};
            sessie.addEventListener(new DefaultAgendaEventListener() {
                @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                    if (e.getMatch().getRule().getName().equals("controle eindgewicht")
                            && e.getMatch().getObjects().stream().anyMatch(o -> o == v1))
                        intermediateState[0] = true;
                }
            });
            for (Object v : List.of(v1, v2, v3)) sessie.insert(v);
            assertNull(DroolsBackend.vuur(b, sessie, 100_000).geenFixpunt(),
                    () -> b.branch() + ": geen fixpunt in volgorde " + names);
            String where = " op " + b.branch() + " in volgorde " + names;
            assertEquals(List.of("controle eindgewicht @ V2", "controle eindgewicht @ V3"),
                    errorFacts(sessie, Map.of(v1, "V1", v2, "V2", v3, "V3")), "foutfeiten" + where);
            DroolsBackend.zetAfgeleideWaarden(b, sessie);
            endsOn(v1, "eindgewicht", "14", "V1" + where);
            endsOn(v2, "eindgewicht", "7", "V2" + where);
            endsOn(v3, "eindgewicht", "2", "V3" + where);
            return intermediateState[0];
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    private static JsonNode chainWithoutViolation() throws Exception {
        return TmsDocuments.caseDocument(List.of(instance("V1", "Vondst", Map.of("gewicht", "12"), List.of(), Map.of())),
                List.of(waarde("V1", "eindgewicht", "14"), consistent()));
    }

    private static JsonNode chainWithViolation() throws Exception {
        return TmsDocuments.caseDocument(
                List.of(instance("V2", "Vondst", Map.of("gewicht", "5"), List.of(), Map.of()),
                        instance("V3", "Vondst", Map.of(), List.of(), Map.of())),
                List.of(waarde("V2", "eindgewicht", "7"), waarde("V3", "eindgewicht", "2"),
                        inconsistent("controle eindgewicht", "V2"), inconsistent("controle eindgewicht", "V3")));
    }

    // ---------------- the worked example of RUNTIME-SEMANTICS ch. 5.6 ----------------

    private static final String BALANCE_MODEL = TmsDocuments.model(List.of(
            TmsDocuments.objecttype("Rekeninghouder", List.of(attribuut("saldo", WHOLE)), List.of()),
            TmsDocuments.objecttype("Dossier", List.of(), List.of("onder verscherpt toezicht"))), List.of());

    private static final String CHECK_BALANCE = consistency("controle saldo", "Rekeninghouder",
            comparison("greaterOrEqual", reading("saldo", "Rekeninghouder"), number("0")));

    private static final String TOEZICHT = characteristicRule("toezicht", "Dossier", "onder verscherpt toezicht",
            channelTest("controle saldo"));

    @Test void the_worked_example_of_the_channel() throws Exception {
        Path ir = TmsDocuments.document(BALANCE_MODEL, List.of(TOEZICHT), List.of(CHECK_BALANCE));
        for (DroolsBackend.Branch branch : BRANCHES) {
            assertNull(CaseRunner.playCase(ir, balanceCase("-5"), branch), branch + ": h1 met saldo -5");
            assertNull(CaseRunner.playCase(ir, balanceCase("5"), branch), branch + ": h1 met saldo 5");
        }
    }

    /** The reading rule, the consistency and the two rules of the channel's standing; the saldo is input and nothing
     *  reads the characteristic. Four rules have 24 orders, so every one is tried, on each branch. */
    @Test void every_order_of_the_generated_rules_of_the_worked_example() throws Exception {
        Path ir = TmsDocuments.document(BALANCE_MODEL, List.of(TOEZICHT), List.of(CHECK_BALANCE));
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        List<Rule> gegenereerd = b.execModelTranspiler().model().getRules();
        assertEquals(4, gegenereerd.size(), () -> "gegenereerde regels: " + gegenereerd.stream().map(Rule::getName).toList());
        int orders = 0;
        for (List<Rule> order : TmsDocuments.permutations(gegenereerd)) {
            KieBase kieBase = TmsDocuments.kieBase(b, order);
            List<String> names = order.stream().map(Rule::getName).toList();
            fireBalance(b, kieBase, names, "-5");
            fireBalance(b, kieBase, names, "5");
            orders++;
        }
        DroolsBackend.Build t = DroolsBackend.build(ir, DroolsBackend.Branch.DRL_TEXT);
        List<String> rules = TextOrder.rules(t);
        assertEquals(gegenereerd.stream().map(Rule::getName).toList(), rules, "de regels van de tekst");
        for (List<String> order : TmsDocuments.permutations(rules)) {
            KieBase kieBase = TextOrder.kieBase(t, order);
            fireBalance(t, kieBase, order, "-5");
            fireBalance(t, kieBase, order, "5");
            orders++;
        }
        assertEquals(48, orders);
    }

    /** Fires h1 (with {@code saldo}), h2 (saldo 10), d1 and d2 through {@code kieBase}, the rules of {@code b} in the
     *  order {@code namen}, and checks the error facts and both dossiers. */
    private static void fireBalance(DroolsBackend.Build b, KieBase kieBase, List<String> names, String balance)
            throws Exception {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        KieSession sessie = kieBase.newKieSession();
        try {
            Object h1 = TmsDocuments.beanObject(b.beanLoader(), "Rekeninghouder", Map.of("saldo", balance), List.of());
            Object h2 = TmsDocuments.beanObject(b.beanLoader(), "Rekeninghouder", Map.of("saldo", "10"), List.of());
            Object d1 = TmsDocuments.beanObject(b.beanLoader(), "Dossier", Map.of(), List.of());
            Object d2 = TmsDocuments.beanObject(b.beanLoader(), "Dossier", Map.of(), List.of());
            for (Object o : List.of(h1, h2, d1, d2)) sessie.insert(o);
            assertNull(DroolsBackend.vuur(b, sessie, 100_000).geenFixpunt(),
                    () -> b.branch() + ": geen fixpunt in volgorde " + names);
            String where = " bij saldo " + balance + " op " + b.branch() + " in volgorde " + names;
            boolean geschonden = balance.startsWith("-");
            assertEquals(geschonden ? List.of("controle saldo @ h1") : List.of(),
                    errorFacts(sessie, Map.of(h1, "h1", h2, "h2", d1, "d1", d2, "d2")), "foutfeiten" + where);
            DroolsBackend.zetAfgeleideWaarden(b, sessie);
            assertEquals(List.of(geschonden, geschonden), List.of(TmsDocuments.has(d1, "onder verscherpt toezicht"),
                    TmsDocuments.has(d2, "onder verscherpt toezicht")), "d1 en d2" + where);
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    private static JsonNode balanceCase(String balance) throws Exception {
        boolean geschonden = balance.startsWith("-");
        List<String> verwachtingen = new ArrayList<>(List.of(verwacht("d1", "onder verscherpt toezicht", geschonden),
                verwacht("d2", "onder verscherpt toezicht", geschonden)));
        verwachtingen.add(geschonden ? inconsistent("controle saldo", "h1") : consistent());
        return TmsDocuments.caseDocument(
                List.of(instance("h1", "Rekeninghouder", Map.of("saldo", balance), List.of(), Map.of()),
                        instance("h2", "Rekeninghouder", Map.of("saldo", "10"), List.of(), Map.of()),
                        instance("d1", "Dossier", Map.of(), List.of(), Map.of()),
                        instance("d2", "Dossier", Map.of(), List.of(), Map.of())),
                verwachtingen);
    }

    // ---------------- leeg in an assertion ----------------

    /** A row of the leeg tables: the two operands (null for leeg) and whether the assertion is violated. */
    private record Row(String left, String right, boolean geschonden) {}

    /** Every assertion in a document of its own, so that a case of one Vondst pins its whole channel: violated or
     *  consistent. Numbers: two leeg are equal, exactly one leeg is unequal, an ordering with a leeg operand does not
     *  hold. Texts: an equality with a leeg operand does not hold, an inequality holds where exactly one is leeg. */
    @Test void empty_under_equality_and_ordering_per_the_tables() throws Exception {
        String numbers = TmsDocuments.model(List.of(TmsDocuments.objecttype("Vondst",
                List.of(attribuut("a", WHOLE), attribuut("b", WHOLE)), List.of())), List.of());
        String texts = TmsDocuments.model(List.of(TmsDocuments.objecttype("Vondst",
                List.of(attribuut("s", TEKST), attribuut("t", TEKST)), List.of())), List.of());
        int rows = 0;
        rows += rows(numbers, "equal", "a", "b", false, List.of(new Row(null, null, false), new Row(null, "5", true),
                new Row("5", "5", false), new Row("5", "3", true)));
        rows += rows(numbers, "notEqual", "a", "b", false, List.of(new Row(null, null, true), new Row(null, "5", false),
                new Row("5", "5", true), new Row("5", "3", false)));
        rows += rows(numbers, "greater", "a", "b", false, List.of(new Row(null, null, true), new Row(null, "5", true),
                new Row("5", "5", true), new Row("5", "3", false)));
        rows += rows(texts, "equal", "s", "t", true, List.of(new Row(null, null, true), new Row(null, "x", true),
                new Row("x", "x", false), new Row("x", "y", true)));
        rows += rows(texts, "notEqual", "s", "t", true, List.of(new Row(null, null, true), new Row(null, "x", false),
                new Row("x", "x", true), new Row("x", "y", false)));
        assertEquals(20, rows);
    }

    private static int rows(String model, String op, String left, String right, boolean text, List<Row> rows)
            throws Exception {
        String naam = "controle " + op;
        Path ir = TmsDocuments.document(model, List.of(), List.of(consistency(naam, "Vondst",
                comparison(op, reading(left, "Vondst"), reading(right, "Vondst")))));
        for (Row queue : rows) {
            List<String> attributes = new ArrayList<>();
            if (queue.left() != null) attributes.add(invoer(left, queue.left(), text));
            if (queue.right() != null) attributes.add(invoer(right, queue.right(), text));
            JsonNode caseDocument = TmsDocuments.caseDocument(List.of("{\"label\":\"V1\",\"objectType\":\"Vondst\",\"attributes\":{"
                    + String.join(",", attributes) + "},\"roles\":{},\"characteristics\":[]}"),
                    List.of(queue.geschonden() ? inconsistent(naam, "V1") : consistent()));
            for (DroolsBackend.Branch branch : BRANCHES)
                assertNull(CaseRunner.playCase(ir, caseDocument, branch), () -> branch + ": " + naam + " over " + queue);
        }
        return rows.size();
    }

    private static final String TEKST = "{\"node\":\"Text\"}";

    private static String invoer(String attribuut, String waarde, boolean text) {
        return "\"" + attribuut + "\":{\"" + (text ? "text" : "number") + "\":\"" + waarde + "\"}";
    }

    // ---------------- what the branch refuses ----------------

    /** "Een Vondst is verdacht indien regelversie controle verdenking inconsistent is" beside a consistency that
     *  asserts "hij is verdacht": a cycle of odd length through the channel, which RUNTIME-SEMANTICS ch. 5.6 puts in the
     *  dependency graph. Under truth maintenance it alternates until the fire cap. */
    @Test void a_cycle_over_the_channel_is_refused_before_building() throws Exception {
        String model = TmsDocuments.model(List.of(TmsDocuments.objecttype("Vondst", List.of(), List.of("verdacht"))),
                List.of());
        Path ir = TmsDocuments.document(model,
                List.of(characteristicRule("verdenking", "Vondst", "verdacht", channelTest("controle verdenking"))),
                List.of(consistency("controle verdenking", "Vondst", check("verdacht", false))));
        for (DroolsBackend.Branch branch : BRANCHES) {
            CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> DroolsBackend.build(ir, branch),
                    branch.toString());
            assertEquals(CYCLE, e.getMessage(), branch.toString());
        }
    }

    /** The refusal of the cycle, which {@code DRL_TEXT} takes from the analysis with its label. */
    private static final String CYCLE = "exec-model: kring in de afhankelijkheden over de knopen Vondst.verdacht en het"
            + " kanaal van \"controle verdenking\" via de regel \"verdenking\" en de consistentieregel \"controle"
            + " verdenking\" — een kring die niet door een creatielid van een recursieve regelgroep loopt, is onwelgevormd"
            + " (WF-045), en truth maintenance geeft er geen eenduidig fixpunt voor: een positieve kring houdt zichzelf in"
            + " stand nadat haar ingang vervalt, een even ontkenningskring beslist op de volgorde van de regels in het"
            + " model, en een oneven ontkenningskring wisselt tot het vuurplafond";

    @Test void is_fired_and_a_channel_without_carried_producer_are_refused() throws Exception {
        String model = TmsDocuments.model(List.of(TmsDocuments.objecttype("Vondst", List.of(),
                List.of("gemeld", "geregistreerd", "nagekeken"))), List.of());
        String registratie = characteristicRule("registratie", "Vondst", "geregistreerd", check("gemeld", false));
        String fired = "{\"node\":\"Regelversie\",\"regel\":\"registratie\",\"inconsistent\":false,\"source\":"
                + "{\"snippet\":\"regelversie registratie is gevuurd\"}}";
        Path withFired = TmsDocuments.document(model,
                List.of(registratie, characteristicRule("nakijken", "Vondst", "nagekeken", fired)));
        for (DroolsBackend.Branch branch : BRANCHES) {
            CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> DroolsBackend.build(withFired, branch),
                    branch.toString());
            assertEquals("transpiler SKIP: regel: nakijken — een Regelversie 'is gevuurd' in de voorwaarde ('regelversie"
                + " registratie is gevuurd'): geen vorm van de kern maar een predicaat van het"
                + " RegelSpraak-dialect over het fixpunt (RUNTIME-SEMANTICS ch. 5.6, CONSTRUCTS rij 30), dat de"
                + " referentie-interpreter en de ASP-backend niet evalueren", e.getMessage(), branch.toString());
        }
        Path withoutProducer = TmsDocuments.document(model,
                List.of(registratie, characteristicRule("nakijken", "Vondst", "nagekeken", channelTest("registratie"))));
        for (DroolsBackend.Branch branch : BRANCHES) {
            CaseRunner.Excluded f = assertThrows(CaseRunner.Excluded.class,
                    () -> DroolsBackend.build(withoutProducer, branch), branch.toString());
            assertEquals("transpiler SKIP: regel: nakijken — een Regelversie 'is inconsistent' over \"registratie\" in de"
                + " voorwaarde ('regelversie registratie is inconsistent'): deze tak draagt als producent"
                + " van het §9.5-kanaal alleen een Consistency, een Uniqueness en een regel met een deling of een wortel"
                + " in haar voorwaarde of haar waarde (§6.5, §6.6)", f.getMessage(), branch.toString());
        }
    }

    // ---------------- helpers ----------------

    /** The §9.5 error facts of the session as "regel @ label", sorted, the subject named through {@code labels}. */
    private static List<String> errorFacts(KieSession sessie, Map<Object, String> labels) throws Exception {
        Map<Object, String> perIdentity = new IdentityHashMap<>(labels);
        List<String> out = new ArrayList<>();
        for (Object f : sessie.getObjects(o -> o.getClass().getName().equals(INCONSISTENCY)))
            out.add(f.getClass().getMethod("getRegel").invoke(f) + " @ "
                    + perIdentity.get(f.getClass().getMethod("getSubject").invoke(f)));
        out.sort(null);
        return out;
    }

    /** Attribute {@code attribuut} of the bean ends at the number {@code lexeme}. */
    private static void endsOn(Object beanObject, String attribuut, String lexeme, String where) throws Exception {
        Object v = TmsDocuments.lees(beanObject, attribuut);
        assertTrue(v instanceof BigDecimal d && new BigDecimal(lexeme).compareTo(d) == 0,
                () -> attribuut + " van " + where + ": verwacht " + lexeme + ", werkelijk " + v);
    }

    private static List<String> names(List<String> rules) throws Exception {
        List<String> out = new ArrayList<>();
        for (String r : rules) out.add(TmsDocuments.JSON.readTree(r).get("name").asText());
        return out;
    }
}
