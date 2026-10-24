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
class UnitConflictWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** A concrete unit from no system the corpus knows: a canonical of its own, so it inter-converts with
     *  nothing a golden declares, and a factor, so it is not the WF-092 factorless form. */
    private static final String FOREIGN = """
            {"node":"Unit","abbreviation":"uchar-vreemd",
             "canonical":"uchar-vreemd-canoniek","factorToCanonical":{"num":3600,"den":1}}""";

    /** A plain denominator, to turn a declared unit into a RATE with the very same numerator. */
    private static final String DENOMINATOR = """
            {"node":"Unit","abbreviation":"mnd","canonical":"mnd","factorToCanonical":{"num":1,"den":1}}""";

    // ---------------- the guard itself: what it refuses and what it lets through ----------------

    private static final Unit KG = new Unit("kg", "Gewicht", "kg", BigDecimal.ONE);
    private static final Unit TON = new Unit("t", "Gewicht", "kg", new BigDecimal("1000"));
    private static final Unit EUR = new Unit("EUR", "Geld", "EUR", BigDecimal.ONE);
    private static final Unit JR = new Unit("jr", "Tijd", "mnd", new BigDecimal("12"));

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

    @Test void a_DERIVED_opaque_product_stays_the_conservative_no_op() {
        // RUNTIME-SEMANTICS ch. 2.10 records this backend's stance on "€·kg": no canonical, so no factor is
        // invented and the value stands in the unit it was computed in. Faulting here would make that recorded
        // sentence false. The unit must come from the DERIVATION — that is where the provenance mark is set.
        Unit opaque = derivedProduct();
        assertNull(opaque.canonical(), "een opaak product draagt per constructie geen canoniek");
        UnitConflict.requireRescalable(opaque, EUR, "witness");
        UnitConflict.requireRescalable(EUR, opaque, "witness");
    }

    @Test void a_namesake_declared_on_the_wire_not_gets_the_vrijstelling() {
        // The hole a name test leaves: a wire-declared unit can carry exactly the four fields Unit.product
        // produces — no canonical, no factor, no denominator, a "·" in the abbreviation — without ever having
        // been derived. ref faults on such a document; so must this. Value-equal to the derived one, and still
        // treated differently, which is the whole point: provenance is not a property of the value.
        Unit derived = derivedProduct();
        Unit namesake = new Unit(derived.abbreviation(), derived.system(), null, null, null);
        assertEquals(derived, namesake, "de twee zijn waarde-gelijk — alleen hun herkomst verschilt");
        assertThrows(UnitConflict.class, () -> UnitConflict.requireRescalable(namesake, EUR, "witness"));
        assertThrows(UnitConflict.class, () -> UnitConflict.requireRescalable(EUR, namesake, "witness"));
    }

    @Test void two_different_derived_products_also_set_each_other_free() {
        // The exemption is carried by ONE side, so €·kg against €·m passes too — not only a product against a
        // concrete unit. Same ch. 2.10 stance (neither side has a canonical, so no factor is invented either
        // way), and the cost is a diagnosis rather than a wrong number. Pinned so the breadth is a statement.
        Unit eurKg = derivedProduct();
        Unit eurM = derivedProduct(new Unit("m", "Lengte", "m", BigDecimal.ONE));
        assertNotEquals(eurKg, eurM, "twee verschillende producten, allebei zonder canoniek");
        UnitConflict.requireRescalable(eurKg, eurM, "witness");
        UnitConflict.requireRescalable(eurM, eurKg, "witness");
    }

    @Test void the_origin_table_grows_per_derivation_not_per_document() {
        // The growth law the class doc states, as a checked claim: Unit.product allocates a fresh instance
        // every time, so deriving the SAME product twice adds TWO entries. The table is monotone for the JVM's
        // lifetime and never cleared — a bounded strong-reference leak, recorded rather than glossed.
        int before = EffectiveUnit.countDerivedProducts();
        Unit one = derivedProduct();
        Unit two = derivedProduct();
        assertEquals(before + 2, EffectiveUnit.countDerivedProducts(),
                "elke maalEenheid-evaluatie voegt een eigen entry toe");
        assertEquals(one, two, "de twee zijn waarde-gelijk");
        assertNotSame(one, two, "en toch verschillende instanties — daarom groeit de tabel");
    }

    @Test void a_rate_against_its_own_numerator_is_a_fault() {
        // €/jr against € was EXEMPT until 2026-09-09, to keep two §7.1 goldens green — and that exemption was
        // place-blind, so an additive join, a write boundary and a comparison between a rate and its numerator
        // all stayed silent while ref and clingo faulted. The repair is the derivation (see the §7.1 test
        // below), not an exemption, so this pair faults again everywhere. Both directions.
        Unit rate = Unit.compound(EUR, JR);
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
        Unit rate = Unit.compound(EUR, JR);
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
        Unit rate = Unit.compound(EUR, JR);
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
        Unit rate = Unit.compound(EUR, JR);
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

    /** An opaque product carrying this backend's own provenance mark: obtained through the DERIVATION (a §4.4
     *  MAAL of two unit-bearing reads), never by calling the constructor, because the mark is set where the
     *  product is built and a value-equal copy does not carry it. */
    private static Unit derivedProduct() { return derivedProduct(KG); }

    /** As above, with the second factor chosen — so two DISTINCT derived products can be put side by side. */
    private static Unit derivedProduct(Unit second) {
        ObjectType ot = new ObjectType("Vracht");
        ot.attributes.put("prijs", num(EUR));
        ot.attributes.put("factor", num(second));
        Expr.AttributeReference prijs = new Expr.AttributeReference("prijs", ot, "een vracht", null, num(EUR), source());
        Expr.AttributeReference other = new Expr.AttributeReference("factor", ot, "een vracht", null, num(second), source());
        Unit out = bare().unitOf(new Expr.Binary(Expr.BinOp.MAAL, prijs, other, num(EUR), source()));
        assertEquals(Unit.product(EUR, second), out, "de afleiding levert het §4.4 opake product");
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
        return e.isObject() && !e.has("denominator") ? name : null;
    }

    /** Give {@code holder}'s unit a "mnd" denominator; null when there is no plain unit to give one to. */
    private static JsonNode withDenominator(JsonNode copy, ObjectNode holder) throws Exception {
        if (holder == null || !(holder.get("unit") instanceof ObjectNode e)) return null;
        if (e.has("denominator")) return null;    // already a rate
        e.set("denominator", JSON.readTree(DENOMINATOR));
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
