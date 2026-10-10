package dev.uchar.regelspraak.transpiler.tms;

import java.util.List;
import java.util.Objects;

/**
 * The value a {@link Stand} carries for an attribute on a timeline whose case input is the asOfDate-bucket scalar
 * ({@link Emmerinvoer}, RUNTIME-SEMANTICS ch. 8.4): the period of the attribute's grid that holds the evaluation date,
 * with the scalar as its value. It is not a timeline: it states no period of its own beyond that one, so it serves the
 * subject's own total without a clip, where the session's date is the horizon (ch. 3.6), and every form that reads the
 * full timeline faults on it ({@link Invoervormfout}). Compared by the period, as {@link Tijdvak} is.
 */
public record Emmerscalair(Tijdvak emmer) {

    public Emmerscalair {
        Objects.requireNonNull(emmer, "emmer");
    }

    /** The bucket scalar the bean of an {@link Emmerinvoer} carries as {@code segmenten}: the case input lays it there
     *  as the one segment of the evaluation date's period. Anything else breaks that contract. */
    static Emmerscalair van(List<?> segmenten) {
        List<Tijdvak> tijdlijn = Tijdvak.lijst(segmenten);
        if (tijdlijn == null || tijdlijn.size() != 1)
            throw new IllegalStateException("een emmerscalair staat op de bean als het ene segment van de periode van de"
                    + " rekendatum, niet als " + tijdlijn);
        return new Emmerscalair(tijdlijn.get(0));
    }

    /** The bucket as a timeline of one segment, which the subject's own total folds (ch. 3.6). */
    List<Tijdvak> tijdlijn() {
        return List.of(emmer);
    }

    @Override public String toString() {
        return "emmerscalair " + emmer;
    }
}
