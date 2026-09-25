package io.github.yhavinga.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.github.yhavinga.regelspraak.ir.Conditie;
import io.github.yhavinga.regelspraak.ir.Default;
import io.github.yhavinga.regelspraak.ir.Expr;
import io.github.yhavinga.regelspraak.ir.PrioritizedDefaults;
import io.github.yhavinga.regelspraak.ir.ResolvedDagsoort;
import io.github.yhavinga.regelspraak.ir.ResolvedObjectCreatie;
import io.github.yhavinga.regelspraak.ir.ResolvedRegelGroep;
import io.github.yhavinga.regelspraak.ir.ResolvedStartpunt;
import io.github.yhavinga.regelspraak.ir.Spelling;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.RolBindingRef;

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
class HernoemingWeigeringWitnessTest {

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
    private static final Map<String, String> KNOPEN = new LinkedHashMap<>();
    static {
        String[][] rijen = { { "Tekst", "Text" }, { "Booleaan", "Boolean" }, { "Enumeratie", "Enumeration" },
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
        for (String[] r : rijen) KNOPEN.put(r[0], r[1]);
    }

    /** The sweep's name for the exceptions record of a default, which carries no node of its own. */
    private static final String UITZONDERINGEN = "Default exceptions record";

    /** Per node (and per record without a node: the exceptions of a default), the v0 keys the renaming replaced. */
    private static final Map<String, List<String>> V0_SLEUTELS = new LinkedHashMap<>();
    static {
        V0_SLEUTELS.put("Enumeration", List.of("domein", "waarden"));
        V0_SLEUTELS.put("FactType", List.of("naam", "wederkerig", "rollen"));
        V0_SLEUTELS.put("Role", List.of("naam", "objecttypeNaam", "meervoud", "meervoudig"));
        V0_SLEUTELS.put("Dimension", List.of("naam", "naamMeervoud", "voorzetsel", "waarden"));
        V0_SLEUTELS.put("Timeline", List.of("naam", "variabelStartpunt", "granulariteit", "aantal"));
        V0_SLEUTELS.put("RoleBinding", List.of("feittype", "rol"));
        V0_SLEUTELS.put("Validity", List.of("vanaf", "tot"));
        V0_SLEUTELS.put("MemberInit", List.of("attribuut", "waarde", "kenmerk"));
        V0_SLEUTELS.put("DayKindDefinition", List.of("naam", "voorwaarde", "geldigheid"));
        V0_SLEUTELS.put("StartPointDetermination", List.of("tijdlijn", "objecttype", "lid"));
        V0_SLEUTELS.put("RuleGroup", List.of("naam", "recursief", "leden"));
        V0_SLEUTELS.put("PrioritizedDefaults", List.of("naam", "onderwerp", "kenmerk", "attribuut", "ongeordend",
                "geldigheid"));
        V0_SLEUTELS.put("Default", List.of("besluit", "voorwaarde", "consequentie", "initialisatie", "excepties"));
        V0_SLEUTELS.put(UITZONDERINGEN, List.of("ongeordend", "leden"));
        V0_SLEUTELS.put("Binary", List.of("links", "rechts"));
        V0_SLEUTELS.put("Rounding", List.of("basis", "modus", "decimalen"));
        V0_SLEUTELS.put("Clamp", List.of("basis"));
        V0_SLEUTELS.put("TextComposition", List.of("delen"));
        V0_SLEUTELS.put("LiteralChunk", List.of("tekst"));
        V0_SLEUTELS.put("AttributeReference", List.of("attribuut", "objecttype", "navigatieBindingen"));
        V0_SLEUTELS.put("DimensionCellRead", List.of("attribuut"));
        V0_SLEUTELS.put("VariableReference", List.of("naam"));
        V0_SLEUTELS.put("TemporalRead", List.of("basis", "datum"));
        V0_SLEUTELS.put("DimensionSelection", List.of("dimensie"));
        V0_SLEUTELS.put("Comparison", List.of("links", "rechts", "periode"));
        V0_SLEUTELS.put("Predicate", List.of("soort", "dagsoort", "negatie", "cijfers", "periode"));
        V0_SLEUTELS.put("CharacteristicCheck", List.of("kenmerk", "negatie", "eigenaar", "periode", "navigatieBinding"));
        V0_SLEUTELS.put("TimeWindow", List.of("vanaf", "tot", "totInclusief"));
        V0_SLEUTELS.put("Compound", List.of("kwantor", "drempel", "onderdelen"));
        V0_SLEUTELS.put("QuantifierFold", List.of("kwantor", "drempel", "elementObjecttype", "toets", "bronRol", "bronPad"));
        V0_SLEUTELS.put("WholePeriod", List.of("jaar", "niet"));
    }

    /** The fields the renaming made optional: a v0 null there is absent on the L5 wire, and a null is refused. */
    private static final List<String> D5A = List.of("Validity.from", "Validity.to", "Role.plural",
            "Comparison.wholePeriod", "CharacteristicCheck.wholePeriod", "CharacteristicCheck.host", "Default.decision",
            "QuantifierFold.filter", "PrioritizedDefaults.characteristic", "Predicate.wholePeriod", "Predicate.dayKind",
            "Default.justification", "TimeWindow.to", "Clamp.maximum", "Clamp.minimum", "Dimension.plural",
            "Dimension.preposition", "TimeWindow.from");

    private static final String NULL_ZIN = "an optional field is written whole or not at all, and null is neither";

    // ================= the families of what the v0 wire wrote =================

    /** Every v0 node name at a position of its L5 node: at a dispatch the shared loader's "unknown … node", at a
     *  fixed position its "expected …, got …" — both naming the v0 name. */
    @Test void elke_v0_knoopnaam_wordt_bij_naam_geweigerd() throws Exception {
        Set<String> expr = Set.of("Binair", "Afgerond", "Begrensd", "TekstReeks", "AttribuutRef", "DimensieCel",
                "ParameterRef", "VariabeleRef");
        Set<String> conditie = Set.of("Vergelijking", "Predicaat", "Kenmerk", "Periode", "Samengesteld", "Kwantorvouw");
        Set<String> datatype = Set.of("Tekst", "Booleaan", "Enumeratie");
        Set<String> deel = Set.of("Letterlijk", "Ingevoegd");
        int rijen = 0;
        for (Map.Entry<String, String> k : KNOPEN.entrySet()) {
            String v0 = k.getKey(), l5 = k.getValue();
            List<Plek> plekken = v0.equals("Onopgelost")
                    ? List.of(synthetisch("Unresolved (expression)"), synthetisch("Unresolved (condition)"))
                    : List.of(plek(l5));
            for (Plek p : plekken) {
                ObjectNode doc = p.document();
                knoop(doc, p.pad()).put("node", v0);
                String zin = expr.contains(v0) || v0.equals("Onopgelost") && p.pad().equals(W)
                        ? "unknown expr node \"" + v0 + "\""
                        : conditie.contains(v0) || v0.equals("Onopgelost") ? "unknown condition node \"" + v0 + "\""
                        : datatype.contains(v0) ? "unknown datatype node \"" + v0 + "\""
                        : deel.contains(v0) ? "unknown TextCompositionPart node \"" + v0 + "\""
                        : "expected \"" + l5 + "\", got \"" + v0 + "\"";
                IrLoader.Documentfout e = weigert(doc, v0);
                assertEquals(p.pad() + "/node", e.plek(), v0 + ": " + e.getMessage());
                assertTrue(e.getMessage().endsWith(": " + zin), v0 + ": " + e.getMessage());
                rijen++;
            }
        }
        assertEquals(33, rijen, "32 v0 node names, the marker at both of its positions");
    }

    /** Every v0 key on the node that replaced it is an unknown field, named at its pointer — a reader never takes a
     *  v0 name beside the L5 one for a second spelling of the field. */
    @Test void elke_v0_sleutel_op_zijn_l5_knoop_is_een_onbekend_veld() throws Exception {
        int rijen = 0;
        for (Map.Entry<String, List<String>> k : V0_SLEUTELS.entrySet()) {
            String soort = k.getKey();
            for (String sleutel : k.getValue()) {
                Plek p = soort.equals("TemporalRead") ? synthetisch("TemporalRead") : plek(soort);
                ObjectNode doc = p.document();
                knoop(doc, p.pad()).put(sleutel, 1);
                IrLoader.Documentfout e = weigert(doc, soort + "." + sleutel);
                assertEquals(p.pad() + "/" + sleutel, e.plek(), soort + "." + sleutel + ": " + e.getMessage());
                String zin = soort.equals(UITZONDERINGEN) ? ": unknown field" : ": unknown field on " + soort;
                assertTrue(e.getMessage().endsWith(zin), soort + "." + sleutel + ": " + e.getMessage());
                rijen++;
            }
        }
        // the sum of the table above: 31 records, 89 renamed keys (the temporal read's two included)
        assertEquals(89, rijen, "the renamed keys of every node the loader reads");
        // the temporal read is a form this backend does not carry: written in its L5 shape it is refused as that
        ObjectNode doc = synthetisch("TemporalRead").document();
        IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class, () -> IrLoader.loadAll(doc));
        assertEquals("Expr:TemporalRead", r.form(), r.getMessage());
    }

