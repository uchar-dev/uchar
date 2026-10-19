package dev.uchar.regelspraak.emit;

import dev.uchar.regelspraak.ir.Source;
import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.ir.Default;
import dev.uchar.regelspraak.ir.ExceptionGroup;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.Validity;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.ir.ResolvedConsistency;
import dev.uchar.regelspraak.ir.ResolvedDayKindDefinition;
import dev.uchar.regelspraak.ir.ResolvedFactCreation;
import dev.uchar.regelspraak.ir.ResolvedObjectCreation;
import dev.uchar.regelspraak.ir.ResolvedRule;
import dev.uchar.regelspraak.ir.ResolvedRuleGroup;
import dev.uchar.regelspraak.ir.ResolvedStartPointDetermination;
import dev.uchar.regelspraak.ir.ResolvedUniqueness;
import dev.uchar.regelspraak.ir.ResolvedUniquenessUnion;
import dev.uchar.regelspraak.ir.ResolvedAllocation;
import dev.uchar.regelspraak.ir.Spelling;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.Unit;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Dimension;
import dev.uchar.regelspraak.model.RegelSpraakModel.UnitSystem;
import dev.uchar.regelspraak.model.RegelSpraakModel.FactType;
import dev.uchar.regelspraak.model.RegelSpraakModel.Granularity;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;
import dev.uchar.regelspraak.model.RegelSpraakModel.Parameter;
import dev.uchar.regelspraak.model.RegelSpraakModel.Role;
import dev.uchar.regelspraak.model.RegelSpraakModel.Timeline;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import static dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic.params;

/**
 * The mirror emitter: a faithful, engine-neutral JSON dump of the complete resolved
 * {@link RegelSpraakModel} — every declaration, every resolved IR list, and the diagnostics.
 * It writes {@code uchar-ir-v1} in its L5 shape: every node, key and enumeration value under its IR-SPEC name, the
 * enumeration values through the one table {@link Spelling}; a node's source text is its {@code source} record
 * ({@code snippet}, and the {@code line} on a consistency version and a rule group), absent where the node has
 * none; an optional field the model leaves empty is absent, not null. The Java records keep their v0 names, so
 * this emitter is where the two spellings meet. The container is the v1 one
 * ({@code {format, model, rules, diagnostics}}, the declarations as lists, an object type's
 * members as {@code Attribute}/{@code Characteristic} nodes, a domain as a {@code Domain}
 * wrapper). The type system is IR-SPEC's: a number is a {@code Number} (a percentage one with the
 * unit '%'), a date a {@code Date} with its precision, a unit a {@code Unit} whose factor is the
 * exact fraction its declaration wrote, a unit system a {@code UnitSystem} listing its units with
 * their spellings, a parameter a {@code Parameter} with its timeline facet, a day kind a
 * {@code DayKind}. A reference to a declared thing carries its symbolic form and nothing beside it:
 * the reference nodes carry no type annotation (the declaration is the one source), a read that
 * navigates carries its role bindings and a read without them is the subject's own, a fold over
 * linked instances, an allocation, a navigated characteristic check and a fact creation carry
 * their bindings, an object creation carries the role its sentence names, and a role check is one
 * {@code RoleMembership} node. Where the resolver found
 * no unique binding for a position that needs one, the model is refused — a document without the
 * binding would be read as something else. A named form
 * ({@code Rule}, {@code Allocation}, {@code ObjectCreation}, {@code FactCreation}, {@code Consistency},
 * {@code Uniqueness}) is its name and its versions, which the model's lists spell as one entry per version; a rule
 * version writes its kind as {@code initialisation} and {@code presentation}; a fold is an {@code Aggregation} and
 * every other function a {@code BuiltinCall}; a literal writes the typed value its type names; the bound day of a
 * day-kind definition is a {@code BoundDay} and a session value a {@code ContextValue}. No filtering: it is the
 * uchar-programme mirror artifact (mirror first, diverge later), with one exception the wire forces — an allocation
 * whose method the frontend could not decide has no spelling, so it is left out and a WARNING names it
 * ({@link #unwrittenAllocations}). The key order of every node is the order this emitter wrote it in before the
 * L5 renaming, with the keys renamed in place and a field left out where the model is empty, so a re-emission is
 * byte-identical to a migrated document. That is the {@code properties} order of the schema
 * ({@code spec/schema/uchar-ir-v1.schema.json}), as {@code spec/schema/README.md} records.
 *
 * <p>Completeness is compiler-enforced where it can be: the {@link Datatype}, {@link Expr}
 * and {@link Condition} sealed hierarchies are walked with EXHAUSTIVE switches (no default
 * branch), so adding a subtype fails compilation until this emitter handles it. Every node
 * carries a {@code "node"} discriminator: its name on the wire, whatever the Java record is called
 * ({@code Regelversie} is the one node the wire still spells in its v0 form, delta B25).
 *
 * <p>Determinism: maps and lists are iterated in their natural (insertion/declaration)
 * order — no sorting — so the output is byte-stable across runs on the same input. Numbers
 * are written via {@link BigDecimal#toPlainString()}. References to declared model objects
 * (an objecttype, a parameter) are emitted by their NAME — the field holds a reference into
 * the top-level declaration maps, so its identity, not a duplicated inline copy, is the
 * faithful serialisation.
 */
public final class IrEmitter {

    /** The allocations the wire cannot carry, as WARNING diagnostics: one whose method the frontend could not decide
     *  (the model's placeholder for it). IR-SPEC ch. 3.2 gives an allocation one of three methods and nothing else,
     *  so it is left out of the document and this names it, which is the producer's account of what stayed unresolved
     *  (WF-113). The model has no source line for an allocation, so the line is 0: a frontend warning without a
     *  token. {@link #emit} appends these after the model's own diagnostics; {@code Resolver --ir} prints them. */
    public static List<Diagnostic> unwrittenAllocations(RegelSpraakModel m) {
        return m.allocations.stream().filter(v -> v.method() == ResolvedAllocation.Method.COMPLEX)
                .map(v -> new Diagnostic(RegelSpraakModel.Severity.WARNING,
                        "RGS-001", params("name", v.name()), 0))
                .toList();
    }

    /** Serialise the resolved model to the IR JSON text (two-space indented, trailing newline). */
    public static String emit(RegelSpraakModel m) {
        // WF-113 (a): an ERROR means the producer refused the document, so a model that carries one has no
        // document to write; checked here and not only in irMode, so no emission route bypasses it. The
        // message and line clauses are the verifier's too: a document carrying either would be refused there.
        // Each arm is refused in the verifier's sentence, at the pointer the verifier reports it at, followed by
        // the refused diagnostic itself: an ERROR's own message is what the caller needs to read.
        List<String> nonWire = new java.util.ArrayList<>();
        for (int i = 0; i < m.diagnostics.size(); i++) {
            Diagnostic d = m.diagnostics.get(i);
            String severity = severityRefusal(d.severity());
            if (severity != null) nonWire.add("at /diagnostics/" + i + "/severity: " + severity + " — " + d);
            if (d.message() == null || d.message().isBlank())
                nonWire.add("at /diagnostics/" + i + "/message: diagnostic with an empty or blank message: an entry "
                        + "that says nothing accounts for nothing — " + d);
            if (d.line() < 0)
                nonWire.add("at /diagnostics/" + i + "/line: diagnostic with line " + d.line() + ": a line is a "
                        + "source position, and 0 is the value for none (a hand-built document, or a producer warning "
                        + "without a token) — " + d);
        }
        if (!nonWire.isEmpty())
            throw new IllegalStateException("IrEmitter: WF-113 (a) " + String.join("; ", nonWire));
        IrEmitter e = new IrEmitter();
        e.model(m);
        e.w.sb.append('\n');
        return e.w.sb.toString();
    }

    /** WF-113 (a)'s severity arm, the verifier's sentences: warning is the one wire severity. Null for a warning. */
    private static String severityRefusal(RegelSpraakModel.Severity s) {
        if (s == null) return "diagnostic without a severity: the one wire severity is warning";
        return switch (s) {
            case WARNING -> null;
            case ERROR -> "severity error is not a wire severity: an error means the producer refused to write the "
                    + "document, so a document that was written carries none";
            case INFO -> "severity info is not a wire severity: the list is the account of what stayed unresolved, "
                    + "and info is tooling talk";
        };
    }

    private final Json w = new Json();

    private IrEmitter() {}

    /** The resolved model, kept for the role-binding derivations of the uchar extension
     *  fields (emitted only when uniquely derivable — absent otherwise, never null). */
    private RegelSpraakModel m;
    /** The subject object type of the form being emitted; fold-source role bindings
     *  resolve against it (null outside a subject-bearing form). */
    private ObjectType currentSubject;
    /** The one AttributeReference currently being emitted as the single argument of an instance fold: its
     *  relation is the fold's range ({@code sourceRole} or {@code sourcePath}), so it never carries a
     *  {@code path} (IR-SPEC ch. 4.1a; the verifier rejects one in a fold slot, WF-053).
     *  Null otherwise. */
    private Expr foldSource;
    /** The name {@link #foldSource} is written with where it differs from the record's: see
     *  {@link #countSourceName}. Null: the record's own name. */
    private String foldSourceName;

    // --- top-level model ---------------------------------------------------

    private void model(RegelSpraakModel m) {
        this.m = m;
        w.beginObject();
        w.key("format").value("uchar-ir-v1");
        // IR-SPEC ch. 2.12: the vocabulary side table, an extension list written only when the document declares a
        // vocabulary, so a document without one emits exactly what it emitted before
        if (!m.vocabularies.isEmpty()) {
            w.key("vocabularies").beginArray();
            for (RegelSpraakModel.Vocabulary v : m.vocabularies) {
                w.beginObject();
                w.key("node").value("Vocabulary");
                w.key("name").value(v.name());
                w.key("default").value(v.isDefault());
                if (v.language() != null) w.key("language").value(v.language());
                w.endObject();
            }
            w.endArray();
        }

        w.key("model").beginObject();

        w.key("objectTypes").beginArray();
        for (var entry : m.objectTypes.entrySet()) objectType(entry.getKey(), entry.getValue());
        w.endArray();

        w.key("factTypes").beginArray();
        for (var entry : m.factTypes.entrySet()) { oneName("feittype", entry.getKey(), entry.getValue().name); factType(entry.getValue()); }
        w.endArray();

        w.key("parameters").beginArray();
        for (var entry : m.parameters.entrySet()) { oneName("parameter", entry.getKey(), entry.getValue().name()); parameter(entry.getValue()); }
        w.endArray();

        w.key("domains").beginArray();
        for (var entry : m.domains.entrySet()) domain(entry.getKey(), entry.getValue());
        w.endArray();

        w.key("unitSystems").beginArray();
        for (var entry : m.unitSystems.entrySet()) { oneName("eenheidstelsel", entry.getKey(), entry.getValue().name); unitSystem(entry.getValue()); }
        w.endArray();

        w.key("dimensions").beginArray();
        for (var entry : m.dimensions.entrySet()) { oneName("dimensie", entry.getKey(), entry.getValue().name()); dimension(entry.getValue()); }
        w.endArray();

        w.key("timelines").beginArray();
        for (Timeline t : m.timelines) timeline(t);
        w.endArray();

        w.key("dayKinds"); dayKinds();

        w.endObject();

        w.key("rules").beginObject();

        // IR-SPEC ch. 2, Rules: each list holds named forms, grouped from the model's one entry per version
        // (vormen); the model's lists keep their v0 names
        w.key("rules").beginArray();
        for (Form<ResolvedRule> v : forms("rules", m.resolvedRules, ResolvedRule::name,
                IrEmitter::annotations)) rule(v);
        w.endArray();

        w.key("dayKindDefinitions").beginArray();
        for (ResolvedDayKindDefinition d : m.resolvedDayKindDefinitions) resolvedDayKindDefinition(d);
        w.endArray();

        w.key("objectCreations").beginArray();
        for (Form<ResolvedObjectCreation> v : forms("objectCreations", m.objectCreations, ResolvedObjectCreation::name,
                null)) objectCreation(v);
        w.endArray();

        // an allocation without one of the three methods has no spelling: it is left out, and
        // unwrittenAllocations names it in the diagnostics below
        w.key("allocations").beginArray();
        List<ResolvedAllocation> allocations = m.allocations.stream()
                .filter(v -> v.method() != ResolvedAllocation.Method.COMPLEX).toList();
        for (Form<ResolvedAllocation> v : forms("allocations", allocations, ResolvedAllocation::name, null))
            allocation(v);
        w.endArray();

        w.key("consistencies").beginArray();
        for (Form<ResolvedConsistency> v : forms("consistencies", m.consistencies, ResolvedConsistency::name, null))
            consistency(v);
        w.endArray();

        // IR-SPEC ch. 3.6: one list of uniquenesses, the single-source forms first and then the unions, each in model
        // order; the model keeps the two in two lists
        w.key("uniquenesses").beginArray();
        for (Form<ResolvedUniqueness> v : forms("uniquenesses", m.uniquenesses, ResolvedUniqueness::name, null))
            uniqueness(v);
        for (Form<ResolvedUniquenessUnion> v : forms("uniquenesses", m.uniquenessUnions, ResolvedUniquenessUnion::name,
                null)) uniquenessUnion(v);
        w.endArray();

        w.key("factCreations").beginArray();
        for (Form<ResolvedFactCreation> v : forms("factCreations", m.factCreations, ResolvedFactCreation::name, null))
            factCreation(v);
        w.endArray();

        w.key("startPointDeterminations").beginArray();
        for (ResolvedStartPointDetermination s : m.resolvedStartPointDeterminations) startPointDetermination(s);
        w.endArray();

        w.key("ruleGroups").beginArray();
        for (ResolvedRuleGroup g : m.ruleGroups) ruleGroup(g);
        w.endArray();

        // §4 (PRIORITIZED-DEFAULTS-EVOLUTIE) the defaults extension: an OPTIONAL list, emitted ONLY when the
        // 'Uitzondering op' / 'Uitzonderingen ongeordend' forms produced defaults groups, or the opgave marker
        // produced its group 'opgave boven berekening X' (grammar/DIVERGENCES.md §7). A flat document
        // produces none, so the key is absent there — the mirror byte-identity of every flat golden holds.
        if (!m.prioritizedDefaults.isEmpty()) {
            w.key("prioritizedDefaults").beginArray();
            for (PrioritizedDefaults p : m.prioritizedDefaults) prioritizedDefaults(p);
            w.endArray();
        }

        w.endObject();

        w.key("diagnostics").beginArray();
        for (Diagnostic d : m.diagnostics) diagnostic(d);
        for (Diagnostic d : unwrittenAllocations(m)) diagnostic(d);
        w.endArray();

        w.endObject();
    }

