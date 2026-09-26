package io.github.yhavinga.regelspraak.emit;

import io.github.yhavinga.regelspraak.ir.Bron;
import io.github.yhavinga.regelspraak.ir.Conditie;
import io.github.yhavinga.regelspraak.ir.Default;
import io.github.yhavinga.regelspraak.ir.ExceptionGroup;
import io.github.yhavinga.regelspraak.ir.Expr;
import io.github.yhavinga.regelspraak.ir.Geldigheid;
import io.github.yhavinga.regelspraak.ir.PrioritizedDefaults;
import io.github.yhavinga.regelspraak.ir.ResolvedConsistentie;
import io.github.yhavinga.regelspraak.ir.ResolvedDagsoort;
import io.github.yhavinga.regelspraak.ir.ResolvedFeitCreatie;
import io.github.yhavinga.regelspraak.ir.ResolvedObjectCreatie;
import io.github.yhavinga.regelspraak.ir.ResolvedRegel;
import io.github.yhavinga.regelspraak.ir.ResolvedRegelGroep;
import io.github.yhavinga.regelspraak.ir.ResolvedStartpunt;
import io.github.yhavinga.regelspraak.ir.ResolvedUniciteit;
import io.github.yhavinga.regelspraak.ir.ResolvedUniciteitUnie;
import io.github.yhavinga.regelspraak.ir.ResolvedVerdeling;
import io.github.yhavinga.regelspraak.ir.Spelling;
import io.github.yhavinga.regelspraak.model.Datatype;
import io.github.yhavinga.regelspraak.model.Eenheid;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Diagnostic;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Dimensie;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Eenheidstelsel;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Feittype;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Granulariteit;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Objecttype;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Parameter;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Rol;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Tijdlijn;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
 * ({@link #ongeschrevenVerdelingen}). The key order of every node is the order this emitter wrote it in before the
 * L5 renaming, with the keys renamed in place and a field left out where the model is empty, so a re-emission is
 * byte-identical to a migrated document. That is the {@code properties} order of the schema
 * ({@code spec/schema/uchar-ir-v1.schema.json}), as {@code spec/schema/README.md} records.
 *
 * <p>Completeness is compiler-enforced where it can be: the {@link Datatype}, {@link Expr}
 * and {@link Conditie} sealed hierarchies are walked with EXHAUSTIVE switches (no default
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

    /** What the model carries that the wire has no field for, as ERROR diagnostics — today only the
     *  off-calendar fixed startpunt (§13.3.6.3–.7, {@link RegelSpraakModel#vasteStartpunten}). The model
     *  resolves it correctly; dropping it here would lay the tijdlijn on the calendar grid the author did
     *  not declare. {@code Resolver --ir} adds these to the diagnostics so it writes nothing, and
     *  {@link #emit} refuses the same models, so no emission route loses the start point in silence. */
    public static List<Diagnostic> wireWeigeringen(RegelSpraakModel m) {
        return m.vasteStartpunten.stream().map(v -> new Diagnostic(RegelSpraakModel.Severity.ERROR,
                "tijdlijn: het vaste startpunt '" + v.bron() + "' (" + v.sectie() + ") wijkt af van de "
                        + "kalenderperiode en de wire heeft er geen veld voor — de zin wordt geweigerd in "
                        + "plaats van het startpunt stil te laten vallen", v.line())).toList();
    }

    /** The allocations the wire cannot carry, as WARNING diagnostics: one whose method the frontend could not decide
     *  (the model's placeholder for it). IR-SPEC ch. 3.2 gives an allocation one of three methods and nothing else,
     *  so it is left out of the document and this names it, which is the producer's account of what stayed unresolved
     *  (WF-113). The model has no source line for an allocation, so the line is 0: a frontend warning without a
     *  token. {@link #emit} appends these after the model's own diagnostics; {@code Resolver --ir} prints them. */
    public static List<Diagnostic> ongeschrevenVerdelingen(RegelSpraakModel m) {
        return m.verdelingen.stream().filter(v -> v.methode() == ResolvedVerdeling.Methode.COMPLEX)
                .map(v -> new Diagnostic(RegelSpraakModel.Severity.WARNING, "verdeling '" + v.naam() + "' wordt niet "
                        + "geschreven: de frontend bepaalde voor haar geen van de drie verdeelmethoden (in gelijke "
                        + "delen, naar rato, op volgorde), en een verdeling zonder methode heeft geen spelling in "
                        + "uchar-ir-v1", 0))
                .toList();
    }

    /** Serialise the resolved model to the IR JSON text (two-space indented, trailing newline). */
    public static String emit(RegelSpraakModel m) {
        List<Diagnostic> weigeringen = wireWeigeringen(m);
        if (!weigeringen.isEmpty())
            throw new IllegalStateException("de wire kan dit model niet dragen: " + weigeringen);
        // WF-113 (a): an ERROR means the producer refused the document, so a model that carries one has no
        // document to write; checked here and not only in irModus, so no emission route bypasses it. The
        // message and line clauses are the verifier's too: a document carrying either would be refused there.
        // Each arm is refused in the verifier's sentence, at the pointer the verifier reports it at, followed by
        // the refused diagnostic itself: an ERROR's own message is what the caller needs to read.
        List<String> nietWire = new java.util.ArrayList<>();
        for (int i = 0; i < m.diagnostics.size(); i++) {
            Diagnostic d = m.diagnostics.get(i);
            String severity = severityWeigering(d.severity());
            if (severity != null) nietWire.add("at /diagnostics/" + i + "/severity: " + severity + " — " + d);
            if (d.message() == null || d.message().isBlank())
                nietWire.add("at /diagnostics/" + i + "/message: diagnostic with an empty or blank message: an entry "
                        + "that says nothing accounts for nothing — " + d);
            if (d.line() < 0)
                nietWire.add("at /diagnostics/" + i + "/line: diagnostic with line " + d.line() + ": a line is a "
                        + "source position, and 0 is the value for none (a hand-built document, or a producer warning "
                        + "without a token) — " + d);
        }
        if (!nietWire.isEmpty())
            throw new IllegalStateException("IrEmitter: WF-113 (a) " + String.join("; ", nietWire));
        IrEmitter e = new IrEmitter();
        e.model(m);
        e.w.sb.append('\n');
        return e.w.sb.toString();
    }

    /** WF-113 (a)'s severity arm, the verifier's sentences: warning is the one wire severity. Null for a warning. */
    private static String severityWeigering(RegelSpraakModel.Severity s) {
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
    private Objecttype huidigSubject;
    /** The one AttribuutRef currently being emitted as the single argument of an instance fold: its
     *  relation is the fold's range ({@code sourceRole} or {@code sourcePath}), so it never carries a
     *  {@code path} (IR-SPEC ch. 4.1a; the verifier rejects one in a fold slot, WF-053).
     *  Null otherwise. */
    private Expr foldBron;
    /** The name {@link #foldBron} is written with where it differs from the record's: see
     *  {@link #telbronNaam}. Null: the record's own name. */
    private String foldBronNaam;

    // --- top-level model ---------------------------------------------------

    private void model(RegelSpraakModel m) {
        this.m = m;
        w.beginObject();
        w.key("format").value("uchar-ir-v1");

        w.key("model").beginObject();

        w.key("objectTypes").beginArray();
        for (var en : m.objecttypen.entrySet()) objectType(en.getKey(), en.getValue());
        w.endArray();

        w.key("factTypes").beginArray();
        for (var en : m.feittypen.entrySet()) { eenNaam("feittype", en.getKey(), en.getValue().naam); feittype(en.getValue()); }
        w.endArray();

        w.key("parameters").beginArray();
        for (var en : m.parameters.entrySet()) { eenNaam("parameter", en.getKey(), en.getValue().naam()); parameter(en.getValue()); }
        w.endArray();

        w.key("domains").beginArray();
        for (var en : m.domeinen.entrySet()) domain(en.getKey(), en.getValue());
        w.endArray();

        w.key("unitSystems").beginArray();
        for (var en : m.eenheidstelsels.entrySet()) { eenNaam("eenheidstelsel", en.getKey(), en.getValue().naam); unitSystem(en.getValue()); }
        w.endArray();

        w.key("dimensions").beginArray();
        for (var en : m.dimensies.entrySet()) { eenNaam("dimensie", en.getKey(), en.getValue().naam()); dimensie(en.getValue()); }
        w.endArray();

        w.key("timelines").beginArray();
        for (Tijdlijn t : m.tijdlijnen) tijdlijn(t);
        w.endArray();

        w.key("dayKinds"); dayKinds();

        w.endObject();

        w.key("rules").beginObject();

        // IR-SPEC ch. 2, Rules: each list holds named forms, grouped from the model's one entry per version
        // (vormen); the model's lists keep their v0 names
        w.key("rules").beginArray();
        for (Vorm<ResolvedRegel> v : vormen("rules", m.resolvedRegels, ResolvedRegel::naam,
                IrEmitter::annotaties)) rule(v);
        w.endArray();

        w.key("dayKindDefinitions").beginArray();
        for (ResolvedDagsoort d : m.resolvedDagsoorten) resolvedDagsoort(d);
        w.endArray();

        w.key("objectCreations").beginArray();
        for (Vorm<ResolvedObjectCreatie> v : vormen("objectCreations", m.objectCreaties, ResolvedObjectCreatie::naam,
                null)) objectCreation(v);
        w.endArray();

        // an allocation without one of the three methods has no spelling: it is left out, and
        // ongeschrevenVerdelingen names it in the diagnostics below
        w.key("allocations").beginArray();
        List<ResolvedVerdeling> verdelingen = m.verdelingen.stream()
                .filter(v -> v.methode() != ResolvedVerdeling.Methode.COMPLEX).toList();
        for (Vorm<ResolvedVerdeling> v : vormen("allocations", verdelingen, ResolvedVerdeling::naam, null))
            allocation(v);
        w.endArray();

        w.key("consistencies").beginArray();
        for (Vorm<ResolvedConsistentie> v : vormen("consistencies", m.consistenties, ResolvedConsistentie::naam, null))
            consistency(v);
        w.endArray();

        // IR-SPEC ch. 3.6: one list of uniquenesses, the single-source forms first and then the unions, each in model
        // order; the model keeps the two in two lists
        w.key("uniquenesses").beginArray();
        for (Vorm<ResolvedUniciteit> v : vormen("uniquenesses", m.uniciteiten, ResolvedUniciteit::naam, null))
            uniqueness(v);
        for (Vorm<ResolvedUniciteitUnie> v : vormen("uniquenesses", m.uniciteitUnies, ResolvedUniciteitUnie::naam,
                null)) uniquenessUnion(v);
        w.endArray();

        w.key("factCreations").beginArray();
        for (Vorm<ResolvedFeitCreatie> v : vormen("factCreations", m.feitCreaties, ResolvedFeitCreatie::naam, null))
            factCreation(v);
        w.endArray();

        w.key("startPointDeterminations").beginArray();
        for (ResolvedStartpunt s : m.resolvedStartpunten) startpunt(s);
        w.endArray();

        w.key("ruleGroups").beginArray();
        for (ResolvedRegelGroep g : m.regelGroepen) regelGroep(g);
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
        for (Diagnostic d : ongeschrevenVerdelingen(m)) diagnostic(d);
        w.endArray();

        w.endObject();
    }

    /** A declaration list writes its name once, on the node: a map key that disagrees with the node's own
     *  name would lose one of the two spellings without a rule for choosing (delta B2), so it is refused. */
    private static void eenNaam(String soort, String sleutel, String naam) {
        if (!sleutel.equals(naam))
            throw new IllegalStateException("IrEmitter: " + soort + " '" + sleutel + "' draagt de eigen naam '"
                    + naam + "'; de lijstvorm heeft voor beide maar één plek");
    }

    // --- §4 prioritized defaults (extension) -------------------------------

    private void prioritizedDefaults(PrioritizedDefaults p) {
        // the path of a default's own-attribute/characteristic read resolves against the group subject
        Objecttype vorigSubject = huidigSubject;
        huidigSubject = m == null ? null : m.objecttypen.get(p.onderwerp());
        w.beginObject();
        w.key("node").value("PrioritizedDefaults");
        w.key("name").value(p.naam());
        w.key("subject").value(p.onderwerp());
        if (p.kenmerk() != null) w.key("characteristic").value(p.kenmerk());   // absent on attribute defaults
        if (p.attribuut() != null) w.key("attribute").value(p.attribuut());   // only on attribute defaults (WF-050)
        w.key("defaults").beginArray();
        for (Default d : p.defaults()) dflt(d);
        w.endArray();
        if (p.ongeordend()) w.key("unordered").value(true);         // only on a declared-unordered group member
        bronrecord(p.bron());
        w.key("validity"); geldigheid(p.geldigheid());
        w.endObject();
        huidigSubject = vorigSubject;
    }

    // `default` is a Java keyword, so the emit method for a Default node is spelled dflt.
    private void dflt(Default d) {
        w.beginObject();
        w.key("node").value("Default");
        // absent on an attribute default
        if (d.besluit() != null) w.key("decision").value(d.besluit().booleanValue());
        // absent on the initialisation default and on a group default (IR-SPEC ch. 6.1)
        if (d.voorwaarde() != null) { w.key("justification"); conditie(d.voorwaarde()); }
        if (d.consequentie() != null) { w.key("consequence"); expr(d.consequentie()); }  // attribute defaults only
        if (d.initialisatie()) w.key("initialisation").value(true);
        if (d.excepties() != null) {
            w.key("exceptions").beginObject();
            w.key("unordered").value(d.excepties().ongeordend());
            w.key("members").beginArray();
            for (ExceptionGroup g : d.excepties().leden()) exceptionGroup(g);
            w.endArray();
            w.endObject();
        }
        bronrecord(d.bron());
        w.endObject();
    }

    private void exceptionGroup(ExceptionGroup g) {
        w.beginObject();
        w.key("node").value("ExceptionGroup");
        w.key("defaults").beginArray();
        for (Default d : g.defaults()) dflt(d);
        w.endArray();
        bronrecord(g.bron());
        w.endObject();
    }

    // --- declaration side --------------------------------------------------

    /** IR-SPEC ch. 2.2: the model keeps its eight member-keyed side-maps; the wire carries each member's facets
     *  on the member's own node. A side-map entry the member nodes cannot carry — a key naming no member, a count
     *  or timeline name without a granularity, a null value — is a frontend defect, refused rather than dropped. */
    private void objectType(String sleutel, Objecttype ot) {
        eenNaam("objecttype", sleutel, ot.naam);
        zijkaartenPassen(ot);
        w.beginObject();
        w.key("node").value("ObjectType");
        w.key("name").value(ot.naam);
        // an extension field under the § Method's discipline — optional, never null, written WHOLE or not at
        // all, so a root type emits no key
        if (ot.supertype != null) w.key("supertype").value(ot.supertype.naam);
        w.key("animate").value(ot.bezield);
        if (ot.meervoud != null) w.key("plural").value(ot.meervoud);
        w.key("attributes").beginArray();
        for (var en : ot.attributen.entrySet()) {
            String a = en.getKey();
            w.beginObject();
            w.key("node").value("Attribute");
            w.key("name").value(a);
            w.key("datatype"); datatype(en.getValue(), true);
            String meervoud = ot.attribuutMeervoud.get(a);
            if (meervoud != null) w.key("plural").value(meervoud);
            List<String> dimensies = ot.attribuutDimensies.get(a);
            if (dimensies != null && !dimensies.isEmpty()) { w.key("dimensions"); stringList(dimensies); }
            if (ot.attribuutGranulariteit.containsKey(a))
                timelineFacet(ot.attribuutGranulariteit.get(a), ot.attribuutTijdlijnAantal.get(a),
                        ot.attribuutTijdlijnNaam.get(a));
            w.endObject();
        }
        w.endArray();
        w.key("characteristics").beginArray();
        for (String k : ot.kenmerken) {
            w.beginObject();
            w.key("node").value("Characteristic");
            w.key("name").value(k);
            // ch. 2.2: an untagged characteristic is nominal
            Objecttype.KenmerkSoort soort = ot.kenmerkSoorten.get(k);
            w.key("kind").value(Spelling.CHARACTERISTIC_KIND.spelling(
                    soort == null ? Objecttype.KenmerkSoort.ZELFSTANDIG : soort));
            if (ot.kenmerkGranulariteit.containsKey(k))
                timelineFacet(ot.kenmerkGranulariteit.get(k), ot.kenmerkTijdlijnAantal.get(k), null);
            w.endObject();
        }
        w.endArray();
        w.endObject();
    }

    /** A facet exists exactly when the member has a granularity; a member without a count is single-period. */
    private void timelineFacet(Granulariteit granulariteit, Integer aantal, String tijdlijnNaam) {
        w.key("timeline").beginObject();
        w.key("node").value("TimelineFacet");
        w.key("granularity").value(Spelling.GRANULARITY.spelling(granulariteit));
        w.key("count").value(aantal == null ? 1 : aantal);
        if (tijdlijnNaam != null) w.key("timelineName").value(tijdlijnNaam);
        w.endObject();
    }

    private static void zijkaartenPassen(Objecttype ot) {
        java.util.Set<String> attributen = ot.attributen.keySet();
        java.util.Set<String> kenmerken = new java.util.HashSet<>(ot.kenmerken);
        zijkaartPast(ot, "attribuutMeervoud", ot.attribuutMeervoud, attributen, false);
        zijkaartPast(ot, "attribuutDimensies", ot.attribuutDimensies, attributen, true);
        zijkaartPast(ot, "attribuutGranulariteit", ot.attribuutGranulariteit, attributen, true);
        zijkaartPast(ot, "attribuutTijdlijnAantal", ot.attribuutTijdlijnAantal, ot.attribuutGranulariteit.keySet(), true);
        zijkaartPast(ot, "attribuutTijdlijnNaam", ot.attribuutTijdlijnNaam, ot.attribuutGranulariteit.keySet(), true);
        zijkaartPast(ot, "kenmerkSoorten", ot.kenmerkSoorten, kenmerken, false);
        zijkaartPast(ot, "kenmerkGranulariteit", ot.kenmerkGranulariteit, kenmerken, true);
        zijkaartPast(ot, "kenmerkTijdlijnAantal", ot.kenmerkTijdlijnAantal, ot.kenmerkGranulariteit.keySet(), true);
    }

    private static void zijkaartPast(Objecttype ot, String kaart, Map<String, ?> zijkaart,
                                     java.util.Set<String> dragers, boolean geenNull) {
        for (var en : zijkaart.entrySet()) {
            if (!dragers.contains(en.getKey()))
                throw new IllegalStateException("IrEmitter: " + ot.naam + "." + kaart + " heeft een sleutel '"
                        + en.getKey() + "' zonder lid (of zonder granulariteit) om hem te dragen");
            if (geenNull && en.getValue() == null)
                throw new IllegalStateException("IrEmitter: " + ot.naam + "." + kaart + "['" + en.getKey()
                        + "'] is null");
        }
    }

    /** IR-SPEC ch. 2.3: the wrapper carries the name, so the datatype at this declaration position carries no
     *  self-name; at every reference position {@code domain} stays (it is the domain reference there). */
    private void domain(String naam, Datatype dt) {
        if (dt == null)
            throw new IllegalStateException("IrEmitter: domein '" + naam + "' heeft geen datatype");
        w.beginObject();
        w.key("node").value("Domain");
        w.key("name").value(naam);
        w.key("datatype");
        if (dt instanceof Datatype.Enumeratie e) {
            eenNaam("domein", naam, e.domein());
            w.beginObject();
            w.key("node").value("Enumeration");
            w.key("values"); stringList(e.waarden());
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
            throw new IllegalStateException("IrEmitter: parameter '" + p.naam() + "' heeft geen datatype; de wire "
                    + "eist er een");
        w.beginObject();
        w.key("node").value("Parameter");
        w.key("name").value(p.naam());
        w.key("datatype"); datatype(p.datatype(), true);
        if (p.granulariteit() != null) timelineFacet(p.granulariteit(), null, null);
        w.endObject();
    }

    /** IR-SPEC ch. 2.5: a unit system is its list of units in declaration order, each unit once with its further
     *  spellings on it. The model keeps two maps — every written abbreviation, and the unit words and plurals
     *  (normalised) — and a key of either that is not the unit's own abbreviation is one of its spellings, the map's
     *  keys before the aliases; a spelling is never the abbreviation itself. What the list cannot carry — an alias of
     *  a unit the system does not declare, one abbreviation for two different units (under the name identity too), a
     *  spelling reached twice — is a frontend defect, refused rather than dropped. */
    private void unitSystem(Eenheidstelsel es) {
        Map<String, Eenheid> eenheden = new java.util.LinkedHashMap<>();
        Map<String, List<String>> spellingen = new java.util.LinkedHashMap<>();
        for (var en : es.eenheden.entrySet()) {
            Eenheid e = stelselEenheid(es, en.getKey(), en.getValue());
            Eenheid eerder = eenheden.putIfAbsent(e.afkorting(), e);
            if (eerder != null && !eerder.equals(e))
                throw new IllegalStateException("IrEmitter: eenheidstelsel '" + es.naam + "' schrijft de afkorting '"
                        + e.afkorting() + "' voor twee verschillende eenheden; de lijst draagt elke eenheid eenmaal");
            spellingen.putIfAbsent(e.afkorting(), new java.util.ArrayList<>());
            if (!en.getKey().equals(e.afkorting())) spellingen.get(e.afkorting()).add(en.getKey());
        }
        for (var en : es.aliassen.entrySet()) {
            Eenheid e = stelselEenheid(es, en.getKey(), en.getValue());
            if (!e.equals(eenheden.get(e.afkorting())))
                throw new IllegalStateException("IrEmitter: eenheidstelsel '" + es.naam + "' kent de spelling '"
                        + en.getKey() + "' toe aan de eenheid '" + e.afkorting() + "', die het niet zo declareert; een "
                        + "spelling staat alleen op een gedeclareerde eenheid");
            if (!en.getKey().equals(e.afkorting())) spellingen.get(e.afkorting()).add(en.getKey());
        }
        // one Unit per abbreviation under the name identity (WF-009): the model keys its units on the exact
        // spelling, so two declarations whose abbreviations differ in case alone are two units here and one name there
        Map<String, String> perNaam = new java.util.HashMap<>();
        for (String a : eenheden.keySet()) {
            String ander = perNaam.putIfAbsent(norm(a), a);
            if (ander != null)
                throw new IllegalStateException("IrEmitter: eenheidstelsel '" + es.naam + "' declareert de eenheden '"
                        + ander + "' en '" + a + "', die onder de naamidentiteit één afkorting zijn; de lijst draagt één "
                        + "eenheid per afkorting (WF-009)");
        }
        w.beginObject();
        w.key("node").value("UnitSystem");
        w.key("name").value(es.naam);
        w.key("units").beginArray();
        for (Eenheid e : eenheden.values()) {
            List<String> sp = spellingen.get(e.afkorting());
            if (new java.util.HashSet<>(sp).size() != sp.size())
                throw new IllegalStateException("IrEmitter: eenheidstelsel '" + es.naam + "' draagt een spelling van '"
                        + e.afkorting() + "' tweemaal, als afkorting en als alias: " + sp);
            w.beginObject();
            unitVelden(e);
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
        List<String> namen = m.dagsoorten;
        List<List<String>> verder = new java.util.ArrayList<>();
        for (int i = 0; i < namen.size(); i++) verder.add(new java.util.ArrayList<>());
        for (var en : m.dagsoortPerEenheid.entrySet()) {
            String waarde = en.getValue();
            List<Integer> kandidaten = new java.util.ArrayList<>();
            for (int i = 0; i < namen.size(); i++)
                if (waarde != null && norm(namen.get(i)).equals(norm(waarde))) kandidaten.add(i);
            if (kandidaten.size() != 1)
                throw new IllegalStateException("IrEmitter: de dagsoortspelling '" + en.getKey() + "' noemt '" + waarde
                        + "', dat " + kandidaten.size() + " gedeclareerde dagsoorten aanduidt; een spelling staat op "
                        + "precies één dagsoort");
            int i = kandidaten.get(0);
            if (!norm(en.getKey()).equals(norm(namen.get(i)))) verder.get(i).add(en.getKey());
        }
        w.beginArray();
        for (int i = 0; i < namen.size(); i++) {
            if (verder.get(i).size() > 1)
                throw new IllegalStateException("IrEmitter: dagsoort '" + namen.get(i) + "' heeft naast haar naam de "
                        + "spellingen " + verder.get(i) + "; de wire draagt een naam en één meervoud");
            w.beginObject();
            w.key("node").value("DayKind");
            w.key("name").value(namen.get(i));
            if (!verder.get(i).isEmpty()) w.key("plural").value(verder.get(i).get(0));
            w.endObject();
        }
        w.endArray();
    }

    private static Eenheid stelselEenheid(Eenheidstelsel es, String sleutel, Eenheid e) {
        if (e == null)
            throw new IllegalStateException("IrEmitter: eenheidstelsel '" + es.naam + "' heeft onder '" + sleutel
                    + "' geen eenheid");
        return e;
    }

    /** A unit where it occurs — on a number, as the denominator of a rate: its fields, never its spellings (those
     *  belong to the declaration in its unit system). */
    private void unit(Eenheid e) {
        w.beginObject();
        unitVelden(e);
        w.endObject();
    }

    /** The fields of a Unit in the schema's order; an absent field is left out. The factor is the exact fraction
     *  (see {@link #exacteFactor}). */
    private void unitVelden(Eenheid e) {
        w.key("node").value("Unit");
        w.key("abbreviation").value(e.afkorting());
        if (e.stelsel() != null) w.key("system").value(e.stelsel());
        if (e.canoniek() != null) w.key("canonical").value(e.canoniek());
        Eenheidstelsel.Factor f = exacteFactor(e);
        if (f != null) {
            w.key("factorToCanonical").beginObject();
            w.key("num").value(f.num());
            w.key("den").value(f.den());
            w.endObject();
        }
        if (e.noemer() != null) { w.key("denominator"); unit(e.noemer()); }
    }

    /** The factor of a unit as the wire writes it: an exact positive fraction in lowest terms, or null when the unit
     *  carries none (a product of units). A unit its system declares takes the fraction that declaration wrote
     *  ({@link Eenheidstelsel#exacteFactoren}) — its record's decimal factor is that fraction rounded per
     *  declaration step, and the two must agree to that rounding. A unit outside every system — the built-in '%', a
     *  bare calendar unit — is its own canonical with factor 1, a whole number, written over 1. Any other factor has
     *  no exact spelling here and is refused, as is a factor that is not positive. */
    private Eenheidstelsel.Factor exacteFactor(Eenheid e) {
        BigDecimal f = e.factorNaarCanoniek();
        if (f == null) return null;
        Eenheidstelsel es = e.stelsel() == null ? null : m.eenheidstelsels.get(e.stelsel());
        Eenheidstelsel.Factor x = es == null ? null : es.exacteFactoren.get(e.afkorting());
        if (x == null) {
            if (f.signum() <= 0 || f.stripTrailingZeros().scale() > 0)
                throw new IllegalStateException("IrEmitter: eenheid '" + e.weergave() + "' draagt de factor "
                        + f.toPlainString() + ", en geen eenheidstelsel declareert haar als breuk; de wire schrijft de "
                        + "factor als exacte positieve breuk");
            return new Eenheidstelsel.Factor(f.toBigIntegerExact(), java.math.BigInteger.ONE);
        }
        if (x.num().signum() <= 0)
            throw new IllegalStateException("IrEmitter: eenheid '" + e.weergave() + "' heeft de gedeclareerde factor "
                    + x.num() + "/" + x.den() + "; de wire schrijft de factor als exacte positieve breuk");
        BigDecimal breuk = new BigDecimal(x.num()).divide(new BigDecimal(x.den()), java.math.MathContext.DECIMAL128);
        if (f.subtract(breuk).abs().compareTo(breuk.movePointLeft(12)) > 0)
            throw new IllegalStateException("IrEmitter: eenheid '" + e.weergave() + "' draagt de factor "
                    + f.toPlainString() + ", maar haar stelsel declareert " + x.num() + "/" + x.den());
        return x;
    }

    private void feittype(Feittype ft) {
        w.beginObject();
        w.key("node").value("FactType");
        w.key("name").value(ft.naam);
        w.key("reciprocal").value(ft.wederkerig);
        w.key("roles").beginArray();
        for (Rol r : ft.rollen) rol(r);
        w.endArray();
        w.endObject();
    }

    private void rol(Rol r) {
        w.beginObject();
        w.key("node").value("Role");
        w.key("name").value(r.naam());
        w.key("objectType").value(r.objecttypeNaam());
        if (r.meervoud() != null) w.key("plural").value(r.meervoud());
        w.key("many").value(r.meervoudig());
        w.endObject();
    }

    private void dimensie(Dimensie d) {
        w.beginObject();
        w.key("node").value("Dimension");
        w.key("name").value(d.naam());
        if (d.naamMeervoud() != null) w.key("plural").value(d.naamMeervoud());
        if (d.voorzetsel() != null) w.key("preposition").value(d.voorzetsel());
        w.key("labels"); stringList(d.waarden());
        w.endObject();
    }

    private void tijdlijn(Tijdlijn t) {
        w.beginObject();
        w.key("node").value("Timeline");
        w.key("name").value(t.naam());
        w.key("variableStart").value(t.variabelStartpunt());
        w.key("granularity").value(t.granulariteit() == null ? null
                : Spelling.GRANULARITY.spelling(t.granulariteit()));
        w.key("count").value(t.aantal());
        w.endObject();
    }

    // --- Datatype (exhaustive) --------------------------------------------

    /** A datatype at a type annotation: the {@code type} of an expression node. */
    private void datatype(Datatype dt) { datatype(dt, false); }

    /** IR-SPEC ch. 2.4. A number is a {@code Number}, its sign always written (a number declared without one is
     *  unspecified); a percentage is a {@code Number} whose unit is '%' — the built-in one when it has none — and at a
     *  {@code declaratie} position (the datatype of an attribute, a parameter or a domain) it carries the presentation
     *  tag, which a type annotation never does. A date is a {@code Date} with its precision. */
    private void datatype(Datatype dt, boolean declaratie) {
        if (dt == null) { w.valueNull(); return; }
        w.beginObject();
        switch (dt) {
            case Datatype.Numeriek n -> {
                w.key("node").value("Number");
                w.key("whole").value(n.geheel());
                w.key("decimals").value(n.decimalen());
                w.key("sign").value(Spelling.NUMBER_SIGN.spelling(
                        n.teken() == null ? Datatype.Teken.ONBEPAALD : n.teken()));
                if (n.eenheid() != null) { w.key("unit"); unit(n.eenheid()); }
            }
            case Datatype.Percentage p -> {
                Eenheid e = p.eenheid() == null ? Datatype.Percentage.PROCENT : p.eenheid();
                if (!"%".equals(e.afkorting()))
                    throw new IllegalStateException("IrEmitter: een percentage met de eenheid '" + e.weergave()
                            + "'; de wire schrijft een percentage als getal met de eenheid '%'");
                w.key("node").value("Number");
                w.key("whole").value(p.geheel());
                w.key("decimals").value(p.decimalen());
                w.key("sign").value(Spelling.NUMBER_SIGN.spelling(Datatype.Teken.ONBEPAALD));
                w.key("unit"); unit(e);
                if (declaratie) w.key("presentation").value("percentage");
            }
            case Datatype.Tekst t -> w.key("node").value("Text");
            case Datatype.Booleaan b -> w.key("node").value("Boolean");
            case Datatype.DatumInDagen d -> {
                w.key("node").value("Date");
                w.key("precision").value(Spelling.DATE_PRECISION.spelling(d));
            }
            case Datatype.DatumTijd d -> {
                w.key("node").value("Date");
                w.key("precision").value(Spelling.DATE_PRECISION.spelling(d));
            }
            case Datatype.Enumeratie e -> {
                w.key("node").value("Enumeration");
                w.key("domain").value(e.domein());
                w.key("values"); stringList(e.waarden());
            }
            // the resolver's "no claim" has no wire spelling: a derived position the frontend cannot type is
            // written null (afgeleidType) and a marker leaves its type out, so an unknown datatype reaching
            // this arm stands at a declaration, a payload or a literal, and a document there would be refused
            // at load
            case Datatype.Onbekend o -> throw onbekendType(o);
        }
        w.endObject();
    }

    private static IllegalStateException onbekendType(Datatype.Onbekend o) {
        return new IllegalStateException("IrEmitter: een onbekend datatype ('" + o.reden() + "') heeft op deze "
                + "positie geen spelling in uchar-ir-v1; alleen een afgeleide positie schrijft null en een "
                + "Unresolved-marker laat zijn type weg");
    }

    /** IR-SPEC ch. 2.10 a DERIVED position — an arithmetic node, a fold, a rounding, a clamp — whose result
     *  the frontend could not type (a datum plus a datum, which it reports and does not refuse): the one
     *  spelling of "derived, but not typeable" is null, which the schema admits there. Not a marker: the
     *  expression is resolved. */
    private void afgeleidType(Datatype dt) {
        if (dt instanceof Datatype.Onbekend) w.valueNull(); else datatype(dt);
    }

    /** The type a function node writes. Two builtins have a result type the spec fixes whatever their operands are,
     *  and the resolver leaves both without a claim, so their null would be refused by WF-034: the day count counts
     *  days and is typed as every count is (RUNTIME-SEMANTICS ch. 2.3 and ch. 3.8: whole, non-negative, unitless),
     *  and the concatenation joins its parts into one text (IR-SPEC ch. 4.3, the text fold). Every other kind writes
     *  the resolver's type. A resolver claim that differs from the fixed type would give one annotation two sources,
     *  so it is refused rather than overwritten. */
    private static Datatype functieType(Expr.Functie f) {
        Datatype vast = switch (f.kind()) {
            case AANTAL_DAGEN_IN -> new Datatype.Numeriek(true, 0, Datatype.Teken.NIET_NEGATIEF, null);
            case CONCATENATIE -> new Datatype.Tekst();
            default -> null;
        };
        if (vast == null) return f.type();
        if (f.type() != null && !(f.type() instanceof Datatype.Onbekend) && !vast.equals(f.type()))
            throw new IllegalStateException("IrEmitter: de " + soort(f) + "-aanroep '" + f.bron().snippet()
                    + "' draagt het type " + f.type().naam() + ", en de spec legt het type " + vast.naam() + " vast");
        return vast;
    }

    /** IR-SPEC ch. 4.1: the value variant of a literal, chosen by its type — a number (a percentage included), a
     *  boolean, a text, a date of day and of millisecond precision, an enumeration label. The frontend has no sum
     *  type, so it never writes the constructor variant. Without a type there is no variant to choose, and a boolean
     *  whose lexeme is not waar or onwaar has no value; both are refused rather than guessed. */
    private static String variant(Expr.Literal l) {
        if (l.waarde() == null)
            throw new IllegalStateException("IrEmitter: de literal '" + l.bron().snippet() + "' heeft geen waarde; de "
                    + "wire schrijft er altijd een");
        return switch (l.type()) {
            case null -> throw new IllegalStateException("IrEmitter: de literal '" + l.bron().snippet() + "' heeft geen "
                    + "type; de wire kiest de waardevariant van een literal naar zijn type");
            case Datatype.Numeriek n -> "number";
            case Datatype.Percentage p -> "number";
            case Datatype.Booleaan b -> {
                if (!l.waarde().equals("waar") && !l.waarde().equals("onwaar"))
                    throw new IllegalStateException("IrEmitter: de booleaanse literal '" + l.bron().snippet()
                            + "' draagt het lexeem '" + l.waarde() + "'; alleen waar en onwaar zijn een booleaanse waarde");
                yield "boolean";
            }
            case Datatype.Tekst t -> "text";
            case Datatype.DatumInDagen d -> "date";
            case Datatype.DatumTijd d -> "dateTime";
            case Datatype.Enumeratie e -> "enumeration";
            case Datatype.Onbekend o -> throw onbekendType(o);
        };
    }

    /** IR-SPEC ch. 4.3: the eight fold kinds are an Aggregation and every other kind a BuiltinCall; the table
     *  splits the one in-memory enum between the two families. */
    private static Spelling.Familie<Expr.FunctieKind> soortFamilie(Expr.FunctieKind k) {
        return Spelling.AGGREGATION_KIND.rijen().containsKey(k) ? Spelling.AGGREGATION_KIND : Spelling.BUILTIN_KIND;
    }

    private static String soort(Expr.Functie f) {
        return soortFamilie(f.kind()).spelling(f.kind());
    }

    /** A field on the function node of the other kind: an Aggregation has no place for a granularity, a BuiltinCall
     *  none for a fold's element type, empty value or dimension selection (IR-SPEC ch. 4.3). */
    private static IllegalStateException facetWeigering(Expr.Functie f, String veld, String drager) {
        return new IllegalStateException("IrEmitter: de " + soort(f) + "-aanroep '" + f.bron().snippet() + "' draagt "
                + veld + ", en in uchar-ir-v1 staat dat veld alleen op " + drager);
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
                    case "boolean" -> w.value(l.waarde().equals("waar"));
                    // the frontend keeps the decimal comma the author wrote; the wire's number is written with a point
                    case "number" -> w.value(l.waarde().replace(',', '.'));
                    default -> w.value(l.waarde());
                }
                w.endObject();
                w.key("type"); datatype(l.type());
                bronrecord(l.bron().snippet());
            }
            case Expr.TekstReeks t -> {
                w.key("node").value("TextComposition");
                w.key("parts").beginArray();
                for (Expr.TekstReeks.Deel d : t.delen()) tekstReeksDeel(d);
                w.endArray();
                w.key("type"); datatype(t.type());
                bronrecord(t.bron().snippet());
            }
            case Expr.Binair b -> {
                w.key("node").value("Binary");
                w.key("op").value(Spelling.BINARY_OP.spelling(b.op()));
                w.key("left"); expr(b.links());
                w.key("right"); expr(b.rechts());
                w.key("type"); afgeleidType(b.type());
                bronrecord(b.bron().snippet());
            }
            // IR-SPEC ch. 4.3: the eight fold kinds are an Aggregation, every other function a BuiltinCall, each
            // with the fields of its own node only
            case Expr.Functie f -> {
                boolean aggregatie = soortFamilie(f.kind()) == Spelling.AGGREGATION_KIND;
                if (aggregatie && f.granulariteit() != null)
                    throw facetWeigering(f, "de granulariteit " + Spelling.GRANULARITY.spelling(f.granulariteit()),
                            "een BuiltinCall");
                if (!aggregatie) {
                    if (f.elementObjecttype() != null)
                        throw facetWeigering(f, "het elementtype '" + f.elementObjecttype().naam + "'", "een Aggregation");
                    if (f.leegwaarde() != null) throw facetWeigering(f, "een leegwaarde", "een Aggregation");
                    if (f.dimensieSelectie() != null) throw facetWeigering(f, "een dimensieselectie", "een Aggregation");
                }
                // IR-SPEC ch. 4.3 (row 28): a dimension fold ranges over the labels of the subject's own
                // dimensioned attribute, not over instances, so it has no element type and no range and its one
                // argument is an ordinary read. The resolver records the subject as its element type; written, that
                // type would demand a range the fold does not have. Any other element type would be a fold this
                // reading does not describe.
                boolean dimensievouw = aggregatie && f.dimensieSelectie() != null;
                if (dimensievouw && f.elementObjecttype() != null && f.elementObjecttype() != huidigSubject)
                    throw vouwWeigering(f, "haar dimensieselectie loopt over een attribuut van "
                            + f.elementObjecttype().naam + ", en een dimensievouw leest het eigen attribuut van "
                            + (huidigSubject == null ? "het onderwerp" : huidigSubject.naam));
                Objecttype element = dimensievouw ? null : f.elementObjecttype();
                w.key("node").value(aggregatie ? "Aggregation" : "BuiltinCall");
                w.key("kind").value(soort(f));
                // decided before the arguments are written: under count the source's name follows the role
                // the range binds
                Vouwbereik bereik = dimensievouw ? null : vouwbereik(f);
                w.key("arguments").beginArray();
                Expr vorigeFoldBron = foldBron;
                String vorigeNaam = foldBronNaam;
                foldBron = bereik == null ? null : f.argumenten().get(0);
                foldBronNaam = bereik == null ? null : bereik.bronNaam();
                for (Expr a : f.argumenten()) expr(a);
                foldBron = vorigeFoldBron;
                foldBronNaam = vorigeNaam;
                w.endArray();
                if (element != null) w.key("elementType").value(element.naam);
                // the filter is a subselectie over the fold ELEMENT: an element-own read there is an own
                // read, not a navigation from the outer subject — emit the filter (and only the filter)
                // with the element as subject so such reads take the own-attribute branch and carry no
                // spurious by-pair path. Restore the outer subject afterwards.
                if (f.filter() != null) {
                    w.key("filter");
                    Objecttype vorigSubject = huidigSubject;
                    if (element != null) huidigSubject = element;
                    conditie(f.filter());
                    huidigSubject = vorigSubject;
                }
                if (f.leegwaarde() != null) { w.key("emptyFallback"); expr(f.leegwaarde()); }
                if (f.dimensieSelectie() != null) { w.key("dimensionSelection"); dimensieSelectie(f.dimensieSelectie()); }
                // IR-SPEC ch. 4.3 / WF-102: the range is ONE field with two spellings. A resolved
                // multi-step range goes out as sourcePath; the one-step range stays sourceRole, and the two are
                // mutually exclusive on the wire — so the path is written INSTEAD OF the binding, never
                // beside it. Only a fold has a range: a builtin reaches here with no element type, so
                // vouwbereik gave it none.
                if (bereik != null && bereik.rol() != null) { w.key("sourceRole"); rolBinding(bereik.rol()); }
                if (bereik != null && !bereik.pad().isEmpty()) {
                    w.key("sourcePath").beginArray();
                    for (RegelSpraakModel.RolBindingRef stap : bereik.pad()) rolBinding(stap);
                    w.endArray();
                }
                if (!aggregatie && f.granulariteit() != null)
                    w.key("granularity").value(Spelling.GRANULARITY.spelling(f.granulariteit()));
                w.key("type"); afgeleidType(functieType(f));
                bronrecord(f.bron().snippet());
            }
            case Expr.Afgerond a -> {
                w.key("node").value("Rounding");
                w.key("base"); expr(a.basis());
                w.key("mode").value(Spelling.ROUNDING_MODE.spelling(a.modus()));
                w.key("decimals").value(a.decimalen());
                w.key("type"); afgeleidType(a.type());
                bronrecord(a.bron().snippet());
            }
            case Expr.Begrensd b -> {
                w.key("node").value("Clamp");
                w.key("base"); expr(b.basis());
                // an absent bound is no bound on that side (RUNTIME-SEMANTICS ch. 5.11)
                if (b.minimum() != null) { w.key("minimum"); expr(b.minimum()); }
                if (b.maximum() != null) { w.key("maximum"); expr(b.maximum()); }
                w.key("type"); afgeleidType(b.type());
                bronrecord(b.bron().snippet());
            }
            case Expr.AttribuutRef r -> {
                w.key("node").value("AttributeReference");
                w.key("attribute").value(r == foldBron && foldBronNaam != null ? foldBronNaam : r.attribuut());
                w.key("subjectType").value(naamVan(r.objecttype()));
                bronrecord(r.bron().snippet());
                if (r != foldBron) {
                    List<RegelSpraakModel.RolBindingRef> nb = navigatieBindingen(r);
                    if (!nb.isEmpty()) {
                        w.key("path").beginArray();
                        for (RegelSpraakModel.RolBindingRef b : nb) rolBinding(b);
                        w.endArray();
                    } else {
                        eigenLezing(r);
                    }
                }
            }
            case Expr.DimensieCel c -> {
                w.key("node").value("DimensionCellRead");
                w.key("attribute"); expr(c.attribuut());
                w.key("labels"); stringList(c.labels());
                bronrecord(c.bron().snippet());
            }
            case Expr.ParameterRef p -> {
                w.key("node").value("ParameterReference");
                w.key("parameter").value(p.parameter() == null ? null : p.parameter().naam());
                bronrecord(p.bron().snippet());
            }
            case Expr.VariabeleRef v -> {
                w.key("node").value("VariableReference");
                w.key("name").value(v.naam());
                bronrecord(v.bron().snippet());
            }
            // ch. 4.10 the Let cluster. Key ORDER is the schema's required order (node, type, binder,
            // value, body, source / node, name, type, source) — gate 2 round-trips these bytes.
            case Expr.Let l -> {
                w.key("node").value("Let");
                w.key("type"); datatype(l.type());
                w.key("binder").value(l.binder());
                w.key("value"); expr(l.value());
                w.key("body"); expr(l.body());
                bronrecord(l.bron().snippet());
            }
            case Expr.BinderRef b -> {
                w.key("node").value("BinderRef");
                w.key("name").value(b.naam());
                bronrecord(b.bron().snippet());
            }
            // IR-SPEC ch. 4.1: the bound day of a day-kind definition is a node of its own and carries no name; a
            // session value is a ContextValue under one of its two names. Any other name has no spelling.
            case Expr.Constante c -> {
                if (c.naam() == null
                        || !c.naam().equals("de dag") && !Spelling.CONTEXT_VALUE_NAME.rijen().containsKey(c.naam()))
                    throw new IllegalStateException("IrEmitter: de constante '" + c.naam() + "' ('" + c.bron().snippet()
                            + "') heeft geen spelling in uchar-ir-v1: de wire kent de gebonden dag van een "
                            + "dagsoortdefinitie en de contextwaarden " + Spelling.CONTEXT_VALUE_NAME.rijen().values());
                if (c.type() == null)
                    throw new IllegalStateException("IrEmitter: de constante '" + c.naam() + "' ('" + c.bron().snippet()
                            + "') heeft geen type; de wire eist er een");
                if (c.naam().equals("de dag")) {
                    w.key("node").value("BoundDay");
                } else {
                    w.key("node").value("ContextValue");
                    w.key("name").value(Spelling.CONTEXT_VALUE_NAME.spelling(c.naam()));
                }
                w.key("type"); datatype(c.type());
                bronrecord(c.bron().snippet());
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
                for (Expr.InstanceSetUnion.Tak t : u.takken()) {
                    w.beginObject();
                    w.key("node").value("InstanceSetBranch");
                    w.key("elementType").value(naamVan(t.elementType()));
                    if (t.bronRol() == null)
                        throw new IllegalStateException("IrEmitter: een selectie van de unie '" + u.bron().snippet()
                                + "' zonder haar relatie; de wire eist die (WF-110)");
                    w.key("sourceRole"); rolBinding(t.bronRol());
                    w.key("filter");
                    Objecttype vorigSubject = huidigSubject;
                    if (t.elementType() != null) huidigSubject = t.elementType();
                    conditie(t.filter());
                    huidigSubject = vorigSubject;
                    bronrecord(t.bron().snippet());
                    w.endObject();
                }
                w.endArray();
                bronrecord(u.bron().snippet());
            }
            case Expr.Onopgelost o -> {
                w.key("node").value("Unresolved");
                // IR-SPEC ch. 4.4: absent is the one spelling of "type unknown"; a real datatype is written
                if (o.type() != null && !(o.type() instanceof Datatype.Onbekend)) { w.key("type"); datatype(o.type()); }
                bronrecord(markerBron(o.bron()));
            }
        }
        w.endObject();
    }

    private void tekstReeksDeel(Expr.TekstReeks.Deel d) {
        w.beginObject();
        switch (d) {
            case Expr.TekstReeks.Letterlijk l -> {
                w.key("node").value("LiteralChunk");
                w.key("text").value(l.tekst());
            }
            case Expr.TekstReeks.Ingevoegd i -> {
                w.key("node").value("EmbeddedExpression");
                w.key("expr"); expr(i.expr());
            }
        }
        w.endObject();
    }

    private void dimensieSelectie(Expr.DimensieSelectie ds) {
        if (ds == null) { w.valueNull(); return; }
        w.beginObject();
        w.key("node").value("DimensionSelection");
        w.key("dimension").value(ds.dimensie());
        w.key("labels"); stringList(ds.labels());
        bronrecord(ds.bron().snippet());
        w.endObject();
    }

    // --- Conditie (exhaustive) --------------------------------------------

    private void conditie(Conditie c) {
        if (c == null) { w.valueNull(); return; }
        w.beginObject();
        switch (c) {
            case Conditie.Vergelijking v -> {
                w.key("node").value("Comparison");
                w.key("op").value(Spelling.COMPARISON_OP.spelling(v.op()));
                w.key("left"); expr(v.links());
                w.key("right"); expr(v.rechts());
                geheelPeriode(v.periode());
                bronrecord(v.bron().snippet());
            }
            case Conditie.Predicaat p -> {
                w.key("node").value("Predicate");
                w.key("kind").value(Spelling.PREDICATE_KIND.spelling(p.soort()));
                w.key("operand"); expr(p.operand());
                if (p.dagsoort() != null) w.key("dayKind").value(p.dagsoort());
                w.key("negated").value(p.negatie());
                w.key("digits").value(p.cijfers());
                geheelPeriode(p.periode());
                bronrecord(p.bron().snippet());
            }
            case Conditie.Kenmerk k -> {
                w.key("node").value("CharacteristicCheck");
                w.key("characteristic").value(k.kenmerk());
                w.key("negated").value(k.negatie());
                // absent is the subject (RUNTIME-SEMANTICS ch. 5.8b)
                if (k.eigenaar() != null) w.key("host").value(k.eigenaar().naam);
                geheelPeriode(k.periode());
                bronrecord(k.bron().snippet());
                // the binding IS the hop: a navigated check without one would read the subject's own kenmerk
                if (k.navigatie() != null) {
                    RegelSpraakModel.RolBindingRef nb = navigatieBinding(k);
                    if (nb == null)
                        throw new IllegalStateException("IrEmitter: de kenmerkcheck '" + k.bron().snippet()
                                + "' navigeert via '" + k.navigatie() + "', en vanaf "
                                + (huidigSubject == null ? "geen onderwerp" : "'" + huidigSubject.naam + "'")
                                + " bindt die stap geen unieke rol");
                    w.key("navigation"); rolBinding(nb);
                }
            }
            // IR-SPEC ch. 4.5: one node for both sides of the role check, naming the fact type, the role the
            // subject plays in it and, where the check narrows it, the type of the other end; where the
            // reference sits is the consumer's storage question, not the wire's
            case Conditie.Rollidmaatschap r -> rolLidmaatschap(lidmaatschapNabij(r), r.negatie(), r.bron());
            case Conditie.VerreRollidmaatschap r -> rolLidmaatschap(lidmaatschapVer(r), r.negatie(), r.bron());
            case Conditie.Periode p -> {
                w.key("node").value("TimeWindow");
                // an absent bound is an open side
                if (p.vanaf() != null) { w.key("from"); expr(p.vanaf()); }
                if (p.tot() != null) { w.key("to"); expr(p.tot()); }
                w.key("toInclusive").value(p.totInclusief());
                bronrecord(p.bron().snippet());
            }
            case Conditie.Samengesteld s -> {
                w.key("node").value("Compound");
                w.key("quantifier").value(Spelling.QUANTIFIER.spelling(s.kwantor()));
                w.key("threshold").value(s.drempel());
                w.key("parts").beginArray();
                for (Conditie o : s.onderdelen()) conditie(o);
                w.endArray();
                bronrecord(s.bron().snippet());
            }
            // IR-SPEC ch. 4.5 / WF-108. Key ORDER is the schema's property order (node, quantifier, the
            // counting threshold, elementType, filter, test, source, then the range) — gate 2
            // round-trips these bytes.
            case Conditie.Kwantorvouw k -> {
                w.key("node").value("QuantifierFold");
                w.key("quantifier").value(Spelling.QUANTIFIER.spelling(k.kwantor()));
                // WF-108 (e): the threshold is written EXACTLY under a counting quantifier and omitted
                // under all / none — unlike Compound, whose threshold is written under every quantifier
                // and read only under the three counting ones, so that under all / none / the it is a
                // number no reader consults (0 in the goldens, except the 1 a gedurende clip's compound
                // carries). Emitting it here too would put a number on the wire the quantifier contradicts.
                if (k.kwantor() == Conditie.Kwantor.TEN_MINSTE || k.kwantor() == Conditie.Kwantor.TEN_HOOGSTE
                        || k.kwantor() == Conditie.Kwantor.PRECIES)
                    w.key("threshold").value(k.drempel());
                w.key("elementType").value(naamVan(k.elementObjecttype()));
                // filter and test are evaluated PER CONTRIBUTOR with the element as subject, exactly as
                // Functie.filter is: an element-own read there is an own read, not a navigation from the
                // outer subject, so emit both with the element as subject and they take the own-attribute
                // branch and carry no spurious by-pair path. Restore the outer subject after.
                Objecttype vorigSubject = huidigSubject;
                if (k.elementObjecttype() != null) huidigSubject = k.elementObjecttype();
                // an absent filter is no subselection
                if (k.filter() != null) { w.key("filter"); conditie(k.filter()); }
                w.key("test"); conditie(k.toets());
                huidigSubject = vorigSubject;
                bronrecord(k.bron().snippet());
                // the range is ONE field with two spellings, mutually exclusive, and required: without it the
                // fold would range over nothing the sentence named (WF-102, WF-108 (c))
                if (k.bronPad() != null && !k.bronPad().isEmpty()) {
                    w.key("sourcePath").beginArray();
                    for (RegelSpraakModel.RolBindingRef stap : k.bronPad()) rolBinding(stap);
                    w.endArray();
                } else if (k.bronRol() != null) {
                    w.key("sourceRole"); rolBinding(k.bronRol());
                } else {
                    throw new IllegalStateException("IrEmitter: de kwantorvouw '" + k.bron().snippet()
                            + "' heeft geen bereik; de resolver vond geen unieke relatie naar "
                            + naamVan(k.elementObjecttype()));
                }
            }
            case Conditie.Regelversie r -> {
                w.key("node").value("Regelversie");
                w.key("regel").value(r.regel());
                w.key("inconsistent").value(r.inconsistent());
                bronrecord(r.bron().snippet());
            }
            case Conditie.Onopgelost o -> {
                w.key("node").value("Unresolved");
                bronrecord(markerBron(o.bron()));
            }
        }
        w.endObject();
    }

    /** WF-113 (b): a marker is accounted for by a warning that quotes its source snippet verbatim. A marker without
     *  one is quoted by no message and a blank one by every message, so neither accounts for anything and the
     *  verifier refuses the document; the frontend can build one, since {@code DefinitionResolver.bron} gives an
     *  empty snippet for a missing context or an empty match. Each is refused in the verifier's sentence for its
     *  arm; an empty snippet is no snippet at all, and {@link #bronrecord} refuses it in the readers' own sentence. */
    private static String markerBron(Bron b) {
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
    private void bronrecord(String snippet) {
        bronrecord(snippet, 0);
    }

    /** As {@link #bronrecord(String)}, with the source line after the snippet — called by the consistency version
     *  and the rule group alone, the two places the model keeps a line. The model's 0 is "no line" (a synthetic
     *  node) and is left out, as the record admits only a line of at least 1. */
    private void bronrecord(String snippet, int regel) {
        if (snippet == null) return;
        if (snippet.isEmpty())
            throw new IllegalStateException("IrEmitter: a source snippet is never empty: a node without source text "
                    + "carries no source");
        if (regel < 0)   // the readers refuse it in this sentence (the loaders' source-line refusal)
            throw new IllegalStateException("IrEmitter: a source line is at least 1");
        w.key("source").beginObject();
        w.key("snippet").value(snippet);
        if (regel > 0) w.key("line").value(regel);
        w.endObject();
    }

    /** The whole-period qualifier of a condition, under its key; absent where the condition has none. */
    private void geheelPeriode(Conditie.GeheelPeriode gp) {
        if (gp == null) return;
        w.key("wholePeriod").beginObject();
        w.key("node").value("WholePeriod");
        w.key("granularity").value(Spelling.WHOLE_PERIOD_GRANULARITY.spelling(gp.jaar()));
        w.key("negated").value(gp.niet());
        w.endObject();
    }

    // --- resolved IR lists -------------------------------------------------

    // --- the named forms (IR-SPEC ch. 3, Versioning) -------------------------

    /** One named form on the wire: the name it is written under and its versions, in version order. */
    private record Vorm<T>(String naam, List<T> versies) {}

    /** The in-memory spelling of one version of a form with several: the form's name, a space, '#' and the version
     *  number (RuleChecker names each version so). */
    private static final java.util.regex.Pattern VERSIE = java.util.regex.Pattern.compile("([\\s\\S]*) #([0-9]+)");

    /** The form name of an entry whose name carries a version suffix, or null when it carries none. */
    private static String basis(String naam) {
        if (naam == null) return null;
        java.util.regex.Matcher mt = VERSIE.matcher(naam);
        return mt.matches() ? mt.group(1) : null;
    }

    private static String versienummer(String naam) {
        java.util.regex.Matcher mt = VERSIE.matcher(naam);
        if (!mt.matches()) throw new IllegalStateException("IrEmitter: '" + naam + "' draagt geen versieachtervoegsel");
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
    private static <T> List<Vorm<T>> vormen(String lijst, List<T> invoeren, java.util.function.Function<T, String> naamVan,
                                            java.util.function.Function<T, List<String>> annotaties) {
        // the runs: an entry without the suffix is a run of its own, the suffixed entries of one form name are one
        // run, and a form name has one run — asked of all runs before any run is written, as m4.py asks it
        List<List<T>> reeksen = new java.util.ArrayList<>();
        java.util.Set<String> gezien = new java.util.HashSet<>();
        int k = 0;
        while (k < invoeren.size()) {
            String basis = basis(naamVan.apply(invoeren.get(k)));
            int j = k + 1;
            while (basis != null && j < invoeren.size() && basis.equals(basis(naamVan.apply(invoeren.get(j))))) j++;
            if (basis != null && !gezien.add(basis))
                throw versieWeigering(lijst, basis, "haar versies staan niet aaneengesloten");
            reeksen.add(invoeren.subList(k, j));
            k = j;
        }
        List<Vorm<T>> uit = new java.util.ArrayList<>();
        for (List<T> reeks : reeksen) {
            String naam = naamVan.apply(reeks.get(0));
            String basis = basis(naam);
            if (basis == null) {
                uit.add(new Vorm<>(naam, reeks));
                continue;
            }
            if (reeks.size() < 2)
                throw versieWeigering(lijst, basis, "het versieachtervoegsel van '" + naam + "' staat op één invoer, "
                        + "en een vorm met één versie draagt haar kale naam");
            List<String> nummers = reeks.stream().map(x -> versienummer(naamVan.apply(x))).sorted().toList();
            List<String> verwacht = java.util.stream.IntStream.rangeClosed(1, reeks.size())
                    .mapToObj(Integer::toString).sorted().toList();
            if (!nummers.equals(verwacht))
                throw versieWeigering(lijst, basis, "de versieachtervoegsels zijn " + nummers + ", niet precies 1 tot "
                        + "en met " + reeks.size());
            if (basis(basis) != null)
                throw versieWeigering(lijst, basis, "de vormnaam eindigt zelf op een versieachtervoegsel");
            if (annotaties != null)
                for (T x : reeks)
                    if (!annotaties.apply(x).equals(annotaties.apply(reeks.get(0))))
                        throw versieWeigering(lijst, basis, "de versies dragen verschillende statuutannotaties, en de "
                                + "wire schrijft één lijst op de vorm");
            List<T> versies = new java.util.ArrayList<>(reeks);
            versies.sort(java.util.Comparator.comparingInt(x -> Integer.parseInt(versienummer(naamVan.apply(x)))));
            uit.add(new Vorm<>(basis, versies));
        }
        return uit;
    }

    private static IllegalStateException versieWeigering(String lijst, String basis, String reden) {
        return new IllegalStateException("IrEmitter: de vorm '" + basis + "' in " + lijst + " heeft geen spelling in "
                + "uchar-ir-v1: " + reden);
    }

    /** The statute annotations of a rule version as the wire writes them (WF-082): an empty annotation carries no
     *  citation and is dropped. */
    private static List<String> annotaties(ResolvedRegel r) {
        return r.bronAnnotaties().stream().filter(x -> !x.isEmpty()).toList();
    }

    /** IR-SPEC ch. 3.10: every version writes its validity; always is a Validity with neither bound. */
    private void validity(Geldigheid g) {
        geldigheid(g == null ? Geldigheid.ALTIJD : g);
    }

    /** IR-SPEC ch. 3.1: the rule and its versions. The extension field statuteAnnotations (WF-082) stands on the
     *  form, written whole or not at all: a rule without annotations leaves no trace on the wire. */
    private void rule(Vorm<ResolvedRegel> vorm) {
        w.beginObject();
        w.key("node").value("Rule");
        w.key("name").value(vorm.naam());
        w.key("versions").beginArray();
        for (ResolvedRegel r : vorm.versies()) ruleVersion(r);
        w.endArray();
        List<String> bronnen = annotaties(vorm.versies().get(0));
        if (!bronnen.isEmpty()) { w.key("statuteAnnotations"); stringList(bronnen); }
        w.endObject();
    }

    /** A rule version writes its kind as its structure: a version with a presentation tag is a gelijkstelling or a
     *  berekening, an initialisation is initialisation true without a tag, and a version with neither writes a
     *  characteristic — which member its target names is the declaration's (IR-SPEC ch. 3.1). The target is an
     *  attribute reference, and a name that is both an attribute and a characteristic of the subject type decides
     *  no member, so such a target is refused (WF-022). */
    private void ruleVersion(ResolvedRegel r) {
        if (r.soort() == null)
            throw new IllegalStateException("IrEmitter: regel '" + r.naam() + "' heeft geen soort; de wire schrijft "
                    + "een versie als initialisatie of niet, met of zonder presentatie");
        if (!(r.doel() instanceof Expr.AttribuutRef d))
            throw new IllegalStateException("IrEmitter: het doel van regel '" + r.naam() + "' is "
                    + (r.doel() == null ? "afwezig" : "een " + r.doel().getClass().getSimpleName())
                    + "; de wire schrijft als doel een attribuutreferentie");
        if (r.waarde() == null)
            throw new IllegalStateException("IrEmitter: regel '" + r.naam() + "' heeft geen waarde; de wire eist er een");
        if (d.objecttype() != null && d.attribuut() != null && isAttribuut(d.objecttype(), d.attribuut())
                && isKenmerk(d.objecttype(), d.attribuut()))
            throw new IllegalStateException("IrEmitter: het doel '" + d.attribuut() + "' van regel '" + r.naam()
                    + "' noemt op '" + d.objecttype().naam + "' zowel een attribuut als een kenmerk; welk lid een "
                    + "regel schrijft bepaalt haar soort, en deze naam bepaalt niets (WF-022)");
        huidigSubject = d.objecttype();
        // the target fixes the subject, so a navigation written on it has no carrier
        if (d.navigatieRol() != null || (d.navigatiePad() != null && !d.navigatiePad().isEmpty()))
            throw new IllegalStateException("IrEmitter: het doel van regel '" + r.naam() + "' ('"
                    + d.bron().snippet() + "') navigeert; het doel legt het onderwerp vast");
        w.beginObject();
        w.key("validity"); validity(r.geldigheid());
        w.key("target"); expr(d);
        w.key("value"); expr(r.waarde());
        if (r.voorwaarde() != null) { w.key("condition"); conditie(r.voorwaarde()); }
        w.key("initialisation").value(r.soort() == ResolvedRegel.Soort.INITIALISATIE);
        if (r.soort() == ResolvedRegel.Soort.GELIJKSTELLING || r.soort() == ResolvedRegel.Soort.BEREKENING)
            w.key("presentation").value(Spelling.PRESENTATION.spelling(r.soort()));
        w.endObject();
        huidigSubject = null;
    }

    /** Whether {@code naam} is an attribute of {@code ot}, own or inherited, under the name identity (WF-001). */
    private static boolean isAttribuut(Objecttype ot, String naam) {
        for (Objecttype t : ot.keten())
            for (String a : t.attributen.keySet()) if (norm(a).equals(norm(naam))) return true;
        return false;
    }

    /** Whether {@code naam} is a characteristic of {@code ot}, own or inherited, under the name identity (WF-001). */
    private static boolean isKenmerk(Objecttype ot, String naam) {
        for (Objecttype t : ot.keten())
            for (String k : t.kenmerken) if (norm(k).equals(norm(naam))) return true;
        return false;
    }

    private void resolvedDagsoort(ResolvedDagsoort d) {
        w.beginObject();
        w.key("node").value("DayKindDefinition");
        w.key("dayKind").value(d.naam());
        w.key("condition"); conditie(d.voorwaarde());
        w.key("validity"); geldigheid(d.geldigheid());
        w.endObject();
    }

    /** IR-SPEC ch. 3.3: an object creation and its versions. A version whose sentence names the created object by a
     *  role writes that role as its back-link, also where the type pair would derive it; a version whose sentence
     *  names the type writes none, and absent is the back-link derived from the type pair (RUNTIME-SEMANTICS
     *  ch. 5.5). */
    private void objectCreation(Vorm<ResolvedObjectCreatie> vorm) {
        w.beginObject();
        w.key("node").value("ObjectCreation");
        w.key("name").value(vorm.naam());
        w.key("versions").beginArray();
        for (ResolvedObjectCreatie oc : vorm.versies()) objectCreationVersion(oc);
        w.endArray();
        w.endObject();
    }

    private void objectCreationVersion(ResolvedObjectCreatie oc) {
        huidigSubject = oc.subject();   // member values and the guard navigate from the creation subject
        w.beginObject();
        w.key("createdType").value(naamVan(oc.nieuwType()));
        w.key("subject").value(naamVan(oc.subject()));
        w.key("initialisations").beginArray();
        for (ResolvedObjectCreatie.Toekenning t : oc.toekenningen()) {
            w.beginObject();
            w.key("node").value("MemberInit");
            w.key("member").value(t.attribuut());
            w.key("value"); expr(t.waarde());
            w.key("characteristic").value(t.kenmerk());
            w.endObject();
        }
        w.endArray();
        if (oc.voorwaarde() != null) { w.key("condition"); conditie(oc.voorwaarde()); }
        w.key("validity"); validity(oc.geldigheid());
        if (oc.verbindendeRol() != null) { w.key("connectingRole"); rolBinding(oc.verbindendeRol()); }
        w.endObject();
        huidigSubject = null;
    }

    /** IR-SPEC ch. 3.2: an allocation and its versions. */
    private void allocation(Vorm<ResolvedVerdeling> vorm) {
        w.beginObject();
        w.key("node").value("Allocation");
        w.key("name").value(vorm.naam());
        w.key("versions").beginArray();
        for (ResolvedVerdeling v : vorm.versies()) allocationVersion(v);
        w.endArray();
        w.endObject();
    }

    /** An optional field left out when the model holds none; the method is one of the three (the model's
     *  placeholder never reaches here, see {@link #ongeschrevenVerdelingen}). */
    private void allocationVersion(ResolvedVerdeling v) {
        if (v.methode() == null)
            throw new IllegalStateException("IrEmitter: de verdeling '" + v.naam() + "' heeft geen methode");
        // IR-SPEC ch. 3.2: the recipient join is the binding; a verdeling without it has no join to execute
        RegelSpraakModel.RolBindingRef rb = m.verbindendeRolBinding(v.bronType(), v.doelType());
        if (rb == null)
            throw new IllegalStateException("IrEmitter: de verdeling '" + v.naam() + "' heeft geen unieke relatie "
                    + "tussen " + naamVan(v.bronType()) + " en " + naamVan(v.doelType()));
        huidigSubject = v.bronType();
        w.beginObject();
        w.key("sourceType").value(naamVan(v.bronType()));
        w.key("sourceAttribute").value(v.bronAttribuut());
        w.key("targetType").value(naamVan(v.doelType()));
        w.key("targetAttribute").value(v.doelAttribuut());
        w.key("method").value(Spelling.METHOD.spelling(v.methode()));
        if (v.ratoAttribuut() != null) w.key("ratioAttribute").value(v.ratoAttribuut());
        if (v.maximumAttribuut() != null) w.key("maximumAttribute").value(v.maximumAttribuut());
        if (v.afrondingDecimalen() != null) w.key("roundingDecimals").value(v.afrondingDecimalen().intValue());
        if (v.restantAttribuut() != null) w.key("remainderAttribute").value(v.restantAttribuut());
        if (v.groepeerAttribuut() != null) w.key("orderingAttribute").value(v.groepeerAttribuut());
        w.key("descending").value(v.afnemend());
        if (v.voorwaarde() != null) { w.key("condition"); conditie(v.voorwaarde()); }
        w.key("validity"); validity(v.geldigheid());
        w.key("connectingRole"); rolBinding(rb);
        w.endObject();
        huidigSubject = null;
    }

    /** IR-SPEC ch. 3.5: a consistency rule and its versions; the source record stands on the version, with the
     *  line. */
    private void consistency(Vorm<ResolvedConsistentie> vorm) {
        w.beginObject();
        w.key("node").value("Consistency");
        w.key("name").value(vorm.naam());
        w.key("versions").beginArray();
        for (ResolvedConsistentie c : vorm.versies()) consistencyVersion(c);
        w.endArray();
        w.endObject();
    }

    private void consistencyVersion(ResolvedConsistentie c) {
        if (c.assertie() == null)
            throw new IllegalStateException("IrEmitter: de consistentieregel '" + c.naam() + "' heeft geen assertie; "
                    + "de wire eist er een");
        huidigSubject = c.subject();
        w.beginObject();
        w.key("subject").value(naamVan(c.subject()));
        w.key("assertion"); conditie(c.assertie());
        w.key("validity"); validity(c.geldigheid());
        bronrecord(c.bron().snippet(), c.line());
        w.endObject();
        huidigSubject = null;
    }

    /** IR-SPEC ch. 3.6: a uniqueness and its versions; a version is its list of sources, one for the single-type
     *  form, and the source record stands on the version. */
    private void uniqueness(Vorm<ResolvedUniciteit> vorm) {
        w.beginObject();
        w.key("node").value("Uniqueness");
        w.key("name").value(vorm.naam());
        w.key("versions").beginArray();
        for (ResolvedUniciteit u : vorm.versies()) {
            // IR-SPEC ch. 3.6: the binding IS the scoping, so a scope the resolver did not bind cannot be written
            // without widening the uniqueness to every instance
            if (u.scopeRol() == null && u.scopeVeld() != null)
                throw new IllegalStateException("IrEmitter: de uniciteit '" + u.naam() + "' is begrensd tot '"
                        + u.scopeVeld() + "', en de resolver vond daarvoor geen unieke rol");
            w.beginObject();
            w.key("sources").beginArray();
            source(u.naam(), naamVan(u.subject()), u.sleutels());
            w.endArray();
            if (u.scopeRol() != null) { w.key("scope"); rolBinding(u.scopeRol()); }
            w.key("validity"); validity(u.geldigheid());
            bronrecord(u.bron().snippet());
            w.endObject();
        }
        w.endArray();
        w.endObject();
    }

    /** The union form of IR-SPEC ch. 3.6: a version with one source per side. It carries no scope (a scoped union is
     *  an open design question), and the model's union record has none. */
    private void uniquenessUnion(Vorm<ResolvedUniciteitUnie> vorm) {
        w.beginObject();
        w.key("node").value("Uniqueness");
        w.key("name").value(vorm.naam());
        w.key("versions").beginArray();
        for (ResolvedUniciteitUnie u : vorm.versies()) {
            if (u.bronnen() == null || u.bronnen().isEmpty())
                throw new IllegalStateException("IrEmitter: de uniciteit '" + u.naam() + "' heeft geen bronnen; een "
                        + "uniciteit heeft er minstens één");
            w.beginObject();
            w.key("sources").beginArray();
            for (ResolvedUniciteitUnie.Bron b : u.bronnen()) source(u.naam(), naamVan(b.type()), b.sleutels());
            w.endArray();
            w.key("validity"); validity(u.geldigheid());
            bronrecord(u.bron().snippet());
            w.endObject();
        }
        w.endArray();
        w.endObject();
    }

    /** One source of a uniqueness: the object type and its key attributes, at least one. */
    private void source(String uniciteit, String objectType, List<String> sleutels) {
        if (sleutels == null || sleutels.isEmpty())
            throw new IllegalStateException("IrEmitter: een bron van de uniciteit '" + uniciteit + "' heeft geen "
                    + "sleutels; een bron heeft er minstens één");
        w.beginObject();
        w.key("node").value("Source");
        w.key("objectType").value(objectType);
        w.key("keys"); stringList(sleutels);
        w.endObject();
    }

    /** The range of an instance fold: a one-step {@code rol} or a multi-step {@code pad} (never both), and
     *  the name the single source is written with ({@code bronNaam}, null for the record's own). A fold
     *  whose source is the subject's own attribute has neither. */
    private record Vouwbereik(RegelSpraakModel.RolBindingRef rol, List<RegelSpraakModel.RolBindingRef> pad,
                              String bronNaam) {}

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
     * total over the subject's own timeline); a fold of another kind over it has no spelling.
     */
    private Vouwbereik vouwbereik(Expr.Functie f) {
        if (f.elementObjecttype() == null || f.argumenten().size() != 1
                || !(f.argumenten().get(0) instanceof Expr.AttribuutRef arg)) return null;
        boolean telbron = arg.type() instanceof Datatype.Onbekend;
        if (arg.navigatiePad() != null && !arg.navigatiePad().isEmpty())
            throw vouwWeigering(f, "haar bron draagt een geschreven pad; de relatie van een vouwbron is het bereik");
        if (arg.navigatieRol() != null && !(telbron && zelfaanduidend(huidigSubject, arg.navigatieRol())))
            throw vouwWeigering(f, "haar bron draagt de geschreven stap '" + arg.navigatieRol()
                    + "', en alleen een telbron die het onderwerp zelf aanduidt mag er een dragen");
        if (!telbron && !arg.set()) {
            if (f.kind() == Expr.FunctieKind.TOTAAL) return new Vouwbereik(null, List.of(), null);
            throw vouwWeigering(f, "haar bron is het eigen attribuut van het onderwerp en geen instantiebron");
        }
        if (m == null || huidigSubject == null)
            throw vouwWeigering(f, "er is geen onderwerp om haar bereik vanaf te bepalen");
        List<RegelSpraakModel.RolBindingRef> stappen = arg.set() && arg.bereikPad() != null ? arg.bereikPad() : List.of();
        if (!stappen.isEmpty())
            return new Vouwbereik(null, stappen, telbronNaam(f, arg, stappen.get(stappen.size() - 1)));
        RegelSpraakModel.RolBindingRef rb;
        if (telbron) {
            rb = m.elementRolBinding(huidigSubject, arg.attribuut());
            if (rb == null) rb = m.verbindendeRolBinding(huidigSubject, f.elementObjecttype());
        } else {
            // the by-pair-then-written-name search lives in the model: the §8.2 quantifier fold's range is
            // derived at resolve time and must classify a written range exactly as this does
            rb = m.vouwbronBinding(huidigSubject, f.elementObjecttype(), arg.onderwerpBron());
        }
        if (rb == null)
            throw vouwWeigering(f, "geen unieke relatie van " + huidigSubject.naam + " naar "
                    + f.elementObjecttype().naam + " draagt haar bereik");
        return new Vouwbereik(rb, List.of(), telbronNaam(f, arg, rb));
    }

    private static IllegalStateException vouwWeigering(Expr.Functie f, String reden) {
        return new IllegalStateException("IrEmitter: de " + soort(f) + "-vouw '" + f.bron().snippet()
                + "' heeft geen spelling in uchar-ir-v1: " + reden);
    }

    /** Under AANTAL the source names the role the range binds (WF-047), by declared name or declared plural.
     *  A written name that does neither — a best-effort plural, the plural of the element type — is written
     *  as the role's declared name, and the written spelling stays in the source snippet. A name that is a member
     *  of the element type makes an attribute source, which WF-093 judges; it is written as it stands, as is
     *  every source under another kind. Null: the record's own name. */
    private String telbronNaam(Expr.Functie f, Expr.AttribuutRef arg, RegelSpraakModel.RolBindingRef rb) {
        if (f.kind() != Expr.FunctieKind.AANTAL || arg.attribuut() == null) return null;
        Rol rol = gebondenRol(rb);
        if (rol == null || duidtAan(rol, arg.attribuut()) || isLid(f.elementObjecttype(), arg.attribuut())) return null;
        return rol.naam();
    }

    private void rolBinding(RegelSpraakModel.RolBindingRef rb) {
        w.beginObject();
        w.key("node").value("RoleBinding");
        w.key("factType").value(rb.feittype());
        w.key("role").value(rb.rol());
        w.endObject();
    }

    /** The {@code path} of a value/condition-position attribute read: one RoleBinding per
     *  navigation hop, subject-outward, each naming the reached (far) role. Three shapes: a declared
     *  CHAIN (one binding per step, whole-or-not — all hops derivable, or none), a single by-NAME hop,
     *  and a by-PAIR read of an off-subject type. Empty when the read does not navigate, no subject
     *  anchors the walk, or no hop set is uniquely derivable; {@link #eigenLezing} then decides whether
     *  the bindingless read is the subject's own or has no spelling. */
    private List<RegelSpraakModel.RolBindingRef> navigatieBindingen(Expr.AttribuutRef a) {
        return navigatieBindingen(m, huidigSubject, a);
    }

    /** As {@link #navigatieBindingen(Expr.AttribuutRef)}, from {@code subject}: the resolver names the hops of a read
     *  it refuses by the path this emitter would write for it. */
    public static List<RegelSpraakModel.RolBindingRef> navigatieBindingen(RegelSpraakModel m, Objecttype subject,
                                                                         Expr.AttribuutRef a) {
        if (m == null || subject == null || a.objecttype() == null) return List.of();
        if (a.navigatiePad() != null && !a.navigatiePad().isEmpty()) {        // §13.4.1 declared chain
            List<Objecttype> bereikt = m.volgKeten(subject, a.navigatiePad());
            if (bereikt == null) return List.of();
            List<RegelSpraakModel.RolBindingRef> uit = new java.util.ArrayList<>();
            Objecttype cur = subject;
            for (int i = 0; i < a.navigatiePad().size(); i++) {
                RegelSpraakModel.RolBindingRef b =
                        m.verbindendeRolViaNaamBinding(cur, bereikt.get(i), a.navigatiePad().get(i));
                if (b == null) return List.of();                             // heel-of-niet over the chain
                uit.add(b);
                cur = bereikt.get(i);
            }
            return uit;
        }
        if (a.navigatieRol() != null) {                                      // single-hop by-name
            RegelSpraakModel.RolBindingRef b =
                    m.verbindendeRolViaNaamBinding(subject, a.objecttype(), a.navigatieRol());
            return b == null ? List.of() : List.of(b);
        }
        if (a.objecttype() != subject) {                                     // by-pair (off-subject type)
            RegelSpraakModel.RolBindingRef b = m.verbindendeRolBinding(subject, a.objecttype());
            return b == null ? List.of() : List.of(b);
        }
        return List.of();                                                    // own attribute — not navigated
    }

    /** The {@code navigation} of a navigated §8.1.8 kenmerk check: the single reached role, found
     *  exactly as {@code RuleChecker.navigatieKenmerk} finds it (the target type via the written role, then
     *  the by-name binding). Null when the check does not navigate or the hop is not uniquely derivable. */
    private RegelSpraakModel.RolBindingRef navigatieBinding(Conditie.Kenmerk k) {
        if (m == null || huidigSubject == null || k.navigatie() == null) return null;
        Objecttype doel = m.rolDoeltypeNaarSubject(huidigSubject, k.navigatie());
        return doel == null ? null : m.verbindendeRolViaNaamBinding(huidigSubject, doel, k.navigatie());
    }

    /**
     * IR-SPEC ch. 4.1a: a read without bindings is the subject's own. It must be one — the type it names is
     * the subject's and nothing was written that walks away from the subject — or the model is refused,
     * because a reader would take the read for the subject's own attribute. The one written step admitted
     * is the zero-hop self-reference ("het strandtegoed van de aanmelder", read on the aanmelder): a single
     * written role that reaches no far role from the subject and that the subject's type itself plays.
     */
    private void eigenLezing(Expr.AttribuutRef a) {
        boolean pad = a.navigatiePad() != null && !a.navigatiePad().isEmpty();
        boolean geschreven = a.navigatieRol() != null || pad;
        boolean vreemdType = huidigSubject != null && a.objecttype() != null && a.objecttype() != huidigSubject;
        if (!geschreven && !vreemdType) return;
        if (!pad && a.navigatieRol() != null && huidigSubject != null && a.objecttype() == huidigSubject
                && m.rolTreffersNaarSubject(huidigSubject, a.navigatieRol()) == 0
                && zelfaanduidend(huidigSubject, a.navigatieRol())) return;
        throw new IllegalStateException("IrEmitter: de lezing '" + a.bron().snippet() + "' van "
                + naamVan(a.objecttype()) + " vanaf " + (huidigSubject == null ? "geen onderwerp" : huidigSubject.naam)
                + " navigeert, en de resolver vond daarvoor geen unieke binding");
    }

    /** Whether the type {@code subject} itself plays a role that {@code naam} denotes (declared name or
     *  declared plural, under the verifier's name identity) — the self-denotation of WF-053's ch. 4.1a
     *  reading, on the declared filler type exactly. */
    private boolean zelfaanduidend(Objecttype subject, String naam) {
        if (m == null || subject == null || naam == null) return false;
        for (Feittype ft : m.feittypen.values())
            for (Rol r : ft.rollen)
                if (r.objecttypeNaam() != null && norm(r.objecttypeNaam()).equals(norm(subject.naam))
                        && duidtAan(r, naam)) return true;
        return false;
    }

    /** A role check as the wire carries it: the binding (the fact type and the role the subject plays) and,
     *  where the check narrows the other end to a strict subtype of that role's declared filler, the type it
     *  narrows to (null: no narrowing). */
    private record Lidmaatschap(RegelSpraakModel.RolBindingRef binding, String tegenpartij) {}

    /** The role check with the subject on the near side ("hij is een wachtjutter"): the fact type in which
     *  the subject plays the written role and {@code anderTeFiller} fills the other. The written name denotes
     *  the role by declared name or declared plural; a check the resolver bound in any other way has no
     *  binding to write, and is refused. */
    private Lidmaatschap lidmaatschapNabij(Conditie.Rollidmaatschap r) {
        Lidmaatschap gevonden = null;
        int n = 0;
        if (m != null && huidigSubject != null && r.rol() != null && r.anderTeFiller() != null)
            for (Feittype ft : m.feittypen.values()) {
                if (ft.wederkerig || ft.rollen.size() != 2) continue;
                for (int i = 0; i < 2; i++) {
                    Rol eigen = ft.rollen.get(i), ander = ft.rollen.get(1 - i);
                    if (duidtAan(eigen, r.rol()) && vult(huidigSubject, eigen) && vult(r.anderTeFiller(), ander)) {
                        gevonden = lidmaatschap(ft, eigen, ander, r.anderTeFiller());
                        n++;
                    }
                }
            }
        if (n != 1)
            throw new IllegalStateException("IrEmitter: de rolcheck '" + r.bron().snippet() + "' bindt " + n
                    + " rollen '" + r.rol() + "' die " + (huidigSubject == null ? "het onderwerp" : huidigSubject.naam)
                    + " vult tegenover " + naamVan(r.anderTeFiller()) + " (gedeclareerde naam of meervoud); de wire "
                    + "eist er precies één");
        return gevonden;
    }

    /** The role check with the subject on the far side ("de wachtpost heeft een wachtjutter"): the fact type
     *  in which the subject plays the role {@code backref} names and {@code filler} fills the other. */
    private Lidmaatschap lidmaatschapVer(Conditie.VerreRollidmaatschap r) {
        Lidmaatschap gevonden = null;
        int n = 0;
        if (m != null && huidigSubject != null && r.backref() != null && r.filler() != null)
            for (Feittype ft : m.feittypen.values()) {
                if (ft.wederkerig && ft.rollen.size() == 1) {
                    Rol rol = ft.rollen.get(0);
                    if (duidtAan(rol, r.backref()) && vult(huidigSubject, rol) && vult(r.filler(), rol)) {
                        gevonden = lidmaatschap(ft, rol, rol, r.filler());
                        n++;
                    }
                    continue;
                }
                if (ft.rollen.size() != 2) continue;
                for (int i = 0; i < 2; i++) {
                    Rol eigen = ft.rollen.get(i), ander = ft.rollen.get(1 - i);
                    if (duidtAan(eigen, r.backref()) && vult(huidigSubject, eigen) && vult(r.filler(), ander)) {
                        gevonden = lidmaatschap(ft, eigen, ander, r.filler());
                        n++;
                    }
                }
            }
        if (n != 1)
            throw new IllegalStateException("IrEmitter: de rolcheck '" + r.bron().snippet() + "' bindt " + n
                    + " rollen '" + r.backref() + "' die " + (huidigSubject == null ? "het onderwerp" : huidigSubject.naam)
                    + " speelt tegenover " + naamVan(r.filler()) + "; de wire eist er precies één");
        return gevonden;
    }

    /** The check's other end narrows only where it names a STRICT subtype of the other role's declared filler;
     *  naming the declared type itself is no narrowing, and the field then stays absent. */
    private Lidmaatschap lidmaatschap(Feittype ft, Rol eigen, Rol ander, Objecttype tegenpartij) {
        Objecttype gedeclareerd = m.objecttypeOpNaam(ander.objecttypeNaam());
        return new Lidmaatschap(new RegelSpraakModel.RolBindingRef(ft.naam, eigen.naam()),
                tegenpartij == gedeclareerd ? null : tegenpartij.naam);
    }

    private void rolLidmaatschap(Lidmaatschap l, boolean negatie, Bron bron) {
        w.key("node").value("RoleMembership");
        w.key("factType").value(l.binding().feittype());
        w.key("role").value(l.binding().rol());
        if (l.tegenpartij() != null) w.key("counterpartType").value(l.tegenpartij());
        w.key("negated").value(negatie);
        bronrecord(bron.snippet());
    }

    /** Whether {@code ot} fills {@code r}: its declared filler type or a subtype of it (IR-SPEC ch. 2.6). */
    private boolean vult(Objecttype ot, Rol r) {
        Objecttype filler = m.objecttypeOpNaam(r.objecttypeNaam());
        return ot != null && filler != null && ot.isEen(filler);
    }

    /** The declared role a binding names, under the verifier's name identity; null when it names none or
     *  several. */
    private Rol gebondenRol(RegelSpraakModel.RolBindingRef rb) {
        if (m == null || rb == null || rb.feittype() == null || rb.rol() == null) return null;
        for (Feittype ft : m.feittypen.values())
            if (norm(ft.naam).equals(norm(rb.feittype()))) {
                List<Rol> treffers = ft.rollen.stream()
                        .filter(r -> r.naam() != null && norm(r.naam()).equals(norm(rb.rol()))).toList();
                return treffers.size() == 1 ? treffers.get(0) : null;
            }
        return null;
    }

    /** Whether {@code naam} denotes {@code r}: its declared name or its declared plural. */
    private static boolean duidtAan(Rol r, String naam) {
        return naam != null && ((r.naam() != null && norm(r.naam()).equals(norm(naam)))
                || (r.meervoud() != null && norm(r.meervoud()).equals(norm(naam))));
    }

    /** Whether {@code naam} is an attribute or a kenmerk of {@code ot}, own or inherited. */
    private static boolean isLid(Objecttype ot, String naam) {
        if (ot == null || naam == null) return false;
        for (Objecttype t : ot.keten()) {
            for (String a : t.attributen.keySet()) if (norm(a).equals(norm(naam))) return true;
            for (String k : t.kenmerken) if (norm(k).equals(norm(naam))) return true;
        }
        return false;
    }

    /** The verifier's name identity ({@code checks.ml norm}): runs of spaces and tabs collapse to one space
     *  and only A-Z fold to lower case. Public for the resolver, which refuses a second declaration of one unit
     *  abbreviation under this same identity before a model can reach the emitter's own check. */
    public static String norm(String s) {
        StringBuilder uit = new StringBuilder();
        for (String woord : s.split("[ \t]+")) {
            if (woord.isEmpty()) continue;
            if (uit.length() > 0) uit.append(' ');
            for (int i = 0; i < woord.length(); i++) {
                char c = woord.charAt(i);
                uit.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
            }
        }
        return uit.toString();
    }

    /** IR-SPEC ch. 3.4: a fact creation and its versions. */
    private void factCreation(Vorm<ResolvedFeitCreatie> vorm) {
        w.beginObject();
        w.key("node").value("FactCreation");
        w.key("name").value(vorm.naam());
        w.key("versions").beginArray();
        for (ResolvedFeitCreatie fc : vorm.versies()) factCreationVersion(fc);
        w.endArray();
        w.endObject();
    }

    /** The creation is its symbolic block — the created relation, both its roles and the path from the anchor, one
     *  binding per hop with the last reaching the target type — so a creation the resolver could not bind whole has
     *  no document. The target type is not written: a reader takes the declared filler of the target role, so the
     *  model's target type must be that filler, or the document would name another one. */
    private void factCreationVersion(ResolvedFeitCreatie fc) {
        if (fc.feittype() == null || fc.ankerRol() == null || fc.doelRol() == null || fc.joinRol() == null
                || fc.tussenstappen().stream().anyMatch(t -> t.rolBinding() == null))
            throw new IllegalStateException("IrEmitter: de feitcreatie '" + fc.naam() + "' is niet heel symbolisch "
                    + "gebonden (het feittype, beide rollen en elke stap van het pad); zonder dat blok heeft ze "
                    + "geen spelling in uchar-ir-v1");
        Feittype ft = m.feittypen.get(fc.feittype());
        List<Rol> doelrollen = ft == null ? List.of()
                : ft.rollen.stream().filter(r -> fc.doelRol().equals(r.naam())).toList();
        if (doelrollen.size() != 1 || !java.util.Objects.equals(doelrollen.get(0).objecttypeNaam(), naamVan(fc.doel())))
            throw new IllegalStateException("IrEmitter: de feitcreatie '" + fc.naam() + "' maakt feiten naar '"
                    + naamVan(fc.doel()) + "', en de wire schrijft die doelsoort niet: een lezer neemt de gedeclareerde "
                    + "vuller van de rol '" + fc.doelRol() + "' in '" + fc.feittype() + "'"
                    + (doelrollen.size() == 1 ? ", en dat is '" + doelrollen.get(0).objecttypeNaam() + "'"
                            : ", en die rol is er " + doelrollen.size() + " keer"));
        huidigSubject = fc.anker();   // the §13.4.7 guard navigates from the anker
        w.beginObject();
        w.key("factType").value(fc.feittype());
        w.key("anchorRole").value(fc.ankerRol());
        w.key("targetRole").value(fc.doelRol());
        w.key("anchor").value(naamVan(fc.anker()));
        w.key("path").beginArray();
        for (ResolvedFeitCreatie.Tussenstap t : fc.tussenstappen()) rolBinding(t.rolBinding());
        rolBinding(fc.joinRol());
        w.endArray();
        if (fc.voorwaarde() != null) { w.key("condition"); conditie(fc.voorwaarde()); }
        w.key("validity"); validity(fc.geldigheid());
        w.endObject();
        huidigSubject = null;
    }

    private void startpunt(ResolvedStartpunt s) {
        w.beginObject();
        w.key("node").value("StartPointDetermination");
        w.key("timeline").value(s.tijdlijn());
        w.key("objectType").value(s.objecttype());
        w.key("member").value(s.lid());
        w.endObject();
    }

    private void regelGroep(ResolvedRegelGroep g) {
        w.beginObject();
        w.key("node").value("RuleGroup");
        w.key("name").value(g.naam());
        w.key("recursive").value(g.recursief());
        w.key("members"); stringList(g.leden());
        bronrecord(g.bron().snippet(), g.line());
        w.endObject();
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
    private void geldigheid(Geldigheid g) {
        if (g == null) { w.valueNull(); return; }
        w.beginObject();
        w.key("node").value("Validity");
        if (g.vanaf() != null) w.key("from").value(g.vanaf().toString());
        if (g.tot() != null) w.key("to").value(g.tot().toString());
        w.endObject();
    }

    // --- shared helpers ----------------------------------------------------

    private static String naamVan(Objecttype ot) { return ot == null ? null : ot.naam; }

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
     * separator and the following value is written inline.
     */
    private static final class Json {
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
