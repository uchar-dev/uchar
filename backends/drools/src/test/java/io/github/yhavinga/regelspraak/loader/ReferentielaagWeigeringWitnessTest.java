package io.github.yhavinga.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.yhavinga.regelspraak.cases.IrPad;
import io.github.yhavinga.regelspraak.ir.Expr;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The loader's refusals of the reference layer in the shape L2 of the v1 wire migration removed: a type written on a
 * reference, the two role-check nodes and their fields beside {@code RoleMembership}, a {@code RoleMembership}
 * without its fact type or role, a narrowing written as {@code null}, the datatype {@code Onbekend} at every kind
 * of position, and a marker's type written as {@code null}. The fields of an attribute read, a creation, a
 * uniqueness and a characteristic check are pinned where their bindings are ({@code NavigatiebindingWitnessTest},
 * {@code BonenveldWitnessTest}); a fold, a quantifier fold and a selection without their range where those are
 * ({@code TemporeleSetvouwBereikWitnessTest}, {@code KwantorvouwBereikWitnessTest}, {@code InstantieUnieWitnessTest}).
 *
 * <p>Every fixture is a corpus golden with ONE twist in memory; the corpus file is read and never written. Each
 * refusal is an {@link IrLoader.Documentfout} at a JSON pointer, never an {@link IrLoader.Refusal}: these are not
 * forms this backend declines but documents that are not {@code uchar-ir-v1}, and a malformed document filed in the
 * gate's auto-excluded channel would leave it green over a document nobody read.
 */
class ReferentielaagWeigeringWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** One parameter, vondstdrempel, read in a rule. Both branches PLAYED. */
    private static final String PENNINGDREMPEL = "jutterswet-penningdrempel";
    /** A Let whose body reads its binder. Both branches PLAYED. */
    private static final String BERGINGSTOESLAG = "jutterswet-bergingstoeslag";
    /** Four role checks over wachtindeling; rules 2 and 3 narrow the other end to Strandjutter. */
    private static final String POSTBEZETTING = "jutterswet-postbezetting";
    /** Temporal attributes on Bakenmeester; rule 0 computes a value, a fold with a type of its own. */
    private static final String BAKENMEESTER = "jutterswet-bakenmeester";

    private static final String NUMERIEK =
            "{\"node\":\"Numeriek\",\"geheel\":false,\"decimalen\":2,\"teken\":\"ONBEPAALD\",\"eenheid\":null}";
    private static final String ONBEKEND = "{\"node\":\"Onbekend\",\"reden\":\"niet opgelost\"}";

    // ---------------- the reference nodes carry no type ----------------

    @Test void een_type_op_een_verwijzing_wordt_bij_naam_geweigerd() throws Exception {
        // the attribute read has its own row with its four other fields (NavigatiebindingWitnessTest)
        ObjectNode ir = golden(PENNINGDREMPEL);
        Treffer p = vind(ir, "ParameterRef");
        p.knoop().set("type", JSON.readTree(NUMERIEK));
        onbekendVeld(ir, p.pad() + "/type", "ParameterRef");

        ir = golden(BERGINGSTOESLAG);
        Treffer b = vind(ir, "BinderRef");
        b.knoop().set("type", JSON.readTree(NUMERIEK));
        onbekendVeld(ir, b.pad() + "/type", "BinderRef");

        // no replaying golden reads a dimension cell or a variable, so each stands in for rule 0's value
        ir = golden(PENNINGDREMPEL);
        ObjectNode regel = (ObjectNode) IrPad.rulesLijst(ir, "resolvedRegels").get(0);
        String waarde = "/rules/resolvedRegels/0/waarde";
        regel.set("waarde", JSON.readTree("{\"node\":\"DimensieCel\",\"attribuut\":" + regel.get("doel")
                + ",\"labels\":[\"noord\"],\"bron\":\"de cel noord\",\"type\":" + NUMERIEK + "}"));
        onbekendVeld(ir, waarde + "/type", "DimensieCel");
        regel.set("waarde", JSON.readTree("{\"node\":\"VariabeleRef\",\"naam\":\"x\",\"bron\":\"x\",\"type\":"
                + NUMERIEK + "}"));
        onbekendVeld(ir, waarde + "/type", "VariabeleRef");
    }

    // ---------------- the role check is one node ----------------

    @Test void de_rolchecks_van_de_l1_vorm_zijn_onbekende_knopen() throws Exception {
        String vw = "/rules/resolvedRegels/0/voorwaarde";
        ObjectNode ir = golden(POSTBEZETTING);
        voorwaarde(ir, 0, "{\"node\":\"Rollidmaatschap\",\"rol\":\"wachtjutter\",\"anderTeFiller\":\"Jutter\","
                + "\"negatie\":false,\"bron\":\"hij een wachtjutter heeft\"}");
        IrLoader.Documentfout e = weigert(ir, vw + "/node");
        assertTrue(e.getMessage().contains("unknown condition node \"Rollidmaatschap\""), e.getMessage());

        voorwaarde(ir, 0, "{\"node\":\"VerreRollidmaatschap\",\"filler\":\"Jutter\",\"backref\":"
                + "\"toegewezen wachtpost\",\"negatie\":false,\"bron\":\"hij een wachtjutter heeft\"}");
        e = weigert(ir, vw + "/node");
        assertTrue(e.getMessage().contains("unknown condition node \"VerreRollidmaatschap\""), e.getMessage());
    }

    @Test void een_rollidmaatschap_draagt_feittype_rol_en_negatie() throws Exception {
        String vw = "/rules/resolvedRegels/0/voorwaarde";
        for (String veld : new String[] { "factType", "role", "negated" }) {
            ObjectNode ir = golden(POSTBEZETTING);
            ((ObjectNode) IrPad.rulesLijst(ir, "resolvedRegels").get(0).get("voorwaarde")).remove(veld);
            IrLoader.Documentfout e = weigert(ir, vw);
            assertTrue(e.getMessage().contains("missing field '" + veld + "'"), e.getMessage());
        }
    }

    @Test void een_veld_van_de_l1_rolcheck_is_op_een_rollidmaatschap_onbekend() throws Exception {
        String vw = "/rules/resolvedRegels/0/voorwaarde";
        for (String veld : new String[] { "rol", "anderTeFiller", "filler", "backref", "negatie" }) {
            ObjectNode ir = golden(POSTBEZETTING);
            ObjectNode rm = (ObjectNode) IrPad.rulesLijst(ir, "resolvedRegels").get(0).get("voorwaarde");
            if (veld.equals("negatie")) rm.put(veld, false); else rm.put(veld, "Jutter");
            onbekendVeld(ir, vw + "/" + veld, "RoleMembership");
        }
    }

    @Test void een_tegenpartij_is_heel_of_afwezig() throws Exception {
        // IR-SPEC ch. 4.5: absent means the other role's declared filler; written, it names a declared object type
        String vw = "/rules/resolvedRegels/2/voorwaarde";
        ObjectNode ir = golden(POSTBEZETTING);
        ObjectNode rm = (ObjectNode) IrPad.rulesLijst(ir, "resolvedRegels").get(2).get("voorwaarde");
        assertEquals("Strandjutter", rm.get("counterpartType").asText(), "de golden vernauwt regel 2");
        rm.putNull("counterpartType");
        IrLoader.Documentfout e = weigert(ir, vw + "/counterpartType");
        assertTrue(e.getMessage().contains("null is neither"), e.getMessage());

        rm.put("counterpartType", "Kustwacht");
        e = weigert(ir, vw + "/counterpartType");
        assertTrue(e.getMessage().contains("'Kustwacht' is not a declared object type"), e.getMessage());

        rm.put("counterpartType", 2);
        e = weigert(ir, vw + "/counterpartType");
        assertTrue(e.getMessage().contains("expected a string"), e.getMessage());
    }

    // ---------------- Onbekend is no datatype ----------------

    @Test void onbekend_is_op_geen_enkele_positie_een_datatype() throws Exception {
        // a declaration (an M1 position, with the pointer of the field that holds it)
        ObjectNode ir = golden(BAKENMEESTER);
        String attr = "/model/objectTypes/" + index(IrPad.modelLijst(ir, "objectTypes"), "name", "Bakenmeester")
                + "/attributes/" + index((ArrayNode) IrPad.objectType(ir, "Bakenmeester").get("attributes"), "name",
                "onderhoudstarief");
        IrPad.attribute(IrPad.objectType(ir, "Bakenmeester"), "onderhoudstarief").set("datatype",
                JSON.readTree(ONBEKEND));
        onbekendDatatype(ir, attr + "/datatype");

        // a derived position: the fold's own annotation
        ir = golden(BAKENMEESTER);
        ((ObjectNode) IrPad.rulesLijst(ir, "resolvedRegels").get(0).get("waarde")).set("type", JSON.readTree(ONBEKEND));
        onbekendDatatype(ir, "/rules/resolvedRegels/0/waarde/type");

        // a marker's type: absent is the unknown one (IR-SPEC ch. 4.4), so no spelling of it is admitted
        ir = golden(BAKENMEESTER);
        ((ObjectNode) IrPad.rulesLijst(ir, "resolvedRegels").get(0)).set("waarde",
                JSON.readTree("{\"node\":\"Onopgelost\",\"type\":" + ONBEKEND + ",\"bron\":\"iets onleesbaars\"}"));
        onbekendDatatype(ir, "/rules/resolvedRegels/0/waarde/type");
    }

    @Test void een_marker_zonder_type_is_onbekend_en_null_is_geen_spelling() throws Exception {
        // rule 0 computes a value, so a marker may stand there (a characteristic assignment admits only waar)
        ObjectNode ir = golden(BAKENMEESTER);
        ObjectNode regel = (ObjectNode) IrPad.rulesLijst(ir, "resolvedRegels").get(0);
        regel.set("waarde", JSON.readTree("{\"node\":\"Onopgelost\",\"bron\":\"iets onleesbaars\"}"));
        Expr w = IrLoader.loadAll(ir).model().resolvedRegels.get(0).waarde();
        assertTrue(w instanceof Expr.Onopgelost, String.valueOf(w));
        assertNull(w.type(), "een marker zonder type draagt geen type — nooit een gok");

        regel.set("waarde", JSON.readTree("{\"node\":\"Onopgelost\",\"type\":" + NUMERIEK + ",\"bron\":\"x\"}"));
        assertNotNull(IrLoader.loadAll(ir).model().resolvedRegels.get(0).waarde().type(), "een echt datatype blijft");

        regel.set("waarde", JSON.readTree("{\"node\":\"Onopgelost\",\"type\":null,\"bron\":\"x\"}"));
        IrLoader.Documentfout e = weigert(ir, "/rules/resolvedRegels/0/waarde/type");
        assertTrue(e.getMessage().contains("null is neither"), e.getMessage());
    }

    // ---------------- fixtures ----------------

    /** A node of the given kind and its JSON pointer. */
    private record Treffer(ObjectNode knoop, String pad) {}

    /** The FIRST node of kind {@code soort} under the rules, in document order; the golden must carry one. */
    private static Treffer vind(JsonNode ir, String soort) {
        Treffer t = vind(ir.get("rules"), "/rules", soort);
        assertNotNull(t, "de golden draagt geen " + soort);
        return t;
    }

    private static Treffer vind(JsonNode n, String pad, String soort) {
        if (n instanceof ObjectNode o) {
            if (soort.equals(o.path("node").asText(null))) return new Treffer(o, pad);
            for (var it = o.properties().iterator(); it.hasNext(); ) {
                var e = it.next();
                Treffer t = vind(e.getValue(), pad + "/" + e.getKey().replace("~", "~0").replace("/", "~1"), soort);
                if (t != null) return t;
            }
        } else if (n instanceof ArrayNode a) {
            for (int i = 0; i < a.size(); i++) {
                Treffer t = vind(a.get(i), pad + "/" + i, soort);
                if (t != null) return t;
            }
        }
        return null;
    }

    private static void voorwaarde(ObjectNode ir, int regel, String json) throws Exception {
        ((ObjectNode) IrPad.rulesLijst(ir, "resolvedRegels").get(regel)).set("voorwaarde", JSON.readTree(json));
    }

    private static int index(ArrayNode lijst, String naamVeld, String naam) {
        for (int i = 0; i < lijst.size(); i++) if (naam.equals(lijst.get(i).path(naamVeld).asText(null))) return i;
        throw new AssertionError("geen " + naam + " in de lijst");
    }

    private static IrLoader.Documentfout weigert(JsonNode ir, String plek) {
        IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.loadAll(ir),
                "verwacht een documentfout op " + plek);
        assertEquals(plek, e.plek(), e.getMessage());
        return e;
    }

    private static void onbekendVeld(JsonNode ir, String plek, String knoop) {
        IrLoader.Documentfout e = weigert(ir, plek);
        assertTrue(e.getMessage().contains("unknown field on " + knoop), e.getMessage());
    }

    private static void onbekendDatatype(JsonNode ir, String plek) {
        IrLoader.Documentfout e = weigert(ir, plek);
        assertTrue(e.getMessage().contains("unknown datatype node \"Onbekend\""), e.getMessage());
    }

    private static ObjectNode golden(String naam) throws Exception {
        Path ir = CORPUS.resolve(naam).resolve("ir.json");
        assertTrue(Files.exists(ir), "golden ir.json present: " + ir.toAbsolutePath());
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(ir));
        IrLoader.loadAll(doc);   // the untwisted golden loads, so the refusal below is the twist's alone
        return doc;
    }
}
