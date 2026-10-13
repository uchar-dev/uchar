package dev.uchar.regelspraak.resolver;

import dev.uchar.regelspraak.kern.Tijdrekening;
import dev.uchar.regelspraak.model.*;
import dev.uchar.regelspraak.model.RegelSpraakModel.*;
import dev.uchar.regelspraak.parser.RegelSpraak;
import dev.uchar.regelspraak.parser.RegelSpraak.*;
import dev.uchar.regelspraak.parser.RegelSpraakLexer;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the typed {@link RegelSpraakModel} from a parsed GegevensSpraak document
 * (§13.3). Two passes: domains and unit systems first (they are referenced by
 * attributes/parameters and may be declared after their use), then object types,
 * parameters, fact types, day types, and rule shells.
 *
 * Multi-word names — which the parser deliberately leaves as token runs — are
 * reconstructed here from the original source slice, with whitespace normalised.
 * This is the post-parse name resolution CLAUDE.md assigns to the resolver.
 */
public final class DefinitionResolver {
    private final RegelSpraakModel model = new RegelSpraakModel();

    public RegelSpraakModel build(RegelSpraakDocumentContext doc) {
        weigerDubbeleNamen(doc);
        for (DefinitieContext d : definities(doc)) {        // pass 1: domains + units + timelines
            if (d.domeinDefinition() != null) domein(d.domeinDefinition());
            if (d.eenheidsysteemDefinition() != null) eenheidsysteem(d.eenheidsysteemDefinition());
            // a named timeline must be registered before pass 2, so an attribute's "voor elke <tijdlijn>"
            // reference can adopt its granularity regardless of declaration order (§13.3.6.9). It reads only
            // its own tokens, so it has no dependency on objecttypes/feittypes processed in pass 2.
            if (d.tijdlijnDefinitie() != null) tijdlijn(d.tijdlijnDefinitie());
        }
        for (DefinitieContext d : definities(doc)) {        // pass 2: everything that references them
            if (d.objectTypeDefinition() != null) objecttype(d.objectTypeDefinition());
            if (d.objectTypeExtensie() != null) extensie(d.objectTypeExtensie());
            if (d.parameterDefinition() != null) parameter(d.parameterDefinition());
            if (d.feitTypeDefinition() != null) feittype(d.feitTypeDefinition());
            if (d.dagsoortDefinition() != null) dagsoort(d.dagsoortDefinition());
            if (d.dimensieDefinition() != null) dimensie(d.dimensieDefinition());
        }
        // the 'is een' clauses bind only now: resolution is document-wide and ORDER-FREE, so a
        // forward reference is legal and only an undeclared name refuses (SUBTYPING §9.2)
        bindSupertypen();
        // a member an extension declares again is judged only now: the head may follow the extension, and a role it
        // names may be declared after it
        weigerHerdeclaraties();
        // a role's filler binds only now, for the same reason: a fact type may name a type declared after it
        weigerOngedeclareerdeVullers();
        for (RegelContext r : alleRegels(doc)) {
            model.regels.add(new Regel(src(r.regelNaam()), "regel"));
        }
        // §4 (PRIORITIZED-DEFAULTS-EVOLUTIE) the ordered 'Uitzondering op' exception blocks carry their own name
        // too — a Regel stub in document position, so each defaults-producing rule appears in `regels` exactly as
        // the flat rules and the group members do (their RESOLVED form lives in the defaults group, not
        // resolvedRegels).
        for (UitzonderingRegelContext u : alleUitzonderingen(doc)) {
            model.regels.add(new Regel(src(u.regelNaam()), "regel"));
        }
        return model;
    }

    /** Every §13.4.2 regel of the document — the top-level ones AND the members of a §9.10 {@link RegelGroepContext}
     *  (a DOCUMENTED EXTENSION; see grammar/DIVERGENCES.md). A grouped rule resolves EXACTLY as an ungrouped
     *  one — the group is namespacing only — so the whole rule pipeline walks this flattened list. Order is
     *  preserved (a group's members appear where the group's header sits). */
    static List<RegelContext> alleRegels(RegelSpraakDocumentContext doc) {
        List<RegelContext> uit = new ArrayList<>();
        for (ParseTree kind : doc.children == null ? List.<ParseTree>of() : doc.children) {
            if (kind instanceof RegelContext r) {
                uit.add(r);
            } else if (kind instanceof RegelGroepContext g) {
                for (GroepLidContext lid : g.groepLid())
                    if (lid.regel() != null) uit.add(lid.regel());
            } else if (kind instanceof UitzonderingGroepContext ug) {
                // §4 (PRIORITIZED-DEFAULTS-EVOLUTIE) an unordered exception group's members are ordinary regels: they
                // resolve through the WHOLE rule pipeline (type-checks included), and the defaults pass then LIFTS each
                // into sibling declared-unordered PrioritizedDefaults (removing it from resolvedRegels — the defaults
                // group is the writer).
                uit.addAll(ug.regel());
            }
        }
        return uit;
    }

    /** §4 (PRIORITIZED-DEFAULTS-EVOLUTIE) every ordered 'Uitzondering op &lt;basis&gt;: &lt;naam&gt;' block of the
     *  document, in document order. Each resolves like a §13.4.2 regel (its regelNaam + regelVersie+ body) and is
     *  then lifted, with its named base rule, into one
     *  {@link dev.uchar.regelspraak.ir.PrioritizedDefaults}. */
    static List<UitzonderingRegelContext> alleUitzonderingen(RegelSpraakDocumentContext doc) {
        List<UitzonderingRegelContext> uit = new ArrayList<>();
        for (ParseTree kind : doc.children == null ? List.<ParseTree>of() : doc.children)
            if (kind instanceof UitzonderingRegelContext u) uit.add(u);
        return uit;
    }

    /** §4 (PRIORITIZED-DEFAULTS-EVOLUTIE) every unordered exception group of the document, in document order. */
    static List<UitzonderingGroepContext> alleUitzonderingGroepen(RegelSpraakDocumentContext doc) {
        List<UitzonderingGroepContext> uit = new ArrayList<>();
        for (ParseTree kind : doc.children == null ? List.<ParseTree>of() : doc.children)
            if (kind instanceof UitzonderingGroepContext g) uit.add(g);
        return uit;
    }

    /** Every §12 beslistabel of the document — top-level ones AND the members of a §9.10 {@link RegelGroepContext}
     *  (a grouped beslistabel is namespacing only; it desugars to grouped regels exactly like an ungrouped one). */
    static List<BeslistabelContext> alleBeslistabellen(RegelSpraakDocumentContext doc) {
        List<BeslistabelContext> uit = new ArrayList<>();
        for (ParseTree kind : doc.children == null ? List.<ParseTree>of() : doc.children) {
            if (kind instanceof BeslistabelContext b) uit.add(b);
            else if (kind instanceof RegelGroepContext g)
                for (GroepLidContext lid : g.groepLid())
                    if (lid.beslistabel() != null) uit.add(lid.beslistabel());
        }
        return uit;
    }

    private static List<DefinitieContext> definities(RegelSpraakDocumentContext doc) {
        return doc.definitie();
    }

    // ---------------- §13.3.1 object types ----------------
    private void objecttype(ObjectTypeDefinitionContext ctx) {
        Objecttype ot = model.objecttypen.computeIfAbsent(src(ctx.objectTypeNaam()), Objecttype::new);
        if (ctx.BEZIELD() != null) ot.bezield = true;
        if (ctx.meervoudsvorm() != null) ot.meervoud = src(ctx.meervoudsvorm()); // §13.2.29, for §8.1.6 plural refs
        supertypeClausules(ot, ctx.objectTypeSupertype());
        for (ObjectTypeMemberContext m : ctx.objectTypeMember()) member(ot, m, ctx, false);
        ledenNaKop(ot, ctx, ctx.objectTypeMember());
    }

    // ---------------- the uchar 'is een' dialect extension (IR-SPEC ch. 2.2) ----------------
    /** The parent names an object type DECLARED, in declaration order, keyed by the type's own name;
     *  plus the source line of its first clause. Held rather than bound: the reference is order-free,
     *  so nothing can be resolved until every declaration has been read ({@link #bindSupertypen}). */
    private final java.util.Map<String, List<String>> supertypeNamen = new java.util.LinkedHashMap<>();
    private final java.util.Map<String, Integer> supertypeRegel = new java.util.LinkedHashMap<>();

    /** The source line of each member DECLARATION, keyed by object type and lowercased member name — the
     *  line WF-105 must cite, because the edit that resolves a shadow is at the redeclaration and not at
     *  the {@code is een} clause. Filled for every member; only read for a type that has a supertype. */
    private final java.util.Map<Objecttype, java.util.Map<String, Integer>> lidRegel = new java.util.IdentityHashMap<>();

    private void onthoudLidRegel(Objecttype ot, ObjectTypeMemberContext m) {
        if (m.koptekst() != null) return;
        String naam = m.attribuutSpecificatie() != null ? naam(m.attribuutSpecificatie().lowerNaam())
                                                        : kenmerkNaam(m.kenmerkSpecificatie());
        lidRegel.computeIfAbsent(ot, k -> new java.util.LinkedHashMap<>())
                .putIfAbsent(naam.toLowerCase(), m.getStart().getLine());
    }

    /** Record the 'is een &lt;Objecttypenaam&gt;' clause(s) of one head. Every name the head writes is
     *  kept — including the ones WF-106 forbids — so the refusal can NAME them instead of reducing to
     *  a syntax error. */
    private void supertypeClausules(Objecttype ot, List<ObjectTypeSupertypeContext> clausules) {
        if (clausules.isEmpty()) return;
        List<String> ouders = supertypeNamen.computeIfAbsent(ot.naam, k -> new ArrayList<>());
        for (ObjectTypeSupertypeContext c : clausules)
            for (ObjectTypeNaamContext n : c.objectTypeNaam()) ouders.add(src(n));
        supertypeRegel.putIfAbsent(ot.naam, clausules.get(0).getStart().getLine());
    }

    /** Bind every declared supertype reference and refuse the shapes the chain cannot carry — the
     *  frontend's share of WF-103/104/105/106. It refuses BEFORE the verifier does because it has the
     *  chain at hand, and WF-106 has nowhere else to be refused at all: a bare-string {@code supertype}
     *  cannot WRITE a second parent, so that rule bounds the language above the wire (IR-SPEC ch. 2.2).
     *  A refused reference stays UNBOUND — a half-built chain would make every later reader disagree
     *  with the document. */
    private void bindSupertypen() {
        for (var e : supertypeNamen.entrySet()) {
            Objecttype ot = model.objecttypen.get(e.getKey());
            if (ot == null) continue;
            int regel = supertypeRegel.getOrDefault(e.getKey(), 0);
            // Distinct by name, ci: a head repeating ONE parent is redundant, not a second parent;
            // two DIFFERENT names are the refusal, however they are spelled (two clauses, or "A en B").
            List<String> ouders = new ArrayList<>();
            for (String n : e.getValue())
                if (ouders.stream().noneMatch(o -> o.equalsIgnoreCase(n))) ouders.add(n);
            if (ouders.size() > 1) {
                fout("objecttype '" + ot.naam + "' declareert meer dan één supertype ("
                        + String.join(", ", ouders) + ") — WF-106: een objecttype declareert hoogstens één "
                        + "direct supertype. De twee bekende gevallen met twee directe ouders "
                        + "(RechtOpUitkeringWazo, RechtUitkeringZw) worden hier geweigerd en niet "
                        + "platgeslagen: kies één ouder, of til het gedeelde deel naar een gemeenschappelijk "
                        + "supertype", regel);
                continue;
            }
            String naam = ouders.get(0);
            if (naam.equalsIgnoreCase(ot.naam)) {
                fout("objecttype '" + ot.naam + "' is zijn eigen supertype — WF-104: de supertypegraaf is "
                        + "acyclisch, zelfverwijzing inbegrepen (een keten zonder wortel laat ledenresolutie "
                        + "nergens stoppen)", regel);
                continue;
            }
            Objecttype ouder = model.objecttypeOpNaam(naam);
            if (ouder == null) {
                String soort = soortVanNaam(naam);
                fout("objecttype '" + ot.naam + "': supertype '" + naam + "' is "
                        + (soort == null ? "nergens als objecttype gedeclareerd" : "een " + soort + ", geen objecttype")
                        + " — WF-103: een supertype noemt een gedeclareerd objecttype, want een keten die daar "
                        + "niet op eindigt lost geen enkel lid op", regel);
                continue;
            }
            ot.supertype = ouder;
        }
        weigerSupertypeCycli();
        weigerGeshadowdeLeden();
    }

