package dev.uchar.regelspraak.transpiler.tms.plan;

import dev.uchar.regelspraak.graph.ProducerConsumer.ElementKey;
import dev.uchar.regelspraak.ir.ResolvedAllocation;
import dev.uchar.regelspraak.transpiler.tms.Outcome.Kind;

import java.util.ArrayList;
import java.util.List;

/**
 * What a rule does when it fires, as the analysis plans it, in a closed vocabulary: which fact it inserts about which
 * bound object, with which key, which writer and which name, how it computes a value from the bound values, and how it
 * inserts. Every decision is a field, so a rendering into closures or into DRL text only spells it out. An object a
 * consequence names is the variable that binds it in the left-hand side ({@code subject}); a value it reads is a slot
 * of the rule's value array.
 *
 * <p>The kinds are those of the ordinary rules and the standings of parameters and the evaluation date (step 1c-A1 of
 * card drl-teksttak-op-truth-maintenance), and the standings of characteristics, attributes and paths (step 1c-A2),
 * where the source of a standing is a field ({@link StateSource}), and those of a consistency, a uniqueness, the §9.5
 * channel and an allocation (step 1c-B1): the key and the violation of a uniqueness, the fault of a key's form, the
 * standing of a channel, the recipient of an allocation and its distribution, whose computation is a route
 * ({@link Route}) the plan names with its arguments; and those of the rules of a fold (step 1c-B2): a member's
 * contribution, with what it contributes as a kind of its own ({@link Input}), and the intermediate facts of a
 * fold's filter and members, with the member's place in the fold's order ({@link FoldPlace}); and those of an object
 * creation, a link and the fault of a second filler of a to-one role (step 1c-C1), where a creation constructs its new
 * object before it inserts anything ({@link Creation}); and those of the rules of a defaults stack (step 1c-C2): the
 * decision of a default or of a reconciliation ({@link Decision}) and the conflict of a reconciliation ({@link Conflict}),
 * each with the facts it inserts as a kind of their own ({@link StackFact}): the target, and the control facts by
 * which the rules of the stack displace one another.
 */
public sealed interface Consequence {

    /** How a fact goes into working memory: logically, justified by the match and withdrawn with it, or stated, where
     *  it stays when the match goes. */
    enum Insertion { LOGICAL, STATED }

    /** The names of a fault: the form whose fault it is ({@code Fault.rule}) and the rule its sentence names. */
    record FaultName(String form, String rule) {}

    /** The §9.5 deposit (RUNTIME-SEMANTICS ch. 1.2) of the evaluation of a value: the probe in slot {@code depositFlag} of the
     *  value array hears the places the evaluation reaches, and where it heard one the consequence inserts the
     *  inconsistency of the form {@code form} on its object. */
    record Deposit(int depositFlag, String form) {}

    /**
     * The value of attribute {@code target} of the object {@code subject} binds, written by {@code writer}: the
     * consequence evaluates {@code value} over a value array of {@code size} slots with the bound values at
     * {@code slots}, collapses an exact rational to a decimal where {@code collapse}, and inserts the value where it is
     * gevuld, or also where it is leeg (null, or a timeline without a segment) where {@code writesEmpty}. Where the
     * evaluation faults, an evaluation fault (RUNTIME-SEMANTICS ch. 5.2), it inserts that fault under {@code fault} in
     * place of the value and of a deposit; otherwise the deposit first ({@code deposit}: the rule's probe, which
     * hears only the places the evaluation of the value reaches, so a probe of the condition alone hears nothing here;
     * null where the rule has no probe).
     */
    record Value(String subject, ElementKey target, String writer, Term value, List<Integer> slots,
                  int size, boolean collapse, boolean writesEmpty, Deposit deposit, FaultName fault,
                  Insertion insertion) implements Consequence {
        public Value {
            slots = List.copyOf(slots);
        }
    }

    /** The characteristic {@code characteristic} of the object {@code subject} binds. */
    record Characteristic(String subject, ElementKey characteristic, Insertion insertion) implements Consequence {}

