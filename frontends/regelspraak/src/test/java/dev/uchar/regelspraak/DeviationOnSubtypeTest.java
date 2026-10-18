package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.ir.Default;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.ExceptionGroup;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Dialect §4 over an inheritance line (grammar/DIVERGENCES.md §4, "Fail-fast refusals"; RUNTIME-SEMANTICS
 * ch. 7.9 and 7.10): an {@code Uitzondering op} or {@code Afwijking van} whose subject is a strict subtype of its
 * base rule's subject, on the target the subtype inherits, is the subtype's own exception without an own base. It
 * lowers to a defaults group on the subtype whose one default is a group-default holding the subtype's blocks, so
 * the composition of ch. 7.10 puts it above the inherited exceptions and keeps the inherited base below it. Measured
 * on the frontend before this card: both heads drew "schrijft een ander doel (Strandjutter.meldtermijn (attribuut))
 * dan de basisregel (Jutter.meldtermijn (attribuut))" (WF-050).
 */
class DeviationOnSubtypeTest {

    private static final String HEAD = """
        Eenheidsysteem Tijd
            de dag dg
            de week wk = 7 dg
            de maand mnd
            het jaar jr = 12 mnd

        Objecttype de Jutter (mv: Jutters) (bezield)
            de vinddag Datum in dagen;
            de meldtermijn Datum in dagen;
        Objecttype de Strandjutter is een Jutter (mv: Strandjutters)
            het vergunningseinde Datum in dagen;
        """;

    private static final String BASE = """

        Regel meldtermijn
            geldig altijd
                De meldtermijn van een Jutter moet berekend worden als zijn vinddag plus 6 mnd
                indien zijn vinddag gevuld is.
        """;

    /** The block of the reproduction under the head {@code head} ("Afwijking van", "Uitzondering op"). */
    private static String vergunningseinde(String head) {
        return "\n" + head + " meldtermijn: meldtermijn tot het vergunningseinde\n"
                + "    geldig altijd\n"
                + "        De meldtermijn van een Strandjutter moet gesteld worden op zijn vergunningseinde\n"
                + "        indien zijn vergunningseinde gevuld is.\n";
    }

    /** The two groups of a clean document: the base's on the supertype and the subtype's own. */
    private static List<PrioritizedDefaults> twoGroups(RegelSpraakModel m) {
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertEquals(0, m.resolvedRegels.size(), "elke regel van de boom hoort gelift te zijn");
        assertEquals(2, m.prioritizedDefaults.size(), () -> "twee defaults-groepen: " + m.prioritizedDefaults);
        return m.prioritizedDefaults;
    }

    /** The base's group on Jutter: the base alone, as without a subtype block. */
    private static void baseGroup(PrioritizedDefaults g) {
        assertEquals("meldtermijn", g.naam());
        assertEquals("Jutter", g.onderwerp());
        assertEquals("meldtermijn", g.attribute());
        assertEquals(1, g.defaults().size(), "alleen de basis");
        assertEquals("zijn vinddag gevuld is", g.defaults().get(0).condition().source().snippet());
        assertNotNull(g.defaults().get(0).consequence());
    }

    /** The subtype's group: one group-default whose one ordered exception group holds the subtype's blocks; the
     *  defaults of that group are returned. The group's source says the base is inherited. */
    private static List<Default> subtypeGroup(PrioritizedDefaults g, PrioritizedDefaults base, String head) {
        assertEquals("meldtermijn voor Strandjutter", g.naam());
        assertEquals("Strandjutter", g.onderwerp());
        assertEquals("meldtermijn", g.attribute());
        assertFalse(g.unordered());
        assertEquals(base.validity(), g.validity(), "het venster van de basisregel");
        assertEquals("eigen uitzonderingen van Strandjutter; de basis 'meldtermijn' is geërfd van Jutter", g.source());
        assertEquals(1, g.defaults().size(), "één groep-default");
        Default gd = g.defaults().get(0);
        assertNull(gd.condition(), "een groep-default draagt geen rechtvaardiging (WF-075)");
        assertNull(gd.consequence(), "een groep-default draagt geen eigen consequentie: geen eigen basis");
        assertNull(gd.decision());
        assertFalse(gd.initialisatie());
        assertEquals(head + " meldtermijn", gd.source());
        assertNotNull(gd.exceptions());
        assertFalse(gd.exceptions().unordered());
        assertEquals(1, gd.exceptions().members().size());
        ExceptionGroup eg = gd.exceptions().members().get(0);
        assertEquals(head + " meldtermijn", eg.source());
        return eg.defaults();
    }