    @Test void de_v0_lijsten_onder_rules_zijn_onbekende_velden() throws Exception {
        String[][] lijsten = { { "rules", "resolvedRegels" }, { "dayKindDefinitions", "resolvedDagsoorten" },
                { "objectCreations", "objectCreaties" }, { "allocations", "verdelingen" },
                { "consistencies", "consistenties" }, { "uniquenesses", "uniciteiten" },
                { "factCreations", "feitCreaties" }, { "startPointDeterminations", "resolvedStartpunten" },
                { "ruleGroups", "regelGroepen" } };
        for (String[] l : lijsten) {
            ObjectNode doc = golden(PENNINGDREMPEL);
            ObjectNode rules = (ObjectNode) doc.get("rules");
            ObjectNode hernoemd = JSON.createObjectNode();
            rules.properties().forEach(e -> hernoemd.set(e.getKey().equals(l[0]) ? l[1] : e.getKey(), e.getValue()));
            doc.set("rules", hernoemd);
            IrLoader.Documentfout e = weigert(doc, l[1]);
            assertEquals("/rules/" + l[1], e.plek(), e.getMessage());
            assertTrue(e.getMessage().endsWith(": unknown field"), e.getMessage());
        }
    }

    /** Every v0 value of every family, at every place of the family a loadable golden (or a row) carries: the shared
     *  loader's "unknown <field> value", the field being the last segment of the place. The v0 spelling of a value is
     *  its record constant's name, which the v0 emitter wrote; the one exception is the whole-period granularity, a
     *  boolean on the v0 wire, and the context-value name, a free string WF-098 closes. The date precision spelled
     *  the same before L5 and has no v0 value to refuse. */
    @Test void elke_v0_enumwaarde_wordt_bij_naam_geweigerd() throws Exception {
        int rijen = 0;
        Set<String> gedekt = new java.util.TreeSet<>();
        for (Spelling.Familie<?> f : Spelling.FAMILIES) {
            if (f == Spelling.DATE_PRECISION) continue;
            List<Object> v0 = new ArrayList<>();
            for (Object k : f.rijen().keySet()) v0.add(k);
            for (Object k : f.geweigerd().keySet()) v0.add(k);
            for (String plaats : f.plaatsen()) {
                Plek p = plaats.equals("ContextValue.name") ? synthetisch("ContextValue")
                        : plaats.equals("QuantifierFold.quantifier") ? plek("QuantifierFold")
                        : plekMetVeld(plaats);
                String veld = plaats.substring(plaats.indexOf('.') + 1);
                for (Object k : v0) {
                    ObjectNode doc = p.document();
                    JsonNode waarde = k instanceof Boolean b ? BooleanNode.valueOf(b)
                            : TextNode.valueOf(k instanceof Enum<?> e ? e.name() : k.toString());
                    knoop(doc, p.pad()).set(veld, waarde);
                    IrLoader.Documentfout e = weigert(doc, plaats + " " + waarde);
                    assertEquals(p.pad() + "/" + veld, e.plek(), plaats + " " + waarde + ": " + e.getMessage());
                    String zin = plaats.equals("ContextValue.name")
                            ? "context value '" + k + "' is not one of the two admissible names evaluationDate,"
                                    + " evaluationYear: the context-value name is a closed set, spelled byte for byte"
                            : plaats.equals("WholePeriod.granularity") ? "expected a string, got " + waarde
                            : "unknown " + veld + " value " + waarde;
                    assertTrue(e.getMessage().endsWith(": " + zin), plaats + " " + waarde + ": " + e.getMessage());
                    rijen++;
                }
                gedekt.add(plaats);
            }
        }
        assertEquals(18, gedekt.size(), "every place of the fifteen families the renaming respelled: " + gedekt);
        // sign 4, kind 3, granularity 5 x 3 places, presentation 2, method 3, comparison 10, binary 8, rounding 5,
        // aggregation 8, builtin 17, predicate 5, quantifier 6 x 2 places, context value 2, whole period 2, severity 3
        assertEquals(99, rijen, "every v0 value at every place of its family");
    }

