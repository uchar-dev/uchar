package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.yhavinga.regelspraak.loader.IrLoader;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Objecttype;
import io.github.yhavinga.regelspraak.runtime.RegelSpraakRuntime;
import io.github.yhavinga.regelspraak.schema.FactModelGenerator;

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
 * roles, an honest ambiguity — and until this card {@code FactModelGenerator.leden} asked it FIRST for every
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
class HerhaaldTypepaarWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** FOUR feittypen relate Haven to Schuit — thuisligging (ligger, to-many), winterberging (winterschuit,
     *  to-many), keurberging (keurschuit, to-many) and vlagvoering (vlaggenschip, to-ONE) — so the by-pair
     *  search is ambiguous by construction while every written role name is unique. */
    private static final String DUBBELVERBINDING = "jutterswet-dubbelverbinding";

    // ---------------- 1. the derivation itself: per ROLE, and the by-pair search untouched ----------------

    @Test void de_bij_paar_afleiding_blijft_ambigu_en_raadt_niet() throws Exception {
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

    @Test void het_beanmodel_draagt_een_veld_per_toone_rol() throws Exception {
        RegelSpraakModel m = model(ir(DUBBELVERBINDING));
        Objecttype haven = m.objecttypeOpNaam("Haven"), schuit = m.objecttypeOpNaam("Schuit");
        // FOUR Haven references on Schuit — one per to-one Haven role — each typed, each named after its role.
        for (String rol : List.of("thuishaven", "winterhaven", "keurhaven", "vlaghaven"))
            assertTrue(FactModelGenerator.heeftReferentieVeld(m, schuit, rol, haven), rol);
        // and exactly ONE the other way: the three to-many Schuit roles store on the Schuit, the to-one
        // `vlaggenschip` is the 1:1 leg that needs a field of its own (ch. 5.7a, consequence 2).
        assertTrue(FactModelGenerator.heeftReferentieVeld(m, haven, "vlaggenschip", schuit));
        for (String rol : List.of("ligger", "winterschuit", "keurschuit"))
            assertTrue(!FactModelGenerator.heeftReferentieVeld(m, haven, rol, schuit),
                    "a to-many role's storage is the far end's field: " + rol);
    }

    // ---------------- 2. the fold over a WRITTEN element role (the old toolchain manifest's count) ------

    @Test void de_telling_over_elke_geschreven_elementrol_kiest_haar_eigen_feittype() throws Exception {
        // The old toolchain's manifest carried one fixture of exactly this shape — a count whose written
        // element role disambiguates a pair that more than one feittype connects — and both of its back-ends
        // produced the pinned number. Here, in this repo's own vocabulary: S1 and S2 lie in H1 (thuisligging)
        // while S1 ALSO winters at H2 (winterberging). Two roles, one type pair, one instance in both.
        // Counting either role over the by-pair search would answer nothing at all; counting the WRONG one
        // would answer 2 and 0 instead of 2 and 1.
        BeideTakken.groen(alleenTellingen(), JSON.readTree(tweeRollenCase("2", "0", "0", "1")),
                "liggertal H1=2 / H2=0, wintertal H1=0 / H2=1");
    }

    @Test void de_verwisselde_rol_is_een_benoemd_verschil_geen_stil_gelijkspel() throws Exception {
        // MUTATION FLANK for the role choice: the same world with the two counts' expectations SWAPPED is a
        // named mismatch in BOTH branches, so a derivation that handed both folds the same field could not
        // pass this row by accident.
        BeideTakken.rood(alleenTellingen(), JSON.readTree(tweeRollenCase("0", "1", "2", "0")),
                "wintertal", "swapping the two roles' counts must be observable");
    }

    // ---------------- 3. the §13.4.7 creation: the six outcomes of the old manifests ----------------

    @Test void feitcreatie_zonder_voorwaarde_legt_de_relatie_op_elke_ligger() throws Exception {
        // (1) the unguarded 1-hop shape: every ligger of the haven gets the new relation.
        BeideTakken.groen(creatieVariant(null, true), JSON.readTree(creatieCase("2", "40", null)),
                "wintertal H1=2 and S1 reads its winterhaven back");
    }

    @Test void feitcreatie_met_ankerkenmerk_vuurt_alleen_voor_de_veilige_haven() throws Exception {
        // (2) the anker-kenmerk guard, BOTH outcomes: with the kenmerk the haven links both its liggers,
        // without it none. The guard rides the ANKER pattern in both branches, and the window is dropped here
        // so this row is about the guard alone.
        Path m = creatieVariant(ankerKenmerk("veilig"), true);
        BeideTakken.groen(m, JSON.readTree(creatieCase("2", "40", null)),
                "veilige haven: wintertal 2 and S1 reads its winterhaven back");
        BeideTakken.groen(m, JSON.readTree(creatieCase("0", null, "onveilig")),
                "without the kenmerk the creation makes nothing — wintertal 0, ligprijs leeg");
    }

    @Test void feitcreatie_met_doelkenmerk_koppelt_alleen_de_gekeurde_liggers() throws Exception {
        // (3) the DOEL-kenmerk guard: the constraint hosts on the $doel pattern, never the anker. S1 is
        // gekeurd and S2 is not, so exactly one of H1's two liggers is linked.
        // NOT a corpus shape: ref FAULTS on a characteristic check hosted on another type (WF-100, decision
        // 2026-09-10), so this row pins the Drools lowering alone — which is precisely the old manifest's
        // fixture, kept because the backend still carries the form.
        BeideTakken.groen(creatieVariant(doelKenmerk("gekeurd"), true), JSON.readTree(creatieCase("1", "40", null)),
                "wintertal H1=1 — only the gekeurde ligger is linked");
    }

    @Test void feitcreatie_met_doelvergelijking_koppelt_alleen_de_lange_liggers() throws Exception {
        // (4) the DOEL-comparison guard, the manifest's sibling of the row above: romplengte > 8 selects S1
        // (12) and rejects S2 (7). Also not frontend-emittable — a comparison naming the doel resolves to an
        // Onopgelost node with the anker as subject — so the node is authored here and the row pins the
        // backend's own lowering, in both branches.
        BeideTakken.groen(creatieVariant(doelVergelijking("romplengte", "8"), true),
                JSON.readTree(creatieCase("1", "40", null)),
                "wintertal H1=1 — only the ligger longer than 8");
    }

    @Test void feitcreatie_binnen_en_voor_het_geldigheidsvenster() throws Exception {
        // (5) and (6), the two date rows of the old window manifest, on the golden itself: the creation is
        // `geldig vanaf 01-03-2026`, so a rekendatum inside the window creates the relation and one before it
        // creates nothing — and the count over the created role, plus the read back from the Schuit side,
        // both follow the window.
        BeideTakken.groen(ir(DUBBELVERBINDING), JSON.readTree(golden("case1-binnen-venster.json")),
                "inside: wintertal 2 / 1 / 0, ligprijs 40 / leeg / 25 / leeg");
        BeideTakken.groen(ir(DUBBELVERBINDING), JSON.readTree(golden("case2-voor-venster.json")),
                "before: wintertal 0 / 0 / 0, every ligprijs leeg");
    }

    // ---------------- 4. RUNTIME-SEMANTICS ch. 5.7a: BOTH relational directions ----------------

    @Test void de_geschreven_rol_landt_op_het_veld_van_dat_feittype() throws Exception {
        // The input path, at bean level: `winterhaven` must reach Schuit.winterhaven and leave the three
        // sister fields alone. A by-pair route could not resolve this at all; a route that resolved it
        // WRONGLY would fill one of the other three and still count something.
        Map<String, Object> beans = CaseRunner.invoerBeans(alleenTellingen(), JSON.readTree(tweeRollenCase("2", "0", "0", "1")));
        assertSame(beans.get("H1"), RegelSpraakRuntime.get(beans.get("S1"), "thuishaven"), "thuisligging -> H1");
        assertSame(beans.get("H2"), RegelSpraakRuntime.get(beans.get("S1"), "winterhaven"), "winterberging -> H2");
        assertNull(RegelSpraakRuntime.get(beans.get("S1"), "keurhaven"), "and nothing else was written");
        assertNull(RegelSpraakRuntime.get(beans.get("S1"), "vlaghaven"));
    }

    @Test void beide_spellingen_van_een_relatie_schrijven_hetzelfde_veld() throws Exception {
        // ch. 5.7a: a supplied link ESTABLISHES the relation and BOTH roles read it, so the 1:N relation's
        // ONE field must be written whichever end the case spells. Spelled from the HAVEN side the sister
        // role is resolved BY NAME (verbindendeRolViaElementnaam) — the only route on a repeated pair.
        Map<String, Object> vanSchuit = CaseRunner.invoerBeans(alleenTellingen(), JSON.readTree(spellingCase(true)));
        Map<String, Object> vanHaven = CaseRunner.invoerBeans(alleenTellingen(), JSON.readTree(spellingCase(false)));
        assertSame(vanSchuit.get("H1"), RegelSpraakRuntime.get(vanSchuit.get("S1"), "thuishaven"));
        assertSame(vanHaven.get("H1"), RegelSpraakRuntime.get(vanHaven.get("S1"), "thuishaven"),
                "the Haven-side spelling writes the SAME field on the Schuit");
    }

    @Test void de_een_op_een_relatie_krijgt_beide_velden_geschreven() throws Exception {
        // ch. 5.7a consequence 2: a 1:1 relation owns TWO fields — one per direction — so the case-input
        // layer must fill the second one itself, on a repeated pair as anywhere else. `vlagvoering` is that
        // shape here. RED FLANK: with the mirror write removed the far side reads a spuriously empty
        // relation, which is the reading ch. 5.7a rejects.
        Map<String, Object> vanHaven = CaseRunner.invoerBeans(alleenTellingen(), JSON.readTree(vlagCase(true)));
        assertSame(vanHaven.get("S1"), RegelSpraakRuntime.get(vanHaven.get("H1"), "vlaggenschip"));
        assertSame(vanHaven.get("H1"), RegelSpraakRuntime.get(vanHaven.get("S1"), "vlaghaven"),
                "the Haven-side spelling ALSO writes the Schuit's own leg");
        Map<String, Object> vanSchuit = CaseRunner.invoerBeans(alleenTellingen(), JSON.readTree(vlagCase(false)));
        assertSame(vanSchuit.get("H1"), RegelSpraakRuntime.get(vanSchuit.get("S1"), "vlaghaven"));
        assertSame(vanSchuit.get("S1"), RegelSpraakRuntime.get(vanSchuit.get("H1"), "vlaggenschip"),
                "and the Schuit-side spelling writes the Haven's");
    }

    @Test void de_aangemaakte_koppeling_staat_op_het_veld_van_haar_eigen_feittype() throws Exception {
        // The creation path, at bean level after the fire: the new relation lands on `winterhaven` and the
        // relation it JOINED over (`thuishaven`) is untouched. The two are different fields over the same
        // type pair, which is the whole construct.
        CaseRunner.NaFire na = CaseRunner.naFire(ir(DUBBELVERBINDING), JSON.readTree(golden("case1-binnen-venster.json")));
        assertSame(na.invoer().get("H1"), RegelSpraakRuntime.get(na.invoer().get("S1"), "winterhaven"),
                "the creation wrote the winterberging leg");
        assertSame(na.invoer().get("H1"), RegelSpraakRuntime.get(na.invoer().get("S1"), "thuishaven"),
                "and the joined-over thuisligging leg still says what the case said");
        assertNull(RegelSpraakRuntime.get(na.invoer().get("S4"), "winterhaven"),
                "H3 is not veilig, so its ligger got nothing");
    }

    @Test void de_aangemaakte_koppeling_wordt_aan_beide_kanten_gelezen() throws Exception {
        // ONE stored field, read from BOTH ends: the Haven counts its winterschuiten (the far side of the
        // field) and the Schuit reads the kadegeld of its winterhaven (the near side). A creation that wrote
        // a field only one reader reaches would still satisfy one of the two.
        // MUTATION FLANK: a creation that set the role of ANOTHER feittype over the same pair moves both
        // readings at once, and the flank names it. Until P2 (2026-09-22) the flank twisted zetVeld; since then
        // the written field is derived from the creation's binding, so the flank twists the BINDING — a zetVeld
        // twist alone now changes nothing, which BonenveldWitnessTest pins.
        BeideTakken.groen(ir(DUBBELVERBINDING), JSON.readTree(golden("case1-binnen-venster.json")),
                "both readings of the created link agree with ref and clingo");
        String verdict = BeideTakken.rood(creatieVerlegd("keurberging", "keurhaven", "keurschuit"), JSON.readTree(golden("case1-binnen-venster.json")),
                "wintertal", "writing the sister feittype's field must be observable");
        assertTrue(verdict.contains("ligprijs"), "and the Schuit-side reading moves with it: " + verdict);
    }

    // ---------------- 5. what STAYS refused, each with its own sentence ----------------

    @Test void een_niet_unieke_rolnaam_leidt_geen_veld_af() throws Exception {
        // Two feittypen whose Haven role carries the SAME name: the written name selects nothing, so no field
        // is derived for it and the fold that joins over it refuses — per NAME, not per pair. The sister
        // roles of the other feittypen keep their fields; only the shared name loses.
        // BOTH branches, one sentence: a producer reading the gate report gets one diagnosis whichever branch
        // produced it, which is the whole point of the shared reason function.
        BeideTakken.geweigerd(rolnaamBotsing(), JSON.readTree(tweeRollenCase("2", "0", "0", "1")),
                "het beanmodel leidt voor de rol 'thuishaven' op Schuit geen referentieveld naar Haven af",
                "a role name two feittypen share derives nothing");
    }

    @Test void naamdeling_met_een_gedeclareerd_lid_blijft_een_eigen_weigering() throws Exception {
        // leden()'s dedup (attribuut > kenmerk > referentie) displaces the reference, so the field EXISTS as a
        // BigDecimal. That is a different failure from an underivable role and must keep its own sentence —
        // blaming ambiguity here would accuse the document of something it is not guilty of.
        BeideTakken.geweigerd(naamdelendeSchuit(), JSON.readTree(tweeRollenCase("2", "0", "0", "1")),
                "de rol 'thuishaven' op Schuit deelt haar naam met een gedeclareerd lid",
                "a declared member of the same name displaces the reference");
    }

    @Test void een_meervoudige_rol_zonder_opslagdragende_overzijde_blijft_geweigerd() throws Exception {
        // BOTH roles of one feittype to-many: a to-many role's storage is the far end's field, and here that
        // end is to-many too, so the model derives nothing at EITHER end. Its own sentence, naming the
        // CARDINALITY rather than a derivation that in fact succeeded.
        //
        // Dual since 2026-09-18. Measured before that: the exec branch named the cardinality while the DRL-text
        // branch answered "het beanmodel leidt voor de rol 'thuishaven' op Schuit geen referentieveld naar Haven
        // af" — one failure, two diagnoses, the second of them wrong about WHY. The cardinality half now lives
        // beside the shared reason function (FactModelGenerator.geenJoinveldReden) and both branches restate it
        // at the fold-join seam; this row is what keeps them together.
        BeideTakken.geweigerd(beideZijdenMeervoudig(), JSON.readTree(tweeRollenCase("2", "0", "0", "1")),
                "de rol 'thuishaven' op Schuit is meervoudig, dus haar opslag is het veld aan de overzijde,"
                + " en ook daar leidt het beanmodel er geen af (beide zijden meervoudig)",
                "both sides to-many: no bean can hold the relation");
    }

    @Test void de_overzijde_die_wel_een_veld_draagt_krijgt_niet_de_beide_zijden_zin() throws Exception {
        // The half of that sentence the shared derivation must NOT over-claim. With the element role to-ONE and
        // the subject's role to-many, the join field is missing HERE but present at the far end — one bean does
        // hold the relation. Saying "beide zijden meervoudig" there would diagnose a cardinality defect the
        // document does not have; measured on the corpus, `anchor/jutterswet-gemeenteberging` is exactly this
        // shape, and the first cut of the shared sentence said it of that golden.
        CaseRunner.Excluded drl = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(elementrolToOne(), JSON.readTree(tweeRollenCase("2", "0", "0", "1")), true));
        assertTrue(drl.getMessage().contains("is meervoudig, dus haar opslag is het veld aan de overzijde"), drl.getMessage());
        assertTrue(!drl.getMessage().contains("beide zijden meervoudig"),
                "the far end DOES derive a field, so the sentence stops there: " + drl.getMessage());
        // The exec branch refuses the same document EARLIER, on its own conservative arm (a to-one element role
        // whose sister is to-many), which this card deliberately leaves standing. Pinned so the asymmetry is a
        // recorded decision rather than an unnoticed drift.
        CaseRunner.Excluded exec = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(elementrolToOne(), JSON.readTree(tweeRollenCase("2", "0", "0", "1")), false));
        assertTrue(exec.getMessage().contains("Deze weigering staat CONSERVATIEF"), exec.getMessage());
    }

    @Test void een_consument_zonder_bereik_laadt_niet() throws Exception {
        // The LOUD-AMBIGUITY row. A SOM's argument is an ATTRIBUTE name, never a role, so a fold that names no
        // range AT ALL has nothing but the type pair — which two fact types make ambiguous here — and no branch may
        // pick a winner from it. Since L2 of the v1 wire migration the range is required on this fold (IR-SPEC
        // ch. 4.3): the document is refused at load, by name, before any branch could guess. The bound flank is
        // in BronRolVouwWitnessTest, where the same fold PLAYS in both branches.
        for (boolean viaDrl : new boolean[] { false, true }) {
            IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class,
                    () -> CaseRunner.speelCase(somOverLiggers(), JSON.readTree(tweeRollenCase("2", "0", "0", "1")), viaDrl));
            assertTrue(e.getMessage().contains("missing field 'bronRol'"), e.getMessage());
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
    private static Path alleenTellingen() throws Exception {
        ObjectNode ir = lees(DUBBELVERBINDING);
        IrPad.rules(ir).putArray("feitCreaties");
        houdRegels(ir, "liggertal van een haven", "wintertal van een haven");
        return tijdelijk("dv-tellingen", ir);
    }

    /** The golden reduced to ONE creation (winterberging) plus the count over the created role and the read
     *  back from the Schuit side, with the given voorwaarde (null = unguarded) and, when {@code altijd}, the
     *  §4.2 window dropped so the row is about the guard alone. */
    private static Path creatieVariant(ObjectNode voorwaarde, boolean altijd) throws Exception {
        ObjectNode ir = lees(DUBBELVERBINDING);
        houdCreaties(ir, "winterberging door de haven");
        houdRegels(ir, "wintertal van een haven", "ligprijs van een schuit");
        ObjectNode fc = (ObjectNode) IrPad.rulesLijst(ir, "feitCreaties").get(0);
        if (voorwaarde == null) fc.putNull("voorwaarde"); else fc.set("voorwaarde", voorwaarde);
        if (altijd) fc.set("geldigheid", JSON.createObjectNode()
                .put("node", "Geldigheid").putNull("vanaf").putNull("tot"));
        // the ligprijs rule carries its own doel-kenmerk gate in the golden; drop it so this row reads the
        // created link for every Schuit and the creation's guard is the only selector.
        for (JsonNode r : IrPad.rulesLijst(ir, "resolvedRegels"))
            if ("ligprijs van een schuit".equals(r.get("naam").asText())) ((ObjectNode) r).putNull("voorwaarde");
        return tijdelijk("dv-creatie", ir);
    }

    /** The golden with the creation bound to ANOTHER feittype over the same pair — the mutation the both-sides
     *  row must catch. Since P2 the written field follows from that binding; twisting {@code zetVeld} alone
     *  changes nothing (BonenveldWitnessTest). */
    private static Path creatieVerlegd(String feittype, String ankerRol, String doelRol) throws Exception {
        ObjectNode ir = lees(DUBBELVERBINDING);
        for (JsonNode f : IrPad.rulesLijst(ir, "feitCreaties"))
            if ("winterberging door de haven".equals(f.get("naam").asText()))
                ((ObjectNode) f).put("feittype", feittype).put("ankerRol", ankerRol).put("doelRol", doelRol);
        return tijdelijk("dv-creatie-verlegd", ir);
    }

    /** The counts-only model with winterberging's Haven role RENAMED to `thuishaven`, so two feittypen reach
     *  Haven from Schuit under one name and neither derives a field. */
    private static Path rolnaamBotsing() throws Exception {
        ObjectNode ir = lees(DUBBELVERBINDING);
        IrPad.rules(ir).putArray("feitCreaties");
        houdRegels(ir, "liggertal van een haven", "wintertal van een haven");
        for (JsonNode r : IrPad.declaratie(ir, "factTypes", "winterberging").get("rollen"))
            if ("winterhaven".equals(r.get("naam").asText())) ((ObjectNode) r).put("naam", "thuishaven");
        return tijdelijk("dv-rolnaambotsing", ir);
    }

    /** The counts-only model with a Numeriek attribute SHARING the role name `thuishaven` on Schuit. */
    private static Path naamdelendeSchuit() throws Exception {
        ObjectNode ir = lees(DUBBELVERBINDING);
        IrPad.rules(ir).putArray("feitCreaties");
        houdRegels(ir, "liggertal van een haven", "wintertal van een haven");
        IrPad.addAttribute(IrPad.objectType(ir, "Schuit"), "thuishaven", numeriek());
        return tijdelijk("dv-naamdeling", ir);
    }

    /** The counts-only model with thuisligging's cardinalities SWAPPED — the element role `ligger` to-one and
     *  the subject's `thuishaven` to-many — so the join field is missing on the Schuit but present on the Haven. */
    private static Path elementrolToOne() throws Exception {
        ObjectNode ir = lees(DUBBELVERBINDING);
        IrPad.rules(ir).putArray("feitCreaties");
        houdRegels(ir, "liggertal van een haven", "wintertal van een haven");
        for (JsonNode r : IrPad.declaratie(ir, "factTypes", "thuisligging").get("rollen"))
            ((ObjectNode) r).put("meervoudig", "thuishaven".equals(r.get("naam").asText()));
        return tijdelijk("dv-elementrol-toone", ir);
    }

    /** The counts-only model with BOTH roles of thuisligging to-many — no field at either end. */
    private static Path beideZijdenMeervoudig() throws Exception {
        ObjectNode ir = lees(DUBBELVERBINDING);
        IrPad.rules(ir).putArray("feitCreaties");
        houdRegels(ir, "liggertal van een haven", "wintertal van een haven");
        for (JsonNode r : IrPad.declaratie(ir, "factTypes", "thuisligging").get("rollen"))
            ((ObjectNode) r).put("meervoudig", true);
        return tijdelijk("dv-beide-meervoudig", ir);
    }

    /** The counts-only model with the liggertal fold turned into a SOM over the liggers' romplengte — whose
     *  argument is an ATTRIBUTE name — and its {@code bronRol} REMOVED, so no route to the range remains but
     *  the by-pair search. (With the binding in place the same fold is lowerable and plays; that is
     *  BronRolVouwWitnessTest's business.) */
    private static Path somOverLiggers() throws Exception {
        ObjectNode ir = lees(DUBBELVERBINDING);
        IrPad.rules(ir).putArray("feitCreaties");
        houdRegels(ir, "liggertal van een haven");
        ObjectNode fold = (ObjectNode) IrPad.rulesLijst(ir, "resolvedRegels").get(0).get("waarde");
        fold.remove("bronRol");
        fold.put("kind", "SOM").put("bron", "de som van de romplengte van alle liggers van de Haven");
        ObjectNode arg = (ObjectNode) fold.get("argumenten").get(0);
        arg.put("attribuut", "romplengte").put("objecttype", "Schuit")
           .put("bron", "de romplengte van alle liggers van de Haven");
        return tijdelijk("dv-som", ir);
    }

    private static void houdRegels(ObjectNode ir, String... namen) {
        List<String> houd = List.of(namen);
        ArrayNode uit = JSON.createArrayNode();
        for (JsonNode r : IrPad.rulesLijst(ir, "resolvedRegels")) if (houd.contains(r.get("naam").asText())) uit.add(r);
        IrPad.rules(ir).set("resolvedRegels", uit);
    }

    private static void houdCreaties(ObjectNode ir, String naam) {
        ArrayNode uit = JSON.createArrayNode();
        for (JsonNode f : IrPad.rulesLijst(ir, "feitCreaties")) if (naam.equals(f.get("naam").asText())) uit.add(f);
        IrPad.rules(ir).set("feitCreaties", uit);
    }

    /** A §8.1.7 characteristic check on the ANKER — no eigenaar, so it hosts on the anker pattern. */
    private static ObjectNode ankerKenmerk(String kenmerk) {
        return JSON.createObjectNode().put("node", "Kenmerk").put("kenmerk", kenmerk)
                .put("negatie", false).putNull("eigenaar")
                .putNull("periode").put("bron", "de Haven " + kenmerk + " is");
    }

    /** A §8.1.7 characteristic check HOSTED on the doel (the eigenaar field, WF-100). */
    private static ObjectNode doelKenmerk(String kenmerk) {
        return JSON.createObjectNode().put("node", "Kenmerk").put("kenmerk", kenmerk)
                .put("negatie", false).put("eigenaar", "Schuit")
                .putNull("periode").put("bron", "de ligger " + kenmerk + " is");
    }

    /** A comparison on an attribute of the DOEL — the shape the frontend leaves unresolved and the transpilers
     *  host on the $doel pattern. */
    private static ObjectNode doelVergelijking(String attribuut, String grens) {
        ObjectNode links = JSON.createObjectNode();
        links.put("node", "AttribuutRef").put("attribuut", attribuut).put("objecttype", "Schuit");
        links.put("bron", "de " + attribuut + " van de ligger");
        ObjectNode rechts = JSON.createObjectNode();
        rechts.put("node", "Literal").put("waarde", grens);
        rechts.set("type", numeriek());
        rechts.put("bron", grens);
        ObjectNode v = JSON.createObjectNode();
        v.put("node", "Vergelijking").put("op", "GROTER");
        v.set("links", links);
        v.set("rechts", rechts);
        v.putNull("periode").put("bron", "de " + attribuut + " van de ligger groter is dan " + grens);
        return v;
    }

    private static ObjectNode numeriek() {
        return JSON.createObjectNode().put("node", "Numeriek").put("geheel", true)
                .put("decimalen", 0).put("teken", "NIET_NEGATIEF").putNull("eenheid");
    }

    /** Two havens and two schuiten with BOTH roles of the repeated pair written on S1: it lies in H1
     *  (thuisligging) and winters at H2 (winterberging). The four counts are the pinned cells. */
    private static String tweeRollenCase(String l1, String l2, String w1, String w2) {
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
    private static String spellingCase(boolean vanSchuit) {
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
                """.formatted(vanSchuit ? "" : "\"liggers\": [\"S1\"]", vanSchuit ? "\"thuishaven\": [\"H1\"]" : "");
    }

    /** The 1:1 vlagvoering, spelled from the HAVEN side (`vlaggenschip`) or the SCHUIT side (`vlaghaven`). */
    private static String vlagCase(boolean vanHaven) {
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
                """.formatted(vanHaven ? "\"vlaggenschip\": [\"S1\"]" : "", vanHaven ? "" : "\"vlaghaven\": [\"H1\"]");
    }

    /** One haven with two liggers — S1 gekeurd and 12 long, S2 not and 7 — with the created role's count and,
     *  when {@code ligprijs} is given, the read back from S1's own side. {@code kenmerk} null makes H1 veilig. */
    private static String creatieCase(String wintertal, String ligprijs, String kenmerk) {
        String ligRij = ligprijs == null
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
                """.formatted("onveilig".equals(kenmerk) ? "" : "\"veilig\"", wintertal, ligRij);
    }

    private static Path tijdelijk(String naam, ObjectNode ir) throws Exception {
        Path tmp = Files.createTempFile(naam, ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }
}
