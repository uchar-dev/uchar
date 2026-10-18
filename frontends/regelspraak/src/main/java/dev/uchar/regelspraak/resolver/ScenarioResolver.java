package dev.uchar.regelspraak.resolver;

import dev.uchar.regelspraak.ir.Conditie;
import dev.uchar.regelspraak.ir.Default;
import dev.uchar.regelspraak.ir.ExceptionGroup;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.Geldigheid;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.ir.ResolvedScenario;
import dev.uchar.regelspraak.ir.ResolvedScenario.*;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.Eenheid;
import dev.uchar.regelspraak.model.RationalParser;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Feittype;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;
import dev.uchar.regelspraak.model.RegelSpraakModel.Parameter;
import dev.uchar.regelspraak.model.RegelSpraakModel.Rol;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.parser.RegelSpraak;
import dev.uchar.regelspraak.parser.RegelSpraak.*;
import dev.uchar.regelspraak.parser.RegelSpraakLexer;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static dev.uchar.regelspraak.emit.CaseEmitter.basisNaam;
import static dev.uchar.regelspraak.emit.CaseEmitter.norm;

/**
 * The resolver of the scenariotoetsen extension (grammar/DIVERGENCES.md, "Scenariotoetsen"): a documented extension
 * reachable only through the {@code testDocument} start rule. It
 * <ol>
 *   <li>parses the source under {@code testDocument};</li>
 *   <li>hands the non-scenario blocks, in document order, to the one model pipeline of {@link Resolver#build} as a
 *       document of their own, so the model — and the IR emitted from it — is the one the production route
 *       builds for the same blocks;</li>
 *   <li>resolves the {@code Situatie}, {@code Parameterset} and {@code Scenario} blocks into {@link ResolvedScenario}s,
 *       typing every value against its declaration and normalising it to what a case file of the format
 *       uchar-case-v0 carries; of a parameter set, a scenario takes the jaargang its peildatum falls in.</li>
 * </ol>
 * Every defect is an ERROR with its source line in the model's diagnostics; the lowering writes nothing for a model
 * that carries one. The sentences are the extension's own and Dutch: no reader refuses a case file for these defects,
 * because a case file that carries one is never written (grammar/DIVERGENCES.md, "Producer refusals without a reader
 * sentence").
 */
public final class ScenarioResolver {

    /** The resolver's output: the model (carrying every diagnostic, the scenario ones included) and the scenarios. */
    public record ScenarioBundel(RegelSpraakModel model, List<ResolvedScenario> scenarios) {
        public boolean isOk() { return model.isOk(); }
    }

    private final RegelSpraakModel model;
    private final TestDocumentContext test;
    /** Situation definitions by normalised name, the first declaration of a name. */
    private final Map<String, SituatieDefinitieContext> situations = new LinkedHashMap<>();
    /** Each situation's own block, validated once. */
    private final Map<String, BlockResult> ownBlocks = new HashMap<>();
    /** Each situation's blocks in merge order (its bases expanded, then its own block), computed once. */
    private final Map<String, List<BlockResult>> expandedBlocks = new HashMap<>();
    /** Parameter sets by normalised name, the first declaration of a name, each validated once. */
    private final Map<String, SetResult> parameterSets = new LinkedHashMap<>();

    private ScenarioResolver(RegelSpraakModel model, TestDocumentContext test) {
        this.model = model;
        this.test = test;
    }

    // ---------------- entry point ----------------

    public static ScenarioBundel resolve(String bron) {
        RegelSpraakLexer lexer = new RegelSpraakLexer(CharStreams.fromString(bron));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        RegelSpraak parser = new RegelSpraak(tokens);
        List<Diagnostic> syntax = new ArrayList<>();
        BaseErrorListener listener = new BaseErrorListener() {
            @Override public void syntaxError(Recognizer<?, ?> r, Object sym, int line, int col,
                                              String msg, RecognitionException e) {
                syntax.add(new Diagnostic(Severity.ERROR, "syntaxfout: " + msg, line));
            }
        };
        lexer.removeErrorListeners(); lexer.addErrorListener(listener);
        parser.removeErrorListeners(); parser.addErrorListener(listener);

        TestDocumentContext test = parser.testDocument();
        RegelSpraakModel model = Resolver.build(productieDocument(test), tokens, syntax);
        ScenarioResolver r = new ScenarioResolver(model, test);
        return new ScenarioBundel(model, r.resolveScenarios());
    }

    /** The non-scenario blocks of {@code test} as a {@code regelSpraakDocument} of their own, in document order. The
     *  typed accessors of the model pipeline scan {@code children}, so adding the blocks is enough; each keeps its
     *  tokens, so the hidden-channel annotations ({@code // Bron:}) above a block stay where the pipeline reads them.
     *  Terminals (EOF, the parser's error nodes) are skipped: the syntax listener already reports a malformed
     *  block. */
    private static RegelSpraakDocumentContext productieDocument(TestDocumentContext test) {
        RegelSpraakDocumentContext prod = new RegelSpraakDocumentContext(null, 0);
        if (test.children != null)
            for (ParseTree kind : test.children) {
                if (kind instanceof DefinitieContext || kind instanceof RegelGroepContext
                        || kind instanceof UitzonderingGroepContext || kind instanceof UitzonderingRegelContext
                        || kind instanceof RegelContext || kind instanceof BeslistabelContext)
                    prod.addAnyChild((ParserRuleContext) kind);
                else if (kind instanceof ParserRuleContext block && !(block instanceof SituatieDefinitieContext)
                        && !(block instanceof ParametersetDefinitieContext) && !(block instanceof ScenarioContext))
                    throw new IllegalStateException("testDocument draagt een blok dat deze route niet kent: "
                            + block.getClass().getSimpleName() + " op regel " + block.getStart().getLine());
            }
        return prod;
    }

    // ---------------- scenarios ----------------

    private List<ResolvedScenario> resolveScenarios() {
        List<ParseTree> children = test.children == null ? List.of() : test.children;
        for (ParseTree kind : children) {
            if (kind instanceof SituatieDefinitieContext sd) registerSituation(sd);
            // validated used or not, so a defect in a set never hides behind disuse
            if (kind instanceof ParametersetDefinitieContext pd) registerParameterSet(pd);
        }
        // every situation is expanded, used or not, so a cycle or an unknown basis never hides behind disuse
        for (String naam : situations.keySet()) foldOut(naam, List.of());
        List<ResolvedScenario> result = new ArrayList<>();
        List<ScenarioContext> contexten = new ArrayList<>();
        for (ParseTree kind : children)
            if (kind instanceof ScenarioContext sc) { result.add(resolveScenario(sc)); contexten.add(sc); }
        oneIrDocument(result, contexten);
        return result;
    }

    /** One document lowers to one IR document beside its case files, so every scenario must carry the same forms.
     *  A scenario without a scope carries all of them. */
    private void oneIrDocument(List<ResolvedScenario> scenarios, List<ScenarioContext> contexten) {
        for (int i = 1; i < scenarios.size(); i++) {
            List<String> first = carried(scenarios.get(0).scope()), current = carried(scenarios.get(i).scope());
            if (!new LinkedHashSet<>(first).equals(new LinkedHashSet<>(current))) {
                fout(contexten.get(i).getStart().getLine(), "de scenario's van één document lopen tegen één "
                        + "IR-document, maar het scenario '" + scenarios.get(i).naam() + "' toetst andere regels dan het "
                        + "scenario '" + scenarios.get(0).naam() + "'; zet scenario's met een andere scope in een "
                        + "eigen document");
                return;
            }
        }
    }

    /** The normalised names of the forms a scope carries; for no scope, every named form of the model. */
    private List<String> carried(Scope scope) {
        if (scope != null) return scope.vormen().stream().map(dev.uchar.regelspraak.emit.CaseEmitter::norm).toList();
        List<String> all = new ArrayList<>();
        for (Form v : vormen()) all.add(norm(v.naam()));
        return all;
    }

    private void registerSituation(SituatieDefinitieContext sd) {
        String written = src(sd.situatieNaam());
        String naam = norm(written);
        BlockResult block = validateBlock(sd.situatieBlok());   // a duplicate's block is still checked
        if (situations.containsKey(naam)) {
            fout(sd.getStart().getLine(), "de situatie '" + written + "' is meer dan eens gedeclareerd");
            return;
        }
        situations.put(naam, sd);
        ownBlocks.put(naam, block);
    }

    /** The blocks situation {@code naam} contributes, in merge order: each basis expanded in written order, then its
     *  own block. A basis that names no situation, or that closes a cycle, is an ERROR at the basis. */
    private List<BlockResult> foldOut(String naam, List<String> pad) {
        List<BlockResult> klaar = expandedBlocks.get(naam);
        if (klaar != null) return klaar;
        SituatieDefinitieContext sd = situations.get(naam);
        List<String> here = new ArrayList<>(pad);
        here.add(naam);
        List<BlockResult> result = new ArrayList<>();
        for (ScenarioBasisContext basis : sd.scenarioBasis()) {
            String written = src(basis.situatieNaam());
            String n = norm(written);
            if (!situations.containsKey(n)) {
                fout(basis.getStart().getLine(), "de situatie '" + written + "' is nergens in het document gedeclareerd");
                continue;
            }
            if (here.contains(n)) {
                List<String> cycle = new ArrayList<>();
                for (String step : here.subList(here.indexOf(n), here.size())) cycle.add("'" + declared(step) + "'");
                cycle.add("'" + declared(n) + "'");
                fout(basis.getStart().getLine(), "de situaties vormen een kring via 'gebaseerd op': "
                        + String.join(" → ", cycle));
                continue;
            }
            result.addAll(foldOut(n, here));
        }
        result.add(ownBlocks.get(naam));
        expandedBlocks.put(naam, result);
        return result;
    }

