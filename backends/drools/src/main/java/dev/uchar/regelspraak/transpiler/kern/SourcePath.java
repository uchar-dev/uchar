package dev.uchar.regelspraak.transpiler.kern;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Feittype;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;
import dev.uchar.regelspraak.model.RegelSpraakModel.Rol;
import dev.uchar.regelspraak.model.RegelSpraakModel.RolBindingRef;
import dev.uchar.regelspraak.schema.FactModelGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * IR-SPEC ch. 4.3 / WF-102 — the fold source's RANGE as a PATH, resolved against the bean model ONCE for both
 * transpiler branches. It answers a single question: given the fold SUBJECT, the ordered {@code sourcePath} steps
 * and the fold's {@code elementType}, WHICH bean field does each hop read and in WHICH direction — or, when the
 * model cannot carry the walk, WHY not, in one sentence both branches print.
 *
 * <p><b>The reading (RUNTIME-SEMANTICS ch. 3.4b).</b> The range is the FLAT UNION of the {@code elementType}
 * instances reachable from the subject along the whole path; an element reachable through more than one
 * intermediate contributes ONCE. The path is a connected walk: each step's OPPOSITE role is filled by the type its
 * predecessor reaches (the first one by the subject), and the last step's bound role lands on the element type.
 * Nothing here is derived from free text or from the endpoint type PAIR — the steps are the claim, which is the
 * whole reason the field exists (a by-pair search binds a coincidental DIRECT relation and folds the wrong set).
 *
 * <p><b>The directions.</b> §13.3.9 stores a relation in exactly one scalar bean field, on whichever end is the ONE
 * side, named after the role of the thing it points AT. So a step whose bound role is TO-MANY stores its link on
 * the step's TARGET, named after the opposite role ({@link Step#backward} — read {@code target.veld == bron}),
 * and a step whose bound role is TO-ONE stores it on the step's SOURCE, named after the bound role (read
 * {@code bron.veld == target}). A step whose field the bean model does not derive at all is a loud per-form
 * refusal, never a silent hop.
 *
 * <p><b>The consequence for the DEDUP, which is why both branches can share one plan.</b> A backward hop is
 * FUNCTIONAL from the target (one scalar getter) and a forward hop is functional from the source. A path that is a
 * forward run followed by a backward run is therefore decided by ONE equality between two getter chains — one
 * walked from the subject, one from the element — and no element can be reached twice. Only where a BACKWARD hop
 * is followed by a FORWARD one can two intermediates meet in the same element; there the intermediate is a genuine
 * existential, and {@link Plan#intermediateNodes} names it so both branches can quantify it EXISTENTIALLY (one tuple per
 * element) instead of joining it into the accumulate source (one tuple per route).
 */
public final class SourcePath {
    private SourcePath() {}

    /** One resolved hop, reduced to what a consumer reads: {@code beanField} is the bean property the join reads, and
     *  {@code backward} says on which end it lives — true -> the field is on the hop's TARGET and holds its
     *  SOURCE; false -> the field is on the source and holds the target. The two object types the hop connects are
     *  NOT repeated here: they are nodes {@code i-1} and {@code i} of {@link Plan#nodeTypes}, and a second copy
     *  would be a second thing to keep true. The feittype and role NAMES stop at {@link #parse}, which is where
     *  they are resolved and where the refusal sentences quote them. */
    public record Step(boolean backward, String beanField) {}

