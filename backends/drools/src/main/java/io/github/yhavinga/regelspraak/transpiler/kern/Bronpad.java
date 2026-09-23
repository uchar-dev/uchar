package io.github.yhavinga.regelspraak.transpiler.kern;

import io.github.yhavinga.regelspraak.model.RegelSpraakModel;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Feittype;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Objecttype;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Rol;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.RolBindingRef;
import io.github.yhavinga.regelspraak.schema.FactModelGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * IR-SPEC ch. 4.3 / WF-102 — the fold source's RANGE as a PATH, resolved against the bean model ONCE for both
 * transpiler branches. It answers a single question: given the fold SUBJECT, the ordered {@code bronPad} steps and
 * the fold's {@code elementObjecttype}, WHICH bean field does each hop read and in WHICH direction — or, when the
 * model cannot carry the walk, WHY not, in one sentence both branches print.
 *
 * <p><b>The reading (RUNTIME-SEMANTICS ch. 3.4b).</b> The range is the FLAT UNION of the {@code elementObjecttype}
 * instances reachable from the subject along the whole path; an element reachable through more than one
 * intermediate contributes ONCE. The path is a connected walk: each step's OPPOSITE role is filled by the type its
 * predecessor reaches (the first one by the subject), and the last step's bound role lands on the element type.
 * Nothing here is derived from free text or from the endpoint type PAIR — the steps are the claim, which is the
 * whole reason the field exists (a by-pair search binds a coincidental DIRECT relation and folds the wrong set).
 *
 * <p><b>The directions.</b> §13.3.9 stores a relation in exactly one scalar bean field, on whichever end is the ONE
 * side, named after the role of the thing it points AT. So a step whose bound role is TO-MANY stores its link on
 * the step's TARGET, named after the opposite role ({@link Stap#achterwaarts} — read {@code target.veld == bron}),
 * and a step whose bound role is TO-ONE stores it on the step's SOURCE, named after the bound role (read
 * {@code bron.veld == target}). A step whose field the bean model does not derive at all is a loud per-form
 * refusal, never a silent hop.
 *
 * <p><b>The consequence for the DEDUP, which is why both branches can share one plan.</b> A backward hop is
 * FUNCTIONAL from the target (one scalar getter) and a forward hop is functional from the source. A path that is a
 * forward run followed by a backward run is therefore decided by ONE equality between two getter chains — one
 * walked from the subject, one from the element — and no element can be reached twice. Only where a BACKWARD hop
 * is followed by a FORWARD one can two intermediates meet in the same element; there the intermediate is a genuine
 * existential, and {@link Plan#tussenknopen} names it so both branches can quantify it EXISTENTIALLY (one tuple per
 * element) instead of joining it into the accumulate source (one tuple per route).
 */
public final class Bronpad {
    private Bronpad() {}

    /** One resolved hop, reduced to what a consumer reads: {@code beanveld} is the bean property the join reads, and
     *  {@code achterwaarts} says on which end it lives — true -> the field is on the hop's TARGET and holds its
     *  SOURCE; false -> the field is on the source and holds the target. The two object types the hop connects are
     *  NOT repeated here: they are nodes {@code i-1} and {@code i} of {@link Plan#knoopTypen}, and a second copy
     *  would be a second thing to keep true. The feittype and role NAMES stop at {@link #ontleed}, which is where
     *  they are resolved and where the refusal sentences quote them. */
    public record Stap(boolean achterwaarts, String beanveld) {}