    private String declared(String normalised) {
        SituatieDefinitieContext sd = situations.get(normalised);
        return sd == null ? normalised : src(sd.situatieNaam());
    }

    private ResolvedScenario resolveScenario(ScenarioContext sc) {
        int line = sc.getStart().getLine();
        String naam = src(sc.scenarioNaam());
        if (dev.uchar.regelspraak.emit.CaseEmitter.slug(naam).isEmpty())
            fout(line, "de naam van het scenario '" + naam + "' geeft geen bestandsnaam: zij draagt geen letter of cijfer");
        Scope scope = sc.scenarioScope() == null ? null : resolveScope(sc.scenarioScope());
        LocalDate peildatum = sc.scenarioPeildatum() == null ? null : readAsOfDate(sc.scenarioPeildatum());
        SetResult set = null;
        int setLine = line;
        if (sc.scenarioParameterset() != null) {
            setLine = sc.scenarioParameterset().getStart().getLine();
            String written = src(sc.scenarioParameterset().parametersetNaam());
            set = parameterSets.get(norm(written));
            if (set == null) fout(setLine, "de parameterset '" + written + "' is nergens in het document gedeclareerd");
        }

        List<BlockResult> sources = new ArrayList<>();
        for (ScenarioBasisContext basis : sc.scenarioBasis()) {
            String written = src(basis.situatieNaam());
            String n = norm(written);
            if (!situations.containsKey(n))
                fout(basis.getStart().getLine(), "de situatie '" + written + "' is nergens in het document gedeclareerd");
            else sources.addAll(foldOut(n, List.of()));
        }
        if (sc.inlineSituatie() != null) sources.add(validateBlock(sc.inlineSituatie().situatieBlok()));
        if (sc.scenarioBasis().isEmpty() && sc.inlineSituatie() == null)
            fout(line, "het scenario '" + naam + "' heeft geen situatie: het is nergens op gebaseerd en draagt geen "
                    + "'Gegeven is de volgende situatie:'");

        Situatie situatie = merge(sources);
        checkRoleTargets(situatie);
        if (set != null) situatie = withParameterSet(situatie, set, peildatum, sc.scenarioPeildatum() != null, naam, setLine);
        Predicate<String> carries = carries(scope);
        // a written peildatum that was refused has its own ERROR, and the gate would call it missing
        if (sc.scenarioPeildatum() == null) asOfDateGate(carries, peildatum, naam, line);
        // a named set the document lacks has its ERROR, and what it would have given cannot be said
        if (sc.scenarioParameterset() == null || set != null) parameterGate(carries, situatie, set, naam, line);
        List<Verwachting> verwachtingen = resolveExpectations(sc.verwachting(), situatie);
        return new ResolvedScenario(naam, scope, peildatum, situatie, verwachtingen, line);
    }

    private LocalDate readAsOfDate(ScenarioPeildatumContext p) {
        String tekst = src(p.datumWaarde());
        int line = p.getStart().getLine();
        if (ExpressionBuilder.sourceDateTime(tekst) != null) {
            fout(line, "de peildatum '" + tekst + "' draagt een tijd, maar een casebestand draagt als peildatum "
                    + "alleen een dag (asOfDate)");
            return null;
        }
        LocalDate d = ExpressionBuilder.sourceDate(tekst);
        if (d == null) fout(line, "de peildatum '" + tekst + "' is geen geldige datum");
        return d;
    }

    // ---------------- the forms of the model, and the scope ----------------

    /** One named form of the model, with what the gates read of it. */
    private record Form(String naam, Geldigheid geldigheid, List<Expr> exprs, List<Conditie> conditions) {}

    /** Every named form of the model the emitted IR can carry or drop: rules, object and fact creations, allocations,
     *  consistencies, uniquenesses and defaults groups, each under its base name (a version suffix taken off). */
    private List<Form> vormen() {
        List<Form> result = new ArrayList<>();
        for (var r : model.resolvedRegels)
            result.add(new Form(basisNaam(r.naam()), r.geldigheid(), lijst(r.doel(), r.waarde()), lijst(r.voorwaarde())));
        for (var oc : model.objectCreaties) {
            List<Expr> exprs = new ArrayList<>();
            for (var t : oc.initialisations()) exprs.add(t.waarde());
            result.add(new Form(basisNaam(oc.naam()), oc.geldigheid(), exprs, lijst(oc.voorwaarde())));
        }
        for (var v : model.verdelingen) result.add(new Form(basisNaam(v.naam()), v.geldigheid(), List.of(), lijst(v.voorwaarde())));
        for (var c : model.consistenties) result.add(new Form(basisNaam(c.naam()), c.geldigheid(), List.of(), lijst(c.assertie())));
        for (var u : model.uniciteiten) result.add(new Form(basisNaam(u.naam()), null, List.of(), List.of()));
        for (var u : model.uniciteitUnies) result.add(new Form(basisNaam(u.naam()), null, List.of(), List.of()));
        for (var f : model.feitCreaties) result.add(new Form(basisNaam(f.naam()), f.geldigheid(), List.of(), lijst(f.voorwaarde())));
        for (var pd : model.prioritizedDefaults) {
            List<Expr> exprs = new ArrayList<>();
            List<Conditie> conditions = new ArrayList<>();
            collectDefaults(pd.defaults(), exprs, conditions);
            result.add(new Form(pd.naam(), pd.geldigheid(), exprs, conditions));
        }
        return result;
    }

    private static void collectDefaults(List<Default> defaults, List<Expr> exprs, List<Conditie> conditions) {
        if (defaults == null) return;
        for (Default d : defaults) {
            if (d.consequentie() != null) exprs.add(d.consequentie());
            if (d.voorwaarde() != null) conditions.add(d.voorwaarde());
            if (d.exceptions() != null)
                for (ExceptionGroup g : d.exceptions().leden()) collectDefaults(g.defaults(), exprs, conditions);
        }
    }

    @SafeVarargs
    private static <T> List<T> lijst(T... xs) {
        List<T> result = new ArrayList<>();
        for (T x : xs) if (x != null) result.add(x);
        return result;
    }

    /** Whether a form of that name is carried: always without a scope, else when the scope names it. */
    private static Predicate<String> carries(Scope scope) {
        if (scope == null) return n -> true;
        Set<String> names = new java.util.HashSet<>();
        for (String v : scope.vormen()) names.add(norm(v));
        return n -> names.contains(norm(n));
    }

    private Scope resolveScope(ScenarioScopeContext scope) {
        if (!model.resolvedStartPointDeterminations.isEmpty())
            fout(scope.getStart().getLine(), "een scope in een document met een startpuntbepaling wordt geweigerd: "
                    + "de bepaling en de regel die haar anker schrijft horen bij elkaar, en een scope die er één van "
                    + "laat vallen legt de tijdlijn op een ander raster");
        if (scope instanceof ScopeEnkeleRegelContext s) {
            String naam = src(s.enkeleRegelReferentie());
            return gescoped(formsOf(naam, s.getStart().getLine()));
        }
        if (scope instanceof ScopeMeerdereRegelsContext s) {
            List<String> names = new ArrayList<>();
            LinkedHashSet<String> vormen = new LinkedHashSet<>();
            for (RegelReferentieContext ref : s.regelReferentie()) {
                String naam = src(ref);
                names.add(naam);
                vormen.addAll(formsOf(naam, ref.getStart().getLine()));
            }
            return gescoped(List.copyOf(vormen));
        }
        ScopeRegelgroepContext s = (ScopeRegelgroepContext) scope;
        String group = src(s.enkeleRegelReferentie());
        var g = model.regelGroepen.stream().filter(rg -> norm(rg.naam()).equals(norm(group))).findFirst().orElse(null);
        if (g == null) {
            fout(s.getStart().getLine(), "de regelgroep '" + group + "' uit de scope bestaat niet in het document");
            return gescoped(List.of());
        }
        LinkedHashSet<String> vormen = new LinkedHashSet<>();
        for (String member : g.leden()) vormen.addAll(formsOf(member, s.getStart().getLine()));
        return gescoped(List.copyOf(vormen));
    }

    /** A scope over {@code named} and the defaults groups the stack of each concrete type needs beside them
     *  ({@link #withStack}), with the source lines of the blocks whose every form it leaves out. */
    private Scope gescoped(List<String> named) {
        List<String> vormen = withStack(named);
        Set<String> carried = new java.util.HashSet<>();
        for (String v : vormen) carried.add(norm(v));
        List<LineRange> omitted = new ArrayList<>();
        for (Block b : blocks()) omitted(b, carried, omitted);
        return new Scope(vormen, List.copyOf(omitted));
    }

    /** {@code vormen} with the defaults groups RUNTIME-SEMANTICS ch. 7.10 composes with them. The own exceptions of a
     *  subtype under the base of its supertype are a group of their own on the subtype that holds no base
     *  (grammar/DIVERGENCES.md §4, the inheritance line: {@code <basis> voor <subtype>}), and the instances of that
     *  subtype take the base from the supertype's group. A scope that carries the base therefore carries the groups
     *  that take it, and a scope that carries such a group carries its base and the other groups that take it, so the
     *  stack of every concrete type is the one of the whole document. Read off the groups — their subject, their
     *  target and whether a default at their top holds content of its own — not off their names. A subtype's own base
     *  is a tree of its own, which goes only where a scope names it. */
    private List<String> withStack(List<String> vormen) {
        LinkedHashSet<String> result = new LinkedHashSet<>(vormen);
        for (String naam : vormen)
            for (PrioritizedDefaults g : model.prioritizedDefaults) {
                if (g.naam() == null || !norm(g.naam()).equals(norm(naam))) continue;
                PrioritizedDefaults basis = withoutBase(g) ? baseOf(g) : g;
                if (basis == null) continue;
                result.add(basis.naam());
                for (PrioritizedDefaults h : model.prioritizedDefaults)
                    if (h != basis && withoutBase(h) && baseOf(h) == basis) result.add(h.naam());
            }
        return List.copyOf(result);
    }