    /** A declaration list writes its name once, on the node: a map key that disagrees with the node's own
     *  name would lose one of the two spellings without a rule for choosing (delta B2), so it is refused. */
    private static void oneName(String category, String key, String name) {
        if (!key.equals(name))
            throw new IllegalStateException("IrEmitter: " + category + " '" + key + "' draagt de eigen naam '"
                    + name + "'; de lijstvorm heeft voor beide maar één plek");
    }

    // --- §4 prioritized defaults (extension) -------------------------------

    private void prioritizedDefaults(PrioritizedDefaults p) {
        // the path of a default's own-attribute/characteristic read resolves against the group subject
        ObjectType previousSubject = currentSubject;
        currentSubject = m == null ? null : m.objectTypes.get(p.subject());
        w.beginObject();
        w.key("node").value("PrioritizedDefaults");
        w.key("name").value(p.name());
        w.key("subject").value(p.subject());
        if (p.characteristic() != null) w.key("characteristic").value(p.characteristic());   // absent on attribute defaults
        if (p.attribute() != null) w.key("attribute").value(p.attribute());   // only on attribute defaults (WF-050)
        w.key("defaults").beginArray();
        for (Default d : p.defaults()) dflt(d);
        w.endArray();
        if (p.unordered()) w.key("unordered").value(true);         // only on a declared-unordered group member
        sourceReference(p.source());
        w.key("validity"); writeValidity(p.validity());
        originAndDesignations(RegelSpraakModel.Designatable.form(p.name()));
        w.endObject();
        currentSubject = previousSubject;
    }

    // `default` is a Java keyword, so the emit method for a Default node is spelled dflt.
    private void dflt(Default d) {
        w.beginObject();
        w.key("node").value("Default");
        // absent on an attribute default
        if (d.decision() != null) w.key("decision").value(d.decision().booleanValue());
        // absent on the initialisation default and on a group default (IR-SPEC ch. 6.1)
        if (d.condition() != null) { w.key("justification"); condition(d.condition()); }
        if (d.consequence() != null) { w.key("consequence"); expr(d.consequence()); }  // attribute defaults only
        if (d.initialisation()) w.key("initialisation").value(true);
        if (d.exceptions() != null) {
            w.key("exceptions").beginObject();
            w.key("unordered").value(d.exceptions().unordered());
            w.key("members").beginArray();
            for (ExceptionGroup g : d.exceptions().members()) exceptionGroup(g);
            w.endArray();
            w.endObject();
        }
        sourceReference(d.source());
        w.endObject();
    }

    private void exceptionGroup(ExceptionGroup g) {
        w.beginObject();
        w.key("node").value("ExceptionGroup");
        w.key("defaults").beginArray();
        for (Default d : g.defaults()) dflt(d);
        w.endArray();
        sourceReference(g.source());
        w.endObject();
    }

    // --- declaration side --------------------------------------------------

    /** IR-SPEC ch. 2.2: the model keeps its eight member-keyed side-maps; the wire carries each member's facets
     *  on the member's own node. A side-map entry the member nodes cannot carry — a key naming no member, a count
     *  or timeline name without a granularity, a null value — is a frontend defect, refused rather than dropped. */
    private void objectType(String key, ObjectType ot) {
        oneName("objecttype", key, ot.name);
        sideMapsFit(ot);
        w.beginObject();
        w.key("node").value("ObjectType");
        w.key("name").value(ot.name);
        // an extension field under the § Method's discipline — optional, never null, written WHOLE or not at
        // all, so a root type emits no key
        if (ot.supertype != null) w.key("supertype").value(ot.supertype.name);
        w.key("animate").value(ot.animate);
        if (ot.plural != null) w.key("plural").value(ot.plural);
        w.key("attributes").beginArray();
        for (var entry : ot.attributes.entrySet()) {
            String a = entry.getKey();
            w.beginObject();
            w.key("node").value("Attribute");
            w.key("name").value(a);
            w.key("datatype"); datatype(entry.getValue(), true);
            String plural = ot.attributePlural.get(a);
            if (plural != null) w.key("plural").value(plural);
            List<String> dimensions = ot.attributeDimensions.get(a);
            if (dimensions != null && !dimensions.isEmpty()) { w.key("dimensions"); stringList(dimensions); }
            if (ot.attributeGranularity.containsKey(a))
                timelineFacet(ot.attributeGranularity.get(a), ot.attributeTimelineCount.get(a),
                        ot.attributeFixedStartPoint.get(a), ot.attributeTimelineName.get(a));
            originAndDesignations(RegelSpraakModel.Designatable.attribute(ot.name, a));
            w.endObject();
        }
        w.endArray();
        w.key("characteristics").beginArray();
        for (String k : ot.characteristics) {
            w.beginObject();
            w.key("node").value("Characteristic");
            w.key("name").value(k);
            // ch. 2.2: an untagged characteristic is nominal
            ObjectType.CharacteristicKind category = ot.characteristicKinds.get(k);
            w.key("kind").value(Spelling.CHARACTERISTIC_KIND.spelling(
                    category == null ? ObjectType.CharacteristicKind.ZELFSTANDIG : category));
            if (ot.characteristicGranularity.containsKey(k))
                timelineFacet(ot.characteristicGranularity.get(k), ot.characteristicTimelineCount.get(k),
                        ot.characteristicFixedStartPoint.get(k), null);
            originAndDesignations(RegelSpraakModel.Designatable.characteristic(ot.name, k));
            w.endObject();
        }
        w.endArray();
        originAndDesignations(RegelSpraakModel.Designatable.objectType(ot.name));
        w.endObject();
    }

    /** A facet exists exactly when the member has a granularity; a member without a count is single-period, and
     *  one without a fixed start point lies on the calendar grid (IR-SPEC ch. 2.8). */
    private void timelineFacet(Granularity granularity, Integer count, String fixedStartPoint, String timelineName) {
        w.key("timeline").beginObject();
        w.key("node").value("TimelineFacet");
        w.key("granularity").value(Spelling.GRANULARITY.spelling(granularity));
        w.key("count").value(count == null ? 1 : count);
        if (fixedStartPoint != null) w.key("fixedStart").value(fixedStartPoint);
        if (timelineName != null) w.key("timelineName").value(timelineName);
        w.endObject();
    }

    private static void sideMapsFit(ObjectType ot) {
        java.util.Set<String> attributes = ot.attributes.keySet();
        java.util.Set<String> characteristics = new java.util.HashSet<>(ot.characteristics);
        sideMapFits(ot, "attribuutMeervoud", ot.attributePlural, attributes, false);
        sideMapFits(ot, "attribuutDimensies", ot.attributeDimensions, attributes, true);
        sideMapFits(ot, "attribuutGranulariteit", ot.attributeGranularity, attributes, true);
        sideMapFits(ot, "attribuutTijdlijnAantal", ot.attributeTimelineCount, ot.attributeGranularity.keySet(), true);
        sideMapFits(ot, "attribuutTijdlijnNaam", ot.attributeTimelineName, ot.attributeGranularity.keySet(), true);
        sideMapFits(ot, "attribuutVastStartpunt", ot.attributeFixedStartPoint, ot.attributeGranularity.keySet(), true);
        sideMapFits(ot, "kenmerkVastStartpunt", ot.characteristicFixedStartPoint, ot.characteristicGranularity.keySet(), true);
        sideMapFits(ot, "kenmerkSoorten", ot.characteristicKinds, characteristics, false);
        sideMapFits(ot, "kenmerkGranulariteit", ot.characteristicGranularity, characteristics, true);
        sideMapFits(ot, "kenmerkTijdlijnAantal", ot.characteristicTimelineCount, ot.characteristicGranularity.keySet(), true);
    }

    private static void sideMapFits(ObjectType ot, String map, Map<String, ?> sideMap,
                                     java.util.Set<String> carriers, boolean noNull) {
        for (var entry : sideMap.entrySet()) {
            if (!carriers.contains(entry.getKey()))
                throw new IllegalStateException("IrEmitter: " + ot.name + "." + map + " heeft een sleutel '"
                        + entry.getKey() + "' zonder lid (of zonder granulariteit) om hem te dragen");
            if (noNull && entry.getValue() == null)
                throw new IllegalStateException("IrEmitter: " + ot.name + "." + map + "['" + entry.getKey()
                        + "'] is null");
        }
    }

    /** IR-SPEC ch. 2.3: the wrapper carries the name, so the datatype at this declaration position carries no
     *  self-name; at every reference position {@code domain} stays (it is the domain reference there). */
    private void domain(String name, Datatype dt) {
        if (dt == null)
            throw new IllegalStateException("IrEmitter: domein '" + name + "' heeft geen datatype");
        w.beginObject();
        w.key("node").value("Domain");
        w.key("name").value(name);
        w.key("datatype");
        if (dt instanceof Datatype.Enumeration e) {
            oneName("domein", name, e.domain());
            w.beginObject();
            w.key("node").value("Enumeration");
            w.key("values"); stringList(e.values());
            valueDesignations(name, e.values());
            w.endObject();
        } else {
            datatype(dt, true);
        }
        w.endObject();
    }

    /** IR-SPEC ch. 2.10: a time-dependent parameter carries the timeline facet an attribute does, without a timeline
     *  name (no parameter declares one). The datatype is required: a parameter without one has no document. */
    private void parameter(Parameter p) {
        if (p.datatype() == null)
            throw new IllegalStateException("IrEmitter: parameter '" + p.name() + "' heeft geen datatype; de wire "
                    + "eist er een");
        w.beginObject();
        w.key("node").value("Parameter");
        w.key("name").value(p.name());
        w.key("datatype"); datatype(p.datatype(), true);
        if (p.granularity() != null) timelineFacet(p.granularity(), null, p.fixedStartPoint(), null);
        originAndDesignations(RegelSpraakModel.Designatable.parameter(p.name()));
        w.endObject();
    }

    /** IR-SPEC ch. 2.5: a unit system is its list of units in declaration order, each unit once with its further
     *  spellings on it. The model keeps two maps — every written abbreviation, and the unit words and plurals
     *  (normalised) — and a key of either that is not the unit's own abbreviation is one of its spellings, the map's
     *  keys before the aliases; a spelling is never the abbreviation itself. What the list cannot carry — an alias of
     *  a unit the system does not declare, one abbreviation for two different units (under the name identity too), a
     *  spelling reached twice — is a frontend defect, refused rather than dropped. */
    private void unitSystem(UnitSystem es) {
        Map<String, Unit> units = new java.util.LinkedHashMap<>();
        Map<String, List<String>> spellingsByAbbreviation = new java.util.LinkedHashMap<>();
        for (var entry : es.units.entrySet()) {
            Unit e = systemUnit(es, entry.getKey(), entry.getValue());
            Unit earlier = units.putIfAbsent(e.abbreviation(), e);
            if (earlier != null && !earlier.equals(e))
                throw new IllegalStateException("IrEmitter: eenheidstelsel '" + es.name + "' schrijft de afkorting '"
                        + e.abbreviation() + "' voor twee verschillende eenheden; de lijst draagt elke eenheid eenmaal");
            spellingsByAbbreviation.putIfAbsent(e.abbreviation(), new java.util.ArrayList<>());
            if (!entry.getKey().equals(e.abbreviation())) spellingsByAbbreviation.get(e.abbreviation()).add(entry.getKey());
        }
        for (var entry : es.spellings.entrySet()) {
            Unit e = systemUnit(es, entry.getKey(), entry.getValue());
            if (!e.equals(units.get(e.abbreviation())))
                throw new IllegalStateException("IrEmitter: eenheidstelsel '" + es.name + "' kent de spelling '"
                        + entry.getKey() + "' toe aan de eenheid '" + e.abbreviation() + "', die het niet zo declareert; een "
                        + "spelling staat alleen op een gedeclareerde eenheid");
            // a word the declaration also writes as an abbreviation ("de meter (mv: meters) m meter") is one spelling
            // of one unit, listed once: nothing is dropped
            List<String> sp = spellingsByAbbreviation.get(e.abbreviation());
            if (!entry.getKey().equals(e.abbreviation()) && !sp.contains(entry.getKey())) sp.add(entry.getKey());
        }
        // one Unit per abbreviation under the name identity (WF-009): the model keys its units on the exact
        // spelling, so two declarations whose abbreviations differ in case alone are two units here and one name there
        Map<String, String> perName = new java.util.HashMap<>();
        for (String a : units.keySet()) {
            String other = perName.putIfAbsent(norm(a), a);
            if (other != null)
                throw new IllegalStateException("IrEmitter: eenheidstelsel '" + es.name + "' declareert de eenheden '"
                        + other + "' en '" + a + "', die onder de naamidentiteit één afkorting zijn; de lijst draagt één "
                        + "eenheid per afkorting (WF-009)");
        }
        w.beginObject();
        w.key("node").value("UnitSystem");
        w.key("name").value(es.name);
        w.key("units").beginArray();
        for (Unit e : units.values()) {
            List<String> sp = spellingsByAbbreviation.get(e.abbreviation());
            if (new java.util.HashSet<>(sp).size() != sp.size())
                throw new IllegalStateException("IrEmitter: eenheidstelsel '" + es.name + "' draagt een spelling van '"
                        + e.abbreviation() + "' tweemaal, als afkorting en als alias: " + sp);
            w.beginObject();
            unitFields(e);
            if (!sp.isEmpty()) { w.key("spellings"); stringList(sp); }
            w.endObject();
        }
        w.endArray();
        w.endObject();
    }

