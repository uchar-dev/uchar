package dev.uchar.regelspraak.emit;

import dev.uchar.regelspraak.ir.ResolvedRuleGroup;
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
    public static String fileName(int n, ResolvedScenario s) {
        return "case" + n + "-" + slug(s.naam()) + ".json";
    }

    /** The scenario name as a file-name part: diacritics taken off, lowercased, every run of characters other than
     *  a-z and 0-9 one '-', none at either end. Empty when the name has no letter or digit, which the resolver
     *  refuses. */
    public static String slug(String naam) {
        String withoutSigns = java.text.Normalizer.normalize(naam, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutSigns.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
    }

    /** The model the IR document is emitted from: {@code m} itself without a scope, and with one a copy of the whole
     *  model ({@link RegelSpraakModel#copy}) from which every named form the scope does not carry is dropped, and
     *  every diagnostic raised on a line of a source block whose forms it leaves out entirely
     *  ({@link Scope#omitted}): the document carries what its forms and its declarations raise. A rule group keeps
     *  its carried members and is dropped when none is left. */
    public static RegelSpraakModel withScope(RegelSpraakModel m, Scope scope) {
        if (scope == null) return m;
        Set<String> names = new HashSet<>();
        for (String v : scope.forms()) names.add(norm(v));
        Predicate<String> carries = naam -> names.contains(norm(baseName(naam)));
        RegelSpraakModel k = m.copy();
        k.resolvedRegels.removeIf(r -> !carries.test(r.naam()));
        k.objectCreations.removeIf(x -> !carries.test(x.naam()));
        k.allocations.removeIf(x -> !carries.test(x.naam()));
        k.consistenties.removeIf(x -> !carries.test(x.naam()));
        k.uniquenesses.removeIf(x -> !carries.test(x.naam()));
        k.uniquenessUnions.removeIf(x -> !carries.test(x.naam()));
        k.factCreations.removeIf(x -> !carries.test(x.naam()));
        k.prioritizedDefaults.removeIf(x -> !carries.test(x.naam()));
        k.diagnostics.removeIf(d -> scope.omitted().stream().anyMatch(b -> b.contains(d.line())));
        k.ruleGroups.clear();
        for (ResolvedRuleGroup g : m.ruleGroups) {
            List<String> members = g.members().stream().filter(carries).toList();
            if (!members.isEmpty()) k.ruleGroups.add(new ResolvedRuleGroup(g.naam(), g.recursive(), members, g.source(), g.line()));
        }
        return k;
    }

    /** The case file of {@code s}: two-space indented, a trailing newline, the corpus's key order. */
    public static String emit(ResolvedScenario s) {
        IrEmitter.Json w = new IrEmitter.Json();
        w.beginObject();
        w.key("format").value("uchar-case-v0");
        w.key("asOfDate");
        if (s.asOfDate() == null) w.valueNull(); else w.value(s.asOfDate().toString());
        w.key("input").beginObject();
        w.key("instances").beginArray();
        for (Instance i : s.situation().instances()) instantie(w, i);
        w.endArray();
        w.key("parameters").beginObject();
        for (ParameterValue p : s.situation().parameters()) {
            w.key(p.parameter());
            if (p.tijdlijn() != null) timelineValue(w, p.tijdlijn()); else value(w, p.value());
        }
        w.endObject();
        w.endObject();
        w.key("expected").beginArray();
        for (Expectation v : s.expectations()) verwachting(w, v);
        w.endArray();
        w.endObject();
        w.sb.append('\n');
        return w.sb.toString();
    }

    private static void instantie(IrEmitter.Json w, Instance i) {
        w.beginObject();
        w.key("label").value(i.label());
        w.key("objectType").value(i.objectType().naam);
        w.key("attributes").beginObject();
        for (AttributeValue a : i.attributen()) {
            w.key(a.attribute());
            if (a.tijdlijn() != null) timelineValue(w, a.tijdlijn()); else value(w, a.value());
        }
        w.endObject();
        w.key("roles").beginObject();
        for (RoleValue r : i.rollen()) {
            w.key(r.key()).beginArray();
            for (String l : r.targetLabels()) w.value(l);
            w.endArray();
        }
        w.endObject();
        w.key("characteristics").beginArray();
        for (CharacteristicValue k : i.kenmerken()) if (k.periods() == null) w.value(k.characteristic());
        w.endArray();
        List<CharacteristicValue> temporal = new ArrayList<>();
        for (CharacteristicValue k : i.kenmerken()) if (k.periods() != null) temporal.add(k);
        if (!temporal.isEmpty()) {
            w.key("characteristicTimelines").beginObject();
            for (CharacteristicValue k : temporal) {
                w.key(k.characteristic()).beginArray();
                for (TimeWindow p : k.periods()) {
                    w.beginObject();
                    w.key("from").value(p.van().toString());
                    w.key("to").value(p.until().toString());
                    w.endObject();
                }
                w.endArray();
            }
            w.endObject();
        }
        w.endObject();
    }

    private static void verwachting(IrEmitter.Json w, Expectation v) {
        w.beginObject();
        switch (v.category()) {
            case ATTRIBUTE_VALUE -> {
                w.key("kind").value("attributeValue");
                w.key("label").value(v.label());
                w.key("attribute").value(v.attribute());
                w.key("value");
                value(w, v.expectedValue());
            }
            case ATTRIBUTE_EMPTY -> {
                w.key("kind").value("attributeEmpty");
                w.key("label").value(v.label());
                w.key("attribute").value(v.attribute());
            }
            case ATTRIBUTE_TIMELINE -> {
                w.key("kind").value("attributeTimeline");
                w.key("label").value(v.label());
                w.key("attribute").value(v.attribute());
                w.key("timeline");
                segments(w, v.tijdlijn());
            }
            case CHARACTERISTIC_PRESENT -> {
                w.key("kind").value("characteristicPresent");
                w.key("label").value(v.label());
                w.key("characteristic").value(v.characteristic());
            }
            case CHARACTERISTIC_ABSENT -> {
                w.key("kind").value("characteristicAbsent");
                w.key("label").value(v.label());
                w.key("characteristic").value(v.characteristic());
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

    private static void timelineValue(IrEmitter.Json w, List<Segment> tijdlijn) {
        w.beginObject();
        w.key("timeline");
        segments(w, tijdlijn);
        w.endObject();
    }

    private static void segments(IrEmitter.Json w, List<Segment> tijdlijn) {
        w.beginArray();
        for (Segment s : tijdlijn) {
            w.beginObject();
            w.key("from").value(s.van().toString());
            w.key("to").value(s.until().toString());
            w.key("value");
            value(w, s.value());
            w.endObject();
        }
        w.endArray();
    }

    /** A value in its one case variant; a date-time in the frontend's lexeme with its three millisecond digits,
     *  which is how the IR writes a date-time literal too. */
    private static void value(IrEmitter.Json w, ScenarioValue v) {
        w.beginObject();
        if (v.number() != null) w.key("number").value(v.number());
        else if (v.date() != null) w.key("date").value(v.date().toString());
        else if (v.dateTime() != null)
            w.key("dateTime").value(v.dateTime().format(java.time.format.DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS")));
        else if (v.bool() != null) w.key("boolean").value(v.bool().booleanValue());
        else if (v.tekst() != null) w.key("text").value(v.tekst());
        else if (v.enumValue() != null) w.key("enumeration").value(v.enumValue());
        else throw new IllegalStateException("CaseEmitter: een waarde zonder variant");
        w.endObject();
    }

    /** A resolved form name without its §13.4.2.3 version suffix ({@code "<naam> #k"}): the name a scope names a
     *  form by. The scenario resolver reads it here too. */
    public static String baseName(String naam) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(.*\\S) #[0-9]+").matcher(naam);
        return m.matches() ? m.group(1) : naam;
    }

    /** A name as the scenario layer compares names: trimmed, its whitespace one space, lowercased. */
    public static String norm(String s) { return s == null ? "" : s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT); }
}
