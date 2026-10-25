package dev.uchar.regelspraak.transpiler.core;

import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.model.RegelSpraakModel;

import java.util.Map;

/**
 * THE multiplicity question of a timeline facet, asked in ONE place by both transpiler branches: is this member a
 * MULTI-PERIOD raster (§13.3.6.7 "voor elke N maanden"), whose producer semantics neither branch carries?
 *
 * <p>It exists because a PRESENCE test, {@code timelineCount.containsKey(member)}, is not the multiplicity test that is
 * meant. The wire's period count defaults to ONE: IR-SPEC §2.2's TimelineFacet table
 * states it as {@code count: integer >= 1 | periods per segment; 1 = single-period}, ch. 2.8 repeats the constraint on
 * the Timeline node and its worked example adopts {@code {granularity: year, count: 1}}, ch. 2.10's v0 migration
 * note reads a bare granularity as {@code {granularity, count: 1}}, and the verifier's own WF-005 named-timeline
 * adoption substitutes 1 for an absent entry. An explicit {@code 1} is therefore the SAME facet as no entry at all, and
 * a raster starts at 2.
 *
 * <p>Every {@code TimelineFacet} carries its count and the IR loader keeps it, 1 included (M1), so an explicit 1 is
 * the ordinary entry this predicate meets on every temporal member. A presence test there is a silent
 * over-rejection: measured 2026-09-11 over the corpus, an explicit {@code 1} on a temporal member flipped 32 of 46
 * member positions (12 of the 17 goldens with temporal members and case files) out of the played channel, in BOTH
 * branches, while the verifier, {@code ref} and clingo all stayed unchanged on the same documents.
 *
 * <p>An explicit count BELOW 1 is WF-005's business, not this predicate's: the verifier rejects it, so it reaches no
 * transpiler through any gate, and it reads here as single-period rather than as a raster.
 *
 * <p>The count is a required integer on every {@code TimelineFacet} (M1): the schema and the loaders refuse a
 * null or absent count (this backend's as a document defect, {@code IrLoader.DocumentError}), so a null count
 * never reaches this predicate.
 */
public final class Timelines {
    private Timelines() {}

    /** Whether {@code member} is declared a MULTI-PERIOD raster in {@code timelineCount} — two periods or more. An absent
     *  entry and an explicit 1 are the same single-period facet (see the class note). {@code member} is the WRITTEN name,
     *  resolved by the same name contract as the temporality test beside it, or a mis-capitalised raster would pass
     *  that test and then read here as single-period. */
    public static boolean morePeriodic(Map<String, Integer> timelineCount, String member) {
        Integer count = Namen.byName(timelineCount, member);
        return count != null && count > 1;
    }

    /** RUNTIME-SEMANTICS ch. 3.2: why a pro-rata part onto {@code target} of {@code subject} is not carried when its
     *  target or its base lies on the grid of a fixed start point, or null. The base is constant across a bucket
     *  because its grid nests in the target's, and a start point off the calendar breaks that nesting; {@code ref}
     *  and the ASP backend refuse the form, so both branches do as well, in their sentence. */
    public static String proRataFixedGrid(RegelSpraakModel.ObjectType subject, String target, Expr value) {
        if (!(value instanceof Expr.FunctionCall f) || f.kind() != Expr.FunctionKind.TIJDSEVENREDIG_DEEL) return null;
        boolean fixed = subject != null && subject.fixedStartPointOf(target) != null;
        for (Expr base : f.arguments()) {
            if (base instanceof Expr.AttributeReference ba && ba.objectType() != null
                    && ba.objectType().fixedStartPointOf(ba.attribute()) != null) fixed = true;
            if (base instanceof Expr.ParameterRef bp && bp.parameter() != null
                    && bp.parameter().fixedStartPoint() != null) fixed = true;
        }
        return fixed ? "pro-rata onto or from a grid that does not lie on the calendar is fenced (RUNTIME-SEMANTICS "
                + "ch. 3.2)" : null;
    }
}