    /** IR-SPEC ch. 2.9: a day kind is one node with its name and the plural it takes as a duration unit. The model
     *  keeps the names and a registry from every unit spelling of a day kind (lower-cased) to its name; a registry
     *  key that is the name itself (under the verifier's name identity, {@link #norm}) adds nothing, one further key
     *  is the plural, and what the node cannot carry — a third spelling, a key naming no single declared day kind —
     *  is a frontend defect, refused rather than dropped. */
    private void dayKinds() {
        List<String> names = m.dayKinds;
        List<List<String>> further = new java.util.ArrayList<>();
        for (int i = 0; i < names.size(); i++) further.add(new java.util.ArrayList<>());
        for (var entry : m.dayKindPerUnit.entrySet()) {
            String value = entry.getValue();
            List<Integer> candidates = new java.util.ArrayList<>();
            for (int i = 0; i < names.size(); i++)
                if (value != null && norm(names.get(i)).equals(norm(value))) candidates.add(i);
            if (candidates.size() != 1)
                throw new IllegalStateException("IrEmitter: de dagsoortspelling '" + entry.getKey() + "' noemt '" + value
                        + "', dat " + candidates.size() + " gedeclareerde dagsoorten aanduidt; een spelling staat op "
                        + "precies één dagsoort");
            int i = candidates.get(0);
            if (!norm(entry.getKey()).equals(norm(names.get(i)))) further.get(i).add(entry.getKey());
        }
        w.beginArray();
        for (int i = 0; i < names.size(); i++) {
            if (further.get(i).size() > 1)
                throw new IllegalStateException("IrEmitter: dagsoort '" + names.get(i) + "' heeft naast haar naam de "
                        + "spellingen " + further.get(i) + "; de wire draagt een naam en één meervoud");
            w.beginObject();
            w.key("node").value("DayKind");
            w.key("name").value(names.get(i));
            if (!further.get(i).isEmpty()) w.key("plural").value(further.get(i).get(0));
            w.endObject();
        }
        w.endArray();
    }

    private static Unit systemUnit(UnitSystem es, String key, Unit e) {
        if (e == null)
            throw new IllegalStateException("IrEmitter: eenheidstelsel '" + es.name + "' heeft onder '" + key
                    + "' geen eenheid");
        return e;
    }

    /** A unit where it occurs — on a number, as the denominator of a rate: its fields, never its spellings (those
     *  belong to the declaration in its unit system). */
    private void unit(Unit e) {
        w.beginObject();
        unitFields(e);
        w.endObject();
    }

    /** The fields of a Unit in the schema's order; an absent field is left out. The factor is the exact fraction
     *  (see {@link #exactFactor}). */
    private void unitFields(Unit e) {
        w.key("node").value("Unit");
        w.key("abbreviation").value(e.abbreviation());
        if (e.system() != null) w.key("system").value(e.system());
        if (e.canonical() != null) w.key("canonical").value(e.canonical());
        UnitSystem.Factor f = exactFactor(e);
        if (f != null) {
            w.key("factorToCanonical").beginObject();
            w.key("num").value(f.num());
            w.key("den").value(f.den());
            w.endObject();
        }
        if (e.denominator() != null) { w.key("denominator"); unit(e.denominator()); }
    }

    /** The factor of a unit as the wire writes it: an exact positive fraction in lowest terms, or null when the unit
     *  carries none (a product of units). A unit its system declares takes the fraction that declaration wrote
     *  ({@link UnitSystem#exactFactors}) — its record's decimal factor is that fraction rounded per
     *  declaration step, and the two must agree to that rounding. A unit outside every system — the built-in '%', a
     *  bare calendar unit — is its own canonical with factor 1, a whole number, written over 1. Any other factor has
     *  no exact spelling here and is refused, as is a factor that is not positive. */
    private UnitSystem.Factor exactFactor(Unit e) {
        BigDecimal f = e.factorToCanonical();
        if (f == null) return null;
        UnitSystem es = e.system() == null ? null : m.unitSystems.get(e.system());
        UnitSystem.Factor x = es == null ? null : es.exactFactors.get(e.abbreviation());
        if (x == null) {
            if (f.signum() <= 0 || f.stripTrailingZeros().scale() > 0)
                throw new IllegalStateException("IrEmitter: eenheid '" + e.rendering() + "' draagt de factor "
                        + f.toPlainString() + ", en geen eenheidstelsel declareert haar als breuk; de wire schrijft de "
                        + "factor als exacte positieve breuk");
            return new UnitSystem.Factor(f.toBigIntegerExact(), java.math.BigInteger.ONE);
        }
        if (x.num().signum() <= 0)
            throw new IllegalStateException("IrEmitter: eenheid '" + e.rendering() + "' heeft de gedeclareerde factor "
                    + x.num() + "/" + x.den() + "; de wire schrijft de factor als exacte positieve breuk");
        BigDecimal rational = new BigDecimal(x.num()).divide(new BigDecimal(x.den()), java.math.MathContext.DECIMAL128);
        if (f.subtract(rational).abs().compareTo(rational.movePointLeft(12)) > 0)
            throw new IllegalStateException("IrEmitter: eenheid '" + e.rendering() + "' draagt de factor "
                    + f.toPlainString() + ", maar haar stelsel declareert " + x.num() + "/" + x.den());
        return x;
    }

    private void factType(FactType ft) {
        w.beginObject();
        w.key("node").value("FactType");
        w.key("name").value(ft.name);
        w.key("reciprocal").value(ft.reciprocal);
        w.key("roles").beginArray();
        for (Role r : ft.roles) role(ft, r);
        w.endArray();
        originAndDesignations(RegelSpraakModel.Designatable.factType(ft.name));
        w.endObject();
    }

    private void role(FactType ft, Role r) {
        w.beginObject();
        w.key("node").value("Role");
        w.key("name").value(r.name());
        w.key("objectType").value(r.objectTypeName());
        if (r.plural() != null) w.key("plural").value(r.plural());
        w.key("many").value(r.many());
        originAndDesignations(RegelSpraakModel.Designatable.role(ft.name, r.name()));
        w.endObject();
    }

    private void dimension(Dimension d) {
        w.beginObject();
        w.key("node").value("Dimension");
        w.key("name").value(d.name());
        if (d.pluralName() != null) w.key("plural").value(d.pluralName());
        if (d.preposition() != null) w.key("preposition").value(d.preposition());
        w.key("labels"); stringList(d.values());
        w.endObject();
    }

    private void timeline(Timeline t) {
        w.beginObject();
        w.key("node").value("Timeline");
        w.key("name").value(t.name());
        w.key("variableStart").value(t.variableStartPoint());
        w.key("granularity").value(t.granularity() == null ? null
                : Spelling.GRANULARITY.spelling(t.granularity()));
        w.key("count").value(t.count());
        if (t.fixedStartPoint() != null) w.key("fixedStart").value(t.fixedStartPoint());
        w.endObject();
    }

    // --- Datatype (exhaustive) --------------------------------------------

    /** A datatype at a type annotation: the {@code type} of an expression node. */
    private void datatype(Datatype dt) { datatype(dt, false); }

    /** IR-SPEC ch. 2.4. A number is a {@code Number}, its sign always written (a number declared without one is
     *  unspecified); a percentage is a {@code Number} whose unit is '%' — the built-in one when it has none — and at a
     *  {@code declaration} position (the datatype of an attribute, a parameter or a domain) it carries the presentation
     *  tag, which a type annotation never does. A date is a {@code Date} with its precision. */
    private void datatype(Datatype dt, boolean declaration) {
        if (dt == null) { w.valueNull(); return; }
        w.beginObject();
        switch (dt) {
            case Datatype.Numeric n -> {
                w.key("node").value("Number");
                w.key("whole").value(n.wholeNumber());
                w.key("decimals").value(n.decimals());
                w.key("sign").value(Spelling.NUMBER_SIGN.spelling(
                        n.sign() == null ? Datatype.Sign.ONBEPAALD : n.sign()));
                if (n.unit() != null) { w.key("unit"); unit(n.unit()); }
            }
            case Datatype.Percentage p -> {
                Unit e = p.unit() == null ? Datatype.Percentage.PERCENT : p.unit();
                if (!"%".equals(e.abbreviation()))
                    throw new IllegalStateException("IrEmitter: een percentage met de eenheid '" + e.rendering()
                            + "'; de wire schrijft een percentage als getal met de eenheid '%'");
                w.key("node").value("Number");
                w.key("whole").value(p.wholeNumber());
                w.key("decimals").value(p.decimals());
                w.key("sign").value(Spelling.NUMBER_SIGN.spelling(Datatype.Sign.ONBEPAALD));
                w.key("unit"); unit(e);
                if (declaration) w.key("presentation").value("percentage");
            }
            case Datatype.Text t -> w.key("node").value("Text");
            case Datatype.BooleanType b -> w.key("node").value("Boolean");
            case Datatype.DateInDays d -> {
                w.key("node").value("Date");
                w.key("precision").value(Spelling.DATE_PRECISION.spelling(d));
            }
            case Datatype.DateTime d -> {
                w.key("node").value("Date");
                w.key("precision").value(Spelling.DATE_PRECISION.spelling(d));
            }
            case Datatype.Enumeration e -> {
                w.key("node").value("Enumeration");
                w.key("domain").value(e.domain());
                w.key("values"); stringList(e.values());
            }
            // the resolver's "no claim" has no wire spelling: a derived position the frontend cannot type is
            // written null (derivedType) and a marker leaves its type out, so an unknown datatype reaching
            // this arm stands at a declaration, a payload or a literal, and a document there would be refused
            // at load
            case Datatype.Unknown o -> throw unknownType(o);
        }
        w.endObject();
    }

    private static IllegalStateException unknownType(Datatype.Unknown o) {
        return new IllegalStateException("IrEmitter: een onbekend datatype ('" + o.reason() + "') heeft op deze "
                + "positie geen spelling in uchar-ir-v1; alleen een afgeleide positie schrijft null en een "
                + "Unresolved-marker laat zijn type weg");
    }

    /** IR-SPEC ch. 2.10 a DERIVED position — an arithmetic node, a fold, a rounding, a clamp — whose result
     *  the frontend could not type (a datum plus a datum, which it reports and does not refuse): the one
     *  spelling of "derived, but not typeable" is null, which the schema admits there. Not a marker: the
     *  expression is resolved. */
    private void derivedType(Datatype dt) {
        if (dt instanceof Datatype.Unknown) w.valueNull(); else datatype(dt);
    }

    /** The type a function node writes. Two builtins have a result type the spec fixes whatever their operands are,
     *  and the resolver leaves both without a claim, so their null would be refused by WF-034: the day count counts
     *  days and is typed as every count is (RUNTIME-SEMANTICS ch. 2.3 and ch. 3.8: whole, non-negative, unitless),
     *  and the concatenation joins its parts into one text (IR-SPEC ch. 4.3, the text fold). Every other kind writes
     *  the resolver's type. A resolver claim that differs from the fixed type would give one annotation two sources,
     *  so it is refused rather than overwritten. */
    private static Datatype functionType(Expr.FunctionCall f) {
        Datatype fixed = switch (f.kind()) {
            case AANTAL_DAGEN_IN -> new Datatype.Numeric(true, 0, Datatype.Sign.NIET_NEGATIEF, null);
            case CONCATENATIE -> new Datatype.Text();
            default -> null;
        };
        if (fixed == null) return f.type();
        if (f.type() != null && !(f.type() instanceof Datatype.Unknown) && !fixed.equals(f.type()))
            throw new IllegalStateException("IrEmitter: de " + category(f) + "-aanroep '" + f.source().snippet()
                    + "' draagt het type " + f.type().name() + ", en de spec legt het type " + fixed.name() + " vast");
        return fixed;
    }

    private static final java.util.regex.Pattern RATIONAL = java.util.regex.Pattern.compile("-?\\d+(_\\d+)?/\\d+");

    /** The number lexeme the wire carries for a written number (IR-SPEC ch. 4.1), and the one normalisation of this
     *  frontend: the scenario lowering writes a case number that needs no restatement with it too, so an IR document
     *  and a case file spell one written number alike. A decimal keeps what the author wrote, the decimal comma a
     *  point. A §13.2.5 fraction, a proper one ("6/8") or a mixed one ("1_1/2", "-0_1/2"), becomes its reduced
     *  rational with the sign on the numerator ("3/4", "3/2", "-1/2"), and a whole number where it reduces to one
     *  ("2_0/5" and "4/2" are "2"): the verifier, ref, clingo and the Drools loader read no mixed form, and one value
     *  has one spelling. The author's spelling stays in the source snippet. The resolver names this lexeme in its
     *  sign refusal, so both read it here. */
    public static String numberLexeme(String value) {
        if (!RATIONAL.matcher(value).matches()) return value.replace(',', '.');
        BigDecimal[] r = dev.uchar.regelspraak.model.RationalParser.of(value);
        java.math.BigInteger numerator = r[0].toBigIntegerExact(), denominator = r[1].toBigIntegerExact();
        java.math.BigInteger divisor = numerator.gcd(denominator);
        numerator = numerator.divide(divisor);
        denominator = denominator.divide(divisor);
        return denominator.equals(java.math.BigInteger.ONE) ? numerator.toString() : numerator + "/" + denominator;
    }

    /** IR-SPEC ch. 4.1: the value variant of a literal, chosen by its type — a number (a percentage included), a
     *  boolean, a text, a date of day and of millisecond precision, an enumeration label. The frontend has no sum
     *  type, so it never writes the constructor variant. Without a type there is no variant to choose, and a boolean
     *  whose lexeme is not waar or onwaar has no value; both are refused rather than guessed. */
    private static String variant(Expr.Literal l) {
        if (l.valueText() == null)
            throw new IllegalStateException("IrEmitter: de literal '" + l.source().snippet() + "' heeft geen waarde; de "
                    + "wire schrijft er altijd een");
        return switch (l.type()) {
            case null -> throw new IllegalStateException("IrEmitter: de literal '" + l.source().snippet() + "' heeft geen "
                    + "type; de wire kiest de waardevariant van een literal naar zijn type");
            case Datatype.Numeric n -> "number";
            case Datatype.Percentage p -> "number";
            case Datatype.BooleanType b -> {
                if (!l.valueText().equals("waar") && !l.valueText().equals("onwaar"))
                    throw new IllegalStateException("IrEmitter: de booleaanse literal '" + l.source().snippet()
                            + "' draagt het lexeem '" + l.valueText() + "'; alleen waar en onwaar zijn een booleaanse waarde");
                yield "boolean";
            }
            case Datatype.Text t -> "text";
            case Datatype.DateInDays d -> "date";
            case Datatype.DateTime d -> "dateTime";
            case Datatype.Enumeration e -> "enumeration";
            case Datatype.Unknown o -> throw unknownType(o);
        };
    }

