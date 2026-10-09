package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import dev.uchar.regelspraak.transpiler.tms.Uitkomst;
import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieSession;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static dev.uchar.regelspraak.cases.TmsDocumenten.DATUM;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuutgroep;
import static dev.uchar.regelspraak.cases.TmsDocumenten.binair;
import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.consistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.contextwaarde;
import static dev.uchar.regelspraak.cases.TmsDocumenten.datumwaarde;
import static dev.uchar.regelspraak.cases.TmsDocumenten.eenheid;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getaltype;
import static dev.uchar.regelspraak.cases.TmsDocumenten.groepsdefault;
import static dev.uchar.regelspraak.cases.TmsDocumenten.instantieMet;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.literaal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.standaard;
import static dev.uchar.regelspraak.cases.TmsDocumenten.vergelijking;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * An exception group below a displaced default evaluates nothing on the truth-maintenance branch: no value, no fault.
 * The reference interpreter reaches the exception groups of a default only where no higher default in its chain decides
 * ({@code decide} in {@code ref/lib/eval.ml}), at every depth (RUNTIME-SEMANTICS ch. 7.1, 7.9), and a form that does
 * not decide is not evaluated (PLAN §9, 2026-10-09). The rules of a member chain therefore wait on the positions above
 * every default whose exception group the chain is, not only on the positions above them in their own chain.
 *
 * <p>The documents: a Vondst, "Een Vondst is gestrand indien hij aangespoeld is", "Een Vondst is wrak indien hij
 * gestrand is", and a defaults group on the heffing whose default "10 indien hij wrak is" displaces a default carrying
 * a default that faults where it is evaluated, in five places: an exception group of the lower default; an exception
 * group of an exception group of it; an exception group of a default that a member chain displaces; and the first and
 * the second of two exception groups, the other one "30 indien hij geregistreerd is". The faulting default, in five
 * forms: "20 indien het Rekenjaar groter is dan 2025" and "het Rekenjaar indien hij geregistreerd is" in a session
 * without an evaluation date, an {@code Unresolved} value and an {@code Unresolved} justification, and "20 indien zijn
 * vinddatum plus 18446744073709551617 dagen later is dan 2026-01-01". V1 (aangespoeld, geregistreerd): heffing 10,
 * consistent; the displacing default reads wrak, which the two rules derive later. V2 (geregistreerd): nothing
 * displaces the lower default, and the session faults with the reference interpreter's sentence. Beside them the
 * document of the lens that found the defect: no rules, wrak as input, the first faulting form below the lower
 * default.
 *
 * <p><b>Measured</b> (2026-10-09, the reference interpreter's runner on every document in both orders of its rules,
 * each document verifier-clean): V1 passes everywhere, heffing 10 and consistent; V2 faults with "the evaluationYear
 * context value needs an evaluation date; the case carries asOfDate null", "Unresolved reached in evaluation: this rule
 * fails observably", "Unresolved condition reached in evaluation" and "duration count 18446744073709551617 is out of
 * range". The ASP runner declines every one of these documents at translation (the context value without a date,
 * "Unresolved reached in translation", a count beyond its 32-bit integers). The deposit variant, a division by a leeg
 * noemer in the justification of the faulting default, which this branch refuses as a division in a default: the
 * reference interpreter passes V1 consistent, and the ASP runner deposits on V1, under a rule that is not the
 * default's ("wrakverklaring × V1" with the two rules, "? × V1" without them).
 *
 * <p><b>Red before the change</b> (measured 2026-10-09 on 8185e0d6): all three tests failed. Every V1, in every place,
 * form and order, ended in the fault of the displaced exception group, none of the fifty played; the lens document
 * ended in "Rekenfout: fout in de regel "defaults A:Vondst.heffing": the evaluationYear context value needs an
 * evaluation date; the case carries asOfDate null"; and the sample failed in its first order on that fault.
 */
class TmsVerdrongenUitzonderingWitnessTest {

    private static final String GEHEEL = getaltype(true, 0, null), DAGEN = getaltype(true, 0, eenheid("dagen"));
    private static final String TE_GROOT = "18446744073709551617";
    private static final String STAPEL = "defaults A:Vondst.heffing";
    private static final String REKENJAAR = "the evaluationYear context value needs an evaluation date; the case"
            + " carries asOfDate null";