    /** A null on a field the renaming made optional: absent is what the v0 null was, so a null is neither. */
    @Test void een_null_op_een_veld_dat_l5_optioneel_maakte_weigert() throws Exception {
        for (String plaats : D5A) {
            String soort = plaats.substring(0, plaats.indexOf('.')), veld = plaats.substring(plaats.indexOf('.') + 1);
            Plek p = plek(soort);
            ObjectNode doc = p.document();
            knoop(doc, p.pad()).putNull(veld);
            IrLoader.Documentfout e = weigert(doc, plaats);
            assertEquals(p.pad() + "/" + veld, e.plek(), plaats + ": " + e.getMessage());
            assertTrue(e.getMessage().endsWith(": " + NULL_ZIN), plaats + ": " + e.getMessage());
        }
    }

    // ================= the values a verifier rule names =================

    /** WF-108 (a) and WF-113 (a): the schema admits none of the three, the verifier loads them so that its rule names
     *  them, and every executor refuses them at load in the rule's sentence — this one too. {@code the} is an ordinary
     *  value of a Compound's quantifier, the bare definite of the dialect. */
    @Test void de_waarden_die_een_verifierregel_noemt_weigeren_bij_het_laden() throws Exception {
        Plek vouw = plek("QuantifierFold");
        ObjectNode doc = vouw.document();
        knoop(doc, vouw.pad()).put("quantifier", "the");
        IrLoader.Documentfout e = weigert(doc, "the");
        assertEquals(vouw.pad() + "/quantifier", e.plek(), e.getMessage());
        assertTrue(e.getMessage().endsWith(": quantifier fold with quantifier the: the bare definite is a description of"
                + " ONE member, not a quantification over the set — a fold over it has no reading"), e.getMessage());

        Plek diagnose = plek("Diagnostic");
        String[][] ernst = {
                { "error", "severity error is not a wire severity: an error means the producer refused to write the"
                        + " document, so a document that was written carries none" },
                { "info", "severity info is not a wire severity: the list is the account of what stayed unresolved,"
                        + " and info is tooling talk" },
                { null, "diagnostic without a severity: the one wire severity is warning" } };
        for (String[] r : ernst) {
            doc = diagnose.document();
            if (r[0] == null) knoop(doc, diagnose.pad()).putNull("severity");
            else knoop(doc, diagnose.pad()).put("severity", r[0]);
            e = weigert(doc, String.valueOf(r[0]));
            assertEquals(diagnose.pad() + "/severity", e.plek(), e.getMessage());
            assertTrue(e.getMessage().endsWith(": " + r[1]), e.getMessage());
        }

        Plek samen = plek("Compound");
        doc = samen.document();
        knoop(doc, samen.pad()).put("quantifier", "the");
        IrLoader.loadAll(doc);
    }

