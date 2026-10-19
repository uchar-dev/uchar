package dev.uchar.regelspraak.resolver;

import dev.uchar.regelspraak.core.TimeCalculation;
import dev.uchar.regelspraak.message.Msg;
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
import static dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic.params;

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
        refuseDuplicateNames(doc);
        for (DefinitieContext d : definitions(doc)) {        // pass 1: domains + units + timelines
            if (d.domeinDefinition() != null) domain(d.domeinDefinition());
            if (d.eenheidsysteemDefinition() != null) unitSystem(d.eenheidsysteemDefinition());
            // a named timeline must be registered before pass 2, so an attribute's "voor elke <tijdlijn>"
            // reference can adopt its granularity regardless of declaration order (§13.3.6.9). It reads only
            // its own tokens, so it has no dependency on objecttypes/feittypes processed in pass 2.
            if (d.tijdlijnDefinitie() != null) timeline(d.tijdlijnDefinitie());
        }
        for (DefinitieContext d : definitions(doc)) {        // pass 2: everything that references them
            if (d.objectTypeDefinition() != null) objectType(d.objectTypeDefinition());
            if (d.objectTypeExtensie() != null) extension(d.objectTypeExtensie());
            if (d.parameterDefinition() != null) parameter(d.parameterDefinition());
            if (d.feitTypeDefinition() != null) factType(d.feitTypeDefinition());
            if (d.dagsoortDefinition() != null) dayKind(d.dagsoortDefinition());
            if (d.dimensieDefinition() != null) dimension(d.dimensieDefinition());
        }
        // the 'is een' clauses bind only now: resolution is document-wide and ORDER-FREE, so a
        // forward reference is legal and only an undeclared name refuses (SUBTYPING §9.2)
        bindSuperTypes();
        // a member an extension declares again is judged only now: the head may follow the extension, and a role it
        // names may be declared after it
        refuseRedeclarations();
        // a role's filler binds only now, for the same reason: a fact type may name a type declared after it
        refuseUndeclaredFillers();
        for (RegelContext r : allRules(doc)) {
            model.rules.add(new Rule(src(r.regelNaam()), "regel"));
        }
        // §4 (PRIORITIZED-DEFAULTS-EVOLUTIE) the ordered 'Uitzondering op' exception blocks carry their own name
        // too — a Regel stub in document position, so each defaults-producing rule appears in `rules` exactly as
        // the flat rules and the group members do (their RESOLVED form lives in the defaults group, not
        // resolvedRules).
        for (UitzonderingRegelContext u : allExceptions(doc)) {
            model.rules.add(new Rule(src(u.regelNaam()), "regel"));
        }
        return model;
    }

    /** Every §13.4.2 regel of the document — the top-level ones AND the members of a §9.10 {@link RegelGroepContext}
     *  (a DOCUMENTED EXTENSION; see grammar/DIVERGENCES.md). A grouped rule resolves EXACTLY as an ungrouped
     *  one — the group is namespacing only — so the whole rule pipeline walks this flattened list. Order is
     *  preserved (a group's members appear where the group's header sits). */
    static List<RegelContext> allRules(RegelSpraakDocumentContext doc) {
        List<RegelContext> result = new ArrayList<>();
        for (ParseTree kind : doc.children == null ? List.<ParseTree>of() : doc.children) {
            if (kind instanceof RegelContext r) {
                result.add(r);
            } else if (kind instanceof RegelGroepContext g) {
                for (GroepLidContext member : g.groepLid())
                    if (member.regel() != null) result.add(member.regel());
            } else if (kind instanceof UitzonderingGroepContext ug) {
                // §4 (PRIORITIZED-DEFAULTS-EVOLUTIE) an unordered exception group's members are ordinary regels: they
                // resolve through the WHOLE rule pipeline (type-checks included), and the defaults pass then LIFTS each
                // into sibling declared-unordered PrioritizedDefaults (removing it from resolvedRules — the defaults
                // group is the writer).
                result.addAll(ug.regel());
            }
        }
        return result;
    }

    /** §4 (PRIORITIZED-DEFAULTS-EVOLUTIE) every ordered 'Uitzondering op &lt;basis&gt;: &lt;naam&gt;' block of the
     *  document, in document order. Each resolves like a §13.4.2 regel (its ruleName + regelVersie+ body) and is
     *  then lifted, with its named base rule, into one
     *  {@link dev.uchar.regelspraak.ir.PrioritizedDefaults}. */
    static List<UitzonderingRegelContext> allExceptions(RegelSpraakDocumentContext doc) {
        List<UitzonderingRegelContext> result = new ArrayList<>();
        for (ParseTree kind : doc.children == null ? List.<ParseTree>of() : doc.children)
            if (kind instanceof UitzonderingRegelContext u) result.add(u);
        return result;
    }

    /** §4 (PRIORITIZED-DEFAULTS-EVOLUTIE) every unordered exception group of the document, in document order. */
    static List<UitzonderingGroepContext> allExceptionGroups(RegelSpraakDocumentContext doc) {
        List<UitzonderingGroepContext> result = new ArrayList<>();
        for (ParseTree kind : doc.children == null ? List.<ParseTree>of() : doc.children)
            if (kind instanceof UitzonderingGroepContext g) result.add(g);
        return result;
    }

    /** Every §12 beslistabel of the document — top-level ones AND the members of a §9.10 {@link RegelGroepContext}
     *  (a grouped beslistabel is namespacing only; it desugars to grouped regels exactly like an ungrouped one). */
    static List<BeslistabelContext> allDecisionTables(RegelSpraakDocumentContext doc) {
        List<BeslistabelContext> result = new ArrayList<>();
        for (ParseTree kind : doc.children == null ? List.<ParseTree>of() : doc.children) {
            if (kind instanceof BeslistabelContext b) result.add(b);
            else if (kind instanceof RegelGroepContext g)
                for (GroepLidContext member : g.groepLid())
                    if (member.beslistabel() != null) result.add(member.beslistabel());
        }
        return result;
    }

    private static List<DefinitieContext> definitions(RegelSpraakDocumentContext doc) {
        return doc.definitie();
    }

    // ---------------- §13.3.1 object types ----------------
    private void objectType(ObjectTypeDefinitionContext ctx) {
        ObjectType ot = model.objectTypes.computeIfAbsent(src(ctx.objectTypeNaam()), ObjectType::new);
        if (ctx.BEZIELD() != null) ot.animate = true;
        if (ctx.meervoudsvorm() != null) ot.plural = src(ctx.meervoudsvorm()); // §13.2.29, for §8.1.6 plural refs
        supertypeClauses(ot, ctx.objectTypeSupertype());
        for (ObjectTypeMemberContext m : ctx.objectTypeMember()) member(ot, m, ctx, false);
        membersAfterHead(ot, ctx, ctx.objectTypeMember());
    }

    // ---------------- the uchar 'is een' dialect extension (IR-SPEC ch. 2.2) ----------------
    /** The parent names an object type DECLARED, in declaration order, keyed by the type's own name;
     *  plus the source line of its first clause. Held rather than bound: the reference is order-free,
     *  so nothing can be resolved until every declaration has been read ({@link #bindSuperTypes}). */
    private final java.util.Map<String, List<String>> supertypeNames = new java.util.LinkedHashMap<>();
    private final java.util.Map<String, Integer> supertypeLine = new java.util.LinkedHashMap<>();

    /** The source line of each member DECLARATION, keyed by object type and lowercased member name — the
     *  line WF-105 must cite, because the edit that resolves a shadow is at the redeclaration and not at
     *  the {@code is een} clause. Filled for every member; only read for a type that has a supertype. */
    private final java.util.Map<ObjectType, java.util.Map<String, Integer>> memberLine = new java.util.IdentityHashMap<>();

    private void recordMemberLine(ObjectType ot, ObjectTypeMemberContext m) {
        if (m.koptekst() != null) return;
        String name = m.attribuutSpecificatie() != null ? name(m.attribuutSpecificatie().lowerNaam())
                                                        : characteristicName(m.kenmerkSpecificatie());
        memberLine.computeIfAbsent(ot, k -> new java.util.LinkedHashMap<>())
                .putIfAbsent(name.toLowerCase(), m.getStart().getLine());
    }

    /** Record the 'is een &lt;Objecttypenaam&gt;' clause(s) of one head. Every name the head writes is
     *  kept — including the ones WF-106 forbids — so the refusal can NAME them instead of reducing to
     *  a syntax error. */
    private void supertypeClauses(ObjectType ot, List<ObjectTypeSupertypeContext> clauses) {
        if (clauses.isEmpty()) return;
        List<String> parents = supertypeNames.computeIfAbsent(ot.name, k -> new ArrayList<>());
        for (ObjectTypeSupertypeContext c : clauses)
            for (ObjectTypeNaamContext n : c.objectTypeNaam()) parents.add(src(n));
        supertypeLine.putIfAbsent(ot.name, clauses.get(0).getStart().getLine());
    }

    /** Bind every declared supertype reference and refuse the shapes the chain cannot carry — the
     *  frontend's share of WF-103/104/105/106. It refuses BEFORE the verifier does because it has the
     *  chain at hand, and WF-106 has nowhere else to be refused at all: a bare-string {@code supertype}
     *  cannot WRITE a second parent, so that rule bounds the language above the wire (IR-SPEC ch. 2.2).
     *  A refused reference stays UNBOUND — a half-built chain would make every later reader disagree
     *  with the document. */
    private void bindSuperTypes() {
        for (var e : supertypeNames.entrySet()) {
            ObjectType ot = model.objectTypes.get(e.getKey());
            if (ot == null) continue;
            int line = supertypeLine.getOrDefault(e.getKey(), 0);
            // Distinct by name, ci: a head repeating ONE parent is redundant, not a second parent;
            // two DIFFERENT names are the refusal, however they are spelled (two clauses, or "A en B").
            List<String> parents = new ArrayList<>();
            for (String n : e.getValue())
                if (parents.stream().noneMatch(o -> o.equalsIgnoreCase(n))) parents.add(n);
            if (parents.size() > 1) {
                error(
                        "RGS-002", params("objectType", ot.name, "names", String.join(", ", parents)), line);
                continue;
            }
            String name = parents.get(0);
            if (name.equalsIgnoreCase(ot.name)) {
                error(
                        "RGS-003", params("objectType", ot.name), line);
                continue;
            }
            ObjectType parent = model.objectTypeByName(name);
            if (parent == null) {
                String category = nameKind(name);
                Msg wf103 = Msg.frontend("RGS-004.wf103");
                if (category == null)
                    error("RGS-004", params("objectType", ot.name, "supertype", name, "rationale", wf103), line);
                else
                    error("RGS-005", params("objectType", ot.name, "supertype", name, "category", category,
                            "rationale", wf103), line);
                continue;
            }
            ot.supertype = parent;
        }
        refuseSupertypeCycles();
        refuseShadowedMembers();
    }

    /** What the document DOES declare {@code naam} as, when it is not an object type — so the refusal can
     *  say "een domein, geen objecttype" instead of sending the author hunting for a typo. Null when the
     *  name is not declared at all. */
    private String nameKind(String name) {
        if (containsName(model.domains.keySet(), name)) return "domein";
        if (containsName(model.parameters.keySet(), name)) return "parameter";
        if (containsName(model.factTypes.keySet(), name)) return "feittype";
        if (containsName(model.dimensions.keySet(), name)) return "dimensie";
        if (containsName(model.unitSystems.keySet(), name)) return "eenheidsysteem";
        if (containsName(model.dayKinds, name)) return "dagsoort";
        return null;
    }

    private static boolean containsName(java.util.Collection<String> names, String name) {
        return names.stream().anyMatch(n -> n.equalsIgnoreCase(name));
    }

    /** WF-104 for cycles of two steps or more (the depth-1 self-edge is refused by name above, so it never
     *  reaches here). Colour DFS with a named cycle path — the uchar verifier's own shape, because the
     *  failure is the same: member resolution has no root to stop at. The back edge is CUT after it is
     *  reported, so every later pass over the model terminates on a document the resolver has already
     *  rejected. */
    private void refuseSupertypeCycles() {
        java.util.Map<ObjectType, Integer> color = new java.util.IdentityHashMap<>();
        for (ObjectType ot : model.objectTypes.values()) findCycle(ot, color, new ArrayList<>());
    }

    private void findCycle(ObjectType t, java.util.Map<ObjectType, Integer> color, List<ObjectType> path) {
        Integer k = color.get(t);
        if (k != null && k == 2) return;
        if (k != null && k == 1) {                       // grey: a back edge onto the current path
            List<String> names = new ArrayList<>();
            for (ObjectType x : path.subList(path.indexOf(t), path.size())) names.add(x.name);
            names.add(t.name);
            ObjectType back = path.get(path.size() - 1);
            error(
                    "RGS-006", params("objectType", back.name, "names", String.join(" -> ", names)),
                    supertypeLine.getOrDefault(back.name, 0));   // the clause that closes the cycle
            back.supertype = null;
            return;
        }
        color.put(t, 1);
        path.add(t);
        if (t.supertype != null) findCycle(t.supertype, color, path);
        path.remove(path.size() - 1);
        color.put(t, 2);
    }

    /** WF-105: no attribute or characteristic name is declared twice along one chain (ci) — WF-002's
     *  per-kind uniqueness lifted from the declaration to the chain. One instance would otherwise carry two
     *  declarations of one name, and which one a reader finds decides that name's datatype, dimensions and
     *  timeline facet on every read of it. NOT refused, and deliberately: a SIBLING collision — two
     *  incomparable subtypes declaring one name — no instance is of two incomparable types, so no read is
     *  ambiguous. */
    private void refuseShadowedMembers() {
        for (ObjectType ot : model.objectTypes.values()) {
            if (ot.supertype == null) continue;
            List<ObjectType> ancestors = ot.chain().subList(1, ot.chain().size());
            for (String a : ot.attributes.keySet()) reportShadow(ot, ancestors, Msg.frontend("PART.member-kind.attribute"), a, true);
            for (String k : ot.characteristics) reportShadow(ot, ancestors, Msg.frontend("PART.member-kind.characteristic-kind"), k, false);
        }
    }

    private void reportShadow(ObjectType ot, List<ObjectType> ancestors, Msg category, String name, boolean attribute) {
        for (ObjectType v : ancestors) {
            boolean collides = attribute ? containsName(v.attributes.keySet(), name) : containsName(v.characteristics, name);
            if (!collides) continue;
            int line = memberLine.getOrDefault(ot, java.util.Map.of()).getOrDefault(name.toLowerCase(), 0);
            error(
                    "RGS-007", params("objectType", ot.name, "category", category, "name", name, "sourceLine", line, "ancestor", v.name, "ancestorLine", memberLine.getOrDefault(v, java.util.Map.of()).getOrDefault(name.toLowerCase(), 0)), line);
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
     * extension adds members to a declared type and is no declaration of the name (refuseRedeclarations); timelines are
     * WF-015's. A definition the parser recovered inside is left alone: its syntax error is the one to act on.
     */
    private void refuseDuplicateNames(RegelSpraakDocumentContext doc) {
        java.util.Map<String, String[]> earlier = new java.util.HashMap<>();   // namespace + name identity -> {name, line}
        for (DefinitieContext d : definitions(doc)) {
            String[] category = null;   // {how the refusal names the kind, the verifier's namespace, the name}
            ParserRuleContext ctx = null;
            if (d.objectTypeDefinition() != null) {
                ctx = d.objectTypeDefinition();
                category = new String[] {"objecttype", "objectTypes", src(d.objectTypeDefinition().objectTypeNaam())};
            } else if (d.domeinDefinition() != null) {
                ctx = d.domeinDefinition();
                category = new String[] {"domein", "domains", src(d.domeinDefinition().domeinNaam())};
            } else if (d.parameterDefinition() != null) {
                ctx = d.parameterDefinition();
                category = new String[] {"parameter", "parameters", src(d.parameterDefinition().parameterNaam())};
            } else if (d.eenheidsysteemDefinition() != null) {
                ctx = d.eenheidsysteemDefinition();
                category = new String[] {"eenheidsysteem", "unitSystems",
                        src(d.eenheidsysteemDefinition().eenheidsysteemNaam())};
            } else if (d.feitTypeDefinition() != null) {
                ctx = d.feitTypeDefinition();
                category = new String[] {"feittype", "factTypes", src(d.feitTypeDefinition().feitTypeNaam())};
            } else if (d.dimensieDefinition() != null) {
                ctx = d.dimensieDefinition();
                category = new String[] {"dimensie", "dimensions", src(d.dimensieDefinition().dimensieNaam())};
            } else if (d.dagsoortDefinition() != null) {
                ctx = d.dagsoortDefinition();
                category = new String[] {"dagsoort", "dayKinds", name(d.dagsoortDefinition().lowerNaam())};
            }
            if (category == null || recovered(ctx)) continue;
            int line = ctx.getStart().getLine();
            String[] first = earlier.putIfAbsent(category[1] + "\u0000" + dev.uchar.regelspraak.emit.IrEmitter.norm(category[2]),
                    new String[] {category[2], String.valueOf(line)});
            if (first == null) continue;
            error(
                    "RGS-008", params("kind", category[0], "namespace", category[1], "name", category[2], "earlierName", first[0], "earlierLine", first[1]), line);
        }
    }

    private void error(String code, java.util.Map<String, Object> params, int line) {
        model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.ERROR, code, params, line));
    }

    // ---------------- a member an extension declares again ----------------
    /** One member declaration, in a head or in an extension, in document order. */
    private record Member(ObjectType type, boolean attribute, String name, boolean extension,
                       ObjectTypeMemberContext ctx, ParserRuleContext definition) {
        int line() { return ctx.getStart().getLine(); }
        /** A declaration of the same member: one type, one kind, one name (ci, WF-002). */
        boolean same(Member other) {
            return type == other.type && attribute == other.attribute && name.equalsIgnoreCase(other.name);
        }
    }
    private final List<Member> members = new ArrayList<>();

    /** Every member an extension declares that its type already carries, and whether that repeat is admitted. The
     *  model holds the declaration the type carries, so the designations on a repeat are not read a second time
     *  ({@link Designations}), and the opgave pass reads the marker of an admitted repeat only ({@link RuleChecker}). */
    private final java.util.Map<ObjectTypeMemberContext, Boolean> repetitions = new java.util.IdentityHashMap<>();

    java.util.Map<ObjectTypeMemberContext, Boolean> repetitions() { return repetitions; }

    /** The source line of each role, per fact type, for the refusal of a role an extension declares again. */
    private final java.util.Map<FactType, java.util.Map<String, Integer>> roleLines = new java.util.IdentityHashMap<>();

    /** An extension adds members to a type and replaces none (grammar/DIVERGENCES.md, §5 and §7). A member it declares
     *  that the type already carries, in its head or, where no head declares it, in an earlier extension, is admitted
     *  as that declaration written again word for word, the opgave marker aside; the repeat may add the marker once.
     *  Any other repeat is an ERROR at the repeat's line that names both lines: until this check the repeat replaced
     *  the declaration without a word, also with another datatype, and a repeated characteristic went to the wire
     *  twice. The words decide, so layout and the closing ';' do not count. The literal repeat is admitted because it
     *  changes nothing, and the casus of the repository's parity zone writes three of them. Along the
     *  inheritance line WF-105 refuses every repeat, literal or not ({@link #refuseShadowedMembers}). A definition the
     *  parser recovered inside is left alone: its syntax error is the one to act on. */
    private void refuseRedeclarations() {
        for (Member d : members) {
            if (!d.extension() || recovered(d.definition())) continue;
            Member b = existing(d);
            if (b == null || recovered(b.definition())) continue;
            if (b != d) judgeRepetition(d, b);
            else if (d.attribute()) refuseRoleRedeclaration(d);
        }
    }

    /** The declaration of d's member the type carries: its head's, else the first in document order. */
    private Member existing(Member d) {
        Member first = null;
        for (Member l : members) {
            if (!l.same(d)) continue;
            if (!l.extension()) return l;
            if (first == null) first = l;
        }
        return first;
    }

    private void judgeRepetition(Member d, Member b) {
        Msg reason = null;
        if (!words(d.ctx()).equals(words(b.ctx())))
            reason = Msg.frontend("RGS-009.not-literal", "declaration", text(d.ctx()), "earlier", text(b.ctx()));
        else if (RuleChecker.markedMember(d.ctx())) {
            Member marked = earlierMarking(d, b);
            if (marked != null)
                reason = Msg.frontend("RGS-009.already-marked", "line", marked.line());
        }
        repetitions.put(d.ctx(), reason == null);
        if (reason == null) return;
        error(
                "RGS-009", params("kind", d.attribute() ? "attribuut" : "kenmerk",
                        "kindPhrase", Msg.frontend(d.attribute() ? "RGS-009.attribute-phrase" : "RGS-009.characteristic-phrase"),
                        "name", d.name(), "objectType", d.type().name, "earlierLine", b.line(), "sourceLine", d.line(), "reason", reason),
                d.line());
    }

    /** The declaration of d's member that carries the opgave marker already: the one the type carries, or an admitted
     *  repeat before d that added it; null when none does. */
    private Member earlierMarking(Member d, Member b) {
        if (RuleChecker.markedMember(b.ctx())) return b;
        for (Member l : members) {
            if (l == d) return null;
            if (l.same(d) && Boolean.TRUE.equals(repetitions.get(l.ctx())) && RuleChecker.markedMember(l.ctx()))
                return l;
        }
        return null;
    }

    /** The words of a member declaration without the opgave marker and the closing ';': what a repeat writes again. */
    private static List<String> words(ParseTree t) {
        List<String> result = new ArrayList<>();
        if (t instanceof TerminalNode n) {
            int category = n.getSymbol().getType();
            if (category != RegelSpraak.MAG_OPGEGEVEN_WORDEN && category != RegelSpraak.SEMICOLON) result.add(n.getText());
        } else {
            for (int i = 0; i < t.getChildCount(); i++) result.addAll(words(t.getChild(i)));
        }
        return result;
    }

    /** A member declaration as written, without its closing ';'. */
    private static String text(ObjectTypeMemberContext m) {
        return src(m).replaceFirst("\\s*;$", "");
    }

    /** An extension declares attributes and characteristics, so the grammar reads a role line in it ('de vinder
     *  Jutter;') as an attribute whose type is a domain. Where that name is no domain but a declared object type, and
     *  the member's name is a role of a fact type the type or a supertype of it takes part in, the line declares that
     *  role again: an ERROR naming both lines. Until this check the line was refused as an undeclared domain on line
     *  0; the member leaves the model, so that refusal does not come beside this one. */
    private void refuseRoleRedeclaration(Member d) {
        AttribuutSpecificatieContext a = d.ctx().attribuutSpecificatie();
        if (a.datatype() != null || a.domeinNaam() == null) return;
        String fillerType = src(a.domeinNaam());
        if (containsName(model.domains.keySet(), fillerType) || model.objectTypeByName(fillerType) == null) return;
        for (FactType ft : model.factTypes.values()) {
            if (ft.roles.stream().noneMatch(r -> d.type().isA(model.objectTypeByName(r.objectTypeName())))) continue;
            for (Role r : ft.roles) {
                if (!r.name().equalsIgnoreCase(d.name())) continue;
                repetitions.put(d.ctx(), false);
                forgetAttribute(d.type(), d.name());
                error(
                        "RGS-010", params("objectType", d.type().name, "name", d.name(), "fillerType", fillerType, "role", r.name(), "factType", ft.name, "roleLine", roleLines.getOrDefault(ft, java.util.Map.of()).getOrDefault(r.name(), 0), "sourceLine", d.line()),
                        d.line());
                return;
            }
        }
    }

    /** Take an attribute back out of a type: the attribute and every side map keyed by its name. */
    private static void forgetAttribute(ObjectType ot, String name) {
        ot.attributes.remove(name);
        ot.attributePlural.remove(name);
        ot.attributeDimensions.remove(name);
        ot.attributeGranularity.remove(name);
        ot.attributeTimelineCount.remove(name);
        ot.attributeTimelineName.remove(name);
        ot.attributeFixedStartPoint.remove(name);
    }

    private void extension(ObjectTypeExtensieContext ctx) {
        // an extensie adds members to an existing (or new) object type and replaces none (refuseRedeclarations)
        ObjectType ot = model.objectTypes.computeIfAbsent(src(ctx.objectTypeNaam()), ObjectType::new);
        for (ObjectTypeMemberContext m : ctx.objectTypeMember()) member(ot, m, ctx, true);
        membersAfterHead(ot, ctx, ctx.objectTypeMember());
    }

    private void member(ObjectType ot, ObjectTypeMemberContext m, ParserRuleContext definition, boolean extension) {
        recordMemberLine(ot, m);
        boolean repetition = false;
        if (m.attribuutSpecificatie() != null || m.kenmerkSpecificatie() != null) {
            boolean attribute = m.attribuutSpecificatie() != null;
            Member member = new Member(ot, attribute, attribute ? name(m.attribuutSpecificatie().lowerNaam())
                    : characteristicName(m.kenmerkSpecificatie()), extension, m, definition);
            repetition = members.stream().anyMatch(l -> l.same(member) && (extension || l.extension()));
            members.add(member);
            // an extension's repeat is judged once every declaration is read; the model keeps the declaration the
            // type already carries, so the rest of the document reads that one
            if (repetition && extension) return;
        }
        if (m.attribuutSpecificatie() != null) {
            AttribuutSpecificatieContext a = m.attribuutSpecificatie();
            String name = name(a.lowerNaam());
            reportCapitalCollision(ot, name, ot.attributes.keySet(), a.getStart().getLine());
            ot.attributes.put(name, withUnit(datatypeOf(a.datatype(), a.domeinNaam()), a, name));
            if (a.meervoudsvorm() != null) ot.attributePlural.put(name, src(a.meervoudsvorm())); // §3.3, for §8.1.6
            // §13.3.7 "gedimensioneerd met <dimensie> (en <dimensie>)*": record the attribute's dimensions
            // in declared order so a reference / aggregation can range over their label sets (§3.6).
            if (!a.dimensieNaam().isEmpty())
                ot.attributeDimensions.put(name, a.dimensieNaam().stream().map(dn -> src(dn)).toList());
            // §13.3.6 a temporal attribute carries a tijdlijn ("voor elk jaar") — record its granularity so
            // the Phase-7 value model can later lower the periods; absent for an ordinary single-value attribute.
            Granularity g = granularityOf(a.tijdlijn());
            if (g != null) {
                ot.attributeGranularity.put(name, g);
                int count = countOf(a.tijdlijn());   // §13.3.6.7 the "5" in "voor elke 5 maanden"; 1 otherwise
                if (count != 1) ot.attributeTimelineCount.put(name, count);
                String fixed = fixedStartPointOf(a.tijdlijn(), g, count);
                if (fixed != null) ot.attributeFixedStartPoint.put(name, fixed);
                // §13.3.6.9 the NAMED form: keep the tijdlijn name so §9.9 can route a variable
                // startpunt to its anchor (the inline "voor elke maand" forms record none).
                if (a.tijdlijn().ELKE() != null && a.tijdlijn().tijdlijnNaam() != null)
                    ot.attributeTimelineName.put(name, normaliseTimelineName(src(a.tijdlijn().tijdlijnNaam())));
            }
        } else if (m.kenmerkSpecificatie() != null) {
            String name = characteristicName(m.kenmerkSpecificatie());
            reportCapitalCollision(ot, name, ot.characteristics, m.getStart().getLine());
            // a head after an extension that declared the characteristic: the name stands once
            if (!repetition || !ot.characteristics.contains(name)) ot.characteristics.add(name);
            ot.characteristicKinds.put(name, characteristicKindOf(m.kenmerkSpecificatie()));   // §3.5 drives the §8.1.8 predicate form
            TijdlijnContext kt = timelineOf(m.kenmerkSpecificatie());
            Granularity gk = granularityOf(kt);
            if (gk != null) {
                ot.characteristicGranularity.put(name, gk);
                int count = countOf(kt);
                if (count != 1) ot.characteristicTimelineCount.put(name, count);
                String fixed = fixedStartPointOf(kt, gk, count);
                if (fixed != null) ot.characteristicFixedStartPoint.put(name, fixed);
            }
        }
        // koptekst carries no model information
    }

    /** §13.2.30: a naam is NOT hoofdletter-onderscheidend, so two members of one object type OF
     *  THE SAME KIND that differ ONLY in case are the same name declared twice — ambiguous (a
     *  third casing would bind whichever was declared first, see {@link ObjectType#attribute}).
     *  Warn at registration. Scoped to one kind: an attribute and a kenmerk MAY legitimately
     *  share a name — a
     *  kenmerk carries no datatype and never enters {@link ObjectType#attribute}, so there is no
     *  cross-kind ambiguity. An exact-case redeclaration is left to the collection. */
    private void reportCapitalCollision(ObjectType ot, String name, java.util.Collection<String> existing, int line) {
        if (existing.contains(name)) return; // exact duplicate of the same kind: not a case botsing
        if (existing.stream().anyMatch(k -> k.equalsIgnoreCase(name)))
            model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.WARNING,
                    "RGS-011", params("name", name, "objectType", ot.name), line));
    }

    private String characteristicName(KenmerkSpecificatieContext k) {
        if (k instanceof BijvoeglijkKenmerkContext b) return name(b.lowerNaam());
        if (k instanceof NaamwoordKenmerkContext n) return name(n.lowerNaam());
        return src(k);
    }

    /** §3.5 kenmerk soort from the declaration: the optional {@code (bijvoeglijk)}/{@code (bezittelijk)} marker
     *  classifies it; an UNTAGGED kenmerk is ZELFSTANDIG (the "is een &lt;k&gt;" noch-noch form — including the
     *  casus shapes the grammar admits via the IS-form without the tag). */
    private static ObjectType.CharacteristicKind characteristicKindOf(KenmerkSpecificatieContext k) {
        if (k instanceof BijvoeglijkKenmerkContext b)
            return b.BIJVOEGLIJK() != null ? ObjectType.CharacteristicKind.BIJVOEGLIJK : ObjectType.CharacteristicKind.ZELFSTANDIG;
        if (k instanceof NaamwoordKenmerkContext n)
            return n.BEZITTELIJK() != null ? ObjectType.CharacteristicKind.BEZITTELIJK
                 : n.BIJVOEGLIJK() != null ? ObjectType.CharacteristicKind.BIJVOEGLIJK
                 : ObjectType.CharacteristicKind.ZELFSTANDIG;
        return ObjectType.CharacteristicKind.ZELFSTANDIG;
    }

    // ---------------- §13.3.3/§13.3.4 datatypes & domains ----------------
    private void domain(DomeinDefinitionContext ctx) {
        Datatype dt = ctx.enumeratieSpecificatie() != null
                ? enumeration(src(ctx.domeinNaam()), ctx.enumeratieSpecificatie())
                : datatypeOf(ctx.datatype(), null);
        model.domains.put(src(ctx.domeinNaam()), dt);
    }

    private Datatype enumeration(String domain, EnumeratieSpecificatieContext ctx) {
        List<String> values = ctx.enumeratieWaarde().stream()
                .map(w -> unquote(w.ENUM_LITERAL().getText())).toList();
        return new Datatype.Enumeration(domain, values);
    }

    /** Resolve an attribute/parameter type: a concrete datatype, or a (possibly forward) domain reference. */
    /** §3.4 the attribute-level unit clause ("de prijs Bedrag met eenheid €"): a domain-typed attribute pins a unit
     *  on its number. The clause used to be parsed and never read, so the unit was dropped and the attribute written
     *  as a bare number, where every unit check reads it. It sets the unit of a number without one; on a number that
     *  carries a unit already, a percentage (whose unit is %) or a value of another kind it has no reading, and is
     *  refused. */
    private Datatype withUnit(Datatype dt, AttribuutSpecificatieContext a, String name) {
        if (a.eenheidExpressie() == null || dt instanceof Datatype.Unknown) return dt;
        Unit e = unitOf(a.eenheidExpressie());
        Msg reason;
        if (dt instanceof Datatype.Numeric n) {
            if (!n.hasUnit() || n.unit().equals(e))
                return new Datatype.Numeric(n.wholeNumber(), n.decimals(), n.sign(), e);
            reason = Msg.frontend("RGS-012.has-unit", "existing", n.unit().rendering());
        } else if (dt instanceof Datatype.Percentage) {
            reason = Msg.frontend("RGS-012.percentage");
        } else {
            reason = Msg.frontend("RGS-012.no-unit", "type", dt.name());
        }
        model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.ERROR,
                "RGS-012", params("name", name, "unit", src(a.eenheidExpressie()), "reason", reason), a.getStart().getLine()));
        return dt;
    }

    private Datatype datatypeOf(DatatypeContext dt, DomeinNaamContext dom) {
        if (dt != null) return datatype(dt);
        if (dom != null) {
            Datatype resolved = model.domains.get(src(dom));
            return resolved != null ? resolved : new Datatype.Unknown("domein " + src(dom));
        }
        return new Datatype.Unknown("geen datatype");
    }

    private Datatype datatype(DatatypeContext dt) {
        if (dt.numeriekDatatype() != null) {
            NumeriekDatatypeContext n = dt.numeriekDatatype();
            // §13.3.3: the getalspecificatie is normatively mandatory ("Numeriek (" <getalspecificatie> ")"),
            // but a worked example (§4.4) omits it ("Numeriek met eenheid jaren"), so the grammar tolerates
            // its absence. Degrade to the least-restrictive reading — equal to a plain "getal" (onbeperkt
            // aantal decimalen, geen tekenbeperking) — and flag the spec-incomplete declaration.
            if (n.getalSpecificatie() == null)
                model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.WARNING, "RGS-013", params(),
                        n.getStart().getLine()));
            NumberInfo g = numberInfo(n.getalSpecificatie());
            Unit e = n.eenheidExpressie() != null ? unitOf(n.eenheidExpressie()) : null;
            return new Datatype.Numeric(g.wholeNumber, g.decimals, g.sign, e);
        }
        if (dt.percentageDatatype() != null) {
            var pd = dt.percentageDatatype();
            NumberInfo g = numberInfo(pd.getalSpecificatie());
            // §3.3.5: a Percentage is always "%", optionally specialized to a rate "%/<eenheid>"
            // (grammar: "met eenheid % ( / <eenheidAfkorting> )?"); a bare or absent clause is "%".
            Unit e = pd.SLASH() != null
                    ? Unit.compound(Datatype.Percentage.PERCENT, model.resolveUnit(src(pd.eenheidAfkorting())))
                    : Datatype.Percentage.PERCENT;
            return new Datatype.Percentage(g.wholeNumber, g.decimals, e);
        }
        if (dt.tekstDatatype() != null) return new Datatype.Text();
        if (dt.booleanDatatype() != null) return new Datatype.BooleanType();
        if (dt.datumTijdDatatype() != null) {
            return dt.datumTijdDatatype().DATUM_IN_DAGEN() != null
                    ? new Datatype.DateInDays() : new Datatype.DateTime();
        }
        return new Datatype.Unknown("datatype");
    }

    private record NumberInfo(boolean wholeNumber, int decimals, Datatype.Sign sign) {}

    private NumberInfo numberInfo(GetalSpecificatieContext g) {
        // No specification given: same degenerate reading as a bare "getal" — unbounded decimals
        // (decimalen 0 is the codebase's unbounded sentinel), not a whole number, no sign restriction.
        if (g == null) return new NumberInfo(false, 0, Datatype.Sign.ONBEPAALD);
        Datatype.Sign sign = Datatype.Sign.ONBEPAALD;
        if (g.NEGATIEF() != null) sign = Datatype.Sign.NEGATIEF;
        else if (g.NIET_NEGATIEF() != null) sign = Datatype.Sign.NIET_NEGATIEF;
        else if (g.POSITIEF() != null) sign = Datatype.Sign.POSITIEF;
        boolean wholeNumber = g.GEHEEL_GETAL() != null;
        int decimals = 0;
        if (g.DECIMALEN() != null && g.NUMBER() != null) {
            try { decimals = Integer.parseInt(g.NUMBER().getText()); } catch (NumberFormatException ignore) {}
        }
        // §13.3.3.7: "getal met x decimalen" requires x >= 1 (aantaldecimalen is a positiefgeheelgetal).
        // A degenerate "getal met 0 decimalen" denotes a whole number — form (a) "geheel getal", capped
        // at 0 — NOT the unbounded form (c) "getal". Normalize it to geheel so it is not encoded as the
        // (geheel=false, decimalen=0) unbounded sentinel, which would silently drop its §6.1.2 ceiling.
        if (!wholeNumber && g.DECIMALEN() != null && decimals == 0) {
            wholeNumber = true;
            model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.WARNING, "RGS-014", params(), g.getStart().getLine()));
        }
        return new NumberInfo(wholeNumber, decimals, sign);
    }

    private Unit unitOf(EenheidExpressieContext e) { return unitOf(model, e); }

    /** §13.3.3.2: {@code <eenheidexpressie> ::= <eenheidmacht> ("/" <eenheidmacht>)*} — the numerator
     *  is the first power; a single trailing {@code "/<unit>"} is the denominator of a rate unit
     *  (€/jr). A second denominator (€/u/s) is not yet modelled — keep the numerator only.
     *  Package-static so the attribute-datatype path (here) and the §13.2.17 numeric LITERAL path
     *  ({@link ExpressionBuilder}) resolve a composite (rate) unit IDENTICALLY — a literal "120 EUR/jr"
     *  must carry the same {@code Unit.compound(EUR, jr)} an attribute "met eenheid EUR/jr"
     *  does, so §4 unit checking and the exacte cross-unit restatement apply to both alike. */
    static Unit unitOf(RegelSpraakModel model, EenheidExpressieContext e) {
        if (e == null || e.eenheidMacht().isEmpty()) return null;
        Unit numerator = model.resolveUnit(src(e.eenheidMacht(0).eenheidAfkorting()));
        if (e.eenheidMacht().size() == 2)
            return Unit.compound(numerator, model.resolveUnit(src(e.eenheidMacht(1).eenheidAfkorting())));
        return numerator;
    }

    // ---------------- §13.3.5 unit systems ----------------
    /** A unit's raw declaration before its conversion factor is resolved: the factor as the decimal the unit record
     *  carries and as the exact fraction the wire writes, both read from the one written number. */
    private record RawUnit(java.util.List<String> abbreviations, String word, String plural,
                              java.math.BigDecimal conversionFactor, UnitSystem.Factor conversionFactorExact, String conversionTarget) {}

    private void unitSystem(EenheidsysteemDefinitionContext ctx) {
        UnitSystem system = new UnitSystem(src(ctx.eenheidsysteemNaam()));
        // pass 1: collect raw definitions keyed by primary abbreviation (or word)
        java.util.Map<String, RawUnit> raw = new java.util.LinkedHashMap<>();
        java.util.Map<String, Integer> rules = new java.util.HashMap<>();
        // every abbreviation a declaration writes, under the name identity: {as written, the unit's primary}
        java.util.Map<String, String[]> written = new java.util.HashMap<>();
        for (EenheidDefinitieContext u : ctx.eenheidDefinitie()) {
            java.util.List<String> abbr = u.eenheidAfkorting().stream().map(DefinitionResolver::src).toList();
            String primary = !abbr.isEmpty() ? abbr.get(0) : (u.eenheidWoord() != null ? src(u.eenheidWoord()) : null);
            if (primary == null) continue;
            // WF-009, one unit per abbreviation under the name identity the emitter checks too: a second declaration
            // of an abbreviation would replace the first in the maps below without a word, so it is refused by name
            // and the first declaration stands
            if (refuseRepeatedAbbreviation(system.name, abbr.isEmpty() ? List.of(primary) : abbr, written, u)) continue;
            java.math.BigDecimal n = null; UnitSystem.Factor exact = null; String to = null;
            if (u.omrekenSpecificatie() != null) {                       // "= 12 mnd" or "= 1/1000 m"
                String number = u.omrekenSpecificatie().NUMBER().getText();
                n = rational(number, primary);
                if (n != null) exact = exactFactor(number);
                to = src(u.omrekenSpecificatie().eenheidAfkorting());
            }
            raw.put(primary, new RawUnit(abbr, u.eenheidWoord() != null ? src(u.eenheidWoord()) : null,
                    u.meervoudsvorm() != null ? src(u.meervoudsvorm()) : null, n, exact, to));
            rules.put(primary, u.getStart().getLine());
        }
        // pass 2: resolve each unit's canonical + factor, then register every spelling
        java.util.Map<String, Unit> resolvedUnits = new java.util.HashMap<>();
        for (String primary : raw.keySet()) {
            Unit e = resolveFactor(primary, raw, system.name, resolvedUnits, new java.util.HashSet<>(),
                    system.exactFactors);
            RawUnit r = raw.get(primary);
            for (String a : r.abbreviations()) system.units.put(a, e);
            if (r.word() != null) system.spellings.put(RegelSpraakModel.normaliseTimeUnit(r.word()), e);
            if (r.plural() != null) system.spellings.put(RegelSpraakModel.normaliseTimeUnit(r.plural()), e);
        }
        model.unitSystems.put(system.name, system);
        // the wire writes a factor as a fraction of two integers from 1 to 2^62 - 1 (IR-SPEC ch. 2.5): a declared zero
        // or negative factor, and one its chain of declarations grows past that bound, have no document. Before this
        // check the first ended in the emitter's exception; each is refused in the loader's sentence now.
        java.math.BigInteger limit = java.math.BigInteger.TWO.pow(62).subtract(java.math.BigInteger.ONE);
        for (var entry : system.exactFactors.entrySet()) {
            UnitSystem.Factor f = entry.getValue();
            if (f.num().signum() > 0 && f.num().compareTo(limit) <= 0 && f.den().compareTo(limit) <= 0) continue;
            model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.ERROR,
                    "RGS-015", params("unit", entry.getKey(), "unitSystem", system.name, "num", f.num(), "den", f.den(),
                            "factor", "{\"num\":" + f.num() + ",\"den\":" + f.den() + "}"),
                    rules.getOrDefault(entry.getKey(), 0)));
        }
    }

    /** Whether one of {@code abbreviations} — the abbreviations one unit declaration writes — was written before by an
     *  EARLIER declaration of the same system, under the name identity ({@code IrEmitter.norm}); if so an ERROR names
     *  the pair. Otherwise the declaration claims them. Two spellings of one abbreviation inside ONE declaration
     *  ("kg KG") are that unit's own spellings and no collision. */
    private boolean refuseRepeatedAbbreviation(String system, List<String> abbreviations, java.util.Map<String, String[]> written,
                                    EenheidDefinitieContext u) {
        for (String a : abbreviations) {
            String[] earlier = written.get(dev.uchar.regelspraak.emit.IrEmitter.norm(a));
            if (earlier == null) continue;
            // the second declaration beside the first on the wire is what the verifier refuses, in these words
            model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.ERROR,
                    "RGS-016", params("unitSystem", system, "abbreviation", a, "earlierAbbreviation", earlier[0], "earlierUnit", earlier[1],
                            "writtenAs", a.equals(earlier[0]) ? "" : Msg.frontend("RGS-016.written-as", "earlierAbbreviation", earlier[0])),
                    u.getStart().getLine()));
            return true;
        }
        for (String a : abbreviations)
            written.putIfAbsent(dev.uchar.regelspraak.emit.IrEmitter.norm(a),
                    new String[]{a, abbreviations.get(0)});
        return false;
    }

    /** Parse an omrekenfactor, which the lexer hands over as a single rational NUMBER
     *  token: a whole number ("12"), a decimal ("1,5"), or the §13.3.5.2 "1/N" form
     *  ("1/1000"). A malformed factor is reported, not silently swallowed. */
    private java.math.BigDecimal rational(String text, String unit) {
        try {
            int slash = text.indexOf('/');
            if (slash >= 0)
                return new java.math.BigDecimal(text.substring(0, slash))
                        .divide(new java.math.BigDecimal(text.substring(slash + 1)), java.math.MathContext.DECIMAL64);
            return new java.math.BigDecimal(text.replace(',', '.'));
        } catch (RuntimeException e) {
            model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.WARNING,
                    "RGS-017", params("value", text, "unit", unit), 0));
            return null;
        }
    }

    /** Reduce a unit to its system canonical and the factor to reach it: a base unit
     *  is its own canonical (factor 1); "<u> = N <v>" inherits v's canonical with
     *  factor N·factor(v). Memoised; a cycle or external target stops at a base.
     *  {@code exact} receives the same factor as an exact fraction, step for step, so the two agree on every
     *  unit, the cycle's included. */
    private Unit resolveFactor(String abbrev, java.util.Map<String, RawUnit> raw, String system,
                                  java.util.Map<String, Unit> memo, java.util.Set<String> busy,
                                  java.util.Map<String, UnitSystem.Factor> exact) {
        if (memo.containsKey(abbrev)) return memo.get(abbrev);
        RawUnit r = raw.get(abbrev);
        Unit e;
        if (r == null || r.conversionTarget() == null || r.conversionFactor() == null || !busy.add(abbrev)) {
            e = new Unit(abbrev, system, abbrev, java.math.BigDecimal.ONE);
            exact.put(abbrev, UnitSystem.Factor.EEN);
        } else {
            Unit base = resolveFactor(r.conversionTarget(), raw, system, memo, busy, exact);
            busy.remove(abbrev);
            e = new Unit(abbrev, system, base.canonical(), r.conversionFactor().multiply(base.factorToCanonical()));
            exact.put(abbrev, r.conversionFactorExact().times(exact.get(base.abbreviation())));
        }
        memo.put(abbrev, e);
        return e;
    }

    /** The omrekenfactor as an exact fraction, from the same NUMBER text {@link #rational} reads and failing on the
     *  same texts (the caller asks only after that reading succeeded): "12" is 12/1, "1,5" is 3/2, "1/24" is 1/24. */
    private static UnitSystem.Factor exactFactor(String text) {
        int slash = text.indexOf('/');
        if (slash >= 0)
            return new UnitSystem.Factor(new java.math.BigInteger(text.substring(0, slash)),
                    new java.math.BigInteger(text.substring(slash + 1)));
        java.math.BigDecimal d = new java.math.BigDecimal(text.replace(',', '.'));
        return d.scale() <= 0
                ? new UnitSystem.Factor(d.toBigIntegerExact(), java.math.BigInteger.ONE)
                : new UnitSystem.Factor(d.unscaledValue(), java.math.BigInteger.TEN.pow(d.scale()));
    }

    // ---------------- §13.3.8 parameters ----------------
    private void parameter(ParameterDefinitionContext ctx) {
        String name = src(ctx.parameterNaam());
        // §13.3.6/§13.3.8 a parameter may carry a tijdlijn ("voor elk jaar") -> its runtime value is a timeline.
        Granularity g = ctx.tijdlijn() != null ? granularityOf(ctx.tijdlijn()) : null;
        String fixed = null;
        if (g != null) {
            int count = countOf(ctx.tijdlijn());
            // IR-SPEC ch. 2.10: a parameter's facet has count 1 while multi-period grids are open, and the model's
            // parameter has no count to write — refused in WF-005's words rather than written as one period
            if (count != 1)
                model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.ERROR,
                        "RGS-018", params("name", name, "count", count), ctx.getStart().getLine()));
            else fixed = fixedStartPointOf(ctx.tijdlijn(), g, count);
        }
        model.parameters.put(name, new Parameter(name, datatypeOf(ctx.datatype(), ctx.domeinNaam()), g, fixed));
    }

    // ---------------- §13.3.6 timelines ----------------
    private void timeline(TijdlijnDefinitieContext ctx) {
        boolean variable = src(ctx).toLowerCase().contains("met variabel startpunt");
        String name = src(ctx.tijdlijnNaam());
        Granularity gran = definitionGranularity(ctx);
        int count = definitionCount(ctx);
        String fixed = gran == null ? null : fixedStartPoint(gran, count, ctx.vastStartpuntDatum(),
                ctx.vastStartpuntMaand(), ctx.vastStartpuntKwartaal(), ctx.vastStartpuntJaar());
        // §13.3.6.10: a "voor elke <tijdlijn>" reference adopts the FIRST timeline of that (normalised) name via
        // findFirst. Warn only when an earlier same-name timeline declares a DIFFERENT periodicity (granularity
        // or count) — then the reference silently disagrees; an identical redeclaration is harmless.
        String normalised = normaliseTimelineName(name);
        if (model.timelines.stream().anyMatch(tl -> normaliseTimelineName(tl.name()).equals(normalised)
                && (tl.granularity() != gran || tl.count() != count)))
            model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.WARNING,
                    "RGS-019", params("name", name),
                    ctx.tijdlijnNaam().getStart().getLine()));
        model.timelines.add(new Timeline(name, variable, gran, count, fixed));
    }

    /** §13.3.6.3–.7 the fixed start point of a tijdlijn attachment in its wire spelling (IR-SPEC ch. 2.8), or null
     *  on the calendar grid. The inline forms write it here; the named form ("voor elke &lt;tijdlijn&gt;") adopts
     *  the definition's with its granularity and count, by the same first-match rule. */
    private String fixedStartPointOf(TijdlijnContext t, Granularity g, int count) {
        if (t.enkelvoudigePeriode() != null) {
            EnkelvoudigePeriodeContext e = t.enkelvoudigePeriode();
            return fixedStartPoint(g, count, e.vastStartpuntDatum(), e.vastStartpuntMaand(), e.vastStartpuntKwartaal(),
                    e.vastStartpuntJaar());
        }
        if (t.meervoudigePeriode() != null)
            return fixedStartPoint(g, count, t.meervoudigePeriode().vastStartpuntDatum(), null, null, null);
        if (t.ELKE() != null && t.tijdlijnNaam() != null) {
            String ref = normaliseTimelineName(src(t.tijdlijnNaam()));
            // the timeline first, then its start point: a timeline on the calendar grid has none (null)
            return model.timelines.stream().filter(tl -> normaliseTimelineName(tl.name()).equals(ref))
                    .findFirst().map(Timeline::fixedStartPoint).orElse(null);
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
    private String fixedStartPoint(Granularity g, int count, VastStartpuntDatumContext date,
                                 VastStartpuntMaandContext month, VastStartpuntKwartaalContext quarter,
                                 VastStartpuntJaarContext year) {
        if (date != null) {
            java.time.LocalDate d = startDate(date);
            if (d == null) return null;
            if (count >= 2 || g == Granularity.WEEK) return d.toString();
            if (g == Granularity.DAG) return null;
            return calendarEmpty(dev.uchar.regelspraak.core.FixedStartPoint.canonical(g, d.getMonthValue(), d.getDayOfMonth()));
        }
        if (month != null) {
            Integer day = wholeNumber(month.NUMBER());
            if (day == null || day < 1 || day > 31) return refuse("§13.3.6.4", month,
                    "RGS-020", params("section", "§13.3.6.4", "value", src(month)));
            return calendarEmpty(dev.uchar.regelspraak.core.FixedStartPoint.canonical(g, 1, day));
        }
        if (quarter != null) {
            List<Integer> months = quarter.kwartaalGroep().NUMBER().stream().map(n -> wholeNumber(n)).toList();
            Integer first = months.get(0);
            if (first == null || first < 1 || first > 3
                    || !months.equals(List.of(first, first + 3, first + 6, first + 9)))
                return refuse("§13.3.6.5", quarter,
                        "RGS-021", params("section", "§13.3.6.5", "value", src(quarter)));
            Integer day = wholeNumber(quarter.NUMBER());
            if (day == null || day < 1 || day > 31) return refuse("§13.3.6.5", quarter,
                    "RGS-020", params("section", "§13.3.6.5", "value", src(quarter)));
            return calendarEmpty(dev.uchar.regelspraak.core.FixedStartPoint.canonical(g, first, day));
        }
        if (year != null) {
            Integer day = wholeNumber(year.NUMBER(0)), m = wholeNumber(year.NUMBER(1));
            if (m == null || m < 1 || m > 12) return refuse("§13.3.6.6", year,
                    "RGS-022", params("section", "§13.3.6.6", "value", src(year)));
            String c = day == null ? null : dev.uchar.regelspraak.core.FixedStartPoint.canonical(g, m, day);
            if (c == null) return refuse("§13.3.6.6", year,
                    "RGS-023", params("section", "§13.3.6.6", "value", src(year), "month", m));
            return calendarEmpty(c);
        }
        return null;
    }

    /** The calendar's own start point is written as none (IR-SPEC ch. 2.8). */
    private static String calendarEmpty(String spelling) {
        return "--01-01".equals(spelling) ? null : spelling;
    }

    private static final java.util.regex.Pattern START_DATE =
            java.util.regex.Pattern.compile("(?:dd\\. +)?(\\d{1,2})-(\\d{1,2})-(\\d{4})");

    /** The day of a §13.3.6.3 "startend op &lt;datum&gt;", or null after refusing a source that names none: a periode
     *  starts on a day, so a time of day has no place in it, and a bare number or an impossible date names no day. */
    private java.time.LocalDate startDate(VastStartpuntDatumContext date) {
        TerminalNode d = date.datumWaarde().DATE();
        java.util.regex.Matcher m = d == null ? null : START_DATE.matcher(d.getText());
        if (m != null && !m.matches() && d.getText().contains(":"))
            return refuseDate(date, "RGS-024.time-of-day");
        if (m == null || !m.matches()) return refuseDate(date, "RGS-024.no-calendar-day");
        try {
            return java.time.LocalDate.of(Integer.parseInt(m.group(3)), Integer.parseInt(m.group(2)),
                    Integer.parseInt(m.group(1)));
        } catch (java.time.DateTimeException noDay) {
            return refuseDate(date, "RGS-024.no-calendar-day");
        }
    }

    private java.time.LocalDate refuseDate(VastStartpuntDatumContext date, String reason) {
        refuse("§13.3.6.3", date, "RGS-024", params("section", "§13.3.6.3", "value", src(date), "reason", Msg.frontend(reason)));
        return null;
    }

    /** A source start point the wire cannot spell, refused by name with its section. */
    private String refuse(String section, ParserRuleContext ctx, String code, java.util.Map<String, Object> params) {
        model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.ERROR, code, params,
                ctx.getStart().getLine()));
        return null;
    }

    /** A NUMBER token as a whole number, or null when it is none (a decimal or a fraction). */
    private static Integer wholeNumber(TerminalNode n) {
        if (n == null || !n.getText().matches("\\d{1,9}")) return null;
        return Integer.parseInt(n.getText());
    }

    /** §13.3.6 the {@link Granularity} of a tijdlijn ATTACHMENT on an attribute/kenmerk. The single-period
     *  form carries it in the token ("voor elke dag" → DAG), the multi-period form in the plural unit ("voor
     *  elke 5 maanden" → MAAND). The named form ("voor elke &lt;tijdlijn&gt;") adopts its definition's
     *  granularity, resolved against {@link RegelSpraakModel#timelines} elsewhere — null here. */
    private Granularity granularityOf(TijdlijnContext t) {
        if (t == null) return null;
        EnkelvoudigePeriodeContext e = t.enkelvoudigePeriode();
        if (e != null) {
            if (e.ELKE_DAG() != null)     return Granularity.DAG;
            if (e.ELKE_WEEK() != null)    return Granularity.WEEK;
            if (e.ELKE_MAAND() != null)   return Granularity.MAAND;
            if (e.ELK_KWARTAAL() != null) return Granularity.KWARTAAL;
            if (e.ELK_JAAR() != null)     return Granularity.JAAR;
        }
        if (t.meervoudigePeriode() != null)
            return pluralGranularity(t.meervoudigePeriode().tijdseenhedenMeervoud());
        // §13.3.6.9 the named form "voor elke <tijdlijn>" adopts the granularity of the standalone Tijdlijn
        // definition of that name (registered in pass 1). The reference drops the leading lidwoord the
        // definition carries ("elke periode …" vs "Tijdlijn de periode …") and may wrap across lines, so
        // match on a whitespace-collapsed, lidwoord-stripped name. An unresolved name is an ERROR: the member
        // would lose its timeline without a word and be written as a single value. "voor elke 2 maanden" without
        // its start date is such a name, since the multi-period form (§13.3.6.7) does not parse without one.
        if (t.ELKE() != null && t.tijdlijnNaam() != null) {
            String ref = normaliseTimelineName(src(t.tijdlijnNaam()));
            Granularity g = model.timelines.stream()
                    .filter(tl -> normaliseTimelineName(tl.name()).equals(ref))
                    .map(Timeline::granularity).findFirst().orElse(null);
            if (g == null) {
                String written = src(t.tijdlijnNaam()).trim().replaceAll("\\s+", " ");
                boolean multiplePeriod = MULTIPLE_PERIOD.matcher(written).matches();
                if (multiplePeriod)
                    model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.ERROR,
                            "RGS-025", params("name", written), t.getStart().getLine()));
                else
                    model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.ERROR,
                            "RGS-026", params("name", written), t.getStart().getLine()));
            }
            return g;
        }
        return null;
    }

    /** A multi-period form written as a name: a count and a plural time unit ("2 maanden"). */
    private static final java.util.regex.Pattern MULTIPLE_PERIOD =
            java.util.regex.Pattern.compile("(?i)\\d+ (dagen|weken|maanden|kwartalen|jaren)");

    /** Normalise a tijdlijn name for cross-reference matching — the shared model implementation
     *  (§9.9 startpuntbepaling resolution matches by the same rule). */
    private static String normaliseTimelineName(String s) {
        return RegelSpraakModel.normaliseTimelineName(s);
    }

    /** §13.3.6.10 the {@link Granularity} a standalone tijdlijn DEFINITION declares ("heeft tijdvakken van
     *  een jaar" → JAAR), from the singular unit token or the plural-period unit. */
    private static Granularity definitionGranularity(TijdlijnDefinitieContext ctx) {
        if (ctx.DAG() != null)      return Granularity.DAG;
        if (ctx.WEEK() != null)     return Granularity.WEEK;
        if (ctx.MAAND() != null)    return Granularity.MAAND;
        if (ctx.KWARTAAL() != null) return Granularity.KWARTAAL;
        if (ctx.JAAR() != null)     return Granularity.JAAR;
        return pluralGranularity(ctx.tijdseenhedenMeervoud());
    }

    /** §13.3.6.7 the period count of a tijdlijn attachment: the "5" in "voor elke 5 maanden". A single-period
     *  form ("voor elke maand") carries none (1); the named form "voor elke &lt;tijdlijn&gt;" adopts its
     *  definition's count (so "heeft tijdvakken van 5 maanden" → 5), mirroring {@link #granularityOf}. */
    private int countOf(TijdlijnContext t) {
        if (t == null) return 1;
        if (t.meervoudigePeriode() != null) return pluralCount(t.meervoudigePeriode().tijdseenhedenMeervoud());
        if (t.ELKE() != null && t.tijdlijnNaam() != null) {
            String ref = normaliseTimelineName(src(t.tijdlijnNaam()));
            return model.timelines.stream()
                    .filter(tl -> normaliseTimelineName(tl.name()).equals(ref))
                    .map(Timeline::count).findFirst().orElse(1);
        }
        return 1;
    }

    /** §13.3.6.10 the period count a standalone tijdlijn DEFINITION declares ("heeft tijdvakken van 5 maanden" →
     *  5), or 1 for the singular "een &lt;unit&gt;" form. */
    private static int definitionCount(TijdlijnDefinitieContext ctx) {
        return pluralCount(ctx.tijdseenhedenMeervoud());
    }

    /** The NUMBER of a plural period unit ("5 maanden" → 5), or 1 when absent. */
    private static int pluralCount(TijdseenhedenMeervoudContext m) {
        if (m == null || m.NUMBER() == null) return 1;
        try { return Integer.parseInt(m.NUMBER().getText()); } catch (NumberFormatException e) { return 1; }
    }

    /** §13.3.6.8 the plural time-unit of a multi-period tijdlijn → its singular {@link Granularity}. */
    private static Granularity pluralGranularity(TijdseenhedenMeervoudContext m) {
        if (m == null) return null;
        if (m.DAGEN() != null)     return Granularity.DAG;
        if (m.WEKEN() != null)     return Granularity.WEEK;
        if (m.MAANDEN() != null)   return Granularity.MAAND;
        if (m.KWARTALEN() != null) return Granularity.KWARTAAL;
        if (m.JAREN() != null)     return Granularity.JAAR;
        return null;
    }

    /** The optional tijdlijn attachment on a kenmerk declaration (both the bijvoeglijk and naamwoord forms
     *  carry one), or null. */
    private static TijdlijnContext timelineOf(KenmerkSpecificatieContext k) {
        if (k instanceof BijvoeglijkKenmerkContext b) return b.tijdlijn();
        if (k instanceof NaamwoordKenmerkContext n) return n.tijdlijn();
        return null;
    }

    // ---------------- §13.3.7 dimensions ----------------
    private void dimension(DimensieDefinitionContext ctx) {
        List<String> values = ctx.labelWaardeSpecificatie().stream()
                .map(l -> src(l.dimensieWaarde())).toList();
        // §3.6/§13.3.7.2: "na het attribuut met voorzetsel <vz>" gives the suffix-read preposition; the
        // "voor het attribuut zonder voorzetsel" form has no voorzetsel (null) — a prefix (bijvoeglijk) read.
        VoorzetselSpecificatieContext prepositionSpec = ctx.voorzetselSpecificatie();
        String preposition = prepositionSpec != null && prepositionSpec.voorzetsel() != null ? src(prepositionSpec.voorzetsel()) : null;
        model.dimensions.put(src(ctx.dimensieNaam()),
                new Dimension(src(ctx.dimensieNaam()), src(ctx.dimensieNaamMeervoud()), preposition, values));
    }

    // ---------------- §13.3.9 fact types ----------------
    private void factType(FeitTypeDefinitionContext ctx) {
        FactType ft = new FactType(src(ctx.feitTypeNaam()));
        ft.reciprocal = ctx.WEDERKERIG_FEITTYPE() != null;
        List<Role> roles = new ArrayList<>();
        boolean recovered = recovered(ctx);
        java.util.Set<ParserRuleContext> onPreviousLine = recovered ? java.util.Set.of()
                : reportMembersOnPreviousLine(ctx, ctx.rolSpecificatie(), RolSpecificatieContext.class, "feittype '" + ft.name + "'");
        for (RolSpecificatieContext rs : ctx.rolSpecificatie()) {
            String roleName = src(rs.rolNaam());
            String plural = rs.meervoudsvorm() != null ? src(rs.meervoudsvorm()) : null; // declared "(mv: …)"
            if (!recovered && !onPreviousLine.contains(rs))
                reportUnreadableName("feittype '" + ft.name + "'", Msg.frontend("PART.member-category.role"), roleName, rs.getStart().getLine());
            if (!recovered && rs.objectTypeNaam() != null && rs.objectTypeNaam().getStart() != null)
                roleFillers.add(new RoleFiller(ft.name, roleName, src(rs.objectTypeNaam()),
                        rs.objectTypeNaam().getStart().getLine()));
            // §13.3.9.3: feitTypeNaam excludes 'de'/'het' (it stops at the role's article), so a feittype
            // name that itself contains an article ("bonus van DE groep") truncates there and the leftover
            // "de <woord>" is absorbed by this greedy roleName — corrupting it to "<woord> de <echte rol>"
            // with no error node. A role name MAY legitimately contain an article, but only after a
            // preposition ("eigenaar van de woning", "behandelaar van het dossier"); the corruption's article
            // instead follows a bare noun ("groep de bonusgroep"). So flag a 'de'/'het' that has a word
            // before AND after it and is NOT preceded by a preposition — the tell-tale of a swallowed
            // name+article boundary — without warning on spec-legal "… van de/het …" names.
            // Beside the line check, not instead of it: that check names the line and the word where a role
            // begins on the head's line, this guess names the likely cause, and it alone sees the swallow where
            // the role name runs on over the next line.
            if (articleAfterBareWord(roleName))
                model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.WARNING,
                        "RGS-027", params("factType", ft.name, "role", roleName),
                        ctx.feitTypeNaam().getStart().getLine()));
            roles.add(new Role(roleName, src(rs.objectTypeNaam()), plural));
            roleLines.computeIfAbsent(ft, k -> new java.util.HashMap<>()).putIfAbsent(roleName, rs.getStart().getLine());
        }
        // §13.3.9.1 cardinaliteit: the line "<q1> <rol1> … <q2> <rol2>" marks each role one or
        // meerdere. A wederkerig role is enkelvoudig (§3.11), so only a normal feittype's roles
        // take a 'meerdere' mark; an unmatched/absent line leaves a role single (fail-open).
        if (!ft.reciprocal && ctx.feitTypeCardinaliteit() != null)
            roles = markCardinality(roles, ctx.feitTypeCardinaliteit());
        ft.roles.addAll(roles);
        model.factTypes.put(ft.name, ft);
    }

    /** The prepositions a role name may carry before an internal article (the §13.4 name words: a role
     *  like "eigenaar van de woning" / "deel in het geheel" is legitimate). */
    private static final java.util.Set<String> PREPOSITIONS =
            java.util.Set.of("van", "op", "in", "voor", "over", "bij", "tot", "per", "met");

    /** Whether {@code roleName} contains a 'de'/'het' that has a word both before and after it and is NOT
     *  preceded by a preposition — the signature of a feittype-name article (§13.3.9.3) swallowed into the
     *  role, as opposed to a legitimate "… van de/het …" role name. */
    private static boolean articleAfterBareWord(String roleName) {
        String[] w = roleName.toLowerCase().split("\\s+");
        for (int i = 1; i + 1 < w.length; i++)            // an article with a word before AND after it
            if ((w[i].equals("de") || w[i].equals("het")) && !PREPOSITIONS.contains(w[i - 1]))
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
    private List<Role> markCardinality(List<Role> roles, FeitTypeCardinaliteitContext card) {
        List<Role> result = new ArrayList<>(roles);
        // Collect the (meervoudig, head-words) segments in order — there are exactly two for a
        // non-wederkerig feittype (the grammar fixes it), one per role.
        List<Boolean> plural = new ArrayList<>();
        List<String[]> segments = new ArrayList<>();
        boolean manyNext = false;
        for (int i = 0; i < card.getChildCount(); i++) {
            ParseTree ch = card.getChild(i);
            if (ch instanceof TerminalNode t) {
                manyNext = t.getSymbol().getType() == RegelSpraak.MEERDERE;
            } else if (ch instanceof CardinaliteitDeelContext part) {
                segments.add(src(part).split("\\s+"));
                plural.add(manyNext);
            }
        }
        // Bind each segment to a role by NAME first (longest leading sub-phrase). A segment whose noun
        // matches no role — an UNDECLARED irregular plural the language does not synthesise (§footnote 2:
        // "vakken"/"pupillen" need consonant doubling, refused by both the best-effort and PluralMatcher)
        // — is then assigned by ELIMINATION: a non-wederkerig feittype has exactly two roles, so if the
        // other segment named one role, this segment must be the remaining one. No plural guessing, just
        // the 2-role structure. (When BOTH segments fail to name a role there is no signal, so both stay.)
        int[] segRole = new int[segments.size()];
        boolean[] roleOccupied = new boolean[result.size()];
        for (int s = 0; s < segments.size(); s++) {
            int j = findHead(result, segments.get(s), roleOccupied);
            segRole[s] = j;
            if (j >= 0) roleOccupied[j] = true;
        }
        for (int s = 0; s < segments.size(); s++) {
            if (segRole[s] >= 0) continue;
            int rest = -1, n = 0;
            for (int j = 0; j < result.size(); j++) if (!roleOccupied[j]) { rest = j; n++; }
            if (n == 1) { segRole[s] = rest; roleOccupied[rest] = true; }
        }
        for (int s = 0; s < segments.size(); s++) {
            int j = segRole[s];
            if (j >= 0 && result.get(j).many() != plural.get(s))
                result.set(j, new Role(result.get(j).name(), result.get(j).objectTypeName(), result.get(j).plural(), plural.get(s)));
        }
        return result;
    }

    /** The index of the role named by the LEADING sub-phrase of {@code words} (the rolnaam head),
     *  or -1 when none matches: try the longest leading phrase first so a multi-word role binds whole
     *  and a more specific role wins over a shorter prefix; stop at the first match so trailing
     *  relatiebeschrijving words never name another role. An already-bound role is skipped so the two
     *  segments cannot both claim the same role. */
    private int findHead(List<Role> result, String[] words, boolean[] roleOccupied) {
        for (int len = words.length; len >= 1; len--) {
            String candidate = String.join(" ", java.util.Arrays.copyOfRange(words, 0, len));
            for (int j = 0; j < result.size(); j++)
                if (!roleOccupied[j] && RegelSpraakModel.roleNameMatches(result.get(j), candidate)) return j;
        }
        return -1;
    }

    /** The two checks on the members of one head: the line check, and the readability of every name it did not
     *  refuse — a refused member is a misreading, so its name is not the one the author declared. */
    private void membersAfterHead(ObjectType ot, ParserRuleContext definition, List<ObjectTypeMemberContext> members) {
        if (recovered(definition)) return;
        String carrier = "objecttype '" + ot.name + "'";
        java.util.Set<ParserRuleContext> refused = reportMembersOnPreviousLine(definition, members, ObjectTypeMemberContext.class, carrier);
        for (ObjectTypeMemberContext m : members) {
            if (refused.contains(m)) continue;
            if (m.attribuutSpecificatie() != null)
                reportUnreadableName(carrier, Msg.frontend("PART.member-category.attribute"), name(m.attribuutSpecificatie().lowerNaam()), m.getStart().getLine());
            else if (m.kenmerkSpecificatie() != null)
                reportUnreadableName(carrier, Msg.frontend("PART.member-category.characteristic-kind"), characteristicName(m.kenmerkSpecificatie()), m.getStart().getLine());
        }
    }

    // ---------------- a declaration on the line where the previous one ended ----------------
    /** Whether the parser recovered inside {@code t}: an error node, or a rule that caught a recognition error. The
     *  boundaries of a recovered definition are the recovery's and not the author's, so the checks below leave it
     *  alone; its syntax error is reported already, and it is the one to act on. */
    private static boolean recovered(ParseTree t) {
        if (t instanceof ErrorNode) return true;
        if (t instanceof ParserRuleContext p && p.exception != null) return true;
        for (int i = 0; i < t.getChildCount(); i++) if (recovered(t.getChild(i))) return true;
        return false;
    }

    /** Where a head ends: its last token before the first member or role, and whether that token ends a NAME — the
     *  type name of the head or of its 'is een' clause, or the fact type's name — rather than an annotation. */
    private record HeadEnd(Token token, boolean onName) {}

    private static HeadEnd headEnd(ParserRuleContext definition, Class<?> member) {
        Token end = null;
        ParseTree last = null;
        for (ParseTree kind : definition.children == null ? List.<ParseTree>of() : definition.children) {
            if (member.isInstance(kind)) break;
            Token t = kind instanceof TerminalNode n ? n.getSymbol()
                    : kind instanceof ParserRuleContext p ? p.getStop() : null;
            if (t == null || t.getTokenIndex() < 0) continue;   // a token the error recovery conjured stands nowhere
            end = t;
            last = kind;
        }
        return end == null ? null : new HeadEnd(end, endsOnName(last));
    }

    private static boolean endsOnName(ParseTree t) {
        if (t instanceof ObjectTypeNaamContext || t instanceof FeitTypeNaamContext) return true;
        if (t instanceof ObjectTypeSupertypeContext s && s.getChildCount() > 0)
            return endsOnName(s.getChild(s.getChildCount() - 1));
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
    private java.util.Set<ParserRuleContext> reportMembersOnPreviousLine(ParserRuleContext definition,
                                                                   List<? extends ParserRuleContext> members,
                                                                   Class<? extends ParserRuleContext> memberKind,
                                                                   String carrier) {
        java.util.Set<ParserRuleContext> refused = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        HeadEnd head = headEnd(definition, memberKind);
        if (head == null) return refused;
        Token preceding = head.token();
        Object previousSentence = Msg.frontend("PART.previous-head");
        boolean onName = head.onName(), onCharacteristic = false;
        boolean role = memberKind == RolSpecificatieContext.class;
        for (ParserRuleContext member : members) {
            Token begin = member.getStart(), end = member.getStop();
            if (begin == null || end == null || end.getTokenIndex() < begin.getTokenIndex()) continue;
            Msg memberSentence = memberSentence(member);
            if (memberSentence != null && begin.getLine() == preceding.getLine()) {
                String word = begin.getText();
                Object hint = "";
                if (onName && keyWord(begin))
                    hint = Msg.frontend("PART.hyphen-hint-before", "word", word, "preceding", preceding.getText());
                else if (onCharacteristic)
                    hint = Msg.frontend("PART.hyphen-hint-characteristic", "word", word);
                error(
                        "RGS-028", params("carrier", carrier, "member", memberSentence, "word", word, "previousSentence", previousSentence, "preceding", preceding.getText(), "memberKind", Msg.frontend(role ? "RGS-028.role" : "RGS-028.member"), "hint", hint),
                        begin.getLine());
                refused.add(member);
            }
            preceding = end;
            previousSentence = memberSentence == null ? Msg.frontend("PART.previous-head-text")
                    : Msg.frontend(role ? "PART.previous-role" : "PART.previous-member", "name", memberName(member));
            onName = role;                                  // a role ends on its filler, an object-type name
            onCharacteristic = !role && end.getType() == RegelSpraak.KENMERK;
        }
        return refused;
    }

    /** "het attribuut 'naam'", "het kenmerk 'naam'" or "de rol 'naam'"; null for a heading. */
    private Msg memberSentence(ParserRuleContext member) {
        if (member instanceof RolSpecificatieContext) return Msg.frontend("PART.member-phrase.role", "name", memberName(member));
        ObjectTypeMemberContext m = (ObjectTypeMemberContext) member;
        if (m.attribuutSpecificatie() != null) return Msg.frontend("PART.member-phrase.attribute", "name", memberName(member));
        if (m.kenmerkSpecificatie() != null) return Msg.frontend("PART.member-phrase.characteristic-name", "name", memberName(member));
        return null;
    }

    private String memberName(ParserRuleContext member) {
        if (member instanceof RolSpecificatieContext r) return src(r.rolNaam());
        ObjectTypeMemberContext m = (ObjectTypeMemberContext) member;
        return m.attribuutSpecificatie() != null ? name(m.attribuutSpecificatie().lowerNaam())
                : m.kenmerkSpecificatie() != null ? characteristicName(m.kenmerkSpecificatie()) : null;
    }

    /** A token of the language rather than a word of the author: neither a name word of either case nor a number. */
    private static boolean keyWord(Token t) {
        return t.getType() != RegelSpraak.UPPER_ID && t.getType() != RegelSpraak.LOWER_ID && t.getType() != RegelSpraak.NUMBER;
    }

    // ---------------- a role's filler is a declared object type ----------------
    private record RoleFiller(String factType, String role, String fillerType, int line) {}
    private final List<RoleFiller> roleFillers = new ArrayList<>();

    /** WF-003 for a role's filler, BEFORE the emission: until this check the frontend wrote the document without a
     *  word and only the verifier refused it, so a check without the verifier passed a filler like 'BK Jutter' — a
     *  role name cut at its first capitalised word, its rest read as the type. Case-insensitive, as WF-003 resolves.
     *  An ERROR where the document shows the misreading: the name is declared as another kind (the sentence says
     *  which), or a shorter tail of it is a declared object type (the sentence says the role name ended at the first
     *  capitalised word). A WARNING where the name is declared nowhere in the document: one file of a model that
     *  declares its object types in another file reads so on its own, and resolves once the files are read as one
     *  (grammar/DIVERGENCES.md, "Namen in declaratiekoppen"); the verifier still refuses such a file emitted on its
     *  own. */
    private void refuseUndeclaredFillers() {
        for (RoleFiller v : roleFillers) {
            if (model.objectTypeByName(v.fillerType()) != null) continue;
            String category = nameKind(v.fillerType());
            boolean misread = category != null;
            String[] words = v.fillerType().split(" ");
            String typePart = null;
            ObjectType suffixType = null;
            for (int i = 1; i < words.length; i++) {
                String suffix = String.join(" ", java.util.Arrays.copyOfRange(words, i, words.length));
                ObjectType ot = model.objectTypeByName(suffix);
                if (ot == null) continue;
                typePart = String.join(" ", java.util.Arrays.copyOfRange(words, 0, i));
                suffixType = ot;
                break;
            }
            if (suffixType != null)
                error("RGS-030",
                        params("factType", v.factType(), "role", v.role(), "fillerType", v.fillerType(),
                                "objectType", suffixType.name, "prefix", typePart,
                                "status", category == null ? Msg.frontend("RGS-030.undeclared")
                                        : Msg.frontend("RGS-030.other-kind", "category", category)),
                        v.line());
            else if (misread)
                error("RGS-029", params("factType", v.factType(), "role", v.role(),
                        "fillerType", v.fillerType(), "category", category), v.line());
            else model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.WARNING,
                    "RGS-031", params("factType", v.factType(), "role", v.role(), "fillerType", v.fillerType()),
                    v.line()));
        }
    }

    // ---------------- a declared name rule text does not read ----------------
    /** A declared attribute, characteristic or role name that rule text cannot read back as one name gets a WARNING
     *  naming the word. The name is lexed again on its own and parsed as the {@code bareName} of rule text, up to its
     *  end: 'als' is no word of a name there, and 'van' separates two parts of one and so neither opens nor closes
     *  it. A WARNING and no ERROR: the declaration itself is well-formed, a document that never reads the name in a
     *  rule is sound, and one source outside this repository carries such a name (grammar/DIVERGENCES.md, "Namen in
     *  declaratiekoppen"). */
    private void reportUnreadableName(String carrier, Msg category, String name, int line) {
        List<String> words = new ArrayList<>();
        int error = unreadableWord(name, words);
        if (error < 0) return;
        List<String> example = new ArrayList<>(words);
        int left = error > 0 ? error - 1 : error;
        if (left + 1 < example.size()) {
            example.set(left, example.get(left) + "-" + example.get(left + 1));
            example.remove(left + 1);
        }
        model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.WARNING,
                "RGS-032", params("carrier", carrier, "category", category, "name", name, "word", words.get(error), "example", String.join(" ", example)), line));
    }

    /** The index, among the tokens of {@code naam} lexed on their own, of the first word that keeps the
     *  {@code bareName} of rule text from reading the whole name; -1 when it reads it. {@code words} receives the
     *  tokens' texts. */
    static int unreadableWord(String name, List<String> words) {
        RegelSpraakLexer lexer = new RegelSpraakLexer(CharStreams.fromString(name));
        lexer.removeErrorListeners();
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();
        List<Token> list = tokens.getTokens().stream()
                .filter(t -> t.getChannel() == Token.DEFAULT_CHANNEL && t.getType() != Token.EOF).toList();
        for (Token t : list) words.add(t.getText());
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
        for (int i = 0; i < list.size(); i++) if (list.get(i).getTokenIndex() == dader.getTokenIndex()) return i;
        throw new IllegalStateException("DefinitionResolver.onleesbaarWoord: het token '" + dader.getText()
                + "' staat niet in de naam '" + name + "'");
    }

    // ---------------- helpers ----------------
    /** §3.12/§13.3.10 register a dagsoort: its singular name, and the unit→dagsoort map for BOTH the singular
     *  and (when declared) the meervoud — a §6 duration unit "5 werkdagen" arrives as the plural eenheidsafkorting. */
    private void dayKind(DagsoortDefinitionContext d) {
        String single = name(d.lowerNaam());
        // A day kind NAMED after a calendar spelling is refused outright (WF-017 over /dagsoorten), and the
        // refusal has to be an ERROR rather than the WARNING-and-drop the plural gets below: dropping the
        // NAME would leave the §9.8 dagsoortdefinitie pointing at a day kind `dagsoorten` no longer declares,
        // which the verifier refuses in its own right. An ERROR blocks the emission entirely — the same
        // answer the grammar already gives for the unit names that are keywords.
        if (TimeCalculation.isCalendarSpelling(single.trim().toLowerCase())) {
            model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.ERROR,
                    "RGS-033", params("name", single),
                    d.getStart().getLine()));
            return;
        }
        int line = d.getStart().getLine();
        model.dayKinds.add(single);
        registerDayKindUnit(single, single, line, false);
        // §3.12 a duration unit is naturally the PLURAL ("5 werkdagen"); register the declared meervoud, or — when
        // none is declared — the derived "-en" plural ("werkdag" -> "werkdagen"), so the unit still resolves.
        if (d.meervoudsvorm() != null) registerDayKindUnit(src(d.meervoudsvorm()).trim(), single, line, true);
        else registerDayKindUnit(single + "en", single, line, null);
    }

    /** Every written spelling of a day kind so far, lower-cased: the day kind, and its written plural or null where
     *  the spelling is the day kind's name. */
    private final java.util.Map<String, String[]> dayKindSpellings = new java.util.HashMap<>();

    /** A dagsoort unit spelling that collides with a spelling of the NORMATIVE calendar table
     *  (uchar's RUNTIME-SEMANTICS ch. 3.8 — thirty spellings, abbreviations and second plurals included) is
     *  NOT registered: it must not shadow the calendar arithmetic of {@code datum ± N dagen}; warn instead.
     *  The comparison goes through {@link TimeCalculation} rather than a list of its own, so this guard and the
     *  arithmetic that reads a unit spelling can never come to know different tables — which is what they
     *  did until 2026-09-20, when this list held the eighteen singulars and plurals and let the twelve
     *  remaining spellings through into an IR that WF-017 refuses.
     *  {@code line} is the declaration's own line: the warning used to be raised at a hard-coded 0, which
     *  points an author at nothing in a document with many day kinds. */
    private void registerDayKindUnit(String spelling, String dayKind, int line, Boolean plural) {
        String k = spelling.trim().toLowerCase();
        if (TimeCalculation.isCalendarSpelling(k)) {
            model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.WARNING,
                    "RGS-034", params("unit", spelling), line));
            return;
        }
        // A written spelling another day kind already carries names two day kinds (WF-017). It used to be dropped
        // without a word: the second day kind lost its declared plural, or its name came to stand for the first. A
        // derived plural ({@code plural} null) is this frontend's own convenience and gives way without a refusal,
        // also to a written spelling that comes after it.
        if (plural == null) {                 // a derived plural gives way to every spelling already there
            model.dayKindPerUnit.putIfAbsent(k, dayKind);
            return;
        }
        String[] earlier = dayKindSpellings.get(k);
        if (earlier == null) {                   // a written spelling: a derived plural there gives way to it
            dayKindSpellings.put(k, new String[]{dayKind, plural ? spelling : null});
            model.dayKindPerUnit.put(k, dayKind);
            return;
        }
        if (!earlier[0].equalsIgnoreCase(dayKind)) {
            if (plural && earlier[1] == null)
                model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.ERROR,
                        "RGS-035",
                        params("spelling", spelling, "dayKind", dayKind, "earlierDayKind", earlier[0]), line));
            else if (plural)
                model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.ERROR,
                        "RGS-036",
                        params("spelling", spelling, "dayKind", dayKind, "earlierDayKind", earlier[0],
                                "earlierPlural", earlier[1]), line));
            else if (earlier[1] != null)
                model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.ERROR,
                        "RGS-037",
                        params("dayKind", dayKind, "earlierDayKind", earlier[0], "earlierPlural", earlier[1]), line));
        }
    }

    private static String name(LowerNaamContext ctx) { return src(ctx); }

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
    static dev.uchar.regelspraak.ir.Source source(ParserRuleContext ctx) {
        return ctx == null ? dev.uchar.regelspraak.ir.Source.synthetic("")
                           : dev.uchar.regelspraak.ir.Source.of(ctx, src(ctx));
    }

    private static String unquote(String s) {
        if (s.length() >= 2) return s.substring(1, s.length() - 1);
        return s;
    }
}
