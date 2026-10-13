package dev.uchar.regelspraak.transpiler.tms.plan;

/**
 * A constraint of a pattern between its own object and the object the variable {@code doel} binds: the predicate
 * {@code functie} over the two, in that order, unindexed, since it compares by value and not by identity. {@code id} is
 * the constraint's own id (see {@link Alfa}).
 */
public record Betatoets(String id, Functie functie, String doel) {}