    @Test void r1_a_deviation_on_a_subtype() {
        RegelSpraakModel m = Resolver.resolve(HEAD + BASE + vergunningseinde("Afwijking van"));
        List<PrioritizedDefaults> groups = twoGroups(m);
        baseGroup(groups.get(0));
        List<Default> own = subtypeGroup(groups.get(1), groups.get(0), "Afwijking van");
        assertEquals(1, own.size());
        // an 'Afwijking van' widens: its own condition only
        assertEquals("zijn vergunningseinde gevuld is", own.get(0).condition().source().snippet());
        assertEquals("zijn vergunningseinde", own.get(0).consequence().source().snippet());
        String ir = IrEmitter.emit(m);
        assertTrue(ir.contains("\"name\": \"meldtermijn voor Strandjutter\""), ir);
    }

    @Test void r2_a_exception_on_a_subtype() {
        RegelSpraakModel m = Resolver.resolve(HEAD + BASE + vergunningseinde("Uitzondering op"));
        List<PrioritizedDefaults> groups = twoGroups(m);
        baseGroup(groups.get(0));
        List<Default> own = subtypeGroup(groups.get(1), groups.get(0), "Uitzondering op");
        assertEquals(1, own.size());
        // an 'Uitzondering op' is restrictive: the base's condition first, read on the subtype, then its own
        Condition.Compound all = assertInstanceOf(Condition.Compound.class, own.get(0).condition());
        assertEquals(Condition.Quantifier.ALLE, all.quantifier());
        assertEquals(List.of("zijn vinddag gevuld is", "zijn vergunningseinde gevuld is"),
                all.components().stream().map(c -> c.source().snippet()).toList());
        Condition.Predicate vinddag = assertInstanceOf(Condition.Predicate.class, all.components().get(0));
        Expr.AttributeReference attributeRead = assertInstanceOf(Expr.AttributeReference.class, vinddag.operand());
        assertEquals("Strandjutter", attributeRead.objectType().naam,
                "een eigen lezing in een default van Strandjutter noemt Strandjutter (WF-053)");
        String ir = IrEmitter.emit(m);
        String subtype = ir.substring(ir.indexOf("\"name\": \"meldtermijn voor Strandjutter\""));
        assertFalse(subtype.contains("\"subjectType\": \"Jutter\""), subtype);
    }

    // ---------------------------------------------------------------------------------- the form space of the card

    /** The model of the measured forms: a grandchild, a sibling, and characteristics to write and to read. */
    private static final String MODEL = HEAD.replace("    de meldtermijn Datum in dagen;\n", """
                de meldtermijn Datum in dagen;
                is meldplichtig kenmerk (bijvoeglijk);
                is gewaarschuwd kenmerk (bijvoeglijk);
            """) + """
        Objecttype de Barnsteenjutter is een Strandjutter (mv: Barnsteenjutters)
            de keurdag Datum in dagen;
        Objecttype de Wrakjutter is een Jutter (mv: Wrakjutters)
            de bergdag Datum in dagen;
        """;

    private static String block(String head, String naam, String sentence, String condition) {
        return "\n" + head + naam + "\n    geldig altijd\n        " + sentence
                + (condition == null ? "." : "\n        indien " + condition + ".") + "\n";
    }

