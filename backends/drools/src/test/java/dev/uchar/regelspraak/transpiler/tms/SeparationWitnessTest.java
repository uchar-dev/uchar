package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.CaseRunner;
import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.transpiler.tms.plan.RulePlan;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The analysis of the exec-model branch and its renderings stay apart (card drl-teksttak-op-truth-maintenance, step
 * 1d). {@code TmsTranspiler} decides and delivers its outcome as data ({@code Modelplan}); only the renderings
 * {@link PatternDslRendering} and {@link ClosureRendering} make Drools' executable model of it. A rendering into DRL text
 * takes the same outcome, so the outcome may hold nothing that only the executable model can take.
 *
 * <p>Three checks, each over what is there rather than over a list of known cases:
 * <ul>
 *   <li>the main sources of {@code transpiler/tms}, read as text, name {@code org.drools.model} only in the two
 *       renderings, and {@code TmsTranspiler} only in its import of {@code Model}, the type of its public
 *       {@code model()}. Every occurrence counts: an import, a fully qualified name in the code or in a comment, also
 *       one broken over two lines, since the scan searches the text without its whitespace;</li>
 *   <li>no class of {@code transpiler/tms/plan} declares an instance field of a functional type (an interface of
 *       {@code java.util.function}, one annotated {@code @FunctionalInterface}, or any other interface that is not
 *       sealed and has one abstract method, through the type arguments as well), apart from
 *       {@code Term.DayKind.datePredicate}: the compiled predicate of a day kind, which the DRL text declares as a global
 *       (decision 6 of the card; step 5 decides its form). A record's fields are its components;</li>
 *   <li>the plans of every golden the branch builds hold only data, walked through every record component, list, map
 *       and array: no value is a lambda, a method reference, an anonymous or local class or an instance of a
 *       functional interface, apart from {@code Term.DayKind.datePredicate}, and no value is of a kind that is not data
 *       (a record, a collection, a string, a number, a truth value, an enum constant, a class, a date or time).</li>
 * </ul>
 *
 * <p><b>Red</b> before step 1d, on {@code 86176419}: {@code TmsTranspiler} built the rules itself and named
 * {@code org.drools.model} in ten imports and one fully qualified name. The other two checks held there already; a
 * mutation (a record with a functional component, a lambda as the key of a read) turns each red. Until the fix round of
 * step 1d the scan read the sources line by line, and a field {@code static org.drools.} with {@code model.Index proef;}
 * on the next line passed it, though it compiles.
 */
class SeparationWitnessTest {

    private static final Path TMS = Path.of("src", "main", "java", "dev", "uchar", "regelspraak", "transpiler", "tms");
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final String PLAN_PACKAGE = "dev.uchar.regelspraak.transpiler.tms.plan.";
    /** The name of Drools' executable model API, searched in a source text without its whitespace: a fully qualified
     *  name compiles with a line break or spaces around its dots, so a scan per line misses one broken over two lines.
     *  Without the whitespace the words around the name join it, so it is searched without word boundaries; a longer
     *  name that begins with it ({@code org.drools.modelcompiler}) counts as well. */
    private static final String DROOLS_MODEL = "org.drools.model";
    /** The two renderings into the executable model. */
    private static final Set<String> RENDERINGS = Set.of("PatternDslRendering.java", "ClosureRendering.java");
    /** The one functional component a plan carries: the predicate of a
     *  {@link dev.uchar.regelspraak.transpiler.tms.plan.Term.DayKind}. */
    private static final String DAGSOORT = "Term.DayKind.datePredicate";

    /** Floors, measured 2026-10-13 at step 1d, so that a check over nothing does not pass: the source files of
     *  {@code transpiler/tms}, the classes of {@code plan} with their nested classes, and the goldens the branch builds
     *  with the plans they hold. A deliberate removal lowers a floor in the same commit. */
    private static final int EXPECTED_MIN_FILES = 66;
    private static final int EXPECTED_MIN_PLAN_CLASSES = 100;
    private static final int EXPECTED_MIN_BUILT = 253;
    private static final int EXPECTED_MIN_PLANS = 1859;