    /** Whether no default at the top of {@code g} holds a decision or a consequence of its own: the group of a
     *  subtype's own exceptions under an inherited base (RUNTIME-SEMANTICS ch. 7.9, the group-default). */
    private static boolean withoutBase(PrioritizedDefaults g) {
        return g.defaults().stream().noneMatch(Default::heeftEigenInhoud);
    }

    /** The group whose base the instances of {@code g}'s subject take: on the nearest strict supertype that has one,
     *  the group with a base on the same target (WF-105 holds a member name to one type of a line). Null if none. */
    private PrioritizedDefaults baseOf(PrioritizedDefaults g) {
        Objecttype onderwerp = model.objecttypeOpNaam(g.onderwerp());
        if (onderwerp == null) return null;
        List<Objecttype> keten = onderwerp.keten();
        for (Objecttype t : keten.subList(1, keten.size()))
            for (PrioritizedDefaults b : model.prioritizedDefaults)
                if (!withoutBase(b) && b.isKenmerk() == g.isKenmerk() && norm(b.doel()).equals(norm(g.doel()))
                        && model.objecttypeOpNaam(b.onderwerp()) == t)
                    return b;
        return null;
    }

    /** A source block that yields named forms: a rule or a decision table (its forms carry its name, a table's rows
     *  its name and a number), an extension block (its defaults land in the group of its base rule, found by the
     *  source lines of the group's nodes), or a rule group (its members). */
    private record Block(String soort, String naam, int van, int tot, List<Block> leden) {}

    private List<Block> blocks() {
        List<Block> result = new ArrayList<>();
        for (ParseTree kind : test.children == null ? List.<ParseTree>of() : test.children) {
            if (kind instanceof RegelGroepContext g) {
                List<Block> leden = new ArrayList<>();
                for (GroepLidContext member : g.groepLid()) {
                    if (member.regel() != null) leden.add(block("regel", src(member.regel().regelNaam()), member.regel(), List.of()));
                    if (member.beslistabel() != null)
                        leden.add(block("tabel", src(member.beslistabel().beslistabelNaam()), member.beslistabel(), List.of()));
                }
                result.add(block("groep", src(g.kaleNaam()), g, leden));
            } else if (kind instanceof RegelContext r) result.add(block("regel", src(r.regelNaam()), r, List.of()));
            else if (kind instanceof BeslistabelContext b) result.add(block("tabel", src(b.beslistabelNaam()), b, List.of()));
            else if (kind instanceof UitzonderingRegelContext || kind instanceof UitzonderingGroepContext)
                result.add(block("uitbreiding", null, (ParserRuleContext) kind, List.of()));
        }
        return result;
    }

    private static Block block(String soort, String naam, ParserRuleContext ctx, List<Block> leden) {
        int van = ctx.getStart().getLine();
        return new Block(soort, naam, van, ctx.getStop() == null ? van : ctx.getStop().getLine(), leden);
    }

    /** Whether block {@code b} yields a carried form (TRUE), only forms the scope leaves out (FALSE) or no named form
     *  at all (null); the lines of each block that yields only left-out forms go to {@code uit}. */
    private Boolean omitted(Block b, Set<String> carried, List<LineRange> result) {
        Boolean carries = null;
        if (b.leden().isEmpty()) {
            for (Form v : vormen())
                if (comesFromBlock(v, b)) carries = Boolean.TRUE.equals(carries) || carried.contains(norm(v.naam()));
        } else {
            List<LineRange> within = new ArrayList<>();
            for (Block member : b.leden()) {
                Boolean d = omitted(member, carried, within);
                if (d != null) carries = Boolean.TRUE.equals(carries) || d;
            }
            if (!Boolean.FALSE.equals(carries)) result.addAll(within);
        }
        if (Boolean.FALSE.equals(carries)) result.add(new LineRange(b.van(), b.tot()));
        return carries;
    }

    /** Whether form {@code v} comes from block {@code b}: by name for a rule and a decision table, and for an
     *  extension block by the source lines of the form's nodes. A table row's nodes are read in a stream of their own,
     *  so their lines are no document lines, and the name is what places them. */
    private static boolean comesFromBlock(Form v, Block b) {
        return switch (b.soort()) {
            case "regel" -> norm(v.naam()).equals(norm(b.naam()));
            case "tabel" -> norm(v.naam()).matches(java.util.regex.Pattern.quote(norm(b.naam())) + " [0-9]+");
            default -> linesOf(v).stream().anyMatch(r -> r >= b.van() && r <= b.tot());
        };
    }

    /** The source lines of the nodes of form {@code v}. */
    private static Set<Integer> linesOf(Form v) {
        Set<Integer> result = new java.util.HashSet<>();
        Predicate<Expr> visitor = x -> {
            if (x.bron() != null && x.bron().line() > 0) result.add(x.bron().line());
            return false;
        };
        v.exprs().forEach(e -> Expr.contains(e, visitor));
        v.conditions().forEach(c -> linesOf(c, visitor, result));
        return result;
    }

    private static void linesOf(Conditie c, Predicate<Expr> visitor, Set<Integer> result) {
        if (c == null) return;
        if (c.bron() != null && c.bron().line() > 0) result.add(c.bron().line());
        Conditie.containsOperand(c, visitor);
        if (c instanceof Conditie.Samengesteld s) s.onderdelen().forEach(o -> linesOf(o, visitor, result));
        if (c instanceof Conditie.QuantifierFold k) { linesOf(k.filter(), visitor, result); linesOf(k.test(), visitor, result); }
    }

    /** The model forms a scope name carries: the named form itself, or every row of the decision table it names. */
    private List<String> formsOf(String naam, int line) {
        String n = norm(naam);
        List<String> result = new ArrayList<>();
        for (Form v : vormen()) if (norm(v.naam()).equals(n) && !result.contains(v.naam())) result.add(v.naam());
        if (!result.isEmpty()) return result;
        if (tableNames().contains(n)) {
            for (Form v : vormen())
                if (norm(v.naam()).matches(java.util.regex.Pattern.quote(n) + " [0-9]+") && !result.contains(v.naam()))
                    result.add(v.naam());
            return result;
        }
        if (model.regelGroepen.stream().anyMatch(g -> norm(g.naam()).equals(n)))
            fout(line, "'" + naam + "' uit de scope is een regelgroep; schrijf 'toetst de regelgroep " + naam + "'");
        else if (exceptionNames().contains(n))
            fout(line, "de regel '" + naam + "' uit de scope is opgegaan in een defaultsgroep; de scope noemt die groep "
                    + "bij de naam van haar basisregel");
        else fout(line, "de regel '" + naam + "' uit de scope bestaat niet in het document");
        return result;
    }

    private Set<String> tableNames() {
        Set<String> result = new java.util.HashSet<>();
        for (ParseTree kind : test.children == null ? List.<ParseTree>of() : test.children) {
            if (kind instanceof BeslistabelContext b) result.add(norm(src(b.beslistabelNaam())));
            if (kind instanceof RegelGroepContext g)
                for (GroepLidContext member : g.groepLid())
                    if (member.beslistabel() != null) result.add(norm(src(member.beslistabel().beslistabelNaam())));
        }
        return result;
    }

    /** The names of the dialect's exception blocks and of the members of its unordered blocks: rules the lowering
     *  lifts into a defaults group, which the IR carries under the group's name. */
    private Set<String> exceptionNames() {
        Set<String> result = new java.util.HashSet<>();
        for (ParseTree kind : test.children == null ? List.<ParseTree>of() : test.children) {
            if (kind instanceof UitzonderingRegelContext u) result.add(norm(src(u.regelNaam())));
            if (kind instanceof UitzonderingGroepContext g)
                for (RegelContext r : g.regel()) result.add(norm(src(r.regelNaam())));
        }
        return result;
    }

    // ---------------- the two gates ----------------

    /** A carried form that reads the evaluation date, or holds only within a validity window, needs a peildatum. */
    private void asOfDateGate(Predicate<String> carries, LocalDate peildatum, String scenario, int line) {
        if (peildatum != null) return;
        for (Form v : vormen()) {
            if (!carries.test(v.naam())) continue;
            boolean window = v.geldigheid() != null && !v.geldigheid().always();
            boolean uses = v.exprs().stream().anyMatch(e -> Expr.contains(e, Expr::isContextValue))
                    || v.conditions().stream().anyMatch(c -> Conditie.containsOperand(c, Expr::isContextValue));
            if (window || uses) {
                fout(line, "de regel '" + v.naam() + "' " + (window ? "heeft een geldigheidsvenster" : "leest de rekendatum")
                        + ", maar het scenario '" + scenario + "' heeft geen peildatum (voeg 'met peildatum <datum>' toe)");
                return;
            }
        }
    }

