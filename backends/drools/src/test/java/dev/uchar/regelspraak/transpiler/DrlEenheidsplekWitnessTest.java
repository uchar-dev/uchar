package dev.uchar.regelspraak.transpiler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.loader.IrLoader;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The DRL-TEXT branch's unit places, held to RUNTIME-SEMANTICS ch. 2.10 one level BELOW the replay: on the
 * emitted TEXT. It grafts the harness's own lie onto every DERIVED position of every corpus golden and
 * demands the emitted DRL be BYTE-IDENTICAL — a rescale factor that moved is a place reading the node's own
 * wire annotation instead of the declaration, and the failure names the line that moved.
 *
 * <p><b>What this adds over {@code scripts/check-reftype-derivation.sh}'s DRL leg</b>, which asks the same
 * question of the same twists: this runs in the backend's own JUnit suite, so gate 12 — unconditional and
 * blocking — catches a DRL unit regression too, and catches it as a named line rather than as a moved
 * number. Its REACH is also wider by construction: it needs no case file and no playable golden, only a
 * document the loader accepts, so a golden that gains a unit-bearing rescale is covered here the day it
 * lands, whether or not it is replayable.
 *
 * <p><b>What its reach is, measured and said out loud.</b> With {@code EffectieveEenheid.van} put back on
 * {@code e.type()} (the state before that class existed) this witness goes red on <b>21</b> goldens
 * (measured 2026-09-18). It used to be 9, the same nine the harness's DRL leg caught, and the difference is
 * this branch's ch. 4.10 lowering: while a Let was a MARKER here, a golden whose unit-bearing positions sat
 * under one emitted no rescale factor at all, so no lie could move its text — {@code jutterswet-lichterloon}
 * and {@code jutterswet-vondstrecht} were two of those and are now among the 21. Wider reach, and no longer
 * the same catch.
 *
 * <p>The twist mirrors the harness's table deliberately — same derived positions, same reason-carrying
 * exceptions (a {@code Literal}'s own lexeme, the WF-035 duration operand, a WF-038 tijdsduur). It is
 * duplicated in Java rather than shared because the harness twists FILES for three executors and this
 * twists a parsed tree for one; if the harness's table gains a class, this list must gain it too. Whether
 * an operand of a date plus a duration is a DATE is read as the harness reads it ({@code gedeclareerd}):
 * a reference carries no type since L2, so it answers with the declaration it names, and a Let's binder
 * with the declaration of its binding. Reading only the node's own annotation, this witness took an own
 * date attribute for no date, twisted the duration beside it, and the loader refused the lying document
 * (measured 2026-09-30 on {@code jutterswet-vroegste-vondst}: "date ± duration: the duration unit
 * 'dag-verdraaid7' is neither a recognised time unit nor a declared day kind").
 */
class DrlEenheidsplekWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** The position classes the harness twists, with its per-node-kind restatement exponent. */
    private static final Map<String, Integer> KLASSE = Map.of(
            "AttributeReference", 1, "ParameterReference", 2, "DimensionCellRead", 3, "BinderRef", 4,
            "Binary", 5, "Let", 6, "Aggregation", 7, "BuiltinCall", 7, "Rounding", 8, "Clamp", 9);
    /** ch. 2.10 clause 2 splits Binary and the two function nodes by OPERATOR: only these branches derive a result
     *  unit. Every kind that derives is an aggregation's, so no BuiltinCall is ever twisted. */
    private static final Set<String> AFGELEIDE_OP = Set.of("plus", "minus", "reducedBy", "times");
    private static final Set<String> AFGELEIDE_KIND = Set.of("sum", "total", "minimum", "maximum", "first", "last");
    /** WF-038: a tijdsduur's unit is the operator's parameter, not a derived result type. */
    private static final Set<String> GEEN_FUNCTIE = Set.of("duration", "absoluteDuration");

    @Test void de_drl_tekst_volgt_de_declaraties_niet_de_wire_herhaling() throws Exception {
        int twists = 0;
        Set<String> geraakt = new LinkedHashSet<>();
        List<String> afwijkend = new ArrayList<>();
        for (Path ir : goldens()) {
            JsonNode eerlijk = JSON.readTree(Files.readString(ir));
            JsonNode liegend = eerlijk.deepCopy();
            int n = verdraai(liegend, false, new Declaraties(eerlijk), Map.of());
            if (n == 0) continue;                    // no derived position carries a convertible unit here
            String a, b;
            try {
                a = drl(eerlijk);
                b = drl(liegend);
            } catch (IrLoader.Refusal weigering) {
                continue;                            // the loader does not carry this form at all
            }
            twists += n;
            geraakt.add(naam(ir));
            if (!a.equals(b)) afwijkend.add(naam(ir) + " — " + eersteVerschil(a, b));
        }
        // never silently green: a corpus without a single twistable unit annotation would make every
        // assertion below vacuous, so the empty probe is itself the failure.
        assertTrue(twists > 0, "no derived position was twisted — the witness probed nothing (" + geraakt + ")");
        assertEquals(List.of(), afwijkend,
                "the DRL text moved when a wire annotation lied: that place reads the repetition, not the declaration");
    }

    // ---------------- the twist (mirrors scripts/check-reftype-derivation.sh) ----------------

    /** Graft the sister unit onto every derived position under {@code x}; return the number of grafts. {@code scope}
     *  maps each binder in scope (lower-cased) to the declaration of its binding. */
    private static int verdraai(JsonNode x, boolean vrijgesteldDoorOuder, Declaraties d, Map<String, JsonNode> scope) {
        if (x.isArray()) {
            int n = 0;
            for (JsonNode kind : x) n += verdraai(kind, false, d, scope);
            return n;
        }
        if (!(x instanceof ObjectNode o)) return 0;
        String node = o.path("node").asText(null);
        // WF-035: the DURATION operand of a date +/- duration carries the calendar unit ref routes on
        JsonNode duur = null;
        if ("Binary".equals(node) && Set.of("plus", "minus", "reducedBy").contains(o.path("op").asText(""))) {
            JsonNode l = o.get("left"), r = o.get("right");
            if (datumGetypeerd(l, d, scope) && !datumGetypeerd(r, d, scope)) duur = r;
            else if (datumGetypeerd(r, d, scope) && !datumGetypeerd(l, d, scope)) duur = l;
        }
        boolean vrijgesteld = vrijgesteldDoorOuder || "Literal".equals(node)
                || ("BuiltinCall".equals(node) && GEEN_FUNCTIE.contains(o.path("kind").asText("")));
        int n = 0;
        if (!vrijgesteld && afgeleid(o) && o.get("type") instanceof ObjectNode t) {
            ObjectNode zusje = zusje(t.get("unit"), KLASSE.get(node));
            if (zusje != null) { t.set("unit", zusje); n++; }
        }
        // the binder a Let opens is in scope of its body, with the declaration of its binding
        Map<String, JsonNode> binnen = scope;
        if ("Let".equals(node)) {
            binnen = new java.util.HashMap<>(scope);
            binnen.put(o.path("binder").asText("").toLowerCase(java.util.Locale.ROOT), gedeclareerd(o.get("value"), d, scope));
        }
        for (var it = o.fields(); it.hasNext(); ) {
            var e = it.next();
            if (e.getKey().equals("type")) continue;   // a DECLARATION never sits under an expression's own type
            n += verdraai(e.getValue(), e.getValue() == duur, d,
                    "Let".equals(node) && e.getKey().equals("body") ? binnen : scope);
        }
        return n;
    }

    /** True iff ch. 2.10 clause 2 DERIVES this position's result unit — the only positions a twist may probe. */
    private static boolean afgeleid(ObjectNode o) {
        String node = o.path("node").asText(null);
        if (node == null) return false;              // not an IR node: a declaration map, a source record, …
        if ("Binary".equals(node)) return AFGELEIDE_OP.contains(o.path("op").asText(""));
        if ("Aggregation".equals(node) || "BuiltinCall".equals(node))
            return AFGELEIDE_KIND.contains(o.path("kind").asText(""));
        return KLASSE.containsKey(node);
    }

    /** A CONVERTIBLE sister of a unit: same canonical, distinct abbreviation, factor x 10^exp — the exact fraction
     *  with its numerator scaled, in lowest terms as the wire writes every factor. */
    private static ObjectNode zusje(JsonNode e, int exp) {
        if (!(e instanceof ObjectNode u)) return null;
        JsonNode a = u.get("abbreviation"), c = u.get("canonical"), f = u.get("factorToCanonical");
        if (a == null || c == null || f == null) return null;
        BigInteger num = f.get("num").bigIntegerValue().multiply(BigInteger.TEN.pow(exp));
        BigInteger den = f.get("den").bigIntegerValue();
        BigInteger ggd = num.gcd(den);
        ObjectNode uit = u.deepCopy();
        uit.put("abbreviation", a.asText() + "-verdraaid" + exp);
        uit.putObject("factorToCanonical").put("num", num.divide(ggd)).put("den", den.divide(ggd));
        return uit;
    }

    private static boolean datumGetypeerd(JsonNode e, Declaraties d, Map<String, JsonNode> scope) {
        JsonNode t = gedeclareerd(e, d, scope);
        return t != null && "Date".equals(t.path("node").asText(""));
    }

    /** The datatype an operand has, as the harness's {@code gedeclareerd} reads it: a reference by the declaration it
     *  names (an attribute up the supertype chain, a parameter, a dimension cell's attribute, a binder's binding),
     *  every other node by its own annotation. */
    private static JsonNode gedeclareerd(JsonNode e, Declaraties d, Map<String, JsonNode> scope) {
        if (e == null || !e.isObject()) return null;
        return switch (e.path("node").asText("")) {
            case "AttributeReference" -> d.attribuut(e.path("subjectType").asText(""), e.path("attribute").asText(""));
            case "ParameterReference" -> d.parameters.get(e.path("parameter").asText("").toLowerCase(java.util.Locale.ROOT));
            case "DimensionCellRead" -> gedeclareerd(e.get("attribute"), d, scope);
            case "BinderRef" -> scope.get(e.path("name").asText("").toLowerCase(java.util.Locale.ROOT));
            default -> e.get("type");
        };
    }

    /** The declarations of one document, lower-cased as the harness keys them. */
    private static final class Declaraties {
        final Map<String, String> supertype = new java.util.HashMap<>();
        final Map<String, JsonNode> attributen = new java.util.HashMap<>();
        final Map<String, JsonNode> parameters = new java.util.HashMap<>();

        Declaraties(JsonNode doc) {
            JsonNode model = doc.path("model");
            for (JsonNode ot : model.path("objectTypes")) {
                String n = ot.path("name").asText("").toLowerCase(java.util.Locale.ROOT);
                if (ot.hasNonNull("supertype")) supertype.put(n, ot.get("supertype").asText().toLowerCase(java.util.Locale.ROOT));
                for (JsonNode a : ot.path("attributes"))
                    attributen.put(n + "|" + a.path("name").asText("").toLowerCase(java.util.Locale.ROOT), a.get("datatype"));
            }
            for (JsonNode p : model.path("parameters"))
                parameters.put(p.path("name").asText("").toLowerCase(java.util.Locale.ROOT), p.get("datatype"));
        }

        JsonNode attribuut(String type, String naam) {
            String a = naam.toLowerCase(java.util.Locale.ROOT);
            for (String t = type.toLowerCase(java.util.Locale.ROOT); t != null; t = supertype.get(t)) {
                JsonNode dt = attributen.get(t + "|" + a);
                if (dt != null) return dt;
            }
            return null;
        }
    }

    // ---------------- helpers ----------------

    private static List<Path> goldens() throws Exception {
        try (Stream<Path> s = Files.walk(CORPUS)) {
            return s.filter(p -> p.getFileName().toString().equals("ir.json")).sorted().toList();
        }
    }

    private static String naam(Path ir) {
        return CORPUS.relativize(ir.getParent()).toString();
    }

    /** The emitted text, built exactly as {@code DroolsBackend}'s DRL branch builds it — the model AND every side
     *  table the loader returns, the document's ch. 4.10 Let index among them. The index is not decoration here: a {@link dev.uchar.regelspraak.ir.Expr.BinderRef}
     *  asked for its unit in isolation has no route back up to its binding, so without it the read falls back to
     *  its own wire annotation and this witness would measure a transpiler no branch uses. */
    private static String drl(JsonNode ir) {
        IrLoader.Loaded l = IrLoader.loadAll(ir);
        return DrlTranspiler.build(l.model(), l.lets(), l.prioritizedDefaults(), l.bronPaden(), l.rolBindingen(),
                l.navigatieBindingen()).drl();
    }

    /** The first line that moved, as "honest | lying" — the diagnostic a bare inequality cannot give. */
    private static String eersteVerschil(String eerlijk, String liegend) {
        String[] a = eerlijk.split("\n"), b = liegend.split("\n");
        for (int i = 0; i < Math.min(a.length, b.length); i++)
            if (!a[i].equals(b[i])) return "line " + (i + 1) + ": [" + a[i].trim() + "] became [" + b[i].trim() + "]";
        return "the texts differ in length (" + a.length + " vs " + b.length + " lines)";
    }
}
