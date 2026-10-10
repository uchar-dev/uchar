package dev.uchar.regelspraak.cases;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.kern.Namen;

import org.drools.model.BetaIndexN;
import org.drools.model.Condition;
import org.drools.model.Constraint;
import org.drools.model.Index;
import org.drools.model.Model;
import org.drools.model.Rule;
import org.drools.model.SingleConstraint;
import org.drools.model.patterns.AccumulatePatternImpl;
import org.drools.model.patterns.PatternImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The joins of the truth-maintenance branch carry an index (card tms-omschakeling, the scale test of point 9). An
 * unindexed join tests every fact of a pattern's class against every tuple on its other side, so a read of a derived
 * value costs one test per subject and a fire grows with the square of the input; {@code Schaalproef} measures it.
 * Which join carries an index is not observable in an outcome, only in time, and no witness may pin a time, so this
 * one reads the structure: the constraints of the executable model the branch builds ({@code Model.getRules()}), each
 * with the index it hands to Drools ({@code SingleConstraint.getIndex()}).
 *
 * <p>A constraint is named by its kind, the id the transpiler gives it without its sequence number. Per step of the
 * card's indexing, every constraint of the step's kinds must carry an equality index over a join; and every kind must
 * occur in the documents read here, so that the check is not met by a kind that no document lowers. The documents
 * are the scale model and goldens of the corpus that together hold every kind checked.
 *
 * <p>Red before the step that indexes a kind, each measured on the commit before its step: {@code identiteitsjoins}
 * found 123 identity joins without an index in these documents, {@code beanveldjoins} 23 bean-field joins, and
 * {@code koppelingseinden} 36 joins on an end of a link, which {@code elkeJoinGeindexeerdOfBenoemd} also named.
 * {@code bonenZonderEqualsOfHashCode} guards what the index rests on, so it was green before; it is red under a
 * mutant of the bean generator that compares by value.
 */
class TmsIndexWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    /** The documents, by name: the scale model, and the goldens that together hold every kind of join the branch
     *  lowers for the corpus beside it (a scan of the corpus on 2026-10-10 chose them): a fold over a created relation,
     *  a fact creation and reads along a path (premiepot), a defaults group in the general form (opgave-vormen), a
     *  timeline in the asOfDate-bucket convention and a fold whose member value can fault (keurlast), an allocation
     *  and a role check (wrakmelding), a fold filter that can fault and deposit (filterwacht), two writers of one target
     *  and a range of more steps (zwaarste-vondst), a uniqueness (postwacht), and a path over an input relation beside
     *  a created one (havenketen). */
    private static final List<String> DOCUMENTEN = List.of("schaal", "jutterswet-premiepot", "jutterswet-opgave-vormen",
            "jutterswet-keurlast", "jutterswet-wrakmelding", "jutterswet-filterwacht", "jutterswet-zwaarste-vondst",
            "jutterswet-postwacht", "jutterswet-havenketen");

    /** Every constraint of every document, read once. */
    private static List<Beperking> beperkingen;

    /** The build of every document, by name. */
    private static final Map<String, DroolsBackend.Build> BOUW = new LinkedHashMap<>();

    /** One constraint of a pattern: the document, the rule, the class of the pattern's fact, its kind, whether it
     *  reads another variable than the pattern's own (a join), and the index it carries, null where none. */
    record Beperking(String document, String regel, Class<?> klasse, String soort, boolean join, Index<?, ?> index) {}

    @BeforeAll
    static void lees() throws Exception {
        beperkingen = new ArrayList<>();
        for (String d : DOCUMENTEN) {
            Path ir = d.equals("schaal") ? Schaalproef.model() : GOLDEN.resolve(d).resolve("ir.json");
            DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.TRUTH_MAINTENANCE);
            BOUW.put(d, b);
            beperkingen.addAll(beperkingen(d, b.tmsTranspiler().model()));
        }
    }

    /** Step 1: the identity joins of a fact field that holds an object, the subject of a standing, a derivation, a
     *  control fact, a contribution or a fault, the object a creation made, and the source of an allocation, with the
     *  object a variable binds. Two kinds of the step no golden lowers, so they are checked where they occur and not
     *  demanded: "lege-schrijver-onderwerp" (a writer that writes leeg beside another) and "doelstand-doel" (the target
     *  check of a fact creation on a derived characteristic); they go through the same helper. */
    @Test
    void identiteitsjoins() {
        geindexeerd(Set.of("stand-onderwerp", "padstand-onderwerp", "stand-bereikt", "waarde-onderwerp",
                "andere-schrijver-onderwerp", "afgeleid-onderwerp", "emmerinvoer-onderwerp", "uitkomst-onderwerp",
                "hoger-uitkomst-onderwerp", "hoger-beslist-onderwerp", "vouw-onderwerp", "filterfout-onderwerp",
                "filterdeponering-onderwerp", "lidfout-onderwerp", "eigen-fout-onderwerp", "gemaakt-onderwerp",
                "nieuw-object", "herkomst", "ontvanger-bron"), Set.of("lege-schrijver-onderwerp", "doelstand-doel"));
    }

    /** Step 2: the bean-field joins of the one-step range of a fold and of an allocation, where the reference field of
     *  the member or of the subject is the object on the other side, and of the role filler of a role check. */
    @Test
    void beanveldjoins() {
        geindexeerd(Set.of("bereik", "verdelingsbereik", "rolvuller"), Set.of());
    }

    /** Step 3: the ends of a link, a {@code Koppeling}: the end the read stands on and the end it reaches, in a fold
     *  over a created relation, in a fact creation's guard against a link that exists, in a role check and a path over
     *  a created relation, and the near end in the guard against a second filler of a to-one role. */
    @Test
    void koppelingseinden() {
        geindexeerd(Set.of("bereik-lid", "bereik-drager", "bestaand-anker", "bestaand-doel", "rolkoppeling-onderwerp",
                "koppeling-nabij", "koppeling-ver", "tweede-vulling-nabij"), Set.of());
    }

    /** The joins that stay without an index, each with its reason: a test that is no identity of two objects, on the
     *  facts an indexed join of the same pattern already selects, or a join the card's steps left out. */
    private static final Map<String, String> ONGEINDEXEERD = Map.of(
            "eerder-lid", "the order of two chains of one stack, among the outcomes of one subject",
            "ander-besluit", "the inequality of two decided values of one stack, among the outcomes of one subject",
            "tweede-vulling-ander", "the inequality of the far ends of two links whose near ends the index pairs",
            "nieuw-in-deze-fase", "the phase number of the mark of the object that the indexed join selects",
            "koppeling-eerder", "the inequality of two phase numbers, beside the indexed end of the link",
            "botsing", "the collision of two uniqueness keys, compared per value (card point 14)",
            "pad", "the range of a fold over more steps, two walks that meet: not among the card's steps",
            "plaats-lid", "the place of a member in an input list, reached by a walk: not among the card's steps",
            "veld-nabij", "a reference field on a path over a created relation: not among the card's steps",
            "veld-ver", "a reference field on a path over a created relation: not among the card's steps");

    /** Every join of the documents carries an equality index or is listed in {@link #ONGEINDEXEERD}, and every listed
     *  kind occurs, so that the list cannot keep a kind that the branch no longer lowers. */
    @Test
    void elkeJoinGeindexeerdOfBenoemd() {
        Set<String> gezien = new TreeSet<>();
        List<String> zonder = new ArrayList<>();
        for (Beperking b : beperkingen) {
            if (!b.join()) continue;
            gezien.add(b.soort());
            boolean index = b.index() instanceof BetaIndexN<?, ?> && b.index().getConstraintType() == Index.ConstraintType.EQUAL;
            if (!index && !ONGEINDEXEERD.containsKey(b.soort()))
                zonder.add(b.soort() + " (" + b.klasse().getSimpleName() + ") in " + b.document() + ", regel '" + b.regel()
                        + "'");
        }
        assertEquals(List.of(), zonder, "joins zonder gelijkheidsindex en zonder reden");
        Set<String> verouderd = new TreeSet<>(ONGEINDEXEERD.keySet());
        verouderd.removeAll(gezien);
        assertEquals(Set.of(), verouderd, "soorten in de lijst zonder index die geen document hier verlaagt");
    }

    /** Step 6: the beans of the fact model override neither {@code equals} nor {@code hashCode}. An indexed join hashes
     *  the object on each side and compares the two with {@code equals}, where its predicate compares with {@code ==},
     *  and Drools trusts the hash; the KieBase in EQUALITY merges equal facts as well. Both hold only while a bean is
     *  equal to itself alone: a bean that compared its values would join a standing to every subject with equal values,
     *  and merge two subjects into one. */
    @Test
    void bonenZonderEqualsOfHashCode() throws Exception {
        List<String> eigen = new ArrayList<>();
        int bonen = 0;
        for (DroolsBackend.Build b : BOUW.values())
            for (RegelSpraakModel.Objecttype ot : b.model().objecttypen.values()) {
                Class<?> k = Class.forName(FactModelGenerator.PAKKET + "." + Namen.ident(ot.naam, true), true,
                        b.beanLoader());
                bonen++;
                if (k.getMethod("equals", Object.class).getDeclaringClass() != Object.class)
                    eigen.add(k.getSimpleName() + ".equals");
                if (k.getMethod("hashCode").getDeclaringClass() != Object.class) eigen.add(k.getSimpleName() + ".hashCode");
            }
        assertTrue(bonen > 0, "geen bonen gelezen");
        assertEquals(List.of(), eigen, "bonen die equals of hashCode overschrijven");
    }

    /** Asserts that every kind of {@code soorten} occurs in the documents, and that every constraint of those kinds and
     *  of {@code zeldzaam}, which need not occur, is a join with an equality index. */
    private static void geindexeerd(Set<String> soorten, Set<String> zeldzaam) {
        Map<String, Integer> gezien = new LinkedHashMap<>();
        List<String> zonder = new ArrayList<>();
        for (Beperking b : beperkingen) {
            if (!soorten.contains(b.soort()) && !zeldzaam.contains(b.soort())) continue;
            gezien.merge(b.soort(), 1, Integer::sum);
            if (!b.join() || !(b.index() instanceof BetaIndexN<?, ?>)
                    || b.index().getConstraintType() != Index.ConstraintType.EQUAL)
                zonder.add(b.soort() + " (" + b.klasse().getSimpleName() + ") in " + b.document() + ", regel '"
                        + b.regel() + "': " + (b.join() ? "" : "geen join, ") + "index " + b.index());
        }
        Set<String> ontbreekt = new TreeSet<>(soorten);
        ontbreekt.removeAll(gezien.keySet());
        assertEquals(Set.of(), ontbreekt, "soorten die geen document hier verlaagt, zodat niets ze toetst");
        assertEquals(List.of(), zonder, "joins zonder gelijkheidsindex (" + gezien + ")");
    }

    /** Every constraint of every pattern of {@code model}, also inside a negation, an existence and the source of an
     *  accumulate. */
    static List<Beperking> beperkingen(String document, Model model) {
        List<Beperking> uit = new ArrayList<>();
        for (Rule r : model.getRules()) loop(document, r.getName(), r.getView(), uit);
        return uit;
    }

    private static void loop(String document, String regel, Condition c, List<Beperking> uit) {
        if (c instanceof PatternImpl<?> p) {
            beperking(document, regel, p.getPatternVariable().getType(), p.getConstraint(), uit);
        } else if (c instanceof AccumulatePatternImpl<?> a) {
            loop(document, regel, a.getCondition(), uit);
            return;
        }
        for (Condition sub : c.getSubConditions()) loop(document, regel, sub, uit);
    }

    private static void beperking(String document, String regel, Class<?> klasse, Constraint c, List<Beperking> uit) {
        if (c == null) return;
        if (c instanceof SingleConstraint s) {
            String id = s.getExprId();
            if (id == null) return;
            int streep = id.lastIndexOf('_');
            uit.add(new Beperking(document, regel, klasse, streep < 0 ? id : id.substring(0, streep),
                    s.getVariables().length > 1, s.getIndex()));
            return;
        }
        for (Constraint k : c.getChildren()) beperking(document, regel, klasse, k, uit);
    }
}
