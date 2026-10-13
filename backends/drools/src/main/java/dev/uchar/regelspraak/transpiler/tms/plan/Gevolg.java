package dev.uchar.regelspraak.transpiler.tms.plan;

import dev.uchar.regelspraak.graaf.ProducentConsument.ElementSleutel;
import dev.uchar.regelspraak.ir.ResolvedVerdeling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What a rule does when it fires, as the analysis plans it, in a closed vocabulary: which fact it inserts about which
 * bound object, with which key, which writer and which name, how it computes a value from the bound values, and how it
 * inserts. Every decision is a field, so a rendering into closures or into DRL text only spells it out. An object a
 * consequence names is the variable that binds it in the left-hand side ({@code onderwerp}); a value it reads is a slot
 * of the rule's value array.
 *
 * <p>The kinds are those of the ordinary rules and the standings of parameters and the evaluation date (step 1c-A1 of
 * card drl-teksttak-op-truth-maintenance), and the standings of characteristics, attributes and paths (step 1c-A2),
 * where the source of a standing is a field ({@link Standbron}), and those of a consistency, a uniqueness, the §9.5
 * channel and an allocation (step 1c-B1): the key and the violation of a uniqueness, the fault of a key's form, the
 * standing of a channel, the recipient of an allocation and its distribution, whose computation is a route
 * ({@link Route}) the plan names with its arguments; and those of the rules of a fold (step 1c-B2): a member's
 * contribution, with what it contributes as a kind of its own ({@link Inbreng}), and the intermediate facts of a
 * fold's filter and members, with the member's place in the fold's order ({@link Vouwplaats}); and those of an object
 * creation, a link and the fault of a second filler of a to-one role (step 1c-C1), where a creation constructs its new
 * object before it inserts anything ({@link Creatie}). The consequences of the defaults are kinds of their own when
 * their step plans them; each names the fact it inserts and how, as these do.
 */
public sealed interface Gevolg {

    /** How a fact goes into working memory: logically, justified by the match and withdrawn with it, or stated, where
     *  it stays when the match goes. */
    enum Invoeging { LOGISCH, GESTELD }

    /** The names of a fault: the form whose fault it is ({@code Fout.regel}) and the rule its sentence names. */
    record Foutnaam(String vorm, String regel) {}

    /** The §9.5 deposit (RUNTIME-SEMANTICS ch. 1.2) of the evaluation of a value: the probe in slot {@code sonde} of the
     *  value array hears the places the evaluation reaches, and where it heard one the consequence inserts the
     *  inconsistency of the form {@code vorm} on its object. */
    record Deponering(int sonde, String vorm) {}

    /**
     * The value of attribute {@code doel} of the object {@code onderwerp} binds, written by {@code schrijver}: the
     * consequence evaluates {@code waarde} over a value array of {@code grootte} slots with the bound values at
     * {@code slots}, collapses an exact rational to a decimal where {@code collapse}, and inserts the value where it is
     * gevuld, or also where it is leeg (null, or a timeline without a segment) where {@code schrijftLeeg}. Where the
     * evaluation faults, an evaluation fault (RUNTIME-SEMANTICS ch. 5.2), it inserts that fault under {@code fout} in
     * place of the value and of a deposit; otherwise the deposit first ({@code deponering}: the rule's probe, which
     * hears only the places the evaluation of the value reaches, so a probe of the condition alone hears nothing here;
     * null where the rule has no probe).
     */
    record Waarde(String onderwerp, ElementSleutel doel, String schrijver, Term waarde, List<Integer> slots,
                  int grootte, boolean collapse, boolean schrijftLeeg, Deponering deponering, Foutnaam fout,
                  Invoeging invoeging) implements Gevolg {
        public Waarde {
            slots = List.copyOf(slots);
        }
    }

    /** The characteristic {@code kenmerk} of the object {@code onderwerp} binds. */
    record Kenmerk(String onderwerp, ElementSleutel kenmerk, Invoeging invoeging) implements Gevolg {}

