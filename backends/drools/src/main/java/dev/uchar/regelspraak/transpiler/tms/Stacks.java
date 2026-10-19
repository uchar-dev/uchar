package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.refusal.DroolsCode;
import dev.uchar.regelspraak.refusal.Reason;
import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.ir.Default;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.ir.ResolvedRule;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;
import dev.uchar.regelspraak.transpiler.core.Namen;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The effective defaults stacks of RUNTIME-SEMANTICS ch. 7.10, one per (concrete type, target), composed as the
 * reference interpreter composes them ({@code Desugar.effective_defaults}), with the refusals in this branch's words.
 * This class is the backend's only composition of ch. 7.10, which both renderings of the analysis carry; until card
 * tms-omschakeling (2026-10-10) it was a copy of the destructive exec-model lowering's, kept apart so that the trial of
 * truth maintenance left that lowering untouched.
 *
 * <p>A subtype's own declarations fold over its supertype's effective stack, highest first: its own exceptions, the
 * inherited exceptions, the base (its own replaces the inherited one), the initialisations. A level that declares no
 * group on a target its chain carries a group on contributes its ordinary rule instead (SUBTYPING §4.1 rules 1 and 4):
 * the computing rule as the base, under its condition or the empty {@code all}, and the initialisation as the
 * initialisation default with its condition. Such a rule is absorbed on the concrete types whose stack took it
 * ({@link #absorbedOn}) and must not fire there as a rule as well.
 */
final class Stacks {

    /** One effective stack: the groups that run on instances of exactly {@code owner}, more than one only for a
     *  declared-unordered root. */
    record Stack(ObjectType owner, List<PrioritizedDefaults> members) {}

    /** A composition this branch does not lower, with the reason. */
    static final class Uncomposable extends RuntimeException {
        private final Reason reason;
        Uncomposable(Reason reason) {
            super(reason.text());
            this.reason = reason;
        }
        DroolsCode code() { return reason.code(); }
        Reason reason() { return reason; }
    }

    /** The vacuously true justification an unconditional rule gets when it becomes a base, the shape of the ch. 7.7
     *  desugaring. */
    private static final Condition ALWAYS = new Condition.Compound(Condition.Quantifier.ALLE, 0, List.of(), null);

    private final RegelSpraakModel model;
    private final List<PrioritizedDefaults> defaults;
    /** The level each default was declared on, by identity: a default is read there (ch. 7.10). An exception group's
     *  defaults are read on the level of the default that carries them and are not in here. */
    private final Map<Default, ObjectType> origin = new IdentityHashMap<>();

    Stacks(RegelSpraakModel model, List<PrioritizedDefaults> defaults) {
        this.model = model;
        this.defaults = defaults;
    }

    /** Every effective stack of the model, per type in the model's order and per target in the order of
     *  {@link #effectiveStacks}. */
    List<Stack> stacks() {
        List<Stack> out = new ArrayList<>();
        for (ObjectType ot : model.objectTypes.values())
            for (List<PrioritizedDefaults> members : effectiveStacks(ot, chainGroupKeys(ot)).values())
                out.add(new Stack(ot, members));
        return out;
    }

    /** The level default {@code d} of a stack owned by {@code owner} is read on. */
    ObjectType levelOf(Default d, ObjectType owner) {
        return origin.getOrDefault(d, owner);
    }

    /** Whether {@code otype} itself declares a group on its attribute {@code attr}: a rule beside it is not
     *  inheritance but WF-050's single-writer lift, which the fold leaves alone. */
    boolean ownGroup(ObjectType otype, String attr) {
        String stackKey = "A:" + Namen.norm(attr);
        for (PrioritizedDefaults l : defaults)
            if (model.objectTypeByName(l.subject()) == otype && stackKey(l).equals(stackKey)) return true;
        return false;
    }

    /** The concrete types on which an ordinary rule writing ({@code otype}, {@code attr}) is folded into an effective
     *  stack: every type that is an {@code otype} and whose chain declares a group on the target. Empty where
     *  {@code otype} itself declares one ({@link #ownGroup}). */
    List<ObjectType> absorbedOn(ObjectType otype, String attr) {
        if (ownGroup(otype, attr)) return List.of();
        String stackKey = "A:" + Namen.norm(attr);
        List<ObjectType> out = new ArrayList<>();
        for (ObjectType t : model.objectTypes.values())
            if (t.isA(otype) && chainGroupKeys(t).contains(stackKey)) out.add(t);
        return out;
    }

    /** The stacks of {@code type} by target key. {@code absorb} is the key set of the stack's owner on which an
     *  ordinary rule counts as a base, threaded down so the answer does not change with the depth. */
    private LinkedHashMap<String, List<PrioritizedDefaults>> effectiveStacks(ObjectType type, Set<String> absorb) {
        LinkedHashMap<String, List<PrioritizedDefaults>> own = new LinkedHashMap<>();
        for (PrioritizedDefaults l : defaults)
            if (Namen.norm(l.subject()).equals(Namen.norm(type.name))) {
                own.computeIfAbsent(stackKey(l), k -> new ArrayList<>()).add(l);
                for (Default d : l.defaults()) origin.put(d, type);
            }
        for (String k : absorb)
            if (!own.containsKey(k)) {
                PrioritizedDefaults m = ruleMember(type, k);
                if (m != null) own.put(k, List.of(m));
            }
        LinkedHashMap<String, List<PrioritizedDefaults>> inherited =
                type.supertype == null ? new LinkedHashMap<>() : effectiveStacks(type.supertype, absorb);
        LinkedHashMap<String, List<PrioritizedDefaults>> out = new LinkedHashMap<>(own);
        inherited.forEach((k, parentMembers) -> {
            List<PrioritizedDefaults> ownMembers = own.get(k);
            if (ownMembers == null) {
                out.put(k, parentMembers);
                return;
            }
            if (ownMembers.size() == 1 && parentMembers.size() > 1)
                throw new Uncomposable(DroolsCode.DEFAULTS_OWN_GROUP_OVER_SET.reason("targetName", targetName(k), "objectType", type.name, "groupCount", parentMembers.size()));
            if (ownMembers.size() != 1 || parentMembers.size() != 1)
                throw new Uncomposable(DroolsCode.DEFAULTS_OWN_SET_OVER_STACK.reason("targetName", targetName(k), "objectType", type.name, "ownCount", ownMembers.size(), "inheritedCount", parentMembers.size()));
            out.put(k, List.of(merge(ownMembers.get(0), parentMembers.get(0))));
        });
        return out;
    }

    /** {@code level}'s own contribution to the attribute target {@code key} where it declares no group there: its
     *  ordinary rules read as the ch. 7.7 shape. Null when no ordinary rule on the level writes the target. Every rule
     *  on the target is collected and refused by name where it cannot fold, since the fold takes it out of the rule
     *  circuit: a rule passed over would be silenced without becoming a base. */
    private PrioritizedDefaults ruleMember(ObjectType level, String key) {
        List<ResolvedRule> comps = new ArrayList<>(), inits = new ArrayList<>(), all = new ArrayList<>();
        for (ResolvedRule r : model.resolvedRules) {
            if (!(r.target() instanceof Expr.AttributeReference d) || d.attribute() == null) continue;
            if (d.objectType() != level || !("A:" + Namen.norm(d.attribute())).equals(key)) continue;
            switch (r.kind()) {
                case GELIJKSTELLING, BEREKENING -> { comps.add(r); all.add(r); }
                case INITIALISATIE -> { inits.add(r); all.add(r); }
                default -> { }
            }
        }
        if (all.isEmpty()) return null;
        for (ResolvedRule r : all)
            if (r.validity() != null && !r.validity().always())
                throw new Uncomposable(DroolsCode.DEFAULTS_BASE_WITH_VALIDITY.reason("targetName", targetName(key), "objectType", level.name, "rule", r.name()));
        for (ResolvedRule r : all)
            if (r.value() == null)
                throw new Uncomposable(DroolsCode.DEFAULTS_BASE_WITHOUT_VALUE.reason("targetName", targetName(key), "objectType", level.name, "rule", r.name()));
        if (comps.size() > 1 || inits.size() > 1)
            throw new Uncomposable(DroolsCode.DEFAULTS_TWO_WRITERS_OVER_STACK.reason("targetName", targetName(key), "objectType", level.name, "computationCount", comps.size(), "initialisationCount", inits.size()));
        List<Default> ds = new ArrayList<>();
        String attr = null;
        for (ResolvedRule r : comps) {
            attr = ((Expr.AttributeReference) r.target()).attribute();
            ds.add(new Default(null, r.condition() != null ? r.condition() : ALWAYS, r.value(), false, null, r.name()));
        }
        // the initialisation keeps its condition: justified where the target is empty in the input and the condition
        // holds; folded without it, it wrote its value where the condition failed
        for (ResolvedRule r : inits) {
            attr = ((Expr.AttributeReference) r.target()).attribute();
            ds.add(new Default(null, r.condition(), r.value(), true, null, r.name()));
        }
        for (Default d : ds) origin.put(d, level);
        return new PrioritizedDefaults("regel als basis: " + attr + " van " + level.name, level.name, null, attr, false,
                List.copyOf(ds), "SUBTYPING §4.1 rules 1/4: an ordinary rule is the base of its level", null);
    }

    /** The target keys on which {@code t}'s own chain declares a group. */
    private Set<String> chainGroupKeys(ObjectType t) {
        Set<String> out = new LinkedHashSet<>();
        for (PrioritizedDefaults l : defaults) {
            ObjectType subject = model.objectTypeByName(l.subject());
            if (subject != null && t.isA(subject)) out.add(stackKey(l));
        }
        return out;
    }

    /** The block order of ch. 7.10, one own group over one inherited group. */
    private static PrioritizedDefaults merge(PrioritizedDefaults own, PrioritizedDefaults inherited) {
        List<Default> out = new ArrayList<>();
        out.addAll(exceptions(own));
        out.addAll(exceptions(inherited));
        Default base = baseOf(own) != null ? baseOf(own) : baseOf(inherited);
        if (base != null) out.add(base);
        out.addAll(initialisations(inherited));
        out.addAll(initialisations(own));
        return new PrioritizedDefaults(own.name(), own.subject(), own.characteristic(), own.attribute(),
                own.unordered(), List.copyOf(out), own.source(), own.validity());
    }

    /** The lowest ordinary default, and only when it decides something itself: a group-default delegates to its
     *  exception groups and is no base, and a declared-unordered member has no lowest one, so its block travels whole. */
    private static Default baseOf(PrioritizedDefaults l) {
        if (l.unordered()) return null;
        Default last = null;
        for (Default d : l.defaults()) if (!d.initialisation()) last = d;
        return last != null && last.hasOwnContent() ? last : null;
    }

    private static List<Default> exceptions(PrioritizedDefaults l) {
        Default base = baseOf(l);
        List<Default> out = new ArrayList<>();
        for (Default d : l.defaults()) if (!d.initialisation() && d != base) out.add(d);
        return out;
    }

    private static List<Default> initialisations(PrioritizedDefaults l) {
        List<Default> out = new ArrayList<>();
        for (Default d : l.defaults()) if (d.initialisation()) out.add(d);
        return out;
    }

    /** The kind-discriminated key of a group's target: a characteristic and an attribute of one name stay apart. */
    static String stackKey(PrioritizedDefaults l) {
        return (l.isCharacteristic() ? "K:" : "A:") + Namen.norm(l.target());
    }

    private static String targetName(String stackKey) {
        return (stackKey.startsWith("K:") ? "kenmerk '" : "attribuut '") + stackKey.substring(2) + "'";
    }
}