    /** IR-SPEC ch. 4.3: the eight fold kinds are an Aggregation and every other kind a BuiltinCall; the table
     *  splits the one in-memory enum between the two families. */
    private static Spelling.Family<Expr.FunctionKind> kindFamily(Expr.FunctionKind k) {
        return Spelling.AGGREGATION_KIND.rows().containsKey(k) ? Spelling.AGGREGATION_KIND : Spelling.BUILTIN_KIND;
    }

    private static String category(Expr.FunctionCall f) {
        return kindFamily(f.kind()).spelling(f.kind());
    }

    /** A field on the function node of the other kind: an Aggregation has no place for a granularity, a BuiltinCall
     *  none for a fold's element type, empty value or dimension selection (IR-SPEC ch. 4.3). */
    private static IllegalStateException facetRefusal(Expr.FunctionCall f, String field, String carrier) {
        return new IllegalStateException("IrEmitter: de " + category(f) + "-aanroep '" + f.source().snippet() + "' draagt "
                + field + ", en in uchar-ir-v1 staat dat veld alleen op " + carrier);
    }

    // --- Expr (exhaustive) -------------------------------------------------

    private void expr(Expr e) {
        if (e == null) { w.valueNull(); return; }
        w.beginObject();
        switch (e) {
            // IR-SPEC ch. 4.1: the value is the variant its type names (the value union of uchar-case-v0); the
            // author's spelling stays in the source snippet
            case Expr.Literal l -> {
                String variant = variant(l);
                w.key("node").value("Literal");
                w.key("value").beginObject();
                w.key(variant);
                switch (variant) {
                    case "boolean" -> w.value(l.valueText().equals("waar"));
                    case "number" -> w.value(numberLexeme(l.valueText()));
                    default -> w.value(l.valueText());
                }
                w.endObject();
                w.key("type"); datatype(l.type());
                sourceReference(l.source().snippet());
            }
            case Expr.TextComposition t -> {
                w.key("node").value("TextComposition");
                w.key("parts").beginArray();
                for (Expr.TextComposition.Part d : t.parts()) textCompositionPart(d);
                w.endArray();
                w.key("type"); datatype(t.type());
                sourceReference(t.source().snippet());
            }
            case Expr.Binary b -> {
                w.key("node").value("Binary");
                w.key("op").value(Spelling.BINARY_OP.spelling(b.op()));
                w.key("left"); expr(b.left());
                w.key("right"); expr(b.right());
                w.key("type"); derivedType(b.type());
                sourceReference(b.source().snippet());
            }
            // IR-SPEC ch. 4.3: the eight fold kinds are an Aggregation, every other function a BuiltinCall, each
            // with the fields of its own node only
            case Expr.FunctionCall f -> {
                boolean aggregation = kindFamily(f.kind()) == Spelling.AGGREGATION_KIND;
                if (aggregation && f.granularity() != null)
                    throw facetRefusal(f, "de granulariteit " + Spelling.GRANULARITY.spelling(f.granularity()),
                            "een BuiltinCall");
                if (!aggregation) {
                    if (f.elementObjectType() != null)
                        throw facetRefusal(f, "het elementtype '" + f.elementObjectType().name + "'", "een Aggregation");
                    if (f.emptyFallback() != null) throw facetRefusal(f, "een leegwaarde", "een Aggregation");
                    if (f.dimensionSelection() != null) throw facetRefusal(f, "een dimensieselectie", "een Aggregation");
                }
                // IR-SPEC ch. 4.3 (row 28): a dimension fold ranges over the labels of the subject's own
                // dimensioned attribute, not over instances, so it has no element type and no range and its one
                // argument is an ordinary read. The resolver records the subject as its element type; written, that
                // type would demand a range the fold does not have. Any other element type would be a fold this
                // reading does not describe.
                boolean dimensionFold = aggregation && f.dimensionSelection() != null;
                if (dimensionFold && f.elementObjectType() != null && f.elementObjectType() != currentSubject)
                    throw foldRefusal(f, "haar dimensieselectie loopt over een attribuut van "
                            + f.elementObjectType().name + ", en een dimensievouw leest het eigen attribuut van "
                            + (currentSubject == null ? "het onderwerp" : currentSubject.name));
                ObjectType element = dimensionFold ? null : f.elementObjectType();
                w.key("node").value(aggregation ? "Aggregation" : "BuiltinCall");
                w.key("kind").value(category(f));
                // decided before the arguments are written: under count the source's name follows the role
                // the range binds
                FoldRange range = dimensionFold ? null : foldRange(f);
                w.key("arguments").beginArray();
                Expr previousFoldSource = foldSource;
                String previousName = foldSourceName;
                foldSource = range == null ? null : f.arguments().get(0);
                foldSourceName = range == null ? null : range.sourceName();
                for (Expr a : f.arguments()) expr(a);
                foldSource = previousFoldSource;
                foldSourceName = previousName;
                w.endArray();
                if (element != null) w.key("elementType").value(element.name);
                // the filter is a subselectie over the fold ELEMENT: an element-own read there is an own
                // read, not a navigation from the outer subject — emit the filter (and only the filter)
                // with the element as subject so such reads take the own-attribute branch and carry no
                // spurious by-pair path. Restore the outer subject afterwards.
                if (f.filter() != null) {
                    w.key("filter");
                    ObjectType previousSubject = currentSubject;
                    if (element != null) currentSubject = element;
                    condition(f.filter());
                    currentSubject = previousSubject;
                }
                if (f.emptyFallback() != null) { w.key("emptyFallback"); expr(f.emptyFallback()); }
                if (f.dimensionSelection() != null) { w.key("dimensionSelection"); dimensionSelection(f.dimensionSelection()); }
                // IR-SPEC ch. 4.3 / WF-102: the range is ONE field with two spellings. A resolved
                // multi-step range goes out as sourcePath; the one-step range stays sourceRole, and the two are
                // mutually exclusive on the wire — so the path is written INSTEAD OF the binding, never
                // beside it. Only a fold has a range: a builtin reaches here with no element type, so
                // vouwbereik gave it none.
                if (range != null && range.role() != null) { w.key("sourceRole"); roleBinding(range.role()); }
                if (range != null && !range.path().isEmpty()) {
                    w.key("sourcePath").beginArray();
                    for (RegelSpraakModel.RoleBindingRef step : range.path()) roleBinding(step);
                    w.endArray();
                }
                if (!aggregation && f.granularity() != null)
                    w.key("granularity").value(Spelling.GRANULARITY.spelling(f.granularity()));
                w.key("type"); derivedType(functionType(f));
                sourceReference(f.source().snippet());
            }
            case Expr.Rounding a -> {
                w.key("node").value("Rounding");
                w.key("base"); expr(a.base());
                w.key("mode").value(Spelling.ROUNDING_MODE.spelling(a.mode()));
                w.key("decimals").value(a.decimals());
                w.key("type"); derivedType(a.type());
                sourceReference(a.source().snippet());
            }
            case Expr.Clamp b -> {
                w.key("node").value("Clamp");
                w.key("base"); expr(b.base());
                // an absent bound is no bound on that side (RUNTIME-SEMANTICS ch. 5.11)
                if (b.minimum() != null) { w.key("minimum"); expr(b.minimum()); }
                if (b.maximum() != null) { w.key("maximum"); expr(b.maximum()); }
                w.key("type"); derivedType(b.type());
                sourceReference(b.source().snippet());
            }
            case Expr.AttributeReference r -> {
                w.key("node").value("AttributeReference");
                w.key("attribute").value(r == foldSource && foldSourceName != null ? foldSourceName : r.attribute());
                w.key("subjectType").value(nameOf(r.objectType()));
                sourceReference(r.source().snippet());
                if (r != foldSource) {
                    List<RegelSpraakModel.RoleBindingRef> nb = navigationBindings(r);
                    if (!nb.isEmpty()) {
                        w.key("path").beginArray();
                        for (RegelSpraakModel.RoleBindingRef b : nb) roleBinding(b);
                        w.endArray();
                    } else {
                        ownRead(r);
                    }
                }
            }
            case Expr.DimensionCellRead c -> {
                w.key("node").value("DimensionCellRead");
                w.key("attribute"); expr(c.attribute());
                w.key("labels"); stringList(c.labels());
                sourceReference(c.source().snippet());
            }
            case Expr.ParameterRef p -> {
                w.key("node").value("ParameterReference");
                w.key("parameter").value(p.parameter() == null ? null : p.parameter().name());
                sourceReference(p.source().snippet());
            }
            case Expr.VariableRef v -> {
                w.key("node").value("VariableReference");
                w.key("name").value(v.name());
                sourceReference(v.source().snippet());
            }
            // ch. 4.10 the Let cluster. Key ORDER is the schema's required order (node, type, binder,
            // value, body, source / node, name, type, source) — gate 2 round-trips these bytes.
            case Expr.Let l -> {
                w.key("node").value("Let");
                w.key("type"); datatype(l.type());
                w.key("binder").value(l.binder());
                w.key("value"); expr(l.value());
                w.key("body"); expr(l.body());
                sourceReference(l.source().snippet());
            }
            case Expr.BinderRef b -> {
                w.key("node").value("BinderRef");
                w.key("name").value(b.name());
                sourceReference(b.source().snippet());
            }
            // IR-SPEC ch. 4.1: the bound day of a day-kind definition is a node of its own and carries no name; a
            // session value is a ContextValue under one of its two names. Any other name has no spelling.
            case Expr.Constant c -> {
                if (c.name() == null
                        || !c.name().equals("de dag") && !Spelling.CONTEXT_VALUE_NAME.rows().containsKey(c.name()))
                    throw new IllegalStateException("IrEmitter: de constante '" + c.name() + "' ('" + c.source().snippet()
                            + "') heeft geen spelling in uchar-ir-v1: de wire kent de gebonden dag van een "
                            + "dagsoortdefinitie en de contextwaarden " + Spelling.CONTEXT_VALUE_NAME.rows().values());
                if (c.type() == null)
                    throw new IllegalStateException("IrEmitter: de constante '" + c.name() + "' ('" + c.source().snippet()
                            + "') heeft geen type; de wire eist er een");
                if (c.name().equals("de dag")) {
                    w.key("node").value("BoundDay");
                } else {
                    w.key("node").value("ContextValue");
                    w.key("name").value(Spelling.CONTEXT_VALUE_NAME.spelling(c.name()));
                }
                w.key("type"); datatype(c.type());
                sourceReference(c.source().snippet());
            }
            // ch. 4.10's third cluster node is NEVER EMITTER-PRODUCED: no RegelSpraak construct lowers to an
            // IfThenElse, so the resolver builds none and this arm is unreachable from every emitter path.
            // It exists because the union is sealed and exhaustive here — and it REFUSES rather than
            // serialising: an emit path nothing exercises could carry a wrong key order or a wrong field name
            // (the wire spells them condition/then/else) and no gate would ever read those bytes. A loud
            // refusal cannot be silently wrong; a dead serialiser can.
            case Expr.IfThenElse ignored -> throw new UnsupportedOperationException(
                    "IfThenElse is never emitter-produced (IR-SPEC ch. 4.10): the resolver builds none, "
                            + "so this emitter has no verified wire shape for it");
            // the §5.7 concatenation as an instance set (IR-SPEC ch. 4.3, WF-110). No "type" key:
            // the node denotes INSTANCES and carries no datatype, which is why the verifier's type
            // prologue exempts it the way it exempts a Lambda. Each selection's filter is emitted
            // with the SELECTION's element type as the subject, exactly as an aggregation's filter is
            // emitted under its elementType — the condition evaluates per contributor.
            case Expr.InstanceSetUnion u -> {
                w.key("node").value("InstanceSetUnion");
                w.key("branches").beginArray();
                for (Expr.InstanceSetUnion.Branch t : u.branches()) {
                    w.beginObject();
                    w.key("node").value("InstanceSetBranch");
                    w.key("elementType").value(nameOf(t.elementType()));
                    if (t.sourceRole() == null)
                        throw new IllegalStateException("IrEmitter: een selectie van de unie '" + u.source().snippet()
                                + "' zonder haar relatie; de wire eist die (WF-110)");
                    w.key("sourceRole"); roleBinding(t.sourceRole());
                    w.key("filter");
                    ObjectType previousSubject = currentSubject;
                    if (t.elementType() != null) currentSubject = t.elementType();
                    condition(t.filter());
                    currentSubject = previousSubject;
                    sourceReference(t.source().snippet());
                    w.endObject();
                }
                w.endArray();
                sourceReference(u.source().snippet());
            }
            case Expr.Unresolved o -> {
                w.key("node").value("Unresolved");
                // IR-SPEC ch. 4.4: absent is the one spelling of "type unknown"; a real datatype is written
                if (o.type() != null && !(o.type() instanceof Datatype.Unknown)) { w.key("type"); datatype(o.type()); }
                sourceReference(markerSource(o.source()));
            }
        }
        w.endObject();
    }

    private void textCompositionPart(Expr.TextComposition.Part d) {
        w.beginObject();
        switch (d) {
            case Expr.TextComposition.LiteralChunk l -> {
                w.key("node").value("LiteralChunk");
                w.key("text").value(l.text());
            }
            case Expr.TextComposition.EmbeddedExpression i -> {
                w.key("node").value("EmbeddedExpression");
                w.key("expr"); expr(i.expr());
            }
        }
        w.endObject();
    }

    private void dimensionSelection(Expr.DimensionSelection ds) {
        if (ds == null) { w.valueNull(); return; }
        w.beginObject();
        w.key("node").value("DimensionSelection");
        w.key("dimension").value(ds.dimension());
        w.key("labels"); stringList(ds.labels());
        sourceReference(ds.source().snippet());
        w.endObject();
    }

    // --- Conditie (exhaustive) --------------------------------------------