    /** The one node that keeps its v0 shape until the parity golden of its v1 form lands: its fields load and are
     *  read, its source is the record of every node, and a key of the v1 form is no key of it yet. */
    @Test void regelversie_houdt_zijn_v0_vorm() throws Exception {
        ObjectNode doc = golden(PENNINGDREMPEL);
        zet(doc, V, "{\"node\":\"Regelversie\",\"regel\":\"penningdrempel\",\"inconsistent\":true,"
                + "\"source\":{\"snippet\":\"regelversie penningdrempel is inconsistent\"}}");
        Conditie.Regelversie rv = assertInstanceOf(Conditie.Regelversie.class,
                IrLoader.load(doc).resolvedRegels.get(0).voorwaarde());
        assertEquals("penningdrempel", rv.regel());
        assertTrue(rv.inconsistent());
        assertEquals("regelversie penningdrempel is inconsistent", rv.bron().snippet());
        knoop(doc, V).put("rule", "penningdrempel");
        IrLoader.Documentfout e = weigert(doc, "Regelversie.rule");
        assertEquals(V + "/rule", e.plek(), e.getMessage());
        assertTrue(e.getMessage().endsWith(": unknown field on Regelversie"), e.getMessage());
    }

    /** The whole-period qualifier's granularity is a family of two: year and month read onto the record's flag, and
     *  any other granularity is no value of it, a week included. */
    @Test void de_heelperiodegranulariteit_is_jaar_of_maand() throws Exception {
        Plek p = plek("WholePeriod");
        for (String[] r : new String[][] { { "year", "true" }, { "month", "false" } }) {
            ObjectNode doc = p.document();
            knoop(doc, p.pad()).put("granularity", r[0]);
            IrLoader.loadAll(doc);
        }
        ObjectNode doc = p.document();
        knoop(doc, p.pad()).put("granularity", "week");
        IrLoader.Documentfout e = weigert(doc, "week");
        assertTrue(e.getMessage().endsWith(": unknown granularity value \"week\""), e.getMessage());
    }

