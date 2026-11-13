package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Declaration;
import dev.uchar.regelspraak.model.RegelSpraakModel.Designatable;
import dev.uchar.regelspraak.resolver.Resolver;
import dev.uchar.regelspraak.resolver.ScenarioResolver;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Where the source declares each element of the model, and the comment written above it
 * ({@link RegelSpraakModel#declarations}): the line of the declaration's first token, and the line comments directly
 * above it, so that a playground or an OpenAPI document reads the meaning of a declaration through the model instead
 * of walking the tokens itself. The comment is the run of comment lines right above the declaration: an empty line
 * ends it, and so does a comment behind code on its own line.
 */
class DeclarationCommentTest {

    private static final String SOURCE = """
        Vocabulaire handboek (taal: nl)

        // de soort van een vondst
        Domein Vondstsoort is van het type Enumeratie
            // drijfhout
            'wrakhout'
            'barnsteen'

        // Bron: art. 1
        // het tarief per kilo
        Parameter het premietarief : Numeriek (geheel getal)

        // los van de declaratie

        // wie aan het strand zoekt
        Objecttype de Jutter (mv: Jutters) (bezield)
            // het totaal van de vondsten
            het vondstbedrag Numeriek (getal met 2 decimalen);
            de naam Tekst; // achter de naam
            is premiewaardig kenmerk (bijvoeglijk);

        // een aanmelding
        Feittype vondstaanmelding
            // wie aanmeldt
            de aanmelder Jutter
            de gemelde (mv: gemelden) Jutter
        Eén aanmelder heeft meerdere gemelden

        // de groep
        Regelgroep premies (handboek: de premies)
        // de regel
        Regel premiewaardigheid
            geldig altijd
                Een Jutter is premiewaardig indien zijn vondstbedrag groter is dan 10.
        """;

    private static RegelSpraakModel clean(RegelSpraakModel m) {
        assertEquals(List.of(), m.errors().stream().map(RegelSpraakModel.Diagnostic::toString).toList());
        return m;
    }

    /** The table as lines of kind, owner and name, line, and the comment lines joined by '|', in its order. */
    private static List<String> table(RegelSpraakModel m) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<Designatable, Declaration> e : m.declarations.entrySet()) {
            Designatable b = e.getKey();
            out.add(b.kind() + " " + (b.owner() == null ? "" : b.owner() + ".") + b.name() + " " + e.getValue().line()
                    + " " + String.join("|", e.getValue().comment()));
        }
        return out;
    }

    @Test
    void everyDeclarationCarriesItsLineAndTheCommentAboveIt() {
        RegelSpraakModel m = clean(Resolver.resolve(SOURCE));
        assertEquals(List.of(
                "OBJECTTYPE Jutter 16 wie aan het strand zoekt",
                "ATTRIBUUT Jutter.vondstbedrag 18 het totaal van de vondsten",
                "ATTRIBUUT Jutter.naam 19 ",
                "KENMERK Jutter.premiewaardig 20 ",
                "FEITTYPE vondstaanmelding 23 een aanmelding",
                "ROL vondstaanmelding.aanmelder 25 wie aanmeldt",
                "ROL vondstaanmelding.gemelde 26 ",
                "PARAMETER premietarief 11 Bron: art. 1|het tarief per kilo",
                "ENUMERATIEWAARDE Vondstsoort.wrakhout 6 drijfhout",
                "ENUMERATIEWAARDE Vondstsoort.barnsteen 7 ",
                "VORM premiewaardigheid 32 de regel",
                "REGELGROEP premies 30 de groep"), table(m));
        assertEquals(m.declarations.get(Designatable.attribute("Jutter", "vondstbedrag")),
                m.declaration(Designatable.attribute("JUTTER", "Vondstbedrag")).orElseThrow());
        assertTrue(m.declaration(Designatable.objectType("Haven")).isEmpty());
    }

    /** The scenario route reads the declarations of a test document from the same token stream. */
    @Test
    void aTestDocumentCarriesTheDeclarationsOfItsBlocks() {
        RegelSpraakModel m = clean(ScenarioResolver.resolve("""
            // een sein aan de mast
            Objecttype het Sein (mv: Seinen)
                // in meters
                de hoogte Numeriek (geheel getal);
                is half stok kenmerk (bijvoeglijk);

            Regel halfstok van een sein
                geldig altijd
                    Een Sein is half stok
                    indien de hoogte van het Sein kleiner is dan 5.

            Scenario laag
            Gegeven is de volgende situatie:
            een Sein S1
            - de hoogte is 3
            Met als verwacht resultaat:
            - S1 is half stok
            """).model());
        assertEquals(new Declaration(2, List.of("een sein aan de mast")), m.declaration(Designatable.objectType("Sein")).orElseThrow());
        assertEquals(new Declaration(4, List.of("in meters")), m.declaration(Designatable.attribute("Sein", "hoogte")).orElseThrow());
        assertEquals(new Declaration(7, List.of()), m.declaration(Designatable.form("halfstok van een sein")).orElseThrow());
    }

    /** Rule groups are WF-001's ninth namespace: a second group of one name is refused (RGS-008), and the declarations
     *  of the other elements of the source stay where they are. */
    @Test
    void aSecondRuleGroupOfOneNameIsRefusedAndTheOtherDeclarationsStand() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Jutter (mv: Jutters) (bezield)
                het vondstbedrag Numeriek (getal met 2 decimalen);
                is premiewaardig kenmerk (bijvoeglijk);
                is rijk kenmerk (bijvoeglijk);

            // eerste
            Regelgroep g
            Regel premiewaardigheid
                geldig altijd
                    Een Jutter is premiewaardig indien zijn vondstbedrag groter is dan 10.

            // tweede
            Regelgroep g
            Regel rijkdom
                geldig altijd
                    Een Jutter is rijk indien zijn vondstbedrag groter is dan 100.
            """);
        assertEquals(List.of("RGS-008 13"), m.errors().stream().map(d -> d.code() + " " + d.line()).toList(),
                m.diagnostics::toString);
        assertEquals(new Declaration(8, List.of()), m.declaration(Designatable.form("premiewaardigheid")).orElseThrow());
    }
}
