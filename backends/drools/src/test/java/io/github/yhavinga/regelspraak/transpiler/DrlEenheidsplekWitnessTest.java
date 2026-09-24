package io.github.yhavinga.regelspraak.transpiler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.yhavinga.regelspraak.loader.IrLoader;

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
 * twists a parsed tree for one; if the harness's table gains a class, this list must gain it too.
 */
class DrlEenheidsplekWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** The position classes the harness twists, with its per-node-kind restatement exponent. */
    private static final Map<String, Integer> KLASSE = Map.of(
            "AttribuutRef", 1, "ParameterRef", 2, "DimensieCel", 3, "BinderRef", 4,
            "Binair", 5, "Let", 6, "Aggregation", 7, "BuiltinCall", 7, "Afgerond", 8, "Begrensd", 9);
    /** ch. 2.10 clause 2 splits Binair and the two function nodes by OPERATOR: only these branches derive a result
     *  unit. Every kind that derives is an aggregation's, so no BuiltinCall is ever twisted. */
    private static final Set<String> AFGELEIDE_OP = Set.of("PLUS", "MIN", "VERMINDERD_MET", "MAAL");
    private static final Set<String> AFGELEIDE_KIND = Set.of("SOM", "TOTAAL", "MINIMUM", "MAXIMUM", "EERSTE", "LAATSTE");
    /** WF-038: a tijdsduur's unit is the operator's parameter, not a derived result type. */
    private static final Set<String> GEEN_FUNCTIE = Set.of("TIJDSDUUR", "ABSOLUTE_TIJDSDUUR");

    @Test void de_drl_tekst_volgt_de_declaraties_niet_de_wire_herhaling() throws Exception {
        int twists = 0;
        Set<String> geraakt = new LinkedHashSet<>();
        List<String> afwijkend = new ArrayList<>();
        for (Path ir : goldens()) {
            JsonNode eerlijk = JSON.readTree(Files.readString(ir));
            JsonNode liegend = eerlijk.deepCopy();
            int n = verdraai(liegend, false);
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

    /** Graft the sister unit onto every derived position under {@code x}; return the number of grafts. */
    private static int verdraai(JsonNode x, boolean vrijgesteldDoorOuder) {
        if (x.isArray()) {
            int n = 0;
            for (JsonNode kind : x) n += verdraai(kind, false);
            return n;
        }
        if (!(x instanceof ObjectNode o)) return 0;
        String node = o.path("node").asText(null);
        // WF-035: the DURATION operand of a date +/- duration carries the calendar unit ref routes on
        JsonNode duur = null;
        if ("Binair".equals(node) && Set.of("PLUS", "MIN", "VERMINDERD_MET").contains(o.path("op").asText(""))) {
            JsonNode l = o.get("links"), r = o.get("rechts");
            if (datumGetypeerd(l) && !datumGetypeerd(r)) duur = r;
            else if (datumGetypeerd(r) && !datumGetypeerd(l)) duur = l;
        }
        boolean vrijgesteld = vrijgesteldDoorOuder || "Literal".equals(node)
                || ("BuiltinCall".equals(node) && GEEN_FUNCTIE.contains(o.path("kind").asText("")));
        int n = 0;
        if (!vrijgesteld && afgeleid(o) && o.get("type") instanceof ObjectNode t) {
            ObjectNode zusje = zusje(t.get("unit"), KLASSE.get(node));
            if (zusje != null) { t.set("unit", zusje); n++; }
        }
        for (var it = o.fields(); it.hasNext(); ) {
            var e = it.next();
            if (e.getKey().equals("type")) continue;   // a DECLARATION never sits under an expression's own type
            n += verdraai(e.getValue(), e.getValue() == duur);
        }
        return n;
    }

    /** True iff ch. 2.10 clause 2 DERIVES this position's result unit — the only positions a twist may probe. */
    private static boolean afgeleid(ObjectNode o) {
        String node = o.path("node").asText(null);
        if (node == null) return false;              // not an IR node: a declaration map, a bron string, …
        if ("Binair".equals(node)) return AFGELEIDE_OP.contains(o.path("op").asText(""));
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

    private static boolean datumGetypeerd(JsonNode e) {
        return e != null && e.get("type") != null && "Date".equals(e.get("type").path("node").asText(""));
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

    /** The emitted text, built exactly as {@code CaseRunner}'s DRL branch builds it — model AND the document's
     *  ch. 4.10 Let index. The index is not decoration here: a {@link io.github.yhavinga.regelspraak.ir.Expr.BinderRef}
     *  asked for its unit in isolation has no route back up to its binding, so without it the read falls back to
     *  its own wire annotation and this witness would measure a transpiler no branch uses. */
    private static String drl(JsonNode ir) {
        IrLoader.Loaded l = IrLoader.loadAll(ir);
        return DrlTranspiler.build(l.model(), l.lets()).drl();
    }

    /** The first line that moved, as "honest | lying" — the diagnostic a bare inequality cannot give. */
    private static String eersteVerschil(String eerlijk, String liegend) {
        String[] a = eerlijk.split("\n"), b = liegend.split("\n");
        for (int i = 0; i < Math.min(a.length, b.length); i++)
            if (!a[i].equals(b[i])) return "line " + (i + 1) + ": [" + a[i].trim() + "] became [" + b[i].trim() + "]";
        return "the texts differ in length (" + a.length + " vs " + b.length + " lines)";
    }
}
