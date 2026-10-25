package dev.uchar.regelspraak.schema;

import dev.uchar.regelspraak.message.Msg;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.FactType;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;
import dev.uchar.regelspraak.model.RegelSpraakModel.Role;
import dev.uchar.regelspraak.refusal.DroolsCode;
import dev.uchar.regelspraak.refusal.Reason;
import dev.uchar.regelspraak.transpiler.core.Namen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates the fact-bean sources a resolved RegelSpraak model implies (the explicit fact-model contract) — the EXPLICIT form
 * of the contract the generated rules assume: one JavaBean per objecttype in {@code dev.uchar.regelspraak.facts}
 * with a getter/setter per attribute ({@code BigDecimal}/{@code String}/{@code Boolean}/{@code LocalDate}/
 * {@code LocalDateTime}; a {@code Map<String,BigDecimal>} for a §3.6 gedimensioneerd attribute; a
 * {@code List<Segment>} for a §13.3.6 tijdsafhankelijk attribute), an {@code is<X>}/{@code set<X>} boolean
 * pair per kenmerk (a temporal kenmerk is a {@code List<Segment>} true-period timeline), and — per §13.3.9
 * feittype — the to-one REFERENCE field on the MANY side, named after the ONE side's role, including the
 * §13.3.9.2 wederkerig SELF-PAIR field (see {@link #selfPairRole}) and the ORDINARY self-typed pair's one
 * field per to-one role (see {@link #selfPairRoles}). {@code Segment} and
 * {@code Inconsistentie} are emitted alongside; the production runtime builds and reads both by name (with {@code van},
 * {@code tot}, {@code waarde}), so they keep these names.
 *
 * <p>The MEMBER DERIVATION ({@link #members}) is shared with {@link ContractValidator}, so what this
 * generator emits and what the validator demands can never drift apart.
 */
public final class FactModelGenerator {
    private FactModelGenerator() {}

    /** The package the generated rules import their fact classes from (both back-ends). */
    public static final String PACKAGE = "dev.uchar.regelspraak.facts";

    /** One expected bean member: the Java property, its type source-name, and whether the getter is the
     *  boolean {@code is<X>} form (a scalar kenmerk). */
    public record Member(String property, String javaType, boolean isForm) {}

    /** The declaration a bean member stands for: an attribute or a characteristic {@code name} of the object type
     *  {@code source} that declares it, or the role {@code name} of the fact type {@code source} (null when the role
     *  resolves to no single declaration). One origin reached twice is one declaration: a role field is derived
     *  again on every subtype of the type that plays the other role, and once per subtype of its own filler. */
    public record Origin(Kind kind, String name, String source) {
        public enum Kind { ATTRIBUUT, KENMERK, ROL }

        /** The declaration in the words of a refusal. */
        public Msg description() {
            return switch (kind) {
                case ATTRIBUUT -> Msg.drools("PART.origin.attribute", "name", name, "source", source);
                case KENMERK -> Msg.drools("PART.origin.characteristic", "name", name, "source", source);
                case ROL -> source == null ? Msg.drools("PART.origin.role", "name", name)
                        : Msg.drools("PART.origin.role-of-fact-type", "name", name, "source", source);
            };
        }
    }

    /** A bean member the model implies and the declaration it stands for, before the dedup of {@link #allMembers}. */
    private record Candidate(Member member, Origin origin) {}

    /** IR-SPEC ch. 2.2: the members a subtype's bean declares ITSELF. The generated bean
     *  {@code extends} its supertype's, so every inherited member is already a field there; emitting
     *  it again would hide the ancestor's. The subtraction is by PROPERTY NAME, the same key the dedup
     *  below uses, because the derived reference arms re-derive the SAME role field on a subtype that
     *  an ancestor already carries (a relation hung on the supertype), and it runs over every ancestor:
     *  subtracting the direct supertype's own members only let a grandchild declare such a field again. */
    public static List<Member> members(RegelSpraakModel model, ObjectType ot) {
        List<Member> all = allMembers(model, ot);
        if (ot.supertype == null) return all;
        java.util.Set<String> inherited = new java.util.HashSet<>();
        List<ObjectType> chain = ot.chain();
        for (int i = 1; i < chain.size(); i++)
            for (Member l : allMembers(model, chain.get(i))) inherited.add(l.property());
        List<Member> out = new ArrayList<>();
        for (Member l : all) if (!inherited.contains(l.property())) out.add(l);
        return out;
    }

    private static List<Member> allMembers(RegelSpraakModel model, ObjectType ot) {
        List<Member> out = new ArrayList<>();
        for (Candidate k : deduplicated(model, ot)) out.add(k.member());
        return out;
    }

    /** The candidates of {@link #candidates} that keep their property on the bean of {@code ot}, with their origin. */
    private static List<Candidate> deduplicated(RegelSpraakModel model, ObjectType ot) {
        List<Candidate> out = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();   // ONE bean property per name: an attribute,
        // kenmerk and to-one role may legally share a name (a single name can be declared as both a kenmerk
        // and a Boolean attribute); the bean can carry only one field, and the runtime property resolution
        // reads whichever getter exists — so the FIRST derivation (attribute > kenmerk > referentie) wins.
        // What lost the property has no field of its own: displacements() names it, and the backend refuses
        // every use of it by name.
        for (Candidate k : candidates(model, ot))
            if (seen.add(k.member().property())) out.add(k);
        return out;
    }

    /** The member the bean of {@code ot} carries under {@code property}, its own or an inherited one, with the
     *  declaration it stands for; null when no bean of the chain has that property. The root-most type of the
     *  chain that derives the property declares the field: {@link #members} leaves it to that ancestor's bean. */
    private static Candidate field(RegelSpraakModel model, ObjectType ot, String property) {
        List<ObjectType> chain = ot.chain();
        for (int i = chain.size() - 1; i >= 0; i--)
            for (Candidate k : deduplicated(model, chain.get(i)))
                if (k.member().property().equals(property)) return k;
        return null;
    }

    /** The declaration the bean property {@code property} of {@code ot}'s bean stands for, its own or an inherited
     *  one; null when no bean of the chain has that property. For a reference field it names the role and its fact
     *  type, so two fields of one name on the beans of two types tell their relations apart. */
    public static Origin origin(RegelSpraakModel model, ObjectType ot, String property) {
        Candidate k = field(model, ot, property);
        return k == null ? null : k.origin();
    }

    /** Every bean member the model implies for {@code ot}, with its origin, in the order the dedup of
     *  {@link #allMembers} keeps the first of: the attributes, the kenmerken, the reference fields. */
    private static List<Candidate> candidates(RegelSpraakModel model, ObjectType ot) {
        List<Candidate> out = new ArrayList<>();
        // The two loops below ITERATE the OWN declaration maps, deliberately, and are the reason this
        // class reads no chain-closed facet map either: {@link #members} subtracts what the ancestors'
        // beans already declare and the generated class `extends` them, so a chain-closed iteration would
        // visit every inherited member once per descendant only to have the subtraction remove it again.
        // The facet lookups INSIDE them are keyed by a name this very iteration produced, so the nearest
        // declaration IS ot (WF-105 refuses shadowing) and the own map already answers for it.
        for (Map.Entry<String, Datatype> e : ot.attributes.entrySet()) {
            String name = e.getKey();
            String type;
            if (!ot.dimensionsOf(name).isEmpty()) type = "java.util.Map<String, java.math.BigDecimal>";   // §3.6
            else if (ot.attributeGranularity.containsKey(name)) type = "java.util.List<Segment>";       // §13.3.6
            else type = javaType(e.getValue());
            out.add(new Candidate(new Member(Namen.ident(name, false), type, false),
                    new Origin(Origin.Kind.ATTRIBUUT, name, ot.name)));
        }
        for (String k : ot.characteristics) {
            boolean temporal = ot.characteristicGranularity.containsKey(k);                                    // §13.3.6
            out.add(new Candidate(new Member(Namen.ident(k, false), temporal ? "java.util.List<Segment>" : "boolean",
                    !temporal), new Origin(Origin.Kind.KENMERK, k, ot.name)));
        }
        // §13.3.9: the scalar to-one reference fields — derived from the SAME authority the transpilers
        // navigate by (connectingRoles + toManyRole): for every other objecttype, the holder carries ONE
        // field per uniquely addressable to-one role of that type, named after the role ("Eén holder betreft
        // meerdere elementen" -> Element.holder : Houder), UNLESS that navigation crosses a to-many role
        // (then the scalar reference lives on the other side).
        //
        // ONE FIELD PER ROLE, not per PAIR (card drools-rolvelden-herhaalde-typenparen). The by-PAIR search,
        // connectingRole(ot, doelwit), answers null the moment a SECOND feittype relates the same two object
        // types, so asked by pair a bean would carry no reference at all and the fold's join, the §13.4.7
        // creation's three positions, the navigated read and the case-input write would each refuse a document
        // whose author wrote a perfectly unique role name. The ambiguity is not in the source; it is in asking
        // the question by pair. connectingRoles asks it by NAME, per role,
        // and keeps every refusal that is genuinely a refusal: a name two roles share resolves to nothing,
        // a to-many role's storage stays the far end's field, and a declared member of the same name still
        // displaces the reference through the dedup above. Under-implied, never over-implied, measured per
        // role and not per pair.
        //
        // The by-pair authority itself stays ambiguous: a consumer that has no written or
        // symbolic role still finds a repeated pair unresolvable (RUNTIME-SEMANTICS ch. 5.5), and this
        // derivation is what makes the field EXIST for the consumers that do have one.
        //
        // The field's TYPE is the role's declared filler, not the doelwit this loop reaches it from. The loop
        // reaches one role once per subtype of its filler (IR-SPEC ch. 2.6: the filler is an upper bound), and
        // the dedup keeps the first; typed after the doelwit, the first-DECLARED of those types would decide the
        // field's class, so a subtype declared before its supertype would narrow the field and an instance of the
        // supertype would not fit it. The filler's class takes every instance the role takes, since each
        // subtype's bean extends it.
        for (ObjectType target : model.objectTypes.values()) {
            if (target == ot) continue;
            for (String role : model.connectingRoles(ot, target)) {
                if (model.toManyRole(ot, target, role)) continue;
                out.add(new Candidate(new Member(Namen.ident(role, false),
                        Namen.ident(fillerType(model, ot, target, role).name, true), false),
                        roleOrigin(model, ot, target, role)));
            }
        }
        // §13.3.9.2: the SELF-PAIR reference field, which the loop above cannot reach because it skips
        // ot itself. It is derivable for exactly one shape — a wederkerig feittype — see selfPairRole.
        String selfRole = selfPairRole(model, ot);
        if (selfRole != null)
            out.add(new Candidate(new Member(Namen.ident(selfRole, false), Namen.ident(ot.name, true), false),
                    roleOrigin(model, ot, ot, selfRole)));
        for (String role : selfPairRoles(model, ot))                                                   // §13.3.9 self-typed
            out.add(new Candidate(new Member(Namen.ident(role, false), Namen.ident(ot.name, true), false),
                    roleOrigin(model, ot, ot, role)));
        return out;
    }

    /** The declaration of the role {@code role} that a field on {@code holder} toward {@code target} stands for:
     *  the role of that name that {@code target} may fill, in a fact type that relates {@code holder} — the search
     *  {@code connectingRoles} admits a role by, which leaves exactly one. Public for the case reader, which tells
     *  the field of this very role apart from a field another role took. */
    public static Origin roleOrigin(RegelSpraakModel model, ObjectType holder, ObjectType target, String role) {
        List<Map.Entry<FactType, Role>> roles = rolesTo(model, holder, target, role);
        return new Origin(Origin.Kind.ROL, role, roles.size() == 1 ? roles.get(0).getKey().name : null);
    }

    /** The declared filler of the role {@code role} that {@code target} fills from {@code holder}: the type the
     *  reference field holds. {@code target} itself where the role resolves to no single declaration or its filler to
     *  no declared type — the fall-back {@link #fills} keeps for a filler the model does not declare. */
    private static ObjectType fillerType(RegelSpraakModel model, ObjectType holder, ObjectType target, String role) {
        List<Map.Entry<FactType, Role>> roles = rolesTo(model, holder, target, role);
        ObjectType filler = roles.size() == 1 ? model.objectTypeByName(roles.get(0).getValue().objectTypeName()) : null;
        return filler != null ? filler : target;
    }

    /** The roles named {@code role} that {@code target} may fill in a fact type relating {@code holder}, each with
     *  its fact type — the one search behind {@link #roleOrigin} and {@link #fillerType}. */
    private static List<Map.Entry<FactType, Role>> rolesTo(RegelSpraakModel model, ObjectType holder,
            ObjectType target, String role) {
        List<Map.Entry<FactType, Role>> out = new ArrayList<>();
        for (FactType ft : model.factTypes.values())
            for (Role r : ft.roles)
                if (role.equalsIgnoreCase(r.name()) && fills(model, r, target)
                        && (ft.reciprocal ? fills(model, r, holder)
                                          : ft.roles.stream().anyMatch(h -> h != r && fills(model, h, holder))))
                    out.add(Map.entry(ft, r));
        return out;
    }

    /** Whether {@code ot} may fill {@code r}: it is the role's filler or a subtype of it. */
    private static boolean fills(RegelSpraakModel model, Role r, ObjectType ot) {
        ObjectType filler = model.objectTypeByName(r.objectTypeName());
        return filler != null ? ot.isA(filler) : r.objectTypeName().equalsIgnoreCase(ot.name);
    }

    /** A bean member without a field of its own. On the bean of {@code type} the property of the member
     *  {@code displaced} stands for belongs to {@code owner}, which the generator ports first: an ancestor's
     *  member before a descendant's, and within one type the attributes, the kenmerken, the reference fields, in
     *  that order and each in declaration order. {@link Namen#ident} drops the spaces and hyphens of a name and
     *  camel-cases its words, so two names the verifier keeps apart can meet here, and WF-002 lets an attribute and
     *  a characteristic share even the same name. Whatever reads or writes {@code displaced} reaches the field of
     *  {@code owner}: a crash at fire when their types differ, and when they agree the one member silently
     *  written through the other. */
    public record Displacement(ObjectType type, Origin displaced, Member displacedMember, Origin owner,
                              Member ownerMember) {
        /** The bean property both fall on, as {@code <Klasse>.<property>}. */
        public String javaName() { return Namen.ident(type.name, true) + "." + displacedMember.property(); }
    }

    /** Every {@link Displacement} of the model, once per member that lost its property: on the type whose bean
     *  would have declared it, not again on each subtype that inherits the loss. A member reached twice from one
     *  declaration is not one. */
    public static List<Displacement> displacements(RegelSpraakModel model) {
        List<Displacement> out = new ArrayList<>();
        for (ObjectType ot : model.objectTypes.values()) {
            Map<String, Candidate> owner = new java.util.HashMap<>();
            List<ObjectType> chain = ot.chain();
            for (int i = chain.size() - 1; i >= 1; i--)                   // the ancestors, root first
                for (Candidate k : candidates(model, chain.get(i))) owner.putIfAbsent(k.member().property(), k);
            for (Candidate k : candidates(model, ot)) {
                Candidate earlier = owner.putIfAbsent(k.member().property(), k);
                if (earlier != null && !earlier.origin().equals(k.origin()))
                    out.add(new Displacement(ot, k.origin(), k.member(), earlier.origin(), earlier.member()));
            }
        }
        return out;
    }

    /** Why {@code v}'s member has no field of its own, in the one sentence both branches and the case reader
     *  refuse a use of it with; the caller appends who uses it. */
    public static Reason displacementReason(Displacement v) {
        return DroolsCode.BEAN_PROPERTY_DISPLACED.reason("displaced", v.displaced().description(), "property", v.javaName(), "owner", v.owner().description());
    }

    /** {@link #displacementReason} for the member a case names on an instance of {@code ot} — an attribute, or with
     *  {@code kenmerk} a characteristic, resolved on {@code ot}'s chain under the verifier's name identity — or
     *  null when that member has a field of its own (or {@code ot} declares none by that name). */
    public static Reason displacedMember(List<Displacement> displacements, ObjectType ot, String name, boolean characteristic) {
        String key = Namen.norm(name);
        for (ObjectType t : ot.chain())
            for (String member : characteristic ? t.characteristics : t.attributes.keySet())
                if (Namen.norm(member).equals(key)) {
                    Origin h = new Origin(characteristic ? Origin.Kind.KENMERK : Origin.Kind.ATTRIBUUT, member, t.name);
                    for (Displacement v : displacements) if (v.displaced().equals(h)) return displacementReason(v);
                    return null;
                }
        return null;
    }

    /** Two ROLES on one reference field of one type, as a refusal, or null. A field of another type is left to the
     *  use sites, whose gate asks name and type ({@link #hasReferenceField}) and refuses by name; one of the same
     *  type passes that gate for both roles, and every reader and writer of either then works one relation. */
    public static Reason roleCollision(List<Displacement> displacements) {
        for (Displacement v : displacements)
            if (v.displaced().kind() == Origin.Kind.ROL && v.owner().kind() == Origin.Kind.ROL
                    && v.displacedMember().javaType().equals(v.ownerMember().javaType()))
                return DroolsCode.BEAN_REFERENCE_FIELD_DISPLACED.reason("displaced", v.displaced().description(), "property", v.javaName(), "owner", v.owner().description());
        return null;
    }

    /** The classes the generator writes into {@link #PACKAGE} itself, with what each is for. */
    private static final Map<String, Msg> HELPER_CLASSES =
            Map.of("Segment", Msg.drools("PART.helper-class.segment"), "Inconsistentie", Msg.drools("PART.helper-class.inconsistency"));

    /** Why the object types of {@code model} do not each get a fact class of their own, or null when they do:
     *  two types on one class name ({@link Namen#ident} is not injective, while WF-001 compares the names
     *  case-insensitively and keeps them apart), a type on the name of a class the generator writes itself, or a
     *  type on the name of a public {@code java.lang} type, which the fact class then hides from the generated beans
     *  (their own package comes first) and from the DRL text (which imports every fact class by name). */
    public static Reason classCollision(RegelSpraakModel model) {
        Map<String, ObjectType> classes = new java.util.HashMap<>();
        for (ObjectType ot : model.objectTypes.values()) {
            String javaClass = Namen.ident(ot.name, true), fqn = PACKAGE + "." + javaClass;
            ObjectType earlier = classes.putIfAbsent(javaClass, ot);
            if (earlier != null)
                return DroolsCode.BEAN_CLASS_COLLISION_OBJECTTYPES.reason("earlierObjectType", earlier.name, "objectType", ot.name, "className", fqn);
            if (HELPER_CLASSES.containsKey(javaClass))
                return DroolsCode.BEAN_CLASS_HELPER_COLLISION.reason("objectType", ot.name, "className", fqn, "helper", HELPER_CLASSES.get(javaClass));
            if (javaLang(javaClass))
                return DroolsCode.BEAN_HIDES_JAVA_LANG.reason("objectType", ot.name, "className", fqn, "simpleName", javaClass);
        }
        return null;
    }

    /** Whether {@code java.lang} has a public type of this simple name — the names every Java source sees
     *  without an import. */
    private static boolean javaLang(String javaClass) {
        try {
            Class<?> c = Class.forName("java.lang." + javaClass, false, ClassLoader.getPlatformClassLoader());
            return java.lang.reflect.Modifier.isPublic(c.getModifiers());
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * §13.3.9 — the scalar to-one reference fields an ORDINARY (non-wederkerig) SELF-TYPED feittype implies on
     * {@code ot}: two DISTINCT sibling roles both filled by {@code ot} ({@code voorganger} and {@code opvolger}
     * on {@code Wachter}). The by-pair loop in {@link #members} cannot reach them (it skips {@code ot} itself) and
     * {@link #selfPairRole} does not cover them (it is the one-role wederkerig shape).
     *
     * <p><b>The decision (card drools-zelfgetypeerd-creatiepaar):</b> the bean carries ONE SCALAR FIELD PER
     * TO-ONE ROLE, named after the role, exactly as the by-pair loop names a reference after the role of the
     * thing it POINTS AT — {@code Wachter.opvolger} is the Wachter that plays {@code opvolger} (the successor),
     * {@code Wachter.voorganger} the one that plays {@code voorganger}. Derived through the SAME authority the
     * transpilers navigate by, here its BY-NAME form ({@code connectingRoleViaName} + {@code toManyRole}), so a
     * role name that does not resolve UNIQUELY derives nothing and a to-many role derives nothing (its storage is
     * the other end's field). {@code connectingRole(ot, ot)} — the BY-PAIR search — is deliberately left as it
     * is: still null, still ambiguous. So these fields exist but nothing reaches them by accident; only a WRITTEN
     * role name or the creation's symbolic {@code connectingRole} wire binding does. Under-implied, never
     * over-implied — the same law the by-pair comment above states.
     *
     * <p>Alternatives considered and REJECTED:
     * <ul>
     *   <li><b>One field named after the FEITTYPE</b> ({@code Wachter.opvolging}): cannot represent both
     *       directions at once (one Wachter is simultaneously someone's opvolger and someone's voorganger), and
     *       the transpilers navigate by ROLE name, so such a field would be symbolically unreachable.</li>
     *   <li><b>One field for the FIRST role only</b>: arbitrary and wire-order dependent, and it silently drops
     *       one navigation direction — over-implied in one direction, under-implied in the other.</li>
     *   <li><b>Teaching {@code connectingRole(ot, ot)} to pick a winner</b> (prefer the to-one / the first role):
     *       that authority is SHARED by every navigated read, feitcreatie, verdeling and by
     *       {@link ContractValidator}; making it guess would turn an honest ambiguity into a silent wrong join
     *       across the whole back-end — precisely what RUNTIME-SEMANTICS ch. 5.5 forbids and what the reference
     *       interpreter refuses ({@code connecting_pair} BLOCKS a self-typed two-role fact type and takes the
     *       explicit binding instead).</li>
     *   <li><b>A collection-valued field per self-pair</b>: a to-one role's storage IS a scalar; a collection
     *       would misreport cardinality to every {@code toManyRole} consumer and to the §9 creation idempotence
     *       guard, which keys a 1:1 target on that very field being empty.</li>
     * </ul>
     */
    public static List<String> selfPairRoles(RegelSpraakModel model, ObjectType ot) {
        List<String> out = new ArrayList<>();
        for (FactType ft : model.factTypes.values()) {
            if (ft.reciprocal) continue;                       // the one-role shape: selfPairRole owns it
            for (Role r : ft.roles) {
                if (!ot.name.equalsIgnoreCase(r.objectTypeName())) continue;
                // the by-NAME authority: null when the name does not resolve uniquely, or when the feittype does
                // not relate ot to ot at all (a role of ot in a feittype whose sibling is another type is the
                // by-pair loop's business, and is reached from THAT side).
                String role = model.connectingRoleViaName(ot, ot, r.name());
                if (role == null || model.toManyRole(ot, ot, role)) continue;
                if (!out.contains(role)) out.add(role);
            }
        }
        return out;
    }

    /**
     * §13.3.9 — whether {@code ot}'s bean really carries the SCALAR REFERENCE field for role {@code role} pointing
     * at {@code targetName}: the property name AND its type, asked of {@link #members}, the ONE derivation this generator
     * renders and {@link ContractValidator} demands.
     *
     * <p>The TYPE half is the whole point. {@code members} ports every member through a name-dedup — attribuut &gt;
     * kenmerk &gt; referentie, because a bean can carry only one field per name — so a declared member that shares
     * the role's name DISPLACES the reference: the property still exists, but it holds a {@code BigDecimal} or a
     * {@code boolean}. A name-only test therefore reads "field present" and sends a FACT into it, which is an
     * unclassified ERROR at fire ({@code ClassCastException} on the creation arm, "kon setter set&lt;X&gt;(…) niet
     * aanroepen" on the case-input arm — lens 2, 2026-08-30), where the honest outcome is a loud per-form refusal.
     *
     * <p>The type test is ASSIGNABILITY, not equality. A role's declared filler is an upper bound (IR-SPEC ch. 2.6),
     * the field is of the filler's class ({@link #fillerType}) and a subtype's bean extends it, so the field of a role
     * filled by {@code Vondst} holds a {@code Wrakvondst}. Asked as equality, every reader and writer of that link
     * refused a document ref and clingo play: the case input, the navigation, the fold and quantifier joins, the
     * allocation's recipient join, the far-side role check and the object and fact creations. A strict ancestor's class is accepted only for the
     * field of the very role declaration that {@code ot}, {@code role} and {@code targetName} resolve to: a field of the same
     * name that ANOTHER role took keeps refusing, even when {@code target} fits its class, since writing through it
     * would join one relation through another's storage.
     */
    public static boolean hasReferenceField(RegelSpraakModel model, ObjectType ot, String role, ObjectType targetName) {
        if (model == null || ot == null || role == null || targetName == null) return false;
        // the bean CARRIES a field it inherits: `members` is own-members-only since the generated
        // subtype bean extends its supertype's, so the question is asked over the whole chain
        Candidate field = field(model, ot, Namen.ident(role, false));
        if (field == null) return false;
        String type = field.member().javaType();
        if (type.equals(Namen.ident(targetName.name, true))) return true;
        boolean ancestor = false;
        for (ObjectType a : targetName.chain()) ancestor |= a != targetName && type.equals(Namen.ident(a.name, true));
        Origin origin = field.origin();
        return ancestor && origin.kind() == Origin.Kind.ROL && origin.source() != null
                && origin.equals(roleOrigin(model, ot, targetName, role));
    }

    /** The declaration whose member the bean of {@code ot} carries under the property of {@code role}, its own or an
     *  inherited one; null when no bean of the chain has that property. Where {@link #hasReferenceField} refuses a
     *  role, this names what holds the property instead, so a refusal can state the cause rather than guess it. */
    public static Origin fieldOrigin(RegelSpraakModel model, ObjectType ot, String role) {
        Candidate field = model == null || ot == null || role == null ? null : field(model, ot, Namen.ident(role, false));
        return field == null ? null : field.origin();
    }

    /**
     * WHY {@link #hasReferenceField} said no — the precise §13.3.9 reason, so a per-form refusal names the
     * bean-model limit instead of a generic scope note. The two causes are DIFFERENT and are named apart: a
     * declared member of the same name DISPLACED the reference ({@link #members}'s dedup), or the model derives
     * no unique field for that role at all.
     *
     * <p>It lives HERE, beside the derivation it explains, because BOTH transpiler branches and every reader
     * refuse the same shapes and must say so in the same words — a producer reading the gate report gets one
     * diagnosis whichever branch produced it. Callers prefix the READER ("genavigeerde lezing",
     * "rollidmaatschapscheck", "objectcreatie", the fold join) and, where it is not already in their sentence,
     * the §13.3.9 pointer; the body is this one string.
     *
     * <p>The second cause states no CARDINALITY (2026-09-11, card drools-ongepoorte-rollezers): not every reader
     * checks {@code toManyRole} before asking — the verdeling's recipient join, the far-side rolcheck, the temporal
     * set fold and the feitcreatie's three positions do NOT — and a to-many role is exactly the shape they meet: measured on a
     * verifier-clean, ref-clean model the verdeling's join over a to-many recipient role died at first fire.
     * Naming it "enkelvoudig" there would assert of the document the very thing that is false about it.
     *
     * <p>A third cause is named apart (2026-09-28): the property EXISTS and another declaration holds it — a
     * member under another spelling ({@link Namen#ident} is not injective) or the field of another role, the shape
     * two same-named roles in two fact types give when one filler is a subtype of the other. "The bean model
     * derives no reference field" is true there and names no cause, so the sentence names the holder.
     */
    public static Reason noReferenceFieldReason(RegelSpraakModel model, ObjectType ot, String role, ObjectType targetName) {
        if (ot.hasMember(role))
            return DroolsCode.ROLE_NAME_SHARES_MEMBER.reason("role", role, "objectType", ot.name, "targetType", targetName.name);
        Candidate field = field(model, ot, Namen.ident(role, false));
        Origin holder = field == null ? null : field.origin();
        if (holder != null && !holder.equals(roleOrigin(model, ot, targetName, role)))
            return DroolsCode.ROLE_FIELD_HELD_OTHER.reason("role", role, "objectType", ot.name, "targetType", targetName.name, "beanClass", Namen.ident(ot.name, true), "property", field.member().property(), "holder", holder.description());
        return DroolsCode.ROLE_NO_REFERENCE_FIELD.reason("role", role, "objectType", ot.name, "targetType", targetName.name);
    }

    /**
     * WHY a FOLD's JOIN has no bean field — {@link #noReferenceFieldReason} plus the one diagnosis that reading
     * cannot reach: the role is TO-MANY, so its storage is the far end's field, and the far end derives none
     * either. That is a statement about CARDINALITY, not about a displaced or underivable name, and a fold that
     * reported it as "the bean model derives no reference field" would blame the document for an ambiguity it
     * does not have (the erflijn shape: `erfvolger` resolves perfectly; what stops it is that BOTH sides are
     * to-many, so neither bean can hold the relation).
     *
     * <p>It lives HERE, beside {@link #noReferenceFieldReason}, for that function's own reason: both transpiler
     * branches refuse the same shapes and must say so in the same words. Callers prefix their own reader name.
     */
    public static Reason noJoinFieldReason(RegelSpraakModel model, ObjectType carrier, String role, ObjectType targetName) {
        if (!model.toManyRole(carrier, targetName, role)) return noReferenceFieldReason(model, carrier, role, targetName);
        // The far end is where a to-many role's storage lives. Whether it HAS a field there is a second
        // question, and the answer decides how much this sentence may claim: "both sides to-many" is the
        // erflijn shape, where neither bean can hold the relation, and asserting it where the far side does
        // derive a field (the gemeenteberging shape: a to-one sibling, whose field exists and is simply on the
        // other bean) would diagnose a cardinality defect the document does not have. The exec branch never
        // reached that case — its own conservative arm refuses a to-one element role with a to-many sister
        // first — but the DRL-text branch does not mirror that arm and arrives here.
        ObjectType far = model.objectTypeByName(targetName.name);
        String sister = model.connectingRoleViaElementName(carrier, role);
        boolean otherSideCarries = sister != null && far != null && hasReferenceField(model, far, sister, carrier);
        Msg sentence = Msg.drools("PART.role-multi-valued", "role", role, "carrier", carrier.name);
        if (otherSideCarries) return DroolsCode.ROLE_MULTI_FAR_SIDE.reason("sentence", sentence);
        // A TO-ONE sister whose field is missing is no cardinality defect: its field was taken, by a member or by
        // another role, and saying "both sides to-many" there names a cause the document does not have.
        if (sister != null && far != null && !model.toManyRole(far, carrier, sister))
            return DroolsCode.SISTER_FIELD_MISSING.reason("sentence", sentence, "reason", noReferenceFieldReason(model, far, sister, carrier).msg());
        return DroolsCode.BOTH_SIDES_MULTI_VALUED.reason("sentence", sentence);
    }

    /**
     * IR-SPEC ch. 4.3 — the role PAIR a symbolic {@code (feittype, rol)} wire binding names:
     * {@code [gebonden, zuster]}, the role the wire names (the folded element's / the created object's) and the
     * one the ANCHOR plays. The sibling's NAME is the reference field on the bound end, and vice versa, which is
     * exactly how {@link #members} names them. A §13.3.9.2 wederkerig fact type carries ONE role that is its own
     * sibling. null when the binding names no such fact type / role, or when a non-wederkerig fact type has no
     * sibling role at all — for those RESOLUTION failures the caller refuses loudly, never guesses. Mirrors
     * {@code ref/lib/eval.ml}'s {@code bound_role} + {@code resolve_binding}, whose Error flank is a fault there.
     *
     * <p>Resolution goes through the fact type the wire NAMES, not through the role name alone: two fact types
     * may declare an element role of the same name while their anchor roles differ, and the binding then still
     * selects exactly one. That is also why this is not {@code connectingRoleViaElementName} with the bound
     * role's name — that search is by NAME over the whole model and answers null on such a document, which would
     * refuse where the wire is unambiguous.
     *
     * <p>It lives HERE for {@link #noJoinFieldReason}'s reason, one step on: a second implementation is a second
     * chance to drift, and the drift is invisible — two readers that resolve the same wire differently produce two
     * different SETS, not an error. One derivation for every reader, as {@code StandingTargets} is for the ch. 7.8
     * standing norm.
     *
     * <p><b>What this does NOT check, and why.</b> The FILLERS. A binding may resolve perfectly and still be
     * wrong for its use site — the bound role filled by the anchor's type instead of the created object's — and
     * this method hands that pair back without a word. That is division of labour, not oversight: <b>WF-047 is
     * the fence</b> ("role binding resolves; fillers agree with the use site"), checked by gate 3 with its own
     * negative cases, {@code wf047-bound-role-fills-source} and {@code wf047-creation-binding-wrong-filler}, so
     * no corpus artefact reaches this method mis-filled. Measured on an {@code anchor/jutterswet-rekenkamer}
     * variant whose creation binding names the SIBLING role: the verifier FAILS it twice on WF-047, while ref
     * does not fault at all — it takes the wrong binding silently and reads a MISMATCH. A duplicate filler check
     * HERE is rejected: it would need the expected filler pair at each call site (creation, fold
     * source, the quantifier fold's contributors), i.e. a second fence that can drift from gate 3's. What the
     * callers DO carry is the typed reference-field gate ({@link #hasReferenceField}), which catches the usual
     * downstream consequence — but that is a bean-model question, not a filler check, and does not claim to be.
     */
    public static Role[] boundRolePair(RegelSpraakModel model, RegelSpraakModel.RoleBindingRef rb) {
        if (model == null || rb == null || rb.factType() == null || rb.role() == null) return null;
        FactType ft = null;
        // the WF-001 form of a name, as the verifier and ref compare it (case and runs of blanks folded)
        String factType = Namen.norm(rb.factType()), role = Namen.norm(rb.role());
        for (Map.Entry<String, FactType> e : model.factTypes.entrySet())
            if (Namen.norm(e.getKey()).equals(factType)) { ft = e.getValue(); break; }
        if (ft == null) return null;
        Role bound = null;
        for (Role r : ft.roles)
            if (r.name() != null && Namen.norm(r.name()).equals(role)) { bound = r; break; }
        if (bound == null) return null;
        if (ft.reciprocal) return new Role[]{bound, bound};
        for (Role r : ft.roles)
            if (r != bound) return new Role[]{bound, r};
        return null;
    }

    /**
     * What {@link #creationLink} decided for one object creation. {@code fwdRole} is the role the created object
     * plays and {@code backRole} the role the subject plays, each with whether it is to-many; {@code fwdRef} is the
     * reference field on the subject that holds the created object and {@code backRef} the one on the created object
     * that holds the subject, each null where that end has no field of its own (a to-many role is stored as the
     * other end's field). {@code oneToOne} is the 1:1 shape, whose idempotence guard reads {@code fwdRef}. Where the
     * creation cannot be lowered, {@code refusal} says why (a backend gap) or {@code ambiguous} names the
     * ambiguous back-link (a fault of the program, RUNTIME-SEMANTICS ch. 5.5), and the rest is not set.
     */
    public record CreationLink(String fwdRole, boolean fwdMany, String backRole, boolean backMany, String fwdRef,
                                   String backRef, boolean oneToOne, Reason refusal, Reason ambiguous) {
        static CreationLink refuse(Reason reason) {
            return new CreationLink(null, false, null, false, null, null, false, reason, null);
        }
    }

    /**
     * THE back-link decision of an object creation (RUNTIME-SEMANTICS ch. 5.5, IR-SPEC ch. 3.3), one derivation,
     * which the analysis ({@code TmsTranspiler}) carries out as a refusal or a fault fact, on both branches alike.
     *
     * <p>The two link roles come from ONE of two routes. The creation's own {@code connectingRole} binding names the
     * role the created object plays, its sibling the one the subject plays; it is authoritative, as the reference
     * interpreter reads it, and for a self-typed pair it is the only route there is. Without it, the by-pair
     * derivation. Only a to-one role has a bean field, and only a TYPED one is writable: a declared member of the same
     * name displaces the reference, which is refused by name rather than leaving the link one-sided. Where neither end
     * has a field, the mint would be reachable from nothing: an ambiguous pair is ch. 5.5's refusal to guess
     * ({@code ambiguous}), a self-typed pair without a binding, a pair without a unique connecting fact type and a
     * pair of two to-many roles are backend gaps, named apart. {@code name} names the creation in the exception of a
     * broken invariant: the 1:1 shape must have the forward field its guard reads.
     */
    public static CreationLink creationLink(RegelSpraakModel model, ObjectType fresh, ObjectType subject,
                                                    RegelSpraakModel.RoleBindingRef connectingRole, String name) {
        String backRole, fwdRole;
        boolean backMany, fwdMany;
        if (connectingRole != null) {
            Role[] pair = boundRolePair(model, connectingRole);
            if (pair == null)
                return CreationLink.refuse(DroolsCode.CONNECTING_ROLE_NO_PAIR.reason("factType", connectingRole.factType(), "role", connectingRole.role()));
            fwdRole = pair[0].name(); fwdMany = pair[0].many();
            backRole = pair[1].name(); backMany = pair[1].many();
        } else {
            backRole = model.connectingRole(fresh, subject);
            fwdRole = model.connectingRole(subject, fresh);
            backMany = backRole != null && model.toManyRole(fresh, subject, backRole);
            fwdMany = fwdRole != null && model.toManyRole(subject, fresh, fwdRole);
        }
        String backRef = backRole != null && !backMany ? backRole : null;
        String fwdRef = fwdRole != null && !fwdMany ? fwdRole : null;
        if (backRef != null && !hasReferenceField(model, fresh, backRef, subject))
            return CreationLink.refuse(noReferenceFieldReason(model, fresh, backRef, subject));
        if (fwdRef != null && !hasReferenceField(model, subject, fwdRef, fresh))
            return CreationLink.refuse(noReferenceFieldReason(model, subject, fwdRef, fresh));
        if (backRef == null && fwdRef == null) {
            if (connectingRole == null && subject != fresh && connectingFactTypes(model, subject, fresh) > 1)
                return new CreationLink(null, false, null, false, null, null, false, null,
                        DroolsCode.CREATION_BACKLINK_AMBIGUOUS.reason("subjectType", subject.name, "createdType", fresh.name));
            return CreationLink.refuse(DroolsCode.CREATION_BACKLINK_UNREACHABLE.reason("sentence", DroolsCode.CREATION_BACKLINK_UNREACHABLE.part(
                    connectingRole == null && subject == fresh ? "self-typed"
                            : connectingRole == null && (backRole == null || fwdRole == null) ? "no-fact-type" : "both-many",
                    "backRole", backRole, "fwdRole", fwdRole)));
        }
        // with a binding the cardinality is the bound role's own, without one the by-pair reading; either way a 1:1
        // arm must HAVE the forward field its guard reads
        boolean oneToOne = connectingRole != null ? !fwdMany : !model.subjectToManyCreation(subject, fresh);
        if (oneToOne != (fwdRef != null))
            throw new IllegalStateException("objectcreatie " + name + ": subjectNaarVeelCreatie(" + oneToOne
                    + ") en het voorwaartse veld (" + fwdRef + ") spreken elkaar tegen");
        return new CreationLink(fwdRole, fwdMany, backRole, backMany, fwdRef, backRef, oneToOne, null, null);
    }

    /** How many fact types RELATE {@code van} and {@code destination} in ch. 5.5's sense, the count the reference interpreter's
     *  {@code connecting_pair} takes: a two-role fact type whose two roles the two types fill, in either order, and, for
     *  one type, a §13.3.9.2 wederkerig one-role fact type. 0 is the linkless shape, 1 derivable, more than one
     *  ch. 5.5's ambiguity. Deliberately not {@code connectingRole}, which collapses 0 and more than one into null. */
    public static int connectingFactTypes(RegelSpraakModel model, ObjectType van, ObjectType destination) {
        if (van == null || destination == null) return 0;
        int n = 0;
        for (FactType ft : model.factTypes.values()) {
            List<Role> r = ft.roles;
            if (r.size() == 2) {
                boolean ab = van.name.equalsIgnoreCase(r.get(0).objectTypeName())
                        && destination.name.equalsIgnoreCase(r.get(1).objectTypeName());
                boolean ba = van.name.equalsIgnoreCase(r.get(1).objectTypeName())
                        && destination.name.equalsIgnoreCase(r.get(0).objectTypeName());
                if (ab || ba) n++;
            } else if (r.size() == 1 && ft.reciprocal && van == destination
                    && van.name.equalsIgnoreCase(r.get(0).objectTypeName())) {
                n++;
            }
        }
        return n;
    }

    /** What {@link #rangeJoin} decided: the scalar join ROLE the wire binding yields, or the REASON it yields
     *  none. Exactly one of the two is non-null; both null means the wire named no range at all, and the caller's
     *  own generic refusal stands. */
    public record RangeJoin(String role, Msg refusal) {}

    /**
     * THE fold RANGE a ch. 4.3 wire binding yields — the one derivation both transpiler branches read at every
     * seam where a fold resolves its own set, and the one SENTENCE they both refuse with.
     *
     * <p>The role handed back is the SIBLING's name: the binding names the role the ELEMENT plays, and the
     * sibling — the role the anchor plays — is the reference field ON the element, which is the side the join
     * reads. The binding is the only range the wire carries, so it is the route for every pair: a unique
     * one, a REPEATED one (where a search by the type pair is ambiguous by construction) and a SELF-TYPED one.
     *
     * <p>A TO-MANY sibling is the one arm with a shared sentence. Its storage is the far end's field (§13.3.9), so
     * the element carries no scalar reference to join on — a resolution success with a cardinality refusal behind
     * it, which every reader names in one sentence, for {@link #noJoinFieldReason}'s reason: a producer reading the
     * gate report must not have to know which branch answered.
     */
    public static RangeJoin rangeJoin(RegelSpraakModel model, RegelSpraakModel.RoleBindingRef rb) {
        Role[] pair = boundRolePair(model, rb);
        if (pair == null) return new RangeJoin(null, null);        // no range on the wire -> the caller's own refusal
        if (!pair[1].many()) return new RangeJoin(pair[1].name(), null);
        return new RangeJoin(null, Msg.drools("PART.range-join-multi-valued", "role", pair[0].name(), "sister", pair[1].name(),
                "type", pair[0].objectTypeName()));
    }

    /** The walk a navigated read's wire bindings name: per hop the reached role (hence the bean field on the type
     *  the walk stands on) and the type that role's filler declares; {@code toMany} when any hop reaches a
     *  to-many role. */
    public record NavigationWalk(List<String> roles, List<ObjectType> reaches, boolean toMany) {}

    /**
     * THE navigation walk of an {@code AttributeReference.navigationBindings} / {@code Characteristic.navigationBinding}
     * (IR-SPEC ch. 4.1a), resolved from {@code subject} — the one derivation both transpiler branches decide a
     * bound navigation by (V1-WIREMIGRATIE P1); the bindings are the only navigation the wire carries.
     *
     * <p>Null when the walk does not resolve from {@code subject}: a binding naming no declared role, a hop whose
     * opposite role the type the walk stands on does not fill (ch. 2.6, extensionally), or — with a non-null
     * {@code endpoint} — a last hop whose filler is not the endpoint or a subtype of it (filler ⊑ endpoint, the
     * direction WF-053 and ref demand: every object the walk can reach must be one the read may address). The caller refuses such a read
     * with its own unresolved-navigation reason. It never falls back to the written name: a document whose
     * binding does not fit is refused, not read along a route the wire did not name. The same subject-fit the
     * written-chain derivation ({@link RegelSpraakModel#followChain}) applies, so a read the transpiler meets
     * under another subject than the one it was bound to is refused, as on the written chain.
     *
     * <p>No cross-check against the written step: WF-053 already demands that every written step denote its
     * hop's bound role (gate 3), the written fields disappear at B11, and a cross-check here would turn a
     * binding that decides into one that merely vetoes.
     */
    public static NavigationWalk navigationWalk(RegelSpraakModel model, ObjectType subject,
            List<RegelSpraakModel.RoleBindingRef> bindings, ObjectType endpoint) {
        if (bindings == null || bindings.isEmpty()) return null;
        List<String> roles = new ArrayList<>();
        List<ObjectType> reaches = new ArrayList<>();
        boolean toMany = false;
        ObjectType cur = subject;
        for (RegelSpraakModel.RoleBindingRef rb : bindings) {
            Role[] pair = boundRolePair(model, rb);
            if (pair == null || cur == null) return null;
            ObjectType near = model.objectTypeByName(pair[1].objectTypeName());
            ObjectType far = model.objectTypeByName(pair[0].objectTypeName());
            if (near == null || far == null || !cur.isA(near)) return null;
            toMany |= pair[0].many();
            roles.add(pair[0].name());
            reaches.add(far);
            cur = far;
        }
        if (endpoint != null && !cur.isA(endpoint)) return null;
        return new NavigationWalk(List.copyOf(roles), List.copyOf(reaches), toMany);
    }

    /** One §9.4 creation hop as the bean model stores it: the type the hop reaches, the reference field, and
     *  whether that field sits on the PARENT (the type the hop starts from, joined {@code this == $prev.<field>}) or
     *  on the reached CHILD (joined {@code <field> == $prev}). */
    public record CreationHop(ObjectType reaches, String beanField, boolean onParent) {}

    /** The bean fields a §13.4.7 creation executes on, derived from its symbolic form: the field it WRITES on the
     *  doel, the field the doel JOINS on, and the hops in between. */
    public record CreationFields(String writeField, String linkField, List<CreationHop> hops) {}

    /**
     * THE bean fields of a fact creation, derived from its role bindings (IR-SPEC ch. 3.4: "derivable from the role
     * bindings by the backend's own role→field mapping") — the one derivation both transpiler branches decide a
     * creation by. The bindings are all the wire carries of a creation.
     *
     * <p>The mapping is {@link #members}'s own: a bean carries a reference field named after the role the OTHER end
     * plays. The write lands on the doel, pointing at the anker, so it is {@code anchorRole}. The join and each hop
     * bind the role the REACHED object plays, so the field on the reached object is the sibling role and the field
     * on the starting object is the bound role itself. Which of the two a hop reads follows the storage direction
     * (§13.3.9): the bound role to-one puts the field on the parent, else a to-one sibling puts it on the child.
     * At 1:1 both beans carry a field and case input writes both (RUNTIME-SEMANTICS ch. 5.7a); the parent side is
     * taken, the side the frontend picks for an objectcreatie-placed link. Where both roles are to-many there is
     * no field at all and the child side is handed back, so the caller's {@link #hasReferenceField} gate refuses
     * it with its usual reason. The join is read on the doel only, as before.
     *
     * <p>Null when a binding names no declared role; the caller refuses the creation as unresolved.
     */
    public static CreationFields creationFields(RegelSpraakModel model,
            dev.uchar.regelspraak.ir.ResolvedFactCreation fc) {
        if (fc.factType() == null || fc.anchorRole() == null || fc.path() == null) return null;
        Role[] made = boundRolePair(model, new RegelSpraakModel.RoleBindingRef(fc.factType(), fc.anchorRole()));
        Role[] join = boundRolePair(model, fc.path());
        if (made == null || join == null) return null;
        List<CreationHop> hops = new ArrayList<>();
        for (var step : fc.pathSteps()) {
            Role[] pair = boundRolePair(model, step.roleBinding());
            if (pair == null) return null;
            ObjectType reaches = model.objectTypeByName(pair[0].objectTypeName());
            if (reaches == null) return null;
            hops.add(!pair[0].many() ? new CreationHop(reaches, pair[0].name(), true)
                                           : new CreationHop(reaches, pair[1].name(), false));
        }
        return new CreationFields(made[0].name(), join[1].name(), List.copyOf(hops));
    }

    /**
     * THE scope field of a parent-scoped uniqueness (ch. 5.9), derived from {@code scopeRole} — the role the scope
     * parent plays, hence the reference field on the subject. IR-SPEC ch. 3.6: the binding IS the scoping, so null
     * means an unscoped uniqueness, or a binding that names no declared role (the caller refuses).
     */
    public static String scopeField(RegelSpraakModel model, dev.uchar.regelspraak.ir.ResolvedUniqueness u) {
        if (u.scopeBinding() == null) return null;
        Role[] pair = boundRolePair(model, u.scopeBinding());
        return pair == null ? null : pair[0].name();
    }

    /** Where the link a role check asks about is stored: the reference {@code beanField}, whether it sits on the SUBJECT
     *  ({@code opSubject}: the subject's field names the other end) or on the other end (its field names the
     *  subject), and the other role's declared filler {@code ander}; or the {@code refusal} when no field holds
     *  it. */
    public record MembershipStorage(String beanField, boolean opSubject, ObjectType otherType, Msg refusal) {}

    /**
     * IR-SPEC ch. 4.5 {@code RoleMembership}: "a link of the fact type exists in which the subject plays the role".
     * The wire names the fact type and the role; where the link is STORED is this backend's question, answered by
     * the member derivation {@link #members} renders (§13.3.9): a to-one OTHER role is a reference field on the
     * subject, named after that role, and the check is its null-test; a to-many other role puts the storage on the
     * other end, whose field — named after the subject's role — must point back at the subject, which is an
     * exists-join; with both roles to-many no bean carries the link at all.
     */
    public static MembershipStorage membershipStorage(RegelSpraakModel model, RegelSpraakModel.RoleBindingRef rb) {
        Role[] pair = boundRolePair(model, rb);
        if (pair == null)
            return new MembershipStorage(null, false, null, Msg.drools("PART.membership-unresolved", "binding",
                    rb == null ? Msg.drools("PART.none") : rb.factType() + ", " + rb.role()));
        ObjectType otherType = model.objectTypeByName(pair[1].objectTypeName());
        if (!pair[1].many()) return new MembershipStorage(pair[1].name(), true, otherType, null);
        if (!pair[0].many()) return new MembershipStorage(pair[0].name(), false, otherType, null);
        return new MembershipStorage(null, false, otherType, Msg.drools("PART.membership-both-multi-valued",
                "factType", rb.factType(), "role", rb.role()));
    }

    /** What {@link #allocationJoin} decided: the join ROLE, or the REASON there is none. Both null means there is
     *  no binding to read, and the caller's own "onopgelost" refusal stands. */
    public record AllocationJoin(String role, Msg refusal) {}

    /**
     * THE recipient join of a §9.7 verdeling — the one derivation both transpiler branches decide it by. The wire's
     * {@code connectingRole} binds the role the RECIPIENT plays (IR-SPEC ch. 3.2), so the join field on the
     * recipient is its sibling, the role the source plays; for a wederkerig fact type that is the bound role
     * itself. The binding is required on the wire, so no pair is asked: on a REPEATED pair — two fact types over
     * the same two types — the pair has no answer at all ({@code anchor/jutterswet-bergingstoezicht}). A binding
     * that names no declared role is refused by name, where ref faults; WF-047 is the fence on its fillers.
     */
    public static AllocationJoin allocationJoin(RegelSpraakModel model, dev.uchar.regelspraak.ir.ResolvedAllocation v,
            RegelSpraakModel.RoleBindingRef rb) {
        ObjectType source = v.sourceType(), targetName = v.targetType();
        if (source == null || targetName == null || rb == null) return new AllocationJoin(null, null);
        Role[] pair = boundRolePair(model, rb);
        if (pair == null)
            return new AllocationJoin(null, Msg.drools("PART.allocation-join-unresolved", "factType", rb.factType(), "role", rb.role()));
        return new AllocationJoin(pair[1].name(), null);
    }

    /**
     * §13.3.9.2 — the scalar to-one SELF-PAIR role a wederkerig feittype implies on {@code ot} (the role name,
     * hence the bean field name), or null when the model implies none.
     *
     * <p>A wederkerig feittype carries a single role that is ITS OWN OPPOSITE (WF: exactly one role), so the
     * pair (ot, ot) resolves through the very same authority the transpilers navigate by —
     * {@code connectingRole}, whose {@code factTypeRelates} admits the reciprocal role as its own sibling.
     * That makes the field name knowable SYMBOLICALLY, from the role alone. An ordinary (non-wederkerig)
     * self-pair stays underived: there two distinct sibling roles both fill the holder, so the by-pair search
     * is ambiguous by construction and the transpilers reach it only through the written role name.
     *
     * <p>The wederkerig guard is belt-and-braces: {@code connectingRole}'s own uniqueness already yields null
     * for a non-wederkerig self-pair (two sibling roles), so the guard adds no reachable behaviour — it only
     * states the shape this derivation claims. A to-many reciprocal role derives nothing either: that read is
     * an aggregation, not a scalar field.
     *
     * <p>Null with {@link #namesReciprocal} true is the UNDERIVABLE half of the shape (an ambiguous pair —
     * two wederkerig feittypen on one objecttype — or a to-many role): callers must refuse it loudly, because
     * the transpilers then navigate by the written role name toward a getter no bean carries.
     */
    public static String selfPairRole(RegelSpraakModel model, ObjectType ot) {
        if (!namesReciprocal(model, ot)) return null;
        String role = model.connectingRole(ot, ot);
        return role == null || model.toManyRole(ot, ot, role) ? null : role;
    }

    /** Whether any wederkerig feittype names {@code ot} — the §13.3.9.2 shape itself, field-derivable or not. */
    public static boolean namesReciprocal(RegelSpraakModel model, ObjectType ot) {
        return model.factTypes.values().stream().anyMatch(ft -> ft.reciprocal
                && ft.roles.stream().anyMatch(r -> ot.name.equalsIgnoreCase(r.objectTypeName())));
    }

    /** The scalar Java type of a datatype — the same mapping the DRL back-end assumes of the beans. */
    public static String javaType(Datatype t) {
        if (t.isPercentage() || t.isNumber()) return "java.math.BigDecimal";
        if (t.isText() || t instanceof Datatype.Enumeration) return "String";   // an enumeratiewaarde is its String label
        if (t instanceof Datatype.BooleanType) return "Boolean";                   // nullable: a leegwaarde is a real state
        if (t instanceof Datatype.DateInDays) return "java.time.LocalDate";
        if (t instanceof Datatype.DateTime) return "java.time.LocalDateTime";
        return "Object";
    }

    /** All bean sources the model implies: simple class name → Java source (incl. Segment + Inconsistentie). */
    public static Map<String, String> generate(RegelSpraakModel model) {
        Map<String, String> out = new LinkedHashMap<>();
        for (ObjectType ot : model.objectTypes.values()) {
            String javaClass = Namen.ident(ot.name, true);
            StringBuilder b = new StringBuilder();
            b.append("package ").append(PACKAGE).append(";\n\n")
             .append("/** Generated fact bean for objecttype '").append(ot.name).append("' (the explicit fact-model contract). */\n")
             .append("public class ").append(javaClass)
             .append(ot.supertype == null ? "" : " extends " + Namen.ident(ot.supertype.name, true))
             .append(" {\n");
            List<Member> members = members(model, ot);
            for (Member l : members)
                b.append("    private ").append(l.javaType()).append(' ').append(l.property()).append(";\n");
            b.append('\n');
            for (Member l : members) {
                // the names every reader and writer of the member takes from the same helper
                String p = l.property();
                Namen.Lidnamen n = Namen.lidnamen(p);
                b.append("    public ").append(l.javaType()).append(' ').append(l.isForm() ? n.isGetter() : n.getter())
                 .append("() { return ").append(p).append("; }\n")
                 .append("    public void ").append(n.setter()).append('(').append(l.javaType()).append(" v) { this.")
                 .append(p).append(" = v; }\n");
            }
            b.append("}\n");
            out.put(javaClass, b.toString());
        }
        out.put("Segment", SEGMENT_SOURCE);
        out.put("Inconsistentie", INCONSISTENCY_SOURCE);
        return out;
    }

    private static final String SEGMENT_SOURCE = """
            package dev.uchar.regelspraak.facts;

            import java.math.BigDecimal;
            import java.time.LocalDate;

            /** One tijdvak [van, tot) of a §13.3.6 temporal attribute's Tijdreeks (generated, the explicit fact-model contract). */
            public class Segment {
                private LocalDate van;
                private LocalDate tot;
                private BigDecimal waarde;

                public Segment() {}
                public Segment(LocalDate van, LocalDate tot, BigDecimal waarde) {
                    this.van = van; this.tot = tot; this.waarde = waarde;
                }

                public LocalDate getVan() { return van; }
                public void setVan(LocalDate v) { this.van = v; }
                public LocalDate getTot() { return tot; }
                public void setTot(LocalDate v) { this.tot = v; }
                public BigDecimal getWaarde() { return waarde; }
                public void setWaarde(BigDecimal v) { this.waarde = v; }
            }
            """;

    private static final String INCONSISTENCY_SOURCE = """
            package dev.uchar.regelspraak.facts;

            /** The observable §13.4.8/§9.5 "inconsistent" result: the violated rule + subject (generated, the explicit fact-model contract),
             *  with the EF codes of the kinds deposited (RUNTIME-SEMANTICS ch. 5.6, 5.7b). */
            public class Inconsistentie {
                private final String rule;
                private final Object subject;
                private final java.util.List<String> codes;
                public Inconsistentie(String rule, Object subject) { this(rule, subject, java.util.List.of()); }
                public Inconsistentie(String rule, Object subject, java.util.List<String> codes) {
                    this.rule = rule; this.subject = subject; this.codes = java.util.List.copyOf(codes);
                }
                public String getRule() { return rule; }
                public Object getSubject() { return subject; }
                public java.util.List<String> getCodes() { return codes; }
                @Override public String toString() { return "Inconsistentie[" + rule + " @ " + subject + "]"; }
            }
            """;
}
