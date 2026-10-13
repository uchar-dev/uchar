package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.CaseRunner;
import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.transpiler.tms.plan.Regelplan;

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
 * {@link PatternDslWeergave} and {@link ClosureWeergave} make Drools' executable model of it. A rendering into DRL text
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
 *       {@code Term.Dagsoort.predikaat}: the compiled predicate of a day kind, which the DRL text declares as a global
 *       (decision 6 of the card; step 5 decides its form). A record's fields are its components;</li>
 *   <li>the plans of every golden the branch builds hold only data, walked through every record component, list, map
 *       and array: no value is a lambda, a method reference, an anonymous or local class or an instance of a
 *       functional interface, apart from {@code Term.Dagsoort.predikaat}, and no value is of a kind that is not data
 *       (a record, a collection, a string, a number, a truth value, an enum constant, a class, a date or time).</li>
 * </ul>
 *
 * <p><b>Red</b> before step 1d, on {@code 86176419}: {@code TmsTranspiler} built the rules itself and named
 * {@code org.drools.model} in ten imports and one fully qualified name. The other two checks held there already; a
 * mutation (a record with a functional component, a lambda as the key of a read) turns each red. Until the fix round of
 * step 1d the scan read the sources line by line, and a field {@code static org.drools.} with {@code model.Index proef;}
 * on the next line passed it, though it compiles.
 */
class ScheidingWitnessTest {

    private static final Path TMS = Path.of("src", "main", "java", "dev", "uchar", "regelspraak", "transpiler", "tms");
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final String PLANPAKKET = "dev.uchar.regelspraak.transpiler.tms.plan.";
    /** The name of Drools' executable model API, searched in a source text without its whitespace: a fully qualified
     *  name compiles with a line break or spaces around its dots, so a scan per line misses one broken over two lines.
     *  Without the whitespace the words around the name join it, so it is searched without word boundaries; a longer
     *  name that begins with it ({@code org.drools.modelcompiler}) counts as well. */
    private static final String DROOLS_MODEL = "org.drools.model";
    /** The two renderings into the executable model. */
    private static final Set<String> WEERGAVEN = Set.of("PatternDslWeergave.java", "ClosureWeergave.java");
    /** The one functional component a plan carries: the predicate of a
     *  {@link dev.uchar.regelspraak.transpiler.tms.plan.Term.Dagsoort}. */
    private static final String DAGSOORT = "Term.Dagsoort.predikaat";

    /** Floors, measured 2026-10-13 at step 1d, so that a check over nothing does not pass: the source files of
     *  {@code transpiler/tms}, the classes of {@code plan} with their nested classes, and the goldens the branch builds
     *  with the plans they hold. A deliberate removal lowers a floor in the same commit. */
    private static final int EXPECTED_MIN_BESTANDEN = 66;
    private static final int EXPECTED_MIN_PLANKLASSEN = 100;
    private static final int EXPECTED_MIN_GEBOUWD = 253;
    private static final int EXPECTED_MIN_PLANNEN = 1859;

    @Test void alleen_de_weergaven_noemen_het_executable_model() throws Exception {
        Map<String, List<String>> buiten = new TreeMap<>();
        Map<String, Integer> inWeergaven = new TreeMap<>();
        List<Path> bestanden;
        try (Stream<Path> s = Files.walk(TMS)) {
            bestanden = s.filter(p -> p.getFileName().toString().endsWith(".java")).sorted().toList();
        }
        for (Path p : bestanden) {
            String naam = TMS.relativize(p).toString().replace(File.separatorChar, '/');
            List<String> treffers = treffers(Files.readString(p));
            if (WEERGAVEN.contains(naam)) inWeergaven.put(naam, treffers.size());
            else if (!treffers.isEmpty()) buiten.put(naam, treffers);
        }
        assertEquals(Map.of("TmsTranspiler.java", List.of("import org.drools.model.Model;")), buiten,
                "org.drools.model buiten de weergaven");
        assertEquals(WEERGAVEN, inWeergaven.keySet(), "de weergaven zelf");
        assertTrue(inWeergaven.get("PatternDslWeergave.java") > 0, () -> "de scan vindt de naam niet in de weergave: "
                + inWeergaven);
        assertTrue(bestanden.size() >= EXPECTED_MIN_BESTANDEN, () -> bestanden.size()
                + " bronbestanden, onder de vloer " + EXPECTED_MIN_BESTANDEN);
    }

