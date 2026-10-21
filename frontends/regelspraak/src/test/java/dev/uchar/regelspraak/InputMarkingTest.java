package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.ir.Default;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.Validity;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.ir.ResolvedConsistency;
import dev.uchar.regelspraak.ir.ResolvedRule;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The opgave marker (grammar/DIVERGENCES.md §7): {@code (mag opgegeven worden)} as the last element of an attribute
 * declaration. For a marked attribute X of object type T the resolver lowers five pieces onto structure the wire
 * already carries: the input attribute {@code opgegeven X}; the attribute {@code berekende X}, which every computing
 * writer of X now targets; one defaults group {@code opgave boven berekening X} on X with the opgave default above
 * the computation default and an authored initialisation of X as the lowest default; the possessive characteristic
 * {@code afwijkende opgave X}, attributed by the rule {@code afwijking opgave X}; and the consistency
 * {@code controle opgave X}. Measured on the frontend before this card: every document below that writes the
 * marker was a syntax error at the marker ("extraneous input '(' expecting {&lt;EOF&gt;, …}"), and a rule reading
 * {@code afwijkende opgave X} or {@code berekende X} kept a marker on the wire with a WARNING, so 28 of the 30
 * witnesses fail there; the two inertness witnesses pass there and must keep passing.
 */
class InputMarkingTest {

    /** The marked declaration stands on line 3. */
    private static final String VONDST = """
        Objecttype de Vondst (mv: Vondsten)
            de taxatie Numeriek (getal met 2 decimalen);
            de waarde Numeriek (getal met 2 decimalen) (mag opgegeven worden);
        """;

    private static final String WAARDERING = """

        Regel waardering
            geldig altijd
                De waarde van een Vondst moet berekend worden als de taxatie van de Vondst verminderd met 5,00.
        """;

    private static final String STANDAARDWAARDE = """

        // Bron: standaardwaarde
        Regel standaardwaarde
            geldig altijd
                De waarde van een Vondst moet geïnitialiseerd worden op 25,00.
        """;

    /** A marked Datum in dagen, computed from another date and initialised with a date literal; the marked
     *  declaration stands on line 3. */
    private static final String VONDST_DATE = """
        Objecttype de Vondst (mv: Vondsten)
            de vinddag Datum in dagen;
            de meldingsdatum Datum in dagen (mag opgegeven worden);

        Regel melding na de vinddag
            geldig altijd
                De meldingsdatum van een Vondst moet berekend worden als de vinddag van de Vondst plus 3 dagen.

        // Bron: seizoensopening
        Regel melding bij onbekende vinddag
            geldig altijd
                De meldingsdatum van een Vondst moet geïnitialiseerd worden op 01-03-2026.
        """;

    private static String compact(String s) {
        return s.replaceAll("\\s+", "");
    }

    private static Diagnostic onlyDiagnostic(RegelSpraakModel m) {
        assertEquals(1, m.diagnostics.size(), () -> "verwachtte precies één diagnose, kreeg: " + m.diagnostics);
        return m.diagnostics.get(0);
    }

    private static Diagnostic onlyError(RegelSpraakModel m) {
        assertEquals(1, m.diagnostics.size(), () -> "verwachtte precies één diagnose, kreeg: " + m.diagnostics);
        Diagnostic d = m.diagnostics.get(0);
        assertEquals(Severity.ERROR, d.severity(), () -> "verwachtte een ERROR: " + d);
        return d;
    }

    private static ResolvedRule rule(RegelSpraakModel m, String name) {
        return m.resolvedRules.stream().filter(r -> r.name().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("geen regel '" + name + "' in " + m.resolvedRules.stream()
                        .map(ResolvedRule::name).toList()));
    }

    private static PrioritizedDefaults group(RegelSpraakModel m, String name) {
        return m.prioritizedDefaults.stream().filter(g -> g.name().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("geen defaults-groep '" + name + "' in " + m.prioritizedDefaults
                        .stream().map(PrioritizedDefaults::name).toList()));
    }

    private static String targetOf(ResolvedRule r) {
        return ((Expr.AttributeReference) r.target()).attribute();
    }

    private static String read(Expr e) {
        return ((Expr.AttributeReference) e).attribute();
    }

    // -------------------------------------------------------------------- the five pieces of the lowering

    @Test void the_marking_builds_the_five_pieces() {
        RegelSpraakModel m = Resolver.resolve(VONDST + WAARDERING);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        ObjectType v = m.objectTypes.get("Vondst");

        // 1 and 2: the two attributes, right after X, with X's datatype
        assertEquals(List.of("taxatie", "waarde", "opgegeven waarde", "berekende waarde"),
                List.copyOf(v.attributes.keySet()));
        assertEquals(v.attributes.get("waarde"), v.attributes.get("opgegeven waarde"));
        assertEquals(v.attributes.get("waarde"), v.attributes.get("berekende waarde"));

        // 2: the computing writer keeps its name and shape and targets berekende X
        ResolvedRule w = rule(m, "waardering");
        assertEquals("berekende waarde", targetOf(w));
        assertEquals(ResolvedRule.Kind.BEREKENING, w.kind());
        assertEquals("taxatie", read(((Expr.Binary) w.value()).left()));

        // 3: one group on X, from the top: the opgave, then the computation
        assertEquals(1, m.prioritizedDefaults.size(), "één defaults-groep");
        PrioritizedDefaults g = group(m, "opgave boven berekening waarde");
        assertEquals("Vondst", g.subject());
        assertEquals("waarde", g.attribute());
        assertNull(g.characteristic());
        assertFalse(g.unordered());
        assertEquals(Validity.ALTIJD, g.validity());
        assertNull(g.source(), "de gegenereerde groep draagt geen statuutannotatie");
        assertEquals(2, g.defaults().size());
        assertEquals(List.of("opgegeven waarde", "berekende waarde"),
                g.defaults().stream().map(d -> read(d.consequence())).toList(), "van hoog naar laag");
        assertEquals(List.of("opgegeven waarde", "berekende waarde"),
                g.defaults().stream().map(Default::source).toList(), "elke default draagt zijn bloknaam");
        for (Default d : g.defaults()) {
            Condition.Compound always = assertInstanceOf(Condition.Compound.class, d.condition());
            assertEquals(Condition.Quantifier.ALLE, always.quantifier());
            assertTrue(always.components().isEmpty());
            assertEquals("altijd", always.source().snippet());
            assertFalse(d.initialisation());
            assertNull(d.decision());
            assertNull(d.exceptions());
        }

        // 4: the possessive characteristic and the rule that attributes it
        assertEquals(List.of("afwijkende opgave waarde"), v.characteristics);
        assertEquals(ObjectType.CharacteristicKind.BEZITTELIJK, v.characteristicKinds.get("afwijkende opgave waarde"));
        ResolvedRule a = rule(m, "afwijking opgave waarde");
        assertEquals(ResolvedRule.Kind.KENMERKTOEKENNING, a.kind());
        assertEquals("afwijkende opgave waarde", targetOf(a));
        assertEquals(Validity.ALTIJD, a.validity());
        Condition.Compound all = assertInstanceOf(Condition.Compound.class, a.condition());
        assertEquals(Condition.Quantifier.ALLE, all.quantifier());
        assertEquals(3, all.components().size());
        Condition.Predicate p0 = assertInstanceOf(Condition.Predicate.class, all.components().get(0));
        Condition.Predicate p1 = assertInstanceOf(Condition.Predicate.class, all.components().get(1));
        Condition.Comparison p2 = assertInstanceOf(Condition.Comparison.class, all.components().get(2));
        assertEquals(Condition.PredicateKind.GEVULD, p0.kind());
        assertEquals("opgegeven waarde", read(p0.operand()));
        assertEquals(Condition.PredicateKind.GEVULD, p1.kind());
        assertEquals("berekende waarde", read(p1.operand()));
        assertEquals(Condition.Operator.ONGELIJK, p2.op());
        assertEquals("opgegeven waarde", read(p2.left()));
        assertEquals("berekende waarde", read(p2.right()));

        // 5: the consistency, its assertion the negated characteristic, at the marked declaration
        assertEquals(1, m.consistencies.size());
        ResolvedConsistency c = m.consistencies.get(0);
        assertEquals("controle opgave waarde", c.name());
        assertEquals("Vondst", c.subject().name);
        Condition.CharacteristicCheck k = assertInstanceOf(Condition.CharacteristicCheck.class, c.assertion());
        assertEquals("afwijkende opgave waarde", k.characteristic());
        assertTrue(k.negation());
        assertEquals(3, c.line());
        assertEquals(Validity.ALTIJD, c.validity());

        // the order of the rule list: the authored rule where it stood, the generated one behind it
        assertEquals(List.of("waardering", "afwijking opgave waarde"),
                m.resolvedRules.stream().map(ResolvedRule::name).toList());
    }