    /**
     * The fault of a condition, evaluated again over the value array of {@code grootte} slots with the bound values at
     * {@code slots}, asking it {@code rol}: its evaluation fault ({@link Toetsitem.Rol#FOUT}), which the test of the
     * rule found, inserted under {@code fout} on the object {@code onderwerp} binds. That the evaluation answers here is
     * a broken contract, since the test faulted over the same values.
     */
    record Toetsfout(String onderwerp, Term toets, Toetsitem.Rol rol, List<Integer> slots, int grootte, Foutnaam fout,
                     Invoeging invoeging) implements Gevolg {
        public Toetsfout {
            slots = List.copyOf(slots);
            if (rol != Toetsitem.Rol.FOUT)
                throw new IllegalArgumentException("een gevolg vraagt van een voorwaarde alleen haar fout, niet " + rol);
        }
    }

    /** A fixed evaluation fault of the branch ({@code Rekenfout}) with the sentence {@code zin}, under {@code fout}, on
     *  the object {@code onderwerp} binds. */
    record Fout(String onderwerp, String zin, Foutnaam fout, Invoeging invoeging) implements Gevolg {}

    /** The §9.5 inconsistency of the form {@code vorm} on the object {@code onderwerp} binds. */
    record Inconsistentie(String onderwerp, String vorm, Invoeging invoeging) implements Gevolg {}

    /**
     * The fault of two or more writers of attribute {@code doel} that stand on one object with different values
     * (RUNTIME-SEMANTICS ch. 5.7), on the object {@code onderwerp} binds, over the value facts the slot {@code waarden}
     * holds: the writers in the order of the document, {@code documentplaats} giving a rule's place and a writer
     * without a place after them by name, the attribute named {@code attribuut} as declared, and the object's own type
     * named by {@code typenamen} from its bean class's simple name. The fault is the first writer's.
     */
    record Schrijversconflict(String onderwerp, int waarden, ElementSleutel doel, String attribuut,
                              Map<String, Integer> documentplaats, Map<String, String> typenamen, Invoeging invoeging)
            implements Gevolg {
        public Schrijversconflict {
            // a copy that keeps a null key: the place of a nameless rule, which the analysis refuses by name, is one
            documentplaats = Collections.unmodifiableMap(new HashMap<>(documentplaats));
            typenamen = Collections.unmodifiableMap(new HashMap<>(typenamen));
        }
    }

    /** The standing of parameter {@code parameter}, whose value the global {@code global} holds: a rule without a
     *  left-hand side, which fires once per session. */
    record Parameterstand(String parameter, String global, Invoeging invoeging) implements Gevolg {}

    /** The standing of the session's evaluation date, which the global {@code global} holds, null where the case
     *  carries none: a rule without a left-hand side, which fires once per session. */
    record Rekendatumstand(String global, Invoeging invoeging) implements Gevolg {}

    /** Where a standing takes what it states from, as the left-hand side of its rule decided: the input on the object's
     *  bean ({@code INVOER}); a derivation, the fact a form inserted ({@code AFLEIDING}); neither, so that a
     *  characteristic is onwaar ({@code GEEN}); the standing of a member on the object a path reaches
     *  ({@code BEREIKT}); or no object, where a link on the path is absent ({@code ONTBREKENDE_LINK}). */
    enum Standbron { INVOER, AFLEIDING, GEEN, BEREIKT, ONTBREKENDE_LINK }

    /**
     * The standing of characteristic {@code kenmerk} on the object {@code onderwerp} binds, {@code waar} or onwaar:
     * waar from the input or from a derivation ({@code bron}), onwaar where neither holds. Where both hold, the two
     * waar standings are equal facts, which truth maintenance keeps as one with a justification each.
     */
    record Kenmerkstand(String onderwerp, ElementSleutel kenmerk, Standbron bron, boolean waar, Invoeging invoeging)
            implements Gevolg {
        public Kenmerkstand {
            if (bron != Standbron.INVOER && bron != Standbron.AFLEIDING && bron != Standbron.GEEN
                    || waar == (bron == Standbron.GEEN))
                throw new IllegalArgumentException("een kenmerkstand is waar uit de invoer of een afleiding en onwaar"
                        + " uit geen van beide, niet " + waar + " uit " + bron);
        }
    }