    // ================= the other side: the L5 fields land in the records =================

    /** The fields of a characteristic check: its host (absent: the subject), its whole-period qualifier with its
     *  granularity and its negation, its navigation and its negation. */
    @Test void een_kenmerkcheck_leest_elk_l5_veld() throws Exception {
        ObjectNode doc = golden(PENNINGDREMPEL);
        zet(doc, V, "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"penninggerechtigd\",\"negated\":true,"
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
        assertEquals(List.of(new RolBindingRef("f", "r")), l.navigatieBindingen().get(k));
        // absent host and qualifier: the subject, no qualifier — the v0 null of the record
        ObjectNode kaal = golden(PENNINGDREMPEL);
        zet(kaal, V, "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"penninggerechtigd\",\"negated\":false,"
                + "\"source\":{\"snippet\":\"hij penninggerechtigd is\"}}");
        Conditie.Kenmerk k2 = (Conditie.Kenmerk) IrLoader.load(kaal).resolvedRegels.get(0).voorwaarde();
        assertNull(k2.eigenaar());
        assertNull(k2.periode());
    }

    /** The expression nodes of the renaming, each with every field written: the values land in the record's
     *  components, the enumeration values through the table. */
    @Test void de_expressieknopen_lezen_elk_l5_veld() throws Exception {
        String getal = "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}";
        String een = "{\"node\":\"Literal\",\"value\":{\"number\":\"1\"},\"type\":" + getal
                + ",\"source\":{\"snippet\":\"1\"}}";
        String twee = "{\"node\":\"Literal\",\"value\":{\"number\":\"2\"},\"type\":" + getal
                + ",\"source\":{\"snippet\":\"2\"}}";
        ObjectNode doc = golden(PREMIEVENSTER);
        zet(doc, W, "{\"node\":\"Rounding\",\"base\":{\"node\":\"Clamp\",\"base\":{\"node\":\"Binary\",\"op\":\"times\","
                + "\"left\":" + een + ",\"right\":" + twee + ",\"type\":" + getal + ",\"source\":{\"snippet\":\"1 maal 2\"}},"
                + "\"minimum\":" + een + ",\"type\":" + getal + ",\"source\":{\"snippet\":\"minimaal 1\"}},"
                + "\"mode\":\"towardZero\",\"decimals\":1,\"type\":" + getal + ",\"source\":{\"snippet\":\"afgerond\"}}");
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
        zet(doc, W, "{\"node\":\"TextComposition\",\"parts\":[{\"node\":\"LiteralChunk\",\"text\":\"vondst \"},"
                + "{\"node\":\"EmbeddedExpression\",\"expr\":" + een + "}],\"type\":{\"node\":\"Text\"},"
                + "\"source\":{\"snippet\":\"vondst 1\"}}");
        Expr.TekstReeks t = assertInstanceOf(Expr.TekstReeks.class, IrLoader.load(doc).resolvedRegels.get(0).waarde());
        assertEquals("vondst ", ((Expr.TekstReeks.Letterlijk) t.delen().get(0)).tekst());
        assertInstanceOf(Expr.Literal.class, ((Expr.TekstReeks.Ingevoegd) t.delen().get(1)).expr());

        // the name is read, and names the node the loader refuses (WF-034)
        doc = golden(PREMIEVENSTER);
        zet(doc, W, "{\"node\":\"VariableReference\",\"name\":\"x\",\"source\":{\"snippet\":\"x\"}}");
        ObjectNode varDoc = doc;
        IrLoader.Documentfout vr = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.load(varDoc));
        assertEquals(W, vr.plek(), vr.getMessage());
        assertTrue(vr.getMessage().endsWith(": " + IrLoader.variabeleVerwijzingWeigering("x")), vr.getMessage());
    }

