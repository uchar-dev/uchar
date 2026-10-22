package dev.uchar.regelspraak;

import dev.uchar.regelspraak.parser.RegelSpraakLexer;
import dev.uchar.regelspraak.resolver.SemanticTokens;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The properties of the semantic-token output that hold for every input (grammar/DIVERGENCES.md, "Semantic tokens"),
 * checked from the outside: the text of each span is the source text at its position, counted in UTF-16 code units;
 * spans do not overlap and carry no whitespace at their edges; every character outside whitespace lies in a span; every
 * token of the lexer lies in exactly one span; and every reference points at a span that is a declaration of the same
 * kind. The tokens are lexed here again, over the same chain of files the frontend reads, so the check does not share
 * the frontend's own reading of them. A decision-table row is one token of the lexer that the frontend splits into its
 * cells; for such a row the character check is the one that holds.
 */
final class SemanticTokensCheck {

    private SemanticTokensCheck() {}

    /** Every property above; {@code files} are the sources the document was made from, in the same order. */
    static void check(List<SemanticTokens.SourceFile> files, SemanticTokens.Document d) {
        assertEquals(files.size(), d.files().size(), "one entry per file read");
        for (int f = 0; f < files.size(); f++) {
            String text = files.get(f).text();
            SemanticTokens.FileSpans fs = d.files().get(f);
            assertEquals(files.get(f).name(), fs.file());
            int[] lineStart = lineStarts(text);
            int previousEnd = 0;
            int[] owner = new int[text.length()];
            java.util.Arrays.fill(owner, -1);
            for (int i = 0; i < fs.spans().size(); i++) {
                SemanticTokens.Span s = fs.spans().get(i);
                int start = offset(lineStart, text, s.line(), s.column(), fs.file());
                int end = offset(lineStart, text, s.endLine(), s.endColumn(), fs.file());
                String where = fs.file() + ":" + s.line() + ":" + s.column();
                assertTrue(start < end, () -> "an empty span at " + where);
                assertEquals(text.substring(start, end), s.text(), () -> "the text of the span at " + where);
                assertTrue(!Character.isWhitespace(text.charAt(start)) && !Character.isWhitespace(text.charAt(end - 1)),
                        () -> "whitespace at the edge of the span at " + where);
                assertTrue(start >= previousEnd, () -> "the span at " + where + " overlaps the one before it or is out of order");
                previousEnd = end;
                for (int k = start; k < end; k++) owner[k] = i;
            }
            for (int k = 0; k < text.length(); k++) {
                if (Character.isWhitespace(text.charAt(k)) || owner[k] >= 0) continue;
                int line = line(lineStart, k);
                fail("character '" + text.charAt(k) + "' at " + fs.file() + ":" + line + ":" + (k - lineStart[line - 1])
                        + " lies in no span");
            }
            for (int[] token : tokens(files, f)) {
                if (token[2] == RegelSpraakLexer.TABEL_RIJ) continue;
                int span = owner[token[0]];
                for (int k = token[0]; k < token[1]; k++)
                    if (owner[k] != span) {
                        int line = line(lineStart, token[0]);
                        fail("the token '" + text.substring(token[0], token[1]) + "' at " + fs.file() + ":" + line + ":"
                                + (token[0] - lineStart[line - 1]) + " lies in more than one span");
                    }
            }
        }
        List<SemanticTokens.Span> declarations = new ArrayList<>();
        List<String> declarationFiles = new ArrayList<>();
        for (SemanticTokens.FileSpans fs : d.files())
            for (SemanticTokens.Span s : fs.spans())
                if (s.declaration()) { declarations.add(s); declarationFiles.add(fs.file()); }
        for (SemanticTokens.FileSpans fs : d.files())
            for (SemanticTokens.Span s : fs.spans()) {
                String where = fs.file() + ":" + s.line() + ":" + s.column() + " '" + s.text() + "'";
                if (s.declaration()) {
                    assertTrue(s.kind().names(), () -> "a declaration of kind " + s.kind().wire() + " at " + where);
                    assertTrue(s.declaredAt() == null, () -> "a declaration that points elsewhere at " + where);
                    continue;
                }
                if (s.generatedFrom() != null) {
                    assertTrue(s.kind().names() && s.declaredAt() == null,
                            () -> "a generated name is a name without a declaration of its own at " + where);
                    assertTrue(declarationAt(declarations, declarationFiles, s.generatedFrom(), null),
                            () -> "the generated name at " + where + " comes from " + s.generatedFrom()
                                    + ", where no declaration stands");
                }
                if (s.declaredAt() == null) continue;
                assertTrue(s.kind().names(), () -> "a span of kind " + s.kind().wire() + " points at a declaration at " + where);
                assertTrue(declarationAt(declarations, declarationFiles, s.declaredAt(), s.kind()),
                        () -> "the reference at " + where + " points at " + s.declaredAt()
                                + ", where no declaration of kind " + s.kind().wire() + " stands");
            }
    }

