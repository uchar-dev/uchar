package dev.uchar.regelspraak.model;

import dev.uchar.regelspraak.message.Component;
import dev.uchar.regelspraak.message.Msg;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The resolved IR: the typed model a RegelSpraak document denotes, plus any
 * diagnostics raised while building and type-checking it. This is what the
 * transpiler front-end hands on to the Drools back-end.
 */
public final class RegelSpraakModel {
    public final Map<String, ObjectType> objectTypes = new LinkedHashMap<>();
    public final Map<String, Datatype> domains = new LinkedHashMap<>();
    public final Map<String, Parameter> parameters = new LinkedHashMap<>();
    public final Map<String, UnitSystem> unitSystems = new LinkedHashMap<>();
    public final Map<String, FactType> factTypes = new LinkedHashMap<>();
    public final List<String> dayKinds = new ArrayList<>();
    /** §3.12 a dagsoort may be the EENHEID of a Numeriek attribute ("5 werkdagen"); maps every unit spelling
     *  (the singular name AND its declared meervoud, lowercased) to the dagsoort's singular name, so the §6
     *  duration paths can recognise a dagsoort unit. */
    public final Map<String, String> dayKindPerUnit = new LinkedHashMap<>();
    /** The §3.12 dagsoort whose unit spelling is {@code abbreviation} (singular or meervoud, case-insensitive), or
     *  null when the unit is not a dagsoort — the §6.10/§6.11 transpilers' dagsoort-unit recognition. */
    public String dayKindForUnit(String abbreviation) {
        return abbreviation == null ? null : dayKindPerUnit.get(abbreviation.trim().toLowerCase());
    }
    public final List<Timeline> timelines = new ArrayList<>();
    public final Map<String, Dimension> dimensions = new LinkedHashMap<>();
    public final List<Rule> rules = new ArrayList<>();
    /** Resolved rule IR (typed expressions + bound references) — the transpiler's input. */
    public final List<dev.uchar.regelspraak.ir.ResolvedRule> resolvedRules = new ArrayList<>();
    /** Resolved §9.8 dagsoortdefinities (dagsoort name + condition over "de dag") — each becomes a
     *  per-date boolean predicate the transpilers compile for §8.1.5 controls and the §6 durations. */
    public final List<dev.uchar.regelspraak.ir.ResolvedDayKindDefinition> resolvedDayKindDefinitions = new ArrayList<>();
    /** Resolved object-creation rules (§13.4.6) — a separate IR shape (new object + attributes). */
    public final List<dev.uchar.regelspraak.ir.ResolvedObjectCreation> objectCreations = new ArrayList<>();
    /** Resolved distribution rules (§13.4.10) — source amount spread over recipients. */
    public final List<dev.uchar.regelspraak.ir.ResolvedAllocation> allocations = new ArrayList<>();
    /** Resolved consistency rules (§13.4.8) — an assertion that must hold, else inconsistent. */
    public final List<dev.uchar.regelspraak.ir.ResolvedConsistency> consistencies = new ArrayList<>();
    /** Resolved §8.1.6 uniqueness checks — a key that must be unique across instances, else inconsistent. */
    public final List<dev.uchar.regelspraak.ir.ResolvedUniqueness> uniquenesses = new ArrayList<>();
    /** Resolved §8.1.6 cross-type "verenigd met" uniqueness — a key unique across the union of types. */
    public final List<dev.uchar.regelspraak.ir.ResolvedUniquenessUnion> uniquenessUnions = new ArrayList<>();
    /** Resolved §13.4.7 feitcreatie rules — a new feittype instance (a reference field) linking two existing objects. */
    public final List<dev.uchar.regelspraak.ir.ResolvedFactCreation> factCreations = new ArrayList<>();
    /** Resolved §9.9/§13.4.12 startpuntbepalingen — the anchor member per (objecttype, variable tijdlijn);
     *  the anchor WRITE itself is an ordinary entry in {@link #resolvedRules}. */
    public final List<dev.uchar.regelspraak.ir.ResolvedStartPointDetermination> resolvedStartPointDeterminations = new ArrayList<>();
    /** §9.10 Regelgroepen — a DOCUMENTED EXTENSION (see grammar/DIVERGENCES.md). Each carries its member
     *  RULE NAMES (the bare written names); members otherwise resolve exactly as ungrouped rules. Both back-ends
     *  prefix ONLY the engine-visible rule-name header with "<groep>: " — every data key stays the bare name. */
    public final List<dev.uchar.regelspraak.ir.ResolvedRuleGroup> ruleGroups = new ArrayList<>();
    /** §4 (PRIORITIZED-DEFAULTS-EVOLUTIE) the PrioritizedDefaults nodes lowered from the uchar-dialect
     *  'Uitzondering op' / 'Uitzonderingen ongeordend' extension — the ONLY path by which the RegelSpraak
     *  frontend produces defaults groups (the flat v0-mirror rules never do). Empty for every document without
     *  the extension, so the emitter omits the {@code prioritizedDefaults} key there (the v0-mirror
     *  byte-identity of all flat goldens). */
    public final List<dev.uchar.regelspraak.ir.PrioritizedDefaults> prioritizedDefaults = new ArrayList<>();
    /** IR-SPEC ch. 2.12 the named vocabularies, in declaration order: with {@link #designations} a side table without
     *  semantics, which no transpiler reads. The frontend fills both from the dialect's 'Vocabulaire' blocks and
     *  annotations (grammar/DIVERGENCES.md, "Vocabulaires en benamingen"), and composes the designations of the
     *  elements a lowered opgave marker generates from the marked attribute's, and those of the group of a subtype's
     *  blocks and of the member and rule of a fold with steps after the set part by part from the declarations their
     *  name is made of ("Gegenereerde elementen"); the Drools IR loader fills both from the wire; a
     *  document without vocabularies leaves both empty, and the emitter then writes neither. Read them through
     *  {@link #designation}, {@link #effectiveName} and {@link #namedElement}. */
    public final List<Vocabulary> vocabularies = new ArrayList<>();
    /** IR-SPEC ch. 2.12 the designations of every declaration that carries one, each list in written order. */
    public final Map<Designatable, List<Designation>> designations = new LinkedHashMap<>();
    /** Every element of {@link #designatable()} the frontend made itself, under a name no declaration in the source
     *  carries, with the declared element it comes from and the form that made it (grammar/DIVERGENCES.md,
     *  "Gegenereerde elementen"). A side table without semantics, like {@link #designations}: no transpiler reads it.
     *  The emitter writes each entry as the field {@code origin} of the element's node (IR-SPEC ch. 2.13), and the
     *  Drools IR loader reads it back, so a model read from the wire has the table the frontend's had. An element
     *  whose name the author wrote has no entry, also where the frontend made the element (a defaults group named
     *  after its base rule). Keyed as {@link #designatable()} spells the element; read it through
     *  {@link #generatedFrom}. */
    public final Map<Designatable, GeneratedFrom> generated = new LinkedHashMap<>();
    public final List<Diagnostic> diagnostics = new ArrayList<>();

    /** A copy of this model that holds every element of every collection (the elements are shared). The scoped IR
     *  document of the scenario lowering starts from it and drops only the forms its scope does not carry. The copy
     *  walks the public collection fields by reflection, so a collection the model gains later is copied without a
     *  second edit: a field-by-field copy left such a collection empty in every scoped document, with no test to
     *  notice. */
    @SuppressWarnings("unchecked")
    public RegelSpraakModel copy() {
        RegelSpraakModel k = new RegelSpraakModel();
        for (java.lang.reflect.Field f : RegelSpraakModel.class.getFields()) {
            if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
            try {
                Object van = f.get(this), to = f.get(k);
                if (van instanceof List<?> l) ((List<Object>) to).addAll(l);
                else if (van instanceof Map<?, ?> mp) ((Map<Object, Object>) to).putAll(mp);
                else throw new IllegalStateException("RegelSpraakModel.kopie: het veld '" + f.getName()
                        + "' is geen lijst of kaart en heeft geen kopieerregel");
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("RegelSpraakModel.kopie: het veld '" + f.getName() + "'", e);
            }
        }
        return k;
    }

    /** §9.10 the group a rule belongs to, given an EMITTED rule name — which is the bare member name, optionally
     *  with a §13.4.2.3 version suffix ("<naam> #k") or a back-end-derived qualifier ("<naam> (onverdeelde rest)").
     *  Matches the LONGEST group member name that is a prefix of {@code emittedName} at a token boundary (end,
     *  " #", or " ("), so a member "R" never spuriously captures an ungrouped "R2". Null when the rule is ungrouped
     *  — the back-ends then emit the bare header. */
    public dev.uchar.regelspraak.ir.ResolvedRuleGroup ruleGroupOf(String emittedName) {
        if (emittedName == null) return null;
        dev.uchar.regelspraak.ir.ResolvedRuleGroup best = null;
        int bestLen = -1;
        for (var g : ruleGroups)
            for (String member : g.members())
                if (member.length() > bestLen && emittedName.equals(member)
                        || member.length() > bestLen && (emittedName.startsWith(member + " #") || emittedName.startsWith(member + " ("))) {
                    best = g; bestLen = member.length();
                }
        return best;
    }

    /** §9.10 fase-2: whether the EMITTED rule {@code emittedName} belongs to a {@code is recursief} Regelgroep.
     *  A recursief-group objectcreatie must forward-chain on its own inserts to reach the fixpoint, so both back-ends
     *  drop {@code no-loop} for it — sound because the §13.4.6/§9 idempotence guard is the anti-runaway control. */
    public boolean isRecursiveGroupMember(String emittedName) {
        var g = ruleGroupOf(emittedName);
        return g != null && g.recursive();
    }

    /** §9.10 fase-1 namespacing: the ENGINE-VISIBLE rule-name header for an emitted rule name — "<groep>: <naam>"
     *  when the rule belongs to a Regelgroep, else the bare name. Called by both back-ends at (and only at) the
     *  point a rule name becomes an engine header; every data key keeps the bare name. */
    public String emittedRuleName(String emittedName) {
        var g = ruleGroupOf(emittedName);
        return g == null ? emittedName : g.name() + ": " + emittedName;
    }

    /** Normalise a tijdlijn name for cross-reference matching: collapse internal whitespace (names may wrap
     *  across source lines), lowercase, and strip a single leading lidwoord (the definition writes "de
     *  periode …", a reference "elke periode …" / "een periode …"). */
    public static String normaliseTimelineName(String s) {
        String n = s.trim().replaceAll("\\s+", " ").toLowerCase();
        for (String lw : new String[]{"de ", "het ", "een "})
            if (n.startsWith(lw)) return n.substring(lw.length());
        return n;
    }

    /** The FIRST declared tijdlijn whose normalised name matches {@code naam} (the same first-match rule
     *  the §13.3.6.10 attribute adoption uses), or null. */
    public Timeline timelineByName(String name) {
        if (name == null) return null;
        String ref = normaliseTimelineName(name);
        return timelines.stream().filter(tl -> normaliseTimelineName(tl.name()).equals(ref))
                .findFirst().orElse(null);
    }