    private static String deviation(String base, String naam, String sentence, String condition) {
        return block("Afwijking van " + base + ": ", naam, sentence, condition);
    }

    private static String exception(String carrier, String naam, String sentence, String condition) {
        return block("Uitzondering op " + carrier + ": ", naam, sentence, condition);
    }

    private static final String VERGUNNINGSEINDE = deviation("meldtermijn", "meldtermijn tot het vergunningseinde",
            "De meldtermijn van een Strandjutter moet gesteld worden op zijn vergunningseinde",
            "zijn vergunningseinde gevuld is");
    private static final String WARNING = exception("meldtermijn", "meldtermijn bij waarschuwing",
            "De meldtermijn van een Jutter moet berekend worden als zijn vinddag plus 1 mnd", "hij gewaarschuwd is");
    private static final String BASE_STRANDJUTTER = block("Regel ", "meldtermijn van de strandjutter",
            "De meldtermijn van een Strandjutter moet berekend worden als zijn vinddag plus 3 mnd",
            "zijn vinddag gevuld is");

    private static Diagnostic onlyError(RegelSpraakModel m) {
        assertEquals(1, m.errors().size(), () -> "verwachtte precies één fout, kreeg: " + m.diagnostics);
        return m.errors().get(0);
    }

    private static List<String> names(RegelSpraakModel m) {
        return m.prioritizedDefaults.stream().map(PrioritizedDefaults::naam).toList();
    }

    /** The defaults of the one ordered exception group under the one group-default of a subtype's group. */
    private static List<Default> ownExceptions(PrioritizedDefaults g) {
        assertEquals(1, g.defaults().size());
        assertNotNull(g.defaults().get(0).exceptions());
        return g.defaults().get(0).exceptions().members().get(0).defaults();
    }

    @Test void a_grandchild_under_the_base_of_the_supertype() {
        RegelSpraakModel m = Resolver.resolve(MODEL + BASE + deviation("meldtermijn", "meldtermijn tot de keurdag",
                "De meldtermijn van een Barnsteenjutter moet gesteld worden op zijn keurdag", "zijn keurdag gevuld is"));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertEquals(List.of("meldtermijn", "meldtermijn voor Barnsteenjutter"), names(m));
        PrioritizedDefaults g = m.prioritizedDefaults.get(1);
        assertEquals("Barnsteenjutter", g.onderwerp());
        assertEquals("eigen uitzonderingen van Barnsteenjutter; de basis 'meldtermijn' is geërfd van Jutter", g.source());
        assertEquals(1, ownExceptions(g).size());
    }

    @Test void two_subtypes_each_have_their_own_group() {
        RegelSpraakModel m = Resolver.resolve(MODEL + BASE + VERGUNNINGSEINDE + deviation("meldtermijn",
                "meldtermijn na de bergdag", "De meldtermijn van een Wrakjutter moet berekend worden als zijn bergdag "
                        + "plus 1 mnd", "zijn bergdag gevuld is"));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertEquals(List.of("meldtermijn", "meldtermijn voor Strandjutter", "meldtermijn voor Wrakjutter"), names(m));
        assertEquals(1, m.prioritizedDefaults.get(0).defaults().size(), "de basis alleen");
    }

    @Test void an_exception_of_the_supertype_stays_in_its_group() {
        RegelSpraakModel m = Resolver.resolve(MODEL + BASE + WARNING + VERGUNNINGSEINDE);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertEquals(List.of("meldtermijn", "meldtermijn voor Strandjutter"), names(m));
        assertEquals(2, m.prioritizedDefaults.get(0).defaults().size(), "de uitzondering van Jutter boven de basis");
        assertEquals(1, ownExceptions(m.prioritizedDefaults.get(1)).size());
    }

