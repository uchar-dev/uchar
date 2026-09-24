package io.github.yhavinga.regelspraak.loader;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.yhavinga.regelspraak.ir.Conditie;
import io.github.yhavinga.regelspraak.ir.Expr;

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
class InstantieUnieWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void de_loader_leest_elke_selectie_letterlijk() throws Exception {
        Expr.Functie f = tellingVan(doc(unie(TAK_AANGEMELD, TAK_GEBORGEN)));
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

    @Test void een_selectiefilter_landt_op_de_selectie_en_niet_op_de_telling() throws Exception {
        Expr.Functie f = tellingVan(doc(unie(TAK_GEMERKT, TAK_GEBORGEN)));
        assertNull(f.filter(), "de telling draagt geen eigen filter (WF-110 (c))");
        Expr.InstanceSetUnion u = (Expr.InstanceSetUnion) f.argumenten().get(0);
        Conditie.Kenmerk k = assertInstanceOf(Conditie.Kenmerk.class, u.takken().get(0).filter());
        assertEquals("gemerkt", k.kenmerk());
        assertNull(u.takken().get(1).filter(), "een selectie zonder deelselectie draagt er geen");
    }

    @Test void de_generieke_wandeling_daalt_af_in_de_selecties() throws Exception {
        // Expr.verzamelOnopgelost walks the sealed union with no default arm; a union that read as an
        // opaque LEAF would report no surviving marker under it, and every caller takes that silence for
        // a licence to omit work. The marker sits in a SELECTION's filter, which is the only place a
        // union carries an expression at all.
        Expr.Functie f = tellingVan(doc(unie(TAK_ONOPGELOST, TAK_GEBORGEN)));
        java.util.List<String> gezien = new java.util.ArrayList<>();
        Expr.verzamelOnopgelost(f, (bron, reden) -> gezien.add(String.valueOf(bron)));
        assertEquals(1, gezien.size(), "de wandeling bereikt de deelselectie van een selectie niet: " + gezien);
    }

