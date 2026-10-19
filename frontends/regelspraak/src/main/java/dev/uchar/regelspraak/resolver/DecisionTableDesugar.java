package dev.uchar.regelspraak.resolver;

import java.util.ArrayList;
import java.util.List;
import static dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic.params;

/**
 * §12 BESLISTABEL desugar (the table-body gap, card 20260624-beslistabel-tabelbody-desugar): turn a captured §12
 * decision-table into ONE ordinary RegelSpraak {@code Regel} source string per (data-row × conclusie-column). Each
 * produced string is fed BACK through the existing {@link Resolver} — so a beslistabel reuses the WHOLE rule pipeline
 * (§9.1 gelijkstelling / §9.2 kenmerktoekenning / §5.7 concatenatie / §8 voorwaarden) and both back-ends get its
 * rows for free, with zero new IR.
 *
 * <p>It takes the table ALREADY split into header cells + data rows (the ANTLR lexer captures each inline markdown
 * pipe line as a {@code TABEL_RIJ} token; {@link RuleChecker} splits it on '|'). A row's cell[0] is the §12 row
 * numbering, used only to name the synthetic regel.
 *
 * <p><b>§12 column model</b> (§12): column 0 is the row numbering. A header that starts with
 * {@code "indien"} is a CONDITIE-kolom; every other header is a CONCLUSIE-kolom (a §9.1 gelijkstelling header ending
 * in "moet gesteld worden op", or a §9.2 kenmerk header "… is …"). Conclusie-kolommen may sit before OR after the
 * conditie-kolommen. Per row: one regel per conclusie-kolom, voorwaarde = the AND of the non-n.v.t. conditie cells.
 *
 * <p><b>§5.7 concatenatie cell</b>: a conditie cell under an equality predicate may list several values separated by a
 * comma and a final " of " ("A", "B" of "C") — a DISJUNCTION over that one column. A multi-value cell therefore
 * desugars to a §13.4.14 "er aan tenminste één van de volgende voorwaarden wordt voldaan" OR-list (reusing the
 * resolver's existing compound-condition support) rather than a verbatim value-series (which the resolver does not
 * lower). Net
 * row shape: AND across conditie-kolommen, OR within a multi-value cell; a disjunction inside an AND nests as a
 * §13.4.14.2 "er voldoet aan tenminste één …" sub-list and is reordered LAST so its greedy bullet capture is harmless.
 *
 * <p><b>Cell value syntax</b>: cells hold native RegelSpraak rule-fragment text — text literals are DOUBLE-quoted
 * ("Friesland"), as a hand-written regel would write them. (The spec's printed tables use single quotes purely as a
 * presentation delimiter; the `.rgs` is real RegelSpraak, where '"' opens a tekstwaarde and "'" does not.)
 */
public final class DecisionTableDesugar {
    private DecisionTableDesugar() {}

    /**
     * Desugar a captured beslistabel into ordinary {@code Regel} source strings (one per data-row × conclusie-column).
     *
     * @param naam    the beslistabel name (each row's regel is named "{@code <naam> <rij-nummer>}")
     * @param geldig  the validity phrase verbatim from the header ("altijd", "vanaf 2024 t/m 2024", …)
     * @param headers the header cells INCLUDING the numbering column at index 0
     * @param rows    the data rows, each a cell list aligned to {@code headers} (cell[0] = the row numbering)
     */
    public static List<String> desugar(String name, String valid, List<String> headers, List<List<String>> rows) {
        return desugar(name, valid, headers, rows, w -> {});
    }

    /**
     * As {@link #desugar(String, String, List, List)} but reporting any row it cannot faithfully render to the
     * {@code warn} sink (the caller surfaces these as diagnostics) — so an un-renderable row is an HONEST skip,
     * never a silent mis-render. Reported: a kenmerk "onwaar" conclusie (RegelSpraak has no negatieve toekenning),
     * and more than one §5.7 disjunction cell in a single EN-row (the nested OR-lists cannot coexist in the bullet
     * grammar — see {@link #conditionClause}).
     */
    public static List<String> desugar(String name, String valid, List<String> headers, List<List<String>> rows,
                                        java.util.function.Consumer<String> warn) {
        return desugarCoded(name, valid, headers, rows, w -> warn.accept(w.text()));
    }

