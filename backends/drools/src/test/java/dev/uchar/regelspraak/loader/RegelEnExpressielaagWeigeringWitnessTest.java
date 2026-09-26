package dev.uchar.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.cases.IrPad;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.ResolvedFeitCreatie;
import dev.uchar.regelspraak.ir.ResolvedRegel;
import dev.uchar.regelspraak.ir.ResolvedUniciteit;
import dev.uchar.regelspraak.ir.ResolvedUniciteitUnie;
import dev.uchar.regelspraak.ir.Spelling;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.RolBindingRef;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The loader on the rule and expression layers of the wire (L4 of the v1 wire migration): what it refuses by name, and
 * how it lays the wire back onto the frontend records. Refused: the rule-layer nodes of the L3 shape, their fields and
 * the list of unions; a method, presentation tag, function kind or granularity outside its set, a kind of the other
 * function node included; a literal value that is not exactly one variant of the case format; an empty list of
 * versions, sources, keys or hops, and a union with a scope; a form name that spells a version; a target that is no
 * attribute reference; a presentation tag beside an initialisation; a context node without its name or datatype — in
 * the words of the shared OCaml loader. And, as the shared loader refuses them only when it reads for an executor: a
 * literal whose variant is not the one its type names (WF-114's sentence) and a context value outside the two names
 * (WF-098's); beside them a number, a date or a date-time outside its grammar (WF-081's, WF-084's and WF-095's
 * sentences; ref and the ASP backend fault when they read such a lexeme), a granularity on a builtin that does not
 * bucket (WF-037's), the member-kind arm and the name that is both an attribute and a characteristic (WF-022's
 * sentences, which need the model the shared loader does not consult) and a fact creation whose anchor or target role
 * is no role of the created relation (WF-028's). Read: every version of a named form as one
 * record, named as the frontend names a version of a form with several; the kind of a rule version from its
 * structure; a fact creation's target type as the filler of its target role and its path as its steps and its join;
 * the one list of uniquenesses split on the number of sources; both function nodes as the one function record; a
 * literal's variant as the lexeme the record holds; the bound day and a session value as the one constant record.
 *
 * <p>Every fixture is a corpus golden with ONE twist in memory; the corpus file is read and never written. Each
 * refusal is an {@link IrLoader.Documentfout} at a JSON pointer, never an {@link IrLoader.Refusal}: every twisted
 * document here is ill-formed.
 */
class RegelEnExpressielaagWeigeringWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** Rule 0 computes an attribute of a Vondst (calculation) from a Number literal; rule 1 has a period condition. */
    private static final String PREMIEVENSTER = "jutterswet-premievenster";
    /** Rule 0 attributes the characteristic penninggerechtigd of a Jutter, under a condition. */
    private static final String PENNINGDREMPEL = "jutterswet-penningdrempel";
    /** Rule 0 is one form of two versions; rule 1 a form of one. */
    private static final String JUTHEFFING = "anchor/jutterswet-jutheffing";
    /** Rule 1 carries two statute annotations. */
    private static final String HARDHEIDSOVERSCHOT = "jutterswet-hardheidsoverschot";
    /** Rule 1 is an initialisation. */
    private static final String WAARDERING = "anchor/jutterswet-waardering-v0vorm";
    /** Rule 3 writes an enumeration literal under its Enumeration type. */
    private static final String KEURSTAAT = "jutterswet-keurstaat";
    /** Allocation 0, with every optional field but an ordering attribute. */
    private static final String BERGING = "jutterswet-berging";
    /** Object creation 0 with its connecting role, and consistency 0. */
    private static final String WACHTERSKETEN = "anchor/jutterswet-wachtersketen";
    /** Fact creation 0 over a path of three hops. */
    private static final String MASTSEIN = "jutterswet-mastsein";
    /** Uniquenesses 0 and 1 of one source, each scoped; 2 and 3 of two sources. */
    private static final String KUSTREGISTER = "anchor/jutterswet-kustregister";
    /** Rule 0's value is a timeProportionalPart with a granularity and a filter. */
    private static final String PACHTVENSTER = "jutterswet-pachtvenster";
    /** Rule 0's value is a yearOf over a date attribute, without a granularity. */
    private static final String PAASVONDST = "jutterswet-paasvondst";
    /** Rule 0's value is a minimum over a two-step path; rule 1's a sum with an empty fallback. */
    private static final String BOOTBERGING = "anchor/jutterswet-bootberging";
    /** Day kind 0 compares the bound day. */
    private static final String HAVENROOSTER = "jutterswet-havenrooster";
    /** Jutter.toelage is an attribute and a characteristic; no rule writes it. */
    private static final String TOELAGEGRONDEN = "anchor/jutterswet-toelagegronden";

    private static final String REGEL = "/rules/rules/0";
    private static final String VERSIE = REGEL + "/versions/0";
    private static final String WAARDE = VERSIE + "/value";
    private static final String BOUNDDAY = "/rules/dayKindDefinitions/0/condition/parts/0/left";

    private static final String NUMBER = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";
    private static final String DAG = "{\"node\":\"Date\",\"precision\":\"day\"}";

    // ---------------- the L3 shape is unknown by name ----------------

    @Test void de_knopen_van_de_l3_vorm_zijn_onbekend() throws Exception {
        String[][] vormen = {
                { PENNINGDREMPEL, "/rules/rules/0", "ResolvedRegel", "Rule" },
                { BERGING, "/rules/allocations/0", "ResolvedVerdeling", "Allocation" },
                { WACHTERSKETEN, "/rules/objectCreations/0", "ResolvedObjectCreatie", "ObjectCreation" },
                { MASTSEIN, "/rules/factCreations/0", "ResolvedFeitCreatie", "FactCreation" },
                { WACHTERSKETEN, "/rules/consistencies/0", "ResolvedConsistentie", "Consistency" },
                { KUSTREGISTER, "/rules/uniquenesses/0", "ResolvedUniciteit", "Uniqueness" },
                { KUSTREGISTER, "/rules/uniquenesses/2", "ResolvedUniciteitUnie", "Uniqueness" },
                // a step of the path and a source of a union, the two child nodes the L4 shape replaced
                { MASTSEIN, "/rules/factCreations/0/versions/0/path/0", "Tussenstap", "RoleBinding" },
                { KUSTREGISTER, "/rules/uniquenesses/2/versions/0/sources/0", "Bron", "Source" } };
        for (String[] v : vormen) {
            ObjectNode ir = golden(v[0]);
            ((ObjectNode) ir.at(v[1])).put("node", v[2]);
            assertTrue(weigert(ir, v[1] + "/node").getMessage().contains("expected \"" + v[3] + "\", got \"" + v[2]
                    + "\""), v[2]);
        }
        // the two expression nodes the L4 shape split, in the shared loader's sentence
        ObjectNode ir = golden(PREMIEVENSTER);
        ((ObjectNode) ir.at(WAARDE)).put("node", "Functie");
        assertTrue(weigert(ir, WAARDE + "/node").getMessage().endsWith(": unknown expr node \"Functie\""));
        ir = golden(HAVENROOSTER);
        ((ObjectNode) ir.at(BOUNDDAY)).put("node", "Constante");
        assertTrue(weigert(ir, BOUNDDAY + "/node").getMessage().endsWith(": unknown expr node \"Constante\""));
        // and the second list of uniquenesses, which the one list replaced
        ir = golden(PENNINGDREMPEL);
        IrPad.rules(ir).putArray("uniciteitUnies");
        assertTrue(weigert(ir, "/rules/uniciteitUnies").getMessage().contains("unknown field"));
    }

    /** Every key of the L3 shape on the node or version record that replaced it — and a version record, which carries
     *  no discriminator, refuses one as it refuses any key it does not have. */
    @Test void de_velden_van_de_l3_vorm_zijn_onbekende_velden() throws Exception {
        Map<String[], List<String>> rijen = new LinkedHashMap<>();
        rijen.put(new String[] { PENNINGDREMPEL, REGEL, "Rule" }, List.of("naam", "bronAnnotaties"));
        rijen.put(new String[] { PENNINGDREMPEL, VERSIE, "RuleVersion" },
                List.of("node", "naam", "soort", "doel", "waarde", "voorwaarde", "geldigheid", "bronAnnotaties"));
        rijen.put(new String[] { BERGING, "/rules/allocations/0/versions/0", "AllocationVersion" },
                List.of("node", "bronType", "bronAttribuut", "doelType", "doelAttribuut", "methode", "ratoAttribuut",
                        "maximumAttribuut", "afrondingDecimalen", "restantAttribuut", "groepeerAttribuut", "afnemend",
                        "verbindendeRol", "voorwaarde", "geldigheid"));
        rijen.put(new String[] { WACHTERSKETEN, "/rules/objectCreations/0/versions/0", "ObjectCreationVersion" },
                List.of("node", "nieuwType", "toekenningen", "verbindendeRol", "voorwaarde", "geldigheid"));
        rijen.put(new String[] { MASTSEIN, "/rules/factCreations/0/versions/0", "FactCreationVersion" },
                List.of("node", "feittype", "ankerRol", "doelRol", "anker", "tussenstappen", "joinRol", "doel",
                        "voorwaarde", "geldigheid"));
        rijen.put(new String[] { WACHTERSKETEN, "/rules/consistencies/0/versions/0", "ConsistencyVersion" },
                List.of("node", "assertie", "geldigheid"));
        rijen.put(new String[] { KUSTREGISTER, "/rules/uniquenesses/0/versions/0", "UniquenessVersion" },
                List.of("node", "sleutels", "scopeRol", "bronnen", "subject", "geldigheid"));
        rijen.put(new String[] { KUSTREGISTER, "/rules/uniquenesses/2/versions/0/sources/0", "Source" },
                List.of("type", "sleutels"));
        // the fold's own fields, and the builtin's facet, which it does not carry
        rijen.put(new String[] { BOOTBERGING, WAARDE, "Aggregation" },
                List.of("argumenten", "elementObjecttype", "leegwaarde", "dimensieSelectie", "bronRol", "bronPad",
                        "granulariteit", "granularity"));
        // the builtin's own fields, and the five fold fields, which it does not carry
        rijen.put(new String[] { PACHTVENSTER, WAARDE, "BuiltinCall" },
                List.of("argumenten", "granulariteit", "elementType", "emptyFallback", "dimensionSelection",
                        "sourceRole", "sourcePath"));
        rijen.put(new String[] { PREMIEVENSTER, WAARDE, "Literal" }, List.of("waarde"));
        rijen.put(new String[] { HAVENROOSTER, BOUNDDAY, "BoundDay" }, List.of("name", "naam"));
        for (Map.Entry<String[], List<String>> r : rijen.entrySet())
            for (String veld : r.getValue()) {
                ObjectNode ir = golden(r.getKey()[0]);
                ((ObjectNode) ir.at(r.getKey()[1])).put(veld, "x");
                onbekendVeld(ir, r.getKey()[1] + "/" + veld, r.getKey()[2]);
            }
        // a session value, which no loadable golden carries, at a rule's value
        ObjectNode ir = golden(PREMIEVENSTER);
        zet(ir, WAARDE, contextwaarde("evaluationDate").put("naam", "x").toString());
        onbekendVeld(ir, WAARDE + "/naam", "ContextValue");
    }

    // ---------------- the closed sets ----------------

    @Test void methode_tag_soort_en_granulariteit_zijn_gesloten_verzamelingen() throws Exception {
        for (String methode : List.of("COMPLEX", "naar_rato")) {
            ObjectNode ir = golden(BERGING);
            ((ObjectNode) ir.at("/rules/allocations/0/versions/0")).put("method", methode);
            assertTrue(weigert(ir, "/rules/allocations/0/versions/0/method").getMessage()
                    .endsWith(": unknown method value \"" + methode + "\""), methode);
        }
        ObjectNode ir = golden(PREMIEVENSTER);
        ((ObjectNode) ir.at(VERSIE)).put("presentation", "KENMERKTOEKENNING");
        assertTrue(weigert(ir, VERSIE + "/presentation").getMessage()
                .endsWith(": unknown presentation value \"KENMERKTOEKENNING\""));
        // a kind of the other function node is as unknown as a kind of none
        String[][] soorten = { { BOOTBERGING, "squareRoot" }, { BOOTBERGING, "GEMIDDELDE" }, { PACHTVENSTER, "sum" },
                { PACHTVENSTER, "filledCount" } };
        for (String[] s : soorten) {
            ir = golden(s[0]);
            ((ObjectNode) ir.at(WAARDE)).put("kind", s[1]);
            assertTrue(weigert(ir, WAARDE + "/kind").getMessage().endsWith(": unknown kind value \"" + s[1] + "\""),
                    s[1]);
        }
        ir = golden(PACHTVENSTER);
        ((ObjectNode) ir.at(WAARDE)).put("granularity", "UUR");
        assertTrue(weigert(ir, WAARDE + "/granularity").getMessage().endsWith(": unknown granularity value \"UUR\""));
    }

    // ---------------- the literal value ----------------

    @Test void een_literaalwaarde_is_precies_een_variant() throws Exception {
        List<String> vormen = List.of("\"1\"", "1", "null", "{}", "{\"number\":1}", "{\"number\":\"1\",\"text\":\"1\"}",
                "{\"getal\":\"1\"}", "{\"boolean\":\"waar\"}", "{\"constructor\":\"Wad\",\"payload\":{}}");
        for (String vorm : vormen) {
            ObjectNode ir = golden(PREMIEVENSTER);
            zet(ir, WAARDE + "/value", vorm);
            IrLoader.Documentfout e = weigert(ir, WAARDE + "/value");
            assertTrue(e.getMessage().endsWith(": " + IrLoader.LITERAALWAARDE), vorm + ": " + e.getMessage());
        }
    }

    /**
     * WF-114, with the verifier's table: the variant a type names — a percentage is a Number — and the article before
     * the variant, "an" before enumeration alone. Every variant under every type this backend carries; the one that
     * matches loads, every other is refused at the value.
     */
    @Test void de_variant_is_die_welke_het_type_noemt() throws Exception {
        String enumeratie = golden(KEURSTAAT).at("/rules/rules/3/versions/0/value/type").toString();
        String procent = golden("jutterswet-vondsttarief").at(WAARDE + "/type").toString();
        // type, the phrase the sentence gives it, the variant it names
        String[][] typen = {
                { NUMBER, "a Number", "number" },
                { procent, "a Number", "number" },
                { "{\"node\":\"Boolean\"}", "a Boolean", "boolean" },
                { "{\"node\":\"Text\"}", "a Text", "text" },
                { DAG, "a day-precision Date", "date" },
                { "{\"node\":\"Date\",\"precision\":\"millisecond\"}", "a millisecond-precision Date", "dateTime" },
                { enumeratie, "an Enumeration", "enumeration" } };
        Map<String, String> varianten = new LinkedHashMap<>();
        varianten.put("number", "{\"number\":\"5\"}");
        varianten.put("boolean", "{\"boolean\":true}");
        varianten.put("text", "{\"text\":\"gaaf\"}");
        varianten.put("date", "{\"date\":\"2024-01-31\"}");
        varianten.put("dateTime", "{\"dateTime\":\"2024-01-31T12:00:00\"}");
        varianten.put("enumeration", "{\"enumeration\":\"gaaf\"}");
        varianten.put("constructor", "{\"constructor\":\"Wad\"}");
        String w = "/rules/rules/3/versions/0/value";
        int geladen = 0, geweigerd = 0;
        for (String[] t : typen)
            for (Map.Entry<String, String> v : varianten.entrySet()) {
                ObjectNode ir = golden(KEURSTAAT);
                zet(ir, w + "/type", t[0]);
                zet(ir, w + "/value", v.getValue());
                if (v.getKey().equals(t[2])) {
                    assertNotNull(IrLoader.load(ir), t[1] + " / " + v.getKey());
                    geladen++;
                    continue;
                }
                IrLoader.Documentfout e = weigert(ir, w + "/value");
                String zin = "literal value is " + (v.getKey().equals("enumeration") ? "an" : "a") + " " + v.getKey()
                        + " value but its type is " + t[1] + ": the variant is the one the type names";
                assertTrue(e.getMessage().endsWith(": " + zin), zin + " / " + e.getMessage());
                geweigerd++;
            }
        assertEquals(7, geladen);
        assertEquals(42, geweigerd);
        // a literal without a type is WF-034's typeless value node: nothing to cross, so it loads
        ObjectNode ir = golden(KEURSTAAT);
        ((ObjectNode) ir.at(w)).putNull("type");
        zet(ir, w + "/value", "{\"text\":\"5\"}");
        assertNotNull(IrLoader.load(ir));
    }

    /** WF-081's sentence, over the case format's number grammar: a decimal comma, a point without a digit before it,
     *  a unit in the lexeme and a denominator with a leading zero are no number. Before this check both branches read
     *  each of them as some other number (.5 and -.5 as 5, 3/04 as 0.75), where ref and the ASP backend fault. */
    @Test void een_getal_volgt_de_getalgrammatica_van_het_casusformaat() throws Exception {
        for (String lexeem : List.of("12,50", ".5", "-.5", "3/04", "5 €", "1.", "+1", "1e3", "1/0", "")) {
            ObjectNode ir = golden(PREMIEVENSTER);
            zet(ir, WAARDE + "/value", "{\"number\":\"" + lexeem + "\"}");
            assertTrue(weigert(ir, WAARDE + "/value").getMessage().endsWith(": numeric literal lexeme '" + lexeem
                    + "' is not a number of the case format (an optional '-', digits, an optional '.' and digits, or a"
                    + " rational a/b whose denominator has no leading zero — ch. 4.1)"), lexeem);
        }
        for (String lexeem : List.of("12.50", "-3", "0", "1/3", "-2/7", "007")) {
            ObjectNode ir = golden(PREMIEVENSTER);
            zet(ir, WAARDE + "/value", "{\"number\":\"" + lexeem + "\"}");
            assertEquals(lexeem, ((Expr.Literal) IrLoader.load(ir).resolvedRegels.get(0).waarde()).waarde());
        }
    }

    /** WF-084's sentence, over the verifier's date grammar: fixed four-two-two digit fields and a day of the calendar.
     *  Before this check the two witnesses' lexemes loaded and both branches refused the rule only while lowering, under
     *  a scope note ("regel (waarde buiten scope)", "TODO datumliteral"), where the verifier names the lexeme. */
    @Test void een_datum_volgt_de_iso_vorm() throws Exception {
        for (String lexeem : List.of("31-01-2024", "dd. 31-01-2024", "2024-1-31", "+2024-01-31", "2024-02-30",
                "2023-02-29", "1900-02-29", "2024-13-01", "2024-01-31T00:00:00", "")) {
            ObjectNode ir = golden(PREMIEVENSTER);
            zet(ir, WAARDE + "/type", DAG);
            zet(ir, WAARDE + "/value", "{\"date\":\"" + lexeem + "\"}");
            assertTrue(weigert(ir, WAARDE + "/value").getMessage().endsWith(": date literal lexeme '" + lexeem
                    + "' is not the ISO form yyyy-mm-dd (fixed field widths, valid ranges — ch. 4.1)"), lexeem);
        }
        for (String lexeem : List.of("2024-01-31", "2024-02-29", "2000-02-29")) {
            ObjectNode ir = golden(PREMIEVENSTER);
            zet(ir, WAARDE + "/type", DAG);
            zet(ir, WAARDE + "/value", "{\"date\":\"" + lexeem + "\"}");
            assertEquals(lexeem, ((Expr.Literal) IrLoader.load(ir).resolvedRegels.get(0).waarde()).waarde());
        }
    }

    /** WF-095's sentence, over the verifier's date-time grammar: the date of WF-084, a 'T', the seconds always written,
     *  a fraction of exactly three digits and nothing after it. The first six lexemes are the six WF-095 witnesses'. */
    @Test void een_datumtijd_volgt_de_canonieke_iso_vorm() throws Exception {
        for (String lexeem : List.of("15-03-2026 12:00:00.000", "2026-03-15T12:00:00.1", "rommel 2026-03-15T12:00:00 x",
                "2026-03-15T12:00", "2026-03-15 12:00:00", "2026-03-15T12:00:00Z", "2026-03-15T12:00:00+01:00",
                "2026-03-15T24:00:00", "2026-03-15T12:60:00", "2026-03-15T12:00:60", "2026-02-30T12:00:00",
                "2026-03-15")) {
            ObjectNode ir = golden(PREMIEVENSTER);
            zet(ir, WAARDE + "/type", "{\"node\":\"Date\",\"precision\":\"millisecond\"}");
            zet(ir, WAARDE + "/value", "{\"dateTime\":\"" + lexeem + "\"}");
            assertTrue(weigert(ir, WAARDE + "/value").getMessage().endsWith(": millisecond-precision date literal lexeme '"
                    + lexeem + "' is not the canonical ISO form yyyy-mm-ddThh:mm:ss[.fff] (fixed field widths, 'T' never"
                    + " a space, the seconds field written, no zone or offset, a fraction of exactly three digits; the"
                    + " day-first source snippet belongs in `source` — ch. 4.1)"), lexeem);
        }
        for (String lexeem : List.of("2026-03-15T12:00:00", "2026-03-15T12:00:00.000", "2024-02-29T23:59:59.999")) {
            ObjectNode ir = golden(PREMIEVENSTER);
            zet(ir, WAARDE + "/type", "{\"node\":\"Date\",\"precision\":\"millisecond\"}");
            zet(ir, WAARDE + "/value", "{\"dateTime\":\"" + lexeem + "\"}");
            assertEquals(lexeem, ((Expr.Literal) IrLoader.load(ir).resolvedRegels.get(0).waarde()).waarde());
        }
    }

    // ---------------- the builtin's granularity ----------------

    /** WF-037's sentence on the builtin node: a granularity belongs to the two bucketing builtins. Before this check
     *  both branches played jutterswet-paasvondst with month on its yearOf as if the facet were not written (PLAYED 5
     *  in each), where the verifier refuses it; every other builtin kind is refused the same way, and the two bucketing
     *  kinds keep it. */
    @Test void een_granulariteit_staat_alleen_op_een_emmerende_builtin() throws Exception {
        ObjectNode ir = golden(PAASVONDST);
        ((ObjectNode) ir.at(WAARDE)).put("granularity", "month");
        assertTrue(weigert(ir, WAARDE).getMessage().endsWith(": granularity facet on a non-bucketing kind"));
        int geweigerd = 0;
        for (Expr.FunctieKind kind : Expr.FunctieKind.values()) {
            // the folds: a kind of the other node, refused by name above
            if (Spelling.AGGREGATION_KIND.rijen().containsKey(kind)) continue;
            ir = golden(PACHTVENSTER);
            ((ObjectNode) ir.at(WAARDE)).put("kind", Spelling.BUILTIN_KIND.spelling(kind));
            if (kind == Expr.FunctieKind.TIJDSEVENREDIG_DEEL || kind == Expr.FunctieKind.AANTAL_DAGEN_IN) {
                assertNotNull(IrLoader.load(ir), kind.name());
                continue;
            }
            assertTrue(weigert(ir, WAARDE).getMessage().endsWith(": granularity facet on a non-bucketing kind"),
                    kind.name());
            geweigerd++;
        }
        assertEquals(15, geweigerd, "the seventeen builtin kinds but the two bucketing ones");
    }

    // ---------------- the context nodes ----------------

    /** WF-098: the two wire names, byte for byte — the v0 names of the two among the refused since L5 — and the record
     *  holds the v0 name the translation table gives for each. */
    @Test void een_contextwaarde_draagt_een_van_de_twee_namen() throws Exception {
        for (String naam : List.of("de dag", "rekendatum", "Rekendatum ", "Peildatum", "Rekendatum", "Rekenjaar",
                "EvaluationDate", "evaluationDate ")) {
            ObjectNode ir = golden(PREMIEVENSTER);
            zet(ir, WAARDE, contextwaarde(naam).toString());
            assertTrue(weigert(ir, WAARDE + "/name").getMessage().endsWith(": context value '" + naam + "' is not one"
                    + " of the two admissible names evaluationDate, evaluationYear: the context-value name is a closed"
                    + " set, spelled byte for byte"), naam);
        }
        String[][] namen = { { "evaluationDate", "Rekendatum" }, { "evaluationYear", "Rekenjaar" } };
        for (String[] naam : namen) {
            ObjectNode ir = golden(PREMIEVENSTER);
            zet(ir, WAARDE, contextwaarde(naam[0]).toString());
            Expr.Constante c = assertInstanceOf(Expr.Constante.class, IrLoader.load(ir).resolvedRegels.get(0).waarde());
            assertEquals(naam[1], c.naam());
            assertInstanceOf(Datatype.DatumInDagen.class, c.type());
        }
    }

    /** WF-034: a VariableReference is the frontend's marker of a variable it could not bind, and the wire serializes no
     *  binding site for it — a rule-local variable is a Let, read by a BinderRef — so the loader refuses it at the node,
     *  in the sentence of the verifier and the shared OCaml loader (corpus/negatief/wf034-variabele-zonder-binding). */
    @Test void een_variabeleverwijzing_wordt_geweigerd() throws Exception {
        ObjectNode ir = golden(PREMIEVENSTER);
        zet(ir, WAARDE, "{\"node\":\"VariableReference\",\"name\":\"vrijstelling\","
                + "\"source\":{\"snippet\":\"de vrijstelling\"}}");
        assertTrue(weigert(ir, WAARDE).getMessage().endsWith(": variable reference 'vrijstelling' resolves against no"
                + " binding: a rule-local variable is a Let, read by a BinderRef"));
    }

    @Test void de_contextknopen_dragen_hun_naam_en_hun_datatype() throws Exception {
        for (String naam : List.of("null", "5")) {
            ObjectNode ir = golden(PREMIEVENSTER);
            ObjectNode c = contextwaarde("evaluationDate");
            c.set("name", JSON.readTree(naam));
            zet(ir, WAARDE, c.toString());
            assertTrue(weigert(ir, WAARDE + "/name").getMessage().endsWith(": " + IrLoader.CONTEXTWAARDE_NAAM), naam);
        }
        ObjectNode ir = golden(PREMIEVENSTER);
        zet(ir, WAARDE, contextwaarde("evaluationDate").putNull("type").toString());
        assertTrue(weigert(ir, WAARDE + "/type").getMessage().endsWith(": " + IrLoader.CONTEXTWAARDE_TYPE));
        ir = golden(HAVENROOSTER);
        ((ObjectNode) ir.at(BOUNDDAY)).putNull("type");
        assertTrue(weigert(ir, BOUNDDAY + "/type").getMessage().endsWith(": " + IrLoader.BOUNDDAY_TYPE));
    }

    // ---------------- the named forms ----------------

    @Test void lege_lijsten_en_een_gescoopte_unie_weigeren() throws Exception {
        ObjectNode ir = golden(PREMIEVENSTER);
        ((ObjectNode) ir.at(REGEL)).putArray("versions");
        assertTrue(weigert(ir, REGEL + "/versions").getMessage().endsWith(": a form carries at least one version"));

        String u = "/rules/uniquenesses/2/versions/0";
        ir = golden(KUSTREGISTER);
        ((ObjectNode) ir.at(u)).putArray("sources");
        assertTrue(weigert(ir, u + "/sources").getMessage()
                .endsWith(": a uniqueness version carries at least one source"));
        ir = golden(KUSTREGISTER);
        ((ObjectNode) ir.at(u + "/sources/0")).putArray("keys");
        assertTrue(weigert(ir, u + "/sources/0/keys").getMessage().endsWith(": a source carries at least one key"));
        ir = golden(KUSTREGISTER);
        ((ObjectNode) ir.at(u)).set("scope", ir.at("/rules/uniquenesses/0/versions/0/scope").deepCopy());
        assertTrue(weigert(ir, u + "/scope").getMessage().endsWith(": a uniqueness version with two or more sources"
                + " carries no scope: a scoped union is an open design question"));

        String f = "/rules/factCreations/0/versions/0";
        ir = golden(MASTSEIN);
        ((ObjectNode) ir.at(f)).putArray("path");
        assertTrue(weigert(ir, f + "/path").getMessage().endsWith(": a fact creation's path carries at least one hop"));
    }

    @Test void een_vormnaam_spelt_geen_versie() throws Exception {
        String zin = ": a form's name does not end in a space, '#' and digits: that spelling names one version of a"
                + " form with several";
        for (String naam : List.of("basispremie #2", "basispremie #01", "basispremie #1")) {
            ObjectNode ir = golden(PREMIEVENSTER);
            ((ObjectNode) ir.at(REGEL)).put("name", naam);
            assertTrue(weigert(ir, REGEL + "/name").getMessage().endsWith(zin), naam);
        }
        ObjectNode ir = golden(BERGING);
        ((ObjectNode) ir.at("/rules/allocations/0")).put("name", "verdeling #3");
        assertTrue(weigert(ir, "/rules/allocations/0/name").getMessage().endsWith(zin));
        // only the whole spelling: a '#' elsewhere, or without its space or its digits, names no version
        for (String naam : List.of("basispremie#2", "basispremie #", "basispremie #2 x", "#2 basispremie"))
            assertNotNull(IrLoader.load(naamGezet(naam)), naam);
    }

    @Test void een_optioneel_veld_is_heel_of_afwezig() throws Exception {
        String zin = "an optional field is written whole or not at all, and null is neither";
        String[][] rijen = { { PREMIEVENSTER, VERSIE, "condition" }, { PREMIEVENSTER, VERSIE, "presentation" },
                { PREMIEVENSTER, REGEL, "statuteAnnotations" },
                { BERGING, "/rules/allocations/0/versions/0", "ratioAttribute" },
                { WACHTERSKETEN, "/rules/objectCreations/0/versions/0", "connectingRole" },
                { KUSTREGISTER, "/rules/uniquenesses/0/versions/0", "scope" },
                { BOOTBERGING, "/rules/rules/1/versions/0/value", "emptyFallback" },
                { PACHTVENSTER, WAARDE, "granularity" } };
        for (String[] r : rijen) {
            ObjectNode ir = golden(r[0]);
            ((ObjectNode) ir.at(r[1])).putNull(r[2]);
            assertTrue(weigert(ir, r[1] + "/" + r[2]).getMessage().endsWith(": " + zin), r[2]);
        }
    }

    // ---------------- a rule version's target and kind ----------------

    @Test void het_doel_is_een_attribuutreferentie() throws Exception {
        List<String> doelen = List.of("null", "{\"node\":\"ParameterReference\",\"parameter\":\"x\",\"source\":{\"snippet\":\"x\"}}",
                "{\"node\":\"Literal\",\"value\":{\"number\":\"1\"},\"type\":" + NUMBER + ",\"source\":{\"snippet\":\"1\"}}");
        for (String doel : doelen) {
            ObjectNode ir = golden(PREMIEVENSTER);
            zet(ir, VERSIE + "/target", doel);
            assertTrue(weigert(ir, VERSIE + "/target").getMessage()
                    .endsWith(": the target of a rule version is an AttributeReference"), doel);
        }
    }

    @Test void een_tag_staat_niet_naast_een_initialisatie() throws Exception {
        String zin = ": presentation beside initialisation true: a presentation tags a computing version, never an"
                + " initialisation";
        ObjectNode ir = golden(WAARDERING);
        ((ObjectNode) ir.at("/rules/rules/1/versions/0")).put("presentation", "calculation");
        assertTrue(weigert(ir, "/rules/rules/1/versions/0/presentation").getMessage().endsWith(zin));
        ir = golden(PREMIEVENSTER);
        ((ObjectNode) ir.at(VERSIE)).put("initialisation", true);
        assertTrue(weigert(ir, VERSIE + "/presentation").getMessage().endsWith(zin));
    }

    /** WF-022, the verifier's sentences: the version's fields say which kind of member it writes, the declaration
     *  says which member the target names, and a name that is both decides nothing. */
    @Test void het_doel_noemt_het_lid_dat_de_versie_schrijft() throws Exception {
        // a computing version whose tag is dropped attributes a characteristic, and basispremie is an attribute
        ObjectNode ir = golden(PREMIEVENSTER);
        ((ObjectNode) ir.at(VERSIE)).remove("presentation");
        assertTrue(weigert(ir, VERSIE + "/target").getMessage().endsWith(": a version without a presentation and"
                + " without initialisation attributes a characteristic, so its target MUST name one, and 'basispremie'"
                + " is an attribute of 'Vondst'"));
        // a characteristic attribution given a tag, or made an initialisation, writes an attribute
        ir = golden(PENNINGDREMPEL);
        ((ObjectNode) ir.at(VERSIE)).put("presentation", "equation");
        String zin = ": a version with a presentation or with initialisation true writes an attribute, so its target"
                + " MUST name one, and 'penninggerechtigd' is a characteristic of 'Jutter'";
        assertTrue(weigert(ir, VERSIE + "/target").getMessage().endsWith(zin));
        ir = golden(PENNINGDREMPEL);
        ((ObjectNode) ir.at(VERSIE)).put("initialisation", true);
        assertTrue(weigert(ir, VERSIE + "/target").getMessage().endsWith(zin));
        // the name of the target as the target writes it
        ir = golden(PENNINGDREMPEL);
        ((ObjectNode) ir.at(VERSIE)).put("presentation", "equation");
        ((ObjectNode) ir.at(VERSIE + "/target")).put("attribute", "Penninggerechtigd").put("subjectType", "jutter");
        assertTrue(weigert(ir, VERSIE + "/target").getMessage().endsWith(": a version with a presentation or with"
                + " initialisation true writes an attribute, so its target MUST name one, and 'Penninggerechtigd' is a"
                + " characteristic of 'jutter'"));
        // a name that is an attribute and a characteristic of the subject type, whatever the version's kind
        for (boolean tag : new boolean[] { false, true }) {
            ir = golden(TOELAGEGRONDEN);
            ObjectNode regel = (ObjectNode) JSON.readTree(golden(PENNINGDREMPEL).at(REGEL).toString());
            ObjectNode v = (ObjectNode) regel.at("/versions/0");
            ((ObjectNode) v.get("target")).put("attribute", "toelage").put("subjectType", "Jutter");
            v.remove("condition");
            if (tag) v.put("presentation", "equation");
            IrPad.rulesLijst(ir, "rules").add(regel);
            assertTrue(weigert(ir, VERSIE + "/target").getMessage().endsWith(": target 'toelage' names both an"
                    + " attribute and a characteristic of 'Jutter': which member a rule writes decides its kind, and"
                    + " this name decides nothing"), "tag " + tag);
        }
    }

    /** WF-028's sentences: the target type of a fact creation is the declared filler of its target role, and a role
     *  that resolves to none leaves the creation without a target. The anchor role is refused in the same words. */
    @Test void een_feitcreatie_zonder_vuller_van_de_doelrol_weigert() throws Exception {
        String f = "/rules/factCreations/0/versions/0";
        ObjectNode ir = golden(MASTSEIN);
        ((ObjectNode) ir.at(f)).put("targetRole", "baken");
        assertTrue(weigert(ir, f + "/targetRole").getMessage().endsWith(": 'baken' is not a role of 'vakbeseining'"));
        // before the fix round of the L4 landing this loaded, and both branches then refused the creation as
        // unresolved ("feitcreatie (onopgelost)"), a sentence that names neither the field nor the relation
        ir = golden(MASTSEIN);
        ((ObjectNode) ir.at(f)).put("anchorRole", "baken");
        assertTrue(weigert(ir, f + "/anchorRole").getMessage().endsWith(": 'baken' is not a role of 'vakbeseining'"));
        ir = golden(MASTSEIN);
        ((ObjectNode) ir.at(f)).put("factType", "bakenbeseining");
        assertTrue(weigert(ir, f + "/factType").getMessage()
                .endsWith(": created fact type 'bakenbeseining' is not declared"));
    }

    // ---------------- read back one to one ----------------

    @Test void elke_versie_wordt_een_record_met_de_naam_van_een_versie() throws Exception {
        ObjectNode ir = golden(JUTHEFFING);
        RegelSpraakModel m = IrLoader.load(ir);
        List<String> namen = new ArrayList<>();
        for (ResolvedRegel r : m.resolvedRegels) namen.add(r.naam());
        assertEquals(List.of("jutheffing naar jaartarief #1", "jutheffing naar jaartarief #2",
                "registratiegetal bij aangifte"), namen, "two versions carry the suffix, a form of one its bare name");
        assertEquals(LocalDate.of(2025, 1, 1), m.resolvedRegels.get(0).geldigheid().vanaf());
        assertEquals(ir.at("/rules/rules/0/versions/1/validity/from").asText(),
                String.valueOf(m.resolvedRegels.get(1).geldigheid().vanaf()), "each version its own validity");
        assertNull(m.resolvedRegels.get(2).geldigheid().vanaf());
        assertNull(m.resolvedRegels.get(2).geldigheid().tot());

        // a nameless form stays nameless on every version, and the form's annotations stand on each
        ((ObjectNode) ir.at("/rules/rules/0")).putNull("name");
        ((ObjectNode) ir.at("/rules/rules/0")).putArray("statuteAnnotations").add("art. 3 Jutterswet");
        m = IrLoader.load(ir);
        assertNull(m.resolvedRegels.get(0).naam());
        assertNull(m.resolvedRegels.get(1).naam());
        assertEquals(List.of("art. 3 Jutterswet"), m.resolvedRegels.get(0).bronAnnotaties());
        assertEquals(List.of("art. 3 Jutterswet"), m.resolvedRegels.get(1).bronAnnotaties());
        assertEquals(List.of(), m.resolvedRegels.get(2).bronAnnotaties());

        ir = golden(HARDHEIDSOVERSCHOT);
        List<String> annotaties = new ArrayList<>();
        ir.at("/rules/rules/1/statuteAnnotations").forEach(a -> annotaties.add(a.asText()));
        assertEquals(2, annotaties.size());
        assertEquals(annotaties, IrLoader.load(ir).resolvedRegels.get(1).bronAnnotaties());
    }

    @Test void de_soort_van_een_versie_is_haar_structuur() throws Exception {
        assertEquals(ResolvedRegel.Soort.BEREKENING, IrLoader.load(golden(PREMIEVENSTER)).resolvedRegels.get(0).soort());
        assertEquals(ResolvedRegel.Soort.GELIJKSTELLING, IrLoader.load(golden(KEURSTAAT)).resolvedRegels.get(3).soort());
        assertEquals(ResolvedRegel.Soort.KENMERKTOEKENNING,
                IrLoader.load(golden(PENNINGDREMPEL)).resolvedRegels.get(0).soort());
        assertEquals(ResolvedRegel.Soort.INITIALISATIE, IrLoader.load(golden(WAARDERING)).resolvedRegels.get(1).soort());
        // the tag is read as written: equation on the same version is the other computing kind
        ObjectNode ir = golden(PREMIEVENSTER);
        ((ObjectNode) ir.at(VERSIE)).put("presentation", "equation");
        assertEquals(ResolvedRegel.Soort.GELIJKSTELLING, IrLoader.load(ir).resolvedRegels.get(0).soort());
    }

    @Test void een_feitcreatie_leest_haar_doelsoort_en_haar_pad() throws Exception {
        ObjectNode ir = golden(MASTSEIN);
        ResolvedFeitCreatie fc = IrLoader.load(ir).feitCreaties.get(0);
        assertEquals("Sein", fc.doel().naam, "the declared filler of targetRole 'vaksein' in 'vakbeseining'");
        assertEquals("Kustvak", fc.anker().naam);
        assertEquals("vakbeseining", fc.feittype());
        assertEquals("seinvak", fc.ankerRol());
        assertEquals("vaksein", fc.doelRol());
        List<RolBindingRef> stappen = new ArrayList<>();
        for (ResolvedFeitCreatie.Tussenstap t : fc.tussenstappen()) stappen.add(t.rolBinding());
        assertEquals(List.of(new RolBindingRef("kustligging", "post"), new RolBindingRef("mastvoering", "mast")),
                stappen, "every hop of the path but the last is a step");
        assertEquals(new RolBindingRef("seinvoering", "sein"), fc.joinRol(), "and the last is the join");
        // one hop is a join without steps
        ArrayNode pad = (ArrayNode) ir.at("/rules/factCreations/0/versions/0/path");
        pad.remove(0);
        pad.remove(0);
        fc = IrLoader.load(ir).feitCreaties.get(0);
        assertEquals(List.of(), fc.tussenstappen());
        assertEquals(new RolBindingRef("seinvoering", "sein"), fc.joinRol());
    }

    @Test void de_ene_lijst_uniciteiten_splitst_op_het_aantal_bronnen() throws Exception {
        ObjectNode ir = golden(KUSTREGISTER);
        RegelSpraakModel m = IrLoader.load(ir);
        assertEquals(List.of("eenmalig kaveletiket", "eenmalige sectiecode"),
                m.uniciteiten.stream().map(ResolvedUniciteit::naam).toList());
        assertEquals(List.of("eenmalig vondstnummer", "eenmalige bakencode"),
                m.uniciteitUnies.stream().map(ResolvedUniciteitUnie::naam).toList());
        ResolvedUniciteit enkel = m.uniciteiten.get(0);
        assertEquals("Kavel", enkel.subject().naam);
        assertEquals(List.of("etiket"), enkel.sleutels());
        assertEquals(new RolBindingRef("kavelindeling", "gemeente"), enkel.scopeRol());
        assertNull(enkel.scopeVeld());
        ResolvedUniciteitUnie unie = m.uniciteitUnies.get(1);
        assertEquals(List.of("Baken", "Baken"), unie.bronnen().stream().map(b -> b.type().naam).toList());
        assertEquals(List.of(List.of("landcode"), List.of("zeecode")),
                unie.bronnen().stream().map(ResolvedUniciteitUnie.Bron::sleutels).toList());
        // the order of the one list carries no meaning: the unions first split the same
        ArrayNode lijst = IrPad.rulesLijst(ir, "uniquenesses");
        ArrayNode omgekeerd = JSON.createArrayNode();
        for (int i = lijst.size() - 1; i >= 0; i--) omgekeerd.add(lijst.get(i));
        IrPad.rules(ir).set("uniquenesses", omgekeerd);
        m = IrLoader.load(ir);
        assertEquals(List.of("eenmalige sectiecode", "eenmalig kaveletiket"),
                m.uniciteiten.stream().map(ResolvedUniciteit::naam).toList());
        assertEquals(List.of("eenmalige bakencode", "eenmalig vondstnummer"),
                m.uniciteitUnies.stream().map(ResolvedUniciteitUnie::naam).toList());
    }

    @Test void beide_functieknopen_worden_het_ene_functierecord() throws Exception {
        Expr.Functie som = assertInstanceOf(Expr.Functie.class,
                IrLoader.load(golden(BOOTBERGING)).resolvedRegels.get(1).waarde());
        assertEquals(Expr.FunctieKind.SOM, som.kind());
        assertEquals("Vondst", som.elementObjecttype().naam);
        assertNotNull(som.leegwaarde(), "emptyFallback");
        assertNull(som.granulariteit(), "a fold carries no granularity");
        Expr.Functie deel = assertInstanceOf(Expr.Functie.class,
                IrLoader.load(golden(PACHTVENSTER)).resolvedRegels.get(0).waarde());
        assertEquals(Expr.FunctieKind.TIJDSEVENREDIG_DEEL, deel.kind());
        assertEquals(Spelling.GRANULARITY.waarde("BuiltinCall.granularity",
                        golden(PACHTVENSTER).at(WAARDE + "/granularity").asText()),
                deel.granulariteit());
        assertNotNull(deel.filter(), "filter");
        assertNull(deel.elementObjecttype(), "a builtin carries none of the fold's fields");
        assertNull(deel.leegwaarde());
        assertNull(deel.dimensieSelectie());
    }

    @Test void een_literaal_houdt_het_lexeem_van_zijn_variant() throws Exception {
        assertEquals("1", ((Expr.Literal) IrLoader.load(golden(PREMIEVENSTER)).resolvedRegels.get(0).waarde()).waarde());
        assertEquals("waar",
                ((Expr.Literal) IrLoader.load(golden(PENNINGDREMPEL)).resolvedRegels.get(0).waarde()).waarde());
        assertEquals("gaaf", ((Expr.Literal) IrLoader.load(golden(KEURSTAAT)).resolvedRegels.get(3).waarde()).waarde());
        ObjectNode ir = golden(PREMIEVENSTER);
        zet(ir, WAARDE, "{\"node\":\"Literal\",\"value\":{\"boolean\":false},\"type\":{\"node\":\"Boolean\"},"
                + "\"source\":{\"snippet\":\"onwaar\"}}");
        assertEquals("onwaar", ((Expr.Literal) IrLoader.load(ir).resolvedRegels.get(0).waarde()).waarde());
        // the text variant admits the empty string: its spelling is the case format's
        ir = golden(PREMIEVENSTER);
        zet(ir, WAARDE, "{\"node\":\"Literal\",\"value\":{\"text\":\"\"},\"type\":{\"node\":\"Text\"},\"source\":{\"snippet\":\"''\"}}");
        assertEquals("", ((Expr.Literal) IrLoader.load(ir).resolvedRegels.get(0).waarde()).waarde());
    }

    @Test void de_gebonden_dag_is_de_constante_de_dag() throws Exception {
        ObjectNode ir = golden(PREMIEVENSTER);
        zet(ir, WAARDE, "{\"node\":\"BoundDay\",\"type\":" + DAG + ",\"source\":{\"snippet\":\"de dag\"}}");
        Expr.Constante c = assertInstanceOf(Expr.Constante.class, IrLoader.load(ir).resolvedRegels.get(0).waarde());
        assertEquals("de dag", c.naam());
        assertInstanceOf(Datatype.DatumInDagen.class, c.type());
        assertNotNull(IrLoader.load(golden(HAVENROOSTER)), "a day-kind definition that compares the bound day loads");
    }

    // ---------------- helpers ----------------

    private static ObjectNode contextwaarde(String naam) {
        ObjectNode c = JSON.createObjectNode();
        c.put("node", "ContextValue").put("name", naam);
        c.set("type", leesJson(DAG));
        c.set("source", IrPad.bronrecord("de " + naam));
        return c;
    }

    private static ObjectNode naamGezet(String naam) throws Exception {
        ObjectNode ir = golden(PREMIEVENSTER);
        ((ObjectNode) ir.at(REGEL)).put("name", naam);
        return ir;
    }

    private static JsonNode leesJson(String json) {
        try {
            return JSON.readTree(json);
        } catch (Exception e) {
            throw new AssertionError(json, e);
        }
    }

    private static void zet(ObjectNode ir, String pad, String json) throws Exception {
        int slash = pad.lastIndexOf('/');
        JsonNode ouder = ir.at(pad.substring(0, slash));
        if (!(ouder instanceof ObjectNode o)) throw new AssertionError("no object at " + pad.substring(0, slash));
        o.set(pad.substring(slash + 1), JSON.readTree(json));
    }

    private static IrLoader.Documentfout weigert(JsonNode ir, String plek) {
        IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.loadAll(ir),
                "verwacht een documentfout op " + plek);
        assertEquals(plek, e.plek(), e.getMessage());
        return e;
    }

    private static void onbekendVeld(JsonNode ir, String plek, String knoop) {
        IrLoader.Documentfout e = weigert(ir, plek);
        assertTrue(e.getMessage().endsWith(": unknown field on " + knoop), e.getMessage());
    }

    private static ObjectNode golden(String naam) throws Exception {
        Path ir = CORPUS.resolve(naam).resolve("ir.json");
        assertTrue(Files.exists(ir), "golden ir.json present: " + ir.toAbsolutePath());
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(ir));
        IrLoader.loadAll(doc);   // the untwisted golden loads, so the refusal below is the twist's alone
        return doc;
    }
}