    /**
     * The fault of a condition, evaluated again over the value array of {@code size} slots with the bound values at
     * {@code slots}, asking it {@code role}: its evaluation fault ({@link TestItem.Role#FAULT}), which the test of the
     * rule found, inserted under {@code fault} on the object {@code subject} binds. That the evaluation answers here is
     * a broken contract, since the test faulted over the same values.
     */
    record TestFault(String subject, Term test, TestItem.Role role, List<Integer> slots, int size, FaultName fault,
                     Insertion insertion) implements Consequence {
        public TestFault {
            slots = List.copyOf(slots);
            if (role != TestItem.Role.FAULT)
                throw new IllegalArgumentException("een gevolg vraagt van een voorwaarde alleen haar fout, niet " + role);
        }
    }

    /** A fixed evaluation fault of the branch ({@code ComputeFault}) with the sentence {@code sentence}, under {@code fault}, on
     *  the object {@code subject} binds. */
    record Fault(String subject, String sentence, FaultName fault, Insertion insertion) implements Consequence {}

    /** The §9.5 inconsistency of the form {@code form} on the object {@code subject} binds, with {@code codes}, the EF
     *  codes of the kinds it deposits (RUNTIME-SEMANTICS ch. 5.6, 5.7b), known when the plan is made. */
    record Inconsistency(String subject, String form, List<String> codes, Insertion insertion) implements Consequence {
        public Inconsistency {
            codes = List.copyOf(codes);
        }
    }

    /**
     * The §9.5 inconsistency of the form {@code form} on the object {@code subject} binds, deposited by its condition
     * {@code condition}, which the test of the rule found to deposit: the consequence evaluates it again over the value
     * array of {@code size} slots with the bound values at {@code slots} and a fresh probe in slot {@code depositFlag},
     * and the inconsistency carries the codes the probe heard (RUNTIME-SEMANTICS ch. 5.6, 5.7b). The codes are not
     * known when the plan is made: one division deposits EF-031 over a leeg divisor and EF-005 over a numeric zero. A
     * probe that hears nothing here is a broken contract.
     */
    record ConditionDeposit(String subject, String form, Term condition, int depositFlag, List<Integer> slots, int size,
                            Insertion insertion) implements Consequence {
        public ConditionDeposit {
            slots = List.copyOf(slots);
        }
    }

    /**
     * The fault of two or more writers of attribute {@code target} that stand on one object with different values
     * (RUNTIME-SEMANTICS ch. 5.7), on the object {@code subject} binds, over the value facts the slot {@code waarden}
     * holds: the writers in the order of the document's rules {@code documentOrder}, each named once, and a writer
     * the document has no rule of after them by name; the attribute named {@code attribute} as declared; and the
     * object's own type, named by the type in {@code types} whose bean class's simple name stands at the same place in
     * {@code typeClasses}, the types of the document in its order. The fault is the first writer's, made by the route
     * {@link Route#WRITERS_CONFLICT}.
     */
    record WritersConflict(String subject, int values, ElementKey target, String attribute,
                              List<String> documentOrder, List<String> typeClasses, List<String> types,
                              Insertion insertion) implements Consequence {
        public WritersConflict {
            documentOrder = List.copyOf(documentOrder);
            typeClasses = List.copyOf(typeClasses);
            types = List.copyOf(types);
            if (typeClasses.size() != types.size())
                throw new IllegalArgumentException("de typen en hun klassen staan in paren: " + typeClasses + ", "
                        + types);
        }
    }

    /** The standing of parameter {@code parameter}, whose value the global {@code global} holds: a rule without a
     *  left-hand side, which fires once per session. */
    record InsertParameterState(String parameter, String global, Insertion insertion) implements Consequence {}

    /** The standing of the session's evaluation date, which the global {@code global} holds, null where the case
     *  carries none: a rule without a left-hand side, which fires once per session. */
    record EvaluationDateState(String global, Insertion insertion) implements Consequence {}

    /** Where a standing takes what it states from, as the left-hand side of its rule decided: the input on the object's
     *  bean ({@code INPUT}); a derivation, the fact a form inserted ({@code DERIVATION}); neither, so that a
     *  characteristic is onwaar ({@code GEEN}); the standing of a member on the object a path reaches
     *  ({@code REACHED}); or no object, where a link on the path is absent ({@code MISSING_LINK}). */
    enum StateSource { INPUT, DERIVATION, NONE, REACHED, MISSING_LINK }

