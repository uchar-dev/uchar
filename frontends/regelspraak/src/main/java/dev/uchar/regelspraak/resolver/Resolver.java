package dev.uchar.regelspraak.resolver;

import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.parser.RegelSpraak;
import dev.uchar.regelspraak.parser.RegelSpraak.RegelSpraakDocumentContext;
import dev.uchar.regelspraak.parser.RegelSpraakLexer;
import org.antlr.v4.runtime.*;
import static dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic.params;

/**
 * Front-end entry point: parse a RegelSpraak source, build the typed model
 * (§13.3), then type-check the rules against the typeringen matrix (§13.4 / the
 * typeringen specification). Syntax errors and type errors land in
 * {@link RegelSpraakModel#diagnostics}.
 */
public final class Resolver {

    public static RegelSpraakModel resolve(String source) {
        // IntStream.UNKNOWN_SOURCE_NAME ("<unknown>") is exactly what CharStreams.fromString(source)
        // already assigns, so this delegation is behaviour-preserving for the nameless callers.
        return resolve(source, IntStream.UNKNOWN_SOURCE_NAME);
    }

    /** As {@link #resolve(String)}, but with the source file name threaded onto the char stream so every
     *  {@link dev.uchar.regelspraak.ir.Source} anchors back to {@code fileName} (uitleg can then
     *  cite the file/regel); otherwise identical. */
    public static RegelSpraakModel resolve(String source, String fileName) {
        RegelSpraakLexer lexer = new RegelSpraakLexer(CharStreams.fromString(source, fileName));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        RegelSpraak parser = new RegelSpraak(tokens);

        var syntax = new java.util.ArrayList<Diagnostic>();
        BaseErrorListener listener = new BaseErrorListener() {
            @Override public void syntaxError(Recognizer<?, ?> r, Object sym, int line, int col,
                                              String msg, RecognitionException e) {
                syntax.add(new Diagnostic(Severity.ERROR, "RGS-091", params("message", msg), line));
            }
        };
        lexer.removeErrorListeners(); lexer.addErrorListener(listener);
        parser.removeErrorListeners(); parser.addErrorListener(listener);

        RegelSpraakDocumentContext doc = parser.regelSpraakDocument();
        return build(doc, tokens, syntax);
    }

    /** The model of a parsed document: definitions, the syntax diagnostics the parse raised, the rule
     *  check and the marker sweep. One pipeline for both routes: the scenario route parses a test document
     *  and hands its non-scenario blocks here as a document of their own ({@link ScenarioResolver}), so
     *  the IR it emits is the one this route emits for the same blocks. */
    static RegelSpraakModel build(RegelSpraakDocumentContext doc, CommonTokenStream tokens,
                                 java.util.List<Diagnostic> syntax) {
        DefinitionResolver definitions = new DefinitionResolver();
        RegelSpraakModel model = definitions.build(doc);
        model.diagnostics.addAll(syntax);
        if (!syntax.isEmpty()) headFormHints(tokens, model);

        // the vocabulary side table (grammar/DIVERGENCES.md, "Vocabulaires en benamingen"): read after the
        // declarations; composed for the generated elements once the rule check has recorded where they come from,
        // and judged with the composed designations once it has written every form
        Designations designations = new Designations(model, definitions.repetitions().keySet());
        designations.parse(doc);
        RuleChecker checker = new RuleChecker(model, tokens, definitions.repetitions());
        checker.check(doc);
        designations.compose(checker.nameParts());
        designations.check();
        reportUnresolved(model);
        return model;
    }

