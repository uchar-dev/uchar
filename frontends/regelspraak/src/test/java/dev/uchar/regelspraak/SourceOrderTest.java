package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.CaseEmitter;
import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import dev.uchar.regelspraak.resolver.ScenarioResolver;
import dev.uchar.regelspraak.resolver.ScenarioResolver.ScenarioBundel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RegelSpraak is declarative: what the frontend writes does not depend on the order of the rules in the source, nor on
 * the order of the files a model is put together from. One model, written in two orders, gives the same IR document
 * (the forms of a list in their own source order aside) and the same diagnostics, and so the same outcome in every
 * executor.
 *
 * <p><b>Red before the fix</b> (the frontend of {@code d07b9f93}), at two places where a rule read what another rule
 * had put in the model while the rules were resolved one by one. A fact creation whose path walks through the back-link
 * of an object creation ("de premiehaven van de verdeelde premiepot") counted only an object creation earlier in the
 * source; written above it, the fact creation was dropped with the WARNING "feitcreatie wordt nog niet naar IR/DRL
 * omgezet … referentieveld-zijde voor tussenstap 'premiehaven' niet af te leiden". The casus of the parity zone has the
 * shape, and the files of its toolchain put the object creation first. And the anchor member a startpuntbepaling
 * declares ("startpunt penningjaar") was declared when its rule came up, so a rule above it that reads the anchor
 * became an unresolved marker. A third place took two declarations of one name together, the last one winning: two
 * heads of one object type, or two parameters, domains, unit systems, fact types or dimensions of one name, which
 * WF-001 makes ill-formed and the IR, carrying one declaration, kept from the verifier.
 */
class SourceOrderTest {

    private static final String PREMIEPOT_MODEL = """
        Parameter de premie per havenjutter : Numeriek (geheel getal)

        Objecttype de Haven (mv: Havens)
            het premietotaal Numeriek (geheel getal);
            het premietal Numeriek (geheel getal);

        Objecttype de Jutter (mv: Jutters) (bezield)
            is premiegerechtigd kenmerk (bijvoeglijk);

        Objecttype de Premiepot (mv: Premiepotten)
            de pot Numeriek (geheel getal);
            het jutteraantal Numeriek (geheel getal);

        Feittype HavenHeeftJutter
            de haven Haven
            de havenjutter (mv: havenjutters) Jutter
        Eén haven heeft meerdere havenjutters

        Feittype HavenHeeftPremiepot
            de premiehaven Haven
            de premiepot Premiepot
        Eén premiehaven heeft één premiepot

        Feittype PremiepotVoorJutters
            de verdeelde premiepot Premiepot
            de premiejutter (mv: premiejutters) Jutter
        Eén verdeelde premiepot heeft meerdere premiejutters

        """;

    private static final String OBJECT_CREATION = """
        Regel premiepot
            geldig altijd
                Een Haven heeft de premiepot met pot gelijk aan het aantal havenjutters van de Haven maal de premie per havenjutter.

        """;

    private static final String FACT_CREATION = """
        Regel premiejutters
            geldig altijd
                Een premiejutter van een verdeelde premiepot is een havenjutter van de premiehaven van de verdeelde premiepot.

        """;

    private static final String READERS = """
        Regel jutteraantal
            geldig altijd
                Het jutteraantal van een Premiepot moet berekend worden als het aantal premiejutters van de Premiepot.

        Regel premietotaal
            geldig altijd
                Het premietotaal van een Haven moet berekend worden als de pot van de premiepot van de Haven.

        Regel premietal
            geldig altijd
                Het premietal van een Haven moet berekend worden als het jutteraantal van de premiepot van de Haven.

        Regel premiegerechtigd
            geldig altijd
                Een Jutter is premiegerechtigd
                indien hij een premiejutter is.

        """;

    private static final String SCENARIO = """
        Scenario twee havens
        Gegeven is de volgende situatie:
        de parameter de premie per havenjutter is 50
        een Haven H1
        een Haven H2
        een Jutter J1
        - de haven is H1
        een Jutter J2
        - de haven is H1
        een Jutter J3
        - de haven is H2
        een Jutter J4
        Met als verwacht resultaat:
        - het premietotaal van H1 is 100
        - het premietal van H1 is 2
        - het premietotaal van H2 is 50
        - het premietal van H2 is 1
        - J1 is premiegerechtigd
        - J4 is niet premiegerechtigd
        """;

    private static String emit(String bron) {
        RegelSpraakModel m = Resolver.resolve(bron);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        return IrEmitter.emit(m);
    }

