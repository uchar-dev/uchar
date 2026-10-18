package dev.uchar.regelspraak.ir;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;

import java.util.List;

/**
 * A resolved §8.1.6 uniciteitscontrole ("de &lt;attribuut&gt; van alle &lt;Type&gt; moeten uniek
 * zijn", or a concatenatie of several attributes): the value — or combination of values — of
 * {@code keys} must be unique across all instances of {@code subject}.
 *
 * <p>The predicate "is uniek" is WAAR when the values are unique (§8.1.6), so the rule is violated
 * precisely when two DISTINCT instances share the key. The transpiler fires on that violation —
 * an instance for which another instance with an equal (non-null) key exists — and records an
 * Inconsistentie, the observable §9.5 "inconsistent" result. A null key cannot establish a duplicate
 * (two unknown identities are not the same real-world object, §8.1.6), so it does not collide.
 *
 * <p>{@code keys} holds one declared attribute name (single-attribute uniqueness) or several (a
 * §5.7 concatenatie). The cross-type {@code verenigd met} form is not lifted here.
 *
 * <p>{@code scopeField} (usually null) makes the uniqueness PARENT-SCOPED — "de X van alle Y van &lt;Z&gt;",
 * unique within each Z rather than globally: it is the reference field on a {@code subject} fact that
 * points at the scope owner, so the count is restricted to instances sharing the subject's scope
 * (e.g. member numbers unique per group: scopeField "groep", counting only deelnemers of the same groep).
 *
 * <p>{@code scopeBinding} (the uchar {@code scopeBinding} extension, null unless the uniqueness is scoped and
 * the scope relation is uniquely derivable) is the symbolic form of {@code scopeField}: the fact type
 * and the (declared) scope-parent role — the reference field's relation, made explicit on the wire.
 */
public record ResolvedUniqueness(String naam, Objecttype subject, List<String> keys, String scopeField,
                                Bron bron, Geldigheid geldigheid,
                                RegelSpraakModel.RoleBindingRef scopeBinding) {

    /** The v0 form without the symbolic scope binding (a global uniqueness, or a scope whose
     *  relation is not uniquely derivable — the extension field then stays absent). */
    public ResolvedUniqueness(String naam, Objecttype subject, List<String> keys, String scopeField,
                             Bron bron, Geldigheid geldigheid) {
        this(naam, subject, keys, scopeField, bron, geldigheid, null);
    }
}