    /**
     * The standing of characteristic {@code characteristic} on the object {@code subject} binds, {@code waar} or onwaar:
     * waar from the input or from a derivation ({@code source}), onwaar where neither holds. Where both hold, the two
     * waar standings are equal facts, which truth maintenance keeps as one with a justification each.
     */
    record InsertCharacteristicState(String subject, ElementKey characteristic, StateSource source, boolean holds, Insertion insertion)
            implements Consequence {
        public InsertCharacteristicState {
            if (source != StateSource.INPUT && source != StateSource.DERIVATION && source != StateSource.NONE
                    || holds == (source == StateSource.NONE))
                throw new IllegalArgumentException("een kenmerkstand is waar uit de invoer of een afleiding en onwaar"
                        + " uit geen van beide, niet " + holds + " uit " + source);
        }
    }

    /**
     * The standing of attribute {@code attribute} on the object {@code subject} binds: the value in slot
     * {@code value} of the value array, from {@code source}, a derivation's value or the input on the bean. Where
     * {@code bucketScalar}, the input, a timeline, stands as the asOfDate-bucket scalar the case gave
     * ({@code BucketScalar}, RUNTIME-SEMANTICS ch. 8.4), which the consequence takes from the bound timeline.
     */
    record State(String subject, ElementKey attribute, StateSource source, int value, boolean bucketScalar,
                 Insertion insertion) implements Consequence {
        public State {
            if (source != StateSource.INPUT && source != StateSource.DERIVATION || bucketScalar && source != StateSource.INPUT)
                throw new IllegalArgumentException("een stand komt uit de invoer of een afleiding, en alleen de invoer"
                        + " staat als emmerscalair, niet " + source + (bucketScalar ? " als emmerscalair" : ""));
        }
    }

    /**
     * The standing of member {@code member} at the end of the path {@code path} on the object {@code subject} binds:
     * where every link on the path is present ({@code source} {@code REACHED}), the value in slot {@code value} of the
     * value array, the member's standing on the object the path reaches; where a link is absent
     * ({@code MISSING_LINK}), null, and {@code value} is -1.
     */
    record InsertPathState(String subject, String path, ElementKey member, StateSource source, int value, Insertion insertion)
            implements Consequence {
        public InsertPathState {
            if (source != StateSource.REACHED && source != StateSource.MISSING_LINK
                    || (source == StateSource.REACHED) != (value >= 0))
                throw new IllegalArgumentException("een padstand neemt de stand op het bereikte object uit een slot, of"
                        + " is leeg bij een ontbrekende link, zonder slot; niet " + source + " met slot " + value);
        }
    }

    /**
     * The keys of the object {@code subject} binds in source {@code source} of the uniqueness {@code uniqueness}
     * (RUNTIME-SEMANTICS ch. 5.6, 5.9), made by the route {@link Route#UNIQUENESS_KEYS}: its key components in the
     * source's order, each the value of its term in {@code components} over the value array of {@code size} slots
     * with the bound values at {@code slots}, an enumeration label in its normalised form; the parent in slot
     * {@code scope}, -1 for a form without a scope; and where the component at position {@code timeline} is on a
     * timeline (-1: none), one key per value that timeline ever carries (ever-uniqueness).
     */
    record UniquenessKey(String subject, String uniqueness, int source, List<Term> components, int timeline,
                             int scope, List<Integer> slots, int size, Insertion insertion) implements Consequence {
        public UniquenessKey {
            components = List.copyOf(components);
            slots = List.copyOf(slots);
        }
    }

    /** The violation of the uniqueness {@code uniqueness} by the object {@code subject} binds, a key of which is in a
     *  duplicate (ch. 5.9). */
    record UniquenessViolation(String subject, String uniqueness, Insertion insertion) implements Consequence {}

    /** The fault of the form of an input on the object {@code subject} binds, the sentence {@code voor}, the object
     *  and {@code na}, under {@code fault}, made by the route {@link Route#INPUT_FORM_FAULT}. */
    record InputFormFault(String subject, String before, String after, FaultName fault, Insertion insertion)
            implements Consequence {}

