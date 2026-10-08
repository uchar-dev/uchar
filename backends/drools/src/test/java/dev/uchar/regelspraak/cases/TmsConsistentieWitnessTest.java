package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.schema.FactModelGenerator;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.runtime.KieSession;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocumenten.GEHEEL;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocumenten.binair;
import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.consistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.consistentie;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.inconsistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.instantie;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kanaaltoets;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.vergelijking;
import static dev.uchar.regelspraak.cases.TmsDocumenten.verwacht;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Consistencies and the §9.5 channel on the truth-maintenance branch (IR-SPEC ch. 3.5, RUNTIME-SEMANTICS ch. 5.6). A
 * consistency is a rule that inserts the {@code Inconsistentie} of its subject logically where its assertion does not
 * hold, so a violation found against a transient value goes when that value changes. The first document has the
 * consistency read an attribute a chain of two rules fills: "Het tussengewicht van een Vondst moet berekend worden als
 * zijn gewicht plus 1", "Het eindgewicht van een Vondst moet berekend worden als zijn tussengewicht plus 1", and "het
 * eindgewicht van een Vondst moet groter zijn dan 10". V1 (gewicht 12): eindgewicht 14, no violation. V2 (gewicht 5):
 * 7, violated. V3 (no gewicht): 2, violated (a leeg operand of plus counts as 0). Before the chain fills, the
 * consistency reads the eindgewicht's input standing, leeg, and an ordering with a leeg operand is violated; that
 * violation must be withdrawn. Both orders of the two rules in the document, the exec-model branch on the document, and
 * a seeded sample of orders of the generated rules, at least one of which must fire the consistency on V1.
 *
 * <p>The second is the worked example of RUNTIME-SEMANTICS ch. 5.6: "controle saldo" asserts that the saldo of a
 * Rekeninghouder is at least 0, and "Een Dossier is onder verscherpt toezicht indien regelversie controle saldo
 * inconsistent is". Houders h1 (saldo -5) and h2 (saldo 10), dossiers d1 and d2: one error fact, on h1, and both
 * dossiers carry the characteristic; with h1 at saldo 5, no error fact and neither dossier. Both branches, and every
 * order of the four generated rules. Then the leeg tables of RUNTIME-SEMANTICS ch. 1.2 in an assertion: an equality,
 * an inequality and an ordering of two numbers, and an equality and an inequality of two texts, each over a leeg and a
 * filled operand in every combination, against the outcome the tables give and against the exec-model branch on the
 * same document. Last, what the branch refuses: a cycle through the channel, a read of "is gevuurd", and a channel test
 * that names no producer the branch carries.
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
 */
class TmsConsistentieWitnessTest {

    private static final String INCONSISTENTIE = FactModelGenerator.PAKKET + ".Inconsistentie";

    // ---------------- a consistency over a chain ----------------

    private static final List<String> KETEN = List.of(
            rekenregel("tussengewicht van een vondst", "BEREKENING", "Vondst", "tussengewicht",
                    binair("plus", lezing("gewicht", "Vondst"), getal("1")), "null"),
            rekenregel("eindgewicht van een vondst", "BEREKENING", "Vondst", "eindgewicht",
                    binair("plus", lezing("tussengewicht", "Vondst"), getal("1")), "null"));

    private static final String CONTROLE = consistentie("controle eindgewicht", "Vondst",
            vergelijking("greater", lezing("eindgewicht", "Vondst"), getal("10")));

