package dev.uchar.regelspraak.loader;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.cases.IrPath;
import dev.uchar.regelspraak.ir.Conditie;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.model.RegelSpraakModel;

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
class IfThenElseUnionWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void the_loader_builds_a_union_node_and_no_unresolved_leaf() throws Exception {
        Expr value = ruleValue(nestedModel());

        // the form sits INSIDE an ordinary node, so reaching it at all means the tree carries it — a side
        // table would have put an Onopgelost here and kept the shape elsewhere.
        Expr.Binair top = assertInstanceOf(Expr.Binair.class, value, "de waarde is de omhullende Binair");
        Expr.IfThenElse choice = assertInstanceOf(Expr.IfThenElse.class, top.links(),
                "de ch. 10.2-vorm is een unie-knoop in de boom, geen Onopgelost-drager");

        // every required field of the wire shape landed on the record (WF-069's node; the wire spells them
        // condition / then / else).
        assertNotNull(choice.type(), "type");
        assertNotNull(choice.bron(), "bron");
        assertInstanceOf(Conditie.Vergelijking.class, choice.conditie(), "de guard is de gewone conditie-unie");
        assertEquals("laag", ((Expr.AttribuutRef) choice.dan()).attribuut(), "de dan-tak");
        assertEquals("hoog", ((Expr.AttribuutRef) choice.anders()).attribuut(), "de anders-tak");