    /** The lowering plan for one path, or a refusal.
     *
     *  @param stappen       the hops, subject-outward; empty on a refusal.
     *  @param knoopTypen    the object type at every NODE, {@code knoopTypen[0]} the subject and
     *                       {@code knoopTypen[n]} the fold's elementObjecttype (which may be a strict SUBTYPE of
     *                       what the last step's role fills — ch. 2.2's exclusion direction).
     *  @param voorwaarts    how many LEADING steps are forward, i.e. the highest node index still reachable from
     *                       the subject by a pure getter chain.
     *  @param achterwaartsVanaf the lowest node index still reachable from the ELEMENT by a pure getter chain, i.e.
     *                       the start of the trailing backward run.
     *  @param weigering     null when the plan carries, else the one-sentence per-form reason.
     */
    public record Plan(List<Stap> stappen, List<Objecttype> knoopTypen, int voorwaarts, int achterwaartsVanaf,
                       String weigering) {

        /** Whether the two functional walks MEET — the path is a forward run followed by a backward run, so one
         *  equality decides it and no intermediate needs quantifying. */
        public boolean ontmoet() { return voorwaarts >= achterwaartsVanaf; }

        /** The node index the two walks meet at (only meaningful when {@link #ontmoet()}). */
        public int ontmoeting() { return achterwaartsVanaf; }

        /** The node indices that need an EXISTENTIAL binding — empty exactly when {@link #ontmoet()}. */
        public List<Integer> tussenknopen() {
            List<Integer> uit = new ArrayList<>();
            for (int i = voorwaarts + 1; i <= achterwaartsVanaf - 1; i++) uit.add(i);
            return uit;
        }

        /** The backward {@code veld}s to apply, in order, to walk from the ELEMENT up to node {@code doel}
         *  (nodes {@code achterwaartsVanaf}..n only). */
        public List<String> ketenVanafElement(int doel) {
            List<String> uit = new ArrayList<>();
            for (int i = stappen.size(); i > doel; i--) uit.add(stappen.get(i - 1).beanveld());
            return uit;
        }

        /** The forward {@code veld}s to apply, in order, to walk from the SUBJECT down to node {@code doel}
         *  (nodes 0..{@code voorwaarts} only). */
        public List<String> ketenVanafOnderwerp(int doel) {
            List<String> uit = new ArrayList<>();
            for (int i = 1; i <= doel; i++) uit.add(stappen.get(i - 1).beanveld());
            return uit;
        }
    }

    private static Plan weiger(String reden) { return new Plan(List.of(), List.of(), 0, 0, reden); }

    /**
     * Resolve {@code pad} against the model. {@code subject} is the fold subject (node 0) and {@code element} the
     * fold's declared elementObjecttype (node n). Every failure is a SENTENCE, never a null: a step that names no
     * feittype/role, a step that does not join what its predecessor reaches, a landing beside the element type, or
     * a hop the bean model derives no field for.
     */
    public static Plan ontleed(RegelSpraakModel model, Objecttype subject, List<RolBindingRef> pad, Objecttype element) {
        if (model == null || subject == null || element == null)
            return weiger("bronPad zonder onderwerp of elementtype (WF-102)");
        if (pad == null || pad.size() < 2)
            return weiger("een bronPad draagt minstens TWEE stappen, gevonden " + (pad == null ? 0 : pad.size())
                    + " — het eenstapsbereik heet bronRol (WF-102 (b))");
        List<Stap> stappen = new ArrayList<>();
        List<Objecttype> knopen = new ArrayList<>();
        knopen.add(subject);
        for (int i = 0; i < pad.size(); i++) {
            RolBindingRef rb = pad.get(i);
            Objecttype van = knopen.get(i);
            boolean laatste = i == pad.size() - 1;
            Feittype ft = feittypeOpNaam(model, rb == null ? null : rb.feittype());
            if (ft == null)
                return weiger("bronPad-stap " + i + " noemt het feittype '" + (rb == null ? null : rb.feittype())
                        + "', dat dit document niet declareert (WF-047)");
            Rol gebonden = rolOpNaam(ft, rb.rol());
            if (gebonden == null)
                return weiger("bronPad-stap " + i + " noemt de rol '" + rb.rol() + "', die het feittype '"
                        + ft.naam + "' niet declareert (WF-047)");
            Rol tegenover = tegenoverliggend(ft, gebonden);
            if (tegenover == null)
                return weiger("bronPad-stap " + i + " bindt de rol '" + gebonden.naam() + "' van feittype '"
                        + ft.naam + "', dat geen tegenoverliggende rol heeft (WF-047)");
            // (d) the walk is CONNECTED: the opposite role must be filled by what the previous step reaches.
            if (!vult(van, tegenover))
                return weiger("bronPad-stap " + i + ": de tegenoverliggende rol '" + tegenover.naam() + "' vult '"
                        + tegenover.objecttypeNaam() + "', maar deze stap begint bij '" + van.naam + "' ("
                        + (i == 0 ? "het vouwonderwerp" : "het type dat de vorige stap bereikt")
                        + ") — een bronPad is een verbonden wandeling (WF-102 (d))");
            Objecttype vuller = model.objecttypeOpNaam(gebonden.objecttypeNaam());
            if (vuller == null)
                return weiger("bronPad-stap " + i + ": de gebonden rol '" + gebonden.naam() + "' vult '"
                        + gebonden.objecttypeNaam() + "', een objecttype dat dit document niet declareert (WF-047)");
            // (d) the LANDING, two-sided exactly as WF-102 has it: elementObjecttype may be a strict SUBTYPE of
            // what the last step's role fills (ch. 2.2's exclusion direction), and the pattern class is then the
            // narrower one — which is what makes the element the population and the relation merely the reach.
            if (laatste && !verenigbaar(element, vuller))
                return weiger("bronPad-stap " + i + ": de gebonden rol '" + gebonden.naam() + "' vult '"
                        + vuller.naam + "', maar het elementObjecttype is '" + element.naam
                        + "' — het pad moet op het elementtype landen (WF-102 (d))");
            Objecttype naar = laatste ? element : vuller;
            // §13.3.9 the storage direction. A to-many bound role keeps its link at the FAR end, named after the
            // opposite role; a to-one bound role keeps it here, named after itself. The question is asked of the
            // ONE shared member derivation (FactModelGenerator), so a path hop can never assume a field the beans
            // do not carry — the failure that used to build a KieBase and die at first fire.
            boolean naarVeel = gebonden.meervoudig();
            Objecttype drager = naarVeel ? naar : van, doel = naarVeel ? van : naar;
            String rolveld = naarVeel ? tegenover.naam() : gebonden.naam();
            if (!FactModelGenerator.heeftReferentieVeld(model, drager, rolveld, doel))
                return weiger("bronPad-stap " + i + " (" + van.naam + " -> " + naar.naam + ") heeft geen beanveld — "
                        + FactModelGenerator.geenJoinveldReden(model, van, gebonden.naam(), naar)
                        + " (§13.3.9 opslagrichting)");
            stappen.add(new Stap(naarVeel, Namen.ident(rolveld, false)));
            knopen.add(naar);
        }
        int voorwaarts = 0;
        while (voorwaarts < stappen.size() && !stappen.get(voorwaarts).achterwaarts()) voorwaarts++;
        int achterVanaf = stappen.size();
        while (achterVanaf > 0 && stappen.get(achterVanaf - 1).achterwaarts()) achterVanaf--;
        return new Plan(List.copyOf(stappen), List.copyOf(knopen), voorwaarts, achterVanaf, null);
    }

