package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * §5.7 the CONCATENATION of subject expressions under "het aantal" — the producer route for the
 * wire's {@code InstanceSetUnion} (uchar IR-SPEC ch. 4.3, WF-110; semantics RUNTIME-SEMANTICS
 * ch. 3.4d; maintainer decision 2026-09-20).
 *
 * <p><b>Red-first, measured on the pre-card frontend (commit 5b42e5b9).</b> Both golden sources of
 * this card — {@code corpus/golden/jutterswet-vondstunie} and {@code …-keurunie} — were refused by
 * {@code countArityError} with the WF-037 arity ERROR and reached no disk at all. What changed is
 * not that refusal but its SCOPE: a count over an operand list is still ill-formed on the wire, and
 * a concatenation of SUBJECT expressions is no longer such a list — it is one union node, one
 * argument.
 *
 * <p>The sister suite {@link CountEnumeratedTest} pins the half that did NOT move: a count over
 * VALUE operands stays refused, in the words it has carried since 2026-09-20. This suite pins the
 * half that opened, and the three edges where it does not.
 */
class CountUnionTest {

    static final String MODEL = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            het aantal strandvondsten Numeriek (niet-negatief geheel getal);
            de gewogen vangst Numeriek (getal met 2 decimalen);
            de gewogen bijvangst Numeriek (getal met 2 decimalen);

        Objecttype de Vondst (mv: Vondsten)
            is gemerkt kenmerk (bijvoeglijk);
            is gekeurd kenmerk (bijvoeglijk);
            het gewicht Numeriek (geheel getal);

        Objecttype de Kavel (mv: Kavels)

        Feittype vondstaanmelding
            de aanmelder Jutter
            de aangemelde vondst (mv: aangemelde vondsten) Vondst
        Eén aanmelder meldt meerdere aangemelde vondsten

        Feittype vondstberging
            de berger Jutter
            de geborgen vondst (mv: geborgen vondsten) Vondst
        Eén berger bergt meerdere geborgen vondsten

        Feittype kaveltoewijzing
            de kavelhouder Jutter
            de duinkavel (mv: duinkavels) Kavel
        Eén kavelhouder houdt meerdere duinkavels
        """;

    /** Two selections over DIFFERENT relations — the shape no OR-filter can spell, and the one where
     *  deduplication stops being automatic and becomes an operation. */
    static final String TWO_RELATIONS = """

        Regel aantal strandvondsten
            geldig altijd
                Het aantal strandvondsten van een Jutter moet berekend worden als het aantal aangemelde vondsten van de Jutter en geborgen vondsten van de Jutter.
        """;

    /** Two selections over ONE relation with their own characteristic subselections — the shape
     *  §5.7's own worked example spells and the one the v0 toolchain lowers. */
    static final String TWO_CHARACTERISTICS = """

        Regel aantal strandvondsten
            geldig altijd
                Het aantal strandvondsten van een Jutter moet berekend worden als het aantal aangemelde vondsten van de Jutter die gemerkt zijn en aangemelde vondsten van de Jutter die gekeurd zijn.
        """;

    /** A filtered selection beside an UNFILTERED one — where the v0 toolchain leaves a marker,
     *  because its lowering ORs the operands' filter terms and has no term to OR for this one. */
    static final String BARE_BESIDE_FILTERED = """

        Regel aantal strandvondsten
            geldig altijd
                Het aantal strandvondsten van een Jutter moet berekend worden als het aantal aangemelde vondsten van de Jutter die gemerkt zijn en aangemelde vondsten van de Jutter.
        """;

    /** Three selections through the comma spelling (§5.7: commas between all but the last two). */
    static final String COMMA_TRIPLE = """

        Regel aantal strandvondsten
            geldig altijd
                Het aantal strandvondsten van een Jutter moet berekend worden als het aantal aangemelde vondsten van de Jutter die gemerkt zijn, aangemelde vondsten van de Jutter die gekeurd zijn en geborgen vondsten van de Jutter.
        """;

    /** The first edge: two POPULATIONS. WF-110 (d) refuses it, and so does the toolchain — there on
     *  the mechanism ground that one accumulate has one source pattern, here because a count answers
     *  a cardinality over one declared type. */
    static final String NOT_EQUAL_ELEMENT_TYPES = """

