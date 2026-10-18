package dev.uchar.regelspraak.transpiler.kern;

import dev.uchar.regelspraak.ir.Conditie;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.Geldigheid;
import dev.uchar.regelspraak.model.Parameterset;
import dev.uchar.regelspraak.model.Parameterset.WaardeInterval;
import dev.uchar.regelspraak.model.RegelSpraakModel;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Compile-time consistentiecheck <b>regel-geldigheid × parameter-geldigheid</b> (card fase 3, gate G3). Een regel die
 * geldig is in een periode waarin een gerefereerde parameter GEEN geldige waarde heeft, wordt per-periode gerapporteerd
 * als WARNING — niet als ERROR: de deployment mag die periode legitiem nooit serveren (dan levert de
 * peildatum-gate/sessie-gap-fout dat af), maar het gat moet ZICHTBAAR zijn op /info en in de CLI-samenvatting (fase 4/5).
 *
 * <p><b>Scoping (gepind).</b> Alleen parameters die IN de geladen sets voorkomen worden gecheckt. Een gerefereerde
 * parameter die helemaal niet in de sets zit is NIET dit kanaal: die wordt door het degenererende
 * setGlobal-/descriptor-default-pad geleverd en door de parametergate ({@code ScenarioResolver.parameterGate}) en de
 * descriptor-validatie bewaakt. Zijn er geen sets geladen (of geen parameter uit een regel in de pool), dan draait de
 * check dus vacuüm leeg — geen enkele melding.
 *
 * <p><b>Constructielijst.</b> {@link #controleer} spiegelt EXACT de constructielijst en de {@code Expr}-walk van
 * {@code ScenarioResolver.regelParameters} (zie {@code resolver/ScenarioResolver.java}, ~regel 490): per resolved
 * construct de {@code ParameterRef}-namen via {@link Expr#bevat} over de waarde/doel-{@code Expr}s en via
 * {@link Conditie#bevatOperand} over de voorwaarde/assertie. {@code regelParameters} loopt precies TWEE
 * resolved-collecties af — {@link RegelSpraakModel#resolvedRegels} (waarde + doel + voorwaarde) en
 * {@link RegelSpraakModel#consistenties} (assertie) — en die twee dekken we hier identiek. Wijzigt {@code regelParameters}
 * ooit zijn collectie-set, dan moet deze spiegel meebewegen.
 *
 * <p><b>Versies.</b> Een regel kan meerdere §13.4.2.5-VERSIES hebben (zelfde geschreven naam, verschillende
 * {@link Geldigheid}). Elke versie is een aparte {@link dev.uchar.regelspraak.ir.ResolvedRegel} met een eigen
 * {@code #k}-gesuffixte naam en eigen geldigheid, dus door direct over {@code resolvedRegels} te itereren checken we elke
 * versie afzonderlijk met de versie-{@link Geldigheid} als regelperiode.
 *
 * <p><b>Dekking.</b> De dekking van één parameter is de UNIE van zijn intervallen over alle sets — de fase-1
 * pool-semantiek van {@link Parameterset#pool}. Een gat is een deelperiode van de regelperiode die door geen enkel
 * pool-interval gedekt is; bounds zijn inclusief, {@code null} = open kant. Een regel {@code geldig altijd} × een
 * parameter met alleen 2025-waarden levert dus TWEE open gaten: {@code [open t/m 2024-12-31]} en
 * {@code [2026-01-01 t/m open]}.
 *
 * <p><b>Idiomatiek.</b> Als {@link Dekking} is {@link #controleer} PUUR (het meldt niets) en is {@link #report} de aparte
 * consumer-aangeroepen methode die deduplicerend een WARNING op het model zet — precies zoals {@code Dekking.report}.
 *
 * @param regel       de (mogelijk {@code #k}-versie-gesuffixte) regelnaam
 * @param parameter   de gerefereerde parameternaam zoals in de sets/pool
 * @param rulePeriod de {@link Geldigheid} van deze regelversie (de regelperiode)
 * @param ontbrekend  de deelperiodes van {@code rulePeriod} die niet door enig parameter-interval gedekt zijn
 *                    ({@link Geldigheid}-shape, {@code null} = open kant); nooit leeg voor een bestaande {@code PeriodeDekking}
 */
public record PeriodeDekking(String regel, String parameter, Geldigheid rulePeriod, List<Geldigheid> missing) {

    /**
     * Puur: elke (regel-versie, parameter)-combinatie met minstens één gat tussen de regelperiode en de parameterdekking.
     * Meldt niets — de consumer roept daarna {@link #report} aan (Dekking-idiomatiek). Alleen parameters die in
     * {@code sets} voorkomen tellen (zie klasse-javadoc: het andere kanaal). Bij lege/afwezige sets is het resultaat
     * altijd leeg.
     */
    public static List<PeriodeDekking> controleer(RegelSpraakModel model, List<Parameterset> sets) {
        List<PeriodeDekking> out = new ArrayList<>();
        if (model == null || sets == null || sets.isEmpty()) return out;
        Map<String, List<WaardeInterval>> pool = Parameterset.pool(sets);
        if (pool.isEmpty()) return out;

        // Spiegelt ScenarioResolver.regelParameters: resolvedRegels (waarde + doel + voorwaarde) en consistenties (assertie).
        for (var r : model.resolvedRegels) {
            LinkedHashSet<String> params = new LinkedHashSet<>();
            catchExpr(r.waarde(), params);
            catchExpr(r.doel(), params);
            catchCondition(r.voorwaarde(), params);
            checkConstruct(r.naam(), r.geldigheid(), params, pool, out);
        }
        for (var c : model.consistenties) {
            LinkedHashSet<String> params = new LinkedHashSet<>();
            catchCondition(c.assertie(), params);
            checkConstruct(c.naam(), c.geldigheid(), params, pool, out);
        }
        return out;
    }

    private static void checkConstruct(String naam, Geldigheid rulePeriod, LinkedHashSet<String> params,
                                            Map<String, List<WaardeInterval>> pool, List<PeriodeDekking> out) {
        Geldigheid period = rulePeriod == null ? Geldigheid.ALTIJD : rulePeriod;
        for (String param : params) {
            List<WaardeInterval> intervals = pool.get(param);
            if (intervals == null) continue;   // niet in de sets -> het andere kanaal, geen melding hier
            List<Geldigheid> holes = holes(period, intervals);
            if (!holes.isEmpty()) out.add(new PeriodeDekking(naam, param, period, holes));
        }
    }

    /** De ParameterRef-namen in een waarde/doel-Expr — dezelfde {@code Expr.bevat}-walk als {@code regelParameters}. */
    private static void catchExpr(Expr e, LinkedHashSet<String> out) {
        Expr.bevat(e, x -> { if (x instanceof Expr.ParameterRef pr) out.add(pr.parameter().naam()); return false; });
    }

    /** De ParameterRef-namen in een voorwaarde/assertie — dezelfde {@code Conditie.bevatOperand}-walk. */
    private static void catchCondition(Conditie c, LinkedHashSet<String> out) {
        Conditie.bevatOperand(c, x -> { if (x instanceof Expr.ParameterRef pr) out.add(pr.parameter().naam()); return false; });
    }

    /**
     * De deelperiodes van {@code rulePeriod} die door geen enkel {@code intervals}-venster gedekt zijn: regelperiode
     * MINUS unie(intervallen). Sorteert de dekking op {@code vanaf} ({@code null} eerst = open begin), loopt de
     * regelperiode af met een cursor; inclusieve bounds ({@code null} = open kant): een gat begint op de dag NÁ de vorige
     * dekking-tot en eindigt op de dag VÓÓR de volgende dekking-vanaf. Overlappende/aangrenzende dekkingen worden gemerged
     * via de cursor. Leeg als de dekking de hele regelperiode omvat.
     */
    static List<Geldigheid> holes(Geldigheid rulePeriod, List<WaardeInterval> intervals) {
        List<Geldigheid> holes = new ArrayList<>();
        LocalDate rFrom = rulePeriod.vanaf();   // null = open begin
        LocalDate rTo = rulePeriod.tot();     // null = open eind

        // Dekking op vanaf sorteren (null-vanaf = open begin, dus eerst). We werken met de sentinel MIN/MAX voor
        // open kanten zodat de sweep geen null hoeft af te vangen; aan het eind vertalen we MIN/MAX terug naar null.
        List<Geldigheid> dekking = new ArrayList<>();
        for (WaardeInterval wi : intervals) dekking.add(wi.geldigheid());
        dekking.sort(Comparator.comparing(g -> g.vanaf() == null ? LocalDate.MIN : g.vanaf()));

        LocalDate lo = rFrom == null ? LocalDate.MIN : rFrom;   // ondergrens regelperiode
        LocalDate hi = rTo == null ? LocalDate.MAX : rTo;   // bovengrens regelperiode

        // cursor = de eerste dag binnen [lo,hi] die nog niet als gedekt bewezen is.
        LocalDate cursor = lo;
        for (Geldigheid d : dekking) {
            if (cursor.isAfter(hi)) break;                     // regelperiode volledig gedekt
            LocalDate dFrom = d.vanaf() == null ? LocalDate.MIN : d.vanaf();
            LocalDate dTo = d.tot()   == null ? LocalDate.MAX : d.tot();
            if (dTo.isBefore(cursor)) continue;               // dekking ligt geheel vóór de cursor
            if (dFrom.isAfter(cursor)) {                         // gat tussen cursor en deze dekking
                LocalDate g1 = min(dFrom.minusDays(1), hi);
                if (!cursor.isAfter(g1)) holes.add(window(cursor, g1));
            }
            if (dTo.equals(LocalDate.MAX)) { cursor = null; break; }   // dekking loopt open door: niets meer ongedekt
            LocalDate after = dTo.plusDays(1);
            if (after.isAfter(cursor)) cursor = after;               // schuif voorbij de dekking (merge overlap/aangrenzend)
        }
        // Staart-gat: alles van cursor t/m hi dat nog niet gedekt is (cursor null = open-eind gedekt).
        if (cursor != null && !cursor.isAfter(hi)) holes.add(window(cursor, hi));
        return holes;
    }

    /** Vertaal een [van,tot]-sweep-venster terug naar {@link Geldigheid}, met MIN/MAX -> open ({@code null}) kant. */
    private static Geldigheid window(LocalDate van, LocalDate to) {
        return new Geldigheid(van.equals(LocalDate.MIN) ? null : van, to.equals(LocalDate.MAX) ? null : to);
    }

    private static LocalDate min(LocalDate a, LocalDate b) { return a.isBefore(b) ? a : b; }

    /**
     * Meld deze bevinding als één WARNING-diagnostic op {@code model}, gededupliceerd (zoals {@code Dekking.report}): één
     * regel tekst die BEIDE periodes citeert — de regelperiode en elk ontbrekend gat. Consumer-aangeroepen; de check
     * ({@link #controleer}) blijft puur.
     */
    public void report(RegelSpraakModel model) {
        StringBuilder holes = new StringBuilder();
        for (int i = 0; i < missing.size(); i++) {
            if (i > 0) holes.append(", ");
            holes.append(toon(missing.get(i)));
        }
        var w = new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.WARNING,
                "periodedekking: regel '" + regel + "' (geldig " + toon(rulePeriod) + ") verwijst naar parameter '"
                        + parameter + "' zonder geldige waarde in " + holes, 0);
        if (!model.diagnostics.contains(w)) model.diagnostics.add(w);
    }

    /** Meld ALLE bevindingen op het model (deduplicerend). Consumer-gemak boven een handmatige lus. */
    public static void meldAlle(List<PeriodeDekking> findings, RegelSpraakModel model) {
        for (PeriodeDekking pd : findings) pd.report(model);
    }

    /** {@code [vanaf t/m tot]} met "(open)" voor een null-kant — zelfde renderstijl als {@link Parameterset}. */
    private static String toon(Geldigheid g) {
        String v = g.vanaf() == null ? "(open)" : g.vanaf().toString();
        String t = g.tot() == null ? "(open)" : g.tot().toString();
        return "[" + v + " t/m " + t + "]";
    }
}