    /**
     * The standing of attribute {@code attribuut} on the object {@code onderwerp} binds: the value in slot
     * {@code waarde} of the value array, from {@code bron}, a derivation's value or the input on the bean. Where
     * {@code emmerscalair}, the input, a timeline, stands as the asOfDate-bucket scalar the case gave
     * ({@code Emmerscalair}, RUNTIME-SEMANTICS ch. 8.4), which the consequence takes from the bound timeline.
     */
    record Stand(String onderwerp, ElementSleutel attribuut, Standbron bron, int waarde, boolean emmerscalair,
                 Invoeging invoeging) implements Gevolg {
        public Stand {
            if (bron != Standbron.INVOER && bron != Standbron.AFLEIDING || emmerscalair && bron != Standbron.INVOER)
                throw new IllegalArgumentException("een stand komt uit de invoer of een afleiding, en alleen de invoer"
                        + " staat als emmerscalair, niet " + bron + (emmerscalair ? " als emmerscalair" : ""));
        }
    }

    /**
     * The standing of member {@code lid} at the end of the path {@code pad} on the object {@code onderwerp} binds:
     * where every link on the path is present ({@code bron} {@code BEREIKT}), the value in slot {@code waarde} of the
     * value array, the member's standing on the object the path reaches; where a link is absent
     * ({@code ONTBREKENDE_LINK}), null, and {@code waarde} is -1.
     */
    record Padstand(String onderwerp, String pad, ElementSleutel lid, Standbron bron, int waarde, Invoeging invoeging)
            implements Gevolg {
        public Padstand {
            if (bron != Standbron.BEREIKT && bron != Standbron.ONTBREKENDE_LINK
                    || (bron == Standbron.BEREIKT) != (waarde >= 0))
                throw new IllegalArgumentException("een padstand neemt de stand op het bereikte object uit een slot, of"
                        + " is leeg bij een ontbrekende link, zonder slot; niet " + bron + " met slot " + waarde);
        }
    }

    /**
     * The keys of the object {@code onderwerp} binds in source {@code bron} of the uniqueness {@code uniciteit}
     * (RUNTIME-SEMANTICS ch. 5.6, 5.9), made by the route {@link Route#UNICITEITSSLEUTELS}: its key components in the
     * source's order, each the value of its term in {@code componenten} over the value array of {@code grootte} slots
     * with the bound values at {@code slots}, an enumeration label in its normalised form; the parent in slot
     * {@code scope}, -1 for a form without a scope; and where the component at position {@code tijdlijn} is on a
     * timeline (-1: none), one key per value that timeline ever carries (ever-uniqueness).
     */
    record Uniciteitssleutel(String onderwerp, String uniciteit, int bron, List<Term> componenten, int tijdlijn,
                             int scope, List<Integer> slots, int grootte, Invoeging invoeging) implements Gevolg {
        public Uniciteitssleutel {
            componenten = List.copyOf(componenten);
            slots = List.copyOf(slots);
        }
    }

    /** The violation of the uniqueness {@code uniciteit} by the object {@code onderwerp} binds, a key of which is in a
     *  duplicate (ch. 5.9). */
    record Uniciteitsschending(String onderwerp, String uniciteit, Invoeging invoeging) implements Gevolg {}

    /** The fault of the form of an input on the object {@code onderwerp} binds, the sentence {@code voor}, the object
     *  and {@code na}, under {@code fout}, made by the route {@link Route#INVOERVORMFOUT}. */
    record Invoervormfout(String onderwerp, String voor, String na, Foutnaam fout, Invoeging invoeging)
            implements Gevolg {}

    /** The standing of the §9.5 channel of the form {@code producent} (ch. 5.6), {@code inconsistent} or consistent: a
     *  rule without a subject, whose left-hand side tests whether an inconsistency of that form stands. */
    record Kanaalstand(String producent, boolean inconsistent, Invoeging invoeging) implements Gevolg {}