    /**
     * The marker discipline (maintainer decision 2026-09-19): every {@link Condition.Unresolved} /
     * {@link Expr.Unresolved} that SURVIVES into the finished model gets a WARNING beside it. A marker is
     * the honest wire form and stays one — a WARNING leaves the document on disk — but the author is told
     * what stayed unresolved and why, instead of finding a marker in the IR with nothing said about it.
     *
     * <p>Why here and not at each construction. A marker is not final where it is built: {@link
     * RuleChecker} markers a condition its own caller then re-reads as a quantifier fold, a feitcreatie
     * guard is re-resolved against the doel, and the §13.4.17.53 re-split rebuilds a markered aggregation
     * operand in place. Diagnosing at the construction would report each of those as unresolved and be
     * wrong about it. So the reason travels ON the node and the report is raised over what is still there
     * when the model is finished — which is also exactly what the emitter writes.
     *
     * <p>A {@code reason} of null means the producing route already raises its own diagnostic (an ERROR
     * that blocks the document, or a WARNING with a more specific text); repeating it here would be
     * noise, so the sweep stays quiet. Such a WARNING must quote the marker's {@code sourceText} verbatim, as the
     * sweep's own text does: that quotation is how a written document accounts for its marker (WF-113 (b)). The roots are the model's IR-carrying collections, i.e. what
     * {@code IrEmitter} serialises: a marker unreachable from those never reaches a consumer either.
     */
    private static void reportUnresolved(RegelSpraakModel m) {
        java.util.function.BiConsumer<dev.uchar.regelspraak.ir.Source, Object> report = (sourceText, reason) -> {
            if (reason == null) return;
            m.diagnostics.add(new Diagnostic(Severity.WARNING, "RGS-092", params("reason", reason, "expression", sourceText),
                    sourceText == null ? 0 : sourceText.line()));
        };
        for (var r : m.resolvedRules) {
            Expr.collectUnresolved(r.target(), report);
            Expr.collectUnresolved(r.value(), report);
            Condition.collectUnresolved(r.condition(), report);
        }
        for (var d : m.resolvedDayKindDefinitions) Condition.collectUnresolved(d.condition(), report);
        for (var oc : m.objectCreations) {
            for (var t : oc.initialisations()) Expr.collectUnresolved(t.value(), report);
            Condition.collectUnresolved(oc.condition(), report);
        }
        for (var v : m.allocations)  Condition.collectUnresolved(v.condition(), report);
        for (var c : m.consistencies) Condition.collectUnresolved(c.assertion(), report);
        for (var fc : m.factCreations) Condition.collectUnresolved(fc.condition(), report);
        java.util.Set<Condition> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (var pd : m.prioritizedDefaults) reportDefaults(pd.defaults(), report, seen);
    }

    /** The §7 defaults tree: each default's guard and consequence, and recursively the defaults of its
     *  exception groups — a marker nested in an exception is as much on the wire as a top-level one. A
     *  restrictive exception repeats its carrier's conjuncts, the very same nodes (grammar/DIVERGENCES.md §4,
     *  form 2), so a guard is walked with {@link Condition#eachConjunctOnce} and each node reported once: one
     *  marker in a base's condition is one WARNING, not one per exception above it. */
    private static void reportDefaults(java.util.List<dev.uchar.regelspraak.ir.Default> defaults,
                                     java.util.function.BiConsumer<dev.uchar.regelspraak.ir.Source, Object> report,
                                     java.util.Set<Condition> seen) {
        if (defaults == null) return;
        for (var d : defaults) {
            Condition.eachConjunctOnce(d.condition(), seen, c -> Condition.collectUnresolved(c, report));
            Expr.collectUnresolved(d.consequence(), report);
            if (d.exceptions() != null)
                for (var group : d.exceptions().members()) reportDefaults(group.defaults(), report, seen);
        }
    }