    private void condition(Condition c) {
        if (c == null) { w.valueNull(); return; }
        w.beginObject();
        switch (c) {
            case Condition.Comparison v -> {
                w.key("node").value("Comparison");
                w.key("op").value(Spelling.COMPARISON_OP.spelling(v.op()));
                w.key("left"); expr(v.left());
                w.key("right"); expr(v.right());
                wholePeriod(v.period());
                sourceReference(v.source().snippet());
            }
            case Condition.Predicate p -> {
                w.key("node").value("Predicate");
                w.key("kind").value(Spelling.PREDICATE_KIND.spelling(p.kind()));
                w.key("operand"); expr(p.operand());
                if (p.dayKind() != null) w.key("dayKind").value(p.dayKind());
                w.key("negated").value(p.negation());
                w.key("digits").value(p.digits());
                wholePeriod(p.period());
                sourceReference(p.source().snippet());
            }
            case Condition.CharacteristicCheck k -> {
                w.key("node").value("CharacteristicCheck");
                w.key("characteristic").value(k.characteristic());
                w.key("negated").value(k.negation());
                // absent is the subject (RUNTIME-SEMANTICS ch. 5.8b)
                if (k.owner() != null) w.key("host").value(k.owner().name);
                wholePeriod(k.period());
                sourceReference(k.source().snippet());
                // the binding IS the hop: a navigated check without one would read the subject's own kenmerk
                if (k.navigation() != null) {
                    RegelSpraakModel.RoleBindingRef nb = navigationBinding(k);
                    if (nb == null)
                        throw new IllegalStateException("IrEmitter: de kenmerkcheck '" + k.source().snippet()
                                + "' navigeert via '" + k.navigation() + "', en vanaf "
                                + (currentSubject == null ? "geen onderwerp" : "'" + currentSubject.name + "'")
                                + " bindt die stap geen unieke rol");
                    w.key("navigation"); roleBinding(nb);
                }
            }
            // IR-SPEC ch. 4.5: one node for both sides of the role check, naming the fact type, the role the
            // subject plays in it and, where the check narrows it, the type of the other end; where the
            // reference sits is the consumer's storage question, not the wire's
            case Condition.RoleMembership r -> roleMembership(nearMembership(r), r.negation(), r.source());
            case Condition.FarRoleMembership r -> roleMembership(farMembership(r), r.negation(), r.source());
            case Condition.TimeWindow p -> {
                w.key("node").value("TimeWindow");
                // an absent bound is an open side
                if (p.from() != null) { w.key("from"); expr(p.from()); }
                if (p.until() != null) { w.key("to"); expr(p.until()); }
                w.key("toInclusive").value(p.toInclusive());
                sourceReference(p.source().snippet());
            }
            case Condition.Compound s -> {
                w.key("node").value("Compound");
                w.key("quantifier").value(Spelling.QUANTIFIER.spelling(s.quantifier()));
                w.key("threshold").value(s.threshold());
                w.key("parts").beginArray();
                for (Condition o : s.components()) condition(o);
                w.endArray();
                sourceReference(s.source().snippet());
            }
            // IR-SPEC ch. 4.5 / WF-108. Key ORDER is the schema's property order (node, quantifier, the
            // counting threshold, elementType, filter, test, source, then the range) — gate 2
            // round-trips these bytes.
            case Condition.QuantifierFold k -> {
                w.key("node").value("QuantifierFold");
                w.key("quantifier").value(Spelling.QUANTIFIER.spelling(k.quantifier()));
                // WF-108 (e): the threshold is written EXACTLY under a counting quantifier and omitted
                // under all / none — unlike Compound, whose threshold is written under every quantifier
                // and read only under the three counting ones, so that under all / none / the it is a
                // number no reader consults (0 in the goldens, except the 1 a gedurende clip's compound
                // carries). Emitting it here too would put a number on the wire the quantifier contradicts.
                if (k.quantifier() == Condition.Quantifier.TEN_MINSTE || k.quantifier() == Condition.Quantifier.TEN_HOOGSTE
                        || k.quantifier() == Condition.Quantifier.PRECIES)
                    w.key("threshold").value(k.threshold());
                w.key("elementType").value(nameOf(k.elementObjectType()));
                // filter and test are evaluated PER CONTRIBUTOR with the element as subject, exactly as
                // FunctionCall.filter is: an element-own read there is an own read, not a navigation from the
                // outer subject, so emit both with the element as subject and they take the own-attribute
                // branch and carry no spurious by-pair path. Restore the outer subject after.
                ObjectType previousSubject = currentSubject;
                if (k.elementObjectType() != null) currentSubject = k.elementObjectType();
                // an absent filter is no subselection
                if (k.filter() != null) { w.key("filter"); condition(k.filter()); }
                w.key("test"); condition(k.test());
                currentSubject = previousSubject;
                sourceReference(k.source().snippet());
                // the range is ONE field with two spellings, mutually exclusive, and required: without it the
                // fold would range over nothing the sentence named (WF-102, WF-108 (c))
                if (k.sourcePath() != null && !k.sourcePath().isEmpty()) {
                    w.key("sourcePath").beginArray();
                    for (RegelSpraakModel.RoleBindingRef step : k.sourcePath()) roleBinding(step);
                    w.endArray();
                } else if (k.sourceRole() != null) {
                    w.key("sourceRole"); roleBinding(k.sourceRole());
                } else {
                    throw new IllegalStateException("IrEmitter: de kwantorvouw '" + k.source().snippet()
                            + "' heeft geen bereik; de resolver vond geen unieke relatie naar "
                            + nameOf(k.elementObjectType()));
                }
            }
            case Condition.RuleStatusCheck r -> {
                w.key("node").value("Regelversie");
                w.key("regel").value(r.rule());
                w.key("inconsistent").value(r.inconsistent());
                sourceReference(r.source().snippet());
            }
            case Condition.Unresolved o -> {
                w.key("node").value("Unresolved");
                sourceReference(markerSource(o.source()));
            }
        }
        w.endObject();
    }

    /** WF-113 (b): a marker is accounted for by a warning that quotes its source snippet verbatim. A marker without
     *  one is quoted by no message and a blank one by every message, so neither accounts for anything and the
     *  verifier refuses the document; the frontend can build one, since {@code DefinitionResolver.source} gives an
     *  empty snippet for a missing context or an empty match. Each is refused in the verifier's sentence for its
     *  arm; an empty snippet is no snippet at all, and {@link #sourceReference} refuses it in the readers' own sentence. */
    private static String markerSource(Source b) {
        String s = b == null ? null : b.snippet();
        if (s == null)
            throw new IllegalStateException("IrEmitter: WF-113 (b) — Unresolved marker without a source: no diagnostic "
                    + "message can contain it, so it cannot be accounted for");
        if (!s.isEmpty() && s.isBlank())
            throw new IllegalStateException("IrEmitter: WF-113 (b) — Unresolved marker whose source snippet is blank: "
                    + "blank counts as absent, since nearly every message contains it and a message about another "
                    + "marker would account for this one");
        return s;
    }

    /** IR-SPEC ch. 4 (B26): the source record, at the place the node's source text stands. A node without source
     *  text carries no record; an empty text is refused and not dropped, since a snippet is never empty and a
     *  document that lost the record would read as a node without source text. The refusal is the sentence every
     *  reader gives for an empty snippet at load. */
    private void sourceReference(String snippet) {
        sourceReference(snippet, 0);
    }

    /** As {@link #sourceReference(String)}, with the source line after the snippet — called by the consistency version
     *  and the rule group alone, the two places the model keeps a line. The model's 0 is "no line" (a synthetic
     *  node) and is left out, as the record admits only a line of at least 1. */
    private void sourceReference(String snippet, int line) {
        if (snippet == null) return;
        if (snippet.isEmpty())
            throw new IllegalStateException("IrEmitter: a source snippet is never empty: a node without source text "
                    + "carries no source");
        if (line < 0)   // the readers refuse it in this sentence (the loaders' source-line refusal)
            throw new IllegalStateException("IrEmitter: a source line is at least 1");
        w.key("source").beginObject();
        w.key("snippet").value(snippet);
        if (line > 0) w.key("line").value(line);
        w.endObject();
    }

    /** The whole-period qualifier of a condition, under its key; absent where the condition has none. */
    private void wholePeriod(Condition.WholePeriod gp) {
        if (gp == null) return;
        w.key("wholePeriod").beginObject();
        w.key("node").value("WholePeriod");
        w.key("granularity").value(Spelling.WHOLE_PERIOD_GRANULARITY.spelling(gp.year()));
        w.key("negated").value(gp.negated());
        w.endObject();
    }

    // --- resolved IR lists -------------------------------------------------

    // --- the named forms (IR-SPEC ch. 3, Versioning) -------------------------

    /** One named form on the wire: the name it is written under and its versions, in version order. */
    private record Form<T>(String name, List<T> versions) {}

    /** The in-memory spelling of one version of a form with several: the form's name, a space, '#' and the version
     *  number (RuleChecker names each version so). */
    private static final java.util.regex.Pattern VERSION = java.util.regex.Pattern.compile("([\\s\\S]*) #([0-9]+)");

    /** The form name of an entry whose name carries a version suffix, or null when it carries none. */
    private static String base(String name) {
        if (name == null) return null;
        java.util.regex.Matcher mt = VERSION.matcher(name);
        return mt.matches() ? mt.group(1) : null;
    }

    private static String versionNumber(String name) {
        java.util.regex.Matcher mt = VERSION.matcher(name);
        if (!mt.matches()) throw new IllegalStateException("IrEmitter: '" + name + "' draagt geen versieachtervoegsel");
        return mt.group(2);
    }

    /**
     * The entries of one model list as the named forms of the wire. The model holds one entry per version and names
     * the versions of a form with several {@code "<name> #<i>"}; the wire writes the form once, with its versions in
     * the order of i. Grouped are only entries whose names carry the suffix and are byte-equal without it; an entry
     * without the suffix is a form with one version, also beside an entry with the same name (WF-020 then speaks). A
     * group that is not exactly #1 to #n, whose entries do not stand together, that has one entry, whose form name
     * itself ends in a version suffix, or whose versions carry different statute annotations has no spelling on the
     * wire and is refused: a reader would take such a name for a version of another form.
     */
    private static <T> List<Form<T>> forms(String list, List<T> input, java.util.function.Function<T, String> nameOf,
                                            java.util.function.Function<T, List<String>> annotations) {
        // the runs: an entry without the suffix is a run of its own, the suffixed entries of one form name are one
        // run, and a form name has one run — asked of all runs before any run is written, as m4.py asks it
        List<List<T>> runs = new java.util.ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        int k = 0;
        while (k < input.size()) {
            String base = base(nameOf.apply(input.get(k)));
            int j = k + 1;
            while (base != null && j < input.size() && base.equals(base(nameOf.apply(input.get(j))))) j++;
            if (base != null && !seen.add(base))
                throw versionRefusal(list, base, "haar versies staan niet aaneengesloten");
            runs.add(input.subList(k, j));
            k = j;
        }
        List<Form<T>> result = new java.util.ArrayList<>();
        for (List<T> sequence : runs) {
            String name = nameOf.apply(sequence.get(0));
            String base = base(name);
            if (base == null) {
                result.add(new Form<>(name, sequence));
                continue;
            }
            if (sequence.size() < 2)
                throw versionRefusal(list, base, "het versieachtervoegsel van '" + name + "' staat op één invoer, "
                        + "en een vorm met één versie draagt haar kale naam");
            List<String> numbers = sequence.stream().map(x -> versionNumber(nameOf.apply(x))).sorted().toList();
            List<String> expected = java.util.stream.IntStream.rangeClosed(1, sequence.size())
                    .mapToObj(Integer::toString).sorted().toList();
            if (!numbers.equals(expected))
                throw versionRefusal(list, base, "de versieachtervoegsels zijn " + numbers + ", niet precies 1 tot "
                        + "en met " + sequence.size());
            if (base(base) != null)
                throw versionRefusal(list, base, "de vormnaam eindigt zelf op een versieachtervoegsel");
            if (annotations != null)
                for (T x : sequence)
                    if (!annotations.apply(x).equals(annotations.apply(sequence.get(0))))
                        throw versionRefusal(list, base, "de versies dragen verschillende statuutannotaties, en de "
                                + "wire schrijft één lijst op de vorm");
            List<T> versions = new java.util.ArrayList<>(sequence);
            versions.sort(java.util.Comparator.comparingInt(x -> Integer.parseInt(versionNumber(nameOf.apply(x)))));
            result.add(new Form<>(base, versions));
        }
        return result;
    }

    private static IllegalStateException versionRefusal(String list, String base, String reason) {
        return new IllegalStateException("IrEmitter: de vorm '" + base + "' in " + list + " heeft geen spelling in "
                + "uchar-ir-v1: " + reason);
    }

    /** The statute annotations of a rule version as the wire writes them (WF-082): an empty annotation carries no
     *  citation and is dropped. */
    private static List<String> annotations(ResolvedRule r) {
        return r.statuteAnnotations().stream().filter(x -> !x.isEmpty()).toList();
    }

    /** IR-SPEC ch. 3.10: every version writes its validity; always is a Validity with neither bound. */
    private void validity(Validity g) {
        writeValidity(g == null ? Validity.ALTIJD : g);
    }

    /** IR-SPEC ch. 3.1: the rule and its versions. The extension field statuteAnnotations (WF-082) stands on the
     *  form, written whole or not at all: a rule without annotations leaves no trace on the wire. */
    private void rule(Form<ResolvedRule> form) {
        w.beginObject();
        w.key("node").value("Rule");
        w.key("name").value(form.name());
        w.key("versions").beginArray();
        for (ResolvedRule r : form.versions()) ruleVersion(r);
        w.endArray();
        List<String> sources = annotations(form.versions().get(0));
        if (!sources.isEmpty()) { w.key("statuteAnnotations"); stringList(sources); }
        originAndDesignations(RegelSpraakModel.Designatable.form(form.name()));
        w.endObject();
    }

