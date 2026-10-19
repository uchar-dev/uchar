package dev.uchar.regelspraak.resolver;

import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.Default;
import dev.uchar.regelspraak.ir.ExceptionGroup;
import dev.uchar.regelspraak.ir.ResolvedFactCreation;
import dev.uchar.regelspraak.ir.Validity;
import dev.uchar.regelspraak.ir.ResolvedObjectCreation;
import dev.uchar.regelspraak.ir.ResolvedRule;
import dev.uchar.regelspraak.ir.ResolvedAllocation;
import dev.uchar.regelspraak.ir.Source;
import dev.uchar.regelspraak.ir.ResolvedUniquenessUnion;
import dev.uchar.regelspraak.message.Component;
import dev.uchar.regelspraak.message.Msg;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.*;
import dev.uchar.regelspraak.parser.RegelSpraak;
import dev.uchar.regelspraak.parser.RegelSpraak.*;
import dev.uchar.regelspraak.parser.RegelSpraakLexer;
import dev.uchar.regelspraak.core.Literals;
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
import static dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic.params;

/**
 * Type-checks rules against the typeringen matrix (Phase: rule checking). It
 * infers an expression's datatype where the references resolve against the model
 * — literals, parameters, arithmetic, and attribute-of-subject references whose
 * subject is a plain object type — and applies {@link Typing}. References it
 * cannot resolve yield {@link Datatype.Unknown}, which {@link Typing} treats
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
     *  {@link ResolvedRule} version it produces. Empty when the rule carried none. */
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
    private final java.util.Set<ResolvedRule> refusedTargets =
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
     *  {@link RegelSpraakModel#generated} is: {@link Designations#compose} composes its designations part by part. */
    private final Map<Designatable, List<Designations.NamePart>> nameParts = new LinkedHashMap<>();
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
    Map<Designatable, List<Designations.NamePart>> nameParts() { return nameParts; }

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
        ruleGroups(doc);   // build the §9.10 group IR + the fase-2 recursief structural checks
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
     * The origin of every element the passes above made under a name of their own ({@link RegelSpraakModel#generated},
     * grammar/DIVERGENCES.md, "Gegenereerde elementen"): the members and forms of a lowered opgave marker, the group of
     * a subtype's own blocks, the rows of a decision table, the member and rule of a fold with steps after the set, and
     * a start point's anchor. Each element is keyed, and each source spelled, as {@link RegelSpraakModel#designatable}
     * spells it; a decision table, the one source that is no element, keeps its written name. A candidate whose element
     * a later pass removed again — the members of a withdrawn marker, a row lifted into a defaults tree — is no element
     * of the model and gets no entry. The group and the fold's member and rule also get the pieces of their name
     * ({@link #nameParts}).
     */
    private void generatedElements() {
        Map<String, GeneratedFrom> candidates = new LinkedHashMap<>();
        Map<String, List<Designations.NamePart>> parts = new LinkedHashMap<>();
        for (Input o : inputs) {
            GeneratedFrom generatedFrom = new GeneratedFrom(Designatable.attribute(o.type().naam, o.attribute()),
                    OriginForm.OPGAVE_BOVEN_BEREKENING);
            kandidaat(candidates, Designatable.attribute(o.type().naam, o.opgegeven()), generatedFrom);
            kandidaat(candidates, Designatable.attribute(o.type().naam, o.computed()), generatedFrom);
            kandidaat(candidates, Designatable.characteristic(o.type().naam, o.deviating()), generatedFrom);
            if (lowered.contains(o))
                for (String form : List.of(o.regel(), o.consistencyCheck(), o.group()))
                    kandidaat(candidates, Designatable.form(form), generatedFrom);
        }
        for (String[] g : subtypeGroups) {
            Designatable group = Designatable.form(g[0]);
            kandidaat(candidates, group, new GeneratedFrom(Designatable.form(g[1]), OriginForm.EXCEPTION_FOR_SUBTYPE));
            parts.put(elementKey(group), List.of(new Designations.NamePart(g[1], Designatable.form(g[1])),
                    new Designations.NamePart(" voor ", null), new Designations.NamePart(g[2], Designatable.objectType(g[2]))));
        }
        for (var t : rowRules.entrySet()) {
            GeneratedFrom generatedFrom = new GeneratedFrom(Designatable.form(DefinitionResolver.src(t.getKey().beslistabelNaam())),
                    OriginForm.BESLISTABELRIJ);
            for (String row : t.getValue()) kandidaat(candidates, Designatable.form(row), generatedFrom);
        }
        for (ExpressionBuilder.HelperMember h : builder.helperMembers()) {
            Designatable member = Designatable.attribute(attributeDeclarant(h.target(), h.member()).naam, h.member());
            GeneratedFrom generatedFrom = new GeneratedFrom(member, OriginForm.STEPS_AFTER_SET);
            Designatable attribute = Designatable.attribute(h.element().naam, h.naam()), regel = Designatable.form(h.naam());
            kandidaat(candidates, attribute, generatedFrom);
            kandidaat(candidates, regel, generatedFrom);
            List<Designations.NamePart> d = helperNameParts(h, member);
            if (d != null) {
                parts.put(elementKey(attribute), d);
                parts.put(elementKey(regel), d);
            }
        }
        for (String[] a : anchorRules.values())
            kandidaat(candidates, Designatable.attribute(a[0], a[1]),
                    new GeneratedFrom(Designatable.form(a[2]), OriginForm.START_POINT));
        List<Designatable> all = model.designatable();
        for (Designatable b : all) {
            GeneratedFrom generatedFrom = candidates.get(elementKey(b));
            if (generatedFrom == null) continue;
            if (generatedFrom.form() == OriginForm.BESLISTABELRIJ) {
                // a table names no element, and a form of the table's name would be another declaration
                model.generated.put(b, generatedFrom);
                continue;
            }
            Designatable source = onName(all, generatedFrom.source());
            if (source == null && model.isOk())
                throw new IllegalStateException("het gegenereerde element " + b + " komt uit " + generatedFrom.source()
                        + ", en dat is geen element van het model");
            model.generated.put(b, source == null ? generatedFrom : new GeneratedFrom(source, generatedFrom.form()));
            List<Designations.NamePart> d = parts.get(elementKey(b));
            if (d != null) d = inModel(all, b, d);
            if (d != null) nameParts.put(b, d);
        }
    }

    /** The element of {@code alle} that {@code b} names under the name identity, or null. */
    private static Designatable onName(List<Designatable> all, Designatable b) {
        return all.stream().filter(x -> elementKey(x).equals(elementKey(b))).findFirst().orElse(null);
    }

    /** The pieces {@code parts} of the name of {@code element}, each declared part spelled as {@code alle} spells it;
     *  null where a part is no element of the model, which an ERROR of the model explains (in a model without one, a
     *  fault of the frontend). */
    private List<Designations.NamePart> inModel(List<Designatable> all, Designatable element,
                                                 List<Designations.NamePart> parts) {
        List<Designations.NamePart> result = new ArrayList<>();
        for (Designations.NamePart d : parts) {
            Designatable part = d.element() == null ? null : onName(all, d.element());
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
    private List<Designations.NamePart> helperNameParts(ExpressionBuilder.HelperMember h, Designatable member) {
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
                    Designatable.role(steps.get(i).factType(), steps.get(i).role())));
        }
        return result;
    }

    /** Records {@code uit} as the origin of {@code element}. Two different origins for one element are two forms that
     *  generate one name, which a refusal (WF-020, a member name taken) reports in a model with an error; in a model
     *  without one they are a fault of the frontend. */
    private void kandidaat(Map<String, GeneratedFrom> candidates, Designatable element, GeneratedFrom generatedFrom) {
        GeneratedFrom earlier = candidates.putIfAbsent(elementKey(element), generatedFrom);
        if (earlier != null && !earlier.equals(generatedFrom) && model.isOk())
            throw new IllegalStateException("het element " + element + " is gegenereerd uit " + earlier + " en uit "
                    + generatedFrom);
    }

    /** The name identity of an element (IR-SPEC ch. 2.1): its kind, owner and name under the frontend's norm. */
    private static String elementKey(Designatable b) {
        return b.category() + "\u0000" + (b.owner() == null ? "" : dev.uchar.regelspraak.emit.IrEmitter.norm(b.owner()))
                + "\u0000" + dev.uchar.regelspraak.emit.IrEmitter.norm(b.naam());
    }

    /** The type on {@code t}'s chain that declares attribute {@code naam}. */
    private static Objecttype attributeDeclarant(Objecttype t, String naam) {
        for (Objecttype k : t.chain())
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
            Dependencies.Form form = k.forms().get(0);
            String naam = Dependencies.withoutVersion(form.naam());
            String[] site = sites.get(dev.uchar.regelspraak.emit.IrEmitter.norm(naam));
            boolean wf045 = "WF-045".equals(k.wf());
            int cycleLine = site == null ? 0 : Integer.parseInt(site[1]);
            if (wf045)
                error(cycleLine, "RGS-096",
                        params("kind", FORM_KIND.get(form.category()), "name", naam, "wf", k.wf(), "sentence", k.sentence(),
                                "negativeEdge", Dependencies.carriesNegativeEdge(k),
                                "negativeEdgeClause", Msg.frontendIf(Dependencies.carriesNegativeEdge(k), "RGS-096.negative-edge")));
            else
                error(cycleLine, "RGS-097",
                        params("kind", FORM_KIND.get(form.category()), "name", naam, "wf", k.wf(), "sentence", k.sentence()));
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
        java.util.function.BiConsumer<String, List<String>> forms = (list, names) -> {
            for (int i = 0; i < names.size(); i++) {
                String naam = names.get(i);
                if (naam == null) continue;
                String plain = withoutVersion(naam);
                boolean continuation = !plain.equals(naam) && !naam.endsWith(" #1") && i > 0
                        && names.get(i - 1) != null && withoutVersion(names.get(i - 1)).equals(plain);
                if (!continuation) lists.computeIfAbsent(dev.uchar.regelspraak.emit.IrEmitter.norm(plain),
                        k -> new ArrayList<>()).add(list);
            }
        };
        forms.accept("rules", model.resolvedRegels.stream().map(ResolvedRule::naam).toList());
        forms.accept("objectCreations", model.objectCreations.stream().map(o -> o.naam()).toList());
        forms.accept("allocations", model.allocations.stream()
                .filter(v -> v.method() != ResolvedAllocation.Method.COMPLEX).map(v -> v.naam()).toList());
        forms.accept("consistencies", model.consistenties.stream().map(c -> c.naam()).toList());
        forms.accept("uniquenesses", java.util.stream.Stream.concat(model.uniquenesses.stream().map(u -> u.naam()),
                model.uniquenessUnions.stream().map(u -> u.naam())).toList());
        forms.accept("factCreations", model.factCreations.stream().map(f -> f.naam()).toList());
        forms.accept("prioritizedDefaults", model.prioritizedDefaults.stream().map(g -> g.naam()).toList());
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
            Msg verifier = arrays.size() == 1
                    ? Msg.frontend("RGS-098.duplicate-name", "name", first[0], "array", arrays.get(0))
                    : Msg.frontend("RGS-098.name-collision", "name", first[0], "other", carriers.get(1)[0],
                            "array", arrays.get(1));
            for (String[] following : carriers.subList(1, carriers.size()))
                error(Integer.parseInt(following[1]),
                        "RGS-098", params("name", following[0], "table", following[2], "earlierLine", first[1], "earlierTable", first[2], "sentence", verifier,
                                "inTable", Msg.frontendIf(following[2] != null, "RGS-098.in-table", "table", following[2]),
                                "earlierRow", Msg.frontendIf(first[2] != null, "RGS-098.earlier-row", "earlierTable", first[2])));
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
            model.diagnostics.add(new Diagnostic(Severity.ERROR, "RGS-099", params("expression", src(p)),
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
        List<ExpressionBuilder.HelperMember> members = builder.helperMembers();
        if (members.isEmpty()) return;
        Map<String, String[]> forms = formSites(doc);
        for (ExpressionBuilder.HelperMember h : members) {
            String[] site = forms.get(dev.uchar.regelspraak.emit.IrEmitter.norm(h.naam()));
            if (site != null) {
                error(h.targetSource().line(),
                        "RGS-100", params("rule", h.naam(), "objectType", h.element().naam, "occupiedBy", site[0], "sourceLine", site[1]));
                continue;
            }
            Expr.AttributeReference targetName = new Expr.AttributeReference(h.naam(), h.element(), "een " + h.element().naam, null,
                    h.type(), h.targetSource());
            model.resolvedRegels.add(new ResolvedRule(h.naam(), ResolvedRule.Kind.GELIJKSTELLING, targetName, helperValue(h),
                    null, Validity.ALTIJD, List.of()));
        }
    }

    /** The read the generated rule of {@code h} sets its member from: the reached member through the steps, from the
     *  type the member stands on. */
    private static Expr.AttributeReference helperValue(ExpressionBuilder.HelperMember h) {
        return new Expr.AttributeReference(h.member(), h.target(), "de " + h.element().naam, h.element().naam, h.type(),
                h.valueSource(), false, h.rollen(), List.of(), null, 0);
    }

    /** §9.10 build the {@link dev.uchar.regelspraak.ir.ResolvedRuleGroup} carriers (member RULE NAMES + the
     *  {@code recursive} marker) and run the fase-2 structural checks — a DOCUMENTED EXTENSION (see
     *  grammar/DIVERGENCES.md '§9.10 recursie / regelgroep'). Members were already resolved as ungrouped
     *  rules above (their {@link ResolvedRule}/objectcreatie/… entries keep the bare written name); this only
     *  records the grouping and validates the recursief marker. Fase-2 checks:
     *  (a) a recursief group with NO objectcreatie member -> ERROR (nothing can advance the chain);
     *  (b) an objectcreatie member WITHOUT any voorwaarde -> WARNING (termination is not automatable — the engine
     *      fire cap is the backstop, sound because the §13.4.6/§9 idempotence guard advances one generation per
     *      instance). The ungrouped cyclic-WARNING posture (RecursionTest) is unchanged. */
    private void ruleGroups(RegelSpraakDocumentContext doc) {
        for (RegelGroepContext g : doc.regelGroep()) {
            boolean recursive = g.RECURSIEF() != null;
            int line = g.getStart().getLine();
            headAnnotation(g, "Regelgroep '" + DefinitionResolver.src(g.kaleNaam()) + "'", "§9.10", line);
            List<String> members = new ArrayList<>();
            // Members are RULE names (IR-SPEC ch. 3.9, WF-033). A decision table is no rule form: it stands for the
            // rules of its rows, so those are what the group lists — the table's own name resolves to nothing.
            for (GroepLidContext member : g.groepLid())
                if (member.regel() != null) members.add(DefinitionResolver.src(member.regel().regelNaam()));
                else if (member.beslistabel() != null) members.addAll(rowRules.getOrDefault(member.beslistabel(), List.of()));
            String naam = DefinitionResolver.src(g.kaleNaam());
            model.ruleGroups.add(new dev.uchar.regelspraak.ir.ResolvedRuleGroup(naam, recursive, members, source(g.kaleNaam()), line));
            if (!recursive) continue;

            // §9.10 fase-2: which members are objectcreatie rules. The model holds one entry per version, named
            // "<naam> #<i>" when the rule has several, so members and creations meet on the name without the version
            // suffix, the key the verifier uses (WF-033), and each version is a member of its own.
            List<ResolvedObjectCreation> creations = model.objectCreations.stream()
                    .filter(oc -> members.stream().anyMatch(l -> withoutVersion(l).equals(withoutVersion(oc.naam())))).toList();
            if (creations.isEmpty()) {
                // the verifier admits a relation creation as the carrier too (ch. 5.5): a group with no creation of
                // either kind is refused in its words, one with a relation creation only in this frontend's (§9.10)
                boolean hasFactCreation = model.factCreations.stream()
                        .anyMatch(fc -> members.stream().anyMatch(l -> withoutVersion(l).equals(withoutVersion(fc.naam()))));
                if (hasFactCreation)
                    model.diagnostics.add(new Diagnostic(Severity.ERROR, "RGS-101",
                            params("name", naam), line));
                else
                    model.diagnostics.add(new Diagnostic(Severity.ERROR, "RGS-102",
                            params("name", naam), line));
                continue;
            }
            // each generation of a recursive group mints new instances, so its object creation carries a guard: the
            // verifier refuses an unguarded one (WF-033), and so does the frontend, in the verifier's words
            for (ResolvedObjectCreation oc : creations)
                if (oc.condition() == null)
                    model.diagnostics.add(new Diagnostic(Severity.ERROR,
                            "RGS-103", params("name", naam, "creation", oc.naam()), line));
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
    private record DefaultsTarget(Objecttype onderwerp, boolean characteristic, String naam) {
        boolean equal(DefaultsTarget a) {
            return a != null && onderwerp == a.onderwerp && characteristic == a.characteristic && naam.equalsIgnoreCase(a.naam);
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
        java.util.Set<ParserRuleContext> refused = ruleGroupScope(doc);
        treeDefaults(doc, refused);
        groupDefaults(doc, refused);
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
        for (ResolvedRule r : model.resolvedRegels) {
            if (!isNegated(r) || refusedTargets.contains(r) || blockNames.contains(withoutVersion(r.naam()))) continue;
            refusedTargets.add(r);
            if (!reported.add(withoutVersion(r.naam()))) continue;
            Expr.AttributeReference targetName = (Expr.AttributeReference) r.target();
            int table = tableLine(r.naam());
            // the lowered rule writes false into the characteristic, which the verifier refuses in these words
            model.diagnostics.add(new Diagnostic(Severity.ERROR,
                    "RGS-104", params("rule", withoutVersion(r.naam()), "table", rowTable(r.naam())),
                    table >= 0 ? table : targetName.source() == null ? 0 : targetName.source().line()));
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
            if (pd.isCharacteristic())
                perTarget.computeIfAbsent(targetKey(pd.onderwerp(), true, pd.target()), k -> new ArrayList<>()).add(pd);
        for (List<PrioritizedDefaults> groups : perTarget.values()) {
            if (groups.stream().anyMatch(pd -> decidesTrue(pd.defaults()))) continue;
            PrioritizedDefaults first = groups.get(0);
            if (otherCharacteristicWriter(model.objectTypeByName(first.onderwerp()), first.target())) continue;
            boolean more = groups.size() > 1;
            Origin h = pdOrigin.getOrDefault(first, new Origin(0, "defaults-groep"));
            // one template for one group and one for several: the verb and the nouns around it agree with the number
            model.diagnostics.add(new Typing.Coded("RGS-105",
                    params("head", h.head(), "groupNames", enumeration(groups.stream().map(PrioritizedDefaults::naam).toList()), "subject", first.onderwerp(), "target", first.target(), "plural", more))
                    .variant(more ? ".plural" : "").at(Severity.WARNING, h.line()));
        }
    }

    /** Whether characteristic {@code kenmerk} has a writer that composes with the defaults groups on {@code type}:
     *  a live plain rule on {@code type} itself (WF-050's second writer), or a defaults group or a live plain rule on
     *  a strict ancestor or descendant of {@code type} (ch. 7.10). A sibling subtype composes with nothing here. */
    private boolean otherCharacteristicWriter(Objecttype type, String characteristic) {
        if (type == null) return false;
        for (PrioritizedDefaults pd : model.prioritizedDefaults)
            if (pd.isCharacteristic() && pd.target().equalsIgnoreCase(characteristic)
                    && inInheritanceLine(type, model.objectTypeByName(pd.onderwerp()))) return true;
        for (ResolvedRule r : model.resolvedRegels)
            if (!refusedTargets.contains(r) && targetOf(r) instanceof DefaultsTarget d && d.characteristic()
                    && d.naam().equalsIgnoreCase(characteristic) && (d.onderwerp() == type || inInheritanceLine(type, d.onderwerp())))
                return true;
        return false;
    }

    /** Whether {@code ander} is a strict ancestor or a strict descendant of {@code type}. */
    private static boolean inInheritanceLine(Objecttype type, Objecttype other) {
        return other != null && other != type && (type.isA(other) || other.isA(type));
    }

    /** Whether some default in {@code defaults}, nested exception groups included, decides 'waar'. */
    private static boolean decidesTrue(List<Default> defaults) {
        if (defaults == null) return false;
        for (Default d : defaults) {
            if (Boolean.TRUE.equals(d.decision())) return true;
            if (d.exceptions() != null)
                for (var group : d.exceptions().members()) if (decidesTrue(group.defaults())) return true;
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
            perTarget.computeIfAbsent(targetKey(pd.onderwerp(), pd.isCharacteristic(), pd.target()), k -> new ArrayList<>()).add(pd);
        for (var e : perTarget.entrySet()) {
            List<PrioritizedDefaults> groups = e.getValue();
            String targetText = groups.get(0).onderwerp() + "." + groups.get(0).target();
            Msg.Enumeration groupNames = enumeration(groups.stream().map(PrioritizedDefaults::naam).toList());
            List<String> others = new ArrayList<>();
            for (ResolvedRule r : model.resolvedRegels) {
                if (refusedTargets.contains(r) || blockNames.contains(withoutVersion(r.naam()))) continue;
                DefaultsTarget d = targetOf(r);
                if (d != null && e.getKey().equals(targetKey(d.onderwerp().naam, d.characteristic(), d.naam())))
                    others.add("'" + r.naam() + "'" + rowTable(r.naam()));
            }
            // both clauses in the verifier's WF-050 sentences, the groups and the other writers named before them
            if (!others.isEmpty()) {
                Origin h = pdOrigin.getOrDefault(groups.get(0), new Origin(0, "defaults-groep"));
                model.diagnostics.add(new Diagnostic(Severity.ERROR,
                        "RGS-106", params("head", h.head(), "groupNames", groupNames, "others", String.join(", ", others), "target", targetText,
                                "groupsLabel", Msg.frontend(groups.size() == 1 ? "RGS-106.group-label" : "RGS-106.groups-label"),
                                "othersLabel", Msg.frontend(others.size() == 1 ? "RGS-106.rule-label" : "RGS-106.rules-label")),
                        h.line()));
            }
            if (groups.size() > 1 && !groups.stream().allMatch(PrioritizedDefaults::unordered)) {
                Origin h = pdOrigin.getOrDefault(groups.get(groups.size() - 1), new Origin(0, "defaults-groep"));
                model.diagnostics.add(new Diagnostic(Severity.ERROR,
                        "RGS-107", params("head", h.head(), "groupNames", groupNames, "target", targetText), h.line()));
            }
        }
    }

    private static String targetKey(String onderwerp, boolean characteristic, String naam) {
        return onderwerp + "|" + (characteristic ? "kenmerk" : "attribuut") + "|" + naam.toLowerCase(java.util.Locale.ROOT);
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
        java.util.Set<ParserRuleContext> refused =
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
                String base = carrierOf(kind);
                // Only where the lift would happen: a base that resolves to no rule gets the ordinary "no base
                // rule" refusal of the lowering, which is the true one there.
                Membership member = base == null || resolvedOf(base).isEmpty() ? null : memberGroup.get(base);
                if (member != null) {
                    // the lift takes the member out of the rule forms, and the verifier refuses the group that still
                    // names it (WF-033); the placement, where it holds too, stays in the context
                    refused.add(block);
                    model.diagnostics.add(new Diagnostic(Severity.ERROR,
                            "RGS-108", params("head", blockHead(block), "table", member.table(), "group", member.group(), "groupBefore", groupBefore, "member", base,
                                    "memberKind", Msg.frontend(member.table() == null ? "RGS-108.member" : "RGS-108.table-row", "table", member.table()),
                                    "placement", Msg.frontendIf(groupBefore != null, "RGS-108.placement", "groupBefore", groupBefore)),
                            block.getStart().getLine()));
                } else if (groupBefore != null) {
                    // the placement alone: every group keeps its members, so the document is well-formed, with the
                    // block and every rule after it outside the group, and no reader refuses it
                    refused.add(block);
                    model.diagnostics.add(new Diagnostic(Severity.ERROR,
                            "RGS-109", params("head", blockHead(block), "groupBefore", groupBefore, "carrier", base, "firstGroup", firstGroup,
                                    // a carrier that is itself a block is no base rule: the message names what was written
                                    "carrierPart", base == null ? "" : Msg.frontend(nodeNames.contains(base) ? "RGS-109.and-carrier" : "RGS-109.and-base")),
                            block.getStart().getLine()));
                }
            }
            preceding = kind;
        }
        return refused;
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
     *  refused where the name is read ({@link #characteristic}, dialect §4), and a positive one still reaches the
     *  verifier's WF-022 — no card has taken that one yet. */
    private void checkRuleTargets() {
        for (ResolvedRule r : model.resolvedRegels) {
            if (!(r.target() instanceof Expr.AttributeReference a)) continue;
            int line = a.source() == null ? 0 : a.source().line();
            String written = a.source() == null ? a.attribute() : a.source().snippet();
            // a name that is both an attribute and a characteristic of the subject decides no member, and so no rule
            // kind; before this check a rule on it ended in the emitter's exception
            if (a.objectType() != null && a.attribute() != null && a.objectType().attribute(a.attribute()) != null
                    && a.objectType().characteristicName(a.attribute()) != null) {
                refusedTargets.add(r);
                model.diagnostics.add(new Diagnostic(Severity.ERROR,
                        "RGS-110", params("written", written, "attribute", a.attribute(), "objectType", a.objectType().naam), line));
                continue;
            }
            Typing.Coded error = null;
            if (a.objectType() == null)
                error = new Typing.Coded("RGS-306", params("written", written, "subjectSource", a.subjectSource()));
            else if (r.category() != ResolvedRule.Kind.KENMERKTOEKENNING && a.objectType().attribute(a.attribute()) == null
                    && cellAsName(a.objectType(), a.attribute()) != null)
                error = new Typing.Coded("RGS-307",
                        params("written", written, "attribute", a.attribute(), "cell", cellAsName(a.objectType(), a.attribute())));
            else if (r.category() != ResolvedRule.Kind.KENMERKTOEKENNING && a.objectType().attribute(a.attribute()) != null
                    && cellAfterSubject(a.objectType(), a.attribute(), a.subjectSource()) != null)
                error = new Typing.Coded("RGS-308",
                        params("written", written, "cell", cellAfterSubject(a.objectType(), a.attribute(), a.subjectSource())));
            else if (a.navigationRole() != null || (a.navigationPath() != null && !a.navigationPath().isEmpty()))
                // "Het kadegeld van zijn thuishaven moet …": the target reads another instance's attribute, which
                // ended in the emitter's exception, since the target fixes the rule's subject
                error = new Typing.Coded("RGS-111", params("written", written, "subjectSource", a.subjectSource()));
            else if (r.category() != ResolvedRule.Kind.KENMERKTOEKENNING && a.objectType().attribute(a.attribute()) == null)
                error = a.attribute().matches("(?i).*\\svan\\s.*")
                        ? new Typing.Coded("RGS-112",
                                params("written", written, "attribute", a.attribute(), "objectType", a.objectType().naam))
                        : new Typing.Coded("RGS-113",
                                params("written", written, "attribute", a.attribute(), "objectType", a.objectType().naam));
            if (error != null) {
                refusedTargets.add(r);
                model.diagnostics.add(error.frame("rule-target", "written", written)
                        .at(Severity.ERROR, line));
            }
        }
    }

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
    private String cellAfterSubject(Objecttype ot, String attribute, String subjectText) {
        if (subjectText == null) return null;
        for (String dn : ot.dimensionsOf(attribute)) {
            RegelSpraakModel.Dimension d = model.dimensionByName(dn);
            if (d == null || d.voorzetsel() == null) continue;
            for (String label : d.waarden())
                if (subjectText.toLowerCase(java.util.Locale.ROOT).endsWith(
                        (" " + d.voorzetsel() + " " + label).toLowerCase(java.util.Locale.ROOT)))
                    return cellText(ot.attributeName(attribute), d, label);
        }
        return null;
    }

    private static String cellText(String attribute, RegelSpraakModel.Dimension d, String label) {
        return "cel van het gedimensioneerde attribuut '" + attribute + "' (dimensie " + d.naam() + ", label '"
                + label + "')";
    }

    /** WF-112 (maintainer decision 2026-09-23), producer side: a time window never sits under 'geen van de' or
     *  a counting quantifier — as a member of such a composite at any depth, or in the filter or test of such a
     *  quantifier fold. The window gates the RULE (RUNTIME-SEMANTICS ch. 3.10); a quantifier that complements or
     *  counts its verdict reads it as something no clause specifies. Before this fence each such document was
     *  written without a word and refused by the verifier. The walk mirrors the verifier's: every condition the
     *  emitter writes (the roots {@code Resolver.reportUnresolved} sweeps), into the conditions nested in
     *  expressions, with {@link Condition#containsTimeWindow} as the window test — the reach of its
     *  {@code cond_has_window}. One ERROR per offending composite or fold, so {@link Resolver#irMode} writes
     *  nothing. */
    private void checkWindowPositions() {
        for (ResolvedRule r : model.resolvedRegels) {
            windowPosition(r.condition()); windowPositionIn(r.target()); windowPositionIn(r.value());
        }
        for (var d : model.resolvedDayKindDefinitions) windowPosition(d.condition());
        for (var oc : model.objectCreations) {
            windowPosition(oc.condition());
            for (var t : oc.initialisations()) windowPositionIn(t.value());
        }
        for (var v : model.allocations) windowPosition(v.condition());
        for (var c : model.consistenties) windowPosition(c.assertie());
        for (var fc : model.factCreations) windowPosition(fc.condition());
        java.util.Set<Condition> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (var pd : model.prioritizedDefaults) windowPositionDefaults(pd.defaults(), seen);
    }

    /** The defaults tree's justifications and consequences. A restrictive exception repeats its carrier's conjuncts,
     *  the very same nodes, so each justification is walked with {@link Condition#eachConjunctOnce}: a window under
     *  a counting quantifier in a base's condition is one refusal, not one per exception above it. */
    private void windowPositionDefaults(List<Default> defaults, java.util.Set<Condition> seen) {
        if (defaults == null) return;
        for (Default d : defaults) {
            Condition.eachConjunctOnce(d.condition(), seen, this::windowPosition);
            windowPositionIn(d.consequence());
            if (d.exceptions() != null)
                for (var group : d.exceptions().members()) windowPositionDefaults(group.defaults(), seen);
        }
    }

    private void windowPosition(Condition c) {
        if (c == null) return;
        switch (c) {
            case Condition.Compound s -> {
                if (countsOrNegates(s.quantifier()) && s.components().stream().anyMatch(Condition::containsTimeWindow))
                    windowError(s.quantifier(), s.threshold(), s.source(), "als lid van een samengestelde voorwaarde");
                s.components().forEach(this::windowPosition);
            }
            case Condition.QuantifierFold k -> {
                if (countsOrNegates(k.quantifier()) && (Condition.containsTimeWindow(k.filter()) || Condition.containsTimeWindow(k.test())))
                    windowError(k.quantifier(), k.threshold(), k.source(), "in het filter of de toets van een kwantorvouw");
                windowPosition(k.filter());
                windowPosition(k.test());
            }
            case Condition.Comparison v -> { windowPositionIn(v.left()); windowPositionIn(v.right()); }
            case Condition.Predicate p -> windowPositionIn(p.operand());
            case Condition.TimeWindow p -> { windowPositionIn(p.from()); windowPositionIn(p.until()); }
            default -> { }
        }
    }

    private void windowPositionIn(Expr e) {
        if (e == null) return;
        switch (e) {
            case Expr.Binary b -> { windowPositionIn(b.left()); windowPositionIn(b.right()); }
            case Expr.FunctionCall f -> {
                f.arguments().forEach(this::windowPositionIn);
                windowPositionIn(f.emptyFallback());
                windowPosition(f.filter());
            }
            case Expr.Rounding a -> windowPositionIn(a.base());
            case Expr.Clamp b -> {
                windowPositionIn(b.base()); windowPositionIn(b.minimum()); windowPositionIn(b.maximum());
            }
            case Expr.TextComposition t -> t.parts().forEach(d -> {
                if (d instanceof Expr.TextComposition.EmbeddedExpression i) windowPositionIn(i.expr());
            });
            case Expr.DimensionCellRead dc -> windowPositionIn(dc.attribute());
            case Expr.Let l -> { windowPositionIn(l.value()); windowPositionIn(l.body()); }
            case Expr.IfThenElse k -> {
                windowPosition(k.condition()); windowPositionIn(k.then()); windowPositionIn(k.otherwise());
            }
            case Expr.InstanceSetUnion u -> u.branches().forEach(t -> windowPosition(t.filter()));
            default -> { }
        }
    }

    /** 'geen van de' and the three counting quantifiers: the ones that complement or count a verdict. */
    private static boolean countsOrNegates(Condition.Quantifier k) {
        return k == Condition.Quantifier.GEEN || k == Condition.Quantifier.TEN_MINSTE
                || k == Condition.Quantifier.TEN_HOOGSTE || k == Condition.Quantifier.PRECIES;
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
        for (ResolvedRule r : model.resolvedRegels) {
            Objecttype s = r.target() instanceof Expr.AttributeReference d ? d.objectType() : null;
            navigation(r.value(), s, reported);
            navigation(r.condition(), s, reported);
        }
        for (var d : model.resolvedDayKindDefinitions) navigation(d.condition(), null, reported);
        for (var oc : model.objectCreations) {
            for (var t : oc.initialisations()) navigation(t.value(), oc.subject(), reported);
            navigation(oc.condition(), oc.subject(), reported);
        }
        for (var v : model.allocations)
            if (v.method() != ResolvedAllocation.Method.COMPLEX) navigation(v.condition(), v.sourceType(), reported);
        for (var c : model.consistenties) navigation(c.assertie(), c.subject(), reported);
        for (var fc : model.factCreations) navigation(fc.condition(), fc.anchor(), reported);
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
            if (x instanceof Expr.AttributeReference a && !a.set() && reported.add(a)) {
                Typing.Coded error = swallowedNameError(a);
                if (error != null)
                    model.diagnostics.add(error.at(Severity.ERROR, a.source() == null ? 0 : a.source().line()));
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
            if (x instanceof Expr.Constant c && c.naam() != null && !c.naam().equals("de dag")
                    && !dev.uchar.regelspraak.ir.Spelling.CONTEXT_VALUE_NAME.rows().containsKey(c.naam())
                    && reported.add(c))
                model.diagnostics.add(new Diagnostic(Severity.ERROR,
                        "RGS-114", params("name", c.naam(), "expression", c.source()),
                        c.source() == null ? 0 : c.source().line()));
            return false;
        });
    }

    /** Every expression the emitter writes, after the lift, handed to {@code visitor} (which returns false to walk on):
     *  rule values and conditions, day-kind conditions, creation values and conditions, allocation and fact-creation
     *  conditions, consistency assertions and the defaults, window bounds and fold sources included. */
    private void everyWrittenExpr(java.util.function.Predicate<Expr> visitor) {
        for (ResolvedRule r : model.resolvedRegels) {
            Expr.contains(r.value(), visitor);
            Condition.containsOperand(r.condition(), visitor);
        }
        for (var d : model.resolvedDayKindDefinitions) Condition.containsOperand(d.condition(), visitor);
        for (var oc : model.objectCreations) {
            for (var t : oc.initialisations()) Expr.contains(t.value(), visitor);
            Condition.containsOperand(oc.condition(), visitor);
        }
        for (var v : model.allocations) Condition.containsOperand(v.condition(), visitor);
        for (var c : model.consistenties) Condition.containsOperand(c.assertie(), visitor);
        for (var fc : model.factCreations) Condition.containsOperand(fc.condition(), visitor);
        for (var pd : model.prioritizedDefaults) writtenDefaults(pd.defaults(), visitor);
    }

    private static void writtenDefaults(List<Default> defaults, java.util.function.Predicate<Expr> visitor) {
        if (defaults == null) return;
        for (Default d : defaults) {
            Condition.containsOperand(d.condition(), visitor);
            Expr.contains(d.consequence(), visitor);
            if (d.exceptions() != null)
                for (ExceptionGroup g : d.exceptions().members()) writtenDefaults(g.defaults(), visitor);
        }
    }

    /** The refusal of {@link #checkSwallowedNames} for {@code a}, or null where its name is not of that shape: the
     *  first 'van' behind which a declared attribute (or its written plural) of the read's type stands. */
    private static Typing.Coded swallowedNameError(Expr.AttributeReference a) {
        Objecttype ot = a.objectType();
        String naam = a.attribute();
        if (ot == null || naam == null || !(a.type() instanceof Datatype.Unknown) || ot.hasMember(naam)) return null;
        java.util.regex.Matcher m = VAN.matcher(naam);
        while (m.find()) {
            String attribute = PluralMatcher.findAttribute(ot, stripArticle(naam.substring(m.end())));
            if (attribute != null)
                return new Typing.Coded("RGS-115",
                        params("expression", a.source(), "prefix", naam.substring(0, m.start()), "attribute", attribute,
                                "name", naam, "objectType", ot.naam));
        }
        return null;
    }

    private static final java.util.regex.Pattern VAN = java.util.regex.Pattern.compile("\\s+van\\s+");

    /** A carrier's conjuncts repeat under its exceptions as the very same nodes, so {@code gemeld} keeps one
     *  refusal per node. */
    private void navigationDefaults(List<Default> defaults, Objecttype subject, Set<Object> reported) {
        if (defaults == null) return;
        for (Default d : defaults) {
            navigation(d.condition(), subject, reported);
            navigation(d.consequence(), subject, reported);
            if (d.exceptions() != null)
                for (ExceptionGroup g : d.exceptions().members()) navigationDefaults(g.defaults(), subject, reported);
        }
    }

    private void navigation(Expr e, Objecttype subject, Set<Object> reported) {
        if (e == null) return;
        switch (e) {
            case Expr.AttributeReference a -> {
                if (a.set() || !reported.add(a)) return;
                Typing.Coded error = dev.uchar.regelspraak.emit.IrEmitter.unboundRead(model, subject, a)
                        ? unboundReadError(a, subject) : manyStepError(a, subject);
                if (error == null) error = memberError(a, subject, false);
                if (error != null) model.diagnostics.add(error.at(Severity.ERROR, a.source().line()));
                else reportDoubleReading(a);
            }
            case Expr.FunctionCall f -> {
                boolean foldSource = dev.uchar.regelspraak.emit.IrEmitter.isFoldSource(f);
                var refusal = dev.uchar.regelspraak.emit.IrEmitter.foldRefusalOf(model, subject, f);
                if (refusal != null && reported.add(f))
                    model.diagnostics.add(foldError(f, subject, refusal).at(Severity.ERROR, f.source().line()));
                // a fold's source is read through its range, never as a read of its own
                for (int i = foldSource ? 1 : 0; i < f.arguments().size(); i++)
                    navigation(f.arguments().get(i), subject, reported);
                navigation(f.emptyFallback(), subject, reported);
                // the filter reads each contributor; a dimension fold has none and reads the subject's labels
                Objecttype element = f.dimensieSelectie() == null ? f.elementObjectType() : null;
                navigation(f.filter(), element != null ? element : subject, reported);
            }
            case Expr.Binary b -> {
                navigation(b.left(), subject, reported);
                navigation(b.right(), subject, reported);
            }
            case Expr.Rounding a -> navigation(a.base(), subject, reported);
            case Expr.Clamp b -> {
                navigation(b.base(), subject, reported);
                navigation(b.minimum(), subject, reported);
                navigation(b.maximum(), subject, reported);
            }
            case Expr.TextComposition t -> {
                for (Expr.TextComposition.Part d : t.parts())
                    if (d instanceof Expr.TextComposition.EmbeddedExpression i) navigation(i.expr(), subject, reported);
            }
            case Expr.DimensionCellRead c -> navigation(c.attribute(), subject, reported);
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
            case Expr.Constant ignored -> { }
            case Expr.Unresolved ignored -> { }
        }
    }

    private void navigation(Condition c, Objecttype subject, Set<Object> reported) {
        if (c == null) return;
        switch (c) {
            case Condition.Comparison v -> {
                navigation(v.left(), subject, reported);
                navigation(v.right(), subject, reported);
            }
            case Condition.Predicate p -> navigation(p.operand(), subject, reported);
            case Condition.Compound s -> s.components().forEach(o -> navigation(o, subject, reported));
            // the filter and the test read each contributor
            case Condition.QuantifierFold k -> {
                Objecttype element = k.elementObjectType() != null ? k.elementObjectType() : subject;
                navigation(k.filter(), element, reported);
                navigation(k.test(), element, reported);
            }
            // every bound that navigates is the window check's refusal; an own read whose name took in a role is not
            case Condition.TimeWindow p -> {
                windowBoundMember(p.from(), subject, reported);
                windowBoundMember(p.until(), subject, reported);
            }
            case Condition.CharacteristicCheck ignored -> { }
            case Condition.RoleMembership r -> roleCheckBinding(r, r.role(), r.otherToFiller(), r.source(), subject, reported);
            case Condition.FarRoleMembership r ->
                    roleCheckBinding(r, r.backref(), r.filler(), r.source(), subject, reported);
            case Condition.RuleStatusCheck ignored -> { }
            case Condition.Unresolved ignored -> { }
        }
    }

    /** A role check whose binding is not one relation: the emitter writes a role check as its fact type and role, and
     *  refused it with an exception where the written role names none from the subject or several ("de Haven heeft
     *  een ligger" where the Haven plays the written role in two fact types). The refusal is the loader's sentence for
     *  the check without its binding. A day kind has no subject, and the day-kind check refuses the role check there
     *  already. */
    private void roleCheckBinding(Condition c, String roleName, Objecttype other, Source source, Objecttype subject,
                                 Set<Object> reported) {
        if (subject == null) return;
        int n = dev.uchar.regelspraak.emit.IrEmitter.roleCheckBindings(model, subject, c);
        if (n == 1 || !reported.add(c)) return;
        String against = " van " + subject.naam + " tegenover " + (other == null ? "?" : other.naam);
        if (n == 0)
            model.diagnostics.add(new Diagnostic(Severity.ERROR, "RGS-116",
                    params("expression", source, "roleName", roleName, "against", against), source == null ? 0 : source.line()));
        else
            model.diagnostics.add(new Diagnostic(Severity.ERROR, "RGS-117",
                    params("expression", source, "count", n, "roleName", roleName, "against", against), source == null ? 0 : source.line()));
    }

    /** The refusal of a read without a binding: what did not bind, then the verifier's WF-053 sentence for the
     *  document the emitter would write without the path — a read that names another type than the subject's.
     *  Where the read names the subject's own type, or there is no subject, that document is the subject's own
     *  read, which no reader refuses, and the refusal keeps this frontend's words. */
    private Typing.Coded unboundReadError(Expr.AttributeReference a, Objecttype subject) {
        Msg van = subject == null ? Msg.frontend("RGS-118.without-subject") : Msg.frontend("RGS-118.from-subject", "subject", subject.naam);
        Msg reason;
        if (a.navigationPath() != null && !a.navigationPath().isEmpty())
            reason = Msg.frontend("RGS-118.chain-unbound", "from", van);
        else if (a.navigationRole() != null)
            reason = Msg.frontend("RGS-118.role-ambiguous", "role", a.navigationRole(), "from", van);
        else reason = pairReason(subject, a.objectType(), a.subjectSource());
        if (subject != null && a.objectType() != null && a.objectType() != subject)
            return new Typing.Coded("RGS-118",
                    params("expression", a.source(), "reason", reason, "subject", subject.naam, "objectType", a.objectType().naam));
        return new Typing.Coded("RGS-119", params("expression", a.source(), "reason", reason));
    }

    /** The refusal of a read whose path reaches a to-many role in a slot that takes one value, in the verifier's
     *  WF-053 sentence, one per such hop; null where no hop is to-many. A §13.4.7 fact creation's condition on the
     *  created relation's target is one: the wire reads that condition from the anchor, so the target is reached
     *  over the to-many role. */
    private Typing.Coded manyStepError(Expr.AttributeReference a, Objecttype subject) {
        List<RegelSpraakModel.RoleBindingRef> bindingPath =
                dev.uchar.regelspraak.emit.IrEmitter.navigationBindings(model, subject, a);
        List<Msg> sentences = new ArrayList<>();
        List<String> hops = new ArrayList<>(), roles = new ArrayList<>();
        for (int i = 0; i < bindingPath.size(); i++)
            if (toMany(bindingPath.get(i))) {
                sentences.add(manyStepSentence(i, bindingPath.get(i).role()));
                hops.add(String.valueOf(i));
                roles.add(bindingPath.get(i).role());
            }
        // one sentence per to-many hop, in path order; the template renders them joined
        return sentences.isEmpty() ? null
                : new Typing.Coded("RGS-120",
                        params("expression", a.source(), "hops", String.join(", ", hops), "roles", String.join(", ", roles),
                                "sentences", sentences));
    }

    /** WF-053's sentence for hop {@code i} over the to-many role {@code roleName} of a read that takes one value. */
    private static Msg manyStepSentence(int i, String roleName) {
        return Msg.frontend("RGS-120.hop", "hop", i, "role", roleName);
    }

    /** A window bound that is an own read: {@link #memberError} for the role its name took in. A bound that navigates is
     *  the window check's to refuse (WF-111), so one defect keeps one refusal. */
    private void windowBoundMember(Expr limit, Objecttype subject, Set<Object> reported) {
        if (!(limit instanceof Expr.AttributeReference a) || a.set() || !reported.add(a)) return;
        Typing.Coded error = memberError(a, subject, true);
        if (error != null) model.diagnostics.add(error.at(Severity.ERROR, a.source().line()));
        else reportDoubleReading(a);
    }

    /** A declared attribute with 'van' in its name wins over the navigation the same words spell — "de premie van de
     *  jutter van de Vondst" reads the Vondst's 'premie van de jutter', not the 'premie' of its jutter — and until
     *  2026-10-01 the author was not told (maintainer decision: a WARNING, the declaration keeps winning). The walk is
     *  {@link #memberError}'s: from the type the read stands on, over the roles its name took in, outward; it warns where a
     *  role is walked and the type it reaches declares the member the remaining words name. A to-many step ends the
     *  walk: that navigation is no read of one value. */
    private void reportDoubleReading(Expr.AttributeReference a) {
        String naam = a.attribute();
        Objecttype t = a.objectType();
        if (a.set() || naam == null || t == null || t.attribute(naam) == null) return;
        String[] parts = naam.split("\\s+van\\s+");
        Objecttype cur = t;
        for (int i = parts.length - 1; i >= 1; i--) {
            RegelSpraakModel.ChainHop hop = model.writtenHop(cur, stripArticle(parts[i]));
            if (hop == null || model.toManyRole(cur, hop.targetName(), hop.role())) return;
            cur = hop.targetName();
            String member = PluralMatcher.findAttribute(cur, String.join(" van ", java.util.Arrays.copyOfRange(parts, 0, i)));
            if (member != null) {
                var d = new Diagnostic(Severity.WARNING,
                        "RGS-121", params("expression", a.source(), "attribute", naam, "objectType", t.naam, "member", member, "otherObjectType", cur.naam, "role", hop.role()),
                        a.source() == null ? 0 : a.source().line());
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
    private Typing.Coded memberError(Expr.AttributeReference a, Objecttype subject, boolean onlyOwn) {
        String naam = a.attribute();
        if (subject == null || a.set() || naam == null) return null;
        Objecttype t = a.objectType();
        if (t == null) return onlyOwn ? null : withoutTypeError(a, subject, naam);
        if (swallowedNameError(a) != null) return null;
        List<RegelSpraakModel.RoleBindingRef> bindingPath =
                dev.uchar.regelspraak.emit.IrEmitter.navigationBindings(model, subject, a);
        if (onlyOwn && !bindingPath.isEmpty()) return null;
        String roleName = bindingPath.isEmpty() ? null : bindingPath.get(bindingPath.size() - 1).role();
        Objecttype fillerType = bindingPath.isEmpty() ? null : roleFiller(bindingPath.get(bindingPath.size() - 1));
        if (t.attribute(naam) != null || t.characteristicName(naam) != null) {
            if (bindingPath.isEmpty()) return null;    // an own read; of a characteristic, the verifier's (WF-080)
            if (downward(fillerType, t)) return downwardError(a, t, fillerType, roleName);
            return t.attribute(naam) != null ? null : characteristicAsValueError(a, naam, t, roleName);
        }
        String[] parts = naam.split("\\s+van\\s+");
        Objecttype cur = t;
        String member = naam;
        int hops = bindingPath.size();
        for (int i = parts.length - 1; i >= 1; i--) {
            String step = stripArticle(parts[i]);
            RegelSpraakModel.ChainHop hop = model.writtenHop(cur, step);
            if (hop == null) {
                // a role or type name that binds no step from here: the author wrote a navigation, not this name
                Msg reason = stepReason(cur, step);
                if (reason == null) break;
                return new Typing.Coded("RGS-122",
                        params("expression", a.source(), "reason", reason, "attribute", naam, "objectType", t.naam));
            }
            if (model.toManyRole(cur, hop.targetName(), hop.role()))
                return new Typing.Coded("RGS-120",
                        params("expression", a.source(), "hops", hops, "roles", hop.role(),
                                "sentences", List.of(manyStepSentence(hops, hop.role()))));
            hops++;
            fillerType = model.roleTargetTypeToSubject(cur, hop.role());
            cur = hop.targetName();
            roleName = hop.role();
            member = String.join(" van ", java.util.Arrays.copyOfRange(parts, 0, i));
            String attribute = PluralMatcher.findAttribute(cur, member);
            if (attribute != null)
                return downward(fillerType, cur) ? downwardError(a, cur, fillerType, roleName)
                        : new Typing.Coded("RGS-309",
                                params("expression", a.source(), "attribute", attribute, "fillerType", cur.naam,
                                        "role", roleName, "name", naam, "objectType", t.naam));
        }
        if (hops == 0) return null;
        if (downward(fillerType, cur)) return downwardError(a, cur, fillerType, roleName);
        if (cur.characteristicName(member) != null) return characteristicAsValueError(a, member, cur, roleName);
        return memberMissingError(a, member, cur, roleName);
    }

    /** Why the step {@code step} binds no hop from {@code van}, where it names an object type or a role anywhere in
     *  the model — a word that only stands in a navigation; null where it names neither. A type name is read as the
     *  type, as {@link RegelSpraakModel#chainHop} reads it, so the pair decides. */
    private Msg stepReason(Objecttype van, String step) {
        Objecttype type = model.objectTypeByName(step);
        if (type != null) {
            int n = model.connectingRoleCount(van, type);
            return Msg.frontend("RGS-122.reads-as-type", "step", step, "type", type.naam,
                    "connection", n == 0 ? Msg.frontend("RGS-122.no-relation", "from", van.naam, "type", type.naam)
                            : Msg.frontend("RGS-122.relations", "count", n, "from", van.naam, "type", type.naam));
        }
        for (FactType ft : model.feittypen.values())
            for (Role r : ft.rollen)
                if (r.naam().equalsIgnoreCase(step) || r.plural() != null && r.plural().equalsIgnoreCase(step))
                    return Msg.frontend("RGS-122.role-ambiguous", "step", step, "from", van.naam);
        return null;
    }

    /** The declared filler of the role a binding reaches, or null where the binding names none. */
    private Objecttype roleFiller(RegelSpraakModel.RoleBindingRef step) {
        FactType ft = model.feittypen.get(step.factType());
        if (ft == null) return null;
        for (Role r : ft.rollen) if (r.naam().equals(step.role())) return model.objectTypeByName(r.objecttypeNaam());
        return null;
    }

    /** Whether the read names a strict subtype of the filler its last role reaches: by the type's name ("de toeslag van
     *  de Strandjutter van de Vondst" over a role a Jutter fills), which only the instances of that subtype satisfy. */
    private static boolean downward(Objecttype fillerType, Objecttype endpoint) {
        return fillerType != null && endpoint != fillerType && endpoint.isA(fillerType);
    }

    private static Typing.Coded downwardError(Expr.AttributeReference a, Objecttype endpoint, Objecttype fillerType, String roleName) {
        return new Typing.Coded("RGS-123",
                params("expression", a.source(), "endpoint", endpoint.naam, "roleName", roleName, "fillerType", fillerType.naam));
    }

    /** A read whose subject phrase names no role and no type from the subject: the emitter writes it without a type. */
    private static Typing.Coded withoutTypeError(Expr.AttributeReference a, Objecttype subject, String naam) {
        Msg onderwerp = a.subjectSource() == null || a.subjectSource().isBlank() ? Msg.frontend("RGS-124.own-subject")
                : Msg.frontend("RGS-124.written-source", "source", a.subjectSource());
        return new Typing.Coded("RGS-124",
                params("expression", a.source(), "subjectPhrase", onderwerp, "subject", subject.naam, "attribute", naam));
    }

    /** A member read on {@code fillerType} that neither it nor a supertype declares: where it does stand, and the
     *  verifier's sentence for the navigation to the one subtype that declares it (WF-053), else for the read on the
     *  filler (WF-034). */
    private Typing.Coded memberMissingError(Expr.AttributeReference a, String member, Objecttype fillerType, String roleName) {
        List<String> subTypes = new ArrayList<>(), otherTypes = new ArrayList<>();
        for (Objecttype o : model.objecttypen.values())
            if (o != fillerType && declaresMember(o, member)) (o.isA(fillerType) ? subTypes : otherTypes).add(o.naam);
        Msg where = !subTypes.isEmpty()
                ? Msg.frontend(subTypes.size() == 1 ? "RGS-125.subtype" : "RGS-125.subtypes", "types", enumeration(subTypes))
                : otherTypes.size() == 1 ? Msg.frontend("RGS-125.other-type", "types", enumeration(otherTypes), "fillerType", fillerType.naam)
                : !otherTypes.isEmpty() ? Msg.frontend("RGS-125.other-types", "types", enumeration(otherTypes), "fillerType", fillerType.naam)
                : Msg.frontend("RGS-125.nowhere");
        Msg sentence = subTypes.size() == 1
                ? Msg.frontend("RGS-125.reaches", "fillerType", fillerType.naam, "endpoint", subTypes.get(0))
                : Msg.frontend("RGS-125.neither-member", "member", member, "fillerType", fillerType.naam);
        return new Typing.Coded("RGS-125",
                params("expression", a.source(), "member", member, "fillerType", fillerType.naam, "roleName", roleName,
                        "where", where, "sentence", sentence));
    }

    private Typing.Coded characteristicAsValueError(Expr.AttributeReference a, String member, Objecttype fillerType, String roleName) {
        return new Typing.Coded("RGS-126",
                params("expression", a.source(), "member", member, "fillerType", fillerType.naam, "roleName", roleName));
    }

    /** Whether {@code o} itself — not a supertype — declares a member {@code lid}, case-insensitively. */
    private static boolean declaresMember(Objecttype o, String member) {
        return o.attributen.keySet().stream().anyMatch(n -> n.equalsIgnoreCase(member))
                || o.kenmerken.stream().anyMatch(n -> n.equalsIgnoreCase(member));
    }

    /** The refusal of a fold the wire has no spelling for: what the fold carries, then the reader's sentence for the
     *  document the emitter would write — the verifier's WF-053 sentence for a step written on the source, else the
     *  one for the fold without a range ({@link #withoutRange}). */
    private Typing.Coded foldError(Expr.FunctionCall f, Objecttype subject, dev.uchar.regelspraak.emit.IrEmitter.FoldRefusal w) {
        Expr.AttributeReference source = (Expr.AttributeReference) f.arguments().get(0);
        return switch (w) {
            case WRITTEN_PATH, WRITTEN_STEP -> new Typing.Coded("RGS-127",
                    params("expression", f.source()));
            case FOREIGN_ATTRIBUTE -> new Typing.Coded("RGS-128",
                    params("expression", f.source(), "attribute", source.attribute(), "objectType", source.objectType().naam,
                            "range", withoutRange(f, subject, source)));
            case OWN_ATTRIBUTE -> new Typing.Coded("RGS-129",
                    params("expression", f.source(), "range", withoutRange(f, subject, source)));
            case NO_SUBJECT -> new Typing.Coded("RGS-130",
                    params("expression", f.source(), "range", withoutRange(f, subject, source)));
            case NO_RELATION -> new Typing.Coded("RGS-131",
                    params("expression", f.source(), "anchor", writtenAnchor(source, subject) != null ? writtenAnchor(source, subject) : "",
                            "reason", writtenAnchor(source, subject) != null ? ""
                                    : pairReason(subject, f.elementObjectType(),
                                            source.type() instanceof Datatype.Unknown ? source.attribute() : source.subjectSource()),
                            "cause", writtenAnchor(source, subject) != null
                                    ? Msg.frontend("RGS-131.written-anchor", "source", source.subjectSource(), "anchor", writtenAnchor(source, subject))
                                    : pairReason(subject, f.elementObjectType(),
                                            source.type() instanceof Datatype.Unknown ? source.attribute() : source.subjectSource()),
                            "range", withoutRange(f, subject, source)));
        };
    }

    /** The anchor a fold source writes behind its element noun where it is another object than the subject
     *  ("zijn thuishaven" in "alle liggers van zijn thuishaven"), or null where the source writes none or names the
     *  subject's own type ("alle Schuiten van de Haven" on a Haven). Such a range is two steps from the subject, and
     *  the derivation by the one relation from the subject to the element type says nothing about it. */
    private static String writtenAnchor(Expr.AttributeReference source, Objecttype subject) {
        String o = source.subjectSource() == null ? "" : source.subjectSource().trim();
        int van = o.toLowerCase(java.util.Locale.ROOT).indexOf(" van ");
        if (van < 0) return null;
        String anchor = o.substring(van + 5).trim();
        return subject != null && stripArticle(anchor).equalsIgnoreCase(subject.naam) ? null : anchor;
    }

    /** The reader's sentence for a fold written without a range: the loader's under every kind but total; under a
     *  total WF-083's, for an element type or a source type that is not the subject's. A total over the subject's
     *  own attribute without a range is the subject's own total, which no reader refuses, and the refusal keeps
     *  this frontend's words. */
    private static Msg withoutRange(Expr.FunctionCall f, Objecttype subject, Expr.AttributeReference source) {
        if (f.kind() != Expr.FunctionKind.TOTAAL)
            return Msg.frontend("RGS-128.range-written");
        if (subject != null && f.elementObjectType() != subject)
            return Msg.frontend("RGS-128.range-element", "elementType", f.elementObjectType().naam, "subject", subject.naam);
        if (subject != null && source.objectType() != null && source.objectType() != subject)
            return Msg.frontend("RGS-128.range-anchor", "sourceType", source.objectType().naam, "subject", subject.naam,
                    "attribute", source.attribute(), "hint", Msg.frontend("RGS-128.reads-own"));
        return Msg.frontend("RGS-128.range-own");
    }

    /** Why the type pair gave no relation: none joins the two types, the one that does is reciprocal (the
     *  two-role binding does not take it), or several do and the written name names none of their roles alone. */
    private Msg pairReason(Objecttype subject, Objecttype targetName, String written) {
        int n = model.connectingRoleCount(subject, targetName);
        if (n == 0) return Msg.frontend("RGS-118.pair-none", "subject", subject.naam, "target", targetName.naam);
        if (n == 1) return Msg.frontend("RGS-118.pair-reciprocal", "subject", subject.naam, "target", targetName.naam);
        return Msg.frontend("RGS-118.pair-many", "count", n, "subject", subject.naam, "target", targetName.naam,
                "written", written);
    }

    private void windowError(Condition.Quantifier k, int threshold, Source source, String place) {
        String words = switch (k) {
            case GEEN -> "geen van de";
            case TEN_MINSTE -> "ten minste " + threshold;
            case TEN_HOOGSTE -> "ten hoogste " + threshold;
            case PRECIES -> "precies " + threshold;
            default -> k.name().toLowerCase();
        };
        model.diagnostics.add(new Diagnostic(Severity.ERROR, "RGS-132", params("words", words, "place", place), source == null ? 0 : source.line()));
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
        final Block category;
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
        boolean error;
        ResolvedRule regel;
        final List<Node> children = new ArrayList<>();

        Node(Block category, ParserRuleContext block, String naam, String carrier, String head, int line) {
            this.category = category; this.block = block; this.naam = naam; this.carrier = carrier; this.head = head;
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
    private void treeDefaults(RegelSpraakDocumentContext doc, java.util.Set<ParserRuleContext> refused) {
        List<Node> nodes = new ArrayList<>();
        java.util.Set<String> separateMembers = new java.util.HashSet<>();
        for (ParseTree kind : doc.children == null ? List.<ParseTree>of() : doc.children) {
            if (kind instanceof UitzonderingRegelContext u)
                nodes.add(new Node(u.AFWIJKING_VAN() != null ? Block.DEVIATION : Block.EXCEPTION, u,
                        src(u.regelNaam()), src(u.uitzonderingBasisNaam()), blockHead(u), u.getStart().getLine()));
            else if (kind instanceof UitzonderingGroepContext g && g.OP() != null) {
                if (!refused.contains(g)) headAnnotation(g, blockHead(g), "dialect §4", g.getStart().getLine());
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
            Typing.Coded reason = separateMembers.contains(k.carrier) ? looseMemberAsCarrier(k)
                    : resolvedOf(k.carrier).isEmpty() ? unknownCarrier(k) : null;
            if (reason == null) { k.root = k.carrier; continue; }
            k.error = true;
            if (!refused.contains(k.block) && reported.add(k.block)) error(k.line, reason);
        }

        // 2. the base rule of each node, found by walking the carriers upward; a walk that comes back to a node
        //    it passed is a cycle, which has no base rule and is refused once, with its path
        for (Node k : nodes) {
            if (k.error || k.root != null) continue;
            List<Node> bindingPath = new ArrayList<>();
            Node h = k;
            while (!h.error && h.root == null && !bindingPath.contains(h)) { bindingPath.add(h); h = h.parent; }
            if (bindingPath.contains(h)) {
                List<Node> cycle = bindingPath.subList(bindingPath.indexOf(h), bindingPath.size());
                Node first = cycle.stream().min(java.util.Comparator.comparingInt(nodes::indexOf)).orElseThrow();
                if (cycle.stream().noneMatch(c -> refused.contains(c.block))) {
                    List<String> names = new ArrayList<>();
                    Node c = first;
                    do { names.add("'" + c.naam + "'"); c = c.parent; } while (c != first);
                    names.add("'" + first.naam + "'");
                    error(first.line, cycle(first, String.join(" -> ", names)));
                }
            }
            for (Node p : bindingPath) {
                if (h.error || h.root == null) p.error = true;
                else p.root = h.root;
            }
        }

        // 3. per tree, in the order its first block was written: the base, then every node top-down
        java.util.Set<String> nodeNames = nodeNames(doc);
        Map<String, String[]> forms = formSites(doc);
        Map<String, List<Node>> perRoot = new LinkedHashMap<>();
        for (Node k : nodes) if (!k.error) perRoot.computeIfAbsent(k.root, w -> new ArrayList<>()).add(k);
        java.util.Set<ResolvedRule> gelift = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (var e : perRoot.entrySet()) {
            String root = e.getKey();
            List<Node> tree = e.getValue();
            if (tree.stream().anyMatch(k -> refused.contains(k.block))) continue;   // refused at a head already
            List<Node> rootChildren = new ArrayList<>();
            for (Node k : tree) (k.parent == null ? rootChildren : k.parent.children).add(k);
            Node head = rootChildren.get(0);
            List<ResolvedRule> base = resolvedOf(root);
            if (base.size() > 1) { multiVersionError("basisregel", root, head.line); continue; }
            ResolvedRule baseRule = base.get(0);
            if (refusedTargets.contains(baseRule)) continue;   // its target is refused already, and by name
            DefaultsTarget targetName = targetOf(baseRule);
            if (targetName == null || baseRule.category() == ResolvedRule.Kind.INITIALISATIE) {
                error(head.line,
                        "RGS-133", params("head", head.head));
                continue;
            }
            if (!checkChildren(null, root, rootChildren, baseRule, targetName, nodeNames)) continue;

            // the blocks for the base's own type, and per subtype the blocks written for it, in written order
            List<Node> ownChildren = new ArrayList<>();
            Map<Objecttype, List<Node>> perSubtype = new LinkedHashMap<>();
            for (Node k : rootChildren) {
                Objecttype o = targetOf(k.regel).onderwerp();
                if (o == targetName.onderwerp()) ownChildren.add(k);
                else perSubtype.computeIfAbsent(o, x -> new ArrayList<>()).add(k);
            }
            if (!perSubtype.isEmpty()) {
                Refusal reason = orderError(root, rootChildren);
                if (reason == null) reason = subtypeNameError(root, perSubtype, forms);
                if (reason != null) { error(reason.line(), reason.refusal()); continue; }
            }

            List<Condition> baseConjuncts = conjuncts(baseRule.condition());
            List<Default> chain = new ArrayList<>();
            if (!ownChildren.isEmpty() && ownChildren.get(0).category == Block.MEMBER)   // one unordered block: a group-default directly above the base
                chain.add(new Default(null, null, null, false,
                        exceptionsOf(ownChildren, baseConjuncts, targetName.characteristic()),
                        headText(ownChildren.get(0).block)));
            else
                for (Node k : ownChildren) chain.add(0, defaultOf(k, baseConjuncts, targetName.characteristic()));
            chain.add(defaultOf(baseRule, targetName.characteristic()));
            PrioritizedDefaults group = defaultsOf(root, targetName, false, chain, baseRule.validity(),
                    annotationText(baseRule.statuteAnnotations()));
            model.prioritizedDefaults.add(group);
            pdOrigin.put(group, new Origin(head.line, head.head));
            for (var s : perSubtype.entrySet()) {
                PrioritizedDefaults own = subtypeGroup(root, baseRule, targetName, s.getKey(), s.getValue());
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
    private boolean checkChildren(Node carrier, String carrierName, List<Node> children, ResolvedRule base,
                                 DefaultsTarget targetName, java.util.Set<String> nodeNames) {
        boolean goed = true;
        // a subtype's own blocks under the base stand above the supertype's in the composition of ch. 7.10, so
        // only children for the carrier's own type can leave their priority open
        List<Node> own = children.stream().filter(k -> subjectOf(k) == null
                || subjectOf(k) == targetName.onderwerp()).toList();
        List<Node> ordered = own.stream().filter(k -> k.category != Block.MEMBER).toList();
        List<ParserRuleContext> blocks = own.stream().filter(k -> k.category == Block.MEMBER).map(k -> k.block)
                .distinct().toList();
        java.util.Set<ParserRuleContext> refusedBlocks =
                java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        if (!blocks.isEmpty() && (!ordered.isEmpty() || blocks.size() > 1)) {
            ParserRuleContext block = ordered.isEmpty() ? blocks.get(1) : blocks.get(0);
            refusedBlocks.add(block);
            error(block.getStart().getLine(),
                    "RGS-134", params("head", blockHead(block), "carrier", carrierName, "siblings", ordered.isEmpty()
                            ? Msg.frontend("RGS-134.other-block")
                            : Msg.frontend("RGS-134.ordered", "names", enumeration(ordered.stream().map(k -> k.naam).toList()))));
            goed = false;
        }
        for (Node k : children) {
            if (k.category == Block.MEMBER && refusedBlocks.contains(k.block)) { goed = false; continue; }
            if (!checkNode(k, carrier, carrierName, base, targetName, nodeNames)) {
                if (k.category == Block.MEMBER) refusedBlocks.add(k.block);
                goed = false;
                continue;
            }
            // below a subtype's own block the carrier's target is the subtype's
            if (!checkChildren(k, k.naam, k.children, base, targetOf(k.regel), nodeNames)) goed = false;
        }
        return goed;
    }

    /** A refusal at a block: its line and its sentence. */
    private record Refusal(int line, Typing.Coded refusal) {}

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
                if (sk == sl || !sk.isA(sl)) continue;
                return new Refusal(k.line, new Typing.Coded("RGS-310",
                        params("head", k.head, "designation", designation(k), "subtype", sk.naam,
                                "laterDesignation", designation(later), "supertype", sl.naam, "root", root,
                                "name", k.naam, "laterName", later.naam)));
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
                                      Map<String, String[]> forms) {
        for (var s : perSubtype.entrySet()) {
            String naam = subtypeGroupName(root, s.getKey());
            String[] site = forms.get(dev.uchar.regelspraak.emit.IrEmitter.norm(naam));
            if (site == null) continue;
            Node k = s.getValue().get(0);
            return new Refusal(k.line, new Typing.Coded("RGS-311",
                    params("head", k.head, "subtype", s.getKey().naam, "root", root, "name", naam, "form", site[0],
                            "sourceLine", site[1])));
        }
        return null;
    }

    /** The group of a subtype's own blocks under the base of its supertype (RUNTIME-SEMANTICS ch. 7.9 and 7.10): on
     *  the subtype, one group-default whose one ordered exception group holds the blocks, the later-written one
     *  first. A group-default is no base, so the composition keeps the inherited base below the subtype's own
     *  exceptions and the inherited ones; a block as an ordinary default would be the subtype's own base and replace
     *  the inherited one. A restrictive block conjoins the base's condition, read on the subtype. The group carries
     *  the base's window, as the blocks do, and its source says where the base comes from. */
    private PrioritizedDefaults subtypeGroup(String root, ResolvedRule base, DefaultsTarget targetName, Objecttype sub,
                                             List<Node> children) {
        List<Condition> conjuncts = conjuncts(opSubtype(base.condition(), targetName.onderwerp(), sub));
        List<Default> chain = new ArrayList<>();
        for (Node k : children) chain.add(0, defaultOf(k, conjuncts, targetName.characteristic()));   // later-written stacks higher
        List<String> heads = children.stream().map(k -> headText(k.block)).distinct().toList();
        String head = String.join(" en ", heads);
        Default groupDefault = new Default(null, null, null, false,
                new Default.Exceptions(false, List.of(new ExceptionGroup(chain, head))), head);
        return defaultsOf(subtypeGroupName(root, sub), new DefaultsTarget(sub, targetName.characteristic(), targetName.naam()), false,
                List.of(groupDefault), base.validity(), "eigen uitzonderingen van " + sub.naam + "; de basis '"
                        + root + "' is geërfd van " + targetName.onderwerp().naam);
    }

    /** {@code c} read on the subtype {@code sub} instead of on {@code sup}: every own read of {@code sup} names
     *  {@code sub}, as the same text written on {@code sub} resolves (IR-SPEC ch. 4.1a: an own read names the subject
     *  type, also for an inherited attribute). A restrictive block of a subtype repeats the conjuncts of its base, and
     *  the base stands on the supertype. Navigated reads, role checks and characteristic checks name no subject type,
     *  and the emitter derives their walk from the group's subject; what a fold or a quantifier reads per element is
     *  the element's. A node in which nothing changes is returned itself, so a conjunct that reads no own attribute
     *  stays the node the base carries. */
    private static Condition opSubtype(Condition c, Objecttype sup, Objecttype sub) {
        if (c == null) return null;
        return switch (c) {
            case Condition.Comparison v -> {
                Expr l = opSubtype(v.left(), sup, sub), r = opSubtype(v.right(), sup, sub);
                yield l == v.left() && r == v.right() ? v
                        : new Condition.Comparison(v.op(), l, r, v.periode(), v.source());
            }
            case Condition.Predicate p -> {
                Expr o = opSubtype(p.operand(), sup, sub);
                yield o == p.operand() ? p
                        : new Condition.Predicate(p.category(), o, p.dayKind(), p.negation(), p.digits(), p.periode(), p.source());
            }
            case Condition.TimeWindow p -> {
                Expr van = opSubtype(p.from(), sup, sub), until = opSubtype(p.until(), sup, sub);
                yield van == p.from() && until == p.until() ? p : new Condition.TimeWindow(van, until, p.toInclusive(), p.source());
            }
            case Condition.Compound s -> {
                List<Condition> parts = s.components().stream().map(o -> opSubtype(o, sup, sub)).toList();
                boolean same = true;
                for (int i = 0; i < parts.size(); i++) same &= parts.get(i) == s.components().get(i);
                yield same ? s : new Condition.Compound(s.quantifier(), s.threshold(), parts, s.source());
            }
            case Condition.CharacteristicCheck k -> k.owner() != sup ? k
                    : new Condition.CharacteristicCheck(k.characteristic(), k.navigation(), k.negation(), sub, k.periode(), k.source());
            // the filter and the test read each element
            case Condition.QuantifierFold k -> k;
            case Condition.RoleMembership r -> r;
            case Condition.FarRoleMembership r -> r;
            case Condition.RuleStatusCheck r -> r;
            case Condition.Unresolved o -> o;
        };
    }

    private static Expr opSubtype(Expr e, Objecttype sup, Objecttype sub) {
        if (e == null) return null;
        return switch (e) {
            case Expr.AttributeReference a -> !ownReadOf(a, sup) ? a
                    : new Expr.AttributeReference(a.attribute(), sub, a.subjectSource(), a.navigationRole(), a.type(), a.source(),
                            a.set(), a.navigationPath(), a.rangePath(), a.quantifier(), a.threshold());
            case Expr.Binary b -> {
                Expr l = opSubtype(b.left(), sup, sub), r = opSubtype(b.right(), sup, sub);
                yield l == b.left() && r == b.right() ? b : new Expr.Binary(b.op(), l, r, b.type(), b.source());
            }
            case Expr.FunctionCall f -> {
                // a fold's source is read through its range: only a source that is the subject's own read (a
                // dimension fold, the subject's own total) is the subject's; the filter of a dimension fold reads the
                // subject's labels, every other filter reads each element
                boolean fold = dev.uchar.regelspraak.emit.IrEmitter.isFoldSource(f);
                List<Expr> args = new ArrayList<>();
                for (int i = 0; i < f.arguments().size(); i++) {
                    Expr x = f.arguments().get(i);
                    args.add(fold && i == 0 && !(x instanceof Expr.AttributeReference a && ownReadOf(a, sup)) ? x
                            : opSubtype(x, sup, sub));
                }
                Expr empty = opSubtype(f.emptyFallback(), sup, sub);
                Condition filter = f.dimensieSelectie() != null ? opSubtype(f.filter(), sup, sub) : f.filter();
                boolean ownSource = fold && !args.isEmpty() && args.get(0) != f.arguments().get(0);
                Objecttype element = f.elementObjectType() == sup && (f.dimensieSelectie() != null || ownSource) ? sub
                        : f.elementObjectType();
                boolean same = empty == f.emptyFallback() && filter == f.filter() && element == f.elementObjectType();
                for (int i = 0; i < args.size(); i++) same &= args.get(i) == f.arguments().get(i);
                yield same ? f : new Expr.FunctionCall(f.kind(), args, element, empty, filter, f.dimensieSelectie(),
                        f.type(), f.source(), f.granulariteit());
            }
            case Expr.Rounding a -> {
                Expr b = opSubtype(a.base(), sup, sub);
                yield b == a.base() ? a : new Expr.Rounding(b, a.mode(), a.decimalen(), a.source());
            }
            case Expr.Clamp b -> {
                Expr x = opSubtype(b.base(), sup, sub), min = opSubtype(b.minimum(), sup, sub),
                        max = opSubtype(b.maximum(), sup, sub);
                yield x == b.base() && min == b.minimum() && max == b.maximum() ? b
                        : new Expr.Clamp(x, min, max, b.source());
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
                yield same ? t : new Expr.TextComposition(parts, t.source());
            }
            case Expr.DimensionCellRead c -> {
                Expr a = opSubtype(c.attribute(), sup, sub);
                yield a == c.attribute() ? c : new Expr.DimensionCellRead((Expr.AttributeReference) a, c.labels(), c.source());
            }
            case Expr.Let l -> {
                Expr v = opSubtype(l.value(), sup, sub), b = opSubtype(l.body(), sup, sub);
                yield v == l.value() && b == l.body() ? l : new Expr.Let(l.binder(), v, b, l.type(), l.source());
            }
            case Expr.IfThenElse k -> {
                Condition c = opSubtype(k.condition(), sup, sub);
                Expr then = opSubtype(k.then(), sup, sub), otherwise = opSubtype(k.otherwise(), sup, sub);
                yield c == k.condition() && then == k.then() && otherwise == k.otherwise() ? k
                        : new Expr.IfThenElse(c, then, otherwise, k.type(), k.source());
            }
            // each selection reads its elements
            case Expr.InstanceSetUnion u -> u;
            case Expr.Literal l -> l;
            case Expr.ParameterRef p -> p;
            case Expr.VariableRef v -> v;
            case Expr.BinderRef b -> b;
            case Expr.Constant k -> k;
            case Expr.Unresolved o -> o;
        };
    }

    /** Whether {@code a} is an own read of {@code type}: no written role or chain, no set, and that type. */
    private static boolean ownReadOf(Expr.AttributeReference a, Objecttype type) {
        return a.objectType() == type && a.navigationRole() == null && !a.set()
                && (a.navigationPath() == null || a.navigationPath().isEmpty());
    }

    /** The subject of the one rule a node's body resolves to, or null where it resolves to none or to several (the
     *  node's own check refuses those). */
    private Objecttype subjectOf(Node k) {
        List<ResolvedRule> r = resolvedOf(k.naam);
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
    private boolean checkNode(Node k, Node carrier, String carrierName, ResolvedRule base, DefaultsTarget targetName,
                              java.util.Set<String> nodeNames) {
        List<ResolvedRule> r = resolvedOf(k.naam);
        if (r.isEmpty()) {
            error(k.line,
                    "RGS-135", params("head", k.head, "node", withArticle(k)));
            return false;
        }
        if (r.size() > 1) { multiVersionError(what(k), k.naam, k.line); return false; }
        ResolvedRule regel = r.get(0);
        if (refusedTargets.contains(regel)) return false;   // refused already, by name
        DefaultsTarget own = targetOf(regel);
        if (own == null || regel.category() == ResolvedRule.Kind.INITIALISATIE) {
            error(k.line,
                    "RGS-136", params("head", k.head, "node", withArticle(k)));
            return false;
        }
        if (k.category == Block.DEVIATION && carrier != null) {
            error(k.line,
                    "RGS-137", params("head", k.head, "carrier", carrier.naam, "carrierKind", kindText(carrier), "member", k.naam, "root", k.root));
            return false;
        }
        boolean subtype = own.characteristic() == targetName.characteristic() && own.naam().equalsIgnoreCase(targetName.naam())
                && own.onderwerp() != targetName.onderwerp() && own.onderwerp().isA(targetName.onderwerp());
        Objecttype rootType = targetOf(base).onderwerp();
        if (subtype && carrier != null) {
            String sub = own.onderwerp().naam, sup = targetName.onderwerp().naam;
            // The subtype's own exception hangs under the base it inherits, which is this tree's unless a type on
            // its line declares one of its own (ch. 7.10: an own base replaces the inherited one).
            Base b = ownBaseOnLine(own, rootType, nodeNames);
            String naam = b == null ? k.root : b.regel();
            String under = b == null ? "de basisregel '" + k.root + "'"
                    : b.type() == own.onderwerp() ? "zijn eigen basisregel '" + b.regel() + "'"
                    : "de basisregel '" + b.regel() + "' van " + b.type().naam + ", die " + sub + " erft";
            error(k.line,
                    "RGS-138", params("head", k.head, "carrier", carrier.naam, "carrierKind", kindText(carrier), "supertype", sup, "designation", designation(k), "subtype", sub, "under", under, "name", naam, "member", k.naam));
            return false;
        }
        if (subtype) {
            Typing.Coded reason = subtypeBlockError(k, own, rootType, nodeNames);
            if (reason != null) { error(k.line, reason); return false; }
        } else if (k.category == Block.MEMBER && own.onderwerp() != rootType && own.equal(targetName)) {
            // a member under a subtype's own block: the subtype's group would carry an unordered block
            error(k.line, subtypeMemberError(k, own.onderwerp(), rootType));
            return false;
        }
        // Default carries no per-default geldigheid: mixed windows are unrepresentable on the defaults group, so a
        // diverging window must refuse — the same fence the multi-version case gets.
        if (!regel.validity().equals(base.validity())) {
            error(k.line,
                    "RGS-139", params("head", k.head, "designation", designation(k)));
            return false;
        }
        if (!subtype && !targetName.equal(own)) {
            error(k.line,
                    "RGS-140", params("head", k.head, "designation", designation(k), "ownTarget", targetText(own), "carrierPart", carrier == null ? Msg.frontend("RGS-140.base-rule")
                            : Msg.frontend(k.category == Block.MEMBER ? "RGS-140.his-carrier" : "RGS-140.her-carrier", "carrier", carrierName),
                            "baseTarget", targetText(targetName), "what", what(k)));
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
    private Base ownBaseOnLine(DefaultsTarget own, Objecttype rootType, java.util.Set<String> nodeNames) {
        for (Objecttype t = own.onderwerp(); t != null && t != rootType; t = t.supertype) {
            DefaultsTarget opT = new DefaultsTarget(t, own.characteristic(), own.naam());
            for (ResolvedRule b : model.resolvedRegels)
                if (b.category() != ResolvedRule.Kind.INITIALISATIE && !refusedTargets.contains(b)
                        && !nodeNames.contains(withoutVersion(b.naam())) && opT.equal(targetOf(b)))
                    return new Base(withoutVersion(b.naam()), t);
        }
        return null;
    }

    /** Why a block for a subtype directly under the base of its supertype does not lower, or null where it does. It
     *  lowers as the subtype's own exception without an own base; where the subtype has a base on its line, that base
     *  replaces the tree's for it, and the block belongs under that one. A member of an unordered block does not
     *  lower ({@link #subtypeMemberError}). */
    private Typing.Coded subtypeBlockError(Node k, DefaultsTarget own, Objecttype rootType,
                                   java.util.Set<String> nodeNames) {
        String sub = own.onderwerp().naam;
        Base b = ownBaseOnLine(own, rootType, nodeNames);
        if (b != null) {
            String has = b.type() == own.onderwerp()
                    ? sub + " heeft een eigen basisregel '" + b.regel() + "' voor '" + own.naam() + "'"
                    : sub + " erft van " + b.type().naam + " de basisregel '" + b.regel() + "' voor '" + own.naam()
                      + "'";
            String form = switch (k.category) {
                case DEVIATION -> "'Afwijking van " + b.regel() + ": " + k.naam + "'";
                case EXCEPTION -> "'Uitzondering op " + b.regel() + ": " + k.naam + "' als zij de voorwaarde van '"
                        + b.regel() + "' erft, of 'Afwijking van " + b.regel() + ": " + k.naam
                        + "' met de volledige voorwaarde";
                case MEMBER -> "het blok als 'Uitzonderingen ongeordend op " + b.regel() + ":'";
            };
            return new Typing.Coded("RGS-141",
                    params("head", k.head, "designation", designation(k), "subtype", sub, "has", has, "root", k.root,
                            "form", form));
        }
        return k.category == Block.MEMBER ? subtypeMemberError(k, own.onderwerp(), rootType) : null;
    }

    /** A member of an 'Uitzonderingen ongeordend op' block among the own exceptions of a subtype under the base of its
     *  supertype. Not lowered: the subtype's group would hold the block, and a conflict between its members would be
     *  keyed on the path of the composed stack (RUNTIME-SEMANTICS ch. 7.9 and 7.10). */
    private static Typing.Coded subtypeMemberError(Node k, Objecttype sub, Objecttype rootType) {
        return new Typing.Coded("RGS-142",
                params("head", k.head, "member", k.naam, "subtype", sub.naam, "root", k.root, "rootType", rootType.naam));
    }

    /** The default a node becomes. An 'Uitzondering op' block and a member of an 'Uitzonderingen ongeordend op'
     *  block are restrictive: the justification is one flat 'all' of the carrier's conjuncts followed by the node's
     *  own, since ch. 7.9 decides a default's exception groups before and regardless of its own justification, and
     *  ch. 7.1 asks the frontend for self-contained justifications. Where the carrier contributes no conjunct (an
     *  unconditional base) the node keeps the condition it was written with. An 'Afwijking van' block carries its
     *  own condition only. */
    private Default defaultOf(Node k, List<Condition> carrierConjuncts, boolean characteristic) {
        Condition own = k.regel.condition();
        Condition justification = k.category == Block.DEVIATION || carrierConjuncts.isEmpty()
                ? (own != null ? own : ALTIJD)
                : conjunction(carrierConjuncts, own);
        return defaultOf(k.regel, characteristic, justification,
                exceptionsOf(k.children, conjuncts(justification), characteristic));
    }

    /** The exception groups of a carrier: one ordered group holding its ordered children, the later-written one
     *  first; or one group per member of its 'Uitzonderingen ongeordend op' block, unordered. Null without
     *  children. A head makes an exception group, not a rule, so the group's source is the head's text
     *  ({@link #headText}) and each block's annotation stands on the default the block becomes. */
    private Default.Exceptions exceptionsOf(List<Node> children, List<Condition> carrierConjuncts, boolean characteristic) {
        if (children.isEmpty()) return null;
        if (children.get(0).category == Block.MEMBER) {
            List<ExceptionGroup> members = new ArrayList<>();
            for (Node member : children)
                members.add(new ExceptionGroup(List.of(defaultOf(member, carrierConjuncts, characteristic)),
                        headText(member.block)));
            return new Default.Exceptions(true, members);
        }
        List<Default> chain = new ArrayList<>();
        for (Node k : children) chain.add(0, defaultOf(k, carrierConjuncts, characteristic));   // later-written stacks higher
        return new Default.Exceptions(false, List.of(new ExceptionGroup(chain, headText(children.get(0).block))));
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
    private static Condition conjunction(List<Condition> carrierConjuncts, Condition own) {
        List<Condition> parts = new ArrayList<>(carrierConjuncts);
        flatten(own, parts);
        StringBuilder sentence = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            String part = parts.get(i).source() == null ? "" : parts.get(i).source().snippet();
            if (i > 0) sentence.append(" en ");
            sentence.append(i < parts.size() - 1 && part.endsWith(".") ? part.substring(0, part.length() - 1) : part);
        }
        String tekst = sentence.toString();
        Source site = own == null ? null : own.source();
        return new Condition.Compound(Condition.Quantifier.ALLE, 0, List.copyOf(parts),
                site == null ? Source.synthetic(tekst) : new Source(site.file(), site.line(), site.column(), tekst));
    }

    /** The conjuncts of a justification: the parts of an 'all', flattened at any depth; another quantifier stays
     *  one conjunct, and no justification (or the empty 'all') has none. */
    private static List<Condition> conjuncts(Condition c) {
        List<Condition> result = new ArrayList<>();
        flatten(c, result);
        return result;
    }

    private static void flatten(Condition c, List<Condition> result) {
        if (c == null) return;
        if (c instanceof Condition.Compound s && s.quantifier() == Condition.Quantifier.ALLE) {
            for (Condition d : s.components()) flatten(d, result);
            return;
        }
        result.add(c);
    }

    private static String what(Node k) {
        return switch (k.category) { case EXCEPTION -> "uitzondering"; case DEVIATION -> "afwijking"; case MEMBER -> "groepslid"; };
    }

    private static String designation(Node k) { return what(k) + " '" + k.naam + "'"; }

    /** The node as the first card's texts name it: "de uitzondering 'E'", "groepslid 'M'". */
    private static String withArticle(Node k) { return (k.category == Block.MEMBER ? "" : "de ") + designation(k); }

    private static String kindText(Node k) {
        return switch (k.category) {
            case EXCEPTION -> "een uitzondering";
            case DEVIATION -> "een afwijking";
            case MEMBER -> "een lid van een 'Uitzonderingen ongeordend op'-blok";
        };
    }

    private Typing.Coded unknownCarrier(Node k) {
        if (k.category == Block.DEVIATION)
            return new Typing.Coded("RGS-312", params("head", k.head, "reason", baseReason(k.carrier)));
        return new Typing.Coded("RGS-313", params("head", k.head, "reason", baseReason(k.carrier)));
    }

    private static Typing.Coded looseMemberAsCarrier(Node k) {
        return new Typing.Coded("RGS-314", params("head", k.head, "carrier", k.carrier));
    }

    private static Typing.Coded cycle(Node k, String bindingPath) {
        return new Typing.Coded("RGS-315", params("head", k.head, "path", bindingPath));
    }

    private void error(int line, String code, java.util.Map<String, Object> params) {
        model.diagnostics.add(new Diagnostic(Severity.ERROR, code, params, line));
    }

    private void error(int line, Typing.Coded refusal) {
        model.diagnostics.add(refusal.at(Severity.ERROR, line));
    }

    /** The unordered group without a carrier: each 'Uitzonderingen ongeordend:' block becomes SIBLING
     *  declared-unordered defaults groups (one per member) over ONE shared target — the §9.5 surface. Every sibling
     *  carries {@code unordered} (WF-050: two-or-more defaults groups on one target require it). All members leave
     *  resolvedRegels. A block with 'op &lt;drager&gt;' hangs in a defaults tree ({@link #treeDefaults}). */
    private void groupDefaults(RegelSpraakDocumentContext doc, java.util.Set<ParserRuleContext> refused) {
        for (UitzonderingGroepContext g : DefinitionResolver.allExceptionGroups(doc)) {
            if (g.OP() != null) continue;           // a member of a defaults tree
            if (refused.contains(g)) continue;   // refused at its head already
            int line = g.getStart().getLine();
            headAnnotation(g, "Uitzonderingen ongeordend", "dialect §4", line);
            List<ResolvedRule> members = new ArrayList<>();
            DefaultsTarget shared = null;
            boolean error = false;
            for (RegelContext member : g.regel()) {
                String naam = DefinitionResolver.src(member.regelNaam());
                List<ResolvedRule> rr = resolvedOf(naam);
                if (rr.isEmpty()) {
                    model.diagnostics.add(new Diagnostic(Severity.ERROR, "RGS-143", params("member", naam), line));
                    error = true; break;
                }
                if (rr.size() > 1) { multiVersionError("groepslid", naam, line); error = true; break; }
                if (refusedTargets.contains(rr.get(0))) { error = true; break; }   // refused already, by name
                DefaultsTarget d = targetOf(rr.get(0));
                if (d == null || rr.get(0).category() == ResolvedRule.Kind.INITIALISATIE) {
                    model.diagnostics.add(new Diagnostic(Severity.ERROR, "RGS-144", params("member", naam), line));
                    error = true; break;
                }
                if (shared == null) shared = d;
                else if (!shared.equal(d)) {
                    model.diagnostics.add(new Diagnostic(Severity.ERROR,
                            "RGS-145", params("member", naam, "target", targetText(d), "sharedTarget", targetText(shared)), line));
                    error = true; break;
                }
                members.add(rr.get(0));
            }
            if (error || shared == null) continue;
            for (ResolvedRule r : members) {
                PrioritizedDefaults sister = defaultsOf(r.naam(), shared, true,
                        List.of(defaultOf(r, shared.characteristic())), r.validity(), annotationText(r.statuteAnnotations()));
                model.prioritizedDefaults.add(sister);
                pdOrigin.put(sister, new Origin(line, "Uitzonderingen ongeordend"));
                model.resolvedRegels.remove(r);
            }
        }
    }

    /** Assemble a {@link PrioritizedDefaults} group for target {@code doel}: kenmerk XOR attribuut per WF-050,
     *  the base/member's geldigheid faithfully, and as {@code bron} the statute annotation of the rule the group
     *  is named after — the base of an ordered family, the member itself for an unordered sibling. */
    private PrioritizedDefaults defaultsOf(String naam, DefaultsTarget targetName, boolean unordered, List<Default> defaults,
                                            Validity g, String source) {
        return new PrioritizedDefaults(naam, targetName.onderwerp().naam,
                targetName.characteristic() ? targetName.naam() : null, targetName.characteristic() ? null : targetName.naam(),
                unordered, defaults, source, g);
    }

    /** One default from a resolved flat rule: on characteristic defaults a kenmerktoekenning decides {@code true},
     *  and a negated one (dialect §4, form 1: {@link #NEGATED}) decides {@code false} — which writes nothing, so
     *  §9.2's monotone writes and ch. 5.3 hold; on attribute defaults the default's value is the rule's resolved
     *  value expression. The justification is the rule's condition.
     *  The rule's {@code // Bron:} annotation becomes the default's {@code bron}: lifting takes the rule out of
     *  resolvedRegels, and its {@code statuteAnnotations} with it, so this is the one place the citation can
     *  survive — and since every lifted rule becomes a default HERE, a nested exception inherits the placement
     *  by construction. */
    private Default defaultOf(ResolvedRule r, boolean characteristic) {
        return defaultOf(r, characteristic, r.condition() != null ? r.condition() : ALTIJD, null);
    }

    /** The same with a given justification and nested exception groups: the default a node of a defaults tree
     *  becomes. */
    private Default defaultOf(ResolvedRule r, boolean characteristic, Condition justification, Default.Exceptions exceptions) {
        String source = annotationText(r.statuteAnnotations());
        return characteristic
                ? new Default(!isNegated(r), justification, null, false, exceptions, source)
                : new Default(null, justification, r.value(), false, exceptions, source);
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
                "RGS-146", params("head", headName, "annotation", tekst, "reference", reference), line));
    }

    /** The vacuously true justification. WF-050 requires EVERY non-initialisation default to carry one,
     *  so an UNCONDITIONAL base rule — a §13.4.2 rule with no {@code indien}, which is the ch. 7.8 standing
     *  norm — lowers to an ALLE-group over no parts: "all of nothing holds" is true, and it is exactly how
     *  the hand-written prioritized-defaults anchors spell "altijd". As the lowest default of a group such a
     *  rule stands whenever nothing above it decides, which is what the flat rule meant on its own. */
    private static final Condition ALTIJD =
            new Condition.Compound(Condition.Quantifier.ALLE, 0, List.of(), Source.synthetic("altijd"));

    /** The resolved versions of the rule named {@code naam} — matched exactly or by the "&lt;naam&gt; #k" version
     *  suffix. Empty when no such rule resolved (it was a non-scalar construct, or the name is unknown). */
    private List<ResolvedRule> resolvedOf(String naam) {
        List<ResolvedRule> result = new ArrayList<>();
        for (ResolvedRule r : model.resolvedRegels)
            if (r.naam().equals(naam) || r.naam().startsWith(naam + " #")) result.add(r);
        return result;
    }

    /** The (subject, kenmerk-vs-attribuut, target-name) a lifted rule writes, or null when its target is not a
     *  plain attribute/characteristic reference (an objectcreatie/verdeling/consistentie never lands here). */
    private DefaultsTarget targetOf(ResolvedRule r) {
        if (!(r.target() instanceof Expr.AttributeReference d) || d.objectType() == null) return null;
        return new DefaultsTarget(d.objectType(), r.category() == ResolvedRule.Kind.KENMERKTOEKENNING, d.attribute());
    }

    private static String targetText(DefaultsTarget d) {
        return d == null ? "onbekend"
                : d.onderwerp().naam + "." + d.naam() + (d.characteristic() ? " (kenmerk)" : " (attribuut)");
    }

    /** A hint appended to the unknown-base ERROR when the name IS resolved, but as a construct no defaults
     *  group can be built over (a verdeling/consistentie/objectcreatie lives in its own model list, not
     *  resolvedRegels). */
    private String baseReason(String naam) {
        boolean otherConstruct = model.allocations.stream().anyMatch(v -> v.naam().equals(naam))
                || model.consistenties.stream().anyMatch(c -> c.naam().equals(naam))
                || model.objectCreations.stream().anyMatch(o -> o.naam().equals(naam))
                || model.uniquenesses.stream().anyMatch(u -> u.naam().equals(naam));
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
    private static Msg.Enumeration enumeration(List<String> names) {
        return new Msg.Enumeration(Component.FRONTEND, names.stream().map(n -> "'" + n + "'").toList());
    }

    private void multiVersionError(String roleName, String naam, int line) {
        model.diagnostics.add(new Diagnostic(Severity.ERROR, "RGS-147", params("role", roleName, "name", naam), line));
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
    private record Input(Objecttype type, String attribute, int line, AttribuutSpecificatieContext declaration) {
        String opgegeven() { return "opgegeven " + attribute; }
        String computed() { return "berekende " + attribute; }
        String deviating() { return "afwijkende opgave " + attribute; }
        String group() { return "opgave boven berekening " + attribute; }
        String regel() { return "afwijking opgave " + attribute; }
        String consistencyCheck() { return "controle opgave " + attribute; }
        String head() { return "attribuut '" + attribute + "' van " + type.naam; }
    }

    private static final String MARKING = "'(mag opgegeven worden)'";

    /** The markers that passed {@link #inputDeclarations}, in declaration order. */
    private final List<Input> inputs = new ArrayList<>();

    /** The markers {@link #inputLowering} lowered, in declaration order: refused and withdrawn ones stay out. */
    private final List<Input> lowered = new ArrayList<>();

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
            List<ObjectTypeMemberContext> members;
            if (d.objectTypeDefinition() != null) {
                typeName = d.objectTypeDefinition().objectTypeNaam();
                members = d.objectTypeDefinition().objectTypeMember();
            } else if (d.objectTypeExtensie() != null) {
                typeName = d.objectTypeExtensie().objectTypeNaam();
                members = d.objectTypeExtensie().objectTypeMember();
            } else continue;
            Objecttype t = model.objecttypen.get(src(typeName));
            if (t == null) continue;
            for (ObjectTypeMemberContext member : members) {
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
                error(member.getStart().getLine(),
                        "RGS-148", params("characteristic", characteristicDeclarationName(member.kenmerkSpecificatie()), "objectType", t.naam, "marking", MARKING));
                continue;
            }
            AttribuutSpecificatieContext a = member.attribuutSpecificatie();
            Input o = new Input(t, src(a.lowerNaam()), a.getStart().getLine(), a);
            Typing.Coded reason = inputDeclarationError(o, memberLines);
            if (reason != null) { error(o.line(), reason); continue; }
            Datatype dt = t.attributen.get(o.attribute());
            java.util.LinkedHashMap<String, Datatype> oud = new java.util.LinkedHashMap<>(t.attributen);
            t.attributen.clear();
            for (var e : oud.entrySet()) {
                t.attributen.put(e.getKey(), e.getValue());
                if (e.getKey().equals(o.attribute())) {
                    t.attributen.put(o.opgegeven(), dt);
                    t.attributen.put(o.computed(), dt);
                }
            }
            t.kenmerken.add(o.deviating());
            t.characteristicKinds.put(o.deviating(), Objecttype.CharacteristicKind.BEZITTELIJK);
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
    private Typing.Coded inputDeclarationError(Input o, Map<Objecttype, Map<String, Integer>> memberLines) {
        Objecttype t = o.type();
        String x = o.attribute();
        AttribuutSpecificatieContext a = o.declaration();
        Datatype dt = t.attributen.get(x);
        // Three families are measured in the three executors, each on a golden of its own: a Numeriek without a unit,
        // a Numeriek with one unit, and a Datum in dagen; another family waits for a witness of its own. One unit: a
        // rate ("€/jr") or a product of units is composite and stays refused. The opgave and the computation share
        // that unit without a conversion: 'opgegeven X' and 'berekende X' carry X's datatype, and a writer or an
        // input in another unit of the same system is refused by the typing of an assignment, as without the marker.
        // A unit clause after a date needs no sentence here: it is an ERROR where the datatype is read.
        boolean memberUnit = a.MET_EENHEID() != null;
        boolean number = dt instanceof Datatype.Numeriek n
                && (n.hasUnit() ? oneUnit(n.eenheid()) : !memberUnit);
        if (!number && !(dt instanceof Datatype.DatumInDagen)) {
            NumeriekDatatypeContext nd = a.datatype() == null ? null : a.datatype().numeriekDatatype();
            String named = dt instanceof Datatype.Numeriek n && n.hasUnit()
                    ? "Numeriek met eenheid " + (nd != null && nd.eenheidExpressie() != null
                            ? src(nd.eenheidExpressie()) : n.eenheid().rendering())
                    : dt instanceof Datatype.Numeriek && memberUnit ? "Numeriek met eenheid " + src(a.eenheidExpressie())
                    : dt == null ? "onbekend" : dt.naam();
            return new Typing.Coded("RGS-149",
                    params("head", o.head(), "attribute", x, "datatype", named));
        }
        if (t.attribuutGranulariteit.containsKey(x))
            return new Typing.Coded("RGS-150", params("head", o.head()));
        List<String> dimensies = t.attributeDimensions.get(x);
        if (dimensies != null && !dimensies.isEmpty())
            return new Typing.Coded("RGS-151",
                    params("head", o.head(), "attribute", x, "dimensions", String.join(" en ", dimensies)));
        // a subtype inherits the generated members, a supertype's members are inherited by T: either way one
        // instance would carry two declarations of one name (WF-105)
        for (String g : List.of(o.opgegeven(), o.computed(), o.deviating())) {
            String key = dev.uchar.regelspraak.emit.IrEmitter.norm(g);
            for (Objecttype u : model.objecttypen.values()) {
                if (u != t && !u.isA(t) && !t.isA(u)) continue;
                boolean attribute = u.attributen.keySet().stream()
                        .anyMatch(k -> dev.uchar.regelspraak.emit.IrEmitter.norm(k).equals(key));
                boolean characteristic = u.kenmerken.stream()
                        .anyMatch(k -> dev.uchar.regelspraak.emit.IrEmitter.norm(k).equals(key));
                if (!attribute && !characteristic) continue;
                int regel = memberLines.getOrDefault(u, Map.of()).getOrDefault(key, 0);
                return new Typing.Coded("RGS-316",
                        params("head", o.head(), "line", o.line(), "generatedKind", Msg.frontend(g.equals(o.deviating()) ? "PART.member-kind.characteristic-kind" : "PART.member-kind.attribute"),
                                "generated", g, "declaringType", u.naam, "declaredKind", Msg.frontend(attribute ? "PART.member-kind.attribute" : "PART.member-kind.characteristic-kind"),
                                "declaredLine", regel, "objectType", t.naam));
            }
        }
        return null;
    }

    /** One unit: no denominator (a rate such as "€/jr") and no product of units ("€·m"). */
    private static boolean oneUnit(dev.uchar.regelspraak.model.Unit e) {
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
        Map<String, String[]> forms = formSites(doc);
        Map<String, Input> generated = new java.util.HashMap<>();
        for (Input o : inputs) {
            Objecttype t = o.type();
            List<Integer> computing = new ArrayList<>();
            List<Integer> groups = new ArrayList<>();
            List<ResolvedRule> init = new ArrayList<>();
            List<ResolvedRule> subInit = new ArrayList<>();
            for (int i = 0; i < model.resolvedRegels.size(); i++) {
                ResolvedRule r = model.resolvedRegels.get(i);
                if (r.category() == ResolvedRule.Kind.KENMERKTOEKENNING) continue;
                Objecttype u = ownTarget(r.target(), o.attribute());
                if (u == null || !u.isA(t)) continue;
                if (r.category() != ResolvedRule.Kind.INITIALISATIE) computing.add(i);
                else if (u == t) init.add(r);
                else subInit.add(r);
            }
            for (int i = 0; i < model.prioritizedDefaults.size(); i++) {
                PrioritizedDefaults pd = model.prioritizedDefaults.get(i);
                Objecttype u = model.objectTypeByName(pd.onderwerp());
                if (!pd.isCharacteristic() && sameName(pd.attribute(), o.attribute()) && u != null && u.isA(t))
                    groups.add(i);
            }
            Typing.Coded reason = inputWriterError(o);
            if (reason == null && computing.isEmpty() && groups.isEmpty()) {
                // nothing to put the opgave above: X is input already
                o.type().attributen.remove(o.opgegeven());
                o.type().attributen.remove(o.computed());
                o.type().kenmerken.remove(o.deviating());
                o.type().characteristicKinds.remove(o.deviating());
                String read = readGenerated(o);
                if (read != null)
                    error(o.line(),
                            "RGS-152", params("head", o.head(), "attribute", o.attribute(), "read", read));
                else
                    model.diagnostics.add(new Diagnostic(Severity.WARNING,
                            "RGS-153", params("head", o.head(), "attribute", o.attribute()), o.line()));
                continue;
            }
            if (reason == null) reason = formNameError(o, forms, generated);
            if (reason == null) reason = initialisationError(o, init);
            if (reason == null) reason = subtypeInitialisationError(o, init, subInit);
            if (reason != null) { error(o.line(), reason); continue; }

            // 2. the computing writers keep their names and shapes and write 'berekende X'
            for (int i : computing) {
                ResolvedRule r = model.resolvedRegels.get(i);
                ResolvedRule moved = new ResolvedRule(r.naam(), r.category(),
                        toComputed((Expr.AttributeReference) r.target(), o.computed()), r.value(), r.condition(),
                        r.validity(), r.statuteAnnotations());
                if (refusedTargets.contains(r)) refusedTargets.add(moved);
                model.resolvedRegels.set(i, moved);
            }
            for (int i : groups) {
                PrioritizedDefaults pd = model.prioritizedDefaults.get(i);
                PrioritizedDefaults moved = new PrioritizedDefaults(pd.naam(), pd.onderwerp(), null, o.computed(),
                        pd.unordered(), pd.defaults(), pd.source(), pd.validity());
                Origin h = pdOrigin.remove(pd);
                if (h != null) pdOrigin.put(moved, h);
                model.prioritizedDefaults.set(i, moved);
            }
            // 3. the group on X: the opgave, the computation, and the initialisation, which stays on X — its
            //    justification is "X is empty in the input", which on 'berekende X' (never input) would always hold
            Datatype dt = t.attributen.get(o.attribute());
            List<Default> defaults = new ArrayList<>();
            defaults.add(new Default(null, ALTIJD, attributeRead(o, o.opgegeven(), dt), false, null, o.opgegeven()));
            defaults.add(new Default(null, ALTIJD, attributeRead(o, o.computed(), dt), false, null, o.computed()));
            if (!init.isEmpty()) {
                ResolvedRule r = init.get(0);
                defaults.add(new Default(null, null, r.value(), true, null, annotationText(r.statuteAnnotations())));
                model.resolvedRegels.removeIf(x -> x == r);
            }
            model.prioritizedDefaults.add(new PrioritizedDefaults(o.group(), t.naam, null, o.attribute(), false,
                    defaults, null, Validity.ALTIJD));
            // 4. the deviation characteristic, spelled as the frontend emits the hand-written rule
            String p0 = "zijn " + o.opgegeven() + " is gevuld";
            String p1 = "zijn " + o.computed() + " is gevuld";
            String p2 = "zijn " + o.opgegeven() + " is ongelijk aan zijn " + o.computed();
            AttribuutSpecificatieContext a = o.declaration();
            Condition deviation = new Condition.Compound(Condition.Quantifier.ALLE, 0, List.of(
                    new Condition.Predicate(Condition.PredicateKind.GEVULD, attributeRead(o, o.opgegeven(), dt), Source.van(a, p0)),
                    new Condition.Predicate(Condition.PredicateKind.GEVULD, attributeRead(o, o.computed(), dt), Source.van(a, p1)),
                    new Condition.Comparison(Condition.Operator.ONGELIJK, attributeRead(o, o.opgegeven(), dt),
                            attributeRead(o, o.computed(), dt), Source.van(a, p2))),
                    Source.van(a, "hij aan alle volgende voorwaarden voldoet: - " + p0 + " - " + p1 + " - " + p2 + "."));
            String onderwerp = "Een " + t.naam;
            model.resolvedRegels.add(new ResolvedRule(o.regel(), ResolvedRule.Kind.KENMERKTOEKENNING,
                    new Expr.AttributeReference(o.deviating(), t, onderwerp, null, new Datatype.BooleanType(),
                            Source.van(a, onderwerp + " heeft " + o.deviating())),
                    new Expr.Literal("waar", new Datatype.BooleanType(), Source.synthetic("waar")), deviation,
                    Validity.ALTIJD, List.of()));
            // 5. the consistency: its source is the marked declaration, the one place it was written
            String declaration = a.getStart().getInputStream().getText(org.antlr.v4.runtime.misc.Interval.of(
                    a.getStart().getStartIndex(), a.MAG_OPGEGEVEN_WORDEN().getSymbol().getStopIndex()))
                    .replaceAll("\\s+", " ").trim();
            model.consistenties.add(new dev.uchar.regelspraak.ir.ResolvedConsistency(o.consistencyCheck(), t,
                    new Condition.CharacteristicCheck(o.deviating(), null, true, Source.van(a, "hij heeft geen " + o.deviating())),
                    Source.van(a, declaration), o.line(), Validity.ALTIJD));
            for (String n : List.of(o.group(), o.regel(), o.consistencyCheck()))
                generated.put(dev.uchar.regelspraak.emit.IrEmitter.norm(n), o);
            lowered.add(o);
        }
    }

    /** The object type whose own attribute {@code naam} the target {@code doel} writes, or null: a navigated target
     *  never reaches here (the target fixes the subject). */
    private static Objecttype ownTarget(Expr targetName, String naam) {
        if (!(targetName instanceof Expr.AttributeReference d) || d.objectType() == null || !sameName(d.attribute(), naam))
            return null;
        return d.objectType();
    }

    private static boolean sameName(String a, String b) {
        return a != null && b != null && dev.uchar.regelspraak.emit.IrEmitter.norm(a)
                .equals(dev.uchar.regelspraak.emit.IrEmitter.norm(b));
    }

    /** The target {@code d} with the attribute renamed; its subject, its source text and everything else stay. */
    private static Expr.AttributeReference toComputed(Expr.AttributeReference d, String naam) {
        return new Expr.AttributeReference(naam, d.objectType(), d.subjectSource(), d.navigationRole(), d.type(), d.source(),
                d.set(), d.navigationPath(), d.rangePath(), d.quantifier(), d.threshold());
    }

    /** A read of the subject's own generated attribute, spelled as the frontend spells the hand-written read. */
    private static Expr.AttributeReference attributeRead(Input o, String naam, Datatype dt) {
        return new Expr.AttributeReference(naam, o.type(), "zijn", null, dt, Source.van(o.declaration(), "zijn " + naam));
    }

    /** The writer refusals: an allocation or an object creation that writes X, and a writer of a generated member.
     *  Null when none holds. A writer of X on a subtype of T is lowered; a supertype of T does not have X (WF-105), so
     *  a rule that writes X there is refused at its own target (WF-022). */
    private Typing.Coded inputWriterError(Input o) {
        Objecttype t = o.type();
        String x = o.attribute();
        for (ResolvedAllocation v : model.allocations)
            if (sameName(v.targetAttribute(), x) && inLine(t, v.targetType()))
                return new Typing.Coded("RGS-154",
                        params("head", o.head(), "allocation", withoutVersion(v.naam()), "attribute", x,
                                "computedAttribute", o.computed()));
        for (ResolvedObjectCreation oc : model.objectCreations)
            if (inLine(t, oc.createdType())
                    && oc.initialisations().stream().anyMatch(k -> !k.characteristic() && sameName(k.attribute(), x)))
                return new Typing.Coded("RGS-155",
                        params("head", o.head(), "objectCreation", withoutVersion(oc.naam()), "attribute", x,
                                "computedAttribute", o.computed()));
        // a generated member has one writer or none: the case supplies 'opgegeven X', the moved writers compute
        // 'berekende X', the generated rule attributes 'afwijkende opgave X'
        for (String g : List.of(o.opgegeven(), o.computed(), o.deviating())) {
            String writer = null;
            int regel = 0;
            for (ResolvedRule r : model.resolvedRegels) {
                Objecttype u = ownTarget(r.target(), g);
                if (u == null || !inLine(t, u)) continue;
                writer = "regel '" + withoutVersion(r.naam()) + "'";
                int table = tableLine(r.naam());
                regel = table >= 0 ? table : r.target().source() == null ? 0 : r.target().source().line();
                break;
            }
            if (writer == null)
                for (PrioritizedDefaults pd : model.prioritizedDefaults) {
                    Objecttype u = model.objectTypeByName(pd.onderwerp());
                    if (!sameName(pd.target(), g) || u == null || !inLine(t, u)) continue;
                    writer = "de defaults-groep '" + pd.naam() + "'";
                    regel = pdOrigin.getOrDefault(pd, new Origin(0, "")).line();
                    break;
                }
            if (writer == null)
                for (ResolvedAllocation v : model.allocations)
                    if (sameName(v.targetAttribute(), g) && inLine(t, v.targetType())) {
                        writer = "de verdeling '" + withoutVersion(v.naam()) + "'";
                        break;
                    }
            if (writer == null)
                for (ResolvedObjectCreation oc : model.objectCreations)
                    if (inLine(t, oc.createdType())
                            && oc.initialisations().stream().anyMatch(k -> sameName(k.attribute(), g))) {
                        writer = "de objectcreatie '" + withoutVersion(oc.naam()) + "'";
                        break;
                    }
            if (writer != null)
                return new Typing.Coded("RGS-317",
                        params("head", o.head(), "line", o.line(), "writer", writer, "member", g,
                                "writerLine", regel > 0 ? regel : "", "inputMember", o.opgegeven(),
                                "computedMember", o.computed(), "attribute", x, "deviatingMember", o.deviating(),
                                "rule", o.regel(), "writerAt", Msg.frontendIf(regel > 0, "RGS-317.at", "line", regel)));
        }
        return null;
    }

    /** Whether {@code u} is {@code t} or a strict ancestor or descendant of it. */
    private static boolean inLine(Objecttype t, Objecttype u) {
        return u != null && (u == t || u.isA(t) || t.isA(u));
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
    private Typing.Coded formNameError(Input o, Map<String, String[]> forms, Map<String, Input> generated) {
        String[][] names = {{"de defaults-groep", o.group()}, {"de regel", o.regel()}, {"de consistentie", o.consistencyCheck()}};
        for (String[] form : names) {
            String key = dev.uchar.regelspraak.emit.IrEmitter.norm(form[1]);
            String[] site = forms.get(key);
            if (site != null)
                return new Typing.Coded("RGS-156",
                        params("head", o.head(), "line", o.line(), "formKind", form[0], "name", form[1], "earlierForm", site[0],
                                "earlierLine", site[1]));
            Input other = generated.get(key);
            if (other != null)
                return new Typing.Coded("RGS-157",
                        params("head", o.head(), "line", o.line(), "formKind", form[0], "name", form[1],
                                "otherHead", other.head(), "otherLine", other.line()));
        }
        return null;
    }

    /** The generated group carries one initialisation default, no window of its own per default, and no condition on
     *  the initialisation default: its justification is that X is empty in the input (ch. 7.3), and WF-050 admits no
     *  written one beside it. Lowered without its condition, the initialisation would decide where the author's
     *  condition fails, so a condition is refused rather than dropped. */
    private Typing.Coded initialisationError(Input o, List<ResolvedRule> init) {
        List<String> names = init.stream().map(r -> withoutVersion(r.naam())).distinct().toList();
        if (names.size() > 1)
            return new Typing.Coded("RGS-158",
                    params("head", o.head(), "attribute", o.attribute(), "names", enumeration(names)));
        if (names.size() == 1 && (init.size() > 1 || !init.get(0).validity().always()))
            return new Typing.Coded("RGS-159",
                    params("head", o.head(), "attribute", o.attribute(), "name", names.get(0),
                            "problem", Msg.frontend(init.size() > 1 ? "RGS-159.versions" : "RGS-159.other-window")));
        if (names.size() == 1 && init.get(0).condition() != null)
            return new Typing.Coded("RGS-160",
                    params("head", o.head(), "attribute", o.attribute(), "name", names.get(0)));
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
    private Typing.Coded subtypeInitialisationError(Input o, List<ResolvedRule> init, List<ResolvedRule> subInit) {
        List<ResolvedRule> lineage = new ArrayList<>(init);
        lineage.addAll(subInit);
        for (int i = 0; i < lineage.size(); i++)
            for (int j = i + 1; j < lineage.size(); j++) {
                ResolvedRule a = lineage.get(i), b = lineage.get(j);
                Objecttype ta = ownTarget(a.target(), o.attribute()), tb = ownTarget(b.target(), o.attribute());
                if (withoutVersion(a.naam()).equals(withoutVersion(b.naam())) || !ta.isA(tb) && !tb.isA(ta)) continue;
                boolean aHigh = tb.isA(ta);
                ResolvedRule high = aHigh ? a : b, low = aHigh ? b : a;
                Objecttype th = aHigh ? ta : tb, tl = aHigh ? tb : ta;
                return new Typing.Coded("RGS-161",
                        params("head", o.head(), "attribute", o.attribute(), "highName", withoutVersion(high.naam()),
                                "highType", th.naam, "lowName", withoutVersion(low.naam()), "lowType", tl.naam));
            }
        for (ResolvedRule s : subInit) {
            long versions = subInit.stream().filter(r -> withoutVersion(r.naam()).equals(withoutVersion(s.naam()))).count();
            if (versions == 1 && s.validity().always()) continue;
            String type = ownTarget(s.target(), o.attribute()).naam;
            return new Typing.Coded("RGS-162",
                    params("head", o.head(), "name", withoutVersion(s.naam()), "attribute", o.attribute(), "subtype", type,
                            "problem", Msg.frontend(versions > 1 ? "RGS-159.versions" : "RGS-159.other-window"),
                            "objectType", o.type().naam));
        }
        return null;
    }

    /** The first generated member name the model reads, or null: a reference to one of the two attributes on a type
     *  of the inheritance line, a check of the characteristic, or an allocation naming one of the attributes. The
     *  walk reaches every condition and value the emitter writes, conditions nested in expressions included. */
    private String readGenerated(Input o) {
        for (String g : List.of(o.opgegeven(), o.computed(), o.deviating())) {
            String key = dev.uchar.regelspraak.emit.IrEmitter.norm(g);
            java.util.function.Predicate<Expr> ref = e -> e instanceof Expr.AttributeReference r
                    && sameName(r.attribute(), g) && (r.objectType() == null || inLine(o.type(), r.objectType()));
            java.util.function.Predicate<Condition> check = c -> c instanceof Condition.CharacteristicCheck k
                    && dev.uchar.regelspraak.emit.IrEmitter.norm(k.characteristic()).equals(key);
            boolean read = false;
            for (ResolvedRule r : model.resolvedRegels)
                read |= uses(r.value(), ref, check) || uses(r.condition(), ref, check);
            for (var c : model.consistenties) read |= uses(c.assertie(), ref, check);
            for (var v : model.allocations)
                read |= uses(v.condition(), ref, check) || sameName(v.sourceAttribute(), g)
                        || sameName(v.ratioAttribute(), g) || sameName(v.maximumAttribute(), g)
                        || sameName(v.orderingAttribute(), g);
            for (var oc : model.objectCreations) {
                read |= uses(oc.condition(), ref, check);
                for (var k : oc.initialisations()) read |= uses(k.value(), ref, check);
            }
            for (var fc : model.factCreations) read |= uses(fc.condition(), ref, check);
            for (var d : model.resolvedDayKindDefinitions) read |= uses(d.condition(), ref, check);
            for (var pd : model.prioritizedDefaults) read |= readsDefaults(pd.defaults(), ref, check);
            if (read) return g;
        }
        return null;
    }

    private static boolean readsDefaults(List<Default> defaults, java.util.function.Predicate<Expr> ref,
                                         java.util.function.Predicate<Condition> check) {
        if (defaults == null) return false;
        for (Default d : defaults) {
            if (uses(d.condition(), ref, check) || uses(d.consequence(), ref, check)) return true;
            if (d.exceptions() != null)
                for (var group : d.exceptions().members()) if (readsDefaults(group.defaults(), ref, check)) return true;
        }
        return false;
    }

    /** Whether a node of {@code c} satisfies {@code check}, or an expression in it holds a node satisfying
     *  {@code ref}: the parts of a composite and of a quantifier fold, and every operand, conditions nested in
     *  expressions included. */
    private static boolean uses(Condition c, java.util.function.Predicate<Expr> ref,
                                 java.util.function.Predicate<Condition> check) {
        if (c == null) return false;
        if (check.test(c)) return true;
        return switch (c) {
            case Condition.Compound s -> s.components().stream().anyMatch(d -> uses(d, ref, check));
            case Condition.QuantifierFold k -> uses(k.filter(), ref, check) || uses(k.test(), ref, check);
            default -> Condition.containsOperand(c, e -> readsHere(e, ref, check));
        };
    }

    private static boolean uses(Expr e, java.util.function.Predicate<Expr> ref,
                                 java.util.function.Predicate<Condition> check) {
        return Expr.contains(e, x -> readsHere(x, ref, check));
    }

    /** One expression node: {@link Expr#contains} visits every node, so only the conditions an expression holds need
     *  entering here. */
    private static boolean readsHere(Expr x, java.util.function.Predicate<Expr> ref,
                                     java.util.function.Predicate<Condition> check) {
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
        for (ResolvedRule r : model.resolvedRegels) ruleEdges(r, uses, tekst);
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
        if (lowered.isEmpty()) return;
        Map<ReadNode, Set<ReadNode>> uses = new LinkedHashMap<>();
        Map<ReadNode, String> tekst = new java.util.HashMap<>();
        for (ResolvedRule r : model.resolvedRegels) ruleEdges(r, uses, tekst);
        for (PrioritizedDefaults pd : model.prioritizedDefaults) {
            Objecttype t = model.objectTypeByName(pd.onderwerp());
            if (t == null) continue;
            Set<ReadNode> reads = uses.computeIfAbsent(readNode(t, pd.target(), pd.isCharacteristic(), tekst),
                    k -> new java.util.LinkedHashSet<>());
            defaultEdges(pd.defaults(), t, reads, tekst);
        }
        Map<ReadNode, ReadNode> toAuthor = new java.util.HashMap<>();
        for (Input o : lowered) {
            String type = o.type().naam;
            toAuthor.put(
                    new ReadNode(type, dev.uchar.regelspraak.emit.IrEmitter.norm(o.computed()), false),
                    new ReadNode(type, dev.uchar.regelspraak.emit.IrEmitter.norm(o.attribute()), false));
        }
        reportCycles(uses, tekst, toAuthor);
    }

    /** The edges of one rule: from its target, an own attribute or characteristic of the subject, to what its value
     *  and its condition read. A navigated target stays out. */
    private static void ruleEdges(ResolvedRule r, Map<ReadNode, Set<ReadNode>> uses,
                                    Map<ReadNode, String> tekst) {
        if (!(r.target() instanceof Expr.AttributeReference d) || d.navigationRole() != null || d.objectType() == null) return;
        ReadNode targetName = readNode(d.objectType(), d.attribute(),
                r.category() == ResolvedRule.Kind.KENMERKTOEKENNING, tekst);
        Set<ReadNode> reads = uses.computeIfAbsent(targetName, k -> new java.util.LinkedHashSet<>());
        collectOwnReads(r.value(), d.objectType(), reads, tekst);
        collectOwnReads(r.condition(), d.objectType(), reads, tekst);
    }

    /** What a defaults group reads: every default's justification and consequence, through its nested exception
     *  groups, as the verifier folds them into the group's reads (ch. 7.4, ch. 7.9). An initialisation default's
     *  justification is the empty input and reads nothing. */
    private static void defaultEdges(List<Default> defaults, Objecttype subject, Set<ReadNode> result,
                                      Map<ReadNode, String> tekst) {
        if (defaults == null) return;
        for (Default d : defaults) {
            collectOwnReads(d.condition(), subject, result, tekst);
            collectOwnReads(d.consequence(), subject, result, tekst);
            if (d.exceptions() != null)
                for (ExceptionGroup g : d.exceptions().members()) defaultEdges(g.defaults(), subject, result, tekst);
        }
    }

    private void reportCycles(Map<ReadNode, Set<ReadNode>> uses, Map<ReadNode, String> tekst,
                             Map<ReadNode, ReadNode> toAuthor) {
        for (ReadNode start : uses.keySet()) {
            List<ReadNode> bindingPath = findCycle(start, start, uses, new java.util.HashSet<>(), new ArrayList<>());
            if (bindingPath != null && cycleNodes.addAll(bindingPath.stream().map(k -> toAuthor.getOrDefault(k, k)).toList()))
                model.diagnostics.add(new Diagnostic(Severity.WARNING,
                        "RGS-163", params("cycle", String.join(" -> ", bindingPath.stream().map(tekst::get).toList()), "start", tekst.get(start)), 0));
        }
    }

    /** A node of the read-dependency graph: an own attribute or characteristic of an object type (ch. 6.1 keeps the
     *  two kinds apart), its name under the verifier's name identity. The warning names it as it was first written. */
    private record ReadNode(String type, String naam, boolean characteristic) {}

    private static ReadNode readNode(Objecttype type, String naam, boolean characteristic, Map<ReadNode, String> tekst) {
        ReadNode k = new ReadNode(type.naam, dev.uchar.regelspraak.emit.IrEmitter.norm(naam), characteristic);
        tekst.putIfAbsent(k, type.naam + "." + naam);
        return k;
    }

    private static List<ReadNode> findCycle(ReadNode start, ReadNode here, Map<ReadNode, Set<ReadNode>> uses,
                                             Set<ReadNode> visited, List<ReadNode> bindingPath) {
        if (!visited.add(here)) return null;
        bindingPath.add(here);
        for (ReadNode to : uses.getOrDefault(here, Set.of())) {
            if (to.equals(start)) return new ArrayList<>(bindingPath);   // one node: a self-read
            List<ReadNode> deep = findCycle(start, to, uses, visited, bindingPath);
            if (deep != null) return deep;
        }
        bindingPath.remove(bindingPath.size() - 1);
        return null;
    }

    /** The own reads of a condition: the sides of a comparison, the operand of a predicate, the bounds of a window,
     *  an own characteristic check in either polarity, through every composite. The filter and the test of a
     *  quantifier fold read the element and not the subject, and stay out, as a fold's filter does in a value; a
     *  navigated or hosted characteristic check reads another object. */
    private static void collectOwnReads(Condition c, Objecttype subject, Set<ReadNode> result,
                                           Map<ReadNode, String> tekst) {
        switch (c) {
            case null -> { }
            case Condition.Comparison v -> {
                collectOwnReads(v.left(), subject, result, tekst);
                collectOwnReads(v.right(), subject, result, tekst);
            }
            case Condition.Predicate p -> collectOwnReads(p.operand(), subject, result, tekst);
            case Condition.TimeWindow p -> {
                collectOwnReads(p.from(), subject, result, tekst);
                collectOwnReads(p.until(), subject, result, tekst);
            }
            case Condition.CharacteristicCheck k -> {
                if (k.navigation() == null && k.owner() == null) result.add(readNode(subject, k.characteristic(), true, tekst));
            }
            case Condition.Compound sg -> sg.components().forEach(o -> collectOwnReads(o, subject, result, tekst));
            default -> { }
        }
    }

    /** The OWN attributes (same objecttype) an expression reads, as attribute nodes — the doel itself included,
     *  since a self-read is a cycle too ({@link #checkCycleReference}). */
    private static void collectOwnReads(Expr e, Objecttype subject, Set<ReadNode> result, Map<ReadNode, String> tekst) {
        if (e instanceof Expr.AttributeReference a) {
            if (a.navigationRole() == null && a.objectType() == subject) result.add(readNode(subject, a.attribute(), false, tekst));
        } else if (e instanceof Expr.Binary b) {
            collectOwnReads(b.left(), subject, result, tekst);
            collectOwnReads(b.right(), subject, result, tekst);
        } else if (e instanceof Expr.FunctionCall f) {
            for (Expr a : f.arguments()) collectOwnReads(a, subject, result, tekst);
        } else if (e instanceof Expr.Rounding roundedExpr) {
            collectOwnReads(roundedExpr.base(), subject, result, tekst);
        } else if (e instanceof Expr.Clamp bg) {
            collectOwnReads(bg.base(), subject, result, tekst);
            collectOwnReads(bg.minimum(), subject, result, tekst);
            collectOwnReads(bg.maximum(), subject, result, tekst);
        } else if (e instanceof Expr.TextComposition tr) {
            for (Expr.TextComposition.Part d : tr.parts())
                if (d instanceof Expr.TextComposition.EmbeddedExpression i) collectOwnReads(i.expr(), subject, result, tekst);
        } else if (e instanceof Expr.DimensionCellRead dc) {
            collectOwnReads(dc.attribute(), subject, result, tekst);
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
     *  ({@link Resolver#irMode} writes nothing when the model carries an error); it is deliberately
     *  NOT taken here, because one committed test expectation —
     *  {@code IrTest.references_resolve_via_fact_type_roles}, a third witness of the same producer
     *  route — asserts that a rule value of exactly this shape resolves error-free, and moving that is
     *  the maintainer's call. Raising the level is a one-token change at the {@link Severity} below.
     *
     *  <p>Scope: the READ. A rule's {@code doel} is a WRITE position, outside the fence (under WF-096 it was
     *  verifier-clean and all three executors ignored the field; since L2 the wire carries no set flag at all),
     *  so it is deliberately not walked.
     *  <p>The three VALUE slots of this repo's IR are all walked, and that is the whole list rather than
     *  a sample: a {@link ResolvedRule}'s waarde, a {@code ResolvedConsistency}'s assertie, and a
     *  §13.4.6 {@link ResolvedObjectCreation.MemberInit}'s waarde — the last a generic expressie RHS and
     *  a measured producer route of its own. {@link ResolvedAllocation} and {@link ResolvedFactCreation}
     *  hold no Expr at all (bare attribute/field NAMES plus a condition), so there is nothing to walk
     *  there; every construct's CONDITION is covered one line after it is built, see the call in
     *  {@link #ruleVersions(String, java.util.List, java.util.List, int)}. */
    private void checkSetReads() {
        // each read from the subject the emitter writes it from: the target's type, the consistency's, the creation's
        for (ResolvedRule r : model.resolvedRegels)
            checkSetRead(r.value(), r.target() instanceof Expr.AttributeReference d ? d.objectType() : null, false);
        for (var c : model.consistenties) checkSetRead(c.assertie(), c.subject());
        for (var oc : model.objectCreations)
            for (var t : oc.initialisations()) checkSetRead(t.value(), oc.subject(), false);
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
            case Expr.AttributeReference a -> {
                if (a.set() && !foldSource)
                    model.diagnostics.add(setReadError(a, subject, countSource).at(Severity.ERROR, a.source().line()));
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
            case Expr.Binary b -> {
                checkSetRead(b.left(), subject, false); checkSetRead(b.right(), subject, false);
            }
            case Expr.Rounding a -> checkSetRead(a.base(), subject, false);
            case Expr.Clamp b -> {
                checkSetRead(b.base(), subject, false); checkSetRead(b.minimum(), subject, false);
                checkSetRead(b.maximum(), subject, false);
            }
            case Expr.TextComposition t -> {
                for (Expr.TextComposition.Part d : t.parts())
                    if (d instanceof Expr.TextComposition.EmbeddedExpression i) checkSetRead(i.expr(), subject, false);
            }
            case Expr.DimensionCellRead c -> checkSetRead(c.attribute(), subject, false);
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
            case Expr.Constant ignored -> { }
            case Expr.Unresolved ignored -> { }
        }
    }

    /** The condition half of the same walk, reaching exactly what {@link Condition#containsExpr} reaches:
     *  comparison sides, the one-sided predicate operand, periode bounds, compound children, and both
     *  halves of a §8.2 quantifier fold. No condition position is a fold source, so every operand is
     *  walked unexempted — the {@link Condition.QuantifierFold} included, and deliberately so: the §8.2
     *  lowering does not EXCUSE a set reading, it REMOVES it (the quantified operand comes out as an
     *  element-own {@code set: false} read), so a surviving set reading under that node is a genuine
     *  violation and is named like any other. */
    private void checkSetRead(Condition c, Objecttype subject) {
        if (c == null) return;
        if (c instanceof Condition.Comparison v) {
            checkSetRead(v.left(), subject, false); checkSetRead(v.right(), subject, false);
        }
        else if (c instanceof Condition.Predicate p) checkSetRead(p.operand(), subject, false);
        else if (c instanceof Condition.TimeWindow p) {
            checkSetRead(p.from(), subject, false); checkSetRead(p.until(), subject, false);
        }
        else if (c instanceof Condition.Compound s) for (Condition d : s.components()) checkSetRead(d, subject);
        else if (c instanceof Condition.QuantifierFold k) {
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
    private Typing.Coded setReadError(Expr.AttributeReference a, Objecttype subject, Objecttype countSource) {
        if (countSource != null && countSource.hasMember(a.attribute()))
            return new Typing.Coded("RGS-164",
                    params("source", a.source(), "attribute", a.attribute(), "elementType", countSource.naam));
        if (countSource == null) {
            List<RegelSpraakModel.RoleBindingRef> bindingPath =
                    dev.uchar.regelspraak.emit.IrEmitter.navigationBindings(model, subject, a);
            List<Msg> sentences = new ArrayList<>();
            List<String> hops = new ArrayList<>(), roles = new ArrayList<>();
            for (int i = 0; i < bindingPath.size(); i++)
                if (toMany(bindingPath.get(i))) {
                    sentences.add(manyStepSentence(i, bindingPath.get(i).role()));
                    hops.add(String.valueOf(i));
                    roles.add(bindingPath.get(i).role());
                }
            // one sentence per to-many hop, in path order; the template renders them joined
            if (!sentences.isEmpty())
                return new Typing.Coded("RGS-165",
                        params("source", a.source(), "hops", String.join(", ", hops), "roles", String.join(", ", roles),
                                "sentences", sentences));
        }
        return countSource != null
                ? new Typing.Coded("RGS-166", params("source", a.source()))
                : new Typing.Coded("RGS-167", params("source", a.source()));
    }

    /** Whether the role a hop reaches is to-many, as its fact type declares it. */
    private boolean toMany(RegelSpraakModel.RoleBindingRef step) {
        FactType ft = model.feittypen.get(step.factType());
        return ft != null && ft.rollen.stream().anyMatch(r -> r.naam().equals(step.role()) && r.meervoudig());
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
     *  or a §4 exception block). Each §13.4.2.3 version becomes a distinct {@link ResolvedRule}. */
    private void ruleVersions(String naam, List<RegelVersieContext> versions, List<String> sourceAnn, int line) {
        // §Bron: the statuutannotaties directly above the rule header, carried onto every version's ResolvedRule.
        currentSourceAnnotations = sourceAnn;
        // §13.4.2.5 validity per version, computed up front so §4.2's non-overlap
        // invariant can be checked across the rule's versions. A version whose validity
        // bound is malformed is null here (already diagnosed) and is not emitted.
        List<Validity> validities = new ArrayList<>();
        for (RegelVersieContext v : versions) validities.add(validityOf(v.versie()));
        if (versions.size() > 1) reportOverlap(naam, versions, validities, line);
        for (int i = 0; i < versions.size(); i++) {
            Validity validity = validities.get(i);
            if (validity == null) continue; // malformed validity bound — already reported
            RegelVersieContext v = versions.get(i);
            RegelSpraakRegelContext rsr = v.regelSpraakRegel();
            // Each §13.4.2.3 version is a distinct rule selected by Rekendatum (§4.2):
            // when a rule has several, give each a unique name.
            String versionName = versions.size() > 1 ? naam + " #" + (i + 1) : naam;
            builder.setSubject(ruleSubjectOf(rsr.resultaatDeel())); // so "zijn …" resolves against the rule subject
            builder.setVariables(rsr.variabeleDeel());          // §11 capture "Daarbij geldt:" vars in BOTH forms (clears per rule)
            Condition condition = rsr.voorwaardeDeel() != null ? buildCondition(rsr.voorwaardeDeel()) : null;
            // The set fence on the CONDITION, here rather than in the naloop: this is the one place every
            // resultaatdeel kind's voorwaarde passes through, so one call covers the constructs that do
            // not become a ResolvedRule (verdeling, objectcreatie, feitcreatie, dagsoort, startpunt)
            // as well as the ones that do. The naloop then walks the VALUES alone, so nothing is said twice.
            checkSetRead(condition, builder.subject());
            resultClause(naam, versionName, rsr.resultaatDeel(), rsr.voorwaardeDeel(), condition, validity);
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
                model.diagnostics.add(new Diagnostic(Severity.WARNING, "RGS-168", params("name", naam),
                        b.getStart().getLine()));
                continue;
            }
            String valid = b.versie() != null ? DefinitionResolver.src(b.versie().versieGeldigheid()) : "altijd";
            List<String> headers = grid.get(0);
            List<List<String>> data = grid.subList(1, grid.size());
            int line = b.getStart().getLine();
            // a row the desugar cannot faithfully render (kenmerk "onwaar" conclusie, >1 OR-cel in an EN-rij) is
            // surfaced as a WARNING — never silently dropped.
            java.util.function.Consumer<Typing.Coded> warn = msg -> model.diagnostics.add(
                    msg.frame("decision-table", "name", naam).at(Severity.WARNING, line));
            // The table's // Bron: annotations go onto EVERY rule it desugars to — the multi-version precedent of
            // IR-SPEC ch. 3.1 (one comment block above a shared header, stamped per flattened entry). Each row is
            // re-parsed in a stream of its own, where no comment stands above it, so the annotations are read here,
            // off the table in the main stream, or they are lost.
            List<String> tableAnnotations = statuteAnnotations(b);
            List<String> duplicates = new ArrayList<>();
            rowTableLine = line;
            for (String ruleSrc : DecisionTableDesugar.desugarCoded(naam, valid, headers, data, warn)) {
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
            if (!duplicates.isEmpty()) {
                model.diagnostics.add(new Diagnostic(Severity.ERROR, "RGS-169",
                        params("name", naam, "names", String.join("; ", duplicates), "sentences",
                                duplicates.stream().map(n -> Msg.frontend("RGS-169.duplicate-name", "name", n, "array", "rules")).toList()),
                        line));
            }
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
        boolean[] error = {false};
        BaseErrorListener listener = new BaseErrorListener() {
            @Override public void syntaxError(Recognizer<?, ?> r, Object sym, int l, int col,
                                              String msg, RecognitionException e) {
                error[0] = true;
                model.diagnostics.add(new Diagnostic(Severity.ERROR, "RGS-170", params("message", msg), line));
            }
        };
        lexer.removeErrorListeners(); lexer.addErrorListener(listener);
        parser.removeErrorListeners(); parser.addErrorListener(listener);
        RegelContext rc = parser.regel();
        return error[0] ? null : rc;
    }

    /** §4.2: the validity periods of a rule's versions must not overlap —
     *  otherwise more than one version applies at a Rekendatum (and, writing the same
     *  attribute, they ping-pong in the engine). Flag the first intersecting pair, in the verifier's WF-021
     *  sentence: the versions of one kind reach one form of one wire array, whose versions the verifier numbers
     *  "<name> #<k>" by their place among the written ones. A day-kind definition is no form WF-021 compares, and
     *  versions of two kinds reach two arrays, so those keep this frontend's words. */
    private void reportOverlap(String naam, List<RegelVersieContext> versions, List<Validity> validities, int line) {
        List<String> rows = versions.stream()
                .map(v -> v.regelSpraakRegel() == null ? null : wireRow(v.regelSpraakRegel().resultaatDeel())).toList();
        boolean oneRow = rows.get(0) != null && rows.stream().allMatch(rows.get(0)::equals);
        for (int i = 0; i < validities.size(); i++)
            for (int j = i + 1; j < validities.size(); j++) {
                Validity a = validities.get(i), b = validities.get(j);
                if (a != null && b != null && overlaps(a, b)) {
                    if (oneRow)
                        model.diagnostics.add(new Diagnostic(Severity.ERROR, "RGS-171",
                                params("name", naam, "earlierVersion", writtenPlace(validities, i),
                                        "laterVersion", writtenPlace(validities, j), "row", rows.get(0)), line));
                    else
                        model.diagnostics.add(new Diagnostic(Severity.ERROR, "RGS-172",
                                params("name", naam), line));
                    return;
                }
            }
    }

    /** The number the verifier gives version {@code i}: its place among the versions with a validity, the ones
     *  written (a version whose bound is malformed is not). */
    private static int writtenPlace(List<Validity> validities, int i) {
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
    private static boolean overlaps(Validity a, Validity b) {
        return !beyond(a.until(), b.from()) && !beyond(b.until(), a.from());
    }
    private static boolean beyond(LocalDate until, LocalDate from) {
        return until != null && from != null && until.isBefore(from);
    }

    /** §13.4.2.5 version validity, or null when a present bound is not a valid date/year
     *  (already diagnosed): such a version is dropped, not silently made always-active.
     *  "geldig altijd" is open both ends; a year widens to the whole year (vanaf -> 1 Jan,
     *  t/m -> 31 Dec), t/m being inclusive. */
    private Validity validityOf(VersieContext v) {
        if (v == null || v.versieGeldigheid() == null) return Validity.ALTIJD;
        VersieGeldigheidContext g = v.versieGeldigheid();
        if (g.ALTIJD() != null) return Validity.ALTIJD;
        int line = v.getStart().getLine();
        java.util.List<DatumOfJaarContext> data = g.datumOfJaar();
        LocalDate from = null, until = null;
        if (g.VANAF() != null) {
            from = boundOrReport(data.get(0), true, line);
            if (from == null) return null;
            if (g.TM() != null && data.size() > 1) {
                until = boundOrReport(data.get(1), false, line);
                if (until == null) return null;
            }
        } else if (g.TM() != null && !data.isEmpty()) {
            until = boundOrReport(data.get(0), false, line);
            if (until == null) return null;
        }
        return new Validity(from, until);
    }

    /** Resolve a validity bound, or report an ERROR and return null when it is neither a
     *  valid calendar date (dd-mm-jjjj) nor a four-digit year (§13.2.24), or when it carries a
     *  time of day. Both defects are reported together, so fixing one never uncovers the other.
     *  A beslistabel re-parses its header validity once per data row; the contains-guard keeps
     *  that to the one report the author's one bound deserves. */
    private LocalDate boundOrReport(DatumOfJaarContext d, boolean under, int line) {
        LocalDate b = limit(d, under);
        if (b == null)
            reportOnce(new Diagnostic(Severity.ERROR, "RGS-173", params("bound", d != null ? src(d) : ""), line));
        // The DATE token admits a time of day, but Geldigheid holds LocalDates: there is no
        // carrier for it, and reading only the date part used to drop the time silently.
        boolean time = d != null && d.DATE() != null && d.DATE().getText().contains(":");
        if (time)
            reportOnce(new Diagnostic(Severity.ERROR, "RGS-174", params("bound", src(d)), line));
        return time ? null : b;
    }

    private void reportOnce(Diagnostic error) {
        if (!model.diagnostics.contains(error)) model.diagnostics.add(error);
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
        if (dt instanceof Datatype.Unknown o && o.reason().startsWith("domein ")) {
            model.diagnostics.add(new Diagnostic(Severity.ERROR, "RGS-175", params("reason", o.reason(), "user", where), 0));
        }
    }

    /** Resolve an assignment resultaatdeel into a {@link ResolvedRule} (the transpiler IR)
     *  and type-check the value against the target (§6 toekenning). Every resultaatdeel kind
     *  carries the version's {@code validity} and its per-version {@code versionName} — the
     *  other kinds gained both as they joined the IR, so no kind is left without a validity guard. */
    private void resultClause(String naam, String versionName, ResultaatDeelContext rd,
                           VoorwaardeDeelContext vd, Condition condition, Validity validity) {
        if (rd == null) return;
        if (rd.gelijkstelling() != null) {
            GelijkstellingContext g = rd.gelijkstelling();
            ResolvedRule.Kind category = g.WORDT_BEREKEND_ALS() != null
                    ? ResolvedRule.Kind.BEREKENING : ResolvedRule.Kind.GELIJKSTELLING;
            register(versionName, category, g.attribuutVanOnderwerp(), g.expressie(), g.getStart().getLine(), condition, validity);
        } else if (rd.initialisatie() != null) {
            InitialisatieContext i = rd.initialisatie();
            register(versionName, ResolvedRule.Kind.INITIALISATIE, i.attribuutVanOnderwerp(),
                       i.expressie(), i.getStart().getLine(), condition, validity);
        } else if (rd.kenmerkToekenning() != null) {
            characteristic(versionName, rd.kenmerkToekenning(), condition, validity);
        } else if (rd.objectCreatie() != null) {
            objectCreation(versionName, rd.objectCreatie(), condition, validity);
        } else if (rd.verdeling() != null) {
            verdeling(versionName, rd.verdeling(), condition, validity);
        } else if (rd.consistentieRegel() != null) {
            consistency(versionName, rd.consistentieRegel(), validity);
        } else if (rd.feitCreatie() != null) {
            feitCreatie(versionName, rd.feitCreatie(), vd, condition, validity);
        } else if (rd.dagsoortResultaat() != null) {
            // §9.8 dagsoortdefinitie ("Een dag is een <dagsoort> indien <voorwaarde over de dag>"): lift the
            // (dagsoort name, condition-over-the-day) into the IR. The voorwaardendeel was already resolved into
            // `condition` — its subject is "de dag" and its leaves are date-component tests, so both transpilers
            // can compile it to a per-date boolean predicate (§8.1.5 controls + §6 dagsoort durations consume it).
            String day = src(rd.dagsoortResultaat().kaleNaam()).trim();
            if (condition == null)
                model.diagnostics.add(new Diagnostic(Severity.WARNING, "RGS-176", params("day", day, "name", naam), rd.getStart().getLine()));
            else {
                // No executor consults a day-kind window, and the predicate compilers key on the dagsoort NAME, so a
                // second definitie (e.g. per §13.4.2.5 version window) is not version-dispatched: the verifier refuses
                // both (WF-031), and so does the frontend, in its words
                if (validity.from() != null || validity.until() != null)
                    model.diagnostics.add(new Typing.Coded("RGS-177", params("day", day, "name", naam))
                            .frame("day-kind-definition", "dayKind", day, "rule", naam).at(Severity.ERROR, rd.getStart().getLine()));
                model.resolvedDayKindDefinitions.stream().filter(earlier -> earlier.naam().equalsIgnoreCase(day)).findFirst()
                        .ifPresent(earlier -> model.diagnostics.add(new Typing.Coded("RGS-178",
                                params("day", day, "name", naam, "dayKind", earlier.naam()))
                                .frame("day-kind-definition", "dayKind", day, "rule", naam).at(Severity.ERROR, rd.getStart().getLine())));
                dayKindPurity(condition, day, naam, rd.getStart().getLine());
                model.resolvedDayKindDefinitions.add(new dev.uchar.regelspraak.ir.ResolvedDayKindDefinition(day, condition, validity));
            }
        } else if (rd.startpuntBepaling() != null) {
            startPointDetermination(versionName, rd.startpuntBepaling(), condition, validity);
        } else {
            // parsed but not yet lifted into the IR. Surface it as a diagnostic
            // rather than letting the rule silently disappear from the model.
            model.diagnostics.add(new Diagnostic(Severity.WARNING, "RGS-179", params("name", naam),
                    rd.getStart().getLine()));
        }
    }

    /** WF-031's purity clause, producer side: a day kind classifies a bare day before the rule graph runs, with no
     *  fact base, so its condition reads dates, date components, easterSunday, literals, parameters and other day
     *  kinds only. Before this check a condition that read an attribute, a session value or a rule's state went to
     *  disk with {@code diagnostics: []}, and the verifier refused the document. The walk and the words are the
     *  verifier's own scan of a day-kind condition: one ERROR per derived read, in walk order, at the line of the read,
     *  and no descent below a read it refuses. */
    private void dayKindPurity(Condition condition, String day, String rule, int regel) {
        dayKindCondition(condition, (sentence, source) -> model.diagnostics.add(sentence
                .frame("day-kind-definition", "dayKind", day, "rule", rule)
                .at(Severity.ERROR, source != null && source.line() > 0 ? source.line() : regel)), day, rule);
    }

    private static Typing.Coded dayKindImpure(String day, String rule, String category, String naam) {
        return new Typing.Coded("RGS-318",
                params("day", day, "rule", rule, "category", category, "name", naam == null ? "?" : naam));
    }

    private static void dayKindExpr(Expr e, java.util.function.BiConsumer<Typing.Coded, Source> report,
                                    String day, String rule) {
        if (e == null) return;
        switch (e) {
            case Expr.AttributeReference a -> report.accept(dayKindImpure(day, rule, "attribute", a.attribute()), a.source());
            // the bound day is the day itself; a session value is one value for every day of a session
            case Expr.Constant c -> {
                if (c.naam() != null && dev.uchar.regelspraak.ir.Spelling.CONTEXT_VALUE_NAME.rows().containsKey(c.naam()))
                    report.accept(new Typing.Coded("RGS-319",
                            params("day", day, "rule", rule,
                                    "value", dev.uchar.regelspraak.ir.Spelling.CONTEXT_VALUE_NAME.spelling(c.naam()))),
                            c.source());
            }
            case Expr.DimensionCellRead dc -> dayKindExpr(dc.attribute(), report, day, rule);
            case Expr.Binary b -> { dayKindExpr(b.left(), report, day, rule); dayKindExpr(b.right(), report, day, rule); }
            case Expr.Rounding a -> dayKindExpr(a.base(), report, day, rule);
            case Expr.Clamp b -> {
                dayKindExpr(b.base(), report, day, rule); dayKindExpr(b.minimum(), report, day, rule); dayKindExpr(b.maximum(), report, day, rule);
            }
            case Expr.TextComposition t -> t.parts().forEach(d -> {
                if (d instanceof Expr.TextComposition.EmbeddedExpression i) dayKindExpr(i.expr(), report, day, rule);
            });
            case Expr.FunctionCall f -> {
                f.arguments().forEach(x -> dayKindExpr(x, report, day, rule));
                dayKindExpr(f.emptyFallback(), report, day, rule);
                dayKindCondition(f.filter(), report, day, rule);
            }
            case Expr.IfThenElse k -> {
                dayKindCondition(k.condition(), report, day, rule); dayKindExpr(k.then(), report, day, rule); dayKindExpr(k.otherwise(), report, day, rule);
            }
            case Expr.Let l -> { dayKindExpr(l.value(), report, day, rule); dayKindExpr(l.body(), report, day, rule); }
            case Expr.InstanceSetUnion u -> u.branches().forEach(t -> {
                report.accept(dayKindImpure(day, rule, "instance population",
                        t.elementType() == null ? null : t.elementType().naam), u.source());
                dayKindCondition(t.filter(), report, day, rule);
            });
            case Expr.Literal ignored -> { }
            case Expr.ParameterRef ignored -> { }
            case Expr.VariableRef ignored -> { }
            case Expr.BinderRef ignored -> { }
            case Expr.Unresolved ignored -> { }
        }
    }

    private static void dayKindCondition(Condition c, java.util.function.BiConsumer<Typing.Coded, Source> report,
                                    String day, String rule) {
        if (c == null) return;
        switch (c) {
            case Condition.Comparison v -> { dayKindExpr(v.left(), report, day, rule); dayKindExpr(v.right(), report, day, rule); }
            case Condition.Predicate p -> dayKindExpr(p.operand(), report, day, rule);
            case Condition.TimeWindow p -> { dayKindExpr(p.from(), report, day, rule); dayKindExpr(p.until(), report, day, rule); }
            case Condition.Compound s -> s.components().forEach(o -> dayKindCondition(o, report, day, rule));
            case Condition.CharacteristicCheck k -> report.accept(dayKindImpure(day, rule, "characteristic", k.characteristic()), k.source());
            case Condition.RoleMembership r -> report.accept(dayKindImpure(day, rule, "role membership", r.role()), r.source());
            case Condition.FarRoleMembership r ->
                    report.accept(dayKindImpure(day, rule, "role membership", r.backref()), r.source());
            case Condition.RuleStatusCheck r -> report.accept(dayKindImpure(day, rule, "rule-version state", r.regel()), r.source());
            case Condition.QuantifierFold k -> report.accept(dayKindImpure(day, rule, "instance quantification",
                    k.elementObjectType() == null ? null : k.elementObjectType().naam), k.source());
            case Condition.Unresolved ignored -> { }
        }
    }

    /** §13.4.8 consistentieregel (single-criterion form): "&lt;expressie&gt; moet
     *  &lt;operator&gt;". The assertion is built with the same operator machinery as a
     *  voorwaarde (conditionFromToplevel), so the operand types and operator legality are the
     *  §5 ones; the subject is the object type the asserted expression is about. Compound
     *  criteria and uniciteit have since been lifted as well — the former into a
     *  {@link Condition.Compound} assertion on the same
     *  {@link dev.uchar.regelspraak.ir.ResolvedConsistency}, the latter into
     *  {@link dev.uchar.regelspraak.ir.ResolvedUniqueness} / {@link ResolvedUniquenessUnion};
     *  what neither path can carry keeps a WARNING. */
    private void consistency(String naam, ConsistentieRegelContext cr, Validity validity) {
        EnkelvoudigeConsistentieRegelContext e = cr.enkelvoudigeConsistentieRegel();
        if (e != null && e.expressie() != null && e.toplevelOperator() != null) {
            Expr lhs = builder.build(e.expressie());
            // The voorwaarde's operator machinery MINUS the ch. 4.5 quantifier fold: a Kwantorvouw has no
            // violation rule on this carrier (transpileerbaar declines it), so folding a quantified
            // assertion here would trade the set fence's ERROR for the "nog niet omgezet" WARNING below and drop
            // the check from the wire. The set reading keeps its fence until the §13.4.8 consistentieregel
            // gets a quantified form of its own. (The BULLET criteria need no such guard: a samengestelde
            // consistentieregel sets no rule subject, so the fold's range never binds.)
            Condition assertie = checkWholePeriod(conditionFromToplevelBare(lhs, e.toplevelOperator(), source(e)),
                    wholePeriod(e.toplevelOperator().gehelePeriodevergelijking()), source(e));
            Objecttype subject = lhs instanceof Expr.AttributeReference a ? a.objectType() : null;
            if (subject != null && transpilable(assertie)) {
                model.consistenties.add(new dev.uchar.regelspraak.ir.ResolvedConsistency(naam, subject, assertie, source(cr), cr.getStart().getLine(), validity));
                return;
            }
        }
        if (cr.uniciteitsControle() != null && uniqueness(naam, cr.uniciteitsControle(), validity)) return;
        // §13.4.8 samengestelde consistentieregel — quantified bullet criteria, in either the
        // subject-led ("<onderwerp> moet voldoen aan alle volgende criteria: …") or the impersonal
        // ("er moet worden voldaan aan …") form. Build a Condition.Compound over the criteria; the
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
            Condition assertie = buildCriteria(kw, crit, source(cr));
            Objecttype subject = subjectAssertion(assertie);
            // All criteria must be about the SAME subject: the rule lowers to one subject pattern, so a
            // criterion over a different object type would emit that type's attribute on the wrong fact
            // (an uncompilable property that sinks the whole DRL package). Mixed subjects -> WARNING.
            if (subject != null && transpilable(assertie) && oneSubject(assertie, subject)) {
                model.consistenties.add(new dev.uchar.regelspraak.ir.ResolvedConsistency(naam, subject, assertie, source(cr), cr.getStart().getLine(), validity));
                return;
            }
        }
        // A membership criterion ("moet een <kenmerk|rol> zijn"), elfproef /
        // numeriek-met-exact, or a computed/unresolved subject: forms whose "inconsistent" we cannot
        // express as a faithful violation rule. (The
        // cross-type "verenigd met" uniqueness left this list when ResolvedUniquenessUnion landed.)
        // Surface the gap as a WARNING rather than lift it into a silent inert rule (which would
        // both drop the check and lose the diagnostic the §13.4.7/.11/.12 path still gives).
        model.diagnostics.add(new Diagnostic(Severity.WARNING, "RGS-180", params("name", naam),
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
        return keys.stream().filter(k -> type.granularityOf(k) != null).count();
    }

    private boolean uniqueness(String naam, UniciteitsControleContext u, Validity validity) {
        // §8.1.6 parent-scoped "de X van alle Y van <parent>": unique within each parent, not globally.
        if (u.vereniging().isEmpty() && u.alleAttribuutVanOnderwerp() != null) {
            Clamp scoped = parentScoped(naam, u.alleAttribuutVanOnderwerp(), source(u), validity);
            // the ERROR names the defect; a WARNING that the rule was not lowered would be a second message for it
            if (scoped == REFUSED) return true;
            if (scoped != null) {
                var s = scoped.uniqueness();
                if (countTemporal(s.subject(), s.keys()) > 1) return false;
                model.uniquenesses.add(s); return true;
            }
        }
        ResolvedUniquenessUnion.Source first = sourceOf(u.alleAttribuutVanOnderwerp(), u.uniciteitConcatenatie());
        if (first == null) return false;
        if (u.vereniging().isEmpty()) {                              // the common single-source form
            if (countTemporal(first.type(), first.keys()) > 1) return false;
            model.uniquenesses.add(new dev.uchar.regelspraak.ir.ResolvedUniqueness(
                    naam, first.type(), first.keys(), null, source(u), validity));
            return true;
        }
        // §8.1.6 "verenigd met": uniqueness across the UNION of two or more sources' key spaces. Every
        // source must resolve and carry the SAME key arity (the values union positionally — "attributen
        // met hetzelfde datatype"); any unresolved/mismatched source leaves the whole rule a WARNING.
        java.util.List<ResolvedUniquenessUnion.Source> sources = new ArrayList<>();
        sources.add(first);
        for (VerenigingContext v : u.vereniging()) {
            ResolvedUniquenessUnion.Source b = sourceOf(v.alleAttribuutVanOnderwerp(), v.uniciteitConcatenatie());
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
        for (ResolvedUniquenessUnion.Source b : sources) {
            if (countTemporal(b.type(), b.keys()) > 1) return false;
        }
        model.uniquenessUnions.add(new dev.uchar.regelspraak.ir.ResolvedUniquenessUnion(naam, sources, source(u), validity));
        return true;
    }

    /** One uniqueness source — "de &lt;attribuut&gt; van alle &lt;Type&gt;" or a §5.7 concatenatie — resolved
     *  to its object type and key attribute(s), or null when it does not resolve (an unknown name, a
     *  parent-scoped navigation, a nested-"van" attribute). Exactly one of the two contexts is non-null. */
    private ResolvedUniquenessUnion.Source sourceOf(AlleAttribuutVanOnderwerpContext a, UniciteitConcatenatieContext c) {
        if (a != null) {
            if (!a.onderwerpKeten().isEmpty()) return null;          // scoped within a parent: not lifted
            Objecttype type = PluralMatcher.findObjectType(model.objecttypen.values(), src(a.kaleNaam(1)));
            if (type == null) return null;
            String attr = PluralMatcher.findAttribute(type, src(a.kaleNaam(0)));  // "DE attr VAN ALLE": no absorbed article
            return attr == null ? null : new ResolvedUniquenessUnion.Source(type, java.util.List.of(attr));
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
            return new ResolvedUniquenessUnion.Source(type, keys);
        }
        return null;
    }

    /** The scoped reading of a uniqueness: its record, or none where {@link #parentScoped} refused the scope. */
    private record Clamp(dev.uchar.regelspraak.ir.ResolvedUniqueness uniqueness) {}

    /** A scope refused with an ERROR. */
    private static final Clamp REFUSED = new Clamp(null);

    /** §8.1.6 parent-scoped uniqueness "de X van alle Y van &lt;parent&gt;" — unique within each parent.
     *  Resolves the element Y, its key attribute X, and the reference field on a Y that points at its
     *  scope owner &lt;parent&gt; (so the count is restricted per-parent). Null when it is not the scoped
     *  shape — then the global single-source path applies — or when the scope does not resolve;
     *  {@link #REFUSED} when it refused the scope with an ERROR. */
    private Clamp parentScoped(String naam, AlleAttribuutVanOnderwerpContext a, Source source, Validity g) {
        if (!a.onderwerpKeten().isEmpty()) return null;
        java.util.List<KaleNaamDeelContext> parts = a.kaleNaam(1).kaleNaamDeel();
        if (parts.size() != 2) return null;                          // element + exactly one parent navigation
        // The element is an object type ("alle Ploegen") or, more often when scoped, a role name
        // ("alle deelnemers van de groep") — the role's plural matches via the feittype, not the
        // conservative object-type plural.
        Objecttype elem = PluralMatcher.findObjectType(model.objecttypen.values(), src(parts.get(0)));
        if (elem == null) elem = model.roleTargetTypeToSubject(null, src(parts.get(0)));
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
            model.diagnostics.add(new Diagnostic(Severity.ERROR,
                    "RGS-181", params("name", naam, "scopeParent", src(parts.get(1)), "relationCount", scope.size(), "parentRole", parentNav, "elementType", elem.naam, "writtenRole", src(parts.get(0))), source.line()));
            return REFUSED;
        }
        RoleBindingRef scopeBinding = scope.get(0);
        // a reciprocal fact type has no two-role binding to write the scope as; this ended in the emitter's exception
        // until the fix round of 2026-09-26
        if (model.feittypen.get(scopeBinding.factType()).reciprocal) {
            model.diagnostics.add(new Diagnostic(Severity.ERROR,
                    "RGS-182", params("name", naam, "scopeParent", src(parts.get(1))), source.line()));
            return REFUSED;
        }
        // a scope parent is one instance: over a to-many role the document went to disk without a diagnostic and the
        // verifier refused it (WF-054), so the refusal is that sentence
        if (toMany(scopeBinding)) {
            model.diagnostics.add(new Diagnostic(Severity.ERROR,
                    "RGS-183", params("name", naam, "scopeParent", src(parts.get(1)), "role", scopeBinding.role()),
                    source.line()));
            return REFUSED;
        }
        return new Clamp(new dev.uchar.regelspraak.ir.ResolvedUniqueness(
                naam, elem, java.util.List.of(attr), scopeBinding.role(), source, g, scopeBinding));
    }

    /** §8.1.6: two union sources may be combined only when their key parts share a datatype. Compared
     *  by datatype KIND positionally (Tekst vs Numeriek differ; a finer unit/precision check is carded).
     *  A non-matching pair is ill-formed — the caller leaves it a WARNING. */
    private static boolean sameDatatypes(ResolvedUniquenessUnion.Source a, ResolvedUniquenessUnion.Source b) {
        for (int i = 0; i < a.keys().size(); i++) {
            Datatype da = a.type().attribute(a.keys().get(i));
            Datatype db = b.type().attribute(b.keys().get(i));
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
    private static boolean transpilable(Condition c) {
        if (c instanceof Condition.Comparison) return true;
        if (c instanceof Condition.Predicate p)
            return p.category() == Condition.PredicateKind.LEEG || p.category() == Condition.PredicateKind.GEVULD;
        // A compound is lowerable when every criterion is: under ALLE the violation is the OR of the
        // criteria's violations, under GEEN and the counting kwantoren (ten minste/hoogste/precies) the
        // negation of the count of the criteria that hold against the drempel — both Drools branches carry
        // it since the card aantal-bronarm-en-drools-lader, as ref and the ASP backend did.
        if (c instanceof Condition.Compound s)
            return !s.components().isEmpty() && s.components().stream().allMatch(RuleChecker::transpilable);
        return false;
    }

    /** §13.4.10 verdeling: distribute a source amount over a recipient attribute. The
     *  method (equal shares / proportional / a group-and-remainder scheme) is captured;
     *  only equal shares has a single-rule Drools form (the transpiler marks the rest). */
    private void verdeling(String naam, VerdelingContext v, Condition condition, Validity validity) {
        if (v.attribuutVanOnderwerp().size() < 2) return;
        Expr source = builder.buildAttribute(v.attribuutVanOnderwerp(0));
        Expr targetName = builder.buildAttribute(v.attribuutVanOnderwerp(1));
        if (!(source instanceof Expr.AttributeReference b) || !(targetName instanceof Expr.AttributeReference d)) return;

        // The bare "in gelijke delen" / "naar rato van" names the method directly. The bulleted
        // meervoudigCriterium (§9.7.7) wraps that same method with optional modifiers; only the subset
        // WITHOUT group ordering has a single forward-rule form, so a present verdelenOverGroepen keeps the
        // whole verdeling COMPLEX (a marker). The §9.7.3 maximum (naar-rato only), the §9.7.4 afronding (floor)
        // and the §9.7.5 onverdeelde rest ride on the equal-shares / naar-rato spine.
        VerdelenZonderGroepenContext z = v.verdelenZonderGroepen();
        Integer roundDec = null;
        String restAttr = null, maxAttr = null, groupAttr = null;
        boolean descending = false;
        MeervoudigCriteriumContext mc = v.meervoudigCriterium();
        // The §9.7.3/§9.7.4/§9.7.5 modifiers (maximum, afronding, onverdeelde rest) attach to BOTH the
        // bare-method form and the op-volgorde form (the grammar lets them co-occur with verdelenOverGroepen),
        // so capture them whenever a meervoudigCriterium is present; only the bare-method `z` is branch-specific.
        if (z == null && mc != null) {
            if (mc.verdelenOverGroepen() == null) z = mc.verdelenZonderGroepen();
            if (mc.verdeelAfronding() != null)
                try { roundDec = Integer.valueOf(mc.verdeelAfronding().NUMBER().getText()); }
                catch (NumberFormatException ignore) {}
            if (mc.maximumAanspraak() != null && mc.maximumAanspraak().attribuutMetLidwoord() != null
                    && mc.maximumAanspraak().attribuutMetLidwoord().kaleNaam() != null)
                maxAttr = src(mc.maximumAanspraak().attribuutMetLidwoord().kaleNaam());   // a recipient (doel) attribute
            if (mc.onverdeeldeRest() != null && b.objectType() != null
                    && builder.buildAttribute(mc.onverdeeldeRest().attribuutVanOnderwerp()) instanceof Expr.AttributeReference ra
                    && ra.objectType() == b.objectType())   // the remainder lives on the verdeler (the source)
                restAttr = ra.attribute();
            if ((roundDec != null || maxAttr != null) && restAttr == null && b.objectType() != null)
                // §9.7.3/§9.7.4: a maximum OR an afronding can leave the plafond under-distributed, so the spec
                // REQUIRES an onverdeelde rest. Without a captured one the leftover is silently lost — surface it.
                // Only when the bron resolves (an unresolved-verdeler fragment markers anyway, so warning is noise).
                model.diagnostics.add(new Diagnostic(Severity.WARNING, "RGS-184", params("name", naam), v.getStart().getLine()));
        }

        ResolvedAllocation.Method method = ResolvedAllocation.Method.COMPLEX;
        String ratio = null;
        if (z != null && z.IN_GELIJKE_DELEN() != null) method = ResolvedAllocation.Method.GELIJKE_DELEN;
        else if (z != null && z.NAAR_RATO_VAN() != null) {
            method = ResolvedAllocation.Method.NAAR_RATO;
            // the BARE weight name (kaleNaam, lidwoord stripped) so it maps straight to a recipient getter,
            // exactly like b.attribute()/d.attribute() — "de bruto premie" -> "bruto premie".
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
            Datatype groupType = groupAttr != null && d.objectType() != null ? d.objectType().attribute(groupAttr) : null;
            Datatype ratioType = ratio != null && d.objectType() != null ? d.objectType().attribute(ratio) : null;
            boolean tieOk = ratio == null || (ratioType != null && ratioType.isNumberLike());
            // §9.7.2 the ordering key need only be ORDERABLE (a number or a date) — the helper compareTo-sorts it;
            // the tie WEIGHT stays getalachtig (it is summed). A non-orderable key keeps the verdeling COMPLEX.
            if (groupType != null && groupType.isOrderable() && tieOk)
                method = ResolvedAllocation.Method.OP_VOLGORDE;
        }
        // §9.7.3: a maximale aanspraak is valid ONLY for a naar-rato split ("niet mogelijk bij gelijke verdeling"
        // — a gelijke verdeling met maximum is voor meerdere uitleg vatbaar), i.e. top-level naar rato OR an
        // op-volgorde with a naar-rato tie, and only when it names a getalachtig recipient attribute (else the
        // cap getter would not compile). Otherwise the verdeling falls back to COMPLEX (a marker).
        if (maxAttr != null) {
            Datatype maxType = d.objectType() != null ? d.objectType().attribute(maxAttr) : null;
            boolean ratioTie = method == ResolvedAllocation.Method.NAAR_RATO
                    || (method == ResolvedAllocation.Method.OP_VOLGORDE && ratio != null);
            if (!ratioTie || maxType == null || !maxType.isNumberLike()) {
                method = ResolvedAllocation.Method.COMPLEX;
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
        if (method != ResolvedAllocation.Method.COMPLEX && joinResolves) {
            Datatype weightType = (method == ResolvedAllocation.Method.NAAR_RATO
                    || method == ResolvedAllocation.Method.OP_VOLGORDE) && ratio != null
                    ? d.objectType().attribute(ratio) : null;   // op-volgorde naar-rato tie weighs like NAAR_RATO
            boolean lowers = method == ResolvedAllocation.Method.GELIJKE_DELEN
                    || method == ResolvedAllocation.Method.OP_VOLGORDE     // both lower once the join resolves
                    || (weightType != null && weightType.isNumberLike());   // mirror the transpiler's lowering gate
            if (lowers) {
                Typing.Coded error = Typing.allocationAssignment(d.objectType().attribute(d.attribute()),
                        b.objectType().attribute(b.attribute()), weightType, roundDec);
                if (error != null)
                    model.diagnostics.add(error.frame("allocation").at(Severity.ERROR, v.getStart().getLine()));
            }
        }
        model.allocations.add(new ResolvedAllocation(naam, b.objectType(), b.attribute(),
                d.objectType(), d.attribute(), method, ratio, maxAttr, roundDec, restAttr,
                groupAttr, descending, condition, validity, join));
    }

    /** The recipient join of an allocation (IR-SPEC ch. 3.2 {@code connectingRole}), as a navigated read binds
     *  (grammar/DIVERGENCES.md, "§13.4.1 A navigated read binds its written role"): the role the recipient phrase
     *  names ("alle hoofdbergers van de berging") where it names one relation between the two types, else the one
     *  relation between them. Where neither binds, the emitter has no join to write, and the refusal is an ERROR in
     *  the loader's sentence for the document without it. Null as well where a type did not resolve: that
     *  allocation is refused or markered on its own. */
    private RegelSpraakModel.RoleBindingRef allocationJoin(String naam, Expr.AttributeReference source, Expr.AttributeReference targetName,
                                                          int line) {
        Objecttype verdeler = source.objectType(), recipient = targetName.objectType();
        if (verdeler == null || recipient == null) return null;
        String written = targetName.subjectSource() == null ? "" : targetName.subjectSource().trim()
                .replaceFirst("^(?i)(alle|de|het|een|elke)\\s+", "");
        for (String kandidaat : RegelSpraakModel.nameCandidates(written)) {
            RegelSpraakModel.RoleBindingRef rb = model.connectingRoleViaNameBinding(verdeler, recipient, kandidaat);
            if (rb != null) return rb;
        }
        RegelSpraakModel.RoleBindingRef rb = model.connectingRoleBinding(verdeler, recipient);
        if (rb != null) return rb;
        String head = RegelSpraakModel.nameCandidates(written).stream().findFirst().orElse(written);
        model.diagnostics.add(new Diagnostic(Severity.ERROR,
                "RGS-185", params("name", naam, "reason", pairReason(verdeler, recipient, head)), line));
        return null;
    }

    /** §13.4.6 objectcreatie: "<subject> heeft <het rol> met <attribuut> gelijk aan <expr>
     *  (, <attribuut> gelijk aan <expr>)*". The first kaleNaam carries "<rol> met
     *  <attribuut>" (the 'met' fold the grammar leaves to the resolver); the rest are
     *  bare attribute names. */
    private void objectCreation(String naam, ObjectCreatieContext oc, Condition condition, Validity validity) {
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
            List<FactType> blockade = selfTypedLinkBlockade(subject);
            if (!blockade.isEmpty())
                model.diagnostics.add(new Diagnostic(Severity.ERROR,
                        "RGS-186", params("name", naam, "objectType", subject.naam, "roles", blockade.stream().map(ft -> ft.rollen.stream().map(r -> "'" + r.naam() + "'").collect(java.util.stream.Collectors.joining(" of ")) + " (feittype '" + ft.naam + "')").collect(java.util.stream.Collectors.joining(", "))),
                        oc.getStart().getLine()));
        }

        List<ResolvedObjectCreation.MemberInit> assignments = new ArrayList<>();
        if (roleAndAttr.length == 2 && !waarden.isEmpty())
            assignments.add(makeMemberInit(createdType, stripArticle(roleAndAttr[1]), waarden.get(0), oc));
        for (int i = 1; i < names.size() && i < waarden.size(); i++)
            assignments.add(makeMemberInit(createdType, stripArticle(src(names.get(i))), waarden.get(i), oc));

        // typeringen §6.5: each ATTRIBUUT met-clause init follows the SAME §6.2/Tabel 61 toekenning rules
        // as a gelijkstelling (family + eenheid + §6.1.2 precision; an Expr.Rounding value already reports
        // its rounded decimal count, and toekenning itself no-ops on an Onbekend value). A kenmerk-init was
        // already §13.4.6.4-checked in makeMemberInit; an unresolved created type carries no claim: skip.
        if (createdType != null)
            for (int i = 0; i < assignments.size(); i++) {
                ResolvedObjectCreation.MemberInit t = assignments.get(i);
                Datatype targetType = createdType.attribute(t.attribute());
                if (targetType == null) continue;
                // §13.2.14: a member init is a WRITE TARGET too (WF-023 names all three), so the created
                // type's declared datatype is this enumeratiewaarde's domain — promoted in place, like registreer.
                Expr promoted = ExpressionBuilder.enumAgainstContext(t.value(), targetType);
                if (promoted != t.value()) {
                    t = new ResolvedObjectCreation.MemberInit(t.attribute(), promoted, t.characteristic());
                    assignments.set(i, t);
                }
                // §13.3.4.2 enum-label membership, like registreer
                checkEnumLabel(targetType, ExpressionBuilder.valueEnumLabel(t.value()), oc.getStart().getLine());
                checkSign(targetType, t.value(), oc.getStart().getLine());   // §3.3.1 Teken-beperking, like registreer
                Typing.Coded error = Typing.assignment(targetType, t.value().type(),
                        ExpressionBuilder.unitlessNumberLiteral(t.value()));
                if (error != null)
                    model.diagnostics.add(error.frame("object-creation").at(Severity.ERROR, oc.getStart().getLine()));
            }

        model.objectCreations.add(new ResolvedObjectCreation(naam, createdType, subject, assignments, condition, validity,
                connectingRole));
    }

    /** RUNTIME-SEMANTICS ch. 5.5: without a written role, the back-link of a creation whose subject creates its own
     *  type is derived from the type pair, and for that pair the derivation links over one reciprocal fact type and
     *  over nothing else — a two-role fact type both of whose roles the type fills leaves it no choice between the
     *  roles, and neither do two candidates; ref and the ASP backend then create the object without a link. Returns
     *  the fact types that stop it, in declaration order, or an empty list where the derivation links or where no
     *  fact type relates the type to itself (the linkless creation). */
    private List<FactType> selfTypedLinkBlockade(Objecttype type) {
        List<FactType> candidates = new ArrayList<>();
        boolean twoRole = false;
        for (FactType ft : model.feittypen.values()) {
            if (ft.reciprocal && ft.rollen.size() == 1 && fillsRole(type, ft.rollen.get(0))) candidates.add(ft);
            else if (!ft.reciprocal && ft.rollen.size() == 2 && fillsRole(type, ft.rollen.get(0))
                    && fillsRole(type, ft.rollen.get(1))) {
                candidates.add(ft);
                twoRole = true;
            }
        }
        return twoRole || candidates.size() > 1 ? candidates : List.of();
    }

    /** Whether {@code type} fills {@code roleName}: the role's declared filler or one of its subtypes (IR-SPEC ch. 2.6). */
    private boolean fillsRole(Objecttype type, Role roleName) {
        Objecttype filler = model.objectTypeByName(roleName.objecttypeNaam());
        return filler != null && type.isA(filler);
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
        Expr value = builder.build(valueCtx);
        if (createdType == null || createdType.attribute(naam) != null)
            return new ResolvedObjectCreation.MemberInit(naam, value);
        // through the chain, exactly as the attribute branch above: an INHERITED kenmerk of the created
        // type is as much a member of it as its own, and reading the own list alone made one sentence's
        // two branches disagree about what the type has (a binding on the left, an ERROR on the right).
        String characteristic = createdType.characteristicName(naam);
        if (characteristic != null) {
            // the verifier refuses a value that is typed and no boolean (WF-027); a boolean-typed expression it
            // admits, where §13.4.6.4 demands the literal, so that refusal keeps its own words
            if (!(value instanceof Expr.Literal lit && lit.type() instanceof Datatype.BooleanType))
                if (value.type() != null && !value.type().isUnknown() && !(value.type() instanceof Datatype.BooleanType))
                    model.diagnostics.add(new Diagnostic(Severity.ERROR, "RGS-187",
                            params("characteristic", characteristic), oc.getStart().getLine()));
                else
                    model.diagnostics.add(new Diagnostic(Severity.ERROR, "RGS-188",
                            params("characteristic", characteristic, "value", value.source()), oc.getStart().getLine()));
            return new ResolvedObjectCreation.MemberInit(characteristic, value, true);
        }
        // the member goes out as an attribute initialisation, which the verifier refuses in these words (WF-027)
        model.diagnostics.add(new Diagnostic(Severity.ERROR, "RGS-189", params("name", naam, "objectType", createdType.naam),
                oc.getStart().getLine()));
        return new ResolvedObjectCreation.MemberInit(naam, value);
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
    private void feitCreatie(String naam, FeitCreatieContext fc, VoorwaardeDeelContext vd, Condition condition, Validity validity) {
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
                    || model.objectTypeByName(stripArticle(backNoun)) == anchor);

        Msg reason = null;
        if (anchor == null) reason = Msg.frontend("RGS-190.no-anchor");
        else if (!directAnchor) reason = Msg.frontend("RGS-190.indirect");
        else {
            // Walk the §9.4 navigation chain anker -> … -> doel-parent. The written kaleNaamDeel are outside-in
            // (roleC at index 0, the innermost hop last), so the hops from the anker are the segments in REVERSE
            // (index size-1 down to 1); segment 0 (roleC) is the final hop onto the doel. The reference field
            // lives on the many side: a to-many hop keeps it on the child (joined `child(veld == $prev)`), a
            // to-one hop on the parent (joined `child(this == $prev.field)`) — the side objectcreatie set it on.
            java.util.List<ResolvedFactCreation.PathStep> steps = new ArrayList<>();
            Objecttype cur = anchor;
            for (int i = parts.size() - 1; i >= 1; i--) {
                String role = stripArticle(src(parts.get(i)));
                Objecttype next = model.roleTargetTypeToSubject(cur, role);
                if (next == null) { reason = Msg.frontend("RGS-190.step-role", "role", role); break; }
                ResolvedFactCreation.PathStep step = pathStep(cur, next, role);
                if (step == null) { reason = Msg.frontend("RGS-190.step-field", "role", role); break; }
                steps.add(step);
                cur = next;
            }
            if (reason == null) {
                Objecttype targetName = model.roleTargetTypeToSubject(cur, roleC);
                String joinField = model.connectingRoleViaElementName(cur, roleC); // existing roleC relation's field on doel
                String setField  = model.connectingRoleViaElementName(anchor, roleB); // the NEW relation's field on doel -> anker
                if (targetName == null || joinField == null || setField == null)
                    reason = Msg.frontend("RGS-190.ambiguous");
                else if (joinField.equalsIgnoreCase(setField))
                    reason = Msg.frontend("RGS-190.same-relation");
                // The reference field lives on the MANY side, pointing at the ONE side (§13.3.9.1 cardinaliteit;
                // the convention objectCreatie links by). Both the final join (roleC, from the last hop) and the
                // set (roleB, from the anker) field exist on doel only when doel is the MANY side of BOTH — else
                // the field belongs on the other side and a setter would be rejected or link the wrong way. WARN.
                else if (!(model.toManyRole(cur, targetName, roleC) && model.toManyRole(anchor, targetName, roleB)))
                    reason = Msg.frontend("RGS-190.many-side");
                else {
                    Condition effective = targetCondition(condition, vd, anchor, targetName, roleC);
                    // uchar symbolic form (WF-028, all-or-nothing): the created relation (anker/doel via
                    // roleB) and the final join relation (cur/doel via roleC), each as a declared-name
                    // binding, plus a RolBinding on every tussenstap. Emitted only when all of that is
                    // uniquely derivable AND the created relation differs from the final hop's; otherwise
                    // the bean-field-only form stays (symbolic fields, and the tussenstap bindings, absent).
                    ElementBindingRef created = model.elementBinding(anchor, roleB);
                    ElementBindingRef join = model.elementBinding(cur, roleC);
                    boolean symbolic = created != null && join != null
                            && !created.factType().equalsIgnoreCase(join.factType())
                            && steps.stream().allMatch(s -> s.roleBinding() != null);
                    if (symbolic) {
                        RoleBindingRef path = new RoleBindingRef(join.factType(), join.elementRole());
                        model.factCreations.add(new dev.uchar.regelspraak.ir.ResolvedFactCreation(
                                naam, anchor, targetName, setField, joinField, steps, effective, validity,
                                created.factType(), created.subjectRole(), created.elementRole(), path));
                    } else {
                        List<ResolvedFactCreation.PathStep> plain = new ArrayList<>();
                        for (ResolvedFactCreation.PathStep s : steps)
                            plain.add(new ResolvedFactCreation.PathStep(s.type(), s.field(), s.viaParentField()));
                        model.factCreations.add(new dev.uchar.regelspraak.ir.ResolvedFactCreation(
                                naam, anchor, targetName, setField, joinField, plain, effective, validity));
                    }
                    return;
                }
            }
        }
        model.diagnostics.add(new Diagnostic(Severity.WARNING, "RGS-190", params("name", naam, "sentence", reason),
                fc.getStart().getLine()));
    }

    /** One §9.4 chain hop cur -&gt; next via {@code role}: which side holds the reference field, and the join
     *  form, or null when undeterminable. The field lives on the MANY side (§13.3.9.1). A TO-MANY hop keeps it
     *  on the CHILD (next) -&gt; {@code next(veld == $prev)}. A TO-ONE hop keeps it on the PARENT (cur) only when
     *  cur is genuinely the many side (the reverse role is meervoudig) or an objectcreatie placed the link on
     *  cur -&gt; {@code next(this == $prev.field)}; it is on the CHILD when an objectcreatie placed it on next. A
     *  bare one-to-one with NEITHER signal is not derivable from the model — return null (honest WARN), never a
     *  guessed side that may emit an uncompilable wrong-side join. */
    private ResolvedFactCreation.PathStep pathStep(Objecttype cur, Objecttype next, String role) {
        // The uchar RolBinding of a hop names the REACHED (far) role: for a to-many / on-child hop that
        // is the ELEMENT role the written name denotes (elementRoleBinding); for a to-one / on-parent hop
        // it is the role next fills (connectingRoleBinding). Null there withholds the whole symbolic form.
        if (model.toManyRole(cur, next, role)) {                            // to-many: field on the child
            String field = model.connectingRoleViaElementName(cur, role);
            return field == null ? null
                    : new ResolvedFactCreation.PathStep(next, field, false, model.elementRoleBinding(cur, role));
        }
        String curRole = model.connectingRoleViaElementName(cur, role);       // cur's own role in this feittype
        boolean curIsMany = curRole != null && model.toManyRole(next, cur, curRole); // reverse to-many -> cur is many
        if (curIsMany || createdTo(cur, next)) {                        // field on cur (parent)
            String field = model.connectingRole(cur, next);
            return field == null ? null
                    : new ResolvedFactCreation.PathStep(next, field, true, model.connectingRoleBinding(cur, next));
        }
        if (createdTo(next, cur)) {                                     // 1-1: objectcreatie put it on next
            String field = model.connectingRoleViaElementName(cur, role);
            return field == null ? null
                    : new ResolvedFactCreation.PathStep(next, field, false, model.elementRoleBinding(cur, role));
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
    private Condition targetCondition(Condition anchorCondition, VoorwaardeDeelContext vd, Objecttype anchor, Objecttype targetName, String roleC) {
        if (!(anchorCondition instanceof Condition.Unresolved) || vd == null || targetName == anchor) return anchorCondition;
        Condition targetV;
        int diagSnapshot = model.diagnostics.size();
        targetRole = roleC;                                    // accept "de <roleC>" as the doel; reject "hij"/anker
        try {
            builder.setSubject(targetName);
            targetV = buildCondition(vd);
        } finally {
            targetRole = null;
            builder.setSubject(anchor);                       // restore: the anker is the iterated subject
            // This second build is SPECULATIVE — the anker build and the separate voorwaarden() type-check own
            // the diagnostics; drop any this pass appended so a flawed guard is never reported twice.
            model.diagnostics.subList(diagSnapshot, model.diagnostics.size()).clear();
        }
        if (targetV instanceof Condition.CharacteristicCheck k && k.navigation() == null && k.owner() == null)
            return new Condition.CharacteristicCheck(k.characteristic(), null, k.negation(), targetName, k.source());
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
    private void characteristic(String naam, KenmerkToekenningContext kt, Condition condition, Validity validity) {
        Objecttype subject = builder.chainObjectType(kt.onderwerpKeten());
        String written = src(kt.kaleNaam());
        String characteristic = written.replaceFirst("^(?i)(de|het|een)\\s+", "");
        int table = tableLine(naam);
        int lineage = table >= 0 ? table : kt.getStart().getLine();   // a table row's lines count in its own stream
        boolean isNegated = false;
        Typing.Coded withoutCharacteristic = null;   // the refusal of a negation that names no declared characteristic
        if (kt.NIET() != null) {
            // 'is niet een X': the grammar carries the negation, so X itself must bind. An unresolved subject is
            // refused by checkRuleTargets, and the negation stays recorded for whatever reads the rule before that.
            isNegated = true;
            String g = subject == null ? null : CharacteristicMatcher.find(subject, written);
            if (g == null && subject != null) g = CharacteristicMatcher.find(subject, characteristic);
            Objecttype.CharacteristicKind category = g == null ? null : subject.characteristicKind(g);
            if (g != null && category != null && category != Objecttype.CharacteristicKind.ZELFSTANDIG) {
                characteristic = g;
                withoutCharacteristic = new Typing.Coded("RGS-191",
                        params("name", naam, "rowTable", rowTable(naam), "characteristic", g,
                                "category", category.name().toLowerCase(java.util.Locale.ROOT),
                                "spelling", negationSpelling(category, g)));
            } else if (g != null) characteristic = g;
            else if (subject != null)
                withoutCharacteristic = negationWithoutCharacteristic(naam, subject, "niet een " + characteristic, characteristic, false);
        } else if (subject != null && kt.EEN() != null) {
            // 'is een …' / 'heeft een …': after 'een' the condition side binds the name whole and reads no negation
            // there (the EEN-form, §8.1.8.3), and a result reads it the same way. A niet/geen after 'een' that leaves
            // the name binding nothing is refused, with the spelling that does negate.
            if (CharacteristicMatcher.find(subject, written) == null && CharacteristicMatcher.find(subject, characteristic) == null
                    && characteristic.matches("(?i)(niet|geen)\\s+\\S.*"))
                withoutCharacteristic = negationAfterArticle(naam, subject, kt.HEEFT() != null ? "heeft" : "is", characteristic);
        } else if (subject != null) {
            boolean has = kt.HEEFT() != null;
            Condition c = characteristicCheckCondition(subject, written, null, has, source(kt));
            if (c instanceof Condition.CharacteristicCheck k && k.negation()) {
                characteristic = k.characteristic();
                isNegated = true;
                checkNegationKind(subject, k, has, lineage);
            } else if (c == null && characteristic.matches("(?i)(niet|geen)\\s+\\S.*"))
                withoutCharacteristic = negationWithoutCharacteristic(naam, subject, characteristic,
                        characteristic.replaceFirst("^(?i)(niet|geen)\\s+", ""), true);
        }
        Expr targetName = new Expr.AttributeReference(characteristic, subject, src(kt.onderwerpKeten()),
                null, new Datatype.BooleanType(), source(kt));
        Expr value = isNegated ? NEGATED : new Expr.Literal("waar", new Datatype.BooleanType(), Source.synthetic("waar"));
        ResolvedRule r = new ResolvedRule(naam, ResolvedRule.Kind.KENMERKTOEKENNING, targetName, value, condition,
                validity, currentSourceAnnotations);
        model.resolvedRegels.add(r);
        if (withoutCharacteristic != null) {
            // Kept in resolvedRegels and marked refused, so the defaults lowering skips its family instead of
            // adding "schrijft een ander doel" about a target the author never meant.
            refusedTargets.add(r);
            model.diagnostics.add(withoutCharacteristic.at(Severity.ERROR, lineage));
        }
    }

    /** The value a negated kenmerktoekenning carries between its resolution and the defaults lowering (dialect §4,
     *  form 1). WF-022 admits only the constant true on a characteristic target, so a rule carrying this value never
     *  reaches the wire: the lowering turns it into besluit false, and {@link #checkFlatNegations} refuses a plain
     *  rule that still carries it. */
    private static final Expr NEGATED = new Expr.Literal("onwaar", new Datatype.BooleanType(), Source.synthetic("onwaar"));

    /** Whether {@code r} is a negated kenmerktoekenning — the {@link #NEGATED} value, matched on content so a copy
     *  of the rule record keeps it. A positive kenmerktoekenning always carries the synthetic "waar". */
    private static boolean isNegated(ResolvedRule r) {
        return r.category() == ResolvedRule.Kind.KENMERKTOEKENNING && r.value() instanceof Expr.Literal l
                && "onwaar".equals(l.valueText());
    }

    /** The refusal of a negation whose rest names no characteristic of the subject (dialect §4, form 1). The author
     *  wrote a negation, so the message names the rest; with {@code wholeNameAlso} the whole name was tried first and
     *  binds nothing either — the verbatim-first reading the condition side has. A rest that names an ATTRIBUTE gets
     *  the pointer to the gelijkstelling: an attribute has no polarity, its false is a value. */
    private Typing.Coded negationWithoutCharacteristic(String regel, Objecttype subject, String written, String rest,
                                           boolean wholeNameAlso) {
        Datatype attribute = subject.attribute(rest);
        Msg why = attribute instanceof Datatype.BooleanType
                ? Msg.frontend("RGS-320.boolean-attribute", "rest", rest, "subject", subject.naam)
                : attribute != null
                ? Msg.frontend("RGS-320.attribute", "rest", rest, "subject", subject.naam)
                : Msg.frontend("RGS-320.not-characteristic", "rest", rest, "subject", subject.naam,
                        "alsoWhole", Msg.frontendIf(wholeNameAlso, "RGS-320.whole-too", "written", written));
        return new Typing.Coded("RGS-320",
                params("name", regel, "rowTable", rowTable(regel), "written", written, "subject", subject.naam, "reason", why));
    }

    /** The refusal of a negation written AFTER 'een' ("is een niet X", "heeft een geen X"). The condition side
     *  reads the name after 'een' whole (§8.1.8.3), so there the words are an unbound name and no negation, and a
     *  result reads them the same way. The message gives the spelling that negates X, by X's kind where X is a
     *  characteristic of the subject. */
    private Typing.Coded negationAfterArticle(String regel, Objecttype subject, String verb, String naam) {
        String rest = naam.replaceFirst("^(?i)(niet|geen)\\s+", "");
        String g = CharacteristicMatcher.find(subject, rest);
        Objecttype.CharacteristicKind category = g == null ? null : subject.characteristicKind(g);
        return new Typing.Coded("RGS-321",
                params("name", regel, "rowTable", rowTable(regel), "verb", verb, "written", naam, "subject", subject.naam,
                        "spelling", category != null ? negationSpelling(category, g) : "",
                        "spellingClause", Msg.frontendIf(category != null, "RGS-321.write", "spelling", category != null ? negationSpelling(category, g) : "")));
    }

    /** The negated spelling Tabel 16 gives a characteristic of this kind (§8.1.8). */
    private static String negationSpelling(Objecttype.CharacteristicKind category, String characteristic) {
        return switch (category) {
            case BIJVOEGLIJK -> "'is niet " + characteristic + "'";
            case BEZITTELIJK -> "'heeft geen " + characteristic + "'";
            case ZELFSTANDIG -> "'is geen " + characteristic + "' of 'is niet een " + characteristic + "'";
        };
    }

    /** §9.9/§13.4.12 startpuntbepaling: "Het startpunt van <tijdlijn> voor <onderwerp> wordt bepaald
     *  door <expressie>". The anchor materialises as a synthetic derived attribute
     *  "startpunt <tijdlijn>" (Datum in dagen) on the subject, written by an ordinary
     *  {@link ResolvedRule} — so voorwaarde, geldigheid, freshness and idempotence ride the standard
     *  machinery in both back-ends; §9.9's "verschillende startpunten onder verschillende voorwaarden"
     *  are simply several writers of that one attribute. §6.8: the deriving expression must be
     *  Datum in dagen (ERROR otherwise; Onbekend makes no claim), and a leeg value is a fout — the
     *  back-ends raise the §9.5 Inconsistentie for it (the write itself degrades to leeg). */
    private void startPointDetermination(String naam, StartpuntBepalingContext sb, Condition condition, Validity validity) {
        int line = sb.getStart().getLine();
        String tijdlijnNaam = RegelSpraakModel.normaliseTimelineName(src(sb.tijdlijnNaam()));
        RegelSpraakModel.Timeline tijdlijn = model.timelineByName(tijdlijnNaam);
        // every refusal below is the verifier's sentence for the document it keeps off disk: WF-032 on the
        // StartPointDetermination, WF-023 on the rule that writes the anchor
        if (tijdlijn == null) {
            model.diagnostics.add(new Diagnostic(Severity.ERROR,
                    "RGS-192", params("name", naam, "timeline", tijdlijnNaam), line));
            return;
        }
        if (!tijdlijn.variabelStartpunt())
            // §9.9 defines the startpuntbepaling for a tijdlijn MET variabel startpunt, and the verifier refuses one
            // for a fixed-startpunt tijdlijn (WF-032), so the frontend refuses it too, in the verifier's words
            model.diagnostics.add(new Diagnostic(Severity.ERROR,
                    "RGS-193", params("name", naam, "timeline", tijdlijnNaam), line));
        Objecttype subject = model.resolveObjectType(src(sb.universeelOnderwerp().kaleNaam()));
        if (subject == null) {
            model.diagnostics.add(new Diagnostic(Severity.ERROR,
                    "RGS-194", params("name", naam, "objectType", src(sb.universeelOnderwerp().kaleNaam())), line));
            return;
        }
        Expr anchor = builder.buildResult(sb.expressie());   // §11: the anchor IS a rule's `waarde` slot (see registreer)
        if (!anchor.type().isUnknown() && !(anchor.type() instanceof Datatype.DatumInDagen))
            model.diagnostics.add(Typing.assignment(new Datatype.DatumInDagen(), anchor.type())
                    .frame("start-point", "rule", naam).at(Severity.ERROR, line));
        String member = "startpunt " + tijdlijnNaam;
        // the synthetic anchor name is reserved: an author-declared attribute of another type under this
        // exact name would otherwise silently merge with the anchor (a latent type-mismatched write).
        // OWN, with the rest of the §9.9 anchor family (RegelSpraakModel.hasVariableStart): the
        // anchor is DECLARED on the named subject and every reader of it keys on that name, so reading
        // this one guard through the chain while the others do not would only shift the seam.
        Datatype existing = subject.attributen.get(member);
        if (existing != null && !(existing instanceof Datatype.DatumInDagen)) {
            // an undeclared domain has no kind word, and its own refusal names it
            if (existing.isUnknown())
                model.diagnostics.add(new Diagnostic(Severity.ERROR, "RGS-195",
                        params("name", naam, "member", member, "existing", existing.naam()), line));
            else
                model.diagnostics.add(new Diagnostic(Severity.ERROR, "RGS-196",
                        params("name", naam, "member", member, "kind", Typing.kindWord(existing)), line));
            return;
        }
        subject.attributen.putIfAbsent(member, new Datatype.DatumInDagen());
        if (generatedAnchors.contains(anchorKey(subject, member)))
            anchorRules.putIfAbsent(anchorKey(subject, member),
                    new String[]{subject.naam, member, RegelSpraakModel.formName(naam)});
        if (model.startPointMember(subject, tijdlijnNaam) == null)
            model.resolvedStartPointDeterminations.add(new dev.uchar.regelspraak.ir.ResolvedStartPointDetermination(tijdlijnNaam, subject.naam, member));
        Expr targetName = new Expr.AttributeReference(member, subject, src(sb.universeelOnderwerp()),
                null, new Datatype.DatumInDagen(), source(sb));
        model.resolvedRegels.add(new ResolvedRule(naam, ResolvedRule.Kind.GELIJKSTELLING, targetName, anchor, condition, validity, currentSourceAnnotations));
    }

    /** The object type a rule is about — the object type of its assignment target. */
    private Objecttype ruleSubjectOf(ResultaatDeelContext rd) {
        AttribuutVanOnderwerpContext targetName = null;
        if (rd == null) return null;
        if (rd.gelijkstelling() != null) targetName = rd.gelijkstelling().attribuutVanOnderwerp();
        else if (rd.initialisatie() != null) targetName = rd.initialisatie().attribuutVanOnderwerp();
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
                    && builder.build(e.expressie()) instanceof Expr.AttributeReference a ? a.objectType() : null;
        }
        return targetName != null && builder.buildAttribute(targetName) instanceof Expr.AttributeReference a ? a.objectType() : null;
    }

    private void register(String naam, ResolvedRule.Kind category, AttribuutVanOnderwerpContext targetName,
                            ExpressieContext value, int line, Condition condition, Validity validity) {
        Expr targetExpr = builder.buildAttribute(targetName);
        // §11 the RESULT-part value: variable reads become BinderRefs under the variabelendeel's ch. 4.10 Let chain.
        // This is the ONE slot the chain is BUILT for — a rule's `waarde` — and it reaches the wire in two shapes:
        // as the ResolvedRule's own value, and (when the §4 defaults pass lifts this rule) as a Default's
        // `consequentie`, which {@link #defaultOf} copies straight from it. The other resultaatdeel kinds
        // (verdeling amounts, objectcreatie toekenningen, feitcreatie, a consistentieregel's asserted expression)
        // keep the v0 inline route: a consistentie assertion is a Conditie, which cannot host an Expr Let at all,
        // and the remaining ones carry several independent value slots that no single chain spans. Documented as a
        // scope limit rather than lowered half-way; no corpus or parity material exercises them.
        // §13.2.14: the doel is the WRITE-TARGET context an enumeratiewaarde takes its domain from — the
        // same context the membership check below reads. Promoting BEFORE the ResolvedRule is built is
        // what puts the typed literal on the wire (ExpressionBuilder.enumAgainstContext).
        Expr valueExpr = ExpressionBuilder.enumAgainstContext(builder.buildResult(value), targetExpr.type());
        model.resolvedRegels.add(new ResolvedRule(naam, category, targetExpr, valueExpr, condition, validity, currentSourceAnnotations));
        Datatype targetType = targetExpr.type(), valueType = valueExpr.type();
        // Both static checks below judge the value's SHAPE, so they see through the ch. 4.10 Let chain the
        // variabelendeel lowering wraps around it: `Daarbij geldt: de keuze is -49` denotes the same literal
        // the bare -49 does, and the lowering changed the wire, not what is statically decidable.
        Expr peeledValue = peeled(valueExpr);
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
        Typing.Coded error = Typing.assignment(targetType, valueType, ExpressionBuilder.unitlessNumberLiteral(valueExpr));
        if (error != null)
            model.diagnostics.add(error.frame("category", "category", category.name().toLowerCase()).at(Severity.ERROR, line));
    }

    /** The expression a ch. 4.10 {@code Let} chain ultimately DENOTES, for the shape-sensitive static checks:
     *  peel the chain, then follow a body that is nothing but a read of one of its own binders. Under ch. 10.6
     *  such a chain evaluates to exactly that expression, so `Daarbij geldt: de keuze is -49` must fail the
     *  Teken-beperking precisely as the bare literal does. Following REMOVES the binding, which bounds the walk
     *  (each binder is followed at most once) without relying on WF-076's value/body scoping. */
    private static Expr peeled(Expr e) {
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
    private void checkSign(Datatype targetType, Expr value, int line) {
        if (!(targetType instanceof Datatype.Numeriek n) || n.teken() == Datatype.Teken.ONBEPAALD) return;
        if (!(value instanceof Expr.Literal lit) || lit.type() == null || !lit.type().isNumber()) return;
        java.math.BigDecimal v = Literals.number(lit);
        if (v == null) return;
        String w = dev.uchar.regelspraak.emit.IrEmitter.numberLexeme(lit.valueText());   // the lexeme IrEmitter writes
        Msg sentence = switch (n.teken()) {
            case NIET_NEGATIEF -> v.signum() < 0 ? Msg.frontend("RGS-197.negative-literal", "lexeme", w) : null;
            case POSITIEF      -> v.signum() > 0 ? null : Msg.frontend("RGS-197.other-literal", "lexeme", w);
            case NEGATIEF      -> v.signum() < 0 ? null : Msg.frontend("RGS-197.other-literal", "lexeme", w);
            case ONBEPAALD     -> null;   // unreachable (guarded above)
        };
        if (sentence != null) model.diagnostics.add(new Diagnostic(Severity.ERROR,
                "RGS-197", params("description", sentence, "sign", signWord(n.teken())), line));
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
        model.diagnostics.add(new Diagnostic(Severity.ERROR, "RGS-198", params("label", label), line));
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
    // gained their own nodes (Condition.CharacteristicCheck, Rollidmaatschap/FarRoleMembership,
    // Periode); what is still unstructured becomes Onopgelost.
    private Condition buildCondition(VoorwaardeDeelContext v) {
        if (v.toplevelSamengesteldeVoorwaarde() != null) return buildCompound(
                v.toplevelSamengesteldeVoorwaarde().voorwaardeKwantificatie(),
                v.toplevelSamengesteldeVoorwaarde().samengesteldeVoorwaardeOnderdeel(), v.toplevelSamengesteldeVoorwaarde());
        if (v.toplevelElementaireVoorwaarde() != null) {
            ToplevelElementaireVoorwaardeContext c = v.toplevelElementaireVoorwaarde();
            // §8.3.1: after 'indien'/'dat'/'gedurende de tijd dat' the grammar admits ONLY the verb-trailing
            // (vragende) toplevelOperator — the verb-leading register is reserved for samengestelde bullets.
            if (c.expressie() != null && c.toplevelOperator() != null)
                return conditionFromToplevel(builder.build(c.expressie()), c.expressie(), c.toplevelOperator(), source(c));
            // §8.1.9 / §13.4.15.18: "indien regelversie <X> is gevuurd|inconsistent" as the whole voorwaarde.
            if (c.consistentieVoorwaarde() != null) return buildRuleStatusCheck(c.consistentieVoorwaarde());
        }
        // §13.4.13.1 / §8.4.3: a standalone periodevergelijking as the whole voorwaardendeel
        // ("… van dd. 1-1-2024 tot dd. 8-2-2025" directly after the resultaatdeel, no 'indien').
        if (v.periodevergelijkingEnkelvoudig() != null) {
            if (followsPeriodBullet(v.periodevergelijkingEnkelvoudig()))
                model.diagnostics.add(new Diagnostic(Severity.ERROR,
                        "RGS-199", params("comparison", src(v.periodevergelijkingEnkelvoudig())),
                        v.getStart().getLine()));
            return buildTimeWindow(v.periodevergelijkingEnkelvoudig());
        }
        return new Condition.Unresolved(source(v),
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
        int[] expected = {RegelSpraakLexer.PERIODE, RegelSpraakLexer.DE, RegelSpraakLexer.IS, RegelSpraakLexer.HET};
        int k = 0;
        for (int j = idx - 1; j >= 0 && k < expected.length; j--) {
            Token t = tokens.get(j);
            if (t.getChannel() != Token.DEFAULT_CHANNEL) continue;
            if (t.getType() != expected[k]) return false;
            k++;
        }
        return k == expected.length;
    }

    /** §13.4.15.68 / §8.4.3: a periodevergelijkingEnkelvoudig (vanaf / van…tot / van…tot en met / tot /
     *  tot en met) to a {@link Condition.TimeWindow}. Each bound is a §13.4.15.68 &lt;datumexpressie&gt;; a bound
     *  whose resolved type makes a non-datum claim is an ERROR (an Onbekend operand carries no claim and
     *  passes). §5.1.3 fixes the bound semantics: vanaf inclusive, tot exclusive, tot en met inclusive. */
    private Condition buildTimeWindow(PeriodevergelijkingEnkelvoudigContext p) {
        Expr van = null, until = null;
        if (p.VANAF() != null) {
            van = builder.build(p.expressie(0));
        } else if (p.VAN() != null) {
            van = builder.build(p.expressie(0));
            until = builder.build(p.expressie(1));
        } else {
            until = builder.build(p.expressie(0));
        }
        for (Expr limit : new Expr[]{van, until})
            if (limit != null && limit.type() != null && !limit.type().isUnknown() && !limit.type().isDate())
                model.diagnostics.add(new Diagnostic(Severity.ERROR,
                        "RGS-200", params("kind", Typing.kindWord(limit.type())), p.getStart().getLine()));
        for (Typing.Coded error : new Typing.Coded[]{periodBoundaryError(van, "vanaf"), periodBoundaryError(until, "tot")})
            if (error != null) model.diagnostics.add(error.at(Severity.ERROR, p.getStart().getLine()));
        return new Condition.TimeWindow(van, until, p.TOT_EN_MET() != null, source(p));
    }

    /** WF-111 (maintainer decision 2026-09-23), producer side: a window bound is a date literal or an own
     *  scalar date attribute of the subject, both of day precision. §13.4.15.68 admits any date expression and
     *  this method is where the frontend narrows it to the wire's two shapes; before it, every other bound
     *  reached disk silently and the verifier refused the document. ERROR, so {@link Resolver#irMode} writes
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
    private Typing.Coded periodBoundaryError(Expr limit, String side) {
        if (limit == null || limit instanceof Expr.Unresolved) return null;
        if (limit.type() != null && !limit.type().isUnknown() && !limit.type().isDate()) return null;
        Objecttype subject = builder.subject();
        String fromTo = side.equals("vanaf") ? "from" : "to";
        Typing.Coded moment = new Typing.Coded("RGS-201",
                params("side", side, "bound", limit.source(), "fromTo", fromTo));
        Msg what;
        if (limit instanceof Expr.Literal) {
            return limit.type() instanceof Datatype.DateTime ? moment : null;
        } else if (limit instanceof Expr.AttributeReference a) {
            if (a.navigationRole() != null || !a.navigationPath().isEmpty() || a.set()
                    || subject != null && a.objectType() != subject) {
                if (dev.uchar.regelspraak.emit.IrEmitter.navigationBindings(model, subject, a).isEmpty())
                    return periodBoundaryWithoutReader(a, side, subject);
                what = Msg.frontend("RGS-202.navigated-read", "attribute", a.attribute());
            } else if (subject == null)
                what = Msg.frontend("RGS-202.no-subject", "attribute", a.attribute());
            else if (subject.granularityOf(a.attribute()) != null)
                what = Msg.frontend("RGS-202.time-dependent", "attribute", a.attribute());
            else if (a.type() instanceof Datatype.DateTime)
                return moment;
            else return null;
        } else if (limit instanceof Expr.ParameterRef pr) {
            what = Msg.frontend("RGS-202.parameter-bound", "parameter", pr.parameter().naam());
        } else {
            what = Msg.frontend("RGS-202.computed");
        }
        return new Typing.Coded("RGS-202",
                params("side", side, "bound", limit.source(), "what", what, "fromTo", fromTo));
    }

    /** A bound that navigates, written or by naming another object type, where no path is derivable: the emitter
     *  then writes no navigation — it refuses such a model, or writes a zero-hop self-reference as the subject's own
     *  read, which the verifier admits — so no reader refuses the bound, and the refusal keeps this frontend's words. */
    private static Typing.Coded periodBoundaryWithoutReader(Expr.AttributeReference a, String side, Objecttype subject) {
        Msg reason;
        if (a.navigationRole() != null || !a.navigationPath().isEmpty() || a.set())
            reason = Msg.frontend("RGS-322.navigated-read", "attribute", a.attribute());
        else if (a.objectType() != null && subject.isA(a.objectType()))
            reason = Msg.frontend("RGS-322.supertype", "attribute", a.attribute(), "type", a.objectType().naam, "subject", subject.naam);
        else if (a.objectType() != null && a.objectType().isA(subject))
            reason = Msg.frontend("RGS-322.subtype", "attribute", a.attribute(), "type", a.objectType().naam, "subject", subject.naam);
        else
            reason = Msg.frontend("RGS-322.other-type", "attribute", a.attribute(),
                    "type", a.objectType() == null ? "?" : a.objectType().naam, "subject", subject.naam);
        return new Typing.Coded("RGS-322",
                params("side", side, "bound", a.source(), "reason", reason));
    }

    private Condition buildCompound(VoorwaardeKwantificatieContext k,
                                      SamengesteldeVoorwaardeOnderdeelContext component, ParserRuleContext whole) {
        // §13.4.14.6: when the indentation-blind grammar ABSORBED a dedented sibling bullet into a deeper compound but
        // the bullet COLUMNS re-nest cleanly, rebuild the correct sibling tree so the compound FIRES (depth-aware
        // re-nesting). reNestParts returns null when there is no column-absorption (the common case — a
        // byte-identical no-op) OR the columns do not disambiguate (the existing inconsistency-marker path then applies).
        List<Condition> herNest = reNestParts(component);
        List<Condition> components;
        if (herNest != null) {
            components = herNest;
        } else {
            components = new ArrayList<>();
            if (component != null)
                for (GenesteVoorwaardeContext g : component.genesteVoorwaarde()) components.add(buildNested(g));
        }
        Condition.Quantifier quantifier = quantifierOf(k);
        int threshold = thresholdOf(k);
        // A counting quantifier's telwoord is a positive whole number (§13.4.14.4 /
        // §13.4.14.5: één/twee/… or a written getal). A 0, negative, decimal, fraction or
        // unparseable value all reach thresholdOf as < 1; flag it rather than let it collapse
        // the rule to always-true (minstens(0) is vacuously true). An over-size N is fine —
        // it is a faithfully unsatisfiable "false", so it is not an error.
        checkThreshold(quantifier, k, (k != null ? k : whole).getStart().getLine());
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
                || (quantifier != Condition.Quantifier.ALLE && quantifier != Condition.Quantifier.DE && ambiguousNestedBullets(components))))
            return new Condition.Unresolved(source(whole),
                    "de bullets van de samengestelde voorwaarde staan op verschillende kolommen of dragen "
                    + "verschillende tekens, dus welke bullet onder welke hoort ligt niet vast (§13.4.14.6)");
        return new Condition.Compound(quantifier, threshold, components, source(whole));
    }

    /** §13.4.14.4/.5 a counting quantifier's telwoord must be a positive whole number (>= 1): a 0 / negative / decimal /
     *  unparseable value reaches {@link #thresholdOf} as &lt; 1 and would collapse a ten-minste/ten-hoogste/precies to
     *  vacuously-true (minstens(0) always holds), so flag it. Shared by {@link #buildCompound} and the depth-aware
     *  re-nesting {@link #buildFromNodes} (which builds inner compounds directly, bypassing buildCompound). */
    private void checkThreshold(Condition.Quantifier quantifier, VoorwaardeKwantificatieContext k, int regel) {
        int threshold = thresholdOf(k);
        if ((quantifier == Condition.Quantifier.TEN_MINSTE || quantifier == Condition.Quantifier.TEN_HOOGSTE
                || quantifier == Condition.Quantifier.PRECIES) && threshold < 1)
            // a whole number reaches the wire as written and the verifier refuses it in WF-041's words; a decimal
            // or fraction reaches it as 0, a number the author did not write, so that refusal keeps its own words
            if (wholeNumeral(k))
                model.diagnostics.add(new Diagnostic(Severity.ERROR, "RGS-203",
                        params("threshold", threshold), regel));
            else
                model.diagnostics.add(new Diagnostic(Severity.ERROR, "RGS-204", params(), regel));
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
    private List<Condition> buildFromNodes(List<LogicalNode> nodes) {
        List<Condition> result = new ArrayList<>();
        for (LogicalNode k : nodes) {
            GenesteSamengesteldeVoorwaardeContext c = k.ctx().genesteSamengesteldeVoorwaarde();
            if (c != null) {
                Condition.Quantifier kw = quantifierOf(c.voorwaardeKwantificatie());
                int threshold = thresholdOf(c.voorwaardeKwantificatie());
                checkThreshold(kw, c.voorwaardeKwantificatie(), c.getStart().getLine());   // a degenerate inner telwoord must still be flagged
                result.add(new Condition.Compound(kw, threshold, buildFromNodes(k.children()), source(c)));
            }
            else if (k.ctx().elementaireVoorwaarde() != null)
                result.add(buildElementary(k.ctx().elementaireVoorwaarde()));
            else
                // GEEN GETUIGE: genesteVoorwaarde is `bullet (genesteSamengesteldeVoorwaarde |
                // elementaireVoorwaarde) DOT?`, dus een knoop draagt er altijd één — en deze tak wordt
                // pas bereikt NA een geslaagde kolom-hernesting, die een kapotte ontleding juist afwijst.
                // De reden staat er voor het geval de grammatica een derde tak krijgt.
                result.add(new Condition.Unresolved(source(k.ctx()),
                        "deze bullet van de hernestte opsomming is geen geneste samengestelde voorwaarde en "
                        + "geen elementaire voorwaarde (§13.4.14)"));
        }
        return result;
    }

    /** §13.4.14.6: if a bullet list carries a column-disambiguatable absorption, return the correctly re-nested
     *  onderdelen; else null (the caller keeps its existing per-bullet build + inconsistency-marker path). Returns null
     *  for the common no-absorption case (a structural no-op, so well-formatted lists are byte-identical) and for the
     *  un-disambiguatable residual (glyph-only or same-column-same-glyph, which the marker path then declines). */
    private List<Condition> reNestParts(SamengesteldeVoorwaardeOnderdeelContext component) {
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
    private static boolean ambiguousNestedBullets(List<Condition> components) {
        return components.stream().anyMatch(o -> o instanceof Condition.Compound inner
                && inner.components().stream().anyMatch(x -> x instanceof Condition.Compound));
    }

    /** The threshold count N of a ten-minste/ten-hoogste/precies quantifier (§13.4.14.4),
     *  or 0 when the quantifier carries no count (alle / geen / de). */
    private int thresholdOf(VoorwaardeKwantificatieContext k) {
        return k == null ? 0 : numeralValue(k.telwoord());
    }

    /** §5.6/§13.4.13.4 a COMPOUND subselectie ("… die aan alle volgende voorwaarden voldoen: - … - …") resolved
     *  against the AGGREGATED ELEMENT type: each bullet binds to the element (not the rule subject), so the resulting
     *  {@link Condition.Compound} is a filter on the accumulate SOURCE pattern. The bouwer subject is saved and
     *  restored around the bullet walk — aggregationFilter runs DURING {@code builder.build}, nested inside the outer rule
     *  resolution, so the rule subject must be put back (in a finally, even on a bullet-resolution throw). */
    private Condition compoundElementFilter(SamengesteldPredicaatContext sp, Objecttype elem) {
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
                return new Condition.Compound(quantifierQuantification(sp.kwantificatie()),
                        sp.kwantificatie() == null ? 0 : numeralValue(sp.kwantificatie().telwoord()),
                        List.of(buildElementary(sp.elementaireVoorwaarde())), source(sp));
            // §13.4.14.6 re-nest a column-absorbed bullet list (the SAME depth-aware machinery as a voorwaarde compound)
            List<Condition> herNest = reNestParts(sp.samengesteldeVoorwaardeOnderdeel());
            List<Condition> components;
            if (herNest != null) {
                components = herNest;
            } else {
                components = new ArrayList<>();
                for (GenesteVoorwaardeContext g : sp.samengesteldeVoorwaardeOnderdeel().genesteVoorwaarde())
                    components.add(buildNested(g));     // the SAME bullet machinery as a voorwaarde compound
            }
            Condition.Quantifier kw = quantifierQuantification(sp.kwantificatie());
            // §13.4.14.6 IR-integrity guard: a nested-compound bullet with a DEEPER compound child under a
            // COUNTING quantifier stays Onopgelost — the indentation-blind bullet grammar can flatten sibling
            // bullets into a depth-2 child in ways the column re-nesting cannot always rebuild, and a
            // mis-merged bullet under a COUNT changes what is counted (AND-merging what the spec OR-s): a
            // fired-wrong shared value. Under ALLE/DE the merge is semantics-preserving (every bullet must
            // hold anyway), so no guard applies. This is a statement about the PARSE, not about what a
            // back-end can lower (back-end-agnostic resolver): capability verdicts live in the back-ends and are reconciled
            // by the manifest-parity test.
            if (kw != Condition.Quantifier.ALLE && kw != Condition.Quantifier.DE
                    && components.stream().anyMatch(c -> c instanceof Condition.Compound s
                            && s.components().stream().anyMatch(x -> x instanceof Condition.Compound)))
                return new Condition.Unresolved(source(sp),
                        "een tellende kwantor over een deelselectie waarvan een bullet zelf nog een diepere "
                        + "samengestelde voorwaarde draagt: de inspringblinde bullet-grammatica kan zusterbullets "
                        + "in die diepere laag getrokken hebben (§13.4.14.6), en dan telt de selectie een andere "
                        + "verzameling dan de zin noemt");
            return new Condition.Compound(kw,
                    sp.kwantificatie() == null ? 0 : numeralValue(sp.kwantificatie().telwoord()), components, source(sp));
        } finally {
            builder.setSubject(previous);                   // RESTORE
        }
    }

    /** §13.4.14.5 kwantificatie (used by the compound subselectie) -> Kwantor. Distinct from voorwaardeKwantificatie:
     *  it admits 'al' (AL, treated as ALLE) and ALLE has no "( van de )?" tail. A bare DE quantifies the whole set. */
    private static Condition.Quantifier quantifierQuantification(KwantificatieContext k) {
        if (k == null || k.ALLE() != null || k.AL() != null) return Condition.Quantifier.ALLE;
        if (k.GEEN_VAN_DE() != null) return Condition.Quantifier.GEEN;
        if (k.TEN_MINSTE() != null)  return Condition.Quantifier.TEN_MINSTE;
        if (k.TEN_HOOGSTE() != null) return Condition.Quantifier.TEN_HOOGSTE;
        if (k.PRECIES() != null)     return Condition.Quantifier.PRECIES;
        return Condition.Quantifier.DE;
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

    /** §13.4.8 samengestelde consistentie-criteria → a {@link Condition.Compound} over the criteria. */
    private Condition buildCriteria(ConsistentieKwantificatieContext k, SamengesteldCriteriumOnderdeelContext component, Source source) {
        List<Condition> components = new ArrayList<>();
        for (GenestCriteriumContext g : component.genestCriterium()) components.add(buildCriterion(g));
        return new Condition.Compound(quantifierConsistency(k), k == null ? 0 : numeralValue(k.telwoord()), components, source);
    }

    private Condition buildCriterion(GenestCriteriumContext g) {
        if (g.samengesteldCriterium() != null) {
            SamengesteldCriteriumContext c = g.samengesteldCriterium();
            return buildCriteria(c.consistentieKwantificatie(), c.samengesteldCriteriumOnderdeel(), source(c));
        }
        if (g.elementaireVoorwaarde() != null) return buildElementary(g.elementaireVoorwaarde());
        // GEEN GETUIGE: genestCriterium draagt altijd een van beide takken, en een consistentieregel met
        // een kapot criterium strandt eerder — op de syntaxfout, met haar eigen ERROR. Gemeten 2026-09-19.
        return new Condition.Unresolved(source(g),
                "dit geneste consistentiecriterium is geen samengesteld criterium en geen elementaire "
                + "voorwaarde (§13.4.8)");
    }

    /** §13.4.14.3 consistentie-kwantor; null (the single "het volgende criterium") is the trivial ALLE. */
    private static Condition.Quantifier quantifierConsistency(ConsistentieKwantificatieContext k) {
        if (k == null || k.ALLE() != null) return Condition.Quantifier.ALLE;
        if (k.GEEN_VAN_DE() != null)  return Condition.Quantifier.GEEN;
        if (k.TEN_MINSTE() != null)   return Condition.Quantifier.TEN_MINSTE;
        if (k.TEN_HOOGSTE() != null)  return Condition.Quantifier.TEN_HOOGSTE;
        if (k.PRECIES() != null)      return Condition.Quantifier.PRECIES;
        return Condition.Quantifier.ALLE;
    }

    /** Every criterion's left operand is about {@code subject} (a literal/parameter left side is fine):
     *  the rule lowers to one subject pattern, so a criterion over another object type cannot be lifted. */
    private static boolean oneSubject(Condition c, Objecttype subject) {
        if (c instanceof Condition.Comparison v)
            return !(v.left() instanceof Expr.AttributeReference a) || a.objectType() == subject;
        if (c instanceof Condition.Predicate p)
            return !(p.operand() instanceof Expr.AttributeReference a) || a.objectType() == subject;
        if (c instanceof Condition.Compound s)
            return s.components().stream().allMatch(d -> oneSubject(d, subject));
        return true;
    }

    /** The type the criteria are about — the object type of the first criterion's left operand. */
    private static Objecttype subjectAssertion(Condition c) {
        if (c instanceof Condition.Comparison v && v.left() instanceof Expr.AttributeReference a) return a.objectType();
        if (c instanceof Condition.Predicate p && p.operand() instanceof Expr.AttributeReference a) return a.objectType();
        if (c instanceof Condition.Compound s)
            for (Condition d : s.components()) { Objecttype t = subjectAssertion(d); if (t != null) return t; }
        return null;
    }

    private Condition buildNested(GenesteVoorwaardeContext g) {
        if (g.genesteSamengesteldeVoorwaarde() != null) {
            GenesteSamengesteldeVoorwaardeContext c = g.genesteSamengesteldeVoorwaarde();
            return buildCompound(c.voorwaardeKwantificatie(), c.samengesteldeVoorwaardeOnderdeel(), c);
        }
        if (g.elementaireVoorwaarde() != null) return buildElementary(g.elementaireVoorwaarde());
        return new Condition.Unresolved(source(g),
                "deze bullet van de samengestelde voorwaarde is geen geneste samengestelde voorwaarde en "
                + "geen elementaire voorwaarde (§13.4.14)");
    }

    /** An §13.4.15 elementaire voorwaarde / consistentie-criterium ("&lt;expressie&gt; &lt;operator&gt;") to a
     *  {@link Condition}; shared by the samengestelde voorwaarde and the samengestelde consistentieregel. */
    private Condition buildElementary(ElementaireVoorwaardeContext c) {
        if (c.expressie() != null && c.nonToplevelOperator() != null)
            return conditionFromNonToplevel(builder.build(c.expressie()), c.expressie(), c.nonToplevelOperator(), source(c));
        if (c.expressie() != null && c.toplevelOperator() != null)
            return conditionFromToplevel(builder.build(c.expressie()), c.expressie(), c.toplevelOperator(), source(c));
        // §13.4.15.69 / §8.4.3: the "het is de periode <periodevergelijkingEnkelvoudig>" bullet form.
        if (c.periodevergelijkingElementair() != null)
            return buildTimeWindow(c.periodevergelijkingElementair().periodevergelijkingEnkelvoudig());
        // §8.1.9 / §13.4.15.66: the "regelversie <X> is gevuurd|inconsistent" regelpredicaat as a bullet.
        if (c.consistentieVoorwaarde() != null) return buildRuleStatusCheck(c.consistentieVoorwaarde());
        // GEEN GETUIGE: elementaireVoorwaarde heeft precies drie takken en alle drie staan hierboven, dus
        // een gevulde context bereikt deze regel niet; een kapotte bullet valt een niveau hoger uit
        // (buildNested, dat wel een getuige heeft). Gemeten 2026-09-19.
        return new Condition.Unresolved(source(c),
                "de elementaire voorwaarde heeft geen van de §13.4.15-vormen — geen expressie met operator, "
                + "geen periodevergelijking en geen regelpredicaat");
    }

    /** §8.1.9 / §13.4.15.66/.18 regelpredicaat to a {@link Condition.RuleStatusCheck}. The reference may point
     *  FORWARD to a rule defined later in the document, so no existence check happens here — the back-ends
     *  verify the name against the model's Inconsistentie producers and decline an unknown reference as an
     *  honest marker (never a silent never-matching exists). The parenthesised versie-aanduiding is parsed
     *  but only carried in bron (see Condition.RuleStatusCheck). */
    private Condition buildRuleStatusCheck(ConsistentieVoorwaardeContext c) {
        return new Condition.RuleStatusCheck(src(c.vrijeNaam()), c.INCONSISTENT() != null, source(c));
    }

    /** §8.2 / §13.4.15.70: the optional "(niet) gedurende de/het gehele jaar|maand" whole-period qualifier that
     *  precedes a predicate (verb-split or verb-leading); null when absent. {@code jaar} reads the JAAR vs MAAND
     *  keyword, {@code niet} the leading negation. The transpilers honour it on a tijdsafhankelijk onderwerp. */
    private static Condition.WholePeriod wholePeriod(GehelePeriodevergelijkingContext g) {
        if (g == null) return null;
        boolean year = g.JAAR() != null;   // else MAAND
        // the negation is either the bare NIET prefix (stellende vorm "is niet gedurende …") or the combined
        // NIET_GEDURENDE_*_GEHELE token (vragende vorm "… niet gedurende …", lexed as one token so the onderwerp
        // kaleNaam cannot absorb the leading "niet").
        boolean negated = g.NIET() != null || g.NIET_GEDURENDE_HET_GEHELE() != null || g.NIET_GEDURENDE_DE_GEHELE() != null;
        return new Condition.WholePeriod(year, negated);
    }

    /** §8.1.4/§13.4.15.84 the &lt;geheelgetal&gt; digit-count N of "numeriek met exact N cijfers". The NUMBER token is
     *  over-broad (it also matches a §13.2.4 decimaalgetal "9,5"), but the spec requires a §13.2.3 &lt;geheelgetal&gt;
     *  (which DOES admit a leading minus); reject a decimal/rational count with an ERROR rather than silently coercing
     *  it to 0 (an inert always-false predicate). 0 if absent. */
    private int countDigits(org.antlr.v4.runtime.tree.TerminalNode num) {
        if (num == null) return 0;
        String t = num.getText();
        if (!t.matches("-?\\d+")) {
            model.diagnostics.add(new Diagnostic(Severity.ERROR, "RGS-205", params("lexeme", t), 0));
            return 0;
        }
        return Integer.parseInt(t);
    }

    /** The (operand, negatie) of an elfproef / numeriek-met-exact predicate. The §8.1 Tabel-16 vragende negated form
     *  "&lt;X&gt; niet aan de elfproef voldoet" / "&lt;X&gt; niet numeriek is met exact N cijfers" has its leading
     *  "niet" greedily absorbed into the operand's kaleNaam (NIET is a kaleNaamWoord), so op.NIET() is null; recover
     *  it by stripping the absorbed " niet" off the operand (verbatim-first via {@link #stripAbsorbedNiet}). The
     *  stellende form carries an explicit NIET token, so {@code explicit} short-circuits the strip. */
    private record ElfOperand(Expr operand, boolean negation) {}
    private static ElfOperand elfOperand(boolean explicit, Expr lhs) {
        // The stellende form carries an explicit NIET token; otherwise recover a vragende negator the greedy parse
        // absorbed into the operand (ExpressionBuilder.subjectPossessive resolves "zijn <attr> niet" to the BARE attribuut but
        // keeps the full bron, so the absorbed "niet" survives as the bron's trailing word).
        return new ElfOperand(lhs, explicit || absorbedNegator(lhs));
    }

    /** Whether {@code lhs} is an operand into whose source the greedy parse absorbed a trailing "niet" negator — an
     *  {@link Expr.AttributeReference} whose bron ends in " niet" but whose (bare) attribuut does NOT end in "niet"
     *  (verbatim-first: an attribuut literally named "… niet" carries its own niet and must not be de-negated). The
     *  elfproef / numeriek-met-exact predicates RECOVER this as their negatie; every OTHER operator cannot honour a
     *  dangling negator, so it must marker rather than silently emit the inverted positive. */
    private static boolean absorbedNegator(Expr lhs) {
        return lhs instanceof Expr.AttributeReference a && a.source() != null
                && a.source().snippet().stripTrailing().toLowerCase().endsWith(" niet")
                && !a.attribute().toLowerCase().endsWith("niet");
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
    private Condition conditionFromToplevel(Expr lhs, ExpressieContext lhsCtx, ToplevelOperatorContext op, Source source) {
        Condition plain = conditionFromToplevelBare(lhs, op, source);
        if (plain instanceof Condition.Unresolved && op.gehelePeriodevergelijking() == null) {
            Condition fold = quantifierCharacteristicCheck(lhs, lhsCtx, op.kaleNaam(), op.EEN() != null,
                    op.HEEFT() != null || op.HEBBEN() != null, source);
            if (fold != null) return fold;
        }
        return quantifierFold(checkWholePeriod(plain, wholePeriod(op.gehelePeriodevergelijking()), source),
                lhsCtx, op.expressie(), source);
    }

    private Condition conditionFromToplevelBare(Expr lhs, ToplevelOperatorContext op, Source source) {
        Condition.WholePeriod periode = wholePeriod(op.gehelePeriodevergelijking());   // §8.2 whole-period check
        // A dangling absorbed "niet" silently INVERTS a comparison / leeg-gevuld (the bare-attr resolution dropped the
        // negator) -> marker it. The kenmerkcheck DE-negates it (bindCharacteristicCheck) and elfproef/numeriek RECOVER it
        // (elfOperand), so only the comparison + leeg/gevuld operators are guarded here.
        if (absorbedNegator(lhs) && (op.vergelijkingsTekenGesplitst() != null || op.LEEG() != null || op.GEVULD() != null))
            return new Condition.Unresolved(source, REASON_NOT_ABSORBED);
        if (op.vergelijkingsTekenGesplitst() != null && op.expressie() != null)
            return ExpressionBuilder.comparison(toOperator(cmpOf(op.vergelijkingsTekenGesplitst())),
                    lhs, builder.build(op.expressie()), periode, source);
        if (op.LEEG() != null)   return new Condition.Predicate(Condition.PredicateKind.LEEG, lhs, periode, source);
        if (op.GEVULD() != null) return new Condition.Predicate(Condition.PredicateKind.GEVULD, lhs, periode, source);
        if (op.AAN_DE_ELFPROEF() != null) {
            ElfOperand e = elfOperand(op.NIET() != null, lhs);   // recover an absorbed vragende "niet"
            return new Condition.Predicate(Condition.PredicateKind.ELFPROEF, e.operand(), e.negation(), periode, source);
        }
        if (op.NUMERIEK_IS_MET_EXACT() != null || op.NUMERIEK_ZIJN_MET_EXACT() != null) {
            ElfOperand e = elfOperand(op.NIET() != null, lhs);
            return new Condition.Predicate(Condition.PredicateKind.NUMERIEK_MET_EXACT, e.operand(), e.negation(),
                    countDigits(op.NUMBER()), periode, source);
        }
        Condition characteristic = characteristicCheckPredicate(lhs, op.kaleNaam(), op.EEN() != null,
                op.HEEFT() != null || op.HEBBEN() != null, periode, source); // EEN? kaleNaam (is|heeft): §8.1.8
        // else objecttype/navigation membership — the caller may still read the whole sentence as a
        // quantifier fold (quantifierCharacteristicCheck), which is exactly why this marker carries its reason instead
        // of raising it here: reported at construction it would flag every sentence that folds cleanly.
        return characteristic != null ? characteristic : new Condition.Unresolved(source, REASON_NO_CHARACTERISTIC_NO_ROLE);
    }

    private Condition conditionFromNonToplevel(Expr lhs, ExpressieContext lhsCtx, NonToplevelOperatorContext op, Source source) {
        Condition plain = conditionFromNonToplevelBare(lhs, op, source);
        // op.NIET() guards the §8.1.8.3 "is niet een <k>" branch above: it resolves the POSITIVE form and
        // flips it, so a null binding there arrives as an Onopgelost whose negation is not in the parse
        // tree any more. Re-binding it here would emit the POSITIVE membership — silently inverted.
        if (plain instanceof Condition.Unresolved && op.gehelePeriodevergelijking() == null && op.NIET() == null) {
            Condition fold = quantifierCharacteristicCheck(lhs, lhsCtx, op.kaleNaam(), op.EEN() != null,
                    op.HEEFT() != null || op.HEBBEN() != null, source);
            if (fold != null) return fold;
        }
        return quantifierFold(checkWholePeriod(plain, wholePeriod(op.gehelePeriodevergelijking()), source),
                lhsCtx, op.expressie(), source);
    }

    /** §8.2: the "gedurende de/het gehele &lt;periode&gt;" check is only defined when the predicate's
     *  onderwerp is TIJDSAFHANKELIJK. A qualifier over a static onderwerp is a spec error — reject it (an ERROR +
     *  Onopgelost) rather than silently markering a rule that can never fire. {@code niet} is irrelevant here (a
     *  whole-period NEGATION of a static check is just as ill-defined). */
    private Condition checkWholePeriod(Condition c, Condition.WholePeriod periode, Source source) {
        if (periode == null || subjectIsTemporal(c)) return c;
        // the verifier refuses the qualifier on a comparison, a predicate and an own characteristic (WF-043); a
        // navigated characteristic and the other condition kinds it does not check, so those keep their own words
        if (c instanceof Condition.Comparison || c instanceof Condition.Predicate)
            model.diagnostics.add(new Diagnostic(Severity.ERROR, "RGS-206", params(), source.line()));
        else if (c instanceof Condition.CharacteristicCheck k && k.navigation() == null)
            model.diagnostics.add(new Diagnostic(Severity.ERROR, "RGS-207", params(), source.line()));
        else
            model.diagnostics.add(new Diagnostic(Severity.ERROR, "RGS-208",
                    params("source", source), source.line()));
        return new Condition.Unresolved(source, null);   // the ERROR above is this marker's diagnosis
    }

    /**
     * §13.4.2.9 + §13.4.15 / IR-SPEC ch. 4.5: lower the official sentence shape "… indien de
     * &lt;attribuut&gt; van ALLE &lt;leden&gt; van de &lt;Onderwerp&gt; groter is dan &lt;grens&gt;" to a
     * {@link Condition.QuantifierFold} — a first-order quantification over the members of a linked instance
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
    private Condition quantifierFold(Condition c, ExpressieContext lhsCtx, ExpressieContext rhsCtx, Source source) {
        Expr.AttributeReference set = onlySetOperand(c);
        if (set == null) return c;
        Condition.Quantifier q = set.quantifier();
        if (q == null) return c;                             // bare "de": no set reading to quantify
        boolean counting = q == Condition.Quantifier.TEN_MINSTE || q == Condition.Quantifier.TEN_HOOGSTE
                || q == Condition.Quantifier.PRECIES;
        int threshold = counting ? set.threshold() : 0;
        if (counting && threshold < 1) {
            model.diagnostics.add(new Diagnostic(Severity.WARNING, "RGS-209", params("source", set.source()), set.source().line()));
            return c;
        }
        Objecttype elem = set.objectType();
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
                c instanceof Condition.Comparison v && v.right() == set ? rhsCtx : lhsCtx, elem, set);
        if (ds != null && ds.filter() == null) {
            model.diagnostics.add(new Diagnostic(Severity.WARNING,
                    "RGS-210", params("source", set.source(), "reason", ds.reason()), set.source().line()));
            return c;
        }
        Condition filter = ds == null ? null : ds.filter();
        // The matrix: the SAME condition, with the quantified operand replaced by an own, element-subject
        // read of the same attribute. The §8.2 qualifier stays ON it — per member, the whole-period test is
        // what each contributor must satisfy. No navigation role/path/range and no quantor survive: the
        // element IS the subject there, which is also why no set reading is left for the fence to find.
        Expr.AttributeReference own = new Expr.AttributeReference(set.attribute(), elem, set.subjectSource(), null,
                set.type(), set.source(), false, List.of(), List.of(), null);
        Condition matrix = elementRead(withOperand(c, set, own), own, elem,
                c instanceof Condition.Comparison v && v.left() == set ? rhsCtx : lhsCtx, source);
        return new Condition.QuantifierFold(q, threshold, elem, filter, matrix, sourceRole, List.of(), source);
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
    private Condition elementRead(Condition matrix, Expr own, Objecttype elem, ExpressieContext otherCtx, Source source) {
        if (!(matrix instanceof Condition.Comparison v) || otherCtx == null) return matrix;
        boolean ownLeft = v.left() == own;
        Expr other = ownLeft ? v.right() : v.left();
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
        Expr left = ownLeft ? own : again, right = ownLeft ? again : own;
        reportCompare(toCmpOp(v.op()), left.type(), right.type(), false, false, source.line(), null, null);
        return ExpressionBuilder.comparison(v.op(), left, right, v.periode(), v.source());
    }

    /** Whether {@code e} reads, on {@code elem} itself, a member {@code elem} does not have — the read a chain that
     *  starts at the element leaves when it is walked from another subject. Arithmetic, a rounding and a clamp are
     *  looked into; another shape is no such read. */
    private static boolean readsUnknownMemberOf(Expr e, Objecttype elem) {
        if (e instanceof Expr.AttributeReference a)
            return a.objectType() == elem && a.attribute() != null && !elem.hasMember(a.attribute());
        if (e instanceof Expr.Binary b) return readsUnknownMemberOf(b.left(), elem) || readsUnknownMemberOf(b.right(), elem);
        if (e instanceof Expr.Rounding a) return readsUnknownMemberOf(a.base(), elem);
        if (e instanceof Expr.Clamp b)
            return readsUnknownMemberOf(b.base(), elem) || readsUnknownMemberOf(b.minimum(), elem)
                    || readsUnknownMemberOf(b.maximum(), elem);
        return false;
    }

    /** The typing operator of a condition operator, {@link #toOperator} inverted. */
    private static Typing.CmpOp toCmpOp(Condition.Operator op) {
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
     * stayed an {@link Condition.Unresolved} marker. The quantifier is nevertheless written and the matrix
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
    private Condition quantifierCharacteristicCheck(Expr lhs, ExpressieContext lhsCtx, KaleNaamContext nameCtx, boolean withIndefiniteArticle,
                                    boolean hasVerb, Source source) {
        if (nameCtx == null || lhsCtx == null) return null;
        ExpressionBuilder.QuantifiedSubject g = builder.quantifiedSubject(lhsCtx);
        if (g == null) return null;                            // no member phrase written — nothing to say
        if (g.reason() != null) { reportFoldRefusal(g.subjectSource(), g.reason(), source); return null; }
        boolean counting = g.quantifier() == Condition.Quantifier.TEN_MINSTE
                || g.quantifier() == Condition.Quantifier.TEN_HOOGSTE || g.quantifier() == Condition.Quantifier.PRECIES;
        int threshold = counting ? g.threshold() : 0;
        if (counting && threshold < 1) {
            model.diagnostics.add(new Diagnostic(Severity.WARNING, "RGS-211", params("source", g.subjectSource()), source.line()));
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
                    source);
            return null;
        }
        // The anchor names the rule subject, and what the parse took after that name belongs to the predicate:
        // a negation, and the leading words of a multi-word name. From an anchor anchorTail cannot read, only a
        // trailing negation word is taken back.
        String suffix = builder.anchorTail(lhsCtx, anchorNames(builder.subject(), sourceRole));
        if (suffix == null) suffix = absorbedNegation(lhs);
        String naam = (suffix.isEmpty() ? "" : suffix + " ") + stripArticle(src(nameCtx));
        Condition test = elementCharacteristicCheck(g.element(), naam, withIndefiniteArticle, hasVerb, source);
        if (test == null) {
            reportFoldRefusal(g.subjectSource(),
                    "'" + naam.replaceFirst("^(?i)(niet|geen)\\s+", "") + "' is geen kenmerk van " + g.element().naam
                    + " — een §8.1.7-rolcontrole van het element is een andere gekwantificeerde vraag, en een "
                    + "kenmerk van het regelonderwerp op het element lezen zou iets anders beantwoorden dan de "
                    + "zin vraagt (§8.1.7/§8.1.8)", source);
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
                    "RGS-212", params("source", g.subjectSource(), "reason", ds.reason()),
                source.line()));
            return null;
        }
        return new Condition.QuantifierFold(g.quantifier(), threshold, g.element(), ds == null ? null : ds.filter(),
                test, sourceRole, List.of(), source);
    }

    /** The names the anchor of a quantified member phrase may call the rule subject by: the type names along
     *  its chain, and the declared name and plural of each role of the fold's relation that the subject's
     *  chain fills. {@link ExpressionBuilder#anchorTail} splits the anchor after the longest of them. */
    private List<String> anchorNames(Objecttype subject, RegelSpraakModel.RoleBindingRef range) {
        List<String> names = new ArrayList<>();
        List<Objecttype> chain = subject.chain();
        for (Objecttype t : chain) names.add(t.naam);
        RegelSpraakModel.FactType ft = model.feittypen.get(range.factType());
        if (ft != null)
            for (RegelSpraakModel.Role r : ft.rollen)
                if (chain.contains(model.objectTypeByName(r.objecttypeNaam()))) {
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
        return lhs instanceof Expr.AttributeReference a && a.source() != null
                && a.source().snippet().stripTrailing().toLowerCase().endsWith(" geen")
                && !a.attribute().toLowerCase().endsWith("geen") ? "geen" : "";
    }

    /** One decline of the membership fold, said out loud: which member phrase, on which axis, and what
     *  the sentence gets instead. Raised only where the phrase IS a quantified member phrase — a sentence
     *  that writes no quantifier gets no word about folds — and always a WARNING: the caller's marker is
     *  the honest wire form and the document stays emittable. */
    private void reportFoldRefusal(String subjectSource, String reason, Source source) {
        model.diagnostics.add(new Diagnostic(Severity.WARNING, "RGS-213", params("subject", subjectSource, "reason", reason), source.line()));
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
    private Condition elementCharacteristicCheck(Objecttype elem, String naam, boolean withIndefiniteArticle, boolean hasVerb, Source source) {
        if (withIndefiniteArticle)
            return hasVerb ? possessiveCharacteristicCheck(elem, naam, source) : nominalCharacteristicCheck(elem, naam, source);
        Condition kc = characteristicCheckCondition(elem, naam, null, hasVerb, source);
        if (kc != null) checkNegationKind(elem, kc, hasVerb);
        return kc;
    }

    /** The §8.2 whole-period qualifier of {@code c}, for the two condition forms that carry Expr operands
     *  (a {@link Condition.CharacteristicCheck} carries one too, but has no operand to quantify). Null otherwise. */
    private static Condition.WholePeriod wholePeriodOf(Condition c) {
        if (c instanceof Condition.Comparison v) return v.periode();
        if (c instanceof Condition.Predicate p)    return p.periode();
        return null;
    }

    /** The one operand of {@code c} that is a §13.4.2 SET reading, or null when there is none or more than
     *  one — two quantified sets in one comparison are two ranges, and which one the sentence quantifies
     *  over is not something this lowering may pick. */
    private static Expr.AttributeReference onlySetOperand(Condition c) {
        Expr[] operands = c instanceof Condition.Comparison v ? new Expr[]{v.left(), v.right()}
                : c instanceof Condition.Predicate p ? new Expr[]{p.operand()}
                : new Expr[0];
        Expr.AttributeReference found = null;
        int n = 0;
        for (Expr e : operands)
            if (e instanceof Expr.AttributeReference a && a.set()) { found = a; n++; }
        return n == 1 ? found : null;
    }

    /** {@code c} with the operand {@code oud} (matched by identity — it was just found there) replaced by
     *  {@code nieuw}; every other field, the §8.2 qualifier included, unchanged. */
    private static Condition withOperand(Condition c, Expr oud, Expr fresh) {
        if (c instanceof Condition.Comparison v)
            return new Condition.Comparison(v.op(), v.left() == oud ? fresh : v.left(),
                    v.right() == oud ? fresh : v.right(), v.periode(), v.source());
        if (c instanceof Condition.Predicate p)
            return new Condition.Predicate(p.category(), fresh, p.dayKind(), p.negation(), p.digits(),
                    p.periode(), p.source());
        return c;
    }

    /** Whether the onderwerp of a §8.2-qualified predicate is tijdsafhankelijk — a temporal attribuut on either side
     *  of a comparison, the operand of a leeg/gevuld/elfproef/numeriek predicate, or a temporal (own or to-one
     *  navigated) kenmerk. A rolcheck / unstructured form has no tijdsafhankelijk onderwerp. */
    private boolean subjectIsTemporal(Condition c) {
        if (c instanceof Condition.Comparison v) return exprTemporal(v.left()) || exprTemporal(v.right());
        if (c instanceof Condition.Predicate p)    return exprTemporal(p.operand());
        if (c instanceof Condition.CharacteristicCheck k)      return characteristicCheckTemporal(k);
        return false;
    }
    /** Whether an operand READS a TIJDSAFHANKELIJK value itself — a §13.3.6 tijdlijn attribuut (own or navigated) or a
     *  §3.8/§13.3.8 tijdlijn parameter. That is WF-043's reading of a time-dependent operand. §8.2 also admits such a
     *  read inside arithmetic ("zijn saldo plus 1"), an afronding, a begrenzing or a dimension cell, but the verifier
     *  refuses the qualifier there, so the frontend refuses it too, in the verifier's words, instead of writing a
     *  document the wire does not carry. */
    private static boolean exprTemporal(Expr e) {
        if (e instanceof Expr.AttributeReference a)
            return a.objectType() != null && a.objectType().granularityOf(a.attribute()) != null;
        if (e instanceof Expr.ParameterRef p)  return p.parameter().granulariteit() != null;
        return false;
    }
    private boolean characteristicCheckTemporal(Condition.CharacteristicCheck k) {
        Objecttype subject = builder.subject();
        Objecttype houder = k.navigation() == null ? subject
                : (subject != null ? model.roleTargetTypeToSubject(subject, k.navigation()) : null);
        return houder != null && houder.characteristicGranularityOf(k.characteristic()) != null;
    }

    private Condition conditionFromNonToplevelBare(Expr lhs, NonToplevelOperatorContext op, Source source) {
        Condition.WholePeriod periode = wholePeriod(op.gehelePeriodevergelijking());   // §8.2 whole-period check
        // A dangling absorbed "niet" silently INVERTS a comparison / leeg-gevuld -> marker it. The kenmerkcheck
        // DE-negates it and elfproef/numeriek RECOVER it (elfOperand), so only those two operators are guarded here.
        if (absorbedNegator(lhs) && (op.vergelijkingsTeken() != null || op.LEEG() != null || op.GEVULD() != null))
            return new Condition.Unresolved(source, REASON_NOT_ABSORBED);
        if (op.vergelijkingsTeken() != null && op.expressie() != null)
            return ExpressionBuilder.comparison(toOperator(cmpOf(op.vergelijkingsTeken())),
                    lhs, builder.build(op.expressie()), periode, source);
        if (op.LEEG() != null)   return new Condition.Predicate(Condition.PredicateKind.LEEG, lhs, periode, source);
        if (op.GEVULD() != null) return new Condition.Predicate(Condition.PredicateKind.GEVULD, lhs, periode, source);
        if (op.AAN_DE_ELFPROEF() != null) {
            ElfOperand e = elfOperand(op.NIET() != null, lhs);   // recover an absorbed vragende "niet"
            return new Condition.Predicate(Condition.PredicateKind.ELFPROEF, e.operand(), e.negation(), periode, source);
        }
        if (op.NUMERIEK_MET_EXACT() != null) {
            ElfOperand e = elfOperand(op.NIET() != null, lhs);
            return new Condition.Predicate(Condition.PredicateKind.NUMERIEK_MET_EXACT, e.operand(), e.negation(),
                    countDigits(op.NUMBER()), periode, source);
        }
        // §8.1.8.3 "is niet een <k>": the explicit NIET EEN form (no AAN_DE_ELFPROEF / NUMERIEK_MET_EXACT keyword
        // follows) is the NEGATED zelfstandig kenmerkcheck — bind the positive EEN-form (is -> ZELFSTANDIG), then flip.
        if (op.NIET() != null && op.EEN() != null) {
            Condition pos = characteristicCheckPredicate(lhs, op.kaleNaam(), true, false, periode, source);
            // Tabel 16 sanctions "is niet een <k>" only for a KENMERK; a rolcheck negates as "is/heeft geen <rol>"
            // (§8.1.7). Lower it the same way (inverse rolcheck), but surface the unsanctioned writing as a WARNING.
            if (pos instanceof Condition.RoleMembership || pos instanceof Condition.FarRoleMembership)
                model.diagnostics.add(new Diagnostic(Severity.WARNING, "RGS-214", params("source", source), 0));
            return negate(pos, source);
        }
        Condition characteristic = characteristicCheckPredicate(lhs, op.kaleNaam(), op.EEN() != null,
                op.HEEFT() != null || op.HEBBEN() != null, periode, source); // (is|heeft) een? kaleNaam: §8.1.8
        // else objecttype/navigation membership — reason-carrying, not reported here: see the sister in
        // conditionFromToplevelBare, whose caller may still fold this very marker into a Kwantorvouw.
        return characteristic != null ? characteristic : new Condition.Unresolved(source, REASON_NO_CHARACTERISTIC_NO_ROLE);
    }

    /** §8.1.7/§8.1.8: "&lt;subject&gt; is/heeft (een)? &lt;naam&gt;" -> a {@link Condition.CharacteristicCheck} (the
     *  §8.1.8 kenmerkcheck) or a {@link Condition.RoleMembership} (the §8.1.7 rolcheck), when the operand
     *  denotes the rule subject (or a to-one navigation from it). WITHOUT "een" the name is the subject's
     *  kenmerk (own or navigated). WITH "een" the grammar conflates a kenmerknaam and a rolnaam, so the
     *  resolver disambiguates by lookup, kenmerk-first (Tabel 16): a ZELFSTANDIG kenmerk (§8.1.8.3 "is een
     *  &lt;k&gt;") binds first, otherwise a role the subject fills is the §8.1.7 rolcheck. A bijvoeglijk /
     *  bezittelijk kenmerk is NOT written with "een", so the EEN-form never binds it. A name that resolves to
     *  neither stays Onopgelost (an honest marker), never a silently-true gate. */
    private Condition characteristicCheckPredicate(Expr lhs, KaleNaamContext nameCtx, boolean withIndefiniteArticle, boolean hasVerb,
                                      Condition.WholePeriod periode, Source source) {
        Condition c = characteristicCheckPredicateBare(lhs, nameCtx, withIndefiniteArticle, hasVerb, source);
        if (periode == null || c == null) return c;
        // §8.2 "is/heeft gedurende het gehele jaar een <kenmerk>" attaches the whole-period qualifier ONLY to a
        // §8.1.8 kenmerkcheck (its onderwerp is a tijdsafhankelijk kenmerk). Over any other membership form (a
        // §8.1.7 rolcheck) the qualifier has no defined meaning -> an honest marker, never silently dropped.
        return c instanceof Condition.CharacteristicCheck k
                ? new Condition.CharacteristicCheck(k.characteristic(), k.navigation(), k.negation(), k.owner(), periode, k.source())
                // GEEN GETUIGE: deze marker haalt het model nooit. Hij ontstaat alleen met een §8.2-
                // kwalificatie, en dan loopt hij altijd door checkWholePeriod, die een Onopgelost nooit
                // tijdsafhankelijk acht en hem dus vervangt door haar eigen marker MET ERROR. De reden
                // staat er omdat zij de weigering van DEZE route beschrijft; dat checkWholePeriod
                // vervolgens harder ingrijpt is haar besluit, niet dat van deze plek. Gemeten 2026-09-19.
                : new Condition.Unresolved(source,
                        "de controle 'gedurende de/het gehele <periode>' hoort bij een kenmerkcontrole over een "
                        + "tijdsafhankelijk onderwerp; over deze lidmaatschapsvorm (een §8.1.7-rolcontrole) heeft "
                        + "zij geen omschreven betekenis (§8.2)");
    }

    /** Flip the negatie of a resolved kenmerk-/rolcheck (the §8.1.8.3 "is niet een &lt;k&gt;" negation). The positive
     *  binding has negatie=false; flipping it yields the "== false" membership. A form that did not structure into a
     *  kenmerk/rolcheck (null, or an Onopgelost) becomes an honest marker, never a silently-true gate. */
    private static Condition negate(Condition c, Source source) {
        if (c instanceof Condition.CharacteristicCheck k)
            return new Condition.CharacteristicCheck(k.characteristic(), k.navigation(), !k.negation(), k.owner(), k.periode(), k.source());
        if (c instanceof Condition.RoleMembership r)
            return new Condition.RoleMembership(r.role(), r.otherToFiller(), r.source(), !r.negation());
        if (c instanceof Condition.FarRoleMembership vr)
            return new Condition.FarRoleMembership(vr.filler(), vr.backref(), !vr.negation(), vr.source());
        return new Condition.Unresolved(source,
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
    private void checkNegationKind(Objecttype houder, Condition kc, boolean hasVerb) {
        // a condition's warning stands at the line of the sentence that writes the check; it stood at 0, which points
        // an author at nothing in a document with many rules
        checkNegationKind(houder, kc, hasVerb, rowTableLine > 0 ? rowTableLine
                : kc == null || kc.source() == null ? 0 : kc.source().line());
    }

    /** As above, at {@code line}. A negated RESULT (dialect §4) passes its sentence's line, so its warning stands at
     *  the rule on the wire; a condition passes the line of its check, or the table's line in a row of a decision
     *  table. */
    private void checkNegationKind(Objecttype houder, Condition kc, boolean hasVerb, int line) {
        if (houder == null || !(kc instanceof Condition.CharacteristicCheck k) || !k.negation()) return;
        Objecttype.CharacteristicKind category = houder.characteristicKind(k.characteristic());
        if (category == null) return;
        boolean owns = category == Objecttype.CharacteristicKind.BEZITTELIJK;
        if (owns != hasVerb)
            model.diagnostics.add(new Diagnostic(Severity.WARNING,
                    "RGS-215", params("category", category.name().toLowerCase(), "characteristic", k.characteristic(), "verb", (hasVerb ? "heeft" : "is"), "required", (owns ? "heeft" : "is"), "source", k.source()), line));
    }

    private Condition characteristicCheckPredicateBare(Expr lhs, KaleNaamContext nameCtx, boolean withIndefiniteArticle, boolean hasVerb, Source source) {
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
                return new Condition.Predicate(Condition.PredicateKind.DAGSOORT, lhs, day, null, source);
            // Tabel 57: the written name IS a declared dagsoort but the operand is NOT datum-typed — a
            // resolver ERROR (was: a silent fall-through into the kenmerk/rol machinery ending as an
            // Onopgelost marker). An Onbekend operand carries no type claim and keeps the fall-through.
            if (day != null && !lhs.type().isUnknown())
                return dayKindOperandError(lhs, nameCtx, source);
        }
        // §8.1.5 + Tabel 16 "<datum> is geen <dagsoort>": the NEGATED dagsoortcontrole, kept on the §8.1.5
        // datum-value path — NEVER mis-folded into the §3.5 kenmerkcheck (a dagsoort is no kenmerk; §5.15-based
        // leegwaarde: a leeg/non-dagsoort datum IS "geen <dagsoort>" = WAAR). "geen" is a LOWER_ID (no GEEN token), so
        // where it lands depends on the operand shape: for a §6 date-arithmetic operand ("de startdatum plus 1 dag") it
        // stays on the operator's kaleNaam ("geen kerstdag"); for a plain AttributeReference the greedy onderwerp absorbs it
        // as a TRAILING word of the operand bron (like "niet"), leaving the bare dagsoort as the kaleNaam.
        if (!hasVerb && lhs != null) {   // §8.1.5/§5.15/Tabel 57: any datum/tijd operand (date part taken)
            boolean date = lhs.type().isDate(), typed = !lhs.type().isUnknown();
            String name0 = stripArticle(src(nameCtx));
            // (a) "geen <dagsoort>" left on the kaleNaam — a date-arithmetic / non-AttributeReference datum operand.
            if (name0 != null && name0.regionMatches(true, 0, "geen ", 0, 5)) {
                String day = dayKindName(stripArticle(name0.substring(5).trim()));
                if (day != null && date) return new Condition.Predicate(Condition.PredicateKind.DAGSOORT, lhs, day, true, 0, null, source);
                if (day != null && typed) return dayKindOperandError(lhs, nameCtx, source);   // Tabel 57 operand gate
            }
            // (b) "geen" absorbed as the operand's trailing word — a plain AttributeReference (mirror absorbedNegator),
            //     guarded against an attribuut literally named "… geen". The kaleNaam is then the bare dagsoort.
            if (lhs instanceof Expr.AttributeReference la && la.source() != null
                    && la.source().snippet().stripTrailing().toLowerCase().endsWith(" geen") && !la.attribute().toLowerCase().endsWith("geen")) {
                String day = dayKindName(name0);
                if (day != null && date) return new Condition.Predicate(Condition.PredicateKind.DAGSOORT, la, day, true, 0, null, source);
                if (day != null && typed) return dayKindOperandError(la, nameCtx, source);   // Tabel 57 operand gate
            }
        }
        Objecttype subject = builder.subject();
        if (subject == null) return null;
        String suffix = stripArticle(src(nameCtx));
        String head = subjectHead(lhs, subject);
        // not the subject itself -> a navigated kenmerk ("zijn reis is duurzaam", or the EEN-form "zijn reis is
        // een belaste reis"). The EEN-form binds only a ZELFSTANDIG kenmerk of the navigated type (the soort guard,
        // §8.1.8.3); a navigated role membership stays out of scope (a marker).
        if (head == null) return navigationCharacteristicCheck(lhs, suffix, hasVerb, withIndefiniteArticle, source);
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
            Condition kc = characteristicCheckCondition(subject, naam, null, hasVerb, source);
            if (kc != null) { checkNegationKind(subject, kc, hasVerb); return kc; }
            // a leftover "geen <name>" that is a role is the INVERSE rolcheck (the subject does not fill it);
            // otherwise the positive rolcheck.
            if (naam.regionMatches(true, 0, "geen ", 0, 5))
                return roleMembership(subject, naam.substring(5).trim(), source, true);
            return roleMembership(subject, naam, source);
        }
        // EEN-form: the grammar conflates a kenmerknaam and a rolnaam after "een", so disambiguate by lookup,
        // kenmerk-first (Tabel 16). The verb selects which kenmerk SOORT the EEN-form may bind: "is een <k>" is
        // §8.1.8.3 (a ZELFSTANDIG kenmerk — a bijvoeglijk kenmerk is written "is <k>" without "een"), while
        // "heeft een <k>" is §8.1.8.2 (a BEZITTELIJK kenmerk — the §7.1 verbatim "heeft een recht op
        // belastingvermindering"; the canonical "heeft het <k>" rides the non-EEN path above). Neither soort can
        // bind under the other verb. A name that is no such kenmerk falls through to the §8.1.7 rolcheck "is/heeft
        // een <rolnaam>" (incl. the far-side "heeft een <rol>" exists-join).
        Condition k = hasVerb ? possessiveCharacteristicCheck(subject, naam, source)
                                    : nominalCharacteristicCheck(subject, naam, source);
        return k != null ? k : roleMembership(subject, naam, source);
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

    /** §8.1.8.3 "&lt;subject&gt; is een &lt;zelfstandig kenmerk&gt;" -> a {@link Condition.CharacteristicCheck} ("k == true"),
     *  but ONLY for a {@link Objecttype.CharacteristicKind#ZELFSTANDIG} kenmerk: a bijvoeglijk kenmerk ("is &lt;k&gt;")
     *  and a bezittelijk one ("heeft het &lt;k&gt;") are not written with "een", so the EEN-form must not bind
     *  them (that mis-binding was the reason the EEN-form kenmerkcheck was deferred). Positive only — the "is
     *  geen" negation lexes into the kaleNaam with no "een" and reaches the non-EEN path (carded). Null when no
     *  ZELFSTANDIG kenmerk of {@code subject} bears the name, so the caller falls back to the §8.1.7 rolcheck. */
    private Condition nominalCharacteristicCheck(Objecttype subject, String naam, Source source) {
        String g = CharacteristicMatcher.find(subject, naam);
        return g != null && subject.characteristicKind(g) == Objecttype.CharacteristicKind.ZELFSTANDIG
                ? new Condition.CharacteristicCheck(g, source) : null;
    }

    /** §8.1.8.2 "&lt;subject&gt; heeft een &lt;bezittelijk kenmerk&gt;" -> a {@link Condition.CharacteristicCheck} ("k == true"),
     *  but ONLY for a {@link Objecttype.CharacteristicKind#BEZITTELIJK} kenmerk: the EEN-form heeft binds a possession of
     *  a kenmerk (the §7.1 "heeft een recht op belastingvermindering"), while a possession of a ROLE
     *  ("heeft een deelnemer") is no kenmerk and stays null for the caller's §8.1.7 far-side rolcheck. A
     *  bijvoeglijk/zelfstandig kenmerk uses "is", so the heeft EEN-form must not bind them. Null when no
     *  BEZITTELIJK kenmerk of {@code subject} bears the name. */
    private Condition possessiveCharacteristicCheck(Objecttype subject, String naam, Source source) {
        String g = CharacteristicMatcher.find(subject, naam);
        return g != null && subject.characteristicKind(g) == Objecttype.CharacteristicKind.BEZITTELIJK
                ? new Condition.CharacteristicCheck(g, source) : null;
    }

    /** §8.1.7 rolcheck: "&lt;subject&gt; is (een) &lt;rolnaam&gt;" — true when the subject fills that role.
     *  Resolves to a {@link Condition.RoleMembership} carrying the OTHER end of the feittype (so the
     *  transpiler can null-test the subject's reference to it). Null when no role the subject fills bears
     *  that name (-> Onopgelost marker, never a wrong gate). */
    private Condition roleMembership(Objecttype subject, String naam, Source source) {
        return roleMembership(subject, naam, source, false);
    }
    /** As above, with {@code negation} for the "is geen &lt;rol&gt;" form (the subject does NOT fill it). When the
     *  subject does not FILL the role (roleSiblingType null) but the role reaches a TO-MANY filler from it, this is
     *  the §8.1.7 FAR-side "&lt;subject&gt; heeft een &lt;rol&gt;": resolve to a {@link Condition.FarRoleMembership}
     *  carrying that filler and its back-reference field, which the transpiler lowers to an exists-join. */
    private Condition roleMembership(Objecttype subject, String naam, Source source, boolean negation) {
        Objecttype other = model.roleSiblingType(subject, naam);
        if (other != null) return new Condition.RoleMembership(naam, other, source, negation);
        Objecttype filler = model.roleTargetTypeToSubject(subject, naam);
        if (filler != null && model.toManyRole(subject, filler, naam)) {
            // The backref is the subject-side role of the SAME feittype as the named role — found by the written
            // role name (connectingRoleViaElementName), which disambiguates a filler that references the subject
            // through MORE THAN ONE feittype (e.g. a Persoon that fills both "deelnemer" and "bonusdeelnemer" of
            // a Ploeg): the by-pair connectingRole is then ambiguous and the far-side would wrongly stay a marker.
            // Fall back to the by-pair lookup for the single-feittype case (both give the same role there).
            String backref = model.connectingRoleViaElementName(subject, naam);
            if (backref == null) backref = model.connectingRole(filler, subject);
            if (backref != null) return new Condition.FarRoleMembership(filler, backref, negation, source);
        }
        return null;
    }

    /** Resolve {@code kenmerk} on {@code type} into a {@link Condition.CharacteristicCheck}, honouring a leading "niet"
     *  or "geen" as NEGATION — but each only after the WHOLE name fails, so a declared "niet-…" kenmerk
     *  still binds positively. {@code navigation} is the to-one navigation role (null for the subject's own).
     *  {@code hasVerb} is true when the predicate verb is hebben (§8.1.8: see the "geen" branch).
     *  Null when neither the whole name nor a de-negated form is a declared kenmerk of {@code type}. */
    private static Condition characteristicCheckCondition(Objecttype type, String characteristic, String navigation, boolean hasVerb, Source source) {
        // Match the written name VERBATIM first. A bijvoeglijk kenmerk may be declared WITH a leading article
        // as part of its name ("is de minimis kenmerk" -> stored "de minimis"): its grammar rule has no
        // separate bepaaldLidwoord? and naamWoordLower admits de/het, so the article is part of the stored
        // name — the false-invariant the first cut tripped on (a blind strip dropped it to a wrong/no match).
        Condition c = bindCharacteristicCheck(type, characteristic, navigation, hasVerb, source);
        if (c != null) return c;
        // Only if the verbatim name binds NOTHING, retry with a leading de/het/een stripped — recovering the
        // bezittelijk/noch-noch case where the article is the kenmerk's OWN (stored article-stripped) and the
        // greedy onderwerp parse left it on the reconstructed name ("het recht op duurzaamheidskorting").
        // Verbatim-first guarantees a kenmerk whose name really begins with an article binds itself, never a
        // shorter sibling.
        String stripped = stripArticle(characteristic);
        return stripped.equals(characteristic) ? null : bindCharacteristicCheck(type, stripped, navigation, hasVerb, source);
    }

    /** Bind one candidate name to a declared kenmerk of {@code type}, honouring a leading "niet" or "geen"
     *  as NEGATION — but each only after the WHOLE name fails, so a kenmerk declared starting with niet/geen
     *  still binds positively (never silently inverted). Null when nothing binds. */
    private static Condition bindCharacteristicCheck(Objecttype type, String naam, String navigation, boolean hasVerb, Source source) {
        String g = CharacteristicMatcher.find(type, naam);
        if (g != null) return new Condition.CharacteristicCheck(g, navigation, false, source);
        // §8.1.8.1 bijvoeglijk negation "is niet <k>" -> "== false" (verb-agnostic).
        if (naam.regionMatches(true, 0, "niet ", 0, 5)) {
            g = CharacteristicMatcher.find(type, naam.substring(5).trim());
            if (g != null) return new Condition.CharacteristicCheck(g, navigation, true, source);
        }
        // Negation with "geen" -> "== false": the §8.1.8.2 bezittelijk form "heeft geen <k>" AND the §8.1.8.3
        // noch-noch form "is geen <k>" (both lower to the same boolean negation, so the verb need not gate it;
        // a soort distinction governs only positive writing-form validation, not the negated lowering). "geen"
        // carries no token (only GEEN_VAN_DE), so it lexes as LOWER_ID and rides inside the kaleNaam into this
        // reconstructed name. Verbatim-first above means a kenmerk literally named "geen …" still binds positively.
        if (naam.regionMatches(true, 0, "geen ", 0, 5)) {
            g = CharacteristicMatcher.find(type, naam.substring(5).trim());
            if (g != null) return new Condition.CharacteristicCheck(g, navigation, true, source);
        }
        return null;
    }

    /** §8.1.8 navigated: "de &lt;rol&gt; van X is &lt;kenmerk&gt;" — the kenmerk is on the object the rol
     *  reaches, not the subject. Lowered when the rol is a TO-ONE navigation from the subject (so the
     *  property path {@code roleName.characteristic} reads one object); a TO-MANY navigation needs an exists/forall and
     *  stays a marker, as does an unresolvable rol or a kenmerk the navigated type does not declare. The rol
     *  is normalised to its declared name (a plural "de deelnemers" is the same role as "de deelnemer"), so
     *  the to-many test and the emitted reference field are the canonical one. With {@code withIndefiniteArticle} the verb selects
     *  the soort, exactly as the subject's own EEN-form ({@link #characteristicCheckPredicate}): "is een &lt;k&gt;" binds a
     *  §8.1.8.3 ZELFSTANDIG kenmerk of the navigated type, "heeft een &lt;k&gt;" a §8.1.8.2 BEZITTELIJK one — neither
     *  binds under the other verb, and a navigated role membership via "een" stays out of scope (null). */
    private Condition navigationCharacteristicCheck(Expr lhs, String suffix, boolean hasVerb, boolean withIndefiniteArticle, Source source) {
        if (!(lhs instanceof Expr.AttributeReference a) || a.objectType() == null) return null;
        Objecttype subject = a.objectType();                                       // the onderwerp the navigation is from
        Objecttype targetName = model.roleTargetTypeToSubject(subject, a.attribute());
        if (targetName == null) return null;
        String roleName = model.connectingRoleViaName(subject, targetName, a.attribute());     // canonical role / reference field
        if (roleName == null) roleName = a.attribute();
        if (model.toManyRole(subject, targetName, roleName)) return null;                     // to-many -> marker (needs exists/forall)
        // The greedy onderwerp parse can absorb a multi-word kenmerk's leading words — or a "niet" — into
        // the operand AFTER the navigated onderwerp. Reconstruct the full kenmerk = that absorbed tail + the
        // operator's kaleNaam, so a multi-word kenmerk binds (and a §8.1.8.1 "niet <kenmerk>" de-negates in
        // characteristicCheckCondition). navigationTail recovers the absorbed tail past the role/type anchor.
        String tail = navigationTail(a);
        String characteristic = tail.isEmpty() ? suffix : tail + " " + suffix;
        if (withIndefiniteArticle) {
            // §8.1.8.2/.3 navigated EEN-form: the verb selects the soort (is -> ZELFSTANDIG, heeft -> BEZITTELIJK),
            // the mirror of the subject's own EEN-form; neither soort binds under the other verb, and a navigated
            // role membership via "een" stays out of scope -> all an honest marker, never a wrong gate.
            String g = CharacteristicMatcher.find(targetName, characteristic);
            Objecttype.CharacteristicKind required = hasVerb
                    ? Objecttype.CharacteristicKind.BEZITTELIJK : Objecttype.CharacteristicKind.ZELFSTANDIG;
            return g != null && targetName.characteristicKind(g) == required
                    ? new Condition.CharacteristicCheck(g, roleName, false, source) : null;
        }
        Condition kc = characteristicCheckCondition(targetName, characteristic, roleName, hasVerb, source);
        checkNegationKind(targetName, kc, hasVerb);
        return kc;
    }

    /** The words the greedy operand parse absorbed AFTER the navigated "de &lt;rol&gt; van &lt;onderwerp&gt;"
     *  (the leading words of a multi-word kenmerk, or a "niet") — empty for a clean navigation. Found as the
     *  operand text following the onderwerp's object-type name. */
    private static String navigationTail(Expr.AttributeReference a) {
        String b = a.source() == null ? "" : a.source().snippet();
        // Anchor on the onderwerp TYPE name ("de <rol> van <Type> <kenmerkwoorden>"); for a POSSESSIVE
        // ("zijn <rol> <kenmerkwoorden>") there is no type name, so fall back to the ROLE name. Take the text
        // after the anchor's LAST whole-word occurrence (so a kenmerk word merely containing it does not
        // mis-split). The role fallback recovers a "niet"/"geen" the greedy possessive onderwerp absorbed
        // ("zijn reis niet" -> "niet"), so a navigated §8.1.8 negation de-negates instead of markering.
        for (String anchor : new String[]{a.objectType() != null ? a.objectType().naam : null, a.attribute()}) {
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
        if (lhs instanceof Expr.Constant c)
            return (targetRole == null && c.naam().equalsIgnoreCase("hij")) ? "" : null;
        String b = lhs.source() == null ? "" : stripArticle(lhs.source().snippet());
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

    private Condition.Quantifier quantifierOf(VoorwaardeKwantificatieContext k) {
        if (k == null) return Condition.Quantifier.DE;
        if (k.ALLE() != null)        return Condition.Quantifier.ALLE;
        if (k.GEEN_VAN_DE() != null) return Condition.Quantifier.GEEN;
        if (k.TEN_MINSTE() != null)  return Condition.Quantifier.TEN_MINSTE;
        if (k.TEN_HOOGSTE() != null) return Condition.Quantifier.TEN_HOOGSTE;
        if (k.PRECIES() != null)     return Condition.Quantifier.PRECIES;
        return Condition.Quantifier.DE;
    }

    static Condition.Operator toOperator(Typing.CmpOp op) {
        if (op == null) return Condition.Operator.GELIJK;
        return switch (op) {
            case KLEINER -> Condition.Operator.KLEINER;
            case KLEINER_OF_GELIJK -> Condition.Operator.KLEINER_OF_GELIJK;
            case GROTER -> Condition.Operator.GROTER;
            case GROTER_OF_GELIJK -> Condition.Operator.GROTER_OF_GELIJK;
            case GELIJK -> Condition.Operator.GELIJK;
            case ONGELIJK -> Condition.Operator.ONGELIJK;
            case EERDER -> Condition.Operator.EERDER;
            case EERDER_OF_GELIJK -> Condition.Operator.EERDER_OF_GELIJK;
            case LATER -> Condition.Operator.LATER;
            case LATER_OF_GELIJK -> Condition.Operator.LATER_OF_GELIJK;
        };
    }

    private static String src(ParserRuleContext c) { return DefinitionResolver.src(c); }
    private static dev.uchar.regelspraak.ir.Source source(ParserRuleContext c) { return DefinitionResolver.source(c); }

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
        if (rLit && l.isNumber() && l.unitOf() != null) r = withUnit(r, l.unitOf());
        else if (lLit && r.isNumber() && r.unitOf() != null) l = withUnit(l, r.unitOf());
        var res = Typing.compare(op, l, r, lLit, rLit);   // Onbekend operands -> ok, so no false positive
        if (!res.ok()) model.diagnostics.add(res.failure().frame("condition").at(Severity.ERROR, line));
    }

    /** A bare numeric literal ("0", "18", "3,5") with NO trailing eenheid — getText collapses whitespace, so a
     *  unit-qualified literal ("18 jr") becomes "18jr" and is correctly excluded. Side-effect-free (no re-bouw). */
    private static boolean bareNumberLiteral(ExpressieContext e) {
        return e != null && e.getText().matches("-?\\d+([.,]\\d+)?");
    }

    /** Re-state a numeriek datatype with {@code eenheid} (for promoting a dimensionless literal to a sibling's unit). */
    private static Datatype withUnit(Datatype t, dev.uchar.regelspraak.model.Unit eenheid) {
        return t instanceof Datatype.Numeriek n ? new Datatype.Numeriek(n.geheel(), n.decimalen(), n.teken(), eenheid) : t;
    }
    private void reportPredicate(Typing.UnairePred p, Datatype operand, int line) {
        var res = Typing.predicaat(p, operand);
        if (!res.ok()) model.diagnostics.add(res.failure().frame("condition").at(Severity.ERROR, line));
    }

    /** Tabel 57 (§5.15): the written name resolved to a DECLARED dagsoort but the operand is not datum-typed —
     *  the Typering DAGSOORT ERROR (this is its only live call site on the condition path), and the conditie
     *  stays an Onopgelost marker so neither back-end builds a dagsoort check over a non-date. */
    private Condition dayKindOperandError(Expr lhs, KaleNaamContext nameCtx, Source source) {
        reportPredicate(Typing.UnairePred.DAGSOORT, lhs.type(), nameCtx.getStart().getLine());
        return new Condition.Unresolved(source, null);   // reportPredicate's ERROR is this marker's diagnosis
    }

    static Typing.CmpOp cmpOf(VergelijkingsTekenContext c) {
        return cmpOf(c.ONGELIJK() != null, c.GROTER() != null, c.KLEINER() != null,
                     c.LATER() != null, c.EERDER() != null, c.GELIJK() != null, c.OF() != null);
    }
    static Typing.CmpOp cmpOf(VergelijkingsTekenGesplitstContext c) {
        return cmpOf(c.ONGELIJK() != null, c.GROTER() != null, c.KLEINER() != null,
                     c.LATER() != null, c.EERDER() != null, c.GELIJK() != null, c.OF() != null);
    }
    static Typing.CmpOp cmpOf(boolean unequal, boolean gr, boolean kl, boolean la, boolean ee, boolean ge, boolean of) {
        if (unequal) return Typing.CmpOp.ONGELIJK;
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
