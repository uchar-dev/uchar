package dev.uchar.regelspraak.transpiler.core;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.refusal.DroolsCode;
import dev.uchar.regelspraak.refusal.Reason;
import dev.uchar.regelspraak.model.RegelSpraakModel.FactType;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;
import dev.uchar.regelspraak.model.RegelSpraakModel.Role;
import dev.uchar.regelspraak.model.RegelSpraakModel.RoleBindingRef;
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
 * the step's TARGET, named after the opposite role ({@link Step#backward} — read {@code target.field == bron}),
 * and a step whose bound role is TO-ONE stores it on the step's SOURCE, named after the bound role (read
 * {@code source.field == target}). A step whose field the bean model does not derive at all is a loud per-form
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
     *  @param steps       the hops, subject-outward; empty on a refusal.
     *  @param nodeTypes    the object type at every NODE, {@code nodeTypes[0]} the subject and
     *                       {@code nodeTypes[n]} the fold's elementType (which may be a strict SUBTYPE of
     *                       what the last step's role fills — ch. 2.2's exclusion direction).
     *  @param forward    how many LEADING steps are forward, i.e. the highest node index still reachable from
     *                       the subject by a pure getter chain.
     *  @param backwardFrom the lowest node index still reachable from the ELEMENT by a pure getter chain, i.e.
     *                       the start of the trailing backward run.
     *  @param refusal     null when the plan carries, else the one-sentence per-form reason.
     */
    public record Plan(List<Step> steps, List<ObjectType> nodeTypes, int forward, int backwardFrom,
                       Reason refusal) {

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

        /** The backward {@code veld}s to apply, in order, to walk from the ELEMENT up to node {@code target}
         *  (nodes {@code backwardFrom}..n only). */
        public List<String> chainFromElement(int target) {
            List<String> out = new ArrayList<>();
            for (int i = steps.size(); i > target; i--) out.add(steps.get(i - 1).beanField());
            return out;
        }

        /** The forward {@code veld}s to apply, in order, to walk from the SUBJECT down to node {@code target}
         *  (nodes 0..{@code forward} only). */
        public List<String> chainFromSubject(int target) {
            List<String> out = new ArrayList<>();
            for (int i = 1; i <= target; i++) out.add(steps.get(i - 1).beanField());
            return out;
        }
    }

    private static Plan refuse(Reason reason) { return new Plan(List.of(), List.of(), 0, 0, reason); }

    /**
     * Resolve {@code path} against the model. {@code subject} is the fold subject (node 0) and {@code element} the
     * fold's declared elementType (node n). Every failure is a SENTENCE, never a null: a step that names no
     * feittype/role, a step that does not join what its predecessor reaches, a landing beside the element type, or
     * a hop the bean model derives no field for.
     */
    public static Plan parse(RegelSpraakModel model, ObjectType subject, List<RoleBindingRef> path, ObjectType element) {
        if (model == null || subject == null || element == null)
            return refuse(DroolsCode.SOURCE_PATH_WITHOUT_SUBJECT.reason());
        if (path == null || path.size() < 2)
            return refuse(DroolsCode.SOURCE_PATH_ONE_STEP.reason("steps", path == null ? 0 : path.size()));
        List<Step> steps = new ArrayList<>();
        List<ObjectType> nodes = new ArrayList<>();
        nodes.add(subject);
        for (int i = 0; i < path.size(); i++) {
            RoleBindingRef rb = path.get(i);
            ObjectType van = nodes.get(i);
            boolean last = i == path.size() - 1;
            FactType ft = factTypeByName(model, rb == null ? null : rb.factType());
            if (ft == null)
                return refuse(DroolsCode.SOURCE_PATH_UNKNOWN_FACT.reason("step", i, "factType", rb == null ? null : rb.factType()));
            Role bound = roleByName(ft, rb.role());
            if (bound == null)
                return refuse(DroolsCode.SOURCE_PATH_UNDECLARED_ROLE.reason("step", i, "role", rb.role(), "factType", ft.name));
            Role opposite = oppositeRole(ft, bound);
            if (opposite == null)
                return refuse(DroolsCode.SOURCE_PATH_NO_OPPOSITE.reason("step", i, "role", bound.name(), "factType", ft.name));
            // (d) the walk is CONNECTED: the opposite role must be filled by what the previous step reaches.
            if (!fills(van, opposite))
                return refuse(DroolsCode.SOURCE_PATH_DISCONNECTED_STEP.reason("step", i, "role", opposite.name(), "fillerType", opposite.objectTypeName(), "startType", van.name,
                        "startReason", DroolsCode.SOURCE_PATH_DISCONNECTED_STEP.part(i == 0 ? "fold-subject" : "previous-step")));
            ObjectType filler = model.objectTypeByName(bound.objectTypeName());
            if (filler == null)
                return refuse(DroolsCode.SOURCE_PATH_UNKNOWN_OBJECT.reason("step", i, "role", bound.name(), "objectType", bound.objectTypeName()));
            // (d) the LANDING, two-sided exactly as WF-102 has it: elementType may be a strict SUBTYPE of
            // what the last step's role fills (ch. 2.2's exclusion direction), and the pattern class is then the
            // narrower one — which is what makes the element the population and the relation merely the reach.
            if (last && !compatible(element, filler))
                return refuse(DroolsCode.SOURCE_PATH_OFF_ELEMENT.reason("step", i, "role", bound.name(), "objectType", filler.name, "elementType", element.name));
            ObjectType destination = last ? element : filler;
            // §13.3.9 the storage direction. A to-many bound role keeps its link at the FAR end, named after the
            // opposite role; a to-one bound role keeps it here, named after itself. The question is asked of the
            // ONE shared member derivation (FactModelGenerator), so a path hop can never assume a field the beans
            // do not carry — the failure that used to build a KieBase and die at first fire.
            boolean toMany = bound.many();
            ObjectType carrier = toMany ? destination : van, target = toMany ? van : destination;
            String roleField = toMany ? opposite.name() : bound.name();
            if (!FactModelGenerator.hasReferenceField(model, carrier, roleField, target))
                return refuse(DroolsCode.SOURCE_PATH_NO_JOIN.reason("step", i, "fromType", van.name, "toType", destination.name, "reason", FactModelGenerator.noJoinFieldReason(model, van, bound.name(), destination).msg()));
            steps.add(new Step(toMany, Namen.ident(roleField, false)));
            nodes.add(destination);
        }
        int forward = 0;
        while (forward < steps.size() && !steps.get(forward).backward()) forward++;
        int backwardStart = steps.size();
        while (backwardStart > 0 && steps.get(backwardStart - 1).backward()) backwardStart--;
        return new Plan(List.copyOf(steps), List.copyOf(nodes), forward, backwardStart, null);
    }

    private static FactType factTypeByName(RegelSpraakModel model, String name) {
        if (name == null) return null;
        for (FactType ft : model.factTypes.values())
            if (ft.name.equalsIgnoreCase(name)) return ft;
        return null;
    }

    /** The DECLARED role name, never a plural and never a best-effort one — the wire binds what the author
     *  declared (the same narrowing {@code elementRoleBinding} makes on the emitting side). */
    private static Role roleByName(FactType ft, String name) {
        if (name == null) return null;
        for (Role r : ft.roles)
            if (r.name() != null && r.name().equalsIgnoreCase(name)) return r;
        return null;
    }

    /** The other end of {@code bound}. A §13.3.9.2 wederkerig feittype carries ONE role that is its own
     *  opposite, so the step's two ends are the same role. */
    private static Role oppositeRole(FactType ft, Role bound) {
        if (ft.reciprocal && ft.roles.size() == 1) return bound;
        Role out = null;
        for (Role r : ft.roles) if (r != bound) { if (out != null) return null; out = r; }
        return out;
    }

    /** Does {@code ot} FILL {@code role}? One-sided (⊑ the declared filler): a role holds every instance at or
     *  below its declared type and none above it — the verifier's {@code fills} on this very arm. */
    private static boolean fills(ObjectType ot, Role role) {
        for (ObjectType t : ot.chain())
            if (t.name.equalsIgnoreCase(String.valueOf(role.objectTypeName()))) return true;
        return false;
    }

    /** ⊑-compatible in EITHER direction — WF-102's landing test, deliberately as wide as WF-047's, so the two
     *  spellings of one fold cannot disagree about a subtype. */
    private static boolean compatible(ObjectType a, ObjectType b) {
        for (ObjectType t : a.chain()) if (t == b || t.name.equalsIgnoreCase(b.name)) return true;
        for (ObjectType t : b.chain()) if (t == a || t.name.equalsIgnoreCase(a.name)) return true;
        return false;
    }
}
