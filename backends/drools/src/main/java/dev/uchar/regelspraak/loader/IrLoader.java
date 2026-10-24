package dev.uchar.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.ir.Source;
import dev.uchar.regelspraak.refusal.DroolsCode;
import dev.uchar.regelspraak.refusal.Reason;
import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.ir.Default;
import dev.uchar.regelspraak.ir.ExceptionGroup;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.Validity;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.ir.ResolvedConsistency;
import dev.uchar.regelspraak.ir.ResolvedDayKindDefinition;
import dev.uchar.regelspraak.ir.ResolvedFactCreation;
import dev.uchar.regelspraak.ir.ResolvedObjectCreation;
import dev.uchar.regelspraak.ir.ResolvedRule;
import dev.uchar.regelspraak.ir.ResolvedRuleGroup;
import dev.uchar.regelspraak.ir.ResolvedStartPointDetermination;
import dev.uchar.regelspraak.ir.ResolvedUniqueness;
import dev.uchar.regelspraak.ir.ResolvedUniquenessUnion;
import dev.uchar.regelspraak.ir.ResolvedAllocation;
import dev.uchar.regelspraak.ir.Spelling;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.Unit;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Dimension;
import dev.uchar.regelspraak.model.RegelSpraakModel.UnitSystem;
import dev.uchar.regelspraak.model.RegelSpraakModel.FactType;
import dev.uchar.regelspraak.model.RegelSpraakModel.Granularity;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;
import dev.uchar.regelspraak.model.RegelSpraakModel.Parameter;
import dev.uchar.regelspraak.model.RegelSpraakModel.RoleBindingRef;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.model.RegelSpraakModel.Timeline;
import dev.uchar.regelspraak.core.TimeCalculation;
import dev.uchar.regelspraak.core.FixedStartPoint;
import dev.uchar.regelspraak.transpiler.core.EffectiveUnit;
import dev.uchar.regelspraak.transpiler.core.Namen;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The ir.json -&gt; {@link RegelSpraakModel} loader — the inverse of the frontend's {@code IrEmitter}.
 * The maintainer architecture anchor (2026-08-27): the Drools backend CONSUMES the uchar IR off the
 * wire, exactly as ref/ and backends/clingo do. This loader parses the {@code uchar-ir-v1} JSON (every node in its
 * L5 shape but the rule-status check {@code Regelversie}, which keeps its v0 shape) into the SAME frontend record
 * types the transpilers consume — the member nodes back into the model's side-maps — so the exec-model branch
 * ({@code TmsTranspiler}) and the DRL-text branch fire the IR — never the RegelSpraak source.
 *
 * <p><b>Refusal discipline.</b> Every wire form the v0 Drools backend does not carry is a LOUD, per-form
 * {@link Refusal} — never a silent skip. That covers a {@code format} other than {@code uchar-ir-v1}, the
 * model's {@code functionDefs} extension, an integer beyond a Java {@code int}, and a member of the schema's
 * expression or datatype union that has no record in the {@link Expr} / {@link Datatype} sealed hierarchies
 * (a struct/list literal, a MapList/Invoke, a temporal read, …), which the refusal calls a node unknown to them. The
 * differential gate turns a Refusal into a reason-carrying exclusion. A document that is not well-formed is no form
 * at all: the container, the declaration lists, the M1 nodes ({@code ObjectType}, {@code Attribute},
 * {@code Characteristic}, {@code TimelineFacet}, {@code Domain} with its datatype) and each {@code Diagnostic} are
 * read as strictly as the OCaml loader reads them — exact keys, the node discriminator, the JSON types,
 * WF-001/WF-002 name identity — and a defect there is a {@link DocumentError} naming its JSON position, a hard
 * error. So is a node name that the schema's union at its position does not have, refused at the discriminator in
 * the shared loader's sentence ({@code unknown expr node "<v>"}, {@code unknown condition node "<v>"},
 * {@code unknown datatype node "<v>"}, {@code unknown TextCompositionPart node "<v>"}); until the L5 landing's fix
 * round such a name was a Refusal, so a document no reader can read was filed as a form this backend does not carry.
 * One step stricter: a Diagnostic's message must be a string, as the schema types it, where the OCaml loader admits a
 * null so that WF-113 can name it. Every datatype, at any position, carries its discriminator.
 *
 * <p><b>The reference layer (L2 of the v1 wire migration).</b> A reference to a declared thing carries its
 * symbolic form only, and the bindings are the one source of every relation this backend walks: the navigation of
 * a read ({@code navigationBindings}) and of a characteristic check ({@code navigation}), the range of a fold and of a quantifier
 * fold ({@code sourceRole}, {@code sourcePath}), the
 * recipient join of an allocation and the back-link of an object creation ({@code connectingRole}), a union
 * selection's {@code sourceRole}, a role check's {@code factType}/{@code role}, a creation's symbolic block and a
 * uniqueness version's {@code scope}. A
 * reference node's type is the declaration's (IR-SPEC ch. 2.10), never an annotation. Since L2's fix round every
 * node this loader reads — not only the nodes L2 changed — is read with the schema's exact keys and a JSON pointer:
 * a field the shape does not have is an unknown field, a binding the shape requires is a missing field, and an
 * {@code Onbekend} datatype or a role check of the L1 shape is an unknown node — each a {@link DocumentError}. So is
 * a reference-layer shape every executor refuses by name: a fold's set source its kind cannot consume, a count's
 * range that is a path, a {@code total} without a range over another type than its subject, a role check naming no link
 * the subject can be in, and a characteristic read at a value position ({@link #checkFoldSource},
 * {@link #checkSubjects}).
 *
 * <p><b>The type system (L3 of the v1 wire migration).</b> A number is a {@code Number}, a date a {@code Date} with
 * its precision, a unit a {@code Unit} whose factor is an exact fraction {@code {num, den}}, a unit system a list
 * of units with their spellings, a parameter carries its timeline facet and a day kind is a {@code DayKind} node. The
 * records keep their own shape and this loader lays the wire back onto them one to one: a {@code Number} whose unit
 * is '%' is a {@link Datatype.Percentage} (its presentation tag, legal only at a declaration, carries no meaning
 * here), any other a {@link Datatype.Numeric}; {@code day} is {@link Datatype.DateInDays} and {@code millisecond}
 * {@link Datatype.DateTime}. The exact factor becomes the record's decimal {@code factorToCanonical} — a whole
 * number exactly, any other fraction rounded to 16 significant digits ({@link MathContext#DECIMAL64}), the rounding
 * the frontend gives a declared "1/N" — and the unit system keeps the fraction itself beside it
 * ({@link UnitSystem#exactFactors}). From there this backend restates between units with the ratio of those
 * decimals, where {@code ref} reckons with the fraction: a tracked delta of the Drools backend (CONSTRUCTS row 26). The
 * corpus does not see it, since every factor there is a whole number, but a verifier-clean document the frontend
 * emits does: with a unit declared "= 1/3" of another, 1.00 of the one plus 3.00 of the other reads 1.9999999999999999
 * in a 2-decimal attribute, and rounded down to 0 decimals it is 1 where 2 is due ({@code ref}: 2.00 and 2). The
 * backend's {@code PreRoundedFactorWitnessTest} pins that answer, and the like one for 1/24, in both branches.
 * Closing the delta means restating through the fraction the unit system keeps; this loader does not. A type-system
 * node of the L2 shape, or one of its fields, is unknown by name.
 *
 * <p><b>The rule and expression layers (L4 of the v1 wire migration).</b> A named form — {@code Rule},
 * {@code Allocation}, {@code ObjectCreation}, {@code FactCreation}, {@code Consistency}, {@code Uniqueness} — is its
 * name and its versions, and each version becomes one record, named {@code "<name> #<i>"} when the form has two or
 * more, as the frontend names them. A rule version's kind is its structure ({@code initialisation},
 * {@code presentation}), crossed against the member its target names; a fact creation's target type is the declared
 * filler of its target role; the one list of uniquenesses splits on the number of sources into the model's two
 * lists. A fold is an {@code Aggregation} and every other function a {@code BuiltinCall}, both read into the one
 * function record; a literal's typed value is read back into the lexeme the record holds, after its variant is
 * crossed against its type; the bound day and the session values are read into the one constant record. Every
 * refusal here is the shared OCaml loader's or the verifier's, in their words, and a field or node of the L3 shape
 * is unknown by name.
 *
 * <p><b>The source record (L5 of the v1 wire migration).</b> A node's source text is its record {@code source}: the
 * {@code snippet}, and on a consistency version and a rule group the {@code line}, which those two records keep; an
 * absent record is a node without source text, read as the records always held one. The record is read strictly —
 * an empty snippet, a line below 1, a file, a column or any other field is refused at its pointer — and the field it
 * replaced, {@code bron}, is unknown on every node, as is a {@code line} beside the record. A marker without a record
 * is refused in WF-113 (b)'s sentence, as the shared loader refuses it for an executor.
 *
 * <p><b>The renaming (L5 of the v1 wire migration).</b> Every node, every key and every list under {@code rules}
 * carries its IR-SPEC name, and every enumeration value its lowerCamel spelling, read through the one translation
 * table of the shared records ({@link Spelling}): the records keep their v0 components and constants, so wire and
 * memory spell differently, and that table is the one place where they meet. A v0 node name, key, list key or
 * enumeration value is unknown and refused by name, in the shared loader's words. A field the renaming made optional
 * is absent or written whole: absent is what the v0 null was — an open bound of a {@code Validity} or a
 * {@code TimeWindow}, a {@code Clamp} without that bound, a characteristic check on the subject itself ({@code host}),
 * no whole-period qualifier, no plural, no preposition, no decision or justification on a default, no characteristic
 * on a defaults group, no day kind on a predicate of another kind, no subselection of a quantifier fold — and a null
 * there is refused. Three values the schema does not admit are a verifier rule's: the quantifier {@code the} on a
 * {@code QuantifierFold} (WF-108 (a)) and the severities {@code error}, {@code info} and null (WF-113 (a)). The shared
 * loader reads them for the verifier and refuses them for an executor, and so does this loader, in the rule's
 * sentences.
 *
 * The frontend records this loader fills keep their v0 components (they are the vendored slice the toolchain
 * compiles); what the wire no longer carries is filled with {@code null}, {@code false} or an empty list, and
 * what the records have no component for rides BESIDE the model in {@link Loaded} — the same side-channel
 * shape {@code prioritizedDefaults} already uses. The additive {@code statuteAnnotations} provenance field is
 * carried through.
 */
public final class IrLoader {

    /** A wire form the v0 Drools backend does not carry. Carries the offending {@code form} token so the
     *  differential gate can list the golden with a precise, reason-carrying exclusion. */
    public static final class Refusal extends RuntimeException {
        private final Reason reason;
        private final String form;
        public Refusal(Reason reason, String form) {
            super(reason.text());
            this.reason = reason;
            this.form = form;
        }
        public String form() { return form; }
        public DroolsCode code() { return reason.code(); }
        /** The sentence in {@code locale}. */
        public String message(java.util.Locale locale) { return reason.text(locale); }
        public Reason reason() { return reason; }
    }

    /** A document that is not a well-formed {@code uchar-ir-v1} document, named by the position it sits at. A hard
     *  error and never a {@link Refusal}: a Refusal files the document under the differential gate's auto-excluded
     *  channel as a form this backend does not carry, which is a green gate over a document nobody read (the reason
     *  {@link #dateOfIrWire} gives for its own fence). {@code site} is a JSON pointer into the document for the
     *  container and the M1 nodes, the root being {@code ""}. An absent field is named at the object that lacks it,
     *  with the field in the detail, as the OCaml loader names it; every other defect at the value itself, an
     *  unknown field included, which the OCaml loader names at its parent. */
    public static final class DocumentError extends IllegalArgumentException {
        private final String site;
        public DocumentError(String site, String detail) {
            super("malformed uchar-ir-v1 document at " + (site.isEmpty() ? "<root>" : "'" + site + "'") + ": "
                    + detail);
            this.site = site;
        }
        public String site() { return site; }
    }

    private static final List<String> ROOT = List.of("format", "model", "rules", "diagnostics");
    private static final List<String> MODEL = List.of(
            "objectTypes", "factTypes", "parameters", "domains", "unitSystems", "dimensions", "timelines",
            "dayKinds");
    private static final List<String> RULES = List.of(
            "rules", "dayKindDefinitions", "objectCreations", "allocations", "consistencies",
            "uniquenesses", "factCreations", "startPointDeterminations", "ruleGroups");

    private final RegelSpraakModel m = new RegelSpraakModel();
    /** IR-SPEC ch. 2.2: the declared supertype NAME per object type, held until every declaration is
     *  read — the wire reference is order-free, so a forward reference is legitimate. An IDENTITY map
     *  because two object types may carry the same declaration content. */
    private final Map<ObjectType, String> supertypeName = new java.util.IdentityHashMap<>();
    /** The JSON position of each {@link #supertypeName} entry, so a reference that does not resolve is reported
     *  where it was written. */
    private final Map<ObjectType, String> supertypePath = new java.util.IdentityHashMap<>();
    /** §6.1 (Phase 4) the prioritized-defaults extension list, parsed into the shared ir records — a top-level
     *  core-layer section the {@link RegelSpraakModel} does not itself carry (the model is the v0 resolved shape),
     *  so it rides beside the model via {@link Loaded}. An empty list is a legitimate flat program (accepted). */
    private final List<PrioritizedDefaults> prioritizedDefaults = new ArrayList<>();
    /** The wire's symbolic role bindings — see {@link Loaded#roleBindings()}. An IDENTITY map: every IR node here
     *  is a RECORD with VALUE equality, so two structurally identical nodes in one document would collapse into
     *  one entry and the second would silently inherit the first's binding. */
    private final Map<Object, RoleBindingRef> roleBindings = new java.util.IdentityHashMap<>();
    /** The JSON pointer of every object and array in the document, by IDENTITY (a Jackson container is a distinct
     *  instance per position), so a node read without a path can still be refused at its place. */
    private final Map<JsonNode, String> paths = new java.util.IdentityHashMap<>();
    /** ch. 4.10: every {@link Expr.Let} this document carries, in parse order — see {@link Loaded#lets()}. */
    private final List<Expr.Let> lets = new ArrayList<>();
    /** ch. 4.3 / WF-102: the fold-source RANGE as a path, per {@link Expr.FunctionCall} node — see
     *  {@link Loaded#sourcePaths()}. Identity-keyed for {@link #roleBindings}'s reason. */
    private final Map<Object, List<RoleBindingRef>> sourcePaths = new java.util.IdentityHashMap<>();
    /** IR-SPEC ch. 4.1a: the navigation bindings per AttributeReference / Kenmerk node — see
     *  {@link Loaded#navigationBindings()}. Identity-keyed for {@link #roleBindings}'s reason. */
    private final Map<Object, List<RoleBindingRef>> navigationBindings = new java.util.IdentityHashMap<>();
    /** IR-SPEC ch. 4.1a, WF-122: the nodes that carry {@code anchor: "form"} — see {@link Loaded#anchored()}.
     *  Identity-keyed for {@link #roleBindings}'s reason. */
    private final java.util.Set<Object> anchored = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    /** While {@link #checkSubjects} walks a form: the type of its subject, which an anchored node is read on. */
    private ObjectType formSubject;
    /** The JSON pointer of every AttributeReference, fold and role check read, by identity: the node kinds
     *  {@link #checkSubjects} refuses after the whole document is read, at the place they were written. */
    private final Map<Object, String> sites = new java.util.IdentityHashMap<>();
    /** The place of every object of the document in its pre-order, by identity: the order {@link #positions} keeps. */
    private final Map<JsonNode, Integer> order = new java.util.IdentityHashMap<>();
    /** Where every condition and expression node and every form version this loader read stands on the wire; see
     *  {@link Positioned}. */
    private final NodePositions positions = new NodePositions();

    private IrLoader() {}

    /** A loaded document: the v0 resolved {@link RegelSpraakModel}, the ch. 7 prioritized-defaults extension, and
     *  the symbolic role bindings the wire carries beside the v0 bean fields. All three ride BESIDE the model —
     *  {@code RegelSpraakModel} is the faithful v0 shape, and the frontend records that would host these
     *  extensions belong to a VENDORED slice the toolchain consumes at a pin, so they cannot grow a field here.
     *  A consumer that needs them reads them from here (see {@link #loadAll}).
     *
     *  @param roleBindings the symbolic (feittype, rol) binding per loaded IR NODE, by IDENTITY. Four node kinds
     *         carry one:
     *         <ul>
     *           <li>a {@link ResolvedObjectCreation} — IR-SPEC ch. 3.3 {@code connectingRole}, naming the role the
     *               CREATED object plays. It is the only route to the back-link of a SELF-TYPED pair, whose
     *               by-pair derivation is ambiguous by construction (two same-typed sibling roles); ch. 5.5 makes
     *               it authoritative, and {@code ref/lib/eval.ml}'s {@code run_object_creation} lets it WIN over
     *               the type-pair derivation.</li>
     *           <li>an {@link Expr.FunctionCall} aggregation — {@code sourceRole}, naming the role the aggregated ELEMENT
     *               plays. Same self-typed gap: "de som van de rang van alle opvolgers van de Wachter" joins two
     *               Wachters, which no by-pair search can disambiguate.</li>
     *           <li>a {@link ResolvedAllocation} — IR-SPEC ch. 3.2 {@code connectingRole}, naming the role the
     *               RECIPIENT plays. It decides the recipient join in both transpiler branches
     *               (V1-WIREMIGRATIE P3), as {@code run_allocation} lets it decide in ref; since L2 the wire
     *               requires it.</li>
     *           <li>a {@link Condition.RoleMembership} — the IR-SPEC ch. 4.5 {@code RoleMembership}, whose binding is its
     *               {@code factType} and the {@code role} the SUBJECT plays. The record is the carrier: its
     *               {@code role} is that role and its other-end slot holds the {@code counterpartType} narrowing or
     *               null (see {@link #counterpart}). Where the reference sits — on the subject or on the far end —
     *               is the transpilers' storage question.</li>
     *         </ul>
     *         In every case the SIBLING role — the one the anchor (subject) plays — is the bean field on the other
     *         end. Absent for a node whose wire record carries no binding: an object creation without one (its
     *         back-link is then derived from the type pair), and a fold that reads no linked instances — the
     *         subject's own read under {@code total}, or a fold over listed arguments (a single-argument fold over an
     *         element type of any other kind without one is refused while loading).
     *  @param lets an INDEX of the {@link Expr.Let} nodes in the document, in parse order — not a channel: the Lets
     *         themselves sit in the expression trees as ordinary union nodes, and every consumer lowers and walks
     *         them there. The index exists for the one consumer that must start a walk at a Let without holding the
     *         tree it came from ({@code EffectiveUnit}'s binder-read table, which answers the unit of a
     *         {@link Expr.BinderRef} asked in isolation). Empty for a document that carries no Let.
     *  @param sourcePaths the ch. 4.3 / WF-102 fold-source RANGE per {@link Expr.FunctionCall} node, by IDENTITY: the
     *         ordered RoleBinding steps, subject-outward, VERBATIM off the wire. It rides here for the same reason
     *         {@code sourceRole} does — {@code Expr.FunctionCall} is the vendored slice — and for a sharper one: unlike
     *         every other extension this loader carries, a path is not re-derivable. The type PAIR answers for the
     *         endpoints only, and a path's intermediate types are its own claim; a consumer that dropped this list
     *         would silently fold whatever direct relation happens to connect subject and element. Empty for a
     *         document whose folds all carry the one-step range.
     *  @param navigationBindings the IR-SPEC ch. 4.1a navigation walk per navigated {@link Expr.AttributeReference}
     *         (its {@code navigationBindings}, one binding per hop, subject-outward) and per navigated
     *         {@link Condition.CharacteristicCheck} (its {@code navigation}, a one-hop list), by IDENTITY, verbatim off the
     *         wire. Where a node carries one it decides the walk in both transpiler branches; a node without one
     *         is the subject's own read (or the element's, inside a fold's filter).
     *  @param anchored the IR-SPEC ch. 4.1a nodes that carry {@code anchor: "form"} (WF-122), by IDENTITY: an
     *         {@link Expr.AttributeReference}, a {@link Condition.CharacteristicCheck}, an aggregation's
     *         {@link Expr.FunctionCall} or a {@link Condition.QuantifierFold} that starts its walk at the subject of the
     *         form, where the filter or the test around it moved the subject to an element. The records are the
     *         frontend's and have no field for it. A consumer that dropped this set would read such a node on the
     *         element, a value the sentence does not name. The {@link Condition.IdentityCheck}, whose anchor is
     *         required and has one value, is not in it. Empty for a document without an anchor. */
    public record Loaded(RegelSpraakModel model, List<PrioritizedDefaults> prioritizedDefaults,
                         Map<Object, RoleBindingRef> roleBindings, List<Expr.Let> lets,
                         Map<Object, List<RoleBindingRef>> sourcePaths,
                         Map<Object, List<RoleBindingRef>> navigationBindings, java.util.Set<Object> anchored) {}

    /** A loaded document with the positions of its nodes on the wire ({@link NodePositions}): the JSON pointer, the
     *  place in the document's pre-order and the wire kind of every node of the condition and the expression union this
     *  loader read, and the pointer of every form version, by identity of the record. No side table of the lowering:
     *  a build with readings reads it, which numbers the nodes of a form by it ({@code TmsTranspiler#withReadings}), and
     *  {@code TmsTranspiler#originOf} names the fold of a rule of a fold by it, in a build with or without readings. */
    public record Positioned(Loaded loaded, NodePositions positions) {}

    /** Parse a {@code uchar-ir-v1} tree into a resolved model, or throw {@link Refusal} for a form
     *  the v0 backend does not carry. What rides beside the model — the prioritized-defaults extension and, since
     *  L2 of the v1 wire migration, every binding a read, a fold, a role check or a verdeling walks — is parsed but
     *  not returned: a transpiler handed this model alone reads every navigation as the subject's own. Use
     *  {@link #loadAll} for anything that transpiles. */
    public static RegelSpraakModel load(JsonNode root) {
        return new IrLoader().model(root);
    }

    /** As {@link #load} but also returns the ch. 7 prioritized-defaults extension list and the ch. 3.3 creation
     *  role bindings beside the model (EMPTY when the document carries none — a flat program, accepted
     *  explicitly). A malformed defaults bundle is a loud {@link Refusal} raised while parsing, never a silent
     *  skip. */
    public static Loaded loadAll(JsonNode root) {
        IrLoader l = new IrLoader();
        RegelSpraakModel model = l.model(root);
        return new Loaded(model, l.prioritizedDefaults, l.roleBindings, l.lets, l.sourcePaths, l.navigationBindings,
                l.anchored);
    }

    /** As {@link #loadAll}, with the positions of the document's nodes beside what it returns. */
    public static Positioned loadWithPositions(JsonNode root) {
        IrLoader l = new IrLoader();
        RegelSpraakModel model = l.model(root);
        return new Positioned(new Loaded(model, l.prioritizedDefaults, l.roleBindings, l.lets, l.sourcePaths,
                l.navigationBindings, l.anchored), l.positions);
    }

    /** The IR-SPEC ch. 4.5 other-end narrowing of a loaded {@code RoleMembership}: the type the filler of the other
     *  role must belong to, or null when the check does not narrow it (the role's declared filler). The loader
     *  keeps it in the record slot the near-side check always used for the other end (the vendored record keeps its
     *  v0 components), and this is the one place that knows so. */
    public static ObjectType counterpart(Condition.RoleMembership r) {
        return r.otherToFiller();
    }

    private RegelSpraakModel model(JsonNode root) {
        if (root == null || !root.isObject()) throw new DocumentError("", "expected an object, got " + shortQuote(root));
        assignPaths(root, "");
        // another format is a form this backend does not carry, whatever else that document holds; an absent or
        // non-string format is a defect of this one
        String format = textAt(root, "", "format");
        if (!"uchar-ir-v1".equals(format)) throw new Refusal(DroolsCode.DOCUMENT_FORMAT_UNSUPPORTED.reason("format", format), "format");
        fields(root, "", null, ROOT, List.of("vocabularies"));
        vocabularies(root);
        JsonNode model = section(root, "", "model");
        JsonNode rules = section(root, "", "rules");
        fields(model, "/model", null, MODEL, List.of("functionDefs"));
        // an extension the schema admits and this backend does not carry: a form, so a Refusal, not a defect —
        // once the section has the form the schema gives it. Its entries are not read, so not checked either.
        if (model.has("functionDefs")) {
            JsonNode fd = model.get("functionDefs");
            if (!fd.isArray()) throw new DocumentError("/model/functionDefs", "expected an array, got " + shortQuote(fd));
            throw new Refusal(DroolsCode.MODEL_FUNCTION_DEFS.reason(), "functionDefs");
        }
        fields(rules, "/rules", null, RULES, List.of("prioritizedDefaults"));

        // declarations first: the resolved lists reference them by name. Read in the v0 table order, so the
        // model fills exactly as it did from the v0 maps. The v0 `regels` stubs left the wire at M1 (delta B3);
        // no consumer here read them.
        list(model, "/model", "objectTypes", (p, n) -> {
            ObjectType ot = objectType(p, n);
            once(m.objectTypes, p + "/name", ot.name, ot);
        });
        bindSupertypes();
        list(model, "/model", "domains", (p, n) -> {
            fields(n, p, "Domain", List.of("node", "name", "datatype"), List.of());
            String name = name(n, p, "name");
            once(m.domains, p + "/name", name, domain(p, name, n));
        });
        list(model, "/model", "parameters", (p, n) -> {
            Parameter par = parameter(p, n);
            once(m.parameters, p + "/name", par.name(), par);
        });
        list(model, "/model", "unitSystems", (p, n) -> {
            UnitSystem es = unitSystem(p, n);
            once(m.unitSystems, p + "/name", es.name, es);
        });
        list(model, "/model", "factTypes", (p, n) -> {
            FactType ft = factType(p, n);
            once(m.factTypes, p + "/name", ft.name, ft);
        });
        list(model, "/model", "dimensions", (p, n) -> {
            Dimension d = dimension(p, n);
            once(m.dimensions, p + "/name", d.name(), d);
        });
        list(model, "/model", "timelines", (p, n) -> m.timelines.add(timeline(n)));
        refuseAdoption();
        list(model, "/model", "dayKinds", this::dayKind);

        // IR-SPEC ch. 3, Versioning: each list holds named forms, and every version is one record, named as the
        // records have always named a version of a form with several ("<name> #<i>")
        list(rules, "/rules", "rules", this::rule);
        list(rules, "/rules", "dayKindDefinitions", (p, n) -> {
            fields(n, p, "DayKindDefinition", List.of("node", "dayKind", "condition", "validity"), List.of());
            m.resolvedDayKindDefinitions.add(new ResolvedDayKindDefinition(text(n, "dayKind"), condition(nodeField(n, "condition")),
                    parsedValidity(n, "dayKind")));
        });
        list(rules, "/rules", "objectCreations", this::objectCreation);
        list(rules, "/rules", "allocations", this::allocation);
        list(rules, "/rules", "consistencies", this::consistency);
        list(rules, "/rules", "uniquenesses", this::uniqueness);
        list(rules, "/rules", "factCreations", this::factCreation);
        list(rules, "/rules", "startPointDeterminations", (p, n) -> {
            fields(n, p, "StartPointDetermination", List.of("node", "timeline", "objectType", "member"), List.of());
            m.resolvedStartPointDeterminations.add(new ResolvedStartPointDetermination(text(n, "timeline"), text(n, "objectType"),
                    text(n, "member")));
        });
        refuseInheritedAnchor();   // the §9.9 family: asked here, where BOTH the tijdlijnen and the determinations are read
        list(rules, "/rules", "ruleGroups", (p, n) -> {
            fields(n, p, "RuleGroup", List.of("node", "name", "recursive", "members"), List.of("source", "origin", "designations"));
            if (text(n, "name") != null) {
                origin(n, p, RegelSpraakModel.Designatable.ruleGroup(text(n, "name")));
                designations(n, p, RegelSpraakModel.Designatable.ruleGroup(text(n, "name")));
            }
            m.ruleGroups.add(new ResolvedRuleGroup(
                    text(n, "name"), bool(n, "recursive"), stringList(n.get("members")), source(n), sourceLine(n)));
        });
        list(root, "", "diagnostics", (p, n) -> m.diagnostics.add(diagnostic(p, n)));
        // §6.1 (Phase 4) the prioritized-defaults extension, parsed AFTER the declarations so a default's
        // condition/value resolves its objecttype/parameter references against the fully-built model. A malformed
        // defaults bundle (a missing subject or target) raises a loud Refusal from these parsers; an unknown inner
        // node is a DocumentError, as it is at every other position.
        if (rules.has("prioritizedDefaults"))
            list(rules, "/rules", "prioritizedDefaults", (p, n) -> prioritizedDefaults.add(prioritizedDefaults(n)));
        checkSubjects();
        checkTyping();
        return m;
    }

    // --- the reference layer's questions that need the subject or the position -----------------------

    /** The checks that need more than the node itself — the SUBJECT a node is read on, or the POSITION it stands
     *  at — made once the whole document is read, and refused as the verifier refuses them, in the words
     *  {@code ref} and the ASP backend fault with where they have them: a role check whose subject does not fill
     *  the role (WF-034, IR-SPEC ch. 4.5 — a link the subject can never be in), a characteristic read at a value
     *  position (WF-080 — a characteristic carries truth, no value), a read without bindings that names another
     *  type than its subject (WF-053, IR-SPEC ch. 4.1a), a value fold's set source that names no
     *  member of the element type ({@link #checkValueFoldSource}), and a {@code total} without a range over another
     *  type than the subject ({@link #checkOwnTotal}). The subject of each position is the one the verifier walks it
     *  with: the rule target's type, the element type inside a fold's filter and a quantifier fold, a selection's
     *  type inside a union branch, the creation and consistency subject, the allocation's source type, the fact
     *  creation's anchor, the defaults' subject, and none in a day-kind definition. A rule target is a write,
     *  never read here; the rest of a fold's set source is {@link #checkFoldSource}'s. Until L2's fix round the role
     *  check was refused with a storage reason, the characteristic read played, the value fold over a role name
     *  failed at first fire, and the own total over a foreign element type played (measured on the L2 union). */
    private void checkSubjects() {
        for (ResolvedRule r : m.resolvedRules) {
            ObjectType s = r.target() instanceof Expr.AttributeReference a ? a.objectType() : null;
            formSubject = s;
            checkValue(r.value(), s);
            checkCondition(r.condition(), s);
        }
        formSubject = null;
        for (ResolvedDayKindDefinition d : m.resolvedDayKindDefinitions) checkCondition(d.condition(), null);
        for (ResolvedObjectCreation oc : m.objectCreations) {
            formSubject = oc.subject();
            for (ResolvedObjectCreation.MemberInit t : oc.initialisations()) checkValue(t.value(), oc.subject());
            checkCondition(oc.condition(), oc.subject());
        }
        for (ResolvedAllocation v : m.allocations) {
            formSubject = v.sourceType();
            checkCondition(v.condition(), v.sourceType());
        }
        for (ResolvedConsistency c : m.consistencies) {
            formSubject = c.subject();
            checkCondition(c.assertion(), c.subject());
        }
        for (ResolvedFactCreation fc : m.factCreations) {
            formSubject = fc.anchor();
            checkCondition(fc.condition(), fc.anchor());
        }
        for (PrioritizedDefaults pd : prioritizedDefaults) {
            formSubject = ot(pd.subject());
            for (Default d : pd.defaults()) checkDefault(d, ot(pd.subject()));
        }
        formSubject = null;
    }

    private void checkDefault(Default d, ObjectType s) {
        checkCondition(d.condition(), s);
        checkValue(d.consequence(), s);
        if (d.exceptions() != null)
            for (ExceptionGroup g : d.exceptions().members())
                for (Default x : g.defaults()) checkDefault(x, s);
    }

    /** A value position and everything below it. Exhaustive over the sealed union, so a node the union grows is a
     *  compile error here rather than a subtree the walk skips. */
    private void checkValue(Expr e, ObjectType s) {
        // IR-SPEC ch. 4.1a: an anchored node is read where the form's own condition or value would read it
        if (e != null && anchored.contains(e)) s = formSubject;
        switch (e) {
            case null -> { }
            case Expr.AttributeReference a -> {
                ObjectType ot = a.objectType();
                if (ot != null && a.attribute() != null && ot.attribute(a.attribute()) == null
                        && ot.characteristicName(a.attribute()) != null)
                    throw new DocumentError(sites.get(a), "'" + a.attribute() + "' is a characteristic of '" + ot.name
                            + "', not an attribute — a characteristic endpoint is ill-formed in a value position");
                // WF-053, IR-SPEC ch. 4.1a: a read without bindings is the subject's own, so it names the subject
                // type itself — a supertype is another type, even where it declares the member. Both branches
                // refused such a read before, with a generic reason; refused here in the sentence ref and the
                // verifier share. A subject-less position (a day-kind definition) anchors nothing.
                if (s != null && ot != null && !navigationBindings.containsKey(a)
                        && !Namen.norm(ot.name).equals(Namen.norm(s.name)))
                    throw new DocumentError(sites.get(a), "an attribute reference without a path is the subject's"
                            + " own read, so its subjectType is the subject type '" + s.name + "' — '"
                            + ot.name + "' names another type, which only a navigation reaches");
            }
            case Expr.FunctionCall f -> {
                boolean setSource = f.elementObjectType() != null && f.arguments().size() == 1
                        && (roleBindings.containsKey(f) || sourcePaths.containsKey(f));
                if (setSource) checkValueFoldSource(f, s);
                else checkOwnTotal(f, s);
                if (!setSource) for (Expr x : f.arguments()) checkValue(x, s);
                checkValue(f.emptyFallback(), s);
                checkCondition(f.filter(), f.elementObjectType() != null ? f.elementObjectType() : s);
            }
            case Expr.Binary b -> { checkValue(b.left(), s); checkValue(b.right(), s); }
            case Expr.Rounding x -> checkValue(x.base(), s);
            case Expr.Clamp x -> {
                checkValue(x.base(), s); checkValue(x.minimum(), s); checkValue(x.maximum(), s);
            }
            case Expr.TextComposition t -> {
                for (Expr.TextComposition.Part d : t.parts())
                    if (d instanceof Expr.TextComposition.EmbeddedExpression i) checkValue(i.expr(), s);
            }
            case Expr.Let l -> { checkValue(l.value(), s); checkValue(l.body(), s); }
            case Expr.IfThenElse k -> {
                checkCondition(k.condition(), s); checkValue(k.then(), s); checkValue(k.otherwise(), s);
            }
            case Expr.InstanceSetUnion u -> {
                for (Expr.InstanceSetUnion.Branch t : u.branches()) checkCondition(t.filter(), t.elementType());
            }
            // a dimension cell reads a dimensioned attribute, which no characteristic is
            case Expr.DimensionCellRead c -> { }
            case Expr.Literal x -> { }
            case Expr.ParameterRef x -> { }
            case Expr.VariableRef x -> { }
            case Expr.BinderRef x -> { }
            case Expr.Constant x -> { }
            case Expr.Unresolved x -> { }
        }
    }

    private void checkCondition(Condition c, ObjectType s) {
        switch (c) {
            case null -> { }
            case Condition.Comparison v -> { checkValue(v.left(), s); checkValue(v.right(), s); }
            case Condition.Predicate p -> checkValue(p.operand(), s);
            case Condition.TimeWindow p -> { checkValue(p.from(), s); checkValue(p.until(), s); }
            case Condition.Compound x -> { for (Condition o : x.components()) checkCondition(o, s); }
            case Condition.QuantifierFold kv -> {
                ObjectType es = kv.elementObjectType() != null ? kv.elementObjectType() : s;
                checkCondition(kv.filter(), es);
                checkCondition(kv.test(), es);
            }
            case Condition.RoleMembership r -> checkFilling(r, s);
            case Condition.CharacteristicCheck k -> { }
            case Condition.IdentityCheck x -> { }
            case Condition.RuleStatusCheck x -> { }
            case Condition.Unresolved x -> { }
            // this loader refuses the node itself, so no document it returns carries one
            case Condition.FarRoleMembership x -> { }
        }
    }

    /** WF-034: the subject fills the role the check names — one-sided, through the supertype chain (ch. 2.6), as
     *  every near-side question is asked. A subject-less position (a day-kind definition) has nothing to cross. */
    private void checkFilling(Condition.RoleMembership r, ObjectType s) {
        if (s == null) return;
        RoleBindingRef rb = roleBindings.get(r);
        RegelSpraakModel.Role role = roleByName(factTypeByName(rb.factType()), rb.role());
        ObjectType filler = ot(role.objectTypeName());
        if (filler != null && !s.isA(filler))
            throw new DocumentError(sites.get(r) + "/role", "role check: the subject \"" + s.name
                    + "\" does not fill the role \"" + rb.role() + "\" of \"" + rb.factType()
                    + "\", so no link of it can hold the subject there");
    }

    /** The declared fact type of that name in the WF-001 form (case and runs of blanks folded), or null. */
    private FactType factTypeByName(String name) {
        if (name == null) return null;
        String n = Namen.norm(name);
        for (FactType ft : m.factTypes.values()) if (n.equals(Namen.norm(ft.name))) return ft;
        return null;
    }

    /** The role of {@code ft} with that DECLARED name (the plural does not name a role here), in the WF-001 form,
     *  or null. */
    private static RegelSpraakModel.Role roleByName(FactType ft, String name) {
        if (ft == null || name == null) return null;
        String n = Namen.norm(name);
        for (RegelSpraakModel.Role r : ft.roles) if (r.name() != null && n.equals(Namen.norm(r.name()))) return r;
        return null;
    }

    /** The other role of a two-role fact type, or null when it has not exactly two. */
    private static RegelSpraakModel.Role otherRole(FactType ft, RegelSpraakModel.Role r) {
        if (ft.roles.size() != 2) return null;
        return ft.roles.get(0) == r ? ft.roles.get(1) : ft.roles.get(0);
    }

    /** The key set of a container object or an M1 node, checked as the OCaml loader's {@code check_keys} checks it:
     *  the node discriminator when {@code expectedKind} names one, no key outside the node's own, every required key
     *  present, and an optional key written whole or not at all — present and null is neither (IR-SPEC § Method).
     *  Read leniently, each of these was a document part ignored or a null taken for an absent field. */
    private static void fields(JsonNode n, String path, String expectedKind, List<String> required, List<String> optional) {
        if (n == null || !n.isObject())
            throw new DocumentError(path, "expected a JSON object"
                    + (expectedKind == null ? "" : " (" + expectedKind + ")") + ", got " + shortQuote(n));
        if (expectedKind != null) {
            JsonNode category = n.get("node");
            if (category == null)
                throw new DocumentError(path, "missing field 'node' (the discriminator \"" + expectedKind + "\")");
            if (!category.isTextual() || !expectedKind.equals(category.asText()))
                throw new DocumentError(path + "/node", "expected \"" + expectedKind + "\", got " + shortQuote(category));
        }
        n.fieldNames().forEachRemaining(k -> {
            if (!required.contains(k) && !optional.contains(k))
                throw new DocumentError(path + "/" + k, "unknown field" + (expectedKind == null ? "" : " on " + expectedKind));
        });
        for (String k : required)
            if (!n.has(k)) throw missing(path, k);
        for (String k : optional)
            if (n.has(k) && n.get(k).isNull())
                throw new DocumentError(path + "/" + k, "an optional field is written whole or not at all, and null is neither");
    }

    /** Record the JSON pointer of every container below {@code n} (RFC 6901: '~' and '/' in a key escaped). */
    private void assignPaths(JsonNode n, String path) {
        if (n == null) return;
        if (n.isObject()) {
            paths.put(n, path);
            order.put(n, order.size());
            for (Map.Entry<String, JsonNode> e : n.properties())
                assignPaths(e.getValue(), path + "/" + e.getKey().replace("~", "~0").replace("/", "~1"));
        } else if (n.isArray()) {
            paths.put(n, path);
            for (int i = 0; i < n.size(); i++) assignPaths(n.get(i), path + "/" + i);
        }
    }

    /** The pointer of a container this document holds. */
    private String pathOf(JsonNode n) {
        String p = paths.get(n);
        if (p == null) throw new IllegalStateException("IrLoader: a node outside the document it is reading: " + shortQuote(n));
        return p;
    }

    /** An absent field, named at the object {@code path} that lacks it, as the OCaml loader's {@code field} names it:
     *  a pointer to the field itself would point at nothing in the document. */
    private static DocumentError missing(String path, String fieldName) {
        return new DocumentError(path, "missing field '" + fieldName + "'");
    }

    private static JsonNode section(JsonNode parent, String path, String name) {
        JsonNode n = parent.get(name);
        if (n == null) throw missing(path, name);
        if (!n.isObject()) throw new DocumentError(path + "/" + name, "expected an object, got " + shortQuote(n));
        return n;
    }

    /** A list the schema requires: an array, each entry read with its own position. The lenient reading took a
     *  non-array for an empty list, so {@code objectTypes: {}} loaded as a document without object types. */
    private static void list(JsonNode parent, String path, String fieldName, java.util.function.BiConsumer<String, JsonNode> f) {
        JsonNode a = parent.get(fieldName);
        String p = path + "/" + fieldName;
        if (a == null) throw missing(path, fieldName);
        if (!a.isArray()) throw new DocumentError(p, "expected an array, got " + shortQuote(a));
        for (int i = 0; i < a.size(); i++) f.accept(p + "/" + i, a.get(i));
    }

    /** The list form writes each name once, on its node, so a nameless entry has no name at all (the v0 map key
     *  that once stood in for it is gone), and a repeated name would silently replace the declaration before it. */
    private static String name(JsonNode n, String path, String fieldName) {
        JsonNode v = n.get(fieldName);
        if (v == null) throw new DocumentError(path, "a declaration without a name: missing field '" + fieldName + "'");
        if (!v.isTextual())
            throw new DocumentError(path + "/" + fieldName, "a declaration without a name: expected a string, got " + shortQuote(v));
        return v.asText();
    }

    private static String textAt(JsonNode n, String path, String fieldName) {
        JsonNode v = n.get(fieldName);
        if (v == null) throw missing(path, fieldName);
        if (!v.isTextual()) throw new DocumentError(path + "/" + fieldName, "expected a string, got " + shortQuote(v));
        return v.asText();
    }

    /** An optional string field: absent is null; present, it is a string ({@link #fields} has refused a null). */
    private static String optText(JsonNode n, String path, String fieldName) {
        return n.has(fieldName) ? textAt(n, path, fieldName) : null;
    }

    private static boolean truth(JsonNode n, String path, String fieldName) {
        JsonNode v = n.get(fieldName);
        if (v == null) throw missing(path, fieldName);
        if (!v.isBoolean()) throw new DocumentError(path + "/" + fieldName, "expected a boolean, got " + shortQuote(v));
        return v.booleanValue();
    }

    private static List<String> textList(JsonNode n, String path, String fieldName) {
        JsonNode a = n.get(fieldName);
        if (a == null) throw missing(path, fieldName);
        if (!a.isArray()) throw new DocumentError(path + "/" + fieldName, "expected an array, got " + shortQuote(a));
        List<String> out = new ArrayList<>();
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).isTextual())
                throw new DocumentError(path + "/" + fieldName + "/" + i, "expected a string, got " + shortQuote(a.get(i)));
            out.add(a.get(i).asText());
        }
        return out;
    }

    /** An integer field, as the OCaml loader's {@code int_} reads it: a JSON integer and nothing else, since
     *  {@code "2"} and {@code 2.0} both read as 2 under {@code asInt}, a value the document did not write as an
     *  integer. An integer beyond a Java {@code int} is well-formed (the OCaml loader reads it into its 63-bit
     *  int) and this backend cannot hold it: a form not carried, so a {@link Refusal} under {@code formName}. */
    private static int whole(JsonNode n, String path, String fieldName, String formName) {
        JsonNode c = n.get(fieldName);
        String p = path + "/" + fieldName;
        if (c == null) throw missing(path, fieldName);
        if (!c.isIntegralNumber()) throw new DocumentError(p, "expected an integer, got " + shortQuote(c));
        if (!c.canConvertToInt())
            throw new Refusal(DroolsCode.INTEGER_OUTSIDE_RANGE.reason("form", formName, "value", c, "path", p, "lowerBound", Integer.MIN_VALUE, "upperBound", Integer.MAX_VALUE), formName);
        return c.intValue();
    }

    /** WF-001: a namespace holds a name once, under the verifier's name identity ({@link Namen#norm}, the same
     *  function both transpiler branches compare declared names with), so this loader refuses the collisions
     *  WF-001/WF-002 refuse and no others. The model's tables are keyed on the exact string and look a name up
     *  case-insensitively after an exact miss, so two spellings of one name would both enter and a lookup would
     *  pick one of them. */
    private static <V> void once(Map<String, V> table, String path, String name, V value) {
        String earlier = collision(table.keySet(), name);
        if (earlier != null) throw new DocumentError(path, duplicate(name, earlier));
        table.put(name, value);
    }

    /** The declared name {@code name} collides with under {@link Namen#norm}, or null. */
    private static String collision(java.util.Collection<String> names, String name) {
        String key = Namen.norm(name);
        for (String n : names) if (Namen.norm(n).equals(key)) return n;
        return null;
    }

    private static String duplicate(String name, String earlier) {
        return name.equals(earlier)
                ? "the name '" + name + "' is declared twice"
                : "the name '" + name + "' collides with the earlier '" + earlier
                        + "' (names are compared case-insensitively)";
    }

    /** A JSON value as a message quotes it, cut short: the value is evidence, not the document. */
    private static String shortQuote(JsonNode n) {
        String s = String.valueOf(n);
        return s.length() > 60 ? s.substring(0, 60) + "…" : s;
    }

    // --- the side table: vocabularies (IR-SPEC ch. 2.12) and origins (ch. 2.13)

    /** The root list {@code vocabularies} into the model. A side table without semantics: read as strictly as every
     *  node here, in the shared loader's sentences, and never read for evaluation; what a vocabulary or a designation
     *  says is the verifier's (WF-116 to WF-120), so nothing here refuses it. */
    private void vocabularies(JsonNode root) {
        for (JsonNode v : extList(root, "", "vocabularies", "a document's vocabularies")) {
            String p = pathOf(v);
            fields(v, p, "Vocabulary", List.of("node", "name", "default"), List.of("language"));
            m.vocabularies.add(new RegelSpraakModel.Vocabulary(textAt(v, p, "name"), truth(v, p, "default"),
                    optText(v, p, "language")));
        }
    }

    /** The origin of the declaration {@code n} at {@code path} (IR-SPEC ch. 2.13), into the model's table
     *  {@code gegenereerd} under {@code element}: read as strictly as every node here, and the form and the kind
     *  refused outside their closed sets in WF-121 (a)'s sentence, as the shared loader refuses them for every
     *  executor, since the model cannot hold a value its set does not name. What the source names — whether it resolves,
     *  whether it fits the form — is the verifier's (WF-121 (b) to (d)), so nothing here refuses it. */
    private void origin(JsonNode n, String path, RegelSpraakModel.Designatable element) {
        if (!n.has("origin")) return;
        String p = path + "/origin";
        JsonNode o = n.get("origin");
        fields(o, p, null, List.of("form", "from"), List.of());
        String vp = p + "/from";
        JsonNode van = o.get("from");
        fields(van, vp, null, List.of("kind", "name"), List.of("owner"));
        String formName = textAt(o, p, "form");
        RegelSpraakModel.OriginForm hv = Spelling.ORIGIN_FORM.value("Origin.form", formName);
        if (hv == null)
            throw new DocumentError(p + "/form", "the origin form '" + formName + "' is none of the forms of IR-SPEC ch. 2.13 ("
                    + enumerated(Spelling.ORIGIN_FORM.rows().values()) + ")");
        String kind = textAt(van, vp, "kind");
        RegelSpraakModel.Designatable.Kind originKind = Spelling.originKindFromWire(kind);
        if (originKind == null) {
            List<String> kinds = new ArrayList<>(Spelling.ORIGIN_KIND.rows().values());
            kinds.add(Spelling.ORIGIN_DECISION_TABLE);
            throw new DocumentError(vp + "/kind", "the origin's source kind '" + kind
                    + "' is none of the kinds of IR-SPEC ch. 2.13 (" + enumerated(kinds) + ")");
        }
        m.generated.put(element, new RegelSpraakModel.GeneratedFrom(
                new RegelSpraakModel.Designatable(originKind, optText(van, vp, "owner"), textAt(van, vp, "name")), hv));
    }

    /** The values, each quoted, joined as the shared loader joins them in WF-121 (a)'s sentence. */
    private static String enumerated(java.util.Collection<String> values) {
        return String.join(", ", values.stream().map(w -> "'" + w + "'").toList());
    }

    /** The designations of the declaration {@code n} at {@code path}, into the model's side table under {@code element}
     *  (a list that is absent carries none; a present one holds at least one). */
    private void designations(JsonNode n, String path, RegelSpraakModel.Designatable element) {
        List<RegelSpraakModel.Designation> list = new ArrayList<>();
        for (JsonNode b : extList(n, path, "designations", "a declaration's designations")) {
            String p = pathOf(b);
            fields(b, p, null, List.of("vocabulary", "designation"), List.of());
            list.add(new RegelSpraakModel.Designation(textAt(b, p, "vocabulary"), textAt(b, p, "designation")));
        }
        if (!list.isEmpty()) m.designations.computeIfAbsent(element, k -> new ArrayList<>()).addAll(list);
    }

    /** An enumeration's {@code valueDesignations}: each entry to the first value its {@code value} names under the name
     *  identity; an entry that names none (WF-117's) keeps the value as written, which names no declaration. */
    private void valueDesignations(JsonNode dt, String path, String domain, List<String> values) {
        for (JsonNode e : extList(dt, path, "valueDesignations", "an enumeration's value designations")) {
            String p = pathOf(e);
            fields(e, p, null, List.of("value", "vocabulary", "designation"), List.of());
            String value = textAt(e, p, "value");
            String declared = values.stream().filter(w -> Namen.norm(w).equals(Namen.norm(value)))
                    .findFirst().orElse(value);
            m.designations.computeIfAbsent(RegelSpraakModel.Designatable.enumerationValue(domain, declared),
                    k -> new ArrayList<>()).add(new RegelSpraakModel.Designation(textAt(e, p, "vocabulary"),
                    textAt(e, p, "designation")));
        }
    }

    /** A list the side table writes whole or not at all, in the shared loader's sentence for an empty one. */
    private List<JsonNode> extList(JsonNode parent, String path, String fieldName, String description) {
        JsonNode a = parent.get(fieldName);
        if (a == null) return List.of();
        String p = path + "/" + fieldName;
        if (!a.isArray()) throw new DocumentError(p, "expected an array, got " + shortQuote(a));
        if (a.isEmpty()) throw new DocumentError(p, description + " are at least one, or the field is absent");
        List<JsonNode> out = new ArrayList<>();
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).isObject()) throw new DocumentError(p + "/" + i, "expected a JSON object, got " + shortQuote(a.get(i)));
            out.add(a.get(i));
        }
        return out;
    }

    // --- prioritized-defaults extension (ch. 7) ----------------------------

    private PrioritizedDefaults prioritizedDefaults(JsonNode n) {
        fields(n, pathOf(n), "PrioritizedDefaults", List.of("node", "name", "subject", "defaults", "validity"),
                List.of("characteristic", "attribute", "unordered", "source", "origin", "designations"));
        if (text(n, "name") != null)
            origin(n, pathOf(n), RegelSpraakModel.Designatable.form(text(n, "name")));
            designations(n, pathOf(n), RegelSpraakModel.Designatable.form(text(n, "name")));
        String subject = text(n, "subject");
        String characteristic = text(n, "characteristic");
        String attribute = text(n, "attribute");
        if (subject == null) throw new Refusal(DroolsCode.PRIORITIZED_DEFAULTS_WITHOUT_SUBJECT.reason(), "PrioritizedDefaults");
        if ((characteristic == null) == (attribute == null))
            throw new Refusal(DroolsCode.PRIORITIZED_DEFAULTS_ONE_TARGET.reason(), "PrioritizedDefaults");
        List<Default> defaults = new ArrayList<>();
        elements(n, "defaults", r -> defaults.add(dflt(r)));
        return new PrioritizedDefaults(text(n, "name"), subject, characteristic, attribute, bool(n, "unordered"),
                defaults, sourceText(n), parsedValidity(n, "name"));
    }

    private Default dflt(JsonNode n) {
        fields(n, pathOf(n), "Default", List.of("node"),
                List.of("name", "decision", "justification", "consequence", "initialisation", "exceptions", "source",
                        "statuteAnnotations"));
        // an absent decision or justification is what the v0 null was; a null there velden has refused
        JsonNode b = n.get("decision");
        Boolean decision = b == null ? null : b.asBoolean();
        // the block's statute annotations, carried through as a rule's are (IR-SPEC ch. 6.1); no executor reads them
        List<String> annotations = n.has("statuteAnnotations")
                ? textList(n, pathOf(n), "statuteAnnotations") : List.of();
        return new Default(decision, condition(nodeField(n, "justification")), expr(nodeField(n, "consequence")),
                bool(n, "initialisation"), exceptions(nodeField(n, "exceptions")), sourceText(n),
                optText(n, pathOf(n), "name"), annotations);
    }

    private Default.Exceptions exceptions(JsonNode n) {
        if (n == null || n.isNull()) return null;
        fields(n, pathOf(n), null, List.of("unordered", "members"), List.of());
        List<ExceptionGroup> members = new ArrayList<>();
        elements(n, "members", s -> members.add(exceptionGroup(s)));
        return new Default.Exceptions(bool(n, "unordered"), members);
    }

    private ExceptionGroup exceptionGroup(JsonNode n) {
        fields(n, pathOf(n), "ExceptionGroup", List.of("node", "defaults"), List.of("source"));
        List<Default> defaults = new ArrayList<>();
        elements(n, "defaults", r -> defaults.add(dflt(r)));
        return new ExceptionGroup(defaults, sourceText(n));
    }

    /** Bind every declared supertype reference, and refuse the two shapes this backend does not carry.
     *  WF-103/104 already reject an unknown or cyclic parent at the document, so both guards here
     *  are a last line: a loader that walked a cycle would hang instead of reporting.
     *
     *  <p>Until 2026-09-12 a third guard refused every chain carrying a temporal OR dimensioned member,
     *  because the facet SIDE MAPS were read per declared type at 122 reads (121 lines) in both transpiler branches
     *  and an inherited timeline would answer "not temporal" for the subtype. Those reads go through the
     *  chain-closed accessors of {@link ObjectType} now, so the broad refusal is gone and the whole
     *  combination plays. Measured at the same card: the DIMENSIONED half was over-broad even then —
     *  every dimension read already went through {@code dimensionsOf}, which has walked the chain since
     *  the kernel. What is left of it is the §9.9 startpunt-ANCHOR family, refused in
     *  {@link #refuseInheritedAnchor} — not here, because it needs the tijdlijn list, which is read later. */
    private void bindSupertypes() {
        supertypeName.forEach((ot, name) -> {
            if (name == null) return;
            // under WF-103's name identity, the one WF-001 declared the names under: the model's own lookup
            // folds case only, and would call "Jutter " undeclared where the verifier, ref and clingo bind it
            String declared = collision(m.objectTypes.keySet(), name);
            ObjectType parent = declared == null ? null : m.objectTypes.get(declared);
            if (parent == null)
                throw new DocumentError(supertypePath.get(ot), "supertype '" + name + "' is not declared");
            ot.supertype = parent;
        });
        for (ObjectType ot : m.objectTypes.values()) {
            java.util.Set<ObjectType> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            for (ObjectType t = ot; t != null; t = t.supertype)
                if (!seen.add(t))
                    throw new DocumentError(supertypePath.get(ot), "supertype cycle at '" + ot.name + "'");
        }
    }

    /** The §9.9 startpunt-ANCHOR family — what is LEFT of the withdrawn temporal/dimensioned refusal,
     *  and the one part of it that really is chain-blind. The family is not a member lookup but a
     *  PROTOCOL keyed on the DECLARED SUBJECT NAME: a startpuntbepaling declares a synthetic attribute
     *  on a named objecttype, and {@code hasVariableStart}, {@code startPointMember} and
     *  {@code isStartPointMember} all key on that name (see the note at
     *  {@link RegelSpraakModel#hasVariableStart}: resolving one of the four through the chain only
     *  moves the seam). Two sites, and they fall independently:
     *
     *  <p>(a) an ancestor declares an attribute riding a named tijdlijn WITH A VARIABEL STARTPUNT.
     *  {@code hasVariableStart} then answers false for the subtype where the truth is true, so the
     *  bucketing is skipped. The guard asks {@code hasVariableStart} itself rather than testing
     *  {@code attributeTimelineName} for emptiness, because on a FIXED-startpunt tijdlijn that predicate
     *  answers false from the own map and from the chain alike — there is no blindness there, and
     *  refusing it would print a reason ("reads it as unanchored") that is simply true of the document.
     *  That narrowness is what makes this guard need the tijdlijn list, hence its place here.
     *
     *  <p>(b) a startpuntbepaling declared on an ANCESTOR. {@code isStartPointMember} is asked of the type
     *  the REFERENCE names and compares the determination's declared subject name, so for every subtype
     *  instance it answers no and the §6.8 leeg-fout companion silently disappears for exactly those.
     *
     *  <p>WF-016 ties the two together on conforming input without merging them: a variable-start
     *  tijdlijn must have SOME determination, but nothing makes that determination name the type that
     *  declares the attribute — so (a) fires alone when it names another type, and (b) fires alone when
     *  no attribute rides the tijdlijn at all. */
    private void refuseInheritedAnchor() {
        for (ObjectType ot : m.objectTypes.values()) {
            if (ot.supertype == null) continue;
            for (ObjectType t = ot.supertype; t != null; t = t.supertype) {       // STRICTLY above ot
                final ObjectType ancestor = t;
                for (String attr : ancestor.attributeTimelineName.keySet())
                    if (m.hasVariableStart(ancestor, attr))
                        throw new Refusal(DroolsCode.INHERITED_VARIABLE_START_TIMELINE.reason("attribute", attr, "objectType", ot.name), "Objecttype.supertype");
                if (m.resolvedStartPointDeterminations.stream().anyMatch(sp -> ancestor.name.equals(sp.objectType())))
                    throw new Refusal(DroolsCode.START_POINT_ON_ANCESTOR.reason("objectType", ot.name), "Objecttype.supertype");
            }
        }
    }

    /** The node's form as the schema gives it: the one wire severity, a message string, the line an integer. The
     *  severities WF-113 (a) names — error, info and none at all — are refused in the rule's sentences, as the
     *  shared loader refuses them for an executor; what WF-113 demands beyond that (a message that is not blank, a
     *  line of at least 0) is the verifier's to refuse, and no consumer reads a diagnostic for evaluation. */
    private static Diagnostic diagnostic(String path, JsonNode n) {
        fields(n, path, "Diagnostic", List.of("node", "severity", "message", "line"), List.of());
        String sp = path + "/severity";
        if (n.get("severity").isNull()) throw new DocumentError(sp, SEVERITY_MISSING);
        String w = textAt(n, path, "severity");
        if (Spelling.SEVERITY.refusesByName("Diagnostic.severity", w))
            throw new DocumentError(sp, severityRefusal(refused(Spelling.SEVERITY, w)));
        return new Diagnostic(enumValue(Spelling.SEVERITY, "Diagnostic.severity", w, sp),
                textAt(n, path, "message"), whole(n, path, "line", "Diagnostic.line"));
    }

    /** WF-113 (a), the verifier's sentences for the two severities the schema does not admit: an executor refuses
     *  them at load, as the shared loader does. */
    static String severityRefusal(Severity s) {
        return switch (s) {
            case ERROR -> "severity error is not a wire severity: an error means the producer refused to write the"
                    + " document, so a document that was written carries none";
            case INFO -> "severity info is not a wire severity: the list is the account of what stayed unresolved,"
                    + " and info is tooling talk";
            case WARNING -> throw new IllegalStateException("IrLoader: warning is the one wire severity");
        };
    }

    /** WF-113 (a), the verifier's sentence for a diagnostic without a severity. */
    static final String SEVERITY_MISSING = "diagnostic without a severity: the one wire severity is warning";

    // --- declaration side --------------------------------------------------

    /** IR-SPEC ch. 2.2: the member nodes back into the model's member-keyed side-maps. Every facet's count is
     *  kept, 1 included, as the OCaml loader keeps it. The frontend's model records a count only when it is not 1,
     *  and the one reader of these tables, {@code Timelines.morePeriodic}, takes an absent entry and an explicit
     *  1 for the same single-period facet, so keeping the 1 moves no outcome; what it adds is that a regression to
     *  a presence test fails on every temporal golden instead of on none ({@code MorePeriodicGateWitnessTest}).
     *  A characteristic's kind is required on the wire since M1; the emitter writes nominal for an untagged one
     *  (ch. 2.2: untagged = nominal). */
    private ObjectType objectType(String path, JsonNode n) {
        fields(n, path, "ObjectType", List.of("node", "name", "animate", "attributes", "characteristics"),
                List.of("supertype", "plural", "origin", "designations"));
        ObjectType ot = new ObjectType(name(n, path, "name"));
        origin(n, path, RegelSpraakModel.Designatable.objectType(ot.name));
        designations(n, path, RegelSpraakModel.Designatable.objectType(ot.name));
        ot.animate = truth(n, path, "animate");
        ot.plural = optText(n, path, "plural");
        supertypeName.put(ot, optText(n, path, "supertype"));   // resolved after every declaration is read
        supertypePath.put(ot, path + "/supertype");
        list(n, path, "attributes", (ap, a) -> {
            fields(a, ap, "Attribute", List.of("node", "name", "datatype"),
                    List.of("plural", "dimensions", "timeline", "origin", "designations"));
            String member = name(a, ap, "name");
            origin(a, ap, RegelSpraakModel.Designatable.attribute(ot.name, member));
            designations(a, ap, RegelSpraakModel.Designatable.attribute(ot.name, member));
            String earlier = collision(ot.attributes.keySet(), member);
            if (earlier != null) throw new DocumentError(ap + "/name", duplicate(member, earlier));
            // null is admitted here and refused by the verifier under WF-059, which names it
            JsonNode dt = a.get("datatype");
            if (!dt.isNull() && !dt.isObject())
                throw new DocumentError(ap + "/datatype", "expected a datatype object or null, got " + shortQuote(dt));
            ot.attributes.put(member, datatype(dt, ap + "/datatype", true));
            String plural = optText(a, ap, "plural");
            if (plural != null) ot.attributePlural.put(member, plural);
            if (a.has("dimensions")) ot.attributeDimensions.put(member, textList(a, ap, "dimensions"));
            if (a.has("timeline")) {
                String tp = ap + "/timeline";
                JsonNode tl = a.get("timeline");
                fields(tl, tp, "TimelineFacet", List.of("node", "granularity", "count"),
                        List.of("fixedStart", "timelineName"));
                Granularity gran = enumValue(Spelling.GRANULARITY, "TimelineFacet.granularity",
                        textAt(tl, tp, "granularity"), tp + "/granularity");
                ot.attributeGranularity.put(member, gran);
                int facetCount = whole(tl, tp, "count", "TimelineFacet.count");
                ot.attributeTimelineCount.put(member, facetCount);
                String fixed = fixedStartPoint(tl, tp, gran, facetCount);
                if (fixed != null) ot.attributeFixedStartPoint.put(member, fixed);
                String timelineName = optText(tl, tp, "timelineName");
                if (timelineName != null) {
                    ot.attributeTimelineName.put(member, timelineName);
                    overnames.add(new Adoption(tp + "/timelineName", timelineName, fixed));
                }
            }
        });
        list(n, path, "characteristics", (cp, c) -> {
            fields(c, cp, "Characteristic", List.of("node", "name", "kind"), List.of("timeline", "origin", "designations"));
            String member = name(c, cp, "name");
            origin(c, cp, RegelSpraakModel.Designatable.characteristic(ot.name, member));
            designations(c, cp, RegelSpraakModel.Designatable.characteristic(ot.name, member));
            // WF-002: read leniently, a second declaration's kind replaced the first one's and its timeline did not
            String earlier = collision(ot.characteristics, member);
            if (earlier != null) throw new DocumentError(cp + "/name", duplicate(member, earlier));
            ot.characteristics.add(member);
            ot.characteristicKinds.put(member, enumValue(Spelling.CHARACTERISTIC_KIND, "Characteristic.kind",
                    textAt(c, cp, "kind"), cp + "/kind"));
            if (c.has("timeline")) {
                String tp = cp + "/timeline";
                JsonNode tl = c.get("timeline");
                fields(tl, tp, "TimelineFacet", List.of("node", "granularity", "count"), List.of("fixedStart"));
                Granularity gran = enumValue(Spelling.GRANULARITY, "TimelineFacet.granularity",
                        textAt(tl, tp, "granularity"), tp + "/granularity");
                ot.characteristicGranularity.put(member, gran);
                int facetCount = whole(tl, tp, "count", "TimelineFacet.count");
                ot.characteristicTimelineCount.put(member, facetCount);
                String fixed = fixedStartPoint(tl, tp, gran, facetCount);
                if (fixed != null) ot.characteristicFixedStartPoint.put(member, fixed);
            }
        });
        return ot;
    }

    /** IR-SPEC ch. 2.3: the wrapper carries the name, and a datatype at this declaration position carries no
     *  self-name — the model's Enumeratie record takes the wrapper's. A self-name left beside it is a second
     *  spelling of one name (delta B2), refused as an unknown field, on the two datatypes this backend does not
     *  carry as well: there the document is malformed before its form is refused. */
    private Datatype domain(String path, String name, JsonNode n) {
        String dp = path + "/datatype";
        JsonNode dt = n.get("datatype");
        if (!dt.isObject()) throw new DocumentError(dp, "expected a datatype object, got " + shortQuote(dt));
        String domainKind = datatypeKind(dt, dp);
        List<String> keyNames = DOMAIN_DATATYPE.get(domainKind);
        // the values' designations stand beside their list, at this declaration position only (IR-SPEC ch. 2.12)
        if (keyNames != null)
            fields(dt, dp, domainKind, keyNames, domainKind.equals("Enumeration") ? List.of("valueDesignations") : List.of());
        if (domainKind.equals("Enumeration")) {
            if (!dt.get("values").isArray())
                throw new DocumentError(dp + "/values", "expected an array, got " + shortQuote(dt.get("values")));
            List<String> values = stringList(dt.get("values"));
            valueDesignations(dt, dp, name, values);
            return new Datatype.Enumeration(name, values);
        }
        return datatype(dt, dp, true);
    }

    /** The key set of each datatype kind at the declaration position {@code Domain.datatype}, as the OCaml loader's
     *  {@code domain_datatype} and {@code datatype} check it: the three kinds that carry a domain name at a
     *  reference position carry none here, and every other kind has the keys it has everywhere ({@code Number} and
     *  {@code Date} are checked in the dispatch, with their optional keys). A kind outside the table reaches the
     *  dispatch. */
    private static final Map<String, List<String>> DOMAIN_DATATYPE = Map.ofEntries(
            Map.entry("Enumeration", List.of("node", "values")),
            Map.entry("SumType", List.of("node", "constructors")),
            Map.entry("StructType", List.of("node", "fields")),
            Map.entry("ListType", List.of("node", "element")),
            Map.entry("Text", List.of("node")),
            Map.entry("Boolean", List.of("node")));

    /** The key set of each datatype kind this backend carries without optional keys, at every position but a
     *  domain's declaration (see {@link #DOMAIN_DATATYPE}): an Enumeration names its domain here. */
    private static final Map<String, List<String>> DATATYPE = Map.ofEntries(
            Map.entry("Text", List.of("node")),
            Map.entry("Boolean", List.of("node")),
            Map.entry("Enumeration", List.of("node", "domain", "values")));

    /** IR-SPEC ch. 2.10: a parameter, its datatype, and when it is time-dependent its timeline facet. The datatype is
     *  required, and the facet carries no timeline name and a count of 1: a parameter's timeline is one period while
     *  multi-period grids are open (RUNTIME-SEMANTICS ch. 3.12), so another count is no document (WF-005), refused in
     *  the verifier's words. The model's parameter holds the granularity. */
    private Parameter parameter(String path, JsonNode n) {
        fields(n, path, "Parameter", List.of("node", "name", "datatype"), List.of("timeline", "origin", "designations"));
        String name = name(n, path, "name");
        origin(n, path, RegelSpraakModel.Designatable.parameter(name));
        designations(n, path, RegelSpraakModel.Designatable.parameter(name));
        JsonNode dt = n.get("datatype");
        if (!dt.isObject()) throw new DocumentError(path + "/datatype", "expected a datatype object, got " + shortQuote(dt));
        Datatype datatype = datatype(dt, path + "/datatype", true);
        Granularity granularity = null;
        String fixed = null;
        if (n.has("timeline")) {
            String tp = path + "/timeline";
            JsonNode tl = n.get("timeline");
            fields(tl, tp, "TimelineFacet", List.of("node", "granularity", "count"), List.of("fixedStart"));
            granularity = enumValue(Spelling.GRANULARITY, "TimelineFacet.granularity",
                    textAt(tl, tp, "granularity"), tp + "/granularity");
            int facetCount = whole(tl, tp, "count", "TimelineFacet.count");
            if (facetCount < 1)
                throw new DocumentError(tp + "/count", "the timeline facet of parameter '" + name + "' has count "
                        + facetCount + " < 1");
            if (facetCount > 1)
                throw new DocumentError(tp + "/count", "the timeline facet of parameter '" + name + "' has count "
                        + facetCount + " > 1: a parameter's timeline is one period while multi-period grids are open"
                        + " (RUNTIME-SEMANTICS ch. 3.12)");
            fixed = fixedStartPoint(tl, tp, granularity, facetCount);
        }
        return new Parameter(name, datatype, granularity, fixed);
    }

    /** IR-SPEC ch. 2.5: a unit system and its units, one Unit per abbreviation under the name identity of ch. 2.1
     *  (WF-009, the identity its canonical lookup resolves under). The model keeps every unit under its abbreviation,
     *  its further spellings in the alias map, and its exact factor beside the record's decimal one. */
    private UnitSystem unitSystem(String path, JsonNode n) {
        fields(n, path, "UnitSystem", List.of("node", "name", "units"), List.of());
        UnitSystem es = new UnitSystem(name(n, path, "name"));
        list(n, path, "units", (up, u) -> {
            Unit e = unit(u, up, es);
            if (collision(es.units.keySet(), e.abbreviation()) != null)
                throw new DocumentError(up + "/abbreviation", "the unit '" + e.abbreviation() + "' is declared twice in"
                        + " the unit system '" + es.name + "': one Unit per abbreviation");
            es.units.put(e.abbreviation(), e);
            if (u.has("spellings"))
                for (String spelling : textList(u, up, "spellings")) es.spellings.put(spelling, e);
        });
        return es;
    }

    /** A unit where it occurs — on a number, as a rate's denominator: IR-SPEC ch. 2.5's fields and nothing else, so
     *  a spelling there is an unknown field (spellings belong to the declaration). */
    private Unit unit(JsonNode n, String path) { return unit(n, path, null); }

    /** A unit, with {@code system} the unit system it is declared in, or null where it occurs. A declared unit may
     *  carry its further spellings, at least one; its exact factor lands in the system. */
    private Unit unit(JsonNode n, String path, UnitSystem system) {
        // a factor written null is a factor in no form, refused in the factor's own sentence
        if (n.isObject() && n.has("factorToCanonical") && n.get("factorToCanonical").isNull())
            throw new DocumentError(path + "/factorToCanonical", FACTOR_FORM + "null");
        fields(n, path, "Unit", List.of("node", "abbreviation"), system == null
                ? List.of("system", "canonical", "factorToCanonical", "denominator")
                : List.of("system", "canonical", "factorToCanonical", "denominator", "spellings"));
        String abbreviation = textAt(n, path, "abbreviation");
        UnitSystem.Factor factor = n.has("factorToCanonical")
                ? factor(n.get("factorToCanonical"), path + "/factorToCanonical") : null;
        Unit denominator = n.has("denominator") ? unit(n.get("denominator"), path + "/denominator") : null;
        if (system != null && n.has("spellings") && textList(n, path, "spellings").isEmpty())
            throw new DocumentError(path + "/spellings", SPELLINGS_EMPTY);
        if (system != null && factor != null) system.exactFactors.put(abbreviation, factor);
        return new Unit(abbreviation, optText(n, path, "system"), optText(n, path, "canonical"),
                factor == null ? null : decimal(factor), denominator);
    }

    /** The shared loader's sentence for an empty list of spellings. */
    static final String SPELLINGS_EMPTY = "a unit's spellings are at least one spelling, or the field is absent";

    /** The exact factor (IR-SPEC ch. 2.5): an object of exactly the two integers {@code num} and {@code den}, each at
     *  least 1 and at most 2^62 - 1, the schema's bound and the shared loader's native integer range. Every other form
     *  — a JSON number, a string, a missing or unknown key, a value below 1 or above the bound — is refused in the
     *  shared loader's one sentence. That the fraction is in lowest terms is the verifier's WF-092, not a load
     *  question, so {2, 4} is read as 1/2, as the shared loader reads it for {@code ref} and the ASP backend. */
    private static UnitSystem.Factor factor(JsonNode n, String path) {
        if (n.isObject() && n.size() == 2 && n.has("num") && n.has("den")) {
            BigInteger num = wholeFromOne(n.get("num")), den = wholeFromOne(n.get("den"));
            if (num != null && den != null) return new UnitSystem.Factor(num, den);
        }
        throw new DocumentError(path, FACTOR_FORM + n);
    }

    /** The shared loader's sentence for a factor in any other form than {@code {num, den}}; the value follows. */
    static final String FACTOR_FORM = "a conversion factor is an exact rational {num, den} whose two integers are at"
            + " least 1 and at most 4611686018427387903 (2^62 - 1), got ";

    private static BigInteger wholeFromOne(JsonNode c) {
        if (!c.isIntegralNumber()) return null;
        BigInteger v = c.bigIntegerValue();
        return v.signum() > 0 && v.bitLength() <= 62 ? v : null;
    }

    /** The record's decimal factor of an exact fraction: a whole number exactly, any other fraction rounded to 16
     *  significant digits, as the frontend rounds a declared "1/N" (see the class comment). */
    private static BigDecimal decimal(UnitSystem.Factor f) {
        return f.den().equals(BigInteger.ONE) ? new BigDecimal(f.num())
                : new BigDecimal(f.num()).divide(new BigDecimal(f.den()), MathContext.DECIMAL64);
    }

    /** IR-SPEC ch. 2.9: a day kind and the plural it takes as a duration unit. The model keeps the names, and a
     *  registry from each unit spelling of a day kind (lower-cased) to its name that the transpilers' day-kind
     *  recognition reads: the name and then the plural, the first to claim a spelling keeping it, as the frontend
     *  registers them. */
    private void dayKind(String path, JsonNode n) {
        fields(n, path, "DayKind", List.of("node", "name"), List.of("plural"));
        String name = textAt(n, path, "name");
        String plural = optText(n, path, "plural");
        m.dayKinds.add(name);
        m.dayKindPerUnit.putIfAbsent(name.trim().toLowerCase(), name);
        if (plural != null) m.dayKindPerUnit.putIfAbsent(plural.trim().toLowerCase(), name);
    }

    /** IR-SPEC ch. 2.6: a fact type and its roles. A role's plural is written whole or not at all: absent is the
     *  record's null. */
    private FactType factType(String path, JsonNode n) {
        fields(n, path, "FactType", List.of("node", "name", "reciprocal", "roles"), List.of("origin", "designations"));
        FactType ft = new FactType(name(n, path, "name"));
        ft.reciprocal = bool(n, "reciprocal");
        origin(n, path, RegelSpraakModel.Designatable.factType(ft.name));
        designations(n, path, RegelSpraakModel.Designatable.factType(ft.name));
        elements(n, "roles", r -> {
            fields(r, pathOf(r), "Role", List.of("node", "name", "objectType", "many"), List.of("plural", "origin", "designations"));
            ft.roles.add(new RegelSpraakModel.Role(
                    text(r, "name"), text(r, "objectType"), text(r, "plural"), bool(r, "many")));
            if (text(r, "name") != null) {
                origin(r, pathOf(r), RegelSpraakModel.Designatable.role(ft.name, text(r, "name")));
                designations(r, pathOf(r), RegelSpraakModel.Designatable.role(ft.name, text(r, "name")));
            }
        });
        return ft;
    }

    /** IR-SPEC ch. 2.7: a dimension and its labels. Its plural and its preposition are written whole or not at all:
     *  absent is the record's null. */
    private Dimension dimension(String path, JsonNode n) {
        fields(n, path, "Dimension", List.of("node", "name", "labels"), List.of("plural", "preposition"));
        return new Dimension(name(n, path, "name"), text(n, "plural"),
                text(n, "preposition"), stringList(n.get("labels")));
    }

    /** IR-SPEC ch. 2.8: a named timeline. Its granularity is the one field of the family that admits a null. Its
     *  fixed start point is refused in the verifier's WF-115 sentences: on a variable start (a), off its grid (b). */
    private Timeline timeline(JsonNode n) {
        String p = pathOf(n);
        fields(n, p, "Timeline", List.of("node", "name", "variableStart", "granularity", "count"), List.of("fixedStart"));
        Granularity granularity = n.get("granularity").isNull()
                && Spelling.GRANULARITY.allowsNull("Timeline.granularity") ? null
                : enumValue(Spelling.GRANULARITY, "Timeline.granularity", textAt(n, p, "granularity"),
                        p + "/granularity");
        String name = text(n, "name");
        boolean variable = bool(n, "variableStart");
        int facetCount = intOf(n, "count");
        String fixed = optText(n, p, "fixedStart");
        if (fixed != null && variable)
            throw new DocumentError(p + "/fixedStart", FixedStartPoint.refusalVariable(name == null ? "" : name, fixed));
        if (fixed != null && granularity != null) {
            String refusal = FixedStartPoint.refusal(granularity, facetCount, fixed);
            if (refusal != null) throw new DocumentError(p + "/fixedStart", refusal);
        }
        return new Timeline(name, variable, granularity, facetCount, fixed);
    }

    /** A facet's fixed start point, refused in the verifier's WF-115 (b) sentence when it does not fit the facet's
     *  grid; null when the facet carries none. */
    private static String fixedStartPoint(JsonNode facet, String tp, Granularity gran, int facetCount) {
        String fixed = optText(facet, tp, "fixedStart");
        if (fixed == null) return null;
        String refusal = FixedStartPoint.refusal(gran, facetCount, fixed);
        if (refusal != null) throw new DocumentError(tp + "/fixedStart", refusal);
        return fixed;
    }

    /** An attribute facet that names a timeline, kept until the timelines are read: WF-115 (c) crosses its fixed
     *  start point with the declaration's. */
    private record Adoption(String path, String timeline, String fixed) {}

    private final List<Adoption> overnames = new ArrayList<>();

    /** WF-115 (c), in the verifier's sentence and at its pointer: a facet that names a timeline carries that
     *  timeline's fixed start point, or none when it has none. A name that resolves to no timeline is WF-003's. */
    private void refuseAdoption() {
        for (Adoption o : overnames) {
            Timeline tl = m.timelineByName(o.timeline());
            if (tl != null && !java.util.Objects.equals(o.fixed(), tl.fixedStartPoint()))
                throw new DocumentError(o.path(), FixedStartPoint.refusalAdoption(o.fixed(), o.timeline(), tl.fixedStartPoint()));
        }
    }

    // --- Datatype ----------------------------------------------------------

    /** The datatype dispatch. {@code path} is the JSON pointer of the datatype; {@code declaration} says whether it
     *  stands at a declaration (the datatype of an attribute, a parameter or a domain) or at a type annotation (the
     *  {@code type} of an expression node), where a presentation tag is refused (WF-007). */
    private Datatype datatype(JsonNode n, String path, boolean declaration) {
        if (n == null || n.isNull()) return null;
        if (!n.isObject()) throw new DocumentError(path, "expected a datatype object or null, got " + shortQuote(n));
        String node = datatypeKind(n, path);
        List<String> keyNames = DATATYPE.get(node);
        if (keyNames != null) fields(n, path, node, keyNames, List.of());
        return switch (node) {
            case "Number" -> number(n, path, declaration);
            case "Date" -> {
                fields(n, path, "Date", List.of("node", "precision"), List.of());
                yield enumValue(Spelling.DATE_PRECISION, "Date.precision", textAt(n, path, "precision"),
                        path + "/precision");
            }
            case "Text" -> new Datatype.Text();
            case "Boolean" -> new Datatype.BooleanType();
            case "Enumeration" -> new Datatype.Enumeration(text(n, "domain"), stringList(n.get("values")));
            // the members of IR-SPEC ch. 2.4's datatype union that have no record here: a form this backend does not
            // carry
            case "ListType" -> throw new Refusal(DroolsCode.DATATYPE_LIST.reason("node", node), "Datatype:" + node);
            case "StructType" -> throw new Refusal(DroolsCode.DATATYPE_STRUCT.reason("node", node), "Datatype:" + node);
            case "SumType" -> throw new Refusal(DroolsCode.DATATYPE_SUM.reason("node", node), "Datatype:" + node);
            // any other name is no node of this format, refused at the discriminator in the shared loader's words:
            // Onbekend among them, because the union has no member for an unknown type (a marker leaves its type out
            // and a derived position the producer could not type is null), the four datatype nodes of the L2 shape,
            // which no document carries since L3, and the three of the v0 shape that L5 renamed
            default -> throw new DocumentError(path + "/node", "unknown datatype node \"" + node + "\"");
        };
    }

    /** IR-SPEC ch. 2.4: a number, its sign always written. One whose unit is '%' is a percentage, which the model
     *  holds as a {@link Datatype.Percentage}; that record has no sign, and the wire writes a percentage's sign
     *  unspecified. The presentation tag presents a percentage, stands at a declaration only and means nothing to
     *  evaluation. Every refusal here is the shared OCaml loader's or the verifier's, in their words. */
    private Datatype number(JsonNode n, String path, boolean declaration) {
        fields(n, path, "Number", List.of("node", "whole", "decimals", "sign"), List.of("unit", "presentation"));
        boolean whole = truth(n, path, "whole");
        int decimals = whole(n, path, "decimals", "Number.decimals");
        Datatype.Sign sign = enumValue(Spelling.NUMBER_SIGN, "Number.sign", textAt(n, path, "sign"), path + "/sign");
        Unit unit = n.has("unit") ? unit(n.get("unit"), path + "/unit") : null;
        String presentation = optText(n, path, "presentation");
        if (presentation != null && !presentation.equals("percentage"))
            throw new DocumentError(path + "/presentation", unknownValue("presentation", presentation));
        boolean percent = unit != null && unit.abbreviation().equals("%");
        if (percent && sign != Datatype.Sign.ONBEPAALD)
            throw new DocumentError(path + "/sign", PERCENT_WITH_SIGN);
        if (!percent && presentation != null) throw new DocumentError(path + "/presentation", TAG_WITHOUT_PERCENT);
        if (presentation != null && !declaration)
            throw new DocumentError(path + "/presentation", TAG_ON_ANNOTATION);
        return percent ? new Datatype.Percentage(whole, decimals, unit)
                : new Datatype.Numeric(whole, decimals, sign, unit);
    }

    /** The shared loader's sentence for a value outside an enumeration: its family's values, a v0 spelling
     *  included, or the one presentation tag. */
    static String unknownValue(String fieldName, String value) {
        return "unknown " + fieldName + " value \"" + value + "\"";
    }

    /** The in-memory value a wire spelling names at {@code place} (IR-SPEC, Conventions: every enumeration value is
     *  read through the one translation table), or the shared loader's refusal naming the field — the last segment
     *  of the place — and the value, a v0 spelling included. A value the place refuses by name never reaches here:
     *  its reader refuses it first, in the rule's sentence. */
    private static <K> K enumValue(Spelling.Family<K> family, String place, String value, String path) {
        K k = family.value(place, value);
        if (k != null) return k;
        if (family.refusesByName(place, value))
            throw new IllegalStateException("IrLoader: '" + value + "' at " + place
                    + " is refused by name by its reader");
        throw new DocumentError(path, unknownValue(place.substring(place.lastIndexOf('.') + 1), value));
    }

    /** The in-memory value of a spelling the table lists among those some place refuses by name. */
    private static <K> K refused(Spelling.Family<K> family, String spelling) {
        for (Map.Entry<K, String> e : family.refused().entrySet())
            if (e.getValue().equals(spelling)) return e.getKey();
        throw new IllegalStateException("IrLoader: '" + spelling + "' is no value the family " + family.name()
                + " refuses by name");
    }

    /** The shared loader's sentence: the percentage record has no sign, and the wire writes a percentage unsigned. */
    static final String PERCENT_WITH_SIGN =
            "a percentage carries no sign restriction: a number whose unit is '%' has the sign unspecified";
    /** The shared loader's sentence: the tag presents a percentage, and a number without the unit '%' is none. */
    static final String TAG_WITHOUT_PERCENT = "the presentation \"percentage\" on a number whose unit is not '%'";
    /** WF-007's presentation arm — the verifier's sentence. */
    static final String TAG_ON_ANNOTATION = "the presentation tag \"percentage\" on a type annotation: the tag is"
            + " written only where a datatype is declared";

    /** The {@code node} of a datatype object, required: a switch on an absent one ended in a NullPointerException
     *  that named no place in the document. */
    private static String datatypeKind(JsonNode n, String path) {
        JsonNode category = n.get("node");
        if (category == null) throw new DocumentError(path, "missing field 'node' (the datatype discriminator)");
        if (!category.isTextual()) throw new DocumentError(path + "/node", "expected a string, got " + shortQuote(category));
        return category.asText();
    }

    // --- the form anchor (IR-SPEC ch. 4.1a, WF-122) ----------------------------

    /** Records {@code node} as anchored where its wire node {@code n} at {@code p} carries {@code anchor}, whose one
     *  value is read as the shared loader reads it. Where it may stand is WF-122's, the verifier's question. */
    private void anchor(JsonNode n, String p, Object node) {
        if (anchorValue(n, p)) anchored.add(node);
    }

    /** Whether {@code n} at {@code p} carries {@code anchor}, refused in the shared loader's sentences where its value
     *  is not the string "form". */
    private boolean anchorValue(JsonNode n, String p) {
        JsonNode a = n.get("anchor");
        if (a == null) return false;
        if (!a.isTextual()) throw new DocumentError(p + "/anchor", "an anchor is the string \"form\"");
        if (!a.asText().equals("form")) throw new DocumentError(p + "/anchor", "unknown anchor value \"" + a.asText() + "\"");
        return true;
    }

    // --- the key sets of the rule-side nodes ----------------------------------

    /** A node's keys as the schema gives them: required, then optional (IR-SPEC § Method). */
    private record Keys(List<String> required, List<String> optional) {}

    private static Map.Entry<String, Keys> keyNames(String node, List<String> required, String... optional) {
        return Map.entry(node, new Keys(required, List.of(optional)));
    }

    /** Every expression node this loader reads, with the schema's keys; an instance-set union's selection is read
     *  inside its union. A node kind outside the table reaches the dispatch's refusal: a form this backend does not
     *  carry where the schema's expression union has the kind, no node of this format where it does not. The
     *  temporal read is such a form, and its keys stand here all the same: a key of its v0 shape is refused by name
     *  before the form is refused. */
    private static final Map<String, Keys> EXPRESSION = Map.ofEntries(
            keyNames("Literal", List.of("node", "value", "type"), "source"),
            keyNames("TextComposition", List.of("node", "parts", "type"), "source"),
            keyNames("Binary", List.of("node", "op", "left", "right", "type"), "source"),
            keyNames("Aggregation", List.of("node", "kind", "arguments", "type"), "elementType", "anchor", "filter",
                    "emptyFallback", "dimensionSelection", "sourceRole", "sourcePath", "source"),
            keyNames("BuiltinCall", List.of("node", "kind", "arguments", "type"), "filter", "granularity", "source"),
            keyNames("Rounding", List.of("node", "base", "mode", "decimals", "type"), "source"),
            keyNames("Clamp", List.of("node", "base", "type"), "minimum", "maximum", "source"),
            keyNames("AttributeReference", List.of("node", "attribute", "subjectType"), "anchor", "path", "source"),
            keyNames("DimensionCellRead", List.of("node", "attribute", "labels"), "source"),
            keyNames("ParameterReference", List.of("node", "parameter"), "source"),
            keyNames("VariableReference", List.of("node", "name"), "source"),
            keyNames("BoundDay", List.of("node", "type"), "source"),
            keyNames("ContextValue", List.of("node", "name", "type"), "source"),
            keyNames("Unresolved", List.of("node"), "type", "source"),
            keyNames("TemporalRead", List.of("node", "base", "date", "type"), "source"),
            keyNames("Let", List.of("node", "type", "binder", "value", "body"), "source"),
            keyNames("BinderRef", List.of("node", "name"), "source"),
            keyNames("IfThenElse", List.of("node", "type", "condition", "then", "else"), "source"),
            keyNames("InstanceSetUnion", List.of("node", "branches"), "source"));

    /** Every condition node this loader reads, with the schema's keys. The condition marker carries no type: that
     *  field is the expression marker's alone. */
    private static final Map<String, Keys> CONDITION = Map.ofEntries(
            keyNames("Comparison", List.of("node", "op", "left", "right"), "wholePeriod", "source"),
            keyNames("Predicate", List.of("node", "kind", "operand", "negated", "digits"), "dayKind", "wholePeriod",
                    "source"),
            keyNames("CharacteristicCheck", List.of("node", "characteristic", "negated"), "host", "anchor", "wholePeriod",
                    "source", "navigation"),
            keyNames("RoleMembership", List.of("node", "factType", "role", "negated"), "counterpartType", "source"),
            keyNames("TimeWindow", List.of("node", "toInclusive"), "from", "to", "source"),
            keyNames("Compound", List.of("node", "quantifier", "threshold", "parts"), "source"),
            keyNames("QuantifierFold", List.of("node", "quantifier", "elementType", "test"),
                    "threshold", "anchor", "filter", "source", "sourceRole", "sourcePath"),
            keyNames("IdentityCheck", List.of("node", "anchor", "negated"), "source"),
            keyNames("Regelversie", List.of("node", "regel", "inconsistent"), "source"),
            keyNames("Unresolved", List.of("node"), "source"));

    /** The two parts of a text composition. */
    private static final Map<String, Keys> PART = Map.ofEntries(
            keyNames("LiteralChunk", List.of("node", "text")),
            keyNames("EmbeddedExpression", List.of("node", "expr")));

    /** The discriminator of an expression or condition node, and — for the kinds this loader reads — its key set,
     *  checked as {@link #fields} checks every node since L2's fix round: no key outside the node's own, every
     *  required key present, an optional one written whole or not at all. Until then a field no node has was read
     *  past on most rule-side nodes, a condition marker carrying a type among them. A scalar where the node should
     *  stand is refused by the caller's {@link #nodeField} or {@link #elements}, at the field's pointer. */
    private String nodeKind(JsonNode n, Map<String, Keys> table, String description) {
        // every caller reads the node through nodeField or elementen, which refuse a non-object at its pointer
        String p = pathOf(n);
        JsonNode category = n.get("node");
        if (category == null) throw new DocumentError(p, "missing field 'node' (the " + description + " discriminator)");
        if (!category.isTextual()) throw new DocumentError(p + "/node", "expected a string, got " + shortQuote(category));
        Keys sl = table.get(category.asText());
        if (sl != null) fields(n, p, category.asText(), sl.required(), sl.optional());
        return category.asText();
    }

    // --- Expr --------------------------------------------------------------

    private Expr expr(JsonNode n) {
        if (n == null || n.isNull()) return null;
        String node = nodeKind(n, EXPRESSION, "expression");
        Expr e = switch (node) {
            case "Literal" -> literal(n);
            case "TextComposition" -> {
                derivedType(n);
                List<Expr.TextComposition.Part> parts = new ArrayList<>();
                elements(n, "parts", d -> parts.add(textCompositionPart(d)));
                yield new Expr.TextComposition(parts, source(n));
            }
            case "Binary" -> {
                String p = pathOf(n);
                Expr.Binary b = new Expr.Binary(
                        enumValue(Spelling.BINARY_OP, "Binary.op", textAt(n, p, "op"), p + "/op"),
                        expr(nodeField(n, "left")), expr(nodeField(n, "right")), typeField(n), source(n));
                sites.put(b, p);
                yield b;
            }
            case "Aggregation" -> aggregation(n);
            case "BuiltinCall" -> builtinCall(n);
            case "Rounding" -> {
                String p = pathOf(n);
                derivedType(n);
                yield new Expr.Rounding(expr(nodeField(n, "base")),
                        enumValue(Spelling.ROUNDING_MODE, "Rounding.mode", textAt(n, p, "mode"), p + "/mode"),
                        intOf(n, "decimals"), source(n));
            }
            // a bound is written whole or not at all: absent is no bound on that side, the record's null
            case "Clamp" -> {
                derivedType(n);
                yield new Expr.Clamp(expr(nodeField(n, "base")), expr(nodeField(n, "minimum")),
                        expr(nodeField(n, "maximum")), source(n));
            }
            // IR-SPEC ch. 4.1/4.1a: the reference nodes carry no type (the declaration is the one source) and a read
            // carries no written navigation; the record's components for both stay empty
            case "AttributeReference" -> {
                String p = pathOf(n);
                // the record carries the anchor beside the table the backend reads it from
                Expr.AttributeReference a = new Expr.AttributeReference(text(n, "attribute"), ot(text(n, "subjectType")),
                        null, null, null, source(n), false, List.of(), List.of(), null, 0, anchorValue(n, p));
                sites.put(a, p);
                anchor(n, p, a);
                if (n.has("path")) {
                    // written whole or not at all, never empty (IR-SPEC ch. 4.1a): an empty list would read as the
                    // subject's own attribute
                    List<RoleBindingRef> steps = roleBindingList(n, p, "path");
                    if (steps.isEmpty()) throw new DocumentError(p + "/path", PATH_WITHOUT_STEP);
                    navigationBindings.put(a, List.copyOf(steps));
                }
                yield a;
            }
            case "DimensionCellRead" -> {
                yield new Expr.DimensionCellRead((Expr.AttributeReference) expr(nodeField(n, "attribute")),
                        stringList(n.get("labels")),
                        source(n));
            }
            case "ParameterReference" -> {
                yield new Expr.ParameterRef(param(text(n, "parameter")), source(n));
            }
            // WF-034: the marker of a variable the frontend could not bind resolves against no binding on the wire —
            // a rule-local variable is a Let, read by a BinderRef — so the document is refused here, at the node and
            // in the sentence of the shared OCaml loader; its source record is read first, as that loader reads the
            // structure before it refuses the node
            case "VariableReference" -> {
                source(n);
                throw new DocumentError(pathOf(n), variableReferenceRefusal(text(n, "name")));
            }
            // IR-SPEC ch. 4.1: the bound day of a day-kind definition and a session value, each a node of its own; the
            // record keeps the one constant it always was, named "de dag" for the first
            case "BoundDay" -> new Expr.Constant("de dag", contextType(n, BOUNDDAY_TYPE), source(n));
            // the record keeps the session value's v0 name, which the table gives for the wire's (WF-098: a name
            // outside the two is refused, byte for byte)
            case "ContextValue" -> {
                String p = pathOf(n);
                JsonNode name = n.get("name");
                if (!name.isTextual()) throw new DocumentError(p + "/name", CONTEXT_VALUE_NAME);
                String contextValueName = Spelling.CONTEXT_VALUE_NAME.value("ContextValue.name", name.asText());
                if (contextValueName == null) throw new DocumentError(p + "/name", contextValueRefusal(name.asText()));
                yield new Expr.Constant(contextValueName, contextType(n, CONTEXT_VALUE_TYPE), source(n));
            }
            // reden null: it is the FRONTEND's own field (the WARNING it raises over a surviving marker)
            // and the wire carries no such key, so a marker read off the wire has none to recover. IR-SPEC
            // ch. 4.4: an absent type is an unknown one, and the record holds null for it.
            case "Unresolved" -> {
                String p = pathOf(n);
                if (!n.has("source")) throw new DocumentError(p, MARKER_WITHOUT_SOURCE);
                yield new Expr.Unresolved(n.has("type") ? datatype(n.get("type"), p + "/type", false) : null, source(n),
                        null);
            }
            // IR-SPEC ch. 4.10 the Let cluster — all three are ORDINARY union nodes, built straight into the tree
            // with nothing riding beside them. Note the wire's field names: a BinderRef's is "name", an
            // IfThenElse's are "condition"/"then"/"else" — the condition reuses the ordinary conditie union.
            case "Let" -> let(new Expr.Let(
                    required("Let", "binder", text(n, "binder")),
                    required("Let", "value", expr(nodeField(n, "value"))),
                    required("Let", "body", expr(nodeField(n, "body"))),
                    required("Let", "type", typeField(n)), source(n)));
            case "BinderRef" -> {
                yield new Expr.BinderRef(required("BinderRef", "name", text(n, "name")), null, source(n));
            }
            case "IfThenElse" -> new Expr.IfThenElse(
                    required("IfThenElse", "condition", condition(nodeField(n, "condition"))),
                    required("IfThenElse", "then", expr(nodeField(n, "then"))),
                    required("IfThenElse", "else", expr(nodeField(n, "else"))),
                    required("IfThenElse", "type", typeField(n)), source(n));
            // IR-SPEC ch. 4.3 / WF-110: the §5.7 concatenation of subject expressions as the source of an
            // instance count. Read VERBATIM, one selection per branch, for the reason sourcePath is: every
            // selection names its own population and its own relation, and a backend that re-derived either
            // from the type pair would give the selections one and the same range — they all share one pair
            // by WF-110 (d) — and count a restatement of one of them. The two required fields are refused
            // BY NAME here, on the required() pattern, so a document that reaches this executor without
            // passing the verifier fails naming the shape it violates.
            case "InstanceSetUnion" -> {
                List<Expr.InstanceSetUnion.Branch> branches = new ArrayList<>();
                elements(n, "branches", b -> {
                    String bp = pathOf(b);
                    fields(b, bp, "InstanceSetBranch", List.of("node", "elementType", "sourceRole", "filter"),
                            List.of("source"));
                    RegelSpraakModel.ObjectType el = required("InstanceSetBranch", "elementType",
                            ot(text(b, "elementType")));
                    // IR-SPEC ch. 4.3: every selection names its relation; nothing derives a missing one
                    RoleBindingRef rb = roleBinding(b, bp, "sourceRole");
                    if (rb == null) throw new DocumentError(bp + "/sourceRole", "expected a RoleBinding, got null");
                    branches.add(new Expr.InstanceSetUnion.Branch(el, rb, condition(nodeField(b, "filter")), source(b)));
                });
                if (branches.size() < 2)
                    throw new Refusal(DroolsCode.INSTANCE_UNION_ONE_BRANCH.reason("branchCount", branches.size()), "InstanceSetUnion.branches");
                yield new Expr.InstanceSetUnion(List.copyOf(branches), source(n));
            }
            // the members of IR-SPEC ch. 4's expression union that have no record here: a form this backend does not
            // carry (the temporal read's keys are checked all the same, see EXPRESSION)
            case "TemporalRead" -> throw new Refusal(DroolsCode.EXPR_TEMPORAL_READ.reason("node", node), "Expr:" + node);
            case "Construct" -> throw new Refusal(DroolsCode.EXPR_CONSTRUCT.reason("node", node), "Expr:" + node);
            case "Match" -> throw new Refusal(DroolsCode.EXPR_MATCH.reason("node", node), "Expr:" + node);
            case "StructLiteral" -> throw new Refusal(DroolsCode.EXPR_STRUCT_LITERAL.reason("node", node), "Expr:" + node);
            case "FieldAccess" -> throw new Refusal(DroolsCode.EXPR_FIELD_ACCESS.reason("node", node), "Expr:" + node);
            case "ListLiteral" -> throw new Refusal(DroolsCode.EXPR_LIST_LITERAL.reason("node", node), "Expr:" + node);
            case "Lambda" -> throw new Refusal(DroolsCode.EXPR_LAMBDA.reason("node", node), "Expr:" + node);
            case "Invoke" -> throw new Refusal(DroolsCode.EXPR_INVOKE.reason("node", node), "Expr:" + node);
            case "Fold" -> throw new Refusal(DroolsCode.EXPR_FOLD.reason("node", node), "Expr:" + node);
            case "MapList" -> throw new Refusal(DroolsCode.EXPR_MAP_LIST.reason("node", node), "Expr:" + node);
            case "FilterList" -> throw new Refusal(DroolsCode.EXPR_FILTER_LIST.reason("node", node), "Expr:" + node);
            case "ListLength" -> throw new Refusal(DroolsCode.EXPR_LIST_LENGTH.reason("node", node), "Expr:" + node);
            // any other name is no node of this format, refused at the discriminator in the shared loader's words: a
            // node of the v0 shape that L4 replaced or L5 renamed, a node of another union, or no node at all
            default -> throw new DocumentError(pathOf(n) + "/node", "unknown expr node \"" + node + "\"");
        };
        positions.node(e, pathOf(n), order.get(n), node);
        return e;
    }

    // --- the expression layer of L4 (B21, B22, B23) -----------------------------------------------------

    /** IR-SPEC ch. 4.3: a fold, one of the eight aggregation kinds, read into the frontend's function record with no
     *  granularity (a builtin's field, unknown on this node). */
    private Expr aggregation(JsonNode n) {
        String p = pathOf(n);
        String aggregationKind = textAt(n, p, "kind");
        Expr.FunctionKind fk = enumValue(Spelling.AGGREGATION_KIND, "Aggregation.kind", aggregationKind, p + "/kind");
        JsonNode raw = n.get("arguments");
        // WF-037, the fold-source arm: the one argument of a fold that names its population is judged by its shape. A
        // node this backend does not carry — that argument, a ListLiteral among them, or a node under it — refuses as
        // a node while it is read, where the verifier, ref and the ASP backend refuse the shape. That refusal
        // therefore waits until the range and the source have been asked below, and is raised only when neither is
        // refused; a DocumentError while reading (an unbound VariableReference, WF-034; an Unresolved marker without a
        // source, WF-113) is not held back, as ref and the ASP backend refuse those first, while loading
        List<Expr> args = new ArrayList<>();
        Refusal uncarried = null;
        try {
            elements(n, "arguments", a -> args.add(expr(a)));
        } catch (Refusal r) {
            if (!n.has("elementType") || raw == null || !raw.isArray() || raw.size() != 1) throw r;
            uncarried = r;
        }
        String elementType = optText(n, p, "elementType");
        Expr.FunctionCall f = new Expr.FunctionCall(fk, args, ot(elementType), expr(nodeField(n, "emptyFallback")),
                condition(nodeField(n, "filter")), dimensionSelection(nodeField(n, "dimensionSelection")), typeField(n),
                source(n), null, anchorValue(n, p));
        // IR-SPEC ch. 4.3: the range of a fold over ONE source is one field with two spellings, and a fold with an
        // element type and a kind other than total carries it — without it the argument is the subject's own
        // attribute and the fold folds something the sentence did not name. Both spellings at once is no document.
        if (n.has("sourceRole") && n.has("sourcePath")) throw new DocumentError(p + "/sourcePath", RANGE_TWICE);
        // WF-102 (c): a count's range is one step, whatever its source reads — asked before anything of the source
        // is, so a source this loader cannot judge (a null name, a navigated or non-reference argument) is no way
        // past it (measured: with a null name the count was counted along the path)
        if (fk == Expr.FunctionKind.AANTAL && n.has("sourcePath")) throw new DocumentError(p + "/sourcePath", SOURCE_PATH_C);
        boolean oneSource = elementType != null && (uncarried != null || args.size() == 1);
        if (oneSource && fk != Expr.FunctionKind.TOTAAL && !n.has("sourceRole") && !n.has("sourcePath"))
            throw new DocumentError(p, RANGE_MISSING);
        // IR-SPEC ch. 4.3 / WF-102: a MULTI-STEP fold-source range. Read VERBATIM — the full ordered list of
        // RoleBinding steps, nothing derived from the presentation strings and nothing re-derived from the endpoint
        // type PAIR. That is the whole point of the field: a by-pair search binds a coincidental DIRECT relation
        // between subject and element type and folds the WRONG set, which is exactly what this executor did when
        // the field was dropped (measured on jutterswet-vondstketen: 5,00 where the sentence says 3,00, with ref
        // and clingo answering 3,00). The two SHAPE arms below are refused BY NAME here rather than downstream, for
        // ref's reason. The MODEL-relative arms (a step that does not join, a landing beside the element type, a
        // hop the beans carry no field for) belong to the walk and are per-form refusals in the transpilers, which
        // know the fold SUBJECT this node does not.
        // The position (WF-102 (c)) is asked before the number of steps, as ref asks it.
        if (n.has("sourcePath")) {
            List<RoleBindingRef> path = roleBindingList(n, p, "sourcePath");
            if (!isMemberFoldSource(n)) throw new DocumentError(p + "/sourcePath", SOURCE_PATH_C);
            if (path.size() < 2)
                throw new Refusal(DroolsCode.AGGREGATE_PATH_ONE_STEP.reason("pathLength", path.size()), "Aggregation.sourcePath");
            sourcePaths.put(f, List.copyOf(path));
        }
        // the one-step range — see Loaded#roleBindings. It stays OUT of the record (that one is vendored).
        RoleBindingRef br = roleBinding(n, p, "sourceRole");
        if (br != null) roleBindings.put(f, br);
        // WF-037: a fold that names its population and hands no source, in every shape, a ListLiteral included. Asked
        // after the range, so a path beside it is refused first under WF-102 (c) and a missing range first as such, as
        // ref and the ASP backend refuse them first
        String sourceless = sourceRefusal(aggregationKind, elementType, raw);
        if (sourceless != null) throw new DocumentError(p, sourceless);
        String union = unionSourceRefusal(fk, elementType, n);
        if (union != null) throw new DocumentError(p, union);
        if (uncarried != null) throw uncarried;
        if (oneSource && (n.has("sourceRole") || n.has("sourcePath"))) checkFoldSource(n, p, fk, f);
        sites.put(f, p);
        anchor(n, p, f);
        return f;
    }

    /** IR-SPEC ch. 4.3: every other function kind, with plain arguments, read into the frontend's function record
     *  with none of the fold's fields (unknown on this node). */
    private Expr builtinCall(JsonNode n) {
        String p = pathOf(n);
        Expr.FunctionKind fk = enumValue(Spelling.BUILTIN_KIND, "BuiltinCall.kind", textAt(n, p, "kind"), p + "/kind");
        List<Expr> args = new ArrayList<>();
        elements(n, "arguments", a -> args.add(expr(a)));
        String g = optText(n, p, "granularity");
        Granularity granularity = null;
        if (g != null) {
            granularity = enumValue(Spelling.GRANULARITY, "BuiltinCall.granularity", g, p + "/granularity");
            // WF-037: the facet belongs to the two bucketing builtins. On any other kind neither branch reads it, and
            // both played the call as if it were not written (measured, yearOf with month), where the verifier
            // refuses it and ref faults.
            if (fk != Expr.FunctionKind.TIJDSEVENREDIG_DEEL && fk != Expr.FunctionKind.AANTAL_DAGEN_IN)
                throw new DocumentError(p, "granularity facet on a non-bucketing kind");
        }
        Expr.FunctionCall f = new Expr.FunctionCall(fk, args, null, null, condition(nodeField(n, "filter")), null, typeField(n),
                source(n), granularity);
        sites.put(f, p);
        return f;
    }

    /** WF-037 (IR-SPEC ch. 4.3), the verifier's sentences for a fold that names its population and hands no source:
     *  no argument, under every kind; one {@code ListLiteral} under a count or a filled count; and under every kind one
     *  node of any other kind but an {@code AttributeReference}, the fold source — a {@code ListLiteral} under a value
     *  fold excepted, which WF-091 refuses on its type and this loader as a node it does not carry, and a union under a
     *  count, which is WF-110 (c)'s. The kind and the node are named as the wire spells them, the node with its
     *  article on the verifier's rule (an before a vowel). Null when the fold carries no {@code elementType} or has its
     *  source. Without it both branches played a count whose one argument was a literal, a parameter or a marker,
     *  counting the instances its range reaches, the DRL-text branch played the filled count the same way and a count
     *  without an argument beside its range, and crashed on a sum, a total, a minimum or a maximum over a literal
     *  (measured before the card aantal-bronarm-en-drools-lader); the exec-model branch refused the rest with a scope
     *  note. */
    static String sourceRefusal(String aggregationKind, String elementType, JsonNode arguments) {
        if (elementType == null || arguments == null || !arguments.isArray()) return null;
        String head = "a " + aggregationKind + " aggregation carries elementType \"" + elementType + "\"";
        if (arguments.isEmpty())
            return head + " but no source operand: an instance fold ranges over a written source";
        if (arguments.size() != 1) return null;
        boolean count = aggregationKind.equals("count") || aggregationKind.equals("filledCount");
        String operandKind = text(arguments.get(0), "node");
        if (operandKind == null || operandKind.isEmpty() || operandKind.equals("AttributeReference")) return null;
        if (operandKind.equals("ListLiteral"))
            return count ? head + " and a ListLiteral operand: a listed value is no source of instances" : null;
        if (operandKind.equals("InstanceSetUnion") && aggregationKind.equals("count")) return null;
        return head + " and " + ("AEIOU".indexOf(operandKind.charAt(0)) >= 0 ? "an " : "a ") + operandKind
                + " operand: the source of an instance fold is an AttributeReference";
    }

    /** WF-110 (c) (IR-SPEC ch. 4.3), the verifier's sentence: a count over an {@code InstanceSetUnion} names no
     *  source of its own, so an {@code elementType}, a range or a {@code filter} on that node is a second claim beside
     *  the union's selections; the fields are named in the verifier's order. Null for any other node. A path there is
     *  refused before this, as a count's path (WF-102 (c)). Without it both branches played such a count, counting the
     *  union and never reading the node's own source (measured before the card aantal-bronarm-en-drools-lader). */
    static String unionSourceRefusal(Expr.FunctionKind fk, String elementType, JsonNode n) {
        JsonNode arguments = n.get("arguments");
        if (fk != Expr.FunctionKind.AANTAL || arguments == null || !arguments.isArray() || arguments.size() != 1
                || !"InstanceSetUnion".equals(text(arguments.get(0), "node"))) return null;
        List<String> fields = new ArrayList<>();
        if (elementType != null) fields.add("elementType");
        if (n.has("sourceRole")) fields.add("sourceRole");
        if (n.has("sourcePath")) fields.add("sourcePath");
        if (n.has("filter")) fields.add("filter");
        if (fields.isEmpty()) return null;
        return "a count over an instance-set union names no source of its own, and this node carries "
                + String.join(" + ", fields) + ": the union's selections carry the population, the relation and the"
                + " subselection, one set per selection, so a source here is a second claim on one node (ch. 4.3)";
    }

    /** The shared loader's sentence for two range spellings on one fold. */
    static final String RANGE_TWICE = "sourcePath beside sourceRole: a fold carries one range, never two";
    /** The shared loader's sentence for a fold over one source without its range. */
    static final String RANGE_MISSING = "a single-argument fold with an elementType carries its range (sourceRole"
            + " or sourcePath) under every kind but total: the range is written, never derived";

    /**
     * IR-SPEC ch. 4.1: a literal's typed value, read into the frontend's literal record as the lexeme it always
     * held — the string of the variant, and waar or onwaar for a boolean. The value is exactly one variant of the
     * case format's value union, without a payload; its string is loaded as written, with the exceptions below,
     * which no executor may read past. A variant that is not the one the literal's type names is refused (WF-114's
     * sentence without its number), so no branch reads {@code {"text": "5"}} under a Number as a number; a literal
     * without a type has nothing to cross it against (WF-034's typeless value node). And a number, a date or a
     * date-time outside its grammar is refused in the verifier's sentence (WF-081, WF-084, WF-095), keyed on the
     * variant as the verifier keys it. For the number that grammar is the one ref and the ASP backend read with: the
     * frontend's shared number reader, which also reads source text, searches the lexeme for its first number, so it
     * took a comma for a point and read {@code .5} and {@code 5 €} as 5, {@code -.5} as 5 and {@code 3/04} as 0.75
     * where those two fault (measured on both branches before this check). A date or date-time outside ISO loaded
     * until the fix round of the L4 landing and was refused only while lowering, under a scope note ("waarde buiten
     * scope", "TODO datumliteral"), where the verifier named the lexeme (measured on the WF-084 and WF-095 witnesses).
     */
    private Expr literal(JsonNode n) {
        String p = pathOf(n);
        JsonNode value = n.get("value");
        String variant = null, lexeme = null;
        if (value.isObject() && value.size() == 1) {
            variant = value.fieldNames().next();
            JsonNode v = value.get(variant);
            if (variant.equals("boolean")) lexeme = v.isBoolean() ? (v.booleanValue() ? "waar" : "onwaar") : null;
            else if (LITERAL_VARIANTS.contains(variant) && v.isTextual()) lexeme = v.asText();
        }
        if (lexeme == null) throw new DocumentError(p + "/value", LITERAL_VALUE);
        Datatype type = typeField(n);
        if (type != null) {
            String refusal = literalVariantRefusal(variant, type);
            if (refusal != null) throw new DocumentError(p + "/value", refusal);
        }
        if (variant.equals("number") && !GETAL.matcher(lexeme).matches())
            throw new DocumentError(p + "/value", "numeric literal lexeme '" + lexeme + "' is not a number of the case"
                    + " format (an optional '-', digits, an optional '.' and digits, or a rational a/b whose denominator"
                    + " has no leading zero — ch. 4.1)");
        if (variant.equals("date") && !isoDate(lexeme))
            throw new DocumentError(p + "/value", "date literal lexeme '" + lexeme + "' is not the ISO form yyyy-mm-dd"
                    + " (fixed field widths, valid ranges — ch. 4.1)");
        if (variant.equals("dateTime") && !isoDateTime(lexeme))
            throw new DocumentError(p + "/value", "millisecond-precision date literal lexeme '" + lexeme + "' is not the"
                    + " canonical ISO form yyyy-mm-ddThh:mm:ss[.fff] (fixed field widths, 'T' never a space, the seconds"
                    + " field written, no zone or offset, a fraction of exactly three digits; the day-first source"
                    + " snippet belongs in `source` — ch. 4.1)");
        return new Expr.Literal(lexeme, type, source(n));
    }

    /** WF-084's grammar, the verifier's: four-two-two digit fields, a month of the year and a day of that month,
     *  leap years included. */
    private static boolean isoDate(String s) {
        java.util.regex.Matcher m = DATE.matcher(s);
        return m.matches() && calendarDay(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)),
                Integer.parseInt(m.group(3)));
    }

    /** WF-095's grammar, the verifier's: the date of WF-084, a 'T', hours, minutes and seconds in range, and an
     *  optional fraction of exactly three digits, with nothing after it. */
    private static boolean isoDateTime(String s) {
        java.util.regex.Matcher m = DATE_TIME.matcher(s);
        return m.matches() && calendarDay(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)),
                Integer.parseInt(m.group(3))) && Integer.parseInt(m.group(4)) <= 23 && Integer.parseInt(m.group(5)) <= 59
                && Integer.parseInt(m.group(6)) <= 59;
    }

    private static boolean calendarDay(int year, int month, int day) {
        return month >= 1 && month <= 12 && day >= 1 && day <= java.time.YearMonth.of(year, month).lengthOfMonth();
    }

    private static final java.util.regex.Pattern DATE =
            java.util.regex.Pattern.compile("([0-9]{4})-([0-9]{2})-([0-9]{2})");
    private static final java.util.regex.Pattern DATE_TIME = java.util.regex.Pattern.compile(
            "([0-9]{4})-([0-9]{2})-([0-9]{2})T([0-9]{2}):([0-9]{2}):([0-9]{2})(\\.[0-9]{3})?");

    /** The number grammar of the case format (uchar-case-v0), which the literal's number variant carries. */
    private static final java.util.regex.Pattern GETAL =
            java.util.regex.Pattern.compile("-?[0-9]+(\\.[0-9]+)?|-?[0-9]+/[1-9][0-9]*");

    /** The seven variants of the case format's value union, in its order. */
    private static final List<String> LITERAL_VARIANTS = List.of("number", "boolean", "text", "date", "dateTime",
            "enumeration", "constructor");

    /** The shared loader's sentence for a literal value in any other shape. */
    static final String LITERAL_VALUE = "a literal value is exactly one variant: {\"number\": string}, {\"boolean\":"
            + " true or false}, {\"text\": string}, {\"date\": string}, {\"dateTime\": string}, {\"enumeration\":"
            + " string} or {\"constructor\": string}, without a payload";

    /** WF-114 (IR-SPEC ch. 4.1), the shared loader's sentence: null when {@code variant} is the one {@code type}
     *  names. Every datatype this backend carries names a variant; a sum, list or struct type is refused as a form
     *  before a literal reaches here. */
    static String literalVariantRefusal(String variant, Datatype type) {
        String named, phrase;
        switch (type) {
            case Datatype.Numeric x -> { named = "number"; phrase = "a Number"; }
            case Datatype.Percentage x -> { named = "number"; phrase = "a Number"; }
            case Datatype.BooleanType x -> { named = "boolean"; phrase = "a Boolean"; }
            case Datatype.Text x -> { named = "text"; phrase = "a Text"; }
            case Datatype.DateInDays x -> { named = "date"; phrase = "a day-precision Date"; }
            case Datatype.DateTime x -> { named = "dateTime"; phrase = "a millisecond-precision Date"; }
            case Datatype.Enumeration x -> { named = "enumeration"; phrase = "an Enumeration"; }
            case Datatype.Unknown x -> throw new IllegalStateException("IrLoader: an Onbekend datatype is refused"
                    + " while it is read, before a literal is crossed against it");
        }
        if (named.equals(variant)) return null;
        return "literal value is " + (variant.equals("enumeration") ? "an" : "a") + " " + variant + " value but its"
                + " type is " + phrase + ": the variant is the one the type names";
    }

    /** WF-098, the shared loader's sentence for a context value outside the closed set of IR-SPEC ch. 4.1 — the bound
     *  day's name included, since that day is a node of its own, and the v0 names of the two. */
    static String contextValueRefusal(String name) {
        return "context value '" + name + "' is not one of the two admissible names evaluationDate, evaluationYear:"
                + " the context-value name is a closed set, spelled byte for byte";
    }

    /** WF-034, the shared loader's sentence for a VariableReference: the frontend's marker of a variable it could not
     *  bind, which resolves against no binding on the wire. */
    static String variableReferenceRefusal(String name) {
        return (name == null ? "a variable reference without a name" : "variable reference '" + name + "'")
                + " resolves against no binding: a rule-local variable is a Let, read by a BinderRef";
    }

    /** The shared loader's sentence for a path without a hop: a read without navigation leaves the field out. */
    static final String PATH_WITHOUT_STEP = "an empty path is not a navigation";

    /** The shared loader's sentences for the name and the datatype the two context nodes require. */
    static final String CONTEXT_VALUE_NAME = "a ContextValue carries its name as a string";
    static final String CONTEXT_VALUE_TYPE = "a ContextValue carries its datatype";
    static final String BOUNDDAY_TYPE = "a BoundDay carries its datatype";
    /** WF-108 (a), the verifier's sentence for the one quantifier a quantifier fold does not admit: the executors
     *  refuse it at load, as the shared loader does when it reads for an executor. */
    static final String QUANTIFIER_FOLD_THE = "quantifier fold with quantifier the: the bare definite is a description of"
            + " ONE member, not a quantification over the set — a fold over it has no reading";
    /** WF-113 (b)'s arm for a marker without a source, the verifier's sentence: the executors refuse such a marker at
     *  load, as the shared loader does when it reads for an executor. */
    static final String MARKER_WITHOUT_SOURCE = "Unresolved marker without a source: no diagnostic message can contain it,"
            + " so it cannot be accounted for";

    /** The required datatype of a context node: anything but a datatype object is refused in the node's sentence. */
    private Datatype contextType(JsonNode n, String sentence) {
        JsonNode t = n.get("type");
        if (!t.isObject()) throw new DocumentError(pathOf(n) + "/type", sentence);
        return typeField(n);
    }

    /** Assert that a ch. 4.10 Let-cluster node carries a field the shape REQUIRES (WF-076/WF-064/WF-069), naming
     *  both the form and the field. Without it a malformed document fails downstream and namelessly: a null
     *  {@code value}/{@code body}/branch throws an unattributed NPE out of the transpiler's {@code List.of}
     *  child accessor, and a missing {@code type} silently produces a node typed null, which every walk that reads
     *  a datatype then mis-reads. Fail fast, with the form and the field in the message. */
    private static <T> T required(String formName, String fieldName, T value) {
        if (value == null) throw new Refusal(DroolsCode.REQUIRED_FIELD_MISSING.reason("form", formName, "field", fieldName), formName);
        return value;
    }

    /** WF-102 (c), the one sentence for both halves of the position: a path ranges a value fold's set source and
     *  nothing else. */
    private static final String SOURCE_PATH_C = "a sourcePath is admissible only as the range of a single-argument VALUE"
            + " fold's set source — a count's range is one step, spelled sourceRole";

    /** WF-102 (c): is this {@code Aggregation} node a single-argument instance fold — the one position a range
     *  stands at? Asked of the RAW node: a declared {@code elementType} plus exactly one argument that is a read
     *  carrying no navigation of its own (IR-SPEC ch. 4.3: with a range, that argument IS the source). */
    private static boolean isMemberFoldSource(JsonNode n) {
        if (!n.hasNonNull("elementType")) return false;
        JsonNode args = n.get("arguments");
        if (args == null || !args.isArray() || args.size() != 1) return false;
        JsonNode a = args.get(0);
        return "AttributeReference".equals(text(a, "node")) && !a.has("path");
    }

    /** The set source of a single-argument fold that carries a range (IR-SPEC ch. 4.3): what its name reads on the
     *  element type decides which fold kinds may consume it, and a document that pairs the two otherwise is refused
     *  by name, as the verifier refuses it and as {@code ref} and the ASP backend fault on it. Under {@code count}
     *  the source names the counted role, so a name that is a member of the element type reads a value per
     *  contributor, which a count does not fold (WF-093 (b)); a count's range is one step (WF-102 (c)), which the
     *  fold's own arm asks before this, whatever the source reads. Under a value fold the name is read per
     *  contributor, so it must be an attribute of the element type: a
     *  characteristic carries no value there (WF-080). A name that is neither is refused once the subject is known
     *  ({@link #checkValueFoldSource}). Without these the exec branch played a count over an attribute as a count of
     *  members and failed with a runtime error at first fire on a value fold over a role name, and the DRL-text
     *  branch compiled neither (measured on the L2 union). A navigated source is WF-053's and is left to the
     *  transpilers' refusal. */
    private void checkFoldSource(JsonNode n, String p, Expr.FunctionKind fk, Expr.FunctionCall f) {
        if (fk == null || f.elementObjectType() == null
                || !(f.arguments().get(0) instanceof Expr.AttributeReference source) || navigationBindings.containsKey(source)
                || source.attribute() == null) return;
        ObjectType el = f.elementObjectType();
        String a = source.attribute(), site = p + "/arguments/0";
        boolean attribute = el.attribute(a) != null, characteristic = !attribute && el.characteristicName(a) != null;
        if (fk == Expr.FunctionKind.AANTAL) {
            if (attribute || characteristic)
                throw new DocumentError(site, "the source of this count aggregation reads '" + a + "', a member of the"
                        + " element type '" + el.name + "': a count folds INSTANCES, so its source is a count source"
                        + " naming the relation's role, never a value read per contributor");
            return;
        }
        if (VALUE_FOLDS.contains(fk) && characteristic)
            throw new DocumentError(site, "'" + a + "' is a characteristic of '" + el.name + "', not an attribute — a"
                    + " characteristic endpoint is ill-formed in an aggregation source position");
    }

    /** The value fold's set source whose name is no member of the element type, judged with the fold SUBJECT, as
     *  the verifier's source reading judges it: a name that is a role reachable from the subject — the far role of
     *  a two-role fact type the subject fills the near role of, or a reciprocal role it fills — is a count source,
     *  which only a count folds (WF-093 (a)); any other name declares nothing on the element type (WF-034), even
     *  where some other type's relation carries a role of that name. At a subject-less position (a day-kind
     *  definition) any declared role name counts, as the verifier has no subject to ask either. */
    private void checkValueFoldSource(Expr.FunctionCall f, ObjectType s) {
        if (!VALUE_FOLDS.contains(f.kind()) || f.elementObjectType() == null
                || !(f.arguments().get(0) instanceof Expr.AttributeReference source) || navigationBindings.containsKey(source)
                || source.attribute() == null) return;
        ObjectType el = f.elementObjectType();
        String a = source.attribute(), site = sites.get(f) + "/arguments/0";
        if (el.attribute(a) != null || el.characteristicName(a) != null) return;   // read, or refused as WF-080 while reading
        if (s == null ? isRoleName(a) : isRoleNameFrom(s, a))
            throw new DocumentError(site, "value aggregation over \"" + a + "\", which is no attribute of \""
                    + el.name + "\": a value fold reads an attribute per contributor, and a role name is a count source"
                    + " only a count folds");
        throw new DocumentError(site, "'" + a + "' is not a declared attribute of '" + el.name + "': a value fold's"
                + " source reads an attribute of each contributor");
    }

    /** WF-083 (c), the element type of the subject's own read: a {@code total} over one source without a range is
     *  the subject's own read, so the type it folds over is the subject type — equal in the WF-001 form. Neither
     *  branch reads the field there, so a foreign type in it changed no answer: {@code jutterswet-tempototaal} with
     *  its own total naming Vondst played in both branches (measured before this check). It is refused here in the
     *  verifier's words. */
    private void checkOwnTotal(Expr.FunctionCall f, ObjectType s) {
        if (f.kind() != Expr.FunctionKind.TOTAAL || f.elementObjectType() == null || s == null
                || f.arguments().size() != 1 || roleBindings.containsKey(f) || sourcePaths.containsKey(f)) return;
        ObjectType el = f.elementObjectType();
        if (Namen.norm(el.name).equals(Namen.norm(s.name))) return;
        throw new DocumentError(sites.get(f) + "/elementType", "the elementType '" + el.name
                + "' of a total without a range is not the fold subject '" + s.name + "': without a range the fold is"
                + " the subject's own read, so its elementType must name the subject type");
    }

    /** The fold kinds that read a VALUE per contributor (IR-SPEC ch. 4.3) — every aggregation kind but
     *  {@code count}, which folds the contributors themselves. */
    private static final java.util.Set<Expr.FunctionKind> VALUE_FOLDS = java.util.EnumSet.of(
            Expr.FunctionKind.SOM, Expr.FunctionKind.TOTAAL, Expr.FunctionKind.MINIMUM, Expr.FunctionKind.MAXIMUM,
            Expr.FunctionKind.EERSTE, Expr.FunctionKind.LAATSTE, Expr.FunctionKind.AANTAL_GEVULDE);

    /** Does {@code name} name a declared role — by its declared name or its declared plural, in the WF-001 form? */
    private boolean isRoleName(String name) {
        for (FactType ft : m.factTypes.values())
            for (RegelSpraakModel.Role r : ft.roles)
                if (hasName(r, name)) return true;
        return false;
    }

    /** Does {@code name} name a role reachable from {@code st}: the far role of a two-role fact type whose near role
     *  {@code st} fills (⊑ through the supertype chain, ch. 2.6), or the one role of a reciprocal fact type it
     *  fills? */
    private boolean isRoleNameFrom(ObjectType st, String name) {
        for (FactType ft : m.factTypes.values()) {
            if (ft.roles.size() == 2) {
                RegelSpraakModel.Role a = ft.roles.get(0), b = ft.roles.get(1);
                if ((fills(st, a) && hasName(b, name)) || (fills(st, b) && hasName(a, name))) return true;
            } else if (ft.roles.size() == 1 && ft.reciprocal) {
                RegelSpraakModel.Role r = ft.roles.get(0);
                if (fills(st, r) && hasName(r, name)) return true;
            }
        }
        return false;
    }

    private boolean fills(ObjectType st, RegelSpraakModel.Role r) {
        ObjectType filler = ot(r.objectTypeName());
        return filler != null && st.isA(filler);
    }

    private static boolean hasName(RegelSpraakModel.Role r, String name) {
        String n = Namen.norm(name);
        return (r.name() != null && Namen.norm(r.name()).equals(n))
                || (r.plural() != null && Namen.norm(r.plural()).equals(n));
    }

    /** The two range spellings of a quantifier fold are one field (IR-SPEC ch. 4.5, WF-102 (a)): exactly one of
     *  them, written, never derived — refused otherwise in the shared loader's sentences. */
    private static void rangeFields(JsonNode n, String p) {
        if (n.has("sourceRole") && n.has("sourcePath"))
            throw new DocumentError(p + "/sourcePath",
                    "sourcePath beside sourceRole: a quantifier fold carries one range, never two");
        if (!n.has("sourceRole") && !n.has("sourcePath"))
            throw new DocumentError(p, "a quantifier fold carries its range (sourceRole or sourcePath): the range is"
                    + " written, never derived");
    }

    /** A derived position whose record computes its own type — a rounding and a clamp from their base, a text
     *  composition as text: the wire's {@code type} there is still a datatype or null (IR-SPEC ch. 2.10), so it is read
     *  for its defects and not kept. An {@code Onbekend} or a scalar in it is no document, as the verifier holds;
     *  until L2's advisory round the field was not read at all. */
    private void derivedType(JsonNode n) {
        typeField(n);
    }

    /** The datatype at a node's {@code type} field — a type annotation — with that field's pointer for every defect
     *  in it. */
    private Datatype typeField(JsonNode n) {
        return datatype(n.get("type"), pathOf(n) + "/type", false);
    }

    /** Index one {@link Expr.Let} (see {@link Loaded#lets()}) and return it — the node itself goes into the tree. */
    private Expr let(Expr.Let l) {
        lets.add(l);
        return l;
    }

    private Expr.TextComposition.Part textCompositionPart(JsonNode d) {
        String node = nodeKind(d, PART, "text part");
        return switch (node) {
            case "LiteralChunk" -> new Expr.TextComposition.LiteralChunk(text(d, "text"));
            case "EmbeddedExpression" -> new Expr.TextComposition.EmbeddedExpression(expr(nodeField(d, "expr")));
            // any other name is no node of this format — the two parts of the v0 shape that L5 renamed among them —
            // refused at the discriminator in the shared loader's words
            default -> throw new DocumentError(pathOf(d) + "/node", "unknown TextCompositionPart node \"" + node + "\"");
        };
    }

    private Expr.DimensionSelection dimensionSelection(JsonNode n) {
        if (n == null || n.isNull()) return null;
        fields(n, pathOf(n), "DimensionSelection", List.of("node", "dimension", "labels"), List.of("source"));
        return new Expr.DimensionSelection(text(n, "dimension"), stringList(n.get("labels")), source(n));
    }

    // --- Conditie ----------------------------------------------------------

    private Condition condition(JsonNode n) {
        if (n == null || n.isNull()) return null;
        String node = nodeKind(n, CONDITION, "condition");
        Condition c = switch (node) {
            // a whole-period qualifier is written whole or not at all: absent is none, the record's null
            case "Comparison" -> {
                String p = pathOf(n);
                yield new Condition.Comparison(
                        enumValue(Spelling.COMPARISON_OP, "Comparison.op", textAt(n, p, "op"), p + "/op"),
                        expr(nodeField(n, "left")), expr(nodeField(n, "right")),
                        wholePeriod(nodeField(n, "wholePeriod")), source(n));
            }
            case "Predicate" -> predicate(n);
            // IR-SPEC ch. 4.5: the binding IS the hop — a check without one tests the subject's own kenmerk; an
            // absent host is the subject (RUNTIME-SEMANTICS ch. 5.8b), the record's null
            case "CharacteristicCheck" -> {
                String p = pathOf(n);
                Condition.CharacteristicCheck k = new Condition.CharacteristicCheck(text(n, "characteristic"), null, bool(n, "negated"),
                        ot(text(n, "host")), wholePeriod(nodeField(n, "wholePeriod")), source(n), anchorValue(n, p));
                RoleBindingRef nb = roleBinding(n, p, "navigation");
                if (nb != null) navigationBindings.put(k, List.of(nb));
                anchor(n, p, k);
                yield k;
            }
            // IR-SPEC ch. 4.5: one node for the role check — the fact type, the role the SUBJECT plays in it, and the
            // narrowing of the other end where there is one. The record is the carrier (see Loaded#roleBindings).
            case "RoleMembership" -> {
                String p = pathOf(n);
                // WF-034, the role check's pair crossed against the declarations, in the words ref and the ASP
                // backend fault with: a check naming no declared link holds for no instance, and until L2's fix
                // round this backend refused it with a storage reason instead (the fact type, the role, the
                // narrowing here; whether the subject fills the role in checkSubjects, which has the subject)
                String factType = textAt(n, p, "factType"), role = textAt(n, p, "role");
                FactType ft = factTypeByName(factType);
                if (ft == null)
                    throw new DocumentError(p + "/factType", "role check names fact type '" + factType + "', which is"
                            + " not declared: the check resolves against no declaration");
                RegelSpraakModel.Role bound = roleByName(ft, role);
                if (bound == null)
                    throw new DocumentError(p + "/role", "'" + role + "' is not a declared role of fact type '" + factType
                            + "' (a role check names the role by its declared name)");
                String counterpart = optText(n, p, "counterpartType");
                ObjectType other = counterpart == null ? null : ot(counterpart);
                if (counterpart != null) {
                    if (other == null)
                        throw new DocumentError(p + "/counterpartType", "role check: the counterpart type \""
                                + counterpart + "\" is no declared object type");
                    RegelSpraakModel.Role otherSide = ft.reciprocal ? bound : otherRole(ft, bound);
                    ObjectType filler = otherSide == null ? null : ot(otherSide.objectTypeName());
                    if (filler != null && !other.isA(filler))
                        throw new DocumentError(p + "/counterpartType", "role check: the counterpart type \""
                                + counterpart + "\" is no subtype of \"" + filler.name + "\", the filler the role \""
                                + otherSide.name() + "\" declares");
                }
                Condition.RoleMembership r = new Condition.RoleMembership(role, other, source(n),
                        truth(n, p, "negated"));
                roleBindings.put(r, new RoleBindingRef(factType, r.role()));
                sites.put(r, p);
                yield r;
            }
            // a bound is written whole or not at all: absent is open on that side, the record's null
            case "TimeWindow" -> new Condition.TimeWindow(expr(nodeField(n, "from")), expr(nodeField(n, "to")),
                    bool(n, "toInclusive"), source(n));
            case "Compound" -> {
                String p = pathOf(n);
                List<Condition> parts = new ArrayList<>();
                elements(n, "parts", o -> parts.add(condition(o)));
                yield new Condition.Compound(
                        enumValue(Spelling.QUANTIFIER, "Compound.quantifier", textAt(n, p, "quantifier"),
                                p + "/quantifier"),
                        intOf(n, "threshold"), parts, source(n));
            }
            // IR-SPEC ch. 4.5 / WF-108 the QUANTIFIER FOLD. Unlike Compound — whose quantifier ranges over the
            // BULLETS of one subject — this one ranges over INSTANCES, and its range fields are the instance fold's
            // (ch. 4.3) spelled unchanged. Both range fields are decoded INTO the record: QuantifierFold is not part
            // of the vendored frontend slice the function/object-creation side-channel exists for, so its range
            // needs no Loaded#roleBindings or sourcePaths entry.
            case "QuantifierFold" -> {
                // IR-SPEC ch. 4.5: the range is required, one field with two spellings. A multi-step range (WF-108
                // (c), RUNTIME-SEMANTICS ch. 3.4b) goes to the transpiler as it is written, which plans it as it plans
                // an Aggregation's sourcePath (transpiler/core/SourcePath); until the card frontend-kwantor-over-een-pad
                // (2026-10-20) this loader refused it (DROOLS-026, retired).
                String p = pathOf(n);
                rangeFields(n, p);
                List<RoleBindingRef> sourcePath = n.has("sourcePath") ? roleBindingList(n, p, "sourcePath") : List.of();
                // WF-108 (a): all, none and the three COUNTING quantifiers are the admitted forms. The value the
                // shares the one quantifier family with Compound, where it is admitted, and is refused here BY NAME,
                // in the verifier's sentence, as the shared loader refuses it for an executor — the bare definite
                // describes one member, and reading it as a forall is the misreading this node exists to prevent. Any
                // other value outside the family is an unknown value.
                String wq = textAt(n, p, "quantifier");
                if (Spelling.QUANTIFIER.refusesByName("QuantifierFold.quantifier", wq))
                    throw new DocumentError(p + "/quantifier", QUANTIFIER_FOLD_THE);
                Condition.Quantifier kw = enumValue(Spelling.QUANTIFIER, "QuantifierFold.quantifier", wq,
                        p + "/quantifier");
                // WF-108 (e): the threshold is present EXACTLY under a counting quantifier. Read as an absence rather
                // than as a 0 — a counting fold whose threshold is missing has no verdict, and guessing one here
                // would answer a question the document does not ask.
                boolean counts = kw == Condition.Quantifier.TEN_MINSTE || kw == Condition.Quantifier.TEN_HOOGSTE
                        || kw == Condition.Quantifier.PRECIES;
                if (counts != n.has("threshold"))
                    throw new Refusal(DroolsCode.QUANTIFIER_THRESHOLD_MISMATCH.reason("quantifier", wq, "thresholdClause", DroolsCode.QUANTIFIER_THRESHOLD_MISMATCH.part(counts ? "and-no-threshold" : "and-one")), "Kwantorvouw.drempel");
                // a subselection is written whole or not at all: absent is none, the record's null
                Condition.QuantifierFold kv = new Condition.QuantifierFold(kw, counts ? intOf(n, "threshold") : 0,
                        ot(text(n, "elementType")),
                        condition(nodeField(n, "filter")), condition(nodeField(n, "test")),
                        sourcePath.isEmpty() ? roleBinding(n, p, "sourceRole") : null, sourcePath, source(n));
                anchor(n, p, kv);
                yield kv;
            }
            // IR-SPEC ch. 4.5, WF-122: the anchor is required and has one value, so it is read for its value only
            case "IdentityCheck" -> {
                String p = pathOf(n);
                anchorValue(n, p);
                yield new Condition.IdentityCheck(truth(n, p, "negated"), source(n));
            }
            case "Regelversie" -> new Condition.RuleStatusCheck(text(n, "regel"), bool(n, "inconsistent"), source(n));
            case "Unresolved" -> {
                if (!n.has("source")) throw new DocumentError(pathOf(n), MARKER_WITHOUT_SOURCE);
                yield new Condition.Unresolved(source(n), null);   // frontend-internal field, absent from the wire
            }
            // this backend carries every member of IR-SPEC ch. 4.5's condition union, so any other name is no node of
            // this format, refused at the discriminator in the shared loader's words: a node of the v0 shape that L5
            // renamed, one of the two role checks of the L1 shape that RoleMembership replaced, a node of another
            // union, or no node at all
            default -> throw new DocumentError(pathOf(n) + "/node", "unknown condition node \"" + node + "\"");
        };
        positions.node(c, pathOf(n), order.get(n), node);
        return c;
    }

    /** A negated leeg/gevuld test is lowered HERE to its sister soort, unnegated: pointwise "niet leeg" IS "gevuld"
     *  (ref: {@code not is_empty} against {@code v <> Empty}). Both transpiler branches read the leeg/gevuld soort at
     *  a dozen sites and none of them reads {@code negated} — the RegelSpraak frontend never writes it on these two
     *  soorten — so answering it once at the wire boundary covers every site, own and navigated, in both branches.
     *  {@code negated} is the INNER negation, per day; the qualifier's own "niet gedurende …" is
     *  {@code WholePeriod.negated} and is untouched here.
     *
     *  <p>The swap holds under the ch. 3.9 whole-period qualifier as well (decided 2026-09-26): the leeg and the
     *  gevuld test both take the threshold days(B), a gap day being a leeg day, so a negated test asks for the
     *  complementary days of the same touched buckets, which is exactly its sister test. Until that date both
     *  forms were refused here, the negated one because the leeg test still had a threshold of its own and the
     *  unnegated one because ref did not execute the fold. */
    private Condition.Predicate predicate(JsonNode n) {
        String p = pathOf(n);
        Condition.PredicateKind predicateKind = enumValue(Spelling.PREDICATE_KIND, "Predicate.kind", textAt(n, p, "kind"),
                p + "/kind");
        boolean negated = bool(n, "negated");
        Condition.WholePeriod qualifier = wholePeriod(nodeField(n, "wholePeriod"));
        boolean emptyOrFilled = predicateKind == Condition.PredicateKind.LEEG || predicateKind == Condition.PredicateKind.GEVULD;
        // WF-043 admits a time-dependent PARAMETER as the operand of a whole-period test, and no executor folds one:
        // ref and the ASP backend refuse it in this sentence, the wire's own spelling of the name included
        JsonNode operand = nodeField(n, "operand");
        if (emptyOrFilled && qualifier != null && operand != null && "ParameterReference".equals(operand.path("node").asText()))
            throw new Refusal(DroolsCode.PERIOD_TEST_ON_PARAMETER.reason("parameter", operand.path("parameter").asText()), "Predicate.operand");
        if (emptyOrFilled && negated) {
            predicateKind = predicateKind == Condition.PredicateKind.LEEG ? Condition.PredicateKind.GEVULD : Condition.PredicateKind.LEEG;
            negated = false;
        }
        // the day kind is written on a predicate of kind dayKind alone: absent elsewhere, the record's null
        return new Condition.Predicate(predicateKind, expr(operand), text(n, "dayKind"), negated,
                intOf(n, "digits"),
                qualifier, source(n));
    }

    /** IR-SPEC ch. 4.5: the whole-period qualifier, its granularity read onto the record's {@code year} flag (year
     *  true, month false). */
    private Condition.WholePeriod wholePeriod(JsonNode n) {
        if (n == null || n.isNull()) return null;
        String p = pathOf(n);
        fields(n, p, "WholePeriod", List.of("node", "granularity", "negated"), List.of());
        return new Condition.WholePeriod(enumValue(Spelling.WHOLE_PERIOD_GRANULARITY, "WholePeriod.granularity",
                textAt(n, p, "granularity"), p + "/granularity"), bool(n, "negated"));
    }

    // --- resolved IR lists -------------------------------------------------

    /** One version of a named form: the name its record carries, the JSON pointer of the version, and the version
     *  record itself. */
    private record Version(String name, String path, JsonNode jsonNode) {}

    /**
     * IR-SPEC ch. 3, Versioning (delta B19): a named form is {@code {node, name, versions}}, and the frontend's
     * records hold one entry per version. A form with two or more versions names them {@code "<name> #<i>"}, i from
     * 1 in wire order, as the frontend's resolver names them; a form with one keeps its bare name; a null name
     * (WF-107's) stays null on every version. That spelling is why a form's own name may not end in a space, '#'
     * and digits: it would read as a version of another form. A form carries at least one version.
     */
    private List<Version> formName(JsonNode n, String path, String expectedKind, List<String> optional) {
        List<String> opt = new ArrayList<>(optional);
        opt.add("origin");     // the origin of a declaration a producer made itself (IR-SPEC ch. 2.13)
        opt.add("designations");   // a named form's names in the vocabularies (IR-SPEC ch. 2.12), on the form
        fields(n, path, expectedKind, List.of("node", "name", "versions"), opt);
        JsonNode nm = n.get("name");
        if (!nm.isNull() && !nm.isTextual())
            throw new DocumentError(path + "/name", "expected a string or null, got " + shortQuote(nm));
        String name = nm.isNull() ? null : nm.asText();
        if (name != null) {
            origin(n, path, RegelSpraakModel.Designatable.form(name));
            designations(n, path, RegelSpraakModel.Designatable.form(name));
        }
        if (name != null && VERSION_NAME.matcher(name).matches()) throw new DocumentError(path + "/name", FORM_NAME);
        String vp = path + "/versions";
        JsonNode vs = n.get("versions");
        if (!vs.isArray()) throw new DocumentError(vp, "expected an array, got " + shortQuote(vs));
        if (vs.isEmpty()) throw new DocumentError(vp, VERSIONS_EMPTY);
        List<Version> out = new ArrayList<>();
        for (int i = 0; i < vs.size(); i++) {
            JsonNode v = vs.get(i);
            if (!v.isObject()) throw new DocumentError(vp + "/" + i, "expected a JSON object, got " + shortQuote(v));
            out.add(new Version(name != null && vs.size() >= 2 ? name + " #" + (i + 1) : name, vp + "/" + i, v));
        }
        return out;
    }

    /** A name ending in a space, '#' and digits: the in-memory spelling of one version of a form with several. */
    private static final java.util.regex.Pattern VERSION_NAME = java.util.regex.Pattern.compile("[\\s\\S]* #[0-9]+");

    /** The shared loader's sentences for a form's name and its versions. */
    static final String FORM_NAME = "a form's name does not end in a space, '#' and digits: that spelling names one"
            + " version of a form with several";
    static final String VERSIONS_EMPTY = "a form carries at least one version";

    /** The key set of a version record, which carries no node discriminator: {@link #fields} without the node, and an
     *  unknown field named on the record's own kind. */
    private static void versionFields(JsonNode n, String path, String category, List<String> required,
                                     List<String> optional) {
        n.fieldNames().forEachRemaining(k -> {
            if (!required.contains(k) && !optional.contains(k))
                throw new DocumentError(path + "/" + k, "unknown field on " + category);
        });
        for (String k : required)
            if (!n.has(k)) throw missing(path, k);
        for (String k : optional)
            if (n.has(k) && n.get(k).isNull())
                throw new DocumentError(path + "/" + k, "an optional field is written whole or not at all, and null is neither");
    }

    /** A field that holds a node, required: a scalar or null there is refused at the field's pointer. */
    private static JsonNode jsonNode(JsonNode parent, String path, String fieldName) {
        JsonNode c = parent.get(fieldName);
        if (!c.isObject()) throw new DocumentError(path + "/" + fieldName, "expected a JSON object, got " + shortQuote(c));
        return c;
    }

    /** A field whose value is a string or null (IR-SPEC § Method: a name a WF rule guards is nullable). */
    private static String textOrNull(JsonNode n, String path, String fieldName) {
        JsonNode v = n.get(fieldName);
        if (v.isNull()) return null;
        if (!v.isTextual()) throw new DocumentError(path + "/" + fieldName, "expected a string or null, got " + shortQuote(v));
        return v.asText();
    }

    /** IR-SPEC ch. 3.10: the validity of a version, always written; each bound is written whole or not at all, an
     *  absent one open on that side, and a Validity with neither bound is always. A malformed bound is named by its
     *  JSON pointer. */
    private Validity validity(JsonNode version, String path) {
        JsonNode g = jsonNode(version, path, "validity");
        String gp = path + "/validity";
        fields(g, gp, "Validity", List.of("node"), List.of("from", "to"));
        String from = optText(g, gp, "from"), to = optText(g, gp, "to");
        return new Validity(from == null ? null : dateOfIrWire(from, gp + "/from"),
                to == null ? null : dateOfIrWire(to, gp + "/to"));
    }

    /** IR-SPEC ch. 3.1: a rule and its versions; the statute annotations stand on the form, and every version's
     *  record carries them. */
    private void rule(String path, JsonNode n) {
        List<Version> versions = formName(n, path, "Rule", List.of("statuteAnnotations"));
        List<String> annotations = n.has("statuteAnnotations") ? textList(n, path, "statuteAnnotations") : List.of();
        for (Version v : versions) m.resolvedRules.add(ruleVersion(v, annotations));
    }

    /**
     * A rule version's kind is read, never looked up (IR-SPEC ch. 3.1; delta B20): a presentation tag is the kind
     * it spells, initialisation true is an initialisation, and a version with neither assigns a characteristic;
     * initialisation true beside a presentation is no kind at all. The target is an attribute reference, and which
     * member it names is the declaration's, crossed here against the kind as the verifier crosses it (WF-022): a
     * version with a tag or an initialisation writes an attribute, a version with neither a characteristic, and a
     * name that is both decides nothing. Without the cross a version without a tag that names a Booleaan attribute
     * would be carried out as a characteristic of that name.
     */
    private ResolvedRule ruleVersion(Version version, List<String> annotations) {
        JsonNode n = version.jsonNode();
        String path = version.path();
        versionFields(n, path, "RuleVersion", List.of("validity", "target", "value", "initialisation"),
                List.of("condition", "presentation"));
        boolean initialisation = truth(n, path, "initialisation");
        String presentation = optText(n, path, "presentation");
        ResolvedRule.Kind tagged = presentation == null ? null : enumValue(Spelling.PRESENTATION,
                "RuleVersion.presentation", presentation, path + "/presentation");
        if (initialisation && presentation != null)
            throw new DocumentError(path + "/presentation", PRESENTATION_AT_INITIALISATION);
        ResolvedRule.Kind ruleKind = initialisation ? ResolvedRule.Kind.INITIALISATIE
                : tagged == null ? ResolvedRule.Kind.KENMERKTOEKENNING : tagged;
        JsonNode t = n.get("target");
        if (!t.isObject() || !"AttributeReference".equals(text(t, "node")))
            throw new DocumentError(path + "/target", TARGET_ATTRIBUTE_REFERENCE);
        Expr.AttributeReference target = (Expr.AttributeReference) expr(t);
        checkMembersKind(target, text(t, "subjectType"), ruleKind, path + "/target");
        ResolvedRule r = new ResolvedRule(version.name(), ruleKind, target, expr(jsonNode(n, path, "value")),
                condition(nodeField(n, "condition")), validity(n, path), annotations);
        positions.version(r, path);
        // WF-022's value clause: a characteristic attribution writes the boolean constant true. Neither branch
        // reads the value — the exec-model one writes true whatever it says, the DRL-text one writes what it
        // says — so on a false (measured 2026-09-23 on the negative wf022-characteristic-false, verifier bypassed)
        // they gave opposite answers where the ASP backend refuses. Refused here, once for both branches.
        if (ruleKind == ResolvedRule.Kind.KENMERKTOEKENNING
                && !(r.value() instanceof Expr.Literal l && "waar".equals(l.valueText())
                     && l.type() instanceof Datatype.BooleanType)) {
            String value = r.value() instanceof Expr.Literal l ? "'" + l.valueText() + "'"
                    : "een " + r.value().getClass().getSimpleName();
            throw new Refusal(DroolsCode.CHARACTERISTIC_FALSE_OUTSIDE_DEFAULTS.reason("rule", r.name(), "characteristic", target.attribute(), "value", value), "RuleVersion.value");
        }
        return r;
    }

    /** The shared loader's sentences for a rule version's kind and target. */
    static final String PRESENTATION_AT_INITIALISATION = "presentation beside initialisation true: a presentation tags"
            + " a computing version, never an initialisation";
    static final String TARGET_ATTRIBUTE_REFERENCE = "the target of a rule version is an AttributeReference";

    /** WF-022, the member-kind arm and the name that is both (IR-SPEC ch. 3.1): the verifier's sentences, which
     *  name the member and the type as the target writes them. A target whose type or member does not resolve is
     *  left to the rule that reports it. */
    private void checkMembersKind(Expr.AttributeReference target, String typeName, ResolvedRule.Kind ruleKind, String site) {
        ObjectType ot = target.objectType();
        String name = target.attribute();
        if (ot == null || name == null) return;
        boolean attribute = false, characteristic = false;
        for (ObjectType t : ot.chain()) {
            for (String a : t.attributes.keySet()) attribute |= Namen.norm(a).equals(Namen.norm(name));
            for (String k : t.characteristics) characteristic |= Namen.norm(k).equals(Namen.norm(name));
        }
        if (attribute && characteristic) throw new DocumentError(site, targetTwofold(name, typeName));
        if (ruleKind == ResolvedRule.Kind.KENMERKTOEKENNING && attribute)
            throw new DocumentError(site, characteristicTarget(name, typeName));
        if (ruleKind != ResolvedRule.Kind.KENMERKTOEKENNING && characteristic)
            throw new DocumentError(site, attributeTarget(name, typeName));
    }

    /** WF-022's two member-kind sentences and the one for a name that is both. */
    static String characteristicTarget(String name, String type) {
        return "a version without a presentation and without initialisation attributes a characteristic, so its"
                + " target MUST name one, and '" + name + "' is an attribute of '" + type + "'";
    }
    static String attributeTarget(String name, String type) {
        return "a version with a presentation or with initialisation true writes an attribute, so its target MUST"
                + " name one, and '" + name + "' is a characteristic of '" + type + "'";
    }
    static String targetTwofold(String name, String type) {
        return "target '" + name + "' names both an attribute and a characteristic of '" + type + "': which member a"
                + " rule writes decides its kind, and this name decides nothing";
    }

    /** IR-SPEC ch. 3.3: an object creation and its versions; the back-link is optional. */
    private void objectCreation(String path, JsonNode n) {
        for (Version v : formName(n, path, "ObjectCreation", List.of())) {
            JsonNode k = v.jsonNode();
            String p = v.path();
            versionFields(k, p, "ObjectCreationVersion", List.of("createdType", "subject", "initialisations", "validity"),
                    List.of("condition", "connectingRole"));
            List<ResolvedObjectCreation.MemberInit> tk = new ArrayList<>();
            list(k, p, "initialisations", (tp, t) -> {
                fields(t, tp, "MemberInit", List.of("node", "member", "value", "characteristic"), List.of());
                tk.add(new ResolvedObjectCreation.MemberInit(text(t, "member"), expr(nodeField(t, "value")),
                        bool(t, "characteristic")));
            });
            ResolvedObjectCreation oc = new ResolvedObjectCreation(v.name(), ot(textOrNull(k, p, "createdType")),
                    ot(textOrNull(k, p, "subject")), tk, condition(nodeField(k, "condition")), validity(k, p));
            // IR-SPEC ch. 3.3: the symbolic connectingRole is NOT a field of the (vendored, pinned)
            // ResolvedObjectCreation record, so it cannot be reconstructed INTO the model the way a fact creation's
            // path can. ch. 5.5 makes it AUTHORITATIVE (the reference interpreter lets it win over the type-pair
            // derivation, and it is the only route for a self-typed pair), so it rides BESIDE the model.
            RoleBindingRef vr = roleBinding(k, p, "connectingRole");
            if (vr != null) roleBindings.put(oc, vr);
            positions.version(oc, p);
            m.objectCreations.add(oc);
        }
    }

    /** IR-SPEC ch. 3.2: an allocation and its versions: one of the three methods, the optional fields absent when
     *  none, and the recipient join required. */
    private void allocation(String path, JsonNode n) {
        for (Version v : formName(n, path, "Allocation", List.of())) {
            JsonNode k = v.jsonNode();
            String p = v.path();
            versionFields(k, p, "AllocationVersion", List.of("sourceType", "sourceAttribute", "targetType",
                    "targetAttribute", "method", "descending", "validity", "connectingRole"), List.of("ratioAttribute",
                    "maximumAttribute", "roundingDecimals", "remainderAttribute", "orderingAttribute", "condition"));
            ResolvedAllocation.Method method = enumValue(Spelling.METHOD, "AllocationVersion.method",
                    textAt(k, p, "method"), p + "/method");
            ResolvedAllocation vd = new ResolvedAllocation(v.name(), ot(textOrNull(k, p, "sourceType")),
                    textOrNull(k, p, "sourceAttribute"), ot(textOrNull(k, p, "targetType")),
                    textOrNull(k, p, "targetAttribute"), method,
                    optText(k, p, "ratioAttribute"), optText(k, p, "maximumAttribute"),
                    k.has("roundingDecimals") ? whole(k, p, "roundingDecimals", "AllocationVersion.roundingDecimals")
                            : null,
                    optText(k, p, "remainderAttribute"), optText(k, p, "orderingAttribute"),
                    truth(k, p, "descending"), condition(nodeField(k, "condition")), validity(k, p));
            // IR-SPEC ch. 3.2: the recipient role binding IS the join, and it is required. Not a field of the
            // vendored record, so it rides beside the model like the object creation's; see Loaded#roleBindings.
            RoleBindingRef vr = roleBinding(k, p, "connectingRole");
            if (vr == null) throw new DocumentError(p + "/connectingRole", "expected a RoleBinding, got null");
            roleBindings.put(vd, vr);
            positions.version(vd, p);
            m.allocations.add(vd);
        }
    }

    /** IR-SPEC ch. 3.5: a consistency rule and its versions; the source record, with its line, stands on the
     *  version. */
    private void consistency(String path, JsonNode n) {
        for (Version v : formName(n, path, "Consistency", List.of())) {
            JsonNode k = v.jsonNode();
            String p = v.path();
            versionFields(k, p, "ConsistencyVersion", List.of("subject", "assertion", "validity"), List.of("source"));
            ResolvedConsistency c = new ResolvedConsistency(v.name(), ot(textOrNull(k, p, "subject")),
                    condition(jsonNode(k, p, "assertion")), source(k), sourceLine(k), validity(k, p));
            positions.version(c, p);
            m.consistencies.add(c);
        }
    }

    /** IR-SPEC ch. 3.6 (delta B27): a uniqueness and its versions. A version with one source is the single-type
     *  form and a record of the model's first list, with two or more the union and a record of its second, which
     *  carries no scope (a scoped union is an open design question). The order of the wire's one list carries no
     *  meaning. */
    private void uniqueness(String path, JsonNode n) {
        for (Version v : formName(n, path, "Uniqueness", List.of())) {
            JsonNode k = v.jsonNode();
            String p = v.path();
            versionFields(k, p, "UniquenessVersion", List.of("sources", "validity"), List.of("scope", "source"));
            List<ResolvedUniquenessUnion.Source> sources = new ArrayList<>();
            list(k, p, "sources", (sp, s) -> {
                fields(s, sp, "Source", List.of("node", "objectType", "keys"), List.of());
                String kp = sp + "/keys";
                JsonNode keys = s.get("keys");
                if (!keys.isArray()) throw new DocumentError(kp, "expected an array, got " + shortQuote(keys));
                if (keys.isEmpty()) throw new DocumentError(kp, KEYS_EMPTY);
                List<String> keyNames = new ArrayList<>();
                for (int i = 0; i < keys.size(); i++) keyNames.add(textOrNull(keys, kp, i));
                sources.add(new ResolvedUniquenessUnion.Source(ot(textOrNull(s, sp, "objectType")), keyNames));
            });
            if (sources.isEmpty()) throw new DocumentError(p + "/sources", SOURCES_EMPTY);
            Validity parsedValidity = validity(k, p);
            if (sources.size() == 1) {
                // IR-SPEC ch. 3.6: the scope binding IS the scoping; the record's field-name component stays empty
                ResolvedUniquenessUnion.Source b = sources.get(0);
                m.uniquenesses.add(new ResolvedUniqueness(v.name(), b.type(), b.keys(), null, source(k), parsedValidity,
                        roleBinding(k, p, "scope")));
            } else {
                if (k.has("scope")) throw new DocumentError(p + "/scope", SCOPED_UNION);
                m.uniquenessUnions.add(new ResolvedUniquenessUnion(v.name(), sources, source(k), parsedValidity));
            }
        }
    }

    /** The shared loader's sentences for the sources of a uniqueness version. */
    static final String SOURCES_EMPTY = "a uniqueness version carries at least one source";
    static final String KEYS_EMPTY = "a source carries at least one key";
    static final String SCOPED_UNION = "a uniqueness version with two or more sources carries no scope: a scoped"
            + " union is an open design question";

    /** An entry of a list of strings or nulls. */
    private static String textOrNull(JsonNode list, String path, int i) {
        JsonNode v = list.get(i);
        if (v.isNull()) return null;
        if (!v.isTextual()) throw new DocumentError(path + "/" + i, "expected a string or null, got " + shortQuote(v));
        return v.asText();
    }

    /**
     * IR-SPEC ch. 3.4: a fact creation and its versions. The creation IS its symbolic block — the created relation,
     * both ends, and the path from the anchor, one binding per hop — and each part is required. The record's
     * bean-field components stay empty; the transpilers derive the fields from the bindings
     * ({@code FactModelGenerator.creationFields}). Every hop of the path but the last is a step, the last is the join.
     * The target type is not written: it is the declared filler of the target role in the created fact type, found
     * as the verifier finds it, and where that does not resolve the version is refused in the verifier's WF-028
     * words — a record without a target type would join to nothing. The anchor role is resolved the same way and
     * refused in the same words: the record keeps it by name, and a name that is no role of the created relation
     * reached both branches as a creation they could only call unresolved.
     */
    private void factCreation(String path, JsonNode n) {
        for (Version v : formName(n, path, "FactCreation", List.of())) {
            JsonNode k = v.jsonNode();
            String p = v.path();
            versionFields(k, p, "FactCreationVersion", List.of("factType", "anchorRole", "targetRole", "anchor", "path",
                    "validity"), List.of("condition"));
            List<RoleBindingRef> hops = roleBindingList(k, p, "path");
            if (hops.isEmpty()) throw new DocumentError(p + "/path", PATH_EMPTY);
            List<ResolvedFactCreation.PathStep> ts = new ArrayList<>();
            for (RoleBindingRef rb : hops.subList(0, hops.size() - 1))
                ts.add(new ResolvedFactCreation.PathStep(null, null, false, rb));
            String factType = textAt(k, p, "factType"), anchorRole = textAt(k, p, "anchorRole"),
                    targetRole = textAt(k, p, "targetRole");
            ResolvedFactCreation fc = new ResolvedFactCreation(v.name(), ot(textOrNull(k, p, "anchor")),
                    targetKind(p, factType, anchorRole, targetRole), null, null, ts, condition(nodeField(k, "condition")),
                    validity(k, p), factType, anchorRole, targetRole, hops.get(hops.size() - 1));
            positions.version(fc, p);
            m.factCreations.add(fc);
        }
    }

    /** The shared loader's sentence for an empty path. */
    static final String PATH_EMPTY = "a fact creation's path carries at least one hop";

    /** The target type of a fact creation: the declared filler of the target role in the created fact type, under
     *  the verifier's name identity. What does not resolve is refused, in WF-028's sentences, the anchor role first
     *  as the verifier reports it. */
    private ObjectType targetKind(String p, String factType, String anchorRole, String targetRole) {
        FactType ft = factTypeByName(factType);
        if (ft == null)
            throw new DocumentError(p + "/factType", "created fact type '" + factType + "' is not declared");
        if (ft.roles.size() != 2)
            throw new DocumentError(p + "/factType", "created fact type '" + factType + "' does not have exactly two"
                    + " roles");
        roleOf(p + "/anchorRole", ft, factType, anchorRole);
        RegelSpraakModel.Role target = roleOf(p + "/targetRole", ft, factType, targetRole);
        ObjectType filler = ot(target.objectTypeName());
        if (filler == null)
            throw new DocumentError(p + "/targetRole", "the role '" + targetRole + "' of '" + factType + "' is filled by '"
                    + target.objectTypeName() + "', which is no declared object type: a fact creation's target"
                    + " type is the filler of its target role");
        return filler;
    }

    /** The role {@code role} of the created fact type {@code ft} under the verifier's name identity, or WF-028's
     *  refusal at {@code path}. */
    private static RegelSpraakModel.Role roleOf(String path, FactType ft, String factType, String role) {
        List<RegelSpraakModel.Role> roles = ft.roles.stream()
                .filter(r -> r.name() != null && Namen.norm(r.name()).equals(Namen.norm(role))).toList();
        if (roles.isEmpty())
            throw new DocumentError(path, "'" + role + "' is not a role of '" + factType + "'");
        if (roles.size() > 1)
            throw new DocumentError(path, "role '" + role + "' is ambiguous within '" + factType + "'");
        return roles.get(0);
    }

    /** The RoleBinding field {@code fieldName} of the object at {@code path}: absent or null is null (the caller decides
     *  whether that is allowed), anything else is a {@code RoleBinding} object with exactly its two string fields. */
    private static RoleBindingRef roleBinding(JsonNode parent, String path, String fieldName) {
        JsonNode n = parent.get(fieldName);
        if (n == null || n.isNull()) return null;
        return roleBindingOn(n, path + "/" + fieldName);
    }

    private static RoleBindingRef roleBindingOn(JsonNode n, String path) {
        fields(n, path, "RoleBinding", List.of("node", "factType", "role"), List.of());
        return new RoleBindingRef(textAt(n, path, "factType"), textAt(n, path, "role"));
    }

    /** A list of RoleBindings at field {@code fieldName} of the object at {@code path}. */
    private static List<RoleBindingRef> roleBindingList(JsonNode parent, String path, String fieldName) {
        List<RoleBindingRef> out = new ArrayList<>();
        list(parent, path, fieldName, (sp, s) -> out.add(roleBindingOn(s, sp)));
        return out;
    }

    /** The validity window of {@code owner} — a day-kind definition or a defaults group, the two carriers outside
     *  the named forms, whose validity may be null (every version of a named form carries its {@code validity}, see
     *  {@link #validity}) — read from ITS node, not from a detached child, so a malformed bound can be reported
     *  against the declaration that carries it: "from" alone does not locate the defect among the hundreds of windows
     *  a document holds. This mirrors {@link #source(JsonNode)} one method below, which reads its own field the same
     *  way.
     *  <p>{@code nameField} is the carrier's name field ({@code dayKind} on a day-kind definition, {@code name} on a
     *  defaults group). The prefix falls back to the NODE KIND where a carrier has none: that field is
     *  {@code ["string","null"]} on both carrying node kinds, so a nameless carrier is a legal wire document, and a
     *  bare "validity.from" would locate nothing. */
    private Validity parsedValidity(JsonNode owner, String nameField) {
        JsonNode n = nodeField(owner, "validity");
        if (n == null) return null;
        String gp = pathOf(n);
        fields(n, gp, "Validity", List.of("node"), List.of("from", "to"));
        String name = text(owner, nameField), category = text(owner, "node");
        String site = (name != null ? name : "<unnamed " + (category == null ? "carrier" : category) + ">")
                + ".validity";
        String from = optText(n, gp, "from"), to = optText(n, gp, "to");
        return new Validity(from == null ? null : dateOfIrWire(from, site + ".from"),
                to == null ? null : dateOfIrWire(to, site + ".to"));
    }

    /* The uchar-ir-v1 validity-bound lexeme, matched WHOLE: four-two-two digit fields, digits only, no
     * sign and no extended year. It is WF-084's date-literal grammar (IR-SPEC ch. 4.1) applied to the other
     * date position on this wire — where, unlike the case wire, the schema carried no pattern at all. */
    private static final java.util.regex.Pattern IR_DATE =
            java.util.regex.Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    /** The calendar day a validity bound names — or a loud refusal naming the FORM, never a guess.
     *  <p>Until 2026-09-23 the three other readers of this wire held a bound as an OPAQUE STRING and
     *  ordered it lexicographically; since card ref-geldigheid-datumlezing only the verifier still does
     *  (sound under WF-101), and ref and the ASP backend read the bounds through their strict date reader. That ordering is only sound INSIDE this lexeme, and nothing
     *  enforced it: measured 2026-09-11 on a hand-edited copy of a two-version golden, the verifier reported
     *  PASS on {@code +012026-01-01}, {@code -0500-01-01}, {@code 2026-02-30} and {@code niet-een-datum}
     *  alike. On {@code +012026-01-01} the interpreter and the ASP backend then applied the version (the
     *  signed lexeme sorts before every four-digit one) while this reader did not (year 12026) — one
     *  document, two answers, and the differential gate sees it only when a pinned expectation happens to
     *  move. So each reader holds its own fence (IR-SPEC ch. 4.1), and this is that fence, one field deeper
     *  than the schema pattern proposed alongside it: the pattern is the form and cannot know the calendar,
     *  which is the second branch here. A malformed bound is a document defect, not a form this backend does
     *  not carry, so it is a hard error and never an {@link Refusal} — a Refusal would file the document
     *  under the differential gate's auto-excluded channel and leave the gate green over a document nobody
     *  read. */
    private static java.time.LocalDate dateOfIrWire(String lexeme, String fieldName) {
        if (!IR_DATE.matcher(lexeme).matches())
            throw new IllegalArgumentException("malformed validity date '" + lexeme + "' on '" + fieldName
                    + "': the uchar-ir-v1 lexeme is yyyy-mm-dd — fixed field widths, digits only,"
                    + " no sign and no extended year");
        try { return java.time.LocalDate.parse(lexeme); }
        catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("malformed validity date '" + lexeme + "' on '" + fieldName
                    + "': the field widths are right but the calendar is not (" + e.getMessage() + ")");
        }
    }

    // --- helpers -----------------------------------------------------------

    private ObjectType ot(String name) { return name == null ? null : m.objectTypeByName(name); }
    private Parameter param(String name) { return name == null ? null : m.parameters.get(name); }

    /** IR-SPEC ch. 4 (B26): the source record of {@code n}, null where the node carries none. Present, it is an
     *  object with its snippet — a string, never empty — and an optional line of at least 1, and nothing beside
     *  them: a file or a column is an unknown field until a card writes positions. Every other form is refused at
     *  its pointer. A line is read wherever it stands; the two records with a place for it keep it
     *  ({@link #sourceLine}), and every other node drops it. */
    private SourceReference sourceReference(JsonNode n) {
        JsonNode s = n.get("source");
        if (s == null) return null;
        String p = pathOf(n) + "/source";
        fields(s, p, null, List.of("snippet"), List.of("line"));
        String snippet = textAt(s, p, "snippet");
        if (snippet.isEmpty()) throw new DocumentError(p + "/snippet", SNIPPET_EMPTY);
        int line = 0;
        if (s.has("line")) {
            line = whole(s, p, "line", "source.line");
            if (line < 1) throw new DocumentError(p + "/line", SOURCE_LINE_UNDER_1);
        }
        return new SourceReference(snippet, line);
    }

    /** A source record as this loader holds it: the snippet, and the line or 0 where the record has none. */
    private record SourceReference(String snippet, int line) {}

    /** The shared loader's sentence: an empty snippet has no reading, as a node without source text carries no
     *  record. */
    static final String SNIPPET_EMPTY = "a source snippet is never empty: a node without source text carries no source";
    /** The shared loader's sentence for a line below 1, on every node. */
    static final String SOURCE_LINE_UNDER_1 = "a source line is at least 1";

    /** The anchor a node's record holds: its snippet, and "" where it carries no source record — the value the
     *  record held for a node without source text before the source record landed. */
    private Source source(JsonNode n) {
        SourceReference b = sourceReference(n);
        return Source.synthetic(b == null ? "" : b.snippet());
    }

    /** The source text of a node of the defaults layer, whose records keep it as a string: null where it carries
     *  no source record. */
    private String sourceText(JsonNode n) {
        SourceReference b = sourceReference(n);
        return b == null ? null : b.snippet();
    }

    /** The source line of a consistency version or a rule group, the two records with a place for one: 0 where the
     *  source record has none or the node carries no record, the records' value for "no line". */
    private int sourceLine(JsonNode n) {
        SourceReference b = sourceReference(n);
        return b == null ? 0 : b.line();
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static boolean bool(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v != null && v.asBoolean();
    }

    private static int intOf(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v == null || v.isNull() ? 0 : v.asInt();
    }

    private static List<String> stringList(JsonNode n) {
        List<String> out = new ArrayList<>();
        if (n != null && n.isArray()) n.forEach(e -> out.add(e.asText()));
        return out;
    }

    private static void forEach(JsonNode array, java.util.function.Consumer<JsonNode> f) {
        if (array != null && array.isArray()) array.forEach(f);
    }

    /** The node at {@code fieldName} of {@code parent}: null when the field is absent or null, and a scalar or an array
     *  there refused at the field's own pointer. A node is found by identity ({@link #pathOf}), which a scalar does
     *  not have — Jackson shares small numbers, booleans and null — so its place is the parent's and the field's. */
    private JsonNode nodeField(JsonNode parent, String fieldName) {
        JsonNode c = parent.get(fieldName);
        if (c == null || c.isNull()) return null;
        if (!c.isObject()) throw new DocumentError(pathOf(parent) + "/" + fieldName, "expected a JSON object, got " + shortQuote(c));
        return c;
    }

    /** Each node of the list at {@code fieldName} of {@code parent}, in order; an absent list is empty, and an entry that is
     *  not an object is refused at its own pointer (see {@link #nodeField}). */
    private void elements(JsonNode parent, String fieldName, java.util.function.Consumer<JsonNode> f) {
        JsonNode a = parent.get(fieldName);
        if (a == null || !a.isArray()) return;
        for (int i = 0; i < a.size(); i++) {
            JsonNode e = a.get(i);
            if (!e.isObject()) throw new DocumentError(pathOf(a) + "/" + i, "expected a JSON object, got " + shortQuote(e));
            f.accept(e);
        }
    }

    // --- WF-035 / WF-038: the operand typing of date arithmetic and of the builtins --------------------------------

    /** The operand typing the verifier refuses under WF-035 (date ± duration: the form, and the duration's unit) and
     *  WF-038 (the operand of a square root or an absolute value, and the duration clause), refused here at load in
     *  the sentence the verifier, {@code ref} and the ASP backend share ({@code spec/verifier/lib/checks.ml}), at the
     *  pointer the verifier names. Without it both branches lowered what they could and answered for the rest in
     *  words of their own: a duration plus a date PLAYED with the operands silently commuted, a text operand under a
     *  square root faulted at fire, and the other forms were refused as rules out of scope (measured 2026-09-25).
     *  Asked once the whole document is read, as the verifier asks it: a reference's type is its declaration's, a
     *  binder read's its binding's ({@link EffectiveUnit#typeOf}), and a type the document does not give
     *  defers, as the verifier defers on it. */
    private void checkTyping() {
        EffectiveUnit types = new EffectiveUnit(lets);
        for (ResolvedRule r : m.resolvedRules) {
            checkTypes(r.value(), types);
            checkTypes(r.condition(), types);
        }
        for (ResolvedDayKindDefinition d : m.resolvedDayKindDefinitions) checkTypes(d.condition(), types);
        for (ResolvedObjectCreation oc : m.objectCreations) {
            for (ResolvedObjectCreation.MemberInit t : oc.initialisations()) checkTypes(t.value(), types);
            checkTypes(oc.condition(), types);
        }
        for (ResolvedAllocation v : m.allocations) checkTypes(v.condition(), types);
        for (ResolvedConsistency c : m.consistencies) checkTypes(c.assertion(), types);
        for (ResolvedFactCreation fc : m.factCreations) checkTypes(fc.condition(), types);
        for (PrioritizedDefaults pd : prioritizedDefaults)
            for (Default d : pd.defaults()) checkDefaultTypes(d, types);
    }

    private void checkDefaultTypes(Default d, EffectiveUnit types) {
        checkTypes(d.condition(), types);
        checkTypes(d.consequence(), types);
        if (d.exceptions() != null)
            for (ExceptionGroup g : d.exceptions().members())
                for (Default x : g.defaults()) checkDefaultTypes(x, types);
    }

    /** Every node below a value position. Exhaustive over the sealed union, so a node the union grows is a compile
     *  error here rather than a subtree the walk skips. */
    private void checkTypes(Expr e, EffectiveUnit types) {
        switch (e) {
            case null -> { }
            case Expr.Binary b -> {
                checkDateCalculation(b, types);
                checkTypes(b.left(), types);
                checkTypes(b.right(), types);
            }
            case Expr.FunctionCall f -> {
                checkBuildingBlock(f, types);
                for (Expr x : f.arguments()) checkTypes(x, types);
                checkTypes(f.emptyFallback(), types);
                checkTypes(f.filter(), types);
            }
            case Expr.Rounding x -> checkTypes(x.base(), types);
            case Expr.Clamp x -> {
                checkTypes(x.base(), types); checkTypes(x.minimum(), types); checkTypes(x.maximum(), types);
            }
            case Expr.TextComposition t -> {
                for (Expr.TextComposition.Part d : t.parts())
                    if (d instanceof Expr.TextComposition.EmbeddedExpression i) checkTypes(i.expr(), types);
            }
            case Expr.Let l -> { checkTypes(l.value(), types); checkTypes(l.body(), types); }
            case Expr.IfThenElse k -> {
                checkTypes(k.condition(), types); checkTypes(k.then(), types); checkTypes(k.otherwise(), types);
            }
            case Expr.InstanceSetUnion u -> {
                for (Expr.InstanceSetUnion.Branch t : u.branches()) checkTypes(t.filter(), types);
            }
            case Expr.DimensionCellRead c -> { }
            case Expr.Literal x -> { }
            case Expr.AttributeReference x -> { }
            case Expr.ParameterRef x -> { }
            case Expr.VariableRef x -> { }
            case Expr.BinderRef x -> { }
            case Expr.Constant x -> { }
            case Expr.Unresolved x -> { }
        }
    }

    private void checkTypes(Condition c, EffectiveUnit types) {
        switch (c) {
            case null -> { }
            case Condition.Comparison v -> { checkTypes(v.left(), types); checkTypes(v.right(), types); }
            case Condition.Predicate p -> checkTypes(p.operand(), types);
            case Condition.TimeWindow p -> { checkTypes(p.from(), types); checkTypes(p.until(), types); }
            case Condition.Compound x -> { for (Condition o : x.components()) checkTypes(o, types); }
            case Condition.QuantifierFold kv -> { checkTypes(kv.filter(), types); checkTypes(kv.test(), types); }
            case Condition.RoleMembership r -> { }
            case Condition.CharacteristicCheck k -> { }
            case Condition.IdentityCheck x -> { }
            case Condition.RuleStatusCheck x -> { }
            case Condition.Unresolved x -> { }
            case Condition.FarRoleMembership x -> { }
        }
    }

    /** WF-035 on plus and min (RUNTIME-SEMANTICS ch. 3.8): the form is decided on the date type of either precision
     *  — two dates, a duration on the left, or a date on the left and the step — and the step's duration is a number
     *  whose unit is a time unit, sub-day only on a millisecond-precision date, or a declared day kind. */
    private void checkDateCalculation(Expr.Binary b, EffectiveUnit types) {
        if (b.op() != Expr.BinOp.PLUS && b.op() != Expr.BinOp.MIN) return;
        Datatype l = types.typeOf(b.left());
        Datatype r = types.typeOf(b.right());
        if (!isDateType(l) && !isDateType(r)) return;
        String p = sites.get(b);
        if (isDateType(l) && isDateType(r)) throw new DocumentError(p, DATE_PLUS_DATE);
        if (!isDateType(l)) throw new DocumentError(p, durationBeforeDateRefusal(b.op() == Expr.BinOp.MIN));
        Unit u = r == null ? null : r.unitOf();
        if (u != null) {
            String time = TimeCalculation.chronoUnit(u);
            if (time != null) {
                if (SUB_DAY.contains(time) && l instanceof Datatype.DateInDays)
                    throw new DocumentError(p, SUB_DAY_ON_DAY_DATE);
            } else if (m.dayKindForUnit(u.abbreviation()) == null)
                throw new DocumentError(p + "/right", dateStepUnknownUnit(abbreviationOf(u)));
        } else if (knownType(r) && !r.isNumberLike())
            throw new DocumentError(p + "/right", dateStepOperandRefusal(r));
        else if (knownType(r)) throw new DocumentError(p + "/right", DATE_STEP_WITHOUT_TIME_UNIT);
    }

    /** WF-038 on the builtins that type their operands here: a square root and an absolute value take a number; a
     *  duration takes two dates of one precision and counts in its node type's unit, a time unit — sub-day only over
     *  millisecond-precision operands — or a declared day kind. In the verifier's order and at its pointers. */
    private void checkBuildingBlock(Expr.FunctionCall f, EffectiveUnit types) {
        Expr.FunctionKind k = f.kind();
        List<Expr> args = f.arguments();
        String p = sites.get(f);
        if ((k == Expr.FunctionKind.WORTEL || k == Expr.FunctionKind.ABSOLUTE_WAARDE) && args.size() == 1) {
            Datatype t = types.typeOf(args.get(0));
            if (knownType(t) && !t.isNumberLike()) throw new DocumentError(p, numericOperandRefusal(k, t));
            return;
        }
        if ((k != Expr.FunctionKind.TIJDSDUUR && k != Expr.FunctionKind.ABSOLUTE_TIJDSDUUR) || args.size() != 2) return;
        Datatype ta = types.typeOf(args.get(0));
        Datatype tb = types.typeOf(args.get(1));
        for (int i = 0; i < 2; i++) {
            Datatype t = i == 0 ? ta : tb;
            if (knownType(t) && !t.isDate()) throw new DocumentError(p + "/arguments/" + i, durationOperandRefusal(k, t));
        }
        if (isDateType(ta) && isDateType(tb) && ta.getClass() != tb.getClass())
            throw new DocumentError(p, durationMixedPrecision(k));
        Datatype typ = f.type();
        if (typ == null) throw new DocumentError(p, durationWithoutNodeType(k));
        Unit u = typ.unitOf();
        if (u == null) throw new DocumentError(p, durationWithoutTimeUnit(k));
        String time = TimeCalculation.chronoUnit(u);
        if (time != null) {
            if (SUB_DAY.contains(time) && (ta instanceof Datatype.DateInDays || tb instanceof Datatype.DateInDays))
                throw new DocumentError(p, durationSubDayOnDayDates(k));
        } else if (m.dayKindForUnit(u.abbreviation()) == null)
            throw new DocumentError(p, durationUnknownUnit(k, abbreviationOf(u)));
    }

    /** The calendar units below a day, as {@link TimeCalculation#chronoUnit} names them. */
    private static final java.util.Set<String> SUB_DAY = java.util.Set.of("HOURS", "MINUTES", "SECONDS", "MILLIS");

    private static boolean isDateType(Datatype t) {
        return t != null && t.isDate();
    }

    /** Whether the document gives this type: an absent or unknown one defers, as the verifier's does. */
    private static boolean knownType(Datatype t) {
        return t != null && !t.isUnknown();
    }

    private static String abbreviationOf(Unit u) {
        return u.abbreviation() == null ? "?" : u.abbreviation();
    }

    /** The verifier's name of a datatype in these sentences ({@code datatype_kind}); an unknown type is never named,
     *  it defers. */
    static String typeKind(Datatype d) {
        return switch (d) {
            case Datatype.Numeric x -> "number";
            case Datatype.Percentage x -> "percentage";
            case Datatype.Text x -> "text";
            case Datatype.BooleanType x -> "boolean";
            case Datatype.DateInDays x -> "date-day";
            case Datatype.DateTime x -> "date-ms";
            case Datatype.Enumeration x -> "enumeration";
            case Datatype.Unknown x -> throw new IllegalArgumentException("an unknown type defers and is never named");
        };
    }

    // The sentences below are the shared OCaml ones, word for word (spec/verifier/lib/checks.ml).

    static final String DATE_PLUS_DATE =
            "date \u00b1 date is ill-formed (date arithmetic takes a date and a duration)";

    static String durationBeforeDateRefusal(boolean min) {
        return min ? "duration \u2212 date is ill-formed (subtraction admits the date on the left only)"
                : "duration + date is ill-formed (the date stands on the left; date arithmetic does not commute)";
    }

    static final String DATE_STEP_WITHOUT_TIME_UNIT =
            "date \u00b1 duration: the duration operand carries no time unit";

    static final String SUB_DAY_ON_DAY_DATE =
            "sub-day time unit on a day-precision date (millisecond precision required)";

    static String dateStepUnknownUnit(String abbreviation) {
        return "date \u00b1 duration: the duration unit '" + abbreviation
                + "' is neither a recognised time unit nor a declared day kind";
    }

    static String dateStepOperandRefusal(Datatype d) {
        return "date \u00b1 duration: the duration operand is " + typeKind(d)
                + ", must be a number carrying a time unit";
    }

    static String numericOperandRefusal(Expr.FunctionKind k, Datatype d) {
        return Spelling.BUILTIN_KIND.spelling(k) + " operand is " + typeKind(d) + ", must be a number";
    }

    static String durationOperandRefusal(Expr.FunctionKind k, Datatype d) {
        return Spelling.BUILTIN_KIND.spelling(k) + " operand is " + typeKind(d) + ", must be a date";
    }

    static String durationMixedPrecision(Expr.FunctionKind k) {
        return Spelling.BUILTIN_KIND.spelling(k) + " operands mix date precisions (both must share one precision)";
    }

    static String durationWithoutNodeType(Expr.FunctionKind k) {
        return Spelling.BUILTIN_KIND.spelling(k) + " without a node type (the counted unit is the node type's unit)";
    }

    static String durationWithoutTimeUnit(Expr.FunctionKind k) {
        return Spelling.BUILTIN_KIND.spelling(k) + " without a recognised time unit on its node type";
    }

    static String durationSubDayOnDayDates(Expr.FunctionKind k) {
        return "sub-day " + Spelling.BUILTIN_KIND.spelling(k)
                + " unit on day-precision operands (millisecond precision required)";
    }

    static String durationUnknownUnit(Expr.FunctionKind k, String abbreviation) {
        return Spelling.BUILTIN_KIND.spelling(k) + ": the counted unit '" + abbreviation
                + "' is neither a recognised time unit nor a declared day kind";
    }
}
