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
 * lowered: the build row was refused by that entry, and the two-names row got the manifest's sentence instead of its
 * own ("but was: &lt;tms-transpiler SKIP: regel: actief — de verlaging van een kenmerktoekenning is nog niet
 * gebouwd&gt;"). The side-table row was green from the start: the frame it pins was written before it.
 *
 * <p>Two rows of the first step pinned the refusal of an attribute target and of a leeg or gevuld test; the second
 * step lowers both, and their rows now pin a value and a condition the branch refuses, a BuiltinCall over a timeline
 * (an Aggregation until the aggregations were carried, the absolute value until the value forms were) and a test of
 * whether a rule fired (a QuantifierFold until the condition forms were carried). <b>Red on the first step</b> (measured 2026-10-08), which refused both rules for another reason:
 * "but was: &lt;tms-transpiler SKIP: regel: lampental — een attribuutdoel (berekening van 'lampental' van Vuurtoren): de
 * truth-maintenance-tak leidt nog alleen kenmerken af&gt;" and "but was: &lt;tms-transpiler SKIP: regel: helder — een
 * QuantifierFold in de voorwaarde ('de sterkte van ten minste 2 van de lampen van de Vuurtoren groter is dan 5'): de
 * truth-maintenance-tak leest nog alleen kenmerken van het eigen onderwerp, ook ontkend en in een Compound&gt;". The
 * value row's dayCount, <b>red at the claim of the card tms-expressievormen-en-rekendatum</b> (measured 2026-10-09),
 * where every builtin call was refused in one sentence: "but was: &lt;tms-transpiler SKIP: regel: lampental — een
 * BuiltinCall in een waarde ('het aantal dagen in het jaar dat hij helder is'): buiten de proef van de
 * truth-maintenance-tak&gt;".
 */
class TmsSkeletWitnessTest {

    private static final List<String> KENMERKEN = List.of("erkend", "vergund", "actief");

    @Test void de_build_draagt_de_tak_en_een_kiebase_in_equality() throws Exception {
        Path ir = TmsDocumenten.document(TmsDocumenten.model(KENMERKEN),
                List.of(kenmerkregel("vergund indien erkend", "vergund", check("erkend", false))));
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        assertEquals(DroolsBackend.Branch.EXEC_MODEL, b.branch());
        assertNull(b.drl());
        assertEquals(List.of("regel: vergund indien erkend"), b.dekking().stream().map(Dekking::sleutel).toList());
        assertTrue(b.dekking().stream().allMatch(Dekking::lowered), b.dekking()::toString);
        assertEquals(RuleBaseConfiguration.AssertBehaviour.EQUALITY, gelijkheid(b.kieBase()));
        // the rule of a form carries the form's name; vergund is read by nothing, so it has no standing rules
        assertEquals(List.of(TmsTranspiler.PKG + ":vergund indien erkend"), regels(b.kieBase()));
        // until card drl-teksttak-op-truth-maintenance the DRL-text branch kept the default of Drools, IDENTITY; it
        // renders the same analysis since, whose design asks EQUALITY
        DroolsBackend.Build drl = DroolsBackend.build(ir, DroolsBackend.Branch.DRL_TEXT);
        assertEquals(RuleBaseConfiguration.AssertBehaviour.EQUALITY, gelijkheid(drl.kieBase()),
                "the DRL-text branch builds its text in EQUALITY as well");
    }

    /** "Het lampental van een Vuurtoren moet berekend worden als het aantal dagen in het jaar dat hij helder is": a
     *  BuiltinCall that buckets a timeline, written to a target that holds one value. Since the card
     *  tms-verdeling-en-tijdlijnen (2026-10-09) the branch writes such a builtin as the timeline of a temporal target,
     *  and refuses it anywhere else in a sentence of its own; until then it refused every timeline as outside the
     *  trial ("… tijdlijnen zijn buiten de proef van de truth-maintenance-tak"). Until the aggregations were carried
     *  (card tms-aggregaties-creatie-en-fouten, 2026-10-08) this row pinned the refusal of "het aantal lampen van de
     *  Vuurtoren", an Aggregation, and until the value forms were carried (card tms-expressievormen-en-rekendatum,
     *  2026-10-09) that of "de absolute waarde van -3", which the branch now computes. */
    @Test void een_waarde_buiten_de_proef_wordt_geweigerd_met_de_knoop() throws Exception {
        String dagen = "{\"node\":\"BuiltinCall\",\"kind\":\"dayCount\",\"arguments\":[],\"filter\":{\"node\":"
                + "\"CharacteristicCheck\",\"characteristic\":\"helder\",\"negated\":false,\"source\":{\"snippet\":"
                + "\"hij helder is\"}},\"granularity\":\"year\",\"type\":" + GEHEEL + ",\"source\":{\"snippet\":"
                + "\"het aantal dagen in het jaar dat hij helder is\"}}";
        String lampental = IrPad.regel("lampental", "BEREKENING", "{\"node\":\"AttributeReference\",\"attribute\":"
                + "\"lampental\",\"subjectType\":\"Vuurtoren\",\"source\":{\"snippet\":\"Het lampental van een Vuurtoren\"}}",
                dagen, "null");
        Path ir = TmsDocumenten.document(VUURTOREN, List.of(lampental));
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL));
        assertEquals("transpiler SKIP: regel: lampental — een BuiltinCall dayCount in een waarde ('het aantal dagen in"
                + " het jaar dat hij helder is'): deze tak rekent een tijdsevenredig deel en een"
                + " dagentelling alleen als de tijdlijn van een tijdsafhankelijk doel (RUNTIME-SEMANTICS ch. 3.7, 3.8,"
                + " 3.11)", e.getMessage());
    }

    /** "Een Vuurtoren is helder indien regelversie verlichting is gevuurd": a Regelversie 'is gevuurd', which the branch
     *  does not carry, since it is no construct of the core. Until the condition forms were carried (card tms-expressievormen-en-rekendatum, 2026-10-09) this
     *  row pinned the refusal of "de sterkte van ten minste 2 van de lampen van de Vuurtoren groter is dan 5", a
     *  QuantifierFold, which the branch now lowers. */
    @Test void een_voorwaarde_buiten_de_proef_wordt_geweigerd_met_de_knoop() throws Exception {
        String snippet = "regelversie verlichting is gevuurd";
        String gevuurd = "{\"node\":\"Regelversie\",\"regel\":\"verlichting\",\"inconsistent\":false,\"source\":"
                + "{\"snippet\":\"" + snippet + "\"}}";
        String helder = IrPad.regel("helder", "KENMERKTOEKENNING", "{\"node\":\"AttributeReference\",\"attribute\":"
                + "\"helder\",\"subjectType\":\"Vuurtoren\",\"source\":{\"snippet\":\"Een Vuurtoren is helder\"}}",
                IrPad.literal("waar", "{\"node\":\"Boolean\"}", "waar"), gevuurd);
        Path ir = TmsDocumenten.document(VUURTOREN, List.of(helder));
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL));
        assertEquals("transpiler SKIP: regel: helder — een Regelversie 'is gevuurd' in de voorwaarde ('" + snippet
                + "'): geen vorm van de kern maar een predicaat van het"
                + " RegelSpraak-dialect over het fixpunt (RUNTIME-SEMANTICS ch. 5.6, CONSTRUCTS rij 30), dat de"
                + " referentie-interpreter en de ASP-backend niet evalueren", e.getMessage());
    }

    /** A Vuurtoren with its lampen over a role to many, as the jutterswet-vuurtoren golden declares them. */
    private static final String VUURTOREN = "{\"objectTypes\":[{\"node\":\"ObjectType\",\"name\":\"Vuurtoren\",\"animate\":"
            + "false,\"plural\":\"Vuurtorens\",\"attributes\":[{\"node\":\"Attribute\",\"name\":\"lampental\",\"datatype\":"
            + "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}}],\"characteristics\":[{\"node\":"
            + "\"Characteristic\",\"name\":\"helder\",\"kind\":\"adjectival\"}]},{\"node\":\"ObjectType\",\"name\":\"Lamp\","
            + "\"animate\":false,\"plural\":\"Lampen\",\"attributes\":[{\"node\":\"Attribute\",\"name\":\"sterkte\",\"datatype\":"
            + "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}}],\"characteristics\":[]}],"
            + "\"factTypes\":[{\"node\":\"FactType\",\"name\":\"torenindeling\",\"reciprocal\":false,\"roles\":[{\"node\":"
            + "\"Role\",\"name\":\"toren\",\"objectType\":\"Vuurtoren\",\"many\":false},{\"node\":\"Role\",\"name\":\"lamp\","
            + "\"objectType\":\"Lamp\",\"plural\":\"lampen\",\"many\":true}]}],\"parameters\":[],\"domains\":[],"
            + "\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";

    @Test void twee_regels_van_een_naam_weigert_de_tak_benoemd() throws Exception {
        Path ir = TmsDocumenten.document(TmsDocumenten.model(KENMERKEN),
                List.of(kenmerkregel("actief", "actief", check("erkend", false)),
                        kenmerkregel("actief", "vergund", check("erkend", false))));
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL));
        assertEquals("exec-model: Duplicate rule name: actief — twee regels van één naam in pakket " + TmsTranspiler.PKG
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
