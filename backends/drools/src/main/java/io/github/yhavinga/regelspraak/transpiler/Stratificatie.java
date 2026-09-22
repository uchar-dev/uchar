package io.github.yhavinga.regelspraak.transpiler;

import io.github.yhavinga.regelspraak.ir.Conditie;
import io.github.yhavinga.regelspraak.ir.Expr;
import io.github.yhavinga.regelspraak.ir.ResolvedRegel;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Objecttype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** §9.2 negation-stratification, DERIVED from the declared dependency graph instead of the one-level
 *  syntactic heuristic (the uchar spec's WF-046 strata principle: along a positive edge the consumer's
 *  stratum is at least the producer's, along a negative edge strictly greater).
 *
 *  <p>A §13.4.5 kenmerktoekenning is MONOTONIC (a kenmerk is default-onwaar and only ever set WAAR), so a
 *  rule reading "niet &lt;kenmerk&gt;" must fire at the FIXPOINT, after every producer of that kenmerk. The
 *  former heuristic sank every negation-consumer to salience -1 — one level. That is exactly right for a
 *  single negation layer, but a CHAIN ("X is k3 indien X niet k2 is" over "X is k2 indien X niet k1 is")
 *  put producer and consumer in the SAME -1 band, where source order decides and a consumer that fires
 *  early writes a once-true kenmerk that persists — the same transient-window bug one level down
 *  (StratificatieKetenTest, the red case).
 *
 *  <p>The derivation: over the kenmerk graph — keyed by (objecttype, kenmerk), matching the resolver's
 *  type-qualified graph, so a kenmerk homonym on an unrelated objecttype adds no edge; only a NAVIGATED
 *  read whose owning type is not resolved falls back to a name-wide (wildcard) producer match — a rule's
 *  DEPTH is the maximum over its kenmerk reads of the producing rules' depth, plus one when the read is
 *  negated. Positive reads pass depth through without sinking the reader by themselves (a positive reader
 *  that fired too early re-fires on the producer's modify — kenmerken are monotone, so a missed positive
 *  is recoverable; a consumed negation is not); a negation-consumer that ALSO positively reads a derived
 *  kenmerk sinks below that producer too — a deliberate, safe over-approximation (deeper only delays, it
 *  never re-opens the transient window). The salience is {@code -depth} for a rule that itself carries a
 *  negated kenmerk read, {@code 0} otherwise — identical to the heuristic everywhere except chains and the
 *  over-approximation cases above.
 *
 *  <p>A negation CYCLE has no strata. The fallback is CYCLE-LOCAL: only the rules whose depth still moved
 *  in the final sweep — and every rule that transitively reads through them — degrade to the one-level
 *  heuristic (-1 for a direct negation reader); converged chains elsewhere in the model keep their derived
 *  strata. NB the resolver's read-write-cycle warning walks assignment VALUES only, not voorwaarden, so a
 *  pure negation-condition cycle carries no resolver diagnostic — the local fallback is the only guard.
 *
 *  <p>Deliberately out of scope, as before: negated tests over DERIVED non-kenmerk state (a leeg-check on a
 *  computed attribute) — the heuristic never covered those either, and they await the spec's full
 *  dependency-graph treatment. */
public final class Stratificatie {

    private final Map<String, Integer> saliencePerRegel = new HashMap<>();

    private Stratificatie() {}

    public static Stratificatie bereken(List<ResolvedRegel> regels) {
        Stratificatie s = new Stratificatie();

        // producers, type-qualified: (objecttype|kenmerk) -> producing rule indexes; plus the name-wide
        // index for the wildcard fallback of an unresolved navigated read
        Map<String, List<Integer>> producenten = new HashMap<>();
        Map<String, List<Integer>> producentenOpNaam = new HashMap<>();
        for (int i = 0; i < regels.size(); i++) {
            ResolvedRegel r = regels.get(i);
            if (r.soort() == ResolvedRegel.Soort.KENMERKTOEKENNING
                    && r.doel() instanceof Expr.AttribuutRef doel && doel.attribuut() != null) {
                producenten.computeIfAbsent(key(doel.objecttype(), doel.attribuut()), k -> new ArrayList<>()).add(i);
                producentenOpNaam.computeIfAbsent(naamKey(doel.attribuut()), k -> new ArrayList<>()).add(i);
            }
        }

        // reads: per rule the kenmerk reads, with the owning type (the read's eigenaar when resolved, else
        // the rule's own subject type for an un-navigated read, else null = wildcard) and negation flag
        List<List<Read>> reads = new ArrayList<>();
        for (ResolvedRegel r : regels) {
            Objecttype subject =
                    r.doel() instanceof Expr.AttribuutRef doel ? doel.objecttype() : null;
            List<Read> uit = new ArrayList<>();
            verzamelReads(r.voorwaarde(), subject, false, uit);
            reads.add(uit);
        }

        // depth fixpoint: depth(r) = max over reads (k, neg): maxProducerDepth(k) + (neg ? 1 : 0).
        // Convergence needs at most one sweep per chain level; regels.size() sweeps bound any acyclic graph.
        int[] depth = new int[regels.size()];
        boolean[] bewoog = new boolean[regels.size()];
        boolean stabiel = false;
        for (int iter = 0; iter <= regels.size() && !stabiel; iter++) {
            stabiel = true;
            java.util.Arrays.fill(bewoog, false);
            for (int i = 0; i < regels.size(); i++) {
                int d = 0;
                for (Read read : reads.get(i)) {
                    int p = 0;
                    for (int j : read.producentenVan(producenten, producentenOpNaam)) p = Math.max(p, depth[j]);
                    d = Math.max(d, p + (read.negatie ? 1 : 0));
                }
                if (d != depth[i]) { depth[i] = d; bewoog[i] = true; stabiel = false; }
            }
        }

        // cycle-local contamination: a rule still moving in the final sweep sits on (or under) a negation
        // cycle; every rule reading through a contaminated producer is contaminated too
        boolean[] besmet = bewoog.clone();
        boolean groei = !stabiel;
        while (groei) {
            groei = false;
            for (int i = 0; i < regels.size(); i++) {
                if (besmet[i]) continue;
                for (Read read : reads.get(i)) {
                    for (int j : read.producentenVan(producenten, producentenOpNaam)) {
                        if (besmet[j]) { besmet[i] = true; groei = true; break; }
                    }
                    if (besmet[i]) break;
                }
            }
        }

        for (int i = 0; i < regels.size(); i++) {
            ResolvedRegel r = regels.get(i);
            boolean negLezer = reads.get(i).stream().anyMatch(read -> read.negatie);
            int salience;
            if (!negLezer) salience = 0;
            else if (!stabiel && besmet[i]) salience = -1;   // on/under a negation cycle: no strata — the
                                                             // one-level heuristic, locally
            else salience = -depth[i];
            s.saliencePerRegel.put(r.naam(), salience);
        }
        return s;
    }

    /** The derived agenda salience for {@code r}: 0, or negative for a negation-consumer (its stratum). */
    public int salience(ResolvedRegel r) {
        return saliencePerRegel.getOrDefault(r.naam(), 0);
    }

    private record Read(String typeKey, String kenmerk, boolean negatie) {
        /** The producer indexes this read can see: type-qualified when the owning type resolved, else the
         *  name-wide wildcard (an unresolved navigated read — over-approximate, never under). */
        List<Integer> producentenVan(Map<String, List<Integer>> perType, Map<String, List<Integer>> opNaam) {
            return typeKey != null
                    ? perType.getOrDefault(typeKey + "|" + kenmerk, List.of())
                    : opNaam.getOrDefault(kenmerk, List.of());
        }
    }

    /** {@code neg} is the NEGATION CONTEXT the walk has entered, carried exactly as the verifier's
     *  {@code dep_cond ~neg} carries it (RUNTIME-SEMANTICS ch. 6.1): once negative, conservatively
     *  negative — a second negation does not cancel the first. */
    private static void verzamelReads(Conditie c, Objecttype subject, boolean neg, List<Read> uit) {
        if (c == null) return;
        if (c instanceof Conditie.Kenmerk k && k.kenmerk() != null) {
            Objecttype eigenaar = k.eigenaar() != null ? k.eigenaar()
                    : k.navigatie() == null ? subject : null;
            uit.add(new Read(eigenaar != null ? typeKey(eigenaar) : null,
                    naamKey(k.kenmerk()), neg || k.negatie()));
        }
        // A compound with GEEN / TEN_HOOGSTE / PRECIES is NEGATION-LIKE (RUNTIME-SEMANTICS ch. 6.1, and
        // the verifier's dep_cond implements it in those words): the verdict turns on members that do
        // NOT hold, so a characteristic read beneath one decides the way a negated read does. Until
        // 2026-09-22 this walk read the polarity out of Kenmerk.negatie() alone and entered a compound
        // with no context, so "aan GEEN van de volgende voorwaarden voldoen: gemerkt zijn" — the same
        // sentence as "niet gemerkt", differently spelled — produced a POSITIVE read and left the
        // consumer in band 0. Measured on a GEEN-rewrite of jutterswet-uniestratum: ref and the ASP
        // backend PASS, both Drools branches `verwacht afwezig, werkelijk aanwezig`. ALLE / DE /
        // TEN_MINSTE pass the context THROUGH rather than flipping it — they are conjunctive, so a
        // positive read under one stays positive and no rule sinks for nothing.
        if (c instanceof Conditie.Samengesteld sam) {
            boolean neg2 = switch (sam.kwantor()) {
                case GEEN, TEN_HOOGSTE, PRECIES -> true;
                case DE, ALLE, TEN_MINSTE -> neg;
            };
            for (Conditie d : sam.onderdelen()) verzamelReads(d, subject, neg2, uit);
        }
        // IR-SPEC ch. 4.5: a quantifier fold's filter and matrix are conditions too, and a NEGATED kenmerk read
        // inside one has to sink this rule's stratum exactly as a top-level one does — an unseen read would let the
        // consumer fire in the transient window before its producer ran. Both children are evaluated with the
        // ELEMENT as subject (RUNTIME-SEMANTICS ch. 3.9a), so the recursion hands down the element type, never the
        // rule's own subject: attributing an element kenmerk to the rule subject would key the read to the wrong type.
        // NAMED FLANK, measured rather than assumed (2026-09-22). The verifier reads everything under a
        // quantifier fold's MATRIX conservatively NEGATIVELY (dep_cond: "the quantifier is a fold with
        // conjunction, so a member that fails decides the verdict the way a negated read does"). This
        // walk only PASSES THE CONTEXT THROUGH, so an outer negation now reaches the fold where it did
        // not before, but a positive read inside one still counts as positive. Mirroring the verifier
        // here is a THIRD behaviour change beyond the two this round repairs, and it is not free:
        // counted over the 730 documents of the four zones, 13 characteristic reads sit under a
        // quantifier fold in a voorwaarde and 12 of them are spelled positively, so the mirror would
        // sink those consumers a band and move gate-12 channels. It belongs to the quantifier fold's
        // own card with its own measurement, not to this one; recorded here so it is a decision
        // someone took rather than a difference nobody saw.
        if (c instanceof Conditie.Kwantorvouw kv) {
            verzamelReads(kv.filter(), kv.elementObjecttype(), neg, uit);
            verzamelReads(kv.toets(), kv.elementObjecttype(), neg, uit);
        }
        // A condition's EXPRESSION operands carry conditions of their own — an aggregation's die/dat
        // filter (IR-SPEC ch. 4.3) and, since 2026-09-20, a §5.7 union's per-selection filters (WF-110) —
        // and a NEGATED kenmerk read inside one has to sink this rule's stratum exactly as a top-level one
        // does. Until 2026-09-22 this walk descended into nested CONDITIONS only, so every such read was
        // invisible: measured on a two-rule document (a rule deriving `gemerkt` on the element, a rule
        // granting the subject a monotone kenmerk when the count of NOT-gemerkt elements exceeds zero),
        // ref and the ASP backend saw the edge and left the consumer unfired while BOTH Drools branches
        // fired it in the transient window and never took the grant back — `verwacht afwezig, werkelijk
        // aanwezig`, on a verifier-clean document. The gap was NOT the union's: the ORDINARY
        // single-source fold with the same filter diverged identically and had done so since the filter
        // landed. One walk closes both.
        if (c instanceof Conditie.Vergelijking v) {
            readsInExpr(v.links(), subject, neg, uit); readsInExpr(v.rechts(), subject, neg, uit);
        }
        if (c instanceof Conditie.Predicaat p) readsInExpr(p.operand(), subject, neg, uit);
        if (c instanceof Conditie.Periode pe) {
            readsInExpr(pe.vanaf(), subject, neg, uit); readsInExpr(pe.tot(), subject, neg, uit);
        }
    }

    /** The kenmerk reads a condition's EXPRESSION operand hides — a fold filter, a union selection's
     *  filter — each attributed to the type it is READ AT (the contributor, never the rule's subject:
     *  RUNTIME-SEMANTICS ch. 3.4a for the fold, ch. 3.4d for the union), exactly as the quantifier fold's
     *  recursion above hands down its element type.
     *
     *  <p>EXHAUSTIVE over the sealed union, no {@code default} — the reason {@code Expr.bevat} is written
     *  the same way: a missing arm here under-reports ("this rule reads no kenmerk"), and an
     *  under-reported read is a rule that fires one stratum too early, which is the defect this method
     *  exists to prevent. */
    private static void readsInExpr(Expr e, Objecttype subject, boolean neg, List<Read> uit) {
        if (e == null) return;
        switch (e) {
            case Expr.Functie f -> {
                for (Expr a : f.argumenten()) readsInExpr(a, subject, neg, uit);
                readsInExpr(f.leegwaarde(), subject, neg, uit);
                // the filter is read AT THE CONTRIBUTOR when the fold names one, and otherwise at the
                // ENCLOSING subject — the verifier's rule verbatim (`fsubject = elementObjecttype else
                // subject`). Handing null down instead means the name-wide producer match, which
                // Stratificatie reserves for an unresolved NAVIGATED read alone: measured 2026-09-22, a
                // filter-carrying fold with no element type drew an edge to a same-named characteristic
                // produced on an UNRELATED object type and sank the consumer to −2 where −1 is
                // normative. A false edge is not only a wrong band — it can close a false CYCLE and trip
                // the cycle-local fallback.
                verzamelReads(f.filter(), f.elementObjecttype() != null ? f.elementObjecttype() : subject,
                        neg, uit);
            }
            case Expr.InstanceSetUnion u -> {
                for (Expr.InstanceSetUnion.Tak t : u.takken())
                    verzamelReads(t.filter(), t.elementType() != null ? t.elementType() : subject, neg, uit);
            }
            case Expr.Binair b -> {
                readsInExpr(b.links(), subject, neg, uit); readsInExpr(b.rechts(), subject, neg, uit);
            }
            case Expr.Afgerond a -> readsInExpr(a.basis(), subject, neg, uit);
            case Expr.Begrensd b -> {
                readsInExpr(b.basis(), subject, neg, uit); readsInExpr(b.minimum(), subject, neg, uit);
                readsInExpr(b.maximum(), subject, neg, uit);
            }
            case Expr.TekstReeks t -> {
                for (Expr.TekstReeks.Deel d : t.delen())
                    if (d instanceof Expr.TekstReeks.Ingevoegd i) readsInExpr(i.expr(), subject, neg, uit);
            }
            case Expr.DimensieCel c -> readsInExpr(c.attribuut(), subject, neg, uit);
            case Expr.Let l -> {
                readsInExpr(l.value(), subject, neg, uit); readsInExpr(l.body(), subject, neg, uit);
            }
            case Expr.IfThenElse k -> {
                verzamelReads(k.conditie(), subject, neg, uit);
                readsInExpr(k.dan(), subject, neg, uit); readsInExpr(k.anders(), subject, neg, uit);
            }
            case Expr.Literal ignored -> { }
            case Expr.AttribuutRef ignored -> { }
            case Expr.ParameterRef ignored -> { }
            case Expr.VariabeleRef ignored -> { }
            case Expr.BinderRef ignored -> { }
            case Expr.Constante ignored -> { }
            case Expr.Onopgelost ignored -> { }
        }
    }

    private static String key(Objecttype ot, String kenmerk) {
        return (ot != null ? typeKey(ot) : "?") + "|" + naamKey(kenmerk);
    }

    private static String typeKey(Objecttype ot) {
        return ot.naam == null ? "?" : ot.naam.toLowerCase(Locale.ROOT);
    }

    private static String naamKey(String kenmerk) {
        return kenmerk.toLowerCase(Locale.ROOT);
    }
}
