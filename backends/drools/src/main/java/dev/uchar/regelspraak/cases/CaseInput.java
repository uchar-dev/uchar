package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import dev.uchar.regelspraak.refusal.DroolsCode;
import dev.uchar.regelspraak.refusal.Reason;

import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.core.Namen;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * The input step of one {@code uchar-case-v0} case on a build of the Drools backend: the beans its instances describe,
 * with their attributes, characteristics and role links set, and what {@link DroolsBackend#insert} needs beside them.
 * The replay reads every case here ({@link CaseRunner}: the gate, the report, the red path and every witness seam), so a
 * consumer that reads a case here puts in the beans the gate plays.
 *
 * <p>{@link #van} reads two members of the case. First {@code asOfDate}, the evaluation date: a scalar input on an
 * attribute on a timeline is placed in the period of the attribute's grid that holds that date, the asOfDate-bucket
 * convention of RUNTIME-SEMANTICS ch. 8.4, so the date is read before any input. Then {@code input.instances}. The
 * parameters and the evaluation date go on the session as globals, by the caller, and the expectations are compared
 * through {@link DroolsBackend#compare}. A consumer whose input is no case file, a decision service with a request of
 * its own among them, writes its validated request as the {@code instances} of a case, with the ids of the request as
 * labels, and reads the case here: there is one reader of an input, whoever supplies it.
 *
 * <p>The steps, each over all instances in the order of the case: a bean of the class {@link DroolsBackend.Build#beanLoader()}
 * loads for the instance's object type; its attributes and characteristics, through the setters of their declared
 * names; its roles; and last the reverse leg of every reciprocal relation. A role link follows RUNTIME-SEMANTICS
 * ch. 5.7a, by which a supplied link establishes the relation in both directions:
 * <ul>
 *   <li>the role the case writes is taken by its name where that name gives a reference field the bean model derives,
 *       and else by the type pair;</li>
 *   <li>a to-one role is written on the instance, and on a one-to-one relation the far field as well, under the sister
 *       role, the role the instance itself fills;</li>
 *   <li>a to-many role is stored in the far end's field, under the sister role, since the bean model keeps a to-many
 *       role as a reference field on each member;</li>
 *   <li>a reciprocal role, one role that is its own opposite, gets its reverse leg in a separate pass over all beans
 *       once every role is written, so the outcome does not depend on the order of the instances.</li>
 * </ul>
 * {@link #inputData()} gives every to-many list in the order of the list the reference interpreter folds over: the members
 * the case wrote under the holder, in the order it wrote them, then the members whose own link the reverse leg mirrors
 * into the list, by label in plain byte order (ch. 5.7a, "member ORDER"), a member in both counting once, where it was
 * written. The order decides which member a fault of a fold names (ch. 5.11), never a value.
 *
 * <p>{@link #van} refuses, before anything goes into a session:
 * <ul>
 *   <li>a form this backend does not carry (an unknown object type, an undeclared attribute, a dimensioned attribute,
 *       a rational value, …): a {@link CaseRunner.Excluded}, in the sentence of the replay's report;</li>
 *   <li>a role link the bean model cannot hold (a label no instance carries, a role that reaches no to-one reference
 *       field, a field a declared member of the same name displaces): a {@link RoleRefusal}, a
 *       {@link CaseRunner.Excluded} as well;</li>
 *   <li>a second, different target on a to-one reference field, one relation instance per to-one role (WF-013), which
 *       the reference interpreter refuses at load: a {@link RoleCollision}, an {@link IllegalArgumentException}, since the
 *       case is wrong;</li>
 *   <li>any other defect of the case (a malformed date or timeline, a scalar on a characteristic on a timeline, …): an
 *       {@link IllegalArgumentException}.</li>
 * </ul>
 * The two refusals of a role link name the link the case wrote ({@link RoleError}), so a consumer can point at the field
 * of its own request that wrote it. While it reads, {@link #van} holds {@link DroolsBackend.Build#beanLoader()} as the
 * thread's context class loader, and gives the previous one back.
 */
public final class CaseInput {

    /** A refused role link of the case input, by the link the case wrote: the instance whose {@code roles} hold it
     *  ({@link #label()}), the role key as the case writes it ({@link #role()}), and the label it names
     *  ({@link #target()}). Where the reverse leg of a reciprocal relation collides, the link is the one whose leg the pass
     *  mirrored. */
    public interface RoleError {
        String label();

        String role();

        String target();
    }

    /** A second, different target on a to-one reference field (WF-013): on the instance's own field, on the far field of
     *  a one-to-one relation, on the field that stores a to-many role, or by the reverse leg of a reciprocal relation. A
     *  defect of the case, which the reference interpreter refuses at load. */
    public static final class RoleCollision extends IllegalArgumentException implements RoleError {
        private final String label, role, target;

        RoleCollision(String sentence, RoleLink link) {
            super(sentence);
            this.label = link.label();
            this.role = link.role();
            this.target = link.target();
        }

        @Override public String label() { return label; }

        @Override public String role() { return role; }

        @Override public String target() { return target; }
    }

    /** A role link this backend does not carry: a label no instance of the case carries, a role that reaches no to-one
     *  reference field, or a field a declared member of the same name displaces. A {@link CaseRunner.Excluded}, which the
     *  report gives as MARKER and the replay as EXCLUDE. */
    public static final class RoleRefusal extends CaseRunner.Excluded implements RoleError {
        private final String label, role, target;

        RoleRefusal(Reason reason, RoleLink link) {
            super(reason);
            this.label = link.label();
            this.role = link.role();
            this.target = link.target();
        }

        @Override public String label() { return label; }

        @Override public String role() { return role; }

        @Override public String target() { return target; }
    }

    /** One link the case wrote: under the {@code roles} of instance {@code label}, the key {@code role}, naming
     *  {@code target}. */
    private record RoleLink(String label, String role, String target) {}

    /** The runner whose value readers the comparison shares: values, dates, timelines, declared member names. */
    private final CaseRunner reader;
    private final RegelSpraakModel model;
    private final ClassLoader cl;
    /** The case's evaluation date, read BEFORE the inputs are set: the asOfDate-bucket convention places a scalar input
     *  on a temporal attribute in the bucket this date falls in ({@link #temporalInput}). */
    private final LocalDate evaluationDate;
    private final Map<String, Object> labelToBean = new LinkedHashMap<>();
    private final Map<String, ObjectType> labelType = new LinkedHashMap<>();
    /** The to-many lists of the case input per holder bean and per reference field of their members, as
     *  {@link #setRoles} writes them: the order {@link #inputData()} gives each list ({@link Input#list}). */
    private final Map<Object, Map<String, ToManyList>> lists = new IdentityHashMap<>();
    /** Every scalar input on a temporal attribute of the case, the asOfDate-bucket convention that
     *  {@link #temporalInput} lays on the bean as one segment: what {@link #inputData()} names as bucket scalars
     *  ({@link Input#bucketScalar}). */
    private final List<Input.Bucket> emmers = new ArrayList<>();
    /** Per bean and per getter of a to-one reference field, the link of the case that wrote the field, which a collision
     *  of the reverse leg names ({@link #mirrorReciprocalRoles}). */
    private final Map<Object, Map<String, RoleLink>> writers = new IdentityHashMap<>();

    private CaseInput(DroolsBackend.Build build, LocalDate evaluationDate) {
        this.reader = CaseRunner.reader(build);
        this.model = build.model();
        this.cl = build.beanLoader();
        this.evaluationDate = evaluationDate;
    }

    /**
     * Reads case {@code c} onto fresh beans of the bean model of {@code build}: its {@code asOfDate} and its
     * {@code input.instances}, as the class comment describes. Everything it refuses, it refuses here.
     *
     * @throws CaseRunner.Excluded      a form this backend does not carry, a {@link RoleRefusal} among them
     * @throws IllegalArgumentException a defect of the case, a {@link RoleCollision} among them, and a case without an
     *                                  array {@code input.instances}
     */
    public static CaseInput van(DroolsBackend.Build build, JsonNode c) {
        Objects.requireNonNull(build, "build");
        Objects.requireNonNull(c, "c");
        LocalDate evaluationDate = CaseRunner.asOfDate(c);
        JsonNode input = c.get("input");
        JsonNode instances = input == null ? null : input.get("instances");
        if (instances == null || !instances.isArray())
            throw new IllegalArgumentException("CaseInput.van: de case draagt geen array input.instances, het deel van"
                    + " een uchar-case-v0-case dat de invoer beschrijft");
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(build.beanLoader());
        try {
            CaseInput inputData = new CaseInput(build, evaluationDate);
            for (JsonNode i : instances) inputData.beanFor(i);
            for (JsonNode i : instances) inputData.setAttributesAndCharacteristics(i);
            for (JsonNode i : instances) inputData.setRoles(i);
            inputData.mirrorReciprocalRoles();
            return inputData;
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    /** The bean of every instance, by label, in the order of the case. */
    public Map<String, Object> beans() {
        return Collections.unmodifiableMap(labelToBean);
    }

    /** The object type of every instance, by label, in the order of the case. */
    public Map<String, ObjectType> types() {
        return Collections.unmodifiableMap(labelType);
    }

    /**
     * The input for {@link DroolsBackend#insert}, a fresh {@link Input} on every call: the beans in the order of the
     * case, every scalar input on a temporal attribute as a bucket scalar, and the order of every to-many list. The bean
     * shows such a scalar as the one segment of its bucket, which a full timeline of one segment would be as well, while
     * the analysis reads the two conventions of RUNTIME-SEMANTICS ch. 8.4 apart: the bucket scalar serves the subject's
     * own total, the full timeline every other read, and a form that needs the one and finds the other faults. The bean
     * model keeps a to-many role as a reference field on each member and no order, while the analysis names the first
     * member in fold order whose filter faults (RUNTIME-SEMANTICS ch. 5.11). The order is that of the list the reference
     * interpreter folds over: the written members first, in the order the case wrote them, then the members whose own
     * link the loader mirrors into the list, in ascending label order (ch. 5.7a, "member ORDER"), a member in both
     * counting once, where it was written.
     */
    public Input inputData() {
        Input inputData = new Input().beans(labelToBean.values());
        for (Input.Bucket e : emmers) inputData.bucketScalar(e.bean(), e.attribute());
        for (Map.Entry<Object, Map<String, ToManyList>> perHouder : lists.entrySet())
            for (ToManyList l : perHouder.getValue().values()) {
                List<Object> members = new ArrayList<>(l.written);
                for (Object member : l.mirrored.values())
                    if (members.stream().noneMatch(x -> x == member)) members.add(member);
                inputData.list(perHouder.getKey(), l.role, members);
            }
        return inputData;
    }

    private Object beanFor(JsonNode i) {
        String label = i.get("label").asText();
        Object bean = labelToBean.get(label);
        if (bean != null) return bean;
        String otName = i.get("objectType").asText();
        ObjectType ot = model.objectTypeByName(otName);
        if (ot == null) throw new CaseRunner.Excluded(DroolsCode.CASE_UNKNOWN_OBJECT_TYPE.reason("objectType", otName));
        try {
            bean = Class.forName(FactModelGenerator.PACKAGE + "." + Namen.ident(ot.name, true), true, cl)
                    .getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("kon bean voor label '" + label + "' niet maken", e);
        }
        labelToBean.put(label, bean);
        labelType.put(label, ot);
        return bean;
    }

    private void setAttributesAndCharacteristics(JsonNode i) {
        Object bean = labelToBean.get(i.get("label").asText());
        ObjectType ot = model.objectTypeByName(i.get("objectType").asText());
        JsonNode attrs = i.get("attributes");
        if (attrs != null) for (Map.Entry<String, JsonNode> e : attrs.properties()) {
            Datatype dt = ot.attribute(e.getKey());
            if (dt == null) throw new CaseRunner.Excluded(DroolsCode.CASE_UNDECLARED_ATTRIBUTE.reason("attribute", e.getKey(), "objectType", ot.name));
            reader.ownField(ot, e.getKey(), false, "sets");
            if (reader.dimensioned(ot, e.getKey()))
                throw new CaseRunner.Excluded(DroolsCode.DIMENSIONED_ATTRIBUTE_INPUT.reason("attribute", e.getKey()));
            if (reader.temporal(ot, e.getKey())) {
                // the bean member was generated from the DECLARATION, so a case key spelled otherwise must be
                // brought back to it or it names a setter the bean does not have (ref reads it as the same name)
                CaseRunner.call(bean, Namen.setter(Namen.declaredName(ot.granularityMembers().keySet(), e.getKey())),
                        List.class, temporalInput(e.getValue(), e.getKey(), i.get("label").asText() + "." + e.getKey(),
                                reader.grid(ot, e.getKey())));
                if (!e.getValue().isNull() && !e.getValue().has("timeline"))
                    emmers.add(new Input.Bucket(bean, e.getKey()));
                continue;
            }
            CaseRunner.call(bean, Namen.setter(CaseRunner.attributeName(ot, e.getKey())), CaseRunner.argType(dt),
                    reader.valueToArg(e.getValue(), e.getKey()));
        }
        JsonNode characteristics = i.get("characteristics");
        if (characteristics != null) for (JsonNode k : characteristics) {
            // A CASE defect, not an uncarried form (maintainer decision 2026-09-09) — the sister arm below has
            // said so about the mirror-image mistake since it was written, and this arm is the same seam from
            // the other side. A TEMPORAL kenmerk's bean member is a List<Segment>, so a scalar-true assertion
            // reaches no reader here; in the interpreter and the ASP backend it reached none either, and BOTH
            // answered 0 where the golden pins 30 rather than saying anything (measured 2026-09-09 on
            // jutterswet-tegenwacht and anchor/jutterswet-dubbelwacht). All three readers refuse it at load
            // since that decision. It stayed an Excluded until then, which filed a broken case under "a form
            // the v0 backend does not carry" — a counted, reasoned refusal in the gate report for a case that
            // is simply wrong, and the one classification that could keep it out of sight.
            if (Namen.containsName(ot.characteristicGranularityMembers(), k.asText()))
                throw new IllegalArgumentException("scalar characteristic input '" + k.asText() + "' on the "
                        + "time-dependent kenmerk of " + ot.name + ": deliver it as a characteristicTimelines "
                        + "entry — the static characteristics list asserts a scalar truth no temporal reader "
                        + "consults");
            reader.ownField(ot, k.asText(), true, "sets");
            CaseRunner.call(bean, Namen.setter(CaseRunner.characteristicName(ot, k.asText())), boolean.class, Boolean.TRUE);
        }
        JsonNode ctls = i.get("characteristicTimelines");
        if (ctls != null) for (Map.Entry<String, JsonNode> e : ctls.properties()) {
            // a CASE defect, not an uncarried form — ref refuses this shape at load (case.ml: "a
            // characteristic timeline on a non-temporal characteristic"), so it is hard here too; an
            // Excluded would file a broken corpus case under "not carried", silently green.
            if (!Namen.containsName(ot.characteristicGranularityMembers(), e.getKey()))
                throw new IllegalArgumentException("kenmerktijdlijn op niet-temporeel kenmerk '" + e.getKey() + "'");
            reader.ownField(ot, e.getKey(), true, "sets");
            CaseRunner.call(bean, Namen.setter(Namen.declaredName(ot.characteristicGranularityMembers().keySet(), e.getKey())),
                    List.class, characteristicTimeline(e.getValue(), e.getKey()));
        }
    }

    private void setRoles(JsonNode i) {
        Object bean = labelToBean.get(i.get("label").asText());
        ObjectType tI = model.objectTypeByName(i.get("objectType").asText());
        JsonNode roles = i.get("roles");
        if (roles == null) return;
        for (Map.Entry<String, JsonNode> e : roles.properties()) {
            for (JsonNode labelNode : e.getValue()) {
                RoleLink link = new RoleLink(i.get("label").asText(), e.getKey(), labelNode.asText());
                Object target = labelToBean.get(labelNode.asText());
                if (target == null)
                    throw new RoleRefusal(DroolsCode.ROLE_UNKNOWN_LABEL.reason("role", e.getKey(), "label", labelNode.asText()), link);
                ObjectType tL = beanObjectType(target);
                // §13.4.1 the WRITTEN role IS the navigation — and wherever the type pair carries more than
                // one role it is the ONLY route: the by-pair search below counts every candidate and derives
                // null by construction (RegelSpraakModel.connectingRole; ref BLOCKS the same derivation in
                // connecting_pair). That is the SELF-TYPED pair's two sibling roles and, since the repeated-pair
                // card, the ordinary cross-type pair related by several feittypen. Resolved through the very
                // authority FactModelGenerator.members derives the fields from and ACCEPTED ONLY when it names one
                // of them (validRoleField), so a hit can only write a field the bean carries — that membership
                // test is what keeps this from being a general by-name override. Where the by-pair search already
                // succeeds the outcome is unchanged: a by-name hit is always one of the by-pair candidates, so a
                // unique pair resolves to the same role either way.
                String viaName = model.connectingRoleViaName(tI, tL, e.getKey());
                boolean writtenField = viaName != null && validRoleField(tI, viaName, tL);
                // the field on THIS bean (to-one from here): the written role, else the type pair
                String roleIL = writtenField ? viaName : model.connectingRole(tI, tL);
                // The SISTER role — the one THIS instance fills — whose reference field lives on the TARGET
                // bean, and which is where a TO-MANY written role stores its links. Resolved BY NAME first,
                // exactly as roleIL is: the written role name names one feittype and its OTHER role is the
                // back-reference (connectingRoleViaElementName). That is the only route wherever the by-pair
                // search counts more than one candidate — the self-typed pair's two siblings (the shape that
                // made W1{opvolgers:[…]} a per-form refusal although Wachter.voorligger exists) and the
                // repeated cross-type pair, where H1{liggers:[…]} must land on Schuit.thuishaven and not on
                // one of the three other Haven fields that bean now carries. RUNTIME-SEMANTICS ch. 5.7a makes
                // this write obligatory, not optional: the case names ONE leg of a relation that has two, and
                // the reader arriving from the other end must reach the same relation instance.
                //
                // Accepted only when the name lands on a field FactModelGenerator really derived
                // (validRoleField, the same membership test roleIL uses), so a written name that resolves to a
                // role WITHOUT a field keeps the honest refusal below instead of reaching a setter no bean
                // carries. Where the by-pair search already succeeds, a by-name hit is one of its candidates,
                // so a unique pair resolves to the same role either way.
                String sister = model.connectingRoleViaElementName(tI, e.getKey());
                boolean sisterField = sister != null && validRoleField(tL, sister, tI);
                String roleLI = sisterField ? sister : model.connectingRole(tL, tI);
                if (roleIL != null && !model.toManyRole(tI, tL, roleIL)) {
                    nameSharingGate(tI, roleIL, tL, link);
                    setToOne(bean, roleIL, target, link);
                    list(target, roleIL).mirrored.putIfAbsent(i.get("label").asText(), bean);
                    // RUNTIME-SEMANTICS ch. 5.7a, the 1:1 arm: a supplied link ESTABLISHES the relation and
                    // BOTH roles read it. With the sister role ALSO to-one the relation owns TWO bean fields —
                    // one per direction — so the forward write above leaves the far side reading a spuriously
                    // empty relation unless the second field is written too. That second write is the whole of
                    // the difference between a 1:1 and a 1:N shape here: at 1:N there is exactly ONE field and
                    // it already serves both directions (the else-branch below writes it), so nothing is left
                    // to mirror. ref's Case.materialise_relations makes the same write on the same role.
                    //
                    // roleLI == roleIL is the WEDERKERIG shape (§13.3.9.2: one role that is its own opposite),
                    // whose mirror is mirrorReciprocalRoles's separate pass — untouched here, so its
                    // order-independence argument and its own conflict message keep holding.
                    if (roleLI != null && !roleLI.equalsIgnoreCase(roleIL) && !model.toManyRole(tL, tI, roleLI)) {
                        // The far field must really exist: a declared member of the same name displaces it
                        // (members()'s dedup), and then this backend cannot hold the far direction at all. Under
                        // ch. 5.7a that is a per-form refusal with its reason, never a silently one-directional
                        // relation — the same standard the forward write is held to one line up.
                        nameSharingGate(tL, roleLI, tI, link);
                        setToOne(target, roleLI, bean, link);
                    }
                } else {
                    // the field lives on the target (this is the one side)
                    if (roleLI == null || model.toManyRole(tL, tI, roleLI))
                        // NOT UNIQUE and UNIQUE BUT TO-MANY are different failures and are reported apart (lens 2,
                        // 2026-08-30): the erflijn shape resolves `erfvolger` by name PERFECTLY, and what stops it
                        // is its CARDINALITY — a to-many role's storage is the far end's field — where the old
                        // single sentence blamed a non-unique derivation that had in fact succeeded.
                        throw new RoleRefusal(DroolsCode.CASE_ROLE_NO_FIELD.reason("role", e.getKey(), "fromType", tI.name, "toType", tL.name, "viaReason", viaName == null ? DroolsCode.CASE_ROLE_NO_FIELD.part("via-none")
                                : DroolsCode.CASE_ROLE_NO_FIELD.part(model.toManyRole(tI, tL, viaName) ? "via-to-many" : "via-to-one", "role", viaName),
                                "directionReason", DroolsCode.CASE_ROLE_NO_FIELD.part(roleIL != null ? "direction-near" : roleLI != null ? "direction-far" : "direction-none",
                                        "role", roleIL != null ? roleIL : roleLI)), link);
                    nameSharingGate(tL, roleLI, tI, link);
                    setToOne(target, roleLI, bean, link);
                    ToManyList l = list(bean, roleLI);
                    if (l.written.stream().noneMatch(x -> x == target)) l.written.add(target);
                }
            }
        }
    }

    /** One to-many list of the case input under the reference field of the holder's role {@code role}: the members the
     *  case wrote under the holder, in the order it wrote them, and the members whose own link to the holder the reverse
     *  leg mirrors into it (RUNTIME-SEMANTICS ch. 5.7a), by label in ref's order, plain byte order. */
    private static final class ToManyList {
        final String role;
        final List<Object> written = new ArrayList<>();
        final TreeMap<String, Object> mirrored = new TreeMap<>(
                Comparator.comparing((String l) -> l.getBytes(StandardCharsets.UTF_8), Arrays::compareUnsigned));

        ToManyList(String role) {
            this.role = role;
        }
    }

    private ToManyList list(Object houder, String role) {
        return lists.computeIfAbsent(houder, k -> new LinkedHashMap<>())
                .computeIfAbsent(Namen.ident(role, false), k -> new ToManyList(role));
    }

    /** Whether {@code role} is one of the reference fields {@link FactModelGenerator#members} DERIVES on
     *  {@code carrier} toward {@code target} — the membership test that lets the WRITTEN role name override the
     *  by-pair search without ever reaching a setter no bean has. It asks the generator's own two derivations:
     *  {@code selfPairRoles} for a self-typed pair (§13.3.9.2 and the ordinary two-role self-pair) and
     *  {@code connectingRoles} for a cross-type one. Deliberately a NAME test, not
     *  {@link FactModelGenerator#hasReferenceField}: a role whose name a declared member displaced is a
     *  DIFFERENT failure with its own sentence ({@link #nameSharingGate}), and collapsing the two here would
     *  send the name-sharing shape back through the by-pair route to be blamed for an ambiguity it does not
     *  have. */
    private boolean validRoleField(ObjectType carrier, String role, ObjectType target) {
        return carrier.name.equalsIgnoreCase(target.name)
                ? FactModelGenerator.selfPairRoles(model, carrier).contains(role)
                : model.connectingRoles(carrier, target).contains(role);
    }

    /** The §13.3.9 NAME-SHARING gate on a resolved to-one role write: {@code carrier}'s bean must carry the
     *  reference field for {@code role} TYPED (a field of {@code target}'s class), not merely a property of that
     *  name. Both roles here come from the very authorities {@link FactModelGenerator#members} derives the fields
     *  from ({@code selfPairRoles} / {@code connectingRoles} / {@code connectingRole}), so the ONE thing that
     *  can still be missing is the
     *  reference itself, displaced by a declared member of the same name (members()'s dedup: attribute >
     *  characteristic > reference). Without this gate the write below reaches {@code set<X>(BigDecimal)} with a
     *  fact — "kon setter setOpvolger(Wachter) niet aanroepen", an unclassified ERROR the gate refuses to excuse
     *  — where the honest outcome is a loud, reason-carrying exclusion (lens 2, 2026-08-30). The §13.3.9.2
     *  wederkerig twin of this limit is refused in {@link DroolsBackend} for the whole model. */
    private void nameSharingGate(ObjectType carrier, String role, ObjectType target, RoleLink link) {
        if (FactModelGenerator.hasReferenceField(model, carrier, role, target)) return;
        // The cause is read off what holds the property, never assumed. Until 2026-09-28 every cause other than a
        // declared member was reported as a second role, and the commonest one was no second role at all: a target
        // of a SUBTYPE of the role's filler, which the field of the filler's class takes (IR-SPEC ch. 2.6) and which
        // the type gate then refused by equality. Two same-named roles toward different types
        // (corpus/red/ambiguous-case-role-key) are the cause that sentence was written for, and it now names them.
        FactModelGenerator.Origin houder = FactModelGenerator.fieldOrigin(model, carrier, role);
        String field = Namen.ident(carrier.name, true) + "." + Namen.ident(role, false);
        String cause;
        if (carrier.hasMember(role)) cause = "which shares its name with a declared member";
        else if (houder == null) cause = "for which the bean model derives no field";
        else if (houder.category() != FactModelGenerator.Origin.Kind.ROL)
            cause = "whose bean property " + field + " belongs to " + described(houder);
        else if (!houder.equals(FactModelGenerator.roleOrigin(model, carrier, target, role)))
            cause = "whose field " + field + " another role already holds, " + described(houder);
        else cause = "whose field " + field + " cannot hold a " + target.name;
        throw new RoleRefusal(DroolsCode.NAME_SHARING_ONE_ROLE.reason("role", link.role(), "targetRole", role, "objectType", carrier.name, "cause", cause), link);
    }

    /** A declaration in the words of this reader's refusals. */
    private static String described(FactModelGenerator.Origin h) {
        return switch (h.category()) {
            case ATTRIBUUT -> "the attribute '" + h.name() + "' of " + h.source();
            case KENMERK -> "the characteristic '" + h.name() + "' of " + h.source();
            case ROL -> "the role '" + h.name() + "'" + (h.source() == null ? "" : " of fact type '" + h.source() + "'");
        };
    }

    /** One to-one role write for {@code link}, WF-013-guarded: a second, DIFFERENT filler on the same to-one field is a
     *  case shape ref refuses at load, so last-wins here would play a broken case green — the mirror pass's own
     *  argument, applied to the forward leg (lens 2, 2026-08-30). The same filler twice stays idempotent. The field
     *  remembers the first link that wrote it, which a collision of the reverse leg names. */
    private void setToOne(Object carrier, String role, Object target, RoleLink link) {
        Object huidig = CaseRunner.invokeGetter(carrier, Namen.getter(role));
        if (huidig != null && huidig != target)
            throw new RoleCollision("to-one rol '" + role + "' krijgt een tweede, ander doel — "
                    + "ref weigert die case bij het laden (WF-013)", link);
        CaseRunner.call(carrier, Namen.setter(role), fieldClass(carrier, role), target);
        writers.computeIfAbsent(carrier, k -> new LinkedHashMap<>()).putIfAbsent(Namen.getter(role), link);
    }

    /** The class of the reference field {@code role} on {@code carrier}'s bean, read off its getter: the class of the
     *  role's declared filler. The setter is looked up by that class and not by the target's own, because a target
     *  of a subtype of the filler (IR-SPEC ch. 2.6) has a class of its own that no setter names. */
    private static Class<?> fieldClass(Object carrier, String role) {
        try {
            return carrier.getClass().getMethod(Namen.getter(role)).getReturnType();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("geen getter " + Namen.getter(role) + " op "
                    + carrier.getClass().getSimpleName(), e);
        }
    }

    /** §13.3.9.2 mirrored: ONE reciprocal entry holds in BOTH directions, so every self-pair leg the case wrote
     *  gets its reverse materialised — exactly what {@code Case.materialise_relations} does in ref, and what
     *  RUNTIME-SEMANTICS ch. 4.1 requires ("the pinned runtime writes both to-one legs so navigation never reads
     *  a spuriously empty side"). Both goldens pin it: a partner that declares no roles of its own still reads its
     *  side, and the reverse read {@code Jutter( maat == $subject )} an aggregation lowers to needs the leg
     *  present. Run as a SEPARATE PASS over the finished links, like ref's, so the outcome cannot depend on the
     *  order instances appear in: a conflict must be seen wherever the colliding leg was written. Idempotent
     *  (a case supplying both legs means one relation), and a CONFLICTING leg on the to-one role is hard — ref
     *  refuses that case at load (WF-013), so overwriting it here would file a broken case under a green replay.
     *
     *  <p>Scoped to the WEDERKERIG shape, and since 2026-09-10 that scope is a DIVISION OF LABOUR rather than
     *  the law it used to state. RUNTIME-SEMANTICS ch. 5.7a (maintainer decision) makes EVERY supplied link
     *  establish its relation in both directions, and ref's {@code Case.materialise_relations} mirrors on the
     *  SISTER role accordingly. Three shapes, three answers, all landed:
     *  <ul>
     *    <li><b>1:N</b> — the relation has exactly ONE reference field and it necessarily serves both
     *        directions, so there is no second write to make. {@link #setRoles} lands the case's write on that
     *        field through the sister role's NAME, and the three spellings of one relation give one fact base
     *        ({@code SisterRoleInputWitnessTest}, over the {@code jutterswet-ketenband} golden).</li>
     *    <li><b>1:1</b> — each direction owns its own field, so ch. 5.7a needs a SECOND write on the target
     *        bean. {@link #setRoles} makes it, right after the forward write, on the sister role and under the
     *        same name-sharing gate. That closed the last ref ≢ Drools flank of the decision: a case writing
     *        {@code W1{opvolger:[W2]}} over a to-one/to-one self-pair now reads W1 as W2's voorganger here, as
     *        it does in ref ({@code OpslagDirectionWitnessTest}, converted at that decision).</li>
     *    <li><b>wederkerig</b> (§13.3.9.2, one role that is its own opposite) — THIS pass, and deliberately
     *        still a separate one: its order-independence argument above is the reason, and the 1:1 arm skips
     *        the shape (it fires only when the sister role is a DIFFERENT role) so the two never collide.</li>
     *  </ul>
     *  A collision names the link whose leg the pass mirrored, the one that wrote the bean's own field: a leg the pass
     *  wrote itself never collides, since it writes only an empty field and its partner then reads back. */
    private void mirrorReciprocalRoles() {
        for (Object bean : labelToBean.values()) {
            // the reciprocal role of an instance of a SUBTYPE of its filler is its ancestor's (IR-SPEC ch. 2.6):
            // asked of the bean's own type alone, such a leg went unmirrored and the partner read an empty relation
            String role = null;
            for (ObjectType t : beanObjectType(bean).chain())
                if ((role = FactModelGenerator.selfPairRole(model, t)) != null) break;
            if (role == null) continue;
            Object partner = CaseRunner.invokeGetter(bean, Namen.getter(role));
            if (partner == null) continue;
            Object readBack = CaseRunner.invokeGetter(partner, Namen.getter(role));
            if (readBack == bean) continue;
            if (readBack != null)
                throw new RoleCollision("wederkerige rol '" + role + "': het doel is al verbonden met "
                        + "een ander — de omgekeerde poot botst (WF-013 to-one)", writtenBy(bean, role));
            CaseRunner.call(partner, Namen.setter(role), fieldClass(partner, role), bean);
        }
    }

    /** The link of the case that wrote the to-one field {@code role} of {@code carrier}. */
    private RoleLink writtenBy(Object carrier, String role) {
        RoleLink link = writers.getOrDefault(carrier, Map.of()).get(Namen.getter(role));
        if (link == null)
            throw new IllegalStateException("het veld '" + role + "' van " + carrier.getClass().getSimpleName()
                    + " draagt een doel dat geen rol van de case schreef");
        return link;
    }

    private ObjectType beanObjectType(Object bean) {
        for (ObjectType ot : model.objectTypes.values())
            if (Namen.ident(ot.name, true).equals(bean.getClass().getSimpleName())) return ot;
        throw new IllegalStateException("geen objecttype voor bean " + bean.getClass());
    }

    // ---------------- §13.3.6 temporal input (the List<Segment> bean members) ----------------

    /** The {@code List<Segment>} a case value denotes for a temporal attribute at {@code gran} — the two input
     *  forms {@code ref} carries (ref/lib/case.ml), mirrored so both executors read the SAME timeline:
     *  <ul>
     *    <li>a {@code {"timeline":[…]}} value: the written segments, validated exactly as ref validates them;</li>
     *    <li>a SCALAR value: the asOfDate-bucket convention recorded in {@code corpus/golden/README.md} — the
     *        scalar IS the value of the bucket the evaluation date falls in. ref keeps that scalar aside and
     *        folds precisely this one-bucket timeline inside the ch. 3.6 collapse ({@code Eval}'s temporal
     *        total: value × whole units of its own grid); the bean model has no scalar slot beside the
     *        {@code List<Segment>} field, so the bucket is materialised here and {@code RegelSpraakRuntime.rsTijdTotaal}
     *        folds the identical segment to the identical scalar.</li>
     *  </ul>
     *  Deliberate consequence of that materialisation: the bean now carries a shape ref REFUSES to serve —
     *  ref has no point-in-time read at all (ch. 3.3: a temporal attribute in a scalar slot is refused, the
     *  collapses are the only bridges), and its no-filter ch. 3.6 total even faults on a case-supplied FULL
     *  timeline ("the asOfDate-bucket convention carries the total", eval.ml). The divergence surface is thus
     *  not a wrong value but a read Drools would answer where ref faults; such a case can never become a
     *  golden, because gate 5 replays every case through ref first — the executors agree wherever the corpus
     *  looks, by construction. The caller records each scalar as a bucket scalar, which
     *  {@link #inputData()} names to {@link DroolsBackend#insert}, and the analysis reads the two conventions apart and
     *  faults where a form needs the one and finds the other (RUNTIME-SEMANTICS ch. 8.4). */
    private List<Object> temporalInput(JsonNode w, String attr, String site, CaseRunner.Grid gran) {
        if (w == null || w.isNull()) return null;   // an absent value is an absent timeline (leeg, ch. 3.1)
        if (w.has("timeline")) return reader.timelineSegments(w.get("timeline"), attr, site, gran, false);
        if (evaluationDate == null)
            throw new CaseRunner.Excluded(DroolsCode.SCALAR_TEMPORAL_NO_DATE.reason("attribute", attr));
        LocalDate[] bucket = CaseRunner.bucket(gran, evaluationDate);
        return List.of(reader.segment(bucket[0], bucket[1], reader.segmentValue(w, attr)));
    }

    /** The TRUE-period {@code List<Segment>} a {@code characteristicTimelines} entry denotes. True periods are
     *  DAY-precise regardless of the declared granularity (ref: the granularity governs the kenmerk's bucketing,
     *  never its period grid — the ch. 3.7 pro-rata's whole point), so no grid check applies; ref sorts the
     *  periods and rejects overlap, and this does the same. The segments carry a null waarde: the runtime builds
     *  its own true-period timelines that way (rsTijdOfUnie) and every consumer reads van/tot only (rsTijdKnip). */
    private List<Object> characteristicTimeline(JsonNode periodes, String characteristic) {
        if (periodes == null || !periodes.isArray())
            throw new IllegalArgumentException("misvormde kenmerktijdlijn op '" + characteristic + "'");
        List<LocalDate[]> sorted = new ArrayList<>();
        for (JsonNode pj : periodes) {
            LocalDate van = CaseRunner.boundDate(pj, "from", characteristic), to = CaseRunner.boundDate(pj, "to", characteristic);
            if (!van.isBefore(to))
                throw new IllegalArgumentException("periode 'to' op '" + characteristic + "' ligt niet na 'from'");
            sorted.add(new LocalDate[]{ van, to });
        }
        sorted.sort((a, b) -> a[0].equals(b[0]) ? a[1].compareTo(b[1]) : a[0].compareTo(b[0]));
        List<Object> out = new ArrayList<>();
        LocalDate previousTo = null;
        for (LocalDate[] p : sorted) {
            if (previousTo != null && p[0].isBefore(previousTo))
                throw new IllegalArgumentException("perioden op '" + characteristic + "' overlappen");
            previousTo = p[1];
            out.add(reader.segment(p[0], p[1], null));
        }
        return out;
    }
}
