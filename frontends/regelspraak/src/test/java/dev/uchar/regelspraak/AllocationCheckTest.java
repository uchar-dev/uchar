package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * What the rule checker refuses of an allocation (§13.4.10, IR-SPEC ch. 3.2) where the verifier would refuse the
 * document, or where the allocation would leave the document without a word.
 */
class AllocationCheckTest {

    static final String MODEL = """
        Eenheidsysteem Munt
            de euro (mv: euros) EUR €

        Objecttype de Berging (mv: Bergingen)
            de opbrengst Numeriek (geheel getal) met eenheid €;
            de omschrijving Tekst;

        Objecttype de Berger (mv: Bergers) (bezield)
            het bergingsaandeel (mv: bergingsaandelen) Numeriek (geheel getal) met eenheid €;
            de notitie (mv: notities) Tekst;
            de naam Tekst;

        Parameter het kortingspercentage : Percentage (geheel getal)

        Feittype bergingsdeelname
            de berging Berging
            de berger (mv: bergers) Berger
        Eén berging heeft meerdere bergers
        """;

    private static String allocation(String sentence) {
        return MODEL + """

            Regel verdeel de opbrengst
                geldig altijd
                    %s
            """.formatted(sentence);
    }

    /** A source that reads as the percentage function over an attribute left the allocation out of the document,
     *  with no diagnostic and a document the verifier passes. */
    @Test void a_source_that_reads_as_a_computation_is_refused() {
        RegelSpraakModel m = Resolver.resolve(allocation("Het kortingspercentage van de opbrengst van een berging wordt "
                + "verdeeld over de bergingsaandelen van alle bergers van de berging, waarbij wordt verdeeld in gelijke delen."));
        List<Diagnostic> errors = m.errors();
        assertEquals(1, errors.size(), () -> "verwachtte precies één fout, kreeg: " + m.diagnostics);
        assertEquals("RGS-421", errors.get(0).code(), errors.get(0).message());
        assertEquals(22, errors.get(0).line(), errors.get(0).message());
        assertTrue(errors.get(0).message().contains("'Het kortingspercentage van de opbrengst van een berging'"),
                errors.get(0).message());
    }

    /** "zijn" in the condition of an allocation reads the source instance the allocation runs on: before, the rule
     *  had no subject there, the read stayed a marker with a warning, and ref faulted on it (EF-019). */
    @Test void a_condition_reads_the_source_as_its_subject() {
        RegelSpraakModel m = Resolver.resolve(allocation("De opbrengst van een berging wordt verdeeld over de "
                + "bergingsaandelen van alle bergers van de berging, waarbij wordt verdeeld in gelijke delen\n"
                + "        indien zijn opbrengst groter is dan 10 €."));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        var c = (dev.uchar.regelspraak.ir.Condition.Comparison) m.allocations.get(0).condition();
        var read = (dev.uchar.regelspraak.ir.Expr.AttributeReference) c.left();
        assertEquals("opbrengst", read.attribute());
        assertEquals("Berging", read.objectType().name);
    }

    /** An allocation computes with amounts (WF-025): a Text pot distributed over a Text share, and a Text weight,
     *  were written without a word, the first with no type check at all. One ERROR per member. */
    @Test void a_member_that_is_no_number_is_refused() {
        RegelSpraakModel m = Resolver.resolve(allocation("De omschrijving van een berging wordt verdeeld over de "
                + "notities van alle bergers van de berging, waarbij wordt verdeeld in gelijke delen."));
        assertEquals(List.of("RGS-423 22 omschrijving", "RGS-423 22 notitie"), m.errors().stream()
                .map(d -> d.code() + " " + d.line() + " " + d.params().get("attribute")).toList(), m.diagnostics::toString);
        RegelSpraakModel w = Resolver.resolve(allocation("De opbrengst van een berging wordt verdeeld over de "
                + "bergingsaandelen van alle bergers van de berging, waarbij wordt verdeeld naar rato van de naam."));
        assertEquals(List.of("RGS-423 naam"), w.errors().stream().map(d -> d.code() + " " + d.params().get("attribute"))
                .toList(), w.diagnostics::toString);
        assertTrue(w.errors().get(0).message().contains("het gewicht 'naam' is van het type Tekst"),
                w.errors().get(0).message());
    }