    /** The condition nodes of the renaming, each with every field written. */
    @Test void de_conditieknopen_lezen_elk_l5_veld() throws Exception {
        String getal = "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}";
        String een = "{\"node\":\"Literal\",\"value\":{\"number\":\"1\"},\"type\":" + getal
                + ",\"source\":{\"snippet\":\"1\"}}";
        String jaar = "{\"node\":\"WholePeriod\",\"granularity\":\"year\",\"negated\":false}";
        ObjectNode doc = golden(PENNINGDREMPEL);
        zet(doc, V, "{\"node\":\"Compound\",\"quantifier\":\"atLeast\",\"threshold\":1,\"parts\":["
                + "{\"node\":\"Comparison\",\"op\":\"lessOrEqual\",\"left\":" + een + ",\"right\":" + een + ",\"wholePeriod\":"
                + jaar + ",\"source\":{\"snippet\":\"a\"}},"
                + "{\"node\":\"Predicate\",\"kind\":\"dayKind\",\"operand\":" + een + ",\"dayKind\":\"feestdag\","
                + "\"negated\":true,\"digits\":3,\"source\":{\"snippet\":\"b\"}},"
                + "{\"node\":\"TimeWindow\",\"to\":" + een + ",\"toInclusive\":true,\"source\":{\"snippet\":\"c\"}}],"
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
    @Test void de_regellaag_leest_elk_l5_veld() throws Exception {
        int dagsoorten = 0, startpunten = 0, groepen = 0, toekenningen = 0, defaults = 0;
        for (Path pad : goldens()) {
            ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(pad));
            IrLoader.Loaded l;
            try { l = IrLoader.loadAll(doc); } catch (IrLoader.Refusal r) { continue; }
            RegelSpraakModel m = l.model();
            JsonNode rules = doc.get("rules");
            for (int i = 0; i < rules.get("dayKindDefinitions").size(); i++, dagsoorten++) {
                JsonNode w = rules.get("dayKindDefinitions").get(i);
                ResolvedDagsoort d = m.resolvedDagsoorten.get(i);
                assertEquals(w.get("dayKind").asText(null), d.naam(), pad.toString());
                assertEquals(w.get("condition").isNull(), d.voorwaarde() == null, pad.toString());
            }
            for (int i = 0; i < rules.get("startPointDeterminations").size(); i++, startpunten++) {
                JsonNode w = rules.get("startPointDeterminations").get(i);
                ResolvedStartpunt sp = m.resolvedStartpunten.get(i);
                assertEquals(w.get("timeline").asText(null), sp.tijdlijn(), pad.toString());
                assertEquals(w.get("objectType").asText(null), sp.objecttype(), pad.toString());
                assertEquals(w.get("member").asText(null), sp.lid(), pad.toString());
            }
            for (int i = 0; i < rules.get("ruleGroups").size(); i++, groepen++) {
                JsonNode w = rules.get("ruleGroups").get(i);
                ResolvedRegelGroep g = m.regelGroepen.get(i);
                assertEquals(w.get("name").asText(null), g.naam(), pad.toString());
                assertEquals(w.get("recursive").asBoolean(), g.recursief(), pad.toString());
                assertEquals(w.get("members").size(), g.leden().size(), pad.toString());
            }
            int oc = 0;
            for (JsonNode vorm : rules.get("objectCreations"))
                for (JsonNode versie : vorm.get("versions")) {
                    ResolvedObjectCreatie c = m.objectCreaties.get(oc++);
                    for (int j = 0; j < versie.get("initialisations").size(); j++, toekenningen++) {
                        JsonNode w = versie.get("initialisations").get(j);
                        ResolvedObjectCreatie.Toekenning t = c.toekenningen().get(j);
                        assertEquals(w.get("member").asText(null), t.attribuut(), pad.toString());
                        assertEquals(w.get("characteristic").asBoolean(), t.kenmerk(), pad.toString());
                        assertEquals(w.get("value").isNull(), t.waarde() == null, pad.toString());
                    }
                }
            JsonNode pds = rules.get("prioritizedDefaults");
            for (int i = 0; pds != null && i < pds.size(); i++) {
                JsonNode w = pds.get(i);
                PrioritizedDefaults pd = l.prioritizedDefaults().get(i);
                assertEquals(w.get("name").asText(null), pd.naam(), pad.toString());
                assertEquals(w.get("subject").asText(), pd.onderwerp(), pad.toString());
                assertEquals(w.has("characteristic") ? w.get("characteristic").asText() : null, pd.kenmerk(),
                        pad.toString());
                assertEquals(w.has("attribute") ? w.get("attribute").asText() : null, pd.attribuut(), pad.toString());
                assertEquals(w.path("unordered").asBoolean(false), pd.ongeordend(), pad.toString());
                defaults += vergelijk(w.get("defaults"), pd.defaults(), pad.toString());
            }
        }
        // measured 2026-09-25 on L5: 7 day-kind definitions, 1 start-point determination, 4 rule groups, 16 member
        // initialisations, 83 defaults
        assertTrue(dagsoorten >= 7 && startpunten >= 1 && groepen >= 4 && toekenningen >= 16 && defaults >= 83,
                "the corpus carries each rule-layer node: " + dagsoorten + " " + startpunten + " " + groepen + " "
                        + toekenningen + " " + defaults);
    }

    /** A list of defaults against its records, the exception groups included; the number of defaults compared. */
    private static int vergelijk(JsonNode wire, List<Default> records, String waar) {
        assertEquals(wire.size(), records.size(), waar);
        int n = 0;
        for (int i = 0; i < wire.size(); i++, n++) {
            JsonNode w = wire.get(i);
            Default d = records.get(i);
            assertEquals(w.has("decision") ? w.get("decision").asBoolean() : null, d.besluit(), waar);
            assertEquals(w.has("justification"), d.voorwaarde() != null, waar);
            assertEquals(w.has("consequence"), d.consequentie() != null, waar);
            assertEquals(w.path("initialisation").asBoolean(false), d.initialisatie(), waar);
            assertEquals(w.has("exceptions"), d.excepties() != null, waar);
            if (w.has("exceptions")) {
                assertEquals(w.get("exceptions").get("unordered").asBoolean(), d.excepties().ongeordend(), waar);
                JsonNode leden = w.get("exceptions").get("members");
                assertEquals(leden.size(), d.excepties().leden().size(), waar);
                for (int j = 0; j < leden.size(); j++)
                    n += vergelijk(leden.get(j).get("defaults"), d.excepties().leden().get(j).defaults(), waar);
            }
        }
        return n;
    }

    // ================= positions =================

    /** A position in a document: the golden it is read from (or a row built on {@link #PENNINGDREMPEL}) and the
     *  pointer of the node. */
    private record Plek(Path ir, String pad, String zet, String json) {
        ObjectNode document() throws Exception {
            ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(ir));
            if (zet != null) HernoemingWeigeringWitnessTest.zet(doc, zet, json);
            return doc;
        }
    }

