package io.github.yhavinga.regelspraak.schema;

import io.github.yhavinga.regelspraak.model.Datatype;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Feittype;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Objecttype;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Rol;
import io.github.yhavinga.regelspraak.transpiler.kern.Namen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates the fact-bean sources a resolved RegelSpraak model implies (the explicit fact-model contract) — the EXPLICIT form
 * of the contract the generated rules assume: one JavaBean per objecttype in {@code io.github.yhavinga.regelspraak.facts}
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
    public static final String PAKKET = "io.github.yhavinga.regelspraak.facts";

    /** One expected bean member: the Java property, its type source-name, and whether the getter is the
     *  boolean {@code is<X>} form (a scalar kenmerk). */
    public record Lid(String eigenschap, String javaType, boolean isVorm) {}

    /** Every bean member the model implies for {@code ot} — the ONE derivation the generator renders and
     *  the {@link ContractValidator} checks. */
    /** IR-SPEC ch. 2.2: the members a subtype's bean declares ITSELF. The generated bean
     *  {@code extends} its supertype's, so every inherited member is already a field there; emitting
     *  it again would hide the parent's. The subtraction is by PROPERTY NAME, the same key the dedup
     *  below uses, because the derived reference arms re-derive the SAME role field on a subtype that
     *  the parent already carries (a relation hung on the supertype). */
    public static List<Lid> leden(RegelSpraakModel model, Objecttype ot) {
        List<Lid> alle = alleLeden(model, ot);
        if (ot.supertype == null) return alle;
        java.util.Set<String> geerfd = new java.util.HashSet<>();
        for (Lid l : leden(model, ot.supertype)) geerfd.add(l.eigenschap());
        List<Lid> uit = new ArrayList<>();
        for (Lid l : alle) if (!geerfd.contains(l.eigenschap())) uit.add(l);
        return uit;
    }

    private static List<Lid> alleLeden(RegelSpraakModel model, Objecttype ot) {
        List<Lid> uit = new ArrayList<>();
        java.util.Set<String> gezien = new java.util.HashSet<>();   // ONE bean property per name: an attribute,
        // kenmerk and to-one role may legally share a name (a single name can be declared as both a kenmerk
        // and a Boolean attribute); the bean can carry only one field, and the runtime property resolution
        // reads whichever getter exists — so the FIRST derivation (attribute > kenmerk > referentie) wins.
        // The two loops below ITERATE the OWN declaration maps, deliberately, and are the reason this
        // class reads no chain-closed facet map either: {@link #leden} subtracts what the supertype's
        // bean already declares and the generated class `extends` it, so a chain-closed iteration would
        // visit every inherited member once per descendant only to have the subtraction remove it again.
        // The facet lookups INSIDE them are keyed by a name this very iteration produced, so the nearest
        // declaration IS ot (WF-105 refuses shadowing) and the own map already answers for it.
        for (Map.Entry<String, Datatype> e : ot.attributen.entrySet()) {
            String naam = e.getKey();
            String type;
            if (!ot.dimensiesVan(naam).isEmpty()) type = "java.util.Map<String, java.math.BigDecimal>";   // §3.6
            else if (ot.attribuutGranulariteit.containsKey(naam)) type = "java.util.List<Segment>";       // §13.3.6
            else type = javaType(e.getValue());
            if (gezien.add(Namen.ident(naam, false)))
                uit.add(new Lid(Namen.ident(naam, false), type, false));
        }
        for (String k : ot.kenmerken) {
            boolean temporeel = ot.kenmerkGranulariteit.containsKey(k);                                    // §13.3.6
            if (gezien.add(Namen.ident(k, false)))
                uit.add(new Lid(Namen.ident(k, false), temporeel ? "java.util.List<Segment>" : "boolean", !temporeel));
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
        for (Objecttype doelwit : model.objecttypen.values()) {
            if (doelwit == ot) continue;
            for (String rol : model.verbindendeRollen(ot, doelwit)) {
                if (model.naarVeelRol(ot, doelwit, rol)) continue;
                if (gezien.add(Namen.ident(rol, false)))
                    uit.add(new Lid(Namen.ident(rol, false), Namen.ident(doelwit.naam, true), false));
            }
        }
        // §13.3.9.2: the SELF-PAIR reference field, which the loop above cannot reach because it skips
        // ot itself. It is derivable for exactly one shape — a wederkerig feittype — see zelfpaarRol.
        String zelf = zelfpaarRol(model, ot);
        if (zelf != null && gezien.add(Namen.ident(zelf, false)))
            uit.add(new Lid(Namen.ident(zelf, false), Namen.ident(ot.naam, true), false));
        for (String rol : zelfpaarRollen(model, ot))                                                   // §13.3.9 self-typed
            if (gezien.add(Namen.ident(rol, false)))
                uit.add(new Lid(Namen.ident(rol, false), Namen.ident(ot.naam, true), false));
        return uit;
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
     * role name or the creation's symbolic {@code verbindendeRol} wire binding does. Under-implied, never
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
     */
    public static boolean heeftReferentieVeld(RegelSpraakModel model, Objecttype ot, String rol, Objecttype doel) {
        if (model == null || ot == null || rol == null || doel == null) return false;
        String eigenschap = Namen.ident(rol, false), type = Namen.ident(doel.naam, true);
        // the bean CARRIES a field it inherits: `leden` is own-members-only since the generated
        // subtype bean extends its supertype's, so the question is asked over the whole chain
        for (Objecttype t : ot.keten())
            if (leden(model, t).stream().anyMatch(l -> l.eigenschap().equals(eigenschap) && l.javaType().equals(type)))
                return true;
        return false;
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
     */
    public static String geenReferentieveldReden(Objecttype ot, String rol, Objecttype doel) {
        return ot.heeftLid(rol)
                ? "de rol '" + rol + "' op " + ot.naam + " deelt haar naam met een gedeclareerd lid — het beanveld"
                  + " draagt dat attribuut/kenmerk en niet de referentie (leden(): attribuut > kenmerk >"
                  + " referentie), dus de koppeling naar " + doel.naam + " heeft geen veld"
                : "het beanmodel leidt voor de rol '" + rol + "' op " + ot.naam
                  + " geen referentieveld naar " + doel.naam + " af";
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
        if (!model.naarVeelRol(drager, doel, rol)) return geenReferentieveldReden(drager, rol, doel);
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
        return "de rol '" + rol + "' op " + drager.naam + " is meervoudig, dus haar opslag is het veld aan de"
                + " overzijde" + (overzijdeDraagt ? "" : ", en ook daar leidt het beanmodel er geen af"
                                                        + " (beide zijden meervoudig)");
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
            package io.github.yhavinga.regelspraak.facts;

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
            package io.github.yhavinga.regelspraak.facts;

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
