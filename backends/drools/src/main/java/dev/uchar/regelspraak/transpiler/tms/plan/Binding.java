package dev.uchar.regelspraak.transpiler.tms.plan;

import dev.uchar.regelspraak.transpiler.core.Namen;

import java.util.List;

/**
 * A value a pattern binds to the variable {@code doel}: from its object along the reference fields of {@code path}, null
 * where a link on it is absent, the field {@code field}, read as {@code form}. On a fact of the branch the path is empty
 * and the field is a component of the fact; on a bean every step is a reference field and the field a member, or the
 * last reference field of a path where the binding binds the object the path reaches.
 */
public record Binding(String target, List<Field> path, Field field, ReadForm form, Class<?> category) {

    public Binding {
        path = List.copyOf(path);
        if (category != null && form != ReadForm.IS_SET)
            throw new IllegalArgumentException("alleen een gezet veld toetst een klasse");
    }

    /** The field {@code field} of an object of {@code klasse}, as it is. */
    public static Binding van(String target, Class<?> javaClass, String field) {
        return new Binding(target, List.of(), new Field(javaClass, field), ReadForm.VALUE, null);
    }

    /** A property {@code naam} of the objects of {@code klasse}, as the document declares the member or as a fact of
     *  the branch names its component, and {@code property}, the property a rendering reads or writes: the name of
     *  the bean property, whose getter and setter are the member's ({@link Namen#property}), and on a fact of the
     *  branch the component itself. */
    public record Field(Class<?> javaClass, String naam, String property) {

        /** The property {@code naam} of the objects of {@code klasse}, with the bean property it has. */
        public Field(Class<?> javaClass, String naam) {
            this(javaClass, naam, Namen.property(naam));
        }
    }

    /**
     * How the field is read: {@code WAARDE} as it is; {@code WAAR} as whether it holds {@code true}, a characteristic
     * that holds one truth value; {@code PERIODS} as the true periods of a characteristic on a timeline, a list of
     * segments; {@code IS_SET} as whether a reference field is set, to an instance of {@code soort} where that is not
     * null.
     */
    public enum ReadForm { VALUE, WAAR, PERIODS, IS_SET }
}
