package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.kern.Namen;
import dev.uchar.regelspraak.transpiler.tms.Stand;
import dev.uchar.regelspraak.transpiler.tms.Waarde;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.BeforeMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.event.rule.DefaultRuleRuntimeEventListener;
import org.kie.api.event.rule.ObjectInsertedEvent;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocumenten.GEHEEL;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocumenten.binair;
import static dev.uchar.regelspraak.cases.TmsDocumenten.consistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.feittypeEenNaarVeel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.geldigheid;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getaltype;
import static dev.uchar.regelspraak.cases.TmsDocumenten.inconsistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rolbinding;
import static dev.uchar.regelspraak.cases.TmsDocumenten.vergelijking;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Allocations on the truth-maintenance branch (RUNTIME-SEMANTICS ch. 4): the distribution is a computation over the
 * recipients its left-hand side binds, with every weight, ordering key, cap and pot bound there, so it follows a value
 * that a rule derives later, a recipient a creation adds, and a condition, and a degradation counts on the §9.5 channel
 * at the fixpoint only.
 *
 * <p>The model: a Berging (opbrengst, onverdeelde rest, rangrest, vrijgavescore, bruto) shares its opbrengst among its Bergers
 * (bergingsdeelname: berging, one Berging; berger, many Bergers). A Berger has the input basisgewicht, extra, jaar,
 * basismaximum and peiljaar (a number with one decimal), the derived toeslag, gewicht, rang, maximum and toezicht, and
 * the shares bergingsaandeel and rangaandeel. Every expectation below is computed by hand from ch. 4.2 to 4.4.
 *
 * <ul>
 *   <li><b>A, a weight and a key derived later.</b> "De toeslag van een berger is zijn extra", "het gewicht is zijn
 *       basisgewicht plus zijn toeslag", "de rang is zijn jaar min zijn toeslag", "het toezicht is zijn bergingsaandeel
 *       plus 1"; the opbrengst naar rato van het gewicht (afgerond op 0 decimalen, rest in de onverdeelde rest) and op
 *       volgorde van toenemende rang (rest in de rangrest). B1 has 900 and J1 (basisgewicht 1, extra 5, jaar 2020) and
 *       J2 (3, 0, 2018). Fixpoint: gewicht 6 and 3, so 600 and 300, rest 0; rang 2015 and 2018, so J1 900 and J2 0,
 *       rest 0; toezicht 601 and 301. Before the toeslag is derived the gewicht of J1 is 1 + leeg = 1 (ch. 1.2), shares
 *       225 and 675, and the rang of J1 is 2020, so J2 900: a share that stayed from that state shows.</li>
 *   <li><b>B, two allocations over one set of recipients</b> (the form of {@code 20260923-drl-verdeling-dubbelbezet}):
 *       1000 naar rato van het gewicht with a maximum, and op volgorde van toenemend jaar, at an equal jaar naar rato van
 *       het gewicht, with the same maximum. D1 (gewicht 3, jaar 2019, maximum 600), D2 (no gewicht, 2020, 600), D3 (1,
 *       2020, 600). Naar rato: D1 750 capped to 600, D3 250, D2 excluded and leeg with a deposit, rest 150. Op volgorde:
 *       D1 alone 1000 capped to 600, then 400 over D2 (an empty weight counts as 0, with a deposit) and D3, so 0 and
 *       400, rest 0.</li>
 *   <li><b>C, the degradations at the fixpoint.</b> "Het gewicht is zijn basisgewicht", "het maximum is zijn
 *       basismaximum"; 1200 naar rato with a maximum. J1 (3, no maximum), J2 (2, maximum -5), J3 (1, 300), J4 (no
 *       gewicht, 300): the worked example of ch. 4.6, 600, 400 and 200, an empty and a negative cap uncapped with a
 *       deposit each, J4 excluded with one; rest 0. Before the maximum of J3 is derived it is leeg, a deposit on J3 that
 *       must be gone at the fixpoint.</li>
 *   <li><b>D, the edge rows of ch. 4.4</b> that no golden holds: eligible weights that sum to 0 distribute nothing and
 *       write no rest, while an empty weight beside them still deposits; an empty pot distributes nothing in every
 *       method, as ch. 4.4 decides for v1, so no share and no rest is written, an input share of 7 stays, and there is
 *       no fault and no deposit, also beside an empty weight of the proportional method and an empty tie-break weight
 *       of the ordered one; a tie group whose weights are all empty gives every member a real 0 and passes the pot on;
 *       two ordering keys of one value in two scales are one group; and a negative proportional weight counts as 0
 *       with a deposit, as ch. 4.4 decides for v1.</li>
 *   <li><b>E, the validity of a version and the condition.</b> Two versions: in gelijke delen until 2025-12-31, naar
 *       rato van het basisgewicht from 2026-01-01 where the vrijgavescore is at least 10. 100 over J1 (1) and J2 (3):
 *       50 and 50 in 2025, 25 and 75 in 2026, nothing where the condition fails (J1's input share 7 stays), and a fault
 *       in a session without a date.</li>
 *   <li><b>F, a recipient a creation adds.</b> "Een Berging heeft een berger" makes a third Berger in the bergingsdeelname,
 *       a relation the branch then reads through its links; 900 in gelijke delen: J1 300, J2 300, rest 0, the made Berger
 *       counted.</li>
 *   <li><b>G, a pot derived later.</b> "De opbrengst van een berging is zijn bruto"; 90 in gelijke delen over J1, J2
 *       and J3: 30 each, rest 0. Before the opbrengst is derived the pot is leeg, which distributes nothing (ch. 4.4,
 *       decided for v1): a firing of the allocation on that state inserts no share, no rest, no fault and no
 *       deposit.</li>
 *   <li><b>H, a second writer of a target</b>: a computing rule or an initialisation beside an allocation on its
 *       target, and two allocations on one target, each refused by name.</li>
 * </ul>
 *
 * <p>Every part runs in every order of its rules and its allocations in the document, and in a seeded sample of orders
 * of the generated rules: A, C and G with a listener that requires at least one order to pass the intermediate state
 * (an inserted share of 225 for J1 and of 900 for J2, a deposit on J3, a firing of the allocation on the empty pot), B
 * with its channel, D, E and F through a replay of their cases in each order.
 *
 * <p><b>Measured in the other executors</b> (2026-10-09, the documents {@link #main} writes, every one verifier-clean
 * but H's two allocations, ill-formed on purpose). The reference interpreter's runner and the ASP runner pass A (15
 * expectations), B (10), C (8), F (4), G (6) and of D the eligible weights that sum to 0 (5), the empty pot of the
 * ordered method (4, no deposit beside the empty weight), the all-empty tie group (6) and the keys in two scales (5).
 * Twice the spec and the two executors part, each time where ch. 4.4 decides for v1 and they keep the v0 hole; both
 * executors are to follow the two decisions on the card 20261009-verdeling-lege-pot-en-negatief-gewicht. An empty pot of the equal-shares and the proportional
 * method is no distribution, as this branch reads it, where the reference interpreter faults ("EVALUATION FAULT: empty
 * source amount in equal-shares allocation (v0 mirror hole, ch. 4.4)", and "… in proportional allocation …") and the
 * ASP runner is unsatisfiable on its guard on either ({@code viol("allocation: empty pot","B1")}). A negative
 * proportional weight counts as 0 with a deposit, as this branch reads it, where the reference interpreter keeps the v0
 * hole ("J1.bergingsaandeel = 150, expected 100 … J2.bergingsaandeel = -50, expected 0 … the channel carries:
 * (empty)") and the ASP runner faults ({@code viol("allocation: negative weight","J2")}). E: the
 * reference interpreter passes the three sessions with a date (4 expectations each) and faults the time-free one, "a
 * validity-bounded form needs an evaluation date; this session is time-free"; the ASP runner does not translate the
 * validity window of an allocation ("allocation validity windows are not translated (no corpus case)").
 *
 * <p>The exec-model branch plays A, B, C, F and of D the zero weights and the ordered empty pot; on G it leaves the rest
 * leeg ("onverdeelde rest: verwacht 0, werkelijk leeg"), its rule for the rest not woken by a pot derived later; on D it
 * splits the
 * all-empty tie group equally ("rangaandeel: verwacht 0, werkelijk 50; … verwacht 100, werkelijk 0") and the keys in
 * two scales apart ("verwacht 50, werkelijk 0; … verwacht 50, werkelijk 100"), takes the v0 hole on the negative
 * weight, throws a NullPointerException on the empty pots of the other two methods, and refuses E, whose two versions
 * it reads as two allocations on one target. The DRL-text branch does the same on D, fails A ("rangaandeel: verwacht
 * 900, werkelijk 450; … verwacht 0, werkelijk 450"), throws on B ("ConsequenceException … cannot invoke method:
 * setBergingsaandeel", the fault of {@code 20260923-drl-verdeling-dubbelbezet}), deposits on J1 and J2 in E's 2026 and
 * does not fault E's time-free session; it plays G.
 *
 * <p><b>Red before the change</b> (2026-10-09, this class on the branch at the card's claim): all thirteen tests, each on
 * "Excluded: tms-transpiler SKIP: verdeling: verdeel de opbrengst naar rato — een Allocation: buiten de proef van de
 * truth-maintenance-tak" or the same refusal of the allocation of its part. <b>Red by mutation</b> (the same day, not
 * committed): the {@link dev.uchar.regelspraak.transpiler.tms.Ontvanger} stated instead of logical fails A's and C's
 * sample, a recipient that outlives its weight; the weight read off the bean in the recipient's consequence instead of
 * bound fails A and C in every form; the deposit stated fails B's and C's sample; a share stated fails A, C and F ("drie
 * schrijvers van één doel … schrijft 900, … 450 en … 300"); the ordering keys handed to {@code rsVerdeelOpVolgorde}
 * unnormalised fails D ("rangaandeel: verwacht 50, werkelijk 0; … verwacht 50, werkelijk 100"); an empty tie-break
 * weight handed on as leeg fails D ("verwacht 0, werkelijk 50; … verwacht 100, werkelijk 0"); a negative proportional
 * weight left unclamped fails D ("verwacht 100, werkelijk 150; … verwacht 0, werkelijk -50"); and the validity gate left
 * out fails E.
 *
 * <p><b>Red before an empty pot became no distribution</b> (measured 2026-10-09 on {@code e9182168} with this class,
 * where an empty pot of the equal-shares and the proportional method was the reference interpreter's fault, a
 * {@link dev.uchar.regelspraak.transpiler.tms.Fout} on the source): D, "lege-pot-gelijk=Rekenfout: fout in de regel
 * "verdeel de opbrengst gelijk": empty source amount in equal-shares allocation (v0 mirror hole, ch. 4.4),
 * lege-pot-naar-rato=Rekenfout: fout in de regel "verdeel de opbrengst naar rato": empty source amount in proportional
 * allocation (v0 mirror hole, ch. 4.4)", and G's sample, "ingevoegd op de lege pot in volgorde […] ==> expected:
 * &lt;[]&gt; but was: &lt;[Fout(Berging@…, verdeel de opbrengst gelijk, fout in de regel "verdeel de opbrengst gelijk":
 * empty source amount in equal-shares allocation (v0 mirror hole, ch. 4.4))]&gt;".
 */
class TmsVerdelingWitnessTest {

    private static final String EEN_DECIMAAL = getaltype(false, 1, null);

    static final String MODEL = TmsDocumenten.model(List.of(
            TmsDocumenten.objecttype("Berging", List.of(attribuut("opbrengst", GEHEEL), attribuut("onverdeelde rest", GEHEEL),
                    attribuut("rangrest", GEHEEL), attribuut("vrijgavescore", GEHEEL), attribuut("bruto", GEHEEL)), List.of()),
            TmsDocumenten.objecttype("Berger", List.of(attribuut("basisgewicht", GEHEEL), attribuut("extra", GEHEEL),
                    attribuut("toeslag", GEHEEL), attribuut("gewicht", GEHEEL), attribuut("jaar", GEHEEL),
                    attribuut("rang", GEHEEL), attribuut("basismaximum", GEHEEL), attribuut("maximum", GEHEEL),
                    attribuut("peiljaar", EEN_DECIMAAL), attribuut("bergingsaandeel", GEHEEL),
                    attribuut("rangaandeel", GEHEEL), attribuut("toezicht", GEHEEL)), List.of())),
            List.of(feittypeEenNaarVeel("bergingsdeelname", "berging", "Berging", "berger", "Berger", "bergers")));

    // ---------------- the wire of an allocation ----------------

    /** An Allocation of the given versions ({@link #versie}). */
    static String verdeling(String naam, List<String> versies) {
        return "{\"node\":\"Allocation\",\"name\":\"" + naam + "\",\"versions\":[" + String.join(",", versies) + "]}";
    }

    /** One version of an allocation of the opbrengst of a Berging over its bergers (the role berger of the
     *  bergingsdeelname), in the field order of IR-SPEC ch. 3.2; every optional field null for none, the validity null
     *  for always valid. */
    static String versie(String methode, String doel, String gewicht, String maximum, Integer decimalen, String rest,
                         String sleutel, boolean afnemend, String voorwaarde, String geldigheid) {
        StringBuilder b = new StringBuilder("{\"sourceType\":\"Berging\",\"sourceAttribute\":\"opbrengst\","
                + "\"targetType\":\"Berger\",\"targetAttribute\":\"" + doel + "\",\"method\":\"" + methode + "\"");
        if (gewicht != null) b.append(",\"ratioAttribute\":\"").append(gewicht).append('"');
        if (maximum != null) b.append(",\"maximumAttribute\":\"").append(maximum).append('"');
        if (decimalen != null) b.append(",\"roundingDecimals\":").append(decimalen);
        if (rest != null) b.append(",\"remainderAttribute\":\"").append(rest).append('"');
        if (sleutel != null) b.append(",\"orderingAttribute\":\"").append(sleutel).append('"');
        b.append(",\"descending\":").append(afnemend);
        if (voorwaarde != null) b.append(",\"condition\":").append(voorwaarde);
        b.append(",\"validity\":").append(geldigheid == null ? "{\"node\":\"Validity\"}" : geldigheid);
        b.append(",\"connectingRole\":").append(rolbinding("bergingsdeelname", "berger")).append('}');
        return b.toString();
    }

    /** The document of {@link #MODEL} with {@code regels}, {@code verdelingen} and {@code objectcreaties}, each in its
     *  order, as JSON. */
    static String ir(List<String> regels, List<String> verdelingen, List<String> objectcreaties) {
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + MODEL + ",\"rules\":{\"rules\":[" + String.join(",", regels)
                + "],\"dayKindDefinitions\":[],\"objectCreations\":[" + String.join(",", objectcreaties)
                + "],\"allocations\":[" + String.join(",", verdelingen) + "],\"consistencies\":[],\"uniquenesses\":[],"
                + "\"factCreations\":[],\"startPointDeterminations\":[],\"ruleGroups\":[],\"prioritizedDefaults\":[]},"
                + "\"diagnostics\":[]}";
    }

    static Path document(List<String> regels, List<String> verdelingen, List<String> objectcreaties) throws Exception {
        Path tmp = Files.createTempFile("tms-verdeling", ".json");
        tmp.toFile().deleteOnExit();
        return Files.writeString(tmp, ir(regels, verdelingen, objectcreaties));
    }

    /** An instance with whole-number attributes (a lexeme "leeg" writes none) and the bergers of a Berging. */
    static String instantie(String label, String type, Map<String, String> getallen, List<String> bergers) {
        Map<String, String> waarden = new LinkedHashMap<>(getallen);
        List<String> a = new ArrayList<>(), b = new ArrayList<>();
        waarden.forEach((naam, lexeem) -> a.add("\"" + naam + "\":{\"number\":\"" + lexeem + "\"}"));
        for (String berger : bergers) b.add("\"" + berger + "\"");
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + String.join(",", a)
                + "},\"roles\":{" + (bergers.isEmpty() ? "" : "\"bergers\":[" + String.join(",", b) + "]")
                + "},\"characteristics\":[]}";
    }

    /** "zijn {@code attribuut}" on a Berger. */
    static String berger(String attribuut) {
        return lezing(attribuut, "Berger");
    }

    /** Every order of the rules and, independently, of the allocations of a part, as documents, with the names of the
     *  forms in that order. */
    static Map<String, Path> volgordes(List<String> regels, List<String> verdelingen, List<String> objectcreaties)
            throws Exception {
        Map<String, Path> uit = new LinkedHashMap<>();
        for (List<String> r : TmsDocumenten.permutaties(regels))
            for (List<String> v : TmsDocumenten.permutaties(verdelingen))
                uit.put(TmsDocumenten.regelnamen(r) + " " + TmsDocumenten.regelnamen(v), document(r, v, objectcreaties));
        return uit;
    }

    private static String speel(Path ir, JsonNode zaak) throws Exception {
        return TmsFoutWitnessTest.speel(ir, zaak, DroolsBackend.Branch.EXEC_MODEL);
    }

    // ---------------- A: a weight and a key derived later ----------------

    static final String TOESLAG = rekenregel("toeslag van een berger", "BEREKENING", "Berger", "toeslag", berger("extra"),
            "null");
    static final String GEWICHT = rekenregel("gewicht van een berger", "BEREKENING", "Berger", "gewicht",
            binair("plus", berger("basisgewicht"), berger("toeslag")), "null");
    static final String RANG = rekenregel("rang van een berger", "BEREKENING", "Berger", "rang",
            binair("minus", berger("jaar"), berger("toeslag")), "null");
    static final String TOEZICHT = rekenregel("toezicht van een berger", "BEREKENING", "Berger", "toezicht",
            binair("plus", berger("bergingsaandeel"), getal("1")), "null");
    static final String NAAR_RATO = verdeling("verdeel de opbrengst naar rato", List.of(versie("proportional",
            "bergingsaandeel", "gewicht", null, 0, "onverdeelde rest", null, false, null, null)));
    static final String OP_RANG = verdeling("verdeel de opbrengst op rang", List.of(versie("ordered", "rangaandeel",
            null, null, 0, "rangrest", "rang", false, null, null)));
    static final List<String> A_REGELS = List.of(TOESLAG, GEWICHT, RANG, TOEZICHT);
    static final List<String> A_VERDELINGEN = List.of(NAAR_RATO, OP_RANG);

    static JsonNode aZaak() throws Exception {
        return TmsDocumenten.zaak(List.of(
                instantie("B1", "Berging", Map.of("opbrengst", "900"), List.of("J1", "J2")),
                instantie("J1", "Berger", Map.of("basisgewicht", "1", "extra", "5", "jaar", "2020"), List.of()),
                instantie("J2", "Berger", Map.of("basisgewicht", "3", "extra", "0", "jaar", "2018"), List.of())),
                List.of(waarde("J1", "toeslag", "5"), waarde("J2", "toeslag", "0"), waarde("J1", "gewicht", "6"),
                        waarde("J2", "gewicht", "3"), waarde("J1", "rang", "2015"), waarde("J2", "rang", "2018"),
                        waarde("J1", "bergingsaandeel", "600"), waarde("J2", "bergingsaandeel", "300"),
                        waarde("B1", "onverdeelde rest", "0"), waarde("J1", "rangaandeel", "900"),
                        waarde("J2", "rangaandeel", "0"), waarde("B1", "rangrest", "0"), waarde("J1", "toezicht", "601"),
                        waarde("J2", "toezicht", "301"), consistent()));
    }

    @Test void a_elke_volgorde_van_de_regels_en_de_verdelingen() throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (Map.Entry<String, Path> d : volgordes(A_REGELS, A_VERDELINGEN, List.of()).entrySet()) {
            uitkomsten.add(d.getKey() + " " + speel(d.getValue(), aZaak()));
            verwacht.add(d.getKey() + " gespeeld");
        }
        assertEquals(verwacht, uitkomsten);
        assertEquals(48, uitkomsten.size());
    }

    /**
     * A seeded sample of orders of the generated rules. In every order the beans end at the fixpoint and the session
     * holds one {@link Waarde} for each share; and in at least one order the allocation inserts a share of the state
     * before the toeslag is derived (J1 225 naar rato, J2 900 op rang), which truth maintenance withdraws.
     */
    @Test void a_steekproef_van_de_gegenereerde_regels() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(document(A_REGELS, A_VERDELINGEN, List.of()),
                DroolsBackend.Branch.EXEC_MODEL);
        int naarRatoOnderweg = 0, opRangOnderweg = 0;
        for (List<Rule> volgorde : TmsDocumenten.steekproef(b.execModelTranspiler().model().getRules(), 20261009L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object b1 = TmsDocumenten.boon(b.beanLoader(), "Berging", Map.of("opbrengst", "900"), List.of());
            Object j1 = TmsDocumenten.boon(b.beanLoader(), "Berger", Map.of("basisgewicht", "1", "extra", "5",
                    "jaar", "2020"), List.of());
            Object j2 = TmsDocumenten.boon(b.beanLoader(), "Berger", Map.of("basisgewicht", "3", "extra", "0",
                    "jaar", "2018"), List.of());
            koppel(j1, b1);
            koppel(j2, b1);
            List<Object> naarRato = new ArrayList<>(), opRang = new ArrayList<>();
            int[] waarden = new int[2];
            TmsDocumenten.vuur(b, volgorde, List.of(b1, j1, j2), sessie -> sessie.addEventListener(
                    new DefaultRuleRuntimeEventListener() {
                        @Override public void objectInserted(ObjectInsertedEvent e) {
                            if (!(e.getObject() instanceof Waarde w)) return;
                            if (w.onderwerp() == j1 && w.attribuut().naam().equals("bergingsaandeel"))
                                naarRato.add(w.waarde());
                            if (w.onderwerp() == j2 && w.attribuut().naam().equals("rangaandeel")) opRang.add(w.waarde());
                        }
                    }), sessie -> {
                        waarden[0] = sessie.getObjects(o -> o instanceof Waarde w && w.onderwerp() == j1
                                && w.attribuut().naam().equals("bergingsaandeel")).size();
                        waarden[1] = sessie.getObjects(o -> o instanceof Waarde w && w.onderwerp() == j2
                                && w.attribuut().naam().equals("rangaandeel")).size();
                    });
            assertEquals(List.of(1, 1), List.of(waarden[0], waarden[1]), () -> "Waarde-feiten in volgorde " + namen);
            Map<String, Object> eind = new LinkedHashMap<>();
            eind.put("J1 bergingsaandeel", TmsDocumenten.lees(j1, "bergingsaandeel"));
            eind.put("J2 bergingsaandeel", TmsDocumenten.lees(j2, "bergingsaandeel"));
            eind.put("B1 onverdeelde rest", TmsDocumenten.lees(b1, "onverdeelde rest"));
            eind.put("J1 rangaandeel", TmsDocumenten.lees(j1, "rangaandeel"));
            eind.put("J2 rangaandeel", TmsDocumenten.lees(j2, "rangaandeel"));
            eind.put("B1 rangrest", TmsDocumenten.lees(b1, "rangrest"));
            eind.put("J1 toezicht", TmsDocumenten.lees(j1, "toezicht"));
            assertEquals("{J1 bergingsaandeel=600, J2 bergingsaandeel=300, B1 onverdeelde rest=0, J1 rangaandeel=900,"
                    + " J2 rangaandeel=0, B1 rangrest=0, J1 toezicht=601}", getallen(eind), () -> "volgorde " + namen);
            if (naarRato.stream().anyMatch(x -> isGetal(x, "225"))) naarRatoOnderweg++;
            if (opRang.stream().anyMatch(x -> isGetal(x, "900"))) opRangOnderweg++;
        }
        assertTrue(naarRatoOnderweg > 0, "geen volgorde liet de verdeling naar rato vuren op het gewicht van vóór de"
                + " toeslag");
        assertTrue(opRangOnderweg > 0, "geen volgorde liet de verdeling op rang vuren op de rang van vóór de toeslag");
    }

    // ---------------- B: two allocations over one set of recipients ----------------

    static final String B_NAAR_RATO = verdeling("verdeel de opbrengst naar rato", List.of(versie("proportional",
            "bergingsaandeel", "gewicht", "maximum", 0, "onverdeelde rest", null, false, null, null)));
    static final String B_OP_JAAR = verdeling("verdeel de opbrengst op jaar", List.of(versie("ordered", "rangaandeel",
            "gewicht", "maximum", 0, "rangrest", "jaar", false, null, null)));
    static final List<String> B_VERDELINGEN = List.of(B_NAAR_RATO, B_OP_JAAR);

    static JsonNode bZaak() throws Exception {
        return TmsDocumenten.zaak(List.of(
                instantie("B1", "Berging", Map.of("opbrengst", "1000"), List.of("D1", "D2", "D3")),
                instantie("D1", "Berger", Map.of("gewicht", "3", "jaar", "2019", "maximum", "600"), List.of()),
                instantie("D2", "Berger", Map.of("jaar", "2020", "maximum", "600"), List.of()),
                instantie("D3", "Berger", Map.of("gewicht", "1", "jaar", "2020", "maximum", "600"), List.of())),
                List.of(waarde("D1", "bergingsaandeel", "600"), waarde("D2", "bergingsaandeel", null),
                        waarde("D3", "bergingsaandeel", "250"), waarde("B1", "onverdeelde rest", "150"),
                        waarde("D1", "rangaandeel", "600"), waarde("D2", "rangaandeel", "0"),
                        waarde("D3", "rangaandeel", "400"), waarde("B1", "rangrest", "0"),
                        inconsistent("verdeel de opbrengst naar rato", "D2"),
                        inconsistent("verdeel de opbrengst op jaar", "D2")));
    }

    @Test void b_twee_verdelingen_over_dezelfde_ontvangers_in_elke_volgorde() throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (Map.Entry<String, Path> d : volgordes(List.of(), B_VERDELINGEN, List.of()).entrySet()) {
            uitkomsten.add(d.getKey() + " " + speel(d.getValue(), bZaak()));
            verwacht.add(d.getKey() + " gespeeld");
        }
        assertEquals(verwacht, uitkomsten);
    }

    /** The deposits of part B are those two and no more, in a seeded sample of orders of the generated rules. */
    @Test void b_alleen_de_lege_gewichten_deponeren() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(document(List.of(), B_VERDELINGEN, List.of()),
                DroolsBackend.Branch.EXEC_MODEL);
        for (List<Rule> volgorde : TmsDocumenten.steekproef(b.execModelTranspiler().model().getRules(), 20261009L, 20)) {
            Object b1 = TmsDocumenten.boon(b.beanLoader(), "Berging", Map.of("opbrengst", "1000"), List.of());
            Object d1 = TmsDocumenten.boon(b.beanLoader(), "Berger", Map.of("gewicht", "3", "jaar", "2019",
                    "maximum", "600"), List.of());
            Object d2 = TmsDocumenten.boon(b.beanLoader(), "Berger", Map.of("jaar", "2020", "maximum", "600"), List.of());
            Object d3 = TmsDocumenten.boon(b.beanLoader(), "Berger", Map.of("gewicht", "1", "jaar", "2020",
                    "maximum", "600"), List.of());
            for (Object d : List.of(d1, d2, d3)) koppel(d, b1);
            Map<Object, String> labels = labels(List.of(b1, d1, d2, d3), List.of("B1", "D1", "D2", "D3"));
            List<String> kanaal = new ArrayList<>();
            TmsDocumenten.vuur(b, volgorde, List.of(b1, d1, d2, d3), sessie -> {},
                    sessie -> kanaal.addAll(TmsDocumenten.inconsistenties(sessie, labels)));
            assertEquals(List.of("verdeel de opbrengst naar rato @ D2", "verdeel de opbrengst op jaar @ D2"), kanaal,
                    () -> "volgorde " + volgorde.stream().map(Rule::getName).toList());
        }
    }

    // ---------------- C: the degradations at the fixpoint ----------------

    static final String C_GEWICHT = rekenregel("gewicht van een berger", "BEREKENING", "Berger", "gewicht",
            berger("basisgewicht"), "null");
    static final String C_MAXIMUM = rekenregel("maximum van een berger", "BEREKENING", "Berger", "maximum",
            berger("basismaximum"), "null");
    static final String C_NAAR_RATO = verdeling("verdeel de opbrengst naar rato", List.of(versie("proportional",
            "bergingsaandeel", "gewicht", "maximum", null, "onverdeelde rest", null, false, null, null)));
    static final List<String> C_REGELS = List.of(C_GEWICHT, C_MAXIMUM);

    static JsonNode cZaak() throws Exception {
        return TmsDocumenten.zaak(List.of(
                instantie("B1", "Berging", Map.of("opbrengst", "1200"), List.of("J1", "J2", "J3", "J4")),
                instantie("J1", "Berger", Map.of("basisgewicht", "3"), List.of()),
                instantie("J2", "Berger", Map.of("basisgewicht", "2", "basismaximum", "-5"), List.of()),
                instantie("J3", "Berger", Map.of("basisgewicht", "1", "basismaximum", "300"), List.of()),
                instantie("J4", "Berger", Map.of("basismaximum", "300"), List.of())),
                List.of(waarde("J1", "bergingsaandeel", "600"), waarde("J2", "bergingsaandeel", "400"),
                        waarde("J3", "bergingsaandeel", "200"), waarde("J4", "bergingsaandeel", null),
                        waarde("B1", "onverdeelde rest", "0"), inconsistent("verdeel de opbrengst naar rato", "J1"),
                        inconsistent("verdeel de opbrengst naar rato", "J2"),
                        inconsistent("verdeel de opbrengst naar rato", "J4")));
    }

    @Test void c_degradaties_in_elke_volgorde() throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (Map.Entry<String, Path> d : volgordes(C_REGELS, List.of(C_NAAR_RATO), List.of()).entrySet()) {
            uitkomsten.add(d.getKey() + " " + speel(d.getValue(), cZaak()));
            verwacht.add(d.getKey() + " gespeeld");
        }
        assertEquals(verwacht, uitkomsten);
    }

    /**
     * A seeded sample of orders of the generated rules: at the fixpoint the channel holds the deposits on J1, J2 and J4
     * and none on J3, whose maximum is 300 there; in at least one order the allocation deposited on J3 on the way,
     * before the maximum was derived, and that deposit went again.
     */
    @Test void c_een_deponering_van_een_tussenstand_vervalt() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(document(C_REGELS, List.of(C_NAAR_RATO), List.of()),
                DroolsBackend.Branch.EXEC_MODEL);
        int onderweg = 0;
        for (List<Rule> volgorde : TmsDocumenten.steekproef(b.execModelTranspiler().model().getRules(), 20261009L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object b1 = TmsDocumenten.boon(b.beanLoader(), "Berging", Map.of("opbrengst", "1200"), List.of());
            Object j1 = TmsDocumenten.boon(b.beanLoader(), "Berger", Map.of("basisgewicht", "3"), List.of());
            Object j2 = TmsDocumenten.boon(b.beanLoader(), "Berger", Map.of("basisgewicht", "2", "basismaximum", "-5"),
                    List.of());
            Object j3 = TmsDocumenten.boon(b.beanLoader(), "Berger", Map.of("basisgewicht", "1", "basismaximum", "300"),
                    List.of());
            Object j4 = TmsDocumenten.boon(b.beanLoader(), "Berger", Map.of("basismaximum", "300"), List.of());
            for (Object j : List.of(j1, j2, j3, j4)) koppel(j, b1);
            Map<Object, String> labels = labels(List.of(b1, j1, j2, j3, j4), List.of("B1", "J1", "J2", "J3", "J4"));
            List<String> kanaal = new ArrayList<>();
            int[] opJ3 = new int[1];
            TmsDocumenten.vuur(b, volgorde, List.of(b1, j1, j2, j3, j4), sessie -> sessie.addEventListener(
                    new DefaultRuleRuntimeEventListener() {
                        @Override public void objectInserted(ObjectInsertedEvent e) {
                            if (e.getObject().getClass().getSimpleName().equals("Inconsistentie")
                                    && onderwerp(e.getObject()) == j3) opJ3[0]++;
                        }
                    }), sessie -> kanaal.addAll(TmsDocumenten.inconsistenties(sessie, labels)));
            assertEquals(List.of("verdeel de opbrengst naar rato @ J1", "verdeel de opbrengst naar rato @ J2",
                    "verdeel de opbrengst naar rato @ J4"), kanaal, () -> "volgorde " + namen);
            Map<String, Object> eind = new LinkedHashMap<>();
            eind.put("J1", nul(TmsDocumenten.lees(j1, "bergingsaandeel")));
            eind.put("J2", nul(TmsDocumenten.lees(j2, "bergingsaandeel")));
            eind.put("J3", nul(TmsDocumenten.lees(j3, "bergingsaandeel")));
            eind.put("J4", nul(TmsDocumenten.lees(j4, "bergingsaandeel")));
            eind.put("B1", nul(TmsDocumenten.lees(b1, "onverdeelde rest")));
            assertEquals("{J1=600, J2=400, J3=200, J4=leeg, B1=0}", getallen(eind), () -> "volgorde " + namen);
            if (opJ3[0] > 0) onderweg++;
        }
        assertTrue(onderweg > 0, "geen volgorde liet de verdeling op J3 deponeren voordat zijn maximum afgeleid was");
    }

    // ---------------- D: the edge rows of ch. 4.4 ----------------

    static final String D_NAAR_RATO = verdeling("verdeel de opbrengst naar rato", List.of(versie("proportional",
            "bergingsaandeel", "gewicht", null, 0, "onverdeelde rest", null, false, null, null)));
    static final String D_GELIJK = verdeling("verdeel de opbrengst gelijk", List.of(versie("equalShares",
            "bergingsaandeel", null, null, 0, "onverdeelde rest", null, false, null, null)));
    static final String D_OP_JAAR = verdeling("verdeel de opbrengst op jaar", List.of(versie("ordered", "rangaandeel",
            "gewicht", null, 0, "rangrest", "jaar", false, null, null)));
    static final String D_OP_PEILJAAR = verdeling("verdeel de opbrengst op peiljaar", List.of(versie("ordered",
            "rangaandeel", null, null, 0, "rangrest", "peiljaar", false, null, null)));

    /** The edge cases of part D: the allocation, the case input and its expectations, by name. */
    static Map<String, Object[]> randgevallen() throws Exception {
        Map<String, Object[]> uit = new LinkedHashMap<>();
        uit.put("gewichten-nul", new Object[]{D_NAAR_RATO, TmsDocumenten.zaak(List.of(
                instantie("B1", "Berging", Map.of("opbrengst", "100"), List.of("J1", "J2", "J3")),
                instantie("J1", "Berger", Map.of("gewicht", "0", "bergingsaandeel", "7"), List.of()),
                instantie("J2", "Berger", Map.of("gewicht", "0"), List.of()),
                instantie("J3", "Berger", Map.of(), List.of())),
                List.of(waarde("J1", "bergingsaandeel", "7"), waarde("J2", "bergingsaandeel", null),
                        waarde("J3", "bergingsaandeel", null), waarde("B1", "onverdeelde rest", null),
                        inconsistent("verdeel de opbrengst naar rato", "J3")))});
        uit.put("lege-pot-gelijk", new Object[]{D_GELIJK, TmsDocumenten.zaak(List.of(
                instantie("B1", "Berging", Map.of(), List.of("J1", "J2")),
                instantie("J1", "Berger", Map.of("bergingsaandeel", "7"), List.of()),
                instantie("J2", "Berger", Map.of(), List.of())),
                List.of(waarde("J1", "bergingsaandeel", "7"), waarde("J2", "bergingsaandeel", null),
                        waarde("B1", "onverdeelde rest", null), consistent()))});
        uit.put("lege-pot-naar-rato", new Object[]{D_NAAR_RATO, TmsDocumenten.zaak(List.of(
                instantie("B1", "Berging", Map.of(), List.of("J1", "J2")),
                instantie("J1", "Berger", Map.of("gewicht", "2", "bergingsaandeel", "7"), List.of()),
                instantie("J2", "Berger", Map.of(), List.of())),
                List.of(waarde("J1", "bergingsaandeel", "7"), waarde("J2", "bergingsaandeel", null),
                        waarde("B1", "onverdeelde rest", null), consistent()))});
        uit.put("lege-pot-op-volgorde", new Object[]{D_OP_JAAR, TmsDocumenten.zaak(List.of(
                instantie("B1", "Berging", Map.of(), List.of("J1", "J2")),
                instantie("J1", "Berger", Map.of("jaar", "2020"), List.of()),
                instantie("J2", "Berger", Map.of("jaar", "2020", "gewicht", "1"), List.of())),
                List.of(waarde("J1", "rangaandeel", null), waarde("J2", "rangaandeel", null),
                        waarde("B1", "rangrest", null), consistent()))});
        uit.put("lege-tiegewichten", new Object[]{D_OP_JAAR, TmsDocumenten.zaak(List.of(
                instantie("B1", "Berging", Map.of("opbrengst", "100"), List.of("J1", "J2", "J3")),
                instantie("J1", "Berger", Map.of("jaar", "2019"), List.of()),
                instantie("J2", "Berger", Map.of("jaar", "2019"), List.of()),
                instantie("J3", "Berger", Map.of("jaar", "2020", "gewicht", "1"), List.of())),
                List.of(waarde("J1", "rangaandeel", "0"), waarde("J2", "rangaandeel", "0"),
                        waarde("J3", "rangaandeel", "100"), waarde("B1", "rangrest", "0"),
                        inconsistent("verdeel de opbrengst op jaar", "J1"),
                        inconsistent("verdeel de opbrengst op jaar", "J2")))});
        uit.put("sleutel-in-twee-schalen", new Object[]{D_OP_PEILJAAR, TmsDocumenten.zaak(List.of(
                instantie("B1", "Berging", Map.of("opbrengst", "100"), List.of("J1", "J2", "J3")),
                instantie("J1", "Berger", Map.of("peiljaar", "2019"), List.of()),
                instantie("J2", "Berger", Map.of("peiljaar", "2019.0"), List.of()),
                instantie("J3", "Berger", Map.of("peiljaar", "2020"), List.of())),
                List.of(waarde("J1", "rangaandeel", "50"), waarde("J2", "rangaandeel", "50"),
                        waarde("J3", "rangaandeel", "0"), waarde("B1", "rangrest", "0"), consistent()))});
        uit.put("negatief-gewicht-naar-rato", new Object[]{D_NAAR_RATO, TmsDocumenten.zaak(List.of(
                instantie("B1", "Berging", Map.of("opbrengst", "100"), List.of("J1", "J2")),
                instantie("J1", "Berger", Map.of("gewicht", "3"), List.of()),
                instantie("J2", "Berger", Map.of("gewicht", "-1"), List.of())),
                List.of(waarde("J1", "bergingsaandeel", "100"), waarde("J2", "bergingsaandeel", "0"),
                        waarde("B1", "onverdeelde rest", "0"), inconsistent("verdeel de opbrengst naar rato", "J2")))});
        return uit;
    }

    @Test void d_de_randgevallen_van_hoofdstuk_4_4() throws Exception {
        Map<String, String> uitkomsten = new LinkedHashMap<>();
        for (Map.Entry<String, Object[]> r : randgevallen().entrySet())
            uitkomsten.put(r.getKey(), speel(document(List.of(), List.of((String) r.getValue()[0]), List.of()),
                    (JsonNode) r.getValue()[1]));
        Map<String, String> verwacht = new LinkedHashMap<>();
        verwacht.put("gewichten-nul", "gespeeld");
        verwacht.put("lege-pot-gelijk", "gespeeld");
        verwacht.put("lege-pot-naar-rato", "gespeeld");
        verwacht.put("lege-pot-op-volgorde", "gespeeld");
        verwacht.put("lege-tiegewichten", "gespeeld");
        verwacht.put("sleutel-in-twee-schalen", "gespeeld");
        verwacht.put("negatief-gewicht-naar-rato", "gespeeld");
        assertEquals(verwacht, uitkomsten);
    }

    // ---------------- E: the validity of a version and the condition ----------------

    static final String E_VERDELING = verdeling("verdeel de opbrengst per jaar", List.of(
            versie("equalShares", "bergingsaandeel", null, null, 0, "onverdeelde rest", null, false, null,
                    geldigheid(null, "2025-12-31")),
            versie("proportional", "bergingsaandeel", "basisgewicht", null, 0, "onverdeelde rest", null, false,
                    vergelijking("greaterOrEqual", lezing("vrijgavescore", "Berging"), getal("10")),
                    geldigheid("2026-01-01", null))));

    static JsonNode eZaak(String asOfDate, String score, List<String> verwachtingen) throws Exception {
        return TmsDocumenten.zaakOp(asOfDate, List.of(
                instantie("B1", "Berging", Map.of("opbrengst", "100", "vrijgavescore", score), List.of("J1", "J2")),
                instantie("J1", "Berger", Map.of("basisgewicht", "1", "bergingsaandeel", "7"), List.of()),
                instantie("J2", "Berger", Map.of("basisgewicht", "3"), List.of())), verwachtingen);
    }

    static Map<String, JsonNode> eZaken() throws Exception {
        Map<String, JsonNode> uit = new LinkedHashMap<>();
        uit.put("2025", eZaak("2025-06-01", "12", List.of(waarde("J1", "bergingsaandeel", "50"),
                waarde("J2", "bergingsaandeel", "50"), waarde("B1", "onverdeelde rest", "0"), consistent())));
        uit.put("2026", eZaak("2026-06-01", "12", List.of(waarde("J1", "bergingsaandeel", "25"),
                waarde("J2", "bergingsaandeel", "75"), waarde("B1", "onverdeelde rest", "0"), consistent())));
        uit.put("2026-niet-vrijgegeven", eZaak("2026-06-01", "5", List.of(waarde("J1", "bergingsaandeel", "7"),
                waarde("J2", "bergingsaandeel", null), waarde("B1", "onverdeelde rest", null), consistent())));
        uit.put("tijdvrij", eZaak(null, "12", List.of(consistent())));
        return uit;
    }

    @Test void e_de_geldigheid_van_de_versie_en_de_voorwaarde() throws Exception {
        Path ir = document(List.of(), List.of(E_VERDELING), List.of());
        Map<String, String> uitkomsten = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> z : eZaken().entrySet()) uitkomsten.put(z.getKey(), speel(ir, z.getValue()));
        String zonderDatum = "a validity-bounded form needs an evaluation date; this session is time-free";
        Map<String, String> verwacht = new LinkedHashMap<>();
        verwacht.put("2025", "gespeeld");
        verwacht.put("2026", "gespeeld");
        verwacht.put("2026-niet-vrijgegeven", "gespeeld");
        verwacht.put("tijdvrij", "Evaluatiefouten: aan het fixpunt staan 2 evaluatiefouten, in de regels \"verdeel de"
                + " opbrengst per jaar #1\", \"verdeel de opbrengst per jaar #2\": fout in de regel \"verdeel de opbrengst"
                + " per jaar #1\": " + zonderDatum + "; fout in de regel \"verdeel de opbrengst per jaar #2\": "
                + zonderDatum);
        assertEquals(verwacht, uitkomsten);
    }

    // ---------------- F: a recipient a creation adds ----------------

    static final String HULPBERGER = TmsCreatieWitnessTest.objectcreatie("maak een hulpberger", "Berging", "Berger",
            List.of(TmsCreatieWitnessTest.init("basisgewicht", getal("1"))), "null", "bergingsdeelname", "berger");
    static final String F_GELIJK = verdeling("verdeel de opbrengst gelijk", List.of(versie("equalShares",
            "bergingsaandeel", null, null, 0, "onverdeelde rest", null, false, null, null)));

    static JsonNode fZaak() throws Exception {
        return TmsDocumenten.zaak(List.of(
                instantie("B1", "Berging", Map.of("opbrengst", "900"), List.of("J1", "J2")),
                instantie("J1", "Berger", Map.of(), List.of()), instantie("J2", "Berger", Map.of(), List.of())),
                List.of(waarde("J1", "bergingsaandeel", "300"), waarde("J2", "bergingsaandeel", "300"),
                        waarde("B1", "onverdeelde rest", "0"), consistent()));
    }

    @Test void f_een_ontvanger_uit_een_creatie() throws Exception {
        assertEquals("gespeeld", speel(document(List.of(), List.of(F_GELIJK), List.of(HULPBERGER)), fZaak()));
    }

    // ---------------- G: a pot derived later ----------------

    static final String POT = rekenregel("opbrengst van een berging", "BEREKENING", "Berging", "opbrengst",
            lezing("bruto", "Berging"), "null");

    static JsonNode gZaak() throws Exception {
        return TmsDocumenten.zaak(List.of(
                instantie("B1", "Berging", Map.of("bruto", "90"), List.of("J1", "J2", "J3")),
                instantie("J1", "Berger", Map.of(), List.of()), instantie("J2", "Berger", Map.of(), List.of()),
                instantie("J3", "Berger", Map.of(), List.of())),
                List.of(waarde("B1", "opbrengst", "90"), waarde("J1", "bergingsaandeel", "30"),
                        waarde("J2", "bergingsaandeel", "30"), waarde("J3", "bergingsaandeel", "30"),
                        waarde("B1", "onverdeelde rest", "0"), consistent()));
    }

    @Test void g_een_pot_die_later_afgeleid_wordt() throws Exception {
        assertEquals("gespeeld", speel(document(List.of(POT), List.of(F_GELIJK), List.of()), gZaak()));
    }

    /** A seeded sample of orders of the generated rules: the write-back writes 30 three times and a rest of 0, and
     *  every firing of the allocation on the empty pot, before the opbrengst is derived, inserts nothing, no share, no
     *  rest, no fault and no deposit (ch. 4.4: an empty pot is no distribution); in at least one order the allocation
     *  fires so. */
    @Test void g_een_lege_pot_op_een_tussenstand_laat_niets_achter() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(document(List.of(POT), List.of(F_GELIJK), List.of()),
                DroolsBackend.Branch.EXEC_MODEL);
        int opLegePot = 0;
        for (List<Rule> volgorde : TmsDocumenten.steekproef(b.execModelTranspiler().model().getRules(), 20261009L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object b1 = TmsDocumenten.boon(b.beanLoader(), "Berging", Map.of("bruto", "90"), List.of());
            List<Object> bergers = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                Object j = TmsDocumenten.boon(b.beanLoader(), "Berger", Map.of(), List.of());
                koppel(j, b1);
                bergers.add(j);
            }
            List<Object> bonen = new ArrayList<>(List.of(b1));
            bonen.addAll(bergers);
            boolean[] legePot = new boolean[1];
            int[] vuringen = new int[1];
            List<String> ingevoegd = new ArrayList<>();
            TmsDocumenten.vuur(b, volgorde, bonen, sessie -> {
                sessie.addEventListener(new DefaultAgendaEventListener() {
                    @Override public void beforeMatchFired(BeforeMatchFiredEvent e) {
                        legePot[0] = e.getMatch().getRule().getName().equals("verdeel de opbrengst gelijk")
                                && e.getMatch().getObjects().stream().anyMatch(o -> o instanceof Stand s
                                        && s.onderwerp() == b1 && s.attribuut().naam().equals("opbrengst")
                                        && s.waarde() == null);
                        if (legePot[0]) vuringen[0]++;
                    }
                    @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                        legePot[0] = false;
                    }
                });
                sessie.addEventListener(new DefaultRuleRuntimeEventListener() {
                    @Override public void objectInserted(ObjectInsertedEvent e) {
                        if (legePot[0]) ingevoegd.add(String.valueOf(e.getObject()));
                    }
                });
            }, sessie -> { });
            Map<String, Object> eind = new LinkedHashMap<>();
            for (int i = 0; i < 3; i++) eind.put("J" + (i + 1), TmsDocumenten.lees(bergers.get(i), "bergingsaandeel"));
            eind.put("B1", TmsDocumenten.lees(b1, "onverdeelde rest"));
            assertEquals("{J1=30, J2=30, J3=30, B1=0}", getallen(eind), () -> "volgorde " + namen);
            assertEquals(List.of(), ingevoegd, () -> "ingevoegd op de lege pot in volgorde " + namen);
            if (vuringen[0] > 0) opLegePot++;
        }
        assertTrue(opLegePot > 0,
                "geen volgorde liet de verdeling op de lege pot vuren voordat de opbrengst afgeleid was");
    }

    // ---------------- H: a second writer of a target ----------------

    /**
     * An allocation whose target another form writes is refused by name, never computed beside it: a computing rule and
     * an initialisation on the share target are well-formed, and this branch does not carry them beside an allocation
     * yet; two allocations on one target are ill-formed (WF-026). The case pins the share the allocation writes, 100
     * (pot 100, J1 the only recipient, weight 1), beside a consistent channel. Measured 2026-10-09: the verifier passes
     * the first two documents and refuses the third, "WF-026 FAIL /rules/allocations/0/versions/0: two allocations write
     * target (Berger, bergingsaandeel)". The reference interpreter passes the initialisation pair (2 expectations), the
     * share replacing the initialisation's 5, and fails the same case with 5 pinned, "MISMATCH: J1.bergingsaandeel =
     * 100, expected 5". It faults the rule pair before any expectation, two writers that disagree as ch. 5.7 has them:
     * "two writers of one target disagree: rule "bergingsaandeel van een berger" writes 5 and allocation "verdeel de
     * opbrengst naar rato" writes 100 to the attribute bergingsaandeel of J1, a Berger". The ASP runner derives both
     * values in both pairs, "MISMATCH: J1.bergingsaandeel = [5;100], expected 100" beside the initialisation and
     * "[100;5]" beside the rule, a fault of that runner recorded with
     * {@code 20261009-clingo-gemaakt-lid-met-twee-waarden}. Both runners pass the two allocations (2 expectations), whose
     * values agree. Red before the change: all three on the refusal of the allocation as such.
     */
    static final String H_REGEL = rekenregel("bergingsaandeel van een berger", "BEREKENING", "Berger",
            "bergingsaandeel", getal("5"), "null");
    static final String H_INITIALISATIE = rekenregel("bergingsaandeel van een berger", "INITIALISATIE", "Berger",
            "bergingsaandeel", getal("5"), "null");

    static JsonNode hZaak() throws Exception {
        return TmsDocumenten.zaak(List.of(instantie("B1", "Berging", Map.of("opbrengst", "100"), List.of("J1")),
                instantie("J1", "Berger", Map.of("gewicht", "1"), List.of())),
                List.of(waarde("J1", "bergingsaandeel", "100"), consistent()));
    }

    @Test void h_een_tweede_schrijver_van_een_doel_wordt_luid_geweigerd() throws Exception {
        JsonNode zaak = hZaak();
        Map<String, String> uitkomsten = new LinkedHashMap<>();
        uitkomsten.put("regel", speel(document(List.of(H_REGEL), List.of(D_NAAR_RATO), List.of()), zaak));
        uitkomsten.put("initialisatie", speel(document(List.of(H_INITIALISATIE), List.of(D_NAAR_RATO), List.of()), zaak));
        uitkomsten.put("twee verdelingen", speel(document(List.of(), List.of(D_NAAR_RATO, D_GELIJK), List.of()), zaak));
        String naast = "Excluded: transpiler SKIP: verdeling: verdeel de opbrengst naar rato — een verdeling naast de"
                + " regel \"bergingsaandeel van een berger\" op het attribuut 'bergingsaandeel' van Berger: een verdeling"
                + " naast een rekenende regel of een initialisatie op één doel; de truth-maintenance-tak draagt deze vorm"
                + " nog niet";
        Map<String, String> verwacht = new LinkedHashMap<>();
        verwacht.put("regel", naast);
        verwacht.put("initialisatie", naast);
        verwacht.put("twee verdelingen", "Excluded: transpiler SKIP: verdeling: verdeel de opbrengst naar rato — twee"
                + " verdelingen schrijven het attribuut 'bergingsaandeel' van Berger (ook \"verdeel de opbrengst gelijk\"):"
                + " een doel heeft hoogstens één verdeling (WF-026)");
        assertEquals(verwacht, uitkomsten);
    }

    // ---------------- D, E and F in orders of the generated rules ----------------

    /** Parts D, E and F hold one allocation each, so the order of the document varies nothing; here every case of them
     *  runs through a seeded sample of orders of the generated rules, with the outcome the replay gives. */
    @Test void d_e_f_in_een_steekproef_van_de_gegenereerde_regels() throws Exception {
        Map<String, String> verwacht = new LinkedHashMap<>(), uitkomsten = new LinkedHashMap<>();
        Map<String, Object[]> delen = new LinkedHashMap<>();
        for (Map.Entry<String, Object[]> r : randgevallen().entrySet())
            delen.put("d " + r.getKey(), new Object[]{document(List.of(), List.of((String) r.getValue()[0]), List.of()),
                    r.getValue()[1]});
        Path e = document(List.of(), List.of(E_VERDELING), List.of());
        for (Map.Entry<String, JsonNode> z : eZaken().entrySet())
            delen.put("e " + z.getKey(), new Object[]{e, z.getValue()});
        delen.put("f", new Object[]{document(List.of(), List.of(F_GELIJK), List.of(HULPBERGER)), fZaak()});
        for (Map.Entry<String, Object[]> d : delen.entrySet()) {
            Path ir = (Path) d.getValue()[0];
            JsonNode zaak = (JsonNode) d.getValue()[1];
            String replay = speel(ir, zaak);
            DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
            for (List<Rule> volgorde : TmsDocumenten.steekproef(b.execModelTranspiler().model().getRules(), 20261009L, 20)) {
                String sleutel = d.getKey() + " " + volgorde.stream().map(Rule::getName).toList();
                uitkomsten.put(sleutel, speelInVolgorde(b, volgorde, zaak));
                verwacht.put(sleutel, replay);
            }
        }
        assertEquals(verwacht, uitkomsten);
    }

    /**
     * Case {@code zaak} of this class's model fired through the rules of {@code b} added in {@code volgorde}, with the
     * backend's own driver: the beans as the case input makes them (whole numbers, the bergers of a Berging), the
     * evaluation date as the global the case runner sets, then the expectations against the beans written back and the
     * §9.5 facts of the session, the rule compared as the case runner compares it. Returns "gespeeld", the mismatches,
     * or the fault of the fixpoint as "SimpleName: message", as {@link TmsFoutWitnessTest#speel} reports them.
     */
    private static String speelInVolgorde(DroolsBackend.Build b, List<Rule> volgorde, JsonNode zaak) throws Exception {
        Map<String, Object> bonen = new LinkedHashMap<>();
        Map<Object, String> labels = new IdentityHashMap<>();
        for (JsonNode i : zaak.get("input").get("instances")) {
            Map<String, String> getallen = new LinkedHashMap<>();
            i.get("attributes").fields().forEachRemaining(a -> getallen.put(a.getKey(), a.getValue().get("number").asText()));
            Object boon = TmsDocumenten.boon(b.beanLoader(), i.get("objectType").asText(), getallen, List.of());
            bonen.put(i.get("label").asText(), boon);
            labels.put(boon, i.get("label").asText());
        }
        for (JsonNode i : zaak.get("input").get("instances"))
            for (JsonNode berger : i.get("roles").path("bergers")) koppel(bonen.get(berger.asText()), bonen.get(i.get("label").asText()));
        String datum = zaak.get("asOfDate").isNull() ? null : zaak.get("asOfDate").asText();
        List<String> kanaal = new ArrayList<>();
        try {
            TmsCreatieWitnessTest.vuur(b, volgorde, new ArrayList<>(bonen.values()), sessie -> {
                if (datum != null) sessie.setGlobal("rekendatum", java.time.LocalDate.parse(datum));
            }, sessie -> kanaal.addAll(TmsDocumenten.inconsistenties(sessie, labels)));
        } catch (RuntimeException e) {
            return e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        List<String> fouten = new ArrayList<>();
        for (JsonNode v : zaak.get("expected")) {
            String soort = v.get("kind").asText();
            switch (soort) {
                case "attributeValue", "attributeEmpty" -> {
                    Object w = TmsDocumenten.lees(bonen.get(v.get("label").asText()), v.get("attribute").asText());
                    boolean goed = soort.equals("attributeEmpty") ? w == null
                            : isGetal(w, v.get("value").get("number").asText());
                    if (!goed) fouten.add(v.get("label").asText() + "." + v.get("attribute").asText() + " " + w);
                }
                case "inconsistent" -> {
                    String regel = v.get("rule").asText(), label = v.get("label").asText();
                    if (kanaal.stream().noneMatch(k -> k.endsWith(" @ " + label)
                            && CaseRunner.zelfdeRegel(regel, k.substring(0, k.length() - (" @ " + label).length()))))
                        fouten.add("geen deponering " + regel + " @ " + label + " in " + kanaal);
                }
                case "consistent" -> {
                    if (!kanaal.isEmpty()) fouten.add("deponeringen " + kanaal);
                }
                default -> throw new AssertionError("verwachting " + soort);
            }
        }
        return fouten.isEmpty() ? "gespeeld" : String.join("; ", fouten);
    }

    // ---------------- helpers ----------------

    /** Links the Berger {@code berger} to the Berging {@code berging}, as the case input stores the bergingsdeelname. */
    private static void koppel(Object berger, Object berging) throws Exception {
        for (java.lang.reflect.Method m : berger.getClass().getMethods())
            if (m.getName().equals(Namen.setter("berging")) && m.getParameterCount() == 1) {
                m.invoke(berger, berging);
                return;
            }
        throw new AssertionError("geen setter voor berging op " + berger.getClass());
    }

    private static Map<Object, String> labels(List<Object> bonen, List<String> namen) {
        Map<Object, String> uit = new IdentityHashMap<>();
        for (int i = 0; i < bonen.size(); i++) uit.put(bonen.get(i), namen.get(i));
        return uit;
    }

    private static Object onderwerp(Object inconsistentie) {
        try {
            return inconsistentie.getClass().getMethod("getSubject").invoke(inconsistentie);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean isGetal(Object x, String lexeem) {
        return x instanceof BigDecimal d && d.compareTo(new BigDecimal(lexeem)) == 0;
    }

    private static Object nul(Object x) {
        return x == null ? "leeg" : x;
    }

    /** {@code waarden} printed with every number in its plain form, so that 600 and 600.00 print alike. */
    private static String getallen(Map<String, Object> waarden) {
        Map<String, Object> uit = new LinkedHashMap<>();
        waarden.forEach((k, v) -> uit.put(k, v instanceof BigDecimal d ? d.stripTrailingZeros().toPlainString() : v));
        return uit.toString();
    }

    /**
     * Writes every document of this class with its cases into the directory {@code args[0]}, one directory per part,
     * so that the reference interpreter and the ASP backend run the same documents:
     * {@code runner.exe <dir>/<part>/ir.json <dir>/<part>/case-<name>.json}. Run it from {@code backends/drools} after
     * {@code mvn -o -q test-compile}, with the test classpath as {@code TmsMeting}'s javadoc builds it.
     */
    public static void main(String[] args) throws Exception {
        Path uit = Path.of(args[0]);
        schrijf(uit.resolve("a-later-afgeleid"), ir(A_REGELS, A_VERDELINGEN, List.of()), Map.of("a", aZaak()));
        schrijf(uit.resolve("b-dubbelbezet"), ir(List.of(), B_VERDELINGEN, List.of()), Map.of("b", bZaak()));
        schrijf(uit.resolve("c-degradaties"), ir(C_REGELS, List.of(C_NAAR_RATO), List.of()), Map.of("c", cZaak()));
        for (Map.Entry<String, Object[]> r : randgevallen().entrySet())
            schrijf(uit.resolve("d-" + r.getKey()), ir(List.of(), List.of((String) r.getValue()[0]), List.of()),
                    Map.of(r.getKey(), (JsonNode) r.getValue()[1]));
        schrijf(uit.resolve("e-geldigheid"), ir(List.of(), List.of(E_VERDELING), List.of()), eZaken());
        schrijf(uit.resolve("f-creatie"), ir(List.of(), List.of(F_GELIJK), List.of(HULPBERGER)), Map.of("f", fZaak()));
        schrijf(uit.resolve("g-pot-later"), ir(List.of(POT), List.of(F_GELIJK), List.of()), Map.of("g", gZaak()));
        schrijf(uit.resolve("h-regel"), ir(List.of(H_REGEL), List.of(D_NAAR_RATO), List.of()), Map.of("h", hZaak()));
        schrijf(uit.resolve("h-initialisatie"), ir(List.of(H_INITIALISATIE), List.of(D_NAAR_RATO), List.of()),
                Map.of("h", hZaak()));
        schrijf(uit.resolve("h-twee-verdelingen"), ir(List.of(), List.of(D_NAAR_RATO, D_GELIJK), List.of()),
                Map.of("h", hZaak()));
    }

    private static void schrijf(Path map, String ir, Map<String, JsonNode> zaken) throws Exception {
        Files.createDirectories(map);
        Files.writeString(map.resolve("ir.json"), TmsDocumenten.JSON.writerWithDefaultPrettyPrinter()
                .writeValueAsString(TmsDocumenten.JSON.readTree(ir)));
        for (Map.Entry<String, JsonNode> z : zaken.entrySet())
            Files.writeString(map.resolve("case-" + z.getKey() + ".json"),
                    TmsDocumenten.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(z.getValue()));
    }
}
