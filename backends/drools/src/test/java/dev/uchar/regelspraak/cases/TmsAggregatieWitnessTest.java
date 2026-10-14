package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.tms.Lijstplaats;
import dev.uchar.regelspraak.transpiler.tms.Waarde;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocumenten.GEHEEL;
import static dev.uchar.regelspraak.cases.TmsDocumenten.TELLING;
import static dev.uchar.regelspraak.cases.TmsDocumenten.aggregatie;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuutgroep;
import static dev.uchar.regelspraak.cases.TmsDocumenten.binair;
import static dev.uchar.regelspraak.cases.TmsDocumenten.bronpad;
import static dev.uchar.regelspraak.cases.TmsDocumenten.bronrol;
import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.consistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.feittypeEenNaarVeel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.gelijsteVouw;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.instantieMetRollen;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rolbinding;
import static dev.uchar.regelspraak.cases.TmsDocumenten.standaard;
import static dev.uchar.regelspraak.cases.TmsDocumenten.vergelijking;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Aggregations on the exec-model branch (RUNTIME-SEMANTICS ch. 1.2 "Aggregations", ch. 3.4a-c): a fold over the
 * members its range reaches recomputes when a member, or a value derived for a member, changes, and its activation
 * that fires again leaves one value for its target, not two.
 *
 * <p>The model: a Jutter reports Vondsten (aanmelding: aanmelder, one Jutter; vondst, many Vondsten, plural vondsten).
 * A Vondst has taxatie and bergloon and the characteristics gemeld, erkend and vrij; "Een Vondst is erkend indien hij
 * gemeld is", "Een Vondst is vrij indien hij niet erkend is"; a defaults group on the bergloon, higher "op zijn taxatie
 * indien hij erkend is", lower "op 5 indien hij gemeld is". The folds: "de som van het bergloon van alle vondsten van de
 * Jutter, of 0 als die er niet zijn" (bergsom), "het aantal vondsten van de Jutter die erkend zijn" (erkendaantal) and
 * "… die vrij zijn" (vrijaantal). J1 reports V1 (gemeld, taxatie 30), V2 (gemeld, taxatie 20) and V3 (taxatie 10, not
 * gemeld, so no bergloon); J2 reports nothing. Fixpoint: bergsom 50, erkendaantal 2 and vrijaantal 1 for J1, the leeg
 * bergloon of V3 skipped; 0, 0 and 0 for J2, the sum by its fallback. On the way the folds pass the lower default's 5
 * as a member's value, a count of erkende vondsten below 2, and a count of vrije vondsten that still holds V1 and V2,
 * which are vrij until erkend is derived for them: a contribution that stayed after its member changed would show in
 * every one of the three. The reference interpreter's runner and the ASP runner passed every case of this class in
 * every document order (measured 2026-10-08), the two standing divisions by zero excepted, on which both fault.
 *
 * <p>Besides: a fold in the justification and the consequence of a default; a division by a count that is 0 on the
 * way and 2 at the fixpoint, beside one whose count stays 0; a fault in a fold's filter; a listed fold over an operand
 * derived later; and a two-step range in which one element is reached along two routes and contributes once
 * (ch. 3.4b).
 *
 * <p><b>Red before the change</b> (measured 2026-10-08 on the branch after its first step): every row of this branch
 * was refused, "Excluded: tms-transpiler SKIP: regel: bergsom — een Aggregation in een waarde ('de som van het bergloon
 * van alle vondsten van de Jutter of 0 als die er niet zijn'): buiten de proef van de truth-maintenance-tak", the default
 * on "een Aggregation in een waarde ('het aantal vondsten van de Jutter die erkend zijn')", and the listed fold, which
 * carries no element type, as a builtin: "een BuiltinCall in een waarde ('de eerste van zijn taxatie en zijn
 * bergloon')". The exec-model rows were green before.
 *
 * <p><b>Red by mutation</b> (measured the same day, not committed). With the contributions inserted by {@code insert}
 * instead of {@code logicalInsert}, a contribution outlived the value it was made from: "bergsom van J1 in volgorde
 * [kenmerkstand Vondst.vrij: onwaar, …]", the document orders, the default and the division failed, five rows in all.
 * (Before the lower default was 5 and the count of vrije vondsten was added, the stale contributions were 0 and a
 * member that stayed erkend, and that mutant passed every row; hence those two.) With contributions that never merge
 * (equality by identity), this class passed and the corpus witness failed on the five goldens with an instance-set
 * union, "jutterswet-keurunie: … attribuut aantal keurstukken: verwacht 3, werkelijk 4": two selections of a union
 * select one member twice, and only the merge of equal contributions counts it once.
 */
