package dev.uchar.regelspraak.ir;

import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;
import dev.uchar.regelspraak.model.RegelSpraakModel.RoleBindingRef;

import java.util.List;

/**
 * A resolved §13.4.6 objectcreatie: the subject creates a new object of
 * {@code createdType}, sets the given attributes, and is linked to it through the
 * feittype that relates them. This is the unit the transpiler turns into a DRL
 * consequence that inserts a new fact.
 *
 * @param naam          the regel name
 * @param createdType     the object type being created (null if unresolved)
 * @param subject       the object type that owns the rule (the creator)
 * @param toekenningen  the attribute assignments on the new object
 * @param voorwaarde    the condition guarding the creation, or null
 * @param geldigheid    the §13.4.2.5 version validity window ({@link Validity#ALTIJD} if always)
 * @param connectingRole the created-side role of the back-link where the sentence names one (IR-SPEC ch. 3.3
 *                      {@code connectingRole}), or null: the sentence names the type, or its role name binds no
 *                      unique role, and the back-link is then derived from the type pair
 */
public record ResolvedObjectCreation(String naam, Objecttype createdType, Objecttype subject,
                                    List<MemberInit> initialisations, Condition condition, Validity validity,
                                    RoleBindingRef connectingRole) {
    /** Without a named role: the reader that carries the wire's binding beside the record (the Drools backend's
     *  loader) builds it this way. */
    public ResolvedObjectCreation(String naam, Objecttype createdType, Objecttype subject, List<MemberInit> initialisations,
                                 Condition condition, Validity validity) {
        this(naam, createdType, subject, initialisations, condition, validity, null);
    }

    /** One §13.4.6.2 waardetoekenning on the new object. {@code kenmerk} distinguishes the DISTINCT
     *  §13.4.6.4 kenmerkwaardetoekenning ("&lt;kenmerknaam&gt; gelijk aan waar|onwaar" — {@code attribute}
     *  then holds the DECLARED kenmerk name and {@code waarde} a Booleaan literal, resolver-enforced)
     *  from the §13.4.6.3 attribuutwaardetoekenning (generic expressie RHS). Both back-ends set the
     *  same-named property either way; the flag keeps the IR honest about which spec form it carries. */
    public record MemberInit(String attribute, Expr value, boolean characteristic) {
        /** An attribuutwaardetoekenning (§13.4.6.3). */
        public MemberInit(String attribute, Expr value) { this(attribute, value, false); }
    }
}
