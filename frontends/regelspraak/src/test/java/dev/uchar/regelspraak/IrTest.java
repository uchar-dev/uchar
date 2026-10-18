package dev.uchar.regelspraak;

import dev.uchar.regelspraak.ir.Conditie;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.ResolvedRegel;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The resolver builds a typed expression IR (the transpiler's input), not just a
 * pass/fail type check: a berekening becomes a {@link ResolvedRegel} whose value is
 * a structured {@link Expr} tree with references bound to the model and datatypes
 * carried on every node.
 */
class IrTest {

    static final String SRC = """
        Eenheidsysteem Geld
            de euro      €
        Objecttype de Verkoop
            het bedrag Numeriek (getal met 2 decimalen) met eenheid €;
            het totaal Numeriek (getal met 2 decimalen) met eenheid €;
        Parameter de toeslag : Numeriek (getal met 2 decimalen) met eenheid €
        Regel bereken totaal
            geldig altijd
                Het totaal van een Verkoop moet berekend worden als het bedrag van de Verkoop plus de toeslag.
        """;

    @Test void calculation_becomes_a_typed_resolved_rule() {
        RegelSpraakModel m = Resolver.resolve(SRC);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        assertEquals(1, m.resolvedRegels.size());

        ResolvedRegel rr = m.resolvedRegels.get(0);
        assertEquals("bereken totaal", rr.naam());
        assertEquals(ResolvedRegel.Soort.BEREKENING, rr.soort());
    }

    @Test void target_is_a_bound_attribute_reference() {
        RegelSpraakModel m = Resolver.resolve(SRC);
        ResolvedRegel rr = m.resolvedRegels.get(0);

        Expr.AttribuutRef doel = assertInstanceOf(Expr.AttribuutRef.class, rr.doel());
        assertEquals("totaal", doel.attribuut());
        assertNotNull(doel.objecttype(), "doel-onderwerp niet opgelost");
        assertSame(m.objecttypen.get("Verkoop"), doel.objecttype());
        assertInstanceOf(Datatype.Numeriek.class, doel.type());
        assertEquals("€", ((Datatype.Numeriek) doel.type()).eenheid().afkorting());
    }

    @Test void value_is_a_typed_summing_tree_with_bound_operands() {
        RegelSpraakModel m = Resolver.resolve(SRC);
        ResolvedRegel rr = m.resolvedRegels.get(0);

        Expr.Binair sum = assertInstanceOf(Expr.Binair.class, rr.waarde());
        assertEquals(Expr.BinOp.PLUS, sum.op());
        assertTrue(sum.type().isNumber(), "som moet numeriek zijn");

        // left operand: het bedrag van de Verkoop -> attribuut bound to objecttype Verkoop
        Expr.AttribuutRef links = assertInstanceOf(Expr.AttribuutRef.class, sum.links());
        assertEquals("bedrag", links.attribuut());
        assertSame(m.objecttypen.get("Verkoop"), links.objecttype());

        // right operand: de toeslag -> bound to the parameter
        Expr.ParameterRef rechts = assertInstanceOf(Expr.ParameterRef.class, sum.rechts());
        assertSame(m.parameters.get("toeslag"), rechts.parameter());
    }

    @Test void aggregation_structure_is_recorded() {
        // The som-aggregation's structure is captured even where its element type is
        // not yet resolved (the argument's subject is a role, not an object type).
        String src = """
            Objecttype de Reis
                de prijs Numeriek (getal met 2 decimalen);
            Regel tel
                geldig altijd
                    De prijs van een Reis moet berekend worden als de som van de prijs van een Reis.
            """;
        RegelSpraakModel m = Resolver.resolve(src);
        // a sum over the subject's own attribute has no spelling on the wire, so the resolver refuses the model;
        // the structure is captured all the same
        assertEquals(List.of("vouw 'de som van de prijs van een Reis' over een eigen attribuut van het onderwerp, dat "
                        + "geen instanties telt: a single-argument fold with an elementType carries its range (sourceRole "
                        + "or sourcePath) under every kind but total: the range is written, never derived"),
                m.errors().stream().map(RegelSpraakModel.Diagnostic::message).toList());
        Expr waarde = m.resolvedRegels.get(0).waarde();
        Expr.FunctionCall sum = assertInstanceOf(Expr.FunctionCall.class, waarde);
        assertEquals(Expr.FunctionKind.SOM, sum.kind());
        assertEquals(1, sum.arguments().size());
    }

    @Test void aggregation_dimension_selection_on_scalar_attribute_stays_a_marker() {
        // A dimensieselectie ("over alle <dim>") whose operand is NOT a gedimensioneerd attribuut (here
        // the scalar "prijs") cannot be lowered — there is no label set to range over — so the resolver
        // keeps the honest marker: an Onopgelost filter + a WARNING, never a silent aggregation of the
        // whole set. (The lowerable case — a genuinely dimensioned operand — is the next test.)
        String src = """
            Dimensie de jaardimensie, bestaande uit de jaardimensies (na het attribuut met voorzetsel van):
                1. vorig jaar
                2. huidig jaar
            Objecttype de Reis
                de prijs Numeriek (geheel getal);
            Regel tel
                geldig altijd
                    De prijs van een Reis moet berekend worden als de som van de prijs van een Reis over alle jaardimensies.
            """;
        RegelSpraakModel m = Resolver.resolve(src);
        Expr.FunctionCall sum = assertInstanceOf(Expr.FunctionCall.class, m.resolvedRegels.get(0).waarde());
        assertNull(sum.dimensieSelectie(), "een scalair operand heeft geen oplosbare dimensieselectie");
        Conditie.Unresolved f = assertInstanceOf(Conditie.Unresolved.class, sum.filter(),
                () -> "de dimensieselectie moet als Onopgelost-marker vastliggen, niet stil verdwijnen");
        assertTrue(f.bron().snippet().contains("over alle jaardimensies"), () -> "bron: " + f.bron());
        assertTrue(m.diagnostics.stream().anyMatch(d -> d.severity() == RegelSpraakModel.Severity.WARNING
                        && d.message().contains("dimensieselectie")),
                () -> "verwachtte een dimensieselectie-WARNING: " + m.diagnostics);
    }

    @Test void dimension_selection_over_dimensioned_attribute_is_resolved() {
        // §5.8.5 / §13.4.17.53: "de som van <gedimensioneerd attribuut> vanaf <a> t/m <b>" resolves to a
        // DimensionSelection carrying the dimension and the CONTIGUOUS label interval [a..b] in the declared
        // order (§13.3.7.5) — positions 3..6 of the seven labels — NOT a marker. No WARNING.
        String src = """
            Objecttype de Natuurlijk persoon (mv: Natuurlijke personen)
                de betaalde belasting over de afgelopen vier jaar Numeriek (geheel getal);
                de betaalde belasting in jaar Numeriek (geheel getal) gedimensioneerd met jaardimensie;
            Dimensie de jaardimensie, bestaande uit de jaardimensies (na het attribuut met voorzetsel van):
                1. zes jaar geleden
                2. vijf jaar geleden
                3. vier jaar geleden
                4. drie jaar geleden
                5. twee jaar geleden
                6. een jaar geleden
                7. heden
            Regel Totaal betaalde belasting over 4 jaar
              geldig altijd
                De betaalde belasting over de afgelopen vier jaar van een Natuurlijk persoon moet berekend worden als de som van zijn betaalde belasting in jaar vanaf vier jaar geleden t/m een jaar geleden.
            """;
        RegelSpraakModel m = Resolver.resolve(src);
        assertTrue(m.errors().isEmpty(), () -> "fouten: " + m.errors());
        Expr.FunctionCall sum = assertInstanceOf(Expr.FunctionCall.class, m.resolvedRegels.get(0).waarde());
        assertEquals(Expr.FunctionKind.SOM, sum.kind());
        Expr.DimensionSelection sel = sum.dimensieSelectie();
        assertNotNull(sel, "de vanaf..t/m dimensieselectie moet opgelost zijn, geen marker");
        assertEquals("jaardimensie", sel.dimension());
        assertEquals(List.of("vier jaar geleden", "drie jaar geleden", "twee jaar geleden", "een jaar geleden"),
                sel.labels(), "interval [vier..een jaar geleden] inclusief, in gedeclareerde volgorde");
        assertFalse(sum.filter() instanceof Conditie.Unresolved, "geen Onopgelost-marker meer");
        assertTrue(m.diagnostics.stream().noneMatch(d -> d.message().contains("dimensieselectie")),
                () -> "geen marker-WARNING verwacht: " + m.diagnostics);
    }

    @Test void dimension_selection_over_multidimensional_attribute_stays_a_marker() {
        // review (lens A): a MULTI-dimension attribuut (§3.6 "inkomen" over jaardimensie EN
        // brutonettodimensie) is keyed by label COMBINATIONS, so the single-Map<label,value> fold would
        // mis-read it (map.get(singleLabel) -> null). Until a composite-key model exists it must stay a
        // marker, not silently fold over one axis. (Single-dimension is the only lowered case.)
        String src = """
            Objecttype de Persoon (mv: Personen)
                de totaal Numeriek (geheel getal);
                de inkomen Numeriek (geheel getal) gedimensioneerd met jaardimensie en brutonettodimensie;
            Dimensie de jaardimensie, bestaande uit de jaardimensies (na het attribuut met voorzetsel van):
                1. vorig jaar
                2. huidig jaar
            Dimensie de brutonettodimensie, bestaande uit de brutonettodimensies (voor het attribuut zonder voorzetsel):
                1. bruto
                2. netto
            Regel tel
              geldig altijd
                De totaal van een Persoon moet berekend worden als de som van zijn inkomen over alle jaardimensies.
            """;
        RegelSpraakModel m = Resolver.resolve(src);
        Expr.FunctionCall sum = assertInstanceOf(Expr.FunctionCall.class, m.resolvedRegels.get(0).waarde());
        assertNull(sum.dimensieSelectie(), "een multidimensioneel attribuut mag (nog) niet over één as gevouwen worden");
        assertInstanceOf(Conditie.Unresolved.class, sum.filter(), "moet de honest marker houden");
        assertTrue(m.diagnostics.stream().anyMatch(d -> d.message().contains("dimensieselectie")),
                () -> "verwachtte een marker-WARNING: " + m.diagnostics);
    }

    @Test void dimension_cell_read_becomes_a_dimension_cell_with_label() {
        // §3.6/§3.8 "zijn betaalde belasting in jaar van huidig jaar" resolves to a DimensionCellRead: the bare
        // attribuut ("betaalde belasting in jaar") plus the peeled jaardimensie label ("huidig jaar").
        String src = """
            Objecttype de Natuurlijk persoon (mv: Natuurlijke personen)
                de uitkomst Numeriek (geheel getal);
                de betaalde belasting in jaar Numeriek (geheel getal) gedimensioneerd met jaardimensie;
            Dimensie de jaardimensie, bestaande uit de jaardimensies (na het attribuut met voorzetsel van):
                1. vorig jaar
                2. huidig jaar
            Regel lees
              geldig altijd
                De uitkomst van een Natuurlijk persoon moet gesteld worden op zijn betaalde belasting in jaar van huidig jaar.
            """;
        RegelSpraakModel m = Resolver.resolve(src);
        assertTrue(m.errors().isEmpty(), () -> "" + m.errors());
        Expr.DimensionCellRead cell = assertInstanceOf(Expr.DimensionCellRead.class, m.resolvedRegels.get(0).waarde(),
                "een gelabelde dimensielezing moet een DimensieCel zijn");
        assertEquals("betaalde belasting in jaar", cell.attribuut().attribuut(), "de kale attribuutnaam");
        assertEquals(List.of("huidig jaar"), cell.labels(), "het geselecteerde label");
        assertTrue(cell.type().isNumber(), "het celtype is het element-datatype van het attribuut");
    }

    @Test void single_condition_becomes_a_comparison() {
        String src = """
            Parameter de grens : Numeriek (geheel getal)
            Objecttype de Reis
                de prijs Numeriek (geheel getal);
                de toeslag Numeriek (geheel getal);
            Regel duur
                geldig altijd
                    De toeslag van een Reis moet gesteld worden op 100
                    indien de prijs van een Reis groter is dan de grens.
            """;
        RegelSpraakModel m = Resolver.resolve(src);
        assertTrue(m.errors().isEmpty(), () -> "fouten: " + m.errors());
        Conditie c = m.resolvedRegels.get(0).voorwaarde();
        Conditie.Vergelijking v = assertInstanceOf(Conditie.Vergelijking.class, c);
        assertEquals(Conditie.Operator.GROTER, v.op());
        assertInstanceOf(Expr.AttribuutRef.class, v.links());
        assertInstanceOf(Expr.ParameterRef.class, v.rechts());
    }

    @Test void compound_condition_becomes_a_tree_of_parts() {
        String src = """
            Objecttype de Reis
                de prijs Numeriek (geheel getal);
                de toeslag Numeriek (geheel getal);
                is duurzaam kenmerk (bijvoeglijk);
            Regel meervoud
                geldig altijd
                    De toeslag van een Reis moet gesteld worden op 1
                    indien de Reis aan alle volgende voorwaarden voldoet:
                        - de prijs van een Reis groter is dan 0
                        - de prijs van een Reis kleiner is dan 100.
            """;
        RegelSpraakModel m = Resolver.resolve(src);
        assertTrue(m.errors().isEmpty(), () -> "fouten: " + m.errors());
        Conditie c = m.resolvedRegels.get(0).voorwaarde();
        Conditie.Samengesteld s = assertInstanceOf(Conditie.Samengesteld.class, c);
        assertEquals(Conditie.Kwantor.ALLE, s.kwantor());
        assertEquals(2, s.onderdelen().size());
        assertInstanceOf(Conditie.Vergelijking.class, s.onderdelen().get(0));
        assertEquals(Conditie.Operator.KLEINER,
                ((Conditie.Vergelijking) s.onderdelen().get(1)).op());
    }

    @Test void unconditional_rule_has_no_condition() {
        RegelSpraakModel m = Resolver.resolve(SRC);
        assertNull(m.resolvedRegels.get(0).voorwaarde());
    }

    @Test void aggregation_takes_over_the_element_type_via_role_navigation() {
        // "de som van <attr> van alle deelnemers van de groep": the element is the
        // deelnemer's attribute (head noun before 'van'), so the sum is its type —
        // not the deeper "groep". Count is a non-negative integer.
        String src = """
            Objecttype de Ploeg
                het totaalbedrag Numeriek (geheel getal);
                het aantal leden Numeriek (geheel getal);
            Objecttype de Persoon
                het bedrag Numeriek (geheel getal);
            Feittype Ploeg van personen
                de groep Ploeg
                de deelnemer Persoon
            Eén groep betreft meerdere deelnemers
            Regel som
                geldig altijd
                    Het totaalbedrag van een groep moet berekend worden als de som van het bedrag van alle deelnemers van de groep.
            Regel telling
                geldig altijd
                    Het aantal leden van een groep moet berekend worden als het aantal deelnemers van de groep.
            """;
        RegelSpraakModel m = Resolver.resolve(src);
        assertTrue(m.errors().isEmpty(), () -> "fouten: " + m.errors());

        Expr.FunctionCall sum = assertInstanceOf(Expr.FunctionCall.class, m.resolvedRegels.get(0).waarde());
        assertEquals(Expr.FunctionKind.SOM, sum.kind());
        assertTrue(sum.type().isNumber(), "som-elementtype niet via rol opgelost");

        Expr.FunctionCall count = assertInstanceOf(Expr.FunctionCall.class, m.resolvedRegels.get(1).waarde());
        assertEquals(Expr.FunctionKind.AANTAL, count.kind());
        assertTrue(count.type().isNumber(), "telling moet numeriek (een telling) zijn");
    }

    @Test void duration_is_typed_with_a_normalised_unit() {
        // "in hele jaren" must be convertible with the declared abbreviation "jr",
        // so a duration can assign to an attribute "met eenheid jr" without a false
        // positive — unit-word/plural/abbreviation all denote the one unit.
        String src = """
            Eenheidsysteem Tijd
                het jaar    jr
            Objecttype de Persoon
                de leeftijd Numeriek (geheel getal) met eenheid jr;
                de geboortedatum Datum in dagen;
                de peildatum Datum in dagen;
            Regel leeftijd
                geldig altijd
                    De leeftijd van een Persoon moet berekend worden als de tijdsduur van de geboortedatum van een Persoon tot de peildatum van een Persoon in hele jaren.
            """;
        RegelSpraakModel m = Resolver.resolve(src);
        assertTrue(m.errors().isEmpty(), () -> "valse fout op jaren/jr: " + m.errors());

        Expr.FunctionCall td = assertInstanceOf(Expr.FunctionCall.class, m.resolvedRegels.get(0).waarde());
        assertEquals(Expr.FunctionKind.TIJDSDUUR, td.kind());
        Datatype.Numeriek n = assertInstanceOf(Datatype.Numeriek.class, td.type());
        assertTrue(n.geheel(), "hele jaren -> geheel");
        assertTrue(n.eenheid().convertibleTo(m.resolveEenheid("jr")), "jaren niet inwisselbaar met jr");
    }

    @Test void declared_plural_wins_over_guessed_morphology() {
        // The role declares an IRREGULAR plural "(mv: steden)"; a "stad"+s guess would
        // produce "stads" and never match "steden". Matching must use the DECLARED form.
        String src = """
            Objecttype de Stad
                de inwoners Numeriek (geheel getal);
            Objecttype de Land
                het totaal Numeriek (geheel getal);
            Feittype Land met steden
                het land Land
                de stad (mv: steden) Stad
            Eén land heeft meerdere steden
            Regel som
                geldig altijd
                    Het totaal van een land moet berekend worden als de som van de inwoners van alle steden van een land.
            """;
        RegelSpraakModel m = Resolver.resolve(src);
        assertTrue(m.errors().isEmpty(), () -> "fouten: " + m.errors());

        Expr.FunctionCall sum = assertInstanceOf(Expr.FunctionCall.class, m.resolvedRegels.get(0).waarde());
        assertEquals(Expr.FunctionKind.SOM, sum.kind());
        Expr.AttribuutRef arg = assertInstanceOf(Expr.AttribuutRef.class, sum.arguments().get(0));
        assertNotNull(arg.objecttype(), "'steden' niet opgelost via gedeclareerd meervoud");
        assertEquals("Stad", arg.objecttype().naam);   // resolved via (mv: steden), not a guess
    }

    @Test void references_resolve_via_fact_type_roles() {
        // Subjects are roles, not object-type names: "een groep" → Ploeg (role groep),
        // "een deelnemer"/"alle deelnemers" → Persoon (role deelnemer, incl. plural).
        String src = """
            Objecttype de Ploeg
                de prijs Numeriek (geheel getal);
            Objecttype de Persoon
                de leeftijd Numeriek (geheel getal);
            Feittype Ploeg van personen
                de groep Ploeg
                de deelnemer Persoon
            Eén groep betreft meerdere deelnemers
            Regel r
                geldig altijd
                    De prijs van een groep moet gesteld worden op de leeftijd van alle deelnemers.
            """;
        RegelSpraakModel m = Resolver.resolve(src);
        // Since the WF-096 promotion (2026-09-11) this very sentence draws the setlezing ERROR:
        // the rule value reads "de leeftijd van alle deelnemers" outside a fold. The role
        // navigation this test pins must still resolve; any OTHER error stays a failure.
        assertEquals(1, m.errors().size(), () -> "verwacht alleen de setlezing-fout: " + m.errors());
        assertTrue(m.errors().get(0).message().contains("setlezing buiten een vouwbron"),
                () -> "valse fout via rolnavigatie: " + m.errors());

        ResolvedRegel rr = m.resolvedRegels.get(0);
        Expr.AttribuutRef doel = assertInstanceOf(Expr.AttribuutRef.class, rr.doel());
        assertNotNull(doel.objecttype(), "rol 'groep' niet opgelost");
        assertEquals("Ploeg", doel.objecttype().naam);          // via role groep

        Expr.AttribuutRef waarde = assertInstanceOf(Expr.AttribuutRef.class, rr.waarde());
        assertNotNull(waarde.objecttype(), "meervoudige rol 'deelnemers' niet opgelost");
        assertEquals("Persoon", waarde.objecttype().naam);       // via plural role deelnemers
        assertTrue(waarde.type().isNumber());
    }

    @Test void unresolved_reference_stays_unknown_no_false_error() {
        // A deep van-chain the resolver cannot bind must not produce a type error.
        String src = """
            Objecttype de Reis
                de prijs Numeriek (getal met 2 decimalen);
            Regel x
                geldig altijd
                    De prijs van een Reis moet gesteld worden op de korting van de aanbieding van de reis.
            """;
        RegelSpraakModel m = Resolver.resolve(src);
        assertTrue(m.errors().isEmpty(), () -> "valse fout op onopgeloste referentie: " + m.errors());
        // value is an attribuut-ref whose datatype stayed Onbekend (subject not resolved)
        Expr waarde = m.resolvedRegels.get(0).waarde();
        assertTrue(waarde.type().isUnknown());
    }
}