    /** A maximum or a tie weight that is declared but no number is one fault, reported once as RGS-423, not again as
     *  the reason the allocation has no method (RGS-401). */
    @Test void a_maximum_or_tie_weight_that_is_no_number_is_reported_once() {
        RegelSpraakModel capped = Resolver.resolve(MODEL + """

            Regel verdeel de opbrengst
                geldig altijd
                    De opbrengst van een berging wordt verdeeld over de bergingsaandelen van alle bergers van de berging, waarbij wordt verdeeld:
                    - naar rato van het bergingsaandeel,
                    - met een maximum van de notitie.
            """);
        assertEquals(List.of("RGS-423 notitie"), capped.errors().stream()
                .map(d -> d.code() + " " + d.params().get("attribute")).toList(), capped.diagnostics::toString);
        RegelSpraakModel tie = Resolver.resolve(MODEL + """

            Regel verdeel de opbrengst
                geldig altijd
                    De opbrengst van een berging wordt verdeeld over de bergingsaandelen van alle bergers van de berging, waarbij wordt verdeeld:
                    - op volgorde van afnemende bergingsaandeel,
                    - bij een even groot criterium naar rato van de naam.
            """);
        assertEquals(List.of("RGS-423 naam"), tie.errors().stream()
                .map(d -> d.code() + " " + d.params().get("attribute")).toList(), tie.diagnostics::toString);
    }