    /** Every parameter a carried form reads must have a value in the merged situation or in the chosen set. */
    private void parameterGate(Predicate<String> carries, Situatie situatie, SetResult set, String scenario, int line) {
        Set<String> assigned = new java.util.HashSet<>();
        for (ParameterWaarde p : situatie.parameters()) assigned.add(norm(p.parameter()));
        // a parameter of the set whose jaargang the peildatum could not choose has its own ERROR already
        if (set != null) assigned.addAll(set.parameters().keySet());
        String where = "in de situatie van het scenario '" + scenario + "'"
                + (set == null ? "" : " en in de parameterset '" + set.naam() + "'");
        Set<String> reported = new java.util.HashSet<>();
        for (Form v : vormen()) {
            if (!carries.test(v.naam())) continue;
            LinkedHashSet<String> read = new LinkedHashSet<>();
            Predicate<Expr> visitor = x -> { if (x instanceof Expr.ParameterRef pr) read.add(pr.parameter().naam()); return false; };
            v.exprs().forEach(e -> Expr.contains(e, visitor));
            v.conditions().forEach(c -> Conditie.containsOperand(c, visitor));
            for (String p : read)
                if (!assigned.contains(norm(p)) && reported.add(norm(p)))
                    fout(line, "de regel '" + v.naam() + "' leest de parameter '" + p + "', die " + where
                            + " geen waarde heeft");
        }
    }

    // ---------------- situation blocks ----------------

    /** One validated block: its instance fragments and its parameter values, in written order. */
    private record BlockResult(List<InstanceFragment> instanties, List<ParameterWaarde> parameters) {}
    /** One block's contribution to a labelled instance. */
    private record InstanceFragment(String label, Objecttype objecttype, List<AttribuutWaarde> attributen,
                                     List<RoleAssignment> rollen, List<KenmerkWaarde> kenmerken, int line) {}
    /** A role assignment keyed by its declared role, so a later assignment of the same role replaces it. */
    private record RoleAssignment(String rol, RolWaarde waarde) {}

    private BlockResult validateBlock(SituatieBlokContext block) {
        List<InstanceFragment> instanties = new ArrayList<>();
        List<ParameterWaarde> parameters = new ArrayList<>();
        Map<String, List<Segment>> parameterTimelines = new LinkedHashMap<>();
        for (SituatieOnderdeelContext ond : block.situatieOnderdeel()) {
            if (ond instanceof InstantieOnderdeelContext io) instanties.add(validateInstance(io.instantie()));
            else if (ond instanceof ParameterWaardeOnderdeelContext po) {
                ParameterWaarde pw = validateParameter(po.parameterWaarde(), parameters, parameterTimelines);
                if (pw != null) parameters.add(pw);
            }
        }
        return new BlockResult(instanties, parameters);
    }

    private InstanceFragment validateInstance(InstantieContext inst) {
        int line = inst.getStart().getLine();
        String typeName = src(inst.objectTypeNaam());
        String label = inst.UPPER_ID().getText();
        Objecttype ot = model.objecttypeOpNaam(typeName);
        if (ot == null) fout(line, "het objecttype '" + typeName + "' is niet gedeclareerd");
        String collision = labelCollidesWithType(label);
        if (collision != null)
            fout(line, "het label '" + label + "' botst met (het eerste woord van) de objecttypenaam '" + collision + "'");

        List<AttribuutWaarde> attributen = new ArrayList<>();
        Map<String, List<Segment>> tijdlijnen = new LinkedHashMap<>();
        Set<String> fixed = new java.util.HashSet<>();
        List<RoleAssignment> rollen = new ArrayList<>();
        List<KenmerkWaarde> kenmerken = new ArrayList<>();
        Map<String, List<Periode>> perioden = new LinkedHashMap<>();
        for (ToewijzingContext t : inst.toewijzing()) {
            if (t instanceof AttribuutToewijzingContext a) attribuut(a, ot, attributen, tijdlijnen, fixed);
            else if (t instanceof RolToOneToewijzingContext r)
                rol(ot, src(r.toewijzingNaam()), List.of(r.UPPER_ID().getText()), false, r.getStart().getLine(), rollen);
            else if (t instanceof RolToManyToewijzingContext r) {
                List<String> labels = new ArrayList<>();
                for (var u : r.UPPER_ID()) labels.add(u.getText());
                rol(ot, src(r.toewijzingNaam()), labels, true, r.getStart().getLine(), rollen);
            } else if (t instanceof KenmerkToewijzingContext k) kenmerk(k, ot, kenmerken, perioden);
        }
        return new InstanceFragment(label, ot, attributen, rollen, kenmerken, line);
    }

    private void attribuut(AttribuutToewijzingContext a, Objecttype ot, List<AttribuutWaarde> attributen,
                           Map<String, List<Segment>> tijdlijnen, Set<String> fixed) {
        int line = a.getStart().getLine();
        String written = withoutArticle(src(a.toewijzingNaam()));
        if (ot == null) return;                                        // unknown type: reported at the opener
        String attr = ot.attributeName(written);
        if (attr == null) {
            fout(line, "het objecttype '" + ot.naam + "' kent geen attribuut '" + written + "'");
            return;
        }
        if (!ot.dimensiesVan(attr).isEmpty()) {
            fout(line, "het attribuut '" + attr + "' is gedimensioneerd, en een casebestand draagt geen "
                    + "dimensiecoördinaten");
            return;
        }
        Datatype dt = ot.attribuut(attr);
        Waarde w = readAndType(a.situatieWaarde(), dt, line, "het attribuut '" + attr + "'");
        if (a.periode() == null) {
            if (tijdlijnen.containsKey(norm(attr))) {
                fout(line, "het attribuut '" + attr + "' krijgt in dit blok zowel perioden als een vaste waarde");
                return;
            }
            fixed.add(norm(attr));
            if (w != null) attributen.add(new AttribuutWaarde(attr, w, null, line));
            return;
        }
        if (ot.granulariteitVan(attr) == null) {
            fout(line, "het attribuut '" + attr + "' is niet tijdsafhankelijk; een periode ('van … tot …') hoort bij "
                    + "een attribuut met een tijdlijn");
            return;
        }
        if (fixed.contains(norm(attr))) {
            fout(line, "het attribuut '" + attr + "' krijgt in dit blok zowel een vaste waarde als perioden");
            return;
        }
        Periode p = readPeriod(a.periode());
        if (p == null || w == null || !onGrid(p, ot, attr, "het attribuut '" + attr + "'", line)) return;
        List<Segment> tl = tijdlijnen.get(norm(attr));
        if (tl == null) {
            tl = new ArrayList<>();
            tijdlijnen.put(norm(attr), tl);
            attributen.add(new AttribuutWaarde(attr, null, tl, line));
        }
        if (follows(tl.isEmpty() ? null : tl.get(tl.size() - 1).tot(), p, line)) tl.add(new Segment(p.van(), p.tot(), w));
    }

    private void kenmerk(KenmerkToewijzingContext k, Objecttype ot, List<KenmerkWaarde> kenmerken,
                         Map<String, List<Periode>> perioden) {
        int line = k.getStart().getLine();
        boolean possessive = k.HEEFT() != null;
        String written = withoutArticle(src(k.kaleNaam()));
        if (ot == null) return;
        String kenmerk = CharacteristicMatcher.find(ot, written);
        if (kenmerk == null) {
            fout(line, "het objecttype '" + ot.naam + "' kent geen kenmerk '" + written + "'");
            return;
        }
        if (!verbFits(ot, kenmerk, possessive, line)) return;
        boolean temporal = ot.kenmerkGranulariteitVan(kenmerk) != null;
        if (k.periode() == null) {
            if (temporal) {
                fout(line, "het kenmerk '" + kenmerk + "' is tijdsafhankelijk; een casebestand levert het als perioden "
                        + "('is " + kenmerk + " van <datum> tot <datum>'), niet als vaste waarde");
                return;
            }
            kenmerken.add(new KenmerkWaarde(kenmerk, null, line));
            return;
        }
        if (!temporal) {
            fout(line, "het kenmerk '" + kenmerk + "' is niet tijdsafhankelijk; een periode ('van … tot …') hoort bij "
                    + "een kenmerk met een tijdlijn");
            return;
        }
        Periode p = readPeriod(k.periode());
        if (p == null) return;
        List<Periode> lijst = perioden.get(norm(kenmerk));
        if (lijst == null) {
            lijst = new ArrayList<>();
            perioden.put(norm(kenmerk), lijst);
            kenmerken.add(new KenmerkWaarde(kenmerk, lijst, line));
        }
        if (follows(lijst.isEmpty() ? null : lijst.get(lijst.size() - 1).tot(), p, line)) lijst.add(p);
    }

    /** The verb follows the characteristic's kind (§3.5): 'heeft' for a possessive one, 'is' for the others. */
    private boolean verbFits(Objecttype ot, String kenmerk, boolean has, int line) {
        boolean possessive = ot.kenmerkSoort(kenmerk) == Objecttype.KenmerkSoort.BEZITTELIJK;
        if (possessive == has) return true;
        fout(line, possessive
                ? "het kenmerk '" + kenmerk + "' is bezittelijk; schrijf 'heeft " + kenmerk + "'"
                : "het kenmerk '" + kenmerk + "' is niet bezittelijk; schrijf 'is " + kenmerk + "'");
        return false;
    }