    /** The standing of the §9.5 channel of the form {@code producer} (ch. 5.6), {@code inconsistent} or consistent: a
     *  rule without a subject, whose left-hand side tests whether an inconsistency of that form stands. */
    record InsertChannelState(String producer, boolean inconsistent, Insertion insertion) implements Consequence {}

    /** A recipient of the allocation {@code verdeling} for the source {@code subject} binds (ch. 4.1): the recipient
     *  in slot {@code recipient}, and its weight, its ordering key and its cap in the slots {@code weight},
     *  {@code key} and {@code maximum}, each -1 where the allocation declares none, so that the fact carries null
     *  there. */
    record Recipient(String subject, String allocation, int recipient, int weight, int key, int maximum,
                     Insertion insertion) implements Consequence {}

    /**
     * The distribution of an allocation over the recipients of the source {@code subject} binds (ch. 4.2 to 4.4),
     * computed by the route {@link Route#PAYOUT}, which calls {@code Allocation.bereken}: by {@code method}, of the
     * pot in slot {@code pot} over the list of {@code Recipient} facts in slot {@code list}, {@code gewogen} where the
     * allocation declares a ratio attribute, {@code capped} where it declares a cap, {@code descending} the ordered
     * method's direction, {@code decimals} the floor's decimals (null: none). Each share is a value of {@code aandeel}
     * on its recipient and the rest a value of {@code rest} on the source (null: the allocation writes no rest), both
     * written by {@code writer}; each degradation is the §9.5 inconsistency of the form {@code form} on its
     * recipient.
     */
    record Payout(String subject, String form, ResolvedAllocation.Method method, int pot, int list,
                     boolean gewogen, boolean capped, boolean descending, Integer decimals, ElementKey aandeel,
                     ElementKey rest, String writer, Insertion insertion) implements Consequence {}

    /**
     * A member's place in the order of a fold, as a rule of the fold binds it, computed by the route
     * {@link Route#FOLD_PLACE}: the number {@code selection} of the selection of an instance-set union (0 for any other
     * fold), then per to-many step of the member's route, in order, the least place of the step's member in its list
     * ({@link Step}). {@code reason} is what the rule needs the place for, which names the broken contract where the
     * input states no place of a step.
     */
    record FoldPlace(int selection, List<Step> steps, PlaceReason reason) {
        public FoldPlace {
            steps = List.copyOf(steps);
        }

        /** One to-many step: the reference field {@code field} its member links through, and the slots of the count of
         *  that member's {@code ListPlace} facts under the field and of the least place among them. */
        public record Step(String field, int count, int place) {}
    }

    /** What a rule of a fold needs a member's place for: the first fault among the members' filters
     *  ({@code FILTER_FAULT}), the first member whose timeline the case gives as the asOfDate-bucket scalar
     *  ({@code MEMBER_FAULT}), or the members that the short circuit of a quantifier fold reaches ({@code QUANTIFIER}). */
    enum PlaceReason { FILTER_FAULT, MEMBER_FAULT, QUANTIFIER }

    /**
     * The contribution ({@code Contribution}, RUNTIME-SEMANTICS ch. 3.4) of the member in slot {@code member}, the subject of a
     * contribution rule, to the fold {@code fold} of the fold's subject, the object {@code subject} binds: what the
     * member contributes is {@code input}, computed over a value array of {@code size} slots with the bound values
     * at {@code slots}.
     */
    record Contribution(String subject, String fold, int member, Input input, List<Integer> slots, int size,
                    Insertion insertion) implements Consequence {
        public Contribution {
            slots = List.copyOf(slots);
        }
    }

    /** What a member contributes to its fold, as the analysis decided from the fold's function. */
    sealed interface Input {
        /** The member itself, without a value: a count of the members. */
        record Count() implements Input {}

        /** The member's value in slot {@code value}, made by the route {@link Route#CONTRIBUTION}: nothing where it is
         *  leeg; where {@code total} is not null the slot holds the member's timeline, which contributes its total over
         *  time per that unit, and nothing where the case gives it as the asOfDate-bucket scalar; where {@code filled},
         *  the member without a value, a count of the members whose value is gevuld. */
        record MemberValue(int value, String total, boolean filled) implements Input {}

        /** The verdict of the matrix of a quantifier fold for the member, the condition {@code matrix}, which cannot
         *  fault or deposit: 1 where it holds, 0 where it does not. */
        record Verdict(Term matrix) implements Input {}