    /** A recipient of the allocation {@code verdeling} for the source {@code onderwerp} binds (ch. 4.1): the recipient
     *  in slot {@code ontvanger}, and its weight, its ordering key and its cap in the slots {@code gewicht},
     *  {@code sleutel} and {@code maximum}, each -1 where the allocation declares none, so that the fact carries null
     *  there. */
    record Ontvanger(String onderwerp, String verdeling, int ontvanger, int gewicht, int sleutel, int maximum,
                     Invoeging invoeging) implements Gevolg {}

    /**
     * The distribution of an allocation over the recipients of the source {@code onderwerp} binds (ch. 4.2 to 4.4),
     * computed by the route {@link Route#UITDELING}, which calls {@code Verdeling.bereken}: by {@code methode}, of the
     * pot in slot {@code pot} over the list of {@code Ontvanger} facts in slot {@code lijst}, {@code gewogen} where the
     * allocation declares a ratio attribute, {@code gemaximeerd} where it declares a cap, {@code afnemend} the ordered
     * method's direction, {@code decimalen} the floor's decimals (null: none). Each share is a value of {@code aandeel}
     * on its recipient and the rest a value of {@code rest} on the source (null: the allocation writes no rest), both
     * written by {@code schrijver}; each degradation is the §9.5 inconsistency of the form {@code vorm} on its
     * recipient.
     */
    record Uitdeling(String onderwerp, String vorm, ResolvedVerdeling.Methode methode, int pot, int lijst,
                     boolean gewogen, boolean gemaximeerd, boolean afnemend, Integer decimalen, ElementSleutel aandeel,
                     ElementSleutel rest, String schrijver, Invoeging invoeging) implements Gevolg {}

    /**
     * A member's place in the order of a fold, as a rule of the fold binds it, computed by the route
     * {@link Route#VOUWPLAATS}: the number {@code selectie} of the selection of an instance-set union (0 for any other
     * fold), then per to-many step of the member's route, in order, the least place of the step's member in its list
     * ({@link Stap}). {@code reden} is what the rule needs the place for, which names the broken contract where the
     * input states no place of a step.
     */
    record Vouwplaats(int selectie, List<Stap> stappen, Plaatsreden reden) {
        public Vouwplaats {
            stappen = List.copyOf(stappen);
        }

        /** One to-many step: the reference field {@code veld} its member links through, and the slots of the count of
         *  that member's {@code Lijstplaats} facts under the field and of the least place among them. */
        public record Stap(String veld, int aantal, int plaats) {}
    }

    /** What a rule of a fold needs a member's place for: the first fault among the members' filters
     *  ({@code FILTERFOUT}), the first member whose timeline the case gives as the asOfDate-bucket scalar
     *  ({@code LIDFOUT}), or the members that the short circuit of a quantifier fold reaches ({@code KWANTOR}). */
    enum Plaatsreden { FILTERFOUT, LIDFOUT, KWANTOR }

    /**
     * The contribution ({@code Bijdrage}, RUNTIME-SEMANTICS ch. 3.4) of the member in slot {@code lid}, the subject of a
     * contribution rule, to the fold {@code vouw} of the fold's subject, the object {@code onderwerp} binds: what the
     * member contributes is {@code inbreng}, computed over a value array of {@code grootte} slots with the bound values
     * at {@code slots}.
     */
    record Bijdrage(String onderwerp, String vouw, int lid, Inbreng inbreng, List<Integer> slots, int grootte,
                    Invoeging invoeging) implements Gevolg {
        public Bijdrage {
            slots = List.copyOf(slots);
        }
    }

    /** What a member contributes to its fold, as the analysis decided from the fold's function. */
    sealed interface Inbreng {
        /** The member itself, without a value: a count of the members. */
        record Telling() implements Inbreng {}

        /** The member's value in slot {@code waarde}, made by the route {@link Route#BIJDRAGE}: nothing where it is
         *  leeg; where {@code totaal} is not null the slot holds the member's timeline, which contributes its total over
         *  time per that unit, and nothing where the case gives it as the asOfDate-bucket scalar; where {@code gevuld},
         *  the member without a value, a count of the members whose value is gevuld. */
        record Lidwaarde(int waarde, String totaal, boolean gevuld) implements Inbreng {}

