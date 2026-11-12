package dev.uchar.regelspraak.transpiler.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.engine.BeanCompiler;
import dev.uchar.regelspraak.ir.Source;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.Unit;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
class UnitConflictWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** A concrete unit from no system the corpus knows: a canonical of its own, so it inter-converts with
     *  nothing a golden declares, and a factor, so it is not the WF-092 factorless form. */
    private static final String FOREIGN = """
            [{"node":"Unit","abbreviation":"uchar-vreemd",
             "canonical":"uchar-vreemd-canoniek","factorToCanonical":{"num":3600,"den":1}}]""";

    /** A plain denominator, to turn a declared unit into a RATE with the very same numerator. */
    private static final String DENOMINATOR = """
            {"node":"Unit","abbreviation":"mnd","canonical":"mnd","factorToCanonical":{"num":1,"den":1},"exponent":-1}""";

    // ---------------- the guard itself: what it refuses and what it lets through ----------------

    private static final Unit KG = new Unit("kg", "Gewicht", "kg", Unit.Fraction.ONE);
    private static final Unit TON = new Unit("t", "Gewicht", "kg", Unit.Fraction.of(1000, 1));
    private static final Unit EUR = new Unit("EUR", "Geld", "EUR", Unit.Fraction.ONE);
    private static final Unit JR = new Unit("jr", "Tijd", "mnd", Unit.Fraction.of(12, 1));

    @Test void two_concrete_units_without_shared_canonical_are_a_fault() {
        UnitConflict f = assertThrows(UnitConflict.class,
                () -> UnitConflict.requireRescalable(JR, KG, "een §4-waardeherschaling"));
        assertSame(JR, f.from());
        assertSame(KG, f.destination());
        assertEquals("een §4-waardeherschaling", f.site());
        // the message must name BOTH sides, the way ref's "incompatible units: schouwdagen vs uur" does:
        // a fault that names only the target leaves the reader without the half that moved.
        assertTrue(f.getMessage().contains("'jr'") && f.getMessage().contains("'kg'"), f.getMessage());
        assertTrue(f.getMessage().contains("een §4-waardeherschaling"), "de plek hoort in de melding");
    }

    @Test void an_interchangeable_pair_goes_through_unhindered() {
        // the ordinary cross-unit restatement: kg <-> t share a canonical, so a factor exists and the site
        // must compute it. Both directions, and the identity pair.
        UnitConflict.requireRescalable(KG, TON, "witness");
        UnitConflict.requireRescalable(TON, KG, "witness");
        UnitConflict.requireRescalable(KG, KG, "witness");
    }

    @Test void a_unitless_side_is_no_conflict() {
        // ch. 2.6: a unitless side adopts the other, value unchanged — ref does the same and no factor is due.
        UnitConflict.requireRescalable(null, KG, "witness");
        UnitConflict.requireRescalable(KG, null, "witness");
        UnitConflict.requireRescalable(null, null, "witness");
    }

    /** Half a conversion pair at a restatement: the two branches fault while the model is built, in the sentence of
     *  {@link Unit#exactRestateFactors}, and the fault is no {@link UnitConflict}: the two units do convert, and what is
     *  missing is the factor. */
    @Test void half_a_conversion_pair_at_a_restatement_faults_in_both_branches() throws Exception {
        JsonNode honest = JSON.readTree(Files.readString(CORPUS.resolve("jutterswet-sleepbaan").resolve("ir.json")));
        JsonNode half = honest.deepCopy();
        int stripped = withoutFactorOf(half, "km");
        assertTrue(stripped > 0, "het document draagt de eenheid km");
        assertEquals(null, catchTms(honest), "het eerlijke document lowert");
        String text = "onbekende omrekenfactor van 'km' (null): canoniek 'm' zonder gedeclareerde factorNaarCanoniek";
        for (Throwable t : List.of(catchTms(half), catchDrl(half))) {
            assertEquals(IllegalStateException.class, t.getClass(), describe(t));
            assertEquals(text, t.getMessage());
        }
    }

    /** {@code node} with the factor taken off every unit {@code abbreviation}, the canonical left on; how many it took. */
    private static int withoutFactorOf(JsonNode node, String abbreviation) {
        int n = 0;
        if (node instanceof com.fasterxml.jackson.databind.node.ObjectNode o && abbreviation.equals(o.path("abbreviation").asText())
                && o.remove("factorToCanonical") != null) n++;
        for (JsonNode child : node) n += withoutFactorOf(child, abbreviation);
        return n;
    }

    @Test void a_product_against_another_dimension_is_a_fault() {
        // A product is a unit like any other: €·kg against € has no factor, in either direction.
        Unit product = derivedProduct();
        assertThrows(UnitConflict.class, () -> UnitConflict.requireRescalable(product, EUR, "witness"));
        assertThrows(UnitConflict.class, () -> UnitConflict.requireRescalable(EUR, product, "witness"));
    }

    @Test void a_product_declared_on_the_wire_is_the_same_unit_as_a_derived_one() {
        // Where a unit came from says nothing about it: a value-equal copy answers the same.
        Unit derived = derivedProduct();
        Unit namesake = new Unit(derived.factors());
        assertEquals(derived, namesake);
        UnitConflict.requireRescalable(derived, namesake, "witness");
        assertThrows(UnitConflict.class, () -> UnitConflict.requireRescalable(namesake, EUR, "witness"));
        assertThrows(UnitConflict.class, () -> UnitConflict.requireRescalable(EUR, namesake, "witness"));
    }

    @Test void two_different_products_are_a_fault() {
        Unit eurKg = derivedProduct();
        Unit eurM = derivedProduct(new Unit("m", "Lengte", "m", Unit.Fraction.ONE));
        assertNotEquals(eurKg, eurM);
        assertThrows(UnitConflict.class, () -> UnitConflict.requireRescalable(eurKg, eurM, "witness"));
        assertThrows(UnitConflict.class, () -> UnitConflict.requireRescalable(eurM, eurKg, "witness"));
    }

    @Test void a_derived_product_against_a_rate_is_a_fault() {
        // A rate has another dimension. Measured on a document writing m × m into an attribute in m^2/s: ref faults and
        // this backend does. The same numerator does not let the pair through.
        Unit product = derivedProduct();
        Unit sameNumerator = Unit.divide(product, JR);
        assertThrows(UnitConflict.class, () -> UnitConflict.requireRescalable(product, sameNumerator, "witness"));
        assertThrows(UnitConflict.class, () -> UnitConflict.requireRescalable(sameNumerator, product, "witness"));
        Unit otherRate = Unit.divide(EUR, JR);
        assertThrows(UnitConflict.class, () -> UnitConflict.requireRescalable(product, otherRate, "witness"));
        assertThrows(UnitConflict.class, () -> UnitConflict.requireRescalable(otherRate, product, "witness"));
    }

    @Test void a_product_into_its_declared_square_plays() {
        // m × m in a declared m^2: identical, no factor due. cm × m in the same: the dimension holds and a factor exists.
        Unit m = new Unit("m", "Lengte", "m", Unit.Fraction.ONE);
        Unit cm = new Unit("cm", "Lengte", "m", Unit.Fraction.of(1, 100));
        Unit square = new Unit(List.of(new Unit.Factor("m", "Lengte", "m", Unit.Fraction.ONE, 2)));
        Unit product = derivedProduct(m, m);
        assertEquals(square, product);
        UnitConflict.requireRescalable(product, square, "witness");
        assertFalse(product.requiredConversion(square));
        Unit cmSquare = derivedProduct(cm, m);
        assertEquals("cm^2", cmSquare.rendering());
        UnitConflict.requireRescalable(cmSquare, square, "witness");
        assertTrue(cmSquare.requiredConversion(square));
    }

    @Test void a_rate_against_its_own_numerator_is_a_fault() {
        // €/jr against € was EXEMPT until 2026-09-09, to keep two §7.1 goldens green — and that exemption was
        // place-blind, so an additive join, a write boundary and a comparison between a rate and its numerator
        // all stayed silent while ref and clingo faulted. The repair is the derivation (see the §7.1 test
        // below), not an exemption, so this pair faults again everywhere. Both directions.
        Unit rate = Unit.divide(EUR, JR);
        assertThrows(UnitConflict.class, () -> UnitConflict.requireRescalable(rate, EUR, "witness"));
        assertThrows(UnitConflict.class, () -> UnitConflict.requireRescalable(EUR, rate, "witness"));
        assertThrows(UnitConflict.class, () -> UnitConflict.requireRescalable(rate, KG, "witness"));
    }

    // ---------------- the §7.1 derivation that replaces the exemption ----------------

    @Test void a_total_over_a_timeline_rate_yields_the_numerator() {
        // §7.1 / ch. 3.6: the operand's own timeline collapses first, each segment contributing value x whole
        // grid units of its span, so a €/mnd rate is multiplied by a COUNT of months and what survives is €.
        // Without this the fold would answer €/mnd for a result that is €, and every restatement downstream
        // would be handed a pair with no factor — which is exactly what the two corpus goldens did.
        Unit rate = Unit.divide(EUR, JR);
        ObjectType withTimeline = new ObjectType("Vondst");
        withTimeline.attributes.put("tarief", num(rate));
        withTimeline.attributeGranularity.put("tarief", RegelSpraakModel.Granularity.MAAND);
        assertEquals(EUR, bare().unitOf(totalOver(withTimeline)), "de noemer is door de vouwing verbruikt");

        // and NOT when the operand carries no timeline: a plain set TOTAAL of rates is still a rate. The same
        // corpus golden carries both readings side by side, which is why the rule keys on the declaration.
        ObjectType withoutTimeline = new ObjectType("Vondst");
        withoutTimeline.attributes.put("tarief", num(rate));
        assertEquals(rate, bare().unitOf(totalOver(withoutTimeline)), "zonder tijdlijn blijft het een tarief");
    }

    @Test void a_sum_over_a_timeline_rate_also_yields_the_numerator() {
        // SOM carries the per-element timeline fold exactly as TOTAAL does — both consumers say so: the
        // exec-model instance-fold guard admits those two kinds and skips a temporal operand for every other,
        // and ref faults on the others in as many words. Restricting the collapse to TOTAAL left SOM answering
        // the rate for an already-folded value: measured 576 for 48 into a €/jr doel (the mnd->jr factor applied
        // twice over), where ref faults. Same declaration, same shape, only the operator differs.
        Unit rate = Unit.divide(EUR, JR);
        ObjectType ot = new ObjectType("Vondst");
        ot.attributes.put("tarief", num(rate));
        ot.attributeGranularity.put("tarief", RegelSpraakModel.Granularity.MAAND);
        Expr.AttributeReference ref = new Expr.AttributeReference("tarief", ot, "alle vondsten", null, num(rate), source());
        Expr.FunctionCall sum = new Expr.FunctionCall(Expr.FunctionKind.SOM, List.of(ref), ot, null, null, num(rate), source());
        assertEquals(EUR, bare().unitOf(sum), "de noemer is door de vouwing verbruikt");
    }

    @Test void a_minimum_over_a_timeline_rate_keeps_the_rate() {
        // A selecting fold sums nothing, so no denominator is consumed and none may be dropped. Measured over
        // the corpus: every fold whose operand is timeline-declared is a TOTAAL, so this is the boundary of a
        // rule that was scoped by measurement rather than by guess.
        Unit rate = Unit.divide(EUR, JR);
        ObjectType ot = new ObjectType("Vondst");
        ot.attributes.put("tarief", num(rate));
        ot.attributeGranularity.put("tarief", RegelSpraakModel.Granularity.MAAND);
        Expr.AttributeReference ref = new Expr.AttributeReference("tarief", ot, "alle vondsten", null, num(rate), source());
        Expr.FunctionCall f = new Expr.FunctionCall(Expr.FunctionKind.MINIMUM, List.of(ref), ot, null, null, num(rate), source());
        assertEquals(rate, bare().unitOf(f));
    }

    // ---------------- authored-model helpers ----------------

    private static Datatype num(Unit e) {
        return new Datatype.Numeric(false, 2, Datatype.Sign.ONBEPAALD, e);
    }

    private static Source source() { return Source.synthetic("witness"); }

    private static EffectiveUnit bare() { return new EffectiveUnit(List.of()); }

    /** "het totaal van het tarief van alle vondsten" over {@code ot} — the §7.1 fold shape. */
    private static Expr.FunctionCall totalOver(ObjectType ot) {
        Datatype t = ot.attributes.get("tarief");
        Expr.AttributeReference ref = new Expr.AttributeReference("tarief", ot, "alle vondsten", null, t, source());
        return new Expr.FunctionCall(Expr.FunctionKind.TOTAAL, List.of(ref), ot, null, null, t, source());
    }

    /** A product obtained through the DERIVATION: a §4.4 MAAL of two unit-bearing reads. */
    private static Unit derivedProduct() { return derivedProduct(KG); }

    /** As above, with the second factor chosen — so two DISTINCT derived products can be put side by side. */
    private static Unit derivedProduct(Unit second) { return derivedProduct(EUR, second); }

    private static Unit derivedProduct(Unit first, Unit second) {
        ObjectType ot = new ObjectType("Vracht");
        ot.attributes.put("prijs", num(first));
        ot.attributes.put("factor", num(second));
        Expr.AttributeReference prijs = new Expr.AttributeReference("prijs", ot, "een vracht", null, num(first), source());
        Expr.AttributeReference other = new Expr.AttributeReference("factor", ot, "een vracht", null, num(second), source());
        Unit out = bare().unitOf(new Expr.Binary(Expr.BinOp.MAAL, prijs, other, num(first), source()));
        assertEquals(Unit.times(first, second), out, "de afleiding is de algebra van het model");
        return out;
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
    @Test void a_contradicting_declaration_is_silently_played_through_by_none_of_both_branches() throws Exception {
        Map<String, Twist> twists = new java.util.LinkedHashMap<>();
        twists.put("additieve operand -> vreemde eenheid", UnitConflictWitnessTest::withTwistedDeclaration);
        twists.put("additieve operand -> tarief met dezelfde teller", UnitConflictWitnessTest::withRateOnAddend);
        twists.put("schrijfgrens -> doel als tarief", UnitConflictWitnessTest::withRateOnTarget);
        twists.put("§5.1 vergelijking -> parameter als tarief", UnitConflictWitnessTest::withRateOnParameter);

        List<String> silent = new ArrayList<>();
        List<String> untried = new ArrayList<>();
        for (Map.Entry<String, Twist> t : twists.entrySet()) {
            int tmsSeen = 0, drlSeen = 0;
            for (Path ir : goldens()) {
                JsonNode honest = JSON.readTree(Files.readString(ir));
                JsonNode lying = t.getValue().apply(honest);
                if (lying == null) continue;                   // this golden does not carry the shape
                if (lowersTms(honest)) {
                    tmsSeen++;
                    if (!(catchTms(lying) instanceof UnitConflict))
                        silent.add(t.getKey() + " | " + name(ir) + " (tms): " + describe(catchTms(lying)));
                }
                if (lowersDrl(honest)) {
                    drlSeen++;
                    if (!(catchDrl(lying) instanceof UnitConflict))
                        silent.add(t.getKey() + " | " + name(ir) + " (DRL-tekst): " + describe(catchDrl(lying)));
                }
            }
            // never silently green: a twist no golden carries would make its whole row vacuous, so an unprobed
            // twist is itself the failure — per branch, because the two lower different subsets of the corpus.
            if (tmsSeen == 0) untried.add(t.getKey() + " (tms)");
            if (drlSeen == 0) untried.add(t.getKey() + " (DRL-tekst)");
        }
        assertEquals(List.of(), untried, "een verdraaiing vond geen enkele golden om op te staan");
        assertEquals(List.of(), silent, "een verdraaide declaratie werd stil gelowered in plaats van gemeld");
    }

    /** One declaration twist: returns the mutated document, or null when the golden does not carry the shape. */
    private interface Twist {
        JsonNode apply(JsonNode ir) throws Exception;
    }

    /**
     * The harness's own direction: move only an ANNOTATION to the same foreign unit. Since L2 of the v1 wire
     * migration a reference carries none — its datatype is its declaration's — so the nearest annotation is the
     * additive composite's own, the derived position the two reads meet in. The declarations are the source, so
     * nothing may fault and the DRL text may not move by a character — this is the property that keeps
     * {@code check-reftype-derivation.sh} green under the fence.
     */
    @Test void a_lying_annotation_above_a_correct_declaration_changes_nothing() throws Exception {
        int seen = 0;
        List<String> moved = new ArrayList<>();
        for (Path ir : goldens()) {
            JsonNode honest = JSON.readTree(Files.readString(ir));
            JsonNode lying = withTwistedAnnotation(honest);
            if (lying == null || !lowersDrl(honest)) continue;
            seen++;
            Throwable t = catchDrl(lying);
            if (t != null) { moved.add(name(ir) + " faalde: " + describe(t)); continue; }
            if (!drl(honest).equals(drl(lying))) moved.add(name(ir) + ": de DRL-tekst bewoog");
            if (lowersTms(honest) && catchTms(lying) != null)
                moved.add(name(ir) + " (tms) faalde: " + describe(catchTms(lying)));
        }
        assertTrue(seen > 0, "geen enkele golden bood een verdraaibare annotatie op deze positie");
        assertEquals(List.of(), moved, "de annotatie is niet de bron: een leugen daar mag niets bewegen");
    }

    /** The reference itself can no longer lie: an annotation written on it is a field the wire does not have, and
     *  the loader refuses it by name, at its pointer, before any branch lowers a thing. */
    @Test void an_annotation_on_the_read_itself_refuses_the_loader_by_name() throws Exception {
        int seen = 0;
        for (Path ir : goldens()) {
            JsonNode copy = JSON.readTree(Files.readString(ir));
            ObjectNode ref = additiveRightRef(copy);
            if (ref == null) continue;
            seen++;
            ref.putObject("type").put("node", "Number").put("whole", false).put("decimals", 2)
                    .put("sign", "unspecified").set("unit", JSON.readTree(FOREIGN));
            IrLoader.DocumentError f = assertThrows(IrLoader.DocumentError.class, () -> IrLoader.loadAll(copy),
                    name(ir));
            assertTrue(f.site().endsWith("/value/right/type") && f.getMessage().contains("unknown field"),
                    name(ir) + ": " + f.getMessage());
        }
        assertTrue(seen > 0, "geen enkele golden droeg een optelling van twee gedeclareerde lezingen");
    }

    // ---------------- twisting ----------------

    /** A copy whose right addend's DECLARED unit is foreign, or null when this golden carries no such shape. */
    private static JsonNode withTwistedDeclaration(JsonNode ir) throws Exception {
        JsonNode copy = ir.deepCopy();
        ObjectNode ref = additiveRightRef(copy);
        if (ref == null) return null;
        ObjectNode decl = declarationFrom(copy, ref);
        decl.set("unit", JSON.readTree(FOREIGN));
        // a percentage presentation names the unit '%' (IR-SPEC ch. 10); with a foreign unit it would make the copy
        // malformed and the loader would refuse it before the unit fence this twist probes
        decl.remove("presentation");
        return copy;
    }

    /** A copy whose right addend's DECLARED unit gains a denominator — "schouwdagen" becomes "schouwdagen/mnd",
     *  the rate-against-its-own-numerator shape, at the ADDITIVE operand boundary. */
    private static JsonNode withRateOnAddend(JsonNode ir) throws Exception {
        JsonNode copy = ir.deepCopy();
        ObjectNode ref = additiveRightRef(copy);
        if (ref == null) return null;
        return withDenominator(copy, declarationFrom(copy, ref));
    }

    /** A copy whose additive rule's DOEL declaration gains a denominator — the same shape at the WRITE boundary. */
    private static JsonNode withRateOnTarget(JsonNode ir) throws Exception {
        JsonNode copy = ir.deepCopy();
        JsonNode rule = additiveRule(copy);
        if (rule == null) return null;
        return withDenominator(copy, declarationFrom(copy, rule.get("target")));
    }

    /** A copy in which the declaration of a parameter READ IN A COMPARISON against a unit-bearing attribute
     *  gains a denominator — the same rate/plain shape at the §5.1 restatement position. The parameter must be
     *  the one the comparison actually reads; twisting an unrelated declaration probes nothing. */
    private static JsonNode withRateOnParameter(JsonNode ir) throws Exception {
        JsonNode copy = ir.deepCopy();
        String name = comparedParameter(copy);
        if (name == null) return null;
        JsonNode p = findMember(copy.path("model").path("parameters"), "name", name);
        return p == null ? null : withDenominator(copy, (ObjectNode) p.get("datatype"));
    }

    /** The name of the first parameter compared against a unit-bearing attribute read, anywhere in the document. */
    private static String comparedParameter(JsonNode ir) {
        List<String> out = new ArrayList<>();
        findComparison(ir, ir, out);
        return out.isEmpty() ? null : out.get(0);
    }

    private static void findComparison(JsonNode squareRoot, JsonNode x, List<String> out) {
        if (!out.isEmpty()) return;
        if (x.isArray()) {
            for (JsonNode k : x) findComparison(squareRoot, k, out);
            return;
        }
        if (!x.isObject()) return;
        if ("Comparison".equals(x.path("node").asText())) {
            JsonNode l = x.get("left"), r = x.get("right");
            String name = parameterWithUnit(squareRoot, l) != null && declarationFrom(squareRoot, r) != null
                    ? parameterWithUnit(squareRoot, l)
                    : (parameterWithUnit(squareRoot, r) != null && declarationFrom(squareRoot, l) != null
                       ? parameterWithUnit(squareRoot, r) : null);
            if (name != null) { out.add(name); return; }
        }
        for (var it = x.fields(); it.hasNext(); ) findComparison(squareRoot, it.next().getValue(), out);
    }

    /** The parameter name behind a unit-bearing, denominator-free {@code ParameterReference}; else null. */
    private static String parameterWithUnit(JsonNode ir, JsonNode ref) {
        if (ref == null || !"ParameterReference".equals(ref.path("node").asText())) return null;
        String name = ref.path("parameter").asText(null);
        if (name == null) return null;
        JsonNode p = findMember(ir.path("model").path("parameters"), "name", name);
        if (p == null) return null;
        JsonNode e = p.path("datatype").path("unit");
        return plainUnit(e) ? name : null;
    }

    /** A unit on the wire whose factors all stand in the numerator. */
    private static boolean plainUnit(JsonNode e) {
        if (!e.isArray()) return false;
        for (JsonNode f : e) if (f.path("exponent").asInt(1) < 0) return false;
        return true;
    }

    /** Give {@code holder}'s unit a "mnd" denominator; null when there is no plain unit to give one to. */
    private static JsonNode withDenominator(JsonNode copy, ObjectNode holder) throws Exception {
        if (holder == null || !plainUnit(holder.path("unit"))) return null;
        ((com.fasterxml.jackson.databind.node.ArrayNode) holder.get("unit")).add(JSON.readTree(DENOMINATOR));
        return copy;
    }

    /** A copy whose additive composite's ANNOTATED unit is foreign — the declarations untouched. */
    private static JsonNode withTwistedAnnotation(JsonNode ir) throws Exception {
        JsonNode copy = ir.deepCopy();
        JsonNode rule = additiveRule(copy);
        if (rule == null || !(rule.get("value").get("type") instanceof ObjectNode type)) return null;
        type.set("unit", JSON.readTree(FOREIGN));
        return copy;
    }

    /**
     * The right operand of the FIRST rule whose value is an additive join of two attribute reads that BOTH
     * resolve to a unit-bearing declaration — the shape an additive restatement always visits. Null when the
     * document carries none. Deterministic: rules are visited in document order and the first hit wins.
     */
    private static ObjectNode additiveRightRef(JsonNode ir) {
        JsonNode rule = additiveRule(ir);
        return rule == null ? null : (ObjectNode) rule.get("value").get("right");
    }

    /** The FIRST rule version whose value is an additive join of two attribute reads that BOTH resolve to a
     *  unit-bearing declaration. Deterministic: rules and their versions are visited in document order and the
     *  first hit wins. */
    private static JsonNode additiveRule(JsonNode ir) {
        JsonNode rules = ir.path("rules").get("rules");
        if (rules == null) return null;
        List<JsonNode> versions = new ArrayList<>();
        for (JsonNode form : rules) form.path("versions").forEach(versions::add);
        for (JsonNode r : versions) {
            JsonNode w = r.get("value");
            if (w == null || !"Binary".equals(w.path("node").asText())
                    || !List.of("plus", "minus", "reducedBy").contains(w.path("op").asText())) continue;
            if (declarationFrom(ir, w.get("left")) == null || declarationFrom(ir, w.get("right")) == null) continue;
            return r;
        }
        return null;
    }

    /** The DECLARATION an {@code AttributeReference} reads from, when it carries a unit; else null. */
    private static ObjectNode declarationFrom(JsonNode ir, JsonNode ref) {
        if (ref == null || !"AttributeReference".equals(ref.path("node").asText())) return null;
        JsonNode ot = findMember(ir.path("model").path("objectTypes"), "name", ref.path("subjectType").asText(""));
        if (ot == null) return null;
        JsonNode member = findMember(ot.path("attributes"), "name", ref.path("attribute").asText(""));
        JsonNode attr = member == null ? null : member.get("datatype");
        if (attr == null || !attr.has("unit")) return null;
        return (ObjectNode) attr;
    }

    /** The entry of a declaration list whose {@code nameField} is {@code name}, or null. A miss is an answer here,
     *  not a fault: it means "this golden does not carry the shape" (an inherited attribute, say), the same null
     *  the v0 map lookup gave. */
    private static JsonNode findMember(JsonNode list, String nameField, String name) {
        for (JsonNode n : list) if (name.equals(n.path(nameField).asText(null))) return n;
        return null;
    }

    // ---------------- driving the two branches ----------------

    /** Lower through the DRL-TEXT branch, returning the throwable it raised (null on success). */
    private static Throwable catchDrl(JsonNode ir) {
        try { drl(ir); return null; } catch (Throwable t) { return t; }
    }

    /** Lower through the EXEC-MODEL branch — the branch gate 12 replays — returning the throwable (null on success). */
    private static Throwable catchTms(JsonNode ir) {
        try { tms(ir); return null; } catch (Throwable t) { return t; }
    }

    private static boolean lowersDrl(JsonNode ir) { return catchDrl(ir) == null; }

    private static boolean lowersTms(JsonNode ir) { return catchTms(ir) == null; }

    /** The text of the DRL-text branch: the analysis rendered as DRL text, through the backend's entry. */
    private static String drl(JsonNode ir) {
        return dev.uchar.regelspraak.cases.DroolsBackend.build(ir,
                dev.uchar.regelspraak.cases.DroolsBackend.Branch.DRL_TEXT).drl();
    }

    private static void tms(JsonNode ir) {
        IrLoader.Positioned positioned = IrLoader.loadWithPositions(ir);
        IrLoader.Loaded loaded = positioned.loaded();
        ClassLoader cl = BeanCompiler.compile(FactModelGenerator.generate(loaded.model()));
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        try {
            assertNotNull(TmsTranspiler.build(loaded.model(), cl, loaded.prioritizedDefaults(),
                    loaded.roleBindings(), loaded.lets(), loaded.sourcePaths(), loaded.navigationBindings(),
                    loaded.anchored(), positioned.positions()));
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    // ---------------- helpers ----------------

    private static List<Path> goldens() throws Exception {
        try (Stream<Path> s = Files.walk(CORPUS)) {
            return s.filter(p -> p.getFileName().toString().equals("ir.json")).sorted().toList();
        }
    }

    private static String name(Path ir) {
        return CORPUS.relativize(ir.getParent()).toString();
    }

    /** What came back instead of the expected fault — the diagnostic a bare boolean cannot give. */
    private static String describe(Throwable t) {
        if (t == null) return "het document lowerde zonder melding";
        return t.getClass().getSimpleName() + ": " + t.getMessage();
    }

    @Test void the_corpus_walk_actually_finds_goldens() throws Exception {
        if (goldens().isEmpty()) fail("geen enkele corpus-golden gevonden onder " + CORPUS.toAbsolutePath());
    }
}