    @Test void een_selectie_zonder_populatie_wordt_benoemd_geweigerd() {
        IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class,
                () -> tellingVan(doc(unie(TAK_AANGEMELD, tak("null", "vondstberging", "geborgen vondst", "null")))));
        assertEquals("InstanceSetBranch", r.form());
        assertTrue(r.getMessage().contains("elementType"), r.getMessage());
    }

    @Test void een_selectie_zonder_relatie_is_een_documentfout() {
        // Since L2 of the v1 wire migration a selection's relation is required and non-null (IR-SPEC ch. 4.3):
        // a selection without one is not a form this backend declines but a document that is not uchar-ir-v1,
        // so it is a defect at its pointer, never the gate's auto-excluded channel.
        IrLoader.Documentfout f = assertThrows(IrLoader.Documentfout.class,
                () -> tellingVan(doc(unie(TAK_AANGEMELD,
                        "{\"node\":\"InstanceSetBranch\",\"elementType\":\"Vondst\",\"sourceRole\":null,"
                                + "\"filter\":null,\"bron\":\"geborgen vondsten van de Jutter\"}"))));
        assertTrue(f.plek().endsWith("/branches/1/sourceRole"), f.plek());
        assertTrue(f.getMessage().contains("expected a RolBinding"), f.getMessage());
    }

    @Test void een_unie_met_een_enkele_selectie_wordt_benoemd_geweigerd() {
        // WF-110 (b): one selection IS the ordinary instance count and has a spelling of its own, so
        // admitting it here would give one meaning two spellings — and this backend would then have two
        // lowerings for one shape
        IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class,
                () -> tellingVan(doc(unie(TAK_AANGEMELD))));
        assertEquals("InstanceSetUnion.branches", r.form());
        assertTrue(r.getMessage().contains("at least TWO selections"), r.getMessage());
        assertTrue(r.getMessage().contains("WF-110 (b)"), r.getMessage());
    }

    // --- the documents ------------------------------------------------------------------------------

    private static Expr.Functie tellingVan(String ir) throws Exception {
        var model = IrLoader.loadAll(JSON.readTree(ir)).model();
        assertEquals(1, model.resolvedRegels.size(), "één regel in dit document");
        return assertInstanceOf(Expr.Functie.class, model.resolvedRegels.get(0).waarde());
    }

    private static String tak(String elementType, String feittype, String rol, String filter) {
        String bronrol = feittype == null ? "null"
                : "{\"node\":\"RolBinding\",\"feittype\":\"%s\",\"rol\":\"%s\"}".formatted(feittype, rol);
        return ("{\"node\":\"InstanceSetBranch\",\"elementType\":%s,\"sourceRole\":%s,\"filter\":%s,"
                + "\"bron\":\"een selectie\"}").formatted(elementType, bronrol, filter);
    }

    private static final String TAK_AANGEMELD = tak("\"Vondst\"", "vondstaanmelding", "aangemelde vondst", "null");
    private static final String TAK_GEBORGEN = tak("\"Vondst\"", "vondstberging", "geborgen vondst", "null");
    private static final String TAK_ONOPGELOST = tak("\"Vondst\"", "vondstaanmelding", "aangemelde vondst",
            "{\"node\":\"Onopgelost\",\"bron\":\"die onleesbaar zijn\"}");
    private static final String TAK_GEMERKT = tak("\"Vondst\"", "vondstaanmelding", "aangemelde vondst",
            "{\"node\":\"Kenmerk\",\"kenmerk\":\"gemerkt\",\"negatie\":false,"
                    + "\"eigenaar\":null,\"periode\":null,\"bron\":\"gemerkt zijn\"}");

    private static String unie(String... takken) {
        return "{\"node\":\"InstanceSetUnion\",\"branches\":[%s],\"bron\":\"de unie\"}"
                .formatted(String.join(",", takken));
    }

    private static String doc(String unie) {
        String heel = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"NIET_NEGATIEF\"}";
        String waarde = "{\"node\":\"Aggregation\",\"kind\":\"AANTAL\",\"arguments\":[%s],\"type\":%s,\"bron\":\"het aantal …\"}"
                .formatted(unie, heel);
        String doel = "{\"node\":\"AttribuutRef\",\"attribuut\":\"aantal strandvondsten\","
                + "\"objecttype\":\"Jutter\",\"bron\":\"Het aantal strandvondsten van een Jutter\"}";
        String jutter = ("{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\","
                + "\"attributes\":[{\"node\":\"Attribute\",\"name\":\"aantal strandvondsten\",\"datatype\":%s}],"
                + "\"characteristics\":[]}").formatted(heel);
        String vondst = "{\"node\":\"ObjectType\",\"name\":\"Vondst\",\"animate\":false,\"plural\":\"Vondsten\","
                + "\"attributes\":[],"
                + "\"characteristics\":[{\"node\":\"Characteristic\",\"name\":\"gemerkt\",\"kind\":\"BIJVOEGLIJK\"}]}";
        String ftAan = "{\"node\":\"Feittype\",\"naam\":\"vondstaanmelding\","
                + "\"wederkerig\":false,\"rollen\":[{\"node\":\"Rol\",\"naam\":\"aanmelder\","
                + "\"objecttypeNaam\":\"Jutter\",\"meervoud\":null,\"meervoudig\":false},"
                + "{\"node\":\"Rol\",\"naam\":\"aangemelde vondst\",\"objecttypeNaam\":\"Vondst\","
                + "\"meervoud\":\"aangemelde vondsten\",\"meervoudig\":true}]}";
        String ftBerg = "{\"node\":\"Feittype\",\"naam\":\"vondstberging\","
                + "\"wederkerig\":false,\"rollen\":[{\"node\":\"Rol\",\"naam\":\"berger\","
                + "\"objecttypeNaam\":\"Jutter\",\"meervoud\":null,\"meervoudig\":false},"
                + "{\"node\":\"Rol\",\"naam\":\"geborgen vondst\",\"objecttypeNaam\":\"Vondst\","
                + "\"meervoud\":\"geborgen vondsten\",\"meervoudig\":true}]}";
        return ("{\"format\":\"uchar-ir-v1\",\"model\":{\"objectTypes\":[%s,%s],\"factTypes\":[%s,%s],"
                + "\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],"
                + "\"dayKinds\":[]},"
                + "\"rules\":{\"resolvedRegels\":[%s],"
                + "\"resolvedDagsoorten\":[],\"objectCreaties\":[],\"verdelingen\":[],\"consistenties\":[],"
                + "\"uniciteiten\":[],\"feitCreaties\":[],\"resolvedStartpunten\":[],"
                + "\"regelGroepen\":[]},\"diagnostics\":[]}")
                .formatted(jutter, vondst, ftAan, ftBerg,
                        io.github.yhavinga.regelspraak.cases.IrPad.regel("aantal strandvondsten", "BEREKENING", doel,
                                waarde, "null"));
    }
}
