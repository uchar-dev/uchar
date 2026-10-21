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
 * @param decision       the characteristic decision (true/false), or null on an attribute default
 * @param condition    the justification, or null on the initialisation default
 * @param consequence  the attribute default's value, or null on a characteristic/group default
 * @param initialisation the initialisation default marker (lowest only)
 * @param exceptions     the nested exception groups (ch. 7.9), or null
 * @param source          the source text, or null where the frontend has none: on a group-default the dialect's head
 *                      that made it; on the opgave and the computation default the opgave marker generates, the
 *                      name of the attribute the default reads ({@code opgegeven X}, {@code berekende X}), so an
 *                      explanation can name the deciding block. A block's statute annotations are not its source
 *                      text: they stand in {@link #statuteAnnotations}
 * @param name           the name the source gives the block the default comes from, or null where it gives none: a
 *                      {@code Regel}, an {@code Uitzondering op X:} or {@code Afwijking van X:} block, a member of an
 *                      {@code Uitzonderingen ongeordend} block; none on a group-default a head makes or on the two
 *                      defaults the opgave marker generates. An extension field without semantics, like
 *                      {@code source} (IR-SPEC ch. 6.1)
 * @param statuteAnnotations the {@code // Bron:} statute annotations written directly above the block the default
 *                      comes from: one opaque string per annotation line, trimmed, non-blank, in source order;
 *                      empty where there are none (the wire then omits the field). The same extension field as on
 *                      a rule (IR-SPEC ch. 3.1 and 6.1, WF-082); nothing here reads or parses an entry
 */
public record Default(Boolean decision, Condition condition, Expr consequence,
                      boolean initialisation, Exceptions exceptions, String source, String name,
                      List<String> statuteAnnotations) {

    public Default {
        statuteAnnotations = List.copyOf(statuteAnnotations);
    }

    /** A default's nested exception groups: one or more {@link ExceptionGroup} members that reconcile as a
     *  declared-{@link #unordered} group at this default's level (WF-075). */
    public record Exceptions(boolean unordered, List<ExceptionGroup> members) {}

    /** Whether this default carries its OWN decision content (a besluit or a consequentie) — false for a
     *  group-default whose only content is its {@link #exceptions} exception groups. */
    public boolean hasOwnContent() { return decision != null || consequence != null; }
}
