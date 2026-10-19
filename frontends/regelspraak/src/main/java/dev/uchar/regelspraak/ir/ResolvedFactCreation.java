package dev.uchar.regelspraak.ir;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;

/**
 * A resolved §13.4.7 feitcreatie: "Een &lt;roleB&gt; van een &lt;anker&gt; is een &lt;roleC&gt; van
 * &lt;onderwerpD&gt;". It CREATES an instance of the feittype that {@code roleB} belongs to, relating
 * each {@code anchor} to the {@code target} objects that fill {@code roleC} of it (§9.4: a new feit links
 * the two existing objects).
 *
 * <p>The fact model represents a relation instance as a reference field on the many-side fact
 * pointing at the one-side fact (the same convention {@code objectCreatie} links by, e.g.
 * {@code Deelnemer.groep -> Ploeg}). So creation is a {@code modify}-set of {@code setField} — the
 * anker-side role of {@code roleB}'s feittype — on each matching {@code target}.
 *
 * <p>The §9.4 canonical example is MULTI-HOP: {@code onderwerpD} reaches the {@code anchor} through a chain
 * of intermediate objects ("… een deelnemer van de groep met punten van de toegekende reeks punten":
 * Reeks &rarr; Ploeg &rarr; Persoon). {@code pathSteps} captures those
 * intermediate hops from the anker; the {@code joinField} then joins the {@code target} to the LAST step,
 * or — for the 1-hop shape, with {@code pathSteps} empty — directly to the anker.
 *
 * @param naam          the regel name
 * @param anker         the onderwerp1 object type — the "one" side, the rule's iterated subject
 * @param doel          the filler object type — the "many" side of both {@code roleB} and the final {@code roleC} hop
 * @param setField       the reference field on {@code target} -&gt; {@code anchor} for the NEW feittype (set in the consequence)
 * @param joinField      the reference field on {@code target} for the EXISTING final-hop relation (joined in the when)
 * @param tussenstappen the intermediate objects on the navigation path anker &rarr; … &rarr; doel-parent (empty = 1-hop)
 * @param voorwaarde    the condition guarding the creation (on the anker or doel), or null
 * @param geldigheid    the §13.4.2.5 validity window ({@link Validity#ALTIJD} if always)
 * @param feittype      the uchar symbolic form: the created fact type's name (null unless the whole
 *                      symbolic form is derivable — WF-028 all-or-nothing)
 * @param anchorRole      the created fact type's role filled by {@code anchor} (== {@code setField}), symbolic
 * @param targetRole       the created fact type's role filled by {@code target}, symbolic
 * @param path       the RolBinding of the FINAL hop onto {@code target} (the existing relation), symbolic
 */
public record ResolvedFactCreation(String naam, Objecttype anchor, Objecttype target,
                                  String setField, String joinField,
                                  java.util.List<PathStep> pathSteps,
                                  Condition condition, Validity validity,
                                  String factType, String anchorRole, String targetRole,
                                  RegelSpraakModel.RoleBindingRef path) {

    /** The v0 bean-field-only form, without the symbolic creation fields (they stay absent unless the
     *  whole symbolic form — created relation, both ends, join, and every tussenstap — is derivable). */
    public ResolvedFactCreation(String naam, Objecttype anchor, Objecttype target, String setField, String joinField,
                               java.util.List<PathStep> pathSteps, Condition condition, Validity validity) {
        this(naam, anchor, target, setField, joinField, pathSteps, condition, validity, null, null, null, null);
    }

    /** One intermediate object on the §9.4 multi-hop navigation path from {@code anchor} to {@code target}.
     *  The reference field lives on the many side (§13.3.9.1): for a TO-MANY hop the CHILD ({@code type})
     *  holds it, joined {@code type( veld == $prev )}; for a TO-ONE hop the PARENT holds it (the side
     *  objectcreatie set it on), joined {@code type( this == $prev.field )} — {@code viaParentField} selects
     *  which. {@code $prev} is the anker for the first step, the prior step otherwise. {@code roleBinding}
     *  is this hop's symbolic RolBinding (null unless the whole feitcreatie symbolic form is derivable). */
    public record PathStep(Objecttype type, String field, boolean viaParentField,
                             RegelSpraakModel.RoleBindingRef roleBinding) {
        public PathStep(Objecttype type, String field, boolean viaParentField) {
            this(type, field, viaParentField, null);
        }
    }
}
