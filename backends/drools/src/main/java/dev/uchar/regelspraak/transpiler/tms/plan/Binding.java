package dev.uchar.regelspraak.transpiler.tms.plan;

import java.util.List;

/**
 * A value a pattern binds to the variable {@code doel}: from its object along the reference fields of {@code pad}, null
 * where a link on it is absent, the field {@code veld}, read as {@code vorm}. On a fact of the branch the path is empty
 * and the field is a component of the fact; on a bean every step is a reference field and the field a member.
 */
public record Binding(String doel, List<Veld> pad, Veld veld, Leesvorm vorm, Class<?> soort) {

    public Binding {
        pad = List.copyOf(pad);
        if (soort != null && vorm != Leesvorm.GEZET)
            throw new IllegalArgumentException("alleen een gezet veld toetst een klasse");
    }

    /** The field {@code veld} of an object of {@code klasse}, as it is. */
    public static Binding van(String doel, Class<?> klasse, String veld) {
        return new Binding(doel, List.of(), new Veld(klasse, veld), Leesvorm.WAARDE, null);
    }

    /** A property {@code naam} of the objects of {@code klasse}. */
    public record Veld(Class<?> klasse, String naam) {}

    /**
     * How the field is read: {@code WAARDE} as it is; {@code WAAR} as whether it holds {@code true}, a characteristic
     * that holds one truth value; {@code TIJDVAKKEN} as the true periods of a characteristic on a timeline, a list of
     * segments; {@code GEZET} as whether a reference field is set, to an instance of {@code soort} where that is not
     * null.
     */
    public enum Leesvorm { WAARDE, WAAR, TIJDVAKKEN, GEZET }
}
