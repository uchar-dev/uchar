package io.github.yhavinga.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;

import io.github.yhavinga.regelspraak.ir.Bron;
import io.github.yhavinga.regelspraak.ir.Conditie;
import io.github.yhavinga.regelspraak.ir.Default;
import io.github.yhavinga.regelspraak.ir.ExceptionGroup;
import io.github.yhavinga.regelspraak.ir.Expr;
import io.github.yhavinga.regelspraak.ir.Geldigheid;
import io.github.yhavinga.regelspraak.ir.PrioritizedDefaults;
import io.github.yhavinga.regelspraak.ir.ResolvedConsistentie;
import io.github.yhavinga.regelspraak.ir.ResolvedDagsoort;
import io.github.yhavinga.regelspraak.ir.ResolvedFeitCreatie;
import io.github.yhavinga.regelspraak.ir.ResolvedObjectCreatie;
import io.github.yhavinga.regelspraak.ir.ResolvedRegel;
import io.github.yhavinga.regelspraak.ir.ResolvedRegelGroep;
import io.github.yhavinga.regelspraak.ir.ResolvedStartpunt;
import io.github.yhavinga.regelspraak.ir.ResolvedUniciteit;
import io.github.yhavinga.regelspraak.ir.ResolvedUniciteitUnie;
import io.github.yhavinga.regelspraak.ir.ResolvedVerdeling;
import io.github.yhavinga.regelspraak.ir.Spelling;
import io.github.yhavinga.regelspraak.model.Datatype;
import io.github.yhavinga.regelspraak.model.Eenheid;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Diagnostic;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Dimensie;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Eenheidstelsel;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Feittype;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Granulariteit;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Objecttype;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Parameter;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.RolBindingRef;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Severity;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Tijdlijn;
import io.github.yhavinga.regelspraak.transpiler.kern.Namen;

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
 * types the transpiler consumes — the member nodes back into the model's side-maps — so the ported
 * {@code ExecModelTranspiler} fires the IR — never the RegelSpraak source.
 *
 * <p><b>Refusal discipline.</b> Every wire form the v0 Drools backend does not carry is a LOUD, per-form
 * {@link Refusal} — never a silent skip. That covers a {@code format} other than {@code uchar-ir-v1}, the
 * model's {@code functionDefs} extension, an integer beyond a Java {@code int}, and an unknown {@code node}
 * discriminator inside the {@link Expr} / {@link Conditie} / {@link Datatype} sealed hierarchies
 * (a struct/list literal, a MapList/Invoke, …). The differential gate turns a Refusal into a
 * reason-carrying exclusion. A document that is not well-formed is no form at all: the container, the
 * declaration lists, the M1 nodes ({@code ObjectType}, {@code Attribute}, {@code Characteristic},
 * {@code TimelineFacet}, {@code Domain} with its datatype) and each {@code Diagnostic} are read as strictly as the
 * OCaml loader reads them — exact keys, the node discriminator, the JSON types, WF-001/WF-002 name identity — and a
 * defect there is a {@link Documentfout} naming its JSON position, a hard error. One step stricter: a Diagnostic's
 * message must be a string, as the schema types it, where the OCaml loader admits a null so that WF-113 can name it.
 * Every datatype, at any position, carries its discriminator.
 *
 * <p><b>The reference layer (L2 of the v1 wire migration).</b> A reference to a declared thing carries its
 * symbolic form only, and the bindings are the one source of every relation this backend walks: the navigation of
 * a read ({@code path}) and of a characteristic check ({@code navigation}), the range of a fold and of a quantifier
 * fold ({@code sourceRole}, {@code sourcePath}), the
 * recipient join of an allocation and the back-link of an object creation ({@code connectingRole}), a union
 * selection's {@code sourceRole}, a role check's {@code factType}/{@code role}, a creation's symbolic block and a
 * uniqueness version's {@code scope}. A
 * reference node's type is the declaration's (IR-SPEC ch. 2.10), never an annotation. Since L2's fix round every
 * node this loader reads — not only the nodes L2 changed — is read with the schema's exact keys and a JSON pointer:
 * a field the shape does not have is an unknown field, a binding the shape requires is a missing field, and an
 * {@code Onbekend} datatype or a role check of the L1 shape is an unknown node — each a {@link Documentfout}. So is
 * a reference-layer shape every executor refuses by name: a fold's set source its kind cannot consume, a count's
 * range that is a path, a {@code total} without a range over another type than its subject, a role check naming no link
 * the subject can be in, and a characteristic read at a value position ({@link #keurVouwbron},
 * {@link #keurOnderwerpen}).
 *
 * <p><b>The type system (L3 of the v1 wire migration).</b> A number is a {@code Number}, a date a {@code Date} with
 * its precision, a unit a {@code Unit} whose factor is an exact fraction {@code {num, den}}, a unit system a list
 * of units with their spellings, a parameter carries its timeline facet and a day kind is a {@code DayKind} node. The
 * records keep their own shape and this loader lays the wire back onto them one to one: a {@code Number} whose unit
 * is '%' is a {@link Datatype.Percentage} (its presentation tag, legal only at a declaration, carries no meaning
 * here), any other a {@link Datatype.Numeriek}; {@code day} is {@link Datatype.DatumInDagen} and {@code millisecond}
 * {@link Datatype.DatumTijd}. The exact factor becomes the record's decimal {@code factorNaarCanoniek} — a whole
 * number exactly, any other fraction rounded to 16 significant digits ({@link MathContext#DECIMAL64}), the rounding
 * the frontend gives a declared "1/N" — and the unit system keeps the fraction itself beside it
 * ({@code Eenheidstelsel.exacteFactoren}). From there this backend restates between units with the ratio of those
 * decimals, where {@code ref} reckons with the fraction: a tracked delta of the Drools backend (CONSTRUCTS row 26). The
 * corpus does not see it, since every factor there is a whole number, but a verifier-clean document the frontend
 * emits does: with a unit declared "= 1/3" of another, 1.00 of the one plus 3.00 of the other reads 1.9999999999999999
 * in a 2-decimal attribute, and rounded down to 0 decimals it is 1 where 2 is due ({@code ref}: 2.00 and 2). The
 * backend's {@code VoorafgerondeFactorWitnessTest} pins that answer, and the like one for 1/24, in both branches.
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
 * shape {@code prioritizedDefaults} already uses. The additive {@code bronAnnotaties} provenance field is
 * carried through.
 */
public final class IrLoader {

    /** A wire form the v0 Drools backend does not carry. Carries the offending {@code form} token so the
     *  differential gate can list the golden with a precise, reason-carrying exclusion. */
    public static final class Refusal extends RuntimeException {
        private final String form;
        public Refusal(String form, String detail) {
            super("form not carried by the v0 Drools backend: " + form + (detail == null ? "" : " (" + detail + ")"));
            this.form = form;
        }
        public String form() { return form; }
    }

    /** A document that is not a well-formed {@code uchar-ir-v1} document, named by the position it sits at. A hard
     *  error and never a {@link Refusal}: a Refusal files the document under the differential gate's auto-excluded
     *  channel as a form this backend does not carry, which is a green gate over a document nobody read (the reason
     *  {@link #datumVanIrWire} gives for its own fence). {@code plek} is a JSON pointer into the document for the
     *  container and the M1 nodes, the root being {@code ""}. An absent field is named at the object that lacks it,
     *  with the field in the detail, as the OCaml loader names it; every other defect at the value itself, an
     *  unknown field included, which the OCaml loader names at its parent. */
    public static final class Documentfout extends IllegalArgumentException {
        private final String plek;
        public Documentfout(String plek, String detail) {
            super("malformed uchar-ir-v1 document at " + (plek.isEmpty() ? "<root>" : "'" + plek + "'") + ": "
                    + detail);
            this.plek = plek;
        }
        public String plek() { return plek; }
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
    private final Map<Objecttype, String> supertypeNaam = new java.util.IdentityHashMap<>();
    /** The JSON position of each {@link #supertypeNaam} entry, so a reference that does not resolve is reported
     *  where it was written. */
    private final Map<Objecttype, String> supertypePad = new java.util.IdentityHashMap<>();
    /** §6.1 (Phase 4) the prioritized-defaults extension list, parsed into the shared ir records — a top-level
     *  core-layer section the {@link RegelSpraakModel} does not itself carry (the model is the v0 resolved shape),
     *  so it rides beside the model via {@link Loaded}. An empty list is a legitimate flat program (accepted). */
    private final List<PrioritizedDefaults> prioritizedDefaults = new ArrayList<>();
    /** The wire's symbolic role bindings — see {@link Loaded#rolBindingen()}. An IDENTITY map: every IR node here
     *  is a RECORD with VALUE equality, so two structurally identical nodes in one document would collapse into
     *  one entry and the second would silently inherit the first's binding. */
    private final Map<Object, RolBindingRef> rolBindingen = new java.util.IdentityHashMap<>();
    /** The JSON pointer of every object and array in the document, by IDENTITY (a Jackson container is a distinct
     *  instance per position), so a node read without a path can still be refused at its place. */
    private final Map<JsonNode, String> paden = new java.util.IdentityHashMap<>();
    /** ch. 4.10: every {@link Expr.Let} this document carries, in parse order — see {@link Loaded#lets()}. */
    private final List<Expr.Let> lets = new ArrayList<>();
    /** ch. 4.3 / WF-102: the fold-source RANGE as a path, per {@link Expr.Functie} node — see
     *  {@link Loaded#bronPaden()}. Identity-keyed for {@link #rolBindingen}'s reason. */
    private final Map<Object, List<RolBindingRef>> bronPaden = new java.util.IdentityHashMap<>();
    /** IR-SPEC ch. 4.1a: the navigation bindings per AttribuutRef / Kenmerk node — see
     *  {@link Loaded#navigatieBindingen()}. Identity-keyed for {@link #rolBindingen}'s reason. */
    private final Map<Object, List<RolBindingRef>> navigatieBindingen = new java.util.IdentityHashMap<>();
    /** The JSON pointer of every AttribuutRef, fold and role check read, by identity: the node kinds
     *  {@link #keurOnderwerpen} refuses after the whole document is read, at the place they were written. */
    private final Map<Object, String> plekken = new java.util.IdentityHashMap<>();

    private IrLoader() {}

    /** A loaded document: the v0 resolved {@link RegelSpraakModel}, the ch. 7 prioritized-defaults extension, and
     *  the symbolic role bindings the wire carries beside the v0 bean fields. All three ride BESIDE the model —
     *  {@code RegelSpraakModel} is the faithful v0 shape, and the frontend records that would host these
     *  extensions belong to a VENDORED slice the toolchain consumes at a pin, so they cannot grow a field here.
     *  A consumer that needs them reads them from here (see {@link #loadAll}).
     *
     *  @param rolBindingen the symbolic (feittype, rol) binding per loaded IR NODE, by IDENTITY. Four node kinds
     *         carry one:
     *         <ul>
     *           <li>a {@link ResolvedObjectCreatie} — IR-SPEC ch. 3.3 {@code connectingRole}, naming the role the
     *               CREATED object plays. It is the only route to the back-link of a SELF-TYPED pair, whose
     *               by-pair derivation is ambiguous by construction (two same-typed sibling roles); ch. 5.5 makes
     *               it authoritative, and {@code ref/lib/eval.ml}'s {@code run_object_creatie} lets it WIN over
     *               the type-pair derivation.</li>
     *           <li>an {@link Expr.Functie} aggregation — {@code sourceRole}, naming the role the aggregated ELEMENT
     *               plays. Same self-typed gap: "de som van de rang van alle opvolgers van de Wachter" joins two
     *               Wachters, which no by-pair search can disambiguate.</li>
     *           <li>a {@link ResolvedVerdeling} — IR-SPEC ch. 3.2 {@code connectingRole}, naming the role the
     *               RECIPIENT plays. It decides the recipient join in both transpiler branches
     *               (V1-WIREMIGRATIE P3), as {@code run_verdeling} lets it decide in ref; since L2 the wire
     *               requires it.</li>
     *           <li>a {@link Conditie.Rollidmaatschap} — the IR-SPEC ch. 4.5 {@code RoleMembership}, whose binding is its
     *               {@code factType} and the {@code role} the SUBJECT plays. The record is the carrier: its
     *               {@code rol} is that role and its other-end slot holds the {@code counterpartType} narrowing or
     *               null (see {@link #tegenpartij}). Where the reference sits — on the subject or on the far end —
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
     *         tree it came from ({@code EffectieveEenheid}'s binder-read table, which answers the unit of a
     *         {@link Expr.BinderRef} asked in isolation). Empty for a document that carries no Let.
     *  @param bronPaden the ch. 4.3 / WF-102 fold-source RANGE per {@link Expr.Functie} node, by IDENTITY: the
     *         ordered RolBinding steps, subject-outward, VERBATIM off the wire. It rides here for the same reason
     *         {@code sourceRole} does — {@code Expr.Functie} is the vendored slice — and for a sharper one: unlike
     *         every other extension this loader carries, a path is not re-derivable. The type PAIR answers for the
     *         endpoints only, and a path's intermediate types are its own claim; a consumer that dropped this list
     *         would silently fold whatever direct relation happens to connect subject and element. Empty for a
     *         document whose folds all carry the one-step range.
     *  @param navigatieBindingen the IR-SPEC ch. 4.1a navigation walk per navigated {@link Expr.AttribuutRef}
     *         (its {@code path}, one binding per hop, subject-outward) and per navigated
     *         {@link Conditie.Kenmerk} (its {@code navigation}, a one-hop list), by IDENTITY, verbatim off the
     *         wire. Where a node carries one it decides the walk in both transpiler branches; a node without one
     *         is the subject's own read (or the element's, inside a fold's filter). */
    public record Loaded(RegelSpraakModel model, List<PrioritizedDefaults> prioritizedDefaults,
                         Map<Object, RolBindingRef> rolBindingen, List<Expr.Let> lets,
                         Map<Object, List<RolBindingRef>> bronPaden,
                         Map<Object, List<RolBindingRef>> navigatieBindingen) {}

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
        return new Loaded(model, l.prioritizedDefaults, l.rolBindingen, l.lets, l.bronPaden, l.navigatieBindingen);
    }

    /** The IR-SPEC ch. 4.5 other-end narrowing of a loaded {@code RoleMembership}: the type the filler of the other
     *  role must belong to, or null when the check does not narrow it (the role's declared filler). The loader
     *  keeps it in the record slot the near-side check always used for the other end (the vendored record keeps its
     *  v0 components), and this is the one place that knows so. */
    public static Objecttype tegenpartij(Conditie.Rollidmaatschap r) {
        return r.anderTeFiller();
    }

    private RegelSpraakModel model(JsonNode root) {
        if (root == null || !root.isObject()) throw new Documentfout("", "expected an object, got " + kort(root));
        wijsPadenToe(root, "");
        // another format is a form this backend does not carry, whatever else that document holds; an absent or
        // non-string format is a defect of this one
        String format = tekst(root, "", "format");
        if (!"uchar-ir-v1".equals(format)) throw new Refusal("format", format);
        velden(root, "", null, ROOT, List.of());
        JsonNode model = sectie(root, "", "model");
        JsonNode rules = sectie(root, "", "rules");
        velden(model, "/model", null, MODEL, List.of("functionDefs"));
        // an extension the schema admits and this backend does not carry: a form, so a Refusal, not a defect —
        // once the section has the form the schema gives it. Its entries are not read, so not checked either.
        if (model.has("functionDefs")) {
            JsonNode fd = model.get("functionDefs");
            if (!fd.isArray()) throw new Documentfout("/model/functionDefs", "expected an array, got " + kort(fd));
            throw new Refusal("functionDefs", "unknown model section");
        }
        velden(rules, "/rules", null, RULES, List.of("prioritizedDefaults"));

        // declarations first: the resolved lists reference them by name. Read in the v0 table order, so the
        // model fills exactly as it did from the v0 maps. The v0 `regels` stubs left the wire at M1 (delta B3);
        // no consumer here read them.
        lijst(model, "/model", "objectTypes", (p, n) -> {
            Objecttype ot = objecttype(p, n);
            eenmaal(m.objecttypen, p + "/name", ot.naam, ot);
        });
        bindSupertypes();
        lijst(model, "/model", "domains", (p, n) -> {
            velden(n, p, "Domain", List.of("node", "name", "datatype"), List.of());
            String naam = naam(n, p, "name");
            eenmaal(m.domeinen, p + "/name", naam, domain(p, naam, n));
        });
        lijst(model, "/model", "parameters", (p, n) -> {
            Parameter par = parameter(p, n);
            eenmaal(m.parameters, p + "/name", par.naam(), par);
        });
        lijst(model, "/model", "unitSystems", (p, n) -> {
            Eenheidstelsel es = unitSystem(p, n);
            eenmaal(m.eenheidstelsels, p + "/name", es.naam, es);
        });
        lijst(model, "/model", "factTypes", (p, n) -> {
            Feittype ft = feittype(p, n);
            eenmaal(m.feittypen, p + "/name", ft.naam, ft);
        });
        lijst(model, "/model", "dimensions", (p, n) -> {
            Dimensie d = dimensie(p, n);
            eenmaal(m.dimensies, p + "/name", d.naam(), d);
        });
        lijst(model, "/model", "timelines", (p, n) -> m.tijdlijnen.add(tijdlijn(n)));
        lijst(model, "/model", "dayKinds", this::dayKind);

        // IR-SPEC ch. 3, Versioning: each list holds named forms, and every version is one record, named as the
        // records have always named a version of a form with several ("<name> #<i>")
        lijst(rules, "/rules", "rules", this::rule);
        lijst(rules, "/rules", "dayKindDefinitions", (p, n) -> {
            velden(n, p, "DayKindDefinition", List.of("node", "dayKind", "condition", "validity"), List.of());
            m.resolvedDagsoorten.add(new ResolvedDagsoort(text(n, "dayKind"), conditie(knoopVeld(n, "condition")),
                    geldigheid(n, "dayKind")));
        });
        lijst(rules, "/rules", "objectCreations", this::objectCreation);
        lijst(rules, "/rules", "allocations", this::allocation);
        lijst(rules, "/rules", "consistencies", this::consistency);
        lijst(rules, "/rules", "uniquenesses", this::uniqueness);
        lijst(rules, "/rules", "factCreations", this::factCreation);
        lijst(rules, "/rules", "startPointDeterminations", (p, n) -> {
            velden(n, p, "StartPointDetermination", List.of("node", "timeline", "objectType", "member"), List.of());
            m.resolvedStartpunten.add(new ResolvedStartpunt(text(n, "timeline"), text(n, "objectType"),
                    text(n, "member")));
        });
        weigerGeerfdAnker();   // the §9.9 family: asked here, where BOTH the tijdlijnen and the determinations are read
        lijst(rules, "/rules", "ruleGroups", (p, n) -> {
            velden(n, p, "RuleGroup", List.of("node", "name", "recursive", "members"), List.of("source"));
            m.regelGroepen.add(new ResolvedRegelGroep(
                    text(n, "name"), bool(n, "recursive"), stringList(n.get("members")), bron(n), bronregel(n)));
        });
        lijst(root, "", "diagnostics", (p, n) -> m.diagnostics.add(diagnostic(p, n)));
        // §6.1 (Phase 4) the prioritized-defaults extension, parsed AFTER the declarations so a default's
        // condition/value resolves its objecttype/parameter references against the fully-built model. A malformed
        // defaults bundle (an unknown inner node, a missing target) raises a loud Refusal from these parsers.
        if (rules.has("prioritizedDefaults"))
            lijst(rules, "/rules", "prioritizedDefaults", (p, n) -> prioritizedDefaults.add(prioritizedDefaults(n)));
        keurOnderwerpen();
        return m;
    }

    // --- the reference layer's questions that need the subject or the position -----------------------

    /** The checks that need more than the node itself — the SUBJECT a node is read on, or the POSITION it stands
     *  at — made once the whole document is read, and refused as the verifier refuses them, in the words
     *  {@code ref} and the ASP backend fault with where they have them: a role check whose subject does not fill
     *  the role (WF-034, IR-SPEC ch. 4.5 — a link the subject can never be in), a characteristic read at a value
     *  position (WF-080 — a characteristic carries truth, no value), a read without bindings that names another
     *  type than its subject (WF-053, IR-SPEC ch. 4.1a), a value fold's set source that names no
     *  member of the element type ({@link #keurWaardevouwbron}), and a {@code total} without a range over another
     *  type than the subject ({@link #keurEigenTotaal}). The subject of each position is the one the verifier walks it
     *  with: the rule target's type, the element type inside a fold's filter and a quantifier fold, a selection's
     *  type inside a union branch, the creation and consistency subject, the allocation's source type, the fact
     *  creation's anchor, the defaults' subject, and none in a day-kind definition. A rule target is a write,
     *  never read here; the rest of a fold's set source is {@link #keurVouwbron}'s. Until L2's fix round the role
     *  check was refused with a storage reason, the characteristic read played, the value fold over a role name
     *  failed at first fire, and the own total over a foreign element type played (measured on the L2 union). */
    private void keurOnderwerpen() {
        for (ResolvedRegel r : m.resolvedRegels) {
            Objecttype s = r.doel() instanceof Expr.AttribuutRef a ? a.objecttype() : null;
            keurWaarde(r.waarde(), s);
            keurConditie(r.voorwaarde(), s);
        }
        for (ResolvedDagsoort d : m.resolvedDagsoorten) keurConditie(d.voorwaarde(), null);
        for (ResolvedObjectCreatie oc : m.objectCreaties) {
            for (ResolvedObjectCreatie.Toekenning t : oc.toekenningen()) keurWaarde(t.waarde(), oc.subject());
            keurConditie(oc.voorwaarde(), oc.subject());
        }
        for (ResolvedVerdeling v : m.verdelingen) keurConditie(v.voorwaarde(), v.bronType());
        for (ResolvedConsistentie c : m.consistenties) keurConditie(c.assertie(), c.subject());
        for (ResolvedFeitCreatie fc : m.feitCreaties) keurConditie(fc.voorwaarde(), fc.anker());
        for (PrioritizedDefaults pd : prioritizedDefaults)
            for (Default d : pd.defaults()) keurStandaard(d, ot(pd.onderwerp()));
    }

    private void keurStandaard(Default d, Objecttype s) {
        keurConditie(d.voorwaarde(), s);
        keurWaarde(d.consequentie(), s);
        if (d.excepties() != null)
            for (ExceptionGroup g : d.excepties().leden())
                for (Default x : g.defaults()) keurStandaard(x, s);
    }

    /** A value position and everything below it. Exhaustive over the sealed union, so a node the union grows is a
     *  compile error here rather than a subtree the walk skips. */
    private void keurWaarde(Expr e, Objecttype s) {
        switch (e) {
            case null -> { }
            case Expr.AttribuutRef a -> {
                Objecttype ot = a.objecttype();
                if (ot != null && a.attribuut() != null && ot.attribuut(a.attribuut()) == null
                        && ot.kenmerkNaam(a.attribuut()) != null)
                    throw new Documentfout(plekken.get(a), "'" + a.attribuut() + "' is a characteristic of '" + ot.naam
                            + "', not an attribute — a characteristic endpoint is ill-formed in a value position"
                            + " (WF-080)");
                // WF-053, IR-SPEC ch. 4.1a: a read without bindings is the subject's own, so it names the subject
                // type itself — a supertype is another type, even where it declares the member. Both branches
                // refused such a read before, with a generic reason; refused here in the sentence ref and the
                // verifier share. A subject-less position (a day-kind definition) anchors nothing.
                if (s != null && ot != null && !navigatieBindingen.containsKey(a)
                        && !Namen.norm(ot.naam).equals(Namen.norm(s.naam)))
                    throw new Documentfout(plekken.get(a), "an attribute reference without a path is the subject's"
                            + " own read, so its subjectType is the subject type '" + s.naam + "' — '"
                            + ot.naam + "' names another type, which only a navigation reaches (WF-053)");
            }
            case Expr.Functie f -> {
                boolean setbron = f.elementObjecttype() != null && f.argumenten().size() == 1
                        && (rolBindingen.containsKey(f) || bronPaden.containsKey(f));
                if (setbron) keurWaardevouwbron(f, s);
                else keurEigenTotaal(f, s);
                if (!setbron) for (Expr x : f.argumenten()) keurWaarde(x, s);
                keurWaarde(f.leegwaarde(), s);
                keurConditie(f.filter(), f.elementObjecttype() != null ? f.elementObjecttype() : s);
            }
            case Expr.Binair b -> { keurWaarde(b.links(), s); keurWaarde(b.rechts(), s); }
            case Expr.Afgerond x -> keurWaarde(x.basis(), s);
            case Expr.Begrensd x -> {
                keurWaarde(x.basis(), s); keurWaarde(x.minimum(), s); keurWaarde(x.maximum(), s);
            }
            case Expr.TekstReeks t -> {
                for (Expr.TekstReeks.Deel d : t.delen())
                    if (d instanceof Expr.TekstReeks.Ingevoegd i) keurWaarde(i.expr(), s);
            }
            case Expr.Let l -> { keurWaarde(l.value(), s); keurWaarde(l.body(), s); }
            case Expr.IfThenElse k -> {
                keurConditie(k.conditie(), s); keurWaarde(k.dan(), s); keurWaarde(k.anders(), s);
            }
            case Expr.InstanceSetUnion u -> {
                for (Expr.InstanceSetUnion.Tak t : u.takken()) keurConditie(t.filter(), t.elementType());
            }
            // a dimension cell reads a dimensioned attribute, which no characteristic is
            case Expr.DimensieCel c -> { }
            case Expr.Literal x -> { }
            case Expr.ParameterRef x -> { }
            case Expr.VariabeleRef x -> { }
            case Expr.BinderRef x -> { }
            case Expr.Constante x -> { }
            case Expr.Onopgelost x -> { }
        }
    }

    private void keurConditie(Conditie c, Objecttype s) {
        switch (c) {
            case null -> { }
            case Conditie.Vergelijking v -> { keurWaarde(v.links(), s); keurWaarde(v.rechts(), s); }
            case Conditie.Predicaat p -> keurWaarde(p.operand(), s);
            case Conditie.Periode p -> { keurWaarde(p.vanaf(), s); keurWaarde(p.tot(), s); }
            case Conditie.Samengesteld x -> { for (Conditie o : x.onderdelen()) keurConditie(o, s); }
            case Conditie.Kwantorvouw kv -> {
                Objecttype es = kv.elementObjecttype() != null ? kv.elementObjecttype() : s;
                keurConditie(kv.filter(), es);
                keurConditie(kv.toets(), es);
            }
            case Conditie.Rollidmaatschap r -> keurVulling(r, s);
            case Conditie.Kenmerk k -> { }
            case Conditie.Regelversie x -> { }
            case Conditie.Onopgelost x -> { }
            // this loader refuses the node itself, so no document it returns carries one
            case Conditie.VerreRollidmaatschap x -> { }
        }
    }

    /** WF-034: the subject fills the role the check names — one-sided, through the supertype chain (ch. 2.6), as
     *  every near-side question is asked. A subject-less position (a day-kind definition) has nothing to cross. */
    private void keurVulling(Conditie.Rollidmaatschap r, Objecttype s) {
        if (s == null) return;
        RolBindingRef rb = rolBindingen.get(r);
        RegelSpraakModel.Rol rol = rolOpNaam(feittypeOpNaam(rb.feittype()), rb.rol());
        Objecttype vuller = ot(rol.objecttypeNaam());
        if (vuller != null && !s.isEen(vuller))
            throw new Documentfout(plekken.get(r) + "/role", "role check: the subject \"" + s.naam
                    + "\" does not fill the role \"" + rb.rol() + "\" of \"" + rb.feittype()
                    + "\", so no link of it can hold the subject there (WF-034)");
    }

    /** The declared fact type of that name in the WF-001 form (case and runs of blanks folded), or null. */
    private Feittype feittypeOpNaam(String naam) {
        if (naam == null) return null;
        String n = Namen.norm(naam);
        for (Feittype ft : m.feittypen.values()) if (n.equals(Namen.norm(ft.naam))) return ft;
        return null;
    }

    /** The role of {@code ft} with that DECLARED name (the plural does not name a role here), in the WF-001 form,
     *  or null. */
    private static RegelSpraakModel.Rol rolOpNaam(Feittype ft, String naam) {
        if (ft == null || naam == null) return null;
        String n = Namen.norm(naam);
        for (RegelSpraakModel.Rol r : ft.rollen) if (r.naam() != null && n.equals(Namen.norm(r.naam()))) return r;
        return null;
    }

    /** The other role of a two-role fact type, or null when it has not exactly two. */
    private static RegelSpraakModel.Rol andereRol(Feittype ft, RegelSpraakModel.Rol r) {
        if (ft.rollen.size() != 2) return null;
        return ft.rollen.get(0) == r ? ft.rollen.get(1) : ft.rollen.get(0);
    }

    /** The key set of a container object or an M1 node, checked as the OCaml loader's {@code check_keys} checks it:
     *  the node discriminator when {@code knoop} names one, no key outside the node's own, every required key
     *  present, and an optional key written whole or not at all — present and null is neither (IR-SPEC § Method).
     *  Read leniently, each of these was a document part ignored or a null taken for an absent field. */
    private static void velden(JsonNode n, String pad, String knoop, List<String> verplicht, List<String> optioneel) {
        if (n == null || !n.isObject())
            throw new Documentfout(pad, "expected a JSON object"
                    + (knoop == null ? "" : " (" + knoop + ")") + ", got " + kort(n));
        if (knoop != null) {
            JsonNode soort = n.get("node");
            if (soort == null)
                throw new Documentfout(pad, "missing field 'node' (the discriminator \"" + knoop + "\")");
            if (!soort.isTextual() || !knoop.equals(soort.asText()))
                throw new Documentfout(pad + "/node", "expected \"" + knoop + "\", got " + kort(soort));
        }
        n.fieldNames().forEachRemaining(k -> {
            if (!verplicht.contains(k) && !optioneel.contains(k))
                throw new Documentfout(pad + "/" + k, "unknown field" + (knoop == null ? "" : " on " + knoop));
        });
        for (String k : verplicht)
            if (!n.has(k)) throw ontbreekt(pad, k);
        for (String k : optioneel)
            if (n.has(k) && n.get(k).isNull())
                throw new Documentfout(pad + "/" + k, "an optional field is written whole or not at all, and null is neither");
    }

    /** Record the JSON pointer of every container below {@code n} (RFC 6901: '~' and '/' in a key escaped). */
    private void wijsPadenToe(JsonNode n, String pad) {
        if (n == null) return;
        if (n.isObject()) {
            paden.put(n, pad);
            for (Map.Entry<String, JsonNode> e : n.properties())
                wijsPadenToe(e.getValue(), pad + "/" + e.getKey().replace("~", "~0").replace("/", "~1"));
        } else if (n.isArray()) {
            paden.put(n, pad);
            for (int i = 0; i < n.size(); i++) wijsPadenToe(n.get(i), pad + "/" + i);
        }
    }

    /** The pointer of a container this document holds. */
    private String padVan(JsonNode n) {
        String p = paden.get(n);
        if (p == null) throw new IllegalStateException("IrLoader: a node outside the document it is reading: " + kort(n));
        return p;
    }

    /** An absent field, named at the object {@code pad} that lacks it, as the OCaml loader's {@code field} names it:
     *  a pointer to the field itself would point at nothing in the document. */
    private static Documentfout ontbreekt(String pad, String veld) {
        return new Documentfout(pad, "missing field '" + veld + "'");
    }

    private static JsonNode sectie(JsonNode ouder, String pad, String naam) {
        JsonNode n = ouder.get(naam);
        if (n == null) throw ontbreekt(pad, naam);
        if (!n.isObject()) throw new Documentfout(pad + "/" + naam, "expected an object, got " + kort(n));
        return n;
    }

    /** A list the schema requires: an array, each entry read with its own position. The lenient reading took a
     *  non-array for an empty list, so {@code objectTypes: {}} loaded as a document without object types. */
    private static void lijst(JsonNode ouder, String pad, String veld, java.util.function.BiConsumer<String, JsonNode> f) {
        JsonNode a = ouder.get(veld);
        String p = pad + "/" + veld;
        if (a == null) throw ontbreekt(pad, veld);
        if (!a.isArray()) throw new Documentfout(p, "expected an array, got " + kort(a));
        for (int i = 0; i < a.size(); i++) f.accept(p + "/" + i, a.get(i));
    }

    /** The list form writes each name once, on its node, so a nameless entry has no name at all (the v0 map key
     *  that once stood in for it is gone), and a repeated name would silently replace the declaration before it. */
    private static String naam(JsonNode n, String pad, String veld) {
        JsonNode v = n.get(veld);
        if (v == null) throw new Documentfout(pad, "a declaration without a name: missing field '" + veld + "'");
        if (!v.isTextual())
            throw new Documentfout(pad + "/" + veld, "a declaration without a name: expected a string, got " + kort(v));
        return v.asText();
    }

    private static String tekst(JsonNode n, String pad, String veld) {
        JsonNode v = n.get(veld);
        if (v == null) throw ontbreekt(pad, veld);
        if (!v.isTextual()) throw new Documentfout(pad + "/" + veld, "expected a string, got " + kort(v));
        return v.asText();
    }

    /** An optional string field: absent is null; present, it is a string ({@link #velden} has refused a null). */
    private static String optTekst(JsonNode n, String pad, String veld) {
        return n.has(veld) ? tekst(n, pad, veld) : null;
    }

    private static boolean waarheid(JsonNode n, String pad, String veld) {
        JsonNode v = n.get(veld);
        if (v == null) throw ontbreekt(pad, veld);
        if (!v.isBoolean()) throw new Documentfout(pad + "/" + veld, "expected a boolean, got " + kort(v));
        return v.booleanValue();
    }

    private static List<String> tekstLijst(JsonNode n, String pad, String veld) {
        JsonNode a = n.get(veld);
        if (a == null) throw ontbreekt(pad, veld);
        if (!a.isArray()) throw new Documentfout(pad + "/" + veld, "expected an array, got " + kort(a));
        List<String> uit = new ArrayList<>();
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).isTextual())
                throw new Documentfout(pad + "/" + veld + "/" + i, "expected a string, got " + kort(a.get(i)));
            uit.add(a.get(i).asText());
        }
        return uit;
    }

    /** An integer field, as the OCaml loader's {@code int_} reads it: a JSON integer and nothing else, since
     *  {@code "2"} and {@code 2.0} both read as 2 under {@code asInt}, a value the document did not write as an
     *  integer. An integer beyond a Java {@code int} is well-formed (the OCaml loader reads it into its 63-bit
     *  int) and this backend cannot hold it: a form not carried, so a {@link Refusal} under {@code vorm}. */
    private static int geheel(JsonNode n, String pad, String veld, String vorm) {
        JsonNode c = n.get(veld);
        String p = pad + "/" + veld;
        if (c == null) throw ontbreekt(pad, veld);
        if (!c.isIntegralNumber()) throw new Documentfout(p, "expected an integer, got " + kort(c));
        if (!c.canConvertToInt())
            throw new Refusal(vorm, c + " at '" + p + "' is outside this backend's integer range ["
                    + Integer.MIN_VALUE + ", " + Integer.MAX_VALUE + "]");
        return c.intValue();
    }

    /** WF-001: a namespace holds a name once, under the verifier's name identity ({@link Namen#norm}, the same
     *  function both transpiler branches compare declared names with), so this loader refuses the collisions
     *  WF-001/WF-002 refuse and no others. The model's tables are keyed on the exact string and look a name up
     *  case-insensitively after an exact miss, so two spellings of one name would both enter and a lookup would
     *  pick one of them. */
    private static <V> void eenmaal(Map<String, V> tabel, String pad, String naam, V waarde) {
        String eerder = botsing(tabel.keySet(), naam);
        if (eerder != null) throw new Documentfout(pad, dubbel(naam, eerder, "WF-001"));
        tabel.put(naam, waarde);
    }

    /** The declared name {@code naam} collides with under {@link Namen#norm}, or null. */
    private static String botsing(java.util.Collection<String> namen, String naam) {
        String sleutel = Namen.norm(naam);
        for (String n : namen) if (Namen.norm(n).equals(sleutel)) return n;
        return null;
    }

    private static String dubbel(String naam, String eerder, String regel) {
        return naam.equals(eerder)
                ? "the name '" + naam + "' is declared twice (" + regel + ")"
                : "the name '" + naam + "' collides with the earlier '" + eerder + "' (" + regel
                        + " compares names case-insensitively)";
    }

    /** A JSON value as a message quotes it, cut short: the value is evidence, not the document. */
    private static String kort(JsonNode n) {
        String s = String.valueOf(n);
        return s.length() > 60 ? s.substring(0, 60) + "…" : s;
    }

    // --- prioritized-defaults extension (ch. 7) ----------------------------

    private PrioritizedDefaults prioritizedDefaults(JsonNode n) {
        velden(n, padVan(n), "PrioritizedDefaults", List.of("node", "name", "subject", "defaults", "validity"),
                List.of("characteristic", "attribute", "unordered", "source"));
        String onderwerp = text(n, "subject");
        String kenmerk = text(n, "characteristic");
        String attribuut = text(n, "attribute");
        if (onderwerp == null) throw new Refusal("PrioritizedDefaults", "defaults without a subject");
        if ((kenmerk == null) == (attribuut == null))
            throw new Refusal("PrioritizedDefaults",
                    "defaults must carry exactly one of characteristic/attribute (WF-050)");
        List<Default> defaults = new ArrayList<>();
        elementen(n, "defaults", r -> defaults.add(dflt(r)));
        return new PrioritizedDefaults(text(n, "name"), onderwerp, kenmerk, attribuut, bool(n, "unordered"),
                defaults, bronTekst(n), geldigheid(n, "name"));
    }

    private Default dflt(JsonNode n) {
        velden(n, padVan(n), "Default", List.of("node"),
                List.of("decision", "justification", "consequence", "initialisation", "exceptions", "source"));
        // an absent decision or justification is what the v0 null was; a null there velden has refused
        JsonNode b = n.get("decision");
        Boolean besluit = b == null ? null : b.asBoolean();
        return new Default(besluit, conditie(knoopVeld(n, "justification")), expr(knoopVeld(n, "consequence")),
                bool(n, "initialisation"), excepties(knoopVeld(n, "exceptions")), bronTekst(n));
    }

    private Default.Excepties excepties(JsonNode n) {
        if (n == null || n.isNull()) return null;
        velden(n, padVan(n), null, List.of("unordered", "members"), List.of());
        List<ExceptionGroup> leden = new ArrayList<>();
        elementen(n, "members", s -> leden.add(exceptionGroup(s)));
        return new Default.Excepties(bool(n, "unordered"), leden);
    }

    private ExceptionGroup exceptionGroup(JsonNode n) {
        velden(n, padVan(n), "ExceptionGroup", List.of("node", "defaults"), List.of("source"));
        List<Default> defaults = new ArrayList<>();
        elementen(n, "defaults", r -> defaults.add(dflt(r)));
        return new ExceptionGroup(defaults, bronTekst(n));
    }

    /** Bind every declared supertype reference, and refuse the two shapes this backend does not carry.
     *  WF-103/104 already reject an unknown or cyclic parent at the document, so both guards here
     *  are a last line: a loader that walked a cycle would hang instead of reporting.
     *
     *  <p>Until 2026-09-12 a third guard refused every chain carrying a temporal OR dimensioned member,
     *  because the facet SIDE MAPS were read per declared type at 122 reads (121 lines) in both transpiler branches
     *  and an inherited timeline would answer "not temporal" for the subtype. Those reads go through the
     *  chain-closed accessors of {@link Objecttype} now, so the broad refusal is gone and the whole
     *  combination plays. Measured at the same card: the DIMENSIONED half was over-broad even then —
     *  every dimension read already went through {@code dimensiesVan}, which has walked the chain since
     *  the kernel. What is left of it is the §9.9 startpunt-ANCHOR family, refused in
     *  {@link #weigerGeerfdAnker} — not here, because it needs the tijdlijn list, which is read later. */
    private void bindSupertypes() {
        supertypeNaam.forEach((ot, naam) -> {
            if (naam == null) return;
            // under WF-103's name identity, the one WF-001 declared the names under: the model's own lookup
            // folds case only, and would call "Jutter " undeclared where the verifier, ref and clingo bind it
            String gedeclareerd = botsing(m.objecttypen.keySet(), naam);
            Objecttype ouder = gedeclareerd == null ? null : m.objecttypen.get(gedeclareerd);
            if (ouder == null)
                throw new Documentfout(supertypePad.get(ot), "supertype '" + naam + "' is not declared (WF-103)");
            ot.supertype = ouder;
        });
        for (Objecttype ot : m.objecttypen.values()) {
            java.util.Set<Objecttype> gezien = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            for (Objecttype t = ot; t != null; t = t.supertype)
                if (!gezien.add(t))
                    throw new Documentfout(supertypePad.get(ot), "supertype cycle at '" + ot.naam + "' (WF-104)");
        }
    }

    /** The §9.9 startpunt-ANCHOR family — what is LEFT of the withdrawn temporal/dimensioned refusal,
     *  and the one part of it that really is chain-blind. The family is not a member lookup but a
     *  PROTOCOL keyed on the DECLARED SUBJECT NAME: a startpuntbepaling declares a synthetic attribute
     *  on a named objecttype, and {@code heeftVariabelStartpunt}, {@code startpuntLid} and
     *  {@code isStartpuntLid} all key on that name (see the note at
     *  {@code RegelSpraakModel.heeftVariabelStartpunt}: resolving one of the four through the chain only
     *  moves the seam). Two sites, and they fall independently:
     *
     *  <p>(a) an ancestor declares an attribute riding a named tijdlijn WITH A VARIABEL STARTPUNT.
     *  {@code heeftVariabelStartpunt} then answers false for the subtype where the truth is true, so the
     *  bucketing is skipped. The guard asks {@code heeftVariabelStartpunt} itself rather than testing
     *  {@code attribuutTijdlijnNaam} for emptiness, because on a FIXED-startpunt tijdlijn that predicate
     *  answers false from the own map and from the chain alike — there is no blindness there, and
     *  refusing it would print a reason ("reads it as unanchored") that is simply true of the document.
     *  That narrowness is what makes this guard need the tijdlijn list, hence its place here.
     *
     *  <p>(b) a startpuntbepaling declared on an ANCESTOR. {@code isStartpuntLid} is asked of the type
     *  the REFERENCE names and compares the determination's declared subject name, so for every subtype
     *  instance it answers no and the §6.8 leeg-fout companion silently disappears for exactly those.
     *
     *  <p>WF-016 ties the two together on conforming input without merging them: a variable-start
     *  tijdlijn must have SOME determination, but nothing makes that determination name the type that
     *  declares the attribute — so (a) fires alone when it names another type, and (b) fires alone when
     *  no attribute rides the tijdlijn at all. */
    private void weigerGeerfdAnker() {
        for (Objecttype ot : m.objecttypen.values()) {
            if (ot.supertype == null) continue;
            for (Objecttype t = ot.supertype; t != null; t = t.supertype) {       // STRICTLY above ot
                final Objecttype voorouder = t;
                for (String attr : voorouder.attribuutTijdlijnNaam.keySet())
                    if (m.heeftVariabelStartpunt(voorouder, attr))
                        throw new Refusal("Objecttype.supertype",
                                "an INHERITED attribute ('" + attr + "') riding a NAMED tijdlijn with a VARIABEL"
                                + " STARTPUNT (§13.3.6.9/§9.9) on the chain of '" + ot.naam + "': the anchor family"
                                + " keys on the DECLARED subject name, so a subtype reads such an attribute as"
                                + " unanchored");
                if (m.resolvedStartpunten.stream().anyMatch(sp -> voorouder.naam.equals(sp.objecttype())))
                    throw new Refusal("Objecttype.supertype",
                            "a §9.9 startpuntbepaling declared on an ANCESTOR of '" + ot.naam
                            + "': the anchor family keys on the declared subject name, so the §6.8 leeg-fout "
                            + "companion is not emitted for the subtype");
            }
        }
    }

    /** The node's form as the schema gives it: the one wire severity, a message string, the line an integer. The
     *  severities WF-113 (a) names — error, info and none at all — are refused in the rule's sentences, as the
     *  shared loader refuses them for an executor; what WF-113 demands beyond that (a message that is not blank, a
     *  line of at least 0) is the verifier's to refuse, and no consumer reads a diagnostic for evaluation. */
    private static Diagnostic diagnostic(String pad, JsonNode n) {
        velden(n, pad, "Diagnostic", List.of("node", "severity", "message", "line"), List.of());
        String sp = pad + "/severity";
        if (n.get("severity").isNull()) throw new Documentfout(sp, SEVERITY_ONTBREEKT);
        String w = tekst(n, pad, "severity");
        if (Spelling.SEVERITY.weigertBijNaam("Diagnostic.severity", w))
            throw new Documentfout(sp, severityWeigering(geweigerd(Spelling.SEVERITY, w)));
        return new Diagnostic(enumWaarde(Spelling.SEVERITY, "Diagnostic.severity", w, sp),
                tekst(n, pad, "message"), geheel(n, pad, "line", "Diagnostic.line"));
    }

    /** WF-113 (a), the verifier's sentences for the two severities the schema does not admit: an executor refuses
     *  them at load, as the shared loader does. */
    static String severityWeigering(Severity s) {
        return switch (s) {
            case ERROR -> "severity error is not a wire severity: an error means the producer refused to write the"
                    + " document, so a document that was written carries none";
            case INFO -> "severity info is not a wire severity: the list is the account of what stayed unresolved,"
                    + " and info is tooling talk";
            case WARNING -> throw new IllegalStateException("IrLoader: warning is the one wire severity");
        };
    }

    /** WF-113 (a), the verifier's sentence for a diagnostic without a severity. */
    static final String SEVERITY_ONTBREEKT = "diagnostic without a severity: the one wire severity is warning";

    // --- declaration side --------------------------------------------------

    /** IR-SPEC ch. 2.2: the member nodes back into the model's member-keyed side-maps. Every facet's count is
     *  kept, 1 included, as the OCaml loader keeps it. The frontend's model records a count only when it is not 1,
     *  and the one reader of these tables, {@code Tijdlijnen.meerPeriodiek}, takes an absent entry and an explicit
     *  1 for the same single-period facet, so keeping the 1 moves no outcome; what it adds is that a regression to
     *  a presence test fails on every temporal golden instead of on none ({@code MeerPeriodiekePoortWitnessTest}).
     *  A characteristic's kind is required on the wire since M1; the emitter writes nominal for an untagged one
     *  (ch. 2.2: untagged = nominal). */
    private Objecttype objecttype(String pad, JsonNode n) {
        velden(n, pad, "ObjectType", List.of("node", "name", "animate", "attributes", "characteristics"),
                List.of("supertype", "plural"));
        Objecttype ot = new Objecttype(naam(n, pad, "name"));
        ot.bezield = waarheid(n, pad, "animate");
        ot.meervoud = optTekst(n, pad, "plural");
        supertypeNaam.put(ot, optTekst(n, pad, "supertype"));   // resolved after every declaration is read
        supertypePad.put(ot, pad + "/supertype");
        lijst(n, pad, "attributes", (ap, a) -> {
            velden(a, ap, "Attribute", List.of("node", "name", "datatype"), List.of("plural", "dimensions", "timeline"));
            String lid = naam(a, ap, "name");
            String eerder = botsing(ot.attributen.keySet(), lid);
            if (eerder != null) throw new Documentfout(ap + "/name", dubbel(lid, eerder, "WF-002"));
            // null is admitted here and refused by the verifier under WF-059, which names it
            JsonNode dt = a.get("datatype");
            if (!dt.isNull() && !dt.isObject())
                throw new Documentfout(ap + "/datatype", "expected a datatype object or null, got " + kort(dt));
            ot.attributen.put(lid, datatype(dt, ap + "/datatype", true));
            String meervoud = optTekst(a, ap, "plural");
            if (meervoud != null) ot.attribuutMeervoud.put(lid, meervoud);
            if (a.has("dimensions")) ot.attribuutDimensies.put(lid, tekstLijst(a, ap, "dimensions"));
            if (a.has("timeline")) {
                String tp = ap + "/timeline";
                JsonNode tl = a.get("timeline");
                velden(tl, tp, "TimelineFacet", List.of("node", "granularity", "count"), List.of("timelineName"));
                ot.attribuutGranulariteit.put(lid, enumWaarde(Spelling.GRANULARITY, "TimelineFacet.granularity",
                        tekst(tl, tp, "granularity"), tp + "/granularity"));
                ot.attribuutTijdlijnAantal.put(lid, geheel(tl, tp, "count", "TimelineFacet.count"));
                String tijdlijnNaam = optTekst(tl, tp, "timelineName");
                if (tijdlijnNaam != null) ot.attribuutTijdlijnNaam.put(lid, tijdlijnNaam);
            }
        });
        lijst(n, pad, "characteristics", (cp, c) -> {
            velden(c, cp, "Characteristic", List.of("node", "name", "kind"), List.of("timeline"));
            String lid = naam(c, cp, "name");
            // WF-002: read leniently, a second declaration's kind replaced the first one's and its timeline did not
            String eerder = botsing(ot.kenmerken, lid);
            if (eerder != null) throw new Documentfout(cp + "/name", dubbel(lid, eerder, "WF-002"));
            ot.kenmerken.add(lid);
            ot.kenmerkSoorten.put(lid, enumWaarde(Spelling.CHARACTERISTIC_KIND, "Characteristic.kind",
                    tekst(c, cp, "kind"), cp + "/kind"));
            if (c.has("timeline")) {
                String tp = cp + "/timeline";
                JsonNode tl = c.get("timeline");
                velden(tl, tp, "TimelineFacet", List.of("node", "granularity", "count"), List.of());
                ot.kenmerkGranulariteit.put(lid, enumWaarde(Spelling.GRANULARITY, "TimelineFacet.granularity",
                        tekst(tl, tp, "granularity"), tp + "/granularity"));
                ot.kenmerkTijdlijnAantal.put(lid, geheel(tl, tp, "count", "TimelineFacet.count"));
            }
        });
        return ot;
    }

    /** IR-SPEC ch. 2.3: the wrapper carries the name, and a datatype at this declaration position carries no
     *  self-name — the model's Enumeratie record takes the wrapper's. A self-name left beside it is a second
     *  spelling of one name (delta B2), refused as an unknown field, on the two datatypes this backend does not
     *  carry as well: there the document is malformed before its form is refused. */
    private Datatype domain(String pad, String naam, JsonNode n) {
        String dp = pad + "/datatype";
        JsonNode dt = n.get("datatype");
        if (!dt.isObject()) throw new Documentfout(dp, "expected a datatype object, got " + kort(dt));
        String soort = datatypeSoort(dt, dp);
        List<String> sleutels = DOMEINDATATYPE.get(soort);
        if (sleutels != null) velden(dt, dp, soort, sleutels, List.of());
        if (soort.equals("Enumeration")) {
            if (!dt.get("values").isArray())
                throw new Documentfout(dp + "/values", "expected an array, got " + kort(dt.get("values")));
            return new Datatype.Enumeratie(naam, stringList(dt.get("values")));
        }
        return datatype(dt, dp, true);
    }

    /** The key set of each datatype kind at the declaration position {@code Domain.datatype}, as the OCaml loader's
     *  {@code domain_datatype} and {@code datatype} check it: the three kinds that carry a domain name at a
     *  reference position carry none here, and every other kind has the keys it has everywhere ({@code Number} and
     *  {@code Date} are checked in the dispatch, with their optional keys). A kind outside the table reaches the
     *  dispatch. */
    private static final Map<String, List<String>> DOMEINDATATYPE = Map.ofEntries(
            Map.entry("Enumeration", List.of("node", "values")),
            Map.entry("SumType", List.of("node", "constructors")),
            Map.entry("StructType", List.of("node", "fields")),
            Map.entry("ListType", List.of("node", "element")),
            Map.entry("Text", List.of("node")),
            Map.entry("Boolean", List.of("node")));

    /** The key set of each datatype kind this backend carries without optional keys, at every position but a
     *  domain's declaration (see {@link #DOMEINDATATYPE}): an Enumeration names its domain here. */
    private static final Map<String, List<String>> DATATYPE = Map.ofEntries(
            Map.entry("Text", List.of("node")),
            Map.entry("Boolean", List.of("node")),
            Map.entry("Enumeration", List.of("node", "domain", "values")));

    /** IR-SPEC ch. 2.10: a parameter, its datatype, and when it is time-dependent its timeline facet. The datatype is
     *  required, and the facet carries no timeline name and a count of 1: a parameter's timeline is one period while
     *  multi-period grids are open (RUNTIME-SEMANTICS ch. 3.12), so another count is no document (WF-005), refused in
     *  the verifier's words. The model's parameter holds the granularity. */
    private Parameter parameter(String pad, JsonNode n) {
        velden(n, pad, "Parameter", List.of("node", "name", "datatype"), List.of("timeline"));
        String naam = naam(n, pad, "name");
        JsonNode dt = n.get("datatype");
        if (!dt.isObject()) throw new Documentfout(pad + "/datatype", "expected a datatype object, got " + kort(dt));
        Datatype datatype = datatype(dt, pad + "/datatype", true);
        Granulariteit granulariteit = null;
        if (n.has("timeline")) {
            String tp = pad + "/timeline";
            JsonNode tl = n.get("timeline");
            velden(tl, tp, "TimelineFacet", List.of("node", "granularity", "count"), List.of());
            granulariteit = enumWaarde(Spelling.GRANULARITY, "TimelineFacet.granularity",
                    tekst(tl, tp, "granularity"), tp + "/granularity");
            int aantal = geheel(tl, tp, "count", "TimelineFacet.count");
            if (aantal < 1)
                throw new Documentfout(tp + "/count", "the timeline facet of parameter '" + naam + "' has count "
                        + aantal + " < 1");
            if (aantal > 1)
                throw new Documentfout(tp + "/count", "the timeline facet of parameter '" + naam + "' has count "
                        + aantal + " > 1: a parameter's timeline is one period while multi-period grids are open"
                        + " (RUNTIME-SEMANTICS ch. 3.12)");
        }
        return new Parameter(naam, datatype, granulariteit);
    }

    /** IR-SPEC ch. 2.5: a unit system and its units, one Unit per abbreviation under the name identity of ch. 2.1
     *  (WF-009, the identity its canonical lookup resolves under). The model keeps every unit under its abbreviation,
     *  its further spellings in the alias map, and its exact factor beside the record's decimal one. */
    private Eenheidstelsel unitSystem(String pad, JsonNode n) {
        velden(n, pad, "UnitSystem", List.of("node", "name", "units"), List.of());
        Eenheidstelsel es = new Eenheidstelsel(naam(n, pad, "name"));
        lijst(n, pad, "units", (up, u) -> {
            Eenheid e = unit(u, up, es);
            if (botsing(es.eenheden.keySet(), e.afkorting()) != null)
                throw new Documentfout(up + "/abbreviation", "the unit '" + e.afkorting() + "' is declared twice in"
                        + " the unit system '" + es.naam + "': one Unit per abbreviation (WF-009)");
            es.eenheden.put(e.afkorting(), e);
            if (u.has("spellings"))
                for (String spelling : tekstLijst(u, up, "spellings")) es.aliassen.put(spelling, e);
        });
        return es;
    }

    /** A unit where it occurs — on a number, as a rate's denominator: IR-SPEC ch. 2.5's fields and nothing else, so
     *  a spelling there is an unknown field (spellings belong to the declaration). */
    private Eenheid unit(JsonNode n, String pad) { return unit(n, pad, null); }

    /** A unit, with {@code stelsel} the unit system it is declared in, or null where it occurs. A declared unit may
     *  carry its further spellings, at least one; its exact factor lands in the system. */
    private Eenheid unit(JsonNode n, String pad, Eenheidstelsel stelsel) {
        // a factor written null is a factor in no form, refused in the factor's own sentence
        if (n.isObject() && n.has("factorToCanonical") && n.get("factorToCanonical").isNull())
            throw new Documentfout(pad + "/factorToCanonical", FACTOR_VORM + "null");
        velden(n, pad, "Unit", List.of("node", "abbreviation"), stelsel == null
                ? List.of("system", "canonical", "factorToCanonical", "denominator")
                : List.of("system", "canonical", "factorToCanonical", "denominator", "spellings"));
        String afkorting = tekst(n, pad, "abbreviation");
        Eenheidstelsel.Factor factor = n.has("factorToCanonical")
                ? factor(n.get("factorToCanonical"), pad + "/factorToCanonical") : null;
        Eenheid noemer = n.has("denominator") ? unit(n.get("denominator"), pad + "/denominator") : null;
        if (stelsel != null && n.has("spellings") && tekstLijst(n, pad, "spellings").isEmpty())
            throw new Documentfout(pad + "/spellings", SPELLINGEN_LEEG);
        if (stelsel != null && factor != null) stelsel.exacteFactoren.put(afkorting, factor);
        return new Eenheid(afkorting, optTekst(n, pad, "system"), optTekst(n, pad, "canonical"),
                factor == null ? null : decimaal(factor), noemer);
    }

    /** The shared loader's sentence for an empty list of spellings. */
    static final String SPELLINGEN_LEEG = "a unit's spellings are at least one spelling, or the field is absent";

    /** The exact factor (IR-SPEC ch. 2.5): an object of exactly the two integers {@code num} and {@code den}, each at
     *  least 1 and at most 2^62 - 1, the schema's bound and the shared loader's native integer range. Every other form
     *  — a JSON number, a string, a missing or unknown key, a value below 1 or above the bound — is refused in the
     *  shared loader's one sentence. That the fraction is in lowest terms is the verifier's WF-092, not a load
     *  question, so {2, 4} is read as 1/2, as the shared loader reads it for {@code ref} and the ASP backend. */
    private static Eenheidstelsel.Factor factor(JsonNode n, String pad) {
        if (n.isObject() && n.size() == 2 && n.has("num") && n.has("den")) {
            BigInteger num = geheelVanAfEen(n.get("num")), den = geheelVanAfEen(n.get("den"));
            if (num != null && den != null) return new Eenheidstelsel.Factor(num, den);
        }
        throw new Documentfout(pad, FACTOR_VORM + n);
    }

    /** The shared loader's sentence for a factor in any other form than {@code {num, den}}; the value follows. */
    static final String FACTOR_VORM = "a conversion factor is an exact rational {num, den} whose two integers are at"
            + " least 1 and at most 4611686018427387903 (2^62 - 1), got ";

    private static BigInteger geheelVanAfEen(JsonNode c) {
        if (!c.isIntegralNumber()) return null;
        BigInteger v = c.bigIntegerValue();
        return v.signum() > 0 && v.bitLength() <= 62 ? v : null;
    }

    /** The record's decimal factor of an exact fraction: a whole number exactly, any other fraction rounded to 16
     *  significant digits, as the frontend rounds a declared "1/N" (see the class comment). */
    private static BigDecimal decimaal(Eenheidstelsel.Factor f) {
        return f.den().equals(BigInteger.ONE) ? new BigDecimal(f.num())
                : new BigDecimal(f.num()).divide(new BigDecimal(f.den()), MathContext.DECIMAL64);
    }

    /** IR-SPEC ch. 2.9: a day kind and the plural it takes as a duration unit. The model keeps the names, and a
     *  registry from each unit spelling of a day kind (lower-cased) to its name that the transpilers' day-kind
     *  recognition reads: the name and then the plural, the first to claim a spelling keeping it, as the frontend
     *  registers them. */
    private void dayKind(String pad, JsonNode n) {
        velden(n, pad, "DayKind", List.of("node", "name"), List.of("plural"));
        String naam = tekst(n, pad, "name");
        String meervoud = optTekst(n, pad, "plural");
        m.dagsoorten.add(naam);
        m.dagsoortPerEenheid.putIfAbsent(naam.trim().toLowerCase(), naam);
        if (meervoud != null) m.dagsoortPerEenheid.putIfAbsent(meervoud.trim().toLowerCase(), naam);
    }

    /** IR-SPEC ch. 2.6: a fact type and its roles. A role's plural is written whole or not at all: absent is the
     *  record's null. */
    private Feittype feittype(String pad, JsonNode n) {
        velden(n, pad, "FactType", List.of("node", "name", "reciprocal", "roles"), List.of());
        Feittype ft = new Feittype(naam(n, pad, "name"));
        ft.wederkerig = bool(n, "reciprocal");
        elementen(n, "roles", r -> {
            velden(r, padVan(r), "Role", List.of("node", "name", "objectType", "many"), List.of("plural"));
            ft.rollen.add(new RegelSpraakModel.Rol(
                    text(r, "name"), text(r, "objectType"), text(r, "plural"), bool(r, "many")));
        });
        return ft;
    }

    /** IR-SPEC ch. 2.7: a dimension and its labels. Its plural and its preposition are written whole or not at all:
     *  absent is the record's null. */
    private Dimensie dimensie(String pad, JsonNode n) {
        velden(n, pad, "Dimension", List.of("node", "name", "labels"), List.of("plural", "preposition"));
        return new Dimensie(naam(n, pad, "name"), text(n, "plural"),
                text(n, "preposition"), stringList(n.get("labels")));
    }

    /** IR-SPEC ch. 2.8: a named timeline. Its granularity is the one field of the family that admits a null. */
    private Tijdlijn tijdlijn(JsonNode n) {
        String p = padVan(n);
        velden(n, p, "Timeline", List.of("node", "name", "variableStart", "granularity", "count"), List.of());
        Granulariteit granulariteit = n.get("granularity").isNull()
                && Spelling.GRANULARITY.laatNullToe("Timeline.granularity") ? null
                : enumWaarde(Spelling.GRANULARITY, "Timeline.granularity", tekst(n, p, "granularity"),
                        p + "/granularity");
        return new Tijdlijn(text(n, "name"), bool(n, "variableStart"), granulariteit, intOf(n, "count"));
    }

    // --- Datatype ----------------------------------------------------------

    /** The datatype dispatch. {@code pad} is the JSON pointer of the datatype; {@code declaratie} says whether it
     *  stands at a declaration (the datatype of an attribute, a parameter or a domain) or at a type annotation (the
     *  {@code type} of an expression node), where a presentation tag is refused (WF-007). */
    private Datatype datatype(JsonNode n, String pad, boolean declaratie) {
        if (n == null || n.isNull()) return null;
        if (!n.isObject()) throw new Documentfout(pad, "expected a datatype object or null, got " + kort(n));
        String node = datatypeSoort(n, pad);
        List<String> sleutels = DATATYPE.get(node);
        if (sleutels != null) velden(n, pad, node, sleutels, List.of());
        return switch (node) {
            case "Number" -> number(n, pad, declaratie);
            case "Date" -> {
                velden(n, pad, "Date", List.of("node", "precision"), List.of());
                yield enumWaarde(Spelling.DATE_PRECISION, "Date.precision", tekst(n, pad, "precision"),
                        pad + "/precision");
            }
            case "Text" -> new Datatype.Tekst();
            case "Boolean" -> new Datatype.Booleaan();
            case "Enumeration" -> new Datatype.Enumeratie(text(n, "domain"), stringList(n.get("values")));
            // no form at all, refused at the discriminator in the shared loader's words: Onbekend, because IR-SPEC
            // ch. 2.4's datatype union has no member for an unknown type (a marker leaves its type out and a derived
            // position the producer could not type is null), the four datatype nodes of the L2 shape, which no
            // document carries since L3, and the three of the v0 shape that L5 renamed
            case "Onbekend", "Numeriek", "Percentage", "DatumInDagen", "DatumTijd", "Tekst", "Booleaan", "Enumeratie" ->
                    throw new Documentfout(pad + "/node", "unknown datatype node \"" + node + "\"");
            default -> throw new Refusal("Datatype:" + node, "unknown datatype node");
        };
    }

    /** IR-SPEC ch. 2.4: a number, its sign always written. One whose unit is '%' is a percentage, which the model
     *  holds as a {@link Datatype.Percentage}; that record has no sign, and the wire writes a percentage's sign
     *  unspecified. The presentation tag presents a percentage, stands at a declaration only and means nothing to
     *  evaluation. Every refusal here is the shared OCaml loader's or the verifier's, in their words. */
    private Datatype number(JsonNode n, String pad, boolean declaratie) {
        velden(n, pad, "Number", List.of("node", "whole", "decimals", "sign"), List.of("unit", "presentation"));
        boolean geheel = waarheid(n, pad, "whole");
        int decimalen = geheel(n, pad, "decimals", "Number.decimals");
        Datatype.Teken teken = enumWaarde(Spelling.NUMBER_SIGN, "Number.sign", tekst(n, pad, "sign"), pad + "/sign");
        Eenheid eenheid = n.has("unit") ? unit(n.get("unit"), pad + "/unit") : null;
        String presentatie = optTekst(n, pad, "presentation");
        if (presentatie != null && !presentatie.equals("percentage"))
            throw new Documentfout(pad + "/presentation", onbekendeWaarde("presentation", presentatie));
        boolean procent = eenheid != null && eenheid.afkorting().equals("%");
        if (procent && teken != Datatype.Teken.ONBEPAALD)
            throw new Documentfout(pad + "/sign", PROCENT_MET_TEKEN);
        if (!procent && presentatie != null) throw new Documentfout(pad + "/presentation", TAG_ZONDER_PROCENT);
        if (presentatie != null && !declaratie)
            throw new Documentfout(pad + "/presentation", TAG_OP_ANNOTATIE + " (WF-007)");
        return procent ? new Datatype.Percentage(geheel, decimalen, eenheid)
                : new Datatype.Numeriek(geheel, decimalen, teken, eenheid);
    }

    /** The shared loader's sentence for a value outside an enumeration: its family's values, a v0 spelling
     *  included, or the one presentation tag. */
    static String onbekendeWaarde(String veld, String waarde) {
        return "unknown " + veld + " value \"" + waarde + "\"";
    }

    /** The in-memory value a wire spelling names at {@code plaats} (IR-SPEC, Conventions: every enumeration value is
     *  read through the one translation table), or the shared loader's refusal naming the field — the last segment
     *  of the place — and the value, a v0 spelling included. A value the place refuses by name never reaches here:
     *  its reader refuses it first, in the rule's sentence. */
    private static <K> K enumWaarde(Spelling.Familie<K> familie, String plaats, String waarde, String pad) {
        K k = familie.waarde(plaats, waarde);
        if (k != null) return k;
        if (familie.weigertBijNaam(plaats, waarde))
            throw new IllegalStateException("IrLoader: '" + waarde + "' at " + plaats
                    + " is refused by name by its reader");
        throw new Documentfout(pad, onbekendeWaarde(plaats.substring(plaats.lastIndexOf('.') + 1), waarde));
    }

    /** The in-memory value of a spelling the table lists among those some place refuses by name. */
    private static <K> K geweigerd(Spelling.Familie<K> familie, String spelling) {
        for (Map.Entry<K, String> e : familie.geweigerd().entrySet())
            if (e.getValue().equals(spelling)) return e.getKey();
        throw new IllegalStateException("IrLoader: '" + spelling + "' is no value the family " + familie.naam()
                + " refuses by name");
    }

    /** The shared loader's sentence: the percentage record has no sign, and the wire writes a percentage unsigned. */
    static final String PROCENT_MET_TEKEN =
            "a percentage carries no sign restriction: a number whose unit is '%' has the sign unspecified";
    /** The shared loader's sentence: the tag presents a percentage, and a number without the unit '%' is none. */
    static final String TAG_ZONDER_PROCENT = "the presentation \"percentage\" on a number whose unit is not '%'";
    /** WF-007's presentation arm — the verifier's sentence. */
    static final String TAG_OP_ANNOTATIE = "the presentation tag \"percentage\" on a type annotation: the tag is"
            + " written only where a datatype is declared";

    /** The {@code node} of a datatype object, required: a switch on an absent one ended in a NullPointerException
     *  that named no place in the document. */
    private static String datatypeSoort(JsonNode n, String pad) {
        JsonNode soort = n.get("node");
        if (soort == null) throw new Documentfout(pad, "missing field 'node' (the datatype discriminator)");
        if (!soort.isTextual()) throw new Documentfout(pad + "/node", "expected a string, got " + kort(soort));
        return soort.asText();
    }

    // --- the key sets of the rule-side nodes ----------------------------------

    /** A node's keys as the schema gives them: required, then optional (IR-SPEC § Method). */
    private record Sleutels(List<String> verplicht, List<String> optioneel) {}

    private static Map.Entry<String, Sleutels> sleutels(String node, List<String> verplicht, String... optioneel) {
        return Map.entry(node, new Sleutels(verplicht, List.of(optioneel)));
    }

    /** Every expression node this loader reads, with the schema's keys. A node kind outside the table is a form this
     *  backend does not carry and reaches the dispatch's refusal; the union's selection is read inside its union. */
    private static final Map<String, Sleutels> EXPRESSIE = Map.ofEntries(
            sleutels("Literal", List.of("node", "value", "type"), "source"),
            sleutels("TextComposition", List.of("node", "parts", "type"), "source"),
            sleutels("Binary", List.of("node", "op", "left", "right", "type"), "source"),
            sleutels("Aggregation", List.of("node", "kind", "arguments", "type"), "elementType", "filter",
                    "emptyFallback", "dimensionSelection", "sourceRole", "sourcePath", "source"),
            sleutels("BuiltinCall", List.of("node", "kind", "arguments", "type"), "filter", "granularity", "source"),
            sleutels("Rounding", List.of("node", "base", "mode", "decimals", "type"), "source"),
            sleutels("Clamp", List.of("node", "base", "type"), "minimum", "maximum", "source"),
            sleutels("AttributeReference", List.of("node", "attribute", "subjectType"), "path", "source"),
            sleutels("DimensionCellRead", List.of("node", "attribute", "labels"), "source"),
            sleutels("ParameterReference", List.of("node", "parameter"), "source"),
            sleutels("VariableReference", List.of("node", "name"), "source"),
            sleutels("BoundDay", List.of("node", "type"), "source"),
            sleutels("ContextValue", List.of("node", "name", "type"), "source"),
            sleutels("Unresolved", List.of("node"), "type", "source"),
            sleutels("Let", List.of("node", "type", "binder", "value", "body"), "source"),
            sleutels("BinderRef", List.of("node", "name"), "source"),
            sleutels("IfThenElse", List.of("node", "type", "condition", "then", "else"), "source"),
            sleutels("InstanceSetUnion", List.of("node", "branches"), "source"));

    /** Every condition node this loader reads, with the schema's keys. The condition marker carries no type: that
     *  field is the expression marker's alone. */
    private static final Map<String, Sleutels> CONDITIE = Map.ofEntries(
            sleutels("Comparison", List.of("node", "op", "left", "right"), "wholePeriod", "source"),
            sleutels("Predicate", List.of("node", "kind", "operand", "negated", "digits"), "dayKind", "wholePeriod",
                    "source"),
            sleutels("CharacteristicCheck", List.of("node", "characteristic", "negated"), "host", "wholePeriod",
                    "source", "navigation"),
            sleutels("RoleMembership", List.of("node", "factType", "role", "negated"), "counterpartType", "source"),
            sleutels("TimeWindow", List.of("node", "toInclusive"), "from", "to", "source"),
            sleutels("Compound", List.of("node", "quantifier", "threshold", "parts"), "source"),
            sleutels("QuantifierFold", List.of("node", "quantifier", "elementType", "test"),
                    "threshold", "filter", "source", "sourceRole", "sourcePath"),
            sleutels("Regelversie", List.of("node", "regel", "inconsistent"), "source"),
            sleutels("Unresolved", List.of("node"), "source"));

    /** The two parts of a text composition. */
    private static final Map<String, Sleutels> DEEL = Map.ofEntries(
            sleutels("LiteralChunk", List.of("node", "text")),
            sleutels("EmbeddedExpression", List.of("node", "expr")));

    /** The discriminator of an expression or condition node, and — for the kinds this loader reads — its key set,
     *  checked as {@link #velden} checks every node since L2's fix round: no key outside the node's own, every
     *  required key present, an optional one written whole or not at all. Until then a field no node has was read
     *  past on most rule-side nodes, a condition marker carrying a type among them. A scalar where the node should
     *  stand is refused by the caller's {@link #knoopVeld} or {@link #elementen}, at the field's pointer. */
    private String knoopSoort(JsonNode n, Map<String, Sleutels> tabel, String wat) {
        // every caller reads the node through knoopVeld or elementen, which refuse a non-object at its pointer
        String p = padVan(n);
        JsonNode soort = n.get("node");
        if (soort == null) throw new Documentfout(p, "missing field 'node' (the " + wat + " discriminator)");
        if (!soort.isTextual()) throw new Documentfout(p + "/node", "expected a string, got " + kort(soort));
        Sleutels sl = tabel.get(soort.asText());
        if (sl != null) velden(n, p, soort.asText(), sl.verplicht(), sl.optioneel());
        return soort.asText();
    }

    // --- Expr --------------------------------------------------------------

    private Expr expr(JsonNode n) {
        if (n == null || n.isNull()) return null;
        String node = knoopSoort(n, EXPRESSIE, "expression");
        return switch (node) {
            case "Literal" -> literal(n);
            case "TextComposition" -> {
                afgeleidType(n);
                List<Expr.TekstReeks.Deel> delen = new ArrayList<>();
                elementen(n, "parts", d -> delen.add(tekstReeksDeel(d)));
                yield new Expr.TekstReeks(delen, bron(n));
            }
            case "Binary" -> {
                String p = padVan(n);
                yield new Expr.Binair(enumWaarde(Spelling.BINARY_OP, "Binary.op", tekst(n, p, "op"), p + "/op"),
                        expr(knoopVeld(n, "left")), expr(knoopVeld(n, "right")), typeVeld(n), bron(n));
            }
            case "Aggregation" -> aggregation(n);
            case "BuiltinCall" -> builtinCall(n);
            case "Rounding" -> {
                String p = padVan(n);
                afgeleidType(n);
                yield new Expr.Afgerond(expr(knoopVeld(n, "base")),
                        enumWaarde(Spelling.ROUNDING_MODE, "Rounding.mode", tekst(n, p, "mode"), p + "/mode"),
                        intOf(n, "decimals"), bron(n));
            }
            // a bound is written whole or not at all: absent is no bound on that side, the record's null
            case "Clamp" -> {
                afgeleidType(n);
                yield new Expr.Begrensd(expr(knoopVeld(n, "base")), expr(knoopVeld(n, "minimum")),
                        expr(knoopVeld(n, "maximum")), bron(n));
            }
            // IR-SPEC ch. 4.1/4.1a: the reference nodes carry no type (the declaration is the one source) and a read
            // carries no written navigation; the record's components for both stay empty
            case "AttributeReference" -> {
                String p = padVan(n);
                Expr.AttribuutRef a = new Expr.AttribuutRef(text(n, "attribute"), ot(text(n, "subjectType")),
                        null, null, null, bron(n), false, List.of());
                plekken.put(a, p);
                if (n.has("path")) {
                    // written whole or not at all, never empty (IR-SPEC ch. 4.1a): an empty list would read as the
                    // subject's own attribute
                    List<RolBindingRef> stappen = rolBindingLijst(n, p, "path");
                    if (stappen.isEmpty()) throw new Documentfout(p + "/path", PAD_ZONDER_STAP);
                    navigatieBindingen.put(a, List.copyOf(stappen));
                }
                yield a;
            }
            case "DimensionCellRead" -> {
                yield new Expr.DimensieCel((Expr.AttribuutRef) expr(knoopVeld(n, "attribute")),
                        stringList(n.get("labels")),
                        bron(n));
            }
            case "ParameterReference" -> {
                yield new Expr.ParameterRef(param(text(n, "parameter")), bron(n));
            }
            case "VariableReference" -> {
                yield new Expr.VariabeleRef(text(n, "name"), null, bron(n));
            }
            // IR-SPEC ch. 4.1: the bound day of a day-kind definition and a session value, each a node of its own; the
            // record keeps the one constant it always was, named "de dag" for the first
            case "BoundDay" -> new Expr.Constante("de dag", contextType(n, BOUNDDAY_TYPE), bron(n));
            // the record keeps the session value's v0 name, which the table gives for the wire's (WF-098: a name
            // outside the two is refused, byte for byte)
            case "ContextValue" -> {
                String p = padVan(n);
                JsonNode naam = n.get("name");
                if (!naam.isTextual()) throw new Documentfout(p + "/name", CONTEXTWAARDE_NAAM);
                String geheugen = Spelling.CONTEXT_VALUE_NAME.waarde("ContextValue.name", naam.asText());
                if (geheugen == null) throw new Documentfout(p + "/name", contextwaardeWeigering(naam.asText()));
                yield new Expr.Constante(geheugen, contextType(n, CONTEXTWAARDE_TYPE), bron(n));
            }
            // the expression nodes of the v0 shape that L4 replaced and L5 renamed: no node of this format
            case "Functie", "Constante", "TekstReeks", "Binair", "Afgerond", "Begrensd", "AttribuutRef", "DimensieCel",
                 "ParameterRef", "VariabeleRef", "Onopgelost" ->
                    throw new Documentfout(padVan(n) + "/node", "unknown expr node \"" + node + "\"");
            // reden null: it is the FRONTEND's own field (the WARNING it raises over a surviving marker)
            // and the wire carries no such key, so a marker read off the wire has none to recover. IR-SPEC
            // ch. 4.4: an absent type is an unknown one, and the record holds null for it.
            case "Unresolved" -> {
                String p = padVan(n);
                if (!n.has("source")) throw new Documentfout(p, MARKER_ZONDER_BRON);
                yield new Expr.Onopgelost(n.has("type") ? datatype(n.get("type"), p + "/type", false) : null, bron(n),
                        null);
            }
            // IR-SPEC ch. 4.10 the Let cluster — all three are ORDINARY union nodes, built straight into the tree
            // with nothing riding beside them. Note the wire's field names: a BinderRef's is "name", an
            // IfThenElse's are "condition"/"then"/"else" — the condition reuses the ordinary conditie union.
            case "Let" -> let(new Expr.Let(
                    verplicht("Let", "binder", text(n, "binder")),
                    verplicht("Let", "value", expr(knoopVeld(n, "value"))),
                    verplicht("Let", "body", expr(knoopVeld(n, "body"))),
                    verplicht("Let", "type", typeVeld(n)), bron(n)));
            case "BinderRef" -> {
                yield new Expr.BinderRef(verplicht("BinderRef", "name", text(n, "name")), null, bron(n));
            }
            case "IfThenElse" -> new Expr.IfThenElse(
                    verplicht("IfThenElse", "condition", conditie(knoopVeld(n, "condition"))),
                    verplicht("IfThenElse", "then", expr(knoopVeld(n, "then"))),
                    verplicht("IfThenElse", "else", expr(knoopVeld(n, "else"))),
                    verplicht("IfThenElse", "type", typeVeld(n)), bron(n));
            // IR-SPEC ch. 4.3 / WF-110: the §5.7 concatenation of subject expressions as the source of an
            // instance count. Read VERBATIM, one selection per branch, for the reason sourcePath is: every
            // selection names its own population and its own relation, and a backend that re-derived either
            // from the type pair would give the selections one and the same range — they all share one pair
            // by WF-110 (d) — and count a restatement of one of them. The two required fields are refused
            // BY NAME here, on the verplicht() pattern, so a document that reaches this executor without
            // passing the verifier fails naming the shape it violates.
            case "InstanceSetUnion" -> {
                List<Expr.InstanceSetUnion.Tak> takken = new ArrayList<>();
                elementen(n, "branches", b -> {
                    String bp = padVan(b);
                    velden(b, bp, "InstanceSetBranch", List.of("node", "elementType", "sourceRole", "filter"),
                            List.of("source"));
                    RegelSpraakModel.Objecttype el = verplicht("InstanceSetBranch", "elementType",
                            ot(text(b, "elementType")));
                    // IR-SPEC ch. 4.3: every selection names its relation; nothing derives a missing one
                    RolBindingRef rb = rolBinding(b, bp, "sourceRole");
                    if (rb == null) throw new Documentfout(bp + "/sourceRole", "expected a RoleBinding, got null");
                    takken.add(new Expr.InstanceSetUnion.Tak(el, rb, conditie(knoopVeld(b, "filter")), bron(b)));
                });
                if (takken.size() < 2)
                    throw new Refusal("InstanceSetUnion.branches",
                            "an instance-set union carries at least TWO selections, found " + takken.size()
                                    + " — a single selection IS the ordinary instance count and has a spelling"
                                    + " of its own (WF-110 (b))");
                yield new Expr.InstanceSetUnion(List.copyOf(takken), bron(n));
            }
            default -> throw new Refusal("Expr:" + node, "unknown expression node");
        };
    }

    // --- the expression layer of L4 (B21, B22, B23) -----------------------------------------------------

    /** IR-SPEC ch. 4.3: a fold, one of the eight aggregation kinds, read into the frontend's function record with no
     *  granularity (a builtin's field, unknown on this node). */
    private Expr aggregation(JsonNode n) {
        String p = padVan(n);
        Expr.FunctieKind fk = enumWaarde(Spelling.AGGREGATION_KIND, "Aggregation.kind", tekst(n, p, "kind"),
                p + "/kind");
        List<Expr> args = new ArrayList<>();
        elementen(n, "arguments", a -> args.add(expr(a)));
        String elementType = optTekst(n, p, "elementType");
        Expr.Functie f = new Expr.Functie(fk, args, ot(elementType), expr(knoopVeld(n, "emptyFallback")),
                conditie(knoopVeld(n, "filter")), dimensieSelectie(knoopVeld(n, "dimensionSelection")), typeVeld(n),
                bron(n), null);
        // IR-SPEC ch. 4.3: the range of a fold over ONE source is one field with two spellings, and a fold with an
        // element type and a kind other than total carries it — without it the argument is the subject's own
        // attribute and the fold folds something the sentence did not name. Both spellings at once is no document.
        if (n.has("sourceRole") && n.has("sourcePath")) throw new Documentfout(p + "/sourcePath", BEREIK_TWEEMAAL);
        // WF-102 (c): a count's range is one step, whatever its source reads — asked before anything of the source
        // is, so a source this loader cannot judge (a null name, a navigated or non-reference argument) is no way
        // past it (measured: with a null name the count was counted along the path)
        if (fk == Expr.FunctieKind.AANTAL && n.has("sourcePath")) throw new Documentfout(p + "/sourcePath", BRONPAD_C);
        boolean eenBron = elementType != null && args.size() == 1;
        if (eenBron && fk != Expr.FunctieKind.TOTAAL && !n.has("sourceRole") && !n.has("sourcePath"))
            throw new Documentfout(p, BEREIK_ONTBREEKT);
        // IR-SPEC ch. 4.3 / WF-102: a MULTI-STEP fold-source range. Read VERBATIM — the full ordered list of
        // RolBinding steps, nothing derived from the presentation strings and nothing re-derived from the endpoint
        // type PAIR. That is the whole point of the field: a by-pair search binds a coincidental DIRECT relation
        // between subject and element type and folds the WRONG set, which is exactly what this executor did when
        // the field was dropped (measured on jutterswet-vondstketen: 5,00 where the sentence says 3,00, with ref
        // and clingo answering 3,00). The two SHAPE arms below are refused BY NAME here rather than downstream, for
        // ref's reason. The MODEL-relative arms (a step that does not join, a landing beside the element type, a
        // hop the beans carry no field for) belong to the walk and are per-form refusals in the transpilers, which
        // know the fold SUBJECT this node does not.
        if (n.has("sourcePath")) {
            List<RolBindingRef> pad = rolBindingLijst(n, p, "sourcePath");
            if (pad.size() < 2)
                throw new Refusal("Aggregation.sourcePath", "a sourcePath carries at least TWO steps, found "
                        + pad.size() + " — the one-step range is spelled sourceRole");
            if (!isLedenVouwbron(n)) throw new Documentfout(p + "/sourcePath", BRONPAD_C);
            bronPaden.put(f, List.copyOf(pad));
        }
        // the one-step range — see Loaded#rolBindingen. It stays OUT of the record (that one is vendored).
        RolBindingRef br = rolBinding(n, p, "sourceRole");
        if (br != null) rolBindingen.put(f, br);
        if (eenBron && (n.has("sourceRole") || n.has("sourcePath"))) keurVouwbron(n, p, fk, f);
        plekken.put(f, p);
        return f;
    }

    /** IR-SPEC ch. 4.3: every other function kind, with plain arguments, read into the frontend's function record
     *  with none of the fold's fields (unknown on this node). */
    private Expr builtinCall(JsonNode n) {
        String p = padVan(n);
        Expr.FunctieKind fk = enumWaarde(Spelling.BUILTIN_KIND, "BuiltinCall.kind", tekst(n, p, "kind"), p + "/kind");
        List<Expr> args = new ArrayList<>();
        elementen(n, "arguments", a -> args.add(expr(a)));
        String g = optTekst(n, p, "granularity");
        Granulariteit granulariteit = null;
        if (g != null) {
            granulariteit = enumWaarde(Spelling.GRANULARITY, "BuiltinCall.granularity", g, p + "/granularity");
            // WF-037: the facet belongs to the two bucketing builtins. On any other kind neither branch reads it, and
            // both played the call as if it were not written (measured, yearOf with month), where the verifier
            // refuses it and ref faults.
            if (fk != Expr.FunctieKind.TIJDSEVENREDIG_DEEL && fk != Expr.FunctieKind.AANTAL_DAGEN_IN)
                throw new Documentfout(p, "granularity facet on a non-bucketing kind");
        }
        Expr.Functie f = new Expr.Functie(fk, args, null, null, conditie(knoopVeld(n, "filter")), null, typeVeld(n),
                bron(n), granulariteit);
        plekken.put(f, p);
        return f;
    }

    /** The shared loader's sentence for two range spellings on one fold. */
    static final String BEREIK_TWEEMAAL = "sourcePath beside sourceRole: a fold carries one range, never two";
    /** The shared loader's sentence for a fold over one source without its range. */
    static final String BEREIK_ONTBREEKT = "a single-argument fold with an elementType carries its range (sourceRole"
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
        String p = padVan(n);
        JsonNode waarde = n.get("value");
        String variant = null, lexeem = null;
        if (waarde.isObject() && waarde.size() == 1) {
            variant = waarde.fieldNames().next();
            JsonNode v = waarde.get(variant);
            if (variant.equals("boolean")) lexeem = v.isBoolean() ? (v.booleanValue() ? "waar" : "onwaar") : null;
            else if (LITERAALVARIANTEN.contains(variant) && v.isTextual()) lexeem = v.asText();
        }
        if (lexeem == null) throw new Documentfout(p + "/value", LITERAALWAARDE);
        Datatype type = typeVeld(n);
        if (type != null) {
            String weigering = literaalvariantWeigering(variant, type);
            if (weigering != null) throw new Documentfout(p + "/value", weigering);
        }
        if (variant.equals("number") && !GETAL.matcher(lexeem).matches())
            throw new Documentfout(p + "/value", "numeric literal lexeme '" + lexeem + "' is not a number of the case"
                    + " format (an optional '-', digits, an optional '.' and digits, or a rational a/b whose denominator"
                    + " has no leading zero — ch. 4.1)");
        if (variant.equals("date") && !isoDatum(lexeem))
            throw new Documentfout(p + "/value", "date literal lexeme '" + lexeem + "' is not the ISO form yyyy-mm-dd"
                    + " (fixed field widths, valid ranges — ch. 4.1)");
        if (variant.equals("dateTime") && !isoDatumTijd(lexeem))
            throw new Documentfout(p + "/value", "millisecond-precision date literal lexeme '" + lexeem + "' is not the"
                    + " canonical ISO form yyyy-mm-ddThh:mm:ss[.fff] (fixed field widths, 'T' never a space, the seconds"
                    + " field written, no zone or offset, a fraction of exactly three digits; the day-first source"
                    + " snippet belongs in `source` — ch. 4.1)");
        return new Expr.Literal(lexeem, type, bron(n));
    }

    /** WF-084's grammar, the verifier's: four-two-two digit fields, a month of the year and a day of that month,
     *  leap years included. */
    private static boolean isoDatum(String s) {
        java.util.regex.Matcher m = DATUM.matcher(s);
        return m.matches() && kalenderdag(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)),
                Integer.parseInt(m.group(3)));
    }

    /** WF-095's grammar, the verifier's: the date of WF-084, a 'T', hours, minutes and seconds in range, and an
     *  optional fraction of exactly three digits, with nothing after it. */
    private static boolean isoDatumTijd(String s) {
        java.util.regex.Matcher m = DATUMTIJD.matcher(s);
        return m.matches() && kalenderdag(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)),
                Integer.parseInt(m.group(3))) && Integer.parseInt(m.group(4)) <= 23 && Integer.parseInt(m.group(5)) <= 59
                && Integer.parseInt(m.group(6)) <= 59;
    }

    private static boolean kalenderdag(int jaar, int maand, int dag) {
        return maand >= 1 && maand <= 12 && dag >= 1 && dag <= java.time.YearMonth.of(jaar, maand).lengthOfMonth();
    }

    private static final java.util.regex.Pattern DATUM =
            java.util.regex.Pattern.compile("([0-9]{4})-([0-9]{2})-([0-9]{2})");
    private static final java.util.regex.Pattern DATUMTIJD = java.util.regex.Pattern.compile(
            "([0-9]{4})-([0-9]{2})-([0-9]{2})T([0-9]{2}):([0-9]{2}):([0-9]{2})(\\.[0-9]{3})?");

    /** The number grammar of the case format (uchar-case-v0), which the literal's number variant carries. */
    private static final java.util.regex.Pattern GETAL =
            java.util.regex.Pattern.compile("-?[0-9]+(\\.[0-9]+)?|-?[0-9]+/[1-9][0-9]*");

    /** The seven variants of the case format's value union, in its order. */
    private static final List<String> LITERAALVARIANTEN = List.of("number", "boolean", "text", "date", "dateTime",
            "enumeration", "constructor");

    /** The shared loader's sentence for a literal value in any other shape. */
    static final String LITERAALWAARDE = "a literal value is exactly one variant: {\"number\": string}, {\"boolean\":"
            + " true or false}, {\"text\": string}, {\"date\": string}, {\"dateTime\": string}, {\"enumeration\":"
            + " string} or {\"constructor\": string}, without a payload";

    /** WF-114 (IR-SPEC ch. 4.1), the shared loader's sentence: null when {@code variant} is the one {@code type}
     *  names. Every datatype this backend carries names a variant; a sum, list or struct type is refused as a form
     *  before a literal reaches here. */
    static String literaalvariantWeigering(String variant, Datatype type) {
        String benoemd, frase;
        switch (type) {
            case Datatype.Numeriek x -> { benoemd = "number"; frase = "a Number"; }
            case Datatype.Percentage x -> { benoemd = "number"; frase = "a Number"; }
            case Datatype.Booleaan x -> { benoemd = "boolean"; frase = "a Boolean"; }
            case Datatype.Tekst x -> { benoemd = "text"; frase = "a Text"; }
            case Datatype.DatumInDagen x -> { benoemd = "date"; frase = "a day-precision Date"; }
            case Datatype.DatumTijd x -> { benoemd = "dateTime"; frase = "a millisecond-precision Date"; }
            case Datatype.Enumeratie x -> { benoemd = "enumeration"; frase = "an Enumeration"; }
            case Datatype.Onbekend x -> throw new IllegalStateException("IrLoader: an Onbekend datatype is refused"
                    + " while it is read, before a literal is crossed against it");
        }
        if (benoemd.equals(variant)) return null;
        return "literal value is " + (variant.equals("enumeration") ? "an" : "a") + " " + variant + " value but its"
                + " type is " + frase + ": the variant is the one the type names";
    }

    /** WF-098, the shared loader's sentence for a context value outside the closed set of IR-SPEC ch. 4.1 — the bound
     *  day's name included, since that day is a node of its own, and the v0 names of the two. */
    static String contextwaardeWeigering(String naam) {
        return "context value '" + naam + "' is not one of the two admissible names evaluationDate, evaluationYear:"
                + " the context-value name is a closed set, spelled byte for byte";
    }

    /** The shared loader's sentence for a path without a hop: a read without navigation leaves the field out. */
    static final String PAD_ZONDER_STAP = "an empty path is not a navigation";

    /** The shared loader's sentences for the name and the datatype the two context nodes require. */
    static final String CONTEXTWAARDE_NAAM = "a ContextValue carries its name as a string";
    static final String CONTEXTWAARDE_TYPE = "a ContextValue carries its datatype";
    static final String BOUNDDAY_TYPE = "a BoundDay carries its datatype";
    /** WF-108 (a), the verifier's sentence for the one quantifier a quantifier fold does not admit: the executors
     *  refuse it at load, as the shared loader does when it reads for an executor. */
    static final String KWANTORVOUW_THE = "quantifier fold with quantifier the: the bare definite is a description of"
            + " ONE member, not a quantification over the set — a fold over it has no reading";
    /** WF-113 (b)'s arm for a marker without a source, the verifier's sentence: the executors refuse such a marker at
     *  load, as the shared loader does when it reads for an executor. */
    static final String MARKER_ZONDER_BRON = "Unresolved marker without a source: no diagnostic message can contain it,"
            + " so it cannot be accounted for";

    /** The required datatype of a context node: anything but a datatype object is refused in the node's sentence. */
    private Datatype contextType(JsonNode n, String zin) {
        JsonNode t = n.get("type");
        if (!t.isObject()) throw new Documentfout(padVan(n) + "/type", zin);
        return typeVeld(n);
    }

    /** Assert that a ch. 4.10 Let-cluster node carries a field the shape REQUIRES (WF-076/WF-064/WF-069), naming
     *  both the form and the field. Without it a malformed document fails downstream and namelessly: a null
     *  {@code value}/{@code body}/branch throws an unattributed NPE out of the transpiler's {@code List.of}
     *  child accessor, and a missing {@code type} silently produces a node typed null, which every walk that reads
     *  a datatype then mis-reads. Fail fast, with the form and the field in the message. */
    private static <T> T verplicht(String vorm, String veld, T waarde) {
        if (waarde == null) throw new Refusal(vorm, "malformed node: missing required field '" + veld + "'");
        return waarde;
    }

    /** WF-102 (c), the one sentence for both halves of the position: a path ranges a value fold's set source and
     *  nothing else. */
    private static final String BRONPAD_C = "a sourcePath is admissible only as the range of a single-argument VALUE"
            + " fold's set source — a count's range is one step, spelled sourceRole";

    /** WF-102 (c): is this {@code Aggregation} node a single-argument instance fold — the one position a range
     *  stands at? Asked of the RAW node: a declared {@code elementType} plus exactly one argument that is a read
     *  carrying no navigation of its own (IR-SPEC ch. 4.3: with a range, that argument IS the source). */
    private static boolean isLedenVouwbron(JsonNode n) {
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
     *  ({@link #keurWaardevouwbron}). Without these the exec branch played a count over an attribute as a count of
     *  members and failed with a runtime error at first fire on a value fold over a role name, and the DRL-text
     *  branch compiled neither (measured on the L2 union). A navigated source is WF-053's and is left to the
     *  transpilers' refusal. */
    private void keurVouwbron(JsonNode n, String p, Expr.FunctieKind fk, Expr.Functie f) {
        if (fk == null || f.elementObjecttype() == null
                || !(f.argumenten().get(0) instanceof Expr.AttribuutRef bron) || navigatieBindingen.containsKey(bron)
                || bron.attribuut() == null) return;
        Objecttype el = f.elementObjecttype();
        String a = bron.attribuut(), plek = p + "/arguments/0";
        boolean attribuut = el.attribuut(a) != null, kenmerk = !attribuut && el.kenmerkNaam(a) != null;
        if (fk == Expr.FunctieKind.AANTAL) {
            if (attribuut || kenmerk)
                throw new Documentfout(plek, "the source of this count aggregation reads '" + a + "', a member of the"
                        + " element type '" + el.naam + "': a count folds INSTANCES, so its source is a count source"
                        + " naming the relation's role, never a value read per contributor (WF-093)");
            return;
        }
        if (WAARDEVOUWEN.contains(fk) && kenmerk)
            throw new Documentfout(plek, "'" + a + "' is a characteristic of '" + el.naam + "', not an attribute — a"
                    + " characteristic endpoint is ill-formed in an aggregation source position (WF-080)");
    }

    /** The value fold's set source whose name is no member of the element type, judged with the fold SUBJECT, as
     *  the verifier's source reading judges it: a name that is a role reachable from the subject — the far role of
     *  a two-role fact type the subject fills the near role of, or a reciprocal role it fills — is a count source,
     *  which only a count folds (WF-093 (a)); any other name declares nothing on the element type (WF-034), even
     *  where some other type's relation carries a role of that name. At a subject-less position (a day-kind
     *  definition) any declared role name counts, as the verifier has no subject to ask either. */
    private void keurWaardevouwbron(Expr.Functie f, Objecttype s) {
        if (!WAARDEVOUWEN.contains(f.kind()) || f.elementObjecttype() == null
                || !(f.argumenten().get(0) instanceof Expr.AttribuutRef bron) || navigatieBindingen.containsKey(bron)
                || bron.attribuut() == null) return;
        Objecttype el = f.elementObjecttype();
        String a = bron.attribuut(), plek = plekken.get(f) + "/arguments/0";
        if (el.attribuut(a) != null || el.kenmerkNaam(a) != null) return;   // read, or refused as WF-080 while reading
        if (s == null ? isRolnaam(a) : isRolnaamVanaf(s, a))
            throw new Documentfout(plek, "value aggregation over \"" + a + "\", which is no attribute of \""
                    + el.naam + "\": a value fold reads an attribute per contributor, and a role name is a count source"
                    + " only a count folds (WF-093)");
        throw new Documentfout(plek, "'" + a + "' is not a declared attribute of '" + el.naam + "': a value fold's"
                + " source reads an attribute of each contributor (WF-034)");
    }

    /** WF-083 (c), the element type of the subject's own read: a {@code total} over one source without a range is
     *  the subject's own read, so the type it folds over is the subject type — equal in the WF-001 form. Neither
     *  branch reads the field there, so a foreign type in it changed no answer: {@code jutterswet-tempototaal} with
     *  its own total naming Vondst played in both branches (measured before this check). It is refused here in the
     *  verifier's words. */
    private void keurEigenTotaal(Expr.Functie f, Objecttype s) {
        if (f.kind() != Expr.FunctieKind.TOTAAL || f.elementObjecttype() == null || s == null
                || f.argumenten().size() != 1 || rolBindingen.containsKey(f) || bronPaden.containsKey(f)) return;
        Objecttype el = f.elementObjecttype();
        if (Namen.norm(el.naam).equals(Namen.norm(s.naam))) return;
        throw new Documentfout(plekken.get(f) + "/elementType", "the elementType '" + el.naam
                + "' of a total without a range is not the fold subject '" + s.naam + "': without a range the fold is"
                + " the subject's own read, so its elementType must name the subject type");
    }

    /** The fold kinds that read a VALUE per contributor (IR-SPEC ch. 4.3) — every aggregation kind but
     *  {@code count}, which folds the contributors themselves. */
    private static final java.util.Set<Expr.FunctieKind> WAARDEVOUWEN = java.util.EnumSet.of(
            Expr.FunctieKind.SOM, Expr.FunctieKind.TOTAAL, Expr.FunctieKind.MINIMUM, Expr.FunctieKind.MAXIMUM,
            Expr.FunctieKind.EERSTE, Expr.FunctieKind.LAATSTE, Expr.FunctieKind.AANTAL_GEVULDE);

    /** Does {@code naam} name a declared role — by its declared name or its declared plural, in the WF-001 form? */
    private boolean isRolnaam(String naam) {
        for (Feittype ft : m.feittypen.values())
            for (RegelSpraakModel.Rol r : ft.rollen)
                if (noemt(r, naam)) return true;
        return false;
    }

    /** Does {@code naam} name a role reachable from {@code st}: the far role of a two-role fact type whose near role
     *  {@code st} fills (⊑ through the supertype chain, ch. 2.6), or the one role of a reciprocal fact type it
     *  fills? */
    private boolean isRolnaamVanaf(Objecttype st, String naam) {
        for (Feittype ft : m.feittypen.values()) {
            if (ft.rollen.size() == 2) {
                RegelSpraakModel.Rol a = ft.rollen.get(0), b = ft.rollen.get(1);
                if ((vult(st, a) && noemt(b, naam)) || (vult(st, b) && noemt(a, naam))) return true;
            } else if (ft.rollen.size() == 1 && ft.wederkerig) {
                RegelSpraakModel.Rol r = ft.rollen.get(0);
                if (vult(st, r) && noemt(r, naam)) return true;
            }
        }
        return false;
    }

    private boolean vult(Objecttype st, RegelSpraakModel.Rol r) {
        Objecttype vuller = ot(r.objecttypeNaam());
        return vuller != null && st.isEen(vuller);
    }

    private static boolean noemt(RegelSpraakModel.Rol r, String naam) {
        String n = Namen.norm(naam);
        return (r.naam() != null && Namen.norm(r.naam()).equals(n))
                || (r.meervoud() != null && Namen.norm(r.meervoud()).equals(n));
    }

    /** The two range spellings of a quantifier fold are one field (IR-SPEC ch. 4.5, WF-102 (a)): exactly one of
     *  them, written, never derived — refused otherwise in the shared loader's sentences. */
    private static void bereikVelden(JsonNode n, String p) {
        if (n.has("sourceRole") && n.has("sourcePath"))
            throw new Documentfout(p + "/sourcePath",
                    "sourcePath beside sourceRole: a quantifier fold carries one range, never two");
        if (!n.has("sourceRole") && !n.has("sourcePath"))
            throw new Documentfout(p, "a quantifier fold carries its range (sourceRole or sourcePath): the range is"
                    + " written, never derived");
    }

    /** A derived position whose record computes its own type — a rounding and a clamp from their base, a text
     *  composition as text: the wire's {@code type} there is still a datatype or null (IR-SPEC ch. 2.10), so it is read
     *  for its defects and not kept. An {@code Onbekend} or a scalar in it is no document, as the verifier holds;
     *  until L2's advisory round the field was not read at all. */
    private void afgeleidType(JsonNode n) {
        typeVeld(n);
    }

    /** The datatype at a node's {@code type} field — a type annotation — with that field's pointer for every defect
     *  in it. */
    private Datatype typeVeld(JsonNode n) {
        return datatype(n.get("type"), padVan(n) + "/type", false);
    }

    /** Index one {@link Expr.Let} (see {@link Loaded#lets()}) and return it — the node itself goes into the tree. */
    private Expr let(Expr.Let l) {
        lets.add(l);
        return l;
    }

    private Expr.TekstReeks.Deel tekstReeksDeel(JsonNode d) {
        String node = knoopSoort(d, DEEL, "text part");
        return switch (node) {
            case "LiteralChunk" -> new Expr.TekstReeks.Letterlijk(text(d, "text"));
            case "EmbeddedExpression" -> new Expr.TekstReeks.Ingevoegd(expr(knoopVeld(d, "expr")));
            // the two parts of the v0 shape that L5 renamed: no node of this format
            case "Letterlijk", "Ingevoegd" -> throw new Documentfout(padVan(d) + "/node",
                    "unknown TextCompositionPart node \"" + node + "\"");
            default -> throw new Refusal("TekstReeks.Deel:" + node, "unknown tekstreeks part");
        };
    }

    private Expr.DimensieSelectie dimensieSelectie(JsonNode n) {
        if (n == null || n.isNull()) return null;
        velden(n, padVan(n), "DimensionSelection", List.of("node", "dimension", "labels"), List.of("source"));
        return new Expr.DimensieSelectie(text(n, "dimension"), stringList(n.get("labels")), bron(n));
    }

    // --- Conditie ----------------------------------------------------------

    private Conditie conditie(JsonNode n) {
        if (n == null || n.isNull()) return null;
        String node = knoopSoort(n, CONDITIE, "condition");
        return switch (node) {
            // a whole-period qualifier is written whole or not at all: absent is none, the record's null
            case "Comparison" -> {
                String p = padVan(n);
                yield new Conditie.Vergelijking(
                        enumWaarde(Spelling.COMPARISON_OP, "Comparison.op", tekst(n, p, "op"), p + "/op"),
                        expr(knoopVeld(n, "left")), expr(knoopVeld(n, "right")),
                        geheelPeriode(knoopVeld(n, "wholePeriod")), bron(n));
            }
            case "Predicate" -> predicaat(n);
            // IR-SPEC ch. 4.5: the binding IS the hop — a check without one tests the subject's own kenmerk; an
            // absent host is the subject (RUNTIME-SEMANTICS ch. 5.8b), the record's null
            case "CharacteristicCheck" -> {
                String p = padVan(n);
                Conditie.Kenmerk k = new Conditie.Kenmerk(text(n, "characteristic"), null, bool(n, "negated"),
                        ot(text(n, "host")), geheelPeriode(knoopVeld(n, "wholePeriod")), bron(n));
                RolBindingRef nb = rolBinding(n, p, "navigation");
                if (nb != null) navigatieBindingen.put(k, List.of(nb));
                yield k;
            }
            // IR-SPEC ch. 4.5: one node for the role check — the fact type, the role the SUBJECT plays in it, and the
            // narrowing of the other end where there is one. The record is the carrier (see Loaded#rolBindingen).
            case "RoleMembership" -> {
                String p = padVan(n);
                // WF-034, the role check's pair crossed against the declarations, in the words ref and the ASP
                // backend fault with: a check naming no declared link holds for no instance, and until L2's fix
                // round this backend refused it with a storage reason instead (the fact type, the role, the
                // narrowing here; whether the subject fills the role in keurOnderwerpen, which has the subject)
                String feittype = tekst(n, p, "factType"), rol = tekst(n, p, "role");
                Feittype ft = feittypeOpNaam(feittype);
                if (ft == null)
                    throw new Documentfout(p + "/factType", "role check names fact type '" + feittype + "', which is"
                            + " not declared: the check resolves against no declaration (WF-034)");
                RegelSpraakModel.Rol gebonden = rolOpNaam(ft, rol);
                if (gebonden == null)
                    throw new Documentfout(p + "/role", "'" + rol + "' is not a declared role of fact type '" + feittype
                            + "' (a role check names the role by its declared name) (WF-034)");
                String tegenpartij = optTekst(n, p, "counterpartType");
                Objecttype ander = tegenpartij == null ? null : ot(tegenpartij);
                if (tegenpartij != null) {
                    if (ander == null)
                        throw new Documentfout(p + "/counterpartType", "role check: the counterpart type \""
                                + tegenpartij + "\" is no declared object type (WF-034)");
                    RegelSpraakModel.Rol overzijde = ft.wederkerig ? gebonden : andereRol(ft, gebonden);
                    Objecttype vuller = overzijde == null ? null : ot(overzijde.objecttypeNaam());
                    if (vuller != null && !ander.isEen(vuller))
                        throw new Documentfout(p + "/counterpartType", "role check: the counterpart type \""
                                + tegenpartij + "\" is no subtype of \"" + vuller.naam + "\", the filler the role \""
                                + overzijde.naam() + "\" declares (WF-034)");
                }
                Conditie.Rollidmaatschap r = new Conditie.Rollidmaatschap(rol, ander, bron(n),
                        waarheid(n, p, "negated"));
                rolBindingen.put(r, new RolBindingRef(feittype, r.rol()));
                plekken.put(r, p);
                yield r;
            }
            // the two role checks of the L1 shape, which RoleMembership replaced: nodes this format no longer has
            case "Rollidmaatschap", "VerreRollidmaatschap" -> throw new Documentfout(padVan(n) + "/node",
                    "unknown condition node \"" + node + "\": uchar-ir-v1 spells a role check as RoleMembership");
            // a bound is written whole or not at all: absent is open on that side, the record's null
            case "TimeWindow" -> new Conditie.Periode(expr(knoopVeld(n, "from")), expr(knoopVeld(n, "to")),
                    bool(n, "toInclusive"), bron(n));
            case "Compound" -> {
                String p = padVan(n);
                List<Conditie> delen = new ArrayList<>();
                elementen(n, "parts", o -> delen.add(conditie(o)));
                yield new Conditie.Samengesteld(
                        enumWaarde(Spelling.QUANTIFIER, "Compound.quantifier", tekst(n, p, "quantifier"),
                                p + "/quantifier"),
                        intOf(n, "threshold"), delen, bron(n));
            }
            // IR-SPEC ch. 4.5 / WF-108 the QUANTIFIER FOLD. Unlike Compound — whose quantifier ranges over the
            // BULLETS of one subject — this one ranges over INSTANCES, and its range fields are the instance fold's
            // (ch. 4.3) spelled unchanged. Both range fields are decoded INTO the record: Kwantorvouw is not part
            // of the vendored frontend slice the function/object-creation side-channel exists for, so its sourceRole
            // needs no Loaded#rolBindingen entry.
            case "QuantifierFold" -> {
                // WF-108 (c), the WF-102 discipline under this node's own rule. Refused for exactly the reason
                // Aggregation.sourcePath is: the model cannot re-derive a multi-step range, and the by-pair fallback would
                // bind a coincidental DIRECT subject-to-element relation and quantify over the WRONG set while the
                // OCaml executors quantify over the right one. A named per-form Refusal puts the golden in gate 12's
                // channel floor by reason instead.
                // IR-SPEC ch. 4.5: the range is required, one field with two spellings
                String p = padVan(n);
                bereikVelden(n, p);
                if (n.has("sourcePath")) {
                    rolBindingLijst(n, p, "sourcePath");   // its shape first: a malformed path is no document
                    throw new Refusal("Kwantorvouw.bronPad",
                            "a multi-step quantifier range (WF-108 (c)); the accumulate source carries one join role");
                }
                // WF-108 (a): all, none and the three COUNTING quantifiers are the admitted forms. The value the
                // shares the one quantifier family with Compound, where it is admitted, and is refused here BY NAME,
                // in the verifier's sentence, as the shared loader refuses it for an executor — the bare definite
                // describes one member, and reading it as a forall is the misreading this node exists to prevent. Any
                // other value outside the family is an unknown value.
                String wq = tekst(n, p, "quantifier");
                if (Spelling.QUANTIFIER.weigertBijNaam("QuantifierFold.quantifier", wq))
                    throw new Documentfout(p + "/quantifier", KWANTORVOUW_THE);
                Conditie.Kwantor kw = enumWaarde(Spelling.QUANTIFIER, "QuantifierFold.quantifier", wq,
                        p + "/quantifier");
                // WF-108 (e): the threshold is present EXACTLY under a counting quantifier. Read as an absence rather
                // than as a 0 — a counting fold whose threshold is missing has no verdict, and guessing one here
                // would answer a question the document does not ask.
                boolean telt = kw == Conditie.Kwantor.TEN_MINSTE || kw == Conditie.Kwantor.TEN_HOOGSTE
                        || kw == Conditie.Kwantor.PRECIES;
                if (telt != n.has("threshold"))
                    throw new Refusal("Kwantorvouw.drempel", "a threshold is written exactly under a counting"
                            + " quantifier (WF-108 (e)); this node has quantifier " + wq
                            + (telt ? " and no threshold" : " and one"));
                // a subselection is written whole or not at all: absent is none, the record's null
                yield new Conditie.Kwantorvouw(kw, telt ? intOf(n, "threshold") : 0,
                        ot(text(n, "elementType")),
                        conditie(knoopVeld(n, "filter")), conditie(knoopVeld(n, "test")),
                        rolBinding(n, p, "sourceRole"), List.of(), bron(n));
            }
            case "Regelversie" -> new Conditie.Regelversie(text(n, "regel"), bool(n, "inconsistent"), bron(n));
            case "Unresolved" -> {
                if (!n.has("source")) throw new Documentfout(padVan(n), MARKER_ZONDER_BRON);
                yield new Conditie.Onopgelost(bron(n), null);   // frontend-internal field, absent from the wire
            }
            // the condition nodes of the v0 shape that L5 renamed: no node of this format
            case "Vergelijking", "Predicaat", "Kenmerk", "Periode", "Samengesteld", "Kwantorvouw", "Onopgelost" ->
                    throw new Documentfout(padVan(n) + "/node", "unknown condition node \"" + node + "\"");
            default -> throw new Refusal("Conditie:" + node, "unknown condition node");
        };
    }

    /** A negated leeg/gevuld test is lowered HERE to its sister soort, unnegated: pointwise "niet leeg" IS "gevuld"
     *  (ref: {@code not is_empty} against {@code v <> Empty}). Both transpiler branches read the leeg/gevuld soort at
     *  a dozen sites and none of them reads {@code negatie} — the RegelSpraak frontend never writes it on these two
     *  soorten — so answering it once at the wire boundary covers every site, own and navigated, in both branches.
     *  {@code negatie} is the INNER negation, per day; the qualifier's own "niet gedurende …" is
     *  {@code GeheelPeriode.niet} and is untouched here. Under the ch. 3.9 whole-period qualifier the swap stays
     *  unanswered: ch. 3.9 gives the leeg test the touched-days threshold and every other test days(B), and which
     *  of the two a NEGATED leeg test takes is fixed nowhere — under days(B) the swap would be exact, under
     *  touched days it would not. ref faults on every whole-period leeg/gevuld test, so this backend refuses the
     *  negated one by name rather than pick a threshold the norm has not chosen.
     *
     *  <p>The UNNEGATED whole-period leeg/gevuld test is normed (touched days for leeg, days(B) for gevuld), but
     *  ref faults on it and the ASP backend refuses it in ref's words, so no executor pins what an answer should
     *  be; both branches used to play it. It is refused here too, under its own key, until ref executes the fold. */
    private Conditie.Predicaat predicaat(JsonNode n) {
        String p = padVan(n);
        Conditie.Predicaatsoort soort = enumWaarde(Spelling.PREDICATE_KIND, "Predicate.kind", tekst(n, p, "kind"),
                p + "/kind");
        boolean negatie = bool(n, "negated");
        Conditie.GeheelPeriode periode = geheelPeriode(knoopVeld(n, "wholePeriod"));
        boolean leegOfGevuld = soort == Conditie.Predicaatsoort.LEEG || soort == Conditie.Predicaatsoort.GEVULD;
        if (leegOfGevuld && periode != null && !negatie)
            throw new Refusal("Predicaat.periode",
                    "a leeg/gevuld test under a whole-period qualifier (ch. 3.9): ref executes neither the leeg fold (touched days) nor the gevuld fold (days(B)), so this backend does not answer the form either");
        if (leegOfGevuld && negatie) {
            if (periode != null)
                throw new Refusal("Predicaat.negatie",
                        "a negated leeg/gevuld test under a whole-period qualifier (ch. 3.9): the threshold of the negated test is not normed");
            soort = soort == Conditie.Predicaatsoort.LEEG ? Conditie.Predicaatsoort.GEVULD : Conditie.Predicaatsoort.LEEG;
            negatie = false;
        }
        // the day kind is written on a predicate of kind dayKind alone: absent elsewhere, the record's null
        return new Conditie.Predicaat(soort, expr(knoopVeld(n, "operand")), text(n, "dayKind"), negatie,
                intOf(n, "digits"),
                periode, bron(n));
    }

    /** IR-SPEC ch. 4.5: the whole-period qualifier, its granularity read onto the record's {@code jaar} flag (year
     *  true, month false). */
    private Conditie.GeheelPeriode geheelPeriode(JsonNode n) {
        if (n == null || n.isNull()) return null;
        String p = padVan(n);
        velden(n, p, "WholePeriod", List.of("node", "granularity", "negated"), List.of());
        return new Conditie.GeheelPeriode(enumWaarde(Spelling.WHOLE_PERIOD_GRANULARITY, "WholePeriod.granularity",
                tekst(n, p, "granularity"), p + "/granularity"), bool(n, "negated"));
    }

    // --- resolved IR lists -------------------------------------------------

    /** One version of a named form: the name its record carries, the JSON pointer of the version, and the version
     *  record itself. */
    private record Versie(String naam, String pad, JsonNode knoop) {}

    /**
     * IR-SPEC ch. 3, Versioning (delta B19): a named form is {@code {node, name, versions}}, and the frontend's
     * records hold one entry per version. A form with two or more versions names them {@code "<name> #<i>"}, i from
     * 1 in wire order, as the frontend's resolver names them; a form with one keeps its bare name; a null name
     * (WF-107's) stays null on every version. That spelling is why a form's own name may not end in a space, '#'
     * and digits: it would read as a version of another form. A form carries at least one version.
     */
    private List<Versie> vorm(JsonNode n, String pad, String knoop, List<String> optioneel) {
        velden(n, pad, knoop, List.of("node", "name", "versions"), optioneel);
        JsonNode nm = n.get("name");
        if (!nm.isNull() && !nm.isTextual())
            throw new Documentfout(pad + "/name", "expected a string or null, got " + kort(nm));
        String naam = nm.isNull() ? null : nm.asText();
        if (naam != null && VERSIENAAM.matcher(naam).matches()) throw new Documentfout(pad + "/name", VORMNAAM);
        String vp = pad + "/versions";
        JsonNode vs = n.get("versions");
        if (!vs.isArray()) throw new Documentfout(vp, "expected an array, got " + kort(vs));
        if (vs.isEmpty()) throw new Documentfout(vp, VERSIES_LEEG);
        List<Versie> uit = new ArrayList<>();
        for (int i = 0; i < vs.size(); i++) {
            JsonNode v = vs.get(i);
            if (!v.isObject()) throw new Documentfout(vp + "/" + i, "expected a JSON object, got " + kort(v));
            uit.add(new Versie(naam != null && vs.size() >= 2 ? naam + " #" + (i + 1) : naam, vp + "/" + i, v));
        }
        return uit;
    }

    /** A name ending in a space, '#' and digits: the in-memory spelling of one version of a form with several. */
    private static final java.util.regex.Pattern VERSIENAAM = java.util.regex.Pattern.compile("[\\s\\S]* #[0-9]+");

    /** The shared loader's sentences for a form's name and its versions. */
    static final String VORMNAAM = "a form's name does not end in a space, '#' and digits: that spelling names one"
            + " version of a form with several";
    static final String VERSIES_LEEG = "a form carries at least one version";

    /** The key set of a version record, which carries no node discriminator: {@link #velden} without the node, and an
     *  unknown field named on the record's own kind. */
    private static void versievelden(JsonNode n, String pad, String soort, List<String> verplicht,
                                     List<String> optioneel) {
        n.fieldNames().forEachRemaining(k -> {
            if (!verplicht.contains(k) && !optioneel.contains(k))
                throw new Documentfout(pad + "/" + k, "unknown field on " + soort);
        });
        for (String k : verplicht)
            if (!n.has(k)) throw ontbreekt(pad, k);
        for (String k : optioneel)
            if (n.has(k) && n.get(k).isNull())
                throw new Documentfout(pad + "/" + k, "an optional field is written whole or not at all, and null is neither");
    }

    /** A field that holds a node, required: a scalar or null there is refused at the field's pointer. */
    private static JsonNode knoop(JsonNode ouder, String pad, String veld) {
        JsonNode c = ouder.get(veld);
        if (!c.isObject()) throw new Documentfout(pad + "/" + veld, "expected a JSON object, got " + kort(c));
        return c;
    }

    /** A field whose value is a string or null (IR-SPEC § Method: a name a WF rule guards is nullable). */
    private static String tekstOfNull(JsonNode n, String pad, String veld) {
        JsonNode v = n.get(veld);
        if (v.isNull()) return null;
        if (!v.isTextual()) throw new Documentfout(pad + "/" + veld, "expected a string or null, got " + kort(v));
        return v.asText();
    }

    /** IR-SPEC ch. 3.10: the validity of a version, always written; each bound is written whole or not at all, an
     *  absent one open on that side, and a Validity with neither bound is always. A malformed bound is named by its
     *  JSON pointer. */
    private Geldigheid validity(JsonNode versie, String pad) {
        JsonNode g = knoop(versie, pad, "validity");
        String gp = pad + "/validity";
        velden(g, gp, "Validity", List.of("node"), List.of("from", "to"));
        String vanaf = optTekst(g, gp, "from"), tot = optTekst(g, gp, "to");
        return new Geldigheid(vanaf == null ? null : datumVanIrWire(vanaf, gp + "/from"),
                tot == null ? null : datumVanIrWire(tot, gp + "/to"));
    }

    /** IR-SPEC ch. 3.1: a rule and its versions; the statute annotations stand on the form, and every version's
     *  record carries them. */
    private void rule(String pad, JsonNode n) {
        List<Versie> versies = vorm(n, pad, "Rule", List.of("statuteAnnotations"));
        List<String> annotaties = n.has("statuteAnnotations") ? tekstLijst(n, pad, "statuteAnnotations") : List.of();
        for (Versie v : versies) m.resolvedRegels.add(ruleVersion(v, annotaties));
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
    private ResolvedRegel ruleVersion(Versie versie, List<String> annotaties) {
        JsonNode n = versie.knoop();
        String pad = versie.pad();
        versievelden(n, pad, "RuleVersion", List.of("validity", "target", "value", "initialisation"),
                List.of("condition", "presentation"));
        boolean initialisatie = waarheid(n, pad, "initialisation");
        String presentatie = optTekst(n, pad, "presentation");
        ResolvedRegel.Soort getagd = presentatie == null ? null : enumWaarde(Spelling.PRESENTATION,
                "RuleVersion.presentation", presentatie, pad + "/presentation");
        if (initialisatie && presentatie != null)
            throw new Documentfout(pad + "/presentation", PRESENTATIE_BIJ_INITIALISATIE);
        ResolvedRegel.Soort soort = initialisatie ? ResolvedRegel.Soort.INITIALISATIE
                : getagd == null ? ResolvedRegel.Soort.KENMERKTOEKENNING : getagd;
        JsonNode t = n.get("target");
        if (!t.isObject() || !"AttributeReference".equals(text(t, "node")))
            throw new Documentfout(pad + "/target", DOEL_ATTRIBUUTREF);
        Expr.AttribuutRef doel = (Expr.AttribuutRef) expr(t);
        keurLedensoort(doel, text(t, "subjectType"), soort, pad + "/target");
        ResolvedRegel r = new ResolvedRegel(versie.naam(), soort, doel, expr(knoop(n, pad, "value")),
                conditie(knoopVeld(n, "condition")), validity(n, pad), annotaties);
        // WF-022's value clause: a characteristic attribution writes the boolean constant true. Neither branch
        // reads the value — the exec-model one writes true whatever it says, the DRL-text one writes what it
        // says — so on a false (measured 2026-09-23 on the negatief wf022-kenmerk-onwaar, verifier bypassed)
        // they gave opposite answers where the ASP backend refuses. Refused here, once for both branches.
        if (soort == ResolvedRegel.Soort.KENMERKTOEKENNING
                && !(r.waarde() instanceof Expr.Literal l && "waar".equals(l.waarde())
                     && l.type() instanceof Datatype.Booleaan)) {
            String waarde = r.waarde() instanceof Expr.Literal l ? "'" + l.waarde() + "'"
                    : "een " + r.waarde().getClass().getSimpleName();
            throw new Refusal("RuleVersion.value", "de kenmerktoekenning '" + r.naam() + "' kent het kenmerk '"
                    + doel.attribuut() + "' " + waarde + " toe; WF-022 eist de booleaanse constante waar, en een"
                    + " negatief besluit over een kenmerk bestaat alleen in een defaultsgroep");
        }
        return r;
    }

    /** The shared loader's sentences for a rule version's kind and target. */
    static final String PRESENTATIE_BIJ_INITIALISATIE = "presentation beside initialisation true: a presentation tags"
            + " a computing version, never an initialisation";
    static final String DOEL_ATTRIBUUTREF = "the target of a rule version is an AttributeReference";

    /** WF-022, the member-kind arm and the name that is both (IR-SPEC ch. 3.1): the verifier's sentences, which
     *  name the member and the type as the target writes them. A target whose type or member does not resolve is
     *  left to the rule that reports it. */
    private void keurLedensoort(Expr.AttribuutRef doel, String typeNaam, ResolvedRegel.Soort soort, String plek) {
        Objecttype ot = doel.objecttype();
        String naam = doel.attribuut();
        if (ot == null || naam == null) return;
        boolean attribuut = false, kenmerk = false;
        for (Objecttype t : ot.keten()) {
            for (String a : t.attributen.keySet()) attribuut |= Namen.norm(a).equals(Namen.norm(naam));
            for (String k : t.kenmerken) kenmerk |= Namen.norm(k).equals(Namen.norm(naam));
        }
        if (attribuut && kenmerk) throw new Documentfout(plek, doelTweeledig(naam, typeNaam));
        if (soort == ResolvedRegel.Soort.KENMERKTOEKENNING && attribuut)
            throw new Documentfout(plek, kenmerkdoel(naam, typeNaam));
        if (soort != ResolvedRegel.Soort.KENMERKTOEKENNING && kenmerk)
            throw new Documentfout(plek, attribuutdoel(naam, typeNaam));
    }

    /** WF-022's two member-kind sentences and the one for a name that is both. */
    static String kenmerkdoel(String naam, String type) {
        return "a version without a presentation and without initialisation attributes a characteristic, so its"
                + " target MUST name one, and '" + naam + "' is an attribute of '" + type + "'";
    }
    static String attribuutdoel(String naam, String type) {
        return "a version with a presentation or with initialisation true writes an attribute, so its target MUST"
                + " name one, and '" + naam + "' is a characteristic of '" + type + "'";
    }
    static String doelTweeledig(String naam, String type) {
        return "target '" + naam + "' names both an attribute and a characteristic of '" + type + "': which member a"
                + " rule writes decides its kind, and this name decides nothing";
    }

    /** IR-SPEC ch. 3.3: an object creation and its versions; the back-link is optional. */
    private void objectCreation(String pad, JsonNode n) {
        for (Versie v : vorm(n, pad, "ObjectCreation", List.of())) {
            JsonNode k = v.knoop();
            String p = v.pad();
            versievelden(k, p, "ObjectCreationVersion", List.of("createdType", "subject", "initialisations", "validity"),
                    List.of("condition", "connectingRole"));
            List<ResolvedObjectCreatie.Toekenning> tk = new ArrayList<>();
            lijst(k, p, "initialisations", (tp, t) -> {
                velden(t, tp, "MemberInit", List.of("node", "member", "value", "characteristic"), List.of());
                tk.add(new ResolvedObjectCreatie.Toekenning(text(t, "member"), expr(knoopVeld(t, "value")),
                        bool(t, "characteristic")));
            });
            ResolvedObjectCreatie oc = new ResolvedObjectCreatie(v.naam(), ot(tekstOfNull(k, p, "createdType")),
                    ot(tekstOfNull(k, p, "subject")), tk, conditie(knoopVeld(k, "condition")), validity(k, p));
            // IR-SPEC ch. 3.3: the symbolic connectingRole is NOT a field of the (vendored, pinned)
            // ResolvedObjectCreatie record, so it cannot be reconstructed INTO the model the way a fact creation's
            // path can. ch. 5.5 makes it AUTHORITATIVE (the reference interpreter lets it win over the type-pair
            // derivation, and it is the only route for a self-typed pair), so it rides BESIDE the model.
            RolBindingRef vr = rolBinding(k, p, "connectingRole");
            if (vr != null) rolBindingen.put(oc, vr);
            m.objectCreaties.add(oc);
        }
    }

    /** IR-SPEC ch. 3.2: an allocation and its versions: one of the three methods, the optional fields absent when
     *  none, and the recipient join required. */
    private void allocation(String pad, JsonNode n) {
        for (Versie v : vorm(n, pad, "Allocation", List.of())) {
            JsonNode k = v.knoop();
            String p = v.pad();
            versievelden(k, p, "AllocationVersion", List.of("sourceType", "sourceAttribute", "targetType",
                    "targetAttribute", "method", "descending", "validity", "connectingRole"), List.of("ratioAttribute",
                    "maximumAttribute", "roundingDecimals", "remainderAttribute", "orderingAttribute", "condition"));
            ResolvedVerdeling.Methode methode = enumWaarde(Spelling.METHOD, "AllocationVersion.method",
                    tekst(k, p, "method"), p + "/method");
            ResolvedVerdeling vd = new ResolvedVerdeling(v.naam(), ot(tekstOfNull(k, p, "sourceType")),
                    tekstOfNull(k, p, "sourceAttribute"), ot(tekstOfNull(k, p, "targetType")),
                    tekstOfNull(k, p, "targetAttribute"), methode,
                    optTekst(k, p, "ratioAttribute"), optTekst(k, p, "maximumAttribute"),
                    k.has("roundingDecimals") ? geheel(k, p, "roundingDecimals", "AllocationVersion.roundingDecimals")
                            : null,
                    optTekst(k, p, "remainderAttribute"), optTekst(k, p, "orderingAttribute"),
                    waarheid(k, p, "descending"), conditie(knoopVeld(k, "condition")), validity(k, p));
            // IR-SPEC ch. 3.2: the recipient role binding IS the join, and it is required. Not a field of the
            // vendored record, so it rides beside the model like the object creation's; see Loaded#rolBindingen.
            RolBindingRef vr = rolBinding(k, p, "connectingRole");
            if (vr == null) throw new Documentfout(p + "/connectingRole", "expected a RoleBinding, got null");
            rolBindingen.put(vd, vr);
            m.verdelingen.add(vd);
        }
    }

    /** IR-SPEC ch. 3.5: a consistency rule and its versions; the source record, with its line, stands on the
     *  version. */
    private void consistency(String pad, JsonNode n) {
        for (Versie v : vorm(n, pad, "Consistency", List.of())) {
            JsonNode k = v.knoop();
            String p = v.pad();
            versievelden(k, p, "ConsistencyVersion", List.of("subject", "assertion", "validity"), List.of("source"));
            m.consistenties.add(new ResolvedConsistentie(v.naam(), ot(tekstOfNull(k, p, "subject")),
                    conditie(knoop(k, p, "assertion")), bron(k), bronregel(k), validity(k, p)));
        }
    }

    /** IR-SPEC ch. 3.6 (delta B27): a uniqueness and its versions. A version with one source is the single-type
     *  form and a record of the model's first list, with two or more the union and a record of its second, which
     *  carries no scope (a scoped union is an open design question). The order of the wire's one list carries no
     *  meaning. */
    private void uniqueness(String pad, JsonNode n) {
        for (Versie v : vorm(n, pad, "Uniqueness", List.of())) {
            JsonNode k = v.knoop();
            String p = v.pad();
            versievelden(k, p, "UniquenessVersion", List.of("sources", "validity"), List.of("scope", "source"));
            List<ResolvedUniciteitUnie.Bron> bronnen = new ArrayList<>();
            lijst(k, p, "sources", (sp, s) -> {
                velden(s, sp, "Source", List.of("node", "objectType", "keys"), List.of());
                String kp = sp + "/keys";
                JsonNode keys = s.get("keys");
                if (!keys.isArray()) throw new Documentfout(kp, "expected an array, got " + kort(keys));
                if (keys.isEmpty()) throw new Documentfout(kp, SLEUTELS_LEEG);
                List<String> sleutels = new ArrayList<>();
                for (int i = 0; i < keys.size(); i++) sleutels.add(tekstOfNull(keys, kp, i));
                bronnen.add(new ResolvedUniciteitUnie.Bron(ot(tekstOfNull(s, sp, "objectType")), sleutels));
            });
            if (bronnen.isEmpty()) throw new Documentfout(p + "/sources", BRONNEN_LEEG);
            Geldigheid geldigheid = validity(k, p);
            if (bronnen.size() == 1) {
                // IR-SPEC ch. 3.6: the scope binding IS the scoping; the record's field-name component stays empty
                ResolvedUniciteitUnie.Bron b = bronnen.get(0);
                m.uniciteiten.add(new ResolvedUniciteit(v.naam(), b.type(), b.sleutels(), null, bron(k), geldigheid,
                        rolBinding(k, p, "scope")));
            } else {
                if (k.has("scope")) throw new Documentfout(p + "/scope", GESCOOPTE_UNIE);
                m.uniciteitUnies.add(new ResolvedUniciteitUnie(v.naam(), bronnen, bron(k), geldigheid));
            }
        }
    }

    /** The shared loader's sentences for the sources of a uniqueness version. */
    static final String BRONNEN_LEEG = "a uniqueness version carries at least one source";
    static final String SLEUTELS_LEEG = "a source carries at least one key";
    static final String GESCOOPTE_UNIE = "a uniqueness version with two or more sources carries no scope: a scoped"
            + " union is an open design question";

    /** An entry of a list of strings or nulls. */
    private static String tekstOfNull(JsonNode lijst, String pad, int i) {
        JsonNode v = lijst.get(i);
        if (v.isNull()) return null;
        if (!v.isTextual()) throw new Documentfout(pad + "/" + i, "expected a string or null, got " + kort(v));
        return v.asText();
    }

    /**
     * IR-SPEC ch. 3.4: a fact creation and its versions. The creation IS its symbolic block — the created relation,
     * both ends, and the path from the anchor, one binding per hop — and each part is required. The record's
     * bean-field components stay empty; the transpilers derive the fields from the bindings
     * ({@code FactModelGenerator.creatievelden}). Every hop of the path but the last is a step, the last is the join.
     * The target type is not written: it is the declared filler of the target role in the created fact type, found
     * as the verifier finds it, and where that does not resolve the version is refused in the verifier's WF-028
     * words — a record without a target type would join to nothing. The anchor role is resolved the same way and
     * refused in the same words: the record keeps it by name, and a name that is no role of the created relation
     * reached both branches as a creation they could only call unresolved.
     */
    private void factCreation(String pad, JsonNode n) {
        for (Versie v : vorm(n, pad, "FactCreation", List.of())) {
            JsonNode k = v.knoop();
            String p = v.pad();
            versievelden(k, p, "FactCreationVersion", List.of("factType", "anchorRole", "targetRole", "anchor", "path",
                    "validity"), List.of("condition"));
            List<RolBindingRef> hops = rolBindingLijst(k, p, "path");
            if (hops.isEmpty()) throw new Documentfout(p + "/path", PAD_LEEG);
            List<ResolvedFeitCreatie.Tussenstap> ts = new ArrayList<>();
            for (RolBindingRef rb : hops.subList(0, hops.size() - 1))
                ts.add(new ResolvedFeitCreatie.Tussenstap(null, null, false, rb));
            String feittype = tekst(k, p, "factType"), ankerRol = tekst(k, p, "anchorRole"),
                    doelRol = tekst(k, p, "targetRole");
            m.feitCreaties.add(new ResolvedFeitCreatie(v.naam(), ot(tekstOfNull(k, p, "anchor")),
                    doelsoort(p, feittype, ankerRol, doelRol), null, null, ts, conditie(knoopVeld(k, "condition")),
                    validity(k, p), feittype, ankerRol, doelRol, hops.get(hops.size() - 1)));
        }
    }

    /** The shared loader's sentence for an empty path. */
    static final String PAD_LEEG = "a fact creation's path carries at least one hop";

    /** The target type of a fact creation: the declared filler of the target role in the created fact type, under
     *  the verifier's name identity. What does not resolve is refused, in WF-028's sentences, the anchor role first
     *  as the verifier reports it. */
    private Objecttype doelsoort(String p, String feittype, String ankerRol, String doelRol) {
        Feittype ft = feittypeOpNaam(feittype);
        if (ft == null)
            throw new Documentfout(p + "/factType", "created fact type '" + feittype + "' is not declared");
        if (ft.rollen.size() != 2)
            throw new Documentfout(p + "/factType", "created fact type '" + feittype + "' does not have exactly two"
                    + " roles");
        rolVan(p + "/anchorRole", ft, feittype, ankerRol);
        RegelSpraakModel.Rol doel = rolVan(p + "/targetRole", ft, feittype, doelRol);
        Objecttype vuller = ot(doel.objecttypeNaam());
        if (vuller == null)
            throw new Documentfout(p + "/targetRole", "the role '" + doelRol + "' of '" + feittype + "' is filled by '"
                    + doel.objecttypeNaam() + "', which is no declared object type: a fact creation's target"
                    + " type is the filler of its target role");
        return vuller;
    }

    /** The role {@code rol} of the created fact type {@code ft} under the verifier's name identity, or WF-028's
     *  refusal at {@code pad}. */
    private static RegelSpraakModel.Rol rolVan(String pad, Feittype ft, String feittype, String rol) {
        List<RegelSpraakModel.Rol> rollen = ft.rollen.stream()
                .filter(r -> r.naam() != null && Namen.norm(r.naam()).equals(Namen.norm(rol))).toList();
        if (rollen.isEmpty())
            throw new Documentfout(pad, "'" + rol + "' is not a role of '" + feittype + "'");
        if (rollen.size() > 1)
            throw new Documentfout(pad, "role '" + rol + "' is ambiguous within '" + feittype + "'");
        return rollen.get(0);
    }

    /** The RoleBinding field {@code veld} of the object at {@code pad}: absent or null is null (the caller decides
     *  whether that is allowed), anything else is a {@code RoleBinding} object with exactly its two string fields. */
    private static RolBindingRef rolBinding(JsonNode ouder, String pad, String veld) {
        JsonNode n = ouder.get(veld);
        if (n == null || n.isNull()) return null;
        return rolBindingOp(n, pad + "/" + veld);
    }

    private static RolBindingRef rolBindingOp(JsonNode n, String pad) {
        velden(n, pad, "RoleBinding", List.of("node", "factType", "role"), List.of());
        return new RolBindingRef(tekst(n, pad, "factType"), tekst(n, pad, "role"));
    }

    /** A list of RoleBindings at field {@code veld} of the object at {@code pad}. */
    private static List<RolBindingRef> rolBindingLijst(JsonNode ouder, String pad, String veld) {
        List<RolBindingRef> uit = new ArrayList<>();
        lijst(ouder, pad, veld, (sp, s) -> uit.add(rolBindingOp(s, sp)));
        return uit;
    }

    /** The validity window of {@code eigenaar} — a day-kind definition or a defaults group, the two carriers outside
     *  the named forms, whose validity may be null (every version of a named form carries its {@code validity}, see
     *  {@link #validity}) — read from ITS node, not from a detached child, so a malformed bound can be reported
     *  against the declaration that carries it: "from" alone does not locate the defect among the hundreds of windows
     *  a document holds. This mirrors {@link #bron(JsonNode)} one method below, which reads its own field the same
     *  way.
     *  <p>{@code naamVeld} is the carrier's name field ({@code dayKind} on a day-kind definition, {@code name} on a
     *  defaults group). The prefix falls back to the NODE KIND where a carrier has none: that field is
     *  {@code ["string","null"]} on both carrying node kinds, so a nameless carrier is a legal wire document, and a
     *  bare "validity.from" would locate nothing. */
    private Geldigheid geldigheid(JsonNode eigenaar, String naamVeld) {
        JsonNode n = knoopVeld(eigenaar, "validity");
        if (n == null) return null;
        String gp = padVan(n);
        velden(n, gp, "Validity", List.of("node"), List.of("from", "to"));
        String naam = text(eigenaar, naamVeld), soort = text(eigenaar, "node");
        String plek = (naam != null ? naam : "<unnamed " + (soort == null ? "carrier" : soort) + ">")
                + ".validity";
        String vanaf = optTekst(n, gp, "from"), tot = optTekst(n, gp, "to");
        return new Geldigheid(vanaf == null ? null : datumVanIrWire(vanaf, plek + ".from"),
                tot == null ? null : datumVanIrWire(tot, plek + ".to"));
    }

    /* The uchar-ir-v1 validity-bound lexeme, matched WHOLE: four-two-two digit fields, digits only, no
     * sign and no extended year. It is WF-084's date-literal grammar (IR-SPEC ch. 4.1) applied to the other
     * date position on this wire — where, unlike the case wire, the schema carried no pattern at all. */
    private static final java.util.regex.Pattern IR_DATUM =
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
    private static java.time.LocalDate datumVanIrWire(String lexeem, String veld) {
        if (!IR_DATUM.matcher(lexeem).matches())
            throw new IllegalArgumentException("malformed validity date '" + lexeem + "' on '" + veld
                    + "': the uchar-ir-v1 lexeme is yyyy-mm-dd — fixed field widths, digits only,"
                    + " no sign and no extended year");
        try { return java.time.LocalDate.parse(lexeem); }
        catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("malformed validity date '" + lexeem + "' on '" + veld
                    + "': the field widths are right but the calendar is not (" + e.getMessage() + ")");
        }
    }

    // --- helpers -----------------------------------------------------------

    private Objecttype ot(String naam) { return naam == null ? null : m.objecttypeOpNaam(naam); }
    private Parameter param(String naam) { return naam == null ? null : m.parameters.get(naam); }

    /** IR-SPEC ch. 4 (B26): the source record of {@code n}, null where the node carries none. Present, it is an
     *  object with its snippet — a string, never empty — and an optional line of at least 1, and nothing beside
     *  them: a file or a column is an unknown field until a card writes positions. Every other form is refused at
     *  its pointer. A line is read wherever it stands; the two records with a place for it keep it
     *  ({@link #bronregel}), and every other node drops it. */
    private Bronrecord bronrecord(JsonNode n) {
        JsonNode s = n.get("source");
        if (s == null) return null;
        String p = padVan(n) + "/source";
        velden(s, p, null, List.of("snippet"), List.of("line"));
        String snippet = tekst(s, p, "snippet");
        if (snippet.isEmpty()) throw new Documentfout(p + "/snippet", SNIPPET_LEEG);
        int regel = 0;
        if (s.has("line")) {
            regel = geheel(s, p, "line", "source.line");
            if (regel < 1) throw new Documentfout(p + "/line", BRONREGEL_ONDER_1);
        }
        return new Bronrecord(snippet, regel);
    }

    /** A source record as this loader holds it: the snippet, and the line or 0 where the record has none. */
    private record Bronrecord(String snippet, int regel) {}

    /** The shared loader's sentence: an empty snippet has no reading, as a node without source text carries no
     *  record. */
    static final String SNIPPET_LEEG = "a source snippet is never empty: a node without source text carries no source";
    /** The shared loader's sentence for a line below 1, on every node. */
    static final String BRONREGEL_ONDER_1 = "a source line is at least 1";

    /** The anchor a node's record holds: its snippet, and "" where it carries no source record — the value the
     *  record held for a node without source text before the source record landed. */
    private Bron bron(JsonNode n) {
        Bronrecord b = bronrecord(n);
        return Bron.synthetisch(b == null ? "" : b.snippet());
    }

    /** The source text of a node of the defaults layer, whose records keep it as a string: null where it carries
     *  no source record. */
    private String bronTekst(JsonNode n) {
        Bronrecord b = bronrecord(n);
        return b == null ? null : b.snippet();
    }

    /** The source line of a consistency version or a rule group, the two records with a place for one: 0 where the
     *  source record has none or the node carries no record, the records' value for "no line". */
    private int bronregel(JsonNode n) {
        Bronrecord b = bronrecord(n);
        return b == null ? 0 : b.regel();
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

    private static Integer integerOf(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v == null || v.isNull() ? null : v.asInt();
    }

    private static List<String> stringList(JsonNode n) {
        List<String> uit = new ArrayList<>();
        if (n != null && n.isArray()) n.forEach(e -> uit.add(e.asText()));
        return uit;
    }

    private static void forEach(JsonNode array, java.util.function.Consumer<JsonNode> f) {
        if (array != null && array.isArray()) array.forEach(f);
    }

    /** The node at {@code veld} of {@code ouder}: null when the field is absent or null, and a scalar or an array
     *  there refused at the field's own pointer. A node is found by identity ({@link #padVan}), which a scalar does
     *  not have — Jackson shares small numbers, booleans and null — so its place is the parent's and the field's. */
    private JsonNode knoopVeld(JsonNode ouder, String veld) {
        JsonNode c = ouder.get(veld);
        if (c == null || c.isNull()) return null;
        if (!c.isObject()) throw new Documentfout(padVan(ouder) + "/" + veld, "expected a JSON object, got " + kort(c));
        return c;
    }

    /** Each node of the list at {@code veld} of {@code ouder}, in order; an absent list is empty, and an entry that is
     *  not an object is refused at its own pointer (see {@link #knoopVeld}). */
    private void elementen(JsonNode ouder, String veld, java.util.function.Consumer<JsonNode> f) {
        JsonNode a = ouder.get(veld);
        if (a == null || !a.isArray()) return;
        for (int i = 0; i < a.size(); i++) {
            JsonNode e = a.get(i);
            if (!e.isObject()) throw new Documentfout(padVan(a) + "/" + i, "expected a JSON object, got " + kort(e));
            f.accept(e);
        }
    }
}