    private static Feittype feittypeOpNaam(RegelSpraakModel model, String naam) {
        if (naam == null) return null;
        for (Feittype ft : model.feittypen.values())
            if (ft.naam.equalsIgnoreCase(naam)) return ft;
        return null;
    }

    /** The DECLARED role name, never a plural and never a best-effort one — the wire binds what the author
     *  declared (the same narrowing {@code elementRolBinding} makes on the emitting side). */
    private static Rol rolOpNaam(Feittype ft, String naam) {
        if (naam == null) return null;
        for (Rol r : ft.rollen)
            if (r.naam() != null && r.naam().equalsIgnoreCase(naam)) return r;
        return null;
    }

    /** The other end of {@code gebonden}. A §13.3.9.2 wederkerig feittype carries ONE role that is its own
     *  opposite, so the step's two ends are the same role. */
    private static Rol tegenoverliggend(Feittype ft, Rol gebonden) {
        if (ft.wederkerig && ft.rollen.size() == 1) return gebonden;
        Rol uit = null;
        for (Rol r : ft.rollen) if (r != gebonden) { if (uit != null) return null; uit = r; }
        return uit;
    }

    /** Does {@code ot} FILL {@code rol}? One-sided (⊑ the declared filler): a role holds every instance at or
     *  below its declared type and none above it — the verifier's {@code fills} on this very arm. */
    private static boolean vult(Objecttype ot, Rol rol) {
        for (Objecttype t : ot.keten())
            if (t.naam.equalsIgnoreCase(String.valueOf(rol.objecttypeNaam()))) return true;
        return false;
    }

    /** ⊑-compatible in EITHER direction — WF-102's landing test, deliberately as wide as WF-047's, so the two
     *  spellings of one fold cannot disagree about a subtype. */
    private static boolean verenigbaar(Objecttype a, Objecttype b) {
        for (Objecttype t : a.keten()) if (t == b || t.naam.equalsIgnoreCase(b.naam)) return true;
        for (Objecttype t : b.keten()) if (t == a || t.naam.equalsIgnoreCase(a.naam)) return true;
        return false;
    }
}