class TmsAggregatieWitnessTest {

    static final String MODEL = TmsDocumenten.model(List.of(
            TmsDocumenten.objecttype("Jutter", List.of(attribuut("bergsom", GEHEEL), attribuut("erkendaantal", GEHEEL),
                    attribuut("vrijaantal", GEHEEL), attribuut("gemiddeld bergloon", GEHEEL), attribuut("premie", GEHEEL)),
                    List.of()),
            TmsDocumenten.objecttype("Vondst", List.of(attribuut("taxatie", GEHEEL), attribuut("bergloon", GEHEEL),
                    attribuut("ondergrens", GEHEEL)), List.of("gemeld", "erkend", "vrij"))),
            List.of(feittypeEenNaarVeel("aanmelding", "aanmelder", "Jutter", "vondst", "Vondst", "vondsten")));

    private static final String ERKENNING = kenmerkregel("erkenning", "Vondst", "erkend", check("gemeld", false));

    private static final String VRIJGAVE = kenmerkregel("vrijgave", "Vondst", "vrij", check("erkend", true));

    private static final String BERGLOON = attribuutgroep("bergloon van een vondst", "Vondst", "bergloon", List.of(
            standaard(check("erkend", false), lezing("taxatie", "Vondst")),
            standaard(check("gemeld", false), getal("5"))));

    private static final String VONDSTEN = "{\"node\":\"AttributeReference\",\"attribute\":\"vondsten\",\"subjectType\":"
            + "\"Jutter\",\"source\":{\"snippet\":\"vondsten van de Jutter\"}}";

    static final String SOM = aggregatie("sum", lezing("bergloon", "Vondst"), "Vondst", bronrol("aanmelding", "vondst"),
            "null", getal("0"), GEHEEL, "de som van het bergloon van alle vondsten van de Jutter of 0 als die er niet zijn");

    static final String ERKEND_AANTAL = aggregatie("count", VONDSTEN, "Vondst", bronrol("aanmelding", "vondst"),
            check("erkend", false), "null", TELLING, "het aantal vondsten van de Jutter die erkend zijn");

    static final String AANTAL = aggregatie("count", VONDSTEN, "Vondst", bronrol("aanmelding", "vondst"), "null", "null",
            TELLING, "het aantal vondsten van de Jutter");

    static final String VRIJ_AANTAL = aggregatie("count", VONDSTEN, "Vondst", bronrol("aanmelding", "vondst"),
            check("vrij", false), "null", TELLING, "het aantal vondsten van de Jutter die vrij zijn");

    static final List<String> REGELS = List.of(ERKENNING, VRIJGAVE,
            rekenregel("bergsom", "BEREKENING", "Jutter", "bergsom", SOM, "null"),
            rekenregel("erkendaantal", "BEREKENING", "Jutter", "erkendaantal", ERKEND_AANTAL, "null"),
            rekenregel("vrijaantal", "BEREKENING", "Jutter", "vrijaantal", VRIJ_AANTAL, "null"));

    static Path document(List<String> regels) throws Exception {
        return TmsDocumenten.document(MODEL, regels, List.of(), List.of(BERGLOON));
    }

    private static List<String> vondsten() {
        return List.of(
                instantieMetRollen("V1", "Vondst", Map.of("taxatie", "30"), List.of("gemeld"), Map.of()),
                instantieMetRollen("V2", "Vondst", Map.of("taxatie", "20"), List.of("gemeld"), Map.of()),
                instantieMetRollen("V3", "Vondst", Map.of("taxatie", "10"), List.of(), Map.of()));
    }

