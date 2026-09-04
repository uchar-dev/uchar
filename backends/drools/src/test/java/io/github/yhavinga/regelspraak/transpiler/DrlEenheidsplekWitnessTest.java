package io.github.yhavinga.regelspraak.transpiler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.yhavinga.regelspraak.loader.IrLoader;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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
 * <p><b>What it does not add, measured and said out loud.</b> With {@code EffectieveEenheid.van} put back on
 * {@code e.type()} (the state before that class existed) this witness goes red on 9 goldens — the same nine
 * the harness's DRL leg catches. The other three twistable goldens (lichterloon, vondstrecht, seizoenvondst)
 * stay green under that mutant, and the reason is not luck: their unit-bearing positions sit under a
 * construct the DRL branch MARKERS (the ch. 4.10 Let), so no rescale factor is emitted there for a lie to
 * move. Wider reach, identical catch — today.
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
            "Binair", 5, "Let", 6, "Functie", 7, "Afgerond", 8, "Begrensd", 9);
    /** ch. 2.10 clause 2 splits Binair and Functie by OPERATOR: only these branches derive a result unit. */
    private static final Set<String> AFGELEIDE_OP = Set.of("PLUS", "MIN", "VERMINDERD_MET", "MAAL");
    private static final Set<String> AFGELEIDE_KIND = Set.of("SOM", "TOTAAL", "MINIMUM", "MAXIMUM", "EERSTE", "LAATSTE");
    /** WF-038: a tijdsduur's unit is the operator's parameter, not a derived result type. */
    private static final Set<String> GEEN_FUNCTIE = Set.of("TIJDSDUUR", "ABSOLUTE_TIJDSDUUR");
    private static final Set<String> DATUM = Set.of("DatumInDagen", "DatumTijd");

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
                || ("Functie".equals(node) && GEEN_FUNCTIE.contains(o.path("kind").asText("")));
        int n = 0;
        if (!vrijgesteld && afgeleid(o) && o.get("type") instanceof ObjectNode t) {
            ObjectNode zusje = zusje(t.get("eenheid"), KLASSE.get(node));
            if (zusje != null) { t.set("eenheid", zusje); n++; }
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
        if ("Functie".equals(node)) return AFGELEIDE_KIND.contains(o.path("kind").asText(""));
        return KLASSE.containsKey(node);
    }

    /** A CONVERTIBLE sister of a unit: same canonical, distinct abbreviation, factor x 10^exp. */
    private static ObjectNode zusje(JsonNode e, int exp) {
        if (!(e instanceof ObjectNode u)) return null;
        JsonNode a = u.get("afkorting"), c = u.get("canoniek"), f = u.get("factorNaarCanoniek");
        if (a == null || a.isNull() || c == null || c.isNull() || f == null || f.isNull()) return null;
        ObjectNode uit = u.deepCopy();
        uit.put("afkorting", a.asText() + "-verdraaid" + exp);
        uit.put("factorNaarCanoniek", new BigDecimal(f.asText()).scaleByPowerOfTen(exp));
        return uit;
    }

    private static boolean datumGetypeerd(JsonNode e) {
        return e != null && e.get("type") != null && DATUM.contains(e.get("type").path("node").asText(""));
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

    private static String drl(JsonNode ir) {
        return DrlTranspiler.transpile(IrLoader.loadAll(ir).model());
    }

    /** The first line that moved, as "honest | lying" — the diagnostic a bare inequality cannot give. */
    private static String eersteVerschil(String eerlijk, String liegend) {
        String[] a = eerlijk.split("\n"), b = liegend.split("\n");
        for (int i = 0; i < Math.min(a.length, b.length); i++)
            if (!a[i].equals(b[i])) return "line " + (i + 1) + ": [" + a[i].trim() + "] became [" + b[i].trim() + "]";
        return "the texts differ in length (" + a.length + " vs " + b.length + " lines)";
    }
}
