package dev.uchar.regelspraak.transpiler.tms.plan;

import dev.uchar.regelspraak.transpiler.tms.Rekenhulpen;

/**
 * A public static method of the branch that a rendering calls around the functions of a term ({@link Functie}), by its
 * class and its name: what a test of deposits and a consequence do beside the evaluation of a term, and the facts a
 * consequence makes whose class or constructor is not public; and what a DRL text needs where MVEL has no construct of
 * its own: catching, naming and throwing an evaluation fault, reading a bean and walking a path, the cell of a
 * {@code Let}, building a list. A DRL text calls each class-qualified, never through an {@code import function}, which a
 * field of a fact with the same name shadows without a complaint (FABLE-2 §2.1); no route is overloaded, so a call by
 * name finds one method. The rendering into closures calls those it has a use for, so that the two renderings run one
 * implementation.
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
    CREATIEDIEPTE(Rekenhulpen.class, "creatiediepte"),
    /** Whether the outcome of an evaluation is an evaluation fault, which a consequence inserts as a fault. */
    IS_EVALUATIEFOUT(Rekenhulpen.class, "isEvaluatiefout"),
    /** An exception caught in an evaluation as its outcome where it is an evaluation fault; any other passes on. */
    GEVANGEN(Rekenhulpen.class, "gevangen"),
    /** An evaluation fault named after the rule it ran in, as a fault carries it. */
    BENOEM(Rekenhulpen.class, "benoem"),
    /** The evaluation fault of a sentence, thrown ({@link Term.Faal}). */
    FAAL(Rekenhulpen.class, "faal"),
    /** A value, and the evaluation fault of a sentence where it is leeg ({@link Term.Vereist}). */
    VEREIST(Rekenhulpen.class, "vereist"),
    /** Texts joined in order ({@link Term.Tekstreeks}). */
    TEKSTREEKS(Rekenhulpen.class, "tekstreeks"),
    /** A member of a bean by its property ({@link Binding.Veld#eigenschap}), null on an absent object. */
    LEES(Rekenhulpen.class, "lees"),
    /** The object a walk over reference fields reaches, null where a link is absent: a read along a path. */
    LANGS(Rekenhulpen.class, "langs"),
    /** A characteristic read off a bean as whether it holds {@code true} ({@link Binding.Leesvorm#WAAR}). */
    WAAR(Rekenhulpen.class, "waar"),
    /** The true periods of a characteristic on a timeline ({@link Binding.Leesvorm#TIJDVAKKEN}). */
    TIJDVAKKEN(Rekenhulpen.class, "tijdvakken"),
    /** Whether a reference field is set, to an instance of a named class ({@link Binding.Leesvorm#GEZET}). */
    GEZET(Rekenhulpen.class, "gezet"),
    /** Whether the evaluation date lies in a validity window ({@link Alfa.Vergelijker#BINNEN}). */
    BINNEN(Rekenhulpen.class, "binnen"),
    /** Whether an object is an instance of a named class ({@link Alfa.Vergelijker#IS_EEN}). */
    IS_EEN(Rekenhulpen.class, "isEen"),
    /** Whether the own class of an object has none of the names ({@link Alfa.Vergelijker#KLASSE_NIET_IN}). */
    KLASSE_NIET_IN(Rekenhulpen.class, "klasseNietIn"),
    /** A fresh cell for the binder of one evaluation of a {@link Term.Let}. */
    BINDERCEL(Rekenhulpen.class, "bindercel"),
    /** Whether the cell of a binder holds its value. */
    BINDERCEL_GEVULD(Rekenhulpen.class, "bindercelGevuld"),
    /** Fills the cell of a binder with its value. */
    VUL_BINDERCEL(Rekenhulpen.class, "vulBindercel"),
    /** The value of a binder, or the barrier of its {@code Let} where it is leeg ({@code Leegbarriere}). */
    BINDERWAARDE(Rekenhulpen.class, "binderwaarde"),
    /** The outcome of a {@code Let} its barrier ended: leeg where the barrier is its own; another passes on. */
    OPGEVANGEN(Rekenhulpen.class, "opgevangen"),
    /** The verdict of a matrix as a member's contribution, 1 or 0 ({@link Gevolg.Inbreng.Oordeel}). */
    OORDEEL(Rekenhulpen.class, "oordeel"),
    /** A broken contract, thrown: no evaluation fault. */
    CONTRACTBREUK(Rekenhulpen.class, "contractbreuk"),
    /** The fault of writers that disagree, as the {@link Gevolg.Schrijversconflict} plans it. */
    SCHRIJVERSCONFLICT(Rekenhulpen.class, "schrijversconflict"),
    /** The empty list, for a route that takes a list. */
    LIJST0(Rekenhulpen.class, "lijst0"),
    /** A list of one value. */
    LIJST1(Rekenhulpen.class, "lijst1"),
    /** A list of two values. */
    LIJST2(Rekenhulpen.class, "lijst2"),
    /** A list of three values. */
    LIJST3(Rekenhulpen.class, "lijst3"),
    /** A list of four values. */
    LIJST4(Rekenhulpen.class, "lijst4"),
    /** A list of five values. */
    LIJST5(Rekenhulpen.class, "lijst5"),
    /** A list of six values. */
    LIJST6(Rekenhulpen.class, "lijst6");

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