    /** The role the written name {@code written} names from an instance of {@code ot}: the other role of a fact
     *  type in which {@code ot} fills a role, matched by its declared name or plural — the reading of the case
     *  readers, so the key the case file carries resolves there to this role. */
    private void rol(Objecttype ot, String writtenWithArticle, List<String> labels, boolean listForm, int line,
                     List<RoleAssignment> rollen) {
        if (ot == null) return;
        String written = withoutArticle(writtenWithArticle);
        // the fact type travels with the role: the consumers of the situation cannot recover it from the name
        record Hit(Feittype feittype, Rol rol) {}
        List<Hit> hits = new ArrayList<>();
        for (Feittype ft : model.feittypen.values()) {
            if (ft.rollen.size() == 2) {
                Rol a = ft.rollen.get(0), b = ft.rollen.get(1);
                if (fills(ot, a) && isNamed(b, written)) hits.add(new Hit(ft, b));
                if (fills(ot, b) && isNamed(a, written)) hits.add(new Hit(ft, a));
            } else if (ft.rollen.size() == 1 && ft.wederkerig) {
                Rol r = ft.rollen.get(0);
                if (fills(ot, r) && isNamed(r, written)) hits.add(new Hit(ft, r));
            }
        }
        if (hits.isEmpty()) {
            fout(line, "het objecttype '" + ot.naam + "' kent geen rol '" + written + "'");
            return;
        }
        if (hits.size() > 1) {
            fout(line, "de rol '" + written + "' vanaf het objecttype '" + ot.naam + "' is niet eenduidig: meer dan "
                    + "één feittype kent haar");
            return;
        }
        Hit hit = hits.get(0);
        Rol far = hit.rol();
        if (far.meervoudig() && !listForm) {
            fout(line, "de rol '" + written + "' is een naar-veel-rol; schrijf de 'zijn'-lijstvorm ('de "
                    + written + " zijn …')");
            return;
        }
        if (!far.meervoudig() && listForm) {
            fout(line, "de rol '" + written + "' is een naar-één-rol; schrijf de 'is'-vorm ('de " + written
                    + " is …')");
            return;
        }
        String sleutel = far.plural() != null && norm(far.plural()).equals(norm(written)) ? far.plural() : far.naam();
        rollen.add(new RoleAssignment(far.naam(), new RolWaarde(sleutel, hit.feittype(),
                model.objecttypeOpNaam(far.objecttypeNaam()), List.copyOf(labels), far.meervoudig(), line)));
    }

    private boolean fills(Objecttype ot, Rol r) {
        Objecttype t = model.objecttypeOpNaam(r.objecttypeNaam());
        return t != null && ot.isEen(t);
    }

    private static boolean isNamed(Rol r, String written) {
        return norm(r.naam()).equals(norm(written)) || r.plural() != null && norm(r.plural()).equals(norm(written));
    }

    private ParameterWaarde validateParameter(ParameterWaardeContext pw, List<ParameterWaarde> parameters,
                                              Map<String, List<Segment>> tijdlijnen) {
        int line = pw.getStart().getLine();
        String written = src(pw.parameterNaam());
        Parameter p = parameterByName(withoutArticle(written));
        if (p == null) {
            fout(line, "de parameter '" + written + "' is niet gedeclareerd");
            return null;
        }
        Waarde w = readAndType(pw.situatieWaarde(), p.datatype(), line, "de parameter '" + p.naam() + "'");
        if (pw.periode() == null) {
            if (tijdlijnen.containsKey(norm(p.naam()))) {
                fout(line, "de parameter '" + p.naam() + "' krijgt in dit blok zowel perioden als een vaste waarde");
                return null;
            }
            return w == null ? null : new ParameterWaarde(p.naam(), w, null, line);
        }
        if (p.granulariteit() == null) {
            fout(line, "de parameter '" + p.naam() + "' is niet tijdsafhankelijk; een periode ('van … tot …') hoort "
                    + "bij een parameter met een tijdlijn");
            return null;
        }
        if (parameters.stream().anyMatch(x -> norm(x.parameter()).equals(norm(p.naam())) && x.waarde() != null)) {
            fout(line, "de parameter '" + p.naam() + "' krijgt in dit blok zowel een vaste waarde als perioden");
            return null;
        }
        Periode per = readPeriod(pw.periode());
        if (per == null || w == null
                || !onGrid(per, p.granulariteit(), 1, p.fixedStartPoint(), false, "de parameter '" + p.naam() + "'", line))
            return null;
        List<Segment> tl = tijdlijnen.get(norm(p.naam()));
        boolean fresh = tl == null;
        if (fresh) { tl = new ArrayList<>(); tijdlijnen.put(norm(p.naam()), tl); }
        if (follows(tl.isEmpty() ? null : tl.get(tl.size() - 1).tot(), per, line)) tl.add(new Segment(per.van(), per.tot(), w));
        return fresh ? new ParameterWaarde(p.naam(), null, tl, line) : null;
    }

    // ---------------- parameter sets ----------------

    /** One validated parameter set: its name as declared and, per parameter (keyed by its normalised name, in the
     *  order of its first row), its rows; {@code incomplete} holds the normalised names of the parameters one of
     *  whose rows was refused, whose jaargang a peildatum therefore cannot be said to miss. */
    private record SetResult(String naam, Map<String, SetParameter> parameters, Set<String> incomplete) {}

    /** The rows of one parameter of a set: its jaargangen, or for a parameter with a timeline the segments of that
     *  timeline; exactly one of the two is non-null. A timeline parameter given one row without a period has one
     *  jaargang, its value on every date. */
    private record SetParameter(String parameter, List<Vintage> vintages, List<Segment> tijdlijn, int line) {}

    /** One jaargang, {@code van} inclusive and {@code tot} exclusive, the period of a situation's segment; both
     *  null for a row without a period, which holds on every date. */
    private record Vintage(LocalDate van, LocalDate tot, Waarde waarde, int line) {
        boolean contains(LocalDate d) { return van == null || !d.isBefore(van) && d.isBefore(tot); }
        boolean overlaps(Vintage j) { return van == null || j.van == null || van.isBefore(j.tot) && j.van.isBefore(tot); }
        String toon() { return van == null ? "zonder periode" : "van " + van + " tot " + tot; }
    }

    private void registerParameterSet(ParametersetDefinitieContext pd) {
        String written = src(pd.parametersetNaam());
        SetResult set = validateSet(written, pd.parameterWaarde());   // a duplicate's rows are still checked
        if (parameterSets.containsKey(norm(written))) {
            fout(pd.getStart().getLine(), "de parameterset '" + written + "' is meer dan eens gedeclareerd");
            return;
        }
        parameterSets.put(norm(written), set);
    }

    /** The rows of set {@code setName}, each value typed against its declaration as a situation types it. The rows
     *  of a parameter without granularity are jaargangen, of which no two may share a day, because the peildatum
     *  of a scenario must choose exactly one; the rows of a parameter with a timeline are held to the rules of a
     *  situation's timeline — periods in order, apart, on the declared grid, and not beside a fixed value. */
    private SetResult validateSet(String setName, List<ParameterWaardeContext> regels) {
        Map<String, SetParameter> parameters = new LinkedHashMap<>();
        Set<String> incomplete = new java.util.HashSet<>();
        for (ParameterWaardeContext pw : regels) {
            String written = src(pw.parameterNaam());
            Parameter p = parameterByName(withoutArticle(written));
            if (p == null) fout(pw.getStart().getLine(), "de parameter '" + written + "' is niet gedeclareerd");
            else if (!setLine(setName, pw, p, parameters)) incomplete.add(norm(p.naam()));
        }
        return new SetResult(setName, parameters, incomplete);
    }

    /** Whether row {@code pw} of parameter {@code p} is accepted into {@code parameters}; a refused row has its
     *  ERROR. */
    private boolean setLine(String setName, ParameterWaardeContext pw, Parameter p, Map<String, SetParameter> parameters) {
        int line = pw.getStart().getLine();
        String what = "de parameter '" + p.naam() + "'";
        Waarde w = readAndType(pw.situatieWaarde(), p.datatype(), line, what);
        Periode per = pw.periode() == null ? null : readPeriod(pw.periode());
        if (pw.periode() != null && per == null) return false;
        boolean segment = p.granulariteit() != null && per != null;
        SetParameter sp = parameters.get(norm(p.naam()));
        if (sp == null) {
            sp = new SetParameter(p.naam(), segment ? null : new ArrayList<>(), segment ? new ArrayList<>() : null, line);
            parameters.put(norm(p.naam()), sp);
        } else if ((sp.tijdlijn() != null) != segment) {
            fout(line, what + " krijgt in dit blok " + (segment ? "zowel een vaste waarde als perioden"
                    : "zowel perioden als een vaste waarde"));
            return false;
        }
        if (w == null) return false;
        if (segment) {
            if (!onGrid(per, p.granulariteit(), 1, p.fixedStartPoint(), false, what, line)) return false;
            List<Segment> tl = sp.tijdlijn();
            if (!follows(tl.isEmpty() ? null : tl.get(tl.size() - 1).tot(), per, line)) return false;
            tl.add(new Segment(per.van(), per.tot(), w));
            return true;
        }
        Vintage fresh = new Vintage(per == null ? null : per.van(), per == null ? null : per.tot(), w, line);
        Vintage collision = sp.vintages().stream().filter(j -> j.overlaps(fresh)).findFirst().orElse(null);
        if (collision != null) {
            fout(line, "de parameterset '" + setName + "' geeft de parameter '" + p.naam() + "' twee jaargangen die "
                    + "elkaar overlappen (" + collision.toon() + " en " + fresh.toon() + "); de peildatum van een scenario "
                    + "kiest precies één jaargang");
            return false;
        }
        sp.vintages().add(fresh);
        return true;
    }

