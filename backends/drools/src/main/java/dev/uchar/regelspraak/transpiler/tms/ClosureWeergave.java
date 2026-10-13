package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.ir.Conditie;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
import dev.uchar.regelspraak.transpiler.tms.plan.Functie;
import dev.uchar.regelspraak.transpiler.tms.plan.Term;
import dev.uchar.regelspraak.transpiler.tms.plan.Toetsitem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The rendering of a planned value or condition ({@link Term}) into a function over the value array of a rule
 * ({@link Waardefunctie}) or a predicate over it, the form in which the consequences and the tests of the executable
 * model compute it. Each node becomes the lambda the
 * value compilers made before the values were planned, with the same calls in the same order, the same guards and the
 * same faults. It decides nothing: the functions, the unit factors, the probes, the faults and the order of evaluation
 * are the plan's ({@code TmsTranspiler}), and a function of the plan is called here by its constant, never chosen. The
 * same holds for the role of a rule's test ({@link Toetsitem.Rol}), which the analysis sets and this class carries out.
 *
 * <p>A {@code Let} is rendered by need, as RUNTIME-SEMANTICS ch. 10.6 evaluates it, whatever {@link Term.Let#gretig}
 * says: a lambda can carry the need, and evaluating by need is right in every case, where evaluating first is right
 * only where the plan marks it so.
 */
final class ClosureWeergave {

    private ClosureWeergave() {}

    /** The function over the value array that computes {@code t}. */
    static Waardefunctie waardefunctie(Term t) {
        return switch (t) {
            case Term.Const c -> {
                Object v = c.waarde();
                yield w -> v;
            }
            case Term.Lees l -> {
                int i = l.slot();
                yield w -> w[i];
            }
            case Term.Binder b -> {
                int i = b.slot();
                yield w -> ((Behoefte) w[i]).lees();
            }
            case Term.Let l -> let(l);
            case Term.Keuze k -> {
                Predicate<Object[]> voorwaarde = toets(k.voorwaarde());
                Waardefunctie dan = waardefunctie(k.dan()), anders = waardefunctie(k.anders());
                yield w -> voorwaarde.test(w) ? dan.van(w) : anders.van(w);
            }
            case Term.Aanroep a -> aanroep(a.functie(), a.argumenten());
            case Term.Herschaal h -> herschaal(h);
            case Term.Deel d -> {
                Waardefunctie l = waardefunctie(d.links()), r = waardefunctie(d.rechts());
                Implementatie f = implementatie(d.functie());
                int sonde = d.sonde();
                yield w -> {
                    Object lv = l.van(w), rv = r.van(w);
                    return f.van(new Object[]{lv, rv, sonde < 0 ? null : w[sonde]});
                };
            }
            case Term.Wortel x -> {
                Waardefunctie radicand = waardefunctie(x.radicand());
                Implementatie f = implementatie(x.functie());
                int sonde = x.sonde();
                yield w -> {
                    Object v = radicand.van(w);
                    return f.van(new Object[]{v, sonde < 0 ? null : w[sonde]});
                };
            }
            case Term.Faal f -> {
                String zin = f.zin();
                yield w -> { throw new Rekenfout(zin); };
            }
            case Term.Vereist v -> {
                Waardefunctie f = waardefunctie(v.waarde());
                String zin = v.zin();
                yield w -> {
                    Object x = f.van(w);
                    if (x == null) throw new Rekenfout(zin);
                    return x;
                };
            }
            case Term.Tekstreeks s -> {
                List<Waardefunctie> delen = s.delen().stream().map(ClosureWeergave::waardefunctie).toList();
                yield w -> {
                    StringBuilder uit = new StringBuilder();
                    for (Waardefunctie deel : delen) uit.append((String) deel.van(w));
                    return uit.toString();
                };
            }
            case Term.GelijsteVouw g -> gelijsteVouw(g);
            case Term.Vouwlezing v -> vouwlezing(v);
            case Term.Samengesteld s -> {
                Predicate<Object[]> p = samengesteld(s);
                yield w -> p.test(w);
            }
            case Term.Kwantorlezing k -> kwantorlezing(k);
            case Term.ZonderFout z -> {
                Predicate<Object[]> p = toets(z);
                yield w -> p.test(w);
            }
            case Term.Dagsoort d -> {
                Predicate<LocalDate> p = d.predikaat();
                yield w -> p;
            }
        };
    }

    /** The test that {@code t}, a condition, holds: a composite as its fold, a condition tested without its fault as
     *  that test, any other term as its value, a truth value. */
    static Predicate<Object[]> toets(Term t) {
        if (t instanceof Term.Samengesteld s) return samengesteld(s);
        if (t instanceof Term.ZonderFout z) return zonderFout(toets(z.voorwaarde()));
        Waardefunctie f = waardefunctie(t);
        return w -> (Boolean) f.van(w);
    }

    /** The test of {@code t}, the condition of a rule's left-hand side, that asks {@code rol} of it
     *  ({@link Toetsitem}). */
    static Predicate<Object[]> toets(Toetsitem t) {
        return toets(t.rol(), toets(t.toets()), t.sonde());
    }

    /** The test of a rule's left-hand side that asks {@code rol} of the condition {@code toets}
     *  ({@link Toetsitem.Rol}); {@code sonde} is the probe's slot of a test of deposits. */
    static Predicate<Object[]> toets(Toetsitem.Rol rol, Predicate<Object[]> toets, int sonde) {
        return switch (rol) {
            case VOORWAARDE, VORM -> toets;
            case ZONDER_FOUT -> zonderFout(toets);
            case FOUT -> w -> fout(toets, w) != null;
            case DEPONERING -> w -> {
                Sonde s = new Sonde();
                w[sonde] = s;
                try {
                    toets.test(w);
                } catch (RuntimeException e) {
                    // a fault is the fault rule's; what the evaluation deposited before it stays heard
                    if (!Rekenfout.isEvaluatiefout(e)) throw e;
                }
                return s.gemeld();
            };
        };
    }

    /** {@code toets} where it answers, and not holding where it faults: the fault is the fault rule's
     *  ({@link Toetsitem.Rol#ZONDER_FOUT}). */
    static Predicate<Object[]> zonderFout(Predicate<Object[]> toets) {
        return w -> {
            try {
                return toets.test(w);
            } catch (RuntimeException e) {
                if (!Rekenfout.isEvaluatiefout(e)) throw e;
                return false;
            }
        };
    }

    /** The evaluation fault {@code toets} raises over {@code w}, or null where it answers. An evaluation fault is a
     *  division by a numeric zero (RUNTIME-SEMANTICS ch. 1.4, point 4) or a {@link Rekenfout}: a date step or a duration
     *  count outside the range of ch. 3.8, a context value or a validity window without an evaluation date, an
     *  {@code Unresolved} node reached, and the other faults that class names. Any other exception is a broken contract
     *  or a bug and leaves the fire as it is. */
    static RuntimeException fout(Predicate<Object[]> toets, Object[] w) {
        try {
            toets.test(w);
            return null;
        } catch (RuntimeException e) {
            if (!Rekenfout.isEvaluatiefout(e)) throw e;
            return e;
        }
    }

    /** The function {@code functie} on the values of {@code argumenten}, evaluated in order. */
    private static Waardefunctie aanroep(Functie functie, List<Term> argumenten) {
        Implementatie f = implementatie(functie);
        Waardefunctie[] argument = new Waardefunctie[argumenten.size()];
        for (int i = 0; i < argument.length; i++) argument[i] = waardefunctie(argumenten.get(i));
        return w -> {
            Object[] x = new Object[argument.length];
            for (int i = 0; i < x.length; i++) x[i] = argument[i].van(w);
            return f.van(x);
        };
    }

    /** A restatement through the function the plan names, with the factors it takes: the numerator and the denominator
     *  of {@link Functie#HERSCHAAL_EXACT}, or the one ratio of {@link Functie#HERSCHAAL}, whose plan has no
     *  denominator ({@link Term.Herschaal}). */
    private static Waardefunctie herschaal(Term.Herschaal h) {
        Waardefunctie f = waardefunctie(h.term());
        Implementatie g = implementatie(h.functie());
        BigDecimal teller = h.teller(), noemer = h.noemer();
        if (noemer != null) return w -> g.van(new Object[]{f.van(w), teller, noemer});
        return w -> g.van(new Object[]{f.van(w), teller});
    }

    /**
     * A {@code Let} (RUNTIME-SEMANTICS ch. 10.6): the body evaluated with the binder bound to its value as a need. The
     * value is evaluated in the enclosing scope (WF-076), at the first read of the binder, and that one result serves
     * every further read (clause 1); a read whose value is leeg ends the {@code Let} with leeg (clause 2), the barrier
     * belonging to the {@code Let} whose binder was read; a body that never reads the binder evaluates nothing of the
     * value, so no leeg, no fault and no deposit come of it (clause 3). The binder is a slot of the value array that no
     * pattern binds: the {@code Let} hands its body a copy of the array with the need in that slot, so the lambda keeps
     * no state and one KieBase serves any number of sessions.
     */
    private static Waardefunctie let(Term.Let l) {
        Waardefunctie waarde = waardefunctie(l.waarde()), lichaam = waardefunctie(l.lichaam());
        int slot = l.slot();
        return w -> {
            Barriere barriere = new Barriere();
            Object[] binnen = w.clone();
            binnen[slot] = new Behoefte(() -> waarde.van(w), barriere);
            try {
                return lichaam.van(binnen);
            } catch (Barriere b) {
                if (b != barriere) throw b;
                return null;
            }
        };
    }

    /** A composite ({@link Term.Samengesteld}): its members read in order, the stop triggers of RUNTIME-SEMANTICS
     *  ch. 5.10 tested before each, and the quantifier's verdict where none stops it, so a member after the stop is not
     *  evaluated. */
    private static Predicate<Object[]> samengesteld(Term.Samengesteld s) {
        List<Predicate<Object[]>> leden = s.leden().stream().map(ClosureWeergave::toets).toList();
        Conditie.Kwantor kw = s.kwantor();
        int drempel = s.drempel();
        return w -> {
            int raak = 0;
            for (int i = 0; i < leden.size(); i++) {
                Boolean vast = Rekenhulpen.vastgesteld(kw, drempel, raak, i, leden.size() - i);
                if (vast != null) return vast;
                if (leden.get(i).test(w)) raak++;
            }
            return Rekenhulpen.eindoordeel(kw, drempel, raak, leden.size());
        };
    }

    /** The verdict of a quantifier fold whose matrix can fault or deposit ({@link Term.Kwantorlezing}): the count and
     *  the outcomes evaluated in that order, then the plan's function over them with the quantifier, the threshold and
     *  the probe in the node's slot, or nothing where the node has none. */
    private static Waardefunctie kwantorlezing(Term.Kwantorlezing k) {
        Waardefunctie aantal = waardefunctie(k.aantal()), bijdragen = waardefunctie(k.bijdragen());
        Implementatie f = implementatie(k.functie());
        Conditie.Kwantor kw = k.kwantor();
        int drempel = k.drempel(), sonde = k.sonde();
        return w -> {
            Object n = aantal.van(w), b = bijdragen.van(w);
            return f.van(new Object[]{n, b, kw, drempel, sonde < 0 ? null : w[sonde]});
        };
    }

    /** A listed fold: each operand taken by the plan's step as it is evaluated, so the order of evaluation and
     *  comparison is the value compiler's ({@link Term.GelijsteVouw}). */
    private static Waardefunctie gelijsteVouw(Term.GelijsteVouw g) {
        Implementatie stap = implementatie(g.functie());
        List<Waardefunctie> operanden = g.operanden().stream().map(ClosureWeergave::waardefunctie).toList();
        return w -> {
            Object acc = null;
            for (Waardefunctie op : operanden) acc = stap.van(new Object[]{acc, op.van(w)});
            return acc;
        };
    }

    /** The evaluation of a fold in the rule that reads it ({@link Term.Vouwlezing}): its filter's deposit heard by this
     *  rule's probe, then the first filter fault, then the first member fault, then the fold's value. */
    private static Waardefunctie vouwlezing(Term.Vouwlezing v) {
        Waardefunctie uitkomst = waardefunctie(v.uitkomst());
        int deponeringen = v.deponeringen(), sonde = v.sonde(), filterfout = v.filterfout(), lidfout = v.lidfout();
        return w -> {
            if (sonde >= 0 && ((Number) w[deponeringen]).longValue() > 0) Sonde.meld(w, sonde);
            if (filterfout >= 0 && w[filterfout] != null) throw ((Filterfout) w[filterfout]).fout();
            // the reference interpreter reads the members' values once every member's filter is evaluated
            if (lidfout >= 0 && w[lidfout] != null) throw ((Lidfout) w[lidfout]).fout();
            return uitkomst.van(w);
        };
    }

    /** A function of the plan as a call on evaluated arguments. */
    @FunctionalInterface
    private interface Implementatie {
        Object van(Object[] x);
    }

    /** The call of {@code functie}: the method its constant names, with the arguments cast as its parameters take
     *  them. */
    @SuppressWarnings("unchecked")
    private static Implementatie implementatie(Functie functie) {
        return switch (functie) {
            case RS_PLUS -> x -> RegelSpraakRuntime.rsPlus(x[0], x[1]);
            case RS_MIN -> x -> RegelSpraakRuntime.rsMin(x[0], x[1]);
            case RS_VERMINDERD -> x -> RegelSpraakRuntime.rsVerminderd(x[0], x[1]);
            case RS_MAAL -> x -> RegelSpraakRuntime.rsMaal(x[0], x[1]);
            case RS_PERCENTAGE_VAN -> x -> RegelSpraakRuntime.rsPercentageVan(x[0], x[1]);
            case RS_MACHT -> x -> RegelSpraakRuntime.rsMacht(x[0], (Integer) x[1]);
            case RS_AFRONDING -> x -> RegelSpraakRuntime.rsAfronding(x[0], (Integer) x[1], (RoundingMode) x[2]);
            case RS_BEGRENS -> x -> RegelSpraakRuntime.rsBegrens(x[0], x[1], x[2]);
            case RS_ABS -> x -> RegelSpraakRuntime.rsAbs(x[0]);
            case RS_TEKST -> x -> RegelSpraakRuntime.rsTekst(x[0]);
            case RS_TIJDSDUUR -> x -> RegelSpraakRuntime.rsTijdsduur((Temporal) x[0], (Temporal) x[1], (String) x[2],
                    (Boolean) x[3]);
            case RS_TIJDSDUUR_DAGSOORT -> x -> RegelSpraakRuntime.rsTijdsduurDagsoort((LocalDate) x[0],
                    (LocalDate) x[1], (Predicate<LocalDate>) x[2], (Boolean) x[3]);
            case RS_DATUM_DEEL -> x -> RegelSpraakRuntime.rsDatumDeel(x[0], (ChronoField) x[1]);
            case RS_REKENJAAR -> x -> RegelSpraakRuntime.rsRekenjaar((LocalDate) x[0]);
            case RS_DIM_SOM -> x -> RegelSpraakRuntime.rsDimSom((Map<?, ?>) x[0], (List<?>) x[1]);
            case RS_DIM_MIN -> x -> RegelSpraakRuntime.rsDimMin((Map<?, ?>) x[0], (List<?>) x[1]);
            case RS_DIM_MAX -> x -> RegelSpraakRuntime.rsDimMax((Map<?, ?>) x[0], (List<?>) x[1]);
            case RS_TIJD_TOTAAL -> x -> RegelSpraakRuntime.rsTijdTotaal((List<?>) x[0], (String) x[1]);
            case RS_TIJD_KNIP -> x -> RegelSpraakRuntime.rsTijdKnip((List<?>) x[0], (List<?>) x[1], (Class<?>) x[2]);
            case RS_TIJD_KNIP_NIET -> x -> RegelSpraakRuntime.rsTijdKnipNiet((List<?>) x[0], (List<?>) x[1],
                    (Class<?>) x[2]);
            case RS_TIJD_CONDITIE -> x -> RegelSpraakRuntime.rsTijdConditie((List<?>) x[0], (Integer) x[1],
                    (BigDecimal) x[2], (Class<?>) x[3]);
            case RS_TIJDSEVENREDIG -> x -> RegelSpraakRuntime.rsTijdsevenredig((List<?>) x[0], (List<?>) x[1],
                    (String) x[2], (Class<?>) x[3]);
            case RS_AANTAL_DAGEN_IN -> x -> RegelSpraakRuntime.rsAantalDagenIn((List<?>) x[0], (String) x[1],
                    (Class<?>) x[2]);
            case RS_AANTAL_DAGEN_IN_VANAF -> x -> RegelSpraakRuntime.rsAantalDagenInVanaf((List<?>) x[0],
                    (String) x[1], (LocalDate) x[2], (Class<?>) x[3]);
            case TIJDVAK_LIJST -> x -> Tijdvak.lijst((List<?>) x[0]);
            case GEDEELD -> x -> Rekenhulpen.gedeeld(x[0], x[1], x[2]);
            case GEDEELD_ABS -> x -> Rekenhulpen.gedeeldAbs(x[0], x[1], x[2]);
            case WORTEL -> x -> Rekenhulpen.wortel(x[0], x[1]);
            case EXACTE_WORTEL -> x -> Rekenhulpen.exacteWortel(x[0], x[1]);
            case DATUMSTAP -> x -> Rekenhulpen.datumstap(x[0], x[1], (String) x[2], (Boolean) x[3]);
            case DAGSOORTSTAP -> x -> Rekenhulpen.dagsoortstap(x[0], x[1], (Predicate<LocalDate>) x[2],
                    (Boolean) x[3]);
            case DATUMTIJD_DAGSOORTSTAP -> x -> Rekenhulpen.datumtijdDagsoortstap(x[0], x[1],
                    (Predicate<LocalDate>) x[2], (Boolean) x[3]);
            case PAASDAG -> x -> Rekenhulpen.paasdag(x[0]);
            case DATUM_MET -> x -> Rekenhulpen.datumMet((String) x[0], x[1], x[2], x[3]);
            case DATUMTIJD_MET -> x -> Rekenhulpen.datumtijdMet((String) x[0], x[1], x[2], x[3], x[4], x[5], x[6],
                    x[7]);
            case CEL -> x -> Rekenhulpen.cel(x[0], (String) x[1]);
            case KAART -> x -> Rekenhulpen.kaart(x[0]);
            case HERSCHAAL_EXACT -> x -> Rekenhulpen.herschaalExact(x[0], (BigDecimal) x[1], (BigDecimal) x[2]);
            case HERSCHAAL -> x -> Rekenhulpen.herschaal(x[0], (BigDecimal) x[1]);
            case KLEINSTE -> x -> Rekenhulpen.kleinste(x[0], x[1]);
            case GROOTSTE -> x -> Rekenhulpen.grootste(x[0], x[1]);
            case TERUGVAL -> x -> Rekenhulpen.terugval(x[0], x[1]);
            case AANTAL -> x -> Rekenhulpen.aantal(x[0]);
            case VOUWWAARDE -> x -> Rekenhulpen.vouwwaarde(x[0], x[1], x[2]);
            case EIGEN_TOTAAL -> x -> Rekenhulpen.eigenTotaal(x[0], x[1], x[2], (String) x[3], (String) x[4]);
            case VOLLEDIG -> x -> Rekenhulpen.volledig(x[0], (String) x[1], x[2], (String) x[3]);
            case COLLAPSE -> x -> Rekenhulpen.collapse(x[0]);
            case RS_AGG_VERGELIJK -> x -> RegelSpraakRuntime.rsAggVergelijk(x[0], x[1], (String) x[2]);
            case DATUM_VERGELIJK -> x -> Rekenhulpen.datumVergelijk(x[0], x[1], (Conditie.Operator) x[2]);
            case TEKST_GELIJK -> x -> Rekenhulpen.tekstGelijk(x[0], x[1], (Boolean) x[2]);
            case RS_ENUM_NAAM -> x -> RegelSpraakRuntime.rsEnumNaam(x[0]);
            case LEEGTOETS -> x -> Rekenhulpen.leegtoets(x[0], (Boolean) x[1]);
            case ELFPROEF -> x -> Rekenhulpen.elfproef(x[0], (Boolean) x[1]);
            case NUMERIEK_MET_EXACT -> x -> Rekenhulpen.numeriekMetExact(x[0], (Integer) x[1], (Boolean) x[2]);
            case KENMERK -> x -> Rekenhulpen.kenmerk(x[0], (Boolean) x[1]);
            case KENMERK_LANGS_PAD -> x -> Rekenhulpen.kenmerkLangsPad(x[0], (Boolean) x[1]);
            case ROLAANTAL -> x -> Rekenhulpen.rolaantal(x[0], (Boolean) x[1]);
            case ROLVELD -> x -> Rekenhulpen.rolveld(x[0], (Boolean) x[1]);
            case TIJDVENSTER -> x -> Rekenhulpen.tijdvenster(x[0], (Boolean) x[1], x[2], (Boolean) x[3],
                    (Boolean) x[4], x[5]);
            case GEHEEL_PERIODE -> x -> Rekenhulpen.geheelPeriode(x[0], x[1], (String) x[2], (Integer) x[3],
                    (Boolean) x[4]);
            case KWANTOROORDEEL -> x -> Rekenhulpen.kwantoroordeel(x[0], x[1], (Conditie.Kwantor) x[2],
                    (Integer) x[3]);
            case KWANTORLEZING -> x -> Rekenhulpen.kwantorlezing(x[0], x[1], (Conditie.Kwantor) x[2], (Integer) x[3],
                    x[4]);
            case KANAAL -> x -> Rekenhulpen.kanaal(x[0]);
            case NIET -> x -> Rekenhulpen.niet(x[0]);
            case EMMERSCALAIR -> x -> Rekenhulpen.emmerscalair(x[0], (Boolean) x[1]);
            case DOEL_LEEG -> x -> Rekenhulpen.doelLeeg(x[0], x[1], (Class<?>) x[2], (String) x[3]);
            case VERSCHILLEN -> x -> Rekenhulpen.verschillen(x[0]);
        };
    }

    /** The barrier of one evaluation of a {@code Let} (RUNTIME-SEMANTICS ch. 10.6, clause 2): raised by a read of its
     *  binder whose value is leeg, and caught by that evaluation alone, since every evaluation makes its own, so a
     *  nested {@code Let} lets an enclosing one's barrier pass. A signal, not a diagnosis: no stack trace. */
    private static final class Barriere extends RuntimeException {
        private static final long serialVersionUID = 1L;

        Barriere() {
            super(null, null, false, false);
        }
    }

    /** The need of a binder in one evaluation of its {@code Let} (RUNTIME-SEMANTICS ch. 10.6, clause 1): the value,
     *  evaluated at the first read and kept for every further one; a leeg value raises the {@code Let}'s barrier at each
     *  read. Made inside that evaluation and reached only through its copy of the value array, so it is never shared
     *  between sessions or threads. */
    private static final class Behoefte {
        private final Supplier<Object> waarde;
        private final Barriere barriere;
        private boolean berekend;
        private Object uitkomst;

        Behoefte(Supplier<Object> waarde, Barriere barriere) {
            this.waarde = waarde;
            this.barriere = barriere;
        }

        Object lees() {
            if (!berekend) {
                uitkomst = waarde.get();
                berekend = true;
            }
            if (uitkomst == null) throw barriere;
            return uitkomst;
        }
    }
}
