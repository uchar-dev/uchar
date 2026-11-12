package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import dev.uchar.regelspraak.refusal.DroolsCode;
import dev.uchar.regelspraak.refusal.Reason;

import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.core.Namen;
import dev.uchar.regelspraak.transpiler.tms.Period;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler;

import org.kie.api.definition.KiePackage;
import org.kie.api.definition.rule.Global;
import org.kie.api.runtime.KieSession;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * The input step of one {@code uchar-case-v0} case on a build of the Drools backend: the beans its instances describe,
 * with their attributes, characteristics and role links set, and what {@link DroolsBackend#insert} needs beside them.
 * The replay reads every case here ({@link CaseRunner}: the gate, the report, the red path and every witness seam), so a
 * consumer that reads a case here puts in the beans the gate plays.
 *
 * <p>{@link #of} reads two members of the case. First {@code asOfDate}, the evaluation date: a scalar input on an
 * attribute on a timeline is placed in the period of the attribute's grid that holds that date, the asOfDate-bucket
 * convention of RUNTIME-SEMANTICS ch. 8.4, so the date is read before any input. Then {@code input.instances}. The
 * parameters and the evaluation date go on the session as globals, by the caller, and the expectations are compared
 * through {@link DroolsBackend#compare}. A consumer whose input is no case file, a decision service with a request of
 * its own among them, writes its validated request as the {@code instances} of a case, with the ids of the request as
 * labels, and reads the case here: there is one reader of an input, whoever supplies it.
 *
 * <p>The steps, each over all instances in the order of the case: a bean of the class {@link DroolsBackend.Build#beanLoader()}
 * loads for the instance's object type; its attributes and characteristics, through the setters of their declared
 * names; its roles; and last the reverse leg of every reciprocal relation. A name the case writes, an object type, an
 * attribute, a characteristic or a role key, is the declaration that it is under the name equality of the reference
 * interpreter's case reader ({@link CaseRunner#attributeName}: runs of spaces and tabs as one space, A–Z as a–z, every
 * other character as written). A role link follows RUNTIME-SEMANTICS
 * ch. 5.7a, by which a supplied link establishes the relation in both directions:
 * <ul>
 *   <li>the role the case writes is taken by its name where that name gives a reference field the bean model derives,
 *       and else by the type pair;</li>
 *   <li>a to-one role is written on the instance, and on a one-to-one relation the far field as well, under the sister
 *       role, the role the instance itself fills;</li>
 *   <li>a to-many role is stored in the far end's field, under the sister role, since the bean model keeps a to-many
 *       role as a reference field on each member;</li>
 *   <li>a role of a relation both of whose roles are to-many, which no bean holds, is a link of its own
 *       ({@link Input#link}), one link however often and from whichever side the case writes it;</li>
 *   <li>a reciprocal role, one role that is its own opposite, gets its reverse leg in a separate pass over all beans
 *       once every role is written, so the outcome does not depend on the order of the instances.</li>
 * </ul>
 * {@link #inputData()} gives every to-many list in the order of the list the reference interpreter folds over: the members
 * the case wrote under the holder, in the order it wrote them, then the members whose own link the reverse leg mirrors
 * into the list, by label in plain byte order (ch. 5.7a, "member ORDER"), a member in both counting once, where it was
 * written. The order decides which member a fault of a fold names (ch. 5.11), never a value.
 *
 * <p>{@link #of} refuses, before anything goes into a session:
 * <ul>
 *   <li>a form this backend does not carry (a rational value, …): a {@link CaseRunner.Excluded}, in the sentence of the
 *       replay's report;</li>
 *   <li>a role link the bean model cannot hold (a role that reaches no to-one reference field, a field a declared
 *       member of the same name displaces): a {@link RoleRefusal}, a {@link CaseRunner.Excluded} as well;</li>
 *   <li>a role link that names a label no instance carries (EF-002): a {@link LinkTargetFault}, an
 *       {@link IllegalArgumentException};</li>
 *   <li>a second, different target on a to-one reference field, one relation instance per to-one role (WF-013), which
 *       the reference interpreter refuses at load: a {@link RoleCollision}, an {@link IllegalArgumentException}, since the
 *       case is wrong;</li>
 *   <li>a cell of a dimensioned attribute whose labels name no cell of it (EF-034) or that the input names twice
 *       (EF-035), RUNTIME-SEMANTICS ch. 5.8c: a {@link CellFault}, an {@link IllegalArgumentException};</li>
 *   <li>any other defect of the case (a label two instances carry, a name that is no declaration, a malformed date or
 *       timeline, a scalar on a characteristic on a timeline, a scalar on a dimensioned attribute, a {@code cells} input
 *       on an attribute without dimensions, …): an {@link IllegalArgumentException}, as the reference interpreter
 *       refuses it at load.</li>
 * </ul>
 * The three refusals of a role link name the link the case wrote ({@link RoleError}), so a consumer can point at the field
 * of its own request that wrote it. While it reads, {@link #of} holds {@link DroolsBackend.Build#beanLoader()} as the
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

    /** A role link this backend does not carry: a role that reaches no to-one reference field, or a field a declared
     *  member of the same name displaces. A {@link CaseRunner.Excluded}, which the report gives as MARKER and the replay
     *  as EXCLUDE. */
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

    /** A role link of the case input that names a label no instance of the case carries (EF-002, RUNTIME-SEMANTICS
     *  ch. 5.7b): a fault kind of the case input, with the role key as the case writes it, the label of the instance whose
     *  {@code roles} hold the link, and the label it names. */
    public static final class LinkTargetFault extends IllegalArgumentException implements RoleError {
        private static final long serialVersionUID = 1L;

        private final String label, role, target;

        LinkTargetFault(RoleLink link) {
            super(dev.uchar.regelspraak.message.Msg.drools("EF-002", "role", link.role(), "label", link.label(), "target",
                    link.target()).text());
            this.label = link.label();
            this.role = link.role();
            this.target = link.target();
        }

        @Override public String label() { return label; }

        @Override public String role() { return role; }

        @Override public String target() { return target; }

        public String code() {
            return "EF-002";
        }

        /** The parameters of the kind (ch. 5.7b): the role key, the label of the instance and the label it names. */
        public Map<String, String> params() {
            Map<String, String> out = new LinkedHashMap<>();
            out.put("role", role);
            out.put("label", label);
            out.put("target", target);
            return out;
        }
    }

    /** One link the case wrote: under the {@code roles} of instance {@code label}, the key {@code role}, naming
     *  {@code target}. */
    private record RoleLink(String label, String role, String target) {}

    /** The runner whose value readers the comparison shares: values, dates, timelines, declared member names. */
    private final CaseRunner reader;
    private final DroolsBackend.Build build;
    private final RegelSpraakModel model;
    private final ClassLoader cl;
    /** The case's evaluation date, read BEFORE the inputs are set: the asOfDate-bucket convention places a scalar input
     *  on a temporal attribute in the bucket this date falls in ({@link #bucketInput}). */
    private final LocalDate evaluationDate;
    private final Map<String, Object> labelToBean = new LinkedHashMap<>();
    private final Map<String, ObjectType> labelType = new LinkedHashMap<>();
    /** The to-many lists of the case input per holder bean and per reference field of their members, as
     *  {@link #setRoles} writes them: the order {@link #inputData()} gives each list ({@link Input#list}). */
    private final Map<Object, Map<String, ToManyList>> lists = new IdentityHashMap<>();
    /** The links of the case over a relation both of whose roles are to-many, which no bean holds, each once however
     *  often and from whichever side the case writes it: what {@link #inputData()} names as links ({@link Input#link}). */
    private final List<Input.Link> links = new ArrayList<>();
    /** Every scalar input on a temporal attribute of the case, the asOfDate-bucket convention that
     *  {@link #bucketInput} lays on the bean as one segment: what {@link #inputData()} names as bucket scalars
     *  ({@link Input#bucketScalar}). */
    private final List<Input.Bucket> emmers = new ArrayList<>();
    /** Per bean and per getter of a to-one reference field, the link of the case that wrote the field, which a collision
     *  of the reverse leg names ({@link #mirrorReciprocalRoles}). */
    private final Map<Object, Map<String, RoleLink>> writers = new IdentityHashMap<>();
    /** The parameters the case binds, each under the name of the global a build declares for its declaration
     *  ({@link Namen#ident} of the declared name), with the value the session reads: what {@link #setGlobals} sets. */
    private final Map<String, Object> parameters = new LinkedHashMap<>();

    private CaseInput(DroolsBackend.Build build, LocalDate evaluationDate) {
        this.reader = CaseRunner.reader(build);
        this.build = build;
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
    public static CaseInput of(DroolsBackend.Build build, JsonNode c) {
        Objects.requireNonNull(build, "build");
        Objects.requireNonNull(c, "c");
        LocalDate evaluationDate = CaseRunner.asOfDate(c);
        JsonNode input = c.get("input");
        if (input != null && !input.isObject())
            throw new IllegalArgumentException("input: expected an object, got " + input);
        JsonNode instances = input == null ? null : input.get("instances");
        if (instances == null || !instances.isArray())
            throw new IllegalArgumentException("CaseInput.of: the case carries no array input.instances, the part of"
                    + " a uchar-case-v0 case that describes the input");
        for (int n = 0; n < instances.size(); n++) shape(instances.get(n), "input.instances[" + n + "]");
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(build.beanLoader());
        try {
            CaseInput inputData = new CaseInput(build, evaluationDate);
            for (JsonNode i : instances) inputData.beanFor(i);
            for (JsonNode i : instances) inputData.setAttributesAndCharacteristics(i);
            for (JsonNode i : instances) inputData.setRoles(i);
            inputData.mirrorReciprocalRoles();
            inputData.refuseLinkCycles(build, instances);
            inputData.readParameters(input.get("parameters"));
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
     * case, every scalar input on a temporal attribute as a bucket scalar, every link of a relation both of whose roles
     * are to-many, and the order of every to-many list. The bean
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
        for (Input.Link k : links) inputData.link(k.factType(), k.first(), k.second());
        for (Map.Entry<Object, Map<String, ToManyList>> perHolder : lists.entrySet())
            for (ToManyList l : perHolder.getValue().values()) {
                List<Object> members = new ArrayList<>(l.written);
                for (Object member : l.mirrored.values())
                    if (members.stream().noneMatch(x -> x == member)) members.add(member);
                inputData.list(perHolder.getKey(), l.role, members);
            }
        return inputData;
    }

    /**
     * Sets on {@code session}, a fresh session of the build this case was read on, the globals of the case that the
     * build declares: every parameter the case binds, under its declared name, the evaluation date as
     * {@code rekendatum}, and on the DRL-text branch the day-kind predicates ({@link DroolsBackend.Build#dayKindGlobals}).
     * A parameter no lowered rule reads is no global of the build and is not set. A parameter a rule reads and the case
     * does not bind stays null, and its read faults EF-014 where the evaluation reaches it (RUNTIME-SEMANTICS ch. 5.7
     * step 2).
     */
    public void setGlobals(KieSession session) {
        Objects.requireNonNull(session, "session");
        Set<String> declared = new HashSet<>();
        for (KiePackage p : build.kieBase().getKiePackages())
            for (Global g : p.getGlobalVariables()) declared.add(g.getName());
        for (Map.Entry<String, Object> e : parameters.entrySet())
            if (declared.contains(e.getKey())) session.setGlobal(e.getKey(), e.getValue());
        if (evaluationDate != null && declared.contains(TmsTranspiler.REKENDATUM))
            session.setGlobal(TmsTranspiler.REKENDATUM, evaluationDate);
        for (Map.Entry<String, Predicate<LocalDate>> e : build.dayKindGlobals().entrySet())
            session.setGlobal(e.getKey(), e.getValue());
    }

    /** Reads {@code input.parameters} as the reference interpreter's case reader reads it (RUNTIME-SEMANTICS ch. 5.7
     *  step 3): every name a declared parameter under the name equality of {@link Namen#norm}, a time-dependent one
     *  with a timeline on its grid, any other with a value of its datatype, each held to the input-precision fence. */
    private void readParameters(JsonNode params) {
        if (params == null) return;
        if (!params.isObject())
            throw new IllegalArgumentException("input.parameters: expected an object, got " + params);
        for (Map.Entry<String, JsonNode> e : params.properties()) {
            RegelSpraakModel.Parameter declared = Namen.byName(model.parameters, e.getKey());
            if (declared == null) throw new IllegalArgumentException("parameter \"" + e.getKey() + "\" is not declared");
            String at = "parameter " + e.getKey();
            JsonNode w = e.getValue();
            boolean granular = declared.granularity() != null;
            if (CaseRunner.isTimeline(w) != granular)
                throw new IllegalArgumentException(at + (granular
                        ? ": a scalar value on a granular parameter (it takes a timeline)"
                        : ": a timeline value on a non-granular parameter"));
            if (granular) CaseRunner.typedSegments(w.get("timeline"), declared.datatype(), at);
            else CaseRunner.typed(w, declared.datatype(), at);
            CaseRunner.inputFits(w, declared.datatype(), at);
            Object value = granular
                    ? Period.list(reader.timelineSegments(w.get("timeline"), declared.datatype(), e.getKey(), at,
                            new CaseRunner.Grid(declared.granularity(), 1, declared.fixedStartPoint(), false), false))
                    : reader.valueToArg(w, e.getKey());
            parameters.put(Namen.ident(declared.name(), false), value);
        }
    }

    /**
     * Holds the instance {@code i} at position {@code at} to the shape of the case format, as the reference
     * interpreter's case reader does, before any part of it is read: an object; a label that is a non-empty string
     * outside the namespace {@code "::"} of the instances a creation makes, which a case label could otherwise hide;
     * an object type that is a string; and of the sections it carries, {@code attributes}, {@code roles} and
     * {@code characteristicTimelines} objects, {@code characteristics} a list of strings and every role a list of labels.
     * Which sections a case file must carry the replay checks ({@link CaseRunner#caseFile}).
     */
    private static void shape(JsonNode i, String at) {
        if (!i.isObject()) throw new IllegalArgumentException(at + ": expected an object, got " + i);
        JsonNode labelNode = i.get("label");
        if (labelNode == null || !labelNode.isTextual() || labelNode.asText().isEmpty())
            throw new IllegalArgumentException(at + ".label: expected a non-empty string, got " + labelNode);
        String label = labelNode.asText();
        if (label.contains("::"))
            throw new IllegalArgumentException("label \"" + label + "\": \"::\" is reserved for created instances");
        if (i.get("objectType") == null || !i.get("objectType").isTextual())
            throw new IllegalArgumentException(label + ".objectType: expected a string, got " + i.get("objectType"));
        for (String section : new String[]{"attributes", "roles", "characteristicTimelines"})
            if (i.has(section) && !i.get(section).isObject())
                throw new IllegalArgumentException(label + "." + section + ": expected an object, got " + i.get(section));
        JsonNode characteristics = i.get("characteristics");
        if (characteristics != null) {
            if (!characteristics.isArray())
                throw new IllegalArgumentException(label + ".characteristics: expected a list, got " + characteristics);
            for (int k = 0; k < characteristics.size(); k++)
                if (!characteristics.get(k).isTextual())
                    throw new IllegalArgumentException(label + ".characteristics[" + k + "]: expected a string, got "
                            + characteristics.get(k));
        }
        if (i.has("roles")) for (Map.Entry<String, JsonNode> role : i.get("roles").properties()) {
            String place = label + ".roles." + role.getKey();
            if (!role.getValue().isArray())
                throw new IllegalArgumentException(place + ": expected a list, got " + role.getValue());
            for (int k = 0; k < role.getValue().size(); k++)
                if (!role.getValue().get(k).isTextual())
                    throw new IllegalArgumentException(place + "[" + k + "]: expected a string, got " + role.getValue().get(k));
        }
    }

    private void beanFor(JsonNode i) {
        String label = i.get("label").asText();
        // one instance per label (the case schema): a second instance under a label would land on the bean of the first
        if (labelToBean.containsKey(label)) throw NameFault.duplicateLabel(label);
        ObjectType ot = CaseRunner.declaredObjectType(model, i.get("objectType").asText());
        if (ot == null) throw NameFault.undeclared("object-type", label, i.get("objectType").asText(), null);
        Object bean;
        try {
            bean = Class.forName(FactModelGenerator.PACKAGE + "." + Namen.ident(ot.name, true), true, cl)
                    .getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("could not make a bean for label '" + label + "'", e);
        }
        labelToBean.put(label, bean);
        labelType.put(label, ot);
    }

    private void setAttributesAndCharacteristics(JsonNode i) {
        Object bean = labelToBean.get(i.get("label").asText());
        ObjectType ot = labelType.get(i.get("label").asText());
        JsonNode attrs = i.get("attributes");
        if (attrs != null) for (Map.Entry<String, JsonNode> e : attrs.properties()) {
            String name = CaseRunner.declaredAttribute(ot, e.getKey());
            if (name == null) throw NameFault.undeclared("attribute", i.get("label").asText(), e.getKey(), ot.name);
            Datatype dt = ot.attribute(name);
            reader.ownField(ot, name, false, "sets");
            String site = i.get("label").asText() + "." + e.getKey();
            // a dimensioned attribute has a value per cell and none of its own (RUNTIME-SEMANTICS ch. 5.8c), so its
            // input is the cells variant and nothing else, as the reference interpreter reads it
            boolean cells = e.getValue().isObject() && e.getValue().size() == 1 && e.getValue().has("cells");
            if (reader.dimensioned(ot, name)) {
                if (!cells)
                    throw new IllegalArgumentException(site + ": a dimensioned attribute takes its input per cell"
                            + " (\"cells\"), never a scalar or a timeline (RUNTIME-SEMANTICS ch. 5.8c)");
                CaseRunner.call(bean, Namen.setter(name), Map.class,
                        cells(e.getValue().get("cells"), ot, name, dt, i.get("label").asText()));
                continue;
            }
            if (cells) throw new IllegalArgumentException(site + ": a cells input on an attribute without dimensions");
            JsonNode w = e.getValue();
            if (CaseRunner.isTimeline(w)) {
                if (!reader.temporal(ot, name))
                    throw new IllegalArgumentException(site + ": a timeline value on a non-temporal attribute");
                CaseRunner.typedSegments(w.get("timeline"), dt, site);
                CaseRunner.inputFits(w, dt, site);
                CaseRunner.call(bean, Namen.setter(name), List.class,
                        reader.timelineSegments(w.get("timeline"), dt, name, site, reader.grid(ot, name), false));
                continue;
            }
            CaseRunner.typed(w, dt, site);
            CaseRunner.inputFits(w, dt, site);
            if (reader.temporal(ot, name)) {
                CaseRunner.call(bean, Namen.setter(name), List.class, bucketInput(w, name, reader.grid(ot, name)));
                emmers.add(new Input.Bucket(bean, name));
                continue;
            }
            CaseRunner.call(bean, Namen.setter(name), CaseRunner.argType(dt), reader.valueToArg(w, name));
        }
        JsonNode characteristics = i.get("characteristics");
        if (characteristics != null) for (JsonNode written : characteristics) {
            String k = CaseRunner.declaredCharacteristic(ot, written.asText());
            if (k == null) throw NameFault.undeclared("characteristic", i.get("label").asText(), written.asText(), ot.name);
            // A CASE defect, not an uncarried form (maintainer decision 2026-09-09), as the sister arm below says
            // of the mirror-image mistake; this arm is the same seam from the other side. A TEMPORAL kenmerk's bean
            // member is a List<Segment>, so a scalar-true assertion reaches no reader here; in the interpreter and
            // the ASP backend it reaches none either, and BOTH answered 0 where the golden pins 30 rather than
            // saying anything (measured 2026-09-09 on jutterswet-tegenwacht and anchor/jutterswet-dubbelwacht).
            // All three readers refuse it at load. An Excluded would file a broken case under "a form the v0
            // backend does not carry" — a counted, reasoned refusal in the gate report for a case that is simply
            // wrong, and the one classification that could keep it out of sight.
            if (Namen.containsName(ot.characteristicGranularityMembers(), k))
                throw new IllegalArgumentException("scalar characteristic input '" + k + "' on the "
                        + "time-dependent characteristic of " + ot.name + ": deliver it as a characteristicTimelines "
                        + "entry — the static characteristics list asserts a scalar truth no temporal reader "
                        + "consults");
            reader.ownField(ot, k, true, "sets");
            CaseRunner.call(bean, Namen.setter(k), boolean.class, Boolean.TRUE);
        }
        JsonNode ctls = i.get("characteristicTimelines");
        if (ctls != null) for (Map.Entry<String, JsonNode> e : ctls.properties()) {
            String k = CaseRunner.declaredCharacteristic(ot, e.getKey());
            if (k == null) throw NameFault.undeclared("characteristic", i.get("label").asText(), e.getKey(), ot.name);
            // a CASE defect, not an uncarried form — ref refuses this shape at load (case.ml: "a
            // characteristic timeline on a non-temporal characteristic"), so it is hard here too; an
            // Excluded would file a broken corpus case under "not carried", silently green.
            if (!Namen.containsName(ot.characteristicGranularityMembers(), k))
                throw new IllegalArgumentException("a characteristic timeline on the non-temporal characteristic '" + e.getKey() + "'");
            reader.ownField(ot, k, true, "sets");
            CaseRunner.call(bean, Namen.setter(k), List.class, characteristicTimeline(e.getValue(), e.getKey()));
        }
    }

    /**
     * The cells of the input of the dimensioned attribute {@code attribute} of the instance {@code label}
     * (RUNTIME-SEMANTICS ch. 5.8c), as the map of its bean member holds them: each value under the key of its cell
     * ({@link FactModelGenerator#cellKey}), in the order of the input. Refused in this order, as the reference interpreter
     * refuses them: labels that name no cell of the attribute (EF-034), a cell the input names twice (EF-035), both
     * before any value is read, and then per cell a value of another variant than the datatype's ({@link
     * CaseRunner#typed}) or one its declared decimals do not hold (EF-004, its place the cell,
     * {@code J1.vondstgewicht[voorjaar]}).
     */
    private Map<List<String>, Object> cells(JsonNode cells, ObjectType ot, String attribute, Datatype dt, String label) {
        if (!cells.isArray())
            throw new IllegalArgumentException(label + "." + attribute + ": the cells input is no list of cells");
        List<String> dimensions = ot.dimensionsOf(attribute);
        List<List<String>> written = new ArrayList<>(), keys = new ArrayList<>();
        for (JsonNode c : cells) {
            if (!c.isObject() || !c.has("labels") || !c.get("labels").isArray() || !c.has("value"))
                throw new IllegalArgumentException(label + "." + attribute + ": a cell of the input carries no list of"
                        + " labels and no value: " + c);
            List<String> labels = new ArrayList<>();
            for (JsonNode l : c.get("labels")) labels.add(l.asText());
            List<String> key = FactModelGenerator.cellKey(model, dimensions, labels);
            if (key == null)
                throw new CellFault("EF-034", label, attribute, labels, dev.uchar.regelspraak.message.Msg.drools("EF-034",
                        "label", label, "attribute", attribute, "labels", String.join(", ", labels), "dimensions",
                        String.join(", ", dimensions)));
            written.add(labels);
            keys.add(key);
        }
        for (int k = 0; k < keys.size(); k++)
            if (keys.subList(0, k).contains(keys.get(k)))
                throw new CellFault("EF-035", label, attribute, written.get(k), dev.uchar.regelspraak.message.Msg.drools(
                        "EF-035", "label", label, "attribute", attribute, "labels", String.join(", ", written.get(k))));
        Map<List<String>, Object> out = new LinkedHashMap<>();
        for (int k = 0; k < keys.size(); k++) {
            String place = label + "." + attribute + "[" + String.join(", ", written.get(k)) + "]";
            JsonNode value = cells.get(k).get("value");
            CaseRunner.typed(value, dt, place);
            CaseRunner.inputFits(value, dt, place);
            out.put(keys.get(k), reader.valueToArg(value, attribute));
        }
        return out;
    }

    /** An instance label the case input carries twice (EF-039), or a name the input writes that the document does not
     *  declare, the object type of an instance, an attribute, a characteristic or a role key (EF-040), RUNTIME-SEMANTICS
     *  ch. 5.7b: a fault kind of the case input, with the label of the instance and, for EF-040, the name as the case
     *  writes it. Public, as {@link CellFault} is, so that a caller that hands this reader a case can name the fault kind
     *  by its type. */
    public static final class NameFault extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;

        private final String code;
        private final String label;
        private final String name;

        private NameFault(String code, String label, String name, dev.uchar.regelspraak.message.Msg message) {
            super(message.text());
            this.code = code;
            this.label = label;
            this.name = name;
        }

        static NameFault duplicateLabel(String label) {
            return new NameFault("EF-039", label, null, dev.uchar.regelspraak.message.Msg.drools("EF-039", "label", label));
        }

        /** {@code what} is the part of the sentence: {@code object-type}, {@code attribute}, {@code characteristic} or
         *  {@code role}; {@code objectType} the type of the instance, which the sentence of an attribute and of a
         *  characteristic names. */
        static NameFault undeclared(String what, String label, String name, String objectType) {
            return new NameFault("EF-040", label, name, objectType == null
                    ? dev.uchar.regelspraak.message.Msg.drools("EF-040." + what, "label", label, "name", name)
                    : dev.uchar.regelspraak.message.Msg.drools("EF-040." + what, "label", label, "name", name,
                            "objectType", objectType));
        }

        public String code() {
            return code;
        }

        /** The parameters of the kind (ch. 5.7b): the label of the instance, and for EF-040 the name. */
        public Map<String, String> params() {
            Map<String, String> out = new LinkedHashMap<>();
            out.put("label", label);
            if (name != null) out.put("name", name);
            return out;
        }
    }

    /** A cell of the case input whose labels name no cell of its attribute (EF-034) or that the input names twice
     *  (EF-035), RUNTIME-SEMANTICS ch. 5.8c: a fault kind of the case input, with the label of the instance, the
     *  attribute and the labels of the cell as the case writes them. Public, as {@link CaseRunner.InputPrecisionFault}
     *  is, so that a caller that hands this reader a case can name the fault kind by its type. */
    public static final class CellFault extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;

        private final String code;
        private final String label;
        private final String attribute;
        private final String labels;

        CellFault(String code, String label, String attribute, List<String> labels, dev.uchar.regelspraak.message.Msg message) {
            super(message.text());
            this.code = code;
            this.label = label;
            this.attribute = attribute;
            this.labels = String.join(", ", labels);
        }

        public String code() {
            return code;
        }

        /** The parameters of the kind (ch. 5.7b): the label of the instance, the attribute and the labels of the cell,
         *  joined by ", ". */
        public Map<String, String> params() {
            Map<String, String> out = new LinkedHashMap<>();
            out.put("label", label);
            out.put("attribute", attribute);
            out.put("labels", labels);
            return out;
        }
    }

    private void setRoles(JsonNode i) {
        Object bean = labelToBean.get(i.get("label").asText());
        ObjectType tI = labelType.get(i.get("label").asText());
        JsonNode roles = i.get("roles");
        if (roles == null) return;
        // per role the key that named it first: a role under its name and its plural would be two lists of one relation
        Map<String, String> keyPerRole = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> written : roles.properties()) {
            // the key in its declared spelling, which the model's role searches compare; the link keeps the written one
            Map.Entry<String, JsonNode> e = Map.entry(declaredRoleKey(i.get("label").asText(), written.getKey()),
                    written.getValue());
            // the role the key names opposite this instance's type, as the reference interpreter resolves it: a key that
            // names a role of a relation this type does not fill is a defect of the case, never a link by the type pair
            List<RegelSpraakModel.Role> far = rolesOpposite(tI, written.getKey());
            if (far.isEmpty())
                throw new IllegalArgumentException("role key \"" + written.getKey() + "\" does not resolve to a declared"
                        + " role opposite \"" + tI.name + "\"");
            if (far.size() == 1) {
                String previous = keyPerRole.putIfAbsent(Namen.norm(far.get(0).name()), written.getKey());
                if (previous != null)
                    throw new IllegalArgumentException("role keys \"" + previous + "\" and \"" + written.getKey() + "\" on \""
                            + i.get("label").asText() + "\" both name the role \"" + far.get(0).name() + "\"; deliver one key");
            }
            for (JsonNode labelNode : e.getValue()) {
                RoleLink link = new RoleLink(i.get("label").asText(), written.getKey(), labelNode.asText());
                Object target = labelToBean.get(labelNode.asText());
                if (target == null) throw new LinkTargetFault(link);
                ObjectType tL = labelType.get(labelNode.asText());
                ObjectType filler = far.size() == 1 ? model.objectTypeByName(far.get(0).objectTypeName()) : null;
                if (filler != null && !tL.isA(filler))
                    throw new IllegalArgumentException("role \"" + written.getKey() + "\" on \"" + link.label() + "\" links \""
                            + link.target() + "\" of type \"" + tL.name + "\"; declared filler type is \"" + filler.name + "\"");
                // a relation both of whose roles are to-many has no reference field on either bean: its link is a fact
                // of its own, the same one from whichever side the case writes it (ch. 5.7a)
                if (manyToManyLink(tI, tL, e.getKey(), bean, target)) continue;
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
                        // is its CARDINALITY — a to-many role's storage is the far end's field — where one sentence
                        // for both would blame a non-unique derivation that in fact succeeded.
                        throw new RoleRefusal(DroolsCode.CASE_ROLE_NO_FIELD.reason("role", written.getKey(), "fromType", tI.name, "toType", tL.name, "viaReason", viaName == null ? DroolsCode.CASE_ROLE_NO_FIELD.part("via-none")
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

    /** Whether the role key {@code key}, in its declared spelling, under which instance {@code bean} of type {@code tI}
     *  names {@code target} of type {@code tL}, is a role of a relation of two roles that are both to-many; if so, its
     *  link is recorded once, the end that fills the first declared role first. The key names the role the target fills,
     *  so the target is the first end where that role is the first. */
    private boolean manyToManyLink(ObjectType tI, ObjectType tL, String key, Object bean, Object target) {
        RegelSpraakModel.RoleBindingRef rb = model.connectingRoleViaNameBinding(tI, tL, key);
        if (rb == null) return false;
        RegelSpraakModel.FactType ft = factType(model, rb.factType());
        if (ft == null || ft.reciprocal || ft.roles.size() != 2 || !ft.roles.get(0).many() || !ft.roles.get(1).many())
            return false;
        boolean targetFirst = Namen.norm(ft.roles.get(0).name()).equals(Namen.norm(rb.role()));
        Object first = targetFirst ? target : bean, second = targetFirst ? bean : target;
        String factType = ft.name;
        if (links.stream().noneMatch(k -> k.factType().equals(factType) && k.first() == first && k.second() == second))
            links.add(new Input.Link(factType, first, second));
        return true;
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

    private ToManyList list(Object holder, String role) {
        return lists.computeIfAbsent(holder, k -> new LinkedHashMap<>())
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
        // The cause is read off what holds the property, never assumed: a declared member, no derived field, a
        // property of another declaration, a field another role holds — two same-named roles toward different
        // types, corpus/red/ambiguous-case-role-key — or a field that cannot hold the target. A target of a SUBTYPE
        // of the role's filler is no cause: the field of the filler's class takes it (IR-SPEC ch. 2.6).
        FactModelGenerator.Origin holder = FactModelGenerator.fieldOrigin(model, carrier, role);
        String field = Namen.ident(carrier.name, true) + "." + Namen.ident(role, false);
        String cause;
        if (carrier.hasMember(role)) cause = "which shares its name with a declared member";
        else if (holder == null) cause = "for which the bean model derives no field";
        else if (holder.kind() != FactModelGenerator.Origin.Kind.ROL)
            cause = "whose bean property " + field + " belongs to " + described(holder);
        else if (!holder.equals(FactModelGenerator.roleOrigin(model, carrier, target, role)))
            cause = "whose field " + field + " another role already holds, " + described(holder);
        else cause = "whose field " + field + " cannot hold a " + target.name;
        throw new RoleRefusal(DroolsCode.NAME_SHARING_ONE_ROLE.reason("role", link.role(), "targetRole", role, "objectType", carrier.name, "cause", cause), link);
    }

    /** A declaration in the words of this reader's refusals. */
    private static String described(FactModelGenerator.Origin h) {
        return switch (h.kind()) {
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
        Object current = CaseRunner.invokeGetter(carrier, Namen.getter(role));
        if (current != null && current != target)
            throw new RoleCollision(DroolsCode.ROLE_SECOND_TARGET.reason("role", role).text(), link);
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
            throw new IllegalStateException("no getter " + Namen.getter(role) + " on "
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
     *  <p>Scoped to the WEDERKERIG shape, and that scope is a DIVISION OF LABOUR: RUNTIME-SEMANTICS ch. 5.7a
     *  (maintainer decision 2026-09-10) makes EVERY supplied link establish its relation in both directions, and
     *  ref's {@code Case.materialise_relations} mirrors on the SISTER role accordingly. Three shapes, three
     *  answers:
     *  <ul>
     *    <li><b>1:N</b> — the relation has exactly ONE reference field and it necessarily serves both
     *        directions, so there is no second write to make. {@link #setRoles} lands the case's write on that
     *        field through the sister role's NAME, and the three spellings of one relation give one fact base
     *        ({@code SisterRoleInputWitnessTest}, over the {@code jutterswet-ketenband} golden).</li>
     *    <li><b>1:1</b> — each direction owns its own field, so ch. 5.7a needs a SECOND write on the target
     *        bean. {@link #setRoles} makes it, right after the forward write, on the sister role and under the
     *        same name-sharing gate: a case writing {@code W1{opvolger:[W2]}} over a to-one/to-one self-pair
     *        reads W1 as W2's voorganger here, as it does in ref ({@code OpslagDirectionWitnessTest}).</li>
     *    <li><b>wederkerig</b> (§13.3.9.2, one role that is its own opposite) — THIS pass, and deliberately
     *        a separate one: its order-independence argument above is the reason, and the 1:1 arm skips
     *        the shape (it fires only when the sister role is a DIFFERENT role) so the two never collide.</li>
     *  </ul>
     *  A collision names the link whose leg the pass mirrored, the one that wrote the bean's own field: a leg the pass
     *  wrote itself never collides, since it writes only an empty field and its partner then reads back. */
    /** A cycle in the links of the switch relation of an instance-ordered part (RUNTIME-SEMANTICS ch. 6.2): the fault
     *  kind EF-033 of the case input, with the declared name of the fact type and the labels of the cycle. */
    public static final class LinkCycleFault extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;

        private final String factType;
        private final String cycle;

        LinkCycleFault(String factType, String cycle) {
            super(dev.uchar.regelspraak.message.Msg.drools("EF-033", "factType", factType, "cycle", cycle).text());
            this.factType = factType;
            this.cycle = cycle;
        }

        public String code() {
            return "EF-033";
        }

        /** The parameters of the kind (ch. 5.7b): the fact type and the labels of the cycle, joined by ", ". */
        public Map<String, String> params() {
            Map<String, String> out = new LinkedHashMap<>();
            out.put("factType", factType);
            out.put("cycle", cycle);
            return out;
        }
    }

    /**
     * RUNTIME-SEMANTICS ch. 6.2: the members of an instance-ordered part evaluate per object along the chain its switch
     * relation lays, and the chain must end. No form writes that relation (IR-SPEC ch. 5.3), so its links are the
     * case's, and a cycle in them is a fault of the case input, EF-033, refused here before anything goes into a
     * session. Truth maintenance would fire such a cycle up to the fire cap, with its values feeding each other. The
     * links are read as the case writes them, from either side: a key that names the predecessor's role gives the
     * instance its predecessor, a key that names the other role makes the instance the predecessor of every label it
     * lists. The cycle reported is the one whose smallest label is the smallest, listed from that label along the
     * predecessor's role, as the reference interpreter's case reader reports it.
     */
    private void refuseLinkCycles(DroolsBackend.Build build, JsonNode instances) {
        for (RegelSpraakModel.RoleBindingRef walk : build.execModelTranspiler().instanceWalks()) {
            RegelSpraakModel.FactType ft = factType(model, walk.factType());
            if (ft == null || ft.roles.size() != 2)
                throw new IllegalStateException("CaseInput: the switch relation " + walk.factType() + " has no two roles");
            RegelSpraakModel.Role toPredecessor = Namen.norm(ft.roles.get(0).name()).equals(Namen.norm(walk.role()))
                    ? ft.roles.get(0) : ft.roles.get(1);
            RegelSpraakModel.Role toSuccessor = toPredecessor == ft.roles.get(0) ? ft.roles.get(1) : ft.roles.get(0);
            ObjectType fillsSuccessor = model.objectTypeByName(toSuccessor.objectTypeName());
            ObjectType fillsPredecessor = model.objectTypeByName(toPredecessor.objectTypeName());
            Map<String, String> predecessor = new TreeMap<>();
            for (JsonNode i : instances) {
                String label = i.get("label").asText();
                ObjectType type = labelType.get(label);
                JsonNode roles = i.get("roles");
                if (roles == null || type == null) continue;
                for (Map.Entry<String, JsonNode> e : (Iterable<Map.Entry<String, JsonNode>>) roles::fields) {
                    String key = Namen.norm(e.getKey());
                    if (names(toPredecessor).contains(key) && type.isA(fillsSuccessor))
                        for (JsonNode l : e.getValue()) predecessor.put(label, l.asText());
                    else if (names(toSuccessor).contains(key) && type.isA(fillsPredecessor))
                        for (JsonNode l : e.getValue()) predecessor.put(l.asText(), label);
                }
            }
            Map<String, Integer> state = new java.util.HashMap<>();
            List<List<String>> cycles = new ArrayList<>();
            for (String start : predecessor.keySet()) {
                List<String> path = new ArrayList<>();
                String label = start;
                while (label != null && !state.containsKey(label)) {
                    state.put(label, 1);
                    path.add(label);
                    label = predecessor.get(label);
                }
                if (label != null && state.get(label) == 1) cycles.add(new ArrayList<>(path.subList(path.indexOf(label), path.size())));
                for (String l : path) state.put(l, 2);
            }
            if (cycles.isEmpty()) continue;
            cycles.sort(Comparator.comparing((List<String> c) -> Collections.min(c)));
            List<String> cycle = cycles.get(0);
            Collections.rotate(cycle, -cycle.indexOf(Collections.min(cycle)));
            throw new LinkCycleFault(ft.name, String.join(", ", cycle));
        }
    }

    /** The declared name or plural of a role that the role key {@code key} of instance {@code label} is under
     *  {@link Namen#norm}, the name equality of the reference interpreter's case reader. A key that names no declared
     *  role is a defect of the case, EF-040, which that reader refuses at load. */
    private String declaredRoleKey(String label, String key) {
        for (RegelSpraakModel.FactType ft : model.factTypes.values())
            for (RegelSpraakModel.Role r : ft.roles)
                for (String declared : Arrays.asList(r.name(), r.plural()))
                    if (declared != null && Namen.norm(declared).equals(Namen.norm(key))) return declared;
        throw NameFault.undeclared("role", label, key, null);
    }

    /** The roles the role key {@code key} on an instance of {@code type} names, as the reference interpreter's case reader
     *  resolves it: of a fact type of two roles the role the key names by its name or plural, where {@code type} fills
     *  the other one (the declared filler an upper bound, IR-SPEC ch. 2.6); of a reciprocal fact type its one role, where
     *  {@code type} fills it and the key names it. More than one is an ambiguity, none a key no role opposite
     *  {@code type} answers to. */
    private List<RegelSpraakModel.Role> rolesOpposite(ObjectType type, String key) {
        List<RegelSpraakModel.Role> out = new ArrayList<>();
        for (RegelSpraakModel.FactType ft : model.factTypes.values()) {
            if (ft.roles.size() == 2) {
                RegelSpraakModel.Role a = ft.roles.get(0), b = ft.roles.get(1);
                if (fills(type, a) && names(b).contains(Namen.norm(key))) out.add(b);
                if (fills(type, b) && names(a).contains(Namen.norm(key))) out.add(a);
            } else if (ft.roles.size() == 1 && ft.reciprocal) {
                RegelSpraakModel.Role r = ft.roles.get(0);
                if (fills(type, r) && names(r).contains(Namen.norm(key))) out.add(r);
            }
        }
        return out;
    }

    /** Whether an instance of {@code type} fills the role {@code r}. */
    private boolean fills(ObjectType type, RegelSpraakModel.Role r) {
        return type.isA(model.objectTypeByName(r.objectTypeName()));
    }

    /** The fact type of {@code model} that {@code name} names under {@link Namen#norm}, or null. */
    static RegelSpraakModel.FactType factType(RegelSpraakModel model, String name) {
        RegelSpraakModel.FactType ft = null;
        for (RegelSpraakModel.FactType f : model.factTypes.values())
            if (Namen.norm(f.name).equals(Namen.norm(name))) ft = f;
        return ft;
    }

    /** The keys a case may write a role under: its name and its plural, normalised. */
    private static List<String> names(RegelSpraakModel.Role r) {
        List<String> out = new ArrayList<>();
        if (r.name() != null) out.add(Namen.norm(r.name()));
        if (r.plural() != null) out.add(Namen.norm(r.plural()));
        return out;
    }

    private void mirrorReciprocalRoles() {
        for (Map.Entry<String, Object> instance : labelToBean.entrySet()) {
            Object bean = instance.getValue();
            // the reciprocal role of an instance of a SUBTYPE of its filler is its ancestor's (IR-SPEC ch. 2.6):
            // asked of the bean's own type alone, such a leg would go unmirrored and the partner read an empty
            // relation
            String role = null;
            for (ObjectType t : labelType.get(instance.getKey()).chain())
                if ((role = FactModelGenerator.selfPairRole(model, t)) != null) break;
            if (role == null) continue;
            Object partner = CaseRunner.invokeGetter(bean, Namen.getter(role));
            if (partner == null) continue;
            Object readBack = CaseRunner.invokeGetter(partner, Namen.getter(role));
            if (readBack == bean) continue;
            if (readBack != null)
                throw new RoleCollision(DroolsCode.ROLE_RECIPROCAL_COLLISION.reason("role", role).text(), writtenBy(bean, role));
            CaseRunner.call(partner, Namen.setter(role), fieldClass(partner, role), bean);
        }
    }

    /** The link of the case that wrote the to-one field {@code role} of {@code carrier}. */
    private RoleLink writtenBy(Object carrier, String role) {
        RoleLink link = writers.getOrDefault(carrier, Map.of()).get(Namen.getter(role));
        if (link == null)
            throw new IllegalStateException("the field '" + role + "' of " + carrier.getClass().getSimpleName()
                    + " carries a target that no role of the case wrote");
        return link;
    }

    // ---------------- §13.3.6 temporal input (the List<Segment> bean members) ----------------

    /** The {@code List<Segment>} a scalar case value {@code w} denotes for the temporal attribute {@code attr} on the
     *  grid {@code gran}: the asOfDate-bucket convention recorded in {@code corpus/golden/README.md}, by which the scalar
     *  is the value of the period of the grid that holds the evaluation date (RUNTIME-SEMANTICS ch. 8.4). The reference
     *  interpreter keeps that scalar aside and folds this one-period timeline inside the collapse of ch. 3.6; the bean
     *  model has no scalar slot beside the {@code List<Segment>} field, so the period is laid here and the subject's own
     *  total ({@code ComputeHelpers.ownTotal}) folds the same segment to the same scalar. The bean then holds a shape the
     *  reference interpreter does not serve as a timeline, so the caller records the scalar as a bucket scalar, which
     *  {@link #inputData()} names to {@link DroolsBackend#insert}, and the analysis reads the two conventions apart and
     *  faults where a form needs the one and finds the other. */
    private List<Object> bucketInput(JsonNode w, String attr, CaseRunner.Grid gran) {
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
            throw new IllegalArgumentException("malformed characteristic timeline on '" + characteristic + "'");
        List<LocalDate[]> sorted = new ArrayList<>();
        for (JsonNode pj : periodes) {
            LocalDate van = CaseRunner.boundDate(pj, "from", characteristic), to = CaseRunner.boundDate(pj, "to", characteristic);
            if (!van.isBefore(to))
                throw new IllegalArgumentException("period 'to' on '" + characteristic + "' does not lie after 'from'");
            sorted.add(new LocalDate[]{ van, to });
        }
        sorted.sort((a, b) -> a[0].equals(b[0]) ? a[1].compareTo(b[1]) : a[0].compareTo(b[0]));
        List<Object> out = new ArrayList<>();
        LocalDate previousTo = null;
        for (LocalDate[] p : sorted) {
            if (previousTo != null && p[0].isBefore(previousTo))
                throw new IllegalArgumentException("periods on '" + characteristic + "' overlap");
            previousTo = p[1];
            out.add(reader.segment(p[0], p[1], null));
        }
        return out;
    }
}