        // and nothing in this tree is unresolved any more: the last Onopgelost CARRIER is gone, so an
        // Onopgelost here would again mean a form parsed into a leaf.
        assertFalse(Expr.bevat(value, x -> x instanceof Expr.Onopgelost),
                "geen Onopgelost-blad meer in een boom die alleen een IfThenElse droeg");
    }

    @Test void a_generic_walk_descends_into_both_arms_and_into_the_guard() throws Exception {
        Expr value = ruleValue(nestedModel());

        // Expr.bevat is the generic walk every consumer's structural question is built on. Each of these three
        // reads is reachable ONLY through the ch. 10.2 node, so each answer is the arm doing its work.
        assertTrue(reads(value, "laag"), "de dan-tak wordt afgedaald");
        assertTrue(reads(value, "hoog"), "de anders-tak wordt afgedaald");
        assertTrue(reads(value, "grondslag"), "de operanden van de guard worden afgedaald");
        // the control: a name the document does not carry must still answer false, so the three above are not
        // a walk that says yes to everything.
        assertFalse(reads(value, "onbestaand"), "een niet-gelezen naam blijft onwaar");
    }

    @Test void the_walk_also_reaches_the_form_under_a_let() throws Exception {
        // ch. 4.10's three nodes nest freely: the walk must reach an IfThenElse through a Let body, which is
        // the shape the corpus goldens actually carry (a variabelendeel lowering wrapping a branch).
        Expr value = ruleValue(underLetModel());

        Expr.Let let = assertInstanceOf(Expr.Let.class, value, "de waarde is de omhullende Let");
        assertInstanceOf(Expr.IfThenElse.class, let.body(), "de body is de ch. 10.2-vorm");
        assertTrue(Expr.bevat(value, x -> x instanceof Expr.IfThenElse),
                "de generieke wandeling vindt de vorm onder een Let");
        assertTrue(reads(value, "laag"), "de dan-tak onder de Let");
        assertTrue(reads(value, "hoog"), "de anders-tak onder de Let");
        assertTrue(Expr.bevat(value, x -> x instanceof Expr.BinderRef b && "g".equals(b.naam())),
                "de binderlezing in de guard onder de Let");
    }

    // ---------------- helpers ----------------

    /** Whether the tree reads the named attribute — the generic scan, asked exactly as a consumer asks it. */
    private static boolean reads(Expr e, String attribute) {
        return Expr.bevat(e, x -> x instanceof Expr.AttribuutRef a && attribute.equals(a.attribuut()));
    }

    private static Expr ruleValue(String ir) throws Exception {
        RegelSpraakModel model = IrLoader.loadAll(JSON.readTree(ir)).model();
        assertEquals(1, model.resolvedRegels.size(), "één regel in dit document");
        return model.resolvedRegels.get(0).waarde();
    }

    // ---------------- the wire shapes ----------------

    private static String num(int dec) {
        return "{\"node\":\"Number\",\"whole\":false,\"decimals\":%d,\"sign\":\"unspecified\"}"
                .formatted(dec);
    }

    private static String attr(String name, String type) {
        return ("{\"node\":\"AttributeReference\",\"attribute\":\"%s\",\"subjectType\":\"Jutter\","
                + "\"source\":{\"snippet\":\"het %s van de Jutter\"}}")
                .formatted(name, name);
    }

    private static String lit(String value, String type) {
        return IrPath.literal(value, type, value);
    }

    private static String binary(String op, String left, String right, String type) {
        return "{\"node\":\"Binary\",\"op\":\"%s\",\"left\":%s,\"right\":%s,\"type\":%s,\"source\":{\"snippet\":\"%s\"}}"
                .formatted(op, left, right, type, op.toLowerCase(Locale.ROOT));
    }

    private static String verg(String op, String left, String right) {
        return "{\"node\":\"Comparison\",\"op\":\"%s\",\"left\":%s,\"right\":%s,\"source\":{\"snippet\":\"%s\"}}"
                .formatted(op, left, right, op.toLowerCase(Locale.ROOT));
    }

    /** NB the wire fields are condition / then / else — two of them are Java keywords, so the record names them
     *  conditie / dan / anders. */
    private static String ite(String condition, String then, String otherwise, String type) {
        return ("{\"node\":\"IfThenElse\",\"type\":%s,\"condition\":%s,\"then\":%s,\"else\":%s,"
                + "\"source\":{\"snippet\":\"als … dan … anders …\"}}").formatted(type, condition, then, otherwise);
    }

    private static String let(String binder, String boundValue, String body, String type) {
        return "{\"node\":\"Let\",\"type\":%s,\"binder\":\"%s\",\"value\":%s,\"body\":%s,\"source\":{\"snippet\":\"%s is …\"}}"
                .formatted(type, binder, boundValue, body, binder);
    }

    private static String bref(String name, String type) {
        return "{\"node\":\"BinderRef\",\"name\":\"%s\",\"source\":{\"snippet\":\"%s\"}}".formatted(name, name);
    }

    /** The ch. 10.2 node as the LEFT operand of an ordinary Binair — so the walk must pass an ordinary node
     *  first and the form cannot be found by looking at the value root alone. */
    private static String nestedModel() {
        String choice = ite(verg("greater", attr("grondslag", num(2)), lit("1.00", num(2))),
                attr("laag", num(2)), attr("hoog", num(2)), num(2));
        return doc(binary("plus", choice, lit("0.00", num(2)), num(2)));
    }

    /** The same form under a Let body, its guard reading the binder. */
    private static String underLetModel() {
        String choice = ite(verg("greater", bref("g", num(2)), lit("1.00", num(2))),
                attr("laag", num(2)), attr("hoog", num(2)), num(2));
        return doc(let("g", attr("grondslag", num(2)), choice, num(2)));
    }

    private static String doc(String value) {
        String attributes = ("{\"node\":\"Attribute\",\"name\":\"grondslag\",\"datatype\":%s},"
                + "{\"node\":\"Attribute\",\"name\":\"laag\",\"datatype\":%s},"
                + "{\"node\":\"Attribute\",\"name\":\"hoog\",\"datatype\":%s},"
                + "{\"node\":\"Attribute\",\"name\":\"uit\",\"datatype\":%s}")
                .formatted(num(2), num(2), num(2), num(2));
        String objectType = ("{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\","
                + "\"attributes\":[%s],\"characteristics\":[]}").formatted(attributes);
        String rule = IrPath.regel("uit van een jutter", "BEREKENING", attr("uit", num(2)), value, "null");
        return ("{\"format\":\"uchar-ir-v1\",\"model\":{\"objectTypes\":[%s],"
                + "\"factTypes\":[],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],"
                + "\"timelines\":[],\"dayKinds\":[]},"
                + "\"rules\":{\"rules\":[%s],\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],"
                + "\"consistencies\":[],\"uniquenesses\":[],\"factCreations\":[],"
                + "\"startPointDeterminations\":[],\"ruleGroups\":[]},\"diagnostics\":[]}")
                .formatted(objectType, rule);
    }
}