    /** The situation of a scenario that chooses {@code set}, with the set's values before its own: per parameter
     *  of the set the jaargang whose period holds the peildatum, or the whole timeline, which a case file carries
     *  as it stands. A parameter the situation gives a value as well is refused rather than one of the two winning,
     *  and so is a jaargang the peildatum cannot choose. A parameter's absence from both stays the parameter gate's
     *  refusal. */
    private Situatie withParameterSet(Situatie situatie, SetResult set, LocalDate peildatum, boolean asOfDateWritten,
                                     String scenario, int line) {
        Set<String> inSituation = new java.util.HashSet<>();
        for (ParameterWaarde p : situatie.parameters()) inSituation.add(norm(p.parameter()));
        List<ParameterWaarde> result = new ArrayList<>();
        boolean withoutAsOfDateReported = false;
        for (SetParameter sp : set.parameters().values()) {
            if (inSituation.contains(norm(sp.parameter()))) {
                fout(line, "de parameter '" + sp.parameter() + "' krijgt een waarde uit de parameterset '" + set.naam()
                        + "' en uit de situatie van het scenario '" + scenario + "'; een parameter krijgt zijn waarde "
                        + "uit één van beide");
                continue;
            }
            // a refused row has its ERROR, and a peildatum that misses the rest would be a second one
            if (set.incomplete().contains(norm(sp.parameter()))) continue;
            if (sp.tijdlijn() != null) {
                result.add(new ParameterWaarde(sp.parameter(), null, sp.tijdlijn(), sp.line()));
                continue;
            }
            List<Vintage> vintages = sp.vintages();
            Vintage chosen = null;
            if (vintages.size() == 1 && vintages.get(0).van() == null) chosen = vintages.get(0);
            else if (peildatum == null) {
                // a written peildatum that was refused has its own ERROR; one sentence names the defect for all
                if (!asOfDateWritten && !withoutAsOfDateReported)
                    fout(line, "de parameter '" + sp.parameter() + "' heeft in de parameterset '" + set.naam()
                            + "' jaargangen, maar het scenario '" + scenario + "' heeft geen peildatum die er één kiest "
                            + "(voeg 'met peildatum <datum>' toe)");
                withoutAsOfDateReported = true;
                continue;
            } else {
                for (Vintage j : vintages) if (j.contains(peildatum)) { chosen = j; break; }
                if (chosen == null) {
                    fout(line, "de parameterset '" + set.naam() + "' heeft voor de parameter '" + sp.parameter()
                            + "' geen jaargang die de peildatum " + peildatum + " van het scenario '" + scenario
                            + "' bevat");
                    continue;
                }
            }
            result.add(new ParameterWaarde(sp.parameter(), chosen.waarde(), null, chosen.line()));
        }
        result.addAll(situatie.parameters());
        return new Situatie(situatie.instanties(), result);
    }

    // ---------------- periods ----------------

    private Periode readPeriod(PeriodeContext p) {
        int line = p.getStart().getLine();
        LocalDate[] bounds = new LocalDate[2];
        // error recovery can leave a period without its second date; the syntax error names it already
        if (p.DATE(1) == null) return null;
        for (int i = 0; i < 2; i++) {
            String tekst = p.DATE(i).getText().replaceAll("\\s+", " ").trim();
            if (ExpressionBuilder.sourceDateTime(tekst) != null) {
                fout(line, "de periodegrens '" + tekst + "' draagt een tijd, maar een periode telt in hele dagen");
                return null;
            }
            bounds[i] = ExpressionBuilder.sourceDate(tekst);
            if (bounds[i] == null) {
                fout(line, "de periodegrens '" + tekst + "' is geen geldige datum");
                return null;
            }
        }
        if (!bounds[0].isBefore(bounds[1])) {
            fout(line, "de periode van " + bounds[0] + " tot " + bounds[1] + " is leeg of loopt terug");
            return null;
        }
        return new Periode(bounds[0], bounds[1]);
    }

    /** Whether period {@code p} may follow a period that ended at {@code previousTo}: the periods of one timeline
     *  stand in order and do not overlap (a gap is allowed). */
    private boolean follows(LocalDate previousTo, Periode p, int line) {
        if (previousTo == null || !p.van().isBefore(previousTo)) return true;
        fout(line, "de periode van " + p.van() + " tot " + p.tot() + " begint vóór het einde van de vorige ("
                + previousTo + "); de perioden van één tijdlijn staan in volgorde en overlappen niet");
        return false;
    }

    /** Whether both bounds of {@code p} sit on the grid of attribute {@code attr} of {@code ot}: its granularity,
     *  count and fixed start point, as its facet declares them. */
    private boolean onGrid(Periode p, Objecttype ot, String attr, String what, int line) {
        Integer count = ot.tijdlijnAantalVan(attr);
        return onGrid(p, ot.granulariteitVan(attr), count == null ? 1 : count, ot.fixedStartPointOf(attr),
                model.hasVariableStart(ot, attr), what, line);
    }

    /** Every case reader holds the segments of a timeline to the walls of its declared grid (the case format's
     *  timeline value), so a bound off the grid is an ERROR here, per bound, rather than a case file those readers
     *  refuse. A variable start lays its grid per object from an anchor a rule writes, and a grid that lays no wall
     *  a day can be held against ({@link dev.uchar.regelspraak.kern.FixedStartPoint#opWand}) has nothing to hold a
     *  bound to: both are left to the readers. */
    private boolean onGrid(Periode p, RegelSpraakModel.Granulariteit g, int count, String fixed, boolean variable, String what,
                             int line) {
        if (variable) return true;
        boolean goed = true;
        for (LocalDate limit : List.of(p.van(), p.tot()))
            if (Boolean.FALSE.equals(dev.uchar.regelspraak.kern.FixedStartPoint.opWand(g, count, fixed, limit))) {
                fout(line, "de periodegrens " + limit + " van " + what + " ligt niet op het " + gridName(g, count)
                        + " van de declaratie" + (fixed == null ? "" : " (vast startpunt " + fixed + ")")
                        + "; een casebestand levert een tijdlijn in de perioden van dat raster");
                goed = false;
            }
        return goed;
    }

    private static String gridName(RegelSpraakModel.Granulariteit g, int count) {
        String[] word = switch (g) {
            case DAG -> new String[]{"dag", "dagen"};
            case WEEK -> new String[]{"week", "weken"};
            case MAAND -> new String[]{"maand", "maanden"};
            case KWARTAAL -> new String[]{"kwartaal", "kwartalen"};
            case JAAR -> new String[]{"jaar", "jaren"};
        };
        return count == 1 ? word[0] + "raster" : "raster van " + count + " " + word[1];
    }

    // ---------------- the merge ----------------

    /** Merge the validated blocks in order into one situation: one label is one instance (its assignments accumulate,
     *  a later assignment of the same attribute, role or characteristic replacing the earlier one in place), and a
     *  later parameter value replaces an earlier one in place. */
    private Situatie merge(List<BlockResult> sources) {
        Map<String, InstanceBuilder> instanties = new LinkedHashMap<>();
        Map<String, ParameterWaarde> parameters = new LinkedHashMap<>();
        for (BlockResult block : sources) {
            for (InstanceFragment frag : block.instanties()) {
                InstanceBuilder builder = instanties.get(frag.label());
                if (builder == null) {
                    builder = new InstanceBuilder(frag.label(), frag.objecttype());
                    instanties.put(frag.label(), builder);
                } else if (frag.objecttype() != null && builder.objecttype != null && builder.objecttype != frag.objecttype()) {
                    fout(frag.line(), "het label '" + frag.label() + "' is eerder gedeclareerd als '"
                            + builder.objecttype.naam + "', hier als '" + frag.objecttype().naam + "'");
                }
                frag.attributen().forEach(builder::setAttribute);
                frag.rollen().forEach(builder::setRole);
                frag.kenmerken().forEach(builder::setCharacteristic);
            }
            for (ParameterWaarde pw : block.parameters()) parameters.put(norm(pw.parameter()), pw);
        }
        List<Instantie> result = new ArrayList<>();
        for (InstanceBuilder b : instanties.values()) result.add(b.build());
        return new Situatie(result, new ArrayList<>(parameters.values()));
    }

    /** After the merge: every role target names a label of the merged situation whose type fills the role — a
     *  role in a based-on situation may point at an inline instance, so this runs on the merged result. */
    private void checkRoleTargets(Situatie situatie) {
        for (Instantie i : situatie.instanties()) {
            for (RolWaarde r : i.rollen()) {
                Objecttype doel = r.doeltype();
                for (String label : r.doelLabels()) {
                    Instantie ref = situatie.instantie(label);
                    if (ref == null)
                        fout(r.line(), "de rol '" + r.sleutel() + "' verwijst naar het label '" + label + "', dat nergens "
                                + "in de samengestelde situatie gedeclareerd is");
                    else if (doel != null && ref.objecttype() != null && !ref.objecttype().isEen(doel))
                        fout(r.line(), "de rol '" + r.sleutel() + "' verwacht een " + doel.naam + ", maar het label '"
                                + label + "' is een " + ref.objecttype().naam);
                }
            }
        }
    }

    /** Accumulates the assignments to one labelled instance across the merge. */
    private static final class InstanceBuilder {
        final String label;
        final Objecttype objecttype;
        private final Map<String, AttribuutWaarde> attributen = new LinkedHashMap<>();
        private final Map<String, RolWaarde> rollen = new LinkedHashMap<>();
        private final Map<String, KenmerkWaarde> kenmerken = new LinkedHashMap<>();

        InstanceBuilder(String label, Objecttype objecttype) { this.label = label; this.objecttype = objecttype; }

        void setAttribute(AttribuutWaarde w) { attributen.put(norm(w.attribuut()), w); }
        void setRole(RoleAssignment t) { rollen.put(norm(t.rol()), t.waarde()); }
        void setCharacteristic(KenmerkWaarde w) { kenmerken.put(norm(w.kenmerk()), w); }

        Instantie build() {
            return new Instantie(label, objecttype, new ArrayList<>(attributen.values()),
                    new ArrayList<>(rollen.values()), new ArrayList<>(kenmerken.values()));
        }
    }

    // ---------------- expectations ----------------

