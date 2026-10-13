package dev.uchar.regelspraak.transpiler.tms.plan;

/**
 * What one slot of a rule's value array holds, as the analysis decided to read it. Each kind comes from one method of
 * {@code TmsTranspiler.Lezingen}, named per constant; a method that decides between two ways of reading gives a kind per
 * way. A validity window ({@code Lezingen.geldig}) gates the rule on the evaluation date and binds no slot, so it is a
 * pattern of the left-hand side and no kind of this table. Some kinds come from outside {@code Lezingen}, from the
 * method each names: the places that {@code TmsTranspiler.plaats} binds in the range of a filter fault rule, with a
 * pattern outside the plan until step 1c of card drl-teksttak-op-truth-maintenance, have a slot all the same, since
 * the table holds every slot; so do the reads of the rules that a form composes itself, an initialisation's guard, the
 * rule of two writers and the standing rules.
 */
public enum Leessoort {
    /** The subject itself, for a consequence that hands it on ({@code lid}). */
    ONDERWERP,
    /** The input of a member of the subject, bound off its bean: of a member that no form writes ({@code eigen}), and
     *  the input that a member's standing carries where no derivation stands
     *  ({@code TmsTranspiler.invoerstandregel}). */
    INVOER,
    /** A derived attribute of the subject, through its {@code Stand} ({@code eigen}). */
    STAND,
    /** A derived characteristic of the subject, through its {@code KenmerkStand} ({@code eigen}). */
    KENMERKSTAND,
    /** The timeline of a member of the subject: an attribute through its {@code Stand}, a characteristic as its true
     *  periods off the bean ({@code tijdlijn}). */
    TIJDLIJN,
    /** A member at the end of a path, through its {@code PadStand} ({@code genavigeerd}). */
    PADSTAND,
    /** An input member at the end of a path of input links, bound off the beans ({@code genavigeerd}). */
    INVOERPAD,
    /** A parameter, through its {@code ParameterStand} ({@code parameter}). */
    PARAMETER,
    /** The session's evaluation date, through its {@code Rekendatum} ({@code rekendatum}). */
    REKENDATUM,
    /** The number of contributions to a fold on the subject ({@code vouw}). */
    VOUW_AANTAL,
    /** The folded value of the contributions ({@code vouw}). */
    VOUW_WAARDE,
    /** The least filter fault of a fold on the subject ({@code vouw}). */
    FILTERFOUT,
    /** The number of filter deposits of a fold on the subject ({@code vouw}). */
    FILTERDEPONERING,
    /** The least member fault of a fold on the subject ({@code vouw}). */
    LIDFOUT,
    /** The standing of the §9.5 channel of a form, through its {@code KanaalStand} ({@code kanaal}). */
    KANAAL,
    /** The number of the creation phase the driver opened ({@code creatiefase}). */
    CREATIEFASE,
    /** The marks of the creation that made the subject ({@code herkomst}). */
    HERKOMST,
    /** The recipients of an allocation from the subject ({@code ontvangers}). */
    ONTVANGERS,
    /** Whether a reference field of the subject is set ({@code rolveld}). */
    ROLVELD,
    /** The number of beans whose reference field names the subject ({@code rolvullers}). */
    ROLVULLERS,
    /** The number of links of a created relation that hold the subject ({@code rolkoppelingen}). */
    ROLKOPPELINGEN,
    /** The parent of the subject in the scope of a uniqueness ({@code scopelezing}). */
    SCOPE,
    /** The binder of a {@code Let}: no pattern binds it ({@code binder}). */
    BINDER,
    /** The probe of the rule's deposits: no pattern binds it ({@code sonde}). */
    SONDE,
    /** The number of places a to-many step's member has in its list ({@code TmsTranspiler.plaats}). */
    LIJSTPLAATSEN,
    /** The least place a to-many step's member has in its list ({@code TmsTranspiler.plaats}). */
    LIJSTPLAATS,
    /** The number of other writers of an initialisation's target that wrote leeg on the subject
     *  ({@code TmsTranspiler.initialisatieRegel}). */
    LEGE_SCHRIJVERS,
    /** The value facts that the writers of one attribute put on the subject ({@code TmsTranspiler.verschilregel}). */
    WAARDEN,
    /** The value a writer put on the subject, off its {@code Waarde}: what the standing of a derived attribute carries
     *  ({@code TmsTranspiler.attribuutstandregels}). */
    AFLEIDING,
    /** The standing of a member on the object a path reaches, off its {@code Stand} or {@code KenmerkStand}: what the
     *  standing of the path carries ({@code TmsTranspiler.padstandregels}). */
    BEREIKTE_STAND,
    /** The input of a member on the object a path over a relation a form creates reaches, off that bean as the
     *  binding's form of reading says: what the standing of such a path carries where no form writes the member
     *  ({@code TmsTranspiler.koppelingpadstandregels}). */
    BEREIKTE_INVOER,
    /** The number of keys of a uniqueness that collide with the key the rule binds, itself included, off an accumulate
     *  ({@code TmsTranspiler.uniciteitsregels}). */
    BOTSINGEN
}
