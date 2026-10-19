package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Designatable;
import dev.uchar.regelspraak.model.RegelSpraakModel.GeneratedFrom;
import dev.uchar.regelspraak.model.RegelSpraakModel.OriginForm;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Every element the frontend makes itself — a name no declaration in the source carries — says in the model which
 * declared element it comes from, and by which form ({@link RegelSpraakModel#generated}). A consumer may not tell
 * from the name: an author can write "berekende" or " voor " in a name of his own, and such an element carries no
 * entry. The table is a side table without semantics, like the designations: the emitter writes nothing of it.
 */
class GeneratedFromTest {

    private static RegelSpraakModel clean(String source) {
        RegelSpraakModel m = Resolver.resolve(source);
        assertEquals(List.of(), m.errors().stream().map(RegelSpraakModel.Diagnostic::toString).toList());
        return m;
    }

    private static String element(Designatable b) {
        return b.category() + " " + (b.owner() == null ? "" : b.owner() + ".") + b.name();
    }

    /** The table as lines "<element> <- <source element> (<key>)", in the order of the map. */
    private static List<String> table(RegelSpraakModel m) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<Designatable, GeneratedFrom> e : m.generated.entrySet())
            result.add(element(e.getKey()) + " <- " + element(e.getValue().source()) + " (" + e.getValue().form().key()
                    + ")");
        return result;
    }

    /** Every key is an element of the model, as {@link RegelSpraakModel#designatable()} spells it, and the lookup by
     *  the name identity finds it in another case. */
    private static void keysAreElements(RegelSpraakModel m) {
        List<Designatable> all = m.designatable();
        for (Designatable b : m.generated.keySet()) {
            assertTrue(all.contains(b), () -> element(b) + " staat in de tabel en is geen element van het model");
            Designatable upperCased = new Designatable(b.category(), b.owner() == null ? null : b.owner().toUpperCase(),
                    b.name().toUpperCase());
            assertEquals(m.generated.get(b), m.generatedFrom(upperCased).orElseThrow());
        }
    }

    private static final String VONDST = """
        Objecttype de Vondst (mv: Vondsten)
            de taxatie Numeriek (getal met 2 decimalen);
            de berekende taxatie Numeriek (getal met 2 decimalen);
            de waarde Numeriek (getal met 2 decimalen) (mag opgegeven worden);

        Regel waarde
            geldig altijd
                De waarde van een Vondst moet berekend worden als zijn taxatie maal 2.

        Regel berekende taxatie
            geldig altijd
                De berekende taxatie van een Vondst moet berekend worden als zijn taxatie plus 1.
        """;

    @Test void the_input_marking_gives_six_elements_out_of_the_marked_attribute() {
        RegelSpraakModel m = clean(VONDST);
        assertEquals(List.of(
                "ATTRIBUUT Vondst.opgegeven waarde <- ATTRIBUUT Vondst.waarde (opgave-boven-berekening)",
                "ATTRIBUUT Vondst.berekende waarde <- ATTRIBUUT Vondst.waarde (opgave-boven-berekening)",
                "KENMERK Vondst.afwijkende opgave waarde <- ATTRIBUUT Vondst.waarde (opgave-boven-berekening)",
                "VORM afwijking opgave waarde <- ATTRIBUUT Vondst.waarde (opgave-boven-berekening)",
                "VORM controle opgave waarde <- ATTRIBUUT Vondst.waarde (opgave-boven-berekening)",
                "VORM opgave boven berekening waarde <- ATTRIBUUT Vondst.waarde (opgave-boven-berekening)"),
                table(m));
        // the author's 'berekende taxatie', attribute and rule, is no generated element
        assertTrue(m.generatedFrom(Designatable.attribute("Vondst", "berekende taxatie")).isEmpty());
        assertTrue(m.generatedFrom(Designatable.form("berekende taxatie")).isEmpty());
        assertTrue(m.generatedFrom(Designatable.attribute("Vondst", "waarde")).isEmpty());
        keysAreElements(m);
    }

    @Test void a_withdrawn_marking_leaves_nothing_behind() {
        // no rule computes 'waarde': the marker generates nothing, with a WARNING, and withdraws its three members
        RegelSpraakModel m = clean("""
            Objecttype de Vondst (mv: Vondsten)
                de waarde Numeriek (getal met 2 decimalen) (mag opgegeven worden);
            """);
        assertEquals(1, m.diagnostics.size(), m.diagnostics::toString);
        assertEquals(Map.of(), m.generated);
    }

    @Test void the_group_of_a_subtype_comes_out_of_the_base_rule() {
        RegelSpraakModel m = clean("""
            Eenheidsysteem Tijd
                de dag dg
                de maand mnd

            Objecttype de Jutter (mv: Jutters) (bezield)
                de vinddag Datum in dagen;
                de meldtermijn Datum in dagen;

            Objecttype de Strandjutter is een Jutter (mv: Strandjutters)
                het vergunningseinde Datum in dagen;

            Regel meldtermijn
                geldig altijd
                    De meldtermijn van een Jutter moet berekend worden als zijn vinddag plus 6 mnd
                    indien zijn vinddag gevuld is.

            Afwijking van meldtermijn: meldtermijn tot het vergunningseinde
                geldig altijd
                    De meldtermijn van een Strandjutter moet gesteld worden op zijn vergunningseinde
                    indien zijn vergunningseinde gevuld is.
            """);
        assertEquals(List.of("VORM meldtermijn voor Strandjutter <- VORM meldtermijn (uitzondering-voor-subtype)"),
                table(m));
        // the group of the tree carries the name of its base rule, a name the author wrote
        assertTrue(m.designatable().contains(Designatable.form("meldtermijn")));
        keysAreElements(m);
    }

    @Test void a_written_name_with_for_is_no_generated_group() {
        RegelSpraakModel m = clean("""
            Objecttype de Jutter (mv: Jutters) (bezield)
                is kustwacht kenmerk (bijvoeglijk);
                is havenmeester kenmerk (bijvoeglijk);
                het wachtgeld Numeriek (geheel getal);

            Uitzonderingen ongeordend:
            Regel wachtgeld voor kustwachters
                geldig altijd
                    Het wachtgeld van een Jutter moet gesteld worden op 30
                    indien hij kustwacht is.

            Regel wachtgeld voor havenmeesters
                geldig altijd
                    Het wachtgeld van een Jutter moet gesteld worden op 45
                    indien hij havenmeester is.
            """);
        assertTrue(m.designatable().contains(Designatable.form("wachtgeld voor kustwachters")));
        assertEquals(Map.of(), m.generated);
    }

    @Test void every_row_of_a_decision_table_comes_out_of_the_table() {
        RegelSpraakModel m = clean("""
            Objecttype de Jutter (mv: Jutters) (bezield)
                de bonus Numeriek (geheel getal);
                het aantal geborgen wrakken Numeriek (niet-negatief geheel getal);

            Beslistabel Bonustabel
                geldig altijd
            | | de bonus van een Jutter moet gesteld worden op | indien zijn aantal geborgen wrakken groter is dan |
            |---|---|---|
            | 1 | 10 | 5 |
            | 2 | 20 | 10 |
            """);
        assertEquals(List.of(
                "VORM Bonustabel 1 <- VORM Bonustabel (beslistabelrij)",
                "VORM Bonustabel 2 <- VORM Bonustabel (beslistabelrij)"),
                table(m));
        // the table itself is no element of the model: its rows are
        assertFalse(m.designatable().contains(Designatable.form("Bonustabel")));
        keysAreElements(m);
    }

    @Test void the_member_of_a_fold_with_steps_comes_out_of_the_reached_attribute_as_declared() {
        // 'gewicht' is declared on Meting, a supertype of the Weging the steps reach
        RegelSpraakModel m = clean("""
            Objecttype de Ploeg (mv: Ploegen)
                het zwaarste gewicht Numeriek (geheel getal);

            Objecttype de Jutter (mv: Jutters) (bezield)
                de startdag Datum in dagen;

            Objecttype de Vondst (mv: Vondsten)
                de vinddag Datum in dagen;

            Objecttype de Meting (mv: Metingen)
                het gewicht Numeriek (geheel getal);

            Objecttype de Weging is een Meting (mv: Wegingen)
                de weegdag Datum in dagen;

            Feittype PloegHeeftJutter
                de ploeg Ploeg
                de jutter (mv: jutters) Jutter
            Eén ploeg heeft meerdere jutters

            Feittype JutterHeeftVondst
                de jutter Jutter
                de vondst Vondst
            Eén jutter heeft één vondst

            Feittype VondstHeeftWeging
                de vondst Vondst
                de weging Weging
            Eén vondst heeft één weging

            Regel zwaarste gewicht
                geldig altijd
                    Het zwaarste gewicht van een Ploeg moet berekend worden als de maximale waarde van het gewicht van de weging van de vondst van alle jutters van de Ploeg.
            """);
        assertEquals(List.of(
                "ATTRIBUUT Jutter.gewicht via vondst en weging <- ATTRIBUUT Meting.gewicht (stappen-na-de-verzameling)",
                "VORM gewicht via vondst en weging <- ATTRIBUUT Meting.gewicht (stappen-na-de-verzameling)"),
                table(m));
        keysAreElements(m);
    }

    private static final String START_POINT = """
        Tijdlijn het penningjaar heeft tijdvakken van een jaar met variabel startpunt;

        Objecttype de Jutter (mv: Jutters) (bezield)
            de aanvangsdatum Datum in dagen;
        %s
        Regel penningjaar van een jutter
            geldig altijd
                Het startpunt van het penningjaar voor een Jutter wordt bepaald door zijn aanvangsdatum.
        """;

    @Test void the_anchor_of_a_start_point_determination_comes_out_of_the_rule_that_writes_it() {
        RegelSpraakModel m = clean(START_POINT.formatted(""));
        assertEquals(List.of(
                "ATTRIBUUT Jutter.startpunt penningjaar <- VORM penningjaar van een jutter (startpunt)"), table(m));
        keysAreElements(m);
        // an author who declares the anchor himself wrote its name
        RegelSpraakModel own = clean(START_POINT.formatted("    het startpunt penningjaar Datum in dagen;\n"));
        assertEquals(Map.of(), own.generated);
    }

    @Test void a_document_without_generated_names_has_an_empty_table() {
        RegelSpraakModel m = clean(VONDST.replace(" (mag opgegeven worden)", ""));
        assertEquals(Map.of(), m.generated);
    }

    @Test void the_keys_of_the_forms_lie_fixed() {
        assertEquals(List.of("opgave-boven-berekening", "uitzondering-voor-subtype", "beslistabelrij",
                        "stappen-na-de-verzameling", "startpunt", "defaultsstapel", "versie"),
                java.util.Arrays.stream(OriginForm.values()).map(OriginForm::key).toList());
    }
}
