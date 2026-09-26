package dev.uchar.regelspraak.transpiler.kern;

import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.ir.ResolvedObjectCreatie;
import dev.uchar.regelspraak.ir.ResolvedRegel;
import dev.uchar.regelspraak.ir.ResolvedVerdeling;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * THE target classification of RUNTIME-SEMANTICS ch. 7.8 (the standing norm, scalar and timeline arms) and
 * ch. 7.7 (the v0 desugaring domain) — the one derivation BOTH Drools back-ends read. It is a port of ref's
 * {@code Desugar} ({@code standing_targets_gen}, {@code standing_targets}, {@code standing_temporal_targets},
 * {@code desugared_targets}), clause for clause, so a target is standing for the exec-model branch exactly
 * when it is standing for the DRL-text branch, and both exactly when it is standing for ref.
 *
 * <p>WHY it is one class and not two mirrored methods: the two branches carried the arm independently for a
 * month — the exec branch had it, the text branch had nothing — and the corpus said so (four goldens whose
 * DRL replay erased what the norm preserves). A second implementation is a second chance to drift; the
 * DOMAIN is the whole content of the norm, since the norm's one visible act is deciding WHERE a leeg write
 * is suppressed. An implementation that suppressed EVERY leeg write would pass every standing witness and
 * break the v0 multi-writer flank, so the boundary is what has to be shared, not the guard.
 *
 * <p>The sets are computed once per instance and cached; an instance belongs to one transpile of one model.
 */
public final class StaandeDoelen {

    private final RegelSpraakModel model;
    private final List<PrioritizedDefaults> prioritizedDefaults;

    private Set<String> temporeel;
    private Set<String> scalair;
    private Set<String> ontsuikerd;
    private Map<String, Integer> schrijvers;

    private StaandeDoelen(RegelSpraakModel model, List<PrioritizedDefaults> prioritizedDefaults) {
        this.model = model;
        this.prioritizedDefaults = prioritizedDefaults == null ? List.of() : prioritizedDefaults;
    }

    /** The classification of {@code model} with the ch. 7 prioritized-defaults extension beside it (the
     *  {@code other_writer} clause consults it). A flat v0 document passes an empty list. */
    public static StaandeDoelen van(RegelSpraakModel model, List<PrioritizedDefaults> prioritizedDefaults) {
        return new StaandeDoelen(model, prioritizedDefaults);
    }

    /** ch. 7.8, the TIMELINE arm: is {@code doel} a standing temporal target, i.e. does an EMPTY result
     *  decide nothing there, leaving the case-supplied input timeline standing whole? The domain holds only
     *  ATTRIBUTE targets, but the key carries no member kind while WF-002 admits a kenmerk sharing an
     *  attribute's name — a kenmerk write site must therefore NOT ask this (ref structurally cannot apply
     *  the arm to a Kenmerktoekenning). No soort test is needed for the attribute sites: a target written by
     *  an initialisatie BESIDE the computing rule has writer count 2 and is out of the domain already. */
    public boolean staandTemporeel(Expr doel) {
        return temporeleDoelen().contains(sleutel(doel));
    }

    /** ch. 7.8, the SCALAR arm TOGETHER with the ch. 7.7 desugaring domain ref's scalar arm consults beside
     *  it: does a LEEG (null) computed value of {@code r} decide nothing, so that the case input — or, in the
     *  desugaring domain, the initialisatie — stands? Only a COMPUTING rule is in ref's arm: a
     *  KENMERKTOEKENNING must never inherit it (the key carries no member kind, WF-002), and an
     *  INITIALISATIE keeps its own §9.6 leeg-guard (ref's Initialisatie branch carries no standing arm). */
    public boolean staandScalair(ResolvedRegel r) {
        if (r.soort() != ResolvedRegel.Soort.GELIJKSTELLING && r.soort() != ResolvedRegel.Soort.BEREKENING)
            return false;
        String k = sleutel(r.doel());
        return scalaireDoelen().contains(k) || ontsuikerdeDoelen().contains(k);
    }