    /** §3.8 whether a temporal attribute rides a NAMED tijdlijn declared "met variabel startpunt".
     *
     *  <p>Reads the OWN declaration deliberately, and it is the one temporality lookup that does. The
     *  §9.9 anchor family is not a member lookup but a protocol: a startpuntbepaling DECLARES a
     *  synthetic attribute on a named subject, {@link #startPointMember} and {@link #isStartPointMember} key on
     *  that subject's name, and {@code dev.uchar.regelspraak.resolver.RuleChecker} reserves the
     *  name and writes the attribute. Resolving one of the four through the chain while the others key
     *  on the declared name would leave the anchor half-reachable. Closing the family is a change with
     *  its own measurement (its consumers are all in the back-ends, where no corpus witness exercises a
     *  subtype yet) — see the card note. */
    public boolean hasVariableStart(ObjectType ot, String attribute) {
        String tlName = ot.attributeTimelineName.get(attribute);
        Timeline tl = tlName == null ? null : timelineByName(tlName);
        return tl != null && tl.variableStartPoint();
    }

    /** §9.9 the startpunt-anchor attribute a variable-startpunt temporal doel buckets at, or null (a
     *  fixed/inline tijdlijn, or no startpuntbepaling resolved for the pair — the back-ends then marker). */
    public String startPointAnchorMember(ObjectType ot, String attribute) {
        if (!hasVariableStart(ot, attribute)) return null;
        return startPointMember(ot, ot.attributeTimelineName.get(attribute));   // own, with the family above
    }

    /** RUNTIME-SEMANTICS ch. 3.2 the constant anchor a temporal doel on a FIXED start point buckets at: the date
     *  its walls are computed from, or null on the calendar grid. The bucketing producers run on a month or year
     *  doel of one unit only, and there the start point is a day-month (WF-115). */
    public java.time.LocalDate fixedStartPointAnchor(ObjectType ot, String attribute) {
        String fixed = ot.fixedStartPointOf(attribute);
        if (fixed == null) return null;
        dev.uchar.regelspraak.core.FixedStartPoint.Lexeme l = dev.uchar.regelspraak.core.FixedStartPoint.parse(fixed);
        if (l == null)
            throw new IllegalStateException("fixed start point '" + fixed + "' of '" + attribute + "' is no lexeme of "
                    + "WF-115, which the loader refuses");
        return dev.uchar.regelspraak.core.FixedStartPoint.anchor(l);
    }

    /** §9.9 whether {@code member} is the synthetic startpunt-anchor attribute of {@code objectType} — the
     *  back-ends key the §6.8 leeg-fout companion on it. */
    public boolean isStartPointMember(ObjectType objectType, String member) {
        return objectType != null && resolvedStartPointDeterminations.stream()
                .anyMatch(sp -> sp.objectType().equals(objectType.name) && sp.member().equals(member));
    }

    /** §9.9 the synthetic anchor-attribute name for {@code objectType} × variable tijdlijn {@code timelineName},
     *  or null when no startpuntbepaling was resolved for that pair. */
    public String startPointMember(ObjectType objectType, String timelineName) {
        if (objectType == null || timelineName == null) return null;
        String ref = normaliseTimelineName(timelineName);
        return resolvedStartPointDeterminations.stream()
                .filter(sp -> sp.objectType().equals(objectType.name) && normaliseTimelineName(sp.timeline()).equals(ref))
                .map(dev.uchar.regelspraak.ir.ResolvedStartPointDetermination::member).findFirst().orElse(null);
    }

    public boolean isOk() { return diagnostics.stream().noneMatch(Diagnostic::isError); }
    public List<Diagnostic> errors() { return diagnostics.stream().filter(Diagnostic::isError).toList(); }

    /** §8.1.9 whether {@code naam} names a rule that INSERTS §9.5 Inconsistentie flag facts — a
     *  consistentie, uniciteit, uniciteit-unie or (via the §9.7.6 Tabel-19 fout channel) verdeling.
     *  The "regelversie X is inconsistent" regelpredicaat lowers to an exists on that channel ONLY for
     *  a known producer; any other name would be a never-matching exists (a silent never-fire), so the
     *  back-ends decline it as an honest marker instead. */
    public boolean inconsistencyProducer(String name) {
        if (name == null) return false;
        return consistencies.stream().anyMatch(c -> name.equals(c.name()))
                || uniquenesses.stream().anyMatch(u -> name.equals(u.name()))
                || uniquenessUnions.stream().anyMatch(u -> name.equals(u.name()))
                || allocations.stream().anyMatch(v -> name.equals(v.name()));
    }

    /** §8.1.9 the rule names referenced by a live "regelversie &lt;X&gt; is gevuurd" predicate anywhere in the
     *  model — walked across every construct that carries a condition (a regel voorwaarde, a verdeling/creatie
     *  voorwaarde, a consistentie assertie). The transpilers emit a {@code RuleFired} marker in the consequence
     *  of a referenced rule ONLY (opt-in emission, no fact-base bloat), and lower the predicate to an
     *  {@code exists RuleFired} only for a name that is both referenced AND an instrumented producer. */
    public java.util.Set<String> firedReferences() {
        java.util.Set<String> result = new java.util.LinkedHashSet<>();
        for (var r : resolvedRules) dev.uchar.regelspraak.ir.Condition.collectFiredReferences(r.condition(), result);
        for (var v : allocations)    dev.uchar.regelspraak.ir.Condition.collectFiredReferences(v.condition(), result);
        for (var oc : objectCreations) dev.uchar.regelspraak.ir.Condition.collectFiredReferences(oc.condition(), result);
        for (var fc : factCreations)  dev.uchar.regelspraak.ir.Condition.collectFiredReferences(fc.condition(), result);
        for (var c : consistencies)  dev.uchar.regelspraak.ir.Condition.collectFiredReferences(c.assertion(), result);
        return result;
    }

    /** §8.1.9 whether {@code naam} names a rule the back-ends INSTRUMENT with a {@code RuleFired} marker — a
     *  main {@code regel()}-path rule (gelijkstelling / berekening / initialisatie / kenmerktoekenning), the
     *  common case whose consequence both back-ends unconditionally mark. "regelversie X is gevuurd" lowers to
     *  an {@code exists RuleFired} ONLY for such a name (else the exists would never match — a silent never-fire);
     *  a gevuurd-reference to any other kind (a consistentie/uniciteit rule, whose "result" is the Inconsistentie
     *  channel that "is inconsistent" reads, or an unknown name) stays an honest symmetric marker. This gates
     *  ONLY the lowering; emission is additionally gated on the name being in {@link #firedReferences()}. */
    public boolean firedProducer(String name) {
        if (name == null) return false;
        return resolvedRules.stream().anyMatch(r -> name.equals(r.name()) && firedInstrumentable(r.category()));
    }

    /** The §8.1.9 main-regel() rule kinds whose consequence carries a {@code RuleFired} insert (the scalar/kenmerk
     *  value writers routed through {@code regel()}); an objectcreatie/feitcreatie/verdeling/consistentie is out
     *  of this instrumented set (its status stays a marker). */
    private static boolean firedInstrumentable(dev.uchar.regelspraak.ir.ResolvedRule.Kind s) {
        return s == dev.uchar.regelspraak.ir.ResolvedRule.Kind.GELIJKSTELLING
                || s == dev.uchar.regelspraak.ir.ResolvedRule.Kind.BEREKENING
                || s == dev.uchar.regelspraak.ir.ResolvedRule.Kind.INITIALISATIE
                || s == dev.uchar.regelspraak.ir.ResolvedRule.Kind.KENMERKTOEKENNING;
    }

    /**
     * Resolve a subject noun to the object type it denotes (§13.4.1). The noun may
     * name an object type directly (case-insensitive: "de groep" → Ploeg) or be a
     * feittype role whose filler is that object type ("een deelnemer" → Persoon
     * via the role "deelnemer"). A role reference is often plural
     * ("deelnemers"); a simple Dutch +s/+en plural is matched against the singular
     * role name. The phrase is tried whole first, then with leading words dropped,
     * so a multi-word role ("groep met punten") binds before its head noun does.
     * Returns null when nothing matches — the caller then makes no type claim.
     */
    public ObjectType resolveObjectType(String name) {
        for (String kandidaat : nameCandidates(name)) {
            ObjectType direct = objectTypeByName(kandidaat);
            if (direct != null) return direct;
            ObjectType viaRole = objectTypeViaRole(kandidaat);
            if (viaRole != null) return viaRole;
        }
        return null;
    }

    /**
     * The role an object creation's written noun names (§13.4.6; IR-SPEC ch. 3.3 {@code connectingRole}): the
     * noun is read at the candidate {@link #resolveObjectType} resolves it at, and that candidate names a role when
     * it is the DECLARED name or plural of a role {@code createdType} fills in a fact type that also relates
     * {@code subject} — one such role, never a guessed plural. Null where the candidate names none or several: the
     * back-link is then the type-pair derivation of RUNTIME-SEMANTICS ch. 5.5, as it was before a role was written.
     * The emitter asks the same of a navigated read whose noun names the other type and whose type pair decides
     * nothing: a noun that is also a role name binds that role (grammar/DIVERGENCES.md, §13.4.1).
     */
    public RoleBindingRef namedRoleBinding(ObjectType subject, ObjectType createdType, String name) {
        if (subject == null || createdType == null) return null;
        for (String kandidaat : nameCandidates(name))
            if (objectTypeByName(kandidaat) != null || objectTypeViaRole(kandidaat) != null)
                return connectingRoleViaNameBinding(subject, createdType, kandidaat);
        return null;
    }

    /** The names {@link #resolveObjectType} tries for {@code naam}, in its order: the HEAD noun phrase
     *  before any " van " navigation ("deelnemers van de groep" names the deelnemer; "van de groep" only
     *  scopes which one), then that head with leading words dropped one at a time (a multi-word role like
     *  "groep met punten" binds before its head noun does). Shared so a caller that must reconcile the
     *  WRITTEN name with what the phrase bound tries exactly the forms the binding came from. */
    public static List<String> nameCandidates(String name) {
        if (name == null || name.isBlank()) return List.of();
        String[] words = name.trim().split("\\s+");
        int end = words.length;
        for (int i = 0; i < words.length; i++) if (words[i].equalsIgnoreCase("van")) { end = i; break; }
        List<String> result = new ArrayList<>();
        for (int start = 0; start < end; start++)
            result.add(String.join(" ", java.util.Arrays.copyOfRange(words, start, end)));
        return result;
    }

    /** The object type DIRECTLY named {@code naam} (case-insensitively), or null — a name match
     *  only, never a feittype role. Distinct from {@link #resolveObjectType}, which also reaches a
     *  type through a role; a feitcreatie back-reference needs the strict form to tell "de groep"
     *  (the iterated anker itself) from "de buurgroep" (a different group of the same type). */
    public ObjectType objectTypeByName(String name) {
        ObjectType exact = objectTypes.get(name);
        if (exact != null) return exact;
        for (var e : objectTypes.entrySet())
            if (e.getKey().equalsIgnoreCase(name)) return e.getValue();
        return null;
    }