    /** A rule version writes its kind as its structure: a version with a presentation tag is a gelijkstelling or a
     *  berekening, an initialisation is initialisation true without a tag, and a version with neither writes a
     *  characteristic — which member its target names is the declaration's (IR-SPEC ch. 3.1). The target is an
     *  attribute reference, and a name that is both an attribute and a characteristic of the subject type decides
     *  no member, so such a target is refused (WF-022). */
    private void ruleVersion(ResolvedRule r) {
        if (r.kind() == null)
            throw new IllegalStateException("IrEmitter: regel '" + r.name() + "' heeft geen soort; de wire schrijft "
                    + "een versie als initialisatie of niet, met of zonder presentatie");
        if (!(r.target() instanceof Expr.AttributeReference d))
            throw new IllegalStateException("IrEmitter: het doel van regel '" + r.name() + "' is "
                    + (r.target() == null ? "afwezig" : "een " + r.target().getClass().getSimpleName())
                    + "; de wire schrijft als doel een attribuutreferentie");
        if (r.value() == null)
            throw new IllegalStateException("IrEmitter: regel '" + r.name() + "' heeft geen waarde; de wire eist er een");
        if (d.objectType() != null && d.attribute() != null && isAttribute(d.objectType(), d.attribute())
                && isCharacteristic(d.objectType(), d.attribute()))
            throw new IllegalStateException("IrEmitter: het doel '" + d.attribute() + "' van regel '" + r.name()
                    + "' noemt op '" + d.objectType().name + "' zowel een attribuut als een kenmerk; welk lid een "
                    + "regel schrijft bepaalt haar soort, en deze naam bepaalt niets (WF-022)");
        currentSubject = d.objectType();
        // the target fixes the subject, so a navigation written on it has no carrier
        if (d.navigationRole() != null || (d.navigationPath() != null && !d.navigationPath().isEmpty()))
            throw new IllegalStateException("IrEmitter: het doel van regel '" + r.name() + "' ('"
                    + d.source().snippet() + "') navigeert; het doel legt het onderwerp vast");
        w.beginObject();
        w.key("validity"); validity(r.validity());
        w.key("target"); expr(d);
        w.key("value"); expr(r.value());
        if (r.condition() != null) { w.key("condition"); condition(r.condition()); }
        w.key("initialisation").value(r.kind() == ResolvedRule.Kind.INITIALISATIE);
        if (r.kind() == ResolvedRule.Kind.GELIJKSTELLING || r.kind() == ResolvedRule.Kind.BEREKENING)
            w.key("presentation").value(Spelling.PRESENTATION.spelling(r.kind()));
        w.endObject();
        currentSubject = null;
    }

    /** Whether {@code name} is an attribute of {@code ot}, own or inherited, under the name identity (WF-001). */
    private static boolean isAttribute(ObjectType ot, String name) {
        for (ObjectType t : ot.chain())
            for (String a : t.attributes.keySet()) if (norm(a).equals(norm(name))) return true;
        return false;
    }

    /** Whether {@code name} is a characteristic of {@code ot}, own or inherited, under the name identity (WF-001). */
    private static boolean isCharacteristic(ObjectType ot, String name) {
        for (ObjectType t : ot.chain())
            for (String k : t.characteristics) if (norm(k).equals(norm(name))) return true;
        return false;
    }

    private void resolvedDayKindDefinition(ResolvedDayKindDefinition d) {
        w.beginObject();
        w.key("node").value("DayKindDefinition");
        w.key("dayKind").value(d.name());
        w.key("condition"); condition(d.condition());
        w.key("validity"); writeValidity(d.validity());
        w.endObject();
    }

    /** IR-SPEC ch. 3.3: an object creation and its versions. A version whose sentence names the created object by a
     *  role writes that role as its back-link, also where the type pair would derive it; a version whose sentence
     *  names the type writes none, and absent is the back-link derived from the type pair (RUNTIME-SEMANTICS
     *  ch. 5.5). */
    private void objectCreation(Form<ResolvedObjectCreation> form) {
        w.beginObject();
        w.key("node").value("ObjectCreation");
        w.key("name").value(form.name());
        w.key("versions").beginArray();
        for (ResolvedObjectCreation oc : form.versions()) objectCreationVersion(oc);
        w.endArray();
        originAndDesignations(RegelSpraakModel.Designatable.form(form.name()));
        w.endObject();
    }

    private void objectCreationVersion(ResolvedObjectCreation oc) {
        currentSubject = oc.subject();   // member values and the guard navigate from the creation subject
        w.beginObject();
        w.key("createdType").value(nameOf(oc.createdType()));
        w.key("subject").value(nameOf(oc.subject()));
        w.key("initialisations").beginArray();
        for (ResolvedObjectCreation.MemberInit t : oc.initialisations()) {
            w.beginObject();
            w.key("node").value("MemberInit");
            w.key("member").value(t.attribute());
            w.key("value"); expr(t.value());
            w.key("characteristic").value(t.characteristic());
            w.endObject();
        }
        w.endArray();
        if (oc.condition() != null) { w.key("condition"); condition(oc.condition()); }
        w.key("validity"); validity(oc.validity());
        if (oc.connectingRole() != null) { w.key("connectingRole"); roleBinding(oc.connectingRole()); }
        w.endObject();
        currentSubject = null;
    }

    /** IR-SPEC ch. 3.2: an allocation and its versions. */
    private void allocation(Form<ResolvedAllocation> form) {
        w.beginObject();
        w.key("node").value("Allocation");
        w.key("name").value(form.name());
        w.key("versions").beginArray();
        for (ResolvedAllocation v : form.versions()) allocationVersion(v);
        w.endArray();
        originAndDesignations(RegelSpraakModel.Designatable.form(form.name()));
        w.endObject();
    }

    /** An optional field left out when the model holds none; the method is one of the three (the model's
     *  placeholder never reaches here, see {@link #unwrittenAllocations}). */
    private void allocationVersion(ResolvedAllocation v) {
        if (v.method() == null)
            throw new IllegalStateException("IrEmitter: de verdeling '" + v.name() + "' heeft geen methode");
        // IR-SPEC ch. 3.2: the recipient join is the binding; a verdeling without it has no join to execute. The
        // resolver binds it (the written role, else the one relation) and refuses what binds nothing, so the
        // refusal below guards a model built by hand
        RegelSpraakModel.RoleBindingRef rb = v.connectingRole() != null ? v.connectingRole()
                : m.connectingRoleBinding(v.sourceType(), v.targetType());
        if (rb == null)
            throw new IllegalStateException("IrEmitter: de verdeling '" + v.name() + "' heeft geen unieke relatie "
                    + "tussen " + nameOf(v.sourceType()) + " en " + nameOf(v.targetType()));
        currentSubject = v.sourceType();
        w.beginObject();
        w.key("sourceType").value(nameOf(v.sourceType()));
        w.key("sourceAttribute").value(v.sourceAttribute());
        w.key("targetType").value(nameOf(v.targetType()));
        w.key("targetAttribute").value(v.targetAttribute());
        w.key("method").value(Spelling.METHOD.spelling(v.method()));
        if (v.ratioAttribute() != null) w.key("ratioAttribute").value(v.ratioAttribute());
        if (v.maximumAttribute() != null) w.key("maximumAttribute").value(v.maximumAttribute());
        if (v.roundingDecimals() != null) w.key("roundingDecimals").value(v.roundingDecimals().intValue());
        if (v.remainderAttribute() != null) w.key("remainderAttribute").value(v.remainderAttribute());
        if (v.orderingAttribute() != null) w.key("orderingAttribute").value(v.orderingAttribute());
        w.key("descending").value(v.descending());
        if (v.condition() != null) { w.key("condition"); condition(v.condition()); }
        w.key("validity"); validity(v.validity());
        w.key("connectingRole"); roleBinding(rb);
        w.endObject();
        currentSubject = null;
    }

    /** IR-SPEC ch. 3.5: a consistency rule and its versions; the source record stands on the version, with the
     *  line. */
    private void consistency(Form<ResolvedConsistency> form) {
        w.beginObject();
        w.key("node").value("Consistency");
        w.key("name").value(form.name());
        w.key("versions").beginArray();
        for (ResolvedConsistency c : form.versions()) consistencyVersion(c);
        w.endArray();
        originAndDesignations(RegelSpraakModel.Designatable.form(form.name()));
        w.endObject();
    }

    private void consistencyVersion(ResolvedConsistency c) {
        if (c.assertion() == null)
            throw new IllegalStateException("IrEmitter: de consistentieregel '" + c.name() + "' heeft geen assertie; "
                    + "de wire eist er een");
        currentSubject = c.subject();
        w.beginObject();
        w.key("subject").value(nameOf(c.subject()));
        w.key("assertion"); condition(c.assertion());
        w.key("validity"); validity(c.validity());
        sourceReference(c.source().snippet(), c.line());
        w.endObject();
        currentSubject = null;
    }

    /** IR-SPEC ch. 3.6: a uniqueness and its versions; a version is its list of sources, one for the single-type
     *  form, and the source record stands on the version. */
    private void uniqueness(Form<ResolvedUniqueness> form) {
        w.beginObject();
        w.key("node").value("Uniqueness");
        w.key("name").value(form.name());
        w.key("versions").beginArray();
        for (ResolvedUniqueness u : form.versions()) {
            // IR-SPEC ch. 3.6: the binding IS the scoping, so a scope the resolver did not bind cannot be written
            // without widening the uniqueness to every instance
            if (u.scopeBinding() == null && u.scopeField() != null)
                throw new IllegalStateException("IrEmitter: de uniciteit '" + u.name() + "' is begrensd tot '"
                        + u.scopeField() + "', en de resolver vond daarvoor geen unieke rol");
            w.beginObject();
            w.key("sources").beginArray();
            source(u.name(), nameOf(u.subject()), u.keys());
            w.endArray();
            if (u.scopeBinding() != null) { w.key("scope"); roleBinding(u.scopeBinding()); }
            w.key("validity"); validity(u.validity());
            sourceReference(u.source().snippet());
            w.endObject();
        }
        w.endArray();
        originAndDesignations(RegelSpraakModel.Designatable.form(form.name()));
        w.endObject();
    }

    /** The union form of IR-SPEC ch. 3.6: a version with one source per side. It carries no scope (a scoped union is
     *  an open design question), and the model's union record has none. */
    private void uniquenessUnion(Form<ResolvedUniquenessUnion> form) {
        w.beginObject();
        w.key("node").value("Uniqueness");
        w.key("name").value(form.name());
        w.key("versions").beginArray();
        for (ResolvedUniquenessUnion u : form.versions()) {
            if (u.sources() == null || u.sources().isEmpty())
                throw new IllegalStateException("IrEmitter: de uniciteit '" + u.name() + "' heeft geen bronnen; een "
                        + "uniciteit heeft er minstens één");
            w.beginObject();
            w.key("sources").beginArray();
            for (ResolvedUniquenessUnion.Source b : u.sources()) source(u.name(), nameOf(b.type()), b.keys());
            w.endArray();
            w.key("validity"); validity(u.validity());
            sourceReference(u.source().snippet());
            w.endObject();
        }
        w.endArray();
        originAndDesignations(RegelSpraakModel.Designatable.form(form.name()));
        w.endObject();
    }

    /** One source of a uniqueness: the object type and its key attributes, at least one. */
    private void source(String uniqueness, String objectType, List<String> keys) {
        if (keys == null || keys.isEmpty())
            throw new IllegalStateException("IrEmitter: een bron van de uniciteit '" + uniqueness + "' heeft geen "
                    + "sleutels; een bron heeft er minstens één");
        w.beginObject();
        w.key("node").value("Source");
        w.key("objectType").value(objectType);
        w.key("keys"); stringList(keys);
        w.endObject();
    }

    /** The range of an instance fold: a one-step {@code role} or a multi-step {@code path} (never both), and
     *  the name the single source is written with ({@code sourceName}, null for the record's own). A fold
     *  whose source is the subject's own attribute has neither. */
    private record FoldRange(RegelSpraakModel.RoleBindingRef role, List<RegelSpraakModel.RoleBindingRef> path,
                              String sourceName) {}

    /**
     * The range of a fold over one source (IR-SPEC ch. 4.3), or null when the fold has no such source —
     * no element type, several operands, or an operand that is no read — and its arguments are ordinary
     * reads.
     *
     * <p>A source that denotes instances — a set reading, or the count source that names a role (the
     * record types it unknown, since a role has no datatype) — is read through the range, so the range is
     * written for it or the model is refused: without one the reader takes the argument for the subject's
     * own attribute and folds something the sentence did not name. The derivation is the resolver's own:
     * the multi-step range it bound off the written chain; else, for a set reading, the type pair and then
     * the written name; for the count source, the written name and then the type pair. The source itself
     * carries no step: a written chain has no place beside a range, and the one written step that may
     * stand is the count source naming the fold subject itself ("de vondsten van de aanmelder", read on
     * the aanmelder), which reads nothing beyond the subject.
     *
     * <p>A source that denotes the subject's own attribute has no range. That is a fold under TOTAAL (the
     * total over the subject's own timeline); a fold of another kind over it has no spelling, and neither has a
     * source of another type that denotes no instances, since written without a range it reads the subject.
     */
    private FoldRange foldRange(Expr.FunctionCall f) {
        if (!isFoldSource(f)) return null;
        Expr.AttributeReference arg = (Expr.AttributeReference) f.arguments().get(0);
        FoldRefusal refusal = foldRefusalOf(m, currentSubject, f);
        if (refusal != null) throw foldRefusal(f, switch (refusal) {
            case WRITTEN_PATH -> "haar bron draagt een geschreven pad; de relatie van een vouwbron is het bereik";
            case WRITTEN_STEP -> "haar bron draagt de geschreven stap '" + arg.navigationRole()
                    + "', en alleen een telbron die het onderwerp zelf aanduidt mag er een dragen";
            case FOREIGN_ATTRIBUTE -> "haar bron leest '" + arg.attribute() + "' op " + nameOf(arg.objectType())
                    + ", en zonder bereik leest een vouw het eigen attribuut van " + currentSubject.name;
            case OWN_ATTRIBUTE -> "haar bron is het eigen attribuut van het onderwerp en geen instantiebron";
            case NO_SUBJECT -> "er is geen onderwerp om haar bereik vanaf te bepalen";
            case NO_RELATION -> "geen unieke relatie van " + currentSubject.name + " naar "
                    + f.elementObjectType().name + " draagt haar bereik";
        });
        // the total over the subject's own timeline has no range
        if (!(arg.type() instanceof Datatype.Unknown) && !arg.set()) return new FoldRange(null, List.of(), null);
        List<RegelSpraakModel.RoleBindingRef> steps = arg.set() && arg.rangePath() != null ? arg.rangePath() : List.of();
        if (!steps.isEmpty())
            return new FoldRange(null, steps, countSourceName(f, arg, steps.get(steps.size() - 1)));
        RegelSpraakModel.RoleBindingRef rb = rangeRole(m, currentSubject, f, arg);
        return new FoldRange(rb, List.of(), countSourceName(f, arg, rb));
    }

    /** Why the wire has no spelling for a fold over one source, as {@link #foldRefusalOf} finds it. */
    public enum FoldRefusal {
        /** the source carries a written chain, where a fold source's relation is the fold's range */
        WRITTEN_PATH,
        /** the source carries a written step and is no count source that names the subject itself */
        WRITTEN_STEP,
        /** the source reads another type than the subject's, and no range carries that step */
        FOREIGN_ATTRIBUTE,
        /** the source is the subject's own attribute under a kind other than total, and has no instances */
        OWN_ATTRIBUTE,
        /** there is no subject to derive the range from */
        NO_SUBJECT,
        /** no relation carries the range */
        NO_RELATION
    }

