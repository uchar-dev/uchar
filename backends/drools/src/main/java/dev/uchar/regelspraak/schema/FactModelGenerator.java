package dev.uchar.regelspraak.schema;

import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Feittype;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;
import dev.uchar.regelspraak.model.RegelSpraakModel.Rol;
import dev.uchar.regelspraak.transpiler.kern.Namen;

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
 * §13.3.9.2 wederkerig SELF-PAIR field (see {@link #zelfpaarRol}) and the ORDINARY self-typed pair's one
 * field per to-one role (see {@link #zelfpaarRollen}). {@code Segment} and
 * {@code Inconsistentie} are emitted alongside.
 *
 * <p>The MEMBER DERIVATION ({@link #leden}) is shared with {@link ContractValidator}, so what this
 * generator emits and what the validator demands can never drift apart.
 */
public final class FactModelGenerator {
    private FactModelGenerator() {}

    /** The package the generated rules import their fact classes from (both back-ends). */
    public static final String PAKKET = "dev.uchar.regelspraak.facts";

    /** One expected bean member: the Java property, its type source-name, and whether the getter is the
     *  boolean {@code is<X>} form (a scalar kenmerk). */
    public record Lid(String eigenschap, String javaType, boolean isVorm) {}

    /** The declaration a bean member stands for: an attribute or a characteristic {@code naam} of the object type
     *  {@code bron} that declares it, or the role {@code naam} of the fact type {@code bron} (null when the role
     *  resolves to no single declaration). One origin reached twice is one declaration: a role field is derived
     *  again on every subtype of the type that plays the other role, and once per subtype of its own filler. */
    public record Herkomst(Soort soort, String naam, String bron) {
        public enum Soort { ATTRIBUUT, KENMERK, ROL }

        /** The declaration in the words of a refusal. */
        public String beschrijving() {
            return switch (soort) {
                case ATTRIBUUT -> "het attribuut '" + naam + "' van " + bron;
                case KENMERK -> "het kenmerk '" + naam + "' van " + bron;
                case ROL -> "de rol '" + naam + "'" + (bron == null ? "" : " (feittype '" + bron + "')");
            };
        }
    }

    /** A bean member the model implies and the declaration it stands for, before the dedup of {@link #alleLeden}. */
    private record Kandidaat(Lid lid, Herkomst herkomst) {}

    /** IR-SPEC ch. 2.2: the members a subtype's bean declares ITSELF. The generated bean
     *  {@code extends} its supertype's, so every inherited member is already a field there; emitting
     *  it again would hide the ancestor's. The subtraction is by PROPERTY NAME, the same key the dedup
     *  below uses, because the derived reference arms re-derive the SAME role field on a subtype that
     *  an ancestor already carries (a relation hung on the supertype), and it runs over every ancestor:
     *  subtracting the direct supertype's own members only let a grandchild declare such a field again. */
    public static List<Lid> leden(RegelSpraakModel model, Objecttype ot) {
        List<Lid> alle = alleLeden(model, ot);
        if (ot.supertype == null) return alle;
        java.util.Set<String> geerfd = new java.util.HashSet<>();
        List<Objecttype> keten = ot.keten();
        for (int i = 1; i < keten.size(); i++)
            for (Lid l : alleLeden(model, keten.get(i))) geerfd.add(l.eigenschap());
        List<Lid> uit = new ArrayList<>();
        for (Lid l : alle) if (!geerfd.contains(l.eigenschap())) uit.add(l);
        return uit;
    }

    private static List<Lid> alleLeden(RegelSpraakModel model, Objecttype ot) {
        List<Lid> uit = new ArrayList<>();
        for (Kandidaat k : gededupliceerd(model, ot)) uit.add(k.lid());
        return uit;
    }

    /** The candidates of {@link #kandidaten} that keep their property on the bean of {@code ot}, with their origin. */
    private static List<Kandidaat> gededupliceerd(RegelSpraakModel model, Objecttype ot) {
        List<Kandidaat> uit = new ArrayList<>();
        java.util.Set<String> gezien = new java.util.HashSet<>();   // ONE bean property per name: an attribute,
        // kenmerk and to-one role may legally share a name (a single name can be declared as both a kenmerk
        // and a Boolean attribute); the bean can carry only one field, and the runtime property resolution
        // reads whichever getter exists — so the FIRST derivation (attribute > kenmerk > referentie) wins.
        // What lost the property has no field of its own: verdringingen() names it, and the backend refuses
        // every use of it by name.
        for (Kandidaat k : kandidaten(model, ot))
            if (gezien.add(k.lid().eigenschap())) uit.add(k);
        return uit;
    }

    /** The member the bean of {@code ot} carries under {@code eigenschap}, its own or an inherited one, with the
     *  declaration it stands for; null when no bean of the chain has that property. The root-most type of the
     *  chain that derives the property declares the field: {@link #leden} leaves it to that ancestor's bean. */
    private static Kandidaat veld(RegelSpraakModel model, Objecttype ot, String eigenschap) {
        List<Objecttype> keten = ot.keten();
        for (int i = keten.size() - 1; i >= 0; i--)
            for (Kandidaat k : gededupliceerd(model, keten.get(i)))
                if (k.lid().eigenschap().equals(eigenschap)) return k;
        return null;
    }

    /** Every bean member the model implies for {@code ot}, with its origin, in the order the dedup of
     *  {@link #alleLeden} keeps the first of: the attributes, the kenmerken, the reference fields. */
    private static List<Kandidaat> kandidaten(RegelSpraakModel model, Objecttype ot) {
        List<Kandidaat> uit = new ArrayList<>();
        // The two loops below ITERATE the OWN declaration maps, deliberately, and are the reason this
        // class reads no chain-closed facet map either: {@link #leden} subtracts what the ancestors'
        // beans already declare and the generated class `extends` them, so a chain-closed iteration would
        // visit every inherited member once per descendant only to have the subtraction remove it again.
        // The facet lookups INSIDE them are keyed by a name this very iteration produced, so the nearest
        // declaration IS ot (WF-105 refuses shadowing) and the own map already answers for it.
        for (Map.Entry<String, Datatype> e : ot.attributen.entrySet()) {
            String naam = e.getKey();
            String type;
            if (!ot.dimensiesVan(naam).isEmpty()) type = "java.util.Map<String, java.math.BigDecimal>";   // §3.6
            else if (ot.attribuutGranulariteit.containsKey(naam)) type = "java.util.List<Segment>";       // §13.3.6
            else type = javaType(e.getValue());
            uit.add(new Kandidaat(new Lid(Namen.ident(naam, false), type, false),
                    new Herkomst(Herkomst.Soort.ATTRIBUUT, naam, ot.naam)));
        }
        for (String k : ot.kenmerken) {
            boolean temporeel = ot.kenmerkGranulariteit.containsKey(k);                                    // §13.3.6
            uit.add(new Kandidaat(new Lid(Namen.ident(k, false), temporeel ? "java.util.List<Segment>" : "boolean",
                    !temporeel), new Herkomst(Herkomst.Soort.KENMERK, k, ot.naam)));
        }
        // §13.3.9: the scalar to-one reference fields — derived from the SAME authority the transpilers
        // navigate by (verbindendeRollen + naarVeelRol): for every other objecttype, the houder carries ONE
        // field per uniquely addressable to-one role of that type, named after the role ("Eén houder betreft
        // meerdere elementen" -> Element.houder : Houder), UNLESS that navigation crosses a to-many role
        // (then the scalar reference lives on the other side).
        //
        // ONE FIELD PER ROLE, not per PAIR (card drools-rolvelden-herhaalde-typenparen). This loop used to
        // ask verbindendeRol(ot, doelwit) — the by-PAIR search — which answers null the moment a SECOND
        // feittype relates the same two object types, so a bean carried no reference at all and the fold's
        // join, the §13.4.7 creation's three positions, the navigated read and the case-input write each
        // refused a document whose author had written a perfectly unique role name. The ambiguity was never
        // in the source; it was in asking the question by pair. verbindendeRollen asks it by NAME, per role,
        // and keeps every refusal that is genuinely a refusal: a name two roles share resolves to nothing,
        // a to-many role's storage stays the far end's field, and a declared member of the same name still
        // displaces the reference through the dedup above. Under-implied, never over-implied — as before,
        // now measured per role instead of per pair.
        //
        // The by-pair authority itself is UNCHANGED and stays ambiguous: a consumer that has no written or
        // symbolic role still finds a repeated pair unresolvable (RUNTIME-SEMANTICS ch. 5.5), and this
        // derivation is what makes the field EXIST for the consumers that do have one.
        //
        // The field's TYPE is the role's declared filler, not the doelwit this loop reaches it from. The loop
        // reaches one role once per subtype of its filler (IR-SPEC ch. 2.6: the filler is an upper bound), and
        // the dedup keeps the first; typed after the doelwit, the first-DECLARED of those types decided the
        // field's class, so a subtype declared before its supertype narrowed the field and an instance of the
        // supertype no longer fitted it. The filler's class takes every instance the role takes, since each
        // subtype's bean extends it.
        for (Objecttype doelwit : model.objecttypen.values()) {
            if (doelwit == ot) continue;
            for (String rol : model.verbindendeRollen(ot, doelwit)) {
                if (model.naarVeelRol(ot, doelwit, rol)) continue;
                uit.add(new Kandidaat(new Lid(Namen.ident(rol, false),
                        Namen.ident(vullertype(model, ot, doelwit, rol).naam, true), false),
                        rolHerkomst(model, ot, doelwit, rol)));
            }
        }
        // §13.3.9.2: the SELF-PAIR reference field, which the loop above cannot reach because it skips
        // ot itself. It is derivable for exactly one shape — a wederkerig feittype — see zelfpaarRol.
        String zelf = zelfpaarRol(model, ot);
        if (zelf != null)
            uit.add(new Kandidaat(new Lid(Namen.ident(zelf, false), Namen.ident(ot.naam, true), false),
                    rolHerkomst(model, ot, ot, zelf)));
        for (String rol : zelfpaarRollen(model, ot))                                                   // §13.3.9 self-typed
            uit.add(new Kandidaat(new Lid(Namen.ident(rol, false), Namen.ident(ot.naam, true), false),
                    rolHerkomst(model, ot, ot, rol)));
        return uit;
    }

    /** The declaration of the role {@code rol} that a field on {@code houder} toward {@code doelwit} stands for:
     *  the role of that name that {@code doelwit} may fill, in a fact type that relates {@code houder} — the search
     *  {@code verbindendeRollen} admits a role by, which leaves exactly one. Public for the case reader, which tells
     *  the field of this very role apart from a field another role took. */
    public static Herkomst rolHerkomst(RegelSpraakModel model, Objecttype houder, Objecttype doelwit, String rol) {
        List<Map.Entry<Feittype, Rol>> rollen = rollenNaar(model, houder, doelwit, rol);
        return new Herkomst(Herkomst.Soort.ROL, rol, rollen.size() == 1 ? rollen.get(0).getKey().naam : null);
    }

    /** The declared filler of the role {@code rol} that {@code doelwit} fills from {@code houder}: the type the
     *  reference field holds. {@code doelwit} itself where the role resolves to no single declaration or its filler to
     *  no declared type — the fall-back {@link #vult} keeps for a filler the model does not declare. */
    private static Objecttype vullertype(RegelSpraakModel model, Objecttype houder, Objecttype doelwit, String rol) {
        List<Map.Entry<Feittype, Rol>> rollen = rollenNaar(model, houder, doelwit, rol);
        Objecttype vuller = rollen.size() == 1 ? model.objecttypeOpNaam(rollen.get(0).getValue().objecttypeNaam()) : null;
        return vuller != null ? vuller : doelwit;
    }

    /** The roles named {@code rol} that {@code doelwit} may fill in a fact type relating {@code houder}, each with
     *  its fact type — the one search behind {@link #rolHerkomst} and {@link #vullertype}. */
    private static List<Map.Entry<Feittype, Rol>> rollenNaar(RegelSpraakModel model, Objecttype houder,
            Objecttype doelwit, String rol) {
        List<Map.Entry<Feittype, Rol>> uit = new ArrayList<>();
        for (Feittype ft : model.feittypen.values())
            for (Rol r : ft.rollen)
                if (rol.equalsIgnoreCase(r.naam()) && vult(model, r, doelwit)
                        && (ft.wederkerig ? vult(model, r, houder)
                                          : ft.rollen.stream().anyMatch(h -> h != r && vult(model, h, houder))))
                    uit.add(Map.entry(ft, r));
        return uit;
    }

    /** Whether {@code ot} may fill {@code r}: it is the role's filler or a subtype of it. */
    private static boolean vult(RegelSpraakModel model, Rol r, Objecttype ot) {
        Objecttype vuller = model.objecttypeOpNaam(r.objecttypeNaam());
        return vuller != null ? ot.isEen(vuller) : r.objecttypeNaam().equalsIgnoreCase(ot.naam);
    }

    /** A bean member without a field of its own. On the bean of {@code type} the property of the member
     *  {@code verdrongen} stands for belongs to {@code eigenaar}, which the generator ports first: an ancestor's
     *  member before a descendant's, and within one type the attributes, the kenmerken, the reference fields, in
     *  that order and each in declaration order. {@code Namen.ident} drops the spaces and hyphens of a name and
     *  camel-cases its words, so two names the verifier keeps apart can meet here, and WF-002 lets an attribute and
     *  a characteristic share even the same name. Whatever reads or writes {@code verdrongen} reaches the field of
     *  {@code eigenaar}: a crash at fire when their types differ, and when they agree the one member silently
     *  written through the other. */
    public record Verdringing(Objecttype type, Herkomst verdrongen, Lid verdrongenLid, Herkomst eigenaar,
                              Lid eigenaarLid) {
        /** The bean property both fall on, as {@code <Klasse>.<eigenschap>}. */
        public String javaNaam() { return Namen.ident(type.naam, true) + "." + verdrongenLid.eigenschap(); }
    }

    /** Every {@link Verdringing} of the model, once per member that lost its property: on the type whose bean
     *  would have declared it, not again on each subtype that inherits the loss. A member reached twice from one
     *  declaration is not one. */
    public static List<Verdringing> verdringingen(RegelSpraakModel model) {
        List<Verdringing> uit = new ArrayList<>();
        for (Objecttype ot : model.objecttypen.values()) {
            Map<String, Kandidaat> eigenaar = new java.util.HashMap<>();
            List<Objecttype> keten = ot.keten();
            for (int i = keten.size() - 1; i >= 1; i--)                   // the ancestors, root first
                for (Kandidaat k : kandidaten(model, keten.get(i))) eigenaar.putIfAbsent(k.lid().eigenschap(), k);
            for (Kandidaat k : kandidaten(model, ot)) {
                Kandidaat eerder = eigenaar.putIfAbsent(k.lid().eigenschap(), k);
                if (eerder != null && !eerder.herkomst().equals(k.herkomst()))
                    uit.add(new Verdringing(ot, k.herkomst(), k.lid(), eerder.herkomst(), eerder.lid()));
            }
        }
        return uit;
    }

    /** Why {@code v}'s member has no field of its own, in the one sentence both branches and the case reader
     *  refuse a use of it with; the caller appends who uses it. */
    public static String verdringingReden(Verdringing v) {
        return "beannaambotsing: " + v.verdrongen().beschrijving() + " valt op de beaneigenschap " + v.javaNaam()
                + " van " + v.eigenaar().beschrijving() + " en heeft geen eigen veld";
    }

    /** {@link #verdringingReden} for the member a case names on an instance of {@code ot} — an attribute, or with
     *  {@code kenmerk} a characteristic, resolved on {@code ot}'s chain under the verifier's name identity — or
     *  null when that member has a field of its own (or {@code ot} declares none by that name). */
    public static String verdrongenLid(List<Verdringing> verdringingen, Objecttype ot, String naam, boolean kenmerk) {
        String sleutel = Namen.norm(naam);
        for (Objecttype t : ot.keten())
            for (String lid : kenmerk ? t.kenmerken : t.attributen.keySet())
                if (Namen.norm(lid).equals(sleutel)) {
                    Herkomst h = new Herkomst(kenmerk ? Herkomst.Soort.KENMERK : Herkomst.Soort.ATTRIBUUT, lid, t.naam);
                    for (Verdringing v : verdringingen) if (v.verdrongen().equals(h)) return verdringingReden(v);
                    return null;
                }
        return null;
    }

    /** Two ROLES on one reference field of one type, as a refusal, or null. A field of another type is left to the
     *  use sites, whose gate asks name and type ({@link #heeftReferentieVeld}) and refuses by name; one of the same
     *  type passes that gate for both roles, and every reader and writer of either then works one relation. */
    public static String rolbotsing(List<Verdringing> verdringingen) {
        for (Verdringing v : verdringingen)
            if (v.verdrongen().soort() == Herkomst.Soort.ROL && v.eigenaar().soort() == Herkomst.Soort.ROL
                    && v.verdrongenLid().javaType().equals(v.eigenaarLid().javaType()))
                return "beannaambotsing: " + v.verdrongen().beschrijving() + " valt op het referentieveld "
                        + v.javaNaam() + " van " + v.eigenaar().beschrijving() + ", een veld van hetzelfde type, en de"
                        + " bean draagt per veld één relatie";
        return null;
    }

    /** The classes the generator writes into {@link #PAKKET} itself, with what each is for. */
    private static final Map<String, String> HULPKLASSEN =
            Map.of("Segment", "de segmenten van een tijdlijn", "Inconsistentie", "een inconsistentie");

    /** Why the object types of {@code model} do not each get a fact class of their own, or null when they do:
     *  two types on one class name ({@code Namen.ident} is not injective, while WF-001 compares the names
     *  case-insensitively and keeps them apart), a type on the name of a class the generator writes itself, or a
     *  type on the name of a public {@code java.lang} type, which the fact class then hides from the generated beans
     *  (their own package comes first) and from the DRL text (which imports every fact class by name). */
    public static String klassebotsing(RegelSpraakModel model) {
        Map<String, Objecttype> klassen = new java.util.HashMap<>();
        for (Objecttype ot : model.objecttypen.values()) {
            String klasse = Namen.ident(ot.naam, true), fqn = PAKKET + "." + klasse;
            Objecttype eerder = klassen.putIfAbsent(klasse, ot);
            if (eerder != null)
                return "beannaambotsing: de objecttypen '" + eerder.naam + "' en '" + ot.naam + "' vallen op één"
                        + " feitklasse, " + fqn + ", en het beanmodel maakt één klasse per naam";
            if (HULPKLASSEN.containsKey(klasse))
                return "beannaambotsing: het objecttype '" + ot.naam + "' valt op de feitklasse " + fqn + ", de"
                        + " hulpklasse die het beanmodel zelf genereert voor " + HULPKLASSEN.get(klasse);
            if (javaLang(klasse))
                return "beannaambotsing: het objecttype '" + ot.naam + "' valt op de feitklasse " + fqn + ", en die"
                        + " verbergt java.lang." + klasse + " voor de gegenereerde beans en de DRL-tekst";
        }
        return null;
    }

    /** Whether {@code java.lang} has a public type of this simple name — the names every Java source sees
     *  without an import. */
    private static boolean javaLang(String klasse) {
        try {
            Class<?> c = Class.forName("java.lang." + klasse, false, ClassLoader.getPlatformClassLoader());
            return java.lang.reflect.Modifier.isPublic(c.getModifiers());
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * §13.3.9 — the scalar to-one reference fields an ORDINARY (non-wederkerig) SELF-TYPED feittype implies on
     * {@code ot}: two DISTINCT sibling roles both filled by {@code ot} ({@code voorganger} and {@code opvolger}
     * on {@code Wachter}). The by-pair loop in {@link #leden} cannot reach them (it skips {@code ot} itself) and
     * {@link #zelfpaarRol} does not cover them (it is the one-role wederkerig shape).
     *
     * <p><b>The decision (card drools-zelfgetypeerd-creatiepaar):</b> the bean carries ONE SCALAR FIELD PER
     * TO-ONE ROLE, named after the role, exactly as the by-pair loop names a reference after the role of the
     * thing it POINTS AT — {@code Wachter.opvolger} is the Wachter that plays {@code opvolger} (the successor),
     * {@code Wachter.voorganger} the one that plays {@code voorganger}. Derived through the SAME authority the
     * transpilers navigate by, here its BY-NAME form ({@code verbindendeRolViaNaam} + {@code naarVeelRol}), so a
     * role name that does not resolve UNIQUELY derives nothing and a to-many role derives nothing (its storage is
     * the other end's field). {@code verbindendeRol(ot, ot)} — the BY-PAIR search — is deliberately left as it
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
     *   <li><b>Teaching {@code verbindendeRol(ot, ot)} to pick a winner</b> (prefer the to-one / the first role):
     *       that authority is SHARED by every navigated read, feitcreatie, verdeling and by
     *       {@link ContractValidator}; making it guess would turn an honest ambiguity into a silent wrong join
     *       across the whole back-end — precisely what RUNTIME-SEMANTICS ch. 5.5 forbids and what the reference
     *       interpreter refuses ({@code connecting_pair} BLOCKS a self-typed two-role fact type and takes the
     *       explicit binding instead).</li>
     *   <li><b>A collection-valued field per self-pair</b>: a to-one role's storage IS a scalar; a collection
     *       would misreport cardinality to every {@code naarVeelRol} consumer and to the §9 creation idempotence
     *       guard, which keys a 1:1 target on that very field being empty.</li>
     * </ul>
     */
    public static List<String> zelfpaarRollen(RegelSpraakModel model, Objecttype ot) {
        List<String> uit = new ArrayList<>();
        for (Feittype ft : model.feittypen.values()) {
            if (ft.wederkerig) continue;                       // the one-role shape: zelfpaarRol owns it
            for (Rol r : ft.rollen) {
                if (!ot.naam.equalsIgnoreCase(r.objecttypeNaam())) continue;
                // the by-NAME authority: null when the name does not resolve uniquely, or when the feittype does
                // not relate ot to ot at all (a role of ot in a feittype whose sibling is another type is the
                // by-pair loop's business, and is reached from THAT side).
                String rol = model.verbindendeRolViaNaam(ot, ot, r.naam());
                if (rol == null || model.naarVeelRol(ot, ot, rol)) continue;
                if (!uit.contains(rol)) uit.add(rol);
            }
        }
        return uit;
    }

    /**
     * §13.3.9 — whether {@code ot}'s bean really carries the SCALAR REFERENCE field for role {@code rol} pointing
     * at {@code doel}: the property name AND its type, asked of {@link #leden}, the ONE derivation this generator
     * renders and {@link ContractValidator} demands.
     *
     * <p>The TYPE half is the whole point. {@code leden} ports every member through a name-dedup — attribuut &gt;
     * kenmerk &gt; referentie, because a bean can carry only one field per name — so a declared member that shares
     * the role's name DISPLACES the reference: the property still exists, but it holds a {@code BigDecimal} or a
     * {@code boolean}. A name-only test therefore reads "field present" and sends a FACT into it, which is an
     * unclassified ERROR at fire ({@code ClassCastException} on the creation arm, "kon setter set&lt;X&gt;(…) niet
     * aanroepen" on the case-input arm — lens 2, 2026-08-30), where the honest outcome is a loud per-form refusal.
     *
     * <p>The type test is ASSIGNABILITY, not equality. A role's declared filler is an upper bound (IR-SPEC ch. 2.6),
     * the field is of the filler's class ({@link #vullertype}) and a subtype's bean extends it, so the field of a role
     * filled by {@code Vondst} holds a {@code Wrakvondst}. Asked as equality, every reader and writer of that link
     * refused a document ref and clingo play: the case input, the navigation, the fold and quantifier joins, the
     * allocation's recipient join, the far-side role check and the object and fact creations. A strict ancestor's class is accepted only for the
     * field of the very role declaration that {@code ot}, {@code rol} and {@code doel} resolve to: a field of the same
     * name that ANOTHER role took keeps refusing, even when {@code doel} fits its class, since writing through it
     * would join one relation through another's storage.
     */
    public static boolean heeftReferentieVeld(RegelSpraakModel model, Objecttype ot, String rol, Objecttype doel) {
        if (model == null || ot == null || rol == null || doel == null) return false;
        // the bean CARRIES a field it inherits: `leden` is own-members-only since the generated
        // subtype bean extends its supertype's, so the question is asked over the whole chain
        Kandidaat veld = veld(model, ot, Namen.ident(rol, false));
        if (veld == null) return false;
        String type = veld.lid().javaType();
        if (type.equals(Namen.ident(doel.naam, true))) return true;
        boolean voorouder = false;
        for (Objecttype a : doel.keten()) voorouder |= a != doel && type.equals(Namen.ident(a.naam, true));
        Herkomst herkomst = veld.herkomst();
        return voorouder && herkomst.soort() == Herkomst.Soort.ROL && herkomst.bron() != null
                && herkomst.equals(rolHerkomst(model, ot, doel, rol));
    }

    /** The declaration whose member the bean of {@code ot} carries under the property of {@code rol}, its own or an
     *  inherited one; null when no bean of the chain has that property. Where {@link #heeftReferentieVeld} refuses a
     *  role, this names what holds the property instead, so a refusal can state the cause rather than guess it. */
    public static Herkomst veldHerkomst(RegelSpraakModel model, Objecttype ot, String rol) {
        Kandidaat veld = model == null || ot == null || rol == null ? null : veld(model, ot, Namen.ident(rol, false));
        return veld == null ? null : veld.herkomst();
    }

    /**
     * WHY {@link #heeftReferentieVeld} said no — the precise §13.3.9 reason, so a per-form refusal names the
     * bean-model limit instead of a generic scope note. The two causes are DIFFERENT and are named apart: a
     * declared member of the same name DISPLACED the reference ({@link #leden}'s dedup), or the model derives
     * no unique field for that role at all.
     *
     * <p>It lives HERE, beside the derivation it explains, because BOTH transpiler branches and every reader
     * refuse the same shapes and must say so in the same words — a producer reading the gate report gets one
     * diagnosis whichever branch produced it. Callers prefix the READER ("genavigeerde lezing",
     * "rollidmaatschapscheck", "objectcreatie", the fold join) and, where it is not already in their sentence,
     * the §13.3.9 pointer; the body is this one string.
     *
     * <p>The second cause states no CARDINALITY (2026-09-11, card drools-ongepoorte-rollezers). It used to read
     * "de enkelvoudige rol", which held only because every caller then checked {@code naarVeelRol} before asking.
     * The readers gated since — the verdeling's recipient join, the far-side rolcheck, the temporal set fold, the
     * feitcreatie's three positions — do NOT, and a to-many role is exactly the shape they meet: measured on a
     * verifier-clean, ref-clean model the verdeling's join over a to-many recipient role died at first fire.
     * Naming it "enkelvoudig" there would assert of the document the very thing that is false about it.
     *
     * <p>A third cause is named apart since 2026-09-28: the property EXISTS and another declaration holds it — a
     * member under another spelling ({@code Namen.ident} is not injective) or the field of another role, the shape
     * two same-named roles in two fact types give when one filler is a subtype of the other. "The bean model
     * derives no reference field" is true there and names no cause, so the sentence names the holder.
     */
    public static String geenReferentieveldReden(RegelSpraakModel model, Objecttype ot, String rol, Objecttype doel) {
        if (ot.heeftLid(rol))
            return "de rol '" + rol + "' op " + ot.naam + " deelt haar naam met een gedeclareerd lid — het beanveld"
                    + " draagt dat attribuut/kenmerk en niet de referentie (leden(): attribuut > kenmerk >"
                    + " referentie), dus de koppeling naar " + doel.naam + " heeft geen veld";
        Kandidaat veld = veld(model, ot, Namen.ident(rol, false));
        Herkomst houder = veld == null ? null : veld.herkomst();
        if (houder != null && !houder.equals(rolHerkomst(model, ot, doel, rol)))
            return "de rol '" + rol + "' op " + ot.naam + " heeft geen eigen referentieveld naar " + doel.naam
                    + ": de beaneigenschap " + Namen.ident(ot.naam, true) + "." + veld.lid().eigenschap()
                    + " draagt " + houder.beschrijving();
        return "het beanmodel leidt voor de rol '" + rol + "' op " + ot.naam + " geen referentieveld naar " + doel.naam
                + " af";
    }

    /**
     * WHY a FOLD's JOIN has no bean field — {@link #geenReferentieveldReden} plus the one diagnosis that reading
     * cannot reach: the role is TO-MANY, so its storage is the far end's field, and the far end derives none
     * either. That is a statement about CARDINALITY, not about a displaced or underivable name, and a fold that
     * reported it as "the bean model derives no reference field" would blame the document for an ambiguity it
     * does not have (the erflijn shape: `erfvolger` resolves perfectly; what stops it is that BOTH sides are
     * to-many, so neither bean can hold the relation).
     *
     * <p>It lives HERE, beside {@link #geenReferentieveldReden}, for that function's own reason: both transpiler
     * branches refuse the same shapes and must say so in the same words. Until 2026-09-18 this cardinality half
     * was an inline string in the exec branch alone, so the two branches diagnosed one fold-join failure with two
     * different sentences — measured on a both-sides-to-many model, exec named the cardinality and the DRL-text
     * branch fell back to the generic reading. Callers still prefix their own reader name.
     */
    public static String geenJoinveldReden(RegelSpraakModel model, Objecttype drager, String rol, Objecttype doel) {
        if (!model.naarVeelRol(drager, doel, rol)) return geenReferentieveldReden(model, drager, rol, doel);
        // The far end is where a to-many role's storage lives. Whether it HAS a field there is a second
        // question, and the answer decides how much this sentence may claim: "both sides to-many" is the
        // erflijn shape, where neither bean can hold the relation, and asserting it where the far side does
        // derive a field (the gemeenteberging shape: a to-one sibling, whose field exists and is simply on the
        // other bean) would diagnose a cardinality defect the document does not have. The exec branch never
        // reached that case — its own conservative arm refuses a to-one element role with a to-many sister
        // first — but the DRL-text branch does not mirror that arm and arrives here.
        Objecttype ver = model.objecttypeOpNaam(doel.naam);
        String zuster = model.verbindendeRolViaElementnaam(drager, rol);
        boolean overzijdeDraagt = zuster != null && ver != null && heeftReferentieVeld(model, ver, zuster, drager);
        String zin = "de rol '" + rol + "' op " + drager.naam + " is meervoudig, dus haar opslag is het veld aan de"
                + " overzijde";
        if (overzijdeDraagt) return zin;
        // A TO-ONE sister whose field is missing is no cardinality defect: its field was taken, by a member or by
        // another role, and saying "both sides to-many" there names a cause the document does not have.
        if (zuster != null && ver != null && !model.naarVeelRol(ver, drager, zuster))
            return zin + ", en dat veld ontbreekt: " + geenReferentieveldReden(model, ver, zuster, drager);
        return zin + ", en ook daar leidt het beanmodel er geen af (beide zijden meervoudig)";
    }

    /**
     * IR-SPEC ch. 4.3 — the role PAIR a symbolic {@code (feittype, rol)} wire binding names:
     * {@code [gebonden, zuster]}, the role the wire names (the folded element's / the created object's) and the
     * one the ANCHOR plays. The sibling's NAME is the reference field on the bound end, and vice versa, which is
     * exactly how {@link #leden} names them. A §13.3.9.2 wederkerig fact type carries ONE role that is its own
     * sibling. null when the binding names no such fact type / role, or when a non-wederkerig fact type has no
     * sibling role at all — for those RESOLUTION failures the caller refuses loudly, never guesses. Mirrors
     * {@code ref/lib/eval.ml}'s {@code bound_role} + {@code resolve_binding}, whose Error flank is a fault there.
     *
     * <p>Resolution goes through the fact type the wire NAMES, not through the role name alone: two fact types
     * may declare an element role of the same name while their anchor roles differ, and the binding then still
     * selects exactly one. That is also why this is not {@code verbindendeRolViaElementnaam} with the bound
     * role's name — that search is by NAME over the whole model and answers null on such a document, which would
     * refuse where the wire is unambiguous.
     *
     * <p>It lives HERE for {@link #geenJoinveldReden}'s reason, one step on: until 2026-09-19 BOTH transpiler
     * branches carried this method privately, line for line identical, and only the dual-branch witnesses held
     * them together. A second implementation is a second chance to drift, and the drift is invisible — two
     * branches that resolve the same wire differently produce two different SETS, not an error. One derivation,
     * two consumers, as {@code StaandeDoelen} already is for the ch. 7.8 standing norm.
     *
     * <p><b>What this does NOT check, and why.</b> The FILLERS. A binding may resolve perfectly and still be
     * wrong for its use site — the bound role filled by the anchor's type instead of the created object's — and
     * this method hands that pair back without a word. That is division of labour, not oversight: <b>WF-047 is
     * the fence</b> ("role binding resolves; fillers agree with the use site"), checked by gate 3 with its own
     * negatief cases, {@code wf047-bound-role-fills-source} and {@code wf047-creation-binding-wrong-filler}, so
     * no corpus artefact reaches this method mis-filled. Measured on an {@code anchor/jutterswet-rekenkamer}
     * variant whose creation binding names the SIBLING role: the verifier FAILS it twice on WF-047, while ref
     * does not fault at all — it takes the wrong binding silently and reads a MISMATCH. A duplicate filler check
     * HERE was weighed and rejected: it would need the expected filler pair at each call site (creation, fold
     * source, the quantifier fold's contributors), i.e. a second fence that can drift from gate 3's. What the
     * callers DO carry is the typed reference-field gate ({@link #heeftReferentieVeld}), which catches the usual
     * downstream consequence — but that is a bean-model question, not a filler check, and does not claim to be.
     */
    public static Rol[] gebondenRolpaar(RegelSpraakModel model, RegelSpraakModel.RolBindingRef rb) {
        if (model == null || rb == null || rb.feittype() == null || rb.rol() == null) return null;
        Feittype ft = null;
        // the WF-001 form of a name, as the verifier and ref compare it (case and runs of blanks folded)
        String feittype = Namen.norm(rb.feittype()), rol = Namen.norm(rb.rol());
        for (Map.Entry<String, Feittype> e : model.feittypen.entrySet())
            if (Namen.norm(e.getKey()).equals(feittype)) { ft = e.getValue(); break; }
        if (ft == null) return null;
        Rol gebonden = null;
        for (Rol r : ft.rollen)
            if (r.naam() != null && Namen.norm(r.naam()).equals(rol)) { gebonden = r; break; }
        if (gebonden == null) return null;
        if (ft.wederkerig) return new Rol[]{gebonden, gebonden};
        for (Rol r : ft.rollen)
            if (r != gebonden) return new Rol[]{gebonden, r};
        return null;
    }

    /**
     * What {@link #creatiekoppeling} decided for one object creation. {@code fwdRol} is the role the created object
     * plays and {@code backRol} the role the subject plays, each with whether it is to-many; {@code fwdRef} is the
     * reference field on the subject that holds the created object and {@code backRef} the one on the created object
     * that holds the subject, each null where that end has no field of its own (a to-many role is stored as the
     * other end's field). {@code eenOpEen} is the 1:1 shape, whose idempotence guard reads {@code fwdRef}. Where the
     * creation cannot be lowered, {@code weigering} says why (a backend gap) or {@code dubbelzinnig} names the
     * ambiguous back-link (a fault of the program, RUNTIME-SEMANTICS ch. 5.5), and the rest is not set.
     */
    public record Creatiekoppeling(String fwdRol, boolean fwdVeel, String backRol, boolean backVeel, String fwdRef,
                                   String backRef, boolean eenOpEen, String weigering, String dubbelzinnig) {
        static Creatiekoppeling weiger(String reden) {
            return new Creatiekoppeling(null, false, null, false, null, null, false, reden, null);
        }
    }

    /**
     * THE back-link decision of an object creation (RUNTIME-SEMANTICS ch. 5.5, IR-SPEC ch. 3.3), one derivation for
     * every transpiler branch, which each carry out in their own way: a skip or a rule that faults at fire on the
     * exec-model branch, a marker on the DRL-text branch, a refusal or a fault fact on the truth-maintenance branch.
     * Until 2026-10-08 the exec-model and the DRL-text branch each carried this block privately
     * ({@code backlog/new/20260923-drools-rolpaar-ontdubbeling.md}).
     *
     * <p>The two link roles come from ONE of two routes. The creation's own {@code connectingRole} binding names the
     * role the created object plays, its sibling the one the subject plays; it is authoritative, as the reference
     * interpreter reads it, and for a self-typed pair it is the only route there is. Without it, the by-pair
     * derivation. Only a to-one role has a bean field, and only a TYPED one is writable: a declared member of the same
     * name displaces the reference, which is refused by name rather than leaving the link one-sided. Where neither end
     * has a field, the mint would be reachable from nothing: an ambiguous pair is ch. 5.5's refusal to guess
     * ({@code dubbelzinnig}), a self-typed pair without a binding, a pair without a unique connecting fact type and a
     * pair of two to-many roles are backend gaps, named apart. {@code naam} names the creation in the exception of a
     * broken invariant: the 1:1 shape must have the forward field its guard reads.
     */
    public static Creatiekoppeling creatiekoppeling(RegelSpraakModel model, Objecttype nieuw, Objecttype subject,
                                                    RegelSpraakModel.RolBindingRef verbindendeRol, String naam) {
        String backRol, fwdRol;
        boolean backVeel, fwdVeel;
        if (verbindendeRol != null) {
            Rol[] paar = gebondenRolpaar(model, verbindendeRol);
            if (paar == null)
                return Creatiekoppeling.weiger("de connectingRole-binding '" + verbindendeRol.feittype() + "."
                        + verbindendeRol.rol() + "' wijst geen rolpaar aan");
            fwdRol = paar[0].naam(); fwdVeel = paar[0].meervoudig();
            backRol = paar[1].naam(); backVeel = paar[1].meervoudig();
        } else {
            backRol = model.verbindendeRol(nieuw, subject);
            fwdRol = model.verbindendeRol(subject, nieuw);
            backVeel = backRol != null && model.naarVeelRol(nieuw, subject, backRol);
            fwdVeel = fwdRol != null && model.naarVeelRol(subject, nieuw, fwdRol);
        }
        String backRef = backRol != null && !backVeel ? backRol : null;
        String fwdRef = fwdRol != null && !fwdVeel ? fwdRol : null;
        if (backRef != null && !heeftReferentieVeld(model, nieuw, backRef, subject))
            return Creatiekoppeling.weiger(geenReferentieveldReden(model, nieuw, backRef, subject));
        if (fwdRef != null && !heeftReferentieVeld(model, subject, fwdRef, nieuw))
            return Creatiekoppeling.weiger(geenReferentieveldReden(model, subject, fwdRef, nieuw));
        if (backRef == null && fwdRef == null) {
            if (verbindendeRol == null && subject != nieuw && verbindendeFeittypen(model, subject, nieuw) > 1)
                return new Creatiekoppeling(null, false, null, false, null, null, false, null,
                        "more than one fact type relates \"" + subject.naam + "\" to \"" + nieuw.naam
                                + "\"; the created object's back-link is ambiguous (ch. 5.5)");
            return Creatiekoppeling.weiger((verbindendeRol == null && subject == nieuw
                    ? "zelfgetypeerd rollenpaar zonder connectingRole-binding: uit twee gelijkgetypeerde rollen leidt"
                      + " de bean geen referentieveld af en de draad wijst geen rol aan"
                    : verbindendeRol == null && (backRol == null || fwdRol == null)
                    ? "geen uniek verbindend feittype: de terugkoppeling is niet af te leiden en ch. 5.5 gist niet"
                    : "beide rollen zijn meervoudig ('" + backRol + "' en '" + fwdRol + "'), dus FactModelGenerator"
                      + " leidt aan geen van beide zijden een referentieveld af")
                    + ", dus het gecreëerde object zou onbereikbaar zijn");
        }
        // with a binding the cardinality is the bound role's own, without one the by-pair reading; either way a 1:1
        // arm must HAVE the forward field its guard reads
        boolean eenOpEen = verbindendeRol != null ? !fwdVeel : !model.subjectNaarVeelCreatie(subject, nieuw);
        if (eenOpEen != (fwdRef != null))
            throw new IllegalStateException("objectcreatie " + naam + ": subjectNaarVeelCreatie(" + eenOpEen
                    + ") en het voorwaartse veld (" + fwdRef + ") spreken elkaar tegen");
        return new Creatiekoppeling(fwdRol, fwdVeel, backRol, backVeel, fwdRef, backRef, eenOpEen, null, null);
    }

    /** How many fact types RELATE {@code van} and {@code naar} in ch. 5.5's sense, the count the reference interpreter's
     *  {@code connecting_pair} takes: a two-role fact type whose two roles the two types fill, in either order, and, for
     *  one type, a §13.3.9.2 wederkerig one-role fact type. 0 is the linkless shape, 1 derivable, more than one
     *  ch. 5.5's ambiguity. Deliberately not {@code verbindendeRol}, which collapses 0 and more than one into null. */
    public static int verbindendeFeittypen(RegelSpraakModel model, Objecttype van, Objecttype naar) {
        if (van == null || naar == null) return 0;
        int n = 0;
        for (Feittype ft : model.feittypen.values()) {
            List<Rol> r = ft.rollen;
            if (r.size() == 2) {
                boolean ab = van.naam.equalsIgnoreCase(r.get(0).objecttypeNaam())
                        && naar.naam.equalsIgnoreCase(r.get(1).objecttypeNaam());
                boolean ba = van.naam.equalsIgnoreCase(r.get(1).objecttypeNaam())
                        && naar.naam.equalsIgnoreCase(r.get(0).objecttypeNaam());
                if (ab || ba) n++;
            } else if (r.size() == 1 && ft.wederkerig && van == naar
                    && van.naam.equalsIgnoreCase(r.get(0).objecttypeNaam())) {
                n++;
            }
        }
        return n;
    }

    /** What {@link #bereikjoin} decided: the scalar join ROLE the wire binding yields, or the REASON it yields
     *  none. Exactly one of the two is non-null; both null means the wire named no range at all, and the caller's
     *  own generic refusal — the one it gave before there were bindings — stands unchanged. */
    public record Bereikjoin(String rol, String weigering) {}

    /**
     * THE fold RANGE a ch. 4.3 wire binding yields — the one derivation both transpiler branches read at every
     * seam where a fold resolves its own set, and the one SENTENCE they both refuse with.
     *
     * <p>The role handed back is the SIBLING's name: the binding names the role the ELEMENT plays, and the
     * sibling — the role the anchor plays — is the reference field ON the element, which is the side the join
     * reads. Since L2 the binding is the only range the wire carries, so it is the route for every pair: a unique
     * one, a REPEATED one (where a search by the type pair is ambiguous by construction) and a SELF-TYPED one.
     *
     * <p>A TO-MANY sibling is the one arm that needed a shared sentence. Its storage is the far end's field
     * (§13.3.9), so the element carries no scalar reference to join on — a resolution success with a cardinality
     * refusal behind it. Until 2026-09-19 each branch answered that with its own GENERIC note (the exec branch's
     * "aggregatie buiten Phase-C scope", the DRL-text branch's bare {@code TODO aggregatie}), so one fold-join
     * failure had two names and neither named the cause. The branches now say the same thing, for
     * {@link #geenJoinveldReden}'s reason: a producer reading the gate report must not have to know which branch
     * answered.
     */
    public static Bereikjoin bereikjoin(RegelSpraakModel model, RegelSpraakModel.RolBindingRef rb) {
        Rol[] paar = gebondenRolpaar(model, rb);
        if (paar == null) return new Bereikjoin(null, null);        // no range on the wire -> the caller's own refusal
        if (!paar[1].meervoudig()) return new Bereikjoin(paar[1].naam(), null);
        return new Bereikjoin(null, "aggregatie-bereik: de wire-binding wijst de rol '" + paar[0].naam()
                + "' aan, wiens zusterrol '" + paar[1].naam() + "' MEERVOUDIG is — die naam zou het referentieveld"
                + " op " + paar[0].objecttypeNaam() + " zijn, maar de opslag van een meervoudige rol ligt aan de"
                + " overzijde, dus dat veld bestaat niet (§13.3.9 opslagrichting)");
    }

    /** The walk a navigated read's wire bindings name: per hop the reached role (hence the bean field on the type
     *  the walk stands on) and the type that role's filler declares; {@code naarVeel} when any hop reaches a
     *  to-many role. */
    public record Navigatiewandeling(List<String> rollen, List<Objecttype> bereikt, boolean naarVeel) {}

    /**
     * THE navigation walk of an {@code AttribuutRef.navigatieBindingen} / {@code Kenmerk.navigatieBinding}
     * (IR-SPEC ch. 4.1a), resolved from {@code subject} — the one derivation both transpiler branches decide a
     * bound navigation by (V1-WIREMIGRATIE P1); since L2 the bindings are the only navigation the wire carries.
     *
     * <p>Null when the walk does not resolve from {@code subject}: a binding naming no declared role, a hop whose
     * opposite role the type the walk stands on does not fill (ch. 2.6, extensionally), or — with a non-null
     * {@code eindpunt} — a last hop whose filler is not the endpoint or a subtype of it (filler ⊑ endpoint, the
     * direction WF-053 and ref demand: every object the walk can reach must be one the read may address). The caller refuses such a read
     * with its own unresolved-navigation reason. It never falls back to the written name: a document whose
     * binding does not fit is refused, not read along a route the wire did not name. The same subject-fit the
     * written-chain derivation ({@code RegelSpraakModel.volgKeten}) applies, so a read the transpiler meets
     * under another subject than the one it was bound to is refused exactly as before.
     *
     * <p>No cross-check against the written step: WF-053 already demands that every written step denote its
     * hop's bound role (gate 3), the written fields disappear at B11, and a cross-check here would turn a
     * binding that decides into one that merely vetoes.
     */
    public static Navigatiewandeling navigatiewandeling(RegelSpraakModel model, Objecttype subject,
            List<RegelSpraakModel.RolBindingRef> bindingen, Objecttype eindpunt) {
        if (bindingen == null || bindingen.isEmpty()) return null;
        List<String> rollen = new ArrayList<>();
        List<Objecttype> bereikt = new ArrayList<>();
        boolean naarVeel = false;
        Objecttype cur = subject;
        for (RegelSpraakModel.RolBindingRef rb : bindingen) {
            Rol[] paar = gebondenRolpaar(model, rb);
            if (paar == null || cur == null) return null;
            Objecttype bijna = model.objecttypeOpNaam(paar[1].objecttypeNaam());
            Objecttype ver = model.objecttypeOpNaam(paar[0].objecttypeNaam());
            if (bijna == null || ver == null || !cur.isEen(bijna)) return null;
            naarVeel |= paar[0].meervoudig();
            rollen.add(paar[0].naam());
            bereikt.add(ver);
            cur = ver;
        }
        if (eindpunt != null && !cur.isEen(eindpunt)) return null;
        return new Navigatiewandeling(List.copyOf(rollen), List.copyOf(bereikt), naarVeel);
    }

    /** One §9.4 creation hop as the bean model stores it: the type the hop reaches, the reference field, and
     *  whether that field sits on the PARENT (the type the hop starts from, joined {@code this == $prev.<field>}) or
     *  on the reached CHILD (joined {@code <field> == $prev}). */
    public record Creatiehop(Objecttype bereikt, String beanveld, boolean opOuder) {}

    /** The bean fields a §13.4.7 creation executes on, derived from its symbolic form: the field it WRITES on the
     *  doel, the field the doel JOINS on, and the hops in between. */
    public record Creatievelden(String schrijfveld, String koppelveld, List<Creatiehop> hops) {}

    /**
     * THE bean fields of a fact creation, derived from its role bindings (IR-SPEC ch. 3.4: "derivable from the role
     * bindings by the backend's own role→field mapping") — the one derivation both transpiler branches decide a
     * creation by. Since L2 of the v1 wire migration the bindings are all the wire carries of a creation.
     *
     * <p>The mapping is {@link #leden}'s own: a bean carries a reference field named after the role the OTHER end
     * plays. The write lands on the doel, pointing at the anker, so it is {@code ankerRol}. The join and each hop
     * bind the role the REACHED object plays, so the field on the reached object is the sibling role and the field
     * on the starting object is the bound role itself. Which of the two a hop reads follows the storage direction
     * (§13.3.9): the bound role to-one puts the field on the parent, else a to-one sibling puts it on the child.
     * At 1:1 both beans carry a field and case input writes both (RUNTIME-SEMANTICS ch. 5.7a); the parent side is
     * taken, the side the frontend picks for an objectcreatie-placed link. Where both roles are to-many there is
     * no field at all and the child side is handed back, so the caller's {@link #heeftReferentieVeld} gate refuses
     * it with its usual reason. The join is read on the doel only, as before.
     *
     * <p>Null when a binding names no declared role; the caller refuses the creation as unresolved.
     */
    public static Creatievelden creatievelden(RegelSpraakModel model,
            dev.uchar.regelspraak.ir.ResolvedFeitCreatie fc) {
        if (fc.feittype() == null || fc.ankerRol() == null || fc.joinRol() == null) return null;
        Rol[] gemaakt = gebondenRolpaar(model, new RegelSpraakModel.RolBindingRef(fc.feittype(), fc.ankerRol()));
        Rol[] join = gebondenRolpaar(model, fc.joinRol());
        if (gemaakt == null || join == null) return null;
        List<Creatiehop> hops = new ArrayList<>();
        for (var stap : fc.tussenstappen()) {
            Rol[] paar = gebondenRolpaar(model, stap.rolBinding());
            if (paar == null) return null;
            Objecttype bereikt = model.objecttypeOpNaam(paar[0].objecttypeNaam());
            if (bereikt == null) return null;
            hops.add(!paar[0].meervoudig() ? new Creatiehop(bereikt, paar[0].naam(), true)
                                           : new Creatiehop(bereikt, paar[1].naam(), false));
        }
        return new Creatievelden(gemaakt[0].naam(), join[1].naam(), List.copyOf(hops));
    }

    /**
     * THE scope field of a parent-scoped uniqueness (ch. 5.9), derived from {@code scopeRol} — the role the scope
     * parent plays, hence the reference field on the subject. IR-SPEC ch. 3.6: the binding IS the scoping, so null
     * means an unscoped uniqueness, or a binding that names no declared role (the caller refuses).
     */
    public static String scopeVeld(RegelSpraakModel model, dev.uchar.regelspraak.ir.ResolvedUniciteit u) {
        if (u.scopeRol() == null) return null;
        Rol[] paar = gebondenRolpaar(model, u.scopeRol());
        return paar == null ? null : paar[0].naam();
    }

    /** Where the link a role check asks about is stored: the reference {@code beanveld}, whether it sits on the SUBJECT
     *  ({@code opSubject}: the subject's field names the other end) or on the other end (its field names the
     *  subject), and the other role's declared filler {@code ander}; or the {@code weigering} when no field holds
     *  it. */
    public record Lidmaatschapsopslag(String beanveld, boolean opSubject, Objecttype ander, String weigering) {}

    /**
     * IR-SPEC ch. 4.5 {@code RoleMembership}: "a link of the fact type exists in which the subject plays the role".
     * The wire names the fact type and the role; where the link is STORED is this backend's question, answered by
     * the member derivation {@link #leden} renders (§13.3.9): a to-one OTHER role is a reference field on the
     * subject, named after that role, and the check is its null-test; a to-many other role puts the storage on the
     * other end, whose field — named after the subject's role — must point back at the subject, which is an
     * exists-join; with both roles to-many no bean carries the link at all.
     */
    public static Lidmaatschapsopslag lidmaatschapsopslag(RegelSpraakModel model, RegelSpraakModel.RolBindingRef rb) {
        Rol[] paar = gebondenRolpaar(model, rb);
        if (paar == null)
            return new Lidmaatschapsopslag(null, false, null, "rollidmaatschap onopgelost: de binding ("
                    + (rb == null ? "geen" : rb.feittype() + ", " + rb.rol()) + ") noemt geen gedeclareerde rol");
        Objecttype ander = model.objecttypeOpNaam(paar[1].objecttypeNaam());
        if (!paar[1].meervoudig()) return new Lidmaatschapsopslag(paar[1].naam(), true, ander, null);
        if (!paar[0].meervoudig()) return new Lidmaatschapsopslag(paar[0].naam(), false, ander, null);
        return new Lidmaatschapsopslag(null, false, ander, "rollidmaatschap (" + rb.feittype() + ", " + rb.rol()
                + "): beide rollen zijn meervoudig, dus geen bean draagt de link (§13.3.9 opslagrichting)");
    }

    /** What {@link #verdelingsjoin} decided: the join ROLE, or the REASON there is none. Both null means there is
     *  no binding to read, and the caller's own "onopgelost" refusal stands. */
    public record Verdelingsjoin(String rol, String weigering) {}

    /**
     * THE recipient join of a §9.7 verdeling — the one derivation both transpiler branches decide it by. The wire's
     * {@code verbindendeRol} binds the role the RECIPIENT plays (IR-SPEC ch. 3.2), so the join field on the
     * recipient is its sibling, the role the source plays; for a wederkerig fact type that is the bound role
     * itself. The binding is required on the wire, so no pair is asked: on a REPEATED pair — two fact types over
     * the same two types — the pair has no answer at all ({@code anchor/jutterswet-bergingstoezicht}). A binding
     * that names no declared role is refused by name, where ref faults; WF-047 is the fence on its fillers.
     */
    public static Verdelingsjoin verdelingsjoin(RegelSpraakModel model, dev.uchar.regelspraak.ir.ResolvedVerdeling v,
            RegelSpraakModel.RolBindingRef rb) {
        Objecttype bron = v.bronType(), doel = v.doelType();
        if (bron == null || doel == null || rb == null) return new Verdelingsjoin(null, null);
        Rol[] paar = gebondenRolpaar(model, rb);
        if (paar == null)
            return new Verdelingsjoin(null, "verbindende rol onopgelost: de binding (" + rb.feittype() + ", "
                    + rb.rol() + ") noemt geen gedeclareerde rol");
        return new Verdelingsjoin(paar[1].naam(), null);
    }

    /**
     * §13.3.9.2 — the scalar to-one SELF-PAIR role a wederkerig feittype implies on {@code ot} (the role name,
     * hence the bean field name), or null when the model implies none.
     *
     * <p>A wederkerig feittype carries a single role that is ITS OWN OPPOSITE (WF: exactly one role), so the
     * pair (ot, ot) resolves through the very same authority the transpilers navigate by —
     * {@code verbindendeRol}, whose {@code feittypeRelateert} admits the reciprocal role as its own sibling.
     * That makes the field name knowable SYMBOLICALLY, from the role alone. An ordinary (non-wederkerig)
     * self-pair stays underived: there two distinct sibling roles both fill the holder, so the by-pair search
     * is ambiguous by construction and the transpilers reach it only through the written role name.
     *
     * <p>The wederkerig guard is belt-and-braces: {@code verbindendeRol}'s own uniqueness already yields null
     * for a non-wederkerig self-pair (two sibling roles), so the guard adds no reachable behaviour — it only
     * states the shape this derivation claims. A to-many reciprocal role derives nothing either: that read is
     * an aggregation, not a scalar field.
     *
     * <p>Null with {@link #noemtWederkerig} true is the UNDERIVABLE half of the shape (an ambiguous pair —
     * two wederkerig feittypen on one objecttype — or a to-many role): callers must refuse it loudly, because
     * the transpilers then navigate by the written role name toward a getter no bean carries.
     */
    public static String zelfpaarRol(RegelSpraakModel model, Objecttype ot) {
        if (!noemtWederkerig(model, ot)) return null;
        String rol = model.verbindendeRol(ot, ot);
        return rol == null || model.naarVeelRol(ot, ot, rol) ? null : rol;
    }

    /** Whether any wederkerig feittype names {@code ot} — the §13.3.9.2 shape itself, field-derivable or not. */
    public static boolean noemtWederkerig(RegelSpraakModel model, Objecttype ot) {
        return model.feittypen.values().stream().anyMatch(ft -> ft.wederkerig
                && ft.rollen.stream().anyMatch(r -> ot.naam.equalsIgnoreCase(r.objecttypeNaam())));
    }

    /** The scalar Java type of a datatype — the same mapping the DRL back-end assumes of the beans. */
    public static String javaType(Datatype t) {
        if (t.isPercentage() || t.isNumeriek()) return "java.math.BigDecimal";
        if (t.isTekst() || t instanceof Datatype.Enumeratie) return "String";   // an enumeratiewaarde is its String label
        if (t instanceof Datatype.Booleaan) return "Boolean";                   // nullable: a leegwaarde is a real state
        if (t instanceof Datatype.DatumInDagen) return "java.time.LocalDate";
        if (t instanceof Datatype.DatumTijd) return "java.time.LocalDateTime";
        return "Object";
    }

    /** All bean sources the model implies: simple class name → Java source (incl. Segment + Inconsistentie). */
    public static Map<String, String> genereer(RegelSpraakModel model) {
        Map<String, String> uit = new LinkedHashMap<>();
        for (Objecttype ot : model.objecttypen.values()) {
            String klasse = Namen.ident(ot.naam, true);
            StringBuilder b = new StringBuilder();
            b.append("package ").append(PAKKET).append(";\n\n")
             .append("/** Generated fact bean for objecttype '").append(ot.naam).append("' (the explicit fact-model contract). */\n")
             .append("public class ").append(klasse)
             .append(ot.supertype == null ? "" : " extends " + Namen.ident(ot.supertype.naam, true))
             .append(" {\n");
            List<Lid> leden = leden(model, ot);
            for (Lid l : leden)
                b.append("    private ").append(l.javaType()).append(' ').append(l.eigenschap()).append(";\n");
            b.append('\n');
            for (Lid l : leden) {
                String p = l.eigenschap(), P = Character.toUpperCase(p.charAt(0)) + p.substring(1);
                b.append("    public ").append(l.javaType()).append(' ').append(l.isVorm() ? "is" : "get").append(P)
                 .append("() { return ").append(p).append("; }\n")
                 .append("    public void set").append(P).append('(').append(l.javaType()).append(" v) { this.")
                 .append(p).append(" = v; }\n");
            }
            b.append("}\n");
            uit.put(klasse, b.toString());
        }
        uit.put("Segment", SEGMENT_BRON);
        uit.put("Inconsistentie", INCONSISTENTIE_BRON);
        return uit;
    }

    private static final String SEGMENT_BRON = """
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

    private static final String INCONSISTENTIE_BRON = """
            package dev.uchar.regelspraak.facts;

            /** The observable §13.4.8/§9.5 "inconsistent" result: the violated rule + subject (generated, the explicit fact-model contract). */
            public class Inconsistentie {
                private final String regel;
                private final Object subject;
                public Inconsistentie(String regel, Object subject) { this.regel = regel; this.subject = subject; }
                public String getRegel() { return regel; }
                public Object getSubject() { return subject; }
                @Override public String toString() { return "Inconsistentie[" + regel + " @ " + subject + "]"; }
            }
            """;
}
