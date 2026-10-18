package dev.uchar.regelspraak.transpiler.tms;

/**
 * The stated mark the driver leaves in a session of the exec-model branch after each run
 * ({@code DroolsBackend.fire}): that the session was fired through the driver, and how its last run ended.
 * {@code noFixpoint} is null where the run reached its fixpoint, and otherwise the sentence of a run that ended without
 * one: at the fire cap, at the creation budget, or with an exception out of the fire. Only the driver fires to the
 * fixpoint, and on a model with a creation only the driver fires the creations, in generations; a plain
 * {@code fireAllRules} leaves no mark. So {@link TmsTranspiler#read} and {@link TmsTranspiler#setDerivedValues} read
 * a session only under a mark whose {@code noFixpoint} is null and where nothing happened outside the driver after
 * it, which the mark cannot see and the {@link Watch} keeps, and what they give is then always a fixpoint. The
 * driver replaces the mark at every run and holds none in the session while it fires, so the facts that compute the
 * fire cap are the consumer's and the branch's own. On a model with a creation it refuses a session that holds one:
 * what a creation made is stated and does not lapse, so a later fire after an input change would keep it beside an
 * input it no longer follows from. No rule reads it.
 */
public record Fired(String noFixpoint) {}
