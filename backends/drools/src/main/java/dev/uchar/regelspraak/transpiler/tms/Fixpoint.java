package dev.uchar.regelspraak.transpiler.tms;

import java.util.List;
import java.util.Objects;

/**
 * What stands in a session of either branch, the exec-model branch or the DRL-text branch, after a fire that reached
 * its fixpoint, read without writing anything on a bean ({@code DroolsBackend.read}). Both branches derive with logical
 * facts and leave the case input alone on the beans, so a consumer that fires one session more than once reads the
 * answer here between the fires: a bean that carried a derived value would have it read as input by the next fire, and
 * an initialisation of that member would not fire again. {@link TmsTranspiler#setDerivedValues} writes exactly this
 * onto the beans, from the same reading, once at the end of a session.
 *
 * <p>Every list is in no particular order. The reader takes the facts in the order of the working memory and groups
 * the derived values per object by identity, so the order can differ from one run to the next, and a consumer that
 * shows a list sorts it itself. Only {@code faults} is sorted, as its description below says. An object is a bean of
 * the build's bean model, compared by identity; a member is named as the model declares it.
 *
 * @param waarden         the derived value of every attribute a rule, a default or an allocation derived
 * @param kenmerken       every characteristic derived to hold; a characteristic the input sets is on the bean already,
 *                        and nothing derives a characteristic not to hold
 * @param objecten        every object a creation made
 * @param koppelingen     every link a creation laid, an object creation's link to its new object among them
 * @param fouten          the evaluation faults that stand, as data, in the order {@link TmsTranspiler#setDerivedValues}
 *                        reports them: by the name of the form, then by the sentence
 * @param inconsistenties the §9.5 deposits, the beans of the generated class {@code Inconsistency}, each with its rule
 *                        and its subject
 */
public record Fixpoint(List<DerivedValue> values, List<DerivedCharacteristic> characteristics, List<MadeObject> objects,
                      List<MadeLink> links, List<Fault> faults, List<Object> inconsistencies) {

    public Fixpoint {
        values = List.copyOf(values);
        characteristics = List.copyOf(characteristics);
        objects = List.copyOf(objects);
        links = List.copyOf(links);
        faults = List.copyOf(faults);
        inconsistencies = List.copyOf(inconsistencies);
    }

    /**
     * The value derived for attribute {@code attribute} of {@code object}, in the form its bean field takes it: a number
     * as a {@code BigDecimal}, a date, a text, a truth value, and a timeline as a list of the bean model's
     * {@code Segment}s, made anew by every read. Null is leeg: a rule outside the standing domain that computed leeg,
     * which empties the input (RUNTIME-SEMANTICS ch. 1.2g). An attribute without an entry keeps its input.
     */
    public record DerivedValue(Object object, String attribute, Object value) {
        public DerivedValue {
            Objects.requireNonNull(object, "object");
            Objects.requireNonNull(attribute, "attribuut");
        }
    }

    /** Characteristic {@code characteristic} of {@code object} derived to hold. */
    public record DerivedCharacteristic(Object object, String characteristic) {
        public DerivedCharacteristic {
            Objects.requireNonNull(object, "object");
            Objects.requireNonNull(characteristic, "kenmerk");
        }
    }

    /** {@code object}, which the object creation named {@code creation} made for {@code subject}. Its attribute
     *  initialisations are on its bean already; what it derives besides is in {@link #values} and {@link #characteristics}. */
    public record MadeObject(Object object, String creation, Object subject) {
        public MadeObject {
            Objects.requireNonNull(object, "object");
            Objects.requireNonNull(creation, "creatie");
            Objects.requireNonNull(subject, "subject");
        }
    }

    /** A link of fact type {@code factType} that a creation laid: {@code first} fills its first declared role,
     *  {@code firstRole}, and {@code second} its second, {@code secondRole}. */
    public record MadeLink(String factType, String firstRole, Object first, String secondRole, Object second) {
        public MadeLink {
            Objects.requireNonNull(factType, "feittype");
            Objects.requireNonNull(firstRole, "eersteRol");
            Objects.requireNonNull(first, "eerste");
            Objects.requireNonNull(secondRole, "tweedeRol");
            Objects.requireNonNull(second, "tweede");
        }
    }
}
