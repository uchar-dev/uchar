package io.github.yhavinga.regelspraak.transpiler.kern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.yhavinga.regelspraak.engine.BeanCompiler;
import io.github.yhavinga.regelspraak.ir.Bron;
import io.github.yhavinga.regelspraak.ir.Expr;
import io.github.yhavinga.regelspraak.loader.IrLoader;
import io.github.yhavinga.regelspraak.model.Datatype;
import io.github.yhavinga.regelspraak.model.Eenheid;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Objecttype;
import io.github.yhavinga.regelspraak.schema.FactModelGenerator;
import io.github.yhavinga.regelspraak.transpiler.DrlTranspiler;
import io.github.yhavinga.regelspraak.transpiler.model.ExecModelTranspiler;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The MIRROR of {@code scripts/check-reftype-derivation.sh}, one direction over.
 *
 * <p>That harness twists an ANNOTATION and holds the DECLARATIONS byte-identical: it asks whether a consumer
 * reproduces the outcome when the wire repetition lies. This witness twists a DECLARATION and holds the
 * annotations byte-identical: it asks whether a consumer NOTICES when the two contradict each other so badly
 * that no conversion factor exists. Measured before the fence below existed (2026-09-09, on a copy of
 * {@code corpus/golden/jutterswet-schouwrooster} whose declaration of one addend moved to {@code uur}, factor
 * 3600 to {@code seconde}): the verifier failed WF-091, {@code ref} faulted "incompatible units: schouwdagen
 * vs uur", clingo faulted ahead of translation, and this backend replayed both cases against the untouched
 * golden's pinned values. Not a wire read — a copy with a CONVERTIBLE sister declaration (factor 0,5) moved
 * the outcome from 15 to 13,5, so the declaration WAS consulted. What was missing is the consequence: every
 * restatement helper answered "no conversion is due" and "no conversion EXISTS" with the same identity.
 *
 * <p><b>Both directions are asserted here, and the second is what keeps the harness green.</b> The fence keys
 * on the two DERIVED units, never on annotation-against-declaration disagreement — so a lying annotation over
 * an honest declaration still lowers, byte-identically, exactly as the harness demands. A fence that fired on
 * disagreement would refuse every one of its twists.
 */
class EenheidsconflictWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** A concrete unit from no system the corpus knows: a canonical of its own, so it inter-converts with
     *  nothing a golden declares, and a factor, so it is not the WF-092 factorless form. */
    private static final String VREEMD = """
            {"node":"Eenheid","afkorting":"uchar-vreemd","stelsel":null,
             "canoniek":"uchar-vreemd-canoniek","factorNaarCanoniek":3600,"noemer":null}""";

    /** A plain denominator, to turn a declared unit into a RATE with the very same numerator. */
    private static final String NOEMER = """
            {"node":"Eenheid","afkorting":"mnd","stelsel":null,"canoniek":"mnd","factorNaarCanoniek":1,"noemer":null}""";

    // ---------------- the guard itself: what it refuses and what it lets through ----------------

    private static final Eenheid KG = new Eenheid("kg", "Gewicht", "kg", BigDecimal.ONE);
    private static final Eenheid TON = new Eenheid("t", "Gewicht", "kg", new BigDecimal("1000"));
    private static final Eenheid EUR = new Eenheid("EUR", "Geld", "EUR", BigDecimal.ONE);
    private static final Eenheid JR = new Eenheid("jr", "Tijd", "mnd", new BigDecimal("12"));

    @Test void twee_concrete_eenheden_zonder_gedeelde_canoniek_zijn_een_fout() {
        Eenheidsconflict f = assertThrows(Eenheidsconflict.class,
                () -> Eenheidsconflict.eisHerschaalbaar(JR, KG, "een §4-waardeherschaling"));
        assertSame(JR, f.van());
        assertSame(KG, f.naar());
        assertEquals("een §4-waardeherschaling", f.plek());
        // the message must name BOTH sides, the way ref's "incompatible units: schouwdagen vs uur" does:
        // a fault that names only the target leaves the reader without the half that moved.
        assertTrue(f.getMessage().contains("'jr'") && f.getMessage().contains("'kg'"), f.getMessage());
        assertTrue(f.getMessage().contains("een §4-waardeherschaling"), "de plek hoort in de melding");
    }

    @Test void een_omwisselbaar_paar_gaat_ongehinderd_door() {
        // the ordinary cross-unit restatement: kg <-> t share a canonical, so a factor exists and the site
        // must compute it. Both directions, and the identity pair.
        Eenheidsconflict.eisHerschaalbaar(KG, TON, "witness");
        Eenheidsconflict.eisHerschaalbaar(TON, KG, "witness");
        Eenheidsconflict.eisHerschaalbaar(KG, KG, "witness");
    }

    @Test void een_eenheidloze_zijde_is_geen_conflict() {
        // ch. 2.6: a unitless side adopts the other, value unchanged — ref does the same and no factor is due.
        Eenheidsconflict.eisHerschaalbaar(null, KG, "witness");
        Eenheidsconflict.eisHerschaalbaar(KG, null, "witness");
        Eenheidsconflict.eisHerschaalbaar(null, null, "witness");
    }

    @Test void een_AFGELEID_opaak_product_blijft_de_behoudende_no_op() {
        // RUNTIME-SEMANTICS ch. 2.10 records this backend's stance on "€·kg": no canonical, so no factor is
        // invented and the value stands in the unit it was computed in. Faulting here would make that recorded
        // sentence false. The unit must come from the DERIVATION — that is where the provenance mark is set.
        Eenheid opaak = afgeleidProduct();
        assertNull(opaak.canoniek(), "een opaak product draagt per constructie geen canoniek");
        Eenheidsconflict.eisHerschaalbaar(opaak, EUR, "witness");
        Eenheidsconflict.eisHerschaalbaar(EUR, opaak, "witness");
    }

    @Test void een_op_de_wire_gedeclareerd_naamgenoot_krijgt_de_vrijstelling_NIET() {
        // The hole a name test leaves: a wire-declared unit can carry exactly the four fields Eenheid.product
        // produces — no canonical, no factor, no denominator, a "·" in the abbreviation — without ever having
        // been derived. ref faults on such a document; so must this. Value-equal to the derived one, and still
        // treated differently, which is the whole point: provenance is not a property of the value.
        Eenheid afgeleid = afgeleidProduct();
        Eenheid naamgenoot = new Eenheid(afgeleid.afkorting(), afgeleid.stelsel(), null, null, null);
        assertEquals(afgeleid, naamgenoot, "de twee zijn waarde-gelijk — alleen hun herkomst verschilt");
        assertThrows(Eenheidsconflict.class, () -> Eenheidsconflict.eisHerschaalbaar(naamgenoot, EUR, "witness"));
        assertThrows(Eenheidsconflict.class, () -> Eenheidsconflict.eisHerschaalbaar(EUR, naamgenoot, "witness"));
    }

    @Test void twee_VERSCHILLENDE_afgeleide_producten_stellen_elkaar_ook_vrij() {
        // The exemption is carried by ONE side, so €·kg against €·m passes too — not only a product against a
        // concrete unit. Same ch. 2.10 stance (neither side has a canonical, so no factor is invented either
        // way), and the cost is a diagnosis rather than a wrong number. Pinned so the breadth is a statement.
        Eenheid eurKg = afgeleidProduct();
        Eenheid eurM = afgeleidProduct(new Eenheid("m", "Lengte", "m", BigDecimal.ONE));
        assertNotEquals(eurKg, eurM, "twee verschillende producten, allebei zonder canoniek");
        Eenheidsconflict.eisHerschaalbaar(eurKg, eurM, "witness");
        Eenheidsconflict.eisHerschaalbaar(eurM, eurKg, "witness");
    }

    @Test void de_herkomsttabel_groeit_per_afleiding_niet_per_document() {
        // The growth law the class doc states, as a checked claim: Eenheid.product allocates a fresh instance
        // every time, so deriving the SAME product twice adds TWO entries. The table is monotone for the JVM's
        // lifetime and never cleared — a bounded strong-reference leak, recorded rather than glossed.
        int voor = EffectieveEenheid.aantalAfgeleideProducten();
        Eenheid een = afgeleidProduct();
        Eenheid twee = afgeleidProduct();
        assertEquals(voor + 2, EffectieveEenheid.aantalAfgeleideProducten(),
                "elke maalEenheid-evaluatie voegt een eigen entry toe");
        assertEquals(een, twee, "de twee zijn waarde-gelijk");
        assertNotSame(een, twee, "en toch verschillende instanties — daarom groeit de tabel");
    }

    @Test void een_tarief_tegen_zijn_eigen_teller_is_een_fout() {
        // €/jr against € was EXEMPT until 2026-09-09, to keep two §7.1 goldens green — and that exemption was
        // place-blind, so an additive join, a write boundary and a comparison between a rate and its numerator
        // all stayed silent while ref and clingo faulted. The repair is the derivation (see the §7.1 test
        // below), not an exemption, so this pair faults again everywhere. Both directions.
        Eenheid tarief = Eenheid.samengesteld(EUR, JR);
        assertThrows(Eenheidsconflict.class, () -> Eenheidsconflict.eisHerschaalbaar(tarief, EUR, "witness"));
        assertThrows(Eenheidsconflict.class, () -> Eenheidsconflict.eisHerschaalbaar(EUR, tarief, "witness"));
        assertThrows(Eenheidsconflict.class, () -> Eenheidsconflict.eisHerschaalbaar(tarief, KG, "witness"));
    }

    // ---------------- the §7.1 derivation that replaces the exemption ----------------

    @Test void een_TOTAAL_over_een_tijdlijn_tarief_levert_de_teller() {
        // §7.1 / ch. 3.6: the operand's own timeline collapses first, each segment contributing value x whole
        // grid units of its span, so a €/mnd rate is multiplied by a COUNT of months and what survives is €.
        // Without this the fold would answer €/mnd for a result that is €, and every restatement downstream
        // would be handed a pair with no factor — which is exactly what the two corpus goldens did.
        Eenheid tarief = Eenheid.samengesteld(EUR, JR);
        Objecttype metTijdlijn = new Objecttype("Vondst");
        metTijdlijn.attributen.put("tarief", num(tarief));
        metTijdlijn.attribuutGranulariteit.put("tarief", RegelSpraakModel.Granulariteit.MAAND);
        assertEquals(EUR, kaal().eenheidVan(totaalOver(metTijdlijn)), "de noemer is door de vouwing verbruikt");

        // and NOT when the operand carries no timeline: a plain set TOTAAL of rates is still a rate. The same
        // corpus golden carries both readings side by side, which is why the rule keys on the declaration.
        Objecttype zonderTijdlijn = new Objecttype("Vondst");
        zonderTijdlijn.attributen.put("tarief", num(tarief));
        assertEquals(tarief, kaal().eenheidVan(totaalOver(zonderTijdlijn)), "zonder tijdlijn blijft het een tarief");
    }

    @Test void een_SOM_over_een_tijdlijn_tarief_levert_ook_de_teller() {
        // SOM carries the per-element timeline fold exactly as TOTAAL does — both consumers say so: the
        // exec-model instance-fold guard admits those two kinds and skips a temporal operand for every other,
        // and ref faults on the others in as many words. Restricting the collapse to TOTAAL left SOM answering
        // the rate for an already-folded value: measured 576 for 48 into a €/jr doel (the mnd->jr factor applied
        // twice over), where ref faults. Same declaration, same shape, only the operator differs.
        Eenheid tarief = Eenheid.samengesteld(EUR, JR);
        Objecttype ot = new Objecttype("Vondst");
        ot.attributen.put("tarief", num(tarief));
        ot.attribuutGranulariteit.put("tarief", RegelSpraakModel.Granulariteit.MAAND);
        Expr.AttribuutRef ref = new Expr.AttribuutRef("tarief", ot, "alle vondsten", null, num(tarief), bron());
        Expr.Functie som = new Expr.Functie(Expr.FunctieKind.SOM, List.of(ref), ot, null, null, num(tarief), bron());
        assertEquals(EUR, kaal().eenheidVan(som), "de noemer is door de vouwing verbruikt");
    }

    @Test void een_MINIMUM_over_een_tijdlijn_tarief_houdt_het_tarief() {
        // A selecting fold sums nothing, so no denominator is consumed and none may be dropped. Measured over
        // the corpus: every fold whose operand is timeline-declared is a TOTAAL, so this is the boundary of a
        // rule that was scoped by measurement rather than by guess.
        Eenheid tarief = Eenheid.samengesteld(EUR, JR);
        Objecttype ot = new Objecttype("Vondst");
        ot.attributen.put("tarief", num(tarief));
        ot.attribuutGranulariteit.put("tarief", RegelSpraakModel.Granulariteit.MAAND);
        Expr.AttribuutRef ref = new Expr.AttribuutRef("tarief", ot, "alle vondsten", null, num(tarief), bron());
        Expr.Functie f = new Expr.Functie(Expr.FunctieKind.MINIMUM, List.of(ref), ot, null, null, num(tarief), bron());
        assertEquals(tarief, kaal().eenheidVan(f));
    }

    // ---------------- authored-model helpers ----------------

    private static Datatype num(Eenheid e) {
        return new Datatype.Numeriek(false, 2, Datatype.Teken.ONBEPAALD, e);
    }

    private static Bron bron() { return Bron.synthetisch("witness"); }

    private static EffectieveEenheid kaal() { return new EffectieveEenheid(List.of()); }

    /** "het totaal van het tarief van alle vondsten" over {@code ot} — the §7.1 fold shape. */
    private static Expr.Functie totaalOver(Objecttype ot) {
        Datatype t = ot.attributen.get("tarief");
        Expr.AttribuutRef ref = new Expr.AttribuutRef("tarief", ot, "alle vondsten", null, t, bron());
        return new Expr.Functie(Expr.FunctieKind.TOTAAL, List.of(ref), ot, null, null, t, bron());
    }

    /** An opaque product carrying this backend's own provenance mark: obtained through the DERIVATION (a §4.4
     *  MAAL of two unit-bearing reads), never by calling the constructor, because the mark is set where the
     *  product is built and a value-equal copy does not carry it. */
    private static Eenheid afgeleidProduct() { return afgeleidProduct(KG); }

    /** As above, with the second factor chosen — so two DISTINCT derived products can be put side by side. */
    private static Eenheid afgeleidProduct(Eenheid tweede) {
        Objecttype ot = new Objecttype("Vracht");
        ot.attributen.put("prijs", num(EUR));
        ot.attributen.put("factor", num(tweede));
        Expr.AttribuutRef prijs = new Expr.AttribuutRef("prijs", ot, "een vracht", null, num(EUR), bron());
        Expr.AttribuutRef ander = new Expr.AttribuutRef("factor", ot, "een vracht", null, num(tweede), bron());
        Eenheid uit = kaal().eenheidVan(new Expr.Binair(Expr.BinOp.MAAL, prijs, ander, num(EUR), bron()));
        assertEquals(Eenheid.product(EUR, tweede), uit, "de afleiding levert het §4.4 opake product");
        return uit;
    }

    // ---------------- the mutant, rebuilt from whatever goldens carry its shape ----------------

    /**
     * FOUR declaration twists, one per restatement position the fence guards, each applied to every golden that
     * carries the shape: leave every annotation untouched, move ONE DECLARATION, and demand that BOTH transpiler
     * branches fault instead of lowering. Each twist must have been probed at least once in each branch.
     *
     * <p>The last three are the {@code rate against its own numerator} shape, which was EXEMPT here until
     * 2026-09-09 at all eight fence positions at once. They are pinned per position because the hole was a
     * place-blindness, and a pin at one position would not have seen it at the other two.
     */
    @Test void een_tegensprekende_declaratie_wordt_door_geen_van_beide_takken_stil_doorgespeeld() throws Exception {
        Map<String, Verdraaiing> twists = new java.util.LinkedHashMap<>();
        twists.put("additieve operand -> vreemde eenheid", EenheidsconflictWitnessTest::metVerdraaideDeclaratie);
        twists.put("additieve operand -> tarief met dezelfde teller", EenheidsconflictWitnessTest::metTariefOpAddend);
        twists.put("schrijfgrens -> doel als tarief", EenheidsconflictWitnessTest::metTariefOpDoel);
        twists.put("§5.1 vergelijking -> parameter als tarief", EenheidsconflictWitnessTest::metTariefOpParameter);

        List<String> stil = new ArrayList<>();
        List<String> ongeprobeerd = new ArrayList<>();
        for (Map.Entry<String, Verdraaiing> t : twists.entrySet()) {
            int execGezien = 0, drlGezien = 0;
            for (Path ir : goldens()) {
                JsonNode eerlijk = JSON.readTree(Files.readString(ir));
                JsonNode liegend = t.getValue().pas(eerlijk);
                if (liegend == null) continue;                   // this golden does not carry the shape
                if (lowertExec(eerlijk)) {
                    execGezien++;
                    if (!(vangExec(liegend) instanceof Eenheidsconflict))
                        stil.add(t.getKey() + " | " + naam(ir) + " (exec-model): " + omschrijf(vangExec(liegend)));
                }
                if (lowertDrl(eerlijk)) {
                    drlGezien++;
                    if (!(vangDrl(liegend) instanceof Eenheidsconflict))
                        stil.add(t.getKey() + " | " + naam(ir) + " (DRL-tekst): " + omschrijf(vangDrl(liegend)));
                }
            }
            // never silently green: a twist no golden carries would make its whole row vacuous, so an unprobed
            // twist is itself the failure — per branch, because the two lower different subsets of the corpus.
            if (execGezien == 0) ongeprobeerd.add(t.getKey() + " (exec-model)");
            if (drlGezien == 0) ongeprobeerd.add(t.getKey() + " (DRL-tekst)");
        }
        assertEquals(List.of(), ongeprobeerd, "een verdraaiing vond geen enkele golden om op te staan");
        assertEquals(List.of(), stil, "een verdraaide declaratie werd stil gelowered in plaats van gemeld");
    }

    /** One declaration twist: returns the mutated document, or null when the golden does not carry the shape. */
    private interface Verdraaiing {
        JsonNode pas(JsonNode ir) throws Exception;
    }

    /**
     * The harness's own direction, at the same position: move only the ANNOTATION of that same reference to the
     * same foreign unit. The declaration is the source, so nothing may fault and the DRL text may not move by a
     * character — this is the property that keeps {@code check-reftype-derivation.sh} green under the fence.
     */
    @Test void een_liegende_annotatie_boven_een_kloppende_declaratie_verandert_niets() throws Exception {
        int gezien = 0;
        List<String> bewogen = new ArrayList<>();
        for (Path ir : goldens()) {
            JsonNode eerlijk = JSON.readTree(Files.readString(ir));
            JsonNode liegend = metVerdraaideAnnotatie(eerlijk);
            if (liegend == null || !lowertDrl(eerlijk)) continue;
            gezien++;
            Throwable t = vangDrl(liegend);
            if (t != null) { bewogen.add(naam(ir) + " faalde: " + omschrijf(t)); continue; }
            if (!drl(eerlijk).equals(drl(liegend))) bewogen.add(naam(ir) + ": de DRL-tekst bewoog");
            if (lowertExec(eerlijk) && vangExec(liegend) != null)
                bewogen.add(naam(ir) + " (exec-model) faalde: " + omschrijf(vangExec(liegend)));
        }
        assertTrue(gezien > 0, "geen enkele golden bood een verdraaibare annotatie op deze positie");
        assertEquals(List.of(), bewogen, "de annotatie is niet de bron: een leugen daar mag niets bewegen");
    }

    // ---------------- twisting ----------------

    /** A copy whose right addend's DECLARED unit is foreign, or null when this golden carries no such shape. */
    private static JsonNode metVerdraaideDeclaratie(JsonNode ir) throws Exception {
        JsonNode kopie = ir.deepCopy();
        ObjectNode ref = additieveRechterRef(kopie);
        if (ref == null) return null;
        ObjectNode decl = declaratieVan(kopie, ref);
        decl.set("eenheid", JSON.readTree(VREEMD));
        return kopie;
    }

    /** A copy whose right addend's DECLARED unit gains a denominator — "schouwdagen" becomes "schouwdagen/mnd",
     *  the rate-against-its-own-numerator shape, at the ADDITIVE operand boundary. */
    private static JsonNode metTariefOpAddend(JsonNode ir) throws Exception {
        JsonNode kopie = ir.deepCopy();
        ObjectNode ref = additieveRechterRef(kopie);
        if (ref == null) return null;
        return metNoemer(kopie, declaratieVan(kopie, ref));
    }

    /** A copy whose additive rule's DOEL declaration gains a denominator — the same shape at the WRITE boundary. */
    private static JsonNode metTariefOpDoel(JsonNode ir) throws Exception {
        JsonNode kopie = ir.deepCopy();
        JsonNode regel = additieveRegel(kopie);
        if (regel == null) return null;
        return metNoemer(kopie, declaratieVan(kopie, regel.get("doel")));
    }

    /** A copy in which the declaration of a parameter READ IN A COMPARISON against a unit-bearing attribute
     *  gains a denominator — the same rate/plain shape at the §5.1 restatement position. The parameter must be
     *  the one the comparison actually reads; twisting an unrelated declaration probes nothing. */
    private static JsonNode metTariefOpParameter(JsonNode ir) throws Exception {
        JsonNode kopie = ir.deepCopy();
        String naam = vergelekenParameter(kopie);
        if (naam == null) return null;
        JsonNode p = kopie.path("parameters").get(naam);
        return p == null ? null : metNoemer(kopie, (ObjectNode) p.get("datatype"));
    }

    /** The name of the first parameter compared against a unit-bearing attribute read, anywhere in the document. */
    private static String vergelekenParameter(JsonNode ir) {
        List<String> uit = new ArrayList<>();
        zoekVergelijking(ir, ir, uit);
        return uit.isEmpty() ? null : uit.get(0);
    }

    private static void zoekVergelijking(JsonNode wortel, JsonNode x, List<String> uit) {
        if (!uit.isEmpty()) return;
        if (x.isArray()) {
            for (JsonNode k : x) zoekVergelijking(wortel, k, uit);
            return;
        }
        if (!x.isObject()) return;
        if ("Vergelijking".equals(x.path("node").asText())) {
            JsonNode l = x.get("links"), r = x.get("rechts");
            String naam = parameterMetEenheid(wortel, l) != null && declaratieVan(wortel, r) != null
                    ? parameterMetEenheid(wortel, l)
                    : (parameterMetEenheid(wortel, r) != null && declaratieVan(wortel, l) != null
                       ? parameterMetEenheid(wortel, r) : null);
            if (naam != null) { uit.add(naam); return; }
        }
        for (var it = x.fields(); it.hasNext(); ) zoekVergelijking(wortel, it.next().getValue(), uit);
    }

    /** The parameter name behind a unit-bearing, denominator-free {@code ParameterRef}; else null. */
    private static String parameterMetEenheid(JsonNode ir, JsonNode ref) {
        if (ref == null || !"ParameterRef".equals(ref.path("node").asText())) return null;
        String naam = ref.path("parameter").asText(null);
        if (naam == null) return null;
        JsonNode e = ir.path("parameters").path(naam).path("datatype").path("eenheid");
        return e.isObject() && e.path("noemer").isNull() ? naam : null;
    }

    /** Give {@code houder}'s unit a "mnd" denominator; null when there is no plain unit to give one to. */
    private static JsonNode metNoemer(JsonNode kopie, ObjectNode houder) throws Exception {
        if (houder == null || !(houder.get("eenheid") instanceof ObjectNode e)) return null;
        if (e.get("noemer") == null || !e.get("noemer").isNull()) return null;    // already a rate
        e.set("noemer", JSON.readTree(NOEMER));
        return kopie;
    }

    /** A copy whose right addend's ANNOTATED unit is foreign — the declaration untouched. */
    private static JsonNode metVerdraaideAnnotatie(JsonNode ir) throws Exception {
        JsonNode kopie = ir.deepCopy();
        ObjectNode ref = additieveRechterRef(kopie);
        if (ref == null) return null;
        ((ObjectNode) ref.get("type")).set("eenheid", JSON.readTree(VREEMD));
        return kopie;
    }

    /**
     * The right operand of the FIRST rule whose value is an additive join of two attribute reads that BOTH
     * resolve to a unit-bearing declaration — the shape an additive restatement always visits. Null when the
     * document carries none. Deterministic: rules are visited in document order and the first hit wins.
     */
    private static ObjectNode additieveRechterRef(JsonNode ir) {
        JsonNode regel = additieveRegel(ir);
        return regel == null ? null : (ObjectNode) regel.get("waarde").get("rechts");
    }

    /** The FIRST rule whose value is an additive join of two attribute reads that BOTH resolve to a unit-bearing
     *  declaration. Deterministic: rules are visited in document order and the first hit wins. */
    private static JsonNode additieveRegel(JsonNode ir) {
        JsonNode regels = ir.get("resolvedRegels");
        if (regels == null) return null;
        for (JsonNode r : regels) {
            JsonNode w = r.get("waarde");
            if (w == null || !"Binair".equals(w.path("node").asText())
                    || !List.of("PLUS", "MIN", "VERMINDERD_MET").contains(w.path("op").asText())) continue;
            if (declaratieVan(ir, w.get("links")) == null || declaratieVan(ir, w.get("rechts")) == null) continue;
            return r;
        }
        return null;
    }

    /** The DECLARATION an {@code AttribuutRef} reads from, when it carries a unit; else null. */
    private static ObjectNode declaratieVan(JsonNode ir, JsonNode ref) {
        if (ref == null || !"AttribuutRef".equals(ref.path("node").asText())) return null;
        JsonNode ot = ir.path("objecttypen").get(ref.path("objecttype").asText(""));
        if (ot == null) return null;
        JsonNode attr = ot.path("attributen").get(ref.path("attribuut").asText(""));
        if (attr == null || attr.path("eenheid").isMissingNode() || attr.path("eenheid").isNull()) return null;
        return (ObjectNode) attr;
    }

    // ---------------- driving the two branches ----------------

    /** Lower through the DRL-TEXT branch, returning the throwable it raised (null on success). */
    private static Throwable vangDrl(JsonNode ir) {
        try { drl(ir); return null; } catch (Throwable t) { return t; }
    }

    /** Lower through the EXEC-MODEL branch — the branch gate 12 replays — returning the throwable (null on success). */
    private static Throwable vangExec(JsonNode ir) {
        try { exec(ir); return null; } catch (Throwable t) { return t; }
    }

    private static boolean lowertDrl(JsonNode ir) { return vangDrl(ir) == null; }

    private static boolean lowertExec(JsonNode ir) { return vangExec(ir) == null; }

    private static String drl(JsonNode ir) {
        return DrlTranspiler.transpile(IrLoader.loadAll(ir).model());
    }

    private static void exec(JsonNode ir) {
        IrLoader.Loaded loaded = IrLoader.loadAll(ir);
        ClassLoader cl = BeanCompiler.compileer(FactModelGenerator.genereer(loaded.model()));
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        try {
            assertNotNull(ExecModelTranspiler.build(loaded.model(), cl, loaded.prioritizedDefaults(),
                    loaded.rolBindingen(), loaded.lets()));
        } finally {
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    // ---------------- helpers ----------------

    private static List<Path> goldens() throws Exception {
        try (Stream<Path> s = Files.walk(CORPUS)) {
            return s.filter(p -> p.getFileName().toString().equals("ir.json")).sorted().toList();
        }
    }

    private static String naam(Path ir) {
        return CORPUS.relativize(ir.getParent()).toString();
    }

    /** What came back instead of the expected fault — the diagnostic a bare boolean cannot give. */
    private static String omschrijf(Throwable t) {
        if (t == null) return "het document lowerde zonder melding";
        return t.getClass().getSimpleName() + ": " + t.getMessage();
    }

    @Test void de_corpuswandeling_vindt_daadwerkelijk_goldens() throws Exception {
        if (goldens().isEmpty()) fail("geen enkele corpus-golden gevonden onder " + CORPUS.toAbsolutePath());
    }
}
