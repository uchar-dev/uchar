package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.ir.Source;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.ResolvedRule;
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
class SourceTextRefusalTest {

    private static final String SOURCE = """
        Objecttype de Jutter
            de vondstteller Numeriek (geheel getal);
            de jaarsom Numeriek (geheel getal);
        Regel jaarsom
            geldig altijd
                De jaarsom van een Jutter moet berekend worden als de vondstteller van de Jutter min 6.
        """;

    private static final String EMPTY_SNIPPET =
            "IrEmitter: a source snippet is never empty: a node without source text carries no source";

    /** The model with the value of its one rule replaced. */
    private static RegelSpraakModel withValue(java.util.function.Function<Expr, Expr> replace) {
        RegelSpraakModel m = Resolver.resolve(SOURCE);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        ResolvedRule r = m.resolvedRegels.get(0);
        m.resolvedRegels.set(0, new ResolvedRule(r.naam(), r.category(), r.target(), replace.apply(r.value()),
                r.condition(), r.validity()));
        return m;
    }

    private static String refusal(RegelSpraakModel m) {
        return assertThrows(IllegalStateException.class, () -> IrEmitter.emit(m)).getMessage();
    }

    @Test void an_empty_snippet_gets_the_sentence_of_the_loader() {
        RegelSpraakModel m = withValue(w -> {
            Expr.Binary b = (Expr.Binary) w;
            Expr.Literal l = (Expr.Literal) b.right();
            return new Expr.Binary(b.op(), b.left(), new Expr.Literal(l.valueText(), l.type(), Source.synthetic("")),
                    b.type(), b.source());
        });
        assertEquals(EMPTY_SNIPPET, refusal(m));
    }

    @Test void a_marker_gets_the_sentence_of_its_arm() {
        String withoutSource = "IrEmitter: WF-113 (b) — Unresolved marker without a source: no diagnostic message can "
                + "contain it, so it cannot be accounted for";
        String blanco = "IrEmitter: WF-113 (b) — Unresolved marker whose source snippet is blank: blank counts as "
                + "absent, since nearly every message contains it and a message about another marker would account "
                + "for this one";
        Object[][] cases = {
                { null, withoutSource }, { Source.synthetic(null), withoutSource },
                { Source.synthetic("   "), blanco }, { Source.synthetic(""), EMPTY_SNIPPET } };
        for (Object[] g : cases) {
            Source source = (Source) g[0];
            RegelSpraakModel m = withValue(w -> new Expr.Unresolved(w.type(), source, "test"));
            m.diagnostics.add(new Diagnostic(Severity.WARNING, "een melding", 1));
            assertEquals(g[1], refusal(m), () -> "de marker met bron "
                    + (source == null ? "null" : source.snippet() == null ? "zonder snippet" : "'" + source.snippet() + "'"));
        }
    }
}
