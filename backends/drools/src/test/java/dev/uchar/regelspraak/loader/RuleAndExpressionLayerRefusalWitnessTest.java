package dev.uchar.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.cases.IrPath;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.ResolvedFactCreation;
import dev.uchar.regelspraak.ir.ResolvedRegel;
import dev.uchar.regelspraak.ir.ResolvedUniqueness;
import dev.uchar.regelspraak.ir.ResolvedUniquenessUnion;
import dev.uchar.regelspraak.ir.Spelling;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.RoleBindingRef;

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
class RuleAndExpressionLayerRefusalWitnessTest {

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
    private static final String VERSION = REGEL + "/versions/0";
    private static final String VALUE = VERSION + "/value";
    private static final String BOUNDDAY = "/rules/dayKindDefinitions/0/condition/parts/0/left";

    private static final String NUMBER = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";
    private static final String DAG = "{\"node\":\"Date\",\"precision\":\"day\"}";

    // ---------------- the L3 shape is unknown by name ----------------

    @Test void the_nodes_of_the_l3_form_are_unknown() throws Exception {
        String[][] forms = {
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
        for (String[] v : forms) {
            ObjectNode ir = golden(v[0]);
            ((ObjectNode) ir.at(v[1])).put("node", v[2]);
            assertTrue(refuses(ir, v[1] + "/node").getMessage().contains("expected \"" + v[3] + "\", got \"" + v[2]
                    + "\""), v[2]);
        }
        // the two expression nodes the L4 shape split, in the shared loader's sentence
        ObjectNode ir = golden(PREMIEVENSTER);
        ((ObjectNode) ir.at(VALUE)).put("node", "Functie");
        assertTrue(refuses(ir, VALUE + "/node").getMessage().endsWith(": unknown expr node \"Functie\""));
        ir = golden(HAVENROOSTER);
        ((ObjectNode) ir.at(BOUNDDAY)).put("node", "Constante");
        assertTrue(refuses(ir, BOUNDDAY + "/node").getMessage().endsWith(": unknown expr node \"Constante\""));
        // and the second list of uniquenesses, which the one list replaced
        ir = golden(PENNINGDREMPEL);
        IrPath.rules(ir).putArray("uniciteitUnies");
        assertTrue(refuses(ir, "/rules/uniciteitUnies").getMessage().contains("unknown field"));
    }

    /** Every key of the L3 shape on the node or version record that replaced it — and a version record, which carries
     *  no discriminator, refuses one as it refuses any key it does not have. */
    @Test void the_fields_of_the_l3_form_are_unknown_fields() throws Exception {
        Map<String[], List<String>> rows = new LinkedHashMap<>();
        rows.put(new String[] { PENNINGDREMPEL, REGEL, "Rule" }, List.of("naam", "bronAnnotaties"));
        rows.put(new String[] { PENNINGDREMPEL, VERSION, "RuleVersion" },
                List.of("node", "naam", "soort", "doel", "waarde", "voorwaarde", "geldigheid", "bronAnnotaties"));
        rows.put(new String[] { BERGING, "/rules/allocations/0/versions/0", "AllocationVersion" },
                List.of("node", "bronType", "bronAttribuut", "doelType", "doelAttribuut", "methode", "ratoAttribuut",
                        "maximumAttribuut", "afrondingDecimalen", "restantAttribuut", "groepeerAttribuut", "afnemend",
                        "verbindendeRol", "voorwaarde", "geldigheid"));
        rows.put(new String[] { WACHTERSKETEN, "/rules/objectCreations/0/versions/0", "ObjectCreationVersion" },
                List.of("node", "nieuwType", "toekenningen", "verbindendeRol", "voorwaarde", "geldigheid"));
        rows.put(new String[] { MASTSEIN, "/rules/factCreations/0/versions/0", "FactCreationVersion" },
                List.of("node", "feittype", "ankerRol", "doelRol", "anker", "tussenstappen", "joinRol", "doel",
                        "voorwaarde", "geldigheid"));
        rows.put(new String[] { WACHTERSKETEN, "/rules/consistencies/0/versions/0", "ConsistencyVersion" },
                List.of("node", "assertie", "geldigheid"));
        rows.put(new String[] { KUSTREGISTER, "/rules/uniquenesses/0/versions/0", "UniquenessVersion" },
                List.of("node", "sleutels", "scopeRol", "bronnen", "subject", "geldigheid"));
        rows.put(new String[] { KUSTREGISTER, "/rules/uniquenesses/2/versions/0/sources/0", "Source" },
                List.of("type", "sleutels"));
        // the fold's own fields, and the builtin's facet, which it does not carry
        rows.put(new String[] { BOOTBERGING, VALUE, "Aggregation" },
                List.of("argumenten", "elementObjecttype", "leegwaarde", "dimensieSelectie", "bronRol", "bronPad",
                        "granulariteit", "granularity"));
        // the builtin's own fields, and the five fold fields, which it does not carry
        rows.put(new String[] { PACHTVENSTER, VALUE, "BuiltinCall" },
                List.of("argumenten", "granulariteit", "elementType", "emptyFallback", "dimensionSelection",
                        "sourceRole", "sourcePath"));
        rows.put(new String[] { PREMIEVENSTER, VALUE, "Literal" }, List.of("waarde"));
        rows.put(new String[] { HAVENROOSTER, BOUNDDAY, "BoundDay" }, List.of("name", "naam"));
        for (Map.Entry<String[], List<String>> r : rows.entrySet())
            for (String field : r.getValue()) {
                ObjectNode ir = golden(r.getKey()[0]);
                ((ObjectNode) ir.at(r.getKey()[1])).put(field, "x");
                unknownField(ir, r.getKey()[1] + "/" + field, r.getKey()[2]);
            }
        // a session value, which no loadable golden carries, at a rule's value
        ObjectNode ir = golden(PREMIEVENSTER);
        set(ir, VALUE, contextValue("evaluationDate").put("naam", "x").toString());
        unknownField(ir, VALUE + "/naam", "ContextValue");
    }

    // ---------------- the closed sets ----------------

    @Test void method_tag_kind_and_granularity_are_closed_sets() throws Exception {
        for (String method : List.of("COMPLEX", "naar_rato")) {
            ObjectNode ir = golden(BERGING);
            ((ObjectNode) ir.at("/rules/allocations/0/versions/0")).put("method", method);
            assertTrue(refuses(ir, "/rules/allocations/0/versions/0/method").getMessage()
                    .endsWith(": unknown method value \"" + method + "\""), method);
        }
        ObjectNode ir = golden(PREMIEVENSTER);
        ((ObjectNode) ir.at(VERSION)).put("presentation", "KENMERKTOEKENNING");
        assertTrue(refuses(ir, VERSION + "/presentation").getMessage()
                .endsWith(": unknown presentation value \"KENMERKTOEKENNING\""));
        // a kind of the other function node is as unknown as a kind of none
        String[][] kinds = { { BOOTBERGING, "squareRoot" }, { BOOTBERGING, "GEMIDDELDE" }, { PACHTVENSTER, "sum" },
                { PACHTVENSTER, "filledCount" } };
        for (String[] s : kinds) {
            ir = golden(s[0]);
            ((ObjectNode) ir.at(VALUE)).put("kind", s[1]);
            assertTrue(refuses(ir, VALUE + "/kind").getMessage().endsWith(": unknown kind value \"" + s[1] + "\""),
                    s[1]);
        }
        ir = golden(PACHTVENSTER);
        ((ObjectNode) ir.at(VALUE)).put("granularity", "UUR");
        assertTrue(refuses(ir, VALUE + "/granularity").getMessage().endsWith(": unknown granularity value \"UUR\""));
    }

    // ---------------- the literal value ----------------

    @Test void a_literal_value_is_exactly_a_variant() throws Exception {
        List<String> forms = List.of("\"1\"", "1", "null", "{}", "{\"number\":1}", "{\"number\":\"1\",\"text\":\"1\"}",
                "{\"getal\":\"1\"}", "{\"boolean\":\"waar\"}", "{\"constructor\":\"Wad\",\"payload\":{}}");
        for (String form : forms) {
            ObjectNode ir = golden(PREMIEVENSTER);
            set(ir, VALUE + "/value", form);
            IrLoader.Documentfout e = refuses(ir, VALUE + "/value");
            assertTrue(e.getMessage().endsWith(": " + IrLoader.LITERAL_VALUE), form + ": " + e.getMessage());
        }
    }

    /**
     * WF-114, with the verifier's table: the variant a type names — a percentage is a Number — and the article before
     * the variant, "an" before enumeration alone. Every variant under every type this backend carries; the one that
     * matches loads, every other is refused at the value.
     */
    @Test void the_variant_is_that_which_the_type_names() throws Exception {
        String enumeration = golden(KEURSTAAT).at("/rules/rules/3/versions/0/value/type").toString();
        String percent = golden("jutterswet-vondsttarief").at(VALUE + "/type").toString();
        // type, the phrase the sentence gives it, the variant it names
        String[][] types = {
                { NUMBER, "a Number", "number" },
                { percent, "a Number", "number" },
                { "{\"node\":\"Boolean\"}", "a Boolean", "boolean" },
                { "{\"node\":\"Text\"}", "a Text", "text" },
                { DAG, "a day-precision Date", "date" },
                { "{\"node\":\"Date\",\"precision\":\"millisecond\"}", "a millisecond-precision Date", "dateTime" },
                { enumeration, "an Enumeration", "enumeration" } };
        Map<String, String> variants = new LinkedHashMap<>();
        variants.put("number", "{\"number\":\"5\"}");
        variants.put("boolean", "{\"boolean\":true}");
        variants.put("text", "{\"text\":\"gaaf\"}");
        variants.put("date", "{\"date\":\"2024-01-31\"}");
        variants.put("dateTime", "{\"dateTime\":\"2024-01-31T12:00:00\"}");
        variants.put("enumeration", "{\"enumeration\":\"gaaf\"}");
        variants.put("constructor", "{\"constructor\":\"Wad\"}");
        String w = "/rules/rules/3/versions/0/value";
        int loaded = 0, refused = 0;
        for (String[] t : types)
            for (Map.Entry<String, String> v : variants.entrySet()) {
                ObjectNode ir = golden(KEURSTAAT);
                set(ir, w + "/type", t[0]);
                set(ir, w + "/value", v.getValue());
                if (v.getKey().equals(t[2])) {
                    assertNotNull(IrLoader.load(ir), t[1] + " / " + v.getKey());
                    loaded++;
                    continue;
                }
                IrLoader.Documentfout e = refuses(ir, w + "/value");
                String sentence = "literal value is " + (v.getKey().equals("enumeration") ? "an" : "a") + " " + v.getKey()
                        + " value but its type is " + t[1] + ": the variant is the one the type names";
                assertTrue(e.getMessage().endsWith(": " + sentence), sentence + " / " + e.getMessage());
                refused++;
            }
        assertEquals(7, loaded);
        assertEquals(42, refused);
        // a literal without a type is WF-034's typeless value node: nothing to cross, so it loads
        ObjectNode ir = golden(KEURSTAAT);
        ((ObjectNode) ir.at(w)).putNull("type");
        set(ir, w + "/value", "{\"text\":\"5\"}");
        assertNotNull(IrLoader.load(ir));
    }

    /** WF-081's sentence, over the case format's number grammar: a decimal comma, a point without a digit before it,
     *  a unit in the lexeme and a denominator with a leading zero are no number. Before this check both branches read
     *  each of them as some other number (.5 and -.5 as 5, 3/04 as 0.75), where ref and the ASP backend fault. */
    @Test void a_number_follows_the_number_grammar_of_the_case_format() throws Exception {
        for (String lexeme : List.of("12,50", ".5", "-.5", "3/04", "5 €", "1.", "+1", "1e3", "1/0", "")) {
            ObjectNode ir = golden(PREMIEVENSTER);
            set(ir, VALUE + "/value", "{\"number\":\"" + lexeme + "\"}");
            assertTrue(refuses(ir, VALUE + "/value").getMessage().endsWith(": numeric literal lexeme '" + lexeme
                    + "' is not a number of the case format (an optional '-', digits, an optional '.' and digits, or a"
                    + " rational a/b whose denominator has no leading zero — ch. 4.1)"), lexeme);
        }
        for (String lexeme : List.of("12.50", "-3", "0", "1/3", "-2/7", "007")) {
            ObjectNode ir = golden(PREMIEVENSTER);
            set(ir, VALUE + "/value", "{\"number\":\"" + lexeme + "\"}");
            assertEquals(lexeme, ((Expr.Literal) IrLoader.load(ir).resolvedRegels.get(0).waarde()).waarde());
        }
    }

    /** WF-084's sentence, over the verifier's date grammar: fixed four-two-two digit fields and a day of the calendar.
     *  Before this check the two witnesses' lexemes loaded and both branches refused the rule only while lowering, under
     *  a scope note ("regel (waarde buiten scope)", "TODO datumliteral"), where the verifier names the lexeme. */
    @Test void a_date_follows_the_iso_form() throws Exception {
        for (String lexeme : List.of("31-01-2024", "dd. 31-01-2024", "2024-1-31", "+2024-01-31", "2024-02-30",
                "2023-02-29", "1900-02-29", "2024-13-01", "2024-01-31T00:00:00", "")) {
            ObjectNode ir = golden(PREMIEVENSTER);
            set(ir, VALUE + "/type", DAG);
            set(ir, VALUE + "/value", "{\"date\":\"" + lexeme + "\"}");
            assertTrue(refuses(ir, VALUE + "/value").getMessage().endsWith(": date literal lexeme '" + lexeme
                    + "' is not the ISO form yyyy-mm-dd (fixed field widths, valid ranges — ch. 4.1)"), lexeme);
        }
        for (String lexeme : List.of("2024-01-31", "2024-02-29", "2000-02-29")) {
            ObjectNode ir = golden(PREMIEVENSTER);
            set(ir, VALUE + "/type", DAG);
            set(ir, VALUE + "/value", "{\"date\":\"" + lexeme + "\"}");
            assertEquals(lexeme, ((Expr.Literal) IrLoader.load(ir).resolvedRegels.get(0).waarde()).waarde());
        }
    }

    /** WF-095's sentence, over the verifier's date-time grammar: the date of WF-084, a 'T', the seconds always written,
     *  a fraction of exactly three digits and nothing after it. The first six lexemes are the six WF-095 witnesses'. */
    @Test void a_date_time_follows_the_canonical_iso_form() throws Exception {
        for (String lexeme : List.of("15-03-2026 12:00:00.000", "2026-03-15T12:00:00.1", "rommel 2026-03-15T12:00:00 x",
                "2026-03-15T12:00", "2026-03-15 12:00:00", "2026-03-15T12:00:00Z", "2026-03-15T12:00:00+01:00",
                "2026-03-15T24:00:00", "2026-03-15T12:60:00", "2026-03-15T12:00:60", "2026-02-30T12:00:00",
                "2026-03-15")) {
            ObjectNode ir = golden(PREMIEVENSTER);
            set(ir, VALUE + "/type", "{\"node\":\"Date\",\"precision\":\"millisecond\"}");
            set(ir, VALUE + "/value", "{\"dateTime\":\"" + lexeme + "\"}");
            assertTrue(refuses(ir, VALUE + "/value").getMessage().endsWith(": millisecond-precision date literal lexeme '"
                    + lexeme + "' is not the canonical ISO form yyyy-mm-ddThh:mm:ss[.fff] (fixed field widths, 'T' never"
                    + " a space, the seconds field written, no zone or offset, a fraction of exactly three digits; the"
                    + " day-first source snippet belongs in `source` — ch. 4.1)"), lexeme);
        }
        for (String lexeme : List.of("2026-03-15T12:00:00", "2026-03-15T12:00:00.000", "2024-02-29T23:59:59.999")) {
            ObjectNode ir = golden(PREMIEVENSTER);
            set(ir, VALUE + "/type", "{\"node\":\"Date\",\"precision\":\"millisecond\"}");
            set(ir, VALUE + "/value", "{\"dateTime\":\"" + lexeme + "\"}");
            assertEquals(lexeme, ((Expr.Literal) IrLoader.load(ir).resolvedRegels.get(0).waarde()).waarde());
        }
    }

    // ---------------- the builtin's granularity ----------------

    /** WF-037's sentence on the builtin node: a granularity belongs to the two bucketing builtins. Before this check
     *  both branches played jutterswet-paasvondst with month on its yearOf as if the facet were not written (PLAYED 5
     *  in each), where the verifier refuses it; every other builtin kind is refused the same way, and the two bucketing
     *  kinds keep it. */
    @Test void a_granularity_stands_only_on_a_bucketing_builtin() throws Exception {
        ObjectNode ir = golden(PAASVONDST);
        ((ObjectNode) ir.at(VALUE)).put("granularity", "month");
        assertTrue(refuses(ir, VALUE).getMessage().endsWith(": granularity facet on a non-bucketing kind"));
        int refused = 0;
        for (Expr.FunctionKind kind : Expr.FunctionKind.values()) {
            // the folds: a kind of the other node, refused by name above
            if (Spelling.AGGREGATION_KIND.rows().containsKey(kind)) continue;
            ir = golden(PACHTVENSTER);
            ((ObjectNode) ir.at(VALUE)).put("kind", Spelling.BUILTIN_KIND.spelling(kind));
            if (kind == Expr.FunctionKind.TIJDSEVENREDIG_DEEL || kind == Expr.FunctionKind.AANTAL_DAGEN_IN) {
                assertNotNull(IrLoader.load(ir), kind.name());
                continue;
            }
            assertTrue(refuses(ir, VALUE).getMessage().endsWith(": granularity facet on a non-bucketing kind"),
                    kind.name());
            refused++;
        }
        assertEquals(15, refused, "the seventeen builtin kinds but the two bucketing ones");
    }

    // ---------------- the context nodes ----------------

    /** WF-098: the two wire names, byte for byte — the v0 names of the two among the refused since L5 — and the record
     *  holds the v0 name the translation table gives for each. */
    @Test void a_context_value_carries_one_of_the_two_names() throws Exception {
        for (String name : List.of("de dag", "rekendatum", "Rekendatum ", "Peildatum", "Rekendatum", "Rekenjaar",
                "EvaluationDate", "evaluationDate ")) {
            ObjectNode ir = golden(PREMIEVENSTER);
            set(ir, VALUE, contextValue(name).toString());
            assertTrue(refuses(ir, VALUE + "/name").getMessage().endsWith(": context value '" + name + "' is not one"
                    + " of the two admissible names evaluationDate, evaluationYear: the context-value name is a closed"
                    + " set, spelled byte for byte"), name);
        }
        String[][] names = { { "evaluationDate", "Rekendatum" }, { "evaluationYear", "Rekenjaar" } };
        for (String[] name : names) {
            ObjectNode ir = golden(PREMIEVENSTER);
            set(ir, VALUE, contextValue(name[0]).toString());
            Expr.Constante c = assertInstanceOf(Expr.Constante.class, IrLoader.load(ir).resolvedRegels.get(0).waarde());
            assertEquals(name[1], c.naam());
            assertInstanceOf(Datatype.DatumInDagen.class, c.type());
        }
    }

    /** WF-034: a VariableReference is the frontend's marker of a variable it could not bind, and the wire serializes no
     *  binding site for it — a rule-local variable is a Let, read by a BinderRef — so the loader refuses it at the node,
     *  in the sentence of the verifier and the shared OCaml loader (corpus/negatief/wf034-variabele-zonder-binding). */
    @Test void a_variable_reference_is_refused() throws Exception {
        ObjectNode ir = golden(PREMIEVENSTER);
        set(ir, VALUE, "{\"node\":\"VariableReference\",\"name\":\"vrijstelling\","
                + "\"source\":{\"snippet\":\"de vrijstelling\"}}");
        assertTrue(refuses(ir, VALUE).getMessage().endsWith(": variable reference 'vrijstelling' resolves against no"
                + " binding: a rule-local variable is a Let, read by a BinderRef"));
    }

    @Test void the_context_nodes_carry_their_name_and_their_datatype() throws Exception {
        for (String name : List.of("null", "5")) {
            ObjectNode ir = golden(PREMIEVENSTER);
            ObjectNode c = contextValue("evaluationDate");
            c.set("name", JSON.readTree(name));
            set(ir, VALUE, c.toString());
            assertTrue(refuses(ir, VALUE + "/name").getMessage().endsWith(": " + IrLoader.CONTEXT_VALUE_NAME), name);
        }
        ObjectNode ir = golden(PREMIEVENSTER);
        set(ir, VALUE, contextValue("evaluationDate").putNull("type").toString());
        assertTrue(refuses(ir, VALUE + "/type").getMessage().endsWith(": " + IrLoader.CONTEXT_VALUE_TYPE));
        ir = golden(HAVENROOSTER);
        ((ObjectNode) ir.at(BOUNDDAY)).putNull("type");
        assertTrue(refuses(ir, BOUNDDAY + "/type").getMessage().endsWith(": " + IrLoader.BOUNDDAY_TYPE));
    }

    // ---------------- the named forms ----------------

    @Test void empty_lists_and_a_scoped_union_are_refused() throws Exception {
        ObjectNode ir = golden(PREMIEVENSTER);
        ((ObjectNode) ir.at(REGEL)).putArray("versions");
        assertTrue(refuses(ir, REGEL + "/versions").getMessage().endsWith(": a form carries at least one version"));

        String u = "/rules/uniquenesses/2/versions/0";
        ir = golden(KUSTREGISTER);
        ((ObjectNode) ir.at(u)).putArray("sources");
        assertTrue(refuses(ir, u + "/sources").getMessage()
                .endsWith(": a uniqueness version carries at least one source"));
        ir = golden(KUSTREGISTER);
        ((ObjectNode) ir.at(u + "/sources/0")).putArray("keys");
        assertTrue(refuses(ir, u + "/sources/0/keys").getMessage().endsWith(": a source carries at least one key"));
        ir = golden(KUSTREGISTER);
        ((ObjectNode) ir.at(u)).set("scope", ir.at("/rules/uniquenesses/0/versions/0/scope").deepCopy());
        assertTrue(refuses(ir, u + "/scope").getMessage().endsWith(": a uniqueness version with two or more sources"
                + " carries no scope: a scoped union is an open design question"));

        String f = "/rules/factCreations/0/versions/0";
        ir = golden(MASTSEIN);
        ((ObjectNode) ir.at(f)).putArray("path");
        assertTrue(refuses(ir, f + "/path").getMessage().endsWith(": a fact creation's path carries at least one hop"));
    }

    @Test void a_form_name_spells_no_version() throws Exception {
        String sentence = ": a form's name does not end in a space, '#' and digits: that spelling names one version of a"
                + " form with several";
        for (String name : List.of("basispremie #2", "basispremie #01", "basispremie #1")) {
            ObjectNode ir = golden(PREMIEVENSTER);
            ((ObjectNode) ir.at(REGEL)).put("name", name);
            assertTrue(refuses(ir, REGEL + "/name").getMessage().endsWith(sentence), name);
        }
        ObjectNode ir = golden(BERGING);
        ((ObjectNode) ir.at("/rules/allocations/0")).put("name", "verdeling #3");
        assertTrue(refuses(ir, "/rules/allocations/0/name").getMessage().endsWith(sentence));
        // only the whole spelling: a '#' elsewhere, or without its space or its digits, names no version
        for (String name : List.of("basispremie#2", "basispremie #", "basispremie #2 x", "#2 basispremie"))
            assertNotNull(IrLoader.load(nameSet(name)), name);
    }

    @Test void an_optional_field_is_whole_or_absent() throws Exception {
        String sentence = "an optional field is written whole or not at all, and null is neither";
        String[][] rows = { { PREMIEVENSTER, VERSION, "condition" }, { PREMIEVENSTER, VERSION, "presentation" },
                { PREMIEVENSTER, REGEL, "statuteAnnotations" },
                { BERGING, "/rules/allocations/0/versions/0", "ratioAttribute" },
                { WACHTERSKETEN, "/rules/objectCreations/0/versions/0", "connectingRole" },
                { KUSTREGISTER, "/rules/uniquenesses/0/versions/0", "scope" },
                { BOOTBERGING, "/rules/rules/1/versions/0/value", "emptyFallback" },
                { PACHTVENSTER, VALUE, "granularity" } };
        for (String[] r : rows) {
            ObjectNode ir = golden(r[0]);
            ((ObjectNode) ir.at(r[1])).putNull(r[2]);
            assertTrue(refuses(ir, r[1] + "/" + r[2]).getMessage().endsWith(": " + sentence), r[2]);
        }
    }

    // ---------------- a rule version's target and kind ----------------

    @Test void the_target_is_an_attribute_reference() throws Exception {
        List<String> targets = List.of("null", "{\"node\":\"ParameterReference\",\"parameter\":\"x\",\"source\":{\"snippet\":\"x\"}}",
                "{\"node\":\"Literal\",\"value\":{\"number\":\"1\"},\"type\":" + NUMBER + ",\"source\":{\"snippet\":\"1\"}}");
        for (String target : targets) {
            ObjectNode ir = golden(PREMIEVENSTER);
            set(ir, VERSION + "/target", target);
            assertTrue(refuses(ir, VERSION + "/target").getMessage()
                    .endsWith(": the target of a rule version is an AttributeReference"), target);
        }
    }

    @Test void a_tag_is_not_beside_an_initialisation() throws Exception {
        String sentence = ": presentation beside initialisation true: a presentation tags a computing version, never an"
                + " initialisation";
        ObjectNode ir = golden(WAARDERING);
        ((ObjectNode) ir.at("/rules/rules/1/versions/0")).put("presentation", "calculation");
        assertTrue(refuses(ir, "/rules/rules/1/versions/0/presentation").getMessage().endsWith(sentence));
        ir = golden(PREMIEVENSTER);
        ((ObjectNode) ir.at(VERSION)).put("initialisation", true);
        assertTrue(refuses(ir, VERSION + "/presentation").getMessage().endsWith(sentence));
    }

    /** WF-022, the verifier's sentences: the version's fields say which kind of member it writes, the declaration
     *  says which member the target names, and a name that is both decides nothing. */
    @Test void the_target_names_the_member_that_the_version_writes() throws Exception {
        // a computing version whose tag is dropped attributes a characteristic, and basispremie is an attribute
        ObjectNode ir = golden(PREMIEVENSTER);
        ((ObjectNode) ir.at(VERSION)).remove("presentation");
        assertTrue(refuses(ir, VERSION + "/target").getMessage().endsWith(": a version without a presentation and"
                + " without initialisation attributes a characteristic, so its target MUST name one, and 'basispremie'"
                + " is an attribute of 'Vondst'"));
        // a characteristic attribution given a tag, or made an initialisation, writes an attribute
        ir = golden(PENNINGDREMPEL);
        ((ObjectNode) ir.at(VERSION)).put("presentation", "equation");
        String sentence = ": a version with a presentation or with initialisation true writes an attribute, so its target"
                + " MUST name one, and 'penninggerechtigd' is a characteristic of 'Jutter'";
        assertTrue(refuses(ir, VERSION + "/target").getMessage().endsWith(sentence));
        ir = golden(PENNINGDREMPEL);
        ((ObjectNode) ir.at(VERSION)).put("initialisation", true);
        assertTrue(refuses(ir, VERSION + "/target").getMessage().endsWith(sentence));
        // the name of the target as the target writes it
        ir = golden(PENNINGDREMPEL);
        ((ObjectNode) ir.at(VERSION)).put("presentation", "equation");
        ((ObjectNode) ir.at(VERSION + "/target")).put("attribute", "Penninggerechtigd").put("subjectType", "jutter");
        assertTrue(refuses(ir, VERSION + "/target").getMessage().endsWith(": a version with a presentation or with"
                + " initialisation true writes an attribute, so its target MUST name one, and 'Penninggerechtigd' is a"
                + " characteristic of 'jutter'"));
        // a name that is an attribute and a characteristic of the subject type, whatever the version's kind
        for (boolean tag : new boolean[] { false, true }) {
            ir = golden(TOELAGEGRONDEN);
            ObjectNode rule = (ObjectNode) JSON.readTree(golden(PENNINGDREMPEL).at(REGEL).toString());
            ObjectNode v = (ObjectNode) rule.at("/versions/0");
            ((ObjectNode) v.get("target")).put("attribute", "toelage").put("subjectType", "Jutter");
            v.remove("condition");
            if (tag) v.put("presentation", "equation");
            IrPath.rulesList(ir, "rules").add(rule);
            assertTrue(refuses(ir, VERSION + "/target").getMessage().endsWith(": target 'toelage' names both an"
                    + " attribute and a characteristic of 'Jutter': which member a rule writes decides its kind, and"
                    + " this name decides nothing"), "tag " + tag);
        }
    }

    /** WF-028's sentences: the target type of a fact creation is the declared filler of its target role, and a role
     *  that resolves to none leaves the creation without a target. The anchor role is refused in the same words. */
    @Test void a_fact_creation_without_filler_of_the_target_role_is_refused() throws Exception {
        String f = "/rules/factCreations/0/versions/0";
        ObjectNode ir = golden(MASTSEIN);
        ((ObjectNode) ir.at(f)).put("targetRole", "baken");
        assertTrue(refuses(ir, f + "/targetRole").getMessage().endsWith(": 'baken' is not a role of 'vakbeseining'"));
        // before the fix round of the L4 landing this loaded, and both branches then refused the creation as
        // unresolved ("feitcreatie (onopgelost)"), a sentence that names neither the field nor the relation
        ir = golden(MASTSEIN);
        ((ObjectNode) ir.at(f)).put("anchorRole", "baken");
        assertTrue(refuses(ir, f + "/anchorRole").getMessage().endsWith(": 'baken' is not a role of 'vakbeseining'"));
        ir = golden(MASTSEIN);
        ((ObjectNode) ir.at(f)).put("factType", "bakenbeseining");
        assertTrue(refuses(ir, f + "/factType").getMessage()
                .endsWith(": created fact type 'bakenbeseining' is not declared"));
    }

    // ---------------- read back one to one ----------------

    @Test void each_version_becomes_a_record_with_the_name_of_a_version() throws Exception {
        ObjectNode ir = golden(JUTHEFFING);
        RegelSpraakModel m = IrLoader.load(ir);
        List<String> names = new ArrayList<>();
        for (ResolvedRegel r : m.resolvedRegels) names.add(r.naam());
        assertEquals(List.of("jutheffing naar jaartarief #1", "jutheffing naar jaartarief #2",
                "registratiegetal bij aangifte"), names, "two versions carry the suffix, a form of one its bare name");
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
        assertEquals(List.of("art. 3 Jutterswet"), m.resolvedRegels.get(0).statuteAnnotations());
        assertEquals(List.of("art. 3 Jutterswet"), m.resolvedRegels.get(1).statuteAnnotations());
        assertEquals(List.of(), m.resolvedRegels.get(2).statuteAnnotations());

        ir = golden(HARDHEIDSOVERSCHOT);
        List<String> annotations = new ArrayList<>();
        ir.at("/rules/rules/1/statuteAnnotations").forEach(a -> annotations.add(a.asText()));
        assertEquals(2, annotations.size());
        assertEquals(annotations, IrLoader.load(ir).resolvedRegels.get(1).statuteAnnotations());
    }

    @Test void the_kind_of_a_version_is_its_structure() throws Exception {
        assertEquals(ResolvedRegel.Soort.BEREKENING, IrLoader.load(golden(PREMIEVENSTER)).resolvedRegels.get(0).soort());
        assertEquals(ResolvedRegel.Soort.GELIJKSTELLING, IrLoader.load(golden(KEURSTAAT)).resolvedRegels.get(3).soort());
        assertEquals(ResolvedRegel.Soort.KENMERKTOEKENNING,
                IrLoader.load(golden(PENNINGDREMPEL)).resolvedRegels.get(0).soort());
        assertEquals(ResolvedRegel.Soort.INITIALISATIE, IrLoader.load(golden(WAARDERING)).resolvedRegels.get(1).soort());
        // the tag is read as written: equation on the same version is the other computing kind
        ObjectNode ir = golden(PREMIEVENSTER);
        ((ObjectNode) ir.at(VERSION)).put("presentation", "equation");
        assertEquals(ResolvedRegel.Soort.GELIJKSTELLING, IrLoader.load(ir).resolvedRegels.get(0).soort());
    }

    @Test void a_fact_creation_reads_its_target_kind_and_its_path() throws Exception {
        ObjectNode ir = golden(MASTSEIN);
        ResolvedFactCreation fc = IrLoader.load(ir).feitCreaties.get(0);
        assertEquals("Sein", fc.doel().naam, "the declared filler of targetRole 'vaksein' in 'vakbeseining'");
        assertEquals("Kustvak", fc.anchor().naam);
        assertEquals("vakbeseining", fc.feittype());
        assertEquals("seinvak", fc.anchorRole());
        assertEquals("vaksein", fc.targetRole());
        List<RoleBindingRef> steps = new ArrayList<>();
        for (ResolvedFactCreation.PathStep t : fc.pathSteps()) steps.add(t.roleBinding());
        assertEquals(List.of(new RoleBindingRef("kustligging", "post"), new RoleBindingRef("mastvoering", "mast")),
                steps, "every hop of the path but the last is a step");
        assertEquals(new RoleBindingRef("seinvoering", "sein"), fc.path(), "and the last is the join");
        // one hop is a join without steps
        ArrayNode path = (ArrayNode) ir.at("/rules/factCreations/0/versions/0/path");
        path.remove(0);
        path.remove(0);
        fc = IrLoader.load(ir).feitCreaties.get(0);
        assertEquals(List.of(), fc.pathSteps());
        assertEquals(new RoleBindingRef("seinvoering", "sein"), fc.path());
    }

    @Test void the_one_list_of_uniquenesses_splits_on_the_count_of_sources() throws Exception {
        ObjectNode ir = golden(KUSTREGISTER);
        RegelSpraakModel m = IrLoader.load(ir);
        assertEquals(List.of("eenmalig kaveletiket", "eenmalige sectiecode"),
                m.uniciteiten.stream().map(ResolvedUniqueness::naam).toList());
        assertEquals(List.of("eenmalig vondstnummer", "eenmalige bakencode"),
                m.uniciteitUnies.stream().map(ResolvedUniquenessUnion::naam).toList());
        ResolvedUniqueness single = m.uniciteiten.get(0);
        assertEquals("Kavel", single.subject().naam);
        assertEquals(List.of("etiket"), single.keys());
        assertEquals(new RoleBindingRef("kavelindeling", "gemeente"), single.scopeBinding());
        assertNull(single.scopeField());
        ResolvedUniquenessUnion union = m.uniciteitUnies.get(1);
        assertEquals(List.of("Baken", "Baken"), union.sources().stream().map(b -> b.type().naam).toList());
        assertEquals(List.of(List.of("landcode"), List.of("zeecode")),
                union.sources().stream().map(ResolvedUniquenessUnion.Bron::keys).toList());
        // the order of the one list carries no meaning: the unions first split the same
        ArrayNode list = IrPath.rulesList(ir, "uniquenesses");
        ArrayNode reversed = JSON.createArrayNode();
        for (int i = list.size() - 1; i >= 0; i--) reversed.add(list.get(i));
        IrPath.rules(ir).set("uniquenesses", reversed);
        m = IrLoader.load(ir);
        assertEquals(List.of("eenmalige sectiecode", "eenmalig kaveletiket"),
                m.uniciteiten.stream().map(ResolvedUniqueness::naam).toList());
        assertEquals(List.of("eenmalige bakencode", "eenmalig vondstnummer"),
                m.uniciteitUnies.stream().map(ResolvedUniquenessUnion::naam).toList());
    }

    @Test void both_function_nodes_become_the_one_function_record() throws Exception {
        Expr.FunctionCall sum = assertInstanceOf(Expr.FunctionCall.class,
                IrLoader.load(golden(BOOTBERGING)).resolvedRegels.get(1).waarde());
        assertEquals(Expr.FunctionKind.SOM, sum.kind());
        assertEquals("Vondst", sum.elementObjectType().naam);
        assertNotNull(sum.emptyFallback(), "emptyFallback");
        assertNull(sum.granulariteit(), "a fold carries no granularity");
        Expr.FunctionCall part = assertInstanceOf(Expr.FunctionCall.class,
                IrLoader.load(golden(PACHTVENSTER)).resolvedRegels.get(0).waarde());
        assertEquals(Expr.FunctionKind.TIJDSEVENREDIG_DEEL, part.kind());
        assertEquals(Spelling.GRANULARITY.waarde("BuiltinCall.granularity",
                        golden(PACHTVENSTER).at(VALUE + "/granularity").asText()),
                part.granulariteit());
        assertNotNull(part.filter(), "filter");
        assertNull(part.elementObjectType(), "a builtin carries none of the fold's fields");
        assertNull(part.emptyFallback());
        assertNull(part.dimensieSelectie());
    }

    @Test void a_literal_keeps_the_lexeme_of_its_variant() throws Exception {
        assertEquals("1", ((Expr.Literal) IrLoader.load(golden(PREMIEVENSTER)).resolvedRegels.get(0).waarde()).waarde());
        assertEquals("waar",
                ((Expr.Literal) IrLoader.load(golden(PENNINGDREMPEL)).resolvedRegels.get(0).waarde()).waarde());
        assertEquals("gaaf", ((Expr.Literal) IrLoader.load(golden(KEURSTAAT)).resolvedRegels.get(3).waarde()).waarde());
        ObjectNode ir = golden(PREMIEVENSTER);
        set(ir, VALUE, "{\"node\":\"Literal\",\"value\":{\"boolean\":false},\"type\":{\"node\":\"Boolean\"},"
                + "\"source\":{\"snippet\":\"onwaar\"}}");
        assertEquals("onwaar", ((Expr.Literal) IrLoader.load(ir).resolvedRegels.get(0).waarde()).waarde());
        // the text variant admits the empty string: its spelling is the case format's
        ir = golden(PREMIEVENSTER);
        set(ir, VALUE, "{\"node\":\"Literal\",\"value\":{\"text\":\"\"},\"type\":{\"node\":\"Text\"},\"source\":{\"snippet\":\"''\"}}");
        assertEquals("", ((Expr.Literal) IrLoader.load(ir).resolvedRegels.get(0).waarde()).waarde());
    }

    @Test void the_bound_day_is_the_constant_the_day() throws Exception {
        ObjectNode ir = golden(PREMIEVENSTER);
        set(ir, VALUE, "{\"node\":\"BoundDay\",\"type\":" + DAG + ",\"source\":{\"snippet\":\"de dag\"}}");
        Expr.Constante c = assertInstanceOf(Expr.Constante.class, IrLoader.load(ir).resolvedRegels.get(0).waarde());
        assertEquals("de dag", c.naam());
        assertInstanceOf(Datatype.DatumInDagen.class, c.type());
        assertNotNull(IrLoader.load(golden(HAVENROOSTER)), "a day-kind definition that compares the bound day loads");
    }

    // ---------------- helpers ----------------

    private static ObjectNode contextValue(String name) {
        ObjectNode c = JSON.createObjectNode();
        c.put("node", "ContextValue").put("name", name);
        c.set("type", readJson(DAG));
        c.set("source", IrPath.sourceReference("de " + name));
        return c;
    }

    private static ObjectNode nameSet(String name) throws Exception {
        ObjectNode ir = golden(PREMIEVENSTER);
        ((ObjectNode) ir.at(REGEL)).put("name", name);
        return ir;
    }

    private static JsonNode readJson(String json) {
        try {
            return JSON.readTree(json);
        } catch (Exception e) {
            throw new AssertionError(json, e);
        }
    }

    private static void set(ObjectNode ir, String path, String json) throws Exception {
        int slash = path.lastIndexOf('/');
        JsonNode parent = ir.at(path.substring(0, slash));
        if (!(parent instanceof ObjectNode o)) throw new AssertionError("no object at " + path.substring(0, slash));
        o.set(path.substring(slash + 1), JSON.readTree(json));
    }

    private static IrLoader.Documentfout refuses(JsonNode ir, String site) {
        IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.loadAll(ir),
                "verwacht een documentfout op " + site);
        assertEquals(site, e.site(), e.getMessage());
        return e;
    }

    private static void unknownField(JsonNode ir, String site, String node) {
        IrLoader.Documentfout e = refuses(ir, site);
        assertTrue(e.getMessage().endsWith(": unknown field on " + node), e.getMessage());
    }

    private static ObjectNode golden(String name) throws Exception {
        Path ir = CORPUS.resolve(name).resolve("ir.json");
        assertTrue(Files.exists(ir), "golden ir.json present: " + ir.toAbsolutePath());
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(ir));
        IrLoader.loadAll(doc);   // the untwisted golden loads, so the refusal below is the twist's alone
        return doc;
    }
}