        /** The verdict of the matrix of a quantifier fold for the member, the condition {@code matrix}, which cannot
         *  fault or deposit: 1 where it holds, 0 where it does not. */
        record Oordeel(Term matrix) implements Inbreng {}

        /** The test of the member by the matrix of a quantifier fold, the condition {@code matrix}, which can fault or
         *  deposit, as a {@code Lidtoets}: the member's {@code plaats} in the order of the members, then the verdict or
         *  the evaluation fault of the matrix, and whether the probe the consequence puts in slot {@code sonde} (-1:
         *  none, the matrix deposits nothing) heard a place that deposits. The place comes first, so a member without a
         *  place breaks the contract before the matrix is evaluated. */
        record Lidtoets(Term matrix, int sonde, Vouwplaats plaats) implements Inbreng {
            public Lidtoets {
                if (plaats.reden() != Plaatsreden.KWANTOR)
                    throw new IllegalArgumentException("de plaats van een lidtoets is die van de kwantor, niet "
                            + plaats.reden());
            }
        }
    }

    /** That evaluating the filter of the fold {@code vouw} for the member in slot {@code lid} deposits, a
     *  {@code Filterdeponering} on the fold's subject, the object {@code onderwerp} binds; the test of the rule found
     *  it. */
    record Filterdeponering(String onderwerp, String vouw, int lid, Invoeging invoeging) implements Gevolg {}

    /**
     * The fault of the filter of the fold {@code vouw} for the member in slot {@code lid}, a {@code Filterfout} on the
     * fold's subject, the object {@code onderwerp} binds: the condition {@code filter}, evaluated again over the value
     * array of {@code grootte} slots with the bound values at {@code slots}, gives its evaluation fault, which the test
     * of the rule found, so that it answering here is a broken contract; then the member's {@code plaats} in the fold's
     * order.
     */
    record Filterfout(String onderwerp, String vouw, int lid, Term filter, Vouwplaats plaats, List<Integer> slots,
                      int grootte, Invoeging invoeging) implements Gevolg {
        public Filterfout {
            slots = List.copyOf(slots);
            if (plaats.reden() != Plaatsreden.FILTERFOUT)
                throw new IllegalArgumentException("de plaats van een filterfout is die van het filter, niet "
                        + plaats.reden());
        }
    }

    /** The fault of the member in slot {@code lid} of the per-element fold {@code vouw} over an attribute on a
     *  timeline, which the case gives as the asOfDate-bucket scalar, a {@code Lidfout} on the fold's subject, the object
     *  {@code onderwerp} binds: the member's {@code plaats} in the fold's order, and the fault of the form of an input,
     *  the sentence {@code voor}, the member and {@code na}, named in no rule yet, made by the route
     *  {@link Route#INVOERVORMFOUT}. */
    record Lidfout(String onderwerp, String vouw, int lid, Vouwplaats plaats, String voor, String na,
                   Invoeging invoeging) implements Gevolg {
        public Lidfout {
            if (plaats.reden() != Plaatsreden.LIDFOUT)
                throw new IllegalArgumentException("de plaats van een lidfout is die van het lid, niet " + plaats.reden());
        }
    }

