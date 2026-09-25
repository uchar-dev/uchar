package io.github.yhavinga.regelspraak;

import io.github.yhavinga.regelspraak.emit.IrEmitter;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Diagnostic;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Severity;
import io.github.yhavinga.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WF-113 (a) at the producer boundary, in the readers' sentences: the wire carries a diagnostic only as a warning
 * with a message that is not empty or blank and a line of at least 0, and each other shape is refused with the
 * sentence the verifier gives for its arm, at the pointer the verifier reports it at, followed by the refused
 * diagnostic itself. Measured on the frontend before this: one Dutch sentence for every arm.
 */
class DiagnoseWeigeringTest {

    private static String weigering(Diagnostic d) {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Jutter
                de vondstteller Numeriek (geheel getal);
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnoses: " + m.diagnostics);
        m.diagnostics.add(new Diagnostic(Severity.WARNING, "een melding", 0));
        m.diagnostics.add(d);
        return assertThrows(IllegalStateException.class, () -> IrEmitter.emit(m)).getMessage();
    }

    @Test void elke_arm_krijgt_de_zin_van_de_verifier() {
        assertEquals("IrEmitter: WF-113 (a) at /diagnostics/1/severity: severity error is not a wire severity: an "
                        + "error means the producer refused to write the document, so a document that was written "
                        + "carries none — ERROR (line 1): een melding",
                weigering(new Diagnostic(Severity.ERROR, "een melding", 1)));
        assertEquals("IrEmitter: WF-113 (a) at /diagnostics/1/severity: severity info is not a wire severity: the "
                        + "list is the account of what stayed unresolved, and info is tooling talk — INFO (line 1): "
                        + "een melding",
                weigering(new Diagnostic(Severity.INFO, "een melding", 1)));
        assertEquals("IrEmitter: WF-113 (a) at /diagnostics/1/severity: diagnostic without a severity: the one wire "
                        + "severity is warning — null (line 1): een melding",
                weigering(new Diagnostic(null, "een melding", 1)));
        for (String leeg : new String[] { null, "", "   " })
            assertEquals("IrEmitter: WF-113 (a) at /diagnostics/1/message: diagnostic with an empty or blank message: "
                            + "an entry that says nothing accounts for nothing — WARNING (line 1): " + leeg,
                    weigering(new Diagnostic(Severity.WARNING, leeg, 1)));
        assertEquals("IrEmitter: WF-113 (a) at /diagnostics/1/line: diagnostic with line -1: a line is a source "
                        + "position, and 0 is the value for none (a hand-built document, or a producer warning "
                        + "without a token) — WARNING (line -1): een melding",
                weigering(new Diagnostic(Severity.WARNING, "een melding", -1)));
    }

    @Test void een_diagnose_met_twee_gebreken_krijgt_beide_zinnen() {
        assertEquals("IrEmitter: WF-113 (a) at /diagnostics/1/severity: severity info is not a wire severity: the "
                        + "list is the account of what stayed unresolved, and info is tooling talk — INFO (line -2): "
                        + "een melding; at /diagnostics/1/line: diagnostic with line -2: a line is a source position, "
                        + "and 0 is the value for none (a hand-built document, or a producer warning without a token) "
                        + "— INFO (line -2): een melding",
                weigering(new Diagnostic(Severity.INFO, "een melding", -2)));
    }
}
