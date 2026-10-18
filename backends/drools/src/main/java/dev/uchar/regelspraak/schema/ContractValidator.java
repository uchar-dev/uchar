package dev.uchar.regelspraak.schema;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;
import dev.uchar.regelspraak.schema.FactModelGenerator.Member;
import dev.uchar.regelspraak.transpiler.kern.Namen;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Checks that the fact beans on a classpath honour the contract a resolved model implies:
 * per objecttype a class in {@code dev.uchar.regelspraak.facts}, per implied member (the SAME derivation
 * {@link FactModelGenerator#members} the generator renders) a getter and setter of the right shape, plus the
 * {@code Segment}/{@code Inconsistentie} carriers when the model needs them. Returns ALL violations — the
 * engine fails fast with the complete list, so a consumer fixes the bean contract in one round trip instead
 * of chasing one reflective fire-time abort per run.
 */
public final class ContractValidator {
    private ContractValidator() {}

    /** Every bean↔model violation, as human-readable messages; empty when the contract holds. */
    public static List<String> valideer(RegelSpraakModel model, ClassLoader cl) {
        List<String> out = new ArrayList<>();
        for (Objecttype ot : model.objecttypen.values()) {
            String javaClass = FactModelGenerator.PAKKET + "." + Namen.ident(ot.naam, true);
            Class<?> c;
            try { c = Class.forName(javaClass, false, cl); }
            catch (ClassNotFoundException e) {
                out.add("objecttype '" + ot.naam + "': feitklasse " + javaClass + " ontbreekt op het classpath");
                continue;
            }
            for (Member l : FactModelGenerator.members(model, ot)) {
                // the names the generator writes the member with, never derived here a second time
                String p = l.property();
                Namen.Lidnamen n = Namen.lidnamen(p);
                String getterName = l.isForm() ? n.isGetter() : n.getter();
                Method getter = find(c, getterName, 0);
                // a scalar kenmerk may also be exposed as get<X>() (mvel resolves the property either way)
                if (getter == null && l.isForm()) getter = find(c, n.getter(), 0);
                if (getter == null)
                    out.add(ot.naam + "." + p + ": getter " + getterName + "() ontbreekt op " + javaClass);
                else if (!typeOk(l.javaType(), getter.getReturnType()))
                    out.add(ot.naam + "." + p + ": getter " + getter.getName() + "() geeft " + getter.getReturnType().getName()
                            + ", het model vraagt " + l.javaType());
                Method setter = find(c, n.setter(), 1);
                if (setter == null)
                    out.add(ot.naam + "." + p + ": setter " + n.setter() + "(" + l.javaType() + ") ontbreekt op " + javaClass);
                else if (!typeOk(l.javaType(), setter.getParameterTypes()[0]))
                    out.add(ot.naam + "." + p + ": setter " + n.setter() + " neemt " + setter.getParameterTypes()[0].getName()
                            + ", het model vraagt " + l.javaType());
            }
        }
        if (!model.objecttypen.isEmpty()) {
            // the carriers: Segment iff any temporal member exists, Inconsistentie iff any §13.4.8/§6.5/§6.6 fout
            // can be raised (consistenties, uniciteiten, or any division/wortel — checked coarsely: consistenties
            // and uniciteiten only; the fout companions of a division/wortel insert one too, but requiring the
            // class only when statically certain keeps the validator permissive for scalar-only models).
            // The OWN maps, deliberately: this is a disjunction over EVERY objecttype in the model, so a
            // declaring ancestor is visited on its own turn and chain-closing the read would only make
            // each inherited timeline count once per descendant — the same answer, later.
            boolean temporal = model.objecttypen.values().stream().anyMatch(ot ->
                    !ot.attribuutGranulariteit.isEmpty() || !ot.kenmerkGranulariteit.isEmpty());
            if (temporal) requireSegment(cl, out);
            if (!model.consistenties.isEmpty() || !model.uniciteiten.isEmpty() || !model.uniciteitUnies.isEmpty())
                requireInconsistency(cl, out);
        }
        return out;
    }

    private static void requireSegment(ClassLoader cl, List<String> out) {
        try {
            Class<?> seg = Class.forName(FactModelGenerator.PAKKET + ".Segment", false, cl);
            seg.getConstructor(java.time.LocalDate.class, java.time.LocalDate.class, java.math.BigDecimal.class);
            for (String m : new String[]{"getVan", "getTot", "getWaarde"})
                if (find(seg, m, 0) == null) out.add("Segment." + m + "() ontbreekt (temporeel model)");
        } catch (ClassNotFoundException e) {
            out.add("temporeel model zonder " + FactModelGenerator.PAKKET + ".Segment op het classpath");
        } catch (NoSuchMethodException e) {
            out.add("Segment(LocalDate, LocalDate, BigDecimal) constructor ontbreekt (temporeel model)");
        }
    }

    private static void requireInconsistency(ClassLoader cl, List<String> out) {
        try {
            Class<?> inc = Class.forName(FactModelGenerator.PAKKET + ".Inconsistentie", false, cl);
            inc.getConstructor(String.class, Object.class);
        } catch (ClassNotFoundException e) {
            out.add("consistentie-/uniciteitsregels zonder " + FactModelGenerator.PAKKET + ".Inconsistentie op het classpath");
        } catch (NoSuchMethodException e) {
            out.add("Inconsistentie(String, Object) constructor ontbreekt (consistentie-/uniciteitsregels)");
        }
    }

    private static Method find(Class<?> c, String naam, int arity) {
        for (Method m : c.getMethods())
            if (m.getName().equals(naam) && m.getParameterCount() == arity) return m;
        return null;
    }

    /** Does the actual class satisfy the expected SOURCE type name? Erasure-level: a generic
     *  {@code Map<String,BigDecimal>}/{@code List<Segment>} checks its raw type; {@code boolean} admits both
     *  the primitive and the wrapper; a reference field checks the simple fact-class name. */
    private static boolean typeOk(String verwacht, Class<?> real) {
        if (verwacht.startsWith("java.util.Map")) return java.util.Map.class.isAssignableFrom(real);
        if (verwacht.startsWith("java.util.List")) return java.util.List.class.isAssignableFrom(real);
        return switch (verwacht) {
            // a Booleaan ATTRIBUTE is three-valued (waar/onwaar/LEEG): the bean member must be the nullable
            // wrapper — a primitive boolean silently coerces leeg, exactly the drift this validator exists
            // to catch. A scalar KENMERK ("boolean") is genuinely two-valued, so either form serves it.
            case "Boolean" -> real == Boolean.class;
            case "boolean" -> real == boolean.class || real == Boolean.class;
            case "String" -> real == String.class;
            case "Object" -> true;   // an Onbekend datatype claims nothing
            default -> real.getName().equals(verwacht)                       // fully-qualified scalar
                    || real.getSimpleName().equals(verwacht);                // a fact-class reference by simple name
        };
    }
}