    /**
     * An object creation (RUNTIME-SEMANTICS ch. 5.5, IR-SPEC ch. 3.3) of the creation {@code creatie} for the subject the
     * variable {@code onderwerp} binds, over a value array of {@code grootte} slots with the bound values at
     * {@code slots}. Its consequence takes four steps, in this order:
     * <ol>
     *   <li>the creation depth of the new object, made by the route {@link Route#CREATIEDIEPTE} from {@code diepte}:
     *       past the budget it throws, the fault at the making, before anything is made;</li>
     *   <li>the value of each of {@code initialisaties}, in order, each collapsed from an exact rational to a decimal;
     *       where one raises an evaluation fault (RUNTIME-SEMANTICS ch. 5.2), that fault, named under {@code fout} and
     *       inserted on the subject as {@code foutinvoeging}, is all the creation does, as in the reference
     *       interpreter, which then makes nothing;</li>
     *   <li>the construction of the new object ({@code constructie}): a bean of its class, on which the attribute
     *       initialisations whose value is gevuld are set before the bean is inserted. That is construction and no
     *       write on a bean in working memory: the bean is in none yet, so no rule ever saw it without them;</li>
     *   <li>the insertions {@code invoegingen}, in order, the new object itself first ({@link Invoegsel}).</li>
     * </ol>
     * The constructor holds the plan to that order, the order of the consequence before the step exactly: every
     * initialisation is set in the construction where it is an attribute, or inserted as a characteristic after the new
     * object where it is one, exactly once; the attributes are set in the order of the initialisations; the new object is
     * inserted first, then its mark, then its link, then the characteristics in the order of the initialisations. Any
     * other order fails loudly.
     */
    record Creatie(String onderwerp, String creatie, Creatiediepte diepte, List<Initialisatie> initialisaties,
                   Foutnaam fout, Invoeging foutinvoeging, Constructie constructie, int fase,
                   List<Invoegsel> invoegingen, List<Integer> slots, int grootte) implements Gevolg {
        public Creatie {
            initialisaties = List.copyOf(initialisaties);
            invoegingen = List.copyOf(invoegingen);
            slots = List.copyOf(slots);
            int[] keer = new int[initialisaties.size()];
            for (Constructie.Zetting z : constructie.zettingen()) {
                if (initialisaties.get(z.initialisatie()).lid().kenmerk())
                    throw new IllegalArgumentException("een kenmerkinitialisatie wordt ingevoegd, niet gezet: "
                            + initialisaties.get(z.initialisatie()).lid());
                keer[z.initialisatie()]++;
            }
            if (invoegingen.isEmpty() || !(invoegingen.get(0) instanceof Invoegsel.Nieuw))
                throw new IllegalArgumentException("een creatie voegt eerst het nieuwe object in: " + invoegingen);
            int merken = 0, links = 0;
            for (Invoegsel i : invoegingen)
                switch (i) {
                    case Invoegsel.Nieuw n -> { }
                    case Invoegsel.Merk m -> merken++;
                    case Invoegsel.Link l -> links++;
                    case Invoegsel.Kenmerk k -> {
                        if (!initialisaties.get(k.initialisatie()).lid().kenmerk())
                            throw new IllegalArgumentException("een attribuutinitialisatie wordt gezet, niet ingevoegd: "
                                    + initialisaties.get(k.initialisatie()).lid());
                        keer[k.initialisatie()]++;
                    }
                }
            if (invoegingen.stream().filter(i -> i instanceof Invoegsel.Nieuw).count() != 1 || merken != 1
                    || links != 1)
                throw new IllegalArgumentException("een creatie voegt het nieuwe object, zijn merk en zijn koppeling elk"
                        + " één keer in: " + invoegingen);
            for (int i = 0; i < keer.length; i++)
                if (keer[i] != 1)
                    throw new IllegalArgumentException("de initialisatie van " + initialisaties.get(i).lid() + " komt "
                            + keer[i] + " keer in de constructie of de invoegingen, niet één keer");
            // the order of the consequence before step 1c-C1, which a rendering carries out as the plan lists it: a
            // rendering into DRL text has no other source for the order of the facts the agenda then sees
            List<Integer> attributen = new ArrayList<>(), kenmerken = new ArrayList<>();
            for (int i = 0; i < initialisaties.size(); i++)
                (initialisaties.get(i).lid().kenmerk() ? kenmerken : attributen).add(i);
            List<Integer> gezet = constructie.zettingen().stream().map(Constructie.Zetting::initialisatie).toList();
            if (!gezet.equals(attributen))
                throw new IllegalArgumentException("een creatie zet haar attribuutinitialisaties in hun volgorde "
                        + attributen + ", niet " + gezet);
            List<String> volgorde = new ArrayList<>(List.of("Nieuw", "Merk", "Link"));
            for (int k : kenmerken) volgorde.add("Kenmerk " + k);
            List<String> gegeven = invoegingen.stream().map(i -> i instanceof Invoegsel.Kenmerk k
                    ? "Kenmerk " + k.initialisatie() : i.getClass().getSimpleName()).toList();
            if (!gegeven.equals(volgorde))
                throw new IllegalArgumentException("een creatie voegt het nieuwe object, zijn merk, zijn koppeling en"
                        + " dan haar kenmerkinitialisaties in hun volgorde in, " + volgorde + ", niet " + gegeven);
        }
    }