    /** Two mis-spellings of the uchar 'is een' head form that ANTLR can only report as "no viable
     *  alternative", which sends the author looking at the wrong thing. Raised only alongside a real syntax
     *  error, over the token stream rather than the (failed) parse tree, and additively: the raw message
     *  stays, this names the fix.
     *
     *  <p>(a) The article dropped — {@code Objecttype het Wrakrecht is Strandrecht}. An IS after the
     *  object-type name is otherwise a §13.3.2 kenmerk declaration, but that one continues with a
     *  {@code lowerNaam}, so an IS followed by a CAPITALISED name can only be the head form missing its
     *  {@code een}.
     *
     *  <p>(b) The clause behind the mv-group — {@code Objecttype het Wrakrecht (mv: …) is een Strandrecht}.
     *  The mutual order of the head annotations is fixed by the grammar and always has been; the extension
     *  inserts one more element and the author has to know where. */
    private static void headFormHints(CommonTokenStream tokens, RegelSpraakModel model) {
        var list = tokens.getTokens().stream().filter(t -> t.getChannel() == Token.DEFAULT_CHANNEL).toList();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getType() != RegelSpraakLexer.OBJECTTYPE) continue;
            boolean afterPlural = false;
            // Walk the HEAD only: the first token no head can contain ends it. Without that bound the
            // scan would run on into rule text, where "is een" is ordinary Dutch and no mistake at all.
            for (int j = i + 1; j < list.size() && headToken(list.get(j).getType()); j++) {
                Token t = list.get(j);
                if (t.getType() == RegelSpraakLexer.MV_START) { afterPlural = true; continue; }
                if (t.getType() != RegelSpraakLexer.IS) continue;
                Token after = j + 1 < list.size() ? list.get(j + 1) : null;
                if (after == null) break;
                if (after.getType() == RegelSpraakLexer.UPPER_ID)
                    model.diagnostics.add(new Diagnostic(Severity.ERROR, "RGS-093", params("word", after.getText()), t.getLine()));
                else if (after.getType() == RegelSpraakLexer.EEN && afterPlural)
                    model.diagnostics.add(new Diagnostic(Severity.ERROR, "RGS-094", params(), t.getLine()));
                break;                                  // the head carries at most one such clause
            }
        }
    }

    /** The token types an object-type HEAD can consist of: its articles and name words, the mv-group, the
     *  animacy marker, and the extension's own two tokens. Anything else — a datatype, KENMERK, a new
     *  construct keyword — is already a member or the next definition. */
    private static boolean headToken(int type) {
        return type == RegelSpraakLexer.DE || type == RegelSpraakLexer.HET
                || type == RegelSpraakLexer.UPPER_ID || type == RegelSpraakLexer.LOWER_ID
                || type == RegelSpraakLexer.MV_START || type == RegelSpraakLexer.RPAREN
                || type == RegelSpraakLexer.BEZIELD
                || type == RegelSpraakLexer.IS || type == RegelSpraakLexer.EEN
                || type == RegelSpraakLexer.EN;
    }

    /** The --ir mode body: {@code rest} = [out.json, file.rgs...] after flag removal — the FIRST remaining arg is
     *  the output file, the rest are the source files of the model chain (concatenated in order). Writes a
     *  FAITHFUL {@code uchar-ir-v1} JSON dump of the complete resolved model (the uchar "mirror" artifact, in the
     *  names of IR-SPEC since the L5 landing, warts included) via
     *  {@link dev.uchar.regelspraak.emit.IrEmitter}.
     *  Returns the process exit code (0 = ir geschreven, 1 = resolver-fouten, 2 = usage) instead of exiting, so the
     *  disk discipline is testable as the REAL CLI path: an unresolvable model writes NOTHING and exits non-zero. */
    public static int irMode(java.util.List<String> rest) throws java.io.IOException {
        if (rest.size() < 2) { System.err.println("usage: Resolver --ir <out.json> <file.rgs...>"); return 2; }
        java.nio.file.Path out = java.nio.file.Path.of(rest.get(0));
        StringBuilder sourceText = new StringBuilder();
        for (String f : rest.subList(1, rest.size()))
            sourceText.append(java.nio.file.Files.readString(java.nio.file.Path.of(f))).append('\n');
        // a single source file threads its name into the Bron coordinates; a
        // concatenated chain deliberately does not (per-file line offsets
        // would lie about every file after the first)
        RegelSpraakModel m = rest.size() == 2
                ? resolve(sourceText.toString(), rest.get(1))
                : resolve(sourceText.toString());
        // Check errors BEFORE touching disk: an unresolvable model writes NOTHING (a mirror of a broken model
        // would carry half-bound references and misleading diagnostics as if authoritative).
        if (!m.isOk()) {
            m.diagnostics.forEach(d -> System.out.println("  " + d));
            return 1;
        }
        // A WARNING does NOT block the emission — that is the whole difference with an ERROR — but it must
        // be SEEN: until 2026-09-19 a successful run printed nothing, so every marker-with-a-warning was
        // invisible on the one path an author actually uses. stderr, so the stdout summary stays parseable.
        m.diagnostics.stream().filter(d -> !d.isError()).forEach(d -> System.err.println("  " + d));
        // the WARNINGs the emitter itself appends, for what the wire cannot carry, are shown the same way
        dev.uchar.regelspraak.emit.IrEmitter.unwrittenAllocations(m)
                .forEach(d -> System.err.println("  " + d));
        java.nio.file.Files.writeString(out, dev.uchar.regelspraak.emit.IrEmitter.emit(m),
                java.nio.charset.StandardCharsets.UTF_8);
        System.out.printf("ir: objecttypen=%d feittypen=%d resolvedRegels=%d geschreven naar %s%n",
                m.objectTypes.size(), m.factTypes.size(), m.resolvedRules.size(), out);
        return 0;
    }

    /** The --cases mode body, beside --ir: {@code rest} = [out.json, file.rgs...] as for {@link #irMode}, the sources
     *  read as ONE test document (scenariotoetsen, grammar/DIVERGENCES.md). It writes the IR document the scenarios
     *  run against — the whole model, or with a scope the forms the scope carries — to {@code out.json}, and each
     *  scenario as one case file {@code case<n>-<slug>.json} into {@code map}. Exit codes as {@link #irMode}: a model
     *  that carries an ERROR, the scenarios' own included, writes NOTHING. */
    public static int casesMode(java.util.List<String> rest, java.nio.file.Path map) throws java.io.IOException {
        String use = "usage: Resolver --ir <out.json> --cases <map> <file.rgs...>";
        if (rest.size() < 2) { System.err.println(use); return 2; }
        java.nio.file.Path out = java.nio.file.Path.of(rest.get(0));
        StringBuilder sourceText = new StringBuilder();
        java.util.List<String> files = rest.subList(1, rest.size());
        java.util.List<Integer> firstLines = new java.util.ArrayList<>();
        int lineNumber = 1;
        for (String f : files) {
            String text = java.nio.file.Files.readString(java.nio.file.Path.of(f));
            firstLines.add(lineNumber);
            sourceText.append(text).append('\n');
            lineNumber += (int) (text + '\n').chars().filter(c -> c == '\n').count();
        }
        ScenarioResolver.ScenarioBundle b = ScenarioResolver.resolve(sourceText.toString());
        RegelSpraakModel m = b.model();
        if (b.scenarios().isEmpty())
            m.diagnostics.add(new Diagnostic(Severity.ERROR,
                    "RGS-095", params(), 0));
        RegelSpraakModel irModel = b.scenarios().isEmpty() ? m
                : dev.uchar.regelspraak.emit.CaseEmitter.withScope(m, b.scenarios().get(0).scope());
        // one chain of files, one line count: each diagnostic names the file and the line in that file, and only
        // those — the line in the chain means nothing to the author
        java.util.function.Function<Diagnostic, String> message = d -> {
            if (d.line() <= 0) return "  " + d.severity() + ": " + d.message();
            int i = firstLines.size() - 1;
            while (i > 0 && firstLines.get(i) > d.line()) i--;
            return "  " + d.severity() + " (" + files.get(i) + ":" + (d.line() - firstLines.get(i) + 1) + "): "
                    + d.message();
        };
        if (!m.isOk()) {
            m.diagnostics.forEach(d -> System.out.println(message.apply(d)));
            return 1;
        }
        m.diagnostics.stream().filter(d -> !d.isError()).forEach(d -> System.err.println(message.apply(d)));
        dev.uchar.regelspraak.emit.IrEmitter.unwrittenAllocations(irModel)
                .forEach(d -> System.err.println("  " + d));
        // Everything is decided before anything is written: the IR document and every case file as text, and the
        // case files the map already holds that this run would not write — a case left there beside the new ones
        // would be replayed against an IR document it was not written for. Written until the fix round of
        // 2026-09-26 file by file, a refusal after the first file left half a case directory behind.
        String ir = dev.uchar.regelspraak.emit.IrEmitter.emit(irModel);
        java.util.Map<String, String> cases = new java.util.LinkedHashMap<>();
        for (int i = 0; i < b.scenarios().size(); i++) {
            var s = b.scenarios().get(i);
            cases.put(dev.uchar.regelspraak.emit.CaseEmitter.fileName(i + 1, s),
                    dev.uchar.regelspraak.emit.CaseEmitter.emit(s));
        }
        java.util.List<String> foreign = new java.util.ArrayList<>();
        if (java.nio.file.Files.isDirectory(map))
            try (var entries = java.nio.file.Files.list(map)) {
                entries.map(p -> p.getFileName().toString())
                        .filter(n -> n.matches("case.*\\.json") && !cases.containsKey(n)).sorted().forEach(foreign::add);
            }
        if (!foreign.isEmpty()) {
            System.out.println("  ERROR: scenario: de map " + map + " draagt al casebestanden die dit document niet "
                    + "schrijft (" + String.join(", ", foreign) + "); ruim ze op of kies een andere map, want een "
                    + "casebestand naast de nieuwe speelt tegen een IR-document waarvoor het niet geschreven is");
            return 1;
        }
        java.nio.file.Files.createDirectories(map);
        for (var e : cases.entrySet())
            java.nio.file.Files.writeString(map.resolve(e.getKey()), e.getValue(), java.nio.charset.StandardCharsets.UTF_8);
        java.nio.file.Files.writeString(out, ir, java.nio.charset.StandardCharsets.UTF_8);
        System.out.printf("cases: %d casebestand(en) naar %s (%s); ir: resolvedRegels=%d geschreven naar %s%n",
                cases.size(), map, String.join(", ", cases.keySet()), irModel.resolvedRules.size(), out);
        return 0;
    }

    /** CLI: with --ir &lt;out.json&gt; over one-or-more source files, emit a FAITHFUL {@code uchar-ir-v1} JSON dump of the
     *  complete resolved model (the uchar "mirror" artifact); with --cases &lt;map&gt; beside it, the sources are read
     *  as one test document and its scenarios are lowered to case files as well ({@link #casesMode}). The IR is the
     *  product; a bare invocation prints usage and exits 2. */
    public static void main(String[] args) throws java.io.IOException {
        var rest = new java.util.ArrayList<>(java.util.List.of(args));
        boolean ir = rest.remove("--ir");
        int c = rest.indexOf("--cases");
        java.nio.file.Path map = null;
        if (c >= 0) {
            if (c + 1 >= rest.size()) { System.err.println("usage: Resolver --ir <out.json> --cases <map> <file.rgs...>"); System.exit(2); }
            map = java.nio.file.Path.of(rest.get(c + 1));
            rest.remove(c + 1);
            rest.remove(c);
        }
        if (!ir) { System.err.println("usage: Resolver --ir <out.json> [--cases <map>] <file.rgs...>"); System.exit(2); }
        System.exit(map == null ? irMode(rest) : casesMode(rest, map));
    }

    private Resolver() {}
}