    /** RUNTIME-SEMANTICS ch. 7.8 (the timeline arm, decided 2026-08-21) — the STANDING TEMPORAL TARGET
     *  domain, a port of ref's {@code Desugar.standing_temporal_targets} over {@code standing_targets_gen}:
     *  a temporal attribute written by EXACTLY ONE version-free computing rule (GELIJKSTELLING/BEREKENING
     *  carrying a value) whose value has the ch. 3.7 pro-rata / ch. 3.8 day-count shape (ref's
     *  {@code executable} — the only temporal writers that execute), with NO other writer: no
     *  prioritized-defaults group, no verdeling, no creation-time write on that attribute. Kenmerk targets
     *  are outside the domain (ref excludes {@code Kenmerktoekenning} from both the writer count and
     *  {@code is_computing}). Keys normalise like ref's {@code norm}, so the two domains coincide construct
     *  for construct. */
    public Set<String> temporeleDoelen() {
        if (temporeel != null) return temporeel;
        Map<String, Integer> tel = schrijversPerDoel();
        Set<String> uit = new LinkedHashSet<>();
        for (ResolvedRegel r : model.resolvedRegels) {
            boolean uitvoerbaar = r.waarde() instanceof Expr.Functie f
                    && (f.kind() == Expr.FunctieKind.TIJDSEVENREDIG_DEEL || f.kind() == Expr.FunctieKind.AANTAL_DAGEN_IN);
            String k = sleutel(r.doel());
            if (!isRekenend(r) || !uitvoerbaar || k == null || tel.getOrDefault(k, 0) != 1) continue;
            Expr.AttribuutRef d = (Expr.AttribuutRef) r.doel();
            if (!temporeelAttribuut(d.objecttype(), d.attribuut())) continue;   // ref's wanted_shape
            if (andereSchrijver(d.objecttype().naam, d.attribuut())) continue;
            uit.add(k);
        }
        temporeel = uit;
        return uit;
    }

    /** RUNTIME-SEMANTICS ch. 7.8 (the scalar norm, decided 2026-08-20) — the STANDING SCALAR TARGET domain,
     *  a port of ref's {@code Desugar.standing_targets} over {@code standing_targets_gen ~temporal:false}:
     *  a NON-temporal attribute written by EXACTLY ONE version-free computing rule with NO other writer —
     *  no prioritized-defaults group, no verdeling, no creation-time write. Two clauses differ from the
     *  temporal twin: ref's {@code executable} is vacuously true off the temporal path (every scalar value
     *  shape executes, so the domain is NOT restricted to particular Functie kinds), and {@code wanted_shape}
     *  demands the ABSENCE of a declared granulariteit. Kenmerk targets are outside the domain exactly as
     *  there. */
    public Set<String> scalaireDoelen() {
        if (scalair != null) return scalair;
        Map<String, Integer> tel = schrijversPerDoel();
        Set<String> uit = new LinkedHashSet<>();
        for (ResolvedRegel r : model.resolvedRegels) {
            String k = sleutel(r.doel());
            if (!isRekenend(r) || k == null || tel.getOrDefault(k, 0) != 1) continue;
            Expr.AttribuutRef d = (Expr.AttribuutRef) r.doel();
            if (temporeelAttribuut(d.objecttype(), d.attribuut())) continue;   // ref's wanted_shape, ~temporal:false
            if (andereSchrijver(d.objecttype().naam, d.attribuut())) continue;
            uit.add(k);
        }
        scalair = uit;
        return uit;
    }

    /** RUNTIME-SEMANTICS ch. 7.7 — the v0 DESUGARING domain, a port of ref's
     *  {@code Desugar.desugared_targets} over {@code chosen_targets}: a target carried ENTIRELY by exactly
     *  one unconditional version-free Initialisatie plus AT MOST one version-free computing rule. ref's
     *  scalar arm consults this set BESIDE the standing one (the leeg-evaluating computing rule writes
     *  nothing, so the initialisatie or the input decides and the fire-but-leeg corner has a fixpoint
     *  instead of an oscillation). Unlike the standing domain this one has NO shape and NO other-writer
     *  clause — ref does not test them here either, and its scalar arm is the only consumer, so a temporal
     *  target never reaches it. */
    public Set<String> ontsuikerdeDoelen() {
        if (ontsuikerd != null) return ontsuikerd;
        Map<String, List<ResolvedRegel>> perDoel = new LinkedHashMap<>();
        for (ResolvedRegel r : model.resolvedRegels) {
            if (r.soort() == ResolvedRegel.Soort.KENMERKTOEKENNING) continue;
            String k = sleutel(r.doel());
            if (k != null) perDoel.computeIfAbsent(k, x -> new ArrayList<>()).add(r);
        }
        Set<String> uit = new LinkedHashSet<>();
        for (Map.Entry<String, List<ResolvedRegel>> e : perDoel.entrySet()) {
            List<ResolvedRegel> inits = new ArrayList<>(), comps = new ArrayList<>();
            for (ResolvedRegel r : e.getValue()) {
                if (r.soort() == ResolvedRegel.Soort.INITIALISATIE) inits.add(r);
                if (isRekenend(r)) comps.add(r);
            }
            // ref's `desugarable`: no rule outside the two families (a versioned computing rule falls in
            // neither and breaks the tally), exactly one initialisation, that initialisation PLAIN, at most
            // one computing rule.
            if (e.getValue().size() != inits.size() + comps.size() || inits.size() != 1 || comps.size() > 1) continue;
            ResolvedRegel init = inits.get(0);
            boolean kaal = init.voorwaarde() == null && (init.geldigheid() == null || init.geldigheid().altijd())
                    && init.waarde() != null;   // ref's is_plain_init
            if (kaal) uit.add(e.getKey());
        }
        ontsuikerd = uit;
        return uit;
    }