    @Test void a_fact_creation_above_the_object_creation_where_it_runs_through_gives_the_same_document() {
        String firstCreation = emit(PREMIEPOT_MODEL + OBJECT_CREATION + FACT_CREATION + READERS);
        String firstFact = emit(PREMIEPOT_MODEL + FACT_CREATION + OBJECT_CREATION + READERS);
        assertTrue(firstCreation.contains("\"node\": \"FactCreation\""), firstCreation);
        // the creations stand in lists of their own, so the two documents are equal byte for byte
        assertEquals(firstCreation, firstFact);
        // declarations after the rules, and the readers first
        assertEquals(firstCreation, emit(READERS + FACT_CREATION + OBJECT_CREATION + PREMIEPOT_MODEL));
    }

    @Test void the_scenario_route_writes_in_both_orders_the_same_cases_and_the_same_document() {
        ScenarioBundel a = ScenarioResolver.resolve(PREMIEPOT_MODEL + OBJECT_CREATION + FACT_CREATION + READERS + SCENARIO);
        ScenarioBundel b = ScenarioResolver.resolve(PREMIEPOT_MODEL + FACT_CREATION + OBJECT_CREATION + READERS + SCENARIO);
        assertTrue(a.model().diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + a.model().diagnostics);
        assertTrue(b.model().diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + b.model().diagnostics);
        assertEquals(IrEmitter.emit(CaseEmitter.metScope(a.model(), a.scenarios().get(0).scope())),
                IrEmitter.emit(CaseEmitter.metScope(b.model(), b.scenarios().get(0).scope())));
        assertEquals(CaseEmitter.emit(a.scenarios().get(0)), CaseEmitter.emit(b.scenarios().get(0)));
    }

    private static final String START_POINT_MODEL = """
        Tijdlijn het penningjaar heeft tijdvakken van een jaar met variabel startpunt;

        Objecttype de Jutter (mv: Jutters) (bezield)
            de aanvangsdatum Datum in dagen;
            de begindag Datum in dagen;

        """;

    private static final String START_POINT = """
        Regel startpunt penningjaar
            geldig altijd
                Het startpunt van het penningjaar voor een Jutter wordt bepaald door zijn aanvangsdatum.

        """;

    private static final String ANCHOR_READER = """
        Regel begindag
            geldig altijd
                De begindag van een Jutter moet gesteld worden op zijn startpunt penningjaar.

        """;

    @Test void a_reader_of_the_start_point_anchor_above_the_start_point_determination_binds_the_anchor() {
        String firstDetermination = emit(START_POINT_MODEL + START_POINT + ANCHOR_READER);
        String firstReader = emit(START_POINT_MODEL + ANCHOR_READER + START_POINT);
        String attributeRead = "\"value\":{\"node\":\"AttributeReference\",\"attribute\":\"startpuntpenningjaar\"";
        assertTrue(firstDetermination.replaceAll("\\s+", "").contains(attributeRead), firstDetermination);
        assertTrue(firstReader.replaceAll("\\s+", "").contains(attributeRead), firstReader);
        // the rules list stands in source order; the rest of the document is the same
        assertEquals(withoutRules(firstDetermination), withoutRules(firstReader));
    }

    private static final String HEAD_NUMBER = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            de waarde Numeriek (geheel getal);

        Parameter de drempel : Numeriek (geheel getal)

        """;

    private static final String HEAD_TEXT = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            de waarde Tekst;

        Parameter de drempel : Tekst

        """;

    @Test void two_declarations_of_a_name_are_refused_in_both_orders() {
        // until the refusal the last head and the last parameter won: the model read 'waarde' and 'drempel' as text
        // in one order and as a number in the other, and wrote a document the verifier passed
        for (String[] order : List.of(new String[] {HEAD_NUMBER, HEAD_TEXT}, new String[] {HEAD_TEXT, HEAD_NUMBER})) {
            RegelSpraakModel m = Resolver.resolve(order[0] + order[1]);
            assertEquals(List.of(
                    "ERROR 6 objecttype 'Jutter' is al gedeclareerd (bronregel 1); een naam komt in zijn soort "
                            + "declaratie één keer voor, hoofdletters niet meegeteld (WF-001): namespace 'objectTypes': "
                            + "names 'Jutter' and 'Jutter' collide case-insensitively",
                    "ERROR 9 parameter 'drempel' is al gedeclareerd (bronregel 4); een naam komt in zijn soort "
                            + "declaratie één keer voor, hoofdletters niet meegeteld (WF-001): namespace 'parameters': "
                            + "names 'drempel' and 'drempel' collide case-insensitively"),
                    m.diagnostics.stream().map(d -> d.severity() + " " + d.line() + " " + d.message()).toList());
        }
    }

    /** The document without its list of rules, the one list the two orders above write in another order. */
    private static String withoutRules(String ir) {
        int van = ir.indexOf("\"rules\": [");
        int tot = ir.indexOf("\"dayKindDefinitions\"");
        assertTrue(van >= 0 && tot > van, ir);
        return ir.substring(0, van) + ir.substring(tot);
    }
}
