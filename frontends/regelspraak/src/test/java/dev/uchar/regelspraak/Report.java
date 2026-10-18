package dev.uchar.regelspraak;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WF-113 read off an emitted document, the way a consumer reads it: (a) every diagnostic has the severity
 * {@code warning}, and (b) every {@code Unresolved} node is accounted for — it carries a source snippet that is not
 * blank, and some warning's {@code message} contains it verbatim. The check reads the JSON TEXT and not the model: the sweep that raises the WARNINGs walks the model, so
 * a check over the model would share its blind spots, and a marker the emitter writes from a place the sweep does
 * not visit would pass unseen. The frontend links no JSON library (jar-only: ANTLR and JUnit), hence the small
 * reader below; it reads the JSON the emitter writes and fails on anything else.
 */
final class Report {

    private Report() {}

    /** Fails on the first diagnostic that is no warning and on the first marker no warning accounts for. */
    static void assertAccountedFor(String json) {
        Map<?, ?> document = (Map<?, ?>) new Reader(json).document();
        List<String> meldingen = new ArrayList<>();
        for (Object o : (List<?>) document.get("diagnostics")) {
            Map<?, ?> d = (Map<?, ?>) o;
            assertEquals("warning", d.get("severity"), () -> "WF-113 (a): de wire draagt alleen warning: " + d);
            meldingen.add((String) d.get("message"));
        }
        for (String source : markerSources(document)) {
            // blank counts as absent: an empty snippet is contained in every message, and an absent one in none
            assertTrue(source != null && !source.isBlank(),
                    () -> "WF-113 (b): een marker zonder brontekst kan door geen warning verantwoord worden: " + source);
            assertTrue(meldingen.stream().anyMatch(m -> m.contains(source)),
                    () -> "WF-113 (b): geen warning citeert de brontekst van de marker letterlijk: '" + source
                            + "'; de meldingen: " + meldingen);
        }
    }

    /** The source snippet of every Unresolved node in the document, in document order; null for a marker that
     *  carries no source record. */
    static List<String> markerSources(String json) {
        return markerSources(new Reader(json).document());
    }

    private static List<String> markerSources(Object document) {
        List<String> result = new ArrayList<>();
        collect(document, result);
        return result;
    }

    /** A JSON text as the reader below gives it: objects as ordered maps, arrays as lists. The name table's test
     *  reads the repository's name table with it rather than with a second reader. */
    static Object json(String tekst) {
        return new Reader(tekst).document();
    }

    private static void collect(Object x, List<String> result) {
        if (x instanceof Map<?, ?> m) {
            if ("Unresolved".equals(m.get("node")))
                result.add(m.get("source") instanceof Map<?, ?> source ? (String) source.get("snippet") : null);
            for (Object v : m.values()) collect(v, result);
        } else if (x instanceof List<?> l) {
            for (Object v : l) collect(v, result);
        }
    }

    /** Objects as ordered maps, arrays as lists, strings unescaped, numbers as their lexeme. */
    private static final class Reader {
        private final String s;
        private int i;

        Reader(String s) { this.s = s; }

        Object document() {
            Object d = value();
            spaces();
            if (i != s.length()) throw error("tekst na het document");
            return d;
        }

        private Object value() {
            spaces();
            if (i >= s.length()) throw error("einde van de tekst");
            char c = s.charAt(i);
            if (c == '{') return object();
            if (c == '[') return list();
            if (c == '"') return tekst();
            if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            if (s.startsWith("null", i)) { i += 4; return null; }
            int begin = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            if (begin == i) throw error("onverwacht teken '" + c + "'");
            return s.substring(begin, i);
        }

        private Map<String, Object> object() {
            i++;
            Map<String, Object> m = new LinkedHashMap<>();
            spaces();
            if (s.charAt(i) == '}') { i++; return m; }
            while (true) {
                spaces();
                String k = tekst();
                spaces();
                if (s.charAt(i++) != ':') throw error("':' verwacht");
                m.put(k, value());
                spaces();
                char c = s.charAt(i++);
                if (c == '}') return m;
                if (c != ',') throw error("',' of '}' verwacht");
            }
        }

        private List<Object> list() {
            i++;
            List<Object> l = new ArrayList<>();
            spaces();
            if (s.charAt(i) == ']') { i++; return l; }
            while (true) {
                l.add(value());
                spaces();
                char c = s.charAt(i++);
                if (c == ']') return l;
                if (c != ',') throw error("',' of ']' verwacht");
            }
        }

        private String tekst() {
            if (s.charAt(i++) != '"') throw error("'\"' verwacht");
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = s.charAt(i++);
                if (c == '"') return sb.toString();
                if (c != '\\') { sb.append(c); continue; }
                char e = s.charAt(i++);
                switch (e) {
                    case '"', '\\', '/' -> sb.append(e);
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> { sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; }
                    default -> throw error("onbekende escape '\\" + e + "'");
                }
            }
        }

        private void spaces() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }

        private AssertionError error(String what) {
            return new AssertionError("geen JSON zoals de emitter hem schrijft, op positie " + i + ": " + what);
        }
    }
}
