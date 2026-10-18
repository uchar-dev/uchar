package dev.uchar.regelspraak.loader;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.ir.Conditie;
import dev.uchar.regelspraak.ir.Expr;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IR-SPEC ch. 4.3 / WF-110 — the {@code InstanceSetUnion} wire form, the §5.7 concatenation of subject
 * expressions as the source of an instance count (semantics RUNTIME-SEMANTICS ch. 3.4d).
 *
 * <p>Two things are pinned here and nowhere else. First, the loader reads every selection VERBATIM — its own
 * population, its own one-step relation, its own die/dat subselection — and derives none of them. That is
 * WF-110 (e)'s ground as code: every selection of one union shares one type pair, so a backend that
 * re-derived a range from that pair would give all the selections the same one and count a restatement of a
 * single selection, silently. Second, the three malformations the loader refuses BY NAME, each of which a
 * well-formed corpus can never carry and which would otherwise arrive as an unattributed crash.
 *
 * <p>The corpus goldens {@code jutterswet-vondstunie} and {@code jutterswet-keurunie} carry the ANSWERS (gate
 * 12 replays them through both branches against the numbers ref and clingo pin). This file carries the shapes
 * no answer can reach. Models are authored here; no corpus file is read, moved or touched.
 */
class InstanceUnionWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void the_loader_reads_each_selection_literally() throws Exception {
        Expr.Functie f = countOf(doc(union(BRANCH_AANGEMELD, BRANCH_GEBORGEN)));
        assertEquals(Expr.FunctieKind.AANTAL, f.kind());
        assertEquals(1, f.argumenten().size(), "de unie is ÉÉN argument");
        assertNull(f.elementObjecttype(), "de telling boven een unie noemt geen eigen bron (WF-110 (c))");
        Expr.InstanceSetUnion u = assertInstanceOf(Expr.InstanceSetUnion.class, f.argumenten().get(0));
        assertEquals(2, u.takken().size());
        // the two selections walk DIFFERENT relations and both are read from the wire: a by-pair search
        // over (Jutter, Vondst) is ambiguous here by construction, which is exactly why the field exists
        assertEquals("vondstaanmelding", u.takken().get(0).bronRol().feittype());
        assertEquals("aangemelde vondst", u.takken().get(0).bronRol().rol());
        assertEquals("vondstberging", u.takken().get(1).bronRol().feittype());
        assertEquals("geborgen vondst", u.takken().get(1).bronRol().rol());
        assertEquals("Vondst", u.takken().get(0).elementType().naam);
        assertEquals("Vondst", u.takken().get(1).elementType().naam);
        assertNotNull(u.bron(), "bron");
    }

    @Test void a_selection_filter_lands_on_the_selection_and_not_on_the_count() throws Exception {
        Expr.Functie f = countOf(doc(union(BRANCH_GEMERKT, BRANCH_GEBORGEN)));
        assertNull(f.filter(), "de telling draagt geen eigen filter (WF-110 (c))");
        Expr.InstanceSetUnion u = (Expr.InstanceSetUnion) f.argumenten().get(0);
        Conditie.Kenmerk k = assertInstanceOf(Conditie.Kenmerk.class, u.takken().get(0).filter());
        assertEquals("gemerkt", k.kenmerk());
        assertNull(u.takken().get(1).filter(), "een selectie zonder deelselectie draagt er geen");
    }

    @Test void the_generic_walk_descends_into_the_selections() throws Exception {
        // Expr.verzamelOnopgelost walks the sealed union with no default arm; a union that read as an
        // opaque LEAF would report no surviving marker under it, and every caller takes that silence for
        // a licence to omit work. The marker sits in a SELECTION's filter, which is the only place a
        // union carries an expression at all.
        Expr.Functie f = countOf(doc(union(BRANCH_UNRESOLVED, BRANCH_GEBORGEN)));
        java.util.List<String> seen = new java.util.ArrayList<>();
        Expr.verzamelOnopgelost(f, (source, reason) -> seen.add(String.valueOf(source)));
        assertEquals(1, seen.size(), "de wandeling bereikt de deelselectie van een selectie niet: " + seen);
    }

    @Test void a_selection_without_population_is_refused_as_a_named_form() {
        IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class,
                () -> countOf(doc(union(BRANCH_AANGEMELD, branch("null", "vondstberging", "geborgen vondst", "null")))));
        assertEquals("InstanceSetBranch", r.form());
        assertTrue(r.getMessage().contains("elementType"), r.getMessage());
    }

    @Test void a_selection_without_relation_is_a_document_error() {
        // Since L2 of the v1 wire migration a selection's relation is required and non-null (IR-SPEC ch. 4.3):
        // a selection without one is not a form this backend declines but a document that is not uchar-ir-v1,
        // so it is a defect at its pointer, never the gate's auto-excluded channel.
        IrLoader.Documentfout f = assertThrows(IrLoader.Documentfout.class,
                () -> countOf(doc(union(BRANCH_AANGEMELD,
                        "{\"node\":\"InstanceSetBranch\",\"elementType\":\"Vondst\",\"sourceRole\":null,"
                                + "\"filter\":null,\"source\":{\"snippet\":\"geborgen vondsten van de Jutter\"}}"))));
        assertTrue(f.site().endsWith("/branches/1/sourceRole"), f.site());
        assertTrue(f.getMessage().contains("expected a RoleBinding"), f.getMessage());
    }

    @Test void a_union_with_a_single_selection_is_refused_as_a_named_form() {
        // WF-110 (b): one selection IS the ordinary instance count and has a spelling of its own, so
        // admitting it here would give one meaning two spellings — and this backend would then have two
        // lowerings for one shape
        IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class,
                () -> countOf(doc(union(BRANCH_AANGEMELD))));
        assertEquals("InstanceSetUnion.branches", r.form());
        assertTrue(r.getMessage().contains("at least TWO selections"), r.getMessage());
        assertTrue(r.getMessage().contains("WF-110 (b)"), r.getMessage());
    }

    // --- the documents ------------------------------------------------------------------------------

    private static Expr.Functie countOf(String ir) throws Exception {
        var model = IrLoader.loadAll(JSON.readTree(ir)).model();
        assertEquals(1, model.resolvedRegels.size(), "één regel in dit document");
        return assertInstanceOf(Expr.Functie.class, model.resolvedRegels.get(0).waarde());
    }

    private static String branch(String elementType, String factType, String role, String filter) {
        String sourceRole = factType == null ? "null"
                : "{\"node\":\"RoleBinding\",\"factType\":\"%s\",\"role\":\"%s\"}".formatted(factType, role);
        return ("{\"node\":\"InstanceSetBranch\",\"elementType\":%s,\"sourceRole\":%s,\"filter\":%s,"
                + "\"source\":{\"snippet\":\"een selectie\"}}").formatted(elementType, sourceRole, filter);
    }

    private static final String BRANCH_AANGEMELD = branch("\"Vondst\"", "vondstaanmelding", "aangemelde vondst", "null");
    private static final String BRANCH_GEBORGEN = branch("\"Vondst\"", "vondstberging", "geborgen vondst", "null");
    private static final String BRANCH_UNRESOLVED = branch("\"Vondst\"", "vondstaanmelding", "aangemelde vondst",
            "{\"node\":\"Unresolved\",\"source\":{\"snippet\":\"die onleesbaar zijn\"}}");
    private static final String BRANCH_GEMERKT = branch("\"Vondst\"", "vondstaanmelding", "aangemelde vondst",
            "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"gemerkt\",\"negated\":false,"
                    + "\"source\":{\"snippet\":\"gemerkt zijn\"}}");

    private static String union(String... branches) {
        return "{\"node\":\"InstanceSetUnion\",\"branches\":[%s],\"source\":{\"snippet\":\"de unie\"}}"
                .formatted(String.join(",", branches));
    }

    private static String doc(String union) {
        String whole = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"nonNegative\"}";
        String value = "{\"node\":\"Aggregation\",\"kind\":\"count\",\"arguments\":[%s],\"type\":%s,\"source\":{\"snippet\":\"het aantal …\"}}"
                .formatted(union, whole);
        String target = "{\"node\":\"AttributeReference\",\"attribute\":\"aantal strandvondsten\","
                + "\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"Het aantal strandvondsten van een Jutter\"}}";
        String jutter = ("{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\","
                + "\"attributes\":[{\"node\":\"Attribute\",\"name\":\"aantal strandvondsten\",\"datatype\":%s}],"
                + "\"characteristics\":[]}").formatted(whole);
        String vondst = "{\"node\":\"ObjectType\",\"name\":\"Vondst\",\"animate\":false,\"plural\":\"Vondsten\","
                + "\"attributes\":[],"
                + "\"characteristics\":[{\"node\":\"Characteristic\",\"name\":\"gemerkt\",\"kind\":\"adjectival\"}]}";
        String ftVondstaanmelding = "{\"node\":\"FactType\",\"name\":\"vondstaanmelding\","
                + "\"reciprocal\":false,\"roles\":[{\"node\":\"Role\",\"name\":\"aanmelder\","
                + "\"objectType\":\"Jutter\",\"many\":false},"
                + "{\"node\":\"Role\",\"name\":\"aangemelde vondst\",\"objectType\":\"Vondst\","
                + "\"plural\":\"aangemelde vondsten\",\"many\":true}]}";
        String ftVondstberging = "{\"node\":\"FactType\",\"name\":\"vondstberging\","
                + "\"reciprocal\":false,\"roles\":[{\"node\":\"Role\",\"name\":\"berger\","
                + "\"objectType\":\"Jutter\",\"many\":false},"
                + "{\"node\":\"Role\",\"name\":\"geborgen vondst\",\"objectType\":\"Vondst\","
                + "\"plural\":\"geborgen vondsten\",\"many\":true}]}";
        return ("{\"format\":\"uchar-ir-v1\",\"model\":{\"objectTypes\":[%s,%s],\"factTypes\":[%s,%s],"
                + "\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],"
                + "\"dayKinds\":[]},"
                + "\"rules\":{\"rules\":[%s],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],"
                + "\"uniquenesses\":[],\"factCreations\":[],\"startPointDeterminations\":[],"
                + "\"ruleGroups\":[]},\"diagnostics\":[]}")
                .formatted(jutter, vondst, ftVondstaanmelding, ftVondstberging,
                        dev.uchar.regelspraak.cases.IrPath.regel("aantal strandvondsten", "BEREKENING", target,
                                value, "null"));
    }
}
