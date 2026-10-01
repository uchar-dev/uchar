package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.ir.Conditie;
import dev.uchar.regelspraak.ir.Default;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.ExceptionGroup;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.model.RegelSpraakModel;
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
class AfwijkingOpSubtypeTest {

    private static final String KOP = """
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

    private static final String BASIS = """

        Regel meldtermijn
            geldig altijd
                De meldtermijn van een Jutter moet berekend worden als zijn vinddag plus 6 mnd
                indien zijn vinddag gevuld is.
        """;

    /** The block of the reproduction under the head {@code kop} ("Afwijking van", "Uitzondering op"). */
    private static String vergunningseinde(String kop) {
        return "\n" + kop + " meldtermijn: meldtermijn tot het vergunningseinde\n"
                + "    geldig altijd\n"
                + "        De meldtermijn van een Strandjutter moet gesteld worden op zijn vergunningseinde\n"
                + "        indien zijn vergunningseinde gevuld is.\n";
    }

    /** The two groups of a clean document: the base's on the supertype and the subtype's own. */
    private static List<PrioritizedDefaults> tweeGroepen(RegelSpraakModel m) {
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertEquals(0, m.resolvedRegels.size(), "elke regel van de boom hoort gelift te zijn");
        assertEquals(2, m.prioritizedDefaults.size(), () -> "twee defaults-groepen: " + m.prioritizedDefaults);
        return m.prioritizedDefaults;
    }

    /** The base's group on Jutter: the base alone, as without a subtype block. */
    private static void basisGroep(PrioritizedDefaults g) {
        assertEquals("meldtermijn", g.naam());
        assertEquals("Jutter", g.onderwerp());
        assertEquals("meldtermijn", g.attribuut());
        assertEquals(1, g.defaults().size(), "alleen de basis");
        assertEquals("zijn vinddag gevuld is", g.defaults().get(0).voorwaarde().bron().snippet());
        assertNotNull(g.defaults().get(0).consequentie());
    }

    /** The subtype's group: one group-default whose one ordered exception group holds the subtype's blocks; the
     *  defaults of that group are returned. The group's source says the base is inherited. */
    private static List<Default> subtypeGroep(PrioritizedDefaults g, PrioritizedDefaults basis, String kop) {
        assertEquals("meldtermijn voor Strandjutter", g.naam());
        assertEquals("Strandjutter", g.onderwerp());
        assertEquals("meldtermijn", g.attribuut());
        assertFalse(g.ongeordend());
        assertEquals(basis.geldigheid(), g.geldigheid(), "het venster van de basisregel");
        assertEquals("eigen uitzonderingen van Strandjutter; de basis 'meldtermijn' is geërfd van Jutter", g.bron());
        assertEquals(1, g.defaults().size(), "één groep-default");
        Default gd = g.defaults().get(0);
        assertNull(gd.voorwaarde(), "een groep-default draagt geen rechtvaardiging (WF-075)");
        assertNull(gd.consequentie(), "een groep-default draagt geen eigen consequentie: geen eigen basis");
        assertNull(gd.besluit());
        assertFalse(gd.initialisatie());
        assertEquals(kop + " meldtermijn", gd.bron());
        assertNotNull(gd.excepties());
        assertFalse(gd.excepties().ongeordend());
        assertEquals(1, gd.excepties().leden().size());
        ExceptionGroup eg = gd.excepties().leden().get(0);
        assertEquals(kop + " meldtermijn", eg.bron());
        return eg.defaults();
    }

    @Test void r1_een_afwijking_op_een_subtype() {
        RegelSpraakModel m = Resolver.resolve(KOP + BASIS + vergunningseinde("Afwijking van"));
        List<PrioritizedDefaults> groepen = tweeGroepen(m);
        basisGroep(groepen.get(0));
        List<Default> eigen = subtypeGroep(groepen.get(1), groepen.get(0), "Afwijking van");
        assertEquals(1, eigen.size());
        // an 'Afwijking van' widens: its own condition only
        assertEquals("zijn vergunningseinde gevuld is", eigen.get(0).voorwaarde().bron().snippet());
        assertEquals("zijn vergunningseinde", eigen.get(0).consequentie().bron().snippet());
        String ir = IrEmitter.emit(m);
        assertTrue(ir.contains("\"name\": \"meldtermijn voor Strandjutter\""), ir);
    }

    @Test void r2_een_uitzondering_op_een_subtype() {
        RegelSpraakModel m = Resolver.resolve(KOP + BASIS + vergunningseinde("Uitzondering op"));
        List<PrioritizedDefaults> groepen = tweeGroepen(m);
        basisGroep(groepen.get(0));
        List<Default> eigen = subtypeGroep(groepen.get(1), groepen.get(0), "Uitzondering op");
        assertEquals(1, eigen.size());
        // an 'Uitzondering op' is restrictive: the base's condition first, read on the subtype, then its own
        Conditie.Samengesteld alle = assertInstanceOf(Conditie.Samengesteld.class, eigen.get(0).voorwaarde());
        assertEquals(Conditie.Kwantor.ALLE, alle.kwantor());
        assertEquals(List.of("zijn vinddag gevuld is", "zijn vergunningseinde gevuld is"),
                alle.onderdelen().stream().map(c -> c.bron().snippet()).toList());
        Conditie.Predicaat vinddag = assertInstanceOf(Conditie.Predicaat.class, alle.onderdelen().get(0));
        Expr.AttribuutRef lezing = assertInstanceOf(Expr.AttribuutRef.class, vinddag.operand());
        assertEquals("Strandjutter", lezing.objecttype().naam,
                "een eigen lezing in een default van Strandjutter noemt Strandjutter (WF-053)");
        String ir = IrEmitter.emit(m);
        String subtype = ir.substring(ir.indexOf("\"name\": \"meldtermijn voor Strandjutter\""));
        assertFalse(subtype.contains("\"subjectType\": \"Jutter\""), subtype);
    }
}
