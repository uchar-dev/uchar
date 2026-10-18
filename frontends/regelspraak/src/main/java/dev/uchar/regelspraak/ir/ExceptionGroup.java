package dev.uchar.regelspraak.ir;

import java.util.List;

/**
 * §6.1 (Phase 5c) one member of a {@link Default}'s exception group ({@code RUNTIME-SEMANTICS} ch. 7.9):
 * a default chain (descending, highest first) inheriting the root group's target and itself possibly nesting
 * via {@link Default#exceptions}. An exception group carries NO target of its own. The RegelSpraak resolver builds
 * it from the dialect's defaults tree ({@code grammar/DIVERGENCES.md} §4, form 2): one ordered group holding the
 * children of an exception, later-written first, or one group per member of an {@code Uitzonderingen ongeordend
 * op} block. A head makes it, not a rule, so there it carries the head's text as {@link #bron}: "Uitzondering op
 * E1" for the ordered group of E1's children, "Uitzonderingen ongeordend op E1" for a member's group.
 *
 * @param defaults the member's priority chain, highest first (at least one)
 * @param bron     the source text, or null
 */
public record ExceptionGroup(List<Default> defaults, String bron) {}
