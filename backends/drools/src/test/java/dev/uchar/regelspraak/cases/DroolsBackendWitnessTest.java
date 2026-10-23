package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.engine.BeanCompiler;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.core.Coverage;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler;

import org.drools.modelcompiler.KieBaseBuilder;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.definition.rule.Rule;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DroolsBackend}, the backend's public entry, and the one way each transpiler is built.
 *
 * <p>The failure this guards against was measured on the toolchain distribution before the entry existed: its
 * service, scenario runner and engine facade built the exec-model transpiler from the model and a bean loader
 * alone, a build that filled every side table with an empty collection. On {@code jutterswet-uitsluiting-rgs}
 * that KieBase held one rule instead of two — the defaults family was not in it — and the manifest said every
 * construct lowered, so four of four HTTP answers called an excluded jutter entitled, with status 200 and no
 * marker. The replay IS the entry here ({@code CaseRunner} builds through it), so gate 12 plays the entry on
 * every golden; these rows pin what the gate cannot see — that the defaults family is in the KieBase the entry
 * hands out, on the exec-model branch as eight rules of its own beside the one that reads it, that the DRL-text
 * branch plays the document as the exec-model branch does, and that no build of the analysis can leave a side table
 * out.
 */
class DroolsBackendWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path EXCLUSION = Path.of("..", "..", "corpus", "golden", "jutterswet-uitsluiting-rgs");

    // ---------------- the entry: one rule and one defaults family ----------------

    @Test void the_entry_sets_the_rule_and_the_defaults_family_in_the_kiebase() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(EXCLUSION.resolve("ir.json"), DroolsBackend.Branch.EXEC_MODEL);
        assertEquals(DroolsBackend.Branch.EXEC_MODEL, b.branch());
        // the family lowers to the rules of its standings and its defaults beside the rule that reads it
        assertEquals(List.of("defaults K:Jutter.uitgesloten: default 0.0.0", "defaults K:Jutter.uitgesloten: default 1",
                "defaults K:Jutter.uitgesloten: eens 0", "defaults K:Jutter.uitgesloten: eens wortel",
                "defaults K:Jutter.uitgesloten: terugval 0", "kenmerkstand Jutter.uitgesloten: onwaar",
                "kenmerkstand Jutter.uitgesloten: waar uit de invoer",
                "kenmerkstand Jutter.uitgesloten: waar uit een afleiding", "penninggerechtigd tenzij uitgesloten"),
                rules(b.kieBase()));
        assertEquals(List.of("defaults: defaults K:Jutter.uitgesloten", "regel: penninggerechtigd tenzij uitgesloten"),
                b.coverage().stream().map(Coverage::key).sorted().toList());
        assertTrue(b.coverage().stream().allMatch(Coverage::lowered), "every entry lowered: " + b.coverage());
        assertTrue(b.dayKindGlobals().isEmpty(), "the exec-model branch declares no day-kind globals");
    }

    /** What the build without the side tables handed out: the same model through the same transpiler with the
     *  defaults withheld — explicitly now, since no build can leave them out. One rule, and a manifest in which
     *  everything lowered, so nothing in the result said the family was missing. */
    @Test void without_the_defaults_the_family_misses_and_the_manifest_says_nothing() throws Exception {
        IrLoader.Loaded l = IrLoader.loadAll(JSON.readTree(Files.readString(EXCLUSION.resolve("ir.json"))));
        ClassLoader cl = BeanCompiler.compile(FactModelGenerator.generate(l.model()));
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        try {
            TmsTranspiler et = TmsTranspiler.build(l.model(), cl, List.of(), l.roleBindings(), l.lets(),
                    l.sourcePaths(), l.navigationBindings(), l.anchored());
            assertEquals(List.of("penninggerechtigd tenzij uitgesloten"),
                    rules(KieBaseBuilder.createKieBaseFromModel(et.model())));
            assertEquals(List.of("regel: penninggerechtigd tenzij uitgesloten"),
                    et.coverage().stream().map(Coverage::key).toList());
            assertTrue(et.coverage().stream().allMatch(Coverage::lowered), "the manifest says nothing: " + et.coverage());
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    // ---------------- the DRL-text branch builds what the exec-model branch builds ----------------

    /** Until card drl-teksttak-op-truth-maintenance the DRL-text branch refused this document in the replay's sentence,
     *  "drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering"; since then it renders the same
     *  analysis, and the entry hands out a KieBase of the text that plays the four cases as the exec-model branch
     *  does. */
    @Test void the_drl_branch_plays_what_the_exec_model_branch_plays() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(EXCLUSION.resolve("ir.json"), DroolsBackend.Branch.DRL_TEXT);
        assertEquals(DroolsBackend.Branch.DRL_TEXT, b.branch());
        assertEquals(b.execModelTranspiler().drl(), b.drl());
        CaseRunner.Result replay = CaseRunner.viaDrlText(EXCLUSION.resolve("ir.json"), cases());
        assertEquals("PLAYED", replay.status(), "the DRL-text replay: " + replay);
        assertEquals(4, replay.casesPlayed());
        CaseRunner.Result exec = CaseRunner.viaExecModel(EXCLUSION.resolve("ir.json"), cases());
        assertEquals("PLAYED", exec.status(), "the exec-model replay of the same document: " + exec);
        assertEquals(4, exec.casesPlayed());
    }

    /** Every build carries the analysis of its document, on either branch: the record refuses one without it. The
     *  scenario report reads the form of each fired rule off that analysis ({@code CaseRunner.listener}); until step 6c
     *  of card drl-teksttak-op-truth-maintenance it guarded a missing analysis itself, in a sentence from the time when
     *  only the exec-model branch had one, a guard this record made unreachable (the end lens on the card). */
    @Test void a_build_always_carries_the_analysis_of_its_document() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(EXCLUSION.resolve("ir.json"), DroolsBackend.Branch.EXEC_MODEL);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new DroolsBackend.Build(
                b.model(), b.beanLoader(), null, null, b.coverage(), b.kieBase()));
        assertEquals("a Build carries the analysis of its document", e.getMessage());
    }

    // ---------------- no build of the analysis leaves a side table out ----------------

    /** What the loader returns beside the model is the list of side tables a document can carry, so it is the
     *  oracle here: every public static member of a transpiler that takes a model must take each of them, and no
     *  public constructor may exist (an instance made that way had lowered nothing and reported an empty
     *  manifest). A side table the loader gains fails this row until the build takes it. Until card
     *  drl-teksttak-op-truth-maintenance the row held for the transpiler of the DRL-text branch as well, which that
     *  card removed. */
    @Test void every_public_entry_that_takes_a_model_takes_every_side_table() {
        List<Type> loaded = Arrays.stream(IrLoader.Loaded.class.getRecordComponents())
                .map(RecordComponent::getGenericType).toList();
        for (Class<?> t : List.of(TmsTranspiler.class)) {
            assertEquals(0, t.getConstructors().length, t.getSimpleName() + " has a public constructor");
            List<Method> entries = modelEntries(t);
            for (Method m : entries) {
                List<Type> parameters = new ArrayList<>(Arrays.asList(m.getGenericParameterTypes()));
                for (Type sideTable : loaded)
                    assertTrue(parameters.remove(sideTable), m + " does not take " + sideTable.getTypeName());
            }
            assertEquals(List.of("build"), entries.stream().map(Method::getName).toList(),
                    t.getSimpleName() + ": the public static members that take a model");
        }
    }

    /** Each side table passed as null is refused before anything is lowered, in a sentence naming it; null used to
     *  stand for "empty", which is the silent omission again by another route. */
    @Test void a_missing_side_table_is_loudly_refused() throws Exception {
        IrLoader.Loaded l = IrLoader.loadAll(JSON.readTree(Files.readString(EXCLUSION.resolve("ir.json"))));
        for (Class<?> t : List.of(TmsTranspiler.class)) {
            Method build = modelEntries(t).get(0);
            Class<?>[] kinds = build.getParameterTypes();
            int tested = 0;
            for (int i = 0; i < kinds.length; i++) {
                if (kinds[i] != List.class && kinds[i] != java.util.Map.class) continue;
                Object[] arguments = arguments(build, l);
                arguments[i] = null;
                InvocationTargetException fault = assertThrows(InvocationTargetException.class,
                        () -> build.invoke(null, arguments));
                NullPointerException npe = assertInstanceOf(NullPointerException.class, fault.getCause());
                assertTrue(npe.getMessage().startsWith("side table '") && npe.getMessage().endsWith("' is null — pass an "
                        + (kinds[i] == List.class ? "empty list" : "empty map") + " for a document without one"),
                        t.getSimpleName() + " parameter " + i + ": " + npe.getMessage());
                tested++;
            }
            assertEquals(5, tested, t.getSimpleName() + ": the five side tables");
        }
    }

    // ---------------- helpers ----------------

    private static List<String> rules(KieBase kieBase) {
        return kieBase.getKiePackages().stream().flatMap(p -> p.getRules().stream()).map(Rule::getName).sorted()
                .toList();
    }

    private static List<Path> cases() throws Exception {
        try (Stream<Path> s = Files.list(EXCLUSION)) {
            return s.filter(f -> f.getFileName().toString().matches("case.*\\.json")).sorted().toList();
        }
    }

    private static List<Method> modelEntries(Class<?> t) {
        return Arrays.stream(t.getMethods())
                .filter(m -> Modifier.isStatic(m.getModifiers()) && m.getDeclaringClass() == t)
                .filter(m -> Arrays.asList(m.getParameterTypes()).contains(dev.uchar.regelspraak.model.RegelSpraakModel.class))
                .toList();
    }

    /** The loader's own values for every parameter of {@code build}, matched by generic type in parameter order;
     *  the bean loader, which is no side table, is null (the transpiler then uses its own). */
    private static Object[] arguments(Method build, IrLoader.Loaded l) throws Exception {
        List<RecordComponent> free = new ArrayList<>(Arrays.asList(IrLoader.Loaded.class.getRecordComponents()));
        Type[] types = build.getGenericParameterTypes();
        Object[] out = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            if (types[i] == ClassLoader.class) continue;
            for (RecordComponent c : free) {
                if (!c.getGenericType().equals(types[i])) continue;
                out[i] = c.getAccessor().invoke(l);
                free.remove(c);
                break;
            }
        }
        return out;
    }
}
