package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.message.Msg;
import dev.uchar.regelspraak.runtime.Breuk;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * The evaluation fault of a value written into a target whose declared decimals do not hold it (RUNTIME-SEMANTICS
 * ch. 2.4 point 1, EF-006): the exact value times ten to the power of the decimals is no whole number. The value
 * counts, not the scale a decimal is written at, so 1.00 fits a whole-number attribute and 1001/2 does not. A form
 * that writes such a value raises this fault where it would write it ({@link ComputeHelpers#fitTarget}), and its
 * consequence inserts the {@link Fault} in place of the write, as for every evaluation fault of the branch.
 *
 * <p>One fault holds every value of one evaluation that does not fit ({@link Misfit}): one for a rule, a default or an
 * initialisation of an object creation, and for an allocation each share and the rest of one source that does not
 * fit, since the allocation writes all of them or none. The fault names one place, the first misfit in the order of
 * the input, after which created objects come by name ({@link #named}); its parameters are that place, the exact
 * value there and the declared decimals of its target (ch. 5.7b).
 *
 * <p>The place is the object and the attribute, the object by its case label. Where the fault is raised, the object is
 * not always known: a value term computes the value of the subject of its form, and the subject is the object of the
 * {@link Fault} the consequence inserts; an initialisation of an object creation computes the value of an object not
 * yet made, which is named after the creation and the subject, as a sentence names a made object. The write-back
 * names the fault ({@link TmsTranspiler#setDerivedValues}), with the subject of its fact.
 */
public final class WriteBoundaryFault extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * One value that does not fit: the object whose attribute it is, null for the subject of the form; the object
     * creation whose new object it is, null where it is none; the attribute; the exact value; the declared decimals of
     * the attribute.
     */
    public record Misfit(Object object, String creation, String attribute, Breuk value, int decimals) {}

    private final transient List<Misfit> misfits;
    private final String place;
    private final String rule;

    WriteBoundaryFault(List<Misfit> misfits) {
        this(List.copyOf(misfits), unnamedPlace(misfits.get(0)), null);
    }

    private WriteBoundaryFault(List<Misfit> misfits, String place, String rule) {
        super(rule == null ? sentence(place, misfits.get(0))
                : Msg.drools("FRAME.fault-in-rule", "rule", rule, "sentence", sentence(place, misfits.get(0))).text());
        this.misfits = misfits;
        this.place = place;
        this.rule = rule;
    }

    /** The misfit of {@code v} in a target of {@code decimals} declared decimals, null where it fits or is no number. */
    static Misfit misfit(Object object, String creation, String attribute, Object v, int decimals) {
        if (!(v instanceof java.math.BigDecimal || v instanceof Breuk)) return null;
        Breuk q = Breuk.van(v);
        Breuk scaled = q.vermenigvuldigen(Breuk.van(BigInteger.TEN.pow(decimals), BigInteger.ONE));
        return scaled.noemer().equals(BigInteger.ONE) ? null : new Misfit(object, creation, attribute, q, decimals);
    }

    /** The same fault named after the rule {@code rule} it ran in, as {@link ComputeFault#inRule} names one. */
    public WriteBoundaryFault inRule(String rule) {
        return new WriteBoundaryFault(misfits, place, rule);
    }

    /**
     * The same fault with its place named: an object that is null is {@code subject}, the subject of the fact of the
     * fault; an object is named by {@code label}, which gives the case label of an input object and null for one a
     * creation made, which keeps its name by type and identity; the misfits are ordered by {@code inputOrder}, the
     * place of an object among the beans of the input (created objects after them), and then by the name of the place;
     * the first names the fault. A null {@code label} names every object by type and identity.
     */
    public WriteBoundaryFault named(Object subject, Function<Object, String> label, ToIntFunction<Object> inputOrder) {
        Function<Object, String> name = o -> {
            String l = label == null ? null : label.apply(o);
            return l != null ? l : FactNames.object(o);
        };
        List<Misfit> resolved = new ArrayList<>();
        for (Misfit m : misfits)
            resolved.add(m.object() != null ? m : new Misfit(subject, m.creation(), m.attribute(), m.value(), m.decimals()));
        Function<Misfit, String> placeOf = m -> m.creation() == null ? name.apply(m.object()) + "." + m.attribute()
                : "«" + m.creation() + "»(" + name.apply(m.object()) + ")." + m.attribute();
        resolved.sort(Comparator.comparingInt((Misfit m) -> m.creation() == null ? inputOrder.applyAsInt(m.object())
                : Integer.MAX_VALUE).thenComparing(placeOf));
        return new WriteBoundaryFault(resolved, placeOf.apply(resolved.get(0)), rule);
    }

    /** The place of the first misfit before the write-back named it: the attribute, of the new object of its creation
     *  where it is one. The object is named only by the write-back, which knows its case label: the fault is a fact of
     *  the session, and a sentence of a fact names no identity, which differs from one session to the next. */
    private static String unnamedPlace(Misfit m) {
        return m.creation() == null ? m.attribute() : "«" + m.creation() + "»." + m.attribute();
    }

    private static String sentence(String place, Misfit m) {
        return Msg.drools("EF-006", "place", place, "value", text(m.value()), "decimals", m.decimals()).text();
    }

    /** {@code q} as the reference interpreter writes an exact value, its numerator over its denominator, the numerator
     *  alone for a whole number. */
    static String text(Breuk q) {
        return q.noemer().equals(BigInteger.ONE) ? q.teller().toString() : q.teller() + "/" + q.noemer();
    }

    /** The sentence of the fault, without the rule. */
    public String sentence() {
        return sentence(place, misfits.get(0));
    }

    /** The rule a named fault ran in; null where nothing named it yet. */
    public String rule() {
        return rule;
    }

    /** The values that do not fit, the first the one the fault names. */
    public List<Misfit> misfits() {
        return misfits;
    }

    /** The fault kind (RUNTIME-SEMANTICS ch. 5.7b). */
    public String code() {
        return "EF-006";
    }

    /** The parameters of the kind (ch. 5.7b): the place the fault names, the exact value there and the declared
     *  decimals of its target. */
    public Map<String, String> params() {
        Map<String, String> p = new LinkedHashMap<>();
        Misfit m = misfits.get(0);
        p.put("place", place);
        p.put("value", text(m.value()));
        p.put("decimals", Integer.toString(m.decimals()));
        return p;
    }
}