    /** The object type a role name denotes: the filler of the roles whose DECLARED name or declared plural is
     *  {@code naam}, and only where no role declares it so, of the roles whose guessed plural (+s/+en) it is. One
     *  filler per pass or none: roles of one name with different fillers choose neither, so no position in the
     *  declaration order decides, and a guessed plural never defeats a declared form (grammar/DIVERGENCES.md
     *  §13.4.1). Until 2026-10-02 the first role in declaration order whose name or plural matched, a guessed plural
     *  included, decided. */
    private ObjectType objectTypeViaRole(String name) {
        ObjectType[] declared = rolesFiller(name, true);
        if (declared != null) return declared[0];
        ObjectType[] derived = rolesFiller(name, false);
        return derived == null ? null : derived[0];
    }

    /** The one filler of the roles {@code naam} names by their declared forms ({@code declared}) or by a guessed
     *  plural: a one-element array holding it, or holding null when those roles have different fillers; null when no
     *  role is named so. A role whose filler is no declared object type is skipped. */
    private ObjectType[] rolesFiller(String name, boolean declared) {
        ObjectType[] found = null;
        for (FactType ft : factTypes.values())
            for (Role r : ft.roles) {
                if (!(declared ? roleMatchesExact(r, name) : roleMatchesBestEffort(r, name))) continue;
                ObjectType fillerType = objectTypeByName(r.objectTypeName());
                if (fillerType == null) continue;
                if (found == null) found = new ObjectType[] {fillerType};
                else if (found[0] != fillerType) found[0] = null;
            }
        return found;
    }

    /**
     * The role name to navigate from {@code houder} to {@code target} — the
     * reference field on a {@code houder} fact that points at a {@code target},
     * which is the role {@code target} plays in a feittype that also relates a
     * {@code houder}. So from a deelnemer (Persoon) to its group (Ploeg)
     * this is "groep". Returns null when there is no such feittype or more than one
     * (ambiguous) — the caller then emits a marker rather than a wrong join. This is
     * the by-pair fallback used when the source did not name the role; prefer
     * {@link #connectingRoleViaName} whenever the written role is available.
     */
    public String connectingRole(ObjectType houder, ObjectType target) {
        if (houder == null || target == null) return null;
        String found = null;
        int n = 0;
        for (FactType ft : factTypes.values())
            for (Role targetName : ft.roles)
                if (roleHasObjectType(targetName, target) && factTypeRelates(ft, targetName, houder)) {
                    found = targetName.name();
                    n++;
                }
        return n == 1 ? found : null;
    }

    /** How many roles of {@code target} are reachable from {@code houder} — the count {@link #connectingRole}
     *  collapses into null at zero and at several; a refusal names it. */
    public int connectingRoleCount(ObjectType houder, ObjectType target) {
        if (houder == null || target == null) return 0;
        int n = 0;
        for (FactType ft : factTypes.values())
            for (Role targetName : ft.roles)
                if (roleHasObjectType(targetName, target) && factTypeRelates(ft, targetName, houder)) n++;
        return n;
    }

    /**
     * Every role of {@code target} that is reachable from {@code houder} AND uniquely addressable BY ITS
     * OWN DECLARED NAME — the set {@link #connectingRole} collapses to null as soon as the type pair is
     * connected by more than one feittype. One role per uniquely-resolving name, in wire order.
     *
     * <p>It is NOT a relaxation of {@link #connectingRole}: a member is admitted only when asking
     * {@link #connectingRoleViaName} for that role's own name answers that very role, so the set is exactly
     * the roles a WRITTEN name can still select. A name two roles share resolves to nothing and neither of
     * them joins the set — the ambiguity is refused per NAME instead of per PAIR. On a uniquely connected
     * pair the two authorities coincide: the single candidate is the single name, so the set is exactly
     * {@code [connectingRole(houder, doelwit)]}.
     *
     * <p>Cardinality is deliberately NOT filtered here: this answers "which roles are addressable", and
     * whether a role's storage lives on this side is the caller's own {@link #toManyRole} question (the
     * same split {@link #connectingRole}'s callers already make).
     */
    public List<String> connectingRoles(ObjectType houder, ObjectType target) {
        List<String> result = new ArrayList<>();
        if (houder == null || target == null) return result;
        for (FactType ft : factTypes.values())
            for (Role targetName : ft.roles)
                if (roleHasObjectType(targetName, target) && factTypeRelates(ft, targetName, houder)) {
                    String r = connectingRoleViaName(houder, target, targetName.name());
                    if (r != null && r.equalsIgnoreCase(targetName.name())
                            && result.stream().noneMatch(x -> x.equalsIgnoreCase(r))) result.add(r);
                }
        return result;
    }

    /**
     * Like {@link #connectingRole} but disambiguated by the role name the author
     * wrote: only a feittype role whose NAME matches {@code writtenRole} (singular,
     * declared plural, or a best-effort plural) and whose object type is
     * {@code target} counts. This resolves the case the by-pair search cannot — two
     * feittypen over the same object-type pair, or one object type playing two roles —
     * because the source names exactly one. Matching by name first, object type
     * second, is the §13.4.1 reading: the written role IS the navigation. Returns null
     * when no role, or more than one, matches (still ambiguous).
     *
     * A DECLARED role name (singular or declared plural) is tried before the best-effort
     * synthesized plural, so a reference that exactly names one role is not defeated by
     * another role whose +s/+en plural happens to collide (§3.1 footnote 2: a synthesized
     * plural is outside the language and must never override a declared name).
     */
    public String connectingRoleViaName(ObjectType houder, ObjectType target, String writtenRole) {
        if (target == null || writtenRole == null) return null;
        String exact = findConnectingRole(houder, target, writtenRole, true);
        return exact != null ? exact : findConnectingRole(houder, target, writtenRole, false);
    }

    /** The unique role of {@code target} that {@code writtenRole} names in a feittype
     *  relating {@code houder}, or null when none or more than one matches. With
     *  {@code exactOnly} only declared role names count; otherwise the best-effort plural
     *  is also allowed (used only when no declared name matched). */
    private String findConnectingRole(ObjectType houder, ObjectType target, String writtenRole, boolean exactOnly) {
        String found = null;
        int n = 0;
        for (FactType ft : factTypes.values())
            for (Role targetName : ft.roles) {
                boolean nameMatches = exactOnly ? roleMatchesExact(targetName, writtenRole) : roleMatches(targetName, writtenRole);
                if (nameMatches && roleHasObjectType(targetName, target) && factTypeRelates(ft, targetName, houder)) {
                    found = targetName.name();
                    n++;
                }
            }
        return n == 1 ? found : null;
    }

    /** Does {@code ft} relate {@code houder} through its other end of {@code targetRole}?
     *  For a normal feittype that means a DISTINCT role filled by {@code houder}; for a
     *  wederkerig feittype (§13.3.9.2) the single role relates the object type to
     *  itself, so it is enough that that role's own type is {@code houder}. A null
     *  {@code houder} imposes no constraint. */
    private boolean factTypeRelates(FactType ft, Role targetRole, ObjectType houder) {
        if (houder == null) return true;
        if (ft.reciprocal) return roleHasObjectType(targetRole, houder);
        return ft.roles.stream().anyMatch(h -> h != targetRole && roleHasObjectType(h, houder));
    }

    /** The object type a role named {@code writtenRole} reaches FROM {@code subject}:
     *  the unique type of a role that {@code writtenRole} names in a feittype relating
     *  {@code subject}. This is the §13.4.1.2/.3 rolnaam reading of a navigation filler —
     *  used to override a same-named objecttype that does not itself join the subject.
     *  Returns null when no role, or more than one (ambiguous), matches. */
    public ObjectType roleTargetTypeToSubject(ObjectType subject, String writtenRole) {
        if (writtenRole == null) return null;
        ObjectType found = null;
        int n = 0;
        for (FactType ft : factTypes.values())
            for (Role r : ft.roles)
                if (roleMatches(r, writtenRole) && factTypeRelates(ft, r, subject)) {
                    ObjectType t = objectTypeByName(r.objectTypeName());
                    if (t != null) { found = t; n++; }
                }
        return n == 1 ? found : null;
    }

    /**
     * The scopes a parent-scoped uniqueness "de X van alle &lt;element&gt; van de &lt;ouder&gt;" can take (§8.1.6;
     * IR-SPEC ch. 3.6 {@code scope}), each the binding of its parent role, found as a navigated read binds its written
     * role (grammar/DIVERGENCES.md, §13.4.1): every relation in which a role {@code ouder} names relates
     * {@code element}, counted as {@link #roleTargetTypeToSubject} counts them, whatever else joins the two types;
     * where those are several and {@code elementName} names the element's role, by its declared name or plural, in
     * exactly one of them, that one. A reciprocal relation counts, so that the caller refuses it for what it is.
     */
    public List<RoleBindingRef> scopeBindings(ObjectType element, String elementName, String parent) {
        List<RoleBindingRef> all = new ArrayList<>(), named = new ArrayList<>();
        if (element == null || parent == null) return all;
        for (FactType ft : factTypes.values())
            for (Role r : ft.roles)
                if (roleMatches(r, parent) && factTypeRelates(ft, r, element)
                        && objectTypeByName(r.objectTypeName()) != null) {
                    RoleBindingRef rb = new RoleBindingRef(ft.name, r.name());
                    all.add(rb);
                    if (elementName != null && ft.roles.stream().anyMatch(e -> (e != r || ft.reciprocal)
                            && roleHasObjectType(e, element) && roleMatchesExact(e, elementName))) named.add(rb);
                }
        return all.size() > 1 && named.size() == 1 ? named : all;
    }

    /** §8.1.7 rolcheck "{@code subject} is een &lt;writtenRole&gt;": the object type at the OTHER end
     *  of the feittype whose role {@code writtenRole} the {@code subject} FILLS — so the subject
     *  participates in that role iff its reference to that other end is set. The mirror of
     *  {@link #roleTargetTypeToSubject} (which navigates FROM the subject TO a role's filler; here the
     *  subject IS the filler). Returns null when no role named {@code writtenRole} is filled by the
     *  subject, when it is ambiguous (more than one feittype/sibling matches), or when the feittype is
     *  wederkerig (no distinct sibling to join — carded). */
    public ObjectType roleSiblingType(ObjectType subject, String writtenRole) {
        if (subject == null || writtenRole == null) return null;
        ObjectType found = null;
        int n = 0;
        for (FactType ft : factTypes.values())
            if (!ft.reciprocal)
                for (Role r : ft.roles)
                    if (roleMatches(r, writtenRole) && roleHasObjectType(r, subject))   // subject fills role r
                        for (Role other : ft.roles)
                            if (other != r) {
                                ObjectType t = objectTypeByName(other.objectTypeName());
                                if (t != null) { found = t; n++; }
                            }
        return n == 1 ? found : null;
    }

    /**
     * The subject-side join field for an aggregation whose ELEMENT role is named
     * {@code writtenRole} — the field on the element fact that points back at the
     * subject (the role {@code subject} plays in the feittype where {@code writtenRole}
     * names the element role). This disambiguates by the WRITTEN element name what the
     * by-pair {@link #connectingRole} cannot: when the (element, subject) pair is joined
     * by more than one feittype, the count names exactly one element role, so the join is
     * unique. Returns null when no role, or more than one, matches — the caller then keeps
     * the by-pair fallback (single-feittype case) or emits a marker.
     */
    public String connectingRoleViaElementName(ObjectType subject, String writtenRole) {
        if (subject == null || writtenRole == null) return null;
        String found = null;
        int n = 0;
        for (FactType ft : factTypes.values())
            for (Role elemRole : ft.roles)
                if (roleMatches(elemRole, writtenRole) && factTypeRelates(ft, elemRole, subject))
                    for (Role subjRole : ft.roles)
                        if (subjRole != elemRole && roleHasObjectType(subjRole, subject)) { found = subjRole.name(); n++; }
        return n == 1 ? found : null;
    }

