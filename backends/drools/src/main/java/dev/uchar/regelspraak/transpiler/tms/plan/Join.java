package dev.uchar.regelspraak.transpiler.tms.plan;

import dev.uchar.regelspraak.transpiler.kern.Namen;

/**
 * An identity join: field {@code veld} of the pattern's object, or the object itself where {@code veld} is null, is the
 * object the variable {@code doel} binds, or where {@code targetField} is not null the object that reference field of it
 * names, by identity, as an indexed join, so that a fact meets only the tuples of its own object. A side that reads null
 * meets nothing. {@code id} is the constraint's own id (see {@link Alpha}). {@code property} is the property a
 * rendering reads of the field, null with {@code veld}: on a bean the bean property of the member
 * ({@link Namen#property}), on a fact of the branch the component itself.
 */
public record Join(String id, String veld, String property, String doel, Binding.Field targetField) {

    public Join {
        if ((veld == null) != (property == null))
            throw new IllegalArgumentException("een join leest een eigenschap precies waar zij een veld leest: " + veld
                    + ", " + property);
    }

    /** A join of field {@code veld} of the pattern's object, with the property it has, to the object {@code doel}
     *  binds, or to the object its reference field {@code targetField} names. */
    public Join(String id, String veld, String doel, Binding.Field targetField) {
        this(id, veld, veld == null ? null : Namen.property(veld), doel, targetField);
    }

    /** A join of field {@code veld} of the pattern's object to the object {@code doel} binds itself. */
    public Join(String id, String veld, String doel) {
        this(id, veld, doel, null);
    }
}
