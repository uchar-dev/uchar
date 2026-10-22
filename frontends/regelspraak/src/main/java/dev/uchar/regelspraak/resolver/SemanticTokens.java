package dev.uchar.regelspraak.resolver;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.FactType;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;
import dev.uchar.regelspraak.model.RegelSpraakModel.Role;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.parser.RegelSpraak.*;
import dev.uchar.regelspraak.parser.RegelSpraakLexer;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The semantic tokens of a RegelSpraak source (grammar/DIVERGENCES.md, "Semantic tokens"): per file a list of spans
 * that covers every token of the lexer, each with a kind and a position, and for a name the declaration it names. An
 * output beside the IR and never on the wire, for a consumer that shows the text: the IR is the abstract form and
 * carries no position per name.
 *
 * <p>A pass AFTER the resolution, over the token stream and the parse tree, asking the finished model what each run of
 * words names with the lookups the resolution uses ({@link RegelSpraakModel#objectTypeByName},
 * {@link ObjectType#attributeName}, {@link PluralMatcher}, {@link CharacteristicMatcher},
 * {@link RegelSpraakModel#roleNameMatches}, …). Not a recorder inside the resolution: that binds a name on its TEXT
 * (a kaleNaam is re-split against the model, a multi-word characteristic is put together from two parse nodes), and
 * much of it tries a reading and falls back, so a token position would have to travel through every one of those
 * string operations. Reading the text afterwards sees the same words the resolution saw. Where the two could part, the
 * corpus witness of the frontend suite measures it: a document without diagnostics has no span {@code unresolved}.
 *
 * <p>Coverage: every token of the lexer, on the default channel and on the hidden one, lies in exactly one span, apart
 * from the whitespace at its edges; spans do not overlap; whitespace between tokens lies outside every span. A name of
 * several words is one span, also over a line end; a comment inside a name splits it in two spans of the same name. A
 * decision-table row, one token of the lexer, is split as the resolution splits it: its pipes and the tokens of each
 * cell are the units of coverage there.
 *
 * <p>Position: line from 1 in the file the span stands in, column from 0 in UTF-16 code units (the Language Server
 * Protocol's default position encoding and the unit of a JavaScript string); the end is exclusive.
 */
public final class SemanticTokens {

    /** What a span is. The first group follows from the lexer alone; the second from the resolution. The names of the
     *  kinds are the wire's where the wire names the concept (grammar/DIVERGENCES.md, "Semantic tokens", gives the
     *  closest LSP 3.17 token type of each). */
    public enum Kind {
        KEYWORD("keyword"), COMMENT("comment"), HEADING("heading"), NUMBER("number"), DATE("date"), STRING("string"),
        PUNCTUATION("punctuation"), DESIGNATION("designation"), WORD("word"), RELATION_DESCRIPTION("relationDescription"),
        OBJECT_TYPE("objectType"), ATTRIBUTE("attribute"), CHARACTERISTIC("characteristic"), ROLE("role"),
        FACT_TYPE("factType"), PARAMETER("parameter"), DOMAIN("domain"), ENUMERATION_VALUE("enumerationValue"),
        UNIT_SYSTEM("unitSystem"), UNIT("unit"), DIMENSION("dimension"), DIMENSION_LABEL("dimensionLabel"),
        TIMELINE("timeline"), DAY_KIND("dayKind"), VOCABULARY("vocabulary"), NAMED_FORM("namedForm"),
        RULE_GROUP("ruleGroup"), VARIABLE("variable"),
        UNRESOLVED("unresolved");

        private final String wire;
        Kind(String wire) { this.wire = wire; }

        /** The kind's name in the output. */
        public String wire() { return wire; }

        /** Whether a span of this kind names a declaration: it is the declaration, or a reference to one. */
        public boolean names() { return ordinal() >= OBJECT_TYPE.ordinal() && ordinal() <= VARIABLE.ordinal(); }
    }

    /** One source file: the name the output gives it, and its text. */
    public record SourceFile(String name, String text) {
        public SourceFile {
            if (name == null || text == null) throw new IllegalArgumentException("een bronbestand heeft een naam en een tekst");
        }
    }

    /** A place in a source file: line from 1, column from 0 in UTF-16 code units. */
    public record Location(String file, int line, int column) {}

    /** One span: start and exclusive end, its kind, its text as written, whether it is a declaration, for a
     *  reference the place of the span of its declaration (null when that is in no file read, or for no name), and
     *  for a name of an element the frontend generates — a member of a lowered opgave marker — the place of the
     *  declaration it is generated from (grammar/DIVERGENCES.md, "Gegenereerde elementen"). */
    public record Span(int line, int column, int endLine, int endColumn, Kind kind, String text,
                       boolean declaration, Location declaredAt, Location generatedFrom) {}

    /** The spans of one file, in source order. */
    public record FileSpans(String file, List<Span> spans) {}

    /** A diagnostic of the resolution with the file and the line in that file it stands on; {@code file} null and
     *  {@code line} 0 for a diagnostic without a place. */
    public record FileDiagnostic(Severity severity, String code, Map<String, String> params, String message,
                                 String file, int line) {}

    /** The whole output: the spans per file in the order the files were given, and the diagnostics. */
    public record Document(List<FileSpans> files, List<FileDiagnostic> diagnostics) {

        /** The JSON text of the output (grammar/DIVERGENCES.md, "Semantic tokens"): one span per line, every
         *  character outside printable ASCII written as an escape, so the text is the same on every platform. */
        public String toJson() {
            StringBuilder sb = new StringBuilder("{\n  \"files\": [");
            for (int f = 0; f < files.size(); f++) {
                FileSpans fs = files.get(f);
                sb.append(f == 0 ? "\n" : ",\n").append("    {\n      \"file\": ");
                string(sb, fs.file());
                sb.append(",\n      \"spans\": [");
                for (int i = 0; i < fs.spans().size(); i++) {
                    Span s = fs.spans().get(i);
                    sb.append(i == 0 ? "\n" : ",\n").append("        {\"line\": ").append(s.line())
                            .append(", \"column\": ").append(s.column()).append(", \"endLine\": ").append(s.endLine())
                            .append(", \"endColumn\": ").append(s.endColumn()).append(", \"kind\": ");
                    string(sb, s.kind().wire());
                    sb.append(", \"text\": ");
                    string(sb, s.text());
                    if (s.declaration()) sb.append(", \"declaration\": true");
                    if (s.declaredAt() != null) location(sb, "declaredAt", s.declaredAt());
                    if (s.generatedFrom() != null) location(sb, "generatedFrom", s.generatedFrom());
                    sb.append('}');
                }
                sb.append(fs.spans().isEmpty() ? "]\n    }" : "\n      ]\n    }");
            }
            sb.append(files.isEmpty() ? "],\n  \"diagnostics\": [" : "\n  ],\n  \"diagnostics\": [");
            for (int i = 0; i < diagnostics.size(); i++) {
                FileDiagnostic d = diagnostics.get(i);
                sb.append(i == 0 ? "\n" : ",\n").append("    {\"severity\": ");
                string(sb, d.severity().name());
                if (d.code() != null) { sb.append(", \"code\": "); string(sb, d.code()); }
                sb.append(", \"params\": {");
                int n = 0;
                for (var e : d.params().entrySet()) {
                    if (n++ > 0) sb.append(", ");
                    string(sb, e.getKey());
                    sb.append(": ");
                    string(sb, e.getValue());
                }
                sb.append("}, \"message\": ");
                string(sb, d.message());
                if (d.file() != null) { sb.append(", \"file\": "); string(sb, d.file()); }
                sb.append(", \"line\": ").append(d.line()).append('}');
            }
            sb.append(diagnostics.isEmpty() ? "]\n}\n" : "\n  ]\n}\n");
            return sb.toString();
        }

        private static void location(StringBuilder sb, String key, Location l) {
            sb.append(", \"").append(key).append("\": {\"file\": ");
            string(sb, l.file());
            sb.append(", \"line\": ").append(l.line()).append(", \"column\": ").append(l.column()).append('}');
        }

        private static void string(StringBuilder sb, String s) {
            sb.append('"');
            for (int i = 0; i < s.length(); i++) {
                char ch = s.charAt(i);
                switch (ch) {
                    case '"' -> sb.append("\\\"");
                    case '\\' -> sb.append("\\\\");
                    case '\n' -> sb.append("\\n");
                    case '\r' -> sb.append("\\r");
                    case '\t' -> sb.append("\\t");
                    default -> {
                        if (ch < 0x20 || ch > 0x7e) sb.append(String.format("\\u%04x", (int) ch));
                        else sb.append(ch);
                    }
                }
            }
            sb.append('"');
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // State: the text, the files in it, the pieces of coverage and what claims them.
    // ---------------------------------------------------------------------------------------------------------------

    /** A unit of coverage: a token of the lexer, or a piece of a decision-table row (a pipe, or a token of a cell
     *  lexed on its own). Offsets are UTF-16 offsets into the chain of files, the whitespace at the edges left out;
     *  {@code end} is exclusive. */
    private record Piece(int type, boolean hidden, int start, int end, int streamIndex) {}

    /** A name: the pieces from {@code first} to {@code last} (the hidden ones between them excepted), its kind, whether
     *  it is the declaration, the declaration it names, and for a generated element the declaration it comes from. */
    private record Name(int first, int last, Kind kind, boolean declaration, Decl target, Decl generatedFrom) {
        Name(int first, int last, Kind kind, boolean declaration, Decl target) {
            this(first, last, kind, declaration, target, null);
        }
    }

    /** A declared thing: its kind, the declaration that scopes it (the object type of an attribute, the fact type of a
     *  role, …; null where the kind is not scoped), and its name, both lower-cased. */
    private record Decl(Kind kind, String owner, String name) {
        static Decl of(Kind kind, String owner, String name) {
            return new Decl(kind, owner == null ? null : norm(owner), norm(name));
        }
    }

    private final String chain;
    private final List<SourceFile> files;
    private final int[] fileStart;          // UTF-16 offset of each file in the chain
    private final int[] lineStart;          // UTF-16 offset of each line of the chain (index: line - 1)
    private final int[] codePointOffset;    // code point index in the chain -> UTF-16 offset
    private final int[] firstLine;          // chain line of each file's first line
    private final List<Token> stream;
    private final RegelSpraakModel model;
    private final boolean recovered;        // the parse reported a syntax error, so the tree is ANTLR's recovery

    private final List<Piece> pieces = new ArrayList<>();
    private final int[] pieceOfToken;       // stream token index -> piece index, -1 for none (a split row, whitespace)
    private final Name[] claim;
    private final Map<Decl, Integer> declarations = new LinkedHashMap<>();
    private final ExpressionBuilder builder;
    /** The most pieces a name in a rule can take: the longest declared name, with room for an inflection. */
    private int longest;

    private SemanticTokens(String chain, List<SourceFile> files, int[] firstLine, CommonTokenStream tokens,
                           RegelSpraakModel model, boolean recovered) {
        this.chain = chain;
        this.files = files;
        this.firstLine = firstLine;
        this.model = model;
        this.recovered = recovered;
        this.stream = tokens.getTokens();
        this.fileStart = new int[files.size() + 1];
        int offset = 0;
        for (int i = 0; i < files.size(); i++) {
            fileStart[i] = offset;
            offset += files.get(i).text().length() + 1;
        }
        fileStart[files.size()] = offset;
        List<Integer> starts = new ArrayList<>();
        starts.add(0);
        for (int i = 0; i < chain.length(); i++) if (chain.charAt(i) == '\n') starts.add(i + 1);
        lineStart = starts.stream().mapToInt(Integer::intValue).toArray();
        codePointOffset = new int[chain.codePointCount(0, chain.length()) + 1];
        for (int cp = 0, i = 0; ; cp++) {
            codePointOffset[cp] = i;
            if (i >= chain.length()) break;
            i += Character.charCount(chain.codePointAt(i));
        }
        pieceOfToken = new int[stream.size()];
        java.util.Arrays.fill(pieceOfToken, -1);
        for (Token t : stream) {
            if (t.getType() == Token.EOF) continue;
            int start = codePointOffset[t.getStartIndex()], end = codePointOffset[t.getStopIndex() + 1];
            if (t.getType() == RegelSpraakLexer.TABEL_RIJ) { tableRow(start, end); continue; }
            int[] trimmed = trim(start, end);
            if (trimmed == null) continue;
            pieceOfToken[t.getTokenIndex()] = pieces.size();
            pieces.add(new Piece(t.getType(), t.getChannel() != Token.DEFAULT_CHANNEL, trimmed[0], trimmed[1],
                    t.getTokenIndex()));
        }
        claim = new Name[pieces.size()];
        // the subject type of a rule and the holder of an attribute come from the resolution's own builder, run on a
        // copy so that a diagnostic it raises again does not reach the model the caller reads
        builder = new ExpressionBuilder(model.copy());
    }

    /** The semantic tokens of {@code files}, resolved as the chain {@code chain} (each file's text followed by a line
     *  end, as {@code Resolver --ir} reads several files) into {@code model} from {@code doc} over {@code tokens};
     *  {@code firstLine} holds the line of the chain each file begins on. */
    static Document of(List<SourceFile> files, String chain, int[] firstLine, CommonTokenStream tokens,
                       RegelSpraakDocumentContext doc, RegelSpraakModel model, boolean recovered) {
        SemanticTokens s = new SemanticTokens(chain, files, firstLine, tokens, model, recovered);
        s.declarations(doc);
        for (Decl d : s.declarations.keySet()) s.longest = Math.max(s.longest, d.name().split(" ").length + 2);
        s.references(doc);
        s.ruleTexts(doc);
        return s.document(model);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Pieces.
    // ---------------------------------------------------------------------------------------------------------------

    /** {@code [start, end)} without its whitespace at the edges, or null when nothing else is in it. */
    private int[] trim(int start, int end) {
        while (start < end && Character.isWhitespace(chain.charAt(start))) start++;
        while (end > start && Character.isWhitespace(chain.charAt(end - 1))) end--;
        return start < end ? new int[]{start, end} : null;
    }

    /** A decision-table row as the resolution reads it (its {@code splitRow}): the pipes outside a balanced quote are
     *  the cell boundaries, and each cell is lexed on its own, as the resolution parses the rule a row desugars to.
     *  The separator row's dashes and a "n.v.t." cell are one piece each, neither being lexed as rule text. */
    private void tableRow(int start, int end) {
        String row = chain.substring(start, end);
        boolean dq = RuleChecker.even(row, '"'), sq = RuleChecker.even(row, '\'');
        char quote = 0;
        int cellStart = 0;
        for (int i = 0; i <= row.length(); i++) {
            char c = i < row.length() ? row.charAt(i) : '|';
            if (i < row.length() && quote == 0 && ((c == '"' && dq) || (c == '\'' && sq))) { quote = c; continue; }
            if (i < row.length() && quote == c) { quote = 0; continue; }
            if (c != '|' || quote != 0) continue;
            cell(start + cellStart, start + i);
            if (i < row.length()) pieces.add(new Piece(PIPE, false, start + i, start + i + 1, -1));
            cellStart = i + 1;
        }
    }

    private void cell(int start, int end) {
        int[] t = trim(start, end);
        if (t == null) return;
        String text = chain.substring(t[0], t[1]);
        if (text.matches("[:\\-\\s]+") || text.equalsIgnoreCase("n.v.t.")) {
            pieces.add(new Piece(text.matches("[:\\-\\s]+") ? SEPARATOR : NOT_APPLICABLE, false, t[0], t[1], -1));
            return;
        }
        RegelSpraakLexer lexer = new RegelSpraakLexer(CharStreams.fromString(text));
        lexer.removeErrorListeners();
        int[] local = new int[text.codePointCount(0, text.length()) + 1];
        for (int cp = 0, i = 0; ; cp++) {
            local[cp] = i;
            if (i >= text.length()) break;
            i += Character.charCount(text.codePointAt(i));
        }
        for (Token token : lexer.getAllTokens()) {
            int[] p = trim(t[0] + local[token.getStartIndex()], t[0] + local[token.getStopIndex() + 1]);
            if (p != null)
                pieces.add(new Piece(token.getType(), token.getChannel() != Token.DEFAULT_CHANNEL, p[0], p[1], -1));
        }
    }

    /** The piece types of a decision-table row beside the lexer's: a pipe, a cell of the separator row, and the cell
     *  "n.v.t." (not applicable), which the resolution reads as one word. Negative, so no lexer type is one. */
    private static final int PIPE = -10, SEPARATOR = -11, NOT_APPLICABLE = -12;

    /** The lexical kind of a piece type, the kind a piece keeps where no name claims it. */
    private static Kind lexical(int type) {
        return switch (type) {
            case PIPE, SEPARATOR -> Kind.PUNCTUATION;
            case NOT_APPLICABLE -> Kind.KEYWORD;
            case RegelSpraakLexer.LINE_COMMENT -> Kind.COMMENT;
            case RegelSpraakLexer.KOPTEKST -> Kind.HEADING;
            case RegelSpraakLexer.NUMBER -> Kind.NUMBER;
            case RegelSpraakLexer.DATE -> Kind.DATE;
            case RegelSpraakLexer.STRING_START, RegelSpraakLexer.STRING_TEXT, RegelSpraakLexer.STRING_END -> Kind.STRING;
            case RegelSpraakLexer.BENAMING_TEKST -> Kind.DESIGNATION;
            case RegelSpraakLexer.UPPER_ID, RegelSpraakLexer.LOWER_ID -> Kind.WORD;
            case RegelSpraakLexer.ENUM_LITERAL -> Kind.UNRESOLVED;
            case RegelSpraakLexer.DOTDOT, RegelSpraakLexer.BULLET, RegelSpraakLexer.LPAREN,
                 RegelSpraakLexer.RPAREN, RegelSpraakLexer.LBRACE, RegelSpraakLexer.RBRACE, RegelSpraakLexer.COMMA,
                 RegelSpraakLexer.DOT, RegelSpraakLexer.COLON, RegelSpraakLexer.SEMICOLON, RegelSpraakLexer.DASH,
                 RegelSpraakLexer.SLASH, RegelSpraakLexer.EQUALS, RegelSpraakLexer.PERCENT_SIGN, RegelSpraakLexer.CARET,
                 RegelSpraakLexer.EURO, RegelSpraakLexer.APOSTROPHE, RegelSpraakLexer.L_ANGLE_QUOTE,
                 RegelSpraakLexer.R_ANGLE_QUOTE, RegelSpraakLexer.BENAMING_EINDE -> Kind.PUNCTUATION;
            default -> Kind.KEYWORD;
        };
    }

    private static boolean isWord(int type) {
        return type == RegelSpraakLexer.UPPER_ID || type == RegelSpraakLexer.LOWER_ID;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Claims.
    // ---------------------------------------------------------------------------------------------------------------

    /** The pieces of {@code ctx}: from the piece of its start token to the piece of its stop token, or null when it
     *  holds none (an empty match, or a context of ANTLR's recovery without tokens). */
    private int[] range(ParserRuleContext ctx) {
        if (ctx == null || ctx.getStart() == null || ctx.getStop() == null) return null;
        if (ctx.getStop().getTokenIndex() < ctx.getStart().getTokenIndex()) return null;
        int first = -1, last = -1;
        for (int i = ctx.getStart().getTokenIndex(); i <= ctx.getStop().getTokenIndex(); i++) {
            int p = i < pieceOfToken.length ? pieceOfToken[i] : -1;
            if (p < 0) continue;
            if (first < 0) first = p;
            last = p;
        }
        return first < 0 ? null : new int[]{first, last};
    }

    private int[] range(TerminalNode t) {
        if (t == null || t.getSymbol() == null || t.getSymbol().getTokenIndex() < 0) return null;
        int p = pieceOfToken[t.getSymbol().getTokenIndex()];
        return p < 0 ? null : new int[]{p, p};
    }

    /** Claim {@code ctx} as one name. Two claims on one piece are a fault of this pass on a parse without errors, and
     *  fail loudly there; on ANTLR's recovery the first claim stands. */
    private void claim(ParserRuleContext ctx, Kind kind, boolean declaration, Decl target) {
        claim(range(ctx), kind, declaration, target);
    }

    private void claim(int[] range, Kind kind, boolean declaration, Decl target) {
        claim(range, kind, declaration, target, null);
    }

    private void claim(int[] range, Kind kind, boolean declaration, Decl target, Decl generatedFrom) {
        if (range == null) return;
        Name name = new Name(range[0], range[1], kind, declaration, target, generatedFrom);
        for (int i = range[0]; i <= range[1]; i++) {
            if (pieces.get(i).hidden()) continue;
            if (claim[i] != null) {
                if (recovered) return;
                throw new IllegalStateException("semantic tokens: twee namen op '" + pieceText(i) + "' in "
                        + location(pieces.get(i).start()) + " (" + claim[i].kind().wire() + " en " + kind.wire() + ")");
            }
        }
        for (int i = range[0]; i <= range[1]; i++) if (!pieces.get(i).hidden()) claim[i] = name;
        if (declaration && target != null) declarations.putIfAbsent(target, range[0]);
    }

    private boolean free(int first, int last) {
        for (int i = first; i <= last; i++) if (!pieces.get(i).hidden() && claim[i] != null) return false;
        return true;
    }

    private String pieceText(int i) { return chain.substring(pieces.get(i).start(), pieces.get(i).end()); }

    /** The text from piece {@code first} to piece {@code last}, whitespace collapsed: the form the resolution reads a
     *  name in ({@link DefinitionResolver#src}). */
    private String text(int first, int last) {
        return chain.substring(pieces.get(first).start(), pieces.get(last).end()).replaceAll("\\s+", " ").trim();
    }

    private static String norm(String s) { return s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT); }

    // ---------------------------------------------------------------------------------------------------------------
    // Pass 1: the declarations, each with its declared name as the model keys it.
    // ---------------------------------------------------------------------------------------------------------------

    private void declarations(RegelSpraakDocumentContext doc) {
        for (DefinitieContext d : doc.definitie()) {
            if (d.objectTypeDefinition() != null) objectType(d.objectTypeDefinition());
            else if (d.objectTypeExtensie() != null) extension(d.objectTypeExtensie());
            else if (d.domeinDefinition() != null) domain(d.domeinDefinition());
            else if (d.eenheidsysteemDefinition() != null) unitSystem(d.eenheidsysteemDefinition());
            else if (d.parameterDefinition() != null) {
                ParameterDefinitionContext p = d.parameterDefinition();
                claim(p.parameterNaam(), Kind.PARAMETER, true, Decl.of(Kind.PARAMETER, null, src(p.parameterNaam())));
            } else if (d.feitTypeDefinition() != null) factType(d.feitTypeDefinition());
            else if (d.dagsoortDefinition() != null) {
                DagsoortDefinitionContext k = d.dagsoortDefinition();
                Decl decl = Decl.of(Kind.DAY_KIND, null, src(k.lowerNaam()));
                claim(k.lowerNaam(), Kind.DAY_KIND, true, decl);
                claim(k.meervoudsvorm(), Kind.DAY_KIND, false, decl);
            } else if (d.tijdlijnDefinitie() != null) {
                TijdlijnNaamContext n = d.tijdlijnDefinitie().tijdlijnNaam();
                if (n != null) claim(n.vrijeNaam(), Kind.TIMELINE, true,
                        Decl.of(Kind.TIMELINE, null, RegelSpraakModel.normaliseTimelineName(src(n))));
            } else if (d.dimensieDefinition() != null) dimension(d.dimensieDefinition());
            else if (d.vocabulaireDefinition() != null) {
                VocabulaireNaamContext v = d.vocabulaireDefinition().vocabulaireNaam();
                claim(v, Kind.VOCABULARY, true, Decl.of(Kind.VOCABULARY, null, src(v)));
            }
        }
        for (RegelContext r : DefinitionResolver.allRules(doc))
            claim(r.regelNaam(), Kind.NAMED_FORM, true, Decl.of(Kind.NAMED_FORM, null, src(r.regelNaam())));
        for (UitzonderingRegelContext u : DefinitionResolver.allExceptions(doc))
            claim(u.regelNaam(), Kind.NAMED_FORM, true, Decl.of(Kind.NAMED_FORM, null, src(u.regelNaam())));
        for (BeslistabelContext b : DefinitionResolver.allDecisionTables(doc))
            claim(b.beslistabelNaam(), Kind.NAMED_FORM, true, Decl.of(Kind.NAMED_FORM, null, src(b.beslistabelNaam())));
        for (RegelGroepContext g : doc.regelGroep())
            claim(g.kaleNaam(), Kind.RULE_GROUP, true, Decl.of(Kind.RULE_GROUP, null, src(g.kaleNaam())));
    }

    private void objectType(ObjectTypeDefinitionContext c) {
        String name = src(c.objectTypeNaam());
        Decl decl = Decl.of(Kind.OBJECT_TYPE, null, name);
        claim(c.objectTypeNaam(), Kind.OBJECT_TYPE, true, decl);
        claim(c.meervoudsvorm(), Kind.OBJECT_TYPE, false, decl);
        for (ObjectTypeMemberContext m : c.objectTypeMember()) member(name, m);
    }

    private void extension(ObjectTypeExtensieContext c) {
        String name = src(c.objectTypeNaam());
        for (ObjectTypeMemberContext m : c.objectTypeMember()) member(name, m);
    }

    private void member(String owner, ObjectTypeMemberContext m) {
        if (m.attribuutSpecificatie() != null) {
            AttribuutSpecificatieContext a = m.attribuutSpecificatie();
            Decl decl = Decl.of(Kind.ATTRIBUTE, owner, src(a.lowerNaam()));
            claim(a.lowerNaam(), Kind.ATTRIBUTE, true, decl);
            claim(a.meervoudsvorm(), Kind.ATTRIBUTE, false, decl);
        } else if (m.kenmerkSpecificatie() != null) {
            KenmerkSpecificatieContext k = m.kenmerkSpecificatie();
            LowerNaamContext n = k instanceof BijvoeglijkKenmerkContext b ? b.lowerNaam()
                    : k instanceof NaamwoordKenmerkContext w ? w.lowerNaam() : null;
            MeervoudsvormContext plural = k instanceof BijvoeglijkKenmerkContext b ? b.meervoudsvorm() : null;
            if (n == null) return;
            Decl decl = Decl.of(Kind.CHARACTERISTIC, owner, src(n));
            claim(n, Kind.CHARACTERISTIC, true, decl);
            claim(plural, Kind.CHARACTERISTIC, false, decl);
        }
    }

    private void domain(DomeinDefinitionContext c) {
        String name = src(c.domeinNaam());
        claim(c.domeinNaam(), Kind.DOMAIN, true, Decl.of(Kind.DOMAIN, null, name));
        if (c.enumeratieSpecificatie() == null) return;
        for (EnumeratieWaardeContext w : c.enumeratieSpecificatie().enumeratieWaarde())
            claim(range(w.ENUM_LITERAL()), Kind.ENUMERATION_VALUE, true,
                    Decl.of(Kind.ENUMERATION_VALUE, name, unquote(w.ENUM_LITERAL().getText())));
    }

    /** The unit declarations of a system, keyed as the resolution keys them: the first abbreviation, or the word
     *  where there is none. The word is the declaration; its plural and its abbreviations name it. */
    private final Map<String, Decl> unitSpellings = new LinkedHashMap<>();

    private void unitSystem(EenheidsysteemDefinitionContext c) {
        String system = src(c.eenheidsysteemNaam());
        claim(c.eenheidsysteemNaam(), Kind.UNIT_SYSTEM, true, Decl.of(Kind.UNIT_SYSTEM, null, system));
        Map<String, Decl> own = new LinkedHashMap<>();
        for (EenheidDefinitieContext u : c.eenheidDefinitie()) {
            List<String> abbreviations = u.eenheidAfkorting().stream().map(DefinitionResolver::src).toList();
            String primary = !abbreviations.isEmpty() ? abbreviations.get(0)
                    : u.eenheidWoord() != null ? src(u.eenheidWoord()) : null;
            if (primary == null) continue;
            Decl decl = Decl.of(Kind.UNIT, system, primary);
            if (u.eenheidWoord() != null) claim(u.eenheidWoord(), Kind.UNIT, true, decl);
            else claim(u.eenheidAfkorting(0), Kind.UNIT, true, decl);
            claim(u.meervoudsvorm(), Kind.UNIT, false, decl);
            for (int i = u.eenheidWoord() != null ? 0 : 1; i < u.eenheidAfkorting().size(); i++)
                claim(u.eenheidAfkorting(i), Kind.UNIT, false, decl);
            for (String a : abbreviations) { own.putIfAbsent(a, decl); unitSpellings.putIfAbsent(a, decl); }
            if (u.eenheidWoord() != null)
                unitSpellings.putIfAbsent(RegelSpraakModel.normaliseTimeUnit(src(u.eenheidWoord())), decl);
            if (u.meervoudsvorm() != null)
                unitSpellings.putIfAbsent(RegelSpraakModel.normaliseTimeUnit(src(u.meervoudsvorm())), decl);
        }
        for (EenheidDefinitieContext u : c.eenheidDefinitie())
            if (u.omrekenSpecificatie() != null && u.omrekenSpecificatie().eenheidAfkorting() != null) {
                EenheidAfkortingContext a = u.omrekenSpecificatie().eenheidAfkorting();
                claim(a, Kind.UNIT, false, own.get(src(a)));
            }
    }

    private void factType(FeitTypeDefinitionContext c) {
        String name = src(c.feitTypeNaam());
        claim(c.feitTypeNaam(), Kind.FACT_TYPE, true, Decl.of(Kind.FACT_TYPE, null, name));
        List<Role> roles = new ArrayList<>();
        for (RolSpecificatieContext r : c.rolSpecificatie()) {
            String role = src(r.rolNaam());
            Decl decl = Decl.of(Kind.ROLE, name, role);
            claim(r.rolNaam(), Kind.ROLE, true, decl);
            claim(r.meervoudsvorm(), Kind.ROLE, false, decl);
            roles.add(new Role(role, src(r.objectTypeNaam()), r.meervoudsvorm() != null ? src(r.meervoudsvorm()) : null));
        }
        if (c.feitTypeCardinaliteit() == null) return;
        // §13.3.9.1: each part of the cardinality line is a role name and the free relatiebeschrijving after it; the
        // role is the LONGEST leading run that names one, as the resolution reads it (DefinitionResolver.findHead)
        for (CardinaliteitDeelContext part : c.feitTypeCardinaliteit().cardinaliteitDeel()) {
            int[] r = range(part);
            if (r == null) continue;
            int head = -1;
            Role named = null;
            for (int last = r[1]; last >= r[0] && named == null; last--) {
                String written = text(r[0], last);
                for (Role role : roles)
                    if (RegelSpraakModel.roleNameMatches(role, written)) { named = role; head = last; break; }
            }
            if (named != null) claim(new int[]{r[0], head}, Kind.ROLE, false, Decl.of(Kind.ROLE, name, named.name()));
            int from = named != null ? head + 1 : r[0];
            if (from <= r[1]) claim(new int[]{from, r[1]}, named != null ? Kind.RELATION_DESCRIPTION : Kind.UNRESOLVED,
                    false, null);
        }
    }

    private void dimension(DimensieDefinitionContext c) {
        String name = src(c.dimensieNaam());
        Decl decl = Decl.of(Kind.DIMENSION, null, name);
        claim(c.dimensieNaam(), Kind.DIMENSION, true, decl);
        claim(c.dimensieNaamMeervoud(), Kind.DIMENSION, false, decl);
        for (LabelWaardeSpecificatieContext l : c.labelWaardeSpecificatie())
            claim(l.dimensieWaarde(), Kind.DIMENSION_LABEL, true,
                    Decl.of(Kind.DIMENSION_LABEL, name, src(l.dimensieWaarde())));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Pass 2: the references a parse node names by its place — an object type in a head, a domain as a datatype, a
    // unit, a timeline, a vocabulary, a dimension and its labels, an enumeration value, a form.
    // ---------------------------------------------------------------------------------------------------------------

    private void references(ParseTree t) {
        if (t instanceof ObjectTypeSupertypeContext c)
            for (ObjectTypeNaamContext n : c.objectTypeNaam()) objectTypeReference(n);
        else if (t instanceof ObjectTypeExtensieContext c) objectTypeReference(c.objectTypeNaam());
        else if (t instanceof ExtensieAanhefContext c) objectTypeReference(c.objectTypeNaam());
        else if (t instanceof RolSpecificatieContext c) objectTypeReference(c.objectTypeNaam());
        else if (t instanceof AttribuutSpecificatieContext c)
            for (DimensieNaamContext n : c.dimensieNaam())
                claimFree(range(n), Kind.DIMENSION, lookup(Kind.DIMENSION, null, src(n)));
        else if (t instanceof DomeinNaamContext c && !(c.getParent() instanceof DomeinDefinitionContext))
            claimFree(range(c), Kind.DOMAIN, lookup(Kind.DOMAIN, null, src(c)));
        else if (t instanceof EenheidExpressieContext c && !(c.getParent() instanceof EenheidDefinitieContext))
            for (EenheidMachtContext m : c.eenheidMacht()) unitReference(range(m.eenheidAfkorting()));
        else if (t instanceof PercentageDatatypeContext c && c.eenheidAfkorting() != null)
            unitReference(range(c.eenheidAfkorting()));
        else if (t instanceof EenheidMeervoudContext c) unitReference(range(c));
        else if (t instanceof TijdlijnNaamContext c && !(c.getParent() instanceof TijdlijnDefinitieContext))
            claimFree(range(c.vrijeNaam()), Kind.TIMELINE,
                    lookup(Kind.TIMELINE, null, RegelSpraakModel.normaliseTimelineName(src(c))));
        else if (t instanceof BenamingContext c) designation(c);
        else if (t instanceof AggregerenOverAlleDimensiesContext c) dimensionReference(c.dimensieNaamMeervoud());
        else if (t instanceof AggregerenOverVerzamelingContext c) {
            String d = dimensionReference(c.dimensieNaamMeervoud());
            for (DimensieWaardeContext w : c.dimensieWaarde()) labelReference(d, w);
        } else if (t instanceof AggregerenOverBereikContext c) {
            String d = dimensionReference(c.dimensieNaamMeervoud());
            for (DimensieWaardeContext w : c.dimensieWaarde()) labelReference(d, w);
        } else if (t instanceof DimensieSelectieContext c && c.dimensieWaarde() != null)
            for (DimensieWaardeContext w : c.dimensieWaarde()) labelReference(null, w);
        else if (t instanceof ConsistentieVoorwaardeContext c && c.vrijeNaam() != null)
            claimFree(range(c.vrijeNaam()), Kind.NAMED_FORM, formReference(src(c.vrijeNaam())));
        else if (t instanceof UitzonderingBasisNaamContext c)
            claimFree(range(c), Kind.NAMED_FORM, formReference(src(c)));
        else if (t instanceof TerminalNode n && n.getSymbol().getType() == RegelSpraakLexer.ENUM_LITERAL
                && !(n.getParent() instanceof EnumeratieWaardeContext))
            enumerationReference(range(n), n.getText());
        for (int i = 0; i < t.getChildCount(); i++) references(t.getChild(i));
    }

    private void claimFree(int[] range, Kind kind, Decl target) {
        if (range == null || !free(range[0], range[1])) return;
        claim(range, target == null && kind != Kind.UNIT ? Kind.UNRESOLVED : kind, false, target);
    }

    private void objectTypeReference(ObjectTypeNaamContext n) {
        if (n == null) return;
        ObjectType ot = model.objectTypeByName(src(n));
        claimFree(range(n), Kind.OBJECT_TYPE, ot == null ? null : lookup(Kind.OBJECT_TYPE, null, ot.name));
    }

    /** A unit spelling: a unit a system of the files declares (abbreviation first, then its word and plural, as
     *  {@link RegelSpraakModel#resolveUnit} reads them), a day kind written as a unit ("5 werkdagen"), or else a unit
     *  of no declaration (a time unit, or a sign no system declares) without a place. */
    private void unitReference(int[] range) {
        if (range == null || !free(range[0], range[1])) return;
        String written = text(range[0], range[1]);
        Decl decl = unitSpellings.get(written);
        if (decl == null) decl = unitSpellings.get(RegelSpraakModel.normaliseTimeUnit(written));
        if (decl == null && model.dayKindForUnit(written) != null) {
            claim(range, Kind.DAY_KIND, false, lookup(Kind.DAY_KIND, null, model.dayKindForUnit(written)));
            return;
        }
        claim(range, Kind.UNIT, false, decl);
    }

    private void designation(BenamingContext c) {
        TerminalNode start = c.BENAMING_START();
        if (start == null) return;
        String word = start.getText().substring(1, start.getText().length() - 1).trim();
        if (c.getParent() instanceof VocabulaireDefinitionContext) return;   // '(taal:' opens a property, a keyword
        claimFree(range(start), Kind.VOCABULARY, lookup(Kind.VOCABULARY, null, word));
    }

    private String dimensionReference(DimensieNaamMeervoudContext c) {
        if (c == null) return null;
        RegelSpraakModel.Dimension d = model.dimensionByPlural(src(c));
        if (d == null) d = model.dimensionByName(src(c));
        claimFree(range(c), Kind.DIMENSION, d == null ? null : lookup(Kind.DIMENSION, null, d.name()));
        return d == null ? null : d.name();
    }

    private void labelReference(String dimension, DimensieWaardeContext w) {
        String label = src(w);
        Decl decl = null;
        for (RegelSpraakModel.Dimension d : model.dimensions.values()) {
            if (dimension != null && !d.name().equals(dimension)) continue;
            for (String v : d.values()) if (v.equalsIgnoreCase(label)) { decl = lookup(Kind.DIMENSION_LABEL, d.name(), v); break; }
            if (decl != null) break;
        }
        claimFree(range(w), Kind.DIMENSION_LABEL, decl);
    }

    private Decl formReference(String written) {
        Decl decl = lookup(Kind.NAMED_FORM, null, written);
        return decl;
    }

    private void enumerationReference(int[] range, String literal) {
        if (range == null || !free(range[0], range[1])) return;
        String value = unquote(literal);
        List<Decl> found = new ArrayList<>();
        for (Decl d : declarations.keySet())
            if (d.kind() == Kind.ENUMERATION_VALUE && d.name().equals(norm(value))) found.add(d);
        // a value several domains declare is one of them, and only its place in an expression says which: the span
        // keeps the kind and names no declaration, as any name the place does not decide
        claim(range, found.isEmpty() ? Kind.UNRESOLVED : Kind.ENUMERATION_VALUE, false,
                found.size() == 1 ? found.get(0) : null);
    }

    /** The declaration of {@code name} of {@code kind}, null when the files declare none. */
    private Decl lookup(Kind kind, String owner, String name) {
        if (name == null) return null;
        Decl d = Decl.of(kind, owner, name);
        return declarations.containsKey(d) ? d : null;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Pass 3: the text of the rules. Every run of words that the model declares is a name, the longest first; what
    // names nothing is a keyword where the lexer says so, and unresolved where it is a word.
    // ---------------------------------------------------------------------------------------------------------------

    private void ruleTexts(RegelSpraakDocumentContext doc) {
        List<RegelSpraakRegelContext> bodies = new ArrayList<>();
        collect(doc, RegelSpraakRegelContext.class, bodies);
        for (RegelSpraakRegelContext body : bodies) ruleText(body);
        for (BeslistabelContext b : DefinitionResolver.allDecisionTables(doc)) decisionTable(b);
        // what no pass claimed and the lexer reads as a word stands outside every name the parse gave: only ANTLR's
        // recovery leaves one there, and it keeps its lexical kind (grammar/DIVERGENCES.md, "Semantic tokens")
    }

    private static <T> void collect(ParseTree t, Class<T> type, List<T> result) {
        if (type.isInstance(t)) { result.add(type.cast(t)); return; }
        for (int i = 0; i < t.getChildCount(); i++) collect(t.getChild(i), type, result);
    }

    /** The scope a run of words is read in: the rule's subject type and its "Daarbij geldt" variables. */
    private record Scope(ObjectType subject, Map<String, Decl> variables) {}

    private void ruleText(RegelSpraakRegelContext body) {
        int[] r = range(body);
        if (r == null) return;
        Map<String, Decl> variables = new LinkedHashMap<>();
        VariabeleDeelContext vd = body.variabeleDeel();
        if (vd != null) {
            String scope = "@" + body.getStart().getTokenIndex();
            for (VariabeleOnderdeelContext v : vd.variabeleOnderdeel()) {
                if (v.variabeleNaam() == null) continue;
                String key = variableKey(src(v.variabeleNaam()));
                Decl decl = Decl.of(Kind.VARIABLE, scope, key);
                int[] name = range(v.variabeleNaam());
                // an article written inside the name is the variable's lidwoord, not its name (ExpressionBuilder)
                if (name != null && articleType(pieces.get(name[0]).type()) && name[0] < name[1]) name[0]++;
                if (name != null && free(name[0], name[1])) claim(name, Kind.VARIABLE, true, decl);
                variables.put(key, decl);
            }
        }
        words(r[0], r[1], new Scope(subjectOf(body.resultaatDeel()), variables));
    }

    private void decisionTable(BeslistabelContext b) {
        for (TerminalNode row : b.TABEL_RIJ()) {
            int start = codePointOffset[row.getSymbol().getStartIndex()];
            int end = codePointOffset[row.getSymbol().getStopIndex() + 1];
            int first = -1, last = -1;
            for (int i = 0; i < pieces.size(); i++) {
                Piece p = pieces.get(i);
                if (p.streamIndex() >= 0 || p.start() < start || p.end() > end) continue;
                if (first < 0) first = i;
                last = i;
            }
            if (first >= 0) words(first, last, new Scope(null, Map.of()));
        }
    }

    /** One declaration a run of words may name: the declaration (null for a generated member), the declaration a
     *  generated member comes from, the object type the name leads to in a navigation (an object type itself, the
     *  filler of a role; null for the other kinds), and what scopes it: the declarant of a member, the fact type of a
     *  role. */
    private record Candidate(Decl target, Decl generatedFrom, ObjectType leadsTo, ObjectType declarant, FactType factType) {
        static Candidate of(Decl target) { return new Candidate(target, null, null, null, null); }
    }

    /** A run of words that names something: its pieces, the kind it names, and the declarations of that kind it may
     *  name. */
    private record Match(int first, int last, Kind kind, List<Candidate> candidates) {}

    /** Read the words of pieces {@code first}..{@code last}: at each piece the longest run of free pieces that names
     *  a declaration in {@code scope} is that name. A navigation is then read from its end, as the resolution walks it
     *  ({@code ExpressionBuilder.attributeRef}): the type a step leads to decides which of several declarations of one
     *  name the step before it names ("de mast van de post" names the role mast of the fact type that relates a post).
     *  Every free word left is unresolved, except 'geen', which the lexer leaves a word and the language reads as the
     *  negation (grammar/DIVERGENCES.md, "negated membership"). */
    private void words(int first, int last, Scope scope) {
        List<Match> matches = new ArrayList<>();
        for (int i = first; i <= last; i++) {
            Piece p = pieces.get(i);
            if (p.hidden() || claim[i] != null) continue;
            if (p.type() == RegelSpraakLexer.ENUM_LITERAL) { enumerationReference(new int[]{i, i}, pieceText(i)); continue; }
            if (!startsName(p.type())) continue;
            int end = i;
            List<Integer> run = new ArrayList<>();
            for (int j = i; j <= last && run.size() < longest; j++) {
                if (pieces.get(j).hidden()) continue;
                if (claim[j] != null || !inName(pieces.get(j).type())) break;
                run.add(j);
            }
            boolean afterStep = afterLink(first, i), afterSubject = afterSubjectWord(first, i);
            for (int k = run.size() - 1; k >= 0; k--) {
                boolean step = navigationStep(run.get(k), last);
                Match m = match(i, run.get(k), scope, step && afterStep ? MIDDLE_ORDER : step ? STEP_ORDER
                        : afterStep ? HEAD_ORDER : afterSubject ? SUBJECT_ORDER : PLACE_ORDER);
                if (m == null) continue;
                matches.add(m);
                end = run.get(k);
                break;
            }
            i = end;
        }
        Candidate[] chosen = new Candidate[matches.size()];
        for (int m = matches.size() - 1; m >= 0; m--) {
            ObjectType context = m + 1 < matches.size() && linked(matches.get(m), matches.get(m + 1))
                    ? leadsTo(matches.get(m + 1), chosen[m + 1]) : null;
            chosen[m] = choose(matches.get(m), context, scope.subject());
        }
        for (int m = 0; m < matches.size(); m++) {
            Match x = matches.get(m);
            Candidate c = chosen[m];
            claim(new int[]{x.first(), x.last()}, x.kind(), false, c == null ? null : c.target(),
                    c == null ? null : c.generatedFrom());
        }
        for (int i = first; i <= last; i++) {
            Piece p = pieces.get(i);
            if (p.hidden() || claim[i] != null || !isWord(p.type())) continue;
            claim(new int[]{i, i}, pieceText(i).equals("geen") ? Kind.KEYWORD : Kind.UNRESOLVED, false, null);
        }
    }

    /** Whether {@code b} is the next step of the navigation {@code a} begins: only 'van' and an article or a quantifier
     *  stand between them ("de mast VAN DE post", "het gewicht VAN ALLE vondsten"). */
    private boolean linked(Match a, Match b) {
        boolean van = false;
        for (int i = a.last() + 1; i < b.first(); i++) {
            Piece p = pieces.get(i);
            if (p.hidden()) continue;
            if (!van && p.type() == RegelSpraakLexer.VAN) { van = true; continue; }
            if (!van || !LINK_WORDS.contains(p.type())) return false;
        }
        return van;
    }

    private static final java.util.Set<Integer> LINK_WORDS = java.util.Set.of(RegelSpraakLexer.DE, RegelSpraakLexer.HET,
            RegelSpraakLexer.EEN, RegelSpraakLexer.ALLE, RegelSpraakLexer.AL, RegelSpraakLexer.ZIJN,
            RegelSpraakLexer.GEEN_VAN_DE, RegelSpraakLexer.TEN_MINSTE, RegelSpraakLexer.TEN_HOOGSTE,
            RegelSpraakLexer.PRECIES, RegelSpraakLexer.NUMBER, RegelSpraakLexer.EEN_TELWOORD,
            RegelSpraakLexer.TWEE_TELWOORD, RegelSpraakLexer.DRIE_TELWOORD, RegelSpraakLexer.VIER_TELWOORD,
            RegelSpraakLexer.VAN);

    /** The object type match {@code m} leads to: that of its chosen declaration, or the one all its declarations share. */
    private static ObjectType leadsTo(Match m, Candidate chosen) {
        if (chosen != null) return chosen.leadsTo();
        ObjectType shared = null;
        for (Candidate c : m.candidates()) {
            if (c.leadsTo() == null || (shared != null && shared != c.leadsTo())) return null;
            shared = c.leadsTo();
        }
        return shared;
    }

    /** The declaration match {@code m} names: its only one; else the one {@code context}, the type the next step of a
     *  navigation leads to, relates to; else the one the rule's subject relates to; else none (an open name, which the
     *  span then carries without a declaration). */
    private Candidate choose(Match m, ObjectType context, ObjectType subject) {
        List<Candidate> all = m.candidates();
        if (all.size() == 1) return all.get(0);
        for (ObjectType t : new ObjectType[]{context, subject}) {
            if (t == null) continue;
            List<Candidate> related = all.stream().filter(c -> related(c, t)).toList();
            if (related.size() == 1) return related.get(0);
        }
        return null;
    }

    /** Whether candidate {@code c} is reached from {@code t}: a member that {@code t} has or a subtype of it declares,
     *  a role of a fact type in which {@code t} takes part. */
    private boolean related(Candidate c, ObjectType t) {
        if (c.declarant() != null) return t.isA(c.declarant()) || c.declarant().isA(t);
        if (c.factType() != null)
            for (Role r : c.factType().roles) {
                ObjectType filler = model.objectTypeByName(r.objectTypeName());
                if (filler != null && (t.isA(filler) || filler.isA(t))) return true;
            }
        return false;
    }

    /** The piece types a name may begin with: a word, a number, and the keywords a name word may be (an article
     *  included, which a characteristic may carry as its own: "is de minimis"). */
    private static boolean startsName(int type) {
        return inName(type) && type != RegelSpraakLexer.VAN;
    }

    /** The piece types a name may hold: words, numbers and every single-word keyword the name rules admit. */
    private static boolean inName(int type) {
        return switch (lexical(type)) {
            case WORD, NUMBER -> true;
            case KEYWORD -> true;
            case PUNCTUATION -> type == RegelSpraakLexer.PERCENT_SIGN;
            default -> false;
        };
    }

    private static boolean articleType(int type) {
        return type == RegelSpraakLexer.DE || type == RegelSpraakLexer.HET || type == RegelSpraakLexer.EEN;
    }

    /** Whether the name ending at piece {@code end} is a step of a navigation: the next piece is 'van' and a word follows
     *  it ("de vondsten VAN de Jutter"). */
    private boolean navigationStep(int end, int last) {
        int next = end + 1;
        while (next <= last && pieces.get(next).hidden()) next++;
        return next < last && pieces.get(next).type() == RegelSpraakLexer.VAN;
    }

    /** Whether piece {@code at} follows a navigation link: 'van' and at most an article or a quantifier stand before it,
     *  so it is the onderwerp a step navigates from ("de leeftijd van DE JUTTER"). */
    private boolean afterLink(int from, int at) {
        int i = at - 1;
        while (i >= from && (pieces.get(i).hidden() || (LINK_WORDS.contains(pieces.get(i).type())
                && pieces.get(i).type() != RegelSpraakLexer.VAN))) i--;
        return i >= from && pieces.get(i).type() == RegelSpraakLexer.VAN;
    }

    /** Whether piece {@code at} follows an indefinite article or a quantifier directly ("EEN Jutter", "ALLE
     *  vondsten", "hij is EEN aanmelder"): the place of a subject or of the name a membership test writes. */
    private boolean afterSubjectWord(int from, int at) {
        int i = at - 1;
        while (i >= from && pieces.get(i).hidden()) i--;
        return i >= from && SUBJECT_WORDS.contains(pieces.get(i).type());
    }

    private static final java.util.Set<Integer> SUBJECT_WORDS = java.util.Set.of(RegelSpraakLexer.EEN,
            RegelSpraakLexer.ALLE, RegelSpraakLexer.AL, RegelSpraakLexer.GEEN_VAN_DE, RegelSpraakLexer.EEN_TELWOORD,
            RegelSpraakLexer.TWEE_TELWOORD, RegelSpraakLexer.DRIE_TELWOORD, RegelSpraakLexer.VIER_TELWOORD);

    /** The order in which the kinds a run of words can name are tried, by its place, after the resolution's reading of
     *  each place ({@code ExpressionBuilder.buildAttribute}, {@code attributeRef}, {@code resolveSubject}). The first
     *  step of a navigation ("DE LEEFTIJD van de Jutter") names a member before anything else; a step inside one
     *  ("van DE MAST van de post") names a role; the onderwerp a navigation ends in ("van DE JUTTER") and a subject
     *  ("EEN Jutter", "alle vondsten") name a type before a role, and never a member first; elsewhere a variable of the
     *  rule shadows a parameter and both go before the members and types of the model. */
    private static final List<Kind> STEP_ORDER = List.of(Kind.ATTRIBUTE, Kind.CHARACTERISTIC, Kind.ROLE, Kind.PARAMETER,
            Kind.VARIABLE, Kind.OBJECT_TYPE, Kind.DIMENSION_LABEL, Kind.DAY_KIND, Kind.DIMENSION, Kind.TIMELINE, Kind.UNIT);
    private static final List<Kind> MIDDLE_ORDER = List.of(Kind.ROLE, Kind.OBJECT_TYPE, Kind.ATTRIBUTE,
            Kind.CHARACTERISTIC, Kind.VARIABLE, Kind.PARAMETER, Kind.DIMENSION_LABEL, Kind.DAY_KIND, Kind.DIMENSION,
            Kind.TIMELINE, Kind.UNIT);
    private static final List<Kind> HEAD_ORDER = List.of(Kind.OBJECT_TYPE, Kind.ROLE, Kind.VARIABLE, Kind.PARAMETER,
            Kind.ATTRIBUTE, Kind.CHARACTERISTIC, Kind.DIMENSION_LABEL, Kind.DAY_KIND, Kind.DIMENSION, Kind.TIMELINE,
            Kind.UNIT);
    private static final List<Kind> SUBJECT_ORDER = List.of(Kind.OBJECT_TYPE, Kind.ROLE, Kind.CHARACTERISTIC,
            Kind.DAY_KIND, Kind.VARIABLE, Kind.PARAMETER, Kind.ATTRIBUTE, Kind.DIMENSION_LABEL, Kind.DIMENSION,
            Kind.TIMELINE, Kind.UNIT);
    private static final List<Kind> PLACE_ORDER = List.of(Kind.VARIABLE, Kind.PARAMETER, Kind.ATTRIBUTE,
            Kind.CHARACTERISTIC, Kind.OBJECT_TYPE, Kind.ROLE, Kind.DAY_KIND, Kind.DIMENSION_LABEL, Kind.DIMENSION,
            Kind.TIMELINE, Kind.UNIT);

    /** What pieces {@code first}..{@code last} name in {@code scope}, or null: the kinds their text can name, in the
     *  order above, with every declaration of the first kind that has one. */
    private Match match(int first, int last, Scope scope, List<Kind> order) {
        String written = text(first, last);
        boolean article = written.matches("(?i)^(de|het|een)\\s.*");
        Map<Kind, List<Candidate>> found = new java.util.EnumMap<>(Kind.class);
        Decl v = scope.variables().get(variableKey(written));
        if (v != null && !article) found.put(Kind.VARIABLE, List.of(Candidate.of(v)));
        if (model.parameters.containsKey(written))
            found.put(Kind.PARAMETER, List.of(Candidate.of(lookup(Kind.PARAMETER, null, written))));
        for (Kind member : List.of(Kind.ATTRIBUTE, Kind.CHARACTERISTIC)) {
            List<Candidate> c = members(written, member);
            if (!c.isEmpty()) found.put(member, c);
        }
        ObjectType ot = model.objectTypeByName(written);
        if (ot == null) ot = PluralMatcher.findObjectType(model.objectTypes.values(), written);
        if (ot != null)
            found.put(Kind.OBJECT_TYPE, List.of(new Candidate(lookup(Kind.OBJECT_TYPE, null, ot.name), null, ot, null, null)));
        List<Candidate> roles = roles(written);
        if (!roles.isEmpty()) found.put(Kind.ROLE, roles);
        for (String d : model.dayKinds)
            if (d.equalsIgnoreCase(written) || written.equalsIgnoreCase(dayKindPlural(d)))
                found.putIfAbsent(Kind.DAY_KIND, List.of(Candidate.of(lookup(Kind.DAY_KIND, null, d))));
        for (RegelSpraakModel.Dimension d : model.dimensions.values()) {
            for (String label : d.values())
                if (label.equalsIgnoreCase(written))
                    found.putIfAbsent(Kind.DIMENSION_LABEL, List.of(Candidate.of(lookup(Kind.DIMENSION_LABEL, d.name(), label))));
            if (d.name().equalsIgnoreCase(written) || written.equalsIgnoreCase(d.pluralName()))
                found.putIfAbsent(Kind.DIMENSION, List.of(Candidate.of(lookup(Kind.DIMENSION, null, d.name()))));
        }
        if (model.timelineByName(written) != null && !article)
            found.put(Kind.TIMELINE, List.of(Candidate.of(lookup(Kind.TIMELINE, null, RegelSpraakModel.normaliseTimelineName(written)))));
        // a unit a system of the files declares, written in words where no unit expression stands: the appositive of
        // an attribute read ("de afstand in kilometers", ExpressionBuilder.attributeRef). A time unit the lexer reads
        // as a keyword ("de maand uit") is a unit only where the parse puts a unit expression (pass 2).
        boolean words = true;
        for (int i = first; i <= last; i++) if (!pieces.get(i).hidden() && !isWord(pieces.get(i).type())) words = false;
        Decl unit = words ? unitSpellings.get(written) : null;
        if (words && unit == null) unit = unitSpellings.get(RegelSpraakModel.normaliseTimeUnit(written));
        if (unit != null) found.put(Kind.UNIT, List.of(Candidate.of(unit)));
        for (Kind kind : order)
            if (found.containsKey(kind)) return new Match(first, last, kind, found.get(kind));
        return null;
    }

    private String dayKindPlural(String dayKind) {
        for (var e : model.dayKindPerUnit.entrySet())
            if (e.getValue().equalsIgnoreCase(dayKind) && !e.getKey().equalsIgnoreCase(dayKind)) return e.getKey();
        return null;
    }

    /** Every attribute or characteristic ({@code kind}) named {@code written}, by its declarant, as the resolution
     *  matches a member on a type ({@link PluralMatcher#findAttribute}, {@link CharacteristicMatcher#find}). */
    private List<Candidate> members(String written, Kind kind) {
        List<Candidate> result = new ArrayList<>();
        for (ObjectType ot : model.objectTypes.values()) {
            String own = kind == Kind.ATTRIBUTE ? PluralMatcher.findAttribute(ot, written) : CharacteristicMatcher.find(ot, written);
            if (own == null) continue;
            ObjectType declarant = ot.declarantOf(own);
            Candidate c = memberCandidate(kind, declarant == null ? ot : declarant, own);
            if (!result.contains(c)) result.add(c);
        }
        return result;
    }

    /** The member {@code name} of {@code declarant}: its declaration, or for a member the frontend generates the
     *  declaration it comes from ({@link RegelSpraakModel#generated}). */
    private Candidate memberCandidate(Kind kind, ObjectType declarant, String name) {
        Decl d = lookup(kind, declarant.name, name);
        if (d != null) return new Candidate(d, null, null, declarant, null);
        RegelSpraakModel.Designatable element = kind == Kind.ATTRIBUTE
                ? RegelSpraakModel.Designatable.attribute(declarant.name, name)
                : RegelSpraakModel.Designatable.characteristic(declarant.name, name);
        RegelSpraakModel.GeneratedFrom from = model.generated.get(element);
        if (from == null || from.source() == null) return new Candidate(null, null, null, declarant, null);
        RegelSpraakModel.Designatable source = from.source();
        Kind sourceKind = switch (source.kind()) {
            case ATTRIBUUT -> Kind.ATTRIBUTE;
            case KENMERK -> Kind.CHARACTERISTIC;
            case VORM -> Kind.NAMED_FORM;
            default -> null;
        };
        return new Candidate(null, sourceKind == null ? null : lookup(sourceKind, source.owner(), source.name()), null,
                declarant, null);
    }

    /** Every role named {@code written}: by its name or its declared plural, and only where no role is named so, by
     *  the plural the resolution guesses — a guessed plural never defeats a declared form
     *  ({@code RegelSpraakModel.objectTypeViaRole}). */
    private List<Candidate> roles(String written) {
        List<Candidate> declared = new ArrayList<>(), guessed = new ArrayList<>();
        for (FactType ft : model.factTypes.values())
            for (Role r : ft.roles) {
                if (!RegelSpraakModel.roleNameMatches(r, written)) continue;
                boolean exact = r.name().equalsIgnoreCase(written) || (r.plural() != null && r.plural().equalsIgnoreCase(written));
                Candidate c = new Candidate(lookup(Kind.ROLE, ft.name, r.name()), null,
                        model.objectTypeByName(r.objectTypeName()), null, ft);
                List<Candidate> into = exact ? declared : guessed;
                if (!into.contains(c)) into.add(c);
            }
        return declared.isEmpty() ? guessed : declared;
    }

    /** The object type a rule is about, read with the resolution's own builder as its rule check reads it
     *  ({@code RuleChecker.ruleSubjectOf}); null where it reads none. */
    private ObjectType subjectOf(ResultaatDeelContext rd) {
        if (rd == null) return null;
        try {
            if (rd.gelijkstelling() != null) return holder(builder.buildAttribute(rd.gelijkstelling().attribuutVanOnderwerp()));
            if (rd.initialisatie() != null) return holder(builder.buildAttribute(rd.initialisatie().attribuutVanOnderwerp()));
            if (rd.kenmerkToekenning() != null) return builder.chainObjectType(rd.kenmerkToekenning().onderwerpKeten());
            if (rd.objectCreatie() != null) return builder.chainObjectType(rd.objectCreatie().onderwerpKeten());
            if (rd.feitCreatie() != null) return builder.resolveSubject(rd.feitCreatie().onderwerp(0));
            if (rd.startpuntBepaling() != null && rd.startpuntBepaling().universeelOnderwerp() != null)
                return model.resolveObjectType(src(rd.startpuntBepaling().universeelOnderwerp().kaleNaam()));
            if (rd.consistentieRegel() != null && rd.consistentieRegel().samengesteldeConsistentie() != null)
                return builder.resolveSubject(rd.consistentieRegel().samengesteldeConsistentie().onderwerp());
            if (rd.verdeling() != null && !rd.verdeling().attribuutVanOnderwerp().isEmpty())
                return holder(builder.buildAttribute(rd.verdeling().attribuutVanOnderwerp(0)));
        } catch (RuntimeException e) {
            // the builder reads a recovered tree only partly; the subject then stays open, which only widens a lookup
            if (!recovered) throw e;
        }
        return null;
    }

    private static ObjectType holder(dev.uchar.regelspraak.ir.Expr e) {
        return e instanceof dev.uchar.regelspraak.ir.Expr.AttributeReference a ? a.objectType() : null;
    }

    private static String variableKey(String name) {
        return name.replaceFirst("^(?i)(de|het|een)\\s+", "").trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static String unquote(String s) {
        return s.length() >= 2 ? s.substring(1, s.length() - 1) : s;
    }

    private static String src(ParserRuleContext ctx) { return DefinitionResolver.src(ctx); }

    // ---------------------------------------------------------------------------------------------------------------
    // The output.
    // ---------------------------------------------------------------------------------------------------------------

    private int fileOf(int offset) {
        int f = 0;
        while (f + 1 < files.size() && fileStart[f + 1] <= offset) f++;
        return f;
    }

    private int chainLine(int offset) {
        int lo = 0, hi = lineStart.length - 1;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (lineStart[mid] <= offset) lo = mid; else hi = mid - 1;
        }
        return lo + 1;
    }

    private Location location(int offset) {
        int f = fileOf(offset), line = chainLine(offset);
        return new Location(files.get(f).name(), line - firstLine[f] + 1, offset - lineStart[line - 1]);
    }

    private Location declarationLocation(Decl d) {
        Integer at = declarations.get(d);
        if (at == null) throw new IllegalStateException("semantic tokens: geen declaratie van " + d);
        return location(pieces.get(at).start());
    }

    private Document document(RegelSpraakModel model) {
        List<List<Span>> perFile = new ArrayList<>();
        for (int f = 0; f < files.size(); f++) perFile.add(new ArrayList<>());
        int i = 0;
        while (i < pieces.size()) {
            Piece p = pieces.get(i);
            Name n = p.hidden() ? null : claim[i];
            int j = i;
            if (n != null) {
                while (j + 1 < pieces.size() && !pieces.get(j + 1).hidden() && claim[j + 1] == n) j++;
            } else if (lexical(p.type()) == Kind.STRING) {
                while (j + 1 < pieces.size() && lexical(pieces.get(j + 1).type()) == Kind.STRING && claim[j + 1] == null) j++;
            }
            Kind kind = n != null ? n.kind() : lexical(p.type());
            Location target = null, generatedFrom = null;
            boolean declaration = false;
            if (n != null && n.declaration()) declaration = true;
            else if (n != null && n.target() != null) target = declarationLocation(n.target());
            if (n != null && n.generatedFrom() != null) generatedFrom = declarationLocation(n.generatedFrom());
            // a span stands in one file: a run over the end of a file (a name that a file ends in the middle of)
            // is split there, each part keeping the kind
            int start = p.start(), end = pieces.get(j).end();
            for (int f = fileOf(start); f < files.size() && start < end; f++) {
                int fileEnd = fileStart[f] + files.get(f).text().length();
                int[] part = trim(Math.max(start, fileStart[f]), Math.min(end, fileEnd));
                if (part != null) {
                    Location from = location(part[0]), to = location(part[1]);
                    perFile.get(f).add(new Span(from.line(), from.column(), to.line(), to.column(), kind,
                            chain.substring(part[0], part[1]), declaration, target, generatedFrom));
                }
                if (end <= fileStart[f + 1]) break;
            }
            i = j + 1;
        }
        List<FileSpans> result = new ArrayList<>();
        for (int f = 0; f < files.size(); f++) result.add(new FileSpans(files.get(f).name(), List.copyOf(perFile.get(f))));
        List<FileDiagnostic> diagnostics = new ArrayList<>();
        List<Diagnostic> all = new ArrayList<>(model.diagnostics);
        all.addAll(dev.uchar.regelspraak.emit.IrEmitter.unwrittenAllocations(model));
        for (Diagnostic d : all) {
            if (d.line() <= 0 || d.line() > lineStart.length) {
                diagnostics.add(new FileDiagnostic(d.severity(), d.code(), d.params(), d.message(), null, 0));
                continue;
            }
            int f = fileOf(lineStart[d.line() - 1]);
            diagnostics.add(new FileDiagnostic(d.severity(), d.code(), d.params(), d.message(), files.get(f).name(),
                    d.line() - firstLine[f] + 1));
        }
        return new Document(List.copyOf(result), List.copyOf(diagnostics));
    }
}
