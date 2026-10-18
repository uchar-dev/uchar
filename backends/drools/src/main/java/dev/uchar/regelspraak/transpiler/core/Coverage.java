package dev.uchar.regelspraak.transpiler.core;

import dev.uchar.regelspraak.model.RegelSpraakModel;

/**
 * One entry in a back-end's coverage manifest: for one named construct, did the back-end lower it
 * to a live rule (LOWERED), emit an honest compilable no-op (MARKER — the DRL back-end's
 * {@code /* TODO *&#47;} / {@code // onopgelost} discipline), or emit nothing at all (SKIP — the
 * exec back-end's {@code overgeslagen} discipline)? Both transpilers produce a manifest over the
 * SAME model construct lists in the SAME order, so the two manifests are positionally comparable:
 * the differential suite diffs them and any legitimate divergence must be allowlisted explicitly.
 *
 * <p>The manifest replaces two weaker signals: the bare skip-COUNT cap (which could not say WHICH
 * construct regressed) and the silent marker (a semantically inert rule with no diagnostic). Every
 * non-LOWERED verdict therefore also surfaces as a deduplicated WARNING via {@link #report}.
 */
public record Coverage(String category, String naam, Verdict verdict, String reason, String spec) {

    public enum Verdict { LOWERED, MARKER, SKIP }

    public static Coverage lowered(String category, String naam) {
        return new Coverage(category, naam, Verdict.LOWERED, "", "");
    }

    public static Coverage marker(String category, String naam, String reason) {
        return new Coverage(category, naam, Verdict.MARKER, reason, specOut(reason));
    }

    public static Coverage skip(String category, String naam, String reason) {
        return new Coverage(category, naam, Verdict.SKIP, reason, specOut(reason));
    }

    /** The construct's identity across back-ends — soort plus its RegelSpraak name. */
    public String key() { return category + ": " + naam; }

    public boolean lowered() { return verdict == Verdict.LOWERED; }

    /** A non-LOWERED verdict is never silent: raise a WARNING on the model, deduplicated so a
     *  re-transpile of the same model (or both back-ends reaching the same verdict) does not pile up. */
    public void report(RegelSpraakModel model) {
        if (lowered()) return;
        var w = new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.WARNING,
                "dekking: " + category + " '" + naam + "' " + verdict
                + (reason.isEmpty() ? "" : " — " + reason), 0);
        if (!model.diagnostics.contains(w)) model.diagnostics.add(w);
    }

    /** The first spec-§ citation in the reason text, or "" — marker/skip reasons usually carry one. */
    private static String specOut(String reason) {
        var m = java.util.regex.Pattern.compile("§[0-9]+(?:\\.[0-9]+)*").matcher(reason);
        return m.find() ? m.group() : "";
    }
}