    @Test void the_wire_carries_the_lowering_in_the_spelling_of_the_written_out_pattern() {
        RegelSpraakModel m = Resolver.resolve(VONDST + WAARDERING);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        String ir = IrEmitter.emit(m);
        Report.assertAccountedFor(ir);
        String c = compact(ir);
        String group = compact("""
            {"node": "PrioritizedDefaults", "name": "opgave boven berekening waarde", "subject": "Vondst",
             "attribute": "waarde", "defaults": [
               {"node": "Default",
                "justification": {"node": "Compound", "quantifier": "all", "threshold": 0, "parts": [],
                                  "source": {"snippet": "altijd"}},
                "consequence": {"node": "AttributeReference", "attribute": "opgegeven waarde", "subjectType": "Vondst",
                                "source": {"snippet": "zijn opgegeven waarde"}},
                "source": {"snippet": "opgegeven waarde"}},
               {"node": "Default",
                "justification": {"node": "Compound", "quantifier": "all", "threshold": 0, "parts": [],
                                  "source": {"snippet": "altijd"}},
                "consequence": {"node": "AttributeReference", "attribute": "berekende waarde", "subjectType": "Vondst",
                                "source": {"snippet": "zijn berekende waarde"}},
                "source": {"snippet": "berekende waarde"}}],
             "validity": {"node": "Validity"},
             "origin": {"form": "inputAboveComputation", "from": {"kind": "attribute", "owner": "Vondst", "name": "waarde"}}}""");
        assertTrue(c.contains(group), () -> "de groep staat niet zo op de wire:\n" + ir);
        String rule = compact("""
            {"node": "Rule", "name": "afwijking opgave waarde", "versions": [
              {"validity": {"node": "Validity"},
               "target": {"node": "AttributeReference", "attribute": "afwijkende opgave waarde", "subjectType": "Vondst",
                          "source": {"snippet": "Een Vondst heeft afwijkende opgave waarde"}},
               "value": {"node": "Literal", "value": {"boolean": true}, "type": {"node": "Boolean"},
                         "source": {"snippet": "waar"}},
               "condition": {"node": "Compound", "quantifier": "all", "threshold": 0, "parts": [
                   {"node": "Predicate", "kind": "filled",
                    "operand": {"node": "AttributeReference", "attribute": "opgegeven waarde", "subjectType": "Vondst",
                                "source": {"snippet": "zijn opgegeven waarde"}},
                    "negated": false, "digits": 0, "source": {"snippet": "zijn opgegeven waarde is gevuld"}},
                   {"node": "Predicate", "kind": "filled",
                    "operand": {"node": "AttributeReference", "attribute": "berekende waarde", "subjectType": "Vondst",
                                "source": {"snippet": "zijn berekende waarde"}},
                    "negated": false, "digits": 0, "source": {"snippet": "zijn berekende waarde is gevuld"}},
                   {"node": "Comparison", "op": "notEqual",
                    "left": {"node": "AttributeReference", "attribute": "opgegeven waarde", "subjectType": "Vondst",
                             "source": {"snippet": "zijn opgegeven waarde"}},
                    "right": {"node": "AttributeReference", "attribute": "berekende waarde", "subjectType": "Vondst",
                              "source": {"snippet": "zijn berekende waarde"}},
                    "source": {"snippet": "zijn opgegeven waarde is ongelijk aan zijn berekende waarde"}}],
                 "source": {"snippet": "hij aan alle volgende voorwaarden voldoet: - zijn opgegeven waarde is gevuld - zijn berekende waarde is gevuld - zijn opgegeven waarde is ongelijk aan zijn berekende waarde."}},
               "initialisation": false}],
             "origin": {"form": "inputAboveComputation", "from": {"kind": "attribute", "owner": "Vondst", "name": "waarde"}}}""");
        assertTrue(c.contains(rule), () -> "de afwijkingsregel staat niet zo op de wire:\n" + ir);
        String consistency = compact("""
            {"node": "Consistency", "name": "controle opgave waarde", "versions": [
              {"subject": "Vondst",
               "assertion": {"node": "CharacteristicCheck", "characteristic": "afwijkende opgave waarde",
                             "negated": true, "source": {"snippet": "hij heeft geen afwijkende opgave waarde"}},
               "validity": {"node": "Validity"},
               "source": {"snippet": "de waarde Numeriek (getal met 2 decimalen) (mag opgegeven worden)", "line": 3}}],
             "origin": {"form": "inputAboveComputation", "from": {"kind": "attribute", "owner": "Vondst", "name": "waarde"}}}""");
        assertTrue(c.contains(consistency), () -> "de consistentie staat niet zo op de wire:\n" + ir);
        String members = compact("""
            {"node": "Attribute", "name": "opgegeven waarde",
             "datatype": {"node": "Number", "whole": false, "decimals": 2, "sign": "unspecified"},
             "origin": {"form": "inputAboveComputation", "from": {"kind": "attribute", "owner": "Vondst", "name": "waarde"}}},
            {"node": "Attribute", "name": "berekende waarde",
             "datatype": {"node": "Number", "whole": false, "decimals": 2, "sign": "unspecified"},
             "origin": {"form": "inputAboveComputation", "from": {"kind": "attribute", "owner": "Vondst", "name": "waarde"}}}],
            "characteristics": [{"node": "Characteristic", "name": "afwijkende opgave waarde", "kind": "possessive",
             "origin": {"form": "inputAboveComputation", "from": {"kind": "attribute", "owner": "Vondst", "name": "waarde"}}}]""");
        assertTrue(c.contains(members), () -> "de gegenereerde leden staan niet zo op de wire:\n" + ir);
        assertTrue(c.contains(compact("""
            "target": {"node": "AttributeReference", "attribute": "berekende waarde", "subjectType": "Vondst",
                       "source": {"snippet": "De waarde van een Vondst"}}""")),
                () -> "de rekenregel schrijft niet 'berekende waarde' met zijn eigen brontekst:\n" + ir);
    }

