package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
import dev.uchar.regelspraak.schema.FactModelGenerator;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The §13.3.9 REPEATED-TYPE-PAIR witness: what the bean model derives when MORE THAN ONE feittype relates the
 * same two object types, which readers may address the resulting fields, and what stays refused.
 *
 * <p>The by-PAIR authority ({@code RegelSpraakModel.verbindendeRol}) answers null there — two or more candidate
 * roles, an honest ambiguity — and until this card {@code FactModelGenerator.members} asked it FIRST for every
 * ordinary cross-type field. So a document whose author wrote a UNIQUE role name got a bean with no reference
 * field at all. RED FIRST, measured on the jar built before the change, on the models below:
 * <pre>
 *   exec  telling    : "de aggregatie-join heeft geen beanveld — het beanmodel leidt voor de rol 'thuishaven'
 *                       op Schuit geen referentieveld naar Haven af (§13.3.9 opslagrichting)"
 *   drl   telling    : "aggregatie-join: het beanmodel leidt voor de rol 'thuishaven' op Schuit geen
 *                       referentieveld naar Haven af (§13.3.9 opslagrichting)"
 *   exec  feitcreatie: "feitcreatie (het beanmodel leidt voor de rol 'thuishaven' op Schuit geen
 *                       referentieveld naar Haven af): winterberging door de haven"
 *   drl   feitcreatie: "feitcreatie: het beanmodel leidt voor de rol 'thuishaven' op Schuit geen
 *                       referentieveld naar Haven af (§13.3.9 opslagrichting)"
 * </pre>
 *
 * <p>The derivation now runs PER ROLE. What is pinned here is therefore both halves: the shapes that became
 * addressable, and the ones that must STAY refused — each with its own sentence, because collapsing them would
 * turn "this model cannot hold that" into "this model is ambiguous", which it is not.
 *
 * <p>Every model is an in-memory variant of ONE corpus golden ({@code jutterswet-dubbelverbinding}, authored
 * for this card); the golden's own files are never touched. Two variants below carry a guard hosted on the
 * DOEL. That form is NOT frontend-emittable as a comparison and the reference interpreter FAULTS on the
 * characteristic shape of it (WF-100, maintainer decision 2026-09-10: the owner names the object the check is
 * about, and ref reads one object per condition), so those two rows pin THIS backend's lowering alone and say
 * so at their site — they are exactly the two forms the corpus cannot carry.
 */
class RepeatedTypePairWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** FOUR feittypen relate Haven to Schuit — thuisligging (ligger, to-many), winterberging (winterschuit,
     *  to-many), keurberging (keurschuit, to-many) and vlagvoering (vlaggenschip, to-ONE) — so the by-pair
     *  search is ambiguous by construction while every written role name is unique. */
    private static final String DUBBELVERBINDING = "jutterswet-dubbelverbinding";

    // ---------------- 1. the derivation itself: per ROLE, and the by-pair search untouched ----------------

    @Test void the_by_pair_derivation_stays_ambiguous_and_does_not_guess() throws Exception {
        RegelSpraakModel m = model(ir(DUBBELVERBINDING));
        Objecttype haven = m.objecttypeOpNaam("Haven"), schuit = m.objecttypeOpNaam("Schuit");
        // The authority every bindingless consumer shares stays exactly as it was. Teaching it to pick a
        // winner would turn an honest ambiguity into a silent wrong join across the whole back-end —
        // RUNTIME-SEMANTICS ch. 5.5 forbids it and ref refuses the same derivation (connecting_pair).
        assertNull(m.verbindendeRol(schuit, haven), "four candidate Haven roles: by PAIR this is ambiguous");
        assertNull(m.verbindendeRol(haven, schuit), "and so is the other direction");
        // ...while every WRITTEN name still selects exactly one, which is what the bean model may derive from.
        assertEquals(List.of("thuishaven", "winterhaven", "keurhaven", "vlaghaven"),
                m.verbindendeRollen(schuit, haven), "one addressable role per uniquely written name");
        assertEquals(List.of("ligger", "winterschuit", "keurschuit", "vlaggenschip"),
                m.verbindendeRollen(haven, schuit));
    }

    @Test void the_bean_model_carries_a_field_per_to_one_role() throws Exception {
        RegelSpraakModel m = model(ir(DUBBELVERBINDING));
        Objecttype haven = m.objecttypeOpNaam("Haven"), schuit = m.objecttypeOpNaam("Schuit");
        // FOUR Haven references on Schuit — one per to-one Haven role — each typed, each named after its role.
        for (String rol : List.of("thuishaven", "winterhaven", "keurhaven", "vlaghaven"))
            assertTrue(FactModelGenerator.hasReferenceField(m, schuit, rol, haven), rol);
        // and exactly ONE the other way: the three to-many Schuit roles store on the Schuit, the to-one
        // `vlaggenschip` is the 1:1 leg that needs a field of its own (ch. 5.7a, consequence 2).
        assertTrue(FactModelGenerator.hasReferenceField(m, haven, "vlaggenschip", schuit));
        for (String rol : List.of("ligger", "winterschuit", "keurschuit"))
            assertTrue(!FactModelGenerator.hasReferenceField(m, haven, rol, schuit),
                    "a to-many role's storage is the far end's field: " + rol);
    }

    // ---------------- 2. the fold over a WRITTEN element role (the old toolchain manifest's count) ------

    @Test void the_count_over_every_written_element_role_chooses_its_own_fact_type() throws Exception {
        // The old toolchain's manifest carried one fixture of exactly this shape — a count whose written
        // element role disambiguates a pair that more than one feittype connects — and both of its back-ends
        // produced the pinned number. Here, in this repo's own vocabulary: S1 and S2 lie in H1 (thuisligging)
        // while S1 ALSO winters at H2 (winterberging). Two roles, one type pair, one instance in both.
        // Counting either role over the by-pair search would answer nothing at all; counting the WRONG one
        // would answer 2 and 0 instead of 2 and 1.
        BothBranches.green(onlyCounts(), JSON.readTree(twoRolesCase("2", "0", "0", "1")),
                "liggertal H1=2 / H2=0, wintertal H1=0 / H2=1");
    }

    @Test void the_swapped_role_is_a_named_difference_no_silent_tie() throws Exception {
        // MUTATION FLANK for the role choice: the same world with the two counts' expectations SWAPPED is a
        // named mismatch in BOTH branches, so a derivation that handed both folds the same field could not
        // pass this row by accident.
        BothBranches.red(onlyCounts(), JSON.readTree(twoRolesCase("0", "1", "2", "0")),
                "wintertal", "swapping the two roles' counts must be observable");
    }

    // ---------------- 3. the §13.4.7 creation: the six outcomes of the old manifests ----------------

    @Test void fact_creation_without_condition_lays_the_relation_on_each_ligger() throws Exception {
        // (1) the unguarded 1-hop shape: every ligger of the haven gets the new relation.
        BothBranches.green(creationVariant(null, true), JSON.readTree(creationCase("2", "40", null)),
                "wintertal H1=2 and S1 reads its winterhaven back");
    }

    @Test void fact_creation_with_anchor_characteristic_fires_only_for_the_safe_haven() throws Exception {
        // (2) the anker-kenmerk guard, BOTH outcomes: with the kenmerk the haven links both its liggers,
        // without it none. The guard rides the ANKER pattern in both branches, and the window is dropped here
        // so this row is about the guard alone.
        Path m = creationVariant(anchorCharacteristicCheck("veilig"), true);
        BothBranches.green(m, JSON.readTree(creationCase("2", "40", null)),
                "veilige haven: wintertal 2 and S1 reads its winterhaven back");
        BothBranches.green(m, JSON.readTree(creationCase("0", null, "onveilig")),
                "without the kenmerk the creation makes nothing — wintertal 0, ligprijs leeg");
    }

    @Test void fact_creation_with_target_characteristic_links_only_the_gekeurde_liggers() throws Exception {
        // (3) the DOEL-kenmerk guard: the constraint hosts on the $doel pattern, never the anker. S1 is
        // gekeurd and S2 is not, so exactly one of H1's two liggers is linked.
        // NOT a corpus shape: ref FAULTS on a characteristic check hosted on another type (WF-100, decision
        // 2026-09-10), so this row pins the Drools lowering alone — which is precisely the old manifest's
        // fixture, kept because the backend still carries the form.
        BothBranches.green(creationVariant(targetCharacteristicCheck("gekeurd"), true), JSON.readTree(creationCase("1", "40", null)),
                "wintertal H1=1 — only the gekeurde ligger is linked");
    }

    @Test void fact_creation_with_target_comparison_is_refused() throws Exception {
        // (4) the DOEL-comparison guard, the manifest's sibling of the row above. Until L2's advisory round this
        // row pinned the backend's own lowering in both branches (romplengte > 8 selected S1 and rejected S2).
        // The guard's one subject is the anker (IR-SPEC ch. 3.4, WF-109): the doel is read in a guard through a
        // characteristic check only (the row above), and an attribute of the doel read without bindings names
        // another type than that subject — refused by the verifier and by ref (WF-053), and by this loader now,
        // in their sentence.
        JsonNode ir = JSON.readTree(Files.readString(creationVariant(targetComparison("romplengte", "8"), true)));
        IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.loadAll(ir));
        assertEquals("/rules/factCreations/0/versions/0/condition/left", e.site(), e.getMessage());
        assertTrue(e.getMessage().endsWith("its subjectType is the subject type 'Haven' — 'Schuit' names another type,"
                + " which only a navigation reaches"), e.getMessage());
    }

    @Test void fact_creation_within_and_before_the_validity_window() throws Exception {
        // (5) and (6), the two date rows of the old window manifest, on the golden itself: the creation is
        // `geldig vanaf 01-03-2026`, so a rekendatum inside the window creates the relation and one before it
        // creates nothing — and the count over the created role, plus the read back from the Schuit side,
        // both follow the window.
        BothBranches.green(ir(DUBBELVERBINDING), JSON.readTree(golden("case1-binnen-venster.json")),
                "inside: wintertal 2 / 1 / 0, ligprijs 40 / leeg / 25 / leeg");
        BothBranches.green(ir(DUBBELVERBINDING), JSON.readTree(golden("case2-voor-venster.json")),
                "before: wintertal 0 / 0 / 0, every ligprijs leeg");
    }

    // ---------------- 4. RUNTIME-SEMANTICS ch. 5.7a: BOTH relational directions ----------------

    @Test void the_written_role_lands_on_the_field_of_that_fact_type() throws Exception {
        // The input path, at bean level: `winterhaven` must reach Schuit.winterhaven and leave the three
        // sister fields alone. A by-pair route could not resolve this at all; a route that resolved it
        // WRONGLY would fill one of the other three and still count something.
        Map<String, Object> beans = CaseRunner.inputBeans(onlyCounts(), JSON.readTree(twoRolesCase("2", "0", "0", "1")));
        assertSame(beans.get("H1"), RegelSpraakRuntime.get(beans.get("S1"), "thuishaven"), "thuisligging -> H1");
        assertSame(beans.get("H2"), RegelSpraakRuntime.get(beans.get("S1"), "winterhaven"), "winterberging -> H2");
        assertNull(RegelSpraakRuntime.get(beans.get("S1"), "keurhaven"), "and nothing else was written");
        assertNull(RegelSpraakRuntime.get(beans.get("S1"), "vlaghaven"));
    }

    @Test void both_spellings_of_a_relation_write_the_same_field() throws Exception {
        // ch. 5.7a: a supplied link ESTABLISHES the relation and BOTH roles read it, so the 1:N relation's
        // ONE field must be written whichever end the case spells. Spelled from the HAVEN side the sister
        // role is resolved BY NAME (verbindendeRolViaElementnaam) — the only route on a repeated pair.
        Map<String, Object> fromSchuit = CaseRunner.inputBeans(onlyCounts(), JSON.readTree(spellingCase(true)));
        Map<String, Object> fromHaven = CaseRunner.inputBeans(onlyCounts(), JSON.readTree(spellingCase(false)));
        assertSame(fromSchuit.get("H1"), RegelSpraakRuntime.get(fromSchuit.get("S1"), "thuishaven"));
        assertSame(fromHaven.get("H1"), RegelSpraakRuntime.get(fromHaven.get("S1"), "thuishaven"),
                "the Haven-side spelling writes the SAME field on the Schuit");
    }

    @Test void the_one_to_one_relation_gets_both_fields_written() throws Exception {
        // ch. 5.7a consequence 2: a 1:1 relation owns TWO fields — one per direction — so the case-input
        // layer must fill the second one itself, on a repeated pair as anywhere else. `vlagvoering` is that
        // shape here. RED FLANK: with the mirror write removed the far side reads a spuriously empty
        // relation, which is the reading ch. 5.7a rejects.
        Map<String, Object> fromHaven = CaseRunner.inputBeans(onlyCounts(), JSON.readTree(flagCase(true)));
        assertSame(fromHaven.get("S1"), RegelSpraakRuntime.get(fromHaven.get("H1"), "vlaggenschip"));
        assertSame(fromHaven.get("H1"), RegelSpraakRuntime.get(fromHaven.get("S1"), "vlaghaven"),
                "the Haven-side spelling ALSO writes the Schuit's own leg");
        Map<String, Object> fromSchuit = CaseRunner.inputBeans(onlyCounts(), JSON.readTree(flagCase(false)));
        assertSame(fromSchuit.get("H1"), RegelSpraakRuntime.get(fromSchuit.get("S1"), "vlaghaven"));
        assertSame(fromSchuit.get("S1"), RegelSpraakRuntime.get(fromSchuit.get("H1"), "vlaggenschip"),
                "and the Schuit-side spelling writes the Haven's");
    }

    @Test void the_created_link_is_on_the_field_of_its_own_fact_type() throws Exception {
        // The creation path, at bean level after the fire: the new relation lands on `winterhaven` and the
        // relation it JOINED over (`thuishaven`) is untouched. The two are different fields over the same
        // type pair, which is the whole construct. On the exec-model branch, through the entry a consumer
        // uses: one vuur to the fixpoint, then the write-back, which puts the link the creation laid on the field.
        CaseRunner.forMeasurement(ir(DUBBELVERBINDING), DroolsBackend.Branch.EXEC_MODEL).withSession(
                JSON.readTree(golden("case1-binnen-venster.json")), (bouw, sessie, labels) -> {
                    assertNull(DroolsBackend.vuur(bouw, sessie).geenFixpunt(), "the fixpoint");
                    DroolsBackend.zetAfgeleideWaarden(bouw, sessie);
                    assertSame(labels.get("H1"), RegelSpraakRuntime.get(labels.get("S1"), "winterhaven"),
                            "the creation wrote the winterberging leg");
                    assertSame(labels.get("H1"), RegelSpraakRuntime.get(labels.get("S1"), "thuishaven"),
                            "and the joined-over thuisligging leg still says what the case said");
                    assertNull(RegelSpraakRuntime.get(labels.get("S4"), "winterhaven"),
                            "H3 is not veilig, so its ligger got nothing");
                    return null;
                });
    }

    @Test void the_created_link_is_read_on_both_sides() throws Exception {
        // ONE stored field, read from BOTH ends: the Haven counts its winterschuiten (the far side of the
        // field) and the Schuit reads the kadegeld of its winterhaven (the near side). A creation that wrote
        // a field only one reader reaches would still satisfy one of the two.
        // MUTATION FLANK: a creation that set the role of ANOTHER feittype over the same pair moves both
        // readings at once, and the flank names it. Until P2 (2026-09-22) the flank twisted zetVeld; since then
        // the written field is derived from the creation's binding, so the flank twists the BINDING. Since L2 the
        // wire has no zetVeld to twist: BeansFieldWitnessTest pins that the loader refuses one.
        BothBranches.green(ir(DUBBELVERBINDING), JSON.readTree(golden("case1-binnen-venster.json")),
                "both readings of the created link agree with ref and clingo");
        String verdict = BothBranches.red(creationMoved("keurberging", "keurhaven", "keurschuit"), JSON.readTree(golden("case1-binnen-venster.json")),
                "wintertal", "writing the sister feittype's field must be observable");
        assertTrue(verdict.contains("ligprijs"), "and the Schuit-side reading moves with it: " + verdict);
    }

    // ---------------- 5. what STAYS refused, each with its own sentence ----------------

    @Test void a_non_unique_role_name_derives_no_field() throws Exception {
        // Two feittypen whose Haven role carries the SAME name: the written name selects nothing, so no field
        // is derived for it and the fold that joins over it refuses — per NAME, not per pair. The sister
        // roles of the other feittypen keep their fields; only the shared name loses.
        // BOTH branches, one sentence: a producer reading the gate report gets one diagnosis whichever branch
        // produced it, which is the whole point of the shared reason function.
        BothBranches.refused(roleNameCollision(), JSON.readTree(twoRolesCase("2", "0", "0", "1")),
                "het beanmodel leidt voor de rol 'thuishaven' op Schuit geen referentieveld naar Haven af",
                "a role name two feittypen share derives nothing");
    }

    @Test void name_sharing_with_a_declared_member_stays_a_refusal_of_its_own() throws Exception {
        // leden()'s dedup (attribuut > kenmerk > referentie) displaces the reference, so the field EXISTS as a
        // BigDecimal. That is a different failure from an underivable role and must keep its own sentence —
        // blaming ambiguity here would accuse the document of something it is not guilty of.
        BothBranches.refused(nameSharingSchuit(), JSON.readTree(twoRolesCase("2", "0", "0", "1")),
                "de rol 'thuishaven' op Schuit deelt haar naam met een gedeclareerd lid",
                "a declared member of the same name displaces the reference");
    }

    @Test void a_many_role_without_opslag_carrying_other_side_stays_refused() throws Exception {
        // BOTH roles of one feittype to-many: a to-many role's storage is the far end's field, and here that
        // end is to-many too, so the model derives nothing at EITHER end. Its own sentence, naming the
        // CARDINALITY rather than a derivation that in fact succeeded.
        //
        // Dual since 2026-09-18. Measured before that: the exec branch named the cardinality while the DRL-text
        // branch answered "het beanmodel leidt voor de rol 'thuishaven' op Schuit geen referentieveld naar Haven
        // af" — one failure, two diagnoses, the second of them wrong about WHY. The cardinality half now lives
        // beside the shared reason function (FactModelGenerator.noJoinFieldReason) and both branches restate it
        // at the fold-join seam; this row is what keeps them together.
        BothBranches.refused(bothSidesMany(), JSON.readTree(twoRolesCase("2", "0", "0", "1")),
                "de rol 'thuishaven' op Schuit is meervoudig, dus haar opslag is het veld aan de overzijde,"
                + " en ook daar leidt het beanmodel er geen af (beide zijden meervoudig)",
                "both sides to-many: no bean can hold the relation");
    }

    @Test void the_other_side_that_indeed_carries_a_field_does_not_get_the_both_sides_sentence() throws Exception {
        // The half of that sentence the shared derivation must NOT over-claim. With the element role to-ONE and
        // the subject's role to-many, the join field is missing HERE but present at the far end — one bean does
        // hold the relation. Saying "beide zijden meervoudig" there would diagnose a cardinality defect the
        // document does not have; measured on the corpus, `anchor/jutterswet-gemeenteberging` is exactly this
        // shape, and the first cut of the shared sentence said it of that golden.
        // The analysis refuses the document EARLIER, on its own conservative arm (a to-one element role whose sister
        // is to-many: the fact type has one bean field for both directions), as the destructive exec-model branch did,
        // and so on both branches since card drl-teksttak-op-truth-maintenance. Until that card the destructive
        // DRL-text branch reached the shared reason function and said "is meervoudig, dus haar opslag is het veld aan de
        // overzijde", stopping there; neither branch now says "beide zijden meervoudig" of this document.
        String sentence = "regel: liggertal van een haven — een Aggregation over de enkelvoudige elementrol 'ligger' wiens"
                + " zusterrol 'thuishaven' meervoudig is ('het aantal liggers van de Haven'): dat feittype heeft één"
                + " beanveld voor beide richtingen, en deze tak weigert de vorm voorzichtig tot een eigen kaart haar meet"
                + " (§13.3.9 opslagrichting)";
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> CaseRunner.playCase(elementRoleToOne(),
                    JSON.readTree(twoRolesCase("2", "0", "0", "1")), branch));
            assertTrue(e.getMessage().endsWith(sentence), branch + ": " + e.getMessage());
            assertTrue(!e.getMessage().contains("beide zijden meervoudig"),
                    "the far end DOES derive a field, so no sentence claims both ends: " + e.getMessage());
        }
    }

    @Test void a_consumer_without_a_range_does_not_load() throws Exception {
        // The LOUD-AMBIGUITY row. A SOM's argument is an ATTRIBUTE name, never a role, so a fold that names no
        // range AT ALL has nothing but the type pair — which two fact types make ambiguous here — and no branch may
        // pick a winner from it. Since L2 of the v1 wire migration the range is required on this fold (IR-SPEC
        // ch. 4.3): the document is refused at load, by name, before any branch could guess. The bound flank is
        // in SourceRoleFoldWitnessTest, where the same fold PLAYS in both branches.
        for (boolean viaDrl : new boolean[] { false, true }) {
            IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class,
                    () -> CaseRunner.playCase(sumOverLiggers(), JSON.readTree(twoRolesCase("2", "0", "0", "1")), viaDrl));
            assertTrue(e.getMessage().contains("carries its range (sourceRole or sourcePath)"), e.getMessage());
        }
    }

    // ---------------- fixtures ----------------

    private static Path ir(String golden) {
        Path p = CORPUS.resolve(golden).resolve("ir.json");
        assertTrue(Files.exists(p), "golden ir.json present: " + p.toAbsolutePath());
        return p;
    }

    private static String golden(String caseFile) throws Exception {
        return Files.readString(CORPUS.resolve(DUBBELVERBINDING).resolve(caseFile));
    }

    private static RegelSpraakModel model(Path irPath) throws Exception {
        return IrLoader.loadAll(JSON.readTree(Files.readString(irPath))).model();
    }

    private static ObjectNode lees(String golden) throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(ir(golden)));
    }

    /** The golden reduced to its two counts — no creations, no navigated reads — so a supplied relation is
     *  read exactly as supplied and no rule writes a role field. */
    private static Path onlyCounts() throws Exception {
        ObjectNode ir = lees(DUBBELVERBINDING);
        IrPath.rules(ir).putArray("factCreations");
        keepRules(ir, "liggertal van een haven", "wintertal van een haven");
        return temporary("dv-tellingen", ir);
    }

    /** The golden reduced to ONE creation (winterberging) plus the count over the created role and the read
     *  back from the Schuit side, with the given voorwaarde (null = unguarded) and, when {@code always}, the
     *  §4.2 window dropped so the row is about the guard alone. */
    private static Path creationVariant(ObjectNode voorwaarde, boolean always) throws Exception {
        ObjectNode ir = lees(DUBBELVERBINDING);
        keepCreations(ir, "winterberging door de haven");
        keepRules(ir, "wintertal van een haven", "ligprijs van een schuit");
        ObjectNode fc = IrPath.version(ir, "factCreations", 0, 0);
        if (voorwaarde == null) fc.remove("condition"); else fc.set("condition", voorwaarde);
        if (always) fc.set("validity", JSON.createObjectNode()
                .put("node", "Validity"));
        // the ligprijs rule carries its own doel-kenmerk gate in the golden; drop it so this row reads the
        // created link for every Schuit and the creation's guard is the only selector.
        IrPath.version(ir, "rules", "ligprijs van een schuit").remove("condition");
        return temporary("dv-creatie", ir);
    }

    /** The golden with the creation bound to ANOTHER feittype over the same pair — the mutation the both-sides
     *  row must catch. Since P2 the written field follows from that binding, and since L2 the wire carries no
     *  field name beside it (BeansFieldWitnessTest pins the loader's refusal of one). */
    private static Path creationMoved(String feittype, String anchorRole, String targetRole) throws Exception {
        ObjectNode ir = lees(DUBBELVERBINDING);
        IrPath.version(ir, "factCreations", "winterberging door de haven")
                .put("factType", feittype).put("anchorRole", anchorRole).put("targetRole", targetRole);
        return temporary("dv-creatie-verlegd", ir);
    }

    /** The counts-only model with winterberging's Haven role RENAMED to `thuishaven`, so two feittypen reach
     *  Haven from Schuit under one name and neither derives a field. */
    private static Path roleNameCollision() throws Exception {
        ObjectNode ir = lees(DUBBELVERBINDING);
        IrPath.rules(ir).putArray("factCreations");
        keepRules(ir, "liggertal van een haven", "wintertal van een haven");
        for (JsonNode r : IrPath.declaratie(ir, "factTypes", "winterberging").get("roles"))
            if ("winterhaven".equals(r.get("name").asText())) ((ObjectNode) r).put("name", "thuishaven");
        return temporary("dv-rolnaambotsing", ir);
    }

    /** The counts-only model with a Numeriek attribute SHARING the role name `thuishaven` on Schuit. */
    private static Path nameSharingSchuit() throws Exception {
        ObjectNode ir = lees(DUBBELVERBINDING);
        IrPath.rules(ir).putArray("factCreations");
        keepRules(ir, "liggertal van een haven", "wintertal van een haven");
        IrPath.addAttribute(IrPath.objectType(ir, "Schuit"), "thuishaven", number());
        return temporary("dv-naamdeling", ir);
    }

    /** The counts-only model with thuisligging's cardinalities SWAPPED — the element role `ligger` to-one and
     *  the subject's `thuishaven` to-many — so the join field is missing on the Schuit but present on the Haven. */
    private static Path elementRoleToOne() throws Exception {
        ObjectNode ir = lees(DUBBELVERBINDING);
        IrPath.rules(ir).putArray("factCreations");
        keepRules(ir, "liggertal van een haven", "wintertal van een haven");
        for (JsonNode r : IrPath.declaratie(ir, "factTypes", "thuisligging").get("roles"))
            ((ObjectNode) r).put("many", "thuishaven".equals(r.get("name").asText()));
        return temporary("dv-elementrol-toone", ir);
    }

    /** The counts-only model with BOTH roles of thuisligging to-many — no field at either end. */
    private static Path bothSidesMany() throws Exception {
        ObjectNode ir = lees(DUBBELVERBINDING);
        IrPath.rules(ir).putArray("factCreations");
        keepRules(ir, "liggertal van een haven", "wintertal van een haven");
        for (JsonNode r : IrPath.declaratie(ir, "factTypes", "thuisligging").get("roles"))
            ((ObjectNode) r).put("many", true);
        return temporary("dv-beide-meervoudig", ir);
    }

    /** The counts-only model with the liggertal fold turned into a SOM over the liggers' romplengte — whose
     *  argument is an ATTRIBUTE name — and its {@code sourceRole} REMOVED, so no route to the range remains but
     *  the by-pair search. (With the binding in place the same fold is lowerable and plays; that is
     *  SourceRoleFoldWitnessTest's business.) */
    private static Path sumOverLiggers() throws Exception {
        ObjectNode ir = lees(DUBBELVERBINDING);
        IrPath.rules(ir).putArray("factCreations");
        keepRules(ir, "liggertal van een haven");
        ObjectNode fold = (ObjectNode) IrPath.version(ir, "rules", 0, 0).get("value");
        fold.remove("sourceRole");
        fold.put("kind", "sum").set("source", IrPath.sourceReference("de som van de romplengte van alle liggers van de Haven"));
        ObjectNode arg = (ObjectNode) fold.get("arguments").get(0);
        arg.put("attribute", "romplengte").put("subjectType", "Schuit")
           .set("source", IrPath.sourceReference("de romplengte van alle liggers van de Haven"));
        return temporary("dv-som", ir);
    }

    private static void keepRules(ObjectNode ir, String... names) {
        List<String> keep = List.of(names);
        ArrayNode out = JSON.createArrayNode();
        for (JsonNode r : IrPath.rulesList(ir, "rules")) if (keep.contains(r.get("name").asText())) out.add(r);
        if (out.size() != keep.size()) throw new AssertionError("not every rule of " + keep + " is in the document");
        IrPath.rules(ir).set("rules", out);
    }

    private static void keepCreations(ObjectNode ir, String naam) {
        ArrayNode out = JSON.createArrayNode();
        out.add(IrPath.vorm(ir, "factCreations", naam));
        IrPath.rules(ir).set("factCreations", out);
    }

    /** A §8.1.7 characteristic check on the ANKER — no eigenaar, so it hosts on the anker pattern. */
    private static ObjectNode anchorCharacteristicCheck(String kenmerk) {
        return JSON.createObjectNode().put("node", "CharacteristicCheck").put("characteristic", kenmerk)
                .put("negated", false)
                .set("source", IrPath.sourceReference("de Haven " + kenmerk + " is"));
    }

    /** A §8.1.7 characteristic check HOSTED on the doel (the eigenaar field, WF-100). */
    private static ObjectNode targetCharacteristicCheck(String kenmerk) {
        return JSON.createObjectNode().put("node", "CharacteristicCheck").put("characteristic", kenmerk)
                .put("negated", false).put("host", "Schuit")
                .set("source", IrPath.sourceReference("de ligger " + kenmerk + " is"));
    }

    /** A comparison on an attribute of the DOEL — the shape the frontend leaves unresolved and the transpilers
     *  host on the $doel pattern. */
    private static ObjectNode targetComparison(String attribuut, String bound) {
        ObjectNode left = JSON.createObjectNode();
        left.put("node", "AttributeReference").put("attribute", attribuut).put("subjectType", "Schuit");
        left.set("source", IrPath.sourceReference("de " + attribuut + " van de ligger"));
        ObjectNode right = JSON.createObjectNode();
        right.put("node", "Literal").putObject("value").put("number", bound);
        right.set("type", number());
        right.set("source", IrPath.sourceReference(bound));
        ObjectNode v = JSON.createObjectNode();
        v.put("node", "Comparison").put("op", "greater");
        v.set("left", left);
        v.set("right", right);
        v.set("source", IrPath.sourceReference("de " + attribuut + " van de ligger groter is dan " + bound));
        return v;
    }

    private static ObjectNode number() {
        return JSON.createObjectNode().put("node", "Number").put("whole", true)
                .put("decimals", 0).put("sign", "nonNegative");
    }

    /** Two havens and two schuiten with BOTH roles of the repeated pair written on S1: it lies in H1
     *  (thuisligging) and winters at H2 (winterberging). The four counts are the pinned cells. */
    private static String twoRolesCase(String l1, String l2, String w1, String w2) {
        return """
                { "format": "uchar-case-v0", "asOfDate": "2026-06-01",
                  "input": { "instances": [
                      { "label": "H1", "objectType": "Haven", "attributes": { "kadegeld": { "number": "40" } },
                        "roles": {}, "characteristics": [] },
                      { "label": "H2", "objectType": "Haven", "attributes": { "kadegeld": { "number": "25" } },
                        "roles": {}, "characteristics": [] },
                      { "label": "S1", "objectType": "Schuit", "attributes": { "romplengte": { "number": "12" } },
                        "roles": { "thuishaven": ["H1"], "winterhaven": ["H2"] }, "characteristics": [] },
                      { "label": "S2", "objectType": "Schuit", "attributes": { "romplengte": { "number": "7" } },
                        "roles": { "thuishaven": ["H1"] }, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [
                      { "kind": "attributeValue", "label": "H1", "attribute": "liggertal", "value": { "number": "%s" } },
                      { "kind": "attributeValue", "label": "H2", "attribute": "liggertal", "value": { "number": "%s" } },
                      { "kind": "attributeValue", "label": "H1", "attribute": "wintertal", "value": { "number": "%s" } },
                      { "kind": "attributeValue", "label": "H2", "attribute": "wintertal", "value": { "number": "%s" } } ] }
                """.formatted(l1, l2, w1, w2);
    }

    /** One relation, spelled from the SCHUIT side (`thuishaven`) or the HAVEN side (`liggers`). */
    private static String spellingCase(boolean fromSchuit) {
        return """
                { "format": "uchar-case-v0", "asOfDate": "2026-06-01",
                  "input": { "instances": [
                      { "label": "H1", "objectType": "Haven", "attributes": { "kadegeld": { "number": "40" } },
                        "roles": { %s }, "characteristics": [] },
                      { "label": "S1", "objectType": "Schuit", "attributes": { "romplengte": { "number": "12" } },
                        "roles": { %s }, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [
                      { "kind": "attributeValue", "label": "H1", "attribute": "liggertal", "value": { "number": "1" } } ] }
                """.formatted(fromSchuit ? "" : "\"liggers\": [\"S1\"]", fromSchuit ? "\"thuishaven\": [\"H1\"]" : "");
    }

    /** The 1:1 vlagvoering, spelled from the HAVEN side (`vlaggenschip`) or the SCHUIT side (`vlaghaven`). */
    private static String flagCase(boolean fromHaven) {
        return """
                { "format": "uchar-case-v0", "asOfDate": "2026-06-01",
                  "input": { "instances": [
                      { "label": "H1", "objectType": "Haven", "attributes": { "kadegeld": { "number": "40" } },
                        "roles": { %s }, "characteristics": [] },
                      { "label": "S1", "objectType": "Schuit", "attributes": { "romplengte": { "number": "12" } },
                        "roles": { %s }, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [
                      { "kind": "attributeValue", "label": "H1", "attribute": "liggertal", "value": { "number": "0" } } ] }
                """.formatted(fromHaven ? "\"vlaggenschip\": [\"S1\"]" : "", fromHaven ? "" : "\"vlaghaven\": [\"H1\"]");
    }

    /** One haven with two liggers — S1 gekeurd and 12 long, S2 not and 7 — with the created role's count and,
     *  when {@code ligprijs} is given, the read back from S1's own side. {@code kenmerk} null makes H1 veilig. */
    private static String creationCase(String wintertal, String ligprijs, String kenmerk) {
        String lieRow = ligprijs == null
                ? "{ \"kind\": \"attributeEmpty\", \"label\": \"S1\", \"attribute\": \"ligprijs\" }"
                : "{ \"kind\": \"attributeValue\", \"label\": \"S1\", \"attribute\": \"ligprijs\","
                  + " \"value\": { \"number\": \"" + ligprijs + "\" } }";
        return """
                { "format": "uchar-case-v0", "asOfDate": "2026-06-01",
                  "input": { "instances": [
                      { "label": "H1", "objectType": "Haven", "attributes": { "kadegeld": { "number": "40" } },
                        "roles": {}, "characteristics": [%s] },
                      { "label": "S1", "objectType": "Schuit", "attributes": { "romplengte": { "number": "12" } },
                        "roles": { "thuishaven": ["H1"] }, "characteristics": ["gekeurd"] },
                      { "label": "S2", "objectType": "Schuit", "attributes": { "romplengte": { "number": "7" } },
                        "roles": { "thuishaven": ["H1"] }, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [
                      { "kind": "attributeValue", "label": "H1", "attribute": "wintertal", "value": { "number": "%s" } },
                      %s ] }
                """.formatted("onveilig".equals(kenmerk) ? "" : "\"veilig\"", wintertal, lieRow);
    }

    private static Path temporary(String naam, ObjectNode ir) throws Exception {
        Path tmp = Files.createTempFile(naam, ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }
}