    @Test void two_blocks_for_a_subtype_the_later_written_one_is_higher() {
        RegelSpraakModel m = Resolver.resolve(MODEL + BASE + VERGUNNINGSEINDE + deviation("meldtermijn",
                "meldtermijn bij waarschuwing", "De meldtermijn van een Strandjutter moet berekend worden als zijn "
                        + "vinddag plus 1 mnd", "hij gewaarschuwd is"));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        List<Default> own = ownExceptions(m.prioritizedDefaults.get(1));
        assertEquals(List.of("hij gewaarschuwd is", "zijn vergunningseinde gevuld is"),
                own.stream().map(d -> d.condition().source().snippet()).toList());
    }

    @Test void both_heads_for_a_subtype() {
        RegelSpraakModel m = Resolver.resolve(MODEL + BASE + exception("meldtermijn",
                "meldtermijn tot het vergunningseinde", "De meldtermijn van een Strandjutter moet gesteld worden op "
                        + "zijn vergunningseinde", "zijn vergunningseinde gevuld is")
                + deviation("meldtermijn", "meldtermijn bij waarschuwing", "De meldtermijn van een Strandjutter moet "
                        + "berekend worden als zijn vinddag plus 1 mnd", "hij gewaarschuwd is"));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        Default gd = m.prioritizedDefaults.get(1).defaults().get(0);
        assertEquals("Uitzondering op meldtermijn en Afwijking van meldtermijn", gd.source());
        assertEquals(gd.source(), gd.exceptions().members().get(0).source());
    }

    @Test void a_characteristic_as_target() {
        RegelSpraakModel m = Resolver.resolve(MODEL + block("Regel ", "meldplicht", "Een Jutter is meldplichtig",
                "zijn vinddag gevuld is") + exception("meldplicht", "vrijstelling met vergunning",
                "Een Strandjutter is niet meldplichtig", "zijn vergunningseinde gevuld is"));
        // no 'kent het nooit toe' warning: the group composes with the supertype's
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        PrioritizedDefaults g = m.prioritizedDefaults.get(1);
        assertEquals("meldplicht voor Strandjutter", g.naam());
        assertEquals("meldplichtig", g.characteristic());
        assertEquals(Boolean.FALSE, ownExceptions(g).get(0).decision());
    }

    @Test void the_condition_of_the_base_is_read_on_the_subtype() {
        RegelSpraakModel m = Resolver.resolve(MODEL + """
            Objecttype de Haven (mv: Havens)
                de sluitdag Datum in dagen;

            Feittype thuishaven van een jutter
                de thuishaven Haven
                de havenjutter (mv: havenjutters) Jutter
            Eén thuishaven heeft meerdere havenjutters
            """ + block("Regel ", "meldtermijn", "De meldtermijn van een Jutter moet berekend worden als zijn vinddag "
                + "plus 6 mnd", "hij aan alle volgende voorwaarden voldoet:\n        - zijn vinddag is gevuld\n"
                + "        - de sluitdag van zijn thuishaven is gevuld\n        - hij is niet gewaarschuwd")
                + exception("meldtermijn", "meldtermijn tot het vergunningseinde", "De meldtermijn van een "
                + "Strandjutter moet gesteld worden op zijn vergunningseinde", "zijn vergunningseinde gevuld is"));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        Condition.Compound base = (Condition.Compound) m.prioritizedDefaults.get(0).defaults().get(0).condition();
        Condition.Compound own = (Condition.Compound) ownExceptions(m.prioritizedDefaults.get(1)).get(0)
                .condition();
        assertEquals(4, own.components().size(), "drie conjuncten van de basis, dan de eigen voorwaarde");
        Expr.AttributeReference vinddag = (Expr.AttributeReference) ((Condition.Predicate) own.components().get(0)).operand();
        assertEquals("Strandjutter", vinddag.objectType().naam, "een eigen lezing noemt het subtype");
        Expr.AttributeReference sluitdag = (Expr.AttributeReference) ((Condition.Predicate) own.components().get(1)).operand();
        assertEquals("Haven", sluitdag.objectType().naam, "een genavigeerde lezing houdt haar eindpunt");
        assertSame(base.components().get(1), own.components().get(1), "geen eigen lezing: dezelfde knoop");
        assertSame(base.components().get(2), own.components().get(2), "een kenmerkcheck noemt geen type");
        assertNotSame(base.components().get(0), own.components().get(0));
        String ir = IrEmitter.emit(m);
        String subtype = ir.substring(ir.indexOf("\"name\": \"meldtermijn voor Strandjutter\""));
        assertFalse(subtype.contains("\"subjectType\": \"Jutter\""), subtype);
    }