    /** The first position of each node kind (a version record under its form's kind, the exceptions of a default as
     *  "Default.exceptions") and of each field of a node ("Kind.field"), over the goldens this backend loads. */
    private static final Map<String, Plek> EERSTE = new TreeMap<>();

    private static synchronized Map<String, Plek> eerste() throws Exception {
        if (!EERSTE.isEmpty()) return EERSTE;
        for (Path ir : goldens()) {
            JsonNode doc = JSON.readTree(Files.readString(ir));
            try { IrLoader.loadAll(doc); } catch (IrLoader.Refusal r) { continue; }
            verzamel(doc, "", ir);
        }
        return EERSTE;
    }

    private static void verzamel(JsonNode n, String pad, Path ir) {
        if (n instanceof ObjectNode o) {
            JsonNode soort = o.get("node");
            // a rule target is always an attribute reference, refused there as the target it is, not by kind
            if (soort != null && soort.isTextual() && !pad.endsWith("/target")) {
                EERSTE.putIfAbsent(soort.asText(), new Plek(ir, pad, null, null));
                o.fieldNames().forEachRemaining(v -> EERSTE.putIfAbsent(soort.asText() + "." + v,
                        new Plek(ir, pad, null, null)));
                if (o.has("exceptions")) EERSTE.putIfAbsent(UITZONDERINGEN, new Plek(ir, pad + "/exceptions", null, null));
            }
            if (soort != null && o.path("versions").isArray())
                for (int i = 0; i < o.get("versions").size(); i++) {
                    String vs = soort.asText() + "Version";
                    Plek p = new Plek(ir, pad + "/versions/" + i, null, null);
                    EERSTE.putIfAbsent(vs, p);
                    o.get("versions").get(i).fieldNames().forEachRemaining(v -> EERSTE.putIfAbsent(vs + "." + v, p));
                }
            o.properties().forEach(e -> verzamel(e.getValue(),
                    pad + "/" + e.getKey().replace("~", "~0").replace("/", "~1"), ir));
        } else if (n instanceof ArrayNode a) {
            for (int i = 0; i < a.size(); i++) verzamel(a.get(i), pad + "/" + i, ir);
        }
    }

    /** The first position of a node kind: a loadable golden's, or the row of a kind no loadable golden carries. */
    private static Plek plek(String soort) throws Exception {
        Plek p = eerste().get(soort);
        if (p != null) return p;
        return synthetisch(soort);
    }

    /** The first position of a node that carries {@code plaats}'s field ("Kind.field"). */
    private static Plek plekMetVeld(String plaats) throws Exception {
        Plek p = eerste().get(plaats);
        if (p == null) fail("geen laadbare golden draagt " + plaats);
        return p;
    }

