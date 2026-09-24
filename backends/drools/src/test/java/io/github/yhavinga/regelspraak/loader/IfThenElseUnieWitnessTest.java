package io.github.yhavinga.regelspraak.loader;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.yhavinga.regelspraak.cases.IrPad;
import io.github.yhavinga.regelspraak.ir.Conditie;
import io.github.yhavinga.regelspraak.ir.Expr;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IR-SPEC ch. 4.10 — the {@code IfThenElse} wire form loads as an ORDINARY {@link Expr} union node, and every
 * GENERIC tree walk therefore descends into it without being told to.
 *
 * <p>What this replaces, and why it is worth a witness. Until the union grew the record, the loader parsed this
 * one form into an {@link Expr.Onopgelost} plus a side table keyed on that node's identity. A side table is
 * invisible to {@link Expr#bevat} and to every hand-written walk built on it: the node read as an opaque LEAF,
 * so a walk asking "does this tree mention X?" answered no for everything under the guard and both branches —
 * silently, since a leaf is a legitimate answer. Only walks that knew about the table saw through it. The pins
 * below are about that difference and nothing else: no value is computed here, and no gate expectation moves.
 *
 * <p>Models are authored in this file; no corpus file is read, moved or touched.
 */
class IfThenElseUnieWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void de_loader_bouwt_een_unieknoop_en_geen_onopgelost_blad() throws Exception {
        Expr waarde = waardeVan(genesteldModel());

        // the form sits INSIDE an ordinary node, so reaching it at all means the tree carries it — a side
        // table would have put an Onopgelost here and kept the shape elsewhere.
        Expr.Binair top = assertInstanceOf(Expr.Binair.class, waarde, "de waarde is de omhullende Binair");
        Expr.IfThenElse keuze = assertInstanceOf(Expr.IfThenElse.class, top.links(),
                "de ch. 10.2-vorm is een unie-knoop in de boom, geen Onopgelost-drager");

        // every required field of the wire shape landed on the record (WF-069's node; the wire spells them
        // condition / then / else).
        assertNotNull(keuze.type(), "type");
        assertNotNull(keuze.bron(), "bron");
        assertInstanceOf(Conditie.Vergelijking.class, keuze.conditie(), "de guard is de gewone conditie-unie");
        assertEquals("laag", ((Expr.AttribuutRef) keuze.dan()).attribuut(), "de dan-tak");
        assertEquals("hoog", ((Expr.AttribuutRef) keuze.anders()).attribuut(), "de anders-tak");

        // and nothing in this tree is unresolved any more: the last Onopgelost CARRIER is gone, so an
        // Onopgelost here would again mean a form parsed into a leaf.
        assertFalse(Expr.bevat(waarde, x -> x instanceof Expr.Onopgelost),
                "geen Onopgelost-blad meer in een boom die alleen een IfThenElse droeg");
    }

    @Test void een_generieke_wandeling_daalt_af_in_beide_takken_en_in_de_guard() throws Exception {
        Expr waarde = waardeVan(genesteldModel());

        // Expr.bevat is the generic walk every consumer's structural question is built on. Each of these three
        // reads is reachable ONLY through the ch. 10.2 node, so each answer is the arm doing its work.
        assertTrue(leest(waarde, "laag"), "de dan-tak wordt afgedaald");
        assertTrue(leest(waarde, "hoog"), "de anders-tak wordt afgedaald");
        assertTrue(leest(waarde, "grondslag"), "de operanden van de guard worden afgedaald");
        // the control: a name the document does not carry must still answer false, so the three above are not
        // a walk that says yes to everything.
        assertFalse(leest(waarde, "onbestaand"), "een niet-gelezen naam blijft onwaar");
    }

    @Test void de_wandeling_bereikt_de_vorm_ook_onder_een_let() throws Exception {
        // ch. 4.10's three nodes nest freely: the walk must reach an IfThenElse through a Let body, which is
        // the shape the corpus goldens actually carry (a variabelendeel lowering wrapping a branch).
        Expr waarde = waardeVan(onderLetModel());

        Expr.Let let = assertInstanceOf(Expr.Let.class, waarde, "de waarde is de omhullende Let");
        assertInstanceOf(Expr.IfThenElse.class, let.body(), "de body is de ch. 10.2-vorm");
        assertTrue(Expr.bevat(waarde, x -> x instanceof Expr.IfThenElse),
                "de generieke wandeling vindt de vorm onder een Let");
        assertTrue(leest(waarde, "laag"), "de dan-tak onder de Let");
        assertTrue(leest(waarde, "hoog"), "de anders-tak onder de Let");
        assertTrue(Expr.bevat(waarde, x -> x instanceof Expr.BinderRef b && "g".equals(b.naam())),
                "de binderlezing in de guard onder de Let");
    }

    // ---------------- helpers ----------------

    /** Whether the tree reads the named attribute — the generic scan, asked exactly as a consumer asks it. */
    private static boolean leest(Expr e, String attribuut) {
        return Expr.bevat(e, x -> x instanceof Expr.AttribuutRef a && attribuut.equals(a.attribuut()));
    }

    private static Expr waardeVan(String ir) throws Exception {
        RegelSpraakModel model = IrLoader.loadAll(JSON.readTree(ir)).model();
        assertEquals(1, model.resolvedRegels.size(), "één regel in dit document");
        return model.resolvedRegels.get(0).waarde();
    }

    // ---------------- the wire shapes ----------------

    private static String num(int dec) {
        return "{\"node\":\"Number\",\"whole\":false,\"decimals\":%d,\"sign\":\"ONBEPAALD\"}"
                .formatted(dec);
    }

    private static String attr(String naam, String type) {
        return ("{\"node\":\"AttribuutRef\",\"attribuut\":\"%s\",\"objecttype\":\"Jutter\","
                + "\"bron\":\"het %s van de Jutter\"}")
                .formatted(naam, naam);
    }

    private static String lit(String waarde, String type) {
        return IrPad.literal(waarde, type, waarde);
    }

    private static String binair(String op, String links, String rechts, String type) {
        return "{\"node\":\"Binair\",\"op\":\"%s\",\"links\":%s,\"rechts\":%s,\"type\":%s,\"bron\":\"%s\"}"
                .formatted(op, links, rechts, type, op.toLowerCase(Locale.ROOT));
    }

    private static String verg(String op, String links, String rechts) {
        return "{\"node\":\"Vergelijking\",\"op\":\"%s\",\"links\":%s,\"rechts\":%s,\"periode\":null,\"bron\":\"%s\"}"
                .formatted(op, links, rechts, op.toLowerCase(Locale.ROOT));
    }

    /** NB the wire fields are condition / then / else — two of them are Java keywords, so the record names them
     *  conditie / dan / anders. */
    private static String ite(String conditie, String dan, String anders, String type) {
        return ("{\"node\":\"IfThenElse\",\"type\":%s,\"condition\":%s,\"then\":%s,\"else\":%s,"
                + "\"bron\":\"als … dan … anders …\"}").formatted(type, conditie, dan, anders);
    }

    private static String let(String binder, String value, String body, String type) {
        return "{\"node\":\"Let\",\"type\":%s,\"binder\":\"%s\",\"value\":%s,\"body\":%s,\"bron\":\"%s is …\"}"
                .formatted(type, binder, value, body, binder);
    }

    private static String bref(String naam, String type) {
        return "{\"node\":\"BinderRef\",\"name\":\"%s\",\"bron\":\"%s\"}".formatted(naam, naam);
    }

    /** The ch. 10.2 node as the LEFT operand of an ordinary Binair — so the walk must pass an ordinary node
     *  first and the form cannot be found by looking at the value root alone. */
    private static String genesteldModel() {
        String keuze = ite(verg("GROTER", attr("grondslag", num(2)), lit("1.00", num(2))),
                attr("laag", num(2)), attr("hoog", num(2)), num(2));
        return doc(binair("PLUS", keuze, lit("0.00", num(2)), num(2)));
    }

    /** The same form under a Let body, its guard reading the binder. */
    private static String onderLetModel() {
        String keuze = ite(verg("GROTER", bref("g", num(2)), lit("1.00", num(2))),
                attr("laag", num(2)), attr("hoog", num(2)), num(2));
        return doc(let("g", attr("grondslag", num(2)), keuze, num(2)));
    }

    private static String doc(String waarde) {
        String attributen = ("{\"node\":\"Attribute\",\"name\":\"grondslag\",\"datatype\":%s},"
                + "{\"node\":\"Attribute\",\"name\":\"laag\",\"datatype\":%s},"
                + "{\"node\":\"Attribute\",\"name\":\"hoog\",\"datatype\":%s},"
                + "{\"node\":\"Attribute\",\"name\":\"uit\",\"datatype\":%s}")
                .formatted(num(2), num(2), num(2), num(2));
        String objecttype = ("{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\","
                + "\"attributes\":[%s],\"characteristics\":[]}").formatted(attributen);
        String regel = IrPad.regel("uit van een jutter", "BEREKENING", attr("uit", num(2)), waarde, "null");
        return ("{\"format\":\"uchar-ir-v1\",\"model\":{\"objectTypes\":[%s],"
                + "\"factTypes\":[],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],"
                + "\"timelines\":[],\"dayKinds\":[]},"
                + "\"rules\":{\"resolvedRegels\":[%s],\"resolvedDagsoorten\":[],\"objectCreaties\":[],\"verdelingen\":[],"
                + "\"consistenties\":[],\"uniciteiten\":[],\"feitCreaties\":[],"
                + "\"resolvedStartpunten\":[],\"regelGroepen\":[]},\"diagnostics\":[]}")
                .formatted(objecttype, regel);
    }
}