    @Test void the_input_marking_also_moves_the_group_of_the_subtype() {
        RegelSpraakModel m = Resolver.resolve(MODEL.replace("de meldtermijn Datum in dagen;",
                "de meldtermijn Datum in dagen (mag opgegeven worden);") + BASE + VERGUNNINGSEINDE);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        PrioritizedDefaults sub = m.prioritizedDefaults.stream()
                .filter(g -> g.naam().equals("meldtermijn voor Strandjutter")).findFirst().orElseThrow();
        assertEquals("berekende meldtermijn", sub.attribute());
    }

    @Test void its_own_base_on_the_subtype_stays_playing() {
        // the second shape of the card (R4): an own base on the subtype and the deviation under it, unchanged
        RegelSpraakModel m = Resolver.resolve(HEAD + BASE + BASE_STRANDJUTTER + deviation(
                "meldtermijn van de strandjutter", "meldtermijn tot het vergunningseinde", "De meldtermijn van een "
                + "Strandjutter moet gesteld worden op zijn vergunningseinde", "zijn vergunningseinde gevuld is"));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertEquals(List.of("meldtermijn van de strandjutter"), names(m));
        assertEquals(1, m.resolvedRegels.size(), "de basis op Jutter blijft een gewone regel");
    }

    // -------------------------------------------------------------------------------------------------- refusals

    @Test void the_nested_form_is_refused_and_names_the_base() {
        Diagnostic d = onlyError(Resolver.resolve(MODEL + BASE + WARNING + exception(
                "meldtermijn bij waarschuwing", "waarschuwing met vergunning", "De meldtermijn van een Strandjutter "
                + "moet gesteld worden op zijn vergunningseinde", "zijn vergunningseinde gevuld is")));
        assertEquals("Uitzondering op 'meldtermijn bij waarschuwing': 'meldtermijn bij waarschuwing' is een "
                + "uitzondering van Jutter, en uitzondering 'waarschuwing met vergunning' schrijft voor het subtype "
                + "Strandjutter — een subtype erft de uitzonderingen van zijn supertype met wat eronder genest is, en "
                + "adresseert er geen (RUNTIME-SEMANTICS ch. 7.10). Schrijf haar als eigen uitzondering van Strandjutter "
                + "onder de basisregel 'meldtermijn': 'Afwijking van meldtermijn: waarschuwing met vergunning', met de "
                + "volledige voorwaarde van 'meldtermijn bij waarschuwing' en de eigen voorwaarde: de eigen "
                + "uitzonderingen van Strandjutter gaan vóór de geërfde, dus zij beslist precies waar beide gelden "
                + "(dialect §4)", d.message());
    }

    @Test void a_subtype_block_for_a_block_of_the_supertype_is_refused() {
        Diagnostic d = onlyError(Resolver.resolve(MODEL + BASE + VERGUNNINGSEINDE + WARNING));
        assertEquals("Afwijking van 'meldtermijn': afwijking 'meldtermijn tot het vergunningseinde' voor Strandjutter "
                + "staat vóór uitzondering 'meldtermijn bij waarschuwing' voor Jutter onder 'meldtermijn', en het later "
                + "geschreven blok gaat voor; RUNTIME-SEMANTICS ch. 7.10 zet de eigen uitzonderingen van Strandjutter "
                + "echter boven de geërfde van Jutter, zodat bij een Strandjutter 'meldtermijn tot het vergunningseinde' "
                + "vóór 'meldtermijn bij waarschuwing' gaat. Schrijf 'meldtermijn tot het vergunningseinde' na "
                + "'meldtermijn bij waarschuwing' (dialect §4)", d.message());
    }