    /** What the document DOES declare {@code naam} as, when it is not an object type — so the refusal can
     *  say "een domein, geen objecttype" instead of sending the author hunting for a typo. Null when the
     *  name is not declared at all. */
    private String soortVanNaam(String naam) {
        if (bevatNaam(model.domeinen.keySet(), naam)) return "domein";
        if (bevatNaam(model.parameters.keySet(), naam)) return "parameter";
        if (bevatNaam(model.feittypen.keySet(), naam)) return "feittype";
        if (bevatNaam(model.dimensies.keySet(), naam)) return "dimensie";
        if (bevatNaam(model.eenheidstelsels.keySet(), naam)) return "eenheidsysteem";
        if (bevatNaam(model.dagsoorten, naam)) return "dagsoort";
        return null;
    }

    private static boolean bevatNaam(java.util.Collection<String> namen, String naam) {
        return namen.stream().anyMatch(n -> n.equalsIgnoreCase(naam));
    }

    /** WF-104 for cycles of two steps or more (the depth-1 self-edge is refused by name above, so it never
     *  reaches here). Colour DFS with a named cycle path — the uchar verifier's own shape, because the
     *  failure is the same: member resolution has no root to stop at. The back edge is CUT after it is
     *  reported, so every later pass over the model terminates on a document the resolver has already
     *  rejected. */
    private void weigerSupertypeCycli() {
        java.util.Map<Objecttype, Integer> kleur = new java.util.IdentityHashMap<>();
        for (Objecttype ot : model.objecttypen.values()) zoekCyclus(ot, kleur, new ArrayList<>());
    }

    private void zoekCyclus(Objecttype t, java.util.Map<Objecttype, Integer> kleur, List<Objecttype> pad) {
        Integer k = kleur.get(t);
        if (k != null && k == 2) return;
        if (k != null && k == 1) {                       // grey: a back edge onto the current path
            List<String> namen = new ArrayList<>();
            for (Objecttype x : pad.subList(pad.indexOf(t), pad.size())) namen.add(x.naam);
            namen.add(t.naam);
            Objecttype terug = pad.get(pad.size() - 1);
            fout("objecttype '" + terug.naam + "': supertypecyclus " + String.join(" -> ", namen)
                    + " — WF-104: de supertypegraaf is acyclisch (een type zou zijn eigen voorouder zijn, "
                    + "dus ledenresolutie heeft geen wortel om bij te stoppen)",
                    supertypeRegel.getOrDefault(terug.naam, 0));   // the clause that closes the cycle
            terug.supertype = null;
            return;
        }
        kleur.put(t, 1);
        pad.add(t);
        if (t.supertype != null) zoekCyclus(t.supertype, kleur, pad);
        pad.remove(pad.size() - 1);
        kleur.put(t, 2);
    }

    /** WF-105: no attribute or characteristic name is declared twice along one chain (ci) — WF-002's
     *  per-kind uniqueness lifted from the declaration to the chain. One instance would otherwise carry two
     *  declarations of one name, and which one a reader finds decides that name's datatype, dimensions and
     *  timeline facet on every read of it. NOT refused, and deliberately: a SIBLING collision — two
     *  incomparable subtypes declaring one name — no instance is of two incomparable types, so no read is
     *  ambiguous. */
    private void weigerGeshadowdeLeden() {
        for (Objecttype ot : model.objecttypen.values()) {
            if (ot.supertype == null) continue;
            List<Objecttype> voorouders = ot.keten().subList(1, ot.keten().size());
            for (String a : ot.attributen.keySet()) meldShadow(ot, voorouders, "attribuut", a, true);
            for (String k : ot.kenmerken) meldShadow(ot, voorouders, "kenmerk", k, false);
        }
    }

    private void meldShadow(Objecttype ot, List<Objecttype> voorouders, String soort, String naam, boolean attribuut) {
        for (Objecttype v : voorouders) {
            boolean botst = attribuut ? bevatNaam(v.attributen.keySet(), naam) : bevatNaam(v.kenmerken, naam);
            if (!botst) continue;
            int regel = lidRegel.getOrDefault(ot, java.util.Map.of()).getOrDefault(naam.toLowerCase(), 0);
            fout("objecttype '" + ot.naam + "': " + soort + " '" + naam + "' (bronregel " + regel + ") is al "
                    + "gedeclareerd op '" + v.naam + "' (bronregel "
                    + lidRegel.getOrDefault(v, java.util.Map.of()).getOrDefault(naam.toLowerCase(), 0)
                    + ") hogerop in de keten — WF-105: een subtype herdeclareert geen geërfde naam (één "
                    + "exemplaar zou twee declaraties van die naam dragen, en welke een lezer vindt bepaalt "
                    + "datatype, dimensies en tijdlijnfacet van elke lezing ervan)", regel);
            return;                                      // one report per member, at its nearest declarant
        }
    }

    /**
     * WF-001: within each namespace of the model a declared name is unique, case not counted. Two declarations of one
     * name were taken together here without a word: a second object type head added its members to the first and
     * replaced a member of the same name, a second domain, parameter, unit system, fact type or dimension replaced the
     * first, and the IR carried one declaration, so the verifier never saw the second. Which one the model kept depended
     * on which stood last in the source or in the files put together, and two day kinds of one name stopped the emitter.
     * A second declaration is refused at its own line, naming the line of the first, in the verifier's sentence. An
     * extension adds members to a declared type and is no declaration of the name (weigerHerdeclaraties); timelines are
     * WF-015's. A definition the parser recovered inside is left alone: its syntax error is the one to act on.
     */
    private void weigerDubbeleNamen(RegelSpraakDocumentContext doc) {
        java.util.Map<String, String[]> eerder = new java.util.HashMap<>();   // namespace + name identity -> {name, line}
        for (DefinitieContext d : definities(doc)) {
            String[] soort = null;   // {how the refusal names the kind, the verifier's namespace, the name}
            ParserRuleContext ctx = null;
            if (d.objectTypeDefinition() != null) {
                ctx = d.objectTypeDefinition();
                soort = new String[] {"objecttype", "objectTypes", src(d.objectTypeDefinition().objectTypeNaam())};
            } else if (d.domeinDefinition() != null) {
                ctx = d.domeinDefinition();
                soort = new String[] {"domein", "domains", src(d.domeinDefinition().domeinNaam())};
            } else if (d.parameterDefinition() != null) {
                ctx = d.parameterDefinition();
                soort = new String[] {"parameter", "parameters", src(d.parameterDefinition().parameterNaam())};
            } else if (d.eenheidsysteemDefinition() != null) {
                ctx = d.eenheidsysteemDefinition();
                soort = new String[] {"eenheidsysteem", "unitSystems",
                        src(d.eenheidsysteemDefinition().eenheidsysteemNaam())};
            } else if (d.feitTypeDefinition() != null) {
                ctx = d.feitTypeDefinition();
                soort = new String[] {"feittype", "factTypes", src(d.feitTypeDefinition().feitTypeNaam())};
            } else if (d.dimensieDefinition() != null) {
                ctx = d.dimensieDefinition();
                soort = new String[] {"dimensie", "dimensions", src(d.dimensieDefinition().dimensieNaam())};
            } else if (d.dagsoortDefinition() != null) {
                ctx = d.dagsoortDefinition();
                soort = new String[] {"dagsoort", "dayKinds", naam(d.dagsoortDefinition().lowerNaam())};
            }
            if (soort == null || hersteld(ctx)) continue;
            int regel = ctx.getStart().getLine();
            String[] eerste = eerder.putIfAbsent(soort[1] + "\u0000" + dev.uchar.regelspraak.emit.IrEmitter.norm(soort[2]),
                    new String[] {soort[2], String.valueOf(regel)});
            if (eerste == null) continue;
            fout(soort[0] + " '" + soort[2] + "' is al gedeclareerd (bronregel " + eerste[1] + "); een naam komt in "
                    + "zijn soort declaratie één keer voor, hoofdletters niet meegeteld (WF-001): namespace '" + soort[1]
                    + "': names '" + eerste[0] + "' and '" + soort[2] + "' collide case-insensitively", regel);
        }
    }

