package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graaf.ProducentConsument;
import dev.uchar.regelspraak.graaf.ProducentConsument.ElementSleutel;
import dev.uchar.regelspraak.graaf.ProducentConsument.Vormsoort;
import dev.uchar.regelspraak.ir.Conditie;
import dev.uchar.regelspraak.ir.ResolvedConsistentie;
import dev.uchar.regelspraak.ir.ResolvedRegel;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;
import dev.uchar.regelspraak.transpiler.kern.Namen;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The cycles among the members the forms of a document read and write, which the truth-maintenance branch refuses
 * before it builds a KieBase. WF-045 makes every cycle ill-formed, and WF-046 every cycle that carries a negation;
 * the verifier refuses both, but this backend does not run the verifier, so a hand-written document brings its cycle
 * here unchecked. Truth maintenance does not settle a cycle into one answer either. Measured on this branch with the
 * refusal switched off (2026-10-08): a positive cycle derived from an input kept both its characteristics after the
 * input was taken off the bean in a running session, since each was the other's justification; an even cycle of
 * negations played, and the rule first in the model decided which characteristic held; an odd one, of length one and
 * of length two, alternated until the fire cap. (Read with a direct {@code not} instead of through the standing, the
 * cycle of length one fires once and ends silently empty: what a cycle does depends even on how the negation is
 * written.) Every cycle is therefore refused, with or without a negation, by name.
 *
 * <p>The graph has a node per member, keyed on its declaring type ({@code ProducentConsument.elementVan}), and an
 * edge from every member a form reads to every member the same form writes. The analysis gives no polarity, and none
 * is needed when every cycle is refused. Keying on the declaring type merges a member that two sibling subtypes
 * inherit into one node, which IR-SPEC ch. 5.1 keeps apart; that can refuse a document without a cycle, never pass
 * one with a cycle.
 *
 * <p>The §9.5 channel of a form is a node as well (IR-SPEC ch. 5.1; RUNTIME-SEMANTICS ch. 5.6: reading the channel
 * makes it part of the dependency graph). {@code ProducentConsument} does not key channels, so the edges are added
 * here: a form that can fill a channel writes the one of its own name, and a rule or consistency whose condition tests
 * "regelversie X is inconsistent" reads X's. A rule writes its channel only where its value deposits; the lowering
 * refuses a test of any other rule before this check runs, so an edge from a rule that cannot deposit reaches no
 * reader. Measured with these edges left out (2026-10-08): a consistency asserting a characteristic that a rule derives
 * from the consistency's own channel passed this check and alternated until the fire cap.
 */
public final class Kringen {
    private Kringen() {}

    /** The order the refusal lists members in, so that it does not depend on the order of the forms in the document. */
    private static final Comparator<ElementSleutel> VOLGORDE = Comparator.comparing(ElementSleutel::objecttype)
            .thenComparing(ElementSleutel::naam).thenComparing(ElementSleutel::kenmerk);

    private static final Comparator<ProducentConsument.Vorm> VORMVOLGORDE =
            Comparator.comparing((ProducentConsument.Vorm v) -> v.soort().ordinal()).thenComparing(ProducentConsument.Vorm::naam);

