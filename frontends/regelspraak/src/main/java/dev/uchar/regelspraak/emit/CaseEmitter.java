package dev.uchar.regelspraak.emit;

import dev.uchar.regelspraak.ir.ResolvedRegelGroep;
import dev.uchar.regelspraak.ir.ResolvedScenario;
import dev.uchar.regelspraak.ir.ResolvedScenario.*;
import dev.uchar.regelspraak.model.RegelSpraakModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The lowering of the scenariotoetsen extension (grammar/DIVERGENCES.md, "Scenariotoetsen"): a resolved scenario
 * becomes one case file of the format uchar-case-v0 ({@code spec/schema/uchar-case-v0.schema.json}), and a scope
 * becomes the set of forms the IR document beside the case files carries. The case format changes in nothing and no
 * executor learns anything new: a scenario is a case file written in RegelSpraak.
 *
 * <p>The key order is the order every case file of the corpus writes: the schema's {@code properties} order, with
 * one exception the corpus fixes — an instance writes {@code characteristicTimelines} after {@code characteristics},
 * where the schema lists it before (all 51 instances in the corpus that carry the field, measured 2026-09-26). The
 * serialiser is the emitter's own ({@link IrEmitter.Json}), so a case file and an IR document share one shape.
 */
public final class CaseEmitter {

    private CaseEmitter() {}

    /** The file name of the {@code n}-th scenario (1-based, in document order): {@code case<n>-<slug>.json}. */
    public static String bestandsnaam(int n, ResolvedScenario s) {
        return "case" + n + "-" + slug(s.naam()) + ".json";
    }