        Regel aantal strandvondsten
            geldig altijd
                Het aantal strandvondsten van een Jutter moet berekend worden als het aantal aangemelde vondsten van de Jutter en duinkavels van de Jutter.
        """;

    /** The second edge: a "die/dat" that is not a characteristic of the element. A silently dropped
     *  filter would count too many, and the union would ABSORB the error — an unfiltered selection
     *  is a superset of every filtered one over the same relation — so it is refused by name. */
    static final String UNSOLVABLE_SUBSELECTION = """

        Regel aantal strandvondsten
            geldig altijd
                Het aantal strandvondsten van een Jutter moet berekend worden als het aantal aangemelde vondsten van de Jutter die groter zijn dan 5 en aangemelde vondsten van de Jutter die gekeurd zijn.
        """;

    /** The counter-proof that must NOT move: VALUE operands keep the refusal of 2026-09-20. */
    static final String VALUE_OPERANDS = """

        Regel aantal strandvondsten
            geldig altijd
                Het aantal strandvondsten van een Jutter moet berekend worden als het aantal de gewogen vangst van de Jutter en de gewogen bijvangst van de Jutter.
        """;

    /** The other counter-proof: v0's own one-argument instance count keeps its shape. */
    static final String ORDINARY_COUNT = """

        Regel aantal strandvondsten
            geldig altijd
                Het aantal strandvondsten van een Jutter moet berekend worden als het aantal aangemelde vondsten van de Jutter.
        """;

    /** A SECOND model, and the only difference that matters: the element role declares NO {@code (mv:)}
     *  plural, the §13.3.9.1 cardinality sentence writes one ("meerdere vondsten"), and the selections
     *  navigate through the SUBJECT'S OWN role name ("van de vinder") rather than through the object
     *  type. That is the shape the toolchain's own concatenation fixtures spell, and it is where the
     *  by-name binding runs out: measured 2026-09-22, the model registers the cardinality sentence's
     *  plural as MULTIPLICITY and drops the spelling, so {@code elementRoleBinding} — which binds only a
     *  declared singular or a declared plural, never a guessed one — finds nothing.
     *
     *  <p>Registering that plural instead was measured and rejected: it would move a golden's bytes and
     *  destroy {@code jutterswet-kaveltelling}, the WF-034 witness whose whole point is that such a
     *  plural does NOT bind and the type pair carries the fold. So the union derives the relation from
     *  the TYPE PAIR — what the one-selection count has always leaned on — and writes it out
     *  explicitly. WF-110 (e) is unmoved: it forbids the WIRE from omitting the relation, not the
     *  producer from deriving the one it writes. */
    static final String CARDINALITY_MODEL = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            het aantal strandvondsten Numeriek (niet-negatief geheel getal);

        Objecttype de Vondst (mv: Vondsten)
            is gemerkt kenmerk (bijvoeglijk);
            is gekeurd kenmerk (bijvoeglijk);

        Feittype vondstmelding
            de vinder Jutter
            de vondst Vondst
        Eén vinder meldt meerdere vondsten

        Regel aantal strandvondsten
            geldig altijd
                Het aantal strandvondsten van een Jutter moet berekend worden als het aantal vondsten van de vinder die gemerkt zijn en vondsten van de vinder die gekeurd zijn.
        """;

    @Test
    void the_cardinality_plural_with_own_role_name_binds_via_the_type_pair() {
        RegelSpraakModel m = Resolver.resolve(CARDINALITY_MODEL);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        Expr.FunctionCall f = assertInstanceOf(Expr.FunctionCall.class, m.resolvedRegels.get(0).value());
        assertEquals(1, f.arguments().size(), "de unie is ÉÉN argument");
        Expr.InstanceSetUnion u = assertInstanceOf(Expr.InstanceSetUnion.class, f.arguments().get(0));
        assertEquals(2, u.branches().size());
        // the derived relation is written out per selection — the wire never omits it (WF-110 (e))
        for (Expr.InstanceSetUnion.Branch t : u.branches()) {
            assertNotNull(t.sourceRole(), "elke selectie noemt haar relatie op de wire");
            assertEquals("vondstmelding", t.sourceRole().factType());
            assertEquals("vondst", t.sourceRole().role(), "de wire noemt de GEDECLAREERDE enkelvoudsrol");
            assertEquals("Vondst", t.elementType().naam);
        }
        assertEquals("gemerkt", assertInstanceOf(dev.uchar.regelspraak.ir.Condition.CharacteristicCheck.class,
                u.branches().get(0).filter()).characteristic());
        assertEquals("gekeurd", assertInstanceOf(dev.uchar.regelspraak.ir.Condition.CharacteristicCheck.class,
                u.branches().get(1).filter()).characteristic());
    }

