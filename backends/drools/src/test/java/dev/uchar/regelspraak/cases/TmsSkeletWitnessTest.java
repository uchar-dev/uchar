package dev.uchar.regelspraak.cases;

import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.transpiler.kern.Dekking;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler;

import org.drools.core.RuleBaseConfiguration;
import org.drools.core.impl.InternalRuleBase;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;

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

import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The frame of the truth-maintenance branch, around its lowering: the build names the branch and carries the one
 * transpiler, the KieBase is configured in EQUALITY mode as the branch's design requires (this row reads the
 * configuration and fires no session; what the branch needs from the merging of equal logical facts is
 * {@code TmsRechtvaardigingWitnessTest}'s), a form the branch does not carry is refused by its manifest entry, which
 * names the form, two rules of one name are refused before the KieBase is built, and the transpiler's one build takes
 * every side table the loader returns. Every document is written here, on Jutterswet material.
 *
 * <p><b>Red on the skeleton</b> (measured 2026-10-08), whose manifest listed every characteristic attribution as not
 * lowered: the build row was refused by that entry, and the two-names row and the predicate row got the manifest's
 * sentence instead of their own ("but was: &lt;tms-transpiler SKIP: regel: actief — de verlaging van een
 * kenmerktoekenning is nog niet gebouwd&gt;"). The attribute-target row and the side-table row were green from the
 * start: the frame they pin was written before them.
 */
class TmsSkeletWitnessTest {

    private static final List<String> KENMERKEN = List.of("erkend", "vergund", "actief");

    @Test void de_build_draagt_de_tak_en_een_kiebase_in_equality() throws Exception {
        Path ir = TmsDocumenten.document(TmsDocumenten.model(KENMERKEN),
                List.of(kenmerkregel("vergund indien erkend", "vergund", check("erkend", false))));
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.TRUTH_MAINTENANCE);
        assertEquals(DroolsBackend.Branch.TRUTH_MAINTENANCE, b.branch());
        assertNull(b.execModelTranspiler());
        assertNull(b.drlTranspiler());
        assertEquals(List.of("regel: vergund indien erkend"), b.dekking().stream().map(Dekking::sleutel).toList());
        assertTrue(b.dekking().stream().allMatch(Dekking::lowered), b.dekking()::toString);
        assertEquals(RuleBaseConfiguration.AssertBehaviour.EQUALITY, gelijkheid(b.kieBase()));
        // the rule of a form carries the form's name; vergund is read by nothing, so it has no standing rules
        assertEquals(List.of(TmsTranspiler.PKG + ":vergund indien erkend"), regels(b.kieBase()));
        DroolsBackend.Build exec = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        assertEquals(RuleBaseConfiguration.AssertBehaviour.IDENTITY, gelijkheid(exec.kieBase()));
    }

    @Test void een_attribuutdoel_wordt_geweigerd_met_de_vorm() throws Exception {
        String model = "{\"objectTypes\":[{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":"
                + "\"Jutters\",\"attributes\":[{\"node\":\"Attribute\",\"name\":\"gewicht\",\"datatype\":" + GEHEEL + "}],"
                + "\"characteristics\":[]}],\"factTypes\":[],\"parameters\":[],\"domains\":[],\"unitSystems\":[],"
                + "\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";
        String gewicht = IrPad.regel("gewicht", "GELIJKSTELLING", "{\"node\":\"AttributeReference\",\"attribute\":"
                + "\"gewicht\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"Het gewicht van een Jutter\"}}",
                IrPad.literal("3", GEHEEL, "3"), "null");
        Path ir = TmsDocumenten.document(model, List.of(gewicht));
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> DroolsBackend.build(ir, DroolsBackend.Branch.TRUTH_MAINTENANCE));
        assertEquals("tms-transpiler SKIP: regel: gewicht — een attribuutdoel (gelijkstelling van 'gewicht' van Jutter):"
                + " de truth-maintenance-tak leidt nog alleen kenmerken af", e.getMessage());
    }

    @Test void een_voorwaarde_buiten_de_kenmerkchecks_wordt_geweigerd_met_de_knoop() throws Exception {
        String model = "{\"objectTypes\":[{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":"
                + "\"Jutters\",\"attributes\":[{\"node\":\"Attribute\",\"name\":\"startdag\",\"datatype\":{\"node\":\"Date\","
                + "\"precision\":\"day\"}}],\"characteristics\":[{\"node\":\"Characteristic\",\"name\":\"actief\",\"kind\":"
                + "\"adjectival\"}]}],\"factTypes\":[],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],"
                + "\"timelines\":[],\"dayKinds\":[]}";
        String gevuld = "{\"node\":\"Predicate\",\"kind\":\"filled\",\"operand\":{\"node\":\"AttributeReference\","
                + "\"attribute\":\"startdag\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"zijn startdag\"}},"
                + "\"negated\":false,\"digits\":0,\"source\":{\"snippet\":\"zijn startdag gevuld is\"}}";
        Path ir = TmsDocumenten.document(model, List.of(kenmerkregel("actief", "actief", gevuld)));
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> DroolsBackend.build(ir, DroolsBackend.Branch.TRUTH_MAINTENANCE));
        assertEquals("tms-transpiler SKIP: regel: actief — een Predicate in de voorwaarde ('zijn startdag gevuld is'): de"
                + " truth-maintenance-tak leest nog alleen kenmerken van het eigen onderwerp, ook ontkend en in een"
                + " Compound", e.getMessage());
    }

    @Test void twee_regels_van_een_naam_weigert_de_tak_benoemd() throws Exception {
        Path ir = TmsDocumenten.document(TmsDocumenten.model(KENMERKEN),
                List.of(kenmerkregel("actief", "actief", check("erkend", false)),
                        kenmerkregel("actief", "vergund", check("erkend", false))));
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> DroolsBackend.build(ir, DroolsBackend.Branch.TRUTH_MAINTENANCE));
        assertEquals("tms: Duplicate rule name: actief — twee regels van één naam in pakket " + TmsTranspiler.PKG
                + "; de KieBase houdt één regel per naam, zodat de tweede de eerste zou vervangen (vormnamen zijn uniek,"
                + " WF-020)", e.getMessage());
    }

    /** The loader's side tables are the oracle, as for the other two transpilers ({@code DroolsBackendWitnessTest}): no
     *  public constructor, one public static member that takes a model, which takes every table, and refuses each one
     *  passed as null in a sentence naming it. */
    @Test void de_enige_ingang_neemt_elke_zijtafel() throws Exception {
        assertEquals(0, TmsTranspiler.class.getConstructors().length);
        List<Method> ingangen = Arrays.stream(TmsTranspiler.class.getMethods())
                .filter(m -> Modifier.isStatic(m.getModifiers()) && m.getDeclaringClass() == TmsTranspiler.class)
                .filter(m -> Arrays.asList(m.getParameterTypes()).contains(dev.uchar.regelspraak.model.RegelSpraakModel.class))
                .toList();
        assertEquals(List.of("build"), ingangen.stream().map(Method::getName).toList());
        Method build = ingangen.get(0);
        List<Type> parameters = new ArrayList<>(Arrays.asList(build.getGenericParameterTypes()));
        for (RecordComponent c : IrLoader.Loaded.class.getRecordComponents())
            assertTrue(parameters.remove(c.getGenericType()), "build does not take " + c.getGenericType().getTypeName());

        Path ir = TmsDocumenten.document(TmsDocumenten.model(KENMERKEN), List.of());
        IrLoader.Loaded l = IrLoader.loadAll(TmsDocumenten.JSON.readTree(Files.readString(ir)));
        Object[] goed = {l.model(), null, l.prioritizedDefaults(), l.rolBindingen(), l.lets(), l.bronPaden(),
                l.navigatieBindingen()};
        assertSame(TmsTranspiler.class, build.invoke(null, goed).getClass());
        int getoetst = 0;
        for (int i = 2; i < goed.length; i++) {
            Object[] argumenten = goed.clone();
            argumenten[i] = null;
            InvocationTargetException fout = assertThrows(InvocationTargetException.class, () -> build.invoke(null, argumenten));
            NullPointerException npe = assertInstanceOf(NullPointerException.class, fout.getCause());
            String soort = build.getParameterTypes()[i] == List.class ? "empty list" : "empty map";
            assertTrue(npe.getMessage().startsWith("side table '")
                    && npe.getMessage().endsWith("' is null — pass an " + soort + " for a document without one"),
                    "parameter " + i + ": " + npe.getMessage());
            getoetst++;
        }
        assertEquals(5, getoetst);
    }

    private static final String GEHEEL = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";

    private static RuleBaseConfiguration.AssertBehaviour gelijkheid(KieBase kb) {
        return ((InternalRuleBase) kb).getRuleBaseConfiguration().getAssertBehaviour();
    }

    private static List<String> regels(KieBase kb) {
        return kb.getKiePackages().stream().flatMap(p -> p.getRules().stream()
                .map(r -> p.getName() + ":" + r.getName())).sorted().toList();
    }
}
