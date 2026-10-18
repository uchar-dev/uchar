package dev.uchar.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import dev.uchar.regelspraak.ir.Conditie;
import dev.uchar.regelspraak.ir.Default;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.ir.ResolvedDagsoort;
import dev.uchar.regelspraak.ir.ResolvedObjectCreatie;
import dev.uchar.regelspraak.ir.ResolvedRegelGroep;
import dev.uchar.regelspraak.ir.ResolvedStartpunt;
import dev.uchar.regelspraak.ir.Spelling;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.RolBindingRef;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The loader on the renamed wire (L5 of the v1 wire migration, deltas B6 and B10): every node, key and list under
 * {@code rules} carries its IR-SPEC name and every enumeration value its lowerCamel spelling, read through the one
 * translation table ({@link Spelling}) onto records that keep their v0 form. One test per family of what the v0 wire
 * wrote and the L5 wire does not: a v0 node name, a v0 key on the node that replaced it, a v0 list key under
 * {@code rules}, a v0 enumeration value, and a null on a field the renaming made optional — each refused by name, in
 * the shared OCaml loader's words where it has them and in this loader's where the refusal is generic, at its JSON
 * pointer. Beside them the three values the schema does not admit and a verifier rule names — the quantifier
 * {@code the} on a QuantifierFold (WF-108 (a)) and the severities {@code error}, {@code info} and null (WF-113 (a)) —
 * refused at load in the rule's sentence, as the shared loader refuses them for an executor; the one node that keeps
 * its v0 shape, {@code Regelversie}; and the other side of every row: the L5 fields land in the records, so none is
 * loaded and ignored.
 *
 * <p>Positions come from a sweep over the corpus goldens this backend loads (the first of each node kind, of each
 * version record and of each enumeration field, in document order), and from rows at a rule's value or condition for
 * the kinds no loadable golden carries. Every refusal is an {@link IrLoader.Documentfout}: a document in a v0 spelling
 * is no document of this format, never a form this backend does not carry.
 */
class RenamingRefusalWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** Rule 0 attributes a characteristic under a condition; its value and condition take the synthetic rows. */
    private static final String PENNINGDREMPEL = "jutterswet-penningdrempel";
    /** Rule 0 computes an attribute of a Vondst from a Number literal: its value takes the expression rows that
     *  must load, since a characteristic attribution admits the constant true alone (WF-022). */
    private static final String PREMIEVENSTER = "jutterswet-premievenster";
    private static final String W = "/rules/rules/0/versions/0/value", V = "/rules/rules/0/versions/0/condition";

    /** The renaming table: every v0 node name that left the wire, and the node that replaced it. {@code Onopgelost}
     *  stands for both markers. */
    private static final Map<String, String> NODES = new LinkedHashMap<>();
    static {
        String[][] rows = { { "Tekst", "Text" }, { "Booleaan", "Boolean" }, { "Enumeratie", "Enumeration" },
                { "Feittype", "FactType" }, { "Rol", "Role" }, { "Dimensie", "Dimension" }, { "Tijdlijn", "Timeline" },
                { "RolBinding", "RoleBinding" }, { "Geldigheid", "Validity" }, { "Toekenning", "MemberInit" },
                { "ResolvedDagsoort", "DayKindDefinition" }, { "ResolvedStartpunt", "StartPointDetermination" },
                { "ResolvedRegelGroep", "RuleGroup" }, { "Binair", "Binary" }, { "Afgerond", "Rounding" },
                { "Begrensd", "Clamp" }, { "TekstReeks", "TextComposition" }, { "Letterlijk", "LiteralChunk" },
                { "Ingevoegd", "EmbeddedExpression" }, { "AttribuutRef", "AttributeReference" },
                { "DimensieCel", "DimensionCellRead" }, { "ParameterRef", "ParameterReference" },
                { "VariabeleRef", "VariableReference" }, { "Onopgelost", "Unresolved" },
                { "DimensieSelectie", "DimensionSelection" }, { "Vergelijking", "Comparison" },
                { "Predicaat", "Predicate" }, { "Kenmerk", "CharacteristicCheck" }, { "Periode", "TimeWindow" },
                { "Samengesteld", "Compound" }, { "Kwantorvouw", "QuantifierFold" },
                { "GeheelPeriode", "WholePeriod" } };
        for (String[] r : rows) NODES.put(r[0], r[1]);
    }

    /** The sweep's name for the exceptions record of a default, which carries no node of its own. */
    private static final String UITZONDERINGEN = "Default exceptions record";

    /** Per node (and per record without a node: the exceptions of a default), the v0 keys the renaming replaced. */
    private static final Map<String, List<String>> V0_KEYS = new LinkedHashMap<>();
    static {
        V0_KEYS.put("Enumeration", List.of("domein", "waarden"));
        V0_KEYS.put("FactType", List.of("naam", "wederkerig", "rollen"));
        V0_KEYS.put("Role", List.of("naam", "objecttypeNaam", "meervoud", "meervoudig"));
        V0_KEYS.put("Dimension", List.of("naam", "naamMeervoud", "voorzetsel", "waarden"));
        V0_KEYS.put("Timeline", List.of("naam", "variabelStartpunt", "granulariteit", "aantal"));
        V0_KEYS.put("RoleBinding", List.of("feittype", "rol"));
        V0_KEYS.put("Validity", List.of("vanaf", "tot"));
        V0_KEYS.put("MemberInit", List.of("attribuut", "waarde", "kenmerk"));
        V0_KEYS.put("DayKindDefinition", List.of("naam", "voorwaarde", "geldigheid"));
        V0_KEYS.put("StartPointDetermination", List.of("tijdlijn", "objecttype", "lid"));
        V0_KEYS.put("RuleGroup", List.of("naam", "recursief", "leden"));
        V0_KEYS.put("PrioritizedDefaults", List.of("naam", "onderwerp", "kenmerk", "attribuut", "ongeordend",
                "geldigheid"));
        V0_KEYS.put("Default", List.of("besluit", "voorwaarde", "consequentie", "initialisatie", "excepties"));
        V0_KEYS.put(UITZONDERINGEN, List.of("ongeordend", "leden"));
        V0_KEYS.put("Binary", List.of("links", "rechts"));
        V0_KEYS.put("Rounding", List.of("basis", "modus", "decimalen"));
        V0_KEYS.put("Clamp", List.of("basis"));
        V0_KEYS.put("TextComposition", List.of("delen"));
        V0_KEYS.put("LiteralChunk", List.of("tekst"));
        V0_KEYS.put("AttributeReference", List.of("attribuut", "objecttype", "navigatieBindingen"));
        V0_KEYS.put("DimensionCellRead", List.of("attribuut"));
        V0_KEYS.put("VariableReference", List.of("naam"));
        V0_KEYS.put("TemporalRead", List.of("basis", "datum"));
        V0_KEYS.put("DimensionSelection", List.of("dimensie"));
        V0_KEYS.put("Comparison", List.of("links", "rechts", "periode"));
        V0_KEYS.put("Predicate", List.of("soort", "dagsoort", "negatie", "cijfers", "periode"));
        V0_KEYS.put("CharacteristicCheck", List.of("kenmerk", "negatie", "eigenaar", "periode", "navigatieBinding"));
        V0_KEYS.put("TimeWindow", List.of("vanaf", "tot", "totInclusief"));
        V0_KEYS.put("Compound", List.of("kwantor", "drempel", "onderdelen"));
        V0_KEYS.put("QuantifierFold", List.of("kwantor", "drempel", "elementObjecttype", "toets", "bronRol", "bronPad"));
        V0_KEYS.put("WholePeriod", List.of("jaar", "niet"));
    }

    /** The fields the renaming made optional: a v0 null there is absent on the L5 wire, and a null is refused. */
    private static final List<String> D5A = List.of("Validity.from", "Validity.to", "Role.plural",
            "Comparison.wholePeriod", "CharacteristicCheck.wholePeriod", "CharacteristicCheck.host", "Default.decision",
            "QuantifierFold.filter", "PrioritizedDefaults.characteristic", "Predicate.wholePeriod", "Predicate.dayKind",
            "Default.justification", "TimeWindow.to", "Clamp.maximum", "Clamp.minimum", "Dimension.plural",
            "Dimension.preposition", "TimeWindow.from");

    private static final String NULL_SENTENCE = "an optional field is written whole or not at all, and null is neither";

    // ================= the families of what the v0 wire wrote =================

    /** Every v0 node name at a position of its L5 node: at a dispatch the shared loader's "unknown … node", at a
     *  fixed position its "expected …, got …" — both naming the v0 name. */
    @Test void every_v0_node_name_is_refused_by_name() throws Exception {
        Set<String> expr = Set.of("Binair", "Afgerond", "Begrensd", "TekstReeks", "AttribuutRef", "DimensieCel",
                "ParameterRef", "VariabeleRef");
        Set<String> conditionNodes = Set.of("Vergelijking", "Predicaat", "Kenmerk", "Periode", "Samengesteld", "Kwantorvouw");
        Set<String> datatype = Set.of("Tekst", "Booleaan", "Enumeratie");
        Set<String> part = Set.of("Letterlijk", "Ingevoegd");
        int rows = 0;
        for (Map.Entry<String, String> k : NODES.entrySet()) {
            String v0 = k.getKey(), l5 = k.getValue();
            List<Site> sites = v0.equals("Onopgelost")
                    ? List.of(synthetic("Unresolved (expression)"), synthetic("Unresolved (condition)"))
                    : List.of(site(l5));
            for (Site p : sites) {
                ObjectNode doc = p.document();
                node(doc, p.path()).put("node", v0);
                String sentence = expr.contains(v0) || v0.equals("Onopgelost") && p.path().equals(W)
                        ? "unknown expr node \"" + v0 + "\""
                        : conditionNodes.contains(v0) || v0.equals("Onopgelost") ? "unknown condition node \"" + v0 + "\""
                        : datatype.contains(v0) ? "unknown datatype node \"" + v0 + "\""
                        : part.contains(v0) ? "unknown TextCompositionPart node \"" + v0 + "\""
                        : "expected \"" + l5 + "\", got \"" + v0 + "\"";
                IrLoader.Documentfout e = refuses(doc, v0);
                assertEquals(p.path() + "/node", e.site(), v0 + ": " + e.getMessage());
                assertTrue(e.getMessage().endsWith(": " + sentence), v0 + ": " + e.getMessage());
                rows++;
            }
        }
        assertEquals(33, rows, "32 v0 node names, the marker at both of its positions");
    }

    /** Every v0 key on the node that replaced it is an unknown field, named at its pointer — a reader never takes a
     *  v0 name beside the L5 one for a second spelling of the field. */
    @Test void every_v0_key_on_its_l5_node_is_an_unknown_field() throws Exception {
        int rows = 0;
        for (Map.Entry<String, List<String>> k : V0_KEYS.entrySet()) {
            String kind = k.getKey();
            for (String key : k.getValue()) {
                Site p = kind.equals("TemporalRead") ? synthetic("TemporalRead") : site(kind);
                ObjectNode doc = p.document();
                node(doc, p.path()).put(key, 1);
                IrLoader.Documentfout e = refuses(doc, kind + "." + key);
                assertEquals(p.path() + "/" + key, e.site(), kind + "." + key + ": " + e.getMessage());
                String sentence = kind.equals(UITZONDERINGEN) ? ": unknown field" : ": unknown field on " + kind;
                assertTrue(e.getMessage().endsWith(sentence), kind + "." + key + ": " + e.getMessage());
                rows++;
            }
        }
        // the sum of the table above: 31 records, 89 renamed keys (the temporal read's two included)
        assertEquals(89, rows, "the renamed keys of every node the loader reads");
        // the temporal read is a form this backend does not carry: written in its L5 shape it is refused as that
        ObjectNode doc = synthetic("TemporalRead").document();
        IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class, () -> IrLoader.loadAll(doc));
        assertEquals("Expr:TemporalRead", r.form(), r.getMessage());
    }

    @Test void the_v0_lists_under_rules_are_unknown_fields() throws Exception {
        String[][] lists = { { "rules", "resolvedRegels" }, { "dayKindDefinitions", "resolvedDagsoorten" },
                { "objectCreations", "objectCreaties" }, { "allocations", "verdelingen" },
                { "consistencies", "consistenties" }, { "uniquenesses", "uniciteiten" },
                { "factCreations", "feitCreaties" }, { "startPointDeterminations", "resolvedStartpunten" },
                { "ruleGroups", "regelGroepen" } };
        for (String[] l : lists) {
            ObjectNode doc = golden(PENNINGDREMPEL);
            ObjectNode rules = (ObjectNode) doc.get("rules");
            ObjectNode renamed = JSON.createObjectNode();
            rules.properties().forEach(e -> renamed.set(e.getKey().equals(l[0]) ? l[1] : e.getKey(), e.getValue()));
            doc.set("rules", renamed);
            IrLoader.Documentfout e = refuses(doc, l[1]);
            assertEquals("/rules/" + l[1], e.site(), e.getMessage());
            assertTrue(e.getMessage().endsWith(": unknown field"), e.getMessage());
        }
    }

    /** Every v0 value of every family, at every place of the family a loadable golden (or a row) carries: the shared
     *  loader's "unknown <field> value", the field being the last segment of the place. The v0 spelling of a value is
     *  its record constant's name, which the v0 emitter wrote; the one exception is the whole-period granularity, a
     *  boolean on the v0 wire, and the context-value name, a free string WF-098 closes. The date precision spelled
     *  the same before L5 and has no v0 value to refuse. */
    @Test void every_v0_enum_value_is_refused_by_name() throws Exception {
        int rows = 0;
        Set<String> covered = new java.util.TreeSet<>();
        for (Spelling.Familie<?> f : Spelling.FAMILIES) {
            if (f == Spelling.DATE_PRECISION) continue;
            List<Object> v0 = new ArrayList<>();
            for (Object k : f.rijen().keySet()) v0.add(k);
            for (Object k : f.geweigerd().keySet()) v0.add(k);
            for (String place : f.plaatsen()) {
                Site p = place.equals("ContextValue.name") ? synthetic("ContextValue")
                        : place.equals("QuantifierFold.quantifier") ? site("QuantifierFold")
                        : siteWithField(place);
                String field = place.substring(place.indexOf('.') + 1);
                for (Object k : v0) {
                    ObjectNode doc = p.document();
                    JsonNode value = k instanceof Boolean b ? BooleanNode.valueOf(b)
                            : TextNode.valueOf(k instanceof Enum<?> e ? e.name() : k.toString());
                    node(doc, p.path()).set(field, value);
                    IrLoader.Documentfout e = refuses(doc, place + " " + value);
                    assertEquals(p.path() + "/" + field, e.site(), place + " " + value + ": " + e.getMessage());
                    String sentence = place.equals("ContextValue.name")
                            ? "context value '" + k + "' is not one of the two admissible names evaluationDate,"
                                    + " evaluationYear: the context-value name is a closed set, spelled byte for byte"
                            : place.equals("WholePeriod.granularity") ? "expected a string, got " + value
                            : "unknown " + field + " value " + value;
                    assertTrue(e.getMessage().endsWith(": " + sentence), place + " " + value + ": " + e.getMessage());
                    rows++;
                }
                covered.add(place);
            }
        }
        assertEquals(18, covered.size(), "every place of the fifteen families the renaming respelled: " + covered);
        // sign 4, kind 3, granularity 5 x 3 places, presentation 2, method 3, comparison 10, binary 8, rounding 5,
        // aggregation 8, builtin 17, predicate 5, quantifier 6 x 2 places, context value 2, whole period 2, severity 3
        assertEquals(99, rows, "every v0 value at every place of its family");
    }

    /** A null on a field the renaming made optional: absent is what the v0 null was, so a null is neither. */
    @Test void a_null_on_a_field_that_l5_made_optional_is_refused() throws Exception {
        for (String place : D5A) {
            String kind = place.substring(0, place.indexOf('.')), field = place.substring(place.indexOf('.') + 1);
            Site p = site(kind);
            ObjectNode doc = p.document();
            node(doc, p.path()).putNull(field);
            IrLoader.Documentfout e = refuses(doc, place);
            assertEquals(p.path() + "/" + field, e.site(), place + ": " + e.getMessage());
            assertTrue(e.getMessage().endsWith(": " + NULL_SENTENCE), place + ": " + e.getMessage());
        }
    }

    // ================= the values a verifier rule names =================

    /** WF-108 (a) and WF-113 (a): the schema admits none of the three, the verifier loads them so that its rule names
     *  them, and every executor refuses them at load in the rule's sentence — this one too. {@code the} is an ordinary
     *  value of a Compound's quantifier, the bare definite of the dialect. */
    @Test void the_values_that_a_verifier_rule_names_are_refused_on_loading() throws Exception {
        Site fold = site("QuantifierFold");
        ObjectNode doc = fold.document();
        node(doc, fold.path()).put("quantifier", "the");
        IrLoader.Documentfout e = refuses(doc, "the");
        assertEquals(fold.path() + "/quantifier", e.site(), e.getMessage());
        assertTrue(e.getMessage().endsWith(": quantifier fold with quantifier the: the bare definite is a description of"
                + " ONE member, not a quantification over the set — a fold over it has no reading"), e.getMessage());

        Site diagnostic = site("Diagnostic");
        String[][] severity = {
                { "error", "severity error is not a wire severity: an error means the producer refused to write the"
                        + " document, so a document that was written carries none" },
                { "info", "severity info is not a wire severity: the list is the account of what stayed unresolved,"
                        + " and info is tooling talk" },
                { null, "diagnostic without a severity: the one wire severity is warning" } };
        for (String[] r : severity) {
            doc = diagnostic.document();
            if (r[0] == null) node(doc, diagnostic.path()).putNull("severity");
            else node(doc, diagnostic.path()).put("severity", r[0]);
            e = refuses(doc, String.valueOf(r[0]));
            assertEquals(diagnostic.path() + "/severity", e.site(), e.getMessage());
            assertTrue(e.getMessage().endsWith(": " + r[1]), e.getMessage());
        }

        Site compound = site("Compound");
        doc = compound.document();
        node(doc, compound.path()).put("quantifier", "the");
        IrLoader.loadAll(doc);
    }

    /** The one node that keeps its v0 shape until the parity golden of its v1 form lands: its fields load and are
     *  read, its source is the record of every node, and a key of the v1 form is no key of it yet. */
    @Test void rule_status_check_keeps_its_v0_form() throws Exception {
        ObjectNode doc = golden(PENNINGDREMPEL);
        set(doc, V, "{\"node\":\"Regelversie\",\"regel\":\"penningdrempel\",\"inconsistent\":true,"
                + "\"source\":{\"snippet\":\"regelversie penningdrempel is inconsistent\"}}");
        Conditie.Regelversie rv = assertInstanceOf(Conditie.Regelversie.class,
                IrLoader.load(doc).resolvedRegels.get(0).voorwaarde());
        assertEquals("penningdrempel", rv.regel());
        assertTrue(rv.inconsistent());
        assertEquals("regelversie penningdrempel is inconsistent", rv.bron().snippet());
        node(doc, V).put("rule", "penningdrempel");
        IrLoader.Documentfout e = refuses(doc, "Regelversie.rule");
        assertEquals(V + "/rule", e.site(), e.getMessage());
        assertTrue(e.getMessage().endsWith(": unknown field on Regelversie"), e.getMessage());
    }

    /** The whole-period qualifier's granularity is a family of two: year and month read onto the record's flag, and
     *  any other granularity is no value of it, a week included. */
    @Test void the_whole_period_granularity_is_year_or_month() throws Exception {
        Site p = site("WholePeriod");
        for (String[] r : new String[][] { { "year", "true" }, { "month", "false" } }) {
            ObjectNode doc = p.document();
            node(doc, p.path()).put("granularity", r[0]);
            IrLoader.loadAll(doc);
        }
        ObjectNode doc = p.document();
        node(doc, p.path()).put("granularity", "week");
        IrLoader.Documentfout e = refuses(doc, "week");
        assertTrue(e.getMessage().endsWith(": unknown granularity value \"week\""), e.getMessage());
    }

    // ================= the other side: the L5 fields land in the records =================

    /** The fields of a characteristic check: its host (absent: the subject), its whole-period qualifier with its
     *  granularity and its negation, its navigation and its negation. */
    @Test void a_characteristic_check_reads_each_l5_field() throws Exception {
        ObjectNode doc = golden(PENNINGDREMPEL);
        set(doc, V, "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"penninggerechtigd\",\"negated\":true,"
                + "\"host\":\"Jutter\",\"wholePeriod\":{\"node\":\"WholePeriod\",\"granularity\":\"month\",\"negated\":true},"
                + "\"source\":{\"snippet\":\"hij niet gedurende de gehele maand penninggerechtigd is\"},"
                + "\"navigation\":{\"node\":\"RoleBinding\",\"factType\":\"f\",\"role\":\"r\"}}");
        IrLoader.Loaded l = IrLoader.loadAll(doc);
        Conditie.Kenmerk k = assertInstanceOf(Conditie.Kenmerk.class, l.model().resolvedRegels.get(0).voorwaarde());
        assertEquals("penninggerechtigd", k.kenmerk());
        assertTrue(k.negatie());
        assertEquals("Jutter", k.eigenaar().naam);
        assertFalse(k.periode().jaar(), "month is the record's jaar false");
        assertTrue(k.periode().niet());
        assertEquals(List.of(new RolBindingRef("f", "r")), l.navigationBindings().get(k));
        // absent host and qualifier: the subject, no qualifier — the v0 null of the record
        ObjectNode bare = golden(PENNINGDREMPEL);
        set(bare, V, "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"penninggerechtigd\",\"negated\":false,"
                + "\"source\":{\"snippet\":\"hij penninggerechtigd is\"}}");
        Conditie.Kenmerk k2 = (Conditie.Kenmerk) IrLoader.load(bare).resolvedRegels.get(0).voorwaarde();
        assertNull(k2.eigenaar());
        assertNull(k2.periode());
    }

    /** The expression nodes of the renaming, each with every field written: the values land in the record's
     *  components, the enumeration values through the table. */
    @Test void the_expression_nodes_read_each_l5_field() throws Exception {
        String number = "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}";
        String one = "{\"node\":\"Literal\",\"value\":{\"number\":\"1\"},\"type\":" + number
                + ",\"source\":{\"snippet\":\"1\"}}";
        String two = "{\"node\":\"Literal\",\"value\":{\"number\":\"2\"},\"type\":" + number
                + ",\"source\":{\"snippet\":\"2\"}}";
        ObjectNode doc = golden(PREMIEVENSTER);
        set(doc, W, "{\"node\":\"Rounding\",\"base\":{\"node\":\"Clamp\",\"base\":{\"node\":\"Binary\",\"op\":\"times\","
                + "\"left\":" + one + ",\"right\":" + two + ",\"type\":" + number + ",\"source\":{\"snippet\":\"1 maal 2\"}},"
                + "\"minimum\":" + one + ",\"type\":" + number + ",\"source\":{\"snippet\":\"minimaal 1\"}},"
                + "\"mode\":\"towardZero\",\"decimals\":1,\"type\":" + number + ",\"source\":{\"snippet\":\"afgerond\"}}");
        Expr.Afgerond a = assertInstanceOf(Expr.Afgerond.class, IrLoader.load(doc).resolvedRegels.get(0).waarde());
        assertEquals(Expr.AfrondModus.RICHTING_NUL, a.modus());
        assertEquals(1, a.decimalen());
        Expr.Begrensd b = assertInstanceOf(Expr.Begrensd.class, a.basis());
        assertNotNull(b.minimum());
        assertNull(b.maximum(), "an absent bound is no bound on that side");
        Expr.Binair bin = assertInstanceOf(Expr.Binair.class, b.basis());
        assertEquals(Expr.BinOp.MAAL, bin.op());
        assertEquals("1", ((Expr.Literal) bin.links()).waarde());
        assertEquals("2", ((Expr.Literal) bin.rechts()).waarde());

        doc = golden(PREMIEVENSTER);
        set(doc, W, "{\"node\":\"TextComposition\",\"parts\":[{\"node\":\"LiteralChunk\",\"text\":\"vondst \"},"
                + "{\"node\":\"EmbeddedExpression\",\"expr\":" + one + "}],\"type\":{\"node\":\"Text\"},"
                + "\"source\":{\"snippet\":\"vondst 1\"}}");
        Expr.TekstReeks t = assertInstanceOf(Expr.TekstReeks.class, IrLoader.load(doc).resolvedRegels.get(0).waarde());
        assertEquals("vondst ", ((Expr.TekstReeks.Letterlijk) t.delen().get(0)).tekst());
        assertInstanceOf(Expr.Literal.class, ((Expr.TekstReeks.Ingevoegd) t.delen().get(1)).expr());

        // the name is read, and names the node the loader refuses (WF-034)
        doc = golden(PREMIEVENSTER);
        set(doc, W, "{\"node\":\"VariableReference\",\"name\":\"x\",\"source\":{\"snippet\":\"x\"}}");
        ObjectNode varDoc = doc;
        IrLoader.Documentfout vr = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.load(varDoc));
        assertEquals(W, vr.site(), vr.getMessage());
        assertTrue(vr.getMessage().endsWith(": " + IrLoader.variableReferenceRefusal("x")), vr.getMessage());
    }

    /** The condition nodes of the renaming, each with every field written. */
    @Test void the_condition_nodes_read_each_l5_field() throws Exception {
        String number = "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}";
        String one = "{\"node\":\"Literal\",\"value\":{\"number\":\"1\"},\"type\":" + number
                + ",\"source\":{\"snippet\":\"1\"}}";
        String wholeYear = "{\"node\":\"WholePeriod\",\"granularity\":\"year\",\"negated\":false}";
        ObjectNode doc = golden(PENNINGDREMPEL);
        set(doc, V, "{\"node\":\"Compound\",\"quantifier\":\"atLeast\",\"threshold\":1,\"parts\":["
                + "{\"node\":\"Comparison\",\"op\":\"lessOrEqual\",\"left\":" + one + ",\"right\":" + one + ",\"wholePeriod\":"
                + wholeYear + ",\"source\":{\"snippet\":\"a\"}},"
                + "{\"node\":\"Predicate\",\"kind\":\"dayKind\",\"operand\":" + one + ",\"dayKind\":\"feestdag\","
                + "\"negated\":true,\"digits\":3,\"source\":{\"snippet\":\"b\"}},"
                + "{\"node\":\"TimeWindow\",\"to\":" + one + ",\"toInclusive\":true,\"source\":{\"snippet\":\"c\"}}],"
                + "\"source\":{\"snippet\":\"ten minste een\"}}");
        Conditie.Samengesteld s = assertInstanceOf(Conditie.Samengesteld.class,
                IrLoader.load(doc).resolvedRegels.get(0).voorwaarde());
        assertEquals(Conditie.Kwantor.TEN_MINSTE, s.kwantor());
        assertEquals(1, s.drempel());
        Conditie.Vergelijking v = assertInstanceOf(Conditie.Vergelijking.class, s.onderdelen().get(0));
        assertEquals(Conditie.Operator.KLEINER_OF_GELIJK, v.op());
        assertNotNull(v.links());
        assertNotNull(v.rechts());
        assertTrue(v.periode().jaar());
        Conditie.Predicaat p = assertInstanceOf(Conditie.Predicaat.class, s.onderdelen().get(1));
        assertEquals(Conditie.Predicaatsoort.DAGSOORT, p.soort());
        assertEquals("feestdag", p.dagsoort());
        assertTrue(p.negatie());
        assertEquals(3, p.cijfers());
        Conditie.Periode w = assertInstanceOf(Conditie.Periode.class, s.onderdelen().get(2));
        assertNull(w.vanaf(), "an absent bound is open on that side");
        assertNotNull(w.tot());
        assertTrue(w.totInclusief());
    }

    /** The rule-layer nodes of the renaming, over every loadable golden that carries them: each wire field equals the
     *  record component it lands in, an absent one the record's null. */
    @Test void the_rule_layer_reads_each_l5_field() throws Exception {
        int dayKinds = 0, startPoints = 0, groups = 0, initialisations = 0, defaults = 0;
        for (Path path : goldens()) {
            ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(path));
            IrLoader.Loaded l;
            try { l = IrLoader.loadAll(doc); } catch (IrLoader.Refusal r) { continue; }
            RegelSpraakModel m = l.model();
            JsonNode rules = doc.get("rules");
            for (int i = 0; i < rules.get("dayKindDefinitions").size(); i++, dayKinds++) {
                JsonNode w = rules.get("dayKindDefinitions").get(i);
                ResolvedDagsoort d = m.resolvedDagsoorten.get(i);
                assertEquals(w.get("dayKind").asText(null), d.naam(), path.toString());
                assertEquals(w.get("condition").isNull(), d.voorwaarde() == null, path.toString());
            }
            for (int i = 0; i < rules.get("startPointDeterminations").size(); i++, startPoints++) {
                JsonNode w = rules.get("startPointDeterminations").get(i);
                ResolvedStartpunt sp = m.resolvedStartpunten.get(i);
                assertEquals(w.get("timeline").asText(null), sp.tijdlijn(), path.toString());
                assertEquals(w.get("objectType").asText(null), sp.objecttype(), path.toString());
                assertEquals(w.get("member").asText(null), sp.lid(), path.toString());
            }
            for (int i = 0; i < rules.get("ruleGroups").size(); i++, groups++) {
                JsonNode w = rules.get("ruleGroups").get(i);
                ResolvedRegelGroep g = m.regelGroepen.get(i);
                assertEquals(w.get("name").asText(null), g.naam(), path.toString());
                assertEquals(w.get("recursive").asBoolean(), g.recursief(), path.toString());
                assertEquals(w.get("members").size(), g.leden().size(), path.toString());
            }
            int oc = 0;
            for (JsonNode form : rules.get("objectCreations"))
                for (JsonNode version : form.get("versions")) {
                    ResolvedObjectCreatie c = m.objectCreaties.get(oc++);
                    for (int j = 0; j < version.get("initialisations").size(); j++, initialisations++) {
                        JsonNode w = version.get("initialisations").get(j);
                        ResolvedObjectCreatie.Toekenning t = c.toekenningen().get(j);
                        assertEquals(w.get("member").asText(null), t.attribuut(), path.toString());
                        assertEquals(w.get("characteristic").asBoolean(), t.kenmerk(), path.toString());
                        assertEquals(w.get("value").isNull(), t.waarde() == null, path.toString());
                    }
                }
            JsonNode pds = rules.get("prioritizedDefaults");
            for (int i = 0; pds != null && i < pds.size(); i++) {
                JsonNode w = pds.get(i);
                PrioritizedDefaults pd = l.prioritizedDefaults().get(i);
                assertEquals(w.get("name").asText(null), pd.naam(), path.toString());
                assertEquals(w.get("subject").asText(), pd.onderwerp(), path.toString());
                assertEquals(w.has("characteristic") ? w.get("characteristic").asText() : null, pd.kenmerk(),
                        path.toString());
                assertEquals(w.has("attribute") ? w.get("attribute").asText() : null, pd.attribuut(), path.toString());
                assertEquals(w.path("unordered").asBoolean(false), pd.ongeordend(), path.toString());
                defaults += compare(w.get("defaults"), pd.defaults(), path.toString());
            }
        }
        // measured 2026-09-25 on L5: 7 day-kind definitions, 1 start-point determination, 4 rule groups, 16 member
        // initialisations, 83 defaults
        assertTrue(dayKinds >= 7 && startPoints >= 1 && groups >= 4 && initialisations >= 16 && defaults >= 83,
                "the corpus carries each rule-layer node: " + dayKinds + " " + startPoints + " " + groups + " "
                        + initialisations + " " + defaults);
    }

    /** A list of defaults against its records, the exception groups included; the number of defaults compared. */
    private static int compare(JsonNode wire, List<Default> records, String where) {
        assertEquals(wire.size(), records.size(), where);
        int n = 0;
        for (int i = 0; i < wire.size(); i++, n++) {
            JsonNode w = wire.get(i);
            Default d = records.get(i);
            assertEquals(w.has("decision") ? w.get("decision").asBoolean() : null, d.besluit(), where);
            assertEquals(w.has("justification"), d.voorwaarde() != null, where);
            assertEquals(w.has("consequence"), d.consequentie() != null, where);
            assertEquals(w.path("initialisation").asBoolean(false), d.initialisatie(), where);
            assertEquals(w.has("exceptions"), d.excepties() != null, where);
            if (w.has("exceptions")) {
                assertEquals(w.get("exceptions").get("unordered").asBoolean(), d.excepties().ongeordend(), where);
                JsonNode members = w.get("exceptions").get("members");
                assertEquals(members.size(), d.excepties().leden().size(), where);
                for (int j = 0; j < members.size(); j++)
                    n += compare(members.get(j).get("defaults"), d.excepties().leden().get(j).defaults(), where);
            }
        }
        return n;
    }

    // ================= positions =================

    /** A position in a document: the golden it is read from (or a row built on {@link #PENNINGDREMPEL}) and the
     *  pointer of the node. */
    private record Site(Path ir, String path, String set, String json) {
        ObjectNode document() throws Exception {
            ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(ir));
            if (set != null) RenamingRefusalWitnessTest.set(doc, set, json);
            return doc;
        }
    }

    /** The first position of each node kind (a version record under its form's kind, the exceptions of a default as
     *  "Default.exceptions") and of each field of a node ("Kind.field"), over the goldens this backend loads. */
    private static final Map<String, Site> FIRST = new TreeMap<>();

    private static synchronized Map<String, Site> first() throws Exception {
        if (!FIRST.isEmpty()) return FIRST;
        for (Path ir : goldens()) {
            JsonNode doc = JSON.readTree(Files.readString(ir));
            try { IrLoader.loadAll(doc); } catch (IrLoader.Refusal r) { continue; }
            collect(doc, "", ir);
        }
        return FIRST;
    }

    private static void collect(JsonNode n, String path, Path ir) {
        if (n instanceof ObjectNode o) {
            JsonNode kind = o.get("node");
            // a rule target is always an attribute reference, refused there as the target it is, not by kind
            if (kind != null && kind.isTextual() && !path.endsWith("/target")) {
                FIRST.putIfAbsent(kind.asText(), new Site(ir, path, null, null));
                o.fieldNames().forEachRemaining(v -> FIRST.putIfAbsent(kind.asText() + "." + v,
                        new Site(ir, path, null, null)));
                if (o.has("exceptions")) FIRST.putIfAbsent(UITZONDERINGEN, new Site(ir, path + "/exceptions", null, null));
            }
            if (kind != null && o.path("versions").isArray())
                for (int i = 0; i < o.get("versions").size(); i++) {
                    String vs = kind.asText() + "Version";
                    Site p = new Site(ir, path + "/versions/" + i, null, null);
                    FIRST.putIfAbsent(vs, p);
                    o.get("versions").get(i).fieldNames().forEachRemaining(v -> FIRST.putIfAbsent(vs + "." + v, p));
                }
            o.properties().forEach(e -> collect(e.getValue(),
                    path + "/" + e.getKey().replace("~", "~0").replace("/", "~1"), ir));
        } else if (n instanceof ArrayNode a) {
            for (int i = 0; i < a.size(); i++) collect(a.get(i), path + "/" + i, ir);
        }
    }

    /** The first position of a node kind: a loadable golden's, or the row of a kind no loadable golden carries. */
    private static Site site(String kind) throws Exception {
        Site p = first().get(kind);
        if (p != null) return p;
        return synthetic(kind);
    }

    /** The first position of a node that carries {@code place}'s field ("Kind.field"). */
    private static Site siteWithField(String place) throws Exception {
        Site p = first().get(place);
        if (p == null) fail("geen laadbare golden draagt " + place);
        return p;
    }

    /** The rows at rule 0 of {@link #PENNINGDREMPEL} for the kinds no loadable golden carries. */
    private static Site synthetic(String kind) throws Exception {
        Path ir = CORPUS.resolve(PENNINGDREMPEL).resolve("ir.json");
        String literal = "{\"node\":\"Literal\",\"value\":{\"text\":\"x\"},\"type\":{\"node\":\"Text\"},"
                + "\"source\":{\"snippet\":\"x\"}}";
        String date = "{\"node\":\"Literal\",\"value\":{\"date\":\"2024-01-01\"},\"type\":{\"node\":\"Date\","
                + "\"precision\":\"day\"},\"source\":{\"snippet\":\"1 januari 2024\"}}";
        String number = "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}";
        String text = "{\"node\":\"TextComposition\",\"parts\":[{\"node\":\"LiteralChunk\",\"text\":\"x\"},"
                + "{\"node\":\"EmbeddedExpression\",\"expr\":" + literal + "}],\"type\":{\"node\":\"Text\"},"
                + "\"source\":{\"snippet\":\"x\"}}";
        return switch (kind) {
            case "TextComposition" -> new Site(ir, W, W, text);
            case "LiteralChunk" -> new Site(ir, W + "/parts/0", W, text);
            case "EmbeddedExpression" -> new Site(ir, W + "/parts/1", W, text);
            case "VariableReference" -> new Site(ir, W, W,
                    "{\"node\":\"VariableReference\",\"name\":\"x\",\"source\":{\"snippet\":\"x\"}}");
            case "Unresolved (expression)" -> new Site(ir, W, W, "{\"node\":\"Unresolved\",\"source\":{\"snippet\":\"x\"}}");
            case "Unresolved (condition)" -> new Site(ir, V, V, "{\"node\":\"Unresolved\",\"source\":{\"snippet\":\"x\"}}");
            case "DimensionSelection" -> new Site(ir, W + "/dimensionSelection", W, "{\"node\":\"Aggregation\","
                    + "\"kind\":\"sum\",\"arguments\":[],\"dimensionSelection\":{\"node\":\"DimensionSelection\","
                    + "\"dimension\":\"d\",\"labels\":[],\"source\":{\"snippet\":\"x\"}},\"type\":null,"
                    + "\"source\":{\"snippet\":\"x\"}}");
            case "TemporalRead" -> new Site(ir, W, W, "{\"node\":\"TemporalRead\",\"base\":" + literal + ",\"date\":"
                    + date + ",\"type\":" + number + ",\"source\":{\"snippet\":\"x op 1 januari 2024\"}}");
            case "ContextValue" -> new Site(ir, W, W, "{\"node\":\"ContextValue\",\"name\":\"evaluationDate\",\"type\":"
                    + "{\"node\":\"Date\",\"precision\":\"day\"},\"source\":{\"snippet\":\"de rekendatum\"}}");
            default -> throw new AssertionError("no position for " + kind);
        };
    }

    // ================= helpers =================

    private static ObjectNode node(JsonNode doc, String path) {
        JsonNode n = doc.at(path);
        if (!(n instanceof ObjectNode o)) throw new AssertionError("no object at " + path);
        return o;
    }

    private static void set(ObjectNode doc, String path, String json) {
        int slash = path.lastIndexOf('/');
        try {
            node(doc, path.substring(0, slash)).set(path.substring(slash + 1), JSON.readTree(json));
        } catch (Exception e) {
            throw new AssertionError(json, e);
        }
    }

    private static IrLoader.Documentfout refuses(JsonNode doc, String what) {
        try {
            IrLoader.loadAll(doc);
        } catch (IrLoader.Documentfout f) {
            return f;
        } catch (IrLoader.Refusal r) {
            fail(what + ": verwacht een documentfout, kreeg een weigering van de vorm: " + r.getMessage());
        }
        return fail(what + ": het document laadde");
    }

    private static ObjectNode golden(String name) throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(CORPUS.resolve(name).resolve("ir.json")));
        IrLoader.loadAll(doc);   // the untwisted golden loads, so a refusal is the twist's alone
        return doc;
    }

    private static List<Path> goldens() throws Exception {
        try (Stream<Path> s = Files.walk(CORPUS)) {
            List<Path> out = s.filter(p -> p.getFileName().toString().equals("ir.json")).sorted().toList();
            if (out.isEmpty()) fail("geen enkele golden gevonden onder " + CORPUS.toAbsolutePath());
            return out;
        }
    }
}
