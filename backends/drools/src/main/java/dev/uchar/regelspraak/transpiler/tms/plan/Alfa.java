package dev.uchar.regelspraak.transpiler.tms.plan;

import dev.uchar.regelspraak.transpiler.kern.Namen;

import java.util.List;

/**
 * One constraint of a pattern on its own object: the conjunction of {@code toetsen}, in order, each on one field. It is
 * one constraint and not one per field, with one {@code id}: Drools' executable model shares a constraint between rules
 * by its id, so the analysis gives every constraint an id of its own, and a rendering into that model keeps it.
 */
public record Alfa(String id, List<Veldtoets> toetsen) {

    public Alfa {
        toetsen = List.copyOf(toetsen);
        if (toetsen.isEmpty()) throw new IllegalArgumentException("een alfaconstraint toetst ten minste één veld");
    }

    /**
     * A test against {@code waarde} of what the test reads of the pattern's object: its field {@code veld}, or with
     * {@code veld} null the object itself, or along the reference fields of {@code pad} the object the path reaches,
     * null where a link on it is absent; read as {@code vorm} ({@link Binding.Leesvorm}), as the analysis decided,
     * among them a characteristic on a bean as whether it holds {@code true}. A path ends in the object it reaches: the
     * class of a field beyond it is not the pattern's. A field on a bean is read through the getter of the class
     * {@code declarant} where that is not null, the class that declares the member, which an object the test sees may
     * extend while the pattern's class does not declare it; else through the getter of the pattern's class.
     * {@code eigenschap} is the property a rendering reads of the field, null with {@code veld}: on a bean the bean
     * property of the member ({@link Namen#eigenschap}), on a fact of the branch the component itself.
     */
    public record Veldtoets(List<Binding.Veld> pad, String veld, String eigenschap, Binding.Leesvorm vorm,
                            Vergelijker op, Object waarde, Class<?> declarant) {
        public Veldtoets {
            pad = List.copyOf(pad);
            if ((veld == null) != (eigenschap == null))
                throw new IllegalArgumentException("een veldtoets leest een eigenschap precies waar zij een veld leest: "
                        + veld + ", " + eigenschap);
            if (!pad.isEmpty() && veld != null)
                throw new IllegalArgumentException("een veldtoets langs een pad toetst het object dat het pad bereikt,"
                        + " niet het veld " + veld);
            if (declarant != null && veld == null)
                throw new IllegalArgumentException("een veldtoets op het object zelf leest geen veld van " + declarant);
        }

        /** A test of what the test reads of the pattern's object, a field through the getter of {@code declarant}, with
         *  the property the field has. */
        public Veldtoets(List<Binding.Veld> pad, String veld, Binding.Leesvorm vorm, Vergelijker op, Object waarde,
                         Class<?> declarant) {
            this(pad, veld, veld == null ? null : Namen.eigenschap(veld), vorm, op, waarde, declarant);
        }

        /** A test of what the test reads of the pattern's object, a field through the getter of the pattern's class. */
        public Veldtoets(List<Binding.Veld> pad, String veld, Binding.Leesvorm vorm, Vergelijker op, Object waarde) {
            this(pad, veld, vorm, op, waarde, null);
        }

        /** A test of field {@code veld} of the pattern's own object as it is; a null {@code veld} tests the object
         *  itself. */
        public Veldtoets(String veld, Vergelijker op, Object waarde) {
            this(List.of(), veld, Binding.Leesvorm.WAARDE, op, waarde, (Class<?>) null);
        }
    }

    /** How a field is tested: {@code GELIJK} by {@code equals}, {@code NIET_GELIJK} by its negation, {@code MINSTENS}
     *  as a whole number at least the value, {@code ONDER} as a whole number below the value, {@code BINNEN} as a date
     *  the validity window {@code waarde} admits, {@code IS_EEN} as an instance of the class {@code waarde},
     *  {@code LEEG} as null and {@code GEVULD} as not null (both without a {@code waarde}), {@code KLASSE_NIET_IN} as an
     *  object whose own class, exactly, is none of the set of classes {@code waarde}, and {@code KLASSE_IS} as an object
     *  whose own class, exactly, is the class {@code waarde}. */
    public enum Vergelijker {
        GELIJK, NIET_GELIJK, MINSTENS, ONDER, BINNEN, IS_EEN, LEEG, GEVULD, KLASSE_NIET_IN, KLASSE_IS
    }
}