    @Test void only_the_renderings_name_the_executable_model() throws Exception {
        Map<String, List<String>> outside = new TreeMap<>();
        Map<String, Integer> inRenderings = new TreeMap<>();
        List<Path> files;
        try (Stream<Path> s = Files.walk(TMS)) {
            files = s.filter(p -> p.getFileName().toString().endsWith(".java")).sorted().toList();
        }
        for (Path p : files) {
            String naam = TMS.relativize(p).toString().replace(File.separatorChar, '/');
            List<String> hits = hits(Files.readString(p));
            if (RENDERINGS.contains(naam)) inRenderings.put(naam, hits.size());
            else if (!hits.isEmpty()) outside.put(naam, hits);
        }
        assertEquals(Map.of("TmsTranspiler.java", List.of("import org.drools.model.Model;")), outside,
                "org.drools.model buiten de weergaven");
        assertEquals(RENDERINGS, inRenderings.keySet(), "de weergaven zelf");
        assertTrue(inRenderings.get("PatternDslRendering.java") > 0, () -> "de scan vindt de naam niet in de weergave: "
                + inRenderings);
        assertTrue(files.size() >= EXPECTED_MIN_FILES, () -> files.size()
                + " bronbestanden, onder de vloer " + EXPECTED_MIN_FILES);
    }