    /** A symbolic role binding: the fact type plus the DECLARED (singular) role name —
     *  the pair the IR emitter's {@code RoleBinding} extension fields carry (a plural
     *  never binds; the wire names the declared role). */
    public record RoleBindingRef(String factType, String role) {}

    /**
     * Like {@link #connectingRole}, but returning the fact type together with the bound
     * role — the pair the IR emitter's symbolic role-binding extension needs. The search
     * and its uniqueness contract are IDENTICAL to {@link #connectingRole} (same counting,
     * so "unique" means the same thing on both paths); on top of that, a winner from a
     * wederkerig feittype is withheld — the two-role binding form does not cover the
     * single-role reciprocal shape, so it stays bindingless there rather than mis-bound.
     */
    public RoleBindingRef connectingRoleBinding(ObjectType houder, ObjectType target) {
        if (houder == null || target == null) return null;
        RoleBindingRef found = null;
        boolean reciprocal = false;
        int n = 0;
        for (FactType ft : factTypes.values())
            for (Role targetName : ft.roles)
                if (roleHasObjectType(targetName, target) && factTypeRelates(ft, targetName, houder)) {
                    found = new RoleBindingRef(ft.name, targetName.name());
                    reciprocal = ft.reciprocal;
                    n++;
                }
        return n == 1 && !reciprocal ? found : null;
    }

    /**
     * Whether {@code written} NAMES the role {@code rb} binds — its declared singular, its declared
     * plural, or (only when no plural is declared) the best-effort plural. The written-name side of a
     * binding the by-PAIR search produced: {@link #connectingRoleBinding} never reads the source, so a
     * pair that is unique still says nothing about what the author wrote, and a binding whose role the
     * sentence does not name is a guess about WHICH collection is folded, not a resolution of it.
     * False for an unknown fact type or a role the fact type does not carry.
     */
    public boolean roleNamedAs(RoleBindingRef rb, String written) {
        if (rb == null || written == null) return false;
        FactType ft = factTypes.get(rb.factType());
        if (ft == null) return false;
        for (Role r : ft.roles)
            if (r.name().equals(rb.role())) return roleMatches(r, written);
        return false;
    }

    /**
     * Like {@link #connectingRoleViaName} but returning the fact type together with the reached
     * (far) role — the pair the IR emitter's {@code navigationBindings} extension carries for a
     * by-name hop. Only a DECLARED role name (singular or declared plural) binds: the best-effort
     * plural is deliberately NOT mirrored (a binding names what the author declared, the same
     * narrowing {@link #elementRoleBinding} makes and the uchar verifier's by-name derivation runs),
     * so a guessed plural stays bindingless rather than bound to a spelling the model never defined.
     * Null when no role, or more than one, matches.
     */
    public RoleBindingRef connectingRoleViaNameBinding(ObjectType houder, ObjectType target, String writtenRole) {
        if (target == null || writtenRole == null) return null;
        RoleBindingRef found = null;
        int n = 0;
        for (FactType ft : factTypes.values())
            for (Role targetName : ft.roles)
                if (roleMatchesExact(targetName, writtenRole) && roleHasObjectType(targetName, target)
                        && factTypeRelates(ft, targetName, houder)) {
                    found = new RoleBindingRef(ft.name, targetName.name());
                    n++;
                }
        return n == 1 ? found : null;
    }

    /**
     * The ELEMENT-role binding of an instance fold, disambiguated by the written element
     * name — the {@link #connectingRoleViaElementName} search capturing the fact type and
     * the ELEMENT role instead of the subject's back-reference, with one deliberate
     * narrowing: only a DECLARED name (singular or declared plural) matches, never the
     * best-effort plural — a binding must name what the author declared, so a guessed
     * plural stays bindingless rather than bound to a spelling the model never defined.
     * Null when no role, or more than one, matches.
     */
    public RoleBindingRef elementRoleBinding(ObjectType subject, String writtenRole) {
        ElementBindingRef eb = elementBinding(subject, writtenRole);
        return eb == null ? null : new RoleBindingRef(eb.factType(), eb.elementRole());
    }

    /**
     * The ONE-STEP RANGE of a set reading over {@code elementType} written from {@code subject} — the
     * binding the uchar wire carries as {@code sourceRole} on an instance fold (IR-SPEC ch. 4.3) and on a
     * §8.2 quantifier fold alike. One search, two callers: the emitter's fold arm and the resolver's
     * §8.2 lowering must never classify one written range differently, so neither owns a copy of it.
     *
     * <p>By-PAIR first, then the WRITTEN name. IR-SPEC ch. 2.2: the pair's uniqueness (WF-048's, until L2) is
     * computed over the CHAIN-CLOSED type pair, so a new subtype can make a pair that was uniquely connected doubly
     * connected — and a range that went out bound yesterday would be refused today. The written name is
     * authoritative (§13.4.1) and still names exactly one role, so the binding stays derivable and the
     * document keeps working.
     *
     * <p>The reached role must declare EXACTLY {@code elementType} as its filler — identity, never the
     * ⊑ of ch. 2.6. That is the difference between this search and every other one in this file, and it is
     * load-bearing: {@link #nameCandidates} forms candidates by dropping leading words, so a truncated
     * phrase can land on a same-named role in ANOTHER fact type, and an upper-bound test would wave it
     * through because the element type is a subtype of that role's filler. The range would then go out
     * bound to a relation the sentence never named — verifier-clean and silently folding the wrong set,
     * which is strictly worse than the loud refusal a bindingless fold gets (the emitter refuses it; the
     * wire requires the range since L2). A phrase that names no
     * role, more than one, or one whose filler is not the element type itself stays bindingless (null),
     * and a binding is never emitted as null.
     */
    public RoleBindingRef foldSourceBinding(ObjectType subject, ObjectType elementType, String subjectSource) {
        if (subject == null || elementType == null) return null;
        RoleBindingRef pair = connectingRoleBinding(subject, elementType);
        if (pair != null) return pair;
        if (subjectSource == null) return null;
        for (String kandidaat : nameCandidates(subjectSource)) {
            RoleBindingRef b = elementRoleBinding(subject, kandidaat);
            if (b != null) return fillsExactly(b, elementType) ? b : null;
        }
        return null;
    }

    /** Whether the role {@code rb} binds declares {@code elementType} ITSELF as its filler. */
    private boolean fillsExactly(RoleBindingRef rb, ObjectType elementType) {
        FactType ft = factTypes.get(rb.factType());
        if (ft == null) return false;
        for (Role r : ft.roles)
            if (r.name().equals(rb.role())) return objectTypeByName(r.objectTypeName()) == elementType;
        return false;
    }

    /** A symbolic navigation binding capturing BOTH ends of one element-name hop: the fact type,
     *  the role the {@code subject} fills (the near/back-reference side), and the ELEMENT role
     *  {@code writtenRole} names (the far side) — the {@link #connectingRoleViaElementName} search
     *  returning the whole pair, so a §13.4.7 feitcreatie can materialise its symbolic hops
     *  (created relation, join, tussenstap) at resolution time. Only a DECLARED role name binds
     *  (never the best-effort plural). Null when no role, or more than one, matches. */
    public record ElementBindingRef(String factType, String subjectRole, String elementRole) {}

    public ElementBindingRef elementBinding(ObjectType subject, String writtenRole) {
        if (subject == null || writtenRole == null) return null;
        ElementBindingRef found = null;
        int n = 0;
        for (FactType ft : factTypes.values())
            for (Role elemRole : ft.roles)
                if (roleMatchesExact(elemRole, writtenRole) && factTypeRelates(ft, elemRole, subject))
                    for (Role subjRole : ft.roles)
                        if (subjRole != elemRole && roleHasObjectType(subjRole, subject)) {
                            found = new ElementBindingRef(ft.name, subjRole.name(), elemRole.name());
                            n++;
                        }
        return n == 1 ? found : null;
    }

    /** One resolved STEP of a multi-step fold-source range (IR-SPEC ch. 4.3 / WF-102): the symbolic
     *  wire binding — the fact type plus the role whose fillers form the reached set — together with the
     *  object type that role reaches. The to-MANY-range sibling of {@link ChainHop}, which is a to-one
     *  getter hop; a step here is neutral about multiplicity, because the flat-union reading is
     *  (RUNTIME-SEMANTICS ch. 3.4b: a to-one step contributes its zero-or-one instance like any other). */
    public record RangeHop(RoleBindingRef binding, ObjectType targetName) {}

    /** Resolve ONE written step of a fold-source range from {@code houder}: the unique fact-type role
     *  that {@code writtenElement} names, in a fact type that also relates {@code houder}. The written
     *  name is authoritative (§13.4.1), and only a DECLARED name binds — singular or declared plural,
     *  never the best-effort plural — which is exactly {@link #elementBinding}'s contract and exactly
     *  WF-047's ("the plural does not bind"). Null on no match, on an ambiguous one, or on a reciprocal
     *  fact type (whose single role cannot produce the two-role step shape); the caller then declines the
     *  WHOLE path rather than binding part of it. */
    public RangeHop rangeHop(ObjectType houder, String writtenElement) {
        ElementBindingRef eb = elementBinding(houder, writtenElement);
        if (eb == null) return null;
        FactType ft = factTypes.get(eb.factType());
        if (ft == null) return null;
        for (Role r : ft.roles)
            if (r.name().equals(eb.elementRole())) {
                ObjectType targetName = objectTypeByName(r.objectTypeName());
                return targetName == null ? null : new RangeHop(new RoleBindingRef(eb.factType(), r.name()), targetName);
            }
        return null;
    }

    /** A role a written step of a fold-source range names from a holder, with the fact type it belongs to. */
    public record StepCandidate(FactType factType, Role role) {}

    /** The roles a written step of a fold-source range names from {@code houder}, split by why {@link #rangeHop}
     *  takes them or not. {@code declared}: by a DECLARED name (singular or declared plural) in a two-role fact
     *  type, one entry for each binding {@link #elementBinding} counts, so the step binds exactly when there is one
     *  and its filler is a declared object type. {@code derived}: by the best-effort plural alone, which binds no
     *  step. {@code reciprocal}: in a reciprocal fact type, whose single role gives no two-role step. It exists for
     *  the refusal that names the step and its cause, so the words follow the search that declined the step. */
    public record StepCandidates(List<StepCandidate> declared, List<StepCandidate> derived,
                                 List<StepCandidate> reciprocal) {}

    public StepCandidates stepCandidates(ObjectType houder, String writtenElement) {
        List<StepCandidate> declared = new ArrayList<>(), derived = new ArrayList<>(),
                reciprocal = new ArrayList<>();
        if (houder != null && writtenElement != null)
            for (FactType ft : factTypes.values())
                for (Role r : ft.roles) {
                    if (!roleMatches(r, writtenElement) || !factTypeRelates(ft, r, houder)) continue;
                    StepCandidate k = new StepCandidate(ft, r);
                    if (ft.reciprocal) reciprocal.add(k);
                    else if (!roleMatchesExact(r, writtenElement)) derived.add(k);
                    else for (Role h : ft.roles)
                        if (h != r && roleHasObjectType(h, houder)) declared.add(k);
                }
        return new StepCandidates(declared, derived, reciprocal);
    }