        /** The test of the member by the matrix of a quantifier fold, the condition {@code matrix}, which can fault or
         *  deposit, as a {@code MemberTest}: the member's {@code place} in the order of the members, then the verdict or
         *  the evaluation fault of the matrix, and whether the probe the consequence puts in slot {@code depositFlag} (-1:
         *  none, the matrix deposits nothing) heard a place that deposits. The place comes first, so a member without a
         *  place breaks the contract before the matrix is evaluated. */
        record MemberTest(Term matrix, int depositFlag, FoldPlace place) implements Input {
            public MemberTest {
                if (place.reason() != PlaceReason.QUANTIFIER)
                    throw new IllegalArgumentException("de plaats van een lidtoets is die van de kwantor, niet "
                            + place.reason());
            }
        }
    }

    /** That evaluating the filter of the fold {@code fold} for the member in slot {@code member} deposits, a
     *  {@code FilterDeposit} on the fold's subject, the object {@code subject} binds; the test of the rule found it.
     *  The consequence evaluates the condition {@code filter} again over the value array of {@code size} slots with the
     *  bound values at {@code slots} and a fresh probe in slot {@code depositFlag}, and the fact carries the codes the
     *  probe heard, as {@link ConditionDeposit} does. */
    record FilterDeposit(String subject, String fold, int member, Term filter, int depositFlag, List<Integer> slots,
                         int size, Insertion insertion) implements Consequence {
        public FilterDeposit {
            slots = List.copyOf(slots);
        }
    }

    /**
     * The fault of the filter of the fold {@code fold} for the member in slot {@code member}, a {@code FilterFault} on the
     * fold's subject, the object {@code subject} binds: the condition {@code filter}, evaluated again over the value
     * array of {@code size} slots with the bound values at {@code slots}, gives its evaluation fault, which the test
     * of the rule found, so that it answering here is a broken contract; then the member's {@code place} in the fold's
     * order.
     */
    record FilterFault(String subject, String fold, int member, Term filter, FoldPlace place, List<Integer> slots,
                      int size, Insertion insertion) implements Consequence {
        public FilterFault {
            slots = List.copyOf(slots);
            if (place.reason() != PlaceReason.FILTER_FAULT)
                throw new IllegalArgumentException("de plaats van een filterfout is die van het filter, niet "
                        + place.reason());
        }
    }

    /** The fault of the member in slot {@code member} of the per-element fold {@code fold} over an attribute on a
     *  timeline, which the case gives as the asOfDate-bucket scalar, a {@code MemberFault} on the fold's subject, the object
     *  {@code subject} binds: the member's {@code place} in the fold's order, and the fault of the form of an input,
     *  the sentence {@code voor}, the member and {@code na}, named in no rule yet, made by the route
     *  {@link Route#INPUT_FORM_FAULT}. */
    record MemberFault(String subject, String fold, int member, FoldPlace place, String before, String after,
                   Insertion insertion) implements Consequence {
        public MemberFault {
            if (place.reason() != PlaceReason.MEMBER_FAULT)
                throw new IllegalArgumentException("de plaats van een lidfout is die van het lid, niet " + place.reason());
        }
    }