    /** Whether a declaration span of {@code kind} (any kind for null) starts at {@code at}. */
    private static boolean declarationAt(List<SemanticTokens.Span> declarations, List<String> files,
                                         SemanticTokens.Location at, SemanticTokens.Kind kind) {
        for (int i = 0; i < declarations.size(); i++) {
            SemanticTokens.Span decl = declarations.get(i);
            if (files.get(i).equals(at.file()) && decl.line() == at.line() && decl.column() == at.column()
                    && (kind == null || decl.kind() == kind)) return true;
        }
        return false;
    }

    /** Whether {@code s} is a reference without a declaration where the kind requires one: every kind but a unit,
     *  which may be a time unit or a sign that no system of the files declares, and every name the frontend generates,
     *  which points at the declaration it comes from instead. */
    static boolean unlinked(SemanticTokens.Span s) {
        return s.kind().names() && !s.declaration() && s.declaredAt() == null && s.generatedFrom() == null
                && s.kind() != SemanticTokens.Kind.UNIT;
    }

    /** The tokens of file {@code f} as {start, end, type}, UTF-16 offsets into its text without the whitespace at the
     *  edges, lexed over the chain the frontend reads (each text followed by a line end). */
    static List<int[]> tokens(List<SemanticTokens.SourceFile> files, int f) {
        StringBuilder chain = new StringBuilder();
        int from = 0;
        for (int i = 0; i < files.size(); i++) {
            if (i == f) from = chain.length();
            chain.append(files.get(i).text()).append('\n');
        }
        String text = chain.toString();
        int to = from + files.get(f).text().length();
        int[] offset = new int[text.codePointCount(0, text.length()) + 1];
        for (int cp = 0, i = 0; ; cp++) {
            offset[cp] = i;
            if (i >= text.length()) break;
            i += Character.charCount(text.codePointAt(i));
        }
        RegelSpraakLexer lexer = new RegelSpraakLexer(CharStreams.fromString(text));
        lexer.removeErrorListeners();
        List<int[]> result = new ArrayList<>();
        for (Token t : lexer.getAllTokens()) {
            int s = offset[t.getStartIndex()], e = offset[t.getStopIndex() + 1];
            while (s < e && Character.isWhitespace(text.charAt(s))) s++;
            while (e > s && Character.isWhitespace(text.charAt(e - 1))) e--;
            if (s >= e || s < from || e > to) continue;
            result.add(new int[]{s - from, e - from, t.getType()});
        }
        return result;
    }

    static int[] lineStarts(String text) {
        List<Integer> starts = new ArrayList<>(List.of(0));
        for (int i = 0; i < text.length(); i++) if (text.charAt(i) == '\n') starts.add(i + 1);
        return starts.stream().mapToInt(Integer::intValue).toArray();
    }

    static int line(int[] lineStart, int offset) {
        int line = 1;
        while (line < lineStart.length && lineStart[line] <= offset) line++;
        return line;
    }

    static int offset(int[] lineStart, String text, int line, int column, String file) {
        assertTrue(line >= 1 && line <= lineStart.length, () -> "line " + line + " is not in " + file);
        int o = lineStart[line - 1] + column;
        assertTrue(o <= text.length(), () -> "column " + column + " of line " + line + " is past the end of " + file);
        return o;
    }
}