    /** As {@link #desugar(String, String, List, List, java.util.function.Consumer)}, the warnings with their templates. */
    public static List<String> desugarCoded(String name, String valid, List<String> headers, List<List<String>> rows,
                                        java.util.function.Consumer<Typing.Coded> warn) {
        List<Integer> conclusionKol = new ArrayList<>(), conditionKol = new ArrayList<>();
        for (int c = 1; c < headers.size(); c++) {
            if (headers.get(c).trim().toLowerCase().startsWith("indien")) conditionKol.add(c);
            else conclusionKol.add(c);
        }
        List<String> result = new ArrayList<>();
        for (List<String> row : rows) {
            String nr = row.isEmpty() ? "" : row.get(0).trim();
            for (int ci = 0; ci < conclusionKol.size(); ci++) {
                int cc = conclusionKol.get(ci);
                String resultClause = resultPart(name, headers.get(cc).trim(), cellAt(row, cc).trim(), nr, warn);
                if (resultClause == null) continue;   // a forbidden / un-renderable conclusie — resultaatdeel already warned

                // each non-n.v.t. conditie cell -> a Condition (a predicate kop + its 0..n alternative values).
                List<Condition> conditions = new ArrayList<>();
                for (int dc : conditionKol) {
                    String cell = cellAt(row, dc).trim();
                    if (isNvt(cell)) continue;
                    String head = stripIf(headers.get(dc).trim());
                    if (cell.equalsIgnoreCase("waar"))         // a kenmerk conditie that holds -> the kop IS the check
                        conditions.add(new Condition(head, List.of()));
                    else if (cell.equalsIgnoreCase("onwaar"))  // a kenmerk conditie that must NOT hold -> negate the check
                        conditions.add(new Condition(negateCharacteristicHead(head), List.of()));
                    else {
                        // §5.7: "is gelijk aan" series uses "of" (a DISJUNCTION, ≤1 value matches); "is ongelijk aan"
                        // uses "en" (§12: a CONJUNCTION of !=, must differ from ALL), which flattens into
                        // the row's AND as separate conditions. Split respects double quotes (a value may contain , / of).
                        boolean notEqual = head.toLowerCase().contains("ongelijk");
                        List<String> values = splitSequence(cell, notEqual);
                        if (notEqual && values.size() > 1)
                            for (String w : values) conditions.add(new Condition(head, List.of(w)));
                        else
                            conditions.add(new Condition(head, values));
                    }
                }

                String clause = conditionClause(name, conditions, nr, warn);
                if (clause == null) continue;   // un-renderable (>1 disjunction in one EN-row) — warned + skipped
                // name: "<naam> <rij>" (+ a conclusie-column suffix only when a row yields more than one regel).
                String ruleName = name + (nr.isEmpty() ? "" : " " + nr) + (conclusionKol.size() > 1 ? " (" + (ci + 1) + ")" : "");
                result.add("Regel " + ruleName + "\n  geldig " + valid + "\n    " + resultClause + clause + ".\n");
            }
        }
        return result;
    }

    /** A single conditie cell: the predicate kop ("zijn klasse gelijk is aan", the header minus "indien") and
     *  its value(s). A §5.7 concatenatie cell ("A, B of C") under an equality predicate yields &gt;1 value. */
    private record Condition(String head, List<String> values) {
        boolean disjunction() { return values.size() > 1; }
    }

    /** Build the "&nbsp;indien …" clause for a row's conditions (empty if none). One condition → inline (or a §13.4.14
     *  "tenminste één van de volgende voorwaarden" list for a §5.7 disjunction); several → an "alle volgende
     *  voorwaarden" AND-list, any disjunction reordered LAST and nested as a §13.4.14.2 sub-list. */
    private static String conditionClause(String table, List<Condition> conditions, String nr, java.util.function.Consumer<Typing.Coded> warn) {
        if (conditions.isEmpty()) return "";
        if (conditions.size() == 1) {
            Condition v = conditions.get(0);
            return v.disjunction()
                    ? " indien " + disjunctionList(v, "      • ", "er aan tenminste één van de volgende voorwaarden wordt voldaan")
                    : " indien " + elementary(v);
        }
        // AND of several. At most ONE disjunction can be nested (reordered last so its greedy bullet capture is
        // harmless); two nested OR-lists would silently swallow each other's bullets in the §13.4.14 bullet grammar,
        // so a row with >1 disjunction cell is an honest skip rather than a silent collapse.
        long disjunctions = conditions.stream().filter(Condition::disjunction).count();
        if (disjunctions > 1) {
            warn.accept(new Typing.Coded("RGS-300", params("table", table, "row", nr)));
            return null;
        }
        List<Condition> ordered = new ArrayList<>();
        for (Condition v : conditions) if (!v.disjunction()) ordered.add(v);
        for (Condition v : conditions) if (v.disjunction()) ordered.add(v);
        StringBuilder b = new StringBuilder(" indien hij aan alle volgende voorwaarden voldoet:");
        for (Condition v : ordered) {
            if (v.disjunction())
                b.append("\n      • ").append(disjunctionList(v, "        •• ", "er voldoet aan tenminste één van de volgende voorwaarden"));
            else
                b.append("\n      • ").append(elementary(v));
        }
        return b.toString();
    }

