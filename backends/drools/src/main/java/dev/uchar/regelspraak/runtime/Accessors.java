package dev.uchar.regelspraak.runtime;

import dev.uchar.regelspraak.transpiler.core.Namen;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Typed per-objecttype fact accessors — the executable-model back-end's "codegen" path (migration option B /
 * the deferred Phase-H optimization). Where the exec-model branch ({@code TmsTranspiler}) knows an objecttype's fact
 * CLASS at model-build time, it resolves a getter/setter to a TYPED {@code Function}/{@code BiConsumer} bound to the
 * bean's {@code get<Prop>()} / {@code set<Prop>(v)} via a per-owner {@link MethodHandles.Lookup} — a direct
 * MethodHandle invoke, no reflection, the same direct-dispatch a Kogito-generated executable model uses. The handle is
 * adapted to the interface with {@link MethodHandleProxies#asInterfaceInstance} (a hidden-class proxy on JDK 21+);
 * {@link java.lang.invoke.LambdaMetafactory} is deliberately NOT used because it demands a full-privilege
 * ({@code ORIGINAL}) lookup, a bar a cross-module {@code privateLookupIn} into a CHILD-classloader bean cannot clear —
 * with it the typed path would throw {@code NoClassDefFound} against generated beans (see {@code ownerLookup}). Two wins
 * over the reflective bridge ({@link RegelSpraakRuntime#get}/{@link RegelSpraakRuntime#set}):
 *
 * <ul>
 *   <li><b>fire-time speed</b> — {@code apply()}/{@code accept()} inline to a direct virtual call, no reflection;</li>
 *   <li><b>build-time fail-fast</b> — a missing getter throws when the transpiler RESOLVES the accessor (model
 *       build), not on the first fire of the rule.</li>
 * </ul>
 *
 * Sites whose runtime class is genuinely dynamic (navigation targets of unknown type, {@code Segment} /
 * {@code Inconsistentie} fields, multi-hop joins) keep the {@link RegelSpraakRuntime} cached-MethodHandle bridge,
 * which is itself reflection-free after the first resolve. Resolution rules (get-then-is for reads; the unique
 * single-arg {@code set<Prop>} for writes) mirror the bridge exactly, so the two paths return byte-identical
 * VALUES for every well-typed read/write the model produces (verified corpus-wide by {@code AccessorsTest}).
 *
 * <p>The only differences from the reflective bridge are on inputs the model never produces, and are
 * exception SHAPE, never a wrong value: a type-mismatched write or a null into a primitive setter throws
 * {@code ClassCastException}/{@code NullPointerException} here vs {@code IllegalArgumentException} via reflection
 * (both abort the fire identically); and a getter whose BODY throws propagates the real exception rather than
 * being masked. A missing getter on the declared class fails FAST at model-build (a clear bean/spec mismatch)
 * instead of at first fire. None is reachable by the value pipeline (kenmerks only ever get {@code TRUE}, the only
 * primitive setters are boolean kenmerks, and the fact-bean getters are pure field reads).
 *
 * <p>{@link #eigenschapGetter}, {@link #eigenschapSetter} and {@link #leesEigenschap} take the member's bean PROPERTY
 * and the names {@code Namen.lidnamen} gives it, the very names the fact-model generator writes, without deriving them
 * again: {@code pascal} of a property is not always its getter ("日 X" has the property {@code 日X}, which
 * {@code pascal} reads as a shouted word, {@code 日x}), and the Pascal form of a declared name is not always the
 * generator's getter either ({@code İş} where the generator writes {@code Iş}). Every reader and writer of a bean member
 * goes through this entry.
 */
public final class Accessors {
    private Accessors() {}

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    /** The caches of the property entry, per owner and property. */
    private static final Map<Class<?>, Map<String, Function<Object, Object>>> EIGENSCHAPGETTERS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Map<String, BiConsumer<Object, Object>>> EIGENSCHAPSETTERS =
            new ConcurrentHashMap<>();
    /** Per-owner lookups. A fact bean loaded in a CHILD classloader lives in a DIFFERENT (unnamed) module than this
     *  class, so the static parent-bound {@link #LOOKUP} cannot unreflect its methods; a {@code privateLookupIn}
     *  teleported into the owner can. Cached because {@code privateLookupIn} is a security-checked call and the same
     *  owner recurs across every one of its props. */
    private static final Map<Class<?>, MethodHandles.Lookup> LOOKUPS = new ConcurrentHashMap<>();

    /** A {@link MethodHandles.Lookup} that can unreflect {@code owner}'s methods wherever it lives (parent OR child
     *  classloader), or {@code null} when the owner's module forbids private access — the caller then keeps the
     *  reflective bridge. For a same-module (app-loaded) owner this is the full-privilege {@link #LOOKUP} itself; for
     *  a child-loaded owner it is a lookup teleported into the owner via {@link MethodHandles#privateLookupIn}. */
    private static MethodHandles.Lookup ownerLookup(Class<?> owner) {
        return LOOKUPS.computeIfAbsent(owner, o -> {
            try { return MethodHandles.privateLookupIn(o, LOOKUP); }
            catch (IllegalAccessException e) { return null; }   // module forbids private access -> reflective guard
        });
    }

    /** Adapt a per-owner unreflected accessor {@link MethodHandle} to a {@code Function}/{@code BiConsumer} via
     *  {@link MethodHandleProxies}. Unlike {@link java.lang.invoke.LambdaMetafactory}, which demands a full-privilege
     *  ({@code ORIGINAL}) lookup in the owner's module — a bar a cross-module {@code privateLookupIn} cannot clear, so
     *  LMF throws {@code LambdaConversionException} against a CHILD-loaded bean — {@code asInterfaceInstance} binds the
     *  handle directly and links wherever the owner lives. It stays reflection-free at call time (a direct MH invoke,
     *  no {@code Method.invoke}); the reflective {@link RegelSpraakRuntime} bridge remains the guard for the shapes
     *  below (no unique setter, forbidden module, subtype/null owner). */
    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> iface, MethodHandle mh, MethodType erased) {
        return MethodHandleProxies.asInterfaceInstance(iface, mh.asType(erased));
    }

    /** How many accessor builds fell back to the reflective bridge because LambdaMetafactory could not bind the
     *  signature. Stays 0 for ordinary POJO getters/setters — a regression in the typed path makes it climb. A
     *  test/diagnostics hook; never read by production code. */
    private static volatile int lmfFallbacks = 0;
    /** Diagnostics: count of accessor builds that fell off the typed LMF path onto the reflective bridge. */
    public static int lmfFallbacks() { return lmfFallbacks; }

    /** A null-safe typed getter for the member of {@code owner} whose bean property is {@code eigenschap}
     *  ({@code Namen.eigenschap} of its declared name): the {@code get} or else the {@code is} method that
     *  {@code Namen.lidnamen} names, as the fact-model generator wrote it, never derived again. Throws at THIS call when
     *  the bean has no such getter. An object that is not an {@code owner} is read through this entry on its own
     *  class. */
    public static Function<Object, Object> eigenschapGetter(Class<?> owner, String eigenschap) {
        return EIGENSCHAPGETTERS.computeIfAbsent(owner, c -> new ConcurrentHashMap<>())
                                .computeIfAbsent(eigenschap, e -> buildEigenschapGetter(owner, e));
    }

    /** A typed setter for the member of {@code owner} whose bean property is {@code eigenschap}: the one single-argument
     *  method that {@code Namen.lidnamen} names. Throws at THIS call where the bean has none, or more than one, since the
     *  generator writes exactly one per member. An object that is not an {@code owner} is written through this entry on
     *  its own class. */
    public static BiConsumer<Object, Object> eigenschapSetter(Class<?> owner, String eigenschap) {
        return EIGENSCHAPSETTERS.computeIfAbsent(owner, c -> new ConcurrentHashMap<>())
                                .computeIfAbsent(eigenschap, e -> buildEigenschapSetter(owner, e));
    }

    /** The member of {@code bean} whose bean property is {@code eigenschap}, through {@link #eigenschapGetter} of the
     *  bean's own class: a read whose class is known only when it runs, along a path or off an object of any class.
     *  Null where {@code bean} is null, a link that is absent. */
    public static Object leesEigenschap(Object bean, String eigenschap) {
        return bean == null ? null : eigenschapGetter(bean.getClass(), eigenschap).apply(bean);
    }

    private static Function<Object, Object> buildEigenschapGetter(Class<?> owner, String eigenschap) {
        Namen.Lidnamen n = Namen.lidnamen(eigenschap);
        Method m = getterMethod(owner, n.getter(), n.isGetter());
        if (m == null)
            throw new IllegalStateException("geen getter voor de eigenschap '" + eigenschap + "' op " + owner.getName()
                    + ": " + n.getter() + " noch " + n.isGetter());
        Function<Object, Object> raw = getypeerd(owner, m);
        // where no typed handle binds, the method itself by reflection; the bridge would derive the name again
        Function<Object, Object> lees = raw != null ? raw : o -> reflectief(m, o);
        return o -> o == null ? null : owner.isInstance(o) ? lees.apply(o)
                : eigenschapGetter(o.getClass(), eigenschap).apply(o);
    }

    /** The typed read through {@code m} of an object of {@code owner}, or null where it does not bind: the owner's
     *  module forbids private access, or the proxy cannot bind the signature. Each null counts in {@link #lmfFallbacks}. */
    private static Function<Object, Object> getypeerd(Class<?> owner, Method m) {
        // A Lookup that can unreflect on the owner wherever it lives (parent OR child loader). null -> the owner's
        // module forbids private access: keep the reflective bridge.
        MethodHandles.Lookup lk = ownerLookup(owner);
        if (lk == null) { lmfFallbacks++; return null; }
        MethodHandle mh;
        try { mh = lk.unreflect(m); }
        catch (IllegalAccessException e) { throw new IllegalStateException(e); }
        try {
            return proxy(Function.class, mh, MethodType.methodType(Object.class, Object.class));   // Function.apply erased: (Owner)Ret
        } catch (Throwable t) {
            // a signature the proxy cannot bind -> the reflective bridge (still correct, just untyped)
            lmfFallbacks++;
            return null;
        }
    }

    private static BiConsumer<Object, Object> buildEigenschapSetter(Class<?> owner, String eigenschap) {
        String naam = Namen.lidnamen(eigenschap).setter();
        Method m = uniqueSetterMethod(owner, naam);
        if (m == null)
            throw new IllegalStateException("niet precies één setter " + naam + " met één argument voor de eigenschap '"
                    + eigenschap + "' op " + owner.getName());
        BiConsumer<Object, Object> raw = getypeerdeSetter(owner, m, eigenschap);
        BiConsumer<Object, Object> zet = raw != null ? raw : (o, v) -> reflectief(m, o, v);
        return (o, v) -> {
            if (owner.isInstance(o)) zet.accept(o, v);
            else eigenschapSetter(o.getClass(), eigenschap).accept(o, v);
        };
    }

    /** The typed write through {@code m} to an object of {@code owner}, or null where it does not bind, as
     *  {@link #getypeerd}; {@code wat} names the member in a fault of the setter. */
    private static BiConsumer<Object, Object> getypeerdeSetter(Class<?> owner, Method m, String wat) {
        // Owner Lookup: both the direct-handle (primitive-setter) and the proxy (object-setter) branches must unreflect
        // on the owner's class, which fails cross-loader off the parent LOOKUP. null -> reflective bridge.
        MethodHandles.Lookup lk = ownerLookup(owner);
        if (lk == null) { lmfFallbacks++; return null; }
        MethodHandle mh;
        try { mh = lk.unreflect(m); }
        catch (IllegalAccessException e) { throw new IllegalStateException(e); }
        if (m.getParameterTypes()[0].isPrimitive()) {
            // A PRIMITIVE setter parameter (a boolean kenmerk's setX(boolean)): invoke the direct cached handle, which
            // unboxes at the call. Still typed + reflection-free (NOT an lmfFallback — a principled path, not a binding
            // regression). The owner Lookup lets this handle link against a CHILD-loaded bean too.
            final MethodHandle fmh = mh;
            return (o, v) -> {
                try { fmh.invoke(o, v); }
                catch (RuntimeException | Error e) { throw e; }
                catch (Throwable t) { throw new IllegalStateException("setter '" + wat + "' faalde op " + owner.getName(), t); }
            };
        }
        try {
            return proxy(BiConsumer.class, mh,
                    MethodType.methodType(void.class, Object.class, Object.class));  // BiConsumer.accept erased: (Owner,ParamT)void
        } catch (Throwable t) {
            lmfFallbacks++;
            return null;
        }
    }

    /** A call of {@code m} on {@code o} by reflection, with the fault of the method itself as it is. */
    private static Object reflectief(Method m, Object o, Object... argumenten) {
        try { return m.invoke(o, argumenten); }
        catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException r) throw r;
            if (e.getCause() instanceof Error f) throw f;
            throw new IllegalStateException(m.getName() + " faalde op " + o.getClass().getName(), e.getCause());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Method getterMethod(Class<?> owner, String get, String is) {
        for (String naam : new String[]{get, is}) {
            try { return owner.getMethod(naam); } catch (NoSuchMethodException ignore) { }
        }
        return null;
    }
    private static Method uniqueSetterMethod(Class<?> owner, String name) {
        Method only = null; int n = 0;
        for (Method m : owner.getMethods())
            if (m.getName().equals(name) && m.getParameterCount() == 1) { only = m; n++; }
        return n == 1 ? only : null;
    }
}