    /**
     * An object creation (RUNTIME-SEMANTICS ch. 5.5, IR-SPEC ch. 3.3) of the creation {@code creation} for the subject the
     * variable {@code subject} binds, over a value array of {@code size} slots with the bound values at
     * {@code slots}. Its consequence takes four steps, in this order:
     * <ol>
     *   <li>the creation depth of the new object, made by the route {@link Route#CREATION_DEPTH} from {@code depth}:
     *       past the budget it throws, the fault at the making, before anything is made;</li>
     *   <li>the value of each of {@code initialisations}, in order, each collapsed from an exact rational to a decimal;
     *       where one raises an evaluation fault (RUNTIME-SEMANTICS ch. 5.2), that fault, named under {@code fault} and
     *       inserted on the subject as {@code faultInsertion}, is all the creation does, as in the reference
     *       interpreter, which then makes nothing;</li>
     *   <li>the construction of the new object ({@code construction}): a bean of its class, on which the attribute
     *       initialisations whose value is gevuld are set before the bean is inserted. That is construction and no
     *       write on a bean in working memory: the bean is in none yet, so no rule ever saw it without them;</li>
     *   <li>the insertions {@code insertions}, in order, the new object itself first ({@link Insert}).</li>
     * </ol>
     * The constructor holds the plan to that order, the order of the consequence before the step exactly: every
     * initialisation is set in the construction where it is an attribute, or inserted as a characteristic after the new
     * object where it is one, exactly once; the attributes are set in the order of the initialisations; the new object is
     * inserted first, then its mark, then its link, then the characteristics in the order of the initialisations. Any
     * other order fails loudly. A creation states everything it makes, the fault at the making among it
     * (RUNTIME-SEMANTICS ch. 5.5): the constructor refuses an insertion that is not {@link Insertion#STATED}.
     */
    record Creation(String subject, String creation, CreationDepth depth, List<Initialisation> initialisations,
                   FaultName fault, Insertion faultInsertion, Construction construction, int phase,
                   List<Insert> insertions, List<Integer> slots, int size) implements Consequence {
        public Creation {
            initialisations = List.copyOf(initialisations);
            insertions = List.copyOf(insertions);
            slots = List.copyOf(slots);
            // a logical insertion lapses when the match of its rule goes, and the match of a creation rule goes as soon
            // as the creation is made: its mark and its fault are the guard of the rule, so each logical fact would be
            // withdrawn right after it went in, and the creation would fire again
            for (Insert i : insertions)
                if (i.insertion() != Insertion.STATED)
                    throw new IllegalArgumentException("een creatie stelt wat ze maakt, want dat vervalt nooit: "
                            + i + " is niet gesteld");
            if (faultInsertion != Insertion.STATED)
                throw new IllegalArgumentException("een creatie stelt haar fout bij het maken, want die houdt haar"
                        + " regel tegen: de foutinvoeging " + faultInsertion + " is niet gesteld");
            int[] time = new int[initialisations.size()];
            for (Construction.Setting z : construction.settings()) {
                if (initialisations.get(z.initialisation()).member().characteristic())
                    throw new IllegalArgumentException("een kenmerkinitialisatie wordt ingevoegd, niet gezet: "
                            + initialisations.get(z.initialisation()).member());
                time[z.initialisation()]++;
            }
            if (insertions.isEmpty() || !(insertions.get(0) instanceof Insert.New))
                throw new IllegalArgumentException("een creatie voegt eerst het nieuwe object in: " + insertions);
            int marks = 0, left = 0;
            for (Insert i : insertions)
                switch (i) {
                    case Insert.New n -> { }
                    case Insert.Mark m -> marks++;
                    case Insert.Link l -> left++;
                    case Insert.Characteristic k -> {
                        if (!initialisations.get(k.initialisation()).member().characteristic())
                            throw new IllegalArgumentException("een attribuutinitialisatie wordt gezet, niet ingevoegd: "
                                    + initialisations.get(k.initialisation()).member());
                        time[k.initialisation()]++;
                    }
                }
            if (insertions.stream().filter(i -> i instanceof Insert.New).count() != 1 || marks != 1
                    || left != 1)
                throw new IllegalArgumentException("een creatie voegt het nieuwe object, zijn merk en zijn koppeling elk"
                        + " één keer in: " + insertions);
            for (int i = 0; i < time.length; i++)
                if (time[i] != 1)
                    throw new IllegalArgumentException("de initialisatie van " + initialisations.get(i).member() + " komt "
                            + time[i] + " keer in de constructie of de invoegingen, niet één keer");
            // the order of the consequence before step 1c-C1, which a rendering carries out as the plan lists it: a
            // rendering into DRL text has no other source for the order of the facts the agenda then sees
            List<Integer> attributes = new ArrayList<>(), characteristics = new ArrayList<>();
            for (int i = 0; i < initialisations.size(); i++)
                (initialisations.get(i).member().characteristic() ? characteristics : attributes).add(i);
            List<Integer> setInitialisations = construction.settings().stream().map(Construction.Setting::initialisation).toList();
            if (!setInitialisations.equals(attributes))
                throw new IllegalArgumentException("een creatie zet haar attribuutinitialisaties in hun volgorde "
                        + attributes + ", niet " + setInitialisations);
            List<String> order = new ArrayList<>(List.of("New", "Mark", "Link"));
            for (int k : characteristics) order.add("Kenmerk " + k);
            List<String> given = insertions.stream().map(i -> i instanceof Insert.Characteristic k
                    ? "Kenmerk " + k.initialisation() : i.getClass().getSimpleName()).toList();
            if (!given.equals(order))
                throw new IllegalArgumentException("een creatie voegt het nieuwe object, zijn merk, zijn koppeling en"
                        + " dan haar kenmerkinitialisaties in hun volgorde in, " + order + ", niet " + given);
        }
    }

