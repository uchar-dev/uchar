package io.github.yhavinga.regelspraak.transpiler.kern;

import java.util.Map;

/**
 * THE multiplicity question of a timeline facet, asked in ONE place by both transpiler branches: is this member a
 * MULTI-PERIOD raster (§13.3.6.7 "voor elke N maanden"), whose producer semantics neither branch carries?
 *
 * <p>It exists because the question was written 24 times as {@code tijdlijnAantal.containsKey(lid)} — a PRESENCE test
 * where a multiplicity test is meant. The wire's period count defaults to ONE: IR-SPEC §2.2's TimelineFacet table
 * states it as {@code count: integer >= 1 | periods per segment; 1 = single-period}, ch. 2.8 repeats the constraint on
 * the Timeline node and its worked example adopts {@code {granularity: year, count: 1}}, ch. 2.10's v0 migration
 * note reads a bare granularity as {@code {granularity, count: 1}}, and the verifier's own WF-005 named-timeline
 * adoption substitutes 1 for an absent entry. An explicit {@code 1} is therefore the SAME facet as no entry at all, and
 * a raster starts at 2.
 *
 * <p>The RegelSpraak frontend writes the entry only for a count other than 1 ({@code DefinitionResolver}), so no
 * frontend-produced document ever carried it at 1 and the presence test never fired wrongly on this repository's own
 * emitter. Every OTHER producer of the wire — a hand-built document, another frontend — walked into a silent
 * over-rejection: measured 2026-09-11 over the corpus, an explicit {@code 1} on a temporal member flipped 32 of 46
 * member positions (12 of the 17 goldens with temporal members and case files) out of the played channel, in BOTH
 * branches, while the verifier, {@code ref} and clingo all stayed unchanged on the same documents.
 *
 * <p>An explicit count BELOW 1 is WF-005's business, not this predicate's: the verifier rejects it, so it reaches no
 * transpiler through any gate, and it reads here as single-period rather than as a raster.
 *
 * <p>A JSON <em>null</em> is a different, REACHABLE case, and the change of behaviour is real rather than theoretical.
 * The schema types these values {@code ["integer","null"]} and WF-005 fires only on a present count below 1 — it does
 * not see an absent one — so a null-valued entry is verifier-CLEAN. The loader reads it as 0, which this predicate
 * reads as single-period where the presence test read it as a raster. Measured 2026-09-11 on
 * {@code corpus/golden/synth-gehelejaarkenmerk} with {@code kenmerkTijdlijnAantal {"bedrijvig": null}}: verifier PASS,
 * {@code ref} and clingo PASS, both Drools branches EXCLUDE before this predicate and PLAYED 4 after. The new outcome
 * is the default-1 reading the verifier itself applies (WF-005's named-timeline adoption substitutes 1 for an absent
 * count), so the three executors now agree where they did not. Whether the LOADER should refuse an explicit null
 * fail-fast instead is a separate, open question: that would be a new refusal on a verifier-clean class of documents,
 * and therefore a maintainer decision rather than something to add in passing.
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