    /** The narrowing the fallback must NOT have loosened: a by-NAME binding still refuses a guessed
     *  plural. Here two fact types relate Jutter to Vondst, so the type pair is ambiguous and there is
     *  nothing to fall back to — the selection is refused by name, exactly as before. Without this row
     *  the fix could have been "accept the best-effort plural", which would bind a spelling the model
     *  never defined. */
    @Test
    void an_ambiguous_type_pair_still_falls_on_the_by_name_refusal() {
        List<Diagnostic> ds = Resolver.resolve(CARDINALITY_MODEL.replace(
                "Regel aantal strandvondsten",
                """
                Feittype vondstberging
                    de berger Jutter
                    de geborgen vondst Vondst
                Eén berger bergt meerdere geborgen vondsten

                Regel aantal strandvondsten""")).errors();
        assertEquals(1, ds.size(), () -> "verwacht één fout, kreeg: " + ds);
        assertTrue(ds.get(0).message().contains("niet eenduidig af te leiden"),
                () -> "de melding noemt de grond niet: " + ds.get(0).message());
        assertTrue(ds.get(0).message().contains("WF-110 (e)"), () -> ds.get(0).message());
    }

    private static RegelSpraakModel clean(String regels) {
        RegelSpraakModel m = Resolver.resolve(MODEL + regels);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        return m;
    }

    private static Expr.InstanceSetUnion unionOf(RegelSpraakModel m) {
        Expr.FunctionCall f = assertInstanceOf(Expr.FunctionCall.class, m.resolvedRegels.get(0).value());
        assertEquals(Expr.FunctionKind.AANTAL, f.kind());
        assertEquals(1, f.arguments().size(), "de unie is ÉÉN argument, niet een operandenlijst");
        assertNull(f.elementObjectType(), "de telling boven een unie noemt geen eigen bron (WF-110 (c))");
        assertNull(f.filter(), "de selecties dragen hun eigen filter (WF-110 (c))");
        return assertInstanceOf(Expr.InstanceSetUnion.class, f.arguments().get(0));
    }

    private static List<Diagnostic> faults(String regels) {
        return Resolver.resolve(MODEL + regels).errors();
    }

    @Test
    void the_concatenation_over_two_relations_becomes_a_union() {
        Expr.InstanceSetUnion u = unionOf(clean(TWO_RELATIONS));
        assertEquals(2, u.branches().size());
        assertEquals("Vondst", u.branches().get(0).elementType().naam);
        assertEquals("Vondst", u.branches().get(1).elementType().naam);
        // the two selections walk DIFFERENT relations: this is the shape the OR-filter form cannot
        // express, and the reason the union node exists rather than a composite filter
        assertEquals("vondstaanmelding", u.branches().get(0).sourceRole().factType());
        assertEquals("aangemelde vondst", u.branches().get(0).sourceRole().role());
        assertEquals("vondstberging", u.branches().get(1).sourceRole().factType());
        assertEquals("geborgen vondst", u.branches().get(1).sourceRole().role());
        assertNull(u.branches().get(0).filter());
        assertNull(u.branches().get(1).filter());
    }

    @Test
    void the_concatenation_over_characteristic_filters_carries_its_filters_per_branch() {
        Expr.InstanceSetUnion u = unionOf(clean(TWO_CHARACTERISTICS));
        assertEquals(2, u.branches().size());
        // one relation, two filters — and the filters sit on the SELECTIONS, never OR-ed onto the
        // count, which is what keeps one written form on one wire form
        assertEquals("vondstaanmelding", u.branches().get(0).sourceRole().factType());
        assertEquals("vondstaanmelding", u.branches().get(1).sourceRole().factType());
        assertEquals("gemerkt",
                assertInstanceOf(dev.uchar.regelspraak.ir.Condition.CharacteristicCheck.class,
                        u.branches().get(0).filter()).characteristic());
        assertEquals("gekeurd",
                assertInstanceOf(dev.uchar.regelspraak.ir.Condition.CharacteristicCheck.class,
                        u.branches().get(1).filter()).characteristic());
    }