    /** The refusal of every cycle among the members {@code vormen} read and write, or null when there is none. The
     *  sentence names each cycle's members and the forms whose reads and writes close it, both sorted, so every order
     *  of the same forms gives the same sentence. */
    public static String weigering(RegelSpraakModel model, List<ProducentConsument.Vorm> vormen) {
        Map<ElementSleutel, Map<ElementSleutel, Set<ProducentConsument.Vorm>>> randen = new TreeMap<>(VOLGORDE);
        for (ProducentConsument.Vorm v : metKanalen(model, vormen))
            for (ElementSleutel lees : v.leest())
                for (ElementSleutel schrijf : v.schrijft()) {
                    randen.computeIfAbsent(lees, k -> new TreeMap<>(VOLGORDE))
                            .computeIfAbsent(schrijf, k -> new LinkedHashSet<>()).add(v);
                    randen.computeIfAbsent(schrijf, k -> new TreeMap<>(VOLGORDE));
                }
        List<String> kringen = new ArrayList<>();
        for (Set<ElementSleutel> component : componenten(randen)) {
            ElementSleutel eerste = component.iterator().next();
            boolean kring = component.size() > 1 || randen.get(eerste).containsKey(eerste);
            if (!kring) continue;
            Set<ProducentConsument.Vorm> sluitend = new TreeSet<>(VORMVOLGORDE);
            for (ElementSleutel van : component)
                for (Map.Entry<ElementSleutel, Set<ProducentConsument.Vorm>> e : randen.get(van).entrySet())
                    if (component.contains(e.getKey())) sluitend.addAll(e.getValue());
            List<String> leden = new ArrayList<>();
            for (ElementSleutel lid : component) leden.add(lid(model, lid));
            List<String> door = new ArrayList<>();
            for (ProducentConsument.Vorm v : sluitend) door.add(vorm(v));
            boolean kanaal = component.stream().anyMatch(k -> KANAAL.equals(k.objecttype()));
            String soort = leden.size() == 1 ? (kanaal ? "" : "het lid ") : (kanaal ? "de knopen " : "de leden ");
            kringen.add("over " + soort + opsomming(leden) + " via " + opsomming(door));
        }
        if (kringen.isEmpty()) return null;
        return "tms: " + (kringen.size() == 1 ? "kring in de afhankelijkheden " + kringen.get(0)
                : kringen.size() + " kringen in de afhankelijkheden: " + String.join("; ", kringen))
                + " — elke kring is onwelgevormd (WF-045; met een ontkenning erop ook WF-046), en truth maintenance geeft"
                + " er geen eenduidig fixpunt voor: een positieve kring houdt zichzelf in stand nadat haar ingang vervalt,"
                + " een even ontkenningskring beslist op de volgorde van de regels in het model, en een oneven"
                + " ontkenningskring wisselt tot het vuurplafond";
    }

    /** The strongly connected components of the graph (Tarjan), each with its members in {@link #VOLGORDE}, the
     *  components in the order of their first member. */
    private static List<Set<ElementSleutel>> componenten(
            Map<ElementSleutel, Map<ElementSleutel, Set<ProducentConsument.Vorm>>> randen) {
        Map<ElementSleutel, Integer> index = new HashMap<>(), laag = new HashMap<>();
        Deque<ElementSleutel> stapel = new ArrayDeque<>();
        Set<ElementSleutel> opStapel = new java.util.HashSet<>();
        List<Set<ElementSleutel>> uit = new ArrayList<>();
        int[] teller = {0};
        for (ElementSleutel k : randen.keySet())
            if (!index.containsKey(k)) bezoek(k, randen, index, laag, stapel, opStapel, uit, teller);
        uit.sort(Comparator.comparing((Set<ElementSleutel> c) -> c.iterator().next(), VOLGORDE));
        return uit;
    }

    private static void bezoek(ElementSleutel k, Map<ElementSleutel, Map<ElementSleutel, Set<ProducentConsument.Vorm>>> randen,
                               Map<ElementSleutel, Integer> index, Map<ElementSleutel, Integer> laag,
                               Deque<ElementSleutel> stapel, Set<ElementSleutel> opStapel,
                               List<Set<ElementSleutel>> uit, int[] teller) {
        index.put(k, teller[0]);
        laag.put(k, teller[0]);
        teller[0]++;
        stapel.push(k);
        opStapel.add(k);
        for (ElementSleutel n : randen.get(k).keySet()) {
            if (!index.containsKey(n)) {
                bezoek(n, randen, index, laag, stapel, opStapel, uit, teller);
                laag.put(k, Math.min(laag.get(k), laag.get(n)));
            } else if (opStapel.contains(n)) {
                laag.put(k, Math.min(laag.get(k), index.get(n)));
            }
        }
        if (laag.get(k).equals(index.get(k))) {
            Set<ElementSleutel> component = new TreeSet<>(VOLGORDE);
            ElementSleutel n;
            do {
                n = stapel.pop();
                opStapel.remove(n);
                component.add(n);
            } while (!n.equals(k));
            uit.add(component);
        }
    }

