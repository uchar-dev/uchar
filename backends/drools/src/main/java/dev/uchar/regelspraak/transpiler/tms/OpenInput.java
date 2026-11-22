package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graph.ProducerConsumer.ElementKey;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The input members of the objects of one session that are not complete (RUNTIME-SEMANTICS ch. 1.5, "The three states",
 * "Complete input"): where the bean holds no value for one, an attribute without one or a characteristic that does not
 * hold, the member is open on that object, not settled leeg. One fact, stated beside the beans by
 * {@code DroolsBackend.insert} in a document in which a member can be open ({@link OpenableMembers}), for the members the
 * analysis finds ({@link OpenableMembers#incompleteInput}); a characteristic the case states not to hold
 * ({@code characteristicsAbsent}) is not among them, and a session that treats every input member as complete has none.
 * An object it does not name, one a creation made, has no such member.
 *
 * <p>A rule that reads such a member binds this fact beside the bean's value, as it binds the evaluation date, and the
 * read takes both ({@link ComputeHelpers#readInput}): it says only which members are open where the bean holds nothing,
 * so a value a consumer puts on the bean between two fires fills the member.
 */
public final class OpenInput {

    private final Map<Object, Set<ElementKey>> members;

    /** The members {@code members} per object, by identity. */
    public OpenInput(Map<Object, Set<ElementKey>> members) {
        this.members = Collections.unmodifiableMap(new IdentityHashMap<>(members));
    }

    /** Whether the input of {@code member} of {@code subject} is not complete. */
    public boolean incomplete(Object subject, ElementKey member) {
        return members.getOrDefault(subject, Set.of()).contains(member);
    }

    /** Per object, by identity, the members whose input is not complete. */
    Map<Object, Set<ElementKey>> members() {
        return members;
    }

    @Override public String toString() {
        return "OpenInput(" + members.values().stream().mapToInt(Set::size).sum() + " members of " + members.size()
                + " objects)";
    }
}