    @Test void its_own_base_of_the_subtype_refuses_the_block_under_the_base_of_the_supertype() {
        Diagnostic d = onlyError(Resolver.resolve(MODEL + BASE + BASE_STRANDJUTTER + VERGUNNINGSEINDE));
        assertEquals("Afwijking van 'meldtermijn': afwijking 'meldtermijn tot het vergunningseinde' schrijft voor het "
                + "subtype Strandjutter, en Strandjutter heeft een eigen basisregel 'meldtermijn van de strandjutter' "
                + "voor 'meldtermijn', die 'meldtermijn' voor Strandjutter vervangt (RUNTIME-SEMANTICS ch. 7.10) — een "
                + "eigen uitzondering van Strandjutter hangt onder die basisregel. Schrijf 'Afwijking van meldtermijn van "
                + "de strandjutter: meldtermijn tot het vergunningseinde' (dialect §4)", d.message());
        Diagnostic u = onlyError(Resolver.resolve(MODEL + BASE + BASE_STRANDJUTTER + exception("meldtermijn",
                "meldtermijn tot het vergunningseinde", "De meldtermijn van een Strandjutter moet gesteld worden op "
                + "zijn vergunningseinde", "zijn vergunningseinde gevuld is")));
        assertTrue(u.message().endsWith("Schrijf 'Uitzondering op meldtermijn van de strandjutter: meldtermijn tot het "
                + "vergunningseinde' als zij de voorwaarde van 'meldtermijn van de strandjutter' erft, of 'Afwijking van "
                + "meldtermijn van de strandjutter: meldtermijn tot het vergunningseinde' met de volledige voorwaarde "
                + "(dialect §4)"), u.message());
    }

    @Test void its_own_base_on_an_intermediate_type_is_refused() {
        Diagnostic d = onlyError(Resolver.resolve(MODEL + BASE + BASE_STRANDJUTTER + deviation("meldtermijn",
                "keuring", "De meldtermijn van een Barnsteenjutter moet gesteld worden op zijn keurdag",
                "zijn keurdag gevuld is")));
        assertEquals("Afwijking van 'meldtermijn': afwijking 'keuring' schrijft voor het subtype Barnsteenjutter, en "
                + "Barnsteenjutter erft van Strandjutter de basisregel 'meldtermijn van de strandjutter' voor "
                + "'meldtermijn', die 'meldtermijn' voor Barnsteenjutter vervangt (RUNTIME-SEMANTICS ch. 7.10) — een eigen "
                + "uitzondering van Barnsteenjutter hangt onder die basisregel. Schrijf 'Afwijking van meldtermijn van de "
                + "strandjutter: keuring' (dialect §4)", d.message());
    }

    @Test void an_unordered_block_for_a_subtype_is_refused() {
        String members = "\nUitzonderingen ongeordend op %s:" + block("Regel ", "lid vergunning",
                "De meldtermijn van een Strandjutter moet gesteld worden op zijn vergunningseinde",
                "zijn vergunningseinde gevuld is") + block("Regel ", "lid waarschuwing",
                "De meldtermijn van een Strandjutter moet berekend worden als zijn vinddag plus 1 mnd",
                "hij gewaarschuwd is");
        Diagnostic d = onlyError(Resolver.resolve(MODEL + BASE + members.formatted("meldtermijn")));
        assertEquals("Uitzonderingen ongeordend op 'meldtermijn': groepslid 'lid vergunning' schrijft voor het subtype "
                + "Strandjutter onder de basisregel 'meldtermijn' van Jutter — een 'Uitzonderingen ongeordend op'-blok "
                + "in de eigen uitzonderingen van een subtype onder de basisregel van zijn supertype lowert deze frontend "
                + "niet: een conflict tussen de leden zou een sleutel van de samengestelde stapel krijgen "
                + "(RUNTIME-SEMANTICS ch. 7.9 en 7.10). Schrijf de leden als geordende blokken ('Uitzondering op' of "
                + "'Afwijking van'), of geef Strandjutter een eigen basisregel en hang het blok daaronder (dialect §4)",
                d.message());
        // under the subtype's own block as well
        Diagnostic e = onlyError(Resolver.resolve(MODEL + BASE + VERGUNNINGSEINDE
                + members.formatted("meldtermijn tot het vergunningseinde")));
        assertTrue(e.message().startsWith("Uitzonderingen ongeordend op 'meldtermijn tot het vergunningseinde': "
                + "groepslid 'lid vergunning' schrijft voor het subtype Strandjutter onder de basisregel 'meldtermijn' "
                + "van Jutter"), e.message());
    }