    /** The objecttype of a channel's node: a member of no declared type. */
    private static final String KANAAL = "§9.5-kanaal";

    private static ElementSleutel kanaal(String naam) {
        return new ElementSleutel(KANAAL, Namen.norm(ProducentConsument.basisNaam(naam)), false);
    }

    /** {@code vormen} with the channel edges of the class comment. Only rules and consistencies are read for channel
     *  tests: every other form that carries a condition is refused by the manifest before this check runs. */
    private static List<ProducentConsument.Vorm> metKanalen(RegelSpraakModel model, List<ProducentConsument.Vorm> vormen) {
        Map<String, Set<ElementSleutel>> toetsen = new HashMap<>();
        for (ResolvedRegel r : model.resolvedRegels)
            kanaaltoetsen(r.voorwaarde(), toetsen.computeIfAbsent(vormsleutel(Vormsoort.REGEL, r.naam()),
                    k -> new LinkedHashSet<>()));
        for (ResolvedConsistentie c : model.consistenties)
            kanaaltoetsen(c.assertie(), toetsen.computeIfAbsent(vormsleutel(Vormsoort.CONSISTENTIE, c.naam()),
                    k -> new LinkedHashSet<>()));
        List<ProducentConsument.Vorm> uit = new ArrayList<>();
        for (ProducentConsument.Vorm v : vormen) {
            Set<ElementSleutel> schrijft = new LinkedHashSet<>(v.schrijft());
            if (v.soort() == Vormsoort.REGEL || v.soort() == Vormsoort.CONSISTENTIE || v.soort() == Vormsoort.UNICITEIT
                    || v.soort() == Vormsoort.VERDELING)
                schrijft.add(kanaal(v.naam()));
            Set<ElementSleutel> leest = new LinkedHashSet<>(v.leest());
            leest.addAll(toetsen.getOrDefault(vormsleutel(v.soort(), v.naam()), Set.of()));
            uit.add(new ProducentConsument.Vorm(v.soort(), v.naam(), schrijft, leest));
        }
        return uit;
    }

    private static String vormsleutel(Vormsoort soort, String naam) {
        return soort + " " + Namen.norm(ProducentConsument.basisNaam(naam));
    }

    /** The channels a condition tests, through composites and quantifier folds. */
    private static void kanaaltoetsen(Conditie c, Set<ElementSleutel> uit) {
        if (c instanceof Conditie.Regelversie rv && rv.inconsistent() && rv.regel() != null) uit.add(kanaal(rv.regel()));
        if (c instanceof Conditie.Samengesteld s) for (Conditie d : s.onderdelen()) kanaaltoetsen(d, uit);
        if (c instanceof Conditie.Kwantorvouw k) {
            kanaaltoetsen(k.filter(), uit);
            kanaaltoetsen(k.toets(), uit);
        }
    }

    /** A member as the document declares it, "Type.naam", or a channel as "het kanaal van" its form; the key holds the
     *  normalised spelling only. */
    private static String lid(RegelSpraakModel model, ElementSleutel e) {
        if (KANAAL.equals(e.objecttype())) return "het kanaal van \"" + e.naam() + "\"";
        Objecttype ot = model.objecttypeOpNaam(e.objecttype());
        if (ot == null) return e.objecttype() + "." + e.naam();
        String naam = e.kenmerk() ? ot.kenmerkNaam(e.naam()) : ot.attribuutNaam(e.naam());
        return ot.naam + "." + (naam != null ? naam : e.naam());
    }

    private static String vorm(ProducentConsument.Vorm v) {
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
}