    /** The creation depth of an object creation's new object (RUNTIME-SEMANTICS ch. 5.5, 6.3), which the route
     *  {@link Route#CREATION_DEPTH} computes: one more than the depth of the subject, of declared type {@code type}, in
     *  the creation's stratum {@code stratum}, from the subject's marks in slot {@code origin} of the value array; past
     *  {@code budget} the route throws the fault at the making. */
    record CreationDepth(int origin, int stratum, String type, int budget) {}

    /** One member initialisation of an object creation: the member {@code member} of the new object, and its value
     *  {@code value}, evaluated over the bound values of the rule. */
    record Initialisation(ElementKey member, Term value) {}

    /** How an object creation makes its new object: a bean of {@code javaClass}, by its constructor without arguments, on
     *  which each of {@code settings} is set, in order, before the bean is inserted. */
    record Construction(Class<?> javaClass, List<Setting> settings) {
        public Construction {
            settings = List.copyOf(settings);
        }

        /** The value of the attribute initialisation at index {@code initialisatie}, set on the new bean through the
         *  setter of the bean property {@code field} where the value is gevuld; a leeg value leaves the member unset. */
        public record Setting(int initialisation, Binding.Field field) {}
    }

    /** One insertion of an object creation, after its construction, with how it goes into working memory. */
    sealed interface Insert {
        Insertion insertion();

        /** The new object itself. */
        record New(Insertion insertion) implements Insert {}

        /** The new object's {@code CreationStamp}: the creation, the subject, the new object, the creation phase in slot
         *  {@link Creation#phase}, the stratum and the depth of {@link Creation#depth}. */
        record Mark(Insertion insertion) implements Insert {}

        /** The {@code RelationLink} of the relation {@code relation} between the new object and the subject, the new object
         *  its first end where {@code newIsFirst} and its second end otherwise, so the relation's order of roles
         *  decides the direction, carrying the creation phase in slot {@link Creation#phase}. */
        record Link(String relation, boolean newIsFirst, Insertion insertion) implements Insert {}

        /** The {@code HasCharacteristic} of the characteristic of the initialisation at index {@code initialisatie} on the
         *  new object, where its value is true. */
        record Characteristic(int initialisation, Insertion insertion) implements Insert {}
    }

    /** The link of the relation {@code relation} whose first end is the object the variable {@code first} binds and
     *  whose second end is the object {@code second} binds, so the relation's order of roles decides the direction,
     *  carrying the creation phase in slot {@code phase} of the value array, or phase 0, the input's, where {@code phase}
     *  is -1: a link a reference field on a bean of the input carries, or the link a fact creation makes. */
    record RelationLink(String relation, String first, String second, int phase, Insertion insertion) implements Consequence {}

    /** The fault of a second distinct filler of a to-one role (RUNTIME-SEMANTICS ch. 5.5), a fault of the program and
     *  no overwrite: on the object {@code subject} binds, the near end of two links of one relation to different
     *  objects, under the declared name of the relation {@code relation}, an {@code IllegalStateException} with the
     *  sentence {@code sentence}. */
    record SecondFilling(String subject, String relation, String sentence, Insertion insertion) implements Consequence {}

