package dev.uchar.regelspraak.transpiler.tms.plan;

import dev.uchar.regelspraak.transpiler.core.Namen;

import java.util.List;

/**
 * One constraint of a pattern on its own object: the conjunction of {@code tests}, in order, each on one field. It is
 * one constraint and not one per field, with one {@code id}: Drools' executable model shares a constraint between rules
 * by its id, so the analysis gives every constraint an id of its own, and a rendering into that model keeps it.
 */
public record Alpha(String id, List<FieldTest> tests) {

    public Alpha {
        tests = List.copyOf(tests);
        if (tests.isEmpty()) throw new IllegalArgumentException("een alfaconstraint toetst ten minste één veld");
    }

    /**
     * A test against {@code waarde} of what the test reads of the pattern's object: its field {@code field}, or with
     * {@code field} null the object itself, or along the reference fields of {@code path} the object the path reaches,
     * null where a link on it is absent; read as {@code form} ({@link Binding.ReadForm}), as the analysis decided,
     * among them a characteristic on a bean as whether it holds {@code true}. A path ends in the object it reaches: the
     * class of a field beyond it is not the pattern's. A field on a bean is read through the getter of the class
     * {@code declarant} where that is not null, the class that declares the member, which an object the test sees may
     * extend while the pattern's class does not declare it; else through the getter of the pattern's class.
     * {@code property} is the property a rendering reads of the field, null with {@code field}: on a bean the bean
     * property of the member ({@link Namen#property}), on a fact of the branch the component itself.
     */
    public record FieldTest(List<Binding.Field> path, String field, String property, Binding.ReadForm form,
                            Comparator op, Object value, Class<?> declarant) {
        public FieldTest {
            path = List.copyOf(path);
            if ((field == null) != (property == null))
                throw new IllegalArgumentException("een veldtoets leest een eigenschap precies waar zij een veld leest: "
                        + field + ", " + property);
            if (!path.isEmpty() && field != null)
                throw new IllegalArgumentException("een veldtoets langs een pad toetst het object dat het pad bereikt,"
                        + " niet het veld " + field);
            if (declarant != null && field == null)
                throw new IllegalArgumentException("een veldtoets op het object zelf leest geen veld van " + declarant);
        }

        /** A test of what the test reads of the pattern's object, a field through the getter of {@code declarant}, with
         *  the property the field has. */
        public FieldTest(List<Binding.Field> path, String field, Binding.ReadForm form, Comparator op, Object value,
                         Class<?> declarant) {
            this(path, field, field == null ? null : Namen.property(field), form, op, value, declarant);
        }

        /** A test of what the test reads of the pattern's object, a field through the getter of the pattern's class. */
        public FieldTest(List<Binding.Field> path, String field, Binding.ReadForm form, Comparator op, Object value) {
            this(path, field, form, op, value, null);
        }

        /** A test of field {@code field} of the pattern's own object as it is; a null {@code field} tests the object
         *  itself. */
        public FieldTest(String field, Comparator op, Object value) {
            this(List.of(), field, Binding.ReadForm.VALUE, op, value, (Class<?>) null);
        }
    }

    /** How a field is tested: {@code GELIJK} by {@code equals}, {@code NOT_EQUAL} by its negation, {@code AT_LEAST}
     *  as a whole number at least the value, {@code ONDER} as a whole number below the value, {@code BINNEN} as a date
     *  the validity window {@code waarde} admits, {@code IS_A} as an instance of the class {@code waarde},
     *  {@code LEEG} as null and {@code GEVULD} as not null (both without a {@code waarde}), {@code CLASS_NOT_IN} as an
     *  object whose own class, exactly, is none of the set of classes {@code waarde}, and {@code CLASS_IS} as an object
     *  whose own class, exactly, is the class {@code waarde}. */
    public enum Comparator {
        GELIJK, NOT_EQUAL, AT_LEAST, UNDER, WITHIN, IS_A, LEEG, GEVULD, CLASS_NOT_IN, CLASS_IS
    }
}
