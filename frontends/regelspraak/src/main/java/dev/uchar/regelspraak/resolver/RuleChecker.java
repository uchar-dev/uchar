package dev.uchar.regelspraak.resolver;

import dev.uchar.regelspraak.ir.Conditie;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.Default;
import dev.uchar.regelspraak.ir.ExceptionGroup;
import dev.uchar.regelspraak.ir.ResolvedFactCreation;
import dev.uchar.regelspraak.ir.Geldigheid;
import dev.uchar.regelspraak.ir.ResolvedObjectCreation;
import dev.uchar.regelspraak.ir.ResolvedRegel;
import dev.uchar.regelspraak.ir.ResolvedVerdeling;
import dev.uchar.regelspraak.ir.Bron;
import dev.uchar.regelspraak.ir.ResolvedUniquenessUnion;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.*;
import dev.uchar.regelspraak.parser.RegelSpraak;
import dev.uchar.regelspraak.parser.RegelSpraak.*;
import dev.uchar.regelspraak.parser.RegelSpraakLexer;
import dev.uchar.regelspraak.kern.Literals;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Type-checks rules against the typeringen matrix (Phase: rule checking). It
 * infers an expression's datatype where the references resolve against the model
 * — literals, parameters, arithmetic, and attribute-of-subject references whose
 * subject is a plain object type — and applies {@link Typing}. References it
 * cannot resolve yield {@link Datatype.Onbekend}, which {@link Typing} treats
 * as "no claim", so the checker never raises a false positive on a construct it
 * does not fully resolve. Deep van-chains and aggregations have since joined the
 * resolved side (a navigationPath on the reference, a typed {@link Expr.FunctionCall}). The result
 * type rules are the spec's; the coverage is honest about its limits.
 */
final class RuleChecker {
    private final RegelSpraakModel model;
    private final ExpressionBuilder builder;
    /** Set (to the role the doel fills) only while re-resolving a §13.4.7 feitcreatie guard against the DOEL:
     *  it makes {@link #subjectHead} also accept "de &lt;roleC&gt;" as denoting the (re-)subject, and REJECT the
     *  bezielde "hij"/bare reference — which in a feitcreatie names the anker, not the doel. null otherwise. */
    private String targetRole = null;
    /** The {@code // Bron: …} statuutannotaties of the rule currently being resolved (the text after
     *  {@code Bron:}, in source order), set at the start of {@link #ruleVersions} and carried onto every
     *  {@link ResolvedRegel} version it produces. Empty when the rule carried none. */
    private List<String> currentSourceAnnotations = List.of();
    /** The names of the rules each §12 decision table desugared to, one per row, recorded by
     *  {@link #decisionTables}. A table has no rule form of its own — its rows are the rules — so a reader that
     *  asks which rules a table stands for asks here. */
    private final Map<BeslistabelContext, List<String>> rowRules = new java.util.IdentityHashMap<>();
    /** The line of the decision table whose rows are being resolved, 0 outside them: a row is parsed in a stream of its
     *  own, so a warning about a condition in a row stands at the table. */
    private int rowTableLine;
    /** The rules whose target {@link #checkRuleTargets} refused. The defaults lowering skips their families: its
     *  own checks would judge a target the author did not write (a navigation swallowed into a name) and add a
     *  second, misleading refusal beside the true one. */
    private final java.util.Set<ResolvedRegel> refusedTargets =
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    /** Where each defaults group was written — the block's head line and the head's own words — so a refusal
     *  about the group can stand at the block that made it. */
    private final Map<PrioritizedDefaults, Origin> pdOrigin = new java.util.IdentityHashMap<>();
    private record Origin(int line, String head) {}
    /** The groups {@link #subtypeGroup} made, each with the base rule it was made under and its subtype: the name, the
     *  base and the subtype's name. */
    private final List<String[]> subtypeGroups = new ArrayList<>();
    /** Per element {@link #generatedElements} names after more than one declaration — the group of a subtype's
     *  blocks, the member and rule of a fold with steps after the set — the pieces of its name, keyed and spelled as
     *  {@link RegelSpraakModel#gegenereerd} is: {@link Designations#compose} composes its designations part by part. */
    private final Map<Benoembaar, List<Designations.NamePart>> nameParts = new LinkedHashMap<>();
    /** The anchor members {@link #startPointAnchors} declared, by {@link #anchorKey}: an anchor of a name the author
     *  declared himself is not in here. */
    private final java.util.Set<String> generatedAnchors = new java.util.HashSet<>();
    /** Per generated anchor, by {@link #anchorKey}, its type, its name and the form name of the first rule that
     *  writes it ({@link #startPointDetermination}). */
    private final Map<String, String[]> anchorRules = new LinkedHashMap<>();

    /** The main parse's token stream, kept so the §Bron statuutannotaties on the HIDDEN channel
     *  (the {@code // Bron: …} comments) can be gathered directly above each rule. Null when the
     *  checker is driven without a stream (no annotations are then collected). */
    private final CommonTokenStream tokens;

    /** The members an extension declares again, with whether the repeat is admitted ({@link DefinitionResolver}): the
     *  opgave pass reads the marker of an admitted repeat only, since a refused one is refused by its own sentence. */
    private final Map<ObjectTypeMemberContext, Boolean> repetitions;

    RuleChecker(RegelSpraakModel model) { this(model, null, Map.of()); }

    RuleChecker(RegelSpraakModel model, CommonTokenStream tokens, Map<ObjectTypeMemberContext, Boolean> repetitions) {
        this.model = model;
        this.tokens = tokens;
        this.repetitions = repetitions;
        this.builder = new ExpressionBuilder(model);
        this.builder.setCompoundFilter(this::compoundElementFilter);   // §13.4.13.4 compound subselectie
    }

    /** The pieces of the names {@link #check} generated after more than one declaration, by element. */
    Map<Benoembaar, List<Designations.NamePart>> nameParts() { return nameParts; }

    void check(RegelSpraakDocumentContext doc) {
        domainReferences(doc);
        // before any rule resolves: a rule may read the members the opgave marker generates (dialect §7)
        inputDeclarations(doc);
        // before any rule resolves: what a rule reads of another rule's form must not depend on which stands first
        createdObjects(doc);
        startPointAnchors(doc);
        // §9.10 Regelgroep (DOCUMENTED EXTENSION): a grouped rule resolves EXACTLY as an ungrouped one, so the
        // whole rule pipeline runs over the flattened list (top-level + group members, in document order).
        for (RegelContext r : DefinitionResolver.allRules(doc)) ruleVersions(r);
        // §4 (PRIORITIZED-DEFAULTS-EVOLUTIE) the ordered exception blocks resolve like regels too, so their
        // bodies get the same type-checks; the group members were already resolved above (allRules includes them).
        for (UitzonderingRegelContext u : DefinitionResolver.allExceptions(doc)) exceptionVersions(u);
        decisionTables(doc);
        helperRules(doc);     // the rules of the members a fold over a set and to-one steps after it generated
        regelGroepen(doc);   // build the §9.10 group IR + the fase-2 recursief structural checks
        checkCycleReference();
        // The set fence runs BEFORE the defaults lift: that pass MOVES resolved rules out of resolvedRegels into
        // PrioritizedDefaults nodes whose consequentie is a copy of the rule's waarde, so checking here
        // covers the lifted rules too — and covers them once.
        checkSetReads();
        checkPartSelections(doc);
        checkRuleTargets();    // WF-022, before the lift: the lowering reads these targets as they are
        defaults(doc);        // §4 lift the base+exception families and unordered groups into PrioritizedDefaults nodes
        // §7 after the lift, so a tree or an unordered set of the dialect's blocks moves to 'berekende X' whole
        inputLowering(doc);
        // §7 a read of a generated member closes a cycle only once the writers have moved to 'berekende X'
        checkCycleReferenceAfterInput();
        checkWindowPositions(); // WF-112, over the conditions as emitted — after the lift, so defaults are covered
        checkPaths();   // a read or a fold the emitter could write only with a binding none supplies
        checkSwallowedNames();      // a read whose name took in a word before 'van' and a declared attribute
        checkConstants();           // a constant the wire has no node for ("hij" as a value)
        checkFormNames(doc);         // WF-020, over the named forms as the emitter writes them
        checkDependencyCycles(doc);   // WF-045 and WF-051, over the document the emitter writes
        generatedElements();    // last: only the elements that are still in the model get an origin
    }

    /**
     * The origin of every element the passes above made under a name of their own ({@link RegelSpraakModel#gegenereerd},
     * grammar/DIVERGENCES.md, "Gegenereerde elementen"): the members and forms of a lowered opgave marker, the group of
     * a subtype's own blocks, the rows of a decision table, the member and rule of a fold with steps after the set, and
     * a start point's anchor. Each element is keyed, and each source spelled, as {@link RegelSpraakModel#benoembaar}
     * spells it; a decision table, the one source that is no element, keeps its written name. A candidate whose element
     * a later pass removed again — the members of a withdrawn marker, a row lifted into a defaults tree — is no element
     * of the model and gets no entry. The group and the fold's member and rule also get the pieces of their name
     * ({@link #nameParts}).
     */
    private void generatedElements() {
        Map<String, GegenereerdUit> candidates = new LinkedHashMap<>();
        Map<String, List<Designations.NamePart>> parts = new LinkedHashMap<>();
        for (Input o : inputs) {
            GegenereerdUit generatedFrom = new GegenereerdUit(Benoembaar.attribuut(o.type().naam, o.attribuut()),
                    Herkomstvorm.OPGAVE_BOVEN_BEREKENING);
            kandidaat(candidates, Benoembaar.attribuut(o.type().naam, o.opgegeven()), generatedFrom);
            kandidaat(candidates, Benoembaar.attribuut(o.type().naam, o.computed()), generatedFrom);
            kandidaat(candidates, Benoembaar.kenmerk(o.type().naam, o.afwijkend()), generatedFrom);
            if (gelowerd.contains(o))
                for (String vorm : List.of(o.regel(), o.consistencyCheck(), o.group()))
                    kandidaat(candidates, Benoembaar.vorm(vorm), generatedFrom);
        }
        for (String[] g : subtypeGroups) {
            Benoembaar group = Benoembaar.vorm(g[0]);
            kandidaat(candidates, group, new GegenereerdUit(Benoembaar.vorm(g[1]), Herkomstvorm.EXCEPTION_FOR_SUBTYPE));
            parts.put(elementKey(group), List.of(new Designations.NamePart(g[1], Benoembaar.vorm(g[1])),
                    new Designations.NamePart(" voor ", null), new Designations.NamePart(g[2], Benoembaar.objecttype(g[2]))));
        }
        for (var t : rowRules.entrySet()) {
            GegenereerdUit generatedFrom = new GegenereerdUit(Benoembaar.vorm(DefinitionResolver.src(t.getKey().beslistabelNaam())),
                    Herkomstvorm.BESLISTABELRIJ);
            for (String row : t.getValue()) kandidaat(candidates, Benoembaar.vorm(row), generatedFrom);
        }
        for (ExpressionBuilder.HelperMember h : builder.helperMembers()) {
            Benoembaar member = Benoembaar.attribuut(attributeDeclarant(h.doel(), h.member()).naam, h.member());
            GegenereerdUit generatedFrom = new GegenereerdUit(member, Herkomstvorm.STEPS_AFTER_SET);
            Benoembaar attribuut = Benoembaar.attribuut(h.element().naam, h.naam()), regel = Benoembaar.vorm(h.naam());
            kandidaat(candidates, attribuut, generatedFrom);
            kandidaat(candidates, regel, generatedFrom);
            List<Designations.NamePart> d = helperNameParts(h, member);
            if (d != null) {
                parts.put(elementKey(attribuut), d);
                parts.put(elementKey(regel), d);
            }
        }
        for (String[] a : anchorRules.values())
            kandidaat(candidates, Benoembaar.attribuut(a[0], a[1]),
                    new GegenereerdUit(Benoembaar.vorm(a[2]), Herkomstvorm.START_POINT));
        List<Benoembaar> all = model.benoembaar();
        for (Benoembaar b : all) {
            GegenereerdUit generatedFrom = candidates.get(elementKey(b));
            if (generatedFrom == null) continue;
            if (generatedFrom.vorm() == Herkomstvorm.BESLISTABELRIJ) {
                // a table names no element, and a form of the table's name would be another declaration
                model.gegenereerd.put(b, generatedFrom);
                continue;
            }
            Benoembaar bron = onName(all, generatedFrom.bron());
            if (bron == null && model.isOk())
                throw new IllegalStateException("het gegenereerde element " + b + " komt uit " + generatedFrom.bron()
                        + ", en dat is geen element van het model");
            model.gegenereerd.put(b, bron == null ? generatedFrom : new GegenereerdUit(bron, generatedFrom.vorm()));
            List<Designations.NamePart> d = parts.get(elementKey(b));
            if (d != null) d = inModel(all, b, d);
            if (d != null) nameParts.put(b, d);
        }
    }

    /** The element of {@code alle} that {@code b} names under the name identity, or null. */
    private static Benoembaar onName(List<Benoembaar> all, Benoembaar b) {
        return all.stream().filter(x -> elementKey(x).equals(elementKey(b))).findFirst().orElse(null);
    }

    /** The pieces {@code parts} of the name of {@code element}, each declared part spelled as {@code alle} spells it;
     *  null where a part is no element of the model, which an ERROR of the model explains (in a model without one, a
     *  fault of the frontend). */
    private List<Designations.NamePart> inModel(List<Benoembaar> all, Benoembaar element,
                                                 List<Designations.NamePart> parts) {
        List<Designations.NamePart> result = new ArrayList<>();
        for (Designations.NamePart d : parts) {
            Benoembaar part = d.element() == null ? null : onName(all, d.element());
            if (d.element() != null && part == null) {
                if (model.isOk())
                    throw new IllegalStateException("het gegenereerde element " + element + " heet naar "
                            + d.element() + ", en dat is geen element van het model");
                return null;
            }
            result.add(part == null ? d : new Designations.NamePart(d.tekst(), part));
        }
        return result;
    }

    /** The pieces of the name '&lt;lid&gt; via &lt;rol 1&gt;, … en &lt;rol n&gt;' of {@code h}: the member the steps
     *  reach, as declared ({@code lid}), and each role as the wire binds its step, read from the type the member stands
     *  on ({@link dev.uchar.regelspraak.emit.IrEmitter#navigationBindings}, the derivation the emitter writes the
     *  generated rule's read with); null where a step binds no role, which the read's ERROR explains
     *  ({@link #checkPaths}; in a model without one, a fault of the frontend). */
    private List<Designations.NamePart> helperNameParts(ExpressionBuilder.HelperMember h, Benoembaar member) {
        List<RoleBindingRef> steps =
                dev.uchar.regelspraak.emit.IrEmitter.navigationBindings(model, h.element(), helperValue(h));
        int n = h.rollen().size();
        if (steps.size() != n) {
            if (model.isOk())
                throw new IllegalStateException("de stappen " + h.rollen() + " van het gegenereerde lid '" + h.naam()
                        + "' van " + h.element().naam + " binden niet elk één rol");
            return null;
        }
        List<Designations.NamePart> result = new ArrayList<>();
        result.add(new Designations.NamePart(h.member(), member));
        for (int i = 0; i < n; i++) {
            result.add(new Designations.NamePart(i == 0 ? " via " : i == n - 1 ? " en " : ", ", null));
            result.add(new Designations.NamePart(h.rollen().get(i),
                    Benoembaar.rol(steps.get(i).feittype(), steps.get(i).rol())));
        }
        return result;
    }

    /** Records {@code uit} as the origin of {@code element}. Two different origins for one element are two forms that
     *  generate one name, which a refusal (WF-020, a member name taken) reports in a model with an error; in a model
     *  without one they are a fault of the frontend. */
    private void kandidaat(Map<String, GegenereerdUit> candidates, Benoembaar element, GegenereerdUit generatedFrom) {
        GegenereerdUit earlier = candidates.putIfAbsent(elementKey(element), generatedFrom);
        if (earlier != null && !earlier.equals(generatedFrom) && model.isOk())
            throw new IllegalStateException("het element " + element + " is gegenereerd uit " + earlier + " en uit "
                    + generatedFrom);
    }

    /** The name identity of an element (IR-SPEC ch. 2.1): its kind, owner and name under the frontend's norm. */
    private static String elementKey(Benoembaar b) {
        return b.soort() + "\u0000" + (b.eigenaar() == null ? "" : dev.uchar.regelspraak.emit.IrEmitter.norm(b.eigenaar()))
                + "\u0000" + dev.uchar.regelspraak.emit.IrEmitter.norm(b.naam());
    }

    /** The type on {@code t}'s chain that declares attribute {@code naam}. */
    private static Objecttype attributeDeclarant(Objecttype t, String naam) {
        for (Objecttype k : t.keten())
            for (String a : k.attributen.keySet())
                if (a.equalsIgnoreCase(naam)) return k;
        throw new IllegalStateException("het attribuut '" + naam + "' staat niet in de keten van " + t.naam);
    }

    /** What a form of the dependency graph is called in a refusal, by the list it comes from. */
    private static final Map<String, String> FORM_KIND = Map.of("rules", "regel", "allocations", "verdeling",
            "consistencies", "consistentieregel", "uniquenesses", "uniciteitsregel", "objectCreations", "objectcreatie",
            "factCreations", "feitcreatie", "prioritizedDefaults", "defaults-groep");

    /**
     * WF-045 and WF-051, the verifier's two cycle rules, over the document the emitter writes, in the verifier's own
     * reading of IR-SPEC ch. 5 ({@link Dependencies}): a dependency cycle that passes through no creation of a
     * recursive rule group, and a cycle through a defaults group, are refused in the verifier's sentence, at the head of
     * the form the verifier reports. {@code check} runs no verifier, and such a document passed it without a word: a
     * value of one object that depends on the same value of another object of its type, over a role, is a cycle of the
     * graph (one node per object type and member, ch. 5.1), and the warning below sees only the reads on a rule's own
     * subject. A cycle that warning names is refused here as well, and the WARNING stays beside the ERROR: it names the
     * cycle in the members the author wrote, where the verifier's sentence names forms. Where a negative edge (ch. 5.2)
     * runs between two forms of a WF-045 cycle the refusal names WF-046 too, the rule the verifier adds for it. A cycle
     * through an object creation outside a recursive group is refused like any other since 2026-10-06; until then it was
     * let through, because a fact creation wrote the population of its target type in the graph and so closed a cycle
     * that does not exist with an object creation that counts members of that type (the casus of the parity zone). One
     * violation is refused once, where the verifier reports it twice over two edges of one pair. Runs last, on a model
     * without an ERROR: a model the emitter refuses has no document to judge, and that refusal is reported where the
     * document is written.
     */
    private void checkDependencyCycles(RegelSpraakDocumentContext doc) {
        if (!model.isOk()) return;
        String ir;
        try {
            ir = dev.uchar.regelspraak.emit.IrEmitter.emit(model);
        } catch (IllegalStateException | UnsupportedOperationException e) {
            return;
        }
        Map<String, String[]> sites = formSites(doc);
        Set<String> reported = new java.util.HashSet<>();
        for (Dependencies.Cycle k : Dependencies.cycles(ir)) {
            if (!reported.add(k.wf() + " " + k.at() + " " + k.sentence())) continue;
            Dependencies.Form vorm = k.vormen().get(0);
            String naam = Dependencies.withoutVersion(vorm.naam());
            String[] site = sites.get(dev.uchar.regelspraak.emit.IrEmitter.norm(naam));
            boolean wf045 = "WF-045".equals(k.wf());
            fout(site == null ? 0 : Integer.parseInt(site[1]), FORM_KIND.get(vorm.soort()) + " '" + naam + "' "
                    + (wf045
                        ? "hangt via een kring van zichzelf af; de afhankelijkheden zijn kringvrij buiten een recursieve creatie"
                        : "ligt op een afhankelijkheidskring, en een kring door een defaults-groep heeft geen strata")
                    + " (" + k.wf() + ")"
                    + (wf045 && Dependencies.carriesNegativeEdge(k)
                        ? ", en een kring draagt geen negatieve rand, zoals een ontkenning (WF-046)" : "")
                    + ": " + k.sentence());
        }
    }

    /** The arrays of named forms in the order the verifier reports a collision between two of them (WF-020). */
    private static final List<String> FORM_LISTS = List.of("consistencies", "factCreations", "objectCreations",
            "prioritizedDefaults", "rules", "uniquenesses", "allocations");

    /**
     * WF-020: two named forms with one name, compared as the verifier compares them ({@link
     * dev.uchar.regelspraak.emit.IrEmitter#norm}). The forms are read off the model after the lift, as the emitter writes
     * them, so a name that does not reach the wire is not judged: the name of an 'Uitzondering op' or 'Afwijking van'
     * block, whose default joins the group of its base, and a declaration that lowers to nothing. A form with versions is
     * one form, its entries named "&lt;naam&gt; #1" … "#n" in a row, so a second "#1" begins a second form. The refusal
     * stands on the second of the 'Regel' heads and decision-table rows that carry the name and names the line of the
     * first, in the verifier's sentence; a collision with a generated form is refused where that form is generated.
     */
    private void checkFormNames(RegelSpraakDocumentContext doc) {
        Map<String, List<String>> lists = new java.util.LinkedHashMap<>();   // name identity -> the array of each form
        java.util.function.BiConsumer<String, List<String>> vormen = (lijst, names) -> {
            for (int i = 0; i < names.size(); i++) {
                String naam = names.get(i);
                if (naam == null) continue;
                String plain = withoutVersion(naam);
                boolean continuation = !plain.equals(naam) && !naam.endsWith(" #1") && i > 0
                        && names.get(i - 1) != null && withoutVersion(names.get(i - 1)).equals(plain);
                if (!continuation) lists.computeIfAbsent(dev.uchar.regelspraak.emit.IrEmitter.norm(plain),
                        k -> new ArrayList<>()).add(lijst);
            }
        };
        vormen.accept("rules", model.resolvedRegels.stream().map(ResolvedRegel::naam).toList());
        vormen.accept("objectCreations", model.objectCreaties.stream().map(o -> o.naam()).toList());
        vormen.accept("allocations", model.verdelingen.stream()
                .filter(v -> v.methode() != ResolvedVerdeling.Methode.COMPLEX).map(v -> v.naam()).toList());
        vormen.accept("consistencies", model.consistenties.stream().map(c -> c.naam()).toList());
        vormen.accept("uniquenesses", java.util.stream.Stream.concat(model.uniciteiten.stream().map(u -> u.naam()),
                model.uniciteitUnies.stream().map(u -> u.naam())).toList());
        vormen.accept("factCreations", model.feitCreaties.stream().map(f -> f.naam()).toList());
        vormen.accept("prioritizedDefaults", model.prioritizedDefaults.stream().map(g -> g.naam()).toList());
        // the authored heads, in document order: a 'Regel' head is {name, line}, a table row {name, line, table}
        List<String[]> heads = new ArrayList<>();
        for (RegelContext r : DefinitionResolver.allRules(doc))
            heads.add(new String[] {src(r.regelNaam()), String.valueOf(r.getStart().getLine()), null});
        for (var e : rowRules.entrySet())
            for (String row : e.getValue())
                heads.add(new String[] {row, String.valueOf(e.getKey().getStart().getLine()),
                        src(e.getKey().beslistabelNaam())});
        heads.sort(java.util.Comparator.comparingInt(k -> Integer.parseInt(k[1])));
        for (var e : lists.entrySet()) {
            if (e.getValue().size() < 2) continue;
            List<String[]> carriers = heads.stream()
                    .filter(k -> dev.uchar.regelspraak.emit.IrEmitter.norm(k[0]).equals(e.getKey())).toList();
            if (carriers.size() < 2) continue;
            String[] first = carriers.get(0);
            List<String> arrays = e.getValue().stream().distinct()
                    .sorted(java.util.Comparator.comparingInt(FORM_LISTS::indexOf)).toList();
            String verifier = arrays.size() == 1
                    ? "duplicate name '" + first[0] + "' in " + arrays.get(0)
                    : "name '" + first[0] + "' collides with '" + carriers.get(1)[0] + "' in " + arrays.get(1)
                            + " (unique across all named forms)";
            for (String[] following : carriers.subList(1, carriers.size()))
                fout(Integer.parseInt(following[1]), "regel '" + following[0] + "'"
                        + (following[2] == null ? "" : " van beslistabel '" + following[2] + "'")
                        + " is al gedeclareerd op regel " + first[1]
                        + (first[2] == null ? "" : ", als rij van beslistabel '" + first[2] + "'")
                        + "; vormnamen zijn uniek (WF-020): " + verifier);
        }
    }

    /**
     * §5.6: every "die/dat &lt;predicaat&gt;" the parse produced must have been CONSUMED by a resolution
     * that accounts for it — bound as a filter, or refused by name. What is left over was silently
     * dropped, and a dropped subselectie is not a missing feature but a WRONG ANSWER: the document then
     * ranges over every instance where the sentence named a selection of them, verifier-clean, and every
     * executor answers it the same and wrongly.
     *
     * <p>ERROR, not WARNING, and the difference is the point: a warning leaves the document on disk.
     * There is no reading of the emitted IR under which the omitted subselectie is harmless, so the
     * refusal has to block the emission.
     *
     * <p>This is not an extension point. Measured against RegelSpraak v2.3.0 (card
     * subselectie-gewone-positie, 2026-09-19): a subselectie is ALWAYS plural (§5.8), and the language
     * admits exactly two consumers of a plural reading — an aggregation (§5.8) and a condition whose
     * subject expression carries a quantifier (ch. 10). Both are folds, both already reach the wire
     * (ch. 4.3 / ch. 4.5, {@code filter}). A subselection on an ordinary position has no reading in the
     * source language at all, so this refusal is the DOCUMENTED END STATE and not a placeholder for a
     * semantics to be built.
     */
    private void checkPartSelections(RegelSpraakDocumentContext doc) {
        java.util.Set<PredicaatContext> consumed = builder.consumedPartSelections();
        List<PredicaatContext> all = new ArrayList<>();
        collectPartSelections(doc, all);
        for (PredicaatContext p : all) {
            if (consumed.contains(p)) continue;
            model.diagnostics.add(new Diagnostic(Severity.ERROR,
                "de 'die/dat'-deelselectie '" + src(p) + "' wordt niet omgezet en zou stil wegvallen: een "
                + "deelselectie kiest WELKE exemplaren de zin bedoelt, dus haar weglaten beantwoordt een "
                + "andere vraag dan er staat. Een subselectie is in RegelSpraak v2.3.0 ALTIJD MEERVOUDIG "
                + "(§5.8), en een meervoudige lezing kent daar precies twee verbruikers: een aggregatie "
                + "(§5.8) of een voorwaarde met een kwantificatie vóór de onderwerpexpressie (hfst. 10). "
                + "Op deze schrijfpositie staat geen van beide, dus de zin heeft in v2.3.0 geen lezing — "
                + "deze weigering is de eindtoestand en geen uitbreidingspunt. Schrijf de selectie als "
                + "vouwbereik (IR-SPEC ch. 4.3 of ch. 4.5, veld 'filter') of zet er een kwantificatie voor "
                + "(§5.6)",
                p.getStart().getLine()));
        }
    }

    /** Every {@code onderwerpKeten ( DIE | DAT ) predicaat} in the tree, in document order. */
    private static void collectPartSelections(org.antlr.v4.runtime.tree.ParseTree t, List<PredicaatContext> result) {
        if (t instanceof OnderwerpKetenContext k && k.predicaat() != null) result.add(k.predicaat());
        for (int i = 0; i < t.getChildCount(); i++) collectPartSelections(t.getChild(i), result);
    }

    /** The rules that set the members {@link ExpressionBuilder} generated for a value fold over a set followed by
     *  to-one steps (grammar/DIVERGENCES.md, "A fold over a set and to-one steps after it"), one per member, behind
     *  every authored rule: "De &lt;lid&gt; van een &lt;element&gt; moet gesteld worden op het &lt;lid&gt; van de
     *  &lt;stap n&gt; … van de &lt;element&gt;", the rule an author writes by hand, under the member's name. A form
     *  of that name is refused at the fold's line (WF-020: a form name is unique in the whole model). */
    private void helperRules(RegelSpraakDocumentContext doc) {
        List<ExpressionBuilder.HelperMember> leden = builder.helperMembers();
        if (leden.isEmpty()) return;
        Map<String, String[]> vormen = formSites(doc);
        for (ExpressionBuilder.HelperMember h : leden) {
            String[] site = vormen.get(dev.uchar.regelspraak.emit.IrEmitter.norm(h.naam()));
            if (site != null) {
                fout(h.targetSource().line(), "de vouw over de stappen na de verzameling genereert de regel '" + h.naam()
                        + "' voor het lid van die naam op " + h.element().naam + ", en " + site[0] + " heet al zo "
                        + "(bronregel " + site[1] + ") — een vormnaam is uniek in het hele model, hoofdletters niet "
                        + "meegeteld (WF-020)");
                continue;
            }
            Expr.AttribuutRef doel = new Expr.AttribuutRef(h.naam(), h.element(), "een " + h.element().naam, null,
                    h.type(), h.targetSource());
            model.resolvedRegels.add(new ResolvedRegel(h.naam(), ResolvedRegel.Soort.GELIJKSTELLING, doel, helperValue(h),
                    null, Geldigheid.ALTIJD, List.of()));
        }
    }

    /** The read the generated rule of {@code h} sets its member from: the reached member through the steps, from the
     *  type the member stands on. */
    private static Expr.AttribuutRef helperValue(ExpressionBuilder.HelperMember h) {
        return new Expr.AttribuutRef(h.member(), h.doel(), "de " + h.element().naam, h.element().naam, h.type(),
                h.valueSource(), false, h.rollen(), List.of(), null, 0);
    }

    /** §9.10 build the {@link dev.uchar.regelspraak.ir.ResolvedRegelGroep} carriers (member RULE NAMES + the
     *  {@code recursive} marker) and run the fase-2 structural checks — a DOCUMENTED EXTENSION (see
     *  grammar/DIVERGENCES.md '§9.10 recursie / regelgroep'). Members were already resolved as ungrouped
     *  rules above (their {@link ResolvedRegel}/objectcreatie/… entries keep the bare written name); this only
     *  records the grouping and validates the recursief marker. Fase-2 checks:
     *  (a) a recursief group with NO objectcreatie member -> ERROR (nothing can advance the chain);
     *  (b) an objectcreatie member WITHOUT any voorwaarde -> WARNING (termination is not automatable — the engine
     *      fire cap is the backstop, sound because the §13.4.6/§9 idempotence guard advances one generation per
     *      instance). The ungrouped cyclic-WARNING posture (RecursieTest) is unchanged. */
    private void regelGroepen(RegelSpraakDocumentContext doc) {
        for (RegelGroepContext g : doc.regelGroep()) {
            boolean recursive = g.RECURSIEF() != null;
            int line = g.getStart().getLine();
            headAnnotation(g, "Regelgroep '" + DefinitionResolver.src(g.kaleNaam()) + "'", "§9.10", line);
            List<String> leden = new ArrayList<>();
            // Members are RULE names (IR-SPEC ch. 3.9, WF-033). A decision table is no rule form: it stands for the
            // rules of its rows, so those are what the group lists — the table's own name resolves to nothing.
            for (GroepLidContext member : g.groepLid())
                if (member.regel() != null) leden.add(DefinitionResolver.src(member.regel().regelNaam()));
                else if (member.beslistabel() != null) leden.addAll(rowRules.getOrDefault(member.beslistabel(), List.of()));
            String naam = DefinitionResolver.src(g.kaleNaam());
            model.regelGroepen.add(new dev.uchar.regelspraak.ir.ResolvedRegelGroep(naam, recursive, leden, bron(g.kaleNaam()), line));
            if (!recursive) continue;

            // §9.10 fase-2: which members are objectcreatie rules. The model holds one entry per version, named
            // "<naam> #<i>" when the rule has several, so members and creations meet on the name without the version
            // suffix, the key the verifier uses (WF-033), and each version is a member of its own.
            List<ResolvedObjectCreation> creations = model.objectCreaties.stream()
                    .filter(oc -> leden.stream().anyMatch(l -> withoutVersion(l).equals(withoutVersion(oc.naam())))).toList();
            if (creations.isEmpty()) {
                // the verifier admits a relation creation as the carrier too (ch. 5.5): a group with no creation of
                // either kind is refused in its words, one with a relation creation only in this frontend's (§9.10)
                boolean hasFactCreation = model.feitCreaties.stream()
                        .anyMatch(fc -> leden.stream().anyMatch(l -> withoutVersion(l).equals(withoutVersion(fc.naam()))));
                model.diagnostics.add(new Diagnostic(Severity.ERROR, hasFactCreation
                        ? "recursieve regelgroep '" + naam + "' zonder objectcreatieregel — de 'is recursief'-markering "
                                + "kan de keten niet laten voortschrijden (§9.10, gedocumenteerde extensie)"
                        : "recursieve regelgroep '" + naam + "': recursive group without a creation member", line));
                continue;
            }
            // each generation of a recursive group mints new instances, so its object creation carries a guard: the
            // verifier refuses an unguarded one (WF-033), and so does the frontend, in the verifier's words
            for (ResolvedObjectCreation oc : creations)
                if (oc.voorwaarde() == null)
                    model.diagnostics.add(new Diagnostic(Severity.ERROR, "recursieve regelgroep '" + naam
                            + "': unguarded object creation '" + oc.naam() + "' in a recursive group", line));
        }
    }

    // ==============================================================================================
    // §4 (PRIORITIZED-DEFAULTS-EVOLUTIE) prioritized-defaults lowering — the uchar DIALECT EXTENSION
    // 'Uitzondering op' / 'Afwijking van' / 'Uitzonderingen ongeordend'. Recognition only ever lowers the extension's
    // OWN blocks; a flat rule family is NEVER folded into a defaults group (PRIORITIZED-DEFAULTS-EVOLUTIE §4.2
    // rejects that — the source never encoded the priority). All forms LIFT already-resolved ResolvedRegels
    // out of resolvedRegels into PrioritizedDefaults nodes: WF-050 forbids both a rule AND a defaults group on
    // one target, so the lifted rule's semantics now live in the defaults group alone. See
    // grammar/DIVERGENCES.md for the surface + lowering.
    // ==============================================================================================

    /** The (subject, kenmerk-vs-attribuut, target-name) a lifted rule writes. */
    private record DefaultsTarget(Objecttype onderwerp, boolean kenmerk, String naam) {
        boolean equal(DefaultsTarget a) {
            return a != null && onderwerp == a.onderwerp && kenmerk == a.kenmerk && naam.equalsIgnoreCase(a.naam);
        }
    }

    /** §4 lower the extension blocks into {@link PrioritizedDefaults} nodes. Fails fast — one ERROR at the block,
     *  and the block with its whole tree stays un-lifted — on each refusal of grammar/DIVERGENCES.md §4: a block
     *  within a Regelgroep's scope or over a group member; an unknown carrier, a cycle of carriers, and a member of
     *  a carrier-less 'Uitzonderingen ongeordend:' block named as a carrier; an 'Afwijking van' under a node that is
     *  no base rule; a subtype's block under a node of its supertype's tree; a subtype's block under its
     *  supertype's base where a type on its line has a base of its own, where it is a member of an unordered block,
     *  where it is written before a block for its supertype, or where its group's name is taken; a carrier with
     *  ordered and unordered children, or with two unordered blocks; and, per edge and per member of a carrier-less
     *  block, a body that
     *  yields no kenmerktoekenning or gelijkstelling, a rule with more than one version, another validity window,
     *  and another target. After the lift it refuses a negated plain rule and a second writer on a group's target,
     *  and warns once per characteristic target that no group ever attributes. */
    private void defaults(RegelSpraakDocumentContext doc) {
        java.util.Set<ParserRuleContext> geweigerd = ruleGroupScope(doc);
        treeDefaults(doc, geweigerd);
        groupDefaults(doc, geweigerd);
        java.util.Set<String> blockNames = blockRuleNames(doc);
        checkFlatNegations(blockNames);   // before the single-writer check: a refused negation is no second writer
        checkOnlyWriter(blockNames);
        checkNeverDeriving();
    }

    /** The names of the rules written in an extension block: each 'Uitzondering op' or 'Afwijking van' block's own
     *  name and its carrier's, each member of an 'Uitzonderingen ongeordend' block, and the carrier such a block
     *  names. Such a rule that is still in resolvedRegels after the lift belongs to a tree or block the lowering
     *  refused, with an ERROR of its own. */
    private static java.util.Set<String> blockRuleNames(RegelSpraakDocumentContext doc) {
        java.util.Set<String> names = nodeNames(doc);
        for (UitzonderingRegelContext u : DefinitionResolver.allExceptions(doc))
            names.add(src(u.uitzonderingBasisNaam()));
        for (UitzonderingGroepContext g : DefinitionResolver.allExceptionGroups(doc))
            if (g.OP() != null) names.add(src(g.uitzonderingBasisNaam()));
        return names;
    }

    /** The names of the rules written as a node of a defaults tree: each 'Uitzondering op' and 'Afwijking van'
     *  block's own name and each member of an 'Uitzonderingen ongeordend' block. A carrier among these is no base
     *  rule: the carrier is looked up among the blocks first. */
    private static java.util.Set<String> nodeNames(RegelSpraakDocumentContext doc) {
        java.util.Set<String> names = new java.util.HashSet<>();
        for (UitzonderingRegelContext u : DefinitionResolver.allExceptions(doc)) names.add(src(u.regelNaam()));
        for (UitzonderingGroepContext g : DefinitionResolver.allExceptionGroups(doc))
            for (RegelContext member : g.regel()) names.add(src(member.regelNaam()));
        return names;
    }

    /** {@code regel} without the " #k" suffix a multi-version rule's versions carry. */
    private static String withoutVersion(String regel) {
        return regel.replaceFirst(" #\\d+$", "");
    }

    /** Dialect §4, form 1 (maintainer decision 2, 2026-09-23): a negated kenmerktoekenning stands only in a rule that
     *  becomes a default — the body of an 'Uitzondering op', a member of 'Uitzonderingen ongeordend', or the base of
     *  an 'Uitzondering op' chain. What still carries the negation after the lift is a plain Regel, and there it has
     *  no reading: without a priority it is either inert under the closed world (§9.2: an unattributed
     *  characteristic is false) or, beside a positive rule, a conflict nothing decides. Before this check it went to
     *  disk as a characteristic named "niet …" with no diagnostic, for the verifier to refuse under WF-022. A rule
     *  written in an extension block that stayed un-lifted was refused by that block's own check, which speaks
     *  alone. One ERROR per rule, not per version. */
    private void checkFlatNegations(java.util.Set<String> blockNames) {
        java.util.Set<String> reported = new java.util.HashSet<>();
        for (ResolvedRegel r : model.resolvedRegels) {
            if (!isNegated(r) || refusedTargets.contains(r) || blockNames.contains(withoutVersion(r.naam()))) continue;
            refusedTargets.add(r);
            if (!reported.add(withoutVersion(r.naam()))) continue;
            Expr.AttribuutRef doel = (Expr.AttribuutRef) r.doel();
            int table = tableLine(r.naam());
            // the lowered rule writes false into the characteristic, which the verifier refuses in these words
            model.diagnostics.add(new Diagnostic(Severity.ERROR, "regel '" + withoutVersion(r.naam()) + "'"
                    + rowTable(r.naam()) + ": characteristic target's value must be the boolean constant true",
                    table >= 0 ? table : doel.bron() == null ? 0 : doel.bron().line()));
        }
    }

    /** The line of the decision table whose row {@code regel} is, or -1 when it is no row. A row is re-parsed in a
     *  stream of its own, so the line numbers inside it count from that stream and not from the document. */
    private int tableLine(String regel) {
        for (var e : rowRules.entrySet())
            if (e.getValue().contains(withoutVersion(regel))) return e.getKey().getStart().getLine();
        return -1;
    }

    /** Dialect §4, form 1 (maintainer decision 2, 2026-09-23): a characteristic target on which every default of
     *  every defaults group decides 'onwaar' is never attributed by those groups — a false decision writes nothing.
     *  That is well-formed (it means "never", and the wire admits it), so it is no error; in a flat model such a
     *  group does nothing that leaving it out would not do as well, so the RuleChecker warns. Once per target, not
     *  per group: an unordered block spreads one target over sibling groups, and a sibling deciding 'onwaar' beside
     *  one deciding 'waar' leaves the target derivable.
     *
     *  <p>No warning where the characteristic has another writer in the subject's inheritance line, a strict
     *  ancestor or descendant. A defaults group there composes with these groups into one effective stack per type
     *  (ch. 7.10), and then the all-'onwaar' group changes outcomes: on a subtype its base replaces the inherited
     *  base, on a supertype its exceptions stay above a subtype's own base. Measured 2026-09-24 in ref, clingo and
     *  the Drools exec branch, both directions: a subtype instance the other group gives the characteristic loses
     *  it. A plain kenmerk rule there is folded into no stack (the fold takes attribute writers only), so it keeps
     *  writing on the subtype's instances and the groups stay inert beside it — measured the same day, same outcome
     *  with and without the group. The warning is left out there as well, because its hint, a missing 'waar'
     *  exception, would point away from the rule that does the writing. No warning either beside a live plain rule
     *  on the same type: {@link #checkOnlyWriter} refuses that rule (WF-050), and a warning beside its ERROR
     *  adds nothing. */
    private void checkNeverDeriving() {
        Map<String, List<PrioritizedDefaults>> perTarget = new LinkedHashMap<>();
        for (PrioritizedDefaults pd : model.prioritizedDefaults)
            if (pd.isKenmerk())
                perTarget.computeIfAbsent(targetKey(pd.onderwerp(), true, pd.doel()), k -> new ArrayList<>()).add(pd);
        for (List<PrioritizedDefaults> groups : perTarget.values()) {
            if (groups.stream().anyMatch(pd -> decidesTrue(pd.defaults()))) continue;
            PrioritizedDefaults first = groups.get(0);
            if (otherCharacteristicWriter(model.objecttypeOpNaam(first.onderwerp()), first.doel())) continue;
            boolean more = groups.size() > 1;
            Origin h = pdOrigin.getOrDefault(first, new Origin(0, "defaults-groep"));
            model.diagnostics.add(new Diagnostic(Severity.WARNING, h.head() + ": "
                    + (more ? "de defaults-groepen " : "de defaults-groep ")
                    + enumeration(groups.stream().map(PrioritizedDefaults::naam).toList()) + " op het kenmerk "
                    + first.onderwerp() + "." + first.doel() + (more ? " kennen" : " kent") + " het nooit toe — "
                    + "elke default beslist 'onwaar', en een besluit 'onwaar' schrijft niets. Dat is welgevormd, maar "
                    + "zo " + (more ? "doen de groepen" : "doet de groep") + " niets wat het weglaten van "
                    + (more ? "de groepen" : "de groep") + " niet ook doet; ga na of er een uitzondering ontbreekt die "
                    + "'waar' beslist (dialect §4)", h.line()));
        }
    }

    /** Whether characteristic {@code kenmerk} has a writer that composes with the defaults groups on {@code type}:
     *  a live plain rule on {@code type} itself (WF-050's second writer), or a defaults group or a live plain rule on
     *  a strict ancestor or descendant of {@code type} (ch. 7.10). A sibling subtype composes with nothing here. */
    private boolean otherCharacteristicWriter(Objecttype type, String kenmerk) {
        if (type == null) return false;
        for (PrioritizedDefaults pd : model.prioritizedDefaults)
            if (pd.isKenmerk() && pd.doel().equalsIgnoreCase(kenmerk)
                    && inInheritanceLine(type, model.objecttypeOpNaam(pd.onderwerp()))) return true;
        for (ResolvedRegel r : model.resolvedRegels)
            if (!refusedTargets.contains(r) && targetOf(r) instanceof DefaultsTarget d && d.kenmerk()
                    && d.naam().equalsIgnoreCase(kenmerk) && (d.onderwerp() == type || inInheritanceLine(type, d.onderwerp())))
                return true;
        return false;
    }

    /** Whether {@code ander} is a strict ancestor or a strict descendant of {@code type}. */
    private static boolean inInheritanceLine(Objecttype type, Objecttype other) {
        return other != null && other != type && (type.isEen(other) || other.isEen(type));
    }

    /** Whether some default in {@code defaults}, nested exception groups included, decides 'waar'. */
    private static boolean decidesTrue(List<Default> defaults) {
        if (defaults == null) return false;
        for (Default d : defaults) {
            if (Boolean.TRUE.equals(d.decision())) return true;
            if (d.exceptions() != null)
                for (var group : d.exceptions().leden()) if (decidesTrue(group.defaults())) return true;
        }
        return false;
    }

    /** WF-050's single writer, producer side (fix round 3, 2026-09-23): a target with a defaults group has no
     *  other writer — no plain rule beside the group, and no second group unless every group on the target is
     *  declared unordered. The lowering built each group from its own block and never looked beside it, so an
     *  'Uitzondering op' with another plain rule on its target, an exception on one row of a decision table (the
     *  other rows write on), two ordered families on one target, a family beside an unordered block, and an
     *  unordered block beside a plain rule all reached disk silently for the verifier to refuse. Keyed on the
     *  DECLARED object type, as WF-050 is: a supertype's and a subtype's stacks are two keys (the inheritance
     *  obligation of IR-SPEC ch. 6.1). One ERROR per target and clause, naming both writers; a rule whose target
     *  is refused already is not counted again, and neither is a rule written in an extension block: it is still in
     *  resolvedRegels only because the lowering refused its family or block, with an ERROR of its own, and "neem de
     *  regel op in de groep" would send the author to a block the rule already stands in. */
    private void checkOnlyWriter(java.util.Set<String> blockNames) {
        Map<String, List<PrioritizedDefaults>> perTarget = new LinkedHashMap<>();
        for (PrioritizedDefaults pd : model.prioritizedDefaults)
            perTarget.computeIfAbsent(targetKey(pd.onderwerp(), pd.isKenmerk(), pd.doel()), k -> new ArrayList<>()).add(pd);
        for (var e : perTarget.entrySet()) {
            List<PrioritizedDefaults> groups = e.getValue();
            String targetText = groups.get(0).onderwerp() + "." + groups.get(0).doel();
            String groupNames = enumeration(groups.stream().map(PrioritizedDefaults::naam).toList());
            List<String> others = new ArrayList<>();
            for (ResolvedRegel r : model.resolvedRegels) {
                if (refusedTargets.contains(r) || blockNames.contains(withoutVersion(r.naam()))) continue;
                DefaultsTarget d = targetOf(r);
                if (d != null && e.getKey().equals(targetKey(d.onderwerp().naam, d.kenmerk(), d.naam())))
                    others.add("'" + r.naam() + "'" + rowTable(r.naam()));
            }
            // both clauses in the verifier's WF-050 sentences, the groups and the other writers named before them
            if (!others.isEmpty()) {
                Origin h = pdOrigin.getOrDefault(groups.get(0), new Origin(0, "defaults-groep"));
                model.diagnostics.add(new Diagnostic(Severity.ERROR, h.head() + " ("
                        + (groups.size() == 1 ? "defaults-groep " : "defaults-groepen ") + groupNames
                        + ", doel ook geschreven door " + (others.size() == 1 ? "regel " : "de regels ")
                        + String.join(", ", others) + "): a rule also writes defaults target '" + targetText
                        + "' — an unordered plain writer would silently defeat the group's decisions", h.line()));
            }
            if (groups.size() > 1 && !groups.stream().allMatch(PrioritizedDefaults::ongeordend)) {
                Origin h = pdOrigin.getOrDefault(groups.get(groups.size() - 1), new Origin(0, "defaults-groep"));
                model.diagnostics.add(new Diagnostic(Severity.ERROR, h.head() + " (defaults-groepen " + groupNames
                        + "): multiple defaults groups on target '" + targetText + "' without the declared-unordered "
                        + "marking — priority is explicit (defaults) or explicitly absent ('unordered'), never implicit "
                        + "(ch. 7.2)", h.line()));
            }
        }
    }

    private static String targetKey(String onderwerp, boolean kenmerk, String naam) {
        return onderwerp + "|" + (kenmerk ? "kenmerk" : "attribuut") + "|" + naam.toLowerCase(java.util.Locale.ROOT);
    }

    /** " (een rij van beslistabel 'T')" when {@code regel} is a row of decision table T, else "". */
    private String rowTable(String regel) {
        for (var e : rowRules.entrySet())
            if (e.getValue().contains(regel)) return " (een rij van beslistabel '" + src(e.getKey().beslistabelNaam()) + "')";
        return "";
    }

    /** §9.10 × §4 (maintainer decision 2026-09-23): an extension block within the scope of a Regelgroep is
     *  refused, at the block's own head. The group exists only to mark recursion (§9.10), and {@code groepLid}
     *  takes rules and decision tables only — so before this refusal a group ENDED SILENTLY at the first
     *  extension block, every rule behind the block fell outside it without a word, and when the block's base was
     *  a member the lift left the group naming no rule form, which the verifier then refused under WF-033 on the
     *  group instead of on the block. Two arms, one ERROR per block naming every reason that holds:
     *  <ul>
     *  <li>POSITIONAL — the block stands directly after a group's last member, where the group would still read
     *      on if the block were a rule. Keyed on "neither a definition nor a group" rather than on the extension
     *      heads by name, so a head a later card adds is refused here by construction instead of reopening the
     *      silent end;</li>
     *  <li>MEMBERSHIP — the block's base rule is a group member, wherever the block stands. A later head that
     *      names a base of its own has to join this arm by name.</li>
     *  </ul>
     *  The membership arm speaks the verifier's WF-033 sentence, with the placement in its Dutch context where that
     *  holds too; the placement alone keeps this frontend's words, since the document it keeps off disk is
     *  well-formed. A refused block stays un-lifted together with its whole base family, as every other refusal of
     *  this pass leaves it. */
    private java.util.Set<ParserRuleContext> ruleGroupScope(RegelSpraakDocumentContext doc) {
        // Keyed on RULE names: a decision-table member stands for the rules of its rows, which is what a block
        // names as its base ("Bergingspremie 1"); the table's own name names no rule at all.
        Map<String, Membership> memberGroup = new LinkedHashMap<>();
        for (RegelGroepContext g : doc.regelGroep())
            for (GroepLidContext member : g.groepLid()) {
                if (member.regel() != null)
                    memberGroup.putIfAbsent(src(member.regel().regelNaam()), new Membership(src(g.kaleNaam()), null));
                else if (member.beslistabel() != null)
                    for (String row : rowRules.getOrDefault(member.beslistabel(), List.of()))
                        memberGroup.putIfAbsent(row, new Membership(src(g.kaleNaam()),
                                src(member.beslistabel().beslistabelNaam())));
            }
        java.util.Set<ParserRuleContext> geweigerd =
                java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        // The way out names the FIRST group header: nothing before it is in any group's scope, whereas "before
        // the header of the group at hand" can itself sit behind another group's last member.
        String firstGroup = doc.regelGroep().isEmpty() ? null : src(doc.regelGroep().get(0).kaleNaam());
        java.util.Set<String> nodeNames = nodeNames(doc);
        ParseTree preceding = null;
        for (ParseTree kind : doc.children == null ? List.<ParseTree>of() : doc.children) {
            if (kind instanceof ParserRuleContext block && !(kind instanceof DefinitieContext)
                    && !(kind instanceof RegelGroepContext) && !(kind instanceof RegelContext)
                    && !(kind instanceof BeslistabelContext)) {
                // A header with no member is already a syntax error, and has no last member to stand behind.
                String groupBefore = preceding instanceof RegelGroepContext g && !g.groepLid().isEmpty()
                        ? src(g.kaleNaam()) : null;
                String basis = carrierOf(kind);
                // Only where the lift would happen: a base that resolves to no rule gets the ordinary "no base
                // rule" refusal of the lowering, which is the true one there.
                Membership member = basis == null || resolvedOf(basis).isEmpty() ? null : memberGroup.get(basis);
                if (member != null) {
                    // the lift takes the member out of the rule forms, and the verifier refuses the group that still
                    // names it (WF-033); the placement, where it holds too, stays in the context
                    geweigerd.add(block);
                    model.diagnostics.add(new Diagnostic(Severity.ERROR, blockHead(block) + " (zijn basisregel is "
                            + (member.table() == null ? "lid" : "een rij van beslistabel '" + member.table() + "', lid")
                            + " van regelgroep '" + member.group() + "'" + (groupBefore == null ? "" : ", en het blok "
                                    + "staat direct na het laatste lid van regelgroep '" + groupBefore + "'")
                            + "): group member '" + basis + "' does not resolve to a rule form",
                            block.getStart().getLine()));
                } else if (groupBefore != null) {
                    // the placement alone: every group keeps its members, so the document is well-formed, with the
                    // block and every rule after it outside the group, and no reader refuses it
                    geweigerd.add(block);
                    model.diagnostics.add(new Diagnostic(Severity.ERROR, blockHead(block) + ": een uitzonderingsblok "
                            + "is in het dialect niet toegestaan binnen de reikwijdte van een regelgroep, noch met "
                            + "een groepslid als basisregel — het staat direct na het laatste lid van regelgroep '"
                            + groupBefore + "', waar de groep stil eindigt, zodat het blok en elke regel erna buiten de "
                            + "groep zouden vallen. Een regelgroep bestaat alleen om recursie te "
                            + "markeren (§9.10) en omvat alleen regels en beslistabellen; een defaults-blok "
                            + "(dialect §4) hoort er niet in. Plaats het blok"
                            // a carrier that is itself a block is no base rule: the message names what was written
                            + (basis == null ? "" : nodeNames.contains(basis) ? " en zijn drager"
                                    : " en zijn basisregel")
                            + " buiten elke regelgroep: vóór de eerste regelgroepkop van het document ('Regelgroep "
                            + firstGroup + "'), of achter een definitie die de groep afsluit",
                            block.getStart().getLine()));
                }
            }
            preceding = kind;
        }
        return geweigerd;
    }

    /** A rule's place in a Regelgroep: the group, and the decision table it is a row of (null for a plain
     *  rule member). */
    private record Membership(String group, String table) {}

    /** The carrier an extension block names — the rule before the ':' of 'Uitzondering op', 'Afwijking van' and
     *  'Uitzonderingen ongeordend op' — or null for a block that names none. */
    private static String carrierOf(ParseTree block) {
        if (block instanceof UitzonderingRegelContext u) return src(u.uitzonderingBasisNaam());
        if (block instanceof UitzonderingGroepContext g && g.OP() != null) return src(g.uitzonderingBasisNaam());
        return null;
    }

    /** WF-022, producer side (fix round 2, 2026-09-23): a rule writes a declared attribute or characteristic of
     *  its OWN universal subject. The frontend builds a target on the subject only, so a sentence that names
     *  another instance's attribute — "De vakpremie van het strandvak van een Jutter moet …" — came out as an
     *  attribute named 'vakpremie van het strandvak' on Jutter: the name swallowed the navigation, the type
     *  stayed unknown, and the document reached disk with {@code diagnostics: []} for the verifier to refuse
     *  under WF-022 and WF-034. The same for an undeclared attribute, and for a characteristic whose subject is a
     *  navigation, which resolves to no object type at all. Each is an ERROR now, naming the form. An undeclared
     *  characteristic on a resolved subject is not judged here: a negated one ("is niet X" with X undeclared) is
     *  refused where the name is read ({@link #kenmerk}, dialect §4), and a positive one still reaches the
     *  verifier's WF-022 — no card has taken that one yet. */
    private void checkRuleTargets() {
        for (ResolvedRegel r : model.resolvedRegels) {
            if (!(r.doel() instanceof Expr.AttribuutRef a)) continue;
            int line = a.bron() == null ? 0 : a.bron().line();
            String written = a.bron() == null ? a.attribuut() : a.bron().snippet();
            // a name that is both an attribute and a characteristic of the subject decides no member, and so no rule
            // kind; before this check a rule on it ended in the emitter's exception
            if (a.objecttype() != null && a.attribuut() != null && a.objecttype().attribuut(a.attribuut()) != null
                    && a.objecttype().kenmerkNaam(a.attribuut()) != null) {
                refusedTargets.add(r);
                model.diagnostics.add(new Diagnostic(Severity.ERROR, "regeldoel '" + written + "': target '"
                        + a.attribuut() + "' names both an attribute and a characteristic of '" + a.objecttype().naam
                        + "': which member a rule writes decides its kind, and this name decides nothing", line));
                continue;
            }
            String fout = null;
            if (a.objecttype() == null)
                fout = "het onderwerp '" + a.subjectSource() + "' lost niet op tot een objecttype: een regel schrijft "
                        + "een attribuut of kenmerk van zijn eigen universele onderwerp ('een <Objecttype>'), en een "
                        + "navigatie naar een ander exemplaar bouwt deze frontend niet als regeldoel";
            else if (r.soort() != ResolvedRegel.Soort.KENMERKTOEKENNING && a.objecttype().attribuut(a.attribuut()) == null
                    && cellAsName(a.objecttype(), a.attribuut()) != null)
                fout = "'" + a.attribuut() + "' is een " + cellAsName(a.objecttype(), a.attribuut()) + NO_CELL_TARGET;
            else if (r.soort() != ResolvedRegel.Soort.KENMERKTOEKENNING && a.objecttype().attribuut(a.attribuut()) != null
                    && cellAfterSubject(a.objecttype(), a.attribuut(), a.subjectSource()) != null)
                fout = "het doel is een " + cellAfterSubject(a.objecttype(), a.attribuut(), a.subjectSource())
                        + NO_CELL_TARGET + "; zonder dit hek schreef de zin het hele attribuut";
            else if (a.navigatieRol() != null || (a.navigationPath() != null && !a.navigationPath().isEmpty()))
                // "Het kadegeld van zijn thuishaven moet …": the target reads another instance's attribute, which
                // ended in the emitter's exception, since the target fixes the rule's subject
                fout = "het doel navigeert via '" + a.subjectSource() + "': een regel schrijft een attribuut van zijn "
                        + "eigen universele onderwerp; schrijf de regel op het objecttype dat het attribuut draagt ('De "
                        + "<attribuut> van een <Objecttype> …')";
            else if (r.soort() != ResolvedRegel.Soort.KENMERKTOEKENNING && a.objecttype().attribuut(a.attribuut()) == null)
                fout = a.attribuut().matches("(?i).*\\svan\\s.*")
                        ? "het doel is een genavigeerde lezing: '" + a.attribuut() + "' is geen attribuut van "
                                + a.objecttype().naam + " — de naam slokte de navigatie op. Een regel schrijft een "
                                + "attribuut van zijn eigen universele onderwerp; schrijf de regel op het objecttype dat "
                                + "het attribuut draagt ('De <attribuut> van een <Objecttype> …')"
                        : "'" + a.attribuut() + "' is geen gedeclareerd attribuut van " + a.objecttype().naam;
            if (fout != null) {
                refusedTargets.add(r);
                model.diagnostics.add(new Diagnostic(Severity.ERROR, "regeldoel '" + written + "': " + fout
                        + " (WF-022, IR-SPEC ch. 3.1)", line));
            }
        }
    }

    /** The wire has no cell assignment: a rule's target is an attribute reference (WF-022 refuses any other node
     *  there), and the dimension-cell node is a READ (IR-SPEC ch. 4.3). */
    private static final String NO_CELL_TARGET = " — een regel kent op de wire een attribuut toe en geen cel (het doel "
            + "is een attribuutverwijzing; de dimensiecel bestaat alleen als lezing)";

    /** "cel van het gedimensioneerde attribuut 'premie' (dimensie seizoen, label 'voorjaar')" when {@code naam} is
     *  a dimensioned attribute of {@code ot} with its preposition and one of its labels written behind it as one
     *  name ("premie in voorjaar"), else null. Single-dimension attributes, the suffix form — the shape
     *  {@code ExpressionBuilder.dimensionCell} reads. */
    private String cellAsName(Objecttype ot, String naam) {
        for (var e : ot.dimensionedMembers().entrySet()) {
            if (e.getValue().size() != 1) continue;
            RegelSpraakModel.Dimension d = model.dimensionByName(e.getValue().get(0));
            if (d == null || d.voorzetsel() == null) continue;
            for (String label : d.waarden())
                if ((e.getKey() + " " + d.voorzetsel() + " " + label).equalsIgnoreCase(naam.trim()))
                    return cellText(e.getKey(), d, label);
        }
        return null;
    }

    /** The same cell written after the SUBJECT ("De premie van een Jutter in voorjaar"): the parse left the
     *  attribute bare and put the preposition and label into the subject text. Null when it is not. */
    private String cellAfterSubject(Objecttype ot, String attribuut, String subjectText) {
        if (subjectText == null) return null;
        for (String dn : ot.dimensiesVan(attribuut)) {
            RegelSpraakModel.Dimension d = model.dimensionByName(dn);
            if (d == null || d.voorzetsel() == null) continue;
            for (String label : d.waarden())
                if (subjectText.toLowerCase(java.util.Locale.ROOT).endsWith(
                        (" " + d.voorzetsel() + " " + label).toLowerCase(java.util.Locale.ROOT)))
                    return cellText(ot.attributeName(attribuut), d, label);
        }
        return null;
    }

    private static String cellText(String attribuut, RegelSpraakModel.Dimension d, String label) {
        return "cel van het gedimensioneerde attribuut '" + attribuut + "' (dimensie " + d.naam() + ", label '"
                + label + "')";
    }

    /** WF-112 (maintainer decision 2026-09-23), producer side: a time window never sits under 'geen van de' or
     *  a counting quantifier — as a member of such a composite at any depth, or in the filter or test of such a
     *  quantifier fold. The window gates the RULE (RUNTIME-SEMANTICS ch. 3.10); a quantifier that complements or
     *  counts its verdict reads it as something no clause specifies. Before this fence each such document was
     *  written without a word and refused by the verifier. The walk mirrors the verifier's: every condition the
     *  emitter writes (the roots {@code Resolver.reportUnresolved} sweeps), into the conditions nested in
     *  expressions, with {@link Conditie#containsTimeWindow} as the window test — the reach of its
     *  {@code cond_has_window}. One ERROR per offending composite or fold, so {@link Resolver#irModus} writes
     *  nothing. */
    private void checkWindowPositions() {
        for (ResolvedRegel r : model.resolvedRegels) {
            windowPosition(r.voorwaarde()); windowPositionIn(r.doel()); windowPositionIn(r.waarde());
        }
        for (var d : model.resolvedDayKindDefinitions) windowPosition(d.voorwaarde());
        for (var oc : model.objectCreaties) {
            windowPosition(oc.voorwaarde());
            for (var t : oc.initialisations()) windowPositionIn(t.waarde());
        }
        for (var v : model.verdelingen) windowPosition(v.voorwaarde());
        for (var c : model.consistenties) windowPosition(c.assertie());
        for (var fc : model.feitCreaties) windowPosition(fc.voorwaarde());
        java.util.Set<Conditie> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (var pd : model.prioritizedDefaults) windowPositionDefaults(pd.defaults(), seen);
    }

    /** The defaults tree's justifications and consequences. A restrictive exception repeats its carrier's conjuncts,
     *  the very same nodes, so each justification is walked with {@link Conditie#eachConjunctOnce}: a window under
     *  a counting quantifier in a base's condition is one refusal, not one per exception above it. */
    private void windowPositionDefaults(List<Default> defaults, java.util.Set<Conditie> seen) {
        if (defaults == null) return;
        for (Default d : defaults) {
            Conditie.eachConjunctOnce(d.voorwaarde(), seen, this::windowPosition);
            windowPositionIn(d.consequentie());
            if (d.exceptions() != null)
                for (var group : d.exceptions().leden()) windowPositionDefaults(group.defaults(), seen);
        }
    }

    private void windowPosition(Conditie c) {
        if (c == null) return;
        switch (c) {
            case Conditie.Samengesteld s -> {
                if (countsOrNegates(s.kwantor()) && s.onderdelen().stream().anyMatch(Conditie::containsTimeWindow))
                    windowError(s.kwantor(), s.threshold(), s.bron(), "als lid van een samengestelde voorwaarde");
                s.onderdelen().forEach(this::windowPosition);
            }
            case Conditie.QuantifierFold k -> {
                if (countsOrNegates(k.kwantor()) && (Conditie.containsTimeWindow(k.filter()) || Conditie.containsTimeWindow(k.test())))
                    windowError(k.kwantor(), k.threshold(), k.bron(), "in het filter of de toets van een kwantorvouw");
                windowPosition(k.filter());
                windowPosition(k.test());
            }
            case Conditie.Vergelijking v -> { windowPositionIn(v.links()); windowPositionIn(v.rechts()); }
            case Conditie.Predicaat p -> windowPositionIn(p.operand());
            case Conditie.Periode p -> { windowPositionIn(p.vanaf()); windowPositionIn(p.tot()); }
            default -> { }
        }
    }

    private void windowPositionIn(Expr e) {
        if (e == null) return;
        switch (e) {
            case Expr.Binair b -> { windowPositionIn(b.links()); windowPositionIn(b.rechts()); }
            case Expr.FunctionCall f -> {
                f.arguments().forEach(this::windowPositionIn);
                windowPositionIn(f.emptyFallback());
                windowPosition(f.filter());
            }
            case Expr.Afgerond a -> windowPositionIn(a.basis());
            case Expr.Begrensd b -> {
                windowPositionIn(b.basis()); windowPositionIn(b.minimum()); windowPositionIn(b.maximum());
            }
            case Expr.TextComposition t -> t.parts().forEach(d -> {
                if (d instanceof Expr.TextComposition.EmbeddedExpression i) windowPositionIn(i.expr());
            });
            case Expr.DimensionCellRead dc -> windowPositionIn(dc.attribuut());
            case Expr.Let l -> { windowPositionIn(l.value()); windowPositionIn(l.body()); }
            case Expr.IfThenElse k -> {
                windowPosition(k.condition()); windowPositionIn(k.then()); windowPositionIn(k.otherwise());
            }
            case Expr.InstanceSetUnion u -> u.branches().forEach(t -> windowPosition(t.filter()));
            default -> { }
        }
    }

    /** 'geen van de' and the three counting quantifiers: the ones that complement or count a verdict. */
    private static boolean countsOrNegates(Conditie.Kwantor k) {
        return k == Conditie.Kwantor.GEEN || k == Conditie.Kwantor.TEN_MINSTE
                || k == Conditie.Kwantor.TEN_HOOGSTE || k == Conditie.Kwantor.PRECIES;
    }

    /**
     * A navigated read that no binding supplies, and a fold the wire cannot spell (IR-SPEC ch. 4.1a, ch. 4.3). The
     * emitter writes a path or a range only where one relation is derivable — by the written role or chain; else by
     * the one relation between the two types, or, where the pair has several, by the role the written noun names —
     * and refuses the model otherwise: written without it, the read would be the subject's own and the fold would
     * have no range (grammar/DIVERGENCES.md, "§13.4.1 A navigated read binds its written role"). It refuses a fold
     * whose source carries a written step, or reads the subject's own attribute under a kind that folds instances,
     * as well. This walk finds those positions first and refuses them with an ERROR, so a model this resolver built
     * never reaches the emitter's refusal. The two tests are the emitter's own
     * ({@link dev.uchar.regelspraak.emit.IrEmitter#unboundRead},
     * {@link dev.uchar.regelspraak.emit.IrEmitter#foldRefusalOf}); the walk follows the emitter's traversal — after
     * the lift, so the defaults are covered — and each position's subject is the one the emitter writes it from. A
     * read whose binding reaches a to-many role in a slot that takes one value is refused here too, in WF-053's
     * sentence for that hop: the emitter writes it, and the verifier refuses the document. A read that binds, but
     * whose member is not where its roles bring it, is refused here as well ({@link #memberError}). A set reading and a
     * window bound are left out: the set fence and the window's own check refuse every one of those that navigates,
     * so one defect keeps one refusal; a window bound that is an own read is judged for the roles its name took in.
     */
    private void checkPaths() {
        Set<Object> reported = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (ResolvedRegel r : model.resolvedRegels) {
            Objecttype s = r.doel() instanceof Expr.AttribuutRef d ? d.objecttype() : null;
            navigation(r.waarde(), s, reported);
            navigation(r.voorwaarde(), s, reported);
        }
        for (var d : model.resolvedDayKindDefinitions) navigation(d.voorwaarde(), null, reported);
        for (var oc : model.objectCreaties) {
            for (var t : oc.initialisations()) navigation(t.waarde(), oc.subject(), reported);
            navigation(oc.voorwaarde(), oc.subject(), reported);
        }
        for (var v : model.verdelingen)
            if (v.methode() != ResolvedVerdeling.Methode.COMPLEX) navigation(v.voorwaarde(), v.sourceType(), reported);
        for (var c : model.consistenties) navigation(c.assertie(), c.subject(), reported);
        for (var fc : model.feitCreaties) navigation(fc.voorwaarde(), fc.anchor(), reported);
        for (var pd : model.prioritizedDefaults)
            navigationDefaults(pd.defaults(), model.objecttypen.get(pd.onderwerp()), reported);
    }

    /**
     * WF-034, producer side, for one shape of read: a name that took in a word before 'van'. The name of a read runs
     * over every inner 'van', so "het maximum van de romplengte van de Schuit" — no function of the language — became a
     * read of an attribute 'maximum van de romplengte' of Schuit, went to disk with {@code diagnostics: []}, and the
     * verifier refused the document. Refused here, in the verifier's sentence, where the name the emitter writes is no
     * member of the type it is read on and its part behind a 'van' is a declared attribute of that type. A name whose
     * part behind a 'van' is a role is {@link #memberError}'s, in the walk of {@link #checkPaths}. An own read
     * of an undeclared name with neither in it is left to the verifier: the casus of the parity zone reads such names,
     * which only its replay slices declare, and an ERROR would stop its emission (grammar/DIVERGENCES.md, "An
     * undeclared attribute read"). A set reading is the set fence's to refuse; the walk reaches every other written
     * read, window bounds and fold sources included, after the lift, so the defaults are covered.
     */
    private void checkSwallowedNames() {
        Set<Object> reported = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        everyWrittenExpr(x -> {
            if (x instanceof Expr.AttribuutRef a && !a.set() && reported.add(a)) {
                String fout = swallowedNameError(a);
                if (fout != null)
                    model.diagnostics.add(new Diagnostic(Severity.ERROR, fout, a.bron() == null ? 0 : a.bron().line()));
            }
            return false;
        });
    }

    /** A constant the wire has no node for: the bound day of a day-kind definition is a BoundDay and the two session
     *  values a ContextValue, and every other name — "hij" written as a value, "de naam … op hij" — would be a
     *  ContextValue outside WF-098's closed set. Before this check such a document ended in the emitter's exception;
     *  now it is refused in the verifier's WF-098 sentence. */
    private void checkConstants() {
        Set<Object> reported = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        everyWrittenExpr(x -> {
            if (x instanceof Expr.Constante c && c.naam() != null && !c.naam().equals("de dag")
                    && !dev.uchar.regelspraak.ir.Spelling.CONTEXT_VALUE_NAME.rows().containsKey(c.naam())
                    && reported.add(c))
                model.diagnostics.add(new Diagnostic(Severity.ERROR, "'" + c.naam() + "' als waarde ('" + c.bron()
                        + "'): context value '" + c.naam() + "' is not one of the two admissible names evaluationDate, "
                        + "evaluationYear: the context-value name is a closed set, spelled byte for byte",
                        c.bron() == null ? 0 : c.bron().line()));
            return false;
        });
    }

    /** Every expression the emitter writes, after the lift, handed to {@code visitor} (which returns false to walk on):
     *  rule values and conditions, day-kind conditions, creation values and conditions, allocation and fact-creation
     *  conditions, consistency assertions and the defaults, window bounds and fold sources included. */
    private void everyWrittenExpr(java.util.function.Predicate<Expr> visitor) {
        for (ResolvedRegel r : model.resolvedRegels) {
            Expr.contains(r.waarde(), visitor);
            Conditie.containsOperand(r.voorwaarde(), visitor);
        }
        for (var d : model.resolvedDayKindDefinitions) Conditie.containsOperand(d.voorwaarde(), visitor);
        for (var oc : model.objectCreaties) {
            for (var t : oc.initialisations()) Expr.contains(t.waarde(), visitor);
            Conditie.containsOperand(oc.voorwaarde(), visitor);
        }
        for (var v : model.verdelingen) Conditie.containsOperand(v.voorwaarde(), visitor);
        for (var c : model.consistenties) Conditie.containsOperand(c.assertie(), visitor);
        for (var fc : model.feitCreaties) Conditie.containsOperand(fc.voorwaarde(), visitor);
        for (var pd : model.prioritizedDefaults) writtenDefaults(pd.defaults(), visitor);
    }

    private static void writtenDefaults(List<Default> defaults, java.util.function.Predicate<Expr> visitor) {
        if (defaults == null) return;
        for (Default d : defaults) {
            Conditie.containsOperand(d.voorwaarde(), visitor);
            Expr.contains(d.consequentie(), visitor);
            if (d.exceptions() != null)
                for (ExceptionGroup g : d.exceptions().leden()) writtenDefaults(g.defaults(), visitor);
        }
    }

    /** The refusal of {@link #checkSwallowedNames} for {@code a}, or null where its name is not of that shape: the
     *  first 'van' behind which a declared attribute (or its written plural) of the read's type stands. */
    private static String swallowedNameError(Expr.AttribuutRef a) {
        Objecttype ot = a.objecttype();
        String naam = a.attribuut();
        if (ot == null || naam == null || !(a.type() instanceof Datatype.Onbekend) || ot.hasMember(naam)) return null;
        java.util.regex.Matcher m = VAN.matcher(naam);
        while (m.find()) {
            String attribuut = PluralMatcher.findAttribute(ot, stripArticle(naam.substring(m.end())));
            if (attribuut != null)
                return "lezing '" + a.bron() + "', waarin '" + naam.substring(0, m.start()) + " van' vóór het "
                        + "attribuut '" + attribuut + "' één naam werd: '" + naam
                        + "' is neither an attribute nor a characteristic of '" + ot.naam + "'";
        }
        return null;
    }

    private static final java.util.regex.Pattern VAN = java.util.regex.Pattern.compile("\\s+van\\s+");

    /** A carrier's conjuncts repeat under its exceptions as the very same nodes, so {@code gemeld} keeps one
     *  refusal per node. */
    private void navigationDefaults(List<Default> defaults, Objecttype subject, Set<Object> reported) {
        if (defaults == null) return;
        for (Default d : defaults) {
            navigation(d.voorwaarde(), subject, reported);
            navigation(d.consequentie(), subject, reported);
            if (d.exceptions() != null)
                for (ExceptionGroup g : d.exceptions().leden()) navigationDefaults(g.defaults(), subject, reported);
        }
    }

    private void navigation(Expr e, Objecttype subject, Set<Object> reported) {
        if (e == null) return;
        switch (e) {
            case Expr.AttribuutRef a -> {
                if (a.set() || !reported.add(a)) return;
                String fout = dev.uchar.regelspraak.emit.IrEmitter.unboundRead(model, subject, a)
                        ? unboundReadError(a, subject) : manyStepError(a, subject);
                if (fout == null) fout = memberError(a, subject, false);
                if (fout != null) model.diagnostics.add(new Diagnostic(Severity.ERROR, fout, a.bron().line()));
                else reportDoubleReading(a);
            }
            case Expr.FunctionCall f -> {
                boolean foldSource = dev.uchar.regelspraak.emit.IrEmitter.isFoldSource(f);
                var refusal = dev.uchar.regelspraak.emit.IrEmitter.foldRefusalOf(model, subject, f);
                if (refusal != null && reported.add(f))
                    model.diagnostics.add(new Diagnostic(Severity.ERROR, foldError(f, subject, refusal),
                            f.bron().line()));
                // a fold's source is read through its range, never as a read of its own
                for (int i = foldSource ? 1 : 0; i < f.arguments().size(); i++)
                    navigation(f.arguments().get(i), subject, reported);
                navigation(f.emptyFallback(), subject, reported);
                // the filter reads each contributor; a dimension fold has none and reads the subject's labels
                Objecttype element = f.dimensieSelectie() == null ? f.elementObjectType() : null;
                navigation(f.filter(), element != null ? element : subject, reported);
            }
            case Expr.Binair b -> {
                navigation(b.links(), subject, reported);
                navigation(b.rechts(), subject, reported);
            }
            case Expr.Afgerond a -> navigation(a.basis(), subject, reported);
            case Expr.Begrensd b -> {
                navigation(b.basis(), subject, reported);
                navigation(b.minimum(), subject, reported);
                navigation(b.maximum(), subject, reported);
            }
            case Expr.TextComposition t -> {
                for (Expr.TextComposition.Part d : t.parts())
                    if (d instanceof Expr.TextComposition.EmbeddedExpression i) navigation(i.expr(), subject, reported);
            }
            case Expr.DimensionCellRead c -> navigation(c.attribuut(), subject, reported);
            case Expr.Let l -> {
                navigation(l.value(), subject, reported);
                navigation(l.body(), subject, reported);
            }
            case Expr.InstanceSetUnion u -> u.branches().forEach(t ->
                    navigation(t.filter(), t.elementType() != null ? t.elementType() : subject, reported));
            // the emitter writes no conditional expression at all (IR-SPEC ch. 4.10), so nothing is read below it
            case Expr.IfThenElse ignored -> { }
            case Expr.Literal ignored -> { }
            case Expr.ParameterRef ignored -> { }
            case Expr.VariableRef ignored -> { }
            case Expr.BinderRef ignored -> { }
            case Expr.Constante ignored -> { }
            case Expr.Unresolved ignored -> { }
        }
    }

    private void navigation(Conditie c, Objecttype subject, Set<Object> reported) {
        if (c == null) return;
        switch (c) {
            case Conditie.Vergelijking v -> {
                navigation(v.links(), subject, reported);
                navigation(v.rechts(), subject, reported);
            }
            case Conditie.Predicaat p -> navigation(p.operand(), subject, reported);
            case Conditie.Samengesteld s -> s.onderdelen().forEach(o -> navigation(o, subject, reported));
            // the filter and the test read each contributor
            case Conditie.QuantifierFold k -> {
                Objecttype element = k.elementObjectType() != null ? k.elementObjectType() : subject;
                navigation(k.filter(), element, reported);
                navigation(k.test(), element, reported);
            }
            // every bound that navigates is the window check's refusal; an own read whose name took in a role is not
            case Conditie.Periode p -> {
                windowBoundMember(p.vanaf(), subject, reported);
                windowBoundMember(p.tot(), subject, reported);
            }
            case Conditie.Kenmerk ignored -> { }
            case Conditie.Rollidmaatschap r -> roleCheckBinding(r, r.rol(), r.otherToFiller(), r.bron(), subject, reported);
            case Conditie.FarRoleMembership r ->
                    roleCheckBinding(r, r.backref(), r.filler(), r.bron(), subject, reported);
            case Conditie.RuleStatusCheck ignored -> { }
            case Conditie.Unresolved ignored -> { }
        }
    }

    /** A role check whose binding is not one relation: the emitter writes a role check as its fact type and role, and
     *  refused it with an exception where the written role names none from the subject or several ("de Haven heeft
     *  een ligger" where the Haven plays the written role in two fact types). The refusal is the loader's sentence for
     *  the check without its binding. A day kind has no subject, and the day-kind check refuses the role check there
     *  already. */
    private void roleCheckBinding(Conditie c, String rol, Objecttype other, Bron bron, Objecttype subject,
                                 Set<Object> reported) {
        if (subject == null) return;
        int n = dev.uchar.regelspraak.emit.IrEmitter.roleCheckBindings(model, subject, c);
        if (n == 1 || !reported.add(c)) return;
        String against = " van " + subject.naam + " tegenover " + (other == null ? "?" : other.naam);
        model.diagnostics.add(new Diagnostic(Severity.ERROR, "rolcheck '" + bron + "' zonder binding ("
                + (n == 0 ? "geen feittype kent de rol '" + rol + "'" + against
                          : n + " feittypen kennen de rol '" + rol + "'" + against)
                + "): missing field 'factType'", bron == null ? 0 : bron.line()));
    }

    /** The refusal of a read without a binding: what did not bind, then the verifier's WF-053 sentence for the
     *  document the emitter would write without the path — a read that names another type than the subject's.
     *  Where the read names the subject's own type, or there is no subject, that document is the subject's own
     *  read, which no reader refuses, and the refusal keeps this frontend's words. */
    private String unboundReadError(Expr.AttribuutRef a, Objecttype subject) {
        String van = subject == null ? "zonder onderwerp" : "vanaf " + subject.naam;
        String reason;
        if (a.navigationPath() != null && !a.navigationPath().isEmpty())
            reason = "de geschreven keten is " + van + " niet stap voor stap te binden";
        else if (a.navigatieRol() != null)
            reason = "de geschreven rol '" + a.navigatieRol() + "' wijst " + van + " niet precies één relatie aan";
        else reason = pairReason(subject, a.objecttype(), a.subjectSource());
        String head = "genavigeerde lezing '" + a.bron() + "' zonder binding (" + reason + ")";
        if (subject != null && a.objecttype() != null && a.objecttype() != subject)
            return head + ": an attribute reference without a path is the subject's own read, so its subjectType is "
                    + "the subject type '" + subject.naam + "' — '" + a.objecttype().naam + "' names another type, "
                    + "which only a navigation reaches";
        return head + ": zonder pad zou de wire haar lezen als een eigen attribuut van het onderwerp, en dat is niet "
                + "wat er staat";
    }

    /** The refusal of a read whose path reaches a to-many role in a slot that takes one value, in the verifier's
     *  WF-053 sentence, one per such hop; null where no hop is to-many. A §13.4.7 fact creation's condition on the
     *  created relation's target is one: the wire reads that condition from the anchor, so the target is reached
     *  over the to-many role. */
    private String manyStepError(Expr.AttribuutRef a, Objecttype subject) {
        List<RegelSpraakModel.RoleBindingRef> pad =
                dev.uchar.regelspraak.emit.IrEmitter.navigationBindings(model, subject, a);
        List<String> sentences = new ArrayList<>();
        for (int i = 0; i < pad.size(); i++)
            if (toMany(pad.get(i))) sentences.add(manyStepSentence(i, pad.get(i).rol()));
        return sentences.isEmpty() ? null
                : "genavigeerde lezing '" + a.bron() + "' over een meervoudige rol: " + String.join("; ", sentences);
    }

    /** WF-053's sentence for hop {@code i} over the to-many role {@code rol} of a read that takes one value. */
    private static String manyStepSentence(int i, String rol) {
        return "hop " + i + " reaches the to-many role '" + rol + "' in a scalar slot — a to-many step needs an "
                + "aggregation, never a silent first pick";
    }

    /** A window bound that is an own read: {@link #memberError} for the role its name took in. A bound that navigates is
     *  the window check's to refuse (WF-111), so one defect keeps one refusal. */
    private void windowBoundMember(Expr limit, Objecttype subject, Set<Object> reported) {
        if (!(limit instanceof Expr.AttribuutRef a) || a.set() || !reported.add(a)) return;
        String fout = memberError(a, subject, true);
        if (fout != null) model.diagnostics.add(new Diagnostic(Severity.ERROR, fout, a.bron().line()));
        else reportDoubleReading(a);
    }

    /** A declared attribute with 'van' in its name wins over the navigation the same words spell — "de premie van de
     *  jutter van de Vondst" reads the Vondst's 'premie van de jutter', not the 'premie' of its jutter — and until
     *  2026-10-01 the author was not told (maintainer decision: a WARNING, the declaration keeps winning). The walk is
     *  {@link #memberError}'s: from the type the read stands on, over the roles its name took in, outward; it warns where a
     *  role is walked and the type it reaches declares the member the remaining words name. A to-many step ends the
     *  walk: that navigation is no read of one value. */
    private void reportDoubleReading(Expr.AttribuutRef a) {
        String naam = a.attribuut();
        Objecttype t = a.objecttype();
        if (a.set() || naam == null || t == null || t.attribuut(naam) == null) return;
        String[] parts = naam.split("\\s+van\\s+");
        Objecttype cur = t;
        for (int i = parts.length - 1; i >= 1; i--) {
            RegelSpraakModel.ChainHop hop = model.writtenHop(cur, stripArticle(parts[i]));
            if (hop == null || model.naarVeelRol(cur, hop.doel(), hop.rol())) return;
            cur = hop.doel();
            String member = PluralMatcher.findAttribute(cur, String.join(" van ", java.util.Arrays.copyOfRange(parts, 0, i)));
            if (member != null) {
                var d = new Diagnostic(Severity.WARNING, "lezing '" + a.bron() + "': het gedeclareerde attribuut '"
                        + naam + "' van '" + t.naam + "' wint; dezelfde woorden lezen ook als het attribuut '" + member
                        + "' van '" + cur.naam + "' over de rol '" + hop.rol() + "', en die lezing geldt hier niet",
                        a.bron() == null ? 0 : a.bron().line());
                if (!model.diagnostics.contains(d)) model.diagnostics.add(d);
                return;
            }
        }
    }

    /**
     * The refusal of a read whose member is not where its written roles bring it, or null (grammar/DIVERGENCES.md,
     * "An undeclared attribute read"). The name of a read runs over every inner 'van', so "de toeslag van de jutter van
     * de Vondst" is read as an attribute 'toeslag van de jutter' of the Vondst where the Jutter has no 'toeslag', and
     * "de toeslag van zijn jutter" navigates to a Jutter that has none; both went to disk with {@code diagnostics: []},
     * and every reader refused them. The walk starts where the read stands — its own type, or the type its path
     * reaches — and takes the roles its name took in, outward, as the resolver's chain walk does
     * ({@link RegelSpraakModel#writtenHop}). Refused where at least one role is walked: a member missing on the
     * filler the walk ends on, in the verifier's sentence for the document the author wrote (the navigation to the one
     * subtype that declares the member, WF-053, else the read on the filler, WF-034); a read that names a subtype of
     * that filler as its endpoint (WF-053); a characteristic read as a value (WF-080); a to-many step (WF-053); and a
     * chain the resolver did not lay although its member stands at the end of it, or a step that names a role or a type
     * but binds no single relation, in WF-034's sentence for the document it would write. A read that names no type at
     * all is refused in WF-034's sentence for the reference without a subject type. An own read with no role or type
     * name in it — "de datum van aanmelding van de Vondst", where 'aanmelding' names neither — stays the verifier's,
     * because the casus of the parity zone reads such names, which only its replay slices declare. With
     * {@code onlyOwn} only an own read is judged: a window bound that navigates is the window check's refusal.
     */
    private String memberError(Expr.AttribuutRef a, Objecttype subject, boolean onlyOwn) {
        String naam = a.attribuut();
        if (subject == null || a.set() || naam == null) return null;
        Objecttype t = a.objecttype();
        if (t == null) return onlyOwn ? null : withoutTypeError(a, subject, naam);
        if (swallowedNameError(a) != null) return null;
        List<RegelSpraakModel.RoleBindingRef> pad =
                dev.uchar.regelspraak.emit.IrEmitter.navigationBindings(model, subject, a);
        if (onlyOwn && !pad.isEmpty()) return null;
        String rol = pad.isEmpty() ? null : pad.get(pad.size() - 1).rol();
        Objecttype fillerType = pad.isEmpty() ? null : roleFiller(pad.get(pad.size() - 1));
        if (t.attribuut(naam) != null || t.kenmerkNaam(naam) != null) {
            if (pad.isEmpty()) return null;    // an own read; of a characteristic, the verifier's (WF-080)
            if (downward(fillerType, t)) return downwardError(a, t, fillerType, rol);
            return t.attribuut(naam) != null ? null : characteristicAsValueError(a, naam, t, rol);
        }
        String[] parts = naam.split("\\s+van\\s+");
        Objecttype cur = t;
        String member = naam;
        int hops = pad.size();
        for (int i = parts.length - 1; i >= 1; i--) {
            String step = stripArticle(parts[i]);
            RegelSpraakModel.ChainHop hop = model.writtenHop(cur, step);
            if (hop == null) {
                // a role or type name that binds no step from here: the author wrote a navigation, not this name
                String reason = stepReason(cur, step);
                if (reason == null) break;
                return "genavigeerde lezing '" + a.bron() + "' bindt niet (" + reason + "), dus zij is gelezen als het "
                        + "attribuut '" + naam + "' van '" + t.naam + "': '" + naam
                        + "' is neither an attribute nor a characteristic of '" + t.naam + "'";
            }
            if (model.naarVeelRol(cur, hop.doel(), hop.rol()))
                return "genavigeerde lezing '" + a.bron() + "' over een meervoudige rol: "
                        + manyStepSentence(hops, hop.rol());
            hops++;
            fillerType = model.rolDoeltypeNaarSubject(cur, hop.rol());
            cur = hop.doel();
            rol = hop.rol();
            member = String.join(" van ", java.util.Arrays.copyOfRange(parts, 0, i));
            String attribuut = PluralMatcher.findAttribute(cur, member);
            if (attribuut != null)
                return downward(fillerType, cur) ? downwardError(a, cur, fillerType, rol)
                        : "genavigeerde lezing '" + a.bron() + "' is niet als navigatie gelegd: '" + attribuut
                        + "' staat op '" + cur.naam + "', de vuller van de rol '" + rol + "', maar zij is gelezen als het "
                        + "attribuut '" + naam + "' van '" + t.naam + "': '" + naam
                        + "' is neither an attribute nor a characteristic of '" + t.naam + "'";
        }
        if (hops == 0) return null;
        if (downward(fillerType, cur)) return downwardError(a, cur, fillerType, rol);
        if (cur.kenmerkNaam(member) != null) return characteristicAsValueError(a, member, cur, rol);
        return memberMissingError(a, member, cur, rol);
    }

    /** Why the step {@code step} binds no hop from {@code van}, where it names an object type or a role anywhere in
     *  the model — a word that only stands in a navigation; null where it names neither. A type name is read as the
     *  type, as {@link RegelSpraakModel#chainHop} reads it, so the pair decides. */
    private String stepReason(Objecttype van, String step) {
        Objecttype type = model.objecttypeOpNaam(step);
        if (type != null) {
            int n = model.connectingRoleCount(van, type);
            return "'" + step + "' leest als het objecttype '" + type.naam + "', en " + (n == 0
                    ? "geen relatie verbindt '" + van.naam + "' met '" + type.naam + "'"
                    : n + " relaties verbinden '" + van.naam + "' met '" + type.naam + "'");
        }
        for (Feittype ft : model.feittypen.values())
            for (Rol r : ft.rollen)
                if (r.naam().equalsIgnoreCase(step) || r.plural() != null && r.plural().equalsIgnoreCase(step))
                    return "de rol '" + step + "' wijst vanaf '" + van.naam + "' niet precies één relatie aan";
        return null;
    }

    /** The declared filler of the role a binding reaches, or null where the binding names none. */
    private Objecttype roleFiller(RegelSpraakModel.RoleBindingRef step) {
        Feittype ft = model.feittypen.get(step.feittype());
        if (ft == null) return null;
        for (Rol r : ft.rollen) if (r.naam().equals(step.rol())) return model.objecttypeOpNaam(r.objecttypeNaam());
        return null;
    }

    /** Whether the read names a strict subtype of the filler its last role reaches: by the type's name ("de toeslag van
     *  de Strandjutter van de Vondst" over a role a Jutter fills), which only the instances of that subtype satisfy. */
    private static boolean downward(Objecttype fillerType, Objecttype endpoint) {
        return fillerType != null && endpoint != fillerType && endpoint.isEen(fillerType);
    }

    private static String downwardError(Expr.AttribuutRef a, Objecttype endpoint, Objecttype fillerType, String rol) {
        return "genavigeerde lezing '" + a.bron() + "' noemt het subtype '" + endpoint.naam + "', maar de rol '" + rol
                + "' komt uit op '" + fillerType.naam + "': the navigation reaches '" + fillerType.naam + "', not the endpoint '"
                + endpoint.naam + "'";
    }

    /** A read whose subject phrase names no role and no type from the subject: the emitter writes it without a type. */
    private static String withoutTypeError(Expr.AttribuutRef a, Objecttype subject, String naam) {
        String onderwerp = a.subjectSource() == null || a.subjectSource().isBlank() ? "haar onderwerp"
                : "'" + a.subjectSource() + "'";
        return "lezing '" + a.bron() + "': " + onderwerp + " noemt vanaf '" + subject.naam + "' geen rol en geen "
                + "objecttype: attribute reference '" + naam + "' without a subjectType: it resolves against no "
                + "declaration";
    }

    /** A member read on {@code fillerType} that neither it nor a supertype declares: where it does stand, and the
     *  verifier's sentence for the navigation to the one subtype that declares it (WF-053), else for the read on the
     *  filler (WF-034). */
    private String memberMissingError(Expr.AttribuutRef a, String member, Objecttype fillerType, String rol) {
        List<String> subTypes = new ArrayList<>(), otherTypes = new ArrayList<>();
        for (Objecttype o : model.objecttypen.values())
            if (o != fillerType && declaresMember(o, member)) (o.isEen(fillerType) ? subTypes : otherTypes).add(o.naam);
        String where = !subTypes.isEmpty()
                ? (subTypes.size() == 1 ? ", wel op het subtype " : ", wel op de subtypen ") + enumeration(subTypes)
                : otherTypes.size() == 1 ? ", wel op " + enumeration(otherTypes) + ", dat geen subtype van '" + fillerType.naam + "' is"
                : !otherTypes.isEmpty() ? ", wel op " + enumeration(otherTypes) + ", die geen subtype van '" + fillerType.naam + "' zijn"
                : ", en op geen ander objecttype";
        String sentence = subTypes.size() == 1
                ? "the navigation reaches '" + fillerType.naam + "', not the endpoint '" + subTypes.get(0) + "'"
                : "'" + member + "' is neither an attribute nor a characteristic of '" + fillerType.naam + "'";
        return "genavigeerde lezing '" + a.bron() + "': het lid '" + member + "' bestaat niet op '" + fillerType.naam
                + "', de vuller van de rol '" + rol + "'" + where + ": " + sentence;
    }

    private String characteristicAsValueError(Expr.AttribuutRef a, String member, Objecttype fillerType, String rol) {
        return "genavigeerde lezing '" + a.bron() + "': het lid '" + member + "' van '" + fillerType.naam
                + "', de vuller van de rol '" + rol + "', is een kenmerk en geen attribuut: '" + member
                + "' is a characteristic of '" + fillerType.naam + "', not an attribute — a characteristic endpoint is "
                + "ill-formed in a value position";
    }

    /** Whether {@code o} itself — not a supertype — declares a member {@code lid}, case-insensitively. */
    private static boolean declaresMember(Objecttype o, String member) {
        return o.attributen.keySet().stream().anyMatch(n -> n.equalsIgnoreCase(member))
                || o.kenmerken.stream().anyMatch(n -> n.equalsIgnoreCase(member));
    }

    /** The refusal of a fold the wire has no spelling for: what the fold carries, then the reader's sentence for the
     *  document the emitter would write — the verifier's WF-053 sentence for a step written on the source, else the
     *  one for the fold without a range ({@link #withoutRange}). */
    private String foldError(Expr.FunctionCall f, Objecttype subject, dev.uchar.regelspraak.emit.IrEmitter.FoldRefusal w) {
        Expr.AttribuutRef bron = (Expr.AttribuutRef) f.arguments().get(0);
        String fold = "vouw '" + f.bron() + "' ";
        return switch (w) {
            case WRITTEN_PATH, WRITTEN_STEP -> fold + "met een geschreven stap op haar bron: a path on a fold "
                    + "source is ill-formed (the fold's relation is its range, sourceRole or sourcePath)";
            case FOREIGN_ATTRIBUTE -> fold + "over '" + bron.attribuut() + "' op " + bron.objecttype().naam
                    + ", een ander type dan het onderwerp" + withoutRange(f, subject, bron);
            case OWN_ATTRIBUTE -> fold + "over een eigen attribuut van het onderwerp, dat geen instanties telt"
                    + withoutRange(f, subject, bron);
            case NO_SUBJECT -> fold + "zonder onderwerp om haar bereik vanaf te bepalen" + withoutRange(f, subject, bron);
            case NO_RELATION -> fold + "zonder bereik (" + (writtenAnchor(bron, subject) != null
                    ? "de geschreven bron '" + bron.subjectSource() + "' loopt via '" + writtenAnchor(bron, subject) + "', een "
                            + "ander anker dan het onderwerp, en een bereik vanaf zo'n anker schrijft deze frontend niet"
                    : pairReason(subject, f.elementObjectType(),
                            bron.type() instanceof Datatype.Onbekend ? bron.attribuut() : bron.subjectSource())) + ")"
                    + withoutRange(f, subject, bron);
        };
    }

    /** The anchor a fold source writes behind its element noun where it is another object than the subject
     *  ("zijn thuishaven" in "alle liggers van zijn thuishaven"), or null where the source writes none or names the
     *  subject's own type ("alle Schuiten van de Haven" on a Haven). Such a range is two steps from the subject, and
     *  the derivation by the one relation from the subject to the element type says nothing about it. */
    private static String writtenAnchor(Expr.AttribuutRef bron, Objecttype subject) {
        String o = bron.subjectSource() == null ? "" : bron.subjectSource().trim();
        int van = o.toLowerCase(java.util.Locale.ROOT).indexOf(" van ");
        if (van < 0) return null;
        String anchor = o.substring(van + 5).trim();
        return subject != null && stripArticle(anchor).equalsIgnoreCase(subject.naam) ? null : anchor;
    }

    /** The reader's sentence for a fold written without a range: the loader's under every kind but total; under a
     *  total WF-083's, for an element type or a source type that is not the subject's. A total over the subject's
     *  own attribute without a range is the subject's own total, which no reader refuses, and the refusal keeps
     *  this frontend's words. */
    private static String withoutRange(Expr.FunctionCall f, Objecttype subject, Expr.AttribuutRef bron) {
        if (f.kind() != Expr.FunctionKind.TOTAAL)
            return ": a single-argument fold with an elementType carries its range (sourceRole or sourcePath) "
                    + "under every kind but total: the range is written, never derived";
        if (subject != null && f.elementObjectType() != subject)
            return ": the elementType '" + f.elementObjectType().naam + "' of a total without a range is not "
                    + "the fold subject '" + subject.naam + "': without a range the fold is the subject's own read, "
                    + "so its elementType must name the subject type";
        if (subject != null && bron.objecttype() != null && bron.objecttype() != subject)
            return ": fold-source subjectType '" + bron.objecttype().naam + "' is not the fold's anchor '"
                    + subject.naam + "' (" + "a fold without a range reads the subject's own attribute"
                    + "): the executors resolve '" + bron.attribuut() + "' against the anchor, never against this "
                    + "field, so the two spellings name different declarations";
        return ": zonder bereik zou de wire haar lezen als het totaal van een eigen attribuut van het onderwerp";
    }

    /** Why the type pair gave no relation: none joins the two types, the one that does is reciprocal (the
     *  two-role binding does not take it), or several do and the written name names none of their roles alone. */
    private String pairReason(Objecttype subject, Objecttype doel, String written) {
        int n = model.connectingRoleCount(subject, doel);
        if (n == 0) return "geen relatie verbindt " + subject.naam + " met " + doel.naam;
        if (n == 1) return "de enige relatie tussen " + subject.naam + " en " + doel.naam + " is wederkerig";
        return n + " relaties verbinden " + subject.naam + " met " + doel.naam + ", en '" + written
                + "' noemt geen rol die er precies één aanwijst";
    }

    private void windowError(Conditie.Kwantor k, int threshold, Bron bron, String place) {
        String words = switch (k) {
            case GEEN -> "geen van de";
            case TEN_MINSTE -> "ten minste " + threshold;
            case TEN_HOOGSTE -> "ten hoogste " + threshold;
            case PRECIES -> "precies " + threshold;
            default -> k.name().toLowerCase();
        };
        model.diagnostics.add(new Diagnostic(Severity.ERROR,
                "periodevergelijking onder '" + words + "' (" + place + "): a time window under none or a counting "
                + "quantifier: the window gates the RULE, and no reading is specified for a verdict that is counted "
                + "or complemented", bron == null ? 0 : bron.line()));
    }

    /** The head a refusal names an extension block by, in the spelling the other §4 refusals use. */
    private static String blockHead(ParserRuleContext block) {
        if (block instanceof UitzonderingRegelContext u)
            return (u.AFWIJKING_VAN() != null ? "Afwijking van '" : "Uitzondering op '") + src(u.uitzonderingBasisNaam()) + "'";
        if (block instanceof UitzonderingGroepContext g)
            return g.OP() != null ? "Uitzonderingen ongeordend op '" + src(g.uitzonderingBasisNaam()) + "'"
                    : "Uitzonderingen ongeordend";
        return block.getStart().getText();
    }

    // ----------------------------------------------------------------------------------------------------------
    // Form 2 (maintainer decisions 4-6 of 2026-09-23; grammar/DIVERGENCES.md §4). The blocks form a TREE: every
    // 'Uitzondering op' and 'Afwijking van' block, and every member of an 'Uitzonderingen ongeordend op' block,
    // names one carrier; a carrier that is no block is the base rule its tree hangs under, and each tree becomes one
    // defaults group. Every block is resolved before anything is lifted: removing the lifted rules family by family,
    // as the first card did, turned the lookup of a carrier that is itself an exception into "not found".
    // ----------------------------------------------------------------------------------------------------------

    /** What a node of the defaults tree is: an 'Uitzondering op' block, an 'Afwijking van' block, or a member of an
     *  'Uitzonderingen ongeordend op' block. */
    private enum Block { EXCEPTION, DEVIATION, MEMBER }

    /** One node of the defaults tree. */
    private static final class Node {
        final Block soort;
        /** The block a refusal stands at: the node's own block, or the 'Uitzonderingen ongeordend op' block of a
         *  member. */
        final ParserRuleContext block;
        final String naam;
        final String carrier;
        final String head;
        final int line;
        /** The carrier when it is a node itself; null when the carrier is the base rule {@link #root}. */
        Node parent;
        /** The base rule the node's tree hangs under, once known. */
        String root;
        /** Refused by an ERROR of its own, or hanging under a refused node or a cycle: no tree lifts it. */
        boolean fout;
        ResolvedRegel regel;
        final List<Node> children = new ArrayList<>();

        Node(Block soort, ParserRuleContext block, String naam, String carrier, String head, int line) {
            this.soort = soort; this.block = block; this.naam = naam; this.carrier = carrier; this.head = head;
            this.line = line;
        }
    }

    /** Builds the defaults tree of every 'Uitzondering op', 'Afwijking van' and 'Uitzonderingen ongeordend op'
     *  block and lowers each tree to one {@link PrioritizedDefaults} group (the lowering of
     *  spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md §5.2): the children of the base are chain positions above
     *  it, the later-written one higher; the children of a node nest in that node's default as one ordered
     *  exception group; the members of an 'Uitzonderingen ongeordend op' block become one exception group each,
     *  unordered, under a node or as a group-default directly above the base. A child of the base written for a
     *  strict subtype of the base's subject, on the target the subtype inherits, is the subtype's own exception: per
     *  subtype one more group, on the subtype, whose group-default holds those children ({@link #subtypeGroup};
     *  RUNTIME-SEMANTICS ch. 7.10 composes it over the base's group). A refused node keeps its whole tree
     *  un-lifted, as a refused family always stayed; every refusal is one ERROR at the block that makes it. */
    private void treeDefaults(RegelSpraakDocumentContext doc, java.util.Set<ParserRuleContext> geweigerd) {
        List<Node> nodes = new ArrayList<>();
        java.util.Set<String> separateMembers = new java.util.HashSet<>();
        for (ParseTree kind : doc.children == null ? List.<ParseTree>of() : doc.children) {
            if (kind instanceof UitzonderingRegelContext u)
                nodes.add(new Node(u.AFWIJKING_VAN() != null ? Block.DEVIATION : Block.EXCEPTION, u,
                        src(u.regelNaam()), src(u.uitzonderingBasisNaam()), blockHead(u), u.getStart().getLine()));
            else if (kind instanceof UitzonderingGroepContext g && g.OP() != null) {
                if (!geweigerd.contains(g)) headAnnotation(g, blockHead(g), "dialect §4", g.getStart().getLine());
                for (RegelContext member : g.regel())
                    nodes.add(new Node(Block.MEMBER, g, src(member.regelNaam()), src(g.uitzonderingBasisNaam()), blockHead(g),
                            g.getStart().getLine()));
            } else if (kind instanceof UitzonderingGroepContext g)
                for (RegelContext member : g.regel()) separateMembers.add(src(member.regelNaam()));
        }
        if (nodes.isEmpty()) return;

        // 1. the carrier of each node: another node by name, else a base rule. A block refused at its head
        //    (geweigerd) says nothing more; a member block says its carrier once.
        Map<String, Node> perName = new java.util.HashMap<>();
        for (Node k : nodes) perName.putIfAbsent(k.naam, k);
        java.util.Set<ParserRuleContext> reported =
                java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (Node k : nodes) {
            k.parent = perName.get(k.carrier);
            if (k.parent != null) continue;
            String reason = separateMembers.contains(k.carrier) ? looseMemberAsCarrier(k)
                    : resolvedOf(k.carrier).isEmpty() ? unknownCarrier(k) : null;
            if (reason == null) { k.root = k.carrier; continue; }
            k.fout = true;
            if (!geweigerd.contains(k.block) && reported.add(k.block)) fout(k.line, reason);
        }

        // 2. the base rule of each node, found by walking the carriers upward; a walk that comes back to a node
        //    it passed is a cycle, which has no base rule and is refused once, with its path
        for (Node k : nodes) {
            if (k.fout || k.root != null) continue;
            List<Node> pad = new ArrayList<>();
            Node h = k;
            while (!h.fout && h.root == null && !pad.contains(h)) { pad.add(h); h = h.parent; }
            if (pad.contains(h)) {
                List<Node> cycle = pad.subList(pad.indexOf(h), pad.size());
                Node first = cycle.stream().min(java.util.Comparator.comparingInt(nodes::indexOf)).orElseThrow();
                if (cycle.stream().noneMatch(c -> geweigerd.contains(c.block))) {
                    List<String> names = new ArrayList<>();
                    Node c = first;
                    do { names.add("'" + c.naam + "'"); c = c.parent; } while (c != first);
                    names.add("'" + first.naam + "'");
                    fout(first.line, cycle(first, String.join(" -> ", names)));
                }
            }
            for (Node p : pad) {
                if (h.fout || h.root == null) p.fout = true;
                else p.root = h.root;
            }
        }

        // 3. per tree, in the order its first block was written: the base, then every node top-down
        java.util.Set<String> nodeNames = nodeNames(doc);
        Map<String, String[]> vormen = formSites(doc);
        Map<String, List<Node>> perRoot = new LinkedHashMap<>();
        for (Node k : nodes) if (!k.fout) perRoot.computeIfAbsent(k.root, w -> new ArrayList<>()).add(k);
        java.util.Set<ResolvedRegel> gelift = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (var e : perRoot.entrySet()) {
            String root = e.getKey();
            List<Node> tree = e.getValue();
            if (tree.stream().anyMatch(k -> geweigerd.contains(k.block))) continue;   // refused at a head already
            List<Node> rootChildren = new ArrayList<>();
            for (Node k : tree) (k.parent == null ? rootChildren : k.parent.children).add(k);
            Node head = rootChildren.get(0);
            List<ResolvedRegel> basis = resolvedOf(root);
            if (basis.size() > 1) { multiVersionError("basisregel", root, head.line); continue; }
            ResolvedRegel baseRule = basis.get(0);
            if (refusedTargets.contains(baseRule)) continue;   // its target is refused already, and by name
            DefaultsTarget doel = targetOf(baseRule);
            if (doel == null || baseRule.soort() == ResolvedRegel.Soort.INITIALISATIE) {
                fout(head.line, head.head + ": de basisregel is geen kenmerktoekenning of gelijkstelling (bv. een "
                        + "initialisatie) — daarover wordt geen defaults-groep gebouwd (§4)");
                continue;
            }
            if (!checkChildren(null, root, rootChildren, baseRule, doel, nodeNames)) continue;

            // the blocks for the base's own type, and per subtype the blocks written for it, in written order
            List<Node> ownChildren = new ArrayList<>();
            Map<Objecttype, List<Node>> perSubtype = new LinkedHashMap<>();
            for (Node k : rootChildren) {
                Objecttype o = targetOf(k.regel).onderwerp();
                if (o == doel.onderwerp()) ownChildren.add(k);
                else perSubtype.computeIfAbsent(o, x -> new ArrayList<>()).add(k);
            }
            if (!perSubtype.isEmpty()) {
                Refusal reason = orderError(root, rootChildren);
                if (reason == null) reason = subtypeNameError(root, perSubtype, vormen);
                if (reason != null) { fout(reason.line(), reason.tekst()); continue; }
            }

            List<Conditie> baseConjuncts = conjuncts(baseRule.voorwaarde());
            List<Default> keten = new ArrayList<>();
            if (!ownChildren.isEmpty() && ownChildren.get(0).soort == Block.MEMBER)   // one unordered block: a group-default directly above the base
                keten.add(new Default(null, null, null, false,
                        exceptionsOf(ownChildren, baseConjuncts, doel.kenmerk()),
                        headText(ownChildren.get(0).block)));
            else
                for (Node k : ownChildren) keten.add(0, defaultOf(k, baseConjuncts, doel.kenmerk()));
            keten.add(defaultOf(baseRule, doel.kenmerk()));
            PrioritizedDefaults group = defaultsOf(root, doel, false, keten, baseRule.geldigheid(),
                    annotationText(baseRule.statuteAnnotations()));
            model.prioritizedDefaults.add(group);
            pdOrigin.put(group, new Origin(head.line, head.head));
            for (var s : perSubtype.entrySet()) {
                PrioritizedDefaults own = subtypeGroup(root, baseRule, doel, s.getKey(), s.getValue());
                model.prioritizedDefaults.add(own);
                subtypeGroups.add(new String[]{own.naam(), root, s.getKey().naam});
                pdOrigin.put(own, new Origin(s.getValue().get(0).line, s.getValue().get(0).head));
            }
            gelift.add(baseRule);
            for (Node k : tree) gelift.add(k.regel);
        }
        model.resolvedRegels.removeIf(gelift::contains);
    }

    /** Checks the children of one carrier ({@code drager} null: the base rule), top-down: which kinds may stand
     *  together, then each child's own body and its edge, then its subtree. Whether the subtree is sound; every
     *  refusal speaks once, and below a refused node nothing is checked any more. The members of one
     *  'Uitzonderingen ongeordend op' block share its header, so a block speaks once too: after one member is
     *  refused, the block's other members are not judged. */
    private boolean checkChildren(Node carrier, String carrierName, List<Node> children, ResolvedRegel basis,
                                 DefaultsTarget doel, java.util.Set<String> nodeNames) {
        boolean goed = true;
        // a subtype's own blocks under the base stand above the supertype's in the composition of ch. 7.10, so
        // only children for the carrier's own type can leave their priority open
        List<Node> own = children.stream().filter(k -> subjectOf(k) == null
                || subjectOf(k) == doel.onderwerp()).toList();
        List<Node> ordered = own.stream().filter(k -> k.soort != Block.MEMBER).toList();
        List<ParserRuleContext> blocks = own.stream().filter(k -> k.soort == Block.MEMBER).map(k -> k.block)
                .distinct().toList();
        java.util.Set<ParserRuleContext> refusedBlocks =
                java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        if (!blocks.isEmpty() && (!ordered.isEmpty() || blocks.size() > 1)) {
            ParserRuleContext block = ordered.isEmpty() ? blocks.get(1) : blocks.get(0);
            refusedBlocks.add(block);
            fout(block.getStart().getLine(), blockHead(block) + ": de drager '" + carrierName + "' heeft ook "
                    + (ordered.isEmpty() ? "een ander 'Uitzonderingen ongeordend op'-blok"
                            : "geordende uitzonderingen (" + enumeration(ordered.stream().map(k -> k.naam).toList()) + ")")
                    + " — de kinderen van één drager vormen óf één geordende keten ('Uitzondering op', 'Afwijking van') "
                    + "óf de leden van één ongeordend blok, want de voorrang daartussen ligt nergens vast. Schrijf ze "
                    + "allemaal geordend, of neem ze allemaal als lid op in één blok 'Uitzonderingen ongeordend op "
                    + carrierName + ":' (dialect §4)");
            goed = false;
        }
        for (Node k : children) {
            if (k.soort == Block.MEMBER && refusedBlocks.contains(k.block)) { goed = false; continue; }
            if (!checkNode(k, carrier, carrierName, basis, doel, nodeNames)) {
                if (k.soort == Block.MEMBER) refusedBlocks.add(k.block);
                goed = false;
                continue;
            }
            // below a subtype's own block the carrier's target is the subtype's
            if (!checkChildren(k, k.naam, k.children, basis, targetOf(k.regel), nodeNames)) goed = false;
        }
        return goed;
    }

    /** A refusal at a block: its line and its sentence. */
    private record Refusal(int line, String tekst) {}

    /** Whether the written order of the base's children says what the composition does. Among the children of one
     *  carrier the later-written block ranks higher; between a block for a type and one for a strict subtype of it,
     *  RUNTIME-SEMANTICS ch. 7.10 decides instead: the subtype's own exceptions stand above every exception it
     *  inherits. A block for the subtype written before one for its supertype would read as ranked below it, and on
     *  the wire it ranks above, so it is refused, at the subtype's block. */
    private Refusal orderError(String root, List<Node> children) {
        for (int i = 0; i < children.size(); i++)
            for (int j = i + 1; j < children.size(); j++) {
                Node k = children.get(i), later = children.get(j);
                Objecttype sk = targetOf(k.regel).onderwerp(), sl = targetOf(later.regel).onderwerp();
                if (sk == sl || !sk.isEen(sl)) continue;
                return new Refusal(k.line, k.head + ": " + aanduiding(k) + " voor " + sk.naam + " staat vóór "
                        + aanduiding(later) + " voor " + sl.naam + " onder '" + root + "', en het later geschreven "
                        + "blok gaat voor; RUNTIME-SEMANTICS ch. 7.10 zet de eigen uitzonderingen van " + sk.naam
                        + " echter boven de geërfde van " + sl.naam + ", zodat bij een " + sk.naam + " '" + k.naam
                        + "' vóór '" + later.naam + "' gaat. Schrijf '" + k.naam + "' na '" + later.naam
                        + "' (dialect §4)");
            }
        return null;
    }

    /** The name of the group that carries a subtype's own blocks under {@code root}. */
    private static String subtypeGroupName(String root, Objecttype sub) {
        return root + " voor " + sub.naam;
    }

    /** A generated group name another form already carries (WF-020: a defaults group and a rule share one namespace,
     *  case aside). Null when every name is free. */
    private Refusal subtypeNameError(String root, Map<Objecttype, List<Node>> perSubtype,
                                      Map<String, String[]> vormen) {
        for (var s : perSubtype.entrySet()) {
            String naam = subtypeGroupName(root, s.getKey());
            String[] site = vormen.get(dev.uchar.regelspraak.emit.IrEmitter.norm(naam));
            if (site == null) continue;
            Node k = s.getValue().get(0);
            return new Refusal(k.line, k.head + ": de eigen uitzonderingen van " + s.getKey().naam + " onder '"
                    + root + "' worden de defaults-groep '" + naam + "', en " + site[0] + " heet al zo (bronregel "
                    + site[1] + ") — een vormnaam is uniek in het hele model, hoofdletters niet meegeteld (WF-020) "
                    + "(dialect §4)");
        }
        return null;
    }

    /** The group of a subtype's own blocks under the base of its supertype (RUNTIME-SEMANTICS ch. 7.9 and 7.10): on
     *  the subtype, one group-default whose one ordered exception group holds the blocks, the later-written one
     *  first. A group-default is no base, so the composition keeps the inherited base below the subtype's own
     *  exceptions and the inherited ones; a block as an ordinary default would be the subtype's own base and replace
     *  the inherited one. A restrictive block conjoins the base's condition, read on the subtype. The group carries
     *  the base's window, as the blocks do, and its source says where the base comes from. */
    private PrioritizedDefaults subtypeGroup(String root, ResolvedRegel basis, DefaultsTarget doel, Objecttype sub,
                                             List<Node> children) {
        List<Conditie> conjuncts = conjuncts(opSubtype(basis.voorwaarde(), doel.onderwerp(), sub));
        List<Default> keten = new ArrayList<>();
        for (Node k : children) keten.add(0, defaultOf(k, conjuncts, doel.kenmerk()));   // later-written stacks higher
        List<String> heads = children.stream().map(k -> headText(k.block)).distinct().toList();
        String head = String.join(" en ", heads);
        Default groupDefault = new Default(null, null, null, false,
                new Default.Exceptions(false, List.of(new ExceptionGroup(keten, head))), head);
        return defaultsOf(subtypeGroupName(root, sub), new DefaultsTarget(sub, doel.kenmerk(), doel.naam()), false,
                List.of(groupDefault), basis.geldigheid(), "eigen uitzonderingen van " + sub.naam + "; de basis '"
                        + root + "' is geërfd van " + doel.onderwerp().naam);
    }

    /** {@code c} read on the subtype {@code sub} instead of on {@code sup}: every own read of {@code sup} names
     *  {@code sub}, as the same text written on {@code sub} resolves (IR-SPEC ch. 4.1a: an own read names the subject
     *  type, also for an inherited attribute). A restrictive block of a subtype repeats the conjuncts of its base, and
     *  the base stands on the supertype. Navigated reads, role checks and characteristic checks name no subject type,
     *  and the emitter derives their walk from the group's subject; what a fold or a quantifier reads per element is
     *  the element's. A node in which nothing changes is returned itself, so a conjunct that reads no own attribute
     *  stays the node the base carries. */
    private static Conditie opSubtype(Conditie c, Objecttype sup, Objecttype sub) {
        if (c == null) return null;
        return switch (c) {
            case Conditie.Vergelijking v -> {
                Expr l = opSubtype(v.links(), sup, sub), r = opSubtype(v.rechts(), sup, sub);
                yield l == v.links() && r == v.rechts() ? v
                        : new Conditie.Vergelijking(v.op(), l, r, v.periode(), v.bron());
            }
            case Conditie.Predicaat p -> {
                Expr o = opSubtype(p.operand(), sup, sub);
                yield o == p.operand() ? p
                        : new Conditie.Predicaat(p.soort(), o, p.dayKind(), p.negatie(), p.digits(), p.periode(), p.bron());
            }
            case Conditie.Periode p -> {
                Expr van = opSubtype(p.vanaf(), sup, sub), tot = opSubtype(p.tot(), sup, sub);
                yield van == p.vanaf() && tot == p.tot() ? p : new Conditie.Periode(van, tot, p.toInclusive(), p.bron());
            }
            case Conditie.Samengesteld s -> {
                List<Conditie> parts = s.onderdelen().stream().map(o -> opSubtype(o, sup, sub)).toList();
                boolean same = true;
                for (int i = 0; i < parts.size(); i++) same &= parts.get(i) == s.onderdelen().get(i);
                yield same ? s : new Conditie.Samengesteld(s.kwantor(), s.threshold(), parts, s.bron());
            }
            case Conditie.Kenmerk k -> k.eigenaar() != sup ? k
                    : new Conditie.Kenmerk(k.kenmerk(), k.navigatie(), k.negatie(), sub, k.periode(), k.bron());
            // the filter and the test read each element
            case Conditie.QuantifierFold k -> k;
            case Conditie.Rollidmaatschap r -> r;
            case Conditie.FarRoleMembership r -> r;
            case Conditie.RuleStatusCheck r -> r;
            case Conditie.Unresolved o -> o;
        };
    }

    private static Expr opSubtype(Expr e, Objecttype sup, Objecttype sub) {
        if (e == null) return null;
        return switch (e) {
            case Expr.AttribuutRef a -> !ownReadOf(a, sup) ? a
                    : new Expr.AttribuutRef(a.attribuut(), sub, a.subjectSource(), a.navigatieRol(), a.type(), a.bron(),
                            a.set(), a.navigationPath(), a.rangePath(), a.kwantor(), a.threshold());
            case Expr.Binair b -> {
                Expr l = opSubtype(b.links(), sup, sub), r = opSubtype(b.rechts(), sup, sub);
                yield l == b.links() && r == b.rechts() ? b : new Expr.Binair(b.op(), l, r, b.type(), b.bron());
            }
            case Expr.FunctionCall f -> {
                // a fold's source is read through its range: only a source that is the subject's own read (a
                // dimension fold, the subject's own total) is the subject's; the filter of a dimension fold reads the
                // subject's labels, every other filter reads each element
                boolean fold = dev.uchar.regelspraak.emit.IrEmitter.isFoldSource(f);
                List<Expr> args = new ArrayList<>();
                for (int i = 0; i < f.arguments().size(); i++) {
                    Expr x = f.arguments().get(i);
                    args.add(fold && i == 0 && !(x instanceof Expr.AttribuutRef a && ownReadOf(a, sup)) ? x
                            : opSubtype(x, sup, sub));
                }
                Expr empty = opSubtype(f.emptyFallback(), sup, sub);
                Conditie filter = f.dimensieSelectie() != null ? opSubtype(f.filter(), sup, sub) : f.filter();
                boolean ownSource = fold && !args.isEmpty() && args.get(0) != f.arguments().get(0);
                Objecttype element = f.elementObjectType() == sup && (f.dimensieSelectie() != null || ownSource) ? sub
                        : f.elementObjectType();
                boolean same = empty == f.emptyFallback() && filter == f.filter() && element == f.elementObjectType();
                for (int i = 0; i < args.size(); i++) same &= args.get(i) == f.arguments().get(i);
                yield same ? f : new Expr.FunctionCall(f.kind(), args, element, empty, filter, f.dimensieSelectie(),
                        f.type(), f.bron(), f.granulariteit());
            }
            case Expr.Afgerond a -> {
                Expr b = opSubtype(a.basis(), sup, sub);
                yield b == a.basis() ? a : new Expr.Afgerond(b, a.modus(), a.decimalen(), a.bron());
            }
            case Expr.Begrensd b -> {
                Expr x = opSubtype(b.basis(), sup, sub), min = opSubtype(b.minimum(), sup, sub),
                        max = opSubtype(b.maximum(), sup, sub);
                yield x == b.basis() && min == b.minimum() && max == b.maximum() ? b
                        : new Expr.Begrensd(x, min, max, b.bron());
            }
            case Expr.TextComposition t -> {
                List<Expr.TextComposition.Part> parts = new ArrayList<>();
                boolean same = true;
                for (Expr.TextComposition.Part d : t.parts()) {
                    Expr.TextComposition.Part n = d;
                    if (d instanceof Expr.TextComposition.EmbeddedExpression i) {
                        Expr x = opSubtype(i.expr(), sup, sub);
                        if (x != i.expr()) n = new Expr.TextComposition.EmbeddedExpression(x);
                    }
                    same &= n == d;
                    parts.add(n);
                }
                yield same ? t : new Expr.TextComposition(parts, t.bron());
            }
            case Expr.DimensionCellRead c -> {
                Expr a = opSubtype(c.attribuut(), sup, sub);
                yield a == c.attribuut() ? c : new Expr.DimensionCellRead((Expr.AttribuutRef) a, c.labels(), c.bron());
            }
            case Expr.Let l -> {
                Expr v = opSubtype(l.value(), sup, sub), b = opSubtype(l.body(), sup, sub);
                yield v == l.value() && b == l.body() ? l : new Expr.Let(l.binder(), v, b, l.type(), l.bron());
            }
            case Expr.IfThenElse k -> {
                Conditie c = opSubtype(k.condition(), sup, sub);
                Expr then = opSubtype(k.then(), sup, sub), otherwise = opSubtype(k.otherwise(), sup, sub);
                yield c == k.condition() && then == k.then() && otherwise == k.otherwise() ? k
                        : new Expr.IfThenElse(c, then, otherwise, k.type(), k.bron());
            }
            // each selection reads its elements
            case Expr.InstanceSetUnion u -> u;
            case Expr.Literal l -> l;
            case Expr.ParameterRef p -> p;
            case Expr.VariableRef v -> v;
            case Expr.BinderRef b -> b;
            case Expr.Constante k -> k;
            case Expr.Unresolved o -> o;
        };
    }

    /** Whether {@code a} is an own read of {@code type}: no written role or chain, no set, and that type. */
    private static boolean ownReadOf(Expr.AttribuutRef a, Objecttype type) {
        return a.objecttype() == type && a.navigatieRol() == null && !a.set()
                && (a.navigationPath() == null || a.navigationPath().isEmpty());
    }

    /** The subject of the one rule a node's body resolves to, or null where it resolves to none or to several (the
     *  node's own check refuses those). */
    private Objecttype subjectOf(Node k) {
        List<ResolvedRegel> r = resolvedOf(k.naam);
        DefaultsTarget d = r.size() == 1 ? targetOf(r.get(0)) : null;
        return d == null ? null : d.onderwerp();
    }

    /** One node's own body and its edge to its carrier: the body resolves to one plain version; an 'Afwijking van'
     *  hangs directly under the base; a subtype does not write under an inherited node; a block for a subtype
     *  directly under the base is the subtype's own exception ({@link #subtypeBlockError}), and no member of an
     *  unordered block stands among a subtype's own exceptions; the window is the group's; the target is the
     *  group's (WF-050) or, directly under the base, the one a subtype inherits. The checks and their texts at the
     *  first level are the first card's. {@code doel} is the carrier's target: below a subtype's own block, the
     *  subtype's. {@code nodeNames} are the rules written as nodes ({@link #nodeNames}): a rule outside them on the
     *  subtype's target is a base the subtype's own exception can hang on. */
    private boolean checkNode(Node k, Node carrier, String carrierName, ResolvedRegel basis, DefaultsTarget doel,
                              java.util.Set<String> nodeNames) {
        List<ResolvedRegel> r = resolvedOf(k.naam);
        if (r.isEmpty()) {
            fout(k.line, k.head + ": " + withArticle(k) + " leverde geen kenmerktoekenning/gelijkstelling op (§4)");
            return false;
        }
        if (r.size() > 1) { multiVersionError(what(k), k.naam, k.line); return false; }
        ResolvedRegel regel = r.get(0);
        if (refusedTargets.contains(regel)) return false;   // refused already, by name
        DefaultsTarget own = targetOf(regel);
        if (own == null || regel.soort() == ResolvedRegel.Soort.INITIALISATIE) {
            fout(k.line, k.head + ": " + withArticle(k) + " is geen kenmerktoekenning of gelijkstelling (bv. een "
                    + "initialisatie) (§4)");
            return false;
        }
        if (k.soort == Block.DEVIATION && carrier != null) {
            fout(k.line, k.head + ": '" + carrier.naam + "' is " + kindText(carrier) + ", geen basisregel — een "
                    + "afwijking erft geen voorwaarde en staat daarom alleen direct boven een basisregel; midden in de "
                    + "boom zou zij beslissen waar de voorwaarde van haar drager niet geldt. Schrijf 'Uitzondering op "
                    + carrier.naam + ": " + k.naam + "' als zij de voorwaarde van '" + carrier.naam + "' erft, of "
                    + "'Afwijking van " + k.root + ": " + k.naam + "' met de volledige voorwaarde (dialect §4)");
            return false;
        }
        boolean subtype = own.kenmerk() == doel.kenmerk() && own.naam().equalsIgnoreCase(doel.naam())
                && own.onderwerp() != doel.onderwerp() && own.onderwerp().isEen(doel.onderwerp());
        Objecttype rootType = targetOf(basis).onderwerp();
        if (subtype && carrier != null) {
            String sub = own.onderwerp().naam, sup = doel.onderwerp().naam;
            // The subtype's own exception hangs under the base it inherits, which is this tree's unless a type on
            // its line declares one of its own (ch. 7.10: an own base replaces the inherited one).
            Base b = ownBaseOnLijn(own, rootType, nodeNames);
            String naam = b == null ? k.root : b.regel();
            String under = b == null ? "de basisregel '" + k.root + "'"
                    : b.type() == own.onderwerp() ? "zijn eigen basisregel '" + b.regel() + "'"
                    : "de basisregel '" + b.regel() + "' van " + b.type().naam + ", die " + sub + " erft";
            fout(k.line, k.head + ": '" + carrier.naam + "' is " + kindText(carrier) + " van " + sup + ", en "
                    + aanduiding(k) + " schrijft voor het subtype " + sub + " — een subtype erft de uitzonderingen van "
                    + "zijn supertype met wat eronder genest is, en adresseert er geen (RUNTIME-SEMANTICS ch. 7.10). "
                    + "Schrijf haar als eigen uitzondering van " + sub + " onder " + under + ": 'Afwijking van " + naam
                    + ": " + k.naam + "', met de volledige voorwaarde van '" + carrier.naam + "' en de eigen voorwaarde: "
                    + "de eigen uitzonderingen van " + sub + " gaan vóór de geërfde, dus zij beslist precies waar beide "
                    + "gelden (dialect §4)");
            return false;
        }
        if (subtype) {
            String reason = subtypeBlockError(k, own, rootType, nodeNames);
            if (reason != null) { fout(k.line, reason); return false; }
        } else if (k.soort == Block.MEMBER && own.onderwerp() != rootType && own.equal(doel)) {
            // a member under a subtype's own block: the subtype's group would carry an unordered block
            fout(k.line, subtypeMemberError(k, own.onderwerp(), rootType));
            return false;
        }
        // Default carries no per-default geldigheid: mixed windows are unrepresentable on the defaults group, so a
        // diverging window must refuse — the same fence the multi-version case gets.
        if (!regel.geldigheid().equals(basis.geldigheid())) {
            fout(k.line, k.head + ": " + aanduiding(k) + " draagt een ander geldigheidsvenster dan de basisregel — de "
                    + "defaults-groep draagt één venster; splits per venster of maak de vensters gelijk (§4)");
            return false;
        }
        if (!subtype && !doel.equal(own)) {
            fout(k.line, k.head + ": " + aanduiding(k) + " schrijft een ander doel (" + targetText(own) + ") dan "
                    + (carrier == null ? "de basisregel" : (k.soort == Block.MEMBER ? "zijn" : "haar") + " drager '"
                            + carrierName + "'")
                    + " (" + targetText(doel) + ") — een " + what(k) + " moet hetzelfde onderwerp en kenmerk/attribuut "
                    + "betreffen (§4, WF-050)");
            return false;
        }
        k.regel = regel;
        return true;
    }

    /** The base rule a type writes on a target of its own: the rule's name, without a version suffix, and the type. */
    private record Base(String regel, Objecttype type) {}

    /** The nearest base on the line from {@code eigen}'s subject up to {@code rootType}, that type excluded: a plain
     *  rule — no initialisation, no node of a tree, a tree's base included — that writes {@code eigen}'s target on a
     *  type of that line. Null where none does, so the subject inherits the base of the tree under
     *  {@code rootType} (RUNTIME-SEMANTICS ch. 7.10: an own base replaces the inherited one, and an ordinary rule is
     *  the base of its level). */
    private Base ownBaseOnLijn(DefaultsTarget own, Objecttype rootType, java.util.Set<String> nodeNames) {
        for (Objecttype t = own.onderwerp(); t != null && t != rootType; t = t.supertype) {
            DefaultsTarget opT = new DefaultsTarget(t, own.kenmerk(), own.naam());
            for (ResolvedRegel b : model.resolvedRegels)
                if (b.soort() != ResolvedRegel.Soort.INITIALISATIE && !refusedTargets.contains(b)
                        && !nodeNames.contains(withoutVersion(b.naam())) && opT.equal(targetOf(b)))
                    return new Base(withoutVersion(b.naam()), t);
        }
        return null;
    }

    /** Why a block for a subtype directly under the base of its supertype does not lower, or null where it does. It
     *  lowers as the subtype's own exception without an own base; where the subtype has a base on its line, that base
     *  replaces the tree's for it, and the block belongs under that one. A member of an unordered block does not
     *  lower ({@link #subtypeMemberError}). */
    private String subtypeBlockError(Node k, DefaultsTarget own, Objecttype rootType,
                                   java.util.Set<String> nodeNames) {
        String sub = own.onderwerp().naam;
        Base b = ownBaseOnLijn(own, rootType, nodeNames);
        if (b != null) {
            String has = b.type() == own.onderwerp()
                    ? sub + " heeft een eigen basisregel '" + b.regel() + "' voor '" + own.naam() + "'"
                    : sub + " erft van " + b.type().naam + " de basisregel '" + b.regel() + "' voor '" + own.naam()
                      + "'";
            String vorm = switch (k.soort) {
                case DEVIATION -> "'Afwijking van " + b.regel() + ": " + k.naam + "'";
                case EXCEPTION -> "'Uitzondering op " + b.regel() + ": " + k.naam + "' als zij de voorwaarde van '"
                        + b.regel() + "' erft, of 'Afwijking van " + b.regel() + ": " + k.naam
                        + "' met de volledige voorwaarde";
                case MEMBER -> "het blok als 'Uitzonderingen ongeordend op " + b.regel() + ":'";
            };
            return k.head + ": " + aanduiding(k) + " schrijft voor het subtype " + sub + ", en " + has + ", die '"
                    + k.root + "' voor " + sub + " vervangt (RUNTIME-SEMANTICS ch. 7.10) — een eigen uitzondering van "
                    + sub + " hangt onder die basisregel. Schrijf " + vorm + " (dialect §4)";
        }
        return k.soort == Block.MEMBER ? subtypeMemberError(k, own.onderwerp(), rootType) : null;
    }

    /** A member of an 'Uitzonderingen ongeordend op' block among the own exceptions of a subtype under the base of its
     *  supertype. Not lowered: the subtype's group would hold the block, and a conflict between its members would be
     *  keyed on the path of the composed stack (RUNTIME-SEMANTICS ch. 7.9 and 7.10). */
    private static String subtypeMemberError(Node k, Objecttype sub, Objecttype rootType) {
        return k.head + ": groepslid '" + k.naam + "' schrijft voor het subtype " + sub.naam + " onder de basisregel '"
                + k.root + "' van " + rootType.naam + " — een 'Uitzonderingen ongeordend op'-blok in de eigen "
                + "uitzonderingen van een subtype onder de basisregel van zijn supertype lowert deze frontend niet: een "
                + "conflict tussen de leden zou een sleutel van de samengestelde stapel krijgen (RUNTIME-SEMANTICS ch. 7.9 "
                + "en 7.10). Schrijf de leden als geordende blokken ('Uitzondering op' of 'Afwijking van'), of geef "
                + sub.naam + " een eigen basisregel en hang het blok daaronder (dialect §4)";
    }

    /** The default a node becomes. An 'Uitzondering op' block and a member of an 'Uitzonderingen ongeordend op'
     *  block are restrictive: the justification is one flat 'all' of the carrier's conjuncts followed by the node's
     *  own, since ch. 7.9 decides a default's exception groups before and regardless of its own justification, and
     *  ch. 7.1 asks the frontend for self-contained justifications. Where the carrier contributes no conjunct (an
     *  unconditional base) the node keeps the condition it was written with. An 'Afwijking van' block carries its
     *  own condition only. */
    private Default defaultOf(Node k, List<Conditie> carrierConjuncts, boolean kenmerk) {
        Conditie own = k.regel.voorwaarde();
        Conditie justification = k.soort == Block.DEVIATION || carrierConjuncts.isEmpty()
                ? (own != null ? own : ALTIJD)
                : conjunction(carrierConjuncts, own);
        return defaultOf(k.regel, kenmerk, justification,
                exceptionsOf(k.children, conjuncts(justification), kenmerk));
    }

    /** The exception groups of a carrier: one ordered group holding its ordered children, the later-written one
     *  first; or one group per member of its 'Uitzonderingen ongeordend op' block, unordered. Null without
     *  children. A head makes an exception group, not a rule, so the group's source is the head's text
     *  ({@link #headText}) and each block's annotation stands on the default the block becomes. */
    private Default.Exceptions exceptionsOf(List<Node> children, List<Conditie> carrierConjuncts, boolean kenmerk) {
        if (children.isEmpty()) return null;
        if (children.get(0).soort == Block.MEMBER) {
            List<ExceptionGroup> leden = new ArrayList<>();
            for (Node member : children)
                leden.add(new ExceptionGroup(List.of(defaultOf(member, carrierConjuncts, kenmerk)),
                        headText(member.block)));
            return new Default.Exceptions(true, leden);
        }
        List<Default> keten = new ArrayList<>();
        for (Node k : children) keten.add(0, defaultOf(k, carrierConjuncts, kenmerk));   // later-written stacks higher
        return new Default.Exceptions(false, List.of(new ExceptionGroup(keten, headText(children.get(0).block))));
    }

    /** The written head of a block up to its carrier's name — "Uitzondering op herroeping", "Uitzonderingen
     *  ongeordend op vrijstelling van een jutter" — as the source text of the exception group or group-default the
     *  head makes: an ordered group holds the 'Uitzondering op' blocks of one carrier, whose heads all read so; a
     *  member's group and a group-default come from their block's head. Each hand-written anchor that nests gives
     *  every exception group and group-default a source. Built from the head's tokens and the carrier's name as
     *  {@link DefinitionResolver#src} gives it, so a comment standing between them does not enter the text. */
    private static String headText(ParserRuleContext block) {
        if (block instanceof UitzonderingRegelContext u)
            return u.getStart().getText().replaceAll("\\s+", " ") + " " + src(u.uitzonderingBasisNaam());
        UitzonderingGroepContext g = (UitzonderingGroepContext) block;
        return g.UITZONDERINGEN().getText() + " " + g.ONGEORDEND().getText() + " " + g.OP().getText() + " "
                + src(g.uitzonderingBasisNaam());
    }

    /** One flat 'all' over the carrier's conjuncts and the node's own condition, its source the conjuncts' sources
     *  joined by " en " — the shape the hand-written anchors give a nested justification. A compound conjunct's
     *  source runs to the period that closes its bullet list; before " en " that period would stand in the middle
     *  of the sentence, so it is left out there, and the last conjunct, which ends the sentence, keeps its text. */
    private static Conditie conjunction(List<Conditie> carrierConjuncts, Conditie own) {
        List<Conditie> parts = new ArrayList<>(carrierConjuncts);
        flatten(own, parts);
        StringBuilder sentence = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            String part = parts.get(i).bron() == null ? "" : parts.get(i).bron().snippet();
            if (i > 0) sentence.append(" en ");
            sentence.append(i < parts.size() - 1 && part.endsWith(".") ? part.substring(0, part.length() - 1) : part);
        }
        String tekst = sentence.toString();
        Bron site = own == null ? null : own.bron();
        return new Conditie.Samengesteld(Conditie.Kwantor.ALLE, 0, List.copyOf(parts),
                site == null ? Bron.synthetic(tekst) : new Bron(site.file(), site.line(), site.column(), tekst));
    }

    /** The conjuncts of a justification: the parts of an 'all', flattened at any depth; another quantifier stays
     *  one conjunct, and no justification (or the empty 'all') has none. */
    private static List<Conditie> conjuncts(Conditie c) {
        List<Conditie> result = new ArrayList<>();
        flatten(c, result);
        return result;
    }

    private static void flatten(Conditie c, List<Conditie> result) {
        if (c == null) return;
        if (c instanceof Conditie.Samengesteld s && s.kwantor() == Conditie.Kwantor.ALLE) {
            for (Conditie d : s.onderdelen()) flatten(d, result);
            return;
        }
        result.add(c);
    }

    private static String what(Node k) {
        return switch (k.soort) { case EXCEPTION -> "uitzondering"; case DEVIATION -> "afwijking"; case MEMBER -> "groepslid"; };
    }

    private static String aanduiding(Node k) { return what(k) + " '" + k.naam + "'"; }

    /** The node as the first card's texts name it: "de uitzondering 'E'", "groepslid 'M'". */
    private static String withArticle(Node k) { return (k.soort == Block.MEMBER ? "" : "de ") + aanduiding(k); }

    private static String kindText(Node k) {
        return switch (k.soort) {
            case EXCEPTION -> "een uitzondering";
            case DEVIATION -> "een afwijking";
            case MEMBER -> "een lid van een 'Uitzonderingen ongeordend op'-blok";
        };
    }

    private String unknownCarrier(Node k) {
        if (k.soort == Block.DEVIATION)
            return k.head + ": geen basisregel met die naam gevonden" + baseReason(k.carrier) + " — de drager van een "
                    + "afwijking is een gewone platte regel (kenmerktoekenning of gelijkstelling) (dialect §4)";
        return k.head + ": geen basisregel met die naam gevonden, en ook geen uitzondering of afwijking"
                + baseReason(k.carrier) + " — noem als drager een gewone platte regel (kenmerktoekenning of "
                + "gelijkstelling), een 'Uitzondering op'- of 'Afwijking van'-blok, of een lid van een 'Uitzonderingen "
                + "ongeordend op'-blok (dialect §4)";
    }

    private static String looseMemberAsCarrier(Node k) {
        return k.head + ": '" + k.carrier + "' is een lid van een 'Uitzonderingen ongeordend:'-blok zonder drager — zo'n "
                + "lid is een eigen defaults-groep naast de andere leden en geen drager. Schrijf de leden onder een "
                + "basisregel ('Uitzonderingen ongeordend op <basisregel>:'): een lid van zo'n blok kan wel drager zijn "
                + "(dialect §4)";
    }

    private static String cycle(Node k, String pad) {
        return k.head + ": de dragers vormen een kring (" + pad + ") — een uitzondering hangt aan één drager, en de "
                + "keten van dragers eindigt bij de basisregel die de defaults-groep draagt; een kring heeft geen "
                + "basisregel. Laat één blok van de kring een gewone platte regel of een blok buiten de kring als drager "
                + "noemen (dialect §4)";
    }

    private void fout(int line, String message) {
        model.diagnostics.add(new Diagnostic(Severity.ERROR, message, line));
    }

    /** The unordered group without a carrier: each 'Uitzonderingen ongeordend:' block becomes SIBLING
     *  declared-unordered defaults groups (one per member) over ONE shared target — the §9.5 surface. Every sibling
     *  carries {@code ongeordend} (WF-050: two-or-more defaults groups on one target require it). All members leave
     *  resolvedRegels. A block with 'op &lt;drager&gt;' hangs in a defaults tree ({@link #treeDefaults}). */
    private void groupDefaults(RegelSpraakDocumentContext doc, java.util.Set<ParserRuleContext> geweigerd) {
        for (UitzonderingGroepContext g : DefinitionResolver.allExceptionGroups(doc)) {
            if (g.OP() != null) continue;           // a member of a defaults tree
            if (geweigerd.contains(g)) continue;   // refused at its head already
            int line = g.getStart().getLine();
            headAnnotation(g, "Uitzonderingen ongeordend", "dialect §4", line);
            List<ResolvedRegel> leden = new ArrayList<>();
            DefaultsTarget shared = null;
            boolean fout = false;
            for (RegelContext member : g.regel()) {
                String naam = DefinitionResolver.src(member.regelNaam());
                List<ResolvedRegel> rr = resolvedOf(naam);
                if (rr.isEmpty()) {
                    model.diagnostics.add(new Diagnostic(Severity.ERROR,
                            "Uitzonderingen ongeordend: groepslid '" + naam + "' leverde geen "
                            + "kenmerktoekenning/gelijkstelling op (§4)", line));
                    fout = true; break;
                }
                if (rr.size() > 1) { multiVersionError("groepslid", naam, line); fout = true; break; }
                if (refusedTargets.contains(rr.get(0))) { fout = true; break; }   // refused already, by name
                DefaultsTarget d = targetOf(rr.get(0));
                if (d == null || rr.get(0).soort() == ResolvedRegel.Soort.INITIALISATIE) {
                    model.diagnostics.add(new Diagnostic(Severity.ERROR,
                            "Uitzonderingen ongeordend: groepslid '" + naam + "' is geen kenmerktoekenning of "
                            + "gelijkstelling (§4)", line));
                    fout = true; break;
                }
                if (shared == null) shared = d;
                else if (!shared.equal(d)) {
                    model.diagnostics.add(new Diagnostic(Severity.ERROR,
                            "Uitzonderingen ongeordend: groepslid '" + naam + "' schrijft een ander doel ("
                            + targetText(d) + ") dan een eerder lid (" + targetText(shared) + ") — een ongeordende "
                            + "groep betreft één gedeeld doel (§4, §9.5)", line));
                    fout = true; break;
                }
                leden.add(rr.get(0));
            }
            if (fout || shared == null) continue;
            for (ResolvedRegel r : leden) {
                PrioritizedDefaults sister = defaultsOf(r.naam(), shared, true,
                        List.of(defaultOf(r, shared.kenmerk())), r.geldigheid(), annotationText(r.statuteAnnotations()));
                model.prioritizedDefaults.add(sister);
                pdOrigin.put(sister, new Origin(line, "Uitzonderingen ongeordend"));
                model.resolvedRegels.remove(r);
            }
        }
    }

    /** Assemble a {@link PrioritizedDefaults} group for target {@code doel}: kenmerk XOR attribuut per WF-050,
     *  the base/member's geldigheid faithfully, and as {@code bron} the statute annotation of the rule the group
     *  is named after — the base of an ordered family, the member itself for an unordered sibling. */
    private PrioritizedDefaults defaultsOf(String naam, DefaultsTarget doel, boolean ongeordend, List<Default> defaults,
                                            Geldigheid g, String bron) {
        return new PrioritizedDefaults(naam, doel.onderwerp().naam,
                doel.kenmerk() ? doel.naam() : null, doel.kenmerk() ? null : doel.naam(),
                ongeordend, defaults, bron, g);
    }

    /** One default from a resolved flat rule: on characteristic defaults a kenmerktoekenning decides {@code true},
     *  and a negated one (dialect §4, form 1: {@link #NEGATED}) decides {@code false} — which writes nothing, so
     *  §9.2's monotone writes and ch. 5.3 hold; on attribute defaults the default's value is the rule's resolved
     *  value expression. The justification is the rule's condition.
     *  The rule's {@code // Bron:} annotation becomes the default's {@code bron}: lifting takes the rule out of
     *  resolvedRegels, and its {@code statuteAnnotations} with it, so this is the one place the citation can
     *  survive — and since every lifted rule becomes a default HERE, a nested exception inherits the placement
     *  by construction. */
    private Default defaultOf(ResolvedRegel r, boolean kenmerk) {
        return defaultOf(r, kenmerk, r.voorwaarde() != null ? r.voorwaarde() : ALTIJD, null);
    }

    /** The same with a given justification and nested exception groups: the default a node of a defaults tree
     *  becomes. */
    private Default defaultOf(ResolvedRegel r, boolean kenmerk, Conditie justification, Default.Exceptions exceptions) {
        String bron = annotationText(r.statuteAnnotations());
        return kenmerk
                ? new Default(!isNegated(r), justification, null, false, exceptions, bron)
                : new Default(null, justification, r.waarde(), false, exceptions, bron);
    }

    /** The annotations of one block as the single string a {@code bron} field holds: the non-blank entries in
     *  source order joined with "; ", or null when there are none — so an unannotated block writes no
     *  {@code source}, as no defaults golden without annotations has one. A blank entry is dropped as the
     *  emitter drops it from {@code statuteAnnotations}: it cites nothing. */
    private static String annotationText(List<String> annotations) {
        List<String> filled = annotations.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
        return filled.isEmpty() ? null : String.join("; ", filled);
    }

    /** A {@code // Bron:} annotation written above a block HEADER — 'Uitzonderingen ongeordend:' or 'Regelgroep
     *  &lt;naam&gt;' — belongs to no rule: an annotation travels with the rule directly below it, and a header is
     *  none, so it reaches no default, no group and no rule. It used to vanish without a word; the WARNING says
     *  where the citation has to go. Not an ERROR: the document is sound, only the citation is lost. These two
     *  headers warn because they open rules, which is where an author expects the citation to reach; an
     *  annotation above a definition documents the declaration and has no wire slot to miss. The group's own
     *  {@code bron} is no place for it either: on a rule group that record's snippet is its name. */
    private void headAnnotation(ParserRuleContext head, String headName, String reference, int line) {
        String tekst = annotationText(statuteAnnotations(head));
        if (tekst == null) return;
        model.diagnostics.add(new Diagnostic(Severity.WARNING,
                headName + ": de // Bron:-annotatie '" + tekst + "' boven de kop hoort bij geen regel — "
                + "een annotatie hoort bij de regel direct eronder, en de kop is geen regel — en komt daarom niet op "
                + "de wire. Zet haar boven het lid of de leden waarvoor zij geldt (" + reference + ")", line));
    }

    /** The vacuously true justification. WF-050 requires EVERY non-initialisation default to carry one,
     *  so an UNCONDITIONAL base rule — a §13.4.2 rule with no {@code indien}, which is the ch. 7.8 standing
     *  norm — lowers to an ALLE-group over no parts: "all of nothing holds" is true, and it is exactly how
     *  the hand-written prioritized-defaults anchors spell "altijd". As the lowest default of a group such a
     *  rule stands whenever nothing above it decides, which is what the flat rule meant on its own. */
    private static final Conditie ALTIJD =
            new Conditie.Samengesteld(Conditie.Kwantor.ALLE, 0, List.of(), Bron.synthetic("altijd"));

    /** The resolved versions of the rule named {@code naam} — matched exactly or by the "&lt;naam&gt; #k" version
     *  suffix. Empty when no such rule resolved (it was a non-scalar construct, or the name is unknown). */
    private List<ResolvedRegel> resolvedOf(String naam) {
        List<ResolvedRegel> result = new ArrayList<>();
        for (ResolvedRegel r : model.resolvedRegels)
            if (r.naam().equals(naam) || r.naam().startsWith(naam + " #")) result.add(r);
        return result;
    }

    /** The (subject, kenmerk-vs-attribuut, target-name) a lifted rule writes, or null when its target is not a
     *  plain attribute/characteristic reference (an objectcreatie/verdeling/consistentie never lands here). */
    private DefaultsTarget targetOf(ResolvedRegel r) {
        if (!(r.doel() instanceof Expr.AttribuutRef d) || d.objecttype() == null) return null;
        return new DefaultsTarget(d.objecttype(), r.soort() == ResolvedRegel.Soort.KENMERKTOEKENNING, d.attribuut());
    }

    private static String targetText(DefaultsTarget d) {
        return d == null ? "onbekend"
                : d.onderwerp().naam + "." + d.naam() + (d.kenmerk() ? " (kenmerk)" : " (attribuut)");
    }

    /** A hint appended to the unknown-base ERROR when the name IS resolved, but as a construct no defaults
     *  group can be built over (a verdeling/consistentie/objectcreatie lives in its own model list, not
     *  resolvedRegels). */
    private String baseReason(String naam) {
        boolean otherConstruct = model.verdelingen.stream().anyMatch(v -> v.naam().equals(naam))
                || model.consistenties.stream().anyMatch(c -> c.naam().equals(naam))
                || model.objectCreaties.stream().anyMatch(o -> o.naam().equals(naam))
                || model.uniciteiten.stream().anyMatch(u -> u.naam().equals(naam));
        if (otherConstruct) return " (regel '" + naam + "' is een verdeling/consistentie/objectcreatie/uniciteit, geen platte regel)";
        // A decision table has no rule of its own name: its rules are its rows, and they all write the table's
        // one target, so none of them alone can be the base of a group that must be that target's only writer.
        for (var e : rowRules.entrySet())
            if (src(e.getKey().beslistabelNaam()).equals(naam) && !e.getValue().isEmpty())
                return " ('" + naam + "' is een beslistabel, geen regel: haar regels zijn de rijen "
                        + enumeration(e.getValue()) + ", die samen hetzelfde doel schrijven — een defaults-groep heeft "
                        + "één basisregel en geen andere schrijver op haar doel (WF-050))";
        return "";
    }

    /** "'a'", "'a' en 'b'", "'a', 'b' en 'c'": names as a Dutch list. */
    private static String enumeration(List<String> names) {
        List<String> q = names.stream().map(n -> "'" + n + "'").toList();
        return q.size() == 1 ? q.get(0)
                : String.join(", ", q.subList(0, q.size() - 1)) + " en " + q.get(q.size() - 1);
    }

    private void multiVersionError(String rol, String naam, int line) {
        model.diagnostics.add(new Diagnostic(Severity.ERROR,
                "Uitzondering op: de " + rol + " '" + naam + "' heeft meer dan één regelversie — meerdere versies op "
                + "één defaults-groep worden in deze kaart nog niet ondersteund (§4)", line));
    }

    // ==============================================================================================
    // The opgave marker '(mag opgegeven worden)' on an attribute declaration — the uchar DIALECT EXTENSION of
    // grammar/DIVERGENCES.md §7: the hardship pattern of spec/proposals/PHASE4-DEFAULTS.md §4.1 as a declaration.
    // For a marked attribute X of object type T the resolver lowers onto structure the wire already carries: the
    // input attribute 'opgegeven X'; the attribute 'berekende X', which every computing writer of X, on T or on a
    // subtype of T, targets from then on, each on its own type; one defaults group 'opgave boven berekening X' on X,
    // the opgave above the computation and an authored initialisation of X on T as the lowest default, which every
    // subtype inherits whole (RUNTIME-SEMANTICS ch. 7.10); the possessive characteristic 'afwijkende opgave X',
    // attributed by the rule 'afwijking opgave X' where opgave and computation are both filled and differ; and the
    // consistency 'controle opgave X', which asserts that characteristic absent, so a contradiction deposits one §9.5
    // fact.
    // The generated members are declared BEFORE the rules resolve, so a rule can read them (an author routes a
    // deviating case through the characteristic); the writers move AFTER the defaults lowering, so a tree or an
    // unordered set of the dialect's own blocks moves whole. Case input on X itself keeps Reading A
    // (RUNTIME-SEMANTICS ch. 7.8): the marker adds an input attribute, it does not change what input on X means.
    // ==============================================================================================

    /** A marked attribute whose declaration passed the declaration checks, with its generated members declared. The
     *  six generated names are fixed by the attribute's declared name; the form names are unique in the whole model
     *  (WF-020), the member names in the inheritance line of the type. */
    private record Input(Objecttype type, String attribuut, int line, AttribuutSpecificatieContext declaratie) {
        String opgegeven() { return "opgegeven " + attribuut; }
        String computed() { return "berekende " + attribuut; }
        String afwijkend() { return "afwijkende opgave " + attribuut; }
        String group() { return "opgave boven berekening " + attribuut; }
        String regel() { return "afwijking opgave " + attribuut; }
        String consistencyCheck() { return "controle opgave " + attribuut; }
        String head() { return "attribuut '" + attribuut + "' van " + type.naam; }
    }

    private static final String MARKING = "'(mag opgegeven worden)'";

    /** The markers that passed {@link #inputDeclarations}, in declaration order. */
    private final List<Input> inputs = new ArrayList<>();

    /** The markers {@link #inputLowering} lowered, in declaration order: refused and withdrawn ones stay out. */
    private final List<Input> gelowerd = new ArrayList<>();

    /** The declaration half, before any rule resolves. A marker on a characteristic is refused. A marked attribute
     *  must be a Numeriek without a unit or with one unit, or a Datum in dagen, not time-dependent and not dimensioned,
     *  and none of the member names it generates may exist yet in the inheritance line of its type; then its two
     *  attributes are
     *  declared right after it and its characteristic at the end of the type's characteristics. One ERROR per marker,
     *  at the declaration's line, naming the first reason that holds. */
    private void inputDeclarations(RegelSpraakDocumentContext doc) {
        // the declaration line of every member, per declaring type, for the collision message
        Map<Objecttype, Map<String, Integer>> memberLines = new java.util.IdentityHashMap<>();
        List<Objecttype> types = new ArrayList<>();
        List<ObjectTypeMemberContext> marked = new ArrayList<>();
        for (DefinitieContext d : doc.definitie()) {
            ObjectTypeNaamContext typeName;
            List<ObjectTypeMemberContext> leden;
            if (d.objectTypeDefinition() != null) {
                typeName = d.objectTypeDefinition().objectTypeNaam();
                leden = d.objectTypeDefinition().objectTypeMember();
            } else if (d.objectTypeExtensie() != null) {
                typeName = d.objectTypeExtensie().objectTypeNaam();
                leden = d.objectTypeExtensie().objectTypeMember();
            } else continue;
            Objecttype t = model.objecttypen.get(src(typeName));
            if (t == null) continue;
            for (ObjectTypeMemberContext member : leden) {
                if (Boolean.FALSE.equals(repetitions.get(member))) continue;
                String naam = member.attribuutSpecificatie() != null ? src(member.attribuutSpecificatie().lowerNaam())
                        : member.kenmerkSpecificatie() != null ? characteristicDeclarationName(member.kenmerkSpecificatie()) : null;
                if (naam == null) continue;
                memberLines.computeIfAbsent(t, k -> new java.util.HashMap<>())
                        .putIfAbsent(dev.uchar.regelspraak.emit.IrEmitter.norm(naam), member.getStart().getLine());
                if (markedMember(member)) { types.add(t); marked.add(member); }
            }
        }
        for (int i = 0; i < marked.size(); i++) {
            Objecttype t = types.get(i);
            ObjectTypeMemberContext member = marked.get(i);
            if (member.kenmerkSpecificatie() != null) {
                fout(member.getStart().getLine(), "kenmerk '" + characteristicDeclarationName(member.kenmerkSpecificatie()) + "' van "
                        + t.naam + ": " + MARKING + " staat alleen op een attribuut — casusinvoer op een kenmerk staat "
                        + "al tegen elk besluit van een defaults-groep (RUNTIME-SEMANTICS ch. 7.8), en een opgave 'niet "
                        + "aanwezig' boven een berekende toekenning vraagt een opgave met drie waarden, die de casus voor "
                        + "een kenmerk niet kent (dialect §7)");
                continue;
            }
            AttribuutSpecificatieContext a = member.attribuutSpecificatie();
            Input o = new Input(t, src(a.lowerNaam()), a.getStart().getLine(), a);
            String reason = inputDeclarationError(o, memberLines);
            if (reason != null) { fout(o.line(), reason); continue; }
            Datatype dt = t.attributen.get(o.attribuut());
            java.util.LinkedHashMap<String, Datatype> oud = new java.util.LinkedHashMap<>(t.attributen);
            t.attributen.clear();
            for (var e : oud.entrySet()) {
                t.attributen.put(e.getKey(), e.getValue());
                if (e.getKey().equals(o.attribuut())) {
                    t.attributen.put(o.opgegeven(), dt);
                    t.attributen.put(o.computed(), dt);
                }
            }
            t.kenmerken.add(o.afwijkend());
            t.characteristicKinds.put(o.afwijkend(), Objecttype.KenmerkSoort.BEZITTELIJK);
            inputs.add(o);
        }
    }

    static boolean markedMember(ObjectTypeMemberContext member) {
        if (member.attribuutSpecificatie() != null) return member.attribuutSpecificatie().MAG_OPGEGEVEN_WORDEN() != null;
        KenmerkSpecificatieContext k = member.kenmerkSpecificatie();
        if (k instanceof BijvoeglijkKenmerkContext b) return b.MAG_OPGEGEVEN_WORDEN() != null;
        if (k instanceof NaamwoordKenmerkContext n) return n.MAG_OPGEGEVEN_WORDEN() != null;
        return false;
    }

    private static String characteristicDeclarationName(KenmerkSpecificatieContext k) {
        if (k instanceof BijvoeglijkKenmerkContext b) return src(b.lowerNaam());
        if (k instanceof NaamwoordKenmerkContext n) return src(n.lowerNaam());
        return src(k);
    }

    /** The first declaration reason that refuses the marker, or null. */
    private String inputDeclarationError(Input o, Map<Objecttype, Map<String, Integer>> memberLines) {
        Objecttype t = o.type();
        String x = o.attribuut();
        AttribuutSpecificatieContext a = o.declaratie();
        Datatype dt = t.attributen.get(x);
        // Three families are measured in the three executors, each on a golden of its own: a Numeriek without a unit,
        // a Numeriek with one unit, and a Datum in dagen; another family waits for a witness of its own. One unit: a
        // rate ("€/jr") or a product of units is composite and stays refused. The opgave and the computation share
        // that unit without a conversion: 'opgegeven X' and 'berekende X' carry X's datatype, and a writer or an
        // input in another unit of the same system is refused by the typing of an assignment, as without the marker.
        // A unit clause after a date needs no sentence here: it is an ERROR where the datatype is read.
        boolean memberUnit = a.MET_EENHEID() != null;
        boolean getal = dt instanceof Datatype.Numeriek n
                && (n.hasUnit() ? oneUnit(n.eenheid()) : !memberUnit);
        if (!getal && !(dt instanceof Datatype.DatumInDagen)) {
            NumeriekDatatypeContext nd = a.datatype() == null ? null : a.datatype().numeriekDatatype();
            String named = dt instanceof Datatype.Numeriek n && n.hasUnit()
                    ? "Numeriek met eenheid " + (nd != null && nd.eenheidExpressie() != null
                            ? src(nd.eenheidExpressie()) : n.eenheid().weergave())
                    : dt instanceof Datatype.Numeriek && memberUnit ? "Numeriek met eenheid " + src(a.eenheidExpressie())
                    : dt == null ? "onbekend" : dt.naam();
            return o.head() + ": " + MARKING + " staat alleen op een Numeriek attribuut zonder eenheid of met één "
                    + "eenheid, of op een Datum in dagen, en '" + x + "' is " + named + " — een defaults-groep op een "
                    + "attribuut van een ander datatype of met een samengestelde eenheid is nog niet in alle drie de "
                    + "uitvoerders gemeten (dialect §7)";
        }
        if (t.attribuutGranulariteit.containsKey(x))
            return o.head() + ": " + MARKING + " staat niet op een tijdsafhankelijk attribuut — een defaults-groep "
                    + "beslist één keer en heeft geen tijdlijn om op te schrijven (WF-086) (dialect §7)";
        List<String> dimensies = t.attributeDimensions.get(x);
        if (dimensies != null && !dimensies.isEmpty())
            return o.head() + ": " + MARKING + " staat niet op een gedimensioneerd attribuut, en '" + x + "' is "
                    + "gedimensioneerd met " + String.join(" en ", dimensies) + " — een defaults-groep op een "
                    + "gedimensioneerd attribuut speelt nog niet in alle drie de uitvoerders (dialect §7)";
        // a subtype inherits the generated members, a supertype's members are inherited by T: either way one
        // instance would carry two declarations of one name (WF-105)
        for (String g : List.of(o.opgegeven(), o.computed(), o.afwijkend())) {
            String sleutel = dev.uchar.regelspraak.emit.IrEmitter.norm(g);
            for (Objecttype u : model.objecttypen.values()) {
                if (u != t && !u.isEen(t) && !t.isEen(u)) continue;
                boolean attribuut = u.attributen.keySet().stream()
                        .anyMatch(k -> dev.uchar.regelspraak.emit.IrEmitter.norm(k).equals(sleutel));
                boolean kenmerk = u.kenmerken.stream()
                        .anyMatch(k -> dev.uchar.regelspraak.emit.IrEmitter.norm(k).equals(sleutel));
                if (!attribuut && !kenmerk) continue;
                int regel = memberLines.getOrDefault(u, Map.of()).getOrDefault(sleutel, 0);
                return o.head() + " (bronregel " + o.line() + "): " + MARKING + " genereert "
                        + (g.equals(o.afwijkend()) ? "het kenmerk '" : "het attribuut '") + g + "', en " + u.naam
                        + " declareert die naam al als " + (attribuut ? "attribuut" : "kenmerk") + " (bronregel " + regel
                        + ") — een naam die de markering genereert, bestaat nog niet in de erflijn van " + t.naam
                        + " (dialect §7)";
            }
        }
        return null;
    }

    /** One unit: no denominator (a rate such as "€/jr") and no product of units ("€·m"). */
    private static boolean oneUnit(dev.uchar.regelspraak.model.Eenheid e) {
        return e != null && e.denominator() == null && !e.afkorting().contains("·");
    }

    /** The rule half, after the defaults lowering: move the computing writers of X to 'berekende X', lift an
     *  initialisation of X into the generated group, and add the group, the attribution rule and the consistency. A
     *  writer on a subtype of T moves as one on T does and keeps its type, so the writers of 'berekende X' compose
     *  per concrete type by ch. 7.10 exactly as those of X did without the marker, and the group on T, which no
     *  subtype writes beside, is the top of every subtype's stack. An initialisation of X on a subtype stays on X,
     *  where the fold of ch. 7.10 makes it the lowest default of that subtype's stack, its condition kept.
     *  Refused, one ERROR at the declaration's line: an allocation or an object creation that writes X (the marker
     *  moves neither, and beside the group each is a second writer, WF-050); a writer of a generated member; a
     *  generated form name another form already carries (WF-020); more than one initialisation of X on T, or one
     *  with a window, with versions or with a condition (the group has one window and one initialisation default, and
     *  that default's justification is the empty input); two initialisations in one inheritance line, and one on a
     *  subtype with a window or with versions ({@link #subtypeInitialisationError}). Without a computing writer X is
     *  input already and nothing is generated: a WARNING, and the generated members are withdrawn — an ERROR instead
     *  when a rule reads one of them, since the name would then read nothing. */
    private void inputLowering(RegelSpraakDocumentContext doc) {
        if (inputs.isEmpty()) return;
        Map<String, String[]> vormen = formSites(doc);
        Map<String, Input> gegenereerd = new java.util.HashMap<>();
        for (Input o : inputs) {
            Objecttype t = o.type();
            List<Integer> computing = new ArrayList<>();
            List<Integer> groups = new ArrayList<>();
            List<ResolvedRegel> init = new ArrayList<>();
            List<ResolvedRegel> subInit = new ArrayList<>();
            for (int i = 0; i < model.resolvedRegels.size(); i++) {
                ResolvedRegel r = model.resolvedRegels.get(i);
                if (r.soort() == ResolvedRegel.Soort.KENMERKTOEKENNING) continue;
                Objecttype u = ownTarget(r.doel(), o.attribuut());
                if (u == null || !u.isEen(t)) continue;
                if (r.soort() != ResolvedRegel.Soort.INITIALISATIE) computing.add(i);
                else if (u == t) init.add(r);
                else subInit.add(r);
            }
            for (int i = 0; i < model.prioritizedDefaults.size(); i++) {
                PrioritizedDefaults pd = model.prioritizedDefaults.get(i);
                Objecttype u = model.objecttypeOpNaam(pd.onderwerp());
                if (!pd.isKenmerk() && sameName(pd.attribuut(), o.attribuut()) && u != null && u.isEen(t))
                    groups.add(i);
            }
            String reason = inputWriterError(o);
            if (reason == null && computing.isEmpty() && groups.isEmpty()) {
                // nothing to put the opgave above: X is input already
                o.type().attributen.remove(o.opgegeven());
                o.type().attributen.remove(o.computed());
                o.type().kenmerken.remove(o.afwijkend());
                o.type().characteristicKinds.remove(o.afwijkend());
                String read = readGenerated(o);
                if (read != null)
                    fout(o.line(), o.head() + ": " + MARKING + " genereert niets, omdat geen regel '" + o.attribuut()
                            + "' berekent of vaststelt, maar het model leest '" + read + "' — die naam bestaat alleen "
                            + "waar de markering een berekening van '" + o.attribuut() + "' verlegt (dialect §7)");
                else
                    model.diagnostics.add(new Diagnostic(Severity.WARNING, o.head() + ": " + MARKING + " staat op een "
                            + "attribuut dat geen regel berekent of vaststelt — '" + o.attribuut() + "' is al invoer, en "
                            + "de markering genereert niets (dialect §7)", o.line()));
                continue;
            }
            if (reason == null) reason = formNameError(o, vormen, gegenereerd);
            if (reason == null) reason = initialisationError(o, init);
            if (reason == null) reason = subtypeInitialisationError(o, init, subInit);
            if (reason != null) { fout(o.line(), reason); continue; }

            // 2. the computing writers keep their names and shapes and write 'berekende X'
            for (int i : computing) {
                ResolvedRegel r = model.resolvedRegels.get(i);
                ResolvedRegel moved = new ResolvedRegel(r.naam(), r.soort(),
                        toComputed((Expr.AttribuutRef) r.doel(), o.computed()), r.waarde(), r.voorwaarde(),
                        r.geldigheid(), r.statuteAnnotations());
                if (refusedTargets.contains(r)) refusedTargets.add(moved);
                model.resolvedRegels.set(i, moved);
            }
            for (int i : groups) {
                PrioritizedDefaults pd = model.prioritizedDefaults.get(i);
                PrioritizedDefaults moved = new PrioritizedDefaults(pd.naam(), pd.onderwerp(), null, o.computed(),
                        pd.ongeordend(), pd.defaults(), pd.bron(), pd.geldigheid());
                Origin h = pdOrigin.remove(pd);
                if (h != null) pdOrigin.put(moved, h);
                model.prioritizedDefaults.set(i, moved);
            }
            // 3. the group on X: the opgave, the computation, and the initialisation, which stays on X — its
            //    justification is "X is empty in the input", which on 'berekende X' (never input) would always hold
            Datatype dt = t.attributen.get(o.attribuut());
            List<Default> defaults = new ArrayList<>();
            defaults.add(new Default(null, ALTIJD, attributeRead(o, o.opgegeven(), dt), false, null, o.opgegeven()));
            defaults.add(new Default(null, ALTIJD, attributeRead(o, o.computed(), dt), false, null, o.computed()));
            if (!init.isEmpty()) {
                ResolvedRegel r = init.get(0);
                defaults.add(new Default(null, null, r.waarde(), true, null, annotationText(r.statuteAnnotations())));
                model.resolvedRegels.removeIf(x -> x == r);
            }
            model.prioritizedDefaults.add(new PrioritizedDefaults(o.group(), t.naam, null, o.attribuut(), false,
                    defaults, null, Geldigheid.ALTIJD));
            // 4. the deviation characteristic, spelled as the frontend emits the hand-written rule
            String p0 = "zijn " + o.opgegeven() + " is gevuld";
            String p1 = "zijn " + o.computed() + " is gevuld";
            String p2 = "zijn " + o.opgegeven() + " is ongelijk aan zijn " + o.computed();
            AttribuutSpecificatieContext a = o.declaratie();
            Conditie deviation = new Conditie.Samengesteld(Conditie.Kwantor.ALLE, 0, List.of(
                    new Conditie.Predicaat(Conditie.PredicateKind.GEVULD, attributeRead(o, o.opgegeven(), dt), Bron.van(a, p0)),
                    new Conditie.Predicaat(Conditie.PredicateKind.GEVULD, attributeRead(o, o.computed(), dt), Bron.van(a, p1)),
                    new Conditie.Vergelijking(Conditie.Operator.ONGELIJK, attributeRead(o, o.opgegeven(), dt),
                            attributeRead(o, o.computed(), dt), Bron.van(a, p2))),
                    Bron.van(a, "hij aan alle volgende voorwaarden voldoet: - " + p0 + " - " + p1 + " - " + p2 + "."));
            String onderwerp = "Een " + t.naam;
            model.resolvedRegels.add(new ResolvedRegel(o.regel(), ResolvedRegel.Soort.KENMERKTOEKENNING,
                    new Expr.AttribuutRef(o.afwijkend(), t, onderwerp, null, new Datatype.Booleaan(),
                            Bron.van(a, onderwerp + " heeft " + o.afwijkend())),
                    new Expr.Literal("waar", new Datatype.Booleaan(), Bron.synthetic("waar")), deviation,
                    Geldigheid.ALTIJD, List.of()));
            // 5. the consistency: its source is the marked declaration, the one place it was written
            String declaratie = a.getStart().getInputStream().getText(org.antlr.v4.runtime.misc.Interval.of(
                    a.getStart().getStartIndex(), a.MAG_OPGEGEVEN_WORDEN().getSymbol().getStopIndex()))
                    .replaceAll("\\s+", " ").trim();
            model.consistenties.add(new dev.uchar.regelspraak.ir.ResolvedConsistency(o.consistencyCheck(), t,
                    new Conditie.Kenmerk(o.afwijkend(), null, true, Bron.van(a, "hij heeft geen " + o.afwijkend())),
                    Bron.van(a, declaratie), o.line(), Geldigheid.ALTIJD));
            for (String n : List.of(o.group(), o.regel(), o.consistencyCheck()))
                gegenereerd.put(dev.uchar.regelspraak.emit.IrEmitter.norm(n), o);
            gelowerd.add(o);
        }
    }

    /** The object type whose own attribute {@code naam} the target {@code doel} writes, or null: a navigated target
     *  never reaches here (the target fixes the subject). */
    private static Objecttype ownTarget(Expr doel, String naam) {
        if (!(doel instanceof Expr.AttribuutRef d) || d.objecttype() == null || !sameName(d.attribuut(), naam))
            return null;
        return d.objecttype();
    }

    private static boolean sameName(String a, String b) {
        return a != null && b != null && dev.uchar.regelspraak.emit.IrEmitter.norm(a)
                .equals(dev.uchar.regelspraak.emit.IrEmitter.norm(b));
    }

    /** The target {@code d} with the attribute renamed; its subject, its source text and everything else stay. */
    private static Expr.AttribuutRef toComputed(Expr.AttribuutRef d, String naam) {
        return new Expr.AttribuutRef(naam, d.objecttype(), d.subjectSource(), d.navigatieRol(), d.type(), d.bron(),
                d.set(), d.navigationPath(), d.rangePath(), d.kwantor(), d.threshold());
    }

    /** A read of the subject's own generated attribute, spelled as the frontend spells the hand-written read. */
    private static Expr.AttribuutRef attributeRead(Input o, String naam, Datatype dt) {
        return new Expr.AttribuutRef(naam, o.type(), "zijn", null, dt, Bron.van(o.declaratie(), "zijn " + naam));
    }

    /** The writer refusals: an allocation or an object creation that writes X, and a writer of a generated member.
     *  Null when none holds. A writer of X on a subtype of T is lowered; a supertype of T does not have X (WF-105), so
     *  a rule that writes X there is refused at its own target (WF-022). */
    private String inputWriterError(Input o) {
        Objecttype t = o.type();
        String x = o.attribuut();
        for (ResolvedVerdeling v : model.verdelingen)
            if (sameName(v.targetAttribute(), x) && inLijn(t, v.targetType()))
                return o.head() + ": " + MARKING + ", maar de verdeling '" + withoutVersion(v.naam()) + "' schrijft '"
                        + x + "' — de markering verlegt een verdeling niet naar '" + o.computed() + "', en naast de "
                        + "defaults-groep op '" + x + "' is zij een tweede schrijver (WF-050) (dialect §7)";
        for (ResolvedObjectCreation oc : model.objectCreaties)
            if (inLijn(t, oc.createdType())
                    && oc.initialisations().stream().anyMatch(k -> !k.kenmerk() && sameName(k.attribuut(), x)))
                return o.head() + ": " + MARKING + ", maar de objectcreatie '" + withoutVersion(oc.naam()) + "' kent '"
                        + x + "' toe bij het aanmaken — de markering verlegt die toekenning niet naar '" + o.computed()
                        + "', en naast de defaults-groep op '" + x + "' is zij een tweede schrijver (WF-050) (dialect §7)";
        // a generated member has one writer or none: the case supplies 'opgegeven X', the moved writers compute
        // 'berekende X', the generated rule attributes 'afwijkende opgave X'
        for (String g : List.of(o.opgegeven(), o.computed(), o.afwijkend())) {
            String writer = null;
            int regel = 0;
            for (ResolvedRegel r : model.resolvedRegels) {
                Objecttype u = ownTarget(r.doel(), g);
                if (u == null || !inLijn(t, u)) continue;
                writer = "regel '" + withoutVersion(r.naam()) + "'";
                int table = tableLine(r.naam());
                regel = table >= 0 ? table : r.doel().bron() == null ? 0 : r.doel().bron().line();
                break;
            }
            if (writer == null)
                for (PrioritizedDefaults pd : model.prioritizedDefaults) {
                    Objecttype u = model.objecttypeOpNaam(pd.onderwerp());
                    if (!sameName(pd.doel(), g) || u == null || !inLijn(t, u)) continue;
                    writer = "de defaults-groep '" + pd.naam() + "'";
                    regel = pdOrigin.getOrDefault(pd, new Origin(0, "")).line();
                    break;
                }
            if (writer == null)
                for (ResolvedVerdeling v : model.verdelingen)
                    if (sameName(v.targetAttribute(), g) && inLijn(t, v.targetType())) {
                        writer = "de verdeling '" + withoutVersion(v.naam()) + "'";
                        break;
                    }
            if (writer == null)
                for (ResolvedObjectCreation oc : model.objectCreaties)
                    if (inLijn(t, oc.createdType())
                            && oc.initialisations().stream().anyMatch(k -> sameName(k.attribuut(), g))) {
                        writer = "de objectcreatie '" + withoutVersion(oc.naam()) + "'";
                        break;
                    }
            if (writer != null)
                return o.head() + " (bronregel " + o.line() + "): " + writer + " schrijft '" + g + "'"
                        + (regel > 0 ? " (bronregel " + regel + ")" : "") + ", een lid dat " + MARKING + " genereert — '"
                        + o.opgegeven() + "' levert de casus, '" + o.computed() + "' krijgt de schrijvers van '" + x
                        + "', en '" + o.afwijkend() + "' kent de regel '" + o.regel() + "' toe; een andere schrijver "
                        + "verdraagt zo'n lid niet (dialect §7)";
        }
        return null;
    }

    /** Whether {@code u} is {@code t} or a strict ancestor or descendant of it. */
    private static boolean inLijn(Objecttype t, Objecttype u) {
        return u != null && (u == t || u.isEen(t) || t.isEen(u));
    }

    /** The source names of the forms WF-020 keeps in one namespace, under its name identity: every 'Regel' head (a
     *  rule, a consistency, an allocation, a creation, a uniqueness; the members of a group or of an unordered block
     *  included), every 'Uitzondering op' and 'Afwijking van' head, and every rule a decision table desugars to —
     *  each with how a message names it and the line it was written on. */
    private Map<String, String[]> formSites(RegelSpraakDocumentContext doc) {
        Map<String, String[]> result = new java.util.HashMap<>();
        for (RegelContext r : DefinitionResolver.allRules(doc)) {
            String naam = src(r.regelNaam());
            result.putIfAbsent(dev.uchar.regelspraak.emit.IrEmitter.norm(naam),
                    new String[] {"regel '" + naam + "'", String.valueOf(r.getStart().getLine())});
        }
        for (UitzonderingRegelContext u : DefinitionResolver.allExceptions(doc)) {
            String naam = src(u.regelNaam());
            result.putIfAbsent(dev.uchar.regelspraak.emit.IrEmitter.norm(naam),
                    new String[] {"regel '" + naam + "'", String.valueOf(u.getStart().getLine())});
        }
        for (var e : rowRules.entrySet())
            for (String row : e.getValue())
                result.putIfAbsent(dev.uchar.regelspraak.emit.IrEmitter.norm(row),
                        new String[] {"regel '" + row + "' van beslistabel '" + src(e.getKey().beslistabelNaam()) + "'",
                                String.valueOf(e.getKey().getStart().getLine())});
        return result;
    }

    /** A generated form name another form already carries (WF-020): an authored one, or the one another marker
     *  generated. Null when all three are free. */
    private String formNameError(Input o, Map<String, String[]> vormen, Map<String, Input> gegenereerd) {
        String[][] names = {{"de defaults-groep", o.group()}, {"de regel", o.regel()}, {"de consistentie", o.consistencyCheck()}};
        for (String[] vorm : names) {
            String sleutel = dev.uchar.regelspraak.emit.IrEmitter.norm(vorm[1]);
            String head = o.head() + " (bronregel " + o.line() + "): " + MARKING + " genereert " + vorm[0] + " '"
                    + vorm[1] + "', en ";
            String suffix = " — een vormnaam is uniek in het hele model, hoofdletters niet meegeteld (WF-020) (dialect §7)";
            String[] site = vormen.get(sleutel);
            if (site != null) return head + site[0] + " heet al zo (bronregel " + site[1] + ")" + suffix;
            Input other = gegenereerd.get(sleutel);
            if (other != null)
                return head + "de markering op " + other.head() + " genereert die naam al (bronregel " + other.line() + ")"
                        + suffix;
        }
        return null;
    }

    /** The generated group carries one initialisation default, no window of its own per default, and no condition on
     *  the initialisation default: its justification is that X is empty in the input (ch. 7.3), and WF-050 admits no
     *  written one beside it. Lowered without its condition, the initialisation would decide where the author's
     *  condition fails, so a condition is refused rather than dropped. */
    private String initialisationError(Input o, List<ResolvedRegel> init) {
        List<String> names = init.stream().map(r -> withoutVersion(r.naam())).distinct().toList();
        if (names.size() > 1)
            return o.head() + ": " + MARKING + ", en '" + o.attribuut() + "' heeft meer dan één initialisatie ("
                    + enumeration(names) + ") — de gegenereerde defaults-groep draagt één initialisatiedefault, de "
                    + "laagste (WF-050) (dialect §7)";
        if (names.size() == 1 && (init.size() > 1 || !init.get(0).geldigheid().always()))
            return o.head() + ": " + MARKING + ", en de initialisatie '" + names.get(0) + "' van '" + o.attribuut()
                    + "' heeft " + (init.size() > 1 ? "meer dan één versie" : "een ander geldigheidsvenster dan altijd")
                    + " — de gegenereerde defaults-groep geldt altijd, en een default draagt geen eigen venster "
                    + "(dialect §7)";
        if (names.size() == 1 && init.get(0).voorwaarde() != null)
            return o.head() + ": " + MARKING + ", en de initialisatie '" + names.get(0) + "' van '" + o.attribuut()
                    + "' heeft een voorwaarde — de initialisatiedefault van de gegenereerde defaults-groep is alleen "
                    + "gerechtvaardigd waar '" + o.attribuut() + "' in de invoer leeg is, en draagt geen eigen "
                    + "voorwaarde (WF-050) (dialect §7)";
        return null;
    }

    /** The initialisations of X on subtypes of T, which stay on X: the generated group opens the fold of ch. 7.10 on
     *  X's chain, and the fold makes each the lowest default of its type's stack, its condition kept, below the
     *  inherited group (measured 2026-09-30 in ref, clingo and the Drools exec branch). Refused: two initialisations
     *  in one inheritance line — T's and a subtype's, two on one subtype, or one on a subtype and one on a subtype of
     *  it — since the stack of the lower type would carry two initialisation defaults; measured, the three executors
     *  gave the higher one without a diagnostic, where the unmarked document is refused under WF-079, whose
     *  initialisation arm the group lifts. And one with a window or with versions: the fold takes no windowed rule,
     *  and the three executors refused it by name where the unmarked document plays. Null when none holds. */
    private String subtypeInitialisationError(Input o, List<ResolvedRegel> init, List<ResolvedRegel> subInit) {
        List<ResolvedRegel> lijn = new ArrayList<>(init);
        lijn.addAll(subInit);
        for (int i = 0; i < lijn.size(); i++)
            for (int j = i + 1; j < lijn.size(); j++) {
                ResolvedRegel a = lijn.get(i), b = lijn.get(j);
                Objecttype ta = ownTarget(a.doel(), o.attribuut()), tb = ownTarget(b.doel(), o.attribuut());
                if (withoutVersion(a.naam()).equals(withoutVersion(b.naam())) || !ta.isEen(tb) && !tb.isEen(ta)) continue;
                boolean aHigh = tb.isEen(ta);
                ResolvedRegel high = aHigh ? a : b, low = aHigh ? b : a;
                Objecttype th = aHigh ? ta : tb, tl = aHigh ? tb : ta;
                return o.head() + ": " + MARKING + ", en '" + o.attribuut() + "' heeft twee initialisaties in één "
                        + "erflijn ('" + withoutVersion(high.naam()) + "' voor " + th.naam + " en '"
                        + withoutVersion(low.naam()) + "' voor " + tl.naam + ") — de effectieve stapel van " + tl.naam
                        + " zou twee initialisatiedefaults dragen, en RUNTIME-SEMANTICS ch. 7.10 kent er één; zonder "
                        + "de markering weigert WF-079 dit paar (dialect §7)";
            }
        for (ResolvedRegel s : subInit) {
            long versions = subInit.stream().filter(r -> withoutVersion(r.naam()).equals(withoutVersion(s.naam()))).count();
            if (versions == 1 && s.geldigheid().always()) continue;
            String type = ownTarget(s.doel(), o.attribuut()).naam;
            return o.head() + ": " + MARKING + ", en de initialisatie '" + withoutVersion(s.naam()) + "' van '"
                    + o.attribuut() + "' voor " + type + " heeft "
                    + (versions > 1 ? "meer dan één versie" : "een ander geldigheidsvenster dan altijd")
                    + " — naast de gegenereerde defaults-groep op " + o.type().naam + " wordt zij een default in de "
                    + "effectieve stapel van " + type + " (RUNTIME-SEMANTICS ch. 7.10), en die neemt geen regel met "
                    + "een venster op (dialect §7)";
        }
        return null;
    }

    /** The first generated member name the model reads, or null: a reference to one of the two attributes on a type
     *  of the inheritance line, a check of the characteristic, or an allocation naming one of the attributes. The
     *  walk reaches every condition and value the emitter writes, conditions nested in expressions included. */
    private String readGenerated(Input o) {
        for (String g : List.of(o.opgegeven(), o.computed(), o.afwijkend())) {
            String sleutel = dev.uchar.regelspraak.emit.IrEmitter.norm(g);
            java.util.function.Predicate<Expr> ref = e -> e instanceof Expr.AttribuutRef r
                    && sameName(r.attribuut(), g) && (r.objecttype() == null || inLijn(o.type(), r.objecttype()));
            java.util.function.Predicate<Conditie> check = c -> c instanceof Conditie.Kenmerk k
                    && dev.uchar.regelspraak.emit.IrEmitter.norm(k.kenmerk()).equals(sleutel);
            boolean read = false;
            for (ResolvedRegel r : model.resolvedRegels)
                read |= uses(r.waarde(), ref, check) || uses(r.voorwaarde(), ref, check);
            for (var c : model.consistenties) read |= uses(c.assertie(), ref, check);
            for (var v : model.verdelingen)
                read |= uses(v.voorwaarde(), ref, check) || sameName(v.sourceAttribute(), g)
                        || sameName(v.ratioAttribute(), g) || sameName(v.maximumAttribute(), g)
                        || sameName(v.orderingAttribute(), g);
            for (var oc : model.objectCreaties) {
                read |= uses(oc.voorwaarde(), ref, check);
                for (var k : oc.initialisations()) read |= uses(k.waarde(), ref, check);
            }
            for (var fc : model.feitCreaties) read |= uses(fc.voorwaarde(), ref, check);
            for (var d : model.resolvedDayKindDefinitions) read |= uses(d.voorwaarde(), ref, check);
            for (var pd : model.prioritizedDefaults) read |= readsDefaults(pd.defaults(), ref, check);
            if (read) return g;
        }
        return null;
    }

    private static boolean readsDefaults(List<Default> defaults, java.util.function.Predicate<Expr> ref,
                                         java.util.function.Predicate<Conditie> check) {
        if (defaults == null) return false;
        for (Default d : defaults) {
            if (uses(d.voorwaarde(), ref, check) || uses(d.consequentie(), ref, check)) return true;
            if (d.exceptions() != null)
                for (var group : d.exceptions().leden()) if (readsDefaults(group.defaults(), ref, check)) return true;
        }
        return false;
    }

    /** Whether a node of {@code c} satisfies {@code check}, or an expression in it holds a node satisfying
     *  {@code ref}: the parts of a composite and of a quantifier fold, and every operand, conditions nested in
     *  expressions included. */
    private static boolean uses(Conditie c, java.util.function.Predicate<Expr> ref,
                                 java.util.function.Predicate<Conditie> check) {
        if (c == null) return false;
        if (check.test(c)) return true;
        return switch (c) {
            case Conditie.Samengesteld s -> s.onderdelen().stream().anyMatch(d -> uses(d, ref, check));
            case Conditie.QuantifierFold k -> uses(k.filter(), ref, check) || uses(k.test(), ref, check);
            default -> Conditie.containsOperand(c, e -> readsHere(e, ref, check));
        };
    }

    private static boolean uses(Expr e, java.util.function.Predicate<Expr> ref,
                                 java.util.function.Predicate<Conditie> check) {
        return Expr.contains(e, x -> readsHere(x, ref, check));
    }

    /** One expression node: {@link Expr#contains} visits every node, so only the conditions an expression holds need
     *  entering here. */
    private static boolean readsHere(Expr x, java.util.function.Predicate<Expr> ref,
                                     java.util.function.Predicate<Conditie> check) {
        if (ref.test(x)) return true;
        return switch (x) {
            case Expr.FunctionCall f -> uses(f.filter(), ref, check);
            case Expr.IfThenElse k -> uses(k.condition(), ref, check);
            case Expr.InstanceSetUnion u -> u.branches().stream().anyMatch(t -> uses(t.filter(), ref, check));
            default -> false;
        };
    }

    /** A read-write dependency CYCLE (x = y+1; y = x+1) never reaches a fixpoint under the freshness semantics
     *  (each write re-fires the other reader with a CHANGED value — the engine caps the run-away instead of
     *  hanging, and both back-ends would diverge forever). WF-045 forbids the writing, and
     *  {@link #checkDependencyCycles} refuses each such cycle with an ERROR in the verifier's sentence, which
     *  names forms; this WARNING stays beside it and names the cycle in the members the author wrote. It is the only
     *  notice of the cycle in a model that carries another ERROR, because that refusal judges only a document the
     *  emitter can write. The graph
     *  mirrors the reads of RUNTIME-SEMANTICS ch. 5.1 and 6.1 as far as they stay on the rule's own subject: a
     *  node is an own attribute or characteristic of the subject's type, the two kinds kept apart and names
     *  compared as the verifier compares them; the edges run from what a rule's value AND condition read —
     *  attribute references, and characteristic checks in either polarity — to its target. A SELF-read (x = x+1,
     *  the clamp idiom "x = x, met een maximum van y", a condition reading the target) is the cycle of one node:
     *  the verifier refuses it under WF-045 as it refuses a longer cycle (measured 2026-09-25), so the Drools
     *  no-loop that makes a single writer terminate is no ground to pass it in silence. Outside this graph, and so
     *  unwarned here: a navigated read, a read in the filter or test of a fold, a role check, a channel test, the forms
     *  that are no rule (an allocation, a creation, a consistency), and a cycle over a supertype and a subtype — a node
     *  belongs to the type the rule is written on, where the verifier lets a write reach every comparable type of the
     *  inheritance line (WF-045); such a cycle is refused in the verifier's sentence by
     *  {@link #checkDependencyCycles}, unless it runs through an object creation. This pass runs over the authored forms,
     *  before the defaults lowering, so a warning names what the author wrote; where the opgave marker lowered an
     *  attribute, {@link #checkCycleReferenceAfterInput} runs the graph once more over the lowered forms. */
    private void checkCycleReference() {
        Map<ReadNode, Set<ReadNode>> uses = new LinkedHashMap<>();
        Map<ReadNode, String> tekst = new java.util.HashMap<>();
        for (ResolvedRegel r : model.resolvedRegels) ruleEdges(r, uses, tekst);
        reportCycles(uses, tekst, Map.of());
    }

    /** The nodes of every cycle warned so far, a generated 'berekende X' counted as the X it computes: a cycle that
     *  brings no node beyond them is not warned again. */
    private final Set<ReadNode> cycleNodes = new java.util.HashSet<>();

    /** The same graph over the forms the opgave marker lowered to (grammar/DIVERGENCES.md §7), when it lowered one:
     *  the moved writers on 'berekende X', the generated rule on 'afwijkende opgave X', and every defaults group, the
     *  generated group on X among them. A cycle through a generated member exists only here — a computation of X that
     *  reads 'berekende X', which it writes once it has moved, or that reads 'afwijkende opgave X', which the generated
     *  rule attributes from 'berekende X' — and the verifier refuses it under WF-045 like any other. Such a cycle is
     *  warned in the names the author read. A cycle the pass over the authored forms already warned (X reading X, which
     *  becomes 'berekende X' reading X through the group) is not warned twice: here 'berekende X' counts as X. A
     *  document without a lowered marker never reaches this pass. */
    private void checkCycleReferenceAfterInput() {
        if (gelowerd.isEmpty()) return;
        Map<ReadNode, Set<ReadNode>> uses = new LinkedHashMap<>();
        Map<ReadNode, String> tekst = new java.util.HashMap<>();
        for (ResolvedRegel r : model.resolvedRegels) ruleEdges(r, uses, tekst);
        for (PrioritizedDefaults pd : model.prioritizedDefaults) {
            Objecttype t = model.objecttypeOpNaam(pd.onderwerp());
            if (t == null) continue;
            Set<ReadNode> reads = uses.computeIfAbsent(readNode(t, pd.doel(), pd.isKenmerk(), tekst),
                    k -> new java.util.LinkedHashSet<>());
            defaultEdges(pd.defaults(), t, reads, tekst);
        }
        Map<ReadNode, ReadNode> toAuthor = new java.util.HashMap<>();
        for (Input o : gelowerd) {
            String type = o.type().naam;
            toAuthor.put(
                    new ReadNode(type, dev.uchar.regelspraak.emit.IrEmitter.norm(o.computed()), false),
                    new ReadNode(type, dev.uchar.regelspraak.emit.IrEmitter.norm(o.attribuut()), false));
        }
        reportCycles(uses, tekst, toAuthor);
    }

    /** The edges of one rule: from its target, an own attribute or characteristic of the subject, to what its value
     *  and its condition read. A navigated target stays out. */
    private static void ruleEdges(ResolvedRegel r, Map<ReadNode, Set<ReadNode>> uses,
                                    Map<ReadNode, String> tekst) {
        if (!(r.doel() instanceof Expr.AttribuutRef d) || d.navigatieRol() != null || d.objecttype() == null) return;
        ReadNode doel = readNode(d.objecttype(), d.attribuut(),
                r.soort() == ResolvedRegel.Soort.KENMERKTOEKENNING, tekst);
        Set<ReadNode> reads = uses.computeIfAbsent(doel, k -> new java.util.LinkedHashSet<>());
        collectOwnReads(r.waarde(), d.objecttype(), reads, tekst);
        collectOwnReads(r.voorwaarde(), d.objecttype(), reads, tekst);
    }

    /** What a defaults group reads: every default's justification and consequence, through its nested exception
     *  groups, as the verifier folds them into the group's reads (ch. 7.4, ch. 7.9). An initialisation default's
     *  justification is the empty input and reads nothing. */
    private static void defaultEdges(List<Default> defaults, Objecttype subject, Set<ReadNode> result,
                                      Map<ReadNode, String> tekst) {
        if (defaults == null) return;
        for (Default d : defaults) {
            collectOwnReads(d.voorwaarde(), subject, result, tekst);
            collectOwnReads(d.consequentie(), subject, result, tekst);
            if (d.exceptions() != null)
                for (ExceptionGroup g : d.exceptions().leden()) defaultEdges(g.defaults(), subject, result, tekst);
        }
    }

    private void reportCycles(Map<ReadNode, Set<ReadNode>> uses, Map<ReadNode, String> tekst,
                             Map<ReadNode, ReadNode> toAuthor) {
        for (ReadNode start : uses.keySet()) {
            List<ReadNode> pad = findCycle(start, start, uses, new java.util.HashSet<>(), new ArrayList<>());
            if (pad != null && cycleNodes.addAll(pad.stream().map(k -> toAuthor.getOrDefault(k, k)).toList()))
                model.diagnostics.add(new Diagnostic(Severity.WARNING,
                        "kringverwijzing tussen berekeningen: " + String.join(" -> ", pad.stream().map(tekst::get).toList())
                        + " -> " + tekst.get(start)
                        + " — herberekening bereikt mogelijk geen fixpunt (de engine begrenst het aantal activaties)", 0));
        }
    }

    /** A node of the read-dependency graph: an own attribute or characteristic of an object type (ch. 6.1 keeps the
     *  two kinds apart), its name under the verifier's name identity. The warning names it as it was first written. */
    private record ReadNode(String type, String naam, boolean kenmerk) {}

    private static ReadNode readNode(Objecttype type, String naam, boolean kenmerk, Map<ReadNode, String> tekst) {
        ReadNode k = new ReadNode(type.naam, dev.uchar.regelspraak.emit.IrEmitter.norm(naam), kenmerk);
        tekst.putIfAbsent(k, type.naam + "." + naam);
        return k;
    }

    private static List<ReadNode> findCycle(ReadNode start, ReadNode here, Map<ReadNode, Set<ReadNode>> uses,
                                             Set<ReadNode> visited, List<ReadNode> pad) {
        if (!visited.add(here)) return null;
        pad.add(here);
        for (ReadNode to : uses.getOrDefault(here, Set.of())) {
            if (to.equals(start)) return new ArrayList<>(pad);   // one node: a self-read
            List<ReadNode> deep = findCycle(start, to, uses, visited, pad);
            if (deep != null) return deep;
        }
        pad.remove(pad.size() - 1);
        return null;
    }

    /** The own reads of a condition: the sides of a comparison, the operand of a predicate, the bounds of a window,
     *  an own characteristic check in either polarity, through every composite. The filter and the test of a
     *  quantifier fold read the element and not the subject, and stay out, as a fold's filter does in a value; a
     *  navigated or hosted characteristic check reads another object. */
    private static void collectOwnReads(Conditie c, Objecttype subject, Set<ReadNode> result,
                                           Map<ReadNode, String> tekst) {
        switch (c) {
            case null -> { }
            case Conditie.Vergelijking v -> {
                collectOwnReads(v.links(), subject, result, tekst);
                collectOwnReads(v.rechts(), subject, result, tekst);
            }
            case Conditie.Predicaat p -> collectOwnReads(p.operand(), subject, result, tekst);
            case Conditie.Periode p -> {
                collectOwnReads(p.vanaf(), subject, result, tekst);
                collectOwnReads(p.tot(), subject, result, tekst);
            }
            case Conditie.Kenmerk k -> {
                if (k.navigatie() == null && k.eigenaar() == null) result.add(readNode(subject, k.kenmerk(), true, tekst));
            }
            case Conditie.Samengesteld sg -> sg.onderdelen().forEach(o -> collectOwnReads(o, subject, result, tekst));
            default -> { }
        }
    }

    /** The OWN attributes (same objecttype) an expression reads, as attribute nodes — the doel itself included,
     *  since a self-read is a cycle too ({@link #checkCycleReference}). */
    private static void collectOwnReads(Expr e, Objecttype subject, Set<ReadNode> result, Map<ReadNode, String> tekst) {
        if (e instanceof Expr.AttribuutRef a) {
            if (a.navigatieRol() == null && a.objecttype() == subject) result.add(readNode(subject, a.attribuut(), false, tekst));
        } else if (e instanceof Expr.Binair b) {
            collectOwnReads(b.links(), subject, result, tekst);
            collectOwnReads(b.rechts(), subject, result, tekst);
        } else if (e instanceof Expr.FunctionCall f) {
            for (Expr a : f.arguments()) collectOwnReads(a, subject, result, tekst);
        } else if (e instanceof Expr.Afgerond roundedExpr) {
            collectOwnReads(roundedExpr.basis(), subject, result, tekst);
        } else if (e instanceof Expr.Begrensd bg) {
            collectOwnReads(bg.basis(), subject, result, tekst);
            collectOwnReads(bg.minimum(), subject, result, tekst);
            collectOwnReads(bg.maximum(), subject, result, tekst);
        } else if (e instanceof Expr.TextComposition tr) {
            for (Expr.TextComposition.Part d : tr.parts())
                if (d instanceof Expr.TextComposition.EmbeddedExpression i) collectOwnReads(i.expr(), subject, result, tekst);
        } else if (e instanceof Expr.DimensionCellRead dc) {
            collectOwnReads(dc.attribuut(), subject, result, tekst);
        } else if (e instanceof Expr.Let l) {
            // ch. 4.10: since the let-lowering card a rule value is a Let CHAIN, so both halves carry reads —
            // a variabelendeel definition (the value) as much as the expression that reads it (the body).
            // Stopping at the Let would drop every §11 variable's edge out of the cycle graph.
            collectOwnReads(l.value(), subject, result, tekst);
            collectOwnReads(l.body(), subject, result, tekst);
        }
    }

    // ==============================================================================================
    // The set fence (IR-SPEC ch. 4.3), producer side. Until L2 of the v1 wire migration it was WF-096 on the
    // wire (maintainer decisions 2026-09-09 and — the KIND half — 2026-09-19). Since L2 the wire has no set
    // flag, and a set reading the fence let through would reach it as a read over a to-many step (WF-053) or,
    // as the source of a count, as a count over member values (WF-093 (b)). The frontend keeps the reading in
    // memory and refuses it here.
    // ==============================================================================================

    /** The seven VALUE folds — the kinds that read a value per contributor, and so the only kinds that
     *  consume what a set reading hands them. This is the exemption the verifier drew in
     *  {@code set_source_exempt} under WF-096 until L2 and draws in {@code source_reading} since (arity one
     *  AND a value-folding kind AND a resolved element type), so the frontend's fence and the wire's are the
     *  same boundary.
     *
     *  <p><b>AANTAL_GEVULDE joined this set on 2026-09-20</b> (decision G9's card). The verifier's
     *  {@code value_fold_name} grew the kind on 2026-09-20 with the wire itself, and this set did not
     *  follow for two cards — the two fences ran six against seven while no frontend could write the
     *  kind, so nothing measured the gap. It is the same class of leak the AANTAL narrowing below was,
     *  from the other side, which is why {@code FoldKindsBoundaryTest} now READS the verifier's own
     *  list instead of trusting a comment that says the two agree.
     *  The kind belongs here for the fence's own reason: a filled count is a count in its RESULT but a
     *  VALUE fold in its READING — it asks each contributor's value whether it is there
     *  (`RUNTIME-SEMANTICS.md` ch. 3.4c), which is exactly what a set reading hands it.
     *
     *  <p><b>AANTAL left this set on 2026-09-19</b> (maintainer decision, `spec/IR-SPEC.md` ch. 4.3):
     *  a count folds the CONTRIBUTORS themselves — `RUNTIME-SEMANTICS.md` ch. 3.4a says so in as many
     *  words and ch. 3.4b restates it for the union — so a set reading under a count names a
     *  collection the node does not read. Keeping it here was a LEAK, not a stale comment: measured
     *  2026-09-19, a count over a set reading passed this naloop untouched and was emitted with
     *  {@code diagnostics: []} although the verifier rejected it under WF-096 and nothing else
     *  ({@link dev.uchar.regelspraak.CountSetSourceTest}).
     *
     *  <p><b>One boundary, one consumer — measured before narrowing.</b> The verifier had to SPLIT its
     *  hoisted exemption then, because one boolean there fed two rules (WF-034's sentinel fence, which
     *  exempted the whole folding family, and WF-096's set fence, which no longer did; both left with L2). Here no
     *  split is needed and that was checked rather than assumed: this set has exactly ONE reader, the
     *  fold-source test in {@link #checkSetRead}, and it is the frontend's only collection of fold
     *  kinds at all — the count's own element derivation keys on {@code kind == AANTAL} directly
     *  ({@code ExpressionBuilder.aggregationElement}) and never consults this set. So the narrowing moves
     *  the set fence and nothing else. The SENTINEL half has no counterpart here: the frontend writes
     *  the row-1 wart from its own route and WF-093 governs it, which is why the bare count stays
     *  clean under this narrowing and its witness says so. */
    static final Set<Expr.FunctionKind> FOLD_KINDS = Set.of(
            Expr.FunctionKind.SOM, Expr.FunctionKind.TOTAAL, Expr.FunctionKind.MINIMUM,
            Expr.FunctionKind.MAXIMUM, Expr.FunctionKind.EERSTE, Expr.FunctionKind.LAATSTE,
            Expr.FunctionKind.AANTAL_GEVULDE);

    /** A §13.4.2 set quantifier ("alle &lt;mv&gt;", and every other quantor that is not the bare
     *  definite "de") makes a reference a SET reading: it names the members' values, one per member —
     *  a collection. Exactly one node consumes a collection, the instance fold that OWNS the source,
     *  so the shape is ill-formed at every other READ position: as a whole rule value, as an operand of a
     *  composite node, inside a condition, and as an operand of the listed value fold (WF-096 until L2; since
     *  then WF-053 at such a position, and WF-093 (b) at a count's source).
     *
     *  <p>Measured on this frontend (2026-09-11), all from the one sentence fragment "het gewicht van
     *  alle vondsten van de Jutter": eight routes reached the wire with {@code diagnostics: []} — a
     *  BEREKENING value, an operand of a {@code Binair}, a §13.4.4 GELIJKSTELLING value, a §13.4.9
     *  INITIALISATIE value, a rule voorwaarde, a §13.4.8 consistentieregel, an objectcreatie
     *  voorwaarde and a §13.4.6 objectcreatie-toekenning value — and the uchar verifier rejected all
     *  eight under WF-096.
     *
     *  <p>A NALOOP over the built tree, not a test inside {@link ExpressionBuilder}: the reference is
     *  built before the node that would consume it exists, so "is there a fold around this" is a
     *  question only the enclosing tree can answer, and it is a local one there.
     *
     *  <p>WARNING, deliberately, and the level is the one knob here. The diagnostic reaches the WIRE
     *  ({@link dev.uchar.regelspraak.emit.IrEmitter} writes every diagnostic), so the emitted
     *  document states that it is ill-formed and why — where it used to say {@code diagnostics: []}.
     *  ERROR would be the fail-fast level and would stop the document reaching disk at all
     *  ({@link Resolver#irModus} writes nothing when the model carries an error); it is deliberately
     *  NOT taken here, because one committed test expectation —
     *  {@code IrTest.references_resolve_via_fact_type_roles}, a third witness of the same producer
     *  route — asserts that a rule value of exactly this shape resolves error-free, and moving that is
     *  the maintainer's call. Raising the level is a one-token change at the {@link Severity} below.
     *
     *  <p>Scope: the READ. A rule's {@code doel} is a WRITE position, outside the fence (under WF-096 it was
     *  verifier-clean and all three executors ignored the field; since L2 the wire carries no set flag at all),
     *  so it is deliberately not walked.
     *  <p>The three VALUE slots of this repo's IR are all walked, and that is the whole list rather than
     *  a sample: a {@link ResolvedRegel}'s waarde, a {@code ResolvedConsistency}'s assertie, and a
     *  §13.4.6 {@link ResolvedObjectCreation.MemberInit}'s waarde — the last a generic expressie RHS and
     *  a measured producer route of its own. {@link ResolvedVerdeling} and {@link ResolvedFactCreation}
     *  hold no Expr at all (bare attribute/field NAMES plus a condition), so there is nothing to walk
     *  there; every construct's CONDITION is covered one line after it is built, see the call in
     *  {@link #ruleVersions(String, java.util.List, java.util.List, int)}. */
    private void checkSetReads() {
        // each read from the subject the emitter writes it from: the target's type, the consistency's, the creation's
        for (ResolvedRegel r : model.resolvedRegels)
            checkSetRead(r.waarde(), r.doel() instanceof Expr.AttribuutRef d ? d.objecttype() : null, false);
        for (var c : model.consistenties) checkSetRead(c.assertie(), c.subject());
        for (var oc : model.objectCreaties)
            for (var t : oc.initialisations()) checkSetRead(t.waarde(), oc.subject(), false);
    }

    /** Walk {@code e} for set readings outside the fence's exempt position. {@code foldSource} marks the ONE exempt
     *  position: the single
     *  argument of a VALUE-folding kind that carries an {@code elementObjectType} — the source the fold
     *  consumes. That is the boundary the verifier draws (arity, kind AND a resolved element type):
     *  narrower here would refuse a legitimate fold source, wider would let the frontend emit a
     *  document the verifier rejects. A {@link Expr.DimensionCellRead} wrapping the source is NOT that
     *  position — the verifier's fold branch matches the bare reference — so the exemption stops at
     *  the wrapper, exactly as it does there.
     *
     *  <p>The switch is EXHAUSTIVE over the sealed union — no {@code default} arm — for the reason
     *  {@link Expr#contains} states: the next node the union grows must be a compile error here rather
     *  than a silent leaf that under-reports. */
    private void checkSetRead(Expr e, Objecttype subject, boolean foldSource) {
        checkSetRead(e, subject, foldSource, null);
    }

    /** {@code countSource}, when not null, is the element type of the {@code AANTAL} whose single argument {@code e}
     *  is: a set reading there is refused under the count's own rule, WF-093 (b); at every other non-exempt
     *  position it is a read over a to-many step, WF-053's. {@code subject} is the type the read is written from,
     *  as the emitter writes it: a fold's filter and a selection's filter read each contributor. */
    private void checkSetRead(Expr e, Objecttype subject, boolean foldSource, Objecttype countSource) {
        if (e == null) return;
        switch (e) {
            case Expr.AttribuutRef a -> {
                if (a.set() && !foldSource)
                    model.diagnostics.add(new Diagnostic(Severity.ERROR, setReadError(a, subject, countSource),
                            a.bron().line()));
            }
            case Expr.FunctionCall f -> {
                boolean isFoldSource = f.elementObjectType() != null && f.arguments().size() == 1
                        && FOLD_KINDS.contains(f.kind());
                Objecttype countElement = f.elementObjectType() != null && f.arguments().size() == 1
                        && f.kind() == Expr.FunctionKind.AANTAL ? f.elementObjectType() : null;
                for (Expr a : f.arguments()) checkSetRead(a, subject, isFoldSource, countElement);
                checkSetRead(f.emptyFallback(), subject, false);   // §5.8.2 fallback: an ordinary read, never a source
                // a dimension fold has no element: its filter reads the subject's own labels
                checkSetRead(f.filter(), f.dimensieSelectie() == null && f.elementObjectType() != null
                        ? f.elementObjectType() : subject);
            }
            case Expr.Binair b -> {
                checkSetRead(b.links(), subject, false); checkSetRead(b.rechts(), subject, false);
            }
            case Expr.Afgerond a -> checkSetRead(a.basis(), subject, false);
            case Expr.Begrensd b -> {
                checkSetRead(b.basis(), subject, false); checkSetRead(b.minimum(), subject, false);
                checkSetRead(b.maximum(), subject, false);
            }
            case Expr.TextComposition t -> {
                for (Expr.TextComposition.Part d : t.parts())
                    if (d instanceof Expr.TextComposition.EmbeddedExpression i) checkSetRead(i.expr(), subject, false);
            }
            case Expr.DimensionCellRead c -> checkSetRead(c.attribuut(), subject, false);
            case Expr.Let l -> { checkSetRead(l.value(), subject, false); checkSetRead(l.body(), subject, false); }
            case Expr.IfThenElse k -> {
                checkSetRead(k.then(), subject, false); checkSetRead(k.otherwise(), subject, false);
                checkSetRead(k.condition(), subject);
            }
            // the §5.7 union's own fields are symbolic (a type, a role binding), so the only place a
            // setlezing could hide is a selection's filter — reached like every other conditie
            case Expr.InstanceSetUnion u -> u.branches().forEach(t ->
                    checkSetRead(t.filter(), t.elementType() != null ? t.elementType() : subject));
            case Expr.Literal ignored -> { }
            case Expr.ParameterRef ignored -> { }
            case Expr.VariableRef ignored -> { }
            case Expr.BinderRef ignored -> { }
            case Expr.Constante ignored -> { }
            case Expr.Unresolved ignored -> { }
        }
    }

    /** The condition half of the same walk, reaching exactly what {@link Conditie#containsExpr} reaches:
     *  comparison sides, the one-sided predicate operand, periode bounds, compound children, and both
     *  halves of a §8.2 quantifier fold. No condition position is a fold source, so every operand is
     *  walked unexempted — the {@link Conditie.QuantifierFold} included, and deliberately so: the §8.2
     *  lowering does not EXCUSE a set reading, it REMOVES it (the quantified operand comes out as an
     *  element-own {@code set: false} read), so a surviving set reading under that node is a genuine
     *  violation and is named like any other. */
    private void checkSetRead(Conditie c, Objecttype subject) {
        if (c == null) return;
        if (c instanceof Conditie.Vergelijking v) {
            checkSetRead(v.links(), subject, false); checkSetRead(v.rechts(), subject, false);
        }
        else if (c instanceof Conditie.Predicaat p) checkSetRead(p.operand(), subject, false);
        else if (c instanceof Conditie.Periode p) {
            checkSetRead(p.vanaf(), subject, false); checkSetRead(p.tot(), subject, false);
        }
        else if (c instanceof Conditie.Samengesteld s) for (Conditie d : s.onderdelen()) checkSetRead(d, subject);
        else if (c instanceof Conditie.QuantifierFold k) {
            // the filter and the test read each contributor, with the element as their subject
            Objecttype element = k.elementObjectType() != null ? k.elementObjectType() : subject;
            checkSetRead(k.filter(), element); checkSetRead(k.test(), element);
        }
    }

    /** The refusal of a set reading outside a fold source, in the verifier's sentence for the read the emitter
     *  writes: under a count, a source that reads a member of the element type (WF-093); anywhere else, a path
     *  with a to-many hop in a slot that takes one value (WF-053), one sentence per such hop. Where the emitter
     *  writes no such read — no path is derivable, no hop is to-many, or the count's source names no member of its
     *  element type — no reader refuses the document in these words, and the refusal keeps this frontend's. */
    private String setReadError(Expr.AttribuutRef a, Objecttype subject, Objecttype countSource) {
        String head = "setlezing buiten een vouwbron '" + a.bron() + "': ";
        if (countSource != null && countSource.hasMember(a.attribuut()))
            return head + "the source of this count aggregation reads '" + a.attribuut() + "', a member of the "
                    + "element type '" + countSource.naam + "': a count folds INSTANCES, so its source is a count source "
                    + "naming the relation's role, never a value read per contributor";
        if (countSource == null) {
            List<RegelSpraakModel.RoleBindingRef> pad =
                    dev.uchar.regelspraak.emit.IrEmitter.navigationBindings(model, subject, a);
            List<String> sentences = new ArrayList<>();
            for (int i = 0; i < pad.size(); i++)
                if (toMany(pad.get(i)))
                    sentences.add("hop " + i + " reaches the to-many role '" + pad.get(i).rol() + "' in a scalar slot "
                            + "— a to-many step needs an aggregation, never a silent first pick");
            if (!sentences.isEmpty()) return head + String.join("; ", sentences);
        }
        return "setlezing buiten een vouwbron: '" + a.bron() + "' noemt de waarde van elk lid van de "
                + "verzameling — alleen toegestaan als bron van één waardevouw (som, totaal, minimum, "
                + "maximum, eerste, laatste, aantal gevulde waarden); "
                + (countSource != null
                        ? "een instantietelling vouwt de leden zelf en leest geen waarde per lid, dus als "
                          + "bron van 'aantal' ook niet (WF-093 (b), IR-SPEC ch. 4.3)"
                        : "hier wordt één waarde verwacht, en een meervoudige stap in een lezing "
                          + "vraagt een aggregatie (WF-053, IR-SPEC ch. 4.1a)");
    }

    /** Whether the role a hop reaches is to-many, as its fact type declares it. */
    private boolean toMany(RegelSpraakModel.RoleBindingRef step) {
        Feittype ft = model.feittypen.get(step.feittype());
        return ft != null && ft.rollen.stream().anyMatch(r -> r.naam().equals(step.rol()) && r.meervoudig());
    }

    /** Process one §13.4.2 regel's versions into the model. Reused for each §12 beslistabel row, which
     *  {@link DecisionTableDesugar} lowers to an ordinary regel and {@link #decisionTables} re-parses to a
     *  {@link RegelContext} — so a table row reuses the WHOLE rule pipeline (and both back-ends) unchanged. */
    private void ruleVersions(RegelContext r) {
        ruleVersions(DefinitionResolver.src(r.regelNaam()), r.regelVersie(), statuteAnnotations(r), r.getStart().getLine());
    }

    /** §4 (PRIORITIZED-DEFAULTS-EVOLUTIE) resolve an ordered 'Uitzondering op' block's body exactly as a §13.4.2 regel — its
     *  own regelNaam + regelVersie+ run through the whole rule pipeline into resolvedRegels, so its condition,
     *  value, geldigheid and type-checks are the standard ones. The defaults pass then LIFTS it (and the base
     *  rule) into a {@link dev.uchar.regelspraak.ir.PrioritizedDefaults} group, removing it from
     *  resolvedRegels. */
    private void exceptionVersions(UitzonderingRegelContext u) {
        ruleVersions(DefinitionResolver.src(u.regelNaam()), u.regelVersie(), statuteAnnotations(u), u.getStart().getLine());
    }

    /** The shared §13.4.2 version pipeline, over a rule's NAME + versions (a top-level regel, a beslistabel row,
     *  or a §4 exception block). Each §13.4.2.3 version becomes a distinct {@link ResolvedRegel}. */
    private void ruleVersions(String naam, List<RegelVersieContext> versions, List<String> sourceAnn, int line) {
        // §Bron: the statuutannotaties directly above the rule header, carried onto every version's ResolvedRegel.
        currentSourceAnnotations = sourceAnn;
        // §13.4.2.5 validity per version, computed up front so §4.2's non-overlap
        // invariant can be checked across the rule's versions. A version whose validity
        // bound is malformed is null here (already diagnosed) and is not emitted.
        List<Geldigheid> validities = new ArrayList<>();
        for (RegelVersieContext v : versions) validities.add(validityOf(v.versie()));
        if (versions.size() > 1) reportOverlap(naam, versions, validities, line);
        for (int i = 0; i < versions.size(); i++) {
            Geldigheid geldigheid = validities.get(i);
            if (geldigheid == null) continue; // malformed validity bound — already reported
            RegelVersieContext v = versions.get(i);
            RegelSpraakRegelContext rsr = v.regelSpraakRegel();
            // Each §13.4.2.3 version is a distinct rule selected by Rekendatum (§4.2):
            // when a rule has several, give each a unique name.
            String versionName = versions.size() > 1 ? naam + " #" + (i + 1) : naam;
            builder.setSubject(ruleSubjectOf(rsr.resultaatDeel())); // so "zijn …" resolves against the rule subject
            builder.setVariables(rsr.variabeleDeel());          // §11 capture "Daarbij geldt:" vars in BOTH forms (clears per rule)
            Conditie voorwaarde = rsr.voorwaardeDeel() != null ? buildCondition(rsr.voorwaardeDeel()) : null;
            // The set fence on the CONDITION, here rather than in the naloop: this is the one place every
            // resultaatdeel kind's voorwaarde passes through, so one call covers the constructs that do
            // not become a ResolvedRegel (verdeling, objectcreatie, feitcreatie, dagsoort, startpunt)
            // as well as the ones that do. The naloop then walks the VALUES alone, so nothing is said twice.
            checkSetRead(voorwaarde, builder.subject());
            resultaat(naam, versionName, rsr.resultaatDeel(), rsr.voorwaardeDeel(), voorwaarde, geldigheid);
            conditions(rsr);   // exhaustive condition type-check (§5), unchanged
        }
    }

    /** §12 Beslistabel (Ch.12 decision tables): desugar the captured inline pipe-table body — one ordinary regel
     *  per (data-row × conclusie-column) via {@link DecisionTableDesugar} — re-parse each synthetic regel and process
     *  it through {@link #ruleVersions}, so a beslistabel reuses the WHOLE rule pipeline and BOTH back-ends get its
     *  rows for free, with zero new IR. A header-only shell (the pre-body corpus, or a table with no data rows) keeps
     *  the honest WARNING — restoring the never-silently-dropped invariant. */
    private void decisionTables(RegelSpraakDocumentContext doc) {
        for (BeslistabelContext b : DefinitionResolver.allDecisionTables(doc)) {
            String naam = DefinitionResolver.src(b.beslistabelNaam());
            // Split each captured row on '|', dropping the markdown '---' separator row(s).
            List<List<String>> grid = new ArrayList<>();
            for (TerminalNode row : b.TABEL_RIJ()) {
                List<String> cells = splitRow(row.getText());
                if (!isSeparationRow(cells)) grid.add(cells);
            }
            List<String> rows = new ArrayList<>();
            rowRules.put(b, rows);
            if (grid.size() < 2) { // need a header row + ≥1 data row to produce anything
                model.diagnostics.add(new Diagnostic(Severity.WARNING,
                        "beslistabel '" + naam + "' heeft geen tabelinhoud (alleen een kop) — wordt niet naar IR/DRL omgezet",
                        b.getStart().getLine()));
                continue;
            }
            String valid = b.versie() != null ? DefinitionResolver.src(b.versie().versieGeldigheid()) : "altijd";
            List<String> headers = grid.get(0);
            List<List<String>> data = grid.subList(1, grid.size());
            int line = b.getStart().getLine();
            // a row the desugar cannot faithfully render (kenmerk "onwaar" conclusie, >1 OR-cel in an EN-rij) is
            // surfaced as a WARNING — never silently dropped.
            java.util.function.Consumer<String> warn = msg -> model.diagnostics.add(
                    new Diagnostic(Severity.WARNING, "beslistabel '" + naam + "': " + msg, line));
            // The table's // Bron: annotations go onto EVERY rule it desugars to — the multi-version precedent of
            // IR-SPEC ch. 3.1 (one comment block above a shared header, stamped per flattened entry). Each row is
            // re-parsed in a stream of its own, where no comment stands above it, so the annotations are read here,
            // off the table in the main stream, or they are lost.
            List<String> tableAnnotations = statuteAnnotations(b);
            List<String> duplicates = new ArrayList<>();
            rowTableLine = line;
            for (String ruleSrc : DecisionTableDesugar.desugar(naam, valid, headers, data, warn)) {
                RegelContext rc = parseRule(ruleSrc, line);
                if (rc == null) continue;
                String rowName = DefinitionResolver.src(rc.regelNaam());
                if (rows.contains(rowName)) { if (!duplicates.contains(rowName)) duplicates.add(rowName); }
                else rows.add(rowName);
                ruleVersions(rowName, rc.regelVersie(), tableAnnotations, rc.getStart().getLine());
            }
            rowTableLine = 0;
            // A row's rule is named after the table and the row's number (its first cell), so rows without a
            // number, or with the same one, share a name: two rule forms of one name, which the verifier refuses
            // in the rules array (WF-020), one sentence per name
            if (!duplicates.isEmpty())
                model.diagnostics.add(new Diagnostic(Severity.ERROR, "beslistabel '" + naam + "': " + String.join("; ",
                        duplicates.stream().map(n -> String.format("duplicate name '%s' in %s", n, "rules")).toList()),
                        line));
        }
    }

    /** Split a captured markdown table row into trimmed cells: strip the surrounding pipes/whitespace (a continuation
     *  row carries a leading newline), then split on each TOP-LEVEL '|'. The split is QUOTE-AWARE — a '|' inside a
     *  double-quoted tekstwaarde ("…", §5.4) or a single-quoted enumeratiewaarde ('…', §13.2.14), both preserved
     *  verbatim by the lexer inside TABEL_MODE, is NOT a cell boundary. The empty col-0 header and the numbering cell
     *  are interior, so only the outer pipes are removed. */
    private static List<String> splitRow(String row) {
        String s = row.strip();
        if (s.startsWith("|")) s = s.substring(1);
        // Only a BALANCED quote char (even count) opens spans — so a stray apostrophe (a §13.2 ' that is NOT an
        // enumeratiewaarde delimiter, e.g. in "1's") stays a literal char and never swallows the following pipes.
        boolean dq = even(s, '"'), sq = even(s, '\'');
        if (s.endsWith("|") && !endsInQuote(s, dq, sq)) s = s.substring(0, s.length() - 1);
        List<String> cells = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        char quote = 0;   // 0 = outside any quote; '"' or '\'' = the char that opened the current span
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (quote == 0 && ((c == '"' && dq) || (c == '\'' && sq))) quote = c;
            else if (quote == c) quote = 0;
            if (c == '|' && quote == 0) { cells.add(cur.toString().trim()); cur.setLength(0); }
            else cur.append(c);
        }
        cells.add(cur.toString().trim());
        return cells;
    }
    /** True when {@code q} occurs an even number of times in {@code s} (so its spans are balanced and may be tracked
     *  as quotes; an odd count means an unbalanced ' / " that must be treated as an ordinary character). */
    static boolean even(String s, char q) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == q) n++;
        return (n & 1) == 0;
    }
    /** True when the final '|' of {@code s} sits INSIDE a (balanced) quoted span, so it is part of a value rather than
     *  the row's trailing edge pipe. */
    private static boolean endsInQuote(String s, boolean dq, boolean sq) {
        char quote = 0;
        for (int i = 0; i < s.length() - 1; i++) {
            char c = s.charAt(i);
            if (quote == 0 && ((c == '"' && dq) || (c == '\'' && sq))) quote = c;
            else if (quote == c) quote = 0;
        }
        return quote != 0;
    }

    /** A markdown separator row ("| --- | :--: | --- |"): every non-empty cell is dashes/colons and at least one is a
     *  dash run. Such rows are layout, not data, and are dropped before desugaring. */
    private static boolean isSeparationRow(List<String> cells) {
        boolean streep = false;
        for (String c : cells) {
            if (c.isEmpty()) continue;
            if (!c.matches("[:\\-\\s]+")) return false;
            if (c.indexOf('-') >= 0) streep = true;
        }
        return streep;
    }

    /** Parse a synthetic regel source (from {@link DecisionTableDesugar}) into a {@link RegelContext}. A syntax error
     *  lands in the model's diagnostics tagged to the beslistabel's line and yields null (the row is skipped, never
     *  silently mis-rendered). */
    private RegelContext parseRule(String src, int line) {
        RegelSpraakLexer lexer = new RegelSpraakLexer(CharStreams.fromString(src));
        RegelSpraak parser = new RegelSpraak(new CommonTokenStream(lexer));
        boolean[] fout = {false};
        BaseErrorListener listener = new BaseErrorListener() {
            @Override public void syntaxError(Recognizer<?, ?> r, Object sym, int l, int col,
                                              String msg, RecognitionException e) {
                fout[0] = true;
                model.diagnostics.add(new Diagnostic(Severity.ERROR,
                        "beslistabel-desugar: syntaxfout in gegenereerde regel: " + msg, line));
            }
        };
        lexer.removeErrorListeners(); lexer.addErrorListener(listener);
        parser.removeErrorListeners(); parser.addErrorListener(listener);
        RegelContext rc = parser.regel();
        return fout[0] ? null : rc;
    }

    /** §4.2: the validity periods of a rule's versions must not overlap —
     *  otherwise more than one version applies at a Rekendatum (and, writing the same
     *  attribute, they ping-pong in the engine). Flag the first intersecting pair, in the verifier's WF-021
     *  sentence: the versions of one kind reach one form of one wire array, whose versions the verifier numbers
     *  "<name> #<k>" by their place among the written ones. A day-kind definition is no form WF-021 compares, and
     *  versions of two kinds reach two arrays, so those keep this frontend's words. */
    private void reportOverlap(String naam, List<RegelVersieContext> versions, List<Geldigheid> validities, int line) {
        List<String> rows = versions.stream()
                .map(v -> v.regelSpraakRegel() == null ? null : wireRow(v.regelSpraakRegel().resultaatDeel())).toList();
        boolean oneRow = rows.get(0) != null && rows.stream().allMatch(rows.get(0)::equals);
        for (int i = 0; i < validities.size(); i++)
            for (int j = i + 1; j < validities.size(); j++) {
                Geldigheid a = validities.get(i), b = validities.get(j);
                if (a != null && b != null && overlaps(a, b)) {
                    model.diagnostics.add(new Diagnostic(Severity.ERROR, oneRow
                            ? "regel '" + naam + "': validity windows of versions '" + naam + " #"
                                    + writtenPlace(validities, i) + "' and '" + naam + " #" + writtenPlace(validities, j)
                                    + "' overlap (" + rows.get(0) + ")"
                            : "geldigheidsperioden van regelversies overlappen (regel '" + naam + "')", line));
                    return;
                }
            }
    }

    /** The number the verifier gives version {@code i}: its place among the versions with a validity, the ones
     *  written (a version whose bound is malformed is not). */
    private static int writtenPlace(List<Geldigheid> validities, int i) {
        int k = 0;
        for (int j = 0; j <= i; j++) if (validities.get(j) != null) k++;
        return k;
    }

    /** The wire array a version of this kind reaches, as WF-021 names it; null for a day-kind definition, which no
     *  rule-form array carries. */
    private static String wireRow(ResultaatDeelContext rd) {
        if (rd == null) return null;
        if (rd.gelijkstelling() != null || rd.initialisatie() != null || rd.kenmerkToekenning() != null
                || rd.startpuntBepaling() != null) return "rules";
        if (rd.objectCreatie() != null) return "objectCreations";
        if (rd.verdeling() != null) return "allocations";
        if (rd.feitCreatie() != null) return "factCreations";
        if (rd.consistentieRegel() != null)
            return rd.consistentieRegel().uniciteitsControle() != null ? "uniquenesses" : "consistencies";
        return null;
    }
    /** Two inclusive windows intersect unless one's upper bound is strictly before the
     *  other's lower bound; a null bound is an open end (±∞), so ALTIJD overlaps everything. */
    private static boolean overlaps(Geldigheid a, Geldigheid b) {
        return !beyond(a.tot(), b.vanaf()) && !beyond(b.tot(), a.vanaf());
    }
    private static boolean beyond(LocalDate tot, LocalDate vanaf) {
        return tot != null && vanaf != null && tot.isBefore(vanaf);
    }

    /** §13.4.2.5 version validity, or null when a present bound is not a valid date/year
     *  (already diagnosed): such a version is dropped, not silently made always-active.
     *  "geldig altijd" is open both ends; a year widens to the whole year (vanaf -> 1 Jan,
     *  t/m -> 31 Dec), t/m being inclusive. */
    private Geldigheid validityOf(VersieContext v) {
        if (v == null || v.versieGeldigheid() == null) return Geldigheid.ALTIJD;
        VersieGeldigheidContext g = v.versieGeldigheid();
        if (g.ALTIJD() != null) return Geldigheid.ALTIJD;
        int line = v.getStart().getLine();
        java.util.List<DatumOfJaarContext> data = g.datumOfJaar();
        LocalDate vanaf = null, tot = null;
        if (g.VANAF() != null) {
            vanaf = boundOrReport(data.get(0), true, line);
            if (vanaf == null) return null;
            if (g.TM() != null && data.size() > 1) {
                tot = boundOrReport(data.get(1), false, line);
                if (tot == null) return null;
            }
        } else if (g.TM() != null && !data.isEmpty()) {
            tot = boundOrReport(data.get(0), false, line);
            if (tot == null) return null;
        }
        return new Geldigheid(vanaf, tot);
    }

    /** Resolve a validity bound, or report an ERROR and return null when it is neither a
     *  valid calendar date (dd-mm-jjjj) nor a four-digit year (§13.2.24), or when it carries a
     *  time of day. Both defects are reported together, so fixing one never uncovers the other.
     *  A beslistabel re-parses its header validity once per data row; the contains-guard keeps
     *  that to the one report the author's one bound deserves. */
    private LocalDate boundOrReport(DatumOfJaarContext d, boolean under, int line) {
        LocalDate b = limit(d, under);
        if (b == null)
            reportOnce(new Diagnostic(Severity.ERROR,
                    "ongeldige versiegeldigheidsgrens '" + (d != null ? src(d) : "")
                            + "' (verwacht een datum dd-mm-jjjj of een jaartal van vier cijfers)", line));
        // The DATE token admits a time of day, but Geldigheid holds LocalDates: there is no
        // carrier for it, and reading only the date part used to drop the time silently.
        boolean time = d != null && d.DATE() != null && d.DATE().getText().contains(":");
        if (time)
            reportOnce(new Diagnostic(Severity.ERROR,
                    "versiegeldigheidsgrens '" + src(d) + "' draagt een tijdcomponent; de geldigheid "
                            + "is datumgranulair (verwacht een datum dd-mm-jjjj of een jaartal van vier cijfers)", line));
        return time ? null : b;
    }

    private void reportOnce(Diagnostic fout) {
        if (!model.diagnostics.contains(fout)) model.diagnostics.add(fout);
    }

    /* The whole DATE token, never a find(): the token takes DIGIT+ for day and month, and a search
     * read 2024-01-2026 as 24-01-2026. The optional time part is matched so the date part of a
     * time-bearing bound can still be judged; boundOrReport refuses the time itself. */
    private static final java.util.regex.Pattern BOUND_DATE = java.util.regex.Pattern.compile(
            "(?:dd\\. +)?(\\d{1,2})-(\\d{1,2})-(\\d{4})(?: +\\d{2}:\\d{2}:\\d{2}\\.\\d{3})?");

    /** The date part of a datumOfJaar, or null when it is not a real calendar date or a
     *  four-digit year. Never throws — an out-of-range day/month/year yields null. */
    private static LocalDate limit(DatumOfJaarContext d, boolean under) {
        if (d == null) return null;
        if (d.DATE() != null) {
            java.util.regex.Matcher m = BOUND_DATE.matcher(d.DATE().getText());
            if (!m.matches()) return null;
            try {
                return LocalDate.of(Integer.parseInt(m.group(3)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1)));
            } catch (java.time.DateTimeException e) { return null; }
        }
        // §13.2.24: a bare year is exactly four digits — reject a decimal/fraction/signed/
        // out-of-range NUMBER the grammar's generic token would otherwise admit.
        if (d.NUMBER() != null && d.NUMBER().getText().matches("\\d{4}")) {
            try {
                int year = Integer.parseInt(d.NUMBER().getText());
                return under ? LocalDate.of(year, 1, 1) : LocalDate.of(year, 12, 31);
            } catch (NumberFormatException | java.time.DateTimeException e) { return null; }
        }
        return null;
    }

    /** A domain-typed attribute/parameter must reference a declared domain (§13.3.4). */
    private void domainReferences(RegelSpraakDocumentContext doc) {
        // OWN declarations on purpose: this ITERATES rather than resolves. Its subject is the
        // declaration itself, and a chain-closed view would report one inherited attribute once per
        // descendant — the same duplicate the verifier's declaration-walking checks avoid.
        model.objecttypen.values().forEach(ot -> ot.attributen.forEach((naam, dt) -> reportUnknownDomain(dt, naam)));
        model.parameters.values().forEach(p -> reportUnknownDomain(p.datatype(), p.naam()));
    }
    private void reportUnknownDomain(Datatype dt, String where) {
        if (dt instanceof Datatype.Onbekend o && o.reason().startsWith("domein ")) {
            model.diagnostics.add(new Diagnostic(Severity.ERROR,
                    "ongedefinieerd " + o.reason() + " (gebruikt door '" + where + "')", 0));
        }
    }

    /** Resolve an assignment resultaatdeel into a {@link ResolvedRegel} (the transpiler IR)
     *  and type-check the value against the target (§6 toekenning). Every resultaatdeel kind
     *  carries the version's {@code geldigheid} and its per-version {@code versionName} — the
     *  other kinds gained both as they joined the IR, so no kind is left without a validity guard. */
    private void resultaat(String naam, String versionName, ResultaatDeelContext rd,
                           VoorwaardeDeelContext vd, Conditie voorwaarde, Geldigheid geldigheid) {
        if (rd == null) return;
        if (rd.gelijkstelling() != null) {
            GelijkstellingContext g = rd.gelijkstelling();
            ResolvedRegel.Soort soort = g.WORDT_BEREKEND_ALS() != null
                    ? ResolvedRegel.Soort.BEREKENING : ResolvedRegel.Soort.GELIJKSTELLING;
            register(versionName, soort, g.attribuutVanOnderwerp(), g.expressie(), g.getStart().getLine(), voorwaarde, geldigheid);
        } else if (rd.initialisatie() != null) {
            InitialisatieContext i = rd.initialisatie();
            register(versionName, ResolvedRegel.Soort.INITIALISATIE, i.attribuutVanOnderwerp(),
                       i.expressie(), i.getStart().getLine(), voorwaarde, geldigheid);
        } else if (rd.kenmerkToekenning() != null) {
            kenmerk(versionName, rd.kenmerkToekenning(), voorwaarde, geldigheid);
        } else if (rd.objectCreatie() != null) {
            objectCreation(versionName, rd.objectCreatie(), voorwaarde, geldigheid);
        } else if (rd.verdeling() != null) {
            verdeling(versionName, rd.verdeling(), voorwaarde, geldigheid);
        } else if (rd.consistentieRegel() != null) {
            consistency(versionName, rd.consistentieRegel(), geldigheid);
        } else if (rd.feitCreatie() != null) {
            feitCreatie(versionName, rd.feitCreatie(), vd, voorwaarde, geldigheid);
        } else if (rd.dagsoortResultaat() != null) {
            // §9.8 dagsoortdefinitie ("Een dag is een <dagsoort> indien <voorwaarde over de dag>"): lift the
            // (dagsoort name, condition-over-the-day) into the IR. The voorwaardendeel was already resolved into
            // `voorwaarde` — its subject is "de dag" and its leaves are date-component tests, so both transpilers
            // can compile it to a per-date boolean predicate (§8.1.5 controls + §6 dagsoort durations consume it).
            String day = src(rd.dagsoortResultaat().kaleNaam()).trim();
            if (voorwaarde == null)
                model.diagnostics.add(new Diagnostic(Severity.WARNING,
                        "dagsoortdefinitie '" + day + "' mist een voorwaarde (regel '" + naam + "')", rd.getStart().getLine()));
            else {
                // No executor consults a day-kind window, and the predicate compilers key on the dagsoort NAME, so a
                // second definitie (e.g. per §13.4.2.5 version window) is not version-dispatched: the verifier refuses
                // both (WF-031), and so does the frontend, in its words
                String head = "dagsoortdefinitie '" + day + "' (regel '" + naam + "'): ";
                if (geldigheid.vanaf() != null || geldigheid.tot() != null)
                    model.diagnostics.add(new Diagnostic(Severity.ERROR, head + "day-kind definition with a bounded "
                            + "validity window; must be always (from and to both absent)", rd.getStart().getLine()));
                model.resolvedDayKindDefinitions.stream().filter(earlier -> earlier.naam().equalsIgnoreCase(day)).findFirst()
                        .ifPresent(earlier -> model.diagnostics.add(new Diagnostic(Severity.ERROR, head
                                + "more than one definition for day kind '" + earlier.naam() + "'", rd.getStart().getLine())));
                dayKindPurity(voorwaarde, head, rd.getStart().getLine());
                model.resolvedDayKindDefinitions.add(new dev.uchar.regelspraak.ir.ResolvedDayKindDefinition(day, voorwaarde, geldigheid));
            }
        } else if (rd.startpuntBepaling() != null) {
            startPointDetermination(versionName, rd.startpuntBepaling(), voorwaarde, geldigheid);
        } else {
            // parsed but not yet lifted into the IR. Surface it as a diagnostic
            // rather than letting the rule silently disappear from the model.
            model.diagnostics.add(new Diagnostic(Severity.WARNING,
                    "resultaatdeel 'resultaatdeel' wordt nog niet naar IR/DRL omgezet (regel '" + naam + "')",
                    rd.getStart().getLine()));
        }
    }

    /** WF-031's purity clause, producer side: a day kind classifies a bare day before the rule graph runs, with no
     *  fact base, so its condition reads dates, date components, easterSunday, literals, parameters and other day
     *  kinds only. Before this check a condition that read an attribute, a session value or a rule's state went to
     *  disk with {@code diagnostics: []}, and the verifier refused the document. The walk and the words are the
     *  verifier's own scan of a day-kind condition: one ERROR per derived read, in walk order, at the line of the read,
     *  and no descent below a read it refuses. */
    private void dayKindPurity(Conditie voorwaarde, String head, int regel) {
        dayKindCondition(voorwaarde, (sentence, bron) -> model.diagnostics.add(new Diagnostic(Severity.ERROR, head + sentence,
                bron != null && bron.line() > 0 ? bron.line() : regel)));
    }

    private static String dayKindImpure(String soort, String naam) {
        return String.format("day-kind condition reads %s '%s' — a day-kind condition references only dates, "
                + "date-components, easterSunday, literals, parameters and other day kinds, never derived object "
                + "state (an attribute or characteristic read)", soort, naam == null ? "?" : naam);
    }

    private static void dayKindExpr(Expr e, java.util.function.BiConsumer<String, Bron> report) {
        if (e == null) return;
        switch (e) {
            case Expr.AttribuutRef a -> report.accept(dayKindImpure("attribute", a.attribuut()), a.bron());
            // the bound day is the day itself; a session value is one value for every day of a session
            case Expr.Constante c -> {
                if (c.naam() != null && dev.uchar.regelspraak.ir.Spelling.CONTEXT_VALUE_NAME.rows().containsKey(c.naam()))
                    report.accept(String.format("day-kind condition reads the session value '%s' — a day kind "
                            + "classifies a bare date at stratum 0 (ch. 5.4), so its predicate is a function of THE "
                            + "DAY alone; the bound day (BoundDay) is admitted, a ContextValue naming a session value "
                            + "is refused", dev.uchar.regelspraak.ir.Spelling.CONTEXT_VALUE_NAME.spelling(c.naam())),
                            c.bron());
            }
            case Expr.DimensionCellRead dc -> dayKindExpr(dc.attribuut(), report);
            case Expr.Binair b -> { dayKindExpr(b.links(), report); dayKindExpr(b.rechts(), report); }
            case Expr.Afgerond a -> dayKindExpr(a.basis(), report);
            case Expr.Begrensd b -> {
                dayKindExpr(b.basis(), report); dayKindExpr(b.minimum(), report); dayKindExpr(b.maximum(), report);
            }
            case Expr.TextComposition t -> t.parts().forEach(d -> {
                if (d instanceof Expr.TextComposition.EmbeddedExpression i) dayKindExpr(i.expr(), report);
            });
            case Expr.FunctionCall f -> {
                f.arguments().forEach(x -> dayKindExpr(x, report));
                dayKindExpr(f.emptyFallback(), report);
                dayKindCondition(f.filter(), report);
            }
            case Expr.IfThenElse k -> {
                dayKindCondition(k.condition(), report); dayKindExpr(k.then(), report); dayKindExpr(k.otherwise(), report);
            }
            case Expr.Let l -> { dayKindExpr(l.value(), report); dayKindExpr(l.body(), report); }
            case Expr.InstanceSetUnion u -> u.branches().forEach(t -> {
                report.accept(dayKindImpure("instance population",
                        t.elementType() == null ? null : t.elementType().naam), u.bron());
                dayKindCondition(t.filter(), report);
            });
            case Expr.Literal ignored -> { }
            case Expr.ParameterRef ignored -> { }
            case Expr.VariableRef ignored -> { }
            case Expr.BinderRef ignored -> { }
            case Expr.Unresolved ignored -> { }
        }
    }

    private static void dayKindCondition(Conditie c, java.util.function.BiConsumer<String, Bron> report) {
        if (c == null) return;
        switch (c) {
            case Conditie.Vergelijking v -> { dayKindExpr(v.links(), report); dayKindExpr(v.rechts(), report); }
            case Conditie.Predicaat p -> dayKindExpr(p.operand(), report);
            case Conditie.Periode p -> { dayKindExpr(p.vanaf(), report); dayKindExpr(p.tot(), report); }
            case Conditie.Samengesteld s -> s.onderdelen().forEach(o -> dayKindCondition(o, report));
            case Conditie.Kenmerk k -> report.accept(dayKindImpure("characteristic", k.kenmerk()), k.bron());
            case Conditie.Rollidmaatschap r -> report.accept(dayKindImpure("role membership", r.rol()), r.bron());
            case Conditie.FarRoleMembership r ->
                    report.accept(dayKindImpure("role membership", r.backref()), r.bron());
            case Conditie.RuleStatusCheck r -> report.accept(dayKindImpure("rule-version state", r.regel()), r.bron());
            case Conditie.QuantifierFold k -> report.accept(dayKindImpure("instance quantification",
                    k.elementObjectType() == null ? null : k.elementObjectType().naam), k.bron());
            case Conditie.Unresolved ignored -> { }
        }
    }

    /** §13.4.8 consistentieregel (single-criterion form): "&lt;expressie&gt; moet
     *  &lt;operator&gt;". The assertion is built with the same operator machinery as a
     *  voorwaarde (conditionFromToplevel), so the operand types and operator legality are the
     *  §5 ones; the subject is the object type the asserted expression is about. Compound
     *  criteria and uniciteit have since been lifted as well — the former into a
     *  {@link Conditie.Samengesteld} assertion on the same
     *  {@link dev.uchar.regelspraak.ir.ResolvedConsistency}, the latter into
     *  {@link dev.uchar.regelspraak.ir.ResolvedUniqueness} / {@link ResolvedUniquenessUnion};
     *  what neither path can carry keeps a WARNING. */
    private void consistency(String naam, ConsistentieRegelContext cr, Geldigheid geldigheid) {
        EnkelvoudigeConsistentieRegelContext e = cr.enkelvoudigeConsistentieRegel();
        if (e != null && e.expressie() != null && e.toplevelOperator() != null) {
            Expr lhs = builder.build(e.expressie());
            // The voorwaarde's operator machinery MINUS the ch. 4.5 quantifier fold: a Kwantorvouw has no
            // violation rule on this carrier (transpileerbaar declines it), so folding a quantified
            // assertion here would trade the set fence's ERROR for the "nog niet omgezet" WARNING below and drop
            // the check from the wire. The set reading keeps its fence until the §13.4.8 consistentieregel
            // gets a quantified form of its own. (The BULLET criteria need no such guard: a samengestelde
            // consistentieregel sets no rule subject, so the fold's range never binds.)
            Conditie assertie = checkWholePeriod(conditionFromToplevelBare(lhs, e.toplevelOperator(), bron(e)),
                    wholePeriod(e.toplevelOperator().gehelePeriodevergelijking()), bron(e));
            Objecttype subject = lhs instanceof Expr.AttribuutRef a ? a.objecttype() : null;
            if (subject != null && transpileerbaar(assertie)) {
                model.consistenties.add(new dev.uchar.regelspraak.ir.ResolvedConsistency(naam, subject, assertie, bron(cr), cr.getStart().getLine(), geldigheid));
                return;
            }
        }
        if (cr.uniciteitsControle() != null && uniqueness(naam, cr.uniciteitsControle(), geldigheid)) return;
        // §13.4.8 samengestelde consistentieregel — quantified bullet criteria, in either the
        // subject-led ("<onderwerp> moet voldoen aan alle volgende criteria: …") or the impersonal
        // ("er moet worden voldaan aan …") form. Build a Conditie.Samengesteld over the criteria; the
        // subject is the type the criteria are about (the first criterion's left operand).
        ConsistentieKwantificatieContext kw = null;
        SamengesteldCriteriumOnderdeelContext crit = null;
        if (cr.samengesteldeConsistentie() != null) {
            kw = cr.samengesteldeConsistentie().consistentieKwantificatie();
            crit = cr.samengesteldeConsistentie().samengesteldCriteriumOnderdeel();
        } else if (cr.toplevelSamengesteldCriterium() != null) {
            kw = cr.toplevelSamengesteldCriterium().consistentieKwantificatie();   // null for the single "volgend criterium"
            crit = cr.toplevelSamengesteldCriterium().samengesteldCriteriumOnderdeel();
        }
        if (crit != null) {
            Conditie assertie = buildCriteria(kw, crit, bron(cr));
            Objecttype subject = subjectAssertion(assertie);
            // All criteria must be about the SAME subject: the rule lowers to one subject pattern, so a
            // criterion over a different object type would emit that type's attribute on the wrong fact
            // (an uncompilable property that sinks the whole DRL package). Mixed subjects -> WARNING.
            if (subject != null && transpileerbaar(assertie) && oneSubject(assertie, subject)) {
                model.consistenties.add(new dev.uchar.regelspraak.ir.ResolvedConsistency(naam, subject, assertie, bron(cr), cr.getStart().getLine(), geldigheid));
                return;
            }
        }
        // A membership criterion ("moet een <kenmerk|rol> zijn"), elfproef /
        // numeriek-met-exact, or a computed/unresolved subject: forms whose "inconsistent" we cannot
        // express as a faithful violation rule. (The
        // cross-type "verenigd met" uniqueness left this list when ResolvedUniquenessUnion landed.)
        // Surface the gap as a WARNING rather than lift it into a silent inert rule (which would
        // both drop the check and lose the diagnostic the §13.4.7/.11/.12 path still gives).
        model.diagnostics.add(new Diagnostic(Severity.WARNING,
                "consistentieregel wordt nog niet (volledig) naar IR/DRL omgezet (regel '" + naam + "')",
                cr.getStart().getLine()));
    }

    /** §8.1.6 uniciteitscontrole — either "de &lt;attribuut&gt; van alle &lt;Type&gt; moeten uniek zijn"
     *  or a §5.7 concatenatie "de concatenatie van &lt;attribuut&gt; en … van alle &lt;Type&gt; …". Resolves
     *  the object type (by name, declared meervoudsvorm, or regular plural) and the key attribute(s)
     *  over which the value — or the COMBINATION of values — must be unique, into a
     *  {@link dev.uchar.regelspraak.ir.ResolvedUniqueness}. The cross-type "verenigd met" union
     *  and the parent-scoped form ("… van alle X van &lt;Y&gt;") have since been lifted too — the union into
     *  a {@link ResolvedUniquenessUnion}, the scoped one into a ResolvedUniqueness carrying its scope role.
     *  Returns false — leaving the WARNING — for a source that does not resolve, a union whose sources
     *  disagree on key arity or datatype, and a key with more than one temporal component; the transpiler
     *  then never silently drops or mis-checks. Returns true for a scope refused with an ERROR, which is then
     *  the rule's one message. */
    /** The count of temporal (attribuutGranulariteit) components in a uniqueness key list. */
    private static long countTemporal(Objecttype type, java.util.List<String> keys) {
        return keys.stream().filter(k -> type.granulariteitVan(k) != null).count();
    }

    private boolean uniqueness(String naam, UniciteitsControleContext u, Geldigheid geldigheid) {
        // §8.1.6 parent-scoped "de X van alle Y van <parent>": unique within each parent, not globally.
        if (u.vereniging().isEmpty() && u.alleAttribuutVanOnderwerp() != null) {
            Begrensd scoped = parentScoped(naam, u.alleAttribuutVanOnderwerp(), bron(u), geldigheid);
            // the ERROR names the defect; a WARNING that the rule was not lowered would be a second message for it
            if (scoped == REFUSED) return true;
            if (scoped != null) {
                var s = scoped.uniqueness();
                if (countTemporal(s.subject(), s.keys()) > 1) return false;
                model.uniciteiten.add(s); return true;
            }
        }
        ResolvedUniquenessUnion.Bron first = sourceOf(u.alleAttribuutVanOnderwerp(), u.uniciteitConcatenatie());
        if (first == null) return false;
        if (u.vereniging().isEmpty()) {                              // the common single-source form
            if (countTemporal(first.type(), first.keys()) > 1) return false;
            model.uniciteiten.add(new dev.uchar.regelspraak.ir.ResolvedUniqueness(
                    naam, first.type(), first.keys(), null, bron(u), geldigheid));
            return true;
        }
        // §8.1.6 "verenigd met": uniqueness across the UNION of two or more sources' key spaces. Every
        // source must resolve and carry the SAME key arity (the values union positionally — "attributen
        // met hetzelfde datatype"); any unresolved/mismatched source leaves the whole rule a WARNING.
        java.util.List<ResolvedUniquenessUnion.Bron> sources = new ArrayList<>();
        sources.add(first);
        for (VerenigingContext v : u.vereniging()) {
            ResolvedUniquenessUnion.Bron b = sourceOf(v.alleAttribuutVanOnderwerp(), v.uniciteitConcatenatie());
            // §8.1.6: "een vereniging is de combinatie van twee attributen met HETZELFDE datatype". The
            // values join one space, so each source's key parts must match the first's in arity AND
            // datatype-kind; otherwise the union is ill-formed (Tekst vs Numeriek would cross-match
            // under mvel's coercive ==) — leave it a WARNING, like an arity mismatch.
            if (b == null || b.keys().size() != first.keys().size() || !sameDatatypes(first, b))
                return false;
            sources.add(b);
        }
        // §8.1.6 ever-uniqueness (uchar RS ch. 5.9): at most ONE temporal component per key list —
        // in the single form and per union source alike; a second would need the same-moment tuple
        // join, which no reading pins yet. More than one leaves the whole rule a WARNING, like an
        // arity mismatch, so BOTH back-ends stay identically silent on the undefined shape.
        for (ResolvedUniquenessUnion.Bron b : sources) {
            if (countTemporal(b.type(), b.keys()) > 1) return false;
        }
        model.uniciteitUnies.add(new dev.uchar.regelspraak.ir.ResolvedUniquenessUnion(naam, sources, bron(u), geldigheid));
        return true;
    }

    /** One uniqueness source — "de &lt;attribuut&gt; van alle &lt;Type&gt;" or a §5.7 concatenatie — resolved
     *  to its object type and key attribute(s), or null when it does not resolve (an unknown name, a
     *  parent-scoped navigation, a nested-"van" attribute). Exactly one of the two contexts is non-null. */
    private ResolvedUniquenessUnion.Bron sourceOf(AlleAttribuutVanOnderwerpContext a, UniciteitConcatenatieContext c) {
        if (a != null) {
            if (!a.onderwerpKeten().isEmpty()) return null;          // scoped within a parent: not lifted
            Objecttype type = PluralMatcher.findObjectType(model.objecttypen.values(), src(a.kaleNaam(1)));
            if (type == null) return null;
            String attr = PluralMatcher.findAttribute(type, src(a.kaleNaam(0)));  // "DE attr VAN ALLE": no absorbed article
            return attr == null ? null : new ResolvedUniquenessUnion.Bron(type, java.util.List.of(attr));
        }
        if (c != null) {
            if (!c.onderwerpKeten().isEmpty()) return null;
            // CONCATENATIE_VAN kaleNaam (COMMA kaleNaam)* EN kaleNaam VAN ALLE <type>: the last kaleNaam
            // is the type, the rest are the key attributes (each absorbs its leading article — strip it);
            // a nested-"van" part does not resolve, leaving a WARNING rather than a partial key.
            java.util.List<KaleNaamContext> names = c.kaleNaam();
            Objecttype type = PluralMatcher.findObjectType(model.objecttypen.values(), src(names.get(names.size() - 1)));
            if (type == null) return null;
            java.util.List<String> keys = new ArrayList<>();
            for (int i = 0; i < names.size() - 1; i++) {
                String attr = PluralMatcher.findAttribute(type, stripArticle(src(names.get(i))));
                if (attr == null) return null;
                keys.add(attr);
            }
            return new ResolvedUniquenessUnion.Bron(type, keys);
        }
        return null;
    }

    /** The scoped reading of a uniqueness: its record, or none where {@link #parentScoped} refused the scope. */
    private record Begrensd(dev.uchar.regelspraak.ir.ResolvedUniqueness uniqueness) {}

    /** A scope refused with an ERROR. */
    private static final Begrensd REFUSED = new Begrensd(null);

    /** §8.1.6 parent-scoped uniqueness "de X van alle Y van &lt;parent&gt;" — unique within each parent.
     *  Resolves the element Y, its key attribute X, and the reference field on a Y that points at its
     *  scope owner &lt;parent&gt; (so the count is restricted per-parent). Null when it is not the scoped
     *  shape — then the global single-source path applies — or when the scope does not resolve;
     *  {@link #REFUSED} when it refused the scope with an ERROR. */
    private Begrensd parentScoped(String naam, AlleAttribuutVanOnderwerpContext a, Bron bron, Geldigheid g) {
        if (!a.onderwerpKeten().isEmpty()) return null;
        java.util.List<KaleNaamDeelContext> parts = a.kaleNaam(1).kaleNaamDeel();
        if (parts.size() != 2) return null;                          // element + exactly one parent navigation
        // The element is an object type ("alle Ploegen") or, more often when scoped, a role name
        // ("alle deelnemers van de groep") — the role's plural matches via the feittype, not the
        // conservative object-type plural.
        Objecttype elem = PluralMatcher.findObjectType(model.objecttypen.values(), src(parts.get(0)));
        if (elem == null) elem = model.rolDoeltypeNaarSubject(null, src(parts.get(0)));
        if (elem == null) return null;
        String attr = PluralMatcher.findAttribute(elem, src(a.kaleNaam(0)));
        if (attr == null) return null;
        String parentNav = stripArticle(src(parts.get(1)));
        // the written names decide, as for a navigated read (grammar/DIVERGENCES.md, §13.4.1): the parent role, and
        // where that role stands in several relations, the element role; until the fix round of 2026-09-26 the type
        // pair decided, and two relations between the types left the rule unlowered whatever the sentence named
        java.util.List<RoleBindingRef> scope = model.scopeBindings(elem, src(parts.get(0)), parentNav);
        if (scope.isEmpty()) return null;
        // the wire writes the scope as its binding, and written without it the uniqueness would hold over every
        // instance, a document no reader refuses (IR-SPEC ch. 3.6), so the refusal keeps this frontend's words
        if (scope.size() > 1) {
            model.diagnostics.add(new Diagnostic(Severity.ERROR, "uniciteit '" + naam + "' binnen '" + src(parts.get(1))
                    + "' zonder binding (" + scope.size() + " relaties kennen een rol '" + parentNav + "' tegenover "
                    + elem.naam + ", en '" + src(parts.get(0)) + "' noemt geen rol die er precies één aanwijst): zonder "
                    + "binding zou de wire haar lezen als een uniciteit over alle instanties van " + elem.naam
                    + ", en dat is niet wat er staat", bron.line()));
            return REFUSED;
        }
        RoleBindingRef scopeBinding = scope.get(0);
        // a reciprocal fact type has no two-role binding to write the scope as; this ended in the emitter's exception
        // until the fix round of 2026-09-26
        if (model.feittypen.get(scopeBinding.feittype()).wederkerig) {
            model.diagnostics.add(new Diagnostic(Severity.ERROR, "uniciteit '" + naam + "' binnen '" + src(parts.get(1))
                    + "': de relatie die de begrenzing draagt is wederkerig, en een begrenzing over een wederkerig "
                    + "feittype schrijft deze frontend niet (de wire schrijft haar als rolbinding van een feittype met "
                    + "twee rollen)", bron.line()));
            return REFUSED;
        }
        // a scope parent is one instance: over a to-many role the document went to disk without a diagnostic and the
        // verifier refused it (WF-054), so the refusal is that sentence
        if (toMany(scopeBinding)) {
            model.diagnostics.add(new Diagnostic(Severity.ERROR, "uniciteit '" + naam + "' binnen '" + src(parts.get(1))
                    + "': the scope role '" + scopeBinding.rol() + "' is to-many — a scope parent is one instance",
                    bron.line()));
            return REFUSED;
        }
        return new Begrensd(new dev.uchar.regelspraak.ir.ResolvedUniqueness(
                naam, elem, java.util.List.of(attr), scopeBinding.rol(), bron, g, scopeBinding));
    }

    /** §8.1.6: two union sources may be combined only when their key parts share a datatype. Compared
     *  by datatype KIND positionally (Tekst vs Numeriek differ; a finer unit/precision check is carded).
     *  A non-matching pair is ill-formed — the caller leaves it a WARNING. */
    private static boolean sameDatatypes(ResolvedUniquenessUnion.Bron a, ResolvedUniquenessUnion.Bron b) {
        for (int i = 0; i < a.keys().size(); i++) {
            Datatype da = a.type().attribuut(a.keys().get(i));
            Datatype db = b.type().attribuut(b.keys().get(i));
            if (da == null || db == null || da.getClass() != db.getClass()) return false;
            // §8.1.6: the sources combine "attributen met HETZELFDE datatype". For Numeriek the unit IS
            // part of the datatype, and units do not survive into the fact model (values are bare
            // BigDecimals), so a union of a EUR attribute and a jr attribute would cross-match "5 EUR" and
            // "5 jr" as one value — a silently-wrong false duplicate. Require the SAME eenheid (both unitless
            // counts too); a unit mismatch is as ill-formed as a Tekst∪Numeriek one and leaves a WARNING.
            if (da instanceof Datatype.Numeriek numericA && db instanceof Datatype.Numeriek nb
                    && !java.util.Objects.equals(numericA.eenheid(), nb.eenheid())) return false;
        }
        return true;
    }

    /** The single-criterion assertions whose negation the transpiler can emit as a
     *  faithful violation rule: a comparison, or an emptiness predicate, and a compound of them
     *  under any quantifier. Everything else (membership, elfproef, numeriek-met-exact,
     *  unstructured) is left to a WARNING. */
    private static boolean transpileerbaar(Conditie c) {
        if (c instanceof Conditie.Vergelijking) return true;
        if (c instanceof Conditie.Predicaat p)
            return p.soort() == Conditie.PredicateKind.LEEG || p.soort() == Conditie.PredicateKind.GEVULD;
        // A compound is lowerable when every criterion is: under ALLE the violation is the OR of the
        // criteria's violations, under GEEN and the counting kwantoren (ten minste/hoogste/precies) the
        // negation of the count of the criteria that hold against the drempel — both Drools branches carry
        // it since the card aantal-bronarm-en-drools-lader, as ref and the ASP backend did.
        if (c instanceof Conditie.Samengesteld s)
            return !s.onderdelen().isEmpty() && s.onderdelen().stream().allMatch(RuleChecker::transpileerbaar);
        return false;
    }

    /** §13.4.10 verdeling: distribute a source amount over a recipient attribute. The
     *  method (equal shares / proportional / a group-and-remainder scheme) is captured;
     *  only equal shares has a single-rule Drools form (the transpiler marks the rest). */
    private void verdeling(String naam, VerdelingContext v, Conditie voorwaarde, Geldigheid geldigheid) {
        if (v.attribuutVanOnderwerp().size() < 2) return;
        Expr bron = builder.buildAttribute(v.attribuutVanOnderwerp(0));
        Expr doel = builder.buildAttribute(v.attribuutVanOnderwerp(1));
        if (!(bron instanceof Expr.AttribuutRef b) || !(doel instanceof Expr.AttribuutRef d)) return;

        // The bare "in gelijke delen" / "naar rato van" names the method directly. The bulleted
        // meervoudigCriterium (§9.7.7) wraps that same method with optional modifiers; only the subset
        // WITHOUT group ordering has a single forward-rule form, so a present verdelenOverGroepen keeps the
        // whole verdeling COMPLEX (a marker). The §9.7.3 maximum (naar-rato only), the §9.7.4 afronding (floor)
        // and the §9.7.5 onverdeelde rest ride on the equal-shares / naar-rato spine.
        VerdelenZonderGroepenContext z = v.verdelenZonderGroepen();
        Integer afrDec = null;
        String restAttr = null, maxAttr = null, groupAttr = null;
        boolean descending = false;
        MeervoudigCriteriumContext mc = v.meervoudigCriterium();
        // The §9.7.3/§9.7.4/§9.7.5 modifiers (maximum, afronding, onverdeelde rest) attach to BOTH the
        // bare-method form and the op-volgorde form (the grammar lets them co-occur with verdelenOverGroepen),
        // so capture them whenever a meervoudigCriterium is present; only the bare-method `z` is branch-specific.
        if (z == null && mc != null) {
            if (mc.verdelenOverGroepen() == null) z = mc.verdelenZonderGroepen();
            if (mc.verdeelAfronding() != null)
                try { afrDec = Integer.valueOf(mc.verdeelAfronding().NUMBER().getText()); }
                catch (NumberFormatException ignore) {}
            if (mc.maximumAanspraak() != null && mc.maximumAanspraak().attribuutMetLidwoord() != null
                    && mc.maximumAanspraak().attribuutMetLidwoord().kaleNaam() != null)
                maxAttr = src(mc.maximumAanspraak().attribuutMetLidwoord().kaleNaam());   // a recipient (doel) attribute
            if (mc.onverdeeldeRest() != null && b.objecttype() != null
                    && builder.buildAttribute(mc.onverdeeldeRest().attribuutVanOnderwerp()) instanceof Expr.AttribuutRef ra
                    && ra.objecttype() == b.objecttype())   // the remainder lives on the verdeler (the source)
                restAttr = ra.attribuut();
            if ((afrDec != null || maxAttr != null) && restAttr == null && b.objecttype() != null)
                // §9.7.3/§9.7.4: a maximum OR an afronding can leave the plafond under-distributed, so the spec
                // REQUIRES an onverdeelde rest. Without a captured one the leftover is silently lost — surface it.
                // Only when the bron resolves (an unresolved-verdeler fragment markers anyway, so warning is noise).
                model.diagnostics.add(new Diagnostic(Severity.WARNING,
                        "verdeling met afronding/maximum zonder herkenbare onverdeelde rest (§9.7.3/§9.7.4): het "
                        + "restant wordt niet vastgelegd (regel '" + naam + "')", v.getStart().getLine()));
        }

        ResolvedVerdeling.Methode methode = ResolvedVerdeling.Methode.COMPLEX;
        String ratio = null;
        if (z != null && z.IN_GELIJKE_DELEN() != null) methode = ResolvedVerdeling.Methode.GELIJKE_DELEN;
        else if (z != null && z.NAAR_RATO_VAN() != null) {
            methode = ResolvedVerdeling.Methode.NAAR_RATO;
            // the BARE weight name (kaleNaam, lidwoord stripped) so it maps straight to a recipient getter,
            // exactly like b.attribuut()/d.attribuut() — "de bruto premie" -> "bruto premie".
            ratio = z.attribuutMetLidwoord() != null && z.attribuutMetLidwoord().kaleNaam() != null
                    ? src(z.attribuutMetLidwoord().kaleNaam()) : null;
        }
        // §9.7.2 op volgorde van: partition the recipients by an ordering attribute and deplete the pot
        // group-by-group; within a tie-group the share is "in gelijke delen" or "naar rato van <gewicht>"
        // (the same role as `rato`, so reuse it). The whole allocation lowers via rsVerdeelOpVolgorde — it
        // stays COMPLEX (a marker) only when the ordering key or the tie weight is missing or non-getalachtig
        // (the helper compareTo-sorts the key and sums the weight, so both must be numeric to lower).
        VerdelenOverGroepenContext og = mc != null ? mc.verdelenOverGroepen() : null;
        if (og != null) {
            descending = og.AFNEMENDE() != null;
            groupAttr = og.attribuutMetLidwoord() != null && og.attribuutMetLidwoord().kaleNaam() != null
                    ? src(og.attribuutMetLidwoord().kaleNaam()) : null;
            CriteriumBijGelijkeVolgordeContext crit = og.criteriumBijGelijkeVolgorde();
            if (crit != null && crit.NAAR_RATO_VAN() != null && crit.attribuutMetLidwoord() != null
                    && crit.attribuutMetLidwoord().kaleNaam() != null)
                ratio = src(crit.attribuutMetLidwoord().kaleNaam());   // gelijke-delen tie leaves rato null
            Datatype groupType = groupAttr != null && d.objecttype() != null ? d.objecttype().attribuut(groupAttr) : null;
            Datatype ratioType = ratio != null && d.objecttype() != null ? d.objecttype().attribuut(ratio) : null;
            boolean tieOk = ratio == null || (ratioType != null && ratioType.isGetalachtig());
            // §9.7.2 the ordering key need only be ORDERABLE (a number or a date) — the helper compareTo-sorts it;
            // the tie WEIGHT stays getalachtig (it is summed). A non-orderable key keeps the verdeling COMPLEX.
            if (groupType != null && groupType.isOrdenbaar() && tieOk)
                methode = ResolvedVerdeling.Methode.OP_VOLGORDE;
        }
        // §9.7.3: a maximale aanspraak is valid ONLY for a naar-rato split ("niet mogelijk bij gelijke verdeling"
        // — a gelijke verdeling met maximum is voor meerdere uitleg vatbaar), i.e. top-level naar rato OR an
        // op-volgorde with a naar-rato tie, and only when it names a getalachtig recipient attribute (else the
        // cap getter would not compile). Otherwise the verdeling falls back to COMPLEX (a marker).
        if (maxAttr != null) {
            Datatype maxType = d.objecttype() != null ? d.objecttype().attribuut(maxAttr) : null;
            boolean ratioTie = methode == ResolvedVerdeling.Methode.NAAR_RATO
                    || (methode == ResolvedVerdeling.Methode.OP_VOLGORDE && ratio != null);
            if (!ratioTie || maxType == null || !maxType.isGetalachtig()) {
                methode = ResolvedVerdeling.Methode.COMPLEX;
                maxAttr = null;
            }
        }
        // §6.1.2 precision, consistent with the ordinary `regel` assignment: a share whose decimal count
        // exceeds the target attribute's must be explicitly rounded (the §9.7.4 afronding). Run it only when
        // the verdeling actually ASSIGNS a share — a COMPLEX marker assigns nothing, a naar-rato whose weight is
        // not getalachtig is lowered to an honest no-op, and EVERY method markers when the join is unresolvable
        // (the transpiler gates all three on join != null), so flagging the precision of a share that is never
        // computed would be a false positive. (No-op for all-integer verdelingen.)
        RegelSpraakModel.RoleBindingRef join = allocationJoin(naam, b, d, v.getStart().getLine());
        boolean joinResolves = join != null;
        if (methode != ResolvedVerdeling.Methode.COMPLEX && joinResolves) {
            Datatype gewType = (methode == ResolvedVerdeling.Methode.NAAR_RATO
                    || methode == ResolvedVerdeling.Methode.OP_VOLGORDE) && ratio != null
                    ? d.objecttype().attribuut(ratio) : null;   // op-volgorde naar-rato tie weighs like NAAR_RATO
            boolean lowers = methode == ResolvedVerdeling.Methode.GELIJKE_DELEN
                    || methode == ResolvedVerdeling.Methode.OP_VOLGORDE     // both lower once the join resolves
                    || (gewType != null && gewType.isGetalachtig());   // mirror the transpiler's lowering gate
            if (lowers) {
                String fout = Typing.allocationAssignment(d.objecttype().attribuut(d.attribuut()),
                        b.objecttype().attribuut(b.attribuut()), gewType, afrDec);
                if (fout != null)
                    model.diagnostics.add(new Diagnostic(Severity.ERROR, "verdeling: " + fout, v.getStart().getLine()));
            }
        }
        model.verdelingen.add(new ResolvedVerdeling(naam, b.objecttype(), b.attribuut(),
                d.objecttype(), d.attribuut(), methode, ratio, maxAttr, afrDec, restAttr,
                groupAttr, descending, voorwaarde, geldigheid, join));
    }

    /** The recipient join of an allocation (IR-SPEC ch. 3.2 {@code connectingRole}), as a navigated read binds
     *  (grammar/DIVERGENCES.md, "§13.4.1 A navigated read binds its written role"): the role the recipient phrase
     *  names ("alle hoofdbergers van de berging") where it names one relation between the two types, else the one
     *  relation between them. Where neither binds, the emitter has no join to write, and the refusal is an ERROR in
     *  the loader's sentence for the document without it. Null as well where a type did not resolve: that
     *  allocation is refused or markered on its own. */
    private RegelSpraakModel.RoleBindingRef allocationJoin(String naam, Expr.AttribuutRef bron, Expr.AttribuutRef doel,
                                                          int line) {
        Objecttype verdeler = bron.objecttype(), recipient = doel.objecttype();
        if (verdeler == null || recipient == null) return null;
        String written = doel.subjectSource() == null ? "" : doel.subjectSource().trim()
                .replaceFirst("^(?i)(alle|de|het|een|elke)\\s+", "");
        for (String kandidaat : RegelSpraakModel.nameCandidates(written)) {
            RegelSpraakModel.RoleBindingRef rb = model.connectingRoleViaNameBinding(verdeler, recipient, kandidaat);
            if (rb != null) return rb;
        }
        RegelSpraakModel.RoleBindingRef rb = model.connectingRoleBinding(verdeler, recipient);
        if (rb != null) return rb;
        String head = RegelSpraakModel.nameCandidates(written).stream().findFirst().orElse(written);
        model.diagnostics.add(new Diagnostic(Severity.ERROR, "verdeling '" + naam + "' zonder verbindende rol ("
                + pairReason(verdeler, recipient, head) + "): missing field 'connectingRole'", line));
        return null;
    }

    /** §13.4.6 objectcreatie: "<subject> heeft <het rol> met <attribuut> gelijk aan <expr>
     *  (, <attribuut> gelijk aan <expr>)*". The first kaleNaam carries "<rol> met
     *  <attribuut>" (the 'met' fold the grammar leaves to the resolver); the rest are
     *  bare attribute names. */
    private void objectCreation(String naam, ObjectCreatieContext oc, Conditie voorwaarde, Geldigheid geldigheid) {
        Objecttype subject = builder.chainObjectType(oc.onderwerpKeten());
        builder.setSubject(subject);
        List<KaleNaamContext> names = oc.kaleNaam();
        List<ExpressieContext> waarden = oc.expressie();
        if (names.isEmpty()) return;

        String[] roleAndAttr = src(names.get(0)).split("(?i)\\s+met\\s+", 2);
        Objecttype createdType = createdTypeOf(oc);
        // the noun that names the created object may name the role it fills: that role is the back-link
        // (IR-SPEC ch. 3.3), where the type pair alone may relate the two twice or, self-typed, ambiguously
        RegelSpraakModel.RoleBindingRef connectingRole =
                model.namedRoleBinding(subject, createdType, stripArticle(roleAndAttr[0]));
        if (connectingRole == null && subject != null && subject == createdType) {
            List<Feittype> blockade = selfTypedLinkBlockade(subject);
            if (!blockade.isEmpty())
                model.diagnostics.add(new Diagnostic(Severity.ERROR, "objectcreatie '" + naam + "': een " + subject.naam
                        + " maakt een " + subject.naam + ", en de zin noemt geen rol die het nieuwe object bindt; dan "
                        + "komt de koppeling uit het typenpaar, en die afleiding koppelt bij een zelfgetypeerd paar "
                        + "niets (RUNTIME-SEMANTICS ch. 5.5). Noem de rol: 'Een " + subject.naam + " heeft een <rol> met "
                        + "…', met als <rol> " + blockade.stream()
                                .map(ft -> ft.rollen.stream().map(r -> "'" + r.naam() + "'")
                                        .collect(java.util.stream.Collectors.joining(" of "))
                                        + " (feittype '" + ft.naam + "')")
                                .collect(java.util.stream.Collectors.joining(", ")),
                        oc.getStart().getLine()));
        }

        List<ResolvedObjectCreation.MemberInit> toek = new ArrayList<>();
        if (roleAndAttr.length == 2 && !waarden.isEmpty())
            toek.add(makeMemberInit(createdType, stripArticle(roleAndAttr[1]), waarden.get(0), oc));
        for (int i = 1; i < names.size() && i < waarden.size(); i++)
            toek.add(makeMemberInit(createdType, stripArticle(src(names.get(i))), waarden.get(i), oc));

        // typeringen §6.5: each ATTRIBUUT met-clause init follows the SAME §6.2/Tabel 61 toekenning rules
        // as a gelijkstelling (family + eenheid + §6.1.2 precision; an Expr.Afgerond value already reports
        // its rounded decimal count, and toekenning itself no-ops on an Onbekend value). A kenmerk-init was
        // already §13.4.6.4-checked in makeMemberInit; an unresolved created type carries no claim: skip.
        if (createdType != null)
            for (int i = 0; i < toek.size(); i++) {
                ResolvedObjectCreation.MemberInit t = toek.get(i);
                Datatype targetType = createdType.attribuut(t.attribuut());
                if (targetType == null) continue;
                // §13.2.14: a member init is a WRITE TARGET too (WF-023 names all three), so the created
                // type's declared datatype is this enumeratiewaarde's domain — promoted in place, like registreer.
                Expr gepromoveerd = ExpressionBuilder.enumAgainstContext(t.waarde(), targetType);
                if (gepromoveerd != t.waarde()) {
                    t = new ResolvedObjectCreation.MemberInit(t.attribuut(), gepromoveerd, t.kenmerk());
                    toek.set(i, t);
                }
                // §13.3.4.2 enum-label membership, like registreer
                checkEnumLabel(targetType, ExpressionBuilder.valueEnumLabel(t.waarde()), oc.getStart().getLine());
                checkSign(targetType, t.waarde(), oc.getStart().getLine());   // §3.3.1 Teken-beperking, like registreer
                String fout = Typing.assignment(targetType, t.waarde().type(),
                        ExpressionBuilder.unitlessNumberLiteral(t.waarde()));
                if (fout != null)
                    model.diagnostics.add(new Diagnostic(Severity.ERROR, "objectcreatie: " + fout,
                            oc.getStart().getLine()));
            }

        model.objectCreaties.add(new ResolvedObjectCreation(naam, createdType, subject, toek, voorwaarde, geldigheid,
                connectingRole));
    }

    /** RUNTIME-SEMANTICS ch. 5.5: without a written role, the back-link of a creation whose subject creates its own
     *  type is derived from the type pair, and for that pair the derivation links over one reciprocal fact type and
     *  over nothing else — a two-role fact type both of whose roles the type fills leaves it no choice between the
     *  roles, and neither do two candidates; ref and the ASP backend then create the object without a link. Returns
     *  the fact types that stop it, in declaration order, or an empty list where the derivation links or where no
     *  fact type relates the type to itself (the linkless creation). */
    private List<Feittype> selfTypedLinkBlockade(Objecttype type) {
        List<Feittype> candidates = new ArrayList<>();
        boolean twoRole = false;
        for (Feittype ft : model.feittypen.values()) {
            if (ft.wederkerig && ft.rollen.size() == 1 && fillsRole(type, ft.rollen.get(0))) candidates.add(ft);
            else if (!ft.wederkerig && ft.rollen.size() == 2 && fillsRole(type, ft.rollen.get(0))
                    && fillsRole(type, ft.rollen.get(1))) {
                candidates.add(ft);
                twoRole = true;
            }
        }
        return twoRole || candidates.size() > 1 ? candidates : List.of();
    }

    /** Whether {@code type} fills {@code rol}: the role's declared filler or one of its subtypes (IR-SPEC ch. 2.6). */
    private boolean fillsRole(Objecttype type, Rol rol) {
        Objecttype filler = model.objecttypeOpNaam(rol.objecttypeNaam());
        return filler != null && type.isEen(filler);
    }

    /** §13.4.6.2 one waardetoekenning of an objectcreatie: the §13.4.6.3 ATTRIBUUT form (generic expressie
     *  RHS, §6.5-type-checked by the caller) or the DISTINCT §13.4.6.4 KENMERK form, whose RHS the EBNF
     *  restricts to the waar|onwaar literal — anything else is an ERROR (it would otherwise lower to a
     *  boolean setter fed a non-boolean: a broken rule at build/fire time). A matched kenmerk stores its
     *  DECLARED name (the match is case-insensitive, the fact property is not). A name that is NEITHER a
     *  declared attribute NOR a kenmerk of the created type is an ERROR too — the old silent skip emitted
     *  a setter on a property that does not exist. */
    private ResolvedObjectCreation.MemberInit makeMemberInit(Objecttype createdType, String naam,
                                                            ExpressieContext valueCtx, ObjectCreatieContext oc) {
        Expr waarde = builder.build(valueCtx);
        if (createdType == null || createdType.attribuut(naam) != null)
            return new ResolvedObjectCreation.MemberInit(naam, waarde);
        // through the chain, exactly as the attribute branch above: an INHERITED kenmerk of the created
        // type is as much a member of it as its own, and reading the own list alone made one sentence's
        // two branches disagree about what the type has (a binding on the left, an ERROR on the right).
        String kenmerk = createdType.kenmerkNaam(naam);
        if (kenmerk != null) {
            // the verifier refuses a value that is typed and no boolean (WF-027); a boolean-typed expression it
            // admits, where §13.4.6.4 demands the literal, so that refusal keeps its own words
            if (!(waarde instanceof Expr.Literal lit && lit.type() instanceof Datatype.Booleaan))
                model.diagnostics.add(new Diagnostic(Severity.ERROR, waarde.type() != null
                        && !waarde.type().isUnknown() && !(waarde.type() instanceof Datatype.Booleaan)
                        ? "kenmerkwaardetoekenning '" + kenmerk + "': characteristic initialisation's value must be "
                                + "a boolean-typed expression"
                        : "kenmerkwaardetoekenning '" + kenmerk + "' vereist waar of onwaar (§13.4.6.4), niet '"
                                + waarde.bron() + "'", oc.getStart().getLine()));
            return new ResolvedObjectCreation.MemberInit(kenmerk, waarde, true);
        }
        // the member goes out as an attribute initialisation, which the verifier refuses in these words (WF-027)
        model.diagnostics.add(new Diagnostic(Severity.ERROR,
                "objectcreatie: '" + naam + "' is not a declared attribute of '" + createdType.naam + "'",
                oc.getStart().getLine()));
        return new ResolvedObjectCreation.MemberInit(naam, waarde);
    }

    private static String stripArticle(String s) { return s.replaceFirst("^(?i)(de|het|een)\\s+", "").trim(); }

    /** §13.4.7 feitcreatie: "Een &lt;roleB&gt; van een &lt;anker&gt; is een &lt;roleC&gt; van
     *  &lt;onderwerpD&gt;" creates an instance of the feittype that {@code roleB} names, relating each
     *  {@code anchor} to the {@code doel} objects that fill {@code roleC} of it (§9.4). The grammar's
     *  greedy kaleNaam splits the RHS at the LAST "van" (the rule's trailing {@code VAN onderwerp}),
     *  so the role part is the FIRST kaleNaam segment and any further segment is a navigation; this
     *  resolver re-splits per the §13.4-names design rather than changing the grammar.
     *  <p>Lifts BOTH the 1-hop shape — {@code onderwerpD} is the anker itself, reached by the single
     *  role {@code roleC} — AND the §9.4 MULTI-HOP shape (a navigation chain on the RHS), whose
     *  intermediate hops become {@link ResolvedFactCreation.PathStep}s walked below. The one hard gate
     *  is that {@code onderwerpD} must be a back-reference to the iterated anker itself (a DIFFERENT
     *  object of the same type reached by a role — "de buurgroep" — keeps an honest WARNING rather
     *  than a silent wrong link); an unresolvable role/onderwerp/cardinaliteit likewise WARNs. */
    private void feitCreatie(String naam, FeitCreatieContext fc, VoorwaardeDeelContext vd, Conditie voorwaarde, Geldigheid geldigheid) {
        String roleB = stripArticle(src(fc.kaleNaam(0)));               // LHS role — the relation being created
        Objecttype anchor = builder.resolveSubject(fc.onderwerp(0));   // the "one" side / iterated subject
        KaleNaamContext rhs = fc.kaleNaam(1);
        java.util.List<KaleNaamDeelContext> parts = rhs.kaleNaamDeel();
        String roleC = stripArticle(src(parts.get(0)));                 // RHS role — the FINAL hop onto the doel
        // onderwerpD must be a back-reference to the iterated anker ITSELF, not a different object of the
        // same type reached by a role: "de groep" lifts, but "de buurgroep" must WARN, not silently
        // reuse the anker's own join. It denotes the anker when its written noun matches the ANKER's own
        // written noun (the anker may itself be named by a role — "een toegewezen reeks punten" —
        // so a type-name-only test is too strict); the strict type match is kept as an additional path.
        String anchorNoun = builder.subjectPrimaryName(fc.onderwerp(0));
        String backNoun = builder.subjectPrimaryName(fc.onderwerp(1));
        boolean directAnchor = anchor != null && backNoun != null
                && ((anchorNoun != null && stripArticle(backNoun).equalsIgnoreCase(stripArticle(anchorNoun)))
                    || model.objecttypeOpNaam(stripArticle(backNoun)) == anchor);

        String reason = null;
        if (anchor == null) reason = "anker-objecttype niet herleidbaar";
        else if (!directAnchor) reason = "doelketen verwijst niet rechtstreeks naar de anker (geen back-reference)";
        else {
            // Walk the §9.4 navigation chain anker -> … -> doel-parent. The written kaleNaamDeel are outside-in
            // (roleC at index 0, the innermost hop last), so the hops from the anker are the segments in REVERSE
            // (index size-1 down to 1); segment 0 (roleC) is the final hop onto the doel. The reference field
            // lives on the many side: a to-many hop keeps it on the child (joined `child(veld == $prev)`), a
            // to-one hop on the parent (joined `child(this == $prev.veld)`) — the side objectcreatie set it on.
            java.util.List<ResolvedFactCreation.PathStep> steps = new ArrayList<>();
            Objecttype cur = anchor;
            for (int i = parts.size() - 1; i >= 1; i--) {
                String role = stripArticle(src(parts.get(i)));
                Objecttype next = model.rolDoeltypeNaarSubject(cur, role);
                if (next == null) { reason = "tussenstap-rol '" + role + "' niet eenduidig te herleiden"; break; }
                ResolvedFactCreation.PathStep step = pathStep(cur, next, role);
                if (step == null) { reason = "referentieveld-zijde voor tussenstap '" + role + "' niet af te leiden"; break; }
                steps.add(step);
                cur = next;
            }
            if (reason == null) {
                Objecttype doel = model.rolDoeltypeNaarSubject(cur, roleC);
                String joinField = model.connectingRoleViaElementName(cur, roleC); // existing roleC relation's field on doel
                String setField  = model.connectingRoleViaElementName(anchor, roleB); // the NEW relation's field on doel -> anker
                if (doel == null || joinField == null || setField == null)
                    reason = "rol of feittype niet eenduidig te herleiden";
                else if (joinField.equalsIgnoreCase(setField))
                    reason = "de nieuwe en de bestaande relatie vallen samen (zelfde referentieveld)";
                // The reference field lives on the MANY side, pointing at the ONE side (§13.3.9.1 cardinaliteit;
                // the convention objectCreatie links by). Both the final join (roleC, from the last hop) and the
                // set (roleB, from the anker) field exist on doel only when doel is the MANY side of BOTH — else
                // the field belongs on the other side and a setter would be rejected or link the wrong way. WARN.
                else if (!(model.naarVeelRol(cur, doel, roleC) && model.naarVeelRol(anchor, doel, roleB)))
                    reason = "referentieveld hoort aan de meervoudige kant; het doel is hier niet de meervoudige kant";
                else {
                    Conditie effective = targetCondition(voorwaarde, vd, anchor, doel, roleC);
                    // uchar symbolic form (WF-028, all-or-nothing): the created relation (anker/doel via
                    // roleB) and the final join relation (cur/doel via roleC), each as a declared-name
                    // binding, plus a RolBinding on every tussenstap. Emitted only when all of that is
                    // uniquely derivable AND the created relation differs from the final hop's; otherwise
                    // the bean-field-only form stays (symbolic fields, and the tussenstap bindings, absent).
                    ElementBindingRef created = model.elementBinding(anchor, roleB);
                    ElementBindingRef join = model.elementBinding(cur, roleC);
                    boolean symbolic = created != null && join != null
                            && !created.feittype().equalsIgnoreCase(join.feittype())
                            && steps.stream().allMatch(s -> s.roleBinding() != null);
                    if (symbolic) {
                        RoleBindingRef path = new RoleBindingRef(join.feittype(), join.elementRole());
                        model.feitCreaties.add(new dev.uchar.regelspraak.ir.ResolvedFactCreation(
                                naam, anchor, doel, setField, joinField, steps, effective, geldigheid,
                                created.feittype(), created.subjectRole(), created.elementRole(), path));
                    } else {
                        List<ResolvedFactCreation.PathStep> plain = new ArrayList<>();
                        for (ResolvedFactCreation.PathStep s : steps)
                            plain.add(new ResolvedFactCreation.PathStep(s.type(), s.veld(), s.viaParentField()));
                        model.feitCreaties.add(new dev.uchar.regelspraak.ir.ResolvedFactCreation(
                                naam, anchor, doel, setField, joinField, plain, effective, geldigheid));
                    }
                    return;
                }
            }
        }
        model.diagnostics.add(new Diagnostic(Severity.WARNING,
                "feitcreatie wordt nog niet naar IR/DRL omgezet (regel '" + naam + "'): " + reason,
                fc.getStart().getLine()));
    }

    /** One §9.4 chain hop cur -&gt; next via {@code role}: which side holds the reference field, and the join
     *  form, or null when undeterminable. The field lives on the MANY side (§13.3.9.1). A TO-MANY hop keeps it
     *  on the CHILD (next) -&gt; {@code next(veld == $prev)}. A TO-ONE hop keeps it on the PARENT (cur) only when
     *  cur is genuinely the many side (the reverse role is meervoudig) or an objectcreatie placed the link on
     *  cur -&gt; {@code next(this == $prev.veld)}; it is on the CHILD when an objectcreatie placed it on next. A
     *  bare one-to-one with NEITHER signal is not derivable from the model — return null (honest WARN), never a
     *  guessed side that may emit an uncompilable wrong-side join. */
    private ResolvedFactCreation.PathStep pathStep(Objecttype cur, Objecttype next, String role) {
        // The uchar RolBinding of a hop names the REACHED (far) role: for a to-many / on-child hop that
        // is the ELEMENT role the written name denotes (elementRoleBinding); for a to-one / on-parent hop
        // it is the role next fills (connectingRoleBinding). Null there withholds the whole symbolic form.
        if (model.naarVeelRol(cur, next, role)) {                            // to-many: field on the child
            String veld = model.connectingRoleViaElementName(cur, role);
            return veld == null ? null
                    : new ResolvedFactCreation.PathStep(next, veld, false, model.elementRoleBinding(cur, role));
        }
        String curRole = model.connectingRoleViaElementName(cur, role);       // cur's own role in this feittype
        boolean curIsMany = curRole != null && model.naarVeelRol(next, cur, curRole); // reverse to-many -> cur is many
        if (curIsMany || createdTo(cur, next)) {                        // field on cur (parent)
            String veld = model.connectingRole(cur, next);
            return veld == null ? null
                    : new ResolvedFactCreation.PathStep(next, veld, true, model.connectingRoleBinding(cur, next));
        }
        if (createdTo(next, cur)) {                                     // 1-1: objectcreatie put it on next
            String veld = model.connectingRoleViaElementName(cur, role);
            return veld == null ? null
                    : new ResolvedFactCreation.PathStep(next, veld, false, model.elementRoleBinding(cur, role));
        }
        return null;                                                        // bare 1-1: side not derivable
    }

    /** True when an objectcreatie creates {@code houder} linked to {@code target} ("Een doelwit heeft een
     *  houder"), so — by the convention objectCreatie sets the reference on the new object — {@code houder}
     *  holds the reference field to {@code target}. Any objectcreatie of the document counts, wherever it stands
     *  ({@link #createdObjects}); until 2026-10-06 only one earlier in the document did, so a feitcreatie
     *  written above the objectcreatie it walks through was dropped with a WARNING. */
    private boolean createdTo(Objecttype houder, Objecttype target) {
        return objectCreations.stream().anyMatch(p -> p[0] == houder && p[1] == target);
    }

    /** The objectcreaties of the document as (created type, subject) pairs, read off every rule version before any
     *  rule resolves, with the resolution {@link #objectCreation} gives both. */
    private final List<Objecttype[]> objectCreations = new ArrayList<>();

    private void createdObjects(RegelSpraakDocumentContext doc) {
        for (ResultaatDeelContext rd : resultParts(doc)) {
            ObjectCreatieContext oc = rd.objectCreatie();
            if (oc == null || oc.kaleNaam().isEmpty()) continue;
            objectCreations.add(new Objecttype[] { createdTypeOf(oc), builder.chainObjectType(oc.onderwerpKeten()) });
        }
    }

    /** The result parts of every rule version of the document, ordinary rules and exception blocks, in document
     *  order — what a pass before the rule loop reads. */
    private static List<ResultaatDeelContext> resultParts(RegelSpraakDocumentContext doc) {
        List<RegelVersieContext> versions = new ArrayList<>();
        for (RegelContext r : DefinitionResolver.allRules(doc)) versions.addAll(r.regelVersie());
        for (UitzonderingRegelContext u : DefinitionResolver.allExceptions(doc)) versions.addAll(u.regelVersie());
        List<ResultaatDeelContext> result = new ArrayList<>();
        for (RegelVersieContext v : versions)
            if (v.regelSpraakRegel() != null && v.regelSpraakRegel().resultaatDeel() != null)
                result.add(v.regelSpraakRegel().resultaatDeel());
        return result;
    }

    /** §9.9: the anchor member every startpuntbepaling declares on its subject, declared before any rule resolves, so
     *  a rule that reads it ("zijn startpunt penningjaar") resolves the same wherever the startpuntbepaling stands.
     *  Until 2026-10-06 {@link #startPointDetermination} declared it when its own rule came up, and a reader written above that rule
     *  became an unresolved marker. {@link #startPointDetermination} still judges every declaration and writes the rule; this pass
     *  declares only what that would declare, and leaves an author's own member of that name to its refusal. */
    private void startPointAnchors(RegelSpraakDocumentContext doc) {
        for (ResultaatDeelContext rd : resultParts(doc)) {
            StartpuntBepalingContext sb = rd.startpuntBepaling();
            if (sb == null) continue;
            String tijdlijnNaam = RegelSpraakModel.normaliseTimelineName(src(sb.tijdlijnNaam()));
            Objecttype subject = model.resolveObjectType(src(sb.universeelOnderwerp().kaleNaam()));
            if (model.timelineByName(tijdlijnNaam) == null || subject == null) continue;
            if (subject.attributen.putIfAbsent("startpunt " + tijdlijnNaam, new Datatype.DatumInDagen()) == null)
                generatedAnchors.add(anchorKey(subject, "startpunt " + tijdlijnNaam));
        }
    }

    private static String anchorKey(Objecttype subject, String member) {
        return subject.naam + "\u0000" + dev.uchar.regelspraak.emit.IrEmitter.norm(member);
    }

    /** The type an objectcreatie creates: the noun before its 'met', resolved as a type or as a role it fills. */
    private Objecttype createdTypeOf(ObjectCreatieContext oc) {
        String[] roleAndAttr = src(oc.kaleNaam(0)).split("(?i)\\s+met\\s+", 2);
        return model.resolveObjectType(stripArticle(roleAndAttr[0]));
    }

    /** §13.4.7 voorwaarde hosting. A feitcreatie guard is built with the ANKER as subject; a guard that
     *  names a kenmerk of the DOEL ("indien de deelnemer vip is") is then an unresolved marker, because the
     *  single-subject §8.1.8 path refuses a to-many navigation from the anker (it would need a separate
     *  pattern). The feitcreatie HAS that pattern ($doel), so re-resolve the guard with the doel as subject;
     *  if it is then a single BARE doel-kenmerk, tag it with {@code eigenaar = doel} so the transpiler hosts
     *  it on the $doel pattern (and never the anker — even under a same-named anker kenmerk). Restricted to
     *  the simple single-kenmerk guard and to {@code doel != anker}: an anker guard that already resolved, a
     *  compound, a self-relation, or a still-unresolved guard is returned unchanged — an un-lowerable guard
     *  stays an honest inert no-op, never a wrong link. */
    private Conditie targetCondition(Conditie anchorCondition, VoorwaardeDeelContext vd, Objecttype anchor, Objecttype doel, String roleC) {
        if (!(anchorCondition instanceof Conditie.Unresolved) || vd == null || doel == anchor) return anchorCondition;
        Conditie targetV;
        int diagSnapshot = model.diagnostics.size();
        targetRole = roleC;                                    // accept "de <roleC>" as the doel; reject "hij"/anker
        try {
            builder.setSubject(doel);
            targetV = buildCondition(vd);
        } finally {
            targetRole = null;
            builder.setSubject(anchor);                       // restore: the anker is the iterated subject
            // This second build is SPECULATIVE — the anker build and the separate voorwaarden() type-check own
            // the diagnostics; drop any this pass appended so a flawed guard is never reported twice.
            model.diagnostics.subList(diagSnapshot, model.diagnostics.size()).clear();
        }
        if (targetV instanceof Conditie.Kenmerk k && k.navigatie() == null && k.eigenaar() == null)
            return new Conditie.Kenmerk(k.kenmerk(), null, k.negatie(), doel, k.bron());
        return anchorCondition;
    }

    /** §13.4.5 kenmerktoekenning: the subject acquires a kenmerk (a boolean property).
     *  Modelled as setting that boolean attribute to true.
     *
     *  <p>Dialect §4, form 1 (maintainer decisions 1–3, 2026-09-23): a negated decision — 'is niet X', 'heeft geen
     *  X', 'is geen X', 'is niet een X' — is read the way the condition side reads a kenmerkcheck
     *  ({@link #characteristicCheckCondition}): the WHOLE written name first, so a declared characteristic whose name begins with
     *  niet/geen binds itself; only a name that binds nothing is read as niet/geen plus the rest. A negated rule
     *  writes the de-negated characteristic with {@link #NEGATED} as its value. The defaults lowering turns that into
     *  besluit false ({@link #defaultOf}); a plain rule that still carries it after the lift is refused
     *  ({@link #checkFlatNegations}), so the marker never reaches the wire. Refused here: a negation whose rest
     *  names no characteristic of the subject; 'is niet een X' on an X that is not zelfstandig — the condition side
     *  binds only a zelfstandig kenmerk under that form (Tabel 16); and a niet/geen written after 'een', which the
     *  condition side reads as part of the name and not as a negation. A positive name keeps its written spelling
     *  (the lookup decides only whether the name is negated), as before, so no document without a negation
     *  changes. */
    private void kenmerk(String naam, KenmerkToekenningContext kt, Conditie voorwaarde, Geldigheid geldigheid) {
        Objecttype subject = builder.chainObjectType(kt.onderwerpKeten());
        String written = src(kt.kaleNaam());
        String kenmerk = written.replaceFirst("^(?i)(de|het|een)\\s+", "");
        int table = tableLine(naam);
        int lijn = table >= 0 ? table : kt.getStart().getLine();   // a table row's lines count in its own stream
        boolean isNegated = false;
        String withoutCharacteristic = null;   // the refusal of a negation that names no declared characteristic
        if (kt.NIET() != null) {
            // 'is niet een X': the grammar carries the negation, so X itself must bind. An unresolved subject is
            // refused by checkRuleTargets, and the negation stays recorded for whatever reads the rule before that.
            isNegated = true;
            String g = subject == null ? null : CharacteristicMatcher.find(subject, written);
            if (g == null && subject != null) g = CharacteristicMatcher.find(subject, kenmerk);
            Objecttype.KenmerkSoort soort = g == null ? null : subject.kenmerkSoort(g);
            if (g != null && soort != null && soort != Objecttype.KenmerkSoort.ZELFSTANDIG) {
                kenmerk = g;
                withoutCharacteristic = "regel '" + naam + "'" + rowTable(naam) + ": 'is niet een " + g + "' — de vorm "
                        + "'is niet een' ontkent een zelfstandig kenmerk (Tabel 16), en '" + g + "' is "
                        + soort.name().toLowerCase(java.util.Locale.ROOT) + "; schrijf "
                        + negationSpelling(soort, g) + " (dialect §4)";
            } else if (g != null) kenmerk = g;
            else if (subject != null)
                withoutCharacteristic = negationWithoutCharacteristic(naam, subject, "niet een " + kenmerk, kenmerk, false);
        } else if (subject != null && kt.EEN() != null) {
            // 'is een …' / 'heeft een …': after 'een' the condition side binds the name whole and reads no negation
            // there (the EEN-form, §8.1.8.3), and a result reads it the same way. A niet/geen after 'een' that leaves
            // the name binding nothing is refused, with the spelling that does negate.
            if (CharacteristicMatcher.find(subject, written) == null && CharacteristicMatcher.find(subject, kenmerk) == null
                    && kenmerk.matches("(?i)(niet|geen)\\s+\\S.*"))
                withoutCharacteristic = negationAfterArticle(naam, subject, kt.HEEFT() != null ? "heeft" : "is", kenmerk);
        } else if (subject != null) {
            boolean has = kt.HEEFT() != null;
            Conditie c = characteristicCheckCondition(subject, written, null, has, bron(kt));
            if (c instanceof Conditie.Kenmerk k && k.negatie()) {
                kenmerk = k.kenmerk();
                isNegated = true;
                checkNegationKind(subject, k, has, lijn);
            } else if (c == null && kenmerk.matches("(?i)(niet|geen)\\s+\\S.*"))
                withoutCharacteristic = negationWithoutCharacteristic(naam, subject, kenmerk,
                        kenmerk.replaceFirst("^(?i)(niet|geen)\\s+", ""), true);
        }
        Expr doel = new Expr.AttribuutRef(kenmerk, subject, src(kt.onderwerpKeten()),
                null, new Datatype.Booleaan(), bron(kt));
        Expr waarde = isNegated ? NEGATED : new Expr.Literal("waar", new Datatype.Booleaan(), Bron.synthetic("waar"));
        ResolvedRegel r = new ResolvedRegel(naam, ResolvedRegel.Soort.KENMERKTOEKENNING, doel, waarde, voorwaarde,
                geldigheid, currentSourceAnnotations);
        model.resolvedRegels.add(r);
        if (withoutCharacteristic != null) {
            // Kept in resolvedRegels and marked refused, so the defaults lowering skips its family instead of
            // adding "schrijft een ander doel" about a target the author never meant.
            refusedTargets.add(r);
            model.diagnostics.add(new Diagnostic(Severity.ERROR, withoutCharacteristic, lijn));
        }
    }

    /** The value a negated kenmerktoekenning carries between its resolution and the defaults lowering (dialect §4,
     *  form 1). WF-022 admits only the constant true on a characteristic target, so a rule carrying this value never
     *  reaches the wire: the lowering turns it into besluit false, and {@link #checkFlatNegations} refuses a plain
     *  rule that still carries it. */
    private static final Expr NEGATED = new Expr.Literal("onwaar", new Datatype.Booleaan(), Bron.synthetic("onwaar"));

    /** Whether {@code r} is a negated kenmerktoekenning — the {@link #NEGATED} value, matched on content so a copy
     *  of the rule record keeps it. A positive kenmerktoekenning always carries the synthetic "waar". */
    private static boolean isNegated(ResolvedRegel r) {
        return r.soort() == ResolvedRegel.Soort.KENMERKTOEKENNING && r.waarde() instanceof Expr.Literal l
                && "onwaar".equals(l.waarde());
    }

    /** The refusal of a negation whose rest names no characteristic of the subject (dialect §4, form 1). The author
     *  wrote a negation, so the message names the rest; with {@code wholeNameAlso} the whole name was tried first and
     *  binds nothing either — the verbatim-first reading the condition side has. A rest that names an ATTRIBUTE gets
     *  the pointer to the gelijkstelling: an attribute has no polarity, its false is a value. */
    private String negationWithoutCharacteristic(String regel, Objecttype subject, String written, String rest,
                                           boolean wholeNameAlso) {
        Datatype attribuut = subject.attribuut(rest);
        String why = attribuut instanceof Datatype.Booleaan
                ? "'" + rest + "' is een attribuut van " + subject.naam + " en geen kenmerk, en een attribuut heeft geen "
                        + "polariteit: dit Boolean-attribuut krijgt onwaar als waarde, te schrijven als 'moet gesteld "
                        + "worden op onwaar'"
                : attribuut != null
                ? "'" + rest + "' is een attribuut van " + subject.naam + " en geen kenmerk, en alleen een kenmerk "
                        + "wordt toegekend of ontkend"
                : "'" + rest + "' is geen kenmerk van " + subject.naam
                        + (wholeNameAlso ? ", en '" + written + "' evenmin" : "") + ". Declareer het kenmerk bij "
                        + subject.naam + ", of schrijf de naam zoals hij gedeclareerd is";
        return "regel '" + regel + "'" + rowTable(regel) + ": '" + written + "' ontkent geen kenmerk van "
                + subject.naam + " — " + why + " (§3.5, dialect §4)";
    }

    /** The refusal of a negation written AFTER 'een' ("is een niet X", "heeft een geen X"). The condition side
     *  reads the name after 'een' whole (§8.1.8.3), so there the words are an unbound name and no negation, and a
     *  result reads them the same way. The message gives the spelling that negates X, by X's kind where X is a
     *  characteristic of the subject. */
    private String negationAfterArticle(String regel, Objecttype subject, String verb, String naam) {
        String rest = naam.replaceFirst("^(?i)(niet|geen)\\s+", "");
        String g = CharacteristicMatcher.find(subject, rest);
        Objecttype.KenmerkSoort soort = g == null ? null : subject.kenmerkSoort(g);
        return "regel '" + regel + "'" + rowTable(regel) + ": '" + verb + " een " + naam + "' ontkent niets — "
                + "achter 'een' leest de frontend de naam als geheel, zoals een voorwaarde dat doet, en '" + naam
                + "' is geen kenmerk van " + subject.naam + ". Een ontkenning staat niet achter 'een'"
                + (soort != null ? "; schrijf " + negationSpelling(soort, g) : "") + " (§8.1.8, dialect §4)";
    }

    /** The negated spelling Tabel 16 gives a characteristic of this kind (§8.1.8). */
    private static String negationSpelling(Objecttype.KenmerkSoort soort, String kenmerk) {
        return switch (soort) {
            case BIJVOEGLIJK -> "'is niet " + kenmerk + "'";
            case BEZITTELIJK -> "'heeft geen " + kenmerk + "'";
            case ZELFSTANDIG -> "'is geen " + kenmerk + "' of 'is niet een " + kenmerk + "'";
        };
    }

    /** §9.9/§13.4.12 startpuntbepaling: "Het startpunt van <tijdlijn> voor <onderwerp> wordt bepaald
     *  door <expressie>". The anchor materialises as a synthetic derived attribute
     *  "startpunt <tijdlijn>" (Datum in dagen) on the subject, written by an ordinary
     *  {@link ResolvedRegel} — so voorwaarde, geldigheid, freshness and idempotence ride the standard
     *  machinery in both back-ends; §9.9's "verschillende startpunten onder verschillende voorwaarden"
     *  are simply several writers of that one attribute. §6.8: the deriving expression must be
     *  Datum in dagen (ERROR otherwise; Onbekend makes no claim), and a leeg value is a fout — the
     *  back-ends raise the §9.5 Inconsistentie for it (the write itself degrades to leeg). */
    private void startPointDetermination(String naam, StartpuntBepalingContext sb, Conditie voorwaarde, Geldigheid geldigheid) {
        int line = sb.getStart().getLine();
        String tijdlijnNaam = RegelSpraakModel.normaliseTimelineName(src(sb.tijdlijnNaam()));
        RegelSpraakModel.Timeline tijdlijn = model.timelineByName(tijdlijnNaam);
        // every refusal below is the verifier's sentence for the document it keeps off disk: WF-032 on the
        // StartPointDetermination, WF-023 on the rule that writes the anchor
        if (tijdlijn == null) {
            model.diagnostics.add(new Diagnostic(Severity.ERROR, "startpuntbepaling (regel '" + naam
                    + "'): start point for undeclared timeline '" + tijdlijnNaam + "'", line));
            return;
        }
        if (!tijdlijn.variabelStartpunt())
            // §9.9 defines the startpuntbepaling for a tijdlijn MET variabel startpunt, and the verifier refuses one
            // for a fixed-startpunt tijdlijn (WF-032), so the frontend refuses it too, in the verifier's words
            model.diagnostics.add(new Diagnostic(Severity.ERROR, "startpuntbepaling (regel '" + naam
                    + "'): start point for fixed-start timeline '" + tijdlijnNaam + "'", line));
        Objecttype subject = model.resolveObjectType(src(sb.universeelOnderwerp().kaleNaam()));
        if (subject == null) {
            model.diagnostics.add(new Diagnostic(Severity.ERROR, "startpuntbepaling (regel '" + naam
                    + "'): object type '" + src(sb.universeelOnderwerp().kaleNaam()) + "' is not declared", line));
            return;
        }
        Expr anchor = builder.buildResult(sb.expressie());   // §11: the anchor IS a rule's `waarde` slot (see registreer)
        if (!anchor.type().isUnknown() && !(anchor.type() instanceof Datatype.DatumInDagen))
            model.diagnostics.add(new Diagnostic(Severity.ERROR, "startpuntbepaling (regel '" + naam + "'): "
                    + Typing.assignment(new Datatype.DatumInDagen(), anchor.type()), line));
        String member = "startpunt " + tijdlijnNaam;
        // the synthetic anchor name is reserved: an author-declared attribute of another type under this
        // exact name would otherwise silently merge with the anchor (a latent type-mismatched write).
        // OWN, with the rest of the §9.9 anchor family (RegelSpraakModel.hasVariableStart): the
        // anchor is DECLARED on the named subject and every reader of it keys on that name, so reading
        // this one guard through the chain while the others do not would only shift the seam.
        Datatype existing = subject.attributen.get(member);
        if (existing != null && !(existing instanceof Datatype.DatumInDagen)) {
            // an undeclared domain has no kind word, and its own refusal names it
            model.diagnostics.add(new Diagnostic(Severity.ERROR, existing.isUnknown()
                    ? "startpuntbepaling: attribuutnaam '" + member + "' is gereserveerd voor het startpunt-anker "
                            + "maar al gedeclareerd als " + existing.naam() + " (regel '" + naam + "')"
                    : "startpuntbepaling (regel '" + naam + "'): anchor member '" + member + "' is "
                            + Typing.kindWord(existing) + ", must be a day-precision date", line));
            return;
        }
        subject.attributen.putIfAbsent(member, new Datatype.DatumInDagen());
        if (generatedAnchors.contains(anchorKey(subject, member)))
            anchorRules.putIfAbsent(anchorKey(subject, member),
                    new String[]{subject.naam, member, RegelSpraakModel.vormnaam(naam)});
        if (model.startPointMember(subject, tijdlijnNaam) == null)
            model.resolvedStartPointDeterminations.add(new dev.uchar.regelspraak.ir.ResolvedStartPointDetermination(tijdlijnNaam, subject.naam, member));
        Expr doel = new Expr.AttribuutRef(member, subject, src(sb.universeelOnderwerp()),
                null, new Datatype.DatumInDagen(), bron(sb));
        model.resolvedRegels.add(new ResolvedRegel(naam, ResolvedRegel.Soort.GELIJKSTELLING, doel, anchor, voorwaarde, geldigheid, currentSourceAnnotations));
    }

    /** The object type a rule is about — the object type of its assignment target. */
    private Objecttype ruleSubjectOf(ResultaatDeelContext rd) {
        AttribuutVanOnderwerpContext doel = null;
        if (rd == null) return null;
        if (rd.gelijkstelling() != null) doel = rd.gelijkstelling().attribuutVanOnderwerp();
        else if (rd.initialisatie() != null) doel = rd.initialisatie().attribuutVanOnderwerp();
        else if (rd.kenmerkToekenning() != null) return builder.chainObjectType(rd.kenmerkToekenning().onderwerpKeten());
        else if (rd.objectCreatie() != null) return builder.chainObjectType(rd.objectCreatie().onderwerpKeten());
        else if (rd.feitCreatie() != null) return builder.resolveSubject(rd.feitCreatie().onderwerp(0)); // the anker — so its voorwaarde (incl. a kenmerk) resolves
        else if (rd.startpuntBepaling() != null) // §13.4.12 — so "zijn <attribuut>" in the anchor + voorwaarde resolves
            return model.resolveObjectType(src(rd.startpuntBepaling().universeelOnderwerp().kaleNaam()));
        else if (rd.consistentieRegel() != null) {
            // §13.4.8 the subject-led samengestelde consistentieregel names its onderwerp ("Een Vondst moet
            // voldoen aan …"), so "zijn …" in its criteria resolves against it
            if (rd.consistentieRegel().samengesteldeConsistentie() != null)
                return builder.resolveSubject(rd.consistentieRegel().samengesteldeConsistentie().onderwerp());
            EnkelvoudigeConsistentieRegelContext e = rd.consistentieRegel().enkelvoudigeConsistentieRegel();
            return e != null && e.expressie() != null
                    && builder.build(e.expressie()) instanceof Expr.AttribuutRef a ? a.objecttype() : null;
        }
        return doel != null && builder.buildAttribute(doel) instanceof Expr.AttribuutRef a ? a.objecttype() : null;
    }

    private void register(String naam, ResolvedRegel.Soort soort, AttribuutVanOnderwerpContext doel,
                            ExpressieContext waarde, int line, Conditie voorwaarde, Geldigheid geldigheid) {
        Expr targetExpr = builder.buildAttribute(doel);
        // §11 the RESULT-part value: variable reads become BinderRefs under the variabelendeel's ch. 4.10 Let chain.
        // This is the ONE slot the chain is BUILT for — a rule's `waarde` — and it reaches the wire in two shapes:
        // as the ResolvedRegel's own value, and (when the §4 defaults pass lifts this rule) as a Default's
        // `consequentie`, which {@link #defaultOf} copies straight from it. The other resultaatdeel kinds
        // (verdeling amounts, objectcreatie toekenningen, feitcreatie, a consistentieregel's asserted expression)
        // keep the v0 inline route: a consistentie assertion is a Conditie, which cannot host an Expr Let at all,
        // and the remaining ones carry several independent value slots that no single chain spans. Documented as a
        // scope limit rather than lowered half-way; no corpus or parity material exercises them.
        // §13.2.14: the doel is the WRITE-TARGET context an enumeratiewaarde takes its domain from — the
        // same context the membership check below reads. Promoting BEFORE the ResolvedRegel is built is
        // what puts the typed literal on the wire (ExpressionBuilder.enumAgainstContext).
        Expr valueExpr = ExpressionBuilder.enumAgainstContext(builder.buildResult(waarde), targetExpr.type());
        model.resolvedRegels.add(new ResolvedRegel(naam, soort, targetExpr, valueExpr, voorwaarde, geldigheid, currentSourceAnnotations));
        Datatype targetType = targetExpr.type(), valueType = valueExpr.type();
        // Both static checks below judge the value's SHAPE, so they see through the ch. 4.10 Let chain the
        // variabelendeel lowering wraps around it: `Daarbij geldt: de keuze is -49` denotes the same literal
        // the bare -49 does, and the lowering changed the wire, not what is statically decidable.
        Expr peeledValue = afgepeld(valueExpr);
        // §13.3.4.2 membership runs BEFORE the Onbekend bail: an enumeratiewaarde carries no domain of its
        // own (it is the doel that supplies one), so the value's type says nothing about whether the label
        // is declared. The label is read through the shared helper because the promotion above reaches a
        // Let chain only as far as the chain's own body leads it: the DIRECT chain is promoted (`Daarbij
        // geldt: de tint is 'gaaf'` writes a fully typed Let), but a NESTED one whose body reads an outer
        // binder past an inner Let is not — afgepeld resolves that shape through its bindings map where
        // enumAgainstContext, which walks the body, cannot. Measured: a rule reading `de tweede`, defined as
        // `de tint`, defined as the literal, keeps the marker AND still reports an undeclared label here.
        checkEnumLabel(targetType, ExpressionBuilder.valueEnumLabel(peeledValue), line);
        checkSign(targetType, peeledValue, line);   // §3.3.1 Teken-beperking, static over a literal value
        if (targetType.isUnknown() || valueType.isUnknown()) return; // not enough resolved to judge
        String fout = Typing.assignment(targetType, valueType, ExpressionBuilder.unitlessNumberLiteral(valueExpr));
        if (fout != null)
            model.diagnostics.add(new Diagnostic(Severity.ERROR, soort.name().toLowerCase() + ": " + fout, line));
    }

    /** The expression a ch. 4.10 {@code Let} chain ultimately DENOTES, for the shape-sensitive static checks:
     *  peel the chain, then follow a body that is nothing but a read of one of its own binders. Under ch. 10.6
     *  such a chain evaluates to exactly that expression, so `Daarbij geldt: de keuze is -49` must fail the
     *  Teken-beperking precisely as the bare literal does. Following REMOVES the binding, which bounds the walk
     *  (each binder is followed at most once) without relying on WF-076's value/body scoping. */
    private static Expr afgepeld(Expr e) {
        Map<String, Expr> bound = new LinkedHashMap<>();
        while (e instanceof Expr.Let l) { bound.put(l.binder(), l.value()); e = l.body(); }
        while (e instanceof Expr.BinderRef b && bound.containsKey(b.naam())) e = bound.remove(b.naam());
        return e;
    }

    /** §3.3.1 / §13.3.3.7: the optional Teken-beperking (negatief / niet-negatief / positief) narrows the
     *  datatype's toegestane waarden (§3.3.6 Tabel: "-49" is niet toegestaan for a niet-negatief geheel
     *  getal). The spec states NO runtime rule for a computed out-of-sign value (unlike §6.1.2 decimals,
     *  which demands an explicit afronding), so enforcement is RESOLVER-STATIC over the statically-decidable
     *  surface — a LITERAL assigned/initialised into a sign-restricted Numeriek doel — exactly the
     *  enum-domein posture (Tabel 61 rij 7). A computed value's sign is undecidable here and stays
     *  unchecked (documented in grammar/DIVERGENCES.md). Percentage carries no Teken in the model.
     *
     *  <p>The verifier refuses the same literals (WF-039's target clause), reading the sign off the literal's value:
     *  zero is neither negative nor positive, so {@code -0} passes a non-negative target and {@code 0/5} fails a
     *  positive one. This check reads it the same way and speaks its sentence, which names the lexeme the emitter
     *  writes. The verifier stands aside where the literal's own type already refuses it; a number literal here is
     *  typed without a sign of its own, so this is the one message. */
    private void checkSign(Datatype targetType, Expr waarde, int line) {
        if (!(targetType instanceof Datatype.Numeriek n) || n.teken() == Datatype.Teken.ONBEPAALD) return;
        if (!(waarde instanceof Expr.Literal lit) || lit.type() == null || !lit.type().isNumber()) return;
        java.math.BigDecimal v = Literals.getal(lit);
        if (v == null) return;
        String w = dev.uchar.regelspraak.emit.IrEmitter.numberLexeme(lit.waarde());   // the lexeme IrEmitter writes
        String sentence = switch (n.teken()) {
            case NIET_NEGATIEF -> v.signum() < 0 ? "negative literal '" + w + "'" : null;
            case POSITIEF      -> v.signum() > 0 ? null : "literal '" + w + "'";
            case NEGATIEF      -> v.signum() < 0 ? null : "literal '" + w + "'";
            case ONBEPAALD     -> null;   // unreachable (guarded above)
        };
        if (sentence != null) model.diagnostics.add(new Diagnostic(Severity.ERROR, "teken-beperking: " + sentence
                + " is written into a target under a " + signWord(n.teken()) + " sign restriction", line));
    }

    /** The verifier's word for a sign restriction in WF-039's sentences. */
    private static String signWord(Datatype.Teken t) {
        return switch (t) {
            case NEGATIEF -> "negative";
            case NIET_NEGATIEF -> "non-negative";
            case POSITIEF -> "positive";
            case ONBEPAALD -> throw new IllegalStateException("RuleChecker: een onbepaald teken heeft geen woord");
        };
    }

    /** Tabel 36/43/61 rij 7 + §13.3.4.2: a written enum label must be IN the context domain's declared
     *  value set — an undeclared label is a dead comparison or a foreign-value assignment, silently. The
     *  literal itself stays typed Onbekend (which domain applies IS this context); a null label (the
     *  expression is not a quoted enumeratiewaarde) or a non-enum context makes no claim. */
    private void checkEnumLabel(Datatype context, String label, int line) {
        if (!(context instanceof Datatype.Enumeratie enumDatatype) || label == null || enumDatatype.waarden().contains(label)) return;
        // the verifier refuses the same literal with WF-039's sentence (the literal carries the context domain)
        model.diagnostics.add(new Diagnostic(Severity.ERROR,
                "enumeration label '" + label + "' is not in the declared domain", line));
    }

    /** The enum label when the WHOLE expressie is a single quoted enumeratiewaarde, else null. An
     *  ENUM_LITERAL is one lexer token, so getText() preserves its interior spaces — no need for the
     *  costlier src() stream reconstruction that runs on every comparison operand. */
    private static String enumLabelOf(ExpressieContext e) {
        return e == null ? null : Literals.enumLabel(e.getText().trim());
    }

    // ---- §13.4.13/.14/.15 condition IR (structure for the transpiler "when" side) ----
    // Built alongside (not instead of) the exhaustive type-check walk below, so it
    // adds structure without changing any diagnostic. The kenmerk/objecttype membership
    // tests and period comparisons this comment once listed as unstructured have since
    // gained their own nodes (Conditie.Kenmerk, Rollidmaatschap/FarRoleMembership,
    // Periode); what is still unstructured becomes Onopgelost.
    private Conditie buildCondition(VoorwaardeDeelContext v) {
        if (v.toplevelSamengesteldeVoorwaarde() != null) return buildCompound(
                v.toplevelSamengesteldeVoorwaarde().voorwaardeKwantificatie(),
                v.toplevelSamengesteldeVoorwaarde().samengesteldeVoorwaardeOnderdeel(), v.toplevelSamengesteldeVoorwaarde());
        if (v.toplevelElementaireVoorwaarde() != null) {
            ToplevelElementaireVoorwaardeContext c = v.toplevelElementaireVoorwaarde();
            // §8.3.1: after 'indien'/'dat'/'gedurende de tijd dat' the grammar admits ONLY the verb-trailing
            // (vragende) toplevelOperator — the verb-leading register is reserved for samengestelde bullets.
            if (c.expressie() != null && c.toplevelOperator() != null)
                return conditionFromToplevel(builder.build(c.expressie()), c.expressie(), c.toplevelOperator(), bron(c));
            // §8.1.9 / §13.4.15.18: "indien regelversie <X> is gevuurd|inconsistent" as the whole voorwaarde.
            if (c.consistentieVoorwaarde() != null) return buildRuleStatusCheck(c.consistentieVoorwaarde());
        }
        // §13.4.13.1 / §8.4.3: a standalone periodevergelijking as the whole voorwaardendeel
        // ("… van dd. 1-1-2024 tot dd. 8-2-2025" directly after the resultaatdeel, no 'indien').
        if (v.periodevergelijkingEnkelvoudig() != null) {
            if (followsPeriodBullet(v.periodevergelijkingEnkelvoudig()))
                model.diagnostics.add(new Diagnostic(Severity.ERROR,
                        "periodevergelijking '" + src(v.periodevergelijkingEnkelvoudig()) + "' volgt op een laatste "
                        + "bullet 'het is de periode' van een samengestelde deelselectie: de parser leest die bullet als "
                        + "een kenmerk 'het is de periode' en haar grens als een venster op de hele regel — een ander "
                        + "document dan er staat (§8.4.3). Schrijf het venster als voorwaardendeel van de regel ('… "
                        + src(v.periodevergelijkingEnkelvoudig()) + "' achter het resultaatdeel: vóór die grens vuurt "
                        + "de regel niet), of zet de periodebullet niet als laatste in de deelselectie (dan leest de "
                        + "parser haar als lid van het filter)",
                        v.getStart().getLine()));
            return buildTimeWindow(v.periodevergelijkingEnkelvoudig());
        }
        return new Conditie.Unresolved(bron(v),
                "het voorwaardendeel heeft geen van de §13.4.13-vormen — geen samengestelde voorwaarde, geen "
                + "elementaire voorwaarde met operator, geen regelpredicaat en geen periodevergelijking");
    }

    /** Whether the on-channel tokens right before {@code p} spell "het is de periode": the split measured in fix
     *  round 2 (2026-09-23). A window bullet written as the LAST bullet of a compound subselection ends that
     *  subselection at its words "het is de periode" (read as a characteristic of that name) and hands the bound
     *  to the rule as its voorwaardendeel. Nothing else puts those four words directly before a rule-level
     *  periodevergelijking. False without the main stream (a desugared table row lives in a stream of its own). */
    private boolean followsPeriodBullet(ParserRuleContext p) {
        if (tokens == null || p.getStart() == null) return false;
        int idx = p.getStart().getTokenIndex();
        if (idx < 0 || idx >= tokens.size() || tokens.get(idx) != p.getStart()) return false;
        int[] verwacht = {RegelSpraakLexer.PERIODE, RegelSpraakLexer.DE, RegelSpraakLexer.IS, RegelSpraakLexer.HET};
        int k = 0;
        for (int j = idx - 1; j >= 0 && k < verwacht.length; j--) {
            Token t = tokens.get(j);
            if (t.getChannel() != Token.DEFAULT_CHANNEL) continue;
            if (t.getType() != verwacht[k]) return false;
            k++;
        }
        return k == verwacht.length;
    }

    /** §13.4.15.68 / §8.4.3: a periodevergelijkingEnkelvoudig (vanaf / van…tot / van…tot en met / tot /
     *  tot en met) to a {@link Conditie.Periode}. Each bound is a §13.4.15.68 &lt;datumexpressie&gt;; a bound
     *  whose resolved type makes a non-datum claim is an ERROR (an Onbekend operand carries no claim and
     *  passes). §5.1.3 fixes the bound semantics: vanaf inclusive, tot exclusive, tot en met inclusive. */
    private Conditie buildTimeWindow(PeriodevergelijkingEnkelvoudigContext p) {
        Expr van = null, tot = null;
        if (p.VANAF() != null) {
            van = builder.build(p.expressie(0));
        } else if (p.VAN() != null) {
            van = builder.build(p.expressie(0));
            tot = builder.build(p.expressie(1));
        } else {
            tot = builder.build(p.expressie(0));
        }
        for (Expr limit : new Expr[]{van, tot})
            if (limit != null && limit.type() != null && !limit.type().isUnknown() && !limit.type().isDate())
                model.diagnostics.add(new Diagnostic(Severity.ERROR, "periodevergelijking: window bound is "
                        + Typing.kindWord(limit.type()) + ", must be a date", p.getStart().getLine()));
        for (String fout : new String[]{periodBoundaryError(van, "vanaf"), periodBoundaryError(tot, "tot")})
            if (fout != null) model.diagnostics.add(new Diagnostic(Severity.ERROR, fout, p.getStart().getLine()));
        return new Conditie.Periode(van, tot, p.TOT_EN_MET() != null, bron(p));
    }

    /** WF-111 (maintainer decision 2026-09-23), producer side: a window bound is a date literal or an own
     *  scalar date attribute of the subject, both of day precision. §13.4.15.68 admits any date expression and
     *  this method is where the frontend narrows it to the wire's two shapes; before it, every other bound
     *  reached disk silently and the verifier refused the document. ERROR, so {@link Resolver#irModus} writes
     *  nothing.
     *
     *  <p>"Own" is compared by IDENTITY with the subject, not by the ⊑ the rule's text allows, and on purpose:
     *  this frontend writes an own read — an inherited attribute included — with the subject as its object type,
     *  and a read naming any other type, a supertype of the subject included, as a navigation: by the written role
     *  or chain, or by the one relation between the two types. The verifier refuses it as navigated (WF-111), so
     *  identity is what gives its verdict on this frontend's output. A marker bound is left to the marker sweep, and
     *  a non-date bound to the WF-042 message above. Null when the bound is admissible.
     *
     *  <p>The refusal is the verifier's WF-111 sentence for the bound the emitter writes. Where no relation reaches
     *  the other type, the emitter has no spelling for the read at all (it refuses such a model), so no document
     *  carries it and the refusal keeps this frontend's words. */
    private String periodBoundaryError(Expr limit, String side) {
        if (limit == null || limit instanceof Expr.Unresolved) return null;
        if (limit.type() != null && !limit.type().isUnknown() && !limit.type().isDate()) return null;
        Objecttype subject = builder.subject();
        String before = "periodevergelijking (de " + side + "-grens '" + limit.bron() + "'): the "
                + (side.equals("vanaf") ? "from" : "to") + " bound of a time window is ";
        String ogenblik = before + "a millisecond-precision date: the window is a range of civil days and no clause "
                + "reads an instant bound";
        String what;
        if (limit instanceof Expr.Literal) {
            return limit.type() instanceof Datatype.DatumTijd ? ogenblik : null;
        } else if (limit instanceof Expr.AttribuutRef a) {
            if (a.navigatieRol() != null || !a.navigationPath().isEmpty() || a.set()
                    || subject != null && a.objecttype() != subject) {
                if (dev.uchar.regelspraak.emit.IrEmitter.navigationBindings(model, subject, a).isEmpty())
                    return periodBoundaryWithoutReader(a, side, subject);
                what = "a navigated read of '" + a.attribuut() + "'";
            } else if (subject == null)
                what = "the attribute '" + a.attribuut() + "' at a position with no subject to own it";
            else if (subject.granulariteitVan(a.attribuut()) != null)
                what = "the time-dependent attribute '" + a.attribuut() + "'";
            else if (a.type() instanceof Datatype.DatumTijd)
                return ogenblik;
            else return null;
        } else if (limit instanceof Expr.ParameterRef pr) {
            what = "the parameter '" + pr.parameter().naam() + "'";
        } else {
            what = "a computed date expression";
        }
        return before + what + " — a bound is a date literal or an own scalar date attribute of the subject; the general "
                + "date expression is an OPEN wire decision";
    }

    /** A bound that navigates, written or by naming another object type, where no path is derivable: the emitter
     *  then writes no navigation — it refuses such a model, or writes a zero-hop self-reference as the subject's own
     *  read, which the verifier admits — so no reader refuses the bound, and the refusal keeps this frontend's words. */
    private static String periodBoundaryWithoutReader(Expr.AttribuutRef a, String side, Objecttype subject) {
        String reason;
        if (a.navigatieRol() != null || !a.navigationPath().isEmpty() || a.set())
            reason = "een genavigeerde of verzamelende lezing van '" + a.attribuut() + "'";
        else if (a.objecttype() != null && subject.isEen(a.objecttype()))
            reason = "het attribuut '" + a.attribuut() + "' van '" + a.objecttype().naam + "', een supertype van "
                    + "het onderwerp '" + subject.naam + "': deze frontend leest die naam als een ander exemplaar, "
                    + "langs een feittype bereikt, en een venstergrens navigeert nooit. Lees de datum van het "
                    + "onderwerp zelf ('zijn " + a.attribuut() + "' of met de naam " + subject.naam + ")";
        else if (a.objecttype() != null && a.objecttype().isEen(subject))
            reason = "het attribuut '" + a.attribuut() + "' van '" + a.objecttype().naam + "', een subtype van het "
                    + "onderwerp '" + subject.naam + "': niet elk onderwerp is een " + a.objecttype().naam;
        else
            reason = "een lezing van '" + a.attribuut() + "' op '"
                    + (a.objecttype() == null ? "?" : a.objecttype().naam) + "', een ander objecttype dan het "
                    + "onderwerp '" + subject.naam + "': een navigatie, en een venstergrens navigeert nooit";
        return "periodevergelijking (§8.4.3): de " + side + "-grens '" + a.bron() + "' is " + reason
                + " — een venstergrens is een datumliteraal of een eigen, niet-tijdsafhankelijk "
                + "datum-in-dagen-attribuut van het onderwerp; de algemene datumexpressie die §13.4.15.68 toelaat "
                + "is een open wirebeslissing (WF-111, IR-SPEC ch. 4.6)";
    }

    private Conditie buildCompound(VoorwaardeKwantificatieContext k,
                                      SamengesteldeVoorwaardeOnderdeelContext component, ParserRuleContext whole) {
        // §13.4.14.6: when the indentation-blind grammar ABSORBED a dedented sibling bullet into a deeper compound but
        // the bullet COLUMNS re-nest cleanly, rebuild the correct sibling tree so the compound FIRES (depth-aware
        // re-nesting). reNestParts returns null when there is no column-absorption (the common case — a
        // byte-identical no-op) OR the columns do not disambiguate (the existing inconsistency-marker path then applies).
        List<Conditie> herNest = reNestParts(component);
        List<Conditie> onderdelen;
        if (herNest != null) {
            onderdelen = herNest;
        } else {
            onderdelen = new ArrayList<>();
            if (component != null)
                for (GenesteVoorwaardeContext g : component.genesteVoorwaarde()) onderdelen.add(buildNested(g));
        }
        Conditie.Kwantor kwantor = quantifierOf(k);
        int threshold = thresholdOf(k);
        // A counting quantifier's telwoord is a positive whole number (§13.4.14.4 /
        // §13.4.14.5: één/twee/… or a written getal). A 0, negative, decimal, fraction or
        // unparseable value all reach thresholdOf as < 1; flag it rather than let it collapse
        // the rule to always-true (minstens(0) is vacuously true). An over-size N is fine —
        // it is a faithfully unsatisfiable "false", so it is not an error.
        checkThreshold(kwantor, k, (k != null ? k : whole).getStart().getLine());
        // §13.4.14.6 the indentation-blind bullet grammar (bullet : (DASH|DOTDOT|BULLET)+, WS skipped) can ABSORB a
        // dedented sibling bullet (a leaf OR another nested compound) into a preceding nested compound's list, so a
        // COUNTING quantifier AND-merges what the spec OR-s/counts -> a fired-WRONG result (the DRL oracle computed a
        // wrong kenmerk/SOM). The grammar discards depth, but ANTLR retains each bullet token's COLUMN and GLYPH: a list
        // whose direct bullets sit at different columns OR carry different glyphs (e.g. a top-level '-' absorbed into a
        // nested '..' list) absorbed an outer-level sibling. Marker such a list (Onopgelost) so BOTH back-ends decline
        // rather than fire a mis-parsed compound — for ANY quantifier (a well-formatted compound is column- AND
        // glyph-consistent, so only a genuinely-ambiguous input trips; the ALLE/DE merge is AND-idempotent but markering
        // it is still safe). A structural depth-2 fallback covers the residual fully-ambiguous case (same glyph AND same
        // column), scoped to COUNTING (a legitimately deep ALLE/DE nesting would otherwise be over-markered). A faithful
        // depth-aware re-nesting (fire correctly) is carded.
        // A re-nested list (herNest != null) has its absorption RESOLVED, so the inconsistency markers below no
        // longer apply; they guard only the un-disambiguatable residual (glyph-only / same-column-same-glyph) that
        // reNestParts declined.
        if (herNest == null && (bulletsInconsistent(component)
                || (kwantor != Conditie.Kwantor.ALLE && kwantor != Conditie.Kwantor.DE && ambiguousNestedBullets(onderdelen))))
            return new Conditie.Unresolved(bron(whole),
                    "de bullets van de samengestelde voorwaarde staan op verschillende kolommen of dragen "
                    + "verschillende tekens, dus welke bullet onder welke hoort ligt niet vast (§13.4.14.6)");
        return new Conditie.Samengesteld(kwantor, threshold, onderdelen, bron(whole));
    }

    /** §13.4.14.4/.5 a counting quantifier's telwoord must be a positive whole number (>= 1): a 0 / negative / decimal /
     *  unparseable value reaches {@link #thresholdOf} as &lt; 1 and would collapse a ten-minste/ten-hoogste/precies to
     *  vacuously-true (minstens(0) always holds), so flag it. Shared by {@link #buildCompound} and the depth-aware
     *  re-nesting {@link #buildFromNodes} (which builds inner compounds directly, bypassing buildCompound). */
    private void checkThreshold(Conditie.Kwantor kwantor, VoorwaardeKwantificatieContext k, int regel) {
        int threshold = thresholdOf(k);
        if ((kwantor == Conditie.Kwantor.TEN_MINSTE || kwantor == Conditie.Kwantor.TEN_HOOGSTE
                || kwantor == Conditie.Kwantor.PRECIES) && threshold < 1)
            // a whole number reaches the wire as written and the verifier refuses it in WF-041's words; a decimal
            // or fraction reaches it as 0, a number the author did not write, so that refusal keeps its own words
            model.diagnostics.add(new Diagnostic(Severity.ERROR, wholeNumeral(k)
                    ? "telwoord van een kwantificatie: counting quantifier with threshold " + threshold + " < 1"
                    : "telwoord van een kwantificatie moet een positief geheel getal (>= 1) zijn", regel));
    }

    /** Whether the telwoord is written as a whole number that {@link #numeralValue} reads as such. */
    private static boolean wholeNumeral(VoorwaardeKwantificatieContext k) {
        if (k == null || k.telwoord() == null || k.telwoord().NUMBER() == null) return false;
        try { Integer.parseInt(k.telwoord().NUMBER().getText()); return true; }
        catch (NumberFormatException e) { return false; }
    }

    // ---- §13.4.14.6 depth-aware bullet re-nesting (column-driven) ----------------------------------------------
    // The bullet grammar is indentation-blind (`bullet : (DASH|DOTDOT|BULLET)+`, WS skipped), so a dedented sibling
    // bullet is ABSORBED as a child of the preceding nested compound. ANTLR retains the discarded indentation on each
    // bullet token (getCharPositionInLine()); when that absorption re-nests cleanly by column, rebuild the intended
    // sibling tree so BOTH back-ends fire the spec-correct OR/count instead of markering an AND-merged mis-parse.

    /** A flattened bullet: its indentation column and the parse node, captured in source order. */
    private record RawBullet(int column, GenesteVoorwaardeContext ctx) {}
    /** A re-nested logical node: a leaf or a compound (its {@code genesteVoorwaarde} ctx) with rebuilt children. */
    private record LogicalNode(GenesteVoorwaardeContext ctx, List<LogicalNode> children) {}

    /** Depth-first (= source-order) flatten of every {@code genesteVoorwaarde} in the subtree, with its bullet column. */
    private static void flattenBullets(SamengesteldeVoorwaardeOnderdeelContext component, List<RawBullet> result) {
        for (GenesteVoorwaardeContext g : component.genesteVoorwaarde()) {
            result.add(new RawBullet(g.bullet().getStart().getCharPositionInLine(), g));
            if (g.genesteSamengesteldeVoorwaarde() != null)
                flattenBullets(g.genesteSamengesteldeVoorwaarde().samengesteldeVoorwaardeOnderdeel(), result);
        }
    }

    /** True iff some onderdeel in the subtree has direct bullets at non-uniform COLUMNS — the reliable signature of the
     *  indentation-blind grammar absorbing a dedented sibling from an outer level into a deeper list. */
    private static boolean columnsInconsistentDeep(SamengesteldeVoorwaardeOnderdeelContext component) {
        List<GenesteVoorwaardeContext> gs = component.genesteVoorwaarde();
        if (gs == null || gs.isEmpty()) return false;   // a childless nested header (malformed parse, ANTLR error-recovery) -> no absorption
        int kol0 = gs.get(0).bullet().getStart().getCharPositionInLine();
        for (GenesteVoorwaardeContext g : gs) {
            if (g.bullet().getStart().getCharPositionInLine() != kol0) return true;
            if (g.genesteSamengesteldeVoorwaarde() != null
                    && columnsInconsistentDeep(g.genesteSamengesteldeVoorwaarde().samengesteldeVoorwaardeOnderdeel()))
                return true;
        }
        return false;
    }

    /** Re-nest a flat bullet list by indentation column into a logical sibling tree: a bullet at a deeper column is a
     *  child of the most recent compound header, a dedent returns to the matching ancestor level. Returns null when the
     *  columns do NOT form a clean indentation tree (a column that lines up with no open level, or a compound header
     *  whose next bullet is not deeper) — i.e. genuinely un-disambiguatable, so the caller keeps the marker path. */
    private static List<LogicalNode> reNestColumns(List<RawBullet> flatten) {
        List<LogicalNode> root = new ArrayList<>();
        List<Integer> kindColumn = new ArrayList<>();                 // the child-column of each open frame
        List<List<LogicalNode>> kindList = new ArrayList<>();     // the children list of each open frame
        kindColumn.add(flatten.get(0).column());
        kindList.add(root);
        for (int i = 0; i < flatten.size(); i++) {
            RawBullet r = flatten.get(i);
            while (kindColumn.size() > 1 && r.column() < kindColumn.get(kindColumn.size() - 1)) {
                kindColumn.remove(kindColumn.size() - 1);
                kindList.remove(kindList.size() - 1);
            }
            if (r.column() != kindColumn.get(kindColumn.size() - 1)) return null;   // column lines up with no open level
            LogicalNode node = new LogicalNode(r.ctx(), new ArrayList<>());
            kindList.get(kindList.size() - 1).add(node);
            if (r.ctx().genesteSamengesteldeVoorwaarde() != null) {              // a compound header: open a child frame
                if (i + 1 >= flatten.size() || flatten.get(i + 1).column() <= r.column()) return null;   // no deeper children
                kindColumn.add(flatten.get(i + 1).column());
                kindList.add(node.children());
            }
        }
        return root;
    }

    /** Build the onderdelen list from a re-nested logical tree, within the current {@code builder} subject (the leaves'
     *  expressies resolve against whichever subject the caller set). A compound knoop reads its own quantifier. */
    private List<Conditie> buildFromNodes(List<LogicalNode> nodes) {
        List<Conditie> result = new ArrayList<>();
        for (LogicalNode k : nodes) {
            GenesteSamengesteldeVoorwaardeContext c = k.ctx().genesteSamengesteldeVoorwaarde();
            if (c != null) {
                Conditie.Kwantor kw = quantifierOf(c.voorwaardeKwantificatie());
                int threshold = thresholdOf(c.voorwaardeKwantificatie());
                checkThreshold(kw, c.voorwaardeKwantificatie(), c.getStart().getLine());   // a degenerate inner telwoord must still be flagged
                result.add(new Conditie.Samengesteld(kw, threshold, buildFromNodes(k.children()), bron(c)));
            }
            else if (k.ctx().elementaireVoorwaarde() != null)
                result.add(buildElementary(k.ctx().elementaireVoorwaarde()));
            else
                // GEEN GETUIGE: genesteVoorwaarde is `bullet (genesteSamengesteldeVoorwaarde |
                // elementaireVoorwaarde) DOT?`, dus een knoop draagt er altijd één — en deze tak wordt
                // pas bereikt NA een geslaagde kolom-hernesting, die een kapotte ontleding juist afwijst.
                // De reden staat er voor het geval de grammatica een derde tak krijgt.
                result.add(new Conditie.Unresolved(bron(k.ctx()),
                        "deze bullet van de hernestte opsomming is geen geneste samengestelde voorwaarde en "
                        + "geen elementaire voorwaarde (§13.4.14)"));
        }
        return result;
    }

    /** §13.4.14.6: if a bullet list carries a column-disambiguatable absorption, return the correctly re-nested
     *  onderdelen; else null (the caller keeps its existing per-bullet build + inconsistency-marker path). Returns null
     *  for the common no-absorption case (a structural no-op, so well-formatted lists are byte-identical) and for the
     *  un-disambiguatable residual (glyph-only or same-column-same-glyph, which the marker path then declines). */
    private List<Conditie> reNestParts(SamengesteldeVoorwaardeOnderdeelContext component) {
        if (component == null || component.genesteVoorwaarde().isEmpty()) return null;
        if (!columnsInconsistentDeep(component)) return null;       // no absorption -> existing path (no-op)
        List<RawBullet> flatten = new ArrayList<>();
        flattenBullets(component, flatten);
        List<LogicalNode> root = reNestColumns(flatten);
        if (root == null) return null;                             // columns don't disambiguate -> marker path
        return buildFromNodes(root);
    }

    /** §13.4.14.6 whether a bullet list's direct bullets are inconsistent — different indentation COLUMNS or different
     *  bullet GLYPHS — the reliable signature of the indentation-blind grammar absorbing a sibling bullet from an outer
     *  level into this list. The lexer skips WS but ANTLR retains each bullet token's column and text, so a well-formatted
     *  compound (siblings at one column with one glyph, e.g. the '-' top / '..' nested convention) reads false; only
     *  a genuinely-ambiguous / mis-nested input reads true. The glyph check catches the un-indented mixed-glyph absorption
     *  the column check cannot (a '-' leaf folded into a '..' list); the residual same-glyph-same-column case is the
     *  ambiguousNestedBullets fallback's job. */
    private static boolean bulletsInconsistent(SamengesteldeVoorwaardeOnderdeelContext component) {
        if (component == null) return false;
        List<GenesteVoorwaardeContext> gs = component.genesteVoorwaarde();
        if (gs == null || gs.size() < 2) return false;
        int col0 = gs.get(0).bullet().getStart().getCharPositionInLine();
        String glyph0 = gs.get(0).bullet().getText();
        for (GenesteVoorwaardeContext g : gs)
            if (g.bullet().getStart().getCharPositionInLine() != col0 || !g.bullet().getText().equals(glyph0)) return true;
        return false;
    }

    /** §13.4.14.6 structural fallback for the residual fully-ambiguous input the column+glyph check cannot see (same
     *  glyph AND same column): a bullet list carrying the flatten signature of 2+ sibling NESTED compounds — a nested
     *  compound bullet that itself contains a DEEPER compound. A single level of nesting (a nested compound of leaves) is
     *  fine. Scoped to COUNTING by the caller (a deep ALLE/DE merge is AND-idempotent). */
    private static boolean ambiguousNestedBullets(List<Conditie> onderdelen) {
        return onderdelen.stream().anyMatch(o -> o instanceof Conditie.Samengesteld inner
                && inner.onderdelen().stream().anyMatch(x -> x instanceof Conditie.Samengesteld));
    }

    /** The threshold count N of a ten-minste/ten-hoogste/precies quantifier (§13.4.14.4),
     *  or 0 when the quantifier carries no count (alle / geen / de). */
    private int thresholdOf(VoorwaardeKwantificatieContext k) {
        return k == null ? 0 : numeralValue(k.telwoord());
    }

    /** §5.6/§13.4.13.4 a COMPOUND subselectie ("… die aan alle volgende voorwaarden voldoen: - … - …") resolved
     *  against the AGGREGATED ELEMENT type: each bullet binds to the element (not the rule subject), so the resulting
     *  {@link Conditie.Samengesteld} is a filter on the accumulate SOURCE pattern. The bouwer subject is saved and
     *  restored around the bullet walk — aggregationFilter runs DURING {@code builder.build}, nested inside the outer rule
     *  resolution, so the rule subject must be put back (in a finally, even on a bullet-resolution throw). */
    private Conditie compoundElementFilter(SamengesteldPredicaatContext sp, Objecttype elem) {
        if (sp == null || elem == null
                || (sp.samengesteldeVoorwaardeOnderdeel() == null && sp.elementaireVoorwaarde() == null)) return null;
        Objecttype previous = builder.subject();          // SAVE the outer rule subject
        try {
            builder.setSubject(elem);                   // "zijn X" / "hij" in the bullets resolve against the element
            // §13.4.13.4 the INLINE single-comparison tail ("… die aan de volgende voorwaarde voldoet: zijn
            // leeftijd is kleiner dan …") — one non-bulleted voorwaarde, resolved by the same elementaire
            // machinery a bullet uses and wrapped as a 1-onderdeel compound so every quantifier lowers
            // through the existing Samengesteld path.
            if (sp.samengesteldeVoorwaardeOnderdeel() == null)
                return new Conditie.Samengesteld(quantifierQuantification(sp.kwantificatie()),
                        sp.kwantificatie() == null ? 0 : numeralValue(sp.kwantificatie().telwoord()),
                        List.of(buildElementary(sp.elementaireVoorwaarde())), bron(sp));
            // §13.4.14.6 re-nest a column-absorbed bullet list (the SAME depth-aware machinery as a voorwaarde compound)
            List<Conditie> herNest = reNestParts(sp.samengesteldeVoorwaardeOnderdeel());
            List<Conditie> onderdelen;
            if (herNest != null) {
                onderdelen = herNest;
            } else {
                onderdelen = new ArrayList<>();
                for (GenesteVoorwaardeContext g : sp.samengesteldeVoorwaardeOnderdeel().genesteVoorwaarde())
                    onderdelen.add(buildNested(g));     // the SAME bullet machinery as a voorwaarde compound
            }
            Conditie.Kwantor kw = quantifierQuantification(sp.kwantificatie());
            // §13.4.14.6 IR-integrity guard: a nested-compound bullet with a DEEPER compound child under a
            // COUNTING quantifier stays Onopgelost — the indentation-blind bullet grammar can flatten sibling
            // bullets into a depth-2 child in ways the column re-nesting cannot always rebuild, and a
            // mis-merged bullet under a COUNT changes what is counted (AND-merging what the spec OR-s): a
            // fired-wrong shared value. Under ALLE/DE the merge is semantics-preserving (every bullet must
            // hold anyway), so no guard applies. This is a statement about the PARSE, not about what a
            // back-end can lower (back-end-agnostic resolver): capability verdicts live in the back-ends and are reconciled
            // by the manifest-parity test.
            if (kw != Conditie.Kwantor.ALLE && kw != Conditie.Kwantor.DE
                    && onderdelen.stream().anyMatch(c -> c instanceof Conditie.Samengesteld s
                            && s.onderdelen().stream().anyMatch(x -> x instanceof Conditie.Samengesteld)))
                return new Conditie.Unresolved(bron(sp),
                        "een tellende kwantor over een deelselectie waarvan een bullet zelf nog een diepere "
                        + "samengestelde voorwaarde draagt: de inspringblinde bullet-grammatica kan zusterbullets "
                        + "in die diepere laag getrokken hebben (§13.4.14.6), en dan telt de selectie een andere "
                        + "verzameling dan de zin noemt");
            return new Conditie.Samengesteld(kw,
                    sp.kwantificatie() == null ? 0 : numeralValue(sp.kwantificatie().telwoord()), onderdelen, bron(sp));
        } finally {
            builder.setSubject(previous);                   // RESTORE
        }
    }

    /** §13.4.14.5 kwantificatie (used by the compound subselectie) -> Kwantor. Distinct from voorwaardeKwantificatie:
     *  it admits 'al' (AL, treated as ALLE) and ALLE has no "( van de )?" tail. A bare DE quantifies the whole set. */
    private static Conditie.Kwantor quantifierQuantification(KwantificatieContext k) {
        if (k == null || k.ALLE() != null || k.AL() != null) return Conditie.Kwantor.ALLE;
        if (k.GEEN_VAN_DE() != null) return Conditie.Kwantor.GEEN;
        if (k.TEN_MINSTE() != null)  return Conditie.Kwantor.TEN_MINSTE;
        if (k.TEN_HOOGSTE() != null) return Conditie.Kwantor.TEN_HOOGSTE;
        if (k.PRECIES() != null)     return Conditie.Kwantor.PRECIES;
        return Conditie.Kwantor.DE;
    }

    /** A telwoord (§13.4.14.4): a written één/twee/drie/vier or a NUMBER, else 0 (no/unparseable count).
     *  Package-private so the onderwerp's own quantifier reads the SAME parse as the composite's
     *  (ExpressionBuilder.writtenThreshold) — one telwoord reading, not two. */
    static int numeralValue(TelwoordContext t) {
        if (t == null) return 0;
        if (t.NUMBER() != null) try { return Integer.parseInt(t.NUMBER().getText()); } catch (NumberFormatException e) { return 0; }
        if (t.EEN_TELWOORD() != null)  return 1;
        if (t.TWEE_TELWOORD() != null) return 2;
        if (t.DRIE_TELWOORD() != null) return 3;
        if (t.VIER_TELWOORD() != null) return 4;
        return 0;
    }

    /** §13.4.8 samengestelde consistentie-criteria → a {@link Conditie.Samengesteld} over the criteria. */
    private Conditie buildCriteria(ConsistentieKwantificatieContext k, SamengesteldCriteriumOnderdeelContext component, Bron bron) {
        List<Conditie> onderdelen = new ArrayList<>();
        for (GenestCriteriumContext g : component.genestCriterium()) onderdelen.add(buildCriterion(g));
        return new Conditie.Samengesteld(quantifierConsistency(k), k == null ? 0 : numeralValue(k.telwoord()), onderdelen, bron);
    }

    private Conditie buildCriterion(GenestCriteriumContext g) {
        if (g.samengesteldCriterium() != null) {
            SamengesteldCriteriumContext c = g.samengesteldCriterium();
            return buildCriteria(c.consistentieKwantificatie(), c.samengesteldCriteriumOnderdeel(), bron(c));
        }
        if (g.elementaireVoorwaarde() != null) return buildElementary(g.elementaireVoorwaarde());
        // GEEN GETUIGE: genestCriterium draagt altijd een van beide takken, en een consistentieregel met
        // een kapot criterium strandt eerder — op de syntaxfout, met haar eigen ERROR. Gemeten 2026-09-19.
        return new Conditie.Unresolved(bron(g),
                "dit geneste consistentiecriterium is geen samengesteld criterium en geen elementaire "
                + "voorwaarde (§13.4.8)");
    }

    /** §13.4.14.3 consistentie-kwantor; null (the single "het volgende criterium") is the trivial ALLE. */
    private static Conditie.Kwantor quantifierConsistency(ConsistentieKwantificatieContext k) {
        if (k == null || k.ALLE() != null) return Conditie.Kwantor.ALLE;
        if (k.GEEN_VAN_DE() != null)  return Conditie.Kwantor.GEEN;
        if (k.TEN_MINSTE() != null)   return Conditie.Kwantor.TEN_MINSTE;
        if (k.TEN_HOOGSTE() != null)  return Conditie.Kwantor.TEN_HOOGSTE;
        if (k.PRECIES() != null)      return Conditie.Kwantor.PRECIES;
        return Conditie.Kwantor.ALLE;
    }

    /** Every criterion's left operand is about {@code subject} (a literal/parameter left side is fine):
     *  the rule lowers to one subject pattern, so a criterion over another object type cannot be lifted. */
    private static boolean oneSubject(Conditie c, Objecttype subject) {
        if (c instanceof Conditie.Vergelijking v)
            return !(v.links() instanceof Expr.AttribuutRef a) || a.objecttype() == subject;
        if (c instanceof Conditie.Predicaat p)
            return !(p.operand() instanceof Expr.AttribuutRef a) || a.objecttype() == subject;
        if (c instanceof Conditie.Samengesteld s)
            return s.onderdelen().stream().allMatch(d -> oneSubject(d, subject));
        return true;
    }

    /** The type the criteria are about — the object type of the first criterion's left operand. */
    private static Objecttype subjectAssertion(Conditie c) {
        if (c instanceof Conditie.Vergelijking v && v.links() instanceof Expr.AttribuutRef a) return a.objecttype();
        if (c instanceof Conditie.Predicaat p && p.operand() instanceof Expr.AttribuutRef a) return a.objecttype();
        if (c instanceof Conditie.Samengesteld s)
            for (Conditie d : s.onderdelen()) { Objecttype t = subjectAssertion(d); if (t != null) return t; }
        return null;
    }

    private Conditie buildNested(GenesteVoorwaardeContext g) {
        if (g.genesteSamengesteldeVoorwaarde() != null) {
            GenesteSamengesteldeVoorwaardeContext c = g.genesteSamengesteldeVoorwaarde();
            return buildCompound(c.voorwaardeKwantificatie(), c.samengesteldeVoorwaardeOnderdeel(), c);
        }
        if (g.elementaireVoorwaarde() != null) return buildElementary(g.elementaireVoorwaarde());
        return new Conditie.Unresolved(bron(g),
                "deze bullet van de samengestelde voorwaarde is geen geneste samengestelde voorwaarde en "
                + "geen elementaire voorwaarde (§13.4.14)");
    }

    /** An §13.4.15 elementaire voorwaarde / consistentie-criterium ("&lt;expressie&gt; &lt;operator&gt;") to a
     *  {@link Conditie}; shared by the samengestelde voorwaarde and the samengestelde consistentieregel. */
    private Conditie buildElementary(ElementaireVoorwaardeContext c) {
        if (c.expressie() != null && c.nonToplevelOperator() != null)
            return conditionFromNonToplevel(builder.build(c.expressie()), c.expressie(), c.nonToplevelOperator(), bron(c));
        if (c.expressie() != null && c.toplevelOperator() != null)
            return conditionFromToplevel(builder.build(c.expressie()), c.expressie(), c.toplevelOperator(), bron(c));
        // §13.4.15.69 / §8.4.3: the "het is de periode <periodevergelijkingEnkelvoudig>" bullet form.
        if (c.periodevergelijkingElementair() != null)
            return buildTimeWindow(c.periodevergelijkingElementair().periodevergelijkingEnkelvoudig());
        // §8.1.9 / §13.4.15.66: the "regelversie <X> is gevuurd|inconsistent" regelpredicaat as a bullet.
        if (c.consistentieVoorwaarde() != null) return buildRuleStatusCheck(c.consistentieVoorwaarde());
        // GEEN GETUIGE: elementaireVoorwaarde heeft precies drie takken en alle drie staan hierboven, dus
        // een gevulde context bereikt deze regel niet; een kapotte bullet valt een niveau hoger uit
        // (buildNested, dat wel een getuige heeft). Gemeten 2026-09-19.
        return new Conditie.Unresolved(bron(c),
                "de elementaire voorwaarde heeft geen van de §13.4.15-vormen — geen expressie met operator, "
                + "geen periodevergelijking en geen regelpredicaat");
    }

    /** §8.1.9 / §13.4.15.66/.18 regelpredicaat to a {@link Conditie.RuleStatusCheck}. The reference may point
     *  FORWARD to a rule defined later in the document, so no existence check happens here — the back-ends
     *  verify the name against the model's Inconsistentie producers and decline an unknown reference as an
     *  honest marker (never a silent never-matching exists). The parenthesised versie-aanduiding is parsed
     *  but only carried in bron (see Conditie.RuleStatusCheck). */
    private Conditie buildRuleStatusCheck(ConsistentieVoorwaardeContext c) {
        return new Conditie.RuleStatusCheck(src(c.vrijeNaam()), c.INCONSISTENT() != null, bron(c));
    }

    /** §8.2 / §13.4.15.70: the optional "(niet) gedurende de/het gehele jaar|maand" whole-period qualifier that
     *  precedes a predicate (verb-split or verb-leading); null when absent. {@code jaar} reads the JAAR vs MAAND
     *  keyword, {@code niet} the leading negation. The transpilers honour it on a tijdsafhankelijk onderwerp. */
    private static Conditie.WholePeriod wholePeriod(GehelePeriodevergelijkingContext g) {
        if (g == null) return null;
        boolean year = g.JAAR() != null;   // else MAAND
        // the negation is either the bare NIET prefix (stellende vorm "is niet gedurende …") or the combined
        // NIET_GEDURENDE_*_GEHELE token (vragende vorm "… niet gedurende …", lexed as one token so the onderwerp
        // kaleNaam cannot absorb the leading "niet").
        boolean negated = g.NIET() != null || g.NIET_GEDURENDE_HET_GEHELE() != null || g.NIET_GEDURENDE_DE_GEHELE() != null;
        return new Conditie.WholePeriod(year, negated);
    }

    /** §8.1.4/§13.4.15.84 the &lt;geheelgetal&gt; digit-count N of "numeriek met exact N cijfers". The NUMBER token is
     *  over-broad (it also matches a §13.2.4 decimaalgetal "9,5"), but the spec requires a §13.2.3 &lt;geheelgetal&gt;
     *  (which DOES admit a leading minus); reject a decimal/rational count with an ERROR rather than silently coercing
     *  it to 0 (an inert always-false predicate). 0 if absent. */
    private int countDigits(org.antlr.v4.runtime.tree.TerminalNode num) {
        if (num == null) return 0;
        String t = num.getText();
        if (!t.matches("-?\\d+")) {
            model.diagnostics.add(new Diagnostic(Severity.ERROR,
                "ongeldig aantal cijfers '" + t + "' bij 'numeriek met exact … cijfers' (verwacht een geheel getal, §8.1.4)", 0));
            return 0;
        }
        return Integer.parseInt(t);
    }

    /** The (operand, negatie) of an elfproef / numeriek-met-exact predicate. The §8.1 Tabel-16 vragende negated form
     *  "&lt;X&gt; niet aan de elfproef voldoet" / "&lt;X&gt; niet numeriek is met exact N cijfers" has its leading
     *  "niet" greedily absorbed into the operand's kaleNaam (NIET is a kaleNaamWoord), so op.NIET() is null; recover
     *  it by stripping the absorbed " niet" off the operand (verbatim-first via {@link #stripAbsorbedNiet}). The
     *  stellende form carries an explicit NIET token, so {@code expliciet} short-circuits the strip. */
    private record ElfOperand(Expr operand, boolean negatie) {}
    private static ElfOperand elfOperand(boolean expliciet, Expr lhs) {
        // The stellende form carries an explicit NIET token; otherwise recover a vragende negator the greedy parse
        // absorbed into the operand (ExpressionBuilder.subjectPossessive resolves "zijn <attr> niet" to the BARE attribuut but
        // keeps the full bron, so the absorbed "niet" survives as the bron's trailing word).
        return new ElfOperand(lhs, expliciet || absorbedNegator(lhs));
    }

    /** Whether {@code lhs} is an operand into whose source the greedy parse absorbed a trailing "niet" negator — an
     *  {@link Expr.AttribuutRef} whose bron ends in " niet" but whose (bare) attribuut does NOT end in "niet"
     *  (verbatim-first: an attribuut literally named "… niet" carries its own niet and must not be de-negated). The
     *  elfproef / numeriek-met-exact predicates RECOVER this as their negatie; every OTHER operator cannot honour a
     *  dangling negator, so it must marker rather than silently emit the inverted positive. */
    private static boolean absorbedNegator(Expr lhs) {
        return lhs instanceof Expr.AttribuutRef a && a.bron() != null
                && a.bron().snippet().stripTrailing().toLowerCase().endsWith(" niet")
                && !a.attribuut().toLowerCase().endsWith("niet");
    }

    /** The two reasons the verb-trailing and verb-leading registers share verbatim: the SAME refusal is
     *  written twice because the grammar spells the operator twice, and one text per axis keeps the
     *  author's two readings of one sentence identical. */
    private static final String REASON_NOT_ABSORBED =
            "de gulzige ontleding trok de ontkenning 'niet' in het onderwerp; een vergelijking of een "
            + "leeg-/gevuld-controle kan haar daar niet uit terughalen, en de positieve vorm emitteren zou "
            + "de vraag omkeren (§8.1)";
    private static final String REASON_NO_CHARACTERISTIC_NO_ROLE =
            "de geschreven naam bindt geen kenmerk en geen rol van het onderwerp (§8.1.7/§8.1.8), dus er is "
            + "geen lidmaatschap om te toetsen";

    /** {@code lhsCtx} is the parse subtree the left operand was built from: the §5.6 die/dat
     *  subselectie lives there, and {@link #quantifierFold} needs the tree, not the Expr. */
    private Conditie conditionFromToplevel(Expr lhs, ExpressieContext lhsCtx, ToplevelOperatorContext op, Bron bron) {
        Conditie plain = conditionFromToplevelBare(lhs, op, bron);
        if (plain instanceof Conditie.Unresolved && op.gehelePeriodevergelijking() == null) {
            Conditie fold = quantifierCharacteristicCheck(lhs, lhsCtx, op.kaleNaam(), op.EEN() != null,
                    op.HEEFT() != null || op.HEBBEN() != null, bron);
            if (fold != null) return fold;
        }
        return quantifierFold(checkWholePeriod(plain, wholePeriod(op.gehelePeriodevergelijking()), bron),
                lhsCtx, op.expressie(), bron);
    }

    private Conditie conditionFromToplevelBare(Expr lhs, ToplevelOperatorContext op, Bron bron) {
        Conditie.WholePeriod periode = wholePeriod(op.gehelePeriodevergelijking());   // §8.2 whole-period check
        // A dangling absorbed "niet" silently INVERTS a comparison / leeg-gevuld (the bare-attr resolution dropped the
        // negator) -> marker it. The kenmerkcheck DE-negates it (bindCharacteristicCheck) and elfproef/numeriek RECOVER it
        // (elfOperand), so only the comparison + leeg/gevuld operators are guarded here.
        if (absorbedNegator(lhs) && (op.vergelijkingsTekenGesplitst() != null || op.LEEG() != null || op.GEVULD() != null))
            return new Conditie.Unresolved(bron, REASON_NOT_ABSORBED);
        if (op.vergelijkingsTekenGesplitst() != null && op.expressie() != null)
            return ExpressionBuilder.comparison(toOperator(cmpOf(op.vergelijkingsTekenGesplitst())),
                    lhs, builder.build(op.expressie()), periode, bron);
        if (op.LEEG() != null)   return new Conditie.Predicaat(Conditie.PredicateKind.LEEG, lhs, periode, bron);
        if (op.GEVULD() != null) return new Conditie.Predicaat(Conditie.PredicateKind.GEVULD, lhs, periode, bron);
        if (op.AAN_DE_ELFPROEF() != null) {
            ElfOperand e = elfOperand(op.NIET() != null, lhs);   // recover an absorbed vragende "niet"
            return new Conditie.Predicaat(Conditie.PredicateKind.ELFPROEF, e.operand(), e.negatie(), periode, bron);
        }
        if (op.NUMERIEK_IS_MET_EXACT() != null || op.NUMERIEK_ZIJN_MET_EXACT() != null) {
            ElfOperand e = elfOperand(op.NIET() != null, lhs);
            return new Conditie.Predicaat(Conditie.PredicateKind.NUMERIEK_MET_EXACT, e.operand(), e.negatie(),
                    countDigits(op.NUMBER()), periode, bron);
        }
        Conditie kenmerk = characteristicCheckPredicate(lhs, op.kaleNaam(), op.EEN() != null,
                op.HEEFT() != null || op.HEBBEN() != null, periode, bron); // EEN? kaleNaam (is|heeft): §8.1.8
        // else objecttype/navigation membership — the caller may still read the whole sentence as a
        // quantifier fold (quantifierCharacteristicCheck), which is exactly why this marker carries its reason instead
        // of raising it here: reported at construction it would flag every sentence that folds cleanly.
        return kenmerk != null ? kenmerk : new Conditie.Unresolved(bron, REASON_NO_CHARACTERISTIC_NO_ROLE);
    }

    private Conditie conditionFromNonToplevel(Expr lhs, ExpressieContext lhsCtx, NonToplevelOperatorContext op, Bron bron) {
        Conditie plain = conditionFromNonToplevelBare(lhs, op, bron);
        // op.NIET() guards the §8.1.8.3 "is niet een <k>" branch above: it resolves the POSITIVE form and
        // flips it, so a null binding there arrives as an Onopgelost whose negation is not in the parse
        // tree any more. Re-binding it here would emit the POSITIVE membership — silently inverted.
        if (plain instanceof Conditie.Unresolved && op.gehelePeriodevergelijking() == null && op.NIET() == null) {
            Conditie fold = quantifierCharacteristicCheck(lhs, lhsCtx, op.kaleNaam(), op.EEN() != null,
                    op.HEEFT() != null || op.HEBBEN() != null, bron);
            if (fold != null) return fold;
        }
        return quantifierFold(checkWholePeriod(plain, wholePeriod(op.gehelePeriodevergelijking()), bron),
                lhsCtx, op.expressie(), bron);
    }

    /** §8.2: the "gedurende de/het gehele &lt;periode&gt;" check is only defined when the predicate's
     *  onderwerp is TIJDSAFHANKELIJK. A qualifier over a static onderwerp is a spec error — reject it (an ERROR +
     *  Onopgelost) rather than silently markering a rule that can never fire. {@code niet} is irrelevant here (a
     *  whole-period NEGATION of a static check is just as ill-defined). */
    private Conditie checkWholePeriod(Conditie c, Conditie.WholePeriod periode, Bron bron) {
        if (periode == null || subjectIsTemporal(c)) return c;
        // the verifier refuses the qualifier on a comparison, a predicate and an own characteristic (WF-043); a
        // navigated characteristic and the other condition kinds it does not check, so those keep their own words
        String sentence = c instanceof Conditie.Vergelijking || c instanceof Conditie.Predicaat
                ? "wholePeriod qualifier on a non-time-dependent operand"
                : c instanceof Conditie.Kenmerk k && k.navigatie() == null
                ? "wholePeriod qualifier on a non-time-dependent characteristic" : null;
        model.diagnostics.add(new Diagnostic(Severity.ERROR, sentence != null
            ? "de check op volledige periode (gedurende de/het gehele jaar/maand): " + sentence
            : "de check op volledige periode (gedurende de/het gehele jaar/maand) kan alleen op een tijdsafhankelijk "
            + "onderwerp worden toegepast (§8.2): " + bron, bron.line()));
        return new Conditie.Unresolved(bron, null);   // the ERROR above is this marker's diagnosis
    }

    /**
     * §13.4.2.9 + §13.4.15 / IR-SPEC ch. 4.5: lower the official sentence shape "… indien de
     * &lt;attribuut&gt; van ALLE &lt;leden&gt; van de &lt;Onderwerp&gt; groter is dan &lt;grens&gt;" to a
     * {@link Conditie.QuantifierFold} — a first-order quantification over the members of a linked instance
     * set. Without it the condition keeps a set operand in a slot that takes one value, and the set
     * fence refuses the whole document: the sentence is officially sanctioned RegelSpraak that this
     * frontend could parse and resolve but never emit.
     *
     * <p>The §8.2 whole-period qualifier is OPTIONAL here, exactly as v2.3.0 writes it. Quantification
     * (§13.4.2.9 {@code kwantificatie?}, §13.4.17.5) and the whole-period check (§13.4.15.70
     * {@code gehelePeriodevergelijking?}, optional in every §13.4.15.19-.48/.71-.100 operator) are
     * independent productions, so "alle X die Y groter zijn dan N" is as much RegelSpraak as its
     * qualified sibling; until 2026-09-19 this lowering opened on a qualifier test and refused the plain
     * spelling, which put a FRONTEND boundary where the language has none (maintainer reading
     * 2026-09-17). The matrix is then an ordinary, non-temporal condition of the member
     * (RUNTIME-SEMANTICS ch. 3.9a, whose formula is stated over an arbitrary {@code test}).
     *
     * <p>Scope is exactly ONE set operand in a CONDITION. Every other producer route of the fence — a rule
     * value, a composite operand, a gelijkstelling/initialisatie value, an objectcreatie toekenning —
     * keeps raising its ERROR verbatim, because for those the set reading really is a collection in a
     * one-value slot and there is no quantifier to give it a home. Widening to a value position is a spec
     * question (which quantified form means what there), not a frontend one. The §13.4.8 consistentieregel
     * ASSERTION is fenced too, and on its own carrier's terms rather than the wire's — see
     * {@link #consistency}.
     *
     * <p>All five §13.4.14 quantifiers over a set lower: ALLE and GEEN, and — since 2026-09-16 — the three
     * COUNTING quantors, which carry the written telwoord as the fold's {@code drempel} (IR-SPEC ch. 4.5,
     * WF-108 (e); the verdict counts the contributors the matrix holds of). A telwoord that is not a
     * positive whole number declines and keeps the fence's error: "ten minste 0 van de" would read as a
     * forall or as nothing at all depending on which arm saw it, and WF-041 refuses the wire form, so
     * emitting it would only move the refusal downstream.
     *
     * <p>Two further shapes decline and keep the fence's error rather than lowering, each because lowering
     * would be a guess: a MULTI-STEP written range (the operand
     * resolved a {@code rangePath}), whose {@code sourcePath} spelling is a separate card; and a range that
     * does not resolve at all — a quantifier bound to no relation is the silent-wrong-set failure a required
     * range prevents (WF-048 until L2; since L2 the quantifier fold carries its range by schema), and a loud
     * refusal beats a fold over a set the sentence never named.
     *
     * <p>A FOURTH shape declines for the same reason the other three do, and it is the §5.6 die/dat
     * SUBSELECTION: a subselectie this resolver cannot bind against the element type leaves the fence's
     * ERROR standing and draws a WARNING naming itself. Dropping it would be worse than refusing —
     * "alle vondsten DIE AANGEMELD ZIJN" would land as a fold over every vondst, a verifier-clean wire
     * form carrying a set the sentence never named, which every executor then answers consistently and
     * wrongly. That is the one failure this node's {@code filter} field exists to prevent.
     */
    private Conditie quantifierFold(Conditie c, ExpressieContext lhsCtx, ExpressieContext rhsCtx, Bron bron) {
        Expr.AttribuutRef set = onlySetOperand(c);
        if (set == null) return c;
        Conditie.Kwantor q = set.kwantor();
        if (q == null) return c;                             // bare "de": no set reading to quantify
        boolean counting = q == Conditie.Kwantor.TEN_MINSTE || q == Conditie.Kwantor.TEN_HOOGSTE
                || q == Conditie.Kwantor.PRECIES;
        int threshold = counting ? set.threshold() : 0;
        if (counting && threshold < 1) {
            model.diagnostics.add(new Diagnostic(Severity.WARNING,
                "het telwoord van de kwantor in '" + set.bron() + "' is geen positief geheel getal (>= 1), dus de "
                + "vouw wordt NIET gelowerd: een drempel onder 1 stelt geen telling die iets kan voldoen en wordt "
                + "op de wire geweigerd (WF-041, IR-SPEC ch. 4.5); de setlezing blijft daarmee buiten een vouwbron "
                + "staan (WF-053)", set.bron().line()));
            return c;
        }
        Objecttype elem = set.objecttype();
        if (elem == null) return c;
        // a written multi-step range resolves into rangePath, which this lowering does not spell — decline
        // the WHOLE thing rather than binding the first step, which would quantify over the wrong set
        if (set.rangePath() != null && !set.rangePath().isEmpty()) return c;
        RegelSpraakModel.RoleBindingRef sourceRole =
                model.foldSourceBinding(builder.subject(), elem, set.subjectSource());
        if (sourceRole == null) return c;                       // unresolvable range — never a silent misbinding
        // §5.6 / ch. 4.5 the die/dat SUBSELECTION, looked for in the parse subtree of the QUANTIFIED
        // operand only: the bijzin trails the onderwerp ("… van alle vondsten van de Jutter die
        // aangemeld zijn") but selects the quantified MEMBERS, so a die on the other side of the
        // comparison belongs to that operand's own navigation and is none of this fold's business.
        ExpressionBuilder.Subselection ds = builder.subselection(
                c instanceof Conditie.Vergelijking v && v.rechts() == set ? rhsCtx : lhsCtx, elem, set);
        if (ds != null && ds.filter() == null) {
            model.diagnostics.add(new Diagnostic(Severity.WARNING,
                "de 'die/dat'-deelselectie in '" + set.bron() + "' wordt niet omgezet naar een "
                + "kwantorvouw-filter: " + ds.reason() + "; de vouw wordt daarom NIET gelowerd — een vouw "
                + "over het volle bereik zou een andere verzameling kwantificeren dan de zin noemt, dus de "
                + "setlezing blijft buiten een vouwbron staan (WF-053)", set.bron().line()));
            return c;
        }
        Conditie filter = ds == null ? null : ds.filter();
        // The matrix: the SAME condition, with the quantified operand replaced by an own, element-subject
        // read of the same attribute. The §8.2 qualifier stays ON it — per member, the whole-period test is
        // what each contributor must satisfy. No navigation role/path/range and no quantor survive: the
        // element IS the subject there, which is also why no set reading is left for the fence to find.
        Expr.AttribuutRef own = new Expr.AttribuutRef(set.attribuut(), elem, set.subjectSource(), null,
                set.type(), set.bron(), false, List.of(), List.of(), null);
        Conditie matrix = elementRead(withOperand(c, set, own), own, elem,
                c instanceof Conditie.Vergelijking v && v.links() == set ? rhsCtx : lhsCtx, bron);
        return new Conditie.QuantifierFold(q, threshold, elem, filter, matrix, sourceRole, List.of(), bron);
    }

    /**
     * §13.4.1 / RUNTIME-SEMANTICS ch. 3.9a: the matrix of a quantifier fold reads with the element as subject, but the
     * operand beside the quantified one was built with the rule subject. A read there that names the element by its
     * type ("de sluitdag van de jutter van de Vondst") then walked nothing — from the rule subject the element is a
     * to-many step — and was refused as a read not laid as a navigation, where "de sluitdag van zijn jutter" bound and
     * the same read binds in a subselection. Such an operand is built again with the element as subject, and replaces
     * the first build only where that left a read of a member the element does not have on the element and the second
     * build leaves none; every operand that bound stays as it was. The comparison is typed again over the new operand,
     * since the first typing saw an unresolved one.
     */
    private Conditie elementRead(Conditie matrix, Expr own, Objecttype elem, ExpressieContext otherCtx, Bron bron) {
        if (!(matrix instanceof Conditie.Vergelijking v) || otherCtx == null) return matrix;
        boolean ownLeft = v.links() == own;
        Expr other = ownLeft ? v.rechts() : v.links();
        if (!readsUnknownMemberOf(other, elem)) return matrix;
        Objecttype previous = builder.subject();
        builder.setSubject(elem);
        Expr again;
        try {
            again = builder.build(otherCtx);
        } finally {
            builder.setSubject(previous);
        }
        if (readsUnknownMemberOf(again, elem)) return matrix;
        Expr links = ownLeft ? own : again, rechts = ownLeft ? again : own;
        reportCompare(toCmpOp(v.op()), links.type(), rechts.type(), false, false, bron.line(), null, null);
        return ExpressionBuilder.comparison(v.op(), links, rechts, v.periode(), v.bron());
    }

    /** Whether {@code e} reads, on {@code elem} itself, a member {@code elem} does not have — the read a chain that
     *  starts at the element leaves when it is walked from another subject. Arithmetic, a rounding and a clamp are
     *  looked into; another shape is no such read. */
    private static boolean readsUnknownMemberOf(Expr e, Objecttype elem) {
        if (e instanceof Expr.AttribuutRef a)
            return a.objecttype() == elem && a.attribuut() != null && !elem.hasMember(a.attribuut());
        if (e instanceof Expr.Binair b) return readsUnknownMemberOf(b.links(), elem) || readsUnknownMemberOf(b.rechts(), elem);
        if (e instanceof Expr.Afgerond a) return readsUnknownMemberOf(a.basis(), elem);
        if (e instanceof Expr.Begrensd b)
            return readsUnknownMemberOf(b.basis(), elem) || readsUnknownMemberOf(b.minimum(), elem)
                    || readsUnknownMemberOf(b.maximum(), elem);
        return false;
    }

    /** The typing operator of a condition operator, {@link #toOperator} inverted. */
    private static Typing.CmpOp toCmpOp(Conditie.Operator op) {
        return switch (op) {
            case KLEINER -> Typing.CmpOp.KLEINER;
            case KLEINER_OF_GELIJK -> Typing.CmpOp.KLEINER_OF_GELIJK;
            case GROTER -> Typing.CmpOp.GROTER;
            case GROTER_OF_GELIJK -> Typing.CmpOp.GROTER_OF_GELIJK;
            case GELIJK -> Typing.CmpOp.GELIJK;
            case ONGELIJK -> Typing.CmpOp.ONGELIJK;
            case EERDER -> Typing.CmpOp.EERDER;
            case EERDER_OF_GELIJK -> Typing.CmpOp.EERDER_OF_GELIJK;
            case LATER -> Typing.CmpOp.LATER;
            case LATER_OF_GELIJK -> Typing.CmpOp.LATER_OF_GELIJK;
        };
    }

    /**
     * §13.4.15.44/.45 + ch. 4.5: the sibling of {@link #quantifierFold} for a MEMBERSHIP matrix — "… indien
     * ALLE wachten van de Schouwploeg gediplomeerd zijn". Here the quantified onderwerp IS the whole
     * operand, so it names no attribute and arrives as no reference at all: {@link #onlySetOperand}
     * finds nothing, the fence does not fire (there is no set READING to fence), and the condition
     * stayed an {@link Conditie.Unresolved} marker. The quantifier is nevertheless written and the matrix
     * is an ordinary kenmerkcheck of the member, so the fold has everything it needs — it just has to read
     * the onderwerp off the parse tree and bind the kenmerk against the ELEMENT type instead of the rule
     * subject (which is why {@link #characteristicCheckPredicate} could not bind it: "gediplomeerd" is no kenmerk of
     * the Schouwploeg).
     *
     * <p>Every decline of {@link #quantifierFold} holds verbatim, for the same reasons: a counting telwoord
     * under 1 (WF-041), a multi-step written range (declined in {@code quantifiedSubject}), an
     * unresolvable range, and a die/dat subselectie that does not bind. Each returns null, which leaves
     * the caller's honest marker standing. Two shapes stay
     * out on top of those: a §8.2-QUALIFIED membership matrix (the caller gates on that — a whole-period
     * quantified kenmerkcheck has no worked semantics here and keeps its marker), and a §8.1.7 ROLCHECK
     * of the element, which is a different quantified question and binds nothing here.
     *
     * <p>Every one of those declines is LOUD (maintainer decision 2026-09-19). Four of them used to be
     * silent — the bare "de", the §8.1.7 rolcheck of the element, a multi-step range and an unbindable
     * one — beside three that always spoke, and a sentence that reads as a quantification is exactly the
     * one whose author cannot see a marker coming. The marker itself is unchanged: the WARNING says which
     * axis the fold declined on, and the document stays emittable.
     */
    private Conditie quantifierCharacteristicCheck(Expr lhs, ExpressieContext lhsCtx, KaleNaamContext nameCtx, boolean withIndefiniteArticle,
                                    boolean hasVerb, Bron bron) {
        if (nameCtx == null || lhsCtx == null) return null;
        ExpressionBuilder.QuantifiedSubject g = builder.quantifiedSubject(lhsCtx);
        if (g == null) return null;                            // no member phrase written — nothing to say
        if (g.reason() != null) { reportFoldRefusal(g.subjectSource(), g.reason(), bron); return null; }
        boolean counting = g.kwantor() == Conditie.Kwantor.TEN_MINSTE
                || g.kwantor() == Conditie.Kwantor.TEN_HOOGSTE || g.kwantor() == Conditie.Kwantor.PRECIES;
        int threshold = counting ? g.threshold() : 0;
        if (counting && threshold < 1) {
            model.diagnostics.add(new Diagnostic(Severity.WARNING,
                "het telwoord van de kwantor in '" + g.subjectSource() + "' is geen positief geheel getal "
                + "(>= 1), dus de vouw wordt NIET gelowerd: een drempel onder 1 stelt geen telling die iets "
                + "kan voldoen en wordt op de wire geweigerd (WF-041, IR-SPEC ch. 4.5)", bron.line()));
            return null;
        }
        // the member phrase without its counting quantifier: "ten minste twee van de liggers van de Haven" names the
        // role 'liggers', and the whole phrase would offer "ten minste twee" as the written name
        String member = g.subjectSource().replaceFirst("(?i)^(ten minste|ten hoogste|precies)\\s+\\S+\\s+van\\s+", "")
                .replaceFirst("(?i)^geen\\s+van\\s+", "");
        RegelSpraakModel.RoleBindingRef sourceRole = model.foldSourceBinding(builder.subject(), g.element(), member);
        if (sourceRole == null) {                                 // unresolvable range — never a silent misbinding
            reportFoldRefusal(g.subjectSource(), (builder.subject() == null
                    ? "het regelonderwerp is onbekend"
                    : pairReason(builder.subject(), g.element(), member)) + ", dus het bereik van de kwantor is niet te "
                    + "binden — een gegokte relatie zou een andere verzameling vouwen dan de zin noemt (IR-SPEC ch. 4.5)",
                    bron);
            return null;
        }
        // The anchor names the rule subject, and what the parse took after that name belongs to the predicate:
        // a negation, and the leading words of a multi-word name. From an anchor anchorTail cannot read, only a
        // trailing negation word is taken back.
        String suffix = builder.anchorTail(lhsCtx, anchorNames(builder.subject(), sourceRole));
        if (suffix == null) suffix = absorbedNegation(lhs);
        String naam = (suffix.isEmpty() ? "" : suffix + " ") + stripArticle(src(nameCtx));
        Conditie test = elementCharacteristicCheck(g.element(), naam, withIndefiniteArticle, hasVerb, bron);
        if (test == null) {
            reportFoldRefusal(g.subjectSource(),
                    "'" + naam.replaceFirst("^(?i)(niet|geen)\\s+", "") + "' is geen kenmerk van " + g.element().naam
                    + " — een §8.1.7-rolcontrole van het element is een andere gekwantificeerde vraag, en een "
                    + "kenmerk van het regelonderwerp op het element lezen zou iets anders beantwoorden dan de "
                    + "zin vraagt (§8.1.7/§8.1.8)", bron);
            return null;
        }
        ExpressionBuilder.Subselection ds = builder.subselection(lhsCtx, g.element(), null);
        if (ds != null && ds.filter() == null) {
            // The comparison route can stop at a WARNING here because the set reading it leaves standing
            // raises the fence's ERROR that blocks the document. This route has no set reading, so the
            // subselectie goes back on checkPartSelections' refusal list and the ERROR comes from there —
            // the WARNING beside it names the axis the binding failed on, which that ERROR cannot.
            builder.returnSubselection(lhsCtx);
            model.diagnostics.add(new Diagnostic(Severity.WARNING,
                "de 'die/dat'-deelselectie in '" + g.subjectSource() + "' wordt niet omgezet naar een "
                + "kwantorvouw-filter: " + ds.reason() + "; de vouw wordt daarom NIET gelowerd — een vouw "
                + "over het volle bereik zou een andere verzameling kwantificeren dan de zin noemt",
                bron.line()));
            return null;
        }
        return new Conditie.QuantifierFold(g.kwantor(), threshold, g.element(), ds == null ? null : ds.filter(),
                test, sourceRole, List.of(), bron);
    }

    /** The names the anchor of a quantified member phrase may call the rule subject by: the type names along
     *  its chain, and the declared name and plural of each role of the fold's relation that the subject's
     *  chain fills. {@link ExpressionBuilder#anchorTail} splits the anchor after the longest of them. */
    private List<String> anchorNames(Objecttype subject, RegelSpraakModel.RoleBindingRef range) {
        List<String> names = new ArrayList<>();
        List<Objecttype> keten = subject.keten();
        for (Objecttype t : keten) names.add(t.naam);
        RegelSpraakModel.Feittype ft = model.feittypen.get(range.feittype());
        if (ft != null)
            for (RegelSpraakModel.Rol r : ft.rollen)
                if (keten.contains(model.objecttypeOpNaam(r.objecttypeNaam()))) {
                    names.add(r.naam());
                    if (r.plural() != null) names.add(r.plural());
                }
        return names;
    }

    /** The negation word a quantified member phrase took into an anchor that {@link ExpressionBuilder#anchorTail}
     *  does not read: "niet" as {@link #absorbedNegator} reads it, or "geen" read the same way; empty when the
     *  operand ends in neither. */
    private static String absorbedNegation(Expr lhs) {
        if (absorbedNegator(lhs)) return "niet";
        return lhs instanceof Expr.AttribuutRef a && a.bron() != null
                && a.bron().snippet().stripTrailing().toLowerCase().endsWith(" geen")
                && !a.attribuut().toLowerCase().endsWith("geen") ? "geen" : "";
    }

    /** One decline of the membership fold, said out loud: which member phrase, on which axis, and what
     *  the sentence gets instead. Raised only where the phrase IS a quantified member phrase — a sentence
     *  that writes no quantifier gets no word about folds — and always a WARNING: the caller's marker is
     *  the honest wire form and the document stays emittable. */
    private void reportFoldRefusal(String subjectSource, String reason, Bron bron) {
        model.diagnostics.add(new Diagnostic(Severity.WARNING,
                "de kwantorvouw over '" + subjectSource + "' wordt NIET gelowerd: " + reason
                + "; de voorwaarde blijft daarom een marker", bron.line()));
    }

    /** The §8.1.8 kenmerkcheck of {@code elem} written as the predicate of a quantified onderwerp — the
     *  matrix of {@link #quantifierCharacteristicCheck}. Reuses the subject-anchored binders with the ELEMENT as the
     *  holder, so the §8.1.8.1/.2/.3 "niet"/"geen" de-negation, the EEN-form soort split and the Tabel-16
     *  verb lint all apply unchanged. Null when the name is no kenmerk of the element (a §8.1.7 rolcheck
     *  among them), which keeps the caller's marker rather than inventing a membership.
     *
     *  <p>{@code naam} is the name as written: the caller has put back in front of the operator's name what
     *  the greedy onderwerp parse pulled INTO the member phrase ("geen van de wachten van de Schouwploeg NIET
     *  gediplomeerd is" leaves the anchor's kaleNaam reading "Schouwploeg niet" and the operator's the bare
     *  kenmerk), so the ordinary verbatim-first de-negation handles a "niet" or "geen" there and a kenmerk
     *  literally named "niet …" or "geen …" still binds positively. */
    private Conditie elementCharacteristicCheck(Objecttype elem, String naam, boolean withIndefiniteArticle, boolean hasVerb, Bron bron) {
        if (withIndefiniteArticle)
            return hasVerb ? possessiveCharacteristicCheck(elem, naam, bron) : nominalCharacteristicCheck(elem, naam, bron);
        Conditie kc = characteristicCheckCondition(elem, naam, null, hasVerb, bron);
        if (kc != null) checkNegationKind(elem, kc, hasVerb);
        return kc;
    }

    /** The §8.2 whole-period qualifier of {@code c}, for the two condition forms that carry Expr operands
     *  (a {@link Conditie.Kenmerk} carries one too, but has no operand to quantify). Null otherwise. */
    private static Conditie.WholePeriod wholePeriodOf(Conditie c) {
        if (c instanceof Conditie.Vergelijking v) return v.periode();
        if (c instanceof Conditie.Predicaat p)    return p.periode();
        return null;
    }

    /** The one operand of {@code c} that is a §13.4.2 SET reading, or null when there is none or more than
     *  one — two quantified sets in one comparison are two ranges, and which one the sentence quantifies
     *  over is not something this lowering may pick. */
    private static Expr.AttribuutRef onlySetOperand(Conditie c) {
        Expr[] operands = c instanceof Conditie.Vergelijking v ? new Expr[]{v.links(), v.rechts()}
                : c instanceof Conditie.Predicaat p ? new Expr[]{p.operand()}
                : new Expr[0];
        Expr.AttribuutRef found = null;
        int n = 0;
        for (Expr e : operands)
            if (e instanceof Expr.AttribuutRef a && a.set()) { found = a; n++; }
        return n == 1 ? found : null;
    }

    /** {@code c} with the operand {@code oud} (matched by identity — it was just found there) replaced by
     *  {@code nieuw}; every other field, the §8.2 qualifier included, unchanged. */
    private static Conditie withOperand(Conditie c, Expr oud, Expr fresh) {
        if (c instanceof Conditie.Vergelijking v)
            return new Conditie.Vergelijking(v.op(), v.links() == oud ? fresh : v.links(),
                    v.rechts() == oud ? fresh : v.rechts(), v.periode(), v.bron());
        if (c instanceof Conditie.Predicaat p)
            return new Conditie.Predicaat(p.soort(), fresh, p.dayKind(), p.negatie(), p.digits(),
                    p.periode(), p.bron());
        return c;
    }

    /** Whether the onderwerp of a §8.2-qualified predicate is tijdsafhankelijk — a temporal attribuut on either side
     *  of a comparison, the operand of a leeg/gevuld/elfproef/numeriek predicate, or a temporal (own or to-one
     *  navigated) kenmerk. A rolcheck / unstructured form has no tijdsafhankelijk onderwerp. */
    private boolean subjectIsTemporal(Conditie c) {
        if (c instanceof Conditie.Vergelijking v) return exprTemporal(v.links()) || exprTemporal(v.rechts());
        if (c instanceof Conditie.Predicaat p)    return exprTemporal(p.operand());
        if (c instanceof Conditie.Kenmerk k)      return characteristicCheckTemporal(k);
        return false;
    }
    /** Whether an operand READS a TIJDSAFHANKELIJK value itself — a §13.3.6 tijdlijn attribuut (own or navigated) or a
     *  §3.8/§13.3.8 tijdlijn parameter. That is WF-043's reading of a time-dependent operand. §8.2 also admits such a
     *  read inside arithmetic ("zijn saldo plus 1"), an afronding, a begrenzing or a dimension cell, but the verifier
     *  refuses the qualifier there, so the frontend refuses it too, in the verifier's words, instead of writing a
     *  document the wire does not carry. */
    private static boolean exprTemporal(Expr e) {
        if (e instanceof Expr.AttribuutRef a)
            return a.objecttype() != null && a.objecttype().granulariteitVan(a.attribuut()) != null;
        if (e instanceof Expr.ParameterRef p)  return p.parameter().granulariteit() != null;
        return false;
    }
    private boolean characteristicCheckTemporal(Conditie.Kenmerk k) {
        Objecttype subject = builder.subject();
        Objecttype houder = k.navigatie() == null ? subject
                : (subject != null ? model.rolDoeltypeNaarSubject(subject, k.navigatie()) : null);
        return houder != null && houder.kenmerkGranulariteitVan(k.kenmerk()) != null;
    }

    private Conditie conditionFromNonToplevelBare(Expr lhs, NonToplevelOperatorContext op, Bron bron) {
        Conditie.WholePeriod periode = wholePeriod(op.gehelePeriodevergelijking());   // §8.2 whole-period check
        // A dangling absorbed "niet" silently INVERTS a comparison / leeg-gevuld -> marker it. The kenmerkcheck
        // DE-negates it and elfproef/numeriek RECOVER it (elfOperand), so only those two operators are guarded here.
        if (absorbedNegator(lhs) && (op.vergelijkingsTeken() != null || op.LEEG() != null || op.GEVULD() != null))
            return new Conditie.Unresolved(bron, REASON_NOT_ABSORBED);
        if (op.vergelijkingsTeken() != null && op.expressie() != null)
            return ExpressionBuilder.comparison(toOperator(cmpOf(op.vergelijkingsTeken())),
                    lhs, builder.build(op.expressie()), periode, bron);
        if (op.LEEG() != null)   return new Conditie.Predicaat(Conditie.PredicateKind.LEEG, lhs, periode, bron);
        if (op.GEVULD() != null) return new Conditie.Predicaat(Conditie.PredicateKind.GEVULD, lhs, periode, bron);
        if (op.AAN_DE_ELFPROEF() != null) {
            ElfOperand e = elfOperand(op.NIET() != null, lhs);   // recover an absorbed vragende "niet"
            return new Conditie.Predicaat(Conditie.PredicateKind.ELFPROEF, e.operand(), e.negatie(), periode, bron);
        }
        if (op.NUMERIEK_MET_EXACT() != null) {
            ElfOperand e = elfOperand(op.NIET() != null, lhs);
            return new Conditie.Predicaat(Conditie.PredicateKind.NUMERIEK_MET_EXACT, e.operand(), e.negatie(),
                    countDigits(op.NUMBER()), periode, bron);
        }
        // §8.1.8.3 "is niet een <k>": the explicit NIET EEN form (no AAN_DE_ELFPROEF / NUMERIEK_MET_EXACT keyword
        // follows) is the NEGATED zelfstandig kenmerkcheck — bind the positive EEN-form (is -> ZELFSTANDIG), then flip.
        if (op.NIET() != null && op.EEN() != null) {
            Conditie pos = characteristicCheckPredicate(lhs, op.kaleNaam(), true, false, periode, bron);
            // Tabel 16 sanctions "is niet een <k>" only for a KENMERK; a rolcheck negates as "is/heeft geen <rol>"
            // (§8.1.7). Lower it the same way (inverse rolcheck), but surface the unsanctioned writing as a WARNING.
            if (pos instanceof Conditie.Rollidmaatschap || pos instanceof Conditie.FarRoleMembership)
                model.diagnostics.add(new Diagnostic(Severity.WARNING,
                    "de schrijfwijze 'is niet een <rolnaam>' is voor een rolcheck niet voorzien; Tabel 16 kent alleen "
                    + "'is/heeft geen <rolnaam>' als ontkenning (§8.1.7): " + bron, 0));
            return negate(pos, bron);
        }
        Conditie kenmerk = characteristicCheckPredicate(lhs, op.kaleNaam(), op.EEN() != null,
                op.HEEFT() != null || op.HEBBEN() != null, periode, bron); // (is|heeft) een? kaleNaam: §8.1.8
        // else objecttype/navigation membership — reason-carrying, not reported here: see the sister in
        // conditionFromToplevelBare, whose caller may still fold this very marker into a Kwantorvouw.
        return kenmerk != null ? kenmerk : new Conditie.Unresolved(bron, REASON_NO_CHARACTERISTIC_NO_ROLE);
    }

    /** §8.1.7/§8.1.8: "&lt;subject&gt; is/heeft (een)? &lt;naam&gt;" -> a {@link Conditie.Kenmerk} (the
     *  §8.1.8 kenmerkcheck) or a {@link Conditie.Rollidmaatschap} (the §8.1.7 rolcheck), when the operand
     *  denotes the rule subject (or a to-one navigation from it). WITHOUT "een" the name is the subject's
     *  kenmerk (own or navigated). WITH "een" the grammar conflates a kenmerknaam and a rolnaam, so the
     *  resolver disambiguates by lookup, kenmerk-first (Tabel 16): a ZELFSTANDIG kenmerk (§8.1.8.3 "is een
     *  &lt;k&gt;") binds first, otherwise a role the subject fills is the §8.1.7 rolcheck. A bijvoeglijk /
     *  bezittelijk kenmerk is NOT written with "een", so the EEN-form never binds it. A name that resolves to
     *  neither stays Onopgelost (an honest marker), never a silently-true gate. */
    private Conditie characteristicCheckPredicate(Expr lhs, KaleNaamContext nameCtx, boolean withIndefiniteArticle, boolean hasVerb,
                                      Conditie.WholePeriod periode, Bron bron) {
        Conditie c = characteristicCheckPredicateBare(lhs, nameCtx, withIndefiniteArticle, hasVerb, bron);
        if (periode == null || c == null) return c;
        // §8.2 "is/heeft gedurende het gehele jaar een <kenmerk>" attaches the whole-period qualifier ONLY to a
        // §8.1.8 kenmerkcheck (its onderwerp is a tijdsafhankelijk kenmerk). Over any other membership form (a
        // §8.1.7 rolcheck) the qualifier has no defined meaning -> an honest marker, never silently dropped.
        return c instanceof Conditie.Kenmerk k
                ? new Conditie.Kenmerk(k.kenmerk(), k.navigatie(), k.negatie(), k.eigenaar(), periode, k.bron())
                // GEEN GETUIGE: deze marker haalt het model nooit. Hij ontstaat alleen met een §8.2-
                // kwalificatie, en dan loopt hij altijd door checkWholePeriod, die een Onopgelost nooit
                // tijdsafhankelijk acht en hem dus vervangt door haar eigen marker MET ERROR. De reden
                // staat er omdat zij de weigering van DEZE route beschrijft; dat checkWholePeriod
                // vervolgens harder ingrijpt is haar besluit, niet dat van deze plek. Gemeten 2026-09-19.
                : new Conditie.Unresolved(bron,
                        "de controle 'gedurende de/het gehele <periode>' hoort bij een kenmerkcontrole over een "
                        + "tijdsafhankelijk onderwerp; over deze lidmaatschapsvorm (een §8.1.7-rolcontrole) heeft "
                        + "zij geen omschreven betekenis (§8.2)");
    }

    /** Flip the negatie of a resolved kenmerk-/rolcheck (the §8.1.8.3 "is niet een &lt;k&gt;" negation). The positive
     *  binding has negatie=false; flipping it yields the "== false" membership. A form that did not structure into a
     *  kenmerk/rolcheck (null, or an Onopgelost) becomes an honest marker, never a silently-true gate. */
    private static Conditie negate(Conditie c, Bron bron) {
        if (c instanceof Conditie.Kenmerk k)
            return new Conditie.Kenmerk(k.kenmerk(), k.navigatie(), !k.negatie(), k.eigenaar(), k.periode(), k.bron());
        if (c instanceof Conditie.Rollidmaatschap r)
            return new Conditie.Rollidmaatschap(r.rol(), r.otherToFiller(), r.bron(), !r.negatie());
        if (c instanceof Conditie.FarRoleMembership vr)
            return new Conditie.FarRoleMembership(vr.filler(), vr.backref(), !vr.negatie(), vr.bron());
        return new Conditie.Unresolved(bron,
                "de vorm 'is niet een <naam>' bond geen kenmerk en geen rol van het onderwerp (§8.1.8.3), dus "
                + "er is niets om te ontkennen");
    }

    /** §8.1.8 / Tabel 16: the kenmerkcheck NEGATION verb is keyed to the kenmerk SOORT — a bijvoeglijk and a
     *  zelfstandig kenmerk negate with "is"/"zijn" ("is niet &lt;k&gt;", "is geen &lt;k&gt;", "is niet een &lt;k&gt;"),
     *  a bezittelijk one with "heeft"/"hebben" ("heeft (geen) &lt;k&gt;"). The de-negation in {@link #bindCharacteristicCheck} is
     *  verb-AGNOSTIC (the "== false" lowering is identical either way), so a cross-soort writing ("heeft geen
     *  &lt;bijvoeglijk&gt;", "is geen &lt;bezittelijk&gt;") slips through with the right meaning but the wrong register.
     *  Surface it as a WARNING — a writing-form lint, never a wrong gate (the lowering is unchanged). Only a de-negated
     *  kenmerk (negatie=true) is checked; the positive form's verb-keying is the EEN-form split's job. */
    private void checkNegationKind(Objecttype houder, Conditie kc, boolean hasVerb) {
        // a condition's warning stands at the line of the sentence that writes the check; it stood at 0, which points
        // an author at nothing in a document with many rules
        checkNegationKind(houder, kc, hasVerb, rowTableLine > 0 ? rowTableLine
                : kc == null || kc.bron() == null ? 0 : kc.bron().line());
    }

    /** As above, at {@code line}. A negated RESULT (dialect §4) passes its sentence's line, so its warning stands at
     *  the rule on the wire; a condition passes the line of its check, or the table's line in a row of a decision
     *  table. */
    private void checkNegationKind(Objecttype houder, Conditie kc, boolean hasVerb, int line) {
        if (houder == null || !(kc instanceof Conditie.Kenmerk k) || !k.negatie()) return;
        Objecttype.KenmerkSoort soort = houder.kenmerkSoort(k.kenmerk());
        if (soort == null) return;
        boolean bezit = soort == Objecttype.KenmerkSoort.BEZITTELIJK;
        if (bezit != hasVerb)
            model.diagnostics.add(new Diagnostic(Severity.WARNING,
                "het " + soort.name().toLowerCase() + " kenmerk '" + k.kenmerk() + "' wordt ontkend met het werkwoord '"
                + (hasVerb ? "heeft" : "is") + "', maar Tabel 16 vereist '" + (bezit ? "heeft" : "is")
                + "' bij dit soort kenmerk (§8.1.8): " + k.bron(), line));
    }

    private Conditie characteristicCheckPredicateBare(Expr lhs, KaleNaamContext nameCtx, boolean withIndefiniteArticle, boolean hasVerb, Bron bron) {
        if (nameCtx == null) return null;
        // §8.1.5 dagsoortcontrole: "<datumexpressie> is een <dagsoort>" (e.g. "de datum van vertrek … is een
        // kerstdag") tests the date against that dagsoort's §9.8 definitie. A dagsoort is neither a kenmerk nor a
        // role — it is keyed only by the operand being a datum and the name matching a declared dagsoort, so it is
        // resolved here BEFORE the kenmerk/rolcheck machinery (the §13.4.15.45 "(een) <dagsoort> is" form). §8.1.5 /
        // §5.15 / Tabel 57 accept ANY datum/tijd operand (Datum-in-dagen OR Datum-en-tijd); the §9.8 dagsoort is
        // day-granular, so a Datum-en-tijd contributes its date part (both back-ends take .toLocalDate()).
        if (withIndefiniteArticle && !hasVerb && lhs != null) {
            String day = dayKindName(stripArticle(src(nameCtx)));
            if (day != null && lhs.type().isDate())
                return new Conditie.Predicaat(Conditie.PredicateKind.DAGSOORT, lhs, day, null, bron);
            // Tabel 57: the written name IS a declared dagsoort but the operand is NOT datum-typed — a
            // resolver ERROR (was: a silent fall-through into the kenmerk/rol machinery ending as an
            // Onopgelost marker). An Onbekend operand carries no type claim and keeps the fall-through.
            if (day != null && !lhs.type().isUnknown())
                return dayKindOperandError(lhs, nameCtx, bron);
        }
        // §8.1.5 + Tabel 16 "<datum> is geen <dagsoort>": the NEGATED dagsoortcontrole, kept on the §8.1.5
        // datum-value path — NEVER mis-folded into the §3.5 kenmerkcheck (a dagsoort is no kenmerk; §5.15-based
        // leegwaarde: a leeg/non-dagsoort datum IS "geen <dagsoort>" = WAAR). "geen" is a LOWER_ID (no GEEN token), so
        // where it lands depends on the operand shape: for a §6 date-arithmetic operand ("de startdatum plus 1 dag") it
        // stays on the operator's kaleNaam ("geen kerstdag"); for a plain AttribuutRef the greedy onderwerp absorbs it
        // as a TRAILING word of the operand bron (like "niet"), leaving the bare dagsoort as the kaleNaam.
        if (!hasVerb && lhs != null) {   // §8.1.5/§5.15/Tabel 57: any datum/tijd operand (date part taken)
            boolean datum = lhs.type().isDate(), getypt = !lhs.type().isUnknown();
            String name0 = stripArticle(src(nameCtx));
            // (a) "geen <dagsoort>" left on the kaleNaam — a date-arithmetic / non-AttribuutRef datum operand.
            if (name0 != null && name0.regionMatches(true, 0, "geen ", 0, 5)) {
                String day = dayKindName(stripArticle(name0.substring(5).trim()));
                if (day != null && datum) return new Conditie.Predicaat(Conditie.PredicateKind.DAGSOORT, lhs, day, true, 0, null, bron);
                if (day != null && getypt) return dayKindOperandError(lhs, nameCtx, bron);   // Tabel 57 operand gate
            }
            // (b) "geen" absorbed as the operand's trailing word — a plain AttribuutRef (mirror absorbedNegator),
            //     guarded against an attribuut literally named "… geen". The kaleNaam is then the bare dagsoort.
            if (lhs instanceof Expr.AttribuutRef la && la.bron() != null
                    && la.bron().snippet().stripTrailing().toLowerCase().endsWith(" geen") && !la.attribuut().toLowerCase().endsWith("geen")) {
                String day = dayKindName(name0);
                if (day != null && datum) return new Conditie.Predicaat(Conditie.PredicateKind.DAGSOORT, la, day, true, 0, null, bron);
                if (day != null && getypt) return dayKindOperandError(la, nameCtx, bron);   // Tabel 57 operand gate
            }
        }
        Objecttype subject = builder.subject();
        if (subject == null) return null;
        String suffix = stripArticle(src(nameCtx));
        String head = subjectHead(lhs, subject);
        // not the subject itself -> a navigated kenmerk ("zijn reis is duurzaam", or the EEN-form "zijn reis is
        // een belaste reis"). The EEN-form binds only a ZELFSTANDIG kenmerk of the navigated type (the soort guard,
        // §8.1.8.3); a navigated role membership stays out of scope (a marker).
        if (head == null) return navigationCharacteristicCheck(lhs, suffix, hasVerb, withIndefiniteArticle, bron);
        // The greedy onderwerp parse absorbs a multi-word kenmerk's leading words into the operand
        // ("de Meting bereikbaar per" + operator "trein"), so the kenmerk is that absorbed kop plus
        // the operator's kaleNaam; for "hij"/a bare subject the kop is empty and the name is the kaleNaam.
        String naam = head.isEmpty() ? suffix : head + " " + suffix;
        if (!withIndefiniteArticle) {
            // §8.1.8 "is/heeft <kenmerk>" first (incl. the §8.1.8.1/.2/.3 "niet"/"geen" negations, which
            // characteristicCheckCondition de-negates to "== false"). When the name is not a kenmerk it is a §8.1.7 rolcheck —
            // kenmerk-first (the non-"een" form IS how a bijvoeglijk kenmerk is written, so characteristicCheckCondition already
            // claims those; only a name that is a ROLE but not a kenmerk newly binds). §13.4.15.44 makes the "een"
            // optional ("hij is deelnemer"); "heeft (geen) <rol>" is the §8.1.7 possessive, which roleMembership
            // resolves to the near-side test or — when the subject owns a to-many filler — the far-side exists-join.
            Conditie kc = characteristicCheckCondition(subject, naam, null, hasVerb, bron);
            if (kc != null) { checkNegationKind(subject, kc, hasVerb); return kc; }
            // a leftover "geen <name>" that is a role is the INVERSE rolcheck (the subject does not fill it);
            // otherwise the positive rolcheck.
            if (naam.regionMatches(true, 0, "geen ", 0, 5))
                return roleMembership(subject, naam.substring(5).trim(), bron, true);
            return roleMembership(subject, naam, bron);
        }
        // EEN-form: the grammar conflates a kenmerknaam and a rolnaam after "een", so disambiguate by lookup,
        // kenmerk-first (Tabel 16). The verb selects which kenmerk SOORT the EEN-form may bind: "is een <k>" is
        // §8.1.8.3 (a ZELFSTANDIG kenmerk — a bijvoeglijk kenmerk is written "is <k>" without "een"), while
        // "heeft een <k>" is §8.1.8.2 (a BEZITTELIJK kenmerk — the §7.1 verbatim "heeft een recht op
        // belastingvermindering"; the canonical "heeft het <k>" rides the non-EEN path above). Neither soort can
        // bind under the other verb. A name that is no such kenmerk falls through to the §8.1.7 rolcheck "is/heeft
        // een <rolnaam>" (incl. the far-side "heeft een <rol>" exists-join).
        Conditie k = hasVerb ? possessiveCharacteristicCheck(subject, naam, bron)
                                    : nominalCharacteristicCheck(subject, naam, bron);
        return k != null ? k : roleMembership(subject, naam, bron);
    }

    /** The declared §3.12 dagsoort whose name matches {@code written} (case-insensitive), or null. The
     *  written §8.1.5 control names the dagsoort in its singular ("is een kerstdag"), which is the form stored
     *  in {@code model.dayKinds}; the §6 duration UNIT (a plural eenheidsafkorting "kerstdagen") is matched
     *  separately by the transpilers via the declared meervoud. */
    private String dayKindName(String written) {
        if (written == null) return null;
        String g = written.trim();
        for (String d : model.dayKinds) if (d.equalsIgnoreCase(g)) return d;
        return null;
    }

    /** §8.1.8.3 "&lt;subject&gt; is een &lt;zelfstandig kenmerk&gt;" -> a {@link Conditie.Kenmerk} ("k == true"),
     *  but ONLY for a {@link Objecttype.KenmerkSoort#ZELFSTANDIG} kenmerk: a bijvoeglijk kenmerk ("is &lt;k&gt;")
     *  and a bezittelijk one ("heeft het &lt;k&gt;") are not written with "een", so the EEN-form must not bind
     *  them (that mis-binding was the reason the EEN-form kenmerkcheck was deferred). Positive only — the "is
     *  geen" negation lexes into the kaleNaam with no "een" and reaches the non-EEN path (carded). Null when no
     *  ZELFSTANDIG kenmerk of {@code subject} bears the name, so the caller falls back to the §8.1.7 rolcheck. */
    private Conditie nominalCharacteristicCheck(Objecttype subject, String naam, Bron bron) {
        String g = CharacteristicMatcher.find(subject, naam);
        return g != null && subject.kenmerkSoort(g) == Objecttype.KenmerkSoort.ZELFSTANDIG
                ? new Conditie.Kenmerk(g, bron) : null;
    }

    /** §8.1.8.2 "&lt;subject&gt; heeft een &lt;bezittelijk kenmerk&gt;" -> a {@link Conditie.Kenmerk} ("k == true"),
     *  but ONLY for a {@link Objecttype.KenmerkSoort#BEZITTELIJK} kenmerk: the EEN-form heeft binds a possession of
     *  a kenmerk (the §7.1 "heeft een recht op belastingvermindering"), while a possession of a ROLE
     *  ("heeft een deelnemer") is no kenmerk and stays null for the caller's §8.1.7 far-side rolcheck. A
     *  bijvoeglijk/zelfstandig kenmerk uses "is", so the heeft EEN-form must not bind them. Null when no
     *  BEZITTELIJK kenmerk of {@code subject} bears the name. */
    private Conditie possessiveCharacteristicCheck(Objecttype subject, String naam, Bron bron) {
        String g = CharacteristicMatcher.find(subject, naam);
        return g != null && subject.kenmerkSoort(g) == Objecttype.KenmerkSoort.BEZITTELIJK
                ? new Conditie.Kenmerk(g, bron) : null;
    }

    /** §8.1.7 rolcheck: "&lt;subject&gt; is (een) &lt;rolnaam&gt;" — true when the subject fills that role.
     *  Resolves to a {@link Conditie.Rollidmaatschap} carrying the OTHER end of the feittype (so the
     *  transpiler can null-test the subject's reference to it). Null when no role the subject fills bears
     *  that name (-> Onopgelost marker, never a wrong gate). */
    private Conditie roleMembership(Objecttype subject, String naam, Bron bron) {
        return roleMembership(subject, naam, bron, false);
    }
    /** As above, with {@code negatie} for the "is geen &lt;rol&gt;" form (the subject does NOT fill it). When the
     *  subject does not FILL the role (roleSiblingType null) but the role reaches a TO-MANY filler from it, this is
     *  the §8.1.7 FAR-side "&lt;subject&gt; heeft een &lt;rol&gt;": resolve to a {@link Conditie.FarRoleMembership}
     *  carrying that filler and its back-reference field, which the transpiler lowers to an exists-join. */
    private Conditie roleMembership(Objecttype subject, String naam, Bron bron, boolean negatie) {
        Objecttype other = model.roleSiblingType(subject, naam);
        if (other != null) return new Conditie.Rollidmaatschap(naam, other, bron, negatie);
        Objecttype filler = model.rolDoeltypeNaarSubject(subject, naam);
        if (filler != null && model.naarVeelRol(subject, filler, naam)) {
            // The backref is the subject-side role of the SAME feittype as the named role — found by the written
            // role name (connectingRoleViaElementName), which disambiguates a filler that references the subject
            // through MORE THAN ONE feittype (e.g. a Persoon that fills both "deelnemer" and "bonusdeelnemer" of
            // a Ploeg): the by-pair connectingRole is then ambiguous and the far-side would wrongly stay a marker.
            // Fall back to the by-pair lookup for the single-feittype case (both give the same role there).
            String backref = model.connectingRoleViaElementName(subject, naam);
            if (backref == null) backref = model.connectingRole(filler, subject);
            if (backref != null) return new Conditie.FarRoleMembership(filler, backref, negatie, bron);
        }
        return null;
    }

    /** Resolve {@code kenmerk} on {@code type} into a {@link Conditie.Kenmerk}, honouring a leading "niet"
     *  or "geen" as NEGATION — but each only after the WHOLE name fails, so a declared "niet-…" kenmerk
     *  still binds positively. {@code navigatie} is the to-one navigation role (null for the subject's own).
     *  {@code hasVerb} is true when the predicate verb is hebben (§8.1.8: see the "geen" branch).
     *  Null when neither the whole name nor a de-negated form is a declared kenmerk of {@code type}. */
    private static Conditie characteristicCheckCondition(Objecttype type, String kenmerk, String navigatie, boolean hasVerb, Bron bron) {
        // Match the written name VERBATIM first. A bijvoeglijk kenmerk may be declared WITH a leading article
        // as part of its name ("is de minimis kenmerk" -> stored "de minimis"): its grammar rule has no
        // separate bepaaldLidwoord? and naamWoordLower admits de/het, so the article is part of the stored
        // name — the false-invariant the first cut tripped on (a blind strip dropped it to a wrong/no match).
        Conditie c = bindCharacteristicCheck(type, kenmerk, navigatie, hasVerb, bron);
        if (c != null) return c;
        // Only if the verbatim name binds NOTHING, retry with a leading de/het/een stripped — recovering the
        // bezittelijk/noch-noch case where the article is the kenmerk's OWN (stored article-stripped) and the
        // greedy onderwerp parse left it on the reconstructed name ("het recht op duurzaamheidskorting").
        // Verbatim-first guarantees a kenmerk whose name really begins with an article binds itself, never a
        // shorter sibling.
        String stripped = stripArticle(kenmerk);
        return stripped.equals(kenmerk) ? null : bindCharacteristicCheck(type, stripped, navigatie, hasVerb, bron);
    }

    /** Bind one candidate name to a declared kenmerk of {@code type}, honouring a leading "niet" or "geen"
     *  as NEGATION — but each only after the WHOLE name fails, so a kenmerk declared starting with niet/geen
     *  still binds positively (never silently inverted). Null when nothing binds. */
    private static Conditie bindCharacteristicCheck(Objecttype type, String naam, String navigatie, boolean hasVerb, Bron bron) {
        String g = CharacteristicMatcher.find(type, naam);
        if (g != null) return new Conditie.Kenmerk(g, navigatie, false, bron);
        // §8.1.8.1 bijvoeglijk negation "is niet <k>" -> "== false" (verb-agnostic).
        if (naam.regionMatches(true, 0, "niet ", 0, 5)) {
            g = CharacteristicMatcher.find(type, naam.substring(5).trim());
            if (g != null) return new Conditie.Kenmerk(g, navigatie, true, bron);
        }
        // Negation with "geen" -> "== false": the §8.1.8.2 bezittelijk form "heeft geen <k>" AND the §8.1.8.3
        // noch-noch form "is geen <k>" (both lower to the same boolean negation, so the verb need not gate it;
        // a soort distinction governs only positive writing-form validation, not the negated lowering). "geen"
        // carries no token (only GEEN_VAN_DE), so it lexes as LOWER_ID and rides inside the kaleNaam into this
        // reconstructed name. Verbatim-first above means a kenmerk literally named "geen …" still binds positively.
        if (naam.regionMatches(true, 0, "geen ", 0, 5)) {
            g = CharacteristicMatcher.find(type, naam.substring(5).trim());
            if (g != null) return new Conditie.Kenmerk(g, navigatie, true, bron);
        }
        return null;
    }

    /** §8.1.8 navigated: "de &lt;rol&gt; van X is &lt;kenmerk&gt;" — the kenmerk is on the object the rol
     *  reaches, not the subject. Lowered when the rol is a TO-ONE navigation from the subject (so the
     *  property path {@code rol.kenmerk} reads one object); a TO-MANY navigation needs an exists/forall and
     *  stays a marker, as does an unresolvable rol or a kenmerk the navigated type does not declare. The rol
     *  is normalised to its declared name (a plural "de deelnemers" is the same role as "de deelnemer"), so
     *  the to-many test and the emitted reference field are the canonical one. With {@code withIndefiniteArticle} the verb selects
     *  the soort, exactly as the subject's own EEN-form ({@link #characteristicCheckPredicate}): "is een &lt;k&gt;" binds a
     *  §8.1.8.3 ZELFSTANDIG kenmerk of the navigated type, "heeft een &lt;k&gt;" a §8.1.8.2 BEZITTELIJK one — neither
     *  binds under the other verb, and a navigated role membership via "een" stays out of scope (null). */
    private Conditie navigationCharacteristicCheck(Expr lhs, String suffix, boolean hasVerb, boolean withIndefiniteArticle, Bron bron) {
        if (!(lhs instanceof Expr.AttribuutRef a) || a.objecttype() == null) return null;
        Objecttype subject = a.objecttype();                                       // the onderwerp the navigation is from
        Objecttype doel = model.rolDoeltypeNaarSubject(subject, a.attribuut());
        if (doel == null) return null;
        String rol = model.connectingRoleViaName(subject, doel, a.attribuut());     // canonical role / reference field
        if (rol == null) rol = a.attribuut();
        if (model.naarVeelRol(subject, doel, rol)) return null;                     // to-many -> marker (needs exists/forall)
        // The greedy onderwerp parse can absorb a multi-word kenmerk's leading words — or a "niet" — into
        // the operand AFTER the navigated onderwerp. Reconstruct the full kenmerk = that absorbed tail + the
        // operator's kaleNaam, so a multi-word kenmerk binds (and a §8.1.8.1 "niet <kenmerk>" de-negates in
        // characteristicCheckCondition). navigationTail recovers the absorbed tail past the role/type anchor.
        String tail = navigationTail(a);
        String kenmerk = tail.isEmpty() ? suffix : tail + " " + suffix;
        if (withIndefiniteArticle) {
            // §8.1.8.2/.3 navigated EEN-form: the verb selects the soort (is -> ZELFSTANDIG, heeft -> BEZITTELIJK),
            // the mirror of the subject's own EEN-form; neither soort binds under the other verb, and a navigated
            // role membership via "een" stays out of scope -> all an honest marker, never a wrong gate.
            String g = CharacteristicMatcher.find(doel, kenmerk);
            Objecttype.KenmerkSoort required = hasVerb
                    ? Objecttype.KenmerkSoort.BEZITTELIJK : Objecttype.KenmerkSoort.ZELFSTANDIG;
            return g != null && doel.kenmerkSoort(g) == required
                    ? new Conditie.Kenmerk(g, rol, false, bron) : null;
        }
        Conditie kc = characteristicCheckCondition(doel, kenmerk, rol, hasVerb, bron);
        checkNegationKind(doel, kc, hasVerb);
        return kc;
    }

    /** The words the greedy operand parse absorbed AFTER the navigated "de &lt;rol&gt; van &lt;onderwerp&gt;"
     *  (the leading words of a multi-word kenmerk, or a "niet") — empty for a clean navigation. Found as the
     *  operand text following the onderwerp's object-type name. */
    private static String navigationTail(Expr.AttribuutRef a) {
        String b = a.bron() == null ? "" : a.bron().snippet();
        // Anchor on the onderwerp TYPE name ("de <rol> van <Type> <kenmerkwoorden>"); for a POSSESSIVE
        // ("zijn <rol> <kenmerkwoorden>") there is no type name, so fall back to the ROLE name. Take the text
        // after the anchor's LAST whole-word occurrence (so a kenmerk word merely containing it does not
        // mis-split). The role fallback recovers a "niet"/"geen" the greedy possessive onderwerp absorbed
        // ("zijn reis niet" -> "niet"), so a navigated §8.1.8 negation de-negates instead of markering.
        for (String anchor : new String[]{a.objecttype() != null ? a.objecttype().naam : null, a.attribuut()}) {
            if (anchor == null || anchor.isEmpty()) continue;
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("(?i)\\b" + java.util.regex.Pattern.quote(anchor) + "\\b").matcher(b);
            int end = -1;
            while (m.find()) end = m.end();
            if (end >= 0) return b.substring(end).trim();
        }
        return "";
    }

    /** When the condition operand denotes the rule subject, the part of a multi-word kenmerk the
     *  greedy onderwerp parse absorbed into the operand (empty for the bezielde "hij" or a bare
     *  subject reference); null when the operand is not the subject. A navigation ("de partner van
     *  …") names a different filler, so it is excluded — its kenmerk would be on another object. */
    private String subjectHead(Expr lhs, Objecttype subject) {
        // In a feitcreatie doel re-resolution (targetRole != null) the bezielde "hij"/bare reference names the
        // ANKER, not the doel (§13.4.7: the doel is written with a definite article, "de <roleC>"), so it must
        // NOT latch onto the doel — reject it. Outside that mode "hij" denotes the single subject as before.
        if (lhs instanceof Expr.Constante c)
            return (targetRole == null && c.naam().equalsIgnoreCase("hij")) ? "" : null;
        String b = lhs.bron() == null ? "" : stripArticle(lhs.bron().snippet());
        if (b.equalsIgnoreCase(subject.naam)) return "";                              // "de Meting"
        if (b.regionMatches(true, 0, subject.naam + " ", 0, subject.naam.length() + 1)) // "de Meting vaarbaar bij"
            return b.substring(subject.naam.length()).trim();
        // The doel is named by the ROLE it fills ("de deelnemer"), which need not equal its type name
        // ("Persoon"); accept that role too, with the same multi-word-kenmerk kop reconstruction.
        if (targetRole != null) {
            if (b.equalsIgnoreCase(targetRole)) return "";
            if (b.regionMatches(true, 0, targetRole + " ", 0, targetRole.length() + 1))
                return b.substring(targetRole.length()).trim();
        }
        return null;
    }

    private Conditie.Kwantor quantifierOf(VoorwaardeKwantificatieContext k) {
        if (k == null) return Conditie.Kwantor.DE;
        if (k.ALLE() != null)        return Conditie.Kwantor.ALLE;
        if (k.GEEN_VAN_DE() != null) return Conditie.Kwantor.GEEN;
        if (k.TEN_MINSTE() != null)  return Conditie.Kwantor.TEN_MINSTE;
        if (k.TEN_HOOGSTE() != null) return Conditie.Kwantor.TEN_HOOGSTE;
        if (k.PRECIES() != null)     return Conditie.Kwantor.PRECIES;
        return Conditie.Kwantor.DE;
    }

    static Conditie.Operator toOperator(Typing.CmpOp op) {
        if (op == null) return Conditie.Operator.GELIJK;
        return switch (op) {
            case KLEINER -> Conditie.Operator.KLEINER;
            case KLEINER_OF_GELIJK -> Conditie.Operator.KLEINER_OF_GELIJK;
            case GROTER -> Conditie.Operator.GROTER;
            case GROTER_OF_GELIJK -> Conditie.Operator.GROTER_OF_GELIJK;
            case GELIJK -> Conditie.Operator.GELIJK;
            case ONGELIJK -> Conditie.Operator.ONGELIJK;
            case EERDER -> Conditie.Operator.EERDER;
            case EERDER_OF_GELIJK -> Conditie.Operator.EERDER_OF_GELIJK;
            case LATER -> Conditie.Operator.LATER;
            case LATER_OF_GELIJK -> Conditie.Operator.LATER_OF_GELIJK;
        };
    }

    private static String src(ParserRuleContext c) { return DefinitionResolver.src(c); }
    private static dev.uchar.regelspraak.ir.Bron bron(ParserRuleContext c) { return DefinitionResolver.bron(c); }

    /** The {@code // Bron: <wetsartikel>} statuutannotaties written directly above {@code ctx} — the line
     *  comments on the HIDDEN channel between the previous on-channel token and {@code ctx}'s start, each with
     *  the {@code //} and the {@code Bron:} tag stripped (case-insensitive), in source order. Empty when there
     *  is no stream, no such comment, or {@code ctx} does not belong to the main stream (the §12 beslistabel
     *  desugar re-parses each synthetic regel in its own stream — a token index into a foreign stream must not
     *  be read, and a desugared regel carries no annotations anyway). */
    private List<String> statuteAnnotations(ParserRuleContext ctx) {
        if (tokens == null || ctx == null || ctx.getStart() == null) return List.of();
        int idx = ctx.getStart().getTokenIndex();
        if (idx < 0 || idx >= tokens.size() || tokens.get(idx) != ctx.getStart()) return List.of();
        List<Token> hidden = tokens.getHiddenTokensToLeft(idx);
        if (hidden == null) return List.of();
        List<String> result = new ArrayList<>();
        for (Token t : hidden) {
            String c = t.getText().replaceFirst("^//+\\s*", "");   // strip the leading // and spaces
            if (c.regionMatches(true, 0, "Bron:", 0, 5)) result.add(c.substring(5).trim());
        }
        return result;
    }

    // ---- §5 condition type-checking (comparisons + predicates) ----
    private void conditions(ParseTree t) {
        if (t == null) return;
        if (t instanceof ToplevelElementaireVoorwaardeContext c) checkTopCondition(c);
        else if (t instanceof ElementaireVoorwaardeContext c) checkElemCondition(c);
        else if (t instanceof EnkelvoudigeConsistentieRegelContext c) checkConsistencyCriterion(c);
        for (int i = 0; i < t.getChildCount(); i++) conditions(t.getChild(i));
    }

    /** A §13.4.8 single criterion ("&lt;expressie&gt; moet &lt;operator&gt;") is the same
     *  predicate over the same onderwerpexpressie as the matching §5 voorwaarde (§8.3.1),
     *  so its operands get the identical typeringen check — the lifted criterion now
     *  transpiles, which would otherwise let an ill-typed comparison through silently. */
    private void checkConsistencyCriterion(EnkelvoudigeConsistentieRegelContext c) {
        if (c.expressie() == null || c.toplevelOperator() == null) return;
        Datatype lhs = infer(c.expressie());
        ToplevelOperatorContext op = c.toplevelOperator();
        int line = c.getStart().getLine();
        if (op.vergelijkingsTekenGesplitst() != null && op.expressie() != null)
            reportCompare(cmpOf(op.vergelijkingsTekenGesplitst()), lhs, infer(op.expressie()),
                    bareNumberLiteral(c.expressie()), bareNumberLiteral(op.expressie()), line,
                    enumLabelOf(c.expressie()), enumLabelOf(op.expressie()));
        else if (op.AAN_DE_ELFPROEF() != null)
            reportPredicate(Typing.UnairePred.ELFPROEF, lhs, line);
        else if (op.NUMERIEK_IS_MET_EXACT() != null || op.NUMERIEK_ZIJN_MET_EXACT() != null)
            reportPredicate(Typing.UnairePred.NUMERIEK_MET_EXACT, lhs, line);
    }

    // §8.3.1/§13.4.15: a TOPLEVEL voorwaarde (after 'indien'/'dat'/'gedurende de tijd dat') takes ONLY the
    // verb-trailing (vragende) toplevelOperator — the grammar no longer admits the verb-leading register here
    // (that is reserved for samengestelde bullets, checkElemCondition, which keeps both families). So only the
    // toplevelOperator needs checking; an ill-typed predicate is still caught by checkToplevelOp.
    private void checkTopCondition(ToplevelElementaireVoorwaardeContext c) {
        if (c.expressie() == null) return;
        Datatype lhs = infer(c.expressie());
        int line = c.getStart().getLine();
        checkToplevelOp(lhs, c.toplevelOperator(), line, bareNumberLiteral(c.expressie()), enumLabelOf(c.expressie()));
    }

    private void checkElemCondition(ElementaireVoorwaardeContext c) {
        if (c.expressie() == null) return;
        Datatype lhs = infer(c.expressie());
        int line = c.getStart().getLine();
        boolean lhsLit = bareNumberLiteral(c.expressie());
        String lhsEnum = enumLabelOf(c.expressie());
        checkNonToplevelOp(lhs, c.nonToplevelOperator(), line, lhsLit, lhsEnum);
        checkToplevelOp(lhs, c.toplevelOperator(), line, lhsLit, lhsEnum);
    }

    /** Type-check a verb-split / trailing operator (vergelijking, elfproef, numeriek-met-exact). */
    private void checkToplevelOp(Datatype lhs, ToplevelOperatorContext op, int line, boolean lhsLit, String lhsEnum) {
        if (op == null) return;
        if (op.vergelijkingsTekenGesplitst() != null && op.expressie() != null)
            reportCompare(cmpOf(op.vergelijkingsTekenGesplitst()), lhs, infer(op.expressie()), lhsLit, bareNumberLiteral(op.expressie()), line,
                    lhsEnum, enumLabelOf(op.expressie()));
        else if (op.AAN_DE_ELFPROEF() != null)
            reportPredicate(Typing.UnairePred.ELFPROEF, lhs, line);
        else if (op.NUMERIEK_IS_MET_EXACT() != null || op.NUMERIEK_ZIJN_MET_EXACT() != null)
            reportPredicate(Typing.UnairePred.NUMERIEK_MET_EXACT, lhs, line);
    }

    /** Type-check a verb-leading operator (vergelijking, elfproef, numeriek-met-exact). */
    private void checkNonToplevelOp(Datatype lhs, NonToplevelOperatorContext op, int line, boolean lhsLit, String lhsEnum) {
        if (op == null) return;
        if (op.vergelijkingsTeken() != null && op.expressie() != null)
            reportCompare(cmpOf(op.vergelijkingsTeken()), lhs, infer(op.expressie()), lhsLit, bareNumberLiteral(op.expressie()), line,
                    lhsEnum, enumLabelOf(op.expressie()));
        else if (op.AAN_DE_ELFPROEF() != null)
            reportPredicate(Typing.UnairePred.ELFPROEF, lhs, line);
        else if (op.NUMERIEK_MET_EXACT() != null)
            reportPredicate(Typing.UnairePred.NUMERIEK_MET_EXACT, lhs, line);
    }

    private void reportCompare(Typing.CmpOp op, Datatype l, Datatype r, boolean lLit, boolean rLit, int line,
                               String lEnum, String rEnum) {
        if (op == null) return;
        // §13.3.4.2 membership: an enum LITERAL on one side is checked against the OTHER side's declared
        // domain (the literal itself is typed Onbekend — its domain IS this context). GELIJK/ONGELIJK only:
        // Tabel 36/43 rij 7 is the enum comparison row; the ordering tables carry no Enumeratie row.
        if (op == Typing.CmpOp.GELIJK || op == Typing.CmpOp.ONGELIJK) {
            checkEnumLabel(l, rEnum, line);
            checkEnumLabel(r, lEnum, line);
        }
        // §13.3.5 / typeringen: a DIMENSIONLESS numeric literal adopts the other operand's unit in a comparison
        // (a shape the spec's own examples use). Promote the literal so the unit rule does not reject a legal
        // literal-vs-unit comparison ("km > 0"); a unitless ATTRIBUTE (not a literal) still triggers the rule.
        // Restricted to a NUMERIEK sibling: a Percentage operand keeps the §5.5 Tabel 41 Numeriek/Percentage
        // family separation (a literal would otherwise be mis-tagged Numeriek('%'), a datatype the spec never forms).
        if (rLit && l.isNumber() && l.eenheidOf() != null) r = withUnit(r, l.eenheidOf());
        else if (lLit && r.isNumber() && r.eenheidOf() != null) l = withUnit(l, r.eenheidOf());
        var res = Typing.compare(op, l, r, lLit, rLit);   // Onbekend operands -> ok, so no false positive
        if (!res.ok()) model.diagnostics.add(new Diagnostic(Severity.ERROR, "voorwaarde: " + res.fout(), line));
    }

    /** A bare numeric literal ("0", "18", "3,5") with NO trailing eenheid — getText collapses whitespace, so a
     *  unit-qualified literal ("18 jr") becomes "18jr" and is correctly excluded. Side-effect-free (no re-bouw). */
    private static boolean bareNumberLiteral(ExpressieContext e) {
        return e != null && e.getText().matches("-?\\d+([.,]\\d+)?");
    }

    /** Re-state a numeriek datatype with {@code eenheid} (for promoting a dimensionless literal to a sibling's unit). */
    private static Datatype withUnit(Datatype t, dev.uchar.regelspraak.model.Eenheid eenheid) {
        return t instanceof Datatype.Numeriek n ? new Datatype.Numeriek(n.geheel(), n.decimalen(), n.teken(), eenheid) : t;
    }
    private void reportPredicate(Typing.UnairePred p, Datatype operand, int line) {
        var res = Typing.predicaat(p, operand);
        if (!res.ok()) model.diagnostics.add(new Diagnostic(Severity.ERROR, "voorwaarde: " + res.fout(), line));
    }

    /** Tabel 57 (§5.15): the written name resolved to a DECLARED dagsoort but the operand is not datum-typed —
     *  the Typering DAGSOORT ERROR (this is its only live call site on the condition path), and the conditie
     *  stays an Onopgelost marker so neither back-end builds a dagsoort check over a non-date. */
    private Conditie dayKindOperandError(Expr lhs, KaleNaamContext nameCtx, Bron bron) {
        reportPredicate(Typing.UnairePred.DAGSOORT, lhs.type(), nameCtx.getStart().getLine());
        return new Conditie.Unresolved(bron, null);   // reportPredicate's ERROR is this marker's diagnosis
    }

    static Typing.CmpOp cmpOf(VergelijkingsTekenContext c) {
        return cmpOf(c.ONGELIJK() != null, c.GROTER() != null, c.KLEINER() != null,
                     c.LATER() != null, c.EERDER() != null, c.GELIJK() != null, c.OF() != null);
    }
    static Typing.CmpOp cmpOf(VergelijkingsTekenGesplitstContext c) {
        return cmpOf(c.ONGELIJK() != null, c.GROTER() != null, c.KLEINER() != null,
                     c.LATER() != null, c.EERDER() != null, c.GELIJK() != null, c.OF() != null);
    }
    static Typing.CmpOp cmpOf(boolean ong, boolean gr, boolean kl, boolean la, boolean ee, boolean ge, boolean of) {
        if (ong) return Typing.CmpOp.ONGELIJK;
        if (gr)  return of ? Typing.CmpOp.GROTER_OF_GELIJK  : Typing.CmpOp.GROTER;
        if (kl)  return of ? Typing.CmpOp.KLEINER_OF_GELIJK : Typing.CmpOp.KLEINER;
        if (la)  return of ? Typing.CmpOp.LATER_OF_GELIJK   : Typing.CmpOp.LATER;
        if (ee)  return of ? Typing.CmpOp.EERDER_OF_GELIJK  : Typing.CmpOp.EERDER;
        if (ge)  return Typing.CmpOp.GELIJK;
        return null;
    }

    // ---- expression datatype, via the resolved IR ----
    // The condition checks need an operand's datatype; that is now the type the
    // ExpressionBuilder computes for the resolved Expr, so inference and IR can never
    // diverge — they are the same code path.
    /** The type check builds an operand the rule's own resolution already built, so a diagnostic this build repeats
     *  verbatim is that build's and is reported once; one only this build raises is kept, never swallowed. */
    private Datatype infer(ExpressieContext e) {
        int before = model.diagnostics.size();
        Datatype type = builder.build(e).type();
        for (int i = model.diagnostics.size() - 1; i >= before; i--)
            if (model.diagnostics.subList(0, before).contains(model.diagnostics.get(i))) model.diagnostics.remove(i);
        return type;
    }
}