    /** One elementary condition: a kenmerk check (no value — the kop IS the condition) or a comparison "kop value". */
    private static String elementary(Condition v) {
        return v.values().isEmpty() ? v.head() : v.head() + " " + v.values().get(0);
    }

    /** Negate a kenmerk-check kop (an "onwaar" conditie cell): a §3.4 bezittelijk check "hij een recht … heeft" →
     *  "hij geen recht … heeft"; a §3.3 bijvoeglijk check with a leading verb "hij is X" → "hij is niet X" or a
     *  trailing verb "hij X is" → "hij X is niet". A kop that matches none is returned with a leading "niet "
     *  (a best-effort the resolver rejects → an honest skip of that row, not a silent mis-render). */
    private static String negateCharacteristicHead(String head) {
        if (head.contains(" een ")) return head.replaceFirst(" een ", " geen ");   // bezittelijk: "heeft een" -> "heeft geen"
        // trailing-verb bijvoeglijk "<vnw> <kenmerk> is" -> the VERIFIED leading-verb negation "<vnw> is niet <kenmerk>"
        // ("hij gepensioneerd is" -> "hij is niet gepensioneerd"). Appending "is niet" instead is parsed as POSITIVE
        // (the trailing "niet" is dropped) — a silent mis-render, hence the rebuild.
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?i)^(hij|zij|ze|het|die|er)\\s+(.+?)\\s+(is|zijn)$").matcher(head);
        if (m.matches()) return m.group(1) + " " + m.group(3) + " niet " + m.group(2);
        if (head.contains(" is ")) return head.replaceFirst(" is ", " is niet ");  // leading-verb: "hij is X" -> "hij is niet X"
        return "niet " + head;   // best-effort -> the resolver rejects -> an HONEST skip of the row (not silent)
    }

    /** A disjunction over one cell's values: a §13.4.14 "… volgende voorwaarden[:]" opener + one bullet per value
     *  ("&lt;kop&gt; &lt;value_i&gt;"). The opener word order differs top-level ("er aan tenminste één … wordt
     *  voldaan", §13.4.14.1) vs nested inside an AND ("er voldoet aan tenminste één …", §13.4.14.2). */
    private static String disjunctionList(Condition v, String bullet, String opener) {
        StringBuilder b = new StringBuilder(opener).append(':');
        for (String w : v.values()) b.append('\n').append(bullet).append(v.head()).append(' ').append(w);
        return b.toString();
    }

    /** A conclusie cell → its resultaatdeel source, or null (with a warning) when §12 forbids it. §12 permits ONLY a
     *  §9.1 gelijkstelling (header "… moet gesteld worden op", cell = the value) or a §9.2 kenmerktoekenning (header
     *  is the assertion, cell = "waar"); everything else is rejected, not silently mis-rendered:
     *  C-7 "(wordt) berekend als" (§12 mandates "moet gesteld worden op"); C-8 an empty conclusie cell; C-9 an inline
     *  "indien" in a value cell; "onwaar" (no RegelSpraak negatieve toekenning); and C-6 any other resultaatdeel
     *  (objectcreatie/feitcreatie/verdeling/consistentie/…). */
    private static String resultPart(String table, String header, String cell, String nr, java.util.function.Consumer<Typing.Coded> warn) {
        String h = header.toLowerCase();
        if (h.contains("moet gesteld worden op")) {   // §9.1 gelijkstelling
            if (cell.isEmpty()) {   // §12 C-8: n.v.t. is conditie-only, a resultaatdeel is mandatory
                warn.accept(new Typing.Coded("RGS-301", params("table", table, "row", nr)));
                return null;
            }
            // §12 C-9: voorwaarden horen in de conditie-kolommen — reject only a STANDALONE "indien" word OUTSIDE any
            // quoted span (so an attribute "het indienjaar" or a Tekst "indien nodig" value is NOT a false positive).
            if (STANDALONE_IF.matcher(maskerQuotes(cell)).find()) {
                warn.accept(new Typing.Coded("RGS-302", params("table", table, "row", nr)));
                return null;
            }
            return header + " " + cell;
        }
        if (h.contains("berekend als")) {   // §12 C-7 — only the forbidden connective (NOT an incidental substring of a gelijkstelling header)
            warn.accept(new Typing.Coded("RGS-303", params("table", table, "row", nr)));
            return null;
        }
        // §9.2 kenmerktoekenning: the header IS the assertion ("een deelnemer is een deelnemer jonger dan 18 jaar").
        if (cell.equalsIgnoreCase("waar")) return header;
        if (cell.equalsIgnoreCase("onwaar")) {
            warn.accept(new Typing.Coded("RGS-304", params("table", table, "row", nr)));
            return null;
        }
        warn.accept(new Typing.Coded("RGS-305", params("table", table, "row", nr)));
        return null;
    }

    /** Split a §5.7 concatenatie cell into its values: at TOP-LEVEL commas and the connector word — " of " for an
     *  equality series, " en " for an inequality series (§12). The split is QUOTE-AWARE: a ',' or the
     *  word inside a double-quoted tekstwaarde (a legal §5.4 value, e.g. {@code "Holland, Noord"} or {@code "A of B"})
     *  is NOT a separator, so the literal stays intact. A single-value cell returns a 1-element list. */
    private static List<String> splitSequence(String cell, boolean notEqual) {
        String connector = notEqual ? "en" : "of";
        // Only a BALANCED quote char (even count) opens a value span — a stray apostrophe stays a literal char and
        // does NOT suppress the §5.7 separators for the rest of the cell.
        boolean dq = even(cell, '"'), sq = even(cell, '\'');
        List<String> values = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        char quote = 0;   // 0 = outside any quote; '"' (tekstwaarde) or '\'' (enumeratiewaarde) = the opening char
        for (int i = 0; i < cell.length(); ) {
            char c = cell.charAt(i);
            if (quote == 0 && ((c == '"' && dq) || (c == '\'' && sq))) { quote = c; cur.append(c); i++; continue; }
            if (quote == c) { quote = 0; cur.append(c); i++; continue; }
            if (quote == 0 && c == ',') { pushValue(values, cur); i++; continue; }
            if (quote == 0 && c == ' ' && connectorOp(cell, i, connector)) {
                pushValue(values, cur);
                i += 2 + connector.length();   // skip " <connector> " (leading space + word + trailing space)
                continue;
            }
            cur.append(c); i++;
        }
        pushValue(values, cur);
        if (values.isEmpty()) values.add(cell.trim());
        return values;
    }
    /** True when {@code q} occurs an even number of times in {@code s} (balanced spans, safe to track as quotes). */
    private static boolean even(String s, char q) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == q) n++;
        return (n & 1) == 0;
    }
    /** Mask the chars inside every BALANCED quoted span with spaces, so a keyword like "indien" inside a tekstwaarde
     *  or enumeratiewaarde is not seen as a standalone token (used by the §12 C-9 check). */
    private static String maskerQuotes(String s) {
        boolean dq = even(s, '"'), sq = even(s, '\'');
        StringBuilder m = new StringBuilder(s.length());
        char quote = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean opener = quote == 0 && ((c == '"' && dq) || (c == '\'' && sq));
            boolean closer = quote == c;
            if (opener) { quote = c; m.append(' '); }
            else if (closer) { quote = 0; m.append(' '); }
            else m.append(quote == 0 ? c : ' ');
        }
        return m.toString();
    }
    private static final java.util.regex.Pattern STANDALONE_IF =
            java.util.regex.Pattern.compile("(?i)(^|\\s)indien(\\s|$)");
    /** True when {@code cell} has the standalone connector word (surrounded by spaces) starting at the space {@code i}. */
    private static boolean connectorOp(String cell, int i, String connector) {
        int w = i + 1, end = w + connector.length();
        return cell.regionMatches(w, connector, 0, connector.length())
                && end < cell.length() && cell.charAt(end) == ' ';
    }
    private static void pushValue(List<String> values, StringBuilder cur) {
        String t = cur.toString().trim();
        if (!t.isEmpty()) values.add(t);
        cur.setLength(0);
    }

    /** Strip a leading "indien " (the conditie header carries it; a bullet / repeated predicate kop does not). */
    private static String stripIf(String cond) {
        return cond.regionMatches(true, 0, "indien ", 0, 7) ? cond.substring(7) : cond;
    }
    private static boolean isNvt(String cell) {
        String c = cell.replace(".", "").trim();
        return c.equalsIgnoreCase("nvt") || c.isEmpty();
    }
    private static String cellAt(List<String> row, int i) { return i < row.size() ? row.get(i) : ""; }
}
