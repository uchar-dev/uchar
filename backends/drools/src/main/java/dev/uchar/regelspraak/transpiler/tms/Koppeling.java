package dev.uchar.regelspraak.transpiler.tms;

import java.util.Objects;

/**
 * One link of a relation that a form of the document creates: {@code eerste} fills the first declared role of the fact
 * type {@code relatie} and {@code tweede} the second. An object creation inserts the link over its connecting relation
 * and a fact creation the link it creates, each stated, so that it never lapses (RUNTIME-SEMANTICS ch. 5.5); the links
 * the case input carries over such a relation are facts as well, inserted logically from the reference fields of the
 * beans, which do not change while the session fires. A form that reads a created relation, a navigation, the range of
 * a fold or the path of a fact creation, joins over these facts, since this branch writes no bean while it fires and a
 * link that arises then is in no bean field. A relation no form creates is read off the beans, as before.
 *
 * <p>{@code fase} is the creation phase that made the link ({@link Creatiefase}), and 0 for a link of the input: a fact
 * creation does not walk a link of the phase it fires in, which the rule layer has not yet seen. {@code relatie} is the
 * fact type's name in its normalised form. Compared by that name and the two objects by identity, and not by
 * {@code fase}: one link is one fact, however it arose.
 */
public record Koppeling(String relatie, Object eerste, Object tweede, int fase) {

    public Koppeling {
        Objects.requireNonNull(relatie, "relatie");
        Objects.requireNonNull(eerste, "eerste");
        Objects.requireNonNull(tweede, "tweede");
        if (fase < 0) throw new IllegalArgumentException("fase " + fase + " < 0");
    }

    @Override public boolean equals(Object o) {
        return o instanceof Koppeling k && k.eerste == eerste && k.tweede == tweede && k.relatie.equals(relatie);
    }

    @Override public int hashCode() {
        return Objects.hash(relatie, System.identityHashCode(eerste), System.identityHashCode(tweede));
    }

    @Override public String toString() {
        return "Koppeling(" + relatie + ", " + Feitnamen.object(eerste) + ", " + Feitnamen.object(tweede) + ", fase "
                + fase + ")";
    }
}