    /** Every occurrence of {@link #DROOLS_MODEL} in {@code text} with its whitespace removed, each as the source lines
     *  it spans, stripped and joined by a space. */
    static List<String> hits(String text) {
        StringBuilder without = new StringBuilder();
        List<Integer> place = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            if (Character.isWhitespace(text.charAt(i))) continue;
            without.append(text.charAt(i));
            place.add(i);
        }
        List<String> out = new ArrayList<>();
        for (int k = without.indexOf(DROOLS_MODEL); k >= 0; k = without.indexOf(DROOLS_MODEL, k + 1)) {
            int van = text.lastIndexOf('\n', place.get(k)) + 1;
            int end = text.indexOf('\n', place.get(k + DROOLS_MODEL.length() - 1));
            out.add(String.join(" ", text.substring(van, end < 0 ? text.length() : end).strip().split("\\s*\n\\s*")));
        }
        return out;
    }

    /** The scan itself: a name broken over two lines, which compiles, counts, with the lines it spans. */
    @Test void the_scan_finds_a_name_broken_over_two_lines() {
        assertEquals(List.of("static org.drools. model.Index proef;"),
                hits("class X {\n    static org.drools.\n            model.Index proef;\n}\n"));
        assertEquals(List.of("import org . drools . model . Model;"), hits("import org . drools . model . Model;"));
    }

    @Test void no_plan_class_carries_a_functional_type() throws Exception {
        List<String> functional = new ArrayList<>();
        List<Class<?>> classes = planClasses();
        for (Class<?> c : classes)
            for (Field f : c.getDeclaredFields())
                if (!Modifier.isStatic(f.getModifiers()) && !f.isSynthetic() && functional(f.getGenericType()))
                    functional.add(naam(c) + "." + f.getName() + ": " + f.getGenericType().getTypeName());
        assertEquals(List.of(DAGSOORT + ": java.util.function.Predicate<java.time.LocalDate>"), functional);
        assertTrue(classes.size() >= EXPECTED_MIN_PLAN_CLASSES, () -> classes.size() + " planklassen, onder de vloer "
                + EXPECTED_MIN_PLAN_CLASSES);
    }

    @Test void no_plan_of_the_corpus_carries_a_lambda_as_value() throws Exception {
        List<Path> goldens;
        try (Stream<Path> s = Files.walk(GOLDEN)) {
            goldens = s.filter(p -> p.getFileName().toString().equals("ir.json")).sorted().toList();
        }
        Walk w = new Walk();
        int built = 0;
        for (Path ir : goldens) {
            DroolsBackend.Build b;
            try {
                b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
            } catch (IrLoader.Refusal | IrLoader.DocumentError | CaseRunner.Excluded refused) {
                // a document the branch does not build has no plan; the floor counts the ones it builds
                continue;
            }
            built++;
            w.loop(b.execModelTranspiler().modelplan(), "Modelplan");
        }
        assertEquals(Set.of(), w.noData, "waarden die geen data zijn");
        assertEquals(Set.of(DAGSOORT), w.functional.keySet(), () -> "functionele waarden: " + w.functional);
        int n = built;
        assertTrue(n >= EXPECTED_MIN_BUILT, () -> n + " gebouwde goldens, onder de vloer " + EXPECTED_MIN_BUILT);
        assertTrue(w.plans >= EXPECTED_MIN_PLANS, () -> w.plans + " plannen, onder de vloer "
                + EXPECTED_MIN_PLANS);
    }

    /** A walk through the values of a plan: per place, the record component that holds a functional value, and the
     *  values that are not data. */
    private static final class Walk {
        final Map<String, Integer> functional = new TreeMap<>();
        final Set<String> noData = new TreeSet<>();
        final Map<Object, Boolean> seen = new IdentityHashMap<>();
        int plans;

        void loop(Object o, String place) throws ReflectiveOperationException {
            if (o == null) return;
            Class<?> c = o.getClass();
            if (o instanceof Enum<?> e) {
                if (functionalClass(e.getDeclaringClass())) functional.merge(place, 1, Integer::sum);
                return;
            }
            if (c.isHidden() || c.isSynthetic() || c.isAnonymousClass() || c.isLocalClass()
                    || (!leaf(o) && functionalClass(c))) {
                functional.merge(place, 1, Integer::sum);
                return;
            }
            if (leaf(o) || seen.put(o, Boolean.TRUE) != null) return;
            if (c.isRecord()) {
                if (o instanceof RulePlan) plans++;
                for (RecordComponent rc : c.getRecordComponents())
                    loop(rc.getAccessor().invoke(o), naam(c) + "." + rc.getName());
            } else if (o instanceof Collection<?> k) {
                for (Object x : k) loop(x, place);
            } else if (o instanceof Map<?, ?> k) {
                for (Map.Entry<?, ?> x : k.entrySet()) {
                    loop(x.getKey(), place);
                    loop(x.getValue(), place);
                }
            } else if (o instanceof Object[] k) {
                for (Object x : k) loop(x, place);
            } else if (!c.isArray()) {
                // an array of a primitive type, such as the slots of a consequence, is data
                noData.add(place + ": " + c.getName());
            }
        }

        /** A value that is data by its kind and holds nothing further. */
        private static boolean leaf(Object o) {
            return o instanceof String || o instanceof Number || o instanceof Boolean || o instanceof Character
                    || o instanceof Class<?> || o instanceof Temporal || o instanceof TemporalAmount;
        }

        /** A class that implements a functional interface, by annotation or by package. */
        private static boolean functionalClass(Class<?> c) {
            for (Class<?> k = c; k != null; k = k.getSuperclass())
                for (Class<?> i : k.getInterfaces())
                    if (markedFunctional(i) || functionalClass(i)) return true;
            return false;
        }
    }

    /** Every class of {@code transpiler/tms/plan}: the top-level class of each source file and its nested classes. */
    private static List<Class<?>> planClasses() throws Exception {
        List<Class<?>> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(TMS.resolve("plan"))) {
            for (Path p : s.filter(f -> f.getFileName().toString().endsWith(".java")).sorted().toList()) {
                String file = p.getFileName().toString();
                nested(Class.forName(PLAN_PACKAGE + file.substring(0, file.length() - ".java".length())), out);
            }
        }
        return out;
    }

    private static void nested(Class<?> c, List<Class<?>> out) {
        out.add(c);
        for (Class<?> d : c.getDeclaredClasses()) nested(d, out);
    }

    /** Whether {@code t} is or holds a functional type: an interface that is not sealed and that is marked functional
     *  ({@link #markedFunctional}) or has exactly one abstract method beside those of {@link Object}. */
    private static boolean functional(Type t) {
        return switch (t) {
            case Class<?> c when c.isArray() -> functional(c.getComponentType());
            case Class<?> c ->
                    c.isInterface() && !c.isSealed() && (markedFunctional(c) || abstractMethods(c) == 1);
            case ParameterizedType p -> {
                boolean f = functional(p.getRawType());
                for (Type a : p.getActualTypeArguments()) f |= functional(a);
                yield f;
            }
            case WildcardType w -> {
                boolean f = false;
                for (Type b : w.getUpperBounds()) f |= functional(b);
                for (Type b : w.getLowerBounds()) f |= functional(b);
                yield f;
            }
            case TypeVariable<?> v -> {
                boolean f = false;
                for (Type b : v.getBounds()) f |= functional(b);
                yield f;
            }
            case GenericArrayType a -> functional(a.getGenericComponentType());
            default -> throw new IllegalStateException("onbekende soort type: " + t);
        };
    }

    /** An interface marked functional: annotated so, or of {@code java.util.function}. */
    private static boolean markedFunctional(Class<?> i) {
        return i.isAnnotationPresent(FunctionalInterface.class) || i.getPackageName().equals("java.util.function");
    }

    /** The abstract methods of interface {@code i}, those it inherits included, without the public methods of
     *  {@link Object} an interface may redeclare. */
    private static long abstractMethods(Class<?> i) {
        return Stream.of(i.getMethods()).filter(m -> Modifier.isAbstract(m.getModifiers()) && !fromObject(m)).count();
    }

    private static boolean fromObject(Method m) {
        try {
            Object.class.getMethod(m.getName(), m.getParameterTypes());
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /** The name of a class of the plan relative to its package, nested classes after a dot. */
    private static String naam(Class<?> c) {
        String n = c.getName();
        return (n.startsWith(PLAN_PACKAGE) ? n.substring(PLAN_PACKAGE.length()) : n).replace('$', '.');
    }
}