    @Test void the_initialisation_stays_on_the_target_as_lowest_default() {
        RegelSpraakModel m = Resolver.resolve(VONDST + WAARDERING + STANDAARDWAARDE);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        PrioritizedDefaults g = group(m, "opgave boven berekening waarde");
        assertEquals("waarde", g.attribute());
        assertEquals(3, g.defaults().size());
        Default init = g.defaults().get(2);
        assertTrue(init.initialisation(), "de initialisatie is de laagste default");
        assertNull(init.condition(), "een initialisatiedefault draagt geen geschreven rechtvaardiging");
        assertEquals("25,00", ((Expr.Literal) init.consequence()).valueText());
        assertEquals("standaardwaarde", init.source(), "de statuutannotatie van de initialisatieregel");
        // card defaults-decider: the authored initialisation keeps its rule name, the generated defaults have none
        assertEquals("standaardwaarde", init.name(), "de naam van de initialisatieregel");
        assertNull(g.defaults().get(0).name(), "de opgave-default is gegenereerd en draagt geen naam");
        assertNull(g.defaults().get(1).name(), "de rekendefault is gegenereerd en draagt geen naam");
        assertFalse(g.defaults().get(0).initialisation());
        assertFalse(g.defaults().get(1).initialisation());
        assertEquals(List.of("waardering", "afwijking opgave waarde"),
                m.resolvedRules.stream().map(ResolvedRule::name).toList(),
                "de initialisatieregel is opgenomen in de groep en staat niet meer als regel");
        String c = compact(IrEmitter.emit(m));
        assertTrue(c.contains(compact("""
            {"node": "Default", "name": "standaardwaarde",
             "consequence": {"node": "Literal", "value": {"number": "25.00"},
                             "type": {"node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"},
                             "source": {"snippet": "25,00"}},
             "initialisation": true, "source": {"snippet": "standaardwaarde"}}],""")), c);
    }

    @Test void on_a_date_the_marking_builds_the_same_five_pieces() {
        RegelSpraakModel m = Resolver.resolve(VONDST_DATE);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        ObjectType v = m.objectTypes.get("Vondst");

        // 1 and 2: the two attributes right after X, each with X's datatype
        assertEquals(List.of("vinddag", "meldingsdatum", "opgegeven meldingsdatum", "berekende meldingsdatum"),
                List.copyOf(v.attributes.keySet()));
        assertEquals(new Datatype.DateInDays(), v.attributes.get("meldingsdatum"));
        assertEquals(new Datatype.DateInDays(), v.attributes.get("opgegeven meldingsdatum"));
        assertEquals(new Datatype.DateInDays(), v.attributes.get("berekende meldingsdatum"));

        // 2: the computation from another date keeps its name and targets berekende X
        ResolvedRule w = rule(m, "melding na de vinddag");
        assertEquals("berekende meldingsdatum", targetOf(w));
        assertEquals("vinddag", read(((Expr.Binary) w.value()).left()));

        // 3: the opgave, the computation, and the initialisation with its date literal as the lowest default
        PrioritizedDefaults g = group(m, "opgave boven berekening meldingsdatum");
        assertEquals("meldingsdatum", g.attribute());
        assertEquals(3, g.defaults().size());
        assertEquals(List.of("opgegeven meldingsdatum", "berekende meldingsdatum"),
                g.defaults().subList(0, 2).stream().map(d -> read(d.consequence())).toList(), "van hoog naar laag");
        Default init = g.defaults().get(2);
        assertTrue(init.initialisation(), "de initialisatie is de laagste default");
        assertNull(init.condition(), "een initialisatiedefault draagt geen geschreven rechtvaardiging");
        Expr.Literal date = assertInstanceOf(Expr.Literal.class, init.consequence());
        assertEquals("2026-03-01", date.valueText(), "de datum zoals de wire haar schrijft; de bron houdt 01-03-2026");
        assertEquals(new Datatype.DateInDays(), date.type());
        assertEquals("seizoensopening", init.source(), "de statuutannotatie van de initialisatieregel");

        // 4 and 5: the characteristic compares the two dates, the consistency asserts it absent
        assertEquals(List.of("afwijkende opgave meldingsdatum"), v.characteristics);
        Condition.Compound all = assertInstanceOf(Condition.Compound.class,
                rule(m, "afwijking opgave meldingsdatum").condition());
        Condition.Comparison notEqual = assertInstanceOf(Condition.Comparison.class, all.components().get(2));
        assertEquals(Condition.Operator.ONGELIJK, notEqual.op());
        assertEquals("opgegeven meldingsdatum", read(notEqual.left()));
        assertEquals("berekende meldingsdatum", read(notEqual.right()));
        assertEquals(List.of("controle opgave meldingsdatum"),
                m.consistencies.stream().map(ResolvedConsistency::name).toList());
        assertEquals(List.of("melding na de vinddag", "afwijking opgave meldingsdatum"),
                m.resolvedRules.stream().map(ResolvedRule::name).toList(),
                "de initialisatieregel is opgenomen in de groep en staat niet meer als regel");
    }

    @Test void on_a_date_carries_the_wire_date_type_and_date_literal() {
        RegelSpraakModel m = Resolver.resolve(VONDST_DATE);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        String ir = IrEmitter.emit(m);
        Report.assertAccountedFor(ir);
        String c = compact(ir);
        assertTrue(c.contains(compact("""
            {"node": "Attribute", "name": "opgegeven meldingsdatum",
             "datatype": {"node": "Date", "precision": "day"},
             "origin": {"form": "inputAboveComputation", "from": {"kind": "attribute", "owner": "Vondst", "name": "meldingsdatum"}}},
            {"node": "Attribute", "name": "berekende meldingsdatum",
             "datatype": {"node": "Date", "precision": "day"},
             "origin": {"form": "inputAboveComputation", "from": {"kind": "attribute", "owner": "Vondst", "name": "meldingsdatum"}}}""")),
                () -> "de gegenereerde attributen dragen het datumtype niet:\n" + ir);
        assertTrue(c.contains(compact("""
            {"node": "Default", "name": "melding bij onbekende vinddag",
             "consequence": {"node": "Literal", "value": {"date": "2026-03-01"},
                             "type": {"node": "Date", "precision": "day"}, "source": {"snippet": "01-03-2026"}},
             "initialisation": true, "source": {"snippet": "seizoensopening"}}],""")),
                () -> "de initialisatiedefault draagt de datumliteral niet zo:\n" + ir);
    }