    /** The lowering plan for one path, or a refusal.
     *
     *  @param stappen       the hops, subject-outward; empty on a refusal.
     *  @param nodeTypes    the object type at every NODE, {@code nodeTypes[0]} the subject and
     *                       {@code nodeTypes[n]} the fold's elementType (which may be a strict SUBTYPE of
     *                       what the last step's role fills — ch. 2.2's exclusion direction).
     *  @param voorwaarts    how many LEADING steps are forward, i.e. the highest node index still reachable from
     *                       the subject by a pure getter chain.
     *  @param backwardFrom the lowest node index still reachable from the ELEMENT by a pure getter chain, i.e.
     *                       the start of the trailing backward run.
     *  @param weigering     null when the plan carries, else the one-sentence per-form reason.
     */
    public record Plan(List<Step> steps, List<Objecttype> nodeTypes, int forward, int backwardFrom,
                       String refusal) {

        /** Whether the two functional walks MEET — the path is a forward run followed by a backward run, so one
         *  equality decides it and no intermediate needs quantifying. */
        public boolean meets() { return forward >= backwardFrom; }

        /** The node index the two walks meet at (only meaningful when {@link #meets()}). */
        public int meeting() { return backwardFrom; }

        /** The node indices that need an EXISTENTIAL binding — empty exactly when {@link #meets()}. */
        public List<Integer> intermediateNodes() {
            List<Integer> out = new ArrayList<>();
            for (int i = forward + 1; i <= backwardFrom - 1; i++) out.add(i);
            return out;
        }

        /** The backward {@code veld}s to apply, in order, to walk from the ELEMENT up to node {@code doel}
         *  (nodes {@code backwardFrom}..n only). */
        public List<String> chainFromElement(int doel) {
            List<String> out = new ArrayList<>();
            for (int i = steps.size(); i > doel; i--) out.add(steps.get(i - 1).beanField());
            return out;
        }

        /** The forward {@code veld}s to apply, in order, to walk from the SUBJECT down to node {@code doel}
         *  (nodes 0..{@code forward} only). */
        public List<String> chainFromSubject(int doel) {
            List<String> out = new ArrayList<>();
            for (int i = 1; i <= doel; i++) out.add(steps.get(i - 1).beanField());
            return out;
        }
    }

    private static Plan refuse(String reden) { return new Plan(List.of(), List.of(), 0, 0, reden); }

    /**
     * Resolve {@code pad} against the model. {@code subject} is the fold subject (node 0) and {@code element} the
     * fold's declared elementType (node n). Every failure is a SENTENCE, never a null: a step that names no
     * feittype/role, a step that does not join what its predecessor reaches, a landing beside the element type, or
     * a hop the bean model derives no field for.
     */
    public static Plan parse(RegelSpraakModel model, Objecttype subject, List<RolBindingRef> pad, Objecttype element) {
        if (model == null || subject == null || element == null)
            return refuse("sourcePath zonder onderwerp of elementtype (WF-102)");
        if (pad == null || pad.size() < 2)
            return refuse("een sourcePath draagt minstens TWEE stappen, gevonden " + (pad == null ? 0 : pad.size())
                    + " — het eenstapsbereik heet sourceRole (WF-102 (b))");
        List<Step> steps = new ArrayList<>();
        List<Objecttype> nodes = new ArrayList<>();
        nodes.add(subject);
        for (int i = 0; i < pad.size(); i++) {
            RolBindingRef rb = pad.get(i);
            Objecttype van = nodes.get(i);
            boolean last = i == pad.size() - 1;
            Feittype ft = factTypeByName(model, rb == null ? null : rb.feittype());
            if (ft == null)
                return refuse("sourcePath-stap " + i + " noemt het feittype '" + (rb == null ? null : rb.feittype())
                        + "', dat dit document niet declareert (WF-047)");
            Rol bound = roleByName(ft, rb.rol());
            if (bound == null)
                return refuse("sourcePath-stap " + i + " noemt de rol '" + rb.rol() + "', die het feittype '"
                        + ft.naam + "' niet declareert (WF-047)");
            Rol opposite = oppositeRole(ft, bound);
            if (opposite == null)
                return refuse("sourcePath-stap " + i + " bindt de rol '" + bound.naam() + "' van feittype '"
                        + ft.naam + "', dat geen tegenoverliggende rol heeft (WF-047)");
            // (d) the walk is CONNECTED: the opposite role must be filled by what the previous step reaches.
            if (!fills(van, opposite))
                return refuse("sourcePath-stap " + i + ": de tegenoverliggende rol '" + opposite.naam() + "' vult '"
                        + opposite.objecttypeNaam() + "', maar deze stap begint bij '" + van.naam + "' ("
                        + (i == 0 ? "het vouwonderwerp" : "het type dat de vorige stap bereikt")
                        + ") — een sourcePath is een verbonden wandeling (WF-102 (d))");
            Objecttype filler = model.objecttypeOpNaam(bound.objecttypeNaam());
            if (filler == null)
                return refuse("sourcePath-stap " + i + ": de gebonden rol '" + bound.naam() + "' vult '"
                        + bound.objecttypeNaam() + "', een objecttype dat dit document niet declareert (WF-047)");
            // (d) the LANDING, two-sided exactly as WF-102 has it: elementType may be a strict SUBTYPE of
            // what the last step's role fills (ch. 2.2's exclusion direction), and the pattern class is then the
            // narrower one — which is what makes the element the population and the relation merely the reach.
            if (last && !compatible(element, filler))
                return refuse("sourcePath-stap " + i + ": de gebonden rol '" + bound.naam() + "' vult '"
                        + filler.naam + "', maar het elementType is '" + element.naam
                        + "' — het pad moet op het elementtype landen (WF-102 (d))");
            Objecttype naar = last ? element : filler;
            // §13.3.9 the storage direction. A to-many bound role keeps its link at the FAR end, named after the
            // opposite role; a to-one bound role keeps it here, named after itself. The question is asked of the
            // ONE shared member derivation (FactModelGenerator), so a path hop can never assume a field the beans
            // do not carry — the failure that used to build a KieBase and die at first fire.
            boolean toMany = bound.meervoudig();
            Objecttype carrier = toMany ? naar : van, doel = toMany ? van : naar;
            String roleField = toMany ? opposite.naam() : bound.naam();
            if (!FactModelGenerator.hasReferenceField(model, carrier, roleField, doel))
                return refuse("sourcePath-stap " + i + " (" + van.naam + " -> " + naar.naam + ") heeft geen beanveld — "
                        + FactModelGenerator.noJoinFieldReason(model, van, bound.naam(), naar)
                        + " (§13.3.9 opslagrichting)");
            steps.add(new Step(toMany, Namen.ident(roleField, false)));
            nodes.add(naar);
        }
        int forward = 0;
        while (forward < steps.size() && !steps.get(forward).backward()) forward++;
        int backwardStart = steps.size();
        while (backwardStart > 0 && steps.get(backwardStart - 1).backward()) backwardStart--;
        return new Plan(List.copyOf(steps), List.copyOf(nodes), forward, backwardStart, null);
    }

