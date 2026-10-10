package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.engine.BeanCompiler;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.DrlTranspiler;
import dev.uchar.regelspraak.transpiler.kern.Dekking;
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
 * hands out, on the truth-maintenance branch as eight rules of its own beside the one that reads it, that the entry
 * refuses in the replay's sentence, and that no build of either transpiler can leave a side table out.
 */
class DroolsBackendWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path UITSLUITING = Path.of("..", "..", "corpus", "golden", "jutterswet-uitsluiting-rgs");

    // ---------------- the entry: one rule and one defaults family ----------------

    @Test void de_ingang_zet_de_regel_en_de_defaultsfamilie_in_de_kiebase() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(UITSLUITING.resolve("ir.json"), DroolsBackend.Branch.EXEC_MODEL);
        assertEquals(DroolsBackend.Branch.EXEC_MODEL, b.branch());
        // the family lowers to the rules of its standings and its defaults beside the rule that reads it
        assertEquals(List.of("defaults K:Jutter.uitgesloten: default 0.0.0", "defaults K:Jutter.uitgesloten: default 1",
                "defaults K:Jutter.uitgesloten: eens 0", "defaults K:Jutter.uitgesloten: eens wortel",
                "defaults K:Jutter.uitgesloten: terugval 0", "kenmerkstand Jutter.uitgesloten: onwaar",
                "kenmerkstand Jutter.uitgesloten: waar uit de invoer",
                "kenmerkstand Jutter.uitgesloten: waar uit een afleiding", "penninggerechtigd tenzij uitgesloten"),
                regels(b.kieBase()));
        assertEquals(List.of("defaults: defaults K:Jutter.uitgesloten", "regel: penninggerechtigd tenzij uitgesloten"),
                b.dekking().stream().map(Dekking::sleutel).sorted().toList());
        assertTrue(b.dekking().stream().allMatch(Dekking::lowered), "every entry lowered: " + b.dekking());
        assertTrue(b.dagsoortGlobalen().isEmpty(), "the truth-maintenance branch declares no day-kind globals");
    }

    /** What the build without the side tables handed out: the same model through the same transpiler with the
     *  defaults withheld — explicitly now, since no build can leave them out. One rule, and a manifest in which
     *  everything lowered, so nothing in the result said the family was missing. */
    @Test void zonder_de_defaults_mist_de_familie_en_zegt_het_manifest_niets() throws Exception {
        IrLoader.Loaded l = IrLoader.loadAll(JSON.readTree(Files.readString(UITSLUITING.resolve("ir.json"))));
        ClassLoader cl = BeanCompiler.compileer(FactModelGenerator.genereer(l.model()));
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        try {
            TmsTranspiler et = TmsTranspiler.build(l.model(), cl, List.of(), l.rolBindingen(), l.lets(),
                    l.bronPaden(), l.navigatieBindingen());
            assertEquals(List.of("penninggerechtigd tenzij uitgesloten"),
                    regels(KieBaseBuilder.createKieBaseFromModel(et.model())));
            assertEquals(List.of("regel: penninggerechtigd tenzij uitgesloten"),
                    et.dekking().stream().map(Dekking::sleutel).toList());
            assertTrue(et.dekking().stream().allMatch(Dekking::lowered), "the manifest says nothing: " + et.dekking());
        } finally {
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    // ---------------- the refusals are the replay's ----------------

    @Test void de_drl_tak_weigert_in_de_zin_van_de_replay() throws Exception {
        String zin = "drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering";
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> DroolsBackend.build(UITSLUITING.resolve("ir.json"), DroolsBackend.Branch.DRL_TEXT));
        assertEquals(zin, e.getMessage());
        CaseRunner.Result replay = CaseRunner.viaDrlTekst(UITSLUITING.resolve("ir.json"), zaken());
        assertEquals("EXCLUDE", replay.status());
        assertEquals(zin, replay.reason());
        CaseRunner.Result exec = CaseRunner.viaExecModel(UITSLUITING.resolve("ir.json"), zaken());
        assertEquals("PLAYED", exec.status(), "the exec-model replay of the same document: " + exec);
        assertEquals(4, exec.casesPlayed());
    }

    // ---------------- no build of either transpiler leaves a side table out ----------------

    /** What the loader returns beside the model is the list of side tables a document can carry, so it is the
     *  oracle here: every public static member of a transpiler that takes a model must take each of them, and no
     *  public constructor may exist (an instance made that way had lowered nothing and reported an empty
     *  manifest). A side table the loader gains fails this row until both builds take it. */
    @Test void elke_publieke_ingang_die_een_model_neemt_neemt_elke_zijtafel() {
        List<Type> geladen = Arrays.stream(IrLoader.Loaded.class.getRecordComponents())
                .map(RecordComponent::getGenericType).toList();
        for (Class<?> t : List.of(TmsTranspiler.class, DrlTranspiler.class)) {
            assertEquals(0, t.getConstructors().length, t.getSimpleName() + " has a public constructor");
            List<Method> ingangen = modelIngangen(t);
            for (Method m : ingangen) {
                List<Type> parameters = new ArrayList<>(Arrays.asList(m.getGenericParameterTypes()));
                for (Type zijtafel : geladen)
                    assertTrue(parameters.remove(zijtafel), m + " does not take " + zijtafel.getTypeName());
            }
            assertEquals(List.of("build"), ingangen.stream().map(Method::getName).toList(),
                    t.getSimpleName() + ": the public static members that take a model");
        }
    }

    /** Each side table passed as null is refused before anything is lowered, in a sentence naming it; null used to
     *  stand for "empty", which is the silent omission again by another route. */
    @Test void een_ontbrekende_zijtafel_wordt_luid_geweigerd() throws Exception {
        IrLoader.Loaded l = IrLoader.loadAll(JSON.readTree(Files.readString(UITSLUITING.resolve("ir.json"))));
        for (Class<?> t : List.of(TmsTranspiler.class, DrlTranspiler.class)) {
            Method build = modelIngangen(t).get(0);
            Class<?>[] soorten = build.getParameterTypes();
            int getoetst = 0;
            for (int i = 0; i < soorten.length; i++) {
                if (soorten[i] != List.class && soorten[i] != java.util.Map.class) continue;
                Object[] argumenten = argumenten(build, l);
                argumenten[i] = null;
                InvocationTargetException fout = assertThrows(InvocationTargetException.class,
                        () -> build.invoke(null, argumenten));
                NullPointerException npe = assertInstanceOf(NullPointerException.class, fout.getCause());
                assertTrue(npe.getMessage().startsWith("side table '") && npe.getMessage().endsWith("' is null — pass an "
                        + (soorten[i] == List.class ? "empty list" : "empty map") + " for a document without one"),
                        t.getSimpleName() + " parameter " + i + ": " + npe.getMessage());
                getoetst++;
            }
            assertEquals(5, getoetst, t.getSimpleName() + ": the five side tables");
        }
    }

    // ---------------- helpers ----------------

    private static List<String> regels(KieBase kieBase) {
        return kieBase.getKiePackages().stream().flatMap(p -> p.getRules().stream()).map(Rule::getName).sorted()
                .toList();
    }

    private static List<Path> zaken() throws Exception {
        try (Stream<Path> s = Files.list(UITSLUITING)) {
            return s.filter(f -> f.getFileName().toString().matches("case.*\\.json")).sorted().toList();
        }
    }

    private static List<Method> modelIngangen(Class<?> t) {
        return Arrays.stream(t.getMethods())
                .filter(m -> Modifier.isStatic(m.getModifiers()) && m.getDeclaringClass() == t)
                .filter(m -> Arrays.asList(m.getParameterTypes()).contains(dev.uchar.regelspraak.model.RegelSpraakModel.class))
                .toList();
    }

    /** The loader's own values for every parameter of {@code build}, matched by generic type in parameter order;
     *  the bean loader, which is no side table, is null (the transpiler then uses its own). */
    private static Object[] argumenten(Method build, IrLoader.Loaded l) throws Exception {
        List<RecordComponent> vrij = new ArrayList<>(Arrays.asList(IrLoader.Loaded.class.getRecordComponents()));
        Type[] typen = build.getGenericParameterTypes();
        Object[] uit = new Object[typen.length];
        for (int i = 0; i < typen.length; i++) {
            if (typen[i] == ClassLoader.class) continue;
            for (RecordComponent c : vrij) {
                if (!c.getGenericType().equals(typen[i])) continue;
                uit[i] = c.getAccessor().invoke(l);
                vrij.remove(c);
                break;
            }
        }
        return uit;
    }
}