    /** Why the fold {@code f}, written from {@code subject}, has no spelling on the wire; null where it has one and
     *  where {@code f} is no fold over one source. {@link #foldRange} refuses each reason, and the resolver asks the
     *  same of every fold it hands the emitter and refuses such a model itself, with an ERROR, so a model it built
     *  never reaches the refusal here. */
    public static FoldRefusal foldRefusalOf(RegelSpraakModel m, ObjectType subject, Expr.FunctionCall f) {
        if (!isFoldSource(f)) return null;
        Expr.AttributeReference arg = (Expr.AttributeReference) f.arguments().get(0);
        boolean countSource = arg.type() instanceof Datatype.Unknown;
        if (arg.navigationPath() != null && !arg.navigationPath().isEmpty()) return FoldRefusal.WRITTEN_PATH;
        if (arg.navigationRole() != null && !(countSource && selfDenoting(m, subject, arg.navigationRole())))
            return FoldRefusal.WRITTEN_STEP;
        if (!countSource && !arg.set()) {
            // written without a range the source is read on the subject, so a source of another type loses its step
            if (subject != null && arg.objectType() != null && arg.objectType() != subject)
                return FoldRefusal.FOREIGN_ATTRIBUTE;
            return f.kind() == Expr.FunctionKind.TOTAAL ? null : FoldRefusal.OWN_ATTRIBUTE;
        }
        if (m == null || subject == null) return FoldRefusal.NO_SUBJECT;
        if (arg.set() && arg.rangePath() != null && !arg.rangePath().isEmpty()) return null;
        return rangeRole(m, subject, f, arg) == null ? FoldRefusal.NO_RELATION : null;
    }

    /** The one-step range of an instance fold whose source {@code arg} denotes instances: for the count source the
     *  written name and then the type pair, for a set reading the type pair and then the written name. Null where
     *  neither yields one relation. */
    private static RegelSpraakModel.RoleBindingRef rangeRole(RegelSpraakModel m, ObjectType subject, Expr.FunctionCall f,
                                                            Expr.AttributeReference arg) {
        if (arg.type() instanceof Datatype.Unknown) {
            RegelSpraakModel.RoleBindingRef rb = m.elementRoleBinding(subject, arg.attribute());
            return rb != null ? rb : m.connectingRoleBinding(subject, f.elementObjectType());
        }
        // the by-pair-then-written-name search lives in the model: the §8.2 quantifier fold's range is
        // derived at resolve time and must classify a written range exactly as this does
        return m.foldSourceBinding(subject, f.elementObjectType(), arg.subjectSource());
    }

    /** Whether {@code f} is an instance fold over one source, whose single argument the wire reads through the
     *  fold's range and never as a read of its own (IR-SPEC ch. 4.3) — the fold {@link #foldRange} spells. */
    public static boolean isFoldSource(Expr.FunctionCall f) {
        return kindFamily(f.kind()) == Spelling.AGGREGATION_KIND && f.dimensionSelection() == null
                && f.elementObjectType() != null && f.arguments().size() == 1
                && f.arguments().get(0) instanceof Expr.AttributeReference;
    }

    private static IllegalStateException foldRefusal(Expr.FunctionCall f, String reason) {
        return new IllegalStateException("IrEmitter: de " + category(f) + "-vouw '" + f.source().snippet()
                + "' heeft geen spelling in uchar-ir-v1: " + reason);
    }

    /** Under AANTAL the source names the role the range binds (WF-047), by declared name or declared plural.
     *  A written name that does neither — a best-effort plural, the plural of the element type — is written
     *  as the role's declared name, and the written spelling stays in the source snippet. A name that is a member
     *  of the element type makes an attribute source, which WF-093 judges; it is written as it stands, as is
     *  every source under another kind. Null: the record's own name. */
    private String countSourceName(Expr.FunctionCall f, Expr.AttributeReference arg, RegelSpraakModel.RoleBindingRef rb) {
        if (f.kind() != Expr.FunctionKind.AANTAL || arg.attribute() == null) return null;
        Role role = boundRole(rb);
        if (role == null || denotes(role, arg.attribute()) || isMember(f.elementObjectType(), arg.attribute())) return null;
        return role.name();
    }

    private void roleBinding(RegelSpraakModel.RoleBindingRef rb) {
        w.beginObject();
        w.key("node").value("RoleBinding");
        w.key("factType").value(rb.factType());
        w.key("role").value(rb.role());
        w.endObject();
    }

    /** The {@code path} of a value/condition-position attribute read: one RoleBinding per
     *  navigation hop, subject-outward, each naming the reached (far) role. Three shapes: a declared
     *  CHAIN (one binding per step, whole-or-not — all hops derivable, or none), a single by-NAME hop,
     *  and a by-PAIR read of an off-subject type. Empty when the read does not navigate, no subject
     *  anchors the walk, or no hop set is uniquely derivable; {@link #ownRead} then decides whether
     *  the bindingless read is the subject's own or has no spelling. */
    private List<RegelSpraakModel.RoleBindingRef> navigationBindings(Expr.AttributeReference a) {
        return navigationBindings(m, currentSubject, a);
    }

    /** As {@link #navigationBindings(Expr.AttributeReference)}, from {@code subject}: the resolver names the hops of a read
     *  it refuses by the path this emitter would write for it. */
    public static List<RegelSpraakModel.RoleBindingRef> navigationBindings(RegelSpraakModel m, ObjectType subject,
                                                                         Expr.AttributeReference a) {
        if (m == null || subject == null || a.objectType() == null) return List.of();
        if (a.navigationPath() != null && !a.navigationPath().isEmpty()) {        // §13.4.1 declared chain
            List<ObjectType> reached = m.followChain(subject, a.navigationPath());
            if (reached == null) return List.of();
            List<RegelSpraakModel.RoleBindingRef> result = new java.util.ArrayList<>();
            ObjectType cur = subject;
            for (int i = 0; i < a.navigationPath().size(); i++) {
                RegelSpraakModel.RoleBindingRef b =
                        m.connectingRoleViaNameBinding(cur, reached.get(i), a.navigationPath().get(i));
                if (b == null) return List.of();                             // heel-of-niet over the chain
                result.add(b);
                cur = reached.get(i);
            }
            return result;
        }
        if (a.navigationRole() != null) {                                      // single-hop by-name
            RegelSpraakModel.RoleBindingRef b =
                    m.connectingRoleViaNameBinding(subject, a.objectType(), a.navigationRole());
            return b == null ? List.of() : List.of(b);
        }
        if (a.objectType() != subject) {                                     // by-pair (off-subject type)
            RegelSpraakModel.RoleBindingRef b = m.connectingRoleBinding(subject, a.objectType());
            // the pair does not decide, and the written noun is a type name that is also a role name: the role it
            // names decides, as a written role does (§13.4.1); a type name alone binds by the one relation only
            if (b == null) b = m.namedRoleBinding(subject, a.objectType(), a.subjectSource());
            return b == null ? List.of() : List.of(b);
        }
        return List.of();                                                    // own attribute — not navigated
    }

    /** The {@code navigation} of a navigated §8.1.8 kenmerk check: the single reached role, found
     *  exactly as {@code RuleChecker.navigationCharacteristicCheck} finds it (the target type via the written role, then
     *  the by-name binding). Null when the check does not navigate or the hop is not uniquely derivable. */
    private RegelSpraakModel.RoleBindingRef navigationBinding(Condition.CharacteristicCheck k) {
        if (m == null || currentSubject == null || k.navigation() == null) return null;
        ObjectType target = m.roleTargetTypeToSubject(currentSubject, k.navigation());
        return target == null ? null : m.connectingRoleViaNameBinding(currentSubject, target, k.navigation());
    }

    /**
     * IR-SPEC ch. 4.1a: a read without bindings is the subject's own. It must be one — the type it names is
     * the subject's and nothing was written that walks away from the subject — or the model is refused,
     * because a reader would take the read for the subject's own attribute. The one written step admitted
     * is the zero-hop self-reference ("het strandtegoed van de aanmelder", read on the aanmelder): a single
     * written role that reaches no far role from the subject and that the subject's type itself plays.
     */
    private void ownRead(Expr.AttributeReference a) {
        if (!unboundRead(m, currentSubject, a)) return;
        throw new IllegalStateException("IrEmitter: de lezing '" + a.source().snippet() + "' van "
                + nameOf(a.objectType()) + " vanaf " + (currentSubject == null ? "geen onderwerp" : currentSubject.name)
                + " navigeert, en de resolver vond daarvoor geen unieke binding");
    }

    /** Whether {@code a}, read from {@code subject} outside a fold source, is a read this emitter refuses: it
     *  navigates — by a written role or chain, or by naming another type than the subject's — and no binding is
     *  derivable ({@link #navigationBindings}), outside the zero-hop self-reference of {@link #ownRead}. The
     *  resolver asks the same question of every read the emitter will write, so a model it built never reaches
     *  this refusal: it refuses such a model itself, with an ERROR. */
    public static boolean unboundRead(RegelSpraakModel m, ObjectType subject, Expr.AttributeReference a) {
        if (!navigationBindings(m, subject, a).isEmpty()) return false;
        boolean path = a.navigationPath() != null && !a.navigationPath().isEmpty();
        boolean written = a.navigationRole() != null || path;
        boolean foreignType = subject != null && a.objectType() != null && a.objectType() != subject;
        if (!written && !foreignType) return false;
        return path || a.navigationRole() == null || subject == null || a.objectType() != subject
                || m.roleHitsToSubject(subject, a.navigationRole()) != 0
                || !selfDenoting(m, subject, a.navigationRole());
    }

    /** Whether the type {@code subject} itself plays a role that {@code name} denotes (declared name or
     *  declared plural, under the verifier's name identity) — the self-denotation of WF-053's ch. 4.1a
     *  reading, on the declared filler type exactly. */
    private static boolean selfDenoting(RegelSpraakModel m, ObjectType subject, String name) {
        if (m == null || subject == null || name == null) return false;
        for (FactType ft : m.factTypes.values())
            for (Role r : ft.roles)
                if (r.objectTypeName() != null && norm(r.objectTypeName()).equals(norm(subject.name))
                        && denotes(r, name)) return true;
        return false;
    }

    /** A role check as the wire carries it: the binding (the fact type and the role the subject plays) and,
     *  where the check narrows the other end to a strict subtype of that role's declared filler, the type it
     *  narrows to (null: no narrowing). */
    private record Membership(RegelSpraakModel.RoleBindingRef binding, String counterpart) {}

    /** The role check with the subject on the near side ("hij is een wachtjutter"): the fact type in which
     *  the subject plays the written role and {@code otherToFiller} fills the other. The written name denotes
     *  the role by declared name or declared plural; a check the resolver bound in any other way has no
     *  binding to write, and is refused. */
    private Membership nearMembership(Condition.RoleMembership r) {
        List<Membership> k = nearCandidates(m, currentSubject, r);
        if (k.size() != 1)
            throw new IllegalStateException("IrEmitter: de rolcheck '" + r.source().snippet() + "' bindt " + k.size()
                    + " rollen '" + r.role() + "' die " + (currentSubject == null ? "het onderwerp" : currentSubject.name)
                    + " vult tegenover " + nameOf(r.otherToFiller()) + " (gedeclareerde naam of meervoud); de wire "
                    + "eist er precies één");
        return k.get(0);
    }

    /** The role check with the subject on the far side ("de wachtpost heeft een wachtjutter"): the fact type
     *  in which the subject plays the role {@code backref} names and {@code filler} fills the other. */
    private Membership farMembership(Condition.FarRoleMembership r) {
        List<Membership> k = farCandidates(m, currentSubject, r);
        if (k.size() != 1)
            throw new IllegalStateException("IrEmitter: de rolcheck '" + r.source().snippet() + "' bindt " + k.size()
                    + " rollen '" + r.backref() + "' die " + (currentSubject == null ? "het onderwerp" : currentSubject.name)
                    + " speelt tegenover " + nameOf(r.filler()) + "; de wire eist er precies één");
        return k.get(0);
    }

    /** How many bindings a role check has from {@code subject}: the emitter writes it where exactly one, and the
     *  resolver asks the same of every role check the emitter will write, so a model it built never reaches the two
     *  refusals above: it refuses such a model itself, with an ERROR. 1 for any other condition. */
    public static int roleCheckBindings(RegelSpraakModel m, ObjectType subject, Condition c) {
        return switch (c) {
            case Condition.RoleMembership r -> nearCandidates(m, subject, r).size();
            case Condition.FarRoleMembership r -> farCandidates(m, subject, r).size();
            default -> 1;
        };
    }

    private static List<Membership> nearCandidates(RegelSpraakModel m, ObjectType subject,
                                                      Condition.RoleMembership r) {
        List<Membership> result = new java.util.ArrayList<>();
        if (m != null && subject != null && r.role() != null && r.otherToFiller() != null)
            for (FactType ft : m.factTypes.values()) {
                if (ft.reciprocal || ft.roles.size() != 2) continue;
                for (int i = 0; i < 2; i++) {
                    Role own = ft.roles.get(i), other = ft.roles.get(1 - i);
                    if (denotes(own, r.role()) && fills(m, subject, own) && fills(m, r.otherToFiller(), other))
                        result.add(membership(m, ft, own, other, r.otherToFiller()));
                }
            }
        return result;
    }

    private static List<Membership> farCandidates(RegelSpraakModel m, ObjectType subject,
                                                    Condition.FarRoleMembership r) {
        List<Membership> result = new java.util.ArrayList<>();
        if (m != null && subject != null && r.backref() != null && r.filler() != null)
            for (FactType ft : m.factTypes.values()) {
                if (ft.reciprocal && ft.roles.size() == 1) {
                    Role role = ft.roles.get(0);
                    if (denotes(role, r.backref()) && fills(m, subject, role) && fills(m, r.filler(), role))
                        result.add(membership(m, ft, role, role, r.filler()));
                    continue;
                }
                if (ft.roles.size() != 2) continue;
                for (int i = 0; i < 2; i++) {
                    Role own = ft.roles.get(i), other = ft.roles.get(1 - i);
                    if (denotes(own, r.backref()) && fills(m, subject, own) && fills(m, r.filler(), other))
                        result.add(membership(m, ft, own, other, r.filler()));
                }
            }
        return result;
    }

