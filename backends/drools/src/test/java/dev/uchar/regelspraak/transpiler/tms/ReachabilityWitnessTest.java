package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.cases.DroolsBackend;
import dev.uchar.regelspraak.transpiler.tms.plan.StaticFunction;
import dev.uchar.regelspraak.transpiler.tms.plan.Route;

import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.conf.EqualityBehaviorOption;
import org.kie.internal.utils.KieHelper;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A DRL text reaches every helper the plan names (card drl-teksttak-op-truth-maintenance, step 1e; FABLE-2 §2.9): each
 * {@link StaticFunction} of a term and each {@link Route} of a test or a consequence is a public static method of a public
 * class, the only public method of its name there, so that a text calls it class-qualified and a call by name finds
 * one method. Two checks:
 * <ul>
 *   <li>the name of every helper is unique in its class, so not overloaded: an overloaded name would let a rendering
 *       into DRL text pick a method by its arguments, and a later overload could change the call without a
 *       complaint;</li>
 *   <li>one generated DRL text calls every helper class-qualified, from a function in the Java dialect, where a term
 *       goes, and every route from the consequence of a rule in MVEL as well, each with typed empty arguments; it is
 *       compiled with {@code KieHelper} under the bean loader of a golden and built in {@code EQUALITY}, as the DRL
 *       text of the branch will be. The text imports nothing, so no bean class reaches the Java dialect, which cannot
 *       see them (FABLE-2 §2.1).</li>
 * </ul>
 *
 * <p><b>Red</b> before the step, on {@code 36a3b0a9}, with the routes that a DRL text needed and that were not public:
 * routes naming {@code ComputeFault.isEvaluationFault}, {@code ComputeFault.designate} and {@code WritersConflict.of}, which
 * are package-private, fail both checks; and the overloaded {@code lijst} of the spike of FABLE-2 fails the first.
 */
class ReachabilityWitnessTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final String PACKAGE = "dev.uchar.regelspraak.tms.bereikbaarheid";

    /** Floors, measured 2026-10-13 at step 1e: the functions of a term and the routes. A deliberate removal lowers a
     *  floor in the same commit. */
    private static final int EXPECTED_MIN_FUNCTIONS = 76;
    private static final int EXPECTED_MIN_ROUTES = 41;

    @Test void every_helper_name_is_unique_in_its_class() {
        List<String> fault = new ArrayList<>();
        for (Map.Entry<String, Object[]> h : helpers().entrySet()) {
            Class<?> javaClass = (Class<?>) h.getValue()[0];
            String method = (String) h.getValue()[1];
            List<Method> methods = Arrays.stream(javaClass.getMethods()).filter(m -> m.getName().equals(method)).toList();
            if (methods.size() != 1 || !Modifier.isStatic(methods.get(0).getModifiers())
                    || !Modifier.isPublic(javaClass.getModifiers()))
                fault.add(h.getKey() + " " + javaClass.getName() + "." + method + ": " + methods);
        }
        assertEquals(List.of(), fault, "hulpen die niet de enige publieke statische methode van hun naam zijn");
    }

    @Test void a_drl_text_calls_every_class_qualified_helper() throws Exception {
        StringBuilder drl = new StringBuilder("package " + PACKAGE + ";\n\n");
        StringBuilder mvel = new StringBuilder();
        int functions = 0, routes = 0;
        for (Map.Entry<String, Object[]> h : helpers().entrySet()) {
            Class<?> javaClass = (Class<?>) h.getValue()[0];
            String method = (String) h.getValue()[1];
            // the arguments follow the method of that name, public or not, so that a helper the text cannot reach
            // fails on its visibility and not on its arguments
            Method m = Arrays.stream(javaClass.getMethods()).filter(x -> x.getName().equals(method)).findFirst()
                    .orElseGet(() -> Arrays.stream(javaClass.getDeclaredMethods()).filter(x -> x.getName().equals(method))
                            .findFirst().orElse(null));
            List<String> java = new ArrayList<>(), empty = new ArrayList<>();
            if (m != null) {
                for (Class<?> p : m.getParameterTypes()) {
                    java.add(p == boolean.class ? "false" : p.isPrimitive() ? "0" : "(" + p.getCanonicalName() + ") null");
                    empty.add(p == boolean.class ? "false" : p.isPrimitive() ? "0" : "null");
                }
            }
            String name = h.getKey().replace(' ', '_').replace('.', '_');
            String call = javaClass.getCanonicalName() + "." + method;
            boolean emptiness = m != null && m.getReturnType() == void.class;
            drl.append("function ").append(emptiness ? "void " : "java.lang.Object ").append(name).append("() { ")
                    .append(emptiness ? "" : "return ").append(call).append("(").append(String.join(", ", java))
                    .append("); }\n");
            if (h.getKey().startsWith("route")) {
                mvel.append("    ").append(call).append("(").append(String.join(", ", empty)).append(");\n");
                routes++;
            } else {
                functions++;
            }
        }
        drl.append("\nrule \"elke route uit een gevolg in MVEL\"\n    dialect \"mvel\"\nwhen\nthen\n").append(mvel)
                .append("end\n");
        ClassLoader beans = DroolsBackend.build(GOLDEN.resolve("jutterswet-penningdrempel").resolve("ir.json"),
                DroolsBackend.Branch.EXEC_MODEL).beanLoader();
        KieHelper helper = new KieHelper();
        helper.setClassLoader(beans);
        helper.addContent(drl.toString(), PACKAGE.replace('.', '/') + "/bereikbaarheid.drl");
        Results result = helper.verify();
        List<String> faults = new ArrayList<>();
        for (Message message : result.getMessages(Message.Level.ERROR))
            faults.add("r. " + message.getLine() + ": " + message.getText().replace('\n', ' '));
        assertEquals(List.of(), faults, () -> "de DRL-tekst compileert niet:\n" + drl);
        KieBase kieBase = helper.build(EqualityBehaviorOption.EQUALITY);
        assertNotNull(kieBase);
        int f = functions, r = routes;
        assertTrue(f >= EXPECTED_MIN_FUNCTIONS, () -> f + " functies, onder de vloer " + EXPECTED_MIN_FUNCTIONS);
        assertTrue(r >= EXPECTED_MIN_ROUTES, () -> r + " routes, onder de vloer " + EXPECTED_MIN_ROUTES);
    }

    /** Every helper the plan names, by "functie NAAM" or "route NAAM", with its class and its method name. */
    private static Map<String, Object[]> helpers() {
        Map<String, Object[]> out = new LinkedHashMap<>();
        for (StaticFunction f : StaticFunction.values()) out.put("functie " + f.name(), new Object[]{f.javaClass(), f.method()});
        for (Route r : Route.values()) out.put("route " + r.name(), new Object[]{r.javaClass(), r.method()});
        return out;
    }
}