    /** How many roles named {@code writtenRole} are reachable FROM {@code subject} as a far role — the
     *  count behind {@link #roleTargetTypeToSubject}, which collapses zero and several into null. The
     *  ch. 4.1a resolution ORDER needs the count and not the winner: far-role resolution comes FIRST, and
     *  a self-denoting reading applies only at ZERO far hits, so an AMBIGUOUS far match must not fall
     *  through to it. */
    public int roleHitsToSubject(ObjectType subject, String writtenRole) {
        if (subject == null || writtenRole == null) return 0;
        int n = 0;
        for (FactType ft : factTypes.values())
            for (Role r : ft.roles)
                if (roleMatches(r, writtenRole) && factTypeRelates(ft, r, subject)
                        && objectTypeByName(r.objectTypeName()) != null) n++;
        return n;
    }

    /** Whether {@code subject} ITSELF fills a role named {@code writtenRole} (ci, declared name or
     *  declared plural, or the best-effort plural) — the WF-083 (b) self-denotation test, used at the
     *  ANCHOR of a fold-source range: "alle vondsten van de aanmelder", read on the Jutter who IS the
     *  aanmelder. Only meaningful AFTER {@link #roleHitsToSubject} returns zero (ch. 4.1a: a far role
     *  is a real hop, whatever its filler type happens to be). */
    public boolean fillsSelfRole(ObjectType subject, String writtenRole) {
        if (subject == null || writtenRole == null) return false;
        for (FactType ft : factTypes.values())
            for (Role r : ft.roles)
                if (roleMatches(r, writtenRole) && roleHasObjectType(r, subject)) return true;
        return false;
    }

    /**
     * The written role name to record on a navigation reference: the {@code noun}
     * itself when it denotes a feittype role (and not an object type by that name),
     * else null. Kept as written so {@link #connectingRoleViaName}'s {@code roleMatches}
     * can still reconcile a plural ("deelnemers") against the declared singular.
     */
    public String navigationRoleName(String noun) {
        if (noun == null || noun.isBlank()) return null;
        if (objectTypeByName(noun) != null) return null; // names an object type directly — not navigation
        for (FactType ft : factTypes.values())
            for (Role r : ft.roles)
                if (roleMatches(r, noun)) return noun;
        return null;
    }

    /** Can {@code ot} fill role {@code r}? IR-SPEC ch. 2.6: the role's DECLARED filler type is an
     *  UPPER bound, so every subtype of it fills the role — which is what lets a feittype declared
     *  on a supertype carry subtype instances. Identity on a flat model, where every type is its
     *  own only ancestor. The name comparison stays as the fall-back for a role naming a type the
     *  model does not declare. */
    private boolean roleHasObjectType(Role r, ObjectType ot) {
        ObjectType filler = objectTypeByName(r.objectTypeName());
        if (filler != null && ot != null) return ot.isA(filler);
        return ot != null && r.objectTypeName().equalsIgnoreCase(ot.name);
    }

    private static boolean roleMatches(Role r, String name) {
        return roleMatchesExact(r, name) || roleMatchesBestEffort(r, name);
    }

    /** A role name the model DECLARES: the singular role name or its declared (mv:)
     *  plural. These are the forms the language actually defines. */
    private static boolean roleMatchesExact(Role r, String name) {
        return r.name().equalsIgnoreCase(name)
                || (r.plural() != null && r.plural().equalsIgnoreCase(name));
    }

    /** A best-effort plural match used ONLY when no plural is declared: RegelSpraak
     *  defines no plural morphology — §3.1 footnote 2 states derivation is ALEF-specific,
     *  OUTSIDE the language — so this must never defeat a declared form, and callers that
     *  disambiguate by name (see {@link #connectingRoleViaName}) try the exact forms
     *  first. (See grammar/DIVERGENCES.md.) */
    private static boolean roleMatchesBestEffort(Role r, String name) {
        if (r.plural() != null) return false;
        if (name.equalsIgnoreCase(r.name() + "s") || name.equalsIgnoreCase(r.name() + "en")) return true;
        String singularHead = name.replaceFirst("^(\\S+?)(?:s|en)(\\s)", "$1$2");
        return singularHead.equalsIgnoreCase(r.name());
    }

    /**
     * Resolve a unit spelling to the Eenheid declared in some system. A unit's
     * abbreviation ("jr"), word ("jaar") and plural ("jaren") all denote the same
     * unit, so the spelling is matched as an abbreviation and, normalised to its
     * singular time-unit word, as an alias of a declared unit. Resolving "jaren" and
     * "jr" to the same system makes them inter-convertible (same-system rule), which
     * is what lets a duration "in hele jaren" assign to an attribute "met eenheid jr".
     */
    public Unit resolveUnit(String spelling) {
        String norm = normaliseTimeUnit(spelling);
        for (UnitSystem s : unitSystems.values()) {
            Unit e = s.units.get(spelling);
            if (e == null) e = s.spellings.get(norm);
            if (e != null) return e;
        }
        return new Unit(norm, null); // unknown system; normalised so plural/singular still unify
    }

    /** Singular form of a built-in time unit (the lexer's nine units), else the spelling trimmed and
     *  lower-cased. That fall-through used to hand back the ORIGINAL string, which made the whole
     *  time-unit table case-sensitive for everything except the nine {@code -en} plurals — those are the
     *  only spellings the switch below REPLACES, and the replacement happened to be lowercase. Measured
     *  2026-09-20: {@code chronoUnit} answered WEEKS for {@code weken} and {@code WEKEN} but null for
     *  {@code WK}, {@code Wk}, {@code ' wk '} and {@code Sec}, so a verifier-clean document with the unit
     *  spelled {@code WK} split the executors 3-against-2 — the verifier, {@code ref} and the ASP backend
     *  reckoned in weeks (they trim and lower-case before the lookup) while both Drools branches refused
     *  the rule as an unknown time unit. RUNTIME-SEMANTICS ch. 3.8 calls the recognition
     *  case-insensitive, so the defect was here and not in the readers. */
    public static String normaliseTimeUnit(String s) {
        String sel = s == null ? "" : s.trim().toLowerCase();
        return switch (sel) {
            case "jaren" -> "jaar";           case "maanden" -> "maand";
            case "dagen" -> "dag";            case "weken" -> "week";
            case "kwartalen" -> "kwartaal";   case "uren" -> "uur";
            case "minuten" -> "minuut";       case "seconden" -> "seconde";
            case "milliseconden" -> "milliseconde";
            default -> sel;
        };
    }

    /** An object type: its attributes (name -> datatype) and kenmerk names. */
    public static final class ObjectType {
        public final String name;
        public boolean animate;
        /** The declared "(mv: …)" plural (§13.2.29), or null. Used to resolve a plural-form reference
         *  ("alle Natuurlijke personen" in a §8.1.6 uniciteitscontrole) back to this type by its exact
         *  declared spelling, without guessing irregular noun/adjective plural morphology. */
        public String plural;
        public final Map<String, Datatype> attributes = new LinkedHashMap<>();
        /** An attribute's declared "(mv: …)" plural (§3.3 naamwoord, footnote 2), keyed by its singular
         *  naam — the spec-sanctioned source for the plural a §8.1.6 uniciteitscontrole writes, so it is
         *  matched exactly (incl. irregular plurals) ahead of any synthesised form. */
        public final Map<String, String> attributePlural = new LinkedHashMap<>();
        /** §13.3.7 the dimensions an attribute is "gedimensioneerd met", in declared order, keyed by its
         *  naam — empty/absent for a scalar attribute. A gedimensioneerd attribuut holds one value per
         *  combination of one label from each of its dimensions (§3.6: "vier mogelijke waardes"), so a
         *  reference must pin every dimension and an aggregation ranges over a dimension's label set. */
        public final Map<String, List<String>> attributeDimensions = new LinkedHashMap<>();
        /** §13.3.6 the granularity of a TEMPORAL attribute's tijdlijn ("de premie Bedrag voor elk jaar" -&gt;
         *  JAAR), keyed by its naam — absent for an ordinary single-value attribute. A temporal attribute
         *  holds one value per period of this granularity (§5.1); recording the declaration here is the
         *  IR-shape foundation for the Phase-7 value model that will lower the periods (knip-merge,
         *  tijdsevenredig, gedurende). The VARIABLE startpunt has since been captured — a named
         *  tijdlijn records it in {@link Timeline#variableStartPoint} and §9.9's startpuntbepaling
         *  resolves its anchor ({@link #startPointAnchorMember}). A FIXED startpunt ("voor elke 5 maanden
         *  startend op …") is {@link #attributeFixedStartPoint}. */
        public final Map<String, Granularity> attributeGranularity = new LinkedHashMap<>();
        /** §13.3.6.7 the period COUNT of a MULTI-period tijdlijn ("voor elke 5 maanden" -&gt; 5), keyed by the
         *  attribute naam — absent (count 1) for a single-period "voor elke maand" as the resolver records it; a
         *  model the Drools IR loader builds carries every facet's count, 1 included. Each tijdvak then spans
         *  {@code count} units of {@link #attributeGranularity}, so a temporal fold must divide by it. */
        public final Map<String, Integer> attributeTimelineCount = new LinkedHashMap<>();
        /** §13.3.6.9 the NAMED tijdlijn an attribute rides ("voor elke &lt;tijdlijnnaam&gt;"), keyed by the
         *  attribute naam — absent for the inline forms ("voor elke maand"). Recorded so the §9.9 machinery
         *  can see whether that tijdlijn has a variabel startpunt and find its anchor. */
        public final Map<String, String> attributeTimelineName = new LinkedHashMap<>();
        /** §13.3.6.3–.7 the FIXED start point of an attribute's tijdlijn in its wire spelling (IR-SPEC ch. 2.8,
         *  {@code fixedStart}), keyed by the attribute naam — absent on the calendar grid, which is also where
         *  the calendar's own start point ("startend op dag 1 van maand 1") leaves it. The named form adopts the
         *  definition's, as it adopts the granularity and the count. */
        public final Map<String, String> attributeFixedStartPoint = new LinkedHashMap<>();
        public final List<String> characteristics = new ArrayList<>();
        /** §13.3.6 the granularity of a temporal kenmerk's tijdlijn ("is duurzaam kenmerk voor elke dag"),
         *  keyed by its naam — the kenmerk analogue of {@link #attributeGranularity}. */
        public final Map<String, Granularity> characteristicGranularity = new LinkedHashMap<>();
        /** §13.3.6.7 the kenmerk analogue of {@link #attributeTimelineCount}. */
        public final Map<String, Integer> characteristicTimelineCount = new LinkedHashMap<>();
        /** §13.3.6.3–.7 the kenmerk analogue of {@link #attributeFixedStartPoint}. */
        public final Map<String, String> characteristicFixedStartPoint = new LinkedHashMap<>();
        /** §3.5 the soort of each kenmerk, keyed by its naam — drives the §8.1.8 predicate form: a
         *  BIJVOEGLIJK kenmerk is written "is &lt;k&gt;", a BEZITTELIJK one "heeft het &lt;k&gt;", a
         *  ZELFSTANDIG one "is een &lt;k&gt;" (so only a ZELFSTANDIG kenmerk binds the EEN-form). */
        public final Map<String, CharacteristicKind> characteristicKinds = new LinkedHashMap<>();
        /** IR-SPEC ch. 2.2: the single direct supertype, or null for a root type. Resolved after
         *  every declaration is read, because the wire reference is order-free. The member maps
         *  above stay OWN — the chain is walked by the accessors below, and a bean generator that
         *  emits {@code extends} needs the own set, not the closed one. */
        public ObjectType supertype;
        public ObjectType(String name) { this.name = name; }