    private static final String KETEN_MODEL = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst",
            List.of(attribuut("gewicht", GEHEEL), attribuut("tussengewicht", GEHEEL), attribuut("eindgewicht", GEHEEL)),
            List.of())), List.of());

    @Test void een_consistentie_over_een_keten_in_elke_volgorde_van_de_regels_in_het_document() throws Exception {
        int volgordes = 0;
        for (List<String> volgorde : TmsDocumenten.permutaties(KETEN)) {
            Path ir = TmsDocumenten.document(KETEN_MODEL, volgorde, List.of(CONTROLE));
            List<String> namen = namen(volgorde);
            assertNull(CaseRunner.speelCase(ir, ketenZonderSchending(), DroolsBackend.Branch.TRUTH_MAINTENANCE),
                    () -> "V1 in volgorde " + namen);
            assertNull(CaseRunner.speelCase(ir, ketenMetSchending(), DroolsBackend.Branch.TRUTH_MAINTENANCE),
                    () -> "V2 en V3 in volgorde " + namen);
            volgordes++;
        }
        assertEquals(2, volgordes);
    }

    @Test void de_exec_modeltak_geeft_dezelfde_schendingen() throws Exception {
        Path ir = TmsDocumenten.document(KETEN_MODEL, KETEN, List.of(CONTROLE));
        assertNull(CaseRunner.speelCase(ir, ketenZonderSchending(), DroolsBackend.Branch.EXEC_MODEL), "V1");
        assertNull(CaseRunner.speelCase(ir, ketenMetSchending(), DroolsBackend.Branch.EXEC_MODEL), "V2 en V3");
    }

    /** Two rules, the consistency, and the standings of the tussengewicht and the eindgewicht (two rules each). */
    @Test void steekproef_van_volgordes_van_de_gegenereerde_regels() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsDocumenten.document(KETEN_MODEL, KETEN, List.of(CONTROLE)),
                DroolsBackend.Branch.TRUTH_MAINTENANCE);
        List<Rule> gegenereerd = b.tmsTranspiler().model().getRules();
        assertEquals(7, gegenereerd.size(), () -> "gegenereerde regels: " + gegenereerd.stream().map(Rule::getName).toList());
        int tussenstand = 0;
        for (List<Rule> volgorde : TmsDocumenten.steekproef(gegenereerd, 20261008L, 100))
            if (vuurKeten(b, volgorde)) tussenstand++;
        assertTrue(tussenstand > 0, "geen volgorde vuurde de consistentie op V1");
    }

    /** Fires V1, V2 and V3 with the rules in {@code volgorde}, checks the error facts at the fixpoint and the beans after
     *  the write-back, and says whether the consistency fired on V1, which holds no violation at the end. */
    private static boolean vuurKeten(DroolsBackend.Build b, List<Rule> volgorde) throws Exception {
        List<String> namen = volgorde.stream().map(Rule::getName).toList();
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        KieSession sessie = TmsDocumenten.kieBase(b, volgorde).newKieSession();
        try {
            Object v1 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("gewicht", "12"), List.of());
            Object v2 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("gewicht", "5"), List.of());
            Object v3 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of(), List.of());
            boolean[] tussenstand = {false};
            sessie.addEventListener(new DefaultAgendaEventListener() {
                @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                    if (e.getMatch().getRule().getName().equals("controle eindgewicht")
                            && e.getMatch().getObjects().stream().anyMatch(o -> o == v1))
                        tussenstand[0] = true;
                }
            });
            for (Object v : List.of(v1, v2, v3)) sessie.insert(v);
            assertTrue(sessie.fireAllRules(100_000) < 100_000, () -> "geen fixpunt in volgorde " + namen);
            String waar = " in volgorde " + namen;
            assertEquals(List.of("controle eindgewicht @ V2", "controle eindgewicht @ V3"),
                    foutfeiten(sessie, Map.of(v1, "V1", v2, "V2", v3, "V3")), "foutfeiten" + waar);
            DroolsBackend.zetAfgeleideWaarden(b, sessie);
            eindigtOp(v1, "eindgewicht", "14", "V1" + waar);
            eindigtOp(v2, "eindgewicht", "7", "V2" + waar);
            eindigtOp(v3, "eindgewicht", "2", "V3" + waar);
            return tussenstand[0];
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    private static JsonNode ketenZonderSchending() throws Exception {
        return TmsDocumenten.zaak(List.of(instantie("V1", "Vondst", Map.of("gewicht", "12"), List.of(), Map.of())),
                List.of(waarde("V1", "eindgewicht", "14"), consistent()));
    }

    private static JsonNode ketenMetSchending() throws Exception {
        return TmsDocumenten.zaak(
                List.of(instantie("V2", "Vondst", Map.of("gewicht", "5"), List.of(), Map.of()),
                        instantie("V3", "Vondst", Map.of(), List.of(), Map.of())),
                List.of(waarde("V2", "eindgewicht", "7"), waarde("V3", "eindgewicht", "2"),
                        inconsistent("controle eindgewicht", "V2"), inconsistent("controle eindgewicht", "V3")));
    }

    // ---------------- the worked example of RUNTIME-SEMANTICS ch. 5.6 ----------------

    private static final String SALDO_MODEL = TmsDocumenten.model(List.of(
            TmsDocumenten.objecttype("Rekeninghouder", List.of(attribuut("saldo", GEHEEL)), List.of()),
            TmsDocumenten.objecttype("Dossier", List.of(), List.of("onder verscherpt toezicht"))), List.of());

    private static final String CONTROLE_SALDO = consistentie("controle saldo", "Rekeninghouder",
            vergelijking("greaterOrEqual", lezing("saldo", "Rekeninghouder"), getal("0")));

    private static final String TOEZICHT = kenmerkregel("toezicht", "Dossier", "onder verscherpt toezicht",
            kanaaltoets("controle saldo"));

    @Test void het_uitgewerkte_voorbeeld_van_het_kanaal_in_beide_takken() throws Exception {
        Path ir = TmsDocumenten.document(SALDO_MODEL, List.of(TOEZICHT), List.of(CONTROLE_SALDO));
        for (DroolsBackend.Branch tak : List.of(DroolsBackend.Branch.TRUTH_MAINTENANCE, DroolsBackend.Branch.EXEC_MODEL)) {
            assertNull(CaseRunner.speelCase(ir, saldoZaak("-5"), tak), () -> tak + ", h1 met saldo -5");
            assertNull(CaseRunner.speelCase(ir, saldoZaak("5"), tak), () -> tak + ", h1 met saldo 5");
        }
    }

    /** The reading rule, the consistency and the two rules of the channel's standing; the saldo is input and nothing
     *  reads the characteristic. Four rules have 24 orders, so every one is tried. */
    @Test void elke_volgorde_van_de_gegenereerde_regels_van_het_uitgewerkte_voorbeeld() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsDocumenten.document(SALDO_MODEL, List.of(TOEZICHT),
                List.of(CONTROLE_SALDO)), DroolsBackend.Branch.TRUTH_MAINTENANCE);
        List<Rule> gegenereerd = b.tmsTranspiler().model().getRules();
        assertEquals(4, gegenereerd.size(), () -> "gegenereerde regels: " + gegenereerd.stream().map(Rule::getName).toList());
        int volgordes = 0;
        for (List<Rule> volgorde : TmsDocumenten.permutaties(gegenereerd)) {
            vuurSaldo(b, volgorde, "-5");
            vuurSaldo(b, volgorde, "5");
            volgordes++;
        }
        assertEquals(24, volgordes);
    }

    /** Fires h1 (with {@code saldo}), h2 (saldo 10), d1 and d2, and checks the error facts and both dossiers. */
    private static void vuurSaldo(DroolsBackend.Build b, List<Rule> volgorde, String saldo) throws Exception {
        List<String> namen = volgorde.stream().map(Rule::getName).toList();
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        KieSession sessie = TmsDocumenten.kieBase(b, volgorde).newKieSession();
        try {
            Object h1 = TmsDocumenten.boon(b.beanLoader(), "Rekeninghouder", Map.of("saldo", saldo), List.of());
            Object h2 = TmsDocumenten.boon(b.beanLoader(), "Rekeninghouder", Map.of("saldo", "10"), List.of());
            Object d1 = TmsDocumenten.boon(b.beanLoader(), "Dossier", Map.of(), List.of());
            Object d2 = TmsDocumenten.boon(b.beanLoader(), "Dossier", Map.of(), List.of());
            for (Object o : List.of(h1, h2, d1, d2)) sessie.insert(o);
            assertTrue(sessie.fireAllRules(100_000) < 100_000, () -> "geen fixpunt in volgorde " + namen);
            String waar = " bij saldo " + saldo + " in volgorde " + namen;
            boolean geschonden = saldo.startsWith("-");
            assertEquals(geschonden ? List.of("controle saldo @ h1") : List.of(),
                    foutfeiten(sessie, Map.of(h1, "h1", h2, "h2", d1, "d1", d2, "d2")), "foutfeiten" + waar);
            DroolsBackend.zetAfgeleideWaarden(b, sessie);
            assertEquals(List.of(geschonden, geschonden), List.of(TmsDocumenten.heeft(d1, "onder verscherpt toezicht"),
                    TmsDocumenten.heeft(d2, "onder verscherpt toezicht")), "d1 en d2" + waar);
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    private static JsonNode saldoZaak(String saldo) throws Exception {
        boolean geschonden = saldo.startsWith("-");
        List<String> verwachtingen = new ArrayList<>(List.of(verwacht("d1", "onder verscherpt toezicht", geschonden),
                verwacht("d2", "onder verscherpt toezicht", geschonden)));
        verwachtingen.add(geschonden ? inconsistent("controle saldo", "h1") : consistent());
        return TmsDocumenten.zaak(
                List.of(instantie("h1", "Rekeninghouder", Map.of("saldo", saldo), List.of(), Map.of()),
                        instantie("h2", "Rekeninghouder", Map.of("saldo", "10"), List.of(), Map.of()),
                        instantie("d1", "Dossier", Map.of(), List.of(), Map.of()),
                        instantie("d2", "Dossier", Map.of(), List.of(), Map.of())),
                verwachtingen);
    }

    // ---------------- leeg in an assertion ----------------

    /** A row of the leeg tables: the two operands (null for leeg) and whether the assertion is violated. */
    private record Rij(String links, String rechts, boolean geschonden) {}

    /** Every assertion in a document of its own, so that a case of one Vondst pins its whole channel: violated or
     *  consistent. Numbers: two leeg are equal, exactly one leeg is unequal, an ordering with a leeg operand does not
     *  hold. Texts: an equality with a leeg operand does not hold, an inequality holds where exactly one is leeg. */
    @Test void leeg_onder_gelijkheid_en_ordening_volgens_de_tabellen_en_als_de_exec_modeltak() throws Exception {
        String getallen = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst",
                List.of(attribuut("a", GEHEEL), attribuut("b", GEHEEL)), List.of())), List.of());
        String teksten = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst",
                List.of(attribuut("s", TEKST), attribuut("t", TEKST)), List.of())), List.of());
        int rijen = 0;
        rijen += rijen(getallen, "equal", "a", "b", false, List.of(new Rij(null, null, false), new Rij(null, "5", true),
                new Rij("5", "5", false), new Rij("5", "3", true)));
        rijen += rijen(getallen, "notEqual", "a", "b", false, List.of(new Rij(null, null, true), new Rij(null, "5", false),
                new Rij("5", "5", true), new Rij("5", "3", false)));
        rijen += rijen(getallen, "greater", "a", "b", false, List.of(new Rij(null, null, true), new Rij(null, "5", true),
                new Rij("5", "5", true), new Rij("5", "3", false)));
        rijen += rijen(teksten, "equal", "s", "t", true, List.of(new Rij(null, null, true), new Rij(null, "x", true),
                new Rij("x", "x", false), new Rij("x", "y", true)));
        rijen += rijen(teksten, "notEqual", "s", "t", true, List.of(new Rij(null, null, true), new Rij(null, "x", false),
                new Rij("x", "x", true), new Rij("x", "y", false)));
        assertEquals(20, rijen);
    }

    private static int rijen(String model, String op, String links, String rechts, boolean tekst, List<Rij> rijen)
            throws Exception {
        String naam = "controle " + op;
        Path ir = TmsDocumenten.document(model, List.of(), List.of(consistentie(naam, "Vondst",
                vergelijking(op, lezing(links, "Vondst"), lezing(rechts, "Vondst")))));
        for (Rij rij : rijen) {
            List<String> attributen = new ArrayList<>();
            if (rij.links() != null) attributen.add(invoer(links, rij.links(), tekst));
            if (rij.rechts() != null) attributen.add(invoer(rechts, rij.rechts(), tekst));
            JsonNode zaak = TmsDocumenten.zaak(List.of("{\"label\":\"V1\",\"objectType\":\"Vondst\",\"attributes\":{"
                    + String.join(",", attributen) + "},\"roles\":{},\"characteristics\":[]}"),
                    List.of(rij.geschonden() ? inconsistent(naam, "V1") : consistent()));
            for (DroolsBackend.Branch tak : List.of(DroolsBackend.Branch.TRUTH_MAINTENANCE, DroolsBackend.Branch.EXEC_MODEL))
                assertNull(CaseRunner.speelCase(ir, zaak, tak), () -> tak + ", " + naam + " over " + rij);
        }
        return rijen.size();
    }

    private static final String TEKST = "{\"node\":\"Text\"}";

    private static String invoer(String attribuut, String waarde, boolean tekst) {
        return "\"" + attribuut + "\":{\"" + (tekst ? "text" : "number") + "\":\"" + waarde + "\"}";
    }

    // ---------------- what the branch refuses ----------------

    /** "Een Vondst is verdacht indien regelversie controle verdenking inconsistent is" beside a consistency that
     *  asserts "hij is verdacht": a cycle of odd length through the channel, which RUNTIME-SEMANTICS ch. 5.6 puts in the
     *  dependency graph. Under truth maintenance it alternates until the fire cap. */
    @Test void een_kring_over_het_kanaal_wordt_voor_het_bouwen_geweigerd() throws Exception {
        String model = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst", List.of(), List.of("verdacht"))),
                List.of());
        Path ir = TmsDocumenten.document(model,
                List.of(kenmerkregel("verdenking", "Vondst", "verdacht", kanaaltoets("controle verdenking"))),
                List.of(consistentie("controle verdenking", "Vondst", check("verdacht", false))));
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> DroolsBackend.build(ir, DroolsBackend.Branch.TRUTH_MAINTENANCE));
        assertEquals("tms: kring in de afhankelijkheden over de knopen Vondst.verdacht en het kanaal van \"controle"
                + " verdenking\" via de regel \"verdenking\" en de consistentieregel \"controle verdenking\" — elke kring"
                + " is onwelgevormd (WF-045; met een ontkenning erop ook WF-046), en truth maintenance geeft er geen"
                + " eenduidig fixpunt voor: een positieve kring houdt zichzelf in stand nadat haar ingang vervalt, een even"
                + " ontkenningskring beslist op de volgorde van de regels in het model, en een oneven ontkenningskring"
                + " wisselt tot het vuurplafond", e.getMessage());
    }

    @Test void is_gevuurd_en_een_kanaal_zonder_gedragen_producent_worden_geweigerd() throws Exception {
        String model = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst", List.of(),
                List.of("gemeld", "geregistreerd", "nagekeken"))), List.of());
        String registratie = kenmerkregel("registratie", "Vondst", "geregistreerd", check("gemeld", false));
        String gevuurd = "{\"node\":\"Regelversie\",\"regel\":\"registratie\",\"inconsistent\":false,\"source\":"
                + "{\"snippet\":\"regelversie registratie is gevuurd\"}}";
        Path metGevuurd = TmsDocumenten.document(model,
                List.of(registratie, kenmerkregel("nakijken", "Vondst", "nagekeken", gevuurd)));
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> DroolsBackend.build(metGevuurd, DroolsBackend.Branch.TRUTH_MAINTENANCE));
        assertEquals("tms-transpiler SKIP: regel: nakijken — een Regelversie 'is gevuurd' in de voorwaarde ('regelversie"
                + " registratie is gevuurd'): buiten de proef van de truth-maintenance-tak", e.getMessage());
        Path zonderProducent = TmsDocumenten.document(model,
                List.of(registratie, kenmerkregel("nakijken", "Vondst", "nagekeken", kanaaltoets("registratie"))));
        CaseRunner.Excluded f = assertThrows(CaseRunner.Excluded.class,
                () -> DroolsBackend.build(zonderProducent, DroolsBackend.Branch.TRUTH_MAINTENANCE));
        assertEquals("tms-transpiler SKIP: regel: nakijken — een Regelversie 'is inconsistent' over \"registratie\" in de"
                + " voorwaarde ('regelversie registratie is inconsistent'): de truth-maintenance-tak draagt als producent"
                + " van het §9.5-kanaal alleen een Consistency en een rekenende regel of initialisatie met een deling"
                + " tussen twee gelezen attributen (§6.5)", f.getMessage());
    }

    // ---------------- helpers ----------------

    /** The §9.5 error facts of the session as "regel @ label", sorted, the subject named through {@code labels}. */
    private static List<String> foutfeiten(KieSession sessie, Map<Object, String> labels) throws Exception {
        Map<Object, String> perIdentiteit = new IdentityHashMap<>(labels);
        List<String> uit = new ArrayList<>();
        for (Object f : sessie.getObjects(o -> o.getClass().getName().equals(INCONSISTENTIE)))
            uit.add(f.getClass().getMethod("getRegel").invoke(f) + " @ "
                    + perIdentiteit.get(f.getClass().getMethod("getSubject").invoke(f)));
        uit.sort(null);
        return uit;
    }

    /** Attribute {@code attribuut} of the bean ends at the number {@code lexeem}. */
    private static void eindigtOp(Object boon, String attribuut, String lexeem, String waar) throws Exception {
        Object v = TmsDocumenten.lees(boon, attribuut);
        assertTrue(v instanceof BigDecimal d && new BigDecimal(lexeem).compareTo(d) == 0,
                () -> attribuut + " van " + waar + ": verwacht " + lexeem + ", werkelijk " + v);
    }

    private static List<String> namen(List<String> regels) throws Exception {
        List<String> uit = new ArrayList<>();
        for (String r : regels) uit.add(TmsDocumenten.JSON.readTree(r).get("name").asText());
        return uit;
    }
}