    /** Every occurrence of {@link #DROOLS_MODEL} in {@code tekst} with its whitespace removed, each as the source lines
     *  it spans, stripped and joined by a space. */
    static List<String> treffers(String tekst) {
        StringBuilder zonder = new StringBuilder();
        List<Integer> plaats = new ArrayList<>();
        for (int i = 0; i < tekst.length(); i++) {
            if (Character.isWhitespace(tekst.charAt(i))) continue;
            zonder.append(tekst.charAt(i));
            plaats.add(i);
        }
        List<String> uit = new ArrayList<>();
        for (int k = zonder.indexOf(DROOLS_MODEL); k >= 0; k = zonder.indexOf(DROOLS_MODEL, k + 1)) {
            int van = tekst.lastIndexOf('\n', plaats.get(k)) + 1;
            int eind = tekst.indexOf('\n', plaats.get(k + DROOLS_MODEL.length() - 1));
            uit.add(String.join(" ", tekst.substring(van, eind < 0 ? tekst.length() : eind).strip().split("\\s*\n\\s*")));
        }
        return uit;
    }

    /** The scan itself: a name broken over two lines, which compiles, counts, with the lines it spans. */
    @Test void de_scan_vindt_een_over_twee_regels_afgebroken_naam() {
        assertEquals(List.of("static org.drools. model.Index proef;"),
                treffers("class X {\n    static org.drools.\n            model.Index proef;\n}\n"));
        assertEquals(List.of("import org . drools . model . Model;"), treffers("import org . drools . model . Model;"));
    }

    @Test void geen_planklasse_draagt_een_functioneel_type() throws Exception {
        List<String> functioneel = new ArrayList<>();
        List<Class<?>> klassen = planklassen();
        for (Class<?> c : klassen)
            for (Field f : c.getDeclaredFields())
                if (!Modifier.isStatic(f.getModifiers()) && !f.isSynthetic() && functioneel(f.getGenericType()))
                    functioneel.add(naam(c) + "." + f.getName() + ": " + f.getGenericType().getTypeName());
        assertEquals(List.of(DAGSOORT + ": java.util.function.Predicate<java.time.LocalDate>"), functioneel);
        assertTrue(klassen.size() >= EXPECTED_MIN_PLANKLASSEN, () -> klassen.size() + " planklassen, onder de vloer "
                + EXPECTED_MIN_PLANKLASSEN);
    }

    @Test void geen_plan_van_het_corpus_draagt_een_lambda_als_waarde() throws Exception {
        List<Path> goldens;
        try (Stream<Path> s = Files.walk(GOLDEN)) {
            goldens = s.filter(p -> p.getFileName().toString().equals("ir.json")).sorted().toList();
        }
        Wandeling w = new Wandeling();
        int gebouwd = 0;
        for (Path ir : goldens) {
            DroolsBackend.Build b;
            try {
                b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
            } catch (IrLoader.Refusal | IrLoader.Documentfout | CaseRunner.Excluded geweigerd) {
                // a document the branch does not build has no plan; the floor counts the ones it builds
                continue;
            }
            gebouwd++;
            w.loop(b.execModelTranspiler().modelplan(), "Modelplan");
        }
        assertEquals(Set.of(), w.geenData, "waarden die geen data zijn");
        assertEquals(Set.of(DAGSOORT), w.functioneel.keySet(), () -> "functionele waarden: " + w.functioneel);
        int n = gebouwd;
        assertTrue(n >= EXPECTED_MIN_GEBOUWD, () -> n + " gebouwde goldens, onder de vloer " + EXPECTED_MIN_GEBOUWD);
        assertTrue(w.plannen >= EXPECTED_MIN_PLANNEN, () -> w.plannen + " plannen, onder de vloer "
                + EXPECTED_MIN_PLANNEN);
    }

    /** A walk through the values of a plan: per place, the record component that holds a functional value, and the
     *  values that are not data. */
    private static final class Wandeling {
        final Map<String, Integer> functioneel = new TreeMap<>();
        final Set<String> geenData = new TreeSet<>();
        final Map<Object, Boolean> gezien = new IdentityHashMap<>();
        int plannen;

