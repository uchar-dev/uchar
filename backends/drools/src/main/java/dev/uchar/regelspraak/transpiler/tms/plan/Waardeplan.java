package dev.uchar.regelspraak.transpiler.tms.plan;

/**
 * A value that a consequence of a rule of the form {@code vorm} computes, as the analysis plans it: the value of a
 * computing rule, an initialisation or a default, and a member initialisation of an object creation, each restated
 * into its target's unit where that is due.
 *
 * <p>Until step 1c of card drl-teksttak-op-truth-maintenance, where the plan of a rule ({@code Regelplan}) takes the
 * consequence and its value over, the values stand apart from the left-hand sides ({@link Linkerkant}), one per value a
 * consequence computes, in the order the rules were lowered.
 */
public record Waardeplan(String vorm, Term waarde) {}
