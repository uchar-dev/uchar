package dev.uchar.regelspraak.ir;

import java.util.List;

/**
 * §6.1 one default of a {@link PrioritizedDefaults} group (or of an {@link ExceptionGroup}) — a
 * (justification, consequence) pair aimed at the group's target. On characteristic defaults a justified
 * default decides {@link #decision} (true = attribute, false = do-not-attribute); on attribute defaults a
 * justified default decides {@link #consequence} when it evaluates gevuld, and FALLS THROUGH on a leeg. The
 * {@link #initialisation} default (lowest only) has a null {@link #condition}: it is justified iff the target
 * is empty in the input facts (ch. 7.3). A default carrying {@link #exceptions} is a nested exception group
 * (ch. 7.9): a group-default (no own besluit/consequentie — its exception groups are its decision) or a form-a
 * default (own content too — the exception groups decide first, its own decision the fall-back when
 * they decide nothing).
 *
 * @param besluit       the characteristic decision (true/false), or null on an attribute default
 * @param voorwaarde    the justification, or null on the initialisation default
 * @param consequentie  the attribute default's value, or null on a characteristic/group default
 * @param initialisatie the initialisation default marker (lowest only)
 * @param excepties     the nested exception groups (ch. 7.9), or null
 * @param bron          the statute/source annotation, or null; on a group-default the dialect's head that made it;
 *                      on the opgave and the computation default the opgave marker generates, the name of the
 *                      attribute the default reads ({@code opgegeven X}, {@code berekende X}), so an explanation
 *                      can name the deciding block
 */
public record Default(Boolean decision, Condition condition, Expr consequence,
                      boolean initialisation, Exceptions exceptions, String source) {

    /** A default's nested exception groups: one or more {@link ExceptionGroup} members that reconcile as a
     *  declared-{@link #unordered} group at this default's level (WF-075). */
    public record Exceptions(boolean unordered, List<ExceptionGroup> members) {}

    /** Whether this default carries its OWN decision content (a besluit or a consequentie) — false for a
     *  group-default whose only content is its {@link #exceptions} exception groups. */
    public boolean hasOwnContent() { return decision != null || consequence != null; }
}
