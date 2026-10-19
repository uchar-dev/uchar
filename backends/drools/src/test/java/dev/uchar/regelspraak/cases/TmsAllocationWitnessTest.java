package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.core.Namen;
import dev.uchar.regelspraak.transpiler.tms.State;
import dev.uchar.regelspraak.transpiler.tms.Value;

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

import static dev.uchar.regelspraak.cases.TmsDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribute;
import static dev.uchar.regelspraak.cases.TmsDocuments.binary;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.factTypeOneToMany;
import static dev.uchar.regelspraak.cases.TmsDocuments.validity;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.numberType;
import static dev.uchar.regelspraak.cases.TmsDocuments.inconsistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.roleBinding;
import static dev.uchar.regelspraak.cases.TmsDocuments.comparison;
import static dev.uchar.regelspraak.cases.TmsDocuments.value;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Allocations on the exec-model branch (RUNTIME-SEMANTICS ch. 4): the distribution is a computation over the
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
 * committed): the {@link dev.uchar.regelspraak.transpiler.tms.Recipient} stated instead of logical fails A's and C's
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
 * {@link dev.uchar.regelspraak.transpiler.tms.Fault} on the source): D, "lege-pot-gelijk=Rekenfout: fout in de regel
 * "verdeel de opbrengst gelijk": empty source amount in equal-shares allocation (v0 mirror hole, ch. 4.4),
 * lege-pot-naar-rato=Rekenfout: fout in de regel "verdeel de opbrengst naar rato": empty source amount in proportional
 * allocation (v0 mirror hole, ch. 4.4)", and G's sample, "ingevoegd op de lege pot in volgorde […] ==> expected:
 * &lt;[]&gt; but was: &lt;[Fout(Berging@…, verdeel de opbrengst gelijk, fout in de regel "verdeel de opbrengst gelijk":
 * empty source amount in equal-shares allocation (v0 mirror hole, ch. 4.4))]&gt;".
 *
 * <p><b>The samples on the text.</b> A's sample fires on both branches since round 5b; those of B, C, G and of D, E and
 * F since fix round 5b in step 6a (lens on round 5b): the empty weights, a deposit that lapses, a pot that is empty
 * first and the edge cases can depend on the order in which the rules are loaded while the replay in the order of the
 * document passes. On the text a sample takes at most {@link TmsDocuments#TEXT_SHUFFLES} shuffles of the seed
 * beside the order of the document and its reverse ({@link TmsDocuments#shuffles}), since every order there
 * compiles a DRL text of its own.
 */
class TmsAllocationWitnessTest {

    private static final String ONE_DECIMAL = numberType(false, 1, null);

    static final String MODEL = TmsDocuments.model(List.of(
            TmsDocuments.objectType("Berging", List.of(attribute("opbrengst", WHOLE), attribute("onverdeelde rest", WHOLE),
                    attribute("rangrest", WHOLE), attribute("vrijgavescore", WHOLE), attribute("bruto", WHOLE)), List.of()),
            TmsDocuments.objectType("Berger", List.of(attribute("basisgewicht", WHOLE), attribute("extra", WHOLE),
                    attribute("toeslag", WHOLE), attribute("gewicht", WHOLE), attribute("jaar", WHOLE),
                    attribute("rang", WHOLE), attribute("basismaximum", WHOLE), attribute("maximum", WHOLE),
                    attribute("peiljaar", ONE_DECIMAL), attribute("bergingsaandeel", WHOLE),
                    attribute("rangaandeel", WHOLE), attribute("toezicht", WHOLE)), List.of())),
            List.of(factTypeOneToMany("bergingsdeelname", "berging", "Berging", "berger", "Berger", "bergers")));

    // ---------------- the wire of an allocation ----------------

    /** An Allocation of the given versions ({@link #version}). */
    static String allocation(String name, List<String> versions) {
        return "{\"node\":\"Allocation\",\"name\":\"" + name + "\",\"versions\":[" + String.join(",", versions) + "]}";
    }

    /** One version of an allocation of the opbrengst of a Berging over its bergers (the role berger of the
     *  bergingsdeelname), in the field order of IR-SPEC ch. 3.2; every optional field null for none, the validity null
     *  for always valid. */
    static String version(String method, String target, String weight, String maximum, Integer decimals, String rest,
                         String key, boolean descending, String condition, String validity) {
        StringBuilder b = new StringBuilder("{\"sourceType\":\"Berging\",\"sourceAttribute\":\"opbrengst\","
                + "\"targetType\":\"Berger\",\"targetAttribute\":\"" + target + "\",\"method\":\"" + method + "\"");
        if (weight != null) b.append(",\"ratioAttribute\":\"").append(weight).append('"');
        if (maximum != null) b.append(",\"maximumAttribute\":\"").append(maximum).append('"');
        if (decimals != null) b.append(",\"roundingDecimals\":").append(decimals);
        if (rest != null) b.append(",\"remainderAttribute\":\"").append(rest).append('"');
        if (key != null) b.append(",\"orderingAttribute\":\"").append(key).append('"');
        b.append(",\"descending\":").append(descending);
        if (condition != null) b.append(",\"condition\":").append(condition);
        b.append(",\"validity\":").append(validity == null ? "{\"node\":\"Validity\"}" : validity);
        b.append(",\"connectingRole\":").append(roleBinding("bergingsdeelname", "berger")).append('}');
        return b.toString();
    }

    /** The document of {@link #MODEL} with {@code rules}, {@code allocations} and {@code objectCreations}, each in its
     *  order, as JSON. */
    static String ir(List<String> rules, List<String> allocations, List<String> objectCreations) {
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + MODEL + ",\"rules\":{\"rules\":[" + String.join(",", rules)
                + "],\"dayKindDefinitions\":[],\"objectCreations\":[" + String.join(",", objectCreations)
                + "],\"allocations\":[" + String.join(",", allocations) + "],\"consistencies\":[],\"uniquenesses\":[],"
                + "\"factCreations\":[],\"startPointDeterminations\":[],\"ruleGroups\":[],\"prioritizedDefaults\":[]},"
                + "\"diagnostics\":[]}";
    }

    static Path document(List<String> rules, List<String> allocations, List<String> objectCreations) throws Exception {
        Path tmp = Files.createTempFile("tms-verdeling", ".json");
        tmp.toFile().deleteOnExit();
        return Files.writeString(tmp, ir(rules, allocations, objectCreations));
    }

    /** An instance with whole-number attributes (a lexeme "leeg" writes none) and the bergers of a Berging. */
    static String instance(String label, String type, Map<String, String> numbers, List<String> bergers) {
        Map<String, String> values = new LinkedHashMap<>(numbers);
        List<String> a = new ArrayList<>(), b = new ArrayList<>();
        values.forEach((name, lexeme) -> a.add("\"" + name + "\":{\"number\":\"" + lexeme + "\"}"));
        for (String berger : bergers) b.add("\"" + berger + "\"");
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + String.join(",", a)
                + "},\"roles\":{" + (bergers.isEmpty() ? "" : "\"bergers\":[" + String.join(",", b) + "]")
                + "},\"characteristics\":[]}";
    }

    /** "zijn {@code attribute}" on a Berger. */
    static String berger(String attribute) {
        return ownRead(attribute, "Berger");
    }

    /** Every order of the rules and, independently, of the allocations of a part, as documents, with the names of the
     *  forms in that order. */
    static Map<String, Path> orders(List<String> rules, List<String> allocations, List<String> objectCreations)
            throws Exception {
        Map<String, Path> out = new LinkedHashMap<>();
        for (List<String> r : TmsDocuments.permutations(rules))
            for (List<String> v : TmsDocuments.permutations(allocations))
                out.put(TmsDocuments.ruleNames(r) + " " + TmsDocuments.ruleNames(v), document(r, v, objectCreations));
        return out;
    }

    /** The outcome of the case on the exec-model branch and on {@code DRL_TEXT}, which must be one. */
    private static String play(Path ir, JsonNode caseDocument) throws Exception {
        return TmsFaultWitnessTest.playBoth(ir, caseDocument);
    }

    // ---------------- A: a weight and a key derived later ----------------

    static final String TOESLAG = computingRule("toeslag van een berger", "BEREKENING", "Berger", "toeslag", berger("extra"),
            "null");
    static final String WEIGHT = computingRule("gewicht van een berger", "BEREKENING", "Berger", "gewicht",
            binary("plus", berger("basisgewicht"), berger("toeslag")), "null");
    static final String RANK = computingRule("rang van een berger", "BEREKENING", "Berger", "rang",
            binary("minus", berger("jaar"), berger("toeslag")), "null");
    static final String TOEZICHT = computingRule("toezicht van een berger", "BEREKENING", "Berger", "toezicht",
            binary("plus", berger("bergingsaandeel"), number("1")), "null");
    static final String PROPORTIONAL = allocation("verdeel de opbrengst naar rato", List.of(version("proportional",
            "bergingsaandeel", "gewicht", null, 0, "onverdeelde rest", null, false, null, null)));
    static final String BY_RANK = allocation("verdeel de opbrengst op rang", List.of(version("ordered", "rangaandeel",
            null, null, 0, "rangrest", "rang", false, null, null)));
    static final List<String> A_RULES = List.of(TOESLAG, WEIGHT, RANK, TOEZICHT);
    static final List<String> A_ALLOCATIONS = List.of(PROPORTIONAL, BY_RANK);

    static JsonNode aCase() throws Exception {
        return TmsDocuments.caseDocument(List.of(
                instance("B1", "Berging", Map.of("opbrengst", "900"), List.of("J1", "J2")),
                instance("J1", "Berger", Map.of("basisgewicht", "1", "extra", "5", "jaar", "2020"), List.of()),
                instance("J2", "Berger", Map.of("basisgewicht", "3", "extra", "0", "jaar", "2018"), List.of())),
                List.of(value("J1", "toeslag", "5"), value("J2", "toeslag", "0"), value("J1", "gewicht", "6"),
                        value("J2", "gewicht", "3"), value("J1", "rang", "2015"), value("J2", "rang", "2018"),
                        value("J1", "bergingsaandeel", "600"), value("J2", "bergingsaandeel", "300"),
                        value("B1", "onverdeelde rest", "0"), value("J1", "rangaandeel", "900"),
                        value("J2", "rangaandeel", "0"), value("B1", "rangrest", "0"), value("J1", "toezicht", "601"),
                        value("J2", "toezicht", "301"), consistent()));
    }

    @Test void a_every_order_of_the_rules_and_the_allocations() throws Exception {
        List<String> outcomes = new ArrayList<>(), expected = new ArrayList<>();
        for (Map.Entry<String, Path> d : orders(A_RULES, A_ALLOCATIONS, List.of()).entrySet()) {
            outcomes.add(d.getKey() + " " + play(d.getValue(), aCase()));
            expected.add(d.getKey() + " gespeeld");
        }
        assertEquals(expected, outcomes);
        assertEquals(48, outcomes.size());
    }

    /**
     * A seeded sample of orders of the generated rules. In every order the beans end at the fixpoint and the session
     * holds one {@link Value} for each share; and in at least one order the allocation inserts a share of the state
     * before the toeslag is derived (J1 225 naar rato, J2 900 op rang), which truth maintenance withdraws.
     *
     * <p>The sample fires on the exec-model branch and on {@code DRL_TEXT}, its analysis rendered as DRL text, with the
     * rules of the text in the same orders ({@code TextOrder}), and each branch meets the same demands (card
     * drl-teksttak-op-truth-maintenance, round 5b, on the lens of round 5). <b>Red</b> on {@code abe891c1}, before the
     * rendering carried the recipients of an allocation: "Excluded drl-tms: de DRL-weergave draagt Ontvanger nog
     * niet".
     */
    @Test void a_sample_of_the_generated_rules() throws Exception {
        Path ir = document(A_RULES, A_ALLOCATIONS, List.of());
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            DroolsBackend.Build b = DroolsBackend.build(ir, branch);
            int proportionalIntermediate = 0, byRankIntermediate = 0;
            for (List<String> names : TmsDocuments.sample(TmsDocuments.generated(b), 20261009L, 100)) {
                String where = names + " op " + branch;
                Object b1 = TmsDocuments.beanObject(b.beanLoader(), "Berging", Map.of("opbrengst", "900"), List.of());
                Object j1 = TmsDocuments.beanObject(b.beanLoader(), "Berger", Map.of("basisgewicht", "1", "extra", "5",
                        "jaar", "2020"), List.of());
                Object j2 = TmsDocuments.beanObject(b.beanLoader(), "Berger", Map.of("basisgewicht", "3", "extra", "0",
                        "jaar", "2018"), List.of());
                join(j1, b1);
                join(j2, b1);
                List<Object> proportional = new ArrayList<>(), byRank = new ArrayList<>();
                int[] values = new int[2];
                TmsDocuments.fire(b, TmsDocuments.kieBaseOf(b, names), names, List.of(b1, j1, j2),
                        session -> session.addEventListener(new DefaultRuleRuntimeEventListener() {
                            @Override public void objectInserted(ObjectInsertedEvent e) {
                                if (!(e.getObject() instanceof Value w)) return;
                                if (w.subject() == j1 && w.attribute().name().equals("bergingsaandeel"))
                                    proportional.add(w.value());
                                if (w.subject() == j2 && w.attribute().name().equals("rangaandeel"))
                                    byRank.add(w.value());
                            }
                        }), session -> {
                            values[0] = session.getObjects(o -> o instanceof Value w && w.subject() == j1
                                    && w.attribute().name().equals("bergingsaandeel")).size();
                            values[1] = session.getObjects(o -> o instanceof Value w && w.subject() == j2
                                    && w.attribute().name().equals("rangaandeel")).size();
                        });
                assertEquals(List.of(1, 1), List.of(values[0], values[1]), () -> "Waarde-feiten in volgorde " + where);
                Map<String, Object> end = new LinkedHashMap<>();
                end.put("J1 bergingsaandeel", TmsDocuments.read(j1, "bergingsaandeel"));
                end.put("J2 bergingsaandeel", TmsDocuments.read(j2, "bergingsaandeel"));
                end.put("B1 onverdeelde rest", TmsDocuments.read(b1, "onverdeelde rest"));
                end.put("J1 rangaandeel", TmsDocuments.read(j1, "rangaandeel"));
                end.put("J2 rangaandeel", TmsDocuments.read(j2, "rangaandeel"));
                end.put("B1 rangrest", TmsDocuments.read(b1, "rangrest"));
                end.put("J1 toezicht", TmsDocuments.read(j1, "toezicht"));
                assertEquals("{J1 bergingsaandeel=600, J2 bergingsaandeel=300, B1 onverdeelde rest=0,"
                        + " J1 rangaandeel=900, J2 rangaandeel=0, B1 rangrest=0, J1 toezicht=601}", numbers(end),
                        () -> "volgorde " + where);
                if (proportional.stream().anyMatch(x -> isNumber(x, "225"))) proportionalIntermediate++;
                if (byRank.stream().anyMatch(x -> isNumber(x, "900"))) byRankIntermediate++;
            }
            assertTrue(proportionalIntermediate > 0, branch + ": geen volgorde liet de verdeling naar rato vuren op het gewicht"
                    + " van vóór de toeslag");
            assertTrue(byRankIntermediate > 0, branch + ": geen volgorde liet de verdeling op rang vuren op de rang van vóór"
                    + " de toeslag");
        }
    }

    // ---------------- B: two allocations over one set of recipients ----------------

    static final String B_PROPORTIONAL = allocation("verdeel de opbrengst naar rato", List.of(version("proportional",
            "bergingsaandeel", "gewicht", "maximum", 0, "onverdeelde rest", null, false, null, null)));
    static final String B_BY_YEAR = allocation("verdeel de opbrengst op jaar", List.of(version("ordered", "rangaandeel",
            "gewicht", "maximum", 0, "rangrest", "jaar", false, null, null)));
    static final List<String> B_ALLOCATIONS = List.of(B_PROPORTIONAL, B_BY_YEAR);

    static JsonNode bCase() throws Exception {
        return TmsDocuments.caseDocument(List.of(
                instance("B1", "Berging", Map.of("opbrengst", "1000"), List.of("D1", "D2", "D3")),
                instance("D1", "Berger", Map.of("gewicht", "3", "jaar", "2019", "maximum", "600"), List.of()),
                instance("D2", "Berger", Map.of("jaar", "2020", "maximum", "600"), List.of()),
                instance("D3", "Berger", Map.of("gewicht", "1", "jaar", "2020", "maximum", "600"), List.of())),
                List.of(value("D1", "bergingsaandeel", "600"), value("D2", "bergingsaandeel", null),
                        value("D3", "bergingsaandeel", "250"), value("B1", "onverdeelde rest", "150"),
                        value("D1", "rangaandeel", "600"), value("D2", "rangaandeel", "0"),
                        value("D3", "rangaandeel", "400"), value("B1", "rangrest", "0"),
                        inconsistent("verdeel de opbrengst naar rato", "D2"),
                        inconsistent("verdeel de opbrengst op jaar", "D2")));
    }

    @Test void b_two_allocations_over_the_same_recipients_in_every_order() throws Exception {
        List<String> outcomes = new ArrayList<>(), expected = new ArrayList<>();
        for (Map.Entry<String, Path> d : orders(List.of(), B_ALLOCATIONS, List.of()).entrySet()) {
            outcomes.add(d.getKey() + " " + play(d.getValue(), bCase()));
            expected.add(d.getKey() + " gespeeld");
        }
        assertEquals(expected, outcomes);
    }

    /** The deposits of part B are those two and no more, in a seeded sample of orders of the generated rules. */
    @Test void b_only_the_empty_gewichten_deposit() throws Exception {
        Path ir = document(List.of(), B_ALLOCATIONS, List.of());
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            DroolsBackend.Build b = DroolsBackend.build(ir, branch);
            for (List<String> names : TmsDocuments.sample(TmsDocuments.generated(b), 20261009L,
                    TmsDocuments.shuffles(branch, 20))) {
                Object b1 = TmsDocuments.beanObject(b.beanLoader(), "Berging", Map.of("opbrengst", "1000"), List.of());
                Object d1 = TmsDocuments.beanObject(b.beanLoader(), "Berger", Map.of("gewicht", "3", "jaar", "2019",
                        "maximum", "600"), List.of());
                Object d2 = TmsDocuments.beanObject(b.beanLoader(), "Berger", Map.of("jaar", "2020", "maximum", "600"),
                        List.of());
                Object d3 = TmsDocuments.beanObject(b.beanLoader(), "Berger", Map.of("gewicht", "1", "jaar", "2020",
                        "maximum", "600"), List.of());
                for (Object d : List.of(d1, d2, d3)) join(d, b1);
                Map<Object, String> labels = labels(List.of(b1, d1, d2, d3), List.of("B1", "D1", "D2", "D3"));
                List<String> channel = new ArrayList<>();
                TmsDocuments.fire(b, TmsDocuments.kieBaseOf(b, names), names, List.of(b1, d1, d2, d3), session -> {},
                        session -> channel.addAll(TmsDocuments.inconsistencies(session, labels)));
                assertEquals(List.of("verdeel de opbrengst naar rato @ D2", "verdeel de opbrengst op jaar @ D2"), channel,
                        () -> "op " + branch + " in volgorde " + names);
            }
        }
    }

    // ---------------- C: the degradations at the fixpoint ----------------

    static final String C_WEIGHT = computingRule("gewicht van een berger", "BEREKENING", "Berger", "gewicht",
            berger("basisgewicht"), "null");
    static final String C_MAXIMUM = computingRule("maximum van een berger", "BEREKENING", "Berger", "maximum",
            berger("basismaximum"), "null");
    static final String C_PROPORTIONAL = allocation("verdeel de opbrengst naar rato", List.of(version("proportional",
            "bergingsaandeel", "gewicht", "maximum", null, "onverdeelde rest", null, false, null, null)));
    static final List<String> C_RULES = List.of(C_WEIGHT, C_MAXIMUM);

    static JsonNode cCase() throws Exception {
        return TmsDocuments.caseDocument(List.of(
                instance("B1", "Berging", Map.of("opbrengst", "1200"), List.of("J1", "J2", "J3", "J4")),
                instance("J1", "Berger", Map.of("basisgewicht", "3"), List.of()),
                instance("J2", "Berger", Map.of("basisgewicht", "2", "basismaximum", "-5"), List.of()),
                instance("J3", "Berger", Map.of("basisgewicht", "1", "basismaximum", "300"), List.of()),
                instance("J4", "Berger", Map.of("basismaximum", "300"), List.of())),
                List.of(value("J1", "bergingsaandeel", "600"), value("J2", "bergingsaandeel", "400"),
                        value("J3", "bergingsaandeel", "200"), value("J4", "bergingsaandeel", null),
                        value("B1", "onverdeelde rest", "0"), inconsistent("verdeel de opbrengst naar rato", "J1"),
                        inconsistent("verdeel de opbrengst naar rato", "J2"),
                        inconsistent("verdeel de opbrengst naar rato", "J4")));
    }

    @Test void c_degradations_in_every_order() throws Exception {
        List<String> outcomes = new ArrayList<>(), expected = new ArrayList<>();
        for (Map.Entry<String, Path> d : orders(C_RULES, List.of(C_PROPORTIONAL), List.of()).entrySet()) {
            outcomes.add(d.getKey() + " " + play(d.getValue(), cCase()));
            expected.add(d.getKey() + " gespeeld");
        }
        assertEquals(expected, outcomes);
    }

    /**
     * A seeded sample of orders of the generated rules: at the fixpoint the channel holds the deposits on J1, J2 and J4
     * and none on J3, whose maximum is 300 there; in at least one order the allocation deposited on J3 on the way,
     * before the maximum was derived, and that deposit went again.
     */
    @Test void c_a_deposit_of_an_intermediate_state_lapses() throws Exception {
        Path ir = document(C_RULES, List.of(C_PROPORTIONAL), List.of());
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            DroolsBackend.Build b = DroolsBackend.build(ir, branch);
            int intermediate = 0;
            for (List<String> names : TmsDocuments.sample(TmsDocuments.generated(b), 20261009L,
                    TmsDocuments.shuffles(branch, 100))) {
                String where = "op " + branch + " in volgorde " + names;
                Object b1 = TmsDocuments.beanObject(b.beanLoader(), "Berging", Map.of("opbrengst", "1200"), List.of());
                Object j1 = TmsDocuments.beanObject(b.beanLoader(), "Berger", Map.of("basisgewicht", "3"), List.of());
                Object j2 = TmsDocuments.beanObject(b.beanLoader(), "Berger", Map.of("basisgewicht", "2",
                        "basismaximum", "-5"), List.of());
                Object j3 = TmsDocuments.beanObject(b.beanLoader(), "Berger", Map.of("basisgewicht", "1",
                        "basismaximum", "300"), List.of());
                Object j4 = TmsDocuments.beanObject(b.beanLoader(), "Berger", Map.of("basismaximum", "300"), List.of());
                for (Object j : List.of(j1, j2, j3, j4)) join(j, b1);
                Map<Object, String> labels = labels(List.of(b1, j1, j2, j3, j4), List.of("B1", "J1", "J2", "J3", "J4"));
                List<String> channel = new ArrayList<>();
                int[] opJ3 = new int[1];
                TmsDocuments.fire(b, TmsDocuments.kieBaseOf(b, names), names, List.of(b1, j1, j2, j3, j4),
                        session -> session.addEventListener(new DefaultRuleRuntimeEventListener() {
                            @Override public void objectInserted(ObjectInsertedEvent e) {
                                if (e.getObject().getClass().getSimpleName().equals("Inconsistentie")
                                        && subject(e.getObject()) == j3) opJ3[0]++;
                            }
                        }), session -> channel.addAll(TmsDocuments.inconsistencies(session, labels)));
                assertEquals(List.of("verdeel de opbrengst naar rato @ J1", "verdeel de opbrengst naar rato @ J2",
                        "verdeel de opbrengst naar rato @ J4"), channel, () -> where);
                Map<String, Object> end = new LinkedHashMap<>();
                end.put("J1", zero(TmsDocuments.read(j1, "bergingsaandeel")));
                end.put("J2", zero(TmsDocuments.read(j2, "bergingsaandeel")));
                end.put("J3", zero(TmsDocuments.read(j3, "bergingsaandeel")));
                end.put("J4", zero(TmsDocuments.read(j4, "bergingsaandeel")));
                end.put("B1", zero(TmsDocuments.read(b1, "onverdeelde rest")));
                assertEquals("{J1=600, J2=400, J3=200, J4=leeg, B1=0}", numbers(end), () -> where);
                if (opJ3[0] > 0) intermediate++;
            }
            assertTrue(intermediate > 0, branch + ": geen volgorde liet de verdeling op J3 deponeren voordat zijn maximum"
                    + " afgeleid was");
        }
    }

    // ---------------- D: the edge rows of ch. 4.4 ----------------

    static final String D_PROPORTIONAL = allocation("verdeel de opbrengst naar rato", List.of(version("proportional",
            "bergingsaandeel", "gewicht", null, 0, "onverdeelde rest", null, false, null, null)));
    static final String D_EQUAL = allocation("verdeel de opbrengst gelijk", List.of(version("equalShares",
            "bergingsaandeel", null, null, 0, "onverdeelde rest", null, false, null, null)));
    static final String D_BY_YEAR = allocation("verdeel de opbrengst op jaar", List.of(version("ordered", "rangaandeel",
            "gewicht", null, 0, "rangrest", "jaar", false, null, null)));
    static final String D_OP_PEILJAAR = allocation("verdeel de opbrengst op peiljaar", List.of(version("ordered",
            "rangaandeel", null, null, 0, "rangrest", "peiljaar", false, null, null)));

    /** The edge cases of part D: the allocation, the case input and its expectations, by name. */
    static Map<String, Object[]> edgeCases() throws Exception {
        Map<String, Object[]> out = new LinkedHashMap<>();
        out.put("gewichten-nul", new Object[]{D_PROPORTIONAL, TmsDocuments.caseDocument(List.of(
                instance("B1", "Berging", Map.of("opbrengst", "100"), List.of("J1", "J2", "J3")),
                instance("J1", "Berger", Map.of("gewicht", "0", "bergingsaandeel", "7"), List.of()),
                instance("J2", "Berger", Map.of("gewicht", "0"), List.of()),
                instance("J3", "Berger", Map.of(), List.of())),
                List.of(value("J1", "bergingsaandeel", "7"), value("J2", "bergingsaandeel", null),
                        value("J3", "bergingsaandeel", null), value("B1", "onverdeelde rest", null),
                        inconsistent("verdeel de opbrengst naar rato", "J3")))});
        out.put("lege-pot-gelijk", new Object[]{D_EQUAL, TmsDocuments.caseDocument(List.of(
                instance("B1", "Berging", Map.of(), List.of("J1", "J2")),
                instance("J1", "Berger", Map.of("bergingsaandeel", "7"), List.of()),
                instance("J2", "Berger", Map.of(), List.of())),
                List.of(value("J1", "bergingsaandeel", "7"), value("J2", "bergingsaandeel", null),
                        value("B1", "onverdeelde rest", null), consistent()))});
        out.put("lege-pot-naar-rato", new Object[]{D_PROPORTIONAL, TmsDocuments.caseDocument(List.of(
                instance("B1", "Berging", Map.of(), List.of("J1", "J2")),
                instance("J1", "Berger", Map.of("gewicht", "2", "bergingsaandeel", "7"), List.of()),
                instance("J2", "Berger", Map.of(), List.of())),
                List.of(value("J1", "bergingsaandeel", "7"), value("J2", "bergingsaandeel", null),
                        value("B1", "onverdeelde rest", null), consistent()))});
        out.put("lege-pot-op-volgorde", new Object[]{D_BY_YEAR, TmsDocuments.caseDocument(List.of(
                instance("B1", "Berging", Map.of(), List.of("J1", "J2")),
                instance("J1", "Berger", Map.of("jaar", "2020"), List.of()),
                instance("J2", "Berger", Map.of("jaar", "2020", "gewicht", "1"), List.of())),
                List.of(value("J1", "rangaandeel", null), value("J2", "rangaandeel", null),
                        value("B1", "rangrest", null), consistent()))});
        out.put("lege-tiegewichten", new Object[]{D_BY_YEAR, TmsDocuments.caseDocument(List.of(
                instance("B1", "Berging", Map.of("opbrengst", "100"), List.of("J1", "J2", "J3")),
                instance("J1", "Berger", Map.of("jaar", "2019"), List.of()),
                instance("J2", "Berger", Map.of("jaar", "2019"), List.of()),
                instance("J3", "Berger", Map.of("jaar", "2020", "gewicht", "1"), List.of())),
                List.of(value("J1", "rangaandeel", "0"), value("J2", "rangaandeel", "0"),
                        value("J3", "rangaandeel", "100"), value("B1", "rangrest", "0"),
                        inconsistent("verdeel de opbrengst op jaar", "J1"),
                        inconsistent("verdeel de opbrengst op jaar", "J2")))});
        out.put("sleutel-in-twee-schalen", new Object[]{D_OP_PEILJAAR, TmsDocuments.caseDocument(List.of(
                instance("B1", "Berging", Map.of("opbrengst", "100"), List.of("J1", "J2", "J3")),
                instance("J1", "Berger", Map.of("peiljaar", "2019"), List.of()),
                instance("J2", "Berger", Map.of("peiljaar", "2019.0"), List.of()),
                instance("J3", "Berger", Map.of("peiljaar", "2020"), List.of())),
                List.of(value("J1", "rangaandeel", "50"), value("J2", "rangaandeel", "50"),
                        value("J3", "rangaandeel", "0"), value("B1", "rangrest", "0"), consistent()))});
        out.put("negatief-gewicht-naar-rato", new Object[]{D_PROPORTIONAL, TmsDocuments.caseDocument(List.of(
                instance("B1", "Berging", Map.of("opbrengst", "100"), List.of("J1", "J2")),
                instance("J1", "Berger", Map.of("gewicht", "3"), List.of()),
                instance("J2", "Berger", Map.of("gewicht", "-1"), List.of())),
                List.of(value("J1", "bergingsaandeel", "100"), value("J2", "bergingsaandeel", "0"),
                        value("B1", "onverdeelde rest", "0"), inconsistent("verdeel de opbrengst naar rato", "J2")))});
        return out;
    }

    @Test void d_the_edge_cases_of_chapter_4_4() throws Exception {
        Map<String, String> outcomes = new LinkedHashMap<>();
        for (Map.Entry<String, Object[]> r : edgeCases().entrySet())
            outcomes.put(r.getKey(), play(document(List.of(), List.of((String) r.getValue()[0]), List.of()),
                    (JsonNode) r.getValue()[1]));
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("gewichten-nul", "gespeeld");
        expected.put("lege-pot-gelijk", "gespeeld");
        expected.put("lege-pot-naar-rato", "gespeeld");
        expected.put("lege-pot-op-volgorde", "gespeeld");
        expected.put("lege-tiegewichten", "gespeeld");
        expected.put("sleutel-in-twee-schalen", "gespeeld");
        expected.put("negatief-gewicht-naar-rato", "gespeeld");
        assertEquals(expected, outcomes);
    }

    // ---------------- E: the validity of a version and the condition ----------------

    static final String E_ALLOCATION = allocation("verdeel de opbrengst per jaar", List.of(
            version("equalShares", "bergingsaandeel", null, null, 0, "onverdeelde rest", null, false, null,
                    validity(null, "2025-12-31")),
            version("proportional", "bergingsaandeel", "basisgewicht", null, 0, "onverdeelde rest", null, false,
                    comparison("greaterOrEqual", ownRead("vrijgavescore", "Berging"), number("10")),
                    validity("2026-01-01", null))));

    static JsonNode eCase(String asOfDate, String score, List<String> expectations) throws Exception {
        return TmsDocuments.caseOn(asOfDate, List.of(
                instance("B1", "Berging", Map.of("opbrengst", "100", "vrijgavescore", score), List.of("J1", "J2")),
                instance("J1", "Berger", Map.of("basisgewicht", "1", "bergingsaandeel", "7"), List.of()),
                instance("J2", "Berger", Map.of("basisgewicht", "3"), List.of())), expectations);
    }

    static Map<String, JsonNode> eCases() throws Exception {
        Map<String, JsonNode> out = new LinkedHashMap<>();
        out.put("2025", eCase("2025-06-01", "12", List.of(value("J1", "bergingsaandeel", "50"),
                value("J2", "bergingsaandeel", "50"), value("B1", "onverdeelde rest", "0"), consistent())));
        out.put("2026", eCase("2026-06-01", "12", List.of(value("J1", "bergingsaandeel", "25"),
                value("J2", "bergingsaandeel", "75"), value("B1", "onverdeelde rest", "0"), consistent())));
        out.put("2026-niet-vrijgegeven", eCase("2026-06-01", "5", List.of(value("J1", "bergingsaandeel", "7"),
                value("J2", "bergingsaandeel", null), value("B1", "onverdeelde rest", null), consistent())));
        out.put("tijdvrij", eCase(null, "12", List.of(consistent())));
        return out;
    }

    @Test void e_the_validity_of_the_version_and_the_condition() throws Exception {
        Path ir = document(List.of(), List.of(E_ALLOCATION), List.of());
        Map<String, String> outcomes = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> z : eCases().entrySet()) outcomes.put(z.getKey(), play(ir, z.getValue()));
        String withoutDate = "a validity-bounded form needs an evaluation date; this session is time-free";
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("2025", "gespeeld");
        expected.put("2026", "gespeeld");
        expected.put("2026-niet-vrijgegeven", "gespeeld");
        expected.put("tijdvrij", "EvaluationFaults: aan het fixpunt staan 2 evaluatiefouten, in de regels \"verdeel de"
                + " opbrengst per jaar #1\", \"verdeel de opbrengst per jaar #2\": fout in de regel \"verdeel de opbrengst"
                + " per jaar #1\": " + withoutDate + "; fout in de regel \"verdeel de opbrengst per jaar #2\": "
                + withoutDate);
        assertEquals(expected, outcomes);
    }

    // ---------------- F: a recipient a creation adds ----------------

    static final String HELPER_BERGER = TmsCreationWitnessTest.objectCreation("maak een hulpberger", "Berging", "Berger",
            List.of(TmsCreationWitnessTest.init("basisgewicht", number("1"))), "null", "bergingsdeelname", "berger");
    static final String F_EQUAL = allocation("verdeel de opbrengst gelijk", List.of(version("equalShares",
            "bergingsaandeel", null, null, 0, "onverdeelde rest", null, false, null, null)));

    static JsonNode fCase() throws Exception {
        return TmsDocuments.caseDocument(List.of(
                instance("B1", "Berging", Map.of("opbrengst", "900"), List.of("J1", "J2")),
                instance("J1", "Berger", Map.of(), List.of()), instance("J2", "Berger", Map.of(), List.of())),
                List.of(value("J1", "bergingsaandeel", "300"), value("J2", "bergingsaandeel", "300"),
                        value("B1", "onverdeelde rest", "0"), consistent()));
    }

    @Test void f_a_recipient_out_of_a_creation() throws Exception {
        assertEquals("gespeeld", play(document(List.of(), List.of(F_EQUAL), List.of(HELPER_BERGER)), fCase()));
    }

    // ---------------- G: a pot derived later ----------------

    static final String POT = computingRule("opbrengst van een berging", "BEREKENING", "Berging", "opbrengst",
            ownRead("bruto", "Berging"), "null");

    static JsonNode gCase() throws Exception {
        return TmsDocuments.caseDocument(List.of(
                instance("B1", "Berging", Map.of("bruto", "90"), List.of("J1", "J2", "J3")),
                instance("J1", "Berger", Map.of(), List.of()), instance("J2", "Berger", Map.of(), List.of()),
                instance("J3", "Berger", Map.of(), List.of())),
                List.of(value("B1", "opbrengst", "90"), value("J1", "bergingsaandeel", "30"),
                        value("J2", "bergingsaandeel", "30"), value("J3", "bergingsaandeel", "30"),
                        value("B1", "onverdeelde rest", "0"), consistent()));
    }

    @Test void g_a_pot_that_is_later_derived() throws Exception {
        assertEquals("gespeeld", play(document(List.of(POT), List.of(F_EQUAL), List.of()), gCase()));
    }

    /** A seeded sample of orders of the generated rules: the write-back writes 30 three times and a rest of 0, and
     *  every firing of the allocation on the empty pot, before the opbrengst is derived, inserts nothing, no share, no
     *  rest, no fault and no deposit (ch. 4.4: an empty pot is no distribution); in at least one order the allocation
     *  fires so. */
    @Test void g_an_empty_pot_on_an_intermediate_state_leaves_nothing_behind() throws Exception {
        Path ir = document(List.of(POT), List.of(F_EQUAL), List.of());
        for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
            DroolsBackend.Build b = DroolsBackend.build(ir, branch);
            int onEmptyPot = 0;
            for (List<String> names : TmsDocuments.sample(TmsDocuments.generated(b), 20261009L,
                    TmsDocuments.shuffles(branch, 100))) {
                String where = " op " + branch + " in volgorde " + names;
                Object b1 = TmsDocuments.beanObject(b.beanLoader(), "Berging", Map.of("bruto", "90"), List.of());
                List<Object> bergers = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    Object j = TmsDocuments.beanObject(b.beanLoader(), "Berger", Map.of(), List.of());
                    join(j, b1);
                    bergers.add(j);
                }
                List<Object> beans = new ArrayList<>(List.of(b1));
                beans.addAll(bergers);
                boolean[] emptyPot = new boolean[1];
                int[] firings = new int[1];
                List<String> inserted = new ArrayList<>();
                TmsDocuments.fire(b, TmsDocuments.kieBaseOf(b, names), names, beans, session -> {
                    session.addEventListener(new DefaultAgendaEventListener() {
                        @Override public void beforeMatchFired(BeforeMatchFiredEvent e) {
                            emptyPot[0] = e.getMatch().getRule().getName().equals("verdeel de opbrengst gelijk")
                                    && e.getMatch().getObjects().stream().anyMatch(o -> o instanceof State s
                                            && s.subject() == b1 && s.attribute().name().equals("opbrengst")
                                            && s.value() == null);
                            if (emptyPot[0]) firings[0]++;
                        }
                        @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                            emptyPot[0] = false;
                        }
                    });
                    session.addEventListener(new DefaultRuleRuntimeEventListener() {
                        @Override public void objectInserted(ObjectInsertedEvent e) {
                            if (emptyPot[0]) inserted.add(String.valueOf(e.getObject()));
                        }
                    });
                }, session -> { });
                Map<String, Object> end = new LinkedHashMap<>();
                for (int i = 0; i < 3; i++)
                    end.put("J" + (i + 1), TmsDocuments.read(bergers.get(i), "bergingsaandeel"));
                end.put("B1", TmsDocuments.read(b1, "onverdeelde rest"));
                assertEquals("{J1=30, J2=30, J3=30, B1=0}", numbers(end), () -> "volgorde" + where);
                assertEquals(List.of(), inserted, () -> "ingevoegd op de lege pot" + where);
                if (firings[0] > 0) onEmptyPot++;
            }
            assertTrue(onEmptyPot > 0, branch + ": geen volgorde liet de verdeling op de lege pot vuren voordat de"
                    + " opbrengst afgeleid was");
        }
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
    static final String H_RULE = computingRule("bergingsaandeel van een berger", "BEREKENING", "Berger",
            "bergingsaandeel", number("5"), "null");
    static final String H_INITIALISATION = computingRule("bergingsaandeel van een berger", "INITIALISATIE", "Berger",
            "bergingsaandeel", number("5"), "null");

    static JsonNode hCase() throws Exception {
        return TmsDocuments.caseDocument(List.of(instance("B1", "Berging", Map.of("opbrengst", "100"), List.of("J1")),
                instance("J1", "Berger", Map.of("gewicht", "1"), List.of())),
                List.of(value("J1", "bergingsaandeel", "100"), consistent()));
    }

    @Test void h_a_second_writer_of_a_target_is_refused_loudly() throws Exception {
        JsonNode caseDocument = hCase();
        Map<String, String> outcomes = new LinkedHashMap<>();
        outcomes.put("regel", play(document(List.of(H_RULE), List.of(D_PROPORTIONAL), List.of()), caseDocument));
        outcomes.put("initialisatie", play(document(List.of(H_INITIALISATION), List.of(D_PROPORTIONAL), List.of()), caseDocument));
        outcomes.put("twee verdelingen", play(document(List.of(), List.of(D_PROPORTIONAL, D_EQUAL), List.of()), caseDocument));
        String beside = "Excluded: transpiler SKIP: verdeling: verdeel de opbrengst naar rato — een verdeling naast de"
                + " regel \"bergingsaandeel van een berger\" op het attribuut 'bergingsaandeel' van Berger: een verdeling"
                + " naast een rekenende regel of een initialisatie op één doel; deze tak draagt deze vorm"
                + " nog niet";
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("regel", beside);
        expected.put("initialisatie", beside);
        expected.put("twee verdelingen", "Excluded: transpiler SKIP: verdeling: verdeel de opbrengst naar rato — twee"
                + " verdelingen schrijven het attribuut 'bergingsaandeel' van Berger (ook \"verdeel de opbrengst gelijk\"):"
                + " een doel heeft hoogstens één verdeling (WF-026)");
        assertEquals(expected, outcomes);
    }

    // ---------------- D, E and F in orders of the generated rules ----------------

    /** Parts D, E and F hold one allocation each, so the order of the document varies nothing; here every case of them
     *  runs through a seeded sample of orders of the generated rules, with the outcome the replay gives. */
    @Test void d_e_f_in_a_sample_of_the_generated_rules() throws Exception {
        Map<String, String> expected = new LinkedHashMap<>(), outcomes = new LinkedHashMap<>();
        Map<String, Object[]> parts = new LinkedHashMap<>();
        for (Map.Entry<String, Object[]> r : edgeCases().entrySet())
            parts.put("d " + r.getKey(), new Object[]{document(List.of(), List.of((String) r.getValue()[0]), List.of()),
                    r.getValue()[1]});
        Path e = document(List.of(), List.of(E_ALLOCATION), List.of());
        for (Map.Entry<String, JsonNode> z : eCases().entrySet())
            parts.put("e " + z.getKey(), new Object[]{e, z.getValue()});
        parts.put("f", new Object[]{document(List.of(), List.of(F_EQUAL), List.of(HELPER_BERGER)), fCase()});
        for (Map.Entry<String, Object[]> d : parts.entrySet()) {
            Path ir = (Path) d.getValue()[0];
            JsonNode caseDocument = (JsonNode) d.getValue()[1];
            String replay = play(ir, caseDocument);
            for (DroolsBackend.Branch branch : TmsDocuments.BRANCHES) {
                DroolsBackend.Build b = DroolsBackend.build(ir, branch);
                for (List<String> names : TmsDocuments.sample(TmsDocuments.generated(b), 20261009L,
                        TmsDocuments.shuffles(branch, 20))) {
                    String key = d.getKey() + " op " + branch + " " + names;
                    outcomes.put(key, playInOrder(b, names, caseDocument));
                    expected.put(key, replay);
                }
            }
        }
        assertEquals(expected, outcomes);
    }

    /**
     * Case {@code caseDocument} of this class's model fired through the rules of {@code b} named in {@code names}, in that
     * order, as its branch builds them ({@link TmsDocuments#kieBaseOf}), with the backend's own driver: the beans as
     * the case input makes them (whole numbers, the bergers of a Berging), the evaluation date as the global the case
     * runner sets, then the expectations against the beans written back and the §9.5 facts of the session, the rule
     * compared as the case runner compares it. Returns "gespeeld", the mismatches, or the fault of the fixpoint as
     * "SimpleName: message", as {@link TmsFaultWitnessTest#play} reports them.
     */
    private static String playInOrder(DroolsBackend.Build b, List<String> names, JsonNode caseDocument) throws Exception {
        Map<String, Object> beans = new LinkedHashMap<>();
        Map<Object, String> labels = new IdentityHashMap<>();
        for (JsonNode i : caseDocument.get("input").get("instances")) {
            Map<String, String> numbers = new LinkedHashMap<>();
            i.get("attributes").fields().forEachRemaining(a -> numbers.put(a.getKey(), a.getValue().get("number").asText()));
            Object beanObject = TmsDocuments.beanObject(b.beanLoader(), i.get("objectType").asText(), numbers, List.of());
            beans.put(i.get("label").asText(), beanObject);
            labels.put(beanObject, i.get("label").asText());
        }
        for (JsonNode i : caseDocument.get("input").get("instances"))
            for (JsonNode berger : i.get("roles").path("bergers")) join(beans.get(berger.asText()), beans.get(i.get("label").asText()));
        String date = caseDocument.get("asOfDate").isNull() ? null : caseDocument.get("asOfDate").asText();
        List<String> channel = new ArrayList<>();
        try {
            TmsCreationWitnessTest.fireInOrder(b, names, new ArrayList<>(beans.values()), session -> {
                if (date != null) session.setGlobal("rekendatum", java.time.LocalDate.parse(date));
            }, session -> channel.addAll(TmsDocuments.inconsistencies(session, labels)));
        } catch (RuntimeException e) {
            return e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        List<String> faults = new ArrayList<>();
        for (JsonNode v : caseDocument.get("expected")) {
            String category = v.get("kind").asText();
            switch (category) {
                case "attributeValue", "attributeEmpty" -> {
                    Object w = TmsDocuments.read(beans.get(v.get("label").asText()), v.get("attribute").asText());
                    boolean goed = category.equals("attributeEmpty") ? w == null
                            : isNumber(w, v.get("value").get("number").asText());
                    if (!goed) faults.add(v.get("label").asText() + "." + v.get("attribute").asText() + " " + w);
                }
                case "inconsistent" -> {
                    String rule = v.get("rule").asText(), label = v.get("label").asText();
                    if (channel.stream().noneMatch(k -> k.endsWith(" @ " + label)
                            && CaseRunner.sameRule(rule, k.substring(0, k.length() - (" @ " + label).length()))))
                        faults.add("geen deponering " + rule + " @ " + label + " in " + channel);
                }
                case "consistent" -> {
                    if (!channel.isEmpty()) faults.add("deponeringen " + channel);
                }
                default -> throw new AssertionError("verwachting " + category);
            }
        }
        return faults.isEmpty() ? "gespeeld" : String.join("; ", faults);
    }

    // ---------------- helpers ----------------

    /** Links the Berger {@code berger} to the Berging {@code berging}, as the case input stores the bergingsdeelname. */
    static void join(Object berger, Object berging) throws Exception {
        for (java.lang.reflect.Method m : berger.getClass().getMethods())
            if (m.getName().equals(Namen.setter("berging")) && m.getParameterCount() == 1) {
                m.invoke(berger, berging);
                return;
            }
        throw new AssertionError("geen setter voor berging op " + berger.getClass());
    }

    static Map<Object, String> labels(List<Object> beans, List<String> names) {
        Map<Object, String> out = new IdentityHashMap<>();
        for (int i = 0; i < beans.size(); i++) out.put(beans.get(i), names.get(i));
        return out;
    }

    private static Object subject(Object inconsistency) {
        try {
            return inconsistency.getClass().getMethod("getSubject").invoke(inconsistency);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean isNumber(Object x, String lexeme) {
        return x instanceof BigDecimal d && d.compareTo(new BigDecimal(lexeme)) == 0;
    }

    private static Object zero(Object x) {
        return x == null ? "leeg" : x;
    }

    /** {@code waarden} printed with every number in its plain form, so that 600 and 600.00 print alike. */
    private static String numbers(Map<String, Object> values) {
        Map<String, Object> out = new LinkedHashMap<>();
        values.forEach((k, v) -> out.put(k, v instanceof BigDecimal d ? d.stripTrailingZeros().toPlainString() : v));
        return out.toString();
    }

    /**
     * Writes every document of this class with its cases into the directory {@code args[0]}, one directory per part,
     * so that the reference interpreter and the ASP backend run the same documents:
     * {@code runner.exe <dir>/<part>/ir.json <dir>/<part>/case-<name>.json}. Run it from {@code backends/drools} after
     * {@code mvn -o -q test-compile}, with the test classpath as {@code TmsMeasurement}'s javadoc builds it.
     */
    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        write(out.resolve("a-later-afgeleid"), ir(A_RULES, A_ALLOCATIONS, List.of()), Map.of("a", aCase()));
        write(out.resolve("b-dubbelbezet"), ir(List.of(), B_ALLOCATIONS, List.of()), Map.of("b", bCase()));
        write(out.resolve("c-degradaties"), ir(C_RULES, List.of(C_PROPORTIONAL), List.of()), Map.of("c", cCase()));
        for (Map.Entry<String, Object[]> r : edgeCases().entrySet())
            write(out.resolve("d-" + r.getKey()), ir(List.of(), List.of((String) r.getValue()[0]), List.of()),
                    Map.of(r.getKey(), (JsonNode) r.getValue()[1]));
        write(out.resolve("e-geldigheid"), ir(List.of(), List.of(E_ALLOCATION), List.of()), eCases());
        write(out.resolve("f-creatie"), ir(List.of(), List.of(F_EQUAL), List.of(HELPER_BERGER)), Map.of("f", fCase()));
        write(out.resolve("g-pot-later"), ir(List.of(POT), List.of(F_EQUAL), List.of()), Map.of("g", gCase()));
        write(out.resolve("h-regel"), ir(List.of(H_RULE), List.of(D_PROPORTIONAL), List.of()), Map.of("h", hCase()));
        write(out.resolve("h-initialisatie"), ir(List.of(H_INITIALISATION), List.of(D_PROPORTIONAL), List.of()),
                Map.of("h", hCase()));
        write(out.resolve("h-twee-verdelingen"), ir(List.of(), List.of(D_PROPORTIONAL, D_EQUAL), List.of()),
                Map.of("h", hCase()));
    }

    private static void write(Path map, String ir, Map<String, JsonNode> cases) throws Exception {
        Files.createDirectories(map);
        Files.writeString(map.resolve("ir.json"), TmsDocuments.JSON.writerWithDefaultPrettyPrinter()
                .writeValueAsString(TmsDocuments.JSON.readTree(ir)));
        for (Map.Entry<String, JsonNode> z : cases.entrySet())
            Files.writeString(map.resolve("case-" + z.getKey() + ".json"),
                    TmsDocuments.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(z.getValue()));
    }
}