    private static final String MODEL = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst",
            List.of(attribuut("heffing", GEHEEL), attribuut("vinddatum", DATUM)),
            List.of("wrak", "gestrand", "aangespoeld", "geregistreerd"))), List.of());

    private static final List<String> REGELS = List.of(
            kenmerkregel("stranding", "Vondst", "gestrand", check("aangespoeld", false)),
            kenmerkregel("wrakverklaring", "Vondst", "wrak", check("gestrand", false)));

    private static final String TIEN = standaard(check("wrak", false), getal("10"));
    private static final String DERTIG = standaard(check("geregistreerd", false), getal("30"));

    /** A default that faults where it is evaluated, and the sentence the reference interpreter faults with there. */
    private record Fout(String naam, String standaard, String zin) {}

    private static final List<Fout> FOUTEN = List.of(
            new Fout("het rekenjaar in de rechtvaardiging", standaard(vergelijking("greater",
                    contextwaarde("evaluationYear", GEHEEL), literaal("2025", GEHEEL)), getal("20")), REKENJAAR),
            new Fout("het rekenjaar als gevolg", standaard(check("geregistreerd", false),
                    contextwaarde("evaluationYear", GEHEEL)), REKENJAAR),
            new Fout("een onopgeloste waarde als gevolg", standaard(check("geregistreerd", false),
                    "{\"node\":\"Unresolved\",\"type\":" + GEHEEL + ",\"source\":{\"snippet\":\"het niet-gemodelleerde"
                            + " tarief\"}}"), "Unresolved reached in evaluation: this rule fails observably"),
            new Fout("een onopgeloste rechtvaardiging", standaard("{\"node\":\"Unresolved\",\"source\":{\"snippet\":"
                    + "\"een niet-gemodelleerde grond\"}}", getal("20")), "Unresolved condition reached in evaluation"),
            new Fout("een datumstap buiten het bereik in de rechtvaardiging", standaard(vergelijking("later",
                    binair("plus", lezing("vinddatum", "Vondst"), literaal(TE_GROOT, DAGEN), DATUM),
                    literaal("2026-01-01", DATUM)), getal("20")), "duration count " + TE_GROOT + " is out of range"));

    /** Where the faulting default sits: the defaults of the group, highest first, around it. */
    private record Plaats(String naam, Function<String, List<String>> defaults) {}

    private static final List<Plaats> PLAATSEN = List.of(
            new Plaats("een uitzonderingsgroep van de lagere default",
                    f -> List.of(TIEN, groepsdefault(List.of(List.of(f))))),
            new Plaats("een uitzonderingsgroep van een uitzonderingsgroep van de lagere default",
                    f -> List.of(TIEN, groepsdefault(List.of(List.of(groepsdefault(List.of(List.of(f)))))))),
            new Plaats("een uitzonderingsgroep van een default die een lidketen verdringt",
                    f -> List.of(groepsdefault(List.of(List.of(TIEN, groepsdefault(List.of(List.of(f)))))))),
            new Plaats("het eerste van twee leden van de lagere default",
                    f -> List.of(TIEN, groepsdefault(List.of(List.of(f), List.of(DERTIG))))),
            new Plaats("het tweede van twee leden van de lagere default",
                    f -> List.of(TIEN, groepsdefault(List.of(List.of(DERTIG), List.of(f))))));

    private static String groep(Plaats p, Fout f) {
        return attribuutgroep("heffing van een vondst", "Vondst", "heffing", p.defaults().apply(f.standaard()));
    }

    /** V1: the higher default decides 10 once wrak is derived. */
    private static JsonNode verdrongen() throws Exception {
        return TmsDocumenten.zaakOp(null, List.of(instantieMet("V1", "Vondst",
                        Map.of("vinddatum", datumwaarde("2026-03-01")), List.of("aangespoeld", "geregistreerd"))),
                List.of(waarde("V1", "heffing", "10"), consistent()));
    }

    /** V2: nothing displaces the lower default, so its exception groups are evaluated. */
    private static JsonNode bereikt() throws Exception {
        return TmsDocumenten.zaakOp(null, List.of(instantieMet("V2", "Vondst",
                Map.of("vinddatum", datumwaarde("2026-03-01")), List.of("geregistreerd"))), List.of(consistent()));
    }

    private static String fout(Fout f) {
        return "Rekenfout: fout in de regel \"" + STAPEL + "\": " + f.zin();
    }

    @Test void het_document_van_de_lens() throws Exception {
        String model = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst",
                List.of(attribuut("heffing", GEHEEL)), List.of("wrak"))), List.of());
        Path ir = TmsDocumenten.document(model, List.of(), List.of(), List.of(groep(PLAATSEN.get(0), FOUTEN.get(0))));
        JsonNode wrak = TmsDocumenten.zaakOp(null, List.of(instantieMet("V1", "Vondst", Map.of(), List.of("wrak"))),
                List.of(waarde("V1", "heffing", "10"), consistent()));
        JsonNode geenWrak = TmsDocumenten.zaakOp(null, List.of(instantieMet("V2", "Vondst", Map.of(), List.of())),
                List.of(consistent()));
        assertEquals(List.of("gespeeld", fout(FOUTEN.get(0))), List.of(
                TmsFoutWitnessTest.speel(ir, wrak, DroolsBackend.Branch.TRUTH_MAINTENANCE),
                TmsFoutWitnessTest.speel(ir, geenWrak, DroolsBackend.Branch.TRUTH_MAINTENANCE)));
    }

    @Test void elke_plaats_en_elke_fout_in_elke_volgorde_van_de_regels() throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (Plaats p : PLAATSEN)
            for (Fout f : FOUTEN)
                for (List<String> volgorde : TmsDocumenten.permutaties(REGELS)) {
                    Path ir = TmsDocumenten.document(MODEL, volgorde, List.of(), List.of(groep(p, f)));
                    String wat = p.naam() + ", " + f.naam() + ", " + TmsDocumenten.regelnamen(volgorde);
                    uitkomsten.add(wat + ", V1: " + TmsFoutWitnessTest.speel(ir, verdrongen(),
                            DroolsBackend.Branch.TRUTH_MAINTENANCE));
                    verwacht.add(wat + ", V1: gespeeld");
                    uitkomsten.add(wat + ", V2: " + TmsFoutWitnessTest.speel(ir, bereikt(),
                            DroolsBackend.Branch.TRUTH_MAINTENANCE));
                    verwacht.add(wat + ", V2: " + fout(f));
                }
        assertEquals(verwacht, uitkomsten);
    }

    /**
     * A seeded sample of the orders of the generated rules, on V1, with the fault in the justification and in the
     * consequence in every place: the fire reaches its fixpoint without throwing, no fault stands there, every
     * {@link Uitkomst} at the fixpoint decides 10, so no exception group below the displaced default settles anything,
     * and the heffing is 10. In some order a displaced exception group was evaluated before wrak was derived; its fault
     * went again when the higher default decided.
     */
    @Test void steekproef_van_volgordes_van_de_gegenereerde_regels() throws Exception {
        for (Plaats p : PLAATSEN)
            for (Fout f : FOUTEN.subList(0, 2)) {
                String wat = p.naam() + ", " + f.naam();
                DroolsBackend.Build b = DroolsBackend.build(TmsDocumenten.document(MODEL, REGELS, List.of(),
                        List.of(groep(p, f))), DroolsBackend.Branch.TRUTH_MAINTENANCE);
                int tussenstand = 0;
                List<Rule> gegenereerd = b.tmsTranspiler().model().getRules();
                for (List<Rule> volgorde : TmsDocumenten.steekproef(gegenereerd, 20261009L, 40)) {
                    List<String> namen = volgorde.stream().map(Rule::getName).toList();
                    Object v1 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of(),
                            List.of("aangespoeld", "geregistreerd"));
                    TmsFoutWitnessTest.Foutfeiten fouten = new TmsFoutWitnessTest.Foutfeiten();
                    List<Object> uitkomsten = new ArrayList<>();
                    vuur(b, volgorde, v1, fouten, uitkomsten, wat + ", volgorde " + namen);
                    assertEquals(fouten.ingevoegd, fouten.ingetrokken, () -> wat + ": een fout staat aan het fixpunt in"
                            + " volgorde " + namen);
                    for (Object u : uitkomsten)
                        assertTrue(((Uitkomst) u).waarde() instanceof BigDecimal w && w.compareTo(BigDecimal.TEN) == 0,
                                () -> wat + ": " + u + " staat aan het fixpunt in volgorde " + namen);
                    TmsFoutWitnessTest.assertGetal("10", TmsDocumenten.lees(v1, "heffing"),
                            wat + ": heffing in volgorde " + namen);
                    if (fouten.ingevoegd > 0) tussenstand++;
                }
                assertTrue(tussenstand > 0, wat + ": geen volgorde rekende de verdrongen uitzonderingsgroep eerst uit");
            }
    }

    /** Fires {@code boon} through the rules of {@code b} added in {@code volgorde}: the fire must reach its fixpoint
     *  without throwing, the write-back must raise no fault, and {@code uitkomsten} receives the {@link Uitkomst} facts
     *  of the fixpoint. */
    private static void vuur(DroolsBackend.Build b, List<Rule> volgorde, Object boon,
                             TmsFoutWitnessTest.Foutfeiten fouten, List<Object> uitkomsten, String wat) {
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        KieSession sessie = TmsDocumenten.kieBase(b, volgorde).newKieSession();
        try {
            sessie.addEventListener(fouten);
            sessie.insert(boon);
            int vuringen;
            try {
                vuringen = sessie.fireAllRules(100_000);
            } catch (RuntimeException e) {
                fail(wat + ": de fire wierp " + e);
                return;
            }
            assertTrue(vuringen < 100_000, () -> wat + ": geen fixpunt");
            try {
                DroolsBackend.zetAfgeleideWaarden(b, sessie);
            } catch (RuntimeException e) {
                fail(wat + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            uitkomsten.addAll(sessie.getObjects(o -> o instanceof Uitkomst));
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }
}