    /**
     * The decision of a default or of a reconciliation in a defaults stack (RUNTIME-SEMANTICS ch. 7.1-7.3, 7.9), on the
     * object {@code subject} binds: the value {@code value} evaluates to over a value array of {@code size} slots
     * with the bound values at {@code slots}, collapsed from an exact rational to a decimal where {@code collapse}. That
     * value is a characteristic's decision as the default declares it, a constant; an attribute's consequence, computed
     * from what the rule bound; or the decision of the first deciding member chain, bound off its outcome
     * ({@link ReadKind#DECISION}). The consequence inserts each of {@code facts} with it, in order, as {@code insertion}
     * says. Where the evaluation raises an evaluation fault (RUNTIME-SEMANTICS ch. 5.2), it inserts that fault under
     * {@code fault} on the object in place of the decision; {@code fault} is null where the value is a constant or a bound
     * decision, which cannot fault, and such a fault would be a broken contract. Where the value is null, a leeg
     * consequence, the default decides nothing and inserts nothing, so the lower defaults decide (ch. 7.1).
     */
    record Decision(String subject, Term value, List<Integer> slots, int size, boolean collapse, FaultName fault,
                  List<StackFact> facts, Insertion insertion) implements Consequence {
        public Decision {
            slots = List.copyOf(slots);
            facts = List.copyOf(facts);
            if (facts.isEmpty()) throw new IllegalArgumentException("een besluit voegt ten minste één feit in");
            for (StackFact f : facts)
                if (f instanceof StackFact.Inconsistency || f instanceof StackFact.Outcome u && u.category() != Kind.DECIDED)
                    throw new IllegalArgumentException("een besluit voegt geen conflict in: " + f);
        }
    }

    /**
     * The conflict of a reconciliation in a defaults stack (RUNTIME-SEMANTICS ch. 7.2, 7.9), on the object
     * {@code subject} binds, under the key {@code key} evaluates to over a value array of {@code size} slots
     * with the bound values at {@code slots}: the key of the reconciliation itself where two of its members decide
     * differently, a constant, or the key of the first conflicting member chain, bound off its outcome
     * ({@link ReadKind#CONFLICT_KEY}), which passes on unchanged. The consequence inserts {@code fact} with the key,
     * as {@code insertion} says: the outcome of the default that reconciles, of the kind {@code CONFLICT}, which the
     * reconciliation above it reads, or at the root of the stack the §9.5 inconsistency under the key.
     */
    record Conflict(String subject, Term key, List<Integer> slots, int size, StackFact fact,
                    Insertion insertion) implements Consequence {
        public Conflict {
            slots = List.copyOf(slots);
            if (!(fact instanceof StackFact.Inconsistency
                    || fact instanceof StackFact.Outcome u && u.category() == Kind.CONFLICT))
                throw new IllegalArgumentException("een conflict voegt zijn uitkomst of de inconsistentie in, niet " + fact);
        }
    }

    /** A fact the rules of a defaults stack insert on their subject with a decision or with the key of a conflict. */
    sealed interface StackFact {

        /** The value of the stack's target attribute {@code target}, the decision, written by {@code writer}, the
         *  stack: a {@code Value}. */
        record Value(ElementKey target, String writer) implements StackFact {}

        /** The attribution of the stack's target characteristic {@code characteristic}: a {@code HasCharacteristic}. The analysis
         *  plans it only where the decision is an attribution, true. */
        record Characteristic(ElementKey characteristic) implements StackFact {}

        /** The control fact of the simple form: default {@code index} of the stack {@code stack} decided, a
         *  {@code Decides}. The analysis plans it only where a lower default of the stack has a rule, which carries the
         *  negation of it over the indices above its own: the condition under which this decision displaces a lower
         *  one. */
        record Decides(String stack, int index) implements StackFact {}

        /** The control fact of the general form: the default at {@code position} in chain {@code chain} of the stack
         *  {@code stack} settled as {@code category} says, {@code DECIDED} with the decision as its value or
         *  {@code CONFLICT} with the key of the conflict: an {@code Outcome}. */
        record Outcome(String stack, int chain, int position, Kind category) implements StackFact {}

        /** The §9.5 inconsistency of the stack's conflict at its root, under the key of the conflict, through the
         *  route {@link Route#INCONSISTENCY}, with {@link #CODES}. */
        record Inconsistency() implements StackFact {
            /** EF-027: the defaults groups of a declared-unordered group decide differently (RUNTIME-SEMANTICS
             *  ch. 7.2, 7.9, 5.7b). */
            public static final List<String> CODES = List.of("EF-027");
        }
    }
}
