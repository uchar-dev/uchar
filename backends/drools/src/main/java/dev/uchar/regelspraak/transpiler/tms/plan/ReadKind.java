package dev.uchar.regelspraak.transpiler.tms.plan;

/**
 * What one slot of a rule's value array holds, as the analysis decided to read it. Each kind comes from one method of
 * {@code TmsTranspiler.Reads}, named per constant; a method that decides between two ways of reading gives a kind per
 * way. A validity window ({@code Reads.valid}) gates the rule on the evaluation date and binds no slot, so it is a
 * pattern of the left-hand side and no kind of this table. Some kinds come from outside {@code Reads}, from the
 * method each names: the places that {@code TmsTranspiler.place} binds in the range of a filter fault rule, with a
 * pattern outside the plan until step 1c of card drl-teksttak-op-truth-maintenance, have a slot all the same, since
 * the table holds every slot; so do the reads of the rules that a form composes itself, an initialisation's guard, the
 * rule of two writers and the standing rules.
 */
public enum ReadKind {
    /** The subject itself, for a consequence that hands it on ({@code lid}). */
    SUBJECT,
    /** The input of a member of the subject, bound off its bean: of a member that no form writes ({@code eigen}), and
     *  the input that a member's standing carries where no derivation stands
     *  ({@code TmsTranspiler.inputStateRule}). */
    INVOER,
    /** A derived attribute of the subject, through its {@code State} ({@code eigen}). */
    STATE,
    /** A derived characteristic of the subject, through its {@code CharacteristicState} ({@code eigen}). */
    CHARACTERISTIC_STATE,
    /** The timeline of a member of the subject: an attribute through its {@code State}, a characteristic as its true
     *  periods off the bean ({@code tijdlijn}). */
    TIJDLIJN,
    /** A member at the end of a path, through its {@code PathState} ({@code genavigeerd}). */
    PATH_STATE,
    /** An input member at the end of a path of input links, bound off the beans ({@code genavigeerd}). */
    INPUT_PATH,
    /** A parameter, through its {@code ParameterState} ({@code parameter}). */
    PARAMETER,
    /** The session's evaluation date, through its {@code EvaluationDate} ({@code rekendatum}). */
    REKENDATUM,
    /** The number of contributions to a fold on the subject ({@code vouw}). */
    FOLD_COUNT,
    /** The folded value of the contributions ({@code vouw}). */
    FOLDED_VALUE,
    /** The least filter fault of a fold on the subject ({@code vouw}). */
    FILTER_FAULT,
    /** The number of filter deposits of a fold on the subject ({@code vouw}). */
    FILTER_DEPOSIT,
    /** The least member fault of a fold on the subject ({@code vouw}). */
    MEMBER_FAULT,
    /** The standing of the §9.5 channel of a form, through its {@code ChannelState} ({@code kanaal}). */
    CHANNEL,
    /** The number of the creation phase the driver opened ({@code creatiefase}). */
    CREATION_PHASE,
    /** The marks of the creation that made the subject ({@code herkomst}). */
    ORIGIN,
    /** The recipients of an allocation from the subject ({@code ontvangers}). */
    RECIPIENTS,
    /** Whether a reference field of the subject is set ({@code rolveld}). */
    ROLE_FIELD,
    /** The number of beans whose reference field names the subject ({@code rolvullers}). */
    ROLE_FILLERS,
    /** The number of links of a created relation that hold the subject ({@code rolkoppelingen}). */
    ROLE_LINKS,
    /** The parent of the subject in the scope of a uniqueness ({@code scopelezing}). */
    SCOPE,
    /** The binder of a {@code Let}: no pattern binds it ({@code binder}). */
    BINDER,
    /** The probe of the rule's deposits: no pattern binds it ({@code sonde}). In a rule whose positions place no probe
     *  the slot stays, so the value array keeps its size, but no term names it ({@link Term}). */
    DEPOSIT_FLAG,
    /** The number of places a to-many step's member has in its list ({@code TmsTranspiler.place}). */
    LIST_PLACES,
    /** The least place a to-many step's member has in its list ({@code TmsTranspiler.place}). */
    LIST_PLACE,
    /** The number of other writers of an initialisation's target that wrote leeg on the subject
     *  ({@code TmsTranspiler.initialisationRule}). */
    EMPTY_WRITERS,
    /** The value facts that the writers of one attribute put on the subject ({@code TmsTranspiler.differenceRule}). */
    VALUES,
    /** The value a writer put on the subject, off its {@code Value}: what the standing of a derived attribute carries
     *  ({@code TmsTranspiler.attributeStateRules}). */
    DERIVATION,
    /** The standing of a member on the object a path reaches, off its {@code State} or {@code CharacteristicState}: what the
     *  standing of the path carries ({@code TmsTranspiler.pathStateRules}). */
    REACHED_STATE,
    /** The input of a member on the object a path over a relation a form creates reaches, off that bean as the
     *  binding's form of reading says: what the standing of such a path carries where no form writes the member
     *  ({@code TmsTranspiler.linkPathStateRules}). */
    REACHED_INPUT,
    /** The number of keys of a uniqueness that collide with the key the rule binds, itself included, off an accumulate
     *  ({@code TmsTranspiler.unionUniquenessRules}). */
    COLLISIONS,
    /** The decision of the first deciding member chain of a reconciliation of a defaults stack, off its
     *  {@code Outcome} ({@code TmsTranspiler.reconciliation}). */
    DECISION,
    /** The key of the first conflicting member chain of a reconciliation of a defaults stack, off its
     *  {@code Outcome}, which passes on unchanged ({@code TmsTranspiler.reconciliation}). */
    CONFLICT_KEY
}
