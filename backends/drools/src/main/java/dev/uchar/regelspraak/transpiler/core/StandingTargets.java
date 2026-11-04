package dev.uchar.regelspraak.transpiler.core;

import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.ir.ResolvedObjectCreation;
import dev.uchar.regelspraak.ir.ResolvedRule;
import dev.uchar.regelspraak.ir.ResolvedAllocation;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * THE target classification of RUNTIME-SEMANTICS ch. 7.8 (the standing norm on a scalar target) and ch. 7.7 (the v0
 * desugaring domain) — the one derivation the analysis of both Drools branches reads. It is a port of ref's
 * {@code Desugar} ({@code standing_targets_gen}, {@code standing_targets}, {@code desugared_targets}), clause for
 * clause, so a target is standing for the Drools backend exactly when it is standing for ref.
 *
 * <p>WHY it is one class and not two mirrored methods: a second implementation is a second chance to drift; the
 * DOMAIN is the whole content of the norm, since the norm's one visible act is deciding WHERE a leeg write
 * is suppressed. An implementation that suppressed EVERY leeg write would pass every standing witness and
 * break the v0 multi-writer flank, so the boundary is what has to be shared, not the guard.
 *
 * <p>The sets are computed once per instance and cached; an instance belongs to one transpile of one model.
 */
public final class StandingTargets {

    private final RegelSpraakModel model;
    private final List<PrioritizedDefaults> prioritizedDefaults;

    private Set<String> scalar;
    private Set<String> desugared;
    private Map<String, Integer> writers;

    private StandingTargets(RegelSpraakModel model, List<PrioritizedDefaults> prioritizedDefaults) {
        this.model = model;
        this.prioritizedDefaults = prioritizedDefaults == null ? List.of() : prioritizedDefaults;
    }

    /** The classification of {@code model} with the ch. 7 prioritized-defaults extension beside it (the
     *  {@code other_writer} clause consults it). A flat v0 document passes an empty list. */
    public static StandingTargets of(RegelSpraakModel model, List<PrioritizedDefaults> prioritizedDefaults) {
        return new StandingTargets(model, prioritizedDefaults);
    }

    /** ch. 7.8, the SCALAR arm TOGETHER with the ch. 7.7 desugaring domain ref's scalar arm consults beside
     *  it: does a LEEG (null) computed value of {@code r} decide nothing, so that the case input — or, in the
     *  desugaring domain, the initialisatie — stands? Only a COMPUTING rule is in ref's arm: a
     *  KENMERKTOEKENNING must never inherit it (the key carries no member kind, WF-002), and an
     *  INITIALISATIE keeps its own §9.6 leeg-guard (ref's Initialisatie branch carries no standing arm). */
    public boolean standingScalar(ResolvedRule r) {
        if (r.kind() != ResolvedRule.Kind.GELIJKSTELLING && r.kind() != ResolvedRule.Kind.BEREKENING)
            return false;
        String k = key(r.target());
        return scalarTargets().contains(k) || desugaredTargets().contains(k);
    }

    /** RUNTIME-SEMANTICS ch. 7.8 (the scalar norm, decided 2026-08-20) — the STANDING SCALAR TARGET domain,
     *  a port of ref's {@code Desugar.standing_targets} over {@code standing_targets_gen ~temporal:false}:
     *  a NON-temporal attribute written by EXACTLY ONE version-free computing rule with NO other writer —
     *  no prioritized-defaults group, no verdeling, no creation-time write. Every scalar value shape
     *  executes, so the domain is not restricted to particular Functie kinds (ref's {@code executable} is
     *  vacuously true off the temporal path), and ref's {@code wanted_shape} demands the ABSENCE of a declared
     *  granulariteit. Kenmerk targets are outside the domain exactly as there. */
    public Set<String> scalarTargets() {
        if (scalar != null) return scalar;
        Map<String, Integer> count = writersPerTarget();
        Set<String> out = new LinkedHashSet<>();
        for (ResolvedRule r : model.resolvedRules) {
            String k = key(r.target());
            if (!isComputing(r) || k == null || count.getOrDefault(k, 0) != 1) continue;
            Expr.AttributeReference d = written(r.target());
            if (temporalAttribute(d.objectType(), member(r.target()))) continue;   // ref's wanted_shape, ~temporal:false
            if (otherWriter(d.objectType().name, member(r.target()))) continue;
            out.add(k);
        }
        scalar = out;
        return out;
    }

    /** RUNTIME-SEMANTICS ch. 7.7 — the v0 DESUGARING domain, a port of ref's
     *  {@code Desugar.desugared_targets} over {@code chosen_targets}: a target carried ENTIRELY by exactly
     *  one unconditional version-free Initialisatie plus AT MOST one version-free computing rule. ref's
     *  scalar arm consults this set BESIDE the standing one (the leeg-evaluating computing rule writes
     *  nothing, so the initialisatie or the input decides and the fire-but-leeg corner has a fixpoint
     *  instead of an oscillation). Unlike the standing domain this one has NO shape and NO other-writer
     *  clause — ref does not test them here either, and its scalar arm is the only consumer, so a temporal
     *  target never reaches it. */
    public Set<String> desugaredTargets() {
        if (desugared != null) return desugared;
        Map<String, List<ResolvedRule>> perTarget = new LinkedHashMap<>();
        for (ResolvedRule r : model.resolvedRules) {
            if (r.kind() == ResolvedRule.Kind.KENMERKTOEKENNING) continue;
            String k = key(r.target());
            if (k != null) perTarget.computeIfAbsent(k, x -> new ArrayList<>()).add(r);
        }
        Set<String> out = new LinkedHashSet<>();
        for (Map.Entry<String, List<ResolvedRule>> e : perTarget.entrySet()) {
            List<ResolvedRule> inits = new ArrayList<>(), comps = new ArrayList<>();
            for (ResolvedRule r : e.getValue()) {
                if (r.kind() == ResolvedRule.Kind.INITIALISATIE) inits.add(r);
                if (isComputing(r)) comps.add(r);
            }
            // ref's `desugarable`: no rule outside the two families (a versioned computing rule falls in
            // neither and breaks the tally), exactly one initialisation, that initialisation PLAIN, at most
            // one computing rule.
            if (e.getValue().size() != inits.size() + comps.size() || inits.size() != 1 || comps.size() > 1) continue;
            ResolvedRule init = inits.get(0);
            boolean bare = init.condition() == null && (init.validity() == null || init.validity().always())
                    && init.value() != null;   // ref's is_plain_init
            if (bare) out.add(e.getKey());
        }
        desugared = out;
        return out;
    }

