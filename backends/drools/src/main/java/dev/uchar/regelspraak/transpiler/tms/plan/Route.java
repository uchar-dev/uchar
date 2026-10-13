package dev.uchar.regelspraak.transpiler.tms.plan;

import dev.uchar.regelspraak.transpiler.tms.Rekenhulpen;

/**
 * A public static method of the branch that a rendering calls around the functions of a term ({@link Functie}), by its
 * class and its name: what a test of deposits and a consequence do beside the evaluation of a term, and the facts a
 * consequence makes whose class or constructor is not public. A DRL text reaches a method of the branch only as an
 * {@code import function} of a public static method, so a rendering into DRL text names these; the rendering into
 * closures calls the same methods, so that the two renderings run one implementation.
 */
public enum Route {
    /** A fresh probe of the §6.5 and §6.6 deposit, which a test of deposits ({@link Toetsitem.Rol#DEPONERING}) and a
     *  consequence with a deposit ({@link Gevolg.Deponering}) put in the probe's slot before they evaluate. */
    VERSE_SONDE(Rekenhulpen.class, "sonde"),
    /** A place that deposits reports to the probe in its slot: the evaluation of a fold whose filter deposited
     *  ({@link Term.Vouwlezing}); a division and a square root report within their own function. */
    MELD(Rekenhulpen.class, "meld"),
    /** Whether the probe heard a place: the verdict of a test of deposits, and the deposit of a consequence. */
    GEMELD(Rekenhulpen.class, "gemeld"),
    /** The evaluation fault of a fixed sentence, named in a rule, which {@link Gevolg.Fout} inserts. */
    REKENFOUT(Rekenhulpen.class, "rekenfout"),
    /** The §9.5 inconsistency of a form on an object, which {@link Gevolg.Inconsistentie} and the deposit of a
     *  {@link Gevolg.Waarde} insert, and of the conflict of a defaults stack at its root under the conflict's key
     *  ({@link Gevolg.Stapelfeit.Inconsistentie}). */
    INCONSISTENTIE(Rekenhulpen.class, "inconsistentie"),
    /** An input timeline as the asOfDate-bucket scalar the case gave, which {@link Gevolg.Stand} carries where it says
     *  so. */
    EMMERSCALAIR(Rekenhulpen.class, "emmerscalairVan"),
    /** The fault of the form of an input, its sentence around the object, named in a rule, which
     *  {@link Gevolg.Invoervormfout} inserts. */
    INVOERVORMFOUT(Rekenhulpen.class, "invoervormfout"),
    /** The keys of an instance in a source of a uniqueness, one per value a component on a timeline ever carries, which
     *  {@link Gevolg.Uniciteitssleutel} inserts. */
    UNICITEITSSLEUTELS(Rekenhulpen.class, "uniciteitssleutels"),
    /** The facts of the distribution of an allocation over the recipients of one source, computed by
     *  {@code Verdeling.bereken}: its deposits, its shares and its rest, which {@link Gevolg.Uitdeling} inserts. */
    UITDELING(Rekenhulpen.class, "uitdeling"),
    /** The contribution of a member to a fold by its value, or none where the value is leeg or the member's timeline
     *  stands as the asOfDate-bucket scalar, which {@link Gevolg.Inbreng.Lidwaarde} inserts. */
    BIJDRAGE(Rekenhulpen.class, "bijdrage"),
    /** The place of a member in a fold's order from the places its rule binds ({@link Gevolg.Vouwplaats}), which a
     *  filter fault, a member fault and a quantifier fold's test of a member carry; where the input states no place,
     *  the broken contract, named after what the rule needs the place for. */
    VOUWPLAATS(Rekenhulpen.class, "vouwplaats"),
    /** The creation depth of an object creation's new object, which {@link Gevolg.Creatiediepte} carries the arguments
     *  of; past the budget the fault at the making, {@code Creatiebudget}, whose constructor is not public. */
    CREATIEDIEPTE(Rekenhulpen.class, "creatiediepte");

    private final Class<?> klasse;
    private final String methode;

    Route(Class<?> klasse, String methode) {
        this.klasse = klasse;
        this.methode = methode;
    }

    /** The class whose public static method this is. */
    public Class<?> klasse() { return klasse; }

    /** The name of the method. */
    public String methode() { return methode; }
}