    @Test
    void a_bare_selection_beside_a_filtered_one_stays_carried() {
        // the DIVERGENCE from the v0 toolchain, deliberately: its marker there is a property of the
        // OR-filter lowering, not of the operation, and our wire carries the selections themselves
        Expr.InstanceSetUnion u = unionOf(clean(BARE_BESIDE_FILTERED));
        assertEquals(2, u.branches().size());
        assertNotNull(u.branches().get(0).filter(), "de eerste selectie draagt haar kenmerkfilter");
        assertNull(u.branches().get(1).filter(), "een kale selectie is hier de gewone vorm");
    }

    @Test
    void the_comma_spelling_builds_three_branches() {
        Expr.InstanceSetUnion u = unionOf(clean(COMMA_TRIPLE));
        assertEquals(3, u.branches().size(), "de komma-spelling verloor een selectie");
        assertEquals("vondstberging", u.branches().get(2).sourceRole().factType());
    }

    @Test
    void the_union_reaches_the_wire_with_its_own_node_name() {
        String json = IrEmitter.emit(clean(TWO_RELATIONS));
        assertTrue(json.contains("\"node\": \"InstanceSetUnion\""), () -> "geen unie op de wire:\n" + json);
        assertTrue(json.contains("\"node\": \"InstanceSetBranch\""), () -> "geen selectie op de wire:\n" + json);
        // every selection names its element type, and the count over the union names none of its own
        assertEquals(json.split("\"node\": \"InstanceSetBranch\"", -1).length - 1,
                json.split("\"elementType\"", -1).length - 1,
                () -> "de telling draagt nog een eigen elementtype:\n" + json);
        // the node denotes INSTANCES and carries no datatype: a "type" key on the union would be an
        // annotation nobody derives, and the verifier's type prologue exempts it for exactly that
        int begin = json.indexOf("\"node\": \"InstanceSetUnion\"");
        int end = json.indexOf("\"branches\"", begin);
        assertTrue(begin >= 0 && end > begin, () -> "geen unie-kop op de wire:\n" + json);
        assertFalse(json.substring(begin, end).contains("\"type\""),
                () -> "de unie draagt een type:\n" + json.substring(begin, end));
    }

    @Test
    void differing_element_types_are_refused_by_name() {
        List<Diagnostic> ds = faults(NOT_EQUAL_ELEMENT_TYPES);
        assertEquals(1, ds.size(), () -> "verwacht één fout, kreeg: " + ds);
        assertEquals(Severity.ERROR, ds.get(0).severity());
        String msg = ds.get(0).message();
        assertTrue(msg.contains("verschillende populaties"), () -> "de melding noemt de grond niet: " + msg);
        assertTrue(msg.contains("'Vondst'") && msg.contains("'Kavel'"),
                () -> "de melding noemt de twee populaties niet: " + msg);
        assertTrue(msg.contains("WF-110 (d)"), () -> "de melding citeert de regel niet: " + msg);
    }

    @Test
    void an_unsolvable_subselection_is_refused_by_name() {
        List<Diagnostic> ds = faults(UNSOLVABLE_SUBSELECTION);
        assertEquals(1, ds.size(), () -> "verwacht één fout, kreeg: " + ds);
        String msg = ds.get(0).message();
        assertTrue(msg.contains("lost niet op naar een kenmerk"),
                () -> "de melding noemt de grond niet: " + msg);
        assertTrue(msg.contains("selectie 1"), () -> "de melding wijst de selectie niet aan: " + msg);
    }

    @Test
    void the_value_operands_keep_their_own_refusal() {
        // the half that did not move: the arity refusal, in the verifier's WF-037 sentence
        List<Diagnostic> ds = faults(VALUE_OPERANDS);
        assertEquals(1, ds.size(), () -> "verwacht één fout, kreeg: " + ds);
        String msg = ds.get(0).message();
        assertTrue(msg.contains("this node carries 2 operands"), () -> "de melding telt de operanden niet: " + msg);
        assertTrue(msg.contains("a count folds INSTANCES and has no LISTED form"),
                () -> "de melding noemt de vorm niet: " + msg);
        assertTrue(msg.contains("the only reading an operand list leaves a count is the number of FILLED operands"),
                () -> "de melding is de ariteitszin van de verifier niet: " + msg);
        // value operands open no union route, so the Dutch context carries no union reason
        assertFalse(msg.contains("geen unie van selecties"),
                () -> "de melding noemt een unie-reden waar geen unie-route opende: " + msg);
    }

