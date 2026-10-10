package dev.uchar.regelspraak.transpiler.tms;

import java.util.List;
import java.util.Objects;

/**
 * What stands in a session of the truth-maintenance branch after a fire that reached its fixpoint, read without
 * writing anything on a bean ({@code DroolsBackend.lees}). The branch derives with logical facts and leaves the case
 * input alone on the beans, so a consumer that fires one session more than once reads the answer here between the
 * fires: a bean that carried a derived value would have it read as input by the next fire, and an initialisation of
 * that member would not fire again. {@link TmsTranspiler#zetAfgeleideWaarden} writes exactly this onto the beans, from
 * the same reading, once at the end of a session.
 *
 * <p>Every list is in no particular order. An object is a bean of the build's bean model, compared by identity; a member
 * is named as the model declares it.
 *
 * @param waarden         the derived value of every attribute a rule, a default or an allocation derived
 * @param kenmerken       every characteristic derived to hold; a characteristic the input sets is on the bean already,
 *                        and nothing derives a characteristic not to hold
 * @param objecten        every object a creation made
 * @param koppelingen     every link a creation laid, an object creation's link to its new object among them
 * @param fouten          the evaluation faults that stand, as data, in the order {@link TmsTranspiler#zetAfgeleideWaarden}
 *                        reports them: by the name of the form, then by the sentence
 * @param inconsistenties the §9.5 deposits, the beans of the generated class {@code Inconsistentie}, each with its rule
 *                        and its subject
 */
public record Fixpunt(List<AfgeleideWaarde> waarden, List<AfgeleidKenmerk> kenmerken, List<GemaaktObject> objecten,
                      List<GemaakteKoppeling> koppelingen, List<Fout> fouten, List<Object> inconsistenties) {

    public Fixpunt {
        waarden = List.copyOf(waarden);
        kenmerken = List.copyOf(kenmerken);
        objecten = List.copyOf(objecten);
        koppelingen = List.copyOf(koppelingen);
        fouten = List.copyOf(fouten);
        inconsistenties = List.copyOf(inconsistenties);
    }

    /**
     * The value derived for attribute {@code attribuut} of {@code object}, in the form its bean field takes it: a number
     * as a {@code BigDecimal}, a date, a text, a truth value, and a timeline as a list of the bean model's
     * {@code Segment}s, made anew by every read. Null is leeg: a rule outside the standing domain that computed leeg,
     * which empties the input (RUNTIME-SEMANTICS ch. 1.2g). An attribute without an entry keeps its input.
     */
    public record AfgeleideWaarde(Object object, String attribuut, Object waarde) {
        public AfgeleideWaarde {
            Objects.requireNonNull(object, "object");
            Objects.requireNonNull(attribuut, "attribuut");
        }
    }

    /** Characteristic {@code kenmerk} of {@code object} derived to hold. */
    public record AfgeleidKenmerk(Object object, String kenmerk) {
        public AfgeleidKenmerk {
            Objects.requireNonNull(object, "object");
            Objects.requireNonNull(kenmerk, "kenmerk");
        }
    }

    /** {@code object}, which the object creation named {@code creatie} made for {@code onderwerp}. Its attribute
     *  initialisations are on its bean already; what it derives besides is in {@link #waarden} and {@link #kenmerken}. */
    public record GemaaktObject(Object object, String creatie, Object onderwerp) {
        public GemaaktObject {
            Objects.requireNonNull(object, "object");
            Objects.requireNonNull(creatie, "creatie");
            Objects.requireNonNull(onderwerp, "onderwerp");
        }
    }

    /** A link of fact type {@code feittype} that a creation laid: {@code eerste} fills its first declared role,
     *  {@code eersteRol}, and {@code tweede} its second, {@code tweedeRol}. */
    public record GemaakteKoppeling(String feittype, String eersteRol, Object eerste, String tweedeRol, Object tweede) {
        public GemaakteKoppeling {
            Objects.requireNonNull(feittype, "feittype");
            Objects.requireNonNull(eersteRol, "eersteRol");
            Objects.requireNonNull(eerste, "eerste");
            Objects.requireNonNull(tweedeRol, "tweedeRol");
            Objects.requireNonNull(tweede, "tweede");
        }
    }
}
