package io.github.yhavinga.regelspraak.transpiler.kern;

import java.util.Map;

/**
 * THE multiplicity question of a timeline facet, asked in ONE place by both transpiler branches: is this member a
 * MULTI-PERIOD raster (§13.3.6.7 "voor elke N maanden"), whose producer semantics neither branch carries?
 *
 * <p>It exists because the question was written 24 times as {@code tijdlijnAantal.containsKey(lid)} — a PRESENCE test
 * where a multiplicity test is meant. The wire's period count defaults to ONE: IR-SPEC ch. 2.8 states the facet as
 * {@code count: integer >= 1}, its worked example adopts {@code {granularity: year, count: 1}}, ch. 2.10's v0 migration
 * note reads a bare granularity as {@code {granularity, count: 1}}, and the verifier's own WF-005 named-timeline
 * adoption substitutes 1 for an absent entry. An explicit {@code 1} is therefore the SAME facet as no entry at all, and
 * a raster starts at 2.
 *
 * <p>The RegelSpraak frontend writes the entry only for a count other than 1 ({@code DefinitionResolver}), so no
 * frontend-produced document ever carried it at 1 and the presence test never fired wrongly on this repository's own
 * emitter. Every OTHER producer of the wire — a hand-built document, another frontend — walked into a silent
 * over-rejection: measured 2026-09-11 over the corpus, an explicit {@code 1} on a temporal member flipped 32 of 46
 * member positions (12 of the 16 goldens with temporal members and case files) out of the played channel, in BOTH
 * branches, while the verifier, {@code ref} and clingo all stayed unchanged on the same documents.
 *
 * <p>Values below 1 are WF-005's business, not this predicate's: a count of 0 (or a JSON null, which the loader reads
 * as 0) makes the document verifier-REJECTED, so it never reaches a transpiler through any gate. Such a count reads
 * here as single-period rather than as a raster — the refusal belongs to the rule that owns it.
 */
public final class Tijdlijnen {
    private Tijdlijnen() {}

    /** Whether {@code lid} is declared a MULTI-PERIOD raster in {@code tijdlijnAantal} — two periods or more. An absent
     *  entry and an explicit 1 are the same single-period facet (see the class note). */
    public static boolean meerPeriodiek(Map<String, Integer> tijdlijnAantal, String lid) {
        Integer aantal = tijdlijnAantal.get(lid);
        return aantal != null && aantal > 1;
    }
}