    private static Feittype factTypeByName(RegelSpraakModel model, String naam) {
        if (naam == null) return null;
        for (Feittype ft : model.feittypen.values())
            if (ft.naam.equalsIgnoreCase(naam)) return ft;
        return null;
    }

    /** The DECLARED role name, never a plural and never a best-effort one — the wire binds what the author
     *  declared (the same narrowing {@code elementRolBinding} makes on the emitting side). */
    private static Rol roleByName(Feittype ft, String naam) {
        if (naam == null) return null;
        for (Rol r : ft.rollen)
            if (r.naam() != null && r.naam().equalsIgnoreCase(naam)) return r;
        return null;
    }

    /** The other end of {@code bound}. A §13.3.9.2 wederkerig feittype carries ONE role that is its own
     *  opposite, so the step's two ends are the same role. */
    private static Rol oppositeRole(Feittype ft, Rol bound) {
        if (ft.wederkerig && ft.rollen.size() == 1) return bound;
        Rol out = null;
        for (Rol r : ft.rollen) if (r != bound) { if (out != null) return null; out = r; }
        return out;
    }

    /** Does {@code ot} FILL {@code rol}? One-sided (⊑ the declared filler): a role holds every instance at or
     *  below its declared type and none above it — the verifier's {@code fills} on this very arm. */
    private static boolean fills(Objecttype ot, Rol rol) {
        for (Objecttype t : ot.keten())
            if (t.naam.equalsIgnoreCase(String.valueOf(rol.objecttypeNaam()))) return true;
        return false;
    }

    /** ⊑-compatible in EITHER direction — WF-102's landing test, deliberately as wide as WF-047's, so the two
     *  spellings of one fold cannot disagree about a subtype. */
    private static boolean compatible(Objecttype a, Objecttype b) {
        for (Objecttype t : a.keten()) if (t == b || t.naam.equalsIgnoreCase(b.naam)) return true;
        for (Objecttype t : b.keten()) if (t == a || t.naam.equalsIgnoreCase(a.naam)) return true;
        return false;
    }
}
