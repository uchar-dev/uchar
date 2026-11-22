package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graph.ProducerConsumer;
import dev.uchar.regelspraak.graph.ProducerConsumer.ElementKey;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.ir.ResolvedRule;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;
import dev.uchar.regelspraak.transpiler.core.Namen;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The members of a document that can be open on an object (RUNTIME-SEMANTICS ch. 1.5), an upper bound: none in a
 * document that infers from an empty value, so that its lowering asks nowhere whether something is open. In a document
 * with {@code inferFromEmpty: false} a member can be open
 * <ul>
 *   <li>as an input that is not complete ({@link #openInput}): a member that holds one value, on the objects of a type on
 *       which no rule and no defaults group writes it, unless its declaration says that its input is complete;</li>
 *   <li>by a writer ({@link #leftOpen}): a member a form writes that reads a member that can be open, at any depth of
 *       its condition and value ({@link ProducerConsumer#forms}).</li>
 * </ul>
 *
 * <p>A population and the link set of a relation, which {@link ProducerConsumer#forms} does not key, are complete but
 * where a creation's condition is undetermined (ch. 1.5, "A creation"): where a creation reads a member that can be open,
 * every form counts as reading one.
 */
public final class OpenableMembers {

    /** No member. */
    static final OpenableMembers NONE = new OpenableMembers(Map.of(), Set.of(), Set.of());

    private final Map<ObjectType, Set<ElementKey>> inputOf;
    private final Set<ElementKey> openInput, leftOpen;

    private OpenableMembers(Map<ObjectType, Set<ElementKey>> inputOf, Set<ElementKey> openInput, Set<ElementKey> leftOpen) {
        this.inputOf = inputOf;
        this.openInput = openInput;
        this.leftOpen = leftOpen;
    }

    /** The members of {@code model} with its defaults groups {@code defaults} that can be open. This is the one place
     *  where the lowering reads the document's {@code inferFromEmpty}. */
    static OpenableMembers of(RegelSpraakModel model, List<PrioritizedDefaults> defaults,
                              ProducerConsumer.Navigation navigation) {
        if (model.inferFromEmpty()) return NONE;
        Map<ObjectType, Set<ElementKey>> inputOf = new IdentityHashMap<>();
        Set<ElementKey> openInput = new HashSet<>();
        for (ObjectType type : model.objectTypes.values()) {
            Set<ElementKey> of = incomplete(model, defaults, type);
            inputOf.put(type, Collections.unmodifiableSet(of));
            openInput.addAll(of);
        }
        List<ProducerConsumer.Form> forms = ProducerConsumer.forms(model, defaults, navigation);
        Set<ElementKey> openable = new HashSet<>(openInput), leftOpen = new HashSet<>();
        boolean relations = false, grown = true;
        while (grown) {
            grown = false;
            for (ProducerConsumer.Form f : forms) {
                if (!relations && f.reads().stream().noneMatch(openable::contains)) continue;
                boolean creation = f.kind() == ProducerConsumer.FormKind.OBJECT_CREATION
                        || f.kind() == ProducerConsumer.FormKind.FACT_CREATION;
                boolean opensMember = leftOpen.addAll(f.writes()), opensRelations = creation && !relations;
                grown |= opensMember || opensRelations;
                relations |= creation;
                openable.addAll(f.writes());
            }
        }
        return new OpenableMembers(inputOf, openInput, leftOpen);
    }

    /** Whether no member can be open. */
    public boolean isEmpty() {
        return openInput.isEmpty() && leftOpen.isEmpty();
    }

    /** Whether {@code member} can be open on an object. */
    boolean contains(ElementKey member) {
        return openInput.contains(member) || leftOpen.contains(member);
    }

    /** Whether {@code member} can be open as an input that is not complete. */
    boolean openInput(ElementKey member) {
        return openInput.contains(member);
    }

    /** Whether a writer can leave {@code member} open. */
    boolean leftOpen(ElementKey member) {
        return leftOpen.contains(member);
    }

    /** The members of an object of {@code type}, its own class, that are input there and not complete, in the order of
     *  their declarations from the type up: each is open where the case gives it no value or does not name it. */
    public Set<ElementKey> incompleteInput(ObjectType type) {
        return inputOf.getOrDefault(type, Set.of());
    }

    private static Set<ElementKey> incomplete(RegelSpraakModel model, List<PrioritizedDefaults> defaults, ObjectType type) {
        Set<ElementKey> out = new LinkedHashSet<>();
        for (ObjectType t : type.chain()) {
            for (String a : t.attributes.keySet())
                if (t.attributeGranularity.get(a) == null && !t.attributeDimensions.containsKey(a)
                        && !t.completeAttributes.contains(a) && !written(model, defaults, type, a, false))
                    out.add(ProducerConsumer.elementOf(t, a, false));
            for (String k : t.characteristics)
                if (t.characteristicGranularity.get(k) == null && !t.completeCharacteristics.contains(k)
                        && !written(model, defaults, type, k, true))
                    out.add(ProducerConsumer.elementOf(t, k, true));
        }
        return out;
    }

    /** Whether a rule or a defaults group of the document writes the member {@code name}, a characteristic where
     *  {@code characteristic}, on the objects of {@code type}: one on {@code type} or on a type above it. */
    private static boolean written(RegelSpraakModel model, List<PrioritizedDefaults> defaults, ObjectType type, String name,
                                   boolean characteristic) {
        for (ResolvedRule r : model.resolvedRules) {
            Expr target = r.target() instanceof Expr.DimensionCellRead c ? c.attribute() : r.target();
            if (target instanceof Expr.AttributeReference a && a.objectType() != null && a.attribute() != null
                    && (r.kind() == ResolvedRule.Kind.KENMERKTOEKENNING) == characteristic
                    && Namen.norm(a.attribute()).equals(Namen.norm(name)) && type.isA(a.objectType()))
                return true;
        }
        for (PrioritizedDefaults p : defaults) {
            ObjectType subject = model.objectTypeByName(p.subject());
            if (subject != null && p.target() != null && p.isCharacteristic() == characteristic
                    && Namen.norm(p.target()).equals(Namen.norm(name)) && type.isA(subject))
                return true;
        }
        return false;
    }
}
