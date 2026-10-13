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
     *  {@link Gevolg.Waarde} insert. */
    INCONSISTENTIE(Rekenhulpen.class, "inconsistentie"),
    /** An input timeline as the asOfDate-bucket scalar the case gave, which {@link Gevolg.Stand} carries where it says
     *  so. */
    EMMERSCALAIR(Rekenhulpen.class, "emmerscalairVan");

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
