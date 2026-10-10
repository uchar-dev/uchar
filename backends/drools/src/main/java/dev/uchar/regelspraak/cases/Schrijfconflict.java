package dev.uchar.regelspraak.cases;

import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.ResolvedRegel;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;
import dev.uchar.regelspraak.runtime.Accessors;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.kern.Namen;

import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.BeforeMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * The writes of the last firings of one session, kept to name what a run that reaches the fire cap diverges on
 * (RUNTIME-SEMANTICS ch. 5.7). Each rule is an equation (ch. 5.1); two rules that write one attribute of one object
 * and whose conditions both hold there have no common fixpoint when they write different values, and each firing of
 * the one makes the other fire again until the cap. WF-079 refuses two unconditional writers; two conditional ones
 * reach the engine. The replay therefore records, for every firing of a rule that writes an attribute (a gelijkstelling,
 * berekening or initialisatie, written on its subject), the value the attribute holds after the firing when the firing
 * changed it, and at the cap names the writers that kept changing one attribute of one object to different values, in
 * the order of the document, with their values. A run that reaches the cap otherwise, such as a creation that keeps
 * minting, shows no such attribute and keeps the replay's own sentence. The replay hangs it on a session of the
 * DRL-text branch only, whose rules write the beans as they fire; the exec-model branch writes no bean while it fires.
 */
final class Schrijfconflict extends DefaultAgendaEventListener {

    /** How many changing firings are kept: a divergence alternates its writers, so the last few show all of them. */
    private static final int VENSTER = 64;

    /** A rule that writes an attribute on its subject: its place in the document, its name there, the attribute as the
     *  target spells it, the bean class of the target's type and the getter of the attribute. */
    private record Schrijver(int volgorde, String regel, String attribuut, Class<?> klasse, Function<Object, Object> lezer) {}

    /** One firing that changed the attribute of {@code bean} to {@code waarde}. */
    private record Schrijfdaad(Schrijver schrijver, Object bean, Object waarde) {}

    private final Map<String, Schrijver> perEngineNaam = new HashMap<>();
    private final Map<Class<?>, String> typeNaam = new HashMap<>();
    private final ArrayDeque<Schrijfdaad> recent = new ArrayDeque<>();
    /** The bean and its value before the firing in progress, read in {@link #beforeMatchFired}. */
    private Object beanVoor;
    private Object waardeVoor;

    Schrijfconflict(RegelSpraakModel model, ClassLoader cl) {
        for (Objecttype ot : model.objecttypen.values()) {
            Class<?> k = beanKlasse(ot, cl);
            if (k != null) typeNaam.put(k, ot.naam);
        }
        for (int i = 0; i < model.resolvedRegels.size(); i++) {
            ResolvedRegel r = model.resolvedRegels.get(i);
            if (r.soort() == ResolvedRegel.Soort.KENMERKTOEKENNING) continue;
            if (!(r.doel() instanceof Expr.AttribuutRef d) || d.objecttype() == null || d.attribuut() == null) continue;
            Class<?> k = beanKlasse(d.objecttype(), cl);
            if (k == null) continue;
            Function<Object, Object> lezer;
            try {
                lezer = Accessors.getter(k, d.attribuut());
            } catch (IllegalStateException e) {
                continue;   // a member the bean model gave no field of its own: the build refuses its writer by name
            }
            perEngineNaam.put(model.geëmitteerdeRegelNaam(r.naam()), new Schrijver(i, r.naam(), d.attribuut(), k, lezer));
        }
    }

    private static Class<?> beanKlasse(Objecttype ot, ClassLoader cl) {
        try {
            return Class.forName(FactModelGenerator.PAKKET + "." + Namen.ident(ot.naam, true), true, cl);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Override public void beforeMatchFired(BeforeMatchFiredEvent e) {
        Schrijver s = perEngineNaam.get(e.getMatch().getRule().getName());
        beanVoor = s == null ? null : onderwerp(e.getMatch().getObjects(), s.klasse());
        waardeVoor = beanVoor == null ? null : s.lezer().apply(beanVoor);
    }

    @Override public void afterMatchFired(AfterMatchFiredEvent e) {
        Schrijver s = perEngineNaam.get(e.getMatch().getRule().getName());
        if (s == null || beanVoor == null) return;
        Object na = s.lezer().apply(beanVoor);
        if (!Objects.equals(waardeVoor, na)) {
            recent.addLast(new Schrijfdaad(s, beanVoor, na));
            if (recent.size() > VENSTER) recent.removeFirst();
        }
        beanVoor = null;
    }

    /** The subject of a firing: the first matched object of the target's bean class. Every rule both transpilers emit
     *  binds its subject in its first pattern. */
    private static Object onderwerp(List<Object> objecten, Class<?> klasse) {
        for (Object o : objecten) if (klasse.isInstance(o)) return o;
        return null;
    }

    /** The sentence for an attribute of one object that two or more writers changed to different values in the last
     *  firings, or null when the last firings show none. {@code label} gives a case label for a bean, null for an
     *  object a creation minted. */
    String conflict(Function<Object, String> label) {
        // per object and attribute, in the order the window first shows them: each writer at its last value
        Map<Object, Map<String, Map<Schrijver, Object>>> perDoel = new IdentityHashMap<>();
        List<Object[]> volgorde = new ArrayList<>();
        for (Schrijfdaad d : recent) {
            Map<String, Map<Schrijver, Object>> perLid = perDoel.computeIfAbsent(d.bean(), b -> new LinkedHashMap<>());
            String lid = Namen.norm(d.schrijver().attribuut());
            if (!perLid.containsKey(lid)) volgorde.add(new Object[] { d.bean(), lid });
            perLid.computeIfAbsent(lid, l -> new LinkedHashMap<>()).put(d.schrijver(), d.waarde());
        }
        for (Object[] doel : volgorde) {
            Map<Schrijver, Object> schrijvers = perDoel.get(doel[0]).get((String) doel[1]);
            if (schrijvers.size() < 2 || schrijvers.values().stream().distinct().count() < 2) continue;
            List<Map.Entry<Schrijver, Object>> lijst = new ArrayList<>(schrijvers.entrySet());
            lijst.sort(Comparator.comparingInt(en -> en.getKey().volgorde()));
            List<String> delen = new ArrayList<>();
            for (Map.Entry<Schrijver, Object> en : lijst)
                delen.add("de regel \"" + en.getKey().regel() + "\" schrijft "
                        + (en.getValue() == null ? "leeg" : en.getValue()));
            String opsomming = delen.size() == 2 ? delen.get(0) + " en " + delen.get(1)
                    : String.join(", ", delen.subList(0, delen.size() - 1)) + " en " + delen.get(delen.size() - 1);
            String l = label.apply(doel[0]);
            String type = typeNaam.getOrDefault(doel[0].getClass(), doel[0].getClass().getSimpleName());
            return "geen fixpunt: " + aantal(lijst.size()) + " schrijvers van één doel geven verschillende waarden — "
                    + opsomming + " in het attribuut " + lijst.get(0).getKey().attribuut() + " van "
                    + (l == null ? "een aangemaakte " + type : l + ", een " + type)
                    + "; divergentie is een fout (RUNTIME-SEMANTICS ch. 5.7)";
        }
        return null;
    }

    private static String aantal(int n) {
        return switch (n) { case 2 -> "twee"; case 3 -> "drie"; case 4 -> "vier"; default -> String.valueOf(n); };
    }
}
