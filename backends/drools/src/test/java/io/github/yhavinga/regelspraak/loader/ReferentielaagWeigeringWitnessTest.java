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
 * of position, and a marker's type written as {@code null}. And the reference-layer shapes every executor refuses by
 * name: a role check naming no link the subject can be in (WF-034), a fold's set source its kind cannot consume
 * (WF-093, WF-080, WF-102 (c), WF-034), a TOTAAL without a range over another type than its subject (WF-083 (c)),
 * and a characteristic read at a value position (WF-080). The fields of an
 * attribute read, a creation, a uniqueness and a characteristic check are pinned where their bindings are
 * ({@code NavigatiebindingWitnessTest}, {@code BonenveldWitnessTest}); a fold, a quantifier fold and a selection
 * without their range where those are ({@code TemporeleSetvouwBereikWitnessTest},
 * {@code KwantorvouwBereikWitnessTest}, {@code InstantieUnieWitnessTest}).
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
    /** Munt ⊑ Vondst; rule 0 counts a Jutter's vondsten over Munt, rule 1 sums their gewicht. Both bound. */
    private static final String MUNTTELLING = "jutterswet-munttelling";
    /** Rule 0 folds over the two-step path Jutter -> Boot -> Vondst. */
    private static final String BOOTBERGING = "anchor/jutterswet-bootberging";
    /** Rule 2 totals the Jutter's own timeline, a TOTAAL without a range over the subject type. */
    private static final String TEMPOTOTAAL = "jutterswet-tempototaal";

    private static final String NUMERIEK =
            "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"ONBEPAALD\"}";
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
        ObjectNode regel = IrPad.versie(ir, "resolvedRegels", 0, 0);
        String waarde = "/rules/resolvedRegels/0/versions/0/value";
        regel.set("value", JSON.readTree("{\"node\":\"DimensieCel\",\"attribuut\":" + regel.get("target")
                + ",\"labels\":[\"noord\"],\"bron\":\"de cel noord\",\"type\":" + NUMERIEK + "}"));
        onbekendVeld(ir, waarde + "/type", "DimensieCel");
        regel.set("value", JSON.readTree("{\"node\":\"VariabeleRef\",\"naam\":\"x\",\"bron\":\"x\",\"type\":"
                + NUMERIEK + "}"));
        onbekendVeld(ir, waarde + "/type", "VariabeleRef");
    }

    // ---------------- the role check is one node ----------------

    @Test void de_rolchecks_van_de_l1_vorm_zijn_onbekende_knopen() throws Exception {
        String vw = "/rules/resolvedRegels/0/versions/0/condition";
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
        String vw = "/rules/resolvedRegels/0/versions/0/condition";
        for (String veld : new String[] { "factType", "role", "negated" }) {
            ObjectNode ir = golden(POSTBEZETTING);
            ((ObjectNode) IrPad.versie(ir, "resolvedRegels", 0, 0).get("condition")).remove(veld);
            IrLoader.Documentfout e = weigert(ir, vw);
            assertTrue(e.getMessage().contains("missing field '" + veld + "'"), e.getMessage());
        }
    }

    @Test void een_veld_van_de_l1_rolcheck_is_op_een_rollidmaatschap_onbekend() throws Exception {
        String vw = "/rules/resolvedRegels/0/versions/0/condition";
        for (String veld : new String[] { "rol", "anderTeFiller", "filler", "backref", "negatie" }) {
            ObjectNode ir = golden(POSTBEZETTING);
            ObjectNode rm = (ObjectNode) IrPad.versie(ir, "resolvedRegels", 0, 0).get("condition");
            if (veld.equals("negatie")) rm.put(veld, false); else rm.put(veld, "Jutter");
            onbekendVeld(ir, vw + "/" + veld, "RoleMembership");
        }
    }

    @Test void een_tegenpartij_is_heel_of_afwezig() throws Exception {
        // IR-SPEC ch. 4.5: absent means the other role's declared filler; written, it names a declared object type
        String vw = "/rules/resolvedRegels/2/versions/0/condition";
        ObjectNode ir = golden(POSTBEZETTING);
        ObjectNode rm = (ObjectNode) IrPad.versie(ir, "resolvedRegels", 2, 0).get("condition");
        assertEquals("Strandjutter", rm.get("counterpartType").asText(), "de golden vernauwt regel 2");
        rm.putNull("counterpartType");
        IrLoader.Documentfout e = weigert(ir, vw + "/counterpartType");
        assertTrue(e.getMessage().contains("null is neither"), e.getMessage());

        rm.put("counterpartType", "Kustwacht");
        e = weigert(ir, vw + "/counterpartType");
        assertTrue(e.getMessage().contains("the counterpart type \"Kustwacht\" is no declared object type (WF-034)"),
                e.getMessage());

        rm.put("counterpartType", 2);
        e = weigert(ir, vw + "/counterpartType");
        assertTrue(e.getMessage().contains("expected a string"), e.getMessage());
    }

    // ---------------- Onbekend is no datatype ----------------

    @Test void onbekend_is_op_geen_enkele_positie_een_datatype() throws Exception {
        // a declaration (an M1 position; the refusal stands at the discriminator of the datatype the field holds)
        ObjectNode ir = golden(BAKENMEESTER);
        String attr = "/model/objectTypes/" + index(IrPad.modelLijst(ir, "objectTypes"), "name", "Bakenmeester")
                + "/attributes/" + index((ArrayNode) IrPad.objectType(ir, "Bakenmeester").get("attributes"), "name",
                "onderhoudstarief");
        IrPad.attribute(IrPad.objectType(ir, "Bakenmeester"), "onderhoudstarief").set("datatype",
                JSON.readTree(ONBEKEND));
        onbekendDatatype(ir, attr + "/datatype");

        // a derived position: the fold's own annotation
        ir = golden(BAKENMEESTER);
        ((ObjectNode) IrPad.versie(ir, "resolvedRegels", 0, 0).get("value")).set("type", JSON.readTree(ONBEKEND));
        onbekendDatatype(ir, "/rules/resolvedRegels/0/versions/0/value/type");

        // a marker's type: absent is the unknown one (IR-SPEC ch. 4.4), so no spelling of it is admitted
        ir = golden(BAKENMEESTER);
        IrPad.versie(ir, "resolvedRegels", 0, 0).set("value",
                JSON.readTree("{\"node\":\"Onopgelost\",\"type\":" + ONBEKEND + ",\"bron\":\"iets onleesbaars\"}"));
        onbekendDatatype(ir, "/rules/resolvedRegels/0/versions/0/value/type");
    }

    @Test void een_marker_zonder_type_is_onbekend_en_null_is_geen_spelling() throws Exception {
        // rule 0 computes a value, so a marker may stand there (a characteristic assignment admits only waar)
        ObjectNode ir = golden(BAKENMEESTER);
        ObjectNode regel = IrPad.versie(ir, "resolvedRegels", 0, 0);
        regel.set("value", JSON.readTree("{\"node\":\"Onopgelost\",\"bron\":\"iets onleesbaars\"}"));
        Expr w = IrLoader.loadAll(ir).model().resolvedRegels.get(0).waarde();
        assertTrue(w instanceof Expr.Onopgelost, String.valueOf(w));
        assertNull(w.type(), "een marker zonder type draagt geen type — nooit een gok");

        regel.set("value", JSON.readTree("{\"node\":\"Onopgelost\",\"type\":" + NUMERIEK + ",\"bron\":\"x\"}"));
        assertNotNull(IrLoader.loadAll(ir).model().resolvedRegels.get(0).waarde().type(), "een echt datatype blijft");

        regel.set("value", JSON.readTree("{\"node\":\"Onopgelost\",\"type\":null,\"bron\":\"x\"}"));
        IrLoader.Documentfout e = weigert(ir, "/rules/resolvedRegels/0/versions/0/value/type");
        assertTrue(e.getMessage().contains("null is neither"), e.getMessage());
    }

    // ---------------- the role check names a link of the model (WF-034) ----------------
    // Until L2's fix round each of these loaded and was then refused with a storage reason, or played: a check
    // that names no link the subject can be in holds for no instance. The sentences are the ones ref and the ASP
    // backend fault with, or the verifier's where those have none.

    @Test void een_rolcheck_over_een_onbekend_feittype_of_een_onbekende_rol_weigert() throws Exception {
        String vw = "/rules/resolvedRegels/0/versions/0/condition";
        ObjectNode ir = golden(POSTBEZETTING);
        rolcheck(ir, 0).put("factType", "wachtrooster");
        IrLoader.Documentfout e = weigert(ir, vw + "/factType");
        assertTrue(e.getMessage().contains("role check names fact type 'wachtrooster', which is not declared"),
                e.getMessage());

        ir = golden(POSTBEZETTING);
        rolcheck(ir, 0).put("role", "wachtjutters");   // the declared PLURAL names no role here
        e = weigert(ir, vw + "/role");
        assertTrue(e.getMessage().contains("'wachtjutters' is not a declared role of fact type 'wachtindeling'"),
                e.getMessage());
    }

    @Test void een_rolcheck_die_het_subject_niet_vult_weigert() throws Exception {
        // "hij een wachtjutter heeft" read with the role the OTHER end plays: a Wachtpost never fills wachtjutter
        ObjectNode ir = golden(POSTBEZETTING);
        rolcheck(ir, 0).put("role", "wachtjutter");
        IrLoader.Documentfout e = weigert(ir, "/rules/resolvedRegels/0/versions/0/condition/role");
        assertTrue(e.getMessage().contains("role check: the subject \"Wachtpost\" does not fill the role"
                + " \"wachtjutter\" of \"wachtindeling\", so no link of it can hold the subject there (WF-034)"),
                e.getMessage());
    }

    @Test void een_tegenpartij_buiten_de_vuller_van_de_andere_rol_weigert() throws Exception {
        ObjectNode ir = golden(POSTBEZETTING);
        rolcheck(ir, 2).put("counterpartType", "Wachtpost");
        IrLoader.Documentfout e = weigert(ir, "/rules/resolvedRegels/2/versions/0/condition/counterpartType");
        assertTrue(e.getMessage().contains("role check: the counterpart type \"Wachtpost\" is no subtype of"
                + " \"Jutter\", the filler the role \"wachtjutter\" declares (WF-034)"), e.getMessage());
        // ⊑ admits the declared filler itself: the check then narrows nothing, and it loads
        rolcheck(ir, 2).put("counterpartType", "Jutter");
        IrLoader.loadAll(ir);
    }

    // ---------------- the set source of a fold with a range (WF-093, WF-080, WF-102 (c), WF-034) ----------------
    // jutterswet-munttelling: rule 0 counts the Jutter's vondsten over Munt, rule 1 sums the gewicht of those munten.
    // Measured on the L2 union before the fix round: the count over an attribute PLAYED in both branches, the value
    // fold over the role name failed with a runtime error at first fire in the exec branch and did not compile in the
    // DRL-text one.

    @Test void een_telling_over_een_attribuut_van_het_elementtype_weigert() throws Exception {
        ObjectNode ir = golden(MUNTTELLING);
        bron(ir, 0).put("attribuut", "gewicht").put("objecttype", "Munt");
        IrLoader.Documentfout e = weigert(ir, "/rules/resolvedRegels/0/versions/0/value/arguments/0");
        assertTrue(e.getMessage().contains("the source of this AANTAL aggregation reads 'gewicht', a member of the"
                + " element type 'Munt': a count folds INSTANCES, so its source is a count source naming the relation's"
                + " role, never a value read per contributor (WF-093)"), e.getMessage());
    }

    @Test void een_waardevouw_over_een_rolnaam_weigert() throws Exception {
        ObjectNode ir = golden(MUNTTELLING);
        bron(ir, 1).put("attribuut", "vondsten").put("objecttype", "Jutter");
        IrLoader.Documentfout e = weigert(ir, "/rules/resolvedRegels/1/versions/0/value/arguments/0");
        assertTrue(e.getMessage().contains("value aggregation over \"vondsten\", which is no attribute of \"Munt\":"
                + " a value fold reads an attribute per contributor, and a role name is a count source only a count"
                + " folds (WF-093)"), e.getMessage());

        // a name that is neither an attribute nor a role declares nothing (WF-034)
        ir = golden(MUNTTELLING);
        bron(ir, 1).put("attribuut", "glans");
        e = weigert(ir, "/rules/resolvedRegels/1/versions/0/value/arguments/0");
        assertTrue(e.getMessage().contains("'glans' is not a declared attribute of 'Munt'"), e.getMessage());
    }

    @Test void een_rolnaam_die_het_subject_niet_bereikt_is_geen_telbron() throws Exception {
        // `aanmelder` IS a declared role, but the one the Jutter itself fills: from the fold subject it reaches no
        // relation, so under the value fold the name is no count source but an undeclared member (WF-034), as the
        // verifier's source reading judges it. Until the fix round's second pass any declared role name counted.
        ObjectNode ir = golden(MUNTTELLING);
        bron(ir, 1).put("attribuut", "aanmelder").put("objecttype", "Jutter");
        IrLoader.Documentfout e = weigert(ir, "/rules/resolvedRegels/1/versions/0/value/arguments/0");
        assertTrue(e.getMessage().contains("'aanmelder' is not a declared attribute of 'Munt': a value fold's source"
                + " reads an attribute of each contributor (WF-034)"), e.getMessage());
    }

    // ---------------- the element type of the subject's own read (WF-083 (c)) ----------------

    @Test void een_totaal_zonder_bereik_over_een_ander_type_weigert() throws Exception {
        // jutterswet-tempototaal's third rule totals the Jutter's own timeline, without a range; the twist names
        // Vondst as its element type. No executor reads that field there, so before this check the twisted
        // document PLAYED in both branches here, as it passed in ref and clingo.
        ObjectNode ir = golden(TEMPOTOTAAL);
        ObjectNode w = (ObjectNode) IrPad.versie(ir, "resolvedRegels", 2, 0).get("value");
        assertEquals("Jutter", w.get("elementType").asText(), "de golden totaliseert de eigen tijdlijn");
        w.put("elementType", "Vondst");
        IrLoader.Documentfout e = weigert(ir, "/rules/resolvedRegels/2/versions/0/value/elementType");
        assertTrue(e.getMessage().contains("the elementType 'Vondst' of a TOTAAL without a range is not the fold"
                + " subject 'Jutter': without a range the fold is the subject's own read, so its elementType must"
                + " name the subject type"), e.getMessage());
    }

    @Test void een_kenmerk_als_bron_van_een_waardevouw_weigert() throws Exception {
        ObjectNode ir = golden(MUNTTELLING);
        IrPad.objectType(ir, "Munt").withArray("characteristics").addObject().put("node", "Characteristic")
                .put("name", "gangbaar").put("kind", "BIJVOEGLIJK");
        bron(ir, 1).put("attribuut", "gangbaar");
        IrLoader.Documentfout e = weigert(ir, "/rules/resolvedRegels/1/versions/0/value/arguments/0");
        assertTrue(e.getMessage().contains("'gangbaar' is a characteristic of 'Munt', not an attribute — a"
                + " characteristic endpoint is ill-formed in an aggregation source position (WF-080)"), e.getMessage());
    }

    @Test void een_telling_over_een_pad_weigert() throws Exception {
        // anchor/jutterswet-bootberging's rule 0 folds over the two-step path Jutter -> Boot -> Vondst; as a count over
        // the reached role it names a range of two steps where a count's range is one
        ObjectNode ir = golden(BOOTBERGING);
        ObjectNode w = (ObjectNode) IrPad.versie(ir, "resolvedRegels", 0, 0).get("value");
        w.put("kind", "AANTAL");
        bron(ir, 0).put("attribuut", "geborgen vondsten").put("objecttype", "Jutter");
        IrLoader.Documentfout e = weigert(ir, "/rules/resolvedRegels/0/versions/0/value/sourcePath");
        assertTrue(e.getMessage().contains("a sourcePath is admissible only as the range of a single-argument VALUE"
                + " fold's set source — a count's range is one step, spelled sourceRole"), e.getMessage());

        // ...whatever the source reads: with a null name the source check has nothing to judge, and until the
        // advisory round the count then passed and was counted along the path (the lens measured "verwacht 2,
        // werkelijk 3" on the exec branch)
        bron(ir, 0).putNull("attribuut");
        e = weigert(ir, "/rules/resolvedRegels/0/versions/0/value/sourcePath");
        assertTrue(e.getMessage().contains("a count's range is one step, spelled sourceRole"), e.getMessage());
    }

    // ---------------- a characteristic at a value position (WF-080) ----------------

    @Test void een_kenmerk_op_een_waardepositie_weigert() throws Exception {
        // "hij bemand gelijk is aan onwaar": the characteristic read as an operand. Measured on the L2 union before
        // the fix round: PLAYED in both branches, where the verifier refuses it.
        ObjectNode ir = golden(POSTBEZETTING);
        IrPad.versie(ir, "resolvedRegels", 1, 0).set("condition", JSON.readTree(
                "{\"node\":\"Vergelijking\",\"op\":\"GELIJK\",\"links\":{\"node\":\"AttribuutRef\",\"attribuut\":"
                + "\"bemand\",\"objecttype\":\"Wachtpost\",\"bron\":\"bemand\"},\"rechts\":{\"node\":\"Literal\","
                + "\"value\":{\"boolean\":false},\"type\":{\"node\":\"Booleaan\"},\"bron\":\"onwaar\"},\"periode\":null,"
                + "\"bron\":\"hij bemand gelijk is aan onwaar\"}"));
        IrLoader.Documentfout e = weigert(ir, "/rules/resolvedRegels/1/versions/0/condition/links");
        assertTrue(e.getMessage().contains("'bemand' is a characteristic of 'Wachtpost', not an attribute — a"
                + " characteristic endpoint is ill-formed in a value position (WF-080)"), e.getMessage());
    }

    // ---------------- fixtures ----------------

    private static ObjectNode rolcheck(ObjectNode ir, int regel) {
        return (ObjectNode) IrPad.versie(ir, "resolvedRegels", regel, 0).get("condition");
    }

    /** The single argument of rule {@code regel}'s fold: its set source. */
    private static ObjectNode bron(ObjectNode ir, int regel) {
        return (ObjectNode) IrPad.versie(ir, "resolvedRegels", regel, 0).get("value").get("arguments").get(0);
    }

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
        IrPad.versie(ir, "resolvedRegels", regel, 0).set("condition", JSON.readTree(json));
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

    /** The shared loader's refusal of the node at the datatype position {@code plek}: at its discriminator, in its
     *  sentence. */
    private static void onbekendDatatype(JsonNode ir, String plek) {
        IrLoader.Documentfout e = weigert(ir, plek + "/node");
        assertTrue(e.getMessage().endsWith(": unknown datatype node \"Onbekend\""), e.getMessage());
    }

    private static ObjectNode golden(String naam) throws Exception {
        Path ir = CORPUS.resolve(naam).resolve("ir.json");
        assertTrue(Files.exists(ir), "golden ir.json present: " + ir.toAbsolutePath());
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(ir));
        IrLoader.loadAll(doc);   // the untwisted golden loads, so the refusal below is the twist's alone
        return doc;
    }
}