    @Test
    void the_ordinary_count_stays_clean() {
        Expr.FunctionCall f = assertInstanceOf(Expr.FunctionCall.class, clean(ORDINARY_COUNT).resolvedRegels.get(0).value());
        assertEquals(Expr.FunctionKind.AANTAL, f.kind());
        assertEquals(1, f.arguments().size());
        assertNotNull(f.elementObjectType(), "de eenargumentstelling verloor haar elementtype");
        assertFalse(f.arguments().get(0) instanceof Expr.InstanceSetUnion,
                "een enkele selectie is geen unie — die vorm heeft haar eigen spelling (WF-110 (b))");
    }

    /** WF-110 (a) restricts the UNION NODE to one position; the COUNT over it is an ordinary whole
     *  number and stands wherever any other count stands — a `voorwaarde` included. The clause was
     *  readable as forbidding that, so it is pinned here and by the golden
     *  `jutterswet-unievoorwaarde` rather than left to a reading. */
    static final String IN_CONDITION = """

        Regel ruim voorzien
            geldig altijd
                Een Jutter is ruim voorzien indien het aantal aangemelde vondsten van de Jutter en geborgen vondsten van de Jutter groter is dan 2.
        """;

    @Test
    void the_union_count_is_also_in_a_condition() {
        RegelSpraakModel m = clean(IN_CONDITION);
        var v = assertInstanceOf(dev.uchar.regelspraak.ir.Condition.Comparison.class,
                m.resolvedRegels.get(0).condition());
        Expr.FunctionCall count = assertInstanceOf(Expr.FunctionCall.class, v.left());
        assertEquals(Expr.FunctionKind.AANTAL, count.kind());
        assertEquals(2, assertInstanceOf(Expr.InstanceSetUnion.class, count.arguments().get(0)).branches().size());
    }

    /** A refused concatenation used to raise a "die/dat wordt nog niet omgezet" WARNING even when no
     *  operand wrote a subselectie at all — a diagnosis about something the sentence does not carry.
     *  The guard asks the question only where there is one; a sentence WITH an unresolvable
     *  subselectie keeps both its ERROR and its WARNING, which is the other half of the same pin. */
    @Test
    void a_refused_union_without_subselection_gets_no_subselection_warning() {
        List<Diagnostic> ds = Resolver.resolve(MODEL + NOT_EQUAL_ELEMENT_TYPES).diagnostics;
        assertEquals(1, ds.size(), () -> "verwacht alleen de benoemde ERROR, kreeg: " + ds);
        assertEquals(Severity.ERROR, ds.get(0).severity());
        List<Diagnostic> withThat = Resolver.resolve(MODEL + UNSOLVABLE_SUBSELECTION).diagnostics;
        assertEquals(2, withThat.size(), () -> "een echte deelselectie hoort haar WARNING te houden: " + withThat);
        // the COUNT alone would pass on any second diagnosis whatever, so pin which one it is: the
        // surviving non-error must be the die/dat WARNING and nothing else
        List<Diagnostic> warnings = withThat.stream()
                .filter(d -> d.severity() == Severity.WARNING).toList();
        assertEquals(1, warnings.size(), () -> "verwacht precies één WARNING: " + withThat);
        assertTrue(warnings.get(0).message().contains("'die/dat'-subselectie op een aggregatie-operand"),
                () -> "de overblijvende WARNING is niet de deelselectie-melding: " + warnings.get(0));
        assertEquals(1, withThat.stream().filter(d -> d.severity() == Severity.ERROR).count(),
                () -> "verwacht precies één ERROR: " + withThat);
    }

    @Test
    void a_refused_union_does_not_reach_the_wire() {
        RegelSpraakModel m = Resolver.resolve(MODEL + NOT_EQUAL_ELEMENT_TYPES);
        assertFalse(m.isOk(), "de geweigerde unie levert geen fout op");
        // the DIAGNOSTIC names the node — it says which route did not open — so the test asks after
        // the node's own wire spelling, not after the word; the ERROR is set aside for that reading
        // ({@link Emission}), since the emitter refuses the model itself (WF-113 (a))
        assertFalse(Emission.withoutErrors(m).contains("\"node\": \"InstanceSetUnion\""),
                "een geweigerde unie hoort geen knoop op de wire achter te laten");
    }
}
