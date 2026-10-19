package dev.uchar.regelspraak.engine;

import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;

import java.util.List;

/** Thrown by {@code RegelSpraakEngine#compile} when a RegelSpraak source has resolver errors, carrying the
 *  diagnostics (severity / message / line) so the caller can surface them. */
public final class RegelSpraakCompileException extends RuntimeException {
    private final transient List<Diagnostic> diagnostics;

    public RegelSpraakCompileException(List<Diagnostic> diagnostics) {
        super("RegelSpraak compile-fouten: " + diagnostics);
        this.diagnostics = List.copyOf(diagnostics);
    }

    /** the fact-model contract: the bean↔model contract violations, ALL of them at once (line 0 — a bean defect has no
     *  source line), so a consumer fixes the fact classes in one round trip. */
    public static RegelSpraakCompileException contract(List<String> violations) {
        return new RegelSpraakCompileException(violations.stream()
                .map(s -> new Diagnostic(dev.uchar.regelspraak.model.RegelSpraakModel.Severity.ERROR,
                        "RGS-345", Diagnostic.params("violation", s), 0)).toList());
    }

    public List<Diagnostic> diagnostics() { return diagnostics; }
}