    private void fout(String bericht, int regel) {
        model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.ERROR, bericht, regel));
    }

    // ---------------- a member an extension declares again ----------------
    /** One member declaration, in a head or in an extension, in document order. */
    private record Lid(Objecttype type, boolean attribuut, String naam, boolean extensie,
                       ObjectTypeMemberContext ctx, ParserRuleContext definitie) {
        int regel() { return ctx.getStart().getLine(); }
        /** A declaration of the same member: one type, one kind, one name (ci, WF-002). */
        boolean zelfde(Lid ander) {
            return type == ander.type && attribuut == ander.attribuut && naam.equalsIgnoreCase(ander.naam);
        }
    }
    private final List<Lid> leden = new ArrayList<>();

    /** Every member an extension declares that its type already carries, and whether that repeat is admitted. The
     *  model holds the declaration the type carries, so the designations on a repeat are not read a second time
     *  ({@link Benamingen}), and the opgave pass reads the marker of an admitted repeat only ({@link RuleChecker}). */
    private final java.util.Map<ObjectTypeMemberContext, Boolean> herhalingen = new java.util.IdentityHashMap<>();

    java.util.Map<ObjectTypeMemberContext, Boolean> herhalingen() { return herhalingen; }

    /** The source line of each role, per fact type, for the refusal of a role an extension declares again. */
    private final java.util.Map<Feittype, java.util.Map<String, Integer>> rolRegels = new java.util.IdentityHashMap<>();

    /** An extension adds members to a type and replaces none (grammar/DIVERGENCES.md, §5 and §7). A member it declares
     *  that the type already carries, in its head or, where no head declares it, in an earlier extension, is admitted
     *  as that declaration written again word for word, the opgave marker aside; the repeat may add the marker once.
     *  Any other repeat is an ERROR at the repeat's line that names both lines: until this check the repeat replaced
     *  the declaration without a word, also with another datatype, and a repeated characteristic went to the wire
     *  twice. The words decide, so layout and the closing ';' do not count. The literal repeat is admitted because it
     *  changes nothing, and the casus of the repository's parity zone writes three of them. Along the
     *  inheritance line WF-105 refuses every repeat, literal or not ({@link #weigerGeshadowdeLeden}). A definition the
     *  parser recovered inside is left alone: its syntax error is the one to act on. */
    private void weigerHerdeclaraties() {
        for (Lid d : leden) {
            if (!d.extensie() || hersteld(d.definitie())) continue;
            Lid b = bestaande(d);
            if (b == null || hersteld(b.definitie())) continue;
            if (b != d) oordeel(d, b);
            else if (d.attribuut()) weigerRolHerdeclaratie(d);
        }
    }

    /** The declaration of d's member the type carries: its head's, else the first in document order. */
    private Lid bestaande(Lid d) {
        Lid eerste = null;
        for (Lid l : leden) {
            if (!l.zelfde(d)) continue;
            if (!l.extensie()) return l;
            if (eerste == null) eerste = l;
        }
        return eerste;
    }

    private void oordeel(Lid d, Lid b) {
        String reden = null;
        if (!woorden(d.ctx()).equals(woorden(b.ctx())))
            reden = "'" + tekst(d.ctx()) + "' is niet letterlijk '" + tekst(b.ctx()) + "'";
        else if (RuleChecker.gemarkeerdLid(d.ctx())) {
            Lid gemarkeerd = eerdereMarkering(d, b);
            if (gemarkeerd != null)
                reden = "dat draagt '(mag opgegeven worden)' al (bronregel " + gemarkeerd.regel() + ")";
        }
        herhalingen.put(d.ctx(), reden == null);
        if (reden == null) return;
        fout("objecttype '" + d.type().naam + "': de extensie declareert " + (d.attribuut() ? "het attribuut '"
                : "het kenmerk '") + d.naam() + "' (bronregel " + d.regel() + "), dat " + d.type().naam + " al draagt "
                + "(bronregel " + b.regel() + "), en " + reden + " — een extensie voegt leden toe en vervangt er geen: "
                + "een lid dat het objecttype al draagt, herhaalt zij alleen letterlijk, en '(mag opgegeven worden)' "
                + "komt er hooguit één keer bij (WF-002, dialect §7)", d.regel());
    }

    /** The declaration of d's member that carries the opgave marker already: the one the type carries, or an admitted
     *  repeat before d that added it; null when none does. */
    private Lid eerdereMarkering(Lid d, Lid b) {
        if (RuleChecker.gemarkeerdLid(b.ctx())) return b;
        for (Lid l : leden) {
            if (l == d) return null;
            if (l.zelfde(d) && Boolean.TRUE.equals(herhalingen.get(l.ctx())) && RuleChecker.gemarkeerdLid(l.ctx()))
                return l;
        }
        return null;
    }

    /** The words of a member declaration without the opgave marker and the closing ';': what a repeat writes again. */
    private static List<String> woorden(ParseTree t) {
        List<String> uit = new ArrayList<>();
        if (t instanceof TerminalNode n) {
            int soort = n.getSymbol().getType();
            if (soort != RegelSpraak.MAG_OPGEGEVEN_WORDEN && soort != RegelSpraak.SEMICOLON) uit.add(n.getText());
        } else {
            for (int i = 0; i < t.getChildCount(); i++) uit.addAll(woorden(t.getChild(i)));
        }
        return uit;
    }

    /** A member declaration as written, without its closing ';'. */
    private static String tekst(ObjectTypeMemberContext m) {
        return src(m).replaceFirst("\\s*;$", "");
    }

    /** An extension declares attributes and characteristics, so the grammar reads a role line in it ('de vinder
     *  Jutter;') as an attribute whose type is a domain. Where that name is no domain but a declared object type, and
     *  the member's name is a role of a fact type the type or a supertype of it takes part in, the line declares that
     *  role again: an ERROR naming both lines. Until this check the line was refused as an undeclared domain on line
     *  0; the member leaves the model, so that refusal does not come beside this one. */
    private void weigerRolHerdeclaratie(Lid d) {
        AttribuutSpecificatieContext a = d.ctx().attribuutSpecificatie();
        if (a.datatype() != null || a.domeinNaam() == null) return;
        String vuller = src(a.domeinNaam());
        if (bevatNaam(model.domeinen.keySet(), vuller) || model.objecttypeOpNaam(vuller) == null) return;
        for (Feittype ft : model.feittypen.values()) {
            if (ft.rollen.stream().noneMatch(r -> d.type().isEen(model.objecttypeOpNaam(r.objecttypeNaam())))) continue;
            for (Rol r : ft.rollen) {
                if (!r.naam().equalsIgnoreCase(d.naam())) continue;
                herhalingen.put(d.ctx(), false);
                vergeetAttribuut(d.type(), d.naam());
                fout("objecttype '" + d.type().naam + "': de extensie declareert '" + d.naam() + "' met het "
                        + "objecttype '" + vuller + "' (bronregel " + d.regel() + "), en dat is de rol '" + r.naam()
                        + "' van feittype '" + ft.naam + "' (bronregel "
                        + rolRegels.getOrDefault(ft, java.util.Map.of()).getOrDefault(r.naam(), 0) + ") — een "
                        + "extensie voegt attributen en kenmerken toe en geen rollen: een rol staat in haar feittype",
                        d.regel());
                return;
            }
        }
    }

    /** Take an attribute back out of a type: the attribute and every side map keyed by its name. */
    private static void vergeetAttribuut(Objecttype ot, String naam) {
        ot.attributen.remove(naam);
        ot.attribuutMeervoud.remove(naam);
        ot.attribuutDimensies.remove(naam);
        ot.attribuutGranulariteit.remove(naam);
        ot.attribuutTijdlijnAantal.remove(naam);
        ot.attribuutTijdlijnNaam.remove(naam);
        ot.attribuutVastStartpunt.remove(naam);
    }

    private void extensie(ObjectTypeExtensieContext ctx) {
        // an extensie adds members to an existing (or new) object type and replaces none (weigerHerdeclaraties)
        Objecttype ot = model.objecttypen.computeIfAbsent(src(ctx.objectTypeNaam()), Objecttype::new);
        for (ObjectTypeMemberContext m : ctx.objectTypeMember()) member(ot, m, ctx, true);
        ledenNaKop(ot, ctx, ctx.objectTypeMember());
    }

    private void member(Objecttype ot, ObjectTypeMemberContext m, ParserRuleContext definitie, boolean extensie) {
        onthoudLidRegel(ot, m);
        boolean herhaling = false;
        if (m.attribuutSpecificatie() != null || m.kenmerkSpecificatie() != null) {
            boolean attribuut = m.attribuutSpecificatie() != null;
            Lid lid = new Lid(ot, attribuut, attribuut ? naam(m.attribuutSpecificatie().lowerNaam())
                    : kenmerkNaam(m.kenmerkSpecificatie()), extensie, m, definitie);
            herhaling = leden.stream().anyMatch(l -> l.zelfde(lid) && (extensie || l.extensie()));
            leden.add(lid);
            // an extension's repeat is judged once every declaration is read; the model keeps the declaration the
            // type already carries, so the rest of the document reads that one
            if (herhaling && extensie) return;
        }
        if (m.attribuutSpecificatie() != null) {
            AttribuutSpecificatieContext a = m.attribuutSpecificatie();
            String naam = naam(a.lowerNaam());
            meldHoofdletterBotsing(ot, naam, ot.attributen.keySet(), a.getStart().getLine());
            ot.attributen.put(naam, metEenheid(datatypeVan(a.datatype(), a.domeinNaam()), a, naam));
            if (a.meervoudsvorm() != null) ot.attribuutMeervoud.put(naam, src(a.meervoudsvorm())); // §3.3, for §8.1.6
            // §13.3.7 "gedimensioneerd met <dimensie> (en <dimensie>)*": record the attribute's dimensions
            // in declared order so a reference / aggregation can range over their label sets (§3.6).
            if (!a.dimensieNaam().isEmpty())
                ot.attribuutDimensies.put(naam, a.dimensieNaam().stream().map(dn -> src(dn)).toList());
            // §13.3.6 a temporal attribute carries a tijdlijn ("voor elk jaar") — record its granularity so
            // the Phase-7 value model can later lower the periods; absent for an ordinary single-value attribute.
            Granulariteit g = granulariteitVan(a.tijdlijn());
            if (g != null) {
                ot.attribuutGranulariteit.put(naam, g);
                int aantal = aantalVan(a.tijdlijn());   // §13.3.6.7 the "5" in "voor elke 5 maanden"; 1 otherwise
                if (aantal != 1) ot.attribuutTijdlijnAantal.put(naam, aantal);
                String vast = vastStartpuntVan(a.tijdlijn(), g, aantal);
                if (vast != null) ot.attribuutVastStartpunt.put(naam, vast);
                // §13.3.6.9 the NAMED form: keep the tijdlijn name so §9.9 can route a variable
                // startpunt to its anchor (the inline "voor elke maand" forms record none).
                if (a.tijdlijn().ELKE() != null && a.tijdlijn().tijdlijnNaam() != null)
                    ot.attribuutTijdlijnNaam.put(naam, normaliseerTijdlijnNaam(src(a.tijdlijn().tijdlijnNaam())));
            }
        } else if (m.kenmerkSpecificatie() != null) {
            String naam = kenmerkNaam(m.kenmerkSpecificatie());
            meldHoofdletterBotsing(ot, naam, ot.kenmerken, m.getStart().getLine());
            // a head after an extension that declared the characteristic: the name stands once
            if (!herhaling || !ot.kenmerken.contains(naam)) ot.kenmerken.add(naam);
            ot.kenmerkSoorten.put(naam, kenmerkSoortVan(m.kenmerkSpecificatie()));   // §3.5 drives the §8.1.8 predicate form
            TijdlijnContext kt = tijdlijnVan(m.kenmerkSpecificatie());
            Granulariteit gk = granulariteitVan(kt);
            if (gk != null) {
                ot.kenmerkGranulariteit.put(naam, gk);
                int aantal = aantalVan(kt);
                if (aantal != 1) ot.kenmerkTijdlijnAantal.put(naam, aantal);
                String vast = vastStartpuntVan(kt, gk, aantal);
                if (vast != null) ot.kenmerkVastStartpunt.put(naam, vast);
            }
        }
        // koptekst carries no model information
    }

    /** §13.2.30: a naam is NOT hoofdletter-onderscheidend, so two members of one object type OF
     *  THE SAME KIND that differ ONLY in case are the same name declared twice — ambiguous (a
     *  third casing would bind whichever was declared first, see {@link Objecttype#attribuut}).
     *  Warn at registration. Scoped to one kind: an attribute and a kenmerk MAY legitimately
     *  share a name — a
     *  kenmerk carries no datatype and never enters {@link Objecttype#attribuut}, so there is no
     *  cross-kind ambiguity. An exact-case redeclaration is left to the collection. */
    private void meldHoofdletterBotsing(Objecttype ot, String naam, java.util.Collection<String> bestaande, int regel) {
        if (bestaande.contains(naam)) return; // exact duplicate of the same kind: not a case botsing
        if (bestaande.stream().anyMatch(k -> k.equalsIgnoreCase(naam)))
            model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.WARNING,
                    "lid '" + naam + "' van objecttype '" + ot.naam + "' verschilt alleen in hoofdletters van een eerder lid; "
                            + "namen zijn niet hoofdletter-onderscheidend (§13.2.30), dus de verwijzing is dubbelzinnig", regel));
    }

    private String kenmerkNaam(KenmerkSpecificatieContext k) {
        if (k instanceof BijvoeglijkKenmerkContext b) return naam(b.lowerNaam());
        if (k instanceof NaamwoordKenmerkContext n) return naam(n.lowerNaam());
        return src(k);
    }

    /** §3.5 kenmerk soort from the declaration: the optional {@code (bijvoeglijk)}/{@code (bezittelijk)} marker
     *  classifies it; an UNTAGGED kenmerk is ZELFSTANDIG (the "is een &lt;k&gt;" noch-noch form — including the
     *  casus shapes the grammar admits via the IS-form without the tag). */
    private static Objecttype.KenmerkSoort kenmerkSoortVan(KenmerkSpecificatieContext k) {
        if (k instanceof BijvoeglijkKenmerkContext b)
            return b.BIJVOEGLIJK() != null ? Objecttype.KenmerkSoort.BIJVOEGLIJK : Objecttype.KenmerkSoort.ZELFSTANDIG;
        if (k instanceof NaamwoordKenmerkContext n)
            return n.BEZITTELIJK() != null ? Objecttype.KenmerkSoort.BEZITTELIJK
                 : n.BIJVOEGLIJK() != null ? Objecttype.KenmerkSoort.BIJVOEGLIJK
                 : Objecttype.KenmerkSoort.ZELFSTANDIG;
        return Objecttype.KenmerkSoort.ZELFSTANDIG;
    }

    // ---------------- §13.3.3/§13.3.4 datatypes & domains ----------------
    private void domein(DomeinDefinitionContext ctx) {
        Datatype dt = ctx.enumeratieSpecificatie() != null
                ? enumeratie(src(ctx.domeinNaam()), ctx.enumeratieSpecificatie())
                : datatypeVan(ctx.datatype(), null);
        model.domeinen.put(src(ctx.domeinNaam()), dt);
    }

    private Datatype enumeratie(String domein, EnumeratieSpecificatieContext ctx) {
        List<String> waarden = ctx.enumeratieWaarde().stream()
                .map(w -> unquote(w.ENUM_LITERAL().getText())).toList();
        return new Datatype.Enumeratie(domein, waarden);
    }

    /** Resolve an attribute/parameter type: a concrete datatype, or a (possibly forward) domain reference. */
    /** §3.4 the attribute-level unit clause ("de prijs Bedrag met eenheid €"): a domain-typed attribute pins a unit
     *  on its number. The clause used to be parsed and never read, so the unit was dropped and the attribute written
     *  as a bare number, where every unit check reads it. It sets the unit of a number without one; on a number that
     *  carries a unit already, a percentage (whose unit is %) or a value of another kind it has no reading, and is
     *  refused. */
    private Datatype metEenheid(Datatype dt, AttribuutSpecificatieContext a, String naam) {
        if (a.eenheidExpressie() == null || dt instanceof Datatype.Onbekend) return dt;
        Eenheid e = eenheidVan(a.eenheidExpressie());
        String reden = null;
        if (dt instanceof Datatype.Numeriek n) {
            if (!n.heeftEenheid() || n.eenheid().equals(e))
                return new Datatype.Numeriek(n.geheel(), n.decimalen(), n.teken(), e);
            reden = "het getal draagt al de eenheid '" + n.eenheid().weergave() + "'";
        } else if (dt instanceof Datatype.Percentage) {
            reden = "een percentage draagt de eenheid % ('Percentage (…) met eenheid %/<eenheid>' voor een verhouding)";
        } else {
            reden = "een " + dt.naam() + " draagt geen eenheid";
        }
        model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.ERROR,
                "attribuut '" + naam + "' met eenheid '" + src(a.eenheidExpressie()) + "': " + reden, a.getStart().getLine()));
        return dt;
    }

    private Datatype datatypeVan(DatatypeContext dt, DomeinNaamContext dom) {
        if (dt != null) return datatype(dt);
        if (dom != null) {
            Datatype resolved = model.domeinen.get(src(dom));
            return resolved != null ? resolved : new Datatype.Onbekend("domein " + src(dom));
        }
        return new Datatype.Onbekend("geen datatype");
    }

    private Datatype datatype(DatatypeContext dt) {
        if (dt.numeriekDatatype() != null) {
            NumeriekDatatypeContext n = dt.numeriekDatatype();
            // §13.3.3: the getalspecificatie is normatively mandatory ("Numeriek (" <getalspecificatie> ")"),
            // but a worked example (§4.4) omits it ("Numeriek met eenheid jaren"), so the grammar tolerates
            // its absence. Degrade to the least-restrictive reading — equal to a plain "getal" (onbeperkt
            // aantal decimalen, geen tekenbeperking) — and flag the spec-incomplete declaration.
            if (n.getalSpecificatie() == null)
                model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.WARNING,
                        "Numeriek datatype zonder verplichte getalspecificatie (§13.3.3); onbeperkte decimalen aangenomen",
                        n.getStart().getLine()));
            GetalInfo g = getalInfo(n.getalSpecificatie());
            Eenheid e = n.eenheidExpressie() != null ? eenheidVan(n.eenheidExpressie()) : null;
            return new Datatype.Numeriek(g.geheel, g.decimalen, g.teken, e);
        }
        if (dt.percentageDatatype() != null) {
            var pd = dt.percentageDatatype();
            GetalInfo g = getalInfo(pd.getalSpecificatie());
            // §3.3.5: a Percentage is always "%", optionally specialized to a rate "%/<eenheid>"
            // (grammar: "met eenheid % ( / <eenheidAfkorting> )?"); a bare or absent clause is "%".
            Eenheid e = pd.SLASH() != null
                    ? Eenheid.samengesteld(Datatype.Percentage.PROCENT, model.resolveEenheid(src(pd.eenheidAfkorting())))
                    : Datatype.Percentage.PROCENT;
            return new Datatype.Percentage(g.geheel, g.decimalen, e);
        }
        if (dt.tekstDatatype() != null) return new Datatype.Tekst();
        if (dt.booleanDatatype() != null) return new Datatype.Booleaan();
        if (dt.datumTijdDatatype() != null) {
            return dt.datumTijdDatatype().DATUM_IN_DAGEN() != null
                    ? new Datatype.DatumInDagen() : new Datatype.DatumTijd();
        }
        return new Datatype.Onbekend("datatype");
    }

    private record GetalInfo(boolean geheel, int decimalen, Datatype.Teken teken) {}

    private GetalInfo getalInfo(GetalSpecificatieContext g) {
        // No specification given: same degenerate reading as a bare "getal" — unbounded decimals
        // (decimalen 0 is the codebase's unbounded sentinel), not a whole number, no sign restriction.
        if (g == null) return new GetalInfo(false, 0, Datatype.Teken.ONBEPAALD);
        Datatype.Teken teken = Datatype.Teken.ONBEPAALD;
        if (g.NEGATIEF() != null) teken = Datatype.Teken.NEGATIEF;
        else if (g.NIET_NEGATIEF() != null) teken = Datatype.Teken.NIET_NEGATIEF;
        else if (g.POSITIEF() != null) teken = Datatype.Teken.POSITIEF;
        boolean geheel = g.GEHEEL_GETAL() != null;
        int decimalen = 0;
        if (g.DECIMALEN() != null && g.NUMBER() != null) {
            try { decimalen = Integer.parseInt(g.NUMBER().getText()); } catch (NumberFormatException ignore) {}
        }
        // §13.3.3.7: "getal met x decimalen" requires x >= 1 (aantaldecimalen is a positiefgeheelgetal).
        // A degenerate "getal met 0 decimalen" denotes a whole number — form (a) "geheel getal", capped
        // at 0 — NOT the unbounded form (c) "getal". Normalize it to geheel so it is not encoded as the
        // (geheel=false, decimalen=0) unbounded sentinel, which would silently drop its §6.1.2 ceiling.
        if (!geheel && g.DECIMALEN() != null && decimalen == 0) {
            geheel = true;
            model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.WARNING,
                    "'getal met 0 decimalen' is geen geldige getalspecificatie (§13.3.3.7 vereist >= 1 decimaal);"
                    + " gelezen als 'geheel getal'", g.getStart().getLine()));
        }
        return new GetalInfo(geheel, decimalen, teken);
    }

    private Eenheid eenheidVan(EenheidExpressieContext e) { return eenheidVan(model, e); }

    /** §13.3.3.2: {@code <eenheidexpressie> ::= <eenheidmacht> ("/" <eenheidmacht>)*} — the numerator
     *  is the first power; a single trailing {@code "/<unit>"} is the denominator of a rate unit
     *  (€/jr). A second denominator (€/u/s) is not yet modelled — keep the numerator only.
     *  Package-static so the attribute-datatype path (here) and the §13.2.17 numeric LITERAL path
     *  ({@link ExpressieBouwer}) resolve a composite (rate) unit IDENTICALLY — a literal "120 EUR/jr"
     *  must carry the same {@code Eenheid.samengesteld(EUR, jr)} an attribute "met eenheid EUR/jr"
     *  does, so §4 unit checking and the exacte cross-unit restatement apply to both alike. */
    static Eenheid eenheidVan(RegelSpraakModel model, EenheidExpressieContext e) {
        if (e == null || e.eenheidMacht().isEmpty()) return null;
        Eenheid teller = model.resolveEenheid(src(e.eenheidMacht(0).eenheidAfkorting()));
        if (e.eenheidMacht().size() == 2)
            return Eenheid.samengesteld(teller, model.resolveEenheid(src(e.eenheidMacht(1).eenheidAfkorting())));
        return teller;
    }

    // ---------------- §13.3.5 unit systems ----------------
    /** A unit's raw declaration before its conversion factor is resolved: the factor as the decimal the unit record
     *  carries and as the exact fraction the wire writes, both read from the one written number. */
    private record EenheidRuw(java.util.List<String> afkortingen, String woord, String meervoud,
                              java.math.BigDecimal omrekenN, Eenheidstelsel.Factor omrekenExact, String omrekenNaar) {}

    private void eenheidsysteem(EenheidsysteemDefinitionContext ctx) {
        Eenheidstelsel stelsel = new Eenheidstelsel(src(ctx.eenheidsysteemNaam()));
        // pass 1: collect raw definitions keyed by primary abbreviation (or word)
        java.util.Map<String, EenheidRuw> ruw = new java.util.LinkedHashMap<>();
        java.util.Map<String, Integer> regels = new java.util.HashMap<>();
        // every abbreviation a declaration writes, under the name identity: {as written, the unit's primary}
        java.util.Map<String, String[]> geschreven = new java.util.HashMap<>();
        for (EenheidDefinitieContext u : ctx.eenheidDefinitie()) {
            java.util.List<String> afk = u.eenheidAfkorting().stream().map(DefinitionResolver::src).toList();
            String primair = !afk.isEmpty() ? afk.get(0) : (u.eenheidWoord() != null ? src(u.eenheidWoord()) : null);
            if (primair == null) continue;
            // WF-009, one unit per abbreviation under the name identity the emitter checks too: a second declaration
            // of an abbreviation would replace the first in the maps below without a word, so it is refused by name
            // and the first declaration stands
            if (tweedeAfkorting(stelsel.naam, afk.isEmpty() ? List.of(primair) : afk, geschreven, u)) continue;
            java.math.BigDecimal n = null; Eenheidstelsel.Factor exact = null; String naar = null;
            if (u.omrekenSpecificatie() != null) {                       // "= 12 mnd" or "= 1/1000 m"
                String getal = u.omrekenSpecificatie().NUMBER().getText();
                n = rationeel(getal, primair);
                if (n != null) exact = exacteFactor(getal);
                naar = src(u.omrekenSpecificatie().eenheidAfkorting());
            }
            ruw.put(primair, new EenheidRuw(afk, u.eenheidWoord() != null ? src(u.eenheidWoord()) : null,
                    u.meervoudsvorm() != null ? src(u.meervoudsvorm()) : null, n, exact, naar));
            regels.put(primair, u.getStart().getLine());
        }
        // pass 2: resolve each unit's canonical + factor, then register every spelling
        java.util.Map<String, Eenheid> opgelost = new java.util.HashMap<>();
        for (String primair : ruw.keySet()) {
            Eenheid e = resolveFactor(primair, ruw, stelsel.naam, opgelost, new java.util.HashSet<>(),
                    stelsel.exacteFactoren);
            EenheidRuw r = ruw.get(primair);
            for (String a : r.afkortingen()) stelsel.eenheden.put(a, e);
            if (r.woord() != null) stelsel.aliassen.put(RegelSpraakModel.normaliseerTijdseenheid(r.woord()), e);
            if (r.meervoud() != null) stelsel.aliassen.put(RegelSpraakModel.normaliseerTijdseenheid(r.meervoud()), e);
        }
        model.eenheidstelsels.put(stelsel.naam, stelsel);
        // the wire writes a factor as a fraction of two integers from 1 to 2^62 - 1 (IR-SPEC ch. 2.5): a declared zero
        // or negative factor, and one its chain of declarations grows past that bound, have no document. Before this
        // check the first ended in the emitter's exception; each is refused in the loader's sentence now.
        java.math.BigInteger grens = java.math.BigInteger.TWO.pow(62).subtract(java.math.BigInteger.ONE);
        for (var en : stelsel.exacteFactoren.entrySet()) {
            Eenheidstelsel.Factor f = en.getValue();
            if (f.num().signum() > 0 && f.num().compareTo(grens) <= 0 && f.den().compareTo(grens) <= 0) continue;
            model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.ERROR,
                    "eenheid '" + en.getKey() + "' van eenheidstelsel '" + stelsel.naam + "': a conversion factor is an "
                            + "exact rational {num, den} whose two integers are at least 1 and at most "
                            + "4611686018427387903 (2^62 - 1), got " + "{\"num\":" + f.num() + ",\"den\":" + f.den() + "}",
                    regels.getOrDefault(en.getKey(), 0)));
        }
    }

    /** Whether one of {@code afkortingen} — the abbreviations one unit declaration writes — was written before by an
     *  EARLIER declaration of the same system, under the name identity ({@code IrEmitter.norm}); if so an ERROR names
     *  the pair. Otherwise the declaration claims them. Two spellings of one abbreviation inside ONE declaration
     *  ("kg KG") are that unit's own spellings and no collision. */
    private boolean tweedeAfkorting(String stelsel, List<String> afkortingen, java.util.Map<String, String[]> geschreven,
                                    EenheidDefinitieContext u) {
        for (String a : afkortingen) {
            String[] eerder = geschreven.get(dev.uchar.regelspraak.emit.IrEmitter.norm(a));
            if (eerder == null) continue;
            // the second declaration beside the first on the wire is what the verifier refuses, in these words
            model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.ERROR,
                    "eenheidstelsel '" + stelsel + "', " + (a.equals(eerder[0]) ? "" : "als '" + eerder[0] + "' ")
                            + "al gedeclareerd voor de eenheid '" + eerder[1] + "': the unit '" + a
                            + "' is declared twice in the unit system '" + stelsel + "': one Unit per abbreviation",
                    u.getStart().getLine()));
            return true;
        }
        for (String a : afkortingen)
            geschreven.putIfAbsent(dev.uchar.regelspraak.emit.IrEmitter.norm(a),
                    new String[]{a, afkortingen.get(0)});
        return false;
    }

    /** Parse an omrekenfactor, which the lexer hands over as a single rational NUMBER
     *  token: a whole number ("12"), a decimal ("1,5"), or the §13.3.5.2 "1/N" form
     *  ("1/1000"). A malformed factor is reported, not silently swallowed. */
    private java.math.BigDecimal rationeel(String tekst, String eenheid) {
        try {
            int slash = tekst.indexOf('/');
            if (slash >= 0)
                return new java.math.BigDecimal(tekst.substring(0, slash))
                        .divide(new java.math.BigDecimal(tekst.substring(slash + 1)), java.math.MathContext.DECIMAL64);
            return new java.math.BigDecimal(tekst.replace(',', '.'));
        } catch (RuntimeException e) {
            model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.WARNING,
                    "onleesbare omrekenfactor '" + tekst + "' bij eenheid '" + eenheid + "'", 0));
            return null;
        }
    }

    /** Reduce a unit to its system canonical and the factor to reach it: a base unit
     *  is its own canonical (factor 1); "<u> = N <v>" inherits v's canonical with
     *  factor N·factor(v). Memoised; a cycle or external target stops at a base.
     *  {@code exact} receives the same factor as an exact fraction, step for step, so the two agree on every
     *  unit, the cycle's included. */
    private Eenheid resolveFactor(String abbrev, java.util.Map<String, EenheidRuw> ruw, String stelsel,
                                  java.util.Map<String, Eenheid> memo, java.util.Set<String> bezig,
                                  java.util.Map<String, Eenheidstelsel.Factor> exact) {
        if (memo.containsKey(abbrev)) return memo.get(abbrev);
        EenheidRuw r = ruw.get(abbrev);
        Eenheid e;
        if (r == null || r.omrekenNaar() == null || r.omrekenN() == null || !bezig.add(abbrev)) {
            e = new Eenheid(abbrev, stelsel, abbrev, java.math.BigDecimal.ONE);
            exact.put(abbrev, Eenheidstelsel.Factor.EEN);
        } else {
            Eenheid basis = resolveFactor(r.omrekenNaar(), ruw, stelsel, memo, bezig, exact);
            bezig.remove(abbrev);
            e = new Eenheid(abbrev, stelsel, basis.canoniek(), r.omrekenN().multiply(basis.factorNaarCanoniek()));
            exact.put(abbrev, r.omrekenExact().maal(exact.get(basis.afkorting())));
        }
        memo.put(abbrev, e);
        return e;
    }

    /** The omrekenfactor as an exact fraction, from the same NUMBER text {@link #rationeel} reads and failing on the
     *  same texts (the caller asks only after that reading succeeded): "12" is 12/1, "1,5" is 3/2, "1/24" is 1/24. */
    private static Eenheidstelsel.Factor exacteFactor(String tekst) {
        int slash = tekst.indexOf('/');
        if (slash >= 0)
            return new Eenheidstelsel.Factor(new java.math.BigInteger(tekst.substring(0, slash)),
                    new java.math.BigInteger(tekst.substring(slash + 1)));
        java.math.BigDecimal d = new java.math.BigDecimal(tekst.replace(',', '.'));
        return d.scale() <= 0
                ? new Eenheidstelsel.Factor(d.toBigIntegerExact(), java.math.BigInteger.ONE)
                : new Eenheidstelsel.Factor(d.unscaledValue(), java.math.BigInteger.TEN.pow(d.scale()));
    }

    // ---------------- §13.3.8 parameters ----------------
    private void parameter(ParameterDefinitionContext ctx) {
        String naam = src(ctx.parameterNaam());
        // §13.3.6/§13.3.8 a parameter may carry a tijdlijn ("voor elk jaar") -> its runtime value is a timeline.
        Granulariteit g = ctx.tijdlijn() != null ? granulariteitVan(ctx.tijdlijn()) : null;
        String vast = null;
        if (g != null) {
            int aantal = aantalVan(ctx.tijdlijn());
            // IR-SPEC ch. 2.10: a parameter's facet has count 1 while multi-period grids are open, and the model's
            // parameter has no count to write — refused in WF-005's words rather than written as one period
            if (aantal != 1)
                model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.ERROR,
                        "parameter '" + naam + "': " + "the timeline facet of parameter '" + naam + "' has count "
                                + aantal + " > 1: a parameter's timeline is one period while multi-period grids are "
                                + "open (RUNTIME-SEMANTICS ch. 3.12)", ctx.getStart().getLine()));
            else vast = vastStartpuntVan(ctx.tijdlijn(), g, aantal);
        }
        model.parameters.put(naam, new Parameter(naam, datatypeVan(ctx.datatype(), ctx.domeinNaam()), g, vast));
    }

    // ---------------- §13.3.6 timelines ----------------
    private void tijdlijn(TijdlijnDefinitieContext ctx) {
        boolean variabel = src(ctx).toLowerCase().contains("met variabel startpunt");
        String naam = src(ctx.tijdlijnNaam());
        Granulariteit gran = granulariteitVanDefinitie(ctx);
        int aantal = aantalVanDefinitie(ctx);
        String vast = gran == null ? null : vastStartpunt(gran, aantal, ctx.vastStartpuntDatum(),
                ctx.vastStartpuntMaand(), ctx.vastStartpuntKwartaal(), ctx.vastStartpuntJaar());
        // §13.3.6.10: a "voor elke <tijdlijn>" reference adopts the FIRST timeline of that (normalised) name via
        // findFirst. Warn only when an earlier same-name timeline declares a DIFFERENT periodicity (granularity
        // or count) — then the reference silently disagrees; an identical redeclaration is harmless.
        String genormaliseerd = normaliseerTijdlijnNaam(naam);
        if (model.tijdlijnen.stream().anyMatch(tl -> normaliseerTijdlijnNaam(tl.naam()).equals(genormaliseerd)
                && (tl.granulariteit() != gran || tl.aantal() != aantal)))
            model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.WARNING,
                    "tijdlijn '" + naam + "' is meermaals met verschillende periodiciteit gedefinieerd; een 'voor "
                            + "elke <naam>' verwijzing neemt die van de eerste (§13.3.6.10)",
                    ctx.tijdlijnNaam().getStart().getLine()));
        model.tijdlijnen.add(new Tijdlijn(naam, variabel, gran, aantal, vast));
    }

    /** §13.3.6.3–.7 the fixed start point of a tijdlijn attachment in its wire spelling (IR-SPEC ch. 2.8), or null
     *  on the calendar grid. The inline forms write it here; the named form ("voor elke &lt;tijdlijn&gt;") adopts
     *  the definition's with its granularity and count, by the same first-match rule. */
    private String vastStartpuntVan(TijdlijnContext t, Granulariteit g, int aantal) {
        if (t.enkelvoudigePeriode() != null) {
            EnkelvoudigePeriodeContext e = t.enkelvoudigePeriode();
            return vastStartpunt(g, aantal, e.vastStartpuntDatum(), e.vastStartpuntMaand(), e.vastStartpuntKwartaal(),
                    e.vastStartpuntJaar());
        }
        if (t.meervoudigePeriode() != null)
            return vastStartpunt(g, aantal, t.meervoudigePeriode().vastStartpuntDatum(), null, null, null);
        if (t.ELKE() != null && t.tijdlijnNaam() != null) {
            String ref = normaliseerTijdlijnNaam(src(t.tijdlijnNaam()));
            // the timeline first, then its start point: a timeline on the calendar grid has none (null)
            return model.tijdlijnen.stream().filter(tl -> normaliseerTijdlijnNaam(tl.naam()).equals(ref))
                    .findFirst().map(Tijdlijn::vastStartpunt).orElse(null);
        }
        return null;
    }

    /** The wire spelling of the fixed start point the source writes, at most one of the four present
     *  (IR-SPEC ch. 2.8): a date on a week grid and on a grid of several units, the canonical day-month on a month,
     *  quarter or year grid of one unit — so a date on such a grid ("elke 1 maanden startend op 15-3-2026") is
     *  written as the day-month it lays. The calendar's own start point is null: day 1 of the month, day 1 of
     *  months 1, 4, 7 and 10 and day 1 of month 1 are the timeline without one, and so is any date on a day grid of
     *  one unit, whose walls are all day boundaries. A source that names no day is refused by name here and yields
     *  null, so no document is written with it. */
    private String vastStartpunt(Granulariteit g, int aantal, VastStartpuntDatumContext datum,
                                 VastStartpuntMaandContext maand, VastStartpuntKwartaalContext kwartaal,
                                 VastStartpuntJaarContext jaar) {
        if (datum != null) {
            java.time.LocalDate d = startdatum(datum);
            if (d == null) return null;
            if (aantal >= 2 || g == Granulariteit.WEEK) return d.toString();
            if (g == Granulariteit.DAG) return null;
            return kalenderLeeg(dev.uchar.regelspraak.kern.VastStartpunt.canoniek(g, d.getMonthValue(), d.getDayOfMonth()));
        }
        if (maand != null) {
            Integer dag = geheel(maand.NUMBER());
            if (dag == null || dag < 1 || dag > 31) return weiger("§13.3.6.4", maand, "noemt geen dag van de maand");
            return kalenderLeeg(dev.uchar.regelspraak.kern.VastStartpunt.canoniek(g, 1, dag));
        }
        if (kwartaal != null) {
            List<Integer> maanden = kwartaal.kwartaalGroep().NUMBER().stream().map(n -> geheel(n)).toList();
            Integer eerste = maanden.get(0);
            if (eerste == null || eerste < 1 || eerste > 3
                    || !maanden.equals(List.of(eerste, eerste + 3, eerste + 6, eerste + 9)))
                return weiger("§13.3.6.5", kwartaal, "noemt geen reeks kwartaalmaanden: die is 1, 4, 7 en 10, "
                        + "2, 5, 8 en 11 of 3, 6, 9 en 12");
            Integer dag = geheel(kwartaal.NUMBER());
            if (dag == null || dag < 1 || dag > 31) return weiger("§13.3.6.5", kwartaal, "noemt geen dag van de maand");
            return kalenderLeeg(dev.uchar.regelspraak.kern.VastStartpunt.canoniek(g, eerste, dag));
        }
        if (jaar != null) {
            Integer dag = geheel(jaar.NUMBER(0)), m = geheel(jaar.NUMBER(1));
            if (m == null || m < 1 || m > 12) return weiger("§13.3.6.6", jaar, "noemt geen maand");
            String c = dag == null ? null : dev.uchar.regelspraak.kern.VastStartpunt.canoniek(g, m, dag);
            if (c == null) return weiger("§13.3.6.6", jaar, "noemt een dag die maand " + m + " niet heeft");
            return kalenderLeeg(c);
        }
        return null;
    }

    /** The calendar's own start point is written as none (IR-SPEC ch. 2.8). */
    private static String kalenderLeeg(String spelling) {
        return "--01-01".equals(spelling) ? null : spelling;
    }

    private static final java.util.regex.Pattern STARTDATUM =
            java.util.regex.Pattern.compile("(?:dd\\. +)?(\\d{1,2})-(\\d{1,2})-(\\d{4})");

    /** The day of a §13.3.6.3 "startend op &lt;datum&gt;", or null after refusing a source that names none: a periode
     *  starts on a day, so a time of day has no place in it, and a bare number or an impossible date names no day. */
    private java.time.LocalDate startdatum(VastStartpuntDatumContext datum) {
        TerminalNode d = datum.datumWaarde().DATE();
        java.util.regex.Matcher m = d == null ? null : STARTDATUM.matcher(d.getText());
        if (m != null && !m.matches() && d.getText().contains(":"))
            return weigerDatum(datum, "draagt een tijdstip, en een tijdvak begint op een dag");
        if (m == null || !m.matches()) return weigerDatum(datum, "noemt geen dag van de kalender");
        try {
            return java.time.LocalDate.of(Integer.parseInt(m.group(3)), Integer.parseInt(m.group(2)),
                    Integer.parseInt(m.group(1)));
        } catch (java.time.DateTimeException geenDag) {
            return weigerDatum(datum, "noemt geen dag van de kalender");
        }
    }

    private java.time.LocalDate weigerDatum(VastStartpuntDatumContext datum, String reden) {
        weiger("§13.3.6.3", datum, reden);
        return null;
    }

    /** A source start point the wire cannot spell, refused by name with its section. */
    private String weiger(String sectie, ParserRuleContext ctx, String reden) {
        model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.ERROR,
                "tijdlijn: het vaste startpunt '" + src(ctx) + "' (" + sectie + ") " + reden,
                ctx.getStart().getLine()));
        return null;
    }

    /** A NUMBER token as a whole number, or null when it is none (a decimal or a fraction). */
    private static Integer geheel(TerminalNode n) {
        if (n == null || !n.getText().matches("\\d{1,9}")) return null;
        return Integer.parseInt(n.getText());
    }

    /** §13.3.6 the {@link Granulariteit} of a tijdlijn ATTACHMENT on an attribute/kenmerk. The single-period
     *  form carries it in the token ("voor elke dag" → DAG), the multi-period form in the plural unit ("voor
     *  elke 5 maanden" → MAAND). The named form ("voor elke &lt;tijdlijn&gt;") adopts its definition's
     *  granularity, resolved against {@link RegelSpraakModel#tijdlijnen} elsewhere — null here. */
    private Granulariteit granulariteitVan(TijdlijnContext t) {
        if (t == null) return null;
        EnkelvoudigePeriodeContext e = t.enkelvoudigePeriode();
        if (e != null) {
            if (e.ELKE_DAG() != null)     return Granulariteit.DAG;
            if (e.ELKE_WEEK() != null)    return Granulariteit.WEEK;
            if (e.ELKE_MAAND() != null)   return Granulariteit.MAAND;
            if (e.ELK_KWARTAAL() != null) return Granulariteit.KWARTAAL;
            if (e.ELK_JAAR() != null)     return Granulariteit.JAAR;
        }
        if (t.meervoudigePeriode() != null)
            return granulariteitVanMeervoud(t.meervoudigePeriode().tijdseenhedenMeervoud());
        // §13.3.6.9 the named form "voor elke <tijdlijn>" adopts the granularity of the standalone Tijdlijn
        // definition of that name (registered in pass 1). The reference drops the leading lidwoord the
        // definition carries ("elke periode …" vs "Tijdlijn de periode …") and may wrap across lines, so
        // match on a whitespace-collapsed, lidwoord-stripped name. An unresolved name is an ERROR: the member
        // would lose its timeline without a word and be written as a single value. "voor elke 2 maanden" without
        // its start date is such a name, since the multi-period form (§13.3.6.7) does not parse without one.
        if (t.ELKE() != null && t.tijdlijnNaam() != null) {
            String ref = normaliseerTijdlijnNaam(src(t.tijdlijnNaam()));
            Granulariteit g = model.tijdlijnen.stream()
                    .filter(tl -> normaliseerTijdlijnNaam(tl.naam()).equals(ref))
                    .map(Tijdlijn::granulariteit).findFirst().orElse(null);
            if (g == null) {
                String geschreven = src(t.tijdlijnNaam()).trim().replaceAll("\\s+", " ");
                model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.ERROR,
                        "tijdlijn 'voor elke " + geschreven + "': " + (MEERVOUDIGE_PERIODE.matcher(geschreven).matches()
                                ? "een tijdlijn van meer perioden noemt haar startdatum ('voor elke " + geschreven
                                        + " startend op <datum>', §13.3.6.7); zonder startdatum leest deze frontend '"
                                        + geschreven + "' als de naam van een tijdlijn, en die is niet gedeclareerd"
                                : "de tijdlijn '" + geschreven + "' is niet gedeclareerd ('Tijdlijn " + geschreven
                                        + " heeft tijdvakken van …', §13.3.6.10)"),
                        t.getStart().getLine()));
            }
            return g;
        }
        return null;
    }

    /** A multi-period form written as a name: a count and a plural time unit ("2 maanden"). */
    private static final java.util.regex.Pattern MEERVOUDIGE_PERIODE =
            java.util.regex.Pattern.compile("(?i)\\d+ (dagen|weken|maanden|kwartalen|jaren)");

    /** Normalise a tijdlijn name for cross-reference matching — the shared model implementation
     *  (§9.9 startpuntbepaling resolution matches by the same rule). */
    private static String normaliseerTijdlijnNaam(String s) {
        return RegelSpraakModel.normaliseerTijdlijnNaam(s);
    }

    /** §13.3.6.10 the {@link Granulariteit} a standalone tijdlijn DEFINITION declares ("heeft tijdvakken van
     *  een jaar" → JAAR), from the singular unit token or the plural-period unit. */
    private static Granulariteit granulariteitVanDefinitie(TijdlijnDefinitieContext ctx) {
        if (ctx.DAG() != null)      return Granulariteit.DAG;
        if (ctx.WEEK() != null)     return Granulariteit.WEEK;
        if (ctx.MAAND() != null)    return Granulariteit.MAAND;
        if (ctx.KWARTAAL() != null) return Granulariteit.KWARTAAL;
        if (ctx.JAAR() != null)     return Granulariteit.JAAR;
        return granulariteitVanMeervoud(ctx.tijdseenhedenMeervoud());
    }

    /** §13.3.6.7 the period count of a tijdlijn attachment: the "5" in "voor elke 5 maanden". A single-period
     *  form ("voor elke maand") carries none (1); the named form "voor elke &lt;tijdlijn&gt;" adopts its
     *  definition's count (so "heeft tijdvakken van 5 maanden" → 5), mirroring {@link #granulariteitVan}. */
    private int aantalVan(TijdlijnContext t) {
        if (t == null) return 1;
        if (t.meervoudigePeriode() != null) return aantalVanMeervoud(t.meervoudigePeriode().tijdseenhedenMeervoud());
        if (t.ELKE() != null && t.tijdlijnNaam() != null) {
            String ref = normaliseerTijdlijnNaam(src(t.tijdlijnNaam()));
            return model.tijdlijnen.stream()
                    .filter(tl -> normaliseerTijdlijnNaam(tl.naam()).equals(ref))
                    .map(Tijdlijn::aantal).findFirst().orElse(1);
        }
        return 1;
    }

    /** §13.3.6.10 the period count a standalone tijdlijn DEFINITION declares ("heeft tijdvakken van 5 maanden" →
     *  5), or 1 for the singular "een &lt;unit&gt;" form. */
    private static int aantalVanDefinitie(TijdlijnDefinitieContext ctx) {
        return aantalVanMeervoud(ctx.tijdseenhedenMeervoud());
    }

    /** The NUMBER of a plural period unit ("5 maanden" → 5), or 1 when absent. */
    private static int aantalVanMeervoud(TijdseenhedenMeervoudContext m) {
        if (m == null || m.NUMBER() == null) return 1;
        try { return Integer.parseInt(m.NUMBER().getText()); } catch (NumberFormatException e) { return 1; }
    }

    /** §13.3.6.8 the plural time-unit of a multi-period tijdlijn → its singular {@link Granulariteit}. */
    private static Granulariteit granulariteitVanMeervoud(TijdseenhedenMeervoudContext m) {
        if (m == null) return null;
        if (m.DAGEN() != null)     return Granulariteit.DAG;
        if (m.WEKEN() != null)     return Granulariteit.WEEK;
        if (m.MAANDEN() != null)   return Granulariteit.MAAND;
        if (m.KWARTALEN() != null) return Granulariteit.KWARTAAL;
        if (m.JAREN() != null)     return Granulariteit.JAAR;
        return null;
    }

    /** The optional tijdlijn attachment on a kenmerk declaration (both the bijvoeglijk and naamwoord forms
     *  carry one), or null. */
    private static TijdlijnContext tijdlijnVan(KenmerkSpecificatieContext k) {
        if (k instanceof BijvoeglijkKenmerkContext b) return b.tijdlijn();
        if (k instanceof NaamwoordKenmerkContext n) return n.tijdlijn();
        return null;
    }

    // ---------------- §13.3.7 dimensions ----------------
    private void dimensie(DimensieDefinitionContext ctx) {
        List<String> waarden = ctx.labelWaardeSpecificatie().stream()
                .map(l -> src(l.dimensieWaarde())).toList();
        // §3.6/§13.3.7.2: "na het attribuut met voorzetsel <vz>" gives the suffix-read preposition; the
        // "voor het attribuut zonder voorzetsel" form has no voorzetsel (null) — a prefix (bijvoeglijk) read.
        VoorzetselSpecificatieContext vzs = ctx.voorzetselSpecificatie();
        String voorzetsel = vzs != null && vzs.voorzetsel() != null ? src(vzs.voorzetsel()) : null;
        model.dimensies.put(src(ctx.dimensieNaam()),
                new Dimensie(src(ctx.dimensieNaam()), src(ctx.dimensieNaamMeervoud()), voorzetsel, waarden));
    }

    // ---------------- §13.3.9 fact types ----------------
    private void feittype(FeitTypeDefinitionContext ctx) {
        Feittype ft = new Feittype(src(ctx.feitTypeNaam()));
        ft.wederkerig = ctx.WEDERKERIG_FEITTYPE() != null;
        List<Rol> rollen = new ArrayList<>();
        boolean hersteld = hersteld(ctx);
        java.util.Set<ParserRuleContext> opVorigeRegel = hersteld ? java.util.Set.of()
                : meldLedenOpVorigeRegel(ctx, ctx.rolSpecificatie(), RolSpecificatieContext.class, "feittype '" + ft.naam + "'");
        for (RolSpecificatieContext rs : ctx.rolSpecificatie()) {
            String rolNaam = src(rs.rolNaam());
            String meervoud = rs.meervoudsvorm() != null ? src(rs.meervoudsvorm()) : null; // declared "(mv: …)"
            if (!hersteld && !opVorigeRegel.contains(rs))
                meldOnleesbareNaam("feittype '" + ft.naam + "'", "de rol", rolNaam, rs.getStart().getLine());
            if (!hersteld && rs.objectTypeNaam() != null && rs.objectTypeNaam().getStart() != null)
                rolVullers.add(new RolVuller(ft.naam, rolNaam, src(rs.objectTypeNaam()),
                        rs.objectTypeNaam().getStart().getLine()));
            // §13.3.9.3: feitTypeNaam excludes 'de'/'het' (it stops at the role's article), so a feittype
            // name that itself contains an article ("bonus van DE groep") truncates there and the leftover
            // "de <woord>" is absorbed by this greedy rolNaam — corrupting it to "<woord> de <echte rol>"
            // with no error node. A role name MAY legitimately contain an article, but only after a
            // preposition ("eigenaar van de woning", "behandelaar van het dossier"); the corruption's article
            // instead follows a bare noun ("groep de bonusgroep"). So flag a 'de'/'het' that has a word
            // before AND after it and is NOT preceded by a preposition — the tell-tale of a swallowed
            // name+article boundary — without warning on spec-legal "… van de/het …" names.
            // Beside the line check, not instead of it: that check names the line and the word where a role
            // begins on the head's line, this guess names the likely cause, and it alone sees the swallow where
            // the role name runs on over the next line.
            if (lidwoordNaBareWoord(rolNaam))
                model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.WARNING,
                        "feittype '" + ft.naam + "': de rolnaam '" + rolNaam + "' bevat een lidwoord (de/het) na een"
                        + " zelfstandig woord — waarschijnlijk bevat de feittypenaam een 'de'/'het' die de naam"
                        + " afkapt en in de rol wordt geabsorbeerd (§13.3.9.3); hernoem het feittype zonder lidwoord",
                        ctx.feitTypeNaam().getStart().getLine()));
            rollen.add(new Rol(rolNaam, src(rs.objectTypeNaam()), meervoud));
            rolRegels.computeIfAbsent(ft, k -> new java.util.HashMap<>()).putIfAbsent(rolNaam, rs.getStart().getLine());
        }
        // §13.3.9.1 cardinaliteit: the line "<q1> <rol1> … <q2> <rol2>" marks each role one or
        // meerdere. A wederkerig role is enkelvoudig (§3.11), so only a normal feittype's roles
        // take a 'meerdere' mark; an unmatched/absent line leaves a role single (fail-open).
        if (!ft.wederkerig && ctx.feitTypeCardinaliteit() != null)
            rollen = markeerKardinaliteit(rollen, ctx.feitTypeCardinaliteit());
        ft.rollen.addAll(rollen);
        model.feittypen.put(ft.naam, ft);
    }

    /** The prepositions a role name may carry before an internal article (the §13.4 name words: a role
     *  like "eigenaar van de woning" / "deel in het geheel" is legitimate). */
    private static final java.util.Set<String> VOORZETSELS =
            java.util.Set.of("van", "op", "in", "voor", "over", "bij", "tot", "per", "met");

    /** Whether {@code rolNaam} contains a 'de'/'het' that has a word both before and after it and is NOT
     *  preceded by a preposition — the signature of a feittype-name article (§13.3.9.3) swallowed into the
     *  role, as opposed to a legitimate "… van de/het …" role name. */
    private static boolean lidwoordNaBareWoord(String rolNaam) {
        String[] w = rolNaam.toLowerCase().split("\\s+");
        for (int i = 1; i + 1 < w.length; i++)            // an article with a word before AND after it
            if ((w[i].equals("de") || w[i].equals("het")) && !VOORZETSELS.contains(w[i - 1]))
                return true;
        return false;
    }

    /** Tag each role single/meerdere from the cardinaliteit line. The line alternates a
     *  quantifier terminal (één | meerdere) with a noun phrase; the quantifier qualifies the
     *  phrase that follows it. Per the §13.3.9.1 EBNF a segment is
     *  {@code (één|meerdere) <rolnaam> <relatiebeschrijving>}: the rolnaam is the LEADING phrase
     *  and the free-text relatiebeschrijving prose follows it. So a role is matched against the
     *  longest LEADING sub-phrase of the deel (not word-by-word). This binds a multi-word role
     *  name whole ("deelnemer met recht op korting") and never marks a role that a trailing
     *  relatiebeschrijving merely names ("… behoren de groep toe aan …" leaves the groep role untouched)
     *  — the two failure modes word-by-word matching had. */
    private List<Rol> markeerKardinaliteit(List<Rol> rollen, FeitTypeCardinaliteitContext card) {
        List<Rol> uit = new ArrayList<>(rollen);
        // Collect the (meervoudig, head-words) segments in order — there are exactly two for a
        // non-wederkerig feittype (the grammar fixes it), one per role.
        List<Boolean> meervoud = new ArrayList<>();
        List<String[]> segmenten = new ArrayList<>();
        boolean meervoudigVolgend = false;
        for (int i = 0; i < card.getChildCount(); i++) {
            ParseTree ch = card.getChild(i);
            if (ch instanceof TerminalNode t) {
                meervoudigVolgend = t.getSymbol().getType() == RegelSpraak.MEERDERE;
            } else if (ch instanceof CardinaliteitDeelContext deel) {
                segmenten.add(src(deel).split("\\s+"));
                meervoud.add(meervoudigVolgend);
            }
        }
        // Bind each segment to a role by NAME first (longest leading sub-phrase). A segment whose noun
        // matches no role — an UNDECLARED irregular plural the language does not synthesise (§footnote 2:
        // "vakken"/"pupillen" need consonant doubling, refused by both the best-effort and MeervoudMatcher)
        // — is then assigned by ELIMINATION: a non-wederkerig feittype has exactly two roles, so if the
        // other segment named one role, this segment must be the remaining one. No plural guessing, just
        // the 2-role structure. (When BOTH segments fail to name a role there is no signal, so both stay.)
        int[] segRol = new int[segmenten.size()];
        boolean[] rolBezet = new boolean[uit.size()];
        for (int s = 0; s < segmenten.size(); s++) {
            int j = vindKop(uit, segmenten.get(s), rolBezet);
            segRol[s] = j;
            if (j >= 0) rolBezet[j] = true;
        }
        for (int s = 0; s < segmenten.size(); s++) {
            if (segRol[s] >= 0) continue;
            int rest = -1, n = 0;
            for (int j = 0; j < uit.size(); j++) if (!rolBezet[j]) { rest = j; n++; }
            if (n == 1) { segRol[s] = rest; rolBezet[rest] = true; }
        }
        for (int s = 0; s < segmenten.size(); s++) {
            int j = segRol[s];
            if (j >= 0 && uit.get(j).meervoudig() != meervoud.get(s))
                uit.set(j, new Rol(uit.get(j).naam(), uit.get(j).objecttypeNaam(), uit.get(j).meervoud(), meervoud.get(s)));
        }
        return uit;
    }

    /** The index of the role named by the LEADING sub-phrase of {@code woorden} (the rolnaam head),
     *  or -1 when none matches: try the longest leading phrase first so a multi-word role binds whole
     *  and a more specific role wins over a shorter prefix; stop at the first match so trailing
     *  relatiebeschrijving words never name another role. An already-bound role is skipped so the two
     *  segments cannot both claim the same role. */
    private int vindKop(List<Rol> uit, String[] woorden, boolean[] rolBezet) {
        for (int len = woorden.length; len >= 1; len--) {
            String kandidaat = String.join(" ", java.util.Arrays.copyOfRange(woorden, 0, len));
            for (int j = 0; j < uit.size(); j++)
                if (!rolBezet[j] && RegelSpraakModel.rolNaamMatcht(uit.get(j), kandidaat)) return j;
        }
        return -1;
    }

    /** The two checks on the members of one head: the line check, and the readability of every name it did not
     *  refuse — a refused member is a misreading, so its name is not the one the author declared. */
    private void ledenNaKop(Objecttype ot, ParserRuleContext definitie, List<ObjectTypeMemberContext> leden) {
        if (hersteld(definitie)) return;
        String drager = "objecttype '" + ot.naam + "'";
        java.util.Set<ParserRuleContext> geweigerd = meldLedenOpVorigeRegel(definitie, leden, ObjectTypeMemberContext.class, drager);
        for (ObjectTypeMemberContext m : leden) {
            if (geweigerd.contains(m)) continue;
            if (m.attribuutSpecificatie() != null)
                meldOnleesbareNaam(drager, "het attribuut", naam(m.attribuutSpecificatie().lowerNaam()), m.getStart().getLine());
            else if (m.kenmerkSpecificatie() != null)
                meldOnleesbareNaam(drager, "het kenmerk", kenmerkNaam(m.kenmerkSpecificatie()), m.getStart().getLine());
        }
    }

    // ---------------- a declaration on the line where the previous one ended ----------------
    /** Whether the parser recovered inside {@code t}: an error node, or a rule that caught a recognition error. The
     *  boundaries of a recovered definition are the recovery's and not the author's, so the checks below leave it
     *  alone; its syntax error is reported already, and it is the one to act on. */
    private static boolean hersteld(ParseTree t) {
        if (t instanceof ErrorNode) return true;
        if (t instanceof ParserRuleContext p && p.exception != null) return true;
        for (int i = 0; i < t.getChildCount(); i++) if (hersteld(t.getChild(i))) return true;
        return false;
    }

    /** Where a head ends: its last token before the first member or role, and whether that token ends a NAME — the
     *  type name of the head or of its 'is een' clause, or the fact type's name — rather than an annotation. */
    private record KopEinde(Token token, boolean opNaam) {}

    private static KopEinde kopEinde(ParserRuleContext definitie, Class<?> lid) {
        Token eind = null;
        ParseTree laatste = null;
        for (ParseTree kind : definitie.children == null ? List.<ParseTree>of() : definitie.children) {
            if (lid.isInstance(kind)) break;
            Token t = kind instanceof TerminalNode n ? n.getSymbol()
                    : kind instanceof ParserRuleContext p ? p.getStop() : null;
            if (t == null || t.getTokenIndex() < 0) continue;   // a token the error recovery conjured stands nowhere
            eind = t;
            laatste = kind;
        }
        return eind == null ? null : new KopEinde(eind, eindigtOpNaam(laatste));
    }

    private static boolean eindigtOpNaam(ParseTree t) {
        if (t instanceof ObjectTypeNaamContext || t instanceof FeitTypeNaamContext) return true;
        if (t instanceof ObjectTypeSupertypeContext s && s.getChildCount() > 0)
            return eindigtOpNaam(s.getChild(s.getChildCount() - 1));
        return false;
    }

    /** A member (attribute, characteristic) or a role that begins on the line where the head, the previous member or
     *  the previous role ended is refused. The lexer skips every line end, so a name the grammar cut short leaves
     *  its rest to be read as a declaration of its own, and that reading went out without a word: one attribute
     *  became two, a fact type lost the words of its name to its first role (grammar/DIVERGENCES.md, "Namen in
     *  declaratiekoppen"). The lines of the tokens decide, never their capitals. A member that runs on over the next
     *  line (the §3.9 example) begins on a line of its own and stays admitted; a heading ('---') runs to the end of
     *  its line, so nothing begins after it there. The sentence names the word the declaration begins with and the
     *  token the previous one ended on, and, where a keyword is the likely cut, the spelling that does read: the
     *  keyword joined by a hyphen to its neighbour. Returns the refused members or roles. */
    private java.util.Set<ParserRuleContext> meldLedenOpVorigeRegel(ParserRuleContext definitie,
                                                                   List<? extends ParserRuleContext> leden,
                                                                   Class<? extends ParserRuleContext> soortLid,
                                                                   String drager) {
        java.util.Set<ParserRuleContext> geweigerd = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        KopEinde kop = kopEinde(definitie, soortLid);
        if (kop == null) return geweigerd;
        Token vorige = kop.token();
        String vorigeZin = "de kop";
        boolean opNaam = kop.opNaam(), opKenmerk = false;
        boolean rol = soortLid == RolSpecificatieContext.class;
        for (ParserRuleContext lid : leden) {
            Token begin = lid.getStart(), eind = lid.getStop();
            if (begin == null || eind == null || eind.getTokenIndex() < begin.getTokenIndex()) continue;
            String lidZin = lidZin(lid);
            if (lidZin != null && begin.getLine() == vorige.getLine()) {
                String woord = begin.getText();
                String hint = "";
                if (opNaam && sleutelwoord(begin))
                    hint = " — hoort '" + woord + "' bij de naam, verbind het dan met een koppelteken aan het woord "
                            + "ervoor ('" + vorige.getText() + "-" + woord + "')";
                else if (opKenmerk)
                    hint = " — hoort 'kenmerk' bij de naam, verbind het dan met een koppelteken aan het woord erna "
                            + "('kenmerk-" + woord + "')";
                fout(drager + ": " + lidZin + " begint met '" + woord + "' op de regel waar " + vorigeZin
                        + " eindigt, direct na '" + vorige.getText() + "'; " + (rol ? "een rol" : "een lid")
                        + " begint op een eigen regel" + hint, begin.getLine());
                geweigerd.add(lid);
            }
            vorige = eind;
            vorigeZin = lidZin == null ? "de koptekst"
                    : (rol ? "de vorige rol " : "het vorige lid ") + lidZin.substring(lidZin.indexOf('\''));
            opNaam = rol;                                  // a role ends on its filler, an object-type name
            opKenmerk = !rol && eind.getType() == RegelSpraak.KENMERK;
        }
        return geweigerd;
    }

    /** "het attribuut 'naam'", "het kenmerk 'naam'" or "de rol 'naam'"; null for a heading. */
    private String lidZin(ParserRuleContext lid) {
        if (lid instanceof RolSpecificatieContext r) return "de rol '" + src(r.rolNaam()) + "'";
        ObjectTypeMemberContext m = (ObjectTypeMemberContext) lid;
        if (m.attribuutSpecificatie() != null) return "het attribuut '" + naam(m.attribuutSpecificatie().lowerNaam()) + "'";
        if (m.kenmerkSpecificatie() != null) return "het kenmerk '" + kenmerkNaam(m.kenmerkSpecificatie()) + "'";
        return null;
    }

    /** A token of the language rather than a word of the author: neither a name word of either case nor a number. */
    private static boolean sleutelwoord(Token t) {
        return t.getType() != RegelSpraak.UPPER_ID && t.getType() != RegelSpraak.LOWER_ID && t.getType() != RegelSpraak.NUMBER;
    }

    // ---------------- a role's filler is a declared object type ----------------
    private record RolVuller(String feittype, String rol, String vuller, int regel) {}
    private final List<RolVuller> rolVullers = new ArrayList<>();

    /** WF-003 for a role's filler, BEFORE the emission: until this check the frontend wrote the document without a
     *  word and only the verifier refused it, so a check without the verifier passed a filler like 'BK Jutter' — a
     *  role name cut at its first capitalised word, its rest read as the type. Case-insensitive, as WF-003 resolves.
     *  An ERROR where the document shows the misreading: the name is declared as another kind (the sentence says
     *  which), or a shorter tail of it is a declared object type (the sentence says the role name ended at the first
     *  capitalised word). A WARNING where the name is declared nowhere in the document: one file of a model that
     *  declares its object types in another file reads so on its own, and resolves once the files are read as one
     *  (grammar/DIVERGENCES.md, "Namen in declaratiekoppen"); the verifier still refuses such a file emitted on its
     *  own. */
    private void weigerOngedeclareerdeVullers() {
        for (RolVuller v : rolVullers) {
            if (model.objecttypeOpNaam(v.vuller()) != null) continue;
            String soort = soortVanNaam(v.vuller());
            String zin = "feittype '" + v.feittype() + "': de rol '" + v.rol() + "' heeft als objecttype '" + v.vuller()
                    + "', en dat is " + (soort == null ? "nergens als objecttype gedeclareerd" : "een " + soort + ", geen objecttype")
                    + " — WF-003: de vuller van een rol is een gedeclareerd objecttype";
            boolean misgelezen = soort != null;
            String[] woorden = v.vuller().split(" ");
            for (int i = 1; i < woorden.length; i++) {
                String staart = String.join(" ", java.util.Arrays.copyOfRange(woorden, i, woorden.length));
                Objecttype ot = model.objecttypeOpNaam(staart);
                if (ot == null) continue;
                zin += "; '" + ot.naam + "' is dat wel: een rolnaam eindigt bij het eerste woord met een hoofdletter, dus '"
                        + String.join(" ", java.util.Arrays.copyOfRange(woorden, 0, i)) + "' is als deel van het objecttype gelezen";
                misgelezen = true;
                break;
            }
            if (misgelezen) fout(zin, v.regel());
            else model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.WARNING,
                    zin + "; staat het in een ander bestand van het model, lees de bestanden dan samen", v.regel()));
        }
    }

    // ---------------- a declared name rule text does not read ----------------
    /** A declared attribute, characteristic or role name that rule text cannot read back as one name gets a WARNING
     *  naming the word. The name is lexed again on its own and parsed as the {@code kaleNaam} of rule text, up to its
     *  end: 'als' is no word of a name there, and 'van' separates two parts of one and so neither opens nor closes
     *  it. A WARNING and no ERROR: the declaration itself is well-formed, a document that never reads the name in a
     *  rule is sound, and one source outside this repository carries such a name (grammar/DIVERGENCES.md, "Namen in
     *  declaratiekoppen"). */
    private void meldOnleesbareNaam(String drager, String soort, String naam, int regel) {
        List<String> woorden = new ArrayList<>();
        int fout = onleesbaarWoord(naam, woorden);
        if (fout < 0) return;
        List<String> voorbeeld = new ArrayList<>(woorden);
        int links = fout > 0 ? fout - 1 : fout;
        if (links + 1 < voorbeeld.size()) {
            voorbeeld.set(links, voorbeeld.get(links) + "-" + voorbeeld.get(links + 1));
            voorbeeld.remove(links + 1);
        }
        model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.WARNING,
                drager + ": regeltekst leest " + soort + " '" + naam + "' niet als één naam, want '" + woorden.get(fout)
                        + "' is daar geen woord van een naam; verbind het met een koppelteken aan een buurwoord ('"
                        + String.join(" ", voorbeeld) + "')", regel));
    }

    /** The index, among the tokens of {@code naam} lexed on their own, of the first word that keeps the
     *  {@code kaleNaam} of rule text from reading the whole name; -1 when it reads it. {@code woorden} receives the
     *  tokens' texts. */
    static int onleesbaarWoord(String naam, List<String> woorden) {
        RegelSpraakLexer lexer = new RegelSpraakLexer(CharStreams.fromString(naam));
        lexer.removeErrorListeners();
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();
        List<Token> lijst = tokens.getTokens().stream()
                .filter(t -> t.getChannel() == Token.DEFAULT_CHANNEL && t.getType() != Token.EOF).toList();
        for (Token t : lijst) woorden.add(t.getText());
        RegelSpraak parser = new RegelSpraak(tokens);
        parser.removeErrorListeners();
        parser.setErrorHandler(new BailErrorStrategy());
        Token dader;
        try {
            parser.kaleNaam();
            dader = tokens.LT(1);
        } catch (ParseCancellationException e) {
            dader = e.getCause() instanceof RecognitionException r ? r.getOffendingToken() : tokens.LT(1);
        }
        if (dader == null || dader.getType() == Token.EOF) return -1;
        for (int i = 0; i < lijst.size(); i++) if (lijst.get(i).getTokenIndex() == dader.getTokenIndex()) return i;
        throw new IllegalStateException("DefinitionResolver.onleesbaarWoord: het token '" + dader.getText()
                + "' staat niet in de naam '" + naam + "'");
    }

    // ---------------- helpers ----------------
    /** §3.12/§13.3.10 register a dagsoort: its singular name, and the unit→dagsoort map for BOTH the singular
     *  and (when declared) the meervoud — a §6 duration unit "5 werkdagen" arrives as the plural eenheidsafkorting. */
    private void dagsoort(DagsoortDefinitionContext d) {
        String enkel = naam(d.lowerNaam());
        // A day kind NAMED after a calendar spelling is refused outright (WF-017 over /dagsoorten), and the
        // refusal has to be an ERROR rather than the WARNING-and-drop the plural gets below: dropping the
        // NAME would leave the §9.8 dagsoortdefinitie pointing at a day kind `dagsoorten` no longer declares,
        // which the verifier refuses in its own right. An ERROR blocks the emission entirely — the same
        // answer the grammar already gives for the unit names that are keywords.
        if (Tijdrekening.isKalenderSpelling(enkel.trim().toLowerCase())) {
            model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.ERROR,
                    "dagsoort '" + enkel + "': day-kind '" + enkel + "' shadows a built-in calendar unit",
                    d.getStart().getLine()));
            return;
        }
        int regel = d.getStart().getLine();
        model.dagsoorten.add(enkel);
        registreerDagsoortEenheid(enkel, enkel, regel, false);
        // §3.12 a duration unit is naturally the PLURAL ("5 werkdagen"); register the declared meervoud, or — when
        // none is declared — the derived "-en" plural ("werkdag" -> "werkdagen"), so the unit still resolves.
        if (d.meervoudsvorm() != null) registreerDagsoortEenheid(src(d.meervoudsvorm()).trim(), enkel, regel, true);
        else registreerDagsoortEenheid(enkel + "en", enkel, regel, null);
    }

    /** Every written spelling of a day kind so far, lower-cased: the day kind, and its written plural or null where
     *  the spelling is the day kind's name. */
    private final java.util.Map<String, String[]> dagsoortSpellingen = new java.util.HashMap<>();

    /** A dagsoort unit spelling that collides with a spelling of the NORMATIVE calendar table
     *  (uchar's RUNTIME-SEMANTICS ch. 3.8 — thirty spellings, abbreviations and second plurals included) is
     *  NOT registered: it must not shadow the calendar arithmetic of {@code datum ± N dagen}; warn instead.
     *  The comparison goes through {@link Tijdrekening} rather than a list of its own, so this guard and the
     *  arithmetic that reads a unit spelling can never come to know different tables — which is what they
     *  did until 2026-09-20, when this list held the eighteen singulars and plurals and let the twelve
     *  remaining spellings through into an IR that WF-017 refuses.
     *  {@code regel} is the declaration's own line: the warning used to be raised at a hard-coded 0, which
     *  points an author at nothing in a document with many day kinds. */
    private void registreerDagsoortEenheid(String spelling, String dagsoort, int regel, Boolean meervoud) {
        String k = spelling.trim().toLowerCase();
        if (Tijdrekening.isKalenderSpelling(k)) {
            model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.WARNING,
                    "dagsoort-eenheid '" + spelling + "' valt samen met een ingebouwde tijdseenheid en wordt niet als dagsoort-eenheid geregistreerd", regel));
            return;
        }
        // A written spelling another day kind already carries names two day kinds (WF-017). It used to be dropped
        // without a word: the second day kind lost its declared plural, or its name came to stand for the first. A
        // derived plural ({@code meervoud} null) is this frontend's own convenience and gives way without a refusal,
        // also to a written spelling that comes after it.
        if (meervoud == null) {                 // a derived plural gives way to every spelling already there
            model.dagsoortPerEenheid.putIfAbsent(k, dagsoort);
            return;
        }
        String[] eerder = dagsoortSpellingen.get(k);
        if (eerder == null) {                   // a written spelling: a derived plural there gives way to it
            dagsoortSpellingen.put(k, new String[]{dagsoort, meervoud ? spelling : null});
            model.dagsoortPerEenheid.put(k, dagsoort);
            return;
        }
        if (!eerder[0].equalsIgnoreCase(dagsoort)) {
            String zin = meervoud
                    ? "the plural '" + spelling + "' of day kind '" + dagsoort + "' is the "
                            + (eerder[1] == null ? "name of day kind '" + eerder[0] + "'"
                                                 : "plural '" + eerder[1] + "' of day kind '" + eerder[0] + "'")
                    : eerder[1] == null ? null
                    : "the plural '" + eerder[1] + "' of day kind '" + eerder[0] + "' is the name of day kind '"
                            + dagsoort + "'";
            if (zin != null)
                model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.ERROR,
                        "dagsoort '" + dagsoort + "': " + zin + ": one spelling names two day kinds", regel));
        }
    }

    private static String naam(LowerNaamContext ctx) { return src(ctx); }

    /** The original source slice of a context, with internal whitespace collapsed. */
    static String src(ParserRuleContext ctx) {
        if (ctx == null || ctx.getStart() == null || ctx.getStop() == null) return "";
        if (ctx.getStop().getStopIndex() < ctx.getStart().getStartIndex()) return ""; // empty match
        String t = ctx.getStart().getInputStream()
                .getText(Interval.of(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex()));
        return t.replaceAll("\\s+", " ").trim();
    }

    /** The structured source anchor of {@code ctx}: {@link #src} as the snippet, plus file/line/column
     *  from the start token (the file being the char stream's source name, set at the front end). A null
     *  ctx yields a synthetic empty anchor, mirroring {@link #src}'s null handling. */
    static dev.uchar.regelspraak.ir.Bron bron(ParserRuleContext ctx) {
        return ctx == null ? dev.uchar.regelspraak.ir.Bron.synthetisch("")
                           : dev.uchar.regelspraak.ir.Bron.van(ctx, src(ctx));
    }

    private static String unquote(String s) {
        if (s.length() >= 2) return s.substring(1, s.length() - 1);
        return s;
    }
}
