package dev.uchar.regelspraak.ir;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;

/**
 * A resolved §13.4.10 verdeling: a source amount ({@code sourceAttribute} of
 * {@code sourceType}) is distributed over a recipient attribute ({@code targetAttribute}
 * of {@code targetType}, the objects related to the source) by some method.
 *
 * {@link Method#GELIJKE_DELEN equal shares} (total / count) and {@link Method#NAAR_RATO
 * proportional} (total · weight / sum-of-weights) each have a clean single forward-rule
 * Drools form. {@link Method#OP_VOLGORDE} (§9.7.2 "op volgorde van toenemende/afnemende
 * \<criterium\>, bij een even groot criterium ...") is a genuinely multi-pass ORDERED allocation:
 * recipients are partitioned into groups by {@code orderingAttribute} and the pot is depleted
 * group-by-group in sort order, each group splitting its entry-pot among its members (naar-rato by
 * {@code ratioAttribute}, or equal shares when that is null), carrying the undistributed remainder
 * (after the §9.7.3 cap / §9.7.4 floor) to the next group. It lowers via the registered
 * {@code rsVerdeelOpVolgorde} helper that computes the whole allocation in one shot. Only the truly
 * unhandled forms — an unresolvable join, a non-numeric ordering/weight key, or a maximum on a
 * gelijke-delen tie — stay {@link Method#COMPLEX} (a marked placeholder).
 *
 * <p>The §9.7.3 "met een maximum van", §9.7.4 "afgerond op N decimalen naar beneden" and §9.7.5
 * "onverdeelde rest" modifiers (the {@code meervoudigCriterium} bullet form) ride on every fireable
 * method. For GELIJKE_DELEN / NAAR_RATO without ordering, each share is capped at {@code maximumAttribute}
 * (§9.7.3, naar-rato only — the surplus is NOT redistributed, it falls to the rest), then floored to
 * {@code roundingDecimals} (the §6.1.2 explicit afronding the spec requires before assigning a division
 * result to a bounded target). For OP_VOLGORDE the cap and floor are applied <em>inside</em> the helper,
 * group by group, so the carry-over reflects the capped+floored shares (the §9.7.5 worked example,
 * Tabel 18). {@code remainderAttribute} on the {@code sourceType} receives the remainder (total − Σ shares)
 * via a separate method-agnostic rule.
 *
 * @param ratioAttribute      for {@link Method#NAAR_RATO} the bare recipient weight attribute, and for
 *                           {@link Method#OP_VOLGORDE} the within-group tie weight ("bij een even groot
 *                           criterium naar rato van \<gewicht\>"); null for a gelijke-delen tie. The
 *                           lidwoord is stripped, so it maps straight to a getter.
 * @param maximumAttribute   §9.7.3 the bare recipient ({@code targetType}) attribute capping each share
 *                           (naar-rato / op-volgorde-with-naar-rato-tie only); null = no cap
 * @param roundingDecimals §9.7.4 floor each share to this many decimals (always "naar beneden");
 *                           null = no rounding
 * @param remainderAttribute   §9.7.5 the bare {@code sourceType} attribute that receives the
 *                           undistributed remainder; null = none
 * @param orderingAttribute  §9.7.2 the bare recipient ordering key partitioning the recipients into
 *                           groups; null unless {@link Method#OP_VOLGORDE}
 * @param afnemend           §9.7.2 true = descending ("afnemende"), false = ascending ("toenemende");
 *                           read only when {@link Method#OP_VOLGORDE}
 * @param connectingRole     the recipient join (IR-SPEC ch. 3.2 {@code connectingRole}): the role the recipient
 *                           phrase names where it names one relation between the two types, else the one relation
 *                           between them; null where the resolver bound none, and the emitter then derives it from
 *                           the type pair
 */
public record ResolvedAllocation(String naam, Objecttype sourceType, String sourceAttribute,
                                Objecttype targetType, String targetAttribute,
                                Method method, String ratioAttribute, String maximumAttribute,
                                Integer roundingDecimals, String remainderAttribute,
                                String orderingAttribute, boolean descending,
                                Condition condition, Validity validity,
                                RegelSpraakModel.RoleBindingRef connectingRole) {
    public enum Method { GELIJKE_DELEN, NAAR_RATO, OP_VOLGORDE, COMPLEX }

    /** Without a bound join: the reader that carries the wire's binding beside the record (the Drools backend's
     *  loader) builds it this way. */
    public ResolvedAllocation(String naam, Objecttype sourceType, String sourceAttribute, Objecttype targetType,
                             String targetAttribute, Method method, String ratioAttribute, String maximumAttribute,
                             Integer roundingDecimals, String remainderAttribute, String orderingAttribute,
                             boolean descending, Condition condition, Validity validity) {
        this(naam, sourceType, sourceAttribute, targetType, targetAttribute, method, ratioAttribute, maximumAttribute,
                roundingDecimals, remainderAttribute, orderingAttribute, descending, condition, validity, null);
    }
}
