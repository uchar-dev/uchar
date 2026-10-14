package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.ir.Conditie;
import dev.uchar.regelspraak.ir.Default;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.ir.ResolvedRegel;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;
import dev.uchar.regelspraak.transpiler.kern.Namen;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The effective defaults stacks of RUNTIME-SEMANTICS ch. 7.10, one per (concrete type, target), composed as the
 * reference interpreter composes them ({@code Desugar.effective_defaults}), with the refusals in this branch's words.
 * This class is the backend's only composition of ch. 7.10, which both renderings of the analysis carry; until card
 * tms-omschakeling (2026-10-10) it was a copy of the destructive exec-model lowering's, kept apart so that the trial of
 * truth maintenance left that lowering untouched.
 *
 * <p>A subtype's own declarations fold over its supertype's effective stack, highest first: its own exceptions, the
 * inherited exceptions, the base (its own replaces the inherited one), the initialisations. A level that declares no
 * group on a target its chain carries a group on contributes its ordinary rule instead (SUBTYPING §4.1 rules 1 and 4):
 * the computing rule as the base, under its condition or the empty {@code all}, and the initialisation as the
 * initialisation default with its condition. Such a rule is absorbed on the concrete types whose stack took it
 * ({@link #geabsorbeerdOp}) and must not fire there as a rule as well.
 */
final class Stapels {

    /** One effective stack: the groups that run on instances of exactly {@code eigenaar}, more than one only for a
     *  declared-unordered root. */
    record Stapel(Objecttype eigenaar, List<PrioritizedDefaults> leden) {}

    /** A composition this branch does not lower, with the reason. */
    static final class Onsamenstelbaar extends RuntimeException {
        Onsamenstelbaar(String reden) { super(reden); }
    }

    /** The vacuously true justification an unconditional rule gets when it becomes a base, the shape of the ch. 7.7
     *  desugaring. */
    private static final Conditie ALTIJD = new Conditie.Samengesteld(Conditie.Kwantor.ALLE, 0, List.of(), null);

    private final RegelSpraakModel model;
    private final List<PrioritizedDefaults> defaults;
    /** The level each default was declared on, by identity: a default is read there (ch. 7.10). An exception group's
     *  defaults are read on the level of the default that carries them and are not in here. */
    private final Map<Default, Objecttype> herkomst = new IdentityHashMap<>();

    Stapels(RegelSpraakModel model, List<PrioritizedDefaults> defaults) {
        this.model = model;
        this.defaults = defaults;
    }

    /** Every effective stack of the model, per type in the model's order and per target in the order of
     *  {@link #effectieveStapels}. */
    List<Stapel> stapels() {
        List<Stapel> uit = new ArrayList<>();
        for (Objecttype ot : model.objecttypen.values())
            for (List<PrioritizedDefaults> leden : effectieveStapels(ot, ketenGroepSleutels(ot)).values())
                uit.add(new Stapel(ot, leden));
        return uit;
    }

    /** The level default {@code d} of a stack owned by {@code eigenaar} is read on. */
    Objecttype niveau(Default d, Objecttype eigenaar) {
        return herkomst.getOrDefault(d, eigenaar);
    }

    /** Whether {@code otype} itself declares a group on its attribute {@code attr}: a rule beside it is not
     *  inheritance but WF-050's single-writer lift, which the fold leaves alone. */
    boolean eigenGroep(Objecttype otype, String attr) {
        String sleutel = "A:" + Namen.norm(attr);
        for (PrioritizedDefaults l : defaults)
            if (model.objecttypeOpNaam(l.onderwerp()) == otype && sleutel(l).equals(sleutel)) return true;
        return false;
    }

    /** The concrete types on which an ordinary rule writing ({@code otype}, {@code attr}) is folded into an effective
     *  stack: every type that is an {@code otype} and whose chain declares a group on the target. Empty where
     *  {@code otype} itself declares one ({@link #eigenGroep}). */
    List<Objecttype> geabsorbeerdOp(Objecttype otype, String attr) {
        if (eigenGroep(otype, attr)) return List.of();
        String sleutel = "A:" + Namen.norm(attr);
        List<Objecttype> uit = new ArrayList<>();
        for (Objecttype t : model.objecttypen.values())
            if (t.isEen(otype) && ketenGroepSleutels(t).contains(sleutel)) uit.add(t);
        return uit;
    }

    /** The stacks of {@code type} by target key. {@code absorb} is the key set of the stack's owner on which an
     *  ordinary rule counts as a base, threaded down so the answer does not change with the depth. */
    private LinkedHashMap<String, List<PrioritizedDefaults>> effectieveStapels(Objecttype type, Set<String> absorb) {
        LinkedHashMap<String, List<PrioritizedDefaults>> eigen = new LinkedHashMap<>();
        for (PrioritizedDefaults l : defaults)
            if (Namen.norm(l.onderwerp()).equals(Namen.norm(type.naam))) {
                eigen.computeIfAbsent(sleutel(l), k -> new ArrayList<>()).add(l);
                for (Default d : l.defaults()) herkomst.put(d, type);
            }
        for (String k : absorb)
            if (!eigen.containsKey(k)) {
                PrioritizedDefaults m = regelLid(type, k);
                if (m != null) eigen.put(k, List.of(m));
            }
        LinkedHashMap<String, List<PrioritizedDefaults>> geerfd =
                type.supertype == null ? new LinkedHashMap<>() : effectieveStapels(type.supertype, absorb);
        LinkedHashMap<String, List<PrioritizedDefaults>> uit = new LinkedHashMap<>(eigen);
        geerfd.forEach((k, ouderLeden) -> {
            List<PrioritizedDefaults> eigenLeden = eigen.get(k);
            if (eigenLeden == null) {
                uit.put(k, ouderLeden);
                return;
            }
            if (eigenLeden.size() == 1 && ouderLeden.size() > 1)
                throw new Onsamenstelbaar("het doel " + doelnaam(k) + " op " + type.naam + " zet één eigen groep op een"
                        + " geërfde ongeordende set van " + ouderLeden.size() + " groepen: SUBTYPING §4.1 regel 3"
                        + " definieert die lezing (het geërfde blok verhuist heel, boven de basis en onder de eigen"
                        + " uitzonderingen), maar ze is niet verlaagd, want ze is een prioriteit in drie lagen en de leden"
                        + " van een groep verzoenen als gelijken (RUNTIME-SEMANTICS ch. 7.10)");
            if (eigenLeden.size() != 1 || ouderLeden.size() != 1)
                throw new Onsamenstelbaar("het doel " + doelnaam(k) + " op " + type.naam + " draagt " + eigenLeden.size()
                        + " eigen en " + ouderLeden.size() + " geërfde groep(en): een vouw van een eigen ongeordende set"
                        + " over een geërfde stapel heeft geen gedefinieerde lezing (RUNTIME-SEMANTICS ch. 7.10)");
            uit.put(k, List.of(voegSamen(eigenLeden.get(0), ouderLeden.get(0))));
        });
        return uit;
    }

    /** {@code level}'s own contribution to the attribute target {@code key} where it declares no group there: its
     *  ordinary rules read as the ch. 7.7 shape. Null when no ordinary rule on the level writes the target. Every rule
     *  on the target is collected and refused by name where it cannot fold, since the fold takes it out of the rule
     *  circuit: a rule passed over would be silenced without becoming a base. */
    private PrioritizedDefaults regelLid(Objecttype level, String key) {
        List<ResolvedRegel> comps = new ArrayList<>(), inits = new ArrayList<>(), alle = new ArrayList<>();
        for (ResolvedRegel r : model.resolvedRegels) {
            if (!(r.doel() instanceof Expr.AttribuutRef d) || d.attribuut() == null) continue;
            if (d.objecttype() != level || !("A:" + Namen.norm(d.attribuut())).equals(key)) continue;
            switch (r.soort()) {
                case GELIJKSTELLING, BEREKENING -> { comps.add(r); alle.add(r); }
                case INITIALISATIE -> { inits.add(r); alle.add(r); }
                default -> { }
            }
        }
        if (alle.isEmpty()) return null;
        for (ResolvedRegel r : alle)
            if (r.geldigheid() != null && !r.geldigheid().altijd())
                throw new Onsamenstelbaar("het doel " + doelnaam(key) + " op " + level.naam + " wordt naast een geërfde"
                        + " defaultsstapel geschreven door de regel \"" + r.naam() + "\" met een geldigheidsvenster: een"
                        + " basis met een venster zet versiekeuze in de stapel, die ch. 7.4 van verdedigbaarheid scheidt"
                        + " (RUNTIME-SEMANTICS ch. 7.10)");
        for (ResolvedRegel r : alle)
            if (r.waarde() == null)
                throw new Onsamenstelbaar("het doel " + doelnaam(key) + " op " + level.naam + " wordt naast een geërfde"
                        + " defaultsstapel geschreven door de regel \"" + r.naam() + "\" zonder waarde: een basis zonder"
                        + " consequentie beslist niets en wordt niet gevouwen (RUNTIME-SEMANTICS ch. 7.10)");
        if (comps.size() > 1 || inits.size() > 1)
            throw new Onsamenstelbaar("het doel " + doelnaam(key) + " op " + level.naam + " wordt naast een geërfde"
                    + " defaultsstapel geschreven door " + comps.size() + " rekenende regel(s) en " + inits.size()
                    + " initialisatie(s): de v0-betekenis van twee schrijvers op één doel is de volgorde op de agenda,"
                    + " die prioritized defaults vervangen (RUNTIME-SEMANTICS ch. 7.10)");
        List<Default> ds = new ArrayList<>();
        String attr = null;
        for (ResolvedRegel r : comps) {
            attr = ((Expr.AttribuutRef) r.doel()).attribuut();
            ds.add(new Default(null, r.voorwaarde() != null ? r.voorwaarde() : ALTIJD, r.waarde(), false, null, r.naam()));
        }
        // the initialisation keeps its condition: justified where the target is empty in the input and the condition
        // holds; folded without it, it wrote its value where the condition failed
        for (ResolvedRegel r : inits) {
            attr = ((Expr.AttribuutRef) r.doel()).attribuut();
            ds.add(new Default(null, r.voorwaarde(), r.waarde(), true, null, r.naam()));
        }
        for (Default d : ds) herkomst.put(d, level);
        return new PrioritizedDefaults("regel als basis: " + attr + " van " + level.naam, level.naam, null, attr, false,
                List.copyOf(ds), "SUBTYPING §4.1 rules 1/4: an ordinary rule is the base of its level", null);
    }

    /** The target keys on which {@code t}'s own chain declares a group. */
    private Set<String> ketenGroepSleutels(Objecttype t) {
        Set<String> uit = new LinkedHashSet<>();
        for (PrioritizedDefaults l : defaults) {
            Objecttype onderwerp = model.objecttypeOpNaam(l.onderwerp());
            if (onderwerp != null && t.isEen(onderwerp)) uit.add(sleutel(l));
        }
        return uit;
    }

    /** The block order of ch. 7.10, one own group over one inherited group. */
    private static PrioritizedDefaults voegSamen(PrioritizedDefaults eigen, PrioritizedDefaults geerfd) {
        List<Default> uit = new ArrayList<>();
        uit.addAll(uitzonderingen(eigen));
        uit.addAll(uitzonderingen(geerfd));
        Default basis = basisVan(eigen) != null ? basisVan(eigen) : basisVan(geerfd);
        if (basis != null) uit.add(basis);
        uit.addAll(initialisaties(geerfd));
        uit.addAll(initialisaties(eigen));
        return new PrioritizedDefaults(eigen.naam(), eigen.onderwerp(), eigen.kenmerk(), eigen.attribuut(),
                eigen.ongeordend(), List.copyOf(uit), eigen.bron(), eigen.geldigheid());
    }

    /** The lowest ordinary default, and only when it decides something itself: a group-default delegates to its
     *  exception groups and is no base, and a declared-unordered member has no lowest one, so its block travels whole. */
    private static Default basisVan(PrioritizedDefaults l) {
        if (l.ongeordend()) return null;
        Default laatste = null;
        for (Default d : l.defaults()) if (!d.initialisatie()) laatste = d;
        return laatste != null && laatste.heeftEigenInhoud() ? laatste : null;
    }

    private static List<Default> uitzonderingen(PrioritizedDefaults l) {
        Default basis = basisVan(l);
        List<Default> uit = new ArrayList<>();
        for (Default d : l.defaults()) if (!d.initialisatie() && d != basis) uit.add(d);
        return uit;
    }

    private static List<Default> initialisaties(PrioritizedDefaults l) {
        List<Default> uit = new ArrayList<>();
        for (Default d : l.defaults()) if (d.initialisatie()) uit.add(d);
        return uit;
    }

    /** The kind-discriminated key of a group's target: a characteristic and an attribute of one name stay apart. */
    static String sleutel(PrioritizedDefaults l) {
        return (l.isKenmerk() ? "K:" : "A:") + Namen.norm(l.doel());
    }

    private static String doelnaam(String sleutel) {
        return (sleutel.startsWith("K:") ? "kenmerk '" : "attribuut '") + sleutel.substring(2) + "'";
    }
}
