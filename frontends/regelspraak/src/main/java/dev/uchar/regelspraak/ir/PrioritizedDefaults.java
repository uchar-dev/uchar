package dev.uchar.regelspraak.ir;

import java.util.List;

/**
 * §6.1 (Phase 4) the prioritized defaults — the engine-neutral wire form of a prioritised default calculus,
 * per {@code spec/RUNTIME-SEMANTICS.md} ch. 7. One writer aimed at one target (a characteristic XOR an
 * attribute of {@link #onderwerp}); {@link #defaults} are in DESCENDING priority and the highest default that
 * decides wins (attribute defaults fall through on a leeg consequence; the initialisation default, lowest, is
 * justified iff the target is empty in the input). Multiple defaults groups on ONE target are admissible only
 * as a declared-{@link #unordered} group (ch. 7.2), whose differing simultaneous decisions route to the §9.5
 * channel. The RegelSpraak resolver builds it from the dialect's own blocks only — {@code Uitzondering op},
 * {@code Afwijking van} and {@code Uitzonderingen ongeordend} ({@code grammar/DIVERGENCES.md} §4; a flat rule family
 * is never folded into one), one group per tree of blocks under a base rule, nested exceptions as
 * {@link Default#exceptions} — with a {@code false} decision where the block's sentence negates the characteristic
 * ({@code is niet X}, {@code heeft geen X}). It also builds one per attribute that carries the opgave marker
 * {@code (mag opgegeven worden)} ({@code grammar/DIVERGENCES.md} §7): the group {@code opgave boven berekening X} on
 * X, the opgave above the computation and an authored initialisation of X as the lowest default. The Catala frontend
 * and the hand-authored anchors carry the same node.
 *
 * @param naam       the defaults-group name, or null
 * @param onderwerp  the subject objecttype NAME (resolved by the consumer against its model)
 * @param kenmerk    the characteristic target, or null on attribute defaults (exactly one of
 *                   kenmerk/attribuut is non-null, WF-050)
 * @param attribuut  the attribute target, or null on characteristic defaults
 * @param ongeordend declared-unordered: two or more defaults groups on one target require this marking on
 *                   every one
 * @param defaults   the priority chain, highest first
 * @param bron       the statute/source annotation, or null
 * @param geldigheid the §13.4.2.5 validity window ({@link Validity#ALTIJD} = always), or null
 */
public record PrioritizedDefaults(String naam, String onderwerp, String characteristic, String attribute,
                                  boolean unordered, List<Default> defaults, String source,
                                  Validity validity) {

    /** Whether this group writes a characteristic (true) or an attribute (false). */
    public boolean isCharacteristic() { return characteristic != null; }

    /** The bare target name — the characteristic or the attribute, whichever this group carries. */
    public String target() { return characteristic != null ? characteristic : attribute; }
}