    /** ref's {@code written_by_rules} count per target key: every rule EXCEPT a Kenmerktoekenning that
     *  writes an objecttype-bound attribuut — an Initialisatie counts as a writer here (which is what keeps
     *  an init-beside-computation target out of the standing domain and inside the ch. 7.7 desugaring's). */
    private Map<String, Integer> schrijversPerDoel() {
        if (schrijvers != null) return schrijvers;
        Map<String, Integer> tel = new LinkedHashMap<>();
        for (ResolvedRegel r : model.resolvedRegels) {
            if (r.soort() == ResolvedRegel.Soort.KENMERKTOEKENNING) continue;
            String k = sleutel(r.doel());
            if (k != null) tel.merge(k, 1, Integer::sum);
        }
        schrijvers = tel;
        return tel;
    }

    /** ref's {@code is_computing}: a version-free Gelijkstelling/Berekening carrying a value. The
     *  version-freedom clause is what keeps a VERSIONED target out of every domain — selection is temporal
     *  (ch. 7.4) and never defeasible. */
    private static boolean isRekenend(ResolvedRegel r) {
        return (r.soort() == ResolvedRegel.Soort.GELIJKSTELLING || r.soort() == ResolvedRegel.Soort.BEREKENING)
                && (r.geldigheid() == null || r.geldigheid().altijd()) && r.waarde() != null;
    }

    /** ref's {@code other_writer}: a prioritized-defaults group, a verdeling or an object-creatie toekenning
     *  writing the same objecttype × attribuut — any of them takes the target out of the standing domain. */
    private boolean andereSchrijver(String ot, String attr) {
        for (PrioritizedDefaults pd : prioritizedDefaults)
            if (gelijkeNaam(pd.onderwerp(), ot) && pd.attribuut() != null && gelijkeNaam(pd.attribuut(), attr)) return true;
        for (ResolvedVerdeling v : model.verdelingen)
            if (v.doelType() != null && gelijkeNaam(v.doelType().naam, ot)
                    && v.doelAttribuut() != null && gelijkeNaam(v.doelAttribuut(), attr)) return true;
        for (ResolvedObjectCreatie oc : model.objectCreaties)
            if (oc.nieuwType() != null && gelijkeNaam(oc.nieuwType().naam, ot))
                for (var t : oc.toekenningen())
                    if (!t.kenmerk() && gelijkeNaam(t.attribuut(), attr)) return true;
        return false;
    }

    /** ref's {@code Checks.attr_temporal}: the attribuut carries a declared granulariteit. Delegates to the
     *  SHARED name contract ({@link Namen#bevatNaam}), so a key written in another capitalisation classifies
     *  the same way here as it resolves at the write site. */
    private static boolean temporeelAttribuut(Objecttype ot, String attr) {
        return ot != null && Namen.bevatNaam(ot.granulariteitLeden(), attr);
    }

    /** The {@code "<objecttype>|<attribuut>"} key of a rule doel (ref's {@code target_of} + {@code key}), or
     *  null when the doel is not an objecttype-bound attribuutreferentie. Normalisation is ref's
     *  {@code Checks.norm} mirror, null-safe. */
    public static String sleutel(Expr doel) {
        return doel instanceof Expr.AttribuutRef d && d.objecttype() != null && d.attribuut() != null
                ? Namen.norm(d.objecttype().naam) + "|" + Namen.norm(d.attribuut()) : null;
    }

    private static boolean gelijkeNaam(String a, String b) {
        return a != null && b != null && Namen.norm(a).equals(Namen.norm(b));
    }
}