    /** The scenario name as a file-name part: diacritics taken off, lowercased, every run of characters other than
     *  a-z and 0-9 one '-', none at either end. Empty when the name has no letter or digit, which the resolver
     *  refuses. */
    public static String slug(String naam) {
        String zonderTekens = java.text.Normalizer.normalize(naam, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return zonderTekens.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
    }

    /** The model the IR document is emitted from: {@code m} itself without a scope, and with one a copy of the whole
     *  model ({@link RegelSpraakModel#kopie}) from which every named form the scope does not carry is dropped, and
     *  every diagnostic raised on a line of a source block whose forms it leaves out entirely
     *  ({@link Scope#weggelaten}): the document carries what its forms and its declarations raise. A rule group keeps
     *  its carried members and is dropped when none is left. */
    public static RegelSpraakModel metScope(RegelSpraakModel m, Scope scope) {
        if (scope == null) return m;
        Set<String> namen = new HashSet<>();
        for (String v : scope.vormen()) namen.add(norm(v));
        Predicate<String> draagt = naam -> namen.contains(norm(basisNaam(naam)));
        RegelSpraakModel k = m.kopie();
        k.resolvedRegels.removeIf(r -> !draagt.test(r.naam()));
        k.objectCreaties.removeIf(x -> !draagt.test(x.naam()));
        k.verdelingen.removeIf(x -> !draagt.test(x.naam()));
        k.consistenties.removeIf(x -> !draagt.test(x.naam()));
        k.uniciteiten.removeIf(x -> !draagt.test(x.naam()));
        k.uniciteitUnies.removeIf(x -> !draagt.test(x.naam()));
        k.feitCreaties.removeIf(x -> !draagt.test(x.naam()));
        k.prioritizedDefaults.removeIf(x -> !draagt.test(x.naam()));
        k.diagnostics.removeIf(d -> scope.weggelaten().stream().anyMatch(b -> b.bevat(d.line())));
        k.regelGroepen.clear();
        for (ResolvedRegelGroep g : m.regelGroepen) {
            List<String> leden = g.leden().stream().filter(draagt).toList();
            if (!leden.isEmpty()) k.regelGroepen.add(new ResolvedRegelGroep(g.naam(), g.recursief(), leden, g.bron(), g.line()));
        }
        return k;
    }

    /** The case file of {@code s}: two-space indented, a trailing newline, the corpus's key order. */
    public static String emit(ResolvedScenario s) {
        IrEmitter.Json w = new IrEmitter.Json();
        w.beginObject();
        w.key("format").value("uchar-case-v0");
        w.key("asOfDate");
        if (s.peildatum() == null) w.valueNull(); else w.value(s.peildatum().toString());
        w.key("input").beginObject();
        w.key("instances").beginArray();
        for (Instantie i : s.situatie().instanties()) instantie(w, i);
        w.endArray();
        w.key("parameters").beginObject();
        for (ParameterWaarde p : s.situatie().parameters()) {
            w.key(p.parameter());
            if (p.tijdlijn() != null) tijdlijnWaarde(w, p.tijdlijn()); else waarde(w, p.waarde());
        }
        w.endObject();
        w.endObject();
        w.key("expected").beginArray();
        for (Verwachting v : s.verwachtingen()) verwachting(w, v);
        w.endArray();
        w.endObject();
        w.sb.append('\n');
        return w.sb.toString();
    }

    private static void instantie(IrEmitter.Json w, Instantie i) {
        w.beginObject();
        w.key("label").value(i.label());
        w.key("objectType").value(i.objecttype().naam);
        w.key("attributes").beginObject();
        for (AttribuutWaarde a : i.attributen()) {
            w.key(a.attribuut());
            if (a.tijdlijn() != null) tijdlijnWaarde(w, a.tijdlijn()); else waarde(w, a.waarde());
        }
        w.endObject();
        w.key("roles").beginObject();
        for (RolWaarde r : i.rollen()) {
            w.key(r.sleutel()).beginArray();
            for (String l : r.doelLabels()) w.value(l);
            w.endArray();
        }
        w.endObject();
        w.key("characteristics").beginArray();
        for (KenmerkWaarde k : i.kenmerken()) if (k.perioden() == null) w.value(k.kenmerk());
        w.endArray();
        List<KenmerkWaarde> temporeel = new ArrayList<>();
        for (KenmerkWaarde k : i.kenmerken()) if (k.perioden() != null) temporeel.add(k);
        if (!temporeel.isEmpty()) {
            w.key("characteristicTimelines").beginObject();
            for (KenmerkWaarde k : temporeel) {
                w.key(k.kenmerk()).beginArray();
                for (Periode p : k.perioden()) {
                    w.beginObject();
                    w.key("from").value(p.van().toString());
                    w.key("to").value(p.tot().toString());
                    w.endObject();
                }
                w.endArray();
            }
            w.endObject();
        }
        w.endObject();
    }

    private static void verwachting(IrEmitter.Json w, Verwachting v) {
        w.beginObject();
        switch (v.soort()) {
            case ATTRIBUUT_WAARDE -> {
                w.key("kind").value("attributeValue");
                w.key("label").value(v.label());
                w.key("attribute").value(v.attribuut());
                w.key("value");
                waarde(w, v.verwachteWaarde());
            }
            case ATTRIBUUT_LEEG -> {
                w.key("kind").value("attributeEmpty");
                w.key("label").value(v.label());
                w.key("attribute").value(v.attribuut());
            }
            case ATTRIBUUT_TIJDLIJN -> {
                w.key("kind").value("attributeTimeline");
                w.key("label").value(v.label());
                w.key("attribute").value(v.attribuut());
                w.key("timeline");
                segmenten(w, v.tijdlijn());
            }
            case KENMERK_AANWEZIG -> {
                w.key("kind").value("characteristicPresent");
                w.key("label").value(v.label());
                w.key("characteristic").value(v.kenmerk());
            }
            case KENMERK_AFWEZIG -> {
                w.key("kind").value("characteristicAbsent");
                w.key("label").value(v.label());
                w.key("characteristic").value(v.kenmerk());
            }
            case CONSISTENT -> w.key("kind").value("consistent");
            case INCONSISTENT -> {
                w.key("kind").value("inconsistent");
                if (v.regel() != null) w.key("rule").value(v.regel());
                if (v.label() != null) w.key("label").value(v.label());
            }
        }
        w.endObject();
    }

    private static void tijdlijnWaarde(IrEmitter.Json w, List<Segment> tijdlijn) {
        w.beginObject();
        w.key("timeline");
        segmenten(w, tijdlijn);
        w.endObject();
    }

    private static void segmenten(IrEmitter.Json w, List<Segment> tijdlijn) {
        w.beginArray();
        for (Segment s : tijdlijn) {
            w.beginObject();
            w.key("from").value(s.van().toString());
            w.key("to").value(s.tot().toString());
            w.key("value");
            waarde(w, s.waarde());
            w.endObject();
        }
        w.endArray();
    }

    /** A value in its one case variant; a date-time in the frontend's lexeme with its three millisecond digits,
     *  which is how the IR writes a date-time literal too. */
    private static void waarde(IrEmitter.Json w, Waarde v) {
        w.beginObject();
        if (v.getal() != null) w.key("number").value(v.getal());
        else if (v.datum() != null) w.key("date").value(v.datum().toString());
        else if (v.datumTijd() != null)
            w.key("dateTime").value(v.datumTijd().format(java.time.format.DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS")));
        else if (v.bool() != null) w.key("boolean").value(v.bool().booleanValue());
        else if (v.tekst() != null) w.key("text").value(v.tekst());
        else if (v.enumWaarde() != null) w.key("enumeration").value(v.enumWaarde());
        else throw new IllegalStateException("CaseEmitter: een waarde zonder variant");
        w.endObject();
    }

    private static String basisNaam(String naam) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(.*\\S) #[0-9]+").matcher(naam);
        return m.matches() ? m.group(1) : naam;
    }

    private static String norm(String s) { return s == null ? "" : s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT); }
}
