package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graaf.ProducentConsument;
import dev.uchar.regelspraak.ir.Conditie;
import dev.uchar.regelspraak.ir.Default;
import dev.uchar.regelspraak.ir.ExceptionGroup;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.ir.ResolvedConsistentie;
import dev.uchar.regelspraak.ir.ResolvedFeitCreatie;
import dev.uchar.regelspraak.ir.ResolvedObjectCreatie;
import dev.uchar.regelspraak.ir.ResolvedRegel;
import dev.uchar.regelspraak.ir.ResolvedRegelGroep;
import dev.uchar.regelspraak.ir.ResolvedUniciteit;
import dev.uchar.regelspraak.ir.ResolvedUniciteitUnie;
import dev.uchar.regelspraak.ir.ResolvedVerdeling;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;
import dev.uchar.regelspraak.model.RegelSpraakModel.RolBindingRef;
import dev.uchar.regelspraak.transpiler.kern.Namen;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The cycles of the dependency graph of IR-SPEC ch. 5, which the exec-model branch refuses before it builds a
 * KieBase: WF-045, a cycle that does not pass through a creation member of a recursive rule group, and WF-046, a
 * negative edge on any cycle, one through such a member included. The verifier refuses both, but this backend does
 * not run the verifier, so a hand-written document brings its cycle here unchecked. Truth maintenance does not settle
 * such a cycle into one answer either. Measured on this branch with the refusal switched off (2026-10-08): a positive
 * cycle derived from an input kept both its characteristics after the input was taken off the bean in a running
 * session, since each was the other's justification; an even cycle of negations played, and the rule first in the
 * model decided which characteristic held; an odd one, of length one and of length two, alternated until the fire
 * cap. A cycle through a recursive creation is admitted: a creation fires only at a fixpoint of the rule layer and
 * what it makes is stated, so the chain advances one generation at a time, and the creation budget, a mint at depth 101
 * in its stratum, bounds a creation that does not stop (RUNTIME-SEMANTICS ch. 5.5, {@code TmsTranspiler.diepte}).
 *
 * <p>The graph is the verifier's ({@code spec/verifier/lib/checks.ml}, {@code dep_forms}, {@code wf045_046}), node for
 * node and edge for edge, and not {@link ProducentConsument}'s, whose member keys are the declaring type and which
 * feeds gate 15 as it is. The nodes are those of ch. 5.1: attribute and characteristic per type as written,
 * channel per rule name, population per type, relation per fact type. A read is literal; a write closes over the
 * subtype chain as ch. 5.1 decides per writer: a form that iterates a type writes the member on every type comparable
 * with it that carries the member, an object creation writes its members and its population on the created type and
 * its ancestors only. Keyed on the declaring type instead, a creation of a supertype was the producer of a read on a
 * subtype it never mints, and the three wrakboeking goldens showed a cycle the verifier does not know. Of the edge
 * kinds of ch. 5.2, WF-046 asks which edges are negative, and the evaluation strata ({@link #strata}), by which the
 * driver orders the creations, ask which are strict or negative.
 *
 * <p>One edge is drawn beyond the verifier: a rule writes the §9.5 channel of its own name. A rule whose value
 * deposits a §6.5 inconsistency fills that channel (RUNTIME-SEMANTICS ch. 5.6), and this branch carries a test of it,
 * where ch. 5.1 keys the channel of a consistency or a uniqueness only. The edge reaches a reader only where such a
 * test passes the lowering, and makes this check stricter than the verifier, never laxer: a test of a rule's channel
 * that closes a cycle is refused here and alternates until the fire cap without the edge. The strata leave the edge
 * out, so that they are the reference interpreter's.
 */
public final class Kringen {
    private Kringen() {}

    /** A read: the node, and whether the edge it draws is negative (ch. 5.2). */
    private record Lezing(String knoop, boolean negatief) {}

    /** One form of the graph: its kind and name as the refusal names it, the IR node it is built from, whether it is a
     *  creation member of a recursive rule group, whether its writes are irrevocable, and the nodes it writes and
     *  reads. A form with irrevocable writes, a characteristic attribution, a consistency, a uniqueness or a creation,
     *  reads every value that is not a characteristic strictly (the verifier's {@code strict_unless_char},
     *  RUNTIME-SEMANTICS ch. 6.1): a transient it acted on would persist. */
    private record Vorm(ProducentConsument.Vormsoort soort, String naam, Object bron, boolean recursieveCreatie,
                        boolean onherroepelijk, Set<String> schrijft, List<Lezing> leest) {}

    /** An edge from the form that writes {@code knoop} to the form that reads it; {@code strikt} where ch. 5.2 makes
     *  the edge strict or negative, so that off a cycle the reader's stratum lies above the writer's. */
    private record Rand(int producent, int consument, String knoop, boolean negatief, boolean strikt) {}

    private static final Comparator<Vorm> VORMVOLGORDE =
            Comparator.comparing((Vorm v) -> v.soort().ordinal()).thenComparing(Vorm::naam);

    /** The refusal of the cycles of {@code geladen}'s forms that WF-045 or WF-046 forbids, or null when there is none.
     *  A cycle names its nodes and the forms whose reads and writes close it, both sorted, so every order of the same
     *  forms gives the same sentence. */
    public static String weigering(IrLoader.Loaded geladen) {
        Graaf g = new Graaf(geladen.model(), geladen.prioritizedDefaults(), geladen.navigatieBindingen(),
                geladen.rolBindingen(), geladen.bronPaden());
        List<Vorm> vormen = g.vormen();
        List<Rand> randen = randen(vormen);
        List<String> kringen = kringen45(g, vormen, randen);
        if (!kringen.isEmpty())
            return "exec-model: " + (kringen.size() == 1 ? "kring in de afhankelijkheden " + kringen.get(0)
                    : kringen.size() + " kringen in de afhankelijkheden: " + String.join("; ", kringen))
                    + " — een kring die niet door een creatielid van een recursieve regelgroep loopt, is onwelgevormd"
                    + " (WF-045), en truth maintenance geeft er geen eenduidig fixpunt voor: een positieve kring houdt"
                    + " zichzelf in stand nadat haar ingang vervalt, een even ontkenningskring beslist op de volgorde van"
                    + " de regels in het model, en een oneven ontkenningskring wisselt tot het vuurplafond";
        List<String> ontkenningen = ontkenningen46(g, vormen, randen);
        if (ontkenningen.isEmpty()) return null;
        return "exec-model: " + (ontkenningen.size() == 1 ? "een ontkennende afhankelijkheid op een kring: " + ontkenningen.get(0)
                : ontkenningen.size() + " ontkennende afhankelijkheden op een kring: " + String.join("; ", ontkenningen))
                + " — geen kring mag een ontkenning dragen, ook niet een kring door een creatielid van een recursieve"
                + " regelgroep (WF-046): de creatie handelt dan op een afwezigheid die de kring daarna vult";
    }

    /**
     * The evaluation stratum of every form of the document, by its IR node, as the reference interpreter computes it
     * over the verifier's graph ({@code ref/lib/eval.ml}, RUNTIME-SEMANTICS ch. 6.3): the smallest stratum at or above
     * the writers of what the form reads positively and above the writers of what it reads strictly or negatively,
     * except along a cycle, where every edge counts as positive (a cycle WF-045 admits passes through a recursive
     * creation, and its generations settle it). The edge this branch draws beyond the verifier, a rule that writes the
     * channel of its own name, is left out, so the strata are the reference interpreter's and no higher. A document
     * with a forbidden cycle gets strata as well; {@link #weigering} refuses it.
     */
    static Map<Object, Integer> strata(RegelSpraakModel model, List<PrioritizedDefaults> defaults,
                                       Map<Object, List<RolBindingRef>> navigatie, Map<Object, RolBindingRef> rolBindingen,
                                       Map<Object, List<RolBindingRef>> bronPaden) {
        List<Vorm> vormen = new Graaf(model, defaults, navigatie, rolBindingen, bronPaden).vormen();
        int n = vormen.size();
        List<Rand> randen = new ArrayList<>();
        for (Rand r : randen(vormen))
            if (!(vormen.get(r.producent()).soort() == ProducentConsument.Vormsoort.REGEL && r.knoop().startsWith("chan:")))
                randen.add(r);
        List<List<Integer>> volgend = new ArrayList<>();
        for (int i = 0; i < n; i++) volgend.add(new ArrayList<>());
        for (Rand r : randen) volgend.get(r.producent()).add(r.consument());
        boolean[] stijgt = new boolean[randen.size()];
        for (int i = 0; i < randen.size(); i++) {
            Rand r = randen.get(i);
            stijgt[i] = r.strikt() && !bereikbaar(volgend, r.consument(), r.producent());
        }
        int[] stratum = new int[n];
        boolean veranderd = true;
        for (int ronde = 1; veranderd; ronde++) {
            // only an edge between two strongly connected components climbs, so the strata settle within n rounds;
            // the reference interpreter faults past n + 2, and so does this
            if (ronde > n + 2) throw new IllegalStateException("exec-model: geen evaluatiestrata na " + (n + 2) + " rondes");
            veranderd = false;
            for (int i = 0; i < randen.size(); i++) {
                Rand r = randen.get(i);
                int nodig = stratum[r.producent()] + (stijgt[i] ? 1 : 0);
                if (stratum[r.consument()] < nodig) {
                    stratum[r.consument()] = nodig;
                    veranderd = true;
                }
            }
        }
        Map<Object, Integer> uit = new IdentityHashMap<>();
        for (int i = 0; i < n; i++) uit.put(vormen.get(i).bron(), stratum[i]);
        return uit;
    }

    /** Every edge of the graph: one for every node a form reads and another form, or the same, writes. */
    private static List<Rand> randen(List<Vorm> vormen) {
        Map<String, List<Integer>> schrijvers = new HashMap<>();
        for (int i = 0; i < vormen.size(); i++)
            for (String k : vormen.get(i).schrijft()) schrijvers.computeIfAbsent(k, x -> new ArrayList<>()).add(i);
        List<Rand> uit = new ArrayList<>();
        for (int c = 0; c < vormen.size(); c++) {
            Vorm lezer = vormen.get(c);
            for (Lezing l : lezer.leest()) {
                boolean strikt = l.negatief() || lezer.onherroepelijk() && !l.knoop().startsWith("char:");
                for (int p : schrijvers.getOrDefault(l.knoop(), List.of()))
                    uit.add(new Rand(p, c, l.knoop(), l.negatief(), strikt));
            }
        }
        return uit;
    }

    /** The cycles of the graph without the creation members of recursive rule groups (WF-045): every strongly connected
     *  component of two or more forms, or of one form that reads what it writes, as "over <nodes> via <forms>". */
    private static List<String> kringen45(Graaf g, List<Vorm> vormen, List<Rand> randen) {
        int n = vormen.size();
        List<List<Rand>> uit = new ArrayList<>();
        for (int i = 0; i < n; i++) uit.add(new ArrayList<>());
        for (Rand r : randen)
            if (!vormen.get(r.producent()).recursieveCreatie() && !vormen.get(r.consument()).recursieveCreatie())
                uit.get(r.producent()).add(r);
        List<String> kringen = new ArrayList<>();
        for (Set<Integer> component : componenten(n, uit)) {
            int eerste = component.iterator().next();
            boolean zelf = uit.get(eerste).stream().anyMatch(r -> r.consument() == eerste);
            if (component.size() == 1 && !zelf) continue;
            Set<Vorm> door = new TreeSet<>(VORMVOLGORDE);
            Set<String> knopen = new LinkedHashSet<>();
            for (int i : component)
                for (Rand r : uit.get(i))
                    if (component.contains(r.consument())) {
                        door.add(vormen.get(i));
                        door.add(vormen.get(r.consument()));
                        knopen.add(r.knoop());
                    }
            List<String> forms = new ArrayList<>();
            for (Vorm v : door) forms.add(vorm(v));
            kringen.add("over " + g.knopen(knopen) + " via " + opsomming(forms));
        }
        kringen.sort(null);
        return kringen;
    }

    /** The negative edges on a cycle of the whole graph (WF-046): an edge from producer p to consumer c on which p is
     *  reachable from c, as "<consumer> leest <node> van <producer>, dat vanuit <consumer> bereikbaar is". */
    private static List<String> ontkenningen46(Graaf g, List<Vorm> vormen, List<Rand> randen) {
        int n = vormen.size();
        List<List<Integer>> volgend = new ArrayList<>();
        for (int i = 0; i < n; i++) volgend.add(new ArrayList<>());
        for (Rand r : randen) volgend.get(r.producent()).add(r.consument());
        Set<String> uit = new TreeSet<>();
        for (Rand r : randen)
            if (r.negatief() && bereikbaar(volgend, r.consument(), r.producent())) {
                String c = vorm(vormen.get(r.consument())), p = vorm(vormen.get(r.producent()));
                uit.add(c + " leest " + g.knoop(r.knoop()) + " ontkennend van " + p + ", dat vanuit " + c
                        + " bereikbaar is");
            }
        return new ArrayList<>(uit);
    }

    private static boolean bereikbaar(List<List<Integer>> volgend, int van, int naar) {
        Set<Integer> gezien = new HashSet<>();
        Deque<Integer> rij = new ArrayDeque<>(List.of(van));
        while (!rij.isEmpty()) {
            int i = rij.pop();
            if (i == naar) return true;
            if (gezien.add(i)) rij.addAll(volgend.get(i));
        }
        return false;
    }

    /** The strongly connected components of the graph over {@code n} forms (Tarjan), each with its forms in index
     *  order. */
    private static List<Set<Integer>> componenten(int n, List<List<Rand>> uit) {
        int[] index = new int[n], laag = new int[n];
        java.util.Arrays.fill(index, -1);
        boolean[] opStapel = new boolean[n];
        Deque<Integer> stapel = new ArrayDeque<>();
        List<Set<Integer>> componenten = new ArrayList<>();
        int[] teller = {0};
        for (int i = 0; i < n; i++)
            if (index[i] < 0) bezoek(i, uit, index, laag, opStapel, stapel, componenten, teller);
        return componenten;
    }

    private static void bezoek(int k, List<List<Rand>> uit, int[] index, int[] laag, boolean[] opStapel,
                               Deque<Integer> stapel, List<Set<Integer>> componenten, int[] teller) {
        index[k] = laag[k] = teller[0]++;
        stapel.push(k);
        opStapel[k] = true;
        for (Rand r : uit.get(k)) {
            int m = r.consument();
            if (index[m] < 0) {
                bezoek(m, uit, index, laag, opStapel, stapel, componenten, teller);
                laag[k] = Math.min(laag[k], laag[m]);
            } else if (opStapel[m]) {
                laag[k] = Math.min(laag[k], index[m]);
            }
        }
        if (laag[k] == index[k]) {
            Set<Integer> component = new TreeSet<>();
            int m;
            do {
                m = stapel.pop();
                opStapel[m] = false;
                component.add(m);
            } while (m != k);
            componenten.add(component);
        }
    }

    private static String vorm(Vorm v) {
        String soort = switch (v.soort()) {
            case REGEL -> "de regel";
            case OBJECTCREATIE -> "de objectcreatie";
            case VERDELING -> "de verdeling";
            case CONSISTENTIE -> "de consistentieregel";
            case UNICITEIT -> "de uniciteitsregel";
            case FEITCREATIE -> "de feitcreatie";
            case DEFAULTSGROEP -> "de defaultsgroep";
        };
        return soort + " \"" + v.naam() + "\"";
    }

    private static String opsomming(List<String> delen) {
        if (delen.size() == 1) return delen.get(0);
        return String.join(", ", delen.subList(0, delen.size() - 1)) + " en " + delen.get(delen.size() - 1);
    }

    /** The verifier's {@code strip_version}: a trailing " #&lt;digits&gt;" comes off, with the space before it. */
    private static String zonderVersie(String naam) {
        int i = naam.lastIndexOf('#');
        if (i >= 2 && naam.charAt(i - 1) == ' ' && i + 1 < naam.length()
                && naam.substring(i + 1).chars().allMatch(c -> c >= '0' && c <= '9'))
            return naam.substring(0, i - 1);
        return naam;
    }

    /** The graph of one document: its forms, built as the verifier's {@code dep_forms} builds them, and the names its
     *  nodes are shown by. */
    private static final class Graaf {
        private final RegelSpraakModel model;
        private final List<PrioritizedDefaults> defaults;
        private final Map<Object, List<RolBindingRef>> navigatie;
        private final Map<Object, RolBindingRef> rolBindingen;
        private final Map<Object, List<RolBindingRef>> bronPaden;
        private final Stapels stapels;
        /** How a node is shown, by key: a member as "Type.naam", spelled as declared. */
        private final Map<String, String> namen = new HashMap<>();
        /** The order nodes are listed in: the members first, by type and name, then the other nodes. */
        private final Map<String, String> volgorde = new HashMap<>();

        Graaf(RegelSpraakModel model, List<PrioritizedDefaults> defaults, Map<Object, List<RolBindingRef>> navigatie,
              Map<Object, RolBindingRef> rolBindingen, Map<Object, List<RolBindingRef>> bronPaden) {
            this.model = model;
            this.defaults = defaults;
            this.navigatie = navigatie;
            this.rolBindingen = rolBindingen;
            this.bronPaden = bronPaden;
            this.stapels = new Stapels(model, defaults);
        }

        String knoop(String k) {
            return namen.getOrDefault(k, k);
        }

        /** {@code knopen} listed as the refusal lists them: "het lid X" or "de leden X en Y" where all are members, and
         *  otherwise the node itself or "de knopen X en Y". */
        String knopen(Set<String> knopen) {
            List<String> gesorteerd = new ArrayList<>(knopen);
            gesorteerd.sort(Comparator.comparing((String k) -> volgorde.getOrDefault(k, "~" + k)));
            boolean leden = gesorteerd.stream().allMatch(k -> k.startsWith("attr:") || k.startsWith("char:"));
            List<String> getoond = gesorteerd.stream().map(this::knoop).toList();
            if (getoond.size() == 1) return (leden ? "het lid " : "") + getoond.get(0);
            return (leden ? "de leden " : "de knopen ") + opsomming(getoond);
        }

        // ---------------- nodes ----------------

        private String lid(Objecttype ot, String naam, boolean kenmerk) {
            String sleutel = (kenmerk ? "char:" : "attr:") + Namen.norm(ot.naam) + ":" + Namen.norm(naam);
            String gedeclareerd = kenmerk ? ot.kenmerkNaam(naam) : ot.attribuutNaam(naam);
            namen.putIfAbsent(sleutel, ot.naam + "." + (gedeclareerd != null ? gedeclareerd : naam));
            volgorde.putIfAbsent(sleutel, "0 " + Namen.norm(ot.naam) + " " + Namen.norm(naam) + " " + (kenmerk ? 1 : 0));
            return sleutel;
        }

        private String kanaal(String regel) {
            String sleutel = "chan:" + Namen.norm(zonderVersie(regel));
            namen.putIfAbsent(sleutel, "het kanaal van \"" + ProducentConsument.basisNaam(regel) + "\"");
            volgorde.putIfAbsent(sleutel, "1 " + sleutel);
            return sleutel;
        }

        private String populatie(Objecttype ot) {
            String sleutel = "pop:" + Namen.norm(ot.naam);
            namen.putIfAbsent(sleutel, "de populatie van " + ot.naam);
            volgorde.putIfAbsent(sleutel, "2 " + sleutel);
            return sleutel;
        }

        private String relatie(String feittype) {
            String sleutel = "rel:" + Namen.norm(feittype);
            namen.putIfAbsent(sleutel, "de relatie " + feittype);
            volgorde.putIfAbsent(sleutel, "3 " + sleutel);
            return sleutel;
        }

        /** Whether {@code ot} carries member {@code naam}, declared on it or inherited. */
        private static boolean draagt(Objecttype ot, String naam, boolean kenmerk) {
            for (Objecttype t : ot.keten())
                if (kenmerk ? t.kenmerken.stream().anyMatch(k -> Namen.norm(k).equals(Namen.norm(naam)))
                            : t.attributen.keySet().stream().anyMatch(a -> Namen.norm(a).equals(Namen.norm(naam))))
                    return true;
            return false;
        }

        /** A member write by a form that iterates {@code ot}: on {@code ot} and every type comparable with it that
         *  carries the member (the verifier's {@code member_writes ~over:`Extension}). */
        private Set<String> uitbreidingsschrijf(Objecttype ot, String naam, boolean kenmerk) {
            Set<String> uit = new LinkedHashSet<>();
            uit.add(lid(ot, naam, kenmerk));
            for (Objecttype s : model.objecttypen.values())
                if ((s.isEen(ot) || ot.isEen(s)) && draagt(s, naam, kenmerk)) uit.add(lid(s, naam, kenmerk));
            return uit;
        }

        /** A member write by an object creation of {@code ot}: on {@code ot} and its ancestors that carry the member
         *  (the verifier's {@code member_writes ~over:`Minted}), since creation mints exactly {@code ot}. */
        private Set<String> muntschrijf(Objecttype ot, String naam, boolean kenmerk) {
            Set<String> uit = new LinkedHashSet<>();
            uit.add(lid(ot, naam, kenmerk));
            for (Objecttype t : ot.keten()) if (draagt(t, naam, kenmerk)) uit.add(lid(t, naam, kenmerk));
            return uit;
        }

        // ---------------- the forms ----------------

        List<Vorm> vormen() {
            Set<String> recursief = new HashSet<>();
            for (ResolvedRegelGroep g : model.regelGroepen)
                if (g.recursief()) for (String lid : g.leden()) if (lid != null) recursief.add(Namen.norm(zonderVersie(lid)));
            List<Vorm> uit = new ArrayList<>();
            for (ResolvedRegel r : model.resolvedRegels) {
                if (!(r.doel() instanceof Expr.AttribuutRef d) || d.objecttype() == null || d.attribuut() == null) continue;
                boolean kenmerk = r.soort() == ResolvedRegel.Soort.KENMERKTOEKENNING;
                Set<String> schrijft = uitbreidingsschrijf(d.objecttype(), d.attribuut(), kenmerk);
                if (r.naam() != null) schrijft.add(kanaal(r.naam()));
                List<Lezing> leest = new ArrayList<>();
                conditie(r.voorwaarde(), d.objecttype(), false, leest);
                expr(r.waarde(), d.objecttype(), leest);
                if (!kenmerk && !stapels.geabsorbeerdOp(d.objecttype(), d.attribuut()).isEmpty()) leest = negatief(leest);
                uit.add(new Vorm(ProducentConsument.Vormsoort.REGEL, ProducentConsument.basisNaam(r.naam()), r, false,
                        kenmerk, schrijft, leest));
            }
            for (ResolvedVerdeling v : model.verdelingen) {
                Set<String> schrijft = new LinkedHashSet<>();
                if (v.doelType() != null && v.doelAttribuut() != null)
                    schrijft.addAll(uitbreidingsschrijf(v.doelType(), v.doelAttribuut(), false));
                if (v.bronType() != null && v.restantAttribuut() != null)
                    schrijft.addAll(uitbreidingsschrijf(v.bronType(), v.restantAttribuut(), false));
                List<Lezing> leest = new ArrayList<>();
                if (v.doelType() != null) leest.add(new Lezing(populatie(v.doelType()), false));
                RolBindingRef rb = rolBindingen.get(v);
                if (rb != null && rb.feittype() != null) leest.add(new Lezing(relatie(rb.feittype()), false));
                if (v.bronType() != null && v.bronAttribuut() != null)
                    leest.add(new Lezing(lid(v.bronType(), v.bronAttribuut(), false), false));
                if (v.doelType() != null)
                    for (String a : new String[] {v.ratoAttribuut(), v.maximumAttribuut(), v.groepeerAttribuut()})
                        if (a != null) leest.add(new Lezing(lid(v.doelType(), a, false), false));
                conditie(v.voorwaarde(), v.bronType(), false, leest);
                uit.add(new Vorm(ProducentConsument.Vormsoort.VERDELING, ProducentConsument.basisNaam(v.naam()), v, false,
                        false, schrijft, leest));
            }
            for (ResolvedConsistentie c : model.consistenties) {
                List<Lezing> leest = new ArrayList<>();
                conditie(c.assertie(), c.subject(), false, leest);
                uit.add(new Vorm(ProducentConsument.Vormsoort.CONSISTENTIE, ProducentConsument.basisNaam(c.naam()), c,
                        false, true, c.naam() == null ? Set.of() : Set.of(kanaal(c.naam())), leest));
            }
            for (ResolvedUniciteit u : model.uniciteiten) {
                List<Lezing> leest = new ArrayList<>();
                if (u.scopeRol() != null && u.scopeRol().feittype() != null)
                    leest.add(new Lezing(relatie(u.scopeRol().feittype()), false));
                if (u.subject() != null)
                    for (String k : u.sleutels()) if (k != null) leest.add(new Lezing(lid(u.subject(), k, false), false));
                uit.add(new Vorm(ProducentConsument.Vormsoort.UNICITEIT, ProducentConsument.basisNaam(u.naam()), u,
                        false, true, u.naam() == null ? Set.of() : Set.of(kanaal(u.naam())), leest));
            }
            for (ResolvedUniciteitUnie u : model.uniciteitUnies) {
                List<Lezing> leest = new ArrayList<>();
                for (ResolvedUniciteitUnie.Bron b : u.bronnen())
                    if (b.type() != null)
                        for (String k : b.sleutels()) if (k != null) leest.add(new Lezing(lid(b.type(), k, false), false));
                uit.add(new Vorm(ProducentConsument.Vormsoort.UNICITEIT, ProducentConsument.basisNaam(u.naam()), u,
                        false, true, u.naam() == null ? Set.of() : Set.of(kanaal(u.naam())), leest));
            }
            for (ResolvedObjectCreatie o : model.objectCreaties) {
                Set<String> schrijft = new LinkedHashSet<>();
                if (o.nieuwType() != null) {
                    schrijft.add(populatie(o.nieuwType()));
                    for (Objecttype t : o.nieuwType().keten()) schrijft.add(populatie(t));
                }
                String terug = terugkoppeling(o);
                if (terug != null) schrijft.add(relatie(terug));
                if (o.nieuwType() != null)
                    for (ResolvedObjectCreatie.Toekenning t : o.toekenningen())
                        if (t.attribuut() != null) schrijft.addAll(muntschrijf(o.nieuwType(), t.attribuut(), t.kenmerk()));
                List<Lezing> leest = new ArrayList<>();
                if (o.subject() != null) leest.add(new Lezing(populatie(o.subject()), false));
                conditie(o.voorwaarde(), o.subject(), false, leest);
                for (ResolvedObjectCreatie.Toekenning t : o.toekenningen()) expr(t.waarde(), o.subject(), leest);
                String naam = o.naam() == null ? "" : o.naam();
                uit.add(new Vorm(ProducentConsument.Vormsoort.OBJECTCREATIE, ProducentConsument.basisNaam(o.naam()), o,
                        recursief.contains(Namen.norm(zonderVersie(naam))), true, schrijft, leest));
            }
            for (ResolvedFeitCreatie f : model.feitCreaties) {
                Set<String> schrijft = new LinkedHashSet<>();
                if (f.feittype() != null) schrijft.add(relatie(f.feittype()));
                List<Lezing> leest = new ArrayList<>();
                for (ResolvedFeitCreatie.Tussenstap t : f.tussenstappen())
                    if (t.rolBinding() != null && t.rolBinding().feittype() != null)
                        leest.add(new Lezing(relatie(t.rolBinding().feittype()), false));
                if (f.joinRol() != null && f.joinRol().feittype() != null)
                    leest.add(new Lezing(relatie(f.joinRol().feittype()), false));
                conditie(f.voorwaarde(), f.anker(), false, leest);
                String naam = f.naam() == null ? "" : f.naam();
                uit.add(new Vorm(ProducentConsument.Vormsoort.FEITCREATIE, ProducentConsument.basisNaam(f.naam()), f,
                        recursief.contains(Namen.norm(zonderVersie(naam))), true, schrijft, leest));
            }
            for (int i = 0; i < defaults.size(); i++) {
                PrioritizedDefaults p = defaults.get(i);
                Objecttype onderwerp = model.objecttypeOpNaam(p.onderwerp());
                Set<String> schrijft = new LinkedHashSet<>();
                if (onderwerp != null && p.doel() != null)
                    schrijft.addAll(uitbreidingsschrijf(onderwerp, p.doel(), p.isKenmerk()));
                List<Lezing> leest = new ArrayList<>();
                for (Default d : p.defaults()) defaultlezingen(d, onderwerp, leest);
                uit.add(new Vorm(ProducentConsument.Vormsoort.DEFAULTSGROEP,
                        p.naam() != null ? ProducentConsument.basisNaam(p.naam()) : "/rules/prioritizedDefaults/" + i, p,
                        false, false, schrijft, negatief(leest)));
            }
            return uit;
        }

        /** The relation an object creation writes as its back-link: its {@code connectingRole} binding's, else the one
         *  fact type relating its subject and created type, none for a self-typed two-role pair, which the derivation
         *  leaves unlinked (the verifier's reading of the by-pair route). */
        private String terugkoppeling(ResolvedObjectCreatie o) {
            RolBindingRef rb = rolBindingen.get(o);
            if (rb != null) return rb.feittype();
            Objecttype s = o.subject(), n = o.nieuwType();
            if (s == null || n == null) return null;
            List<RegelSpraakModel.Feittype> kandidaten = new ArrayList<>();
            for (RegelSpraakModel.Feittype ft : model.feittypen.values()) {
                List<RegelSpraakModel.Rol> r = ft.rollen;
                if (r.size() == 2) {
                    boolean ab = s.naam.equalsIgnoreCase(r.get(0).objecttypeNaam())
                            && n.naam.equalsIgnoreCase(r.get(1).objecttypeNaam());
                    boolean ba = s.naam.equalsIgnoreCase(r.get(1).objecttypeNaam())
                            && n.naam.equalsIgnoreCase(r.get(0).objecttypeNaam());
                    if (ab || ba) kandidaten.add(ft);
                } else if (r.size() == 1 && ft.wederkerig && s == n && s.naam.equalsIgnoreCase(r.get(0).objecttypeNaam())) {
                    kandidaten.add(ft);
                }
            }
            if (kandidaten.size() != 1) return null;
            RegelSpraakModel.Feittype ft = kandidaten.get(0);
            return s == n && !ft.wederkerig ? null : ft.naam;
        }

        private void defaultlezingen(Default d, Objecttype onderwerp, List<Lezing> uit) {
            conditie(d.voorwaarde(), onderwerp, false, uit);
            expr(d.consequentie(), onderwerp, uit);
            if (d.excepties() == null) return;
            for (ExceptionGroup g : d.excepties().leden())
                for (Default lid : g.defaults()) defaultlezingen(lid, onderwerp, uit);
        }

        private static List<Lezing> negatief(List<Lezing> l) {
            List<Lezing> uit = new ArrayList<>();
            for (Lezing x : l) uit.add(new Lezing(x.knoop(), true));
            return uit;
        }

        // ---------------- the reads ----------------

        /** The verifier's {@code dep_expr}: every expression node, so a node the IR records gain later fails to compile
         *  here until it is decided what it reads. */
        private void expr(Expr e, Objecttype subject, List<Lezing> uit) {
            if (e == null) return;
            switch (e) {
                case Expr.AttribuutRef a -> {
                    if (a.objecttype() == null || a.attribuut() == null) return;
                    for (RolBindingRef hop : navigatie.getOrDefault(a, List.of()))
                        if (hop.feittype() != null) uit.add(new Lezing(relatie(hop.feittype()), false));
                    uit.add(new Lezing(lid(a.objecttype(), a.attribuut(), false), false));
                }
                case Expr.Binair b -> { expr(b.links(), subject, uit); expr(b.rechts(), subject, uit); }
                case Expr.Functie f -> {
                    Objecttype el = f.elementObjecttype();
                    if (el != null) uit.add(new Lezing(populatie(el), false));
                    List<RolBindingRef> pad = bronPaden.get(f);
                    if (pad != null)
                        for (RolBindingRef hop : pad) if (hop.feittype() != null) uit.add(new Lezing(relatie(hop.feittype()), false));
                    RolBindingRef rol = rolBindingen.get(f);
                    if (rol != null && rol.feittype() != null) uit.add(new Lezing(relatie(rol.feittype()), false));
                    // the count source names the relation's role, not a member: it reads the link set and the
                    // population drawn above, never a member of the contributor
                    boolean telbron = f.kind() == Expr.FunctieKind.AANTAL && el != null && f.argumenten().size() == 1
                            && f.argumenten().get(0) instanceof Expr.AttribuutRef bron && bron.attribuut() != null
                            && (pad != null || rol != null)
                            && !draagt(el, bron.attribuut(), false) && !draagt(el, bron.attribuut(), true);
                    if (!telbron) for (Expr a : f.argumenten()) expr(a, subject, uit);
                    expr(f.leegwaarde(), subject, uit);
                    conditie(f.filter(), el != null ? el : subject, false, uit);
                }
                case Expr.InstanceSetUnion u -> {
                    for (Expr.InstanceSetUnion.Tak t : u.takken()) {
                        if (t.elementType() != null) uit.add(new Lezing(populatie(t.elementType()), false));
                        if (t.bronRol() != null && t.bronRol().feittype() != null)
                            uit.add(new Lezing(relatie(t.bronRol().feittype()), false));
                        conditie(t.filter(), t.elementType(), false, uit);
                    }
                }
                case Expr.Afgerond a -> expr(a.basis(), subject, uit);
                case Expr.Begrensd b -> {
                    expr(b.basis(), subject, uit);
                    expr(b.minimum(), subject, uit);
                    expr(b.maximum(), subject, uit);
                }
                case Expr.DimensieCel c -> expr(c.attribuut(), subject, uit);
                case Expr.TekstReeks t -> {
                    for (Expr.TekstReeks.Deel d : t.delen())
                        if (d instanceof Expr.TekstReeks.Ingevoegd i) expr(i.expr(), subject, uit);
                }
                case Expr.Let l -> { expr(l.value(), subject, uit); expr(l.body(), subject, uit); }
                case Expr.IfThenElse i -> {
                    conditie(i.conditie(), subject, false, uit);
                    expr(i.dan(), subject, uit);
                    expr(i.anders(), subject, uit);
                }
                case Expr.Literal ignored -> { }
                case Expr.ParameterRef ignored -> { }
                case Expr.VariabeleRef ignored -> { }
                case Expr.BinderRef ignored -> { }
                case Expr.Constante ignored -> { }
                case Expr.Onopgelost ignored -> { }
            }
        }

        /** The verifier's {@code dep_cond}: {@code neg} is the negation-like context a characteristic read under it
         *  takes; a leeg or gevuld test makes every read under it negative, in either polarity; a quantifier fold reads
         *  its range negatively under all, none, atMost and exactly and its matrix under none, atMost and exactly. */
        private void conditie(Conditie c, Objecttype subject, boolean neg, List<Lezing> uit) {
            if (c == null) return;
            switch (c) {
                case Conditie.Vergelijking v -> { expr(v.links(), subject, uit); expr(v.rechts(), subject, uit); }
                case Conditie.Predicaat p -> {
                    if (p.soort() == Conditie.Predicaatsoort.LEEG || p.soort() == Conditie.Predicaatsoort.GEVULD) {
                        List<Lezing> onder = new ArrayList<>();
                        expr(p.operand(), subject, onder);
                        uit.addAll(negatief(onder));
                    } else {
                        expr(p.operand(), subject, uit);
                    }
                }
                case Conditie.Kenmerk k -> {
                    if (k.kenmerk() == null) return;
                    boolean negatief = neg || k.negatie();
                    List<RolBindingRef> pad = navigatie.get(k);
                    if (pad != null && !pad.isEmpty()) {
                        for (RolBindingRef hop : pad)
                            if (hop.feittype() != null) uit.add(new Lezing(relatie(hop.feittype()), false));
                        Objecttype bereikt = vuller(pad.get(pad.size() - 1));
                        if (bereikt != null) uit.add(new Lezing(lid(bereikt, k.kenmerk(), true), negatief));
                        return;
                    }
                    Objecttype drager = k.eigenaar() != null ? k.eigenaar() : subject;
                    if (drager != null) uit.add(new Lezing(lid(drager, k.kenmerk(), true), negatief));
                }
                case Conditie.Rollidmaatschap r -> {
                    RolBindingRef rb = rolBindingen.get(r);
                    if (rb != null && rb.feittype() != null) uit.add(new Lezing(relatie(rb.feittype()), neg || r.negatie()));
                }
                // a role check the loader records without a binding: no relation of its own to name
                case Conditie.VerreRollidmaatschap ignored -> { }
                case Conditie.Periode p -> { expr(p.vanaf(), subject, uit); expr(p.tot(), subject, uit); }
                case Conditie.Samengesteld s -> {
                    boolean onder = switch (s.kwantor()) {
                        case GEEN, TEN_HOOGSTE, PRECIES -> true;
                        default -> neg;
                    };
                    for (Conditie d : s.onderdelen()) conditie(d, subject, onder, uit);
                }
                case Conditie.Kwantorvouw kv -> {
                    boolean bereikNeg = neg || kv.kwantor() != Conditie.Kwantor.TEN_MINSTE;
                    boolean matrixNeg = neg || !(kv.kwantor() == Conditie.Kwantor.TEN_MINSTE
                            || kv.kwantor() == Conditie.Kwantor.ALLE);
                    List<RolBindingRef> bereik = new ArrayList<>();
                    if (kv.bronRol() != null) bereik.add(kv.bronRol());
                    if (kv.bronPad() != null) bereik.addAll(kv.bronPad());
                    for (RolBindingRef hop : bereik)
                        if (hop.feittype() != null) uit.add(new Lezing(relatie(hop.feittype()), bereikNeg));
                    deel(kv.filter(), kv.elementObjecttype(), bereikNeg, uit);
                    deel(kv.toets(), kv.elementObjecttype(), matrixNeg, uit);
                }
                case Conditie.Regelversie rv -> {
                    if (rv.regel() != null) uit.add(new Lezing(kanaal(rv.regel()), true));
                }
                case Conditie.Onopgelost ignored -> { }
            }
        }

        /** A part of a quantifier fold: negative throughout where {@code negatief}, its own kinds otherwise. */
        private void deel(Conditie c, Objecttype subject, boolean negatief, List<Lezing> uit) {
            List<Lezing> onder = new ArrayList<>();
            conditie(c, subject, negatief, onder);
            uit.addAll(negatief ? negatief(onder) : onder);
        }

        /** The declared filler of the role a hop binds: the type it reaches. */
        private Objecttype vuller(RolBindingRef hop) {
            for (RegelSpraakModel.Feittype ft : model.feittypen.values())
                if (Namen.norm(ft.naam).equals(Namen.norm(hop.feittype())))
                    for (RegelSpraakModel.Rol r : ft.rollen)
                        if (r.naam() != null && Namen.norm(r.naam()).equals(Namen.norm(hop.rol())))
                            return model.objecttypeOpNaam(r.objecttypeNaam());
            return null;
        }
    }
}
