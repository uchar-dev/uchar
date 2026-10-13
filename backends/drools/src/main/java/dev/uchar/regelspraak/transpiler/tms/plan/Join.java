package dev.uchar.regelspraak.transpiler.tms.plan;

/**
 * An identity join: field {@code veld} of the pattern's object, or the object itself where {@code veld} is null, is the
 * object the variable {@code doel} binds, or where {@code doelveld} is not null the object that reference field of it
 * names, by identity, as an indexed join, so that a fact meets only the tuples of its own object. A side that reads null
 * meets nothing. {@code id} is the constraint's own id (see {@link Alfa}).
 */
public record Join(String id, String veld, String doel, Binding.Veld doelveld) {

    /** A join of field {@code veld} of the pattern's object to the object {@code doel} binds itself. */
    public Join(String id, String veld, String doel) {
        this(id, veld, doel, null);
    }
}
