package dev.uchar.regelspraak.transpiler.tms.plan;

/**
 * An identity join: field {@code veld} of the pattern's object is the object the variable {@code doel} binds, by
 * identity, as an indexed join, so that a fact meets only the tuples of its own object. A side that reads null meets
 * nothing. {@code id} is the constraint's own id (see {@link Alfa}).
 */
public record Join(String id, String veld, String doel) {}
