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
        return "case" + n + "-" + slug(s.name()) + ".json";
    }

    /** The scenario name as a file-name part: diacritics taken off, lowercased, every run of characters other than
     *  a-z and 0-9 one '-', none at either end. Empty when the name has no letter or digit, which the resolver
     *  refuses. */
    public static String slug(String name) {
        String withoutSigns = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
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
        Predicate<String> carries = name -> names.contains(norm(baseName(name)));
        RegelSpraakModel k = m.copy();
        k.resolvedRules.removeIf(r -> !carries.test(r.name()));
        k.objectCreations.removeIf(x -> !carries.test(x.name()));
        k.allocations.removeIf(x -> !carries.test(x.name()));
        k.consistencies.removeIf(x -> !carries.test(x.name()));
        k.uniquenesses.removeIf(x -> !carries.test(x.name()));
        k.uniquenessUnions.removeIf(x -> !carries.test(x.name()));
        k.factCreations.removeIf(x -> !carries.test(x.name()));
        k.prioritizedDefaults.removeIf(x -> !carries.test(x.name()));
        k.diagnostics.removeIf(d -> scope.omitted().stream().anyMatch(b -> b.contains(d.line())));
        k.ruleGroups.clear();
        for (ResolvedRuleGroup g : m.ruleGroups) {
            List<String> members = g.members().stream().filter(carries).toList();
            if (!members.isEmpty()) k.ruleGroups.add(new ResolvedRuleGroup(g.name(), g.recursive(), members, g.source(), g.line()));
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
        for (Instance i : s.situation().instances()) instance(w, i);
        w.endArray();
        w.key("parameters").beginObject();
        for (ParameterValue p : s.situation().parameters()) {
            w.key(p.parameter());
            if (p.timeline() != null) timelineValue(w, p.timeline()); else value(w, p.value());
        }
        w.endObject();
        w.endObject();
        w.key("expected").beginArray();
        for (Expectation v : s.expectations()) expectation(w, v);
        w.endArray();
        w.endObject();
        w.sb.append('\n');
        return w.sb.toString();
    }

    private static void instance(IrEmitter.Json w, Instance i) {
        w.beginObject();
        w.key("label").value(i.label());
        w.key("objectType").value(i.objectType().name);
        w.key("attributes").beginObject();
        for (AttributeValue a : i.attributes()) {
            w.key(a.attribute());
            if (a.timeline() != null) timelineValue(w, a.timeline()); else value(w, a.value());
        }
        w.endObject();
        w.key("roles").beginObject();
        for (RoleValue r : i.roles()) {
            w.key(r.key()).beginArray();
            for (String l : r.targetLabels()) w.value(l);
            w.endArray();
        }
        w.endObject();
        w.key("characteristics").beginArray();
        for (CharacteristicValue k : i.characteristics()) if (k.periods() == null) w.value(k.characteristic());
        w.endArray();
        List<CharacteristicValue> temporal = new ArrayList<>();
        for (CharacteristicValue k : i.characteristics()) if (k.periods() != null) temporal.add(k);
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

    private static void expectation(IrEmitter.Json w, Expectation v) {
        w.beginObject();
        switch (v.kind()) {
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
                segments(w, v.timeline());
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
                if (v.rule() != null) w.key("rule").value(v.rule());
                if (v.label() != null) w.key("label").value(v.label());
            }
        }
        w.endObject();
    }

    private static void timelineValue(IrEmitter.Json w, List<Segment> timeline) {
        w.beginObject();
        w.key("timeline");
        segments(w, timeline);
        w.endObject();
    }

    private static void segments(IrEmitter.Json w, List<Segment> timeline) {
        w.beginArray();
        for (Segment s : timeline) {
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
        else if (v.text() != null) w.key("text").value(v.text());
        else if (v.enumValue() != null) w.key("enumeration").value(v.enumValue());
        else throw new IllegalStateException("CaseEmitter: een waarde zonder variant");
        w.endObject();
    }

    /** A resolved form name without its §13.4.2.3 version suffix ({@code "<naam> #k"}): the name a scope names a
     *  form by. The scenario resolver reads it here too. */
    public static String baseName(String name) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(.*\\S) #[0-9]+").matcher(name);
        return m.matches() ? m.group(1) : name;
    }

    /** A name as the scenario layer compares names: trimmed, its whitespace one space, lowercased. */
    public static String norm(String s) { return s == null ? "" : s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT); }
}