        /** This type and every ancestor, nearest first. Cycle-safe by construction: WF-104 refuses
         *  a cycle at the document, and every reader here must still terminate on one. */
        public List<ObjectType> chain() {
            List<ObjectType> result = new ArrayList<>();
            for (ObjectType t = this; t != null && !result.contains(t); t = t.supertype) result.add(t);
            return result;
        }

        /** §3.5 kenmerk soort: bijvoeglijk ("is &lt;k&gt;"), bezittelijk ("heeft het &lt;k&gt;") or
         *  zelfstandig/noch-noch ("is een &lt;k&gt;"). An untagged declaration is zelfstandig. */
        public enum CharacteristicKind { BIJVOEGLIJK, BEZITTELIJK, ZELFSTANDIG }

        /** What {@code map} holds for member {@code naam} on the NEAREST type of this chain that has
         *  an entry for it, matched case-INsensitively; null when nothing on the chain does. Every
         *  member-keyed side map is read through this one walk, because reading any of them on the own
         *  type alone answers "no such member" for an inherited one — and for the TEMPORAL maps that
         *  answer is not a missing binding but a WRONG judgement (a timeline read as a scalar). */
        private <V> V onChain(java.util.function.Function<ObjectType, Map<String, V>> map, String name) {
            if (name == null) return null;
            for (ObjectType t : chain()) {      // upward only (ch. 2.2): the nearest declaration answers
                Map<String, V> m = map.apply(t);
                V exact = m.get(name);
                if (exact != null) return exact;
                for (var e : m.entrySet())
                    if (e.getKey().equalsIgnoreCase(name)) return e.getValue();
            }
            return null;
        }

        /** The declared datatype of attribute {@code naam}, matched case-INsensitively —
         *  object-type names already resolve this way (see {@link #objectTypeByName}), so a
         *  reference that differs only in case (casus source writes both "datum-tijd" and
         *  "Datum-tijd") binds the same attribute. Null when this type declares no such
         *  attribute. */
        public Datatype attribute(String name) {
            return onChain(t -> t.attributes, name);
        }

        /** The DECLARED spelling of attribute {@code naam} (which may differ from the written one only
         *  in case), or null when the chain declares no such attribute. The IR carries declared names
         *  only, so a writer that reconstructs a reference reads its spelling here. */
        public String attributeName(String name) {
            for (ObjectType t : chain())
                for (String a : t.attributes.keySet())
                    if (a.equalsIgnoreCase(name)) return a;
            return null;
        }

        /** The DECLARED spelling of kenmerk {@code naam}, or null when the chain declares no such
         *  kenmerk — the kenmerk twin of {@link #attributeName}. */
        public String characteristicName(String name) {
            for (ObjectType t : chain())
                for (String k : t.characteristics)
                    if (k.equalsIgnoreCase(name)) return k;
            return null;
        }

        /** The §13.3.7 dimensions of attribute {@code naam} (case-insensitively), in declared order,
         *  or an empty list when the attribute is scalar or undeclared. */
        public List<String> dimensionsOf(String name) {
            List<String> dims = onChain(t -> t.attributeDimensions, name);
            return dims == null ? List.of() : dims;
        }

        /** The chain-closed VIEW of member-keyed side map {@code map}: every entry this type or an
         *  ancestor declares, nearest declaration first (a nearer entry wins — WF-105 refuses shadowing,
         *  so the collision is unreachable and the rule only fixes the reading). The map-shaped twin of
         *  {@link #onChain}, for the two readers a one-name question does not serve: one that must
         *  SEARCH the map, and one that hands the WHOLE map to a name contract OF ITS OWN — the Drools
         *  back-end matches member names by ref's {@code norm} (lowercase-ASCII + collapsed whitespace),
         *  which is neither wider nor narrower than the {@code equalsIgnoreCase} above, so routing those
         *  sites through {@link #onChain} would close the chain and open a name-matching divergence in
         *  the same edit. Closing the chain is this method's whole job. */
        public <V> Map<String, V> onChainMap(java.util.function.Function<ObjectType, Map<String, V>> map) {
            Map<String, V> result = new LinkedHashMap<>();
            for (ObjectType t : chain()) map.apply(t).forEach(result::putIfAbsent);
            return result;
        }

        /** Every declared attribute reachable from this type — its own and its ancestors' — keyed by
         *  declared naam, nearest declaration first. The chain-closed member set of ch. 2.2. */
        public Map<String, Datatype> attributeMembers() {
            return onChainMap(t -> t.attributes);
        }

        /** Every gedimensioneerd attribute reachable from this type — its own and its ancestors' —
         *  keyed by declared naam, nearest declaration first. A reader that must SEARCH the
         *  dimensioned attributes (rather than ask about one name) iterates this instead of the own
         *  map, so an inherited cell read resolves like a declared one. */
        public Map<String, List<String>> dimensionedMembers() {
            return onChainMap(t -> t.attributeDimensions);
        }

        /** §13.3.6 the tijdlijn granularity of attribute {@code naam}, or null when it is an ordinary
         *  single-value attribute. */
        public Granularity granularityOf(String name) {
            return onChain(t -> t.attributeGranularity, name);
        }

        /** §13.3.6 every TEMPORAL attribute reachable from this type, keyed by declared naam —
         *  {@link #granularityOf}'s map-shaped twin, for a reader that asks the whole facet map at
         *  once. Reading the own map instead answers "not temporal" for an INHERITED timeline, which is
         *  not a missing binding but a wrong judgement (a timeline lowered as a scalar). */
        public Map<String, Granularity> granularityMembers() {
            return onChainMap(t -> t.attributeGranularity);
        }

        /** §13.3.6.7 the period COUNT of attribute {@code naam}'s multi-period tijdlijn, or null where the model
         *  records none — the resolver records none for a single-period (count 1) one; see
         *  {@link #attributeTimelineCount}. */
        public Integer timelineCountOf(String name) {
            return onChain(t -> t.attributeTimelineCount, name);
        }

        /** §13.3.6.7 the chain-closed period-count map — {@link #timelineCountOf}'s map-shaped twin,
         *  for the multiplicity predicate both Drools branches share, which takes the whole map. */
        public Map<String, Integer> timelineCountMembers() {
            return onChainMap(t -> t.attributeTimelineCount);
        }

        /** §13.3.6.3–.7 the fixed start point of attribute {@code naam}'s tijdlijn in its wire spelling, or null
         *  on the calendar grid — chain-closed like the granularity, since an inherited attribute brings its facet. */
        public String fixedStartPointOf(String name) {
            return onChain(t -> t.attributeFixedStartPoint, name);
        }

        // NOTE: attributeTimelineName has NO chain accessor on purpose — see hasVariableStart.

        /** §13.3.6 the tijdlijn granularity of kenmerk {@code naam} — the kenmerk twin of
         *  {@link #granularityOf}; null for an ordinary boolean kenmerk. */
        public Granularity characteristicGranularityOf(String name) {
            return onChain(t -> t.characteristicGranularity, name);
        }

        /** §13.3.6 the kenmerk twin of {@link #granularityMembers}. */
        public Map<String, Granularity> characteristicGranularityMembers() {
            return onChainMap(t -> t.characteristicGranularity);
        }

        /** §13.3.6.7 the kenmerk twin of {@link #timelineCountOf}. */
        public Integer characteristicTimelineCountOf(String name) {
            return onChain(t -> t.characteristicTimelineCount, name);
        }

        /** §13.3.6.7 the kenmerk twin of {@link #timelineCountMembers}. */
        public Map<String, Integer> characteristicTimelineCountMembers() {
            return onChainMap(t -> t.characteristicTimelineCount);
        }

        /** Does this type declare a member named {@code naam} (an attribute or a kenmerk),
         *  case-insensitively? Used to decide whether a navigation can emit a getter for it
         *  or must stay an honest marker. */
        public boolean hasMember(String name) {
            return attribute(name) != null || characteristicName(name) != null;
        }

        /** Is {@code other} this type or one of its ancestors? The ⊑ of IR-SPEC ch. 2.2, which is
         *  what every type-ranging position ranges over and what a role's declared filler type
         *  bounds from above. */
        public boolean isA(ObjectType other) {
            return other != null && chain().contains(other);
        }

        /** The §3.5 soort of kenmerk {@code naam} (case-insensitively), or null when no such kenmerk is
         *  declared. An untagged kenmerk is {@link CharacteristicKind#ZELFSTANDIG}. */
        public CharacteristicKind characteristicKind(String name) {
            return onChain(t -> t.characteristicKinds, name);
        }

        /** The type on this chain that DECLARES {@code naam} as an attribute or kenmerk — this type
         *  itself when it declares it, null when nothing on the chain does. The ch. 7.2 conflict key
         *  names it, so one target reports under one name however many subtypes inherit it. */
        public ObjectType declarantOf(String name) {
            for (ObjectType t : chain())
                if (t.attributes.keySet().stream().anyMatch(k -> k.equalsIgnoreCase(name))
                        || t.characteristics.stream().anyMatch(k -> k.equalsIgnoreCase(name)))
                    return t;
            return null;
        }
    }

    /** §13.3.8 a declared parameter. {@code granularity} is its §13.3.6 tijdlijn ("kan jaarlijks wijzigen", written
     *  "voor elk jaar") when the parameter is tijdsafhankelijk — its runtime value is then a timeline (a
     *  {@code List<Segment>}) rather than a scalar; null for an ordinary scalar parameter. */
    public record Parameter(String name, Datatype datatype, Granularity granularity, String fixedStartPoint) {
        public Parameter(String name, Datatype datatype) { this(name, datatype, null, null); }
    }

    public static final class FactType {
        public final String name;
        public final List<Role> roles = new ArrayList<>();
        public boolean reciprocal;
        public FactType(String name) { this.name = name; }
    }
    /** A feittype role. `plural` is the plural the model DECLARES via "(mv: …)"
     *  (§13.3.9.1), or null when none is declared — the resolver matches plural
     *  references against the declared form, never a synthesised one. `many` is the
     *  role's cardinality from the feittype's cardinaliteit line (§13.3.9.1): true when this
     *  role is the "meerdere" side (a navigation TO it yields many), false when "één". */
    public record Role(String name, String objectTypeName, String plural, boolean many) {
        public Role(String name, String objectTypeName) { this(name, objectTypeName, null, false); }
        public Role(String name, String objectTypeName, String plural) { this(name, objectTypeName, plural, false); }
    }

