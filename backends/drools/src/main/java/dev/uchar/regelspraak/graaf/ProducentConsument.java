package dev.uchar.regelspraak.graaf;

import dev.uchar.regelspraak.ir.Conditie;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.ResolvedRegel;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.transpiler.kern.Namen;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The pure-IR producer/consumer analysis over a resolved {@link RegelSpraakModel}: which rule WRITES which
 * kenmerk/attribuut ({@link #producentIndex}) and which kenmerken/attributen a rule READS ({@link #verbruikt}).
 *
 * <p>It walks the IR records only (no Drools), so a model the loader rebuilt from {@code ir.json} and a model a
 * resolver built from source give the same answers. It is the one authority for its two readers: the
 * {@link RegelGraaf} dataflow graph, and a scope-completeness check that asks whether a consumed element has a
 * producer outside a rule scope. Names are compared as the verifier compares them ({@link Namen#norm}), so a
 * case-only spelling difference still matches producer to consumer and the element identity is the node identity
 * of IR-SPEC ch. 5.1.
 */
public final class ProducentConsument {

    private ProducentConsument() {}

    /** The identity of a consumed/produced model element: the objecttype it lives on (canonical name, or ""
     *  when the owner is unknown), its kenmerk/attribuut naam, and whether it is a kenmerk (§13.4.5 boolean
     *  property) rather than an ordinary attribuut. Objecttype and naam are held in {@link Namen#norm} form.
     *
     *  <p><b>The objecttype is the member's DECLARING type</b> (IR-SPEC ch. 2.2), not the type the rule that
     *  touched it happens to range over — see {@link #elementVan}. A member is declared once and inherited, so
     *  a write on the supertype and a read on the subtype touch ONE element and must key alike. */
    public record ElementSleutel(String objecttype, String naam, boolean kenmerk) {
        public static ElementSleutel van(String objecttype, String naam, boolean kenmerk) {
            return new ElementSleutel(Namen.norm(objecttype), Namen.norm(naam), kenmerk);
        }
    }

    /**
     * The element identity of member {@code naam} touched through {@code houder} — keyed by the type on
     * {@code houder}'s chain that DECLARES it (IR-SPEC ch. 2.2), so producer and consumer meet on one key.
     *
     * <p>This is the whole ⊑-awareness of this analysis, and it sits HERE rather than in each reader because
     * both readers ask the same question of the same index: the scope check matches a consumed element against
     * the producer index, and {@link RegelGraaf} decides INVOER / eind-output by the same membership. Keying on
     * the rule's own subject type instead makes a producer on the supertype and a consumer on the subtype two
     * different elements: the scope check then concludes "no producer at all, so this is a genuine request
     * input" and lets a scope through whose kenmerk silently evaluates false, and the graph draws the same
     * member twice — once as INVOER nobody writes, once as an eind-output nobody reads. Both claims are false.
     *
     * <p>Both directions collapse onto the declarant, deliberately. Producer-on-supertype / consumer-on-subtype
     * is the full-coverage case. Producer-on-subtype / consumer-on-supertype is PARTIAL coverage (that producer
     * only serves subtype instances), and it still keys the same: the scope check's question is whether a
     * producer EXISTS that fell outside the scope, and a partial producer outside the scope is one. Chain
     * awareness here can therefore only ever report MORE than the flat reading, never silence it.
     *
     * <p>The chain walk reads the OWN declaration maps per type on purpose, because the question is WHICH type
     * declares the member, and it is kind-aware: a kenmerk and an attribuut may legally share a name on
     * different types of one chain, and the model's own {@code declarantVan} answers for both kinds at once. A
     * member nothing on the chain declares keeps its holder, which is the reading from before this rule existed.
     */
    public static ElementSleutel elementVan(RegelSpraakModel.Objecttype houder, String naam, boolean kenmerk) {
        if (houder == null) return ElementSleutel.van("", naam, kenmerk);
        return ElementSleutel.van(declarant(houder, naam, kenmerk).naam, naam, kenmerk);
    }

    private static RegelSpraakModel.Objecttype declarant(RegelSpraakModel.Objecttype houder, String naam,
                                                         boolean kenmerk) {
        if (naam == null) return houder;
        for (RegelSpraakModel.Objecttype t : houder.keten())
            if (kenmerk ? t.kenmerken.stream().anyMatch(k -> zelfdeNaam(k, naam))
                        : t.attributen.keySet().stream().anyMatch(a -> zelfdeNaam(a, naam)))
                return t;
        return houder;
    }

    /**
     * The model-wide producer index: for every value-writing {@link ResolvedRegel} (a kenmerktoekenning,
     * gelijkstelling, berekening or initialisatie), map the element it WRITES (its {@code doel}) to the set of
     * rule base-names that write it. A kenmerk and an attribuut of the same spelling are distinct keys.
     */
    public static Map<ElementSleutel, Set<String>> producentIndex(RegelSpraakModel model) {
        Map<ElementSleutel, Set<String>> uit = new LinkedHashMap<>();
        for (ResolvedRegel r : model.resolvedRegels) {
            if (!(r.doel() instanceof Expr.AttribuutRef ref)) continue;
            boolean kenmerk = r.soort() == ResolvedRegel.Soort.KENMERKTOEKENNING;
            uit.computeIfAbsent(elementVan(ref.objecttype(), ref.attribuut(), kenmerk),
                    k -> new LinkedHashSet<>()).add(basisNaam(r.naam()));
        }
        return uit;
    }

    /**
     * The object type a navigated kenmerk check reaches, as one route to a model records it. The check reads the
     * kenmerk of the object its hop reaches (IR-SPEC ch. 4.5), and its record does not name that type: a model the
     * loader rebuilt from the wire carries the hop beside the model, and a resolver's record names the role the
     * hop walks. The analysis cannot tell which route built its input, so the caller answers with the route's own
     * {@link #bronNavigatie} or {@link #wireNavigatie}; the answer of the other route reads every navigated check
     * as the subject's own kenmerk.
     */
    @FunctionalInterface
    public interface Navigatie {
        /** The type {@code check}'s hop reaches from {@code onderwerp}, or null when the check does not navigate. */
        RegelSpraakModel.Objecttype bereikt(Conditie.Kenmerk check, RegelSpraakModel.Objecttype onderwerp);
    }

    /** The answer of a resolver's model: the record names the role its hop walks, and the reached type is the
     *  filler of the role of that name in a relation the subject takes part in, found as the emitter finds the
     *  binding it writes for the hop. A navigated check whose role reaches no unique type fails loudly, as the
     *  emitter refuses to write it. */
    public static Navigatie bronNavigatie(RegelSpraakModel model) {
        return (k, onderwerp) -> {
            if (k.navigatie() == null) return null;
            RegelSpraakModel.Objecttype bereikt = model.rolDoeltypeNaarSubject(onderwerp, k.navigatie());
            if (bereikt == null)
                throw new IllegalStateException("the characteristic check '" + k.kenmerk() + "' navigates by the role '"
                        + k.navigatie() + "', which reaches no unique object type from "
                        + (onderwerp == null ? "an unknown subject" : "'" + onderwerp.naam + "'"));
            return bereikt;
        };
    }

    /** The answer of a model the loader rebuilt: the hop rides beside the model ({@code navigatieBindingen}), and
     *  its binding names the role the reached object plays, whose declared filler is the reached type. A binding
     *  that names no declared role with a declared filler fails loudly; the verifier refuses such a document
     *  (WF-053). */
    public static Navigatie wireNavigatie(IrLoader.Loaded geladen) {
        RegelSpraakModel model = geladen.model();
        Map<Object, List<RegelSpraakModel.RolBindingRef>> hops = geladen.navigatieBindingen();
        return (k, onderwerp) -> {
            List<RegelSpraakModel.RolBindingRef> pad = hops.get(k);
            if (pad == null || pad.isEmpty()) return null;
            RegelSpraakModel.RolBindingRef hop = pad.get(pad.size() - 1);
            for (RegelSpraakModel.Feittype ft : model.feittypen.values())
                if (zelfdeNaam(ft.naam, hop.feittype()))
                    for (RegelSpraakModel.Rol rol : ft.rollen)
                        if (rol.naam() != null && zelfdeNaam(rol.naam(), hop.rol())) {
                            RegelSpraakModel.Objecttype bereikt = model.objecttypeOpNaam(rol.objecttypeNaam());
                            if (bereikt != null) return bereikt;
                        }
            throw new IllegalStateException("the characteristic check '" + k.kenmerk() + "' navigates by the role '"
                    + hop.rol() + "' of '" + hop.feittype() + "', which names no declared role with a declared filler");
        };
    }

    /** The kenmerken/attributen a resolved rule CONSUMES: every kenmerk check and attribuut read in its
     *  voorwaarde and in its RHS {@code waarde}, at any depth — the conditions inside an expression included (an
     *  aggregation's filter, a branch of an instance-set union, the guard of an if-then-else), each with the
     *  subject it is evaluated against. The rule's OWN write target ({@code doel}) is not scanned — a rule
     *  reading what it writes is not a cross-rule dependency. The walk reads what IR-SPEC ch. 5.1 lets a rule
     *  read of attributes and characteristics, node kind by node kind, and each switch below is exhaustive, so a
     *  node kind the IR records gain later fails to compile here until someone decides what it reads.
     *
     *  <p>A navigated kenmerk check reads the kenmerk of the type its hop reaches, which is {@code navigatie}'s
     *  answer (see {@link Navigatie}). */
    public static Set<ElementSleutel> verbruikt(ResolvedRegel r, Navigatie navigatie) {
        Set<ElementSleutel> uit = new LinkedHashSet<>();
        Lezer lezer = new Lezer(navigatie, uit);
        RegelSpraakModel.Objecttype subject = r.doel() instanceof Expr.AttribuutRef ref ? ref.objecttype() : null;
        lezer.conditie(r.voorwaarde(), subject);
        lezer.expr(r.waarde(), subject);
        return uit;
    }

    /** The one walk behind {@link #verbruikt}, over a condition or an expression with the subject a kenmerk check
     *  without an owner of its own is evaluated against, and from which a navigated check's hop starts. Only a
     *  kenmerk check asks for the subject; an attribuut read names its type itself. */
    private record Lezer(Navigatie navigatie, Set<ElementSleutel> uit) {

        void conditie(Conditie c, RegelSpraakModel.Objecttype subject) {
            if (c == null) return;
            switch (c) {
                case Conditie.Vergelijking v -> { expr(v.links(), subject); expr(v.rechts(), subject); }
                case Conditie.Predicaat p -> expr(p.operand(), subject);
                case Conditie.Periode p -> { expr(p.vanaf(), subject); expr(p.tot(), subject); }
                // A navigated check lives on the object its hop reaches; any other check lives on its host, or on
                // the subject when it names none.
                case Conditie.Kenmerk k -> {
                    RegelSpraakModel.Objecttype bereikt = navigatie.bereikt(k, subject);
                    RegelSpraakModel.Objecttype drager =
                            bereikt != null ? bereikt : k.eigenaar() != null ? k.eigenaar() : subject;
                    uit.add(elementVan(drager, k.kenmerk(), true));
                }
                case Conditie.Samengesteld s -> { for (Conditie d : s.onderdelen()) conditie(d, subject); }
                // IR-SPEC ch. 4.5: a quantifier fold's filter and matrix are evaluated with the ELEMENT as subject
                // (RUNTIME-SEMANTICS ch. 3.9a), hence the element type rather than the enclosing subject.
                case Conditie.Kwantorvouw kv -> {
                    conditie(kv.filter(), kv.elementObjecttype());
                    conditie(kv.toets(), kv.elementObjecttype());
                }
                // A role check reads a relation's link set and a rule-version test a channel: nodes of IR-SPEC
                // ch. 5.1 this graph does not draw, so neither contributes an element.
                case Conditie.Rollidmaatschap ignored -> { }
                case Conditie.VerreRollidmaatschap ignored -> { }
                case Conditie.Regelversie ignored -> { }
                case Conditie.Onopgelost ignored -> { }
            }
        }

        void expr(Expr e, RegelSpraakModel.Objecttype subject) {
            if (e == null) return;
            switch (e) {
                case Expr.AttribuutRef ref -> uit.add(elementVan(ref.objecttype(), ref.attribuut(), false));
                case Expr.Binair b -> { expr(b.links(), subject); expr(b.rechts(), subject); }
                case Expr.Functie f -> {
                    // A count's source names the relation's ROLE, never a member of the element type (the loader
                    // refuses a member there): it reads the link set, not an attribuut of each contributor.
                    if (!isTelbron(f)) for (Expr a : f.argumenten()) expr(a, subject);
                    expr(f.leegwaarde(), subject);
                    // the filter is evaluated per contributor, with the element as subject
                    conditie(f.filter(), f.elementObjecttype() != null ? f.elementObjecttype() : subject);
                }
                // each selection's filter is evaluated per contributor, as a fold's filter is
                case Expr.InstanceSetUnion u -> {
                    for (Expr.InstanceSetUnion.Tak t : u.takken()) conditie(t.filter(), t.elementType());
                }
                case Expr.Afgerond a -> expr(a.basis(), subject);
                case Expr.Begrensd b -> { expr(b.basis(), subject); expr(b.minimum(), subject); expr(b.maximum(), subject); }
                case Expr.DimensieCel c -> expr(c.attribuut(), subject);
                case Expr.TekstReeks t -> {
                    for (Expr.TekstReeks.Deel d : t.delen())
                        if (d instanceof Expr.TekstReeks.Ingevoegd i) expr(i.expr(), subject);
                }
                // Call-by-need does not narrow this: a value that is never evaluated is still a read the author
                // wrote, and IR-SPEC ch. 5 draws the edge for it as well (a static over-approximation).
                case Expr.Let l -> { expr(l.value(), subject); expr(l.body(), subject); }
                case Expr.IfThenElse i -> { conditie(i.conditie(), subject); expr(i.dan(), subject); expr(i.anders(), subject); }
                case Expr.Literal ignored -> { }
                case Expr.ParameterRef ignored -> { }
                case Expr.VariabeleRef ignored -> { }
                case Expr.BinderRef ignored -> { }
                case Expr.Constante ignored -> { }
                case Expr.Onopgelost ignored -> { }
            }
        }
    }

    /** A count over one source whose argument names no member of the element type: the count source of IR-SPEC
     *  ch. 4.3, which names the relation's role. */
    private static boolean isTelbron(Expr.Functie f) {
        return f.kind() == Expr.FunctieKind.AANTAL && f.elementObjecttype() != null && f.argumenten().size() == 1
                && f.argumenten().get(0) instanceof Expr.AttribuutRef bron && bron.attribuut() != null
                && f.elementObjecttype().attribuut(bron.attribuut()) == null
                && f.elementObjecttype().kenmerkNaam(bron.attribuut()) == null;
    }

    private static boolean zelfdeNaam(String a, String b) {
        return Namen.norm(a).equals(Namen.norm(b));
    }

    /** Strip a §9.10 "&lt;groep&gt;: " prefix and a §13.4.2.3 " #k" version suffix (and a beslistabel " (n)"
     *  multi-conclusie suffix) off an emitted rule name — the base name the index, a rule scope, and the
     *  transpiler dekking all agree on. */
    public static String basisNaam(String naam) {
        if (naam == null) return "";
        String n = naam;
        int hash = n.indexOf(" #");
        if (hash > 0) n = n.substring(0, hash);
        int colon = n.indexOf(": ");
        if (colon > 0) n = n.substring(colon + 2);
        n = n.replaceFirst("\\s*\\(\\d+\\)\\s*$", "");
        return n.trim();
    }
}
