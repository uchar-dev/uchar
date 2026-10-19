package dev.uchar.regelspraak.model;

import dev.uchar.regelspraak.ir.Validity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Een <b>parameterset</b>: gedeployde, geldigheids-geversioneerde parameterwaarden als DATA (geen RegelSpraak-syntax).
 * De wet is datum-afhankelijk — tarieven wijzigen elk jaar op 1 januari — dus een parameter draagt niet één waarde
 * maar een lijst {@link ValueInterval}s, elk met een {@link Validity}-venster. Per verzoek (session) wordt tegen
 * ÉÉN datumautoriteit (de peildatum, die tevens de rekendatum-global is) de vintage geselecteerd: het unieke interval
 * dat de peildatum bevat levert de waarde. Zie de card
 * {@code backlog/new/20260705-15:18-geldigheid-parametersets-peildatum.md}.
 *
 * <p>Sets zijn <b>organisatorisch</b> (bijv. één set per belastingjaar); de waarheid per parameter is de UNIE van alle
 * intervallen over alle sets heen. De hulpfuncties hieronder werken daarom over een {@code List<ParameterSet>} en
 * poolen per parameternaam. De semantiek is <b>fail-fast</b>:
 * <ul>
 *   <li><b>overlap</b> — twee intervallen van één parameter die minstens één datum delen is een LAAD-fout
 *       ({@link #overlapErrors});</li>
 *   <li><b>gap</b> — geen interval dat de peildatum bevat is een SESSION-fout die parameter + datum noemt, nooit een
 *       stille default ({@link #valuesOn}).</li>
 * </ul>
 *
 * <p>Bounds zijn inclusief; een {@code null}-bound is open (zie {@link Validity#contains}). {@link Validity#ALTIJD}
 * (beide null) overlapt elk ander interval en bevat elke datum — het degenererende single-value-geval.
 *
 * @param naam    de naam van de set (organisatorisch; verschijnt op /info)
 * @param waarden per parameternaam de lijst intervallen; de keys zijn de namen zoals in de set gebruikt
 *                (de lader normaliseert naar de canonieke {@code model}-Parameter.name())
 */
public record ParameterSet(String name, Map<String, List<ValueInterval>> values) {

    /**
     * Eén geversioneerde parameterwaarde: de gedecodeerde {@code value} (Java-type per {@link Datatype}, {@code null}
     * = expliciete leegwaarde) geldig binnen {@code validity}.
     */
    public record ValueInterval(Object value, Validity validity) {}

    /**
     * De UNIE van alle intervallen per parameternaam over alle sets heen (de waarheid per parameter). Publiek zodat een
     * compile-time consistentiecheck exact dezelfde
     * pool-semantiek deelt: dekking-per-parameter is de vereniging over alle sets, precies zoals {@link #valuesOn} en
     * {@link #overlapErrors} die zien. De keys zijn de parameternamen zoals ze in de sets staan (canoniek genormaliseerd
     * door de lader). Een lege/afwezige set draagt niets bij.
     */
    public static Map<String, List<ValueInterval>> pool(List<ParameterSet> sets) {
        Map<String, List<ValueInterval>> pool = new LinkedHashMap<>();
        for (ParameterSet s : sets)
            if (s != null && s.values() != null)
                for (var e : s.values().entrySet())
                    pool.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).addAll(e.getValue());
        return pool;
    }

    /**
     * Elke overlap-fout over alle sets heen: twee intervallen van ÉÉN parameter (gepoold) die minstens één datum
     * delen. Bounds inclusief, {@code null} = open, dus een open kant overlapt alles aan die kant. Aangrenzend
     * ({@code tot=2025-12-31} naast {@code vanaf=2026-01-01}) overlapt NIET. Elke fout noemt de parameter en beide
     * periodes; leeg = geen overlap.
     */
    public static List<String> overlapErrors(List<ParameterSet> sets) {
        List<String> faults = new ArrayList<>();
        for (var e : pool(sets).entrySet()) {
            List<ValueInterval> list = e.getValue();
            for (int i = 0; i < list.size(); i++)
                for (int j = i + 1; j < list.size(); j++) {
                    Validity a = list.get(i).validity(), b = list.get(j).validity();
                    if (overlaps(a, b))
                        faults.add("parameter '" + e.getKey() + "' heeft overlappende geldigheidsperiodes "
                                + show(a) + " en " + show(b));
                }
        }
        return faults;
    }

    /** Twee inclusieve, open-toegestane intervallen delen minstens één datum. */
    private static boolean overlaps(Validity a, Validity b) {
        // a start na b's einde -> disjunct; b start na a's einde -> disjunct; anders overlap.
        if (a.from() != null && b.until() != null && a.from().isAfter(b.until())) return false;
        if (b.from() != null && a.until() != null && b.from().isAfter(a.until())) return false;
        return true;
    }

    /**
     * Per parameter (gepoold over alle sets) de waarde van het unieke interval dat {@code asOfDate} bevat. Ontbreekt
     * zo'n interval, dan wordt een fout die parameter + peildatum noemt aan {@code faults} toegevoegd (loud, nooit een
     * default) en de parameter niet in het resultaat opgenomen. De keys van het resultaat zijn de parameternamen zoals
     * ze in de sets staan. Een expliciete leegwaarde ({@code null}) is een geldige uitkomst en wordt wél opgenomen.
     */
    public static Map<String, Object> valuesOn(List<ParameterSet> sets, LocalDate asOfDate, List<String> faults) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (var e : pool(sets).entrySet()) {
            ValueInterval chosen = null;
            for (ValueInterval wi : e.getValue())
                if (wi.validity().contains(asOfDate)) { chosen = wi; break; }
            if (chosen == null)
                faults.add("parameter '" + e.getKey() + "' heeft geen geldige waarde op peildatum " + asOfDate);
            else
                result.put(e.getKey(), chosen.value());
        }
        return result;
    }

    /**
     * Of de sets meer dan één vintage bevatten: {@code true} zodra enige parameter &gt;1 interval heeft OF enig
     * interval begrensd is (niet {@link Validity#ALTIJD}). Consumers gebruiken dit om een ontbrekende peildatum te
     * eisen (multi-vintage = de peildatum is verplicht; anders is het degenererende single-value-pad toegestaan).
     */
    public static boolean multiVintage(List<ParameterSet> sets) {
        for (var e : pool(sets).entrySet()) {
            List<ValueInterval> list = e.getValue();
            if (list.size() > 1) return true;
            for (ValueInterval wi : list)
                if (!wi.validity().always()) return true;
        }
        return false;
    }

    private static String show(Validity g) {
        String v = g.from() == null ? "(open)" : g.from().toString();
        String t = g.until() == null ? "(open)" : g.until().toString();
        return "[" + v + " t/m " + t + "]";
    }
}