    /** Whether navigating from {@code houder} to {@code target} through the role named
     *  {@code roleName} crosses a TO-MANY (meerdere) role — i.e. the field on {@code houder}
     *  would be a collection, so a scalar getter chain is wrong and the read needs an
     *  aggregation (§13.3.9.1 cardinaliteit). Finds the unique {@code target} role that
     *  {@code roleName} names in a feittype relating {@code houder} and returns its
     *  cardinality; false when single, unknown, or not uniquely found (fail-open: never
     *  refuse a navigation we cannot prove is to-many). */
    public boolean toManyRole(ObjectType houder, ObjectType target, String roleName) {
        if (target == null || roleName == null) return false;
        Role found = null; int n = 0;
        for (FactType ft : factTypes.values())
            for (Role r : ft.roles)
                if (r.name().equalsIgnoreCase(roleName) && roleHasObjectType(r, target) && factTypeRelates(ft, r, houder)) {
                    found = r; n++;
                }
        return n == 1 && found.many();
    }

    /** §13.4.6/§9 whether {@code subject} can relate to MORE THAN ONE instance of the created type
     *  {@code fresh} — the cardinality that decides an objectcreatie's idempotence guard shape: a 1:1
     *  target ({@code false}) can be keyed on the LINKED-INSTANCE's absence (a fact-base-derived check on
     *  the created type itself); a to-many target ({@code true}) needs a per-rule per-subject control fact
     *  (a linked instance already existing is legitimate). Reads the cardinality of {@code fresh}'s own role
     *  in the unique feittype relating the pair — {@link #connectingRole}(subject, fresh) names that role,
     *  {@link #toManyRole} reads its meervoudig. Fail-open to to-many ({@code true}) when the role cannot be
     *  uniquely resolved: the control-fact guard is always correct (at-most-once), only less tight than the
     *  1:1 instance check, so an unprovable pair is never wrongly treated as 1:1. */
    public boolean subjectToManyCreation(ObjectType subject, ObjectType fresh) {
        if (subject == null || fresh == null) return true;   // no cardinality knowledge -> the safe (control-fact) path
        String newRole = connectingRole(subject, fresh);    // the role nieuw plays, from the subject's side
        if (newRole == null) return true;                   // ambiguous / self-pair -> control-fact path
        return toManyRole(subject, fresh, newRole);
    }

    /** Package-visible role-name match (singular, declared plural, or best-effort plural),
     *  used by {@code dev.uchar.regelspraak.resolver.DefinitionResolver} to map a cardinaliteit
     *  noun onto a role. */
    public static boolean roleNameMatches(Role r, String word) { return roleMatches(r, word); }

    /** One resolved hop of a §13.4.1 multi-hop navigation chain ("de X van de Y van de Z"):
     *  the DECLARED role name to navigate through from the holder, and the object type it reaches. */
    public record ChainHop(String role, ObjectType targetName) {}

    /** Resolve one written chain filler (a role name or an objecttype name, article already stripped)
     *  into a to-ONE hop from {@code houder} — or null when it does not resolve uniquely or crosses a
     *  to-many role, in which case the caller declines the WHOLE chain (an honest marker, never a wrong
     *  join). A filler that names an objecttype directly takes the by-pair role (§13.4.1.2/.3: the
     *  objecttype reading), any other filler the by-name role reading. */
    public ChainHop chainHop(ObjectType houder, String written) {
        ChainHop hop = writtenHop(houder, written);
        return hop == null || toManyRole(houder, hop.targetName(), hop.role()) ? null : hop;
    }

    /** The hop {@link #chainHop} resolves, a to-MANY one included, or null when the filler names no role or
     *  several. The check of a read that did not bind walks the same hops and must tell a to-many step apart from
     *  a name that is no role ({@code RuleChecker}). */
    public ChainHop writtenHop(ObjectType houder, String written) {
        if (houder == null || written == null) return null;
        ObjectType asType = objectTypeByName(written);   // STRICT name match — a role-named filler must take the role reading
        String role; ObjectType targetName;
        if (asType != null) { targetName = asType; role = connectingRole(houder, asType); }
        else {
            targetName = roleTargetTypeToSubject(houder, written);
            role = targetName == null ? null : connectingRoleViaName(houder, targetName, written);
        }
        return role == null || targetName == null ? null : new ChainHop(role, targetName);
    }

    /** Re-walk a RESOLVED navigation chain (declared role names, subject-outward) from {@code van},
     *  returning the object type reached per hop (size == pad size) — or null when any hop does not
     *  resolve to-one from there. The transpilers re-validate a chain against their actual pattern
     *  subject with this before emitting getters, so a chain resolved against one subject can never
     *  silently lower against another. */
    public List<ObjectType> followChain(ObjectType van, List<String> path) {
        List<ObjectType> reached = new ArrayList<>();
        ObjectType cur = van;
        for (String role : path) {
            if (cur == null) return null;
            ObjectType targetName = roleTargetTypeToSubject(cur, role);
            if (targetName == null || toManyRole(cur, targetName, role)) return null;
            reached.add(targetName);
            cur = targetName;
        }
        return reached;
    }

    public static final class UnitSystem {
        public final String name;
        public final Map<String, Unit> units = new LinkedHashMap<>(); // abbreviation -> unit
        public final Map<String, Unit> spellings = new LinkedHashMap<>(); // unit word / plural (normalised) -> unit
        /** The EXACT conversion factor of each declared unit, by abbreviation: the product of the fractions its
         *  declaration chain writes ("= 1/60 u" over "= 1/24 dg" is 1/1440), a base unit 1/1. The unit's own
         *  {@link Unit#factorToCanonical} is the product of those fractions each rounded to 16 digits, which no
         *  reader can turn back into the fraction for certain; the wire writes this one ({@code factorToCanonical}
         *  of uchar-ir-v1 is an exact fraction). */
        public final Map<String, Factor> exactFactors = new LinkedHashMap<>();
        public UnitSystem(String name) { this.name = name; }

        /** A fraction in lowest terms, its sign on the numerator. A declared factor may be zero or negative; the
         *  emitter refuses those, because the wire writes a factor as a positive fraction. */
        public record Factor(java.math.BigInteger num, java.math.BigInteger den) {
            public static final Factor EEN = new Factor(java.math.BigInteger.ONE, java.math.BigInteger.ONE);

            public Factor {
                if (den.signum() == 0) throw new IllegalArgumentException("een factor met noemer 0");
                if (den.signum() < 0) { num = num.negate(); den = den.negate(); }
                java.math.BigInteger g = num.gcd(den);
                num = num.divide(g);
                den = den.divide(g);
            }

            public Factor times(Factor o) { return new Factor(num.multiply(o.num), den.multiply(o.den)); }
        }
    }

    /** §13.3.6.2/§13.3.6.10 the period unit of a tijdlijn — the granularity at which a temporal value
     *  changes (one value per dag / week / maand / kwartaal / jaar). Both the inline attribute form
     *  ("voor elk jaar") and a named {@link Timeline} definition ("heeft tijdvakken van een jaar")
     *  reduce to one of these. */
    public enum Granularity { DAG, WEEK, MAAND, KWARTAAL, JAAR }

    /** §13.3.6.10 a named timeline: its naam, its {@link Granularity} and period {@code count} (so a "voor
     *  elke &lt;naam&gt;" attribute reference adopts BOTH — "heeft tijdvakken van 5 maanden" is granularity MAAND,
     *  count 5), whether `variableStartPoint` is bound by a §13.4.12 startpuntbepaling, and its fixed start point
     *  in the wire spelling (IR-SPEC ch. 2.8), null on the calendar grid or with a variable start. */
    public record Timeline(String name, boolean variableStartPoint, Granularity granularity, int count,
                           String fixedStartPoint) {}

    /** §13.3.7 a dimension and its ordered label values. {@code preposition} is the §3.6 read preposition
     *  for the suffix form ("na het attribuut met voorzetsel <vz>": a cell reads "&lt;attribuut&gt; &lt;vz&gt;
     *  &lt;label&gt;"), or null for the prefix form ("voor het attribuut zonder voorzetsel": "&lt;label&gt;
     *  &lt;attribuut&gt;"). */
    public record Dimension(String name, String pluralName, String preposition, List<String> values) {}

    /** The §13.3.7 dimension named {@code naam} (singular) case-insensitively, or null. */
    public Dimension dimensionByName(String name) {
        if (name == null) return null;
        Dimension exact = dimensions.get(name);
        if (exact != null) return exact;
        for (Dimension d : dimensions.values())
            if (d.name().equalsIgnoreCase(name)) return d;
        return null;
    }

    /** The §13.3.7 dimension whose PLURAL name (dimensienaammeervoud) is {@code plural},
     *  case-insensitively, or null — the §13.4.17.52-.54 over-forms reference a dimension by its
     *  plural ("over alle jaardimensies", "over de jaardimensies vanaf … t/m …"). */
    public Dimension dimensionByPlural(String plural) {
        if (plural == null) return null;
        for (Dimension d : dimensions.values())
            if (d.pluralName() != null && d.pluralName().equalsIgnoreCase(plural)) return d;
        return null;
    }

    // --- the vocabulary side table (IR-SPEC ch. 2.12) -----------------------------------------------------

    /** A named vocabulary: its name, whether it is the default one, and its language code (null: none declared). A
     *  vocabulary has no kind: every designation is text (maintainer decision 2026-09-27). */
    public record Vocabulary(String name, boolean isDefault, String language) {}

    /** One designation: the vocabulary it is written in, and its text. */
    public record Designation(String vocabulary, String wording) {}

    /** A declaration a vocabulary can name (IR-SPEC ch. 2.12): its kind, the declaration that scopes it where the kind
     *  is scoped — the object type of an attribute or a characteristic (the one that DECLARES it), the fact type of a
     *  role, the domain of an enumeration value; null otherwise — and its canonical name, each as declared. A named
     *  form ({@link Kind#VORM}) is named by its form name, without a version suffix, whatever list it stands in. */
    public record Designatable(Kind category, String owner, String name) {
        public enum Kind { OBJECTTYPE, ATTRIBUUT, KENMERK, FEITTYPE, ROL, ENUMERATIEWAARDE, PARAMETER, VORM, REGELGROEP }

        public static Designatable objectType(String name) { return new Designatable(Kind.OBJECTTYPE, null, name); }
        public static Designatable attribute(String objectType, String name) { return new Designatable(Kind.ATTRIBUUT, objectType, name); }
        public static Designatable characteristic(String objectType, String name) { return new Designatable(Kind.KENMERK, objectType, name); }
        public static Designatable factType(String name) { return new Designatable(Kind.FEITTYPE, null, name); }
        public static Designatable role(String factType, String name) { return new Designatable(Kind.ROL, factType, name); }
        public static Designatable enumerationValue(String domain, String value) { return new Designatable(Kind.ENUMERATIEWAARDE, domain, value); }
        public static Designatable parameter(String name) { return new Designatable(Kind.PARAMETER, null, name); }
        public static Designatable form(String name) { return new Designatable(Kind.VORM, null, name); }
        public static Designatable ruleGroup(String name) { return new Designatable(Kind.REGELGROEP, null, name); }
    }