    /** The creation depth of an object creation's new object (RUNTIME-SEMANTICS ch. 5.5, 6.3), which the route
     *  {@link Route#CREATIEDIEPTE} computes: one more than the depth of the subject, of declared type {@code type}, in
     *  the creation's stratum {@code stratum}, from the subject's marks in slot {@code herkomst} of the value array; past
     *  {@code budget} the route throws the fault at the making. */
    record Creatiediepte(int herkomst, int stratum, String type, int budget) {}

    /** One member initialisation of an object creation: the member {@code lid} of the new object, and its value
     *  {@code waarde}, evaluated over the bound values of the rule. */
    record Initialisatie(ElementSleutel lid, Term waarde) {}

    /** How an object creation makes its new object: a bean of {@code klasse}, by its constructor without arguments, on
     *  which each of {@code zettingen} is set, in order, before the bean is inserted. */
    record Constructie(Class<?> klasse, List<Zetting> zettingen) {
        public Constructie {
            zettingen = List.copyOf(zettingen);
        }

        /** The value of the attribute initialisation at index {@code initialisatie}, set on the new bean through the
         *  setter of the bean property {@code veld} where the value is gevuld; a leeg value leaves the member unset. */
        public record Zetting(int initialisatie, Binding.Veld veld) {}
    }

    /** One insertion of an object creation, after its construction, with how it goes into working memory. */
    sealed interface Invoegsel {
        Invoeging invoeging();

        /** The new object itself. */
        record Nieuw(Invoeging invoeging) implements Invoegsel {}

        /** The new object's {@code Gemaakt}: the creation, the subject, the new object, the creation phase in slot
         *  {@link Creatie#fase}, the stratum and the depth of {@link Creatie#diepte}. */
        record Merk(Invoeging invoeging) implements Invoegsel {}

        /** The {@code Koppeling} of the relation {@code relatie} between the new object and the subject, the new object
         *  its first end where {@code nieuwIsEerste} and its second end otherwise, so the relation's order of roles
         *  decides the direction, carrying the creation phase in slot {@link Creatie#fase}. */
        record Link(String relatie, boolean nieuwIsEerste, Invoeging invoeging) implements Invoegsel {}

        /** The {@code HeeftKenmerk} of the characteristic of the initialisation at index {@code initialisatie} on the
         *  new object, where its value is true. */
        record Kenmerk(int initialisatie, Invoeging invoeging) implements Invoegsel {}
    }

    /** The link of the relation {@code relatie} whose first end is the object the variable {@code eerste} binds and
     *  whose second end is the object {@code tweede} binds, so the relation's order of roles decides the direction,
     *  carrying the creation phase in slot {@code fase} of the value array, or phase 0, the input's, where {@code fase}
     *  is -1: a link a reference field on a bean of the input carries, or the link a fact creation makes. */
    record Koppeling(String relatie, String eerste, String tweede, int fase, Invoeging invoeging) implements Gevolg {}

    /** The fault of a second distinct filler of a to-one role (RUNTIME-SEMANTICS ch. 5.5), a fault of the program and
     *  no overwrite: on the object {@code onderwerp} binds, the near end of two links of one relation to different
     *  objects, under the declared name of the relation {@code relatie}, an {@code IllegalStateException} with the
     *  sentence {@code zin}. */
    record TweedeVulling(String onderwerp, String relatie, String zin, Invoeging invoeging) implements Gevolg {}
}