    /** ref's {@code written_by_rules} count per target key: every rule EXCEPT a Kenmerktoekenning that
     *  writes an objecttype-bound attribuut — an Initialisatie counts as a writer here (which is what keeps
     *  an init-beside-computation target out of the standing domain and inside the ch. 7.7 desugaring's). */
    private Map<String, Integer> writersPerTarget() {
        if (writers != null) return writers;
        Map<String, Integer> count = new LinkedHashMap<>();
        for (ResolvedRule r : model.resolvedRules) {
            if (r.kind() == ResolvedRule.Kind.KENMERKTOEKENNING) continue;
            String k = key(r.target());
            if (k != null) count.merge(k, 1, Integer::sum);
        }
        writers = count;
        return count;
    }

    /** ref's {@code is_computing}: a version-free Gelijkstelling/Berekening carrying a value. The
     *  version-freedom clause is what keeps a VERSIONED target out of every domain — selection is temporal
     *  (ch. 7.4) and never defeasible. */
    private static boolean isComputing(ResolvedRule r) {
        return (r.kind() == ResolvedRule.Kind.GELIJKSTELLING || r.kind() == ResolvedRule.Kind.BEREKENING)
                && (r.validity() == null || r.validity().always()) && r.value() != null;
    }

    /** ref's {@code other_writer}: a prioritized-defaults group, a verdeling or an object-creatie toekenning
     *  writing the same objecttype × attribuut — any of them takes the target out of the standing domain. */
    private boolean otherWriter(String ot, String attr) {
        for (PrioritizedDefaults pd : prioritizedDefaults)
            if (equalName(pd.subject(), ot) && pd.attribute() != null && equalName(pd.attribute(), attr)) return true;
        for (ResolvedAllocation v : model.allocations)
            if (v.targetType() != null && equalName(v.targetType().name, ot)
                    && v.targetAttribute() != null && equalName(v.targetAttribute(), attr)) return true;
        for (ResolvedObjectCreation oc : model.objectCreations)
            if (oc.createdType() != null && equalName(oc.createdType().name, ot))
                for (var t : oc.initialisations())
                    if (!t.characteristic() && t.attribute() != null && equalName(cellMember(t.attribute(), t.labels()), attr))
                        return true;
        return false;
    }

    /** ref's {@code Checks.attr_temporal}: the attribuut carries a declared granulariteit. Delegates to the
     *  SHARED name contract ({@link Namen#containsName}), so a key written in another capitalisation classifies
     *  the same way here as it resolves at the write site. */
    private static boolean temporalAttribute(ObjectType ot, String attr) {
        return ot != null && Namen.containsName(ot.granularityMembers(), attr);
    }

    /** The {@code "<objecttype>|<attribuut>"} key of a rule doel (ref's {@code target_of} + {@code key}), or
     *  null when the doel is not an objecttype-bound attribuutreferentie. A target that names a cell of a dimensioned
     *  attribute keys on the cell, {@code "jutter|vondstgewicht[najaar]"}, so a cell is a target of its own in every
     *  domain here (RUNTIME-SEMANTICS ch. 5.8c), as ref's {@code target_of} keys it. Normalisation is ref's
     *  {@code Checks.norm} mirror, null-safe. */
    public static String key(Expr target) {
        Expr.AttributeReference d = written(target);
        return d != null && d.objectType() != null && d.attribute() != null
                ? Namen.norm(d.objectType().name) + "|" + Namen.norm(member(target)) : null;
    }

    /** The attribute reference a target writes: itself, or the reference under a target that names a cell; null for
     *  any other target. */
    private static Expr.AttributeReference written(Expr target) {
        return target instanceof Expr.DimensionCellRead c ? c.attribute()
                : target instanceof Expr.AttributeReference d ? d : null;
    }

    /** The member a target writes as ref's {@code Checks.cell_member} spells it: the attribute, or the cell its labels
     *  name, {@code vondstgewicht[najaar]}. */
    private static String member(Expr target) {
        Expr.AttributeReference d = written(target);
        return cellMember(d.attribute(), target instanceof Expr.DimensionCellRead c ? c.labels() : null);
    }

    /** ref's {@code Checks.cell_member}: {@code attribute}, or with {@code labels} the cell they name. */
    private static String cellMember(String attribute, List<String> labels) {
        return labels == null ? attribute : attribute + "[" + String.join(", ", labels) + "]";
    }

    private static boolean equalName(String a, String b) {
        return a != null && b != null && Namen.norm(a).equals(Namen.norm(b));
    }
}