    /** Every attribute an allocation names is a declared attribute of its holder (WF-034): the amount and the rest on
     *  the distributor, the share, the weight, the maximum and the ordering key on the recipients. An undeclared one
     *  went out silently (amount, share, weight: the verifier refused the document) or left the allocation without a
     *  method behind a WARNING (maximum, key). One ERROR at the allocation, naming the attribute. */
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource(delimiter = '|', value = {
            "De winst van een berging wordt verdeeld over de bergingsaandelen van alle bergers van de berging, waarbij wordt verdeeld in gelijke delen. | winst | Berging",
            "De opbrengst van een berging wordt verdeeld over de bonussen van alle bergers van de berging, waarbij wordt verdeeld in gelijke delen. | bonussen | Berger",
            "De opbrengst van een berging wordt verdeeld over de bergingsaandelen van alle bergers van de berging, waarbij wordt verdeeld naar rato van de inzet. | inzet | Berger",
    })
    void an_undeclared_member_is_refused(String sentence, String attribute, String holder) {
        RegelSpraakModel m = Resolver.resolve(allocation(sentence));
        assertEquals(List.of("RGS-425 22 " + attribute + " " + holder), m.errors().stream()
                .map(d -> d.code() + " " + d.line() + " " + d.params().get("attribute") + " " + d.params().get("objectType"))
                .toList(), m.diagnostics::toString);
        assertTrue(m.errors().get(0).message().contains("'" + attribute + "'")
                && m.errors().get(0).message().contains("WF-034"), m.errors().get(0).message());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource(delimiter = '|', value = {
            "- naar rato van het bergingsaandeel,;- met een maximum van het plafond.;Als onverdeelde rest blijft de opbrengst van de berging over. | plafond | Berger",
            "- op volgorde van afnemende rang,;- bij een even groot criterium in gelijke delen. | rang | Berger",
            "- in gelijke delen,;- afgerond op 0 decimalen naar beneden.;Als onverdeelde rest blijft het restant van de berging over. | restant | Berging",
    })
    void an_undeclared_member_of_a_bulleted_allocation_is_refused(String bullets, String attribute, String holder) {
        RegelSpraakModel m = Resolver.resolve(MODEL + """

            Regel verdeel de opbrengst
                geldig altijd
                    De opbrengst van een berging wordt verdeeld over de bergingsaandelen van alle bergers van de berging, waarbij wordt verdeeld:
            """ + String.join("\n", java.util.Arrays.stream(bullets.split(";")).map(b -> "        " + b).toList()) + "\n");
        assertEquals(List.of("RGS-425 22 " + attribute + " " + holder), m.errors().stream()
                .map(d -> d.code() + " " + d.line() + " " + d.params().get("attribute") + " " + d.params().get("objectType"))
                .toList(), m.diagnostics::toString);
    }

    /** At most one allocation writes a target (WF-026), also through a type chain: an allocation over Berger and one
     *  over its subtype Hoofdberger both write the Hoofdberger's share. Both went out, and the verifier refused the
     *  document. One ERROR at the second allocation, naming the first. */
    @Test void two_allocations_to_one_target_are_refused() {
        String second = """

            Regel verdeel de opbrengst opnieuw
                geldig altijd
                    De opbrengst van een berging wordt verdeeld over de bergingsaandelen van alle bergers van de berging, waarbij wordt verdeeld in gelijke delen.
            """;
        RegelSpraakModel m = Resolver.resolve(allocation("De opbrengst van een berging wordt verdeeld over de "
                + "bergingsaandelen van alle bergers van de berging, waarbij wordt verdeeld in gelijke delen.") + second);
        assertEquals(List.of("RGS-426 26 verdeel de opbrengst opnieuw verdeel de opbrengst bergingsaandeel"),
                m.errors().stream().map(d -> d.code() + " " + d.line() + " " + d.params().get("name") + " "
                        + d.params().get("other") + " " + d.params().get("attribute")).toList(), m.diagnostics::toString);
        assertTrue(m.errors().get(0).message().contains("WF-026"), m.errors().get(0).message());

        RegelSpraakModel chain = Resolver.resolve(MODEL + """
            Objecttype de Hoofdberger is een Berger (mv: Hoofdbergers) (bezield)
                de rang Numeriek (niet-negatief geheel getal);

            Feittype hoofdbergingsdeelname
                de hoofdberging Berging
                de hoofdberger (mv: hoofdbergers) Hoofdberger
            Eén hoofdberging heeft meerdere hoofdbergers

            Regel verdeel de opbrengst
                geldig altijd
                    De opbrengst van een berging wordt verdeeld over de bergingsaandelen van alle bergers van de berging, waarbij wordt verdeeld in gelijke delen.

            Regel verdeel de opbrengst over hoofdbergers
                geldig altijd
                    De opbrengst van een berging wordt verdeeld over de bergingsaandelen van alle hoofdbergers van de berging, waarbij wordt verdeeld in gelijke delen.
            """);
        assertEquals(List.of("RGS-426 verdeel de opbrengst over hoofdbergers verdeel de opbrengst"),
                chain.errors().stream().map(d -> d.code() + " " + d.params().get("name") + " " + d.params().get("other"))
                        .toList(), chain.diagnostics::toString);
    }

    @Test void an_allocation_of_attributes_stays_clean() {
        RegelSpraakModel m = Resolver.resolve(allocation("De opbrengst van een berging wordt verdeeld over de "
                + "bergingsaandelen van alle bergers van de berging, waarbij wordt verdeeld in gelijke delen."));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertEquals(1, m.allocations.size());
    }

    /** A distributor and recipients with a weight in kg, and {@code %s} for the type of the maximum and of the rest. */
    private static final String CAPPED = """
        Eenheidsysteem Munt
            de euro (mv: euros) EUR €

        Eenheidsysteem Gewicht
            het kilogram (mv: kilogrammen) kg

        Objecttype de Berging (mv: Bergingen)
            de opbrengst Numeriek (geheel getal) met eenheid €;
            de onverdeelde rest %s;

        Objecttype de Berger (mv: Bergers) (bezield)
            het bergingsaandeel (mv: bergingsaandelen) Numeriek (geheel getal) met eenheid €;
            het geborgen gewicht Numeriek (getal met 2 decimalen) met eenheid kg;
            het bergingsmaximum %s;

        Feittype bergingsdeelname
            de berging Berging
            de berger (mv: bergers) Berger
        Eén berging heeft meerdere bergers

        Regel verdeel de opbrengst
            geldig altijd
                De opbrengst van een berging wordt verdeeld over de bergingsaandelen van alle bergers van de berging, waarbij wordt verdeeld:
                - naar rato van het geborgen gewicht,
                - met een maximum van het bergingsmaximum,
                - afgerond op 0 decimalen naar beneden.
                Als onverdeelde rest blijft de onverdeelde rest van de berging over.
        """;

    private static List<String> cappedErrors(String rest, String maximum) {
        RegelSpraakModel m = Resolver.resolve(CAPPED.formatted(rest, maximum));
        return m.errors().stream().map(d -> d.code() + " " + d.line() + " " + d.params().get("attribute") + " "
                + d.params().get("unit")).toList();
    }

    /** WF-025: amount, rest and maximum reduce to the unit of the share. A maximum in % or in kg beside a share in €
     *  passed the frontend and the verifier, and every executor faulted on it without a code. */
    @Test void a_maximum_or_rest_whose_unit_is_not_the_share_unit_is_refused() {
        String euro = "Numeriek (geheel getal) met eenheid €";
        assertEquals(List.of(), cappedErrors(euro, euro));
        assertEquals(List.of("RGS-435 23 bergingsmaximum %"), cappedErrors(euro, "Percentage (geheel getal)"));
        assertEquals(List.of("RGS-435 23 bergingsmaximum kg"), cappedErrors(euro, "Numeriek (geheel getal) met eenheid kg"));
        assertEquals(List.of("RGS-435 23 onverdeelde rest geen"), cappedErrors("Numeriek (geheel getal)", euro));
    }
}