    private List<Verwachting> resolveExpectations(List<VerwachtingContext> bullets, Situatie situatie) {
        List<Verwachting> result = new ArrayList<>();
        Map<String, Verwachting> tijdlijnen = new HashMap<>();          // label|attribuut -> its one entry
        Map<String, Boolean> alwaysEmpty = new HashMap<>();
        for (VerwachtingContext v : bullets) {
            int line = v.getStart().getLine();
            if (v instanceof VerwachtAttribuutLeegContext a) {
                String label = a.UPPER_ID().getText();
                String attr = expectedAttribute(label, withoutArticle(src(a.verwachtingNaam())), situatie, line);
                if (attr != null)
                    result.add(new Verwachting(ExpectationKind.ATTRIBUUT_LEEG, label, attr, null, null, null, null, line));
            } else if (v instanceof VerwachtTijdlijnLeegContext a) {
                String label = a.UPPER_ID().getText();
                String attr = expectedAttribute(label, withoutArticle(src(a.verwachtingNaam())), situatie, line);
                if (attr == null || !temporalAttribute(label, attr, situatie, line)) continue;
                String sleutel = label + "|" + norm(attr);
                if (tijdlijnen.containsKey(sleutel)) {
                    fout(line, "de verwachting dat '" + attr + "' van " + label + " altijd leeg is, spreekt de "
                            + "perioden ervoor tegen");
                    continue;
                }
                Verwachting t = new Verwachting(ExpectationKind.ATTRIBUUT_TIJDLIJN, label, attr, null, null,
                        new ArrayList<>(), null, line);
                tijdlijnen.put(sleutel, t);
                alwaysEmpty.put(sleutel, true);
                result.add(t);
            } else if (v instanceof VerwachtAttribuutWaardeContext a) {
                String label = a.UPPER_ID().getText();
                String attr = expectedAttribute(label, withoutArticle(src(a.verwachtingNaam())), situatie, line);
                if (attr == null) continue;
                Datatype dt = situatie.instantie(label).objecttype().attribuut(attr);
                Waarde w = readAndType(a.situatieWaarde(), dt, line, "de verwachting voor '" + attr + "'");
                if (a.periode() == null) {
                    if (w != null)
                        result.add(new Verwachting(ExpectationKind.ATTRIBUUT_WAARDE, label, attr, null, w, null, null, line));
                    continue;
                }
                if (!temporalAttribute(label, attr, situatie, line)) continue;
                Periode p = readPeriod(a.periode());
                if (p == null || w == null
                        || !onGrid(p, situatie.instantie(label).objecttype(), attr, "de verwachting voor '" + attr + "'", line))
                    continue;
                String sleutel = label + "|" + norm(attr);
                if (Boolean.TRUE.equals(alwaysEmpty.get(sleutel))) {
                    fout(line, "de periode voor '" + attr + "' van " + label + " spreekt de verwachting tegen dat het "
                            + "attribuut altijd leeg is");
                    continue;
                }
                Verwachting t = tijdlijnen.get(sleutel);
                if (t == null) {
                    t = new Verwachting(ExpectationKind.ATTRIBUUT_TIJDLIJN, label, attr, null, null, new ArrayList<>(),
                            null, line);
                    tijdlijnen.put(sleutel, t);
                    result.add(t);
                }
                List<Segment> tl = t.tijdlijn();
                if (follows(tl.isEmpty() ? null : tl.get(tl.size() - 1).tot(), p, line)) tl.add(new Segment(p.van(), p.tot(), w));
            } else if (v instanceof VerwachtKenmerkContext k) {
                Verwachting r = verwachtKenmerk(k.UPPER_ID().getText(), withoutArticle(src(k.kaleNaam())), false,
                        situatie, line);
                if (r != null) result.add(r);
            } else if (v instanceof VerwachtKenmerkHeeftContext k) {
                Verwachting r = verwachtKenmerk(k.UPPER_ID().getText(), withoutArticle(src(k.kaleNaam())), true,
                        situatie, line);
                if (r != null) result.add(r);
            } else if (v instanceof VerwachtInconsistentContext c) {
                String regel = null, label = null;
                InconsistentieSleutelContext s = c.inconsistentieSleutel();
                if (s != null) {
                    regel = textContent(s.tekstWaarde(), line, "de regel van de inconsistentie");
                    if (regel != null && !knownInconsistencyRule(regel)) {
                        fout(line, "de regel '" + regel + "' van de inconsistentie bestaat niet in het document en is "
                                + "geen conflictkanaal ('<Objecttype>.<attribuut of kenmerk>', eventueel met '/<pad>')");
                        regel = null;
                    }
                    if (s.UPPER_ID() != null) {
                        label = s.UPPER_ID().getText();
                        if (situatie.instantie(label) == null) {
                            fout(line, "het label '" + label + "' in de verwachting is nergens in de situatie gedeclareerd");
                            label = null;
                        }
                    }
                }
                if (s == null || regel != null)
                    result.add(new Verwachting(ExpectationKind.INCONSISTENT, label, null, null, null, null, regel, line));
            } else if (v instanceof VerwachtConsistentContext c) {
                result.add(new Verwachting(ExpectationKind.CONSISTENT, null, null, null, null, null, null, line));
            } else if (!src(v).isBlank()) {
                // error recovery leaves a bare VerwachtingContext; the syntax error is reported already, and a bare
                // context without text is recovery's own and names nothing
                fout(line, "de verwachting '" + src(v) + "' is syntactisch niet herkend");
            }
        }
        return result;
    }

    /** The declared attribute {@code written} names on the instance labelled {@code label}; null after an ERROR. */
    private String expectedAttribute(String label, String written, Situatie situatie, int line) {
        Instantie i = situatie.instantie(label);
        if (i == null) {
            fout(line, "het label '" + label + "' in de verwachting is nergens in de situatie gedeclareerd");
            return null;
        }
        if (i.objecttype() == null) return null;                      // unknown type: reported at the opener
        String attr = i.objecttype().attributeName(written);
        if (attr == null) {
            fout(line, "het objecttype '" + i.objecttype().naam + "' kent geen attribuut '" + written + "'");
            return null;
        }
        if (!i.objecttype().dimensiesVan(attr).isEmpty()) {
            fout(line, "het attribuut '" + attr + "' is gedimensioneerd, en een casebestand draagt geen "
                    + "dimensiecoördinaten");
            return null;
        }
        return attr;
    }

    private boolean temporalAttribute(String label, String attr, Situatie situatie, int line) {
        if (situatie.instantie(label).objecttype().granulariteitVan(attr) != null) return true;
        fout(line, "het attribuut '" + attr + "' is niet tijdsafhankelijk; een tijdlijnverwachting hoort bij een "
                + "attribuut met een tijdlijn");
        return false;
    }

    private Verwachting verwachtKenmerk(String label, String written, boolean has, Situatie situatie, int line) {
        // "geen <kenmerk>" and "niet <kenmerk>" are the absent form: the grammar keeps the word inside the kaleNaam
        String klein = written.toLowerCase(java.util.Locale.ROOT);
        boolean absent = klein.startsWith("geen ") || klein.startsWith("niet ");
        String naam = absent ? written.substring(5).trim() : written;
        Instantie i = situatie.instantie(label);
        if (i == null) {
            fout(line, "het label '" + label + "' in de verwachting is nergens in de situatie gedeclareerd");
            return null;
        }
        if (i.objecttype() == null) return null;
        String kenmerk = CharacteristicMatcher.find(i.objecttype(), withoutArticle(naam));
        if (kenmerk == null) {
            fout(line, "het objecttype '" + i.objecttype().naam + "' kent geen kenmerk '" + naam + "'");
            return null;
        }
        if (!verbFits(i.objecttype(), kenmerk, has, line)) return null;
        return new Verwachting(absent ? ExpectationKind.KENMERK_AFWEZIG : ExpectationKind.KENMERK_AANWEZIG,
                label, null, kenmerk, null, null, null, line);
    }

    /** A rule an inconsistency may name: a named form of the model, or a conflict channel — a target form
     *  {@code <Objecttype>.<member>}, optionally with a default index path {@code /<n>[/<n>…]}. */
    private boolean knownInconsistencyRule(String regel) {
        String n = norm(regel);
        for (Form v : vormen()) if (norm(v.naam()).equals(n)) return true;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("([^./]+)\\.([^/]+)(/[0-9]+)*").matcher(regel.trim());
        if (!m.matches()) return false;
        Objecttype ot = model.objecttypeOpNaam(m.group(1).trim());
        return ot != null && ot.hasMember(m.group(2).trim());
    }

    // ---------------- values ----------------

    /** Read {@code ctx}, type it against {@code doel} and normalise it to its case form; null after an ERROR. */
    private Waarde readAndType(SituatieWaardeContext ctx, Datatype doel, int line, String what) {
        if (ctx instanceof GetalSituatieWaardeContext g) return getal(g.getalWaarde(), doel, line, what);
        if (ctx instanceof DatumSituatieWaardeContext d) {
            String tekst = src(d.datumWaarde());
            LocalDateTime dt = ExpressionBuilder.sourceDateTime(tekst);
            LocalDate dd = dt == null ? ExpressionBuilder.sourceDate(tekst) : null;
            if (dt == null && dd == null) {
                fout(line, what + ": de datum '" + tekst + "' is geen geldige datum");
                return null;
            }
            Datatype raw = dt != null ? new Datatype.DatumTijd() : new Datatype.DatumInDagen();
            if (!toekenbaar(doel, raw, line, what)) return null;
            return dt != null ? Waarde.fromDateTime(dt) : Waarde.fromDate(dd);
        }
        if (ctx instanceof BooleanSituatieWaardeContext b) {
            if (!toekenbaar(doel, new Datatype.Booleaan(), line, what)) return null;
            return Waarde.fromBoolean(b.booleanWaarde().WAAR() != null);
        }
        if (ctx instanceof EnumSituatieWaardeContext e) {
            String label = dev.uchar.regelspraak.kern.Literals.enumLabel(e.ENUM_LITERAL().getText());
            if (!(doel instanceof Datatype.Enumeratie enumDatatype)) {
                if (doel != null && !doel.isUnknown())
                    fout(line, what + ": een enumeratiewaarde ('" + label + "') past niet op een " + doel.naam());
                return null;
            }
            for (String w : enumDatatype.waarden()) if (w.equalsIgnoreCase(label)) return Waarde.fromEnum(w);
            fout(line, what + ": '" + label + "' is geen waarde van de enumeratie " + enumDatatype.domein() + " ("
                    + String.join(", ", enumDatatype.waarden()) + ")");
            return null;
        }
        if (ctx instanceof TekstSituatieWaardeContext t) {
            String tekst = textContent(t.tekstWaarde(), line, what);
            if (tekst == null || !toekenbaar(doel, new Datatype.Tekst(), line, what)) return null;
            return Waarde.fromText(tekst);
        }
        // a value the parse could not read ("de parameter de vondstdrempel is" and no value): the syntax error stands
        // beside this one, which names the position
        fout(line, what + (src(ctx).isBlank() ? ": er staat geen waarde" : ": de waarde '" + src(ctx) + "' is niet gelezen"));
        return null;
    }