    /** The check's other end narrows only where it names a STRICT subtype of the other role's declared filler;
     *  naming the declared type itself is no narrowing, and the field then stays absent. */
    private static Membership membership(RegelSpraakModel m, FactType ft, Role own, Role other,
                                            ObjectType counterpart) {
        ObjectType declared = m.objectTypeByName(other.objectTypeName());
        return new Membership(new RegelSpraakModel.RoleBindingRef(ft.name, own.name()),
                counterpart == declared ? null : counterpart.name);
    }

    private void roleMembership(Membership l, boolean negation, Source source) {
        w.key("node").value("RoleMembership");
        w.key("factType").value(l.binding().factType());
        w.key("role").value(l.binding().role());
        if (l.counterpart() != null) w.key("counterpartType").value(l.counterpart());
        w.key("negated").value(negation);
        sourceReference(source.snippet());
    }

    /** Whether {@code ot} fills {@code r}: its declared filler type or a subtype of it (IR-SPEC ch. 2.6). */
    private static boolean fills(RegelSpraakModel m, ObjectType ot, Role r) {
        ObjectType filler = m.objectTypeByName(r.objectTypeName());
        return ot != null && filler != null && ot.isA(filler);
    }

    /** The declared role a binding names, under the verifier's name identity; null when it names none or
     *  several. */
    private Role boundRole(RegelSpraakModel.RoleBindingRef rb) {
        if (m == null || rb == null || rb.factType() == null || rb.role() == null) return null;
        for (FactType ft : m.factTypes.values())
            if (norm(ft.name).equals(norm(rb.factType()))) {
                List<Role> hits = ft.roles.stream()
                        .filter(r -> r.name() != null && norm(r.name()).equals(norm(rb.role()))).toList();
                return hits.size() == 1 ? hits.get(0) : null;
            }
        return null;
    }

    /** Whether {@code name} denotes {@code r}: its declared name or its declared plural. */
    private static boolean denotes(Role r, String name) {
        return name != null && ((r.name() != null && norm(r.name()).equals(norm(name)))
                || (r.plural() != null && norm(r.plural()).equals(norm(name))));
    }

    /** Whether {@code name} is an attribute or a kenmerk of {@code ot}, own or inherited. */
    private static boolean isMember(ObjectType ot, String name) {
        if (ot == null || name == null) return false;
        for (ObjectType t : ot.chain()) {
            for (String a : t.attributes.keySet()) if (norm(a).equals(norm(name))) return true;
            for (String k : t.characteristics) if (norm(k).equals(norm(name))) return true;
        }
        return false;
    }

    /** The verifier's name identity ({@code checks.ml norm}): runs of spaces and tabs collapse to one space
     *  and only A-Z fold to lower case. Public for the resolver, which refuses a second declaration of one unit
     *  abbreviation under this same identity before a model can reach the emitter's own check. */
    public static String norm(String s) {
        StringBuilder result = new StringBuilder();
        for (String word : s.split("[ \t]+")) {
            if (word.isEmpty()) continue;
            if (result.length() > 0) result.append(' ');
            for (int i = 0; i < word.length(); i++) {
                char c = word.charAt(i);
                result.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
            }
        }
        return result.toString();
    }

    /** IR-SPEC ch. 3.4: a fact creation and its versions. */
    private void factCreation(Form<ResolvedFactCreation> form) {
        w.beginObject();
        w.key("node").value("FactCreation");
        w.key("name").value(form.name());
        w.key("versions").beginArray();
        for (ResolvedFactCreation fc : form.versions()) factCreationVersion(fc);
        w.endArray();
        originAndDesignations(RegelSpraakModel.Designatable.form(form.name()));
        w.endObject();
    }

    /** The creation is its symbolic block — the created relation, both its roles and the path from the anchor, one
     *  binding per hop with the last reaching the target type — so a creation the resolver could not bind whole has
     *  no document. The target type is not written: a reader takes the declared filler of the target role, so the
     *  model's target type must be that filler, or the document would name another one. */
    private void factCreationVersion(ResolvedFactCreation fc) {
        if (fc.factType() == null || fc.anchorRole() == null || fc.targetRole() == null || fc.path() == null
                || fc.pathSteps().stream().anyMatch(t -> t.roleBinding() == null))
            throw new IllegalStateException("IrEmitter: de feitcreatie '" + fc.name() + "' is niet heel symbolisch "
                    + "gebonden (het feittype, beide rollen en elke stap van het pad); zonder dat blok heeft ze "
                    + "geen spelling in uchar-ir-v1");
        FactType ft = m.factTypes.get(fc.factType());
        List<Role> targetRoles = ft == null ? List.of()
                : ft.roles.stream().filter(r -> fc.targetRole().equals(r.name())).toList();
        if (targetRoles.size() != 1 || !java.util.Objects.equals(targetRoles.get(0).objectTypeName(), nameOf(fc.target())))
            throw new IllegalStateException("IrEmitter: de feitcreatie '" + fc.name() + "' maakt feiten naar '"
                    + nameOf(fc.target()) + "', en de wire schrijft die doelsoort niet: een lezer neemt de gedeclareerde "
                    + "vuller van de rol '" + fc.targetRole() + "' in '" + fc.factType() + "'"
                    + (targetRoles.size() == 1 ? ", en dat is '" + targetRoles.get(0).objectTypeName() + "'"
                            : ", en die rol is er " + targetRoles.size() + " keer"));
        currentSubject = fc.anchor();   // the §13.4.7 guard navigates from the anker
        w.beginObject();
        w.key("factType").value(fc.factType());
        w.key("anchorRole").value(fc.anchorRole());
        w.key("targetRole").value(fc.targetRole());
        w.key("anchor").value(nameOf(fc.anchor()));
        w.key("path").beginArray();
        for (ResolvedFactCreation.PathStep t : fc.pathSteps()) roleBinding(t.roleBinding());
        roleBinding(fc.path());
        w.endArray();
        if (fc.condition() != null) { w.key("condition"); condition(fc.condition()); }
        w.key("validity"); validity(fc.validity());
        w.endObject();
        currentSubject = null;
    }

    private void startPointDetermination(ResolvedStartPointDetermination s) {
        w.beginObject();
        w.key("node").value("StartPointDetermination");
        w.key("timeline").value(s.timeline());
        w.key("objectType").value(s.objectType());
        w.key("member").value(s.member());
        w.endObject();
    }

    private void ruleGroup(ResolvedRuleGroup g) {
        w.beginObject();
        w.key("node").value("RuleGroup");
        w.key("name").value(g.name());
        w.key("recursive").value(g.recursive());
        w.key("members"); stringList(g.members());
        sourceReference(g.source().snippet(), g.line());
        originAndDesignations(RegelSpraakModel.Designatable.ruleGroup(g.name()));
        w.endObject();
    }

    // --- the side table: origins (IR-SPEC ch. 2.13) and vocabularies (ch. 2.12) -------------------------

    /** The origin and the designations of {@code element}: the last two fields of its node, each written whole or not
     *  at all. */
    private void originAndDesignations(RegelSpraakModel.Designatable element) {
        origin(element);
        designations(element);
    }

    /** The origin of {@code element} where the frontend made it ({@link RegelSpraakModel#generated}): the form that
     *  made it and the declaration it comes from, by kind, owner and name as the model records them. A declaration
     *  whose name the source declares has no entry and gets no field. */
    private void origin(RegelSpraakModel.Designatable element) {
        java.util.Optional<RegelSpraakModel.GeneratedFrom> generatedFrom = m.generatedFrom(element);
        if (generatedFrom.isEmpty()) return;
        RegelSpraakModel.Designatable source = generatedFrom.get().source();
        w.key("origin").beginObject();
        w.key("form").value(Spelling.ORIGIN_FORM.spelling(generatedFrom.get().form()));
        w.key("from").beginObject();
        w.key("kind").value(Spelling.originKind(generatedFrom.get()));
        if (source.owner() != null) w.key("owner").value(source.owner());
        w.key("name").value(source.name());
        w.endObject();
        w.endObject();
    }

    /** The designations of {@code element}, sorted by the declaration order of their vocabularies: the last field of
     *  its node, written whole or not at all. */
    private void designations(RegelSpraakModel.Designatable element) {
        List<RegelSpraakModel.Designation> list = sorted(m.designations.get(element));
        if (list.isEmpty()) return;
        w.key("designations").beginArray();
        for (RegelSpraakModel.Designation b : list) {
            w.beginObject();
            w.key("vocabulary").value(b.vocabulary());
            w.key("designation").value(b.wording());
            w.endObject();
        }
        w.endArray();
    }

    /** The designations of an enumeration's values, beside its value list: value by value in the declared order, each
     *  value's designations sorted as {@link #designations} sorts them. */
    private void valueDesignations(String domain, List<String> values) {
        List<String[]> rules = new java.util.ArrayList<>();
        for (String value : values)
            for (RegelSpraakModel.Designation b : sorted(m.designations.get(
                    RegelSpraakModel.Designatable.enumerationValue(domain, value))))
                rules.add(new String[]{value, b.vocabulary(), b.wording()});
        if (rules.isEmpty()) return;
        w.key("valueDesignations").beginArray();
        for (String[] r : rules) {
            w.beginObject();
            w.key("value").value(r[0]);
            w.key("vocabulary").value(r[1]);
            w.key("designation").value(r[2]);
            w.endObject();
        }
        w.endArray();
    }

    /** A declaration's designations in the declaration order of their vocabularies (maintainer decision 2026-09-27); a
     *  designation whose vocabulary the model does not declare has no place in that order and no document, since the
     *  resolver refuses it (WF-117). */
    private List<RegelSpraakModel.Designation> sorted(List<RegelSpraakModel.Designation> list) {
        if (list == null || list.isEmpty()) return List.of();
        List<RegelSpraakModel.Designation> result = new java.util.ArrayList<>(list);
        result.sort(java.util.Comparator.comparingInt(b -> {
            for (int i = 0; i < m.vocabularies.size(); i++)
                if (m.vocabularies.get(i).name().equalsIgnoreCase(b.vocabulary())) return i;
            throw new IllegalStateException("IrEmitter: de benaming '" + b.wording() + "' noemt het vocabulaire '"
                    + b.vocabulary() + "', dat het model niet declareert (WF-117)");
        }));
        return result;
    }

    private void diagnostic(Diagnostic d) {
        w.beginObject();
        w.key("node").value("Diagnostic");
        w.key("severity").value(d.severity() == null ? null : Spelling.SEVERITY.spelling(d.severity()));
        w.key("message").value(d.message());
        w.key("line").value(d.line());
        w.endObject();
    }

    /** IR-SPEC ch. 3.10: an absent bound is open on that side, and a validity with neither bound is always. */
    private void writeValidity(Validity g) {
        if (g == null) { w.valueNull(); return; }
        w.beginObject();
        w.key("node").value("Validity");
        if (g.from() != null) w.key("from").value(g.from().toString());
        if (g.until() != null) w.key("to").value(g.until().toString());
        w.endObject();
    }

    // --- shared helpers ----------------------------------------------------

    private static String nameOf(ObjectType ot) { return ot == null ? null : ot.name; }

    private void stringList(List<String> xs) {
        w.beginArray();
        if (xs != null) for (String x : xs) w.value(x);
        w.endArray();
    }


    /**
     * A minimal streaming JSON writer: two-space indentation, correct escaping (quotes,
     * backslash, control chars, non-ASCII as {@code \\uXXXX}), numbers via
     * {@link BigDecimal#toPlainString()}, explicit {@code null}. Containers track their own
     * child count so commas and newlines are placed deterministically; a key emits its own
     * separator and the following value is written inline. Package-visible because the case lowering
     * ({@link CaseEmitter}) writes its files with it: the IR and the case files share one shape.
     */
    static final class Json {
        final StringBuilder sb = new StringBuilder();
        private final java.util.ArrayDeque<int[]> counts = new java.util.ArrayDeque<>();
        private boolean afterKey = false;
        private int depth = 0;

        private void nl() {
            sb.append('\n');
            for (int i = 0; i < depth; i++) sb.append("  ");
        }

        /** Emit the separator+newline before a value that is a direct array element or the root. */
        private void preValue() {
            if (afterKey) { afterKey = false; return; }
            if (!counts.isEmpty()) {
                int[] c = counts.peek();
                if (c[0] > 0) sb.append(',');
                nl();
                c[0]++;
            }
        }

        Json beginObject() { preValue(); sb.append('{'); counts.push(new int[]{0}); depth++; return this; }
        Json endObject() { depth--; int[] c = counts.pop(); if (c[0] > 0) nl(); sb.append('}'); return this; }
        Json beginArray() { preValue(); sb.append('['); counts.push(new int[]{0}); depth++; return this; }
        Json endArray() { depth--; int[] c = counts.pop(); if (c[0] > 0) nl(); sb.append(']'); return this; }

        Json key(String k) {
            int[] c = counts.peek();
            if (c[0] > 0) sb.append(',');
            nl();
            c[0]++;
            escape(k);
            sb.append(": ");
            afterKey = true;
            return this;
        }

        Json value(String s) { preValue(); if (s == null) sb.append("null"); else escape(s); return this; }
        Json value(BigDecimal d) { preValue(); sb.append(d == null ? "null" : d.toPlainString()); return this; }
        Json value(java.math.BigInteger i) { preValue(); sb.append(i.toString()); return this; }
        Json value(Integer i) { preValue(); sb.append(i == null ? "null" : Integer.toString(i)); return this; }
        Json value(int i) { preValue(); sb.append(Integer.toString(i)); return this; }
        Json value(boolean b) { preValue(); sb.append(b ? "true" : "false"); return this; }
        Json valueNull() { preValue(); sb.append("null"); return this; }

        private void escape(String s) {
            sb.append('"');
            for (int i = 0; i < s.length(); i++) {
                char ch = s.charAt(i);
                switch (ch) {
                    case '"' -> sb.append("\\\"");
                    case '\\' -> sb.append("\\\\");
                    case '\n' -> sb.append("\\n");
                    case '\r' -> sb.append("\\r");
                    case '\t' -> sb.append("\\t");
                    case '\b' -> sb.append("\\b");
                    case '\f' -> sb.append("\\f");
                    default -> {
                        if (ch < 0x20 || ch > 0x7e) sb.append(String.format("\\u%04x", (int) ch));
                        else sb.append(ch);
                    }
                }
            }
            sb.append('"');
        }
    }
}
