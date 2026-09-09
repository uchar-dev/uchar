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
 * wire, exactly as ref/ and backends/clingo do. This loader parses the {@code regelspraak-ir-v0} JSON
 * into the SAME frontend record types the transpiler consumes, so the ported {@code ExecModelTranspiler}
 * fires the IR — never the RegelSpraak source.
 *
 * <p><b>Refusal discipline.</b> Every wire form the v0 Drools backend does not carry is a LOUD, per-form
 * {@link Refusal} — never a silent skip. That covers a non-v0 {@code formaat}, an unknown top-level
 * section (a v1 extension: {@code prioritizedDefaults}, {@code functionDefs}, …), and an unknown {@code node}
 * discriminator inside the {@link Expr} / {@link Conditie} / {@link Datatype} sealed hierarchies
 * (a struct/list literal, a MapList/Invoke, …). The differential gate turns a Refusal into a
 * reason-carrying exclusion.
 *
 * <p><b>Authority.</b> The v0 BEAN fields are authoritative: where the emitter also writes a symbolic
 * extension that is a record field (a feitcreatie's {@code feittype/ankerRol/doelRol/joinRol}, a
 * uniciteit's {@code scopeRol}, a tussenstap's {@code rolBinding}) it is reconstructed faithfully; where
 * the extension is purely emitter-derived and NOT a record field (an AttribuutRef's
 * {@code navigatieBindingen}, a Functie's {@code bronRol}, a Kenmerk's {@code navigatieBinding}, a
 * verdeling's {@code verbindendeRol}) it is ignored — the transpiler re-derives it from the model. The
 * additive {@code bronAnnotaties} provenance field is carried through.
 *
 * <p>TWO extensions escape that dichotomy, both because the model cannot ALWAYS re-derive them: an
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

    private static final java.util.Set<String> TOP_LEVEL = java.util.Set.of(
            "formaat", "objecttypen", "domeinen", "parameters", "eenheidstelsels", "feittypen",
            "dimensies", "tijdlijnen", "dagsoorten", "dagsoortPerEenheid", "regels", "resolvedRegels",
            "resolvedDagsoorten", "objectCreaties", "verdelingen", "consistenties", "uniciteiten",
            "uniciteitUnies", "feitCreaties", "resolvedStartpunten", "regelGroepen", "diagnostics",
            "prioritizedDefaults");

    private final RegelSpraakModel m = new RegelSpraakModel();
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
    /** The ch. 4.10 {@code IfThenElse}, keyed by its {@link Expr.Onopgelost} carrier — see
     *  {@link Loaded#keuzes()}. IDENTITY-keyed for the same reason as {@link #rolBindingen}, and more sharply so:
     *  two carriers of one datatype and one bron are VALUE-equal records. */
    private final Map<Object, Keuze> keuzes = new java.util.IdentityHashMap<>();

    private IrLoader() {}

    /** A loaded document: the v0 resolved {@link RegelSpraakModel}, the ch. 7 prioritized-defaults extension, and
     *  the symbolic role bindings the wire carries beside the v0 bean fields. All three ride BESIDE the model —
     *  {@code RegelSpraakModel} is the faithful v0 shape, and the frontend records that would host these
     *  extensions belong to a VENDORED slice the toolchain consumes at a pin, so they cannot grow a field here.
     *  A consumer that needs them reads them from here (see {@link #loadAll}).
     *
     *  @param rolBindingen the symbolic (feittype, rol) binding per loaded IR NODE, by IDENTITY. Two node kinds
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
     *         </ul>
     *         In both cases the SIBLING role — the one the anchor (subject) plays — is the bean field on the other
     *         end. Absent for a node whose wire record carries no binding; the by-pair derivation then stands.
     *  @param lets an INDEX of the {@link Expr.Let} nodes in the document, in parse order — not a channel: the Lets
     *         themselves sit in the expression trees as ordinary union nodes, and every consumer lowers and walks
     *         them there. The index exists for the one consumer that must start a walk at a Let without holding the
     *         tree it came from ({@code EffectieveEenheid}'s binder-read table, which answers the unit of a
     *         {@link Expr.BinderRef} asked in isolation). Empty for a document that carries no Let.
     *  @param keuzes the IR-SPEC ch. 4.10 {@code IfThenElse} per {@link Expr.Onopgelost} CARRIER, by IDENTITY — the
     *         one Let-cluster node the vendored {@link Expr} union does NOT carry (it grew {@code Let} and
     *         {@code BinderRef} with the RegelSpraak emitter's §13.4.2.10 lowering, but {@code IfThenElse} is
     *         emitter-free, IR-SPEC ch. 4.10). Extending that union would mean editing the shared frontend slice the
     *         toolchain consumes at a pin, so this one form still parses into a carrier plus its shape here (see
     *         {@link Keuze}). Empty for a document that carries none. */
    public record Loaded(RegelSpraakModel model, List<PrioritizedDefaults> prioritizedDefaults,
                         Map<Object, RolBindingRef> rolBindingen, List<Expr.Let> lets,
                         Map<Object, Keuze> keuzes) {}

    /**
     * IR-SPEC ch. 4.10's {@code IfThenElse} (semantics {@code RUNTIME-SEMANTICS.md} ch. 10.2): the value-level
     * conditional — evaluate the condition under the ordinary comparison discipline (its operands disciplined to
     * bound-non-leeg reads by WF-077), then evaluate EXACTLY the chosen branch, strict, no coalesce.
     *
     * <p>Its two siblings, {@code Let} and {@code BinderRef}, are ordinary {@link Expr} union nodes, so they need no
     * shape here and no channel: they ride in the tree and every generic walk descends them. This one cannot, for
     * the reason given at {@link Loaded#keuzes()} — so it keeps a carrier, and what that costs is stated where the
     * cost lands: a carrier is an opaque LEAF to a generic {@link Expr} walk, so a walk that must see through it
     * takes {@code ExecModelTranspiler.letKinderen}, which descends both branches and every Expr operand of the
     * condition. A walk that does not descend under-reports in SILENCE. On the VALUE paths the carrier is safe by
     * construction — nothing lowers an {@code Onopgelost}, so a missed consultation degrades to a LOUD,
     * reason-carrying skip in the gate report.
     */
    public record Keuze(Conditie conditie, Expr dan, Expr anders, Datatype type) {}

    /** Parse a {@code regelspraak-ir-v0} tree into a resolved model, or throw {@link Refusal} for a form
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
        return new Loaded(model, l.prioritizedDefaults, l.rolBindingen, l.lets, l.keuzes);
    }

    private RegelSpraakModel model(JsonNode root) {
        String formaat = text(root, "formaat");
        if (!"regelspraak-ir-v0".equals(formaat))
            throw new Refusal("formaat", String.valueOf(formaat));
        root.fieldNames().forEachRemaining(k -> {
            if (!TOP_LEVEL.contains(k)) throw new Refusal(k, "unknown top-level section");
        });

        // declarations first: the resolved lists reference them by name.
        forEachField(root.get("objecttypen"), (naam, n) -> m.objecttypen.put(naam, objecttype(naam, n)));
        forEachField(root.get("domeinen"), (naam, n) -> m.domeinen.put(naam, datatype(n)));
        forEachField(root.get("parameters"), (naam, n) -> m.parameters.put(naam, parameter(n)));
        forEachField(root.get("eenheidstelsels"), (naam, n) -> m.eenheidstelsels.put(naam, eenheidstelsel(naam, n)));
        forEachField(root.get("feittypen"), (naam, n) -> m.feittypen.put(naam, feittype(naam, n)));
        forEachField(root.get("dimensies"), (naam, n) -> m.dimensies.put(naam, dimensie(n)));
        forEach(root.get("tijdlijnen"), n -> m.tijdlijnen.add(tijdlijn(n)));
        forEach(root.get("dagsoorten"), n -> m.dagsoorten.add(n.asText()));
        forEachField(root.get("dagsoortPerEenheid"), (k, v) -> m.dagsoortPerEenheid.put(k, v.asText()));
        forEach(root.get("regels"), n -> m.regels.add(new RegelSpraakModel.Regel(text(n, "naam"), text(n, "soort"))));

        forEach(root.get("resolvedRegels"), n -> m.resolvedRegels.add(resolvedRegel(n)));
        forEach(root.get("resolvedDagsoorten"), n -> m.resolvedDagsoorten.add(
                new ResolvedDagsoort(text(n, "naam"), conditie(n.get("voorwaarde")), geldigheid(n.get("geldigheid")))));
        forEach(root.get("objectCreaties"), n -> m.objectCreaties.add(objectCreatie(n)));
        forEach(root.get("verdelingen"), n -> m.verdelingen.add(verdeling(n)));
        forEach(root.get("consistenties"), n -> m.consistenties.add(consistentie(n)));
        forEach(root.get("uniciteiten"), n -> m.uniciteiten.add(uniciteit(n)));
        forEach(root.get("uniciteitUnies"), n -> m.uniciteitUnies.add(uniciteitUnie(n)));
        forEach(root.get("feitCreaties"), n -> m.feitCreaties.add(feitCreatie(n)));
        forEach(root.get("resolvedStartpunten"), n -> m.resolvedStartpunten.add(
                new ResolvedStartpunt(text(n, "tijdlijn"), text(n, "objecttype"), text(n, "lid"))));
        forEach(root.get("regelGroepen"), n -> m.regelGroepen.add(new ResolvedRegelGroep(
                text(n, "naam"), bool(n, "recursief"), stringList(n.get("leden")), bron(n), intOf(n, "line"))));
        forEach(root.get("diagnostics"), n -> m.diagnostics.add(new Diagnostic(
                enumOf(Severity.class, text(n, "severity")), text(n, "message"), intOf(n, "line"))));
        // §6.1 (Phase 4) the prioritized-defaults extension, parsed AFTER the declarations so a default's
        // condition/value resolves its objecttype/parameter references against the fully-built model. A malformed
        // defaults bundle (an unknown inner node, a missing target) raises a loud Refusal from these parsers.
        forEach(root.get("prioritizedDefaults"), n -> prioritizedDefaults.add(prioritizedDefaults(n)));
        return m;
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
                defaults, text(n, "bron"), geldigheid(n.get("geldigheid")));
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

    // --- declaration side --------------------------------------------------

    private Objecttype objecttype(String naam, JsonNode n) {
        Objecttype ot = new Objecttype(text(n, "naam") != null ? text(n, "naam") : naam);
        ot.bezield = bool(n, "bezield");
        ot.meervoud = text(n, "meervoud");
        forEachField(n.get("attributen"), (k, v) -> ot.attributen.put(k, datatype(v)));
        forEachField(n.get("attribuutMeervoud"), (k, v) -> ot.attribuutMeervoud.put(k, v.asText()));
        forEachField(n.get("attribuutDimensies"), (k, v) -> ot.attribuutDimensies.put(k, stringList(v)));
        forEachField(n.get("attribuutGranulariteit"), (k, v) -> ot.attribuutGranulariteit.put(k, enumOf(Granulariteit.class, v.asText())));
        forEachField(n.get("attribuutTijdlijnAantal"), (k, v) -> ot.attribuutTijdlijnAantal.put(k, v.asInt()));
        forEachField(n.get("attribuutTijdlijnNaam"), (k, v) -> ot.attribuutTijdlijnNaam.put(k, v.asText()));
        forEach(n.get("kenmerken"), v -> ot.kenmerken.add(v.asText()));
        forEachField(n.get("kenmerkSoorten"), (k, v) -> ot.kenmerkSoorten.put(k, enumOf(KenmerkSoort.class, v.asText())));
        forEachField(n.get("kenmerkGranulariteit"), (k, v) -> ot.kenmerkGranulariteit.put(k, enumOf(Granulariteit.class, v.asText())));
        forEachField(n.get("kenmerkTijdlijnAantal"), (k, v) -> ot.kenmerkTijdlijnAantal.put(k, v.asInt()));
        return ot;
    }

    private Parameter parameter(JsonNode n) {
        return new Parameter(text(n, "naam"), datatype(n.get("datatype")),
                enumOf(Granulariteit.class, text(n, "granulariteit")));
    }

    private Eenheidstelsel eenheidstelsel(String naam, JsonNode n) {
        Eenheidstelsel es = new Eenheidstelsel(text(n, "naam") != null ? text(n, "naam") : naam);
        forEachField(n.get("eenheden"), (k, v) -> es.eenheden.put(k, eenheid(v)));
        forEachField(n.get("aliassen"), (k, v) -> es.aliassen.put(k, eenheid(v)));
        return es;
    }

    private Eenheid eenheid(JsonNode n) {
        if (n == null || n.isNull()) return null;
        return new Eenheid(text(n, "afkorting"), text(n, "stelsel"), text(n, "canoniek"),
                decimal(n, "factorNaarCanoniek"), eenheid(n.get("noemer")));
    }

    private Feittype feittype(String naam, JsonNode n) {
        Feittype ft = new Feittype(text(n, "naam") != null ? text(n, "naam") : naam);
        ft.wederkerig = bool(n, "wederkerig");
        forEach(n.get("rollen"), r -> ft.rollen.add(new RegelSpraakModel.Rol(
                text(r, "naam"), text(r, "objecttypeNaam"), text(r, "meervoud"), bool(r, "meervoudig"))));
        return ft;
    }

    private Dimensie dimensie(JsonNode n) {
        return new Dimensie(text(n, "naam"), text(n, "naamMeervoud"), text(n, "voorzetsel"), stringList(n.get("waarden")));
    }

    private Tijdlijn tijdlijn(JsonNode n) {
        return new Tijdlijn(text(n, "naam"), bool(n, "variabelStartpunt"),
                enumOf(Granulariteit.class, text(n, "granulariteit")), intOf(n, "aantal"));
    }

    // --- Datatype ----------------------------------------------------------

    private Datatype datatype(JsonNode n) {
        if (n == null || n.isNull()) return null;
        String node = text(n, "node");
        return switch (node) {
            case "Numeriek" -> new Datatype.Numeriek(bool(n, "geheel"), intOf(n, "decimalen"),
                    enumOf(Datatype.Teken.class, text(n, "teken")), eenheid(n.get("eenheid")));
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
            case "Binair" -> new Expr.Binair(enumOf(Expr.BinOp.class, text(n, "op")),
                    expr(n.get("links")), expr(n.get("rechts")), datatype(n.get("type")), bron(n));
            case "Functie" -> {
                List<Expr> args = new ArrayList<>();
                forEach(n.get("argumenten"), a -> args.add(expr(a)));
                Expr.Functie f = new Expr.Functie(enumOf(Expr.FunctieKind.class, text(n, "kind")), args,
                        ot(text(n, "elementObjecttype")), expr(n.get("leegwaarde")), conditie(n.get("filter")),
                        dimensieSelectie(n.get("dimensieSelectie")), datatype(n.get("type")), bron(n),
                        enumOf(Granulariteit.class, text(n, "granulariteit")));
                // the aggregation's symbolic bronRol — see Loaded#rolBindingen. It stays OUT of the record (that
                // one is vendored) and the transpiler still re-derives the join by pair FIRST; the binding is the
                // fallback for the pair no by-pair search can disambiguate, a SELF-TYPED one.
                RolBindingRef br = rolBinding(n.get("bronRol"));
                if (br != null) rolBindingen.put(f, br);
                yield f;
            }
            case "Afgerond" -> new Expr.Afgerond(expr(n.get("basis")),
                    enumOf(Expr.AfrondModus.class, text(n, "modus")), intOf(n, "decimalen"), bron(n));
            case "Begrensd" -> new Expr.Begrensd(expr(n.get("basis")), expr(n.get("minimum")), expr(n.get("maximum")), bron(n));
            case "AttribuutRef" -> new Expr.AttribuutRef(text(n, "attribuut"), ot(text(n, "objecttype")),
                    text(n, "onderwerpBron"), text(n, "navigatieRol"), datatype(n.get("type")), bron(n),
                    bool(n, "set"), stringList(n.get("navigatiePad")));
            case "DimensieCel" -> new Expr.DimensieCel((Expr.AttribuutRef) expr(n.get("attribuut")),
                    stringList(n.get("labels")), bron(n));
            case "ParameterRef" -> new Expr.ParameterRef(param(text(n, "parameter")), bron(n));
            case "VariabeleRef" -> new Expr.VariabeleRef(text(n, "naam"), datatype(n.get("type")), bron(n));
            case "Constante" -> new Expr.Constante(text(n, "naam"), datatype(n.get("type")), bron(n));
            case "Onopgelost" -> new Expr.Onopgelost(datatype(n.get("type")), bron(n));
            // IR-SPEC ch. 4.10 the Let cluster. Let and BinderRef are ORDINARY union nodes — the vendored Expr
            // slice grew both when the RegelSpraak emitter learned to lower a §13.4.2.10 variabelendeel — so they
            // are built here and nothing rides beside them. IfThenElse is emitter-free and has no union record, so
            // it alone keeps a carrier (see Loaded#keuzes). Note the wire's field names: a BinderRef's is "name",
            // an IfThenElse's are "condition"/"then"/"else" — the condition reuses the ordinary conditie union.
            case "Let" -> let(new Expr.Let(
                    verplicht("Let", "binder", text(n, "binder")),
                    verplicht("Let", "value", expr(n.get("value"))),
                    verplicht("Let", "body", expr(n.get("body"))),
                    verplicht("Let", "type", datatype(n.get("type"))), bron(n)));
            case "BinderRef" -> new Expr.BinderRef(
                    verplicht("BinderRef", "name", text(n, "name")),
                    verplicht("BinderRef", "type", datatype(n.get("type"))), bron(n));
            case "IfThenElse" -> keuzeDrager(n, new Keuze(
                    verplicht("IfThenElse", "condition", conditie(n.get("condition"))),
                    verplicht("IfThenElse", "then", expr(n.get("then"))),
                    verplicht("IfThenElse", "else", expr(n.get("else"))),
                    verplicht("IfThenElse", "type", datatype(n.get("type")))));
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

    /** Index one {@link Expr.Let} (see {@link Loaded#lets()}) and return it — the node itself goes into the tree. */
    private Expr let(Expr.Let l) {
        lets.add(l);
        return l;
    }

    /** Register one ch. 4.10 {@code IfThenElse} beside the model and return its carrier — a fresh
     *  {@link Expr.Onopgelost} typed with the node's own datatype, so a consumer that never consults
     *  {@link Loaded#keuzes()} sees an unresolved leaf and refuses loudly rather than guessing. */
    private Expr keuzeDrager(JsonNode n, Keuze keuze) {
        Expr drager = new Expr.Onopgelost(keuze.type(), bron(n));
        keuzes.put(drager, keuze);
        return drager;
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
            case "Vergelijking" -> new Conditie.Vergelijking(enumOf(Conditie.Operator.class, text(n, "op")),
                    expr(n.get("links")), expr(n.get("rechts")), geheelPeriode(n.get("periode")), bron(n));
            case "Predicaat" -> new Conditie.Predicaat(enumOf(Conditie.Predicaatsoort.class, text(n, "soort")),
                    expr(n.get("operand")), text(n, "dagsoort"), bool(n, "negatie"), intOf(n, "cijfers"),
                    geheelPeriode(n.get("periode")), bron(n));
            case "Kenmerk" -> new Conditie.Kenmerk(text(n, "kenmerk"), text(n, "navigatie"), bool(n, "negatie"),
                    ot(text(n, "eigenaar")), geheelPeriode(n.get("periode")), bron(n));
            case "Rollidmaatschap" -> new Conditie.Rollidmaatschap(text(n, "rol"), ot(text(n, "anderTeFiller")),
                    bron(n), bool(n, "negatie"));
            case "VerreRollidmaatschap" -> new Conditie.VerreRollidmaatschap(ot(text(n, "filler")),
                    text(n, "backref"), bool(n, "negatie"), bron(n));
            case "Periode" -> new Conditie.Periode(expr(n.get("vanaf")), expr(n.get("tot")), bool(n, "totInclusief"), bron(n));
            case "Samengesteld" -> {
                List<Conditie> delen = new ArrayList<>();
                forEach(n.get("onderdelen"), o -> delen.add(conditie(o)));
                yield new Conditie.Samengesteld(enumOf(Conditie.Kwantor.class, text(n, "kwantor")),
                        intOf(n, "drempel"), delen, bron(n));
            }
            case "Regelversie" -> new Conditie.Regelversie(text(n, "regel"), bool(n, "inconsistent"), bron(n));
            case "Onopgelost" -> new Conditie.Onopgelost(bron(n));
            default -> throw new Refusal("Conditie:" + node, "unknown condition node");
        };
    }

    private Conditie.GeheelPeriode geheelPeriode(JsonNode n) {
        if (n == null || n.isNull()) return null;
        return new Conditie.GeheelPeriode(bool(n, "jaar"), bool(n, "niet"));
    }

    // --- resolved IR lists -------------------------------------------------

    private ResolvedRegel resolvedRegel(JsonNode n) {
        List<String> bronAnnotaties = n.has("bronAnnotaties") ? stringList(n.get("bronAnnotaties")) : List.of();
        return new ResolvedRegel(text(n, "naam"), enumOf(ResolvedRegel.Soort.class, text(n, "soort")),
                expr(n.get("doel")), expr(n.get("waarde")), conditie(n.get("voorwaarde")),
                geldigheid(n.get("geldigheid")), bronAnnotaties);
    }

    private ResolvedObjectCreatie objectCreatie(JsonNode n) {
        List<ResolvedObjectCreatie.Toekenning> tk = new ArrayList<>();
        forEach(n.get("toekenningen"), t -> tk.add(new ResolvedObjectCreatie.Toekenning(
                text(t, "attribuut"), expr(t.get("waarde")), bool(t, "kenmerk"))));
        ResolvedObjectCreatie oc = new ResolvedObjectCreatie(text(n, "naam"), ot(text(n, "nieuwType")),
                ot(text(n, "subject")), tk, conditie(n.get("voorwaarde")), geldigheid(n.get("geldigheid")));
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
        return new ResolvedVerdeling(text(n, "naam"), ot(text(n, "bronType")), text(n, "bronAttribuut"),
                ot(text(n, "doelType")), text(n, "doelAttribuut"),
                enumOf(ResolvedVerdeling.Methode.class, text(n, "methode")), text(n, "ratoAttribuut"),
                text(n, "maximumAttribuut"), integerOf(n, "afrondingDecimalen"), text(n, "restantAttribuut"),
                text(n, "groepeerAttribuut"), bool(n, "afnemend"), conditie(n.get("voorwaarde")),
                geldigheid(n.get("geldigheid")));
    }

    private ResolvedConsistentie consistentie(JsonNode n) {
        return new ResolvedConsistentie(text(n, "naam"), ot(text(n, "subject")), conditie(n.get("assertie")),
                bron(n), intOf(n, "line"), geldigheid(n.get("geldigheid")));
    }

    private ResolvedUniciteit uniciteit(JsonNode n) {
        return new ResolvedUniciteit(text(n, "naam"), ot(text(n, "subject")), stringList(n.get("sleutels")),
                text(n, "scopeVeld"), bron(n), geldigheid(n.get("geldigheid")), rolBinding(n.get("scopeRol")));
    }

    private ResolvedUniciteitUnie uniciteitUnie(JsonNode n) {
        List<ResolvedUniciteitUnie.Bron> bronnen = new ArrayList<>();
        forEach(n.get("bronnen"), b -> bronnen.add(new ResolvedUniciteitUnie.Bron(
                ot(text(b, "type")), stringList(b.get("sleutels")))));
        return new ResolvedUniciteitUnie(text(n, "naam"), bronnen, bron(n), geldigheid(n.get("geldigheid")));
    }

    private ResolvedFeitCreatie feitCreatie(JsonNode n) {
        List<ResolvedFeitCreatie.Tussenstap> ts = new ArrayList<>();
        forEach(n.get("tussenstappen"), t -> ts.add(new ResolvedFeitCreatie.Tussenstap(
                ot(text(t, "type")), text(t, "veld"), bool(t, "viaOuderVeld"), rolBinding(t.get("rolBinding")))));
        if (n.has("feittype") && !n.get("feittype").isNull()) {
            return new ResolvedFeitCreatie(text(n, "naam"), ot(text(n, "anker")), ot(text(n, "doel")),
                    text(n, "zetVeld"), text(n, "joinVeld"), ts, conditie(n.get("voorwaarde")),
                    geldigheid(n.get("geldigheid")), text(n, "feittype"), text(n, "ankerRol"), text(n, "doelRol"),
                    rolBinding(n.get("joinRol")));
        }
        return new ResolvedFeitCreatie(text(n, "naam"), ot(text(n, "anker")), ot(text(n, "doel")),
                text(n, "zetVeld"), text(n, "joinVeld"), ts, conditie(n.get("voorwaarde")),
                geldigheid(n.get("geldigheid")));
    }

    private RolBindingRef rolBinding(JsonNode n) {
        if (n == null || n.isNull()) return null;
        return new RolBindingRef(text(n, "feittype"), text(n, "rol"));
    }

    private Geldigheid geldigheid(JsonNode n) {
        if (n == null || n.isNull()) return null;
        String vanaf = text(n, "vanaf"), tot = text(n, "tot");
        return new Geldigheid(vanaf == null ? null : java.time.LocalDate.parse(vanaf),
                tot == null ? null : java.time.LocalDate.parse(tot));
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

    private static <E extends Enum<E>> E enumOf(Class<E> type, String naam) {
        return naam == null ? null : Enum.valueOf(type, naam);
    }

    private static void forEach(JsonNode array, java.util.function.Consumer<JsonNode> f) {
        if (array != null && array.isArray()) array.forEach(f);
    }

    private static void forEachField(JsonNode obj, java.util.function.BiConsumer<String, JsonNode> f) {
        if (obj == null || !obj.isObject()) return;
        for (Map.Entry<String, JsonNode> e : obj.properties()) f.accept(e.getKey(), e.getValue());
    }
}