        void loop(Object o, String plaats) throws ReflectiveOperationException {
            if (o == null) return;
            Class<?> c = o.getClass();
            if (o instanceof Enum<?> e) {
                if (functioneleKlasse(e.getDeclaringClass())) functioneel.merge(plaats, 1, Integer::sum);
                return;
            }
            if (c.isHidden() || c.isSynthetic() || c.isAnonymousClass() || c.isLocalClass()
                    || (!blad(o) && functioneleKlasse(c))) {
                functioneel.merge(plaats, 1, Integer::sum);
                return;
            }
            if (blad(o) || gezien.put(o, Boolean.TRUE) != null) return;
            if (c.isRecord()) {
                if (o instanceof Regelplan) plannen++;
                for (RecordComponent rc : c.getRecordComponents())
                    loop(rc.getAccessor().invoke(o), naam(c) + "." + rc.getName());
            } else if (o instanceof Collection<?> k) {
                for (Object x : k) loop(x, plaats);
            } else if (o instanceof Map<?, ?> k) {
                for (Map.Entry<?, ?> x : k.entrySet()) {
                    loop(x.getKey(), plaats);
                    loop(x.getValue(), plaats);
                }
            } else if (o instanceof Object[] k) {
                for (Object x : k) loop(x, plaats);
            } else if (!c.isArray()) {
                // an array of a primitive type, such as the slots of a consequence, is data
                geenData.add(plaats + ": " + c.getName());
            }
        }

        /** A value that is data by its kind and holds nothing further. */
        private static boolean blad(Object o) {
            return o instanceof String || o instanceof Number || o instanceof Boolean || o instanceof Character
                    || o instanceof Class<?> || o instanceof Temporal || o instanceof TemporalAmount;
        }

        /** A class that implements a functional interface, by annotation or by package. */
        private static boolean functioneleKlasse(Class<?> c) {
            for (Class<?> k = c; k != null; k = k.getSuperclass())
                for (Class<?> i : k.getInterfaces())
                    if (gemarkeerdFunctioneel(i) || functioneleKlasse(i)) return true;
            return false;
        }
    }

    /** Every class of {@code transpiler/tms/plan}: the top-level class of each source file and its nested classes. */
    private static List<Class<?>> planklassen() throws Exception {
        List<Class<?>> uit = new ArrayList<>();
        try (Stream<Path> s = Files.list(TMS.resolve("plan"))) {
            for (Path p : s.filter(f -> f.getFileName().toString().endsWith(".java")).sorted().toList()) {
                String bestand = p.getFileName().toString();
                genest(Class.forName(PLANPAKKET + bestand.substring(0, bestand.length() - ".java".length())), uit);
            }
        }
        return uit;
    }

    private static void genest(Class<?> c, List<Class<?>> uit) {
        uit.add(c);
        for (Class<?> d : c.getDeclaredClasses()) genest(d, uit);
    }

    /** Whether {@code t} is or holds a functional type: an interface that is not sealed and that is marked functional
     *  ({@link #gemarkeerdFunctioneel}) or has exactly one abstract method beside those of {@link Object}. */
    private static boolean functioneel(Type t) {
        return switch (t) {
            case Class<?> c when c.isArray() -> functioneel(c.getComponentType());
            case Class<?> c ->
                    c.isInterface() && !c.isSealed() && (gemarkeerdFunctioneel(c) || abstracteMethoden(c) == 1);
            case ParameterizedType p -> {
                boolean f = functioneel(p.getRawType());
                for (Type a : p.getActualTypeArguments()) f |= functioneel(a);
                yield f;
            }
            case WildcardType w -> {
                boolean f = false;
                for (Type b : w.getUpperBounds()) f |= functioneel(b);
                for (Type b : w.getLowerBounds()) f |= functioneel(b);
                yield f;
            }
            case TypeVariable<?> v -> {
                boolean f = false;
                for (Type b : v.getBounds()) f |= functioneel(b);
                yield f;
            }
            case GenericArrayType a -> functioneel(a.getGenericComponentType());
            default -> throw new IllegalStateException("onbekende soort type: " + t);
        };
    }

    /** An interface marked functional: annotated so, or of {@code java.util.function}. */
    private static boolean gemarkeerdFunctioneel(Class<?> i) {
        return i.isAnnotationPresent(FunctionalInterface.class) || i.getPackageName().equals("java.util.function");
    }

    /** The abstract methods of interface {@code i}, those it inherits included, without the public methods of
     *  {@link Object} an interface may redeclare. */
    private static long abstracteMethoden(Class<?> i) {
        return Stream.of(i.getMethods()).filter(m -> Modifier.isAbstract(m.getModifiers()) && !vanObject(m)).count();
    }

    private static boolean vanObject(Method m) {
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
        return (n.startsWith(PLANPAKKET) ? n.substring(PLANPAKKET.length()) : n).replace('$', '.');
    }
}
