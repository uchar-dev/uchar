package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.ir.Bron;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.ResolvedRegel;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The emitter refuses a source text no reader accepts in the sentence the readers give for it, so the producer's
 * refusal and the reader's name one defect with one sentence. An empty snippet is a load refusal in every reader,
 * a marker too (IR-SPEC ch. 4, the source record); a marker without a source and one whose snippet is blank are
 * WF-113's two arms in the verifier. Measured on the frontend before this: the three refusals were written in Dutch,
 * and an empty and a blank marker snippet shared one sentence.
 */
class BrontekstWeigeringTest {

    private static final String BRON = """
        Objecttype de Jutter
            de vondstteller Numeriek (geheel getal);
            de jaarsom Numeriek (geheel getal);
        Regel jaarsom
            geldig altijd
                De jaarsom van een Jutter moet berekend worden als de vondstteller van de Jutter min 6.
        """;

    private static final String LEGE_SNIPPET =
            "IrEmitter: a source snippet is never empty: a node without source text carries no source";

    /** The model with the value of its one rule replaced. */
    private static RegelSpraakModel metWaarde(java.util.function.Function<Expr, Expr> vervang) {
        RegelSpraakModel m = Resolver.resolve(BRON);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        ResolvedRegel r = m.resolvedRegels.get(0);
        m.resolvedRegels.set(0, new ResolvedRegel(r.naam(), r.soort(), r.doel(), vervang.apply(r.waarde()),
                r.voorwaarde(), r.geldigheid()));
        return m;
    }

    private static String weigering(RegelSpraakModel m) {
        return assertThrows(IllegalStateException.class, () -> IrEmitter.emit(m)).getMessage();
    }

    @Test void een_lege_snippet_krijgt_de_zin_van_de_lader() {
        RegelSpraakModel m = metWaarde(w -> {
            Expr.Binair b = (Expr.Binair) w;
            Expr.Literal l = (Expr.Literal) b.rechts();
            return new Expr.Binair(b.op(), b.links(), new Expr.Literal(l.waarde(), l.type(), Bron.synthetisch("")),
                    b.type(), b.bron());
        });
        assertEquals(LEGE_SNIPPET, weigering(m));
    }

    @Test void een_marker_krijgt_de_zin_van_zijn_arm() {
        String zonderBron = "IrEmitter: WF-113 (b) — Unresolved marker without a source: no diagnostic message can "
                + "contain it, so it cannot be accounted for";
        String blanco = "IrEmitter: WF-113 (b) — Unresolved marker whose source snippet is blank: blank counts as "
                + "absent, since nearly every message contains it and a message about another marker would account "
                + "for this one";
        Object[][] gevallen = {
                { null, zonderBron }, { Bron.synthetisch(null), zonderBron },
                { Bron.synthetisch("   "), blanco }, { Bron.synthetisch(""), LEGE_SNIPPET } };
        for (Object[] g : gevallen) {
            Bron bron = (Bron) g[0];
            RegelSpraakModel m = metWaarde(w -> new Expr.Onopgelost(w.type(), bron, "test"));
            m.diagnostics.add(new Diagnostic(Severity.WARNING, "een melding", 1));
            assertEquals(g[1], weigering(m), () -> "de marker met bron "
                    + (bron == null ? "null" : bron.snippet() == null ? "zonder snippet" : "'" + bron.snippet() + "'"));
        }
    }
}