    @Test void readers_of_the_target_stay_to_read_the_target() {
        RegelSpraakModel m = Resolver.resolve(VONDST.replace("(mv: Vondsten)\n",
                "(mv: Vondsten)\n    de heffing Numeriek (getal met 2 decimalen);\n") + WAARDERING + """

            Regel heffing
                geldig altijd
                    De heffing van een Vondst moet berekend worden als de waarde van de Vondst maal 2.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        ResolvedRule h = rule(m, "heffing");
        assertEquals("heffing", targetOf(h));
        assertEquals("waarde", read(((Expr.Binary) h.value()).left()), "de lezer leest het doel zelf");
    }

    @Test void the_generated_characteristic_and_the_computed_value_are_readable() {
        RegelSpraakModel m = Resolver.resolve(VONDST.replace("(mv: Vondsten)\n",
                "(mv: Vondsten)\n    is ter controle kenmerk (bijvoeglijk);\n    de korting Numeriek (getal met 2 decimalen);\n")
                + WAARDERING + """

            Regel controle
                geldig altijd
                    Een Vondst is ter controle
                    indien hij afwijkende opgave waarde heeft.

            Regel korting
                geldig altijd
                    De korting van een Vondst moet berekend worden als de taxatie van de Vondst min zijn berekende waarde.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        Condition.CharacteristicCheck k = assertInstanceOf(Condition.CharacteristicCheck.class, rule(m, "controle").condition());
        assertEquals("afwijkende opgave waarde", k.characteristic());
        assertFalse(k.negation());
        assertEquals("berekende waarde", read(((Expr.Binary) rule(m, "korting").value()).right()));
    }

    @Test void an_exception_tree_moves_whole_to_the_calculation() {
        RegelSpraakModel m = Resolver.resolve(VONDST.replace("(mv: Vondsten)\n",
                "(mv: Vondsten)\n    is beschadigd kenmerk (bijvoeglijk);\n") + WAARDERING + """

            Uitzondering op waardering: waardering bij schade
                geldig altijd
                    De waarde van een Vondst moet berekend worden als de taxatie van de Vondst verminderd met 10,00
                    indien hij beschadigd is.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertEquals(List.of("waardering", "opgave boven berekening waarde"),
                m.prioritizedDefaults.stream().map(PrioritizedDefaults::name).toList());
        PrioritizedDefaults tree = group(m, "waardering");
        assertEquals("berekende waarde", tree.attribute(), "de boom schrijft de berekening");
        assertEquals(2, tree.defaults().size(), "naam en vorm blijven");
        assertEquals("waarde", group(m, "opgave boven berekening waarde").attribute());
        assertEquals(List.of("afwijking opgave waarde"), m.resolvedRules.stream().map(ResolvedRule::name).toList());
    }

    @Test void an_unordered_block_moves_as_set() {
        RegelSpraakModel m = Resolver.resolve(VONDST.replace("(mv: Vondsten)\n",
                "(mv: Vondsten)\n    is beschadigd kenmerk (bijvoeglijk);\n    is gerestaureerd kenmerk (bijvoeglijk);\n")
                + """

            Uitzonderingen ongeordend:
            Regel waardering bij schade
                geldig altijd
                    De waarde van een Vondst moet gesteld worden op 10,00
                    indien hij beschadigd is.

            Regel waardering na restauratie
                geldig altijd
                    De waarde van een Vondst moet gesteld worden op 30,00
                    indien hij gerestaureerd is.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertEquals(3, m.prioritizedDefaults.size());
        for (String member : List.of("waardering bij schade", "waardering na restauratie")) {
            PrioritizedDefaults g = group(m, member);
            assertEquals("berekende waarde", g.attribute(), member + " schrijft de berekening");
            assertTrue(g.unordered(), member + " blijft ongeordend");
        }
        PrioritizedDefaults input = group(m, "opgave boven berekening waarde");
        assertEquals("waarde", input.attribute());
        assertFalse(input.unordered());
    }

    @Test void a_decision_table_moves_per_row() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Jutter (mv: Jutters) (bezield)
                de bonus Numeriek (geheel getal) (mag opgegeven worden);
                het aantal geborgen wrakken Numeriek (niet-negatief geheel getal);

            Beslistabel Bonustabel
                geldig altijd
            | | de bonus van een Jutter moet gesteld worden op | indien zijn aantal geborgen wrakken groter is dan |
            |---|---|---|
            | 1 | 10 | 5 |
            | 2 | 20 | 10 |
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertEquals(List.of("berekende bonus", "berekende bonus"), m.resolvedRules.stream()
                .filter(r -> r.name().startsWith("Bonustabel")).map(InputMarkingTest::targetOf).toList());
        assertEquals("bonus", group(m, "opgave boven berekening bonus").attribute());
    }

    // -------------------------------------------------------------------- writers on a subtype (ch. 7.10)

    /** The card's reproduction: the date is declared on the supertype, where the source declares it and every subtype
     *  has it; the rule that derives it stands on the subtype, the only type that knows its operand. The marked
     *  declaration stands on line 3. */
    private static final String STRANDVONDST = """
        Objecttype de Vondst (mv: Vondsten)
            de vinddatum Datum in dagen;
            de meldingsdatum Datum in dagen (mag opgegeven worden);
        Objecttype de Strandvondst is een Vondst (mv: Strandvondsten)
            de aanspoeldatum Datum in dagen;
            is beschadigd kenmerk (bijvoeglijk);
        """;

    private static final String BARNSTEENVONDST = """
        Objecttype de Barnsteenvondst is een Strandvondst (mv: Barnsteenvondsten)
            de keurdatum Datum in dagen;
        """;

    private static final String VINDDAG = """

        Regel melding op vinddag
            geldig altijd
                De meldingsdatum van een Vondst moet gesteld worden op zijn vinddatum
                indien zijn vinddatum gevuld is.
        """;

    private static final String AANSPOELDAG = """

        Regel melding op aanspoeldag
            geldig altijd
                De meldingsdatum van een Strandvondst moet gesteld worden op zijn aanspoeldatum
                indien zijn aanspoeldatum gevuld is.
        """;

    private static ObjectType targetType(ResolvedRule r) {
        return ((Expr.AttributeReference) r.target()).objectType();
    }

    @Test void a_writer_on_a_subtype_writes_the_calculation_of_its_own_type() {
        RegelSpraakModel m = Resolver.resolve(STRANDVONDST + AANSPOELDAG);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        // the members stand on the declaring type; the subtype inherits them
        assertEquals(List.of("vinddatum", "meldingsdatum", "opgegeven meldingsdatum", "berekende meldingsdatum"),
                List.copyOf(m.objectTypes.get("Vondst").attributes.keySet()));
        assertEquals(List.of("aanspoeldatum"), List.copyOf(m.objectTypes.get("Strandvondst").attributes.keySet()));
        // the writer keeps its type: 'berekende meldingsdatum' of a Strandvondst is what its own rule computes
        ResolvedRule r = rule(m, "melding op aanspoeldag");
        assertEquals("berekende meldingsdatum", targetOf(r));
        assertEquals("Strandvondst", targetType(r).name);
        // one group, on the declaring type: each subtype inherits it whole, so the opgave tops every type's stack
        assertEquals(List.of("opgave boven berekening meldingsdatum"),
                m.prioritizedDefaults.stream().map(PrioritizedDefaults::name).toList());
        PrioritizedDefaults g = group(m, "opgave boven berekening meldingsdatum");
        assertEquals("Vondst", g.subject());
        assertEquals("meldingsdatum", g.attribute());
        assertEquals(List.of("opgegeven meldingsdatum", "berekende meldingsdatum"),
                g.defaults().stream().map(d -> read(d.consequence())).toList());
        assertEquals("Vondst", targetType(rule(m, "afwijking opgave meldingsdatum")).name);
        assertEquals("Vondst", m.consistencies.get(0).subject().name);
        assertEquals(List.of("melding op aanspoeldag", "afwijking opgave meldingsdatum"),
                m.resolvedRules.stream().map(ResolvedRule::name).toList());
        String ir = IrEmitter.emit(m);
        Report.assertAccountedFor(ir);
        assertTrue(compact(ir).contains(compact("""
            "target": {"node": "AttributeReference", "attribute": "berekende meldingsdatum", "subjectType": "Strandvondst",
                       "source": {"snippet": "De meldingsdatum van een Strandvondst"}}""")),
                () -> "de regel van het subtype schrijft niet 'berekende meldingsdatum' met zijn eigen brontekst:\n" + ir);
    }

    @Test void writers_on_the_supertype_and_the_subtype_move_each_on_their_own_type() {
        RegelSpraakModel m = Resolver.resolve(STRANDVONDST + VINDDAG + AANSPOELDAG);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        for (String[] w : new String[][] {{"melding op vinddag", "Vondst"}, {"melding op aanspoeldag", "Strandvondst"}}) {
            ResolvedRule r = rule(m, w[0]);
            assertEquals("berekende meldingsdatum", targetOf(r), w[0]);
            assertEquals(w[1], targetType(r).name, w[0]);
        }
        assertEquals(1, m.prioritizedDefaults.size(), "één defaults-groep");
        assertEquals("Vondst", group(m, "opgave boven berekening meldingsdatum").subject());
    }

    @Test void a_deviation_tree_on_the_subtype_moves_whole_to_the_calculation() {
        RegelSpraakModel m = Resolver.resolve(STRANDVONDST + VINDDAG + AANSPOELDAG + """

            Afwijking van melding op aanspoeldag: melding bij schade
                geldig altijd
                    De meldingsdatum van een Strandvondst moet gesteld worden op zijn vinddatum
                    indien hij beschadigd is.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertEquals(List.of("melding op aanspoeldag", "opgave boven berekening meldingsdatum"),
                m.prioritizedDefaults.stream().map(PrioritizedDefaults::name).toList());
        PrioritizedDefaults tree = group(m, "melding op aanspoeldag");
        assertEquals("Strandvondst", tree.subject(), "de boom blijft op het subtype");
        assertEquals("berekende meldingsdatum", tree.attribute(), "de boom schrijft de berekening");
        assertEquals(2, tree.defaults().size(), "naam en vorm blijven");
        assertEquals("berekende meldingsdatum", targetOf(rule(m, "melding op vinddag")));
        assertEquals("Vondst", group(m, "opgave boven berekening meldingsdatum").subject());
    }

    @Test void a_writer_on_a_grandchild_moves_on_its_own_type() {
        RegelSpraakModel m = Resolver.resolve(STRANDVONDST + BARNSTEENVONDST + """

            Regel melding op keurdag
                geldig altijd
                    De meldingsdatum van een Barnsteenvondst moet gesteld worden op zijn keurdatum
                    indien zijn keurdatum gevuld is.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        ResolvedRule r = rule(m, "melding op keurdag");
        assertEquals("berekende meldingsdatum", targetOf(r));
        assertEquals("Barnsteenvondst", targetType(r).name);
        assertEquals("Vondst", group(m, "opgave boven berekening meldingsdatum").subject());
    }

    @Test void the_initialisation_of_the_declaring_type_is_the_lowest_default_also_for_the_subtype() {
        RegelSpraakModel m = Resolver.resolve(STRANDVONDST + AANSPOELDAG + """

            Regel melding bij onbekende dag
                geldig altijd
                    De meldingsdatum van een Vondst moet geïnitialiseerd worden op 01-03-2026.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        PrioritizedDefaults g = group(m, "opgave boven berekening meldingsdatum");
        assertEquals(3, g.defaults().size());
        assertTrue(g.defaults().get(2).initialisation(), "de initialisatie is de laagste default");
        assertEquals(List.of("melding op aanspoeldag", "afwijking opgave meldingsdatum"),
                m.resolvedRules.stream().map(ResolvedRule::name).toList(), "de initialisatie verlaat de regellijst");
    }

    @Test void an_initialisation_on_a_subtype_stays_with_its_condition_on_the_target() {
        // ch. 7.10 folds it into the stack of the subtype with its condition, below the inherited group
        RegelSpraakModel m = Resolver.resolve(STRANDVONDST + AANSPOELDAG + """

            Regel melding bij schade zonder aanspoeldag
                geldig altijd
                    De meldingsdatum van een Strandvondst moet geïnitialiseerd worden op 01-03-2026
                    indien hij beschadigd is.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        ResolvedRule init = rule(m, "melding bij schade zonder aanspoeldag");
        assertEquals(ResolvedRule.Kind.INITIALISATIE, init.kind());
        assertEquals("meldingsdatum", targetOf(init));
        assertEquals("Strandvondst", targetType(init).name);
        assertNotNull(init.condition(), "de voorwaarde blijft");
        assertEquals(2, group(m, "opgave boven berekening meldingsdatum").defaults().size(),
                "de groep op Vondst draagt geen initialisatie");
    }

    // -------------------------------------------------------------------- inertness: the token and its words

    @Test void without_marking_nothing_changes() {
        String without = VONDST.replace(" (mag opgegeven worden)", "") + WAARDERING + """

            // de waarde (mag opgegeven worden) staat hier in commentaar
            """;
        RegelSpraakModel m = Resolver.resolve(without);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertEquals("waarde", targetOf(rule(m, "waardering")));
        assertTrue(m.prioritizedDefaults.isEmpty());
        assertTrue(m.consistencies.isEmpty());
        String ir = IrEmitter.emit(m);
        assertFalse(ir.contains("opgegeven") || ir.contains("berekende"), ir);
    }

    @Test void the_words_of_the_marking_stay_name_words_and_text() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Vondst (mv: Vondsten)
                de opgegeven stand Numeriek (geheel getal);
                de mag worden stand Numeriek (geheel getal);
                de toelichting Tekst;

            Regel toelichting
                geldig altijd
                    De toelichting van een Vondst moet gesteld worden op "(mag opgegeven worden)".
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertEquals(List.of("opgegeven stand", "mag worden stand", "toelichting"),
                List.copyOf(m.objectTypes.get("Vondst").attributes.keySet()));
        assertTrue(m.prioritizedDefaults.isEmpty());
        assertEquals("(mag opgegeven worden)", ((Expr.Literal) rule(m, "toelichting").value()).valueText());
    }

    // -------------------------------------------------------------------- refusals, each an ERROR at the declaration

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "bijvoeglijk | is getaxeerd kenmerk (bijvoeglijk) (mag opgegeven worden); | getaxeerd",
            "bezittelijk | het keurmerk kenmerk (bezittelijk) (mag opgegeven worden); | keurmerk"
    })
    void the_marking_on_a_characteristic_is_refused(String category, String declaration, String characteristic) {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Vondst (mv: Vondsten)
                de taxatie Numeriek (getal met 2 decimalen);
                %s
            """.formatted(declaration));
        Diagnostic d = onlyError(m);
        assertEquals("kenmerk '" + characteristic + "' van Vondst: '(mag opgegeven worden)' staat alleen op een attribuut — "
                + "casusinvoer op een kenmerk staat al tegen elk besluit van een defaults-groep (RUNTIME-SEMANTICS "
                + "ch. 7.8), en een opgave 'niet aanwezig' boven een berekende toekenning vraagt een opgave met drie "
                + "waarden, die de casus voor een kenmerk niet kent (dialect §7)", d.message());
        assertEquals(3, d.line());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "Tekst                                   | Tekst",
            "Numeriek (geheel getal) met eenheid €/jr | Numeriek met eenheid €/jr",
            "Percentage (getal met 2 decimalen)      | Percentage",
            "Boolean                                 | Boolean",
            "Datum en tijd in millisecondes          | Datum-tijd"
    })
    void the_marking_on_something_otherwise_than_a_number_without_unit_or_a_date_is_refused(String datatype,
                                                                                       String named) {
        RegelSpraakModel m = Resolver.resolve("""
            Eenheidsysteem Munt
                de euro (mv: euros) EUR €

            Objecttype de Vondst (mv: Vondsten)
                de opgave %s (mag opgegeven worden);
            """.formatted(datatype));
        Diagnostic d = onlyError(m);
        assertEquals("attribuut 'opgave' van Vondst: '(mag opgegeven worden)' staat alleen op een Numeriek attribuut "
                + "zonder eenheid of met één eenheid, of op een Datum in dagen, en 'opgave' is " + named + " — een "
                + "defaults-groep op een attribuut van een ander datatype of met een samengestelde eenheid is nog niet in "
                + "alle drie de uitvoerders gemeten (dialect §7)", d.message());
        assertEquals(5, d.line());
    }

    @Test void the_marking_on_a_time_dependent_attribute_is_refused() {
        RegelSpraakModel m = Resolver.resolve(VONDST.replace("(getal met 2 decimalen) (mag",
                "(getal met 2 decimalen) voor elk jaar (mag"));
        Diagnostic d = onlyError(m);
        assertEquals("attribuut 'waarde' van Vondst: '(mag opgegeven worden)' staat niet op een tijdsafhankelijk "
                + "attribuut — een defaults-groep beslist één keer en heeft geen tijdlijn om op te schrijven (WF-086) "
                + "(dialect §7)", d.message());
        assertEquals(3, d.line());
    }

    @Test void the_marking_on_a_dimensioned_attribute_is_refused() {
        RegelSpraakModel m = Resolver.resolve("""
            Dimensie het seizoen, bestaande uit de seizoenen (na het attribuut met voorzetsel in):
                1. voorjaar
                2. najaar

            Objecttype de Jutter (mv: Jutters) (bezield)
                de premie Numeriek (geheel getal) gedimensioneerd met seizoen (mag opgegeven worden);
            """);
        Diagnostic d = onlyError(m);
        assertEquals("attribuut 'premie' van Jutter: '(mag opgegeven worden)' staat niet op een gedimensioneerd "
                + "attribuut, en 'premie' is gedimensioneerd met seizoen — een defaults-groep op een gedimensioneerd "
                + "attribuut speelt nog niet in alle drie de uitvoerders (dialect §7)", d.message());
        assertEquals(6, d.line());
    }

    @Test void an_allocation_that_writes_the_target_is_refused() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Sluitkas (mv: Sluitkassen)
                de sluitpremie Numeriek (geheel getal);
                de sluitrest Numeriek (geheel getal);

            Objecttype de Jutter (mv: Jutters) (bezield)
                de strandrang Numeriek (geheel getal);
                het sluitdeel Numeriek (geheel getal) (mag opgegeven worden);

            Feittype sluitdienst
                de sluitkas Sluitkas
                de sluitjutter (mv: sluitjutters) Jutter
            Eén sluitkas heeft meerdere sluitjutters

            Regel verdeel de sluitpremie
                geldig altijd
                    De sluitpremie van een sluitkas wordt verdeeld over het sluitdeel van alle sluitjutters van de sluitkas, waarbij wordt verdeeld:
                    - op volgorde van afnemende strandrang,
                    - bij een even groot criterium in gelijke delen,
                    - afgerond op 0 decimalen naar beneden.
                    Als onverdeelde rest blijft de sluitrest van de sluitkas over.
            """);
        Diagnostic d = onlyError(m);
        assertEquals("attribuut 'sluitdeel' van Jutter: '(mag opgegeven worden)', maar de verdeling 'verdeel de "
                + "sluitpremie' schrijft 'sluitdeel' — de markering verlegt een verdeling niet naar 'berekende "
                + "sluitdeel', en naast de defaults-groep op 'sluitdeel' is zij een tweede schrijver (WF-050) "
                + "(dialect §7)", d.message());
        assertEquals(7, d.line());
    }

    @Test void an_object_creation_that_assigns_the_target_is_refused() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype het Strandrecht (mv: Strandrechten)
                de grondslag Numeriek (niet-negatief getal met 2 decimalen) (mag opgegeven worden);

            Objecttype het Wrakrecht (mv: Wrakrechten)
                de bergingsvergoeding Numeriek (niet-negatief getal met 2 decimalen);

            Feittype wrakboeking
                de boeker Wrakrecht
                het geboekt strandrecht Strandrecht
            Eén boeker boekt één geboekt strandrecht

            Regel boek een strandrecht
                geldig altijd
                    Een Wrakrecht heeft een geboekt strandrecht met grondslag gelijk aan zijn bergingsvergoeding.
            """);
        Diagnostic d = onlyError(m);
        assertEquals("attribuut 'grondslag' van Strandrecht: '(mag opgegeven worden)', maar de objectcreatie 'boek een "
                + "strandrecht' kent 'grondslag' toe bij het aanmaken — de markering verlegt die toekenning niet naar "
                + "'berekende grondslag', en naast de defaults-groep op 'grondslag' is zij een tweede schrijver "
                + "(WF-050) (dialect §7)", d.message());
        assertEquals(2, d.line());
    }

    @Test void an_initialisation_on_the_declaring_type_and_one_on_a_subtype_are_refused() {
        // measured with the marker lowered: the stack of a Strandvondst carries both, and ref, clingo and the Drools
        // exec branch give it the group's initialisation without a diagnostic; unmarked, the verifier refuses the pair
        RegelSpraakModel m = Resolver.resolve(STRANDVONDST + AANSPOELDAG + """

            Regel melding bij onbekende dag
                geldig altijd
                    De meldingsdatum van een Vondst moet geïnitialiseerd worden op 01-03-2026.

            Regel melding bij onbekende aanspoeldag
                geldig altijd
                    De meldingsdatum van een Strandvondst moet geïnitialiseerd worden op 15-03-2026.
            """);
        Diagnostic d = onlyError(m);
        assertEquals("attribuut 'meldingsdatum' van Vondst: '(mag opgegeven worden)', en 'meldingsdatum' heeft twee "
                + "initialisaties in één erflijn ('melding bij onbekende dag' voor Vondst en 'melding bij onbekende "
                + "aanspoeldag' voor Strandvondst) — de effectieve stapel van Strandvondst zou twee "
                + "initialisatiedefaults dragen, en RUNTIME-SEMANTICS ch. 7.10 kent er één; zonder de markering "
                + "weigert WF-079 dit paar (dialect §7)", d.message());
        assertEquals(3, d.line());
        assertTrue(m.prioritizedDefaults.isEmpty(), "de markering genereert niets");
    }

    @Test void initialisations_on_a_subtype_and_a_grandchild_are_refused() {
        RegelSpraakModel m = Resolver.resolve(STRANDVONDST + BARNSTEENVONDST + AANSPOELDAG + """

            Regel melding bij onbekende aanspoeldag
                geldig altijd
                    De meldingsdatum van een Strandvondst moet geïnitialiseerd worden op 01-03-2026.

            Regel melding bij onbekende keurdag
                geldig altijd
                    De meldingsdatum van een Barnsteenvondst moet geïnitialiseerd worden op 15-03-2026.
            """);
        Diagnostic d = onlyError(m);
        assertEquals("attribuut 'meldingsdatum' van Vondst: '(mag opgegeven worden)', en 'meldingsdatum' heeft twee "
                + "initialisaties in één erflijn ('melding bij onbekende aanspoeldag' voor Strandvondst en 'melding bij "
                + "onbekende keurdag' voor Barnsteenvondst) — de effectieve stapel van Barnsteenvondst zou twee "
                + "initialisatiedefaults dragen, en RUNTIME-SEMANTICS ch. 7.10 kent er één; zonder de markering "
                + "weigert WF-079 dit paar (dialect §7)", d.message());
        assertEquals(3, d.line());
    }

    @Test void an_initialisation_on_a_subtype_with_a_window_is_refused() {
        // measured with the marker lowered: ref, clingo and the Drools exec branch each refuse a windowed rule beside
        // an inherited defaults stack, where the unmarked document plays
        RegelSpraakModel m = Resolver.resolve(STRANDVONDST + AANSPOELDAG + """

            Regel melding bij onbekende aanspoeldag
                geldig vanaf 01-01-2026
                    De meldingsdatum van een Strandvondst moet geïnitialiseerd worden op 01-03-2026.
            """);
        Diagnostic d = onlyError(m);
        assertEquals("attribuut 'meldingsdatum' van Vondst: '(mag opgegeven worden)', en de initialisatie 'melding bij "
                + "onbekende aanspoeldag' van 'meldingsdatum' voor Strandvondst heeft een ander geldigheidsvenster dan "
                + "altijd — naast de gegenereerde defaults-groep op Vondst wordt zij een default in de effectieve "
                + "stapel van Strandvondst (RUNTIME-SEMANTICS ch. 7.10), en die neemt geen regel met een venster op "
                + "(dialect §7)", d.message());
        assertEquals(3, d.line());
    }

    @Test void a_writer_on_a_supertype_gets_only_the_target_refusal() {
        // a supertype does not have the marked attribute (WF-105 keeps a name to one type of the line), so its rule's
        // target is refused where the rule is read, and the marker adds no sentence of its own
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Vondst (mv: Vondsten)
                de vinddatum Datum in dagen;
            Objecttype de Strandvondst is een Vondst (mv: Strandvondsten)
                de aanspoeldatum Datum in dagen;
                de meldingsdatum Datum in dagen (mag opgegeven worden);
            """ + VINDDAG + AANSPOELDAG);
        Diagnostic d = onlyError(m);
        assertEquals("regeldoel 'De meldingsdatum van een Vondst': 'meldingsdatum' is geen gedeclareerd attribuut van "
                + "Vondst (WF-022, IR-SPEC ch. 3.1)", d.message());
    }

    @Test void a_generated_member_name_that_is_already_declared_is_refused() {
        RegelSpraakModel m = Resolver.resolve(VONDST + "    de opgegeven waarde Numeriek (getal met 2 decimalen);\n"
                + WAARDERING);
        Diagnostic d = onlyError(m);
        assertEquals("attribuut 'waarde' van Vondst (bronregel 3): '(mag opgegeven worden)' genereert het attribuut "
                + "'opgegeven waarde', en Vondst declareert die naam al als attribuut (bronregel 4) — een naam die de "
                + "markering genereert, bestaat nog niet in de erflijn van Vondst (dialect §7)", d.message());
        assertEquals(3, d.line());
    }

    @Test void a_generated_form_name_that_already_carries_a_rule_is_refused() {
        RegelSpraakModel m = Resolver.resolve(VONDST.replace("(mv: Vondsten)\n",
                "(mv: Vondsten)\n    de heffing Numeriek (getal met 2 decimalen);\n") + WAARDERING + """

            Regel Afwijking opgave waarde
                geldig altijd
                    De heffing van een Vondst moet gesteld worden op 1,00.
            """);
        Diagnostic d = onlyError(m);
        assertEquals("attribuut 'waarde' van Vondst (bronregel 4): '(mag opgegeven worden)' genereert de regel "
                + "'afwijking opgave waarde', en regel 'Afwijking opgave waarde' heet al zo (bronregel 10) — een "
                + "vormnaam is uniek in het hele model, hoofdletters niet meegeteld (WF-020) (dialect §7)", d.message());
        assertEquals(4, d.line());
    }

    @Test void two_marking_on_a_name_in_two_object_types_collide() {
        RegelSpraakModel m = Resolver.resolve(VONDST + """

            Objecttype het Wrak (mv: Wrakken)
                de taxatie Numeriek (getal met 2 decimalen);
                de waarde Numeriek (getal met 2 decimalen) (mag opgegeven worden);
            """ + WAARDERING + """

            Regel wrakwaardering
                geldig altijd
                    De waarde van een Wrak moet berekend worden als de taxatie van het Wrak verminderd met 9,00.
            """);
        Diagnostic d = onlyError(m);
        assertEquals("attribuut 'waarde' van Wrak (bronregel 7): '(mag opgegeven worden)' genereert de defaults-groep "
                + "'opgave boven berekening waarde', en de markering op attribuut 'waarde' van Vondst genereert die "
                + "naam al (bronregel 3) — een vormnaam is uniek in het hele model, hoofdletters niet meegeteld "
                + "(WF-020) (dialect §7)", d.message());
        assertEquals(7, d.line());
    }

    @Test void a_rule_that_writes_a_generated_member_is_refused() {
        RegelSpraakModel m = Resolver.resolve(VONDST + WAARDERING + """

            Regel directe berekening
                geldig altijd
                    De berekende waarde van een Vondst moet gesteld worden op 7,00.
            """);
        Diagnostic d = onlyError(m);
        assertEquals("attribuut 'waarde' van Vondst (bronregel 3): regel 'directe berekening' schrijft 'berekende "
                + "waarde' (bronregel 11), een lid dat '(mag opgegeven worden)' genereert — 'opgegeven waarde' levert "
                + "de casus, 'berekende waarde' krijgt de schrijvers van 'waarde', en 'afwijkende opgave waarde' kent "
                + "de regel 'afwijking opgave waarde' toe; een andere schrijver verdraagt zo'n lid niet (dialect §7)",
                d.message());
        assertEquals(3, d.line());
    }

    @Test void two_initialisations_of_the_target_are_refused() {
        RegelSpraakModel m = Resolver.resolve(VONDST + WAARDERING + STANDAARDWAARDE + """

            Regel tweede standaardwaarde
                geldig altijd
                    De waarde van een Vondst moet geïnitialiseerd worden op 30,00.
            """);
        Diagnostic d = onlyError(m);
        assertEquals("attribuut 'waarde' van Vondst: '(mag opgegeven worden)', en 'waarde' heeft meer dan één "
                + "initialisatie ('standaardwaarde' en 'tweede standaardwaarde') — de gegenereerde defaults-groep draagt "
                + "één initialisatiedefault, de laagste (WF-050) (dialect §7)", d.message());
        assertEquals(3, d.line());
    }

    @Test void an_initialisation_with_a_window_is_refused() {
        RegelSpraakModel m = Resolver.resolve(VONDST + WAARDERING
                + STANDAARDWAARDE.replace("geldig altijd", "geldig vanaf 2024"));
        Diagnostic d = onlyError(m);
        assertEquals("attribuut 'waarde' van Vondst: '(mag opgegeven worden)', en de initialisatie 'standaardwaarde' "
                + "van 'waarde' heeft een ander geldigheidsvenster dan altijd — de gegenereerde defaults-groep geldt "
                + "altijd, en een default draagt geen eigen venster (dialect §7)", d.message());
        assertEquals(3, d.line());
    }

    @Test void an_initialisation_with_a_condition_is_refused() {
        // the group has no place for the condition: lowered without it, the initialisation decided where its
        // condition fails (measured before this refusal: 25,00 in ref, clingo and the Drools exec branch where the
        // unmarked document leaves the target empty)
        RegelSpraakModel m = Resolver.resolve(VONDST + WAARDERING + STANDAARDWAARDE.replace("op 25,00.",
                "op 25,00\n        indien zijn taxatie groter is dan 10,00."));
        Diagnostic d = onlyError(m);
        assertEquals("attribuut 'waarde' van Vondst: '(mag opgegeven worden)', en de initialisatie 'standaardwaarde' "
                + "van 'waarde' heeft een voorwaarde — de initialisatiedefault van de gegenereerde defaults-groep is "
                + "alleen gerechtvaardigd waar 'waarde' in de invoer leeg is, en draagt geen eigen voorwaarde (WF-050) "
                + "(dialect §7)", d.message());
        assertEquals(3, d.line());
        assertTrue(m.prioritizedDefaults.isEmpty(), "de markering genereert niets");
    }

    // -------------------------------------------------------------------- the warning: nothing to put the opgave above

    @Test void without_computing_writer_the_marking_warns_and_generates_nothing() {
        RegelSpraakModel m = Resolver.resolve(VONDST + STANDAARDWAARDE);
        Diagnostic d = onlyDiagnostic(m);
        assertEquals(Severity.WARNING, d.severity());
        assertEquals("attribuut 'waarde' van Vondst: '(mag opgegeven worden)' staat op een attribuut dat geen regel "
                + "berekent of vaststelt — 'waarde' is al invoer, en de markering genereert niets (dialect §7)",
                d.message());
        assertEquals(3, d.line());
        ObjectType v = m.objectTypes.get("Vondst");
        assertEquals(List.of("taxatie", "waarde"), List.copyOf(v.attributes.keySet()));
        assertTrue(v.characteristics.isEmpty());
        assertTrue(m.prioritizedDefaults.isEmpty());
        assertTrue(m.consistencies.isEmpty());
        assertEquals("waarde", targetOf(rule(m, "standaardwaarde")), "de initialisatie blijft een gewone regel");
        Report.assertAccountedFor(IrEmitter.emit(m));
    }

    @Test void without_computing_writer_a_reader_of_a_generated_name_is_refused() {
        RegelSpraakModel m = Resolver.resolve(VONDST.replace("(mv: Vondsten)\n",
                "(mv: Vondsten)\n    de korting Numeriek (getal met 2 decimalen);\n") + """

            Regel korting
                geldig altijd
                    De korting van een Vondst moet berekend worden als de taxatie van de Vondst min zijn berekende waarde.
            """);
        Diagnostic d = onlyError(m);
        assertEquals("attribuut 'waarde' van Vondst: '(mag opgegeven worden)' genereert niets, omdat geen regel "
                + "'waarde' berekent of vaststelt, maar het model leest 'berekende waarde' — die naam bestaat alleen "
                + "waar de markering een berekening van 'waarde' verlegt (dialect §7)", d.message());
        assertEquals(4, d.line());
    }
}
