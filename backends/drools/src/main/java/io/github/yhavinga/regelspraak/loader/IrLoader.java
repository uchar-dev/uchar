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
import io.github.yhavinga.regelspraak.model.Datatype;
import io.github.yhavinga.regelspraak.model.Eenheid;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Diagnostic;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Dimensie;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Eenheidstelsel;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Feittype;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Granulariteit;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Objecttype;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Objecttype.KenmerkSoort;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Parameter;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.RolBindingRef;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Severity;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Tijdlijn;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The ir.json -&gt; {@link RegelSpraakModel} loader — the inverse of the frontend's {@code IrEmitter}.
 * The maintainer architecture anchor (2026-08-27): the Drools backend CONSUMES the uchar IR off the
 * wire, exactly as ref/ and backends/clingo do. This loader parses the {@code uchar-ir-v1} JSON (its M1
 * shape: the v1 container and member nodes, every other node v0) into the SAME frontend record types the
 * transpiler consumes — the member nodes back into the model's side-maps — so the ported {@code ExecModelTranspiler}
 * fires the IR — never the RegelSpraak source.
 *
 * <p><b>Refusal discipline.</b> Every wire form the v0 Drools backend does not carry is a LOUD, per-form
 * {@link Refusal} — never a silent skip. That covers a {@code format} other than {@code uchar-ir-v1}, the
 * model's {@code functionDefs} extension, an integer beyond a Java {@code int}, and an unknown {@code node}
 * discriminator inside the {@link Expr} / {@link Conditie} / {@link Datatype} sealed hierarchies
 * (a struct/list literal, a MapList/Invoke, …). The differential gate turns a Refusal into a
 * reason-carrying exclusion. A document that is not well-formed is no form at all: the container, the
 * declaration lists and the M1 nodes ({@code ObjectType}, {@code Attribute}, {@code Characteristic},
 * {@code TimelineFacet}, {@code Domain}) are read as strictly as the OCaml loader reads them — exact keys, the
 * node discriminator, the JSON types, WF-001/WF-002 name identity — and a defect there is a {@link Documentfout}
 * naming its JSON position, a hard error.
 *
 * <p><b>Authority.</b> The v0 BEAN fields are authoritative: where the emitter also writes a symbolic
 * extension that is a record field (a feitcreatie's {@code feittype/ankerRol/doelRol/joinRol}, a
 * uniciteit's {@code scopeRol}, a tussenstap's {@code rolBinding}) it is reconstructed faithfully; where
 * the extension is purely emitter-derived and NOT a record field (an AttribuutRef's
 * {@code navigatieBindingen}, a Functie's {@code bronRol}, a Kenmerk's {@code navigatieBinding}) it is
 * ignored — the transpiler re-derives it from the model. The
 * additive {@code bronAnnotaties} provenance field is carried through.
 *
 * <p>FOUR extensions escape that dichotomy. The navigation bindings (an AttribuutRef's
 * {@code navigatieBindingen}, a Kenmerk's {@code navigatieBinding}) are read into
 * {@link Loaded#navigatieBindingen()} and DECIDE a bound navigation in both transpiler branches
 * (V1-WIREMIGRATIE P1): the written {@code navigatieRol}/{@code navigatiePad} they would otherwise be
 * re-derived from are v0 fields migration step B11 removes. A verdeling's {@code verbindendeRol} is read into
 * {@link Loaded#rolBindingen()} and DECIDES the recipient join in both branches (V1-WIREMIGRATIE P3): the
 * type-pair derivation it replaces is ambiguous on a repeated pair, and migration step B12 retires it. The
 * other two, below, are read because the model cannot always re-derive them.
 *
 * <p>Those two: both because the model cannot ALWAYS re-derive them: an
 * OBJECTCREATIE's {@code verbindendeRol} (IR-SPEC ch. 3.3, which RUNTIME-SEMANTICS ch. 5.5 makes AUTHORITATIVE)
 * and an aggregation {@link Expr.Functie}'s {@code bronRol}. On a SELF-TYPED role pair — two same-typed sibling
 * roles of one fact type — the by-pair search is ambiguous by construction and the symbolic binding is the only
 * route there is. Their records belong to the vendored frontend slice (extending them would drag a re-vendor +
 * pin bump through the toolchain), so both ride BESIDE the model in {@link Loaded#rolBindingen()} — the same
 * side-channel shape {@code prioritizedDefaults} already uses.
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
     *  {@link #datumVanIrWire} gives for its own fence). {@code plek} is a JSON pointer into the document, in the
     *  OCaml loader's notation, for the container and the M1 nodes; the v0 nodes inside them are read without a
     *  path, and there it names the node kind and the field. */
    public static final class Documentfout extends IllegalArgumentException {
        private final String plek;
        public Documentfout(String plek, String detail) {
            super("malformed uchar-ir-v1 document at '" + plek + "': " + detail);
            this.plek = plek;
        }
        public String plek() { return plek; }
    }

    private static final List<String> ROOT = List.of("format", "model", "rules", "diagnostics");
    private static final List<String> MODEL = List.of(
            "objectTypes", "factTypes", "parameters", "domains", "unitSystems", "dimensions", "timelines",
            "dayKinds", "dagsoortPerEenheid");
    private static final List<String> RULES = List.of(
            "resolvedRegels", "resolvedDagsoorten", "objectCreaties", "verdelingen", "consistenties",
            "uniciteiten", "uniciteitUnies", "feitCreaties", "resolvedStartpunten", "regelGroepen");

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
    /** ch. 4.10: every {@link Expr.Let} this document carries, in parse order — see {@link Loaded#lets()}. */
    private final List<Expr.Let> lets = new ArrayList<>();
    /** ch. 4.3 / WF-102: the fold-source RANGE as a path, per {@link Expr.Functie} node — see
     *  {@link Loaded#bronPaden()}. Identity-keyed for {@link #rolBindingen}'s reason. */
    private final Map<Object, List<RolBindingRef>> bronPaden = new java.util.IdentityHashMap<>();
    /** IR-SPEC ch. 4.1a: the navigation bindings per AttribuutRef / Kenmerk node — see
     *  {@link Loaded#navigatieBindingen()}. Identity-keyed for {@link #rolBindingen}'s reason. */
    private final Map<Object, List<RolBindingRef>> navigatieBindingen = new java.util.IdentityHashMap<>();

    private IrLoader() {}

    /** A loaded document: the v0 resolved {@link RegelSpraakModel}, the ch. 7 prioritized-defaults extension, and
     *  the symbolic role bindings the wire carries beside the v0 bean fields. All three ride BESIDE the model —
     *  {@code RegelSpraakModel} is the faithful v0 shape, and the frontend records that would host these
     *  extensions belong to a VENDORED slice the toolchain consumes at a pin, so they cannot grow a field here.
     *  A consumer that needs them reads them from here (see {@link #loadAll}).
     *
     *  @param rolBindingen the symbolic (feittype, rol) binding per loaded IR NODE, by IDENTITY. Three node kinds
     *         carry one:
     *         <ul>
     *           <li>a {@link ResolvedObjectCreatie} — IR-SPEC ch. 3.3 {@code verbindendeRol}, naming the role the
     *               CREATED object plays. It is the only route to the back-link of a SELF-TYPED pair, whose
     *               by-pair derivation is ambiguous by construction (two same-typed sibling roles); ch. 5.5 makes
     *               it authoritative, and {@code ref/lib/eval.ml}'s {@code run_object_creatie} lets it WIN over
     *               the type-pair derivation.</li>
     *           <li>an {@link Expr.Functie} aggregation — {@code bronRol}, naming the role the aggregated ELEMENT
     *               plays. Same self-typed gap: "de som van de rang van alle opvolgers van de Wachter" joins two
     *               Wachters, which no by-pair search can disambiguate.</li>
     *           <li>a {@link ResolvedVerdeling} — IR-SPEC ch. 3.2 {@code verbindendeRol}, naming the role the
     *               RECIPIENT plays. It decides the recipient join in both transpiler branches
     *               (V1-WIREMIGRATIE P3), as {@code run_verdeling} lets it decide in ref; only a bindingless
     *               verdeling, which WF-048 admits while the type pair is unique, is still derived from the
     *               pair.</li>
     *         </ul>
     *         In every case the SIBLING role — the one the anchor (subject) plays — is the bean field on the other
     *         end. Absent for a node whose wire record carries no binding; the by-pair derivation then stands.
     *  @param lets an INDEX of the {@link Expr.Let} nodes in the document, in parse order — not a channel: the Lets
     *         themselves sit in the expression trees as ordinary union nodes, and every consumer lowers and walks
     *         them there. The index exists for the one consumer that must start a walk at a Let without holding the
     *         tree it came from ({@code EffectieveEenheid}'s binder-read table, which answers the unit of a
     *         {@link Expr.BinderRef} asked in isolation). Empty for a document that carries no Let.
     *  @param bronPaden the ch. 4.3 / WF-102 fold-source RANGE per {@link Expr.Functie} node, by IDENTITY: the
     *         ordered RolBinding steps, subject-outward, VERBATIM off the wire. It rides here for the same reason
     *         {@code bronRol} does — {@code Expr.Functie} is the vendored slice — and for a sharper one: unlike
     *         every other extension this loader carries, a path is not re-derivable. The type PAIR answers for the
     *         endpoints only, and a path's intermediate types are its own claim; a consumer that dropped this list
     *         would silently fold whatever direct relation happens to connect subject and element. Empty for a
     *         document whose folds all carry the one-step range.
     *  @param navigatieBindingen the IR-SPEC ch. 4.1a navigation walk per navigated {@link Expr.AttribuutRef}
     *         ({@code navigatieBindingen}, one binding per hop, subject-outward) and per navigated
     *         {@link Conditie.Kenmerk} ({@code navigatieBinding}, a one-hop list), by IDENTITY, verbatim off the
     *         wire. Where a node carries one it decides the walk in both transpiler branches; a node without one
     *         is the bindingless form WF-053 also admits, and only there is the walk derived from the written
     *         step. */
    public record Loaded(RegelSpraakModel model, List<PrioritizedDefaults> prioritizedDefaults,
                         Map<Object, RolBindingRef> rolBindingen, List<Expr.Let> lets,
                         Map<Object, List<RolBindingRef>> bronPaden,
                         Map<Object, List<RolBindingRef>> navigatieBindingen) {}

    /** Parse a {@code uchar-ir-v1} tree into a resolved model, or throw {@link Refusal} for a form
     *  the v0 backend does not carry. The prioritized-defaults extension (if any) is parsed but not returned —
     *  use {@link #loadAll} to obtain it beside the model. */
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

    private RegelSpraakModel model(JsonNode root) {
        if (root == null || !root.isObject()) throw new Documentfout("/", "expected an object, got " + kort(root));
        String format = text(root, "format");
        if (!"uchar-ir-v1".equals(format))
            throw new Refusal("format", String.valueOf(format));
        velden(root, "", null, ROOT, List.of());
        JsonNode model = sectie(root, "", "model");
        JsonNode rules = sectie(root, "", "rules");
        velden(model, "/model", null, MODEL, List.of("functionDefs"));
        // an extension the schema admits and this backend does not carry: a form, so a Refusal, not a defect
        if (model.has("functionDefs")) throw new Refusal("functionDefs", "unknown model section");
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
            eenmaal(m.parameters, p + "/naam", par.naam(), par);
        });
        lijst(model, "/model", "unitSystems", (p, n) -> {
            Eenheidstelsel es = eenheidstelsel(p, n);
            eenmaal(m.eenheidstelsels, p + "/naam", es.naam, es);
        });
        lijst(model, "/model", "factTypes", (p, n) -> {
            Feittype ft = feittype(p, n);
            eenmaal(m.feittypen, p + "/naam", ft.naam, ft);
        });
        lijst(model, "/model", "dimensions", (p, n) -> {
            Dimensie d = dimensie(p, n);
            eenmaal(m.dimensies, p + "/naam", d.naam(), d);
        });
        lijst(model, "/model", "timelines", (p, n) -> m.tijdlijnen.add(tijdlijn(n)));
        lijst(model, "/model", "dayKinds", (p, n) -> m.dagsoorten.add(n.asText()));
        forEachField(sectie(model, "/model", "dagsoortPerEenheid"), (k, v) -> m.dagsoortPerEenheid.put(k, v.asText()));

        lijst(rules, "/rules", "resolvedRegels", (p, n) -> m.resolvedRegels.add(resolvedRegel(n)));
        lijst(rules, "/rules", "resolvedDagsoorten", (p, n) -> m.resolvedDagsoorten.add(
                new ResolvedDagsoort(text(n, "naam"), conditie(n.get("voorwaarde")), geldigheid(n))));
        lijst(rules, "/rules", "objectCreaties", (p, n) -> m.objectCreaties.add(objectCreatie(n)));
        lijst(rules, "/rules", "verdelingen", (p, n) -> m.verdelingen.add(verdeling(n)));
        lijst(rules, "/rules", "consistenties", (p, n) -> m.consistenties.add(consistentie(n)));
        lijst(rules, "/rules", "uniciteiten", (p, n) -> m.uniciteiten.add(uniciteit(n)));
        lijst(rules, "/rules", "uniciteitUnies", (p, n) -> m.uniciteitUnies.add(uniciteitUnie(n)));
        lijst(rules, "/rules", "feitCreaties", (p, n) -> m.feitCreaties.add(feitCreatie(n)));
        lijst(rules, "/rules", "resolvedStartpunten", (p, n) -> m.resolvedStartpunten.add(
                new ResolvedStartpunt(text(n, "tijdlijn"), text(n, "objecttype"), text(n, "lid"))));
        weigerGeerfdAnker();   // the §9.9 family: asked here, where BOTH the tijdlijnen and the determinations are read
        lijst(rules, "/rules", "regelGroepen", (p, n) -> m.regelGroepen.add(new ResolvedRegelGroep(
                text(n, "naam"), bool(n, "recursief"), stringList(n.get("leden")), bron(n), intOf(n, "line"))));
        lijst(root, "", "diagnostics", (p, n) -> m.diagnostics.add(new Diagnostic(
                enumOf(Severity.class, text(n, "severity"), p + "/severity"), text(n, "message"), intOf(n, "line"))));
        // §6.1 (Phase 4) the prioritized-defaults extension, parsed AFTER the declarations so a default's
        // condition/value resolves its objecttype/parameter references against the fully-built model. A malformed
        // defaults bundle (an unknown inner node, a missing target) raises a loud Refusal from these parsers.
        if (rules.has("prioritizedDefaults"))
            lijst(rules, "/rules", "prioritizedDefaults", (p, n) -> prioritizedDefaults.add(prioritizedDefaults(n)));
        return m;
    }

    /** The key set of a container object or an M1 node, checked as the OCaml loader's {@code check_keys} checks it:
     *  the node discriminator when {@code knoop} names one, no key outside the node's own, every required key
     *  present, and an optional key written whole or not at all — present and null is neither (IR-SPEC § Method).
     *  Read leniently, each of these was a document part ignored or a null taken for an absent field. */
    private static void velden(JsonNode n, String pad, String knoop, List<String> verplicht, List<String> optioneel) {
        if (n == null || !n.isObject())
            throw new Documentfout(pad.isEmpty() ? "/" : pad, "expected a JSON object"
                    + (knoop == null ? "" : " (" + knoop + ")") + ", got " + kort(n));
        if (knoop != null) {
            JsonNode soort = n.get("node");
            if (soort == null)
                throw new Documentfout(pad + "/node", "required field is absent (the discriminator \"" + knoop + "\")");
            if (!soort.isTextual() || !knoop.equals(soort.asText()))
                throw new Documentfout(pad + "/node", "expected \"" + knoop + "\", got " + kort(soort));
        }
        n.fieldNames().forEachRemaining(k -> {
            if (!verplicht.contains(k) && !optioneel.contains(k))
                throw new Documentfout(pad + "/" + k, "unknown field" + (knoop == null ? "" : " on " + knoop));
        });
        for (String k : verplicht)
            if (!n.has(k)) throw new Documentfout(pad + "/" + k, "required field is absent");
        for (String k : optioneel)
            if (n.has(k) && n.get(k).isNull())
                throw new Documentfout(pad + "/" + k, "an optional field is written whole or not at all, and null is neither");
    }

    private static JsonNode sectie(JsonNode ouder, String pad, String naam) {
        JsonNode n = ouder.get(naam);
        if (n == null) throw new Documentfout(pad + "/" + naam, "required field is absent");
        if (!n.isObject()) throw new Documentfout(pad + "/" + naam, "expected an object, got " + kort(n));
        return n;
    }

    /** A list the schema requires: an array, each entry read with its own position. The lenient reading took a
     *  non-array for an empty list, so {@code objectTypes: {}} loaded as a document without object types. */
    private static void lijst(JsonNode ouder, String pad, String veld, java.util.function.BiConsumer<String, JsonNode> f) {
        JsonNode a = ouder.get(veld);
        String p = pad + "/" + veld;
        if (a == null) throw new Documentfout(p, "required field is absent");
        if (!a.isArray()) throw new Documentfout(p, "expected an array, got " + kort(a));
        for (int i = 0; i < a.size(); i++) f.accept(p + "/" + i, a.get(i));
    }

    /** The list form writes each name once, on its node, so a nameless entry has no name at all (the v0 map key
     *  that once stood in for it is gone), and a repeated name would silently replace the declaration before it. */
    private static String naam(JsonNode n, String pad, String veld) {
        JsonNode v = n.get(veld);
        if (v == null || !v.isTextual())
            throw new Documentfout(pad + "/" + veld, "a declaration without a name: expected a string, got "
                    + (v == null ? "no field" : kort(v)));
        return v.asText();
    }

    private static String tekst(JsonNode n, String pad, String veld) {
        JsonNode v = n.get(veld);
        if (v == null) throw new Documentfout(pad + "/" + veld, "required field is absent");
        if (!v.isTextual()) throw new Documentfout(pad + "/" + veld, "expected a string, got " + kort(v));
        return v.asText();
    }

    /** An optional string field: absent is null; present, it is a string ({@link #velden} has refused a null). */
    private static String optTekst(JsonNode n, String pad, String veld) {
        return n.has(veld) ? tekst(n, pad, veld) : null;
    }

    private static boolean waarheid(JsonNode n, String pad, String veld) {
        JsonNode v = n.get(veld);
        if (v == null) throw new Documentfout(pad + "/" + veld, "required field is absent");
        if (!v.isBoolean()) throw new Documentfout(pad + "/" + veld, "expected a boolean, got " + kort(v));
        return v.booleanValue();
    }

    private static List<String> tekstLijst(JsonNode n, String pad, String veld) {
        JsonNode a = n.get(veld);
        if (a == null || !a.isArray()) throw new Documentfout(pad + "/" + veld, "expected an array, got " + kort(a));
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
        if (c == null) throw new Documentfout(p, "required field is absent");
        if (!c.isIntegralNumber()) throw new Documentfout(p, "expected an integer, got " + kort(c));
        if (!c.canConvertToInt())
            throw new Refusal(vorm, c + " at '" + p + "' is outside this backend's integer range ["
                    + Integer.MIN_VALUE + ", " + Integer.MAX_VALUE + "]");
        return c.intValue();
    }

    /** WF-001: a namespace holds a name once, under the verifier's name identity ({@link #naamSleutel}). The
     *  model's tables are keyed on the exact string and look a name up case-insensitively after an exact miss, so
     *  two spellings of one name would both enter and a lookup would pick one of them. */
    private static <V> void eenmaal(Map<String, V> tabel, String pad, String naam, V waarde) {
        String eerder = botsing(tabel.keySet(), naam);
        if (eerder != null) throw new Documentfout(pad, dubbel(naam, eerder, "WF-001"));
        tabel.put(naam, waarde);
    }

    /** The declared name {@code naam} collides with under {@link #naamSleutel}, or null. */
    private static String botsing(java.util.Collection<String> namen, String naam) {
        String sleutel = naamSleutel(naam);
        for (String n : namen) if (naamSleutel(n).equals(sleutel)) return n;
        return null;
    }

    private static String dubbel(String naam, String eerder, String regel) {
        return naam.equals(eerder)
                ? "the name '" + naam + "' is declared twice (" + regel + ")"
                : "the name '" + naam + "' collides with the earlier '" + eerder + "' (" + regel
                        + " compares names case-insensitively)";
    }

    /** The verifier's name identity ({@code norm} in {@code spec/verifier/lib/checks.ml}): ASCII letters folded to
     *  lower case, runs of spaces and tabs collapsed, the ends trimmed. Mirrored exactly, so this loader refuses the
     *  collisions WF-001/WF-002 refuse and no others. */
    private static String naamSleutel(String naam) {
        StringBuilder sb = new StringBuilder();
        for (String deel : naam.split("[ \t]+")) {
            if (deel.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            for (char c : deel.toCharArray()) sb.append(c >= 'A' && c <= 'Z' ? (char) (c + ('a' - 'A')) : c);
        }
        return sb.toString();
    }

    /** A JSON value as a message quotes it, cut short: the value is evidence, not the document. */
    private static String kort(JsonNode n) {
        String s = String.valueOf(n);
        return s.length() > 60 ? s.substring(0, 60) + "…" : s;
    }

    // --- prioritized-defaults extension (ch. 7) ----------------------------

    private PrioritizedDefaults prioritizedDefaults(JsonNode n) {
        String onderwerp = text(n, "onderwerp");
        String kenmerk = text(n, "kenmerk");
        String attribuut = text(n, "attribuut");
        if (onderwerp == null) throw new Refusal("PrioritizedDefaults", "defaults without a subject (onderwerp)");
        if ((kenmerk == null) == (attribuut == null))
            throw new Refusal("PrioritizedDefaults", "defaults must carry exactly one of kenmerk/attribuut (WF-050)");
        List<Default> defaults = new ArrayList<>();
        forEach(n.get("defaults"), r -> defaults.add(dflt(r)));
        return new PrioritizedDefaults(text(n, "naam"), onderwerp, kenmerk, attribuut, bool(n, "ongeordend"),
                defaults, text(n, "bron"), geldigheid(n));
    }

    private Default dflt(JsonNode n) {
        JsonNode b = n.get("besluit");
        Boolean besluit = b == null || b.isNull() ? null : b.asBoolean();
        return new Default(besluit, conditie(n.get("voorwaarde")), expr(n.get("consequentie")),
                bool(n, "initialisatie"), excepties(n.get("excepties")), text(n, "bron"));
    }

    private Default.Excepties excepties(JsonNode n) {
        if (n == null || n.isNull()) return null;
        List<ExceptionGroup> leden = new ArrayList<>();
        forEach(n.get("leden"), s -> leden.add(exceptionGroup(s)));
        return new Default.Excepties(bool(n, "ongeordend"), leden);
    }

    private ExceptionGroup exceptionGroup(JsonNode n) {
        List<Default> defaults = new ArrayList<>();
        forEach(n.get("defaults"), r -> defaults.add(dflt(r)));
        return new ExceptionGroup(defaults, text(n, "bron"));
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

    // --- declaration side --------------------------------------------------

    /** IR-SPEC ch. 2.2: the member nodes back into the model's member-keyed side-maps. Every facet's count is
     *  kept, 1 included, as the OCaml loader keeps it. The frontend's model records a count only when it is not 1,
     *  and the one reader of these tables, {@code Tijdlijnen.meerPeriodiek}, takes an absent entry and an explicit
     *  1 for the same single-period facet, so keeping the 1 moves no outcome; what it adds is that a regression to
     *  a presence test fails on every temporal golden instead of on none ({@code MeerPeriodiekePoortWitnessTest}).
     *  A characteristic's kind is required on the wire since M1; the emitter writes ZELFSTANDIG for an untagged
     *  one (ch. 2.2: untagged = nominal). */
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
            ot.attributen.put(lid, datatype(dt));
            String meervoud = optTekst(a, ap, "plural");
            if (meervoud != null) ot.attribuutMeervoud.put(lid, meervoud);
            if (a.has("dimensions")) ot.attribuutDimensies.put(lid, tekstLijst(a, ap, "dimensions"));
            if (a.has("timeline")) {
                String tp = ap + "/timeline";
                JsonNode tl = a.get("timeline");
                velden(tl, tp, "TimelineFacet", List.of("node", "granularity", "count"), List.of("timelineName"));
                ot.attribuutGranulariteit.put(lid,
                        enumOf(Granulariteit.class, tekst(tl, tp, "granularity"), tp + "/granularity"));
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
            ot.kenmerkSoorten.put(lid, enumOf(KenmerkSoort.class, tekst(c, cp, "kind"), cp + "/kind"));
            if (c.has("timeline")) {
                String tp = cp + "/timeline";
                JsonNode tl = c.get("timeline");
                velden(tl, tp, "TimelineFacet", List.of("node", "granularity", "count"), List.of());
                ot.kenmerkGranulariteit.put(lid,
                        enumOf(Granulariteit.class, tekst(tl, tp, "granularity"), tp + "/granularity"));
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
        switch (String.valueOf(text(dt, "node"))) {
            case "Enumeratie" -> {
                velden(dt, dp, "Enumeratie", List.of("node", "waarden"), List.of());
                if (!dt.get("waarden").isArray())
                    throw new Documentfout(dp + "/waarden", "expected an array, got " + kort(dt.get("waarden")));
                return new Datatype.Enumeratie(naam, stringList(dt.get("waarden")));
            }
            case "SumType" -> velden(dt, dp, "SumType", List.of("node", "constructors"), List.of());
            case "StructType" -> velden(dt, dp, "StructType", List.of("node", "fields"), List.of());
            default -> { }
        }
        return datatype(dt);
    }

    private Parameter parameter(String pad, JsonNode n) {
        return new Parameter(naam(n, pad, "naam"), datatype(n.get("datatype")),
                enumOf(Granulariteit.class, n, "granulariteit"));
    }

    private Eenheidstelsel eenheidstelsel(String pad, JsonNode n) {
        Eenheidstelsel es = new Eenheidstelsel(naam(n, pad, "naam"));
        forEachField(n.get("eenheden"), (k, v) -> es.eenheden.put(k, eenheid(v)));
        forEachField(n.get("aliassen"), (k, v) -> es.aliassen.put(k, eenheid(v)));
        return es;
    }

    private Eenheid eenheid(JsonNode n) {
        if (n == null || n.isNull()) return null;
        return new Eenheid(text(n, "afkorting"), text(n, "stelsel"), text(n, "canoniek"),
                decimal(n, "factorNaarCanoniek"), eenheid(n.get("noemer")));
    }

    private Feittype feittype(String pad, JsonNode n) {
        Feittype ft = new Feittype(naam(n, pad, "naam"));
        ft.wederkerig = bool(n, "wederkerig");
        forEach(n.get("rollen"), r -> ft.rollen.add(new RegelSpraakModel.Rol(
                text(r, "naam"), text(r, "objecttypeNaam"), text(r, "meervoud"), bool(r, "meervoudig"))));
        return ft;
    }

    private Dimensie dimensie(String pad, JsonNode n) {
        return new Dimensie(naam(n, pad, "naam"), text(n, "naamMeervoud"),
                text(n, "voorzetsel"), stringList(n.get("waarden")));
    }

    private Tijdlijn tijdlijn(JsonNode n) {
        return new Tijdlijn(text(n, "naam"), bool(n, "variabelStartpunt"),
                enumOf(Granulariteit.class, n, "granulariteit"), intOf(n, "aantal"));
    }

    // --- Datatype ----------------------------------------------------------

    private Datatype datatype(JsonNode n) {
        if (n == null || n.isNull()) return null;
        String node = text(n, "node");
        return switch (node) {
            case "Numeriek" -> new Datatype.Numeriek(bool(n, "geheel"), intOf(n, "decimalen"),
                    enumOf(Datatype.Teken.class, n, "teken"), eenheid(n.get("eenheid")));
            case "Percentage" -> new Datatype.Percentage(bool(n, "geheel"), intOf(n, "decimalen"),
                    n.get("eenheid") == null || n.get("eenheid").isNull() ? Datatype.Percentage.PROCENT : eenheid(n.get("eenheid")));
            case "Tekst" -> new Datatype.Tekst();
            case "Booleaan" -> new Datatype.Booleaan();
            case "DatumInDagen" -> new Datatype.DatumInDagen();
            case "DatumTijd" -> new Datatype.DatumTijd();
            case "Enumeratie" -> new Datatype.Enumeratie(text(n, "domein"), stringList(n.get("waarden")));
            case "Onbekend" -> new Datatype.Onbekend(text(n, "reden"));
            default -> throw new Refusal("Datatype:" + node, "unknown datatype node");
        };
    }

    // --- Expr --------------------------------------------------------------

    private Expr expr(JsonNode n) {
        if (n == null || n.isNull()) return null;
        String node = text(n, "node");
        return switch (node) {
            case "Literal" -> new Expr.Literal(text(n, "waarde"), datatype(n.get("type")), bron(n));
            case "TekstReeks" -> {
                List<Expr.TekstReeks.Deel> delen = new ArrayList<>();
                forEach(n.get("delen"), d -> delen.add(tekstReeksDeel(d)));
                yield new Expr.TekstReeks(delen, bron(n));
            }
            case "Binair" -> new Expr.Binair(enumOf(Expr.BinOp.class, n, "op"),
                    expr(n.get("links")), expr(n.get("rechts")), datatype(n.get("type")), bron(n));
            case "Functie" -> {
                List<Expr> args = new ArrayList<>();
                forEach(n.get("argumenten"), a -> args.add(expr(a)));
                // A kind this backend's IR records do not carry is a NAMED per-form Refusal, on the
                // Kwantorvouw.kwantor precedent below: Enum.valueOf would otherwise throw an unattributed
                // IllegalArgumentException, which gate 12 reads as an ERROR on the golden rather than as a
                // reasoned channel, and which names no form a reader could act on. The arm is written for the
                // CLASS rather than for one kind, and it has already outlived its first occasion: AANTAL_GEVULDE
                // (IR-SPEC ch. 4.3) reached it while it was wire-only and passes it since both transpiler
                // branches carry the kind (2026-09-20). The next wire-first kind gets the same answer.
                Expr.FunctieKind fk;
                try { fk = enumOf(Expr.FunctieKind.class, n, "kind"); }
                catch (IllegalArgumentException e) { fk = null; }
                if (fk == null && text(n, "kind") != null)
                    throw new Refusal("Functie.kind", "'" + text(n, "kind") + "' is an aggregation or builtin kind"
                            + " this backend does not carry; the wire form is defined (IR-SPEC ch. 4.3) and a"
                            + " transpiler arm for it has not landed here yet");
                Expr.Functie f = new Expr.Functie(fk, args,
                        ot(text(n, "elementObjecttype")), expr(n.get("leegwaarde")), conditie(n.get("filter")),
                        dimensieSelectie(n.get("dimensieSelectie")), datatype(n.get("type")), bron(n),
                        enumOf(Granulariteit.class, n, "granulariteit"));
                // IR-SPEC ch. 4.3 / WF-102: a MULTI-STEP fold-source range. Read VERBATIM — the full ordered list
                // of RolBinding steps, nothing derived from the presentation strings and nothing re-derived from
                // the endpoint type PAIR. That is the whole point of the field: a by-pair search binds a
                // coincidental DIRECT relation between subject and element type and folds the WRONG set, which is
                // exactly what this executor did when the field was dropped (measured on jutterswet-vondstketen:
                // 5,00 where the sentence says 3,00, with ref and clingo answering 3,00). The three SHAPE arms
                // below are refused BY NAME here rather than downstream, for ref's reason: a document that reaches
                // an executor without passing the verifier must fail naming the shape it violates instead of being
                // read as the one-step form with the range dropped. Arm (a) comes FIRST so a document carrying
                // both spellings is named for carrying both. The MODEL-relative arms (a step that does not join,
                // a landing beside the element type, a hop the beans carry no field for) belong to the walk and
                // are per-form refusals in the transpilers, which know the fold SUBJECT this node does not.
                List<RolBindingRef> pad = null;
                if (n.hasNonNull("bronPad")) {
                    if (n.hasNonNull("bronRol"))
                        throw new Refusal("Functie.bronPad",
                                "a bronPad beside a bronRol: the one-step range IS bronRol (WF-102 (a))");
                    pad = new ArrayList<>();
                    for (JsonNode s : n.get("bronPad")) pad.add(rolBinding(s));
                    if (pad.size() < 2)
                        throw new Refusal("Functie.bronPad", "a bronPad carries at least TWO steps, found "
                                + pad.size() + " — the one-step range is spelled bronRol (WF-102 (b))");
                    if (!isLedenVouwbron(n))
                        throw new Refusal("Functie.bronPad", "a bronPad is admissible only on the SET source of a"
                                + " single-argument instance fold — the position bronRol stands at (WF-102 (c))");
                    bronPaden.put(f, List.copyOf(pad));
                }
                // the aggregation's symbolic bronRol — see Loaded#rolBindingen. It stays OUT of the record (that
                // one is vendored) and the transpiler still re-derives the join by pair FIRST; the binding is the
                // fallback for the pair no by-pair search can disambiguate, a SELF-TYPED one.
                RolBindingRef br = rolBinding(n.get("bronRol"));
                if (br != null) rolBindingen.put(f, br);
                yield f;
            }
            case "Afgerond" -> new Expr.Afgerond(expr(n.get("basis")),
                    enumOf(Expr.AfrondModus.class, n, "modus"), intOf(n, "decimalen"), bron(n));
            case "Begrensd" -> new Expr.Begrensd(expr(n.get("basis")), expr(n.get("minimum")), expr(n.get("maximum")), bron(n));
            case "AttribuutRef" -> {
                Expr.AttribuutRef a = new Expr.AttribuutRef(text(n, "attribuut"), ot(text(n, "objecttype")),
                        text(n, "onderwerpBron"), text(n, "navigatieRol"), datatype(n.get("type")), bron(n),
                        bool(n, "set"), stringList(n.get("navigatiePad")));
                JsonNode nb = n.get("navigatieBindingen");
                if (nb != null) {
                    // written whole or not at all, never null, never empty (IR-SPEC ch. 4.1a): a malformed list
                    // is refused by name rather than read as the bindingless form, which would re-derive the walk
                    if (!nb.isArray() || nb.isEmpty())
                        throw new Refusal("AttribuutRef.navigatieBindingen", "malformed: a navigatieBindingen field is a"
                                + " non-empty list of RolBindings, never null (IR-SPEC ch. 4.1a)");
                    List<RolBindingRef> stappen = new ArrayList<>();
                    for (JsonNode s : nb) stappen.add(navigatieStap("AttribuutRef.navigatieBindingen", s));
                    navigatieBindingen.put(a, List.copyOf(stappen));
                }
                yield a;
            }
            case "DimensieCel" -> new Expr.DimensieCel((Expr.AttribuutRef) expr(n.get("attribuut")),
                    stringList(n.get("labels")), bron(n));
            case "ParameterRef" -> new Expr.ParameterRef(param(text(n, "parameter")), bron(n));
            case "VariabeleRef" -> new Expr.VariabeleRef(text(n, "naam"), datatype(n.get("type")), bron(n));
            case "Constante" -> new Expr.Constante(text(n, "naam"), datatype(n.get("type")), bron(n));
            // reden null: it is the FRONTEND's own field (the WARNING it raises over a surviving marker)
            // and the v0 wire carries no such key, so a marker read off the wire has none to recover.
            case "Onopgelost" -> new Expr.Onopgelost(datatype(n.get("type")), bron(n), null);
            // IR-SPEC ch. 4.10 the Let cluster — all three are ORDINARY union nodes, built straight into the tree
            // with nothing riding beside them. Note the wire's field names: a BinderRef's is "name", an
            // IfThenElse's are "condition"/"then"/"else" — the condition reuses the ordinary conditie union.
            case "Let" -> let(new Expr.Let(
                    verplicht("Let", "binder", text(n, "binder")),
                    verplicht("Let", "value", expr(n.get("value"))),
                    verplicht("Let", "body", expr(n.get("body"))),
                    verplicht("Let", "type", datatype(n.get("type"))), bron(n)));
            case "BinderRef" -> new Expr.BinderRef(
                    verplicht("BinderRef", "name", text(n, "name")),
                    verplicht("BinderRef", "type", datatype(n.get("type"))), bron(n));
            case "IfThenElse" -> new Expr.IfThenElse(
                    verplicht("IfThenElse", "condition", conditie(n.get("condition"))),
                    verplicht("IfThenElse", "then", expr(n.get("then"))),
                    verplicht("IfThenElse", "else", expr(n.get("else"))),
                    verplicht("IfThenElse", "type", datatype(n.get("type"))), bron(n));
            // IR-SPEC ch. 4.3 / WF-110: the §5.7 concatenation of subject expressions as the source of an
            // instance count. Read VERBATIM, one selection per branch, for the reason bronPad is: every
            // selection names its own population and its own relation, and a backend that re-derived either
            // from the type pair would give the selections one and the same range — they all share one pair
            // by WF-110 (d) — and count a restatement of one of them. The two required fields are refused
            // BY NAME here, on the verplicht() pattern, so a document that reaches this executor without
            // passing the verifier fails naming the shape it violates.
            case "InstanceSetUnion" -> {
                List<Expr.InstanceSetUnion.Tak> takken = new ArrayList<>();
                forEach(n.get("branches"), b -> {
                    RegelSpraakModel.Objecttype el = verplicht("InstanceSetBranch", "elementType",
                            ot(text(b, "elementType")));
                    RolBindingRef rb = verplicht("InstanceSetBranch", "sourceRole", rolBinding(b.get("sourceRole")));
                    takken.add(new Expr.InstanceSetUnion.Tak(el, rb, conditie(b.get("filter")), bron(b)));
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

    /** Assert that a ch. 4.10 Let-cluster node carries a field the shape REQUIRES (WF-076/WF-064/WF-069), naming
     *  both the form and the field. Without it a malformed document fails downstream and namelessly: a null
     *  {@code value}/{@code body}/branch throws an unattributed NPE out of the transpiler's {@code List.of}
     *  child accessor, and a missing {@code type} silently produces a node typed null, which every walk that reads
     *  a datatype then mis-reads. Fail fast, with the form and the field in the message. */
    private static <T> T verplicht(String vorm, String veld, T waarde) {
        if (waarde == null) throw new Refusal(vorm, "malformed node: missing required field '" + veld + "'");
        return waarde;
    }

    /** WF-102 (c): is this {@code Functie} node the SET source of a single-argument instance fold — the one
     *  position {@code bronRol} stands at, and hence the only one a RANGE applies to? Asked of the RAW node, in
     *  the verifier's own terms: a declared {@code elementObjecttype} plus exactly one argument that is a plain
     *  set-reading AttribuutRef. The other fold-source shapes hand the fold something else entirely — the temporal
     *  total reads the SUBJECT's own timeline, the row-1 wart hands it the subject's linked instances — so a range
     *  does not apply to them and a document that writes one there is refused rather than half-read. */
    private static boolean isLedenVouwbron(JsonNode n) {
        if (!n.hasNonNull("elementObjecttype")) return false;
        JsonNode args = n.get("argumenten");
        if (args == null || !args.isArray() || args.size() != 1) return false;
        JsonNode a = args.get(0);
        return "AttribuutRef".equals(text(a, "node")) && bool(a, "set")
                && !a.hasNonNull("navigatieRol")
                && (!a.hasNonNull("navigatiePad") || a.get("navigatiePad").isEmpty());
    }

    /** Index one {@link Expr.Let} (see {@link Loaded#lets()}) and return it — the node itself goes into the tree. */
    private Expr let(Expr.Let l) {
        lets.add(l);
        return l;
    }

    private Expr.TekstReeks.Deel tekstReeksDeel(JsonNode d) {
        String node = text(d, "node");
        return switch (node) {
            case "Letterlijk" -> new Expr.TekstReeks.Letterlijk(text(d, "tekst"));
            case "Ingevoegd" -> new Expr.TekstReeks.Ingevoegd(expr(d.get("expr")));
            default -> throw new Refusal("TekstReeks.Deel:" + node, "unknown tekstreeks part");
        };
    }

    private Expr.DimensieSelectie dimensieSelectie(JsonNode n) {
        if (n == null || n.isNull()) return null;
        return new Expr.DimensieSelectie(text(n, "dimensie"), stringList(n.get("labels")), bron(n));
    }

    // --- Conditie ----------------------------------------------------------

    private Conditie conditie(JsonNode n) {
        if (n == null || n.isNull()) return null;
        String node = text(n, "node");
        return switch (node) {
            case "Vergelijking" -> new Conditie.Vergelijking(enumOf(Conditie.Operator.class, n, "op"),
                    expr(n.get("links")), expr(n.get("rechts")), geheelPeriode(n.get("periode")), bron(n));
            case "Predicaat" -> predicaat(n);
            case "Kenmerk" -> {
                Conditie.Kenmerk k = new Conditie.Kenmerk(text(n, "kenmerk"), text(n, "navigatie"), bool(n, "negatie"),
                        ot(text(n, "eigenaar")), geheelPeriode(n.get("periode")), bron(n));
                if (n.has("navigatieBinding")) {
                    // shape before meaning: a null or incomplete binding is malformed whatever the check does
                    RolBindingRef nb = navigatieStap("Kenmerk.navigatieBinding", n.get("navigatieBinding"));
                    // ref faults on the same shape: a binding on a check that does not navigate (WF-053)
                    if (k.navigatie() == null)
                        throw new Refusal("Kenmerk.navigatieBinding",
                                "a navigation binding on a characteristic check that does not navigate (WF-053)");
                    navigatieBindingen.put(k, List.of(nb));
                }
                yield k;
            }
            case "Rollidmaatschap" -> new Conditie.Rollidmaatschap(text(n, "rol"), ot(text(n, "anderTeFiller")),
                    bron(n), bool(n, "negatie"));
            case "VerreRollidmaatschap" -> new Conditie.VerreRollidmaatschap(ot(text(n, "filler")),
                    text(n, "backref"), bool(n, "negatie"), bron(n));
            case "Periode" -> new Conditie.Periode(expr(n.get("vanaf")), expr(n.get("tot")), bool(n, "totInclusief"), bron(n));
            case "Samengesteld" -> {
                List<Conditie> delen = new ArrayList<>();
                forEach(n.get("onderdelen"), o -> delen.add(conditie(o)));
                yield new Conditie.Samengesteld(enumOf(Conditie.Kwantor.class, n, "kwantor"),
                        intOf(n, "drempel"), delen, bron(n));
            }
            // IR-SPEC ch. 4.5 / WF-108 the QUANTIFIER FOLD. Unlike Samengesteld — whose kwantor ranges over the
            // BULLETS of one subject — this one ranges over INSTANCES, and its range fields are the instance fold's
            // (ch. 4.3) spelled unchanged. Both extension fields are decoded INTO the record: Kwantorvouw is not part
            // of the vendored frontend slice the Functie/ResolvedObjectCreatie side-channel exists for, so bronRol
            // needs no Loaded#rolBindingen entry.
            case "Kwantorvouw" -> {
                // WF-108 (c), the WF-102 discipline under this node's own rule. Refused for exactly the reason
                // Functie.bronPad is: the model cannot re-derive a multi-step range, and the by-pair fallback would
                // bind a coincidental DIRECT subject-to-element relation and quantify over the WRONG set while the
                // OCaml executors quantify over the right one. A named per-form Refusal puts the golden in gate 12's
                // channel floor by reason instead.
                if (n.hasNonNull("bronPad"))
                    throw new Refusal("Kwantorvouw.bronPad",
                            "a multi-step quantifier range (WF-108 (c)); the accumulate source carries one join role");
                // WF-108 (a): ALLE, GEEN and the three COUNTING quantors are the admitted forms. DE shares
                // Samengesteld's enum and is refused BY NAME — the bare definite describes one member, and reading
                // it as a forall is the misreading this node exists to prevent. An unknown token never reaches
                // Enum.valueOf's unattributed IllegalArgumentException either: it lands in the same named Refusal.
                Conditie.Kwantor kw;
                try { kw = enumOf(Conditie.Kwantor.class, n, "kwantor"); }
                catch (IllegalArgumentException e) { kw = null; }
                if (kw == null || kw == Conditie.Kwantor.DE)
                    throw new Refusal("Kwantorvouw.kwantor", "'" + text(n, "kwantor") + "' is not a quantifier over"
                            + " a set (WF-108 (a)); the forms are ALLE, GEEN, TEN_MINSTE, TEN_HOOGSTE and PRECIES");
                // WF-108 (e): the threshold is present EXACTLY under a counting quantor. Read as an absence rather
                // than as a 0 — a counting fold whose threshold is missing has no verdict, and guessing one here
                // would answer a question the document does not ask.
                boolean telt = kw == Conditie.Kwantor.TEN_MINSTE || kw == Conditie.Kwantor.TEN_HOOGSTE
                        || kw == Conditie.Kwantor.PRECIES;
                if (telt != n.hasNonNull("drempel"))
                    throw new Refusal("Kwantorvouw.drempel", "a drempel is written exactly under a counting"
                            + " kwantor (WF-108 (e)); this node has kwantor " + kw
                            + (telt ? " and no drempel" : " and one"));
                yield new Conditie.Kwantorvouw(kw, telt ? intOf(n, "drempel") : 0,
                        ot(text(n, "elementObjecttype")),
                        conditie(n.get("filter")), conditie(n.get("toets")),
                        rolBinding(n.get("bronRol")), List.of(), bron(n));
            }
            case "Regelversie" -> new Conditie.Regelversie(text(n, "regel"), bool(n, "inconsistent"), bron(n));
            case "Onopgelost" -> new Conditie.Onopgelost(bron(n), null);   // frontend-internal field, absent from the wire
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
        Conditie.Predicaatsoort soort = enumOf(Conditie.Predicaatsoort.class, n, "soort");
        boolean negatie = bool(n, "negatie");
        Conditie.GeheelPeriode periode = geheelPeriode(n.get("periode"));
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
        return new Conditie.Predicaat(soort, expr(n.get("operand")), text(n, "dagsoort"), negatie, intOf(n, "cijfers"),
                periode, bron(n));
    }

    private Conditie.GeheelPeriode geheelPeriode(JsonNode n) {
        if (n == null || n.isNull()) return null;
        return new Conditie.GeheelPeriode(bool(n, "jaar"), bool(n, "niet"));
    }

    // --- resolved IR lists -------------------------------------------------

    private ResolvedRegel resolvedRegel(JsonNode n) {
        List<String> bronAnnotaties = n.has("bronAnnotaties") ? stringList(n.get("bronAnnotaties")) : List.of();
        ResolvedRegel r = new ResolvedRegel(text(n, "naam"), enumOf(ResolvedRegel.Soort.class, n, "soort"),
                expr(n.get("doel")), expr(n.get("waarde")), conditie(n.get("voorwaarde")),
                geldigheid(n), bronAnnotaties);
        // WF-022's value clause: a characteristic attribution writes the boolean constant true. Neither branch
        // reads the value — the exec-model one writes true whatever it says, the DRL-text one writes what it
        // says — so on an `onwaar` (measured 2026-09-23 on the negatief wf022-kenmerk-onwaar, verifier bypassed)
        // they gave opposite answers where the ASP backend refuses. Refused here, once for both branches.
        if (r.soort() == ResolvedRegel.Soort.KENMERKTOEKENNING
                && !(r.waarde() instanceof Expr.Literal l && "waar".equals(l.waarde())
                     && l.type() instanceof Datatype.Booleaan)) {
            String doel = r.doel() instanceof Expr.AttribuutRef a ? "'" + a.attribuut() + "'" : "zijn doel";
            String waarde = r.waarde() instanceof Expr.Literal l ? "'" + l.waarde() + "'"
                    : r.waarde() == null ? "geen waarde" : "een " + r.waarde().getClass().getSimpleName();
            throw new Refusal("ResolvedRegel.waarde", "de kenmerktoekenning '" + r.naam() + "' kent het kenmerk "
                    + doel + " " + waarde + " toe; WF-022 eist de booleaanse constante waar, en een negatief besluit"
                    + " over een kenmerk bestaat alleen in een defaultsgroep");
        }
        return r;
    }

    private ResolvedObjectCreatie objectCreatie(JsonNode n) {
        List<ResolvedObjectCreatie.Toekenning> tk = new ArrayList<>();
        forEach(n.get("toekenningen"), t -> tk.add(new ResolvedObjectCreatie.Toekenning(
                text(t, "attribuut"), expr(t.get("waarde")), bool(t, "kenmerk"))));
        ResolvedObjectCreatie oc = new ResolvedObjectCreatie(text(n, "naam"), ot(text(n, "nieuwType")),
                ot(text(n, "subject")), tk, conditie(n.get("voorwaarde")), geldigheid(n));
        // IR-SPEC ch. 3.3: the symbolic verbindendeRol is NOT a field of the (vendored, pinned)
        // ResolvedObjectCreatie record, so it cannot be reconstructed INTO the model the way a feitcreatie's
        // joinRol can. It is not emitter-derived either — ch. 5.5 makes it AUTHORITATIVE (the reference
        // interpreter lets it win over the type-pair derivation, and it is the only route for a self-typed
        // pair) — so it rides BESIDE the model instead of being dropped by the authority paragraph above.
        RolBindingRef vr = rolBinding(n.get("verbindendeRol"));
        if (vr != null) rolBindingen.put(oc, vr);
        return oc;
    }

    private ResolvedVerdeling verdeling(JsonNode n) {
        ResolvedVerdeling v = new ResolvedVerdeling(text(n, "naam"), ot(text(n, "bronType")), text(n, "bronAttribuut"),
                ot(text(n, "doelType")), text(n, "doelAttribuut"),
                enumOf(ResolvedVerdeling.Methode.class, n, "methode"), text(n, "ratoAttribuut"),
                text(n, "maximumAttribuut"), integerOf(n, "afrondingDecimalen"), text(n, "restantAttribuut"),
                text(n, "groepeerAttribuut"), bool(n, "afnemend"), conditie(n.get("voorwaarde")),
                geldigheid(n));
        // IR-SPEC ch. 3.2: the recipient role binding decides the join (P3). Not a field of the vendored
        // record, so it rides beside the model like the objectcreatie's; see Loaded#rolBindingen.
        RolBindingRef vr = rolBinding(n.get("verbindendeRol"));
        if (vr != null) rolBindingen.put(v, vr);
        return v;
    }

    private ResolvedConsistentie consistentie(JsonNode n) {
        return new ResolvedConsistentie(text(n, "naam"), ot(text(n, "subject")), conditie(n.get("assertie")),
                bron(n), intOf(n, "line"), geldigheid(n));
    }

    private ResolvedUniciteit uniciteit(JsonNode n) {
        return new ResolvedUniciteit(text(n, "naam"), ot(text(n, "subject")), stringList(n.get("sleutels")),
                text(n, "scopeVeld"), bron(n), geldigheid(n), rolBinding(n.get("scopeRol")));
    }

    private ResolvedUniciteitUnie uniciteitUnie(JsonNode n) {
        List<ResolvedUniciteitUnie.Bron> bronnen = new ArrayList<>();
        forEach(n.get("bronnen"), b -> bronnen.add(new ResolvedUniciteitUnie.Bron(
                ot(text(b, "type")), stringList(b.get("sleutels")))));
        return new ResolvedUniciteitUnie(text(n, "naam"), bronnen, bron(n), geldigheid(n));
    }

    private ResolvedFeitCreatie feitCreatie(JsonNode n) {
        List<ResolvedFeitCreatie.Tussenstap> ts = new ArrayList<>();
        forEach(n.get("tussenstappen"), t -> ts.add(new ResolvedFeitCreatie.Tussenstap(
                ot(text(t, "type")), text(t, "veld"), bool(t, "viaOuderVeld"), rolBinding(t.get("rolBinding")))));
        if (n.has("feittype") && !n.get("feittype").isNull()) {
            return new ResolvedFeitCreatie(text(n, "naam"), ot(text(n, "anker")), ot(text(n, "doel")),
                    text(n, "zetVeld"), text(n, "joinVeld"), ts, conditie(n.get("voorwaarde")),
                    geldigheid(n), text(n, "feittype"), text(n, "ankerRol"), text(n, "doelRol"),
                    rolBinding(n.get("joinRol")));
        }
        return new ResolvedFeitCreatie(text(n, "naam"), ot(text(n, "anker")), ot(text(n, "doel")),
                text(n, "zetVeld"), text(n, "joinVeld"), ts, conditie(n.get("voorwaarde")),
                geldigheid(n));
    }

    /** One navigation hop off the wire, refused BY NAME when malformed — null, not a RolBinding object, or without a
     *  string {@code feittype} / {@code rol}. The verifier and ref reject the same document at load naming the field;
     *  read leniently it would surface only as an unresolved-navigation skip in the transpiler, far from its cause. */
    private static RolBindingRef navigatieStap(String vorm, JsonNode s) {
        if (s == null || !s.isObject() || !"RolBinding".equals(text(s, "node")))
            throw new Refusal(vorm, "malformed: each navigation hop is a RolBinding object, never null");
        for (String veld : new String[] { "feittype", "rol" })
            if (!s.hasNonNull(veld) || !s.get(veld).isTextual())
                throw new Refusal(vorm, "malformed RolBinding: missing required field '" + veld + "'");
        return new RolBindingRef(s.get("feittype").asText(), s.get("rol").asText());
    }

    private RolBindingRef rolBinding(JsonNode n) {
        if (n == null || n.isNull()) return null;
        return new RolBindingRef(text(n, "feittype"), text(n, "rol"));
    }

    /** The validity window of {@code eigenaar} — read from ITS node, not from a detached child, so a
     *  malformed bound can be reported against the declaration that carries it. A document holds hundreds of
     *  windows (848 across the corpus as it stands) and nine node kinds carry one, so "vanaf" alone does not
     *  locate the defect. This mirrors {@link #bron(JsonNode)} one method below, which reads its own field
     *  the same way.
     *  <p>The prefix falls back to the NODE KIND where a carrier has none: {@code naam} is
     *  {@code ["string","null"]} on all nine carrying node kinds, so a nameless carrier is a legal wire
     *  document, and a bare "geldigheid.vanaf" would locate nothing among the hundreds. No corpus document
     *  exercises this today (measured 2026-09-11: 0 of 424 carriers in golden plus rood), which is exactly
     *  why it is written down and asserted rather than left to be discovered. */
    private Geldigheid geldigheid(JsonNode eigenaar) {
        JsonNode n = eigenaar.get("geldigheid");
        if (n == null || n.isNull()) return null;
        String naam = text(eigenaar, "naam"), soort = text(eigenaar, "node");
        String plek = (naam != null ? naam : "<unnamed " + (soort == null ? "carrier" : soort) + ">")
                + ".geldigheid";
        String vanaf = text(n, "vanaf"), tot = text(n, "tot");
        return new Geldigheid(vanaf == null ? null : datumVanIrWire(vanaf, plek + ".vanaf"),
                tot == null ? null : datumVanIrWire(tot, plek + ".tot"));
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

    private Bron bron(JsonNode n) {
        JsonNode b = n.get("bron");
        return Bron.synthetisch(b == null || b.isNull() ? "" : b.asText());
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

    private static BigDecimal decimal(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v == null || v.isNull() ? null : new BigDecimal(v.asText());
    }

    private static List<String> stringList(JsonNode n) {
        List<String> uit = new ArrayList<>();
        if (n != null && n.isArray()) n.forEach(e -> uit.add(e.asText()));
        return uit;
    }

    /** The member a wire value names — or a hard error naming the place, the value and the members, never the
     *  JDK's "No enum constant" sentence, which names a Java class and no place in the document. Absent is null. */
    private static <E extends Enum<E>> E enumOf(Class<E> type, String waarde, String plek) {
        if (waarde == null) return null;
        for (E e : type.getEnumConstants()) if (e.name().equals(waarde)) return e;
        throw new Documentfout(plek, "'" + waarde + "' is not one of "
                + java.util.Arrays.stream(type.getEnumConstants()).map(Enum::name).toList());
    }

    /** {@link #enumOf(Class, String, String)} on a v0 node, which this loader reads without a JSON position: the
     *  place is the node kind and the field. */
    private static <E extends Enum<E>> E enumOf(Class<E> type, JsonNode n, String veld) {
        String soort = text(n, "node");
        return enumOf(type, text(n, veld), (soort == null ? "?" : soort) + "." + veld);
    }

    private static void forEach(JsonNode array, java.util.function.Consumer<JsonNode> f) {
        if (array != null && array.isArray()) array.forEach(f);
    }

    private static void forEachField(JsonNode obj, java.util.function.BiConsumer<String, JsonNode> f) {
        if (obj == null || !obj.isObject()) return;
        for (Map.Entry<String, JsonNode> e : obj.properties()) f.accept(e.getKey(), e.getValue());
    }
}