    /** The form that makes an element under a name of its own, with a fixed key for consumers. The frontend makes the
     *  first five ({@link #generated}); the last two name entries of the Drools backend's coverage manifest. */
    public enum OriginForm {
        /** The opgave marker on attribute X (dialect §7): {@code opgegeven X}, {@code berekende X},
         *  {@code afwijkende opgave X}, {@code afwijking opgave X}, {@code controle opgave X} and
         *  {@code opgave boven berekening X}, from X. */
        OPGAVE_BOVEN_BEREKENING("opgave-boven-berekening"),
        /** The group {@code B voor S} of the blocks written for a subtype S under base rule B (dialect §4, the
         *  inheritance line), from the form B. */
        EXCEPTION_FOR_SUBTYPE("uitzondering-voor-subtype"),
        /** A row of decision table B, from B, named as a form: the table is no element of the model, its rows are. */
        BESLISTABELRIJ("beslistabelrij"),
        /** The member and the rule {@code <lid> via <rol 1>, … en <rol n>} of a fold over a set and to-one steps
         *  after it, from the member the steps reach, as declared. */
        STEPS_AFTER_SET("stappen-na-de-verzameling"),
        /** The anchor member {@code startpunt <tijdlijn>} of a start point determination, from the rule that writes
         *  it. */
        START_POINT("startpunt"),
        /** An effective defaults stack of the Drools backend, from the member it decides, as declared. */
        DEFAULTS_STACK("defaultsstapel"),
        /** A version {@code R #k} of a form R with several versions, from R. */
        VERSION("versie");

        private final String key;

        OriginForm(String key) { this.key = key; }

        /** The fixed key of this form. */
        public String key() { return key; }
    }

    /** Where a generated element comes from: the declared element ({@code source}) and the form that made it. */
    public record GeneratedFrom(Designatable source, OriginForm form) {
        public GeneratedFrom {
            java.util.Objects.requireNonNull(source, "source");
            java.util.Objects.requireNonNull(form, "form");
        }
    }

    /** The origin of {@code element} when the frontend made it ({@link #generated}), matched on its kind and
     *  case-insensitively on its owner and name, the name identity of IR-SPEC ch. 2.1; empty for an element whose
     *  name the author wrote, and for one the model does not have. */
    public java.util.Optional<GeneratedFrom> generatedFrom(Designatable element) {
        if (element == null) return java.util.Optional.empty();
        for (var e : generated.entrySet())
            if (same(e.getKey(), element)) return java.util.Optional.of(e.getValue());
        return java.util.Optional.empty();
    }

    /** The version suffix a model entry of a form with several versions carries ({@code "<naam> #<i>"}). */
    private static final java.util.regex.Pattern VERSION = java.util.regex.Pattern.compile("([\\s\\S]*) #[0-9]+");

    /** The form name of a model entry: its name without a version suffix. */
    public static String formName(String name) {
        if (name == null) return null;
        java.util.regex.Matcher m = VERSION.matcher(name);
        return m.matches() ? m.group(1) : name;
    }

    /** The vocabulary named {@code naam}, compared case-insensitively (IR-SPEC ch. 2.1), or null. */
    public Vocabulary vocabulary(String name) {
        if (name == null) return null;
        for (Vocabulary v : vocabularies) if (v.name().equalsIgnoreCase(name)) return v;
        return null;
    }

    /** The designation of {@code element} in the vocabulary {@code vocabulary} (its name compared case-insensitively),
     *  or empty when the declaration carries none there. The element is matched on its kind and case-insensitively on
     *  its owner and name, the name identity of IR-SPEC ch. 2.1. */
    public java.util.Optional<String> designation(String vocabulary, Designatable element) {
        if (vocabulary == null || element == null) return java.util.Optional.empty();
        for (var e : designations.entrySet()) {
            if (!same(e.getKey(), element)) continue;
            for (Designation b : e.getValue())
                if (b.vocabulary().equalsIgnoreCase(vocabulary)) return java.util.Optional.of(b.wording());
        }
        return java.util.Optional.empty();
    }

    /** The effective name of {@code element} in {@code vocabulary}: its designation there, and its canonical name where
     *  it carries none (IR-SPEC ch. 2.12). */
    public String effectiveName(String vocabulary, Designatable element) {
        return designation(vocabulary, element).orElse(element.name());
    }

    /** The reverse of {@link #effectiveName}: the declaration of kind {@code category} whose effective name in
     *  {@code vocabulary} is {@code naam}, compared case-insensitively, within {@code owner} where the kind is scoped
     *  — for an attribute or a characteristic along the supertype chain of that object type, so an inherited member is
     *  found from its subtype; null {@code owner} for the other kinds. Empty when no declaration has that effective
     *  name, or when two have (WF-118 refuses the second on the wire, so a verifier-clean document answers uniquely);
     *  the vocabulary need not name every declaration. */
    public java.util.Optional<Designatable> namedElement(String vocabulary, Designatable.Kind category, String owner,
                                                  String name) {
        if (vocabulary == null || category == null || name == null) return java.util.Optional.empty();
        List<String> owners = new ArrayList<>();
        if (category == Designatable.Kind.ATTRIBUUT || category == Designatable.Kind.KENMERK) {
            ObjectType ot = owner == null ? null : objectTypeByName(owner);
            if (ot == null) return java.util.Optional.empty();
            for (ObjectType t : ot.chain()) owners.add(t.name);
        } else if (owner != null) {
            owners.add(owner);
        }
        Designatable found = null;
        for (Designatable b : designatable()) {
            if (b.category() != category) continue;
            if (b.owner() == null ? !owners.isEmpty()
                    : owners.stream().noneMatch(e -> e.equalsIgnoreCase(b.owner()))) continue;
            if (!effectiveName(vocabulary, b).equalsIgnoreCase(name)) continue;
            if (found != null) return java.util.Optional.empty();
            found = b;
        }
        return java.util.Optional.ofNullable(found);
    }

    /** Every declaration of this model a vocabulary can name, in the order the wire writes them: the object types, each
     *  with its own attributes and characteristics; the fact types, each with its roles; the parameters; the values of
     *  every enumeration domain; the named forms — rules, object creations, allocations (the ones the wire writes),
     *  consistencies, uniquenesses, fact creations, defaults groups — each once, by its form name; the rule groups. */
    public List<Designatable> designatable() {
        List<Designatable> result = new ArrayList<>();
        for (ObjectType ot : objectTypes.values()) {
            result.add(Designatable.objectType(ot.name));
            for (String a : ot.attributes.keySet()) result.add(Designatable.attribute(ot.name, a));
            for (String k : ot.characteristics) result.add(Designatable.characteristic(ot.name, k));
        }
        for (FactType ft : factTypes.values()) {
            result.add(Designatable.factType(ft.name));
            for (Role r : ft.roles) result.add(Designatable.role(ft.name, r.name()));
        }
        for (Parameter p : parameters.values()) result.add(Designatable.parameter(p.name()));
        for (var d : domains.entrySet())
            if (d.getValue() instanceof Datatype.Enumeration e)
                for (String w : e.values()) result.add(Designatable.enumerationValue(d.getKey(), w));
        java.util.Set<String> forms = new java.util.LinkedHashSet<>();
        for (var r : resolvedRules) forms.add(formName(r.name()));
        java.util.Set<String> list = new java.util.LinkedHashSet<>();
        List<java.util.Set<String>> lists = new ArrayList<>();
        lists.add(forms);
        java.util.function.Consumer<List<String>> neem = names -> {
            java.util.Set<String> l = new java.util.LinkedHashSet<>();
            for (String n : names) l.add(formName(n));
            lists.add(l);
        };
        neem.accept(objectCreations.stream().map(dev.uchar.regelspraak.ir.ResolvedObjectCreation::name).toList());
        neem.accept(allocations.stream()
                .filter(v -> v.method() != dev.uchar.regelspraak.ir.ResolvedAllocation.Method.COMPLEX)
                .map(dev.uchar.regelspraak.ir.ResolvedAllocation::name).toList());
        neem.accept(consistencies.stream().map(dev.uchar.regelspraak.ir.ResolvedConsistency::name).toList());
        List<String> unique = new ArrayList<>(uniquenesses.stream().map(dev.uchar.regelspraak.ir.ResolvedUniqueness::name).toList());
        unique.addAll(uniquenessUnions.stream().map(dev.uchar.regelspraak.ir.ResolvedUniquenessUnion::name).toList());
        neem.accept(unique);
        neem.accept(factCreations.stream().map(dev.uchar.regelspraak.ir.ResolvedFactCreation::name).toList());
        neem.accept(prioritizedDefaults.stream().map(dev.uchar.regelspraak.ir.PrioritizedDefaults::name).toList());
        for (java.util.Set<String> l : lists)
            for (String n : l) if (n != null) result.add(Designatable.form(n));
        for (var g : ruleGroups) if (g.name() != null) result.add(Designatable.ruleGroup(g.name()));
        return result;
    }

    /** Whether two elements name one declaration: the same kind, and owner and name equal under the name identity. */
    private static boolean same(Designatable a, Designatable b) {
        return a.category() == b.category() && a.name().equalsIgnoreCase(b.name())
                && (a.owner() == null ? b.owner() == null
                    : b.owner() != null && a.owner().equalsIgnoreCase(b.owner()));
    }

    /** A rule shell (name + the parse subtree); type-checking annotates diagnostics. */
    public record Rule(String name, String category) {}

    public enum Severity { ERROR, WARNING, INFO }
    /** A message of the frontend: {@code code} (an {@code RGS-###} of the register) and {@code args} (its named
     *  parameters, a value a string or a {@link Msg}, a sentence part) name the template that {@code message} is
     *  rendered from, in the default language of the frontend. A diagnostic read off the wire carries neither
     *  (IR-SPEC ch. 2 {@code diagnostics} has no code): {@code code} and {@code text} are null there and
     *  {@code message} is the wire's own. {@code text} is the message to render when it is a frame around the
     *  sentence of {@code code} ({@code FRAME.*}), else the code's own template. */
    public record Diagnostic(Severity severity, String code, Map<String, Object> args, Msg text, String message,
                             int line) {
        public Diagnostic {
            args = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(args));
            // rendered at once: a missing entry or a wrong parameter fails where the message arises
            if (text != null) message = text.text();
        }
        /** A message without a code: the wire's, and the form the tests build. */
        public Diagnostic(Severity severity, String message, int line) {
            this(severity, null, Map.of(), null, message, line);
        }
        /** A message that code and parameters name; its text is the catalogue entry of the code. */
        public Diagnostic(Severity severity, String code, Map<String, Object> args, int line) {
            this(severity, code, args, new Msg(Component.FRONTEND, code, args), null, line);
        }
        /** The parameters of a template, in the order given; a value is kept as a {@link Msg} when it is a
         *  sentence part, and renders as the message renders it otherwise. */
        public static Map<String, Object> params(Object... namesAndValues) {
            return Msg.args(namesAndValues);
        }
        /** The parameters as strings, a sentence part in the default language. */
        public Map<String, String> params() {
            Map<String, String> m = new LinkedHashMap<>();
            args.forEach((k, v) -> m.put(k, dev.uchar.regelspraak.message.Messages.valueText(v,
                    dev.uchar.regelspraak.message.Messages.defaultLocale(Component.FRONTEND))));
            return m;
        }
        /** The message in {@code locale}; the wire's own text for a diagnostic without a code. */
        public String message(java.util.Locale locale) { return text == null ? message : text.text(locale); }
        public boolean isError() { return severity == Severity.ERROR; }
        @Override public String toString() { return severity + " (line " + line + "): " + message; }
    }
}