    @Test void a_taken_group_name_is_refused() {
        Diagnostic d = onlyError(Resolver.resolve(MODEL + BASE + VERGUNNINGSEINDE + block("Regel ",
                "meldtermijn voor Strandjutter", "De vinddag van een Wrakjutter moet gesteld worden op zijn bergdag",
                null)));
        assertTrue(d.message().startsWith("Afwijking van 'meldtermijn': de eigen uitzonderingen van Strandjutter onder "
                + "'meldtermijn' worden de defaults-groep 'meldtermijn voor Strandjutter', en regel 'meldtermijn voor "
                + "Strandjutter' heet al zo (bronregel "), d.message());
        assertTrue(d.message().endsWith(") — een vormnaam is uniek in het hele model, hoofdletters niet meegeteld "
                + "(WF-020) (dialect §4)"), d.message());
    }

    @Test void the_reversed_direction_keeps_the_target_refusal() {
        // a block for the supertype under a base of the subtype: WF-050 keeps its sentence
        Diagnostic d = onlyError(Resolver.resolve(MODEL + BASE_STRANDJUTTER + deviation(
                "meldtermijn van de strandjutter", "meldtermijn bij waarschuwing",
                "De meldtermijn van een Jutter moet gesteld worden op zijn vinddag", "hij gewaarschuwd is")));
        assertEquals("Afwijking van 'meldtermijn van de strandjutter': afwijking 'meldtermijn bij waarschuwing' schrijft "
                + "een ander doel (Jutter.meldtermijn (attribuut)) dan de basisregel (Strandjutter.meldtermijn "
                + "(attribuut)) — een afwijking moet hetzelfde onderwerp en kenmerk/attribuut betreffen (§4, WF-050)",
                d.message());
    }

    @Test void another_inheritance_line_and_another_attribute_keep_the_target_refusal() {
        Diagnostic sister = onlyError(Resolver.resolve(MODEL + BASE_STRANDJUTTER + deviation(
                "meldtermijn van de strandjutter", "meldtermijn na de bergdag",
                "De meldtermijn van een Wrakjutter moet gesteld worden op zijn bergdag", "zijn bergdag gevuld is")));
        assertTrue(sister.message().startsWith("Afwijking van 'meldtermijn van de strandjutter': afwijking 'meldtermijn "
                + "na de bergdag' schrijft een ander doel (Wrakjutter.meldtermijn (attribuut)) dan de basisregel "
                + "(Strandjutter.meldtermijn (attribuut))"), sister.message());
        Diagnostic attribute = onlyError(Resolver.resolve(MODEL + BASE + deviation("meldtermijn",
                "vinddag tot het vergunningseinde", "De vinddag van een Strandjutter moet gesteld worden op zijn "
                + "vergunningseinde", "zijn vergunningseinde gevuld is")));
        assertTrue(attribute.message().startsWith("Afwijking van 'meldtermijn': afwijking 'vinddag tot het "
                + "vergunningseinde' schrijft een ander doel (Strandjutter.vinddag (attribuut)) dan de basisregel "
                + "(Jutter.meldtermijn (attribuut))"), attribute.message());
    }
}