    static JsonNode zaak() throws Exception {
        List<String> instanties = new ArrayList<>(List.of(
                instantieMetRollen("J1", "Jutter", Map.of(), List.of(), Map.of("vondsten", List.of("V1", "V2", "V3"))),
                instantieMetRollen("J2", "Jutter", Map.of(), List.of(), Map.of())));
        instanties.addAll(vondsten());
        return TmsDocumenten.zaak(instanties, List.of(waarde("J1", "bergsom", "50"), waarde("J1", "erkendaantal", "2"),
                waarde("J1", "vrijaantal", "1"), waarde("J2", "bergsom", "0"), waarde("J2", "erkendaantal", "0"),
                waarde("J2", "vrijaantal", "0"), waarde("V1", "bergloon", "30"), waarde("V2", "bergloon", "20"),
                waarde("V3", "bergloon", null), consistent()));
    }

    // ---------------- a fold over members whose values arrive later ----------------

    @Test void elke_volgorde_van_de_regels_in_het_document() throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(REGELS)) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            uitkomsten.add(namen + " " + TmsFoutWitnessTest.speel(document(volgorde), zaak(),
                    DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(namen + " gespeeld");
        }
        assertEquals(verwacht, uitkomsten);
        assertEquals(120, uitkomsten.size());
    }

    /**
     * A seeded sample of orders of the generated rules. In every order the beans end at the fixpoint and the session
     * holds exactly one {@link Waarde} for the bergsom of J1 and one for its erkendaantal: when the result of an
     * accumulate changes, the activation that read it fires again, and Drools withdraws the logical facts its earlier
     * firing inserted and did not insert again. At least one order must let the bergsom fire on J1 more than once with
     * different values on the way, and the count fire on a count below 2.
     */
    @Test void een_opnieuw_vurende_vouw_laat_een_waarde_voor_het_doel() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(document(REGELS), DroolsBackend.Branch.EXEC_MODEL);
        List<Rule> gegenereerd = b.execModelTranspiler().model().getRules();
        int opnieuw = 0, onderweg = 0, uitgevallen = 0;
        for (List<Rule> volgorde : TmsDocumenten.steekproef(gegenereerd, 20261008L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of());
            Object j2 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of());
            Object v1 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("taxatie", "30"), List.of("gemeld"));
            Object v2 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("taxatie", "20"), List.of("gemeld"));
            Object v3 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("taxatie", "10"), List.of());
            for (Object v : List.of(v1, v2, v3)) zetRol(v, "aanmelder", j1);
            Map<String, List<Object>> gezien = new java.util.HashMap<>();
            List<Long> tellingen = new ArrayList<>(), vrijtellingen = new ArrayList<>();
            int[] waarden = {0, 0};
            TmsDocumenten.vuur(b, volgorde, List.of(j1, j2, v1, v2, v3), sessie ->
                    sessie.addEventListener(new DefaultAgendaEventListener() {
                        @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                            String regel = e.getMatch().getRule().getName();
                            if ((regel.equals("bergsom") || regel.equals("erkendaantal"))
                                    && e.getMatch().getObjects().contains(j1))
                                gezien.computeIfAbsent(regel, r -> new ArrayList<>()).add(gebondenGetal(e));
                            if (regel.equals("erkendaantal") && e.getMatch().getObjects().contains(j1))
                                tellingen.add(kleinsteGetal(e));
                            if (regel.equals("vrijaantal") && e.getMatch().getObjects().contains(j1))
                                vrijtellingen.add(kleinsteGetal(e));
                        }
                    }), sessie -> {
                        waarden[0] = sessie.getObjects(o -> o instanceof Waarde w && w.onderwerp() == j1
                                && w.attribuut().naam().equals("bergsom")).size();
                        waarden[1] = sessie.getObjects(o -> o instanceof Waarde w && w.onderwerp() == j1
                                && w.attribuut().naam().equals("erkendaantal")).size();
                    });
            assertEquals(List.of(1, 1), List.of(waarden[0], waarden[1]), () -> "Waarde-feiten van J1 in volgorde " + namen);
            assertGetal("50", TmsDocumenten.lees(j1, "bergsom"), "bergsom van J1 in volgorde " + namen);
            assertGetal("2", TmsDocumenten.lees(j1, "erkendaantal"), "erkendaantal van J1 in volgorde " + namen);
            assertGetal("1", TmsDocumenten.lees(j1, "vrijaantal"), "vrijaantal van J1 in volgorde " + namen);
            assertGetal("0", TmsDocumenten.lees(j2, "bergsom"), "bergsom van J2 in volgorde " + namen);
            assertGetal("0", TmsDocumenten.lees(j2, "erkendaantal"), "erkendaantal van J2 in volgorde " + namen);
            assertGetal("0", TmsDocumenten.lees(j2, "vrijaantal"), "vrijaantal van J2 in volgorde " + namen);
            List<Object> sommen = gezien.getOrDefault("bergsom", List.of());
            if (sommen.size() > 1 && sommen.stream().distinct().count() > 1) opnieuw++;
            if (tellingen.stream().anyMatch(x -> x != null && x < 2)) onderweg++;
            if (vrijtellingen.stream().anyMatch(x -> x != null && x > 1)) uitgevallen++;
        }
        assertTrue(opnieuw > 0, "geen volgorde liet de som van J1 met verschillende waarden opnieuw vuren");
        assertTrue(onderweg > 0, "geen volgorde liet de telling van J1 vuren op een telling onder 2");
        assertTrue(uitgevallen > 0, "geen volgorde liet de telling van vrije vondsten van J1 vuren voordat V1 en V2 hun"
                + " vrij verloren");
    }

    // ---------------- a fold in a default ----------------

    private static final String PREMIE = attribuutgroep("premie van een jutter", "Jutter", "premie", List.of(
            standaard(vergelijking("greater", ERKEND_AANTAL, getal("1")), aggregatie("sum", lezing("bergloon", "Vondst"),
                    "Vondst", bronrol("aanmelding", "vondst"), "null", "null", GEHEEL,
                    "de som van het bergloon van alle vondsten van de Jutter")),
            standaard(vergelijking("greater", AANTAL, getal("0")), getal("5"))));

    static final List<String> DEFAULTREGELS = List.of(ERKENNING);

    static Path defaultdocument(List<String> regels) throws Exception {
        return TmsDocumenten.document(MODEL, regels, List.of(), List.of(BERGLOON, PREMIE));
    }

    /** J1 as above: two erkende vondsten, so the higher default decides the sum, 50. J3 reports V4 (gemeld, taxatie 7):
     *  one erkende vondst, so the lower default decides 5. J2 reports nothing: no default holds, premie leeg. */
    static JsonNode defaultzaak() throws Exception {
        List<String> instanties = new ArrayList<>(List.of(
                instantieMetRollen("J1", "Jutter", Map.of(), List.of(), Map.of("vondsten", List.of("V1", "V2", "V3"))),
                instantieMetRollen("J2", "Jutter", Map.of(), List.of(), Map.of()),
                instantieMetRollen("J3", "Jutter", Map.of(), List.of(), Map.of("vondsten", List.of("V4")))));
        instanties.addAll(vondsten());
        instanties.add(instantieMetRollen("V4", "Vondst", Map.of("taxatie", "7"), List.of("gemeld"), Map.of()));
        return TmsDocumenten.zaak(instanties, List.of(waarde("J1", "premie", "50"), waarde("J2", "premie", null),
                waarde("J3", "premie", "5"), consistent()));
    }

    @Test void een_vouw_in_rechtvaardiging_en_consequentie_van_een_default() throws Exception {
        assertEquals("gespeeld", TmsFoutWitnessTest.speel(defaultdocument(DEFAULTREGELS), defaultzaak(),
                DroolsBackend.Branch.EXEC_MODEL));
    }

    /** The generated rules in a seeded sample of orders; at least one lets the lower default decide 5 for J1 before
     *  the count of its erkende vondsten reaches 2, and the higher default take over. */
    @Test void steekproef_van_de_default_met_vouwen() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(defaultdocument(DEFAULTREGELS), DroolsBackend.Branch.EXEC_MODEL);
        int overgenomen = 0;
        for (List<Rule> volgorde : TmsDocumenten.steekproef(b.execModelTranspiler().model().getRules(), 20261008L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of());
            Object j2 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of());
            Object j3 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of());
            Object v1 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("taxatie", "30"), List.of("gemeld"));
            Object v2 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("taxatie", "20"), List.of("gemeld"));
            Object v3 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("taxatie", "10"), List.of());
            Object v4 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("taxatie", "7"), List.of("gemeld"));
            for (Object v : List.of(v1, v2, v3)) zetRol(v, "aanmelder", j1);
            zetRol(v4, "aanmelder", j3);
            List<String> gevuurd = new ArrayList<>();
            TmsDocumenten.vuur(b, volgorde, List.of(j1, j2, j3, v1, v2, v3, v4), sessie ->
                    sessie.addEventListener(new DefaultAgendaEventListener() {
                        @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                            if (e.getMatch().getObjects().contains(j1)) gevuurd.add(e.getMatch().getRule().getName());
                        }
                    }), sessie -> { });
            assertGetal("50", TmsDocumenten.lees(j1, "premie"), "premie van J1 in volgorde " + namen);
            assertNull(TmsDocumenten.lees(j2, "premie"), () -> "premie van J2 in volgorde " + namen);
            assertGetal("5", TmsDocumenten.lees(j3, "premie"), "premie van J3 in volgorde " + namen);
            if (gevuurd.contains("defaults A:Jutter.premie: default 1")) overgenomen++;
        }
        assertTrue(overgenomen > 0, "geen volgorde liet de lagere default voor J1 beslissen voordat de telling 2 werd");
    }

    // ---------------- a division by a count ----------------

    private static final String GEMIDDELD = rekenregel("gemiddeld bergloon", "BEREKENING", "Jutter", "gemiddeld bergloon",
            binair("dividedBy", SOM, ERKEND_AANTAL), "null");

    static final List<String> DELINGREGELS = List.of(ERKENNING, GEMIDDELD);

    static Path delingdocument(List<String> regels) throws Exception {
        return TmsDocumenten.document(MODEL, regels, List.of(), List.of(BERGLOON));
    }

    /** J1 as above: 50 divided by 2, where the count passes 0 on the way. */
    static JsonNode tijdelijkeDelingzaak() throws Exception {
        List<String> instanties = new ArrayList<>(List.of(
                instantieMetRollen("J1", "Jutter", Map.of(), List.of(), Map.of("vondsten", List.of("V1", "V2", "V3")))));
        instanties.addAll(vondsten());
        return TmsDocumenten.zaak(instanties, List.of(waarde("J1", "gemiddeld bergloon", "25"), consistent()));
    }

    /** J2 reports nothing: the sum is its fallback 0 and the count 0, at the fixpoint as well. */
    static JsonNode blijvendeDelingzaak() throws Exception {
        return TmsDocumenten.zaak(List.of(instantieMetRollen("J2", "Jutter", Map.of(), List.of(), Map.of())),
                List.of(consistent()));
    }

    @Test void een_deling_door_een_telling_die_onderweg_nul_is() throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(DELINGREGELS)) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            uitkomsten.add(namen + " " + TmsFoutWitnessTest.speel(delingdocument(volgorde), tijdelijkeDelingzaak(),
                    DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(namen + " gespeeld");
            uitkomsten.add(namen + " " + TmsFoutWitnessTest.speel(delingdocument(volgorde), blijvendeDelingzaak(),
                    DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(namen + " DelingDoorNul: deling door nul in de regel \"gemiddeld bergloon\": 0 gedeeld door 0 —"
                    + " een numerieke nul als deler is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4)");
        }
        assertEquals(verwacht, uitkomsten);
        assertEquals(4, uitkomsten.size());
    }

    /** The generated rules in a seeded sample of orders: the fire ends at its fixpoint without throwing, and at least
     *  one order divides by the count while it is still 0, a fault fact that is gone at the fixpoint. */
    @Test void steekproef_van_de_deling_door_een_telling() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(delingdocument(DELINGREGELS), DroolsBackend.Branch.EXEC_MODEL);
        int onderweg = 0;
        for (List<Rule> volgorde : TmsDocumenten.steekproef(b.execModelTranspiler().model().getRules(), 20261008L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of());
            Object v1 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("taxatie", "30"), List.of("gemeld"));
            Object v2 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("taxatie", "20"), List.of("gemeld"));
            Object v3 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("taxatie", "10"), List.of());
            for (Object v : List.of(v1, v2, v3)) zetRol(v, "aanmelder", j1);
            TmsFoutWitnessTest.Foutfeiten feiten = new TmsFoutWitnessTest.Foutfeiten();
            assertNull(TmsFoutWitnessTest.vuur(b, volgorde, List.of(j1, v1, v2, v3), new DefaultAgendaEventListener() {},
                    feiten), () -> "volgorde " + namen);
            assertEquals(feiten.ingevoegd, feiten.ingetrokken, () -> "foutfeiten in volgorde " + namen);
            assertGetal("25", TmsDocumenten.lees(j1, "gemiddeld bergloon"), "gemiddeld bergloon in volgorde " + namen);
            if (feiten.ingevoegd > 0) onderweg++;
        }
        assertTrue(onderweg > 0, "geen volgorde deelde door de telling terwijl die nog 0 was");
    }

    // ---------------- a fault in a fold's filter ----------------

    static final String FILTERMODEL = TmsDocumenten.model(List.of(
            TmsDocumenten.objecttype("Jutter", List.of(attribuut("zware vondsten", GEHEEL)), List.of()),
            TmsDocumenten.objecttype("Vondst", List.of(attribuut("gewicht", GEHEEL), attribuut("deelnemers", GEHEEL)),
                    List.of())),
            List.of(feittypeEenNaarVeel("aanmelding", "aanmelder", "Jutter", "vondst", "Vondst", "vondsten")));

    static final List<String> FILTERREGELS = List.of(rekenregel("zware vondsten", "BEREKENING", "Jutter",
            "zware vondsten", aggregatie("count", VONDSTEN, "Vondst", bronrol("aanmelding", "vondst"),
                    vergelijking("greater", binair("dividedBy", lezing("gewicht", "Vondst"), lezing("deelnemers", "Vondst")),
                            getal("2")), "null", TELLING, "het aantal vondsten van de Jutter die aan de volgende voorwaarde"
                            + " voldoen: zijn gewicht gedeeld door zijn deelnemers is groter dan 2"), "null"));

    /** J1 reports V1 (gewicht 10, deelnemers 2: 5, counted) and V2 (gewicht 9, deelnemers 0): the filter divides 9 by
     *  0 for V2, and the rule, which has no condition, reaches the fold: a fault at the fixpoint, on J1 and named after
     *  the rule. */
    static JsonNode filterzaak() throws Exception {
        return TmsDocumenten.zaak(List.of(
                instantieMetRollen("J1", "Jutter", Map.of(), List.of(), Map.of("vondsten", List.of("V1", "V2"))),
                instantieMetRollen("V1", "Vondst", Map.of("gewicht", "10", "deelnemers", "2"), List.of(), Map.of()),
                instantieMetRollen("V2", "Vondst", Map.of("gewicht", "9", "deelnemers", "0"), List.of(), Map.of())),
                List.of(consistent()));
    }

    static Path filterdocument(List<String> regels) throws Exception {
        return TmsDocumenten.document(FILTERMODEL, regels);
    }

    /** The fault of a fold's filter is the fault of the rule that reads the fold, which the rule raises where its
     *  evaluation reaches the fold while the member's {@code Filterfout} stands ({@code TmsFilterfoutWitnessTest}),
     *  reported at the fixpoint with the sentence the exec-model branch gives for the same filter, and the fire ends at
     *  its fixpoint in every order of the generated rules. */
    @Test void een_fout_in_het_filter_van_een_vouw_staat_aan_het_fixpunt() throws Exception {
        String zin = "DelingDoorNul: deling door nul in de regel \"zware vondsten\": 9 gedeeld door 0 — een numerieke"
                + " nul als deler is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4)";
        assertEquals(zin, TmsFoutWitnessTest.speel(filterdocument(FILTERREGELS), filterzaak(),
                DroolsBackend.Branch.EXEC_MODEL));
        DroolsBackend.Build b = DroolsBackend.build(filterdocument(FILTERREGELS), DroolsBackend.Branch.EXEC_MODEL);
        int volgordes = 0;
        for (List<Rule> volgorde : TmsDocumenten.permutaties(b.execModelTranspiler().model().getRules())) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of());
            Object v1 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("gewicht", "10", "deelnemers", "2"), List.of());
            Object v2 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("gewicht", "9", "deelnemers", "0"), List.of());
            zetRol(v1, "aanmelder", j1);
            zetRol(v2, "aanmelder", j1);
            // the places of the list the case would write, which a fold whose filter can fault reads
            List<Object> invoer = List.of(j1, v1, v2, new Lijstplaats(v1, "aanmelder", 0), new Lijstplaats(v2, "aanmelder", 1));
            assertEquals(zin, TmsFoutWitnessTest.vuur(b, volgorde, invoer, new DefaultAgendaEventListener() {},
                    new TmsFoutWitnessTest.Foutfeiten()), () -> "volgorde " + namen);
            volgordes++;
        }
        // four generated rules: the reading rule, the contribution rule, the filter fault rule and, since the filter
        // divides, the filter deposit rule (fixround 1 of tms-expressievormen-en-rekendatum)
        assertEquals(24, volgordes);
    }

    // ---------------- a listed fold ----------------

    private static final String ONDERGRENS = rekenregel("ondergrens", "BEREKENING", "Vondst", "ondergrens",
            gelijsteVouw("first", List.of(lezing("taxatie", "Vondst"), lezing("bergloon", "Vondst")), GEHEEL,
                    "de eerste van zijn taxatie en zijn bergloon"), "null");

    static final List<String> GELIJSTREGELS = List.of(ERKENNING, ONDERGRENS);

    /** "De eerste van zijn taxatie en zijn bergloon", the least of the two (RUNTIME-SEMANTICS ch. 1.2e). V1 and V2: 5 on
     *  the way where the lower default decides first, then the taxatie; V3 has no bergloon, so the leeg operand is
     *  skipped and the fold is its taxatie. The ASP runner translates no listed minimum ("expression kind not
     *  translated", measured 2026-10-08), so the witness takes the eerste, which both runners carry. */
    static JsonNode gelijstezaak() throws Exception {
        return TmsDocumenten.zaak(vondsten(), List.of(waarde("V1", "ondergrens", "30"), waarde("V2", "ondergrens", "20"),
                waarde("V3", "ondergrens", "10"), consistent()));
    }

    @Test void een_gelijste_vouw_over_een_later_afgeleide_operand() throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(GELIJSTREGELS)) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            uitkomsten.add(namen + " " + TmsFoutWitnessTest.speel(document(volgorde), gelijstezaak(),
                    DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(namen + " gespeeld");
        }
        assertEquals(verwacht, uitkomsten);
    }

    // ---------------- a two-step range ----------------

    static final String PADMODEL = TmsDocumenten.model(List.of(
            TmsDocumenten.objecttype("Jutter", List.of(attribuut("vangstsom", GEHEEL)), List.of()),
            TmsDocumenten.objecttype("Boot", List.of(), List.of()),
            TmsDocumenten.objecttype("Bergplaats", List.of(attribuut("ruw gewicht", GEHEEL),
                    attribuut("vangstgewicht", GEHEEL)), List.of())),
            List.of(feittypeEenNaarVeel("bemanning", "schipper", "Jutter", "gevoerde boot", "Boot", "gevoerde boten"),
                    feittypeEenNaarVeel("lossing", "losplaats", "Bergplaats", "lossende boot", "Boot", "lossende boten")));

    static final List<String> PADREGELS = List.of(
            rekenregel("vangst", "BEREKENING", "Bergplaats", "vangstgewicht",
                    binair("plus", lezing("ruw gewicht", "Bergplaats"), getal("1")), "null"),
            rekenregel("vangstsom", "BEREKENING", "Jutter", "vangstsom", aggregatie("sum",
                    lezing("vangstgewicht", "Bergplaats"), "Bergplaats", bronpad(List.of(
                            rolbinding("bemanning", "gevoerde boot"), rolbinding("lossing", "losplaats"))),
                    "null", getal("0"), GEHEEL,
                    "de som van het vangstgewicht van alle losplaatsen van alle gevoerde boten van de Jutter"), "null"));

    /** J1 sails B1, B2 and B3; B1 and B2 unload at P1 (ruw gewicht 2, vangstgewicht 3), B3 at P2 (4, so 5). P1 is
     *  reached along two boats and contributes once: 3 + 5 = 8, not 11. J2 sails nothing: the fallback, 0. */
    static JsonNode padzaak() throws Exception {
        return TmsDocumenten.zaak(List.of(
                instantieMetRollen("J1", "Jutter", Map.of(), List.of(), Map.of("gevoerde boot", List.of("B1", "B2", "B3"))),
                instantieMetRollen("J2", "Jutter", Map.of(), List.of(), Map.of()),
                instantieMetRollen("B1", "Boot", Map.of(), List.of(), Map.of("losplaats", List.of("P1"))),
                instantieMetRollen("B2", "Boot", Map.of(), List.of(), Map.of("losplaats", List.of("P1"))),
                instantieMetRollen("B3", "Boot", Map.of(), List.of(), Map.of("losplaats", List.of("P2"))),
                instantieMetRollen("P1", "Bergplaats", Map.of("ruw gewicht", "2"), List.of(), Map.of()),
                instantieMetRollen("P2", "Bergplaats", Map.of("ruw gewicht", "4"), List.of(), Map.of())),
                List.of(waarde("J1", "vangstsom", "8"), waarde("J2", "vangstsom", "0"), consistent()));
    }

    static Path paddocument(List<String> regels) throws Exception {
        return TmsDocumenten.document(PADMODEL, regels);
    }

    @Test void een_lid_dat_via_twee_routes_bereikt_wordt_draagt_een_keer_bij() throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(PADREGELS)) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            uitkomsten.add(namen + " " + TmsFoutWitnessTest.speel(paddocument(volgorde), padzaak(),
                    DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(namen + " gespeeld");
        }
        assertEquals(verwacht, uitkomsten);
    }

    // ---------------- helpers ----------------

    /** The numbers a firing bound, by declaration: the results of its folds. */
    private static Object gebondenGetal(AfterMatchFiredEvent e) {
        Map<String, String> uit = new java.util.TreeMap<>();
        for (String d : e.getMatch().getDeclarationIds())
            if (e.getMatch().getDeclarationValue(d) instanceof Number n) uit.put(d, n.toString());
        return uit.toString();
    }

    /** The least number a firing bound, or null where it bound none. */
    private static Long kleinsteGetal(AfterMatchFiredEvent e) {
        Long uit = null;
        for (String d : e.getMatch().getDeclarationIds())
            if (e.getMatch().getDeclarationValue(d) instanceof Number n && (uit == null || n.longValue() < uit))
                uit = n.longValue();
        return uit;
    }

    /** Sets the to-one reference field {@code rol} of {@code boon} to {@code doel}, as the case reader does. */
    private static void zetRol(Object boon, String rol, Object doel) throws Exception {
        for (java.lang.reflect.Method m : boon.getClass().getMethods())
            if (m.getName().equals(dev.uchar.regelspraak.transpiler.kern.Namen.setter(rol)) && m.getParameterCount() == 1) {
                m.invoke(boon, doel);
                return;
            }
        throw new AssertionError("geen setter voor " + rol + " op " + boon.getClass());
    }

    private static void assertGetal(String verwacht, Object werkelijk, String wat) {
        assertTrue(werkelijk instanceof BigDecimal w && new BigDecimal(verwacht).compareTo(w) == 0,
                () -> wat + ": verwacht " + verwacht + ", werkelijk " + werkelijk);
    }

}