    private boolean toekenbaar(Datatype doel, Datatype raw, int line, String what) {
        if (doel == null || doel.isUnknown()) return true;
        String f = Typing.assignment(doel, raw);
        if (f == null) return true;
        fout(line, what + ": " + f);
        return false;
    }

    /** A number: its exact value in the unit written, typed against {@code doel}, restated exactly in the declared
     *  unit. Without a restatement the lexeme is the emitter's for the written number
     *  ({@link dev.uchar.regelspraak.emit.IrEmitter#numberLexeme}) — a decimal its written precision (2400,00 stays
     *  2400.00), a fraction reduced (6/8 is 3/4) and a whole number where it reduces to one (2_0/5 is 2); a restated
     *  value is a terminating decimal without trailing zeros, or else a reduced fraction. */
    private Waarde getal(GetalWaardeContext g, Datatype doel, int line, String what) {
        String tekst = g.NUMBER().getText();
        BigDecimal[] r = RationalParser.van(tekst);
        // the parse enters a number value on a NUMBER token only, and every NUMBER token is a number Rationaal reads
        if (r == null) throw new IllegalStateException("ScenarioResolver: het getal '" + tekst + "' is geen getal");
        BigInteger[] rational = rational(r[0], r[1]);
        int dec = r[1].compareTo(BigDecimal.ONE) == 0 ? Math.max(0, r[0].stripTrailingZeros().scale())
                : terminates(rational[1]) ? Math.max(0, new BigDecimal(rational[0]).divide(new BigDecimal(rational[1])).stripTrailingZeros().scale())
                : 16;
        boolean geheel = dec == 0;
        Datatype raw;
        Eenheid sourceUnit = null;
        if (g.PERCENT_SIGN() != null) raw = new Datatype.Percentage(geheel, dec);
        else {
            sourceUnit = g.eenheidExpressie() != null ? DefinitionResolver.unitOf(model, g.eenheidExpressie()) : null;
            raw = new Datatype.Numeriek(geheel, dec, Datatype.Teken.ONBEPAALD, sourceUnit);
        }
        if (!toekenbaar(doel, raw, line, what)) return null;
        Eenheid targetUnit = doel == null ? null : doel.eenheidOf();
        if (sourceUnit != null && targetUnit != null && sourceUnit.convertibleTo(targetUnit) && sourceUnit.requiredConversion(targetUnit)) {
            BigInteger[] fb = exactRescaling(sourceUnit, targetUnit);
            // the typing admits two units with one canonical only, and every declared unit has its exact factor
            if (fb == null)
                throw new IllegalStateException("ScenarioResolver: tussen de eenheden '" + sourceUnit.weergave() + "' en '"
                        + targetUnit.weergave() + "' is geen exacte factor, terwijl de typering ze omwisselbaar noemt");
            return Waarde.fromNumber(lexemeText(rational[0].multiply(fb[0]), rational[1].multiply(fb[1])));
        }
        // no restatement: the written number in the emitter's lexeme, so a case file and an IR document agree
        return Waarde.fromNumber(dev.uchar.regelspraak.emit.IrEmitter.numberLexeme(tekst));
    }

    /** The exact factor {num, den} that restates a quantity in {@code van} as one in {@code naar}: each unit's
     *  factor to the shared canonical as its system declares it, a rate's denominators inverted. The unit records
     *  carry those factors rounded to 16 digits, and a restatement through them was a rounding: "2 gtd" with
     *  "gtd = 1/3 etm" became 0.6666666666666666 etm where the wire and every executor read 2/3. Null where a unit
     *  has no exact factor. */
    private BigInteger[] exactRescaling(Eenheid van, Eenheid to) {
        BigInteger[] a = exactFactor(van.numerator()), b = exactFactor(to.numerator());
        if (a == null || b == null) return null;
        BigInteger num = a[0].multiply(b[1]), den = a[1].multiply(b[0]);
        if (van.denominator() != null && to.denominator() != null) {
            BigInteger[] c = exactFactor(van.denominator()), d = exactFactor(to.denominator());
            if (c == null || d == null) return null;
            num = num.multiply(d[0]).multiply(c[1]);
            den = den.multiply(d[1]).multiply(c[0]);
        }
        return new BigInteger[]{num, den};
    }

    /** The exact factor of {@code e} to its canonical: the fraction its system declares, else a whole decimal factor
     *  (a unit that is its own canonical, a built-in unit), else null — as the emitter writes it on the wire. */
    private BigInteger[] exactFactor(Eenheid e) {
        BigDecimal f = e.factorNaarCanoniek();
        if (f == null) return null;
        RegelSpraakModel.UnitSystem es = e.stelsel() == null ? null : model.unitSystems.get(e.stelsel());
        RegelSpraakModel.UnitSystem.Factor x = es == null ? null : es.exactFactors.get(e.afkorting());
        if (x != null) return new BigInteger[]{x.num(), x.den()};
        return f.signum() > 0 && f.stripTrailingZeros().scale() <= 0 ? new BigInteger[]{f.toBigIntegerExact(), BigInteger.ONE}
                : null;
    }

    /** {@code a / b} as a reduced fraction of integers, the denominator positive. */
    private static BigInteger[] rational(BigDecimal a, BigDecimal b) {
        BigInteger[] ta = asRational(a), tb = asRational(b);
        BigInteger numerator = ta[0].multiply(tb[1]), denominator = ta[1].multiply(tb[0]);
        if (denominator.signum() < 0) { numerator = numerator.negate(); denominator = denominator.negate(); }
        BigInteger ggd = numerator.gcd(denominator);
        if (ggd.signum() != 0) { numerator = numerator.divide(ggd); denominator = denominator.divide(ggd); }
        return new BigInteger[]{numerator, denominator};
    }

    private static BigInteger[] asRational(BigDecimal x) {
        return x.scale() >= 0 ? new BigInteger[]{x.unscaledValue(), BigInteger.TEN.pow(x.scale())}
                : new BigInteger[]{x.unscaledValue().multiply(BigInteger.TEN.pow(-x.scale())), BigInteger.ONE};
    }

    private static boolean terminates(BigInteger denominator) {
        BigInteger n = denominator;
        for (BigInteger p : new BigInteger[]{BigInteger.TWO, BigInteger.valueOf(5)})
            while (n.mod(p).signum() == 0) n = n.divide(p);
        return n.equals(BigInteger.ONE);
    }

    /** The case lexeme of a reduced fraction: a decimal without trailing zeros where it terminates, else {@code p/q}. */
    private static String lexemeText(BigInteger numerator, BigInteger denominator) {
        BigInteger[] r = rational(new BigDecimal(numerator), new BigDecimal(denominator));
        if (r[1].equals(BigInteger.ONE)) return r[0].toString();
        if (terminates(r[1])) {
            BigDecimal d = new BigDecimal(r[0]).divide(new BigDecimal(r[1])).stripTrailingZeros();
            return (d.scale() < 0 ? d.setScale(0) : d).toPlainString();
        }
        return r[0] + "/" + r[1];
    }

    /** The literal content of a text value; an interpolated «expressie» is refused, since a case value is literal. */
    private String textContent(TekstWaardeContext t, int line, String what) {
        StringBuilder sb = new StringBuilder();
        for (TekstReeksDeelContext part : t.tekstReeksDeel()) {
            if (part.STRING_TEXT() == null) {
                fout(line, what + ": de tekstwaarde draagt een «expressie», en een waarde in een scenario is letterlijk");
                return null;
            }
            sb.append(part.STRING_TEXT().getText());
        }
        return sb.toString();
    }

    // ---------------- small helpers ----------------

    private Parameter parameterByName(String naam) {
        Parameter exact = model.parameters.get(naam);
        if (exact != null) return exact;
        for (var e : model.parameters.entrySet()) if (norm(e.getKey()).equals(norm(naam))) return e.getValue();
        return null;
    }

    /** The declared object-type name whose first word (or whole name) {@code label} equals, or null. */
    private String labelCollidesWithType(String label) {
        for (String naam : model.objecttypen.keySet())
            if (naam.equalsIgnoreCase(label) || naam.split("\\s+")[0].equalsIgnoreCase(label)) return naam;
        return null;
    }

    private static String src(ParserRuleContext ctx) { return DefinitionResolver.src(ctx); }

    private static String withoutArticle(String naam) {
        String n = naam == null ? "" : naam.trim();
        for (String lw : new String[]{"de ", "het ", "een "})
            if (n.toLowerCase(java.util.Locale.ROOT).startsWith(lw)) return n.substring(lw.length()).trim();
        return n;
    }

    private void fout(int line, String message) {
        model.diagnostics.add(new Diagnostic(Severity.ERROR, "scenario: " + message, line));
    }
}
