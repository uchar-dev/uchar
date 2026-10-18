package dev.uchar.regelspraak.graaf;

import dev.uchar.regelspraak.ir.Conditie;
import dev.uchar.regelspraak.ir.Default;
import dev.uchar.regelspraak.ir.ExceptionGroup;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.ir.ResolvedConsistentie;
import dev.uchar.regelspraak.ir.ResolvedFeitCreatie;
import dev.uchar.regelspraak.ir.ResolvedObjectCreatie;
import dev.uchar.regelspraak.ir.ResolvedRegel;
import dev.uchar.regelspraak.ir.ResolvedUniciteit;
import dev.uchar.regelspraak.ir.ResolvedUniciteitUnie;
import dev.uchar.regelspraak.ir.ResolvedVerdeling;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.transpiler.kern.Namen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The pure-IR producer/consumer analysis over a resolved {@link RegelSpraakModel}: which form of the rule layer
 * WRITES which kenmerk/attribuut ({@link #producentIndex}) and which kenmerken/attributen a form READS
 * ({@link #vormen}, and {@link #verbruikt} for one rule). The forms are those of IR-SPEC ch. 5.1: rules, object
 * creations, allocations, consistencies, uniquenesses, fact creations and defaults groups.
 *
 * <p>It walks the IR records only (no Drools), so a model the loader rebuilt from {@code ir.json} and a model a
 * resolver built from source give the same answers, once each route supplies what rides beside its model: the
 * defaults-group list, and where a navigated kenmerk check leads ({@link Navigatie}). It is the one authority for
 * its two readers: the {@link RegelGraaf} dataflow graph, and a scope-completeness check that asks whether a
 * consumed element has a producer outside a rule scope. Names are compared as the verifier compares them
 * ({@link Namen#norm}), so a case-only spelling difference still matches producer to consumer and the element
 * identity is the node identity of IR-SPEC ch. 5.1.
 */
public final class ProducentConsument {

    private ProducentConsument() {}

    /** The identity of a consumed/produced model element: the objecttype it lives on (canonical name, or ""
     *  when the owner is unknown), its kenmerk/attribuut naam, and whether it is a kenmerk (§13.4.5 boolean
     *  property) rather than an ordinary attribuut. Objecttype and naam are held in {@link Namen#norm} form.
     *
     *  <p><b>The objecttype is the member's DECLARING type</b> (IR-SPEC ch. 2.2), not the type the form that
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
            if (kenmerk ? t.kenmerken.stream().anyMatch(k -> sameName(k, naam))
                        : t.attributen.keySet().stream().anyMatch(a -> sameName(a, naam)))
                return t;
        return houder;
    }

    /**
     * The model-wide producer index: every element a form WRITES ({@link #vormen}), mapped to the base names of
     * the forms that write it. A kenmerk and an attribuut of the same spelling are distinct keys. {@code defaults}
     * is the defaults-group list, which a resolver's model holds ({@code model.prioritizedDefaults}) and a model the
     * loader rebuilt carries beside it ({@code IrLoader.Loaded.prioritizedDefaults()}).
     */
    public static Map<ElementSleutel, Set<String>> producentIndex(RegelSpraakModel model,
                                                                  List<PrioritizedDefaults> defaults) {
        Map<ElementSleutel, Set<String>> out = new LinkedHashMap<>();
        for (Vorm v : lees(model, defaults, null))
            for (ElementSleutel el : v.writes()) out.computeIfAbsent(el, k -> new LinkedHashSet<>()).add(v.naam());
        return out;
    }

    /** The kind of a form of the rule layer (IR-SPEC ch. 2, Rules), one per list the forms sit in: a rule (an
     *  assignment, initialisation or characteristic attribution), an object creation, an allocation, a
     *  consistency, a uniqueness (single-source or a union), a fact creation, a defaults group. */
    public enum Vormsoort { REGEL, OBJECTCREATIE, VERDELING, CONSISTENTIE, UNICITEIT, FEITCREATIE, DEFAULTSGROEP }

    /** One form as this analysis reads it: its kind, its base name ({@link #basisNaam}; a defaults group without a
     *  name is named by its pointer in the document), the elements it WRITES and the elements it READS. */
    public record Vorm(Vormsoort soort, String naam, Set<ElementSleutel> writes, Set<ElementSleutel> leest) {}

    /**
     * Every form of the rule layer with the attributes and characteristics it writes and reads, per IR-SPEC ch. 5.1
     * and the producer-consumer table of RUNTIME-SEMANTICS ch. 6.1. The forms come in the order of the document's
     * lists (the rules, the object creations, the allocations, the consistencies, the uniquenesses with the
     * single-source ones first, the fact creations), and the defaults groups of {@code defaults} last.
     *
     * <ul>
     *   <li>a rule writes its target and reads its condition and value ({@link #verbruikt});</li>
     *   <li>an object creation writes the members its assignments set on the created type, and reads its condition
     *       and the assigned values, evaluated against the creation's subject;</li>
     *   <li>an allocation writes its target attribute and its remainder attribute, and reads its source amount,
     *       the ratio, maximum and ordering attributes of the recipients, and its condition;</li>
     *   <li>a consistency reads its assertion, a uniqueness its key attributes (a union those of every source);
     *       what either writes is its channel, no member;</li>
     *   <li>a fact creation reads its condition; what it writes is a population and a relation, no member;</li>
     *   <li>a defaults group writes its target and reads the justification and consequence of every default,
     *       nested exception groups included.</li>
     * </ul>
     *
     * A population, a relation's link set and a channel are nodes of ch. 5.1 too; this analysis does not key them,
     * so a form's reads and writes of those are not here.
     */
    public static List<Vorm> vormen(RegelSpraakModel model, List<PrioritizedDefaults> defaults, Navigatie navigation) {
        return lees(model, defaults, java.util.Objects.requireNonNull(navigation, "navigatie"));
    }

    /** The reading behind {@link #vormen} and {@link #producentIndex}. Without {@code navigation} every read set is
     *  empty: the producer index asks what a form writes, and no write depends on where a check navigates. */
    private static List<Vorm> lees(RegelSpraakModel model, List<PrioritizedDefaults> defaults, Navigatie navigation) {
        java.util.Objects.requireNonNull(defaults, "defaults");
        List<Vorm> out = new ArrayList<>();
        for (ResolvedRegel r : model.resolvedRegels) {
            Set<ElementSleutel> writes = new LinkedHashSet<>();
            if (r.doel() instanceof Expr.AttribuutRef ref)
                writes.add(elementVan(ref.objecttype(), ref.attribuut(),
                        r.soort() == ResolvedRegel.Soort.KENMERKTOEKENNING));
            out.add(new Vorm(Vormsoort.REGEL, basisNaam(r.naam()), writes,
                    navigation == null ? Set.of() : verbruikt(r, navigation)));
        }
        for (ResolvedObjectCreatie o : model.objectCreaties) {
            Set<ElementSleutel> writes = new LinkedHashSet<>();
            for (ResolvedObjectCreatie.Toekenning t : o.toekenningen())
                if (t.attribuut() != null) writes.add(elementVan(o.nieuwType(), t.attribuut(), t.kenmerk()));
            Reader l = new Reader(navigation, new LinkedHashSet<>());
            l.condition(o.voorwaarde(), o.subject());
            for (ResolvedObjectCreatie.Toekenning t : o.toekenningen()) l.expr(t.waarde(), o.subject());
            out.add(new Vorm(Vormsoort.OBJECTCREATIE, basisNaam(o.naam()), writes, l.out()));
        }
        for (ResolvedVerdeling v : model.verdelingen) {
            Set<ElementSleutel> writes = new LinkedHashSet<>();
            if (v.doelAttribuut() != null) writes.add(elementVan(v.doelType(), v.doelAttribuut(), false));
            if (v.restantAttribuut() != null) writes.add(elementVan(v.bronType(), v.restantAttribuut(), false));
            Reader l = new Reader(navigation, new LinkedHashSet<>());
            l.member(v.bronType(), v.bronAttribuut(), false);
            for (String a : new String[] { v.ratoAttribuut(), v.maximumAttribuut(), v.groepeerAttribuut() })
                l.member(v.doelType(), a, false);
            l.condition(v.voorwaarde(), v.bronType());
            out.add(new Vorm(Vormsoort.VERDELING, basisNaam(v.naam()), writes, l.out()));
        }
        for (ResolvedConsistentie c : model.consistenties) {
            Reader l = new Reader(navigation, new LinkedHashSet<>());
            l.condition(c.assertie(), c.subject());
            out.add(new Vorm(Vormsoort.CONSISTENTIE, basisNaam(c.naam()), Set.of(), l.out()));
        }
        for (ResolvedUniciteit u : model.uniciteiten) {
            Reader l = new Reader(navigation, new LinkedHashSet<>());
            for (String sleutel : u.sleutels()) l.member(u.subject(), sleutel, false);
            out.add(new Vorm(Vormsoort.UNICITEIT, basisNaam(u.naam()), Set.of(), l.out()));
        }
        for (ResolvedUniciteitUnie u : model.uniciteitUnies) {
            Reader l = new Reader(navigation, new LinkedHashSet<>());
            for (ResolvedUniciteitUnie.Bron b : u.bronnen())
                if (b.type() != null) for (String sleutel : b.sleutels()) l.member(b.type(), sleutel, false);
            out.add(new Vorm(Vormsoort.UNICITEIT, basisNaam(u.naam()), Set.of(), l.out()));
        }
        for (ResolvedFeitCreatie f : model.feitCreaties) {
            Reader l = new Reader(navigation, new LinkedHashSet<>());
            l.condition(f.voorwaarde(), f.anker());
            out.add(new Vorm(Vormsoort.FEITCREATIE, basisNaam(f.naam()), Set.of(), l.out()));
        }
        for (int i = 0; i < defaults.size(); i++) {
            PrioritizedDefaults p = defaults.get(i);
            RegelSpraakModel.Objecttype onderwerp = model.objecttypeOpNaam(p.onderwerp());
            Set<ElementSleutel> writes = new LinkedHashSet<>();
            if (p.doel() != null) writes.add(elementVan(onderwerp, p.doel(), p.isKenmerk()));
            // the group reads what its defaults read, so the per-default reading and this one cannot drift apart
            Set<ElementSleutel> leest = new LinkedHashSet<>();
            for (DefaultLezing d : readDefaults(model, p, navigation)) {
                leest.addAll(d.rechtvaardiging());
                leest.addAll(d.consequentie());
            }
            out.add(new Vorm(Vormsoort.DEFAULTSGROEP,
                    p.naam() != null ? basisNaam(p.naam()) : "/rules/prioritizedDefaults/" + i, writes, leest));
        }
        return out;
    }

    /** What one default of a defaults group reads. {@code pad} is its place in the tree: [i] for the i-th default of
     *  the group, [i, g, j] for the j-th default of the g-th exception group of default i, and so on. {@code
     *  rechtvaardiging} is what its justification reads, {@code consequentie} what its consequence reads; a
     *  group-default has neither (RUNTIME-SEMANTICS ch. 7.9). */
    public record DefaultLezing(List<Integer> pad, Default dflt, Set<ElementSleutel> rechtvaardiging,
                                Set<ElementSleutel> consequentie) {}

    /**
     * What every default of {@code groep} reads, in the order of the group (highest priority first) and per default the
     * default itself before the defaults of its exception groups, all against the subject of the group (RUNTIME-SEMANTICS
     * ch. 7.9: a descendant's read is the group's read). The walk is that of {@link #vormen}, which takes the group's
     * reads from here, so the union of every justification and consequence set is exactly that group's
     * {@link Vorm#leest()}. A consumer that names the default where a read falls outside a scope reads the default's own
     * source text ({@code Default.bron()}, or that of its justification or consequence) beside the path.
     */
    public static List<DefaultLezing> lezingen(RegelSpraakModel model, PrioritizedDefaults group, Navigatie navigation) {
        return readDefaults(model, group, java.util.Objects.requireNonNull(navigation, "navigatie"));
    }

    /** The reading behind {@link #lezingen}; without {@code navigation} every set is empty, as in {@link #lees}. */
    private static List<DefaultLezing> readDefaults(RegelSpraakModel model, PrioritizedDefaults group, Navigatie navigation) {
        RegelSpraakModel.Objecttype onderwerp = model.objecttypeOpNaam(group.onderwerp());
        List<DefaultLezing> out = new ArrayList<>();
        for (int i = 0; i < group.defaults().size(); i++)
            lezingen(group.defaults().get(i), List.of(i), onderwerp, navigation, out);
        return out;
    }

    private static void lezingen(Default d, List<Integer> pad, RegelSpraakModel.Objecttype onderwerp,
                                 Navigatie navigation, List<DefaultLezing> out) {
        Reader rechtvaardiging = new Reader(navigation, new LinkedHashSet<>());
        rechtvaardiging.condition(d.voorwaarde(), onderwerp);
        Reader consequentie = new Reader(navigation, new LinkedHashSet<>());
        consequentie.expr(d.consequentie(), onderwerp);
        out.add(new DefaultLezing(pad, d, java.util.Collections.unmodifiableSet(rechtvaardiging.out()),
                java.util.Collections.unmodifiableSet(consequentie.out())));
        if (d.excepties() == null) return;
        List<ExceptionGroup> groups = d.excepties().leden();
        for (int g = 0; g < groups.size(); g++) {
            List<Default> members = groups.get(g).defaults();
            for (int j = 0; j < members.size(); j++) {
                List<Integer> sub = new ArrayList<>(pad);
                sub.add(g);
                sub.add(j);
                lezingen(members.get(j), List.copyOf(sub), onderwerp, navigation, out);
            }
        }
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
        RegelSpraakModel.Objecttype reaches(Conditie.Kenmerk check, RegelSpraakModel.Objecttype onderwerp);
    }

    /** The answer of a resolver's model: the record names the role its hop walks, and the reached type is the
     *  filler of the role of that name in a relation the subject takes part in, found as the emitter finds the
     *  binding it writes for the hop. A navigated check whose role reaches no unique type fails loudly, as the
     *  emitter refuses to write it. */
    public static Navigatie bronNavigatie(RegelSpraakModel model) {
        return (k, onderwerp) -> {
            if (k.navigatie() == null) return null;
            RegelSpraakModel.Objecttype reaches = model.rolDoeltypeNaarSubject(onderwerp, k.navigatie());
            if (reaches == null)
                throw new IllegalStateException("the characteristic check '" + k.kenmerk() + "' navigates by the role '"
                        + k.navigatie() + "', which reaches no unique object type from "
                        + (onderwerp == null ? "an unknown subject" : "'" + onderwerp.naam + "'"));
            return reaches;
        };
    }

    /** The answer of a model the loader rebuilt: the hop rides beside the model ({@code path}), and
     *  its binding names the role the reached object plays, whose declared filler is the reached type. A binding
     *  that names no declared role with a declared filler fails loudly; the verifier refuses such a document
     *  (WF-053). */
    public static Navigatie wireNavigatie(IrLoader.Loaded loaded) {
        RegelSpraakModel model = loaded.model();
        Map<Object, List<RegelSpraakModel.RolBindingRef>> hops = loaded.navigationBindings();
        return (k, onderwerp) -> {
            List<RegelSpraakModel.RolBindingRef> pad = hops.get(k);
            if (pad == null || pad.isEmpty()) return null;
            RegelSpraakModel.RolBindingRef hop = pad.get(pad.size() - 1);
            for (RegelSpraakModel.Feittype ft : model.feittypen.values())
                if (sameName(ft.naam, hop.feittype()))
                    for (RegelSpraakModel.Rol rol : ft.rollen)
                        if (rol.naam() != null && sameName(rol.naam(), hop.rol())) {
                            RegelSpraakModel.Objecttype reaches = model.objecttypeOpNaam(rol.objecttypeNaam());
                            if (reaches != null) return reaches;
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
     *  <p>A navigated kenmerk check reads the kenmerk of the type its hop reaches, which is {@code navigation}'s
     *  answer (see {@link Navigatie}). */
    public static Set<ElementSleutel> verbruikt(ResolvedRegel r, Navigatie navigation) {
        Set<ElementSleutel> out = new LinkedHashSet<>();
        Reader reader = new Reader(java.util.Objects.requireNonNull(navigation, "navigatie"), out);
        RegelSpraakModel.Objecttype subject = r.doel() instanceof Expr.AttribuutRef ref ? ref.objecttype() : null;
        reader.condition(r.voorwaarde(), subject);
        reader.expr(r.waarde(), subject);
        return out;
    }

    /** The one walk behind {@link #verbruikt}, {@link #vormen} and {@link #lezingen}, over a condition or an
     *  expression with the subject a kenmerk check without an owner of its own is evaluated against, and from which a
     *  navigated check's hop starts. Only a kenmerk check asks for the subject; an attribuut read names its type
     *  itself. Without a {@code navigation} it reads nothing (see {@link #lees}). */
    private record Reader(Navigatie navigation, Set<ElementSleutel> out) {

        /** A member a form names outright rather than through an expression: an allocation's source amount or
         *  recipient attribute, a key of a uniqueness. */
        void member(RegelSpraakModel.Objecttype houder, String naam, boolean kenmerk) {
            if (navigation == null || naam == null) return;
            out.add(elementVan(houder, naam, kenmerk));
        }

        void condition(Conditie c, RegelSpraakModel.Objecttype subject) {
            if (c == null || navigation == null) return;
            switch (c) {
                case Conditie.Vergelijking v -> { expr(v.links(), subject); expr(v.rechts(), subject); }
                case Conditie.Predicaat p -> expr(p.operand(), subject);
                case Conditie.Periode p -> { expr(p.vanaf(), subject); expr(p.tot(), subject); }
                // A navigated check lives on the object its hop reaches; any other check lives on its host, or on
                // the subject when it names none.
                case Conditie.Kenmerk k -> {
                    RegelSpraakModel.Objecttype reaches = navigation.reaches(k, subject);
                    RegelSpraakModel.Objecttype carrier =
                            reaches != null ? reaches : k.eigenaar() != null ? k.eigenaar() : subject;
                    out.add(elementVan(carrier, k.kenmerk(), true));
                }
                case Conditie.Samengesteld s -> { for (Conditie d : s.onderdelen()) condition(d, subject); }
                // IR-SPEC ch. 4.5: a quantifier fold's filter and matrix are evaluated with the ELEMENT as subject
                // (RUNTIME-SEMANTICS ch. 3.9a), hence the element type rather than the enclosing subject.
                case Conditie.Kwantorvouw kv -> {
                    condition(kv.filter(), kv.elementObjecttype());
                    condition(kv.toets(), kv.elementObjecttype());
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
            if (e == null || navigation == null) return;
            switch (e) {
                case Expr.AttribuutRef ref -> out.add(elementVan(ref.objecttype(), ref.attribuut(), false));
                case Expr.Binair b -> { expr(b.links(), subject); expr(b.rechts(), subject); }
                case Expr.Functie f -> {
                    // A count's source names the relation's ROLE, never a member of the element type (the loader
                    // refuses a member there): it reads the link set, not an attribuut of each contributor.
                    if (!isCountSource(f)) for (Expr a : f.argumenten()) expr(a, subject);
                    expr(f.leegwaarde(), subject);
                    // the filter is evaluated per contributor, with the element as subject
                    condition(f.filter(), f.elementObjecttype() != null ? f.elementObjecttype() : subject);
                }
                // each selection's filter is evaluated per contributor, as a fold's filter is
                case Expr.InstanceSetUnion u -> {
                    for (Expr.InstanceSetUnion.Tak t : u.takken()) condition(t.filter(), t.elementType());
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
                case Expr.IfThenElse i -> { condition(i.conditie(), subject); expr(i.dan(), subject); expr(i.anders(), subject); }
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
    private static boolean isCountSource(Expr.Functie f) {
        return f.kind() == Expr.FunctieKind.AANTAL && f.elementObjecttype() != null && f.argumenten().size() == 1
                && f.argumenten().get(0) instanceof Expr.AttribuutRef bron && bron.attribuut() != null
                && f.elementObjecttype().attribuut(bron.attribuut()) == null
                && f.elementObjecttype().kenmerkNaam(bron.attribuut()) == null;
    }

    private static boolean sameName(String a, String b) {
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