    /** The rows at rule 0 of {@link #PENNINGDREMPEL} for the kinds no loadable golden carries. */
    private static Plek synthetisch(String soort) throws Exception {
        Path ir = CORPUS.resolve(PENNINGDREMPEL).resolve("ir.json");
        String literal = "{\"node\":\"Literal\",\"value\":{\"text\":\"x\"},\"type\":{\"node\":\"Text\"},"
                + "\"source\":{\"snippet\":\"x\"}}";
        String datum = "{\"node\":\"Literal\",\"value\":{\"date\":\"2024-01-01\"},\"type\":{\"node\":\"Date\","
                + "\"precision\":\"day\"},\"source\":{\"snippet\":\"1 januari 2024\"}}";
        String getal = "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}";
        String tekst = "{\"node\":\"TextComposition\",\"parts\":[{\"node\":\"LiteralChunk\",\"text\":\"x\"},"
                + "{\"node\":\"EmbeddedExpression\",\"expr\":" + literal + "}],\"type\":{\"node\":\"Text\"},"
                + "\"source\":{\"snippet\":\"x\"}}";
        return switch (soort) {
            case "TextComposition" -> new Plek(ir, W, W, tekst);
            case "LiteralChunk" -> new Plek(ir, W + "/parts/0", W, tekst);
            case "EmbeddedExpression" -> new Plek(ir, W + "/parts/1", W, tekst);
            case "VariableReference" -> new Plek(ir, W, W,
                    "{\"node\":\"VariableReference\",\"name\":\"x\",\"source\":{\"snippet\":\"x\"}}");
            case "Unresolved (expression)" -> new Plek(ir, W, W, "{\"node\":\"Unresolved\",\"source\":{\"snippet\":\"x\"}}");
            case "Unresolved (condition)" -> new Plek(ir, V, V, "{\"node\":\"Unresolved\",\"source\":{\"snippet\":\"x\"}}");
            case "DimensionSelection" -> new Plek(ir, W + "/dimensionSelection", W, "{\"node\":\"Aggregation\","
                    + "\"kind\":\"sum\",\"arguments\":[],\"dimensionSelection\":{\"node\":\"DimensionSelection\","
                    + "\"dimension\":\"d\",\"labels\":[],\"source\":{\"snippet\":\"x\"}},\"type\":null,"
                    + "\"source\":{\"snippet\":\"x\"}}");
            case "TemporalRead" -> new Plek(ir, W, W, "{\"node\":\"TemporalRead\",\"base\":" + literal + ",\"date\":"
                    + datum + ",\"type\":" + getal + ",\"source\":{\"snippet\":\"x op 1 januari 2024\"}}");
            case "ContextValue" -> new Plek(ir, W, W, "{\"node\":\"ContextValue\",\"name\":\"evaluationDate\",\"type\":"
                    + "{\"node\":\"Date\",\"precision\":\"day\"},\"source\":{\"snippet\":\"de rekendatum\"}}");
            default -> throw new AssertionError("no position for " + soort);
        };
    }

    // ================= helpers =================

    private static ObjectNode knoop(JsonNode doc, String pad) {
        JsonNode n = doc.at(pad);
        if (!(n instanceof ObjectNode o)) throw new AssertionError("no object at " + pad);
        return o;
    }

    private static void zet(ObjectNode doc, String pad, String json) {
        int slash = pad.lastIndexOf('/');
        try {
            knoop(doc, pad.substring(0, slash)).set(pad.substring(slash + 1), JSON.readTree(json));
        } catch (Exception e) {
            throw new AssertionError(json, e);
        }
    }

    private static IrLoader.Documentfout weigert(JsonNode doc, String wat) {
        try {
            IrLoader.loadAll(doc);
        } catch (IrLoader.Documentfout f) {
            return f;
        } catch (IrLoader.Refusal r) {
            fail(wat + ": verwacht een documentfout, kreeg een weigering van de vorm: " + r.getMessage());
        }
        return fail(wat + ": het document laadde");
    }

    private static ObjectNode golden(String naam) throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(CORPUS.resolve(naam).resolve("ir.json")));
        IrLoader.loadAll(doc);   // the untwisted golden loads, so a refusal is the twist's alone
        return doc;
    }

    private static List<Path> goldens() throws Exception {
        try (Stream<Path> s = Files.walk(CORPUS)) {
            List<Path> uit = s.filter(p -> p.getFileName().toString().equals("ir.json")).sorted().toList();
            if (uit.isEmpty()) fail("geen enkele golden gevonden onder " + CORPUS.toAbsolutePath());
            return uit;
        }
    }
}
