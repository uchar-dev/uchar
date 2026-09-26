package dev.uchar.regelspraak.resolver;

import dev.uchar.regelspraak.ir.Conditie;
import dev.uchar.regelspraak.ir.Default;
import dev.uchar.regelspraak.ir.ExceptionGroup;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.Geldigheid;
import dev.uchar.regelspraak.ir.ResolvedScenario;
import dev.uchar.regelspraak.ir.ResolvedScenario.*;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.Eenheid;
import dev.uchar.regelspraak.model.Rationaal;
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
 *   <li>hands the non-scenario blocks, in document order, to the one model pipeline of {@link Resolver#bouw} as a
 *       document of their own, so the model — and the IR emitted from it — is the one the production route
 *       builds for the same blocks;</li>
 *   <li>resolves the {@code Situatie} and {@code Scenario} blocks into {@link ResolvedScenario}s, typing every value
 *       against its declaration and normalising it to what a case file of the format uchar-case-v0 carries.</li>
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
    private final Map<String, SituatieDefinitieContext> situaties = new LinkedHashMap<>();
    /** Each situation's own block, validated once. */
    private final Map<String, BlokResultaat> eigenBlokken = new HashMap<>();
    /** Each situation's blocks in merge order (its bases expanded, then its own block), computed once. */
    private final Map<String, List<BlokResultaat>> uitgevouwen = new HashMap<>();

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
        RegelSpraakModel model = Resolver.bouw(productieDocument(test), tokens, syntax);
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
                else if (kind instanceof ParserRuleContext blok
                        && !(blok instanceof SituatieDefinitieContext) && !(blok instanceof ScenarioContext))
                    throw new IllegalStateException("testDocument draagt een blok dat deze route niet kent: "
                            + blok.getClass().getSimpleName() + " op regel " + blok.getStart().getLine());
            }
        return prod;
    }

    // ---------------- scenarios ----------------

    private List<ResolvedScenario> resolveScenarios() {
        List<ParseTree> kinderen = test.children == null ? List.of() : test.children;
        for (ParseTree kind : kinderen)
            if (kind instanceof SituatieDefinitieContext sd) registreerSituatie(sd);
        // every situation is expanded, used or not, so a cycle or an unknown basis never hides behind disuse
        for (String naam : situaties.keySet()) vouwUit(naam, List.of());
        List<ResolvedScenario> uit = new ArrayList<>();
        List<ScenarioContext> contexten = new ArrayList<>();
        for (ParseTree kind : kinderen)
            if (kind instanceof ScenarioContext sc) { uit.add(resolveScenario(sc)); contexten.add(sc); }
        eenIrDocument(uit, contexten);
        return uit;
    }

    /** One document lowers to one IR document beside its case files, so every scenario must carry the same forms.
     *  A scenario without a scope carries all of them. */
    private void eenIrDocument(List<ResolvedScenario> scenarios, List<ScenarioContext> contexten) {
        for (int i = 1; i < scenarios.size(); i++) {
            List<String> eerste = gedragen(scenarios.get(0).scope()), deze = gedragen(scenarios.get(i).scope());
            if (!new LinkedHashSet<>(eerste).equals(new LinkedHashSet<>(deze))) {
                fout(contexten.get(i).getStart().getLine(), "de scenario's van één document lopen tegen één "
                        + "IR-document, maar het scenario '" + scenarios.get(i).naam() + "' toetst andere regels dan het "
                        + "scenario '" + scenarios.get(0).naam() + "'; zet scenario's met een andere scope in een "
                        + "eigen document");
                return;
            }
        }
    }

    /** The normalised names of the forms a scope carries; for no scope, every named form of the model. */
    private List<String> gedragen(Scope scope) {
        if (scope != null) return scope.vormen().stream().map(dev.uchar.regelspraak.emit.CaseEmitter::norm).toList();
        List<String> alle = new ArrayList<>();
        for (Vorm v : vormen()) alle.add(norm(v.naam()));
        return alle;
    }

    private void registreerSituatie(SituatieDefinitieContext sd) {
        String geschreven = src(sd.situatieNaam());
        String naam = norm(geschreven);
        BlokResultaat blok = valideerBlok(sd.situatieBlok());   // a duplicate's block is still checked
        if (situaties.containsKey(naam)) {
            fout(sd.getStart().getLine(), "de situatie '" + geschreven + "' is meer dan eens gedeclareerd");
            return;
        }
        situaties.put(naam, sd);
        eigenBlokken.put(naam, blok);
    }

    /** The blocks situation {@code naam} contributes, in merge order: each basis expanded in written order, then its
     *  own block. A basis that names no situation, or that closes a cycle, is an ERROR at the basis. */
    private List<BlokResultaat> vouwUit(String naam, List<String> pad) {
        List<BlokResultaat> klaar = uitgevouwen.get(naam);
        if (klaar != null) return klaar;
        SituatieDefinitieContext sd = situaties.get(naam);
        List<String> hier = new ArrayList<>(pad);
        hier.add(naam);
        List<BlokResultaat> uit = new ArrayList<>();
        for (ScenarioBasisContext basis : sd.scenarioBasis()) {
            String geschreven = src(basis.situatieNaam());
            String n = norm(geschreven);
            if (!situaties.containsKey(n)) {
                fout(basis.getStart().getLine(), "de situatie '" + geschreven + "' is nergens in het document gedeclareerd");
                continue;
            }
            if (hier.contains(n)) {
                List<String> kring = new ArrayList<>();
                for (String stap : hier.subList(hier.indexOf(n), hier.size())) kring.add("'" + gedeclareerd(stap) + "'");
                kring.add("'" + gedeclareerd(n) + "'");
                fout(basis.getStart().getLine(), "de situaties vormen een kring via 'gebaseerd op': "
                        + String.join(" → ", kring));
                continue;
            }
            uit.addAll(vouwUit(n, hier));
        }
        uit.add(eigenBlokken.get(naam));
        uitgevouwen.put(naam, uit);
        return uit;
    }

    private String gedeclareerd(String genormaliseerd) {
        SituatieDefinitieContext sd = situaties.get(genormaliseerd);
        return sd == null ? genormaliseerd : src(sd.situatieNaam());
    }

    private ResolvedScenario resolveScenario(ScenarioContext sc) {
        int line = sc.getStart().getLine();
        String naam = src(sc.scenarioNaam());
        if (dev.uchar.regelspraak.emit.CaseEmitter.slug(naam).isEmpty())
            fout(line, "de naam van het scenario '" + naam + "' geeft geen bestandsnaam: zij draagt geen letter of cijfer");
        Scope scope = sc.scenarioScope() == null ? null : resolveScope(sc.scenarioScope());
        LocalDate peildatum = sc.scenarioPeildatum() == null ? null : leesPeildatum(sc.scenarioPeildatum());

        List<BlokResultaat> bronnen = new ArrayList<>();
        for (ScenarioBasisContext basis : sc.scenarioBasis()) {
            String geschreven = src(basis.situatieNaam());
            String n = norm(geschreven);
            if (!situaties.containsKey(n))
                fout(basis.getStart().getLine(), "de situatie '" + geschreven + "' is nergens in het document gedeclareerd");
            else bronnen.addAll(vouwUit(n, List.of()));
        }
        if (sc.inlineSituatie() != null) bronnen.add(valideerBlok(sc.inlineSituatie().situatieBlok()));
        if (sc.scenarioBasis().isEmpty() && sc.inlineSituatie() == null)
            fout(line, "het scenario '" + naam + "' heeft geen situatie: het is nergens op gebaseerd en draagt geen "
                    + "'Gegeven is de volgende situatie:'");

        Situatie situatie = voegSamen(bronnen);
        checkRolDoelen(situatie);
        Predicate<String> draagt = draagt(scope);
        // a written peildatum that was refused has its own ERROR, and the gate would call it missing
        if (sc.scenarioPeildatum() == null) peildatumPoort(draagt, peildatum, naam, line);
        parameterPoort(draagt, situatie, naam, line);
        List<Verwachting> verwachtingen = resolveVerwachtingen(sc.verwachting(), situatie);
        return new ResolvedScenario(naam, scope, peildatum, situatie, verwachtingen, line);
    }

    private LocalDate leesPeildatum(ScenarioPeildatumContext p) {
        String tekst = src(p.datumWaarde());
        int line = p.getStart().getLine();
        if (ExpressieBouwer.bronDatumTijd(tekst) != null) {
            fout(line, "de peildatum '" + tekst + "' draagt een tijd, maar een casebestand draagt als peildatum "
                    + "alleen een dag (asOfDate)");
            return null;
        }
        LocalDate d = ExpressieBouwer.bronDatum(tekst);
        if (d == null) fout(line, "de peildatum '" + tekst + "' is geen geldige datum");
        return d;
    }

    // ---------------- the forms of the model, and the scope ----------------

    /** One named form of the model, with what the gates read of it. */
    private record Vorm(String naam, Geldigheid geldigheid, List<Expr> exprs, List<Conditie> condities) {}

    /** Every named form of the model the emitted IR can carry or drop: rules, object and fact creations, allocations,
     *  consistencies, uniquenesses and defaults groups, each under its base name (a version suffix taken off). */
    private List<Vorm> vormen() {
        List<Vorm> uit = new ArrayList<>();
        for (var r : model.resolvedRegels)
            uit.add(new Vorm(basisNaam(r.naam()), r.geldigheid(), lijst(r.doel(), r.waarde()), lijst(r.voorwaarde())));
        for (var oc : model.objectCreaties) {
            List<Expr> exprs = new ArrayList<>();
            for (var t : oc.toekenningen()) exprs.add(t.waarde());
            uit.add(new Vorm(basisNaam(oc.naam()), oc.geldigheid(), exprs, lijst(oc.voorwaarde())));
        }
        for (var v : model.verdelingen) uit.add(new Vorm(basisNaam(v.naam()), v.geldigheid(), List.of(), lijst(v.voorwaarde())));
        for (var c : model.consistenties) uit.add(new Vorm(basisNaam(c.naam()), c.geldigheid(), List.of(), lijst(c.assertie())));
        for (var u : model.uniciteiten) uit.add(new Vorm(basisNaam(u.naam()), null, List.of(), List.of()));
        for (var u : model.uniciteitUnies) uit.add(new Vorm(basisNaam(u.naam()), null, List.of(), List.of()));
        for (var f : model.feitCreaties) uit.add(new Vorm(basisNaam(f.naam()), f.geldigheid(), List.of(), lijst(f.voorwaarde())));
        for (var pd : model.prioritizedDefaults) {
            List<Expr> exprs = new ArrayList<>();
            List<Conditie> condities = new ArrayList<>();
            verzamelDefaults(pd.defaults(), exprs, condities);
            uit.add(new Vorm(pd.naam(), pd.geldigheid(), exprs, condities));
        }
        return uit;
    }

    private static void verzamelDefaults(List<Default> defaults, List<Expr> exprs, List<Conditie> condities) {
        if (defaults == null) return;
        for (Default d : defaults) {
            if (d.consequentie() != null) exprs.add(d.consequentie());
            if (d.voorwaarde() != null) condities.add(d.voorwaarde());
            if (d.excepties() != null)
                for (ExceptionGroup g : d.excepties().leden()) verzamelDefaults(g.defaults(), exprs, condities);
        }
    }

    @SafeVarargs
    private static <T> List<T> lijst(T... xs) {
        List<T> uit = new ArrayList<>();
        for (T x : xs) if (x != null) uit.add(x);
        return uit;
    }

    /** Whether a form of that name is carried: always without a scope, else when the scope names it. */
    private static Predicate<String> draagt(Scope scope) {
        if (scope == null) return n -> true;
        Set<String> namen = new java.util.HashSet<>();
        for (String v : scope.vormen()) namen.add(norm(v));
        return n -> namen.contains(norm(n));
    }

    private Scope resolveScope(ScenarioScopeContext scope) {
        if (!model.resolvedStartpunten.isEmpty())
            fout(scope.getStart().getLine(), "een scope in een document met een startpuntbepaling wordt geweigerd: "
                    + "de bepaling en de regel die haar anker schrijft horen bij elkaar, en een scope die er één van "
                    + "laat vallen legt de tijdlijn op een ander raster");
        if (scope instanceof ScopeEnkeleRegelContext s) {
            String naam = src(s.enkeleRegelReferentie());
            return gescoped(vormenVan(naam, s.getStart().getLine()));
        }
        if (scope instanceof ScopeMeerdereRegelsContext s) {
            List<String> namen = new ArrayList<>();
            LinkedHashSet<String> vormen = new LinkedHashSet<>();
            for (RegelReferentieContext ref : s.regelReferentie()) {
                String naam = src(ref);
                namen.add(naam);
                vormen.addAll(vormenVan(naam, ref.getStart().getLine()));
            }
            return gescoped(List.copyOf(vormen));
        }
        ScopeRegelgroepContext s = (ScopeRegelgroepContext) scope;
        String groep = src(s.enkeleRegelReferentie());
        var g = model.regelGroepen.stream().filter(rg -> norm(rg.naam()).equals(norm(groep))).findFirst().orElse(null);
        if (g == null) {
            fout(s.getStart().getLine(), "de regelgroep '" + groep + "' uit de scope bestaat niet in het document");
            return gescoped(List.of());
        }
        LinkedHashSet<String> vormen = new LinkedHashSet<>();
        for (String lid : g.leden()) vormen.addAll(vormenVan(lid, s.getStart().getLine()));
        return gescoped(List.copyOf(vormen));
    }

    /** A scope over {@code vormen}, with the source lines of the blocks whose every form it leaves out. */
    private Scope gescoped(List<String> vormen) {
        Set<String> gedragen = new java.util.HashSet<>();
        for (String v : vormen) gedragen.add(norm(v));
        List<Regelbereik> weggelaten = new ArrayList<>();
        for (Blok b : blokken()) weggelaten(b, gedragen, weggelaten);
        return new Scope(vormen, List.copyOf(weggelaten));
    }

    /** A source block that yields named forms: a rule or a decision table (its forms carry its name, a table's rows
     *  its name and a number), an extension block (its defaults land in the group of its base rule, found by the
     *  source lines of the group's nodes), or a rule group (its members). */
    private record Blok(String soort, String naam, int van, int tot, List<Blok> leden) {}

    private List<Blok> blokken() {
        List<Blok> uit = new ArrayList<>();
        for (ParseTree kind : test.children == null ? List.<ParseTree>of() : test.children) {
            if (kind instanceof RegelGroepContext g) {
                List<Blok> leden = new ArrayList<>();
                for (GroepLidContext lid : g.groepLid()) {
                    if (lid.regel() != null) leden.add(blok("regel", src(lid.regel().regelNaam()), lid.regel(), List.of()));
                    if (lid.beslistabel() != null)
                        leden.add(blok("tabel", src(lid.beslistabel().beslistabelNaam()), lid.beslistabel(), List.of()));
                }
                uit.add(blok("groep", src(g.kaleNaam()), g, leden));
            } else if (kind instanceof RegelContext r) uit.add(blok("regel", src(r.regelNaam()), r, List.of()));
            else if (kind instanceof BeslistabelContext b) uit.add(blok("tabel", src(b.beslistabelNaam()), b, List.of()));
            else if (kind instanceof UitzonderingRegelContext || kind instanceof UitzonderingGroepContext)
                uit.add(blok("uitbreiding", null, (ParserRuleContext) kind, List.of()));
        }
        return uit;
    }

    private static Blok blok(String soort, String naam, ParserRuleContext ctx, List<Blok> leden) {
        int van = ctx.getStart().getLine();
        return new Blok(soort, naam, van, ctx.getStop() == null ? van : ctx.getStop().getLine(), leden);
    }

    /** Whether block {@code b} yields a carried form (TRUE), only forms the scope leaves out (FALSE) or no named form
     *  at all (null); the lines of each block that yields only left-out forms go to {@code uit}. */
    private Boolean weggelaten(Blok b, Set<String> gedragen, List<Regelbereik> uit) {
        Boolean draagt = null;
        if (b.leden().isEmpty()) {
            for (Vorm v : vormen())
                if (uitBlok(v, b)) draagt = Boolean.TRUE.equals(draagt) || gedragen.contains(norm(v.naam()));
        } else {
            List<Regelbereik> binnen = new ArrayList<>();
            for (Blok lid : b.leden()) {
                Boolean d = weggelaten(lid, gedragen, binnen);
                if (d != null) draagt = Boolean.TRUE.equals(draagt) || d;
            }
            if (!Boolean.FALSE.equals(draagt)) uit.addAll(binnen);
        }
        if (Boolean.FALSE.equals(draagt)) uit.add(new Regelbereik(b.van(), b.tot()));
        return draagt;
    }

    /** Whether form {@code v} comes from block {@code b}: by name for a rule and a decision table, and for an
     *  extension block by the source lines of the form's nodes. A table row's nodes are read in a stream of their own,
     *  so their lines are no document lines, and the name is what places them. */
    private static boolean uitBlok(Vorm v, Blok b) {
        return switch (b.soort()) {
            case "regel" -> norm(v.naam()).equals(norm(b.naam()));
            case "tabel" -> norm(v.naam()).matches(java.util.regex.Pattern.quote(norm(b.naam())) + " [0-9]+");
            default -> regelsVan(v).stream().anyMatch(r -> r >= b.van() && r <= b.tot());
        };
    }

    /** The source lines of the nodes of form {@code v}. */
    private static Set<Integer> regelsVan(Vorm v) {
        Set<Integer> uit = new java.util.HashSet<>();
        Predicate<Expr> vang = x -> {
            if (x.bron() != null && x.bron().line() > 0) uit.add(x.bron().line());
            return false;
        };
        v.exprs().forEach(e -> Expr.bevat(e, vang));
        v.condities().forEach(c -> regelsVan(c, vang, uit));
        return uit;
    }

    private static void regelsVan(Conditie c, Predicate<Expr> vang, Set<Integer> uit) {
        if (c == null) return;
        if (c.bron() != null && c.bron().line() > 0) uit.add(c.bron().line());
        Conditie.bevatOperand(c, vang);
        if (c instanceof Conditie.Samengesteld s) s.onderdelen().forEach(o -> regelsVan(o, vang, uit));
        if (c instanceof Conditie.Kwantorvouw k) { regelsVan(k.filter(), vang, uit); regelsVan(k.toets(), vang, uit); }
    }

    /** The model forms a scope name carries: the named form itself, or every row of the decision table it names. */
    private List<String> vormenVan(String naam, int line) {
        String n = norm(naam);
        List<String> uit = new ArrayList<>();
        for (Vorm v : vormen()) if (norm(v.naam()).equals(n) && !uit.contains(v.naam())) uit.add(v.naam());
        if (!uit.isEmpty()) return uit;
        if (tabelNamen().contains(n)) {
            for (Vorm v : vormen())
                if (norm(v.naam()).matches(java.util.regex.Pattern.quote(n) + " [0-9]+") && !uit.contains(v.naam()))
                    uit.add(v.naam());
            return uit;
        }
        if (model.regelGroepen.stream().anyMatch(g -> norm(g.naam()).equals(n)))
            fout(line, "'" + naam + "' uit de scope is een regelgroep; schrijf 'toetst de regelgroep " + naam + "'");
        else if (uitzonderingNamen().contains(n))
            fout(line, "de regel '" + naam + "' uit de scope is opgegaan in een defaultsgroep; de scope noemt die groep "
                    + "bij de naam van haar basisregel");
        else fout(line, "de regel '" + naam + "' uit de scope bestaat niet in het document");
        return uit;
    }

    private Set<String> tabelNamen() {
        Set<String> uit = new java.util.HashSet<>();
        for (ParseTree kind : test.children == null ? List.<ParseTree>of() : test.children) {
            if (kind instanceof BeslistabelContext b) uit.add(norm(src(b.beslistabelNaam())));
            if (kind instanceof RegelGroepContext g)
                for (GroepLidContext lid : g.groepLid())
                    if (lid.beslistabel() != null) uit.add(norm(src(lid.beslistabel().beslistabelNaam())));
        }
        return uit;
    }

    /** The names of the dialect's exception blocks and of the members of its unordered blocks: rules the lowering
     *  lifts into a defaults group, which the IR carries under the group's name. */
    private Set<String> uitzonderingNamen() {
        Set<String> uit = new java.util.HashSet<>();
        for (ParseTree kind : test.children == null ? List.<ParseTree>of() : test.children) {
            if (kind instanceof UitzonderingRegelContext u) uit.add(norm(src(u.regelNaam())));
            if (kind instanceof UitzonderingGroepContext g)
                for (RegelContext r : g.regel()) uit.add(norm(src(r.regelNaam())));
        }
        return uit;
    }

    // ---------------- the two gates ----------------

    /** A carried form that reads the evaluation date, or holds only within a validity window, needs a peildatum. */
    private void peildatumPoort(Predicate<String> draagt, LocalDate peildatum, String scenario, int line) {
        if (peildatum != null) return;
        for (Vorm v : vormen()) {
            if (!draagt.test(v.naam())) continue;
            boolean venster = v.geldigheid() != null && !v.geldigheid().altijd();
            boolean leest = v.exprs().stream().anyMatch(e -> Expr.bevat(e, Expr::isRekenConstante))
                    || v.condities().stream().anyMatch(c -> Conditie.bevatOperand(c, Expr::isRekenConstante));
            if (venster || leest) {
                fout(line, "de regel '" + v.naam() + "' " + (venster ? "heeft een geldigheidsvenster" : "leest de rekendatum")
                        + ", maar het scenario '" + scenario + "' heeft geen peildatum (voeg 'met peildatum <datum>' toe)");
                return;
            }
        }
    }

    /** Every parameter a carried form reads must have a value in the merged situation. */
    private void parameterPoort(Predicate<String> draagt, Situatie situatie, String scenario, int line) {
        Set<String> gezet = new java.util.HashSet<>();
        for (ParameterWaarde p : situatie.parameters()) gezet.add(norm(p.parameter()));
        Set<String> gemeld = new java.util.HashSet<>();
        for (Vorm v : vormen()) {
            if (!draagt.test(v.naam())) continue;
            LinkedHashSet<String> gelezen = new LinkedHashSet<>();
            Predicate<Expr> vang = x -> { if (x instanceof Expr.ParameterRef pr) gelezen.add(pr.parameter().naam()); return false; };
            v.exprs().forEach(e -> Expr.bevat(e, vang));
            v.condities().forEach(c -> Conditie.bevatOperand(c, vang));
            for (String p : gelezen)
                if (!gezet.contains(norm(p)) && gemeld.add(norm(p)))
                    fout(line, "de regel '" + v.naam() + "' leest de parameter '" + p + "', die in de situatie van het "
                            + "scenario '" + scenario + "' geen waarde heeft");
        }
    }

    // ---------------- situation blocks ----------------

    /** One validated block: its instance fragments and its parameter values, in written order. */
    private record BlokResultaat(List<InstantieFragment> instanties, List<ParameterWaarde> parameters) {}
    /** One block's contribution to a labelled instance. */
    private record InstantieFragment(String label, Objecttype objecttype, List<AttribuutWaarde> attributen,
                                     List<RolToewijzing> rollen, List<KenmerkWaarde> kenmerken, int line) {}
    /** A role assignment keyed by its declared role, so a later assignment of the same role replaces it. */
    private record RolToewijzing(String rol, RolWaarde waarde) {}

    private BlokResultaat valideerBlok(SituatieBlokContext blok) {
        List<InstantieFragment> instanties = new ArrayList<>();
        List<ParameterWaarde> parameters = new ArrayList<>();
        Map<String, List<Segment>> parameterTijdlijnen = new LinkedHashMap<>();
        for (SituatieOnderdeelContext ond : blok.situatieOnderdeel()) {
            if (ond instanceof InstantieOnderdeelContext io) instanties.add(valideerInstantie(io.instantie()));
            else if (ond instanceof ParameterWaardeOnderdeelContext po) {
                ParameterWaarde pw = valideerParameter(po.parameterWaarde(), parameters, parameterTijdlijnen);
                if (pw != null) parameters.add(pw);
            }
        }
        return new BlokResultaat(instanties, parameters);
    }

    private InstantieFragment valideerInstantie(InstantieContext inst) {
        int line = inst.getStart().getLine();
        String typeNaam = src(inst.objectTypeNaam());
        String label = inst.UPPER_ID().getText();
        Objecttype ot = model.objecttypeOpNaam(typeNaam);
        if (ot == null) fout(line, "het objecttype '" + typeNaam + "' is niet gedeclareerd");
        String botsing = labelBotstMetType(label);
        if (botsing != null)
            fout(line, "het label '" + label + "' botst met (het eerste woord van) de objecttypenaam '" + botsing + "'");

        List<AttribuutWaarde> attributen = new ArrayList<>();
        Map<String, List<Segment>> tijdlijnen = new LinkedHashMap<>();
        Set<String> vast = new java.util.HashSet<>();
        List<RolToewijzing> rollen = new ArrayList<>();
        List<KenmerkWaarde> kenmerken = new ArrayList<>();
        Map<String, List<Periode>> perioden = new LinkedHashMap<>();
        for (ToewijzingContext t : inst.toewijzing()) {
            if (t instanceof AttribuutToewijzingContext a) attribuut(a, ot, attributen, tijdlijnen, vast);
            else if (t instanceof RolToOneToewijzingContext r)
                rol(ot, src(r.toewijzingNaam()), List.of(r.UPPER_ID().getText()), false, r.getStart().getLine(), rollen);
            else if (t instanceof RolToManyToewijzingContext r) {
                List<String> labels = new ArrayList<>();
                for (var u : r.UPPER_ID()) labels.add(u.getText());
                rol(ot, src(r.toewijzingNaam()), labels, true, r.getStart().getLine(), rollen);
            } else if (t instanceof KenmerkToewijzingContext k) kenmerk(k, ot, kenmerken, perioden);
        }
        return new InstantieFragment(label, ot, attributen, rollen, kenmerken, line);
    }

    private void attribuut(AttribuutToewijzingContext a, Objecttype ot, List<AttribuutWaarde> attributen,
                           Map<String, List<Segment>> tijdlijnen, Set<String> vast) {
        int line = a.getStart().getLine();
        String geschreven = zonderLidwoord(src(a.toewijzingNaam()));
        if (ot == null) return;                                        // unknown type: reported at the opener
        String attr = ot.attribuutNaam(geschreven);
        if (attr == null) {
            fout(line, "het objecttype '" + ot.naam + "' kent geen attribuut '" + geschreven + "'");
            return;
        }
        if (!ot.dimensiesVan(attr).isEmpty()) {
            fout(line, "het attribuut '" + attr + "' is gedimensioneerd, en een casebestand draagt geen "
                    + "dimensiecoördinaten");
            return;
        }
        Datatype dt = ot.attribuut(attr);
        Waarde w = leesEnTypeer(a.situatieWaarde(), dt, line, "het attribuut '" + attr + "'");
        if (a.periode() == null) {
            if (tijdlijnen.containsKey(norm(attr))) {
                fout(line, "het attribuut '" + attr + "' krijgt in dit blok zowel perioden als een vaste waarde");
                return;
            }
            vast.add(norm(attr));
            if (w != null) attributen.add(new AttribuutWaarde(attr, w, null, line));
            return;
        }
        if (ot.granulariteitVan(attr) == null) {
            fout(line, "het attribuut '" + attr + "' is niet tijdsafhankelijk; een periode ('van … tot …') hoort bij "
                    + "een attribuut met een tijdlijn");
            return;
        }
        if (vast.contains(norm(attr))) {
            fout(line, "het attribuut '" + attr + "' krijgt in dit blok zowel een vaste waarde als perioden");
            return;
        }
        Periode p = leesPeriode(a.periode());
        if (p == null || w == null || !opRaster(p, ot, attr, "het attribuut '" + attr + "'", line)) return;
        List<Segment> tl = tijdlijnen.get(norm(attr));
        if (tl == null) {
            tl = new ArrayList<>();
            tijdlijnen.put(norm(attr), tl);
            attributen.add(new AttribuutWaarde(attr, null, tl, line));
        }
        if (volgtOp(tl.isEmpty() ? null : tl.get(tl.size() - 1).tot(), p, line)) tl.add(new Segment(p.van(), p.tot(), w));
    }

    private void kenmerk(KenmerkToewijzingContext k, Objecttype ot, List<KenmerkWaarde> kenmerken,
                         Map<String, List<Periode>> perioden) {
        int line = k.getStart().getLine();
        boolean bezittelijk = k.HEEFT() != null;
        String geschreven = zonderLidwoord(src(k.kaleNaam()));
        if (ot == null) return;
        String kenmerk = KenmerkMatcher.vind(ot, geschreven);
        if (kenmerk == null) {
            fout(line, "het objecttype '" + ot.naam + "' kent geen kenmerk '" + geschreven + "'");
            return;
        }
        if (!werkwoordPast(ot, kenmerk, bezittelijk, line)) return;
        boolean temporeel = ot.kenmerkGranulariteitVan(kenmerk) != null;
        if (k.periode() == null) {
            if (temporeel) {
                fout(line, "het kenmerk '" + kenmerk + "' is tijdsafhankelijk; een casebestand levert het als perioden "
                        + "('is " + kenmerk + " van <datum> tot <datum>'), niet als vaste waarde");
                return;
            }
            kenmerken.add(new KenmerkWaarde(kenmerk, null, line));
            return;
        }
        if (!temporeel) {
            fout(line, "het kenmerk '" + kenmerk + "' is niet tijdsafhankelijk; een periode ('van … tot …') hoort bij "
                    + "een kenmerk met een tijdlijn");
            return;
        }
        Periode p = leesPeriode(k.periode());
        if (p == null) return;
        List<Periode> lijst = perioden.get(norm(kenmerk));
        if (lijst == null) {
            lijst = new ArrayList<>();
            perioden.put(norm(kenmerk), lijst);
            kenmerken.add(new KenmerkWaarde(kenmerk, lijst, line));
        }
        if (volgtOp(lijst.isEmpty() ? null : lijst.get(lijst.size() - 1).tot(), p, line)) lijst.add(p);
    }

    /** The verb follows the characteristic's kind (§3.5): 'heeft' for a possessive one, 'is' for the others. */
    private boolean werkwoordPast(Objecttype ot, String kenmerk, boolean heeft, int line) {
        boolean bezittelijk = ot.kenmerkSoort(kenmerk) == Objecttype.KenmerkSoort.BEZITTELIJK;
        if (bezittelijk == heeft) return true;
        fout(line, bezittelijk
                ? "het kenmerk '" + kenmerk + "' is bezittelijk; schrijf 'heeft " + kenmerk + "'"
                : "het kenmerk '" + kenmerk + "' is niet bezittelijk; schrijf 'is " + kenmerk + "'");
        return false;
    }

    /** The role the written name {@code geschreven} names from an instance of {@code ot}: the other role of a fact
     *  type in which {@code ot} fills a role, matched by its declared name or plural — the reading of the case
     *  readers, so the key the case file carries resolves there to this role. */
    private void rol(Objecttype ot, String geschrevenMetLidwoord, List<String> labels, boolean zijnVorm, int line,
                     List<RolToewijzing> rollen) {
        if (ot == null) return;
        String geschreven = zonderLidwoord(geschrevenMetLidwoord);
        List<Rol[]> treffers = new ArrayList<>();
        for (Feittype ft : model.feittypen.values()) {
            if (ft.rollen.size() == 2) {
                Rol a = ft.rollen.get(0), b = ft.rollen.get(1);
                if (vult(ot, a) && noemt(b, geschreven)) treffers.add(new Rol[]{b});
                if (vult(ot, b) && noemt(a, geschreven)) treffers.add(new Rol[]{a});
            } else if (ft.rollen.size() == 1 && ft.wederkerig) {
                Rol r = ft.rollen.get(0);
                if (vult(ot, r) && noemt(r, geschreven)) treffers.add(new Rol[]{r});
            }
        }
        if (treffers.isEmpty()) {
            fout(line, "het objecttype '" + ot.naam + "' kent geen rol '" + geschreven + "'");
            return;
        }
        if (treffers.size() > 1) {
            fout(line, "de rol '" + geschreven + "' vanaf het objecttype '" + ot.naam + "' is niet eenduidig: meer dan "
                    + "één feittype kent haar");
            return;
        }
        Rol ver = treffers.get(0)[0];
        if (ver.meervoudig() && !zijnVorm) {
            fout(line, "de rol '" + geschreven + "' is een naar-veel-rol; schrijf de 'zijn'-lijstvorm ('de "
                    + geschreven + " zijn …')");
            return;
        }
        if (!ver.meervoudig() && zijnVorm) {
            fout(line, "de rol '" + geschreven + "' is een naar-één-rol; schrijf de 'is'-vorm ('de " + geschreven
                    + " is …')");
            return;
        }
        String sleutel = ver.meervoud() != null && norm(ver.meervoud()).equals(norm(geschreven)) ? ver.meervoud() : ver.naam();
        rollen.add(new RolToewijzing(ver.naam(), new RolWaarde(sleutel, model.objecttypeOpNaam(ver.objecttypeNaam()),
                List.copyOf(labels), ver.meervoudig(), line)));
    }

    private boolean vult(Objecttype ot, Rol r) {
        Objecttype t = model.objecttypeOpNaam(r.objecttypeNaam());
        return t != null && ot.isEen(t);
    }

    private static boolean noemt(Rol r, String geschreven) {
        return norm(r.naam()).equals(norm(geschreven)) || r.meervoud() != null && norm(r.meervoud()).equals(norm(geschreven));
    }

    private ParameterWaarde valideerParameter(ParameterWaardeContext pw, List<ParameterWaarde> parameters,
                                              Map<String, List<Segment>> tijdlijnen) {
        int line = pw.getStart().getLine();
        String geschreven = src(pw.parameterNaam());
        Parameter p = parameterOpNaam(zonderLidwoord(geschreven));
        if (p == null) {
            fout(line, "de parameter '" + geschreven + "' is niet gedeclareerd");
            return null;
        }
        Waarde w = leesEnTypeer(pw.situatieWaarde(), p.datatype(), line, "de parameter '" + p.naam() + "'");
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
        Periode per = leesPeriode(pw.periode());
        if (per == null || w == null
                || !opRaster(per, p.granulariteit(), 1, p.vastStartpunt(), false, "de parameter '" + p.naam() + "'", line))
            return null;
        List<Segment> tl = tijdlijnen.get(norm(p.naam()));
        boolean nieuw = tl == null;
        if (nieuw) { tl = new ArrayList<>(); tijdlijnen.put(norm(p.naam()), tl); }
        if (volgtOp(tl.isEmpty() ? null : tl.get(tl.size() - 1).tot(), per, line)) tl.add(new Segment(per.van(), per.tot(), w));
        return nieuw ? new ParameterWaarde(p.naam(), null, tl, line) : null;
    }

    // ---------------- periods ----------------

    private Periode leesPeriode(PeriodeContext p) {
        int line = p.getStart().getLine();
        LocalDate[] grenzen = new LocalDate[2];
        for (int i = 0; i < 2; i++) {
            String tekst = p.DATE(i).getText().replaceAll("\\s+", " ").trim();
            if (ExpressieBouwer.bronDatumTijd(tekst) != null) {
                fout(line, "de periodegrens '" + tekst + "' draagt een tijd, maar een periode telt in hele dagen");
                return null;
            }
            grenzen[i] = ExpressieBouwer.bronDatum(tekst);
            if (grenzen[i] == null) {
                fout(line, "de periodegrens '" + tekst + "' is geen geldige datum");
                return null;
            }
        }
        if (!grenzen[0].isBefore(grenzen[1])) {
            fout(line, "de periode van " + grenzen[0] + " tot " + grenzen[1] + " is leeg of loopt terug");
            return null;
        }
        return new Periode(grenzen[0], grenzen[1]);
    }

    /** Whether period {@code p} may follow a period that ended at {@code vorigeTot}: the periods of one timeline
     *  stand in order and do not overlap (a gap is allowed). */
    private boolean volgtOp(LocalDate vorigeTot, Periode p, int line) {
        if (vorigeTot == null || !p.van().isBefore(vorigeTot)) return true;
        fout(line, "de periode van " + p.van() + " tot " + p.tot() + " begint vóór het einde van de vorige ("
                + vorigeTot + "); de perioden van één tijdlijn staan in volgorde en overlappen niet");
        return false;
    }

    /** Whether both bounds of {@code p} sit on the grid of attribute {@code attr} of {@code ot}: its granularity,
     *  count and fixed start point, as its facet declares them. */
    private boolean opRaster(Periode p, Objecttype ot, String attr, String wat, int line) {
        Integer aantal = ot.tijdlijnAantalVan(attr);
        return opRaster(p, ot.granulariteitVan(attr), aantal == null ? 1 : aantal, ot.vastStartpuntVan(attr),
                model.heeftVariabelStartpunt(ot, attr), wat, line);
    }

    /** Every case reader holds the segments of a timeline to the walls of its declared grid (the case format's
     *  timeline value), so a bound off the grid is an ERROR here, per bound, rather than a case file those readers
     *  refuse. A variable start lays its grid per object from an anchor a rule writes, and a grid that lays no wall
     *  a day can be held against ({@link dev.uchar.regelspraak.kern.VastStartpunt#opWand}) has nothing to hold a
     *  bound to: both are left to the readers. */
    private boolean opRaster(Periode p, RegelSpraakModel.Granulariteit g, int aantal, String vast, boolean variabel, String wat,
                             int line) {
        if (variabel) return true;
        boolean goed = true;
        for (LocalDate grens : List.of(p.van(), p.tot()))
            if (Boolean.FALSE.equals(dev.uchar.regelspraak.kern.VastStartpunt.opWand(g, aantal, vast, grens))) {
                fout(line, "de periodegrens " + grens + " van " + wat + " ligt niet op het " + rasterNaam(g, aantal)
                        + " van de declaratie" + (vast == null ? "" : " (vast startpunt " + vast + ")")
                        + "; een casebestand levert een tijdlijn in de perioden van dat raster");
                goed = false;
            }
        return goed;
    }

    private static String rasterNaam(RegelSpraakModel.Granulariteit g, int aantal) {
        String[] woord = switch (g) {
            case DAG -> new String[]{"dag", "dagen"};
            case WEEK -> new String[]{"week", "weken"};
            case MAAND -> new String[]{"maand", "maanden"};
            case KWARTAAL -> new String[]{"kwartaal", "kwartalen"};
            case JAAR -> new String[]{"jaar", "jaren"};
        };
        return aantal == 1 ? woord[0] + "raster" : "raster van " + aantal + " " + woord[1];
    }

    // ---------------- the merge ----------------

    /** Merge the validated blocks in order into one situation: one label is one instance (its assignments accumulate,
     *  a later assignment of the same attribute, role or characteristic replacing the earlier one in place), and a
     *  later parameter value replaces an earlier one in place. */
    private Situatie voegSamen(List<BlokResultaat> bronnen) {
        Map<String, InstantieBouwer> instanties = new LinkedHashMap<>();
        Map<String, ParameterWaarde> parameters = new LinkedHashMap<>();
        for (BlokResultaat blok : bronnen) {
            for (InstantieFragment frag : blok.instanties()) {
                InstantieBouwer bouwer = instanties.get(frag.label());
                if (bouwer == null) {
                    bouwer = new InstantieBouwer(frag.label(), frag.objecttype());
                    instanties.put(frag.label(), bouwer);
                } else if (frag.objecttype() != null && bouwer.objecttype != null && bouwer.objecttype != frag.objecttype()) {
                    fout(frag.line(), "het label '" + frag.label() + "' is eerder gedeclareerd als '"
                            + bouwer.objecttype.naam + "', hier als '" + frag.objecttype().naam + "'");
                }
                frag.attributen().forEach(bouwer::zetAttribuut);
                frag.rollen().forEach(bouwer::zetRol);
                frag.kenmerken().forEach(bouwer::zetKenmerk);
            }
            for (ParameterWaarde pw : blok.parameters()) parameters.put(norm(pw.parameter()), pw);
        }
        List<Instantie> uit = new ArrayList<>();
        for (InstantieBouwer b : instanties.values()) uit.add(b.bouw());
        return new Situatie(uit, new ArrayList<>(parameters.values()));
    }

    /** After the merge: every role target names a label of the merged situation whose type fills the role — a
     *  role in a based-on situation may point at an inline instance, so this runs on the merged result. */
    private void checkRolDoelen(Situatie situatie) {
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
    private static final class InstantieBouwer {
        final String label;
        final Objecttype objecttype;
        private final Map<String, AttribuutWaarde> attributen = new LinkedHashMap<>();
        private final Map<String, RolWaarde> rollen = new LinkedHashMap<>();
        private final Map<String, KenmerkWaarde> kenmerken = new LinkedHashMap<>();

        InstantieBouwer(String label, Objecttype objecttype) { this.label = label; this.objecttype = objecttype; }

        void zetAttribuut(AttribuutWaarde w) { attributen.put(norm(w.attribuut()), w); }
        void zetRol(RolToewijzing t) { rollen.put(norm(t.rol()), t.waarde()); }
        void zetKenmerk(KenmerkWaarde w) { kenmerken.put(norm(w.kenmerk()), w); }

        Instantie bouw() {
            return new Instantie(label, objecttype, new ArrayList<>(attributen.values()),
                    new ArrayList<>(rollen.values()), new ArrayList<>(kenmerken.values()));
        }
    }

    // ---------------- expectations ----------------

    private List<Verwachting> resolveVerwachtingen(List<VerwachtingContext> bullets, Situatie situatie) {
        List<Verwachting> uit = new ArrayList<>();
        Map<String, Verwachting> tijdlijnen = new HashMap<>();          // label|attribuut -> its one entry
        Map<String, Boolean> altijdLeeg = new HashMap<>();
        for (VerwachtingContext v : bullets) {
            int line = v.getStart().getLine();
            if (v instanceof VerwachtAttribuutLeegContext a) {
                String label = a.UPPER_ID().getText();
                String attr = verwachtAttribuut(label, zonderLidwoord(src(a.verwachtingNaam())), situatie, line);
                if (attr != null)
                    uit.add(new Verwachting(VerwachtingSoort.ATTRIBUUT_LEEG, label, attr, null, null, null, null, line));
            } else if (v instanceof VerwachtTijdlijnLeegContext a) {
                String label = a.UPPER_ID().getText();
                String attr = verwachtAttribuut(label, zonderLidwoord(src(a.verwachtingNaam())), situatie, line);
                if (attr == null || !temporeelAttribuut(label, attr, situatie, line)) continue;
                String sleutel = label + "|" + norm(attr);
                if (tijdlijnen.containsKey(sleutel)) {
                    fout(line, "de verwachting dat '" + attr + "' van " + label + " altijd leeg is, spreekt de "
                            + "perioden ervoor tegen");
                    continue;
                }
                Verwachting t = new Verwachting(VerwachtingSoort.ATTRIBUUT_TIJDLIJN, label, attr, null, null,
                        new ArrayList<>(), null, line);
                tijdlijnen.put(sleutel, t);
                altijdLeeg.put(sleutel, true);
                uit.add(t);
            } else if (v instanceof VerwachtAttribuutWaardeContext a) {
                String label = a.UPPER_ID().getText();
                String attr = verwachtAttribuut(label, zonderLidwoord(src(a.verwachtingNaam())), situatie, line);
                if (attr == null) continue;
                Datatype dt = situatie.instantie(label).objecttype().attribuut(attr);
                Waarde w = leesEnTypeer(a.situatieWaarde(), dt, line, "de verwachting voor '" + attr + "'");
                if (a.periode() == null) {
                    if (w != null)
                        uit.add(new Verwachting(VerwachtingSoort.ATTRIBUUT_WAARDE, label, attr, null, w, null, null, line));
                    continue;
                }
                if (!temporeelAttribuut(label, attr, situatie, line)) continue;
                Periode p = leesPeriode(a.periode());
                if (p == null || w == null
                        || !opRaster(p, situatie.instantie(label).objecttype(), attr, "de verwachting voor '" + attr + "'", line))
                    continue;
                String sleutel = label + "|" + norm(attr);
                if (Boolean.TRUE.equals(altijdLeeg.get(sleutel))) {
                    fout(line, "de periode voor '" + attr + "' van " + label + " spreekt de verwachting tegen dat het "
                            + "attribuut altijd leeg is");
                    continue;
                }
                Verwachting t = tijdlijnen.get(sleutel);
                if (t == null) {
                    t = new Verwachting(VerwachtingSoort.ATTRIBUUT_TIJDLIJN, label, attr, null, null, new ArrayList<>(),
                            null, line);
                    tijdlijnen.put(sleutel, t);
                    uit.add(t);
                }
                List<Segment> tl = t.tijdlijn();
                if (volgtOp(tl.isEmpty() ? null : tl.get(tl.size() - 1).tot(), p, line)) tl.add(new Segment(p.van(), p.tot(), w));
            } else if (v instanceof VerwachtKenmerkContext k) {
                Verwachting r = verwachtKenmerk(k.UPPER_ID().getText(), zonderLidwoord(src(k.kaleNaam())), false,
                        situatie, line);
                if (r != null) uit.add(r);
            } else if (v instanceof VerwachtKenmerkHeeftContext k) {
                Verwachting r = verwachtKenmerk(k.UPPER_ID().getText(), zonderLidwoord(src(k.kaleNaam())), true,
                        situatie, line);
                if (r != null) uit.add(r);
            } else if (v instanceof VerwachtInconsistentContext c) {
                String regel = null, label = null;
                InconsistentieSleutelContext s = c.inconsistentieSleutel();
                if (s != null) {
                    regel = tekstInhoud(s.tekstWaarde(), line, "de regel van de inconsistentie");
                    if (regel != null && !bekendeInconsistentieRegel(regel)) {
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
                    uit.add(new Verwachting(VerwachtingSoort.INCONSISTENT, label, null, null, null, null, regel, line));
            } else if (v instanceof VerwachtConsistentContext c) {
                uit.add(new Verwachting(VerwachtingSoort.CONSISTENT, null, null, null, null, null, null, line));
            } else {
                // error recovery leaves a bare VerwachtingContext; the syntax error is reported already
                fout(line, "de verwachting '" + src(v) + "' is syntactisch niet herkend");
            }
        }
        return uit;
    }

    /** The declared attribute {@code geschreven} names on the instance labelled {@code label}; null after an ERROR. */
    private String verwachtAttribuut(String label, String geschreven, Situatie situatie, int line) {
        Instantie i = situatie.instantie(label);
        if (i == null) {
            fout(line, "het label '" + label + "' in de verwachting is nergens in de situatie gedeclareerd");
            return null;
        }
        if (i.objecttype() == null) return null;                      // unknown type: reported at the opener
        String attr = i.objecttype().attribuutNaam(geschreven);
        if (attr == null) {
            fout(line, "het objecttype '" + i.objecttype().naam + "' kent geen attribuut '" + geschreven + "'");
            return null;
        }
        if (!i.objecttype().dimensiesVan(attr).isEmpty()) {
            fout(line, "het attribuut '" + attr + "' is gedimensioneerd, en een casebestand draagt geen "
                    + "dimensiecoördinaten");
            return null;
        }
        return attr;
    }

    private boolean temporeelAttribuut(String label, String attr, Situatie situatie, int line) {
        if (situatie.instantie(label).objecttype().granulariteitVan(attr) != null) return true;
        fout(line, "het attribuut '" + attr + "' is niet tijdsafhankelijk; een tijdlijnverwachting hoort bij een "
                + "attribuut met een tijdlijn");
        return false;
    }

    private Verwachting verwachtKenmerk(String label, String geschreven, boolean heeft, Situatie situatie, int line) {
        // "geen <kenmerk>" and "niet <kenmerk>" are the absent form: the grammar keeps the word inside the kaleNaam
        String klein = geschreven.toLowerCase(java.util.Locale.ROOT);
        boolean afwezig = klein.startsWith("geen ") || klein.startsWith("niet ");
        String naam = afwezig ? geschreven.substring(5).trim() : geschreven;
        Instantie i = situatie.instantie(label);
        if (i == null) {
            fout(line, "het label '" + label + "' in de verwachting is nergens in de situatie gedeclareerd");
            return null;
        }
        if (i.objecttype() == null) return null;
        String kenmerk = KenmerkMatcher.vind(i.objecttype(), zonderLidwoord(naam));
        if (kenmerk == null) {
            fout(line, "het objecttype '" + i.objecttype().naam + "' kent geen kenmerk '" + naam + "'");
            return null;
        }
        if (!werkwoordPast(i.objecttype(), kenmerk, heeft, line)) return null;
        return new Verwachting(afwezig ? VerwachtingSoort.KENMERK_AFWEZIG : VerwachtingSoort.KENMERK_AANWEZIG,
                label, null, kenmerk, null, null, null, line);
    }

    /** A rule an inconsistency may name: a named form of the model, or a conflict channel — a target form
     *  {@code <Objecttype>.<member>}, optionally with a default index path {@code /<n>[/<n>…]}. */
    private boolean bekendeInconsistentieRegel(String regel) {
        String n = norm(regel);
        for (Vorm v : vormen()) if (norm(v.naam()).equals(n)) return true;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("([^./]+)\\.([^/]+)(/[0-9]+)*").matcher(regel.trim());
        if (!m.matches()) return false;
        Objecttype ot = model.objecttypeOpNaam(m.group(1).trim());
        return ot != null && ot.heeftLid(m.group(2).trim());
    }

    // ---------------- values ----------------

    /** Read {@code ctx}, type it against {@code doel} and normalise it to its case form; null after an ERROR. */
    private Waarde leesEnTypeer(SituatieWaardeContext ctx, Datatype doel, int line, String wat) {
        if (ctx instanceof GetalSituatieWaardeContext g) return getal(g.getalWaarde(), doel, line, wat);
        if (ctx instanceof DatumSituatieWaardeContext d) {
            String tekst = src(d.datumWaarde());
            LocalDateTime dt = ExpressieBouwer.bronDatumTijd(tekst);
            LocalDate dd = dt == null ? ExpressieBouwer.bronDatum(tekst) : null;
            if (dt == null && dd == null) {
                fout(line, wat + ": de datum '" + tekst + "' is geen geldige datum");
                return null;
            }
            Datatype ruw = dt != null ? new Datatype.DatumTijd() : new Datatype.DatumInDagen();
            if (!toekenbaar(doel, ruw, line, wat)) return null;
            return dt != null ? Waarde.vanDatumTijd(dt) : Waarde.vanDatum(dd);
        }
        if (ctx instanceof BooleanSituatieWaardeContext b) {
            if (!toekenbaar(doel, new Datatype.Booleaan(), line, wat)) return null;
            return Waarde.vanBoolean(b.booleanWaarde().WAAR() != null);
        }
        if (ctx instanceof EnumSituatieWaardeContext e) {
            String label = dev.uchar.regelspraak.kern.Literalen.enumLabel(e.ENUM_LITERAL().getText());
            if (!(doel instanceof Datatype.Enumeratie en)) {
                if (doel != null && !doel.isOnbekend())
                    fout(line, wat + ": een enumeratiewaarde ('" + label + "') past niet op een " + doel.naam());
                return null;
            }
            for (String w : en.waarden()) if (w.equalsIgnoreCase(label)) return Waarde.vanEnum(w);
            fout(line, wat + ": '" + label + "' is geen waarde van de enumeratie " + en.domein() + " ("
                    + String.join(", ", en.waarden()) + ")");
            return null;
        }
        if (ctx instanceof TekstSituatieWaardeContext t) {
            String tekst = tekstInhoud(t.tekstWaarde(), line, wat);
            if (tekst == null || !toekenbaar(doel, new Datatype.Tekst(), line, wat)) return null;
            return Waarde.vanTekst(tekst);
        }
        fout(line, wat + ": de waarde '" + src(ctx) + "' is niet gelezen");
        return null;
    }

    private boolean toekenbaar(Datatype doel, Datatype ruw, int line, String wat) {
        if (doel == null || doel.isOnbekend()) return true;
        String f = Typering.toekenning(doel, ruw);
        if (f == null) return true;
        fout(line, wat + ": " + f);
        return false;
    }

    /** A number: its exact value in the unit written, typed against {@code doel}, restated exactly in the declared
     *  unit. Without a restatement the lexeme is the emitter's for the written number
     *  ({@link dev.uchar.regelspraak.emit.IrEmitter#getalLexeem}) — a decimal its written precision (2400,00 stays
     *  2400.00), a fraction reduced (6/8 is 3/4) and a whole number where it reduces to one (2_0/5 is 2); a restated
     *  value is a terminating decimal without trailing zeros, or else a reduced fraction. */
    private Waarde getal(GetalWaardeContext g, Datatype doel, int line, String wat) {
        String tekst = g.NUMBER().getText();
        BigDecimal[] r = Rationaal.van(tekst);
        if (r == null) {
            fout(line, wat + ": het getal '" + tekst + "' is niet gelezen");
            return null;
        }
        BigInteger[] breuk = breuk(r[0], r[1]);
        int dec = r[1].compareTo(BigDecimal.ONE) == 0 ? Math.max(0, r[0].stripTrailingZeros().scale())
                : eindigt(breuk[1]) ? Math.max(0, new BigDecimal(breuk[0]).divide(new BigDecimal(breuk[1])).stripTrailingZeros().scale())
                : 16;
        boolean geheel = dec == 0;
        Datatype ruw;
        Eenheid bronE = null;
        if (g.PERCENT_SIGN() != null) ruw = new Datatype.Percentage(geheel, dec);
        else {
            bronE = g.eenheidExpressie() != null ? DefinitionResolver.eenheidVan(model, g.eenheidExpressie()) : null;
            ruw = new Datatype.Numeriek(geheel, dec, Datatype.Teken.ONBEPAALD, bronE);
        }
        if (!toekenbaar(doel, ruw, line, wat)) return null;
        Eenheid doelE = doel == null ? null : doel.eenheidOf();
        if (bronE != null && doelE != null && bronE.convertibleNaar(doelE) && bronE.vereistOmrekening(doelE)) {
            BigDecimal[] f = bronE.exacteHerschaalFactoren(doelE);
            if (f == null) {
                fout(line, wat + ": tussen de eenheden '" + bronE.weergave() + "' en '" + doelE.weergave() + "' is geen "
                        + "exacte omrekening");
                return null;
            }
            BigInteger[] fb = breuk(f[0], f[1]);
            return Waarde.vanGetal(lexeem(breuk[0].multiply(fb[0]), breuk[1].multiply(fb[1])));
        }
        // no restatement: the written number in the emitter's lexeme, so a case file and an IR document agree
        return Waarde.vanGetal(dev.uchar.regelspraak.emit.IrEmitter.getalLexeem(tekst));
    }

    /** {@code a / b} as a reduced fraction of integers, the denominator positive. */
    private static BigInteger[] breuk(BigDecimal a, BigDecimal b) {
        BigInteger[] ta = alsBreuk(a), tb = alsBreuk(b);
        BigInteger teller = ta[0].multiply(tb[1]), noemer = ta[1].multiply(tb[0]);
        if (noemer.signum() < 0) { teller = teller.negate(); noemer = noemer.negate(); }
        BigInteger ggd = teller.gcd(noemer);
        if (ggd.signum() != 0) { teller = teller.divide(ggd); noemer = noemer.divide(ggd); }
        return new BigInteger[]{teller, noemer};
    }

    private static BigInteger[] alsBreuk(BigDecimal x) {
        return x.scale() >= 0 ? new BigInteger[]{x.unscaledValue(), BigInteger.TEN.pow(x.scale())}
                : new BigInteger[]{x.unscaledValue().multiply(BigInteger.TEN.pow(-x.scale())), BigInteger.ONE};
    }

    private static boolean eindigt(BigInteger noemer) {
        BigInteger n = noemer;
        for (BigInteger p : new BigInteger[]{BigInteger.TWO, BigInteger.valueOf(5)})
            while (n.mod(p).signum() == 0) n = n.divide(p);
        return n.equals(BigInteger.ONE);
    }

    /** The case lexeme of a reduced fraction: a decimal without trailing zeros where it terminates, else {@code p/q}. */
    private static String lexeem(BigInteger teller, BigInteger noemer) {
        BigInteger[] r = breuk(new BigDecimal(teller), new BigDecimal(noemer));
        if (r[1].equals(BigInteger.ONE)) return r[0].toString();
        if (eindigt(r[1])) {
            BigDecimal d = new BigDecimal(r[0]).divide(new BigDecimal(r[1])).stripTrailingZeros();
            return (d.scale() < 0 ? d.setScale(0) : d).toPlainString();
        }
        return r[0] + "/" + r[1];
    }

    /** The literal content of a text value; an interpolated «expressie» is refused, since a case value is literal. */
    private String tekstInhoud(TekstWaardeContext t, int line, String wat) {
        StringBuilder sb = new StringBuilder();
        for (TekstReeksDeelContext deel : t.tekstReeksDeel()) {
            if (deel.STRING_TEXT() == null) {
                fout(line, wat + ": de tekstwaarde draagt een «expressie», en een waarde in een scenario is letterlijk");
                return null;
            }
            sb.append(deel.STRING_TEXT().getText());
        }
        return sb.toString();
    }

    // ---------------- small helpers ----------------

    private Parameter parameterOpNaam(String naam) {
        Parameter exact = model.parameters.get(naam);
        if (exact != null) return exact;
        for (var e : model.parameters.entrySet()) if (norm(e.getKey()).equals(norm(naam))) return e.getValue();
        return null;
    }

    /** The declared object-type name whose first word (or whole name) {@code label} equals, or null. */
    private String labelBotstMetType(String label) {
        for (String naam : model.objecttypen.keySet())
            if (naam.equalsIgnoreCase(label) || naam.split("\\s+")[0].equalsIgnoreCase(label)) return naam;
        return null;
    }

    private static String src(ParserRuleContext ctx) { return DefinitionResolver.src(ctx); }

    private static String zonderLidwoord(String naam) {
        String n = naam == null ? "" : naam.trim();
        for (String lw : new String[]{"de ", "het ", "een "})
            if (n.toLowerCase(java.util.Locale.ROOT).startsWith(lw)) return n.substring(lw.length()).trim();
        return n;
    }

    private void fout(int line, String bericht) {
        model.diagnostics.add(new Diagnostic(Severity.ERROR, "scenario: " + bericht, line));
    }
}
